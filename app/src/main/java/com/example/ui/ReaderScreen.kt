package com.example.ui

import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.FitMode
import com.example.model.PageSpacing
import com.example.model.PdfDocument
import com.example.model.SearchState
import com.example.ui.theme.Amber500
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.Emerald400
import com.example.ui.theme.PillControlGradient
import com.example.ui.theme.PrimaryGradient
import com.example.ui.theme.Rose500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import java.net.URLEncoder
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
fun ReaderScreen(
  document: PdfDocument,
  isFullscreen: Boolean,
  searchState: SearchState,
  fitMode: FitMode,
  pageSpacing: PageSpacing,
  lockZoomIn: Boolean = false,
  lockZoomOut: Boolean = false,
  onBack: () -> Unit,
  onToggleFullscreen: () -> Unit,
  onOpenSearch: () -> Unit,
  onCloseSearch: () -> Unit,
  onSearchQueryChange: (String) -> Unit,
  onSearchNavigate: (Int) -> Unit,
  onDismissSearchNotice: () -> Unit,
  onOpenDownloadModal: () -> Unit,
  onOpenRemoveModal: () -> Unit,
  onOpenSettings: () -> Unit,
  onToggleFitMode: () -> Unit,
  onToggleLockZoomIn: () -> Unit = {},
  onToggleLockZoomOut: () -> Unit = {},
  onChangePage: (Int) -> Unit,
  onSetPage: (Int) -> Unit,
  onAdjustZoom: (Int) -> Unit = {},
  getPageBitmap: (suspend (Int, Float) -> Bitmap?)? = null,
  onPrefetchPage: ((Int, Float) -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  var isMenuExpanded by remember { mutableStateOf(false) }
  val lazyListState = rememberLazyListState()
  val coroutineScope = rememberCoroutineScope()

  // Two-finger pinch to zoom & pan state
  // By default zoom fits page to width (1.0f)
  var zoomScale by remember { mutableFloatStateOf(1f) }
  var panOffset by remember { mutableStateOf(Offset.Zero) }

  // When lockZoomOut is false (default), zoom out below fit-to-width is enabled (down to 0.4x)
  // When lockZoomIn is false (default), zoom in is enabled (up to 4.0x)
  val minAllowedZoom = 0.4f
  val maxAllowedZoom = 4.0f

  LaunchedEffect(Unit) {
    if (zoomScale < minAllowedZoom) {
      zoomScale = minAllowedZoom
      panOffset = Offset.Zero
    } else if (zoomScale > maxAllowedZoom) {
      zoomScale = maxAllowedZoom
    }
  }

  val resetZoom = {
    zoomScale = 1f
    panOffset = Offset.Zero
  }

  val zoomInStep = {
    if (zoomScale < maxAllowedZoom) {
      val target = (zoomScale + 0.25f).coerceAtMost(maxAllowedZoom)
      zoomScale = target
    }
  }

  val zoomOutStep = {
    if (zoomScale > minAllowedZoom) {
      val target = (zoomScale - 0.25f).coerceAtLeast(minAllowedZoom)
      zoomScale = target
      if (zoomScale <= 1.05f) {
        panOffset = Offset.Zero
      }
    }
  }

  // Prefetch adjacent pages as user scrolls for instant loading
  LaunchedEffect(lazyListState.firstVisibleItemIndex, zoomScale) {
    val idx = lazyListState.firstVisibleItemIndex
    onPrefetchPage?.invoke(idx - 1, zoomScale)
    onPrefetchPage?.invoke(idx + 1, zoomScale)
    onPrefetchPage?.invoke(idx + 2, zoomScale)
  }

  // Jump immediately to initial/saved page on document load
  LaunchedEffect(document.url) {
    if (document.currentPage > 1 && document.totalPages > 0) {
      val targetIndex = (document.currentPage - 1).coerceIn(0, document.totalPages - 1)
      lazyListState.scrollToItem(targetIndex)
    }
  }

  // Auto-scroll when active search match changes
  LaunchedEffect(searchState.currentMatchIndex) {
    if (searchState.isOpen && searchState.matches.isNotEmpty()) {
      val currentMatch = searchState.matches.getOrNull(searchState.currentMatchIndex - 1)
      if (currentMatch != null && document.totalPages > 0) {
        val targetIndex = (currentMatch.page - 1).coerceIn(0, document.totalPages - 1)
        lazyListState.animateScrollToItem(targetIndex)
      }
    }
  }

  // Observe scroll position to update the page counter dynamically as user scrolls
  LaunchedEffect(lazyListState) {
    snapshotFlow {
      val layoutInfo = lazyListState.layoutInfo
      val visibleItems = layoutInfo.visibleItemsInfo
      if (visibleItems.isNotEmpty()) {
        val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
        val mostVisible = visibleItems.minByOrNull { item ->
          val itemCenter = item.offset + item.size / 2
          kotlin.math.abs(itemCenter - viewportCenter)
        }
        val calculated = (mostVisible?.index ?: lazyListState.firstVisibleItemIndex) + 1
        calculated.coerceIn(1, maxOf(1, document.totalPages))
      } else {
        (lazyListState.firstVisibleItemIndex + 1).coerceIn(1, maxOf(1, document.totalPages))
      }
    }
      .distinctUntilChanged()
      .collect { page ->
        if (lazyListState.isScrollInProgress && page != document.currentPage) {
          onSetPage(page)
        }
      }
  }

  // Helper lambda to scroll by page delta cleanly across all pages in the PDF
  val navigateToPageDelta: (Int) -> Unit = { delta ->
    if (document.totalPages > 0) {
      val currentIdx = lazyListState.firstVisibleItemIndex
      val nextIdx = (currentIdx + delta).coerceIn(0, document.totalPages - 1)
      val targetPage = nextIdx + 1
      onSetPage(targetPage)
      coroutineScope.launch {
        lazyListState.animateScrollToItem(nextIdx)
      }
    } else {
      onChangePage(delta)
    }
  }

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(
          if (MaterialTheme.colorScheme.background == Color(0xFFF8FAFC)) {
            Color(0xFFE2E8F0)
          } else {
            Color(0xFF0C101A)
          }
        ),
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Top Toolbar
      AnimatedVisibility(
        visible = !isFullscreen,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
      ) {
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .background(MaterialTheme.colorScheme.surface)
              .shadow(2.dp),
        ) {
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            // Back & Document Info
            Row(
              modifier = Modifier.weight(1f).padding(end = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("btn_reader_back"),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                  contentDescription = "Back to Home",
                  tint = MaterialTheme.colorScheme.onSurface,
                )
              }

              Spacer(modifier = Modifier.width(4.dp))

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = document.title,
                  style =
                    MaterialTheme.typography.titleMedium.copy(
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onSurface,
                      fontSize = 14.sp,
                    ),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )

                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Text(
                    text = "${document.totalPages} Pages",
                    style =
                      MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                      ),
                  )
                  Box(
                    modifier =
                      Modifier.size(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
                  )
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    Box(
                      modifier =
                        Modifier.size(6.dp)
                          .clip(RoundedCornerShape(50))
                          .background(Emerald400),
                    )
                    Text(
                      text = "Active",
                      style =
                        MaterialTheme.typography.labelSmall.copy(
                          color = Emerald400,
                          fontWeight = FontWeight.SemiBold,
                          fontSize = 11.sp,
                        ),
                    )
                  }
                }
              }
            }

            // Action Buttons
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
              IconButton(
                onClick = onOpenSearch,
                modifier = Modifier.size(38.dp).testTag("btn_reader_search"),
              ) {
                Icon(
                  imageVector = Icons.Default.Search,
                  contentDescription = "Search",
                  tint = MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.size(20.dp),
                )
              }

              IconButton(
                onClick = onOpenDownloadModal,
                modifier = Modifier.size(38.dp).testTag("btn_reader_download"),
              ) {
                Icon(
                  imageVector = Icons.Default.Download,
                  contentDescription = "Download",
                  tint = MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.size(20.dp),
                )
              }

              IconButton(
                onClick = onToggleFullscreen,
                modifier = Modifier.size(38.dp).testTag("btn_reader_fullscreen"),
              ) {
                Icon(
                  imageVector = Icons.Default.Fullscreen,
                  contentDescription = "Fullscreen",
                  tint = MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.size(20.dp),
                )
              }

              // Overflow Menu
              Box {
                IconButton(
                  onClick = { isMenuExpanded = true },
                  modifier = Modifier.size(38.dp).testTag("btn_reader_menu"),
                ) {
                  Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More Options",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                  )
                }

                DropdownMenu(
                  expanded = isMenuExpanded,
                  onDismissRequest = { isMenuExpanded = false },
                ) {
                  DropdownMenuItem(
                    text = { Text("Settings") },
                    leadingIcon = {
                      Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                      )
                    },
                    onClick = {
                      isMenuExpanded = false
                      onOpenSettings()
                    },
                  )
                  HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                  )
                  DropdownMenuItem(
                    text = {
                      Text(
                        text = "Remove PDF from PDFGo",
                        color = Rose500,
                        fontWeight = FontWeight.Medium,
                      )
                    },
                    leadingIcon = {
                      Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = Rose500,
                        modifier = Modifier.size(18.dp),
                      )
                    },
                    onClick = {
                      isMenuExpanded = false
                      onOpenRemoveModal()
                    },
                  )
                }
              }
            }
          }

          // Search Bar Overlay
          if (searchState.isOpen) {
            Column(
              modifier =
                Modifier.fillMaxWidth()
                  .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                  .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                  )
                  .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                OutlinedTextField(
                  value = searchState.query,
                  onValueChange = onSearchQueryChange,
                  placeholder = { Text("Search in PDF…", fontSize = 12.sp) },
                  leadingIcon = {
                    Icon(
                      imageVector = Icons.Default.Search,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.onSurfaceVariant,
                      modifier = Modifier.size(16.dp),
                    )
                  },
                  trailingIcon = {
                    Text(
                      text =
                        if (searchState.totalMatches > 0) {
                          "${searchState.currentMatchIndex} of ${searchState.totalMatches}"
                        } else if (searchState.query.isEmpty()) {
                          "0 matches"
                        } else {
                          "0 of 0"
                        },
                      style =
                        MaterialTheme.typography.labelSmall.copy(
                          fontFamily = FontFamily.Monospace,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                          fontSize = 11.sp,
                        ),
                      modifier = Modifier.padding(end = 8.dp),
                    )
                  },
                  singleLine = true,
                  shape = RoundedCornerShape(12.dp),
                  colors =
                    OutlinedTextFieldDefaults.colors(
                      focusedContainerColor = MaterialTheme.colorScheme.surface,
                      unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                      focusedBorderColor = BrandBlue,
                      unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    ),
                  modifier = Modifier.weight(1f).testTag("input_reader_search"),
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                  IconButton(
                    onClick = { onSearchNavigate(-1) },
                    enabled = searchState.totalMatches > 0,
                    modifier = Modifier.size(34.dp).testTag("btn_search_prev"),
                  ) {
                    Icon(
                      imageVector = Icons.Default.NavigateBefore,
                      contentDescription = "Previous Match",
                      modifier = Modifier.size(20.dp),
                    )
                  }
                  IconButton(
                    onClick = { onSearchNavigate(1) },
                    enabled = searchState.totalMatches > 0,
                    modifier = Modifier.size(34.dp).testTag("btn_search_next"),
                  ) {
                    Icon(
                      imageVector = Icons.Default.NavigateNext,
                      contentDescription = "Next Match",
                      modifier = Modifier.size(20.dp),
                    )
                  }
                  IconButton(
                    onClick = onCloseSearch,
                    modifier = Modifier.size(34.dp).testTag("btn_search_close"),
                  ) {
                    Icon(
                      imageVector = Icons.Default.Close,
                      contentDescription = "Close Search",
                      modifier = Modifier.size(18.dp),
                    )
                  }
                }
              }

              // Search special notice
              if (searchState.specialNotice != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                  modifier =
                    Modifier.fillMaxWidth()
                      .clip(RoundedCornerShape(8.dp))
                      .background(Amber500.copy(alpha = 0.15f))
                      .padding(horizontal = 10.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Text(
                    text = searchState.specialNotice,
                    style =
                      MaterialTheme.typography.bodySmall.copy(
                        color = Amber500,
                        fontSize = 11.sp,
                      ),
                  )
                  Text(
                    text = "Dismiss",
                    style =
                      MaterialTheme.typography.labelSmall.copy(
                        color = Amber500,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                      ),
                    modifier = Modifier.clickable { onDismissSearchNotice() },
                  )
                }
              }
            }
          }

          // Modern gradient accent line below toolbar
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .height(2.dp)
                .background(PrimaryGradient),
          )
        }
      }

      // Main PDF Viewport (Render the user's actual document with two-finger pinch-to-zoom and pan)
      val pageGap = pageSpacing.dpValue.dp

      BoxWithConstraints(
        modifier =
          Modifier.weight(1f)
            .fillMaxWidth()
            .clipToBounds()
            .pointerInput(Unit) {
              detectTapGestures(
                onDoubleTap = { tapOffset ->
                  if (zoomScale > 1.05f || zoomScale < 0.95f) {
                    resetZoom()
                  } else {
                    zoomScale = 2.0f
                    val targetPanX = (size.width / 2f - tapOffset.x) * 1.0f
                    val maxPanX = (size.width * 1.0f) / 2f
                    panOffset = Offset(
                      targetPanX.coerceIn(-maxPanX, maxPanX),
                      0f
                    )
                  }
                }
              )
            }
            .pointerInput(Unit) {
              awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                do {
                  val event = awaitPointerEvent()
                  val activePointers = event.changes.filter { it.pressed }

                  if (activePointers.size >= 2) {
                    // Two fingers: fluid pinch-to-zoom & horizontal pan
                    val zoomChange = event.calculateZoom()
                    val panChange = event.calculatePan()

                    val rawNewScale = zoomScale * zoomChange
                    val newScale = rawNewScale.coerceIn(minAllowedZoom, maxAllowedZoom)

                    if (kotlin.math.abs(newScale - 1f) < 0.03f) {
                      zoomScale = 1f
                      panOffset = Offset.Zero
                    } else {
                      zoomScale = newScale
                      val maxPanX = if (zoomScale > 1f) ((size.width * (zoomScale - 1f)) / 2f) else 0f
                      val newX = (panOffset.x + panChange.x).coerceIn(-maxPanX, maxPanX)
                      panOffset = Offset(newX, 0f)
                    }
                    event.changes.forEach {
                      if (it.positionChanged()) it.consume()
                    }
                  } else if (activePointers.size == 1 && zoomScale > 1.05f) {
                    // One finger when zoomed in: pan horizontally across page and scroll vertically
                    val panChange = event.calculatePan()
                    val maxPanX = (size.width * (zoomScale - 1f)) / 2f
                    val newX = (panOffset.x + panChange.x).coerceIn(-maxPanX, maxPanX)
                    val verticalPan = panChange.y
                    panOffset = Offset(newX, 0f)

                    if (kotlin.math.abs(verticalPan) > 0.5f) {
                      coroutineScope.launch {
                        lazyListState.scrollBy(-verticalPan)
                      }
                    }

                    event.changes.forEach {
                      if (it.positionChanged()) it.consume()
                    }
                  }
                } while (event.changes.any { it.pressed })
              }
            },
        contentAlignment = Alignment.TopCenter,
      ) {
        val viewportWidth = maxWidth
        val baseWidth = (viewportWidth - 24.dp).coerceAtLeast(200.dp)
        val effectiveWidth = (baseWidth * zoomScale).coerceAtLeast(160.dp)

        Box(
          modifier = Modifier.fillMaxSize()
        ) {
          if (document.totalPages > 0 && !document.useWebViewFallback) {
            // Native PdfRenderer pages from user's PDF (rendered dynamically on-demand and sharp at any zoom level)
            LazyColumn(
              state = lazyListState,
              modifier = Modifier.fillMaxSize(),
              contentPadding = PaddingValues(vertical = 16.dp, horizontal = 12.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(pageGap),
            ) {
              items(
                count = document.totalPages,
                key = { it }
              ) { index ->
                val pageNum = index + 1
                val isMatchedPage = searchState.isOpen && searchState.matches.any { it.page == pageNum }
                val isCurrentMatchPage = searchState.isOpen &&
                  searchState.matches.getOrNull(searchState.currentMatchIndex - 1)?.page == pageNum

                PdfPageCard(
                  pageNumber = pageNum,
                  totalPages = document.totalPages,
                  preloadedBitmap = document.pageBitmaps.getOrNull(index),
                  getPageBitmap = getPageBitmap,
                  pageIndex = index,
                  fitMode = fitMode,
                  zoomScale = zoomScale,
                  isMatchedPage = isMatchedPage,
                  isCurrentMatchPage = isCurrentMatchPage,
                  modifier = Modifier
                    .width(effectiveWidth)
                    .offset { IntOffset(panOffset.x.roundToInt(), 0) },
                )
              }
            }
          } else if (document.useWebViewFallback) {
            // Real WebView rendering user's PDF
            AndroidView(
              factory = { ctx ->
                WebView(ctx).apply {
                  layoutParams =
                    ViewGroup.LayoutParams(
                      ViewGroup.LayoutParams.MATCH_PARENT,
                      ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                  settings.javaScriptEnabled = true
                  settings.domStorageEnabled = true
                  settings.builtInZoomControls = true
                  settings.displayZoomControls = false
                  settings.loadWithOverviewMode = true
                  settings.useWideViewPort = true
                  settings.allowFileAccess = true
                  settings.setSupportZoom(true)
                  webViewClient =
                    object : WebViewClient() {
                      override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?,
                      ): Boolean {
                        return false
                      }
                    }
                  val encodedUrl = URLEncoder.encode(document.url, "UTF-8")
                  loadUrl("https://docs.google.com/viewer?url=$encodedUrl&embedded=true")
                }
              },
              modifier = Modifier.fillMaxSize(),
            )
          } else {
            // Fallback document card for testing / unit test environments
            Card(
              shape = RoundedCornerShape(8.dp),
              colors = CardDefaults.cardColors(containerColor = Color.White),
              elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
              modifier =
                Modifier.fillMaxWidth()
                  .widthIn(max = 520.dp)
                  .padding(24.dp),
            ) {
              Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Icon(
                  imageVector = Icons.Default.Description,
                  contentDescription = null,
                  tint = BrandBlue,
                  modifier = Modifier.size(48.dp),
                )
                Text(
                  text = document.title,
                  style =
                    MaterialTheme.typography.titleMedium.copy(
                      fontWeight = FontWeight.Bold,
                      color = Color(0xFF0F172A),
                    ),
                )
                Text(
                  text = document.url,
                  style =
                    MaterialTheme.typography.bodySmall.copy(
                      color = Color(0xFF64748B),
                    ),
                )
              }
            }
          }
        }

        // Floating Reset Zoom Pill at Top Right when zoomed in or out
        if (zoomScale > 1.05f || zoomScale < 0.95f) {
          Surface(
            onClick = resetZoom,
            shape = RoundedCornerShape(50.dp),
            color = Slate900.copy(alpha = 0.92f),
            contentColor = Color.White,
            shadowElevation = 6.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).testTag("btn_reset_zoom"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Text(
                text = "${(zoomScale * 100).toInt()}%",
                style =
                  MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Emerald400,
                    fontSize = 11.sp,
                  ),
              )
              Text(
                text = "Fit Width",
                style =
                  MaterialTheme.typography.labelSmall.copy(
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                  ),
              )
            }
          }
        }
      }
    }

    // Floating Fullscreen Exit Pill at Top
    AnimatedVisibility(
      visible = isFullscreen,
      enter = fadeIn() + slideInVertically { -it },
      exit = fadeOut() + slideOutVertically { -it },
      modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp),
    ) {
      Surface(
        onClick = onToggleFullscreen,
        shape = RoundedCornerShape(50.dp),
        color = Color.Transparent,
        contentColor = Color.White,
        shadowElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Brush.horizontalGradient(listOf(Slate800, Slate700))),
        modifier = Modifier.clip(RoundedCornerShape(50.dp)).background(PillControlGradient),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Default.FullscreenExit,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp),
          )
          Text(
            text = "Exit Fullscreen",
            style =
              MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
              ),
          )
        }
      }
    }

    // Floating Bottom Bar (Controls)
    AnimatedVisibility(
      visible = !isFullscreen,
      enter = slideInVertically { it } + fadeIn(),
      exit = slideOutVertically { it } + fadeOut(),
      modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
    ) {
      Surface(
        shape = RoundedCornerShape(50.dp),
        color = Color.Transparent,
        contentColor = Color.White,
        shadowElevation = 10.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Brush.horizontalGradient(listOf(Slate800, Slate700))),
        modifier = Modifier.clip(RoundedCornerShape(50.dp)).background(PillControlGradient),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          // Previous Page Button
          IconButton(
            onClick = { navigateToPageDelta(-1) },
            enabled = document.currentPage > 1,
            modifier = Modifier.size(32.dp).testTag("btn_page_prev"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Previous Page",
              tint = if (document.currentPage > 1) Color.White else Color.White.copy(alpha = 0.35f),
              modifier = Modifier.size(16.dp),
            )
          }

          // Page Number Indicator
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Box(
              modifier =
                Modifier.clip(RoundedCornerShape(6.dp))
                  .background(PrimaryGradient)
                  .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
              Text(
                text = "${document.currentPage}",
                style =
                  MaterialTheme.typography.labelMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 12.sp,
                  ),
              )
            }
            Text(
              text = "/",
              style =
                MaterialTheme.typography.labelMedium.copy(
                  color = Color.White.copy(alpha = 0.5f),
                  fontSize = 12.sp,
                ),
            )
            Text(
              text = "${document.totalPages}",
              style =
                MaterialTheme.typography.labelMedium.copy(
                  fontFamily = FontFamily.Monospace,
                  color = Color.White.copy(alpha = 0.8f),
                  fontSize = 12.sp,
                ),
            )
          }

          // Next Page Button
          IconButton(
            onClick = { navigateToPageDelta(1) },
            enabled = document.currentPage < document.totalPages,
            modifier = Modifier.size(32.dp).testTag("btn_page_next"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForward,
              contentDescription = "Next Page",
              tint = if (document.currentPage < document.totalPages) Color.White else Color.White.copy(alpha = 0.35f),
              modifier = Modifier.size(16.dp),
            )
          }

          VerticalDivider(
            modifier = Modifier.height(20.dp),
            color = Color.White.copy(alpha = 0.25f),
          )
        }
      }
    }
  }
}

