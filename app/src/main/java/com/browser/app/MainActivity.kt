package com.browser.app

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.browser.app.databinding.ActivityMainBinding

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

        binding.tabHome.setOnClickListener {
            navController.navigate(com.browser.app.R.id.homeFragment)
            updateTabSelection(0)
        }

        binding.tabWindows.setOnClickListener {
            navController.navigate(com.browser.app.R.id.windowsFragment)
            updateTabSelection(1)
        }

        binding.tabBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.tabProfile.setOnClickListener {
            navController.navigate(com.browser.app.R.id.profileFragment)
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
                getColor(com.browser.app.R.color.primary)
            } else {
                getColor(com.browser.app.R.color.gray)
            }
            icon.setColorFilter(color)
            text.setTextColor(color)
        }
    }
}