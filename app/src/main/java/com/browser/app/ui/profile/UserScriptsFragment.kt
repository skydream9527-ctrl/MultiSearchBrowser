package com.browser.app.ui.profile

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
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
import com.browser.app.data.entity.UserScriptEntity
import com.browser.app.databinding.FragmentUserScriptsBinding
import com.browser.app.databinding.ItemUserScriptBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UserScriptsFragment : Fragment() {
    private var _binding: FragmentUserScriptsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: UserScriptsViewModel by viewModels()
    private lateinit var adapter: UserScriptAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserScriptsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeScripts()
        setupAddButton()
        setupBackButton()
    }

    private fun setupRecyclerView() {
        adapter = UserScriptAdapter(
            onLongClick = { script ->
                showToggleDialog(script)
            }
        )
        binding.userScriptsList.layoutManager = LinearLayoutManager(requireContext())
        binding.userScriptsList.adapter = adapter
    }

    private fun observeScripts() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.scripts.collect { scripts ->
                    adapter.submitList(scripts)
                    binding.emptyText.visibility = if (scripts.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun setupAddButton() {
        binding.btnAdd.setOnClickListener {
            showAddScriptDialog()
        }
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun showAddScriptDialog() {
        val context = requireContext()
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }
        val nameInput = EditText(context).apply {
            hint = getString(R.string.script_name_label)
            inputType = InputType.TYPE_CLASS_TEXT
        }
        val patternInput = EditText(context).apply {
            hint = getString(R.string.url_pattern_label)
            inputType = InputType.TYPE_CLASS_TEXT
        }
        val codeInput = EditText(context).apply {
            hint = getString(R.string.script_code_label)
            inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            minLines = 3
        }
        container.addView(nameInput)
        container.addView(patternInput)
        container.addView(codeInput)
        AlertDialog.Builder(context)
            .setTitle(R.string.add_user_script)
            .setView(container)
            .setPositiveButton(R.string.action_clear) { _, _ ->
                val name = nameInput.text.toString().trim()
                val pattern = patternInput.text.toString().trim()
                val code = codeInput.text.toString()
                if (name.isEmpty() || code.isEmpty()) {
                    Toast.makeText(context, R.string.input_search_query, Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.addScript(name, pattern.ifEmpty { ".*" }, code)
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun showToggleDialog(script: UserScriptEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.toggle_enabled)
            .setMessage(R.string.toggle_enabled)
            .setPositiveButton(R.string.action_clear) { _, _ ->
                viewModel.toggleEnabled(script)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class UserScriptAdapter(
    private val onLongClick: (UserScriptEntity) -> Unit
) : ListAdapter<UserScriptEntity, UserScriptAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUserScriptBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemUserScriptBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(script: UserScriptEntity) {
            binding.name.text = script.name
            binding.pattern.text = script.pattern
            binding.enabled.text = if (script.enabled) {
                itemView.context.getString(R.string.toggle_enabled)
            } else {
                itemView.context.getString(R.string.action_cancel)
            }
            binding.root.setOnLongClickListener {
                onLongClick(script)
                true
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<UserScriptEntity>() {
            override fun areItemsTheSame(oldItem: UserScriptEntity, newItem: UserScriptEntity) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: UserScriptEntity, newItem: UserScriptEntity) =
                oldItem == newItem
        }
    }
}
