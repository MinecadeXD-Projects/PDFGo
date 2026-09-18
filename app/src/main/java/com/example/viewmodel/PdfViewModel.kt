package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.DownloadModalState
import com.example.model.FitMode
import com.example.model.PageSpacing
import com.example.model.PdfDocument
import com.example.model.Screen
import com.example.model.SearchState
import com.example.model.ToastMessage
import com.example.model.ToastType
import com.example.ui.theme.AppThemeSetting
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PdfViewModel : ViewModel() {

  private val _currentScreen = MutableStateFlow(Screen.HOME)
  val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

  private val _activeDocument = MutableStateFlow<PdfDocument?>(null)
  val activeDocument: StateFlow<PdfDocument?> = _activeDocument.asStateFlow()

  private val _urlInput = MutableStateFlow("https://arxiv.org/pdf/2402.quantum_mechanics.pdf")
  val urlInput: StateFlow<String> = _urlInput.asStateFlow()

  private val _urlError = MutableStateFlow<String?>(null)
  val urlError: StateFlow<String?> = _urlError.asStateFlow()

  private val _loadingProgress = MutableStateFlow(0f)
  val loadingProgress: StateFlow<Float> = _loadingProgress.asStateFlow()

  // Settings
  private val _themeSetting = MutableStateFlow(AppThemeSetting.DARK)
  val themeSetting: StateFlow<AppThemeSetting> = _themeSetting.asStateFlow()

  private val _fitMode = MutableStateFlow(FitMode.FIT_WIDTH)
  val fitMode: StateFlow<FitMode> = _fitMode.asStateFlow()

  private val _pageSpacing = MutableStateFlow(PageSpacing.NORMAL)
  val pageSpacing: StateFlow<PageSpacing> = _pageSpacing.asStateFlow()

  private val _keepScreenAwake = MutableStateFlow(true)
  val keepScreenAwake: StateFlow<Boolean> = _keepScreenAwake.asStateFlow()

  private val _saveReadingPosition = MutableStateFlow(true)
  val saveReadingPosition: StateFlow<Boolean> = _saveReadingPosition.asStateFlow()

  // Reader state
  private val _isFullscreen = MutableStateFlow(false)
  val isFullscreen: StateFlow<Boolean> = _isFullscreen.asStateFlow()

  private val _searchState = MutableStateFlow(SearchState())
  val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

  // Modals
  private val _downloadState = MutableStateFlow(DownloadModalState())
  val downloadState: StateFlow<DownloadModalState> = _downloadState.asStateFlow()

  private val _isRemoveModalOpen = MutableStateFlow(false)
  val isRemoveModalOpen: StateFlow<Boolean> = _isRemoveModalOpen.asStateFlow()

  private val _isExitModalOpen = MutableStateFlow(false)
  val isExitModalOpen: StateFlow<Boolean> = _isExitModalOpen.asStateFlow()

  private val _isPasswordModalOpen = MutableStateFlow(false)
  val isPasswordModalOpen: StateFlow<Boolean> = _isPasswordModalOpen.asStateFlow()

  // Toast
  private val _toastMessage = MutableStateFlow<ToastMessage?>(null)
  val toastMessage: StateFlow<ToastMessage?> = _toastMessage.asStateFlow()

  private var loadJob: Job? = null
  private var downloadJob: Job? = null

  fun onUrlChange(newVal: String) {
    _urlInput.value = newVal
    _urlError.value = null
  }

  fun pasteUrl(text: String) {
    _urlInput.value = text.trim()
    _urlError.value = null
    showToast("Pasted link from clipboard", ToastType.INFO)
  }

  fun attemptOpenPdf() {
    val raw = _urlInput.value.trim()
    if (raw.isEmpty() || (!raw.startsWith("http://") && !raw.startsWith("https://"))) {
      _urlError.value = "Enter a valid PDF link starting with http:// or https://"
      return
    }

    if (raw.contains("protected", ignoreCase = true)) {
      _isPasswordModalOpen.value = true
      return
    }

    startLoadingPdf(raw)
  }

  private fun startLoadingPdf(url: String) {
    _currentScreen.value = Screen.LOADING
    _loadingProgress.value = 0.15f

    loadJob?.cancel()
    loadJob = viewModelScope.launch {
      delay(300)
      _loadingProgress.value = 0.65f
      delay(400)
      _loadingProgress.value = 1.0f
      delay(250)

      val filename = extractFilename(url)
      _activeDocument.value = PdfDocument(
        url = url,
        title = filename,
        totalPages = 84,
        currentPage = 12,
        zoomPercent = 100
      )
      _currentScreen.value = Screen.READER
      showToast("PDF loaded successfully", ToastType.SUCCESS)
    }
  }

  fun cancelLoading() {
    loadJob?.cancel()
    _loadingProgress.value = 0f
    _currentScreen.value = Screen.HOME
    showToast("Opening cancelled", ToastType.INFO)
  }

  fun unlockPasswordProtected(password: String) {
    _isPasswordModalOpen.value = false
    startLoadingPdf(_urlInput.value.trim())
  }

  fun closePasswordModal() {
    _isPasswordModalOpen.value = false
  }

  private fun extractFilename(url: String): String {
    return try {
      val lastSegment = url.substringAfterLast('/').substringBefore('?').substringBefore('#')
      if (lastSegment.isNotBlank()) {
        if (!lastSegment.endsWith(".pdf", ignoreCase = true)) "$lastSegment.pdf" else lastSegment
      } else {
        "Document.pdf"
      }
    } catch (_: Exception) {
      "Document.pdf"
    }
  }

  // Navigation
  fun navigateTo(screen: Screen) {
    _currentScreen.value = screen
  }

  fun navigateBackFromSettings() {
    if (_activeDocument.value != null) {
      _currentScreen.value = Screen.READER
    } else {
      _currentScreen.value = Screen.HOME
    }
  }

  fun navigateBackFromReader() {
    _currentScreen.value = Screen.HOME
    showToast("Saved active reading state", ToastType.INFO)
  }

  // Settings modification
  fun setTheme(setting: AppThemeSetting) {
    _themeSetting.value = setting
  }

  fun setFitMode(mode: FitMode) {
    _fitMode.value = mode
    showToast("Mode: ${mode.label}", ToastType.INFO)
  }

  fun toggleFitMode() {
    val next = if (_fitMode.value == FitMode.FIT_WIDTH) FitMode.FIT_PAGE else FitMode.FIT_WIDTH
    setFitMode(next)
  }

  fun setPageSpacing(spacing: PageSpacing) {
    _pageSpacing.value = spacing
  }

  fun toggleKeepScreenAwake() {
    _keepScreenAwake.value = !_keepScreenAwake.value
  }

  fun toggleSaveReadingPosition() {
    _saveReadingPosition.value = !_saveReadingPosition.value
  }

  // Reader Controls
  fun toggleFullscreen() {
    val newState = !_isFullscreen.value
    _isFullscreen.value = newState
    if (newState) {
      showToast("PDFGo Fullscreen active (Tap pill to exit)", ToastType.INFO)
    }
  }

  fun exitFullscreen() {
    _isFullscreen.value = false
  }

  fun changePage(delta: Int) {
    val doc = _activeDocument.value ?: return
    val newPage = (doc.currentPage + delta).coerceIn(1, doc.totalPages)
    _activeDocument.value = doc.copy(currentPage = newPage)
  }

  fun setPage(targetPage: Int) {
    val doc = _activeDocument.value ?: return
    val newPage = targetPage.coerceIn(1, doc.totalPages)
    _activeDocument.value = doc.copy(currentPage = newPage)
  }

  fun adjustZoom(delta: Int) {
    val doc = _activeDocument.value ?: return
    val newZoom = (doc.zoomPercent + delta).coerceIn(60, 200)
    _activeDocument.value = doc.copy(zoomPercent = newZoom)
  }

  // Search
  fun openSearch() {
    _searchState.value = SearchState(
      isOpen = true,
      query = "quantum",
      currentMatchIndex = 3,
      totalMatches = 18,
      specialNotice = null
    )
  }

  fun closeSearch() {
    _searchState.value = SearchState(isOpen = false)
  }

  fun onSearchQueryChange(newQuery: String) {
    val trimmed = newQuery.trim().lowercase()
    when {
      trimmed == "quantum" -> {
        _searchState.value = _searchState.value.copy(
          query = newQuery,
          currentMatchIndex = 3,
          totalMatches = 18,
          specialNotice = null
        )
      }
      trimmed == "scanned" -> {
        _searchState.value = _searchState.value.copy(
          query = newQuery,
          currentMatchIndex = 0,
          totalMatches = 0,
          specialNotice = "Text search isn't available for this PDF"
        )
      }
      trimmed.isEmpty() -> {
        _searchState.value = _searchState.value.copy(
          query = newQuery,
          currentMatchIndex = 0,
          totalMatches = 0,
          specialNotice = null
        )
      }
      else -> {
        _searchState.value = _searchState.value.copy(
          query = newQuery,
          currentMatchIndex = 0,
          totalMatches = 0,
          specialNotice = "No matches found"
        )
      }
    }
  }

  fun navigateSearch(step: Int) {
    val state = _searchState.value
    if (state.totalMatches <= 0) return
    var next = state.currentMatchIndex + step
    if (next > state.totalMatches) next = 1
    if (next < 1) next = state.totalMatches
    _searchState.value = state.copy(currentMatchIndex = next)
  }

  fun dismissSearchNotice() {
    _searchState.value = _searchState.value.copy(specialNotice = null)
  }

  // Download Dialog
  fun openDownloadModal() {
    val doc = _activeDocument.value
    val filename = doc?.title ?: "Physics_Lecture_01.pdf"
    _downloadState.value = DownloadModalState(
      isOpen = true,
      filename = filename,
      isDownloading = false,
      progressPercent = 0,
      progressBytes = "",
      isCompleted = false
    )
  }

  fun closeDownloadModal() {
    downloadJob?.cancel()
    _downloadState.value = DownloadModalState(isOpen = false)
  }

  fun onDownloadFilenameChange(newName: String) {
    _downloadState.value = _downloadState.value.copy(filename = newName)
  }

  fun startDownload() {
    downloadJob?.cancel()
    downloadJob = viewModelScope.launch {
      _downloadState.update {
        it.copy(
          isDownloading = true,
          progressPercent = 32,
          progressBytes = "2.1 / 6.5 MB"
        )
      }
      delay(450)
      _downloadState.update {
        it.copy(
          progressPercent = 64,
          progressBytes = "4.2 / 6.5 MB"
        )
      }
      delay(550)
      _downloadState.update {
        it.copy(
          progressPercent = 100,
          progressBytes = "Saved to Downloads/PDFGo/",
          isCompleted = true
        )
      }
      delay(700)
      _downloadState.value = DownloadModalState(isOpen = false)
      showToast("Saved to Downloads/PDFGo/", ToastType.SUCCESS)
    }
  }

  // Remove PDF
  fun openRemovePdfModal() {
    _isRemoveModalOpen.value = true
  }

  fun closeRemovePdfModal() {
    _isRemoveModalOpen.value = false
  }

  fun executeRemovePdf() {
    _isRemoveModalOpen.value = false
    _activeDocument.value = null
    _urlInput.value = ""
    _isFullscreen.value = false
    _searchState.value = SearchState()
    _currentScreen.value = Screen.HOME
    showToast("PDF removed from PDFGo", ToastType.INFO)
  }

  // Exit App
  fun openExitModal() {
    _isExitModalOpen.value = true
  }

  fun closeExitModal() {
    _isExitModalOpen.value = false
  }

  // Toast
  fun showToast(msg: String, type: ToastType) {
    _toastMessage.value = ToastMessage(text = msg, type = type)
    viewModelScope.launch {
      delay(2600)
      if (_toastMessage.value?.text == msg) {
        _toastMessage.value = null
      }
    }
  }

  fun dismissToast() {
    _toastMessage.value = null
  }

  // System Back Pressed Handling
  fun handleBack(): Boolean {
    // Return true if handled internally, false if activity should finish
    if (_downloadState.value.isOpen) {
      closeDownloadModal()
      return true
    }
    if (_isRemoveModalOpen.value) {
      closeRemovePdfModal()
      return true
    }
    if (_isExitModalOpen.value) {
      closeExitModal()
      return true
    }
    if (_isPasswordModalOpen.value) {
      closePasswordModal()
      return true
    }
    if (_currentScreen.value == Screen.READER && _isFullscreen.value) {
      exitFullscreen()
      return true
    }
    if (_currentScreen.value == Screen.READER && _searchState.value.isOpen) {
      closeSearch()
      return true
    }
    if (_currentScreen.value == Screen.SETTINGS) {
      navigateBackFromSettings()
      return true
    }
    if (_currentScreen.value == Screen.READER) {
      navigateBackFromReader()
      return true
    }
    if (_currentScreen.value == Screen.LOADING) {
      cancelLoading()
      return true
    }
    if (_currentScreen.value == Screen.HOME) {
      openExitModal()
      return true
    }
    return false
  }
}
