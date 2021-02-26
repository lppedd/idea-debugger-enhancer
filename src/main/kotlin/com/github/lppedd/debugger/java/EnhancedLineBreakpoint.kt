package com.github.lppedd.debugger.java

import com.github.lppedd.debugger.EnhancedDebuggerBundle
import com.github.lppedd.debugger.ForceType
import com.intellij.debugger.engine.*
import com.intellij.debugger.engine.evaluation.TextWithImportsImpl
import com.intellij.debugger.engine.events.SuspendContextCommandImpl
import com.intellij.debugger.impl.DebuggerContextImpl
import com.intellij.debugger.impl.DebuggerUtilsEx
import com.intellij.debugger.jdi.ThreadReferenceProxyImpl
import com.intellij.debugger.settings.DebuggerSettings
import com.intellij.debugger.ui.breakpoints.LineBreakpoint
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.AppUIUtil
import com.intellij.xdebugger.XExpression
import com.intellij.xdebugger.breakpoints.XBreakpoint
import com.intellij.xdebugger.impl.breakpoints.XBreakpointBase
import com.sun.jdi.Method
import com.sun.jdi.ObjectReference
import com.sun.jdi.Value
import com.sun.jdi.event.LocatableEvent
import org.jetbrains.java.debugger.breakpoints.properties.JavaLineBreakpointProperties

/**
 * @author Edoardo Luppi
 */
internal class EnhancedLineBreakpoint(project: Project, xBreakpoint: XBreakpoint<*>) :
  LineBreakpoint<JavaLineBreakpointProperties>(project, xBreakpoint) {
  override fun getSuspendPolicy(): String {
    val breakpoint = xBreakpoint

    return if (breakpoint is XBreakpointBase<*, *, *> && properties.isEnabled(breakpoint)) {
      DebuggerSettings.SUSPEND_NONE
    } else {
      super.getSuspendPolicy()
    }
  }

  override fun processLocatableEvent(action: SuspendContextCommandImpl, event: LocatableEvent): Boolean {
    val breakpoint = xBreakpoint

    if (breakpoint is XBreakpointBase<*, *, *>) {
      val suspendContext = action.suspendContext!!
      suspendContext.initExecutionStacks(suspendContext.thread)

      val proxy = suspendContext.frameProxy!!
      val javaStackFrame = suspendContext.activeExecutionStack!!.createStackFrame(proxy) as JavaStackFrame
      val isEnabled = properties.isEnabled(breakpoint)
      val expression = properties.getExpression(breakpoint)

      if (isEnabled && expression != null) {
        threadAction(
          properties.getForceType(breakpoint),
          suspendContext.debugProcess,
          javaStackFrame,
          expression.toXExpression()!!,
          event,
        )

        return true
      }
    }

    return super.processLocatableEvent(action, event)
  }

  override fun getProperties(): EnhancedJavaLineBreakpointProperties =
    super.getProperties() as EnhancedJavaLineBreakpointProperties

  private fun threadAction(
    forceType: ForceType,
    debugProcess: DebugProcessImpl,
    myStackFrame: JavaStackFrame,
    expression: XExpression,
    event: LocatableEvent
  ) {
    val process = debugProcess.xdebugProcess
    val text = TextWithImportsImpl.fromXExpression(expression)
    val nodeManager = process!!.nodeManager
    val descriptor = nodeManager.getWatchItemDescriptor(null, text, null)
    val evalContext =
      JavaStackFrame::class.java.getDeclaredMethod("getFrameDebuggerContext", DebuggerContextImpl::class.java).let {
        it.isAccessible = true
        it.invoke(myStackFrame, debugProcess.debuggerContext) as DebuggerContextImpl
      }

    val createEvaluationContext = evalContext.createEvaluationContext()
    descriptor.setContext(createEvaluationContext)

    val exception = descriptor.evaluateException

    if (exception != null && descriptor.value == null) {
      return
    }

    val value = descriptor.value
    val thread = myStackFrame.descriptor.frameProxy.threadProxy()

    when (forceType) {
      ForceType.VALUE -> returnValue(debugProcess, thread, value, event.location().method())
      ForceType.EXCEPTION -> throwException(debugProcess, value, thread)
    }
  }

  private fun returnValue(
    debugProcess: DebugProcessImpl,
    thread: ThreadReferenceProxyImpl,
    value: Value,
    method: Method,
  ) {
    val valueType = value.type()
    val returnTypeName = method.returnTypeName()

    if (DebuggerUtilsEx.isVoid(method) || !DebuggerUtilsEx.isAssignableFrom(returnTypeName, valueType)) {
      val message = EnhancedDebuggerBundle["error.value.message", valueType.name(), returnTypeName]
      val title = EnhancedDebuggerBundle["error.value.title"]
      showErrorMessage(title, message)
      disableBreakpoint(debugProcess)
      return
    }

    debugProcess.startWatchingMethodReturn(thread)
    thread.forceEarlyReturn(value)
  }

  private fun throwException(
    debugProcess: DebugProcessImpl,
    value: Value,
    thread: ThreadReferenceProxyImpl
  ) {
    val valueType = value.type()

    if (!DebuggerUtilsEx.isAssignableFrom("java.lang.Exception", valueType)) {
      val message = EnhancedDebuggerBundle["error.exception.message", valueType.name()]
      val title = EnhancedDebuggerBundle["error.exception.title"]
      showErrorMessage(title, message)
      disableBreakpoint(debugProcess)
      return
    }

    thread.stop(value as ObjectReference)
  }

  private fun disableBreakpoint(debugProcess: DebugProcessImpl) {
    debugProcess.requestsManager.deleteRequest(this)
    debugProcess.addDebugProcessListener(object : DebugProcessListener {
      override fun resumed(suspendContext: SuspendContext) {
        disableIt()
      }

      override fun processDetached(process: DebugProcess, closedByUser: Boolean) {
        disableIt()
      }

      private fun disableIt() {
        debugProcess.removeDebugProcessListener(this)

        val requestManager = debugProcess.requestsManager
        markVerified(requestManager.isVerified(this@EnhancedLineBreakpoint))
        requestManager.deleteRequest(this@EnhancedLineBreakpoint)

        AppUIUtil.invokeOnEdt(this@EnhancedLineBreakpoint::updateUI)
      }
    })
  }

  private fun showErrorMessage(title: String, message: String) {
    AppUIUtil.invokeOnEdt { Messages.showMessageDialog(message, title, Messages.getErrorIcon()) }
  }
}
