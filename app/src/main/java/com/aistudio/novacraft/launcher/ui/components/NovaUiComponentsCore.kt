package com.aistudio.novacraft.launcher.ui.components

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.SurfaceTexture
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.util.Base64
import android.util.Log
import android.util.LruCache
import android.view.Gravity
import android.view.InputDevice
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AdsClick
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Gamepad
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Mouse
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.ViewCarousel
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text as RawM3Text
import androidx.compose.material3.Text as RawText
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.webkit.WebViewAssetLoader
import com.aistudio.novacraft.launcher.FpsMode
import com.aistudio.novacraft.launcher.R
import com.aistudio.novacraft.launcher.core.account.SkinUtils
import com.aistudio.novacraft.launcher.core.controls.ControlLayoutManager
import com.aistudio.novacraft.launcher.core.controls.ControlShowMode
import com.aistudio.novacraft.launcher.core.controls.CustomControlButton
import com.aistudio.novacraft.launcher.core.controls.GamepadNav
import com.aistudio.novacraft.launcher.core.controls.JoystickLogic
import com.aistudio.novacraft.launcher.core.controls.TouchForwarder
import com.aistudio.novacraft.launcher.core.controls.TouchSink
import com.aistudio.novacraft.launcher.core.utils.CrashAdvisor
import com.aistudio.novacraft.launcher.core.utils.CrashRecovery
import com.aistudio.novacraft.launcher.core.utils.CrashSource
import com.aistudio.novacraft.launcher.core.utils.CrashUtils
import com.aistudio.novacraft.launcher.core.utils.GameCrashInfo
import com.aistudio.novacraft.launcher.core.utils.GameLogArchive
import com.aistudio.novacraft.launcher.core.utils.LauncherLog
import com.aistudio.novacraft.launcher.core.utils.RecoveryAction
import com.aistudio.novacraft.launcher.core.utils.RecoveryOption
import com.aistudio.novacraft.launcher.core.mods.HorizonEngine
import com.aistudio.novacraft.launcher.core.mods.ModIconLoader
import com.aistudio.novacraft.launcher.core.mods.RustPerformanceReport
import com.aistudio.novacraft.launcher.core.mods.RustSettingsStore
import com.aistudio.novacraft.launcher.core.mods.RustTheme
import com.aistudio.novacraft.launcher.core.launch.UpdateChecker
import com.aistudio.novacraft.launcher.core.utils.DeviceProfiles
import com.aistudio.novacraft.launcher.core.utils.InstalledModItem
import com.aistudio.novacraft.launcher.core.utils.LauncherSettings
import com.aistudio.novacraft.launcher.core.utils.PhysicalMouseChecker
import com.aistudio.novacraft.launcher.core.utils.PowerSaver
import com.aistudio.novacraft.launcher.core.utils.SettingsManager
import com.aistudio.novacraft.launcher.ui.screens.AmberCard
import com.aistudio.novacraft.launcher.ui.screens.AmberPillButton
import com.aistudio.novacraft.launcher.ui.screens.AmberRoundIcon
import com.aistudio.novacraft.launcher.ui.screens.AmberSectionLabel
import com.aistudio.novacraft.launcher.ui.theme.Amber
import com.aistudio.novacraft.launcher.ui.theme.AmberUi
import com.aistudio.novacraft.launcher.ui.theme.LocalGlassBackdrop
import com.aistudio.novacraft.launcher.ui.theme.LocalNuxScale
import com.aistudio.novacraft.launcher.ui.theme.LocalNuxScaled
import com.aistudio.novacraft.launcher.ui.theme.NuxColors
import com.aistudio.novacraft.launcher.ui.theme.NuxReadable
import com.aistudio.novacraft.launcher.ui.theme.NuxSizes
import com.aistudio.novacraft.launcher.ui.theme.Text
import com.aistudio.novacraft.launcher.ui.theme.Toast
import com.aistudio.novacraft.launcher.ui.theme.Tr
import com.aistudio.novacraft.launcher.ui.theme.amberGlass
import com.aistudio.novacraft.launcher.ui.theme.resp
import com.movtery.inputmap.keycodes.LwjglGlfwKeycode
import com.movtery.zalithlauncher.bridge.CURSOR_DISABLED
import com.movtery.zalithlauncher.bridge.CURSOR_ENABLED
import com.movtery.zalithlauncher.bridge.CursorShape
import com.movtery.zalithlauncher.bridge.ZLBridgeStates
import com.movtery.zalithlauncher.game.sdl.SdlBridge
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.lwjgl.glfw.CallbackBridge

// ============================================================================
// Section: NovaUiComponentsCore.kt
// ============================================================================

// ----------------------------------------------------------------------------
// Section: ui/components/AmberCrashPanel.kt
// ----------------------------------------------------------------------------

private const val INTRO_GAME =
    "Crash tidak selalu berarti launcher rusak. Mod, batas RAM HP, dan driver grafis jauh lebih sering menjadi penyebabnya—periksa diagnosis di bawah."

private const val INTRO_LAUNCHER =
    "NovaCraft berhenti mendadak karena kesalahan tak terduga. Salin atau bagikan file log untuk memeriksa detail kendala."

private const val RECOVERY_BODY =
    "Perbaikan sekali ketuk berdasarkan pola log crash. Semua perubahan aman dan bisa dibatalkan."

private const val RECOVERY_DONE =
    "Perubahan disimpan. Kembali ke NovaCraft lalu jalankan instance lagi."

private const val MAX_SHOWN_LOG_CHARS = 40_000

/**
 * Pusat Diagnostik & Pemulihan NovaCraft.
 * Dipakai bersama oleh [com.aistudio.novacraft.launcher.ui.dialogs.NuxCrashDialog] di Beranda
 * dan [com.aistudio.novacraft.launcher.ErrorActivity].
 */
@Composable
fun AmberCrashPanel(
    info: GameCrashInfo,
    logText: String,
    logFile: File,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onBackToLauncher: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcherCrash = info.isLauncherCrash
    val source = remember(info) { CrashAdvisor.sourceOf(info) }
    val log = remember(logText) {
        if (logText.isBlank()) Tr.t("Tidak ada rincian log yang tersedia.") else logText
    }

    var recovery by remember(info) { mutableStateOf<List<RecoveryOption>>(emptyList()) }
    var recovered by remember(info) { mutableStateOf<Set<RecoveryAction>>(emptySet()) }
    LaunchedEffect(info, log) {
        recovery = withContext(Dispatchers.IO) { CrashRecovery.suggest(context, info, log) }
    }
    val inFlight = remember(info) { HashSet<RecoveryAction>() }
    val applyRecovery: (RecoveryOption) -> Unit = { option ->
        if (option.action !in recovered && inFlight.add(option.action)) {
            scope.launch {
                val result = withContext(Dispatchers.IO) { CrashRecovery.apply(context, info, option) }
                if (result.ok) recovered = recovered + option.action
                inFlight.remove(option.action)
                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    val copyLog: () -> Unit = {
        copyToClipboard(context, "Crash Log", CrashAdvisor.logForSharing(log))
        Toast.makeText(context, "Log disalin ke papan klip.", Toast.LENGTH_SHORT).show()
    }
    val shareFile: () -> Unit = { CrashUtils.shareLogFile(context, logFile) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        DiagnosticBanner(
            title = if (launcherCrash) "NovaCraft berhenti" else "Minecraft berhenti mendadak",
            subtitle = if (launcherCrash) {
                "Aplikasi tertutup karena kesalahan tak terduga"
            } else {
                "${info.instanceName} · Minecraft ${info.mcVersion}"
            },
            badgeText = if (launcherCrash) "CRASH APP" else "EXIT ${info.exitCode}",
            onBackToLauncher = onBackToLauncher,
            onClose = onClose
        )
        Spacer(modifier = Modifier.height(12.dp))
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val isNarrow = maxWidth < 600.dp
            if (isNarrow) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CrashExplainPane(
                        info = info,
                        source = source,
                        logText = log,
                        logFile = logFile,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.54f)
                    )
                    CrashHelpPane(
                        launcherCrash = launcherCrash,
                        recovery = recovery,
                        recovered = recovered,
                        onRecover = applyRecovery,
                        onCopyLog = copyLog,
                        onShareFile = shareFile,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.46f)
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CrashExplainPane(
                        info = info,
                        source = source,
                        logText = log,
                        logFile = logFile,
                        modifier = Modifier
                            .weight(1.06f)
                            .fillMaxHeight()
                    )
                    CrashHelpPane(
                        launcherCrash = launcherCrash,
                        recovery = recovery,
                        recovered = recovered,
                        onRecover = applyRecovery,
                        onCopyLog = copyLog,
                        onShareFile = shareFile,
                        modifier = Modifier
                            .weight(0.94f)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
private fun DiagnosticBanner(
    title: String,
    subtitle: String,
    badgeText: String,
    onBackToLauncher: (() -> Unit)?,
    onClose: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Amber.Danger.copy(alpha = 0.16f),
                        AmberUi.Surface
                    )
                ),
                shape
            )
            .border(1.dp, Amber.Danger.copy(alpha = 0.42f), shape)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        val bannerWidth = maxWidth
        val stackActions = bannerWidth < 480.dp && onBackToLauncher != null
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Amber.Danger.copy(alpha = 0.2f))
                        .border(1.dp, Amber.Danger.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        tint = Amber.DangerSoft,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            color = Amber.Text,
                            fontSize = if (bannerWidth < 420.dp) 15.sp else 18.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Amber.Danger.copy(alpha = 0.22f), CircleShape)
                                .border(1.dp, Amber.Danger.copy(alpha = 0.5f), CircleShape)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badgeText,
                                color = Amber.DangerSoft,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.6.sp,
                                maxLines = 1
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = Amber.TextDim,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (onBackToLauncher != null && !stackActions) {
                    Spacer(modifier = Modifier.width(10.dp))
                    AmberButton(
                        text = "Kembali ke launcher",
                        onClick = onBackToLauncher,
                        style = AmberButtonStyle.Ghost,
                        height = 40.dp,
                        modifier = Modifier.testTag("crash_back_button")
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable { onClose() }
                        .testTag("crash_close_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Amber.Fill)
                            .border(1.dp, Amber.LineStrong, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Tutup laporan crash",
                            tint = Amber.Text,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            if (stackActions && onBackToLauncher != null) {
                AmberButton(
                    text = "Kembali ke launcher",
                    onClick = onBackToLauncher,
                    style = AmberButtonStyle.Ghost,
                    height = 40.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("crash_back_button")
                )
            }
        }
    }
}

/** Kolom kiri: tab Diagnosis & Konsol Log bergaya terminal. */
@Composable
private fun CrashExplainPane(
    info: GameCrashInfo,
    source: CrashSource,
    logText: String,
    logFile: File,
    modifier: Modifier
) {
    var tab by remember { mutableIntStateOf(0) }
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(AmberUi.Surface, RoundedCornerShape(12.dp))
                .border(1.dp, Amber.Line, RoundedCornerShape(12.dp))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            DiagnosticTabPill(
                label = "Diagnosis",
                icon = Icons.Outlined.BugReport,
                selected = tab == 0,
                onClick = { tab = 0 },
                modifier = Modifier
                    .weight(1f)
                    .testTag("crash_tab_explain")
            )
            DiagnosticTabPill(
                label = "Konsol Log",
                icon = Icons.Outlined.Terminal,
                selected = tab == 1,
                onClick = { tab = 1 },
                modifier = Modifier
                    .weight(1f)
                    .testTag("crash_tab_log")
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        if (tab == 0) {
            CrashExplainContent(info, source, Modifier.weight(1f))
        } else {
            CrashLogContent(logText, logFile, Modifier.weight(1f))
        }
    }
}

@Composable
private fun DiagnosticTabPill(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(9.dp)
    Row(
        modifier = modifier
            .height(34.dp)
            .clip(shape)
            .background(if (selected) AmberUi.Accent else Color.Transparent, shape)
            .clickable { onClick() },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) Color.White else Amber.TextDim,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = if (selected) Color.White else Amber.TextDim,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun CrashExplainContent(info: GameCrashInfo, source: CrashSource, modifier: Modifier) {
    val launcherCrash = info.isLauncherCrash
    val sourceAccent = when (source) {
        CrashSource.LAUNCHER -> Amber.Danger
        CrashSource.MEMORY -> Amber.Yellow
        CrashSource.GRAPHICS -> AmberUi.AccentSoft
        CrashSource.GAME_OR_MODS -> Color(0xFFF59E0B)
        CrashSource.UNKNOWN -> Amber.TextDim
    }
    val sourceIcon = when (source) {
        CrashSource.LAUNCHER -> Icons.Outlined.BugReport
        CrashSource.MEMORY -> Icons.Outlined.Memory
        CrashSource.GRAPHICS -> Icons.Outlined.Tune
        CrashSource.GAME_OR_MODS -> Icons.Outlined.Extension
        CrashSource.UNKNOWN -> Icons.Outlined.HelpOutline
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = if (launcherCrash) INTRO_LAUNCHER else INTRO_GAME,
            color = Amber.TextDim,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )

        // Root-Cause Highlight Card
        val causeShape = RoundedCornerShape(16.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(causeShape)
                .background(sourceAccent.copy(alpha = 0.1f), causeShape)
                .border(1.dp, sourceAccent.copy(alpha = 0.42f), causeShape)
                .padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(52.dp)
                    .clip(CircleShape)
                    .background(sourceAccent)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = sourceIcon,
                        contentDescription = null,
                        tint = sourceAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "DUGAAN PENYEBAB",
                        color = sourceAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = sourceLabel(source),
                    color = Amber.Text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = sourceDetail(source),
                    color = Amber.TextDim,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }

        // System & Session Telemetry Card
        DiagnosticCard(title = "DETAIL SESI & LINGKUNGAN") {
            if (launcherCrash) {
                DiagnosticSpecRow("Aplikasi", "NovaCraft")
                DiagnosticSpecRow("Status", "Kesalahan internal tak terduga")
            } else {
                DiagnosticSpecRow("Instance", "${info.instanceName} (MC ${info.mcVersion})")
                val hasLoader = !info.loader.equals("vanilla", ignoreCase = true)
                if (hasLoader) {
                    val loaderName = com.aistudio.novacraft.launcher.core.launch.ForgeFlavor.label(info.loader)
                    DiagnosticSpecRow("Loader", "$loaderName ${info.loaderVersion ?: ""}".trim())
                }
                if (info.installedMods.isNotEmpty()) {
                    DiagnosticSpecRow("Mod terpasang", "${info.installedMods.size} mod")
                }
                if (!info.graphicsInfo.isNullOrBlank()) {
                    DiagnosticSpecRow("Grafis", info.graphicsInfo)
                }
                DiagnosticSpecRow("Status keluar", CrashAdvisor.describeExit(info))
            }
        }
    }
}

@Composable
private fun DiagnosticSpecRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            color = Amber.TextFaint,
            fontSize = 12.sp,
            modifier = Modifier.widthIn(min = 84.dp, max = 116.dp)
        )
        Text(
            text = value,
            color = Amber.Text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CrashLogContent(logText: String, logFile: File, modifier: Modifier) {
    val shape = RoundedCornerShape(16.dp)
    val shown = remember(logText) {
        if (logText.length > MAX_SHOWN_LOG_CHARS) logText.takeLast(MAX_SHOWN_LOG_CHARS) else logText
    }
    val fileName = remember(logFile) {
        if (logFile.name.isNotBlank() && logFile.exists()) logFile.name else "latestlog.txt"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0xFF040814), shape)
            .border(1.dp, Amber.LineStrong, shape)
    ) {
        // Terminal header bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AmberUi.Surface)
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Terminal,
                    contentDescription = null,
                    tint = AmberUi.AccentSoft,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = fileName,
                    color = Amber.Text,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "${shown.length} chars",
                color = Amber.TextFaint,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Amber.Line)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(10.dp)
        ) {
            // Raw log bypasses translator via Material Text
            androidx.compose.material3.Text(
                text = shown,
                color = Amber.TextDim,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                softWrap = false,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState())
            )
        }
    }
}

/** Kolom kanan: Pemulihan cepat & Berbagi Log. */
@Composable
private fun CrashHelpPane(
    launcherCrash: Boolean,
    recovery: List<RecoveryOption>,
    recovered: Set<RecoveryAction>,
    onRecover: (RecoveryOption) -> Unit,
    onCopyLog: () -> Unit,
    onShareFile: () -> Unit,
    modifier: Modifier
) {
    val sharePrimary = launcherCrash || recovery.isEmpty()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (!launcherCrash && recovery.isNotEmpty()) {
            RecoverySection(recovery = recovery, recovered = recovered, onRecover = onRecover)
        }
        DiagnosticCard(title = "BAGIKAN LOG") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AmberButton(
                    text = "Bagikan file",
                    onClick = onShareFile,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("crash_share_file_button"),
                    style = if (sharePrimary) AmberButtonStyle.Primary else AmberButtonStyle.Ghost,
                    icon = "share",
                    height = 44.dp
                )
                AmberButton(
                    text = "Salin log",
                    onClick = onCopyLog,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("crash_copy_log_button"),
                    style = AmberButtonStyle.Ghost,
                    icon = "copy",
                    height = 44.dp
                )
            }
        }
    }
}

@Composable
private fun RecoverySection(
    recovery: List<RecoveryOption>,
    recovered: Set<RecoveryAction>,
    onRecover: (RecoveryOption) -> Unit
) {
    DiagnosticCard(title = "PEMULIHAN CEPAT (1-KETUK)") {
        Text(text = RECOVERY_BODY, color = Amber.TextDim, fontSize = 12.sp, lineHeight = 17.sp)
        Spacer(modifier = Modifier.height(6.dp))
        recovery.forEachIndexed { index, option ->
            val done = option.action in recovered
            val cardShape = RoundedCornerShape(12.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(
                        if (done) Amber.Good.copy(alpha = 0.14f) else Amber.Fill,
                        cardShape
                    )
                    .border(
                        1.dp,
                        if (done) Amber.Good.copy(alpha = 0.5f) else Amber.Line,
                        cardShape
                    )
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = option.detail, color = Amber.TextDim, fontSize = 12.sp, lineHeight = 17.sp)
                AmberButton(
                    text = if (done) "Sudah diterapkan" else option.label,
                    onClick = { onRecover(option) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("crash_recovery_${option.action.name}"),
                    style = if (index == 0 && !done) AmberButtonStyle.Primary else AmberButtonStyle.Ghost,
                    enabled = !done,
                    height = 40.dp
                )
            }
            if (index < recovery.lastIndex) {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
        if (recovered.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = Amber.Good,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = RECOVERY_DONE, color = Amber.Good, fontSize = 12.sp, lineHeight = 17.sp)
            }
        }
    }
}

@Composable
private fun DiagnosticCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AmberUi.Surface, shape)
            .border(1.dp, Amber.Line, shape)
            .padding(12.dp)
    ) {
        Text(
            text = title,
            color = AmberUi.AccentSoft,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.9.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        content()
    }
}

private fun sourceLabel(source: CrashSource): String = when (source) {
    CrashSource.LAUNCHER -> "Kesalahan di dalam NovaCraft"
    CrashSource.MEMORY -> "Memori (RAM) HP habis"
    CrashSource.GRAPHICS -> "Renderer atau driver grafis"
    CrashSource.GAME_OR_MODS -> "Mod, versi Java, atau file game"
    CrashSource.UNKNOWN -> "Belum bisa dipastikan"
}

private fun sourceDetail(source: CrashSource): String = when (source) {
    CrashSource.LAUNCHER ->
        "Launcher sendiri mengalami error tak terduga, bukan game atau mod. Periksa konsol log atau bagikan file log."
    CrashSource.MEMORY ->
        "Sistem menghentikan game karena RAM tidak mencukupi. Kurangi alokasi RAM (maksimal setengah RAM HP), kurangi mod berat, dan tutup aplikasi lain."
    CrashSource.GRAPHICS ->
        "Game berhenti di lapisan grafis atau driver perangkat. Coba ganti renderer di Pengaturan > Grafis atau gunakan pemulihan cepat."
    CrashSource.GAME_OR_MODS ->
        "Error berasal dari game atau mod: kemungkinan ketidakcocokan versi mod/loader, dependency kurang, atau versi Java tidak sesuai."
    CrashSource.UNKNOWN ->
        "Penyebab pasti terlihat dari isi konsol log. Buka tab Konsol Log atau bagikan file log untuk memeriksanya."
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    } catch (_: Throwable) {
    }
}

// ----------------------------------------------------------------------------
// Section: ui/components/AmberDialogs.kt
// ----------------------------------------------------------------------------

/** Gaya tombol AmberLauncher: utama (kuning), bahaya (merah), dan kaca (hanya garis tepi). */
enum class AmberButtonStyle { Primary, Danger, Ghost }

/**
 * Tombol datar AmberLauncher. Tanpa bayangan, tanpa animasi pegas; umpan balik tekan hanya
 * lapisan putih tipis yang digambar di fase gambar (lihat [amberTap]).
 */
@Composable
fun AmberButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: AmberButtonStyle = AmberButtonStyle.Primary,
    icon: String? = null,
    enabled: Boolean = true,
    height: Dp = 44.dp
) {
    val shape = RoundedCornerShape(12.dp)
    val fill = when (style) {
        AmberButtonStyle.Primary -> Amber.Yellow
        AmberButtonStyle.Danger -> Amber.Danger
        AmberButtonStyle.Ghost -> Color.Transparent
    }
    val ink = when {
        !enabled -> Amber.TextFaint
        style == AmberButtonStyle.Ghost -> Amber.Text
        else -> Amber.Ink
    }
    val base = modifier
        .height(height)
        .clip(shape)
        .background(if (enabled) fill else Amber.Fill)
    val framed = if (style == AmberButtonStyle.Ghost || !enabled) {
        base.border(1.dp, Amber.LineStrong, shape)
    } else {
        base
    }
    val tappable = if (enabled) framed.amberTap(onClick) else framed

    Row(
        modifier = tappable.padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            AmberIcon(icon, 18.dp, ink)
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            color = ink,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

/**
 * Kerangka dialog kecil (konfirmasi, peringatan, proses): ikon berwarna + judul, isi, lalu baris
 * tombol (boleh kosong untuk dialog proses). Memakai NuxDialog sehingga tepi kaca dan latar redupnya sama dengan dialog lain.
 */
@Composable
fun AmberAlert(
    onDismissRequest: () -> Unit,
    title: String,
    icon: String,
    accent: Color,
    modifier: Modifier = Modifier,
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    NuxDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.widthIn(max = 460.dp),
        compact = true
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    AmberIcon(icon, 22.dp, accent)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    color = Amber.Text,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
            if (actions != null) {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    content = actions
                )
            }
        }
    }
}

/** Kotak info kecil di dalam dialog (nama instance, dsb.): isian kaca + tepi tipis. */
@Composable
fun AmberInfoBox(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Amber.Fill)
            .border(1.dp, Amber.Line, shape)
            .padding(12.dp),
        content = content
    )
}

// ----------------------------------------------------------------------------
// Section: ui/components/AmberPage.kt
// ----------------------------------------------------------------------------

// =============================================================================
// HALAMAN KACA untuk dialog: bahasa tampilan yang sama dengan layar Mod/Akun/Pengaturan
// (pil kaca, bar atas melayang, kartu bersudut besar) supaya dialog terasa bagian dari launcher,
// bukan kotak asing di tengah layar.
// =============================================================================

/**
 * Ukuran ruang yang tersedia untuk isi halaman.
 * - [narrow]: lebar < 560dp (HP portrait): susun satu kolom.
 * - [short]: tinggi < 300dp (HP landscape kecil): rapatkan jarak.
 */
@Immutable
class PageInfo(val narrow: Boolean, val short: Boolean)

/**
 * Jendela dialog layar penuh dasar untuk semua dialog gaya baru.
 *
 * - Bar status dan navigasi Android disembunyikan (lihat [ImmersiveWindowHook]); isi tidak
 *   mengalah ke bar tersebut. Yang dihindari hanya poni layar, keyboard, dan bar navigasi/taskbar
 *   di bawah bila masih tampil (supaya isi paling bawah tidak tertutup di tablet).
 * - Ukuran huruf dan jarak sama dengan layar lain (0.9x), jadi tidak terasa raksasa.
 * - Kaca selalu gaya Default di sini; kaca buram Liquid Glass tidak bisa dipakai di jendela terpisah.
 */
@Composable
fun AmberOverlay(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissOnScrimTap: Boolean = false,
    scrimAlpha: Float = 0.82f,
    content: @Composable BoxScope.() -> Unit
) {
    val inScaledScreen = LocalNuxScaled.current
    val dismiss by rememberUpdatedState(onDismiss)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        GamepadWindowHook()
        NuxReadable(scale = if (inScaledScreen) 1f else 0.94f, boost = 1f) {
            CompositionLocalProvider(LocalGlassBackdrop provides null) {
                val overlayScrim = if (AmberUi.isDark) {
                    Color(0xFF121115).copy(alpha = scrimAlpha)
                } else {
                    Color(0xFFE2E8F0).copy(alpha = (scrimAlpha + 0.08f).coerceAtMost(0.95f))
                }
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .background(overlayScrim)
                        .then(
                            if (dismissOnScrimTap) {
                                Modifier.pointerInput(Unit) { detectTapGestures { dismiss() } }
                            } else {
                                Modifier
                            }
                        )
                        .windowInsetsPadding(
                            WindowInsets.displayCutout
                                .union(WindowInsets.ime)
                                .union(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                        ),
                    content = content
                )
            }
        }
    }
}

/**
 * Halaman dialog: bar atas melayang (panah kembali + judul dalam pil kaca, keterangan, tombol aksi di
 * kanan), lalu isi di bawahnya. Tidak ada tombol di dasar layar, jadi tidak ada yang bisa tertutup bar
 * sistem di tablet; aksi utama (Simpan) ada di bar atas.
 *
 * Panah kembali menutup halaman tanpa menyimpan. Isi mengatur gulirannya sendiri.
 */
@Composable
fun AmberPage(
    onDismiss: () -> Unit,
    title: String,
    icon: ImageVector? = null,
    subtitle: String? = null,
    dismissOnScrimTap: Boolean = false,
    scrimAlpha: Float = 0.82f,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable BoxScope.(PageInfo) -> Unit
) {
    AmberOverlay(
        onDismiss = onDismiss,
        dismissOnScrimTap = dismissOnScrimTap,
        scrimAlpha = scrimAlpha
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val info = PageInfo(narrow = maxWidth < 560.dp, short = maxHeight < 300.dp)
            Column(modifier = Modifier.fillMaxSize()) {
                AmberPageBar(
                    title = title,
                    icon = icon,
                    subtitle = if (info.narrow) null else subtitle,
                    onDismiss = onDismiss,
                    actions = actions
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    content(info)
                }
            }
        }
    }
}

@Composable
private fun AmberPageBar(
    title: String,
    icon: ImageVector?,
    subtitle: String?,
    onDismiss: () -> Unit,
    actions: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Pil kiri: panah kembali + judul, bentuknya sama dengan pil navigasi di layar Mod/Akun
        Row(
            modifier = Modifier
                .weight(1f, fill = false)
                .defaultMinSize(minHeight = 48.dp)
                .clip(CircleShape)
                .amberGlass(CircleShape)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AmberRoundIcon(
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                description = "Tutup",
                onClick = onDismiss
            )
            Row(
                modifier = Modifier
                    .defaultMinSize(minHeight = 40.dp)
                    .clip(CircleShape)
                    .background(AmberUi.SurfaceRaised, CircleShape)
                    .border(1.dp, AmberUi.BorderStrong, CircleShape)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = AmberUi.Text,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = title,
                    color = AmberUi.Text,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp)
        ) {
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = AmberUi.TextDim,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = actions
        )
    }
}

/** Judul kecil huruf renggang di atas satu bagian isi (NAMA, JAVA, dll). */
@Composable
fun AmberPageLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = AmberUi.TextDim,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        maxLines = 1,
        modifier = modifier.padding(start = 4.dp, bottom = 6.dp)
    )
}

/** Kolom isian berbentuk pil kaca, sama dengan kolom cari di layar Mod. */
@Composable
fun AmberField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clip(CircleShape)
            .amberGlass(CircleShape)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AmberUi.Text,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
        }
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = AmberUi.TextDim,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    color = AmberUi.Text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(AmberUi.AccentSoft),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** Pil pilihan: terpilih = isian aksen dengan tepi terang, selain itu kaca. */
@Composable
fun AmberChoicePill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 36.dp)
            .clip(CircleShape)
            .then(
                if (selected) {
                    Modifier
                        .background(AmberUi.Accent, CircleShape)
                        .border(1.dp, AmberUi.AccentEdge, CircleShape)
                } else {
                    Modifier.amberGlass(CircleShape)
                }
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) AmberUi.OnAccent else AmberUi.Text,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Baris aksi berbentuk pil: ikon dalam lingkaran, nama, keterangan kecil, dan panah bila membuka
 * halaman lain. [danger] = bingkai dan teks merah (hapus).
 */
