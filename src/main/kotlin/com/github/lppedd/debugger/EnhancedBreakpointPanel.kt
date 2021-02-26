package com.github.lppedd.debugger

import com.github.lppedd.debugger.java.EnhancedJavaLineBreakpointProperties
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.EnumComboBoxModel
import com.intellij.ui.components.JBCheckBox
import com.intellij.util.ui.JBUI
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import com.intellij.xdebugger.breakpoints.ui.XBreakpointCustomPropertiesPanel
import com.intellij.xdebugger.impl.breakpoints.XBreakpointBase
import com.intellij.xdebugger.impl.ui.XDebuggerExpressionComboBox
import org.jetbrains.java.debugger.breakpoints.properties.JavaLineBreakpointProperties
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.Box
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * @author Edoardo Luppi
 */
internal class EnhancedBreakpointPanel(
  private val project: Project,
  private val otherPanel: XBreakpointCustomPropertiesPanel<XLineBreakpoint<JavaLineBreakpointProperties>>?,
) : XBreakpointCustomPropertiesPanel<XLineBreakpoint<JavaLineBreakpointProperties>>() {
  private companion object {
    private const val HISTORY_ID = "ENHANCED_FORCE_EXPRESSION"
  }

  private val mainPanel = JPanel(BorderLayout())
  private val forceReturnCheckBox = JBCheckBox("Force return with")
  private val typeComboBox = ComboBox(EnumComboBoxModel(ForceType::class.java), JBUI.scale(90))

  @Volatile
  private lateinit var expressionTextField: XDebuggerExpressionComboBox

  override fun getComponent(): JComponent =
    if (otherPanel == null) {
      mainPanel
    } else JPanel(BorderLayout(0, JBUI.scale(5))).also {
      it.add(otherPanel.component, BorderLayout.PAGE_START)
      it.add(mainPanel, BorderLayout.CENTER)
    }

  override fun saveTo(breakpoint: XLineBreakpoint<JavaLineBreakpointProperties>) {
    otherPanel?.saveTo(breakpoint)

    expressionTextField.saveTextInHistory()
    val properties = breakpoint.properties

    if (properties is EnhancedJavaLineBreakpointProperties) {
      properties.setExpression(EnhancedBreakpointExpressionState(expressionTextField.expression))
      properties.setEnabled(forceReturnCheckBox.isSelected)
      properties.setForceType(typeComboBox.selectedItem as ForceType)
    }
  }

  override fun loadFrom(breakpoint: XLineBreakpoint<JavaLineBreakpointProperties>) {
    otherPanel?.loadFrom(breakpoint)

    val type = breakpoint.type

    // noinspection ConstantConditions
    expressionTextField = XDebuggerExpressionComboBox(
      project,
      type.getEditorsProvider(breakpoint, project)!!,
      HISTORY_ID,
      breakpoint.sourcePosition,
      true,
      true
    )

    val properties = breakpoint.properties

    if (breakpoint is XBreakpointBase<*, *, *> && properties is EnhancedJavaLineBreakpointProperties) {
      val expression = properties.getExpression(breakpoint)
      if (expression != null) {
        expressionTextField.expression = expression.toXExpression()
      }
      expressionTextField.setEnabled(properties.isEnabled(breakpoint))
      forceReturnCheckBox.isSelected = properties.isEnabled(breakpoint)
      typeComboBox.model.selectedItem = properties.getForceType(breakpoint)
      typeComboBox.isEnabled = properties.isEnabled(breakpoint)
    }

    forceReturnCheckBox.border = JBUI.Borders.emptyRight(3)
    forceReturnCheckBox.addItemListener {
      val isSelected = forceReturnCheckBox.isSelected
      expressionTextField.setEnabled(isSelected)
      typeComboBox.setEnabled(isSelected)
    }

    val forceReturnPanel = JPanel(FlowLayout(FlowLayout.LEADING, 0, 0))
    forceReturnPanel.add(forceReturnCheckBox)
    forceReturnPanel.add(Box.createHorizontalStrut(JBUI.scale(1)))
    forceReturnPanel.add(typeComboBox)

    mainPanel.add(forceReturnPanel, BorderLayout.PAGE_START)
    mainPanel.add(expressionTextField.component, BorderLayout.CENTER)
    mainPanel.border = JBUI.Borders.empty(5, 0)
  }
}
