package com.aistudio.novacraft.launcher.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text as RawText
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.aistudio.novacraft.launcher.FpsMode
import com.aistudio.novacraft.launcher.R
import com.aistudio.novacraft.launcher.core.account.AccountManager
import com.aistudio.novacraft.launcher.core.account.CapePreset
import com.aistudio.novacraft.launcher.core.account.MicrosoftAuthService
import com.aistudio.novacraft.launcher.core.account.MicrosoftCape
import com.aistudio.novacraft.launcher.core.account.MicrosoftDeviceCode
import com.aistudio.novacraft.launcher.core.account.MicrosoftWardrobeService
import com.aistudio.novacraft.launcher.core.account.SkinUtils
import com.aistudio.novacraft.launcher.core.controls.KeyCap
import com.aistudio.novacraft.launcher.core.controls.KeyOption
import com.aistudio.novacraft.launcher.core.controls.KeyboardLayout
import com.aistudio.novacraft.launcher.core.controls.KeycodeCatalog
import com.aistudio.novacraft.launcher.core.utils.GameCrashInfo
import com.aistudio.novacraft.launcher.core.utils.LauncherLog
import com.aistudio.novacraft.launcher.core.launch.ForgeFlavor
import com.aistudio.novacraft.launcher.core.launch.InstanceManager
import com.aistudio.novacraft.launcher.core.launch.JavaRuntimeManager
import com.aistudio.novacraft.launcher.core.mods.NovaUnifiedFileImportEngine
import com.aistudio.novacraft.launcher.core.mods.NuxModManager
import com.aistudio.novacraft.launcher.core.mods.PendingAddonImport
import com.aistudio.novacraft.launcher.core.launch.EnvSettingUnit
import com.aistudio.novacraft.launcher.core.launch.MCNewsFeed
import com.aistudio.novacraft.launcher.core.launch.MCNewsItem
import com.aistudio.novacraft.launcher.core.launch.MCNewsService
import com.aistudio.novacraft.launcher.core.launch.NewsKind
import com.aistudio.novacraft.launcher.core.launch.NuxRendererInfo
import com.aistudio.novacraft.launcher.core.launch.NuxRendererV2Manager
import com.aistudio.novacraft.launcher.core.launch.PatchNoteBlock
import com.aistudio.novacraft.launcher.core.launch.PatchNoteDetail
import com.aistudio.novacraft.launcher.core.launch.RendererV2Data
import com.aistudio.novacraft.launcher.core.launch.ScreenRecorder
import com.aistudio.novacraft.launcher.core.utils.InstalledModItem
import com.aistudio.novacraft.launcher.core.utils.Instance
import com.aistudio.novacraft.launcher.core.utils.UserAccount
import com.aistudio.novacraft.launcher.core.utils.VersionItem
import com.aistudio.novacraft.launcher.ui.components.AmberActionPill
import com.aistudio.novacraft.launcher.ui.components.AmberAlert
import com.aistudio.novacraft.launcher.ui.components.AmberButton
import com.aistudio.novacraft.launcher.ui.components.AmberButtonStyle
import com.aistudio.novacraft.launcher.ui.components.AmberChoicePill
import com.aistudio.novacraft.launcher.ui.components.AmberCrashPanel
import com.aistudio.novacraft.launcher.ui.components.AmberField
import com.aistudio.novacraft.launcher.ui.components.AmberIcon
import com.aistudio.novacraft.launcher.ui.components.AmberInfoBox
import com.aistudio.novacraft.launcher.ui.components.AmberPage
import com.aistudio.novacraft.launcher.ui.components.AmberPageLabel
import com.aistudio.novacraft.launcher.ui.components.AmberSegmented
import com.aistudio.novacraft.launcher.ui.components.AmberSlider
import com.aistudio.novacraft.launcher.ui.components.AmberSpinner
import com.aistudio.novacraft.launcher.ui.components.AmberToggle
import com.aistudio.novacraft.launcher.ui.components.AmberWaitBar
import com.aistudio.novacraft.launcher.ui.components.FpsReadout
import com.aistudio.novacraft.launcher.ui.components.GamepadWindowHook
import com.aistudio.novacraft.launcher.ui.components.ModIcon
import com.aistudio.novacraft.launcher.ui.components.NoWindowAnimationHook
import com.aistudio.novacraft.launcher.ui.components.NuxButton
import com.aistudio.novacraft.launcher.ui.components.NuxCard
import com.aistudio.novacraft.launcher.ui.components.NuxDialog
import com.aistudio.novacraft.launcher.ui.components.NuxIconButton
import com.aistudio.novacraft.launcher.ui.components.NuxNetworkImage
import com.aistudio.novacraft.launcher.ui.components.NuxWindowDialog
import com.aistudio.novacraft.launcher.ui.components.PlayerSkin
import com.aistudio.novacraft.launcher.ui.components.SettingChip
import com.aistudio.novacraft.launcher.ui.components.amberTap
import com.aistudio.novacraft.launcher.ui.components.MouseControlMode
import com.aistudio.novacraft.launcher.ui.screens.AmberPillButton
import com.aistudio.novacraft.launcher.ui.theme.Amber
import com.aistudio.novacraft.launcher.ui.theme.AmberUi
import com.aistudio.novacraft.launcher.ui.theme.NuxColors
import com.aistudio.novacraft.launcher.ui.theme.NuxSizes
import com.aistudio.novacraft.launcher.ui.theme.Text
import com.aistudio.novacraft.launcher.ui.theme.Toast
import com.aistudio.novacraft.launcher.ui.theme.Tr
import com.aistudio.novacraft.launcher.ui.theme.amberGlass
import com.movtery.inputmap.keycodes.LwjglGlfwKeycode
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ----------------------------------------------------------------------------
// Section: ui/dialogs/GameMenuSheet.kt
// ----------------------------------------------------------------------------

/**
 * Menu dalam game: panel di sisi kanan yang membiarkan game tetap terlihat di kiri.
 * Aksi yang paling sering dipakai ada di baris ikon paling atas, sisanya satu daftar bergulir
 * tanpa tab. Sengaja polos supaya ringan selagi game berjalan di belakangnya: tanpa blur, tanpa
 * peredup layar penuh (satu isian hitam transparan seluas layar yang digambar ulang tiap kali
 * menu berubah), tanpa animasi masuk, dan angka FPS hanya dibaca oleh lencana kecil di kepala
 * panel. Mengetuk area game di luar panel menutup menu.
 */
