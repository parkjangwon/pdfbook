package dev.parkjw.pdfbook

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class MainActivity : ComponentActivity() {
    private val volumeDeltas = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    private val volumeDeltaFlow: SharedFlow<Int> = volumeDeltas

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var pdfUri by remember { mutableStateOf<Uri?>(null) }
                val openPdf = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument(),
                ) { uri -> pdfUri = uri }

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
                    val uri = pdfUri!!
                    val context = LocalContext.current
                    val renderer = remember(uri) {
                        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                            ?: return@remember null
                        PageRenderer(pfd)
                    }
                    DisposableEffect(renderer) {
                        onDispose { renderer?.close() }
                    }
                    if (renderer != null) {
                        val twoPage = rememberTwoPage()
                        val spreadList = remember(renderer.pageCount, twoPage) {
                            spreads(renderer.pageCount, twoPage = twoPage, coverAlone = true)
                        }
                        var keptPage by remember { mutableIntStateOf(1) }
                        val pagerState = rememberPagerState(
                            initialPage = spreadIndexOf(spreadList, keptPage),
                            pageCount = { spreadList.size.coerceAtLeast(1) },
                        )
                        LaunchedEffect(pagerState.settledPage, spreadList) {
                            spreadList.getOrNull(pagerState.settledPage)?.firstOrNull()?.let {
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

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
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
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN -> true
            else -> super.onKeyUp(keyCode, event)
        }
    }
}