@Composable
fun AmberActionPill(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    showChevron: Boolean = false,
    danger: Boolean = false
) {
    val tone = if (danger) AmberUi.Danger else AmberUi.AccentSoft
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .clip(CircleShape)
            .then(
                if (danger) {
                    Modifier
                        .background(AmberUi.Danger.copy(alpha = 0.08f), CircleShape)
                        .border(1.dp, AmberUi.Danger.copy(alpha = 0.55f), CircleShape)
                } else {
                    Modifier.amberGlass(CircleShape)
                }
            )
            .clickable { onClick() }
            .padding(start = 10.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(tone.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tone,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            color = if (danger) AmberUi.Danger else AmberUi.Text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) {
            Text(
                text = trailing,
                color = AmberUi.TextDim,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .widthIn(max = 120.dp)
            )
        }
        if (showChevron) {
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = AmberUi.TextDim,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ----------------------------------------------------------------------------
// Section: ui/components/AmberParts.kt
// ----------------------------------------------------------------------------

/**
 * Bahan dasar gaya AmberLauncher untuk dialog dan panel di dalam game. Semuanya digambar langsung
 * di kanvas, tanpa ripple, tanpa blur, dan status tekan dibaca hanya saat menggambar, jadi
 * menyentuh sesuatu di menu tidak menyusun ulang layar sementara game masih berjalan.
 */

/**
 * Ketuk dengan umpan balik tekan (lapisan putih tipis) dan sorotan fokus untuk controller/D-pad.
 * Pasang setelah clip bentuknya. Status tekan dan fokus hanya dibaca saat menggambar.
 */
@Composable
fun Modifier.amberTap(onClick: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed = interaction.collectIsPressedAsState()
    val focused = interaction.collectIsFocusedAsState()
    return this
        .drawWithContent {
            drawContent()
            if (pressed.value) drawRect(Amber.Press)
            if (focused.value) drawGamepadFocusRing()
        }
        .clickable(interactionSource = interaction, indication = null, onClick = onClick)
}

/** Ikon garis buatan sendiri (lihat drawControlIcon di ControlSkin). */
@Composable
fun AmberIcon(
    name: String,
    iconSize: Dp,
    color: Color,
    modifier: Modifier = Modifier,
    slash: Boolean = false
) {
    Box(
        modifier = modifier
            .size(iconSize)
            .drawBehind {
                drawControlIcon(name, size.width / 2f, size.height / 2f, size.minDimension, color, slash)
            }
    )
}

fun fpsColor(value: Int): Color = when {
    value >= 50 -> Amber.Good
    value >= 25 -> Amber.Yellow
    value > 0 -> Amber.Danger
    else -> Amber.TextFaint
}

/** Titik warna + angka FPS. Angka dibaca lewat lambda di sini saja. */
@Composable
fun FpsReadout(fps: () -> Int) {
    val value = fps()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(fpsColor(value), CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (value > 0) "$value FPS" else "-- FPS",
            color = Amber.Text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

/** Saklar tipis: kuning saat aktif. */
@Composable
fun AmberToggle(checked: Boolean, onChange: (Boolean) -> Unit) {
    val callback by rememberUpdatedState(onChange)
    val interaction = remember { MutableInteractionSource() }
    val focused = interaction.collectIsFocusedAsState()
    Box(
        modifier = Modifier
            .size(width = 40.dp, height = 24.dp)
            .drawBehind {
                val r = size.height / 2f
                if (checked) {
                    drawRoundRect(color = Amber.Yellow, cornerRadius = CornerRadius(r))
                } else {
                    val sw = 1.dp.toPx()
                    drawRoundRect(color = Amber.Fill, cornerRadius = CornerRadius(r))
                    drawRoundRect(
                        color = Amber.LineStrong,
                        topLeft = Offset(sw / 2f, sw / 2f),
                        size = Size(size.width - sw, size.height - sw),
                        cornerRadius = CornerRadius(r - sw / 2f),
                        style = Stroke(width = sw)
                    )
                }
                val cx = if (checked) size.width - r else r
                drawCircle(
                    color = if (checked) Amber.Ink else Amber.TextDim,
                    radius = r - 5.dp.toPx(),
                    center = Offset(cx, r)
                )
                if (focused.value) drawGamepadFocusRing()
            }
            .clickable(interactionSource = interaction, indication = null) { callback(!checked) }
    )
}

/**
 * Penggeser tipis satu simpul gambar. [value] dibaca saat menggambar, jadi menggeser hanya
 * menggambar ulang penggeser itu sendiri.
 */
@Composable
fun AmberSlider(
    value: () -> Float,
    range: ClosedFloatingPointRange<Float>,
    step: Float,
    onChange: (Float) -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val change by rememberUpdatedState(onChange)
    val finish by rememberUpdatedState(onFinished)
    val read by rememberUpdatedState(value)
    val interaction = remember { MutableInteractionSource() }
    val focused = interaction.collectIsFocusedAsState()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(30.dp)
            // D-pad kiri/kanan menggeser satu langkah saat penggeser disorot.
            .onKeyEvent { event ->
                val dir = when (event.key) {
                    Key.DirectionLeft -> -1f
                    Key.DirectionRight -> 1f
                    else -> 0f
                }
                if (dir == 0f) {
                    false
                } else {
                    if (event.type == KeyEventType.KeyDown) {
                        val unit = if (step > 0f) step else (range.endInclusive - range.start) / 20f
                        change((read() + dir * unit).coerceIn(range.start, range.endInclusive))
                        finish()
                    }
                    true
                }
            }
            .focusable(interactionSource = interaction)
            .drawBehind {
                val pad = 10.dp.toPx()
                val y = size.height / 2f
                val span = size.width - 2f * pad
                val frac = ((read() - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
                val sw = 3.dp.toPx()
                drawLine(Amber.LineStrong, Offset(pad, y), Offset(pad + span, y), sw, StrokeCap.Round)
                if (frac > 0f) {
                    drawLine(Amber.Yellow, Offset(pad, y), Offset(pad + span * frac, y), sw, StrokeCap.Round)
                }
                val c = Offset(pad + span * frac, y)
                drawCircle(Amber.YellowTint, 10.dp.toPx(), c)
                drawCircle(Amber.Yellow, 6.5.dp.toPx(), c)
                if (focused.value) drawGamepadFocusRing()
            }
            .pointerInput(range, step) {
                val pad = 10.dp.toPx()
                fun toValue(x: Float): Float {
                    val span = size.width - 2f * pad
                    val f = if (span <= 0f) 0f else ((x - pad) / span).coerceIn(0f, 1f)
                    val raw = range.start + f * (range.endInclusive - range.start)
                    return if (step > 0f) {
                        ((raw / step).roundToInt() * step).coerceIn(range.start, range.endInclusive)
                    } else {
                        raw
                    }
                }
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    change(toValue(down.position.x))
                    while (true) {
                        val event = awaitPointerEvent()
                        val ch = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!ch.pressed) break
                        ch.consume()
                        change(toValue(ch.position.x))
                    }
                    finish()
                }
            }
    )
}

/** Pilihan 2-3 segmen: segmen terpilih kuning dengan teks tinta. */
@Composable
fun AmberSegmented(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val outer = RoundedCornerShape(10.dp)
    val inner = RoundedCornerShape(8.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .clip(outer)
            .background(Amber.Fill)
            .border(1.dp, Amber.Line, outer)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEachIndexed { index, label ->
            val on = index == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(inner)
                    .background(if (on) Amber.Yellow else Color.Transparent)
                    .amberTap { onSelect(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (on) Amber.Ink else Amber.TextDim,
                    fontSize = 12.sp,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}

// ----------------------------------------------------------------------------
// Section: ui/components/AmbientGlow.kt
// ----------------------------------------------------------------------------

/** Kisi warna kecil (W x H, opak) dari bingkai GIF atau video yang sedang tampil. */
@Immutable
class AmbientColors(val grid: IntArray)

/** Tempat penyampaian warna dari latar ke lapisan cahaya; null = ambient mati atau belum ada data. */
@Stable
class AmbientState {
    var colors by mutableStateOf<AmbientColors?>(null)

    /** Lebar / tinggi kotak tempat latar tampil; dipakai agar GIF dibaca terpotong seperti tampilannya. */
    @Volatile
    var aspect: Float = 16f / 9f
}

/**
 * Mengambil kisi warna dari bingkai latar. Bingkai digambar dulu ke bitmap kecil (192x108) lalu
 * dirata-ratakan jadi 48x27 supaya tidak bergerigi. Semuanya murah: sekitar 20 ribu piksel, jadi
 * aman dilakukan di thread utama beberapa kali per detik.
 */
object AmbientSampler {
    const val W = 48
    const val H = 27
    private const val SRC_W = W * 4
    private const val SRC_H = H * 4

    /** Jeda antar pengambilan warna (ms). */
    const val INTERVAL_MS = 700L

    fun newBitmap(): Bitmap = Bitmap.createBitmap(SRC_W, SRC_H, Bitmap.Config.ARGB_8888)

    fun newPixels(): IntArray = IntArray(SRC_W * SRC_H)

    /** [bmp] berukuran 192x108 sudah berisi bingkai yang diperkecil; null bila bingkai belum tergambar. */
    fun fromBitmap(bmp: Bitmap, pixels: IntArray): AmbientColors? {
        if (bmp.width != SRC_W || bmp.height != SRC_H) return null
        bmp.getPixels(pixels, 0, SRC_W, 0, 0, SRC_W, SRC_H)
        val grid = IntArray(W * H)
        var opaque = 0
        for (gy in 0 until H) {
            for (gx in 0 until W) {
                var r = 0
                var g = 0
                var b = 0
                var n = 0
                for (y in gy * 4 until gy * 4 + 4) {
                    val row = y * SRC_W
                    for (x in gx * 4 until gx * 4 + 4) {
                        val c = pixels[row + x]
                        if ((c ushr 24) < 32) continue
                        r += (c shr 16) and 0xFF
                        g += (c shr 8) and 0xFF
                        b += c and 0xFF
                        n++
                    }
                }
                if (n == 0) {
                    grid[gy * W + gx] = 0xFF000000.toInt()
                } else {
                    opaque++
                    grid[gy * W + gx] = (0xFF shl 24) or ((r / n) shl 16) or ((g / n) shl 8) or (b / n)
                }
            }
        }
        if (opaque < W * H / 4) return null
        return AmbientColors(grid)
    }

    /**
     * Menggambar bingkai [drawable] saat ini (mis. GIF) ke bitmap kecil lalu mengambil warnanya.
     * [boxAspect] = lebar/tinggi kotak tampil; gambar dipotong tengah seperti CENTER_CROP.
     */
    fun fromDrawable(drawable: Drawable, bmp: Bitmap, pixels: IntArray, boxAspect: Float): AmbientColors? {
        val b = drawable.bounds
        val dw = b.width().toFloat()
        val dh = b.height().toFloat()
        if (dw <= 0f || dh <= 0f) return null
        val aspect = if (boxAspect > 0.1f) boxAspect else 16f / 9f
        val visW: Float
        val visH: Float
        if (dw / dh > aspect) {
            visH = dh
            visW = dh * aspect
        } else {
            visW = dw
            visH = dw / aspect
        }
        val srcLeft = b.left + (dw - visW) / 2f
        val srcTop = b.top + (dh - visH) / 2f
        bmp.eraseColor(android.graphics.Color.BLACK)
        val canvas = android.graphics.Canvas(bmp)
        canvas.scale(SRC_W / visW, SRC_H / visH)
        canvas.translate(-srcLeft, -srcTop)
        drawable.draw(canvas)
        return fromBitmap(bmp, pixels)
    }
}

/**
 * Pembuat "medan cahaya": bitmap kecil (satu piksel = sekitar 4dp) seukuran layar yang sudah
 * berisi hasil akhir latar. Warna di luar kartu mengikuti tepi video di titik yang sama (diperluas
 * keluar seperti Ambilight), memudar halus makin jauh dari kartu. Tidak ada garis atau cincin.
 * Dikerjakan di thread latar sekali tiap sampel; yang tergambar tiap bingkai hanya satu gambar kecil.
 */
private class AmbientFieldBuilder {
    private data class Geo(
        val w: Int, val h: Int,
        val l: Float, val t: Float, val r: Float, val b: Float,
        val radius: Float, val density: Float, val strength: Float
    )

    private var geo: Geo? = null
    private var fw = 0
    private var fh = 0
    private var colIdx = IntArray(0)
    private var colW = FloatArray(0)
    private var rowIdx = IntArray(0)
    private var rowW = FloatArray(0)
    private var alpha = FloatArray(0)

    private val ink = NuxColors.Ink.toArgb()
    private val inkR = (ink shr 16) and 0xFF
    private val inkG = (ink shr 8) and 0xFF
    private val inkB = ink and 0xFF

    @Synchronized
    fun build(
        grid: IntArray,
        width: Int,
        height: Int,
        box: Rect,
        radiusPx: Float,
        density: Float,
        strength: Float
    ): Bitmap? {
        if (width <= 0 || height <= 0 || box.width <= 1f || box.height <= 1f) return null
        val key = Geo(width, height, box.left, box.top, box.right, box.bottom, radiusPx, density, strength)
        if (key != geo) {
            prepare(key)
            geo = key
        }

        val gw = AmbientSampler.W
        val gh = AmbientSampler.H
        val n = gw * gh
        val pr = FloatArray(n)
        val pg = FloatArray(n)
        val pb = FloatArray(n)
        for (i in 0 until n) {
            val c = grid[i]
            pr[i] = ((c shr 16) and 0xFF) / 255f
            pg[i] = ((c shr 8) and 0xFF) / 255f
            pb[i] = (c and 0xFF) / 255f
        }

        // Haluskan dulu: detail gambar tidak penting, yang dicari suasana warnanya.
        val scratch = FloatArray(n)
        blur(pr, gw, gh, scratch)
        blur(pg, gw, gh, scratch)
        blur(pb, gw, gh, scratch)

        // Warna lebih jenuh dan gelapnya sedikit diangkat, tapi terangnya dibatasi supaya teks
        // putih di sidebar tetap terbaca.
        val cap = 0.45f + 0.35f * strength
        for (i in 0 until n) {
            var r = pr[i]
            var g = pg[i]
            var b = pb[i]
            val lum = 0.2126f * r + 0.7152f * g + 0.0722f * b
            r = (lum + (r - lum) * SATURATION).coerceIn(0f, 1f).pow(GAMMA)
            g = (lum + (g - lum) * SATURATION).coerceIn(0f, 1f).pow(GAMMA)
            b = (lum + (b - lum) * SATURATION).coerceIn(0f, 1f).pow(GAMMA)
            val lum2 = 0.2126f * r + 0.7152f * g + 0.0722f * b
            if (lum2 > cap) {
                val k = cap / lum2
                r *= k
                g *= k
                b *= k
            }
            pr[i] = r
            pg[i] = g
            pb[i] = b
        }

        // Lebarkan ke medan: mendatar dulu, lalu menurun (bikubik, tepi diperpanjang keluar).
        val tr = FloatArray(gh * fw)
        val tg = FloatArray(gh * fw)
        val tb = FloatArray(gh * fw)
        for (y in 0 until gh) {
            val row = y * gw
            for (fx in 0 until fw) {
                val o = fx * 4
                var r = 0f
                var g = 0f
                var b = 0f
                for (k in 0 until 4) {
                    val src = row + colIdx[o + k]
                    val wgt = colW[o + k]
                    r += pr[src] * wgt
                    g += pg[src] * wgt
                    b += pb[src] * wgt
                }
                tr[y * fw + fx] = r
                tg[y * fw + fx] = g
                tb[y * fw + fx] = b
            }
        }

        val out = IntArray(fw * fh)
        for (fy in 0 until fh) {
            val o = fy * 4
            for (fx in 0 until fw) {
                var r = 0f
                var g = 0f
                var b = 0f
                for (k in 0 until 4) {
                    val src = rowIdx[o + k] * fw + fx
                    val wgt = rowW[o + k]
                    r += tr[src] * wgt
                    g += tg[src] * wgt
                    b += tb[src] * wgt
                }
                val a = alpha[fy * fw + fx]
                val ro = (inkR * (1f - a) + r.coerceIn(0f, 1f) * 255f * a).roundToInt().coerceIn(0, 255)
                val go = (inkG * (1f - a) + g.coerceIn(0f, 1f) * 255f * a).roundToInt().coerceIn(0, 255)
                val bo = (inkB * (1f - a) + b.coerceIn(0f, 1f) * 255f * a).roundToInt().coerceIn(0, 255)
                out[fy * fw + fx] = (0xFF shl 24) or (ro shl 16) or (go shl 8) or bo
            }
        }

        val bmp = Bitmap.createBitmap(fw, fh, Bitmap.Config.ARGB_8888)
        bmp.setPixels(out, 0, fw, 0, 0, fw, fh)
        return bmp
    }

    /** Hitung hal yang hanya bergantung pada ukuran layar, posisi kartu, dan kekuatan. */
    private fun prepare(g: Geo) {
        val cell = max(4, (4f * g.density).roundToInt())
        fw = ceil(g.w / cell.toFloat()).toInt()
        fh = ceil(g.h / cell.toFloat()).toInt()

        colIdx = IntArray(fw * 4)
        colW = FloatArray(fw * 4)
        val bw = g.r - g.l
        for (fx in 0 until fw) {
            val x = (fx + 0.5f) * cell
            val u = ((x - g.l) / bw).coerceIn(0f, 1f)
            taps(u * AmbientSampler.W - 0.5f, AmbientSampler.W, colIdx, colW, fx * 4)
        }
        rowIdx = IntArray(fh * 4)
        rowW = FloatArray(fh * 4)
        val bh = g.b - g.t
        for (fy in 0 until fh) {
            val y = (fy + 0.5f) * cell
            val v = ((y - g.t) / bh).coerceIn(0f, 1f)
            taps(v * AmbientSampler.H - 0.5f, AmbientSampler.H, rowIdx, rowW, fy * 4)
        }

        // Seberapa kuat cahaya di tiap titik: terkuat di tepi kartu, memudar ke luar.
        val s = g.strength.coerceIn(0f, 1f)
        val gain = 0.35f + 0.75f * s
        val reach = (48f + 70f * s) * g.density
        val cx = (g.l + g.r) / 2f
        val cy = (g.t + g.b) / 2f
        val hw = (g.r - g.l) / 2f - g.radius
        val hh = (g.b - g.t) / 2f - g.radius
        alpha = FloatArray(fw * fh)
        for (fy in 0 until fh) {
            val y = (fy + 0.5f) * cell
            for (fx in 0 until fw) {
                val x = (fx + 0.5f) * cell
                val dx = abs(x - cx) - hw
                val dy = abs(y - cy) - hh
                val outside = sqrt(max(dx, 0f) * max(dx, 0f) + max(dy, 0f) * max(dy, 0f))
                val inside = min(max(dx, dy), 0f)
                val dist = max(outside + inside - g.radius, 0f)
                val falloff = exp(-(dist / reach).pow(1.25f))
                alpha[fy * fw + fx] = min(MAX_ALPHA, gain * falloff)
            }
        }
    }

    /** Bobot bikubik Catmull-Rom untuk titik [pos] di antara [n] sel. */
    private fun taps(pos: Float, n: Int, idx: IntArray, w: FloatArray, off: Int) {
        val i0 = floor(pos).toInt()
        val t = pos - i0
        val t2 = t * t
        val t3 = t2 * t
        w[off] = -0.5f * t3 + t2 - 0.5f * t
        w[off + 1] = 1.5f * t3 - 2.5f * t2 + 1f
        w[off + 2] = -1.5f * t3 + 2f * t2 + 0.5f * t
        w[off + 3] = 0.5f * t3 - 0.5f * t2
        for (k in 0 until 4) idx[off + k] = (i0 - 1 + k).coerceIn(0, n - 1)
    }

    /** Dua kali blur kotak berjari-jari 2 sel di kedua arah. */
    private fun blur(a: FloatArray, w: Int, h: Int, tmp: FloatArray) {
        val radius = 2
        val div = (2 * radius + 1).toFloat()
        repeat(2) {
            for (y in 0 until h) {
                val row = y * w
                for (x in 0 until w) {
                    var sum = 0f
                    for (k in -radius..radius) sum += a[row + (x + k).coerceIn(0, w - 1)]
                    tmp[row + x] = sum / div
                }
            }
            for (x in 0 until w) {
                for (y in 0 until h) {
                    var sum = 0f
                    for (k in -radius..radius) sum += tmp[(y + k).coerceIn(0, h - 1) * w + x]
                    a[y * w + x] = sum / div
                }
            }
        }
    }

    private companion object {
        const val SATURATION = 1.6f
        const val GAMMA = 0.72f
        const val MAX_ALPHA = 0.95f
    }
}

/** Lama peralihan dari satu sampel ke sampel berikutnya (ms). */
private const val FADE_MS = 600

/**
 * Cahaya ambient di belakang sidebar dan di sekeliling kartu Beranda. Warnanya mengikuti isi GIF
 * atau video di titik yang sama: tepi kiri video menerangi sisi kiri, tepi atas menerangi sisi atas,
 * dan seterusnya, memudar lembut makin jauh dari kartu. Digambar sebagai satu bitmap kecil yang
 * dibentangkan ke seluruh layar (bukan lapisan tebal atau cincin), jadi murah untuk GPU HP kelas bawah.
 *
 * [bounds] = kotak kartu dalam koordinat root; [intensity] 0..100.
 */
@Composable
fun AmbientGlow(
    state: AmbientState,
    intensity: Int,
    bounds: Rect?,
    cornerRadius: Dp,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    val density = LocalDensity.current.density
    val radiusPx = with(LocalDensity.current) { cornerRadius.toPx() }
    val builder = remember { AmbientFieldBuilder() }

    var origin by remember { mutableStateOf(Offset.Zero) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var current by remember { mutableStateOf<ImageBitmap?>(null) }
    var previous by remember { mutableStateOf<ImageBitmap?>(null) }
    val mix = remember { Animatable(1f) }
    val master = remember { Animatable(0f) }

    val frame = state.colors
    val strength = (intensity.coerceIn(0, 100)) / 100f

    LaunchedEffect(frame, canvasSize, origin, bounds, strength, density) {
        if (frame == null || bounds == null || canvasSize.width <= 0 || canvasSize.height <= 0) {
            master.animateTo(0f, tween(durationMillis = 600))
            if (frame == null) {
                current = null
                previous = null
            }
            return@LaunchedEffect
        }
        val local = Rect(
            bounds.left - origin.x,
            bounds.top - origin.y,
            bounds.right - origin.x,
            bounds.bottom - origin.y
        )
        val bmp = withContext(Dispatchers.Default) {
            try {
                builder.build(frame.grid, canvasSize.width, canvasSize.height, local, radiusPx, density, strength)
            } catch (_: Throwable) {
                null
            }
        } ?: return@LaunchedEffect

        previous = if (master.value > 0.01f) current else null
        current = bmp.asImageBitmap()
        mix.snapTo(0f)
        coroutineScope {
            launch { master.animateTo(1f, tween(durationMillis = 700)) }
            launch { mix.animateTo(1f, tween(durationMillis = FADE_MS, easing = LinearEasing)) }
        }
        previous = null
    }

    Canvas(
        modifier = modifier.onGloballyPositioned { coords ->
            val p = coords.positionInRoot()
            if (p != origin) origin = p
            if (coords.size != canvasSize) canvasSize = coords.size
        }
    ) {
        val img = current ?: return@Canvas
        val m = master.value
        if (m <= 0.01f) return@Canvas
        val dst = IntSize(size.width.roundToInt(), size.height.roundToInt())
        val old = previous
        val t = mix.value
        if (old != null && t < 0.999f) {
            // Peralihan mulus: gambar lama penuh, gambar baru dimasukkan pelan-pelan di atasnya.
            drawImage(old, dstSize = dst, alpha = m, filterQuality = FilterQuality.Low)
            drawImage(img, dstSize = dst, alpha = t * m, filterQuality = FilterQuality.Low)
        } else {
            drawImage(img, dstSize = dst, alpha = m, filterQuality = FilterQuality.Low)
        }
    }
}

// ----------------------------------------------------------------------------
// Section: ui/components/ControlSettingsSection.kt
// ----------------------------------------------------------------------------

private const val PHYSICAL_INPUT_DESC =
    "Tidak perlu dinyalakan atau diatur. Colok controller (gamepad) lalu main: stick kiri jalan, stick kanan kamera, " +
        "A lompat, RT pukul, LT pasang, Y inventaris, Start = Esc. Di menu stick menggerakkan kursor. " +
        "Mouse dan keyboard fisik juga langsung bisa dipakai. " +
        "Tombol layar tersembunyi sendiri saat controller atau mouse dipakai. " +
        "Kecepatan stick mengikuti sensitivitas kamera dan kursor di atas. " +
        "Bila mod controller (mis. Controlify) terdeteksi, controller langsung diserahkan ke mod."

/**
 * Tab Pengaturan > Kontrol yang diperbarui:
 * - Kartu Studio Kontrol Virtual dengan badge jumlah kontrol aktif dan aksi cepat Editor/Ekspor/Impor
 * - Kontrol saklar berikon agar lebih mudah dipindai
 * - Penggeser sensitivitas & ukuran dengan tombol penyesuaian halus (- / +) 48dp
 * - Pemilih mode mouse 2-kartu visual (Slide vs Click)
 * - Tetap terintegrasi dengan [LocalSettingsFilter] agar fitur cari pengaturan berfungsi penuh.
 */
@Composable
fun ControlSettingsSection(
    onOpenGuiEditor: () -> Unit,
    onExportLayout: (suggestedFilename: String) -> Unit,
    onImportLayout: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        ControlLayoutManager.init(context)
    }
    val currentSettings by SettingsManager.settings.collectAsState()
    val layoutButtons by ControlLayoutManager.buttons.collectAsState()
    val searching = LocalSettingsFilter.current != null

    fun update(transform: (LauncherSettings) -> LauncherSettings) {
        SettingsManager.updateSettings(context, transform(SettingsManager.settings.value))
    }

    // 1. Studio Kontrol Virtual (Hero Card)
    ControlGroup(label = "Tata letak kontrol", searching = searching) {
        SettingFrame(
            title = "Kustomisasi kontrol virtual",
            desc = "Ubah tata letak tombol, ukuran, transparansi, scroll hotbar, dan pemetaan tombol Minecraft."
        ) {
            val activeCount = layoutButtons.size
            val joystickCount = remember(layoutButtons) { layoutButtons.count { it.isJoystick } }
            val heroShape = RoundedCornerShape(20.dp)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clip(heroShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                AmberUi.Accent.copy(alpha = 0.22f),
                                AmberUi.ObsidianSlate.copy(alpha = 0.85f)
                            )
                        ),
                        heroShape
                    )
                    .border(1.dp, AmberUi.AccentEdge.copy(alpha = 0.45f), heroShape)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(AmberUi.Accent.copy(alpha = 0.25f))
                            .border(1.dp, AmberUi.AccentEdge.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Tune,
                            contentDescription = null,
                            tint = AmberUi.AccentSoft,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Kustomisasi kontrol virtual",
                            color = AmberUi.Text,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Ubah tata letak tombol, ukuran, transparansi, scroll hotbar, dan pemetaan tombol Minecraft.",
                            color = AmberUi.TextDim,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // Ringkasan jumlah kontrol aktif
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ControlStatChip(
                        icon = Icons.Outlined.SportsEsports,
                        label = "$activeCount tombol"
                    )
                    if (joystickCount > 0) {
                        ControlStatChip(
                            icon = Icons.Outlined.Gamepad,
                            label = "$joystickCount joystick"
                        )
                    }
                }

                // Tombol utama Buka editor & Ekspor/Impor responsif
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val stackButtons = maxWidth < 340.dp
                    if (stackButtons) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AmberPillButton(
                                text = "Buka editor",
                                icon = Icons.Outlined.Edit,
                                onClick = onOpenGuiEditor,
                                filled = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("settings_open_gui_editor")
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AmberPillButton(
                                    text = "Ekspor",
                                    icon = Icons.Outlined.FileUpload,
                                    onClick = {
                                        val stamp = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
                                        onExportLayout("novacraft-controls-$stamp.json")
                                    },
                                    filled = false,
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 44.dp)
                                        .testTag("settings_export_controls")
                                )
                                AmberPillButton(
                                    text = "Impor",
                                    icon = Icons.Outlined.FileDownload,
                                    onClick = onImportLayout,
                                    filled = false,
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 44.dp)
                                        .testTag("settings_import_controls")
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AmberPillButton(
                                text = "Buka editor",
                                icon = Icons.Outlined.Edit,
                                onClick = onOpenGuiEditor,
                                filled = true,
                                modifier = Modifier
                                    .weight(1.2f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("settings_open_gui_editor")
                            )
                            AmberPillButton(
                                text = "Ekspor",
                                icon = Icons.Outlined.FileUpload,
                                onClick = {
                                    val stamp = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
                                    onExportLayout("novacraft-controls-$stamp.json")
                                },
                                filled = false,
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 44.dp)
                                    .testTag("settings_export_controls")
                            )
                            AmberPillButton(
                                text = "Impor",
                                icon = Icons.Outlined.FileDownload,
                                onClick = onImportLayout,
                                filled = false,
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 44.dp)
                                    .testTag("settings_import_controls")
                            )
                        }
                    }
                }

                Text(
                    text = "Simpan tata letak tombol ke berkas .json untuk dicadangkan atau dibagikan, atau muat dari berkas milik orang lain.",
                    color = AmberUi.TextDim,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }

    // 1b. Tuỳ chỉnh bộ điều khiển
    var showResetControlsConfirm by remember { mutableStateOf(false) }
    ControlGroup(label = "Tuỳ chỉnh bộ điều khiển", searching = searching) {
        ControlIconActionRow(
            icon = Icons.Outlined.DeleteOutline,
            title = "Thiết đặt lại cấu hình điều khiển",
            desc = "Cho phép bạn thay đổi các cấu hình tay cầm.",
            onClick = { showResetControlsConfirm = true },
            modifier = Modifier.testTag("settings_reset_controls")
        )
        SettingDivider()
        ControlAdjustSliderRow(
            icon = Icons.Outlined.SportsEsports,
            title = "Độ rộng vùng chết cần điều khiển",
            desc = "Tăng nếu cần điều khiển bị lệch.",
            value = currentSettings.controllerDeadzonePercent,
            min = 50,
            max = 200,
            step = 10,
            unit = " %",
            onCommit = { v -> update { it.copy(controllerDeadzonePercent = v) } }
        )
        SettingDivider()
        ControlAdjustSliderRow(
            icon = Icons.Outlined.OpenInFull,
            title = "Độ rộng nút điều khiển",
            desc = "Tăng độ rộng nếu nút quá nhỏ.",
            value = currentSettings.controlButtonWidthPercent,
            min = 50,
            max = 200,
            step = 10,
            unit = " %",
            onCommit = { v -> update { it.copy(controlButtonWidthPercent = v) } }
        )
        SettingDivider()
        ControlAdjustSliderRow(
            icon = Icons.Outlined.Layers,
            title = "Buttons transparency",
            desc = "Changes on-screen buttons transparency",
            value = currentSettings.controlButtonOpacityPercent,
            min = 10,
            max = 100,
            step = 5,
            unit = " %",
            onCommit = { v -> update { it.copy(controlButtonOpacityPercent = v) } }
        )
        SettingDivider()
        ControlIconSwitchRow(
            icon = Icons.Outlined.Edit,
            title = "Chỉ được dùng những chữ cái in hoa trong các nhãn nút",
            desc = "Tắt đi nếu bạn muốn sử dụng chữ cái thường trong mục điều khiển.",
            checked = currentSettings.uppercaseButtonLabels,
            onCheckedChange = { on -> update { it.copy(uppercaseButtonLabels = on) } }
        )
    }

    if (showResetControlsConfirm) {
        NuxDialog(onDismissRequest = { showResetControlsConfirm = false }) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Thiết đặt lại cấu hình điều khiển",
                    color = AmberUi.Text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Cho phép bạn thay đổi các cấu hình tay cầm và khôi phục bố cục điều khiển về mặc định.",
                    color = AmberUi.TextDim,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    AmberPillButton(
                        text = "Hủy",
                        onClick = { showResetControlsConfirm = false },
                        filled = false
                    )
                    AmberPillButton(
                        text = "Đặt lại",
                        onClick = {
                            ControlLayoutManager.resetToDefaults(context)
                            SettingsManager.resetControlSettings(context)
                            showResetControlsConfirm = false
                            Toast.makeText(context, "Đã đặt lại cấu hình điều khiển về mặc định", Toast.LENGTH_SHORT).show()
                        },
                        filled = true,
                        danger = true
                    )
                }
            }
        }
    }

    // 2. Kontrol Sentuh & Cử chỉ
    ControlGroup(label = "NHỮNG CỬ CHỈ", searching = searching) {
        ControlIconSwitchRow(
            icon = Icons.Outlined.TouchApp,
            title = "Tắt cử chỉ",
            desc = "Tắt cử chỉ, ví dụ như giữ để phá khối, và chạm để đặt khối.",
            checked = currentSettings.disableGestures,
            onCheckedChange = { on -> update { it.copy(disableGestures = on) } }
        )
        SettingDivider()
        ControlIconSwitchRow(
            icon = Icons.Outlined.SwapHoriz,
            title = "Tắt nhấn đúp để đổi tay",
            desc = "Tắt việc nhấn đúp vào vật phẩm trên thanh công cụ để đổi nó vào tay phụ.",
            checked = currentSettings.disableDoubleTapOffhand,
            onCheckedChange = { on -> update { it.copy(disableDoubleTapOffhand = on) } }
        )
        SettingDivider()
        ControlAdjustSliderRow(
            icon = Icons.Outlined.Timer,
            title = "Kích hoạt độ trễ chạm cao",
            desc = "Thay đổi khoảng thời gian chạm giữ để phá khối và ném đồ vật.",
            value = currentSettings.longPressDelayMs,
            min = 100,
            max = 1000,
            step = 50,
            unit = " ms",
            onCommit = { v -> update { it.copy(longPressDelayMs = v) } }
        )
        SettingDivider()
        ControlIconSwitchRow(
            icon = Icons.Outlined.TouchApp,
            title = "Pukul lewat layar",
            desc = "Ketuk atau tahan di area kosong layar untuk memukul. Matikan supaya menggeser kamera tidak ikut memukul; pakai tombol pukul.",
            checked = currentSettings.touchAttackEnabled,
            onCheckedChange = { on -> update { it.copy(touchAttackEnabled = on) } }
        )
        SettingDivider()
        ControlIconSwitchRow(
            icon = Icons.Outlined.ViewCarousel,
            title = "Hotbar sentuh",
            desc = "Ketuk hotbar di bagian bawah layar untuk memilih slot, seperti di Minecraft Bedrock. Posisi slot mengikuti GUI scale game.",
            checked = currentSettings.hotbarTouchEnabled,
            onCheckedChange = { on -> update { it.copy(hotbarTouchEnabled = on) } }
        )
        SettingDivider()
        ControlIconSwitchRow(
            icon = Icons.Outlined.ScreenRotation,
            title = "Geser kamera dari tombol",
            desc = "Menahan tombol pukul, pakai, atau lompat lalu menggeser jari ikut memutar kamera. Matikan supaya pandangan tidak bergeser saat bertarung (PvP).",
            checked = currentSettings.buttonLookDrag,
            onCheckedChange = { on -> update { it.copy(buttonLookDrag = on) } }
        )
        SettingDivider()
        ControlIconSwitchRow(
            icon = Icons.Outlined.Gamepad,
            title = "Dukungan TouchController",
            desc = "Eksperimental. Bila mod TouchController terpasang dan tersambung, sentuhan saat bermain juga diteruskan ke mod. Tombol dan mouse virtual launcher tetap aktif, jadi keduanya bisa dipakai bersamaan.",
            checked = currentSettings.touchControllerEnabled,
            onCheckedChange = { on -> update { it.copy(touchControllerEnabled = on) } }
        )
    }

    // 2b. KEYBOARD SETTINGS
    ControlGroup(label = "KEYBOARD SETTINGS", searching = searching) {
        ControlIconSwitchRow(
            icon = Icons.Outlined.Keyboard,
            title = "Enable keyboard panning",
            desc = "Allows on-screen keyboard to move the game window instead of overlapping it if the last touch was below the visible zone. Has no effect on custom control layout buttons with the same function",
            checked = currentSettings.enableKeyboardPanning,
            onCheckedChange = { on -> update { it.copy(enableKeyboardPanning = on) } }
        )
        SettingDivider()
        ControlIconSwitchRow(
            icon = Icons.Outlined.Keyboard,
            title = "Preview ketikan",
            desc = "Saat keyboard layar terbuka, huruf yang kamu ketik tampil di bar kecil tepat di atas keyboard supaya bisa diperiksa sebelum dikirim. Hanya menampilkan yang kamu ketik sejak keyboard dibuka.",
            checked = currentSettings.typingPreview,
            onCheckedChange = { on -> update { it.copy(typingPreview = on) } }
        )
    }

    // 2c. ĐIỀU KHIỂN CON QUAY HỒI CHUYỂN
    ControlGroup(label = "ĐIỀU KHIỂN CON QUAY HỒI CHUYỂN", searching = searching) {
        ControlIconSwitchRow(
            icon = Icons.Outlined.ScreenRotation,
            title = "Điều khiển con quay hồi chuyển",
            desc = "Bật xoay góc nhìn trong game bằng cảm biến con quay hồi chuyển trên thiết bị.",
            checked = currentSettings.gyroEnabled,
            onCheckedChange = { on -> update { it.copy(gyroEnabled = on) } }
        )
        SettingDivider()
        ControlAdjustSliderRow(
            icon = Icons.Outlined.Speed,
            title = "Độ nhạy con quay hồi chuyển",
            desc = "Điều chỉnh tốc độ xoay camera khi nghiêng thiết bị.",
            value = currentSettings.gyroSensitivityPercent,
            min = 20,
            max = 300,
            step = 10,
            unit = " %",
            onCommit = { v -> update { it.copy(gyroSensitivityPercent = v) } }
        )
    }

    // 3. CHUỘT ẢO & Kamera
    ControlGroup(label = "CHUỘT ẢO", searching = searching) {
        ControlAdjustSliderRow(
            icon = Icons.Outlined.Cameraswitch,
            title = "Sensitivitas kamera",
            desc = "Kecepatan memutar pandangan di dalam game.",
            value = currentSettings.captureSensitivity,
            min = 50,
            max = 300,
            step = 10,
            unit = "%",
            onCommit = { v -> update { it.copy(captureSensitivity = v) } }
        )
        SettingDivider()
        ControlMouseModeRow(
            selected = currentSettings.mouseControlMode,
            onSelect = { mode -> update { it.copy(mouseControlMode = mode) } }
        )
        SettingDivider()
        ControlAdjustSliderRow(
            icon = Icons.Outlined.Mouse,
            title = "Tốc độ chuột",
            desc = "Thay đổi tốc độ của chuột ảo.",
            value = currentSettings.cursorSensitivity,
            min = 50,
            max = 300,
            step = 10,
            unit = " %",
            onCommit = { v -> update { it.copy(cursorSensitivity = v) } }
        )
        SettingDivider()
        ControlIconSwitchRow(
            icon = Icons.Outlined.AdsClick,
            title = "Khởi động với chuột ảo bật",
            desc = "Chuột ảo xuất hiện khi khởi động khi cài đặt này được mở.",
            checked = currentSettings.startWithVirtualMouse,
            onCheckedChange = { on -> update { it.copy(startWithVirtualMouse = on) } }
        )
        SettingDivider()
        ControlAdjustSliderRow(
            icon = Icons.Outlined.OpenInFull,
            title = "Độ lớn của chuột",
            desc = "Thay đổi kích thước của chuột ảo.",
            value = currentSettings.virtualMouseSizePercent,
            min = 50,
            max = 200,
            step = 10,
            unit = " %",
            onCommit = { v -> update { it.copy(virtualMouseSizePercent = v) } }
        )
        SettingDivider()
        ControlAdjustSliderRow(
            icon = Icons.Outlined.OpenInFull,
            title = "Ukuran kursor virtual",
            desc = null,
            value = currentSettings.mouseSizeDp,
            min = 16,
            max = 48,
            step = 2,
            unit = "dp",
            onCommit = { v -> update { it.copy(mouseSizeDp = v) } }
        )
    }

    // 4. Penunjuk Kursor
    ControlGroup(label = "Penunjuk kursor", searching = searching) {
        PointerSettingsCards(
            hotspots = currentSettings.pointerHotspots,
            onHotspotsChange = { map -> update { it.copy(pointerHotspots = map) } }
        )
    }

    // 5. Mouse Fisik
    ControlGroup(label = "Mouse fisik", searching = searching) {
        ControlIconSwitchRow(
            icon = Icons.Outlined.Mouse,
            title = "Pakai kursor sistem Android",
            desc = "Mati (bawaan): mouse menggerakkan kursor launcher di menu game, jadi gambar, ukuran, dan hotspot " +
                "kursor di atas berlaku. Nyalakan bila mouse lebih nyaman dengan penunjuk bawaan Android.",
            checked = currentSettings.physicalMouseSystemPointer,
            onCheckedChange = { on -> update { it.copy(physicalMouseSystemPointer = on) } }
        )
        SettingDivider()
        ControlAdjustSliderRow(
            icon = Icons.Outlined.Speed,
            title = "Kecepatan kursor mouse",
            desc = "Pengali di atas sensitivitas kursor virtual, hanya untuk mouse fisik di menu game. 100% = sama.",
            value = currentSettings.physicalMouseCursorSpeed,
            min = 20,
            max = 300,
            step = 10,
            unit = "%",
            onCommit = { v -> update { it.copy(physicalMouseCursorSpeed = v) } }
        )
        SettingDivider()
        ControlAdjustSliderRow(
            icon = Icons.Outlined.Cameraswitch,
            title = "Kecepatan kamera mouse",
            desc = "Pengali di atas sensitivitas kamera, hanya untuk mouse fisik saat bermain. 100% = sama.",
            value = currentSettings.physicalMouseCameraSpeed,
            min = 20,
            max = 300,
            step = 10,
            unit = "%",
            onCommit = { v -> update { it.copy(physicalMouseCameraSpeed = v) } }
        )
    }

    // 6. Controller & Mouse Fisik
    ControlGroup(label = "Controller dan mouse fisik", searching = searching) {
        SettingFrame("Langsung bisa dipakai", PHYSICAL_INPUT_DESC) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ControlStatChip(icon = Icons.Outlined.SportsEsports, label = "Gamepad Plug & Play")
                    ControlStatChip(icon = Icons.Outlined.Mouse, label = "Mouse & Keyboard")
                }
                SettingRowText(
                    title = "Langsung bisa dipakai",
                    desc = PHYSICAL_INPUT_DESC
                )
            }
        }
    }
}

