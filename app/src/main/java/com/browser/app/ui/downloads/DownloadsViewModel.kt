package com.browser.app.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.browser.app.data.entity.DownloadEntity
import com.browser.app.repository.DownloadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    val downloads: StateFlow<List<DownloadEntity>> = downloadRepository.getAllDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteDownload(entity: DownloadEntity) {
        viewModelScope.launch {
            downloadRepository.deleteRecord(entity.id)
        }
    }

    fun openDownloadIntent(entity: DownloadEntity, onResult: (android.content.Intent?) -> Unit) {
        onResult(downloadRepository.buildOpenIntent(entity))
    }
}
