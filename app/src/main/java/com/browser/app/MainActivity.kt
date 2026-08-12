package com.browser.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.browser.app.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // launchSingleTop + popUpTo(startDestination) 避免重复压栈导致的返回键累积
        val tabNavOptions = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setPopUpTo(R.id.homeFragment, false, false)
            .build()

        binding.tabHome.setOnClickListener {
            navController.navigate(R.id.homeFragment, null, tabNavOptions)
            updateTabSelection(0)
        }

        binding.tabWindows.setOnClickListener {
            navController.navigate(R.id.windowsFragment, null, tabNavOptions)
            updateTabSelection(1)
        }

        binding.tabBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
            // 返回键不切换高亮，保留当前 tab 选中态更直观
        }

        binding.tabProfile.setOnClickListener {
            navController.navigate(R.id.profileFragment, null, tabNavOptions)
            updateTabSelection(3)
        }
    }

    private fun updateTabSelection(selectedIndex: Int) {
        val tabs = listOf(
            Triple(binding.tabHomeIcon, binding.tabHomeText, binding.tabHome),
            Triple(binding.tabWindowsIcon, binding.tabWindowsText, binding.tabWindows),
            Triple(binding.tabBackIcon, binding.tabBackText, binding.tabBack),
            Triple(binding.tabProfileIcon, binding.tabProfileText, binding.tabProfile)
        )

        tabs.forEachIndexed { index, (icon, text, _) ->
            val color = if (index == selectedIndex) {
                getColor(R.color.primary)
            } else {
                getColor(R.color.gray)
            }
            icon.setColorFilter(color)
            text.setTextColor(color)
        }
    }
}