@Composable
private fun ControlIconActionRow(
    icon: ImageVector,
    title: String,
    desc: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingFrame(title, desc) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .clickable(onClick = onClick)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(AmberUi.ObsidianSlate.copy(alpha = 0.7f))
                    .border(1.dp, AmberUi.Border, RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AmberUi.TextDim,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            SettingRowText(title = title, desc = desc, modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = AmberUi.TextDim,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ControlGroup(
    label: String,
    searching: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    if (searching) {
        Column(content = content)
    } else {
        AmberSectionLabel(label)
        AmberCard(content = content)
    }
}

@Composable
private fun ControlStatChip(icon: ImageVector, label: String) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(AmberUi.ObsidianDeep.copy(alpha = 0.6f), CircleShape)
            .border(1.dp, AmberUi.BorderStrong, CircleShape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AmberUi.AccentSoft,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            color = AmberUi.Text,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ControlIconSwitchRow(
    icon: ImageVector,
    title: String,
    desc: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    SettingFrame(title, desc) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .clickable { onCheckedChange(!checked) }
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(
                        if (checked) AmberUi.Accent.copy(alpha = 0.22f)
                        else AmberUi.ObsidianSlate.copy(alpha = 0.7f)
                    )
                    .border(
                        1.dp,
                        if (checked) AmberUi.AccentEdge.copy(alpha = 0.55f) else AmberUi.Border,
                        RoundedCornerShape(11.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (checked) AmberUi.AccentSoft else AmberUi.TextDim,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            SettingRowText(title = title, desc = desc, modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.focusProperties { canFocus = false },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AmberUi.OnAccent,
                    checkedTrackColor = AmberUi.Accent,
                    checkedBorderColor = AmberUi.AccentEdge,
                    uncheckedThumbColor = if (AmberUi.isDark) AmberUi.TextDim else Color.White,
                    uncheckedTrackColor = if (AmberUi.isDark) AmberUi.SurfaceRaised else Color(0xFFCBD5E1),
                    uncheckedBorderColor = AmberUi.BorderStrong
                )
            )
        }
    }
}

@Composable
private fun ControlAdjustSliderRow(
    icon: ImageVector,
    title: String,
    desc: String?,
    value: Int,
    min: Int,
    max: Int,
    step: Int,
    unit: String,
    onCommit: (Int) -> Unit
) {
    SettingFrame(title, desc) {
        var live by remember(value) { mutableFloatStateOf(value.coerceIn(min, max).toFloat()) }
        val shownInt = ((live / step).roundToInt() * step).coerceIn(min, max)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AmberUi.ObsidianSlate.copy(alpha = 0.8f))
                        .border(1.dp, AmberUi.Border, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = AmberUi.AccentSoft,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                SettingRowText(title = title, desc = desc, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(AmberUi.Accent.copy(alpha = 0.22f), CircleShape)
                        .border(1.dp, AmberUi.AccentEdge.copy(alpha = 0.6f), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$shownInt$unit",
                        color = AmberUi.Text,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ControlStepperButton(
                    icon = Icons.Outlined.Remove,
                    description = "Kurangi $title",
                    enabled = shownInt > min,
                    onClick = {
                        val next = (shownInt - step).coerceIn(min, max)
                        live = next.toFloat()
                        onCommit(next)
                    }
                )
                Slider(
                    value = live,
                    onValueChange = { live = it },
                    onValueChangeFinished = {
                        val snapped = ((live / step).roundToInt() * step).coerceIn(min, max)
                        live = snapped.toFloat()
                        onCommit(snapped)
                    },
                    valueRange = min.toFloat()..max.toFloat(),
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = if (AmberUi.isDark) Color.White else AmberUi.Accent,
                        activeTrackColor = AmberUi.AccentSoft,
                        inactiveTrackColor = AmberUi.BorderStrong,
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent
                    )
                )
                ControlStepperButton(
                    icon = Icons.Outlined.Add,
                    description = "Tambah $title",
                    enabled = shownInt < max,
                    onClick = {
                        val next = (shownInt + step).coerceIn(min, max)
                        live = next.toFloat()
                        onCommit(next)
                    }
                )
            }
        }
    }
}

@Composable
private fun ControlStepperButton(
    icon: ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
            .clip(CircleShape)
            .background(AmberUi.ObsidianSlate.copy(alpha = 0.85f), CircleShape)
            .border(1.dp, if (enabled) AmberUi.BorderStrong else AmberUi.Border, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (enabled) AmberUi.Text else AmberUi.TextDim.copy(alpha = 0.35f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun ControlMouseModeRow(
    selected: String,
    onSelect: (String) -> Unit
) {
    val desc = if (selected == "CLICK") {
        "Sentuh tombol atau slot menu langsung di mana jari menekan."
    } else {
        "Geser jari untuk menggerakkan kursor virtual, ketuk layar untuk klik."
    }
    SettingFrame("Mode mouse di menu", desc) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SettingRowText(title = "Mode mouse di menu", desc = desc)
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val vertical = maxWidth < 340.dp
                val modes = listOf(
                    Triple("SLIDE", "Slide (trackpad)", Icons.Outlined.TouchApp),
                    Triple("CLICK", "Click (langsung)", Icons.Outlined.AdsClick)
                )
                if (vertical) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        modes.forEach { (id, label, icon) ->
                            MouseModeCard(
                                label = label,
                                icon = icon,
                                selected = selected == id,
                                onClick = { onSelect(id) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        modes.forEach { (id, label, icon) ->
                            MouseModeCard(
                                label = label,
                                icon = icon,
                                selected = selected == id,
                                onClick = { onSelect(id) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MouseModeCard(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 52.dp)
            .clip(shape)
            .background(
                if (selected) AmberUi.Accent.copy(alpha = 0.24f) else AmberUi.ObsidianSlate.copy(alpha = 0.6f),
                shape
            )
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) AmberUi.AccentEdge else AmberUi.Border,
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) AmberUi.AccentSoft else AmberUi.TextDim,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            color = if (selected) AmberUi.Text else AmberUi.TextDim,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = AmberUi.AccentSoft,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ----------------------------------------------------------------------------
// Section: ui/components/GameLoadingOverlay.kt
// ----------------------------------------------------------------------------

private object ForgePalette {
    val BgTop = Color(0xFF040814)
    val BgMid = Color(0xFF081226)
    val BgBottom = Color(0xFF050A16)
    val Card = Color(0xD90B152C)
    val CardElevated = Color(0xEB101D38)
    val CardBorder = Color(0xFF1E345A)
    val Grid = Color(0xFF142A4F)
    val Cyan = Amber.Yellow
    val CyanSoft = Amber.YellowSoft
    val CyanDeep = Color(0xFF0891B2)
    val Gold = Color(0xFFF59E0B)
    val GoldSoft = Color(0xFFFCD34D)
    val Emerald = Amber.Good
    val EmeraldSoft = Color(0xFF6EE7B7)
    val TextPrimary = Amber.Text
    val TextMuted = Amber.TextDim
    val TextDim = Amber.TextFaint
}

private data class JavaBootStage(
    val index: Int,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val progress: Float
)

private val JAVA_BOOT_STAGES = listOf(
    JavaBootStage(
        index = 0,
        title = "OpenJDK & JVM",
        subtitle = "Menyiapkan runtime Java, jalur memori & konfigurasi",
        icon = Icons.Outlined.Memory,
        progress = 0.24f
    ),
    JavaBootStage(
        index = 1,
        title = "Surface & Renderer",
        subtitle = "Mengikat jendela native, shader cache & pustaka grafis",
        icon = Icons.Outlined.Layers,
        progress = 0.48f
    ),
    JavaBootStage(
        index = 2,
        title = "Loader & Kelas Game",
        subtitle = "Memulai VMLauncher, LWJGL & inisialisasi mod",
        icon = Icons.Outlined.ViewInAr,
        progress = 0.74f
    ),
    JavaBootStage(
        index = 3,
        title = "Aset & Frame Awal",
        subtitle = "Memuat ResourceManager, OpenAL & layar Mojang",
        icon = Icons.Outlined.Bolt,
        progress = 0.93f
    )
)

private val JAVA_BOOT_TIPS = listOf(
    "Ketuk ikon gerigi atau angka FPS di pojok layar kapan saja untuk mengatur sensitivitas, resolusi, atau membuka log.",
    "Pembukaan pertama setelah memasang mod atau mengganti renderer bisa memakan waktu lebih lama untuk kompilasi shader.",
    "Gunakan tombol Log di kanan atas bila ingin memantau proses muat JVM dan mod secara langsung.",
    "Mode Kontrol Sentuh, Hotbar Langsung, dan Gamepad Plug & Play dapat disesuaikan langsung dari menu dalam game.",
    "Jika game macet lebih dari 60 detik, tombol kontrol akan otomatis muncul agar Anda bisa memeriksa log atau keluar dengan aman."
)

/**
 * Layar tunggu khusus Minecraft Java Edition dengan estetika NovaCraft Obsidian Forge.
 *
 * Dirancang proporsional dan sejajar di semua rasio layar (ponsel landscape, notch, split-screen, tablet)
 * serta tetap ringan CPU selama JVM dan modpack menyala (tick bertahap 450 ms tanpa animasi 60fps).
 */
@Composable
fun GameLoadingOverlay(
    visible: Boolean,
    instanceName: String,
    latestLog: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    showClose: Boolean = true,
    mcVersion: String = "",
    loader: String = "vanilla",
    loaderVersion: String? = null,
    runtimeName: String = "",
    rendererName: String = "",
    logs: List<String> = emptyList()
) {
    if (!visible) return

    var step by remember { mutableIntStateOf(0) }
    var elapsedSec by remember { mutableIntStateOf(0) }
    var showLogConsole by remember { mutableIntStateOf(0) } // 0 = Pipeline, 1 = Live Log
    var maxStage by remember { mutableIntStateOf(0) }

    val detectedStage = remember(latestLog) { detectBootStage(latestLog) }
    if (detectedStage > maxStage) {
        maxStage = detectedStage
    }
    val activeStage = JAVA_BOOT_STAGES[maxStage.coerceIn(0, JAVA_BOOT_STAGES.lastIndex)]
    val progressFraction = remember(maxStage, elapsedSec) {
        val base = activeStage.progress
        val micro = ((elapsedSec % 18) / 18f) * 0.05f
        (base + micro).coerceIn(0.14f, 0.96f)
    }

    LaunchedEffect(visible) {
        var accMs = 0L
        while (isActive) {
            delay(450L)
            step = (step + 1) % 16
            accMs += 450L
            if (accMs >= 1000L) {
                elapsedSec += (accMs / 1000L).toInt()
                accMs %= 1000L
            }
        }
    }

    val elapsedText = remember(elapsedSec) {
        val mins = elapsedSec / 60
        val secs = elapsedSec % 60
        "%02d:%02d".format(mins, secs)
    }
    val activeTip = remember(elapsedSec / 6) {
        JAVA_BOOT_TIPS[(elapsedSec / 6) % JAVA_BOOT_TIPS.size]
    }

    val cleanLoader = remember(loader, loaderVersion) {
        val base = when (loader.lowercase()) {
            "fabric" -> "Fabric"
            "forge" -> "Forge"
            "neoforge" -> "NeoForge"
            "quilt" -> "Quilt"
            "vanilla", "" -> "Vanilla"
            else -> loader.replaceFirstChar { it.uppercase() }
        }
        if (!loaderVersion.isNullOrBlank() && !loader.equals("vanilla", ignoreCase = true)) {
            "$base $loaderVersion"
        } else {
            base
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            // Blokir sentuhan agar tidak bocor ke Surface/Touchpad Minecraft di belakang overlay
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    }
                }
            }
            .drawBehind {
                drawRect(
                    Brush.verticalGradient(
                        0f to ForgePalette.BgTop,
                        0.55f to ForgePalette.BgMid,
                        1f to ForgePalette.BgBottom
                    )
                )
                val gridStep = 36.dp.toPx()
                var gx = gridStep
                while (gx < size.width) {
                    drawLine(
                        color = ForgePalette.Grid.copy(alpha = 0.22f),
                        start = Offset(gx, 0f),
                        end = Offset(gx, size.height),
                        strokeWidth = 1f
                    )
                    gx += gridStep
                }
                var gy = gridStep
                while (gy < size.height) {
                    drawLine(
                        color = ForgePalette.Grid.copy(alpha = 0.22f),
                        start = Offset(0f, gy),
                        end = Offset(size.width, gy),
                        strokeWidth = 1f
                    )
                    gy += gridStep
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(ForgePalette.Cyan.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(size.width * 0.18f, size.height * 0.24f),
                        radius = size.maxDimension * 0.48f
                    ),
                    center = Offset(size.width * 0.18f, size.height * 0.24f),
                    radius = size.maxDimension * 0.48f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(ForgePalette.Gold.copy(alpha = 0.10f), Color.Transparent),
                        center = Offset(size.width * 0.86f, size.height * 0.84f),
                        radius = size.maxDimension * 0.42f
                    ),
                    center = Offset(size.width * 0.86f, size.height * 0.84f),
                    radius = size.maxDimension * 0.42f
                )
            }
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 1040.dp)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            val availableWidth = maxWidth
            val availableHeight = maxHeight
            val wideLayout = availableWidth >= 560.dp
            val compactHeight = availableHeight < 440.dp
            val veryCompactHeight = availableHeight < 340.dp

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(if (veryCompactHeight) 8.dp else 12.dp)
            ) {
                // =========================================================
                // 1. HEADER BAR: Chip Info + Timer + Toggle Log + Tombol Batal
                // =========================================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ForgeBadge(
                            text = "NOVACRAFT × JAVA",
                            accent = ForgePalette.Cyan,
                            filled = true
                        )
                        if (mcVersion.isNotBlank() && mcVersion != "Unknown") {
                            ForgeBadge(
                                text = "MC $mcVersion",
                                accent = ForgePalette.GoldSoft
                            )
                        }
                        ForgeBadge(
                            text = cleanLoader,
                            accent = ForgePalette.EmeraldSoft
                        )
                        if (runtimeName.isNotBlank() && wideLayout) {
                            ForgeBadge(
                                text = runtimeName.uppercase(),
                                accent = ForgePalette.TextMuted
                            )
                        }
                        if (rendererName.isNotBlank() && availableWidth >= 680.dp) {
                            ForgeBadge(
                                text = rendererName,
                                accent = ForgePalette.CyanSoft
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .defaultMinSize(minHeight = 40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ForgePalette.Card)
                                .border(1.dp, ForgePalette.CardBorder, RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            RawText(
                                text = elapsedText,
                                color = ForgePalette.CyanSoft,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        val logOpen = showLogConsole == 1
                        Row(
                            modifier = Modifier
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (logOpen) ForgePalette.Cyan.copy(alpha = 0.18f)
                                    else ForgePalette.Card
                                )
                                .border(
                                    1.dp,
                                    if (logOpen) ForgePalette.Cyan.copy(alpha = 0.7f) else ForgePalette.CardBorder,
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { showLogConsole = if (logOpen) 0 else 1 }
                                .testTag("game_loading_log_toggle")
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Terminal,
                                contentDescription = "Lihat log proses muat",
                                tint = if (logOpen) ForgePalette.CyanSoft else ForgePalette.TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                            if (wideLayout) {
                                Text(
                                    text = if (logOpen) "Tahap" else "Log",
                                    color = if (logOpen) ForgePalette.CyanSoft else ForgePalette.TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (showClose) {
                            Row(
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Amber.DangerTint)
                                    .border(1.dp, Amber.Danger.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                                    .clickable(onClick = onClose)
                                    .testTag("game_loading_cancel_button")
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Batal dan keluar",
                                    tint = Amber.DangerSoft,
                                    modifier = Modifier.size(18.dp)
                                )
                                if (wideLayout) {
                                    Text(
                                        text = "Batal",
                                        color = Amber.DangerSoft,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                // =========================================================
                // 2. MIDDLE WORKSPACE: Proporsional, Sejajar & Anti-Distorsi
                // =========================================================
                if (wideLayout) {
                    val coreSize = when {
                        veryCompactHeight -> 68.dp
                        compactHeight || availableWidth < 680.dp -> 78.dp
                        else -> 90.dp
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Panel Kiri: Inti Isometrik 1:1 + Judul + Progres + Tip
                        Column(
                            modifier = Modifier
                                .weight(1.05f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                JavaVoxelCore(
                                    step = step,
                                    progress = progressFraction,
                                    modifier = Modifier.size(coreSize)
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    RawText(
                                        text = "MEMUAT MINECRAFT JAVA",
                                        color = ForgePalette.Cyan,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    RawText(
                                        text = instanceName,
                                        color = ForgePalette.TextPrimary,
                                        fontSize = if (compactHeight) 21.sp else 26.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        RawText(
                                            text = "${(progressFraction * 100).toInt()}%",
                                            color = ForgePalette.CyanSoft,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = activeStage.subtitle,
                                            color = ForgePalette.TextMuted,
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            if (!veryCompactHeight) {
                                Spacer(modifier = Modifier.height(if (compactHeight) 10.dp else 14.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(ForgePalette.Card.copy(alpha = 0.72f))
                                        .border(1.dp, ForgePalette.CardBorder.copy(alpha = 0.8f), RoundedCornerShape(14.dp))
                                        .padding(horizontal = 12.dp, vertical = 9.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Bolt,
                                            contentDescription = null,
                                            tint = ForgePalette.GoldSoft,
                                            modifier = Modifier
                                                .padding(top = 1.dp)
                                                .size(15.dp)
                                        )
                                        Text(
                                            text = activeTip,
                                            color = ForgePalette.TextMuted,
                                            fontSize = 11.5.sp,
                                            lineHeight = 16.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        // Panel Kanan: Pipeline 4 Tahap (wrapHeight terpusat & scroll-safe) atau Terminal Boot Log
                        val panelShape = RoundedCornerShape(18.dp)
                        if (showLogConsole == 1) {
                            Box(
                                modifier = Modifier
                                    .weight(0.95f)
                                    .fillMaxHeight()
                                    .widthIn(max = 440.dp)
                                    .clip(panelShape)
                                    .background(ForgePalette.CardElevated)
                                    .border(1.dp, ForgePalette.CardBorder, panelShape)
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                BootLogConsolePane(logs = logs)
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .weight(0.95f)
                                    .wrapContentHeight()
                                    .widthIn(max = 440.dp)
                                    .clip(panelShape)
                                    .background(ForgePalette.CardElevated)
                                    .border(1.dp, ForgePalette.CardBorder, panelShape)
                                    .padding(horizontal = 14.dp, vertical = if (compactHeight) 10.dp else 13.dp)
                            ) {
                                BootStagePipeline(
                                    activeIndex = maxStage,
                                    step = step,
                                    compact = compactHeight
                                )
                            }
                        }
                    }
                } else {
                    // Tata letak sempit / split-screen / portrait: selalu terpusat & dapat digulir
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (showLogConsole == 1) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .widthIn(max = 520.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(ForgePalette.CardElevated)
                                    .border(1.dp, ForgePalette.CardBorder, RoundedCornerShape(18.dp))
                                    .padding(12.dp)
                            ) {
                                BootLogConsolePane(logs = logs)
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 520.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                JavaVoxelCore(
                                    step = step,
                                    progress = progressFraction,
                                    modifier = Modifier.size(if (compactHeight) 74.dp else 86.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                RawText(
                                    text = instanceName,
                                    color = ForgePalette.TextPrimary,
                                    fontSize = 21.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${activeStage.title} · ${(progressFraction * 100).toInt()}%",
                                    color = ForgePalette.CyanSoft,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(ForgePalette.CardElevated)
                                        .border(1.dp, ForgePalette.CardBorder, RoundedCornerShape(16.dp))
                                        .padding(12.dp)
                                ) {
                                    BootStagePipeline(
                                        activeIndex = maxStage,
                                        step = step,
                                        compact = true
                                    )
                                }
                            }
                        }
                    }
                }

                // =========================================================
                // 3. BOTTOM TELEMETRY BAR: Progress + Baris Log Terakhir
                // =========================================================
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (step % 2 == 0) ForgePalette.Cyan else ForgePalette.Emerald
                                    )
                            )
                            RawText(
                                text = sanitizeBootLine(latestLog).ifBlank {
                                    "Menyiapkan mesin Java dan antarmuka grafis..."
                                },
                                color = ForgePalette.TextMuted,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (elapsedSec >= 25) {
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (elapsedSec >= 60) "Kontrol darurat aktif" else "Memuat aset/mod...",
                                color = ForgePalette.GoldSoft,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }

                    ForgeProgressBar(
                        progress = progressFraction,
                        step = step,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun JavaVoxelCore(
    step: Int,
    progress: Float,
    modifier: Modifier = Modifier
) {
    // Jaga rasio tepat 1:1 agar cincin orbit & kubus isometrik tidak pernah gepeng atau lonjong
    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            val ringRadius = minOf(w, h) * 0.43f

            drawCircle(
                color = ForgePalette.CardBorder.copy(alpha = 0.7f),
                radius = ringRadius,
                center = Offset(cx, cy),
                style = Stroke(width = 2.dp.toPx())
            )

            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        ForgePalette.CyanDeep,
                        ForgePalette.Cyan,
                        ForgePalette.CyanSoft,
                        ForgePalette.CyanDeep
                    )
                ),
                startAngle = -90f + (step * 22.5f),
                sweepAngle = (progress * 300f).coerceIn(50f, 320f),
                useCenter = false,
                topLeft = Offset(cx - ringRadius, cy - ringRadius),
                size = Size(ringRadius * 2f, ringRadius * 2f),
                style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
            )

            val orbitAngleRad = Math.toRadians((-90.0 + step * 22.5))
            val orbX = cx + (ringRadius * cos(orbitAngleRad)).toFloat()
            val orbY = cy + (ringRadius * sin(orbitAngleRad)).toFloat()
            drawCircle(
                color = ForgePalette.GoldSoft,
                radius = 3.dp.toPx(),
                center = Offset(orbX, orbY)
            )

            val r = minOf(w, h) * 0.24f
            val dx = r * 0.866f
            val dy = r * 0.5f

            val topCenter = Offset(cx, cy - r)
            val topRight = Offset(cx + dx, cy - dy)
            val centerPt = Offset(cx, cy)
            val topLeft = Offset(cx - dx, cy - dy)
            val botLeft = Offset(cx - dx, cy + dy)
            val botCenter = Offset(cx, cy + r)
            val botRight = Offset(cx + dx, cy + dy)

            val topFace = Path().apply {
                moveTo(topCenter.x, topCenter.y)
                lineTo(topRight.x, topRight.y)
                lineTo(centerPt.x, centerPt.y)
                lineTo(topLeft.x, topLeft.y)
                close()
            }
            val leftFace = Path().apply {
                moveTo(topLeft.x, topLeft.y)
                lineTo(centerPt.x, centerPt.y)
                lineTo(botCenter.x, botCenter.y)
                lineTo(botLeft.x, botLeft.y)
                close()
            }
            val rightFace = Path().apply {
                moveTo(centerPt.x, centerPt.y)
                lineTo(topRight.x, topRight.y)
                lineTo(botRight.x, botRight.y)
                lineTo(botCenter.x, botCenter.y)
                close()
            }

            drawPath(
                topFace,
                brush = Brush.linearGradient(
                    listOf(Color(0xFF164E63), Color(0xFF0E7490))
                )
            )
            drawPath(leftFace, color = Color(0xFF0B1F3A))
            drawPath(rightFace, color = Color(0xFF09162B))

            val wireStroke = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
            drawPath(topFace, color = ForgePalette.CyanSoft, style = wireStroke)
            drawPath(leftFace, color = ForgePalette.Cyan.copy(alpha = 0.72f), style = wireStroke)
            drawPath(rightFace, color = ForgePalette.Cyan.copy(alpha = 0.52f), style = wireStroke)
        }
    }
}

@Composable
private fun BootStagePipeline(
    activeIndex: Int,
    step: Int,
    compact: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp)
    ) {
        RawText(
            text = "TAHAP PELUNCURAN JAVA",
            color = ForgePalette.CyanSoft,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.3.sp
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 7.dp)
        ) {
            JAVA_BOOT_STAGES.forEach { stage ->
                val done = stage.index < activeIndex
                val active = stage.index == activeIndex
                val rowBg = when {
                    active -> ForgePalette.Cyan.copy(alpha = 0.12f)
                    done -> ForgePalette.Emerald.copy(alpha = 0.08f)
                    else -> Color.Transparent
                }
                val rowBorder = when {
                    active -> ForgePalette.Cyan.copy(alpha = 0.55f)
                    done -> ForgePalette.Emerald.copy(alpha = 0.35f)
                    else -> ForgePalette.CardBorder.copy(alpha = 0.5f)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(rowBg)
                        .border(1.dp, rowBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = if (compact) 6.dp else 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    done -> ForgePalette.Emerald.copy(alpha = 0.22f)
                                    active -> ForgePalette.Cyan.copy(alpha = 0.22f)
                                    else -> ForgePalette.Card
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (done) Icons.Outlined.Check else stage.icon,
                            contentDescription = null,
                            tint = when {
                                done -> ForgePalette.EmeraldSoft
                                active -> if (step % 2 == 0) ForgePalette.CyanSoft else ForgePalette.Cyan
                                else -> ForgePalette.TextDim
                            },
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stage.title,
                            color = when {
                                done -> ForgePalette.EmeraldSoft
                                active -> ForgePalette.TextPrimary
                                else -> ForgePalette.TextDim
                            },
                            fontSize = if (compact) 12.sp else 13.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!compact && active) {
                            Text(
                                text = stage.subtitle,
                                color = ForgePalette.TextMuted,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    RawText(
                        text = when {
                            done -> "OK"
                            active -> "..."
                            else -> ""
                        },
                        color = if (done) ForgePalette.EmeraldSoft else ForgePalette.CyanSoft,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.End,
                        modifier = Modifier.widthIn(min = 24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BootLogConsolePane(logs: List<String>) {
    val tail = remember(logs.size, logs.lastOrNull()) { logs.takeLast(60) }
    val listState = rememberLazyListState()
    LaunchedEffect(tail.size) {
        if (tail.isNotEmpty()) {
            listState.scrollToItem(tail.lastIndex)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RawText(
                text = "KONSOL BOOT JVM",
                color = ForgePalette.CyanSoft,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.3.sp
            )
            RawText(
                text = "${logs.size} baris",
                color = ForgePalette.TextDim,
                fontSize = 10.5.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        if (tail.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Menunggu output dari mesin Java...",
                    color = ForgePalette.TextDim,
                    fontSize = 12.sp
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                items(tail) { line ->
                    val isWarn = line.contains("warn", ignoreCase = true) ||
                        line.contains("error", ignoreCase = true) ||
                        line.contains("gagal", ignoreCase = true)
                    RawText(
                        text = line,
                        color = if (isWarn) ForgePalette.GoldSoft else ForgePalette.TextMuted,
                        fontSize = 10.5.sp,
                        lineHeight = 14.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun ForgeBadge(
    text: String,
    accent: Color,
    filled: Boolean = false
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (filled) accent.copy(alpha = 0.2f) else ForgePalette.Card)
            .border(1.dp, accent.copy(alpha = if (filled) 0.65f else 0.35f), RoundedCornerShape(10.dp))
            .padding(horizontal = 9.dp, vertical = 5.dp)
    ) {
        RawText(
            text = text,
            color = accent,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.6.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun ForgeProgressBar(
    progress: Float,
    step: Int,
    modifier: Modifier = Modifier
) {
    val clamped = progress.coerceIn(0.05f, 1f)
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(ForgePalette.Card)
            .border(1.dp, ForgePalette.CardBorder, CircleShape)
            .drawBehind {
                val fillW = size.width * clamped
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        listOf(ForgePalette.CyanDeep, ForgePalette.Cyan, ForgePalette.CyanSoft)
                    ),
                    size = Size(fillW, size.height),
                    cornerRadius = CornerRadius(size.height / 2f)
                )
                val pulseW = (fillW * 0.24f).coerceAtLeast(16.dp.toPx())
                if (fillW > pulseW) {
                    val pos = (step % 8) / 7f
                    val pulseX = (fillW - pulseW) * pos
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.42f),
                        topLeft = Offset(pulseX, 0f),
                        size = Size(pulseW, size.height),
                        cornerRadius = CornerRadius(size.height / 2f)
                    )
                }
            }
    )
}

private fun detectBootStage(rawLog: String): Int {
    if (rawLog.isBlank()) return 0
    val l = rawLog.lowercase()
    return when {
        l.contains("first graphical") ||
            l.contains("reloading resourcemanager") ||
            l.contains("openal initialized") ||
            l.contains("sound engine started") ||
            l.contains("created:") ||
            l.contains("narrator") ||
            l.contains("backend library: lwjgl") -> 3

        l.contains("vmlauncher") ||
            l.contains("launchjvm") ||
            l.contains("touchcontroller") ||
            l.contains("fabricloader") ||
            l.contains("modlauncher") ||
            l.contains("neoforge") ||
            l.contains("minecraftforge") ||
            l.contains("mixin") ||
            l.contains("setting user") ||
            l.contains("lwjgl") -> 2

        l.contains("render bridge") ||
            l.contains("pre-dlopen") ||
            l.contains("plugin") ||
            l.contains("renderer") ||
            l.contains("surfacechanged") ||
            l.contains("opengl") ||
            l.contains("vulkan") ||
            l.contains("zink") -> 1

        else -> 0
    }
}

private fun sanitizeBootLine(line: String): String {
    val trimmed = line.trim()
    if (trimmed.isEmpty()) return ""
    return trimmed.removePrefix("▷").trim()
}

// ----------------------------------------------------------------------------
// Section: ui/components/GameLogSettingsSection.kt
// ----------------------------------------------------------------------------

/**
 * Isi tab "Log game" di Pengaturan: daftar log sesi game yang tersimpan otomatis tiap keluar dari
 * Minecraft (lihat [GameLogArchive]), dengan tombol Bagikan untuk mengirimnya ke pengembang.
 */
@Composable
fun GameLogSettingsSection() {
    val context = LocalContext.current
    var logs by remember { mutableStateOf<List<File>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var reloadTick by remember { mutableIntStateOf(0) }
    var preparing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Sesi yang terputus (mis. game ditutup paksa oleh Android) disimpan dulu, lalu daftar dimuat.
    LaunchedEffect(reloadTick) {
        logs = withContext(Dispatchers.IO) {
            try {
                GameLogArchive.archiveIfNeeded(context)
            } catch (_: Throwable) {
            }
            GameLogArchive.list(context)
        }
        loaded = true
    }

    SettingRowText(
        title = "Log game",
        desc = "Setiap keluar dari Minecraft, log sesi itu disimpan otomatis (10 terakhir), tanpa token akun. " +
            "Bila ada masalah, misalnya shader tidak termuat, bagikan log terbaru ke pengembang."
    )
    SettingDivider()

    // Laporan khusus launcher: kejadian di luar game (unduhan, login, galat sebelum game dimulai)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        SettingRowText(
            title = "Log launcher",
            desc = "Kejadian di dalam launcher di luar game, misalnya unduhan, login, dan pesan galat. " +
                "Bagikan bila masalahnya muncul sebelum game dimulai.",
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        NuxButton(
            onClick = {
                if (!preparing) {
                    preparing = true
                    scope.launch {
                        val report = withContext(Dispatchers.IO) { GameLogArchive.saveLauncherReport(context) }
                        preparing = false
                        if (report != null) {
                            reloadTick += 1
                            CrashUtils.shareLogFile(context, report, "Log launcher NovaCraft", "Bagikan log launcher")
                        } else {
                            Toast.makeText(context, "Gagal menyiapkan log launcher.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            backgroundColor = Color.Transparent,
            cornerRadius = 20.dp,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp)
        ) {
            Text(
                text = if (preparing) "Menyiapkan log..." else "Bagikan",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
    SettingDivider()

    val gameLogs = logs.filterNot { GameLogArchive.isLauncherReport(it) }
    val launcherReports = logs.filter { GameLogArchive.isLauncherReport(it) }

    if (loaded && gameLogs.isEmpty()) {
        Text(
            text = "Belum ada log. Mainkan game sekali lalu keluar; log tersimpan otomatis.",
            color = NuxColors.GrayNeutral,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(vertical = 9.dp)
        )
    }

    gameLogs.forEachIndexed { index, file ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SettingRowText(
                title = if (index == 0) "Log terbaru" else "Log sebelumnya",
                desc = summary(file),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            NuxButton(
                onClick = {
                    CrashUtils.shareLogFile(context, file, "Log game NovaCraft", "Bagikan log game")
                },
                backgroundColor = if (index == 0) AmberUi.Accent else Color.Transparent,
                cornerRadius = 20.dp,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "Bagikan",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
        SettingDivider()
    }

    launcherReports.forEach { file ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SettingRowText(
                title = "Log launcher",
                desc = launcherSummary(file),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            NuxButton(
                onClick = {
                    CrashUtils.shareLogFile(context, file, "Log launcher NovaCraft", "Bagikan log launcher")
                },
                backgroundColor = Color.Transparent,
                cornerRadius = 20.dp,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "Bagikan",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
        SettingDivider()
    }

    if (logs.isNotEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SettingRowText(
                title = "Hapus semua log",
                desc = null,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            NuxButton(
                onClick = {
                    if (!confirmClear) {
                        confirmClear = true
                    } else {
                        GameLogArchive.deleteAll(context)
                        confirmClear = false
                        reloadTick += 1
                        Toast.makeText(context, "Log dihapus.", Toast.LENGTH_SHORT).show()
                    }
                },
                backgroundColor = Color.Transparent,
                cornerRadius = 20.dp,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (confirmClear) "Ketuk lagi untuk menghapus" else "Hapus",
                    color = if (confirmClear) NuxColors.ErrorRed else Color.Unspecified,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    Text(
        text = "Lokasi: " + GameLogArchive.logsDir(context).absolutePath,
        color = NuxColors.GrayNeutral,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        modifier = Modifier.padding(top = 6.dp)
    )
    Text(
        text = "Kirim lewat WhatsApp, Telegram, atau Google Drive dari tombol Bagikan, atau ambil langsung dari folder itu lewat pengelola berkas.",
        color = NuxColors.GrayNeutral,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        modifier = Modifier.padding(top = 4.dp)
    )
}

/** Satu baris ringkas: waktu, nama instance, dan ukuran berkas. */
private fun summary(file: File): String {
    val (time, name) = GameLogArchive.describe(file)
    val size = sizeLabel(file.length())
    val label = name.replace('_', ' ').trim()
    return if (label.isEmpty()) "$time - $size" else "$time - $label - $size"
}

/** Ringkasan laporan launcher: waktu dan ukuran berkas. */
private fun launcherSummary(file: File): String {
    val (time, _) = GameLogArchive.describe(file)
    return "$time - " + sizeLabel(file.length())
}

private fun sizeLabel(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> String.format(Locale.US, "%.1f MB", bytes / 1048576.0)
    bytes >= 1024L -> (bytes / 1024L).toString() + " KB"
    else -> bytes.toString() + " B"
}

// ----------------------------------------------------------------------------
// Section: ui/components/GamepadFocus.kt
// ----------------------------------------------------------------------------

private val RingColor = Color(0xFFFFD23F)
private val FocusTint = Color.White.copy(alpha = 0.07f)
private val PressTint = Color.White.copy(alpha = 0.12f)

/** Bingkai kuning tipis di sekeliling yang sedang disorot (untuk navigasi controller/D-pad/keyboard). */
fun DrawScope.drawGamepadFocusRing() {
    val stroke = 2.dp.toPx()
    drawRect(FocusTint)
    drawRoundRect(
        color = RingColor,
        topLeft = Offset(stroke / 2f, stroke / 2f),
        size = Size(size.width - stroke, size.height - stroke),
        cornerRadius = CornerRadius(10.dp.toPx()),
        style = Stroke(width = stroke)
    )
}

/**
 * Pengganti umpan balik klik bawaan: tint tipis saat ditekan dan bingkai saat disorot lewat
 * D-pad/controller. Semuanya digambar di fase gambar, tanpa menyusun ulang layar.
 */
object FocusRingIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        FocusRingNode(interactionSource)

    override fun hashCode(): Int = -1

    override fun equals(other: Any?): Boolean = other === this
}

private class FocusRingNode(
    private val interactionSource: InteractionSource
) : Modifier.Node(), DrawModifierNode {
    private var pressed = false
    private var focused = false

    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> pressed = true
                    is PressInteraction.Release -> pressed = false
                    is PressInteraction.Cancel -> pressed = false
                    is FocusInteraction.Focus -> focused = true
                    is FocusInteraction.Unfocus -> focused = false
                }
                invalidateDraw()
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        if (pressed) drawRect(PressTint)
        if (focused) drawGamepadFocusRing()
    }
}

/** Pasang di akar layar: semua `clickable` di bawahnya otomatis punya sorotan fokus. */
@Composable
fun GamepadFocusProvider(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalIndication provides FocusRingIndication) {
        content()
    }
}

/**
 * Letakkan di dalam isi sebuah Dialog: jendela dialog ikut bisa dinavigasi dengan controller
 * (A pilih, B tutup, stick pindah sorotan).
 */
@Composable
fun GamepadWindowHook() {
    // Semua dialog memanggil hook ini, jadi bar sistem disembunyikan di sini sekali untuk semuanya.
    ImmersiveWindowHook()
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.parent as? DialogWindowProvider)?.window
        val undo = if (window != null) GamepadNav.install(window, allowBumper = false) else null
        onDispose { undo?.invoke() }
    }
}

/**
 * Menyembunyikan bar status dan bar navigasi Android di jendela dialog.
 *
 * Kenapa perlu: dialog adalah jendela terpisah dan TIDAK ikut mode layar penuh milik Activity.
 * Begitu dialog (atau menu samping in-game) terbuka, Android memunculkan lagi bar status dan
 * navigasi di atasnya, sehingga tampilan terdorong atau tertutup bar. Di sini jendela dialog
 * meminta hal yang sama dengan Activity: bar disembunyikan, dan hanya muncul sebentar bila
 * pengguna menggesernya dari tepi layar.
 *
 * Dipanggil lagi tiap jendela dapat fokus, karena sebagian HP memunculkan bar kembali saat itu.
 */
@Composable
fun ImmersiveWindowHook() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.parent as? DialogWindowProvider)?.window
        if (window == null) {
            return@DisposableEffect onDispose { }
        }
        val hideBars: () -> Unit = {
            try {
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val attrs = window.attributes
                    if (attrs.layoutInDisplayCutoutMode !=
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    ) {
                        attrs.layoutInDisplayCutoutMode =
                            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                        window.attributes = attrs
                    }
                }
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                    )
            } catch (_: Throwable) {
                // Bar sistem tetap tampil di perangkat yang menolak; tampilan tetap berfungsi.
            }
        }
        hideBars()
        // Sekali lagi setelah jendela benar-benar tampil: panggilan pertama bisa terlalu awal.
        view.post { hideBars() }
        val focusListener = ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
            if (hasFocus) hideBars()
        }
        try {
            val observer = view.viewTreeObserver
            if (observer.isAlive) observer.addOnWindowFocusChangeListener(focusListener)
        } catch (_: Throwable) {
        }
        onDispose {
            try {
                val observer = view.viewTreeObserver
                if (observer.isAlive) observer.removeOnWindowFocusChangeListener(focusListener)
            } catch (_: Throwable) {
            }
        }
    }
}

/**
 * Letakkan di dalam isi sebuah Dialog: dialog muncul dan hilang langsung, tanpa animasi jendela
 * bawaan sistem (memudar/membesar). Dipanggil sekali per dialog; tidak ada pekerjaan setelahnya.
 */
@Composable
fun NoWindowAnimationHook() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.parent as? DialogWindowProvider)?.window
        if (window != null && window.attributes.windowAnimations != 0) {
            window.setWindowAnimations(0)
        }
        onDispose { }
    }
}

// ----------------------------------------------------------------------------
// Section: ui/components/HomeVideoPlayer.kt
// ----------------------------------------------------------------------------

/**
 * Mengendalikan MediaPlayer dan TextureView untuk latar video Beranda. Semua dipanggil dari
 * thread utama. Hanya memakai API bawaan Android, jadi tidak menambah ukuran APK.
 */
private class HomeVideoController(private val file: File) {
    /** true = bingkai pertama sudah tampil; false = permukaan hilang dan perlu poster lagi. */
    var onFrame: (Boolean) -> Unit = {}
    var onError: () -> Unit = {}

    private var player: MediaPlayer? = null
    private var surface: Surface? = null
    private var texture: TextureView? = null
    private var container: FrameLayout? = null
    private var prepared = false
    private var rendered = false
    private var released = false
    private var failed = false
    private var wantPlay = false
    private var volume = 0f
    private var videoW = 0
    private var videoH = 0
    private val sampleBitmap: Bitmap by lazy { AmbientSampler.newBitmap() }
    private val samplePixels = AmbientSampler.newPixels()

    fun createView(ctx: Context): View {
        val frame = FrameLayout(ctx)
        val tv = TextureView(ctx)
        frame.addView(
            tv,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.CENTER
            )
        )
        frame.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> fitCover() }
        tv.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
                startPlayer(st)
            }

            override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {}

            override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                stopPlayer()
                if (rendered) {
                    rendered = false
                    onFrame(false)
                }
                return true
            }

            override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
        }
        container = frame
        texture = tv
        return frame
    }

    /** Memenuhi panel tanpa merusak rasio (potong tepi), seperti CENTER_CROP pada gambar. */
    private fun fitCover() {
        val c = container ?: return
        val tv = texture ?: return
        val cw = c.width
        val ch = c.height
        if (cw <= 0 || ch <= 0 || videoW <= 0 || videoH <= 0) return
        val scale = maxOf(cw.toFloat() / videoW, ch.toFloat() / videoH)
        val w = maxOf(cw, (videoW * scale).toInt())
        val h = maxOf(ch, (videoH * scale).toInt())
        val lp = tv.layoutParams as? FrameLayout.LayoutParams ?: return
        if (lp.width != w || lp.height != h) {
            lp.width = w
            lp.height = h
            tv.layoutParams = lp
        }
    }

    private fun startPlayer(st: SurfaceTexture) {
        if (released || failed) return
        stopPlayer()
        val s = Surface(st)
        surface = s
        prepared = false
        val mp = MediaPlayer()
        player = mp
        try {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .build()
            )
            mp.setDataSource(file.absolutePath)
            mp.setSurface(s)
            mp.isLooping = true
            mp.setVolume(volume, volume)
            mp.setOnVideoSizeChangedListener { _, w, h ->
                videoW = w
                videoH = h
                fitCover()
            }
            mp.setOnInfoListener { _, what, _ ->
                if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START && !rendered) {
                    rendered = true
                    onFrame(true)
                }
                false
            }
            mp.setOnPreparedListener {
                prepared = true
                applyPlayState()
            }
            mp.setOnErrorListener { _, _, _ ->
                fail()
                true
            }
            mp.prepareAsync()
        } catch (e: Throwable) {
            fail()
        }
    }

    private fun stopPlayer() {
        val mp = player
        player = null
        prepared = false
        if (mp != null) {
            try {
                mp.setOnErrorListener(null)
                mp.setOnInfoListener(null)
                mp.setOnPreparedListener(null)
                mp.release()
            } catch (_: Throwable) {
            }
        }
        try {
            surface?.release()
        } catch (_: Throwable) {
        }
        surface = null
    }

    private fun fail() {
        if (failed) return
        failed = true
        stopPlayer()
        onError()
    }

    private fun applyPlayState() {
        val mp = player ?: return
        if (!prepared || failed) return
        try {
            if (wantPlay) {
                mp.setVolume(volume, volume)
                if (!mp.isPlaying) mp.start()
            } else if (mp.isPlaying) {
                mp.pause()
            }
        } catch (e: Throwable) {
            fail()
        }
    }

    /**
     * Putar atau jeda, dan atur volume (0..1). Bila akan mulai memutar dengan suara dan aplikasi
     * lain sedang memutar musik, video dibisukan supaya tidak menimpa musik pengguna.
     */
    fun setState(play: Boolean, vol: Float, otherMusicActive: () -> Boolean) {
        val wasPlaying = try {
            player?.isPlaying == true
        } catch (_: Throwable) {
            false
        }
        volume = if (vol > 0f && play && !wasPlaying && otherMusicActive()) 0f else vol
        wantPlay = play
        applyPlayState()
    }

    /** Warna tepi bingkai yang sedang tampil, atau null bila belum bisa dibaca. */
    fun sample(): AmbientColors? {
        val tv = texture ?: return null
        if (player == null || !prepared || !rendered || !tv.isAvailable) return null
        return try {
            tv.getBitmap(sampleBitmap)
            AmbientSampler.fromBitmap(sampleBitmap, samplePixels)
        } catch (e: Throwable) {
            null
        }
    }

    fun release() {
        released = true
        stopPlayer()
        container = null
        texture = null
    }
}

/**
 * Video latar yang diputar berulang tanpa henti, tanpa suara kecuali [volume] > 0. Jeda otomatis
 * saat aplikasi tidak di layar depan. Bila [ambient] tidak null, warna tepi bingkai dikirim ke
 * sana beberapa kali per detik untuk efek cahaya.
 */
@Composable
fun HomeVideoLayer(
    file: File,
    volume: Float,
    playing: Boolean,
    ambient: AmbientState?,
    onFrameReady: (Boolean) -> Unit,
    onFailed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val resumed = rememberIsResumed()
    val shouldPlay = playing && resumed

    val controller = remember(file) { HomeVideoController(file) }
    val frameCallback by rememberUpdatedState(onFrameReady)
    val failCallback by rememberUpdatedState(onFailed)
    remember(controller) {
        controller.onFrame = { frameCallback(it) }
        controller.onError = { failCallback() }
        true
    }

    LaunchedEffect(controller, shouldPlay, volume) {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        controller.setState(shouldPlay, volume) { audio?.isMusicActive == true }
    }

    if (ambient != null && shouldPlay) {
        LaunchedEffect(controller, ambient) {
            delay(400)
            while (true) {
                controller.sample()?.let { ambient.colors = it }
                delay(AmbientSampler.INTERVAL_MS)
            }
        }
    }

    DisposableEffect(controller) {
        onDispose { controller.release() }
    }

    AndroidView(
        factory = { ctx -> controller.createView(ctx) },
        modifier = modifier
    )
}

/** True selama layar aplikasi ini berada di depan (RESUMED); dipakai untuk menjeda animasi. */
@Composable
fun rememberIsResumed(): Boolean {
    val context = LocalContext.current
    val owner = remember(context) { findLifecycleOwner(context) }
    var resumed by remember(owner) {
        mutableStateOf(owner?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) ?: true)
    }
    DisposableEffect(owner) {
        val lifecycle = owner?.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> resumed = true
                Lifecycle.Event.ON_PAUSE -> resumed = false
                else -> {}
            }
        }
        lifecycle?.addObserver(observer)
        onDispose { lifecycle?.removeObserver(observer) }
    }
    return resumed
}

private fun findLifecycleOwner(context: Context): LifecycleOwner? {
    var c: Context? = context
    while (c is ContextWrapper) {
        if (c is LifecycleOwner) return c
        c = c.baseContext
    }
    return null
}

/** True bila mode Hemat Baterai HP sedang menyala; ikut berubah saat pengguna menyalakannya. */
@Composable
fun rememberPowerSaveMode(): Boolean {
    val appContext = LocalContext.current.applicationContext
    val powerManager = remember { appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager }
    var saving by remember { mutableStateOf(powerManager?.isPowerSaveMode ?: false) }
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                saving = powerManager?.isPowerSaveMode ?: false
            }
        }
        try {
            ContextCompat.registerReceiver(
                appContext,
                receiver,
                IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } catch (_: Throwable) {
        }
        onDispose {
            try {
                appContext.unregisterReceiver(receiver)
            } catch (_: Throwable) {
            }
        }
    }
    return saving
}

// ----------------------------------------------------------------------------
// Section: ui/components/NovaUiSharedCards.kt
// ----------------------------------------------------------------------------

/**
 * NovaCraft unified shared UI components:
 * - HomeInfo
 * - FocusableBox
 * - ModIcon
 * - AmberSpinner & AmberWaitBar
 * - MobileGluesCacheRow
 * - PowerSaverCard
 * - DeviceProfileCard
 */

private const val SPINNER_STEPS = 12
private const val SPINNER_STEP_MS = 100L
private const val BAR_POSITIONS = 9
private const val BAR_STEP_MS = 450L

object HomeInfo {
    const val RAM_GOOD_MB = 1400
    const val RAM_OK_MB = 800

    fun ramBucket(freeMb: Int): Int = (freeMb.coerceAtLeast(0) / 100) * 100

    fun ramLevel(freeMb: Int): Int = when {
        freeMb >= RAM_GOOD_MB -> 2
        freeMb >= RAM_OK_MB -> 1
        else -> 0
    }

    fun ramText(freeMb: Int): String {
        val gb = freeMb.coerceAtLeast(0) / 1024f
        val locale = if (Tr.effective == Tr.EN || Tr.effective == Tr.TH) Locale.US else Locale.forLanguageTag("id-ID")
        val value = String.format(locale, "%.1f", gb)
        return "RAM bebas $value GB"
    }

    fun rendererLabel(id: String, displayName: String): String {
        if (id.equals("auto", ignoreCase = true)) return "Otomatis"
        val first = displayName.trim().substringBefore(' ')
        return first.ifBlank { displayName.trim() }.ifBlank { "Otomatis" }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FocusableBox(
    modifier: Modifier = Modifier,
    requestKey: Any? = null,
    canRequestFocus: () -> Boolean = { true },
    hoverIcon: State<PointerIcon>? = null
) {
    val focusRequester = remember { FocusRequester() }
    val currentCanRequestFocus by rememberUpdatedState(canRequestFocus)

    val iconModifier = if (hoverIcon != null) Modifier.pointerHoverIcon(hoverIcon.value) else Modifier

    Box(
        modifier = iconModifier
            .then(modifier)
            .focusRequester(focusRequester)
            .focusable(enabled = true)
    )

    LaunchedEffect(requestKey) {
        if (currentCanRequestFocus()) {
            focusRequester.requestFocus()
        }
    }
}

@Composable
fun ModIcon(
    item: InstalledModItem,
    size: Dp,
    modifier: Modifier = Modifier,
    dimmed: Boolean = false
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(size * 0.26f)
    val initial = remember(item.filePath, item.sizeBytes) { ModIconLoader.peek(item.filePath) }
    val bitmap by produceState<Bitmap?>(initialValue = initial, item.filePath, item.sizeBytes) {
        if (value == null && item.filePath.isNotEmpty()) {
            value = ModIconLoader.load(context, File(item.filePath), item.itemType)
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .alpha(if (dimmed) 0.45f else 1f)
            .clip(shape)
            .background(if (bitmap != null) NuxColors.SurfaceElevated else NuxColors.YellowPale, shape)
            .border(NuxSizes.GlassBorder, NuxColors.CardBorder, shape),
        contentAlignment = Alignment.Center
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = item.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                filterQuality = if (bmp.width <= 48) FilterQuality.None else FilterQuality.Medium
            )
        } else {
            androidx.compose.material3.Text(
                text = item.name.take(2).uppercase(),
                color = NuxColors.Yellow,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun AmberSpinner(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    strokeWidth: Dp = 2.dp
) {
    var step by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(SPINNER_STEP_MS)
            step = (step + 1) % SPINNER_STEPS
        }
    }
    Box(
        modifier = modifier
            .size(24.dp)
            .drawBehind {
                val stroke = strokeWidth.toPx()
                drawArc(
                    color = color,
                    startAngle = step * (360f / SPINNER_STEPS) - 90f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = Offset(stroke / 2f, stroke / 2f),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
    )
}

@Composable
fun AmberWaitBar(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    trackColor: Color = Color.White.copy(alpha = 0.18f)
) {
    var step by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        val cycle = 2 * (BAR_POSITIONS - 1)
        while (true) {
            delay(BAR_STEP_MS)
            step = (step + 1) % cycle
        }
    }
    Box(
        modifier = modifier
            .background(trackColor)
            .drawBehind {
                val last = BAR_POSITIONS - 1
                val pos = if (step <= last) step else 2 * last - step
                val segment = size.width * 0.3f
                val x = (size.width - segment) * (pos.toFloat() / last)
                drawRect(
                    color = color,
                    topLeft = Offset(x, 0f),
                    size = Size(segment, size.height)
                )
            }
    )
}

@Composable
fun MobileGluesCacheRow() {
    val context = LocalContext.current
    val settings by SettingsManager.settings.collectAsState()

    SettingSwitchRow(
        title = "Cache shader MobileGlues",
        desc = "Menyimpan hasil terjemahan shader di HP, jadi game berikutnya memuat shader lebih cepat dan lebih sedikit patah-patah. Dimatikan = MobileGlues memakai pengaturan bawaannya tanpa cache. Berlaku saat game dijalankan berikutnya.",
        checked = settings.mobileGluesConfig,
        onCheckedChange = { on ->
            SettingsManager.updateSettings(context, SettingsManager.settings.value.copy(mobileGluesConfig = on))
        }
    )
}

@Composable
fun PowerSaverCard() {
    val context = LocalContext.current
    val settings by SettingsManager.settings.collectAsState()

    NuxCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        cornerRadius = 24.dp,
        backgroundColor = AmberUi.Surface,
        borderColor = AmberUi.Border
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = null,
                    tint = AmberUi.AccentSoft,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Mode Hemat",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = NuxColors.DarkGray
                    )
                    Text(
                        text = "Untuk HP yang cepat panas atau baterainya cepat habis.",
                        fontSize = 11.sp,
                        color = NuxColors.GrayNeutral,
                        lineHeight = 15.sp
                    )
                }
            }
            SettingSwitchRow(
                title = "Nyalakan Mode Hemat",
                desc = "Menyalakan mode performa stabil sistem dan V-Sync Zink, serta membatasi skala resolusi maksimal ${PowerSaver.MAX_RESOLUTION}%.",
                checked = settings.powerSaverMode,
                onCheckedChange = { on ->
                    SettingsManager.updateSettings(context, PowerSaver.set(SettingsManager.settings.value, on))
                }
            )
            Text(
                text = "Berlaku saat game dijalankan berikutnya. Dimatikan kembali, setelan sebelumnya dipulihkan.",
                fontSize = 11.sp,
                color = NuxColors.GrayNeutral,
                lineHeight = 15.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
            )
        }
    }
}

@Composable
fun DeviceProfileCard() {
    val context = LocalContext.current
    val settings by SettingsManager.settings.collectAsState()
    val totalRamMb = remember { SettingsManager.getTotalDeviceMemoryMb(context) }
    if (!DeviceProfiles.isSmallRamPhone(totalRamMb)) return
    val gcMode = DeviceProfiles.normalizeGeneralGcMode(settings.generalGcMode)

    NuxCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        cornerRadius = 24.dp,
        backgroundColor = AmberUi.Surface,
        borderColor = AmberUi.Border
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Memory,
                    contentDescription = null,
                    tint = AmberUi.AccentSoft,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Optimasi HP RAM kecil",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = NuxColors.DarkGray
                    )
                    Text(
                        text = "RAM HP ini ${totalRamMb} MB, jadi cara kerja pembersih memori Java bisa disetel.",
                        fontSize = 11.sp,
                        color = NuxColors.GrayNeutral,
                        lineHeight = 15.sp
                    )
                }
            }
            SettingChoiceRow(
                title = "Mode GC (pembersih memori Java)",
                desc = when (gcMode) {
                    DeviceProfiles.GC_SERIAL ->
                        "Serial: paling ringan untuk prosesor, jeda GC sedikit lebih panjang. Coba bila game sering tersendat."
                    DeviceProfiles.GC_DEFAULT ->
                        "Bawaan: tanpa flag GC tambahan dari launcher."
                    else ->
                        "Otomatis: G1 dengan memori terbatas, cocok untuk kebanyakan HP. Coba ini dulu."
                },
                options = listOf(
                    DeviceProfiles.GC_AUTO to "Otomatis",
                    DeviceProfiles.GC_SERIAL to "Serial",
                    DeviceProfiles.GC_DEFAULT to "Bawaan"
                ),
                selected = gcMode,
                onSelect = { modeId ->
                    SettingsManager.updateSettings(context, SettingsManager.settings.value.copy(generalGcMode = modeId))
                }
            )
            Text(
                text = "Berlaku saat game dijalankan berikutnya. Untuk membandingkan, lihat baris [Kinerja] di log game.",
                fontSize = 11.sp,
                color = NuxColors.GrayNeutral,
                lineHeight = 15.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
            )
        }
    }
}

// ----------------------------------------------------------------------------
// Section: ui/components/NuxComponents.kt
// ----------------------------------------------------------------------------

/**
 * Kartu kaca: isian transparan tipis, tepi putih tipis (atau gradien lewat borderBrush), tanpa
 * bayangan. [shadowColor] dan [shadowOffset] dipertahankan supaya pemanggil lama tetap
 * dikompilasi, nilainya tidak dipakai lagi.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun NuxCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = NuxColors.SurfaceWhite,
    borderColor: Color = NuxColors.CardBorder,
    shadowColor: Color = NuxColors.Ink,
    shadowOffset: Dp = 0.dp,
    cornerRadius: Dp = (20.dp).resp(),
    borderWidth: Dp = NuxSizes.GlassBorder,
    borderBrush: Brush? = null,
    fillMaxHeight: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val cardModifier = if (fillMaxHeight) modifier.fillMaxSize() else modifier.fillMaxWidth()
    val shape = RoundedCornerShape(cornerRadius)
    val edge = if (borderBrush != null) {
        Modifier.border(borderWidth, borderBrush, shape)
    } else {
        Modifier.border(borderWidth, borderColor, shape)
    }

    Box(
        modifier = cardModifier
            .background(backgroundColor, shape)
            .then(edge)
            .clip(shape)
    ) {
        content()
    }
}

/**
 * Tombol AmberLauncher, gayanya dipilih otomatis dari warna isiannya:
 * - isian terang solid (kuning/putih): tombol utama, datar tanpa tepi dan tanpa bayangan, teks
 *   otomatis Ink supaya selalu terbaca;
 * - selain itu (kaca/transparan/gelap): tombol kaca, tepi putih tipis.
 * Umpan balik tekan digambar hanya di fase gambar (lapisan gelap/terang tipis), jadi menyentuh
 * tombol tidak menyusun ulang layar. [shadowOffset] dipertahankan supaya pemanggil lama tetap
 * dikompilasi, nilainya tidak dipakai lagi.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun NuxButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = NuxColors.ForestGreen,
    contentColor: Color = Color.Unspecified,
    borderColor: Color = Color.Unspecified,
    enabled: Boolean = true,
    shadowOffset: Dp = 0.dp,
    cornerRadius: Dp = (12.dp).resp(),
    contentPadding: PaddingValues = PaddingValues(horizontal = (14.dp).resp(), vertical = (6.dp).resp()),
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressedState = interactionSource.collectIsPressedAsState()
    val focusedState = interactionSource.collectIsFocusedAsState()

    val opaqueFill = backgroundColor.alpha >= 0.9f
    val brightFill = opaqueFill && backgroundColor.luminance() > 0.45f
    val edgeColor = when {
        !enabled -> NuxColors.CardBorder
        borderColor != Color.Unspecified -> borderColor
        else -> NuxColors.CardBorder
    }
    val textColor = when {
        !enabled -> NuxColors.GrayNeutral.copy(alpha = 0.6f)
        brightFill && (contentColor == Color.Unspecified || contentColor.luminance() > 0.6f) -> NuxColors.Ink
        opaqueFill && !brightFill && contentColor == Color.Unspecified -> Color.White
        !opaqueFill && backgroundColor.alpha < 0.5f && contentColor != Color.Unspecified &&
            contentColor.luminance() < 0.05f -> NuxColors.DarkGray
        contentColor == Color.Unspecified -> NuxColors.DarkGray
        else -> contentColor
    }
    val shape = RoundedCornerShape(cornerRadius)
    val pressTint = if (opaqueFill) Color.Black.copy(alpha = 0.14f) else AmberUi.Text.copy(alpha = 0.10f)
    val edge = if (opaqueFill && enabled && borderColor == Color.Unspecified) {
        Modifier
    } else {
        Modifier.border(NuxSizes.GlassBorder, edgeColor, shape)
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(if (enabled) backgroundColor else NuxColors.SurfaceWhite, shape)
            .then(edge)
            .drawWithContent {
                drawContent()
                if (enabled && pressedState.value) drawRect(pressTint)
                if (enabled && focusedState.value) drawGamepadFocusRing()
            }
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .defaultMinSize(minHeight = (40.dp).resp())
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides textColor
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                content()
            }
        }
    }
}

/**
 * Lencana kecil berbentuk pil dengan tepi kaca tipis.
 */
@Composable
fun NuxBadge(
    text: String,
    backgroundColor: Color = NuxColors.SoftLime,
    textColor: Color = NuxColors.DarkGray,
    borderColor: Color? = null,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape((8.dp).resp())
    val effectiveBorderColor = borderColor ?: NuxColors.CardBorder

    Box(
        modifier = modifier
            .background(backgroundColor, shape)
            .border(NuxSizes.GlassBorder, effectiveBorderColor, shape)
            .padding(horizontal = (10.dp).resp(), vertical = (4.dp).resp())
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = (11.sp).resp(),
            fontWeight = FontWeight.Bold,
            letterSpacing = (0.4.sp).resp(),
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * Kolom isian kaca dengan tepi putih tipis, kursor kuning, tinggi nyaman untuk jari.
 */
@Composable
fun NuxTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle? = null,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val shape = RoundedCornerShape((14.dp).resp())
    val resolvedTextStyle = textStyle ?: TextStyle(
        color = NuxColors.DarkGray,
        fontSize = (14.sp).resp(),
        fontWeight = FontWeight.SemiBold
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(NuxColors.SurfaceInput, shape)
            .border(NuxSizes.GlassBorder, NuxColors.CardBorder, shape)
            .defaultMinSize(minHeight = (46.dp).resp())
            .padding(horizontal = (14.dp).resp(), vertical = (10.dp).resp()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = NuxColors.GrayNeutral.copy(alpha = 0.7f),
                    fontSize = (14.sp).resp(),
                    fontWeight = FontWeight.Normal,
                    maxLines = 1
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = resolvedTextStyle,
                cursorBrush = SolidColor(NuxColors.Yellow),
                singleLine = true,
                visualTransformation = visualTransformation,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (trailingContent != null) {
            Spacer(modifier = Modifier.width((8.dp).resp()))
            trailingContent()
        }
    }
}

/**
 * Jendela dialog biasa (Dialog Compose) yang isinya memakai skala pembacaan seimbang.
 * Dipakai oleh dialog yang menyusun tampilannya sendiri.
 */
@Composable
fun NuxWindowDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        GamepadWindowHook()
        NuxReadable(scale = 1f, boost = 1f) {
            content()
        }
    }
}

/**
 * Dialog kaca: panel gelap hampir buram dengan tepi gradien putih tipis di atas latar redup.
 */
@Composable
fun NuxDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    fillMaxHeight: Boolean = false,
    compact: Boolean = false,
    content: @Composable () -> Unit
) {
    val inScaledScreen = LocalNuxScaled.current
    val isTablet = LocalNuxScale.current.isTablet
    val defaultWidthFraction = if (isTablet) 0.84f else 0.94f
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

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        GamepadWindowHook()
        NuxReadable(
            scale = if (compact && !inScaledScreen) 0.94f else 1f,
            boost = 1f
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = if (dark) 0.62f else 0.42f))
                    .windowInsetsPadding(
                        WindowInsets.displayCutout
                            .union(WindowInsets.ime)
                            .union(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                    )
                    .padding(horizontal = (12.dp).resp(), vertical = (8.dp).resp()),
                contentAlignment = Alignment.Center
            ) {
                val dialogModifier = if (fillMaxHeight) {
                    modifier.fillMaxWidth(defaultWidthFraction).fillMaxHeight(0.94f)
                } else {
                    modifier.fillMaxWidth(defaultWidthFraction).wrapContentHeight()
                }
                val animatePopup = AmberUi.animDialogsPopups
                val popupProgress = remember { Animatable(if (animatePopup) 0f else 1f) }
                LaunchedEffect(animatePopup) {
                    if (animatePopup) {
                        popupProgress.snapTo(0f)
                        popupProgress.animateTo(1f, animationSpec = tween(durationMillis = 170))
                    } else {
                        popupProgress.snapTo(1f)
                    }
                }
                val s = 0.94f + 0.06f * popupProgress.value
                NuxCard(
                    modifier = dialogModifier.graphicsLayer {
                        scaleX = s
                        scaleY = s
                        alpha = popupProgress.value
                    },
                    backgroundColor = NuxColors.Panel,
                    borderBrush = dialogEdge,
                    cornerRadius = (24.dp).resp(),
                    fillMaxHeight = fillMaxHeight
                ) {
                    content()
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------
// Section: ui/components/NuxNetworkImage.kt
// ----------------------------------------------------------------------------

private const val IMAGE_HTTP_CACHE_BYTES = 40L * 1024L * 1024L
private const val IMAGE_MAX_DOWNLOAD_BYTES = 12L * 1024L * 1024L
private const val IMAGE_DEFAULT_MAX_SIDE_PX = 512

/**
 * Cache memori berbasis UKURAN BYTE (bukan jumlah gambar): 50 gambar besar tidak lagi bisa
 * menghabiskan puluhan MB RAM. Batas = 1/12 heap aplikasi, antara 8 dan 32 MB.
 */
private val imageCache: LruCache<String, ImageBitmap> by lazy {
    val maxBytes = (Runtime.getRuntime().maxMemory() / 12L)
        .coerceIn(8L * 1024L * 1024L, 32L * 1024L * 1024L)
        .toInt()
    object : LruCache<String, ImageBitmap>(maxBytes) {
        override fun sizeOf(key: String, value: ImageBitmap): Int {
            val bytes = value.width.toLong() * value.height.toLong() * 4L
            return if (bytes > Int.MAX_VALUE.toLong()) Int.MAX_VALUE else bytes.toInt()
        }
    }
}

private val imageClientLock = Any()

@Volatile
private var imageClientInstance: OkHttpClient? = null

/**
 * Klien HTTP gambar dengan cache DISK (40 MB di folder cache aplikasi): ikon mod / avatar yang
 * sudah pernah dibuka tidak diunduh ulang setiap aplikasi dibuka atau daftar di-scroll.
 */
private fun imageHttpClient(context: Context): OkHttpClient {
    imageClientInstance?.let { return it }
    return synchronized(imageClientLock) {
        imageClientInstance ?: buildImageHttpClient(context.applicationContext).also {
            imageClientInstance = it
        }
    }
}

private fun buildImageHttpClient(appContext: Context): OkHttpClient {
    val builder = OkHttpClient.Builder()
        .dns(com.aistudio.novacraft.launcher.core.utils.NuxDns)
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
    try {
        builder.cache(Cache(File(appContext.cacheDir, "nux_image_http"), IMAGE_HTTP_CACHE_BYTES))
        // Gambar jarang berubah: simpan 7 hari walau server tidak mengirim header cache yang benar.
        builder.addNetworkInterceptor(
            Interceptor { chain ->
                val response = chain.proceed(chain.request())
                if (response.isSuccessful) {
                    response.newBuilder()
                        .removeHeader("Pragma")
                        .header("Cache-Control", "public, max-age=604800")
                        .build()
                } else {
                    response
                }
            }
        )
    } catch (_: Throwable) {
        // Tanpa cache disk bila foldernya tidak bisa dibuat; gambar tetap tampil.
    }
    return builder.build()
}

/**
 * Menjalankan panggilan HTTP dan membaca isi gambarnya di thread OkHttp. Dapat dibatalkan:
 * saat item keluar dari layar (coroutine dibatalkan) unduhan ikut dihentikan, jadi scroll
 * cepat di daftar mod tidak menumpuk puluhan unduhan yang tak terpakai.
 */
private suspend fun Call.awaitImageBytes(maxBytes: Long): ByteArray? =
    suspendCancellableCoroutine { cont ->
        cont.invokeOnCancellation {
            try {
                this@awaitImageBytes.cancel()
            } catch (_: Throwable) {
            }
        }
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (cont.isActive) cont.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val bytes: ByteArray? = response.use { r ->
                        if (!r.isSuccessful) {
                            null
                        } else {
                            val body = r.body
                            if ((body?.contentLength() ?: -1L) > maxBytes) null else body?.bytes()
                        }
                    }
                    if (cont.isActive) cont.resume(bytes)
                } catch (e: Throwable) {
                    if (cont.isActive) cont.resumeWithException(e)
                }
            }
        })
    }

/**
 * Pangkat dua terbesar yang masih menjaga sisi terpanjang gambar >= [targetPx].
 * Gambar 2000x2000 yang cuma tampil 40dp tidak perlu dimuat penuh (16 MB) di RAM.
 */
private fun sampleSizeFor(width: Int, height: Int, targetPx: Int): Int {
    if (width <= 0 || height <= 0 || targetPx <= 0) return 1
    val longest = maxOf(width, height)
    var sample = 1
    while (longest / (sample * 2) >= targetPx) sample *= 2
    return sample
}

private fun decodeSampled(bytes: ByteArray, targetPx: Int): ImageBitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    val opts = BitmapFactory.Options().apply {
        inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, targetPx)
    }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)?.asImageBitmap()
}

private fun decodeSampledFromUri(context: Context, uri: Uri, targetPx: Int): ImageBitmap? {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, bounds)
    }
    val opts = BitmapFactory.Options().apply {
        inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, targetPx)
    }
    return resolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, opts)
    }?.asImageBitmap()
}

private suspend fun loadImageBitmap(context: Context, model: Any?, targetPx: Int): ImageBitmap? {
    return try {
        when (model) {
            is Uri -> withContext(Dispatchers.IO) { decodeSampledFromUri(context, model, targetPx) }
            is String -> when {
                model.startsWith("http://") || model.startsWith("https://") -> {
                    // Gambar selalu lewat HTTPS (alamat http:// dinaikkan), supaya tidak bisa disadap atau diganti
                    val secureUrl = if (model.startsWith("http://")) "https://" + model.removePrefix("http://") else model
                    val request = Request.Builder().url(secureUrl).build()
                    val bytes = imageHttpClient(context).newCall(request).awaitImageBytes(IMAGE_MAX_DOWNLOAD_BYTES)
                    if (bytes == null || bytes.isEmpty()) {
                        null
                    } else {
                        withContext(Dispatchers.Default) { decodeSampled(bytes, targetPx) }
                    }
                }
                model.startsWith("content://") || model.startsWith("file://") ->
                    withContext(Dispatchers.IO) { decodeSampledFromUri(context, Uri.parse(model), targetPx) }
                else -> null
            }
            else -> null
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Throwable) {
        null
    }
}

private fun imageCacheKey(model: Any?, targetPx: Int): String? {
    if (model == null) return null
    val raw = model.toString()
    if (raw.isBlank()) return null
    return "$raw|$targetPx"
}

/**
 * Neo-Brutalist Network & Local Uri Image Loader dengan cache memori (byte), cache disk,
 * decode hemat memori (downsample sesuai [maxSidePx]) dan pembatalan saat keluar layar.
 *
 * @param maxSidePx perkiraan sisi terpanjang gambar yang benar-benar dibutuhkan di layar
 *        (dalam piksel). Makin kecil makin hemat RAM; ikon 38-48dp cukup 192.
 * @param showSpinner tampilkan indikator putar saat memuat (hanya untuk gambar besar; ikon
 *        kecil memakai latar statis supaya daftar panjang tidak menggambar puluhan animasi).
 */
@Composable
fun NuxNetworkImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    fallbackInitials: String = "N",
    shape: Shape = RoundedCornerShape(12.dp),
    contentScale: ContentScale = ContentScale.Crop,
    maxSidePx: Int = IMAGE_DEFAULT_MAX_SIDE_PX,
    showSpinner: Boolean = false
) {
    val context = LocalContext.current
    val cacheKey = remember(model, maxSidePx) { imageCacheKey(model, maxSidePx) }
    var imageBitmap by remember(cacheKey) {
        mutableStateOf<ImageBitmap?>(cacheKey?.let { imageCache.get(it) })
    }
    var isLoading by remember(cacheKey) {
        mutableStateOf(cacheKey != null && imageBitmap == null)
    }

    LaunchedEffect(cacheKey) {
        val key = cacheKey
        if (key == null) {
            imageBitmap = null
            isLoading = false
            return@LaunchedEffect
        }
        if (imageBitmap != null) {
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true
        try {
            val loaded = loadImageBitmap(context, model, maxSidePx)
            if (loaded != null) {
                imageCache.put(key, loaded)
                imageBitmap = loaded
            }
        } finally {
            isLoading = false
        }
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(NuxColors.SurfaceWhite),
        contentAlignment = Alignment.Center
    ) {
        val bitmap = imageBitmap
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        } else if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NuxColors.LightGray.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                if (showSpinner) {
                    AmberSpinner(
                        modifier = Modifier.size(18.dp),
                        color = NuxColors.SageGreen,
                        strokeWidth = 2.dp
                    )
                }
            }
        } else {
            // Fallback Initials Display with Neo-Brutalist gaming colors
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NuxColors.SoftLime),
                contentAlignment = Alignment.Center
            ) {
                RawM3Text(
                    text = fallbackInitials.take(2).uppercase(),
                    color = NuxColors.SageGreen,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

// ----------------------------------------------------------------------------
// Section: ui/components/NuxSettingParts.kt
// ----------------------------------------------------------------------------

/**
 * Bahan penyusun layar Pengaturan gaya baru: baris lega dengan judul + keterangan di kiri dan
 * kontrol di kanan, pemisah tipis, chip pilihan setinggi 38dp, dan tab samping.
 * Semua target sentuh minimal 44-48dp dan huruf terkecil 12sp supaya enak dipakai di HP.
 */

/**
 * Penyaring pencarian pengaturan. Null = tidak sedang mencari (baris tampil di dalam kartu
 * kelompoknya). Ada = tiap baris yang cocok tampil sendiri di dalam satu kartu panjang.
 */
class SettingsFilter(val query: String)

val LocalSettingsFilter = compositionLocalOf<SettingsFilter?> { null }

private fun matchesFilter(filter: SettingsFilter, vararg texts: String?): Boolean {
    val q = filter.query.trim()
    if (q.isEmpty()) return false
    return texts.any { t -> t != null && (t.contains(q, ignoreCase = true) || Tr.t(t).contains(q, ignoreCase = true)) }
}

/** Pembungkus baris: ikut penyaringan pencarian bila aktif. */
@Composable
fun SettingFrame(title: String, desc: String?, content: @Composable () -> Unit) {
    val filter = LocalSettingsFilter.current
    if (filter == null) {
        content()
    } else if (matchesFilter(filter, title, desc)) {
        AmberCard { content() }
    }
}

/** Garis pemisah tipis antar baris dalam satu kartu (tidak tampil saat mencari). */
@Composable
fun SettingDivider() {
    if (LocalSettingsFilter.current != null) return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(AmberUi.Border)
    )
}

/** Judul baris + keterangan kecil di bawahnya. */
@Composable
fun SettingRowText(
    title: String,
    desc: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            color = AmberUi.Text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        if (!desc.isNullOrBlank()) {
            Text(
                text = desc,
                color = AmberUi.TextDim,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

/** Chip pilihan berbentuk pil. Terpilih = isian aksen dengan tepi terang. */
@Composable
fun SettingChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 36.dp)
            .clip(shape)
            .background(if (selected) AmberUi.Accent else Color.Transparent, shape)
            .border(
                1.dp,
                if (selected) AmberUi.AccentEdge else AmberUi.BorderStrong,
                shape
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) AmberUi.OnAccent else AmberUi.Text,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Baris saklar: ketuk di mana saja pada baris untuk mengubah. */
@Composable
fun SettingSwitchRow(
    title: String,
    desc: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    SettingFrame(title, desc) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { onCheckedChange(!checked) }
                .alpha(if (enabled) 1f else 0.55f)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingRowText(title = title, desc = desc, modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(16.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                // Barisnya sudah bisa disorot dan dipilih; saklar tidak perlu jadi perhentian fokus kedua.
                modifier = Modifier.focusProperties { canFocus = false },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AmberUi.OnAccent,
                    checkedTrackColor = AmberUi.Accent,
                    checkedBorderColor = AmberUi.AccentEdge,
                    uncheckedThumbColor = if (AmberUi.isDark) AmberUi.TextDim else Color.White,
                    uncheckedTrackColor = if (AmberUi.isDark) AmberUi.SurfaceRaised else Color(0xFFCBD5E1),
                    uncheckedBorderColor = AmberUi.BorderStrong
                )
            )
        }
    }
}

/** Baris pilihan: judul + keterangan, lalu deretan chip (bisa digeser bila kepanjangan). */
@Composable
fun <T> SettingChoiceRow(
    title: String,
    desc: String?,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    SettingFrame(title, desc) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            SettingRowText(title = title, desc = desc)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.forEach { (value, label) ->
                    SettingChip(
                        text = label,
                        selected = value == selected,
                        onClick = { onSelect(value) }
                    )
                }
            }
        }
    }
}

