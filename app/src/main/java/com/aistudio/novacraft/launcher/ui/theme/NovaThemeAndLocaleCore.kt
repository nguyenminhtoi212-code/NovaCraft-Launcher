package com.aistudio.novacraft.launcher.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.novacraft.launcher.core.utils.LauncherLog
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

// ----------------------------------------------------------------------------
// Section: ui/theme/NovaThemeLayout.kt
// ----------------------------------------------------------------------------

// ============================================================================
// NovaCraft Unified Display Cutout Insets & Adaptive DPI Responsive System
// ============================================================================

@Composable
fun rememberCameraSideInsets(gap: Dp = 4.dp): Pair<Dp, Dp> {
    val density = LocalDensity.current
    val layoutDir = LocalLayoutDirection.current
    val view = LocalView.current

    val cutout = WindowInsets.displayCutout
    val safeLeft = cutout.getLeft(density, layoutDir)
    val safeRight = cutout.getRight(density, layoutDir)

    var leftPx = 0
    var rightPx = 0
    if (safeLeft > 0 || safeRight > 0) {
        val rawDensity = view.resources.displayMetrics.density.coerceAtLeast(1f)
        val cap = (48f * rawDensity).toInt()
        leftPx = minOf(safeLeft, cap)
        rightPx = minOf(safeRight, cap)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val rects = view.rootWindowInsets?.displayCutout?.boundingRects
            val width = view.width
            if (!rects.isNullOrEmpty() && width > 0) {
                val edgeTol = (4f * rawDensity).toInt().coerceAtLeast(4)
                var camLeft = 0
                var camRight = 0
                for (r in rects) {
                    val touchesLeft = r.left <= edgeTol
                    val touchesRight = r.right >= width - edgeTol
                    if (touchesLeft && !touchesRight) camLeft = maxOf(camLeft, r.right)
                    if (touchesRight && !touchesLeft) camRight = maxOf(camRight, width - r.left)
                }
                if (camLeft > 0) leftPx = minOf(safeLeft, camLeft)
                if (camRight > 0) rightPx = minOf(safeRight, camRight)
            }
        }
    }

    val left = if (leftPx > 0) with(density) { leftPx.toDp() } + gap else 0.dp
    val right = if (rightPx > 0) with(density) { rightPx.toDp() } + gap else 0.dp
    return Pair(left, right)
}

data class NuxScaleMetrics(
    val scaleFactor: Float = 1.0f,
    val fontScaleFactor: Float = 1.0f,
    val isCompact: Boolean = false,
    val isTablet: Boolean = false,
    val screenWidthDp: Dp = 800.dp,
    val screenHeightDp: Dp = 392.dp
) {
    fun scaledDp(value: Dp): Dp = (value.value * scaleFactor).dp
    fun scaledSp(value: TextUnit): TextUnit = (value.value * fontScaleFactor).sp
}

val LocalNuxScale = staticCompositionLocalOf { NuxScaleMetrics() }

@Composable
fun NuxResponsiveTheme(
    content: @Composable () -> Unit
) {
    val configuration = LocalConfiguration.current
    val systemDensity = LocalDensity.current

    val screenHeightDp = configuration.screenHeightDp.dp
    val screenWidthDp = configuration.screenWidthDp.dp

    val shortSideDp = min(screenWidthDp.value, screenHeightDp.value)
    val baseRatio = shortSideDp / 390f

    val isTablet = configuration.smallestScreenWidthDp >= 600
    val isCompact = shortSideDp < 350f

    val scaleFactor = min(max(baseRatio, 0.86f), if (isTablet) 1.16f else 1.06f)
    val clampedSystemFontScale = min(max(systemDensity.fontScale, 0.90f), 1.12f)

    val metrics = NuxScaleMetrics(
        scaleFactor = scaleFactor,
        fontScaleFactor = scaleFactor * clampedSystemFontScale,
        isCompact = isCompact,
        isTablet = isTablet,
        screenWidthDp = screenWidthDp,
        screenHeightDp = screenHeightDp
    )

    val responsiveDensity = Density(
        density = systemDensity.density * scaleFactor,
        fontScale = clampedSystemFontScale
    )

    CompositionLocalProvider(
        LocalNuxScale provides metrics,
        LocalDensity provides responsiveDensity
    ) {
        content()
    }
}

