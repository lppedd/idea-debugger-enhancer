package com.github.lppedd.debugger

import com.intellij.debugger.DebuggerBundle
import com.intellij.debugger.ui.breakpoints.Breakpoint
import com.intellij.debugger.ui.breakpoints.JavaLineBreakpointType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ReflectionUtil
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.breakpoints.XBreakpoint
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import com.intellij.xdebugger.breakpoints.ui.XBreakpointCustomPropertiesPanel
import org.jetbrains.java.debugger.breakpoints.properties.JavaLineBreakpointProperties
import java.util.stream.Collectors
import javax.swing.Icon

/**
 * @author Edoardo Luppi
 */
internal class EnhancedJavaLineBreakpointType :
  JavaLineBreakpointType("java-line", DebuggerBundle.message("line.breakpoints.tab.title")) {
  private companion object {
    private val SOURCE_POSITION =
      ReflectionUtil.getDeclaredField(XLineBreakpointAllVariant::class.java, "mySourcePosition")

    init {
      if (SOURCE_POSITION == null) {
        throw UnsupportedOperationException("Cannot find XLineBreakpointAllVariant#mySourcePosition")
      }

      SOURCE_POSITION.isAccessible = true
    }
  }

  override fun createJavaBreakpoint(
    project: Project,
    breakpoint: XBreakpoint<JavaLineBreakpointProperties>
  ): Breakpoint<JavaLineBreakpointProperties> =
    EnhancedLineBreakpoint(project, breakpoint)

  override fun computeVariants(
    project: Project,
    position: XSourcePosition
  ): List<JavaBreakpointVariant> =
    super.computeVariants(project, position)
      .stream()
      .map(::wrapVariant)
      .collect(Collectors.toList())

  override fun createProperties(): EnhancedJavaLineBreakpointProperties =
    EnhancedJavaLineBreakpointProperties()

  override fun createBreakpointProperties(
    file: VirtualFile,
    line: Int
  ): EnhancedJavaLineBreakpointProperties =
    EnhancedJavaLineBreakpointProperties()

  override fun createCustomPropertiesPanel(project: Project): XBreakpointCustomPropertiesPanel<XLineBreakpoint<JavaLineBreakpointProperties>> =
    EnhancedBreakpointPanel(project)

  private fun wrapVariant(variant: JavaBreakpointVariant): DelegatingXLineBreakpointAllVariant =
    DelegatingXLineBreakpointAllVariant(variant)

  private inner class DelegatingXLineBreakpointAllVariant(private val delegate: JavaBreakpointVariant) :
    JavaBreakpointVariant((SOURCE_POSITION!!.get(delegate) as XSourcePosition)) {
    override fun getText(): String =
      delegate.text

    override fun getIcon(): Icon? =
      delegate.icon

    override fun getHighlightRange(): TextRange? =
      delegate.highlightRange

    override fun createProperties(): JavaLineBreakpointProperties? {
      val properties = super.createProperties()
      val delegateProperties = delegate.createProperties()

      if (properties != null && delegateProperties != null) {
        properties.lambdaOrdinal = delegateProperties.lambdaOrdinal
      }

      return properties
    }
  }
}