@Composable
fun GameMenuSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    fps: () -> Int,
    instanceName: String,
    mcVersion: String,
    currentResolutionRatio: Int,
    onResolutionChange: (Int) -> Unit,
    fpsMode: FpsMode,
    onFpsModeChange: (FpsMode) -> Unit,
    onOpenConsoleLog: () -> Unit,
    cursorSensitivity: Int,
    onCursorSensitivityChange: (Int) -> Unit,
    captureSensitivity: Int,
    onCaptureSensitivityChange: (Int) -> Unit,
    mouseControlMode: MouseControlMode,
    onMouseControlModeChange: (MouseControlMode) -> Unit,
    buttonLookDrag: Boolean = false,
    onButtonLookDragChange: (Boolean) -> Unit = {},
    isControlVisible: Boolean,
    onToggleControlVisibility: () -> Unit,
    onRequestKeyboard: () -> Unit,
    onSendKeycode: (Int) -> Unit,
    onExitRequest: () -> Unit,
    onOpenCustomGui: () -> Unit,
    onToggleRecord: () -> Unit = {},
    recordWithControls: Boolean = false,
    onRecordWithControlsChange: (Boolean) -> Unit = {}
) {
    // Selama menu terbuka rekaman dijeda: menunya (dan geseran masuk/keluarnya) tidak perlu ikut
    // terekam. Dilepas sedikit terlambat supaya animasi menutup tidak masuk video.
    DisposableEffect(visible) {
        if (visible) ScreenRecorder.hold(ScreenRecorder.Hold.MENU)
        onDispose {
            if (visible) ScreenRecorder.release(ScreenRecorder.Hold.MENU, delayMs = 350L)
        }
    }
    if (!visible) return

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        GamepadWindowHook()
        val dialogView = LocalView.current
        SideEffect {
            val win = (dialogView.parent as? DialogWindowProvider)?.window
            if (win != null) {
                // Tanpa peredup dan tanpa blur latar: game tetap terlihat jelas dan menu tidak perlu
                // menggambar satu isian gelap seluas layar tiap kali berubah. Tanpa animasi jendela
                // (memudar/membesar) saat muncul dan hilang. Perubahan jendela hanya dikirim ke sistem
                // bila memang ada yang berbeda, jadi menyusun ulang menu tidak memicu panggilan lagi.
                val attrs = win.attributes
                var changed = false
                if (attrs.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND != 0) {
                    attrs.flags = attrs.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND.inv()
                    changed = true
                }
                if (attrs.windowAnimations != 0) {
                    attrs.windowAnimations = 0
                    changed = true
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                    attrs.layoutInDisplayCutoutMode !=
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                ) {
                    attrs.layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    changed = true
                }
                if (changed) win.attributes = attrs
            }
        }

        val dismiss by rememberUpdatedState(onDismissRequest)
        val panelShape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)

        // Wadah penuh layar hanya menangkap ketukan di luar panel (menutup menu); tidak menggambar apa pun.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures { dismiss() } },
            contentAlignment = Alignment.CenterEnd
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 360.dp)
                    .fillMaxWidth()
                    .clip(panelShape)
                    .background(Amber.Panel)
                    .border(1.dp, Amber.Line, panelShape)
                    // Menelan ketukan di area kosong supaya tidak menutup menu.
                    // Hanya menghindari poni layar; bar sistem sudah disembunyikan (ImmersiveWindowHook),
                    // jadi menu menempel penuh ke tepi tanpa mengalah.
                    .pointerInput(Unit) { detectTapGestures { } }
                    .windowInsetsPadding(
                        WindowInsets.displayCutout.only(WindowInsetsSides.End + WindowInsetsSides.Vertical)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Kepala: judul, FPS, tutup
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Menu",
                            color = Amber.Text,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$instanceName  \u00B7  $mcVersion",
                            color = Amber.TextDim,
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    FpsReadout(fps)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .amberTap(onDismissRequest),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .border(1.dp, Amber.LineStrong, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            AmberIcon("close", 14.dp, Amber.Text)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Aksi cepat
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickTile(
                        modifier = Modifier.weight(1f),
                        icon = "keyboard",
                        label = "Keyboard",
                        onClick = {
                            onDismissRequest()
                            onRequestKeyboard()
                        }
                    )
                    QuickTile(
                        modifier = Modifier.weight(1f),
                        icon = "eye",
                        label = "Tombol",
                        active = isControlVisible,
                        slash = !isControlVisible,
                        onClick = onToggleControlVisibility
                    )
                    QuickTile(
                        modifier = Modifier.weight(1f),
                        icon = "sliders",
                        label = "Atur tombol",
                        onClick = {
                            onDismissRequest()
                            onOpenCustomGui()
                        }
                    )
                    QuickTile(
                        modifier = Modifier.weight(1f),
                        icon = "terminal",
                        label = "Log",
                        onClick = {
                            onDismissRequest()
                            onOpenConsoleLog()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                RecordBar(onClick = {
                    onDismissRequest()
                    onToggleRecord()
                })

                Spacer(modifier = Modifier.height(4.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    SectionTitle("REKAM")
                    RecordModeRow(
                        withControls = recordWithControls,
                        onWithControlsChange = onRecordWithControlsChange
                    )
                    SectionTitle("TAMPILAN")
                    SliderRow(
                        label = "Resolusi render",
                        value = currentResolutionRatio,
                        min = 40,
                        max = 125,
                        step = 5,
                        onCommit = onResolutionChange
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    AmberSegmented(
                        options = listOf("Hemat 50%", "Seimbang 75%", "Penuh 100%"),
                        selected = when (currentResolutionRatio) {
                            50 -> 0
                            75 -> 1
                            100 -> 2
                            else -> -1
                        },
                        onSelect = { index -> onResolutionChange(intArrayOf(50, 75, 100)[index]) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ToggleRow(
                        title = "Pin FPS di layar",
                        subtitle = "Tetap tampil saat tombol layar disembunyikan",
                        checked = fpsMode == FpsMode.PINNED,
                        onChange = { on -> onFpsModeChange(if (on) FpsMode.PINNED else FpsMode.NORMAL) }
                    )

                    SectionTitle("KAMERA DAN KURSOR")
                    SliderRow(
                        label = "Kecepatan kamera",
                        value = captureSensitivity,
                        min = 25,
                        max = 250,
                        step = 5,
                        onCommit = onCaptureSensitivityChange
                    )
                    SliderRow(
                        label = "Kecepatan kursor",
                        value = cursorSensitivity,
                        min = 25,
                        max = 250,
                        step = 5,
                        onCommit = onCursorSensitivityChange
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Mode sentuh",
                        color = Amber.TextDim,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    AmberSegmented(
                        options = listOf("Geser", "Ketuk"),
                        selected = if (mouseControlMode == MouseControlMode.SLIDE) 0 else 1,
                        onSelect = { index ->
                            onMouseControlModeChange(
                                if (index == 0) MouseControlMode.SLIDE else MouseControlMode.CLICK
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Geser: layar jadi trackpad. Ketuk: jari langsung jadi kursor.",
                        color = Amber.TextFaint,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ToggleRow(
                        title = "Geser kamera dari tombol",
                        subtitle = "Tahan tombol pukul atau lompat lalu geser jari untuk memutar kamera. Matikan untuk PvP.",
                        checked = buttonLookDrag,
                        onChange = onButtonLookDragChange
                    )

                    SectionTitle("PINTAS")
                    val keys = listOf(
                        Triple("F3", "Debug", LwjglGlfwKeycode.GLFW_KEY_F3),
                        Triple("F5", "Kamera", LwjglGlfwKeycode.GLFW_KEY_F5),
                        Triple("ESC", "Jeda", LwjglGlfwKeycode.GLFW_KEY_ESCAPE),
                        Triple("T", "Chat", LwjglGlfwKeycode.GLFW_KEY_T),
                        Triple("TAB", "Pemain", LwjglGlfwKeycode.GLFW_KEY_TAB),
                        Triple("F1", "Sembunyi HUD", LwjglGlfwKeycode.GLFW_KEY_F1)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        keys.take(3).forEach { (label, desc, code) ->
                            KeyChip(Modifier.weight(1f), label, desc) { onSendKeycode(code) }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        keys.drop(3).forEach { (label, desc, code) ->
                            KeyChip(Modifier.weight(1f), label, desc) { onSendKeycode(code) }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Satu-satunya jalan keluar dari game: tombol merah yang selalu terlihat di dasar panel.
                Spacer(modifier = Modifier.height(8.dp))
                ExitButton(
                    onClick = {
                        onDismissRequest()
                        onExitRequest()
                    }
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = text,
        color = Amber.YellowSoft,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp
    )
    Spacer(modifier = Modifier.height(6.dp))
}

/** Satu baris penggeser. Nilai sementara disimpan di sini supaya menggeser tidak menyusun ulang menu. */
@Composable
private fun SliderRow(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    step: Int,
    onCommit: (Int) -> Unit
) {
    var live by remember(value) { mutableFloatStateOf(value.toFloat()) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = Amber.Text,
                fontSize = 12.5.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${live.roundToInt()}%",
                color = Amber.Yellow,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
        AmberSlider(
            value = { live },
            range = min.toFloat()..max.toFloat(),
            step = step.toFloat(),
            onChange = { live = it },
            onFinished = { onCommit(live.roundToInt()) }
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Amber.Text,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = Amber.TextDim,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        AmberToggle(checked = checked, onChange = onChange)
    }
}

@Composable
private fun QuickTile(
    modifier: Modifier,
    icon: String,
    label: String,
    active: Boolean = false,
    slash: Boolean = false,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .height(56.dp)
            .clip(shape)
            .background(if (active) Amber.YellowTint else Amber.Fill)
            .border(1.dp, if (active) Amber.Yellow else Amber.Line, shape)
            .amberTap(onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AmberIcon(icon, 20.dp, if (active) Amber.Yellow else Amber.Text, slash = slash)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = if (active) Amber.YellowSoft else Amber.TextDim,
            fontSize = 11.sp,
            maxLines = 1
        )
    }
}

/**
 * Tombol rekam layar. Waktu berjalan dihitung hanya selama menu terbuka dan tidak menghitung
 * waktu jeda; saat menu tertutup tidak ada pekerjaan UI sama sekali dari fitur rekam.
 */
@Composable
private fun RecordBar(onClick: () -> Unit) {
    val st by ScreenRecorder.state.collectAsState()
    var seconds by remember { mutableLongStateOf(0L) }
    LaunchedEffect(st) {
        val s = st
        while (s.recording) {
            seconds = s.elapsedMs(SystemClock.elapsedRealtime()) / 1000L
            if (s.paused) break // waktu berhenti selama dijeda; tidak perlu dihitung ulang
            delay(1000L)
        }
    }
    val rec = st.recording
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(shape)
            .background(if (rec) Amber.DangerTint else Amber.Fill)
            .border(1.dp, if (rec) Amber.Danger else Amber.Line, shape)
            .amberTap(onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AmberIcon(if (rec) "stop" else "record", 20.dp, if (rec) Amber.Danger else Amber.Text)
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = if (rec) "Berhenti merekam" else "Rekam layar",
            color = if (rec) Amber.DangerSoft else Amber.Text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = if (rec) "%02d:%02d".format(seconds / 60L, seconds % 60L) else "Ketuk untuk mulai",
            color = if (rec) Amber.DangerSoft else Amber.TextDim,
            fontSize = 11.5.sp,
            maxLines = 1
        )
    }
}

/** Pilihan isi rekaman: game saja, atau game beserta tombol layarnya. Berlaku juga saat sedang merekam. */
@Composable
private fun RecordModeRow(
    withControls: Boolean,
    onWithControlsChange: (Boolean) -> Unit
) {
    val st by ScreenRecorder.state.collectAsState()
    AmberSegmented(
        options = listOf("Game saja", "Dengan tombol"),
        selected = if (withControls) 1 else 0,
        onSelect = { onWithControlsChange(it == 1) }
    )
    Text(
        text = when {
            st.recording -> "Rekaman dijeda selama menu terbuka dan lanjut otomatis saat menu ditutup."
            withControls -> "Tombol layar ikut terekam di video."
            else -> "Tombol layar disembunyikan dari video (tetap bisa disentuh)."
        },
        color = Amber.TextDim,
        fontSize = 10.5.sp,
        lineHeight = 14.sp,
        modifier = Modifier.padding(top = 4.dp, start = 2.dp, end = 2.dp)
    )
}

@Composable
private fun KeyChip(
    modifier: Modifier,
    label: String,
    desc: String,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .height(44.dp)
            .clip(shape)
            .background(Amber.Fill)
            .border(1.dp, Amber.Line, shape)
            .amberTap(onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            color = Amber.Text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Text(
            text = desc,
            color = Amber.TextDim,
            fontSize = 10.5.sp,
            maxLines = 1
        )
    }
}

/** Tombol keluar: merah penuh, tulisan gelap seperti tombol kuning di tempat lain. */
@Composable
private fun ExitButton(onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(shape)
            .background(Amber.Danger)
            .amberTap(onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        AmberIcon("power", 18.dp, Amber.Ink)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Keluar dari game",
            color = Amber.Ink,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ----------------------------------------------------------------------------
// Section: ui/dialogs/MCNewsDialog.kt
// ----------------------------------------------------------------------------

private enum class NewsFilter(val label: String) {
    ALL("Semua"),
    RELEASE("Rilis"),
    SNAPSHOT("Snapshot"),
    NEWS("Berita")
}

private fun openExternalUrl(context: Context, url: String) {
    if (url.isBlank()) return
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: Exception) {
    }
}

@Composable
fun MCNewsDialog(
    initialVersion: String? = null,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    var feed by remember { mutableStateOf(MCNewsService.getCachedFeed()?.copy(isCached = true)) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var reloadTick by remember { mutableIntStateOf(0) }
    var filter by remember { mutableStateOf(NewsFilter.ALL) }
    var searchQuery by remember(initialVersion) { mutableStateOf(initialVersion.orEmpty()) }
    var handledInitialVersion by remember(initialVersion) { mutableStateOf(false) }

    var activeItem by remember { mutableStateOf<MCNewsItem?>(null) }
    var detail by remember { mutableStateOf<PatchNoteDetail?>(null) }
    var detailLoading by remember { mutableStateOf(false) }
    var detailError by remember { mutableStateOf<String?>(null) }
    var detailReloadTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(reloadTick) {
        loading = true
        errorMessage = null
        val force = reloadTick > 0 || feed == null || feed?.isCached == true
        val result = MCNewsService.fetchFeed(forceRefresh = force)
        result.fold(
            onSuccess = { loaded ->
                feed = loaded
                errorMessage = null
                val targetVersion = initialVersion?.trim().orEmpty()
                if (targetVersion.isNotEmpty() && !handledInitialVersion) {
                    handledInitialVersion = true
                    MCNewsService.findItemForVersion(loaded, targetVersion)?.let { matched ->
                        activeItem = matched
                    }
                }
            },
            onFailure = { err ->
                errorMessage = err.message ?: "Gagal memuat pembaruan dari server Mojang."
                if (feed == null) {
                    feed = MCNewsService.getCachedFeed()?.copy(isCached = true)
                }
            }
        )
        loading = false
    }

    LaunchedEffect(activeItem?.id, detailReloadTick) {
        val current = activeItem ?: return@LaunchedEffect
        if (current.contentPath.isBlank()) {
            detail = null
            detailLoading = false
            detailError = null
            return@LaunchedEffect
        }
        detail = null
        detailLoading = true
        detailError = null
        MCNewsService.fetchPatchNoteDetail(
            contentPath = current.contentPath,
            forceRefresh = detailReloadTick > 0
        ).fold(
            onSuccess = { loaded -> detail = loaded },
            onFailure = { err -> detailError = err.message ?: "Gagal memuat catatan rilis." }
        )
        detailLoading = false
    }

    if (activeItem != null) {
        BackHandler {
            activeItem = null
        }
    }

    val dark = AmberUi.isDark
    val dialogEdge = remember(dark) {
        if (dark) {
            Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = 0.42f),
                    Color.White.copy(alpha = 0.08f),
                    Color.White.copy(alpha = 0.22f)
                )
            )
        } else {
            Brush.linearGradient(
                listOf(
                    Color(0xFF94A3B8).copy(alpha = 0.55f),
                    Color(0xFFCBD5E1).copy(alpha = 0.35f),
                    Color(0xFF94A3B8).copy(alpha = 0.55f)
                )
            )
        }
    }

    NuxWindowDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (dark) 0.62f else 0.42f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest
                )
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            // Ukuran kompak & proporsional: tidak memenuhi layar atau melar di tablet/lanskap.
            val availableW = maxWidth
            val availableH = maxHeight
            val dialogWidth = minOf(availableW * 0.84f, 540.dp)
                .coerceAtLeast(minOf(availableW, 300.dp))
            val dialogHeight = minOf(availableH * 0.88f, 430.dp)
                .coerceAtLeast(minOf(availableH, 260.dp))

            NuxCard(
                modifier = Modifier
                    .width(dialogWidth)
                    .height(dialogHeight)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                backgroundColor = NuxColors.Panel,
                borderBrush = dialogEdge,
                cornerRadius = 20.dp,
                fillMaxHeight = true
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    val selected = activeItem
                    if (selected != null) {
                        PatchDetailView(
                            item = selected,
                            detail = detail,
                            loading = detailLoading,
                            error = detailError,
                            onBack = { activeItem = null },
                            onRetry = { detailReloadTick++ },
                            onOpenExternal = { url -> openExternalUrl(context, url) },
                            onClose = onDismissRequest
                        )
                    } else {
                        NewsFeedHeader(
                            feed = feed,
                            loading = loading,
                            onRefresh = { reloadTick++ },
                            onClose = onDismissRequest
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        when {
                            loading && feed == null -> {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        AmberSpinner(
                                            modifier = Modifier.size(26.dp),
                                            color = AmberUi.AccentSoft,
                                            strokeWidth = 2.5.dp
                                        )
                                        Text(
                                            text = "Mengambil berita & versi terbaru dari Mojang...",
                                            color = NuxColors.GrayNeutral,
                                            fontSize = 12.5.sp
                                        )
                                    }
                                }
                            }

                            errorMessage != null && feed == null -> {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(horizontal = 20.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.CloudOff,
                                            contentDescription = null,
                                            tint = NuxColors.AmberDark,
                                            modifier = Modifier.size(30.dp)
                                        )
                                        Text(
                                            text = "Gagal memuat berita waktu nyata",
                                            color = NuxColors.DarkGray,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.5.sp
                                        )
                                        RawText(
                                            text = errorMessage.orEmpty(),
                                            color = NuxColors.GrayNeutral,
                                            fontSize = 12.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        AmberButton(
                                            text = "Coba lagi",
                                            onClick = { reloadTick++ },
                                            modifier = Modifier.testTag("news_retry_button"),
                                            style = AmberButtonStyle.Primary
                                        )
                                    }
                                }
                            }

                            else -> {
                                val currentFeed = feed
                                if (currentFeed != null) {
                                    if (errorMessage != null || currentFeed.isCached) {
                                        StaleCacheBanner(
                                            hasError = errorMessage != null,
                                            loading = loading,
                                            onRetry = { reloadTick++ }
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }

                                    NewsSearchBar(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        placeholder = Tr.t("Cari versi (mis. 1.21.4) atau judul berita..."),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("news_search_input")
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    val trimmedQuery = searchQuery.trim()
                                    val queryFiltered = remember(currentFeed.items, trimmedQuery) {
                                        if (trimmedQuery.isEmpty()) currentFeed.items
                                        else currentFeed.items.filter { item ->
                                            item.version.contains(trimmedQuery, ignoreCase = true) ||
                                                item.title.contains(trimmedQuery, ignoreCase = true) ||
                                                item.summary.contains(trimmedQuery, ignoreCase = true) ||
                                                item.category.contains(trimmedQuery, ignoreCase = true)
                                        }
                                    }
                                    val releaseCount = remember(queryFiltered) {
                                        queryFiltered.count { it.kind == NewsKind.RELEASE }
                                    }
                                    val snapshotCount = remember(queryFiltered) {
                                        queryFiltered.count { it.kind == NewsKind.SNAPSHOT }
                                    }
                                    val newsCount = remember(queryFiltered) {
                                        queryFiltered.count { it.kind == NewsKind.NEWS }
                                    }
                                    val shownItems = remember(queryFiltered, filter) {
                                        when (filter) {
                                            NewsFilter.ALL -> queryFiltered
                                            NewsFilter.RELEASE -> queryFiltered.filter { it.kind == NewsKind.RELEASE }
                                            NewsFilter.SNAPSHOT -> queryFiltered.filter { it.kind == NewsKind.SNAPSHOT }
                                            NewsFilter.NEWS -> queryFiltered.filter { it.kind == NewsKind.NEWS }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        NewsFilter.values().forEachIndexed { index, option ->
                                            val count = when (option) {
                                                NewsFilter.ALL -> queryFiltered.size
                                                NewsFilter.RELEASE -> releaseCount
                                                NewsFilter.SNAPSHOT -> snapshotCount
                                                NewsFilter.NEWS -> newsCount
                                            }
                                            FilterChipPill(
                                                label = option.label,
                                                count = count,
                                                selected = filter == option,
                                                modifier = Modifier.testTag("news_filter_$index"),
                                                onClick = { filter = option }
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    val showLiveStrip = trimmedQuery.isEmpty() &&
                                        (currentFeed.latestRelease != null || currentFeed.latestSnapshot != null)

                                    if (shownItems.isEmpty() && !showLiveStrip) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = if (trimmedQuery.isNotEmpty()) {
                                                        "Tidak ada versi atau berita yang cocok"
                                                    } else {
                                                        "Tidak ada item pada kategori ini"
                                                    },
                                                    color = NuxColors.GrayNeutral,
                                                    fontSize = 13.sp
                                                )
                                                if (trimmedQuery.isNotEmpty()) {
                                                    AmberButton(
                                                        text = "Reset pencarian",
                                                        onClick = { searchQuery = "" },
                                                        style = AmberButtonStyle.Ghost
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        LazyColumn(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            contentPadding = PaddingValues(bottom = 4.dp)
                                        ) {
                                            if (showLiveStrip) {
                                                item(key = "live_versions_strip") {
                                                    LiveVersionsStrip(
                                                        release = currentFeed.latestRelease,
                                                        snapshot = currentFeed.latestSnapshot,
                                                        onClickVersion = { versionId ->
                                                            val matched = MCNewsService.findItemForVersion(currentFeed, versionId)
                                                            if (matched != null) {
                                                                activeItem = matched
                                                            } else {
                                                                searchQuery = versionId
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                            if (shownItems.isEmpty()) {
                                                item(key = "empty_filtered") {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 20.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = "Tidak ada item pada kategori ini",
                                                            color = NuxColors.GrayNeutral,
                                                            fontSize = 13.sp
                                                        )
                                                    }
                                                }
                                            } else {
                                                items(shownItems, key = { it.id }) { item ->
                                                    val isLatestBadge = item.version.isNotBlank() && (
                                                        item.version == currentFeed.latestRelease?.id ||
                                                            item.version == currentFeed.latestSnapshot?.id
                                                        )
                                                    NewsCard(
                                                        item = item,
                                                        isLatest = isLatestBadge,
                                                        onClick = { activeItem = item }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactCircleIconButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(NuxColors.SurfaceElevated, CircleShape)
                .border(NuxSizes.GlassBorder, NuxColors.CardBorder, CircleShape),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}

@Composable
private fun StaleCacheBanner(
    hasError: Boolean,
    loading: Boolean,
    onRetry: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(NuxColors.Amber.copy(alpha = 0.12f), shape)
            .border(1.dp, NuxColors.Amber.copy(alpha = 0.38f), shape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.WarningAmber,
            contentDescription = null,
            tint = NuxColors.AmberDark,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (hasError) {
                "Menampilkan cache terakhir · Gagal menyegarkan"
            } else {
                "Menampilkan cache · Menyinkronkan pembaruan..."
            },
            color = NuxColors.DarkGray,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (!loading && hasError) {
            Spacer(modifier = Modifier.width(6.dp))
            RawText(
                text = Tr.t("Coba lagi"),
                color = NuxColors.AmberDark,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onRetry() }
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
private fun NewsSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(11.dp)
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 38.dp)
            .clip(shape)
            .background(NuxColors.SurfaceInput, shape)
            .border(1.dp, NuxColors.CardBorder, shape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = NuxColors.GrayNeutral,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) {
                RawText(
                    text = placeholder,
                    color = NuxColors.GrayNeutral.copy(alpha = 0.8f),
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    color = NuxColors.DarkGray,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(AmberUi.Accent),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (value.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable { onValueChange("") },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = Tr.t("Hapus teks"),
                    tint = NuxColors.GrayNeutral,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun NewsFeedHeader(
    feed: MCNewsFeed?,
    loading: Boolean,
    onRefresh: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(AmberUi.Accent.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Newspaper,
                contentDescription = null,
                tint = AmberUi.AccentSoft,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Berita & Versi Minecraft",
                color = NuxColors.DarkGray,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val subtitle = if (feed != null) {
                val clock = remember(feed.fetchedAtMillis) {
                    MCNewsService.formatClock(feed.fetchedAtMillis)
                }
                "Pembaruan langsung Mojang · $clock"
            } else {
                "Pembaruan langsung dari server resmi Mojang"
            }
            Text(
                text = subtitle,
                color = NuxColors.GrayNeutral,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        CompactCircleIconButton(
            onClick = onRefresh,
            enabled = !loading,
            modifier = Modifier.testTag("news_refresh_button")
        ) {
            if (loading) {
                AmberSpinner(
                    modifier = Modifier.size(15.dp),
                    color = AmberUi.AccentSoft,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = Tr.t("Segarkan"),
                    tint = AmberUi.AccentSoft,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
        CompactCircleIconButton(
            onClick = onClose,
            modifier = Modifier.testTag("news_close_button")
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = Tr.t("Tutup"),
                tint = NuxColors.GrayNeutral,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

@Composable
private fun LiveVersionsStrip(
    release: VersionItem?,
    snapshot: VersionItem?,
    onClickVersion: (String) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val stackVertically = maxWidth < 330.dp && release != null && snapshot != null
        if (stackVertically && release != null && snapshot != null) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LiveVersionChipCard(
                    label = "Rilis Terbaru",
                    version = release.id,
                    date = MCNewsService.formatIsoDate(release.releaseTime),
                    accent = NuxColors.SuccessGreen,
                    icon = Icons.Outlined.NewReleases,
                    onClick = { onClickVersion(release.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("news_live_release_card")
                )
                LiveVersionChipCard(
                    label = "Snapshot Terbaru",
                    version = snapshot.id,
                    date = MCNewsService.formatIsoDate(snapshot.releaseTime),
                    accent = NuxColors.AmberDark,
                    icon = Icons.Outlined.Science,
                    onClick = { onClickVersion(snapshot.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("news_live_snapshot_card")
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (release != null) {
                    LiveVersionChipCard(
                        label = "Rilis Terbaru",
                        version = release.id,
                        date = MCNewsService.formatIsoDate(release.releaseTime),
                        accent = NuxColors.SuccessGreen,
                        icon = Icons.Outlined.NewReleases,
                        onClick = { onClickVersion(release.id) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("news_live_release_card")
                    )
                }
                if (snapshot != null) {
                    LiveVersionChipCard(
                        label = "Snapshot Terbaru",
                        version = snapshot.id,
                        date = MCNewsService.formatIsoDate(snapshot.releaseTime),
                        accent = NuxColors.AmberDark,
                        icon = Icons.Outlined.Science,
                        onClick = { onClickVersion(snapshot.id) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("news_live_snapshot_card")
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveVersionChipCard(
    label: String,
    version: String,
    date: String,
    accent: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(11.dp)
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 46.dp)
            .clip(shape)
            .background(accent.copy(alpha = 0.10f), shape)
            .border(1.dp, accent.copy(alpha = 0.35f), shape)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(15.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                RawText(
                    text = version,
                    color = NuxColors.DarkGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (date.isNotBlank()) {
                    RawText(
                        text = date,
                        color = NuxColors.GrayNeutral,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = Tr.t("Lihat catatan perubahan"),
            tint = accent,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun FilterChipPill(
    label: String,
    count: Int,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = CircleShape
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 32.dp)
            .clip(shape)
            .background(
                if (selected) AmberUi.Accent else NuxColors.SurfaceElevated,
                shape
            )
            .border(
                1.dp,
                if (selected) AmberUi.AccentEdge else NuxColors.CardBorder,
                shape
            )
            .clickable { onClick() }
            .padding(horizontal = 11.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else NuxColors.DarkGray,
            fontSize = 11.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1
        )
        RawText(
            text = count.toString(),
            color = if (selected) Color.White.copy(alpha = 0.85f) else NuxColors.GrayNeutral,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NewsCard(
    item: MCNewsItem,
    isLatest: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(13.dp)
    val accent = when (item.kind) {
        NewsKind.RELEASE -> NuxColors.SuccessGreen
        NewsKind.SNAPSHOT -> NuxColors.AmberDark
        NewsKind.NEWS -> AmberUi.AccentSoft
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val thumbWidth = if (maxWidth < 340.dp) 68.dp else 84.dp
        val thumbGap = if (maxWidth < 340.dp) 8.dp else 10.dp
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(NuxColors.SurfaceInput, shape)
                .border(1.dp, NuxColors.CardBorder, shape)
                .clickable { onClick() }
                .padding(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (item.imageUrl.isNotBlank()) {
                NuxNetworkImage(
                    model = item.imageUrl,
                    contentDescription = item.title,
                    fallbackInitials = item.version.ifBlank { "MC" },
                    shape = RoundedCornerShape(9.dp),
                    contentScale = ContentScale.Crop,
                    maxSidePx = 320,
                    modifier = Modifier
                        .width(thumbWidth)
                        .aspectRatio(16f / 10f)
                        .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(9.dp))
                )
                Spacer(modifier = Modifier.width(thumbGap))
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FlowRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(accent.copy(alpha = 0.16f), RoundedCornerShape(5.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            RawText(
                                text = item.category.uppercase(),
                                color = accent,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                        if (isLatest) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(AmberUi.Accent.copy(alpha = 0.18f), RoundedCornerShape(5.dp))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "TERBARU",
                                    color = AmberUi.Accent,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1
                                )
                            }
                        }
                        if (item.version.isNotBlank()) {
                            RawText(
                                text = item.version,
                                color = NuxColors.DarkGray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (item.displayDate.isNotBlank()) {
                        RawText(
                            text = item.displayDate,
                            color = NuxColors.GrayNeutral,
                            fontSize = 10.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                RawText(
                    text = item.title,
                    color = NuxColors.DarkGray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.summary.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    RawText(
                        text = item.summary,
                        color = NuxColors.GrayNeutral,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (item.contentPath.isNotBlank()) "Lihat catatan perubahan" else "Baca selengkapnya",
                        color = accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (item.contentPath.isBlank()) {
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.PatchDetailView(
    item: MCNewsItem,
    detail: PatchNoteDetail?,
    loading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onOpenExternal: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val versionMeta = remember(item.version) {
        if (item.version.isNotBlank()) MCNewsService.findManifestVersion(item.version) else null
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompactCircleIconButton(
            onClick = onBack,
            modifier = Modifier.testTag("news_detail_back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = Tr.t("Kembali"),
                tint = NuxColors.DarkGray,
                modifier = Modifier.size(17.dp)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            RawText(
                text = detail?.title?.ifBlank { item.title } ?: item.title,
                color = NuxColors.DarkGray,
                fontWeight = FontWeight.Black,
                fontSize = 14.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            RawText(
                text = listOfNotNull(
                    item.version.takeIf { it.isNotBlank() },
                    item.category.takeIf { it.isNotBlank() },
                    item.displayDate.takeIf { it.isNotBlank() }
                ).joinToString(" · "),
                color = NuxColors.GrayNeutral,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        CompactCircleIconButton(
            onClick = {
                clipboard.setText(AnnotatedString(MCNewsService.toPlainText(item, detail)))
                Toast.makeText(context, "Disalin ke papan klip", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.testTag("news_detail_copy_button")
        ) {
            Icon(
                imageVector = Icons.Outlined.ContentCopy,
                contentDescription = Tr.t("Salin catatan"),
                tint = NuxColors.DarkGray,
                modifier = Modifier.size(16.dp)
            )
        }
        CompactCircleIconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = Tr.t("Tutup"),
                tint = NuxColors.GrayNeutral,
                modifier = Modifier.size(17.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    when {
        loading -> {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AmberSpinner(
                        modifier = Modifier.size(24.dp),
                        color = AmberUi.AccentSoft,
                        strokeWidth = 2.5.dp
                    )
                    Text(
                        text = "Memuat catatan perubahan...",
                        color = NuxColors.GrayNeutral,
                        fontSize = 12.sp
                    )
                }
            }
        }

        error != null -> {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 20.dp)
                ) {
                    RawText(
                        text = error,
                        color = NuxColors.GrayNeutral,
                        fontSize = 12.sp
                    )
                    AmberButton(
                        text = "Coba lagi",
                        onClick = onRetry,
                        style = AmberButtonStyle.Primary
                    )
                }
            }
        }

        else -> {
            val loaded = detail
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val heroUrl = loaded?.imageUrl?.ifBlank { item.imageUrl } ?: item.imageUrl
                if (heroUrl.isNotBlank()) {
                    NuxNetworkImage(
                        model = heroUrl,
                        contentDescription = item.title,
                        fallbackInitials = item.version.ifBlank { "MC" },
                        shape = RoundedCornerShape(12.dp),
                        contentScale = ContentScale.Crop,
                        maxSidePx = 640,
                        showSpinner = true,
                        modifier = Modifier
                            .widthIn(max = 320.dp)
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .align(Alignment.CenterHorizontally)
                            .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(12.dp))
                    )
                }

                if (versionMeta != null || item.manifestSha1.isNotBlank()) {
                    val sha = item.manifestSha1.ifBlank { versionMeta?.sha1.orEmpty() }
                    val metaShape = RoundedCornerShape(10.dp)
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(metaShape)
                            .background(NuxColors.SurfaceInput, metaShape)
                            .border(1.dp, NuxColors.CardBorder, metaShape)
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (item.version.isNotBlank()) {
                            MetaInfoChip(label = "Versi", value = item.version)
                        }
                        if (versionMeta != null) {
                            MetaInfoChip(label = "Tipe", value = versionMeta.type.uppercase())
                        }
                        if (sha.isNotBlank()) {
                            MetaInfoChip(label = "SHA-1", value = sha.take(10))
                        }
                    }
                }

                if (loaded != null && loaded.blocks.isNotEmpty()) {
                    for (block in loaded.blocks) {
                        when (block) {
                            is PatchNoteBlock.Heading -> {
                                Spacer(modifier = Modifier.height(2.dp))
                                RawText(
                                    text = block.text,
                                    color = if (block.level == 1) AmberUi.Accent else NuxColors.DarkGray,
                                    fontSize = when (block.level) {
                                        1 -> 14.5.sp
                                        2 -> 13.5.sp
                                        else -> 12.5.sp
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            is PatchNoteBlock.Paragraph -> {
                                RawText(
                                    text = block.text,
                                    color = NuxColors.GrayNeutral,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp
                                )
                            }
                            is PatchNoteBlock.Bullet -> {
                                Row(verticalAlignment = Alignment.Top) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 6.dp)
                                            .size(4.5.dp)
                                            .background(AmberUi.AccentSoft, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    RawText(
                                        text = block.text,
                                        color = NuxColors.DarkGray.copy(alpha = 0.9f),
                                        fontSize = 12.sp,
                                        lineHeight = 17.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            is PatchNoteBlock.Quote -> {
                                val quoteShape = RoundedCornerShape(10.dp)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(quoteShape)
                                        .background(AmberUi.Accent.copy(alpha = 0.12f), quoteShape)
                                        .border(1.dp, AmberUi.AccentSoft.copy(alpha = 0.35f), quoteShape)
                                        .padding(10.dp)
                                ) {
                                    RawText(
                                        text = block.text,
                                        color = NuxColors.DarkGray,
                                        fontSize = 11.5.sp,
                                        fontStyle = FontStyle.Italic,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                            is PatchNoteBlock.Image -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    NuxNetworkImage(
                                        model = block.url,
                                        contentDescription = block.alt.ifBlank { item.title },
                                        fallbackInitials = "MC",
                                        shape = RoundedCornerShape(10.dp),
                                        contentScale = ContentScale.Crop,
                                        maxSidePx = 560,
                                        showSpinner = true,
                                        modifier = Modifier
                                            .widthIn(max = 320.dp)
                                            .fillMaxWidth()
                                            .aspectRatio(16f / 9f)
                                            .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(10.dp))
                                    )
                                    if (block.alt.isNotBlank()) {
                                        RawText(
                                            text = block.alt,
                                            color = NuxColors.GrayNeutral,
                                            fontSize = 10.5.sp,
                                            fontStyle = FontStyle.Italic
                                        )
                                    }
                                }
                            }
                            PatchNoteBlock.Divider -> {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 3.dp),
                                    color = NuxColors.CardBorder
                                )
                            }
                        }
                    }
                } else if (item.summary.isNotBlank()) {
                    RawText(
                        text = item.summary,
                        color = NuxColors.GrayNeutral,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp
                    )
                }

                val links = loaded?.links.orEmpty()
                if (links.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Tautan terkait",
                        color = NuxColors.DarkGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        links.forEach { link ->
                            val chipShape = RoundedCornerShape(8.dp)
                            Row(
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 32.dp)
                                    .clip(chipShape)
                                    .background(NuxColors.SurfaceElevated, chipShape)
                                    .border(1.dp, NuxColors.CardBorder, chipShape)
                                    .clickable { onOpenExternal(link.url) }
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Link,
                                    contentDescription = null,
                                    tint = AmberUi.Accent,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                RawText(
                                    text = link.label,
                                    color = NuxColors.DarkGray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                if (item.readMoreUrl.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    AmberButton(
                        text = "Buka di Browser",
                        onClick = { onOpenExternal(item.readMoreUrl) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("news_open_web_button"),
                        style = AmberButtonStyle.Ghost,
                        icon = "external"
                    )
                }
            }
        }
    }
}

@Composable
private fun MetaInfoChip(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label:",
            color = NuxColors.GrayNeutral,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(4.dp))
        RawText(
            text = value,
            color = NuxColors.DarkGray,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

// ----------------------------------------------------------------------------
// Section: ui/dialogs/NovaGameAndDownloadDialogs.kt
// ----------------------------------------------------------------------------

/**
 * NovaCraft unified Game & Download Dialogs:
 * - ExitConfirmDialog
 * - GameConsolePanel
 * - NuxDownloadProgressDialog
 */

@Composable
fun ExitConfirmDialog(
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        GamepadWindowHook()
        NoWindowAnimationHook()
        val shape = RoundedCornerShape(22.dp)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 360.dp)
                    .clip(shape)
                    .background(Amber.Panel)
                    .border(1.dp, Amber.Line, shape)
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Text(
                    text = "Keluar dari game?",
                    color = Amber.Text,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Minecraft akan dihentikan dan kamu kembali ke launcher. Dunia yang belum tersimpan bisa hilang.",
                    color = Amber.TextDim,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PillButton(label = "Batal", filled = false, onClick = onCancel)
                    PillButton(label = "Keluar", filled = true, onClick = onConfirm)
                }
            }
        }
    }
}

@Composable
private fun PillButton(label: String, filled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .defaultMinSize(minHeight = 40.dp)
            .clip(shape)
            .background(if (filled) Amber.Danger else Amber.Fill)
            .border(1.dp, if (filled) Amber.DangerSoft else Amber.LineStrong, shape)
            .amberTap(onClick)
            .padding(horizontal = 22.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (filled) Amber.Ink else Amber.Text,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
fun GameConsolePanel(
    modifier: Modifier,
    fps: () -> Int,
    logs: List<String>,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onFpsClick: () -> Unit,
    onClose: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = if (expanded) 420.dp else 300.dp)
            .clip(shape)
            .background(Amber.Glass)
            .border(1.dp, Amber.Line, shape)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .amberTap(onFpsClick)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                FpsReadout(fps)
            }
            Box(modifier = Modifier.weight(1f))
            PanelIconButton(if (expanded) "chev_up" else "chev_down", onToggleExpanded)
            PanelIconButton("close", onClose)
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (expanded) {
            val listState = rememberLazyListState()
            LaunchedEffect(logs.size, logs.lastOrNull()) {
                if (logs.isNotEmpty()) listState.scrollToItem(logs.size - 1)
            }
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                items(logs) { line -> LogLine(line, maxLines = 4) }
            }
        } else {
            logs.takeLast(4).forEach { line -> LogLine(line, maxLines = 1) }
        }
    }
}

@Composable
private fun PanelIconButton(icon: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .amberTap(onClick),
        contentAlignment = Alignment.Center
    ) {
        AmberIcon(icon, 16.dp, Amber.TextDim)
    }
}

@Composable
private fun LogLine(text: String, maxLines: Int) {
    val color = when {
        text.contains("error", ignoreCase = true) ||
            text.contains("exception", ignoreCase = true) ||
            text.contains("fatal", ignoreCase = true) -> Amber.DangerSoft
        text.startsWith("[Amber") -> Amber.YellowSoft
        else -> Color(0xFFD8D6CC)
    }
    Text(
        text = text,
        color = color,
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun NuxDownloadProgressDialog(
    instanceName: String,
    progress: Float,
    message: String,
    onDismissRequest: () -> Unit = {},
    onCancel: (() -> Unit)? = null
) {
    val fraction = progress.coerceIn(0f, 1f)
    val percent = (fraction * 100f).toInt()

    val cancelActions: (@Composable RowScope.() -> Unit)? = if (onCancel == null) {
        null
    } else {
        {
            AmberButton(
                text = "Batal",
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                style = AmberButtonStyle.Ghost
            )
        }
    }

    AmberAlert(
        onDismissRequest = onDismissRequest,
        title = "Mengunduh game",
        icon = "download",
        accent = Amber.Yellow,
        actions = cancelActions
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = instanceName,
                color = Amber.Text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$percent%",
                color = Amber.Yellow,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
        Box(
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth()
                .height(10.dp)
                .clip(CircleShape)
                .drawBehind {
                    drawRect(Amber.Fill)
                    val w = size.width * fraction
                    if (w > 0f) {
                        drawRoundRect(
                            color = Amber.Yellow,
                            size = Size(w, size.height),
                            cornerRadius = CornerRadius(size.height / 2f)
                        )
                    }
                }
                .border(1.dp, Amber.Line, CircleShape)
        )
        Text(
            text = message.ifBlank { "Menyiapkan file game..." },
            color = Amber.TextDim,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

// ----------------------------------------------------------------------------
// Section: ui/dialogs/NovaUtilityDialogs.kt
// ----------------------------------------------------------------------------

// ============================================================================
// NovaCraft Unified Utility Dialogs (Crash, Storage, Import Layout, Renderer Warning, Delete Instance)
// ============================================================================

@Composable
fun NuxCrashDialog(
    crashInfo: GameCrashInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val logFile = remember(crashInfo.fullLogPath) {
        if (crashInfo.fullLogPath.isNotBlank()) File(crashInfo.fullLogPath)
        else File(context.filesDir, "latestlog.txt")
    }

    NuxDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = 900.dp),
        fillMaxHeight = true,
        compact = true
    ) {
        AmberCrashPanel(
            info = crashInfo,
            logText = crashInfo.logSnippet,
            logFile = logFile,
            onClose = onDismiss
        )
    }
}

@Composable
fun AmberStorageIntro(
    initiallyVisible: Boolean,
    onOpenSettings: () -> Unit,
    onFinished: () -> Unit
) {
    var visible by remember { mutableStateOf(initiallyVisible) }

    if (visible) {
        AmberAlert(
            onDismissRequest = {
                visible = false
                onFinished()
            },
            title = "Izin penyimpanan",
            icon = "gear",
            accent = Amber.Yellow,
            actions = {
                AmberButton(
                    text = "Nanti",
                    onClick = {
                        visible = false
                        onFinished()
                    },
                    modifier = Modifier.weight(1f),
                    style = AmberButtonStyle.Ghost
                )
                AmberButton(
                    text = "Buka pengaturan",
                    onClick = {
                        visible = false
                        onOpenSettings()
                    },
                    modifier = Modifier.weight(1f),
                    style = AmberButtonStyle.Primary
                )
            }
        ) {
            Text(
                text = "NovaCraft menyimpan dunia, mod, dan pengaturan di folder novacraft milik aplikasi tanpa memerlukan izin akses penuh penyimpanan.",
                color = Amber.Text,
                fontSize = 14.sp,
                lineHeight = 21.sp
            )
            Text(
                text = "Kamu tetap bisa memindahkan atau memasang mod eksternal kapan saja lewat pemilih berkas Android.",
                color = Amber.TextDim,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

@Composable
fun NuxImportLayoutDialog(
    buttonCount: Int,
    macroCommands: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    sourceName: String = "",
    skipped: Int = 0
) {
    AmberAlert(
        onDismissRequest = onDismiss,
        title = "Impor layout kontrol?",
        icon = "warning",
        accent = Amber.Yellow,
        actions = {
            AmberButton(
                text = "Batal",
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                style = AmberButtonStyle.Ghost
            )
            AmberButton(
                text = "Ganti layout",
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
                style = AmberButtonStyle.Primary,
                icon = "download"
            )
        }
    ) {
        Text(
            text = "Layout tombol saat ini akan diganti dengan $buttonCount tombol dari berkas ini.",
            color = Amber.TextDim,
            fontSize = 14.sp,
            lineHeight = 21.sp
        )
        if (sourceName.isNotEmpty()) {
            val note = if (skipped > 0) {
                "$skipped entri dilewati atau digabung karena tidak ada padanannya di NovaCraft (misalnya laci, mouse virtual, layer tersembunyi, dan tombol scroll kedua)."
            } else {
                "Semua tombol berhasil diubah. Periksa posisinya di editor kontrol."
            }
            Text(
                text = "Layout ini berasal dari $sourceName dan diubah ke format NovaCraft. $note",
                color = Amber.TextDim,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
        if (macroCommands > 0) {
            Text(
                text = "Berkas ini berisi $macroCommands tombol makro yang mengetik chat atau perintah otomatis. Periksa dulu sebelum dipakai di server.",
                color = Amber.TextDim,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

@Composable
fun NuxRendererWarningDialog(
    renderer: NuxRendererInfo,
    mcVersion: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AmberAlert(
        onDismissRequest = onDismiss,
        title = "Perender tidak cocok",
        icon = "warning",
        accent = Amber.Yellow,
        actions = {
            AmberButton(
                text = "Batal",
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                style = AmberButtonStyle.Ghost
            )
            AmberButton(
                text = "Tetap jalankan",
                onClick = onConfirm,
                modifier = Modifier.weight(1.3f),
                style = AmberButtonStyle.Primary
            )
        }
    ) {
        AmberInfoBox {
            Text(
                text = renderer.displayName,
                color = Amber.Text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Minecraft $mcVersion",
                color = Amber.TextDim,
                fontSize = 13.sp,
                maxLines = 1
            )
        }
        Text(
            text = "Perender ini tidak didukung secara resmi untuk Minecraft $mcVersion. Memaksakannya bisa membuat tampilan rusak, layar membeku, atau game crash. Lanjutkan?",
            color = Amber.TextDim,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            modifier = Modifier.padding(top = 12.dp)
        )
        if (renderer.compatibility.isNotBlank()) {
            Text(
                text = "Dukungan resmi: ${renderer.compatibility}",
                color = Amber.Yellow,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 19.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun NuxDeleteInstanceDialog(
    instance: Instance,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val loaderLine = buildString {
        append("Minecraft ")
        append(instance.mcVersion)
        append("  \u00B7  ")
        append(instance.loader.uppercase())
        if (instance.loaderVersion.isNotBlank()) {
            append(" ")
            append(instance.loaderVersion)
        }
    }

    AmberAlert(
        onDismissRequest = onDismiss,
        title = "Hapus instance?",
        icon = "trash",
        accent = Amber.Danger,
        actions = {
            AmberButton(
                text = "Batal",
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                style = AmberButtonStyle.Ghost
            )
            AmberButton(
                text = "Hapus",
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
                style = AmberButtonStyle.Danger,
                icon = "trash"
            )
        }
    ) {
        AmberInfoBox {
            Text(
                text = instance.name,
                color = Amber.Text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = loaderLine,
                color = Amber.TextDim,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = "Semua dunia, mod, savegame, dan konfigurasi di instance ini akan dihapus permanen dan tidak bisa dikembalikan.",
            color = Amber.TextDim,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

// ----------------------------------------------------------------------------
// Section: ui/dialogs/NuxAboutDialog.kt
// ----------------------------------------------------------------------------

private const val COPY = "Copyright \u00A9"
private const val GPL_URL = "https://www.gnu.org/licenses/gpl-3.0.html"
private const val CO_DEVELOPER_NAME = "MinhToi"

private data class AboutProject(
    val title: String,
    val copyright: String,
    val license: String,
    val description: String,
    val url: String
)

private data class OpenSourceLibrary(
    val name: String,
    val copyright: String,
    val license: String,
    val role: String,
    val url: String
)

private val PROJECTS = listOf(
    AboutProject(
        title = "NovaCraft",
        copyright = "$COPY 2026 ItsVynzzzz & Contributors",
        license = "GNU General Public License v3.0 (GPL-3.0)",
        description = "Peluncur ini. Kode sumbernya terbuka dan boleh dipelajari, diubah, dan dibagikan lagi dengan syarat GPL-3.0 yang sama.",
        url = "https://github.com/ItsVynzzzz/AmberLauncher"
    ),
    AboutProject(
        title = "Zalith Launcher 2",
        copyright = "$COPY 2024-2026 MovTery & Contributors",
        license = "GNU General Public License v3.0 (GPL-3.0)",
        description = "Komponen arsitektur bridge JNI native (ZLBridge), integrasi SDL3 surface, direct gamepad input, penanganan resolusi DNS, dan optimasi eksekusi OpenJDK mobile.",
        url = "https://github.com/ZalithLauncher/ZalithLauncher2"
    ),
    AboutProject(
        title = "PojavLauncher",
        copyright = "$COPY 2020-present PojavLauncherTeam & Contributors",
        license = "GNU Lesser General Public License v3.0 (LGPL-3.0)",
        description = "Proyek pionir peluncuran Minecraft Java Edition di Android. Menyediakan porting LWJGL 3, GLFW Stubs, AWT stubs, dan integrasi grafis renderer mobile.",
        url = "https://github.com/PojavLauncherTeam/PojavLauncher"
    ),
    AboutProject(
        title = "NUX Launcher (sumber asal fork)",
        copyright = "$COPY 2026 IsraaDeveloper & Contributors",
        license = "GNU General Public License v3.0 (GPL-3.0)",
        description = "Proyek asal yang di-fork oleh NovaCraft. Kode sumber terbuka di bawah GNU GPL-3.0.",
        url = "https://github.com/IsraaDeveloper/nuxlabs"
    )
)

private val LIBRARIES = listOf(
    OpenSourceLibrary(
        name = "Zalith Launcher 2",
        copyright = "$COPY 2024-2026 MovTery & Contributors",
        license = "GPL-3.0 License",
        role = "Arsitektur runtime JVM, ZLBridge JNI, integrasi SDL3, direct gamepad, dan optimasi Android",
        url = "https://github.com/ZalithLauncher/ZalithLauncher2"
    ),
    OpenSourceLibrary(
        name = "PojavLauncher",
        copyright = "$COPY 2020-present PojavLauncherTeam & Contributors",
        license = "LGPL-3.0 License",
        role = "Mesin peluncuran Java Minecraft Android, LWJGL 3 Android Natives, GLFW Stub",
        url = "https://github.com/PojavLauncherTeam/PojavLauncher"
    ),
    OpenSourceLibrary(
        name = "Horizon (Android port)",
        copyright = "$COPY 2026 Ludovic Marie, Vasysik & Contributors",
        license = "MIT License",
        role = "Implementasi client Minecraft Java berbasis Rust (arm64-v8a)",
        url = "https://github.com/starling-client/starling"
    ),
    OpenSourceLibrary(
        name = "MobileGlues",
        copyright = "$COPY 2023-2026 MobileGlues Team",
        license = "LGPL-2.1 License",
        role = "Penerjemah modern OpenGL 4.0 ke OpenGL ES 3.2 untuk Minecraft 1.17+",
        url = "https://github.com/FCL-Team/MobileGlues"
    ),
    OpenSourceLibrary(
        name = "LWJGL - Lightweight Java Game Library",
        copyright = "$COPY 2012-present LWJGL All rights reserved",
        license = "BSD 3-Clause License",
        role = "Pustaka binding grafis, audio (OpenAL), dan jendela game Java",
        url = "https://github.com/LWJGL/lwjgl3"
    ),
    OpenSourceLibrary(
        name = "SDL3 & sdl2-compat",
        copyright = "$COPY 1997-2026 Sam Lantinga",
        license = "Zlib License",
        role = "Manajemen surface rendering native, touch input, dan audio output",
        url = "https://github.com/libsdl-org/SDL"
    ),
    OpenSourceLibrary(
        name = "Mesa 3D (Zink / Turnip / VirGL)",
        copyright = "$COPY The Mesa Authors",
        license = "MIT License",
        role = "Vulkan-to-OpenGL translation & driver akselerasi GPU mobile",
        url = "https://mesa3d.org/"
    ),
    OpenSourceLibrary(
        name = "ANGLE",
        copyright = "$COPY 2018 The ANGLE Project Authors",
        license = "BSD 3-Clause License",
        role = "Mesin penerjemah OpenGL ES berbasis backend Vulkan",
        url = "https://angleproject.org/"
    ),
    OpenSourceLibrary(
        name = "ByteHook",
        copyright = "$COPY 2020-2024 ByteDance, Inc.",
        license = "MIT License",
        role = "Dynamic binary instrumentation dan hook Android library",
        url = "https://github.com/bytedance/bhook"
    ),
    OpenSourceLibrary(
        name = "TouchController (proxy client)",
        copyright = "$COPY fifth_light",
        license = "LGPL-3.0",
        role = "Klien proxy untuk mod kontrol sentuh TouchController",
        url = "https://github.com/TouchController/TouchController"
    ),
    OpenSourceLibrary(
        name = "Jetpack Compose & Material 3",
        copyright = "$COPY The Android Open Source Project",
        license = "Apache 2.0",
        role = "Toolkit UI modern, deklaratif, dan responsif",
        url = "https://developer.android.com/jetpack/compose"
    ),
    OpenSourceLibrary(
        name = "Modrinth API",
        copyright = "$COPY Rinth, Inc. & Community",
        license = "Open API / AGPL",
        role = "Katalog publik mod, modpack, shaderpack, dan resource pack",
        url = "https://modrinth.com"
    ),
    OpenSourceLibrary(
        name = "sora-editor",
        copyright = "Copyright (C) 2020-2026 Rosemoe",
        license = "LGPL-2.1 License",
        role = "Editor teks dan log penampil performa tinggi",
        url = "https://github.com/Rosemoe/sora-editor"
    ),
    OpenSourceLibrary(
        name = "skinview3d",
        copyright = "$COPY Kent Rasmussen & contributors",
        license = "MIT License",
        role = "Viewer interaktif 3D skin & jubah Minecraft berbasis WebGL",
        url = "https://github.com/bs-community/skinview3d"
    ),
    OpenSourceLibrary(
        name = "OkHttp & Okio",
        copyright = "$COPY Square, Inc.",
        license = "Apache 2.0",
        role = "Klien HTTP jaringan performa tinggi",
        url = "https://github.com/square/okhttp"
    )
)

private fun openLink(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: Exception) {
        // Tidak ada peramban: tidak ada yang bisa dilakukan, biarkan dialog tetap terbuka.
    }
}

private fun appVersionName(context: Context): String {
    return try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
    } catch (e: Exception) {
        ""
    }
}

/**
 * Dialog Tentang: ringkasan proyek, tim pengembang, ucapan terima kasih khusus, daftar pustaka open
 * source, dan ketentuan GPL-3.0 (termasuk syarat tambahan Zalith). Di layar lebar tabnya jadi menu
 * samping, di layar sempit jadi segmen di atas.
 */
@Composable
fun NuxAboutDialog(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }
    val scroll = rememberScrollState()
    LaunchedEffect(tab) { scroll.scrollTo(0) }

    val version = remember(context) { appVersionName(context) }
    val onOpen: (String) -> Unit = remember(context) { { url: String -> openLink(context, url) } }
    val tabs = remember { listOf("Ringkasan", "Pustaka (${LIBRARIES.size})", "GPL-3.0") }

    NuxDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.widthIn(max = 720.dp),
        fillMaxHeight = true,
        compact = true
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            if (maxWidth >= 520.dp) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .width(208.dp)
                            .fillMaxHeight()
                            .padding(horizontal = 12.dp, vertical = 14.dp)
                    ) {
                        Brand(version)
                        Spacer(modifier = Modifier.height(14.dp))
                        tabs.forEachIndexed { index, label ->
                            RailTab(
                                label = label,
                                selected = index == tab,
                                modifier = Modifier.testTag("about_tab_$index")
                            ) { tab = index }
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        CloseButton(
                            onClick = onDismissRequest,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("about_close_button")
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(Amber.Line)
                    )
                    AboutContent(
                        tab = tab,
                        onOpen = onOpen,
                        scroll = scroll,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                ) {
                    Brand(version)
                    Spacer(modifier = Modifier.height(10.dp))
                    AmberSegmented(options = tabs, selected = tab, onSelect = { tab = it })
                    AboutContent(
                        tab = tab,
                        onOpen = onOpen,
                        scroll = scroll,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    CloseButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("about_close_button")
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutContent(
    tab: Int,
    onOpen: (String) -> Unit,
    scroll: ScrollState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(scroll)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        when (tab) {
            0 -> OverviewTab(onOpen)
            1 -> LibrariesTab(onOpen)
            else -> LicenseTab(onOpen)
        }
    }
}

@Composable
private fun OverviewTab(onOpen: (String) -> Unit) {
    AboutTitle(
        title = "Tentang novacraft launcher",
        subtitle = "Peluncur Minecraft Java modern untuk Android, ringan dan stabil."
    )
    Notice(
        title = "Unofficial Modified Version",
        body = "novacraft launcher adalah peluncur independen hasil modifikasi dari NUX Launcher, yang sendirinya berasal dari Zalith Launcher 2 dan PojavLauncher. Aplikasi ini bukan aplikasi resmi Zalith Launcher Team, NUX Launcher, maupun Mojang Studios.",
        highlight = true
    )
    SubHeading("Tim pengembang")
    CoDeveloperCard(
        name = CO_DEVELOPER_NAME,
        role = "Pengembang Pendamping",
        description = "Mengelola penyesuaian NovaCraft, pembaruan antarmuka modern, penyelarasan 64-bit, dan perbaikan stabilitas."
    )
    SubHeading("Ucapan terima kasih khusus")
    ThanksCard(
        icon = Icons.Outlined.AutoAwesome,
        title = "Penulis asli & pelopor",
        body = "Terima kasih sebesar-besarnya kepada ItsVynzzzz (AmberLauncher), IsraaDeveloper (NUX Launcher), MovTery (Zalith Launcher 2), PojavLauncherTeam, serta tim Horizon atas fondasi teknis yang luar biasa."
    )
    ThanksCard(
        icon = Icons.Outlined.Groups,
        title = "Komunitas Minecraft & sumber terbuka",
        body = "Terima kasih kepada seluruh komunitas pemain, penguji, pembuat mod, penerjemah, dan kontributor open source yang terus memberi masukan dan menjaga ekosistem Minecraft Java di Android tetap hidup."
    )
    SubHeading("Proyek dan hak cipta")
    for (project in PROJECTS) {
        ProjectRow(project, onOpen)
    }
    Notice(
        title = "Penafian Minecraft",
        body = "Bukan produk resmi Minecraft. Tidak disetujui oleh atau terkait dengan Mojang Studios atau Microsoft. Minecraft adalah merek dagang milik Mojang AB / Microsoft Corporation. Seluruh aset game diunduh langsung dari server distribusi resmi Mojang.",
        highlight = false
    )
}

@Composable
private fun LibrariesTab(onOpen: (String) -> Unit) {
    AboutTitle(
        title = "Pustaka open source",
        subtitle = "Ketuk untuk membuka halaman proyeknya."
    )
    for (lib in LIBRARIES) {
        LibraryRow(lib, onOpen)
    }
}

@Composable
private fun LicenseTab(onOpen: (String) -> Unit) {
    AboutTitle(
        title = "GNU GPL-3.0",
        subtitle = "Lisensi yang menaungi NovaCraft."
    )
    Body("Proyek ini dilisensikan di bawah ketentuan GNU General Public License versi 3.0.")
    SubHeading("Empat kebebasan perangkat lunak bebas")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Numbered(1, "Kebebasan untuk menjalankan program untuk keperluan apa pun.")
        Numbered(2, "Kebebasan untuk mempelajari cara kerja program dan menyesuaikannya dengan kebutuhan Anda.")
        Numbered(3, "Kebebasan untuk mendistribusikan kembali salinan untuk membantu sesama.")
        Numbered(4, "Kebebasan untuk menyempurnakan program dan merilis penyempurnaan tersebut kepada publik.")
    }
    SubHeading("Ketentuan tambahan (GPLv3 Pasal 7)")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Bullet("Modifikasi tidak boleh menyalahgunakan nama dagang 'ZalithLauncher' atau 'ZL'.")
        Bullet("Modifikasi wajib menampilkan keterangan bahwa ini adalah 'Unofficial Modified Version'.")
        Bullet("Hak cipta penulis asli (Copyright \u00A9 MovTery & PojavLauncherTeam) tidak boleh dihapus.")
    }
    Spacer(modifier = Modifier.height(2.dp))
    AmberButton(
        text = "Baca teks lisensi lengkap",
        onClick = { onOpen(GPL_URL) },
        modifier = Modifier.fillMaxWidth(),
        style = AmberButtonStyle.Ghost,
        icon = "external"
    )
}

// ---------------------------------------------------------------------------------------------
// Bagian kecil
// ---------------------------------------------------------------------------------------------

@Composable
private fun Brand(version: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(id = R.drawable.img_novacraft_logo),
            contentDescription = "NovaCraft logo",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Amber.LineStrong, RoundedCornerShape(12.dp))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = "novacraft launcher",
                color = Amber.Text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            if (version.isNotBlank()) {
                Text(
                    text = "Versi $version",
                    color = Amber.TextDim,
                    fontSize = 12.5.sp,
                    maxLines = 1
                )
            }
            // Pemberitahuan versi modifikasi tidak resmi (dulu tampil di Home, kini cukup di sini)
            Text(
                text = "UNOFFICIAL MODIFIED VERSION",
                color = Amber.TextDim,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun RailTab(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .background(if (selected) Amber.YellowTint else Color.Transparent)
            .amberTap(onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(16.dp)
                .background(
                    if (selected) Amber.Yellow else Color.Transparent,
                    RoundedCornerShape(2.dp)
                )
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            color = if (selected) Amber.Yellow else Amber.TextDim,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun CloseButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    AmberButton(
        text = "Tutup",
        onClick = onClick,
        modifier = modifier,
        style = AmberButtonStyle.Ghost
    )
}

@Composable
private fun CoDeveloperCard(
    name: String,
    role: String,
    description: String
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Amber.Fill)
            .border(1.dp, Amber.LineStrong, shape)
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Amber.YellowTint)
                .border(1.dp, Amber.Yellow.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.PersonOutline,
                contentDescription = role,
                tint = Amber.Yellow,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    color = Amber.Text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Chip("Co-Developer")
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = role,
                color = Amber.YellowSoft,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = description,
                color = Amber.TextDim,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        }
    }
}

@Composable
private fun ThanksCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Amber.Fill)
            .border(1.dp, Amber.Line, shape)
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Amber.YellowTint),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Amber.Yellow,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    color = Amber.Text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    tint = Amber.YellowSoft,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = body,
                color = Amber.TextDim,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        }
    }
}

@Composable
private fun AboutTitle(title: String, subtitle: String) {
    Column {
        Text(
            text = title,
            color = Amber.Text,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            color = Amber.TextDim,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun SubHeading(text: String) {
    Text(
        text = text,
        color = Amber.Yellow,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun Body(text: String) {
    Text(
        text = text,
        color = Amber.TextDim,
        fontSize = 14.sp,
        lineHeight = 21.sp
    )
}

@Composable
private fun Notice(title: String, body: String, highlight: Boolean) {
    val shape = RoundedCornerShape(12.dp)
    val fill = if (highlight) Amber.YellowTint else Amber.Fill
    val line = if (highlight) Amber.Yellow.copy(alpha = 0.40f) else Amber.Line
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(fill)
            .border(1.dp, line, shape)
            .padding(12.dp)
    ) {
        Text(
            text = title,
            color = if (highlight) Amber.Yellow else Amber.Text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = body,
            color = Amber.TextDim,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )
    }
}

@Composable
private fun ProjectRow(project: AboutProject, onOpen: (String) -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Amber.Fill)
            .border(1.dp, Amber.Line, shape)
            .amberTap { onOpen(project.url) }
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = project.title,
                color = Amber.Text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            AmberIcon("external", 16.dp, Amber.TextDim)
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = project.copyright,
            color = Amber.TextFaint,
            fontSize = 12.sp
        )
        Text(
            text = project.license,
            color = Amber.TextFaint,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = project.description,
            color = Amber.TextDim,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )
    }
}

@Composable
private fun LibraryRow(lib: OpenSourceLibrary, onOpen: (String) -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Amber.Fill)
            .border(1.dp, Amber.Line, shape)
            .amberTap { onOpen(lib.url) }
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = lib.name,
                color = Amber.Text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Chip(lib.license.removeSuffix(" License"))
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = lib.copyright,
            color = Amber.TextFaint,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = lib.role,
            color = Amber.TextDim,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )
    }
}

@Composable
private fun Chip(text: String) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(Amber.YellowTint)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            color = Amber.Yellow,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun Numbered(number: Int, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(Amber.YellowTint, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                color = Amber.Yellow,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            color = Amber.TextDim,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun Bullet(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .padding(top = 7.dp, start = 7.dp)
                .size(6.dp)
                .background(Amber.Yellow, CircleShape)
        )
        Spacer(modifier = Modifier.width(13.dp))
        Text(
            text = text,
            color = Amber.TextDim,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

// ----------------------------------------------------------------------------
// Section: ui/dialogs/NuxAddonImportDialog.kt
// ----------------------------------------------------------------------------

private data class AddonTypeOption(val id: String, val label: String, val icon: String)

private val ADDON_TYPES = listOf(
    AddonTypeOption("mods", "Mod Java (.jar)", "🧩"),
    AddonTypeOption("worlds", "Dunia Minecraft Java (.zip)", "🌍"),
    AddonTypeOption("horizon_mods", "Mod Horizon (.hmod)", "⚡"),
    AddonTypeOption("modpacks", "Modpack (.mrpack / .zip)", "📦"),
    AddonTypeOption("resourcepacks", "Resource Pack (.zip)", "🎨"),
    AddonTypeOption("shaderpacks", "Shaderpack (.zip)", "✨"),
    AddonTypeOption("horizon_shaders", "Shader Horizon (.hshader)", "🌟")
)

@Composable
fun NuxAddonImportDialog(
    pendingImport: PendingAddonImport,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val instances by InstanceManager.instances.collectAsState()
    val activeInstance by InstanceManager.selectedInstance.collectAsState()

    var selectedType by remember {
        mutableStateOf(
            if (ADDON_TYPES.any { it.id == pendingImport.suggestedType }) {
                pendingImport.suggestedType
            } else {
                "mods"
            }
        )
    }

    val isHorizonAddon = selectedType == "horizon_mods" || selectedType == "horizon_shaders"

    var selectedInstance by remember {
        mutableStateOf<Instance?>(
            when {
                isHorizonAddon -> {
                    activeInstance?.takeIf { com.aistudio.novacraft.launcher.core.mods.HorizonEngine.isEngine(it.loader) }
                        ?: instances.firstOrNull { com.aistudio.novacraft.launcher.core.mods.HorizonEngine.isEngine(it.loader) }
                        ?: activeInstance ?: instances.firstOrNull()
                }
                selectedType == "mods" || selectedType == "modpacks" || selectedType == "worlds" -> {
                    activeInstance?.takeIf { !com.aistudio.novacraft.launcher.core.mods.HorizonEngine.isEngine(it.loader) }
                        ?: instances.firstOrNull { !com.aistudio.novacraft.launcher.core.mods.HorizonEngine.isEngine(it.loader) }
                        ?: activeInstance ?: instances.firstOrNull()
                }
                else -> activeInstance ?: instances.firstOrNull()
            }
        )
    }

    var isImporting by remember { mutableStateOf(false) }
    var importStatusText by remember { mutableStateOf("") }
    var importCurrent by remember { mutableStateOf(0) }
    var importTotal by remember { mutableStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    NuxWindowDialog(
        onDismissRequest = {
            if (!isImporting) {
                NovaUnifiedFileImportEngine.clearPendingImport()
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !isImporting,
            dismissOnClickOutside = !isImporting,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f))
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            NuxCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .fillMaxHeight(0.92f),
                backgroundColor = NuxColors.Panel,
                borderColor = NuxColors.CardBorder,
                cornerRadius = NuxSizes.CornerRadiusLarge,
                fillMaxHeight = false
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NuxColors.YellowPale)
                                    .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "📥", fontSize = 16.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "NOVACRAFT ADDON INSTALLER",
                                    color = NuxColors.DarkGray,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "Impor Mod & Addon Eksternal",
                                    color = NuxColors.SageGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (!isImporting) {
                            val closeShape = RoundedCornerShape(8.dp)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(closeShape)
                                    .background(NuxColors.SurfaceInput, closeShape)
                                    .border(NuxSizes.BorderWidth, NuxColors.CardBorder, closeShape)
                                    .clickable {
                                        NovaUnifiedFileImportEngine.clearPendingImport()
                                        onDismiss()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "✕",
                                    color = NuxColors.DarkGray,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Scrollable Dialog Body
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // File Info Box
                        val fileCardShape = RoundedCornerShape(10.dp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(fileCardShape)
                                .background(NuxColors.SurfaceWhite, fileCardShape)
                                .border(1.dp, NuxColors.DarkGray.copy(alpha = 0.27f), fileCardShape)
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(NuxColors.SurfaceElevated),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val icon = when (selectedType) {
                                        "mods" -> "🧩"
                                        "worlds" -> "🌍"
                                        "horizon_mods" -> "⚡"
                                        "modpacks" -> "📦"
                                        "resourcepacks" -> "🎨"
                                        "shaderpacks" -> "✨"
                                        "horizon_shaders" -> "🌟"
                                        else -> "📄"
                                    }
                                    Text(text = icon, fontSize = 20.sp)
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = pendingImport.fileName,
                                        color = NuxColors.DarkGray,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "Tipe file: ${selectedType.uppercase()} · Siap dipasang",
                                        color = NuxColors.GrayNeutral,
                                        fontSize = 10.5.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Type Selector Chips
                        Text(
                            text = "KATEGORI ADDON",
                            color = NuxColors.GrayNeutral,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(ADDON_TYPES) { option ->
                                val isSelected = selectedType == option.id
                                val chipShape = RoundedCornerShape(8.dp)
                                Box(
                                    modifier = Modifier
                                        .clip(chipShape)
                                        .background(
                                            if (isSelected) NuxColors.YellowPale else NuxColors.SurfaceElevated,
                                            chipShape
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) NuxColors.ForestGreen else NuxColors.DarkGray.copy(alpha = 0.19f),
                                            shape = chipShape
                                        )
                                        .clickable(enabled = !isImporting) {
                                            selectedType = option.id
                                            val wantsRust = option.id == "horizon_mods" || option.id == "horizon_shaders"
                                            val wantsJava = option.id == "mods" || option.id == "modpacks" || option.id == "worlds"
                                            if (wantsRust && (selectedInstance == null || !com.aistudio.novacraft.launcher.core.mods.HorizonEngine.isEngine(selectedInstance?.loader))) {
                                                instances.firstOrNull { com.aistudio.novacraft.launcher.core.mods.HorizonEngine.isEngine(it.loader) }?.let {
                                                    selectedInstance = it
                                                }
                                            } else if (wantsJava && selectedInstance != null && com.aistudio.novacraft.launcher.core.mods.HorizonEngine.isEngine(selectedInstance?.loader)) {
                                                instances.firstOrNull { !com.aistudio.novacraft.launcher.core.mods.HorizonEngine.isEngine(it.loader) }?.let {
                                                    selectedInstance = it
                                                }
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = option.icon, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = option.label,
                                            color = if (isSelected) NuxColors.DarkGray else NuxColors.GrayNeutral,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Instance Selector
                        Text(
                            text = "TARGET INSTANCE MINECRAFT",
                            color = NuxColors.GrayNeutral,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        if (instances.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NuxColors.ErrorRed.copy(alpha = 0.12f))
                                    .border(1.dp, NuxColors.ErrorRed.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = "⚠️ Belum ada instance Minecraft yang dibuat! Buat instance terlebih dahulu di Dashboard.",
                                    color = NuxColors.ErrorRed,
                                    fontSize = 11.sp
                                )
                            }
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(instances) { inst ->
                                    val isSelected = selectedInstance?.id == inst.id
                                    val cardShape = RoundedCornerShape(8.dp)
                                    Box(
                                        modifier = Modifier
                                            .clip(cardShape)
                                            .background(
                                            if (isSelected) NuxColors.YellowPale else NuxColors.SurfaceElevated,
                                                cardShape
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) NuxColors.ForestGreen else NuxColors.DarkGray.copy(alpha = 0.19f),
                                                shape = cardShape
                                            )
                                            .clickable(enabled = !isImporting) {
                                                selectedInstance = inst
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Column {
                                                Text(
                                                    text = inst.name,
                                                    color = NuxColors.DarkGray,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "v${inst.mcVersion} · ${inst.loader.uppercase()}",
                                                    color = if (isSelected) NuxColors.SageGreen else NuxColors.GrayNeutral,
                                                    fontSize = 10.sp
                                                )
                                            }
                                            if (isSelected) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .background(NuxColors.ForestGreen, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    androidx.compose.material3.Icon(
                                                        imageVector = androidx.compose.material.icons.Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = Color.Black,
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Progress Section
                        if (isImporting) {
                            Spacer(modifier = Modifier.height(14.dp))
                            val progressShape = RoundedCornerShape(8.dp)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(progressShape)
                                    .background(NuxColors.SoftLime, progressShape)
                                    .border(1.dp, NuxColors.CardBorder, progressShape)
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (importStatusText.isNotBlank()) importStatusText else "Memasang addon...",
                                            color = NuxColors.SageGreen,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (importTotal > 0) {
                                            Text(
                                                text = "$importCurrent / $importTotal",
                                                color = NuxColors.DarkGray,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (importTotal > 0) {
                                        LinearProgressIndicator(
                                            progress = { (importCurrent.toFloat() / importTotal.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = NuxColors.SageGreen,
                                            trackColor = NuxColors.SoftLime
                                        )
                                    } else {
                                        AmberWaitBar(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = NuxColors.SageGreen,
                                            trackColor = NuxColors.SoftLime
                                        )
                                    }
                                }
                            }
                        }

                        // Error message
                        errorMessage?.let { err ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NuxColors.ErrorRed.copy(alpha = 0.12f))
                                    .border(1.dp, NuxColors.ErrorRed.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = "Gagal: $err",
                                    color = NuxColors.ErrorRed,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Action Buttons (Sticky at bottom, always visible)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isImporting) {
                            Box(
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        NovaUnifiedFileImportEngine.clearPendingImport()
                                        onDismiss()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "BATAL",
                                    color = NuxColors.GrayNeutral,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        val canInstall = selectedInstance != null && !isImporting
                        NuxButton(
                            onClick = {
                                val target = selectedInstance ?: return@NuxButton
                                isImporting = true
                                errorMessage = null
                                importStatusText = "Mempersiapkan pemasangan..."
                                importCurrent = 0
                                importTotal = 0

                                scope.launch {
                                    val result = NovaUnifiedFileImportEngine.importLocalFile(
                                        context = context,
                                        instance = target,
                                        itemType = selectedType,
                                        uri = pendingImport.uri,
                                        onProgress = { status, current, total ->
                                            importStatusText = status
                                            importCurrent = current
                                            importTotal = total
                                        }
                                    )

                                    isImporting = false
                                    if (result.isSuccess) {
                                        Toast.makeText(
                                            context,
                                            "✓ Berhasil memasang \"${pendingImport.fileName}\" ke ${target.name}!",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        NovaUnifiedFileImportEngine.clearPendingImport()
                                        onDismiss()
                                    } else {
                                        val err = result.exceptionOrNull()?.message ?: "Terjadi kesalahan yang tidak diketahui"
                                        errorMessage = err
                                        Toast.makeText(
                                            context,
                                            "Gagal memasang: $err",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            },
                            enabled = canInstall,
                            backgroundColor = NuxColors.ForestGreen,
                            contentColor = NuxColors.DarkGray,
                            cornerRadius = 8.dp,
                            modifier = Modifier.defaultMinSize(minHeight = 40.dp)
                        ) {
                            if (isImporting) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AmberSpinner(
                                        modifier = Modifier.size(16.dp),
                                        color = NuxColors.Ink,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "MEMASANG...",
                                        color = NuxColors.Ink,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            } else {
                                Text(
                                    text = "PASANG SEKARANG",
                                    color = NuxColors.Ink,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------
// Section: ui/dialogs/NuxEditInstanceDialog.kt
// ----------------------------------------------------------------------------

/**
 * Halaman "Ubah instance" (tombol pensil di Home): nama, versi Java, aksi (Mod & konten, Buka
 * folder), dan Hapus.
 *
 * Bentuknya halaman kaca layar penuh seperti layar Mod/Akun/Pengaturan, bukan kotak di tengah:
 * bar atas melayang berisi panah kembali, judul, dan tombol Simpan; di bawahnya dua kolom (kiri
 * isian, kanan aksi berbentuk pil). Tidak ada tombol di dasar layar, jadi tidak bisa tertutup bar
 * sistem atau taskbar tablet. Di layar sempit (HP portrait) menjadi satu kolom yang bisa digulir.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NuxEditInstanceDialog(
    instance: Instance,
    onDismiss: () -> Unit,
    onInstanceUpdated: (Instance) -> Unit,
    onInstanceDeleted: (Instance) -> Unit,
    onOpenContent: () -> Unit
) {
    val context = LocalContext.current
    var editName by remember { mutableStateOf(instance.name) }
    var selectedJavaRuntime by remember { mutableStateOf(instance.javaRuntime) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var modCount by remember { mutableStateOf<Int?>(null) }

    val autoRecommendedRuntime = remember(instance.mcVersion) {
        JavaRuntimeManager.getRecommendedRuntime(instance.mcVersion)
    }

    // Jumlah mod hanya dihitung dari nama file (cepat), tanpa membuka isi jar.
    LaunchedEffect(instance.id) {
        modCount = withContext(Dispatchers.IO) {
            val files = NuxModManager.getTargetDir(context, instance, "mods").listFiles()
            files?.count { f ->
                f.isFile && (f.name.endsWith(".jar", true) || f.name.endsWith(".jar.disabled", true))
            } ?: 0
        }
    }

    val runtimeOptions = listOf(
        "auto" to "Auto (${autoRecommendedRuntime.replace("jre-", "Java ")})",
        "jre-8" to "Java 8",
        "jre-17" to "Java 17",
        "jre-21" to "Java 21",
        "jre-25" to "Java 25"
    )

    val subtitle = "MC ${instance.mcVersion} · ${instance.loader.uppercase()}" +
        if (instance.loaderVersion.isNotBlank()) " · ${instance.loaderVersion}" else ""

    val save: () -> Unit = {
        val trimmed = editName.trim()
        if (trimmed.isBlank()) {
            Toast.makeText(context, "Nama instance tidak boleh kosong", Toast.LENGTH_SHORT).show()
        } else {
            val updated = instance.copy(
                name = trimmed,
                javaRuntime = selectedJavaRuntime
            )
            onInstanceUpdated(updated)
            Toast.makeText(context, "Instance berhasil diperbarui!", Toast.LENGTH_SHORT).show()
            onDismiss()
        }
    }

    // Isi kartu kiri: nama dan versi Java
    val identity: @Composable () -> Unit = {
        AmberPageLabel("Nama")
        AmberField(
            value = editName,
            onValueChange = { editName = it },
            placeholder = "Nama instance",
            icon = Icons.Outlined.Edit
        )
        Spacer(modifier = Modifier.height(16.dp))
        AmberPageLabel("Java")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            runtimeOptions.forEach { (key, label) ->
                AmberChoicePill(
                    text = label,
                    selected = selectedJavaRuntime == key,
                    onClick = { selectedJavaRuntime = key }
                )
            }
        }
    }

    // Kolom kanan: aksi berbentuk pil. Hapus dipisah paling bawah dan butuh konfirmasi.
    val actions: @Composable () -> Unit = {
        AmberActionPill(
            icon = Icons.Outlined.Extension,
            label = "Mod & konten",
            trailing = modCount?.let { "$it terpasang" },
            showChevron = true,
            onClick = onOpenContent
        )
        AmberActionPill(
            icon = Icons.Outlined.Folder,
            label = "Buka folder instance",
            showChevron = true,
            onClick = {
                Toast.makeText(context, "Membuka folder instance...", Toast.LENGTH_SHORT).show()
                InstanceManager.openInstanceFolder(context, instance)
            }
        )
        if (!showDeleteConfirm) {
            AmberActionPill(
                icon = Icons.Outlined.DeleteOutline,
                label = "Hapus instance",
                danger = true,
                onClick = { showDeleteConfirm = true }
            )
        } else {
            DeleteConfirmCard(
                name = instance.name,
                onCancel = { showDeleteConfirm = false },
                onConfirm = {
                    onInstanceDeleted(instance)
                    onDismiss()
                }
            )
        }
    }

    AmberPage(
        onDismiss = onDismiss,
        title = "Ubah instance",
        icon = Icons.Outlined.Tune,
        subtitle = subtitle,
        actions = {
            AmberPillButton(
                text = "Simpan",
                onClick = save,
                icon = Icons.Outlined.Check
            )
        }
    ) { info ->
        if (info.narrow) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EditCard { identity() }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { actions() }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    EditCard { identity() }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    actions()
                }
            }
        }
    }
}

/** Kartu kaca bersudut besar untuk satu kelompok isian. */
@Composable
private fun EditCard(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .amberGlass(shape)
            .padding(16.dp)
    ) {
        content()
    }
}

/** Konfirmasi hapus di tempat (menggantikan pil Hapus): pesan, lalu Batal dan Ya, hapus. */
@Composable
private fun DeleteConfirmCard(
    name: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    val shape = RoundedCornerShape(26.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AmberUi.Danger.copy(alpha = 0.10f), shape)
            .border(1.dp, AmberUi.Danger.copy(alpha = 0.55f), shape)
            .padding(14.dp)
    ) {
        Text(
            text = "Hapus '$name'?",
            color = AmberUi.Danger,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "Semua world, mod, dan file instance dihapus permanen.",
            color = AmberUi.TextDim,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AmberPillButton(
                text = "Batal",
                onClick = onCancel,
                filled = false,
                modifier = Modifier.weight(1f)
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(CircleShape)
                    .background(AmberUi.Danger, CircleShape)
                    .clickable { onConfirm() },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ya, hapus",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1
                )
            }
        }
    }
}

// ----------------------------------------------------------------------------
// Section: ui/dialogs/NuxInstalledModsDialog.kt
// ----------------------------------------------------------------------------

/**
 * "Lihat mod": tampilan bersih isi satu instance. Kotak-kotak kecil dengan gambar, nama, dan versi
 * tiap mod (dibaca dari filenya, tanpa internet). Hanya untuk dilihat; memasang dan menghapus tetap
 * lewat menu Mod di sidebar.
 */
@Composable
fun NuxInstalledModsDialog(
    instance: Instance,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var type by remember { mutableStateOf("mods") }
    var loaded by remember { mutableStateOf<Map<String, List<InstalledModItem>>>(emptyMap()) }

    LaunchedEffect(instance.id, type) {
        if (!loaded.containsKey(type)) {
            val result = NuxModManager.getInstalledItems(context, instance, type)
            loaded = loaded + (type to result)
        }
    }

    val tabs = listOf(
        "mods" to "Mod",
        "worlds" to "Dunia",
        "resourcepacks" to "Resource",
        "shaderpacks" to "Shader"
    )
    val shown = loaded[type]

    NuxDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = 700.dp),
        fillMaxHeight = true,
        compact = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            // Judul + tutup
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Mod terpasang",
                        color = NuxColors.DarkGray,
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        letterSpacing = (-0.2).sp
                    )
                    Text(
                        text = "${instance.name} · ${instance.mcVersion} · ${instance.loader.uppercase()}",
                        color = NuxColors.GrayNeutral,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                NuxIconButton(
                    icon = Icons.Outlined.Close,
                    description = "Tutup",
                    onClick = onDismiss
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Jenis: Mod / Resource / Shader
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                tabs.forEach { (id, label) ->
                    val count = loaded[id]?.size
                    SettingChip(
                        text = if (count != null) "$label $count" else label,
                        selected = type == id,
                        onClick = { type = id }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (shown == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        AmberSpinner(
                            color = NuxColors.Yellow,
                            modifier = Modifier.size(26.dp),
                            strokeWidth = 2.5.dp
                        )
                    }
                } else if (shown.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Extension,
                            contentDescription = null,
                            tint = NuxColors.GrayNeutral,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = when (type) {
                                "worlds" -> "Belum ada dunia tersimpan"
                                "resourcepacks" -> "Belum ada resource pack"
                                "shaderpacks" -> "Belum ada shader"
                                else -> "Belum ada mod terpasang"
                            },
                            color = NuxColors.DarkGray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Pasang lewat menu Mod di sidebar.",
                            color = NuxColors.GrayNeutral,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 190.dp),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        gridItems(shown, key = { it.originalFilename }) { item ->
                            InstalledModTile(item)
                        }
                    }
                }
            }
        }
    }
}

/** Satu kotak mod: gambar di kiri, nama di atas dan versi di bawahnya. */
@Composable
private fun InstalledModTile(item: InstalledModItem) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NuxColors.SurfaceWhite, shape)
            .border(NuxSizes.GlassBorder, NuxColors.CardBorder, shape)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ModIcon(
            item = item,
            size = 40.dp,
            dimmed = !item.isEnabled
        )
        Spacer(modifier = Modifier.size(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                color = if (item.isEnabled) NuxColors.DarkGray else NuxColors.GrayNeutral,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (item.isEnabled) item.version else "Nonaktif · ${item.version}",
                color = NuxColors.GrayNeutral,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ----------------------------------------------------------------------------
// Section: ui/dialogs/NuxInstancePickerDialog.kt
// ----------------------------------------------------------------------------

/**
 * Pemilih instance berbentuk rak kartu (seperti perpustakaan game), bukan daftar di dalam kotak.
 *
 * Tiap instance adalah kartu kaca: label loader, nama, versi, dan tiga tombol bulat (buka folder,
 * ubah, hapus). Yang aktif diberi isian aksen dan centang. Kartu putus-putus di ujung untuk menambah
 * instance, dan tombol + di bar atas supaya tetap terjangkau saat instance banyak.
 *
 * Rak hanya setinggi kartu dan digulir ke samping, jadi tinggi layar sekecil apa pun tidak memotong
 * isinya, dan tidak ada tombol di dasar layar yang bisa tertutup bar sistem. Ketuk area gelap di
 * luar rak untuk menutup.
 */
@Composable
fun NuxInstancePickerDialog(
    instances: List<Instance>,
    selectedId: String?,
    onSelect: (Instance) -> Unit,
    onOpenFolder: (Instance) -> Unit,
    onEdit: (Instance) -> Unit,
    onDelete: (Instance) -> Unit,
    onAdd: () -> Unit,
    onDismiss: () -> Unit
) {
    val listState = rememberLazyListState()
    // Buka dengan instance aktif sudah terlihat, bukan selalu dari kartu pertama.
    LaunchedEffect(Unit) {
        val index = instances.indexOfFirst { it.id == selectedId }
        if (index > 0) listState.scrollToItem(index)
    }

    AmberPage(
        onDismiss = onDismiss,
        title = "PILIH INSTANCE",
        icon = Icons.Outlined.Folder,
        dismissOnScrimTap = true,
        scrimAlpha = 0.74f,
        actions = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(AmberUi.Accent, CircleShape)
                    .border(1.dp, AmberUi.AccentEdge, CircleShape)
                    .clickable { onAdd() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "TAMBAH INSTANCE",
                    tint = AmberUi.Text,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { _ ->
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (instances.isEmpty()) {
                Column(
                    modifier = Modifier.pointerInput(Unit) { detectTapGestures { } },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Belum ada instance. Buat yang pertama di bawah.",
                        color = AmberUi.TextDim,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AddInstanceCard(onClick = onAdd)
                }
            } else {
                LazyRow(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        // Celah di antara kartu tidak ikut menutup dialog
                        .pointerInput(Unit) { detectTapGestures { } },
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                ) {
                    items(instances, key = { it.id }) { inst ->
                        InstanceShelfCard(
                            inst = inst,
                            selected = inst.id == selectedId,
                            onSelect = { onSelect(inst) },
                            onOpenFolder = { onOpenFolder(inst) },
                            onEdit = { onEdit(inst) },
                            onDelete = { onDelete(inst) }
                        )
                    }
                    item(key = "__add_card__") {
                        AddInstanceCard(onClick = onAdd)
                    }
                }
            }
        }
    }
}

private val ShelfCardShape = RoundedCornerShape(26.dp)

/** Satu kartu instance. Tinggi tetap supaya semua kartu rata walau nama panjang. */
@Composable
private fun InstanceShelfCard(
    inst: Instance,
    selected: Boolean,
    onSelect: () -> Unit,
    onOpenFolder: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(184.dp)
            .height(176.dp)
            .clip(ShelfCardShape)
            .then(
                if (selected) {
                    Modifier
                        .background(AmberUi.Accent.copy(alpha = 0.62f), ShelfCardShape)
                        .border(1.5.dp, AmberUi.AccentEdge, ShelfCardShape)
                } else {
                    Modifier.amberGlass(ShelfCardShape)
                }
            )
            .clickable { onSelect() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // Label loader (ruang di kanan disisakan untuk tanda centang)
                Box(
                    modifier = Modifier
                        .padding(end = 24.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .padding(horizontal = 9.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = ForgeFlavor.label(inst.loader).uppercase(),
                        color = AmberUi.AccentSoft,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = inst.name,
                    color = AmberUi.Text,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 21.sp,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = inst.mcVersion,
                    color = AmberUi.TextDim,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ShelfRoundButton(Icons.Outlined.Folder, "Buka folder", AmberUi.Text, onOpenFolder)
                ShelfRoundButton(Icons.Outlined.Tune, "Ubah instance", AmberUi.Text, onEdit)
                ShelfRoundButton(Icons.Outlined.DeleteOutline, "Hapus instance", AmberUi.Danger, onDelete)
            }
        }
        if (selected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = AmberUi.Text,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 12.dp)
                    .size(20.dp)
            )
        }
    }
}

/** Tombol bulat kecil di dasar kartu. */
@Composable
private fun ShelfRoundButton(
    icon: ImageVector,
    description: String,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.10f), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
    }
}

/** Kartu putus-putus di ujung rak untuk menambah instance. */
@Composable
private fun AddInstanceCard(onClick: () -> Unit) {
    val dash = AmberUi.BorderStrong
    Column(
        modifier = Modifier
            .width(150.dp)
            .height(176.dp)
            .clip(ShelfCardShape)
            .clickable { onClick() }
            .drawBehind {
                val stroke = 1.5.dp.toPx()
                drawRoundRect(
                    color = dash,
                    topLeft = Offset(stroke / 2f, stroke / 2f),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(26.dp.toPx()),
                    style = Stroke(
                        width = stroke,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f))
                    )
                )
            }
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(AmberUi.Accent, CircleShape)
                .border(1.dp, AmberUi.AccentEdge, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = AmberUi.Text,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "TAMBAH INSTANCE",
            color = AmberUi.Text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.4.sp,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

// ----------------------------------------------------------------------------
// Section: ui/dialogs/NuxKeyPickerDialog.kt
// ----------------------------------------------------------------------------

private val KEY_SHAPE = RoundedCornerShape(10.dp)
private val TAB_NUMBERS = arrayOf("①", "②", "③")

/** "W (Maju)" -> ("W", "Maju"); tanpa kurung -> (nama, ""). */
private fun splitLabel(name: String): Pair<String, String> {
    val open = name.indexOf(" (")
    if (open < 0 || !name.endsWith(")")) return name to ""
    val main = name.substring(0, open).trim()
    val sub = name.substring(open + 2, name.length - 1).trim()
    return main to sub
}

private fun List<List<KeyCap>>.hasCurrent(isCurrent: (KeyOption) -> Boolean): Boolean =
    any { row -> row.any { cap -> cap.option?.let(isCurrent) == true } }

/**
 * Pemilih tombol berbentuk keyboard. Halaman 1 = keyboard utama (Esc, F1-F12, angka, huruf,
 * modifier, Space); halaman 2 = navigasi, panah, dan numpad; halaman 3 = mouse dan scroll (tidak ada
 * di mode kombinasi). Tinggi tiap baris dan lebar tiap tombol ikut ukuran dialog, jadi muat di layar
 * pendek tanpa menggulir.
 *
 * Susunan tombol berupa data statis ([KeyboardLayout]) dan digambar dengan Row/Column biasa tanpa
 * daftar lazy atau state per tombol, jadi membuka dialog ringan dan tidak ada yang berubah saat idle.
 *
 * @param comboMode true = memilih tombol kedua untuk kombinasi (tanpa mouse/scroll).
 * @param isCurrent menandai pilihan yang sedang dipakai tombol; halamannya dibuka lebih dulu.
 */
@Composable
fun NuxKeyPickerDialog(
    comboMode: Boolean,
    onPick: (KeyOption) -> Unit,
    onDismiss: () -> Unit,
    isCurrent: (KeyOption) -> Boolean = { false }
) {
    val mouseKeys = remember(comboMode) {
        if (comboMode) emptyList() else KeycodeCatalog.ALL_KEYS.filter { it.isMouseButton || it.isScroll }
    }
    val tabTitles = remember(comboMode) {
        if (comboMode) listOf("Utama", "Navigasi & Numpad") else listOf("Utama", "Navigasi & Numpad", "Mouse")
    }
    var tab by remember {
        mutableIntStateOf(
            when {
                mouseKeys.any(isCurrent) -> 2
                KeyboardLayout.NAV.hasCurrent(isCurrent) -> 1
                else -> 0
            }
        )
    }

    NuxDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = 820.dp),
        fillMaxHeight = true,
        compact = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (comboMode) "Tombol kedua" else "Pilih tombol",
                        color = NuxColors.DarkGray,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        letterSpacing = (-0.2).sp
                    )
                    Text(
                        text = if (comboMode) "Ditekan bersamaan dengan tombol utama" else "Ketuk tombol yang diinginkan",
                        color = NuxColors.GrayNeutral,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                NuxIconButton(
                    icon = Icons.Outlined.Close,
                    description = "Tutup",
                    onClick = onDismiss
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                tabTitles.forEachIndexed { index, title ->
                    PickerTab(
                        number = TAB_NUMBERS[index],
                        title = title,
                        selected = tab == index,
                        onClick = { tab = index },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(NuxColors.CardBorder)
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            ) {
                when (tab) {
                    0 -> KeyboardPage(KeyboardLayout.MAIN, isCurrent, onPick)
                    1 -> KeyboardPage(KeyboardLayout.NAV, isCurrent, onPick)
                    else -> MousePage(mouseKeys, isCurrent, onPick)
                }
            }
        }
    }
}

@Composable
private fun PickerTab(
    number: String,
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ink = if (selected) NuxColors.Yellow else NuxColors.GrayNeutral
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 42.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Text(text = number, color = ink, fontSize = 15.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                color = ink,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Black else FontWeight.Bold,
                maxLines = 1
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(2.dp)
                .background(if (selected) NuxColors.Yellow else Color.Transparent)
        )
    }
}

/**
 * Satu halaman keyboard. Tiap baris memakai bobot yang sama untuk tinggi, tiap tombol memakai
 * bobot lebarnya ([KeyCap.w]), sehingga kolom antar baris selalu sejajar di ukuran layar apa pun.
 */
@Composable
private fun KeyboardPage(
    rows: List<List<KeyCap>>,
    isCurrent: (KeyOption) -> Boolean,
    onPick: (KeyOption) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isNarrowKeyboard = maxWidth < 520.dp
        val boardModifier = if (isNarrowKeyboard) {
            Modifier
                .width(560.dp)
                .fillMaxHeight()
        } else {
            Modifier.fillMaxSize()
        }
        val containerModifier = if (isNarrowKeyboard) {
            Modifier
                .fillMaxSize()
                .horizontalScroll(rememberScrollState())
        } else {
            Modifier.fillMaxSize()
        }

        Box(modifier = containerModifier) {
            Column(modifier = boardModifier) {
                for (row in rows) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        for (cap in row) {
                            val option = cap.option
                            if (option == null) {
                                Spacer(
                                    modifier = Modifier
                                        .weight(cap.w)
                                        .fillMaxHeight()
                                )
                            } else {
                                KeyCapTile(
                                    label = cap.label,
                                    current = isCurrent(option),
                                    onClick = { onPick(option) },
                                    modifier = Modifier
                                        .weight(cap.w)
                                        .fillMaxHeight()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyCapTile(
    label: String,
    current: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Jarak antar tombol dibuat dari padding di dalam bobot, bukan spacedBy, supaya lebar kolom
    // antar baris tetap persis sejajar.
    Box(modifier = modifier.padding(2.dp)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(KEY_SHAPE)
                .background(if (current) NuxColors.Yellow else NuxColors.SurfaceElevated)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            // Nama tombol fisik (Esc, Enter, Shift, ...) sengaja tidak diterjemahkan.
            androidx.compose.material3.Text(
                text = label,
                color = if (current) NuxColors.Ink else NuxColors.DarkGray,
                fontSize = if (label.length > 3) 11.sp else 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Mouse kiri/kanan/tengah, tombol 4-5, dan scroll: tiga kolom, nama di atas dan fungsinya di bawah. */
@Composable
private fun MousePage(
    keys: List<KeyOption>,
    isCurrent: (KeyOption) -> Boolean,
    onPick: (KeyOption) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val columns = if (maxWidth < 380.dp) 2 else 3
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (row in keys.chunked(columns)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (option in row) {
                        MouseTile(
                            option = option,
                            current = isCurrent(option),
                            onClick = { onPick(option) },
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 64.dp)
                        )
                    }
                    repeat(columns - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun MouseTile(
    option: KeyOption,
    current: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Diterjemahkan dulu baru dipecah, karena teks hanya diterjemahkan secara utuh.
    val (main, sub) = remember(option) { splitLabel(Tr.t(option.displayName)) }
    Column(
        modifier = modifier
            .clip(KEY_SHAPE)
            .background(if (current) NuxColors.Yellow else NuxColors.SurfaceElevated)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = main,
            color = if (current) NuxColors.Ink else NuxColors.DarkGray,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        if (sub.isNotEmpty()) {
            Text(
                text = sub,
                color = if (current) NuxColors.Ink.copy(alpha = 0.7f) else NuxColors.GrayNeutral,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ----------------------------------------------------------------------------
// Section: ui/dialogs/NuxMicrosoftAuthDialog.kt
// ----------------------------------------------------------------------------

private const val CHROME_PACKAGE = "com.android.chrome"

/**
 * Dialog login Microsoft / Xbox Live lewat browser perangkat.
 * Diperbarui agar selaras dengan Portal Login baru, adaptif tema Terang/Gelap, dan aman pada semua rasio layar.
 */
@Composable
fun NuxMicrosoftAuthDialog(
    deviceCode: MicrosoftDeviceCode,
    onDismiss: () -> Unit,
    onSuccess: (UserAccount) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isCancelled by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Menunggu otorisasi di browser...") }

    val linkUrl = remember(deviceCode.userCode) {
        "https://microsoft.com/link?otc=${Uri.encode(deviceCode.userCode)}"
    }
    val msBlue = NuxColors.SkyBlueDark

    val openInBrowser: () -> Unit = {
        fun tryStart(pkg: String?): Boolean = try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(linkUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (pkg != null) setPackage(pkg)
            }
            context.startActivity(intent)
            true
        } catch (_: Throwable) {
            false
        }
        if (!tryStart(CHROME_PACKAGE) && !tryStart(null)) {
            Toast.makeText(context, "Tidak ada browser yang bisa dibuka", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(deviceCode.userCode) {
        runCatching {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText("Microsoft User Code", deviceCode.userCode))
        }
        openInBrowser()
    }

    LaunchedEffect(deviceCode) {
        withContext(Dispatchers.IO) {
            val tokenResult = MicrosoftAuthService.pollForToken(
                deviceCode = deviceCode,
                isCancelled = { isCancelled },
                onStatusUpdate = { newStatus ->
                    scope.launch { statusText = newStatus }
                }
            )

            tokenResult.fold(
                onSuccess = { (msAccessToken, msRefreshToken) ->
                    scope.launch { statusText = "Token diterima! Memverifikasi profil Xbox & Minecraft..." }
                    val mcAuthResult = MicrosoftAuthService.completeMinecraftAuth(
                        msAccessToken = msAccessToken,
                        msRefreshToken = msRefreshToken,
                        onStatusUpdate = { stepStatus ->
                            scope.launch { statusText = stepStatus }
                        }
                    )

                    mcAuthResult.fold(
                        onSuccess = { account ->
                            LauncherLog.event("Login", "Login Microsoft berhasil")
                            withContext(Dispatchers.Main) {
                                onSuccess(account)
                            }
                        },
                        onFailure = { err ->
                            if (err !is CancellationException && !isCancelled) {
                                LauncherLog.w("Login", "Autentikasi Minecraft gagal: ${err.message}", err)
                                withContext(Dispatchers.Main) {
                                    onError(err.message ?: "Gagal memproses autentikasi Minecraft.")
                                }
                            }
                        }
                    )
                },
                onFailure = { err ->
                    if (err !is CancellationException && !isCancelled) {
                        LauncherLog.w("Login", "Otorisasi Microsoft gagal: ${err.message}", err)
                        withContext(Dispatchers.Main) {
                            onError(err.message ?: "Otorisasi Microsoft gagal.")
                        }
                    }
                }
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            isCancelled = true
        }
    }

    NuxDialog(
        onDismissRequest = {
            isCancelled = true
            onDismiss()
        },
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .widthIn(max = 460.dp)
            .wrapContentHeight(),
        fillMaxHeight = false,
        compact = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(msBlue.copy(alpha = 0.15f))
                            .border(1.dp, msBlue.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Language,
                            contentDescription = "Microsoft",
                            tint = msBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Verifikasi Microsoft",
                            color = AmberUi.Text,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.5.sp
                        )
                        Text(
                            text = "Selesaikan masuk di browser lalu kembali ke sini",
                            color = AmberUi.TextDim,
                            fontSize = 11.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable {
                            isCancelled = true
                            onDismiss()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AmberUi.SurfaceRaised)
                            .border(1.dp, AmberUi.Border, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Tutup",
                            tint = AmberUi.TextDim,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AmberUi.SurfaceRaised)
                    .border(1.dp, AmberUi.Border, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AmberSpinner(
                    modifier = Modifier.size(16.dp),
                    color = msBlue,
                    strokeWidth = 2.dp
                )
                Text(
                    text = statusText,
                    color = AmberUi.Text,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AmberUi.SurfaceRaised)
                    .border(1.5.dp, msBlue.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                    .clickable {
                        runCatching {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(ClipData.newPlainText("Microsoft User Code", deviceCode.userCode))
                        }
                        Toast.makeText(context, "Kode '${deviceCode.userCode}' disalin!", Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "KODE:",
                    color = AmberUi.TextDim,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = deviceCode.userCode,
                    color = msBlue,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp
                )
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = "Salin Kode",
                    tint = msBlue,
                    modifier = Modifier.size(16.dp)
                )
            }

            Text(
                text = "Kode sudah disalin ke papan klip. Bila kolom di browser kosong, tempel lalu lanjutkan.",
                color = AmberUi.TextDim,
                fontSize = 11.5.sp,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center
            )

            NuxButton(
                onClick = openInBrowser,
                backgroundColor = NuxColors.Yellow,
                cornerRadius = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Buka di Browser", fontWeight = FontWeight.Black, fontSize = 13.sp)
            }
        }
    }
}

// ----------------------------------------------------------------------------
// Section: ui/dialogs/NuxRendererV2ConfigDialog.kt
// ----------------------------------------------------------------------------

@Composable
fun NuxRendererV2ConfigDialog(
    rendererInfo: NuxRendererInfo?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val targetPkg = rendererInfo?.pluginPackageName ?: "top.mobilegl.plugin"
    val v2Data: RendererV2Data? = remember(targetPkg, rendererInfo) {
        NuxRendererV2Manager.getPluginData(targetPkg)
            ?: (rendererInfo?.id?.let { NuxRendererV2Manager.getPluginData(it) })
            ?: (rendererInfo?.displayName?.let { NuxRendererV2Manager.getPluginData(it) })
            ?: NuxRendererV2Manager.getPluginData("top.mobilegl.plugin")
            ?: NuxRendererV2Manager.getAllV2Plugins().firstOrNull()
    }

    val activePkg = v2Data?.packageName ?: rendererInfo?.pluginPackageName ?: "top.mobilegl.plugin"
    val isAppInstalled = remember(activePkg) {
        try {
            context.packageManager.getPackageInfo(activePkg, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    NuxWindowDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.82f))
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            NuxCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 580.dp)
                    .fillMaxHeight(0.94f),
                backgroundColor = NuxColors.Panel,
                borderColor = NuxColors.CardBorder,
                cornerRadius = NuxSizes.CornerRadiusLarge,
                fillMaxHeight = false
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NuxColors.YellowPale)
                                    .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = NuxColors.SageGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "KONFIGURASI RENDERER",
                                    color = NuxColors.DarkGray,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = v2Data?.config?.displayName ?: rendererInfo?.displayName ?: "MobileGlues Engine",
                                    color = NuxColors.SageGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Close button
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(NuxColors.SurfaceInput)
                                .border(NuxSizes.BorderWidth, NuxColors.CardBorder, RoundedCornerShape(8.dp))
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✕",
                                color = NuxColors.DarkGray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Scrollable Config Items Body
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Shortcut ke Aplikasi Plugin (jika terpasang)
                        if (isAppInstalled) {
                            val cardShape = RoundedCornerShape(10.dp)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(cardShape)
                                    .background(NuxColors.SurfaceElevated, cardShape)
                                    .border(1.dp, NuxColors.CardBorder, cardShape)
                                    .clickable {
                                        try {
                                            val launchIntent = context.packageManager.getLaunchIntentForPackage(activePkg)
                                            if (launchIntent != null) {
                                                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                context.startActivity(launchIntent)
                                            } else {
                                                Toast.makeText(context, "Tidak dapat membuka panel aplikasi", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(NuxColors.YellowPale),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = androidx.compose.material.icons.Icons.Default.OpenInNew,
                                                contentDescription = null,
                                                tint = NuxColors.SageGreen,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Buka Panel Aplikasi ${v2Data?.config?.displayName ?: rendererInfo?.displayName ?: "MobileGL"}",
                                                color = NuxColors.DarkGray,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = "Akses pengaturan driver native, benchmark, dan preferensi aplikasi",
                                                color = NuxColors.GrayNeutral,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Default.OpenInNew,
                                        contentDescription = null,
                                        tint = NuxColors.SageGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // Daftar Environment Variables Dinamis (Selectable / Toggleable / Customizable)
                        val units = v2Data?.units ?: emptyList()
                        if (units.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Tidak ada environment variable yang dapat dikonfigurasi untuk renderer ini.",
                                    color = NuxColors.GrayNeutral,
                                    fontSize = 11.sp
                                )
                            }
                        } else {
                            Text(
                                text = "PENGATURAN ENVIRONMENT VARIABLES",
                                color = NuxColors.GrayNeutral,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            units.forEach { unit ->
                                when (unit) {
                                    is EnvSettingUnit.Selectable -> {
                                        SelectableEnvItemCard(unit)
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                    is EnvSettingUnit.Toggleable -> {
                                        ToggleableEnvItemCard(unit)
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                    is EnvSettingUnit.Customizable -> {
                                        CustomizableEnvItemCard(unit)
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Sticky Footer Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tombol Reset ke Default
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    v2Data?.resetAll()
                                    Toast.makeText(context, "Pengaturan direset ke default", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.RestartAlt,
                                    contentDescription = null,
                                    tint = NuxColors.GrayNeutral,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "RESET DEFAULT",
                                    color = NuxColors.GrayNeutral,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        NuxButton(
                            onClick = onDismiss,
                            backgroundColor = NuxColors.ForestGreen,
                            contentColor = NuxColors.DarkGray,
                            cornerRadius = 8.dp,
                            modifier = Modifier.defaultMinSize(minHeight = 40.dp)
                        ) {
                            Text(
                                text = "SIMPAN & TUTUP",
                                color = NuxColors.Ink,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Komponen Kartu untuk Selectable Environment (Dropdown Picker)
 * Contoh: MOBILEGL_BACKEND_TYPE (DirectGLES, DirectVulkan)
 */
@Composable
private fun SelectableEnvItemCard(unit: EnvSettingUnit.Selectable) {
    var expanded by remember { mutableStateOf(false) }
    val cardShape = RoundedCornerShape(10.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(NuxColors.SurfaceWhite, cardShape)
            .border(1.dp, if (unit.isEnabled) NuxColors.Yellow else NuxColors.DarkGray.copy(alpha = 0.19f), cardShape)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { if (unit.isEnabled) expanded = !expanded }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Checkbox aktif/tidak
                Checkbox(
                    checked = unit.isEnabled,
                    onCheckedChange = { checked ->
                        unit.saveCheck(checked)
                        if (!checked) expanded = false
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = NuxColors.ForestGreen,
                        uncheckedColor = NuxColors.LightGray,
                        checkmarkColor = NuxColors.Ink
                    ),
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = unit.key,
                        color = if (unit.isEnabled) NuxColors.DarkGray else NuxColors.GrayNeutral,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    if (!unit.summary.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = unit.summary,
                            color = NuxColors.GrayNeutral,
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Nilai aktif: ",
                            color = NuxColors.GrayNeutral,
                            fontSize = 10.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(NuxColors.YellowPale)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = unit.state,
                                color = NuxColors.SageGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = if (unit.isEnabled) NuxColors.DarkGray else NuxColors.GrayNeutral,
                    modifier = Modifier
                        .size(22.dp)
                        .rotate(if (expanded) 180f else 0f)
                )
            }

            if (expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NuxColors.SurfaceInput)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    unit.values.forEach { option ->
                        val isSelected = unit.state == option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) NuxColors.YellowPale else Color.Transparent)
                                .clickable {
                                    unit.save(option)
                                    expanded = false
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = option,
                                color = if (isSelected) NuxColors.SageGreen else NuxColors.GrayNeutral,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = NuxColors.SageGreen,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Komponen Kartu untuk Toggleable Environment (Switch ON/OFF)
 * Contoh: MOBILEGL_DISABLE_TIMERQUERRY, MOBILEGL_MAGME_DISABLE_SUBGROUP
 */
@Composable
private fun ToggleableEnvItemCard(unit: EnvSettingUnit.Toggleable) {
    val cardShape = RoundedCornerShape(10.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(NuxColors.SurfaceWhite, cardShape)
            .border(1.dp, if (unit.isEnabled) NuxColors.Yellow else NuxColors.DarkGray.copy(alpha = 0.19f), cardShape)
            .clickable { unit.setToggle(!unit.isEnabled) }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = unit.key,
                    color = if (unit.isEnabled) NuxColors.DarkGray else NuxColors.GrayNeutral,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                if (!unit.summary.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = unit.summary,
                        color = NuxColors.GrayNeutral,
                        fontSize = 10.sp
                    )
                }
            }

            Switch(
                checked = unit.isEnabled,
                onCheckedChange = { checked -> unit.setToggle(checked) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = NuxColors.Ink,
                    checkedTrackColor = NuxColors.ForestGreen,
                    uncheckedThumbColor = NuxColors.LightGray,
                    uncheckedTrackColor = NuxColors.SurfaceElevated
                )
            )
        }
    }
}

/**
 * Komponen Kartu untuk Customizable Environment (Input Text)
 * Contoh: MOBILEGL_CUSTOM_FLAGS
 */
@Composable
private fun CustomizableEnvItemCard(unit: EnvSettingUnit.Customizable) {
    val cardShape = RoundedCornerShape(10.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(NuxColors.SurfaceWhite, cardShape)
            .border(1.dp, NuxColors.DarkGray.copy(alpha = 0.19f), cardShape)
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = unit.key,
                color = NuxColors.DarkGray,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            if (!unit.summary.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = unit.summary,
                    color = NuxColors.GrayNeutral,
                    fontSize = 10.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = unit.state,
                onValueChange = { unit.save(it) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = TextStyle(
                    color = NuxColors.DarkGray,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                ),
                placeholder = {
                    Text(text = "Masukkan nilai (opsional)", color = NuxColors.GrayNeutral, fontSize = 11.sp)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NuxColors.ForestGreen,
                    unfocusedBorderColor = NuxColors.CardBorder,
                    focusedContainerColor = NuxColors.SurfaceInput,
                    unfocusedContainerColor = NuxColors.SurfaceInput,
                    cursorColor = NuxColors.Yellow
                ),
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}

// ----------------------------------------------------------------------------
// Section: ui/dialogs/NuxWardrobeDialog.kt
// ----------------------------------------------------------------------------

@Composable
fun NuxWardrobeDialog(
    account: UserAccount,
    onDismissRequest: () -> Unit,
    onSaved: (UserAccount) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isMicrosoft = account.safeAccountType == "microsoft"
    val isElyBy = account.safeAccountType == "elyby"

    // 3D Engine Instance
    val playerSkin = remember { PlayerSkin(context) }
    var isWebViewReady by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            playerSkin.destroy()
        }
    }

    // Tab state: "skin" or "cape"
    var activeTab by remember { mutableStateOf("skin") }

    // Wardrobe editing state
    var selectedModel by remember { mutableStateOf(account.safeSkinModel) }
    var currentSkinFile by remember {
        mutableStateOf(account.getSkinFile())
    }
    var currentCapeFile by remember {
        mutableStateOf(account.getCapeFile())
    }
    var selectedPresetCapeId by remember { mutableStateOf<String?>(null) }
    var isAutoRotating by remember { mutableStateOf(true) }

    var activeAccount by remember { mutableStateOf(account) }

    // Microsoft cape list state
    var isLoadingMicrosoftCapes by remember { mutableStateOf(false) }
    var microsoftCapes by remember { mutableStateOf<List<MicrosoftCape>>(emptyList()) }
    var selectedMicrosoftCapeId by remember { mutableStateOf<String?>(null) }
    var isSkinFileChanged by remember { mutableStateOf(false) }
    var isCapeChanged by remember { mutableStateOf(false) }

    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var selectedAnim by remember { mutableStateOf("Walking") }

    // Load Microsoft Capes from Minecraft.net if Microsoft account
    LaunchedEffect(account.id) {
        if (isMicrosoft) {
            isLoadingMicrosoftCapes = true
            scope.launch {
                val profRes = MicrosoftWardrobeService.getProfile(context, activeAccount)
                profRes.onSuccess { (updatedAcc, prof) ->
                    activeAccount = updatedAcc
                    microsoftCapes = prof.capes
                    val activeCape = prof.capes.find { it.state.equals("ACTIVE", ignoreCase = true) }
                    selectedMicrosoftCapeId = activeCape?.id
                    if (activeCape != null) {
                        playerSkin.loadCapeUrl(activeCape.url)
                    }
                }
                profRes.onFailure { err ->
                    statusMessage = "Gagal memuat jubah: ${err.message}"
                }
                isLoadingMicrosoftCapes = false
            }
        }
    }

    val persistAccountChanges: (File?, File?, String) -> UserAccount = { skinFile, capeFile, model ->
        val updated = activeAccount.copy(
            skinModel = model,
            accessToken = activeAccount.safeAccessToken,
            refreshToken = activeAccount.safeRefreshToken,
            email = activeAccount.safeEmail,
            tier = activeAccount.safeTier,
            customSkinPath = skinFile?.absolutePath,
            customCapePath = capeFile?.absolutePath
        )
        SkinUtils.invalidateHeadCache(activeAccount.id)
        AccountManager.updateAccount(context, updated)
        AccountManager.selectAccount(updated)
        onSaved(updated)
        updated
    }

    // Pickers (Preview only - do not persist immediately)
    val skinPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isProcessing = true
                statusMessage = "Memvalidasi skin..."
                SkinUtils.importSkinFromUri(context, account.id, uri).fold(
                    onSuccess = { file ->
                        currentSkinFile = file
                        isSkinFileChanged = true
                        val isSlim = SkinUtils.isSlimModel(file)
                        selectedModel = if (isSlim) "slim" else "classic"
                        playerSkin.loadAccount(account, customSkinFile = file, customCapeFile = currentCapeFile)
                        playerSkin.loadSkin(file, selectedModel)
                        statusMessage = "Skin dimuat di preview (${if (isSlim) "Slim" else "Klasik"}) — Klik Simpan untuk menerapkan"
                    },
                    onFailure = { err ->
                        statusMessage = "Gagal: ${err.message}"
                        Toast.makeText(context, err.message ?: "File tidak valid", Toast.LENGTH_SHORT).show()
                    }
                )
                isProcessing = false
            }
        }
    }

    val capePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isProcessing = true
                statusMessage = "Memvalidasi cape..."
                SkinUtils.importCapeFromUri(context, account.id, uri).fold(
                    onSuccess = { file ->
                        currentCapeFile = file
                        selectedPresetCapeId = null
                        playerSkin.loadAccount(account, customSkinFile = currentSkinFile, customCapeFile = file)
                        playerSkin.loadCape(file)
                        statusMessage = "Cape kustom dimuat di preview — Klik Simpan untuk menerapkan"
                    },
                    onFailure = { err ->
                        statusMessage = "Gagal: ${err.message}"
                        Toast.makeText(context, err.message ?: "File cape tidak valid", Toast.LENGTH_SHORT).show()
                    }
                )
                isProcessing = false
            }
        }
    }

    NuxDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.widthIn(max = 920.dp),
        fillMaxHeight = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "WARDROBE & CUSTOM SKIN",
                            color = NuxColors.DarkGray,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.5.sp,
                            letterSpacing = 0.6.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(NuxColors.YellowPale, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = account.username,
                                color = NuxColors.SageGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Kustomisasi tampilan skin dan jubah pemain Minecraft",
                        color = NuxColors.GrayNeutral,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(NuxColors.SurfaceInput, CircleShape)
                        .border(NuxSizes.BorderWidth, NuxColors.CardBorder, CircleShape)
                        .clickable {
                            onDismissRequest()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Tutup",
                        tint = NuxColors.GrayNeutral,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val viewerPane: @Composable (Modifier) -> Unit = { viewerModifier ->
                // 3D SKIN VIEWER (Interactive)
                Box(
                    modifier = viewerModifier
                        .background(NuxColors.SurfaceInput, RoundedCornerShape(14.dp))
                        .border(NuxSizes.BorderWidth, NuxColors.CardBorder, RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp))
                ) {
                    AndroidView(
                        factory = { ctx ->
                            playerSkin.loadWebView(ctx) {
                                isWebViewReady = true
                                playerSkin.loadAccount(account, currentSkinFile, currentCapeFile)
                                playerSkin.startAnim(selectedAnim, 0.8f)
                                playerSkin.setAutoRotate(isAutoRotating, 0.7f)
                                playerSkin.setCamera(0, 10, 52)
                            }
                        },
                        update = {
                            if (isWebViewReady) {
                                playerSkin.loadAccount(account, currentSkinFile, currentCapeFile)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Top Quick Controls (Orbit Info & Camera/Rotate controls like Windows)
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "3D (DRAG / PUTAR)",
                                color = NuxColors.SageGreen,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Auto-rotate toggle
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .background(
                                        if (isAutoRotating) NuxColors.ForestGreen else Color.Black.copy(alpha = 0.65f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        isAutoRotating = !isAutoRotating
                                        playerSkin.setAutoRotate(isAutoRotating, 0.7f)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.RotateRight,
                                    contentDescription = "Putar Otomatis",
                                    tint = if (isAutoRotating) NuxColors.Ink else NuxColors.DarkGray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Recenter Camera
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                                    .clickable {
                                        playerSkin.resetCamera()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CenterFocusStrong,
                                    contentDescription = "Reset Kamera",
                                    tint = NuxColors.DarkGray,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    // Animation Switcher bar (Walking, Idle, Running, Wave)
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Walking" to "Jalan", "DefaultIdle" to "Diam", "Running" to "Lari", "Wave" to "Sapa").forEach { (animKey, label) ->
                            val isSelected = selectedAnim == animKey
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isSelected) NuxColors.ForestGreen else Color.Transparent,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        selectedAnim = animKey
                                        playerSkin.startAnim(animKey, 0.8f)
                                    }
                                    .padding(horizontal = 9.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) NuxColors.Ink else NuxColors.GrayNeutral,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            val controlsPane: @Composable (Modifier) -> Unit = { controlsModifier ->
                // TABS & CONTROLS
                Column(
                    modifier = controlsModifier,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Scrollable Controls Column
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Section Tabs: Skin vs Cape
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NuxColors.SurfaceInput, RoundedCornerShape(8.dp))
                                .padding(2.5.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Tab Skin
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (activeTab == "skin") NuxColors.ForestGreen else Color.Transparent,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { activeTab = "skin" }
                                    .padding(vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Person,
                                        contentDescription = null,
                                        tint = if (activeTab == "skin") NuxColors.Ink else NuxColors.GrayNeutral,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "KUSTOM SKIN",
                                        color = NuxColors.DarkGray,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            // Tab Cape
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (activeTab == "cape") Color(0xFFA855F7) else Color.Transparent,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { activeTab = "cape" }
                                    .padding(vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Shield,
                                        contentDescription = null,
                                        tint = if (activeTab == "cape") NuxColors.DarkGray else NuxColors.GrayNeutral,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "JUBAH / CAPE",
                                        color = NuxColors.DarkGray,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }

                        // TAB CONTENT: SKIN
                        if (activeTab == "skin") {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (isElyBy) {
                                    // PANDUAN SKIN AKUN ELY.BY
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF8E24AA).copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                            .border(1.dp, Color(0xFF8E24AA).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                            .padding(10.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                text = "Sistem Skin & Jubah Ely.by",
                                                color = NuxColors.PurpleDark,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            )
                                            Text(
                                                text = "Akun Ely.by menggunakan server skinsystem terpusat. Untuk mengganti skin atau tipe model lengan, silakan unggah langsung melalui website resmi ely.by agar tersinkronisasi in-game.",
                                                color = NuxColors.DarkGray,
                                                fontSize = 8.5.sp,
                                                lineHeight = 12.sp
                                            )
                                            NuxButton(
                                                onClick = {
                                                    try {
                                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse("https://account.ely.by"))
                                                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                        context.startActivity(intent)
                                                    } catch (_: Exception) {}
                                                },
                                                backgroundColor = Color(0xFF8E24AA),
                                                contentColor = NuxColors.DarkGray,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .defaultMinSize(minHeight = 36.dp)
                                            ) {
                                                Text("KUNJUNGI ACCOUNT.ELY.BY", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                } else {
                                    // Upload Button
                                    NuxButton(
                                        onClick = { skinPickerLauncher.launch("image/png") },
                                        backgroundColor = NuxColors.YellowPale,
                                        contentColor = NuxColors.SageGreen,
                                        cornerRadius = NuxSizes.CornerRadiusSmall,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .defaultMinSize(minHeight = 38.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.UploadFile,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "PILIH FILE SKIN (.PNG)",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp
                                        )
                                    }

                                // Arm Model Selection (Steve vs Alex)
                                Text(
                                    text = "TIPE MODEL LENGAN:",
                                    color = NuxColors.GrayNeutral,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Classic (Klasik - 4px)
                                    val isClassic = selectedModel == "classic"
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                if (isClassic) NuxColors.YellowPale else NuxColors.SurfaceInput,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .border(
                                                1.dp,
                                                if (isClassic) NuxColors.ForestGreen else NuxColors.CardBorder,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                selectedModel = "classic"
                                                playerSkin.loadSkin(currentSkinFile, "classic")
                                                statusMessage = "Model Klasik dipilih (Preview) — Klik Simpan"
                                            }
                                            .padding(vertical = 6.dp, horizontal = 8.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = "Klasik",
                                                color = if (isClassic) NuxColors.SageGreen else NuxColors.DarkGray,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Lengan tebal (4px)",
                                                color = NuxColors.GrayNeutral,
                                                fontSize = 8.sp
                                            )
                                        }
                                    }

                                    // Slim (Slim - 3px)
                                    val isSlim = selectedModel == "slim"
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                if (isSlim) NuxColors.YellowPale else NuxColors.SurfaceInput,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .border(
                                                1.dp,
                                                if (isSlim) NuxColors.ForestGreen else NuxColors.CardBorder,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                selectedModel = "slim"
                                                playerSkin.loadSkin(currentSkinFile, "slim")
                                                statusMessage = "Model Slim dipilih (Preview) — Klik Simpan"
                                            }
                                            .padding(vertical = 6.dp, horizontal = 8.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = "Slim",
                                                color = if (isSlim) NuxColors.SageGreen else NuxColors.DarkGray,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Lengan ramping (3px)",
                                                color = NuxColors.GrayNeutral,
                                                fontSize = 8.sp
                                            )
                                        }
                                    }
                                }

                                    // Reset Skin Button
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                currentSkinFile = null
                                                selectedModel = "classic"
                                                playerSkin.resetSkin()
                                                playerSkin.loadCape(currentCapeFile)
                                                statusMessage = "Skin direset ke default Steve (Preview) — Klik Simpan"
                                            }
                                            .padding(vertical = 3.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "↺ Reset ke Skin Default (Klasik)",
                                            color = NuxColors.GrayNeutral,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        } else {
                            // TAB CONTENT: CAPE
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                if (isMicrosoft) {
                                    // KHUSUS AKUN MICROSOFT: Hanya tampilkan jubah resmi milik pengguna di minecraft.net
                                    Text(
                                        text = "JUBAH RESMI MINECRAFT.NET:",
                                        color = NuxColors.GrayNeutral,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    if (isLoadingMicrosoftCapes) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 12.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            AmberSpinner(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = NuxColors.PurpleDark
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Memuat jubah dari Minecraft.net...",
                                                color = NuxColors.GrayNeutral,
                                                fontSize = 9.5.sp
                                            )
                                        }
                                    } else if (microsoftCapes.isEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(NuxColors.SurfaceInput, RoundedCornerShape(8.dp))
                                                .border(NuxSizes.BorderWidth, NuxColors.CardBorder, RoundedCornerShape(8.dp))
                                                .padding(10.dp)
                                        ) {
                                            Text(
                                                text = "Akun Microsoft Anda belum memiliki jubah resmi di Minecraft.net.",
                                                color = NuxColors.GrayNeutral,
                                                fontSize = 9.sp
                                            )
                                        }
                                    } else {
                                        // Opsi Lepas Cape (Tanpa Jubah)
                                        val isNoCapeSelected = selectedMicrosoftCapeId == null
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    if (isNoCapeSelected) Color(0xFFA855F7).copy(alpha = 0.18f) else NuxColors.SurfaceInput,
                                                    RoundedCornerShape(7.dp)
                                                )
                                                .border(
                                                    1.dp,
                                                    if (isNoCapeSelected) Color(0xFFA855F7) else NuxColors.CardBorder,
                                                    RoundedCornerShape(7.dp)
                                                )
                                                .clickable {
                                                    selectedMicrosoftCapeId = null
                                                    isCapeChanged = true
                                                    playerSkin.loadCapeUrl(null)
                                                    statusMessage = "Jubah dilepas (Preview) — Klik Simpan"
                                                }
                                                .padding(horizontal = 9.dp, vertical = 6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Tanpa Jubah (Lepas Cape)",
                                                    color = if (isNoCapeSelected) NuxColors.PurpleDark else NuxColors.DarkGray,
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (isNoCapeSelected) {
                                                    Text("DIPILIH", color = NuxColors.PurpleDark, fontSize = 8.sp, fontWeight = FontWeight.Black)
                                                }
                                            }
                                        }

                                        // Daftar Jubah Resmi Milik User dari minecraft.net
                                        microsoftCapes.forEach { cape ->
                                            val isSelected = selectedMicrosoftCapeId == cape.id
                                            val isMojangActive = cape.state.equals("ACTIVE", ignoreCase = true)

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(
                                                        if (isSelected) Color(0xFFA855F7).copy(alpha = 0.18f) else NuxColors.SurfaceInput,
                                                        RoundedCornerShape(7.dp)
                                                    )
                                                    .border(
                                                        1.dp,
                                                        if (isSelected) Color(0xFFA855F7) else NuxColors.CardBorder,
                                                        RoundedCornerShape(7.dp)
                                                    )
                                                    .clickable {
                                                        selectedMicrosoftCapeId = cape.id
                                                        isCapeChanged = true
                                                        playerSkin.loadCapeUrl(cape.url)
                                                        statusMessage = "Jubah ${cape.alias ?: "Mojang"} dipilih (Preview) — Klik Simpan"
                                                    }
                                                    .padding(horizontal = 9.dp, vertical = 6.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(8.dp)
                                                                .background(Color(0xFFA855F7), CircleShape)
                                                        )
                                                        Text(
                                                            text = MicrosoftWardrobeService.getCapeDisplayName(cape.alias),
                                                            color = if (isSelected) NuxColors.PurpleDark else NuxColors.DarkGray,
                                                            fontSize = 9.5.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }

                                                    if (isMojangActive) {
                                                        Box(
                                                            modifier = Modifier
                                                                .background(NuxColors.YellowPale, RoundedCornerShape(4.dp))
                                                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = "AKTIF DI MOJANG",
                                                                color = NuxColors.SageGreen,
                                                                fontSize = 7.5.sp,
                                                                fontWeight = FontWeight.Black
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else if (isElyBy) {
                                    // PANDUAN AKUN ELY.BY
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF8E24AA).copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                            .border(1.dp, Color(0xFF8E24AA).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                            .padding(10.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                text = "Sistem Skin & Cape Ely.by",
                                                color = NuxColors.PurpleDark,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            )
                                            Text(
                                                text = "Akun Ely.by menggunakan server skinsystem resmi. Seluruh skin dan jubah dikelola langsung melalui website ely.by dan akan otomatis tampil di dalam game.",
                                                color = NuxColors.DarkGray,
                                                fontSize = 8.5.sp,
                                                lineHeight = 12.sp
                                            )
                                            NuxButton(
                                                onClick = {
                                                    try {
                                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse("https://account.ely.by"))
                                                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                        context.startActivity(intent)
                                                    } catch (_: Exception) {}
                                                },
                                                backgroundColor = Color(0xFF8E24AA),
                                                contentColor = NuxColors.DarkGray,
                                                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 36.dp)
                                            ) {
                                                Text("BUKA WEBSITE ELY.BY", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                } else {
                                    // KHUSUS AKUN OFFLINE: Upload custom cape atau pilih preset
                                    NuxButton(
                                        onClick = { capePickerLauncher.launch("image/png") },
                                        backgroundColor = Color(0xFFA855F7).copy(alpha = 0.15f),
                                        contentColor = NuxColors.PurpleDark,
                                        cornerRadius = NuxSizes.CornerRadiusSmall,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .defaultMinSize(minHeight = 38.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.UploadFile,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "PILIH FILE CAPE KUSTOM (.PNG)",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Text(
                                        text = "ATAU PILIH PRESET JUBAH:",
                                        color = NuxColors.GrayNeutral,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    // Preset Capes Grid
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        SkinUtils.PRESET_CAPES.chunked(2).forEach { pair ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                pair.forEach { preset ->
                                                    val isPresetSelected = selectedPresetCapeId == preset.id || (preset.id == "none" && currentCapeFile == null)
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .background(
                                                                if (isPresetSelected) Color(preset.badgeColor).copy(alpha = 0.18f) else NuxColors.SurfaceInput,
                                                                RoundedCornerShape(7.dp)
                                                            )
                                                            .border(
                                                                1.dp,
                                                                if (isPresetSelected) Color(preset.badgeColor) else NuxColors.CardBorder,
                                                                RoundedCornerShape(7.dp)
                                                            )
                                                            .clickable {
                                                                selectedPresetCapeId = preset.id
                                                                scope.launch {
                                                                    val generatedCape = SkinUtils.applyPresetCape(context, account.id, preset.id)
                                                                    currentCapeFile = generatedCape
                                                                    playerSkin.loadCape(generatedCape)
                                                                    statusMessage = if (preset.id == "none") "Cape dilepas (Preview) — Klik Simpan" else "Jubah ${preset.name} dipilih (Preview) — Klik Simpan"
                                                                }
                                                            }
                                                            .padding(horizontal = 7.dp, vertical = 5.dp)
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(7.dp)
                                                                    .background(Color(preset.badgeColor), CircleShape)
                                                            )
                                                            Text(
                                                                text = preset.name,
                                                                color = if (isPresetSelected) Color(preset.badgeColor) else NuxColors.DarkGray,
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                maxLines = 1
                                                            )
                                                        }
                                                    }
                                                }
                                                if (pair.size == 1) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Bottom Action Bar (PINNED & ALWAYS VISIBLE)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        statusMessage?.let { msg ->
                            Text(
                                text = msg,
                                color = NuxColors.SageGreen,
                                fontSize = 9.sp,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            NuxButton(
                                onClick = onDismissRequest,
                                backgroundColor = NuxColors.SurfaceElevated,
                                borderColor = NuxColors.DarkGray.copy(alpha = 0.40f),
                                contentColor = NuxColors.DarkGray,
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 38.dp)
                            ) {
                                Text("BATAL", color = NuxColors.DarkGray, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            if (isElyBy) {
                                NuxButton(
                                    onClick = onDismissRequest,
                                    backgroundColor = Color(0xFF8E24AA),
                                    contentColor = NuxColors.DarkGray,
                                    modifier = Modifier
                                        .weight(1.5f)
                                        .defaultMinSize(minHeight = 38.dp)
                                ) {
                                    Text("MENGERTI", fontWeight = FontWeight.Black, fontSize = 11.sp)
                                }
                            } else {
                                NuxButton(
                                    onClick = {
                                        if (isMicrosoft) {
                                            scope.launch {
                                                isProcessing = true
                                                statusMessage = "Menyinkronkan ke Minecraft.net..."
                                                var errorMsg: String? = null

                                                val skinToUpload = currentSkinFile
                                                if (isSkinFileChanged && skinToUpload != null) {
                                                    val res = MicrosoftWardrobeService.uploadSkin(context, activeAccount, skinToUpload, selectedModel == "slim")
                                                    if (res.isFailure) {
                                                        errorMsg = res.exceptionOrNull()?.message
                                                    } else {
                                                        activeAccount = res.getOrThrow()
                                                    }
                                                }

                                                if (isCapeChanged) {
                                                    val res = MicrosoftWardrobeService.changeCape(context, activeAccount, selectedMicrosoftCapeId)
                                                    if (res.isFailure) {
                                                        val capeErr = res.exceptionOrNull()?.message
                                                        errorMsg = if (errorMsg != null) "$errorMsg | $capeErr" else capeErr
                                                    } else {
                                                        activeAccount = res.getOrThrow()
                                                        SkinUtils.rememberMojangCape(
                                                            activeAccount.id,
                                                            microsoftCapes.firstOrNull { it.id == selectedMicrosoftCapeId }
                                                                ?.takeIf { it.id != "mojang_active_cape" }
                                                                ?.url
                                                        )
                                                    }
                                                }

                                                isProcessing = false
                                                if (errorMsg != null) {
                                                    Toast.makeText(context, "Gagal memperbarui Minecraft.net: $errorMsg", Toast.LENGTH_LONG).show()
                                                    statusMessage = "Gagal: $errorMsg"
                                                } else {
                                                    persistAccountChanges(currentSkinFile, currentCapeFile, selectedModel)
                                                    Toast.makeText(context, "Perubahan berhasil diterapkan ke Minecraft.net!", Toast.LENGTH_SHORT).show()
                                                    onDismissRequest()
                                                }
                                            }
                                        } else {
                                            persistAccountChanges(currentSkinFile, currentCapeFile, selectedModel)
                                            Toast.makeText(context, "Skin & Cape berhasil disimpan!", Toast.LENGTH_SHORT).show()
                                            onDismissRequest()
                                        }
                                    },
                                    backgroundColor = NuxColors.ForestGreen,
                                    contentColor = NuxColors.DarkGray,
                                    enabled = !isProcessing,
                                    modifier = Modifier
                                        .weight(1.5f)
                                        .defaultMinSize(minHeight = 38.dp)
                                ) {
                                    if (isProcessing) {
                                        AmberSpinner(modifier = Modifier.size(13.dp), color = NuxColors.Ink, strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(5.dp))
                                    } else {
                                        Icon(
                                            imageVector = Icons.Outlined.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                    }
                                    Text(
                                        text = if (isMicrosoft) "SIMPAN KE MOJANG" else "SIMPAN PERUBAHAN",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        letterSpacing = 0.4.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (maxWidth < 560.dp) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        viewerPane(
                            Modifier
                                .fillMaxWidth()
                                .weight(0.42f)
                        )
                        controlsPane(
                            Modifier
                                .fillMaxWidth()
                                .weight(0.58f)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        viewerPane(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        controlsPane(
                            Modifier
                                .weight(1.3f)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}
