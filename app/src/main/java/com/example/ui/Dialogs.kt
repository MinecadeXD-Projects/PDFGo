package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.DownloadModalState
import com.example.ui.theme.Amber500
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.DangerGradient
import com.example.ui.theme.Emerald500
import com.example.ui.theme.PrimaryGradient
import com.example.ui.theme.Rose500

@Composable
fun DownloadPdfDialog(
  state: DownloadModalState,
  onFilenameChange: (String) -> Unit,
  onStartDownload: () -> Unit,
  onDismiss: () -> Unit,
) {
  if (!state.isOpen) return

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
      modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.Top,
        ) {
          Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
              text = "Download PDF",
              style =
                MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                ),
            )
            Text(
              text = "Save a local offline copy to your device",
              style =
                MaterialTheme.typography.bodySmall.copy(
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  fontSize = 12.sp,
                ),
            )
          }

          Box(
            modifier =
              Modifier.size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(BrandBlue.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Default.Download,
              contentDescription = null,
              tint = BrandBlue,
              modifier = Modifier.size(20.dp),
            )
          }
        }

        // Filename Input
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "Filename",
            style =
              MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
              ),
          )
          OutlinedTextField(
            value = state.filename,
            onValueChange = onFilenameChange,
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            enabled = !state.isDownloading,
            textStyle =
              MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
              ),
            colors =
              OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrandBlue,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
              ),
            modifier = Modifier.fillMaxWidth().testTag("input_download_filename"),
          )
        }

        // Location Box
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
              .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                RoundedCornerShape(12.dp),
              )
              .padding(12.dp),
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
              )
              Text(
                text = "Storage Location:",
                style =
                  MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  ),
              )
            }
            Text(
              text = "Downloads/PDFGo/",
              style =
                MaterialTheme.typography.bodySmall.copy(
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.SemiBold,
                  color = BrandBlue,
                  fontSize = 12.sp,
                ),
              modifier = Modifier.padding(start = 22.dp),
            )
          }
        }

        // Progress indicator
        if (state.isDownloading) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = if (state.isCompleted) "PDF downloaded successfully" else "Downloading… ${state.progressPercent}%",
                style =
                  MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (state.isCompleted) Emerald500 else MaterialTheme.colorScheme.onSurface,
                  ),
              )
              Text(
                text = state.progressBytes,
                style =
                  MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                  ),
              )
            }

            LinearProgressIndicator(
              progress = { state.progressPercent / 100f },
              modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50.dp)),
              color = if (state.isCompleted) Emerald500 else BrandBlue,
              trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            )
          }
        }

        // Actions
        if (!state.isDownloading) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            TextButton(
              onClick = onDismiss,
              shape = RoundedCornerShape(10.dp),
            ) {
              Text("Cancel")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
              modifier =
                Modifier.shadow(4.dp, RoundedCornerShape(10.dp), spotColor = BrandBlue)
                  .clip(RoundedCornerShape(10.dp))
                  .background(PrimaryGradient)
                  .clickable { onStartDownload() }
                  .padding(horizontal = 16.dp, vertical = 9.dp)
                  .testTag("btn_confirm_download"),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = "Download",
                style =
                  MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                  ),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun RemovePdfDialog(
  isOpen: Boolean,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  if (!isOpen) return

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
      modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Row(
          verticalAlignment = Alignment.Top,
          horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Box(
            modifier =
              Modifier.size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Amber500.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Default.Warning,
              contentDescription = null,
              tint = Amber500,
              modifier = Modifier.size(24.dp),
            )
          }

          Column {
            Text(
              text = "Remove PDF from PDFGo?",
              style =
                MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                ),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text =
                "This removes the saved link and reading state from PDFGo. Any downloaded copy will NOT be deleted.",
              style =
                MaterialTheme.typography.bodySmall.copy(
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  lineHeight = 18.sp,
                ),
            )
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          TextButton(
            onClick = onDismiss,
            shape = RoundedCornerShape(10.dp),
          ) {
            Text("Cancel")
          }
          Spacer(modifier = Modifier.width(8.dp))
          Box(
            modifier =
              Modifier.shadow(4.dp, RoundedCornerShape(10.dp), spotColor = Rose500)
                .clip(RoundedCornerShape(10.dp))
                .background(DangerGradient)
                .clickable { onConfirm() }
                .padding(horizontal = 16.dp, vertical = 9.dp)
                .testTag("btn_confirm_remove"),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "Remove",
              style =
                MaterialTheme.typography.labelLarge.copy(
                  fontWeight = FontWeight.SemiBold,
                  color = Color.White,
                ),
            )
          }
        }
      }
    }
  }
}

