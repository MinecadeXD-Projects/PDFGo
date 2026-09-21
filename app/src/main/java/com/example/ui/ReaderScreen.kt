package com.example.ui

import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.Job
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.PageSpacing
import com.example.model.PdfDocument
import com.example.model.SearchState
import com.example.ui.theme.Amber500
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandCyan
import com.example.ui.theme.Emerald400
import com.example.ui.theme.PillControlGradient
import com.example.ui.theme.PrimaryGradient
import com.example.ui.theme.Rose500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import java.net.URLEncoder
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
fun ReaderScreen(
  document: PdfDocument,
  isFullscreen: Boolean,
  searchState: SearchState,
  pageSpacing: PageSpacing,
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
  onChangePage: (Int) -> Unit,
  onSetPage: (Int) -> Unit,
  onAdjustZoom: (Int) -> Unit = {},
  onSetZoomPercent: (Int) -> Unit = {},
  getPageBitmap: (suspend (Int) -> Bitmap?)? = null,
  modifier: Modifier = Modifier,
) {
  var isMenuExpanded by remember { mutableStateOf(false) }
  val lazyListState = rememberLazyListState(
    initialFirstVisibleItemIndex = (document.currentPage - 1).coerceAtLeast(0)
  )
  val coroutineScope = rememberCoroutineScope()

  // Track page aspect ratios dynamically to prevent list layout shifting when scrolling up
  val pageAspectRatios = remember(document.url) { mutableStateMapOf<Int, Float>() }
  var docAspectRatio by remember(document.url) { mutableFloatStateOf(0.707f) }

  // Two-finger pinch to zoom & pan state
  var zoomScale by remember { mutableFloatStateOf(1f) }
  var panOffset by remember { mutableStateOf(Offset.Zero) }
  var zoomAnimationJob by remember { mutableStateOf<Job?>(null) }

  val resetZoom = {
    zoomAnimationJob?.cancel()
    val startZoom = zoomScale
    val startPan = panOffset
    val targetZoom = 1.0f
    val targetPan = Offset.Zero
    zoomAnimationJob = coroutineScope.launch {
      val anim = Animatable(0f)
      anim.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
      ) {
        val p = value
        zoomScale = startZoom + (targetZoom - startZoom) * p
        panOffset = Offset(
          startPan.x + (targetPan.x - startPan.x) * p,
          startPan.y + (targetPan.y - startPan.y) * p,
        )
      }
      onSetZoomPercent(100)
    }
  }

  LaunchedEffect(document.zoomPercent) {
    val targetScale = document.zoomPercent / 100f
    if (kotlin.math.abs(zoomScale - targetScale) > 0.01f) {
      zoomScale = targetScale
      if (targetScale <= 1f) {
        panOffset = Offset.Zero
      }
    }
  }

  // System bar insets to prevent topmost and bottommost pages from being cut off by Android bars in full screen
  val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
  val statusBarHeight = systemBarsPadding.calculateTopPadding()
  val navBarHeight = systemBarsPadding.calculateBottomPadding()

  val topContentPadding = if (isFullscreen) {
    maxOf(statusBarHeight + 16.dp, 48.dp)
  } else {
    16.dp
  }

  val bottomContentPadding = if (isFullscreen) {
    maxOf(navBarHeight + 24.dp, 56.dp)
  } else {
    80.dp
  }

  // Jump immediately to initial/saved page on document load or external page reset
  LaunchedEffect(document.url, document.currentPage) {
    if (document.totalPages > 0 && !lazyListState.isScrollInProgress) {
      val targetIndex = (document.currentPage - 1).coerceIn(0, document.totalPages - 1)
      if (lazyListState.firstVisibleItemIndex != targetIndex) {
        lazyListState.scrollToItem(targetIndex)
      }
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

  var visiblePageRangeStr by remember { mutableStateOf("${document.currentPage}") }
  var isPageCounterVisible by remember { mutableStateOf(true) }
  var isZoomPillVisible by remember { mutableStateOf(false) }

  // Show page counter on page/scroll activity
  LaunchedEffect(
    document.currentPage,
    lazyListState.isScrollInProgress,
    lazyListState.firstVisibleItemScrollOffset
  ) {
    isPageCounterVisible = true
  }

  // Auto-hide page counter 3s after last activity or manual toggle
  LaunchedEffect(
    isPageCounterVisible,
    document.currentPage,
    lazyListState.isScrollInProgress,
    lazyListState.firstVisibleItemScrollOffset
  ) {
    if (isPageCounterVisible) {
      kotlinx.coroutines.delay(3000L)
      if (!lazyListState.isScrollInProgress) {
        isPageCounterVisible = false
      }
    }
  }

  // Ensure zoom pill is visible on zoom/pan activity
  LaunchedEffect(zoomScale, panOffset) {
    if (kotlin.math.abs(zoomScale - 1f) > 0.05f) {
      isZoomPillVisible = true
    }
  }

  // Auto-hide zoom pill 3s after last activity or manual toggle
  LaunchedEffect(isZoomPillVisible, zoomScale, panOffset) {
    if (kotlin.math.abs(zoomScale - 1f) > 0.05f && isZoomPillVisible) {
      kotlinx.coroutines.delay(3000L)
      isZoomPillVisible = false
    }
  }

  // Observe scroll position to update the page counter dynamically as user scrolls
  LaunchedEffect(lazyListState) {
    snapshotFlow {
      val scale = zoomScale
      val offset = panOffset
      val layoutInfo = lazyListState.layoutInfo
      val visibleItems = layoutInfo.visibleItemsInfo
      if (visibleItems.isNotEmpty()) {
        val viewportHeight = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).toFloat()
        val halfHeight = viewportHeight / 2f

        // Mathematically calculate the visible layout boundaries under zoom and translation
        val viewportTop = halfHeight - (halfHeight + offset.y) / scale
        val viewportBottom = halfHeight + (halfHeight - offset.y) / scale

        // Find all pages that are completely visible (fully inside the zoomed viewport)
        val completelyVisible = visibleItems.filter { item ->
          item.offset >= viewportTop && (item.offset + item.size) <= viewportBottom
        }

        // Calculate most visible page for standard view-tracking / persistence
        val mostVisible = visibleItems.maxByOrNull { item ->
          val itemTop = item.offset
          val itemBottom = item.offset + item.size
          maxOf(0f, minOf(itemBottom.toFloat(), viewportBottom) - maxOf(itemTop.toFloat(), viewportTop)).toInt()
        }
        val fallbackPage = (mostVisible?.index ?: lazyListState.firstVisibleItemIndex) + 1
        val coercedFallback = fallbackPage.coerceIn(1, maxOf(1, document.totalPages))

        val rangeStr = if (completelyVisible.isNotEmpty()) {
          val firstPageNum = completelyVisible.first().index + 1
          val lastPageNum = completelyVisible.last().index + 1
          if (firstPageNum == lastPageNum) {
            "$firstPageNum"
          } else {
            "$firstPageNum-$lastPageNum"
          }
        } else {
          "$coercedFallback"
        }

        Pair(coercedFallback, rangeStr)
      } else {
        val fallback = (lazyListState.firstVisibleItemIndex + 1).coerceIn(1, maxOf(1, document.totalPages))
        Pair(fallback, "$fallback")
      }
    }
      .distinctUntilChanged()
      .collect { (page, rangeStr) ->
        visiblePageRangeStr = rangeStr
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

  var showJumpDialog by remember { mutableStateOf(false) }
  var jumpInputText by remember { mutableStateOf("") }

  val jumpToPage: (Int) -> Unit = { pageNum ->
    if (document.totalPages > 0) {
      val targetPage = pageNum.coerceIn(1, document.totalPages)
      val targetIdx = targetPage - 1
      onSetPage(targetPage)
      coroutineScope.launch {
        lazyListState.scrollToItem(targetIdx)
      }
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
                        text = "Remove PDF",
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

      Box(
        modifier =
          Modifier.weight(1f)
            .fillMaxWidth()
            .clipToBounds()
            .pointerInput(Unit) {
              detectTapGestures(
                onDoubleTap = { tapOffset ->
                  zoomAnimationJob?.cancel()
                  val targetZoom: Float
                  val targetPan: Offset

                  if (zoomScale < 0.98f) {
                    // Below 100% -> Smoothly zoom to 100%
                    targetZoom = 1.0f
                    targetPan = Offset.Zero
                  } else if (zoomScale <= 1.05f) {
                    // At ~100% -> Smoothly zoom to 220% centered exactly at double-tap position
                    targetZoom = 2.2f
                    val centroidRelative = tapOffset - Offset(size.width / 2f, size.height / 2f)
                    val scaleFactor = targetZoom - 1f
                    val maxPanX = (size.width * scaleFactor) / 2f
                    val maxPanY = (size.height * scaleFactor) / 2f
                    targetPan = Offset(
                      (-centroidRelative.x * scaleFactor).coerceIn(-maxPanX, maxPanX),
                      (-centroidRelative.y * scaleFactor).coerceIn(-maxPanY, maxPanY),
                    )
                  } else {
                    // Anywhere from 100% to 600% -> Smoothly zoom back to 100%
                    targetZoom = 1.0f
                    targetPan = Offset.Zero
                  }

                  val startZoom = zoomScale
                  val startPan = panOffset

                  zoomAnimationJob = coroutineScope.launch {
                    val anim = Animatable(0f)
                    anim.animateTo(
                      targetValue = 1f,
                      animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                    ) {
                      val p = value
                      zoomScale = startZoom + (targetZoom - startZoom) * p
                      panOffset = Offset(
                        startPan.x + (targetPan.x - startPan.x) * p,
                        startPan.y + (targetPan.y - startPan.y) * p,
                      )
                    }
                    onSetZoomPercent((targetZoom * 100).toInt())
                  }
                },
                onTap = {
                  isPageCounterVisible = !isPageCounterVisible
                  if (kotlin.math.abs(zoomScale - 1f) > 0.05f) {
                    isZoomPillVisible = !isZoomPillVisible
                  }
                }
              )
            }
            .pointerInput(Unit) {
              awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                zoomAnimationJob?.cancel()
                var didZoom = false
                val initialZoomScale = zoomScale

                do {
                  val event = awaitPointerEvent()
                  val activePointers = event.changes.filter { it.pressed }

                  if (activePointers.size >= 2) {
                    // Two fingers: fluid pinch-to-zoom & pan across the document
                    val zoomChange = event.calculateZoom()
                    val panChange = event.calculatePan()

                    if (kotlin.math.abs(zoomChange - 1f) > 0.002f) {
                      didZoom = true
                    }

                    val targetScale = zoomScale * zoomChange
                    val newScale = targetScale.coerceIn(0.5f, 6.0f)
                    val actualZoomChange = if (zoomScale > 0f) newScale / zoomScale else 1f

                    if (kotlin.math.abs(newScale - 1f) <= 0.02f) {
                      zoomScale = 1f
                      panOffset = Offset.Zero
                    } else if (newScale > 1f) {
                      val centroid = event.calculateCentroid(useCurrent = true)
                      val centroidRelative = centroid - Offset(size.width / 2f, size.height / 2f)

                      val maxPanX = (size.width * (newScale - 1f)) / 2f
                      val maxPanY = (size.height * (newScale - 1f)) / 2f

                      // Mathematical zoom focus centering (translates pan offset to anchor zoom at gesture centroid using actualZoomChange)
                      val newX = (panOffset.x * actualZoomChange + panChange.x + centroidRelative.x * (1f - actualZoomChange)).coerceIn(-maxPanX, maxPanX)
                      val newY = (panOffset.y * actualZoomChange + panChange.y + centroidRelative.y * (1f - actualZoomChange)).coerceIn(-maxPanY, maxPanY)

                      zoomScale = newScale
                      panOffset = Offset(newX, newY)
                    } else {
                      // Zoomed out (< 1f): keep pages centered horizontally and vertically
                      zoomScale = newScale
                      panOffset = Offset.Zero
                    }
                    event.changes.forEach {
                      if (it.positionChanged()) it.consume()
                    }
                  } else if (activePointers.size == 1 && zoomScale > 1.05f) {
                    // One finger when zoomed in: pan horizontally and vertically, allowing vertical scroll across pages
                    val panChange = event.calculatePan()
                    val maxPanX = (size.width * (zoomScale - 1f)) / 2f
                    val maxPanY = (size.height * (zoomScale - 1f)) / 2f
                    val newX = (panOffset.x + panChange.x).coerceIn(-maxPanX, maxPanX)
                    val newY = (panOffset.y + panChange.y).coerceIn(-maxPanY, maxPanY)
                    
                    val verticalRemainder = (panOffset.y + panChange.y) - newY
                    panOffset = Offset(newX, newY)

                    // If at vertical pan boundary, dispatch remainder to LazyColumn
                    if (kotlin.math.abs(verticalRemainder) > 0.5f) {
                      coroutineScope.launch {
                        lazyListState.scrollBy(-verticalRemainder)
                      }
                    }

                    // Always consume horizontal movement so page horizontal pan is smooth
                    event.changes.forEach {
                      if (it.positionChanged()) it.consume()
                    }
                  }
                  // When activePointers.size == 1 and zoomScale <= 1.05f:
                  // Nothing is consumed, so single finger vertical scrolling on LazyColumn works seamlessly!
                } while (event.changes.any { it.pressed })

                // Re-render clear bitmap only when user finishes zooming and removes fingers
                if (didZoom && kotlin.math.abs(zoomScale - initialZoomScale) > 0.05f) {
                  val targetZoomPercent = (zoomScale * 100).toInt().coerceIn(50, 600)
                  onSetZoomPercent(targetZoomPercent)
                }
              }
            },
        contentAlignment = Alignment.TopCenter,
      ) {
        Box(
          modifier =
            Modifier.fillMaxSize()
              .graphicsLayer {
                if (zoomScale > 1f) {
                  scaleX = zoomScale
                  scaleY = zoomScale
                  translationX = panOffset.x
                  translationY = panOffset.y
                } else {
                  scaleX = 1f
                  scaleY = 1f
                  translationX = 0f
                  translationY = 0f
                }
              }
        ) {
          if (document.totalPages > 0 && !document.useWebViewFallback) {
            // Native PdfRenderer pages from user's PDF (all pages rendered dynamically on-demand)
            LazyColumn(
              state = lazyListState,
              modifier = Modifier.fillMaxSize(),
              contentPadding = PaddingValues(
                top = topContentPadding,
                bottom = bottomContentPadding,
                start = 12.dp,
                end = 12.dp,
              ),
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

                val pageRatio = pageAspectRatios[index] ?: docAspectRatio
                PdfPageCard(
                  pageNumber = pageNum,
                  zoomPercent = document.zoomPercent,
                  zoomScale = zoomScale,
                  preloadedBitmap = document.pageBitmaps.getOrNull(index),
                  getPageBitmap = getPageBitmap,
                  pageIndex = index,
                  isMatchedPage = isMatchedPage,
                  isCurrentMatchPage = isCurrentMatchPage,
                  initialAspectRatio = pageRatio,
                  onAspectRatioLoaded = { ratio ->
                    if (ratio > 0.1f) {
                      pageAspectRatios[index] = ratio
                      if (docAspectRatio == 0.707f) {
                        docAspectRatio = ratio
                      }
                    }
                  }
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
              modifier = Modifier
                .fillMaxSize()
                .padding(
                  top = if (isFullscreen) topContentPadding else 0.dp,
                  bottom = if (isFullscreen) bottomContentPadding else 0.dp,
                ),
            )
          } else {
            // Fallback document card for testing / unit test environments
            Box(
              modifier = Modifier
                .fillMaxSize()
                .padding(
                  top = if (isFullscreen) topContentPadding else 0.dp,
                  bottom = if (isFullscreen) bottomContentPadding else 0.dp,
                ),
              contentAlignment = Alignment.Center,
            ) {
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
      }

        // Floating Reset Zoom Pill at Top Right when zoomed in or out (not 100%)
        androidx.compose.animation.AnimatedVisibility(
          visible = kotlin.math.abs(zoomScale - 1f) > 0.05f && isZoomPillVisible,
          enter = fadeIn(),
          exit = fadeOut(),
          modifier = Modifier.align(Alignment.TopEnd)
        ) {
          Surface(
            onClick = resetZoom,
            shape = RoundedCornerShape(50.dp),
            color = Slate900.copy(alpha = 0.9f),
            contentColor = Color.White,
            shadowElevation = 6.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
            modifier = Modifier.padding(
              top = if (isFullscreen) maxOf(statusBarHeight + 12.dp, 40.dp) else 16.dp,
              end = 16.dp,
              start = 16.dp,
              bottom = 16.dp
            ).testTag("btn_reset_zoom"),
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
                    color = if (zoomScale < 0.95f) BrandCyan else Emerald400,
                    fontSize = 11.sp,
                  ),
              )
              Text(
                text = "Tap to reset",
                style =
                  MaterialTheme.typography.labelSmall.copy(
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                  ),
              )
            }
          }
        }
      }
    }

    // Floating Bottom Bar (Controls)
    AnimatedVisibility(
      visible = isPageCounterVisible,
      enter = slideInVertically { it } + fadeIn(),
      exit = slideOutVertically { it } + fadeOut(),
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(
          bottom = if (isFullscreen) maxOf(navBarHeight + 16.dp, 24.dp) else 16.dp
        ),
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
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .clickable {
                jumpInputText = "${document.currentPage}"
                showJumpDialog = true
              }
              .padding(horizontal = 6.dp, vertical = 2.dp)
              .testTag("btn_page_indicator"),
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
                text = visiblePageRangeStr,
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
        }
      }
    }

    if (showJumpDialog) {
      AlertDialog(
        onDismissRequest = { showJumpDialog = false },
        containerColor = Color(0xFF1E293B), // Slate800ish
        titleContentColor = Color.White,
        textContentColor = Color.White.copy(alpha = 0.9f),
        title = { Text("Jump to Page") },
        text = {
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text(
              text = "Enter page number (1 - ${document.totalPages}):",
              style = MaterialTheme.typography.bodyMedium
            )
            OutlinedTextField(
              value = jumpInputText,
              onValueChange = { jumpInputText = it },
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = BrandBlue,
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                cursorColor = BrandBlue,
              ),
              modifier = Modifier.fillMaxWidth().testTag("input_jump_page")
            )
          }
        },
        confirmButton = {
          TextButton(
            onClick = {
              showJumpDialog = false
              val pageNum = jumpInputText.toIntOrNull() ?: document.currentPage
              val clamped = pageNum.coerceIn(1, maxOf(1, document.totalPages))
              jumpToPage(clamped)
            },
            modifier = Modifier.testTag("btn_jump_confirm")
          ) {
            Text("Go", color = BrandBlue)
          }
        },
        dismissButton = {
          TextButton(
            onClick = { showJumpDialog = false },
            modifier = Modifier.testTag("btn_jump_cancel")
          ) {
            Text("Cancel", color = Color.White.copy(alpha = 0.7f))
          }
        },
        shape = RoundedCornerShape(16.dp)
      )
    }
  }
}

