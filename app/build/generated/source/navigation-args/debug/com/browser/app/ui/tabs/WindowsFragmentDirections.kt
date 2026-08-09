package com.browser.app.ui.tabs

import android.os.Bundle
import androidx.navigation.NavDirections
import com.browser.app.R
import kotlin.Int
import kotlin.Long
import kotlin.String

public class WindowsFragmentDirections private constructor() {
  private data class ActionWindowsFragmentToWebviewFragment(
    public val url: String,
    public val windowId: Long = 0L,
  ) : NavDirections {
    public override val actionId: Int = R.id.action_windowsFragment_to_webviewFragment

    public override val arguments: Bundle
      get() {
        val result = Bundle()
        result.putString("url", this.url)
        result.putLong("windowId", this.windowId)
        return result
      }
  }

  public companion object {
    public fun actionWindowsFragmentToWebviewFragment(url: String, windowId: Long = 0L):
        NavDirections = ActionWindowsFragmentToWebviewFragment(url, windowId)
  }
}