@Composable
fun NuxReadable(
    scale: Float = 0.94f,
    boost: Float = 1f,
    content: @Composable () -> Unit
) {
    val current = LocalDensity.current
    val readable = Density(
        density = current.density * scale,
        fontScale = min(current.fontScale * boost, 1.15f)
    )
    val alreadyScaled = LocalNuxScaled.current
    CompositionLocalProvider(
        LocalDensity provides readable,
        LocalContentColor provides NuxColors.DarkGray,
        LocalNuxScaled provides (alreadyScaled || scale != 1f)
    ) {
        content()
    }
}

val LocalNuxScaled = compositionLocalOf { false }

@Composable
fun Dp.resp(): Dp = this

@Composable
fun TextUnit.resp(): TextUnit = this

@Composable
fun Int.respDp(): Dp = this.dp

@Composable
fun Float.respDp(): Dp = this.dp

@Composable
fun Int.respSp(): TextUnit = this.sp

@Composable
fun Float.respSp(): TextUnit = this.sp

// ----------------------------------------------------------------------------
// Section: ui/theme/NovaThemeSystem.kt
// ----------------------------------------------------------------------------

// ============================================================================
// NovaCraft Unified Theme, Accent, Light/Dark & Liquid Glass System
// ============================================================================

/**
 * Palet identitas NovaCraft untuk dialog sistem, panel crash, dan HUD dalam game.
 */
object Amber {
    val Ink = Color(0xFF080E24)
    val Yellow = Color(0xFF22D3EE)
    val YellowSoft = Color(0xFF67E8F9)
    val YellowTint = Color(0x2622D3EE)

    val Panel = Color(0xF40B1328)
    val Glass = Color(0xDE0B1328)

    val Fill = Color(0x1438BDF8)
    val Line = Color(0x2B94A3B8)
    val LineStrong = Color(0x54BAE6FD)
    val Press = Color(0x1FFFFFFF)

    val Text = Color(0xFFF8FAFC)
    val TextDim = Color(0xFFAEBBD0)
    val TextFaint = Color(0xFF7D8DA6)

    val Danger = Color(0xFFF43F5E)
    val DangerSoft = Color(0xFFFDA4AF)
    val DangerTint = Color(0x24F43F5E)
    val Good = Color(0xFF10B981)
}

class AmberAccent(
    val id: String,
    val label: String,
    val base: Color,
    val edge: Color,
    val soft: Color,
    val deep: Color,
    val lightText: Color
)

val AMBER_ACCENTS = listOf(
    AmberAccent("navy", "Nova Cyan", Color(0xFF0891B2), Color(0xFF67E8F9), Color(0xFF5EEAD4), Color(0xFF0E4A5C), Color(0xFF0E7490)),
    AmberAccent("green", "Emerald", Color(0xFF10B981), Color(0xFF6EE7B7), Color(0xFF86EFAC), Color(0xFF065F46), Color(0xFF047857)),
    AmberAccent("purple", "Nebula", Color(0xFF7C3AED), Color(0xFFC4B5FD), Color(0xFFD8B4FE), Color(0xFF4C1D95), Color(0xFF6D28D9)),
    AmberAccent("teal", "Cobalt", Color(0xFF2563EB), Color(0xFF93C5FD), Color(0xFFA5F3FC), Color(0xFF1E3A8A), Color(0xFF1D4ED8))
)

/** Mode tema launcher: DARK = Gelap, LIGHT = Terang, SYSTEM = mengikuti tema Android. */
enum class AmberThemeMode(val id: String, val label: String) {
    DARK("dark", "Gelap"),
    LIGHT("light", "Terang"),
    SYSTEM("system", "Ikuti sistem");

