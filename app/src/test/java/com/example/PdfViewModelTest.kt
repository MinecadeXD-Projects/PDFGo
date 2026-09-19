package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.model.FitMode
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
  fun `fullscreen toggles correctly`() {
    assertFalse(viewModel.isFullscreen.value)
    viewModel.toggleFullscreen()
    assertTrue(viewModel.isFullscreen.value)
    viewModel.exitFullscreen()
    assertFalse(viewModel.isFullscreen.value)
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
    viewModel.setFitMode(FitMode.FIT_PAGE)
    viewModel.setPageSpacing(PageSpacing.COMPACT)
    viewModel.toggleKeepScreenAwake() // default was true, now false

    assertEquals(AppThemeSetting.LIGHT, viewModel.themeSetting.value)
    assertEquals(FitMode.FIT_PAGE, viewModel.fitMode.value)
    assertEquals(PageSpacing.COMPACT, viewModel.pageSpacing.value)
    assertFalse(viewModel.keepScreenAwake.value)

    // Simulate app restart with new ViewModel
    val newVm = PdfViewModel(app, testDispatcher)
    assertEquals(AppThemeSetting.LIGHT, newVm.themeSetting.value)
    assertEquals(FitMode.FIT_PAGE, newVm.fitMode.value)
    assertEquals(PageSpacing.COMPACT, newVm.pageSpacing.value)
    assertFalse(newVm.keepScreenAwake.value)
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
}
