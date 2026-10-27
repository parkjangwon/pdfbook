package org.parkjw.apps.pdfbook.lock

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.parkjw.apps.pdfbook.R
import org.parkjw.apps.pdfbook.ui.SafeTopBar
import org.parkjw.apps.pdfbook.ui.theme.CardCorner
import org.parkjw.apps.pdfbook.ui.theme.PdfBookSpacing
import org.parkjw.apps.pdfbook.ui.theme.extendedColors
import org.parkjw.apps.pdfbook.ui.theme.pdfBookIconButtonColors
import org.parkjw.apps.pdfbook.ui.theme.pdfBookRadioButtonColors

@Composable
fun AppLockSettingsScreen(
    config: LockConfig,
    session: AppLockState,
    biometricAvailable: Boolean,
    onBack: () -> Unit,
    onAuthenticated: () -> Unit,
    onVerify: suspend (CharArray, Long) -> PinVerify,
    onSavePin: suspend (CharArray, AppLockMode) -> Unit,
    onSetMode: suspend (AppLockMode) -> Boolean,
    onSetHideInRecents: suspend (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var phase by remember { mutableStateOf<LockEditPhase>(LockEditPhase.Menu) }
    val scope = rememberCoroutineScope()
    fun finish(edit: LockEdit) {
        val next = phaseAfterConfirmed(edit, config.hasPin)
        if (next is LockEditPhase.Menu) {
            scope.launch {
                when (edit) {
                    LockEdit.None -> onSetMode(AppLockMode.None)
                    LockEdit.Pin -> onSetMode(AppLockMode.Pin)
                    LockEdit.Biometric -> onSetMode(AppLockMode.Biometric)
                    LockEdit.HideOff -> onSetHideInRecents(false)
                    LockEdit.ChangePin -> Unit
                }
                phase = LockEditPhase.Menu
            }
        } else {
            phase = next
        }
    }
    BackHandler {
        if (phase == LockEditPhase.Menu) onBack() else phase = LockEditPhase.Menu
    }
    when (val current = phase) {
        LockEditPhase.Menu -> AppLockSettingsMenu(
            config = config,
            biometricAvailable = biometricAvailable,
            onBack = onBack,
            onEdit = { edit -> phase = phaseFor(edit, config, biometricAvailable) },
            onHideChange = { hide ->
                if (hide) {
                    scope.launch { onSetHideInRecents(true) }
                } else {
                    phase = phaseFor(LockEdit.HideOff, config, biometricAvailable)
                }
            },
            modifier = modifier,
        )
        is LockEditPhase.Confirm -> ConfirmPinScreen(
            config = config,
            session = session,
            canAuthenticate = biometricAvailable,
            forceKeypad = current.keypad,
            verify = onVerify,
            onSuccess = {
                onAuthenticated()
                finish(current.edit)
            },
            onUsePin = { phase = current.copy(keypad = true) },
            onRequestBiometric = { phase = current.copy(keypad = false) },
        )
        is LockEditPhase.Create -> PinCreateScreen(
            onCreated = { pin ->
                onAuthenticated()
                scope.launch {
                    onSavePin(pin, modeForNewPin(current.edit, config.mode))
                    phase = LockEditPhase.Menu
                }
            },
            onBack = { phase = LockEditPhase.Menu },
        )
    }
}

@Composable
internal fun AppLockSettingsMenu(
    config: LockConfig,
    biometricAvailable: Boolean,
    onBack: () -> Unit,
    onEdit: (LockEdit) -> Unit,
    onHideChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val locked = config.mode != AppLockMode.None
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            SafeTopBar {
                IconButton(onClick = onBack, colors = pdfBookIconButtonColors()) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = stringResource(R.string.reader_back),
                    )
                }
                Text(
                    text = stringResource(R.string.app_lock),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = PdfBookSpacing.lg),
        ) {
            LockCard {
                Column(Modifier.fillMaxWidth().selectableGroup()) {
                    LockChoice(AppLockMode.None, config.mode, enabled = true) { onEdit(LockEdit.None) }
                    HorizontalDivider(color = MaterialTheme.extendedColors.hairline)
                    LockChoice(AppLockMode.Pin, config.mode, enabled = true) { onEdit(LockEdit.Pin) }
                    HorizontalDivider(color = MaterialTheme.extendedColors.hairline)
                    val biometricEnabled = biometricAvailable && config.hasPin
                    LockChoice(
                        mode = AppLockMode.Biometric,
                        selected = config.mode,
                        enabled = biometricEnabled,
                        supporting = if (!config.hasPin) {
                            stringResource(R.string.app_lock_biometric_needs_pin)
                        } else {
                            null
                        },
                    ) {
                        onEdit(LockEdit.Biometric)
                    }
                }
            }
            if (locked) {
                LockCard {
                    ListItem(
                        headlineContent = {
                            Text(
                                stringResource(R.string.pin_change),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        },
                        trailingContent = {
                            Icon(
                                painter = painterResource(R.drawable.ic_chevron_right),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = false, onClick = { onEdit(LockEdit.ChangePin) }),
                    )
                }
                LockCard {
                    ListItem(
                        headlineContent = {
                            Text(
                                stringResource(R.string.hide_in_recents),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        },
                        supportingContent = {
                            Text(
                                stringResource(R.string.hide_in_recents_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = config.hideInRecents,
                                onCheckedChange = null,
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    uncheckedBorderColor = MaterialTheme.extendedColors.hairlineStrong,
                                ),
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = config.hideInRecents,
                                onValueChange = onHideChange,
                                role = Role.Switch,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun LockCard(content: @Composable () -> Unit) {
    Surface(
        shape = CardCorner,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.extendedColors.hairline),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = PdfBookSpacing.screenHorizontalFolded,
                vertical = PdfBookSpacing.md,
            ),
    ) {
        content()
    }
}

@Composable
private fun LockChoice(
    mode: AppLockMode,
    selected: AppLockMode,
    enabled: Boolean,
    supporting: String? = null,
    onClick: () -> Unit,
) {
    val label = when (mode) {
        AppLockMode.None -> stringResource(R.string.app_lock_none)
        AppLockMode.Pin -> stringResource(R.string.app_lock_pin)
        AppLockMode.Biometric -> stringResource(R.string.app_lock_biometric)
    }
    val description = supporting ?: when (mode) {
        AppLockMode.None -> stringResource(R.string.app_lock_none_desc)
        AppLockMode.Pin -> stringResource(R.string.app_lock_pin_desc)
        AppLockMode.Biometric -> stringResource(R.string.app_lock_biometric_desc)
    }
    ListItem(
        headlineContent = {
            Text(label, style = MaterialTheme.typography.bodyLarge)
        },
        supportingContent = {
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        leadingContent = {
            RadioButton(
                selected = selected == mode,
                onClick = null,
                enabled = enabled,
                colors = pdfBookRadioButtonColors(),
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.38f)
            .selectable(
                selected = selected == mode,
                enabled = enabled,
                onClick = onClick,
                role = Role.RadioButton,
            ),
    )
}

@Composable
internal fun SecurityValueRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    supporting: String? = null,
    enabled: Boolean = true,
) {
    val muted = MaterialTheme.extendedColors.muted
    val titleColor = if (enabled) Color.Unspecified else muted
    val valueColor = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else muted
    ListItem(
        headlineContent = {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = titleColor)
        },
        supportingContent = supporting?.let {
            {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = valueColor,
                )
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else muted,
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = false, enabled = enabled, onClick = onClick),
    )
}
