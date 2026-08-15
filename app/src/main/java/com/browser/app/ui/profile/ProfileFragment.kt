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
import androidx.navigation.fragment.findNavController
import coil.load
import com.browser.app.ui.profile.ProfileFragmentDirections
import com.browser.app.R
import com.browser.app.databinding.FragmentProfileBinding
import com.browser.app.utils.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    @Inject lateinit var preferenceManager: PreferenceManager

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleSelectedImage(uri)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openImagePicker()
        } else {
            Toast.makeText(requireContext(), R.string.storage_permission_required, Toast.LENGTH_SHORT).show()
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
        val uriString = preferenceManager.avatarUri
        if (uriString.isNullOrEmpty()) {
            binding.avatarImage.setImageResource(R.drawable.ic_avatar_placeholder)
            return
        }
        binding.avatarImage.load(Uri.parse(uriString)) {
            crossfade(true)
            placeholder(R.drawable.ic_avatar_placeholder)
            error(R.drawable.ic_avatar_placeholder)
        }
    }

    private fun setupClickListeners() {
        binding.uploadAvatarBtn.setOnClickListener {
            checkPermissionAndPickImage()
        }

        binding.avatarImage.setOnClickListener {
            checkPermissionAndPickImage()
        }

        binding.historySection.setOnClickListener {
            findNavController().navigate(ProfileFragmentDirections.actionProfileFragmentToHistoryFragment())
        }

        binding.bookmarksSection.setOnClickListener {
            findNavController().navigate(ProfileFragmentDirections.actionProfileFragmentToBookmarksFragment())
        }

        binding.rssSection.setOnClickListener {
            findNavController().navigate(ProfileFragmentDirections.actionProfileFragmentToRssFragment())
        }

        binding.notesSection.setOnClickListener {
            findNavController().navigate(ProfileFragmentDirections.actionProfileFragmentToNotesFragment())
        }

        binding.passwordsSection.setOnClickListener {
            findNavController().navigate(ProfileFragmentDirections.actionProfileFragmentToPasswordsFragment())
        }

        binding.userScriptsSection.setOnClickListener {
            findNavController().navigate(ProfileFragmentDirections.actionProfileFragmentToUserScriptsFragment())
        }

        binding.statsSection.setOnClickListener {
            findNavController().navigate(ProfileFragmentDirections.actionProfileFragmentToStatsFragment())
        }

        binding.settingsSection.setOnClickListener {
            Toast.makeText(requireContext(), R.string.settings_coming_soon, Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPermissionAndPickImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                openImagePicker()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                openImagePicker()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
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
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            // Ignore
        }

        preferenceManager.avatarUri = uri.toString()
        binding.avatarImage.load(uri) {
            crossfade(true)
            placeholder(R.drawable.ic_avatar_placeholder)
            error(R.drawable.ic_avatar_placeholder)
        }
        Toast.makeText(requireContext(), R.string.avatar_uploaded, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
