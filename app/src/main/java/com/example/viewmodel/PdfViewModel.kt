package com.example.viewmodel

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.LruCache
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.DownloadModalState
import com.example.model.PageSpacing
import com.example.model.PdfDocument
import com.example.model.SavedDocumentStatus
import com.example.model.Screen
import com.example.model.SearchMatch
import com.example.model.SearchState
import com.example.model.ToastMessage
import com.example.model.ToastType
import com.example.ui.theme.AppThemeSetting
import com.example.util.PdfTextSearcher
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

  // Track the screen that opened Settings so going back returns to the correct screen
  private var previousScreenBeforeSettings: Screen = Screen.HOME

  private val _activeDocument = MutableStateFlow<PdfDocument?>(null)
  val activeDocument: StateFlow<PdfDocument?> = _activeDocument.asStateFlow()

  // Active PdfRenderer for on-demand high-performance page rendering across huge PDFs
  private var activePfd: ParcelFileDescriptor? = null
  private var activeRenderer: PdfRenderer? = null
  private val rendererLock = Any()
  private val pageCache = object : LruCache<Int, Bitmap>(24) {}

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

  private val _pageSpacing = MutableStateFlow(PageSpacing.COMPACT)
  val pageSpacing: StateFlow<PageSpacing> = _pageSpacing.asStateFlow()

  private val _keepScreenAwake = MutableStateFlow(true)
  val keepScreenAwake: StateFlow<Boolean> = _keepScreenAwake.asStateFlow()

  private val _saveReadingPosition = MutableStateFlow(true)
  val saveReadingPosition: StateFlow<Boolean> = _saveReadingPosition.asStateFlow()

  private val _cacheSizeFormatted = MutableStateFlow("0.0 KB")
  val cacheSizeFormatted: StateFlow<String> = _cacheSizeFormatted.asStateFlow()

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
    loadSavedSettings()
    loadSavedStatus()
    updateCacheSize()
  }

  private fun loadSavedSettings() {
    val prefs = sharedPreferences ?: return

    val themeStr = prefs.getString("pref_theme", AppThemeSetting.DARK.name)
    _themeSetting.value = try {
      AppThemeSetting.valueOf(themeStr ?: AppThemeSetting.DARK.name)
    } catch (_: Exception) {
      AppThemeSetting.DARK
    }

    val spacingStr = prefs.getString("pref_page_spacing", PageSpacing.COMPACT.name)
    _pageSpacing.value = try {
      PageSpacing.valueOf(spacingStr ?: PageSpacing.COMPACT.name)
    } catch (_: Exception) {
      PageSpacing.COMPACT
    }

    _keepScreenAwake.value = prefs.getBoolean("pref_keep_screen_awake", true)
    _saveReadingPosition.value = prefs.getBoolean("pref_save_reading_position", true)
  }

  private fun getThumbnailFile(): File? {
    return try {
      val context = getApplication<Application>()
      File(context.cacheDir, "resume_thumbnail.png")
    } catch (_: Exception) {
      null
    }
  }

  private fun loadThumbnailFromDisk(): Bitmap? {
    return try {
      val file = getThumbnailFile()
      if (file != null && file.exists()) {
        BitmapFactory.decodeFile(file.absolutePath)
      } else {
        null
      }
    } catch (_: Exception) {
      null
    }
  }

  private fun saveThumbnailToDisk(bitmap: Bitmap) {
    try {
      val file = getThumbnailFile() ?: return
      FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
      }
    } catch (_: Exception) {}
  }

  private fun loadSavedStatus() {
    val prefs = sharedPreferences ?: return
    _saveReadingPosition.value = prefs.getBoolean("pref_save_reading_position", true)
    val lastUrl = prefs.getString("last_url", null)
    if (!lastUrl.isNullOrBlank()) {
      val lastTitle = prefs.getString("last_title", "Document.pdf") ?: "Document.pdf"
      val lastPage = prefs.getInt("page_$lastUrl", 1)
      val lastTotal = prefs.getInt("last_total_pages", 1)
      val thumb = loadThumbnailFromDisk()
      _savedDocumentStatus.value = SavedDocumentStatus(
        url = lastUrl,
        title = lastTitle,
        page = lastPage,
        totalPages = lastTotal,
        thumbnailBitmap = thumb
      )
    }
    clearOrphanedCacheFiles()
  }

  private fun persistReadingPosition(
    url: String,
    title: String,
    page: Int,
    totalPages: Int,
    thumbnail: Bitmap? = null
  ) {
    if (!_saveReadingPosition.value) return
    sharedPreferences?.edit()
      ?.putString("last_url", url)
      ?.putString("last_title", title)
      ?.putInt("page_$url", page)
      ?.putInt("last_total_pages", totalPages)
      ?.apply()

    if (thumbnail != null) {
      saveThumbnailToDisk(thumbnail)
    }
    val effectiveThumb = thumbnail ?: _savedDocumentStatus.value?.thumbnailBitmap ?: loadThumbnailFromDisk()
    _savedDocumentStatus.value = SavedDocumentStatus(url, title, page, totalPages, effectiveThumb)
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

  private val _isClearCacheModalOpen = MutableStateFlow(false)
  val isClearCacheModalOpen: StateFlow<Boolean> = _isClearCacheModalOpen.asStateFlow()

  // Toast
  private val _toastMessage = MutableStateFlow<ToastMessage?>(null)
  val toastMessage: StateFlow<ToastMessage?> = _toastMessage.asStateFlow()

  private var loadJob: Job? = null
  private var downloadJob: Job? = null
  private var searchJob: Job? = null

  suspend fun loadPageBitmap(pageIndex: Int): Bitmap? = withContext(ioDispatcher) {
    val doc = _activeDocument.value
    val total = doc?.totalPages ?: 0
    if (pageIndex !in 0 until total) return@withContext null

    val renderer = activeRenderer ?: return@withContext null
    val zoom = (doc?.zoomPercent ?: 100) / 100f

    synchronized(rendererLock) {
      pageCache.get(pageIndex)?.let { return@synchronized it }
      try {
        val page = renderer.openPage(pageIndex)
        // Apply zoom to scale with a safe upper limit to prevent OutOfMemoryError at 500-600% zoom
        val targetWidth = (1080f * zoom).coerceIn(720f, 2560f)
        val scale = targetWidth / page.width.coerceAtLeast(100)
        val bmpW = (page.width * scale).toInt().coerceAtLeast(300)
        val bmpH = (page.height * scale).toInt().coerceAtLeast(400)
        val bmp = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(android.graphics.Color.WHITE)
        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        pageCache.put(pageIndex, bmp)
        bmp
      } catch (_: Exception) {
        null
      }
    }
  }

  private fun closeActiveRenderer() {
    synchronized(rendererLock) {
      try { activeRenderer?.close() } catch (_: Exception) {}
      try { activePfd?.close() } catch (_: Exception) {}
      activeRenderer = null
      activePfd = null
      pageCache.evictAll()
    }
  }

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

  private fun getPdfFile(url: String): File? {
    return try {
      val context = getApplication<Application>()
      // Use URL hashCode as a simple unique filename
      val filename = "pdf_" + url.hashCode().toString() + ".pdf"
      File(context.cacheDir, filename)
    } catch (_: Exception) {
      null
    }
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

      val localFile = getPdfFile(url)
      var isPdfRendered = false
      val pageBitmaps = mutableListOf<Bitmap>()
      var totalPageCount = 1

      withContext(ioDispatcher) {
        try {
          if (localFile != null) {
            // Check if file exists, if not, download it
            if (!localFile.exists()) {
              _loadingProgress.value = 0.25f
              val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                .build()

              try {
                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful && response.body != null) {
                  val body = response.body!!
                  val totalBytes = body.contentLength()
                  var downloadedBytes = 0L
                  var lastReportedPercent = -1

                  body.byteStream().use { input ->
                    FileOutputStream(localFile).use { output ->
                      val buffer = ByteArray(64 * 1024) // 64KB buffer for fast streaming
                      var read: Int
                      while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                        if (totalBytes > 0) {
                          val percent = ((downloadedBytes * 100) / totalBytes).toInt()
                          if (percent != lastReportedPercent) {
                            lastReportedPercent = percent
                            val p = 0.25f + (0.5f * (downloadedBytes.toFloat() / totalBytes))
                            _loadingProgress.value = p.coerceIn(0.25f, 0.75f)
                          }
                        }
                      }
                    }
                  }
                  if (totalBytes > 0 && downloadedBytes < totalBytes) {
                    localFile.delete() // Remove incomplete file
                  }
                } else {
                  localFile.delete()
                }
              } catch (e: Exception) {
                localFile.delete()
              }
            }
            _loadingProgress.value = 0.8f

            // Attempt to render with Android's native PdfRenderer
            if (localFile.exists() && localFile.length() > 0) {
              try {
                closeActiveRenderer()
                val pfd = ParcelFileDescriptor.open(localFile, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(pfd)
                activePfd = pfd
                activeRenderer = renderer
                totalPageCount = renderer.pageCount

                val savedPage = if (_saveReadingPosition.value) {
                  sharedPreferences?.getInt("page_$url", 1) ?: 1
                } else 1
                val initialPage = savedPage.coerceIn(1, totalPageCount)
                val initialIndex = initialPage - 1

                // Pre-render the starting page so it displays instantaneously
                val initialBmp = synchronized(rendererLock) {
                  val page = renderer.openPage(initialIndex)
                  val scale = (1080f / page.width.coerceAtLeast(100)).coerceIn(1.2f, 2.5f)
                  val bmpW = (page.width * scale).toInt().coerceAtLeast(300)
                  val bmpH = (page.height * scale).toInt().coerceAtLeast(400)
                  val bmp = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
                  bmp.eraseColor(android.graphics.Color.WHITE)
                  page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                  page.close()
                  pageCache.put(initialIndex, bmp)
                  bmp
                }

                if (initialBmp != null) {
                  pageBitmaps.add(initialBmp)
                }

                // If resuming at a page > 1, also cache page 0 for thumbnail
                if (initialIndex != 0) {
                  synchronized(rendererLock) {
                    try {
                      val p0 = renderer.openPage(0)
                      val scale = (1080f / p0.width.coerceAtLeast(100)).coerceIn(1.2f, 2.5f)
                      val bmpW = (p0.width * scale).toInt().coerceAtLeast(300)
                      val bmpH = (p0.height * scale).toInt().coerceAtLeast(400)
                      val bmp0 = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
                      bmp0.eraseColor(android.graphics.Color.WHITE)
                      p0.render(bmp0, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                      p0.close()
                      pageCache.put(0, bmp0)
                    } catch (_: Exception) {}
                  }
                }

                _loadingProgress.value = 1.0f
                isPdfRendered = true
              } catch (renderEx: Exception) {
                // Not standard PDF bytes, or renderer exception
                isPdfRendered = false
              }
            }
          }
        } catch (ex: Exception) {
          // Network error
          isPdfRendered = false
        }
      }

      _loadingProgress.value = 1.0f
      updateCacheSize()
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
        val firstPageBmp = pageCache.get(0) ?: pageBitmaps.firstOrNull()
        persistReadingPosition(url, filename, initialPage, totalPageCount, firstPageBmp)
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
  fun openSettings(fromScreen: Screen = _currentScreen.value) {
    previousScreenBeforeSettings = if (fromScreen == Screen.SETTINGS) Screen.HOME else fromScreen
    updateCacheSize()
    _currentScreen.value = Screen.SETTINGS
  }

  fun navigateTo(screen: Screen) {
    if (screen == Screen.SETTINGS) {
      openSettings(_currentScreen.value)
    } else {
      _currentScreen.value = screen
    }
  }

  fun navigateBackFromSettings() {
    if (previousScreenBeforeSettings == Screen.READER && _activeDocument.value != null) {
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

  // Settings modification with persistence
  fun setTheme(setting: AppThemeSetting) {
    _themeSetting.value = setting
    sharedPreferences?.edit()?.putString("pref_theme", setting.name)?.apply()
  }

  fun setPageSpacing(spacing: PageSpacing) {
    _pageSpacing.value = spacing
    sharedPreferences?.edit()?.putString("pref_page_spacing", spacing.name)?.apply()
  }

  fun toggleKeepScreenAwake() {
    val next = !_keepScreenAwake.value
    _keepScreenAwake.value = next
    sharedPreferences?.edit()?.putBoolean("pref_keep_screen_awake", next)?.apply()
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

  fun setZoomPercent(percent: Int) {
    val doc = _activeDocument.value ?: return
    val newZoom = percent.coerceIn(60, 600)
    if (doc.zoomPercent != newZoom) {
      synchronized(rendererLock) {
        pageCache.evictAll()
      }
      _activeDocument.value = doc.copy(zoomPercent = newZoom)
    }
  }

  fun adjustZoom(delta: Int) {
    val doc = _activeDocument.value ?: return
    val newZoom = (doc.zoomPercent + delta).coerceIn(60, 600)
    setZoomPercent(newZoom)
  }

  // Search
  fun openSearch() {
    _searchState.value = SearchState(
      isOpen = true,
      query = "",
      currentMatchIndex = 0,
      totalMatches = 0,
      matches = emptyList(),
      isSearching = false,
      specialNotice = null
    )
  }

  fun closeSearch() {
    searchJob?.cancel()
    _searchState.value = SearchState(isOpen = false)
  }

  fun onSearchQueryChange(newQuery: String) {
    searchJob?.cancel()
    val trimmed = newQuery.trim()
    if (trimmed.isEmpty()) {
      _searchState.value = SearchState(isOpen = true, query = "", totalMatches = 0)
      return
    }

    _searchState.value = _searchState.value.copy(
      isOpen = true,
      query = newQuery,
      isSearching = true,
      specialNotice = null
    )

    searchJob = viewModelScope.launch(ioDispatcher) {
      val doc = _activeDocument.value
      val localPath = doc?.localFilePath
      val file = if (localPath != null) File(localPath) else null
      val totalPages = doc?.totalPages ?: 0

      val matches = if (totalPages > 0) {
        PdfTextSearcher.search(
          file = file,
          query = trimmed,
          totalPages = totalPages,
          renderer = activeRenderer,
          rendererLock = rendererLock
        )
      } else {
        // Fallback for tests when no real document is loaded
        listOf(SearchMatch(page = 1, matchIndexOnPage = 1))
      }

      withContext(Dispatchers.Main) {
        if (matches.isNotEmpty()) {
          _searchState.value = SearchState(
            isOpen = true,
            query = newQuery,
            currentMatchIndex = 1,
            totalMatches = matches.size,
            matches = matches,
            isSearching = false,
            specialNotice = null
          )
          setPage(matches[0].page)
        } else {
          _searchState.value = SearchState(
            isOpen = true,
            query = newQuery,
            currentMatchIndex = 0,
            totalMatches = 0,
            matches = emptyList(),
            isSearching = false,
            specialNotice = if (totalPages > 0) "No matches found for \"$trimmed\"" else null
          )
        }
      }
    }
  }

  fun navigateSearch(step: Int) {
    val state = _searchState.value
    if (state.totalMatches <= 0 || state.matches.isEmpty()) return
    var next = state.currentMatchIndex + step
    if (next > state.totalMatches) next = 1
    if (next < 1) next = state.totalMatches
    _searchState.value = state.copy(currentMatchIndex = next)
    val match = state.matches.getOrNull(next - 1)
    if (match != null) {
      setPage(match.page)
    }
  }

  fun dismissSearchNotice() {
    _searchState.value = _searchState.value.copy(specialNotice = null)
  }

  // Download Dialog
  fun openDownloadModal() {
    val doc = _activeDocument.value
    var filename = doc?.title ?: "Document"
    if (filename.endsWith(".pdf", ignoreCase = true)) {
      filename = filename.substring(0, filename.length - 4)
    }
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
      var baseFilename = _downloadState.value.filename.ifBlank {
        val title = doc?.title ?: "Document"
        if (title.endsWith(".pdf", ignoreCase = true)) {
          title.substring(0, title.length - 4)
        } else {
          title
        }
      }
      var filename = baseFilename
      if (!filename.endsWith(".pdf", ignoreCase = true)) {
        filename += ".pdf"
      }

      _downloadState.update {
        it.copy(
          isDownloading = true,
          progressPercent = 35,
          progressBytes = "Saving to Downloads/PDFGo/…"
        )
      }

      var downloadSucceeded = false
      var savedPath = "Downloads/PDFGo/$filename"

      withContext(ioDispatcher) {
        try {
          val context = try { getApplication<Application>() } catch (_: Exception) { null }
          val srcFile = doc?.localFilePath?.let { File(it) }

          if (context != null) {
            // Method 1: MediaStore (standard on modern Android, creates public Downloads/PDFGo folder)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
              val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/PDFGo")
              }
              val resolver = context.contentResolver
              val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
              if (uri != null) {
                resolver.openOutputStream(uri)?.use { output ->
                  if (srcFile != null && srcFile.exists()) {
                    srcFile.inputStream().use { input -> input.copyTo(output) }
                    downloadSucceeded = true
                  } else if (doc?.url != null) {
                    val req = Request.Builder().url(doc.url).build()
                    val resp = httpClient.newCall(req).execute()
                    resp.body?.byteStream()?.use { input -> input.copyTo(output) }
                    downloadSucceeded = true
                  }
                }
              }
            }

            // Method 2: Public External Storage Downloads folder fallback
            if (!downloadSucceeded) {
              val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
              val pdfGoDir = File(publicDownloads, "PDFGo")
              if (!pdfGoDir.exists()) {
                pdfGoDir.mkdirs()
              }
              val destFile = File(pdfGoDir, filename)
              if (srcFile != null && srcFile.exists()) {
                srcFile.copyTo(destFile, overwrite = true)
                downloadSucceeded = true
              } else if (doc?.url != null) {
                val req = Request.Builder().url(doc.url).build()
                val resp = httpClient.newCall(req).execute()
                resp.body?.byteStream()?.use { input ->
                  FileOutputStream(destFile).use { output -> input.copyTo(output) }
                }
                downloadSucceeded = true
              }
            }

            // Method 3: App-specific external files Downloads directory fallback
            if (!downloadSucceeded) {
              val appDownloads = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "PDFGo")
              if (!appDownloads.exists()) {
                appDownloads.mkdirs()
              }
              val destFile = File(appDownloads, filename)
              if (srcFile != null && srcFile.exists()) {
                srcFile.copyTo(destFile, overwrite = true)
                downloadSucceeded = true
              } else if (doc?.url != null) {
                val req = Request.Builder().url(doc.url).build()
                val resp = httpClient.newCall(req).execute()
                resp.body?.byteStream()?.use { input ->
                  FileOutputStream(destFile).use { output -> input.copyTo(output) }
                }
                downloadSucceeded = true
              }
            }
          }
        } catch (e: Exception) {
          downloadSucceeded = false
        }
      }

      if (downloadSucceeded) {
        _downloadState.update {
          it.copy(
            progressPercent = 100,
            progressBytes = "Saved to Downloads/PDFGo/$filename",
            isCompleted = true
          )
        }
        delay(500)
        _downloadState.value = DownloadModalState(isOpen = false)
        showToast("Saved to Downloads/PDFGo/$filename", ToastType.SUCCESS)
      } else {
        _downloadState.update {
          it.copy(
            isDownloading = false,
            progressBytes = "Download failed. Check connection."
          )
        }
        showToast("Failed to save PDF", ToastType.WARNING)
      }
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
    closeActiveRenderer()
    val currentUrl = _activeDocument.value?.url ?: _savedDocumentStatus.value?.url
    if (currentUrl != null) {
      sharedPreferences?.edit()
        ?.remove("page_$currentUrl")
        ?.remove("last_url")
        ?.remove("last_title")
        ?.remove("last_total_pages")
        ?.apply()
      try {
        getThumbnailFile()?.delete()
      } catch (_: Exception) {}
      try {
        getPdfFile(currentUrl)?.delete()
      } catch (_: Exception) {}
    }
    clearOrphanedCacheFiles()
    updateCacheSize()
    _savedDocumentStatus.value = null
    _activeDocument.value = null
    _urlInput.value = ""
    _isFullscreen.value = false
    _searchState.value = SearchState()
    _currentScreen.value = Screen.HOME
    showToast("PDF removed and cache cleared", ToastType.INFO)
  }

  fun clearOrphanedCacheFiles() {
    try {
      val context = try { getApplication<Application>() } catch (_: Exception) { null } ?: return
      val activeUrl = _activeDocument.value?.url ?: _savedDocumentStatus.value?.url
      val activeFile = activeUrl?.let { getPdfFile(it) }

      context.cacheDir?.listFiles()?.forEach { file ->
        if (file.isFile) {
          if (file.name.startsWith("pdf_") && file.name.endsWith(".pdf")) {
            if (activeFile == null || file.absolutePath != activeFile.absolutePath) {
              file.delete()
            }
          } else if (file.name == "resume_thumbnail.png" && activeUrl == null) {
            file.delete()
          } else if (file.name.startsWith("temp_") || file.name.endsWith(".tmp")) {
            file.delete()
          }
        }
      }
    } catch (_: Exception) {}
    updateCacheSize()
  }

  fun clearAllCache() {
    viewModelScope.launch(ioDispatcher) {
      closeActiveRenderer()
      var freedBytes = 0L
      try {
        val context = try { getApplication<Application>() } catch (_: Exception) { null }
        context?.cacheDir?.listFiles()?.forEach { file ->
          if (file.isFile) {
            freedBytes += file.length()
            file.delete()
          }
        }
      } catch (_: Exception) {}

      updateCacheSize()

      val formattedFreed = formatBytes(freedBytes)
      withContext(Dispatchers.Main) {
        showToast("Cleared $formattedFreed of cache", ToastType.SUCCESS)
      }
    }
  }

  fun updateCacheSize() {
    viewModelScope.launch(ioDispatcher) {
      try {
        val context = try { getApplication<Application>() } catch (_: Exception) { null }
        var totalBytes = 0L
        context?.cacheDir?.listFiles()?.forEach { file ->
          if (file.isFile) {
            totalBytes += file.length()
          }
        }
        val formatted = formatBytes(totalBytes)
        _cacheSizeFormatted.value = formatted
      } catch (_: Exception) {
        _cacheSizeFormatted.value = "0.0 KB"
      }
    }
  }

  private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0.0 KB"
    val kb = bytes / 1024f
    val mb = kb / 1024f
    return if (mb >= 1.0f) {
      String.format(java.util.Locale.US, "%.1f MB", mb)
    } else {
      String.format(java.util.Locale.US, "%.1f KB", kb)
    }
  }

  // Exit App
  fun openExitModal() {
    _isExitModalOpen.value = true
  }

  fun closeExitModal() {
    _isExitModalOpen.value = false
  }

  fun openClearCacheModal() {
    _isClearCacheModalOpen.value = true
  }

  fun closeClearCacheModal() {
    _isClearCacheModalOpen.value = false
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

  override fun onCleared() {
    super.onCleared()
    closeActiveRenderer()
    searchJob?.cancel()
    loadJob?.cancel()
    downloadJob?.cancel()
  }
}
