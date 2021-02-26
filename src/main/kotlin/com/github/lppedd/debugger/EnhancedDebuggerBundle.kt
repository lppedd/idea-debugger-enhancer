package com.github.lppedd.debugger

import com.intellij.BundleBase
import com.intellij.reference.SoftReference
import org.jetbrains.annotations.PropertyKey
import java.lang.ref.Reference
import java.util.*

/**
 * @author Edoardo Luppi
 */
@Suppress("UnstableApiUsage")
internal object EnhancedDebuggerBundle {
  private const val BUNDLE = "messages.EnhancedDebuggerBundle"

  private var bundleReference: Reference<ResourceBundle>? = null
  private val bundle: ResourceBundle by lazy {
    SoftReference.dereference(bundleReference)
      ?: ResourceBundle.getBundle(BUNDLE).also {
        bundleReference = java.lang.ref.SoftReference(it)
      }
  }

  @JvmStatic
  operator fun get(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String =
    /* We cannot use anything else but BundleBase to be compatible
     * with all IDE versions and to avoid deprecated methods */
    BundleBase.message(bundle, key, *params)
}
