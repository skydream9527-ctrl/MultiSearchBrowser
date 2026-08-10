package com.browser.app.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.browser.app.R
import com.browser.app.databinding.FragmentSettingsBinding
import com.browser.app.utils.SearchEngine
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBackButton()
        setupEngineSpinner()
        setupUaChips()
        setupPrivacySwitches()
        setupDisplaySwitches()
        setupClearDataButton()
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
    }

    private fun setupEngineSpinner() {
        // Spinner 显示引擎名称，tag 关联 id
        val engines = SearchEngine.ALL
        val names = engines.map { getString(it.nameResId) }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            names
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.engineSpinner.adapter = adapter
        val currentId = viewModel.selectedEngineId()
        binding.engineSpinner.setSelection(
            engines.indexOfFirst { it.id == currentId }.coerceAtLeast(0)
        )
        binding.engineSpinner.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    viewModel.setSearchEngine(engines[position].id)
                }

                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }
    }

    private fun setupUaChips() {
        val mobileChip = chip(R.string.webview_ua_mobile)
        val desktopChip = chip(R.string.webview_ua_desktop)
        val currentUa = viewModel.defaultUserAgent()
        mobileChip.isChecked = currentUa == "mobile"
        desktopChip.isChecked = currentUa == "desktop"
        mobileChip.setOnClickListener {
            viewModel.setDefaultUserAgent("mobile")
            desktopChip.isChecked = false
        }
        desktopChip.setOnClickListener {
            viewModel.setDefaultUserAgent("desktop")
            mobileChip.isChecked = false
        }
        binding.uaChips.addView(mobileChip)
        binding.uaChips.addView(desktopChip)
    }

    private fun chip(textResId: Int): Chip = Chip(requireContext()).apply {
        text = getString(textResId)
        isCheckable = true
        setChipBackgroundColorResource(R.color.white)
    }

    private fun setupPrivacySwitches() {
        binding.swJs.isChecked = viewModel.isJavaScriptEnabled()
        binding.swJs.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setJavaScriptEnabled(isChecked)
        }

        binding.swCookie.isChecked = viewModel.isCookieEnabled()
        binding.swCookie.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setCookieEnabled(isChecked)
            // 关闭 Cookie 时联动关掉第三方 Cookie（避免设置矛盾）
            if (!isChecked) {
                binding.swThirdPartyCookie.isChecked = false
                viewModel.setThirdPartyCookieEnabled(false)
            }
        }

        binding.swThirdPartyCookie.isChecked = viewModel.isThirdPartyCookieEnabled()
        binding.swThirdPartyCookie.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setThirdPartyCookieEnabled(isChecked)
            // 开启第三方 Cookie 时，如果 Cookie 总开关关了，自动打开
            if (isChecked && !binding.swCookie.isChecked) {
                binding.swCookie.isChecked = true
                viewModel.setCookieEnabled(true)
            }
        }

        binding.swBlockMixed.isChecked = viewModel.isBlockMixedContent()
        binding.swBlockMixed.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setBlockMixedContent(isChecked)
        }
    }

    private fun setupDisplaySwitches() {
        binding.swNight.isChecked = viewModel.isNightMode()
        binding.swNight.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setNightMode(isChecked)
        }
    }

    private fun setupClearDataButton() {
        binding.btnClearData.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.settings_clear_browsing_data_title)
                .setMessage(R.string.settings_clear_browsing_data_message)
                .setPositiveButton(R.string.dialog_delete) { _, _ ->
                    viewModel.clearBrowsingData {
                        Toast.makeText(
                            requireContext(),
                            R.string.settings_clear_browsing_data_done,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .setNegativeButton(R.string.dialog_cancel, null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