    companion object {
        fun fromId(id: String?): AmberThemeMode = entries.firstOrNull { it.id == id } ?: DARK
    }
}

/** Gaya tampilan: DEFAULT = tema NovaCraft Glass, LIQUID = kaca buram (blur) ala liquid glass. */
enum class AmberStyle(val id: String, val label: String) {
    DEFAULT("default", "Default"),
    LIQUID("liquid", "Liquid Glass");

    companion object {
        fun fromId(id: String?): AmberStyle = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}

enum class AmberColorSource(val id: String, val label: String) {
    NOVACRAFT("novacraft", "NovaCraft"),
    DYNAMIC("dynamic", "Màu động hệ thống (Monet)"),
    SYSTEM_ACCENT("system_accent", "Màu nhấn thiết bị");

    companion object {
        fun fromId(id: String?): AmberColorSource = entries.firstOrNull { it.id == id } ?: NOVACRAFT
    }
}

/**
 * Palet desain NovaCraft: mendukung mode Gelap (obsidian kosmik #080E24) dan mode Terang
 * (kaca frost cerah #F1F5F9) secara langsung di seluruh antarmuka.
 */
object AmberUi {
    private var accentPreset by mutableStateOf(AMBER_ACCENTS.first())
    private var dynamicBaseColor by mutableStateOf<Color?>(null)

    var themeMode by mutableStateOf(AmberThemeMode.DARK)
        private set

    var colorSource by mutableStateOf(AmberColorSource.NOVACRAFT)
        private set

    var systemDark by mutableStateOf(true)
        private set

    val isDark: Boolean
        get() = when (themeMode) {
            AmberThemeMode.DARK -> true
            AmberThemeMode.LIGHT -> false
            AmberThemeMode.SYSTEM -> systemDark
        }

    var style by mutableStateOf(AmberStyle.DEFAULT)
        private set

    var dim by mutableStateOf(0.5f)
        private set

    var animScreenTransitions by mutableStateOf(true)
        private set
    var animDialogsPopups by mutableStateOf(true)
        private set
    var animButtonEffects by mutableStateOf(true)
        private set
    var animListsContent by mutableStateOf(true)
        private set

    fun applyDim(value: Float) {
        dim = value.coerceIn(0f, 1f)
    }

    fun setAccent(id: String?) {
        accentPreset = AMBER_ACCENTS.firstOrNull { it.id == id } ?: AMBER_ACCENTS.first()
    }

    fun setStyle(id: String?) {
        style = AmberStyle.fromId(id)
    }

    fun setThemeMode(id: String?) {
        themeMode = AmberThemeMode.fromId(id)
    }

    fun setColorSource(id: String?, context: Context? = null) {
        colorSource = AmberColorSource.fromId(id)
        if (context != null) updateDynamicAccent(context)
    }

