package com.example.util

import android.graphics.pdf.PdfRenderer
import android.os.Build
import com.example.model.NormalizedRect
import com.example.model.SearchMatch
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream

object PdfTextSearcher {

  /**
   * Searches for [query] in the PDF file across all pages up to [totalPages].
   * Uses Android 15's native searchText if available (with exact bounding boxes),
   * with a fast and robust pure-Kotlin PDF content stream extractor fallback for earlier Android versions.
   */
  fun search(
    file: File?,
    query: String,
    totalPages: Int,
    renderer: PdfRenderer? = null,
    rendererLock: Any? = null
  ): List<SearchMatch> {
    val trimmed = query.trim()
    if (trimmed.isEmpty() || totalPages <= 0) return emptyList()

    // 1. Try Android 15+ (API 35+) native PdfRenderer.Page.searchText
    if (Build.VERSION.SDK_INT >= 35 && renderer != null) {
      try {
        var nativeSuccess = false
        val nativeMatches = mutableListOf<SearchMatch>()
        val lock = rendererLock ?: renderer

        synchronized(lock) {
          for (pageIdx in 0 until totalPages) {
            try {
              val page = renderer.openPage(pageIdx)
              try {
                val matches = page.searchText(trimmed)
                if (matches.isNotEmpty()) {
                  for ((mIdx, matchObj) in matches.withIndex()) {
                    val boundsList = try {
                      matchObj.bounds.map { r ->
                        NormalizedRect(
                          left = (r.left / page.width.coerceAtLeast(1)).coerceIn(0f, 1f),
                          top = (r.top / page.height.coerceAtLeast(1)).coerceIn(0f, 1f),
                          right = (r.right / page.width.coerceAtLeast(1)).coerceIn(0f, 1f),
                          bottom = (r.bottom / page.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                        )
                      }
                    } catch (_: Throwable) {
                      emptyList()
                    }
                    nativeMatches.add(
                      SearchMatch(
                        page = pageIdx + 1,
                        matchIndexOnPage = mIdx + 1,
                        bounds = boundsList
                      )
                    )
                  }
                }
                nativeSuccess = true
              } finally {
                page.close()
              }
            } catch (_: Throwable) {
              // If searchText throws or is not supported on this runtime, break to fallback
              nativeSuccess = false
              break
            }
          }
        }

        if (nativeSuccess && nativeMatches.isNotEmpty()) {
          return nativeMatches
        }
      } catch (_: Throwable) {
        // Fallback to stream parsing
      }
    }

    // 2. Stream parsing fallback from the PDF file directly
    if (file != null && file.exists() && file.length() > 0) {
      try {
        val streamMatches = extractAndSearchStreams(file, trimmed, totalPages)
        if (streamMatches.isNotEmpty()) {
          return streamMatches
        }
      } catch (_: Exception) {
        // Continue to fallback
      }
    }

    return emptyList()
  }

  /**
   * Extracts readable text for a specific page (1-based index) for text selection and copying.
   */
  fun extractPageText(
    file: File?,
    pageIndex: Int,
    totalPages: Int,
    renderer: PdfRenderer? = null,
    rendererLock: Any? = null
  ): String {
    // 1. Try Android 15+ native textContents
    if (Build.VERSION.SDK_INT >= 35 && renderer != null) {
      try {
        val lock = rendererLock ?: renderer
        synchronized(lock) {
          if (pageIndex in 0 until totalPages) {
            val page = renderer.openPage(pageIndex)
            try {
              val contents = page.textContents
              if (contents.isNotEmpty()) {
                val sb = StringBuilder()
                for (c in contents) {
                  val text = c.text
                  if (text.isNotBlank()) {
                    sb.append(text).append("\n")
                  }
                }
                val result = sb.toString().trim()
                if (result.isNotBlank()) return result
              }
            } finally {
              page.close()
            }
          }
        }
      } catch (_: Throwable) {}
    }

    // 2. Fallback: Parse text from the PDF file's page stream
    if (file != null && file.exists() && file.length() > 0) {
      try {
        val text = extractPageTextFromStream(file, pageIndex, totalPages)
        if (text.isNotBlank()) return text
      } catch (_: Throwable) {}
    }

    return "Page ${pageIndex + 1}\n\n(No selectable text could be extracted from this page. This may be an image-only or scanned PDF.)"
  }

  /**
   * Scans PDF page content streams and extracts text strings with positional bounds.
   */
  private fun extractAndSearchStreams(file: File, query: String, totalPages: Int): List<SearchMatch> {
    val matches = mutableListOf<SearchMatch>()
    val queryLower = query.lowercase()

    RandomAccessFile(file, "r").use { raf ->
      val fileLength = raf.length()
      val maxRead = fileLength.coerceAtMost(64L * 1024 * 1024).toInt()
      val bytes = ByteArray(maxRead)
      raf.seek(0)
      raf.readFully(bytes)

      val content = String(bytes, Charsets.ISO_8859_1)
      val pageRegex = Regex("""/Type\s*/Page\b""")
      val pageMatches = pageRegex.findAll(content).toList()

      if (pageMatches.isNotEmpty()) {
        val effectivePages = pageMatches.size.coerceAtMost(totalPages)

        for (pageIndex in 0 until effectivePages) {
          val pageNum = pageIndex + 1
          val matchPos = pageMatches[pageIndex].range.first
          val objStart = content.lastIndexOf("obj", matchPos).coerceAtLeast(0)
          val objEnd = content.indexOf("endobj", matchPos).let { if (it == -1) content.length else it + 6 }
          val pageObjSnippet = content.substring(objStart, objEnd)

          val contentsRegex = Regex("""/Contents\s*(\d+)\s+\d+\s+R""")
          val contentsArrayRegex = Regex("""/Contents\s*\[([^\]]+)\]""")

          val streamObjIds = mutableListOf<String>()
          val arrayMatch = contentsArrayRegex.find(pageObjSnippet)
          if (arrayMatch != null) {
            val refs = Regex("""(\d+)\s+\d+\s+R""").findAll(arrayMatch.groupValues[1])
            for (r in refs) {
              streamObjIds.add(r.groupValues[1])
            }
          } else {
            val singleMatch = contentsRegex.find(pageObjSnippet)
            if (singleMatch != null) {
              streamObjIds.add(singleMatch.groupValues[1])
            }
          }

          val pageTextBuilder = StringBuilder()
          for (objId in streamObjIds) {
            val streamText = extractTextFromStreamObject(content, bytes, objId)
            if (streamText.isNotEmpty()) {
              pageTextBuilder.append(streamText).append("\n")
            }
          }

          val rawPageText = pageTextBuilder.toString()
          val pageTextLower = rawPageText.lowercase()
          val lines = rawPageText.lines()
          val totalLines = lines.size.coerceAtLeast(1)

          var count = 0
          var searchIdx = 0
          while (searchIdx < pageTextLower.length) {
            val found = pageTextLower.indexOf(queryLower, searchIdx)
            if (found >= 0) {
              count++
              // Compute approximate normalized highlight bounds for this match
              var accumulated = 0
              var matchLineIdx = 0
              var matchColIdx = 0
              for ((lIdx, line) in lines.withIndex()) {
                if (found in accumulated..(accumulated + line.length)) {
                  matchLineIdx = lIdx
                  matchColIdx = (found - accumulated).coerceAtLeast(0)
                  break
                }
                accumulated += line.length + 1
              }

              val topNorm = (0.06f + (matchLineIdx.toFloat() / totalLines) * 0.86f).coerceIn(0.04f, 0.94f)
              val bottomNorm = (topNorm + 0.035f).coerceIn(topNorm + 0.01f, 0.98f)
              val lineLen = lines.getOrNull(matchLineIdx)?.length?.coerceAtLeast(20) ?: 50
              val leftNorm = (0.05f + (matchColIdx.toFloat() / lineLen) * 0.86f).coerceIn(0.04f, 0.92f)
              val widthNorm = (query.length.toFloat() / lineLen * 0.86f).coerceIn(0.05f, 0.45f)
              val rightNorm = (leftNorm + widthNorm).coerceIn(leftNorm + 0.02f, 0.98f)

              val highlight = listOf(NormalizedRect(leftNorm, topNorm, rightNorm, bottomNorm))
              matches.add(
                SearchMatch(
                  page = pageNum,
                  matchIndexOnPage = count,
                  bounds = highlight
                )
              )

              searchIdx = found + queryLower.length.coerceAtLeast(1)
            } else {
              break
            }
          }
        }
      }

      // Fallback if structured page mapping was not found
      if (matches.isEmpty()) {
        val streamRegex = Regex("""stream\r?\n""")
        val streamMatchesList = streamRegex.findAll(content).toList()
        var streamPageEst = 1

        for (streamMatch in streamMatchesList) {
          val streamStart = streamMatch.range.last + 1
          val streamEnd = content.indexOf("endstream", streamStart)
          if (streamEnd > streamStart) {
            val isFlate = content.substring((streamStart - 200).coerceAtLeast(0), streamStart).contains("/FlateDecode")
            val rawStreamBytes = bytes.copyOfRange(streamStart, streamEnd)
            val decompressed = if (isFlate) decompress(rawStreamBytes) else rawStreamBytes
            if (decompressed != null) {
              val extracted = parseTextFromPdfOperators(String(decompressed, Charsets.ISO_8859_1))
              val textLower = extracted.lowercase()
              var count = 0
              var sIdx = 0
              while (sIdx < textLower.length) {
                val f = textLower.indexOf(queryLower, sIdx)
                if (f >= 0) {
                  count++
                  val progress = f.toFloat() / textLower.length.coerceAtLeast(1)
                  val topNorm = (0.08f + progress * 0.82f).coerceIn(0.05f, 0.92f)
                  val bounds = listOf(NormalizedRect(0.08f, topNorm, 0.55f, topNorm + 0.035f))
                  matches.add(
                    SearchMatch(
                      page = streamPageEst.coerceIn(1, totalPages),
                      matchIndexOnPage = count,
                      bounds = bounds
                    )
                  )
                  sIdx = f + queryLower.length.coerceAtLeast(1)
                } else {
                  break
                }
              }
              if (extracted.isNotBlank() && streamPageEst < totalPages) {
                streamPageEst++
              }
            }
          }
        }
      }
    }

    return matches
  }

  private fun extractPageTextFromStream(file: File, pageIndex: Int, totalPages: Int): String {
    RandomAccessFile(file, "r").use { raf ->
      val fileLength = raf.length()
      val maxRead = fileLength.coerceAtMost(64L * 1024 * 1024).toInt()
      val bytes = ByteArray(maxRead)
      raf.seek(0)
      raf.readFully(bytes)

      val content = String(bytes, Charsets.ISO_8859_1)
      val pageRegex = Regex("""/Type\s*/Page\b""")
      val pageMatches = pageRegex.findAll(content).toList()

      if (pageIndex in pageMatches.indices) {
        val matchPos = pageMatches[pageIndex].range.first
        val objStart = content.lastIndexOf("obj", matchPos).coerceAtLeast(0)
        val objEnd = content.indexOf("endobj", matchPos).let { if (it == -1) content.length else it + 6 }
        val pageObjSnippet = content.substring(objStart, objEnd)

        val contentsRegex = Regex("""/Contents\s*(\d+)\s+\d+\s+R""")
        val contentsArrayRegex = Regex("""/Contents\s*\[([^\]]+)\]""")

        val streamObjIds = mutableListOf<String>()
        val arrayMatch = contentsArrayRegex.find(pageObjSnippet)
        if (arrayMatch != null) {
          val refs = Regex("""(\d+)\s+\d+\s+R""").findAll(arrayMatch.groupValues[1])
          for (r in refs) {
            streamObjIds.add(r.groupValues[1])
          }
        } else {
          val singleMatch = contentsRegex.find(pageObjSnippet)
          if (singleMatch != null) {
            streamObjIds.add(singleMatch.groupValues[1])
          }
        }

        val pageTextBuilder = StringBuilder()
        for (objId in streamObjIds) {
          val streamText = extractTextFromStreamObject(content, bytes, objId)
          if (streamText.isNotEmpty()) {
            pageTextBuilder.append(streamText).append("\n\n")
          }
        }
        val res = pageTextBuilder.toString().trim()
        if (res.isNotBlank()) return res
      }
    }
    return ""
  }

  private fun extractTextFromStreamObject(content: String, bytes: ByteArray, objId: String): String {
    val objHeader = "$objId 0 obj"
    val objIdx = content.indexOf(objHeader)
    if (objIdx == -1) return ""

    val streamTag = "stream"
    val streamIdx = content.indexOf(streamTag, objIdx)
    if (streamIdx == -1) return ""

    // Check if stream tag is followed by \r\n or \n
    val streamStart = if (bytes.getOrNull(streamIdx + 6) == '\r'.code.toByte() && bytes.getOrNull(streamIdx + 7) == '\n'.code.toByte()) {
      streamIdx + 8
    } else {
      streamIdx + 7
    }

    val endStreamIdx = content.indexOf("endstream", streamStart)
    if (endStreamIdx <= streamStart) return ""

    val dict = content.substring(objIdx, streamIdx)
    val isFlate = dict.contains("/FlateDecode")

    val streamBytes = bytes.copyOfRange(streamStart, endStreamIdx)
    val decompressedBytes = if (isFlate) decompress(streamBytes) else streamBytes
    if (decompressedBytes == null) return ""

    val streamContent = String(decompressedBytes, Charsets.ISO_8859_1)
    return parseTextFromPdfOperators(streamContent)
  }

  private fun decompress(data: ByteArray): ByteArray? {
    // Try standard zlib first
    try {
      InflaterInputStream(ByteArrayInputStream(data)).use { inflater ->
        val out = ByteArrayOutputStream()
        val buf = ByteArray(4096)
        var read: Int
        while (inflater.read(buf).also { read = it } != -1) {
          out.write(buf, 0, read)
        }
        return out.toByteArray()
      }
    } catch (_: Exception) {}

    // Try raw deflate (nowrap = true)
    try {
      val inflater = Inflater(true)
      inflater.setInput(data)
      val out = ByteArrayOutputStream()
      val buf = ByteArray(4096)
      while (!inflater.finished() && !inflater.needsInput()) {
        val count = inflater.inflate(buf)
        if (count > 0) {
          out.write(buf, 0, count)
        } else {
          break
        }
      }
      inflater.end()
      if (out.size() > 0) return out.toByteArray()
    } catch (_: Exception) {}

    return null
  }

  /**
   * Extracts readable text from PDF stream operators:
   * (Text) Tj, [(T) 10 (ext)] TJ, ' or "
   */
  private fun parseTextFromPdfOperators(stream: String): String {
    val sb = StringBuilder()
    var i = 0
    val len = stream.length

    while (i < len) {
      val ch = stream[i]
      if (ch == '(') {
        // String literal in PDF
        i++
        val str = StringBuilder()
        var depth = 1
        while (i < len && depth > 0) {
          val c = stream[i]
          if (c == '\\' && i + 1 < len) {
            i++
            val next = stream[i]
            when (next) {
              'n' -> str.append('\n')
              'r' -> str.append('\r')
              't' -> str.append('\t')
              'b' -> str.append('\b')
              'f' -> str.append('\u000C')
              '(', ')', '\\' -> str.append(next)
              in '0'..'7' -> {
                // Octal escape
                var oct = "$next"
                if (i + 1 < len && stream[i + 1] in '0'..'7') {
                  i++
                  oct += stream[i]
                  if (i + 1 < len && stream[i + 1] in '0'..'7') {
                    i++
                    oct += stream[i]
                  }
                }
                str.append(oct.toIntOrNull(8)?.toChar() ?: '?')
              }
              else -> str.append(next)
            }
          } else if (c == '(') {
            depth++
            str.append('(')
          } else if (c == ')') {
            depth--
            if (depth > 0) str.append(')')
          } else {
            str.append(c)
          }
          i++
        }
        sb.append(str).append(" ")
      } else {
        i++
      }
    }

    return sb.toString()
  }
}
