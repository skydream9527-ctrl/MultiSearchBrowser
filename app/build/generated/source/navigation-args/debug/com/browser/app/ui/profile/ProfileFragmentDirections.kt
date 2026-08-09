package com.browser.app.ui.profile

import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections
import com.browser.app.R

public class ProfileFragmentDirections private constructor() {
  public companion object {
    public fun actionProfileFragmentToHistoryFragment(): NavDirections =
        ActionOnlyNavDirections(R.id.action_profileFragment_to_historyFragment)

    public fun actionProfileFragmentToBookmarksFragment(): NavDirections =
        ActionOnlyNavDirections(R.id.action_profileFragment_to_bookmarksFragment)
  }
}