    fun updateDynamicAccent(context: Context) {
        dynamicBaseColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val resId = if (colorSource == AmberColorSource.DYNAMIC) {
                    android.R.color.system_accent1_500
                } else {
                    android.R.color.system_accent2_500
                }
                Color(context.resources.getColor(resId, context.theme))
            } catch (_: Throwable) {
                null
            }
        } else {
            null
        }
    }

    fun setAnimations(
        screenTransitions: Boolean,
        dialogsPopups: Boolean,
        buttonEffects: Boolean,
        listsContent: Boolean
    ) {
        animScreenTransitions = screenTransitions
        animDialogsPopups = dialogsPopups
        animButtonEffects = buttonEffects
        animListsContent = listsContent
    }

    fun updateSystemDark(dark: Boolean) {
        systemDark = dark
    }

    private val activeDynamic: Color?
        get() = if (colorSource != AmberColorSource.NOVACRAFT) dynamicBaseColor else null

    val Accent: Color get() = activeDynamic ?: accentPreset.base
    val AccentEdge: Color
        get() = activeDynamic?.let { lerp(it, Color.White, if (isDark) 0.35f else 0.12f) }
            ?: if (isDark) accentPreset.edge else accentPreset.base
    val AccentSoft: Color
        get() = activeDynamic?.let {
            if (isDark) lerp(it, Color.White, 0.45f) else lerp(it, Color.Black, 0.25f)
        } ?: if (isDark) accentPreset.soft else accentPreset.lightText
    val AccentDeep: Color
        get() = activeDynamic?.let { lerp(it, Color.Black, 0.45f) } ?: accentPreset.deep
    val OnAccent: Color = Color.White

    val ObsidianDeep: Color get() = if (isDark) Color(0xFF080E24) else Color(0xFFF1F5F9)
    val ObsidianSlate: Color get() = if (isDark) Color(0xFF0E1730) else Color(0xFFE2E8F0)
    val Surface: Color get() = if (isDark) Color(0xB80B1226) else Color(0xEAFFFFFF)
    val SurfaceRaised: Color get() = if (isDark) Color(0x1E7DD3FC) else Color(0xFFE8EFF8)
    val Border: Color get() = if (isDark) Color(0x2894A3B8) else Color(0x4064748B)
    val BorderStrong: Color get() = if (isDark) Color(0x52BAE6FD) else Color(0x66475569)
    val Text: Color get() = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val TextDim: Color get() = if (isDark) Color(0xFFB3C0D4) else Color(0xFF475569)
    val ModrinthGreen: Color get() = if (isDark) Color(0xFF1BD96A) else Color(0xFF059669)

    val HomeGlass: Color get() = if (isDark) Color(0xCC0A1124) else Color(0xE0FFFFFF)
    val HomeGlassEdge: Color get() = if (isDark) Color(0x457DD3FC) else Color(0x7894A3B8)

    val MenuCard: Color get() = if (isDark) Color(0xF00B1328) else Color(0xFAFFFFFF)

    val Danger: Color get() = if (isDark) Color(0xFFF43F5E) else Color(0xFFE11D48)
}

fun amberScrimBrush(dim: Float): Brush {
    val d = dim.coerceIn(0f, 1f)
    return if (AmberUi.isDark) {
        val top = (0.35f + d * 0.55f).coerceAtMost(0.92f)
        val bottom = (0.50f + d * 0.45f).coerceAtMost(0.96f)
        Brush.verticalGradient(
            listOf(
                Color(0xFF070C1D).copy(alpha = top),
                Color(0xFF0A1128).copy(alpha = (top + bottom) / 2f),
                Color(0xFF060A18).copy(alpha = bottom)
            )
        )
    } else {
        val top = (0.48f + d * 0.44f).coerceAtMost(0.92f)
        val bottom = (0.60f + d * 0.36f).coerceAtMost(0.96f)
        Brush.verticalGradient(
            listOf(
                Color(0xFFF8FAFC).copy(alpha = top),
                Color(0xFFF1F5F9).copy(alpha = (top + bottom) / 2f),
                Color(0xFFE2E8F0).copy(alpha = bottom)
            )
        )
    }
}

val LocalGlassBackdrop = staticCompositionLocalOf<GraphicsLayer?> { null }

enum class AmberGlassKind {
    Panel,
    Home
}

