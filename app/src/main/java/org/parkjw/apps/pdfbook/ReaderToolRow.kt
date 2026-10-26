package org.parkjw.apps.pdfbook

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.parkjw.apps.pdfbook.ui.theme.ReaderOnOverlay

/**
 * Fixed left-to-right order for reader bottom-bar tools. Shown tools keep this order in both
 * LTR and RTL reading directions (the row itself does not mirror).
 */
enum class ReaderTool {
    Brightness,
    AutoTurn,
    Bookmarks,
    AutoCrop,
}

sealed interface ReaderToolItem {
    val tool: ReaderTool
    val iconRes: Int
    val label: String
    val testTag: String

    data class Action(
        override val tool: ReaderTool,
        @DrawableRes override val iconRes: Int,
        override val label: String,
        override val testTag: String,
        val onClick: () -> Unit,
    ) : ReaderToolItem

    data class Toggle(
        override val tool: ReaderTool,
        @DrawableRes override val iconRes: Int,
        override val label: String,
        override val testTag: String,
        val checked: Boolean,
        val onCheckedChange: (Boolean) -> Unit,
        val stateDescription: String? = null,
    ) : ReaderToolItem
}

@Composable
fun ReaderToolRow(
    items: List<ReaderToolItem>,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    val ordered = items.sortedBy { it.tool.ordinal }
    // Tool order is fixed LTR regardless of reading direction (see ReaderTool KDoc).
    // mergeDescendants boundary so testTag is not absorbed by reader_overlay_bottom's clickable.
    Box(
        modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .testTag("reader_tool_row"),
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (item in ordered) {
                    ReaderToolSlot(
                        item = item,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ReaderToolSlot(
    item: ReaderToolItem,
    modifier: Modifier = Modifier,
) {
    // Label Text is the accessible name (merged by clickable/toggleable).
    val interaction = when (item) {
        is ReaderToolItem.Action ->
            Modifier.clickable(role = Role.Button, onClick = item.onClick)
        is ReaderToolItem.Toggle ->
            Modifier
                .toggleable(
                    value = item.checked,
                    role = Role.Switch,
                    onValueChange = item.onCheckedChange,
                )
                .semantics {
                    item.stateDescription?.let { stateDescription = it }
                }
    }
    Column(
        modifier = modifier
            .heightIn(min = 56.dp)
            .then(interaction)
            .testTag(item.testTag)
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(item.iconRes),
            contentDescription = null,
            tint = ReaderOnOverlay,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall,
            color = ReaderOnOverlay,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
