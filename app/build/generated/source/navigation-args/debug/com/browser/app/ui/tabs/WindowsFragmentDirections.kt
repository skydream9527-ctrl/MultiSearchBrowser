package com.browser.app.ui.tabs

import android.os.Bundle
import androidx.navigation.NavDirections
import com.browser.app.R
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.String

public class WindowsFragmentDirections private constructor() {
  private data class ActionWindowsFragmentToWebviewFragment(
    public val url: String,
    public val windowId: Long = 0L,
    public val isIncognito: Boolean = false,
  ) : NavDirections {
    public override val actionId: Int = R.id.action_windowsFragment_to_webviewFragment

    public override val arguments: Bundle
      get() {
        val result = Bundle()
        result.putString("url", this.url)
        result.putLong("windowId", this.windowId)
        result.putBoolean("isIncognito", this.isIncognito)
        return result
      }
  }

  public companion object {
    public fun actionWindowsFragmentToWebviewFragment(
      url: String,
      windowId: Long = 0L,
      isIncognito: Boolean = false,
    ): NavDirections = ActionWindowsFragmentToWebviewFragment(url, windowId, isIncognito)
  }
}