fun Modifier.amberGlass(shape: Shape, kind: AmberGlassKind = AmberGlassKind.Panel): Modifier = composed {
    val style = AmberUi.style
    val dark = AmberUi.isDark
    val backdrop = LocalGlassBackdrop.current
    if (style == AmberStyle.LIQUID && backdrop != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        var origin by remember { mutableStateOf(Offset.Zero) }
        val shadeTint = if (dark) {
            val shade = if (kind == AmberGlassKind.Home) 0.40f else 0.18f + 0.42f * AmberUi.dim
            Color.Black.copy(alpha = shade)
        } else {
            val shade = if (kind == AmberGlassKind.Home) 0.62f else 0.56f + 0.34f * AmberUi.dim
            Color.White.copy(alpha = shade)
        }
        val glow = if (dark) {
            Brush.verticalGradient(
                0f to Color.White.copy(alpha = 0.16f),
                0.5f to Color.White.copy(alpha = 0.03f),
                1f to Color.Transparent
            )
        } else {
            Brush.verticalGradient(
                0f to Color.White.copy(alpha = 0.55f),
                0.5f to Color.White.copy(alpha = 0.12f),
                1f to Color.Transparent
            )
        }
        val rim = if (dark) {
            Brush.linearGradient(
                0f to Color.White.copy(alpha = 0.60f),
                0.5f to Color.White.copy(alpha = 0.08f),
                1f to Color.White.copy(alpha = 0.30f)
            )
        } else {
            Brush.linearGradient(
                0f to Color.White.copy(alpha = 0.90f),
                0.5f to Color(0xFF64748B).copy(alpha = 0.25f),
                1f to Color.White.copy(alpha = 0.72f)
            )
        }
        this
            .onGloballyPositioned { origin = it.positionInRoot() }
            .clip(shape)
            .drawBehind {
                translate(-origin.x, -origin.y) { drawLayer(backdrop) }
                drawRect(shadeTint)
                drawRect(glow)
            }
            .border(0.8.dp, rim, shape)
    } else {
        val fill = if (kind == AmberGlassKind.Home) AmberUi.HomeGlass else AmberUi.Surface
        val edge = if (kind == AmberGlassKind.Home) AmberUi.HomeGlassEdge else AmberUi.Border
        this
            .background(fill, shape)
            .border(1.dp, edge, shape)
    }
}

object NuxColors {
    val Yellow: Color get() = AmberUi.AccentSoft
    val YellowSoft: Color get() = if (AmberUi.isDark) lerp(AmberUi.AccentSoft, Color.White, 0.35f) else AmberUi.Accent
    val YellowPale: Color get() = AmberUi.AccentSoft.copy(alpha = if (AmberUi.isDark) 0.18f else 0.14f)
    val YellowMist: Color get() = AmberUi.AccentSoft.copy(alpha = 0.08f)