@Composable
fun PdfPageCard(
  pageNumber: Int,
  zoomPercent: Int = 100,
  zoomScale: Float = 1f,
  preloadedBitmap: Bitmap?,
  getPageBitmap: (suspend (Int) -> Bitmap?)?,
  pageIndex: Int,
  isMatchedPage: Boolean,
  isCurrentMatchPage: Boolean,
  initialAspectRatio: Float,
  onAspectRatioLoaded: (Float) -> Unit,
  modifier: Modifier = Modifier,
) {
  var bitmap by remember(pageIndex, preloadedBitmap) { mutableStateOf(preloadedBitmap) }
  var displayedBitmap by remember(pageIndex) { mutableStateOf(preloadedBitmap) }
  var isLoading by remember(pageIndex, preloadedBitmap) { mutableStateOf(preloadedBitmap == null) }

  LaunchedEffect(pageIndex, zoomPercent) {
    if (getPageBitmap != null) {
      if (bitmap == null) {
        isLoading = true
      }
      val loaded = getPageBitmap(pageIndex)
      if (loaded != null) {
        bitmap = loaded
        displayedBitmap = loaded
      }
      isLoading = false
    } else if (preloadedBitmap != null) {
      bitmap = preloadedBitmap
      displayedBitmap = preloadedBitmap
      isLoading = false
    }
  }

  val aspectRatio = if (displayedBitmap != null) {
    val ratio = displayedBitmap!!.width.toFloat() / displayedBitmap!!.height.toFloat()
    if (ratio > 0.1f) {
      onAspectRatioLoaded(ratio)
    }
    ratio
  } else {
    initialAspectRatio
  }

  val borderColor = when {
    isCurrentMatchPage -> BrandBlue
    isMatchedPage -> Amber500
    else -> Color(0xFFCBD5E1)
  }
  val borderWidth = if (isCurrentMatchPage) 2.5.dp else if (isMatchedPage) 1.5.dp else 1.dp

  val effectiveWidthFraction = if (zoomScale < 1f) zoomScale else 1f
  val maxBaseWidth = 680.dp
  val effectiveMaxWidth = maxBaseWidth * effectiveWidthFraction

  Card(
    shape = RoundedCornerShape(8.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrentMatchPage) 8.dp else 4.dp),
    modifier = modifier
      .widthIn(max = effectiveMaxWidth)
      .fillMaxWidth(effectiveWidthFraction)
      .border(borderWidth, borderColor, RoundedCornerShape(8.dp)),
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(aspectRatio),
      contentAlignment = Alignment.Center
    ) {
      if (displayedBitmap != null) {
        Image(
          bitmap = displayedBitmap!!.asImageBitmap(),
          contentDescription = "Page $pageNumber",
          modifier = Modifier.fillMaxWidth(),
          contentScale = ContentScale.FillWidth,
        )
      } else {
        // Clean placeholder only when no bitmap has ever loaded (initial load)
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
          shape = RoundedCornerShape(6.dp),
          color = Slate900.copy(alpha = 0.75f),
          contentColor = Color.White,
        ) {
          Text(
            text = "$pageNumber",
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
              fontSize = 10.sp,
              color = Color.White,
            ),
          )
        }
      }
    }
  }
}