/** Baris slider: judul di kiri, nilai di kanan (merah bila berlebihan), slider di bawah. */
@Composable
fun SettingSliderRow(
    title: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    desc: String? = null,
    warn: Boolean = false,
    // Dipanggil saat jari diangkat. Dipakai pengaturan yang ditulis ke berkas hanya sekali per geser.
    onValueChangeFinished: (() -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null
) {
    SettingFrame(title, desc) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SettingRowText(title = title, desc = desc, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = valueText,
                    color = if (warn) AmberUi.Danger else AmberUi.Text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                onValueChangeFinished = onValueChangeFinished,
                valueRange = valueRange,
                steps = steps,
                colors = SliderDefaults.colors(
                    thumbColor = if (AmberUi.isDark) Color.White else AmberUi.Accent,
                    activeTrackColor = AmberUi.AccentSoft,
                    inactiveTrackColor = AmberUi.BorderStrong,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent
                )
            )
            if (footer != null) {
                footer()
            }
        }
    }
}

/** Baris aksi: judul + keterangan di kiri, tombol pil di kanan. */
@Composable
fun SettingActionRow(
    title: String,
    desc: String?,
    actionLabel: String,
    onClick: () -> Unit,
    filled: Boolean = false,
    danger: Boolean = false
) {
    SettingFrame(title, desc) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingRowText(title = title, desc = desc, modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(12.dp))
            AmberPillButton(text = actionLabel, onClick = onClick, filled = filled, danger = danger)
        }
    }
}

