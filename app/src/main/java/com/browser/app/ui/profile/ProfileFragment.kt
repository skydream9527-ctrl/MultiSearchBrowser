package com.browser.app.ui.profile

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.browser.app.R
import com.browser.app.databinding.FragmentProfileBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri -> handleSelectedImage(uri) }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openImagePicker()
        } else {
            Toast.makeText(requireContext(), "需要存储权限才能上传头像", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadAvatar()
        setupClickListeners()
    }

    private fun loadAvatar() {
        viewModel.avatarUriString()?.let { uriString ->
            try {
                val uri = Uri.parse(uriString)
                binding.avatarImage.setImageURI(uri)
            } catch (e: Exception) {
                binding.avatarImage.setImageResource(R.drawable.ic_avatar_placeholder)
            }
        }
    }

    private fun setupClickListeners() {
        binding.uploadAvatarBtn.setOnClickListener { checkPermissionAndPickImage() }
        binding.avatarImage.setOnClickListener { checkPermissionAndPickImage() }
        binding.historySection.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_historyFragment)
        }
        binding.bookmarksSection.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_bookmarksFragment)
        }
    }

    private fun checkPermissionAndPickImage() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (ContextCompat.checkSelfPermission(requireContext(), permission) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            openImagePicker()
        } else {
            requestPermissionLauncher.launch(permission)
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }

    private fun handleSelectedImage(uri: Uri) {
        try {
            requireContext().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: Exception) {
            // 忽略：部分 Provider 不允许持久化授权
        }
        viewModel.saveAvatarUri(uri.toString())
        binding.avatarImage.setImageURI(uri)
        Toast.makeText(requireContext(), R.string.avatar_uploaded, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
