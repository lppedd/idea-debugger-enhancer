package com.github.lppedd.debugger

import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.JavaBreakpointHandler
import com.intellij.debugger.engine.JavaBreakpointHandlerFactory

/**
 * @author Edoardo Luppi
 */
internal class EnhancedJavaLineBreakpointHandlerFactory : JavaBreakpointHandlerFactory {
  override fun createHandler(process: DebugProcessImpl): JavaBreakpointHandler =
    JavaBreakpointHandler(EnhancedJavaLineBreakpointType::class.java, process)
}
