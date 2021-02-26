package com.github.lppedd.debugger

import com.github.lppedd.debugger.java.EnhancedJavaLineBreakpointProperties
import com.intellij.openapi.project.Project
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBRadioButton
import com.intellij.util.ui.JBUI
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import com.intellij.xdebugger.breakpoints.ui.XBreakpointCustomPropertiesPanel
import com.intellij.xdebugger.impl.breakpoints.XBreakpointBase
import com.intellij.xdebugger.impl.ui.XDebuggerExpressionComboBox
import org.jetbrains.java.debugger.breakpoints.properties.JavaLineBreakpointProperties
import java.awt.BorderLayout
import javax.swing.*

/**
 * @author Edoardo Luppi
 */
internal class EnhancedBreakpointPanel(
  private val project: Project,
  private val otherPanel: XBreakpointCustomPropertiesPanel<XLineBreakpoint<JavaLineBreakpointProperties>>?,
) : XBreakpointCustomPropertiesPanel<XLineBreakpoint<JavaLineBreakpointProperties>>() {
  private companion object {
    private const val HISTORY_ID = "ENHANCED_FORCE_EXPRESSION"
    private val separatorColor = JBColor.namedColor("Group.separatorColor", JBColor(Gray.xCD, Gray.x51))
  }

  private val mainPanel = JPanel(BorderLayout())
  private val forceReturnCheckBox = JBCheckBox("Force return")
  private val valueButton = JBRadioButton("Value")
  private val exceptionButton = JBRadioButton("Exception")

  @Volatile
  private lateinit var expressionTextField: XDebuggerExpressionComboBox

  override fun getComponent(): JComponent =
    if (otherPanel == null) {
      mainPanel.border = JBUI.Borders.empty(5, 0)
      mainPanel
    } else JPanel(BorderLayout()).also {
      mainPanel.border = JBUI.Borders.merge(
        JBUI.Borders.emptyTop(10),
        JBUI.Borders.customLine(separatorColor, 1, 0, 0, 0),
        true,
      )

      it.add(otherPanel.component, BorderLayout.CENTER)
      it.add(mainPanel, BorderLayout.PAGE_END)
    }

  override fun saveTo(breakpoint: XLineBreakpoint<JavaLineBreakpointProperties>) {
    otherPanel?.saveTo(breakpoint)

    expressionTextField.saveTextInHistory()
    val properties = breakpoint.properties

    if (properties is EnhancedJavaLineBreakpointProperties) {
      properties.setExpression(EnhancedBreakpointExpressionState(expressionTextField.expression))
      properties.setEnabled(forceReturnCheckBox.isSelected)
      properties.setForceType(if (valueButton.isSelected) ForceType.VALUE else ForceType.EXCEPTION)
    }
  }

  override fun loadFrom(breakpoint: XLineBreakpoint<JavaLineBreakpointProperties>) {
    otherPanel?.loadFrom(breakpoint)

    expressionTextField = XDebuggerExpressionComboBox(
      project,
      breakpoint.type.getEditorsProvider(breakpoint, project)!!,
      HISTORY_ID,
      breakpoint.sourcePosition,
      true,
      false,
    )

    val properties = breakpoint.properties

    if (breakpoint is XBreakpointBase<*, *, *> && properties is EnhancedJavaLineBreakpointProperties) {
      properties.getExpression(breakpoint)?.let {
        expressionTextField.expression = it.toXExpression()
      }

      val isEnabled = properties.isEnabled(breakpoint)
      expressionTextField.setEnabled(isEnabled)
      forceReturnCheckBox.isSelected = isEnabled
      valueButton.isEnabled = isEnabled
      exceptionButton.isEnabled = isEnabled

      val toSelect = when (properties.getForceType(breakpoint)) {
        ForceType.VALUE     -> valueButton
        ForceType.EXCEPTION -> exceptionButton
      }

      toSelect.isSelected = true
    }

    forceReturnCheckBox.border = JBUI.Borders.emptyRight(4)
    forceReturnCheckBox.addItemListener {
      val isSelected = forceReturnCheckBox.isSelected
      expressionTextField.setEnabled(isSelected)
      valueButton.isEnabled = isSelected
      exceptionButton.isEnabled = isSelected
    }

    ButtonGroup().also {
      it.add(valueButton)
      it.add(exceptionButton)
    }

    val forceReturnBox = Box.createHorizontalBox().also {
      it.add(forceReturnCheckBox)
      it.add(Box.createHorizontalStrut(JBUI.scale(6)))
      it.add(valueButton)
      it.add(Box.createHorizontalStrut(JBUI.scale(6)))
      it.add(exceptionButton)
      it.border = BorderFactory.createEmptyBorder(0, 1, JBUI.scale(5), 0)
    }

    val languageChooserBox = Box.createHorizontalBox().also {
      it.add(Box.createHorizontalGlue())
      it.add(expressionTextField.languageChooser)
      it.border = JBUI.Borders.empty(0, 2, 4, 2)
    }

    mainPanel.add(forceReturnBox, BorderLayout.PAGE_START)
    mainPanel.add(expressionTextField.component, BorderLayout.CENTER)
    mainPanel.add(languageChooserBox, BorderLayout.PAGE_END)
  }
}
