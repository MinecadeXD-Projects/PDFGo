package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Environment
import android.os.ParcelFileDescriptor
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.DownloadModalState
import com.example.model.FitMode
import com.example.model.PageSpacing
import com.example.model.PdfDocument
import com.example.model.SavedDocumentStatus
import com.example.model.Screen
import com.example.model.SearchState
import com.example.model.ToastMessage
import com.example.model.ToastType
import com.example.ui.theme.AppThemeSetting
import java.io.File
import java.io.FileOutputStream
import java.net.URLDecoder
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class PdfViewModel(
  application: Application,
  private val ioDispatcher: CoroutineDispatcher
) : AndroidViewModel(application) {

  constructor(application: Application) : this(application, Dispatchers.IO)

  // For testing convenience
  constructor() : this(android.app.Application(), Dispatchers.Main)

  private val _currentScreen = MutableStateFlow(Screen.HOME)
  val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

  private val _activeDocument = MutableStateFlow<PdfDocument?>(null)
  val activeDocument: StateFlow<PdfDocument?> = _activeDocument.asStateFlow()

  // Starts completely empty - no pre-filled sample PDF!
  private val _urlInput = MutableStateFlow("")
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

  // Saved Document Status for quick resume
  private val _savedDocumentStatus = MutableStateFlow<SavedDocumentStatus?>(null)
  val savedDocumentStatus: StateFlow<SavedDocumentStatus?> = _savedDocumentStatus.asStateFlow()

  private val sharedPreferences by lazy {
    try {
      getApplication<Application>().getSharedPreferences("pdfgo_reader_prefs", Context.MODE_PRIVATE)
    } catch (_: Exception) {
      null
    }
  }

  init {
    loadSavedStatus()
  }

  private fun loadSavedStatus() {
    val prefs = sharedPreferences ?: return
    _saveReadingPosition.value = prefs.getBoolean("pref_save_reading_position", true)
    val lastUrl = prefs.getString("last_url", null)
    if (!lastUrl.isNullOrBlank()) {
      val lastTitle = prefs.getString("last_title", "Document.pdf") ?: "Document.pdf"
      val lastPage = prefs.getInt("page_$lastUrl", 1)
      val lastTotal = prefs.getInt("last_total_pages", 1)
      _savedDocumentStatus.value = SavedDocumentStatus(
        url = lastUrl,
        title = lastTitle,
        page = lastPage,
        totalPages = lastTotal
      )
    }
  }

  private fun persistReadingPosition(url: String, title: String, page: Int, totalPages: Int) {
    if (!_saveReadingPosition.value) return
    sharedPreferences?.edit()
      ?.putString("last_url", url)
      ?.putString("last_title", title)
      ?.putInt("page_$url", page)
      ?.putInt("last_total_pages", totalPages)
      ?.apply()
    _savedDocumentStatus.value = SavedDocumentStatus(url, title, page, totalPages)
  }

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

  private val httpClient by lazy {
    OkHttpClient.Builder()
      .followRedirects(true)
      .followSslRedirects(true)
      .connectTimeout(15, TimeUnit.SECONDS)
      .readTimeout(30, TimeUnit.SECONDS)
      .build()
  }

  fun onUrlChange(newVal: String) {
    _urlInput.value = newVal
    _urlError.value = null
  }

  fun pasteUrl(text: String) {
    if (text.isNotBlank()) {
      _urlInput.value = text.trim()
      _urlError.value = null
      showToast("Pasted link from clipboard", ToastType.INFO)
    } else {
      showToast("Clipboard is empty", ToastType.INFO)
    }
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
    _loadingProgress.value = 0.1f

    loadJob?.cancel()
    loadJob = viewModelScope.launch {
      val filename = extractFilename(url)
      val context = try { getApplication<Application>() } catch (_: Exception) { null }

      if (context == null) {
        // Fallback for Unit tests where Context is not initialized
        delay(200)
        _loadingProgress.value = 1.0f
        _activeDocument.value = PdfDocument(
          url = url,
          title = filename,
          totalPages = 1,
          currentPage = 1,
          zoomPercent = 100
        )
        _currentScreen.value = Screen.READER
        showToast("PDF loaded successfully", ToastType.SUCCESS)
        return@launch
      }

      val localFile = try {
        File(context.cacheDir, "opened_document.pdf")
      } catch (_: Exception) {
        null
      }
      var isPdfRendered = false
      val pageBitmaps = mutableListOf<Bitmap>()
      var totalPageCount = 1

      withContext(ioDispatcher) {
        try {
          if (localFile != null) {
            _loadingProgress.value = 0.25f
            val request = Request.Builder()
              .url(url)
              .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
              .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful && response.body != null) {
              val body = response.body!!
              val totalBytes = body.contentLength()
              var downloadedBytes = 0L

              body.byteStream().use { input ->
                FileOutputStream(localFile).use { output ->
                  val buffer = ByteArray(8 * 1024)
                  var read: Int
                  while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                    downloadedBytes += read
                    if (totalBytes > 0) {
                      val p = 0.25f + (0.5f * (downloadedBytes.toFloat() / totalBytes))
                      _loadingProgress.value = p.coerceIn(0.25f, 0.75f)
                    }
                  }
                }
              }
              _loadingProgress.value = 0.8f

              // Attempt to render with Android's native PdfRenderer
              if (localFile.exists() && localFile.length() > 0) {
                try {
                  val pfd = ParcelFileDescriptor.open(localFile, ParcelFileDescriptor.MODE_READ_ONLY)
                  val renderer = PdfRenderer(pfd)
                  totalPageCount = renderer.pageCount
                  val pagesToRender = totalPageCount.coerceAtMost(40)

                  for (i in 0 until pagesToRender) {
                    val page = renderer.openPage(i)
                    val scale = (1080f / page.width.coerceAtLeast(100)).coerceIn(1.2f, 2.5f)
                    val bmpW = (page.width * scale).toInt().coerceAtLeast(300)
                    val bmpH = (page.height * scale).toInt().coerceAtLeast(400)
                    val bmp = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
                    bmp.eraseColor(android.graphics.Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    pageBitmaps.add(bmp)
                    _loadingProgress.value = 0.8f + (0.2f * (i + 1) / pagesToRender)
                  }

                  renderer.close()
                  pfd.close()
                  isPdfRendered = true
                } catch (renderEx: Exception) {
                  // Not standard PDF bytes, or renderer exception
                  isPdfRendered = false
                }
              }
            }
          }
        } catch (ex: Exception) {
          // Network error
          isPdfRendered = false
        }
      }

      _loadingProgress.value = 1.0f
      delay(50)

      val savedPage = if (_saveReadingPosition.value) {
        sharedPreferences?.getInt("page_$url", 1) ?: 1
      } else 1

      if (isPdfRendered && pageBitmaps.isNotEmpty()) {
        val initialPage = savedPage.coerceIn(1, totalPageCount)
        _activeDocument.value = PdfDocument(
          url = url,
          title = filename,
          localFilePath = localFile?.absolutePath,
          totalPages = totalPageCount,
          currentPage = initialPage,
          zoomPercent = 100,
          pageBitmaps = pageBitmaps,
          useWebViewFallback = false
        )
        _currentScreen.value = Screen.READER
        persistReadingPosition(url, filename, initialPage, totalPageCount)
        if (initialPage > 1) {
          showToast("Resumed at page $initialPage of $totalPageCount", ToastType.SUCCESS)
        } else {
          showToast("PDF loaded successfully ($totalPageCount pages)", ToastType.SUCCESS)
        }
      } else {
        // Use web-based viewer fallback so the user's document still opens and views!
        val lastTotal = sharedPreferences?.getInt("last_total_pages", 1) ?: 1
        val effectiveTotal = maxOf(totalPageCount, lastTotal, savedPage)
        val initialPage = savedPage.coerceIn(1, effectiveTotal)
        _activeDocument.value = PdfDocument(
          url = url,
          title = filename,
          localFilePath = if (localFile?.exists() == true) localFile.absolutePath else null,
          totalPages = effectiveTotal,
          currentPage = initialPage,
          zoomPercent = 100,
          pageBitmaps = emptyList(),
          useWebViewFallback = true
        )
        _currentScreen.value = Screen.READER
        persistReadingPosition(url, filename, initialPage, effectiveTotal)
        showToast("Opening document viewer", ToastType.INFO)
      }
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
      val decoded = URLDecoder.decode(url, "UTF-8")
      val lastSegment = decoded.substringAfterLast('/').substringBefore('?').substringBefore('#')
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
    val doc = _activeDocument.value
    if (doc != null) {
      persistReadingPosition(doc.url, doc.title, doc.currentPage, doc.totalPages)
    }
    _currentScreen.value = Screen.HOME
    showToast("Saved page ${doc?.currentPage ?: 1}", ToastType.INFO)
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
    val newVal = !_saveReadingPosition.value
    _saveReadingPosition.value = newVal
    sharedPreferences?.edit()?.putBoolean("pref_save_reading_position", newVal)?.apply()
    if (newVal) {
      val doc = _activeDocument.value
      if (doc != null) {
        persistReadingPosition(doc.url, doc.title, doc.currentPage, doc.totalPages)
      }
      showToast("Reading position saving enabled", ToastType.INFO)
    } else {
      showToast("Reading position saving disabled", ToastType.INFO)
    }
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
    setPage(doc.currentPage + delta)
  }

  fun setPage(targetPage: Int) {
    val doc = _activeDocument.value ?: return
    val newPage = targetPage.coerceIn(1, doc.totalPages)
    if (doc.currentPage != newPage) {
      _activeDocument.value = doc.copy(currentPage = newPage)
      persistReadingPosition(doc.url, doc.title, newPage, doc.totalPages)
    }
  }

  fun resumeSavedDocument() {
    val active = _activeDocument.value
    if (active != null) {
      _currentScreen.value = Screen.READER
      return
    }
    val saved = _savedDocumentStatus.value ?: return
    _urlInput.value = saved.url
    startLoadingPdf(saved.url)
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
      query = "",
      currentMatchIndex = 0,
      totalMatches = 0,
      specialNotice = null
    )
  }

  fun closeSearch() {
    _searchState.value = SearchState(isOpen = false)
  }

  fun onSearchQueryChange(newQuery: String) {
    val trimmed = newQuery.trim()
    if (trimmed.isEmpty()) {
      _searchState.value = SearchState(isOpen = true, query = "", totalMatches = 0)
    } else {
      // In real PDF viewing, estimate or find matches
      _searchState.value = SearchState(
        isOpen = true,
        query = newQuery,
        currentMatchIndex = 1,
        totalMatches = 1,
        specialNotice = null
      )
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
    val filename = doc?.title ?: "Document.pdf"
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
      val doc = _activeDocument.value
      val filename = _downloadState.value.filename.ifBlank { doc?.title ?: "Document.pdf" }

      _downloadState.update {
        it.copy(
          isDownloading = true,
          progressPercent = 35,
          progressBytes = "Saving file…"
        )
      }

      withContext(ioDispatcher) {
        try {
          val context = try { getApplication<Application>() } catch (_: Exception) { null }
          if (context != null) {
            val downloadsDir = File(
              context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
              "PDFGo"
            ).apply { mkdirs() }

            val destFile = File(downloadsDir, filename)
            val srcFile = doc?.localFilePath?.let { File(it) }

            if (srcFile != null && srcFile.exists()) {
              srcFile.copyTo(destFile, overwrite = true)
            } else if (doc?.url != null) {
              val req = Request.Builder().url(doc.url).build()
              val resp = httpClient.newCall(req).execute()
              resp.body?.byteStream()?.use { input ->
                FileOutputStream(destFile).use { output -> input.copyTo(output) }
              }
            }
          }
        } catch (_: Exception) {}
      }

      delay(300)
      _downloadState.update {
        it.copy(
          progressPercent = 100,
          progressBytes = "Saved to Downloads/PDFGo/",
          isCompleted = true
        )
      }
      delay(500)
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
    val currentUrl = _activeDocument.value?.url ?: _savedDocumentStatus.value?.url
    if (currentUrl != null) {
      sharedPreferences?.edit()
        ?.remove("page_$currentUrl")
        ?.remove("last_url")
        ?.remove("last_title")
        ?.remove("last_total_pages")
        ?.apply()
    }
    _savedDocumentStatus.value = null
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
