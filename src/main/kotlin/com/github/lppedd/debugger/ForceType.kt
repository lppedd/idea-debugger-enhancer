package com.github.lppedd.debugger

/**
 * @author Edoardo Luppi
 */
internal enum class ForceType(private val value: String) {
  VALUE("Value"),
  EXCEPTION("Exception");

  override fun toString(): String =
    value
}
