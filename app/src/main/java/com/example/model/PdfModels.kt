package com.example.model

import android.graphics.Bitmap
import com.example.ui.theme.AppThemeSetting

enum class Screen {
  HOME,
  LOADING,
  READER,
  SETTINGS
}

enum class PageSpacing(val label: String, val dpValue: Int) {
  NORMAL("Normal (16dp)", 16),
  COMPACT("Compact (8dp)", 8),
  NONE("None (Continuous)", 0)
}

data class SavedDocumentStatus(
  val url: String,
  val title: String,
  val page: Int,
  val totalPages: Int,
  val thumbnailBitmap: Bitmap? = null
)

data class PdfDocument(
  val url: String,
  val title: String,
  val localFilePath: String? = null,
  val totalPages: Int = 1,
  val currentPage: Int = 1,
  val zoomPercent: Int = 100,
  val pageBitmaps: List<Bitmap> = emptyList(),
  val useWebViewFallback: Boolean = false,
  val loadError: String? = null
)

data class SearchMatch(
  val page: Int,
  val matchIndexOnPage: Int = 1,
  val snippet: String = ""
)

data class SearchState(
  val isOpen: Boolean = false,
  val query: String = "",
  val currentMatchIndex: Int = 0,
  val totalMatches: Int = 0,
  val matches: List<SearchMatch> = emptyList(),
  val isSearching: Boolean = false,
  val specialNotice: String? = null
)

data class DownloadModalState(
  val isOpen: Boolean = false,
  val filename: String = "",
  val isDownloading: Boolean = false,
  val progressPercent: Int = 0,
  val progressBytes: String = "",
  val isCompleted: Boolean = false
)

enum class ToastType {
  INFO,
  SUCCESS,
  WARNING
}

data class ToastMessage(
  val text: String,
  val type: ToastType = ToastType.INFO,
  val timestamp: Long = System.currentTimeMillis()
)