/** Baris nilai: judul di kiri, nilai saat ini dan panah di kanan; ketuk untuk memilih. */
@Composable
fun SettingValueRow(
    title: String,
    desc: String?,
    value: String,
    onClick: () -> Unit
) {
    SettingFrame(title, desc) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingRowText(title = title, desc = desc, modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = value,
                color = AmberUi.Text,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 140.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = AmberUi.TextDim,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** Tab samping (ikon + nama). Terpilih = tint kuning dengan tepi kuning tipis. */
@Composable
fun SettingTab(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 44.dp)
            .clip(shape)
            .background(if (selected) NuxColors.YellowPale else Color.Transparent, shape)
            .border(
                NuxSizes.GlassBorder,
                if (selected) NuxColors.Yellow.copy(alpha = 0.45f) else Color.Transparent,
                shape
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) NuxColors.Yellow else NuxColors.GrayNeutral,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            color = if (selected) NuxColors.Yellow else NuxColors.DarkGray,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Black else FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Tombol ikon bulat 44dp (tutup, hapus, dll). ring = lingkaran kaca di belakang ikon. */
@Composable
fun NuxIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = NuxColors.GrayNeutral,
    ring: Boolean = true,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .then(
                if (ring) {
                    Modifier
                        .background(NuxColors.SurfaceElevated, CircleShape)
                        .border(NuxSizes.GlassBorder, NuxColors.CardBorder, CircleShape)
                } else {
                    Modifier
                }
            )
            .clickable(enabled = enabled) { onClick() },
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

/** Baris pilihan satu-dari-banyak dalam dialog (daftar versi, loader). Terpilih = tint kuning + centang. */
@Composable
fun NuxPickRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 42.dp)
            .clip(shape)
            .background(if (selected) NuxColors.YellowPale else NuxColors.SurfaceWhite, shape)
            .border(
                NuxSizes.GlassBorder,
                if (selected) NuxColors.Yellow.copy(alpha = 0.55f) else NuxColors.CardBorder,
                shape
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (selected) NuxColors.Yellow else NuxColors.DarkGray,
            fontWeight = if (selected) FontWeight.Black else FontWeight.Bold,
            fontSize = 13.sp,
            maxLines = 2,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
        if (selected) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = NuxColors.Yellow,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ----------------------------------------------------------------------------
// Section: ui/components/NuxSidebar.kt
// ----------------------------------------------------------------------------

data class NuxNavItem(
    val id: String,
    val icon: ImageVector,
    val contentDescription: String
)

val NUX_NAV_ITEMS = listOf(
    NuxNavItem("home", Icons.Outlined.Home, "Home"),
    NuxNavItem("accounts", Icons.Outlined.Person, "Akun"),
    NuxNavItem("mods", Icons.Outlined.Extension, "Mod"),
    NuxNavItem("settings", Icons.Outlined.Settings, "Pengaturan")
)

/**
 * Rel navigasi penuh tinggi di tepi kiri layar. Tanpa kotak sendiri: ia berdiri langsung di atas
 * latar hitam layar, jadi menyatu dengan kartu panel di sebelahnya.
 * [startInset] = lebar area kamera/poni di sisi kiri: rel diperlebar sebesar itu
 * sehingga isi rel tetap berada di luar poni.
 *
 * Atas: logo. Tengah: 4 menu (ikon + label, indikator kuning untuk menu aktif).
 * Bawah: tombol info (Tentang & Lisensi).
 */
@Composable
fun NuxSidebar(
    activeTab: String,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    startInset: Dp = 0.dp,
    onOpenAbout: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .width(startInset + 68.dp)
            .fillMaxHeight()
    ) {
        Box(
            modifier = Modifier
                .padding(start = startInset)
                .width(68.dp)
                .fillMaxHeight()
        ) {
            // Logo (ketuk = kembali ke Home)
            val logoShape = RoundedCornerShape(12.dp)
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
                    .size(48.dp)
                    .clip(logoShape)
                    .clickable { onTabSelected("home") },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_novacraft_logo),
                    contentDescription = "NovaCraft Logo",
                    modifier = Modifier.size(40.dp)
                )
            }

            // Menu utama di tengah
            Column(
                modifier = Modifier.align(Alignment.Center),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                NUX_NAV_ITEMS.forEach { item ->
                    val isSelected = activeTab == item.id
                    Column(
                        modifier = Modifier
                            .width(62.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onTabSelected(item.id) }
                            .padding(vertical = 3.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 52.dp, height = 34.dp)
                                .background(
                                    color = if (isSelected) NuxColors.Yellow else Color.Transparent,
                                    shape = RoundedCornerShape(17.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.contentDescription,
                                tint = if (isSelected) (if (AmberUi.isDark) NuxColors.Ink else AmberUi.OnAccent) else NuxColors.GrayNeutral,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.contentDescription,
                            color = if (isSelected) NuxColors.Yellow else NuxColors.GrayNeutral,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }

            // Info: Tentang & Lisensi
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { onOpenAbout() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "Tentang & Lisensi",
                    tint = NuxColors.GrayNeutral,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

// ----------------------------------------------------------------------------
// Section: ui/components/PlayerSkin.kt
// ----------------------------------------------------------------------------

@SuppressLint("SetJavaScriptEnabled")
class PlayerSkin(
    val context: Context,
    localSkinsDir: File = SkinUtils.getSkinsDir(context),
    localCapesDir: File = SkinUtils.getCapesDir(context)
) {
    private val assetLoader = WebViewAssetLoader.Builder()
        .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
        .addPathHandler(
            "/skins/",
            WebViewAssetLoader.InternalStoragePathHandler(context, localSkinsDir)
        )
        .addPathHandler(
            "/capes/",
            WebViewAssetLoader.InternalStoragePathHandler(context, localCapesDir)
        )
        .build()

    private var webview: WebView? = null

    private val skinViewUrl = "https://appassets.androidplatform.net/assets/skinview/skinview.html"
    private val defaultSkinUrl = "https://appassets.androidplatform.net/assets/steve.png"

    fun loadWebView(
        context: Context,
        onPageFinished: () -> Unit = {}
    ): WebView {
        val view = WebView(context).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                // Halaman ini hanya memuat aset lewat WebViewAssetLoader (https), tidak perlu akses berkas/konten
                allowFileAccess = false
                allowContentAccess = false
                loadWithOverviewMode = true
                useWideViewPort = true
            }
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            overScrollMode = WebView.OVER_SCROLL_NEVER
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false

            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest
                ): WebResourceResponse? {
                    return assetLoader.shouldInterceptRequest(request.url)
                }

                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)
                    onPageFinished()
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                    Log.d("SkinViewerConsole", "${consoleMessage.message()} (L${consoleMessage.lineNumber()})")
                    return true
                }
            }

            loadUrl(skinViewUrl)
        }
        this.webview = view
        return view
    }

    fun loadSkin(file: File?, model: String = "classic") {
        if (file != null && file.exists()) {
            runCatching {
                file.inputStream().use { stream ->
                    loadSkinStream(stream, model)
                }
            }.onFailure {
                loadSkinUrl(defaultSkinUrl, model)
            }
        } else {
            loadSkinUrl(defaultSkinUrl, model)
        }
    }

    fun loadSkinStream(inputStream: InputStream?, model: String = "classic") {
        val modelParam = if (model.lowercase() == "slim") "slim" else "default"
        if (inputStream != null) {
            val bytes = inputStream.readBytes()
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val dataUrl = "data:image/png;base64,$base64"
            webview?.evaluateJavascript("loadSkin('$dataUrl', '$modelParam')", null)
        } else {
            loadSkinUrl(defaultSkinUrl, modelParam)
        }
    }

    fun loadSkinUrl(url: String, model: String = "classic") {
        val modelParam = if (model.lowercase() == "slim") "slim" else "default"
        // Alamat bisa datang dari server skin: dikutip aman supaya tanda ' di dalamnya tidak menyuntik skrip
        webview?.evaluateJavascript("loadSkin(${org.json.JSONObject.quote(url)}, '$modelParam')", null)
    }

    fun loadCape(file: File?) {
        if (file != null && file.exists()) {
            runCatching {
                file.inputStream().use { stream ->
                    val bytes = stream.readBytes()
                    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    val dataUrl = "data:image/png;base64,$base64"
                    webview?.evaluateJavascript("loadCape('$dataUrl')", null)
                }
            }.onFailure {
                webview?.evaluateJavascript("loadCape(null)", null)
            }
        } else {
            webview?.evaluateJavascript("loadCape(null)", null)
        }
    }

    fun loadCapeUrl(url: String?) {
        if (!url.isNullOrBlank()) {
            val safeUrl = if (url.startsWith("http://")) url.replace("http://", "https://") else url
            webview?.evaluateJavascript("loadCape(${org.json.JSONObject.quote(safeUrl)})", null)
        } else {
            webview?.evaluateJavascript("loadCape(null)", null)
        }
    }

    fun loadAccount(
        account: com.aistudio.novacraft.launcher.core.utils.UserAccount,
        customSkinFile: File? = null,
        customCapeFile: File? = null
    ) {
        val skinFile = customSkinFile ?: SkinUtils.resolveSkinFile(context, account)
        val capeFile = customCapeFile ?: SkinUtils.resolveCapeFile(context, account)
        val model = account.safeSkinModel

        if (skinFile != null && skinFile.exists() && skinFile.length() > 0) {
            loadSkin(skinFile, model)
        } else if (account.safeAccountType == "elyby") {
            // Skin Ely.by diunduh ke berkas oleh SkinUtils. Memuat alamat jarak jauh langsung ke WebView
            // gagal (CORS + redirect) dan membuat model kosong, jadi sementara tampilkan Steve.
            loadSkin(null, model)
        } else if (!account.skinUrl.isNullOrBlank()) {
            val safeSkinUrl = if (account.skinUrl.startsWith("http://")) {
                account.skinUrl.replace("http://", "https://")
            } else {
                account.skinUrl
            }
            loadSkinUrl(safeSkinUrl, model)
        } else if (!account.isOffline) {
            loadSkinUrl("https://minotar.net/skin/${account.username}", model)
        } else {
            loadSkin(null, model)
        }

        if (capeFile != null && capeFile.exists() && capeFile.length() > 0) {
            loadCape(capeFile)
        } else {
            loadCape(null)
        }
    }

    fun resetSkin() {
        loadSkinUrl(defaultSkinUrl, "classic")
        webview?.evaluateJavascript("loadCape(null)", null)
    }

    fun startAnim(animName: String = "Walking", speed: Float = 0.8f) {
        webview?.evaluateJavascript("startAnim('$animName', $speed)", null)
    }

    fun setCamera(azimuthDeg: Int = 0, pitchDeg: Int = 10, distance: Int = 60) {
        webview?.evaluateJavascript("setAzimuthAndPitch($azimuthDeg, $pitchDeg, $distance)", null)
    }

    fun setAutoRotate(enable: Boolean, speed: Float = 0.8f) {
        webview?.evaluateJavascript("setAutoRotate($enable, $speed)", null)
    }

    fun resetCamera() {
        webview?.evaluateJavascript("resetCamera()", null)
    }

    fun destroy() {
        webview?.apply {
            stopLoading()
            loadUrl("about:blank")
            clearHistory()
            removeAllViews()
            destroy()
        }
        webview = null
    }
}

// ----------------------------------------------------------------------------
// Section: ui/components/PointerSettingsCards.kt
// ----------------------------------------------------------------------------

/**
 * Kartu pengaturan penunjuk (kursor) launcher, satu baris per bentuk kursor game: pratinjau, ganti gambar,
 * atur hotspot dengan stepper presisi, dan kembalikan ke bawaan.
 */
@Composable
fun PointerSettingsCards(
    hotspots: Map<String, Int>,
    onHotspotsChange: (Map<String, Int>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pickShape by remember { mutableStateOf<CursorShape?>(null) }
    var openShape by remember { mutableStateOf<CursorShape?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        val shape = pickShape
        pickShape = null
        if (uri != null && shape != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) { PointerStyle.importImage(context, shape, uri) }
                if (ok) {
                    PointerStyle.bump()
                } else {
                    Toast.makeText(context, "Gambar tidak bisa dibaca", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PointerStyle.shapes.forEach { shape ->
            val title = PointerStyle.title(shape)
            val summary = PointerStyle.summary(shape)
            val savedX = PointerStyle.hotspotX(hotspots, shape)
            val savedY = PointerStyle.hotspotY(hotspots, shape)
            var hotX by remember(shape, savedX) { mutableIntStateOf(savedX) }
            var hotY by remember(shape, savedY) { mutableIntStateOf(savedY) }
            val hasCustomImage = PointerStyle.bitmap(context, shape) != null
            val hasCustomHotspot = PointerStyle.hasHotspot(hotspots, shape)
            val editing = openShape == shape

            SettingFrame(title, summary) {
                val panelShape = RoundedCornerShape(16.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(panelShape)
                        .background(AmberUi.ObsidianSlate.copy(alpha = 0.55f), panelShape)
                        .border(
                            1.dp,
                            if (editing) AmberUi.AccentEdge.copy(alpha = 0.6f) else AmberUi.Border,
                            panelShape
                        )
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PointerPreview(
                            shape = shape,
                            hotX = hotX,
                            hotY = hotY,
                            showHotspot = editing,
                            customized = hasCustomImage || hasCustomHotspot
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        SettingRowText(title = title, desc = summary, modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AmberPillButton(
                            text = if (hasCustomImage) "Ganti gambar" else "Pilih gambar",
                            icon = androidx.compose.material.icons.Icons.Outlined.Image,
                            onClick = {
                                pickShape = shape
                                picker.launch("image/*")
                            },
                            filled = false
                        )
                        AmberPillButton(
                            text = if (editing) "Tutup hotspot" else "Atur hotspot",
                            icon = Icons.Outlined.CenterFocusStrong,
                            onClick = { openShape = if (editing) null else shape },
                            filled = editing
                        )
                        if (hasCustomImage || hasCustomHotspot) {
                            AmberPillButton(
                                text = "Reset",
                                icon = Icons.Outlined.RestartAlt,
                                onClick = {
                                    if (hasCustomImage) PointerStyle.removeImage(context, shape)
                                    if (hasCustomHotspot) onHotspotsChange(PointerStyle.withoutHotspot(hotspots, shape))
                                },
                                filled = false,
                                danger = true
                            )
                        }
                    }
                    if (editing) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AmberUi.ObsidianDeep.copy(alpha = 0.65f))
                                .border(1.dp, AmberUi.Border, RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Hotspot adalah titik yang dihitung sebagai ujung kursor saat mengklik (titik merah di pratinjau).",
                                color = AmberUi.TextDim,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                            HotspotSlider(
                                label = "Hotspot X",
                                value = hotX,
                                onChange = { hotX = it },
                                onFinished = { onHotspotsChange(PointerStyle.withHotspot(hotspots, shape, hotX, hotY)) },
                                onStep = { delta ->
                                    hotX = (hotX + delta).coerceIn(0, 100)
                                    onHotspotsChange(PointerStyle.withHotspot(hotspots, shape, hotX, hotY))
                                }
                            )
                            HotspotSlider(
                                label = "Hotspot Y",
                                value = hotY,
                                onChange = { hotY = it },
                                onFinished = { onHotspotsChange(PointerStyle.withHotspot(hotspots, shape, hotX, hotY)) },
                                onStep = { delta ->
                                    hotY = (hotY + delta).coerceIn(0, 100)
                                    onHotspotsChange(PointerStyle.withHotspot(hotspots, shape, hotX, hotY))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HotspotSlider(
    label: String,
    value: Int,
    onChange: (Int) -> Unit,
    onFinished: () -> Unit,
    onStep: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = AmberUi.TextDim,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(AmberUi.SurfaceRaised)
                    .border(1.dp, AmberUi.BorderStrong, CircleShape)
                    .padding(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "$value%",
                    color = AmberUi.AccentSoft,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            HotspotStepButton(
                icon = Icons.Outlined.Remove,
                description = "Kurangi $label",
                enabled = value > 0,
                onClick = { onStep(-1) }
            )
            Slider(
                value = value.toFloat(),
                onValueChange = { onChange(it.roundToInt()) },
                onValueChangeFinished = onFinished,
                valueRange = 0f..100f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = if (AmberUi.isDark) Color.White else AmberUi.Accent,
                    activeTrackColor = AmberUi.AccentSoft,
                    inactiveTrackColor = AmberUi.BorderStrong,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent
                )
            )
            HotspotStepButton(
                icon = Icons.Outlined.Add,
                description = "Tambah $label",
                enabled = value < 100,
                onClick = { onStep(1) }
            )
        }
    }
}

@Composable
private fun HotspotStepButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
            .clip(CircleShape)
            .background(AmberUi.Surface, CircleShape)
            .border(1.dp, if (enabled) AmberUi.BorderStrong else AmberUi.Border, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (enabled) AmberUi.Text else AmberUi.TextDim.copy(alpha = 0.4f),
            modifier = Modifier.size(18.dp)
        )
    }
}

/** Pratinjau kursor (gambar kustom atau bentuk bawaan); titik merah menandai hotspot saat diatur. */
@Composable
private fun PointerPreview(
    shape: CursorShape,
    hotX: Int,
    hotY: Int,
    showHotspot: Boolean,
    customized: Boolean
) {
    val context = LocalContext.current
    val d = LocalDensity.current.density
    val custom = PointerStyle.bitmap(context, shape)
    val glyph = PointerStyle.glyphFor(shape, d)
    val hx = hotX / 100f
    val hy = hotY / 100f
    Canvas(
        modifier = Modifier
            .size(54.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AmberUi.ObsidianDeep)
            .border(
                1.dp,
                if (customized) AmberUi.AccentEdge.copy(alpha = 0.7f) else AmberUi.BorderStrong,
                RoundedCornerShape(12.dp)
            )
    ) {
        val sizePx = 34f * d
        val offsetPx = (size.width - sizePx) / 2f
        translate(left = offsetPx, top = offsetPx) {
            drawPointer(glyph, custom, d, sizePx, hx, hy, false)
            if (showHotspot) {
                drawCircle(
                    color = Color(0xFFFF3B30),
                    radius = 2.8f * d,
                    center = Offset(hx * sizePx, hy * sizePx)
                )
            }
        }
    }
}

// ----------------------------------------------------------------------------
// Section: ui/components/RustSettingsSection.kt
// ----------------------------------------------------------------------------

/**
 * Isi tab Rust di layar Pengaturan. Pengaturan mesin Rust (Horizon) berbeda dari jalur Java (Fabric, Forge, dan
 * kawan-kawan), jadi punya tab sendiri dan tidak dicampur ke tab Game. Setiap fungsi di sini mengisi satu kelompok
 * (SettingsGroup di SettingsScreen). Nilainya disimpan oleh [RustSettingsStore] ke config milik mesin sendiri dan
 * berlaku pada PLAY berikutnya.
 */

/** Membaca ulang config mesin dari berkas setiap tab dibuka. Tidak menggambar apa pun. */
@Composable
fun RustSettingsLoader() {
    val context = LocalContext.current
    LaunchedEffect(Unit) { RustSettingsStore.reload(context) }
}

/** Baris keterangan: judul + keterangan di kiri, nilai teks di kanan (tidak bisa diketuk). */
@Composable
private fun RustInfoRow(title: String, desc: String?, value: String, warn: Boolean = false) {
    SettingFrame(title, desc) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingRowText(title = title, desc = desc, modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = value,
                color = if (warn) AmberUi.Danger else AmberUi.Text,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

/** Status mesin dan sumber daya game, plus catatan bahwa pengaturan ini hanya untuk instance Rust. */
@Composable
fun RustStatusRows() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val availability = remember { HorizonEngine.availability(context) }
    val installed = remember { HorizonEngine.gameInstalled(context) }
    val liveVersion by HorizonEngine.liveMcVersion.collectAsState()
    val latestMojang26x by HorizonEngine.latestMojang26x.collectAsState()
    var syncingVersion by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        HorizonEngine.refreshLatestVersion(context, forceNetwork = false)
    }

    val unavailable = availability as? HorizonEngine.Availability.Unavailable
    val lowRam = (availability as? HorizonEngine.Availability.Ready)?.lowRam == true
    RustInfoRow(
        title = "Mesin Rust (Horizon)",
        desc = when {
            unavailable != null -> unavailable.reason
            lowRam -> HorizonEngine.LOW_RAM_WARNING
            else -> "Pengganti seluruh client Java: tanpa JVM dan tanpa mod Java."
        },
        value = if (unavailable != null) "Tidak tersedia" else "Siap",
        warn = unavailable != null
    )
    SettingDivider()
    RustInfoRow(
        title = "Sumber daya game",
        desc = "Diunduh otomatis saat PLAY pertama, sekitar 500 MiB.",
        value = if (installed) "Terpasang" else "Belum diunduh"
    )
    SettingDivider()
    SettingActionRow(
        title = "Versi Minecraft Horizon",
        desc = if (!latestMojang26x.isNullOrBlank() && latestMojang26x != liveVersion) {
            "Sinkron real-time: aktif $liveVersion · Mojang 26.x terbaru $latestMojang26x."
        } else {
            "Otomatis menerima pembaruan versi Minecraft $liveVersion secara real-time."
        },
        actionLabel = if (syncingVersion) "..." else "$liveVersion · Cek",
        filled = false,
        onClick = {
            if (!syncingVersion) {
                syncingVersion = true
                scope.launch {
                    val v = HorizonEngine.refreshLatestVersion(context, forceNetwork = true)
                    syncingVersion = false
                    Toast.makeText(context, "Versi aktif Horizon: $v", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )
    SettingDivider()
    SettingFrame(
        "Berlaku untuk instance Rust",
        "Pilihan di tab ini dipakai instance Rust saja dan berlaku pada PLAY berikutnya. Instance Vanilla, Fabric, dan Forge tidak terpengaruh."
    ) {
        SettingRowText(
            title = "Berlaku untuk instance Rust",
            desc = "Pilihan di tab ini dipakai instance Rust saja dan berlaku pada PLAY berikutnya. Instance Vanilla, Fabric, dan Forge tidak terpengaruh.",
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        )
    }
}

/** Renderer dan skala resolusi. */
@Composable
fun RustGraphicsRows() {
    val context = LocalContext.current
    val s by RustSettingsStore.settings.collectAsState()

    SettingChoiceRow(
        title = "Renderer Rust",
        desc = "Kompatibel paling aman untuk semua HP. Vulkan cepat memakai batching GPU. OpenGL ES masih eksperimental.",
        options = listOf(
            RustSettingsStore.RENDERER_COMPAT to "Vulkan kompatibel",
            "vulkan" to "Vulkan cepat",
            "open-gl" to "OpenGL ES"
        ),
        selected = s.renderer,
        onSelect = { value -> RustSettingsStore.update(context) { it.copy(renderer = value) } }
    )
    SettingDivider()
    SettingSliderRow(
        title = "Skala resolusi Rust",
        desc = "Menurunkan resolusi render membuat game lebih ringan, gambar jadi lebih buram.",
        valueText = "${s.renderScale}%",
        value = s.renderScale.toFloat(),
        valueRange = 25f..100f,
        steps = 14,
        onValueChange = { raw ->
            val percent = ((raw / 5f).roundToInt() * 5).coerceIn(25, 100)
            RustSettingsStore.update(context) { it.copy(renderScale = percent) }
        },
        footer = {
            Row(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RustSettingsStore.RENDER_SCALE_PRESETS.forEach { preset ->
                    SettingChip(
                        text = "$preset%",
                        selected = s.renderScale == preset,
                        onClick = { RustSettingsStore.update(context) { it.copy(renderScale = preset) } }
                    )
                }
            }
        }
    )
    SettingDivider()
    SettingSwitchRow(
        title = "Sederhanakan daun (eksperimental)",
        desc = "Menyederhanakan dedaunan yang rapat agar lebih ringan. Tampilan daun berubah dan fitur ini masih eksperimental. Berlaku di PLAY berikutnya.",
        checked = s.leafCulling,
        onCheckedChange = { on -> RustSettingsStore.update(context) { it.copy(leafCulling = on) } }
    )
}

/** Mode performa, animasi, laju layar, petunjuk performa, batas pekerja chunk, dan cache. */
@Composable
fun RustPerformanceRows() {
    val context = LocalContext.current
    val s by RustSettingsStore.settings.collectAsState()

    SettingChoiceRow(
        title = "Mode performa",
        desc = "Normal memakai bawaan mesin. Super hemat menurunkan jarak render, partikel, awan, dan bayangan. Hemat baterai membatasi 60 FPS dengan VSync. FPS maksimum melepas batas FPS dan mematikan VSync. Mode ini mengubah pengaturan video game saat PLAY.",
        options = listOf(
            RustSettingsStore.PERF_NORMAL to "Normal",
            RustSettingsStore.PERF_SUPER to "Super hemat",
            RustSettingsStore.PERF_BATTERY to "Hemat baterai",
            RustSettingsStore.PERF_FPS to "FPS maksimum"
        ),
        selected = s.perfMode,
        onSelect = { value -> RustSettingsStore.update(context) { it.copy(perfMode = value) } }
    )
    SettingDivider()
    SettingSwitchRow(
        title = "Kurangi animasi game",
        desc = "Mematikan tekstur beranimasi, fade chunk, goyangan kamera saat berjalan, dan blur menu. Lebih ringan di HP lemah.",
        checked = s.gameAnimationsOff,
        onCheckedChange = { on -> RustSettingsStore.update(context) { it.copy(gameAnimationsOff = on) } }
    )
    SettingDivider()
    SettingChoiceRow(
        title = "Laju layar",
        desc = "Permintaan ke Android, bukan batas FPS game. Laju yang tidak didukung layar bisa diabaikan.",
        options = RustSettingsStore.DISPLAY_RATES.map { hz -> hz to (if (hz == 0) "Bawaan sistem" else "$hz Hz") },
        selected = s.displayHz,
        onSelect = { hz -> RustSettingsStore.update(context) { it.copy(displayHz = hz) } }
    )
    SettingDivider()
    SettingSwitchRow(
        title = "Petunjuk performa",
        desc = "Memberi tahu Android bahwa game butuh performa stabil. Hanya berguna di HP yang mendukungnya.",
        checked = s.performanceHints,
        onCheckedChange = { on -> RustSettingsStore.update(context) { it.copy(performanceHints = on) } }
    )
    SettingDivider()
    SettingSliderRow(
        title = "Batas pekerja chunk",
        desc = "Batas pekerja yang membuat dan merakit chunk, bukan semua utas aplikasi. Nilai kecil mengurangi beban CPU tetapi memuat dunia lebih lambat.",
        valueText = if (s.workerLimit == 0) "Tanpa batas" else "${s.workerLimit}",
        value = s.workerLimit.toFloat(),
        valueRange = 0f..RustSettingsStore.MAX_WORKERS.toFloat(),
        steps = RustSettingsStore.MAX_WORKERS - 1,
        onValueChange = { raw ->
            val limit = raw.roundToInt().coerceIn(0, RustSettingsStore.MAX_WORKERS)
            RustSettingsStore.update(context) { it.copy(workerLimit = limit) }
        }
    )
    SettingDivider()
    SettingSwitchRow(
        title = "Unggah chunk cepat",
        desc = "Menaikkan jatah unggah mesh chunk ke GPU menjadi 4 MiB per frame, dibuat untuk mengurangi tersendat saat bergerak dan memuat dunia. Menyala secara bawaan. Matikan bila game malah terasa patah-patah. Berlaku di PLAY berikutnya.",
        checked = s.fastChunkUpload,
        onCheckedChange = { on -> RustSettingsStore.update(context) { it.copy(fastChunkUpload = on) } }
    )
    SettingDivider()
    SettingChoiceRow(
        title = "Cache RAM",
        desc = "Suara yang sudah didekode (80%) dan buffer mesh kosong (20%), dibuat ulang dengan kualitas sama. Bukan batas total RAM atau VRAM. Nilai kecil bisa menambah kerja CPU.",
        options = RustSettingsStore.CACHE_BUDGETS.map { mib -> mib to (if (mib == 0) "Bawaan (80 MiB)" else "$mib MiB") },
        selected = s.cacheBudgetMib,
        onSelect = { mib -> RustSettingsStore.update(context) { it.copy(cacheBudgetMib = mib) } }
    )
}

/** Arah layar, rasio, mode layar penuh, dan area poni. */
@Composable
fun RustScreenRows() {
    val context = LocalContext.current
    val s by RustSettingsStore.settings.collectAsState()

    SettingChoiceRow(
        title = "Arah layar",
        desc = "Arah layar saat bermain. Bawaannya landscape.",
        options = listOf(
            "landscape" to "Landscape",
            "reverse-landscape" to "Landscape terbalik",
            "portrait" to "Portrait",
            "reverse-portrait" to "Portrait terbalik",
            "auto" to "Otomatis"
        ),
        selected = s.orientation,
        onSelect = { value -> RustSettingsStore.update(context) { it.copy(orientation = value) } }
    )
    SettingDivider()
    SettingChoiceRow(
        title = "Rasio layar",
        desc = "Membatasi tampilan game ke rasio tertentu. Asli mengikuti rasio layar HP.",
        options = listOf(
            "native" to "Asli",
            "wide" to "16:9",
            "classic" to "4:3",
            "square" to "1:1"
        ),
        selected = s.aspect,
        onSelect = { value -> RustSettingsStore.update(context) { it.copy(aspect = value) } }
    )
    SettingDivider()
    SettingSwitchRow(
        title = "Layar penuh imersif",
        desc = "Menyembunyikan bilah navigasi dan bilah status saat bermain. Mati secara bawaan: di sebagian HP bisa membuat game macet.",
        checked = s.immersive,
        onCheckedChange = { on -> RustSettingsStore.update(context) { it.copy(immersive = on) } }
    )
    SettingDivider()
    SettingSwitchRow(
        title = "Hindari poni layar",
        desc = "Menggeser tampilan game agar tidak tertutup lubang kamera atau poni.",
        checked = s.avoidCutout,
        onCheckedChange = { on -> RustSettingsStore.update(context) { it.copy(avoidCutout = on) } }
    )
}

/** Perangkat input utama mesin. */
@Composable
fun RustControlRows() {
    val context = LocalContext.current
    val s by RustSettingsStore.settings.collectAsState()
    val launcher by SettingsManager.settings.collectAsState()

    SettingSwitchRow(
        title = "Kontrol NovaCraft",
        desc = "Posisi, ukuran, dan tombol layar mengikuti editor kontrol NovaCraft, jadi hanya ada satu set kontrol. Matikan untuk memakai kontrol bawaan mesin. Berlaku di PLAY berikutnya.",
        checked = launcher.rustAmberControls,
        onCheckedChange = { on ->
            SettingsManager.updateSettings(context, SettingsManager.settings.value.copy(rustAmberControls = on))
        }
    )
    SettingDivider()
    SettingSwitchRow(
        title = "Menu garis tiga",
        desc = "Tombol bergaris tiga di layar game yang membuka menu cepat mesin (preset, kontrol, pengaturan). Warnanya mengikuti tema NovaCraft. Berlaku di PLAY berikutnya.",
        checked = launcher.rustControlHub,
        onCheckedChange = { on ->
            SettingsManager.updateSettings(context, SettingsManager.settings.value.copy(rustControlHub = on))
        }
    )
    SettingDivider()
    SettingChoiceRow(
        title = "Perangkat input",
        desc = "Otomatis memilih sendiri antara layar sentuh, keyboard dan mouse, atau gamepad sesuai yang terpasang.",
        options = listOf(
            "auto" to "Otomatis",
            "touch" to "Layar sentuh",
            "keyboard-mouse" to "Keyboard dan mouse",
            "controller" to "Gamepad"
        ),
        selected = s.inputHost,
        onSelect = { value -> RustSettingsStore.update(context) { it.copy(inputHost = value) } }
    )
}

/** Rekaman performa untuk laporan masalah: dinyalakan, dimainkan sebentar, lalu dibagikan. */
@Composable
fun RustDiagnosticRows() {
    val context = LocalContext.current
    val s by RustSettingsStore.settings.collectAsState()

    SettingSwitchRow(
        title = "Rekam performa",
        desc = "Merekam waktu frame dan pembuatan chunk saat bermain (sampai 30 menit) ke laporan di HP ini. Laporan tidak berisi akun, chat, atau nama dunia. Perekaman menambah sedikit beban, jadi matikan lagi bila tidak diperlukan. Berlaku di PLAY berikutnya.",
        checked = s.recordPerformance,
        onCheckedChange = { on -> RustSettingsStore.update(context) { it.copy(recordPerformance = on) } }
    )
    SettingDivider()
    SettingActionRow(
        title = "Bagikan laporan performa",
        desc = "Mengirim laporan rekaman terakhir lewat aplikasi pilihanmu, misalnya untuk dilampirkan saat melaporkan game yang lambat.",
        actionLabel = "Bagikan",
        onClick = { RustPerformanceReport.share(context) }
    )
}

/** Server yang langsung dimasuki saat PLAY, dan tombol setel ulang. */
@Composable
fun RustGameRows() {
    val context = LocalContext.current
    val s by RustSettingsStore.settings.collectAsState()

    // Teks isian dipegang di sini agar kursor tidak melompat; disamakan ulang bila config selesai dibaca.
    var text by remember { mutableStateOf(s.server) }
    LaunchedEffect(s.server) {
        if (RustSettingsStore.cleanServer(text) != s.server) text = s.server
    }

    SettingFrame(
        "Server otomatis",
        "Kosongkan untuk bermain di dunia sendiri. Bila diisi, PLAY langsung masuk ke server ini."
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 9.dp)
        ) {
            SettingRowText(
                title = "Server otomatis",
                desc = "Kosongkan untuk bermain di dunia sendiri. Bila diisi, PLAY langsung masuk ke server ini."
            )
            Spacer(modifier = Modifier.height(8.dp))
            NuxTextField(
                value = text,
                onValueChange = { raw ->
                    val cleaned = RustSettingsStore.cleanServer(raw)
                    text = cleaned
                    RustSettingsStore.update(context) { it.copy(server = cleaned) }
                },
                placeholder = "Contoh: mc.contoh.com:25565"
            )
        }
    }
    SettingDivider()
    SettingActionRow(
        title = "Setel ulang pengaturan Rust",
        desc = "Mengembalikan semua pilihan di tab ini ke bawaan mesin. Instance dan dunia tidak disentuh.",
        actionLabel = "Setel ulang",
        onClick = {
            RustSettingsStore.reset(context)
            text = ""
            Toast.makeText(context, "Pengaturan Rust dikembalikan ke bawaan.", Toast.LENGTH_SHORT).show()
        },
        danger = true
    )
}

// ----------------------------------------------------------------------------
// Section: ui/components/UpdateSettingsSection.kt
// ----------------------------------------------------------------------------

/**
 * Informasi versi pada tab Tentang > Pembaruan.
 * Pembaruan otomatis dan koneksi ke GitHub telah diputuskan.
 */
@Composable
fun UpdateSettingsSection() {
    val context = LocalContext.current

    val versionName = remember { UpdateChecker.currentVersionName(context) }
    val versionCode = remember { UpdateChecker.currentVersionCode(context) }
    val bits = remember { if (UpdateChecker.wants32BitApk()) "32-bit" else "64-bit" }

    SettingRowText(
        title = "Versi terpasang",
        desc = "$versionName · build $versionCode · $bits"
    )
    SettingDivider()
    SettingRowText(
        title = "Pembaruan GitHub",
        desc = "Terputus — pemeriksaan pembaruan otomatis dari GitHub dinonaktifkan."
    )
    Text(
        text = "Launcher berjalan secara mandiri tanpa terhubung ke repositori pembaruan GitHub.",
        color = NuxColors.GrayNeutral,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        modifier = Modifier.padding(top = 6.dp)
    )
}

// ============================================================================
// Section: NovaUiControlCore.kt
// ============================================================================

// ----------------------------------------------------------------------------
// Section: ui/control/ControlOverlay.kt
// ----------------------------------------------------------------------------

/**
 * Lapisan semua tombol layar di atas game. Tiap tombol hanya satu simpul yang menggambar dirinya
 * sendiri di kanvas ([ControlSkin]); posisi dihitung saat tata letak, bukan saat penyusunan.
 * Angka FPS dibaca lewat lambda di dalam teks FPS saja, jadi pembaruan 2x per detik tidak
 * menyusun ulang tombol lain. Tombol gerigi hanya membuka menu; keluar dari game lewat tombol
 * merah di dalam menu.
 *
 * Tiap tombol punya lapisan gambar (RenderNode) sendiri. Tanpa itu, satu tombol yang berubah
 * (ditekan, angka FPS baru, kenop joystick bergeser) membuat thread utama merekam ulang gambar
 * SEMUA tombol, jadi makin banyak tombol makin berat tiap sentuhan. Dengan lapisan sendiri hanya
 * tombol yang berubah yang direkam ulang; tombol lain cukup memakai daftar gambar yang sudah ada.
 *
 * Dua mode tampil: tiap tombol biasa punya [CustomControlButton.showMode] (selalu / hanya saat main /
 * hanya saat menu). [inMenu] true bila game sedang menampilkan kursor (inventori, chat, jeda). Tombol
 * yang tidak cocok dengan mode saat ini tidak disusun sama sekali, jadi tidak membebani layar.
 */
@Composable
fun GameControlsLayer(
    buttons: List<CustomControlButton>,
    screenWidth: Float,
    screenHeight: Float,
    controlsVisible: Boolean,
    fpsMode: FpsMode,
    consoleVisible: Boolean,
    keyboardActive: Boolean,
    fps: () -> Int,
    perfText: () -> String,
    perfWarn: () -> Boolean,
    onFpsClick: () -> Unit,
    onToggleKeyboard: () -> Unit,
    onToggleControls: () -> Unit,
    onOpenMenu: () -> Unit,
    // Game sedang menampilkan kursor (menu/GUI). Dibaca dari status kursor game, bukan dari layar.
    inMenu: Boolean = false,
    buttonWidthScale: Float = 1f,
    buttonOpacityScale: Float = 1f,
    uppercaseLabels: Boolean = true
) {
    val density = LocalDensity.current
    val safeWScale = buttonWidthScale.coerceIn(0.5f, 2f)
    val safeOScale = buttonOpacityScale.coerceIn(0.1f, 1f)
    buttons.forEach { rawBtn ->
        key(rawBtn.id) {
            val btn = if (!rawBtn.isSystem && (safeWScale != 1f || safeOScale != 1f)) {
                val newW = (rawBtn.widthDp * safeWScale).roundToInt().coerceIn(24, 260)
                val newH = if (rawBtn.isJoystick) (rawBtn.heightDp * safeWScale).roundToInt().coerceIn(48, 280) else rawBtn.heightDp
                rawBtn.copy(
                    widthDp = newW,
                    heightDp = newH,
                    opacity = (rawBtn.opacity * safeOScale).coerceIn(0.08f, 1f)
                )
            } else rawBtn
            val widthPx = with(density) { btn.widthDp.dp.toPx() }
            val heightPx = with(density) { btn.heightDp.dp.toPx() }
            val left = (screenWidth * (btn.xPercent / 100f) - widthPx / 2f)
                .coerceIn(0f, (screenWidth - widthPx).coerceAtLeast(0f))
            val top = (screenHeight * (btn.yPercent / 100f) - heightPx / 2f)
                .coerceIn(0f, (screenHeight - heightPx).coerceAtLeast(0f))
            // graphicsLayer harus sebelum size/drawBehind milik tombol, supaya gambarnya masuk ke lapisan ini.
            val place = Modifier
                .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
                .graphicsLayer { }

            if (btn.isSystem) {
                when (btn.systemAction) {
                    "FPS" -> {
                        val show = when (fpsMode) {
                            FpsMode.NORMAL -> controlsVisible && !consoleVisible
                            FpsMode.PINNED -> !consoleVisible
                            FpsMode.SHOW_LOG -> false
                        }
                        if (show) {
                            FpsText(btn, place, fpsMode == FpsMode.PINNED, fps, perfText, perfWarn, onFpsClick)
                        }
                    }
                    "KEYBOARD" -> if (controlsVisible) {
                        SystemIconButton(btn, place, active = keyboardActive, onTap = onToggleKeyboard)
                    }
                    // Selalu tampil supaya tombol layar bisa dimunculkan lagi.
                    "HIDE_GUI" -> SystemIconButton(btn, place, slash = !controlsVisible, onTap = onToggleControls)
                    // Gerigi: hanya membuka menu. "CLOSE" = nama lama tombol ini di tata letak tersimpan.
                    "MENU", "CLOSE" -> if (controlsVisible) {
                        SystemIconButton(btn, place, onTap = onOpenMenu)
                    }
                }
            } else if (controlsVisible && ControlShowMode.visible(btn.showMode, inMenu)) {
                if (btn.isJoystick) {
                    CustomVirtualJoystick(button = btn, modifier = place)
                } else {
                    CustomVirtualButton(button = btn, modifier = place, uppercaseLabels = uppercaseLabels)
                }
            }
            Unit
        }
    }
}

/**
 * Wadah makro "ketik perintah". Mengetik / atau T membuka chat, yang mengganti mode ke menu dan
 * bisa membuat tombolnya hilang di tengah jalan; kalau pengetikan ikut terhenti bersama tombolnya,
 * perintahnya setengah terkirim. Karena itu pengetikan jalan di wadah sendiri, bukan di komposisi tombol.
 */
private val macroTypingScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

private val fpsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    fontFeatureSettings = "tnum"
}

/**
 * Angka FPS minimalis: teks saja tanpa kotak atau garis. Warnanya mengikuti kondisi (putih,
 * kuning, merah muda). Info RAM, panas, atau beban UI hanya muncul bila perlu. Ketuk membuka menu.
 *
 * Semua nilai dibaca di dalam fase gambar saja: angka baru hanya menggambar ulang teks ini, tanpa
 * menyusun ulang atau mengukur ulang tata letak.
 */
@Composable
private fun FpsText(
    btn: CustomControlButton,
    modifier: Modifier,
    pinned: Boolean,
    fps: () -> Int,
    perfText: () -> String,
    perfWarn: () -> Boolean,
    onClick: () -> Unit
) {
    val click by rememberUpdatedState(onClick)
    Box(
        modifier = modifier
            .size(width = btn.widthDp.dp, height = btn.heightDp.dp)
            .drawBehind { drawFpsReadout(fps(), pinned, if (perfWarn()) perfText() else "") }
            .pointerInput(Unit) { detectTapGestures { click() } }
    )
}

private fun drawFpsPiece(cv: android.graphics.Canvas, s: String, x: Float, y: Float, size: Float, spacing: Float, color: Color, shadowPx: Float) {
    fpsPaint.textSize = size
    fpsPaint.letterSpacing = spacing
    fpsPaint.color = Color.Black.copy(alpha = color.alpha * 0.55f).toArgb()
    cv.drawText(s, x, y + shadowPx, fpsPaint)
    fpsPaint.color = color.toArgb()
    cv.drawText(s, x, y, fpsPaint)
}

private fun DrawScope.drawFpsReadout(value: Int, pinned: Boolean, extra: String) {
    val tint = when {
        value <= 0 -> Color.White.copy(alpha = 0.45f)
        value >= 50 -> Color.White.copy(alpha = 0.86f)
        value >= 25 -> ControlSkin.YellowSoft
        else -> Color(0xFFFFA399)
    }
    val numSize = 14.dp.toPx()
    val labSize = 8.dp.toPx()
    val gap = 3.dp.toPx()
    val shadow = 1.dp.toPx()
    val num = if (value > 0) value.toString() else "--"

    fpsPaint.textSize = numSize
    fpsPaint.letterSpacing = 0f
    val numW = fpsPaint.measureText(num)
    fpsPaint.textSize = labSize
    fpsPaint.letterSpacing = 0.1f
    val labW = fpsPaint.measureText("FPS")
    val x0 = (size.width - (numW + gap + labW)) / 2f
    val base = size.height / 2f + numSize * 0.36f

    drawIntoCanvas { c ->
        val cv = c.nativeCanvas
        drawFpsPiece(cv, num, x0, base, numSize, 0f, tint, shadow)
        drawFpsPiece(
            cv, "FPS", x0 + numW + gap, base, labSize, 0.1f,
            if (pinned) ControlSkin.Yellow else Color.White.copy(alpha = 0.50f), shadow
        )
        if (extra.isNotEmpty()) {
            drawFpsPiece(
                cv, extra, x0 + numW + gap + labW + 6.dp.toPx(), base, labSize, 0f,
                ControlSkin.Yellow.copy(alpha = 0.85f), shadow
            )
        }
    }
}

/** Tombol sistem berbentuk ikon (keyboard, sembunyikan GUI, gerigi menu). */
@Composable
private fun SystemIconButton(
    btn: CustomControlButton,
    modifier: Modifier,
    active: Boolean = false,
    slash: Boolean = false,
    onTap: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val spec = remember(btn, slash) { skinSpecFor(btn, slash = slash) }
    val tap by rememberUpdatedState(onTap)

    Box(
        modifier = modifier
            .size(width = btn.widthDp.dp, height = btn.heightDp.dp)
            .drawBehind { drawControlButton(spec, pressed || active) }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = { tap() }
                )
            }
    )
}

/**
 * Tombol virtual biasa, widget gulir hotbar, dan tombol makro. Tombol serang/pakai/lompat bisa
 * digeser sambil ditahan untuk memutar kamera (satu jempol untuk menggali sambil membidik), tetapi
 * hanya bila "Geser kamera dari tombol" dinyalakan di Pengaturan. Bawaannya mati: jari yang sedikit
 * bergeser saat menahan tombol (PvP) tidak boleh menggeser pandangan. Memutar kamera dengan jari
 * lain di area kosong layar tetap bekerja seperti biasa.
 */
@Composable
fun CustomVirtualButton(
    button: CustomControlButton,
    modifier: Modifier = Modifier,
    uppercaseLabels: Boolean = true
) {
    var isPressed by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var turboJob by remember { mutableStateOf<Job?>(null) }
    val spec = remember(button, uppercaseLabels) { skinSpecFor(button, uppercaseLabels = uppercaseLabels) }

    // Nilai terbaru tombol dibaca saat dilepas dari layar, bukan salinan saat pertama disusun.
    val currentButton by rememberUpdatedState(button)

    DisposableEffect(button.id) {
        onDispose {
            turboJob?.cancel()
            // Tombol kunci (toggle) yang sedang aktif dilepas saat tombolnya hilang dari layar
            // (mis. kontrol disembunyikan). Tanpa ini tombol (Shift/Ctrl, dll.) tetap tertahan di
            // game padahal tombol di layar sudah terlihat mati, dan butuh dua ketukan untuk memperbaikinya.
            // Kombinasi tombol yang sedang ditahan juga dilepas (mis. tombol hilang karena mode
            // berganti ke menu saat jari masih di atasnya), kalau tidak tombolnya tetap tertahan.
            val b = currentButton
            val plainToggle = !b.isScroll && (
                (b.isToggle && !(b.isMacro && b.macroType == "COMMAND")) ||
                    (b.isMacro && b.macroType == "COMBO")
                )
            if (plainToggle && isPressed) {
                if (b.isMacro && b.macroType == "COMBO") {
                    val keys = if (b.macroComboKeys.isNotEmpty()) b.macroComboKeys
                    else if (b.macroComboKey != 0) listOf(b.macroComboKey) else emptyList()
                    if (b.isMouseButton) CallbackBridge.putMouseEvent(b.mouseButton, false)
                    else CallbackBridge.sendKeyPress(b.keyCode, false)
                    for (k in keys.asReversed()) CallbackBridge.sendKeyPress(k, false)
                } else if (b.isMouseButton) {
                    CallbackBridge.putMouseEvent(b.mouseButton, false)
                } else {
                    CallbackBridge.sendKeyPress(b.keyCode, false)
                }
            }
        }
    }

    if (button.isScroll) {
        val density = LocalDensity.current
        val thresholdPx = with(density) { 14.dp.toPx() }

        Box(
            modifier = modifier
                .size(width = button.widthDp.dp, height = button.heightDp.dp)
                .drawBehind { drawControlScroll(spec, isPressed) }
                .pointerInput(button.id) {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            isPressed = true
                            val startY = down.position.y
                            var totalDragY = 0f
                            var hasDragged = false

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstFast { it.id == down.id } ?: break
                                if (!change.pressed) {
                                    if (!hasDragged) {
                                        // Ketuk: setengah atas gulir naik, setengah bawah gulir turun
                                        val h = size.height
                                        if (startY < h / 2f) {
                                            CallbackBridge.sendScroll(0.0, 1.0)
                                        } else {
                                            CallbackBridge.sendScroll(0.0, -1.0)
                                        }
                                    }
                                    break
                                }

                                val dy = change.position.y - change.previousPosition.y
                                totalDragY += dy
                                if (abs(totalDragY) > 6f) {
                                    hasDragged = true
                                }

                                if (totalDragY <= -thresholdPx) {
                                    // Geser ke atas: slot hotbar sebelumnya
                                    CallbackBridge.sendScroll(0.0, 1.0)
                                    totalDragY = 0f
                                } else if (totalDragY >= thresholdPx) {
                                    // Geser ke bawah: slot hotbar berikutnya
                                    CallbackBridge.sendScroll(0.0, -1.0)
                                    totalDragY = 0f
                                }
                                change.consume()
                            }
                            isPressed = false
                        }
                    }
                }
        )
        return
    }

    fun sendPress(down: Boolean) {
        if (button.isMouseButton) {
            CallbackBridge.putMouseEvent(button.mouseButton, down)
        } else {
            CallbackBridge.sendKeyPress(button.keyCode, down)
        }
    }

    // Tombol biasa (bukan toggle, bukan makro): tahan = aktif.
    val simple = !button.isMacro && !button.isToggle
    // Tombol yang boleh memutar kamera bila digeser (hanya bila dinyalakan di Pengaturan, lihat di bawah).
    val lookDrag = button.isMouseButton || button.keyCode == LwjglGlfwKeycode.GLFW_KEY_SPACE

    val base = modifier
        .size(width = button.widthDp.dp, height = button.heightDp.dp)
        .drawBehind { drawControlButton(spec, isPressed) }

    Box(
        modifier = if (simple) {
            base.pointerInput(button.id, button.isMouseButton, button.mouseButton, button.keyCode) {
                val slop = 6.dp.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    isPressed = true
                    sendPress(true)
                    // Dibaca tiap jari menyentuh tombol, jadi mengubah pengaturan langsung berlaku.
                    val canLook = lookDrag && SettingsManager.settings.value.buttonLookDrag
                    var last = down.position
                    var looking = false
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstFast { it.id == down.id } ?: break
                            if (!change.pressed) {
                                change.consume()
                                break
                            }
                            val delta = change.position - last
                            last = change.position
                            if (canLook) {
                                if (!looking && (change.position - down.position).getDistance() > slop) {
                                    looking = true
                                }
                                if (looking && ZLBridgeStates.cursorMode.value == CURSOR_DISABLED) {
                                    val sens = SettingsManager.settings.value.captureSensitivity / 100f
                                    CallbackBridge.sendCursorDelta(delta.x * sens, delta.y * sens)
                                }
                            }
                            change.consume()
                        }
                    } finally {
                        isPressed = false
                        sendPress(false)
                    }
                }
            }
        } else {
            base.pointerInput(button.id, button.isToggle, button.isMacro, button.macroType, button.macroCommand, button.macroComboKey, button.macroComboKeys, button.macroTurboIntervalMs) {
                detectTapGestures(
                    onPress = {
                        if (button.isMacro) {
                            when (button.macroType) {
                                "COMMAND" -> {
                                    val cmd = button.macroCommand.trim()
                                    if (cmd.isNotEmpty()) {
                                        isPressed = true
                                        macroTypingScope.launch {
                                            try {
                                                if (cmd.startsWith("/")) {
                                                    CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_SLASH, true)
                                                    delay(20)
                                                    CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_SLASH, false)
                                                    delay(50)
                                                    val chars = cmd.substring(1)
                                                    for (ch in chars) {
                                                        CallbackBridge.sendChar(ch, 0)
                                                        delay(10)
                                                    }
                                                } else {
                                                    CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_T, true)
                                                    delay(20)
                                                    CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_T, false)
                                                    delay(50)
                                                    for (ch in cmd) {
                                                        CallbackBridge.sendChar(ch, 0)
                                                        delay(10)
                                                    }
                                                }
                                                delay(40)
                                                CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_ENTER, true)
                                                delay(25)
                                                CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_ENTER, false)
                                            } finally {
                                                delay(50)
                                                isPressed = false
                                            }
                                        }
                                        tryAwaitRelease()
                                    }
                                }
                                "COMBO" -> {
                                    val comboKeys = if (button.macroComboKeys.isNotEmpty()) {
                                        button.macroComboKeys
                                    } else if (button.macroComboKey != 0) {
                                        listOf(button.macroComboKey)
                                    } else emptyList()

                                    if (button.isToggle) {
                                        isPressed = !isPressed
                                        if (isPressed) {
                                            for (k in comboKeys) {
                                                CallbackBridge.sendKeyPress(k, true)
                                                delay(12)
                                            }
                                            sendPress(true)
                                        } else {
                                            sendPress(false)
                                            for (k in comboKeys.reversed()) {
                                                CallbackBridge.sendKeyPress(k, false)
                                                delay(12)
                                            }
                                        }
                                    } else {
                                        isPressed = true
                                        for (k in comboKeys) {
                                            CallbackBridge.sendKeyPress(k, true)
                                            delay(12)
                                        }
                                        sendPress(true)
                                        tryAwaitRelease()
                                        sendPress(false)
                                        for (k in comboKeys.reversed()) {
                                            CallbackBridge.sendKeyPress(k, false)
                                            delay(12)
                                        }
                                        isPressed = false
                                    }
                                }
                                "TURBO" -> {
                                    val interval = button.macroTurboIntervalMs.coerceAtLeast(30L)
                                    val downTime = (interval / 2).coerceAtLeast(15L)
                                    val upTime = (interval - downTime).coerceAtLeast(15L)

                                    if (button.isToggle) {
                                        isPressed = !isPressed
                                        if (isPressed) {
                                            turboJob = coroutineScope.launch {
                                                while (isActive) {
                                                    sendPress(true)
                                                    delay(downTime)
                                                    sendPress(false)
                                                    delay(upTime)
                                                }
                                            }
                                        } else {
                                            turboJob?.cancel()
                                            turboJob = null
                                            sendPress(false)
                                        }
                                    } else {
                                        isPressed = true
                                        val job = coroutineScope.launch {
                                            while (isActive) {
                                                sendPress(true)
                                                delay(downTime)
                                                sendPress(false)
                                                delay(upTime)
                                            }
                                        }
                                        tryAwaitRelease()
                                        job.cancel()
                                        sendPress(false)
                                        isPressed = false
                                    }
                                }
                                else -> {
                                    if (button.isToggle) {
                                        isPressed = !isPressed
                                        sendPress(isPressed)
                                    } else {
                                        isPressed = true
                                        sendPress(true)
                                        tryAwaitRelease()
                                        isPressed = false
                                        sendPress(false)
                                    }
                                }
                            }
                        } else {
                            if (button.isToggle) {
                                isPressed = !isPressed
                                sendPress(isPressed)
                            } else {
                                isPressed = true
                                sendPress(true)
                                tryAwaitRelease()
                                isPressed = false
                                sendPress(false)
                            }
                        }
                    }
                )
            }
        }
    )
}

