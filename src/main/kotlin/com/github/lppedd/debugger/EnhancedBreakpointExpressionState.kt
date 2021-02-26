package com.github.lppedd.debugger

import com.intellij.xdebugger.XExpression
import com.intellij.xdebugger.impl.breakpoints.XExpressionState

/**
 * @author Edoardo Luppi
 */
internal class EnhancedBreakpointExpressionState : XExpressionState {
  @Suppress("unused")
  constructor() {
    // Serializer needs a default constructor
  }

  constructor(expression: XExpression) : super(false, expression)
}
