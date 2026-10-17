package org.parkjw.apps.pdfbook

import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import org.parkjw.apps.pdfbook.ui.theme.PdfBookTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class MainActivity : ComponentActivity() {
    private val volumeDeltas = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    private val volumeDeltaFlow: SharedFlow<Int> = volumeDeltas
    @Volatile private var documentOpen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PdfBookTheme {
                var pdfUriString by rememberSaveable { mutableStateOf<String?>(null) }
                val pdfUri = pdfUriString?.let(Uri::parse)
                val openPdf = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument(),
                ) { uri ->
                    pdfUriString = uri?.toString()
                }

                DisposableEffect(pdfUri) {
                    documentOpen = pdfUri != null
                    onDispose { documentOpen = false }
                }

                if (pdfUri == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Button(onClick = { openPdf.launch(arrayOf("application/pdf")) }) {
                            Text("Open PDF")
                        }
                    }
                } else {
                    val uri = pdfUri
                    val context = LocalContext.current
                    val openResult by produceState<PdfOpenResult>(PdfOpenResult.Loading, uri, context) {
                        value = PdfOpenResult.Loading
                        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                        if (pfd == null) {
                            value = PdfOpenResult.Failed
                            return@produceState
                        }
                        var opened: PageRenderer? = null
                        value = try {
                            PageRenderer.open(pfd).also { opened = it }.let { PdfOpenResult.Ready(it) }
                        } catch (_: Exception) {
                            PdfOpenResult.Failed
                        }
                        awaitDispose { opened?.close() }
                    }
                    when (val result = openResult) {
                        PdfOpenResult.Loading -> Box(Modifier.fillMaxSize())
                        PdfOpenResult.Failed -> Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(stringResource(R.string.open_failed))
                        }
                        is PdfOpenResult.Ready -> {
                            val renderer = result.renderer
                            val twoPage = rememberTwoPage()
                            val spreadList = remember(renderer.pageCount, twoPage) {
                                spreads(renderer.pageCount, twoPage = twoPage, coverAlone = true)
                            }
                            var keptPage by rememberSaveable(uri.toString()) { mutableIntStateOf(1) }
                            if (!hasReadableSpreads(spreadList)) {
                                Box(Modifier.fillMaxSize())
                            } else {
                                val pagerState = rememberPagerState(
                                    initialPage = spreadIndexOf(spreadList, keptPage),
                                    pageCount = { spreadList.size },
                                )
                                LaunchedEffect(pagerState.settledPage) {
                                    keptPageFromSettle(spreadList, pagerState.settledPage)?.let {
                                        keptPage = it
                                    }
                                }
                                LaunchedEffect(spreadList) {
                                    val index = spreadIndexOf(spreadList, keptPage)
                                    if (pagerState.currentPage != index) {
                                        pagerState.scrollToPage(index)
                                    }
                                }
                                ReaderScreen(
                                    renderer = renderer,
                                    pagerState = pagerState,
                                    spreads = spreadList,
                                    direction = ReadingDirection.RTL,
                                    volumeDelta = volumeDeltaFlow,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (!documentOpen) return super.onKeyDown(keyCode, event)
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                volumeDeltas.tryEmit(1)
                true
            }
            KeyEvent.KEYCODE_VOLUME_UP -> {
                volumeDeltas.tryEmit(-1)
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (!documentOpen) return super.onKeyUp(keyCode, event)
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN -> true
            else -> super.onKeyUp(keyCode, event)
        }
    }
}

private sealed class PdfOpenResult {
    data object Loading : PdfOpenResult()
    data object Failed : PdfOpenResult()
    data class Ready(val renderer: PageRenderer) : PdfOpenResult()
}