    val Ink = Color(0xFF070B1A)
    val Background: Color get() = if (AmberUi.isDark) Color(0xFF080E24) else Color(0xFFF1F5F9)
    val Panel: Color get() = if (AmberUi.isDark) Color(0xF40B1328) else Color(0xFAFFFFFF)
    val SurfaceWhite: Color get() = if (AmberUi.isDark) Color(0x1438BDF8) else Color(0xFFF8FAFC)
    val SurfaceElevated: Color get() = if (AmberUi.isDark) Color(0x2038BDF8) else Color(0xFFFFFFFF)
    val SurfaceInput: Color get() = if (AmberUi.isDark) Color(0x281E293B) else Color(0xFFE2E8F0)
    val DarkGray: Color get() = if (AmberUi.isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val GrayNeutral: Color get() = if (AmberUi.isDark) Color(0xFFB3C0D4) else Color(0xFF475569)
    val LightGray: Color get() = if (AmberUi.isDark) Color(0x2694A3B8) else Color(0x3364748B)
    val CardBorder: Color get() = if (AmberUi.isDark) Color(0x3894A3B8) else Color(0x5264748B)
    val ErrorRed: Color get() = if (AmberUi.isDark) Color(0xFFF43F5E) else Color(0xFFE11D48)
    val Amber: Color get() = if (AmberUi.isDark) Color(0xFFF59E0B) else Color(0xFFD97706)
    val AmberDark: Color get() = if (AmberUi.isDark) Color(0xFFFBBF24) else Color(0xFFB45309)
    val SkyBlueDark: Color get() = if (AmberUi.isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val PurpleDark: Color get() = if (AmberUi.isDark) Color(0xFFC084FC) else Color(0xFF7C3AED)
    val TextPrimary: Color get() = DarkGray
    val SuccessGreen: Color get() = if (AmberUi.isDark) Color(0xFF10B981) else Color(0xFF059669)

    val ForestGreen: Color get() = Yellow
    val MintGreen: Color get() = YellowSoft
    val SageGreen: Color get() = Yellow
    val SoftLime: Color get() = YellowPale
    val LightGreen: Color get() = YellowMist
    val Mint: Color get() = YellowPale
    val MintDark: Color get() = YellowSoft
    val ForestDark: Color get() = Ink
    val ForestLight: Color get() = GrayNeutral
    val ForestMuted: Color get() = GrayNeutral
    val Cream: Color get() = Background
    val Coral: Color get() = ErrorRed
    val TextMuted: Color get() = GrayNeutral
    val SkyBlue: Color get() = if (AmberUi.isDark) Color(0xFF2AA9F0) else Color(0xFF0284C7)
}

object NuxSizes {
    val BorderWidth = 2.dp
    val GlassBorder = 1.dp
    val CornerRadius = 20.dp
    val CornerRadiusLarge = 28.dp
    val CornerRadiusSmall = 14.dp
    val ShadowOffset = 3.dp
    val BorderDefault: BorderStroke get() = BorderStroke(BorderWidth, NuxColors.CardBorder)
    val ShapeDefault = RoundedCornerShape(CornerRadius)
    val ShapeLarge = RoundedCornerShape(CornerRadiusLarge)
    val ShapeSmall = RoundedCornerShape(CornerRadiusSmall)
}

// ----------------------------------------------------------------------------
// Section: ui/i18n/NovaLocaleEngine.kt
// ----------------------------------------------------------------------------

// ============================================================================
// NovaCraft Unified Localization & Translation Engine
// ============================================================================

private val LocalAmberLanguage = staticCompositionLocalOf { 0 }

/** Dipasang sekali di akar UI (MainActivity). */
@Composable
fun AmberLanguageHost(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAmberLanguage provides Tr.version.intValue) {
        content()
    }
}

/**
 * Pengganti `androidx.compose.material3.Text` yang menerjemahkan teks ke bahasa pilihan.
 */
@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalTextStyle.current
) {
    val language = LocalAmberLanguage.current
    androidx.compose.material3.Text(
        text = if (language >= 0) Tr.t(text) else text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style
    )
}

/**
 * Pengganti `android.widget.Toast` yang menerjemahkan pesan ke bahasa pilihan dan mencatat log.
 */
object Toast {
    const val LENGTH_SHORT = android.widget.Toast.LENGTH_SHORT
    const val LENGTH_LONG = android.widget.Toast.LENGTH_LONG

    private val mainHandler by lazy { android.os.Handler(android.os.Looper.getMainLooper()) }

    fun makeText(context: Context, text: CharSequence?, duration: Int): android.widget.Toast {
        if (text != null) LauncherLog.event("Toast", text.toString())
        val shown: CharSequence? = if (text == null) null else Tr.t(text.toString())
        return android.widget.Toast.makeText(context, shown, duration)
    }

    fun makeText(context: Context, resId: Int, duration: Int): android.widget.Toast {
        try {
            LauncherLog.event("Toast", context.getString(resId))
        } catch (_: Throwable) {
        }
        return android.widget.Toast.makeText(context, resId, duration)
    }

    /** Menampilkan Toast dengan aman dari thread mana pun (termasuk Dispatchers.IO). */
    fun show(context: Context, text: CharSequence?, duration: Int = LENGTH_SHORT) {
        if (text == null) return
        val appCtx = context.applicationContext
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            try {
                makeText(appCtx, text, duration).show()
            } catch (_: Throwable) {
            }
        } else {
            mainHandler.post {
                try {
                    makeText(appCtx, text, duration).show()
                } catch (_: Throwable) {
                }
            }
        }
    }
}