/**
 * Joystick WASD dengan kunci lari: dorong sampai tepi lingkaran supaya lari otomatis (Ctrl
 * ditahan) selama maju masih ditahan. Dilepas = berhenti. Lari otomatis bisa dimatikan per joystick
 * di editor tombol ([CustomControlButton.autoSprint]).
 */
@Composable
fun CustomVirtualJoystick(
    button: CustomControlButton,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val sizePx = with(density) { button.widthDp.dp.toPx() }
    val radiusPx = sizePx / 2f
    val knobRadiusPx = radiusPx * 0.36f
    val maxDragDistance = radiusPx - knobRadiusPx

    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    var isTouching by remember { mutableStateOf(false) }
    var sprint by remember { mutableStateOf(false) }
    // Dibaca saat jari bergerak, jadi perubahan dari editor langsung dipakai tanpa menyusun ulang.
    val autoSprint by rememberUpdatedState(button.autoSprint)

    // Posisi kenop yang terakhir digambar. Kenop hanya digambar ulang bila bergeser cukup jauh:
    // jari yang ditahan hampir selalu bergetar 1-2 piksel, dan tiap penggambaran ulang berarti
    // satu frame UI penuh yang berebut GPU dan CPU dengan game.
    val shown = remember { FloatArray(2) }
    val knobStepPx = with(density) { 2.5.dp.toPx() }

    // Hitungan arah dan lari (zona mati, titik tumpu, histeresis) ada di JoystickLogic.
    val logic = remember { JoystickLogic() }

    // Status tombol arah disimpan biasa (bukan state) supaya menggeser kenop tidak menyusun ulang.
    val keys = remember { BooleanArray(4) } // W, A, S, D
    val ctrlDown = remember { BooleanArray(1) }

    fun updateKeys(needW: Boolean, needA: Boolean, needS: Boolean, needD: Boolean) {
        if (needW != keys[0]) {
            keys[0] = needW
            CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_W, needW)
        }
        if (needA != keys[1]) {
            keys[1] = needA
            CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_A, needA)
        }
        if (needS != keys[2]) {
            keys[2] = needS
            CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_S, needS)
        }
        if (needD != keys[3]) {
            keys[3] = needD
            CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_D, needD)
        }
    }

    fun setSprint(on: Boolean) {
        if (on != ctrlDown[0]) {
            ctrlDown[0] = on
            CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_LEFT_CONTROL, on)
        }
        if (sprint != on) sprint = on
    }

    fun releaseAll() {
        logic.release()
        updateKeys(false, false, false, false)
        setSprint(false)
        shown[0] = 0f
        shown[1] = 0f
        knobOffset = Offset.Zero
        isTouching = false
    }

    // Terapkan hasil JoystickLogic: gambar ulang kenop hanya bila bergeser cukup jauh, kirim tombol
    // hanya bila berubah.
    fun applyLogic() {
        val kx = logic.knobX
        val ky = logic.knobY
        val ddx = kx - shown[0]
        val ddy = ky - shown[1]
        if (ddx * ddx + ddy * ddy >= knobStepPx * knobStepPx) {
            shown[0] = kx
            shown[1] = ky
            knobOffset = Offset(kx, ky)
        }
        updateKeys(logic.w, logic.a, logic.s, logic.d)
        setSprint(logic.sprint)
    }

    DisposableEffect(button.id) {
        onDispose {
            releaseAll()
        }
    }

    Box(
        modifier = modifier
            .size(button.widthDp.dp)
            .drawBehind { drawControlJoystick(button.opacity, knobOffset, isTouching, sprint) }
            .pointerInput(button.id) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        isTouching = true
                        logic.begin(
                            down.position.x, down.position.y,
                            size.width / 2f, size.height / 2f,
                            radiusPx, maxDragDistance
                        )
                        logic.update(down.position.x, down.position.y, autoSprint)
                        applyLogic()

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstFast { it.id == down.id } ?: break
                            if (!change.pressed) {
                                break
                            }
                            logic.update(change.position.x, change.position.y, autoSprint)
                            applyLogic()
                            change.consume()
                        }
                        releaseAll()
                    }
                }
            }
    )
}

// ----------------------------------------------------------------------------
// Section: ui/control/ControlSkin.kt
// ----------------------------------------------------------------------------

/**
 * Tampilan semua tombol layar: kaca sangat tipis dengan garis halus, berubah aksen Nova Cyan saat
 * ditekan atau dipilih. Tombol sistem (menu, keyboard, sembunyikan GUI) tanpa garis luar: ikon di
 * atas bulatan kaca obsidian tipis. Dipakai bersama oleh game dan editor tata letak supaya yang
 * terlihat di editor sama persis dengan di dalam game.
 *
 * Semua digambar langsung di kanvas (tanpa lapisan transparansi per tombol, tanpa gambar atau
 * font tambahan) dan status tekan dibaca hanya saat menggambar, jadi menekan tombol tidak
 * menyusun ulang apa pun.
 */
object ControlSkin {
    val Ink = Amber.Ink
    val Yellow = Amber.Yellow
    val YellowSoft = Amber.YellowSoft
}

/** Ringkasan satu tombol untuk digambar. */
@Immutable
class SkinSpec(
    val label: String,
    val icon: String,
    val primary: Boolean,
    val system: Boolean,
    val opacity: Float,
    val cornerDp: Float,
    val slash: Boolean = false
)

fun skinSpecFor(
    b: CustomControlButton,
    iconOverride: String? = null,
    slash: Boolean = false,
    uppercaseLabels: Boolean = true
): SkinSpec {
    val icon = iconOverride ?: when {
        b.isSystem && (b.systemAction == "CLOSE" || b.systemAction == "MENU") -> "gear"
        b.icon.isNotBlank() -> b.icon
        b.isSystem -> when (b.systemAction) {
            "KEYBOARD" -> "keyboard"
            "HIDE_GUI" -> "eye"
            // "CLOSE" adalah nama lama tombol keluar; sekarang tombol ini hanya membuka menu.
            "CLOSE", "MENU" -> "gear"
            else -> ""
        }
        else -> ""
    }
    val displayLabel = if (!b.isSystem && !uppercaseLabels) {
        b.name.lowercase(Locale.getDefault())
    } else {
        b.name
    }
    return SkinSpec(
        label = displayLabel,
        icon = icon,
        primary = !b.isSystem && b.heightDp >= 56,
        system = b.isSystem,
        opacity = b.opacity.coerceIn(0.08f, 1f),
        cornerDp = b.cornerRadiusDp.toFloat(),
        slash = slash
    )
}

// Huruf bobot sedang dengan jarak antarhuruf sedikit renggang: lebih tenang daripada huruf tebal.
private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    textAlign = Paint.Align.CENTER
    letterSpacing = 0.06f
}

/** Tombol biasa: isian kaca, garis putih; ditekan atau aktif = kuning penuh. */
fun DrawScope.drawControlButton(spec: SkinSpec, pressed: Boolean, selected: Boolean = false) {
    drawButtonBase(spec, pressed, selected)

    val w = size.width
    val h = size.height
    val k = spec.opacity
    val glyph = when {
        spec.system -> if (pressed) ControlSkin.Yellow else Color.White.copy(alpha = 0.58f + 0.32f * k)
        pressed -> ControlSkin.Ink
        else -> Color.White.copy(alpha = 0.45f + 0.40f * k)
    }
    if (spec.icon.isNotEmpty()) {
        val s = min(w, h) * (if (spec.primary) 0.52f else if (spec.system) 0.56f else 0.58f)
        drawControlIcon(spec.icon, w / 2f, h / 2f, s, glyph, spec.slash)
    } else {
        drawControlLabel(spec.label, w / 2f, h / 2f, w * 0.86f, h, glyph)
    }
}

/** Widget gulir hotbar: dua panah dan nama kecil di tengah. */
fun DrawScope.drawControlScroll(spec: SkinSpec, pressed: Boolean, selected: Boolean = false) {
    drawButtonBase(spec, pressed, selected)

    val w = size.width
    val h = size.height
    val k = spec.opacity
    val glyph = if (pressed) ControlSkin.Ink else Color.White.copy(alpha = 0.45f + 0.40f * k)
    val s = min(w * 0.56f, h * 0.24f)
    drawControlIcon("chev_up", w / 2f, h * 0.22f, s, glyph)
    drawControlIcon("chev_down", w / 2f, h * 0.78f, s, glyph)
    if (spec.label.isNotEmpty()) {
        drawControlLabel(spec.label.take(6), w / 2f, h / 2f, w * 0.80f, h * 0.5f, glyph.copy(alpha = glyph.alpha * 0.75f))
    }
}

private fun DrawScope.drawButtonBase(spec: SkinSpec, pressed: Boolean, selected: Boolean) {
    val w = size.width
    val h = size.height
    val k = spec.opacity
    val radius = min(spec.cornerDp.dp.toPx(), min(w, h) / 2f)

    if (spec.system) {
        // Tombol sistem: bulatan gelap tipis tanpa garis; saat ditekan atau aktif jadi tint kuning.
        val ghost = when {
            pressed -> ControlSkin.Yellow.copy(alpha = 0.26f)
            selected -> ControlSkin.Yellow.copy(alpha = 0.16f)
            else -> Color.Black.copy(alpha = 0.20f * (0.4f + 0.6f * k))
        }
        drawRoundRect(color = ghost, cornerRadius = CornerRadius(radius))
        if (selected) {
            val ow = 1.6f.dp.toPx()
            drawRoundRect(
                color = ControlSkin.Yellow,
                topLeft = Offset(ow / 2f, ow / 2f),
                size = Size(w - ow, h - ow),
                cornerRadius = CornerRadius(max(radius - ow / 2f, 0f)),
                style = Stroke(width = ow)
            )
        }
        return
    }

    val fill = when {
        pressed -> ControlSkin.Yellow.copy(alpha = 0.34f + 0.30f * k)
        selected -> ControlSkin.Yellow.copy(alpha = 0.18f)
        else -> Color.Black.copy(alpha = (if (spec.primary) 0.30f else 0.22f) * (0.4f + 0.6f * k))
    }
    val edge = when {
        pressed -> ControlSkin.YellowSoft.copy(alpha = 0.85f)
        selected -> ControlSkin.Yellow
        else -> Color.White.copy(alpha = (if (spec.primary) 0.22f else 0.16f) + 0.14f * k)
    }
    val sw = (if (selected) 2.2f else if (spec.primary) 1.3f else 1.2f).dp.toPx()

    drawRoundRect(color = fill, cornerRadius = CornerRadius(radius))
    drawRoundRect(
        color = edge,
        topLeft = Offset(sw / 2f, sw / 2f),
        size = Size(w - sw, h - sw),
        cornerRadius = CornerRadius(max(radius - sw / 2f, 0f)),
        style = Stroke(width = sw)
    )
}

private fun DrawScope.drawControlLabel(
    text: String,
    cx: Float,
    cy: Float,
    maxW: Float,
    maxH: Float,
    color: Color
) {
    val shown = text.take(9)
    val len = max(shown.length, 1)
    val size = min(maxH * 0.36f, maxW / (len * 0.74f)).coerceIn(8.dp.toPx(), 13.dp.toPx())
    labelPaint.textSize = size
    labelPaint.color = color.toArgb()
    val fm = labelPaint.fontMetrics
    val baseline = cy - (fm.ascent + fm.descent) / 2f
    drawIntoCanvas { it.nativeCanvas.drawText(shown, cx, baseline, labelPaint) }
}

/**
 * Ikon garis sederhana dalam kotak [s] berpusat di ([cx], [cy]). Nama: jump, sneak, attack, use,
 * inventory, chat, pause, keyboard, eye, close, gear, sliders, terminal, power, coffee, external,
 * trash, warning, download, copy, share, record, stop, chev_up, chev_down, chev_right.
 */
fun DrawScope.drawControlIcon(name: String, cx: Float, cy: Float, s: Float, color: Color, slash: Boolean = false) {
    val x0 = cx - s / 2f
    val y0 = cy - s / 2f
    val sw = s * 0.078f
    val stroke = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)

    fun p(u: Float, v: Float) = Offset(x0 + u * s, y0 + v * s)
    fun line(a: Offset, b: Offset, width: Float = sw) =
        drawLine(color, a, b, strokeWidth = width, cap = StrokeCap.Round)
    // Titik diberikan sebagai pasangan u, v (0..1) dalam FloatArray; Offset tidak boleh jadi vararg.
    fun poly(closed: Boolean, uv: FloatArray) {
        val path = Path()
        path.moveTo(x0 + uv[0] * s, y0 + uv[1] * s)
        for (i in 1 until uv.size / 2) path.lineTo(x0 + uv[2 * i] * s, y0 + uv[2 * i + 1] * s)
        if (closed) path.close()
        drawPath(path, color, style = stroke)
    }

    when (name) {
        "jump" -> {
            line(p(0.5f, 0.82f), p(0.5f, 0.26f))
            poly(false, floatArrayOf(0.26f, 0.50f, 0.5f, 0.26f, 0.74f, 0.50f))
        }
        "sneak" -> {
            line(p(0.5f, 0.18f), p(0.5f, 0.74f))
            poly(false, floatArrayOf(0.26f, 0.50f, 0.5f, 0.74f, 0.74f, 0.50f))
        }
        "attack" -> {
            line(p(0.30f, 0.70f), p(0.80f, 0.20f), sw * 1.35f)
            line(p(0.26f, 0.54f), p(0.46f, 0.74f))
            line(p(0.30f, 0.70f), p(0.20f, 0.80f))
        }
        "use" -> {
            poly(
                true,
                floatArrayOf(0.50f, 0.20f, 0.76f, 0.35f, 0.76f, 0.65f, 0.50f, 0.80f, 0.24f, 0.65f, 0.24f, 0.35f))
            poly(false, floatArrayOf(0.24f, 0.35f, 0.50f, 0.50f, 0.76f, 0.35f))
            line(p(0.50f, 0.50f), p(0.50f, 0.80f))
        }
        "inventory" -> {
            val q = Size(0.24f * s, 0.24f * s)
            val cr = CornerRadius(0.05f * s)
            drawRoundRect(color, p(0.22f, 0.22f), q, cr, stroke)
            drawRoundRect(color, p(0.54f, 0.22f), q, cr, stroke)
            drawRoundRect(color, p(0.22f, 0.54f), q, cr, stroke)
            drawRoundRect(color, p(0.54f, 0.54f), q, cr, stroke)
        }
        "chat" -> {
            drawRoundRect(color, p(0.18f, 0.22f), Size(0.64f * s, 0.42f * s), CornerRadius(0.09f * s), stroke)
            poly(false, floatArrayOf(0.34f, 0.64f, 0.32f, 0.80f, 0.52f, 0.64f))
        }
        "pause" -> {
            val bar = Size(0.14f * s, 0.52f * s)
            val cr = CornerRadius(0.04f * s)
            drawRoundRect(color, p(0.30f, 0.24f), bar, cr)
            drawRoundRect(color, p(0.56f, 0.24f), bar, cr)
        }
        "keyboard" -> {
            drawRoundRect(color, p(0.14f, 0.30f), Size(0.72f * s, 0.40f * s), CornerRadius(0.07f * s), stroke)
            for (u in floatArrayOf(0.28f, 0.41f, 0.54f, 0.67f)) {
                drawCircle(color, 0.032f * s, p(u, 0.43f))
            }
            line(p(0.32f, 0.58f), p(0.68f, 0.58f))
        }
        "eye" -> {
            drawOval(color, p(0.12f, 0.32f), Size(0.76f * s, 0.36f * s), style = stroke)
            drawCircle(color, 0.10f * s, p(0.5f, 0.5f))
            if (slash) line(p(0.22f, 0.80f), p(0.78f, 0.20f))
        }
        "close" -> {
            line(p(0.28f, 0.28f), p(0.72f, 0.72f))
            line(p(0.72f, 0.28f), p(0.28f, 0.72f))
        }
        "gear" -> {
            // Cincin, lubang tengah, dan delapan gigi pendek di sekelilingnya.
            val c = p(0.5f, 0.5f)
            drawCircle(color, 0.23f * s, c, style = stroke)
            drawCircle(color, 0.08f * s, c, style = stroke)
            for (i in 0 until 8) {
                val a = i * (Math.PI / 4.0)
                val ca = cos(a).toFloat()
                val sa = sin(a).toFloat()
                line(
                    Offset(c.x + ca * 0.32f * s, c.y + sa * 0.32f * s),
                    Offset(c.x + ca * 0.42f * s, c.y + sa * 0.42f * s),
                    sw * 1.7f
                )
            }
        }
        "sliders" -> {
            line(p(0.18f, 0.28f), p(0.82f, 0.28f))
            line(p(0.18f, 0.50f), p(0.82f, 0.50f))
            line(p(0.18f, 0.72f), p(0.82f, 0.72f))
            drawCircle(color, 0.08f * s, p(0.36f, 0.28f))
            drawCircle(color, 0.08f * s, p(0.64f, 0.50f))
            drawCircle(color, 0.08f * s, p(0.44f, 0.72f))
        }
        "terminal" -> {
            poly(false, floatArrayOf(0.26f, 0.34f, 0.44f, 0.50f, 0.26f, 0.66f))
            line(p(0.52f, 0.68f), p(0.76f, 0.68f))
        }
        "power" -> {
            drawArc(color, -60f, 300f, false, p(0.22f, 0.26f), Size(0.56f * s, 0.56f * s), style = stroke)
            line(p(0.5f, 0.14f), p(0.5f, 0.46f))
        }
        "coffee" -> {
            // Cangkir (badan bersudut miring di bawah), pegangan, alas, dan dua uap.
            poly(
                true,
                floatArrayOf(0.20f, 0.44f, 0.66f, 0.44f, 0.66f, 0.62f, 0.56f, 0.76f, 0.30f, 0.76f, 0.20f, 0.62f))
            drawArc(color, -90f, 180f, false, p(0.50f, 0.48f), Size(0.30f * s, 0.20f * s), style = stroke)
            line(p(0.14f, 0.86f), p(0.72f, 0.86f))
            line(p(0.34f, 0.18f), p(0.34f, 0.32f))
            line(p(0.52f, 0.18f), p(0.52f, 0.32f))
        }
        "external" -> {
            // Kotak terbuka di kanan-atas dengan panah keluar.
            poly(false, floatArrayOf(0.44f, 0.24f, 0.22f, 0.24f, 0.22f, 0.78f, 0.76f, 0.78f, 0.76f, 0.56f))
            line(p(0.50f, 0.50f), p(0.80f, 0.20f))
            poly(false, floatArrayOf(0.58f, 0.20f, 0.80f, 0.20f, 0.80f, 0.42f))
        }
        "trash" -> {
            line(p(0.20f, 0.30f), p(0.80f, 0.30f))
            poly(false, floatArrayOf(0.40f, 0.30f, 0.40f, 0.20f, 0.60f, 0.20f, 0.60f, 0.30f))
            poly(false, floatArrayOf(0.28f, 0.30f, 0.32f, 0.80f, 0.68f, 0.80f, 0.72f, 0.30f))
            line(p(0.43f, 0.46f), p(0.43f, 0.66f))
            line(p(0.57f, 0.46f), p(0.57f, 0.66f))
        }
        "warning" -> {
            poly(true, floatArrayOf(0.50f, 0.20f, 0.82f, 0.76f, 0.18f, 0.76f))
            line(p(0.50f, 0.42f), p(0.50f, 0.58f))
            drawCircle(color, 0.035f * s, p(0.50f, 0.67f))
        }
        "download" -> {
            line(p(0.50f, 0.20f), p(0.50f, 0.60f))
            poly(false, floatArrayOf(0.32f, 0.44f, 0.50f, 0.62f, 0.68f, 0.44f))
            poly(false, floatArrayOf(0.24f, 0.72f, 0.24f, 0.82f, 0.76f, 0.82f, 0.76f, 0.72f))
        }
        "copy" -> {
            // Dua lembar bertumpuk: lembar depan utuh, lembar belakang hanya sisi yang tidak tertutup.
            drawRoundRect(color, p(0.38f, 0.34f), Size(0.42f * s, 0.46f * s), CornerRadius(0.06f * s), stroke)
            poly(false, floatArrayOf(0.38f, 0.66f, 0.20f, 0.66f, 0.20f, 0.20f, 0.62f, 0.20f, 0.62f, 0.34f))
        }
        "share" -> {
            // Tiga titik terhubung: satu di kiri, dua di kanan.
            line(p(0.28f, 0.50f), p(0.70f, 0.26f))
            line(p(0.28f, 0.50f), p(0.70f, 0.74f))
            drawCircle(color, 0.09f * s, p(0.28f, 0.50f))
            drawCircle(color, 0.09f * s, p(0.70f, 0.26f))
            drawCircle(color, 0.09f * s, p(0.70f, 0.74f))
        }
        "record" -> {
            // Cincin dengan titik penuh di tengah.
            drawCircle(color, 0.32f * s, p(0.5f, 0.5f), style = stroke)
            drawCircle(color, 0.15f * s, p(0.5f, 0.5f))
        }
        "stop" -> drawRoundRect(color, p(0.28f, 0.28f), Size(0.44f * s, 0.44f * s), CornerRadius(0.07f * s))
        "chev_right" -> poly(false, floatArrayOf(0.38f, 0.22f, 0.66f, 0.50f, 0.38f, 0.78f))
        "chev_up" -> poly(false, floatArrayOf(0.22f, 0.62f, 0.5f, 0.34f, 0.78f, 0.62f))
        "chev_down" -> poly(false, floatArrayOf(0.22f, 0.38f, 0.5f, 0.66f, 0.78f, 0.38f))
    }
}

/**
 * Joystick: alas kaca bulat, empat tanda arah, dan kenop. Saat disentuh kenop jadi kuning; saat
 * kunci lari aktif (didorong sampai tepi) cincin luar kuning dan kenop berisi dua panah naik.
 */
fun DrawScope.drawControlJoystick(
    opacity: Float,
    knob: Offset,
    touching: Boolean,
    sprint: Boolean,
    selected: Boolean = false
) {
    val k = opacity.coerceIn(0.1f, 1f)
    val c = Offset(size.width / 2f, size.height / 2f)
    val r = min(size.width, size.height) / 2f
    val sw = (if (sprint || selected) 2.2f else 1.2f).dp.toPx()

    drawCircle(color = Color.Black.copy(alpha = 0.20f * (0.4f + 0.6f * k)), radius = r, center = c)
    drawCircle(
        color = when {
            sprint || selected -> ControlSkin.Yellow
            touching -> Color.White.copy(alpha = 0.34f)
            else -> Color.White.copy(alpha = 0.16f + 0.12f * k)
        },
        radius = r - sw / 2f,
        center = c,
        style = Stroke(width = sw)
    )

    // Empat tanda arah kecil di dekat tepi
    val tick = Color.White.copy(alpha = 0.16f + 0.22f * k)
    val tr = r * 0.80f
    val ts = r * 0.07f
    val tw = 1.3f.dp.toPx()
    fun chevron(dx: Float, dy: Float) {
        val m = Offset(c.x + dx * tr, c.y + dy * tr)
        val px = -dy
        val py = dx
        val a = Offset(m.x - dx * ts + px * ts * 1.3f, m.y - dy * ts + py * ts * 1.3f)
        val b = Offset(m.x - dx * ts - px * ts * 1.3f, m.y - dy * ts - py * ts * 1.3f)
        drawLine(tick, a, m, strokeWidth = tw, cap = StrokeCap.Round)
        drawLine(tick, b, m, strokeWidth = tw, cap = StrokeCap.Round)
    }
    chevron(0f, -1f)
    chevron(1f, 0f)
    chevron(0f, 1f)
    chevron(-1f, 0f)

    val kr = r * 0.36f
    val kc = c + knob
    drawCircle(
        color = if (touching) ControlSkin.Yellow.copy(alpha = 0.40f + 0.30f * k) else Color.White.copy(alpha = 0.12f + 0.08f * k),
        radius = kr,
        center = kc
    )
    drawCircle(
        color = if (touching) ControlSkin.YellowSoft.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.34f + 0.30f * k),
        radius = kr - 0.75f.dp.toPx(),
        center = kc,
        style = Stroke(width = 1.5f.dp.toPx())
    )

    // Kunci lari aktif: dua panah naik di dalam kenop (digambar setelah kenop supaya tidak tertutup)
    if (sprint) {
        val s = kr * 1.15f
        drawControlIcon("chev_up", kc.x, kc.y - s * 0.16f, s, ControlSkin.Ink)
        drawControlIcon("chev_up", kc.x, kc.y + s * 0.16f, s, ControlSkin.Ink)
    }
}

// ----------------------------------------------------------------------------
// Section: ui/control/MenuMouse.kt
// ----------------------------------------------------------------------------

/*
 * Mouse fisik di MENU game (kursor Minecraft tampil), ditangani langsung di GameActivity.
 *
 * Kenapa di sini dan bukan di Compose: log diagnosis menunjukkan Android mengirim klik mouse ke
 * GameActivity, tetapi tidak satu pun sampai ke penangan Compose di lapisan game (kursor ikut
 * bergerak, klik hilang). Pengiriman event Compose bergantung pada urutan lapisan, hit-test, dan
 * konsumsi event, jadi di sini jalurnya dipotong: event mentah dibaca sebelum Compose menyentuhnya.
 * Cara ini sama dengan yang dipakai SDL dan PojavLauncher (View/Activity, bukan Compose).
 *
 * Tombol dihitung dari buttonState setiap event (bukan hanya dari ACTION_DOWN/UP), jadi satu event
 * yang hilang tidak membuat tombol tersangkut. Hanya PERUBAHAN status yang dikirim, dan satu pelacak
 * dipakai bersama klik kanan yang datang sebagai KEYCODE_BACK, jadi tidak pernah terkirim ganda.
 *
 * Event tidak dikonsumsi di sini: Compose tetap menerimanya (posisi hover, penunjuk sistem, dan
 * penyembunyian tombol layar). Tombol dan gulir mouse di filter Compose dimatikan untuk menghindari
 * kiriman ganda (lihat physicalMouseHover).
 *
 * Murni digerakkan event: tanpa polling, tanpa thread, tanpa alokasi per event.
 */
internal object MenuMouse {
    /** Saklar cadangan. Bila dimatikan, jalur Compose lama kembali memegang tombol dan gulir. */
    @JvmField
    var enabled = true

    /**
     * Lapisan launcher (menu dalam game, dialog keluar, editor, layar loading) sedang menutupi game.
     * Diisi dari Compose. Saat true, klik mouse milik lapisan itu dan tidak diteruskan ke game.
     */
    @JvmField
    var overlayOpen = false

    private val loc = IntArray(2)
    private var syntheticDown = false
    private var backDirect = false

    // Tombol GLFW yang sedang tertekan akibat mouse fisik (bit = nomor tombol). Dipakai SEMUA jalur mouse
    // fisik (menu absolut, pointer capture, klik kanan lewat BACK) sebagai pintu terakhir ke game.
    private var heldGlfw = 0

    /**
     * Kirim tombol mouse fisik ke game. Tekan ganda atau lepas ganda dibuang, mis. klik kanan yang datang
     * dua kali (tombol BACK + event gerak), jadi game tidak pernah menerima satu klik dua kali.
     */
    fun sendButton(glfw: Int, pressed: Boolean) {
        if (glfw < 0 || glfw > 30) {
            CallbackBridge.sendMouseButton(glfw, pressed)
            return
        }
        val bit = 1 shl glfw
        if (((heldGlfw and bit) != 0) == pressed) return
        heldGlfw = if (pressed) heldGlfw or bit else heldGlfw and bit.inv()
        CallbackBridge.sendMouseButton(glfw, pressed)
    }

