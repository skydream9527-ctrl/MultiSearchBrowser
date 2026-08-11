package com.browser.app.ui.profile

import android.os.Bundle
import androidx.navigation.NavDirections
import com.browser.app.R
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.String

public class HistoryFragmentDirections private constructor() {
  private data class ActionHistoryFragmentToWebviewFragment(
    public val url: String,
    public val windowId: Long = 0L,
    public val isIncognito: Boolean = false,
  ) : NavDirections {
    public override val actionId: Int = R.id.action_historyFragment_to_webviewFragment

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
    public fun actionHistoryFragmentToWebviewFragment(
      url: String,
      windowId: Long = 0L,
      isIncognito: Boolean = false,
    ): NavDirections = ActionHistoryFragmentToWebviewFragment(url, windowId, isIncognito)
  }
}
