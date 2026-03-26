package com.browser.app.ui.home

public object HomeFragmentDirections {
    public class ActionHomeFragmentToWebviewFragment(
        public val url: String
    ) : androidx.navigation.NavDirections {
        override fun getActionId(): Int = com.browser.app.R.id.action_homeFragment_to_webviewFragment
        override fun getArguments(): Bundle {
            val result = Bundle()
            result.putString("url", url)
            return result
        }
    }

    public fun actionHomeFragmentToWebviewFragment(url: String): ActionHomeFragmentToWebviewFragment {
        return ActionHomeFragmentToWebviewFragment(url)
    }
}