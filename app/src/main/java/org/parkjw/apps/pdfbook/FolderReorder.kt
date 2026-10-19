package org.parkjw.apps.pdfbook

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive
import org.parkjw.apps.pdfbook.folders.moveInOrder

@Stable
class FolderReorderState(private val gridState: LazyGridState) {
    var draggingUri by mutableStateOf<String?>(null)
        private set
    var pendingOrder by mutableStateOf<List<String>?>(null)
        private set
    private var initialOffset by mutableStateOf(Offset.Zero)
    private var draggedDelta by mutableStateOf(Offset.Zero)
    internal var baseOrder by mutableStateOf<List<String>>(emptyList())
        private set

    fun start(uri: String) {
        val item = itemInfo(uri) ?: return
        pendingOrder = pendingOrder?.takeIf { it.toSet() == baseOrder.toSet() } ?: baseOrder
        draggingUri = uri
        initialOffset = Offset(item.offset.x.toFloat(), item.offset.y.toFloat())
        draggedDelta = Offset.Zero
    }

    fun drag(delta: Offset) {
        if (draggingUri == null) return
        draggedDelta += delta
        swapIfNeeded()
    }

    fun end(): List<String>? {
        val order = pendingOrder
        draggingUri = null
        draggedDelta = Offset.Zero
        return order?.takeIf { it != baseOrder }
    }

    fun cancel() {
        draggingUri = null
        draggedDelta = Offset.Zero
        pendingOrder = null
    }

    fun canMove(uri: String, delta: Int): Boolean {
        val current = pendingOrder?.takeIf { it.toSet() == baseOrder.toSet() } ?: baseOrder
        return moveInOrder(current, uri, delta) != current
    }

    fun moveBy(uri: String, delta: Int): List<String>? {
        val current = pendingOrder?.takeIf { it.toSet() == baseOrder.toSet() } ?: baseOrder
        val next = moveInOrder(current, uri, delta)
        if (next == current) return null
        keepScrollPosition()
        pendingOrder = next
        return next
    }

    fun syncBase(order: List<String>) {
        if (order != baseOrder && draggingUri == null) pendingOrder = null
        baseOrder = order
    }

    fun translation(uri: String): Offset {
        if (uri != draggingUri) return Offset.Zero
        val item = itemInfo(uri) ?: return Offset.Zero
        return initialOffset + draggedDelta - Offset(item.offset.x.toFloat(), item.offset.y.toFloat())
    }

    internal suspend fun autoScroll(edge: Float, maxStep: Float) {
        val uri = draggingUri ?: return
        val item = itemInfo(uri) ?: return
        val layout = gridState.layoutInfo
        val top = initialOffset.y + draggedDelta.y
        val step = reorderAutoScrollStep(
            itemStart = top,
            itemEnd = top + item.size.height,
            viewportStart = layout.viewportStartOffset.toFloat(),
            viewportEnd = layout.viewportEndOffset.toFloat(),
            edge = edge,
            maxStep = maxStep,
        )
        if (step != 0f) {
            gridState.scrollBy(step)
            swapIfNeeded()
        }
    }

    private fun swapIfNeeded() {
        val uri = draggingUri ?: return
        val order = pendingOrder ?: return
        val item = itemInfo(uri) ?: return
        val center = initialOffset + draggedDelta +
            Offset(item.size.width / 2f, item.size.height / 2f)
        val target = gridState.layoutInfo.visibleItemsInfo.firstOrNull { info ->
            val key = info.key
            key is String && key != uri && key in order &&
                center.x >= info.offset.x && center.x < info.offset.x + info.size.width &&
                center.y >= info.offset.y && center.y < info.offset.y + info.size.height
        } ?: return
        keepScrollPosition()
        pendingOrder = moveBefore(order, uri, target.key as String)
    }

    private fun keepScrollPosition() {
        gridState.requestScrollToItem(gridState.firstVisibleItemIndex, gridState.firstVisibleItemScrollOffset)
    }

    private fun itemInfo(uri: String): LazyGridItemInfo? =
        gridState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == uri }
}

@Composable
fun rememberFolderReorderState(gridState: LazyGridState, order: List<String>): FolderReorderState {
    val state = remember(gridState) { FolderReorderState(gridState) }
    SideEffect { state.syncBase(order) }
    val density = LocalDensity.current
    val edge = with(density) { 64.dp.toPx() }
    val maxStep = with(density) { 12.dp.toPx() }
    LaunchedEffect(state.draggingUri) {
        if (state.draggingUri == null) return@LaunchedEffect
        while (isActive) {
            withFrameMillis { }
            state.autoScroll(edge, maxStep)
        }
    }
    return state
}

@Composable
fun FolderReorderHandle(
    uri: String,
    state: FolderReorderState,
    onReorder: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val currentOnReorder by rememberUpdatedState(onReorder)
    val label = stringResource(R.string.folder_reorder_handle)
    val moveUp = stringResource(R.string.folder_move_up)
    val moveDown = stringResource(R.string.folder_move_down)
    val canMoveUp = state.canMove(uri, -1)
    val canMoveDown = state.canMove(uri, 1)
    Box(
        modifier = modifier
            .size(48.dp)
            .testTag("folder_reorder_handle")
            .semantics {
                contentDescription = label
                customActions = listOfNotNull(
                    CustomAccessibilityAction(moveUp) {
                        state.moveBy(uri, -1)?.also(currentOnReorder) != null
                    }.takeIf { canMoveUp },
                    CustomAccessibilityAction(moveDown) {
                        state.moveBy(uri, 1)?.also(currentOnReorder) != null
                    }.takeIf { canMoveDown },
                )
            }
            .pointerInput(uri, state) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        state.start(uri)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        state.drag(amount)
                    },
                    onDragEnd = { state.end()?.let(currentOnReorder) },
                    onDragCancel = { state.cancel() },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_drag_handle),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
    }
}
