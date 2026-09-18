package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FitMode
import com.example.model.PageSpacing
import com.example.model.PdfDocument
import com.example.model.SearchState
import com.example.ui.theme.Amber400
import com.example.ui.theme.Amber500
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Rose500
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900

@Composable
fun ReaderScreen(
  document: PdfDocument,
  isFullscreen: Boolean,
  searchState: SearchState,
  fitMode: FitMode,
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
  onToggleFitMode: () -> Unit,
  onChangePage: (Int) -> Unit,
  onSetPage: (Int) -> Unit,
  onAdjustZoom: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  var isMenuExpanded by remember { mutableStateOf(false) }
  var isEditingPage by remember { mutableStateOf(false) }
  var pageInputText by remember(document.currentPage) {
    mutableStateOf(document.currentPage.toString())
  }
  val scrollState = rememberScrollState()

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(if (MaterialTheme.colorScheme.background == Color(0xFFF8FAFC)) Color(0xFFE2E8F0) else Color(0xFF0C101A)),
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

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = document.title,
                  style =
                    MaterialTheme.typography.titleSmall.copy(
                      fontWeight = FontWeight.SemiBold,
                      color = MaterialTheme.colorScheme.onSurface,
                    ),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Text(
                    text = "${document.totalPages} Pages",
                    style =
                      MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                      ),
                  )
                  Text(
                    text = "•",
                    style =
                      MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                      ),
                  )
                  Text(
                    text = "Active",
                    style =
                      MaterialTheme.typography.bodySmall.copy(
                        color = Emerald500,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                      ),
                  )
                }
              }
            }

            // Toolbar action buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(
                onClick = onOpenSearch,
                modifier = Modifier.testTag("btn_search"),
              ) {
                Icon(
                  imageVector = Icons.Default.Search,
                  contentDescription = "Search",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(20.dp),
                )
              }

              IconButton(
                onClick = onOpenDownloadModal,
                modifier = Modifier.testTag("btn_download"),
              ) {
                Icon(
                  imageVector = Icons.Default.Download,
                  contentDescription = "Download PDF",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(20.dp),
                )
              }

              IconButton(
                onClick = onToggleFullscreen,
                modifier = Modifier.testTag("btn_fullscreen"),
              ) {
                Icon(
                  imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                  contentDescription = "Toggle Fullscreen",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(20.dp),
                )
              }

              Box {
                IconButton(
                  onClick = { isMenuExpanded = !isMenuExpanded },
                  modifier = Modifier.testTag("btn_more_options"),
                ) {
                  Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More Options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                  )
                }

                DropdownMenu(
                  expanded = isMenuExpanded,
                  onDismissRequest = { isMenuExpanded = false },
                  modifier =
                    Modifier.background(MaterialTheme.colorScheme.surface)
                      .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        RoundedCornerShape(12.dp),
                      ),
                ) {
                  DropdownMenuItem(
                    text = { Text("Reader Settings") },
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
                  DropdownMenuItem(
                    text = { Text(fitMode.label) },
                    leadingIcon = {
                      Icon(
                        imageVector = Icons.Default.AspectRatio,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                      )
                    },
                    onClick = {
                      isMenuExpanded = false
                      onToggleFitMode()
                    },
                  )
                  HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
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
                  modifier =
                    Modifier.weight(1f)
                      .height(44.dp)
                      .testTag("input_search"),
                )

                IconButton(
                  onClick = { onSearchNavigate(-1) },
                  modifier =
                    Modifier.size(36.dp)
                      .clip(RoundedCornerShape(8.dp))
                      .background(MaterialTheme.colorScheme.surface)
                      .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        RoundedCornerShape(8.dp),
                      )
                      .testTag("btn_search_prev"),
                ) {
                  Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Previous match",
                    modifier = Modifier.size(18.dp),
                  )
                }

                IconButton(
                  onClick = { onSearchNavigate(1) },
                  modifier =
                    Modifier.size(36.dp)
                      .clip(RoundedCornerShape(8.dp))
                      .background(MaterialTheme.colorScheme.surface)
                      .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        RoundedCornerShape(8.dp),
                      )
                      .testTag("btn_search_next"),
                ) {
                  Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Next match",
                    modifier = Modifier.size(18.dp),
                  )
                }

                IconButton(
                  onClick = onCloseSearch,
                  modifier = Modifier.size(36.dp).testTag("btn_search_close"),
                ) {
                  Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close search",
                    modifier = Modifier.size(18.dp),
                  )
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
        }
      }

      // Main PDF Viewport (Multi-page document view)
      val scaleFactor = document.zoomPercent / 100f
      val pageGap = pageSpacing.dpValue.dp

      Box(
        modifier =
          Modifier.weight(1f)
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(vertical = 16.dp, horizontal = 12.dp),
        contentAlignment = Alignment.TopCenter,
      ) {
        Column(
          modifier =
            Modifier.widthIn(max = 520.dp)
              .scale(scaleFactor),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(pageGap),
        ) {
          // Document Page 1
          PdfDocumentPageOne(
            searchQuery = if (searchState.isOpen) searchState.query else "",
            pageNumber = document.currentPage,
            totalPages = document.totalPages,
          )

          // Document Page 2
          PdfDocumentPageTwo(
            searchQuery = if (searchState.isOpen) searchState.query else "",
            pageNumber = (document.currentPage + 1).coerceAtMost(document.totalPages),
            totalPages = document.totalPages,
          )
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
        color = Slate900.copy(alpha = 0.9f),
        contentColor = Color.White,
        shadowElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Exit Fullscreen",
            tint = BrandBlue,
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

    // Floating Bottom Bar with Page Navigation & Zoom
    AnimatedVisibility(
      visible = !isFullscreen,
      enter = slideInVertically { it } + fadeIn(),
      exit = slideOutVertically { it } + fadeOut(),
      modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
    ) {
      Surface(
        shape = RoundedCornerShape(24.dp),
        color = Slate900.copy(alpha = 0.92f),
        contentColor = Color.White,
        shadowElevation = 10.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          // Previous Page Button
          IconButton(
            onClick = { onChangePage(-1) },
            modifier = Modifier.size(32.dp).testTag("btn_page_prev"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Previous Page",
              tint = Color.White,
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
                  .background(Slate800)
                  .padding(horizontal = 8.dp, vertical = 4.dp)
                  .clickable { isEditingPage = !isEditingPage },
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
            onClick = { onChangePage(1) },
            modifier = Modifier.size(32.dp).testTag("btn_page_next"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForward,
              contentDescription = "Next Page",
              tint = Color.White,
              modifier = Modifier.size(16.dp),
            )
          }

          VerticalDivider(
            modifier = Modifier.height(18.dp),
            color = Slate800,
            thickness = 1.dp,
          )

          // Zoom Controls
          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
              onClick = { onAdjustZoom(-10) },
              modifier = Modifier.size(28.dp).testTag("btn_zoom_out"),
            ) {
              Text(
                text = "－",
                color = Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
              )
            }
            Text(
              text = "${document.zoomPercent}%",
              style =
                MaterialTheme.typography.labelSmall.copy(
                  fontFamily = FontFamily.Monospace,
                  color = Color.White,
                  fontSize = 11.sp,
                ),
              modifier = Modifier.padding(horizontal = 2.dp),
            )
            IconButton(
              onClick = { onAdjustZoom(10) },
              modifier = Modifier.size(28.dp).testTag("btn_zoom_in"),
            ) {
              Text(
                text = "＋",
                color = Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun PdfDocumentPageOne(
  searchQuery: String,
  pageNumber: Int,
  totalPages: Int,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier =
      modifier
        .fillMaxWidth()
        .shadow(
          elevation = 8.dp,
          shape = RoundedCornerShape(10.dp),
          ambientColor = Color.Black.copy(alpha = 0.4f),
          spotColor = Color.Black.copy(alpha = 0.4f),
        ),
    shape = RoundedCornerShape(10.dp),
    color = Color.White,
    contentColor = Color(0xFF0F172A),
  ) {
    Column(
      modifier = Modifier.padding(24.dp),
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "DEPARTMENT OF APPLIED PHYSICS",
          style =
            MaterialTheme.typography.labelSmall.copy(
              fontFamily = FontFamily.Monospace,
              color = Color(0xFF64748B),
              fontSize = 9.sp,
              letterSpacing = 1.sp,
            ),
        )
        Text(
          text = "LECTURE NOTES • SEC 04",
          style =
            MaterialTheme.typography.labelSmall.copy(
              fontFamily = FontFamily.Monospace,
              color = Color(0xFF64748B),
              fontSize = 9.sp,
            ),
        )
      }

      HorizontalDivider(
        color = Color(0xFFCBD5E1),
        thickness = 1.dp,
        modifier = Modifier.padding(vertical = 10.dp),
      )

      // Title
      Text(
        text = "1. Principles of Wavefunction Mechanics",
        style =
          MaterialTheme.typography.titleLarge.copy(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF020617),
            fontSize = 20.sp,
          ),
      )

      Text(
        text = "Author: Dr. H. Vance • Academic Year 2024-2025",
        style =
          MaterialTheme.typography.bodySmall.copy(
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            color = Color(0xFF475569),
            fontSize = 11.sp,
          ),
        modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
      )

      // Abstract Box
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFFF8FAFC))
            .border(
              width = 2.dp,
              color = BrandBlue,
              shape = RoundedCornerShape(4.dp),
            )
            .padding(12.dp),
      ) {
        val abstractText = buildAnnotatedString {
          withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))) {
            append("Abstract: ")
          }
          append("This chapter introduces the foundational mathematical axioms governing state vectors in Hilbert space, emphasizing boundary behavior in potential wells.")
        }
        Text(
          text = abstractText,
          style =
            MaterialTheme.typography.bodySmall.copy(
              color = Color(0xFF334155),
              fontSize = 11.sp,
              lineHeight = 16.sp,
            ),
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Paragraph 1 with search highlight
      HighlightedParagraph(
        fullText =
          "In non-relativistic physics, the physical state of a physical system is represented at any given instant by a state vector. The fundamental postulate of quantum theory establishes that the probability amplitude evolves deterministically according to the time-dependent Schrödinger equation:",
        searchQuery = searchQuery,
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Math Formula Box
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFF1F5F9))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "iℏ ∂/∂t |Ψ(t)⟩ = Ĥ |Ψ(t)⟩",
          style =
            MaterialTheme.typography.bodyMedium.copy(
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF0F172A),
              fontSize = 13.sp,
            ),
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Paragraph 2 with search highlight
      HighlightedParagraph(
        fullText =
          "Here, Ĥ denotes the Hamiltonian operator corresponding to total energy. When subjected to stationary conditions, eigenstates exhibit harmonic oscillations with distinct energy eigenvalues. In modern quantum computing topologies, these superposition states represent qubit phase distributions.",
        searchQuery = searchQuery,
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Standing Wave Diagram Simulation
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(
              1.dp,
              Color(0xFFCBD5E1),
              RoundedCornerShape(8.dp),
            )
            .background(Color(0xFFF8FAFC))
            .padding(12.dp),
        contentAlignment = Alignment.Center,
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Canvas(
            modifier = Modifier.fillMaxWidth().height(48.dp),
          ) {
            val width = size.width
            val height = size.height
            val midY = height / 2

            // Baseline dashed line
            drawLine(
              color = Color(0xFF94A3B8),
              start = Offset(0f, midY),
              end = Offset(width, midY),
              strokeWidth = 2f,
              pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f),
            )

            // Sine wave harmonic
            val path = Path()
            path.moveTo(0f, midY)
            val segments = 80
            for (i in 0..segments) {
              val x = (i.toFloat() / segments) * width
              val angle = (i.toFloat() / segments) * 4 * Math.PI
              val y = midY + (Math.sin(angle) * (height * 0.38f)).toFloat()
              path.lineTo(x, y)
            }

            drawPath(
              path = path,
              color = BrandBlue,
              style = Stroke(width = 3.5f),
            )
          }

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = "Figure 1.1: Standing wave harmonics in finite box",
            style =
              MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF64748B),
                fontSize = 9.sp,
              ),
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      HorizontalDivider(
        color = Color(0xFFE2E8F0),
        thickness = 1.dp,
      )

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "PDFGo Reading Mode",
          style =
            MaterialTheme.typography.labelSmall.copy(
              fontFamily = FontFamily.Monospace,
              color = Color(0xFF94A3B8),
              fontSize = 9.sp,
            ),
        )
        Text(
          text = "Page $pageNumber of $totalPages",
          style =
            MaterialTheme.typography.labelSmall.copy(
              fontFamily = FontFamily.Monospace,
              color = Color(0xFF94A3B8),
              fontSize = 9.sp,
            ),
        )
      }
    }
  }
}