    private fun releaseHeld() {
        var glfw = 0
        while (heldGlfw != 0 && glfw <= 30) {
            if ((heldGlfw and (1 shl glfw)) != 0) sendButton(glfw, false)
            glfw++
        }
        heldGlfw = 0
    }

    private val buttons = CapturedMouseButtons { button, pressed ->
        val glfw = MouseButtons.toGlfw(button)
        if (glfw != null) {
            MouseDiag.buttonsSent++
            sendButton(glfw, pressed)
        } else {
            MouseDiag.buttonsDropped++
        }
    }

    /** Kursor menu game tampil dan tidak ada lapisan launcher di atasnya. */
    private fun menuActive(): Boolean =
        enabled && !overlayOpen && ZLBridgeStates.cursorMode.value != CURSOR_DISABLED

    /**
     * Event absolut (hover/sentuh) ditangani di sini hanya bila mouse tidak ditangkap. Saat mouse launcher
     * aktif (capture dipegang) event itu tidak datang lagi, dan bila ada yang nyasar, posisi absolutnya akan
     * melompatkan kursor launcher.
     */
    private fun canHandle(): Boolean = menuActive() && !LauncherMouse.captureHeld.value

    /**
     * Tombol dari jalur tangkap (mouse launcher di menu). Memakai pelacak yang sama dengan klik kanan yang
     * datang sebagai KEYCODE_BACK, jadi satu klik tidak pernah terkirim dua kali.
     */
    fun setButton(button: Int, pressed: Boolean) {
        if (pressed && LauncherMouse.captureHeld.value) LauncherMouse.syncCursor?.invoke()
        buttons.set(button, pressed)
    }

    private fun resetMenu() {
        syntheticDown = false
        buttons.reset()
    }

    /** Lepaskan semua tombol mouse fisik yang masih tertahan (sekali per tombol), dari jalur mana pun. */
    fun reset() {
        resetMenu()
        releaseHeld()
    }

    /**
     * Event sentuh dari mouse: ACTION_DOWN / MOVE (saat tombol ditahan) / UP. Dipanggil dari
     * GameActivity.dispatchTouchEvent sebelum Compose. [content] dipakai untuk mengubah koordinat
     * jendela menjadi koordinat lapisan game.
     */
    fun onTouch(event: MotionEvent, content: View?) {
        if (!event.isFromSource(InputDevice.SOURCE_MOUSE)) return
        if (!canHandle()) {
            MouseDiag.actMenuBlocked++
            resetMenu()
            return
        }
        MouseDiag.actMenu++

        val action = event.actionMasked
        if (action == MotionEvent.ACTION_CANCEL) {
            resetMenu()
            return
        }

        // Posisi dulu: klik harus jatuh di titik penunjuk, bukan di posisi hover terakhir yang sempat sampai.
        sendPosition(event.x, event.y, content)

        var state = event.buttonState
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                // Sebagian touchpad melaporkan ketukan sebagai DOWN tanpa tombol: anggap klik kiri.
                syntheticDown = state == 0
                if (syntheticDown) state = MotionEvent.BUTTON_PRIMARY
            }

            MotionEvent.ACTION_MOVE -> {
                if (syntheticDown && state == 0) state = MotionEvent.BUTTON_PRIMARY
            }

            // Mouse satu penunjuk: UP berarti tombol terakhir dilepas.
            MotionEvent.ACTION_UP -> {
                syntheticDown = false
                state = 0
            }
        }
        buttons.onSnapshot(state)
    }

    /**
     * Event generik dari mouse: hover, ACTION_BUTTON_PRESS/RELEASE, dan roda gulir. Dipanggil dari
     * GameActivity.dispatchGenericMotionEvent sebelum Compose.
     */
    fun onGeneric(event: MotionEvent) {
        if (!event.isFromSource(InputDevice.SOURCE_MOUSE)) return
        if (!canHandle()) {
            MouseDiag.actMenuBlocked++
            resetMenu()
            return
        }
        MouseDiag.actMenu++

        when (event.actionMasked) {
            MotionEvent.ACTION_BUTTON_PRESS,
            MotionEvent.ACTION_BUTTON_RELEASE ->
                buttons.onEvent(event.actionMasked, event.actionButton, event.buttonState)

            // Hover berarti tidak ada tombol yang ditahan: sekaligus melepas tombol yang tersangkut.
            MotionEvent.ACTION_HOVER_ENTER,
            MotionEvent.ACTION_HOVER_MOVE,
            MotionEvent.ACTION_HOVER_EXIT -> buttons.onSnapshot(event.buttonState)

            MotionEvent.ACTION_SCROLL -> {
                MouseDiag.scrollSent++
                CallbackBridge.sendScroll(
                    event.getAxisValue(MotionEvent.AXIS_HSCROLL).toDouble(),
                    event.getAxisValue(MotionEvent.AXIS_VSCROLL).toDouble()
                )
            }
        }
    }

    /**
     * Klik kanan mouse yang datang sebagai KEYCODE_BACK. Di menu, tombolnya lewat pelacak yang sama
     * dengan event gerak (yang juga membawa BUTTON_SECONDARY), jadi tidak terkirim dua kali.
     * Mengembalikan true bila sudah ditangani di sini; false berarti pemanggil mengirim langsung
     * (mode bermain / lapisan launcher terbuka).
     */
    fun onBackKey(down: Boolean): Boolean {
        MouseDiag.actBack++
        if (down) {
            if (!menuActive()) {
                backDirect = true
                return false
            }
            backDirect = false
            setButton(MotionEvent.BUTTON_SECONDARY, true)
            return true
        }
        if (backDirect) {
            // Tekannya dikirim langsung, jadi lepasnya juga dikirim langsung.
            backDirect = false
            return false
        }
        buttons.set(MotionEvent.BUTTON_SECONDARY, false)
        return true
    }

    private fun sendPosition(rawX: Float, rawY: Float, content: View?) {
        val winW = CallbackBridge.windowWidth
        val winH = CallbackBridge.windowHeight
        var x = rawX
        var y = rawY
        var w = 0
        var h = 0
        if (content != null) {
            content.getLocationInWindow(loc)
            x -= loc[0]
            y -= loc[1]
            w = content.width
            h = content.height
        }
        if (w > 0 && h > 0 && winW > 0 && winH > 0) {
            x = x.coerceIn(0f, w.toFloat()) * (winW.toFloat() / w)
            y = y.coerceIn(0f, h.toFloat()) * (winH.toFloat() / h)
        }
        MouseDiag.cursorSent++
        CallbackBridge.sendCursorPos(x, y)
    }
}

// ----------------------------------------------------------------------------
// Section: ui/control/PhysicalMouseInput.kt
// ----------------------------------------------------------------------------

/*
 * Mouse fisik (mouse sistem Android), mengikuti cara Zalith Launcher 2.
 *
 * Tiga jalur, dan hanya satu yang aktif pada satu waktu:
 *  - Menu game, mouse launcher (utama): mouse ditangkap lewat pointer capture, jadi penunjuk sistem Android
 *    hilang. Gerakan relatif menggeser kursor milik launcher (digambar launcher, dikirim ke game sebagai
 *    posisi kursor GLFW), dan klik datang dari event tangkap yang sama. Tidak bergantung pada posisi hover,
 *    bentuk penunjuk, atau klik yang menyamar sebagai event sentuh.
 *  - Menu game, penunjuk sistem (cadangan): dipakai selama Android belum atau tidak memberi pointer capture
 *    (mis. jendela tanpa fokus). Posisi absolut dari event hover/move dikirim sebagai posisi kursor GLFW,
 *    dan penunjuk yang terlihat adalah penunjuk sistem (bentuknya mengikuti bentuk kursor game).
 *  - Bermain (kursor Minecraft terkunci): mouse ditangkap lewat pointer capture, gerakan relatif dikirim
 *    sebagai delta kamera.
 *
 * Semuanya digerakkan event, tanpa polling dan tanpa thread tambahan.
 */

/** Tombol Android -> tombol GLFW. Tombol samping (BACK/FORWARD) sengaja tidak dipetakan. */
internal object MouseButtons {
    fun toGlfw(button: Int): Int? = when (button) {
        MotionEvent.BUTTON_PRIMARY -> LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT
        MotionEvent.BUTTON_SECONDARY, MotionEvent.BUTTON_STYLUS_SECONDARY -> LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT
        MotionEvent.BUTTON_TERTIARY -> LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_MIDDLE
        // BUTTON_BACK dari mouse sering datang juga sebagai KEYCODE_BACK; itu sudah diubah jadi klik kanan
        // di GameActivity.dispatchKeyEvent. Dipetakan lagi di sini akan membuat klik kanan terkirim dua kali.
        else -> null
    }
}

/**
 * Status mouse launcher di menu game. [captureHeld] diisi dari GameActivity.onPointerCaptureChanged, jadi
 * mencerminkan keadaan SEBENARNYA (Android memegang capture), bukan sekadar permintaan. Selama true di menu,
 * kursor yang terlihat milik launcher dan jalur penunjuk sistem dimatikan; begitu false, jalur penunjuk
 * sistem langsung bekerja lagi.
 */
internal object LauncherMouse {
    val captureHeld = mutableStateOf(false)

    /**
     * Mengirim posisi kursor launcher ke game. Game bisa memindahkan kursornya sendiri (ke tengah saat layar
     * baru terbuka), jadi posisi disamakan sebelum klik dan gulir. Diisi oleh SwitchableMouseLayout.
     */
    @JvmField
    var syncCursor: (() -> Unit)? = null
}

/**
 * Pelacak tombol mouse yang ditekan. Hanya mengirim PERUBAHAN status, jadi snapshot yang berulang
 * tidak menghasilkan klik ganda. Murni (tanpa Compose), mudah diuji.
 *
 * Mode tangkap kadang mengirim ACTION_DOWN/UP dengan actionButton = 0; status tombol selalu diambil
 * dari buttonState, bukan dari actionButton.
 */
internal class CapturedMouseButtons(private val onMouseButton: (Int, Boolean) -> Unit) {
    private var heldButtons = 0

    fun onEvent(action: Int, actionButton: Int, buttonState: Int) {
        if (action == MotionEvent.ACTION_CANCEL) {
            reset()
            return
        }

        val buttons = when (action) {
            MotionEvent.ACTION_BUTTON_PRESS -> buttonState or actionButton
            MotionEvent.ACTION_BUTTON_RELEASE -> buttonState and actionButton.inv()
            else -> buttonState
        }
        update(buttons and SUPPORTED_BUTTONS)
    }

    /** Jalur menu: buttonState dari event hover/move dipakai apa adanya. */
    fun onSnapshot(buttonState: Int) {
        update(buttonState and SUPPORTED_BUTTONS)
    }

    /** Lepaskan semua tombol yang masih tertahan (sekali saja per tombol). */
    fun reset() {
        update(0)
    }

    /** Tekan atau lepas satu tombol tanpa mengubah tombol lain. Tidak mengirim bila statusnya sudah sama. */
    fun set(button: Int, pressed: Boolean) {
        update(if (pressed) heldButtons or button else heldButtons and button.inv())
    }

    private fun update(buttons: Int) {
        val changed = heldButtons xor buttons
        heldButtons = buttons
        if (changed == 0) return

        fun dispatchButton(button: Int) {
            if (changed and button != 0) {
                onMouseButton(button, buttons and button != 0)
            }
        }

        dispatchButton(MotionEvent.BUTTON_PRIMARY)
        dispatchButton(MotionEvent.BUTTON_SECONDARY)
        dispatchButton(MotionEvent.BUTTON_TERTIARY)
        dispatchButton(MotionEvent.BUTTON_BACK)
        dispatchButton(MotionEvent.BUTTON_FORWARD)
        dispatchButton(MotionEvent.BUTTON_STYLUS_SECONDARY)
    }

    companion object {
        private const val SUPPORTED_BUTTONS = MotionEvent.BUTTON_PRIMARY or
            MotionEvent.BUTTON_SECONDARY or MotionEvent.BUTTON_TERTIARY or
            MotionEvent.BUTTON_BACK or MotionEvent.BUTTON_FORWARD or
            MotionEvent.BUTTON_STYLUS_SECONDARY
    }
}

/**
 * Catatan diagnosis mouse fisik. Ditulis ke panel log game DAN ke buku harian launcher ([LauncherLog]),
 * jadi barisnya ikut di laporan launcher yang dibagikan dari Pengaturan.
 *
 * Isinya, supaya laporan "mouse tidak berfungsi" bisa dipetakan ke penyebabnya:
 *  - perangkat: mouse/touchpad/trackball/stylus yang dilihat Android.
 *  - satu baris per jalur masuk (sumber, alat, aksi, tombol event pertama).
 *  - kursor game (menu atau terkunci) tiap kali berganti.
 *  - ringkasan jumlah event: yang tiba di Activity, yang sampai ke penangan menu/tangkap, dan yang
 *    benar-benar diteruskan ke game. Selisih antar angka menunjukkan di mana event hilang.
 *
 * Biaya per event hanya satu penambahan Int dan satu uji bit: tanpa alokasi, tanpa kunci, tanpa tulis
 * berkas. Semua event input datang di thread utama, jadi penghitung biasa cukup. Penulisan log terjadi
 * paling banyak beberapa lusin kali per sesi.
 */
internal object MouseDiag {
    // Jalur (bit). Satu baris log per jalur per sesi.
    const val MENU_MOUSE = 1
    const val MENU_STYLUS = 2
    const val CAPTURE = 4
    const val CAPTURE_TOUCHPAD = 8
    const val TRACKBALL = 16
    const val ARROW_SWALLOWED = 32
    const val ACT_GENERIC = 64
    const val ACT_TOUCH = 128
    const val MENU_IGNORED = 256

    private const val MAX_SUMMARIES = 14
    private const val MAX_MODE_LOGS = 20
    private const val MAX_CAPTURE_LOGS = 20
    private const val MAX_DEVICE_LOGS = 3
    private const val MAX_COMBO_LOGS = 40

    // Kelompok kombinasi (aksi, tombol) yang dicatat: di Activity (sentuh/generik) dan di filter Compose.
    const val COMBO_TOUCH = 0
    const val COMBO_GENERIC = 1
    const val COMBO_FILTER = 2

    @Volatile
    var sink: ((String) -> Unit)? = null

    // 3 kelompok x 128 kombinasi (aksi 0..15 x tombol 0..7), 2 Long per kelompok.
    private val comboSeen = LongArray(6)
    private var comboLogs = 0

    private var seenMask = 0
    private var summaries = 0
    private var lastTotal = -1
    private var modeLogs = 0
    private var captureLogs = 0
    private var deviceLogs = 0
    private var lastDevices = ""

    // Event yang tiba di GameActivity (sebelum Compose): generik = hover/gulir/tangkap, sentuh = klik.
    @JvmField var actGeneric = 0
    @JvmField var actTouch = 0
    // Event yang sampai ke penangan menu, yang diabaikan karena game sedang mengunci kursor, dan penangan tangkap.
    @JvmField var menuEvents = 0
    @JvmField var menuIgnored = 0
    @JvmField var captureEvents = 0
    @JvmField var trackballEvents = 0
    // Yang diteruskan ke game.
    @JvmField var cursorSent = 0
    @JvmField var deltaSent = 0
    @JvmField var buttonsSent = 0
    @JvmField var buttonsDropped = 0
    @JvmField var scrollSent = 0
    // Penangan mouse menu di Activity: ditangani / dilewati (lapisan launcher terbuka atau game mengunci kursor) /
    // klik kanan yang datang sebagai tombol BACK.
    @JvmField var actMenu = 0
    @JvmField var actMenuBlocked = 0
    @JvmField var actBack = 0

    private fun comboName(group: Int) = when (group) {
        COMBO_TOUCH -> "activity-sentuh"
        COMBO_GENERIC -> "activity-generik"
        else -> "filter-compose"
    }

    /**
     * Catat setiap kombinasi (aksi, tombol) yang BERBEDA, sekali saja per kelompok. Menjawab pertanyaan
     * "event klik jenis apa yang tiba di mana". Satu uji bit per event, tanpa alokasi.
     */
    fun combo(group: Int, action: Int, buttons: Int) {
        val idx = ((action and 15) shl 3) or (buttons and 7)
        val word = group * 2 + (idx ushr 6)
        val bit = 1L shl (idx and 63)
        if ((comboSeen[word] and bit) != 0L) return
        comboSeen[word] = comboSeen[word] or bit
        if (comboLogs >= MAX_COMBO_LOGS) return
        comboLogs++
        emit("kombinasi ${comboName(group)} aksi=$action tombol=$buttons")
    }

    private fun pathName(path: Int) = when (path) {
        MENU_MOUSE -> "menu-mouse"
        MENU_STYLUS -> "menu-stylus"
        CAPTURE -> "tangkap"
        CAPTURE_TOUCHPAD -> "tangkap-touchpad"
        TRACKBALL -> "trackball"
        ARROW_SWALLOWED -> "panah-sintetis-ditelan"
        ACT_GENERIC -> "aktivitas-generik"
        ACT_TOUCH -> "aktivitas-sentuh"
        MENU_IGNORED -> "menu-diabaikan-saat-terkunci"
        else -> "jalur-$path"
    }

    private fun emit(body: String) {
        LauncherLog.event("Mouse", body)
        sink?.invoke("[Mouse] $body")
    }

    /** Catat event pertama pada [path]. Setelah itu hanya satu uji bit. */
    fun once(path: Int, source: Int, toolType: Int, action: Int, buttons: Int) {
        if ((seenMask and path) != 0) return
        seenMask = seenMask or path
        emit("jalur=${pathName(path)} sumber=0x${Integer.toHexString(source)} alat=$toolType aksi=$action tombol=$buttons")
    }

    fun once(path: Int, event: MotionEvent) {
        if ((seenMask and path) != 0) return
        once(path, event.source, event.getToolType(0), event.actionMasked, event.buttonState)
    }

    /** Perangkat penunjuk yang dilihat Android. Hanya ditulis bila daftarnya berubah. */
    fun devices() {
        if (deviceLogs >= MAX_DEVICE_LOGS) return
        try {
            val sb = StringBuilder()
            for (id in InputDevice.getDeviceIds()) {
                val d = InputDevice.getDevice(id) ?: continue
                val pointerLike = d.supportsSource(InputDevice.SOURCE_MOUSE) ||
                    d.supportsSource(InputDevice.SOURCE_MOUSE_RELATIVE) ||
                    d.supportsSource(InputDevice.SOURCE_TOUCHPAD) ||
                    d.supportsSource(InputDevice.SOURCE_TRACKBALL) ||
                    d.supportsSource(InputDevice.SOURCE_STYLUS)
                if (!pointerLike) continue
                if (sb.isNotEmpty()) sb.append(" | ")
                sb.append(d.name).append(" #").append(id)
                    .append(" sumber=0x").append(Integer.toHexString(d.sources))
                    .append(" v").append(d.vendorId).append(" p").append(d.productId)
                if (Build.VERSION.SDK_INT >= 29) sb.append(if (d.isExternal) " luar" else " dalam")
            }
            val text = if (sb.isEmpty()) "tidak ada mouse/touchpad/trackball/stylus terdeteksi" else sb.toString()
            if (text == lastDevices) return
            lastDevices = text
            deviceLogs++
            emit("perangkat: $text")
        } catch (_: Throwable) {
        }
    }

    /** Kursor game berganti: menu (bebas) atau bermain (terkunci). Sekaligus rangkuman sejauh ini. */
    fun cursorMode(captured: Boolean) {
        if (modeLogs >= MAX_MODE_LOGS) return
        modeLogs++
        emit("kursor game=${if (captured) "terkunci (bermain)" else "bebas (menu)"} sdl=${SdlBridge.sdlEnabled}")
        summary("ganti-mode")
    }

    /** Android memberi atau mencabut pointer capture dari jendela. */
    fun captureChanged(has: Boolean) {
        if (captureLogs >= MAX_CAPTURE_LOGS) return
        captureLogs++
        emit("pointer-capture jendela=$has")
    }

    /** Permintaan pointer capture dikirim. Hasilnya datang lewat [captureChanged]. */
    fun captureRequested(windowFocus: Boolean, viewFocus: Boolean) {
        if (captureLogs >= MAX_CAPTURE_LOGS) return
        captureLogs++
        val mode = if (ZLBridgeStates.cursorMode.value == CURSOR_DISABLED) "bermain" else "menu"
        emit("pointer-capture diminta mode=$mode fokusJendela=$windowFocus fokusView=$viewFocus")
    }

    /**
     * Rangkuman jumlah event. Dilewati bila tidak ada yang berubah sejak rangkuman terakhir, tapi rangkuman
     * pertama selalu ditulis: angka nol semua berarti Android tidak mengirim satu pun event mouse.
     */
    fun summary(reason: String) {
        val total = actGeneric + actTouch + menuEvents + menuIgnored + captureEvents + trackballEvents +
            cursorSent + deltaSent + buttonsSent + buttonsDropped + scrollSent + actMenu + actMenuBlocked + actBack
        if (total == lastTotal || summaries >= MAX_SUMMARIES) return
        lastTotal = total
        summaries++
        val mode = if (ZLBridgeStates.cursorMode.value == CURSOR_DISABLED) "terkunci" else "bebas"
        emit(
            "ringkasan($reason) kursor=$mode sdl=${SdlBridge.sdlEnabled} " +
                "aktivitas[generik=$actGeneric sentuh=$actTouch ditangani=$actMenu dilewati=$actMenuBlocked back=$actBack] " +
                "menu[masuk=$menuEvents diabaikan=$menuIgnored] tangkap[masuk=$captureEvents] trackball=$trackballEvents " +
                "ke-game[kursor=$cursorSent delta=$deltaSent tombol=$buttonsSent tombolTakDipetakan=$buttonsDropped gulir=$scrollSent]"
        )
    }
}

/**
 * Menangkap mouse fisik saat game mengunci kursor (bermain). Gerakan relatif jadi delta kamera.
 * Touchpad yang ikut tertangkap diterjemahkan: selisih posisi jadi gerakan, dua jari jadi gulir
 * dan ketuk dua jari jadi klik kanan.
 */
@Composable
internal fun PhysicalMouseCapture(
    enabled: Boolean,
    onMouse: () -> Unit,
    // Gerakan dari mouse sungguhan (bukan touchpad): tanda bahwa mouse launcher boleh dipakai di menu.
    onRealMouse: () -> Unit = {},
    onMouseMove: (Offset) -> Unit,
    onMouseScroll: (Offset) -> Unit,
    onMouseButton: (button: Int, pressed: Boolean) -> Unit
) {
    val view by rememberUpdatedState(LocalView.current)
    val currentOnMouse by rememberUpdatedState(onMouse)
    val currentOnRealMouse by rememberUpdatedState(onRealMouse)
    val currentOnMouseMove by rememberUpdatedState(onMouseMove)
    val currentOnMouseScroll by rememberUpdatedState(onMouseScroll)
    val currentOnMouseButton by rememberUpdatedState(onMouseButton)

    fun syncCaptureState() {
        if (enabled) {
            MouseDiag.captureRequested(view.hasWindowFocus(), view.hasFocus())
            if (view.hasWindowFocus()) {
                view.requestPointerCapture()
            }
        } else {
            view.releasePointerCapture()
        }
    }

    val composeFocus by SdlBridge.composeFocus.collectAsStateWithLifecycle()
    LaunchedEffect(composeFocus) {
        syncCaptureState()
    }

    DisposableEffect(view, enabled) {
        view.setOnCapturedPointerListener(null)
        val physicalButtons = CapturedMouseButtons { button, pressed ->
            currentOnMouseButton(button, pressed)
        }

        val focusListener = ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
            if (enabled && hasFocus) {
                view.requestPointerCapture()
            } else if (!hasFocus) {
                physicalButtons.reset()
            }
        }
        view.viewTreeObserver.addOnWindowFocusChangeListener(focusListener)

        syncCaptureState()

        if (enabled) {
            // Event touchpad membawa x/y absolut di permukaan pad, bukan selisih. Bila sumbu relatif
            // tidak tersedia, selisih dihitung dari dua posisi berurutan.
            var touchpadLastPos: Offset? = null
            val twoFingerGesture = TouchpadTwoFingerGesture()
            // Touchpad gaya sentuh melaporkan sentuhan pad sebagai DOWN/UP; selama menempel dianggap klik kiri.
            var touchpadLeftPressed = false
            // Bila klik kiri sudah dibatalkan saat dua jari turun, lepasnya jari terakhir tidak dikirim lagi.
            var touchpadLeftSuppressed = false

            val pointerListener = View.OnCapturedPointerListener { _, event ->
                val isTouchpad = event.isFromSource(InputDevice.SOURCE_TOUCHPAD)
                MouseDiag.captureEvents++
                MouseDiag.once(if (isTouchpad) MouseDiag.CAPTURE_TOUCHPAD else MouseDiag.CAPTURE, event)
                if (!isTouchpad) {
                    physicalButtons.onEvent(event.actionMasked, event.actionButton, event.buttonState)
                }

                if (isTouchpad) {
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN,
                        MotionEvent.ACTION_POINTER_DOWN,
                        MotionEvent.ACTION_HOVER_ENTER -> touchpadLastPos = Offset(event.x, event.y)

                        MotionEvent.ACTION_UP,
                        MotionEvent.ACTION_POINTER_UP,
                        MotionEvent.ACTION_HOVER_EXIT -> touchpadLastPos = null
                    }

                    // Cadangan dua jari bila firmware tidak membuat event gulir/klik kanan sendiri.
                    when (event.actionMasked) {
                        MotionEvent.ACTION_POINTER_DOWN ->
                            if (event.pointerCount >= 2 && !twoFingerGesture.isActive()) {
                                twoFingerGesture.start(event)
                                if (touchpadLeftPressed) {
                                    currentOnMouseButton(MotionEvent.BUTTON_PRIMARY, false)
                                    touchpadLeftPressed = false
                                    touchpadLeftSuppressed = true
                                }
                            }

                        MotionEvent.ACTION_MOVE ->
                            if (twoFingerGesture.isActive()) {
                                if (event.pointerCount >= 2) {
                                    twoFingerGesture.onMove(event)?.let { notches ->
                                        if (notches.x != 0f || notches.y != 0f) {
                                            currentOnMouseScroll(notches)
                                        }
                                    }
                                }
                                // Gerak dua jari dihitung sebagai gulir, bukan memutar kamera.
                                return@OnCapturedPointerListener true
                            }

                        MotionEvent.ACTION_POINTER_UP ->
                            if (twoFingerGesture.isActive() && twoFingerGesture.onFingerUp(event)) {
                                currentOnMouseButton(MotionEvent.BUTTON_SECONDARY, true)
                                currentOnMouseButton(MotionEvent.BUTTON_SECONDARY, false)
                            }

                        MotionEvent.ACTION_UP ->
                            if (twoFingerGesture.isActive()) {
                                twoFingerGesture.end()
                                if (touchpadLeftSuppressed) {
                                    touchpadLeftSuppressed = false
                                    return@OnCapturedPointerListener true
                                }
                            }

                        MotionEvent.ACTION_CANCEL ->
                            if (twoFingerGesture.isActive()) {
                                twoFingerGesture.end()
                                touchpadLeftPressed = false
                                touchpadLeftSuppressed = false
                            }
                    }
                }

                when (event.actionMasked) {
                    MotionEvent.ACTION_HOVER_MOVE, MotionEvent.ACTION_MOVE -> {
                        var deltaX = 0f
                        var deltaY = 0f

                        val relX = event.getAxisValue(MotionEvent.AXIS_RELATIVE_X)
                        val relY = event.getAxisValue(MotionEvent.AXIS_RELATIVE_Y)
                        if (isTouchpad) {
                            if (relX != 0f || relY != 0f) {
                                deltaX += relX
                                deltaY += relY
                            } else {
                                touchpadLastPos?.let { last ->
                                    deltaX += event.x - last.x
                                    deltaY += event.y - last.y
                                }
                                touchpadLastPos = Offset(event.x, event.y)
                            }
                        } else {
                            deltaX += if (relX != 0f) relX else event.x
                            deltaY += if (relY != 0f) relY else event.y
                        }

                        val historySize = event.historySize
                        for (i in 0 until historySize) {
                            deltaX += event.getHistoricalAxisValue(MotionEvent.AXIS_RELATIVE_X, i)
                            deltaY += event.getHistoricalAxisValue(MotionEvent.AXIS_RELATIVE_Y, i)
                        }

                        if (!isTouchpad) currentOnRealMouse()
                        currentOnMouse()
                        currentOnMouseMove(Offset(deltaX, deltaY))
                        true
                    }
                    MotionEvent.ACTION_SCROLL -> {
                        currentOnMouseScroll(
                            Offset(
                                event.getAxisValue(MotionEvent.AXIS_HSCROLL),
                                event.getAxisValue(MotionEvent.AXIS_VSCROLL)
                            )
                        )
                        true
                    }
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_BUTTON_PRESS -> {
                        if (isTouchpad && event.actionMasked == MotionEvent.ACTION_DOWN) {
                            touchpadLeftPressed = true
                            touchpadLeftSuppressed = false
                        }
                        if (isTouchpad) currentOnMouseButton(event.actionButton, true)
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_BUTTON_RELEASE -> {
                        if (isTouchpad && event.actionMasked == MotionEvent.ACTION_UP) {
                            touchpadLeftPressed = false
                        }
                        if (isTouchpad) currentOnMouseButton(event.actionButton, false)
                        true
                    }
                    MotionEvent.ACTION_CANCEL -> !isTouchpad
                    else -> false
                }
            }

            view.setOnCapturedPointerListener(pointerListener)
        } else {
            view.setOnCapturedPointerListener(null)
        }

        onDispose {
            physicalButtons.reset()
            view.viewTreeObserver.removeOnWindowFocusChangeListener(focusListener)
            view.setOnCapturedPointerListener(null)
        }
    }
}

/**
 * Mouse fisik di menu game (kursor tidak terkunci): posisi absolut, klik, dan roda gulir.
 * Menolak event yang bukan mouse/stylus supaya sentuhan jari tetap lewat ke penangan sentuh.
 *
 * Tombol yang masih tertahan dilepas saat modifier ini dinonaktifkan (mis. klik "Play" lalu game
 * langsung menangkap mouse), supaya tidak ada tombol yang tersangkut dan klik pertama sesudahnya
 * tidak hilang.
 */
@OptIn(ExperimentalComposeUiApi::class)
internal fun Modifier.physicalMouseHover(
    disabled: Boolean,
    onMouse: () -> Unit = {},
    // Mouse sungguhan (bukan touchpad, bukan stylus): layout boleh menangkapnya dan memakai kursor launcher.
    onRealMouse: () -> Unit = {},
    // Stylus: tetap lewat penunjuk sistem, jadi penangkapan mouse dilepas.
    onStylus: () -> Unit = {},
    onMouseMove: (Offset) -> Unit = {},
    onMouseScroll: (Offset) -> Unit = {},
    onMouseButton: (Int, Boolean) -> Unit = { _, _ -> },
) = composed {
    val currentDisabled by rememberUpdatedState(disabled)
    val currentOnMouse by rememberUpdatedState(onMouse)
    val currentOnRealMouse by rememberUpdatedState(onRealMouse)
    val currentOnStylus by rememberUpdatedState(onStylus)
    val currentOnMouseMove by rememberUpdatedState(onMouseMove)
    val currentOnMouseScroll by rememberUpdatedState(onMouseScroll)
    val currentOnMouseButton by rememberUpdatedState(onMouseButton)

    val buttons = remember {
        CapturedMouseButtons { button, pressed -> currentOnMouseButton(button, pressed) }
    }

    LaunchedEffect(disabled) {
        if (disabled) buttons.reset()
    }

    pointerInteropFilter { event ->
        if (currentDisabled) {
            // Game sedang mengunci kursor, jadi mouse seharusnya lewat jalur tangkap. Event mouse yang
            // tetap sampai ke sini berarti pointer capture tidak diberikan Android.
            if (event.isFromSource(InputDevice.SOURCE_MOUSE)) {
                MouseDiag.menuIgnored++
                MouseDiag.once(MouseDiag.MENU_IGNORED, event)
            }
            return@pointerInteropFilter false
        }

        val isMouse = event.isFromSource(InputDevice.SOURCE_MOUSE)
        // Stylus (Chromebook, Samsung, dsb.) diperlakukan seperti mouse.
        val isStylus = event.isFromSource(InputDevice.SOURCE_STYLUS)
        if (!isMouse && !isStylus) {
            return@pointerInteropFilter false
        }

        MouseDiag.menuEvents++
        MouseDiag.once(if (isMouse) MouseDiag.MENU_MOUSE else MouseDiag.MENU_STYLUS, event)
        MouseDiag.combo(MouseDiag.COMBO_FILTER, event.actionMasked, event.buttonState)
        if (isMouse) {
            // Touchpad yang melapor sebagai mouse tidak ikut: tap-to-click-nya hanya dikenali jalur penunjuk sistem.
            if (event.device?.supportsSource(InputDevice.SOURCE_TOUCHPAD) != true) currentOnRealMouse()
        } else {
            currentOnStylus()
        }
        currentOnMouse()

        // Tombol dan gulir mouse (bukan stylus) dikirim dari GameActivity lewat MenuMouse, sebelum Compose.
        // Di sini keduanya dilewati supaya tidak terkirim dua kali; posisi hover tetap diteruskan.
        val activityOwned = isMouse && MenuMouse.enabled

        if (event.actionMasked == MotionEvent.ACTION_CANCEL) {
            if (!activityOwned) buttons.reset()
            return@pointerInteropFilter true
        }

        if (!activityOwned) buttons.onSnapshot(event.buttonState)

        when (event.actionMasked) {
            MotionEvent.ACTION_HOVER_MOVE,
            MotionEvent.ACTION_MOVE -> {
                currentOnMouseMove(Offset(event.x, event.y))
            }

            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (isStylus) {
                    currentOnMouseButton(MotionEvent.BUTTON_PRIMARY, true)
                }
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_POINTER_UP -> {
                if (isStylus) {
                    currentOnMouseButton(MotionEvent.BUTTON_PRIMARY, false)
                }
            }

            MotionEvent.ACTION_SCROLL -> {
                if (!activityOwned) {
                    currentOnMouseScroll(
                        Offset(
                            event.getAxisValue(MotionEvent.AXIS_HSCROLL),
                            event.getAxisValue(MotionEvent.AXIS_VSCROLL)
                        )
                    )
                }
            }
        }
        true
    }
}

/**
 * Cadangan dua jari di touchpad: tarik dua jari = roda gulir, ketuk dua jari = klik kanan.
 * Ambang dan jarak per langkah gulir mengikuti ukuran permukaan pad.
 */
private class TouchpadTwoFingerGesture {
    private var active = false
    private var tapCandidate = true
    private var downTime = 0L
    private var startCentroid: Offset? = null
    private var lastCentroid: Offset? = null
    private var remainderX = 0f
    private var remainderY = 0f
    private var tapSlop = DEFAULT_TAP_SLOP
    private var scrollNotch = DEFAULT_SCROLL_NOTCH

    fun isActive() = active

    /** Jari kedua turun: mulai gestur. */
    fun start(event: MotionEvent) {
        active = true
        tapCandidate = true
        downTime = event.eventTime
        remainderX = 0f
        remainderY = 0f

        event.device?.let { device ->
            device.getMotionRange(MotionEvent.AXIS_X)?.let {
                val width = it.max - it.min
                if (width > 0f) tapSlop = width * 0.02f
            }
            device.getMotionRange(MotionEvent.AXIS_Y)?.let {
                val height = it.max - it.min
                if (height > 0f) scrollNotch = height * 0.12f
            }
        }

        startCentroid = centroid(event)
        lastCentroid = startCentroid
    }

    /** Null selama masih dalam batas ketukan; sesudahnya jumlah langkah gulir (horizontal, vertikal). */
    fun onMove(event: MotionEvent): Offset? {
        val centroid = centroid(event)
        val last = lastCentroid
        lastCentroid = centroid
        val start = startCentroid
        if (last == null || start == null) return null

        if (tapCandidate && hypot(centroid.x - start.x, centroid.y - start.y) > tapSlop) {
            tapCandidate = false
        }
        if (tapCandidate) return null

        remainderX += centroid.x - last.x
        remainderY -= centroid.y - last.y
        val notchesX = (remainderX / scrollNotch).toInt()
        val notchesY = (remainderY / scrollNotch).toInt()
        remainderX -= notchesX * scrollNotch
        remainderY -= notchesY * scrollNotch
        return Offset(notchesX.toFloat(), notchesY.toFloat())
    }

    /** Satu jari terangkat: true bila masih memenuhi syarat ketukan dua jari (hanya sekali). */
    fun onFingerUp(event: MotionEvent): Boolean {
        val isTap = active && tapCandidate && event.eventTime - downTime <= TAP_TIMEOUT_MS
        if (isTap) tapCandidate = false
        return isTap
    }

    fun end() {
        active = false
        lastCentroid = null
    }

    private fun centroid(event: MotionEvent): Offset {
        var x = 0f
        var y = 0f
        for (i in 0 until event.pointerCount) {
            x += event.getX(i)
            y += event.getY(i)
        }
        return Offset(x / event.pointerCount, y / event.pointerCount)
    }

    companion object {
        private const val DEFAULT_TAP_SLOP = 12f
        private const val DEFAULT_SCROLL_NOTCH = 60f
        private const val TAP_TIMEOUT_MS = 300L
    }
}

// ----------------------------------------------------------------------------
// Section: ui/control/PointerStyle.kt
// ----------------------------------------------------------------------------

/*
 * Tampilan kursor launcher (kursor virtual): satu gambar per bentuk kursor game (panah, tangan, kursor teks,
 * crosshair, ubah ukuran, pindahkan, tidak diizinkan), masing-masing bisa diganti gambar sendiri dan diberi
 * hotspot (titik yang dianggap ujung kursor, dalam persen kotak kursor).
 *
 * Tanpa gambar kustom, bentuknya digambar dari path vektor kecil (tanpa berkas gambar bawaan, tanpa decode).
 * Gambar kustom didecode sekali, dikecilkan ke maksimal 192 px, dan disimpan sebagai ImageBitmap di cache
 * sampai gambarnya berubah. Menggambar kursor tidak mengalokasikan apa pun per frame.
 */
internal object PointerStyle {
    private const val MAX_SIDE = 192
    private const val DIR = "pointers"

    /** Naik tiap kali gambar kustom berubah, supaya tampilan yang membacanya ikut diperbarui. */
    var revision by mutableIntStateOf(0)
        private set

    fun bump() {
        revision++
    }

    val shapes: List<CursorShape> = CursorShape.values().toList()