@Composable
fun ExitAppDialog(
  isOpen: Boolean,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  if (!isOpen) return

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
      modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Row(
          verticalAlignment = Alignment.Top,
          horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Box(
            modifier =
              Modifier.size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Default.ExitToApp,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(24.dp),
            )
          }

          Column {
            Text(
              text = "Exit PDFGo?",
              style =
                MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                ),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Are you sure you want to close PDFGo?",
              style =
                MaterialTheme.typography.bodySmall.copy(
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          TextButton(
            onClick = onDismiss,
            shape = RoundedCornerShape(10.dp),
          ) {
            Text("Cancel")
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = onConfirm,
            shape = RoundedCornerShape(10.dp),
            colors =
              ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
              ),
            modifier = Modifier.testTag("btn_confirm_exit"),
          ) {
            Text("Exit")
          }
        }
      }
    }
  }
}

@Composable
fun PasswordProtectedDialog(
  isOpen: Boolean,
  onUnlock: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  if (!isOpen) return
  var password by remember { mutableStateOf("") }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
      modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Row(
          verticalAlignment = Alignment.Top,
          horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Box(
            modifier =
              Modifier.size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BrandBlue.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Default.Lock,
              contentDescription = null,
              tint = BrandBlue,
              modifier = Modifier.size(24.dp),
            )
          }

          Column {
            Text(
              text = "Password Protected",
              style =
                MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                ),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "This document is encrypted. Enter password to open.",
              style =
                MaterialTheme.typography.bodySmall.copy(
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
          }
        }

        OutlinedTextField(
          value = password,
          onValueChange = { password = it },
          placeholder = { Text("Enter document password", fontSize = 12.sp) },
          singleLine = true,
          visualTransformation = PasswordVisualTransformation(),
          shape = RoundedCornerShape(12.dp),
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedBorderColor = BrandBlue,
              unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            ),
          modifier = Modifier.fillMaxWidth(),
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          TextButton(
            onClick = onDismiss,
            shape = RoundedCornerShape(10.dp),
          ) {
            Text("Cancel")
          }
          Spacer(modifier = Modifier.width(8.dp))
          Box(
            modifier =
              Modifier.shadow(4.dp, RoundedCornerShape(10.dp), spotColor = BrandBlue)
                .clip(RoundedCornerShape(10.dp))
                .background(PrimaryGradient)
                .clickable { onUnlock(password) }
                .padding(horizontal = 16.dp, vertical = 9.dp)
                .testTag("btn_unlock_password"),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "Unlock",
              style =
                MaterialTheme.typography.labelLarge.copy(
                  fontWeight = FontWeight.SemiBold,
                  color = Color.White,
                ),
            )
          }
        }
      }
    }
  }
}

@Composable
fun TextSelectionDialog(
  state: com.example.model.TextSelectionState,
  onDismiss: () -> Unit,
  onCopy: (String) -> Unit,
) {
  if (!state.isOpen) return

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 8.dp,
      modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Page ${state.page} Text",
              style =
                MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                ),
            )
            Text(
              text = "Select text with long press or copy to clipboard",
              style =
                MaterialTheme.typography.bodySmall.copy(
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  fontSize = 11.sp,
                ),
            )
          }
        }

        if (state.isLoading) {
          Box(
            modifier = Modifier.fillMaxWidth().height(140.dp),
            contentAlignment = Alignment.Center,
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = BrandBlue,
                strokeWidth = 3.dp,
              )
              Text(
                text = "Extracting text from page...",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
              )
            }
          }
        } else if (state.text.isBlank()) {
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(16.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "No extractable text found on page ${state.page}.\n(This page might be an image or scanned document)",
              style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
              ),
              textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
          }
        } else {
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                .border(
                  1.dp,
                  MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                  RoundedCornerShape(12.dp),
                )
                .padding(12.dp),
          ) {
            androidx.compose.foundation.text.selection.SelectionContainer {
              androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxWidth(),
              ) {
                item {
                  Text(
                    text = state.text,
                    style =
                      MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp,
                        fontSize = 13.sp,
                      ),
                  )
                }
              }
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          TextButton(
            onClick = onDismiss,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.testTag("btn_close_text_selection"),
          ) {
            Text("Close")
          }
          if (!state.isLoading && state.text.isNotBlank()) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
              modifier =
                Modifier.shadow(4.dp, RoundedCornerShape(10.dp), spotColor = BrandBlue)
                  .clip(RoundedCornerShape(10.dp))
                  .background(PrimaryGradient)
                  .clickable {
                    onCopy(state.text)
                    onDismiss()
                  }
                  .padding(horizontal = 16.dp, vertical = 9.dp)
                  .testTag("btn_copy_page_text"),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = "Copy Page Text",
                style =
                  MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                  ),
              )
            }
          }
        }
      }
    }
  }
}
