package com.browser.app.ui.profile

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.browser.app.R
import com.browser.app.data.entity.PasswordEntity
import com.browser.app.databinding.FragmentPasswordsBinding
import com.browser.app.databinding.ItemPasswordBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PasswordsFragment : Fragment() {
    private var _binding: FragmentPasswordsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PasswordsViewModel by viewModels()
    private lateinit var adapter: PasswordAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPasswordsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observePasswords()
        setupAddButton()
        setupBackButton()
    }

    private fun setupRecyclerView() {
        adapter = PasswordAdapter(
            onItemClick = { password ->
                showPasswordDialog(password)
            }
        )
        binding.passwordsList.layoutManager = LinearLayoutManager(requireContext())
        binding.passwordsList.adapter = adapter
    }

    private fun observePasswords() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.passwords.collect { passwords ->
                    adapter.submitList(passwords)
                    binding.emptyText.visibility = if (passwords.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun setupAddButton() {
        binding.btnAdd.setOnClickListener {
            showAddPasswordDialog()
        }
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun showAddPasswordDialog() {
        val context = requireContext()
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }
        val siteInput = EditText(context).apply {
            hint = getString(R.string.site_label)
            inputType = InputType.TYPE_CLASS_TEXT
        }
        val usernameInput = EditText(context).apply {
            hint = getString(R.string.username_label)
            inputType = InputType.TYPE_CLASS_TEXT
        }
        val passwordInput = EditText(context).apply {
            hint = getString(R.string.password_label)
            inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        container.addView(siteInput)
        container.addView(usernameInput)
        container.addView(passwordInput)
        AlertDialog.Builder(context)
            .setTitle(R.string.add_password)
            .setView(container)
            .setPositiveButton(R.string.action_clear) { _, _ ->
                val site = siteInput.text.toString().trim()
                val username = usernameInput.text.toString().trim()
                val pwd = passwordInput.text.toString()
                if (site.isEmpty() || username.isEmpty() || pwd.isEmpty()) {
                    Toast.makeText(context, R.string.input_search_query, Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.addPassword(site, username, pwd, site)
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun showPasswordDialog(password: PasswordEntity) {
        val context = requireContext()
        // 通过 getByUrl 重新拉取明文密码，避免显示过期的列表数据
        viewLifecycleOwner.lifecycleScope.launch {
            val entity = if (password.url.isNotEmpty()) {
                viewModel.getPasswordByUrl(password.url)
            } else {
                password
            }
            val plain = entity?.encryptedPassword ?: ""
            val message = "${getString(R.string.site_label)}: ${password.site}\n" +
                    "${getString(R.string.username_label)}: ${password.username}\n" +
                    "${getString(R.string.password_label)}: $plain"
            val textView = TextView(context).apply {
                text = message
                textSize = 14f
                setPadding(48, 24, 48, 24)
                setTextIsSelectable(true)
            }
            AlertDialog.Builder(context)
                .setTitle(R.string.passwords_title)
                .setView(textView)
                .setPositiveButton(R.string.action_cancel, null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class PasswordAdapter(
    private val onItemClick: (PasswordEntity) -> Unit
) : ListAdapter<PasswordEntity, PasswordAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPasswordBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemPasswordBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(password: PasswordEntity) {
            binding.site.text = password.site
            binding.username.text = password.username
            binding.timestamp.text = formatTimestamp(password.timestamp)
            // 不显示密码内容
            binding.root.setOnClickListener { onItemClick(password) }
        }

        private fun formatTimestamp(timestamp: Long): String {
            if (timestamp <= 0L) return ""
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
            return sdf.format(java.util.Date(timestamp))
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<PasswordEntity>() {
            override fun areItemsTheSame(oldItem: PasswordEntity, newItem: PasswordEntity) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: PasswordEntity, newItem: PasswordEntity) =
                oldItem == newItem
        }
    }
}
