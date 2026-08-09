package com.browser.app.ui.profile

import androidx.lifecycle.ViewModel
import com.browser.app.utils.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    fun avatarUriString(): String? = preferenceManager.avatarUri

    fun saveAvatarUri(uri: String) {
        preferenceManager.avatarUri = uri
    }
}