    fun key(shape: CursorShape): String = when (shape) {
        CursorShape.Arrow -> "arrow"
        CursorShape.IBeam -> "ibeam"
        CursorShape.Hand -> "hand"
        CursorShape.CrossHair -> "crosshair"
        CursorShape.ResizeNS -> "resize_ns"
        CursorShape.ResizeEW -> "resize_ew"
        CursorShape.ResizeAll -> "resize_all"
        CursorShape.NotAllowed -> "not_allowed"
    }

    fun title(shape: CursorShape): String = when (shape) {
        CursorShape.Arrow -> "Penunjuk (Default)"
        CursorShape.IBeam -> "Penunjuk (Kursor Teks)"
        CursorShape.Hand -> "Penunjuk (Tangan)"
        CursorShape.CrossHair -> "Penunjuk (Crosshair)"
        CursorShape.ResizeNS -> "Penunjuk (Atas/Bawah)"
        CursorShape.ResizeEW -> "Penunjuk (Kiri/Kanan)"
        CursorShape.ResizeAll -> "Penunjuk (Pindahkan)"
        CursorShape.NotAllowed -> "Penunjuk (Tidak Diizinkan)"
    }

    fun summary(shape: CursorShape): String = when (shape) {
        CursorShape.Arrow -> "Bentuk biasa. Dipakai di semua versi Minecraft."
        CursorShape.IBeam -> "MC 1.21.9+: dipakai saat kursor di atas kolom teks."
        CursorShape.Hand -> "MC 1.21.9+: dipakai saat kursor di atas komponen yang bisa diklik."
        CursorShape.CrossHair -> "MC 1.21.9+: bentuk crosshair."
        CursorShape.ResizeNS -> "MC 1.21.9+: dipakai di komponen yang bisa digulir ke atas/bawah."
        CursorShape.ResizeEW -> "MC 1.21.9+: dipakai di komponen penggeser (slider)."
        CursorShape.ResizeAll -> "MC 1.21.9+: bentuk pindahkan."
        CursorShape.NotAllowed -> "MC 1.21.9+: dipakai di komponen yang dinonaktifkan."
    }

    // Hotspot bawaan (persen). Panah: ujungnya di pojok kiri atas. Tangan: ujung jari telunjuk.
    fun defaultHotspotX(shape: CursorShape): Int = when (shape) {
        CursorShape.Arrow -> 0
        CursorShape.Hand -> 41
        else -> 50
    }

    fun defaultHotspotY(shape: CursorShape): Int = when (shape) {
        CursorShape.Arrow -> 0
        CursorShape.Hand -> 6
        else -> 50
    }

    fun hotspotX(map: Map<String, Int>, shape: CursorShape): Int =
        (map[key(shape) + ".x"] ?: defaultHotspotX(shape)).coerceIn(0, 100)

    fun hotspotY(map: Map<String, Int>, shape: CursorShape): Int =
        (map[key(shape) + ".y"] ?: defaultHotspotY(shape)).coerceIn(0, 100)

    fun hasHotspot(map: Map<String, Int>, shape: CursorShape): Boolean =
        map.containsKey(key(shape) + ".x") || map.containsKey(key(shape) + ".y")

    fun withHotspot(map: Map<String, Int>, shape: CursorShape, x: Int, y: Int): Map<String, Int> {
        val out = HashMap<String, Int>(map)
        out[key(shape) + ".x"] = x.coerceIn(0, 100)
        out[key(shape) + ".y"] = y.coerceIn(0, 100)
        return out
    }

    fun withoutHotspot(map: Map<String, Int>, shape: CursorShape): Map<String, Int> {
        val out = HashMap<String, Int>(map)
        out.remove(key(shape) + ".x")
        out.remove(key(shape) + ".y")
        return out
    }

    // ---------------------------------------------------------------- gambar kustom

    private fun dir(context: Context): File = File(context.filesDir, DIR)

    fun file(context: Context, shape: CursorShape): File = File(dir(context), key(shape) + ".png")

    private class Entry(val revision: Int, val bitmap: ImageBitmap?)

    // Hanya diakses dari thread utama (saat menyusun tampilan).
    private val cache = HashMap<String, Entry>()

    /**
     * Gambar kustom untuk bentuk ini, atau null bila memakai bentuk vektor bawaan. Dipanggil saat menyusun
     * tampilan: membaca [revision], jadi tampilan ikut disusun ulang begitu gambarnya diganti atau dihapus.
     */
    fun bitmap(context: Context, shape: CursorShape): ImageBitmap? {
        val rev = revision
        val k = key(shape)
        val hit = cache[k]
        if (hit != null && hit.revision == rev) return hit.bitmap
        val f = file(context, shape)
        val bmp: ImageBitmap? = if (f.isFile) {
            try {
                BitmapFactory.decodeFile(f.path)?.asImageBitmap()
            } catch (_: Throwable) {
                null
            }
        } else {
            null
        }
        cache[k] = Entry(rev, bmp)
        return bmp
    }

    /**
     * Salin gambar pilihan pengguna jadi gambar kursor. Dijalankan di thread IO. Hasilnya dibuat persegi
     * (gambar di pojok kiri atas) dan dikecilkan ke maksimal 192 px, jadi hotspot dalam persen selalu merujuk
     * ke kotak yang sama. Mengembalikan false bila gambar tidak bisa dibaca.
     */
    fun importImage(context: Context, shape: CursorShape, uri: Uri): Boolean {
        return try {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options()
            bounds.inJustDecodeBounds = true
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return false

            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_SIDE * 2) sample *= 2
            val opts = BitmapFactory.Options()
            opts.inSampleSize = sample
            val src = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?: return false

            val side = maxOf(src.width, src.height)
            val square = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
            android.graphics.Canvas(square).drawBitmap(src, 0f, 0f, null)
            val result = if (side > MAX_SIDE) Bitmap.createScaledBitmap(square, MAX_SIDE, MAX_SIDE, true) else square

            val folder = dir(context)
            folder.mkdirs()
            val target = file(context, shape)
            val tmp = File(folder, key(shape) + ".tmp")
            tmp.outputStream().use { result.compress(Bitmap.CompressFormat.PNG, 100, it) }
            if (!tmp.renameTo(target)) {
                target.delete()
                if (!tmp.renameTo(target)) return false
            }
            true
        } catch (_: Throwable) {
            false
        }
    }

    /** Hapus gambar kustom (kembali ke bentuk bawaan). */
    fun removeImage(context: Context, shape: CursorShape) {
        file(context, shape).delete()
        bump()
    }

    // ---------------------------------------------------------------- bentuk vektor bawaan

    /**
     * Bentuk bawaan dalam kisi 24 x 24 (dikali kepadatan layar). [fill] diisi putih dengan garis tepi hitam,
     * [lines] digambar sebagai garis tebal hitam lalu garis tipis berwarna [lineColor].
     */
    class Glyph(
        val fill: Path?,
        val shadow: Path?,
        val lines: Path?,
        val lineColor: Color,
        val outline: Stroke,
        val lineBack: Stroke,
        val lineFront: Stroke
    )

    // Bentuk bawaan dibuat sekali per bentuk dan kepadatan layar, bukan tiap kali bentuk kursor game berganti
    // (kursor sering bolak-balik panah/tangan saat melewati tombol). Hanya diakses dari thread utama.
    private var glyphDensity = 0f
    private val glyphCache = arrayOfNulls<Glyph>(shapes.size)

    fun glyphFor(shape: CursorShape, d: Float): Glyph {
        if (glyphDensity != d) {
            glyphDensity = d
            java.util.Arrays.fill(glyphCache, null)
        }
        val i = shape.ordinal
        return glyphCache[i] ?: glyph(shape, d).also { glyphCache[i] = it }
    }

    private fun glyph(shape: CursorShape, d: Float): Glyph {
        var fill: Path? = null
        var shadow: Path? = null
        var lines: Path? = null
        var lineColor = Color.White

        when (shape) {
            CursorShape.Arrow -> {
                val arrow = Path()
                arrow.moveTo(0f, 0f)
                arrow.lineTo(0f, 22f * d)
                arrow.lineTo(5.5f * d, 16.5f * d)
                arrow.lineTo(10.5f * d, 24.5f * d)
                arrow.lineTo(13.5f * d, 23f * d)
                arrow.lineTo(8.5f * d, 15f * d)
                arrow.lineTo(15.5f * d, 15f * d)
                arrow.close()
                val sh = Path()
                sh.addPath(arrow, Offset(2f * d, 2f * d))
                fill = arrow
                shadow = sh
            }

            CursorShape.Hand -> fill = polygon(
                d,
                8f, 1.5f, 11.5f, 1.5f, 11.5f, 9f, 13.5f, 9f, 13.5f, 10f, 16f, 10f, 16f, 11f,
                18.5f, 11f, 18.5f, 12f, 21f, 12f, 21f, 19f, 18.5f, 22.5f, 10.5f, 22.5f,
                7.5f, 18.5f, 4f, 13f, 5.2f, 11.8f, 8f, 14.2f
            )

            CursorShape.IBeam -> {
                val p = Path()
                p.moveTo(12f * d, 3f * d)
                p.lineTo(12f * d, 21f * d)
                p.moveTo(8.5f * d, 3f * d)
                p.lineTo(15.5f * d, 3f * d)
                p.moveTo(8.5f * d, 21f * d)
                p.lineTo(15.5f * d, 21f * d)
                lines = p
            }

            CursorShape.CrossHair -> {
                val p = Path()
                p.moveTo(12f * d, 3f * d)
                p.lineTo(12f * d, 21f * d)
                p.moveTo(3f * d, 12f * d)
                p.lineTo(21f * d, 12f * d)
                lines = p
            }

            CursorShape.ResizeNS -> fill = polygon(
                d,
                12f, 2f, 17f, 8f, 13.5f, 8f, 13.5f, 16f, 17f, 16f, 12f, 22f, 7f, 16f, 10.5f, 16f, 10.5f, 8f, 7f, 8f
            )

            CursorShape.ResizeEW -> fill = polygon(
                d,
                2f, 12f, 8f, 7f, 8f, 10.5f, 16f, 10.5f, 16f, 7f, 22f, 12f, 16f, 17f, 16f, 13.5f, 8f, 13.5f, 8f, 17f
            )

            CursorShape.ResizeAll -> fill = polygon(
                d,
                12f, 1.5f, 16f, 6f, 13.2f, 6f, 13.2f, 10.8f, 18f, 10.8f, 18f, 8f, 22.5f, 12f, 18f, 16f,
                18f, 13.2f, 13.2f, 13.2f, 13.2f, 18f, 16f, 18f, 12f, 22.5f, 8f, 18f, 10.8f, 18f,
                10.8f, 13.2f, 6f, 13.2f, 6f, 16f, 1.5f, 12f, 6f, 8f, 6f, 10.8f, 10.8f, 10.8f, 10.8f, 6f, 8f, 6f
            )

            CursorShape.NotAllowed -> {
                val p = Path()
                p.addOval(Rect(3f * d, 3f * d, 21f * d, 21f * d))
                p.moveTo(5.6f * d, 5.6f * d)
                p.lineTo(18.4f * d, 18.4f * d)
                lines = p
                lineColor = Color(0xFFE53935)
            }
        }

        return Glyph(
            fill = fill,
            shadow = shadow,
            lines = lines,
            lineColor = lineColor,
            outline = Stroke(width = 1.8f * d, cap = StrokeCap.Round, join = StrokeJoin.Round),
            lineBack = Stroke(width = 4.4f * d, cap = StrokeCap.Round, join = StrokeJoin.Round),
            lineFront = Stroke(width = 2.2f * d, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }

    /** Poligon tertutup dari pasangan x, y dalam kisi 24 x 24. */
    private fun polygon(d: Float, vararg xy: Float): Path {
        val p = Path()
        var i = 0
        while (i + 1 < xy.size) {
            if (i == 0) p.moveTo(xy[i] * d, xy[i + 1] * d) else p.lineTo(xy[i] * d, xy[i + 1] * d)
            i += 2
        }
        p.close()
        return p
    }
}

/** Gambar satu bentuk bawaan di kisi 24 x 24 (dikali kepadatan layar). */
internal fun DrawScope.drawGlyph(g: PointerStyle.Glyph) {
    val lines = g.lines
    if (lines != null) {
        drawPath(path = lines, color = Color.Black, style = g.lineBack)
        drawPath(path = lines, color = g.lineColor, style = g.lineFront)
    }
    val fill = g.fill
    if (fill != null) {
        val shadow = g.shadow
        if (shadow != null) {
            drawPath(path = shadow, color = Color.Black.copy(alpha = 0.45f), style = Fill)
        }
        drawPath(path = fill, color = Color.White, style = Fill)
        drawPath(path = fill, color = Color.Black, style = g.outline)
    }
}

/**
 * Gambar kursor di kotak persegi [sizePx] yang pojok kiri atasnya ada di titik (0, 0). Titik hotspot
 * ([hotX], [hotY], 0..1 dari kotak) jadi pusat efek "ditekan" (mengecil sedikit). Gambar kustom dipakai bila
 * ada, kalau tidak bentuk vektor bawaan.
 */
internal fun DrawScope.drawPointer(
    glyph: PointerStyle.Glyph,
    custom: ImageBitmap?,
    d: Float,
    sizePx: Float,
    hotX: Float,
    hotY: Float,
    pressed: Boolean
) {
    val press = if (pressed) 0.88f else 1.0f
    scale(press, pivot = Offset(hotX * sizePx, hotY * sizePx)) {
        if (custom != null) {
            val side = sizePx.toInt().coerceAtLeast(1)
            drawImage(custom, dstSize = IntSize(side, side))
        } else {
            scale(sizePx / (24f * d), pivot = Offset.Zero) {
                drawGlyph(glyph)
            }
        }
    }
}

// ----------------------------------------------------------------------------
// Section: ui/control/SwitchableMouseLayout.kt
// ----------------------------------------------------------------------------

enum class MouseControlMode {
    SLIDE, // Trackpad mode: Drag to move cursor, tap anywhere to click at cursor
    CLICK  // Direct touch: Tap or drag directly sets cursor to finger and clicks
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SwitchableMouseLayout(
    modifier: Modifier = Modifier,
    screenWidth: Float,
    screenHeight: Float,
    cursorMode: Int,
    controlMode: MouseControlMode,
    // Lambda supaya posisi kursor dibaca hanya saat jari bergerak, bukan saat menyusun ulang.
    cursorPosition: () -> Offset,
    cursorSensitivity: Float = 1.0f,
    touchAttackEnabled: Boolean = true,
    disableGestures: Boolean = false,
    longPressDelayMs: Long = 300L,
    disableDoubleTapOffhand: Boolean = false,
    // Mouse launcher di menu: false saat lapisan launcher (menu dalam game, dialog, editor, loading) menutupi
    // game, supaya penunjuk sistem Android bisa dipakai di lapisan itu.
    menuCaptureAllowed: Boolean = true,
    // Pengali kecepatan khusus mouse fisik (1 = sama dengan sensitivitas di atas): kursor launcher di menu dan
    // kamera saat bermain. Tidak memengaruhi jari.
    physicalCursorSpeed: Float = 1f,
    physicalCameraSpeed: Float = 1f,
    // Hotbar sentuh: mengembalikan slot 0..8 di titik sentuh (atau -1). Null = fitur mati.
    hotbarSlotAt: ((Offset) -> Int)? = null,
    onHotbarSelect: (Int) -> Unit = {},
    // Mod TouchController tersambung: sentuhan jari saat bermain juga diteruskan mentah ke penerima ini
    // (koordinat 0..1), sementara lapisan ini tetap bekerja seperti biasa.
    touchSink: TouchSink? = null,
    onCursorPositionChange: (Offset) -> Unit,
    onPhysicalMouseModeChange: (Boolean) -> Unit = {},
    onMouse: () -> Unit = {},
    onTouch: () -> Unit = {},
    onTouchDown: (Offset) -> Unit = {},
    onTap: (Offset) -> Unit = { pos ->
        val winW = CallbackBridge.windowWidth
        val winH = CallbackBridge.windowHeight
        val targetX = if (screenWidth > 0 && winW > 0) pos.x * (winW.toFloat() / screenWidth) else pos.x
        val targetY = if (screenHeight > 0 && winH > 0) pos.y * (winH.toFloat() / screenHeight) else pos.y
        CallbackBridge.putMouseEventWithCoords(
            LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT,
            targetX,
            targetY
        )
    },
    onLongPress: (Offset) -> Unit = {
        CallbackBridge.putMouseEvent(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT, true)
    },
    onLongPressEnd: () -> Unit = {
        CallbackBridge.putMouseEvent(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT, false)
    },
    onCapturedMove: (Offset) -> Unit = { delta ->
        CallbackBridge.sendCursorDelta(delta.x, delta.y)
    }
) {
    val coroutineScope = rememberCoroutineScope()
    val viewConfiguration = LocalViewConfiguration.current
    val currentControlMode by rememberUpdatedState(controlMode)
    val currentCursorMode by rememberUpdatedState(cursorMode)
    val currentCursorPos by rememberUpdatedState(cursorPosition)
    val currentSensitivity by rememberUpdatedState(cursorSensitivity)
    val currentPhysicalCursorSpeed by rememberUpdatedState(physicalCursorSpeed)
    val currentPhysicalCameraSpeed by rememberUpdatedState(physicalCameraSpeed)
    val currentTouchAttack by rememberUpdatedState(touchAttackEnabled)
    val currentDisableGestures by rememberUpdatedState(disableGestures)
    val currentLongPressDelayMs by rememberUpdatedState(longPressDelayMs)
    val currentDisableDoubleTapOffhand by rememberUpdatedState(disableDoubleTapOffhand)
    val currentHotbarSlotAt by rememberUpdatedState(hotbarSlotAt)
    val currentOnHotbarSelect by rememberUpdatedState(onHotbarSelect)
    val currentTouchSink by rememberUpdatedState(touchSink)
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    val currentOnLongPressEnd by rememberUpdatedState(onLongPressEnd)
    val currentOnCapturedMove by rememberUpdatedState(onCapturedMove)
    val currentOnCursorPositionChange by rememberUpdatedState(onCursorPositionChange)
    val currentOnPhysicalMouseModeChange by rememberUpdatedState(onPhysicalMouseModeChange)
    val currentOnMouse by rememberUpdatedState(onMouse)
    val currentOnTouch by rememberUpdatedState(onTouch)
    val currentOnTouchDown by rememberUpdatedState(onTouchDown)
    // Blok sentuh di bawah tidak pernah dimulai ulang, jadi nilai yang bisa berubah dibaca lewat State.
    val currentScreenWidth by rememberUpdatedState(screenWidth)
    val currentScreenHeight by rememberUpdatedState(screenHeight)

    val isCaptured = currentCursorMode == CURSOR_DISABLED

    // Mouse fisik sedang dipakai (diketahui dari event, bukan dari pengaturan). Di menu, mouse ditangkap dan
    // kursor yang terlihat milik launcher; bila Android belum memberi capture, penunjuk sistem jadi cadangan.
    var isPhysicalMouseMode by remember { mutableStateOf(PhysicalMouseChecker.physicalMouseConnected) }

    // Mouse sungguhan (bukan touchpad, bukan stylus) terlihat memakai event. Hanya ini yang boleh ditangkap di
    // menu. Tidak diisi dari daftar perangkat: sebagian keyboard 2.4G memuat antarmuka mouse yang tidak pernah
    // bergerak, dan itu akan menyembunyikan penunjuk sistem tanpa ada mouse yang dipakai.
    var realMouseActive by remember { mutableStateOf(false) }

    fun checkPhysicalMouseMode(using: Boolean) {
        if (!using) realMouseActive = false
        if (isPhysicalMouseMode != using) {
            isPhysicalMouseMode = using
            currentOnPhysicalMouseModeChange(using)
        }
    }

    // Mouse fisik ditangkap Android (penunjuk sistem hilang) dalam dua keadaan:
    //  - bermain (kursor game terkunci): gerakan relatif menggerakkan kamera;
    //  - menu, selama mouse fisik sedang dipakai dan tidak ada lapisan launcher di atas game: gerakan relatif
    //    menggeser kursor milik launcher (digambar GameActivity), sama seperti jari di mode trackpad.
    // Penunjuk sistem hanya dipakai sebagai cadangan selama Android belum benar-benar memberi capture.
    val menuCaptureWanted = !isCaptured && realMouseActive && menuCaptureAllowed
    val capturePointer = isCaptured || menuCaptureWanted
    val captureHeld by LauncherMouse.captureHeld
    val launcherMouseOn = menuCaptureWanted && captureHeld

    // Bentuk penunjuk sistem mengikuti bentuk kursor game. State-nya hanya dibaca di FocusableBox,
    // jadi pergantian bentuk tidak menyusun ulang lapisan ini.
    val cursorShapeState = ZLBridgeStates.cursorShape.collectAsStateWithLifecycle()
    val hoverIcon = remember { derivedStateOf { cursorShapeState.value.composeIcon } }

    // Penerus tombol dan gulir mouse fisik ke game. Satu instance dipakai kedua jalur (menu dan bermain).
    val sendMouseButton = remember<(Int, Boolean) -> Unit> {
        { button, pressed ->
            val glfwBtn = MouseButtons.toGlfw(button)
            if (glfwBtn != null) {
                MouseDiag.buttonsSent++
                // Pintu bersama semua jalur mouse fisik: tekan/lepas ganda dibuang, dan tombol yang tertahan
                // bisa dilepas sekaligus saat fokus hilang (MenuMouse.reset).
                MenuMouse.sendButton(glfwBtn, pressed)
            } else {
                MouseDiag.buttonsDropped++
            }
        }
    }
    val sendMouseScroll = remember<(Offset) -> Unit> {
        { scroll ->
            MouseDiag.scrollSent++
            CallbackBridge.sendScroll(scroll.x.toDouble(), scroll.y.toDouble())
        }
    }

    val composeFocusCount by SdlBridge.composeFocus.collectAsStateWithLifecycle()

    fun sendScaledCursor(x: Float, y: Float) {
        val winW = CallbackBridge.windowWidth
        val winH = CallbackBridge.windowHeight
        val sw = currentScreenWidth
        val sh = currentScreenHeight
        val targetX = if (sw > 0 && winW > 0) x * (winW.toFloat() / sw) else x
        val targetY = if (sh > 0 && winH > 0) y * (winH.toFloat() / sh) else y
        CallbackBridge.sendCursorPos(targetX, targetY)
    }

    // Mouse launcher di menu: menggeser kursor launcher sebesar gerakan relatif mouse, lalu memberi tahu game.
    fun moveMenuCursor(delta: Offset) {
        val cur = currentCursorPos()
        val sens = currentSensitivity * currentPhysicalCursorSpeed
        val nx = (cur.x + delta.x * sens).coerceIn(0f, currentScreenWidth)
        val ny = (cur.y + delta.y * sens).coerceIn(0f, currentScreenHeight)
        MouseDiag.cursorSent++
        currentOnCursorPositionChange(Offset(nx, ny))
        sendScaledCursor(nx, ny)
    }

    // Game bisa memindahkan kursornya sendiri (ke tengah saat layar baru terbuka): posisi kursor launcher
    // disamakan sebelum klik dan gulir.
    DisposableEffect(Unit) {
        LauncherMouse.syncCursor = {
            val p = currentCursorPos()
            sendScaledCursor(p.x, p.y)
        }
        onDispose { LauncherMouse.syncCursor = null }
    }

    FocusableBox(
        modifier = modifier
            .fillMaxSize()
            // 1. Touchscreen input processing.
            // Kunci Unit: blok ini TIDAK dimulai ulang saat mode kursor atau mode kontrol berganti.
            // Dulu dimulai ulang tiap inventori dibuka/ditutup, sehingga jari yang sudah menempel
            // dilupakan dan baru bergerak lagi setelah diangkat. Semua nilai berubah dibaca lewat State.
            .pointerInput(Unit) {
                coroutineScope {
                    var activePointerId: PointerId? = null
                    var startPosition = Offset.Zero
                    var isDragging = false
                    var longPressTriggered = false
                    var longPressJob: Job? = null
                    // Slot hotbar yang disentuh jari ini (-1 = bukan hotbar). Hanya saat bermain.
                    var hotbarSlot = -1
                    // Jari KEDUA yang mengetuk hotbar saat jari pertama masih memegang kamera (atau ketukan lain).
                    // Dulu hanya satu jari yang dilacak, jadi hotbar tidak bisa diketuk selama layar sedang digeser.
                    // Jari ini tidak menggerakkan kamera; ketukan singkatnya saja yang memilih slot.
                    var hotbarPointerId: PointerId? = null
                    var hotbarPointerSlot = -1
                    var hotbarPointerStart = Offset.Zero
                    var hotbarPointerMoved = false
                    // Jari yang sedang diteruskan ke mod.
                    val forwarder = TouchForwarder()
                    // Status kursor terkunci pada kejadian sebelumnya, untuk mendeteksi pergantian mode.
                    var lastCaptured = currentCursorMode == CURSOR_DISABLED
                    var lastHotbarTapSlot = -1
                    var lastHotbarTapAt = 0L

                    fun handleHotbarTap(slot: Int) {
                        val now = android.os.SystemClock.uptimeMillis()
                        if (!currentDisableDoubleTapOffhand &&
                            slot == lastHotbarTapSlot &&
                            (now - lastHotbarTapAt) in 1L..320L
                        ) {
                            lastHotbarTapAt = 0L
                            lastHotbarTapSlot = -1
                            currentOnHotbarSelect(slot)
                            CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_F, true)
                            CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_F, false)
                        } else {
                            lastHotbarTapSlot = slot
                            lastHotbarTapAt = now
                            currentOnHotbarSelect(slot)
                        }
                    }

                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()

                            // Mod TouchController (lihat touchSink di atas).
                            val sink = currentTouchSink
                            if (sink != null) {
                                // Berdampingan: sentuhan tetap diproses launcher di bawah (tidak dikonsumsi),
                                // dan salinannya dikirim ke mod hanya saat bermain (kursor terkunci). Di menu
                                // game launcher yang bekerja sendiri, kalau tidak klik akan terhitung dua kali.
                                val invW = if (currentScreenWidth > 0f) 1f / currentScreenWidth else 0f
                                val invH = if (currentScreenHeight > 0f) 1f / currentScreenHeight else 0f
                                val fChanges = event.changes
                                for (ci in fChanges.indices) {
                                    val c = fChanges[ci]
                                    if (c.type != PointerType.Touch) continue
                                    val key = c.id.value
                                    if (c.changedToDown()) {
                                        // Jari yang sudah dipakai tombol launcher (sudah dikonsumsi) tidak ikut dikirim.
                                        if (!c.isConsumed && currentCursorMode == CURSOR_DISABLED) {
                                            forwarder.down(sink, key, c.position.x * invW, c.position.y * invH)
                                        }
                                    } else if (c.changedToUpIgnoreConsumed()) {
                                        forwarder.up(sink, key)
                                    } else if (c.pressed && c.positionChanged()) {
                                        forwarder.move(sink, key, c.position.x * invW, c.position.y * invH)
                                    }
                                }
                            }

                            // Mode kursor berganti (inventori dibuka/ditutup) saat jari sudah menempel: jari yang
                            // sama diteruskan ke mode baru tanpa harus diangkat dulu.
                            val capturedNow0 = currentCursorMode == CURSOR_DISABLED
                            if (capturedNow0 != lastCaptured) {
                                lastCaptured = capturedNow0
                                // Ketukan hotbar tambahan tidak berlaku lagi setelah mode berganti (inventori dibuka/ditutup).
                                hotbarPointerId = null
                                val heldId = activePointerId
                                if (heldId != null) {
                                    longPressJob?.cancel()
                                    if (longPressTriggered) {
                                        // Tombol kiri yang sedang ditahan dilepas, supaya tidak ikut tertahan di mode baru.
                                        longPressTriggered = false
                                        currentOnLongPressEnd()
                                    }
                                    hotbarSlot = -1
                                    // Dianggap sudah menggeser: jari langsung menggerakkan kamera/kursor, dan saat
                                    // diangkat tidak menghasilkan ketukan (pukul/klik) yang tidak disengaja.
                                    isDragging = true
                                    val heldChange = event.changes.firstFast { it.id == heldId }
                                    if (heldChange != null) {
                                        startPosition = heldChange.position
                                        if (!capturedNow0 && currentControlMode == MouseControlMode.CLICK) {
                                            currentOnCursorPositionChange(heldChange.position)
                                            sendScaledCursor(heldChange.position.x, heldChange.position.y)
                                        }
                                    }
                                }
                            }

                            // Touch Down (perulangan biasa: tanpa membuat daftar baru di tiap kejadian sentuh)
                            val changes = event.changes
                            for (ci in changes.indices) {
                                val downEvent = changes[ci]
                                if (!downEvent.changedToDown() || downEvent.type != PointerType.Touch) {
                                    continue
                                }

                                checkPhysicalMouseMode(false)
                                currentOnTouch()

                                // Jari tambahan saat bermain (kursor terkunci): satu jari sudah memegang kamera atau
                                // ketukan, tetapi hotbar tetap harus bisa diketuk dengan jari lain.
                                if (activePointerId != null && !downEvent.isConsumed &&
                                    hotbarPointerId == null && currentCursorMode == CURSOR_DISABLED
                                ) {
                                    val lookup = currentHotbarSlotAt
                                    if (lookup != null) {
                                        val slotHere = lookup(downEvent.position)
                                        if (slotHere >= 0) {
                                            // Jari baru jatuh di hotbar: ketukan singkatnya memilih slot dan jari
                                            // pertama (kamera) tidak terganggu.
                                            hotbarPointerId = downEvent.id
                                            hotbarPointerSlot = slotHere
                                            hotbarPointerStart = downEvent.position
                                            hotbarPointerMoved = false
                                        } else if (hotbarSlot >= 0 && !isDragging && !longPressTriggered) {
                                            // Jari pertama masih menunggu ketukan di hotbar, lalu jari baru jatuh di luar
                                            // hotbar: jari baru menjadi jari kamera, jari pertama tetap menjadi ketukan hotbar.
                                            hotbarPointerId = activePointerId
                                            hotbarPointerSlot = hotbarSlot
                                            hotbarPointerStart = startPosition
                                            hotbarPointerMoved = false
                                            longPressJob?.cancel()
                                            activePointerId = null
                                            hotbarSlot = -1
                                        }
                                    }
                                }

                                if (activePointerId == null && !downEvent.isConsumed) {
                                    currentOnTouchDown(downEvent.position)
                                    activePointerId = downEvent.id
                                    startPosition = downEvent.position
                                    isDragging = false
                                    longPressTriggered = false

                                    val capturedNow = currentCursorMode == CURSOR_DISABLED
                                    hotbarSlot = if (capturedNow) {
                                        currentHotbarSlotAt?.invoke(downEvent.position) ?: -1
                                    } else {
                                        -1
                                    }

                                    if (!capturedNow && currentControlMode == MouseControlMode.CLICK) {
                                        currentOnCursorPositionChange(downEvent.position)
                                        sendScaledCursor(downEvent.position.x, downEvent.position.y)
                                    }

                                    longPressJob?.cancel()
                                    longPressJob = launch {
                                        delay(currentLongPressDelayMs.coerceIn(80L, 1500L))
                                        // Saat bermain (kursor terkunci), tahan di layar hanya memukul bila diizinkan
                                        if (!isDragging && activePointerId == downEvent.id && hotbarSlot < 0 &&
                                            (!capturedNow || (!currentDisableGestures && currentTouchAttack))
                                        ) {
                                            longPressTriggered = true
                                            val targetPos = if (capturedNow) {
                                                Offset.Zero
                                            } else if (currentControlMode == MouseControlMode.CLICK) {
                                                startPosition
                                            } else {
                                                currentCursorPos()
                                            }
                                            currentOnLongPress(targetPos)
                                        }
                                    }
                                }
                            }

                            // Touch Move
                            activePointerId?.let { pointerId ->
                                event.changes.firstFast { it.id == pointerId && it.positionChanged() && !it.isConsumed }?.let { moveChange ->
                                    val distanceFromStart = (moveChange.position - startPosition).getDistance()
                                    // Kejadian ini baru melewati ambang geser: gerak sebelumnya (di dalam ambang)
                                    // belum dipakai di beberapa mode, jadi dihitung dari titik awal agar tidak hilang.
                                    var crossedSlop = false
                                    if (!isDragging && distanceFromStart > viewConfiguration.touchSlop) {
                                        isDragging = true
                                        crossedSlop = true
                                        longPressJob?.cancel()
                                    }

                                    if (currentCursorMode == CURSOR_DISABLED) {
                                        // In gameplay: touch drag rotates the camera. Ketukan di hotbar tidak
                                        // menggoyang kamera selama jari belum benar-benar menggeser.
                                        if (hotbarSlot < 0 || isDragging) {
                                            val delta = if (crossedSlop && hotbarSlot >= 0) {
                                                moveChange.position - startPosition
                                            } else {
                                                moveChange.positionChange()
                                            }
                                            currentOnCapturedMove(delta)
                                        }
                                    } else {
                                        // In menu: move cursor
                                        if (currentControlMode == MouseControlMode.SLIDE) {
                                            if (isDragging || longPressTriggered) {
                                                val delta = if (crossedSlop && !longPressTriggered) {
                                                    moveChange.position - startPosition
                                                } else {
                                                    moveChange.positionChange()
                                                }
                                                val cur = currentCursorPos()
                                                val newX = (cur.x + delta.x * currentSensitivity).coerceIn(0f, currentScreenWidth)
                                                val newY = (cur.y + delta.y * currentSensitivity).coerceIn(0f, currentScreenHeight)
                                                val newPos = Offset(newX, newY)
                                                currentOnCursorPositionChange(newPos)
                                                sendScaledCursor(newX, newY)
                                            }
                                        } else {
                                            currentOnCursorPositionChange(moveChange.position)
                                            sendScaledCursor(moveChange.position.x, moveChange.position.y)
                                        }
                                    }
                                    moveChange.consume()
                                }
                            }

                            // Touch Up
                            activePointerId?.let { pointerId ->
                                event.changes.firstFast { it.id == pointerId && it.changedToUpIgnoreConsumed() }?.let { upChange ->
                                    longPressJob?.cancel()
                                    if (longPressTriggered) {
                                        currentOnLongPressEnd()
                                    } else if (!isDragging) {
                                        if (hotbarSlot >= 0 && currentCursorMode == CURSOR_DISABLED) {
                                            // Ketuk hotbar = pilih slot (gaya Bedrock), ketuk 2x = đổi tay phụ
                                            handleHotbarTap(hotbarSlot)
                                        } else if (currentCursorMode == CURSOR_DISABLED) {
                                            // Ketuk layar saat bermain = pukul; hanya bila diizinkan di Pengaturan
                                            if (!currentDisableGestures && currentTouchAttack) {
                                                CallbackBridge.putMouseEvent(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT)
                                            }
                                        } else {
                                            val clickPos = if (currentControlMode == MouseControlMode.CLICK) {
                                                upChange.position
                                            } else {
                                                currentCursorPos()
                                            }
                                            currentOnTap(clickPos)
                                        }
                                    }
                                    upChange.consume()
                                    activePointerId = null
                                    hotbarSlot = -1
                                }
                            }

                            // Jari hotbar tambahan: tidak menggerakkan kamera. Bila diangkat tanpa bergeser, slotnya dipilih.
                            hotbarPointerId?.let { hotbarId ->
                                val hc = event.changes.firstFast { it.id == hotbarId }
                                if (hc == null) {
                                    hotbarPointerId = null
                                } else if (hc.changedToUpIgnoreConsumed()) {
                                    if (!hotbarPointerMoved && currentCursorMode == CURSOR_DISABLED) {
                                        handleHotbarTap(hotbarPointerSlot)
                                    }
                                    hc.consume()
                                    hotbarPointerId = null
                                } else if (!hotbarPointerMoved &&
                                    (hc.position - hotbarPointerStart).getDistance() > viewConfiguration.touchSlop
                                ) {
                                    hotbarPointerMoved = true
                                }
                            }

                            if (!event.changes.anyPressed()) {
                                activePointerId = null
                                hotbarSlot = -1
                                hotbarPointerId = null
                                isDragging = false
                                longPressJob?.cancel()
                            }
                        }
                    }
                }
            }
            // 2. Mouse fisik di menu game (kursor tidak terkunci): posisi absolut, klik, gulir.
            .then(
                Modifier.physicalMouseHover(
                    // Mati hanya bila capture benar-benar dipegang (bermain, atau mouse launcher di menu).
                    // Selama menunggu capture di menu, penunjuk sistem tetap berfungsi sebagai cadangan.
                    disabled = isCaptured || launcherMouseOn,
                    onMouse = {
                        checkPhysicalMouseMode(true)
                        currentOnMouse()
                    },
                    onRealMouse = { realMouseActive = true },
                    onStylus = { realMouseActive = false },
                    onMouseMove = { pos ->
                        val newX = pos.x.coerceIn(0f, currentScreenWidth)
                        val newY = pos.y.coerceIn(0f, currentScreenHeight)
                        MouseDiag.cursorSent++
                        currentOnCursorPositionChange(Offset(newX, newY))
                        sendScaledCursor(newX, newY)
                    },
                    onMouseScroll = sendMouseScroll,
                    onMouseButton = sendMouseButton
                )
            ),
        requestKey = cursorMode to composeFocusCount,
        hoverIcon = hoverIcon
    )

    // 3. Pointer capture (aktif saat bermain / CURSOR_DISABLED): gerakan relatif jadi delta kamera.
    PhysicalMouseCapture(
        enabled = capturePointer,
        onMouse = {
            checkPhysicalMouseMode(true)
            currentOnMouse()
        },
        onRealMouse = { realMouseActive = true },
        onMouseMove = { delta ->
            if (currentCursorMode == CURSOR_DISABLED) {
                MouseDiag.deltaSent++
                currentOnCapturedMove(delta * currentPhysicalCameraSpeed)
            } else {
                moveMenuCursor(delta)
            }
        },
        onMouseScroll = { scroll ->
            if (currentCursorMode != CURSOR_DISABLED) LauncherMouse.syncCursor?.invoke()
            sendMouseScroll(scroll)
        },
        onMouseButton = { button, pressed ->
            if (currentCursorMode == CURSOR_DISABLED) {
                sendMouseButton(button, pressed)
            } else {
                // Menu: pelacak yang sama dengan klik kanan KEYCODE_BACK, jadi tidak terkirim dua kali.
                MenuMouse.setButton(button, pressed)
            }
        }
    )
}

/** Pengganti firstOrNull untuk jalur sentuhan: perulangan berindeks tanpa membuat iterator. */
internal inline fun List<PointerInputChange>.firstFast(predicate: (PointerInputChange) -> Boolean): PointerInputChange? {
    for (i in indices) {
        val c = this[i]
        if (predicate(c)) return c
    }
    return null
}

internal fun List<PointerInputChange>.anyPressed(): Boolean {
    for (i in indices) {
        if (this[i].pressed) return true
    }
    return false
}
