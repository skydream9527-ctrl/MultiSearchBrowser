package com.browser.app.ui.home

import android.os.Bundle
import androidx.navigation.NavDirections
import com.browser.app.R
import kotlin.Int
import kotlin.Long
import kotlin.String

public class HomeFragmentDirections private constructor() {
  private data class ActionHomeFragmentToWebviewFragment(
    public val url: String,
    public val windowId: Long = 0L,
  ) : NavDirections {
    public override val actionId: Int = R.id.action_homeFragment_to_webviewFragment

    public override val arguments: Bundle
      get() {
        val result = Bundle()
        result.putString("url", this.url)
        result.putLong("windowId", this.windowId)
        return result
      }
  }

  public companion object {
    public fun actionHomeFragmentToWebviewFragment(url: String, windowId: Long = 0L): NavDirections
        = ActionHomeFragmentToWebviewFragment(url, windowId)
  }
}
