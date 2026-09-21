package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.model.PageSpacing
import com.example.model.Screen
import com.example.ui.theme.AppThemeSetting
import com.example.viewmodel.PdfViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PdfViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var viewModel: PdfViewModel

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    val app = ApplicationProvider.getApplicationContext<Application>()
    viewModel = PdfViewModel(app, testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `initial state is home screen and url is empty`() {
    assertEquals(Screen.HOME, viewModel.currentScreen.value)
    assertNull(viewModel.activeDocument.value)
    assertEquals("", viewModel.urlInput.value)
  }

  @Test
  fun `invalid url triggers error message`() {
    viewModel.onUrlChange("invalid-url-string")
    viewModel.attemptOpenPdf()
    assertNotNull(viewModel.urlError.value)
    assertEquals(Screen.HOME, viewModel.currentScreen.value)
  }

  @Test
  fun `valid url opens reader after loading`() = runTest(testDispatcher) {
    viewModel.onUrlChange("https://example.com/documents/research_paper.pdf")
    viewModel.attemptOpenPdf()
    assertEquals(Screen.LOADING, viewModel.currentScreen.value)

    advanceUntilIdle()

    assertEquals(Screen.READER, viewModel.currentScreen.value)
    assertNotNull(viewModel.activeDocument.value)
    assertEquals("research_paper.pdf", viewModel.activeDocument.value?.title)
  }

  @Test
  fun `page change stays within bounds`() = runTest(testDispatcher) {
    viewModel.onUrlChange("https://example.com/sample.pdf")
    viewModel.attemptOpenPdf()
    advanceUntilIdle()

    val initialPage = viewModel.activeDocument.value?.currentPage ?: 1
    viewModel.changePage(1)
    assertTrue((viewModel.activeDocument.value?.currentPage ?: 1) >= initialPage)

    viewModel.changePage(-1)
    assertEquals(initialPage, viewModel.activeDocument.value?.currentPage)
  }

  @Test
  fun `fullscreen toggles correctly and shows back button toast`() {
    assertFalse(viewModel.isFullscreen.value)
    viewModel.toggleFullscreen()
    assertTrue(viewModel.isFullscreen.value)
    assertEquals("Press back button on navigation bar to exit full screen", viewModel.toastMessage.value?.text)

    viewModel.exitFullscreen()
    assertFalse(viewModel.isFullscreen.value)
  }

  @Test
  fun `back press in fullscreen exits fullscreen instead of leaving reader`() = runTest(testDispatcher) {
    viewModel.onUrlChange("https://example.com/test.pdf")
    viewModel.attemptOpenPdf()
    advanceUntilIdle()
    assertEquals(Screen.READER, viewModel.currentScreen.value)

    viewModel.toggleFullscreen()
    assertTrue(viewModel.isFullscreen.value)

    // Pressing back button on navigation bar exits fullscreen mode only
    val handled = viewModel.handleBack()
    assertTrue(handled)
    assertFalse(viewModel.isFullscreen.value)
    assertEquals(Screen.READER, viewModel.currentScreen.value)
  }

  @Test
  fun `search query updates results`() = runTest(testDispatcher) {
    viewModel.openSearch()
    assertTrue(viewModel.searchState.value.isOpen)
    assertEquals("", viewModel.searchState.value.query)

    viewModel.onSearchQueryChange("test")
    advanceUntilIdle()
    assertEquals("test", viewModel.searchState.value.query)
    assertEquals(1, viewModel.searchState.value.totalMatches)

    viewModel.closeSearch()
    assertFalse(viewModel.searchState.value.isOpen)
  }

  @Test
  fun `remove pdf clears document and returns to home`() = runTest(testDispatcher) {
    viewModel.onUrlChange("https://example.com/sample.pdf")
    viewModel.attemptOpenPdf()
    advanceUntilIdle()

    viewModel.executeRemovePdf()
    assertNull(viewModel.activeDocument.value)
    assertEquals(Screen.HOME, viewModel.currentScreen.value)
    assertEquals("", viewModel.urlInput.value)
  }

  @Test
  fun `settings modifications update state and persist across restart`() = runTest(testDispatcher) {
    val app = ApplicationProvider.getApplicationContext<Application>()
    viewModel.setTheme(AppThemeSetting.LIGHT)
    viewModel.setPageSpacing(PageSpacing.COMPACT)
    viewModel.toggleKeepScreenAwake() // default was true, now false
    viewModel.toggleSaveReadingPosition() // default was true, now false

    assertEquals(AppThemeSetting.LIGHT, viewModel.themeSetting.value)
    assertEquals(PageSpacing.COMPACT, viewModel.pageSpacing.value)
    assertFalse(viewModel.keepScreenAwake.value)
    assertFalse(viewModel.saveReadingPosition.value)

    // Simulate app restart with new ViewModel
    val newVm = PdfViewModel(app, testDispatcher)
    assertEquals(AppThemeSetting.LIGHT, newVm.themeSetting.value)
    assertEquals(PageSpacing.COMPACT, newVm.pageSpacing.value)
    assertFalse(newVm.keepScreenAwake.value)
    assertFalse(newVm.saveReadingPosition.value)
  }

  @Test
  fun `navigating back from settings returns to home when opened from home`() = runTest(testDispatcher) {
    // 1. Open a PDF
    viewModel.onUrlChange("https://example.com/sample.pdf")
    viewModel.attemptOpenPdf()
    advanceUntilIdle()
    assertEquals(Screen.READER, viewModel.currentScreen.value)

    // 2. Navigate back to Home (closing the PDF view)
    viewModel.navigateBackFromReader()
    assertEquals(Screen.HOME, viewModel.currentScreen.value)

    // 3. Open Settings from Home screen
    viewModel.openSettings(Screen.HOME)
    assertEquals(Screen.SETTINGS, viewModel.currentScreen.value)

    // 4. Click back from Settings - must return to HOME, not READER
    viewModel.navigateBackFromSettings()
    assertEquals(Screen.HOME, viewModel.currentScreen.value)
  }

  @Test
  fun `navigating back from settings returns to reader when opened from reader`() = runTest(testDispatcher) {
    viewModel.onUrlChange("https://example.com/sample.pdf")
    viewModel.attemptOpenPdf()
    advanceUntilIdle()
    assertEquals(Screen.READER, viewModel.currentScreen.value)

    viewModel.openSettings(Screen.READER)
    assertEquals(Screen.SETTINGS, viewModel.currentScreen.value)

    viewModel.navigateBackFromSettings()
    assertEquals(Screen.READER, viewModel.currentScreen.value)
  }

  @Test
  fun `reading position saves and restores across sessions`() = runTest(testDispatcher) {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val prefs = app.getSharedPreferences("pdfgo_reader_prefs", android.content.Context.MODE_PRIVATE)
    prefs.edit()
      .putString("last_url", "https://example.com/test_document.pdf")
      .putString("last_title", "test_document.pdf")
      .putInt("page_https://example.com/test_document.pdf", 4)
      .putInt("last_total_pages", 10)
      .apply()

    // Create ViewModel instance representing app restart
    val vm = PdfViewModel(app, testDispatcher)
    val restored = vm.savedDocumentStatus.value
    assertNotNull(restored)
    assertEquals(4, restored?.page)
    assertEquals("test_document.pdf", restored?.title)
    assertEquals(10, restored?.totalPages)

    // Resume saved document
    vm.resumeSavedDocument()
    advanceUntilIdle()
    assertEquals(Screen.READER, vm.currentScreen.value)
    assertEquals(4, vm.activeDocument.value?.currentPage)
    assertEquals(10, vm.activeDocument.value?.totalPages)
  }

  @Test
  fun `when save reading position is disabled reading position resets to 1 on resume and back`() = runTest(testDispatcher) {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val prefs = app.getSharedPreferences("pdfgo_reader_prefs", android.content.Context.MODE_PRIVATE)
    prefs.edit().putInt("last_total_pages", 10).apply()

    // 1. Open document and navigate to page 5
    viewModel.onUrlChange("https://example.com/test_doc.pdf")
    viewModel.attemptOpenPdf()
    advanceUntilIdle()
    viewModel.setPage(5)
    assertEquals(5, viewModel.activeDocument.value?.currentPage)

    // 2. Disable save reading position
    viewModel.toggleSaveReadingPosition()
    assertFalse(viewModel.saveReadingPosition.value)

    // Verify saved document status is reset to page 1
    assertEquals(1, viewModel.savedDocumentStatus.value?.page)

    // 3. User navigates back from reader
    viewModel.navigateBackFromReader()
    assertEquals(Screen.HOME, viewModel.currentScreen.value)
    assertEquals(1, viewModel.savedDocumentStatus.value?.page)

    // 4. Resume saved document - should start from page 1
    viewModel.resumeSavedDocument()
    advanceUntilIdle()
    assertEquals(Screen.READER, viewModel.currentScreen.value)
    assertEquals(1, viewModel.activeDocument.value?.currentPage)
  }

  @Test
  fun `when cache is cleared and app restarts reading position resets to 1`() = runTest(testDispatcher) {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val prefs = app.getSharedPreferences("pdfgo_reader_prefs", android.content.Context.MODE_PRIVATE)
    prefs.edit().putInt("last_total_pages", 10).apply()

    // 1. Open document and navigate to page 5
    viewModel.onUrlChange("https://example.com/test_doc.pdf")
    viewModel.attemptOpenPdf()
    advanceUntilIdle()
    viewModel.setPage(5)
    assertEquals(5, viewModel.activeDocument.value?.currentPage)

    // 2. Clear cache with completion callback
    var restartTriggered = false
    viewModel.clearAllCache {
      restartTriggered = true
    }
    advanceUntilIdle()

    assertTrue(restartTriggered)
    assertEquals(1, viewModel.savedDocumentStatus.value?.page)

    // 3. Simulate app restart with new ViewModel
    val restartVm = PdfViewModel(app, testDispatcher)
    advanceUntilIdle()

    // Saved document on Home Screen should display page 1
    assertEquals("https://example.com/test_doc.pdf", restartVm.savedDocumentStatus.value?.url)
    assertEquals(1, restartVm.savedDocumentStatus.value?.page)
    assertEquals(10, restartVm.savedDocumentStatus.value?.totalPages)

    // 4. Resuming reading loads from page 1
    restartVm.resumeSavedDocument()
    advanceUntilIdle()
    assertEquals(Screen.READER, restartVm.currentScreen.value)
    assertEquals(1, restartVm.activeDocument.value?.currentPage)
  }

  @Test
  fun `zoom out down to 50 percent is supported`() = runTest(testDispatcher) {
    viewModel.onUrlChange("https://example.com/test_doc.pdf")
    viewModel.attemptOpenPdf()
    advanceUntilIdle()

    assertEquals(100, viewModel.activeDocument.value?.zoomPercent)

    // Zoom out to 60%
    viewModel.setZoomPercent(60)
    assertEquals(60, viewModel.activeDocument.value?.zoomPercent)

    // Coerce at minimum 50%
    viewModel.setZoomPercent(10)
    assertEquals(50, viewModel.activeDocument.value?.zoomPercent)

    // Adjust zoom
    viewModel.adjustZoom(25)
    assertEquals(75, viewModel.activeDocument.value?.zoomPercent)
  }
}