@Composable
private fun PdfDocumentPageTwo(
  searchQuery: String,
  pageNumber: Int,
  totalPages: Int,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier =
      modifier
        .fillMaxWidth()
        .shadow(
          elevation = 8.dp,
          shape = RoundedCornerShape(10.dp),
          ambientColor = Color.Black.copy(alpha = 0.4f),
          spotColor = Color.Black.copy(alpha = 0.4f),
        ),
    shape = RoundedCornerShape(10.dp),
    color = Color.White,
    contentColor = Color(0xFF0F172A),
  ) {
    Column(
      modifier = Modifier.padding(24.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "2. OPERATOR ALGEBRA & OBSERVABLES",
          style =
            MaterialTheme.typography.labelSmall.copy(
              fontFamily = FontFamily.Monospace,
              color = Color(0xFF64748B),
              fontSize = 9.sp,
              letterSpacing = 1.sp,
            ),
        )
        Text(
          text = "PAGE $pageNumber",
          style =
            MaterialTheme.typography.labelSmall.copy(
              fontFamily = FontFamily.Monospace,
              color = Color(0xFF64748B),
              fontSize = 9.sp,
            ),
        )
      }

      HorizontalDivider(
        color = Color(0xFFCBD5E1),
        thickness = 1.dp,
        modifier = Modifier.padding(vertical = 10.dp),
      )

      Text(
        text = "2.1 Hermitian Operators and Expectation Values",
        style =
          MaterialTheme.typography.titleMedium.copy(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF020617),
            fontSize = 16.sp,
          ),
      )

      Spacer(modifier = Modifier.height(10.dp))

      HighlightedParagraph(
        fullText =
          "Every physically measurable dynamical variable is associated with a linear Hermitian operator whose eigenvalues are strictly real numbers. Commutation relations between conjugate variables dictate the Heisenberg uncertainty bound:",
        searchQuery = searchQuery,
      )

      Spacer(modifier = Modifier.height(10.dp))

      Box(
        modifier =
          Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFF1F5F9))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "[x̂, p̂] = iℏ",
          style =
            MaterialTheme.typography.bodyMedium.copy(
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF0F172A),
              fontSize = 13.sp,
            ),
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      HighlightedParagraph(
        fullText =
          "Consequently, measurement collapses the wavefunction into the corresponding eigenbasis, illustrating non-classical measurement thermodynamics in modern quantum state tomography.",
        searchQuery = searchQuery,
      )

      Spacer(modifier = Modifier.height(24.dp))

      HorizontalDivider(
        color = Color(0xFFE2E8F0),
        thickness = 1.dp,
      )

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "PDFGo Reading Mode",
          style =
            MaterialTheme.typography.labelSmall.copy(
              fontFamily = FontFamily.Monospace,
              color = Color(0xFF94A3B8),
              fontSize = 9.sp,
            ),
        )
        Text(
          text = "Page $pageNumber of $totalPages",
          style =
            MaterialTheme.typography.labelSmall.copy(
              fontFamily = FontFamily.Monospace,
              color = Color(0xFF94A3B8),
              fontSize = 9.sp,
            ),
        )
      }
    }
  }
}