/**
 * Pengelola bahasa tampilan NovaCraft (ID asli + kamus .tsv di assets/lang/<kode>/).
 */
object Tr {
    const val SYSTEM = "system"
    const val ID = "id"
    const val EN = "en"
    const val ES = "es"
    const val VI = "vi"
    const val PT = "pt"
    const val TH = "th"

    const val DEFAULT_CHOICE = EN

    private val DICTIONARY_LANGS = setOf(EN, ES, VI, PT, TH)

    private const val PREFS_NAME = "amber_launcher_prefs"
    private const val KEY_LANGUAGE = "language"

    @Volatile
    private var translator: Translator? = null

    @Volatile
    var choice: String = DEFAULT_CHOICE
        private set

    @Volatile
    var effective: String = ID
        private set

    internal val version = mutableIntStateOf(0)

    fun init(context: Context) {
        val saved = try {
            context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_LANGUAGE, DEFAULT_CHOICE)
        } catch (_: Throwable) {
            DEFAULT_CHOICE
        }
        choice = normalize(saved)
        apply(context)
    }

    fun setLanguage(context: Context, newChoice: String) {
        val c = normalize(newChoice)
        if (c == choice) return
        choice = c
        try {
            context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LANGUAGE, c)
                .apply()
        } catch (_: Throwable) {
        }
        apply(context)
        version.intValue = version.intValue + 1
    }

    fun t(text: String): String {
        val tr = translator ?: return text
        return tr.translate(text)
    }

    private fun normalize(value: String?): String = when {
        value == null -> DEFAULT_CHOICE
        value == SYSTEM || value == ID || value in DICTIONARY_LANGS -> value
        else -> DEFAULT_CHOICE
    }

    private fun resolve(c: String): String {
        if (c == ID || c in DICTIONARY_LANGS) return c
        val lang = Locale.getDefault().language
        return when {
            lang == "in" || lang == "id" -> ID
            lang in DICTIONARY_LANGS -> lang
            else -> EN
        }
    }

    private fun apply(context: Context) {
        val lang = resolve(choice)
        effective = lang
        translator = if (lang == ID) null else load(context, lang)
    }

    private fun load(context: Context, lang: String): Translator? {
        return try {
            val assets = context.applicationContext.assets
            val dir = "lang/$lang"
            val names = (assets.list(dir) ?: emptyArray()).filter { it.endsWith(".tsv") }.sorted()
            if (names.isEmpty()) return null
            val lines = ArrayList<String>(1024)
            for (name in names) {
                assets.open("$dir/$name").bufferedReader(Charsets.UTF_8).use { reader ->
                    reader.forEachLine { lines.add(it) }
                }
            }
            Translator.parse(lines)
        } catch (_: Throwable) {
            null
        }
    }
}

/**
 * Mesin pencocok terjemahan exact + template berpola `{0}`, `{~0}`.
 */
