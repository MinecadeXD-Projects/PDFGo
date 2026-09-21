package com.example

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.model.Screen
import com.example.ui.ClearCacheDialog
import com.example.ui.DownloadPdfDialog
import com.example.ui.ExitAppDialog
import com.example.ui.HomeScreen
import com.example.ui.LoadingScreen
import com.example.ui.PasswordProtectedDialog
import com.example.ui.ReaderScreen
import com.example.ui.RemovePdfDialog
import com.example.ui.SettingsScreen
import com.example.ui.ToastPill
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.PdfViewModel

class MainActivity : ComponentActivity() {
  private val viewModel: PdfViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      val themeSetting by viewModel.themeSetting.collectAsState()
      val keepScreenAwake by viewModel.keepScreenAwake.collectAsState()
      val currentScreen by viewModel.currentScreen.collectAsState()

      LaunchedEffect(keepScreenAwake, currentScreen) {
        if (keepScreenAwake && currentScreen == Screen.READER) {
          window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
          window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
      }

      MyApplicationTheme(themeSetting = themeSetting) {
        PdfGoApp(
          viewModel = viewModel,
          onExitApp = { finish() },
          onRestartApp = {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
            Runtime.getRuntime().exit(0)
          },
        )
      }
    }
  }
}

@Composable
fun PdfGoApp(
  viewModel: PdfViewModel,
  onExitApp: () -> Unit,
  onRestartApp: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val currentScreen by viewModel.currentScreen.collectAsState()
  val activeDoc by viewModel.activeDocument.collectAsState()
  val urlInput by viewModel.urlInput.collectAsState()
  val urlError by viewModel.urlError.collectAsState()
  val loadingProgress by viewModel.loadingProgress.collectAsState()
  val themeSetting by viewModel.themeSetting.collectAsState()
  val pageSpacing by viewModel.pageSpacing.collectAsState()
  val keepScreenAwake by viewModel.keepScreenAwake.collectAsState()
  val saveReadingPosition by viewModel.saveReadingPosition.collectAsState()
  val savedDocumentStatus by viewModel.savedDocumentStatus.collectAsState()
  val isFullscreen by viewModel.isFullscreen.collectAsState()
  val searchState by viewModel.searchState.collectAsState()
  val downloadState by viewModel.downloadState.collectAsState()
  val isRemoveModalOpen by viewModel.isRemoveModalOpen.collectAsState()
  val isExitModalOpen by viewModel.isExitModalOpen.collectAsState()
  val isClearCacheModalOpen by viewModel.isClearCacheModalOpen.collectAsState()
  val isPasswordModalOpen by viewModel.isPasswordModalOpen.collectAsState()
  val cacheSizeFormatted by viewModel.cacheSizeFormatted.collectAsState()
  val toastMessage by viewModel.toastMessage.collectAsState()

  BackHandler {
    val handled = viewModel.handleBack()
    if (!handled) {
      onExitApp()
    }
  }

  val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()

  Scaffold(
    modifier = modifier.fillMaxSize(),
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
  ) { innerPadding ->
    Box(
      modifier =
        Modifier.fillMaxSize()
          .padding(
            top = if (isFullscreen) innerPadding.calculateTopPadding() else systemBarsPadding.calculateTopPadding(),
            bottom = if (isFullscreen) innerPadding.calculateBottomPadding() else systemBarsPadding.calculateBottomPadding(),
          ),
    ) {
      Crossfade(
        targetState = currentScreen,
        animationSpec = tween(220),
        label = "screen_crossfade",
      ) { screen ->
        when (screen) {
          Screen.HOME -> {
            HomeScreen(
              urlInput = urlInput,
              urlError = urlError,
              savedDocument = savedDocumentStatus,
              onUrlChange = { viewModel.onUrlChange(it) },
              onPaste = { viewModel.pasteUrl(it) },
              onOpenPdf = { viewModel.attemptOpenPdf() },
              onResumeSaved = { viewModel.resumeSavedDocument() },
              onRemovePdf = { viewModel.openRemovePdfModal() },
              onOpenSettings = { viewModel.openSettings(Screen.HOME) },
            )
          }

          Screen.LOADING -> {
            LoadingScreen(
              progress = loadingProgress,
              onCancel = { viewModel.cancelLoading() },
            )
          }

          Screen.READER -> {
            activeDoc?.let { doc ->
              ReaderScreen(
                document = doc,
                isFullscreen = isFullscreen,
                searchState = searchState,
                pageSpacing = pageSpacing,
                onBack = { viewModel.navigateBackFromReader() },
                onToggleFullscreen = { viewModel.toggleFullscreen() },
                onOpenSearch = { viewModel.openSearch() },
                onCloseSearch = { viewModel.closeSearch() },
                onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                onSearchNavigate = { viewModel.navigateSearch(it) },
                onDismissSearchNotice = { viewModel.dismissSearchNotice() },
                onOpenDownloadModal = { viewModel.openDownloadModal() },
                onOpenRemoveModal = { viewModel.openRemovePdfModal() },
                onOpenSettings = { viewModel.openSettings(Screen.READER) },
                onChangePage = { viewModel.changePage(it) },
                onSetPage = { viewModel.setPage(it) },
                onAdjustZoom = { viewModel.adjustZoom(it) },
                onSetZoomPercent = { viewModel.setZoomPercent(it) },
                getPageBitmap = { viewModel.loadPageBitmap(it) },
              )
            } ?: run {
              HomeScreen(
                urlInput = urlInput,
                urlError = urlError,
                savedDocument = savedDocumentStatus,
                onUrlChange = { viewModel.onUrlChange(it) },
                onPaste = { viewModel.pasteUrl(it) },
                onOpenPdf = { viewModel.attemptOpenPdf() },
                onResumeSaved = { viewModel.resumeSavedDocument() },
                onRemovePdf = { viewModel.openRemovePdfModal() },
                onOpenSettings = { viewModel.openSettings(Screen.HOME) },
              )
            }
          }

          Screen.SETTINGS -> {
            SettingsScreen(
              currentTheme = themeSetting,
              pageSpacing = pageSpacing,
              keepScreenAwake = keepScreenAwake,
              saveReadingPosition = saveReadingPosition,
              onBack = { viewModel.navigateBackFromSettings() },
              onThemeChange = { viewModel.setTheme(it) },
              onPageSpacingChange = { viewModel.setPageSpacing(it) },
              onToggleKeepAwake = { viewModel.toggleKeepScreenAwake() },
              onToggleSavePosition = { viewModel.toggleSaveReadingPosition() },
              cacheSize = cacheSizeFormatted,
              onOpenClearCacheDialog = { viewModel.openClearCacheModal() },
            )
          }
        }
      }

      // Dialogs
      DownloadPdfDialog(
        state = downloadState,
        onFilenameChange = { viewModel.onDownloadFilenameChange(it) },
        onStartDownload = { viewModel.startDownload() },
        onDismiss = { viewModel.closeDownloadModal() },
      )

      RemovePdfDialog(
        isOpen = isRemoveModalOpen,
        onConfirm = { viewModel.executeRemovePdf() },
        onDismiss = { viewModel.closeRemovePdfModal() },
      )

      ExitAppDialog(
        isOpen = isExitModalOpen,
        onConfirm = {
          viewModel.closeExitModal()
          onExitApp()
        },
        onDismiss = { viewModel.closeExitModal() },
      )

      ClearCacheDialog(
        isOpen = isClearCacheModalOpen,
        onConfirm = {
          viewModel.closeClearCacheModal()
          viewModel.clearAllCache()
          onRestartApp()
        },
        onDismiss = { viewModel.closeClearCacheModal() },
      )

      PasswordProtectedDialog(
        isOpen = isPasswordModalOpen,
        onUnlock = { viewModel.unlockPasswordProtected(it) },
        onDismiss = { viewModel.closePasswordModal() },
      )

      // Toast Pills
      ToastPill(toastMessage = toastMessage)
    }
  }
}

// Backwards compatibility for existing screenshot unit tests
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
  MyApplicationTheme { Greeting("Android") }
}
