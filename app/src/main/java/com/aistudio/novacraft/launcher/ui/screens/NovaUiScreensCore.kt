package com.aistudio.novacraft.launcher.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.widget.ImageView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ControlCamera
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Gamepad
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text as RawText
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.novacraft.launcher.GameActivity
import com.aistudio.novacraft.launcher.R
import com.aistudio.novacraft.launcher.core.account.AccountManager
import com.aistudio.novacraft.launcher.core.account.ElyByAuthService
import com.aistudio.novacraft.launcher.core.account.ElyByLoginState
import com.aistudio.novacraft.launcher.core.account.MicrosoftAuthService
import com.aistudio.novacraft.launcher.core.account.MicrosoftDeviceCode
import com.aistudio.novacraft.launcher.core.account.SkinUtils
import com.aistudio.novacraft.launcher.core.controls.ControlLayoutIO
import com.aistudio.novacraft.launcher.core.controls.ControlLayoutManager
import com.aistudio.novacraft.launcher.core.controls.ControlLimits
import com.aistudio.novacraft.launcher.core.controls.ControlShowMode
import com.aistudio.novacraft.launcher.core.controls.CustomControlButton
import com.aistudio.novacraft.launcher.core.controls.KeycodeCatalog
import com.aistudio.novacraft.launcher.core.utils.CrashManager
import com.aistudio.novacraft.launcher.core.launch.FabricService
import com.aistudio.novacraft.launcher.core.launch.ForgeFlavor
import com.aistudio.novacraft.launcher.core.launch.ForgeService
import com.aistudio.novacraft.launcher.core.launch.GameLauncher
import com.aistudio.novacraft.launcher.core.launch.InstanceManager
import com.aistudio.novacraft.launcher.core.launch.JavaRuntimeManager
import com.aistudio.novacraft.launcher.core.launch.MinecraftDownloader
import com.aistudio.novacraft.launcher.core.launch.MojangManifestService
import com.aistudio.novacraft.launcher.core.mods.CurseForgeApi
import com.aistudio.novacraft.launcher.core.mods.CurseForgeMapper
import com.aistudio.novacraft.launcher.core.mods.HorizonContent
import com.aistudio.novacraft.launcher.core.mods.HorizonEngine
import com.aistudio.novacraft.launcher.core.mods.HorizonMods
import com.aistudio.novacraft.launcher.core.mods.ModFavoritesViewModel
import com.aistudio.novacraft.launcher.core.mods.ModSearchHistoryEntity
import com.aistudio.novacraft.launcher.core.mods.NovaUnifiedFileImportEngine
import com.aistudio.novacraft.launcher.core.mods.NuxModManager
import com.aistudio.novacraft.launcher.core.mods.WorldInfo
import com.aistudio.novacraft.launcher.core.launch.MCNewsService
import com.aistudio.novacraft.launcher.core.launch.MobileGluesConfig
import com.aistudio.novacraft.launcher.core.launch.NuxRendererInfo
import com.aistudio.novacraft.launcher.core.launch.NuxRendererPluginManager
import com.aistudio.novacraft.launcher.core.launch.NuxRendererRegistry
import com.aistudio.novacraft.launcher.core.utils.*
import com.aistudio.novacraft.launcher.core.utils.BgImportResult
import com.aistudio.novacraft.launcher.core.utils.DeviceProfiles
import com.aistudio.novacraft.launcher.core.utils.HomeVideo
import com.aistudio.novacraft.launcher.core.utils.Instance
import com.aistudio.novacraft.launcher.core.utils.LauncherSettings
import com.aistudio.novacraft.launcher.core.utils.McVulkan
import com.aistudio.novacraft.launcher.core.utils.NativePluginManager
import com.aistudio.novacraft.launcher.core.utils.NuxVersionUtils
import com.aistudio.novacraft.launcher.core.utils.SettingsManager
import com.aistudio.novacraft.launcher.core.utils.UserAccount
import com.aistudio.novacraft.launcher.core.utils.VerdictLevel
import com.aistudio.novacraft.launcher.core.utils.VersionItem
import com.aistudio.novacraft.launcher.core.utils.VulkanCheck
import com.aistudio.novacraft.launcher.core.utils.VulkanReport
import com.aistudio.novacraft.launcher.core.utils.VulkanState
import com.aistudio.novacraft.launcher.ui.components.*
import com.aistudio.novacraft.launcher.ui.components.AmberButton
import com.aistudio.novacraft.launcher.ui.components.AmberButtonStyle
import com.aistudio.novacraft.launcher.ui.components.AmberSpinner
import com.aistudio.novacraft.launcher.ui.components.AmberWaitBar
import com.aistudio.novacraft.launcher.ui.components.AmbientGlow
import com.aistudio.novacraft.launcher.ui.components.AmbientSampler
import com.aistudio.novacraft.launcher.ui.components.AmbientState
import com.aistudio.novacraft.launcher.ui.components.GamepadWindowHook
import com.aistudio.novacraft.launcher.ui.components.HomeInfo
import com.aistudio.novacraft.launcher.ui.components.HomeVideoLayer
import com.aistudio.novacraft.launcher.ui.components.NUX_NAV_ITEMS
import com.aistudio.novacraft.launcher.ui.components.NuxButton
import com.aistudio.novacraft.launcher.ui.components.NuxCard
import com.aistudio.novacraft.launcher.ui.components.NuxDialog
import com.aistudio.novacraft.launcher.ui.components.rememberIsResumed
import com.aistudio.novacraft.launcher.ui.components.rememberPowerSaveMode
import com.aistudio.novacraft.launcher.ui.components.drawControlButton
import com.aistudio.novacraft.launcher.ui.components.drawControlJoystick
import com.aistudio.novacraft.launcher.ui.components.drawControlScroll
import com.aistudio.novacraft.launcher.ui.components.skinSpecFor
import com.aistudio.novacraft.launcher.ui.dialogs.NuxAboutDialog
import com.aistudio.novacraft.launcher.ui.dialogs.NuxAddonImportDialog
import com.aistudio.novacraft.launcher.ui.dialogs.NuxCrashDialog
import com.aistudio.novacraft.launcher.ui.dialogs.NuxDeleteInstanceDialog
import com.aistudio.novacraft.launcher.ui.dialogs.NuxDownloadProgressDialog
import com.aistudio.novacraft.launcher.ui.dialogs.NuxEditInstanceDialog
import com.aistudio.novacraft.launcher.ui.dialogs.NuxImportLayoutDialog
import com.aistudio.novacraft.launcher.ui.dialogs.NuxInstancePickerDialog
import com.aistudio.novacraft.launcher.ui.dialogs.NuxKeyPickerDialog
import com.aistudio.novacraft.launcher.ui.dialogs.NuxMicrosoftAuthDialog
import com.aistudio.novacraft.launcher.ui.dialogs.NuxRendererV2ConfigDialog
import com.aistudio.novacraft.launcher.ui.dialogs.NuxRendererWarningDialog
import com.aistudio.novacraft.launcher.ui.dialogs.NuxWardrobeDialog
import com.aistudio.novacraft.launcher.ui.theme.AMBER_ACCENTS
import com.aistudio.novacraft.launcher.ui.theme.Amber
import com.aistudio.novacraft.launcher.ui.theme.AmberColorSource
import com.aistudio.novacraft.launcher.ui.theme.AmberGlassKind
import com.aistudio.novacraft.launcher.ui.theme.AmberStyle
import com.aistudio.novacraft.launcher.ui.theme.AmberThemeMode
import com.aistudio.novacraft.launcher.ui.theme.AmberUi
import com.aistudio.novacraft.launcher.ui.theme.LocalGlassBackdrop
import com.aistudio.novacraft.launcher.ui.theme.NuxColors
import com.aistudio.novacraft.launcher.ui.theme.NuxReadable
import com.aistudio.novacraft.launcher.ui.theme.NuxSizes
import com.aistudio.novacraft.launcher.ui.theme.Text
import com.aistudio.novacraft.launcher.ui.theme.Toast
import com.aistudio.novacraft.launcher.ui.theme.Tr
import com.aistudio.novacraft.launcher.ui.theme.amberGlass
import com.aistudio.novacraft.launcher.ui.theme.amberScrimBrush
import com.aistudio.novacraft.launcher.ui.theme.rememberCameraSideInsets
import com.movtery.inputmap.keycodes.LwjglGlfwKeycode
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

// ----------------------------------------------------------------------------
// Section: ui/screens/AccountsScreen.kt
// ----------------------------------------------------------------------------

private val skinPreviewHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()
}

/**
 * Layar Akun & Portal Login Baru NovaCraft:
 * - Bar atas: Navigasi, saklar cepat Mode Online / Offline, dan tombol pintasan Login Baru.
 * - Saat kosong: langsung menampilkan Portal Login Terpadu (Offline / Microsoft / Ely.by).
 * - Saat ada profil: tata letak asimetris 2-zona (Kiri: Sorotan Identitas & Pratinjau 3D;
 *   Kanan: Ruang Kerja Ganda Profil Tersimpan + Status Sesi Lokal / Form Login Baru).
 */
@Composable
fun AccountsScreen(
    onNavigateBack: () -> Unit,
    onOpenMods: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val accounts by AccountManager.accounts.collectAsState()
    val currentAccount by AccountManager.currentAccount.collectAsState()
    val settings by SettingsManager.settings.collectAsState()

    // 0 = Profil Tersimpan, 1 = Login Baru
    var workspaceTab by remember { mutableIntStateOf(0) }
    var accountToDelete by remember { mutableStateOf<UserAccount?>(null) }
    var accountForWardrobe by remember { mutableStateOf<UserAccount?>(null) }
    var refreshing by remember { mutableStateOf(false) }

    LaunchedEffect(accounts.isEmpty()) {
        if (accounts.isEmpty()) workspaceTab = 1
    }

    BackHandler(enabled = accounts.isNotEmpty() && workspaceTab == 1) {
        workspaceTab = 0
    }

    val openWardrobe: (UserAccount) -> Unit = { acc ->
        if (acc.safeAccountType == "microsoft") {
            Toast.makeText(
                context,
                "Skin & jubah akun Microsoft diatur di minecraft.net",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            accountForWardrobe = acc
        }
    }

    val toggleOfflineMode: () -> Unit = {
        val nextOffline = !settings.offlineMode
        SettingsManager.updateSettings(context, settings.copy(offlineMode = nextOffline))
        Toast.makeText(
            context,
            if (nextOffline) {
                "Mode Offline aktif: sesi lokal dipakai saat bermain"
            } else {
                "Mode Online aktif: verifikasi akun penuh dipakai"
            },
            Toast.LENGTH_SHORT
        ).show()
    }

    val refreshMicrosoftAccount: (UserAccount) -> Unit = { acc ->
        if (acc.safeAccountType == "microsoft" && !refreshing) {
            coroutineScope.launch {
                refreshing = true
                MicrosoftAuthService.refreshMicrosoftAccount(acc)
                    .onSuccess { fresh ->
                        AccountManager.updateAccount(context, fresh)
                        Toast.makeText(context, "Sesi akun berhasil disegarkan", Toast.LENGTH_SHORT).show()
                    }
                    .onFailure { e ->
                        Toast.makeText(
                            context,
                            e.message ?: "Gagal menyegarkan sesi akun",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                refreshing = false
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 10.dp)
    ) {
        val isNarrow = maxWidth < 640.dp
        val isUltraNarrow = maxWidth < 500.dp

        Column(modifier = Modifier.fillMaxSize()) {
            // ── TOP COMMAND HEADER ──
            if (isUltraNarrow) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        AmberNavPill(
                            active = "accounts",
                            activeLabel = "Akun",
                            onBack = onNavigateBack,
                            onMods = onOpenMods,
                            onAccounts = {},
                            onSettings = onOpenSettings
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ConnectionModePill(
                            offlineMode = settings.offlineMode,
                            onClick = toggleOfflineMode,
                            modifier = Modifier.weight(1f)
                        )
                        if (accounts.isNotEmpty()) {
                            AmberPillButton(
                                text = if (workspaceTab == 1) "Lihat Profil" else "Login Baru",
                                icon = if (workspaceTab == 1) Icons.Outlined.People else Icons.Outlined.PersonAdd,
                                filled = workspaceTab != 1,
                                onClick = { workspaceTab = if (workspaceTab == 1) 0 else 1 },
                                modifier = Modifier.testTag("accounts_add_button")
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AmberNavPill(
                        active = "accounts",
                        activeLabel = "Akun",
                        onBack = onNavigateBack,
                        onMods = onOpenMods,
                        onAccounts = {},
                        onSettings = onOpenSettings
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    ConnectionModePill(
                        offlineMode = settings.offlineMode,
                        onClick = toggleOfflineMode
                    )
                    if (accounts.isNotEmpty()) {
                        AmberPillButton(
                            text = if (workspaceTab == 1) "Lihat Profil" else "Login Baru",
                            icon = if (workspaceTab == 1) Icons.Outlined.People else Icons.Outlined.PersonAdd,
                            filled = workspaceTab != 1,
                            onClick = { workspaceTab = if (workspaceTab == 1) 0 else 1 },
                            modifier = Modifier.testTag("accounts_add_button")
                        )
                    }
                }
            }

            // ── MAIN WORKSPACE ──
            if (accounts.isEmpty()) {
                IntegratedOnboardingPortal(
                    offlineMode = settings.offlineMode,
                    persistSession = settings.persistAccountSession,
                    onTogglePersist = { enabled ->
                        SettingsManager.updateSettings(context, settings.copy(persistAccountSession = enabled))
                        AccountManager.setSessionPersistence(context, enabled)
                    },
                    onAccountLoggedIn = { newAcc, alreadyStored ->
                        if (!alreadyStored) {
                            AccountManager.addAccount(context, newAcc)
                        }
                        Toast.makeText(context, "Selamat datang, ${newAcc.username}!", Toast.LENGTH_SHORT).show()
                        workspaceTab = 0
                    },
                    isNarrow = isNarrow,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            } else {
                val activeAccount = currentAccount ?: accounts.first()
                if (isNarrow) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PlayerSpotlightCard(
                            account = activeAccount,
                            offlineMode = settings.offlineMode,
                            persistSession = settings.persistAccountSession,
                            refreshing = refreshing,
                            onWardrobe = { openWardrobe(activeAccount) },
                            onRefresh = { refreshMicrosoftAccount(activeAccount) },
                            onDelete = { accountToDelete = activeAccount },
                            scrollableStage = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                        AccountWorkspaceCard(
                            accounts = accounts,
                            currentAccountId = activeAccount.id,
                            workspaceTab = workspaceTab,
                            onSelectTab = { workspaceTab = it },
                            persistSession = settings.persistAccountSession,
                            onTogglePersist = { enabled ->
                                SettingsManager.updateSettings(context, settings.copy(persistAccountSession = enabled))
                                AccountManager.setSessionPersistence(context, enabled)
                            },
                            onSelectAccount = { picked ->
                                AccountManager.selectAccount(context, picked)
                                Toast.makeText(context, "Akun aktif: ${picked.username}", Toast.LENGTH_SHORT).show()
                            },
                            onWardrobeAccount = openWardrobe,
                            onDeleteAccount = { accountToDelete = it },
                            onAccountLoggedIn = { newAcc, alreadyStored ->
                                if (!alreadyStored) {
                                    AccountManager.addAccount(context, newAcc)
                                }
                                Toast.makeText(context, "Akun ${newAcc.username} berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                                workspaceTab = 0
                            },
                            scrollableList = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PlayerSpotlightCard(
                            account = activeAccount,
                            offlineMode = settings.offlineMode,
                            persistSession = settings.persistAccountSession,
                            refreshing = refreshing,
                            onWardrobe = { openWardrobe(activeAccount) },
                            onRefresh = { refreshMicrosoftAccount(activeAccount) },
                            onDelete = { accountToDelete = activeAccount },
                            scrollableStage = true,
                            modifier = Modifier
                                .weight(1.05f)
                                .fillMaxHeight()
                        )
                        AccountWorkspaceCard(
                            accounts = accounts,
                            currentAccountId = activeAccount.id,
                            workspaceTab = workspaceTab,
                            onSelectTab = { workspaceTab = it },
                            persistSession = settings.persistAccountSession,
                            onTogglePersist = { enabled ->
                                SettingsManager.updateSettings(context, settings.copy(persistAccountSession = enabled))
                                AccountManager.setSessionPersistence(context, enabled)
                            },
                            onSelectAccount = { picked ->
                                AccountManager.selectAccount(context, picked)
                                Toast.makeText(context, "Akun aktif: ${picked.username}", Toast.LENGTH_SHORT).show()
                            },
                            onWardrobeAccount = openWardrobe,
                            onDeleteAccount = { accountToDelete = it },
                            onAccountLoggedIn = { newAcc, alreadyStored ->
                                if (!alreadyStored) {
                                    AccountManager.addAccount(context, newAcc)
                                }
                                Toast.makeText(context, "Akun ${newAcc.username} berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                                workspaceTab = 0
                            },
                            scrollableList = true,
                            modifier = Modifier
                                .weight(1.25f)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }
    }

    accountForWardrobe?.let { wardrobeAccount ->
        NuxWardrobeDialog(
            account = wardrobeAccount,
            onDismissRequest = { accountForWardrobe = null },
            onSaved = { accountForWardrobe = null }
        )
    }

    accountToDelete?.let { deleteTarget ->
        DeleteAccountDialog(
            target = deleteTarget,
            onDismiss = { accountToDelete = null }
        )
    }
}

/** Pill saklar cepat Mode Online / Mode Offline di bilah atas layar Akun. */
@Composable
private fun ConnectionModePill(
    offlineMode: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = if (offlineMode) NuxColors.Yellow else AmberUi.ModrinthGreen
    val shape = CircleShape
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(shape)
            .background(accent.copy(alpha = if (AmberUi.isDark) 0.14f else 0.12f), shape)
            .border(1.dp, accent.copy(alpha = 0.55f), shape)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("accounts_mode_toggle_pill"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Icon(
            imageVector = if (offlineMode) Icons.Outlined.WifiOff else Icons.Outlined.Wifi,
            contentDescription = if (offlineMode) "Mode Offline" else "Mode Online",
            tint = accent,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = if (offlineMode) "Mode Offline" else "Mode Online",
            color = AmberUi.Text,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Portal login terintegrasi saat belum ada akun tersimpan:
 * Kiri = informasi sesi & mode koneksi; Kanan = composer login 3 penyedia siap pakai.
 */
@Composable
private fun IntegratedOnboardingPortal(
    offlineMode: Boolean,
    persistSession: Boolean,
    onTogglePersist: (Boolean) -> Unit,
    onAccountLoggedIn: (UserAccount, Boolean) -> Unit,
    isNarrow: Boolean,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(22.dp)
    if (isNarrow) {
        Column(
            modifier = modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OnboardingInfoPanel(
                offlineMode = offlineMode,
                persistSession = persistSession,
                onTogglePersist = onTogglePersist,
                modifier = Modifier.fillMaxWidth()
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(AmberUi.Surface, cardShape)
                    .border(1.dp, AmberUi.Border, cardShape)
                    .padding(16.dp)
            ) {
                UnifiedLoginComposer(
                    onAccountLoggedIn = onAccountLoggedIn
                )
            }
        }
    } else {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OnboardingInfoPanel(
                offlineMode = offlineMode,
                persistSession = persistSession,
                onTogglePersist = onTogglePersist,
                modifier = Modifier
                    .weight(0.95f)
                    .fillMaxHeight()
            )
            Column(
                modifier = Modifier
                    .weight(1.35f)
                    .fillMaxHeight()
                    .clip(cardShape)
                    .background(AmberUi.Surface, cardShape)
                    .border(1.dp, AmberUi.Border, cardShape)
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
            ) {
                UnifiedLoginComposer(
                    onAccountLoggedIn = onAccountLoggedIn
                )
            }
        }
    }
}

@Composable
private fun OnboardingInfoPanel(
    offlineMode: Boolean,
    persistSession: Boolean,
    onTogglePersist: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(22.dp)
    val dark = AmberUi.isDark
    val heroBrush = remember(dark) {
        if (dark) {
            Brush.verticalGradient(listOf(Color(0xFF132145), Color(0xFF0A1124)))
        } else {
            Brush.verticalGradient(listOf(Color(0xFFE0ECFF), Color(0xFFF8FAFC)))
        }
    }
    Column(
        modifier = modifier
            .clip(cardShape)
            .background(heroBrush, cardShape)
            .border(1.dp, AmberUi.BorderStrong, cardShape)
            .padding(18.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(AmberUi.Accent.copy(alpha = 0.18f))
                    .border(1.dp, AmberUi.AccentEdge.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = null,
                    tint = AmberUi.AccentSoft,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = "Portal Masuk Minecraft",
                color = AmberUi.Text,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.3).sp
            )
            Text(
                text = "Masuk menggunakan akun Microsoft resmi, akun Ely.by untuk skin kustom, atau profil Offline lokal untuk bermain cepat.",
                color = AmberUi.TextDim,
                fontSize = 12.5.sp,
                lineHeight = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(AmberUi.SurfaceRaised)
                    .border(1.dp, AmberUi.Border, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Persistensi Sesi Lokal",
                        color = AmberUi.Text,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (persistSession) "Akun tetap tersimpan otomatis" else "Sesi hanya aktif sementara",
                        color = AmberUi.TextDim,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                AmberToggle(
                    checked = persistSession,
                    onChange = onTogglePersist
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(AmberUi.SurfaceRaised)
                    .border(1.dp, AmberUi.Border, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (offlineMode) Icons.Outlined.WifiOff else Icons.Outlined.Wifi,
                    contentDescription = null,
                    tint = if (offlineMode) NuxColors.Yellow else AmberUi.ModrinthGreen,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (offlineMode) {
                        "Mode Offline: game diluncurkan memakai sesi lokal"
                    } else {
                        "Mode Online: menggunakan sesi terverifikasi asli"
                    },
                    color = AmberUi.TextDim,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * ZONA KIRI/ATAS: Kartu Sorotan Identitas Pemain Aktif.
 * Menggantikan tata letak kartu telemetri 2x2 lama dengan sorotan vertikal terpadu:
 * Header identitas + Stage 3D adaptif tema + Strip info UUID/Sesi + Bar aksi cepat.
 */
@Composable
private fun PlayerSpotlightCard(
    account: UserAccount,
    offlineMode: Boolean,
    persistSession: Boolean,
    refreshing: Boolean,
    onWardrobe: () -> Unit,
    onRefresh: () -> Unit,
    onDelete: () -> Unit,
    scrollableStage: Boolean,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val type = account.safeAccountType
    val accent = acctAccent(type)
    val dark = AmberUi.isDark
    val cardShape = RoundedCornerShape(22.dp)
    val stageShape = RoundedCornerShape(16.dp)
    val stageBrush = remember(dark) {
        if (dark) {
            Brush.radialGradient(listOf(Color(0xFF142146), Color(0xFF070D1D)))
        } else {
            Brush.radialGradient(listOf(Color(0xFFEFF6FF), Color(0xFFDCE7F5)))
        }
    }

    Column(
        modifier = modifier
            .clip(cardShape)
            .background(AmberUi.Surface, cardShape)
            .border(1.dp, AmberUi.BorderStrong, cardShape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Header Identitas Pemain
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(AmberUi.SurfaceRaised)
                    .border(1.5.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                NuxAccountAvatar(
                    account = account,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.username,
                    color = AmberUi.Text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AccountTypePill(type)
                    if (offlineMode && type != "offline") {
                        SpotlightMiniBadge(
                            text = "Sesi Offline",
                            color = NuxColors.Yellow
                        )
                    } else if (type == "microsoft") {
                        SpotlightMiniBadge(
                            text = "Terverifikasi",
                            color = AmberUi.ModrinthGreen
                        )
                    }
                }
            }
        }

        // 2. Panggung Pratinjau Skin 3D Adaptif
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (scrollableStage) Modifier.weight(1f) else Modifier.height(210.dp))
                .clip(stageShape)
                .background(stageBrush, stageShape)
                .border(1.dp, AmberUi.Border, stageShape)
        ) {
            AccountSkinPreview(
                account = account,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 12.dp, bottom = 26.dp)
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
                    .clip(CircleShape)
                    .background(AmberUi.Surface.copy(alpha = 0.85f), CircleShape)
                    .border(1.dp, AmberUi.Border, CircleShape)
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                val modelText = if (account.safeSkinModel == "slim") "Alex · Slim" else "Steve · Classic"
                Text(
                    text = "Geser 360° · $modelText",
                    color = AmberUi.TextDim,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // 3. Strip Info Ringkas (UUID & Sesi Lokal)
        val shortUuid = remember(account.uuid) {
            if (account.uuid.length >= 12) "${account.uuid.take(8)}…${account.uuid.takeLast(4)}" else account.uuid
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 40.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(AmberUi.SurfaceRaised)
                    .border(1.dp, AmberUi.Border, RoundedCornerShape(11.dp))
                    .clickable {
                        clipboard.setText(AnnotatedString(account.uuid))
                        Toast.makeText(context, "UUID disalin", Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Fingerprint,
                    contentDescription = "Salin UUID",
                    tint = AmberUi.TextDim,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = shortUuid,
                    color = AmberUi.Text,
                    fontSize = 11.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier
                    .defaultMinSize(minHeight = 40.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(AmberUi.SurfaceRaised)
                    .border(1.dp, AmberUi.Border, RoundedCornerShape(11.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = if (persistSession) Icons.Outlined.Save else Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = if (persistSession) AmberUi.ModrinthGreen else AmberUi.TextDim,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = if (persistSession) "Tersimpan" else "Sementara",
                    color = AmberUi.Text,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }

        // 4. Bar Aksi Cepat (Semua tombol minHeight = 48.dp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SpotlightActionButton(
                label = "Skin & Jubah",
                icon = Icons.Outlined.Checkroom,
                onClick = onWardrobe,
                accent = AmberUi.AccentSoft,
                modifier = Modifier
                    .weight(1f)
                    .testTag("account_wardrobe_button")
            )
            if (type == "microsoft") {
                SpotlightActionButton(
                    label = if (refreshing) "Menyegarkan" else "Segarkan",
                    icon = Icons.Outlined.Refresh,
                    onClick = onRefresh,
                    busy = refreshing,
                    accent = NuxColors.SkyBlueDark,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("account_refresh_button")
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AmberUi.Danger.copy(alpha = 0.14f))
                    .border(1.dp, AmberUi.Danger.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .clickable { onDelete() }
                    .testTag("account_delete_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Hapus akun",
                    tint = AmberUi.Danger,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

@Composable
private fun SpotlightMiniBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 1.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun SpotlightActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
    busy: Boolean = false
) {
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AmberUi.SurfaceRaised)
            .border(1.dp, AmberUi.Border, RoundedCornerShape(12.dp))
            .clickable(enabled = !busy) { onClick() }
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (busy) {
            AmberSpinner(
                modifier = Modifier.size(15.dp),
                color = accent,
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(17.dp)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = AmberUi.Text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * ZONA KANAN/BAWAH: Kartu Ruang Kerja Ganda.
 * Tab 0 = Daftar Profil Tersimpan + Status Persistensi Sesi Lokal.
 * Tab 1 = Unified Login Composer (Offline / Microsoft / Ely.by) tertanam langsung tanpa modal tambahan.
 */
@Composable
private fun AccountWorkspaceCard(
    accounts: List<UserAccount>,
    currentAccountId: String,
    workspaceTab: Int,
    onSelectTab: (Int) -> Unit,
    persistSession: Boolean,
    onTogglePersist: (Boolean) -> Unit,
    onSelectAccount: (UserAccount) -> Unit,
    onWardrobeAccount: (UserAccount) -> Unit,
    onDeleteAccount: (UserAccount) -> Unit,
    onAccountLoggedIn: (UserAccount, Boolean) -> Unit,
    scrollableList: Boolean,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(22.dp)

    Column(
        modifier = modifier
            .clip(cardShape)
            .background(AmberUi.Surface, cardShape)
            .border(1.dp, AmberUi.Border, cardShape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Segmented Tabs (48dp touch height)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(AmberUi.SurfaceRaised)
                .border(1.dp, AmberUi.Border, RoundedCornerShape(14.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            WorkspaceTabChip(
                label = "Profil (${accounts.size})",
                icon = Icons.Outlined.Group,
                selected = workspaceTab == 0,
                onClick = { onSelectTab(0) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("accounts_tab_saved")
            )
            WorkspaceTabChip(
                label = "Login Baru",
                icon = Icons.Outlined.Login,
                selected = workspaceTab == 1,
                onClick = { onSelectTab(1) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("accounts_tab_login")
            )
        }

        if (workspaceTab == 0) {
            // Banner Persistensi Sesi Lokal
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(AmberUi.SurfaceRaised)
                    .border(1.dp, AmberUi.Border, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            (if (persistSession) AmberUi.ModrinthGreen else AmberUi.TextDim).copy(alpha = 0.14f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (persistSession) Icons.Outlined.VerifiedUser else Icons.Outlined.NoEncryptionGmailerrorred,
                        contentDescription = null,
                        tint = if (persistSession) AmberUi.ModrinthGreen else AmberUi.TextDim,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Simpan Sesi Akun Lokal",
                        color = AmberUi.Text,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (persistSession) {
                            "Akun aktif otomatis dipulihkan saat launcher dibuka lagi."
                        } else {
                            "Akun aktif tidak diingat setelah launcher ditutup."
                        },
                        color = AmberUi.TextDim,
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    )
                }
                Box(modifier = Modifier.testTag("accounts_session_persist_switch")) {
                    AmberToggle(
                        checked = persistSession,
                        onChange = onTogglePersist
                    )
                }
            }

            if (scrollableList) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 4.dp)
                ) {
                    items(accounts, key = { it.id }) { acc ->
                        SavedAccountRow(
                            account = acc,
                            isCurrent = acc.id == currentAccountId,
                            onSelect = { onSelectAccount(acc) },
                            onWardrobe = { onWardrobeAccount(acc) },
                            onDelete = { onDeleteAccount(acc) }
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    accounts.forEach { acc ->
                        SavedAccountRow(
                            account = acc,
                            isCurrent = acc.id == currentAccountId,
                            onSelect = { onSelectAccount(acc) },
                            onWardrobe = { onWardrobeAccount(acc) },
                            onDelete = { onDeleteAccount(acc) }
                        )
                    }
                }
            }
        } else {
            val loginContentMod = if (scrollableList) {
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            } else {
                Modifier.fillMaxWidth()
            }
            Column(modifier = loginContentMod) {
                UnifiedLoginComposer(
                    onAccountLoggedIn = onAccountLoggedIn
                )
            }
        }
    }
}

@Composable
private fun WorkspaceTabChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp)
            .clip(shape)
            .background(if (selected) NuxColors.Yellow else Color.Transparent, shape)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) NuxColors.Ink else AmberUi.TextDim,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = if (selected) NuxColors.Ink else AmberUi.Text,
            fontSize = 12.5.sp,
            fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SavedAccountRow(
    account: UserAccount,
    isCurrent: Boolean,
    onSelect: () -> Unit,
    onWardrobe: () -> Unit,
    onDelete: () -> Unit
) {
    val type = account.safeAccountType
    val accent = acctAccent(type)
    val shape = RoundedCornerShape(15.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 62.dp)
            .clip(shape)
            .background(
                if (isCurrent) accent.copy(alpha = if (AmberUi.isDark) 0.14f else 0.10f) else AmberUi.SurfaceRaised,
                shape
            )
            .border(
                if (isCurrent) 1.5.dp else 1.dp,
                if (isCurrent) accent.copy(alpha = 0.8f) else AmberUi.Border,
                shape
            )
            .clickable { onSelect() }
            .padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(AmberUi.Surface)
                .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            NuxAccountAvatar(
                account = account,
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = account.username,
                    color = AmberUi.Text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isCurrent) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(NuxColors.Yellow)
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "AKTIF",
                            color = NuxColors.Ink,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(3.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AccountTypePill(type)
                Text(
                    text = acctTypeDesc(type),
                    color = AmberUi.TextDim,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (type != "microsoft") {
            AccountIconAction(
                icon = Icons.Outlined.Checkroom,
                description = "Skin & Jubah ${account.username}",
                onClick = onWardrobe,
                tint = AmberUi.TextDim
            )
        }
        AccountIconAction(
            icon = Icons.Outlined.Delete,
            description = "Hapus ${account.username}",
            onClick = onDelete,
            tint = AmberUi.Danger.copy(alpha = 0.85f)
        )
    }
}

/**
 * Mesin Login Terpadu yang dipakai langsung di Workspace Akun, Onboarding Kosong, dan AddAccountModal.
 * Mendukung Offline, Microsoft Device Code Browser, dan Ely.by Browser Flow.
 */
@Composable
private fun UnifiedLoginComposer(
    onAccountLoggedIn: (UserAccount, Boolean) -> Unit,
    initialTab: String = "offline",
    verticalProviderRail: Boolean = false
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(initialTab) }
    var offlineName by remember { mutableStateOf("") }

    var msDeviceCode by remember { mutableStateOf<MicrosoftDeviceCode?>(null) }
    var msErrorMsg by remember { mutableStateOf<String?>(null) }
    var isMsLoading by remember { mutableStateOf(false) }

    val elyLoginState by ElyByAuthService.loginState.collectAsState()

    LaunchedEffect(elyLoginState) {
        val state = elyLoginState
        if (state is ElyByLoginState.Success) {
            ElyByAuthService.resetState()
            onAccountLoggedIn(state.account, true)
        }
    }

    val providerBar: @Composable () -> Unit = {
        if (verticalProviderRail) {
            Column(
                modifier = Modifier.width(156.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProviderOptionTile(
                    title = "Offline",
                    subtitle = "Lokal tanpa akun",
                    icon = Icons.Outlined.Person,
                    accent = NuxColors.Yellow,
                    selected = selectedTab == "offline",
                    onClick = { selectedTab = "offline" },
                    modifier = Modifier.testTag("provider_offline")
                )
                ProviderOptionTile(
                    title = "Microsoft",
                    subtitle = "Akun resmi Mojang",
                    icon = Icons.Outlined.Language,
                    accent = NuxColors.SkyBlueDark,
                    selected = selectedTab == "microsoft",
                    onClick = { selectedTab = "microsoft" },
                    modifier = Modifier.testTag("provider_microsoft")
                )
                ProviderOptionTile(
                    title = "Ely.by",
                    subtitle = "Skin & jubah gratis",
                    icon = Icons.Outlined.Public,
                    accent = NuxColors.PurpleDark,
                    selected = selectedTab == "elyby",
                    onClick = { selectedTab = "elyby" },
                    modifier = Modifier.testTag("provider_elyby")
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProviderOptionTile(
                    title = "Offline",
                    subtitle = "Lokal",
                    icon = Icons.Outlined.Person,
                    accent = NuxColors.Yellow,
                    selected = selectedTab == "offline",
                    onClick = { selectedTab = "offline" },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("provider_offline")
                )
                ProviderOptionTile(
                    title = "Microsoft",
                    subtitle = "Resmi",
                    icon = Icons.Outlined.Language,
                    accent = NuxColors.SkyBlueDark,
                    selected = selectedTab == "microsoft",
                    onClick = { selectedTab = "microsoft" },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("provider_microsoft")
                )
                ProviderOptionTile(
                    title = "Ely.by",
                    subtitle = "Skin & Cape",
                    icon = Icons.Outlined.Public,
                    accent = NuxColors.PurpleDark,
                    selected = selectedTab == "elyby",
                    onClick = { selectedTab = "elyby" },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("provider_elyby")
                )
            }
        }
    }

    val formBody: @Composable (Modifier) -> Unit = { bodyModifier ->
        Column(
            modifier = bodyModifier,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedTab) {
                "offline" -> {
                    val trimmed = offlineName.trim()
                    val validLength = trimmed.length in 1..16 && !trimmed.contains(' ')
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Masuk Profil Offline",
                            color = AmberUi.Text,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Buat profil lokal instan untuk bermain singleplayer, LAN, atau server offline-mode.",
                            color = AmberUi.TextDim,
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(AmberUi.SurfaceRaised)
                                .border(1.dp, AmberUi.Border, RoundedCornerShape(13.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (trimmed.isNotBlank()) {
                                NuxNetworkImage(
                                    model = "https://mc-heads.net/avatar/$trimmed/80",
                                    contentDescription = trimmed,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(9.dp)),
                                    fallbackInitials = trimmed.take(2).uppercase(),
                                    shape = RoundedCornerShape(9.dp),
                                    maxSidePx = 128
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = null,
                                    tint = AmberUi.TextDim,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            NuxTextField(
                                value = offlineName,
                                onValueChange = { offlineName = it },
                                placeholder = "Username pemain (maks. 16 huruf)"
                            )
                        }
                    }

                    if (trimmed.isNotEmpty() && !validLength) {
                        Text(
                            text = "Tips: Gunakan 1–16 karakter tanpa spasi agar kompatibel dengan semua mesin game.",
                            color = NuxColors.Yellow,
                            fontSize = 11.sp
                        )
                    }

                    NuxButton(
                        onClick = {
                            if (trimmed.isBlank()) {
                                Toast.makeText(context, "Username tidak boleh kosong", Toast.LENGTH_SHORT).show()
                                return@NuxButton
                            }
                            val newAcc = AccountManager.createGuestAccount(trimmed)
                            offlineName = ""
                            onAccountLoggedIn(newAcc, false)
                        },
                        backgroundColor = NuxColors.Yellow,
                        enabled = trimmed.isNotBlank(),
                        cornerRadius = 16.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("login_offline_submit")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simpan & Masuk Offline", fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }
                }

                "microsoft" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Masuk dengan Microsoft / Xbox",
                            color = NuxColors.SkyBlueDark,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Otorisasi resmi lewat browser menggunakan kode perangkat sekali pakai. Aman tanpa menyimpan kata sandi.",
                            color = AmberUi.TextDim,
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp
                        )
                    }

                    msErrorMsg?.let { errText ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AmberUi.Danger.copy(alpha = 0.12f))
                                .border(1.dp, AmberUi.Danger.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = errText,
                                color = AmberUi.Danger,
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    NuxButton(
                        onClick = {
                            isMsLoading = true
                            msErrorMsg = null
                            scope.launch {
                                val res = MicrosoftAuthService.startDeviceCodeFlow()
                                isMsLoading = false
                                res.fold(
                                    onSuccess = { dc -> msDeviceCode = dc },
                                    onFailure = { err ->
                                        msErrorMsg = err.message ?: "Gagal memulai otorisasi Microsoft."
                                    }
                                )
                            }
                        },
                        backgroundColor = NuxColors.Yellow,
                        enabled = !isMsLoading,
                        cornerRadius = 16.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("login_microsoft_submit")
                    ) {
                        if (isMsLoading) {
                            AmberSpinner(
                                modifier = Modifier.size(16.dp),
                                color = NuxColors.Ink,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Menghubungi Microsoft...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Language,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Masuk dengan Microsoft", fontWeight = FontWeight.Black, fontSize = 13.sp)
                        }
                    }
                }

                "elyby" -> {
                    when (val state = elyLoginState) {
                        is ElyByLoginState.WaitingForApproval -> {
                            val dc = state.deviceCode
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                AmberSpinner(
                                    modifier = Modifier.size(16.dp),
                                    color = NuxColors.PurpleDark,
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = state.statusMsg.ifBlank { "Menunggu konfirmasi di browser Ely.by..." },
                                    color = AmberUi.Text,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(AmberUi.SurfaceRaised)
                                        .border(1.dp, NuxColors.PurpleDark.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = dc.userCode,
                                        color = NuxColors.PurpleDark,
                                        fontSize = 17.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    )
                                }
                                NuxButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(dc.userCode))
                                        Toast.makeText(context, "Kode disalin", Toast.LENGTH_SHORT).show()
                                    },
                                    backgroundColor = AmberUi.SurfaceRaised,
                                    contentColor = AmberUi.Text,
                                    cornerRadius = 12.dp,
                                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ContentCopy,
                                        contentDescription = "Salin kode",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Salin", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                NuxButton(
                                    onClick = {
                                        ElyByAuthService.openBrowser(context, dc.verificationUrlWithCode)
                                    },
                                    backgroundColor = NuxColors.Yellow,
                                    cornerRadius = 14.dp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 48.dp)
                                ) {
                                    Text("Buka Browser", fontWeight = FontWeight.Black, fontSize = 12.5.sp)
                                }
                                NuxButton(
                                    onClick = { ElyByAuthService.cancelLogin() },
                                    backgroundColor = AmberUi.SurfaceRaised,
                                    contentColor = AmberUi.Text,
                                    cornerRadius = 14.dp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 48.dp)
                                ) {
                                    Text("Batal", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                }
                            }
                        }

                        else -> {
                            val isStarting = state is ElyByLoginState.Starting
                            val errMsg = (state as? ElyByLoginState.Error)?.message

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Masuk dengan Ely.by",
                                    color = NuxColors.PurpleDark,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "Browser akan dibuka untuk konfirmasi akun Ely.by sekaligus sinkronisasi skin dan jubah kustom.",
                                    color = AmberUi.TextDim,
                                    fontSize = 11.5.sp,
                                    lineHeight = 16.sp
                                )
                            }

                            if (errMsg != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(AmberUi.Danger.copy(alpha = 0.12f))
                                        .border(1.dp, AmberUi.Danger.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = errMsg,
                                        color = AmberUi.Danger,
                                        fontSize = 11.5.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }

                            NuxButton(
                                onClick = { ElyByAuthService.startLogin(context) },
                                backgroundColor = NuxColors.Yellow,
                                enabled = !isStarting,
                                cornerRadius = 16.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("login_elyby_submit")
                            ) {
                                if (isStarting) {
                                    AmberSpinner(
                                        modifier = Modifier.size(16.dp),
                                        color = NuxColors.Ink,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Menghubungi Ely.by...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.OpenInBrowser,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Buka Browser Ely.by", fontWeight = FontWeight.Black, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (verticalProviderRail) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            providerBar()
            formBody(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(AmberUi.SurfaceRaised)
                    .border(1.dp, AmberUi.Border, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            )
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            providerBar()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(AmberUi.SurfaceRaised)
                    .border(1.dp, AmberUi.Border, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                formBody(Modifier.fillMaxWidth())
            }
        }
    }

    msDeviceCode?.let { dc ->
        NuxMicrosoftAuthDialog(
            deviceCode = dc,
            onDismiss = { msDeviceCode = null },
            onSuccess = { acc ->
                msDeviceCode = null
                onAccountLoggedIn(acc, false)
            },
            onError = { err ->
                msDeviceCode = null
                msErrorMsg = err
            }
        )
    }
}

@Composable
private fun ProviderOptionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 52.dp)
            .clip(shape)
            .background(
                if (selected) accent.copy(alpha = if (AmberUi.isDark) 0.16f else 0.13f) else AmberUi.SurfaceRaised,
                shape
            )
            .border(
                if (selected) 1.5.dp else 1.dp,
                if (selected) accent else AmberUi.Border,
                shape
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) accent else AmberUi.TextDim,
            modifier = Modifier.size(17.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (selected) AmberUi.Text else AmberUi.Text,
                fontWeight = if (selected) FontWeight.Black else FontWeight.Bold,
                fontSize = 12.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = AmberUi.TextDim,
                fontSize = 10.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Pratinjau skin 3D yang ringan: enam kotak (kepala, badan, dua tangan, dua kaki) digambar langsung
 * dari berkas skin di Canvas, lengkap dengan lapisan luarnya.
 */
@Composable
private fun AccountSkinPreview(
    account: UserAccount,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val skinState = produceState<ImageBitmap?>(
        initialValue = null,
        account.id,
        account.customSkinPath,
        account.skinModel,
        account.safeAccountType
    ) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val updated = SkinUtils.ensureSkinAndCapeCached(context, account)
                val local = SkinUtils.resolveSkinFile(context, updated)
                val bmp = if (local != null) {
                    BitmapFactory.decodeFile(local.absolutePath)
                } else if (account.safeAccountType == "microsoft" && account.uuid.isNotBlank()) {
                    val req = Request.Builder().url("https://mc-heads.net/skin/${account.uuid}").build()
                    skinPreviewHttpClient.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            resp.body?.bytes()?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
                        } else null
                    }
                } else null
                bmp?.asImageBitmap()
            }.getOrNull()
        }
    }
    val skin = skinState.value
    val slim = account.safeSkinModel == "slim"

    val capeRevision by SkinUtils.capeRevision.collectAsState()
    val capeState = produceState<ImageBitmap?>(
        initialValue = null,
        account.id,
        account.customCapePath,
        account.safeAccountType,
        capeRevision
    ) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val file = if (account.safeAccountType == "microsoft") {
                    SkinUtils.ensureMojangCape(context, account)
                } else {
                    SkinUtils.resolveCapeFile(context, account)
                }
                file?.takeIf { it.length() > 0 }
                    ?.let { BitmapFactory.decodeFile(it.absolutePath) }
                    ?.asImageBitmap()
            }.getOrNull()
        }
    }
    val cape = capeState.value

    var angle by remember(account.id) { mutableFloatStateOf(0.5f) }

    if (skin == null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            NuxAccountAvatar(
                account = account,
                modifier = Modifier
                    .size(92.dp)
                    .clip(RoundedCornerShape(14.dp))
            )
        }
    } else {
        Canvas(
            modifier = modifier
                .padding(vertical = 4.dp)
                .pointerInput(account.id) {
                    detectHorizontalDragGestures { _, dragX -> angle += dragX * 0.012f }
                }
        ) {
            drawSkin3D(skin, cape, slim, angle)
        }
    }
}

private class SkinBox(
    val cx: Float,
    val top: Float,
    val w: Int,
    val h: Int,
    val d: Int,
    val u: Int,
    val v: Int,
    val ou: Int,
    val ov: Int,
    val hasOverlay: Boolean
)

private fun DrawScope.drawSkin3D(img: ImageBitmap, cape: ImageBitmap?, slim: Boolean, angle: Float) {
    val legacy = img.height < 64
    val armW = if (slim) 3 else 4
    val unit = minOf(size.width / 16f, size.height / 32.6f)
    if (unit <= 0f) return
    val ox = size.width / 2f
    val oy = (size.height - 32f * unit) / 2f
    val cosA = cos(angle)
    val sinA = sin(angle)

    drawOval(
        color = Color.Black.copy(alpha = 0.28f),
        topLeft = Offset(ox - 6f * unit, oy + 32f * unit - unit * 0.3f),
        size = Size(12f * unit, unit * 1.3f)
    )

    val boxes = listOf(
        SkinBox(0f, 0f, 8, 8, 8, 0, 0, 32, 0, true),
        SkinBox(0f, 8f, 8, 12, 4, 16, 16, 16, 32, !legacy),
        SkinBox(-(4f + armW / 2f), 8f, armW, 12, 4, 40, 16, 40, 32, !legacy),
        SkinBox(
            4f + armW / 2f, 8f, armW, 12, 4,
            if (legacy) 40 else 32, if (legacy) 16 else 48, 48, 48, !legacy
        ),
        SkinBox(-2f, 20f, 4, 12, 4, 0, 16, 0, 32, !legacy),
        SkinBox(2f, 20f, 4, 12, 4, if (legacy) 0 else 16, if (legacy) 16 else 48, 0, 48, !legacy)
    )

    val capeBehind = cosA >= 0f
    if (cape != null && capeBehind) drawCape(cape, unit, ox, oy, cosA, sinA)

    for (b in boxes.sortedByDescending { -it.cx * sinA }) {
        val hw = b.w / 2f
        val hd = b.d / 2f

        fun face(u0: Int, v0: Int, srcW: Int, x1: Float, z1: Float, x2: Float, z2: Float) {
            val sx1 = (b.cx + x1) * cosA + z1 * sinA
            val sx2 = (b.cx + x2) * cosA + z2 * sinA
            if (sx2 - sx1 < 0.01f) return
            val dstW = ((sx2 - sx1) * unit).roundToInt().coerceAtLeast(1)
            drawImage(
                image = img,
                srcOffset = IntOffset(u0, v0),
                srcSize = IntSize(srcW, b.h),
                dstOffset = IntOffset((ox + sx1 * unit).roundToInt(), (oy + b.top * unit).roundToInt()),
                dstSize = IntSize(dstW, (b.h * unit).roundToInt()),
                filterQuality = FilterQuality.None
            )
        }

        fun layer(u: Int, v: Int) {
            face(u + b.d, v + b.d, b.w, -hw, -hd, hw, -hd)
            face(u + 2 * b.d + b.w, v + b.d, b.w, hw, hd, -hw, hd)
            face(u, v + b.d, b.d, -hw, hd, -hw, -hd)
            face(u + b.d + b.w, v + b.d, b.d, hw, -hd, hw, hd)
        }

        layer(b.u, b.v)
        if (b.hasOverlay) layer(b.ou, b.ov)
    }

    if (cape != null && !capeBehind) drawCape(cape, unit, ox, oy, cosA, sinA)
}

private fun DrawScope.drawCape(cape: ImageBitmap, unit: Float, ox: Float, oy: Float, cosA: Float, sinA: Float) {
    val s = when {
        cape.width >= 64 -> cape.width / 64
        cape.width >= 22 -> cape.width / 22
        else -> 1
    }
    if (cape.width < 22 * s || cape.height < 17 * s) return

    val top = 8f
    val hw = 5f
    val zIn = 2f
    val zOut = 3f

    fun face(u0: Int, v0: Int, srcW: Int, x1: Float, z1: Float, x2: Float, z2: Float) {
        val sx1 = x1 * cosA + z1 * sinA
        val sx2 = x2 * cosA + z2 * sinA
        if (sx2 - sx1 < 0.01f) return
        val dstW = ((sx2 - sx1) * unit).roundToInt().coerceAtLeast(1)
        drawImage(
            image = cape,
            srcOffset = IntOffset(u0 * s, v0 * s),
            srcSize = IntSize(srcW * s, 16 * s),
            dstOffset = IntOffset((ox + sx1 * unit).roundToInt(), (oy + top * unit).roundToInt()),
            dstSize = IntSize(dstW, (16 * unit).roundToInt()),
            filterQuality = FilterQuality.None
        )
    }

    face(1, 1, 10, hw, zOut, -hw, zOut)
    face(0, 1, 1, -hw, zOut, -hw, zIn)
    face(11, 1, 1, hw, zIn, hw, zOut)
}

private fun acctAccent(type: String): Color = when (type) {
    "microsoft" -> NuxColors.SkyBlueDark
    "elyby" -> NuxColors.PurpleDark
    else -> NuxColors.Yellow
}

private fun acctTypeLabel(type: String): String = when (type) {
    "microsoft" -> "Microsoft"
    "elyby" -> "Ely.by"
    else -> "Offline"
}

private fun acctTypeDesc(type: String): String = when (type) {
    "microsoft" -> "Akun resmi Microsoft"
    "elyby" -> "Akun terverifikasi Ely.by"
    else -> "Akun lokal"
}

@Composable
private fun AccountTypePill(type: String) {
    val accent = acctAccent(type)
    val shape = RoundedCornerShape(7.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(accent.copy(alpha = 0.16f), shape)
            .border(NuxSizes.GlassBorder, accent.copy(alpha = 0.42f), shape)
            .padding(horizontal = 7.dp, vertical = 1.dp)
    ) {
        Text(
            text = acctTypeLabel(type),
            color = accent,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun AccountIconAction(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    tint: Color = AmberUi.TextDim,
    ring: Boolean = false
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .then(
                    if (ring) {
                        Modifier
                            .background(AmberUi.SurfaceRaised, CircleShape)
                            .border(1.dp, AmberUi.Border, CircleShape)
                    } else {
                        Modifier
                    }
                ),
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
}

/** Dialog konfirmasi hapus akun. Dipakai di layar Akun dan menu cepat Beranda. */
@Composable
internal fun DeleteAccountDialog(
    target: UserAccount,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    NuxDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .widthIn(max = 420.dp)
            .wrapContentHeight(),
        compact = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(AmberUi.Danger.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = AmberUi.Danger,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Hapus akun?",
                color = AmberUi.Text,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Hapus profil '${target.username}' (${acctTypeLabel(target.safeAccountType)}) dari perangkat ini?",
                color = AmberUi.TextDim,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NuxButton(
                    onClick = onDismiss,
                    backgroundColor = AmberUi.SurfaceRaised,
                    contentColor = AmberUi.Text,
                    cornerRadius = 16.dp,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                ) {
                    Text("Batal", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                NuxButton(
                    onClick = {
                        AccountManager.removeAccount(context, target)
                        Toast.makeText(context, "Profil '${target.username}' dihapus", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    backgroundColor = AmberUi.Danger,
                    contentColor = Color.White,
                    cornerRadius = 16.dp,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                ) {
                    Text("Hapus", fontWeight = FontWeight.Black, fontSize = 13.sp)
                }
            }
        }
    }
}

/**
 * Dialog Login / Tambah Akun untuk menu cepat Beranda.
 * Memakai UnifiedLoginComposer yang sama dengan tampilan baru agar konsisten di seluruh aplikasi.
 */
@Composable
internal fun AddAccountModal(
    onDismiss: () -> Unit,
    onAccountAdded: (UserAccount) -> Unit
) {
    val context = LocalContext.current

    NuxDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.94f)
            .widthIn(max = 580.dp)
            .wrapContentHeight(),
        fillMaxHeight = false,
        compact = true
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            val useSideRail = maxWidth >= 500.dp
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Portal Login Akun",
                            color = AmberUi.Text,
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            letterSpacing = (-0.2).sp
                        )
                        Text(
                            text = "Pilih penyedia akun untuk masuk ke Minecraft",
                            color = AmberUi.TextDim,
                            fontSize = 11.5.sp
                        )
                    }
                    AccountIconAction(
                        icon = Icons.Outlined.Close,
                        description = "Tutup",
                        ring = true,
                        onClick = onDismiss
                    )
                }

                UnifiedLoginComposer(
                    verticalProviderRail = useSideRail,
                    onAccountLoggedIn = { acc, alreadyStored ->
                        if (alreadyStored) {
                            Toast.makeText(context, "Akun ${acc.username} berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        } else {
                            onAccountAdded(acc)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun NuxAccountAvatar(
    account: UserAccount,
    modifier: Modifier = Modifier,
    fallbackInitials: String = account.username.take(2).uppercase()
) {
    val context = LocalContext.current
    val headBitmapState = produceState<Bitmap?>(
        initialValue = null,
        account.id,
        account.customSkinPath,
        account.safeAccountType
    ) {
        value = SkinUtils.getAccountHeadBitmap(context, account, targetSize = 64)
    }

    val headBitmap = headBitmapState.value

    if (headBitmap != null) {
        Image(
            bitmap = headBitmap.asImageBitmap(),
            contentDescription = account.username,
            modifier = modifier,
            filterQuality = FilterQuality.None
        )
    } else {
        Box(
            modifier = modifier.background(AmberUi.SurfaceRaised),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = fallbackInitials,
                color = AmberUi.Text,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ----------------------------------------------------------------------------
// Section: ui/screens/AmberScreenParts.kt
// ----------------------------------------------------------------------------

// =============================================================================
// KOMPONEN KACA BERSAMA untuk layar Mod, Kelola konten, Akun, dan Pengaturan
// =============================================================================

/** Tombol ikon bulat di dalam pil navigasi. */
@Composable
internal fun AmberRoundIcon(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = AmberUi.Text,
            modifier = Modifier.size(22.dp)
        )
    }
}

/**
 * Pil navigasi kiri atas yang sama di layar Mod, Akun, dan Pengaturan: panah kembali ke Home,
 * lalu tiga tujuan (Mod, Akun, Pengaturan). Tujuan yang sedang dibuka tampil sebagai chip
 * berlabel, tujuan lain berupa ikon yang bisa diketuk.
 * [active] salah satu dari "mods", "accounts", "settings".
 */
@Composable
internal fun AmberNavPill(
    active: String,
    activeLabel: String,
    onBack: () -> Unit,
    onMods: () -> Unit,
    onAccounts: () -> Unit,
    onSettings: () -> Unit,
    activeIcon: ImageVector? = null
) {
    val items = listOf(
        Triple("mods", Icons.Outlined.FileDownload, onMods),
        Triple("accounts", Icons.Outlined.AccountCircle, onAccounts),
        Triple("settings", Icons.Outlined.Tune, onSettings)
    )
    val descriptions = mapOf("mods" to "Mod", "accounts" to "Akun", "settings" to "Pengaturan")
    Row(
        modifier = Modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(CircleShape)
            .amberGlass(CircleShape)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AmberRoundIcon(
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
            description = "Kembali ke Home",
            onClick = onBack
        )
        items.forEach { (id, icon, onClick) ->
            if (id == active) {
                Row(
                    modifier = Modifier
                        .defaultMinSize(minHeight = 40.dp)
                        .clip(CircleShape)
                        .background(AmberUi.SurfaceRaised, CircleShape)
                        .border(1.dp, AmberUi.BorderStrong, CircleShape)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = activeIcon ?: icon,
                        contentDescription = null,
                        tint = AmberUi.Text,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = activeLabel,
                        color = AmberUi.Text,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 136.dp)
                    )
                }
            } else {
                AmberRoundIcon(
                    icon = icon,
                    description = descriptions[id] ?: id,
                    onClick = onClick
                )
            }
        }
    }
}

/** Kartu panjang berkaca: satu kelompok baris pengaturan atau satu blok isi. */
@Composable
internal fun AmberCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (AmberUi.animListsContent) Modifier.animateContentSize(tween(150)) else Modifier)
            .clip(shape)
            .amberGlass(shape)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        content = content
    )
}

/** Judul kecil kelompok pengaturan di atas kartu (mis. Memori, Grafis). */
@Composable
internal fun AmberSectionLabel(text: String) {
    Text(
        text = text,
        color = AmberUi.TextDim,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        maxLines = 1,
        modifier = Modifier.padding(start = 6.dp, top = 14.dp, bottom = 8.dp)
    )
}

/** Kolom cari berbentuk pil di kanan atas. */
@Composable
internal fun AmberSearchPill(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(CircleShape)
            .amberGlass(CircleShape)
            .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = AmberUi.Text,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
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
        if (value.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .clickable { onValueChange("") },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Hapus teks",
                    tint = AmberUi.TextDim,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}


/** Menu jenis addon di sisi kiri (Modpack, Mod, Shader, Resource Pack, Datapack). */
@Composable
internal fun AmberSideItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 44.dp)
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
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) AmberUi.OnAccent else AmberUi.AccentSoft,
            modifier = Modifier.size(19.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            color = if (selected) AmberUi.OnAccent else AmberUi.Text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


/** Pil filter: label redup + nilai tebal + panah, mis. "Kategori Semua ⌄". */
@Composable
internal fun AmberFilterPill(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showChevron: Boolean = true
) {
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 40.dp)
            .clip(CircleShape)
            .amberGlass(CircleShape)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = AmberUi.TextDim,
            fontSize = 13.sp,
            maxLines = 1
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = value,
            color = AmberUi.Text,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 120.dp)
        )
        if (showChevron) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                tint = AmberUi.TextDim,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}


/** Tombol pil: terisi biru (aksi utama) atau hanya garis tepi (aksi biasa). */
@Composable
internal fun AmberPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    filled: Boolean = true,
    enabled: Boolean = true,
    danger: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.4f)
            .defaultMinSize(minHeight = 40.dp)
            .clip(CircleShape)
            .background(if (filled) AmberUi.Accent else Color.Transparent, CircleShape)
            .border(1.dp, if (danger) AmberUi.Danger.copy(alpha = 0.6f) else if (filled) AmberUi.AccentEdge else AmberUi.BorderStrong, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                enabled = enabled
            ) { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val fg = when {
            danger && !filled -> AmberUi.Danger
            filled -> AmberUi.OnAccent
            else -> AmberUi.Text
        }
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            color = fg,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}


/** Satu baris pilihan di dalam dialog pemilih. Terpilih = isian biru + centang. */
@Composable
internal fun AmberPickRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 42.dp)
            .clip(shape)
            .background(if (selected) AmberUi.Accent else NuxColors.SurfaceWhite, shape)
            .border(1.dp, if (selected) AmberUi.AccentEdge else NuxColors.CardBorder, shape)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (selected) AmberUi.OnAccent else AmberUi.Text,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            fontSize = 13.sp,
            maxLines = 2,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = AmberUi.OnAccent,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}


/** Daftar pilihan dalam dialog kaca (pengganti menu tarik-turun): instance dan world. */
@Composable
internal fun AmberPickerDialog(
    title: String,
    options: List<Pair<String, String>>,
    selectedId: String?,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    NuxDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .widthIn(max = 460.dp)
            .wrapContentHeight(),
        compact = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                color = NuxColors.DarkGray
            )
            Spacer(modifier = Modifier.height(10.dp))
            Column(
                modifier = Modifier
                    .heightIn(max = 220.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                options.forEach { (id, label) ->
                    AmberPickRow(
                        label = label,
                        selected = id == selectedId,
                        onClick = { onPick(id) }
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------
// Section: ui/screens/CreateInstanceScreen.kt
// ----------------------------------------------------------------------------

/** Filter jenis versi Minecraft di layar Buat Instance. */
enum class VersionCategory(val label: String) {
    RELEASE("Release"),
    SNAPSHOT("Snapshot"),
    OLD_BETA("Old Beta"),
    OLD_ALPHA("Old Alpha"),
    ALL("Semua")
}

/** Satu loader di rel kiri. */
private class CiLoader(val id: String, val label: String, val icon: ImageVector)

private val CI_LOADERS = listOf(
    CiLoader("fabric", "Fabric", Icons.Outlined.Layers),
    CiLoader("forge", "Forge", Icons.Outlined.Extension),
    CiLoader("neoforge", "NeoForge", Icons.Outlined.Memory),
    CiLoader(HorizonEngine.LOADER_ID, HorizonEngine.DISPLAY_NAME, Icons.Outlined.ViewInAr),
    CiLoader("vanilla", "Vanilla", Icons.Outlined.Public)
)

/**
 * Kartu versi untuk mesin Rust bila daftar Mojang belum atau gagal dimuat: mesinnya hanya mengerjakan satu
 * versi dan tidak butuh daftar itu. releaseTime kosong membuat kartu menampilkan nama mesin, bukan tanggal.
 */
private fun ciEngineVersion(versionId: String) = VersionItem(
    id = versionId,
    type = "release",
    url = "",
    time = "",
    releaseTime = ""
)

/** Satu baris versi loader (Fabric/Forge/NeoForge): [tag] kosong bila tidak ada keterangan. */
private class CiChoice(val id: String, val tag: String)

private val CI_RELEASE_COLOR = Color(0xFF5FD38D)
private val CI_SNAPSHOT_COLOR = Color(0xFFF2B84B)

/**
 * Layar penuh "Buat Instance", polanya sama dengan layar Mod: rel loader di kiri (Fabric, Forge,
 * NeoForge, Vanilla), daftar versi Minecraft yang didukung loader itu di kanan. Mengetuk sebuah
 * versi membuka daftar versi loader tepat di bawah kartunya; loader terbaru/disarankan terpilih
 * otomatis, tapi bisa diganti (mis. loader versi lama). Nama instance dan tombol Install ada di
 * panel atas.
 */
@Composable
fun CreateInstanceScreen(
    onNavigateBack: () -> Unit,
    onInstanceCreated: (Instance) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var selectedLoader by remember { mutableStateOf("fabric") }
    var instanceName by remember { mutableStateOf("") }
    var selectedVersion by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(VersionCategory.RELEASE) }

    var versionsList by remember { mutableStateOf<List<VersionItem>>(emptyList()) }
    var latestReleaseId by remember { mutableStateOf<String?>(null) }
    var latestSnapshotId by remember { mutableStateOf<String?>(null) }
    var manifestUpdatedAtMillis by remember { mutableStateOf(0L) }
    var isLoadingVersions by remember { mutableStateOf(true) }
    var manifestError by remember { mutableStateOf(false) }
    var manifestErrorMessage by remember { mutableStateOf<String?>(null) }
    var reloadTick by remember { mutableIntStateOf(0) }

    // Versi Minecraft yang punya loader terpilih. null = belum ada / gagal dimuat (pakai perkiraan).
    var supported by remember { mutableStateOf<Set<String>?>(null) }
    var supportedKey by remember { mutableStateOf("") }

    // Versi loader untuk versi Minecraft terpilih; loaderLoadedKey = "<loader>:<mc>" saat sudah selesai dimuat.
    var loaderChoices by remember { mutableStateOf<List<CiChoice>>(emptyList()) }
    var loaderVersion by remember { mutableStateOf("") }
    var loaderLoadedKey by remember { mutableStateOf("") }

    val horizonMcVersion by HorizonEngine.liveMcVersion.collectAsState()

    LaunchedEffect(reloadTick) {
        isLoadingVersions = true
        manifestError = false
        manifestErrorMessage = null
        val result = MojangManifestService.getVersionManifest(forceRefresh = true)
        result.fold(
            onSuccess = { manifest ->
                versionsList = manifest.versions
                latestReleaseId = manifest.latest.release
                latestSnapshotId = manifest.latest.snapshot
                manifestUpdatedAtMillis = System.currentTimeMillis()
                manifestError = false
                manifestErrorMessage = null
            },
            onFailure = { err ->
                manifestError = true
                manifestErrorMessage = err.message
            }
        )
        HorizonEngine.refreshLatestVersion(context, forceNetwork = reloadTick > 0)
        isLoadingVersions = false
    }

    LaunchedEffect(selectedLoader, reloadTick) {
        supported = null
        supportedKey = ""
        val force = reloadTick > 0
        val result = when (selectedLoader) {
            "fabric" -> FabricService.getSupportedGameVersions(forceRefresh = force)
            "forge" -> ForgeService.getSupportedMcVersions(ForgeFlavor.FORGE, forceRefresh = force)
            "neoforge" -> ForgeService.getSupportedMcVersions(ForgeFlavor.NEOFORGE, forceRefresh = force)
            else -> null
        }
        supported = result?.getOrNull()
        supportedKey = selectedLoader
    }

    LaunchedEffect(selectedVersion, selectedLoader, reloadTick) {
        loaderChoices = emptyList()
        loaderVersion = ""
        loaderLoadedKey = ""
        val mc = selectedVersion
        if (mc == null || selectedLoader == "vanilla" || selectedLoader == HorizonEngine.LOADER_ID) return@LaunchedEffect
        val force = reloadTick > 0
        if (selectedLoader == "fabric") {
            val loaders = FabricService.getFabricLoaders(mc, forceRefresh = force).getOrNull() ?: emptyList()
            loaderChoices = loaders.take(40).mapIndexed { index, v ->
                CiChoice(v, if (index == 0) "Terbaru" else "")
            }
            loaderVersion = loaders.firstOrNull() ?: ""
        } else {
            val flavor = ForgeFlavor.of(selectedLoader)
            if (flavor != null) {
                val builds = ForgeService.getBuilds(mc, flavor, forceRefresh = force).getOrNull() ?: emptyList()
                loaderChoices = builds.take(40).map { build ->
                    CiChoice(
                        build.version,
                        when {
                            build.recommended -> "Disarankan"
                            build.latest -> "Terbaru"
                            else -> ""
                        }
                    )
                }
                loaderVersion = ForgeService.pickDefault(builds)?.version ?: ""
            }
        }
        loaderLoadedKey = "$selectedLoader:$mc"
    }

    val isVanilla = selectedLoader == "vanilla"
    val isEngine = selectedLoader == HorizonEngine.LOADER_ID
    // Vanilla dan mesin Rust tidak punya versi loader untuk dipilih.
    val noLoaderVersions = isVanilla || isEngine
    val loaderInfo = CI_LOADERS.firstOrNull { it.id == selectedLoader } ?: CI_LOADERS.first()
    val flavor = ForgeFlavor.of(selectedLoader)

    // Vanilla punya semua jenis; loader hanya rilis dan snapshot; mesin Rust hanya satu versi, tanpa penyaring.
    val categories = when {
        isEngine -> listOf(VersionCategory.ALL)
        isVanilla -> VersionCategory.values().toList()
        else -> listOf(VersionCategory.RELEASE, VersionCategory.SNAPSHOT, VersionCategory.ALL)
    }
    val effectiveCategory = if (category in categories) category else categories.first()

    // Perkiraan bila daftar resmi gagal dimuat: Fabric mulai Minecraft 1.14; Forge/NeoForge sesuai batas Amber.
    val fabricMinTime = remember(versionsList) { versionsList.firstOrNull { it.id == "1.14" }?.releaseTime ?: "" }
    val supportedNow = supported
    val visibleVersions = remember(versionsList, supportedNow, selectedLoader, effectiveCategory, query) {
        val q = query.trim()
        versionsList.filter { item ->
            val matchesCategory = when (effectiveCategory) {
                VersionCategory.ALL -> true
                VersionCategory.RELEASE -> item.type == "release"
                VersionCategory.SNAPSHOT -> item.type == "snapshot" || item.type == "pending" || item.type == "unobfuscated"
                VersionCategory.OLD_BETA -> item.type == "old_beta" || item.id.startsWith("b")
                VersionCategory.OLD_ALPHA -> item.type == "old_alpha" || item.id.startsWith("a")
            }
            val matchesQuery = q.isEmpty() || item.id.contains(q, ignoreCase = true)
            val matchesLoader = when {
                selectedLoader == "vanilla" -> true
                supportedNow != null -> supportedNow.contains(item.id)
                flavor != null -> ForgeService.isMcSupported(item.id, flavor)
                else -> item.type != "old_beta" && item.type != "old_alpha" &&
                    (fabricMinTime.isEmpty() || item.releaseTime >= fabricMinTime)
            }
            matchesCategory && matchesQuery && matchesLoader
        }
    }

    // Mesin Rust otomatis mendeteksi versi aktif Horizon terbaru (26.3 ke atas) secara real-time.
    val shownVersions = if (isEngine) {
        listOf(versionsList.firstOrNull { it.id == horizonMcVersion } ?: ciEngineVersion(horizonMcVersion))
    } else {
        visibleVersions
    }

    // Ada versi Minecraft 26.x ke atas di daftar: tampilkan peringatan dan pengecekan Vulkan di atasnya.
    val hasYearVersions = remember(shownVersions) { shownVersions.any { McVulkan.isYearVersion(it.id) } }

    val supportedPending = !noLoaderVersions && supportedKey != selectedLoader
    val listLoading = !isEngine && ((isLoadingVersions && versionsList.isEmpty()) || supportedPending)

    val currentKey = if (selectedVersion != null) "$selectedLoader:$selectedVersion" else ""
    val loaderPending = !noLoaderVersions && selectedVersion != null && loaderLoadedKey != currentKey
    val chosenMc = selectedVersion

    fun quickPickMojangVersion(versionId: String, targetCategory: VersionCategory) {
        val currentSupported = when {
            selectedLoader == "vanilla" -> true
            supportedNow != null -> supportedNow.contains(versionId)
            flavor != null -> ForgeService.isMcSupported(versionId, flavor)
            else -> versionsList.firstOrNull { it.id == versionId }?.type != "old_beta"
        }
        if (!currentSupported) {
            selectedLoader = "vanilla"
        }
        query = ""
        category = targetCategory
        selectedVersion = versionId
        expanded = !isVanilla && currentSupported
        scope.launch { listState.animateScrollToItem(0) }
    }

    // Mesin Rust: build ini harus membawa mesinnya dan HP-nya harus mampu. Alasan bila tidak, tampil di CiEngineNotice.
    val engineStatus = remember(isEngine) { if (isEngine) HorizonEngine.availability(context) else null }
    val engineBlocked = engineStatus is HorizonEngine.Availability.Unavailable
    val canInstall = chosenMc != null && !engineBlocked &&
        (noLoaderVersions || (!loaderPending && loaderVersion.isNotBlank()))
    val defaultName = if (chosenMc != null) "$chosenMc ${loaderInfo.label}" else ""

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 10.dp)
    ) {
        val isNarrow = maxWidth < 640.dp

        Column(modifier = Modifier.fillMaxSize()) {
            // =========================================================================
            // BARIS ATAS: navigasi | ringkasan pilihan + nama instance + tombol Install
            // =========================================================================
            val summaryBox: @Composable (Modifier) -> Unit = { m ->
                Column(
                    modifier = m,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (chosenMc != null) {
                        RawText(
                            text = "$chosenMc ${loaderInfo.label}",
                            color = AmberUi.Text,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = "Pilih versi",
                            color = AmberUi.TextDim,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (chosenMc != null && !noLoaderVersions && loaderVersion.isNotBlank()) {
                        RawText(
                            text = "${loaderInfo.label} $loaderVersion",
                            color = AmberUi.AccentSoft,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            val nameField: @Composable (Modifier) -> Unit = { m ->
                Box(
                    modifier = m,
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (instanceName.isEmpty()) {
                        if (defaultName.isNotEmpty()) {
                            RawText(
                                text = defaultName,
                                color = AmberUi.TextDim,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = "Nama instance",
                                color = AmberUi.TextDim,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    BasicTextField(
                        value = instanceName,
                        onValueChange = { instanceName = it.take(48) },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = AmberUi.Text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(AmberUi.AccentSoft),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_instance_name_input")
                    )
                }
            }

            val installButton: @Composable () -> Unit = {
                AmberPillButton(
                    text = "Install",
                    icon = Icons.Outlined.Download,
                    enabled = canInstall,
                    modifier = Modifier.testTag("create_instance_install_button"),
                    onClick = {
                        val mc = selectedVersion
                        if (mc != null) {
                            val label = loaderInfo.label
                            onInstanceCreated(
                                Instance(
                                    id = "inst_${UUID.randomUUID()}",
                                    name = instanceName.trim().ifBlank { "$mc $label" },
                                    mcVersion = mc,
                                    loader = selectedLoader,
                                    loaderVersion = if (noLoaderVersions) "" else loaderVersion,
                                    icon = if (selectedLoader == "fabric") "fabric" else "crafting_table",
                                    isDownloaded = isEngine
                                )
                            )
                        }
                    }
                )
            }

            if (isNarrow) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CiNavPill(onBack = onNavigateBack)
                        summaryBox(Modifier.weight(1f))
                        installButton()
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 46.dp)
                            .clip(CircleShape)
                            .amberGlass(CircleShape)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        nameField(Modifier.fillMaxWidth())
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CiNavPill(onBack = onNavigateBack)
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(CircleShape)
                            .amberGlass(CircleShape)
                            .padding(start = 18.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        summaryBox(Modifier.widthIn(min = 96.dp, max = 190.dp))
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .width(1.dp)
                                .height(24.dp)
                                .background(AmberUi.Border)
                        )
                        nameField(Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(8.dp))
                        installButton()
                    }
                }
            }

            // =========================================================================
            // ISI: rel loader (kiri di layar lebar, atas di layar sempit) + daftar versi
            // =========================================================================
            val onPickLoader: (String) -> Unit = { loaderId ->
                if (selectedLoader != loaderId) {
                    selectedLoader = loaderId
                    selectedVersion = if (loaderId == HorizonEngine.LOADER_ID) {
                        horizonMcVersion
                    } else {
                        null
                    }
                    expanded = false
                    scope.launch { listState.scrollToItem(0) }
                }
            }

            if (isNarrow) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CI_LOADERS.forEach { loader ->
                        CiChip(
                            label = loader.label,
                            selected = selectedLoader == loader.id,
                            onClick = { onPickLoader(loader.id) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!isNarrow) {
                    Column(
                        modifier = Modifier
                            .width(148.dp)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        CI_LOADERS.forEach { loader ->
                            AmberSideItem(
                                icon = loader.icon,
                                label = loader.label,
                                selected = selectedLoader == loader.id,
                                onClick = { onPickLoader(loader.id) }
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    if (!isEngine) {
                        val categoriesStrip: @Composable (Modifier) -> Unit = { m ->
                            Row(
                                modifier = m.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                categories.forEach { entry ->
                                    CiChip(
                                        label = entry.label,
                                        selected = effectiveCategory == entry,
                                        modifier = Modifier.testTag("create_instance_cat_${entry.name.lowercase()}"),
                                        onClick = { category = entry }
                                    )
                                }
                            }
                        }

                        val refreshBtn: @Composable () -> Unit = {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .amberGlass(CircleShape)
                                    .clickable(enabled = !isLoadingVersions) { reloadTick += 1 }
                                    .testTag("create_instance_refresh_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLoadingVersions) {
                                    AmberSpinner(
                                        modifier = Modifier.size(18.dp),
                                        color = AmberUi.AccentSoft,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.Refresh,
                                        contentDescription = "Segarkan daftar versi",
                                        tint = AmberUi.Text,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        if (isNarrow) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                categoriesStrip(Modifier.fillMaxWidth())
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AmberSearchPill(
                                        value = query,
                                        onValueChange = { query = it },
                                        placeholder = "Cari versi...",
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("create_instance_search_input")
                                    )
                                    refreshBtn()
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                categoriesStrip(Modifier.weight(1f))
                                AmberSearchPill(
                                    value = query,
                                    onValueChange = { query = it },
                                    placeholder = "Cari versi...",
                                    modifier = Modifier
                                        .width(190.dp)
                                        .testTag("create_instance_search_input")
                                )
                                refreshBtn()
                            }
                        }

                    if (latestReleaseId != null || latestSnapshotId != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            latestReleaseId?.let { relId ->
                                CiLiveMojangChip(
                                    title = "Rilis Terbaru",
                                    versionId = relId,
                                    accent = CI_RELEASE_COLOR,
                                    icon = Icons.Outlined.NewReleases,
                                    selected = selectedVersion == relId,
                                    modifier = Modifier.testTag("create_instance_latest_release_chip"),
                                    onClick = { quickPickMojangVersion(relId, VersionCategory.RELEASE) }
                                )
                            }
                            latestSnapshotId?.takeIf { it != latestReleaseId }?.let { snapId ->
                                CiLiveMojangChip(
                                    title = "Snapshot Terbaru",
                                    versionId = snapId,
                                    accent = CI_SNAPSHOT_COLOR,
                                    icon = Icons.Outlined.Science,
                                    selected = selectedVersion == snapId,
                                    modifier = Modifier.testTag("create_instance_latest_snapshot_chip"),
                                    onClick = { quickPickMojangVersion(snapId, VersionCategory.SNAPSHOT) }
                                )
                            }
                            if (manifestUpdatedAtMillis > 0L) {
                                RawText(
                                    text = "Mojang · ${MCNewsService.formatClock(manifestUpdatedAtMillis)}",
                                    color = AmberUi.TextDim,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    }
                }

                when {
                    listLoading -> CiNote(
                        text = if (isLoadingVersions) "Memuat versi Minecraft..." else "Memuat versi yang didukung ${loaderInfo.label}..."
                    )
                    manifestError && versionsList.isEmpty() && !isEngine -> CiNote(
                        text = "Gagal memuat daftar versi Minecraft",
                        detailText = manifestErrorMessage,
                        actionLabel = "Coba lagi",
                        actionTag = "create_instance_retry_button",
                        onAction = { reloadTick += 1 }
                    )
                    shownVersions.isEmpty() -> CiNote(text = "Tidak ada versi yang cocok")
                    else -> {
                        if (manifestError && !isEngine) {
                            CiRefreshErrorBanner(
                                detail = manifestErrorMessage,
                                onRetry = { reloadTick += 1 }
                            )
                        }
                        val headerOffset = if (isEngine || hasYearVersions) 1 else 0
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 12.dp)
                        ) {
                            if (isEngine) {
                                item(key = "engine-notice") {
                                    CiEngineNotice(status = engineStatus)
                                }
                            } else if (hasYearVersions) {
                                item(key = "mc26-notice") { CiModernNotice(selectedMc = selectedVersion) }
                            }
                            items(shownVersions, key = { it.id }) { item ->
                                val isSelected = item.id == selectedVersion
                                CiVersionCard(
                                    item = item,
                                    loaderLabel = loaderInfo.label,
                                    loaderIcon = loaderInfo.icon,
                                    isVanilla = noLoaderVersions,
                                    isLatestRelease = item.id == latestReleaseId,
                                    isLatestSnapshot = item.id == latestSnapshotId,
                                    selected = isSelected,
                                    expanded = isSelected && expanded,
                                    loading = isSelected && loaderPending,
                                    choices = if (isSelected) loaderChoices else emptyList(),
                                    chosenId = if (isSelected) loaderVersion else "",
                                    onClick = {
                                        if (isSelected) {
                                            expanded = !expanded
                                        } else {
                                            selectedVersion = item.id
                                            expanded = true
                                            scope.launch {
                                                val index = shownVersions.indexOfFirst { it.id == item.id }
                                                if (index >= 0) {
                                                    listState.animateScrollToItem(index + headerOffset)
                                                }
                                            }
                                        }
                                    },
                                    onPick = { loaderVersion = it }
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

// -------------------------------------------------------------------- bagian kecil

/** Pil navigasi kiri atas: panah kembali ke Home dan penanda layar ini. */
@Composable
private fun CiNavPill(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .height(48.dp)
            .clip(CircleShape)
            .amberGlass(CircleShape)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable { onBack() }
                .testTag("create_instance_back_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Kembali ke Home",
                tint = AmberUi.Text,
                modifier = Modifier.size(22.dp)
            )
        }
        Row(
            modifier = Modifier
                .height(40.dp)
                .clip(CircleShape)
                .background(AmberUi.SurfaceRaised, CircleShape)
                .border(1.dp, AmberUi.BorderStrong, CircleShape)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                tint = AmberUi.Text,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Buat Instance",
                color = AmberUi.Text,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1
            )
        }
    }
}

/** Pil filter jenis versi: terpilih berisi warna aksen, selain itu kaca. */
@Composable
private fun CiChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .height(48.dp)
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
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = AmberUi.Text,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            fontSize = 13.sp,
            maxLines = 1
        )
    }
}

/** Pil cepat rilis/snapshot terbaru langsung dari manifest resmi Mojang. */
@Composable
private fun CiLiveMojangChip(
    title: String,
    versionId: String,
    accent: Color,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = CircleShape
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(shape)
            .background(
                if (selected) accent.copy(alpha = 0.24f) else accent.copy(alpha = 0.10f),
                shape
            )
            .border(
                1.dp,
                if (selected) accent else accent.copy(alpha = 0.4f),
                shape
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = title,
            color = accent,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        RawText(
            text = versionId,
            color = AmberUi.Text,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
    }
}

@Composable
private fun CiRefreshErrorBanner(
    detail: String?,
    onRetry: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(shape)
            .background(AmberUi.Danger.copy(alpha = 0.14f), shape)
            .border(1.dp, AmberUi.Danger.copy(alpha = 0.45f), shape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Gagal memperbarui daftar versi terbaru",
                color = AmberUi.Text,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            if (!detail.isNullOrBlank()) {
                RawText(
                    text = detail,
                    color = AmberUi.TextDim,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        AmberPillButton(
            text = "Coba lagi",
            onClick = onRetry,
            modifier = Modifier.testTag("create_instance_banner_retry_button")
        )
    }
}

/** Teks tengah untuk keadaan memuat, kosong, atau gagal (tanpa indikator berputar). */
@Composable
private fun ColumnScope.CiNote(
    text: String,
    detailText: String? = null,
    actionLabel: String? = null,
    actionTag: String? = null,
    onAction: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            color = AmberUi.TextDim,
            fontSize = 14.sp,
            maxLines = 2,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        if (!detailText.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            RawText(
                text = detailText,
                color = AmberUi.TextDim.copy(alpha = 0.8f),
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
        if (actionLabel != null) {
            Spacer(modifier = Modifier.height(12.dp))
            AmberPillButton(
                text = actionLabel,
                onClick = onAction,
                modifier = if (actionTag != null) Modifier.testTag(actionTag) else Modifier
            )
        }
    }
}

/**
 * Keterangan mesin Rust di atas kartu versi: apa bedanya dengan loader lain, batas versinya, dan alasan bila
 * mesinnya belum bisa dipakai di build atau HP ini. Isi teksnya persis sama dengan kunci di
 * assets/lang/<bahasa>/horizon.tsv supaya ikut diterjemahkan.
 */
@Composable
private fun CiEngineNotice(
    status: HorizonEngine.Availability?
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .amberGlass(shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "MESIN RUST (HORIZON)",
            color = AmberUi.TextDim,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            maxLines = 1
        )
        Text(
            text = "Mesin Rust menggantikan seluruh client Java Minecraft: tanpa Java, tanpa mod Fabric, Forge, atau NeoForge.",
            color = AmberUi.Text,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
        val activeHorizonVersion by HorizonEngine.liveMcVersion.collectAsState()
        Text(
            text = "Versi aktif real-time Horizon: Minecraft $activeHorizonVersion (otomatis diperbarui). Sumber daya game (~500 MiB) diunduh mesin saat pertama dibuka.",
            color = AmberUi.TextDim,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
        Text(
            text = "Eksperimental: butuh Android 9 ke atas dan perangkat 64-bit (arm64-v8a).",
            color = AmberUi.TextDim,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
        if (status is HorizonEngine.Availability.Unavailable) {
            Text(
                text = status.reason,
                color = AmberUi.Danger,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 18.sp
            )
        } else if (status is HorizonEngine.Availability.Ready && status.lowRam) {
            Text(
                text = HorizonEngine.LOW_RAM_WARNING,
                color = CI_SNAPSHOT_COLOR,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
    }
}

/**
 * Peringatan di atas daftar versi bila ada Minecraft 26.x ke atas: versi itu memakai Java 25 dan mesin grafis
 * baru yang belum bisa dijalankan semua HP, jadi pemain diberi tahu lebih dulu bahwa gagal dibuka belum tentu
 * salah launcher. Di bawahnya hasil pengecekan Vulkan HP ini (berjalan otomatis sekali, di proses terpisah;
 * lihat VulkanCheck), dinilai untuk versi yang sedang dipilih. Kunci teksnya ada di
 * assets/lang/<bahasa>/minecraft26.tsv.
 */
@Composable
private fun CiModernNotice(selectedMc: String?) {
    val context = LocalContext.current
    var report by remember { mutableStateOf<VulkanReport?>(null) }
    LaunchedEffect(Unit) {
        report = try {
            VulkanCheck.ensure(context)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            VulkanReport(VulkanState.NO_RESPONSE, detail = e.javaClass.simpleName)
        }
    }
    val verdict = remember(report, selectedMc) { McVulkan.verdict(report, selectedMc) }
    val verdictColor = when (verdict.level) {
        VerdictLevel.OK -> CI_RELEASE_COLOR
        VerdictLevel.WARN -> CI_SNAPSHOT_COLOR
        VerdictLevel.BAD -> AmberUi.Danger
        VerdictLevel.CHECKING, VerdictLevel.INFO -> AmberUi.TextDim
    }

    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .amberGlass(shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "MINECRAFT 26.X KE ATAS",
            color = AmberUi.TextDim,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            maxLines = 1
        )
        Text(
            text = "Versi ini memakai Java 25 dan mesin grafis baru. Belum semua HP bisa menjalankannya, terutama yang driver Vulkan-nya lama atau kurang lengkap.",
            color = AmberUi.Text,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
        Text(
            text = "Kalau game gagal dibuka, bisa jadi penyebabnya HP atau versi game ini, bukan launchernya. Coba ganti renderer di Pengaturan, atau pakai Minecraft 1.21.x.",
            color = AmberUi.TextDim,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
        Text(
            text = verdict.text,
            color = verdictColor,
            fontSize = 13.sp,
            fontWeight = if (verdict.level == VerdictLevel.BAD || verdict.level == VerdictLevel.WARN) {
                FontWeight.SemiBold
            } else {
                FontWeight.Normal
            },
            lineHeight = 18.sp
        )
        if (verdict.detail.isNotEmpty()) {
            Text(
                text = verdict.detail,
                color = AmberUi.TextDim,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
    }
}

/** Lencana kecil (jenis versi, Terbaru, Disarankan). */
@Composable
private fun CiTag(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

/** Teks dan warna lencana jenis versi. */
private fun ciTypeBadge(item: VersionItem): Pair<String, Color> = when {
    item.type == "release" -> "RELEASE" to CI_RELEASE_COLOR
    item.type == "snapshot" -> "SNAPSHOT" to CI_SNAPSHOT_COLOR
    item.type == "old_beta" || item.id.startsWith("b") -> "BETA" to AmberUi.TextDim
    item.type == "old_alpha" || item.id.startsWith("a") -> "ALPHA" to AmberUi.TextDim
    else -> item.type.uppercase() to AmberUi.TextDim
}

/**
 * Kartu satu versi Minecraft. Saat terpilih dan terbuka (bukan Vanilla), di bawahnya muncul daftar
 * versi loader: yang terbaru/disarankan sudah terpilih, dan bisa diganti ke versi lain.
 */
@Composable
private fun CiVersionCard(
    item: VersionItem,
    loaderLabel: String,
    loaderIcon: ImageVector,
    isVanilla: Boolean,
    isLatestRelease: Boolean,
    isLatestSnapshot: Boolean,
    selected: Boolean,
    expanded: Boolean,
    loading: Boolean,
    choices: List<CiChoice>,
    chosenId: String,
    onClick: () -> Unit,
    onPick: (String) -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    val iconShape = RoundedCornerShape(14.dp)
    val (badgeText, badgeColor) = ciTypeBadge(item)
    val isLatest = isLatestRelease || isLatestSnapshot

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .amberGlass(shape)
            .then(if (selected) Modifier.border(1.5.dp, AmberUi.AccentEdge, shape) else Modifier)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(iconShape)
                    .background(if (selected) AmberUi.Accent else AmberUi.SurfaceRaised, iconShape)
                    .border(1.dp, AmberUi.Border, iconShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = loaderIcon,
                    contentDescription = null,
                    tint = if (selected) Color.White else AmberUi.AccentSoft,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RawText(
                        text = item.id,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = AmberUi.Text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    CiTag(badgeText, badgeColor)
                    if (isLatest) {
                        CiTag("Terbaru", if (isLatestRelease) CI_RELEASE_COLOR else CI_SNAPSHOT_COLOR)
                    }
                }
                if (selected && !isVanilla && chosenId.isNotBlank()) {
                    RawText(
                        text = "$loaderLabel $chosenId",
                        fontSize = 13.sp,
                        color = AmberUi.AccentSoft,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else if (item.releaseTime.isBlank()) {
                    RawText(
                        text = loaderLabel,
                        fontSize = 13.sp,
                        color = AmberUi.TextDim,
                        maxLines = 1
                    )
                } else {
                    Text(
                        text = "Dirilis ${MCNewsService.formatIsoDate(item.releaseTime)}",
                        fontSize = 13.sp,
                        color = AmberUi.TextDim,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Icon(
                imageVector = when {
                    selected && isVanilla -> Icons.Outlined.Check
                    expanded -> Icons.Outlined.KeyboardArrowDown
                    else -> Icons.Outlined.ChevronRight
                },
                contentDescription = null,
                tint = if (selected) AmberUi.AccentSoft else AmberUi.TextDim,
                modifier = Modifier.size(22.dp)
            )
        }

        if (expanded && !isVanilla) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(AmberUi.Border)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 14.dp)
            ) {
                Text(
                    text = "LOADER ${loaderLabel.uppercase()}",
                    color = AmberUi.TextDim,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(8.dp))
                when {
                    loading -> Text(
                        text = "Memuat loader $loaderLabel...",
                        color = AmberUi.TextDim,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                    choices.isEmpty() -> Text(
                        text = "Loader $loaderLabel belum tersedia untuk Minecraft ${item.id}",
                        color = AmberUi.Danger,
                        fontSize = 13.sp,
                        maxLines = 2
                    )
                    else -> {
                        Text(
                            text = "Versi terbaru dipilih otomatis. Ketuk versi lain bila butuh loader yang lebih lama.",
                            color = AmberUi.TextDim,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            choices.forEach { choice ->
                                CiLoaderRow(
                                    choice = choice,
                                    picked = choice.id == chosenId,
                                    onClick = { onPick(choice.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Satu baris versi loader di dalam kartu versi. Terpilih = isian aksen + centang. */
@Composable
private fun CiLoaderRow(choice: CiChoice, picked: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .background(if (picked) AmberUi.Accent else AmberUi.SurfaceRaised, shape)
            .border(1.dp, if (picked) AmberUi.AccentEdge else AmberUi.Border, shape)
            .clickable { onClick() }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RawText(
            text = choice.id,
            color = if (picked) AmberUi.OnAccent else AmberUi.Text,
            fontSize = 14.sp,
            fontWeight = if (picked) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (choice.tag.isNotEmpty()) {
            Spacer(modifier = Modifier.width(8.dp))
            CiTag(choice.tag, if (picked) Color.White else AmberUi.AccentSoft)
        }
        Spacer(modifier = Modifier.weight(1f))
        if (picked) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ----------------------------------------------------------------------------
// Section: ui/screens/CustomGuiEditorScreen.kt
// ----------------------------------------------------------------------------

/**
 * Seperti coerceIn, tapi tidak melempar galat bila rentangnya terbalik (tombol lebih besar dari
 * layar): pusatnya dipasang di tengah rentang.
 */
private fun Float.clampCenter(lo: Float, hi: Float): Float =
    if (lo > hi) (lo + hi) / 2f else coerceIn(lo, hi)

/** Pilihan "lihat" di editor: semua tombol, atau hanya yang tampil saat main / saat menu. */
private const val VIEW_ALL = "ALL"

/** Tombol sistem selalu terlihat di semua tampilan; tombol biasa mengikuti mode tampilnya. */
private fun editorShows(b: CustomControlButton, view: String): Boolean =
    view == VIEW_ALL || b.isSystem || ControlShowMode.visible(b.showMode, view == ControlShowMode.MENU)

@Composable
fun CustomGuiEditorScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    isIngame: Boolean = false
) {
    val context = LocalContext.current
    val savedButtons by ControlLayoutManager.buttons.collectAsState()

    var buttonsList by remember { mutableStateOf(savedButtons.ifEmpty { ControlLayoutManager.getDefaultButtons() }) }
    var selectedButtonId by remember { mutableStateOf<String?>(null) }
    var showKeyPickerForButtonId by remember { mutableStateOf<String?>(null) }
    var keyPickerTargetMode by remember { mutableStateOf("MAIN") } // "MAIN" or "COMBO"
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var manualFlipSide by remember { mutableStateOf<Boolean?>(null) }
    var viewMode by remember { mutableStateOf(VIEW_ALL) }
    var uiHidden by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        ControlLayoutManager.init(context)
        if (ControlLayoutManager.buttons.value.isNotEmpty()) {
            buttonsList = ControlLayoutManager.buttons.value
        }
    }

    LaunchedEffect(savedButtons) {
        if (savedButtons.isNotEmpty() && buttonsList.isEmpty()) {
            buttonsList = savedButtons
        }
    }

    BackHandler {
        when {
            showKeyPickerForButtonId != null -> showKeyPickerForButtonId = null
            showResetConfirmDialog -> showResetConfirmDialog = false
            selectedButtonId != null -> selectedButtonId = null
            uiHidden -> uiHidden = false
            else -> onNavigateBack()
        }
    }

    val selectedButton = remember(buttonsList, selectedButtonId) {
        buttonsList.firstOrNull { it.id == selectedButtonId }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(if (isIngame) Color.Black.copy(alpha = 0.44f) else Amber.Ink)
    ) {
        val screenWidthPx = constraints.maxWidth.toFloat()
        val screenHeightPx = constraints.maxHeight.toFloat()
        val density = LocalDensity.current
        val isNarrowEditor = maxWidth < 600.dp
        val (cutoutLeft, cutoutRight) = rememberCameraSideInsets(gap = 6.dp)
        val safeSystemPadding = if (isIngame) {
            Modifier.padding(start = cutoutLeft, end = cutoutRight)
        } else {
            Modifier
                .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.navigationBars))
                .padding(start = cutoutLeft, end = cutoutRight)
        }

        // 1. Cyber-Glass Blueprint Grid Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 32.dp.toPx()
            val dotColor = if (isIngame) Amber.YellowSoft.copy(alpha = 0.22f) else Amber.Line
            val guideLineColor = if (isIngame) Amber.YellowSoft.copy(alpha = 0.20f) else Amber.LineStrong.copy(alpha = 0.28f)
            var x = step
            while (x < size.width) {
                var y = step
                while (y < size.height) {
                    drawCircle(dotColor, radius = 1.4.dp.toPx(), center = Offset(x, y))
                    y += step
                }
                x += step
            }

            drawLine(
                color = guideLineColor,
                start = Offset(size.width / 2f, 0f),
                end = Offset(size.width / 2f, size.height),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = guideLineColor,
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width, size.height / 2f),
                strokeWidth = 1.dp.toPx()
            )
        }

        // 2. Interactive Draggable Buttons on Screen (rendered with shared ControlSkin)
        buttonsList.filter { editorShows(it, viewMode) }.forEach { btn ->
            key(btn.id) {
                val isSelected = btn.id == selectedButtonId
                val btnWidthPx = with(density) { btn.widthDp.dp.toPx() }
                val btnHeightPx = with(density) { btn.heightDp.dp.toPx() }

                val centerX = screenWidthPx * (btn.xPercent / 100f)
                val centerY = screenHeightPx * (btn.yPercent / 100f)

                val leftPx = (centerX - btnWidthPx / 2f).coerceIn(0f, (screenWidthPx - btnWidthPx).coerceAtLeast(0f))
                val topPx = (centerY - btnHeightPx / 2f).coerceIn(0f, (screenHeightPx - btnHeightPx).coerceAtLeast(0f))

                val leftDp = with(density) { leftPx.toDp() }
                val topDp = with(density) { topPx.toDp() }

                val spec = remember(btn) { skinSpecFor(btn) }

                Box(
                    modifier = Modifier
                        .offset(x = leftDp, y = topDp)
                        .size(width = btn.widthDp.dp, height = btn.heightDp.dp)
                        .drawBehind {
                            when {
                                btn.isJoystick -> drawControlJoystick(btn.opacity, Offset.Zero, false, false, isSelected)
                                btn.isScroll -> drawControlScroll(spec, false, isSelected)
                                else -> drawControlButton(spec, false, isSelected)
                            }
                        }
                        .pointerInput(btn.id) {
                            detectTapGestures {
                                manualFlipSide = null
                                selectedButtonId = if (selectedButtonId == btn.id) null else btn.id
                            }
                        }
                        .pointerInput(btn.id, screenWidthPx, screenHeightPx) {
                            detectDragGestures(
                                onDragStart = {
                                    manualFlipSide = null
                                    selectedButtonId = btn.id
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val currentBtn = buttonsList.firstOrNull { it.id == btn.id } ?: return@detectDragGestures
                                    val curCenterX = screenWidthPx * (currentBtn.xPercent / 100f)
                                    val curCenterY = screenHeightPx * (currentBtn.yPercent / 100f)

                                    val halfW = with(density) { currentBtn.widthDp.dp.toPx() } / 2f
                                    val halfH = with(density) { currentBtn.heightDp.dp.toPx() } / 2f
                                    val newCenterX = (curCenterX + dragAmount.x).clampCenter(halfW, screenWidthPx - halfW)
                                    val newCenterY = (curCenterY + dragAmount.y).clampCenter(halfH, screenHeightPx - halfH)

                                    val newXPercent = (newCenterX / screenWidthPx) * 100f
                                    val newYPercent = (newCenterY / screenHeightPx) * 100f

                                    buttonsList = buttonsList.map {
                                        if (it.id == btn.id) it.copy(xPercent = newXPercent, yPercent = newYPercent) else it
                                    }
                                }
                            )
                        }
                )
            }
        }

        // 3. FLOATING CYBER-GLASS TOP TOOLBAR
        if (!uiHidden) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .then(safeSystemPadding)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EditorActionPill(
                    label = if (isIngame) "SELESAI" else "KEMBALI",
                    icon = if (isIngame) Icons.Outlined.Check else Icons.AutoMirrored.Outlined.ArrowBack,
                    filled = isIngame,
                    onClick = {
                        if (isIngame) {
                            ControlLayoutManager.saveButtons(context, buttonsList)
                            Toast.makeText(context, "✓ Layout tombol in-game disimpan!", Toast.LENGTH_SHORT).show()
                        }
                        onNavigateBack()
                    },
                    modifier = Modifier.testTag("editor_back_button")
                )

                if (!isNarrowEditor) {
                    Spacer(modifier = Modifier.weight(1f))
                }

                Row(
                    modifier = Modifier
                        .then(if (isNarrowEditor) Modifier.weight(1f) else Modifier)
                        .horizontalScroll(rememberScrollState())
                        .clip(CircleShape)
                        .background(Amber.Panel, CircleShape)
                        .border(1.dp, Amber.LineStrong, CircleShape)
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EditorToolbarChip(
                        label = "+ TOMBOL",
                        icon = Icons.Outlined.Add,
                        accent = false,
                        danger = false,
                        onClick = {
                            val newBtn = CustomControlButton(
                                id = "btn_" + UUID.randomUUID().toString().take(6),
                                name = "NEW",
                                keyCode = LwjglGlfwKeycode.GLFW_KEY_SPACE,
                                xPercent = 50f,
                                yPercent = 50f,
                                widthDp = 52,
                                heightDp = 48,
                                showMode = if (viewMode == VIEW_ALL) ControlShowMode.ALWAYS else viewMode
                            )
                            buttonsList = buttonsList + newBtn
                            selectedButtonId = newBtn.id
                            manualFlipSide = null
                            keyPickerTargetMode = "MAIN"
                            showKeyPickerForButtonId = newBtn.id
                            Toast.makeText(context, "Tombol baru ditambahkan di tengah layar!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("editor_add_button")
                    )

                    EditorToolbarChip(
                        label = "+ JOYSTICK",
                        icon = Icons.Outlined.ControlCamera,
                        accent = false,
                        danger = false,
                        highlightedBorder = true,
                        onClick = {
                            val newJoy = CustomControlButton(
                                id = "joy_" + UUID.randomUUID().toString().take(6),
                                name = "JOYSTICK",
                                isJoystick = true,
                                xPercent = 20f,
                                yPercent = 70f,
                                widthDp = 130,
                                heightDp = 130,
                                cornerRadiusDp = 65,
                                opacity = 0.85f,
                                showMode = if (viewMode == VIEW_ALL) ControlShowMode.ALWAYS else viewMode
                            )
                            buttonsList = buttonsList + newJoy
                            selectedButtonId = newJoy.id
                            manualFlipSide = null
                            Toast.makeText(context, "Joystick WASD ditambahkan!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("editor_add_joystick")
                    )

                    EditorToolbarChip(
                        label = "RESET",
                        icon = Icons.Outlined.RestartAlt,
                        accent = false,
                        danger = true,
                        onClick = { showResetConfirmDialog = true },
                        modifier = Modifier.testTag("editor_reset_button")
                    )

                    EditorToolbarChip(
                        label = "SIMPAN",
                        icon = Icons.Outlined.Check,
                        accent = true,
                        danger = false,
                        onClick = {
                            ControlLayoutManager.saveButtons(context, buttonsList)
                            Toast.makeText(context, "✓ Layout GUI berhasil disimpan!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("editor_save_button")
                    )
                }

                if (!isNarrowEditor) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        // 4. SMART AUTO-DOCKING CYBER-GLASS INSPECTOR PANEL
        if (selectedButton != null) {
            val shouldDockLeft = manualFlipSide ?: (selectedButton.xPercent > 48f)

            val panelAlignment = when {
                isNarrowEditor -> if (selectedButton.yPercent > 52f) Alignment.TopCenter else Alignment.BottomCenter
                shouldDockLeft -> Alignment.CenterStart
                else -> Alignment.CenterEnd
            }
            val panelModifier = if (isNarrowEditor) {
                Modifier
                    .align(panelAlignment)
                    .then(safeSystemPadding)
                    .padding(horizontal = 12.dp, vertical = 62.dp)
            } else {
                Modifier
                    .align(panelAlignment)
                    .then(safeSystemPadding)
                    .padding(
                        start = if (shouldDockLeft) 16.dp else 0.dp,
                        end = if (shouldDockLeft) 0.dp else 16.dp
                    )
            }

            fun nudgeSelected(dxPercent: Float, dyPercent: Float) {
                buttonsList = buttonsList.map { b ->
                    if (b.id != selectedButton.id) {
                        b
                    } else {
                        val halfW = with(density) { b.widthDp.dp.toPx() } / 2f
                        val halfH = with(density) { b.heightDp.dp.toPx() } / 2f
                        val targetCx = (screenWidthPx * ((b.xPercent + dxPercent) / 100f))
                            .clampCenter(halfW, screenWidthPx - halfW)
                        val targetCy = (screenHeightPx * ((b.yPercent + dyPercent) / 100f))
                            .clampCenter(halfH, screenHeightPx - halfH)
                        b.copy(
                            xPercent = (targetCx / screenWidthPx) * 100f,
                            yPercent = (targetCy / screenHeightPx) * 100f
                        )
                    }
                }
            }

            Box(modifier = panelModifier) {
                val inspectorShape = RoundedCornerShape(20.dp)
                Column(
                    modifier = if (isNarrowEditor) {
                        Modifier
                            .fillMaxWidth()
                            .widthIn(max = 390.dp)
                            .fillMaxHeight(0.48f)
                    } else {
                        Modifier
                            .width(296.dp)
                            .fillMaxHeight(0.82f)
                    }
                        .clip(inspectorShape)
                        .background(Amber.Panel, inspectorShape)
                        .border(1.dp, Amber.LineStrong, inspectorShape)
                        .padding(14.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Inspector Header with Flip-Side & Close (48dp touch targets)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "PENGATURAN TOMBOL",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = Amber.Text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Amber.Fill, CircleShape)
                                    .border(1.dp, Amber.Line, CircleShape)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Posisi: ${selectedButton.xPercent.roundToInt()}% x ${selectedButton.yPercent.roundToInt()}%",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Amber.YellowSoft
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            InspectorIconButton(
                                icon = Icons.Outlined.SwapHoriz,
                                description = "Pindah sisi panel",
                                tint = Amber.YellowSoft,
                                containerColor = Amber.Fill,
                                borderColor = Amber.LineStrong,
                                onClick = { manualFlipSide = !shouldDockLeft }
                            )
                            InspectorIconButton(
                                icon = Icons.Outlined.Close,
                                description = "Tutup pengaturan tombol",
                                tint = Amber.DangerSoft,
                                containerColor = Amber.DangerTint,
                                borderColor = Amber.Danger.copy(alpha = 0.5f),
                                onClick = { selectedButtonId = null }
                            )
                        }
                    }

                    // Fine Position Nudge Controls (1% steps)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AmberUi.ObsidianSlate.copy(alpha = 0.65f))
                            .border(1.dp, Amber.Line, RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Geser halus",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Amber.TextDim
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            NudgeButton("X-", "Geser kiri") { nudgeSelected(-1f, 0f) }
                            NudgeButton("X+", "Geser kanan") { nudgeSelected(1f, 0f) }
                            NudgeButton("Y-", "Geser atas") { nudgeSelected(0f, -1f) }
                            NudgeButton("Y+", "Geser bawah") { nudgeSelected(0f, 1f) }
                        }
                    }

                    HorizontalDivider(color = Amber.Line, thickness = 1.dp)

                    // 1. Label Name Field
                    Text("Label Teks Tombol:", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Amber.TextDim)
                    if (selectedButton.isSystem) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(AmberUi.ObsidianSlate.copy(alpha = 0.6f))
                                .border(1.dp, Amber.Line, RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(selectedButton.name, color = Amber.Text, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Outlined.Lock, contentDescription = null, tint = Amber.YellowSoft, modifier = Modifier.size(13.dp))
                                Text("Tetap", color = Amber.YellowSoft, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Amber.Ink)
                                .border(1.dp, Amber.LineStrong, RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (selectedButton.name.isEmpty()) {
                                Text("Label tombol...", color = Amber.TextFaint, fontSize = 13.sp)
                            }
                            BasicTextField(
                                value = selectedButton.name,
                                onValueChange = { newName ->
                                    buttonsList = buttonsList.map { if (it.id == selectedButton.id) it.copy(name = newName) else it }
                                },
                                textStyle = TextStyle(color = Amber.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                cursorBrush = SolidColor(Amber.Yellow),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    if (selectedButton.isJoystick) {
                        Text("Tipe Kontrol:", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Amber.TextDim)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Amber.Fill)
                                .border(1.dp, Amber.LineStrong, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Outlined.Gamepad, contentDescription = null, tint = Amber.YellowSoft, modifier = Modifier.size(20.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("Virtual Analog Joystick (WASD)", color = Amber.YellowSoft, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                Text("Analog 8-arah halus menggerakkan karakter menggantikan tombol W, A, S, D.", color = Amber.TextDim, fontSize = 9.5.sp, lineHeight = 13.sp)
                            }
                        }

                        EditorAdjustSlider(
                            label = "Ukuran Joystick (Diameter):",
                            valueText = "${selectedButton.widthDp} dp",
                            value = selectedButton.widthDp.toFloat(),
                            range = ControlLimits.MIN_JOYSTICK_DP.toFloat()..ControlLimits.MAX_JOYSTICK_DP.toFloat(),
                            canDecrease = selectedButton.widthDp > ControlLimits.MIN_JOYSTICK_DP,
                            canIncrease = selectedButton.widthDp < ControlLimits.MAX_JOYSTICK_DP,
                            onStep = { delta ->
                                val next = (selectedButton.widthDp + delta * 4)
                                    .coerceIn(ControlLimits.MIN_JOYSTICK_DP, ControlLimits.MAX_JOYSTICK_DP)
                                buttonsList = buttonsList.map {
                                    if (it.id == selectedButton.id) it.copy(widthDp = next, heightDp = next, cornerRadiusDp = next / 2) else it
                                }
                            },
                            onValueChange = { newD ->
                                val d = newD.roundToInt()
                                buttonsList = buttonsList.map {
                                    if (it.id == selectedButton.id) it.copy(widthDp = d, heightDp = d, cornerRadiusDp = d / 2) else it
                                }
                            }
                        )

                        EditorAdjustSlider(
                            label = "Transparansi (Opacity):",
                            valueText = "${(selectedButton.opacity * 100).roundToInt()}%",
                            value = selectedButton.opacity,
                            range = 0.15f..1.0f,
                            canDecrease = selectedButton.opacity > 0.16f,
                            canIncrease = selectedButton.opacity < 0.99f,
                            onStep = { delta ->
                                val next = ((selectedButton.opacity + delta * 0.05f) * 100).roundToInt() / 100f
                                buttonsList = buttonsList.map {
                                    if (it.id == selectedButton.id) it.copy(opacity = next.coerceIn(0.15f, 1.0f)) else it
                                }
                            },
                            onValueChange = { newOp ->
                                buttonsList = buttonsList.map {
                                    if (it.id == selectedButton.id) it.copy(opacity = (newOp * 100).roundToInt() / 100f) else it
                                }
                            }
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .clickable {
                                    val on = !selectedButton.autoSprint
                                    buttonsList = buttonsList.map { if (it.id == selectedButton.id) it.copy(autoSprint = on) else it }
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Lari otomatis:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Amber.Text)
                                Text("Dorong sampai tepi lingkaran untuk berlari. Matikan bila ingin berlari lewat tombol CTRL sendiri.", fontSize = 9.sp, color = Amber.TextDim, lineHeight = 12.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = selectedButton.autoSprint,
                                onCheckedChange = { on ->
                                    buttonsList = buttonsList.map { if (it.id == selectedButton.id) it.copy(autoSprint = on) else it }
                                },
                                colors = editorSwitchColors()
                            )
                        }
                    } else {
                        // 2. Mapped Keycode / Input
                        Text("Tombol / Aksi Input:", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Amber.TextDim)
                        if (selectedButton.isSystem) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AmberUi.ObsidianSlate.copy(alpha = 0.6f))
                                    .border(1.dp, Amber.Line, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                val actionDesc = when (selectedButton.systemAction) {
                                    "FPS" -> "Indikator FPS In-Game"
                                    "KEYBOARD" -> "Toggle Keyboard Layar"
                                    "HIDE_GUI" -> "Sembunyikan/Tampilkan GUI"
                                    "MENU", "CLOSE" -> "Buka Menu (pengaturan dan keluar)"
                                    else -> "Aksi Sistem Launcher"
                                }
                                Text(actionDesc, color = Amber.YellowSoft, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        } else {
                            val mappedKeyName = remember(selectedButton) {
                                if (selectedButton.isScroll) {
                                    "Scroll Wheel (Slide Naik/Turun)"
                                } else if (selectedButton.isMouseButton) {
                                    when (selectedButton.mouseButton) {
                                        LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT -> "Mouse Kiri (Attack)"
                                        LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT -> "Mouse Kanan (Use)"
                                        LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_MIDDLE -> "Mouse Tengah (Pick)"
                                        else -> "Mouse Btn ${selectedButton.mouseButton}"
                                    }
                                } else {
                                    KeycodeCatalog.ALL_KEYS.firstOrNull { it.keyCode == selectedButton.keyCode }?.displayName
                                        ?: "Key Code ${selectedButton.keyCode}"
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 46.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Amber.Ink)
                                    .border(1.dp, Amber.LineStrong, RoundedCornerShape(10.dp))
                                    .clickable {
                                        keyPickerTargetMode = "MAIN"
                                        showKeyPickerForButtonId = selectedButton.id
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = mappedKeyName,
                                    color = Amber.Text,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Ubah", color = Amber.YellowSoft, fontWeight = FontWeight.Black, fontSize = 10.5.sp)
                                    Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = Amber.YellowSoft, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        // 3. Width Slider
                        EditorAdjustSlider(
                            label = "Lebar (Width):",
                            valueText = "${selectedButton.widthDp} dp",
                            value = selectedButton.widthDp.toFloat(),
                            range = ControlLimits.MIN_SIZE_DP.toFloat()..ControlLimits.MAX_WIDTH_DP.toFloat(),
                            canDecrease = selectedButton.widthDp > ControlLimits.MIN_SIZE_DP,
                            canIncrease = selectedButton.widthDp < ControlLimits.MAX_WIDTH_DP,
                            onStep = { delta ->
                                val next = (selectedButton.widthDp + delta * 2)
                                    .coerceIn(ControlLimits.MIN_SIZE_DP, ControlLimits.MAX_WIDTH_DP)
                                buttonsList = buttonsList.map { if (it.id == selectedButton.id) it.copy(widthDp = next) else it }
                            },
                            onValueChange = { newW ->
                                buttonsList = buttonsList.map { if (it.id == selectedButton.id) it.copy(widthDp = newW.roundToInt()) else it }
                            }
                        )

                        // 4. Height Slider
                        EditorAdjustSlider(
                            label = "Tinggi (Height):",
                            valueText = "${selectedButton.heightDp} dp",
                            value = selectedButton.heightDp.toFloat(),
                            range = ControlLimits.MIN_SIZE_DP.toFloat()..ControlLimits.MAX_HEIGHT_DP.toFloat(),
                            canDecrease = selectedButton.heightDp > ControlLimits.MIN_SIZE_DP,
                            canIncrease = selectedButton.heightDp < ControlLimits.MAX_HEIGHT_DP,
                            onStep = { delta ->
                                val next = (selectedButton.heightDp + delta * 2)
                                    .coerceIn(ControlLimits.MIN_SIZE_DP, ControlLimits.MAX_HEIGHT_DP)
                                buttonsList = buttonsList.map { if (it.id == selectedButton.id) it.copy(heightDp = next) else it }
                            },
                            onValueChange = { newH ->
                                buttonsList = buttonsList.map { if (it.id == selectedButton.id) it.copy(heightDp = newH.roundToInt()) else it }
                            }
                        )

                        // 5. Opacity Slider
                        EditorAdjustSlider(
                            label = "Transparansi (Opacity):",
                            valueText = "${(selectedButton.opacity * 100).roundToInt()}%",
                            value = selectedButton.opacity,
                            range = 0.15f..1.0f,
                            canDecrease = selectedButton.opacity > 0.16f,
                            canIncrease = selectedButton.opacity < 0.99f,
                            onStep = { delta ->
                                val next = ((selectedButton.opacity + delta * 0.05f) * 100).roundToInt() / 100f
                                buttonsList = buttonsList.map {
                                    if (it.id == selectedButton.id) it.copy(opacity = next.coerceIn(0.15f, 1.0f)) else it
                                }
                            },
                            onValueChange = { newOp ->
                                buttonsList = buttonsList.map {
                                    if (it.id == selectedButton.id) it.copy(opacity = (newOp * 100).roundToInt() / 100f) else it
                                }
                            }
                        )

                        // 6. Corner Radius Slider
                        EditorAdjustSlider(
                            label = "Kebulatan Sudut:",
                            valueText = "${selectedButton.cornerRadiusDp} dp",
                            value = selectedButton.cornerRadiusDp.toFloat(),
                            range = 0f..ControlLimits.MAX_CORNER_DP.toFloat(),
                            canDecrease = selectedButton.cornerRadiusDp > 0,
                            canIncrease = selectedButton.cornerRadiusDp < ControlLimits.MAX_CORNER_DP,
                            onStep = { delta ->
                                val next = (selectedButton.cornerRadiusDp + delta)
                                    .coerceIn(0, ControlLimits.MAX_CORNER_DP)
                                buttonsList = buttonsList.map { if (it.id == selectedButton.id) it.copy(cornerRadiusDp = next) else it }
                            },
                            onValueChange = { newR ->
                                buttonsList = buttonsList.map { if (it.id == selectedButton.id) it.copy(cornerRadiusDp = newR.roundToInt()) else it }
                            }
                        )
                    }

                    // 6b. Show Mode selector
                    if (!selectedButton.isSystem) {
                        val currentShow = if (ControlShowMode.isValid(selectedButton.showMode)) selectedButton.showMode else ControlShowMode.ALWAYS
                        Text("Tampil saat:", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Amber.TextDim)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            listOf(
                                ControlShowMode.ALWAYS to "Selalu",
                                ControlShowMode.GAME to "Saat main",
                                ControlShowMode.MENU to "Saat menu"
                            ).forEach { (modeKey, modeLabel) ->
                                val isChosen = currentShow == modeKey
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 38.dp)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(if (isChosen) AmberUi.Accent else Amber.Ink)
                                        .border(
                                            1.dp,
                                            if (isChosen) AmberUi.AccentEdge else Amber.Line,
                                            RoundedCornerShape(9.dp)
                                        )
                                        .clickable {
                                            buttonsList = buttonsList.map {
                                                if (it.id == selectedButton.id) it.copy(showMode = modeKey) else it
                                            }
                                            if (viewMode != VIEW_ALL && !ControlShowMode.visible(modeKey, viewMode == ControlShowMode.MENU)) {
                                                viewMode = modeKey
                                            }
                                        }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = modeLabel,
                                        fontSize = 9.5.sp,
                                        fontWeight = if (isChosen) FontWeight.Black else FontWeight.SemiBold,
                                        color = if (isChosen) Color.White else Amber.TextDim,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        Text("Menu = inventori, chat, dan layar lain di dalam game.", fontSize = 9.sp, color = Amber.TextFaint)
                    }

                    // 7. Toggle Mode Switch
                    if (!selectedButton.isSystem && !selectedButton.isJoystick) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .clickable {
                                    val isToggled = !selectedButton.isToggle
                                    buttonsList = buttonsList.map { if (it.id == selectedButton.id) it.copy(isToggle = isToggled) else it }
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Mode Toggle:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Amber.Text)
                                Text("Sekali tap untuk kunci", fontSize = 9.sp, color = Amber.TextDim)
                            }
                            Switch(
                                checked = selectedButton.isToggle,
                                onCheckedChange = { isToggled ->
                                    buttonsList = buttonsList.map { if (it.id == selectedButton.id) it.copy(isToggle = isToggled) else it }
                                },
                                colors = editorSwitchColors()
                            )
                        }
                    }

                    // 8. Macro Studio Card
                    if (!selectedButton.isSystem && !selectedButton.isScroll && !selectedButton.isJoystick) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(AmberUi.ObsidianSlate.copy(alpha = 0.72f))
                                .border(
                                    1.dp,
                                    if (selectedButton.isMacro) Amber.Yellow else Amber.Line,
                                    RoundedCornerShape(14.dp)
                                )
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 44.dp)
                                    .clickable {
                                        val isMacroEnabled = !selectedButton.isMacro
                                        buttonsList = buttonsList.map {
                                            if (it.id == selectedButton.id) it.copy(isMacro = isMacroEnabled) else it
                                        }
                                    },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.Bolt,
                                            contentDescription = null,
                                            tint = if (selectedButton.isMacro) Amber.YellowSoft else Amber.Text,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "FITUR MAKRO",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (selectedButton.isMacro) Amber.YellowSoft else Amber.Text
                                        )
                                        if (selectedButton.isMacro) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(Amber.YellowTint, CircleShape)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("AKTIF", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Amber.YellowSoft)
                                            }
                                        }
                                    }
                                    Text("Jadikan tombol ini sebagai makro otomatis", fontSize = 9.sp, color = Amber.TextDim)
                                }
                                Switch(
                                    checked = selectedButton.isMacro,
                                    onCheckedChange = { isMacroEnabled ->
                                        buttonsList = buttonsList.map {
                                            if (it.id == selectedButton.id) it.copy(isMacro = isMacroEnabled) else it
                                        }
                                    },
                                    colors = editorSwitchColors()
                                )
                            }

                            if (selectedButton.isMacro) {
                                HorizontalDivider(color = Amber.Line, thickness = 1.dp)

                                Text("Pilih Mode Makro:", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Amber.TextDim)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val types = listOf(
                                        Triple("COMMAND", "Chat", Icons.Outlined.Keyboard),
                                        Triple("COMBO", "Kombinasi", Icons.Outlined.Link),
                                        Triple("TURBO", "Turbo", Icons.Outlined.TouchApp)
                                    )
                                    types.forEach { (typeKey, typeLabel, icon) ->
                                        val isChosen = selectedButton.macroType == typeKey
                                        Row(
                                            modifier = Modifier
                                                .weight(1f)
                                                .defaultMinSize(minHeight = 36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isChosen) AmberUi.Accent else Amber.Ink)
                                                .border(
                                                    1.dp,
                                                    if (isChosen) AmberUi.AccentEdge else Amber.Line,
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .clickable {
                                                    buttonsList = buttonsList.map {
                                                        if (it.id == selectedButton.id) it.copy(macroType = typeKey) else it
                                                    }
                                                }
                                                .padding(horizontal = 4.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = if (isChosen) Color.White else Amber.TextDim,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = typeLabel,
                                                fontSize = 8.5.sp,
                                                fontWeight = if (isChosen) FontWeight.Black else FontWeight.SemiBold,
                                                color = if (isChosen) Color.White else Amber.TextDim,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }

                                // Mode 1: COMMAND
                                if (selectedButton.macroType == "COMMAND") {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("Teks / Perintah yang diketik otomatis:", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Amber.Text)
                                        BasicTextField(
                                            value = selectedButton.macroCommand,
                                            onValueChange = { newCmd ->
                                                buttonsList = buttonsList.map {
                                                    if (it.id == selectedButton.id) it.copy(macroCommand = newCmd) else it
                                                }
                                            },
                                            textStyle = TextStyle(color = Amber.Text, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold),
                                            cursorBrush = SolidColor(Amber.YellowSoft),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .defaultMinSize(minHeight = 40.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Amber.Ink)
                                                .border(1.dp, Amber.LineStrong, RoundedCornerShape(8.dp))
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            decorationBox = { innerTextField ->
                                                if (selectedButton.macroCommand.isEmpty()) {
                                                    Text(
                                                        text = "Misal: /gamemode creative atau /home",
                                                        color = Amber.TextFaint,
                                                        fontSize = 10.sp
                                                    )
                                                }
                                                innerTextField()
                                            }
                                        )

                                        Text("Template Cepat:", fontSize = 9.sp, color = Amber.TextDim)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            listOf("/gamemode c", "/gamemode s", "/spawn", "/home").forEach { sample ->
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(Amber.Ink)
                                                        .border(1.dp, Amber.Line, RoundedCornerShape(6.dp))
                                                        .clickable {
                                                            buttonsList = buttonsList.map {
                                                                if (it.id == selectedButton.id) it.copy(macroCommand = sample) else it
                                                            }
                                                        }
                                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                                ) {
                                                    Text(sample, fontSize = 9.sp, color = Amber.YellowSoft, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }

                                // Mode 2: COMBO
                                if (selectedButton.macroType == "COMBO") {
                                    val comboKeysList = remember(selectedButton.macroComboKeys, selectedButton.macroComboKey) {
                                        if (selectedButton.macroComboKeys.isNotEmpty()) {
                                            selectedButton.macroComboKeys
                                        } else if (selectedButton.macroComboKey != 0) {
                                            listOf(selectedButton.macroComboKey)
                                        } else {
                                            emptyList()
                                        }
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Urutan Key Kombinasi:", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Amber.Text)
                                            if (comboKeysList.isNotEmpty()) {
                                                Text(
                                                    text = "Hapus Semua",
                                                    fontSize = 9.sp,
                                                    color = Amber.DangerSoft,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .clickable {
                                                            buttonsList = buttonsList.map {
                                                                if (it.id == selectedButton.id) it.copy(macroComboKeys = emptyList(), macroComboKey = 0) else it
                                                            }
                                                        }
                                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                                )
                                            }
                                        }

                                        if (comboKeysList.isEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Amber.Ink)
                                                    .border(1.dp, Amber.Line, RoundedCornerShape(8.dp))
                                                    .padding(vertical = 10.dp, horizontal = 12.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "Belum ada key kombinasi. Tambahkan key di bawah.",
                                                    color = Amber.YellowSoft,
                                                    fontSize = 9.5.sp
                                                )
                                            }
                                        } else {
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                comboKeysList.forEachIndexed { index, keyCode ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(Amber.Ink)
                                                            .border(1.dp, Amber.Line, RoundedCornerShape(8.dp))
                                                            .padding(start = 8.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(4.dp))
                                                                    .background(Amber.YellowTint)
                                                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                                                            ) {
                                                                Text("#${index + 1}", fontSize = 8.5.sp, fontWeight = FontWeight.Black, color = Amber.YellowSoft)
                                                            }
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text(
                                                                text = KeycodeCatalog.getKeyName(keyCode),
                                                                color = Amber.Text,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 10.5.sp,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                        Box(
                                                            modifier = Modifier
                                                                .size(40.dp)
                                                                .clip(CircleShape)
                                                                .clickable {
                                                                    val updated = comboKeysList.toMutableList().apply { removeAt(index) }
                                                                    buttonsList = buttonsList.map {
                                                                        if (it.id == selectedButton.id) {
                                                                            it.copy(macroComboKeys = updated, macroComboKey = updated.firstOrNull() ?: 0)
                                                                        } else it
                                                                    }
                                                                },
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(24.dp)
                                                                    .background(Amber.DangerTint, CircleShape)
                                                                    .border(1.dp, Amber.Danger.copy(alpha = 0.5f), CircleShape),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Outlined.Close,
                                                                    contentDescription = "Hapus key",
                                                                    tint = Amber.DangerSoft,
                                                                    modifier = Modifier.size(12.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .defaultMinSize(minHeight = 42.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Amber.Ink)
                                                .border(1.dp, Amber.LineStrong, RoundedCornerShape(8.dp))
                                                .clickable {
                                                    keyPickerTargetMode = "COMBO"
                                                    showKeyPickerForButtonId = selectedButton.id
                                                }
                                                .padding(vertical = 8.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Outlined.Add, contentDescription = null, tint = Amber.YellowSoft, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Tambah Key ke Kombinasi", fontSize = 10.5.sp, color = Amber.Text, fontWeight = FontWeight.Bold)
                                        }

                                        Text("Tambah Cepat:", fontSize = 9.sp, color = Amber.TextDim)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            listOf(
                                                "F3" to LwjglGlfwKeycode.GLFW_KEY_F3,
                                                "B" to LwjglGlfwKeycode.GLFW_KEY_B,
                                                "SHIFT" to LwjglGlfwKeycode.GLFW_KEY_LEFT_SHIFT,
                                                "CTRL" to LwjglGlfwKeycode.GLFW_KEY_LEFT_CONTROL,
                                                "ALT" to LwjglGlfwKeycode.GLFW_KEY_LEFT_ALT,
                                                "Q" to LwjglGlfwKeycode.GLFW_KEY_Q
                                            ).forEach { (label, code) ->
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(Amber.Ink)
                                                        .border(1.dp, Amber.Line, RoundedCornerShape(6.dp))
                                                        .clickable {
                                                            val updated = comboKeysList + code
                                                            buttonsList = buttonsList.map {
                                                                if (it.id == selectedButton.id) {
                                                                    it.copy(macroComboKeys = updated, macroComboKey = updated.firstOrNull() ?: 0)
                                                                } else it
                                                            }
                                                        }
                                                        .padding(horizontal = 7.dp, vertical = 4.dp)
                                                ) {
                                                    Text("+ $label", fontSize = 9.sp, color = Amber.YellowSoft, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                        Text("Dieksekusi berurutan dari atas (#1) ke bawah saat ditekan.", fontSize = 8.5.sp, color = Amber.TextDim)
                                    }
                                }

                                // Mode 3: TURBO
                                if (selectedButton.macroType == "TURBO") {
                                    val cps = (1000f / selectedButton.macroTurboIntervalMs.coerceAtLeast(1L)).roundToInt()
                                    EditorAdjustSlider(
                                        label = "Interval Auto-Click:",
                                        valueText = "${selectedButton.macroTurboIntervalMs} ms (~$cps CPS)",
                                        value = selectedButton.macroTurboIntervalMs.toFloat(),
                                        range = 40f..500f,
                                        canDecrease = selectedButton.macroTurboIntervalMs > 40L,
                                        canIncrease = selectedButton.macroTurboIntervalMs < 500L,
                                        onStep = { delta ->
                                            val next = (selectedButton.macroTurboIntervalMs + delta * 10L).coerceIn(40L, 500L)
                                            buttonsList = buttonsList.map {
                                                if (it.id == selectedButton.id) it.copy(macroTurboIntervalMs = next) else it
                                            }
                                        },
                                        onValueChange = { newInterval ->
                                            buttonsList = buttonsList.map {
                                                if (it.id == selectedButton.id) it.copy(macroTurboIntervalMs = newInterval.toLong()) else it
                                            }
                                        }
                                    )
                                    Text("Tombol akan menekan & melepas berulang-ulang sangat cepat secara otomatis saat ditekan.", fontSize = 8.5.sp, color = Amber.TextDim)
                                }
                            }
                        }
                    }

                    // 9. Delete Button or System Locked Banner
                    if (!selectedButton.isSystem) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Amber.DangerTint)
                                .border(1.dp, Amber.Danger.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                                .clickable {
                                    buttonsList = buttonsList.filter { it.id != selectedButton.id }
                                    selectedButtonId = null
                                    Toast.makeText(context, "Tombol dihapus", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = Amber.DangerSoft, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("HAPUS TOMBOL", color = Amber.DangerSoft, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AmberUi.ObsidianSlate.copy(alpha = 0.6f))
                                .border(1.dp, Amber.Line, RoundedCornerShape(12.dp))
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Lock, contentDescription = null, tint = Amber.TextDim, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Tombol sistem tidak dapat dihapus",
                                color = Amber.TextDim,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp
                            )
                        }
                    }
                }
            }
        }

        // 5. BOTTOM VIEW-MODE BAR & UI HIDE TOGGLE
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .then(safeSystemPadding)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!uiHidden) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isIngame) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Amber.Panel, CircleShape)
                                .border(1.dp, Amber.Yellow.copy(alpha = 0.5f), CircleShape)
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .background(Amber.Yellow, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(7.dp))
                                Text(
                                    text = "MODE IN-GAME · Geser tombol sesuai HUD game lalu klik SELESAI",
                                    color = Amber.YellowSoft,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .clip(CircleShape)
                            .background(Amber.Panel, CircleShape)
                            .border(1.dp, Amber.LineStrong, CircleShape)
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            VIEW_ALL to "Semua tombol",
                            ControlShowMode.GAME to "Saat main",
                            ControlShowMode.MENU to "Saat menu"
                        ).forEach { (viewKey, viewLabel) ->
                            val chosen = viewMode == viewKey
                            Box(
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 36.dp)
                                    .clip(CircleShape)
                                    .background(if (chosen) AmberUi.Accent else Color.Transparent, CircleShape)
                                    .border(
                                        1.dp,
                                        if (chosen) AmberUi.AccentEdge else Color.Transparent,
                                        CircleShape
                                    )
                                    .clickable {
                                        viewMode = viewKey
                                        val sel = buttonsList.firstOrNull { it.id == selectedButtonId }
                                        if (sel != null && !editorShows(sel, viewKey)) selectedButtonId = null
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    viewLabel,
                                    fontWeight = if (chosen) FontWeight.Black else FontWeight.SemiBold,
                                    fontSize = 10.5.sp,
                                    color = if (chosen) Color.White else Amber.TextDim,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (uiHidden) AmberUi.Accent else Amber.Panel, CircleShape)
                    .border(
                        1.dp,
                        if (uiHidden) AmberUi.AccentEdge else Amber.LineStrong,
                        CircleShape
                    )
                    .clickable { uiHidden = !uiHidden }
                    .testTag("editor_toggle_ui_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (uiHidden) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                    contentDescription = if (uiHidden) "Tampilkan UI editor" else "Sembunyikan UI editor",
                    tint = if (uiHidden) Color.White else Amber.YellowSoft,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    // Keycode Picker Dialog
    showKeyPickerForButtonId?.let { targetId ->
        val isComboMode = keyPickerTargetMode == "COMBO"
        val targetButton = buttonsList.firstOrNull { it.id == targetId }

        NuxKeyPickerDialog(
            comboMode = isComboMode,
            onDismiss = { showKeyPickerForButtonId = null },
            isCurrent = { k ->
                if (isComboMode || targetButton == null) false
                else if (targetButton.isScroll) k.isScroll
                else if (targetButton.isMouseButton) k.isMouseButton && k.mouseButton == targetButton.mouseButton
                else !k.isScroll && !k.isMouseButton && k.keyCode == targetButton.keyCode
            },
            onPick = { keyOpt ->
                buttonsList = buttonsList.map {
                    if (it.id == targetId) {
                        if (isComboMode) {
                            val existing = it.macroComboKeys.ifEmpty {
                                if (it.macroComboKey != 0) listOf(it.macroComboKey) else emptyList()
                            }
                            val updated = existing + keyOpt.keyCode
                            it.copy(macroComboKeys = updated, macroComboKey = updated.firstOrNull() ?: 0)
                        } else {
                            it.copy(
                                keyCode = keyOpt.keyCode,
                                isMouseButton = keyOpt.isMouseButton,
                                mouseButton = keyOpt.mouseButton,
                                isScroll = keyOpt.isScroll,
                                name = if (keyOpt.isScroll) "SCROLL"
                                else if (it.name == "NEW" || it.name == "BTN" || it.name == "SCROLL") {
                                    keyOpt.displayName.substringBefore(" ").take(6)
                                } else it.name
                            )
                        }
                    } else it
                }
                showKeyPickerForButtonId = null
            }
        )
    }

    // Reset Confirmation Dialog
    if (showResetConfirmDialog) {
        Dialog(
            onDismissRequest = { showResetConfirmDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            GamepadWindowHook()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.72f))
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                NuxCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 380.dp),
                    backgroundColor = Amber.Panel,
                    borderColor = Amber.LineStrong,
                    shadowOffset = 6.dp,
                    cornerRadius = 20.dp
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Reset Layout GUI?", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Amber.Text)
                        Text(
                            "Semua posisi tombol dan kustomisasi akan dikembalikan ke tata letak awal standar NovaCraft.",
                            fontSize = 12.sp,
                            color = Amber.TextDim,
                            lineHeight = 17.sp
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NuxButton(
                                onClick = { showResetConfirmDialog = false },
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp),
                                backgroundColor = AmberUi.ObsidianSlate,
                                borderColor = Amber.LineStrong,
                                contentColor = Amber.Text,
                                shadowOffset = 2.dp
                            ) {
                                Text("BATAL", fontWeight = FontWeight.Black, fontSize = 11.5.sp, color = Amber.Text)
                            }

                            NuxButton(
                                onClick = {
                                    buttonsList = ControlLayoutManager.getDefaultButtons()
                                    selectedButtonId = null
                                    showResetConfirmDialog = false
                                    Toast.makeText(context, "Layout direset ke standar", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp),
                                backgroundColor = Amber.Danger,
                                borderColor = Amber.DangerSoft,
                                contentColor = Amber.Ink,
                                shadowOffset = 2.dp
                            ) {
                                Text("RESET", fontWeight = FontWeight.Black, fontSize = 11.5.sp, color = Amber.Ink)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorActionPill(
    label: String,
    icon: ImageVector,
    filled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp)
            .clip(CircleShape)
            .background(if (filled) AmberUi.Accent else Amber.Panel, CircleShape)
            .border(
                1.dp,
                if (filled) AmberUi.AccentEdge else Amber.LineStrong,
                CircleShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 11.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun EditorToolbarChip(
    label: String,
    icon: ImageVector,
    accent: Boolean,
    danger: Boolean,
    highlightedBorder: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = when {
        accent -> AmberUi.Accent
        danger -> Amber.DangerTint
        else -> AmberUi.ObsidianSlate
    }
    val border = when {
        accent -> AmberUi.AccentEdge
        danger -> Amber.Danger.copy(alpha = 0.55f)
        highlightedBorder -> Amber.Yellow.copy(alpha = 0.7f)
        else -> Amber.Line
    }
    val tint = when {
        accent -> Color.White
        danger -> Amber.DangerSoft
        highlightedBorder -> Amber.YellowSoft
        else -> Amber.Text
    }
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 38.dp)
            .clip(CircleShape)
            .background(bg, CircleShape)
            .border(1.dp, border, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
        Text(label, color = tint, fontWeight = FontWeight.Black, fontSize = 11.sp, maxLines = 1)
    }
}

@Composable
private fun InspectorIconButton(
    icon: ImageVector,
    description: String,
    tint: Color,
    containerColor: Color,
    borderColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(containerColor, CircleShape)
                .border(1.dp, borderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = description, tint = tint, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun NudgeButton(
    label: String,
    description: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 36.dp, minHeight = 36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Amber.Ink)
            .border(1.dp, Amber.LineStrong, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Black, color = Amber.YellowSoft)
    }
}

@Composable
private fun EditorAdjustSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    canDecrease: Boolean,
    canIncrease: Boolean,
    onStep: (Int) -> Unit,
    onValueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Amber.TextDim)
            Text(valueText, fontSize = 10.5.sp, fontWeight = FontWeight.Black, color = Amber.YellowSoft)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            EditorStepCircle(
                icon = Icons.Outlined.Remove,
                description = "Kurangi $label",
                enabled = canDecrease,
                onClick = { onStep(-1) }
            )
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = range,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = AmberUi.AccentSoft,
                    inactiveTrackColor = Amber.LineStrong
                )
            )
            EditorStepCircle(
                icon = Icons.Outlined.Add,
                description = "Tambah $label",
                enabled = canIncrease,
                onClick = { onStep(1) }
            )
        }
    }
}

@Composable
private fun EditorStepCircle(
    icon: ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Amber.Ink, CircleShape)
            .border(1.dp, if (enabled) Amber.LineStrong else Amber.Line, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (enabled) Amber.Text else Amber.TextFaint.copy(alpha = 0.4f),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun editorSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = AmberUi.Accent,
    checkedBorderColor = AmberUi.AccentEdge,
    uncheckedThumbColor = Amber.TextDim,
    uncheckedTrackColor = AmberUi.ObsidianSlate,
    uncheckedBorderColor = Amber.LineStrong
)

// ----------------------------------------------------------------------------
// Section: ui/screens/DashboardScreen.kt
// ----------------------------------------------------------------------------

@Composable
fun DashboardScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentTab by remember { mutableStateOf("home") }
    // Jenis addon yang dibuka di halaman Mod dan tab tujuan tombol kembali
    // (dari Home = Home, dari layar kelola konten = layar kelola konten).
    var modsInitialType by remember { mutableStateOf("mods") }
    var modsBackTab by remember { mutableStateOf("home") }

    BackHandler(enabled = currentTab != "home") {
        currentTab = when (currentTab) {
            "mods" -> modsBackTab
            "gui_editor" -> "settings"
            else -> "home"
        }
    }

    // Controller: LB/RB berpindah antar tab (Home, Akun, Mod, Pengaturan).
    DisposableEffect(Unit) {
        val ids = NUX_NAV_ITEMS.map { it.id }
        val listener: (Int) -> Unit = { step ->
            val index = ids.indexOf(currentTab)
            if (index >= 0) currentTab = ids[(index + step + ids.size) % ids.size]
        }
        com.aistudio.novacraft.launcher.core.controls.GamepadNav.bumperListener = listener
        onDispose {
            if (com.aistudio.novacraft.launcher.core.controls.GamepadNav.bumperListener === listener) {
                com.aistudio.novacraft.launcher.core.controls.GamepadNav.bumperListener = null
            }
        }
    }

    val activeCrash by CrashManager.activeCrash.collectAsState()

    val instances by InstanceManager.instances.collectAsState()
    val selectedInstance by InstanceManager.selectedInstance.collectAsState()
    val currentAccount by AccountManager.currentAccount.collectAsState()
    val launcherSettings by SettingsManager.settings.collectAsState()

    LaunchedEffect(selectedInstance?.id, selectedInstance?.mcVersion) {
        val inst = selectedInstance ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            JavaRuntimeManager.configureJavaForInstance(context, inst, persistChanges = true)
        }
    }

    var showEditInstanceDialog by remember { mutableStateOf(false) }
    var showInstancePicker by remember { mutableStateOf(false) }
    var instanceToDelete by remember { mutableStateOf<Instance?>(null) }
    var pendingLaunchInstance by remember { mutableStateOf<Instance?>(null) }
    var unsupportedRendererInfo by remember { mutableStateOf<NuxRendererInfo?>(null) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showNewsDialog by remember { mutableStateOf(false) }
    var newsInitialVersion by remember { mutableStateOf<String?>(null) }
    val pendingImport by NovaUnifiedFileImportEngine.pendingImport.collectAsState()

    // Download progress state
    var isDownloading by remember { mutableStateOf(false) }
    var downloadTargetName by remember { mutableStateOf("") }
    // Job unduhan yang sedang jalan; dipegang supaya tombol Batal di dialog bisa menghentikannya.
    var downloadJob by remember { mutableStateOf<Job?>(null) }
    // Kemajuan unduhan disimpan di objek tersendiri yang hanya dibaca dialog,
    // sehingga tiap kemajuan tidak menggambar ulang seluruh layar Home.
    val dl = remember { DownloadUiState() }

    val downloader = remember { MinecraftDownloader(context) }

    // Latar belakang Home yang bisa diganti (foto, GIF, atau video dari galeri). Untuk video,
    // bgFile berisi satu bingkai diam (poster) dan videonya disimpan terpisah di bgVideoFile.
    val bgFile = remember { File(context.filesDir, BG_FILE_NAME) }
    val bgVideoFile = remember { HomeVideo.file(context) }
    var hasCustomBg by remember { mutableStateOf(bgFile.exists()) }
    var hasVideoBg by remember { mutableStateOf(bgFile.exists() && bgVideoFile.exists()) }
    var bgVersion by remember { mutableIntStateOf(0) }
    var showBgDialog by remember { mutableStateOf(false) }

    // Animasi latar (GIF/video) boleh jalan atau tidak, plus suara video dan cahaya ambient.
    val powerSave = rememberPowerSaveMode()
    val animateBackground = launcherSettings.homeBgAnimate &&
        !(launcherSettings.homeBgPauseOnPowerSave && powerSave)
    val videoVolume = if (launcherSettings.homeBgSound) {
        val p = launcherSettings.homeBgVolume.coerceIn(0, 100) / 100f
        p * p
    } else {
        0f
    }

    val pickBackground = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            Toast.makeText(context, "Memproses berkas...", Toast.LENGTH_SHORT).show()
            scope.launch {
                val result = withContext(Dispatchers.IO) { importMedia(context, uri, bgFile, bgVideoFile) }
                when (result) {
                    BgImportResult.IMAGE, BgImportResult.VIDEO -> {
                        hasCustomBg = bgFile.exists()
                        hasVideoBg = bgVideoFile.exists()
                        bgVersion++
                        val msg = if (result == BgImportResult.VIDEO) {
                            "Video dipasang sebagai latar. Suara dan ambient diatur di Pengaturan > Tampilan"
                        } else {
                            "Latar belakang diganti"
                        }
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                    BgImportResult.TOO_BIG -> Toast.makeText(
                        context,
                        "Terlalu besar: foto atau GIF maksimal 30 MB, video maksimal 200 MB dan resolusi 2560 px",
                        Toast.LENGTH_LONG
                    ).show()
                    BgImportResult.UNSUPPORTED -> Toast.makeText(
                        context,
                        "Berkas tidak bisa dipakai. Pakai foto, GIF, atau video MP4 (H.264)",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // Status unduhan instance terpilih: dihitung ulang hanya saat instance berganti
    // atau saat proses unduh selesai (hindari cek file di tiap recomposition).
    val isInstanceReady = remember(selectedInstance, isDownloading) {
        selectedInstance?.let { it.isDownloaded && InstanceManager.isInstanceDownloaded(context, it) } ?: false
    }

    // Pasang instance: siapkan Java, lalu unduh client, library, dan aset game. Dipakai tombol UNDUH
    // dan dipanggil otomatis begitu instance baru dibuat, supaya mod tidak lagi ditaruh sebelum
    // instance terpasang. Dialog unduhan memblokir layar sampai selesai atau dibatalkan.
    fun installInstance(inst: Instance) {
        if (isDownloading) return
        val javaConfig = JavaRuntimeManager.configureJavaForInstance(context, inst, persistChanges = true)
        val targetRuntime = javaConfig.resolvedRuntimeId
        isDownloading = true
        downloadTargetName = javaConfig.instance.name
        dl.progress = 0f
        dl.message = "Menyiapkan OpenJDK (${JavaRuntimeManager.getRuntimeDisplayName(targetRuntime)})..."

        com.aistudio.novacraft.launcher.DownloadKeepAliveService.start(context)
        downloadJob = scope.launch {
            if (javaConfig.requiresExtraction) {
                JavaRuntimeManager.extractRuntime(context, targetRuntime) { p, msg ->
                    dl.progress = p
                    dl.message = msg
                }
            }
            // Dibatalkan lewat tombol Batal: layar sudah dikembalikan oleh pemanggil, jangan disentuh lagi.
            if (!isActive) return@launch

            val res = downloader.downloadInstance(inst) { p, msg ->
                dl.progress = p
                dl.message = msg
            }
            if (!isActive) return@launch

            isDownloading = false
            downloadJob = null
            if (res.isSuccess) {
                Toast.makeText(context, "Instalasi selesai! Tekan PLAY untuk bermain.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Gagal mengunduh: ${res.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }.also { job ->
            // Selesai, gagal, atau dibatalkan: penjaga proses tidak diperlukan lagi
            job.invokeOnCompletion {
                com.aistudio.novacraft.launcher.DownloadKeepAliveService.stop(context)
            }
        }
    }

    // Tombol Batal di dialog unduhan. Berkas yang sudah selesai tetap dipakai; tekan UNDUH untuk melanjutkan.
    fun cancelInstall() {
        downloadJob?.cancel()
        downloadJob = null
        isDownloading = false
        Toast.makeText(context, "Unduhan dibatalkan. Tekan UNDUH untuk melanjutkan.", Toast.LENGTH_SHORT).show()
    }

    // Tombol PLAY / UNDUH: unduh dulu bila belum lengkap, siapkan Java bila belum ada, lalu luncurkan.
    fun startInstance(inst: Instance) {
        val account = currentAccount
        if (account == null) {
            Toast.makeText(context, "Silakan buat atau pilih akun terlebih dahulu!", Toast.LENGTH_SHORT).show()
            currentTab = "accounts"
            return
        }

        // Mesin Rust (Horizon): tanpa JVM dan tanpa unduhan lewat launcher; langsung masuk game dengan akun yang dipilih
        if (HorizonEngine.isEngine(inst.loader)) {
            HorizonEngine.launch(context, inst, account)
            return
        }

        val isFullyDownloaded = inst.isDownloaded && InstanceManager.isInstanceDownloaded(context, inst)
        if (!isFullyDownloaded) {
            installInstance(inst)
        } else {
            val javaConfig = JavaRuntimeManager.configureJavaForInstance(context, inst, persistChanges = true)
            val resolvedInst = javaConfig.instance
            val targetRuntime = javaConfig.resolvedRuntimeId
            if (javaConfig.requiresExtraction) {
                isDownloading = true
                downloadTargetName = resolvedInst.name
                dl.progress = 0f
                dl.message = "Menyiapkan OpenJDK (${JavaRuntimeManager.getRuntimeDisplayName(targetRuntime)})..."
                scope.launch {
                    val extRes = JavaRuntimeManager.extractRuntime(context, targetRuntime) { p, msg ->
                        dl.progress = p
                        dl.message = msg
                    }
                    isDownloading = false
                    if (extRes.isSuccess) {
                        Toast.makeText(context, "Meluncurkan ${resolvedInst.name}...", Toast.LENGTH_SHORT).show()
                        GameLauncher.launch(context, resolvedInst, account)
                    } else {
                        Toast.makeText(context, "Gagal menyiapkan OpenJDK: ${extRes.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    }
                }
                return
            }

            val currentRendererInfo = NuxRendererRegistry.findRendererById(launcherSettings.selectedRenderer)
            val isSupported = NuxRendererRegistry.isSupportedForVersion(currentRendererInfo, resolvedInst.mcVersion)
            if (!isSupported) {
                unsupportedRendererInfo = currentRendererInfo
                pendingLaunchInstance = resolvedInst
            } else {
                Toast.makeText(context, "Meluncurkan ${resolvedInst.name}...", Toast.LENGTH_SHORT).show()
                GameLauncher.launch(context, resolvedInst, account)
            }
        }
    }

    if (currentTab == "gui_editor") {
        CustomGuiEditorScreen(
            onNavigateBack = { currentTab = "settings" },
            modifier = Modifier.fillMaxSize()
        )
    } else {
        // Semua layar utama memakai layar penuh dengan latar pilihan pengguna di belakangnya.
        // Jarak kiri/kanan hanya selebar kamera (plus sedikit celah); navigasi sistem tidak dihitung.
        val (leftInset, rightInset) = rememberCameraSideInsets()

        // Latar pilihan pengguna (foto/GIF/video, atau gambar bawaan) dan lapisan gelap tipisnya dipakai
        // di semua layar. Untuk gaya Liquid Glass, latar direkam lalu diburamkan sekali di sini dan
        // dipakai semua permukaan kaca (Android 12 ke atas; di bawah itu otomatis kembali ke Default).
        val liquid = AmberUi.style == AmberStyle.LIQUID && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        val rawBackdrop = rememberGraphicsLayer()
        val blurBackdrop = rememberGraphicsLayer()
        val blurPx = with(LocalDensity.current) { 26.dp.toPx() }
        // Redupkan latar: di Liquid Glass lapisan gelapnya dikurangi supaya wallpaper tetap terlihat.
        val scrim = remember(launcherSettings.bgDim, liquid) {
            amberScrimBrush(launcherSettings.bgDim / 100f * (if (liquid) 0.7f else 1f))
        }
        SideEffect {
            AmberUi.applyDim(launcherSettings.bgDim / 100f)
            if (liquid) {
                // Latar yang diburamkan dibuat lebih hidup warnanya supaya kaca terasa menyatu dengan wallpaper.
                blurBackdrop.renderEffect = BlurEffect(blurPx, blurPx, TileMode.Clamp)
                blurBackdrop.colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(1.5f) })
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NuxColors.Background)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        rawBackdrop.record { this@drawWithContent.drawContent() }
                        drawLayer(rawBackdrop)
                        // Yang direkam hanya wallpaper (tanpa lapisan gelap), jadi kaca menampilkan warna aslinya.
                        if (liquid) blurBackdrop.record { drawLayer(rawBackdrop) }
                    }
            ) {
                HomeBackground(
                    customFile = if (hasCustomBg) bgFile else null,
                    version = bgVersion,
                    videoFile = if (hasVideoBg && hasCustomBg) bgVideoFile else null,
                    animate = animateBackground,
                    videoVolume = videoVolume,
                    ambient = null,
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (currentTab != "home") {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(scrim)
                )
            }

            CompositionLocalProvider(LocalGlassBackdrop provides (if (liquid) blurBackdrop else null)) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    if (AmberUi.animScreenTransitions) {
                        fadeIn(tween(150)) togetherWith fadeOut(tween(110))
                    } else {
                        EnterTransition.None togetherWith ExitTransition.None
                    }
                },
                label = "dashboard_tab"
            ) { tab ->
            when (tab) {
                // Home: layar penuh tanpa sidebar dan kartu; navigasinya ada di bar bawah.
                "home" -> CompositionLocalProvider(LocalContentColor provides NuxColors.DarkGray) {
                    val rendererLabel = remember(launcherSettings.selectedRenderer) {
                        val info = NuxRendererRegistry.findRendererById(launcherSettings.selectedRenderer)
                        HomeInfo.rendererLabel(info.id, info.displayName)
                    }
                    HomeScene(
                        instance = selectedInstance,
                        isReady = isInstanceReady,
                        account = currentAccount,
                        rendererLabel = rendererLabel,
                        onOpenAbout = { showAboutDialog = true },
                        onOpenNews = { version ->
                            newsInitialVersion = version
                            showNewsDialog = true
                        },
                        onPickInstance = { showInstancePicker = true },
                        onPlay = { selectedInstance?.let { startInstance(it) } },
                        onCreateInstance = { currentTab = "create_instance" },
                        onEditInstance = { showEditInstanceDialog = true },
                        onNavigate = { nextTab ->
                            if (nextTab == "mods") {
                                modsInitialType = "mods"
                                modsBackTab = "home"
                            }
                            currentTab = nextTab
                        },
                        startInset = leftInset,
                        endInset = rightInset,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Mod, kelola konten, Buat Instance, Akun, dan Pengaturan: layar penuh dengan pil navigasi yang sama
                // di kiri atas (kembali, Mod, Akun, Pengaturan).
                else -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(
                            WindowInsets.displayCutout.only(WindowInsetsSides.Vertical)
                                .union(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                        )
                        .padding(start = leftInset, end = rightInset)
                ) {
                    NuxReadable {
                        val openMods = {
                            modsInitialType = "mods"
                            modsBackTab = "home"
                            currentTab = "mods"
                        }
                        when (tab) {
                            "mods" -> ModsScreen(
                                onNavigateBack = { currentTab = modsBackTab },
                                onOpenAccounts = { currentTab = "accounts" },
                                onOpenSettings = { currentTab = "settings" },
                                initialType = modsInitialType,
                                modifier = Modifier.fillMaxSize()
                            )
                            "instance_content" -> ModsScreen(
                                mode = ModsMode.MANAGE,
                                onNavigateBack = { currentTab = "home" },
                                onOpenAccounts = { currentTab = "accounts" },
                                onOpenSettings = { currentTab = "settings" },
                                onOpenBrowse = { type ->
                                    modsInitialType = type
                                    modsBackTab = "instance_content"
                                    currentTab = "mods"
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                            "create_instance" -> CreateInstanceScreen(
                                onNavigateBack = { currentTab = "home" },
                                onInstanceCreated = { newInst ->
                                    InstanceManager.createInstance(context, newInst)
                                    currentTab = "home"
                                    if (newInst.isDownloaded) {
                                        // Mesin Horizon (Rust) tidak butuh unduhan lewat launcher
                                        Toast.makeText(context, "Instance ${newInst.name} berhasil dibuat!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        // Langsung unduh supaya mod tidak ditambahkan sebelum instance terpasang
                                        Toast.makeText(context, "Instance ${newInst.name} dibuat. Mengunduh game...", Toast.LENGTH_SHORT).show()
                                        installInstance(newInst)
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                            "accounts" -> AccountsScreen(
                                onNavigateBack = { currentTab = "home" },
                                onOpenMods = openMods,
                                onOpenSettings = { currentTab = "settings" },
                                modifier = Modifier.fillMaxSize()
                            )
                            else -> SettingsScreen(
                                onNavigateBack = { currentTab = "home" },
                                onOpenMods = openMods,
                                onOpenAccounts = { currentTab = "accounts" },
                                onOpenGuiEditor = { currentTab = "gui_editor" },
                                onChangeBackground = { showBgDialog = true },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
            }
            }
        }

        // Dialog ganti latar belakang
        if (showBgDialog) {
            BackgroundDialog(
                hasCustom = hasCustomBg,
                onPick = {
                    showBgDialog = false
                    pickBackground.launch(arrayOf("image/*", "video/*"))
                },
                onReset = {
                    showBgDialog = false
                    bgFile.delete()
                    bgVideoFile.delete()
                    hasCustomBg = false
                    hasVideoBg = false
                    bgVersion++
                    Toast.makeText(context, "Latar belakang dikembalikan ke bawaan", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { showBgDialog = false }
            )
        }

        // Pemilih instance (ganti, tambah, ubah, hapus, buka folder)
        if (showInstancePicker) {
            NuxInstancePickerDialog(
                instances = instances,
                selectedId = selectedInstance?.id,
                onSelect = { inst ->
                    InstanceManager.selectInstance(inst)
                    showInstancePicker = false
                },
                onOpenFolder = { inst ->
                    showInstancePicker = false
                    Toast.makeText(context, "Membuka folder instance...", Toast.LENGTH_SHORT).show()
                    InstanceManager.openInstanceFolder(context, inst)
                },
                onEdit = { inst ->
                    showInstancePicker = false
                    InstanceManager.selectInstance(inst)
                    showEditInstanceDialog = true
                },
                onDelete = { inst ->
                    showInstancePicker = false
                    instanceToDelete = inst
                },
                onAdd = {
                    showInstancePicker = false
                    currentTab = "create_instance"
                },
                onDismiss = { showInstancePicker = false }
            )
        }

        // Edit Instance Dialog
        if (showEditInstanceDialog) {
            selectedInstance?.let { inst ->
                NuxEditInstanceDialog(
                    instance = inst,
                    onDismiss = { showEditInstanceDialog = false },
                    onInstanceUpdated = { updated ->
                        InstanceManager.updateInstance(context, updated)
                    },
                    onOpenContent = {
                        showEditInstanceDialog = false
                        currentTab = "instance_content"
                    },
                    onInstanceDeleted = { toDelete ->
                        InstanceManager.deleteInstance(context, toDelete.id)
                        Toast.makeText(context, "Instance ${toDelete.name} berhasil dihapus!", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        // Delete Instance Confirmation Dialog
        instanceToDelete?.let { inst ->
            NuxDeleteInstanceDialog(
                instance = inst,
                onConfirm = {
                    instanceToDelete = null
                    InstanceManager.deleteInstance(context, inst.id)
                    Toast.makeText(context, "Instance ${inst.name} berhasil dihapus!", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { instanceToDelete = null }
            )
        }

        // Download Progress Dialog
        if (isDownloading) {
            DownloadDialogHost(name = downloadTargetName, state = dl, onCancel = { cancelInstall() })
        }

        // Unsupported Renderer Warning Dialog (Persis seperti Zalith)
        val warnRenderer = unsupportedRendererInfo
        val launchInst = pendingLaunchInstance
        if (warnRenderer != null && launchInst != null) {
            NuxRendererWarningDialog(
                renderer = warnRenderer,
                mcVersion = launchInst.mcVersion,
                onConfirm = {
                    val toLaunch = pendingLaunchInstance
                    val account = currentAccount
                    unsupportedRendererInfo = null
                    pendingLaunchInstance = null
                    if (toLaunch != null && account != null) {
                        Toast.makeText(context, "Meluncurkan ${toLaunch.name}...", Toast.LENGTH_SHORT).show()
                        GameLauncher.launch(context, toLaunch, account)
                    }
                },
                onDismiss = {
                    unsupportedRendererInfo = null
                    pendingLaunchInstance = null
                }
            )
        }

        // Game Crash Popup Dialog
        activeCrash?.let { crash ->
            NuxCrashDialog(
                crashInfo = crash,
                onDismiss = {
                    CrashManager.dismissCrash(context)
                }
            )
        }

        // External Addon Import Dialog (Open With from File Manager)
        pendingImport?.let { importItem ->
            NuxAddonImportDialog(
                pendingImport = importItem,
                onDismiss = {
                    NovaUnifiedFileImportEngine.clearPendingImport()
                }
            )
        }

        // About & Open Source Licenses Dialog (GPL-3.0 & Zalith Compliance)
        if (showAboutDialog) {
            NuxAboutDialog(
                onDismissRequest = { showAboutDialog = false }
            )
        }

        // MC News & Alerts Dialog
        if (showNewsDialog) {
            com.aistudio.novacraft.launcher.ui.dialogs.MCNewsDialog(
                initialVersion = newsInitialVersion,
                onDismissRequest = { showNewsDialog = false }
            )
        }
    }
}

/**
 * Layar Home NovaCraft: Command Deck asimetris berlatar kosmik obsidian.
 * - Bar atas: lencana logo NovaCraft + pil akun cepat di kiri, tombol MC News & Tentang di kanan.
 * - Area utama: rel navigasi kaca ramping di kiri, kartu showcase instance aktif + telemetri RAM di tengah,
 *   dan dok peluncuran PLAY/UNDUH + pintasan Mod di kanan.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeScene(
    instance: Instance?,
    isReady: Boolean,
    account: UserAccount?,
    rendererLabel: String,
    onOpenAbout: () -> Unit,
    onOpenNews: (String?) -> Unit,
    onPickInstance: () -> Unit,
    onPlay: () -> Unit,
    onCreateInstance: () -> Unit,
    onEditInstance: () -> Unit,
    onNavigate: (String) -> Unit,
    startInset: Dp,
    endInset: Dp,
    modifier: Modifier = Modifier
) {
    val freeRamMb = rememberFreeRamMb()
    val dark = AmberUi.isDark
    val textShadow = remember(dark) {
        if (dark) {
            Shadow(Color(0xFF050816).copy(alpha = 0.55f), Offset(0f, 2f), 10f)
        } else {
            Shadow(Color.White.copy(alpha = 0.65f), Offset(0f, 1f), 6f)
        }
    }

    val context = LocalContext.current
    val accounts by AccountManager.accounts.collectAsState()
    val liveManifest by com.aistudio.novacraft.launcher.core.launch.MojangManifestService.liveManifest.collectAsState()
    val liveLatestRelease = liveManifest?.latest?.release.orEmpty()
    LaunchedEffect(Unit) {
        if (liveManifest == null) {
            com.aistudio.novacraft.launcher.core.launch.MojangManifestService.getVersionManifest()
        }
    }
    var accountMenuOpen by remember { mutableStateOf(false) }
    var showAddAccount by remember { mutableStateOf(false) }
    var accountToDelete by remember { mutableStateOf<UserAccount?>(null) }

    BoxWithConstraints(modifier = modifier) {
        val availWidth = (maxWidth - startInset - endInset).coerceAtLeast(280.dp)
        val compact = availWidth < 620.dp
        val stackedDeck = availWidth < 540.dp
        val shortScreen = maxHeight < 380.dp
        val nameSize = if (shortScreen || stackedDeck) 26.sp else 34.sp

        // Atmosfer gradien agar kartu & tipografi selalu tajam di atas wallpaper mana pun
        val vStops = if (dark) {
            arrayOf(
                0f to Color(0xFF050816).copy(alpha = 0.42f),
                0.32f to Color.Transparent,
                0.68f to Color(0xFF060B1D).copy(alpha = 0.36f),
                1f to Color(0xFF060B1D).copy(alpha = 0.86f)
            )
        } else {
            arrayOf(
                0f to Color(0xFFF8FAFC).copy(alpha = 0.48f),
                0.32f to Color.Transparent,
                0.68f to Color(0xFFF1F5F9).copy(alpha = 0.40f),
                1f to Color(0xFFE2E8F0).copy(alpha = 0.86f)
            )
        }
        val hStops = if (dark) {
            arrayOf(
                0f to Color(0xFF060B1D).copy(alpha = 0.62f),
                0.55f to Color.Transparent,
                1f to Color(0xFF060B1D).copy(alpha = 0.45f)
            )
        } else {
            arrayOf(
                0f to Color(0xFFF8FAFC).copy(alpha = 0.56f),
                0.55f to Color.Transparent,
                1f to Color(0xFFF1F5F9).copy(alpha = 0.42f)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(colorStops = vStops))
                .background(Brush.horizontalGradient(colorStops = hStops))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.displayCutout.only(WindowInsetsSides.Vertical)
                        .union(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                )
                .padding(
                    start = startInset + 16.dp,
                    end = endInset + 16.dp,
                    top = 14.dp,
                    bottom = 14.dp
                )
        ) {
            // ---- BAR ATAS: Emblem NovaCraft + Pil Pemain di kiri, MC News & Info di kanan ----
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(CircleShape)
                        .amberGlass(CircleShape, AmberGlassKind.Home)
                        .padding(start = 8.dp, end = if (stackedDeck) 8.dp else 12.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_novacraft_logo),
                        contentDescription = "NovaCraft",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .border(1.dp, AmberUi.AccentEdge.copy(alpha = 0.6f), CircleShape)
                    )
                    if (!stackedDeck) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "NOVACRAFT",
                            color = AmberUi.Text,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            letterSpacing = 1.6.sp,
                            maxLines = 1
                        )
                    }
                    if (!compact) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(AmberUi.Accent.copy(alpha = 0.25f))
                                .border(1.dp, AmberUi.AccentEdge.copy(alpha = 0.45f), CircleShape)
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "2.3",
                                color = AmberUi.AccentSoft,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Pil pemain (ketuk = menu akun cepat)
                Row(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(CircleShape)
                        .amberGlass(CircleShape, AmberGlassKind.Home)
                        .clickable {
                            if (accounts.isEmpty()) showAddAccount = true
                            else accountMenuOpen = !accountMenuOpen
                        }
                        .testTag("home_account_button")
                        .padding(start = 8.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (account != null) {
                        NuxAccountAvatar(
                            account = account,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(AmberUi.Accent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Add,
                                contentDescription = null,
                                tint = AmberUi.OnAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = account?.username ?: "Tambah akun",
                        color = AmberUi.Text,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = if (stackedDeck) 104.dp else 140.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (accountMenuOpen) Icons.Default.KeyboardArrowUp else androidx.compose.material.icons.Icons.Default.KeyboardArrowDown,
                        contentDescription = "Ganti akun",
                        tint = AmberUi.AccentSoft,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Tombol MC News & Alerts berlabel + versi rilis resmi real-time dari Mojang
                Row(
                    modifier = Modifier
                        .height(48.dp)
                        .clip(CircleShape)
                        .amberGlass(CircleShape, AmberGlassKind.Home)
                        .clickable { onOpenNews(null) }
                        .testTag("home_news_button")
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (liveLatestRelease.isNotEmpty()) NuxColors.SuccessGreen else AmberUi.AccentSoft,
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Outlined.Campaign,
                        contentDescription = "MC News & Alerts",
                        tint = AmberUi.Text,
                        modifier = Modifier.size(19.dp)
                    )
                    if (!compact) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "MC News",
                            color = AmberUi.Text,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                        if (liveLatestRelease.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(7.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(NuxColors.SuccessGreen.copy(alpha = 0.22f))
                                    .border(1.dp, NuxColors.SuccessGreen.copy(alpha = 0.45f), CircleShape)
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                androidx.compose.material3.Text(
                                    text = liveLatestRelease,
                                    color = if (AmberUi.isDark) Color(0xFF6EE7B7) else NuxColors.SuccessGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Tentang & Lisensi (GPL-3.0)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .amberGlass(CircleShape, AmberGlassKind.Home)
                        .clickable { onOpenAbout() }
                        .testTag("home_about_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Tentang & Lisensi",
                        tint = AmberUi.Text,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // ---- COMMAND DECK: Adaptif Portrait/Semipit (susun vertikal) vs Landscape/Tablet (3 kolom) ----
            val cardShape = RoundedCornerShape(24.dp)
            val showcaseCard: @Composable (Modifier) -> Unit = { cardMod ->
                Column(
                    modifier = cardMod
                        .clip(cardShape)
                        .amberGlass(cardShape, AmberGlassKind.Home)
                        .padding(horizontal = 18.dp, vertical = if (shortScreen) 12.dp else 16.dp)
                ) {
                    if (instance != null) {
                        val isRust = HorizonEngine.isEngine(instance.loader)
                        val javaLabel = remember(instance.id, instance.mcVersion, instance.javaRuntime, instance.loader) {
                            if (isRust) {
                                "Horizon Rust"
                            } else {
                                val rt = if (instance.javaRuntime != "auto") instance.javaRuntime
                                else JavaRuntimeManager.getRecommendedRuntime(instance.mcVersion)
                                rt.replace("jre-", "Java ")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(14.dp)
                                    .background(AmberUi.AccentSoft, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "INSTANCE AKTIF",
                                color = AmberUi.AccentSoft,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 1.8.sp,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            val statusBg = if (isReady) Color(0xFF10B981).copy(alpha = if (AmberUi.isDark) 0.22f else 0.16f) else NuxColors.Amber.copy(alpha = 0.20f)
                            val statusText = if (isReady) "Siap" else "Perlu unduh"
                            val statusFg = if (isReady) {
                                if (AmberUi.isDark) Color(0xFF6EE7B7) else NuxColors.SuccessGreen
                            } else {
                                NuxColors.Amber
                            }
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(statusBg)
                                    .border(1.dp, statusFg.copy(alpha = 0.45f), CircleShape)
                                    .padding(horizontal = 9.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = statusText,
                                    color = statusFg,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            HomeInstanceButton(
                                icon = Icons.Outlined.SwapHoriz,
                                description = "Ganti instance",
                                testTag = "home_pick_instance_button",
                                onClick = onPickInstance
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            HomeInstanceButton(
                                icon = Icons.Outlined.Edit,
                                description = "Ubah instance",
                                testTag = "home_edit_instance_button",
                                onClick = onEditInstance
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = instance.name,
                            color = AmberUi.Text,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = nameSize,
                            lineHeight = nameSize * 1.08f,
                            letterSpacing = (-1.0).sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = LocalTextStyle.current.copy(shadow = textShadow)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            HomeMetaBadge(
                                text = "MC ${instance.mcVersion}",
                                highlighted = false,
                                icon = Icons.Outlined.Newspaper,
                                modifier = Modifier.testTag("home_version_news_badge"),
                                onClick = { onOpenNews(instance.mcVersion) }
                            )
                            HomeMetaBadge(
                                text = com.aistudio.novacraft.launcher.core.launch.ForgeFlavor.label(instance.loader),
                                highlighted = true
                            )
                            HomeMetaBadge(
                                text = javaLabel,
                                highlighted = false
                            )
                            if (!compact || stackedDeck) {
                                HomeMetaBadge(
                                    text = rendererLabel,
                                    highlighted = false
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val ramColor = when (HomeInfo.ramLevel(freeRamMb)) {
                            2 -> if (AmberUi.isDark) Color(0xFF34D399) else NuxColors.SuccessGreen
                            1 -> NuxColors.Amber
                            else -> AmberUi.Danger
                        }
                        val ramRatio = (freeRamMb / 2400f).coerceIn(0.12f, 1f)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Memory,
                                contentDescription = null,
                                tint = ramColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = HomeInfo.ramText(freeRamMb),
                                color = AmberUi.TextDim,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(5.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (AmberUi.isDark) Color.White.copy(alpha = 0.12f)
                                        else Color(0xFF0F172A).copy(alpha = 0.12f)
                                    )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(ramRatio)
                                        .clip(CircleShape)
                                        .background(ramColor)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "NOVACRAFT WORKSPACE",
                            color = AmberUi.AccentSoft,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.8.sp,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Belum ada instance",
                            color = AmberUi.Text,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = nameSize,
                            letterSpacing = (-1.0).sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Buat instance Minecraft pertamamu untuk mulai bermain.",
                            color = AmberUi.TextDim,
                            fontSize = 13.sp,
                            maxLines = 2
                        )
                    }
                }
            }

            val launchDock: @Composable (Modifier) -> Unit = { dockMod ->
                Column(
                    modifier = dockMod
                        .clip(cardShape)
                        .amberGlass(cardShape, AmberGlassKind.Home)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (instance != null) {
                        HomeMainButton(
                            text = if (isReady) "PLAY" else "UNDUH",
                            icon = if (isReady) Icons.Default.PlayArrow else androidx.compose.material.icons.Icons.Default.Download,
                            onClick = onPlay,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("home_play_button")
                        )
                        val isHorizonDock = HorizonEngine.isEngine(instance.loader)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 40.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(AmberUi.SurfaceRaised)
                                .border(1.dp, AmberUi.BorderStrong, RoundedCornerShape(14.dp))
                                .clickable {
                                    onNavigate(if (isHorizonDock) "instance_content" else "mods")
                                }
                                .testTag("home_quick_mods_button")
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isHorizonDock) Icons.Outlined.Folder else Icons.Outlined.FileDownload,
                                contentDescription = null,
                                tint = AmberUi.AccentSoft,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHorizonDock) "Vault Lokal" else "Jelajah Mod",
                                color = AmberUi.Text,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    } else {
                        HomeMainButton(
                            text = "TAMBAH",
                            icon = androidx.compose.material.icons.Icons.Default.Add,
                            onClick = onCreateInstance,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("home_create_instance_button")
                        )
                    }
                }
            }

            if (stackedDeck) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    showcaseCard(Modifier.fillMaxWidth())
                    launchDock(Modifier.fillMaxWidth())
                    HomeCommandRail(
                        selected = "home",
                        onSelect = onNavigate,
                        compact = true,
                        horizontal = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    HomeCommandRail(
                        selected = "home",
                        onSelect = onNavigate,
                        compact = shortScreen || compact
                    )
                    showcaseCard(Modifier.weight(1f))
                    launchDock(Modifier.width(if (compact) 168.dp else 224.dp))
                }
            }
        }

        // Ketuk di luar menu = tutup
        if (accountMenuOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { accountMenuOpen = false }
            )
            HomeAccountMenu(
                accounts = accounts,
                currentAccountId = account?.id,
                onSelect = { picked ->
                    AccountManager.selectAccount(context, picked)
                    accountMenuOpen = false
                },
                onDelete = { accountToDelete = it },
                onAdd = {
                    accountMenuOpen = false
                    showAddAccount = true
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Vertical))
                    .padding(start = startInset + 16.dp, top = 14.dp + 54.dp)
                    .fillMaxWidth(0.86f)
                    .widthIn(max = 280.dp)
            )
        }
    }

    if (showAddAccount) {
        AddAccountModal(
            onDismiss = { showAddAccount = false },
            onAccountAdded = { newAcc ->
                AccountManager.addAccount(context, newAcc)
                Toast.makeText(context, "Akun ${newAcc.username} berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                showAddAccount = false
            }
        )
    }
    accountToDelete?.let { target ->
        DeleteAccountDialog(
            target = target,
            onDismiss = { accountToDelete = null }
        )
    }
}

@Composable
private fun HomeMetaBadge(
    text: String,
    highlighted: Boolean,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(
                if (highlighted) AmberUi.Accent.copy(alpha = if (AmberUi.isDark) 0.24f else 0.14f)
                else if (AmberUi.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFF0F172A).copy(alpha = 0.06f)
            )
            .border(
                1.dp,
                if (highlighted) AmberUi.AccentEdge.copy(alpha = 0.5f) else AmberUi.Border,
                shape
            )
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (highlighted) AmberUi.AccentSoft else AmberUi.TextDim,
                modifier = Modifier.size(13.dp)
            )
        }
        Text(
            text = text,
            color = if (highlighted) AmberUi.AccentSoft else AmberUi.Text,
            fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Medium,
            fontSize = 12.sp,
            maxLines = 1
        )
    }
}

/** Tombol bulat kecil di samping kartu instance (ganti dan ubah instance). */
@Composable
private fun HomeInstanceButton(
    icon: ImageVector,
    description: String,
    testTag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(
                if (AmberUi.isDark) Color.White.copy(alpha = 0.08f)
                else Color(0xFF0F172A).copy(alpha = 0.06f)
            )
            .border(1.dp, AmberUi.BorderStrong, CircleShape)
            .clickable { onClick() }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = AmberUi.Text,
            modifier = Modifier.size(18.dp)
        )
    }
}

private val HOME_NAV_ITEMS = listOf(
    Triple("home", "Home", Icons.Outlined.Home),
    Triple("mods", "Mod", Icons.Outlined.FileDownload),
    Triple("accounts", "Akun", Icons.Outlined.Person),
    Triple("settings", "Pengaturan", Icons.Outlined.Tune)
)

/** Rel navigasi kaca NovaCraft: Home, Mod, Akun, Pengaturan (vertikal di landscape, horizontal di portrait). */
@Composable
private fun HomeCommandRail(
    selected: String,
    onSelect: (String) -> Unit,
    compact: Boolean,
    horizontal: Boolean = false,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(22.dp)
    if (horizontal) {
        Row(
            modifier = modifier
                .clip(shape)
                .amberGlass(shape, AmberGlassKind.Home)
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HOME_NAV_ITEMS.forEach { (id, label, icon) ->
                val isSelected = id == selected
                val itemShape = RoundedCornerShape(16.dp)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 42.dp)
                        .clip(itemShape)
                        .background(
                            if (isSelected) AmberUi.Accent else Color.Transparent,
                            itemShape
                        )
                        .border(
                            1.dp,
                            if (isSelected) AmberUi.AccentEdge else Color.Transparent,
                            itemShape
                        )
                        .clickable { if (!isSelected) onSelect(id) }
                        .testTag("nav_$id")
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) Color.White else AmberUi.TextDim,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        return
    }

    Column(
        modifier = modifier
            .width(if (compact) 58.dp else 128.dp)
            .clip(shape)
            .amberGlass(shape, AmberGlassKind.Home)
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        HOME_NAV_ITEMS.forEach { (id, label, icon) ->
            val isSelected = id == selected
            val itemShape = RoundedCornerShape(16.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = if (compact) 42.dp else 44.dp)
                    .clip(itemShape)
                    .background(
                        if (isSelected) AmberUi.Accent else Color.Transparent,
                        itemShape
                    )
                    .border(
                        1.dp,
                        if (isSelected) AmberUi.AccentEdge else Color.Transparent,
                        itemShape
                    )
                    .clickable { if (!isSelected) onSelect(id) }
                    .testTag("nav_$id")
                    .padding(horizontal = if (compact) 0.dp else 12.dp, vertical = 6.dp),
                horizontalArrangement = if (compact) Arrangement.Center else Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isSelected) Color.White else AmberUi.TextDim,
                    modifier = Modifier.size(19.dp)
                )
                if (!compact) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else AmberUi.TextDim,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/** Tombol utama NovaCraft (PLAY / UNDUH / TAMBAH) dengan efek tekan taktil. */
@Composable
private fun HomeMainButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val lift by animateDpAsState(if (pressed) 0.dp else 4.dp, label = "homeMainLift")
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .height(60.dp)
            .clickable(interactionSource = interaction, indication = null) { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .align(Alignment.BottomCenter)
                .background(AmberUi.AccentDeep, shape)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .align(Alignment.TopCenter)
                .offset(y = 4.dp - lift)
                .clip(shape)
                .background(AmberUi.Accent, shape)
                .border(1.5.dp, AmberUi.AccentEdge, shape),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 17.sp,
                letterSpacing = 2.sp,
                maxLines = 1
            )
        }
    }
}

/** Sisa RAM HP (dibulatkan 100 MB), diperbarui tiap 5 detik selama Home tampil. */
@Composable
private fun rememberFreeRamMb(): Int {
    val context = LocalContext.current.applicationContext
    val initial = remember { HomeInfo.ramBucket(SettingsManager.getAvailableDeviceMemoryMb(context)) }
    val state = produceState(initialValue = initial) {
        while (true) {
            delay(5000L)
            value = HomeInfo.ramBucket(SettingsManager.getAvailableDeviceMemoryMb(context))
        }
    }
    return state.value
}

private const val BG_FILE_NAME = "home_bg"
private const val BG_MAX_BYTES = 30L * 1024 * 1024
private const val BG_MAX_SIDE = 1600
private const val BG_ANIM_SIDE = 1280   // GIF/WebP bergerak: sisi terpanjang pas dengan lebar panel Home (tidak diperbesar kasar)

/** Status unduhan; hanya dibaca oleh dialog unduhan. */
private class DownloadUiState {
    var progress by mutableFloatStateOf(0f)
    var message by mutableStateOf("")
}

@Composable
private fun DownloadDialogHost(name: String, state: DownloadUiState, onCancel: () -> Unit) {
    NuxDownloadProgressDialog(
        instanceName = name,
        progress = state.progress,
        message = state.message,
        onCancel = onCancel
    )
}

/** Hasil muat latar: drawable null berarti gagal. */
private class BgState(val drawable: Drawable?)

/**
 * Decode foto/GIF/WebP jadi Drawable. Foto dikecilkan (sisi terpanjang <= 1600 px) supaya hemat RAM.
 * GIF/WebP animasi menjadi AnimatedImageDrawable (Android 9 ke atas) dan diubah ukurannya saat
 * decode ke sisi terpanjang 1280 px, kira-kira selebar panel Home. Kalau lebih kecil dari itu ikut
 * diperhalus saat decode, bukan diperbesar kasar oleh ImageView (itu yang tampak kotak-kotak).
 */
private fun decodeBackground(file: File): Drawable? = try {
    if (Build.VERSION.SDK_INT >= 28) {
        ImageDecoder.decodeDrawable(ImageDecoder.createSource(file)) { decoder, info, _ ->
            val w = info.size.width
            val h = info.size.height
            val longSide = maxOf(w, h)
            val mime = info.mimeType
            val maybeAnimated = mime.equals("image/gif", ignoreCase = true) ||
                mime.equals("image/webp", ignoreCase = true)
            val target = if (maybeAnimated) BG_ANIM_SIDE else BG_MAX_SIDE
            // Foto: hanya dikecilkan. GIF/WebP: dibawa ke ukuran target (naik atau turun).
            val needsResize = if (maybeAnimated) longSide != target else longSide > target
            if (needsResize) {
                val scale = target.toFloat() / longSide
                decoder.setTargetSize(maxOf(1, (w * scale).toInt()), maxOf(1, (h * scale).toInt()))
            }
        }
    } else {
        val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
        BitmapFactory.decodeFile(file.absolutePath, opts)?.let {
            BitmapDrawable(android.content.res.Resources.getSystem(), it)
        }
    }
} catch (e: Throwable) {
    null
}

/** Salin gambar pilihan ke penyimpanan aplikasi, uji decode, lalu pasang sebagai latar. */
private fun importBackground(context: Context, uri: Uri, target: File): Boolean {
    val tmp = File(target.parentFile, "$BG_FILE_NAME.tmp")
    return try {
        val input = context.contentResolver.openInputStream(uri) ?: return false
        input.use { ins ->
            tmp.outputStream().use { out ->
                val buf = ByteArray(64 * 1024)
                var total = 0L
                while (true) {
                    val n = ins.read(buf)
                    if (n < 0) break
                    total += n
                    if (total > BG_MAX_BYTES) throw java.io.IOException("terlalu besar")
                    out.write(buf, 0, n)
                }
            }
        }
        if (decodeBackground(tmp) == null) {
            tmp.delete()
            return false
        }
        if (target.exists()) target.delete()
        tmp.renameTo(target)
    } catch (e: Throwable) {
        tmp.delete()
        false
    }
}

/**
 * Memasang berkas pilihan sebagai latar. Video disimpan bersama poster-nya; foto/GIF menghapus
 * video lama supaya tidak ada dua latar sekaligus.
 */
private fun importMedia(context: Context, uri: Uri, imageTarget: File, videoTarget: File): BgImportResult {
    val mime = try {
        context.contentResolver.getType(uri)
    } catch (_: Throwable) {
        null
    }
    if (mime != null && mime.startsWith("video/")) {
        return HomeVideo.importVideo(context, uri, videoTarget, imageTarget)
    }
    if (importBackground(context, uri, imageTarget)) {
        videoTarget.delete()
        return BgImportResult.IMAGE
    }
    // Jenis berkas tidak dikenali oleh pemilih file: coba sebagai video sebelum menyerah.
    if (mime == null || mime == "application/octet-stream") {
        val result = HomeVideo.importVideo(context, uri, videoTarget, imageTarget)
        if (result != BgImportResult.UNSUPPORTED) return result
    }
    return BgImportResult.UNSUPPORTED
}

/**
 * Latar Home: gambar bawaan, foto/GIF pilihan pengguna, atau video. Video diputar di bawah dan
 * posternya (satu bingkai diam) menutupinya sampai bingkai pertama tampil; bila video gagal diputar
 * atau animasi dimatikan, poster itu tetap jadi latarnya.
 */
@Composable
private fun HomeBackground(
    customFile: File?,
    version: Int,
    videoFile: File?,
    animate: Boolean,
    videoVolume: Float,
    ambient: AmbientState?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var videoFailed by remember(videoFile, version) { mutableStateOf(false) }
    var videoReady by remember(videoFile, version) { mutableStateOf(false) }
    LaunchedEffect(videoFailed) {
        if (videoFailed) {
            Toast.makeText(
                context,
                "Video tidak bisa diputar di HP ini, latar memakai gambar diam",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    if (videoFile != null && animate && !videoFailed) {
        Box(modifier = modifier) {
            HomeVideoLayer(
                file = videoFile,
                volume = videoVolume,
                playing = true,
                ambient = ambient,
                onFrameReady = { videoReady = it },
                onFailed = { videoFailed = true },
                modifier = Modifier.fillMaxSize()
            )
            // Poster diam menutupi video sampai bingkai pertama tampil, lalu hilang seketika (tanpa pudar).
            if (!videoReady) {
                HomeStillBackground(
                    customFile = customFile,
                    version = version,
                    animate = false,
                    ambient = null,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    } else {
        HomeStillBackground(
            customFile = customFile,
            version = version,
            animate = animate,
            ambient = ambient,
            modifier = modifier
        )
    }
}

/** Gambar bawaan, atau foto/GIF pilihan pengguna bila ada (GIF bergerak bila [animate]). */
@Composable
private fun HomeStillBackground(
    customFile: File?,
    version: Int,
    animate: Boolean,
    ambient: AmbientState?,
    modifier: Modifier = Modifier
) {
    if (customFile == null) {
        Image(
            painter = painterResource(id = R.drawable.img_novacraft_hero),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
        return
    }

    val state by produceState<BgState?>(initialValue = null, customFile, version) {
        value = BgState(withContext(Dispatchers.IO) { decodeBackground(customFile) })
    }
    val st = state
    val drawable = st?.drawable
    when {
        st == null -> Box(modifier = modifier.background(NuxColors.Ink))
        drawable == null -> Image(
            painter = painterResource(id = R.drawable.img_novacraft_hero),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
        else -> {
            // GIF: berhenti otomatis saat keluar dari layar Home
            DisposableEffect(drawable) {
                onDispose {
                    if (Build.VERSION.SDK_INT >= 28 && drawable is AnimatedImageDrawable) drawable.stop()
                }
            }

            // Cahaya ambient dari GIF: ambil warna bingkai yang sedang tampil beberapa kali per detik.
            val resumed = rememberIsResumed()
            if (ambient != null && animate && resumed && Build.VERSION.SDK_INT >= 28 && drawable is AnimatedImageDrawable) {
                val sampleBitmap = remember(drawable) { AmbientSampler.newBitmap() }
                val samplePixels = remember(drawable) { AmbientSampler.newPixels() }
                LaunchedEffect(drawable, ambient) {
                    delay(400)
                    while (true) {
                        try {
                            AmbientSampler.fromDrawable(drawable, sampleBitmap, samplePixels, ambient.aspect)
                                ?.let { ambient.colors = it }
                        } catch (_: Throwable) {
                        }
                        delay(AmbientSampler.INTERVAL_MS)
                    }
                }
            }

            AndroidView(
                factory = { ctx -> ImageView(ctx).apply { scaleType = ImageView.ScaleType.CENTER_CROP } },
                update = { iv ->
                    iv.setImageDrawable(drawable)
                    if (Build.VERSION.SDK_INT >= 28 && drawable is AnimatedImageDrawable) {
                        drawable.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
                        if (animate) drawable.start() else drawable.stop()
                    }
                },
                modifier = modifier
            )
        }
    }
}

/** Dialog kecil: pilih foto, GIF, atau video dari galeri, atau kembali ke latar bawaan. */
@Composable
private fun BackgroundDialog(
    hasCustom: Boolean,
    onPick: () -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    NuxDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = 460.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LATAR BELAKANG",
                    color = NuxColors.DarkGray,
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.weight(1f)
                )
                PickerIconButton(
                    icon = androidx.compose.material.icons.Icons.Default.Close,
                    description = "Tutup",
                    tint = NuxColors.DarkGray,
                    onClick = onDismiss
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Foto paling ringan. GIF bergerak dengan ringan. Video paling halus dan bisa HD 60 fps " +
                    "(MP4 H.264, maksimal 200 MB), tapi memakai baterai lebih banyak. " +
                    "Suara video, ambient, dan hemat baterai diatur di Pengaturan > Tampilan.",
                color = NuxColors.GrayNeutral,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            NuxButton(
                onClick = onPick,
                backgroundColor = NuxColors.Yellow,
                contentColor = NuxColors.Ink,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Outlined.Image,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PILIH FOTO / GIF / VIDEO",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    letterSpacing = 0.5.sp
                )
            }

            if (hasCustom) {
                Spacer(modifier = Modifier.height(10.dp))
                NuxButton(
                    onClick = onReset,
                    backgroundColor = NuxColors.SurfaceWhite,
                    contentColor = NuxColors.DarkGray,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "KEMBALI KE BAWAAN",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

/** Tombol ikon bulat 40dp (target sentuh nyaman) untuk dialog pemilih instance. */
@Composable
private fun PickerIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
    }
}

// ----------------------------------------------------------------------------
// Section: ui/screens/HomeAccountMenu.kt
// ----------------------------------------------------------------------------

/**
 * Menu akun cepat yang muncul di bawah pil pemain Home: ganti akun dengan satu ketukan,
 * hapus akun, dan tambah akun baru tanpa harus membuka tab Akun.
 * Akun yang sedang dipakai diberi garis tepi biru.
 */
@Composable
internal fun HomeAccountMenu(
    accounts: List<UserAccount>,
    currentAccountId: String?,
    onSelect: (UserAccount) -> Unit,
    onDelete: (UserAccount) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Daftar akun (bisa digulir bila banyak)
        Column(
            modifier = Modifier
                .heightIn(max = 190.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            accounts.forEach { acc ->
                val isCurrent = acc.id == currentAccountId
                val shape = RoundedCornerShape(24.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .glassOrCard(shape)
                        .then(
                            if (isCurrent) Modifier.border(1.5.dp, AmberUi.AccentEdge, shape) else Modifier
                        )
                        .clickable { onSelect(acc) }
                        .padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NuxAccountAvatar(
                        account = acc,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = acc.username,
                        color = AmberUi.Text,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .clickable { onDelete(acc) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Hapus akun",
                            tint = AmberUi.Danger,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // Tambah akun
        val addShape = RoundedCornerShape(24.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(addShape)
                .glassOrCard(addShape)
                .clickable { onAdd() }
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    tint = AmberUi.Text,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = "Tambah akun",
                color = AmberUi.Text,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                maxLines = 1
            )
        }
    }
}

/** Kartu menu: kaca saat gaya Liquid Glass, kartu gelap pekat saat gaya Default. */
private fun Modifier.glassOrCard(shape: androidx.compose.ui.graphics.Shape): Modifier =
    if (AmberUi.style == AmberStyle.LIQUID) {
        amberGlass(shape, AmberGlassKind.Home)
    } else {
        this
            .background(AmberUi.MenuCard, shape)
            .border(1.dp, AmberUi.Border, shape)
    }

// ----------------------------------------------------------------------------
// Section: ui/screens/ModsScreen.kt
// ----------------------------------------------------------------------------

// (Helper MIME/Ekstensi/URI telah disatukan ke NovaUnifiedFileImportEngine)

data class ModCategoryItem(val id: String, val label: String)

private const val MODS_CATEGORY_FAVORITES = "__favorites__"

private val CATEGORIES_MAP = mapOf(
    "mods" to listOf(
        ModCategoryItem("Semua", "Tất cả"),
        ModCategoryItem(MODS_CATEGORY_FAVORITES, "Yêu thích"),
        ModCategoryItem("utility", "Utility"),
        ModCategoryItem("performance", "Performance"),
        ModCategoryItem("cosmetic", "Cosmetic"),
        ModCategoryItem("worldgen", "World Gen"),
        ModCategoryItem("technology", "Technology"),
        ModCategoryItem("magic", "Magic"),
        ModCategoryItem("adventure", "Adventure"),
        ModCategoryItem("storage", "Storage")
    ),
    "modpacks" to listOf(
        ModCategoryItem("Semua", "Tất cả"),
        ModCategoryItem(MODS_CATEGORY_FAVORITES, "Yêu thích"),
        ModCategoryItem("adventure", "Adventure"),
        ModCategoryItem("performance", "Performance"),
        ModCategoryItem("technology", "Technology"),
        ModCategoryItem("magic", "Magic"),
        ModCategoryItem("quests", "Quests")
    ),
    "resourcepacks" to listOf(
        ModCategoryItem("Semua", "Tất cả"),
        ModCategoryItem(MODS_CATEGORY_FAVORITES, "Yêu thích"),
        ModCategoryItem("16x", "16x"),
        ModCategoryItem("32x", "32x"),
        ModCategoryItem("simplistic", "Simplistic"),
        ModCategoryItem("traditional", "Traditional"),
        ModCategoryItem("medieval", "Medieval")
    ),
    "shaderpacks" to listOf(
        ModCategoryItem("Semua", "Tất cả"),
        ModCategoryItem(MODS_CATEGORY_FAVORITES, "Yêu thích"),
        ModCategoryItem("realistic", "Realistic"),
        ModCategoryItem("stylized", "Stylized"),
        ModCategoryItem("fantasy", "Fantasy")
    ),
    "datapacks" to listOf(
        ModCategoryItem("Semua", "Tất cả"),
        ModCategoryItem(MODS_CATEGORY_FAVORITES, "Yêu thích"),
        ModCategoryItem("adventure", "Adventure"),
        ModCategoryItem("utility", "Utility"),
        ModCategoryItem("magic", "Magic"),
        ModCategoryItem("worldgen", "World Gen")
    ),
    "worlds" to listOf(
        ModCategoryItem("Semua", "Tất cả"),
        ModCategoryItem(MODS_CATEGORY_FAVORITES, "Yêu thích"),
        ModCategoryItem("adventure", "Adventure"),
        ModCategoryItem("survival", "Survival"),
        ModCategoryItem("skyblock", "Skyblock"),
        ModCategoryItem("parkour", "Parkour"),
        ModCategoryItem("worldgen", "World Gen")
    )
)

/**
 * BROWSE = jelajah dan pasang dari Modrinth (tab Mod di Home).
 * MANAGE = kelola isi instance yang terpasang (dibuka dari Ubah instance).
 */
enum class ModsMode { BROWSE, MANAGE }

@Composable
fun ModsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenAccounts: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    mode: ModsMode = ModsMode.BROWSE,
    initialType: String = "mods",
    onOpenBrowse: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val favViewModel = remember(context) {
        var cur: Context? = context
        var act: androidx.activity.ComponentActivity? = null
        while (cur is ContextWrapper) {
            if (cur is androidx.activity.ComponentActivity) {
                act = cur
                break
            }
            cur = cur.baseContext
        }
        if (act != null) {
            ViewModelProvider(act)[ModFavoritesViewModel::class.java]
        } else {
            ModFavoritesViewModel(context.applicationContext as android.app.Application)
        }
    }

    val instances by InstanceManager.instances.collectAsState()
    val selectedInstance by InstanceManager.selectedInstance.collectAsState()
    val allFavorites by favViewModel.favorites.collectAsStateWithLifecycle()
    val favoriteIds by favViewModel.favoriteIds.collectAsStateWithLifecycle()

    // Mode: "installed" (kelola isi instance) | "browse" (jelajah Modrinth)
    val mainMode = if (mode == ModsMode.MANAGE) "installed" else "browse"

    // Mesin Rust (Horizon) hanya memakai Shader dan Resource Pack, dan hanya dari Modrinth
    val rustInstance = HorizonEngine.isEngine(selectedInstance?.loader)

    // Item Type: "mods" | "modpacks" | "resourcepacks" | "shaderpacks" | "datapacks"
    var activeTypeState by remember { mutableStateOf(initialType) }
    val activeType = if (rustInstance && !HorizonContent.supports(activeTypeState)) {
        HorizonContent.TYPE_SHADERS
    } else {
        activeTypeState
    }
    val recentQueries by favViewModel.recentQueriesFor(activeType).collectAsStateWithLifecycle(initialValue = emptyList())

    // World state for Datapacks
    var worlds by remember { mutableStateOf<List<WorldInfo>>(emptyList()) }
    var selectedWorld by remember { mutableStateOf("") }
    var showWorldMenu by remember { mutableStateOf(false) }

    // Installed list state
    var installedItems by remember { mutableStateOf<List<InstalledModItem>>(emptyList()) }
    var isInstalledLoading by remember { mutableStateOf(false) }
    var installedSearch by remember { mutableStateOf("") }
    var itemToDelete by remember { mutableStateOf<InstalledModItem?>(null) }

    // Browse Modrinth & CurseForge state
    var browseQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Semua") }
    var searchHits by remember { mutableStateOf<List<ModrinthSearchHit>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchErrorMessage by remember { mutableStateOf<String?>(null) }
    var filterByGameVersion by remember { mutableStateOf(true) }
    var selectedMcVersionOverride by remember { mutableStateOf<String?>(null) }
    var filterByLoader by remember { mutableStateOf(true) }
    var selectedLoaderOverride by remember { mutableStateOf<String?>(null) }
    var selectedSort by remember { mutableStateOf("relevance") }
    var selectedTimeWindow by remember { mutableStateOf("all") }
    // Chi 2 nha cung cap truc tuyen duy nhat: Modrinth & CurseForge (bi an hoan toan khi bat Local-Only)
    val observedSettings by SettingsManager.settings.collectAsState()
    val localOnlyMode = observedSettings.localOnlyMode
    val curseForgeAvailable = remember(observedSettings.disableCurseForge, localOnlyMode) { CurseForgeApi.isConfigured() }
    var browseSource by remember { mutableStateOf(NuxModManager.SOURCE_ALL) }
    val effectiveBrowseSource = if (localOnlyMode) {
        NuxModManager.SOURCE_MODRINTH
    } else if (curseForgeAvailable) {
        browseSource
    } else {
        NuxModManager.SOURCE_MODRINTH
    }
    val isCurseForge = !localOnlyMode && effectiveBrowseSource == NuxModManager.SOURCE_CURSEFORGE && !rustInstance
    val isAllSources = !localOnlyMode && effectiveBrowseSource == NuxModManager.SOURCE_ALL && !rustInstance
    // Mesin Rust: mod selalu dari katalog Horizon; shader dan resource pack dari Horizon atau Modrinth (pilihan pengguna)
    var rustSource by remember { mutableStateOf(NuxModManager.SOURCE_HORIZON) }
    val rustMods = rustInstance && activeType == HorizonContent.TYPE_MODS
    val rustHorizon = rustInstance && (rustMods || rustSource == NuxModManager.SOURCE_HORIZON)
    val activeSource = if (rustInstance) {
        if (rustHorizon) NuxModManager.SOURCE_HORIZON else NuxModManager.SOURCE_MODRINTH
    } else {
        effectiveBrowseSource
    }
    val sourceLabel = when {
        localOnlyMode -> "Bộ nhớ đệm cục bộ (Local-Only)"
        rustHorizon -> "Horizon"
        isAllSources -> "Modrinth & CurseForge"
        isCurseForge -> "CurseForge"
        else -> "Modrinth"
    }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showVersionMenu by remember { mutableStateOf(false) }
    var showLoaderMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showTimeWindowMenu by remember { mutableStateOf(false) }
    var currentPage by remember { mutableStateOf(1) }
    var totalHits by remember { mutableStateOf(0) }
    var downloadingProjectIds by remember { mutableStateOf<Map<String, String>>(emptyMap()) } // projectId -> status

    // Detail Modal state
    var detailModalHit by remember { mutableStateOf<ModrinthSearchHit?>(null) }
    var detailProject by remember { mutableStateOf<ModrinthProject?>(null) }
    var compatibleVersions by remember { mutableStateOf<List<ModrinthVersion>>(emptyList()) }
    var selectedVersion by remember { mutableStateOf<ModrinthVersion?>(null) }
    var isDetailLoading by remember { mutableStateOf(false) }
    var isDetailInstalling by remember { mutableStateOf(false) }
    var detailProgressText by remember { mutableStateOf("") }
    var detailTab by remember { mutableStateOf("overview") } // "overview" | "versions"
    var previewGalleryUrl by remember { mutableStateOf<String?>(null) }

    // Instance switch dropdown
    var showInstanceMenu by remember { mutableStateOf(false) }

    // Load available worlds for selected instance
    fun reloadWorlds() {
        val inst = selectedInstance ?: return
        val list = NuxModManager.getInstanceWorlds(context, inst)
        worlds = list
        if (list.isNotEmpty()) {
            if (selectedWorld.isBlank() || list.none { it.folderName == selectedWorld }) {
                selectedWorld = list.first().folderName
            }
        } else {
            selectedWorld = ""
        }
    }

    // Reload installed items
    fun reloadInstalled() {
        val inst = selectedInstance ?: return
        isInstalledLoading = true
        scope.launch {
            val targetWorld = if (activeType == "datapacks") selectedWorld else null
            installedItems = NuxModManager.getInstalledItems(context, inst, activeType, targetWorld)
            isInstalledLoading = false
        }
    }

    // Search Modrinth & CurseForge
    fun performSearch(page: Int = 1) {
        if (selectedCategory == MODS_CATEGORY_FAVORITES) {
            isSearching = false
            searchErrorMessage = null
            currentPage = 1
            return
        }
        val inst = selectedInstance ?: return
        isSearching = true
        searchErrorMessage = null
        val source = activeSource
        scope.launch {
            val cat = if (selectedCategory == "Semua") null else selectedCategory
            val res = NuxModManager.searchProjects(
                query = browseQuery,
                itemType = activeType,
                instance = inst,
                category = cat,
                filterByGameVersion = filterByGameVersion,
                page = page,
                limit = MODS_PAGE_SIZE,
                sortIndex = selectedSort,
                filterByLoader = filterByLoader,
                source = source,
                mcVersionOverride = selectedMcVersionOverride,
                loaderOverride = selectedLoaderOverride,
                timeWindow = selectedTimeWindow
            )
            if (res.isSuccess) {
                val data = res.getOrNull()
                searchHits = data?.hits ?: emptyList()
                totalHits = data?.totalHits ?: 0
                currentPage = page
                searchErrorMessage = null
                if (browseQuery.trim().length >= 2 && searchHits.isNotEmpty()) {
                    favViewModel.recordSearch(browseQuery, activeType)
                }
            } else {
                searchHits = emptyList()
                totalHits = 0
                val failure = res.exceptionOrNull()
                searchErrorMessage = failure?.message
                    ?: "Gagal terhubung ke $sourceLabel"
            }
            isSearching = false
        }
    }

    // Trigger reload when tab, type, world or instance changes
    LaunchedEffect(
        selectedInstance,
        activeType,
        mainMode,
        selectedWorld,
        filterByGameVersion,
        selectedMcVersionOverride,
        filterByLoader,
        selectedLoaderOverride,
        selectedSort,
        selectedTimeWindow,
        browseSource,
        rustSource,
        localOnlyMode
    ) {
        if (selectedInstance != null) {
            reloadWorlds()
            reloadInstalled()
            if (mainMode == "browse") {
                performSearch(1)
            }
        }
    }

    // Debounce search query
    var searchJob by remember { mutableStateOf<Job?>(null) }
    fun onQueryChanged(newQuery: String) {
        browseQuery = newQuery
        searchJob?.cancel()
        searchJob = scope.launch {
            delay(300)
            currentPage = 1
            performSearch(1)
        }
    }

    var isImportingModpack by remember { mutableStateOf(false) }
    var importDialogType by remember { mutableStateOf("modpacks") }
    var modpackImportStatus by remember { mutableStateOf("") }
    var modpackImportCurrent by remember { mutableStateOf(0) }
    var modpackImportTotal by remember { mutableStateOf(0) }
    var modpackImportName by remember { mutableStateOf("") }
    var showWorldImportChooser by remember { mutableStateOf(false) }

    // File Picker terpadu melalui NovaUnifiedFileImportEngine
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri>? ->
        val inst = selectedInstance
        if (!uris.isNullOrEmpty() && inst != null) {
            scope.launch {
                val targetWorld = if (activeType == "datapacks") selectedWorld else null

                // 1. Filter file berdasarkan ekstensi yang didukung lewat NovaUnifiedFileImportEngine
                val validUris = mutableListOf<Pair<Uri, String>>()
                val rejectedNames = mutableListOf<String>()

                for (uri in uris) {
                    val fileName = NovaUnifiedFileImportEngine.getFileName(context, uri)
                    if (NovaUnifiedFileImportEngine.isValidExtensionForType(activeType, fileName, rustInstance)) {
                        validUris.add(Pair(uri, fileName))
                    } else {
                        rejectedNames.add(fileName)
                    }
                }

                if (rejectedNames.isNotEmpty()) {
                    val hint = NovaUnifiedFileImportEngine.getAllowedExtensionLabel(activeType, rustInstance)
                    val msg = if (rejectedNames.size == 1) {
                        "File \"${rejectedNames.first()}\" diabaikan! Format yang didukung untuk $activeType hanya $hint"
                    } else {
                        "${rejectedNames.size} file diabaikan karena format tidak didukung ($hint)!"
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }

                if (validUris.isEmpty()) return@launch

                // 2. Import file yang valid
                if (activeType == "modpacks" || activeType == "worlds") {
                    importDialogType = activeType
                    isImportingModpack = true
                    var successCount = 0
                    var lastError: String? = null

                    for ((uri, fileName) in validUris) {
                        modpackImportName = fileName
                        modpackImportStatus = if (activeType == "worlds") {
                            "Menyiapkan arsip dunia..."
                        } else {
                            "Membaca arsip modpack..."
                        }
                        modpackImportCurrent = 0
                        modpackImportTotal = 0

                        val importRes = NovaUnifiedFileImportEngine.importLocalFile(
                            context = context,
                            instance = inst,
                            itemType = activeType,
                            uri = uri,
                            worldName = targetWorld,
                            onProgress = { statusText, cur, tot ->
                                modpackImportStatus = statusText
                                modpackImportCurrent = cur
                                modpackImportTotal = tot
                            }
                        )

                        if (importRes.isSuccess) {
                            successCount++
                        } else {
                            lastError = importRes.exceptionOrNull()?.message
                        }
                    }

                    isImportingModpack = false
                    reloadWorlds()
                    reloadInstalled()

                    if (successCount > 0) {
                        Toast.makeText(
                            context,
                            if (activeType == "worlds") {
                                "Berhasil mengimpor dan memasang $successCount dunia!"
                            } else {
                                "Berhasil mengimpor dan memasang $successCount modpack!"
                            },
                            Toast.LENGTH_SHORT
                        ).show()
                    } else if (lastError != null) {
                        Toast.makeText(
                            context,
                            if (activeType == "worlds") {
                                "Gagal mengimpor dunia: $lastError"
                            } else {
                                "Gagal mengimpor modpack: $lastError"
                            },
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    var successCount = 0
                    var lastError: String? = null

                    for ((uri, _) in validUris) {
                        val importRes = NovaUnifiedFileImportEngine.importLocalFile(
                            context = context,
                            instance = inst,
                            itemType = activeType,
                            uri = uri,
                            worldName = targetWorld
                        )
                        if (importRes.isSuccess) {
                            successCount++
                        } else {
                            lastError = importRes.exceptionOrNull()?.message
                        }
                    }

                    reloadInstalled()

                    if (successCount > 0) {
                        Toast.makeText(
                            context,
                            "Berhasil mengimpor $successCount file $activeType!",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else if (lastError != null) {
                        Toast.makeText(
                            context,
                            "Gagal mengimpor file: $lastError",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    // Pemilih folder dunia Minecraft Java (berisi level.dat + region/)
    val worldFolderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        val inst = selectedInstance
        if (treeUri != null && inst != null) {
            scope.launch {
                importDialogType = "worlds"
                modpackImportName = NovaUnifiedFileImportEngine.getFileName(context, treeUri)
                modpackImportStatus = "Memeriksa folder dunia Java..."
                modpackImportCurrent = 0
                modpackImportTotal = 0
                isImportingModpack = true

                val res = NovaUnifiedFileImportEngine.importJavaWorldFolder(
                    context = context,
                    instance = inst,
                    treeUri = treeUri,
                    onProgress = { statusText, cur, tot ->
                        modpackImportStatus = statusText
                        modpackImportCurrent = cur
                        modpackImportTotal = tot
                    }
                )

                isImportingModpack = false
                reloadWorlds()
                reloadInstalled()

                if (res.isSuccess) {
                    val worldTitle = res.getOrNull().orEmpty()
                    Toast.makeText(
                        context,
                        "Dunia $worldTitle berhasil dipasang!",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    val err = res.exceptionOrNull()?.message ?: "Terjadi kesalahan yang tidak diketahui"
                    Toast.makeText(
                        context,
                        "Gagal mengimpor dunia: $err",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    fun launchImportPicker() {
        if (selectedInstance == null) {
            Toast.makeText(context, "Pilih instance terlebih dahulu!", Toast.LENGTH_SHORT).show()
            return
        }
        if (activeType == "worlds" && !rustInstance) {
            showWorldImportChooser = true
        } else {
            filePicker.launch(NovaUnifiedFileImportEngine.getMimeTypesForType(activeType, rustInstance))
        }
    }

    // Apakah hasil pencarian ini sudah terpasang di instance aktif
    fun isHitInstalled(hit: ModrinthSearchHit): Boolean = installedItems.any { inst ->
        // Modul bawaan mesin Rust bukan hasil katalog; hanya berkas .hmod yang dihitung
        if (rustMods && !HorizonMods.removable(inst)) return@any false
        val titleMatches = inst.name.equals(hit.title, ignoreCase = true)
        val slugMatches = !hit.slug.isNullOrBlank() && inst.filename.contains(hit.slug, ignoreCase = true)
        val idMatches = inst.id.equals(hit.projectId, ignoreCase = true)
        titleMatches || slugMatches || idMatches
    }

    // Buka jendela detail untuk satu proyek Modrinth / CurseForge
    fun openDetail(hit: ModrinthSearchHit) {
        val inst = selectedInstance ?: return
        detailModalHit = hit
        detailProject = null
        detailTab = "overview"
        compatibleVersions = emptyList()
        selectedVersion = null
        isDetailLoading = true
        scope.launch {
            val pRes = NuxModManager.getProjectDetails(hit.projectId, horizon = rustHorizon)
            val vRes = NuxModManager.getCompatibleVersions(
                projectId = hit.projectId,
                instance = inst,
                itemType = activeType,
                filterByGameVersion = filterByGameVersion && !selectedMcVersionOverride.equals("all", ignoreCase = true),
                source = activeSource,
                mcVersionOverride = selectedMcVersionOverride,
                loaderOverride = selectedLoaderOverride,
                filterByLoader = filterByLoader && !selectedLoaderOverride.equals("all", ignoreCase = true)
            )
            detailProject = pRes.getOrNull()
            val vers = vRes.getOrNull() ?: emptyList()
            compatibleVersions = vers
            selectedVersion = vers.firstOrNull()
            isDetailLoading = false
        }
    }

    // Pasang cepat dari daftar (versi paling cocok + dependensi)
    fun quickInstall(hit: ModrinthSearchHit) {
        if (downloadingProjectIds.containsKey(hit.projectId)) return
        val inst = selectedInstance ?: return
        scope.launch {
            downloadingProjectIds = downloadingProjectIds + (hit.projectId to "Mencari versi...")
            val vRes = NuxModManager.getCompatibleVersions(
                projectId = hit.projectId,
                instance = inst,
                itemType = activeType,
                filterByGameVersion = filterByGameVersion && !selectedMcVersionOverride.equals("all", ignoreCase = true),
                source = activeSource,
                mcVersionOverride = selectedMcVersionOverride,
                loaderOverride = selectedLoaderOverride,
                filterByLoader = filterByLoader && !selectedLoaderOverride.equals("all", ignoreCase = true)
            )
            val compList = vRes.getOrNull()
            if (compList.isNullOrEmpty()) {
                val failure = vRes.exceptionOrNull()
                Toast.makeText(
                    context,
                    failure?.message ?: if (rustInstance) {
                        HorizonContent.MSG_NO_ZIP
                    } else {
                        "Tidak ada versi cocok untuk ${selectedMcVersionOverride ?: inst.mcVersion}!"
                    },
                    Toast.LENGTH_LONG
                ).show()
                downloadingProjectIds = downloadingProjectIds - hit.projectId
                return@launch
            }

            val targetVer = compList.first()
            downloadingProjectIds = downloadingProjectIds + (hit.projectId to "Mengunduh...")

            val targetWorld = if (activeType == "datapacks" || activeType == "worlds") selectedWorld else null
            val downRes = NuxModManager.downloadVersionAndDependencies(
                context = context,
                instance = inst,
                version = targetVer,
                itemType = activeType,
                worldName = targetWorld,
                onProgress = { status ->
                    downloadingProjectIds = downloadingProjectIds + (hit.projectId to status)
                }
            )

            downloadingProjectIds = downloadingProjectIds - hit.projectId

            if (downRes.isSuccess) {
                val downloaded = downRes.getOrNull() ?: emptyList()
                val depCount = (downloaded.size - 1).coerceAtLeast(0)
                val msg = when {
                    activeType == "worlds" -> "Dunia ${hit.title} berhasil dipasang!"
                    activeType == "modpacks" -> "Modpack ${hit.title} berhasil dipasang!"
                    depCount > 0 -> "Berhasil memasang ${hit.title} beserta $depCount dependensi!"
                    else -> "Berhasil memasang ${hit.title}!"
                }
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                reloadWorlds()
                reloadInstalled()
            } else {
                Toast.makeText(context, "Gagal mengunduh: ${downRes.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Pasang dari jendela detail (versi yang dipilih)
    fun installFromDetail(hit: ModrinthSearchHit) {
        val ver = selectedVersion ?: compatibleVersions.firstOrNull()
        val inst = selectedInstance
        if (ver == null || inst == null) return
        scope.launch {
            isDetailInstalling = true
            detailProgressText = "Memulai pengunduhan..."

            val targetWorld = if (activeType == "datapacks" || activeType == "worlds") selectedWorld else null
            val res = NuxModManager.downloadVersionAndDependencies(
                context = context,
                instance = inst,
                version = ver,
                itemType = activeType,
                worldName = targetWorld,
                onProgress = { status ->
                    detailProgressText = status
                }
            )

            isDetailInstalling = false

            if (res.isSuccess) {
                val files = res.getOrNull() ?: emptyList()
                val depCount = (files.size - 1).coerceAtLeast(0)
                val msg = when {
                    activeType == "worlds" -> "Dunia ${hit.title} berhasil dipasang!"
                    activeType == "modpacks" -> "Modpack ${hit.title} berhasil dipasang!"
                    depCount > 0 -> "Berhasil memasang ${hit.title} beserta $depCount dependensi!"
                    else -> "Berhasil memasang ${hit.title}!"
                }
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                reloadWorlds()
                reloadInstalled()
            } else {
                Toast.makeText(context, "Gagal mengunduh: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 10.dp)
    ) {
        val isNarrow = maxWidth < 640.dp
        val useTwoRowHeader = maxWidth < 900.dp

        Column(modifier = Modifier.fillMaxSize()) {
            // =========================================================================
            // NOVACRAFT STOREFRONT HEADER
            // =========================================================================
            if (useTwoRowHeader) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AmberNavPill(
                            active = "mods",
                            activeLabel = if (mainMode == "installed") "Vault ${modsTypeShortLabel(activeType)}" else "Store",
                            activeIcon = if (mainMode == "installed") Icons.Outlined.Inventory2 else Icons.Outlined.FileDownload,
                            onBack = onNavigateBack,
                            onMods = {},
                            onAccounts = onOpenAccounts,
                            onSettings = onOpenSettings
                        )
                        if (mainMode == "browse") {
                            if (!rustInstance && curseForgeAvailable) ModsSourcePill(
                                selectedSource = effectiveBrowseSource,
                                onSelect = { src ->
                                    if (src != browseSource) {
                                        browseSource = src
                                        if (selectedCategory != MODS_CATEGORY_FAVORITES) {
                                            selectedCategory = "Semua"
                                        }
                                        currentPage = 1
                                    }
                                }
                            ) else if (rustInstance && !rustMods) {
                                RustSourcePill(
                                    selectedSource = rustSource,
                                    onSelect = { src ->
                                        if (src != rustSource) {
                                            rustSource = src
                                            selectedCategory = "Semua"
                                            selectedSort = "relevance"
                                            currentPage = 1
                                        }
                                    }
                                )
                            }
                        } else {
                            ModsActionPill(
                                onAdd = { onOpenBrowse(activeType) },
                                onImport = { launchImportPicker() }
                            )
                        }
                    }
                    AmberSearchPill(
                        value = if (mainMode == "browse") browseQuery else installedSearch,
                        onValueChange = { if (mainMode == "browse") onQueryChanged(it) else installedSearch = it },
                        placeholder = if (mainMode == "browse") modsSearchPlaceholder(activeType) else "Cari di ${installedItems.size} item terpasang...",
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("mods_search_input")
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AmberNavPill(
                        active = "mods",
                        activeLabel = if (mainMode == "installed") "Vault ${modsTypeShortLabel(activeType)}" else "Store",
                        activeIcon = if (mainMode == "installed") Icons.Outlined.Inventory2 else Icons.Outlined.FileDownload,
                        onBack = onNavigateBack,
                        onMods = {},
                        onAccounts = onOpenAccounts,
                        onSettings = onOpenSettings
                    )
                    if (mainMode == "browse") {
                        if (!rustInstance && curseForgeAvailable) ModsSourcePill(
                            selectedSource = effectiveBrowseSource,
                            onSelect = { src ->
                                if (src != browseSource) {
                                    browseSource = src
                                    if (selectedCategory != MODS_CATEGORY_FAVORITES) {
                                        selectedCategory = "Semua"
                                    }
                                    currentPage = 1
                                }
                            }
                        ) else if (rustInstance && !rustMods) {
                            RustSourcePill(
                                selectedSource = rustSource,
                                onSelect = { src ->
                                    if (src != rustSource) {
                                        rustSource = src
                                        selectedCategory = "Semua"
                                        selectedSort = "relevance"
                                        currentPage = 1
                                    }
                                }
                            )
                        }
                    } else {
                        ModsActionPill(
                            onAdd = { onOpenBrowse(activeType) },
                            onImport = { launchImportPicker() }
                        )
                    }
                    AmberSearchPill(
                        value = if (mainMode == "browse") browseQuery else installedSearch,
                        onValueChange = { if (mainMode == "browse") onQueryChanged(it) else installedSearch = it },
                        placeholder = if (mainMode == "browse") modsSearchPlaceholder(activeType) else "Cari di ${installedItems.size} item terpasang...",
                        modifier = Modifier
                            .weight(1f)
                            .testTag("mods_search_input")
                    )
                }
            }

            val currentInst = selectedInstance
            if (currentInst == null) {
                ModsMessage(
                    icon = Icons.Outlined.ExtensionOff,
                    title = "Belum ada instance",
                    subtitle = "Pilih atau buat instance di Home untuk mulai memasang mod.",
                    actionLabel = "Kembali ke Home",
                    onAction = { onNavigateBack() }
                )
            } else {
                val categories = CATEGORIES_MAP[activeType] ?: listOf(ModCategoryItem("Semua", "Semua"))
                val selectedCategoryLabel = categories.firstOrNull { it.id == selectedCategory }?.label ?: "Semua"
                val sortOptions = if (isCurseForge || isAllSources) MODS_SORT_OPTIONS_CF else MODS_SORT_OPTIONS
                val selectedSortLabel = sortOptions.firstOrNull { it.first == selectedSort }?.second ?: "Relevansi"
                val canSwitchInstance = mainMode == "browse" && instances.size > 1

                if (isNarrow) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val chipShape = CircleShape
                        Row(
                            modifier = Modifier
                                .defaultMinSize(minHeight = 38.dp)
                                .clip(chipShape)
                                .amberGlass(chipShape)
                                .then(
                                    if (canSwitchInstance) {
                                        Modifier
                                            .clickable { showInstanceMenu = true }
                                            .testTag("mods_instance_picker")
                                    } else Modifier
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = currentInst.name,
                                color = AmberUi.Text,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                            if (canSwitchInstance) {
                                Icon(
                                    imageVector = Icons.Outlined.SwapHoriz,
                                    contentDescription = "Ganti instance",
                                    tint = AmberUi.AccentSoft,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        MODS_TYPE_ITEMS.filter { !rustInstance || HorizonContent.supports(it.first) }
                            .forEach { (typeId, typeLabel, typeIcon) ->
                                val picked = activeType == typeId
                                Row(
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 38.dp)
                                        .clip(chipShape)
                                        .background(
                                            if (picked) AmberUi.Accent.copy(alpha = 0.24f) else AmberUi.Surface,
                                            chipShape
                                        )
                                        .border(
                                            1.dp,
                                            if (picked) AmberUi.AccentEdge else AmberUi.Border,
                                            chipShape
                                        )
                                        .clickable {
                                            activeTypeState = typeId
                                            selectedCategory = "Semua"
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                        .testTag("mods_type_$typeId"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = typeIcon,
                                        contentDescription = null,
                                        tint = if (picked) AmberUi.AccentSoft else AmberUi.TextDim,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = typeLabel,
                                        color = if (picked) AmberUi.Text else AmberUi.TextDim,
                                        fontWeight = if (picked) FontWeight.Bold else FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                    }
                }

                // =========================================================================
                // BODY: DOCK KATEGORI KIRI + WORKSPACE ETALASE / VAULT KANAN
                // =========================================================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!isNarrow) {
                        // Left Storefront Category Dock
                        Column(
                            modifier = Modifier
                                .width(164.dp)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Target Instance Context Card
                            val targetShape = RoundedCornerShape(18.dp)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(targetShape)
                                    .amberGlass(targetShape)
                                    .then(
                                        if (canSwitchInstance) {
                                            Modifier
                                                .clickable { showInstanceMenu = true }
                                                .testTag("mods_instance_picker")
                                        } else Modifier
                                    )
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "TARGET INSTANCE",
                                        color = AmberUi.AccentSoft,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.9.sp
                                    )
                                    if (canSwitchInstance) {
                                        Icon(
                                            imageVector = Icons.Outlined.SwapHoriz,
                                            contentDescription = "Ganti instance",
                                            tint = AmberUi.AccentSoft,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = currentInst.name,
                                    color = AmberUi.Text,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "MC ${currentInst.mcVersion} · ${currentInst.loader.uppercase()}",
                                    color = AmberUi.TextDim,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Category Stack
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                MODS_TYPE_ITEMS.filter { !rustInstance || HorizonContent.supports(it.first) }
                                    .forEach { (typeId, typeLabel, typeIcon) ->
                                        StoreCategoryDockItem(
                                            icon = typeIcon,
                                            label = typeLabel,
                                            selected = activeType == typeId,
                                            onClick = {
                                                activeTypeState = typeId
                                                selectedCategory = "Semua"
                                            },
                                            modifier = Modifier.testTag("mods_type_$typeId")
                                        )
                                    }
                            }
                        }
                    }

            // Right Storefront Workspace
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Filter & Catalog Status Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (activeType == "datapacks") {
                            AmberFilterPill(
                                label = "World",
                                value = if (worlds.isNotEmpty()) selectedWorld else "Tidak ada world",
                                showChevron = worlds.isNotEmpty(),
                                onClick = { if (worlds.isNotEmpty()) showWorldMenu = true },
                                modifier = Modifier.testTag("mods_filter_world")
                            )
                        }

                        if (mainMode == "browse") {
                            if (!rustHorizon) {
                                AmberFilterPill(
                                    label = "Kategori",
                                    value = modsPlainLabel(selectedCategoryLabel),
                                    onClick = { showCategoryMenu = true },
                                    modifier = Modifier.testTag("mods_filter_category")
                                )
                            }
                            if (!rustInstance) {
                                val versionFilterLabel = when {
                                    !filterByGameVersion || selectedMcVersionOverride == "all" -> "Semua"
                                    !selectedMcVersionOverride.isNullOrBlank() -> selectedMcVersionOverride!!
                                    else -> "Auto (${currentInst.mcVersion})"
                                }
                                AmberFilterPill(
                                    label = "Versi",
                                    value = versionFilterLabel,
                                    onClick = { showVersionMenu = true },
                                    modifier = Modifier.testTag("mods_filter_version")
                                )
                            }
                            if (!rustInstance && (activeType == "mods" || activeType == "modpacks")) {
                                val loaderFilterLabel = when {
                                    !filterByLoader || selectedLoaderOverride == "all" -> "Semua"
                                    !selectedLoaderOverride.isNullOrBlank() -> modsLoaderTag(selectedLoaderOverride!!)
                                    else -> currentInst.loader.replaceFirstChar { it.uppercase() }
                                }
                                AmberFilterPill(
                                    label = "Loader",
                                    value = loaderFilterLabel,
                                    onClick = { showLoaderMenu = true },
                                    modifier = Modifier.testTag("mods_filter_loader")
                                )
                            }
                            AmberFilterPill(
                                label = "Urutkan",
                                value = selectedSortLabel,
                                onClick = { showSortMenu = true },
                                modifier = Modifier.testTag("mods_filter_sort")
                            )
                            if (!rustHorizon) {
                                val selectedTimeLabel = MODS_TIME_WINDOWS.firstOrNull { it.first == selectedTimeWindow }?.second ?: "Mọi lúc"
                                AmberFilterPill(
                                    label = "Thời gian",
                                    value = selectedTimeLabel,
                                    onClick = { showTimeWindowMenu = true },
                                    modifier = Modifier.testTag("mods_filter_time")
                                )
                            }
                            if (activeType == "worlds" && !rustInstance) {
                                AmberPillButton(
                                    text = "Impor Dunia",
                                    icon = Icons.Outlined.FileUpload,
                                    filled = false,
                                    onClick = { launchImportPicker() },
                                    modifier = Modifier.testTag("worlds_browse_import_button")
                                )
                            }
                        } else {
                            // Manage Mode summary pills
                            val enabledCount = installedItems.count { it.isEnabled }
                            StoreStatPill(
                                text = if (activeType == "worlds") "${installedItems.size} Dunia" else "${installedItems.size} Terpasang",
                                accent = AmberUi.AccentSoft
                            )
                            if (activeType != "worlds") {
                                StoreStatPill(
                                    text = "$enabledCount Aktif",
                                    accent = AmberUi.ModrinthGreen
                                )
                            }
                        }
                    }

                    if (mainMode == "browse" && localOnlyMode) {
                        StoreStatPill(
                            text = "Local-Only · Bộ nhớ đệm",
                            accent = NuxColors.Yellow
                        )
                    }
                    val shownResultCount = if (selectedCategory == MODS_CATEGORY_FAVORITES) {
                        allFavorites.count { it.itemType == activeType }
                    } else {
                        totalHits
                    }
                    if (mainMode == "browse" && shownResultCount > 0 && !isSearching) {
                        StoreStatPill(
                            text = "${formatCompactNumber(shownResultCount.toLong())} hasil",
                            accent = AmberUi.AccentSoft
                        )
                    }
                }

                // Thanh điều hướng bộ lọc & danh mục trong trình duyệt mod (Utility, Performance, Cosmetic, World Gen, Yêu thích)
                if (mainMode == "browse" && !rustHorizon) {
                    val favCountForType = remember(allFavorites, activeType) {
                        allFavorites.count { it.itemType == activeType }
                    }
                    ModsCategoryNavBar(
                        categories = categories,
                        selectedCategory = selectedCategory,
                        favoriteCount = favCountForType,
                        onSelectCategory = { catId ->
                            selectedCategory = catId
                            currentPage = 1
                            performSearch(1)
                        }
                    )
                    if (recentQueries.isNotEmpty() && browseQuery.isBlank() && selectedCategory != MODS_CATEGORY_FAVORITES) {
                        RecentModSearchesRow(
                            history = recentQueries,
                            onPickQuery = { q ->
                                browseQuery = q
                                currentPage = 1
                                performSearch(1)
                            },
                            onClear = { favViewModel.clearSearchHistory(activeType) }
                        )
                    }
                }

                if (rustInstance) {
                    RustNotice(type = activeType, horizon = rustHorizon)
                }

                // =====================================================================
                // DAFTAR VAULT (TERPASANG) ATAU ETALASE KATALOG (BROWSE)
                // =====================================================================
                if (mainMode == "installed") {
                    val filteredInstalled = remember(installedItems, installedSearch) {
                        if (installedSearch.isBlank()) installedItems
                        else installedItems.filter {
                            it.name.contains(installedSearch, ignoreCase = true) ||
                                it.filename.contains(installedSearch, ignoreCase = true)
                        }
                    }

                    if (isInstalledLoading) {
                        ModsLoading()
                    } else if (filteredInstalled.isEmpty()) {
                        ModsMessage(
                            icon = Icons.Outlined.FolderOpen,
                            title = if (activeType == "datapacks" && worlds.isEmpty())
                                "Belum ada world di instance ini. Buka game untuk membuat world terlebih dahulu."
                            else if (activeType == "worlds" && installedSearch.isBlank())
                                "Belum ada dunia tersimpan di instance ini"
                            else if (installedSearch.isBlank())
                                "Belum ada ${activeType} terpasang di instance ini"
                            else "Tidak ada item yang cocok",
                            actionLabel = if (activeType == "worlds") "Jelajahi Toko Dunia" else "Jelajahi Katalog",
                            onAction = { onOpenBrowse(activeType) }
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 260.dp),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 14.dp)
                        ) {
                            // Kunci wajib originalFilename agar file .jar & .jar.disabled tidak bentrok
                            gridItems(filteredInstalled, key = { it.originalFilename }) { item ->
                                val rustModItem = rustInstance && activeType == HorizonContent.TYPE_MODS
                                InstalledVaultCard(
                                    item = item,
                                    showToggle = activeType != "worlds",
                                    deletable = !rustModItem || HorizonMods.removable(item),
                                    subtitle = if (rustModItem && !HorizonMods.removable(item)) item.description else null,
                                    onToggle = {
                                        scope.launch {
                                            if (rustInstance) {
                                                val picked = NuxModManager.toggleEngineItem(context, activeType, item)
                                                val problem = picked.exceptionOrNull()
                                                if (problem != null) {
                                                    Toast.makeText(
                                                        context,
                                                        problem.message ?: HorizonContent.MSG_SELECT,
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            } else {
                                                val targetWorld = if (activeType == "datapacks") selectedWorld else null
                                                NuxModManager.toggleItemEnabled(context, currentInst, activeType, item, targetWorld)
                                            }
                                            reloadInstalled()
                                        }
                                    },
                                    onDelete = { itemToDelete = item }
                                )
                            }
                        }
                    }
                } else if (selectedCategory == MODS_CATEGORY_FAVORITES) {
                    val favoriteHits = remember(allFavorites, activeType, browseQuery, selectedSort) {
                        val base = allFavorites
                            .filter { it.itemType == activeType }
                            .map { it.toSearchHit() }
                        val filtered = if (browseQuery.isBlank()) {
                            base
                        } else {
                            val q = browseQuery.trim()
                            base.filter {
                                it.title.contains(q, ignoreCase = true) ||
                                    (it.author?.contains(q, ignoreCase = true) == true) ||
                                    (it.description?.contains(q, ignoreCase = true) == true)
                            }
                        }
                        when (selectedSort) {
                            "downloads" -> filtered.sortedByDescending { it.downloads }
                            "follows" -> filtered.sortedByDescending { it.follows }
                            "updated", "newest" -> filtered.sortedByDescending { it.dateModified ?: it.dateCreated ?: "" }
                            else -> filtered
                        }
                    }
                    if (favoriteHits.isEmpty()) {
                        ModsMessage(
                            icon = Icons.Outlined.FavoriteBorder,
                            iconTint = Color(0xFFFF4D6D),
                            title = if (browseQuery.isBlank()) "Chưa có mục yêu thích nào" else "Không tìm thấy mod yêu thích phù hợp",
                            subtitle = "Nhấn biểu tượng trái tim trên mod trong cửa hàng để lưu vào cơ sở dữ liệu nội bộ để truy cập nhanh.",
                            actionLabel = "Khám phá tất cả",
                            onAction = {
                                selectedCategory = "Semua"
                                browseQuery = ""
                                currentPage = 1
                                performSearch(1)
                            }
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 260.dp),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 14.dp)
                        ) {
                            gridItems(favoriteHits, key = { it.projectId }) { hit ->
                                ModStoreCard(
                                    hit = hit,
                                    isInstalled = isHitInstalled(hit),
                                    isDownloading = downloadingProjectIds.containsKey(hit.projectId),
                                    downloadStatus = downloadingProjectIds[hit.projectId] ?: "",
                                    isFavorite = favoriteIds.contains(hit.projectId),
                                    showSourceBadge = !rustHorizon,
                                    onOpen = { openDetail(hit) },
                                    onInstall = { quickInstall(hit) },
                                    onToggleFavorite = {
                                        favViewModel.toggleFavorite(hit, activeType) { saved ->
                                            Toast.makeText(
                                                context,
                                                if (saved) "Đã lưu vào Yêu thích" else "Đã xóa khỏi Yêu thích",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                )
                            }
                        }
                    }
                } else {
                    if (isSearching) {
                        ModsLoading(text = "Memuat katalog dari $sourceLabel...")
                    } else if (searchErrorMessage != null) {
                        ModsMessage(
                            icon = Icons.Outlined.CloudOff,
                            iconTint = NuxColors.ErrorRed,
                            title = "Gagal memuat dari $sourceLabel",
                            subtitle = searchErrorMessage ?: "Terjadi kesalahan jaringan",
                            actionLabel = "Coba lagi",
                            onAction = { performSearch(currentPage) }
                        )
                    } else if (searchHits.isEmpty()) {
                        ModsMessage(
                            icon = if (localOnlyMode) Icons.Outlined.CloudOff else Icons.Outlined.SearchOff,
                            title = if (localOnlyMode) {
                                "Chưa có mod nào trong bộ nhớ đệm cục bộ"
                            } else if (filterByGameVersion && !rustInstance) {
                                "Tidak ada hasil cocok untuk Minecraft ${currentInst.mcVersion} (${currentInst.loader.uppercase()})"
                            } else {
                                "Tidak ada hasil ditemukan untuk kata kunci ini"
                            },
                            subtitle = if (localOnlyMode) {
                                "Chế độ Local-Only đang ẩn trình duyệt CurseForge trực tuyến và chỉ đọc dữ liệu đã lưu cục bộ. Tắt Local-Only trong Cài đặt khi có mạng hoặc bỏ lọc phiên bản."
                            } else if (rustInstance && rustHorizon && !rustMods) {
                                "Coba ubah kata kunci atau pindah ke sumber Modrinth"
                            } else if (rustInstance) {
                                "Coba ubah kata kunci"
                            } else {
                                "Coba ubah kata kunci atau nonaktifkan filter versi"
                            },
                            actionLabel = if (filterByGameVersion && !rustInstance) "Cari di semua versi Minecraft" else null,
                            onAction = {
                                filterByGameVersion = false
                                selectedMcVersionOverride = null
                                currentPage = 1
                                performSearch(1)
                            }
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 260.dp),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 14.dp)
                        ) {
                            gridItems(searchHits, key = { it.projectId }) { hit ->
                                ModStoreCard(
                                    hit = hit,
                                    isInstalled = isHitInstalled(hit),
                                    isDownloading = downloadingProjectIds.containsKey(hit.projectId),
                                    downloadStatus = downloadingProjectIds[hit.projectId] ?: "",
                                    isFavorite = favoriteIds.contains(hit.projectId),
                                    showSourceBadge = !rustHorizon,
                                    onOpen = { openDetail(hit) },
                                    onInstall = { quickInstall(hit) },
                                    onToggleFavorite = {
                                        favViewModel.toggleFavorite(hit, activeType) { saved ->
                                            Toast.makeText(
                                                context,
                                                if (saved) "Đã lưu vào Yêu thích" else "Đã xóa khỏi Yêu thích",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                )
                            }

                            item(span = { GridItemSpan(maxLineSpan) }) {
                                val pageStep = if (isAllSources) MODS_PAGE_SIZE * 2 else MODS_PAGE_SIZE
                                val totalPages = ((totalHits + pageStep - 1) / pageStep).coerceAtLeast(1)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp, bottom = 4.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AmberPillButton(
                                        text = "Sebelumnya",
                                        filled = false,
                                        enabled = currentPage > 1,
                                        onClick = { performSearch(currentPage - 1) },
                                        modifier = Modifier.testTag("mods_page_prev")
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Box(
                                        modifier = Modifier
                                            .height(36.dp)
                                            .clip(CircleShape)
                                            .amberGlass(CircleShape)
                                            .padding(horizontal = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$currentPage / $totalPages",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = AmberUi.Text
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    AmberPillButton(
                                        text = "Selanjutnya",
                                        filled = false,
                                        enabled = currentPage < totalPages,
                                        onClick = { performSearch(currentPage + 1) },
                                        modifier = Modifier.testTag("mods_page_next")
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

    // --- PILIH INSTANCE ---
    if (showInstanceMenu) {
        AmberPickerDialog(
            title = "Pilih instance",
            options = instances.map { it.id to "${it.name} · ${it.mcVersion} · ${it.loader.uppercase()}" },
            selectedId = selectedInstance?.id,
            onPick = { id ->
                instances.firstOrNull { it.id == id }?.let { InstanceManager.selectInstance(it) }
                showInstanceMenu = false
            },
            onDismiss = { showInstanceMenu = false }
        )
    }

    // --- PILIH WORLD (DATAPACK) ---
    if (showWorldMenu) {
        AmberPickerDialog(
            title = "Pilih world",
            options = worlds.map { it.folderName to it.displayName },
            selectedId = selectedWorld,
            onPick = { id ->
                selectedWorld = id
                showWorldMenu = false
                reloadInstalled()
            },
            onDismiss = { showWorldMenu = false }
        )
    }

    // --- PILIH KATEGORI / VERSI / LOADER / URUTAN ---
    if (showCategoryMenu) {
        AmberPickerDialog(
            title = "Pilih kategori",
            options = (CATEGORIES_MAP[activeType] ?: listOf(ModCategoryItem("Semua", "Semua"))).map { it.id to it.label },
            selectedId = selectedCategory,
            onPick = { id ->
                selectedCategory = id
                showCategoryMenu = false
                currentPage = 1
                performSearch(1)
            },
            onDismiss = { showCategoryMenu = false }
        )
    }
    if (showVersionMenu) {
        val currentInstMc = selectedInstance?.mcVersion.orEmpty()
        val versionOptions = buildList {
            add("auto" to "Auto ($currentInstMc)")
            add("all" to "Semua versi")
            MODS_POPULAR_MC_VERSIONS.filter { it != currentInstMc }.forEach { mc ->
                add(mc to "Minecraft $mc")
            }
        }
        val selectedVerId = when {
            !filterByGameVersion || selectedMcVersionOverride == "all" -> "all"
            !selectedMcVersionOverride.isNullOrBlank() -> selectedMcVersionOverride
            else -> "auto"
        }
        AmberPickerDialog(
            title = "Pilih versi",
            options = versionOptions,
            selectedId = selectedVerId,
            onPick = { id ->
                when (id) {
                    "auto" -> {
                        filterByGameVersion = true
                        selectedMcVersionOverride = null
                    }
                    "all" -> {
                        filterByGameVersion = false
                        selectedMcVersionOverride = null
                    }
                    else -> {
                        filterByGameVersion = true
                        selectedMcVersionOverride = id
                    }
                }
                currentPage = 1
                showVersionMenu = false
            },
            onDismiss = { showVersionMenu = false }
        )
    }
    if (showLoaderMenu) {
        val loaderOptions = buildList {
            add("auto" to "Auto (${(selectedInstance?.loader ?: "").uppercase()})")
            add("all" to "Semua loader")
            addAll(MODS_LOADER_CHOICES)
        }
        val selectedLoaderId = when {
            !filterByLoader || selectedLoaderOverride == "all" -> "all"
            !selectedLoaderOverride.isNullOrBlank() -> selectedLoaderOverride
            else -> "auto"
        }
        AmberPickerDialog(
            title = "Pilih loader",
            options = loaderOptions,
            selectedId = selectedLoaderId,
            onPick = { id ->
                when (id) {
                    "auto" -> {
                        filterByLoader = true
                        selectedLoaderOverride = null
                    }
                    "all" -> {
                        filterByLoader = false
                        selectedLoaderOverride = null
                    }
                    else -> {
                        filterByLoader = true
                        selectedLoaderOverride = id
                    }
                }
                currentPage = 1
                showLoaderMenu = false
            },
            onDismiss = { showLoaderMenu = false }
        )
    }
    if (showSortMenu) {
        AmberPickerDialog(
            title = "Urutkan",
            options = if (isCurseForge || isAllSources) MODS_SORT_OPTIONS_CF else MODS_SORT_OPTIONS,
            selectedId = selectedSort,
            onPick = { id ->
                selectedSort = id
                currentPage = 1
                showSortMenu = false
            },
            onDismiss = { showSortMenu = false }
        )
    }
    if (showTimeWindowMenu) {
        AmberPickerDialog(
            title = "Khoảng thời gian",
            options = MODS_TIME_WINDOWS,
            selectedId = selectedTimeWindow,
            onPick = { id ->
                selectedTimeWindow = id
                currentPage = 1
                showTimeWindowMenu = false
            },
            onDismiss = { showTimeWindowMenu = false }
        )
    }
    // --- PILIH METODE IMPOR DUNIA JAVA ---
    if (showWorldImportChooser) {
        JavaWorldImportDialog(
            onPickZip = {
                showWorldImportChooser = false
                filePicker.launch(NovaUnifiedFileImportEngine.getMimeTypesForType("worlds", false))
            },
            onPickFolder = {
                showWorldImportChooser = false
                worldFolderPicker.launch(null)
            },
            onDismiss = { showWorldImportChooser = false }
        )
    }
    // --- DIALOG HAPUS ---
    itemToDelete?.let { item ->
        NuxDialog(
            onDismissRequest = { itemToDelete = null },
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .wrapContentHeight(),
            compact = true
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(NuxColors.ErrorRed.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        tint = NuxColors.ErrorRed,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (activeType == "modpacks") "Hapus modpack?" else "Hapus ${item.category}?",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = NuxColors.DarkGray
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (activeType == "modpacks") {
                        "Yakin ingin menghapus modpack \"${item.name}\"? Semua mod yang terpasang dari modpack ini juga akan dihapus dari instance."
                    } else {
                        "Yakin ingin menghapus \"${item.name}\" dari instance ini?"
                    },
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = NuxColors.GrayNeutral,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NuxButton(
                        onClick = { itemToDelete = null },
                        backgroundColor = Color.Transparent,
                        cornerRadius = 20.dp,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("mod_delete_cancel_button")
                    ) {
                        Text("Batal", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    NuxButton(
                        onClick = {
                            val targetInst = selectedInstance
                            if (targetInst != null) {
                                scope.launch {
                                    val targetWorld = if (activeType == "datapacks") selectedWorld else null
                                    NuxModManager.deleteItem(context, targetInst, activeType, item, targetWorld)
                                    itemToDelete = null
                                    reloadInstalled()
                                    val toastMsg = if (activeType == "modpacks") {
                                        "Modpack dan semua mod berhasil dihapus!"
                                    } else {
                                        "${item.name} berhasil dihapus!"
                                    }
                                    Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        backgroundColor = NuxColors.ErrorRed,
                        contentColor = NuxColors.DarkGray,
                        cornerRadius = 20.dp,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("mod_delete_confirm_button")
                    ) {
                        Text("Hapus", fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }
                }
            }
        }
    }

    // --- PROGRES IMPOR MODPACK ---
    if (isImportingModpack) {
        NuxDialog(
            onDismissRequest = { /* Tidak bisa ditutup saat sedang mengunduh */ },
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .wrapContentHeight(),
            compact = true
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(NuxColors.YellowPale)
                        .border(NuxSizes.GlassBorder, NuxColors.Yellow.copy(alpha = 0.45f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    AmberSpinner(
                        modifier = Modifier.size(22.dp),
                        color = NuxColors.Yellow,
                        strokeWidth = 2.5.dp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (importDialogType == "worlds") {
                        "Mengimpor & memasang dunia"
                    } else {
                        "Mengimpor & memasang modpack"
                    },
                    fontWeight = FontWeight.Black,
                    fontSize = 15.5.sp,
                    color = NuxColors.DarkGray
                )

                if (modpackImportName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = modpackImportName,
                        fontSize = 12.sp,
                        color = NuxColors.Yellow,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (modpackImportStatus.isNotBlank()) {
                        modpackImportStatus
                    } else if (importDialogType == "worlds") {
                        "Mengekstrak arsip dunia ke folder saves..."
                    } else {
                        "Membaca daftar mod dan mengunduh file..."
                    },
                    fontSize = 12.sp,
                    color = NuxColors.GrayNeutral,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (modpackImportTotal > 0) {
                    val progressRatio = (modpackImportCurrent.toFloat() / modpackImportTotal.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progressRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = NuxColors.Yellow,
                        trackColor = NuxColors.SurfaceElevated
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (importDialogType == "worlds") {
                            "$modpackImportCurrent / $modpackImportTotal entri"
                        } else {
                            "$modpackImportCurrent / $modpackImportTotal file mod"
                        },
                        fontSize = 11.sp,
                        color = NuxColors.GrayNeutral
                    )
                } else {
                    AmberWaitBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = NuxColors.Yellow,
                        trackColor = NuxColors.SurfaceElevated
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (importDialogType == "worlds") {
                        "Launcher sedang memverifikasi level.dat dan mengekstrak dunia ke saves/. Mohon tunggu."
                    } else {
                        "Launcher sedang membaca daftar mod dan mengunduh modpack. Mohon tunggu."
                    },
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = NuxColors.GrayNeutral.copy(alpha = 0.75f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }

    // --- DETAIL PROYEK MODRINTH ---
    detailModalHit?.let { hit ->
        ModDetailDialog(
            hit = hit,
            project = detailProject,
            versions = compatibleVersions,
            selectedVersion = selectedVersion,
            isLoading = isDetailLoading,
            isInstalling = isDetailInstalling,
            progressText = detailProgressText,
            isInstalled = isHitInstalled(hit),
            isFavorite = favoriteIds.contains(hit.projectId),
            tab = detailTab,
            instance = selectedInstance,
            onTab = { detailTab = it },
            onPickVersion = { selectedVersion = it },
            onOpenGallery = { previewGalleryUrl = it },
            onInstall = { installFromDetail(hit) },
            onToggleFavorite = {
                favViewModel.toggleFavorite(hit, activeType) { saved ->
                    Toast.makeText(
                        context,
                        if (saved) "Đã lưu vào Yêu thích" else "Đã xóa khỏi Yêu thích",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onClose = { detailModalHit = null }
        )
    }

    // --- PRATINJAU GAMBAR ---
    previewGalleryUrl?.let { url ->
        NuxDialog(onDismissRequest = { previewGalleryUrl = null }, compact = true) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tangkapan layar",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = NuxColors.DarkGray
                    )
                    ModsIconButton(
                        icon = Icons.Outlined.Close,
                        description = "Tutup",
                        onClick = { previewGalleryUrl = null }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                NuxNetworkImage(
                    model = url,
                    contentDescription = "Pratinjau gambar",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(NuxSizes.GlassBorder, NuxColors.CardBorder, RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    maxSidePx = 1280,
                    showSpinner = true
                )
            }
        }
    }
}

// ============================================================================
// KOMPONEN ETALASE & VAULT NOVACRAFT
// ============================================================================

@Composable
private fun StoreCategoryDockItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clip(shape)
            .then(
                if (selected) {
                    Modifier
                        .background(AmberUi.Accent.copy(alpha = 0.24f), shape)
                        .border(1.2.dp, AmberUi.AccentEdge, shape)
                } else {
                    Modifier.amberGlass(shape)
                }
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(20.dp)
                .clip(CircleShape)
                .background(if (selected) AmberUi.AccentSoft else Color.Transparent)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(if (selected) AmberUi.Accent else AmberUi.SurfaceRaised),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color.White else AmberUi.AccentSoft,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(9.dp))
        Text(
            text = label,
            color = if (selected) AmberUi.Text else AmberUi.TextDim,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StoreStatPill(
    text: String,
    accent: Color
) {
    Row(
        modifier = Modifier
            .defaultMinSize(minHeight = 34.dp)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.14f), CircleShape)
            .border(1.dp, accent.copy(alpha = 0.35f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(accent)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            color = AmberUi.Text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun ModsIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = NuxColors.GrayNeutral,
    ring: Boolean = true,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .then(
                    if (ring) {
                        Modifier
                            .background(NuxColors.SurfaceElevated, CircleShape)
                            .border(NuxSizes.GlassBorder, NuxColors.CardBorder, CircleShape)
                    } else {
                        Modifier
                    }
                ),
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
}

private val MODS_TYPE_ITEMS = listOf(
    Triple("modpacks", "Modpack", Icons.Outlined.Layers),
    Triple("worlds", "Dunia", Icons.Outlined.Terrain),
    Triple("mods", "Mod", Icons.Outlined.ViewInAr),
    Triple("shaderpacks", "Shader", Icons.Outlined.LightMode),
    Triple("resourcepacks", "Resource Pack", Icons.Outlined.Image),
    Triple("datapacks", "Datapack", Icons.Outlined.Public)
)

private const val MODS_PAGE_SIZE = 24

private val MODS_POPULAR_MC_VERSIONS = listOf(
    "1.21.5", "1.21.4", "1.21.1", "1.20.6", "1.20.4", "1.20.1",
    "1.19.4", "1.19.2", "1.18.2", "1.16.5", "1.12.2", "1.8.9", "1.7.10"
)

private val MODS_LOADER_CHOICES = listOf(
    "fabric" to "Fabric",
    "forge" to "Forge",
    "neoforge" to "NeoForge",
    "quilt" to "Quilt"
)

private val MODS_SORT_OPTIONS = listOf(
    "relevance" to "Relevansi",
    "downloads" to "Unduhan",
    "follows" to "Pengikut",
    "newest" to "Terbaru",
    "updated" to "Diperbarui"
)

private val MODS_SORT_OPTIONS_CF = listOf(
    "relevance" to "Relevansi",
    "downloads" to "Unduhan",
    "follows" to "Populer",
    "updated" to "Cập nhật gần đây",
    "newest" to "Mới phát hành"
)

private val MODS_TIME_WINDOWS = listOf(
    "all" to "Mọi lúc",
    "day" to "24 giờ qua",
    "week" to "7 ngày qua",
    "month" to "30 ngày qua",
    "year" to "Năm nay"
)

private fun formatRelativeTimeVi(iso: String?): String? {
    if (iso.isNullOrBlank()) return null
    return try {
        val ms = java.time.Instant.parse(iso.trim()).toEpochMilli()
        val diffSec = ((System.currentTimeMillis() - ms) / 1000L).coerceAtLeast(0L)
        when {
            diffSec < 3600L -> "${(diffSec / 60L).coerceAtLeast(1L)}p trước"
            diffSec < 86_400L -> "${diffSec / 3600L}g trước"
            diffSec < 30L * 86_400L -> "${diffSec / 86_400L} ngày"
            diffSec < 365L * 86_400L -> "${diffSec / (30L * 86_400L)} tháng"
            else -> "${diffSec / (365L * 86_400L)} năm"
        }
    } catch (_: Exception) {
        null
    }
}

private fun formatShortDateVi(iso: String?): String? {
    if (iso.isNullOrBlank()) return null
    return try {
        val instant = java.time.Instant.parse(iso.trim())
        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
            .withZone(java.time.ZoneId.systemDefault())
            .format(instant)
    } catch (_: Exception) {
        null
    }
}

private val MODS_LOADER_IDS = setOf("fabric", "forge", "neoforge", "quilt")

private fun modsTypeShortLabel(type: String): String = when (type) {
    "modpacks" -> "Modpack"
    "worlds" -> "Dunia"
    "shaderpacks" -> "Shader"
    "resourcepacks" -> "Resource"
    "datapacks" -> "Datapack"
    else -> "Mod"
}

private fun modsSearchPlaceholder(type: String): String = when (type) {
    "modpacks" -> "Cari modpack global & Việt Nam..."
    "worlds" -> "Cari dunia & map global..."
    "shaderpacks" -> "Cari shader global & Việt Nam..."
    "resourcepacks" -> "Cari resource pack global..."
    "datapacks" -> "Cari datapack global..."
    else -> "Cari mod global & Việt Nam..."
}

private fun modsPlainLabel(label: String): String {
    val firstSpace = label.indexOf(' ')
    return if (firstSpace > 0 && !label[0].isLetterOrDigit()) label.substring(firstSpace + 1) else label
}

private fun modsCategoryTag(id: String): String = when (id) {
    "optimization" -> "Optimasi"
    "technology" -> "Teknologi"
    "magic" -> "Sihir"
    "adventure" -> "Petualangan"
    "utility" -> "Utilitas"
    "decoration" -> "Dekorasi"
    "equipment" -> "Perlengkapan"
    "food" -> "Makanan"
    "storage" -> "Penyimpanan"
    "library" -> "Pustaka"
    "lightweight" -> "Ringan"
    "realistic" -> "Realistis"
    "fantasy" -> "Fantasi"
    else -> id.replace('-', ' ').replaceFirstChar { it.uppercase() }
}

private fun modsLoaderTag(id: String): String = when (id) {
    "neoforge" -> "NeoForge"
    else -> id.replaceFirstChar { it.uppercase() }
}

private fun modsTagsFor(hit: ModrinthSearchHit): List<String> {
    val all = (hit.displayCategories ?: hit.categories ?: emptyList()).map { it.lowercase() }.distinct()
    val loaders = all.filter { it in MODS_LOADER_IDS }
    val topics = all.filter { it !in MODS_LOADER_IDS }
    return topics.take(2).map { modsCategoryTag(it) } + loaders.take(2).map { modsLoaderTag(it) }
}

@Composable
private fun ModsSourcePill(
    selectedSource: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .defaultMinSize(minHeight = 44.dp)
            .clip(CircleShape)
            .amberGlass(CircleShape)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ModsSourceChip(
            selected = selectedSource == NuxModManager.SOURCE_ALL,
            iconRes = null,
            iconSize = 0.dp,
            label = "Tất cả",
            onClick = { onSelect(NuxModManager.SOURCE_ALL) },
            modifier = Modifier.testTag("mods_source_all")
        )
        ModsSourceChip(
            selected = selectedSource == NuxModManager.SOURCE_MODRINTH,
            iconRes = com.aistudio.novacraft.launcher.R.drawable.ic_source_modrinth,
            iconSize = 18.dp,
            label = "Modrinth",
            onClick = { onSelect(NuxModManager.SOURCE_MODRINTH) },
            modifier = Modifier.testTag("mods_source_modrinth")
        )
        ModsSourceChip(
            selected = selectedSource == NuxModManager.SOURCE_CURSEFORGE,
            iconRes = com.aistudio.novacraft.launcher.R.drawable.ic_source_curseforge,
            iconSize = 16.dp,
            label = "CurseForge",
            onClick = { onSelect(NuxModManager.SOURCE_CURSEFORGE) },
            modifier = Modifier.testTag("mods_source_curseforge")
        )
    }
}

@Composable
private fun RustSourcePill(
    selectedSource: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .defaultMinSize(minHeight = 44.dp)
            .clip(CircleShape)
            .amberGlass(CircleShape)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ModsSourceChip(
            selected = selectedSource == NuxModManager.SOURCE_HORIZON,
            iconRes = null,
            iconSize = 0.dp,
            label = "Horizon",
            onClick = { onSelect(NuxModManager.SOURCE_HORIZON) },
            modifier = Modifier.testTag("mods_source_horizon")
        )
        ModsSourceChip(
            selected = selectedSource == NuxModManager.SOURCE_MODRINTH,
            iconRes = com.aistudio.novacraft.launcher.R.drawable.ic_source_modrinth,
            iconSize = 18.dp,
            label = "Modrinth",
            onClick = { onSelect(NuxModManager.SOURCE_MODRINTH) },
            modifier = Modifier.testTag("mods_source_modrinth")
        )
    }
}

@Composable
private fun ModsSourceChip(
    selected: Boolean,
    iconRes: Int?,
    iconSize: androidx.compose.ui.unit.Dp,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(36.dp)
            .clip(CircleShape)
            .then(
                if (selected) {
                    Modifier
                        .background(AmberUi.Accent.copy(alpha = 0.28f), CircleShape)
                        .border(1.dp, AmberUi.AccentEdge, CircleShape)
                } else {
                    Modifier
                }
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (iconRes != null) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = if (selected) AmberUi.AccentSoft else AmberUi.TextDim,
                modifier = Modifier.size(iconSize)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = label,
            color = if (selected) AmberUi.Text else AmberUi.TextDim,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            fontSize = 13.sp,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun JavaWorldImportDialog(
    onPickZip: () -> Unit,
    onPickFolder: () -> Unit,
    onDismiss: () -> Unit
) {
    NuxDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 460.dp)
            .wrapContentHeight(),
        compact = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Impor Dunia Java",
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                color = NuxColors.DarkGray
            )
            Text(
                text = "Pilih arsip .zip atau folder dunia Minecraft Java (berisi level.dat).",
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = NuxColors.GrayNeutral
            )
            val optionShape = RoundedCornerShape(14.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 56.dp)
                    .clip(optionShape)
                    .background(NuxColors.SurfaceElevated, optionShape)
                    .border(1.dp, NuxColors.CardBorder, optionShape)
                    .clickable { onPickZip() }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("world_import_zip_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.FolderZip,
                    contentDescription = null,
                    tint = AmberUi.AccentSoft,
                    modifier = Modifier.size(22.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Arsip Dunia Java (.zip)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = NuxColors.DarkGray
                    )
                    Text(
                        text = "Pilih berkas .zip map Minecraft Java",
                        fontSize = 11.sp,
                        color = NuxColors.GrayNeutral
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 56.dp)
                    .clip(optionShape)
                    .background(NuxColors.SurfaceElevated, optionShape)
                    .border(1.dp, NuxColors.CardBorder, optionShape)
                    .clickable { onPickFolder() }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("world_import_folder_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CreateNewFolder,
                    contentDescription = null,
                    tint = AmberUi.ModrinthGreen,
                    modifier = Modifier.size(22.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Folder Dunia Java",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = NuxColors.DarkGray
                    )
                    Text(
                        text = "Pilih folder save yang berisi level.dat & region/",
                        fontSize = 11.sp,
                        color = NuxColors.GrayNeutral
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                NuxButton(
                    onClick = onDismiss,
                    backgroundColor = Color.Transparent,
                    cornerRadius = 20.dp,
                    modifier = Modifier.testTag("world_import_cancel_button")
                ) {
                    Text("Batal", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun ModsActionPill(
    onAdd: () -> Unit,
    onImport: () -> Unit
) {
    Row(
        modifier = Modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(CircleShape)
            .amberGlass(CircleShape)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AmberPillButton(
            text = "Tambah",
            icon = Icons.Outlined.Add,
            onClick = onAdd,
            modifier = Modifier.testTag("mods_browse_button")
        )
        AmberPillButton(
            text = "Impor",
            icon = Icons.Outlined.FileUpload,
            filled = false,
            onClick = onImport,
            modifier = Modifier.testTag("mods_import_button")
        )
    }
}

@Composable
private fun ModsTagChip(text: String) {
    Box(
        modifier = Modifier
            .height(22.dp)
            .clip(CircleShape)
            .background(AmberUi.SurfaceRaised.copy(alpha = 0.6f), CircleShape)
            .border(1.dp, AmberUi.Border, CircleShape)
            .padding(horizontal = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = AmberUi.TextDim,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun RustNotice(type: String, horizon: Boolean) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(shape)
            .background(AmberUi.Surface, shape)
            .border(1.dp, AmberUi.Border, shape)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = "Mesin Rust: hidupkan saklar di daftar Terpasang untuk memakainya. Berlaku saat game dibuka lagi.",
            color = AmberUi.TextDim,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
        if (type == HorizonContent.TYPE_SHADERS) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Shader di mesin Rust masih tahap uji dan bisa memberatkan HP.",
                color = AmberUi.TextDim,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
        if (type == HorizonContent.TYPE_SHADERS || type == HorizonContent.TYPE_RESOURCES) {
            val sourceNote = when {
                type == HorizonContent.TYPE_SHADERS && horizon -> "Sumber Horizon: shader asli mesin Rust (.hshader)."
                type == HorizonContent.TYPE_SHADERS -> "Sumber Modrinth: shader klasik, kompatibilitas dengan mesin Rust masih terbatas."
                horizon -> "Sumber Horizon: resource pack yang dibuat atau diuji untuk mesin Rust."
                else -> null
            }
            if (sourceNote != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = sourceNote,
                    color = AmberUi.TextDim,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }
        if (type == HorizonContent.TYPE_MODS) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Mod mesin Rust berformat .hmod (bukan .jar Fabric atau Forge). Mod berkode dipasang dalam keadaan mati; hidupkan setelah memeriksa izinnya di Options > Mods dalam game.",
                color = AmberUi.TextDim,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
            if (!android.os.Process.is64Bit()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = HorizonMods.MSG_NEED_64,
                    color = AmberUi.TextDim,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.ModsMessage(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    iconTint: Color = AmberUi.TextDim,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .clip(RoundedCornerShape(24.dp))
                .amberGlass(RoundedCornerShape(24.dp))
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                color = AmberUi.Text,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    color = AmberUi.TextDim,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            if (actionLabel != null) {
                Spacer(modifier = Modifier.height(14.dp))
                AmberPillButton(text = actionLabel, onClick = onAction)
            }
        }
    }
}

@Composable
private fun ColumnScope.ModsLoading(text: String? = null) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AmberSpinner(
                color = AmberUi.AccentSoft,
                modifier = Modifier.size(30.dp)
            )
            if (text != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(text, color = AmberUi.TextDim, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ModsCategoryNavBar(
    categories: List<ModCategoryItem>,
    selectedCategory: String,
    favoriteCount: Int,
    onSelectCategory: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        categories.forEach { cat ->
            val picked = selectedCategory == cat.id
            val isFav = cat.id == MODS_CATEGORY_FAVORITES
            val favAccent = Color(0xFFFF4D6D)
            val icon = when (cat.id) {
                "Semua" -> Icons.Outlined.Apps
                MODS_CATEGORY_FAVORITES -> if (picked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder
                "utility" -> Icons.Outlined.Build
                "performance", "optimization" -> Icons.Outlined.Speed
                "cosmetic", "decoration" -> Icons.Outlined.Palette
                "worldgen" -> Icons.Outlined.Public
                "technology" -> Icons.Outlined.Memory
                "magic" -> Icons.Outlined.AutoAwesome
                "adventure" -> Icons.Outlined.Explore
                "storage" -> Icons.Outlined.Inventory2
                "realistic" -> Icons.Outlined.WbSunny
                "fantasy", "stylized" -> Icons.Outlined.Brush
                else -> Icons.Outlined.Category
            }
            val labelText = if (isFav && favoriteCount > 0) "${cat.label} ($favoriteCount)" else cat.label
            Row(
                modifier = Modifier
                    .height(34.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            picked && isFav -> favAccent.copy(alpha = 0.22f)
                            picked -> AmberUi.Accent.copy(alpha = 0.24f)
                            else -> AmberUi.Surface
                        },
                        CircleShape
                    )
                    .border(
                        1.dp,
                        when {
                            picked && isFav -> favAccent.copy(alpha = 0.7f)
                            picked -> AmberUi.AccentEdge
                            else -> AmberUi.Border
                        },
                        CircleShape
                    )
                    .clickable { onSelectCategory(cat.id) }
                    .padding(horizontal = 12.dp)
                    .testTag("mods_cat_nav_${cat.id}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = when {
                        isFav -> favAccent
                        picked -> AmberUi.AccentSoft
                        else -> AmberUi.TextDim
                    },
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = labelText,
                    color = if (picked) AmberUi.Text else AmberUi.TextDim,
                    fontSize = 12.sp,
                    fontWeight = if (picked) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun RecentModSearchesRow(
    history: List<ModSearchHistoryEntity>,
    onPickQuery: (String) -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.History,
            contentDescription = null,
            tint = AmberUi.TextDim,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = "Tìm gần đây:",
            color = AmberUi.TextDim,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false
        )
        history.take(6).forEach { entry ->
            Row(
                modifier = Modifier
                    .height(28.dp)
                    .clip(CircleShape)
                    .background(AmberUi.SurfaceRaised, CircleShape)
                    .border(1.dp, AmberUi.Border, CircleShape)
                    .clickable { onPickQuery(entry.displayQuery) }
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.displayQuery,
                    color = AmberUi.Text,
                    fontSize = 11.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
        Text(
            text = "Xóa",
            color = AmberUi.AccentSoft,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(CircleShape)
                .clickable { onClear() }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/** Kartu vault terpasang dengan garis status vertikal di sisi kiri, saklar, dan tombol hapus 48dp. */
@Composable
private fun InstalledVaultCard(
    item: InstalledModItem,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    showToggle: Boolean = true,
    deletable: Boolean = true,
    subtitle: String? = null
) {
    val shape = RoundedCornerShape(20.dp)
    val stripeColor = if (item.isEnabled) AmberUi.ModrinthGreen else AmberUi.TextDim.copy(alpha = 0.35f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (item.isEnabled) AmberUi.Surface else AmberUi.Surface.copy(alpha = 0.4f),
                shape
            )
            .border(
                1.dp,
                if (item.isEnabled) AmberUi.Border else AmberUi.Border.copy(alpha = 0.5f),
                shape
            )
            .padding(start = 10.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(36.dp)
                .clip(CircleShape)
                .background(stripeColor)
        )
        Spacer(modifier = Modifier.width(10.dp))
        ModIcon(
            item = item,
            size = 44.dp,
            dimmed = !item.isEnabled
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (item.isEnabled) AmberUi.Text else AmberUi.TextDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (!item.isEnabled) {
                    Spacer(modifier = Modifier.width(6.dp))
                    NuxBadge(
                        text = "NONAKTIF",
                        backgroundColor = NuxColors.SurfaceElevated,
                        textColor = NuxColors.GrayNeutral
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle ?: "${item.filename} · ${formatBytes(item.sizeBytes)}",
                fontSize = 11.sp,
                color = AmberUi.TextDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        if (showToggle) {
            Switch(
                checked = item.isEnabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = AmberUi.Accent,
                    checkedBorderColor = AmberUi.AccentEdge,
                    uncheckedThumbColor = AmberUi.TextDim,
                    uncheckedTrackColor = NuxColors.SurfaceElevated
                ),
                modifier = Modifier.testTag("mod_toggle_${item.originalFilename}")
            )
        }
        if (deletable) {
            ModsIconButton(
                icon = Icons.Outlined.Delete,
                description = "Hapus ${item.name}",
                onClick = onDelete,
                tint = NuxColors.ErrorRed,
                ring = false,
                modifier = Modifier.testTag("mod_delete_${item.originalFilename}")
            )
        }
    }
}

/** Kartu etalase NovaCraft 2-kolom untuk hasil jelajah katalog Modrinth/CurseForge/Horizon. */
@Composable
private fun ModStoreCard(
    hit: ModrinthSearchHit,
    isInstalled: Boolean,
    isDownloading: Boolean,
    downloadStatus: String,
    isFavorite: Boolean = false,
    showSourceBadge: Boolean = true,
    onOpen: () -> Unit,
    onInstall: () -> Unit,
    onToggleFavorite: () -> Unit = {}
) {
    val shape = RoundedCornerShape(22.dp)
    val iconShape = RoundedCornerShape(14.dp)
    val tags = remember(hit.projectId, hit.displayCategories, hit.categories) { modsTagsFor(hit) }
    val isFromCurseForge = remember(hit.projectId) { CurseForgeMapper.isCurseForgeId(hit.projectId) }
    val relUpdated = remember(hit.dateModified, hit.dateCreated) {
        formatRelativeTimeVi(hit.dateModified ?: hit.dateCreated)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .amberGlass(shape)
            .then(
                if (isInstalled) Modifier.border(1.dp, AmberUi.ModrinthGreen.copy(alpha = 0.45f), shape)
                else Modifier
            )
            .clickable { onOpen() }
            .padding(14.dp)
            .testTag("mod_card_${hit.projectId}")
    ) {
        // Header: icon + title/author + favorite heart + compact download badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NuxNetworkImage(
                model = hit.iconUrl,
                contentDescription = hit.title,
                modifier = Modifier
                    .size(48.dp)
                    .border(1.dp, AmberUi.Border, iconShape),
                fallbackInitials = hit.title.take(2).uppercase(),
                shape = iconShape,
                maxSidePx = 192
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = hit.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = AmberUi.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = buildString {
                        append("oleh ")
                        append(hit.author ?: "Komunitas")
                        if (!relUpdated.isNullOrBlank()) {
                            append(" · ")
                            append(relUpdated)
                        }
                    },
                    fontSize = 11.sp,
                    color = AmberUi.TextDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        if (isFavorite) Color(0xFFFF4D6D).copy(alpha = 0.16f) else AmberUi.SurfaceRaised,
                        CircleShape
                    )
                    .border(
                        1.dp,
                        if (isFavorite) Color(0xFFFF4D6D).copy(alpha = 0.5f) else AmberUi.Border,
                        CircleShape
                    )
                    .clickable { onToggleFavorite() }
                    .testTag("mod_fav_${hit.projectId}"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (isFavorite) "Xóa khỏi Yêu thích" else "Lưu vào Yêu thích",
                    tint = if (isFavorite) Color(0xFFFF4D6D) else AmberUi.TextDim,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(AmberUi.SurfaceRaised, CircleShape)
                    .border(1.dp, AmberUi.Border, CircleShape)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Download,
                    contentDescription = null,
                    tint = AmberUi.AccentSoft,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = formatCompactNumber(hit.downloads),
                    color = AmberUi.Text,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Uniform 2-line description
        Text(
            text = hit.description ?: "Tidak ada deskripsi",
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = AmberUi.TextDim,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Tag chips + Action CTA footer (tinggi & sejajar seragam tanpa zoom/geser)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clipToBounds()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showSourceBadge) {
                    val srcColor = if (isFromCurseForge) AmberUi.AccentSoft else AmberUi.ModrinthGreen
                    NuxBadge(
                        text = if (isFromCurseForge) "CF" else "MR",
                        backgroundColor = srcColor.copy(alpha = 0.16f),
                        textColor = srcColor,
                        borderColor = srcColor.copy(alpha = 0.45f)
                    )
                }
                if (hit.projectType == "modpack") {
                    NuxBadge(
                        text = "PACK",
                        backgroundColor = AmberUi.Accent.copy(alpha = 0.25f),
                        textColor = AmberUi.AccentSoft,
                        borderColor = AmberUi.AccentEdge.copy(alpha = 0.5f)
                    )
                }
                tags.take(3).forEach { tag -> ModsTagChip(tag) }
            }

            if (isDownloading) {
                Row(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(CircleShape)
                        .background(AmberUi.Accent.copy(alpha = 0.16f), CircleShape)
                        .border(1.dp, AmberUi.AccentEdge.copy(alpha = 0.45f), CircleShape)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AmberSpinner(
                        color = AmberUi.AccentSoft,
                        modifier = Modifier.size(15.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = downloadStatus.ifBlank { "Unduh..." },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberUi.AccentSoft,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 86.dp)
                    )
                }
            } else if (isInstalled) {
                Row(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(CircleShape)
                        .background(AmberUi.ModrinthGreen.copy(alpha = 0.18f), CircleShape)
                        .border(1.dp, AmberUi.ModrinthGreen.copy(alpha = 0.55f), CircleShape)
                        .clickable { onOpen() }
                        .padding(horizontal = 12.dp)
                        .testTag("mod_installed_${hit.projectId}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = AmberUi.ModrinthGreen,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Terpasang",
                        color = AmberUi.ModrinthGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .height(38.dp)
                        .defaultMinSize(minWidth = 88.dp)
                        .clip(CircleShape)
                        .background(AmberUi.Accent, CircleShape)
                        .border(1.dp, AmberUi.AccentEdge, CircleShape)
                        .clickable { onInstall() }
                        .padding(horizontal = 12.dp)
                        .testTag("mod_install_${hit.projectId}"),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Download,
                        contentDescription = null,
                        tint = AmberUi.OnAccent,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Pasang",
                        color = AmberUi.OnAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

/**
 * Jendela detail dua panel: kiri = identitas proyek + tombol pasang, kanan = Ringkasan / Versi.
 */
@Composable
private fun ModDetailDialog(
    hit: ModrinthSearchHit,
    project: ModrinthProject?,
    versions: List<ModrinthVersion>,
    selectedVersion: ModrinthVersion?,
    isLoading: Boolean,
    isInstalling: Boolean,
    progressText: String,
    isInstalled: Boolean,
    isFavorite: Boolean = false,
    tab: String,
    instance: Instance?,
    onTab: (String) -> Unit,
    onPickVersion: (ModrinthVersion) -> Unit,
    onOpenGallery: (String) -> Unit,
    onInstall: () -> Unit,
    onToggleFavorite: () -> Unit = {},
    onClose: () -> Unit
) {
    val typeColor = when (hit.projectType) {
        "mod" -> NuxColors.Yellow
        "modpack" -> NuxColors.PurpleDark
        "world" -> NuxColors.ForestGreen
        "resourcepack" -> NuxColors.SkyBlueDark
        else -> NuxColors.AmberDark
    }

    NuxDialog(
        onDismissRequest = { if (!isInstalling) onClose() },
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 880.dp),
        fillMaxHeight = true,
        compact = true
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            val isNarrowDialog = maxWidth < 560.dp

            @Composable
            fun InstallActionArea() {
                if (isInstalling) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AmberSpinner(
                            color = NuxColors.Yellow,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = progressText.ifBlank { "Sedang mengunduh dan memasang..." },
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = NuxColors.Yellow,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else if (isInstalled) {
                    val okShape = RoundedCornerShape(20.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 42.dp)
                            .clip(okShape)
                            .background(AmberUi.ModrinthGreen.copy(alpha = 0.18f), okShape)
                            .border(NuxSizes.GlassBorder, AmberUi.ModrinthGreen.copy(alpha = 0.5f), okShape)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            tint = AmberUi.ModrinthGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Sudah terpasang",
                            color = AmberUi.ModrinthGreen,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    NuxButton(
                        onClick = onInstall,
                        backgroundColor = NuxColors.Yellow,
                        enabled = versions.isNotEmpty(),
                        cornerRadius = 20.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("mod_detail_install_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (hit.projectType) {
                                "modpack" -> "Pasang modpack"
                                "world" -> "Pasang dunia"
                                else -> "Pasang ke instance"
                            },
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            @Composable
            fun DetailBody(bodyModifier: Modifier) {
                Column(
                    modifier = bodyModifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AmberSpinner(color = NuxColors.Yellow, modifier = Modifier.size(28.dp))
                        }
                    } else if (tab == "overview") {
                        val galleryList = project?.gallery ?: emptyList()
                        if (galleryList.isNotEmpty()) {
                            Text(
                                text = "Tangkapan layar",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = NuxColors.GrayNeutral
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(galleryList) { gal ->
                                    Box(
                                        modifier = Modifier
                                            .width(160.dp)
                                            .height(92.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(NuxColors.SurfaceWhite)
                                            .border(NuxSizes.GlassBorder, NuxColors.CardBorder, RoundedCornerShape(12.dp))
                                            .clickable { onOpenGallery(gal.url) }
                                    ) {
                                        NuxNetworkImage(
                                            model = gal.url,
                                            contentDescription = gal.title ?: "Screenshot",
                                            modifier = Modifier.fillMaxSize(),
                                            shape = RoundedCornerShape(12.dp),
                                            maxSidePx = 400,
                                            showSpinner = true
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        Text(
                            text = "Deskripsi",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = NuxColors.GrayNeutral
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = project?.description ?: hit.description ?: "Tidak ada ringkasan deskripsi.",
                            fontSize = 13.sp,
                            color = NuxColors.DarkGray.copy(alpha = 0.88f),
                            lineHeight = 18.sp
                        )

                        val updatedDateStr = formatShortDateVi(hit.dateModified)
                        val createdDateStr = formatShortDateVi(hit.dateCreated)
                        if (updatedDateStr != null || createdDateStr != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                updatedDateStr?.let {
                                    NuxBadge(
                                        text = "Cập nhật: $it",
                                        backgroundColor = NuxColors.SurfaceElevated,
                                        textColor = NuxColors.GrayNeutral
                                    )
                                }
                                createdDateStr?.let {
                                    NuxBadge(
                                        text = "Phát hành: $it",
                                        backgroundColor = NuxColors.SurfaceElevated,
                                        textColor = NuxColors.GrayNeutral
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val allLoaders = (project?.loaders ?: hit.categories ?: emptyList()).distinct()
                        if (allLoaders.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                allLoaders.take(6).forEach { tag ->
                                    NuxBadge(
                                        text = tag.uppercase(),
                                        backgroundColor = NuxColors.SurfaceElevated,
                                        textColor = NuxColors.GrayNeutral
                                    )
                                }
                            }
                        }
                    } else {
                        if (versions.isEmpty()) {
                            val warnShape = RoundedCornerShape(12.dp)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(NuxColors.ErrorRed.copy(alpha = 0.12f), warnShape)
                                    .border(NuxSizes.GlassBorder, NuxColors.ErrorRed.copy(alpha = 0.4f), warnShape)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "Tidak ada versi yang didukung",
                                    color = NuxColors.ErrorRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Proyek ini tidak punya rilis yang cocok dengan Minecraft ${instance?.mcVersion ?: ""} (${(instance?.loader ?: "vanilla").uppercase()}).",
                                    color = NuxColors.GrayNeutral,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                versions.forEach { ver ->
                                    val isPicked = selectedVersion?.id == ver.id
                                    val rowShape = RoundedCornerShape(14.dp)
                                    val verColor = when (ver.versionType) {
                                        "release" -> NuxColors.Yellow
                                        "beta" -> NuxColors.AmberDark
                                        else -> NuxColors.ErrorRed
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(rowShape)
                                            .background(if (isPicked) NuxColors.YellowPale else NuxColors.SurfaceWhite, rowShape)
                                            .border(
                                                NuxSizes.GlassBorder,
                                                if (isPicked) NuxColors.Yellow.copy(alpha = 0.55f) else NuxColors.CardBorder,
                                                rowShape
                                            )
                                            .clickable { onPickVersion(ver) }
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(if (isPicked) NuxColors.Yellow else Color.Transparent)
                                                .border(
                                                    NuxSizes.GlassBorder,
                                                    if (isPicked) NuxColors.Yellow else NuxColors.GrayNeutral,
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isPicked) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Check,
                                                    contentDescription = null,
                                                    tint = NuxColors.Ink,
                                                    modifier = Modifier.size(11.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = ver.name.ifBlank { ver.versionNumber },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = NuxColors.DarkGray,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Versi ${ver.versionNumber} · MC ${ver.gameVersions.joinToString(", ")}",
                                                fontSize = 11.sp,
                                                color = NuxColors.GrayNeutral,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        NuxBadge(
                                            text = ver.versionType.uppercase(),
                                            backgroundColor = verColor.copy(alpha = 0.16f),
                                            textColor = verColor,
                                            borderColor = verColor.copy(alpha = 0.4f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = when (hit.projectType) {
                            "modpack" -> "Modpack diekstrak langsung ke instance (overrides, konfigurasi, dan manifest)."
                            "world" -> "Dunia diekstrak langsung ke folder saves instance ini."
                            else -> "Dependensi wajib ikut terpasang otomatis."
                        },
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = NuxColors.GrayNeutral
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            if (isNarrowDialog) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(AmberUi.Surface, RoundedCornerShape(16.dp))
                            .border(1.dp, AmberUi.Border, RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NuxNetworkImage(
                            model = hit.iconUrl,
                            contentDescription = hit.title,
                            modifier = Modifier
                                .size(50.dp)
                                .border(NuxSizes.GlassBorder, NuxColors.CardBorder, RoundedCornerShape(12.dp)),
                            fallbackInitials = hit.title.take(2).uppercase(),
                            shape = RoundedCornerShape(12.dp),
                            maxSidePx = 192
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = hit.title,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = NuxColors.DarkGray,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "oleh ${hit.author ?: "Komunitas"} · ${formatCompactNumber(hit.downloads)} unduhan",
                                fontSize = 11.sp,
                                color = NuxColors.GrayNeutral,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        ModsIconButton(
                            icon = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            description = if (isFavorite) "Xóa khỏi Yêu thích" else "Lưu vào Yêu thích",
                            tint = if (isFavorite) Color(0xFFFF4D6D) else NuxColors.GrayNeutral,
                            onClick = onToggleFavorite,
                            modifier = Modifier.testTag("mod_detail_fav_button")
                        )
                        ModsIconButton(
                            icon = Icons.Outlined.Close,
                            description = "Tutup",
                            onClick = onClose,
                            enabled = !isInstalling,
                            modifier = Modifier.testTag("mod_detail_close_button")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SettingChip(
                            text = "Ringkasan",
                            selected = tab == "overview",
                            onClick = { onTab("overview") }
                        )
                        SettingChip(
                            text = "Versi (${versions.size})",
                            selected = tab == "versions",
                            onClick = { onTab("versions") }
                        )
                    }

                    DetailBody(Modifier.weight(1f))

                    InstallActionArea()
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // ---------------- KIRI ----------------
                    Column(
                        modifier = Modifier
                            .weight(0.36f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(18.dp))
                            .background(AmberUi.Surface, RoundedCornerShape(18.dp))
                            .border(1.dp, AmberUi.Border, RoundedCornerShape(18.dp))
                            .padding(14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            NuxNetworkImage(
                                model = hit.iconUrl,
                                contentDescription = hit.title,
                                modifier = Modifier
                                    .size(58.dp)
                                    .border(NuxSizes.GlassBorder, NuxColors.CardBorder, RoundedCornerShape(14.dp)),
                                fallbackInitials = hit.title.take(2).uppercase(),
                                shape = RoundedCornerShape(14.dp),
                                maxSidePx = 192
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = hit.title,
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp,
                                lineHeight = 22.sp,
                                color = NuxColors.DarkGray,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "oleh ${hit.author ?: "Komunitas"}",
                                fontSize = 11.sp,
                                color = NuxColors.GrayNeutral
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                NuxBadge(
                                    text = hit.projectType.uppercase(),
                                    backgroundColor = typeColor.copy(alpha = 0.16f),
                                    textColor = typeColor,
                                    borderColor = typeColor.copy(alpha = 0.4f)
                                )
                                NuxBadge(
                                    text = "${formatCompactNumber(hit.downloads)} unduhan",
                                    backgroundColor = NuxColors.SurfaceElevated,
                                    textColor = NuxColors.GrayNeutral
                                )
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isFavorite) Color(0xFFFF4D6D).copy(alpha = 0.18f) else NuxColors.SurfaceElevated,
                                            CircleShape
                                        )
                                        .clickable { onToggleFavorite() }
                                        .testTag("mod_detail_fav_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                        contentDescription = if (isFavorite) "Xóa khỏi Yêu thích" else "Lưu vào Yêu thích",
                                        tint = if (isFavorite) Color(0xFFFF4D6D) else NuxColors.GrayNeutral,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                            if (selectedVersion != null && !isInstalled) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Versi dipilih",
                                    fontSize = 11.sp,
                                    color = NuxColors.GrayNeutral
                                )
                                Text(
                                    text = selectedVersion.name.ifBlank { selectedVersion.versionNumber },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NuxColors.DarkGray,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        InstallActionArea()
                    }

                    // ---------------- KANAN ----------------
                    Column(
                        modifier = Modifier
                            .weight(0.64f)
                            .fillMaxHeight()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SettingChip(
                                    text = "Ringkasan",
                                    selected = tab == "overview",
                                    onClick = { onTab("overview") }
                                )
                                SettingChip(
                                    text = "Versi (${versions.size})",
                                    selected = tab == "versions",
                                    onClick = { onTab("versions") }
                                )
                            }
                            ModsIconButton(
                                icon = Icons.Outlined.Close,
                                description = "Tutup",
                                onClick = onClose,
                                enabled = !isInstalling,
                                modifier = Modifier.testTag("mod_detail_close_button")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        DetailBody(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, 3)
    return DecimalFormat("#,##0.#").format(bytes / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
}

private fun formatCompactNumber(number: Long): String {
    return when {
        number >= 1_000_000 -> DecimalFormat("#,##0.#").format(number / 1_000_000.0) + "M"
        number >= 1_000 -> DecimalFormat("#,##0.#").format(number / 1_000.0) + "K"
        else -> number.toString()
    }
}

// ----------------------------------------------------------------------------
// Section: ui/screens/SettingsScreen.kt
// ----------------------------------------------------------------------------

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onOpenMods: () -> Unit = {},
    onOpenAccounts: () -> Unit = {},
    onOpenGuiEditor: () -> Unit = {},
    onChangeBackground: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentSettings by SettingsManager.settings.collectAsState()

    var showAboutDialog by remember { mutableStateOf(false) }

    // Ekspor/impor layout kontrol lewat pemilih berkas sistem. Baca/tulis berkas dan penyimpanan
    // dikerjakan di thread IO; layar hanya menyimpan hasil pemeriksaan sampai pengguna menyetujui.
    var pendingImport by remember { mutableStateOf<ControlLayoutIO.ImportResult.Ok?>(null) }
    val exportLayoutLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) { ControlLayoutManager.exportTo(context, uri) }
                Toast.makeText(
                    context,
                    if (ok) "Layout kontrol disimpan ke berkas" else "Gagal menyimpan berkas",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    val importLayoutLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val r = withContext(Dispatchers.IO) { ControlLayoutManager.readImport(context, uri) }
                if (r is ControlLayoutIO.ImportResult.Ok) {
                    pendingImport = r
                } else if (r is ControlLayoutIO.ImportResult.Failed) {
                    val message = when (r.reason) {
                        ControlLayoutIO.Reason.TOO_LARGE -> "Berkas terlalu besar untuk layout kontrol"
                        ControlLayoutIO.Reason.NO_BUTTONS -> "Berkas tidak berisi tombol"
                        ControlLayoutIO.Reason.NOT_LAYOUT -> "Berkas ini bukan layout kontrol yang valid"
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    var importDataBusy by remember { mutableStateOf(false) }
    val importDataLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null && !importDataBusy) {
            importDataBusy = true
            scope.launch {
                val res = withContext(Dispatchers.IO) { LauncherDataImporter.importFromUri(context, uri) }
                importDataBusy = false
                res.onSuccess { summary ->
                    Toast.makeText(
                        context,
                        "Đã nhập ${summary.importedInstances} instance và ${summary.importedControls} sơ đồ điều khiển",
                        Toast.LENGTH_LONG
                    ).show()
                }.onFailure { err ->
                    Toast.makeText(
                        context,
                        err.message ?: "Không thể nhập dữ liệu từ tệp đã chọn",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // Local mutable state for instant responsive UI before saving
    var ramMb by remember(currentSettings.ramMb) { mutableIntStateOf(currentSettings.ramMb) }
    var initialHeapMb by remember(currentSettings.initialHeapMb) { mutableIntStateOf(currentSettings.initialHeapMb) }
    var customJvmArgs by remember(currentSettings.customJvmArgs) { mutableStateOf(currentSettings.customJvmArgs) }
    var defaultRuntime by remember(currentSettings.defaultJavaRuntime) { mutableStateOf(currentSettings.defaultJavaRuntime) }
    var jarSandboxEnabled by remember(currentSettings.jarSandboxEnabled) { mutableStateOf(currentSettings.jarSandboxEnabled) }
    var showJavaRuntimeDialog by remember { mutableStateOf(false) }

    var resolutionRatio by remember(currentSettings.resolutionRatio) { mutableIntStateOf(currentSettings.resolutionRatio) }
    var autoOptimizeMC by remember(currentSettings.autoOptimizeMinecraft) { mutableStateOf(currentSettings.autoOptimizeMinecraft) }
    var sustainedPerf by remember(currentSettings.sustainedPerformanceMode) { mutableStateOf(currentSettings.sustainedPerformanceMode) }
    var thermalPerfCap by remember(currentSettings.thermalPerfCap) {
        mutableIntStateOf(if (currentSettings.thermalPerfCap <= 0) 90 else currentSettings.thermalPerfCap.coerceIn(50, 100))
    }
    var selectedRenderer by remember(currentSettings.selectedRenderer) { mutableStateOf(currentSettings.selectedRenderer) }
    var vulkanDriver by remember(currentSettings.vulkanDriver) { mutableStateOf(currentSettings.vulkanDriver) }
    var graphicsApi by remember(currentSettings.graphicsApi) { mutableStateOf(currentSettings.graphicsApi) }
    var zinkPreferSystemDriver by remember(currentSettings.zinkPreferSystemDriver) { mutableStateOf(currentSettings.zinkPreferSystemDriver) }
    var vsyncInZink by remember(currentSettings.vsyncInZink) { mutableStateOf(currentSettings.vsyncInZink) }
    var forceVsync by remember(currentSettings.forceVsync) { mutableStateOf(currentSettings.forceVsync) }
    var useAngle by remember(currentSettings.useAngle) { mutableStateOf(currentSettings.useAngle) }
    var useSystemAngle by remember(currentSettings.useSystemAngle) { mutableStateOf(currentSettings.useSystemAngle) }
    var curseForgeApiKeyInput by remember(currentSettings.curseForgeApiKey) { mutableStateOf(currentSettings.curseForgeApiKey) }
    var curseForgeKeyVisible by remember { mutableStateOf(false) }
    var recordQuality by remember(currentSettings.recordQuality) { mutableStateOf(currentSettings.recordQuality) }
    var recordFps by remember(currentSettings.recordFps) { mutableIntStateOf(currentSettings.recordFps) }
    var recordMic by remember(currentSettings.recordMic) { mutableStateOf(currentSettings.recordMic) }
    var recordWithControls by remember(currentSettings.recordWithControls) { mutableStateOf(currentSettings.recordWithControls) }
    var recordThermalGuard by remember(currentSettings.recordThermalGuard) { mutableStateOf(currentSettings.recordThermalGuard) }
    var showRendererDialog by remember { mutableStateOf(false) }
    var rendererListTick by remember { mutableIntStateOf(0) }
    var showRendererConfigDialog by remember { mutableStateOf(false) }
    var selectedConfigRenderer by remember { mutableStateOf<NuxRendererInfo?>(null) }
    var showAdrenoWarningDialog by remember { mutableStateOf(false) }

    // Plugin native library: daftar dari hasil pemindaian yang sudah ada (tidak memindai sendiri saat dibuka
    // kecuali hasilnya belum pernah ada atau sudah tua), dan plugin yang dimatikan pengguna.
    var nativePlugins by remember { mutableStateOf(NativePluginManager.getPlugins()) }
    var nativeScanBusy by remember { mutableStateOf(false) }
    var disabledNativePlugins by remember(currentSettings.disabledNativePlugins) { mutableStateOf(currentSettings.disabledNativePlugins) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try { NuxRendererPluginManager.scanPluginsIfStale(context) } catch (_: Throwable) {}
        }
        nativePlugins = NativePluginManager.getPlugins()
    }

    var appLanguage by remember { mutableStateOf(Tr.choice) }
    var bgAnimate by remember(currentSettings.homeBgAnimate) { mutableStateOf(currentSettings.homeBgAnimate) }
    var themeModeId by remember(currentSettings.themeMode) { mutableStateOf(currentSettings.themeMode) }
    var colorSourceId by remember(currentSettings.colorSource) { mutableStateOf(currentSettings.colorSource) }
    var accentId by remember(currentSettings.accentColor) { mutableStateOf(currentSettings.accentColor) }
    var uiStyleId by remember(currentSettings.uiStyle) { mutableStateOf(currentSettings.uiStyle) }
    var bgDim by remember(currentSettings.bgDim) { mutableIntStateOf(currentSettings.bgDim) }
    var animScreenTransitions by remember(currentSettings.animScreenTransitions) { mutableStateOf(currentSettings.animScreenTransitions) }
    var animDialogsPopups by remember(currentSettings.animDialogsPopups) { mutableStateOf(currentSettings.animDialogsPopups) }
    var animButtonEffects by remember(currentSettings.animButtonEffects) { mutableStateOf(currentSettings.animButtonEffects) }
    var animListsContent by remember(currentSettings.animListsContent) { mutableStateOf(currentSettings.animListsContent) }
    var startupSoundEnabled by remember(currentSettings.startupSoundEnabled) { mutableStateOf(currentSettings.startupSoundEnabled) }
    var showAnimTypesDialog by remember { mutableStateOf(false) }

    var verifyGameFiles by remember(currentSettings.verifyGameFiles) { mutableStateOf(currentSettings.verifyGameFiles) }
    var fastGameStartup by remember(currentSettings.fastGameStartup) { mutableStateOf(currentSettings.fastGameStartup) }
    var gameDownloadSource by remember(currentSettings.gameDownloadSource) { mutableStateOf(currentSettings.gameDownloadSource) }
    var checkGameVersionList by remember(currentSettings.checkGameVersionList) { mutableStateOf(currentSettings.checkGameVersionList) }
    var localOnlyMode by remember(currentSettings.localOnlyMode) { mutableStateOf(currentSettings.localOnlyMode) }
    var alternateSurfaceRendering by remember(currentSettings.alternateSurfaceRendering) { mutableStateOf(currentSettings.alternateSurfaceRendering) }
    var discordRichPresenceOnPlay by remember(currentSettings.discordRichPresenceOnPlay) { mutableStateOf(currentSettings.discordRichPresenceOnPlay) }
    var discordShowServer by remember(currentSettings.discordShowServer) { mutableStateOf(currentSettings.discordShowServer) }
    var discordPresenceIdle by remember(currentSettings.discordPresenceIdle) { mutableStateOf(currentSettings.discordPresenceIdle) }

    var dumpShadersEnabled by remember(currentSettings.dumpShadersEnabled) { mutableStateOf(currentSettings.dumpShadersEnabled) }
    var forceBigCoreRender by remember(currentSettings.forceBigCoreRender) { mutableStateOf(currentSettings.forceBigCoreRender) }
    var freedrenoSysmem by remember(currentSettings.freedrenoSysmem) { mutableStateOf(currentSettings.freedrenoSysmem) }
    var forceOpenSlAudio by remember(currentSettings.forceOpenSlAudio) { mutableStateOf(currentSettings.forceOpenSlAudio) }
    var disableCurseForge by remember(currentSettings.disableCurseForge) { mutableStateOf(currentSettings.disableCurseForge) }
    var ubwcWorkaround by remember(currentSettings.ubwcWorkaround) { mutableStateOf(currentSettings.ubwcWorkaround) }

    var bgPauseOnPowerSave by remember(currentSettings.homeBgPauseOnPowerSave) { mutableStateOf(currentSettings.homeBgPauseOnPowerSave) }
    var bgSound by remember(currentSettings.homeBgSound) { mutableStateOf(currentSettings.homeBgSound) }
    var bgVolume by remember(currentSettings.homeBgVolume) { mutableIntStateOf(currentSettings.homeBgVolume) }
    var ambientOn by remember(currentSettings.homeAmbientEnabled) { mutableStateOf(currentSettings.homeAmbientEnabled) }
    var ambientIntensity by remember(currentSettings.homeAmbientIntensity) { mutableIntStateOf(currentSettings.homeAmbientIntensity) }
    var offlineMode by remember(currentSettings.offlineMode) { mutableStateOf(currentSettings.offlineMode) }
    var persistAccountSession by remember(currentSettings.persistAccountSession) { mutableStateOf(currentSettings.persistAccountSession) }

    val totalRamMb = remember { SettingsManager.getTotalDeviceMemoryMb(context) }
    val maxAllocatableRam = remember(totalRamMb) { (totalRamMb * 0.6f).toInt().coerceAtLeast(1024) }
    val cpuCores = remember { SettingsManager.getCpuCoreCount() }
    val showRefreshLock = remember { DeviceProfiles.supportsHighRefresh(context) }

    fun commitSettings() {
        val updated = SettingsManager.settings.value.copy(
            ramMb = ramMb,
            initialHeapMb = initialHeapMb,
            customJvmArgs = customJvmArgs,
            defaultJavaRuntime = defaultRuntime,
            jarSandboxEnabled = jarSandboxEnabled,
            resolutionRatio = resolutionRatio,
            autoOptimizeMinecraft = autoOptimizeMC,
            sustainedPerformanceMode = sustainedPerf,
            thermalPerfCap = thermalPerfCap.coerceIn(50, 100),
            selectedRenderer = selectedRenderer,
            vulkanDriver = vulkanDriver,
            graphicsApi = graphicsApi,
            zinkPreferSystemDriver = zinkPreferSystemDriver,
            vsyncInZink = vsyncInZink,
            forceVsync = forceVsync,
            useAngle = useAngle,
            useSystemAngle = useSystemAngle,
            curseForgeApiKey = curseForgeApiKeyInput.trim(),
            disabledNativePlugins = disabledNativePlugins,
            recordQuality = recordQuality,
            recordFps = recordFps,
            recordMic = recordMic,
            recordWithControls = recordWithControls,
            recordThermalGuard = recordThermalGuard,
            homeBgAnimate = bgAnimate,
            homeBgPauseOnPowerSave = bgPauseOnPowerSave,
            homeBgSound = bgSound,
            homeBgVolume = bgVolume,
            homeAmbientEnabled = ambientOn,
            homeAmbientIntensity = ambientIntensity,
            offlineMode = offlineMode,
            persistAccountSession = persistAccountSession,
            themeMode = themeModeId,
            colorSource = colorSourceId,
            accentColor = accentId,
            uiStyle = uiStyleId,
            bgDim = bgDim,
            animScreenTransitions = animScreenTransitions,
            animDialogsPopups = animDialogsPopups,
            animButtonEffects = animButtonEffects,
            animListsContent = animListsContent,
            startupSoundEnabled = startupSoundEnabled,
            verifyGameFiles = verifyGameFiles,
            fastGameStartup = fastGameStartup,
            gameDownloadSource = gameDownloadSource,
            checkGameVersionList = checkGameVersionList,
            localOnlyMode = localOnlyMode,
            alternateSurfaceRendering = alternateSurfaceRendering,
            discordRichPresenceOnPlay = discordRichPresenceOnPlay,
            discordShowServer = discordShowServer,
            discordPresenceIdle = discordPresenceIdle,
            dumpShadersEnabled = dumpShadersEnabled,
            forceBigCoreRender = forceBigCoreRender,
            freedrenoSysmem = freedrenoSysmem,
            forceOpenSlAudio = forceOpenSlAudio,
            disableCurseForge = disableCurseForge,
            ubwcWorkaround = ubwcWorkaround
        )
        SettingsManager.updateSettings(context, updated)
    }

    // Pindai ulang atas permintaan pengguna (setelah memasang plugin baru), di thread latar.
    fun rescanNativePlugins() {
        if (nativeScanBusy) return
        nativeScanBusy = true
        scope.launch {
            withContext(Dispatchers.IO) {
                try { NuxRendererPluginManager.scanPlugins(context) } catch (_: Throwable) {}
            }
            nativePlugins = NativePluginManager.getPlugins()
            nativeScanBusy = false
        }
    }

    // Kategori aktif: 0 = Game, 1 = Kontrol, 2 = Tampilan, 3 = Tentang, 4 = Rust (urutan di sisi kiri: Game, Rust,
    // Kontrol, Tampilan, Tentang). Isinya digulir sendiri.
    var section by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    val searching = query.isNotBlank()
    var showLanguageDialog by remember { mutableStateOf(false) }
    val contentScroll = rememberScrollState()
    LaunchedEffect(section, searching) { contentScroll.scrollTo(0) }

    val languageOptions = listOf(
        Tr.SYSTEM to "Ikuti sistem",
        Tr.ID to "Bahasa Indonesia",
        Tr.EN to "English",
        Tr.ES to "Espa\u00f1ol",
        Tr.VI to "Ti\u1ebfng Vi\u1ec7t",
        Tr.PT to "Portugu\u00eas (Brasil)",
        Tr.TH to "\u0e20\u0e32\u0e29\u0e32\u0e44\u0e17\u0e22"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 10.dp)
    ) {
        val isNarrow = maxWidth < 600.dp
        val categories = listOf(
            Triple(0, "Game", Icons.Outlined.ViewInAr),
            // Mesin Rust punya pengaturan sendiri yang berbeda dari jalur Java, jadi tidak digabung ke Game.
            Triple(4, "Rust", Icons.Outlined.Memory),
            Triple(1, "Kontrol", Icons.Outlined.SportsEsports),
            Triple(2, "Tampilan", Icons.Outlined.Image),
            Triple(3, "Tentang", Icons.Outlined.Info)
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // Baris atas: navigasi dan kolom cari (bertumpuk rapi di layar sempit/portrait)
            if (isNarrow) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        AmberNavPill(
                            active = "settings",
                            activeLabel = "Pengaturan",
                            onBack = onNavigateBack,
                            onMods = onOpenMods,
                            onAccounts = onOpenAccounts,
                            onSettings = {}
                        )
                    }
                    AmberSearchPill(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = "Cari pengaturan...",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AmberNavPill(
                        active = "settings",
                        activeLabel = "Pengaturan",
                        onBack = onNavigateBack,
                        onMods = onOpenMods,
                        onAccounts = onOpenAccounts,
                        onSettings = {}
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    AmberSearchPill(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = "Cari pengaturan...",
                        modifier = Modifier.widthIn(min = 180.dp, max = 260.dp)
                    )
                }
            }

            if (isNarrow) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    categories.forEach { (index, label, icon) ->
                        val picked = !searching && section == index
                        val chipShape = CircleShape
                        Row(
                            modifier = Modifier
                                .defaultMinSize(minHeight = 40.dp)
                                .clip(chipShape)
                                .background(
                                    if (picked) AmberUi.Accent.copy(alpha = 0.24f) else AmberUi.Surface,
                                    chipShape
                                )
                                .border(
                                    1.dp,
                                    if (picked) AmberUi.AccentEdge else AmberUi.Border,
                                    chipShape
                                )
                                .clickable {
                                    section = index
                                    query = ""
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (picked) AmberUi.AccentSoft else AmberUi.TextDim,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = label,
                                color = if (picked) AmberUi.Text else AmberUi.TextDim,
                                fontWeight = if (picked) FontWeight.Bold else FontWeight.SemiBold,
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!isNarrow) {
                    // Kategori (sidebar)
                    Column(
                        modifier = Modifier
                            .width(148.dp)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        categories.forEach { (index, label, icon) ->
                            AmberSideItem(
                                icon = icon,
                                label = label,
                                selected = !searching && section == index,
                                onClick = {
                                    section = index
                                    query = ""
                                }
                            )
                        }
                        if (!searching) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${totalRamMb / 1024} GB RAM · $cpuCores inti CPU",
                                color = AmberUi.TextDim,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                        }
                    }
                }

                // Isi kategori: tiap kelompok berupa kartu panjang. Saat mencari, semua baris yang cocok
                // dari semua kategori tampil, masing-masing di kartunya sendiri.
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(contentScroll)
                ) {
                CompositionLocalProvider(
                    LocalSettingsFilter provides (if (searching) SettingsFilter(query) else null)
                ) {
                    // ===================== GAME =====================
                    if (searching || section == 0) {
                        // Profil khusus perangkat: hanya muncul di HP yang terdaftar, di HP lain tidak menggambar apa pun.
                        if (!searching) DeviceProfileCard()

                        SettingsGroup("Akun & Sesi") {
                            SettingChoiceRow(
                                title = "Mode koneksi akun",
                                desc = if (offlineMode) {
                                    "Mode Offline aktif: akun masuk dengan sesi lokal tanpa verifikasi online (server cracking/LAN). Token online tersimpan tetap aman."
                                } else {
                                    "Mode Online aktif: memakai token Microsoft/Ely.by asli dan menyegarkan sesi saat dibutuhkan."
                                },
                                options = listOf(false to "Online", true to "Offline"),
                                selected = offlineMode,
                                onSelect = { pickedOffline ->
                                    offlineMode = pickedOffline
                                    commitSettings()
                                }
                            )
                            SettingDivider()
                            SettingSwitchRow(
                                title = "Simpan sesi akun lokal",
                                desc = "Menyimpan akun Minecraft aktif secara lokal di perangkat agar otomatis dipulihkan saat launcher dibuka lagi.",
                                checked = persistAccountSession,
                                onCheckedChange = { enabled ->
                                    persistAccountSession = enabled
                                    commitSettings()
                                    AccountManager.setSessionPersistence(context, enabled)
                                }
                            )
                        }

                        SettingsGroup("ĐIỀU CHỈNH JAVA") {
                            SettingValueRow(
                                title = "Quản lí Java Runtime",
                                desc = "Quản lý các hệ thống thời gian chạy Java.",
                                value = JavaRuntimeManager.getRuntimeDisplayName(defaultRuntime),
                                onClick = { showJavaRuntimeDialog = true }
                            )
                            SettingDivider()
                            SettingFrame(
                                "Các chỉ số chạy JVM",
                                "Cẩn thận, việc này sẽ làm trò chơi gặp sự cố nếu sửa đổi mà không có nhiều kiến thức."
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 9.dp)
                                ) {
                                    SettingRowText(
                                        title = "Các chỉ số chạy JVM",
                                        desc = "Cẩn thận, việc này sẽ làm trò chơi gặp sự cố nếu sửa đổi mà không có nhiều kiến thức."
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    NuxTextField(
                                        value = customJvmArgs,
                                        onValueChange = {
                                            customJvmArgs = it
                                            commitSettings()
                                        },
                                        placeholder = "Contoh: -XX:+UseG1GC",
                                        trailingContent = if (customJvmArgs.isNotEmpty()) {
                                            {
                                                IconButton(
                                                    onClick = {
                                                        customJvmArgs = ""
                                                        commitSettings()
                                                    },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Close,
                                                        contentDescription = "Xóa chỉ số JVM",
                                                        tint = AmberUi.TextDim,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        } else null
                                    )
                                }
                            }
                            SettingDivider()
                            val ramTooBig = ramMb > (totalRamMb * 0.5f)
                            SettingSliderRow(
                                title = "Phân bổ bộ nhớ",
                                desc = if (ramTooBig) {
                                    "Controls how much memory is given to the Java runtime. RAM vượt 50% bộ nhớ thiết bị có thể khiến Android đóng game."
                                } else {
                                    "Controls how much memory is given to the Java runtime."
                                },
                                valueText = "$ramMb MB",
                                value = ramMb.toFloat(),
                                valueRange = 512f..maxAllocatableRam.toFloat(),
                                steps = ((maxAllocatableRam - 512) / 128) - 1,
                                onValueChange = {
                                    ramMb = ((it / 128).roundToInt() * 128).coerceIn(512, maxAllocatableRam)
                                    commitSettings()
                                },
                                warn = ramTooBig,
                                footer = {
                                    Row(
                                        modifier = Modifier
                                            .padding(top = 8.dp)
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(
                                            1024 to "1024 MB",
                                            1536 to "1536 MB",
                                            2048 to "2048 MB",
                                            3072 to "3072 MB",
                                            4096 to "4096 MB"
                                        ).forEach { (preset, label) ->
                                            if (preset <= maxAllocatableRam) {
                                                SettingChip(
                                                    text = label,
                                                    selected = ramMb == preset,
                                                    onClick = {
                                                        ramMb = preset
                                                        commitSettings()
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            )
                            SettingDivider()
                            SettingSwitchRow(
                                title = "Thực thi tệp .jar trong môi trường thử nghiệm (sandbox)",
                                desc = "Điều khiển tính khả dụng của trình quản lý bảo mật môi trường thử nghiệm (sandbox) khi thực thi tệp .jar.",
                                checked = jarSandboxEnabled,
                                onCheckedChange = {
                                    jarSandboxEnabled = it
                                    commitSettings()
                                }
                            )
                        }

                        if (!searching) PowerSaverCard()

                        SettingsGroup("Grafis") {
                        val activeRendererInfo = remember(selectedRenderer) {
                            NuxRendererRegistry.findRendererById(selectedRenderer)
                        }
                            SettingValueRow(
                                title = "Renderer",
                                desc = activeRendererInfo.summary,
                                value = activeRendererInfo.displayName,
                                onClick = { showRendererDialog = true }
                            )
                            if (activeRendererInfo.isConfigurable) {
                                SettingDivider()
                                SettingActionRow(
                                    title = "Konfigurasi renderer",
                                    desc = "Pengaturan khusus untuk renderer yang sedang dipakai.",
                                    actionLabel = "Atur",
                                    onClick = {
                                        selectedConfigRenderer = activeRendererInfo
                                        showRendererConfigDialog = true
                                    }
                                )
                            }
                            if (MobileGluesConfig.isMobileGlues(activeRendererInfo.libraryName)) {
                                SettingDivider()
                                MobileGluesCacheRow()
                            }
                        SettingDivider()
                        // Rentang 40-125% kelipatan 5, sama dengan slider "Resolusi render" di menu dalam game.
                        SettingSliderRow(
                            title = "Skala resolusi layar",
                            desc = "Lebih kecil = FPS lebih tinggi, gambar lebih buram.",
                            valueText = "$resolutionRatio%",
                            value = resolutionRatio.toFloat(),
                            valueRange = 40f..125f,
                            steps = 16,
                            onValueChange = {
                                resolutionRatio = (it / 5f).roundToInt() * 5
                                commitSettings()
                            }
                        )
                        SettingDivider()
                        SettingSwitchRow(
                            title = "Bắt buộc kích hoạt VSync",
                            desc = "Đồng bộ tốc độ khung hình với tần số quét để hạn chế xé hình khi chơi game.",
                            checked = forceVsync,
                            onCheckedChange = {
                                forceVsync = it
                                commitSettings()
                            }
                        )
                        SettingDivider()
                        SettingSwitchRow(
                            title = "Bật chế độ duy trì hiệu suất",
                            desc = "Hạn chế giảm hiệu suất vì quá nhiệt bằng cách giảm hiệu suất tối đa.",
                            checked = sustainedPerf,
                            onCheckedChange = {
                                sustainedPerf = it
                                commitSettings()
                            },
                            modifier = Modifier.testTag("settings_sustained_performance_toggle")
                        )
                        SettingDivider()
                        SettingSliderRow(
                            title = "Giới hạn hiệu suất tối đa",
                            desc = "Hạn chế giảm hiệu suất vì quá nhiệt bằng cách giảm hiệu suất tối đa.",
                            valueText = "$thermalPerfCap",
                            value = thermalPerfCap.toFloat(),
                            valueRange = 50f..100f,
                            steps = 9,
                            onValueChange = {
                                thermalPerfCap = ((it / 5f).roundToInt() * 5).coerceIn(50, 100)
                                commitSettings()
                            }
                        )
                        SettingDivider()
                        SettingSwitchRow(
                            title = "Dùng kết xuất bề mặt thay thế",
                            desc = "Có thể cải thiện hiệu năng nếu GPU bị nghẽn cổ chai.",
                            checked = alternateSurfaceRendering,
                            onCheckedChange = {
                                alternateSurfaceRendering = it
                                commitSettings()
                            },
                            modifier = Modifier.testTag("settings_alternate_surface_toggle")
                        )
                        SettingDivider()
                        SettingSwitchRow(
                            title = "Use ANGLE",
                            desc = "Allows using ANGLE instead of the system driver. Can fix graphical issues with mods on some GPUs",
                            checked = useAngle,
                            onCheckedChange = {
                                useAngle = it
                                commitSettings()
                            }
                        )
                        SettingDivider()
                        SettingSwitchRow(
                            title = "System ANGLE",
                            desc = "Allows using ANGLE instead of the system driver. Can fix graphical issues with mods on some GPUs",
                            checked = useSystemAngle,
                            onCheckedChange = {
                                useSystemAngle = it
                                commitSettings()
                            }
                        )
                        // Kunci 60 Hz untuk layar 90/120 Hz.
                        if (showRefreshLock) {
                            SettingDivider()
                            SettingSwitchRow(
                                title = "Kunci layar 60 Hz",
                                desc = "Layar 90/120 Hz dikunci 60 Hz saat main supaya gerak lebih rata dan sedikit lebih hemat panas. Cocok bila game jarang mencapai 90 FPS. HUD FPS menampilkan Hz yang aktif.",
                                checked = currentSettings.lockRefresh60,
                                onCheckedChange = { on ->
                                    SettingsManager.updateSettings(context, SettingsManager.settings.value.copy(lockRefresh60 = on))
                                }
                            )
                        }
                        SettingDivider()
                        SettingSwitchRow(
                            title = "Permukaan game TextureView",
                            desc = "Nyalakan bila tampilan game terputar 90 derajat atau terlihat miring padahal launcher landscape (kasus yang dilaporkan di Minecraft 26.x dengan renderer Zink). Sedikit lebih berat dari bawaan. Berlaku saat game dibuka lagi.",
                            checked = currentSettings.forceTextureView,
                            onCheckedChange = { on ->
                                SettingsManager.updateSettings(context, SettingsManager.settings.value.copy(forceTextureView = on))
                            }
                        )
                        SettingDivider()
                        SettingSwitchRow(
                            title = "Optimasi otomatis options.txt",
                            desc = "Mematikan awan dan bayangan secara otomatis supaya FPS lebih tinggi.",
                            checked = autoOptimizeMC,
                            onCheckedChange = {
                                autoOptimizeMC = it
                                commitSettings()
                            }
                        )
                        }

                        SettingsGroup("Perekaman layar") {
                        // Rekam layar bawaan (tombol Rekam ada di menu dalam game). Bawaannya sengaja hemat.
                        SettingChoiceRow(
                            title = "Kualitas rekaman layar",
                            desc = "Makin kecil makin ringan untuk HP. Hemat 540p, Ringan 720p, Tajam 1080p.",
                            options = listOf("eco" to "Hemat", "light" to "Ringan", "high" to "Tajam"),
                            selected = recordQuality,
                            onSelect = {
                                recordQuality = it
                                commitSettings()
                            }
                        )
                        SettingDivider()
                        SettingChoiceRow(
                            title = "FPS rekaman",
                            desc = "30 fps jauh lebih ringan. 60 fps hanya untuk HP yang kuat.",
                            options = listOf(30 to "30 fps", 60 to "60 fps"),
                            selected = recordFps,
                            onSelect = {
                                recordFps = it
                                commitSettings()
                            }
                        )
                        SettingDivider()
                        SettingChoiceRow(
                            title = "Isi rekaman",
                            desc = "Game saja: tombol layar disembunyikan dari video (tetap bisa disentuh). Dengan tombol: tombol layar ikut terekam. Bisa diganti juga dari menu dalam game.",
                            options = listOf(false to "Game saja", true to "Dengan tombol"),
                            selected = recordWithControls,
                            onSelect = {
                                recordWithControls = it
                                commitSettings()
                            }
                        )
                        SettingDivider()
                        SettingSwitchRow(
                            title = "Rekam suara mikrofon",
                            desc = "Menambahkan suara dari mikrofon ke rekaman. Suara game belum ikut terekam.",
                            checked = recordMic,
                            onCheckedChange = {
                                recordMic = it
                                commitSettings()
                            }
                        )
                        SettingDivider()
                        SettingSwitchRow(
                            title = "Hentikan rekaman saat HP terlalu panas",
                            desc = "Menyimpan rekaman dan berhenti otomatis supaya game tidak ikut melambat karena panas.",
                            checked = recordThermalGuard,
                            onCheckedChange = {
                                recordThermalGuard = it
                                commitSettings()
                            }
                        )
                        }

                        // Pengaturan untuk pengguna mahir: tidak perlu disentuh untuk bermain.
                        SettingsGroup("Lanjutan") {
                        SettingChoiceRow(
                            title = "Driver Vulkan",
                            desc = "Pilih implementasi driver Vulkan yang dipakai sistem/game.",
                            options = listOf("auto" to "Auto", "system" to "Sistem", "turnip" to "Turnip"),
                            selected = vulkanDriver,
                            onSelect = {
                                vulkanDriver = it
                                commitSettings()
                            }
                        )
                        SettingDivider()
                        SettingChoiceRow(
                            title = "API grafis",
                            desc = "Atur API grafis yang dipakai Minecraft modern 26.2+.",
                            options = listOf("DEFAULT" to "Default", "OPENGL" to "OpenGL", "VULKAN" to "Vulkan"),
                            selected = graphicsApi,
                            onSelect = {
                                graphicsApi = it
                                commitSettings()
                            }
                        )
                        SettingDivider()
                        SettingSwitchRow(
                            title = "Sử dụng trình điều khiển hệ thống cho Vulkan",
                            desc = "Buộc sử dụng trình đồ hoạ Vulkan hệ thống thay vì Turnip trên vi xử lí đồ hoạ Qualcomm Adreno. Chỉ ảnh hưởng tới Zink và mod sử dụng Vulkan.",
                            checked = zinkPreferSystemDriver,
                            onCheckedChange = { checked ->
                                if (checked && NuxVersionUtils.isAdrenoGPU()) {
                                    showAdrenoWarningDialog = true
                                } else {
                                    zinkPreferSystemDriver = checked
                                    commitSettings()
                                }
                            }
                        )
                        SettingDivider()
                        SettingSwitchRow(
                            title = "V-Sync Zink",
                            desc = "Sinkronisasi vertikal untuk renderer Kopper Zink lewat antarmuka sistem.",
                            checked = vsyncInZink,
                            onCheckedChange = {
                                vsyncInZink = it
                                commitSettings()
                            }
                        )
                        }

                        SettingsGroup("Các cài đặt khác") {
                            SettingSwitchRow(
                                title = "Verify game files",
                                desc = "Verify downloaded game files against the checksum and size set in the metadata. Metadata JSON files are verified regardless of this setting.",
                                checked = verifyGameFiles,
                                onCheckedChange = {
                                    verifyGameFiles = it
                                    commitSettings()
                                }
                            )
                            SettingDivider()
                            SettingSwitchRow(
                                title = "Fast game startup",
                                desc = "Don't verify file checksums upon sequential restarts. Makes the game start-up very fast.",
                                checked = fastGameStartup,
                                onCheckedChange = {
                                    fastGameStartup = it
                                    commitSettings()
                                }
                            )
                            SettingDivider()
                            SettingChoiceRow(
                                title = "Nguồn tải trò chơi",
                                desc = "Chọn máy chủ tải tài nguyên và phiên bản Minecraft Java.",
                                options = listOf("default" to "Mặc định", "bmclapi" to "BMCLAPI"),
                                selected = gameDownloadSource,
                                onSelect = {
                                    gameDownloadSource = it
                                    commitSettings()
                                }
                            )
                            SettingDivider()
                            SettingSwitchRow(
                                title = "Kiểm tra danh sách phiên bản trò chơi",
                                desc = "Khi bật, trình khởi chạy sẽ kiểm tra danh sách phiên bản trò chơi và thư viện.",
                                checked = checkGameVersionList,
                                onCheckedChange = {
                                    checkGameVersionList = it
                                    commitSettings()
                                }
                            )
                            SettingDivider()
                            SettingSwitchRow(
                                title = "Chế độ Local-Only (chỉ dùng cục bộ)",
                                desc = "Ẩn trình duyệt CurseForge trực tuyến và buộc dùng bộ nhớ đệm mod cục bộ để khởi động nhanh hơn ở khu vực kết nối yếu.",
                                checked = localOnlyMode,
                                onCheckedChange = {
                                    localOnlyMode = it
                                    commitSettings()
                                },
                                modifier = Modifier.testTag("settings_local_only_toggle")
                            )
                            SettingDivider()
                            SettingActionRow(
                                title = "Import data",
                                desc = "Begin importing data (instances, control maps) from the old app installation",
                                actionLabel = if (importDataBusy) "Đang nhập..." else "Chọn tệp",
                                onClick = {
                                    if (!importDataBusy) {
                                        importDataLauncher.launch(arrayOf("application/zip", "application/json", "*/*"))
                                    }
                                }
                            )
                        }

                        SettingsGroup("Discord Rich Presence") {
                            val discordInstalled = remember(context) {
                                com.aistudio.novacraft.launcher.core.utils.DiscordPresenceManager.isDiscordInstalled(context)
                            }
                            if (LocalSettingsFilter.current == null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp, bottom = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Trạng thái ứng dụng Discord trên máy",
                                        color = AmberUi.TextDim,
                                        fontSize = 12.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(
                                                if (discordInstalled) AmberUi.Accent.copy(alpha = 0.20f) else AmberUi.SurfaceRaised,
                                                CircleShape
                                            )
                                            .border(
                                                1.dp,
                                                if (discordInstalled) AmberUi.AccentEdge else AmberUi.BorderStrong,
                                                CircleShape
                                            )
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (discordInstalled) "Discord đã cài đặt" else "Chưa cài đặt Discord",
                                            color = if (discordInstalled) AmberUi.AccentSoft else AmberUi.TextDim,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                SettingDivider()
                            }
                            SettingSwitchRow(
                                title = "Discord Rich Presence khi chơi",
                                desc = "Hiển thị Amber Launcher cùng phiên bản Minecraft và loader (hoặc engine Rust) trên hồ sơ Discord của bạn khi chơi. Tên instance và tài khoản của bạn không được chia sẻ. Cần ứng dụng Discord đã cài và đã đăng nhập trên điện thoại này; không dùng thông tin đăng nhập hay token. Không dành cho người dùng dưới 13 tuổi. Có hiệu lực từ lần chơi tiếp theo.",
                                checked = discordRichPresenceOnPlay,
                                onCheckedChange = {
                                    discordRichPresenceOnPlay = it
                                    commitSettings()
                                },
                                modifier = Modifier.testTag("settings_discord_rpc_play_toggle")
                            )
                            SettingDivider()
                            SettingSwitchRow(
                                title = "Hiện máy chủ đang chơi",
                                desc = "Khi chơi trên máy chủ nhiều người chơi (loader Java), hồ sơ Discord của bạn hiển thị địa chỉ máy chủ, ví dụ Playing on mc.example.com. Bất kỳ ai xem được hồ sơ của bạn đều thấy địa chỉ đó. Địa chỉ mạng cục bộ không bao giờ được hiển thị. Địa chỉ được đọc từ nhật ký game: sau khi bạn chủ động rời máy chủ, địa chỉ có thể còn lại cho đến khi bạn vào máy chủ khác hoặc mở thế giới chơi đơn. Dùng cùng Discord Rich Presence khi chơi. Có hiệu lực từ lần chơi tiếp theo.",
                                checked = discordShowServer,
                                onCheckedChange = {
                                    discordShowServer = it
                                    commitSettings()
                                },
                                enabled = discordRichPresenceOnPlay,
                                modifier = Modifier.testTag("settings_discord_rpc_server_toggle")
                            )
                            SettingDivider()
                            SettingSwitchRow(
                                title = "Discord Rich Presence khi mở launcher",
                                desc = "Khi launcher đang mở và bạn chưa bắt đầu chơi, hồ sơ Discord của bạn hiển thị Amber Launcher với trạng thái Getting ready to play. Có hiệu lực vào lần mở launcher tiếp theo. Dùng cùng ứng dụng Discord bạn đã đăng nhập; không dành cho người dùng dưới 13 tuổi.",
                                checked = discordPresenceIdle,
                                onCheckedChange = {
                                    discordPresenceIdle = it
                                    commitSettings()
                                },
                                modifier = Modifier.testTag("settings_discord_rpc_idle_toggle")
                            )
                        }

                        SettingsGroup("Những cài đặt thử nghiệm") {
                            SettingSwitchRow(
                                title = "Kích hoạt đổ bóng",
                                desc = "Ghi các trình đổ bóng đã chuyển đổi vào tệp nhật ký.",
                                checked = dumpShadersEnabled,
                                onCheckedChange = {
                                    dumpShadersEnabled = it
                                    commitSettings()
                                }
                            )
                            SettingDivider()
                            SettingSwitchRow(
                                title = "Buộc trình kết xuất chạy trên lõi lớn",
                                desc = "Forces the game's render thread to run on the core with the highest maximum frequency.",
                                checked = forceBigCoreRender,
                                onCheckedChange = {
                                    forceBigCoreRender = it
                                    commitSettings()
                                }
                            )
                            SettingDivider()
                            SettingSwitchRow(
                                title = "Force rendering into the system memory",
                                desc = "Forces Freedreno to render directly into the system memory. Enable if performance is low or you experience rendering issues",
                                checked = freedrenoSysmem,
                                onCheckedChange = {
                                    freedrenoSysmem = it
                                    commitSettings()
                                }
                            )
                            SettingDivider()
                            SettingSwitchRow(
                                title = "Forcefully use OpenSL for audio",
                                desc = "Forcefully enables OpenSL backend in ALSoft instead of Oboe. Can fix audio capture issues on some devices. Disable if you see audio playback/capture issues",
                                checked = forceOpenSlAudio,
                                onCheckedChange = {
                                    forceOpenSlAudio = it
                                    commitSettings()
                                }
                            )
                            SettingDivider()
                            SettingSwitchRow(
                                title = "Disable CurseForge",
                                desc = "Tắt hoàn toàn nguồn CurseForge khi tìm kiếm mod/nội dung — chỉ dùng Modrinth. Mặc định cả Modrinth và CurseForge đều hoạt động không cần API key.",
                                checked = disableCurseForge,
                                onCheckedChange = {
                                    disableCurseForge = it
                                    commitSettings()
                                }
                            )
                            SettingDivider()
                            SettingFrame(
                                "Your own CurseForge API key",
                                "Tùy chọn. Cả Modrinth và CurseForge đều kết nối API trực tiếp không cần API key. Bạn có thể nhập key riêng từ console.curseforge.com nếu muốn tăng giới hạn."
                            ) {
                                val hasKey = curseForgeApiKeyInput.trim().isNotEmpty()
                                val hasCustomOrBuildKey = remember(curseForgeApiKeyInput, currentSettings.curseForgeApiKey) {
                                    curseForgeApiKeyInput.trim().isNotEmpty() || CurseForgeApi.hasApiKey()
                                }
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        SettingRowText(
                                            title = "Your own CurseForge API key",
                                            desc = "Tùy chọn. Modrinth và CurseForge đều kết nối API trực tiếp không cần API key. Chỉ thêm key riêng từ console.curseforge.com nếu muốn dùng hạn mức riêng.",
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(
                                                    if (disableCurseForge || localOnlyMode) AmberUi.SurfaceRaised
                                                    else AmberUi.Accent.copy(alpha = 0.20f),
                                                    CircleShape
                                                )
                                                .border(
                                                    1.dp,
                                                    if (disableCurseForge || localOnlyMode) AmberUi.BorderStrong
                                                    else AmberUi.AccentEdge,
                                                    CircleShape
                                                )
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = when {
                                                    localOnlyMode -> "Ẩn (Local-Only)"
                                                    disableCurseForge -> "Đã tắt"
                                                    hasCustomOrBuildKey -> "Dùng API key"
                                                    else -> "Không cần key"
                                                },
                                                color = if (!disableCurseForge && !localOnlyMode) AmberUi.AccentSoft else AmberUi.TextDim,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    NuxTextField(
                                        value = curseForgeApiKeyInput,
                                        onValueChange = { curseForgeApiKeyInput = it },
                                        placeholder = "Nhập CurseForge API Key...",
                                        visualTransformation = if (curseForgeKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        trailingContent = {
                                            IconButton(
                                                onClick = { curseForgeKeyVisible = !curseForgeKeyVisible },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (curseForgeKeyVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                                    contentDescription = if (curseForgeKeyVisible) "Ẩn khóa" else "Hiện khóa",
                                                    tint = AmberUi.TextDim,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        AmberPillButton(
                                            text = "Lưu API key",
                                            icon = Icons.Outlined.Check,
                                            onClick = {
                                                commitSettings()
                                                Toast.makeText(
                                                    context,
                                                    if (curseForgeApiKeyInput.trim().isNotEmpty()) "Đã lưu API key trong Cài đặt" else "Đã đặt lại API key về trống",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            },
                                            filled = true,
                                            modifier = Modifier
                                                .defaultMinSize(minHeight = 44.dp)
                                                .testTag("settings_save_api_key")
                                        )
                                        if (hasKey) {
                                            AmberPillButton(
                                                text = "Xóa",
                                                icon = Icons.Outlined.DeleteOutline,
                                                onClick = {
                                                    curseForgeApiKeyInput = ""
                                                    commitSettings()
                                                    Toast.makeText(context, "Đã xóa API key", Toast.LENGTH_SHORT).show()
                                                },
                                                filled = false,
                                                modifier = Modifier
                                                    .defaultMinSize(minHeight = 44.dp)
                                                    .testTag("settings_clear_api_key")
                                            )
                                        }
                                    }
                                }
                            }
                            SettingDivider()
                            SettingSwitchRow(
                                title = "Enable UBWC compression workaround",
                                desc = "Enables \"tp_ubwc_flag_hint\" flag needed for select Adreno GPUs (Adreno 740). Enable if you see rendering issues on freedreno or Turnip",
                                checked = ubwcWorkaround,
                                onCheckedChange = {
                                    ubwcWorkaround = it
                                    commitSettings()
                                }
                            )
                        }

                        // Plugin native library (format Zalith/FCL) untuk mod yang butuh pustaka native Android,
                        // mis. Distant Horizons. Dipakai hanya saat game dijalankan; tanpa plugin tidak ada beban.
                        SettingsGroup("Plugin native library") {
                        if (nativePlugins.isEmpty()) {
                            SettingValueRow(
                                title = "Belum ada plugin terpasang",
                                desc = "Pasang APK plugin native library (format Zalith/FCL) yang dibutuhkan mod, misalnya Distant Horizons, lalu ketuk untuk memindai ulang.",
                                value = if (nativeScanBusy) "Memindai..." else "Pindai",
                                onClick = { rescanNativePlugins() }
                            )
                        } else {
                            nativePlugins.forEachIndexed { index, plugin ->
                                if (index > 0) SettingDivider()
                                val range = listOfNotNull(
                                    plugin.minMcVersion?.let { ">= $it" },
                                    plugin.maxMcVersion?.let { "<= $it" }
                                ).joinToString(" ")
                                SettingSwitchRow(
                                    title = plugin.displayName,
                                    desc = plugin.packageName + if (range.isNotEmpty()) " · Minecraft $range" else "",
                                    checked = plugin.packageName !in disabledNativePlugins,
                                    onCheckedChange = { on ->
                                        disabledNativePlugins = if (on) {
                                            disabledNativePlugins - plugin.packageName
                                        } else {
                                            (disabledNativePlugins + plugin.packageName).distinct()
                                        }
                                        commitSettings()
                                    }
                                )
                            }
                            SettingDivider()
                            SettingValueRow(
                                title = "Pindai ulang plugin",
                                desc = "Gunakan setelah memasang atau menghapus plugin. Plugin juga dipindai otomatis tiap game dijalankan.",
                                value = if (nativeScanBusy) "Memindai..." else "Pindai",
                                onClick = { rescanNativePlugins() }
                            )
                        }
                        }
                    }

                    // ===================== RUST =====================
                    // Pengaturan mesin Rust (Horizon). Disimpan di config mesin sendiri, tidak lewat LauncherSettings.
                    if (searching || section == 4) {
                        RustSettingsLoader()
                        SettingsGroup("Mesin Rust") { RustStatusRows() }
                        SettingsGroup("Grafis") { RustGraphicsRows() }
                        SettingsGroup("Performa") { RustPerformanceRows() }
                        SettingsGroup("Layar") { RustScreenRows() }
                        SettingsGroup("Kontrol") { RustControlRows() }
                        SettingsGroup("Diagnostik") { RustDiagnosticRows() }
                        SettingsGroup("Permainan") { RustGameRows() }
                    }

                    // ===================== KONTROL =====================
                    if (searching || section == 1) {
                        ControlSettingsSection(
                            onOpenGuiEditor = onOpenGuiEditor,
                            onExportLayout = { fileName -> exportLayoutLauncher.launch(fileName) },
                            onImportLayout = { importLayoutLauncher.launch(arrayOf("*/*")) }
                        )
                    }

                    // ===================== TAMPILAN =====================
                    if (searching || section == 2) {
                        SettingsGroup("ANIMATIONS") {
                            val enabledCount = listOf(
                                animScreenTransitions,
                                animDialogsPopups,
                                animButtonEffects,
                                animListsContent
                            ).count { it }
                            SettingValueRow(
                                title = "Animation types",
                                desc = "Screen transitions, Dialogs and popups, Button press effects, Lists and content",
                                value = "$enabledCount/4 bật",
                                onClick = { showAnimTypesDialog = true }
                            )
                        }

                        SettingsGroup("STARTUP") {
                            SettingSwitchRow(
                                title = "Startup sound",
                                desc = "Short Craft Studio chime with the launcher intro (silent in silent/vibrate mode)",
                                checked = startupSoundEnabled,
                                onCheckedChange = {
                                    startupSoundEnabled = it
                                    commitSettings()
                                }
                            )
                        }

                        SettingsGroup("Bahasa") {
                            SettingValueRow(
                                title = "Bahasa",
                                desc = "Bahasa teks di launcher. Berlaku langsung tanpa memulai ulang.",
                                value = languageOptions.firstOrNull { it.first == appLanguage }?.second ?: "",
                                onClick = { showLanguageDialog = true }
                            )
                        }

                        SettingsGroup("Tema") {
                            SettingFrame(
                                "Mode tema",
                                "Pilih tampilan Gelap, Terang, atau mengikuti tema sistem Android."
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp)
                                ) {
                                    SettingRowText(
                                        title = "Mode tema",
                                        desc = "Pilih tampilan Gelap, Terang, atau mengikuti tema sistem Android."
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    val themeChoices = listOf(
                                        Triple(AmberThemeMode.DARK, Icons.Outlined.DarkMode, "Gelap"),
                                        Triple(AmberThemeMode.LIGHT, Icons.Outlined.LightMode, "Terang"),
                                        Triple(AmberThemeMode.SYSTEM, Icons.Outlined.BrightnessAuto, "Ikuti sistem")
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        themeChoices.forEach { (mode, icon, label) ->
                                            val selected = themeModeId == mode.id
                                            val chipShape = RoundedCornerShape(16.dp)
                                            Row(
                                                modifier = Modifier
                                                    .defaultMinSize(minHeight = 48.dp)
                                                    .clip(chipShape)
                                                    .background(
                                                        if (selected) AmberUi.Accent else AmberUi.SurfaceRaised,
                                                        chipShape
                                                    )
                                                    .border(
                                                        1.dp,
                                                        if (selected) AmberUi.AccentEdge else AmberUi.BorderStrong,
                                                        chipShape
                                                    )
                                                    .clickable {
                                                        themeModeId = mode.id
                                                        AmberUi.setThemeMode(mode.id)
                                                        commitSettings()
                                                    }
                                                    .testTag("theme_mode_${mode.id}")
                                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                                            ) {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = null,
                                                    tint = if (selected) AmberUi.OnAccent else AmberUi.AccentSoft,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = label,
                                                    color = if (selected) AmberUi.OnAccent else AmberUi.Text,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            SettingDivider()
                            SettingFrame("Aksen warna", "Warna tombol dan penanda terpilih di seluruh launcher.") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp)
                                ) {
                                    SettingRowText(
                                        title = "Aksen warna",
                                        desc = "Warna tombol dan penanda terpilih di seluruh launcher."
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AMBER_ACCENTS.forEach { accent ->
                                            val picked = accent.id == accentId
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(accent.base, CircleShape)
                                                    .border(
                                                        if (picked) 2.5.dp else 1.dp,
                                                        if (picked) AmberUi.Text else AmberUi.BorderStrong,
                                                        CircleShape
                                                    )
                                                    .clickable {
                                                        accentId = accent.id
                                                        AmberUi.setAccent(accent.id)
                                                        commitSettings()
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (picked) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Check,
                                                        contentDescription = accent.label,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Text(
                                        text = AMBER_ACCENTS.firstOrNull { it.id == accentId }?.label ?: "",
                                        color = AmberUi.TextDim,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                }
                            }
                            SettingDivider()
                            SettingChoiceRow(
                                title = "Gaya tampilan",
                                desc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    "Default memakai tema NovaCraft sendiri. Liquid Glass memburamkan latar di belakang panel dan tombol."
                                } else {
                                    "Liquid Glass butuh Android 12 ke atas. Di HP ini otomatis memakai Default."
                                },
                                options = AmberStyle.entries.map { it.id to it.label },
                                selected = uiStyleId,
                                onSelect = {
                                    uiStyleId = it
                                    AmberUi.setStyle(it)
                                    commitSettings()
                                }
                            )
                            SettingDivider()
                            SettingChoiceRow(
                                title = "Color source",
                                desc = "Chọn nguồn màu chủ đạo cho giao diện launcher (bảng màu mặc định hoặc lấy màu động hệ thống).",
                                options = AmberColorSource.entries.map { it.id to it.label },
                                selected = colorSourceId,
                                onSelect = {
                                    colorSourceId = it
                                    AmberUi.setColorSource(it, context)
                                    commitSettings()
                                }
                            )
                        }

                        SettingsGroup("Latar") {
                            SettingBackgroundRow(onChange = onChangeBackground)
                            SettingDivider()
                            SettingSliderRow(
                                title = "Redupkan latar",
                                desc = "Seberapa gelap latar di layar selain Home.",
                                valueText = "$bgDim%",
                                value = bgDim.toFloat(),
                                valueRange = 0f..100f,
                                steps = 19,
                                onValueChange = {
                                    bgDim = ((it / 5f).roundToInt() * 5).coerceIn(0, 100)
                                    commitSettings()
                                }
                            )
                            SettingDivider()
                        SettingSwitchRow(
                            title = "Gerakkan GIF dan video",
                            desc = "Matikan untuk menampilkan gambar diam saja. Paling hemat baterai dan RAM.",
                            checked = bgAnimate,
                            onCheckedChange = {
                                bgAnimate = it
                                commitSettings()
                            }
                        )
                        SettingDivider()
                        SettingSwitchRow(
                            title = "Jeda saat Hemat Baterai",
                            desc = "GIF dan video berhenti otomatis selama mode Hemat Baterai HP menyala.",
                            checked = bgPauseOnPowerSave,
                            onCheckedChange = {
                                bgPauseOnPowerSave = it
                                commitSettings()
                            }
                        )
                        SettingDivider()
                        SettingSwitchRow(
                            title = "Suara video",
                            desc = "Bawaan mati. Otomatis bisu bila aplikasi lain sedang memutar musik.",
                            checked = bgSound,
                            onCheckedChange = {
                                bgSound = it
                                commitSettings()
                            }
                        )
                        if (bgSound) {
                            SettingDivider()
                            SettingSliderRow(
                                title = "Volume video",
                                valueText = "$bgVolume%",
                                value = bgVolume.toFloat(),
                                valueRange = 0f..100f,
                                steps = 19,
                                onValueChange = {
                                    bgVolume = ((it / 5f).roundToInt() * 5).coerceIn(0, 100)
                                    commitSettings()
                                }
                            )
                        }
                        }
                    }

                    // ===================== TENTANG =====================
                    if (searching || section == 3) {
                        if (!searching) {
                            SettingsGroup("Pembaruan") {
                                UpdateSettingsSection()
                            }
                            SettingsGroup("Log game") {
                                GameLogSettingsSection()
                            }
                        }
                        SettingsGroup("Izin aplikasi") {
                            AppPermissionsSettingsSection()
                        }
                        SettingsGroup("Aplikasi") {
                            SettingActionRow(
                                title = "Tentang & lisensi",
                                desc = "Informasi aplikasi, lisensi GPL-3.0, dan kredit.",
                                actionLabel = "Buka",
                                onClick = { showAboutDialog = true }
                            )
                            SettingDivider()
                            SettingActionRow(
                                title = "Reset ke default",
                                desc = "Semua pengaturan kembali ke bawaan. Perubahan lain tersimpan otomatis.",
                                actionLabel = "Reset default",
                                danger = true,
                                onClick = {
                                    SettingsManager.resetToDefaults(context)
                    val reset = SettingsManager.settings.value
                    ramMb = reset.ramMb
                    initialHeapMb = reset.initialHeapMb
                    customJvmArgs = reset.customJvmArgs
                    defaultRuntime = reset.defaultJavaRuntime
                    jarSandboxEnabled = reset.jarSandboxEnabled
                    resolutionRatio = reset.resolutionRatio
                    autoOptimizeMC = reset.autoOptimizeMinecraft
                    sustainedPerf = reset.sustainedPerformanceMode
                    thermalPerfCap = if (reset.thermalPerfCap <= 0) 90 else reset.thermalPerfCap
                    selectedRenderer = reset.selectedRenderer
                    vulkanDriver = reset.vulkanDriver
                    graphicsApi = reset.graphicsApi
                    zinkPreferSystemDriver = reset.zinkPreferSystemDriver
                    vsyncInZink = reset.vsyncInZink
                    forceVsync = reset.forceVsync
                    useAngle = reset.useAngle
                    useSystemAngle = reset.useSystemAngle
                    curseForgeApiKeyInput = reset.curseForgeApiKey
                    recordQuality = reset.recordQuality
                    recordFps = reset.recordFps
                    recordMic = reset.recordMic
                    recordWithControls = reset.recordWithControls
                    recordThermalGuard = reset.recordThermalGuard
                    bgAnimate = reset.homeBgAnimate
                    bgPauseOnPowerSave = reset.homeBgPauseOnPowerSave
                    bgSound = reset.homeBgSound
                    bgVolume = reset.homeBgVolume
                    ambientOn = reset.homeAmbientEnabled
                    ambientIntensity = reset.homeAmbientIntensity
                    offlineMode = reset.offlineMode
                    persistAccountSession = reset.persistAccountSession
                    AccountManager.setSessionPersistence(context, reset.persistAccountSession)
                    themeModeId = reset.themeMode
                    colorSourceId = reset.colorSource
                    accentId = reset.accentColor
                    uiStyleId = reset.uiStyle
                    bgDim = reset.bgDim
                    animScreenTransitions = reset.animScreenTransitions
                    animDialogsPopups = reset.animDialogsPopups
                    animButtonEffects = reset.animButtonEffects
                    animListsContent = reset.animListsContent
                    startupSoundEnabled = reset.startupSoundEnabled
                    verifyGameFiles = reset.verifyGameFiles
                    fastGameStartup = reset.fastGameStartup
                    gameDownloadSource = reset.gameDownloadSource
                    checkGameVersionList = reset.checkGameVersionList
                    localOnlyMode = reset.localOnlyMode
                    alternateSurfaceRendering = reset.alternateSurfaceRendering
                    discordRichPresenceOnPlay = reset.discordRichPresenceOnPlay
                    discordShowServer = reset.discordShowServer
                    discordPresenceIdle = reset.discordPresenceIdle
                    dumpShadersEnabled = reset.dumpShadersEnabled
                    forceBigCoreRender = reset.forceBigCoreRender
                    freedrenoSysmem = reset.freedrenoSysmem
                    forceOpenSlAudio = reset.forceOpenSlAudio
                    disableCurseForge = reset.disableCurseForge
                    ubwcWorkaround = reset.ubwcWorkaround
                    AmberUi.setThemeMode(reset.themeMode)
                    AmberUi.setColorSource(reset.colorSource, context)
                    AmberUi.setAccent(reset.accentColor)
                    AmberUi.setStyle(reset.uiStyle)
                    AmberUi.applyDim(reset.bgDim / 100f)
                    AmberUi.setAnimations(
                        reset.animScreenTransitions,
                        reset.animDialogsPopups,
                        reset.animButtonEffects,
                        reset.animListsContent
                    )
                    Toast.makeText(context, "Pengaturan di-reset ke default!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        if (showAnimTypesDialog) {
            NuxDialog(onDismissRequest = { showAnimTypesDialog = false }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Animation types",
                        color = AmberUi.Text,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tùy chỉnh các loại hiệu ứng chuyển động trong trình khởi chạy.",
                        color = AmberUi.TextDim,
                        fontSize = 12.sp
                    )
                    SettingSwitchRow(
                        title = "Screen transitions",
                        desc = null,
                        checked = animScreenTransitions,
                        onCheckedChange = {
                            animScreenTransitions = it
                            AmberUi.setAnimations(animScreenTransitions, animDialogsPopups, animButtonEffects, animListsContent)
                            commitSettings()
                        }
                    )
                    SettingDivider()
                    SettingSwitchRow(
                        title = "Dialogs and popups",
                        desc = null,
                        checked = animDialogsPopups,
                        onCheckedChange = {
                            animDialogsPopups = it
                            AmberUi.setAnimations(animScreenTransitions, animDialogsPopups, animButtonEffects, animListsContent)
                            commitSettings()
                        }
                    )
                    SettingDivider()
                    SettingSwitchRow(
                        title = "Button press effects",
                        desc = null,
                        checked = animButtonEffects,
                        onCheckedChange = {
                            animButtonEffects = it
                            AmberUi.setAnimations(animScreenTransitions, animDialogsPopups, animButtonEffects, animListsContent)
                            commitSettings()
                        }
                    )
                    SettingDivider()
                    SettingSwitchRow(
                        title = "Lists and content",
                        desc = null,
                        checked = animListsContent,
                        onCheckedChange = {
                            animListsContent = it
                            AmberUi.setAnimations(animScreenTransitions, animDialogsPopups, animButtonEffects, animListsContent)
                            commitSettings()
                        }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        AmberPillButton(
                            text = "Đóng",
                            onClick = { showAnimTypesDialog = false },
                            filled = true
                        )
                    }
                }
            }
        }

        // Pilih bahasa
        if (showLanguageDialog) {
            AmberPickerDialog(
                title = "Bahasa",
                options = languageOptions,
                selectedId = appLanguage,
                onPick = { picked ->
                    appLanguage = picked
                    Tr.setLanguage(context, picked)
                    showLanguageDialog = false
                },
                onDismiss = { showLanguageDialog = false }
            )
        }


        // Renderer Selection Dialog (Responsive across Portrait & Landscape)
        if (showRendererDialog) {
            // Pindai plugin di thread latar; daftar langsung tampil dan diperbarui setelah selesai.
            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    try { NuxRendererPluginManager.scanPlugins(context) } catch (_: Throwable) {}
                }
                rendererListTick++
            }
            val rendererList = remember(rendererListTick) { NuxRendererRegistry.availableRenderers }
            NuxDialog(
                onDismissRequest = { showRendererDialog = false },
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp),
                fillMaxHeight = true
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Layers,
                                contentDescription = null,
                                tint = NuxColors.SageGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "PILIH RENDERER GRAFIK",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = NuxColors.DarkGray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Sesuaikan backend grafis dengan Minecraft & GPU perangkat",
                                    fontSize = 11.sp,
                                    color = NuxColors.GrayNeutral,
                                    maxLines = 2
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { showRendererDialog = false },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Close,
                                contentDescription = "Tutup",
                                tint = NuxColors.DarkGray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Scrollable Renderer List
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(rendererList) { rendererItem ->
                            val isSelected = selectedRenderer.equals(rendererItem.id, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) NuxColors.YellowPale else NuxColors.SurfaceInput
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) NuxColors.MintGreen else NuxColors.CardBorder,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        selectedRenderer = rendererItem.id
                                        commitSettings()
                                        showRendererDialog = false
                                        Toast.makeText(context, "Renderer diubah ke: ${rendererItem.displayName}", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = {
                                                    selectedRenderer = rendererItem.id
                                                    commitSettings()
                                                    showRendererDialog = false
                                                    Toast.makeText(context, "Renderer diubah ke: ${rendererItem.displayName}", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = RadioButtonDefaults.colors(
                                                    selectedColor = NuxColors.ForestGreen,
                                                    unselectedColor = NuxColors.GrayNeutral
                                                ),
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = rendererItem.displayName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (isSelected) NuxColors.SageGreen else NuxColors.DarkGray,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (rendererItem.isConfigurable) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(NuxColors.SurfaceElevated)
                                                        .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(8.dp))
                                                        .clickable {
                                                            selectedConfigRenderer = rendererItem
                                                            showRendererDialog = false
                                                            showRendererConfigDialog = true
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Settings,
                                                        contentDescription = "Konfigurasi ${rendererItem.displayName}",
                                                        tint = NuxColors.SageGreen,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        if (isSelected) NuxColors.ForestGreen else NuxColors.SurfaceElevated,
                                                        RoundedCornerShape(6.dp)
                                                    )
                                                    .border(NuxSizes.BorderWidth, NuxColors.CardBorder, RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = rendererItem.badge,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) NuxColors.Ink else NuxColors.GrayNeutral,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = rendererItem.summary,
                                        fontSize = 11.5.sp,
                                        color = NuxColors.GrayNeutral,
                                        lineHeight = 16.sp,
                                        modifier = Modifier.padding(start = 30.dp)
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "Kesesuaian: ${rendererItem.compatibility}",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = NuxColors.SageGreen,
                                        modifier = Modifier.padding(start = 30.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        NuxButton(
                            onClick = { showRendererDialog = false },
                            modifier = Modifier.defaultMinSize(minHeight = 40.dp),
                            backgroundColor = NuxColors.SurfaceInput,
                            contentColor = NuxColors.DarkGray,
                            cornerRadius = 10.dp
                        ) {
                            Text("TUTUP", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        }
    }

    // Adreno GPU Warning Dialog
    if (showAdrenoWarningDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showAdrenoWarningDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            GamepadWindowHook()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                NuxCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp)
                        .wrapContentHeight(),
                    backgroundColor = NuxColors.SurfaceElevated,
                    borderColor = NuxColors.DarkGray.copy(alpha = 0.40f),
                    cornerRadius = NuxSizes.CornerRadiusLarge,
                    fillMaxHeight = false
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚠️", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "PERINGATAN DRIVER ADRENO",
                                color = NuxColors.AmberDark,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                        }
                        Text(
                            text = "Launcher mendeteksi perangkatmu menggunakan GPU Adreno. Mengaktifkan opsi ini dapat menyebabkan perender Zink tidak berfungsi atau crash. Apakah Anda yakin ingin mengaktifkannya?",
                            color = NuxColors.GrayNeutral,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                NuxButton(
                                    onClick = {
                                        zinkPreferSystemDriver = false
                                        commitSettings()
                                        showAdrenoWarningDialog = false
                                    },
                                    backgroundColor = NuxColors.SurfaceInput,
                                    contentColor = NuxColors.DarkGray,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "BATAL",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                NuxButton(
                                    onClick = {
                                        zinkPreferSystemDriver = true
                                        commitSettings()
                                        showAdrenoWarningDialog = false
                                    },
                                    backgroundColor = NuxColors.Amber,
                                    contentColor = Color.Black,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "AKTIFKAN",
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

    // Dialog Konfigurasi Renderer V2 (persis Zalith untuk MobileGlues, NGG dll)
    if (showRendererConfigDialog && selectedConfigRenderer != null) {
        NuxRendererV2ConfigDialog(
            rendererInfo = selectedConfigRenderer,
            onDismiss = { showRendererConfigDialog = false }
        )
    }

    // Dialog Tentang & Lisensi Open Source (GPL-3.0 & Zalith Compliance)
    pendingImport?.let { pending ->
        NuxImportLayoutDialog(
            buttonCount = pending.buttons.size,
            macroCommands = pending.macroCommands,
            sourceName = pending.source,
            skipped = pending.skipped,
            onConfirm = {
                pendingImport = null
                scope.launch {
                    val count = withContext(Dispatchers.IO) {
                        ControlLayoutManager.importLayout(context, pending.buttons).size
                    }
                    Toast.makeText(context, "Layout kontrol diimpor ($count tombol)", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { pendingImport = null }
        )
    }

    if (showJavaRuntimeDialog) {
        var detectedRuntimes by remember { mutableStateOf<List<JavaRuntimeManager.DetectedJavaRuntime>>(emptyList()) }
        var installedMap by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
        var bundledMap by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
        var extractingId by remember { mutableStateOf<String?>(null) }
        var extractProgress by remember { mutableFloatStateOf(0f) }
        var extractStatus by remember { mutableStateOf("") }
        var deleteMode by remember { mutableStateOf(false) }
        var runtimePendingDelete by remember { mutableStateOf<String?>(null) }
        var refreshTick by remember { mutableIntStateOf(0) }

        val runtimeImporter = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->
            if (uri != null && extractingId == null) {
                extractingId = "__importing__"
                extractProgress = 0.05f
                extractStatus = "Đang chuẩn bị nhập gói Java..."
                scope.launch {
                    val fileName = NovaUnifiedFileImportEngine.getFileName(context, uri)
                    val res = JavaRuntimeManager.importCustomRuntime(
                        context = context,
                        uri = uri,
                        rawFileName = fileName
                    ) { p, msg ->
                        extractProgress = p
                        extractStatus = msg
                    }
                    extractingId = null
                    if (res.isSuccess) {
                        val imported = res.getOrNull()
                        extractProgress = 1f
                        extractStatus = "Đã thêm ${imported?.id ?: "runtime"} (${imported?.versionString.orEmpty()})"
                        deleteMode = false
                        refreshTick++
                    } else {
                        extractProgress = 0f
                        extractStatus = res.exceptionOrNull()?.message ?: "Không thể nhập gói Java"
                    }
                }
            }
        }

        LaunchedEffect(refreshTick) {
            withContext(Dispatchers.IO) {
                val detected = JavaRuntimeManager.detectAvailableJavaRuntimes(context)
                val known = JavaRuntimeManager.knownRuntimeIds()
                val installed = known.associateWith { JavaRuntimeManager.isRuntimeInstalled(context, it) }
                val bundled = known.associateWith { JavaRuntimeManager.hasBundledRuntimeAsset(context, it) }
                detectedRuntimes = detected
                installedMap = installed
                bundledMap = bundled
            }
        }

        NuxDialog(
            onDismissRequest = { if (extractingId == null) showJavaRuntimeDialog = false },
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp),
            fillMaxHeight = true
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                // Header giống ảnh mẫu: icon badge + "Các hệ thống thời gian chạy Java" + nút đóng tròn (X)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AmberUi.SurfaceRaised)
                                .border(1.dp, AmberUi.BorderStrong, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Coffee,
                                contentDescription = null,
                                tint = AmberUi.Text,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Các hệ thống thời gian chạy Java",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = AmberUi.Text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(AmberUi.SurfaceRaised.copy(alpha = 0.7f))
                            .border(1.dp, AmberUi.BorderStrong, CircleShape)
                            .clickable(enabled = extractingId == null) { showJavaRuntimeDialog = false }
                            .testTag("java_runtime_dialog_close"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Đóng",
                            tint = AmberUi.Text,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(AmberUi.Border)
                )

                if (extractStatus.isNotBlank() || extractingId != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (extractStatus.isNotBlank()) {
                            Text(
                                text = extractStatus,
                                color = AmberUi.AccentSoft,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (extractingId != null) {
                            LinearProgressIndicator(
                                progress = { extractProgress.coerceIn(0.05f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(CircleShape),
                                color = AmberUi.AccentSoft,
                                trackColor = AmberUi.SurfaceRaised
                            )
                        }
                    }
                }

                val allIds = (JavaRuntimeManager.knownRuntimeIds() + detectedRuntimes.map { it.id }).distinct()
                val hasDeletableInstalled = allIds.any { id ->
                    installedMap[id] == true || detectedRuntimes.any {
                        it.id.equals(id, ignoreCase = true) && it.source == JavaRuntimeManager.RuntimeSource.LAUNCHER_INTERNAL
                    }
                }

                // Danh sách các hệ thống thời gian chạy Java (giống bố cục 2 cột trong ảnh)
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Tùy chọn Auto (tự chọn Java phù hợp từng phiên bản Minecraft)
                    item(key = "__auto__") {
                        val isAutoSelected = defaultRuntime.equals("auto", ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = extractingId == null && !deleteMode && !isAutoSelected) {
                                    defaultRuntime = "auto"
                                    commitSettings()
                                }
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Auto",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = AmberUi.Text
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Tự động theo phiên bản Minecraft",
                                    fontSize = 12.5.sp,
                                    color = AmberUi.TextDim
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .widthIn(min = 136.dp)
                                    .defaultMinSize(minHeight = 44.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (deleteMode) {
                                    Text(
                                        text = "Hệ thống",
                                        color = AmberUi.TextDim.copy(alpha = 0.7f),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                } else if (isAutoSelected) {
                                    Text(
                                        text = "Mặc định",
                                        color = AmberUi.TextDim,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                } else {
                                    TextButton(
                                        onClick = {
                                            defaultRuntime = "auto"
                                            commitSettings()
                                        },
                                        enabled = extractingId == null,
                                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                                    ) {
                                        Text(
                                            text = "Đặt làm mặc định",
                                            color = AmberUi.Text,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    items(allIds, key = { it }) { rtId ->
                        val isSelected = defaultRuntime.equals(rtId, ignoreCase = true)
                        val detectedMatch = detectedRuntimes.firstOrNull { it.id.equals(rtId, ignoreCase = true) }
                        val isInstalled = installedMap[rtId] == true || detectedMatch != null
                        val hasBundled = bundledMap[rtId] == true
                        val busyHere = extractingId == rtId
                        val canDelete = isInstalled && (
                            installedMap[rtId] == true ||
                                detectedMatch?.source == JavaRuntimeManager.RuntimeSource.LAUNCHER_INTERNAL
                            )
                        val detectedVer = detectedMatch?.versionString?.takeIf { it.isNotBlank() }
                        val versionSubtext = when {
                            isInstalled && detectedVer != null -> detectedVer
                            isInstalled -> "Java ${detectedMatch?.majorVersion ?: rtId.removePrefix("jre-")}"
                            else -> "Not Installed"
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .then(
                                    if (!deleteMode && isInstalled && !isSelected) {
                                        Modifier.clickable(enabled = extractingId == null) {
                                            defaultRuntime = rtId
                                            commitSettings()
                                        }
                                    } else Modifier
                                )
                                .padding(horizontal = 8.dp, vertical = 10.dp)
                                .testTag("java_runtime_row_$rtId"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = JavaRuntimeManager.getInternalRuntimeLabel(rtId),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = AmberUi.Text
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = versionSubtext,
                                    fontSize = 12.5.sp,
                                    color = AmberUi.TextDim
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .widthIn(min = 136.dp)
                                    .defaultMinSize(minHeight = 44.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                when {
                                    deleteMode -> {
                                        if (canDelete) {
                                            TextButton(
                                                onClick = { runtimePendingDelete = rtId },
                                                enabled = extractingId == null,
                                                modifier = Modifier
                                                    .defaultMinSize(minHeight = 48.dp)
                                                    .testTag("java_runtime_delete_$rtId")
                                            ) {
                                                Text(
                                                    text = "Xóa",
                                                    color = AmberUi.Danger,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = if (isInstalled) "Hệ thống" else "Not Installed",
                                                color = AmberUi.TextDim.copy(alpha = 0.7f),
                                                fontSize = 12.5.sp
                                            )
                                        }
                                    }
                                    isSelected && isInstalled -> {
                                        Text(
                                            text = "Mặc định",
                                            color = AmberUi.TextDim,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    isInstalled -> {
                                        TextButton(
                                            onClick = {
                                                defaultRuntime = rtId
                                                commitSettings()
                                            },
                                            enabled = extractingId == null,
                                            modifier = Modifier
                                                .defaultMinSize(minHeight = 48.dp)
                                                .testTag("java_runtime_set_default_$rtId")
                                        ) {
                                            Text(
                                                text = "Đặt làm mặc định",
                                                color = AmberUi.Text,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                    hasBundled -> {
                                        TextButton(
                                            onClick = {
                                                if (extractingId == null) {
                                                    extractingId = rtId
                                                    extractProgress = 0.05f
                                                    extractStatus = "Đang tải & cài đặt $rtId..."
                                                    scope.launch {
                                                        val res = JavaRuntimeManager.extractRuntime(context, rtId) { p, msg ->
                                                            extractProgress = p
                                                            extractStatus = msg
                                                        }
                                                        extractingId = null
                                                        if (res.isSuccess) {
                                                            extractProgress = 1f
                                                            extractStatus = "Đã cài đặt ${JavaRuntimeManager.getInternalRuntimeLabel(rtId)} thành công!"
                                                            refreshTick++
                                                        } else {
                                                            extractProgress = 0f
                                                            extractStatus = "Lỗi cài đặt $rtId: ${res.exceptionOrNull()?.message ?: ""}"
                                                        }
                                                    }
                                                }
                                            },
                                            enabled = extractingId == null,
                                            modifier = Modifier
                                                .defaultMinSize(minHeight = 48.dp)
                                                .testTag("java_runtime_download_$rtId")
                                        ) {
                                            Text(
                                                text = if (busyHere) "Đang tải..." else "Download",
                                                color = if (busyHere) AmberUi.AccentSoft else AmberUi.Text,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                    else -> {
                                        TextButton(
                                            onClick = {
                                                if (extractingId == null) {
                                                    runtimeImporter.launch(arrayOf("*/*"))
                                                }
                                            },
                                            enabled = extractingId == null,
                                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                                        ) {
                                            Text(
                                                text = "Download",
                                                color = AmberUi.TextDim,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(AmberUi.Border)
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Thanh hành động dưới cùng giống ảnh mẫu: "Xóa runtime" (trái) và "Thêm mới" (phải)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            if (extractingId == null) {
                                if (!deleteMode && !hasDeletableInstalled) {
                                    Toast.makeText(context, "Chưa có runtime đã cài đặt để xóa", Toast.LENGTH_SHORT).show()
                                } else {
                                    deleteMode = !deleteMode
                                }
                            }
                        },
                        enabled = extractingId == null,
                        shape = CircleShape,
                        border = BorderStroke(
                            1.dp,
                            if (deleteMode) AmberUi.Danger else AmberUi.BorderStrong
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (deleteMode) AmberUi.Danger else AmberUi.Text
                        ),
                        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("java_runtime_delete_mode")
                    ) {
                        Text(
                            text = if (deleteMode) "Xong" else "Xóa runtime",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }

                    Button(
                        onClick = {
                            if (extractingId == null) {
                                deleteMode = false
                                runtimeImporter.launch(arrayOf("*/*"))
                            }
                        },
                        enabled = extractingId == null,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberUi.Text,
                            contentColor = AmberUi.ObsidianDeep
                        ),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("java_runtime_add_new")
                    ) {
                        Text(
                            text = "Thêm mới",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        val targetDeleteId = runtimePendingDelete
        if (targetDeleteId != null) {
            NuxDialog(
                onDismissRequest = { runtimePendingDelete = null },
                compact = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Xóa ${JavaRuntimeManager.getInternalRuntimeLabel(targetDeleteId)}?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = AmberUi.Text
                    )
                    Text(
                        text = "Thư mục runtime này sẽ bị xóa khỏi thiết bị. Bạn luôn có thể tải lại hoặc thêm mới bất cứ lúc nào.",
                        fontSize = 13.sp,
                        color = AmberUi.TextDim
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { runtimePendingDelete = null },
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        ) {
                            Text("Hủy", color = AmberUi.TextDim)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val idToDelete = targetDeleteId
                                runtimePendingDelete = null
                                extractingId = idToDelete
                                extractProgress = 0.5f
                                extractStatus = "Đang xóa $idToDelete..."
                                scope.launch {
                                    val delRes = JavaRuntimeManager.deleteRuntime(context, idToDelete)
                                    extractingId = null
                                    if (delRes.isSuccess) {
                                        if (defaultRuntime.equals(idToDelete, ignoreCase = true)) {
                                            defaultRuntime = "auto"
                                            commitSettings()
                                        }
                                        extractStatus = "Đã xóa ${JavaRuntimeManager.getInternalRuntimeLabel(idToDelete)}"
                                        deleteMode = false
                                        refreshTick++
                                    } else {
                                        extractStatus = delRes.exceptionOrNull()?.message ?: "Không thể xóa runtime"
                                    }
                                }
                            },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AmberUi.Danger,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        ) {
                            Text("Xóa runtime", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showAboutDialog) {
        NuxAboutDialog(
            onDismissRequest = { showAboutDialog = false }
        )
    }
}

/** Satu kelompok pengaturan: judul kecil + kartu panjang. Saat mencari, baris tampil sendiri-sendiri. */
@Composable
private fun SettingsGroup(label: String, content: @Composable ColumnScope.() -> Unit) {
    if (LocalSettingsFilter.current != null) {
        Column(content = content)
    } else {
        AmberSectionLabel(label)
        AmberCard(content = content)
    }
}

/** Baris latar: pratinjau kecil latar saat ini, keterangan, dan tombol Ganti latar. */
@Composable
private fun SettingBackgroundRow(onChange: () -> Unit) {
    val context = LocalContext.current
    val thumbState = produceState<ImageBitmap?>(initialValue = null) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val video = HomeVideo.file(context)
                val still = File(context.filesDir, "home_bg")
                val bmp: Bitmap? = if (video.exists() && video.length() > 0) {
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(video.absolutePath)
                        retriever.getFrameAtTime(0)
                    } finally {
                        retriever.release()
                    }
                } else if (still.exists() && still.length() > 0) {
                    BitmapFactory.decodeFile(still.absolutePath, BitmapFactory.Options().apply { inSampleSize = 4 })
                } else null
                bmp?.asImageBitmap()
            }.getOrNull()
        }
    }
    val thumb = thumbState.value
    val shape = RoundedCornerShape(14.dp)
    SettingFrame("Latar", "Foto, GIF, atau video") {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            val compactThumb = maxWidth < 360.dp
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(if (compactThumb) 104.dp else 132.dp)
                        .height(if (compactThumb) 66.dp else 80.dp)
                        .clip(shape)
                        .border(1.dp, AmberUi.BorderStrong, shape)
                ) {
                    if (thumb != null) {
                        Image(
                            bitmap = thumb,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Image(
                            painter = painterResource(id = com.aistudio.novacraft.launcher.R.drawable.mc_hero_bg),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Latar",
                        color = AmberUi.Text,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (compactThumb) 16.sp else 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Foto, GIF, atau video",
                        color = AmberUi.TextDim,
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AmberPillButton(
                        text = "Ganti latar",
                        icon = Icons.Outlined.Image,
                        filled = false,
                        onClick = onChange
                    )
                }
            }
        }
    }
}

private tailrec fun Context.findSettingsActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findSettingsActivity()
    else -> null
}

@Composable
private fun AppPermissionsSettingsSection() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var refreshTick by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshTick++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var askedNotif by remember { mutableStateOf(false) }
    var askedMic by remember { mutableStateOf(false) }
    var askedStorage by remember { mutableStateOf(false) }

    val notifGranted = remember(refreshTick) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
    val micGranted = remember(refreshTick) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
    }
    val isAndroid11Plus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    val legacyStorageGranted = remember(refreshTick) {
        if (isAndroid11Plus) {
            true
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
        }
    }

    fun openSystemAppPermissions() {
        val intent = Intent(
            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching {
            context.startActivity(intent)
        }.onFailure {
            Toast.makeText(context, "Tidak dapat membuka pengaturan izin aplikasi.", Toast.LENGTH_SHORT).show()
        }
    }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        refreshTick++
        Toast.makeText(
            context,
            if (granted) "Izin notifikasi diaktifkan." else "Izin notifikasi ditolak. Anda bisa mengaturnya lewat Pengaturan sistem.",
            Toast.LENGTH_SHORT
        ).show()
    }

    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        refreshTick++
        Toast.makeText(
            context,
            if (granted) "Izin mikrofon diaktifkan." else "Izin mikrofon ditolak. Anda bisa mengaturnya lewat Pengaturan sistem.",
            Toast.LENGTH_SHORT
        ).show()
    }

    val legacyStorageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        refreshTick++
        val allGranted = grants.values.all { it }
        Toast.makeText(
            context,
            if (allGranted) "Izin penyimpanan diaktifkan." else "Izin penyimpanan ditolak. Anda bisa mengaturnya lewat Pengaturan sistem.",
            Toast.LENGTH_SHORT
        ).show()
    }

    SettingActionRow(
        title = "Izin notifikasi",
        desc = "Status unduhan, pemasangan mod/loader, dan kontrol rekam layar.",
        actionLabel = if (notifGranted) "Diizinkan" else "Izinkan",
        filled = !notifGranted,
        onClick = {
            if (notifGranted) {
                Toast.makeText(context, "Izin notifikasi sudah aktif.", Toast.LENGTH_SHORT).show()
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val activity = context.findSettingsActivity()
                val shouldShowRationale = activity != null && ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS
                )
                if (askedNotif && !shouldShowRationale) {
                    openSystemAppPermissions()
                } else {
                    askedNotif = true
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    )
    SettingDivider()
    SettingActionRow(
        title = "Izin mikrofon",
        desc = "Opsional untuk merekam suara mikrofon saat merekam layar game.",
        actionLabel = if (micGranted) "Diizinkan" else "Izinkan",
        filled = !micGranted,
        onClick = {
            if (micGranted) {
                Toast.makeText(context, "Izin mikrofon sudah aktif.", Toast.LENGTH_SHORT).show()
            } else {
                val activity = context.findSettingsActivity()
                val shouldShowRationale = activity != null && ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.RECORD_AUDIO
                )
                if (askedMic && !shouldShowRationale) {
                    openSystemAppPermissions()
                } else {
                    askedMic = true
                    micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        }
    )
    SettingDivider()
    if (isAndroid11Plus) {
        SettingActionRow(
            title = "Izin penyimpanan (Android 11+)",
            desc = "Dibatasi aturan Android 11+: memakai folder privat novacraft dan pemilih SAF tanpa melewati pembatasan sistem.",
            actionLabel = "Dibatasi",
            filled = false,
            onClick = {
                Toast.makeText(
                    context,
                    "Pembatasan penyimpanan Android 11+ aktif dan tidak dilewati.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    } else {
        SettingActionRow(
            title = "Izin penyimpanan",
            desc = "Akses baca/tulis berkas lokal untuk perangkat Android 10 ke bawah.",
            actionLabel = if (legacyStorageGranted) "Diizinkan" else "Izinkan",
            filled = !legacyStorageGranted,
            onClick = {
                if (legacyStorageGranted) {
                    Toast.makeText(context, "Izin penyimpanan sudah aktif.", Toast.LENGTH_SHORT).show()
                } else {
                    val activity = context.findSettingsActivity()
                    val shouldShowRationale = activity != null && ActivityCompat.shouldShowRequestPermissionRationale(
                        activity,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                    if (askedStorage && !shouldShowRationale) {
                        openSystemAppPermissions()
                    } else {
                        askedStorage = true
                        legacyStorageLauncher.launch(
                            arrayOf(
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                            )
                        )
                    }
                }
            }
        )
    }
    SettingDivider()
    SettingActionRow(
        title = "Pengaturan izin sistem",
        desc = "Buka halaman izin aplikasi Android untuk mengelola akses secara manual.",
        actionLabel = "Buka",
        filled = false,
        onClick = { openSystemAppPermissions() }
    )
}
