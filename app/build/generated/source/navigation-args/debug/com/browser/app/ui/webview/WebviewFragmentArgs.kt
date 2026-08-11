package com.browser.app.ui.webview

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavArgs
import java.lang.IllegalArgumentException
import kotlin.Boolean
import kotlin.Long
import kotlin.String
import kotlin.jvm.JvmStatic

public data class WebviewFragmentArgs(
  public val url: String,
  public val windowId: Long = 0L,
  public val isIncognito: Boolean = false,
) : NavArgs {
  public fun toBundle(): Bundle {
    val result = Bundle()
    result.putString("url", this.url)
    result.putLong("windowId", this.windowId)
    result.putBoolean("isIncognito", this.isIncognito)
    return result
  }

  public fun toSavedStateHandle(): SavedStateHandle {
    val result = SavedStateHandle()
    result.set("url", this.url)
    result.set("windowId", this.windowId)
    result.set("isIncognito", this.isIncognito)
    return result
  }

  public companion object {
    @JvmStatic
    public fun fromBundle(bundle: Bundle): WebviewFragmentArgs {
      bundle.setClassLoader(WebviewFragmentArgs::class.java.classLoader)
      val __url : String?
      if (bundle.containsKey("url")) {
        __url = bundle.getString("url")
        if (__url == null) {
          throw IllegalArgumentException("Argument \"url\" is marked as non-null but was passed a null value.")
        }
      } else {
        throw IllegalArgumentException("Required argument \"url\" is missing and does not have an android:defaultValue")
      }
      val __windowId : Long
      if (bundle.containsKey("windowId")) {
        __windowId = bundle.getLong("windowId")
      } else {
        __windowId = 0L
      }
      val __isIncognito : Boolean
      if (bundle.containsKey("isIncognito")) {
        __isIncognito = bundle.getBoolean("isIncognito")
      } else {
        __isIncognito = false
      }
      return WebviewFragmentArgs(__url, __windowId, __isIncognito)
    }

    @JvmStatic
    public fun fromSavedStateHandle(savedStateHandle: SavedStateHandle): WebviewFragmentArgs {
      val __url : String?
      if (savedStateHandle.contains("url")) {
        __url = savedStateHandle["url"]
        if (__url == null) {
          throw IllegalArgumentException("Argument \"url\" is marked as non-null but was passed a null value")
        }
      } else {
        throw IllegalArgumentException("Required argument \"url\" is missing and does not have an android:defaultValue")
      }
      val __windowId : Long?
      if (savedStateHandle.contains("windowId")) {
        __windowId = savedStateHandle["windowId"]
        if (__windowId == null) {
          throw IllegalArgumentException("Argument \"windowId\" of type long does not support null values")
        }
      } else {
        __windowId = 0L
      }
      val __isIncognito : Boolean?
      if (savedStateHandle.contains("isIncognito")) {
        __isIncognito = savedStateHandle["isIncognito"]
        if (__isIncognito == null) {
          throw IllegalArgumentException("Argument \"isIncognito\" of type boolean does not support null values")
        }
      } else {
        __isIncognito = false
      }
      return WebviewFragmentArgs(__url, __windowId, __isIncognito)
    }
  }
}
