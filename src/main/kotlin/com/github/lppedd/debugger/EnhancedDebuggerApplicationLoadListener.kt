package com.github.lppedd.debugger

import com.intellij.debugger.engine.JavaDebugProcess
import com.intellij.debugger.ui.breakpoints.JavaLineBreakpointType
import com.intellij.ide.ApplicationLoadListener
import com.intellij.openapi.application.Application
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.XDebuggerManagerListener
import com.intellij.xdebugger.breakpoints.XBreakpointHandler
import com.intellij.xdebugger.breakpoints.XBreakpointType
import java.lang.reflect.Field
import java.lang.reflect.Modifier

/**
 * @author Edoardo Luppi
 */
@Suppress("UnstableApiUsage")
internal class EnhancedDebuggerApplicationLoadListener : ApplicationLoadListener {
  override fun beforeApplicationLoaded(application: Application, configPath: String) {
    @Suppress("deprecation")
    XBreakpointType.EXTENSION_POINT_NAME.getPoint(null).unregisterExtension(JavaLineBreakpointType::class.java)
    application.messageBus.connect(application).subscribe(ProjectManager.TOPIC, MyProjectManagerListener())
  }

  private class MyProjectManagerListener : ProjectManagerListener {
    override fun projectOpened(project: Project) {
      project.messageBus.connect(project).subscribe(XDebuggerManager.TOPIC, MyXDebuggerManagerListener())
    }
  }

  private class MyXDebuggerManagerListener : XDebuggerManagerListener {
    override fun processStarted(debugProcess: XDebugProcess) {
      if (debugProcess !is JavaDebugProcess) {
        return
      }

      val field = JavaDebugProcess::class.java.getDeclaredField("myBreakpointHandlers")

      @Suppress("unchecked_cast")
      val breakpointHandlers = field.let {
        it.removeFinal()
        it.isAccessible = true
        it.get(debugProcess) as Array<XBreakpointHandler<*>>
      }

      val newBreakpointHandlers = breakpointHandlers.filter {
        it.breakpointTypeClass != JavaLineBreakpointType::class.java
      }

      field.set(debugProcess, newBreakpointHandlers.toTypedArray())
    }

    private fun Field.removeFinal() {
      val modifiers = Field::class.java.getDeclaredField("modifiers")
      modifiers.isAccessible = true
      modifiers.setInt(this, this.modifiers and Modifier.FINAL.inv())
    }
  }
}
