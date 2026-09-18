package com.example

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
    viewModel = PdfViewModel()
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `initial state is home screen`() {
    assertEquals(Screen.HOME, viewModel.currentScreen.value)
    assertNull(viewModel.activeDocument.value)
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
    viewModel.onUrlChange("https://arxiv.org/pdf/2402.quantum_mechanics.pdf")
    viewModel.attemptOpenPdf()
    assertEquals(Screen.LOADING, viewModel.currentScreen.value)

    advanceUntilIdle()

    assertEquals(Screen.READER, viewModel.currentScreen.value)
    assertNotNull(viewModel.activeDocument.value)
    assertEquals("2402.quantum_mechanics.pdf", viewModel.activeDocument.value?.title)
  }

  @Test
  fun `page change stays within bounds`() = runTest(testDispatcher) {
    viewModel.onUrlChange("https://arxiv.org/pdf/sample.pdf")
    viewModel.attemptOpenPdf()
    advanceUntilIdle()

    val initialPage = viewModel.activeDocument.value?.currentPage ?: 12
    viewModel.changePage(1)
    assertEquals(initialPage + 1, viewModel.activeDocument.value?.currentPage)

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
  fun `search query updates results`() {
    viewModel.openSearch()
    assertTrue(viewModel.searchState.value.isOpen)

    viewModel.onSearchQueryChange("quantum")
    assertEquals(18, viewModel.searchState.value.totalMatches)

    viewModel.onSearchQueryChange("scanned")
    assertNotNull(viewModel.searchState.value.specialNotice)

    viewModel.closeSearch()
    assertFalse(viewModel.searchState.value.isOpen)
  }

  @Test
  fun `remove pdf clears document and returns to home`() = runTest(testDispatcher) {
    viewModel.onUrlChange("https://arxiv.org/pdf/sample.pdf")
    viewModel.attemptOpenPdf()
    advanceUntilIdle()

    viewModel.executeRemovePdf()
    assertNull(viewModel.activeDocument.value)
    assertEquals(Screen.HOME, viewModel.currentScreen.value)
    assertEquals("", viewModel.urlInput.value)
  }

  @Test
  fun `settings modifications update state`() {
    viewModel.setTheme(AppThemeSetting.LIGHT)
    assertEquals(AppThemeSetting.LIGHT, viewModel.themeSetting.value)

    viewModel.setFitMode(FitMode.FIT_PAGE)
    assertEquals(FitMode.FIT_PAGE, viewModel.fitMode.value)

    viewModel.setPageSpacing(PageSpacing.COMPACT)
    assertEquals(PageSpacing.COMPACT, viewModel.pageSpacing.value)
  }
}
