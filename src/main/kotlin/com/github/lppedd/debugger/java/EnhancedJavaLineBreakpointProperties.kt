package com.github.lppedd.debugger.java

import com.github.lppedd.debugger.EnhancedBreakpointExpressionState
import com.github.lppedd.debugger.ForceType
import com.intellij.util.ReflectionUtil
import com.intellij.util.xmlb.XmlSerializer
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.xdebugger.impl.breakpoints.BreakpointState
import com.intellij.xdebugger.impl.breakpoints.XBreakpointBase
import org.jetbrains.java.debugger.breakpoints.properties.JavaLineBreakpointProperties

/**
 * @author Edoardo Luppi
 */
internal class EnhancedJavaLineBreakpointProperties : JavaLineBreakpointProperties() {
  private companion object {
    private val myStateField = ReflectionUtil.getDeclaredField(XBreakpointBase::class.java, "myState")

    init {
      if (myStateField == null) {
        throw UnsupportedOperationException("Cannot find XBreakpointBase#myState")
      }

      myStateField.isAccessible = true
    }
  }

  @Attribute
  @Volatile
  private var isEnabled: Boolean? = null

  @Attribute
  @Volatile
  private var forceType: ForceType? = null

  @Tag
  @Volatile
  private var expression: EnhancedBreakpointExpressionState? = null

  fun isEnabled(breakpoint: XBreakpointBase<*, *, *>): Boolean {
    var enabled = isEnabled

    if (enabled != null) {
      return enabled
    }

    enabled = getStateProperties(breakpoint)?.isEnabled
    isEnabled = enabled
    return enabled ?: false
  }

  fun getForceType(breakpoint: XBreakpointBase<*, *, *>): ForceType {
    var tempForceType = forceType

    if (tempForceType != null) {
      return tempForceType
    }

    tempForceType = getStateProperties(breakpoint)?.forceType
    forceType = tempForceType
    return tempForceType ?: ForceType.VALUE
  }

  fun getExpression(breakpoint: XBreakpointBase<*, *, *>): EnhancedBreakpointExpressionState? {
    var tempExpression = expression

    if (tempExpression != null) {
      return tempExpression
    }

    tempExpression = getStateProperties(breakpoint)?.expression
    expression = tempExpression
    return tempExpression
  }

  fun setEnabled(isEnabled: Boolean) {
    this.isEnabled = isEnabled
  }

  fun setForceType(forceType: ForceType?) {
    this.forceType = forceType
  }

  fun setExpression(expression: EnhancedBreakpointExpressionState?) {
    this.expression = expression
  }

  private fun getStateProperties(breakpoint: XBreakpointBase<*, *, *>): EnhancedJavaLineBreakpointProperties? {
    val state = myStateField!!.get(breakpoint) as BreakpointState<*, *, *>
    return XmlSerializer.deserialize(state.propertiesElement ?: return null, javaClass)
  }
}