@Composable
private fun HighlightedParagraph(
  fullText: String,
  searchQuery: String,
  modifier: Modifier = Modifier,
) {
  val annotated = buildAnnotatedString {
    if (searchQuery.isNotBlank() && fullText.contains(searchQuery, ignoreCase = true)) {
      var startIndex = 0
      val lowerFull = fullText.lowercase()
      val lowerQuery = searchQuery.lowercase()

      while (startIndex < fullText.length) {
        val foundIndex = lowerFull.indexOf(lowerQuery, startIndex)
        if (foundIndex != -1) {
          append(fullText.substring(startIndex, foundIndex))
          withStyle(
            SpanStyle(
              background = Amber400,
              color = Color(0xFF020617),
              fontWeight = FontWeight.Bold,
            ),
          ) {
            append(fullText.substring(foundIndex, foundIndex + searchQuery.length))
          }
          startIndex = foundIndex + searchQuery.length
        } else {
          append(fullText.substring(startIndex))
          break
        }
      }
    } else {
      append(fullText)
    }
  }

  Text(
    text = annotated,
    style =
      MaterialTheme.typography.bodySmall.copy(
        fontFamily = FontFamily.Serif,
        color = Color(0xFF1E293B),
        fontSize = 12.sp,
        lineHeight = 18.sp,
        textAlign = TextAlign.Justify,
      ),
    modifier = modifier,
  )
}