class Translator private constructor(
    private val exact: Map<String, String>,
    private val templates: List<Template>
) {

    private class Template(
        private val pattern: String,
        val anchor: String,
        val parts: List<Any>,
        val nested: Set<Int>,
        val weight: Int
    ) {
        val regex: Regex? by lazy(LazyThreadSafetyMode.NONE) {
            try {
                Regex(pattern, RegexOption.DOT_MATCHES_ALL)
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun render(t: Template, match: MatchResult, depth: Int): String {
        val sb = StringBuilder()
        for (p in t.parts) {
            if (p is Int) {
                val g = match.groups[p + 1]
                if (g != null) {
                    if (depth < MAX_DEPTH && p in t.nested) sb.append(translateAt(g.value, depth + 1))
                    else sb.append(g.value)
                }
            } else {
                sb.append(p as String)
            }
        }
        return sb.toString()
    }

    private val memo = HashMap<String, String>()

    val size: Int get() = exact.size + templates.size

    fun translate(text: String): String = translateAt(text, 0)

    private fun translateAt(text: String, depth: Int): String {
        if (text.isEmpty()) return text
        val hit = exact[text]
        if (hit != null) return hit
        if (templates.isEmpty()) return text
        return translateByTemplate(text, depth)
    }

    @Synchronized
    private fun translateByTemplate(text: String, depth: Int): String {
        val cached = memo[text]
        if (cached != null) return cached
        var result = text
        for (t in templates) {
            if (t.anchor.isNotEmpty() && !text.contains(t.anchor)) continue
            val m = t.regex?.matchEntire(text) ?: continue
            result = render(t, m, depth)
            break
        }
        if (memo.size >= MEMO_MAX) memo.clear()
        memo[text] = result
        return result
    }

    companion object {
        private const val MEMO_MAX = 512
        private const val MAX_DEPTH = 2
        private val PLACEHOLDER = Regex("\\{(~?)(\\d+)\\}")
        private val VALUE_PLACEHOLDER = Regex("\\{(\\d+)\\}")

        fun parse(lines: List<String>): Translator {
            val exact = HashMap<String, String>()
            val templates = ArrayList<Template>()
            for (raw in lines) {
                if (raw.isEmpty() || raw[0] == '#') continue
                val tab = raw.indexOf('\t')
                if (tab <= 0) continue
                val key = unescape(raw.substring(0, tab))
                val value = unescape(raw.substring(tab + 1))
                if (value.isEmpty()) continue
                if (PLACEHOLDER.containsMatchIn(key)) {
                    val t = buildTemplate(key, value)
                    if (t != null) templates.add(t)
                } else {
                    exact[key] = value
                }
            }
            return Translator(exact, templates.sortedByDescending { it.weight })
        }

        private fun buildTemplate(key: String, value: String): Template? {
            val pattern = StringBuilder("^")
            var anchor = ""
            var last = 0
            var groups = 0
            var weight = 0
            val nested = HashSet<Int>()
            for (m in PLACEHOLDER.findAll(key)) {
                val literal = key.substring(last, m.range.first)
                if (literal.length > anchor.length) anchor = literal
                weight += literal.length
                pattern.append(Regex.escape(literal)).append("(.+?)")
                if (m.groupValues[1] == "~") nested.add(m.groupValues[2].toInt())
                groups++
                last = m.range.last + 1
            }
            val tail = key.substring(last)
            if (tail.length > anchor.length) anchor = tail
            weight += tail.length
            pattern.append(Regex.escape(tail)).append("$")

            val parts = ArrayList<Any>()
            var pos = 0
            for (m in VALUE_PLACEHOLDER.findAll(value)) {
                if (m.range.first > pos) parts.add(value.substring(pos, m.range.first))
                val index = m.groupValues[1].toInt()
                if (index >= groups) return null
                parts.add(index)
                pos = m.range.last + 1
            }
            if (pos < value.length) parts.add(value.substring(pos))

            return Template(pattern.toString(), anchor, parts, nested, weight)
        }

        fun unescape(s: String): String {
            if (s.indexOf('\\') < 0) return s
            val sb = StringBuilder(s.length)
            var i = 0
            while (i < s.length) {
                val c = s[i]
                if (c != '\\' || i + 1 >= s.length) {
                    sb.append(c)
                    i++
                    continue
                }
                val n = s[i + 1]
                when (n) {
                    'n' -> { sb.append('\n'); i += 2 }
                    't' -> { sb.append('\t'); i += 2 }
                    '\\' -> { sb.append('\\'); i += 2 }
                    'u' -> {
                        val hex = if (i + 6 <= s.length) s.substring(i + 2, i + 6) else ""
                        val code = hex.toIntOrNull(16)
                        if (code != null) {
                            sb.append(code.toChar())
                            i += 6
                        } else {
                            sb.append(c)
                            i++
                        }
                    }
                    else -> { sb.append(c); i++ }
                }
            }
            return sb.toString()
        }
    }
}