@Composable
fun PdfPageCard(
  pageNumber: Int,
  totalPages: Int,
  preloadedBitmap: Bitmap?,
  getPageBitmap: (suspend (Int, Float) -> Bitmap?)?,
  pageIndex: Int,
  fitMode: FitMode,
  zoomScale: Float,
  isMatchedPage: Boolean,
  isCurrentMatchPage: Boolean,
  modifier: Modifier = Modifier,
) {
  var bitmap by remember(pageIndex) { mutableStateOf(preloadedBitmap) }
  var isLoading by remember(pageIndex) { mutableStateOf(preloadedBitmap == null) }

  // Re-render when zooming into new resolution tier: 0.5, 0.75, 1.0, 1.25, 1.5, 2.0, etc.
  val zoomTier = remember(zoomScale) {
    (zoomScale * 4).roundToInt().coerceIn(1, 16)
  }

  LaunchedEffect(pageIndex, zoomTier) {
    if (getPageBitmap != null) {
      if (bitmap == null) isLoading = true
      val loaded = getPageBitmap(pageIndex, zoomScale)
      if (loaded != null) {
        bitmap = loaded
      }
      isLoading = false
    } else if (preloadedBitmap != null && bitmap == null) {
      bitmap = preloadedBitmap
      isLoading = false
    }
  }

  val borderColor = when {
    isCurrentMatchPage -> BrandBlue
    isMatchedPage -> Amber500
    else -> Color(0xFFCBD5E1)
  }
  val borderWidth = if (isCurrentMatchPage) 2.5.dp else if (isMatchedPage) 1.5.dp else 1.dp

  Card(
    shape = RoundedCornerShape(8.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrentMatchPage) 8.dp else 4.dp),
    modifier = modifier
      .fillMaxWidth()
      .border(borderWidth, borderColor, RoundedCornerShape(8.dp)),
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .then(if (bitmap == null) Modifier.aspectRatio(0.707f) else Modifier),
      contentAlignment = Alignment.Center
    ) {
      if (bitmap != null) {
        Image(
          bitmap = bitmap!!.asImageBitmap(),
          contentDescription = "Page $pageNumber of $totalPages",
          modifier = Modifier.fillMaxWidth(),
          contentScale = ContentScale.FillWidth,
        )
      } else {
        // Clean placeholder while page renders
        Column(
          modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
        ) {
          if (isLoading) {
            CircularProgressIndicator(
              color = BrandBlue,
              modifier = Modifier.size(32.dp),
              strokeWidth = 2.5.dp
            )
            Spacer(modifier = Modifier.height(10.dp))
          }
          Text(
            text = "Page $pageNumber",
            style = MaterialTheme.typography.bodyMedium.copy(
              color = Slate700,
              fontWeight = FontWeight.SemiBold
            )
          )
        }
      }

      // Page counter / search match indicators top-right
      Row(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (isCurrentMatchPage) {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = BrandBlue,
            contentColor = Color.White,
          ) {
            Text(
              text = "MATCH",
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
              )
            )
          }
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = Slate900.copy(alpha = 0.78f),
          contentColor = Color.White,
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Text(
              text = "$pageNumber",
              style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = Color.White,
              ),
            )
            Text(
              text = "/",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.55f),
              ),
            )
            Text(
              text = "$totalPages",
              style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.8f),
              ),
            )
          }
        }
      }
    }
  }
}
