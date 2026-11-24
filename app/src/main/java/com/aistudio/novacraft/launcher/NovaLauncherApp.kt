package com.aistudio.novacraft.launcher

import com.aistudio.novacraft.launcher.core.utils.VulkanCheck
import com.aistudio.novacraft.launcher.core.utils.VulkanReport
import com.aistudio.novacraft.launcher.core.utils.VulkanState
import com.aistudio.novacraft.launcher.core.launch.UpdateInstaller
import com.aistudio.novacraft.launcher.core.launch.UpdateManager
import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.content.pm.Signature
import android.graphics.SurfaceTexture
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.os.Process
import android.os.SystemClock
import android.provider.Settings
import android.system.Os
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.novacraft.launcher.core.account.AccountManager
import com.aistudio.novacraft.launcher.core.controls.ControlLayoutManager
import com.aistudio.novacraft.launcher.core.controls.CustomControlButton
import com.aistudio.novacraft.launcher.core.controls.DexClassScanner
import com.aistudio.novacraft.launcher.core.controls.GamepadRouter
import com.aistudio.novacraft.launcher.core.controls.GuiScaleReader
import com.aistudio.novacraft.launcher.core.controls.HotbarGeometry
import com.aistudio.novacraft.launcher.core.controls.TouchControllerBridge
import com.aistudio.novacraft.launcher.core.controls.TouchControllerSupport
import com.aistudio.novacraft.launcher.core.controls.TouchSink
import com.aistudio.novacraft.launcher.core.utils.CrashManager
import com.aistudio.novacraft.launcher.core.utils.GameCrashInfo
import com.aistudio.novacraft.launcher.core.utils.GameLogArchive
import com.aistudio.novacraft.launcher.core.utils.LauncherLog
import com.aistudio.novacraft.launcher.core.game.input.EfficientAndroidLWJGLKeycode
import com.aistudio.novacraft.launcher.core.game.input.HidableInputLayout
import com.aistudio.novacraft.launcher.core.game.input.TouchCharInput
import com.aistudio.novacraft.launcher.core.game.input.TypingMirror
import com.aistudio.novacraft.launcher.core.game.input.TypingPreviewBar
import com.aistudio.novacraft.launcher.core.launch.InstanceManager
import com.aistudio.novacraft.launcher.core.launch.JavaRuntimeManager
import com.aistudio.novacraft.launcher.core.mods.NovaUnifiedFileImportEngine
import com.aistudio.novacraft.launcher.core.mods.PendingAddonImport
import com.aistudio.novacraft.launcher.core.launch.ElfDeps
import com.aistudio.novacraft.launcher.core.launch.GlErrorPolicy
import com.aistudio.novacraft.launcher.core.launch.MobileGluesConfig
import com.aistudio.novacraft.launcher.core.launch.NuxRendererRegistry
import com.aistudio.novacraft.launcher.core.launch.ScreenRecorder
import com.aistudio.novacraft.launcher.core.utils.DeviceProfiles
import com.aistudio.novacraft.launcher.core.utils.GamePerfMonitor
import com.aistudio.novacraft.launcher.core.utils.MCOptions
import com.aistudio.novacraft.launcher.core.utils.NuxVersionUtils
import com.aistudio.novacraft.launcher.core.utils.PhysicalMouseChecker
import com.aistudio.novacraft.launcher.core.utils.SettingsManager
import com.aistudio.novacraft.launcher.ui.components.AmberCrashPanel
import com.aistudio.novacraft.launcher.ui.components.GameLoadingOverlay
import com.aistudio.novacraft.launcher.ui.components.GamepadFocusProvider
import com.aistudio.novacraft.launcher.ui.components.NuxBadge
import com.aistudio.novacraft.launcher.ui.components.NuxButton
import com.aistudio.novacraft.launcher.ui.components.NuxCard
import com.aistudio.novacraft.launcher.ui.components.GameControlsLayer
import com.aistudio.novacraft.launcher.ui.components.LauncherMouse
import com.aistudio.novacraft.launcher.ui.components.MenuMouse
import com.aistudio.novacraft.launcher.ui.components.MouseControlMode
import com.aistudio.novacraft.launcher.ui.components.MouseDiag
import com.aistudio.novacraft.launcher.ui.components.PointerStyle
import com.aistudio.novacraft.launcher.ui.components.SwitchableMouseLayout
import com.aistudio.novacraft.launcher.ui.components.drawPointer
import com.aistudio.novacraft.launcher.ui.dialogs.ExitConfirmDialog
import com.aistudio.novacraft.launcher.ui.dialogs.GameConsolePanel
import com.aistudio.novacraft.launcher.ui.dialogs.GameMenuSheet
import com.aistudio.novacraft.launcher.ui.screens.CustomGuiEditorScreen
import com.aistudio.novacraft.launcher.ui.screens.DashboardScreen
import com.aistudio.novacraft.launcher.ui.theme.Amber
import com.aistudio.novacraft.launcher.ui.theme.AmberLanguageHost
import com.aistudio.novacraft.launcher.ui.theme.NuxColors
import com.aistudio.novacraft.launcher.ui.theme.NuxReadable
import com.aistudio.novacraft.launcher.ui.theme.NuxResponsiveTheme
import com.aistudio.novacraft.launcher.ui.theme.NuxSizes
import com.aistudio.novacraft.launcher.ui.theme.Text
import com.aistudio.novacraft.launcher.ui.theme.Toast
import com.aistudio.novacraft.launcher.ui.theme.Tr
import com.movtery.inputmap.keycodes.LwjglGlfwKeycode
import com.movtery.zalithlauncher.bridge.CURSOR_DISABLED
import com.movtery.zalithlauncher.bridge.CURSOR_ENABLED
import com.movtery.zalithlauncher.bridge.LoggerBridge
import com.movtery.zalithlauncher.bridge.ZLBridge
import com.movtery.zalithlauncher.bridge.ZLBridgeStates
import com.movtery.zalithlauncher.bridge.ZLNativeInvoker
import com.movtery.zalithlauncher.game.sdl.SdlBridge
import com.movtery.zalithlauncher.utils.device.VulkanChecker
import com.oracle.dalvik.VMLauncher
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.Writer
import java.security.MessageDigest
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.libsdl.app.SDLActivity
import org.libsdl.app.SDLSurface
import org.lwjgl.glfw.CallbackBridge

// ----------------------------------------------------------------------------
// Section: NuxApplication.kt
// ----------------------------------------------------------------------------

class NuxApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Buku harian kejadian launcher (tiap proses): masalah di luar game tercatat di sini.
        try {
            LauncherLog.init(this)
            registerLifecycleLogging()
        } catch (_: Throwable) {
        }

        // Bahasa tampilan (Indonesia / English). Dipanggil di tiap proses, termasuk proses game. Proses mesin Rust
        // (:horizon) tidak menampilkan layar launcher, jadi kamusnya tidak perlu dimuat di sana.
        if (!isEngineProcess()) {
            try {
                Tr.init(this)
            } catch (_: Throwable) {
            }
        }

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                LauncherLog.crash(thread, throwable)
            } catch (_: Throwable) {
            }
            try {
                Log.e("NuxApplication", "Uncaught exception on thread ${thread.name}", throwable)
                val now = System.currentTimeMillis()
                val marker = java.io.File(cacheDir, ".last_crash_ms")
                val lastCrashMs = try {
                    if (marker.isFile) marker.readText().trim().toLongOrNull() ?: 0L else 0L
                } catch (_: Throwable) {
                    0L
                }
                val isErrorActivityCrash = try {
                    android.util.Log.getStackTraceString(throwable).contains("ErrorActivity")
                } catch (_: Throwable) {
                    false
                }
                val isLooping = (now - lastCrashMs) in 0L..4_000L || isErrorActivityCrash

                try {
                    marker.writeText(now.toString())
                } catch (_: Throwable) {
                }

                // Catat ke sistem crash manager
                CrashManager.recordLauncherCrash(this, throwable)
                if (!isLooping) {
                    // Tampilkan ErrorActivity mandiri (hanya jika tidak sedang loop crash)
                    ErrorActivity.showLauncherCrash(this, throwable)
                } else {
                    Log.e("NuxApplication", "Suppressing ErrorActivity launch due to rapid crash loop")
                    defaultHandler?.uncaughtException(thread, throwable)
                }
            } catch (e: Throwable) {
                Log.e("NuxApplication", "Failed to dispatch crash activity", e)
                try {
                    defaultHandler?.uncaughtException(thread, throwable)
                } catch (_: Throwable) {
                }
            }

            // Hentikan proses yang mengalami crash agar tidak memicu ANR / loop
            android.os.Process.killProcess(android.os.Process.myPid())
            System.exit(10)
        }
    }

    /** True di proses :horizon (mesin Rust). Mesin itu butuh Android 9 ke atas, jadi di bawahnya selalu false. */
    private fun isEngineProcess(): Boolean = try {
        android.os.Build.VERSION.SDK_INT >= 28 && Application.getProcessName()?.endsWith(":horizon") == true
    } catch (_: Throwable) {
        false
    }

    /** Mencatat layar yang dibuka dan ditutup (tanpa logcat; hanya ke buku harian launcher). */
    private fun registerLifecycleLogging() {
        registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                LauncherLog.event(
                    "Layar",
                    activity.javaClass.simpleName + " dibuka" + (if (savedInstanceState != null) " (dipulihkan)" else "")
                )
            }

            override fun onActivityDestroyed(activity: Activity) {
                LauncherLog.event(
                    "Layar",
                    activity.javaClass.simpleName + " ditutup" + (if (activity.isFinishing) "" else " (dibuat ulang oleh sistem)")
                )
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        })
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // 10 = RAM mulai sempit, 15 = kritis, 40 ke atas = proses ini berada di latar dan bisa dimatikan Android.
        if (level == 10 || level == 15 || level >= 40) {
            LauncherLog.event("Memori", "Android meminta aplikasi melepas memori (tingkat $level)")
        }
    }
}

// ----------------------------------------------------------------------------
// Section: MainActivity.kt
// ----------------------------------------------------------------------------

class MainActivity : ComponentActivity() {

    // Pekerjaan berat (baca file, pindai aplikasi) dijalankan di thread latar satu per satu,
    // supaya layar tidak membeku saat dibuka atau saat kembali dari pemilih foto.
    private val bgExecutor: java.util.concurrent.ExecutorService =
        java.util.concurrent.Executors.newSingleThreadExecutor()
    private var storageWasGranted = false
    private var navUninstall: (() -> Unit)? = null

    private fun storageAccessGranted(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ menggunakan penyimpanan privat aplikasi (novacraft) + SAF tanpa melewati pembatasan sistem.
            true
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        hideSystemBars()
        // Controller: A pilih, B kembali, stick/D-pad pindah sorotan, LB/RB ganti tab.
        navUninstall = com.aistudio.novacraft.launcher.core.controls.GamepadNav.install(window, allowBumper = true)
        checkAndRequestStoragePermissions()
        storageWasGranted = storageAccessGranted()

        handleIncomingAddonIntent(intent)

        // Initialize core engines (Settings terlebih dahulu agar persistensi sesi akun terbaca)
        SettingsManager.init(this)
        com.aistudio.novacraft.launcher.core.utils.DiscordPresenceManager.initLauncherStartup(applicationContext)
        AccountManager.init(this)
        InstanceManager.init(this)
        // Terapkan mode tema, warna aksen, sumber warna, animasi, dan gaya tampilan tersimpan sebelum layar pertama digambar
        SettingsManager.settings.value.let {
            com.aistudio.novacraft.launcher.ui.theme.AmberUi.setThemeMode(it.themeMode)
            com.aistudio.novacraft.launcher.ui.theme.AmberUi.setColorSource(it.colorSource, this)
            com.aistudio.novacraft.launcher.ui.theme.AmberUi.setAccent(it.accentColor)
            com.aistudio.novacraft.launcher.ui.theme.AmberUi.setStyle(it.uiStyle)
            com.aistudio.novacraft.launcher.ui.theme.AmberUi.applyDim(it.bgDim / 100f)
            com.aistudio.novacraft.launcher.ui.theme.AmberUi.setAnimations(
                it.animScreenTransitions,
                it.animDialogsPopups,
                it.animButtonEffects,
                it.animListsContent
            )
            if (it.startupSoundEnabled && savedInstanceState == null) {
                playStartupChimeAsync()
            }
        }
        com.aistudio.novacraft.launcher.core.utils.PhysicalMouseChecker.initChecker(this)
        com.aistudio.novacraft.launcher.core.controls.ControlLayoutManager.init(this)
        val appCtx = applicationContext
        bgExecutor.execute {
            try {
                com.aistudio.novacraft.launcher.core.mods.NovaLauncherDatabase.getInstance(appCtx)
                com.aistudio.novacraft.launcher.core.launch.MojangManifestService.prewarmFromDiskCache(appCtx)
            } catch (_: Throwable) {}
            try {
                com.aistudio.novacraft.launcher.core.launch.NuxRendererPluginManager.scanPlugins(appCtx)
            } catch (_: Throwable) {}
            try {
                com.aistudio.novacraft.launcher.core.utils.CrashManager.checkAndNotify(appCtx)
            } catch (_: Throwable) {}
        }

        setContent {
            val sysDark = isSystemInDarkTheme()
            SideEffect {
                com.aistudio.novacraft.launcher.ui.theme.AmberUi.updateSystemDark(sysDark)
            }
            val colorScheme = if (com.aistudio.novacraft.launcher.ui.theme.AmberUi.isDark) {
                darkColorScheme(
                    primary = com.aistudio.novacraft.launcher.ui.theme.AmberUi.Accent,
                    onPrimary = com.aistudio.novacraft.launcher.ui.theme.AmberUi.OnAccent,
                    background = NuxColors.Background,
                    onBackground = NuxColors.DarkGray,
                    surface = NuxColors.Panel,
                    onSurface = NuxColors.DarkGray
                )
            } else {
                lightColorScheme(
                    primary = com.aistudio.novacraft.launcher.ui.theme.AmberUi.Accent,
                    onPrimary = com.aistudio.novacraft.launcher.ui.theme.AmberUi.OnAccent,
                    background = NuxColors.Background,
                    onBackground = NuxColors.DarkGray,
                    surface = NuxColors.Panel,
                    onSurface = NuxColors.DarkGray
                )
            }
            AmberLanguageHost {
                NuxResponsiveTheme {
                    MaterialTheme(colorScheme = colorScheme) {
                        com.aistudio.novacraft.launcher.ui.components.GamepadFocusProvider {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = NuxColors.Background
                        ) {
                            DashboardScreen()
                        }
                        }
                    }
                }
            }
        }
    }

    private fun playStartupChimeAsync() {
        bgExecutor.execute {
            try {
                val am = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                if (am != null && am.ringerMode != AudioManager.RINGER_MODE_NORMAL) return@execute

                val sampleRate = 22050
                val durationMs = 280
                val totalSamples = sampleRate * durationMs / 1000
                val pcm = ShortArray(totalSamples)
                val freqs = doubleArrayOf(523.25, 659.25, 783.99) // C5 -> E5 -> G5
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    val env = kotlin.math.sin(Math.PI * i / totalSamples).coerceAtLeast(0.0)
                    val noteIdx = ((i * freqs.size) / totalSamples).coerceIn(0, freqs.lastIndex)
                    val sample = kotlin.math.sin(2.0 * Math.PI * freqs[noteIdx] * t) * 0.14 * env
                    pcm[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(pcm.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(pcm, 0, pcm.size)
                track.play()
                Thread.sleep(durationMs.toLong() + 40L)
                track.stop()
                track.release()
            } catch (_: Throwable) {
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        com.aistudio.novacraft.launcher.core.utils.DiscordPresenceManager.onReturnedToLauncher(applicationContext)
        val appCtx = applicationContext
        // Muat ulang instance hanya bila izin penyimpanan baru saja berubah (bukan tiap kali
        // kembali ke aplikasi, mis. setelah memilih foto) dan tidak di thread utama.
        val granted = storageAccessGranted()
        if (granted != storageWasGranted) {
            storageWasGranted = granted
            bgExecutor.execute {
                try { InstanceManager.init(appCtx) } catch (_: Throwable) {}
                // Izin baru diberikan: coba pulihkan layout kontrol dari cadangan (jika ada)
                try { com.aistudio.novacraft.launcher.core.controls.ControlLayoutManager.init(appCtx) } catch (_: Throwable) {}
            }
        }
        // Cek apakah game sebelumnya crash (membaca file log, jadi di thread latar)
        bgExecutor.execute {
            try {
                com.aistudio.novacraft.launcher.core.utils.CrashManager.checkAndNotify(appCtx)
            } catch (_: Throwable) {}
        }
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    override fun onDestroy() {
        if (isFinishing) {
            com.aistudio.novacraft.launcher.core.utils.DiscordPresenceManager.clearPresence(applicationContext)
        }
        navUninstall?.invoke()
        navUninstall = null
        super.onDestroy()
        bgExecutor.shutdown()
        com.aistudio.novacraft.launcher.core.account.OfflineSkinServerManager.stopServer()
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val params = window.attributes
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
            window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
            window.attributes = params
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
    }

    private fun checkAndRequestStoragePermissions() {
        // Hanya Android 10 ke bawah (< Android 11 / R). Android 11+ memakai penyimpanan privat (novacraft) + SAF.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            val permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            val needed = permissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            if (needed.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, needed.toTypedArray(), 1001)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingAddonIntent(intent)
    }

    private fun handleIncomingAddonIntent(intent: Intent?) {
        if (intent == null) return
        val uri = intent.data ?: (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        }) ?: return

        // Intent ini bisa dikirim aplikasi apa pun: tolak alamat yang menunjuk ke data pribadi launcher
        if (!NovaUnifiedFileImportEngine.isAcceptableSource(this, uri)) return

        val aliasClass = intent.component?.className
        val fileName = NovaUnifiedFileImportEngine.getFileName(this, uri)
        val resolvedType = NovaUnifiedFileImportEngine.resolveTypeFromAliasOrFile(this, uri, aliasClass)

        NovaUnifiedFileImportEngine.setPendingImport(
            PendingAddonImport(
                uri = uri,
                fileName = fileName,
                suggestedType = resolvedType
            )
        )
    }
}

// ----------------------------------------------------------------------------
// Section: GameActivity.kt
// ----------------------------------------------------------------------------

class GameActivity : ComponentActivity(), SurfaceHolder.Callback {

    companion object {
        const val EXTRA_INSTANCE_NAME = "extra_instance_name"
        const val EXTRA_MC_VERSION = "extra_mc_version"
        const val EXTRA_USERNAME = "extra_username"
        const val EXTRA_UUID = "extra_uuid"
        const val EXTRA_GAME_DIR = "extra_game_dir"
        const val EXTRA_ASSETS_DIR = "extra_assets_dir"
        const val EXTRA_ASSET_INDEX = "extra_asset_index"
        const val EXTRA_MAIN_CLASS = "extra_main_class"
        const val EXTRA_CLASSPATH = "extra_classpath"
        const val EXTRA_RUNTIME_NAME = "extra_runtime_name"
        const val EXTRA_RUNTIME_HOME = "extra_runtime_home"
        const val EXTRA_LWJGL_NATIVES_DIR = "extra_lwjgl_natives_dir"
        const val EXTRA_ACCESS_TOKEN = "extra_access_token"
        const val EXTRA_USER_TYPE = "extra_user_type"
        const val EXTRA_AUTHLIB_INJECTOR_PATH = "extra_authlib_injector_path"
        const val EXTRA_AUTHLIB_URL = "extra_authlib_url"
        const val EXTRA_USE_WRAPPER = "extra_use_wrapper"
        const val EXTRA_LOADER = "extra_loader"
        const val EXTRA_LOADER_VERSION = "extra_loader_version"
        const val EXTRA_INSTALLED_MODS = "extra_installed_mods"
        const val EXTRA_EXTRA_JVM_ARGS = "extra_extra_jvm_args"
        const val EXTRA_EXTRA_GAME_ARGS = "extra_extra_game_args"
        const val EXTRA_DISCORD_RPC_PLAY = "extra_discord_rpc_play"
        const val EXTRA_DISCORD_RPC_SERVER = "extra_discord_rpc_server"

        // Setelah sesi ketik layar berakhir, tombol sisa dari keyboard layar yang datang dalam rentang ini
        // tidak dianggap keyboard fisik (tidak menyembunyikan kontrol).
        private const val SOFT_KEY_GRACE_MS = 1500L
    }

    private val liveLogs = mutableStateListOf<String>()
    private var isGameStarted = false
    private var surfaceHolderRef: SurfaceHolder? = null

    // Minecraft 26.x: permukaan game berupa TextureView (lihat shouldUseTextureView)
    private var useTextureView = false
    private var gameTextureView: TextureView? = null
    // Tampilan Compose penuh layar. Dipakai MenuMouse untuk mengubah koordinat jendela ke koordinat game.
    private var gameComposeView: View? = null
    private var gameTextureSurface: android.view.Surface? = null
    private val isControlVisibleState = mutableStateOf(true)
    private val isGameRenderingState = mutableStateOf(false)

    // Mod TouchController: terisi hanya bila modnya terpasang, pengaturannya aktif, dan klien proxy
    // berhasil jalan. Selama null, kontrol bawaan launcher dipakai seperti biasa.
    private val touchSinkState = mutableStateOf<TouchSink?>(null)
    @Volatile private var touchBridge: TouchControllerBridge? = null

    private var isManualExit = false
    private val exitHandled = AtomicBoolean(false)
    private var sessionStartTime = System.currentTimeMillis()
    private var gameLogArchived = false
    private var discordRpcPlay = false
    private var discordRpcShowServer = false

    // ---- Rekam layar bawaan -------------------------------------------------------------
    // Alurnya: (izin mikrofon/notifikasi bila perlu) -> dialog izin tangkap layar sistem ->
    // RecordService (foreground) -> ScreenRecorder (encoder perangkat keras). Peluncur hasil
    // harus didaftarkan sebelum Activity mulai, jadi dibuat sebagai properti.
    private var askedRecordPermissions = false

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (result.resultCode == android.app.Activity.RESULT_OK && data != null) {
            val (w, h, dpi) = screenForRecording()
            RecordService.start(this, result.resultCode, data, w, h, dpi)
        } else {
            com.aistudio.novacraft.launcher.ui.theme.Toast.makeText(
                this, "Izin merekam layar tidak diberikan", com.aistudio.novacraft.launcher.ui.theme.Toast.LENGTH_SHORT
            ).show()
        }
    }

    private val recordPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants[Manifest.permission.RECORD_AUDIO] == false) {
            com.aistudio.novacraft.launcher.ui.theme.Toast.makeText(
                this, "Izin mikrofon ditolak, merekam tanpa suara", com.aistudio.novacraft.launcher.ui.theme.Toast.LENGTH_SHORT
            ).show()
        }
        launchProjectionPrompt()
    }

    /** Ketuk tombol Rekam di menu: berhenti bila sedang merekam, selain itu mulai alur izin. */
    private fun toggleRecording() {
        if (ScreenRecorder.isRecording) {
            ScreenRecorder.stop()
            return
        }
        val needed = ArrayList<String>()
        if (SettingsManager.settings.value.recordMic &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
        ) {
            needed.add(Manifest.permission.RECORD_AUDIO)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            needed.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        // Minta izin tambahan paling banyak sekali per sesi game supaya tidak mengganggu.
        if (needed.isNotEmpty() && !askedRecordPermissions) {
            askedRecordPermissions = true
            recordPermissionLauncher.launch(needed.toTypedArray())
        } else {
            launchProjectionPrompt()
        }
    }

    private fun launchProjectionPrompt() {
        try {
            val mpm = getSystemService(android.content.Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projectionLauncher.launch(mpm.createScreenCaptureIntent())
        } catch (e: Throwable) {
            com.aistudio.novacraft.launcher.ui.theme.Toast.makeText(
                this, "Perekam layar tidak tersedia di HP ini", com.aistudio.novacraft.launcher.ui.theme.Toast.LENGTH_SHORT
            ).show()
        }
    }

    /** Lebar, tinggi (landscape), dan densitas layar asli untuk menentukan ukuran rekaman. */
    private fun screenForRecording(): Triple<Int, Int, Int> {
        val (rawW, rawH) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            Pair(bounds.width(), bounds.height())
        } else {
            val dm = android.util.DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(dm)
            Pair(dm.widthPixels, dm.heightPixels)
        }
        return Triple(maxOf(rawW, rawH), minOf(rawW, rawH), resources.displayMetrics.densityDpi)
    }

    // Keluar dari layar game (Home, aplikasi lain): jeda rekaman supaya layar lain tidak ikut
    // terekam dan encoder tidak bekerja sia-sia. Kembali ke game: lanjut.
    override fun onStart() {
        super.onStart()
        ScreenRecorder.release(ScreenRecorder.Hold.SCREEN)
    }

    override fun onStop() {
        MenuMouse.reset()
        MouseDiag.summary("berhenti")
        MouseDiag.devices()
        ScreenRecorder.hold(ScreenRecorder.Hold.SCREEN)
        // Jari yang masih tercatat di mod tidak akan pernah menerima "angkat" bila aplikasi keluar layar.
        touchBridge?.onCancelAll()
        GamepadRouter.reset()
        super.onStop()
    }

    private fun handleGameExit(exitCode: Int, isSignal: Boolean, errorDetail: String? = null) {
        if (!exitHandled.compareAndSet(false, true)) return
        // Pindahkan baris log yang masih mengantre supaya laporan crash memuat baris terakhirnya
        flushPendingLogs()

        // Cek apakah game keluar bersih secara normal atas instruksi user (klik quit game / pause exit)
        val isNormalExit = isManualExit || (exitCode == 0 && !isSignal)

        if (!isNormalExit) {
            // Ini adalah CRASH (JVM Exit code != 0, fatal signal, OOM, atau exception)
            val crashReportsDir = File(gameDirPath, "crash-reports")
            val latestCrashReport = if (crashReportsDir.exists() && crashReportsDir.isDirectory) {
                crashReportsDir.listFiles { f -> f.isFile && f.name.startsWith("crash-") && f.name.endsWith(".txt") }
                    ?.maxByOrNull { it.lastModified() }
            } else null

            val hasRecentCrashReport = latestCrashReport != null && (latestCrashReport.lastModified() >= sessionStartTime - 10000L)
            val logFile = File(filesDir, "latestlog.txt")
            val effectiveLogPath = if (hasRecentCrashReport && latestCrashReport != null) latestCrashReport.absolutePath else if (logFile.exists()) logFile.absolutePath else ""

            val finalExitCode = if (exitCode != 0) exitCode else 1

            // 1. Rekam di CrashManager agar MainActivity juga tahu jika dibuka kembali
            CrashManager.recordCrash(
                context = this,
                instanceName = instanceName,
                mcVersion = mcVersion,
                exitCode = finalExitCode,
                isSignal = isSignal,
                gameDirPath = gameDirPath,
                liveLogs = liveLogs.toList(),
                exceptionDetail = errorDetail,
                loader = loader,
                loaderVersion = loaderVersion,
                installedMods = installedMods,
                graphicsInfo = graphicsSummary
            )

            // 2. Langsung luncurkan ErrorActivity mandiri (persis seperti Zalith Launcher)
            try {
                ErrorActivity.showGameCrash(
                    context = this,
                    exitCode = finalExitCode,
                    isSignal = isSignal,
                    logPath = effectiveLogPath,
                    instanceName = instanceName,
                    mcVersion = mcVersion
                )
            } catch (e: Throwable) {
                try {
                    val launcherIntent = Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    startActivity(launcherIntent)
                } catch (_: Throwable) {}
            }
        } else {
            // Normal Exit
            CrashManager.onGameSessionEnded(this)
            try {
                val launcherIntent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                startActivity(launcherIntent)
            } catch (_: Throwable) {}
        }

        terminateGameProcess()
    }

    /** Menyimpan log sesi ini ke folder logs (sekali saja per sesi); dipanggil sebelum proses game dimatikan. */
    private fun archiveGameLogOnce() {
        if (gameLogArchived) return
        gameLogArchived = true
        try { GameLogArchive.archiveSession(this) } catch (_: Throwable) {}
    }

    private fun terminateGameProcess() {
        archiveGameLogOnce()
        try { com.aistudio.novacraft.launcher.core.utils.DiscordPresenceManager.clearPresence(applicationContext) } catch (_: Throwable) {}
        // Selesaikan berkas rekaman dulu; proses ini dimatikan paksa di bawah.
        try { ScreenRecorder.stopBlocking() } catch (_: Throwable) {}
        try {
            isManualExit = true
            CrashManager.onGameSessionEnded(this)
            ZLBridge.releaseBridgeWindow()
            com.aistudio.novacraft.launcher.core.account.OfflineSkinServerManager.stopServer()
            com.aistudio.novacraft.launcher.core.account.OfflineYggdrasilServer.activeInstance?.stop()
        } catch (_: Throwable) {}
        finish()
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    override fun onDestroy() {
        super.onDestroy()
        MouseDiag.sink = null
        logHandler.removeCallbacks(logFlushRunnable)
        try { LoggerBridge.setListener(null) } catch (_: Throwable) {}
        try { CallbackBridge.setGraphicOutputListener(null) } catch (_: Throwable) {}
        try { ZLNativeInvoker.onExitCallback = null } catch (_: Throwable) {}
        try { com.aistudio.novacraft.launcher.core.utils.DiscordPresenceManager.clearPresence(applicationContext) } catch (_: Throwable) {}
        archiveGameLogOnce()
        // Selesaikan berkas rekaman dulu; proses ini dimatikan paksa di bawah.
        try { ScreenRecorder.stopBlocking() } catch (_: Throwable) {}
        try {
            isManualExit = true
            CrashManager.onGameSessionEnded(this)
            ZLBridge.releaseBridgeWindow()
            com.aistudio.novacraft.launcher.core.account.OfflineSkinServerManager.stopServer()
            com.aistudio.novacraft.launcher.core.account.OfflineYggdrasilServer.activeInstance?.stop()
        } catch (_: Throwable) {}
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    /**
     * Android memberi tahu bila RAM HP hampir habis (tingkat 10 = rendah, 15 = kritis). Hanya dicatat
     * di log: kalau proses game lalu ditutup sistem, penyebabnya terbaca dari log, bukan ditebak.
     */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= 10 && level < 20) {
            try {
                LoggerBridge.append("▷ [Kinerja] RAM HP hampir habis (tingkat $level dari 15): Android meminta aplikasi melepas memori")
            } catch (_: Throwable) {}
        }
    }

    private var instanceName = "Minecraft"
    private var mcVersion = "Unknown"
    private var username = "Player"
    private var uuid = "00000000-0000-0000-0000-000000000000"
    private var gameDirPath = ""

    // Hotbar sentuh: GUI scale Minecraft dibaca dari options.txt, hanya saat jari menyentuh hotbar.
    private var guiScaleReader: GuiScaleReader? = null

    private fun currentGuiScale(): Int {
        var reader = guiScaleReader
        if (reader == null) {
            if (gameDirPath.isEmpty()) return 0
            reader = GuiScaleReader(File(gameDirPath, "options.txt"))
            guiScaleReader = reader
        }
        return reader.get(SystemClock.uptimeMillis())
    }
    private var assetsDirPath = ""
    private var assetIndexId = ""
    private var mainClass = "net.minecraft.client.main.Main"
    private var classpath = ""
    private var runtimeName = "jre-17"
    private var runtimeHomePath = ""
    private var lwjglNativesDirPath = ""
    private var accessToken = "0"
    private var userType = "mojang"
    private var authlibInjectorPath: String? = null
    private var authlibUrl: String? = null
    private var useWrapper = false
    private var loader = "vanilla"
    private var loaderVersion: String? = null
    /** Ringkasan renderer + backend grafis sesi ini; ikut ke laporan crash supaya penyebab "layar miring/hitam" mudah dilacak. */
    private var graphicsSummary: String? = null
    private var installedMods: List<String> = emptyList()

    // Argumen tambahan dari mod loader (Forge): sebelum -cp dan sesudah argumen game biasa
    private var extraJvmArgs: List<String> = emptyList()
    private var extraGameArgs: List<String> = emptyList()

    private fun getScaledDisplayDimensions(): Pair<Int, Int> {
        val settings = SettingsManager.settings.value
        val ratio = (settings.resolutionRatio / 100f).coerceIn(0.35f, 1.5f)
        val (rawW, rawH) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            Pair(bounds.width(), bounds.height())
        } else {
            val dm = android.util.DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(dm)
            Pair(dm.widthPixels, dm.heightPixels)
        }
        // GameActivity adalah sensorLandscape, pastikan lebar horizontal selalu lebih besar dari tinggi vertikal
        val screenW = maxOf(rawW, rawH)
        val screenH = minOf(rawW, rawH)
        val width = (screenW * ratio).roundToInt()
        val height = (screenH * ratio).roundToInt()
        return Pair(width, height)
    }

    private fun applyDynamicResolution(newRatio: Int) {
        val current = SettingsManager.settings.value
        val updated = current.copy(resolutionRatio = newRatio)
        SettingsManager.updateSettings(this, updated)

        val (targetW, targetH) = getScaledDisplayDimensions()
        surfaceHolderRef?.setFixedSize(targetW, targetH)
        gameTextureView?.surfaceTexture?.setDefaultBufferSize(targetW, targetH)
        CallbackBridge.sendUpdateWindowSize(targetW, targetH)
        LoggerBridge.append("▷ [In-Game Resolution] Dinamis diperbarui ke $newRatio% (${targetW}x${targetH})")
    }

    private fun applyImmersiveFullscreen() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val params = window.attributes
                params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                window.attributes = params
            }
            WindowCompat.setDecorFitsSystemWindows(window, false)

            val decor = window.peekDecorView() ?: try { window.decorView } catch (_: Throwable) { null }
            if (decor != null) {
                val insetsController = WindowCompat.getInsetsController(window, decor)
                insetsController.hide(
                    WindowInsetsCompat.Type.systemBars() or
                    WindowInsetsCompat.Type.displayCutout()
                )
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

                @Suppress("DEPRECATION")
                decor.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                )
            }
        } catch (_: Throwable) {}
    }

    override fun onPointerCaptureChanged(hasCapture: Boolean) {
        super.onPointerCaptureChanged(hasCapture)
        // Keadaan sebenarnya dari Android: menentukan apakah kursor launcher atau penunjuk sistem yang dipakai di menu.
        LauncherMouse.captureHeld.value = hasCapture
        MouseDiag.captureChanged(hasCapture)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            applyImmersiveFullscreen()
        } else {
            GamepadRouter.reset()
            MenuMouse.reset()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PhysicalMouseChecker.initChecker(this)
        // Activity baru belum memegang pointer capture, apa pun keadaan activity sebelumnya.
        LauncherMouse.captureHeld.value = false
        // Catatan jalur masuk mouse fisik (satu baris per jalur per sesi) ke log game.
        MouseDiag.sink = { line -> appendUiLog(line) }
        MouseDiag.devices()
        SettingsManager.init(this)
        ControlLayoutManager.init(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && SettingsManager.settings.value.sustainedPerformanceMode) {
            runCatching {
                val pm = getSystemService(android.os.PowerManager::class.java)
                if (pm?.isSustainedPerformanceModeSupported != false) {
                    window.setSustainedPerformanceMode(true)
                }
            }
        }
        CallbackBridge.sContext = this

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val params = window.attributes
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
            window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
            window.attributes = params
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)

        instanceName = intent.getStringExtra(EXTRA_INSTANCE_NAME) ?: "Minecraft"
        mcVersion = intent.getStringExtra(EXTRA_MC_VERSION) ?: "Unknown"
        username = intent.getStringExtra(EXTRA_USERNAME) ?: "Player"
        uuid = intent.getStringExtra(EXTRA_UUID) ?: "00000000-0000-0000-0000-000000000000"
        gameDirPath = intent.getStringExtra(EXTRA_GAME_DIR) ?: filesDir.absolutePath
        assetsDirPath = intent.getStringExtra(EXTRA_ASSETS_DIR) ?: File(filesDir, "minecraft/assets").absolutePath
        assetIndexId = intent.getStringExtra(EXTRA_ASSET_INDEX) ?: mcVersion
        mainClass = intent.getStringExtra(EXTRA_MAIN_CLASS) ?: "net.minecraft.client.main.Main"
        classpath = intent.getStringExtra(EXTRA_CLASSPATH) ?: ""
        runtimeName = intent.getStringExtra(EXTRA_RUNTIME_NAME) ?: "jre-17"
        runtimeHomePath = intent.getStringExtra(EXTRA_RUNTIME_HOME) ?: ""
        lwjglNativesDirPath = intent.getStringExtra(EXTRA_LWJGL_NATIVES_DIR) ?: ""
        accessToken = intent.getStringExtra(EXTRA_ACCESS_TOKEN) ?: "0"
        userType = intent.getStringExtra(EXTRA_USER_TYPE) ?: "mojang"
        authlibInjectorPath = intent.getStringExtra(EXTRA_AUTHLIB_INJECTOR_PATH)
        authlibUrl = intent.getStringExtra(EXTRA_AUTHLIB_URL)
        useWrapper = intent.getBooleanExtra(EXTRA_USE_WRAPPER, false)
        loader = intent.getStringExtra(EXTRA_LOADER) ?: "vanilla"
        loaderVersion = intent.getStringExtra(EXTRA_LOADER_VERSION)
        installedMods = intent.getStringArrayListExtra(EXTRA_INSTALLED_MODS) ?: CrashManager.getInstalledMods(gameDirPath)
        extraJvmArgs = intent.getStringArrayListExtra(EXTRA_EXTRA_JVM_ARGS) ?: emptyList()
        extraGameArgs = intent.getStringArrayListExtra(EXTRA_EXTRA_GAME_ARGS) ?: emptyList()
        val curSettings = SettingsManager.settings.value
        discordRpcPlay = if (intent.hasExtra(EXTRA_DISCORD_RPC_PLAY)) {
            intent.getBooleanExtra(EXTRA_DISCORD_RPC_PLAY, false)
        } else {
            curSettings.discordRichPresenceOnPlay
        }
        discordRpcShowServer = if (intent.hasExtra(EXTRA_DISCORD_RPC_SERVER)) {
            intent.getBooleanExtra(EXTRA_DISCORD_RPC_SERVER, false)
        } else {
            curSettings.discordShowServer
        }
        if (discordRpcPlay) {
            com.aistudio.novacraft.launcher.core.utils.DiscordPresenceManager.startGameSession(
                context = applicationContext,
                mcVersion = mcVersion,
                loader = loader,
                allowServerDisplay = discordRpcShowServer
            )
        } else {
            com.aistudio.novacraft.launcher.core.utils.DiscordPresenceManager.clearPresence(applicationContext)
        }
        useTextureView = shouldUseTextureView()

        // Setup real-time native LoggerBridge
        setupLogger()

        // Setup exit callback
        ZLNativeInvoker.onExitCallback = { exitCode, isSignal ->
            runOnUiThread {
                liveLogs.add("[Amber Engine] Game process ended (code: $exitCode, signal: $isSignal)")
                handleGameExit(exitCode, isSignal)
            }
        }

        // Setup Graphic Output Listener (Mendeteksi Logo Mojang mulai dirender)
        CallbackBridge.setGraphicOutputListener {
            runOnUiThread {
                isGameRenderingState.value = true
                liveLogs.add("🎨 [Render Engine] First graphical frame rendered on Surface!")
                val tc = touchBridge
                if (tc != null && !tc.isConnected) {
                    LoggerBridge.append("▷ [TouchController] Game sudah tampil tetapi mod belum tersambung; kontrol bawaan tetap dipakai")
                }
                val midX = (CallbackBridge.windowWidth.takeIf { it > 0 } ?: 1280) / 2f
                val midY = (CallbackBridge.windowHeight.takeIf { it > 0 } ?: 720) / 2f
                CallbackBridge.sendCursorPos(midX, midY)
            }
        }

        val composeView = ComposeView(this).apply {
            setContent {
              GamepadFocusProvider {
                GameScreen(
                    instanceName = instanceName,
                    mcVersion = mcVersion,
                    username = username,
                    mainClass = mainClass,
                    runtimeName = runtimeName,
                    liveLogs = liveLogs,
                    loader = loader,
                    loaderVersion = loaderVersion,
                    isControlVisibleState = isControlVisibleState,
                    isGameRenderingState = isGameRenderingState,
                    touchSinkState = touchSinkState,
                    onSurfaceReady = { surfaceHolder ->
                        surfaceHolderRef = surfaceHolder
                        if (SettingsManager.settings.value.alternateSurfaceRendering) {
                            runCatching { surfaceHolder.setFormat(android.graphics.PixelFormat.RGBX_8888) }
                        }
                        val (targetWidth, targetHeight) = getScaledDisplayDimensions()
                        surfaceHolder.setFixedSize(targetWidth, targetHeight)
                        surfaceHolder.addCallback(this@GameActivity)
                    },
                    useTextureView = useTextureView,
                    onTextureViewReady = { textureView ->
                        gameTextureView = textureView
                        if (SettingsManager.settings.value.alternateSurfaceRendering) {
                            textureView.isOpaque = true
                        }
                        textureView.surfaceTextureListener = textureListener
                    },
                    onResolutionChange = { newRatio ->
                        applyDynamicResolution(newRatio)
                    },
                    onToggleRecord = { toggleRecording() },
                    guiScaleProvider = { currentGuiScale() },
                    onExit = {
                        isManualExit = true
                        terminateGameProcess()
                    }
                )
              }
            }
        }

        gameComposeView = composeView
        setContentView(composeView)
        applyImmersiveFullscreen()
        applyDeviceDisplayProfile()
    }

    /**
     * Kunci frekuensi layar selama game berjalan, mengikuti saklar "Kunci layar 60 Hz" di
     * Pengaturan > Grafis (bawaan mati). Di layar 90/120 Hz, game di HP lemah jarang mencapai
     * frekuensi itu; pada 20/30/45 FPS ritme tampil di 90 Hz tidak rata (frame bergantian tampil
     * 1 dan 2 kali) dan terasa patah-patah. 60 Hz kelipatan 20/30 sehingga ritmenya rata, dan
     * sedikit lebih hemat panas. Bisa dinyalakan/dimatikan di Pengaturan untuk dibandingkan.
     */
    private fun applyDeviceDisplayProfile() {
        val targetHz = DeviceProfiles.refreshLockTarget(SettingsManager.settings.value)
        if (targetHz <= 0f) return
        try {
            val params = window.attributes
            params.preferredRefreshRate = targetHz
            val modeId = DeviceProfiles.findDisplayModeId(this, targetHz)
            if (modeId != 0) {
                params.preferredDisplayModeId = modeId
            }
            window.attributes = params
            LoggerBridge.append("▷ [Layar] Meminta ${targetHz.toInt()} Hz (modeId=$modeId)")
        } catch (e: Throwable) {
            try {
                LoggerBridge.append("▷ [Layar] Gagal mengunci refresh rate: ${e.message}")
            } catch (_: Throwable) {
            }
        }
    }

    // Log game dari thread native ditampung dulu, lalu dipindah ke daftar tampilan paling cepat
    // tiap 250 ms. Sebelumnya SETIAP baris log memicu runOnUiThread + gambar ulang seluruh layar
    // kontrol; saat game ramai mencatat log (chat server, peringatan renderer, mod) thread utama
    // kewalahan dan FPS ikut turun. Berkas latestlog.txt tetap menerima semua baris (ditulis native).
    private val pendingLogs = java.util.concurrent.ArrayBlockingQueue<String>(2000)
    private val logFlushScheduled = java.util.concurrent.atomic.AtomicBoolean(false)
    private val logHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val logFlushRunnable = Runnable { flushPendingLogs() }

    /** Menambahkan log dari thread mana pun secara aman lewat antrean [pendingLogs]. */
    private fun appendUiLog(line: String) {
        if (pendingLogs.offer(line) && logFlushScheduled.compareAndSet(false, true)) {
            logHandler.postDelayed(logFlushRunnable, 250L)
        }
    }

    private fun flushPendingLogs() {
        logFlushScheduled.set(false)
        var moved = 0
        // Baris dikumpulkan dulu lalu dimasukkan ke daftar sekaligus: tiap perubahan daftar Compose menyalin
        // daftar dan menandai tampilan kotor, jadi ratusan perubahan per putaran membuat thread utama
        // tersendat di HP lemah saat game ramai mencatat log (memuat dunia, mod yang cerewet).
        val batch = ArrayList<String>(64)
        while (moved < 500) {
            val line = pendingLogs.poll() ?: break
            moved++
            batch.add(line)
            // Deteksi sekunder jika frame grafik atau sistem audio game benar-benar mulai aktif
            // (tidak memakai "Setting user:" karena muncul terlalu awal sebelum OpenGL/Surface siap)
            if (!isGameRenderingState.value) {
                if (line.contains("OpenAL initialized") ||
                    line.contains("Reloading ResourceManager") ||
                    line.contains("Sound engine started")) {
                    isGameRenderingState.value = true
                }
            }
        }
        if (batch.isNotEmpty()) {
            liveLogs.addAll(batch)
            val excess = liveLogs.size - 200
            if (excess > 0) liveLogs.subList(0, excess).clear()
        }
        // Masih ada sisa (lonjakan log): jadwalkan putaran berikutnya.
        if (!pendingLogs.isEmpty() && logFlushScheduled.compareAndSet(false, true)) {
            logHandler.postDelayed(logFlushRunnable, 250L)
        }
    }

    private fun setupLogger() {
        val logFile = File(filesDir, "latestlog.txt")
        // Sesi sebelumnya yang belum sempat tersimpan (mis. proses game dimatikan Android) disimpan dulu,
        // sebelum berkas log-nya ditimpa oleh sesi ini.
        try { GameLogArchive.archiveIfNeeded(this) } catch (_: Throwable) {}
        if (logFile.exists()) logFile.delete()
        logFile.createNewFile()
        try {
            GameLogArchive.beginSession(this, sessionStartTime, instanceName, mcVersion, loader, loaderVersion, gameDirPath)
        } catch (_: Throwable) {}

        LoggerBridge.setListener { text ->
            if (text != null) {
                if (discordRpcPlay && discordRpcShowServer) {
                    com.aistudio.novacraft.launcher.core.utils.DiscordPresenceManager.onGameLogLine(applicationContext, text)
                }
                pendingLogs.offer(text)
                if (logFlushScheduled.compareAndSet(false, true)) {
                    logHandler.postDelayed(logFlushRunnable, 250L)
                }
            }
        }

        try {
            LoggerBridge.start(logFile.absolutePath)
            LoggerBridge.appendTitle("NovaCraft Game Session")
            LoggerBridge.append("▷ Starting $instanceName ($mcVersion) on $runtimeName")
        } catch (e: Throwable) {
            liveLogs.add("[LoggerBridge Init] ${e.message}")
        }
    }

    /**
     * Minecraft 26.x punya backend Vulkan sendiri (dan bisa jatuh ke Vulkan bila OpenGL gagal). Swapchain
     * Vulkan di Android membaca "transform hint" permukaannya. Pada SurfaceView di HP berlayar-alami-portrait,
     * hint itu 90/270 derajat, dan backend Vulkan Mojang memakainya sebagai preTransform tanpa memutar isi
     * gambar (pre-rotation), sehingga buffer landscape diputar 90 derajat lalu diregangkan: seluruh tampilan game
     * terlihat portrait/miring padahal launcher landscape. Permukaan TextureView seharusnya tidak punya hint itu (0),
     * sehingga swapchain memakai orientasi normal dan gambarnya tegak. (Belum diuji di perangkat: bila
     * layar masih miring, cek baris log "[Graphics]" untuk tahu permukaan dan backend yang dipakai.)
     *
     * TextureView sedikit lebih berat dari SurfaceView, jadi hanya dipakai untuk 26.x, dan tidak dipakai bila
     * pemain sudah memilih "Prefer OpenGL" di options.txt (preferredGraphicsBackend:"opengl") dengan renderer yang
     * tidak membuat swapchain Vulkan sendiri. Zink (Kopper) membuatnya walau game memakai backend OpenGL, jadi untuk
     * renderer itu TextureView tetap dipakai. Saklar "Permukaan game TextureView" di Pengaturan memaksanya untuk
     * semua versi dan renderer.
     */
    private fun shouldUseTextureView(): Boolean {
        val settings = SettingsManager.settings.value
        if (settings.forceTextureView) return true
        if (!GlErrorPolicy.hasBackendChoice(mcVersion)) return false
        val selected = settings.selectedRenderer
        if (selected.contains("zink", ignoreCase = true) ||
            selected.contains("kopper", ignoreCase = true) ||
            selected.contains("vulkan", ignoreCase = true)
        ) return true
        val backend = runCatching {
            File(gameDirPath, "options.txt").useLines { lines ->
                lines.map { it.trim() }
                    .firstOrNull { it.startsWith("preferredGraphicsBackend:") }
                    ?.substringAfter(':')
                    ?.trim()
            }
        }.getOrNull()
        return backend != "\"opengl\""
    }

    private val textureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
            val (targetWidth, targetHeight) = getScaledDisplayDimensions()
            st.setDefaultBufferSize(targetWidth, targetHeight)
            val surface = android.view.Surface(st)
            gameTextureSurface = surface
            onGameSurfaceCreated(surface, this, targetWidth, targetHeight, "TextureView")
            CallbackBridge.sendUpdateWindowSize(targetWidth, targetHeight)
            LoggerBridge.append("▷ [SurfaceChanged] Native TextureView buffer: ${targetWidth}x${targetHeight} (Scale: ${SettingsManager.settings.value.resolutionRatio}%)")
        }

        // Ukuran buffer ditentukan setDefaultBufferSize (bukan ukuran View), jadi tidak ada yang diubah di sini
        override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {}

        override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
            onGameSurfaceDestroyed(this)
            try {
                gameTextureSurface?.release()
            } catch (_: Throwable) {
            }
            gameTextureSurface = null
            return true
        }

        override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        val (targetWidth, targetHeight) = getScaledDisplayDimensions()
        holder.setFixedSize(targetWidth, targetHeight)
        onGameSurfaceCreated(holder.surface, holder, targetWidth, targetHeight, "SurfaceView")
    }

    private fun onGameSurfaceCreated(
        surface: android.view.Surface,
        source: Any,
        targetWidth: Int,
        targetHeight: Int,
        kind: String
    ) {
        // Kunci layar (saklar "Kunci layar 60 Hz"): beri tahu sistem bahwa surface game cukup 60 Hz (Android 11+)
        val lockHz = DeviceProfiles.refreshLockTarget(SettingsManager.settings.value)
        if (lockHz > 0f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                surface.setFrameRate(lockHz, android.view.Surface.FRAME_RATE_COMPATIBILITY_DEFAULT)
            } catch (_: Throwable) {
            }
        }
        val rootLayout = window.decorView as? ViewGroup
        SdlBridge.prepareSurface(this, surface, rootLayout, source)
        try {
            ZLBridge.setupBridgeWindow(surface)
            liveLogs.add("[Render Bridge] Native window bound to $kind (${targetWidth}x${targetHeight}).")
        } catch (e: Throwable) {
            liveLogs.add("[Render Bridge Error] ${e.message}")
        }

        if (!isGameStarted) {
            isGameStarted = true
            startGameJVM()
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        val landscapeW = maxOf(width, height)
        val landscapeH = minOf(width, height)
        CallbackBridge.sendUpdateWindowSize(landscapeW, landscapeH)
        LoggerBridge.append("▷ [SurfaceChanged] Native surface buffer: ${landscapeW}x${landscapeH} (Scale: ${SettingsManager.settings.value.resolutionRatio}%)")
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        onGameSurfaceDestroyed(holder)
    }

    private fun onGameSurfaceDestroyed(source: Any) {
        try {
            val nativeSurface = SDLSurface.getNativeSurface()
            if (SdlBridge.beginSurfaceDestroy(source, nativeSurface)) {
                if (SdlBridge.sdlEnabled) {
                    SDLActivity.getSDLSurface()?.surfaceDestroyed()
                }
                SdlBridge.unregisterSurface(nativeSurface)
            }
            ZLBridge.releaseBridgeWindow()
        } catch (_: Throwable) {}
    }

    /**
     * Menyembunyikan tombol layar karena perangkat input lain dipakai, dan mencatat penyebabnya ke log game
     * SEKALI setiap kali tombol berubah dari terlihat ke tersembunyi (tanpa banjir log), supaya laporan
     * "kontrol tiba-tiba hilang" bisa ditelusuri dari log.
     */
    private fun hideControlsFor(reason: String) {
        if (!isControlVisibleState.value) return
        isControlVisibleState.value = false
        noteControlsHidden(reason)
    }

    private fun noteControlsHidden(reason: String) {
        try {
            LoggerBridge.append("▷ [Kontrol] Tombol layar disembunyikan otomatis: $reason")
        } catch (_: Throwable) {
            // Pustaka log belum siap: tombol tetap disembunyikan, hanya catatannya yang dilewati.
        }
    }

    /**
     * True bila tombol ini berasal dari keyboard fisik. Tombol dari keyboard layar (IME) tidak dihitung: ia
     * bertanda FLAG_SOFT_KEYBOARD dan/atau datang dari perangkat virtual (id negatif), dan sisanya masih bisa
     * dikirim sesaat setelah sesi ketik layar ditutup.
     */
    private fun isPhysicalKeyboardKey(event: KeyEvent): Boolean =
        event.deviceId >= 0 &&
            (event.flags and KeyEvent.FLAG_SOFT_KEYBOARD) == 0 &&
            !TouchCharInput.recentlyActive(SOFT_KEY_GRACE_MS)

    @android.annotation.SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // If soft keyboard IME input view is active, allow standard handling
        if (TouchCharInput.isActive()) {
            return super.dispatchKeyEvent(event)
        }

        // Controller: diteruskan ke mod (SDL/GLFW) atau diterjemahkan menjadi keyboard+mouse
        if (GamepadRouter.isPadKey(event)) {
            if (event.action == KeyEvent.ACTION_DOWN) hideControlsFor("tombol controller")
            if (GamepadRouter.onKey(event)) return true
        }

        val source = event.source

        // Tombol panah sintetis: Android mengubah gerakan mouse/trackball yang tidak tertangani
        // (sumber kelas trackball) menjadi DPAD. Jangan diteruskan ke game, kalau tidak fokus
        // menu Minecraft berpindah sendiri saat mouse digerakkan. Panah keyboard asli bersumber
        // keyboard, jadi tidak terpengaruh.
        if (source and InputDevice.SOURCE_CLASS_TRACKBALL != 0 && event.isDpadArrow()) {
            MouseDiag.once(MouseDiag.ARROW_SWALLOWED, source, 0, event.action, 0)
            return true
        }

        // Intercept right click from mouse being treated as KEYCODE_BACK
        if (source and InputDevice.SOURCE_MOUSE == InputDevice.SOURCE_MOUSE ||
            source and InputDevice.SOURCE_MOUSE_RELATIVE == InputDevice.SOURCE_MOUSE_RELATIVE) {
            if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                val isDown = event.action == KeyEvent.ACTION_DOWN
                // Di menu game tombolnya lewat pelacak yang sama dengan event gerak mouse (tidak terkirim ganda).
                if (!MenuMouse.onBackKey(isDown)) {
                    // Pelacak yang sama dengan jalur gerak/tangkap: klik kanan yang datang dua kali tidak terkirim ganda.
                    MenuMouse.sendButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT, isDown)
                }
                return true
            }
        }

        // Physical keyboard key pressed -> automatically hide touch controls. Tombol dari keyboard layar tidak
        // termasuk (lihat isPhysicalKeyboardKey): dulu semua tombol di sini menyembunyikan kontrol, jadi tombol
        // sisa dari keyboard layar yang baru ditutup ikut menghilangkan seluruh kontrol.
        if (event.action == KeyEvent.ACTION_DOWN && isPhysicalKeyboardKey(event)) {
            hideControlsFor("keyboard fisik (${KeyEvent.keyCodeToString(event.keyCode)}, device=${event.deviceId})")
        }

        val index = EfficientAndroidLWJGLKeycode.getIndexByKey(event.keyCode)
        if (index >= 0) {
            EfficientAndroidLWJGLKeycode.execKey(event, index)
            return true
        }

        return super.dispatchKeyEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (GamepadRouter.isPadMotion(event)) {
            // Controller dipakai: tombol layar disembunyikan otomatis, seperti saat mouse fisik dipakai.
            if (GamepadRouter.isDeliberate(event)) hideControlsFor("gerakan controller")
            if (GamepadRouter.onMotion(event)) return true
        }
        if (event.isFromSource(InputDevice.SOURCE_MOUSE) || event.isFromSource(InputDevice.SOURCE_MOUSE_RELATIVE)) {
            // Physical mouse action detected -> hide touch controls
            hideControlsFor("mouse fisik (gerak/klik/gulir)")
            MouseDiag.actGeneric++
            MouseDiag.once(MouseDiag.ACT_GENERIC, event)
            MouseDiag.combo(MouseDiag.COMBO_GENERIC, event.actionMasked, event.buttonState)
            // Menu game: tombol dan gulir mouse dikirim dari sini, sebelum Compose (lihat MenuMouse).
            MenuMouse.onGeneric(event)
        }
        return super.dispatchGenericMotionEvent(event)
    }

    /**
     * Mouse/touchpad yang dilaporkan sebagai trackball (gerakan relatif, bukan hover absolut) tidak
     * lewat jalur hover maupun pointer capture. Tanpa penanganan ini Android mengubahnya menjadi
     * tombol panah, jadi kursor diam tapi fokus menu berpindah. Di sini gerakannya diteruskan sebagai
     * mouse: delta kamera saat game menangkap kursor, selain itu menggeser kursor menu.
     * Hanya berjalan saat ada event; tanpa alokasi dan tanpa polling.
     */
    override fun dispatchTrackballEvent(event: MotionEvent): Boolean {
        if (event.source and InputDevice.SOURCE_CLASS_TRACKBALL == 0) {
            return super.dispatchTrackballEvent(event)
        }
        if (event.actionMasked == MotionEvent.ACTION_MOVE) {
            MouseDiag.trackballEvents++
            MouseDiag.once(MouseDiag.TRACKBALL, event)
            hideControlsFor("trackball/touchpad")
            val s = SettingsManager.settings.value
            if (ZLBridgeStates.cursorMode.value == CURSOR_DISABLED) {
                val sens = s.captureSensitivity / 100f
                CallbackBridge.sendCursorDelta(event.x * sens, event.y * sens)
            } else {
                val sens = s.cursorSensitivity / 100f
                GamepadRouter.cursorMover?.invoke(event.x * sens, event.y * sens)
            }
        }
        // Ditelan semua (gerak/klik): jangan biarkan Android membuat tombol panah dari event ini.
        return true
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.isFromSource(InputDevice.SOURCE_MOUSE) || event.isFromSource(InputDevice.SOURCE_MOUSE_RELATIVE)) {
            hideControlsFor("mouse fisik (sentuhan dari sumber mouse)")
            MouseDiag.actTouch++
            MouseDiag.once(MouseDiag.ACT_TOUCH, event)
            MouseDiag.combo(MouseDiag.COMBO_TOUCH, event.actionMasked, event.buttonState)
            // Menu game: klik dan seret mouse dikirim dari sini, sebelum Compose (lihat MenuMouse).
            MenuMouse.onTouch(event, gameComposeView)
        }
        return super.dispatchTouchEvent(event)
    }

    private fun startGameJVM() {
        thread(name = "NovaCraft-JVM-Thread") {
            try {
                val nativeLibDir = applicationInfo.nativeLibraryDir
                val runtimeHome = File(runtimeHomePath)
                val gameDir = File(gameDirPath)
                val assetsDir = File(assetsDirPath)

                appendUiLog("[NovaCraft] Menyiapkan environment JVM...")

                val activeSettings = SettingsManager.settings.value
                val (targetWidth, targetHeight) = getScaledDisplayDimensions()

                // Jarak pandang awal mengikuti RAM HP. HP 4 GB ke bawah tetap 2 chunk seperti biasa;
                // HP yang lebih besar mendapat nilai lebih lega. Hanya dipakai bila options.txt belum
                // punya nilainya (pertama kali instance dijalankan), jadi pilihan pemain tidak ditimpa.
                val totalRamForOptions = SettingsManager.getTotalDeviceMemoryMb(this@GameActivity)
                val defaultRenderDistance = when {
                    totalRamForOptions >= 7000 -> "6"
                    totalRamForOptions >= 5000 -> "4"
                    else -> "2"
                }

                // Setup & Optimize Minecraft options.txt (Zalith pure behavior)
                MCOptions.setup(this@GameActivity, gameDir)
                MCOptions.apply {
                    set("fullscreen", "false")
                    set("touchscreen", "false")
                    set("options.narrator", "0")
                    set("narrator", "0")

                    // Ensure default low-end mobile performance options if enabled in settings
                    if (activeSettings.autoOptimizeMinecraft) {
                        if (!containsKey("clouds")) set("clouds", "false")
                        if (!containsKey("renderClouds")) set("renderClouds", "false")
                        if (!containsKey("entityShadows")) set("entityShadows", "false")
                        if (!containsKey("renderDistance")) set("renderDistance", defaultRenderDistance)
                        if (!containsKey("simulationDistance")) set("simulationDistance", "5")
                        // Pilihan grafis paling hemat (daun tanpa transparansi, partikel minimal, tanpa
                        // pencampuran warna bioma, entitas jauh dikurangi). Hanya diisi bila belum ada,
                        // jadi pilihan pemain di dalam game tidak pernah ditimpa; kunci yang tidak
                        // dikenal versi lama diabaikan Minecraft.
                        if (!containsKey("graphicsMode")) set("graphicsMode", "0")
                        if (!containsKey("particles")) set("particles", "2")
                        if (!containsKey("biomeBlendRadius")) set("biomeBlendRadius", "0")
                        if (!containsKey("entityDistanceScaling")) set("entityDistanceScaling", "0.5")
                    }

                    set("overrideWidth", "$targetWidth")
                    set("overrideHeight", "$targetHeight")
                    save()
                }
                if (GlErrorPolicy.hasBackendChoice(mcVersion)) {
                    @Suppress("DEPRECATION")
                    val displayRotation = try { windowManager.defaultDisplay.rotation * 90 } catch (_: Throwable) { -1 }
                    LoggerBridge.append("▷ [Graphics] preferredGraphicsBackend=${MCOptions.get("preferredGraphicsBackend") ?: "(belum ada)"}, permukaan game=${if (useTextureView) "TextureView" else "SurfaceView"}, renderer=${activeSettings.selectedRenderer}, rotasi layar=${displayRotation} derajat")
                }
                LoggerBridge.append("▷ [MCOptions] Minecraft options verified (res=${targetWidth}x${targetHeight}, autoOptimize=${activeSettings.autoOptimizeMinecraft})")

                // Verify & Ensure OpenJDK Runtime is fully installed
                if (!com.aistudio.novacraft.launcher.core.launch.JavaRuntimeManager.isRuntimeInstalled(this@GameActivity, runtimeName)) {
                    appendUiLog("[Amber Runtime] Mempersiapkan OpenJDK ($runtimeName)...")
                    LoggerBridge.append("▷ [Amber Runtime] Runtime $runtimeName missing or incomplete, extracting from assets...")
                    val extRes = kotlinx.coroutines.runBlocking {
                        com.aistudio.novacraft.launcher.core.launch.JavaRuntimeManager.extractRuntime(this@GameActivity, runtimeName) { msg ->
                            appendUiLog("[Amber Runtime] $msg")
                            LoggerBridge.append("▷ [Amber Runtime] $msg")
                        }
                    }
                    if (extRes.isFailure) {
                        val errMsg = extRes.exceptionOrNull()?.message ?: "Extraction failed"
                        appendUiLog("[ERROR] Gagal mengekstrak OpenJDK $runtimeName: $errMsg")
                        LoggerBridge.append("[ERROR] Gagal mengekstrak OpenJDK: $errMsg")
                        throw Exception("Gagal menyiapkan OpenJDK ($runtimeName): $errMsg")
                    }
                    appendUiLog("[Amber Runtime] OpenJDK ($runtimeName) siap digunakan!")
                }

                // Combined Library Path (Include Java runtime libraries for pojavexec dlopen)
                val javaLibDir = File(runtimeHome, "lib")
                val javaServerDir = File(runtimeHome, "lib/server")
                val javaJliDir = File(runtimeHome, "lib/jli")

                val javaLibPaths = listOf(javaLibDir, javaServerDir, javaJliDir)
                    .filter { it.exists() }
                    .map { it.absolutePath }

                // Resolve Selected Graphics Renderer
                try {
                    com.aistudio.novacraft.launcher.core.launch.NuxRendererPluginManager.scanPlugins(this)
                } catch (e: Throwable) {
                    LoggerBridge.append("▷ [Renderer] Plugin scan error: ${e.message}")
                }

                val targetRenderer = NuxRendererRegistry.resolveRenderer(
                    selectedId = activeSettings.selectedRenderer,
                    mcVersion = mcVersion,
                    nativeLibDir = File(nativeLibDir),
                    context = this
                )
                val rendererId = targetRenderer.rendererId
                val rendererSoName = targetRenderer.libraryName

                LoggerBridge.append("▷ [Renderer] Active Backend: ${targetRenderer.displayName} ($rendererId)")
                LoggerBridge.append("▷ [Renderer] Library: $rendererSoName, Selection: ${activeSettings.selectedRenderer}, IsPlugin: ${targetRenderer.isPlugin}")
                graphicsSummary = "${targetRenderer.displayName} ($rendererId, $rendererSoName), " +
                    "backend=${MCOptions.get("preferredGraphicsBackend") ?: "-"}, " +
                    "surface=${if (useTextureView) "TextureView" else "SurfaceView"}"

                val runtimeHomeStr = runtimeHome.absolutePath
                // Ditentukan dari bitness proses ini (bukan daftar ABI HP): APK 32-bit di HP 64-bit
                // tetap perlu /system/lib, bukan /system/lib64.
                val libDirName = if (android.os.Process.is64Bit()) "lib64" else "lib"

                // Plugin native library (Distant Horizons dkk). Hasil pemindaian di atas sudah memuatnya, jadi ini
                // hanya membaca daftar di memori. Tanpa plugin terpasang rencananya kosong dan tidak ada yang berubah.
                val nativePlan = com.aistudio.novacraft.launcher.core.utils.NativePluginManager.planFor(
                    mcVersion, activeSettings.disabledNativePlugins
                )
                nativePlan.active.forEach {
                    LoggerBridge.append("▷ [Plugin Native] ${it.displayName} (${it.packageName}): ${it.nativeLibDir}")
                }
                nativePlan.skipped.forEach { LoggerBridge.append("▷ [Plugin Native] dilewati - $it") }

                // 1. Runtime linker library path (Dipakai oleh ZLBridge.setLdLibraryPath untuk memperbarui Android Bionic Linker)
                val runtimeLdPaths = mutableListOf<String>()
                val pluginNative = targetRenderer.pluginNativePath
                if (targetRenderer.isPlugin && !pluginNative.isNullOrBlank()) {
                    runtimeLdPaths.add(pluginNative)
                }
                // Deteksi subdirektori arsitektur Java 8 (misal lib/aarch64/)
                val archSubDir = listOf("aarch64", "aarch32", "arm", "i386", "amd64", "x86_64")
                    .map { File("$runtimeHomeStr/lib", it) }
                    .firstOrNull { it.exists() && it.isDirectory }

                if (archSubDir != null) {
                    runtimeLdPaths.add("${archSubDir.absolutePath}/jli")
                    runtimeLdPaths.add("${archSubDir.absolutePath}/server")
                    runtimeLdPaths.add("${archSubDir.absolutePath}/client")
                    runtimeLdPaths.add(archSubDir.absolutePath)
                }

                runtimeLdPaths.add("$runtimeHomeStr/lib/jli")
                if (File("$runtimeHomeStr/jre").exists()) {
                    runtimeLdPaths.add("$runtimeHomeStr/jre/lib/server")
                    runtimeLdPaths.add("$runtimeHomeStr/jre/lib")
                } else {
                    runtimeLdPaths.add("$runtimeHomeStr/lib/server")
                    runtimeLdPaths.add("$runtimeHomeStr/lib")
                }
                if (lwjglNativesDirPath.isNotBlank()) runtimeLdPaths.add(lwjglNativesDirPath)
                runtimeLdPaths.add(nativeLibDir)
                // Setelah folder milik launcher: pustaka launcher (mis. libc++_shared) tidak boleh tertimpa
                // salinan di dalam plugin, tapi pustaka unik plugin tetap ditemukan lewat linker.
                runtimeLdPaths.addAll(nativePlan.libPaths)
                runtimeLdPaths.add("/system/$libDirName")
                runtimeLdPaths.add("/vendor/$libDirName")
                runtimeLdPaths.add("/vendor/$libDirName/hw")
                runtimeLdPaths.add("/system_ext/$libDirName")

                val runtimeLdLibraryPath = runtimeLdPaths.distinct().joinToString(":")
                ZLBridge.setLdLibraryPath(runtimeLdLibraryPath)

                // 2. Game LD_LIBRARY_PATH (Identik dengan ZalithLauncher2 getLibraryPath() - JANGAN SAMPAI ADA FOLDER JRE lib/server / lib/jli!)
                // Jika LD_LIBRARY_PATH mengandung 'lib/server' atau 'lib/client', OpenJDK libjli.so akan mengira perlu re-exec
                // dan memanggil execve(bin/java) yang otomatis ditolak Android kernel (Permission denied)!
                val gameLdPaths = mutableListOf<String>()
                if (lwjglNativesDirPath.isNotBlank()) gameLdPaths.add(lwjglNativesDirPath)
                gameLdPaths.add("/system/$libDirName")
                gameLdPaths.add("/vendor/$libDirName")
                gameLdPaths.add("/vendor/$libDirName/hw")
                gameLdPaths.add("/system_ext/$libDirName")
                if (targetRenderer.isPlugin && !pluginNative.isNullOrBlank()) {
                    gameLdPaths.add(pluginNative)
                }
                gameLdPaths.add(nativeLibDir)
                // Plugin native library: dipakai System.loadLibrary mod (java.library.path) dan dlopen via nama.
                gameLdPaths.addAll(nativePlan.libPaths)
                val gameLdLibraryPath = gameLdPaths.distinct().joinToString(":")

                val glLibPath = if (targetRenderer.isPlugin && !targetRenderer.pluginNativePath.isNullOrBlank()) {
                    if (rendererSoName.startsWith("/")) rendererSoName else "${targetRenderer.pluginNativePath}/$rendererSoName"
                } else {
                    "$nativeLibDir/$rendererSoName"
                }

                val actualPojavRenderer = when {
                    rendererId.startsWith("opengles") -> rendererId
                    rendererId.startsWith("vulkan") -> rendererId
                    rendererId.startsWith("gallium") -> rendererId
                    rendererId == "custom_gallium" -> rendererId
                    else -> "opengles3"
                }
                Os.setenv("POJAV_NATIVEDIR", nativeLibDir, true)
                Os.setenv("POJAV_RENDERER", actualPojavRenderer, true)
                Os.setenv("POJAV_SDL_REUSE_WINDOW", "1", true)
                Os.setenv("SDL_OPENGL_LIBRARY", glLibPath, true)

                // Apply renderer specific environment variables
                targetRenderer.envVariables.forEach { (k, v) ->
                    Os.setenv(k, v, true)
                }

                // Apply Renderer V2 Environment Variables (persis Zalith fclPlugin_V2 & MobileGlues)
                try {
                    val v2Envs = com.aistudio.novacraft.launcher.core.launch.NuxRendererV2Manager.getEffectiveEnv(targetRenderer)
                    v2Envs.forEach { (k, v) ->
                        Os.setenv(k, v, true)
                        LoggerBridge.append("▷ [Renderer V2 Env] $k = $v")
                    }
                } catch (e: Exception) {
                    LoggerBridge.append("▷ [Renderer V2 Env Warning] Gagal menginjeksi V2 env: ${e.message}")
                }
                // MobileGlues hanya membaca config.json (cache shader dan lain-lain) bila launcher dikenalnya.
                // Tanpa MG_DIR_PATH ia berkata "Unsupported launcher detected" dan memaksa semua ke bawaan, termasuk
                // cache terjemahan shader mati. Bila MG_DIR_PATH sudah diisi renderer/plugin, itu dihormati.
                try {
                    if (activeSettings.mobileGluesConfig &&
                        MobileGluesConfig.isMobileGlues(rendererSoName) &&
                        Os.getenv(MobileGluesConfig.ENV_DIR).isNullOrBlank()
                    ) {
                        val mgDir = MobileGluesConfig.prepare(
                            this@GameActivity,
                            File(glLibPath),
                            enableAngle = activeSettings.useAngle || activeSettings.useSystemAngle
                        )
                        if (mgDir != null) {
                            Os.setenv(MobileGluesConfig.ENV_DIR, mgDir.absolutePath, true)
                            LoggerBridge.append("▷ [MobileGlues] Config launcher dipakai: ${mgDir.absolutePath}")
                        } else {
                            LoggerBridge.append("▷ [MobileGlues Warning] Config launcher tidak bisa disiapkan, MobileGlues memakai bawaannya")
                        }
                    }
                } catch (e: Throwable) {
                    LoggerBridge.append("▷ [MobileGlues Warning] Config launcher dilewati: ${e.message}")
                }
                try {
                    Os.getenv("MG_DIR_PATH")?.takeIf { it.isNotBlank() }?.let { GameLogArchive.noteMgDir(this@GameActivity, it) }
                } catch (_: Throwable) {}

                // Minecraft 26.x memeriksa glGetError saat membuat backend OpenGL. "LIBGL_NOERROR=1"
                // (bawaan Krypton/GL4ES demi kecepatan) membuatnya selalu menjawab "tanpa galat" dan
                // game berhenti dengan "glGetError mismatch". Matikan hanya untuk versi itu.
                if (GlErrorPolicy.needsRealGlError(mcVersion)) {
                    Os.setenv("LIBGL_NOERROR", "0", true)
                    LoggerBridge.append("▷ [Renderer] Minecraft $mcVersion memeriksa glGetError: LIBGL_NOERROR dimatikan")
                }

                // Pastikan flag GLSL & Extension compatibility selalu aktif untuk non-GL4ES renderers
                // (Standar kompatibilitas Zalith Launcher 2 untuk MobileGL, Zink, Mesa, dan Custom Renderer)
                val isPureLegacyGl4es = targetRenderer.id == "gl4es" || targetRenderer.rendererId == "opengles2"
                if (!isPureLegacyGl4es) {
                    Os.setenv("allow_higher_compat_version", "true", true)
                    Os.setenv("allow_glsl_extension_directive_midshader", "true", true)
                    Os.setenv("force_glsl_extensions_warn", "true", true)
                    LoggerBridge.append("▷ [GLSL Compat] Injected allow_glsl_extension_directive_midshader=true, allow_higher_compat_version=true, force_glsl_extensions_warn=true")
                }

                // Apply EGL specific library path if specified (full absolute path for plugins)
                val rawEgl = targetRenderer.eglName
                val eglPath = if (!rawEgl.isNullOrBlank()) {
                    if (rawEgl.startsWith("/")) rawEgl
                    else if (targetRenderer.isPlugin && !pluginNative.isNullOrBlank()) "$pluginNative/$rawEgl"
                    else "$nativeLibDir/$rawEgl"
                } else null

                if (!eglPath.isNullOrBlank()) {
                    Os.setenv("POJAVEXEC_EGL", eglPath, true)
                    Os.setenv("SDL_EGL_LIBRARY", eglPath, true)
                }

                // Apply Vulkan / Zink / ANGLE / VSync / Thermal settings
                if (activeSettings.zinkPreferSystemDriver) {
                    Os.setenv("POJAV_ZINK_PREFER_SYSTEM_DRIVER", "1", true)
                    LoggerBridge.append("▷ [Renderer Config] POJAV_ZINK_PREFER_SYSTEM_DRIVER=1")
                }
                if (activeSettings.vsyncInZink || activeSettings.forceVsync) {
                    Os.setenv("POJAV_VSYNC_IN_ZINK", "1", true)
                    LoggerBridge.append("▷ [Renderer Config] POJAV_VSYNC_IN_ZINK=1")
                }
                if (activeSettings.forceVsync) {
                    Os.setenv("POJAV_FORCE_VSYNC", "1", true)
                    Os.setenv("MESA_VK_WSI_PRESENT_MODE", "fifo", true)
                    Os.setenv("vblank_mode", "1", true)
                    LoggerBridge.append("▷ [Renderer Config] Force VSync aktif (POJAV_FORCE_VSYNC=1)")
                }
                if (activeSettings.useAngle) {
                    Os.setenv("POJAV_USE_ANGLE", "1", true)
                    Os.setenv("LIBGL_USE_ANGLE", "1", true)
                    Os.setenv("ANGLE_DEFAULT_PROGRAM", "vulkan", true)
                    LoggerBridge.append("▷ [Renderer Config] Use ANGLE aktif (POJAV_USE_ANGLE=1)")
                }
                if (activeSettings.useSystemAngle) {
                    Os.setenv("POJAV_SYSTEM_ANGLE", "1", true)
                    Os.setenv("EGL_PLATFORM", "android", true)
                    LoggerBridge.append("▷ [Renderer Config] System ANGLE aktif (POJAV_SYSTEM_ANGLE=1)")
                }
                val resolvedPerfCap = activeSettings.thermalPerfCap.let { if (it <= 0) 90 else it.coerceIn(50, 100) }
                Os.setenv("POJAV_THERMAL_PERF_CAP", "$resolvedPerfCap", true)
                if (activeSettings.sustainedPerformanceMode) {
                    Os.setenv("POJAV_SUSTAINED_PERFORMANCE", "1", true)
                    LoggerBridge.append("▷ [Thermal] Đã bật chế độ duy trì hiệu suất (POJAV_SUSTAINED_PERFORMANCE=1)")
                }
                if (activeSettings.alternateSurfaceRendering) {
                    Os.setenv("POJAV_ALTERNATE_SURFACE", "1", true)
                    Os.setenv("LIBGL_NOERROR", "1", true)
                    Os.setenv("MESA_NO_ERROR", "1", true)
                    LoggerBridge.append("▷ [Graphics] Dùng kết xuất bề mặt thay thế (POJAV_ALTERNATE_SURFACE=1, RGBX_8888)")
                }
                Os.setenv("POJAV_JAR_SANDBOX", if (activeSettings.jarSandboxEnabled) "1" else "0", true)
                if (activeSettings.graphicsApi != "DEFAULT") {
                    Os.setenv("POJAV_GRAPHICS_API", activeSettings.graphicsApi, true)
                    LoggerBridge.append("▷ [Renderer Config] POJAV_GRAPHICS_API=${activeSettings.graphicsApi}")
                }
                if (activeSettings.vulkanDriver != "auto" && activeSettings.vulkanDriver.isNotBlank()) {
                    Os.setenv("POJAV_VULKAN_DRIVER", activeSettings.vulkanDriver, true)
                    LoggerBridge.append("▷ [Renderer Config] POJAV_VULKAN_DRIVER=${activeSettings.vulkanDriver}")
                }
                if (activeSettings.dumpShadersEnabled) {
                    Os.setenv("LIBGL_SHADERCONV_DUMP", "1", true)
                    Os.setenv("MG_DUMP_SHADERS", "1", true)
                    Os.setenv("ZINK_DEBUG", "shader", true)
                    LoggerBridge.append("▷ [Experimental] Shader dump aktif")
                }
                if (activeSettings.forceBigCoreRender) {
                    Os.setenv("POJAV_BIG_CORE_AFFINITY", "1", true)
                    try {
                        Process.setThreadPriority(Process.THREAD_PRIORITY_DISPLAY)
                    } catch (_: Throwable) {}
                    LoggerBridge.append("▷ [Experimental] Render thread big-core affinity aktif")
                }
                val mesaDebugFlags = mutableListOf<String>()
                if (activeSettings.freedrenoSysmem) mesaDebugFlags += "sysmem"
                if (activeSettings.ubwcWorkaround) {
                    mesaDebugFlags += "ubwc"
                    Os.setenv("TP_UBWC_FLAG_HINT", "1", true)
                    LoggerBridge.append("▷ [Experimental] UBWC workaround aktif (TP_UBWC_FLAG_HINT=1)")
                }
                if (mesaDebugFlags.isNotEmpty()) {
                    val joined = mesaDebugFlags.joinToString(",")
                    Os.setenv("FD_MESA_DEBUG", joined, true)
                    Os.setenv("TU_DEBUG", joined, true)
                    LoggerBridge.append("▷ [Experimental] FD_MESA_DEBUG=$joined")
                }

                // Pre-dlopen plugin libraries from the plugin's own directory ONLY
                if (targetRenderer.isPlugin && !targetRenderer.pluginNativePath.isNullOrBlank()) {
                    val pluginDir = File(targetRenderer.pluginNativePath)

                    // Muat dulu dependensi pustaka plugin (mis. libSDL3.so). Pustaka di folder aplikasi
                    // plugin tidak bisa menemukannya sendiri, dan game berhenti dengan
                    // "dlopen failed: library libSDL3.so not found". Setelah dimuat lewat path lengkap,
                    // Android mengenalinya berdasarkan nama. Folder launcher dicari lebih dulu supaya
                    // SDL3 yang dipakai plugin sama dengan yang dipakai integrasi SDL launcher.
                    var preloadedDeps: Set<String> = emptySet()
                    try {
                        val depDirs = listOf(File(nativeLibDir), pluginDir)
                        val depRoots = listOfNotNull(File(glLibPath), eglPath?.let { File(it) })
                            .filter { it.isFile }
                        val deps = depRoots.flatMap { ElfDeps.loadOrder(it, depDirs) }.distinct()
                        preloadedDeps = deps.map { it.name }.toSet()
                        deps.forEach { dep ->
                            val ok = try { ZLBridge.dlopen(dep.absolutePath) } catch (e: Throwable) { false }
                            LoggerBridge.append(
                                if (ok) "▷ [Plugin Dependency] ${dep.name} dimuat dari ${dep.parent}"
                                else "▷ [Plugin Dependency Warning] ${dep.name} gagal dimuat"
                            )
                        }
                    } catch (e: Throwable) {
                        LoggerBridge.append("▷ [Plugin Dependency Warning] ${e.message}")
                    }

                    targetRenderer.dlopenLibs.forEach { dlName ->
                        val dlFile = File(pluginDir, dlName)
                        // Yang sudah dimuat sebagai dependensi tidak dimuat lagi dari folder plugin
                        // (salinan kedua dengan nama sama akan menjadi pustaka terpisah).
                        if (dlName !in preloadedDeps && dlFile.exists()) {
                            try {
                                ZLBridge.dlopen(dlFile.absolutePath)
                                LoggerBridge.append("▷ [Plugin DLOPEN] $dlName")
                            } catch (e: Throwable) {
                                LoggerBridge.append("▷ [Plugin DLOPEN Warning] $dlName: ${e.message}")
                            }
                        }
                    }
                    val targetSo = File(glLibPath)
                    if (targetSo.exists()) {
                        try {
                            if (ZLBridge.dlopen(targetSo.absolutePath)) {
                                LoggerBridge.append("▷ [Plugin GL Loaded] ${targetSo.absolutePath}")
                            } else {
                                LoggerBridge.append("▷ [Plugin GL Warning] ${targetRenderer.libraryName}: dlopen gagal (dependensi tidak ditemukan?)")
                            }
                        } catch (e: Throwable) {
                            LoggerBridge.append("▷ [Plugin GL Warning] ${targetRenderer.libraryName}: ${e.message}")
                        }
                    }
                    if (!eglPath.isNullOrBlank() && eglPath != glLibPath) {
                        val eglFile = File(eglPath)
                        if (eglFile.exists()) {
                            try {
                                ZLBridge.dlopen(eglFile.absolutePath)
                                LoggerBridge.append("▷ [Plugin EGL Loaded] ${eglFile.absolutePath}")
                            } catch (e: Throwable) {
                                LoggerBridge.append("▷ [Plugin EGL Warning]: ${e.message}")
                            }
                        }
                    }
                }

                // Mesa GLSL Cache Directory
                if (rendererId.startsWith("gallium") || rendererId.contains("zink")) {
                    Os.setenv("MESA_GLSL_CACHE_DIR", cacheDir.absolutePath, true)
                }
                Os.setenv("JAVA_HOME", runtimeHome.absolutePath, true)
                Os.setenv("HOME", gameDir.absolutePath, true)
                Os.setenv("TMPDIR", cacheDir.absolutePath, true)
                Os.setenv("PATH", "${runtimeHome.absolutePath}/bin:" + (Os.getenv("PATH") ?: "/system/bin"), true)
                Os.setenv("LD_LIBRARY_PATH", gameLdLibraryPath, true)
                Os.setenv("AWTSTUB_WIDTH", "$targetWidth", true)
                Os.setenv("AWTSTUB_HEIGHT", "$targetHeight", true)
                Os.setenv("ALSOFT_DRIVERS", if (activeSettings.forceOpenSlAudio) "opensl" else "oboe,opensl", true)
                if (activeSettings.forceOpenSlAudio) {
                    Os.setenv("POJAV_FORCE_OPENSL", "1", true)
                    LoggerBridge.append("▷ [Experimental] ALSoft dipaksa memakai OpenSL")
                }

                // Mod TouchController: socket harus sudah dikenal lewat env sebelum JVM berjalan.
                // Tanpa modnya, tidak ada yang dibuat sama sekali (tanpa thread, socket, atau penelusuran).
                if (activeSettings.touchControllerEnabled && TouchControllerSupport.isModInstalled(installedMods)) {
                    val socketName = TouchControllerSupport.newSocketName()
                    val waitStart = System.currentTimeMillis()
                    val bridge = TouchControllerBridge.start(
                        socketName = socketName,
                        scanner = TouchControllerBridge.ClassScanner { DexClassScanner.find(this@GameActivity, it) },
                        log = { LoggerBridge.append("▷ $it") },
                        // Sentuhan baru diteruskan ke mod setelah modnya benar-benar tersambung;
                        // sebelum itu (dan bila tidak pernah) layar dipakai seperti biasa.
                        onConnected = { connected ->
                            val sec = (System.currentTimeMillis() - waitStart) / 1000
                            LoggerBridge.append(
                                "▷ [TouchController] Mod tersambung (${sec}s setelah dimulai): " +
                                    "sentuhan juga diteruskan ke mod, kontrol bawaan tetap aktif"
                            )
                            runOnUiThread {
                                touchSinkState.value = connected
                            }
                        }
                    )
                    if (bridge != null) {
                        Os.setenv(TouchControllerSupport.ENV_SOCKET, socketName, true)
                        touchBridge = bridge
                    }
                }

                // Android DNS Resolver Setup (Direct match with Zalith Launcher 2 & Pojav)
                val resolvConf = File(gameDir, "resolv.conf")
                val dnsServers = buildSet {
                    try {
                        val cm = getSystemService(android.net.ConnectivityManager::class.java)
                        val activeNet = cm?.activeNetwork
                        if (activeNet != null) {
                            val lp = cm.getLinkProperties(activeNet)
                            lp?.dnsServers
                                ?.mapNotNull { it.hostAddress?.takeIf(String::isNotEmpty) }
                                ?.filterNot { it.contains(':') }
                                ?.let { addAll(it) }
                        }
                    } catch (_: Throwable) {}
                    add("1.1.1.1")
                    add("1.0.0.1")
                }
                val configText = dnsServers.joinToString(separator = "\n") { "nameserver $it" }
                runCatching {
                    if (!resolvConf.exists() || resolvConf.readText().trim() != configText.trim()) {
                        resolvConf.writeText(configText)
                    }
                    val altResolv = File(filesDir, "resolv.conf")
                    if (!altResolv.exists() || altResolv.readText().trim() != configText.trim()) {
                        altResolv.writeText(configText)
                    }
                }
                try {
                    Os.setenv("RESOLV_CONF", resolvConf.absolutePath, true)
                } catch (_: Throwable) {}

                // Pre-dlopen Java Runtime core libraries (Identical to Zalith Launcher 2 dlopenJavaRuntime)
                val isJava8 = File(runtimeHome, "jre").exists() || File(runtimeHome, "lib/rt.jar").exists() || File(runtimeHome, "lib/rt.jar.pack").exists()
                val rtLibDir = if (File(runtimeHome, "jre/lib").exists()) File(runtimeHome, "jre/lib") else File(runtimeHome, "lib")
                val archLibDir = listOf("aarch64", "aarch32", "arm", "i386", "amd64", "x86_64")
                    .map { File(rtLibDir, it) }
                    .firstOrNull { it.exists() && it.isDirectory } ?: rtLibDir

                val rtJliDir = when {
                    File(rtLibDir, "jli/libjli.so").exists() -> File(rtLibDir, "jli")
                    File(archLibDir, "jli/libjli.so").exists() -> File(archLibDir, "jli")
                    else -> rtLibDir
                }
                val rtJvmDir = when {
                    File(rtLibDir, "server/libjvm.so").exists() -> File(rtLibDir, "server")
                    File(rtLibDir, "client/libjvm.so").exists() -> File(rtLibDir, "client")
                    File(archLibDir, "server/libjvm.so").exists() -> File(archLibDir, "server")
                    File(archLibDir, "client/libjvm.so").exists() -> File(archLibDir, "client")
                    else -> rtLibDir
                }

                val essentialLibs = listOf(
                    File(rtJliDir, "libjli.so"),
                    File(rtJvmDir, "libjvm.so"),
                    File(rtLibDir, "libfreetype.so"),
                    File(archLibDir, "libfreetype.so"),
                    File(rtLibDir, "libverify.so"),
                    File(archLibDir, "libverify.so"),
                    File(rtLibDir, "libjava.so"),
                    File(archLibDir, "libjava.so"),
                    File(rtLibDir, "libnet.so"),
                    File(archLibDir, "libnet.so"),
                    File(rtLibDir, "libnio.so"),
                    File(archLibDir, "libnio.so"),
                    File(rtLibDir, "libawt.so"),
                    File(archLibDir, "libawt.so"),
                    File(rtLibDir, "libawt_headless.so"),
                    File(archLibDir, "libawt_headless.so"),
                    File(rtLibDir, "libfontmanager.so"),
                    File(archLibDir, "libfontmanager.so")
                )
                essentialLibs.forEach { so ->
                    if (so.exists()) {
                        try {
                            ZLBridge.dlopen(so.absolutePath)
                            LoggerBridge.append("▷ [Pre-dlopen] Loaded ${so.name}")
                        } catch (e: Throwable) {
                            LoggerBridge.append("▷ [Pre-dlopen Warning] ${so.name}: ${e.message}")
                        }
                    }
                }

                // Pre-dlopen all remaining runtime shared libraries (.so) as Zalith does
                runtimeHome.walkTopDown().filter { it.isFile && it.name.endsWith(".so") }.forEach { so ->
                    try {
                        ZLBridge.dlopen(so.absolutePath)
                    } catch (_: Throwable) {}
                }

                // Pre-dlopen OpenAL engine (Matches Zalith dlopenEngine)
                val openalFile = File(nativeLibDir, "libopenal.so")
                if (openalFile.exists()) {
                    try {
                        ZLBridge.dlopen(openalFile.absolutePath)
                        LoggerBridge.append("▷ [Pre-dlopen] Loaded libopenal.so")
                    } catch (_: Throwable) {}
                }

                // Pre-dlopen internal renderer ONLY if NOT a plugin (prevents overriding plugin libraries with internal ones)
                if (!targetRenderer.isPlugin) {
                    try {
                        val rendererFile = File(nativeLibDir, rendererSoName)
                        if (rendererFile.exists()) {
                            ZLBridge.dlopen(rendererFile.absolutePath)
                            LoggerBridge.append("▷ [Pre-dlopen] Loaded internal renderer: $rendererSoName")
                        } else {
                            LoggerBridge.append("▷ [Pre-dlopen Warning] Renderer file not found: $rendererSoName")
                        }
                    } catch (e: Throwable) {
                        LoggerBridge.append("▷ [Pre-dlopen Warning] Renderer $rendererSoName: ${e.message}")
                    }

                    if (!eglPath.isNullOrBlank() && eglPath != glLibPath) {
                        val eglFile = File(eglPath)
                        if (eglFile.exists()) {
                            try {
                                ZLBridge.dlopen(eglFile.absolutePath)
                                LoggerBridge.append("▷ [Pre-dlopen] Loaded internal EGL: ${eglFile.name}")
                            } catch (_: Throwable) {}
                        }
                    }
                }

                // Pre-dlopen SPIRV-Cross if available
                val spirvLibFile = File(nativeLibDir, "libspirv-cross-c-shared.so")
                if (spirvLibFile.exists()) {
                    try {
                        ZLBridge.dlopen(spirvLibFile.absolutePath)
                        LoggerBridge.append("▷ [Pre-dlopen] Loaded libspirv-cross-c-shared.so")
                    } catch (e: Throwable) {
                        LoggerBridge.append("▷ [Pre-dlopen Warning] spirv: ${e.message}")
                    }
                }

                // Pre-dlopen LWJGL natives
                if (lwjglNativesDirPath.isNotBlank()) {
                    val lwjglDir = File(lwjglNativesDirPath)
                    listOf("liblwjgl.so", "liblwjgl_opengl.so", "liblwjgl_stb.so", "liblwjgl_tinyfd.so", "libfreetype.so", "libshaderc.so", "liblwjgl_vma.so", "libspirv-cross.so").forEach { libName ->
                        val so = File(lwjglDir, libName)
                        if (so.exists()) {
                            try {
                                ZLBridge.dlopen(so.absolutePath)
                                LoggerBridge.append("▷ [Pre-dlopen] Loaded ${so.name}")
                            } catch (e: Throwable) {
                                LoggerBridge.append("▷ [Pre-dlopen Warning] ${so.name}: ${e.message}")
                            }
                        }
                    }
                }

                ZLBridge.setupExitMethod(this@GameActivity)
                ZLBridge.initializeGameExitHook()
                ZLBridge.chdir(gameDir.absolutePath)

                // Build JVM Launch Arguments
                val effectiveLwjglDir = if (lwjglNativesDirPath.isNotBlank()) lwjglNativesDirPath else nativeLibDir
                val freetypeLib = if (lwjglNativesDirPath.isNotBlank() && File(lwjglNativesDirPath, "libfreetype.so").exists()) {
                    File(lwjglNativesDirPath, "libfreetype.so").absolutePath
                } else {
                    "$nativeLibDir/libfreetype.so"
                }

                val spirvLibPath = if (lwjglNativesDirPath.isNotBlank() && File(lwjglNativesDirPath, "libspirv-cross.so").exists()) {
                    File(lwjglNativesDirPath, "libspirv-cross.so").absolutePath
                } else if (File(nativeLibDir, "libspirv-cross-c-shared.so").exists()) {
                    File(nativeLibDir, "libspirv-cross-c-shared.so").absolutePath
                } else {
                    "libspirv-cross-c-shared.so"
                }

                // Pastikan permission executable (0755) pada bin/java dan runtime native libraries
                // (hanya dikerjakan penuh sekali per instalasi runtime; selanjutnya cukup cek satu penanda)
                com.aistudio.novacraft.launcher.core.launch.JavaRuntimeManager.ensureExecutablePermissionsOnce(runtimeHome)

                val jvmArgs = mutableListOf<String>()
                jvmArgs.add("${runtimeHome.absolutePath}/bin/java")
                jvmArgs.add("-Djava.home=${runtimeHome.absolutePath}")
                jvmArgs.add("-Djava.io.tmpdir=${cacheDir.absolutePath}")
                jvmArgs.add("-Djava.library.path=$gameLdLibraryPath")
                jvmArgs.add("-Dorg.lwjgl.librarypath=$effectiveLwjglDir")
                jvmArgs.add("-Dorg.lwjgl.opengl.libname=$glLibPath")
                jvmArgs.add("-Dorg.lwjgl.openal.libname=$nativeLibDir/libopenal.so")
                jvmArgs.add("-Dorg.lwjgl.vulkan.libname=libvulkan.so")
                jvmArgs.add("-Dorg.lwjgl.freetype.libname=$freetypeLib")
                jvmArgs.add("-Dorg.lwjgl.spvc.libname=$spirvLibPath")
                jvmArgs.add("-Dorg.lwjgl.spvc.defaultname=spirv-cross-c-shared")
                jvmArgs.add("-Dorg.lwjgl.system.allocator=system")
                jvmArgs.add("-Djna.boot.library.path=$nativeLibDir")
                jvmArgs.add("-Dglfwstub.windowWidth=$targetWidth")
                jvmArgs.add("-Dglfwstub.windowHeight=$targetHeight")
                jvmArgs.add("-Dglfwstub.initEgl=false")
                jvmArgs.add("-Dos.name=Linux")
                jvmArgs.add("-Dos.version=Android-${Build.VERSION.RELEASE}")
                jvmArgs.add("-Duser.home=${gameDir.parentFile?.absolutePath ?: gameDir.absolutePath}")
                jvmArgs.add("-Dnet.minecraft.clientmodname=NovaCraft")
                jvmArgs.add("-Dlog4j2.formatMsgNoLookups=true")
                jvmArgs.add("-Djava.rmi.server.useCodebaseOnly=true")
                jvmArgs.add("-Dcom.sun.jndi.rmi.object.trustURLCodebase=false")
                jvmArgs.add("-Dcom.sun.jndi.cosnaming.object.trustURLCodebase=false")
                jvmArgs.add("-Dfml.earlyprogresswindow=false")
                jvmArgs.add("-Dfml.ignoreInvalidMinecraftCertificates=true")
                jvmArgs.add("-Dfml.ignorePatchDiscrepancies=true")
                jvmArgs.add("-Dloader.disable_forked_guis=true")
                jvmArgs.add("-Djdk.lang.Process.launchMechanism=FORK")
                jvmArgs.add("-Dsodium.checks.issue2561=false")
                jvmArgs.add("-Dfile.encoding=UTF-8")
                jvmArgs.add("-Dsun.stdout.encoding=UTF-8")
                jvmArgs.add("-Dsun.stderr.encoding=UTF-8")

                // High-performance Android Network & Netty Socket stabilization (Direct match with Zalith)
                jvmArgs.add("-Dext.net.resolvPath=${resolvConf.absolutePath}")
                jvmArgs.add("-Djava.net.preferIPv4Stack=true")
                jvmArgs.add("-Djava.net.preferIPv6Addresses=false")
                jvmArgs.add("-Dio.netty.native.workdir=${cacheDir.absolutePath}")
                jvmArgs.add("-Djna.tmpdir=${cacheDir.absolutePath}")
                jvmArgs.add("-Dorg.lwjgl.system.SharedLibraryExtractPath=${cacheDir.absolutePath}")
                jvmArgs.add("-Dio.netty.tryReflectionSetAccessible=true")

                // Properti sistem dari plugin native library (mis. -Dorg.sqlite.lib.path=...). Sudah disaring
                // di NativePluginManager; argumen JVM kustom pengguna di bawah tetap bisa menimpanya.
                nativePlan.jvmArgs.forEach { jvmArgs.add(it) }

                jvmArgs.add("-XX:ActiveProcessorCount=${Runtime.getRuntime().availableProcessors()}")
                // Data kinerja JVM (untuk jstat/jcmd) tidak terpakai di HP: mematikannya menghapus tugas
                // pencuplik yang bangun 20 kali per detik di thread watcher JVM. Tidak mengubah isi game.
                jvmArgs.add("-XX:-UsePerfData")

                // Satu jalur untuk semua HP (tidak ada profil per model): heap awal disamakan dengan heap
                // maksimum (GC tidak perlu membesarkan heap sedikit demi sedikit) dan, di HP RAM kecil,
                // generasi muda G1 dibatasi supaya RAM tidak membengkak. Java 8 tidak diubah.
                val deviceTotalRamMb = SettingsManager.getTotalDeviceMemoryMb(this)
                var heapMaxMb = activeSettings.ramMb
                var heapInitMb = DeviceProfiles.generalInitialHeapMb(activeSettings.initialHeapMb, heapMaxMb, isJava8)
                var profileFlags: List<String> = DeviceProfiles.generalJvmFlags(
                    deviceTotalRamMb, isJava8, activeSettings.customJvmArgs, activeSettings.generalGcMode
                )
                LoggerBridge.append("▷ [Perangkat] ${DeviceProfiles.describeDevice()}")
                if (!android.os.Process.is64Bit()) {
                    // Proses 32-bit hanya punya ruang alamat 3-4 GB, dan sebagian sudah terpakai
                    // pustaka dan driver grafis. Heap lebih besar dari ini membuat JVM gagal start
                    // ("Could not reserve enough space"). Tidak berlaku di APK 64-bit.
                    val heapCap32Mb = 1280
                    if (heapMaxMb > heapCap32Mb) {
                        LoggerBridge.append("▷ [JVM] Proses 32-bit: heap maksimum dibatasi $heapCap32Mb MB (diminta $heapMaxMb MB)")
                        heapMaxMb = heapCap32Mb
                    }
                    if (heapInitMb > heapMaxMb) heapInitMb = heapMaxMb
                }
                // Heap hanya sebagian memori game (JIT, driver grafis, dan tekstur memakai memori di luar heap).
                // Heap di atas ~60% RAM HP hampir pasti membuat Android mematikan game (kode 137), jadi nilai
                // tersimpan yang lebih besar dari batas slider Pengaturan dibatasi di sini. Lantai 1024 MB sama dengan slider.
                val heapCapByDeviceMb = maxOf((deviceTotalRamMb * 0.6f).toInt(), 1024)
                if (heapMaxMb > heapCapByDeviceMb) {
                    LoggerBridge.append("▷ [JVM] RAM game diminta $heapMaxMb MB, RAM HP $deviceTotalRamMb MB: heap dibatasi $heapCapByDeviceMb MB")
                    heapMaxMb = heapCapByDeviceMb
                    if (heapInitMb > heapMaxMb) heapInitMb = heapMaxMb
                }
                // Mode Serial dengan heap kecil: code cache JIT dikecilkan (cara Battly). Heap sudah final di sini.
                profileFlags = profileFlags + DeviceProfiles.serialCodeCacheFlags(
                    DeviceProfiles.flagsSelectSerial(profileFlags), heapMaxMb, activeSettings.customJvmArgs
                )
                LoggerBridge.append(
                    "▷ [JVM] Flag GC: ${if (profileFlags.isEmpty()) "(bawaan JVM)" else profileFlags.joinToString(" ")}"
                )
                jvmArgs.add("-Xms${heapInitMb}M")
                jvmArgs.add("-Xmx${heapMaxMb}M")
                profileFlags.forEach { jvmArgs.add(it) }
                // Log GC untuk pemantau kinerja (Java 9 ke atas; Java 8 tidak mengenal -Xlog). Berkas
                // lama dihapus dulu supaya tiap sesi mulai dari nol.
                val gcLogFile: File? = if (isJava8) null else File(cacheDir, "amber-gc.log")
                if (gcLogFile != null) {
                    gcLogFile.delete()
                    jvmArgs.add("-Xlog:gc:file=${gcLogFile.absolutePath}:uptime:filecount=0")
                }
                LoggerBridge.append(
                    "▷ [JVM] Heap awal $heapInitMb MB, maksimum $heapMaxMb MB | " +
                        "RAM HP total $deviceTotalRamMb MB, " +
                        "tersedia ${SettingsManager.getAvailableDeviceMemoryMb(this)} MB"
                )
                if (activeSettings.customJvmArgs.isNotBlank()) {
                    activeSettings.customJvmArgs.split(" ")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .forEach { jvmArgs.add(it) }
                }

                jvmArgs.add("-Dnovacraft.jar.sandbox=${activeSettings.jarSandboxEnabled}")
                if (activeSettings.jarSandboxEnabled) {
                    val javaMajorVersion = runCatching {
                        val rel = File(runtimeHome, "release")
                        if (rel.isFile) {
                            rel.useLines { lines ->
                                lines.firstOrNull { it.startsWith("JAVA_VERSION=") }
                                    ?.substringAfter('=')?.trim()?.trim('"', '\'')
                                    ?.let { v ->
                                        if (v.startsWith("1.")) v.removePrefix("1.").substringBefore('.').substringBefore('_').toIntOrNull()
                                        else v.substringBefore('.').substringBefore('-').toIntOrNull()
                                    }
                            }
                        } else null
                    }.getOrNull() ?: when {
                        isJava8 -> 8
                        runtimeName.contains("25") -> 25
                        runtimeName.contains("21") -> 21
                        else -> 17
                    }
                    val hasSecFlag = activeSettings.customJvmArgs.contains("java.security.manager")
                    if (!hasSecFlag) {
                        when {
                            javaMajorVersion <= 8 -> jvmArgs.add("-Djava.security.manager")
                            javaMajorVersion in 9..23 -> jvmArgs.add("-Djava.security.manager=allow")
                            else -> LoggerBridge.append("▷ [Sandbox] Java $javaMajorVersion menonaktifkan SecurityManager JVM; mode sandbox peluncur tetap aktif")
                        }
                    }
                }

                // Hanya salinan pribadi yang diekstrak dari aset aplikasi yang dipakai sebagai agent. Berkas di
                // penyimpanan bersama bisa diganti aplikasi lain, dan agent berjalan dengan hak penuh di dalam game.
                val patcherJar = File(filesDir, "components/launcher/MioLibPatcher.jar")
                if (patcherJar.exists()) {
                    jvmArgs.add("-javaagent:${patcherJar.absolutePath}")
                    LoggerBridge.append("▷ Enabled MioLibPatcher bytecode agent")
                }

                // Enable authlib-injector if using Ely.by or custom auth server
                val injectorPath = authlibInjectorPath
                if (!injectorPath.isNullOrBlank() && !authlibUrl.isNullOrBlank()) {
                    val authlibJar = File(injectorPath)
                    if (authlibJar.exists()) {
                        jvmArgs.add("-javaagent:${authlibJar.absolutePath}=$authlibUrl")
                        jvmArgs.add("-Dauthlibinjector.side=client")
                        LoggerBridge.append("▷ Enabled authlib-injector for external auth ($authlibUrl)")
                    }
                }

                if (!isJava8) {
                    jvmArgs.add("--add-opens=java.base/java.lang=ALL-UNNAMED")
                    jvmArgs.add("--add-opens=java.base/java.lang.reflect=ALL-UNNAMED")
                    jvmArgs.add("--add-opens=java.base/java.util=ALL-UNNAMED")
                    jvmArgs.add("--add-opens=java.base/java.text=ALL-UNNAMED")
                    jvmArgs.add("--add-opens=java.base/java.net=ALL-UNNAMED")
                    jvmArgs.add("--add-opens=jdk.naming.dns/com.sun.jndi.dns=ALL-UNNAMED")
                    jvmArgs.add("--add-exports=jdk.naming.dns/com.sun.jndi.dns=ALL-UNNAMED")
                    jvmArgs.add("--add-opens=java.desktop/sun.awt=ALL-UNNAMED")
                    jvmArgs.add("--add-opens=java.desktop/java.awt=ALL-UNNAMED")
                    jvmArgs.add("--add-opens=java.desktop/sun.font=ALL-UNNAMED")
                    jvmArgs.add("--add-opens=java.desktop/sun.java2d=ALL-UNNAMED")

                    if (mainClass.contains(".")) {
                        val pkg = mainClass.substring(0, mainClass.lastIndexOf("."))
                        jvmArgs.add("--add-exports")
                        jvmArgs.add("$pkg/$pkg=ALL-UNNAMED")
                    }
                }

                // Sanitasi classpath: buang duplikat dan pastikan tidak ada bentrok versi ASM jika menjalankan Fabric/Knot
                val effectiveClasspath = if (mainClass.contains("knot") || mainClass.contains("fabric")) {
                    val entries = classpath.split(File.pathSeparator).filter { it.isNotBlank() }
                    val hasModernAsm = entries.any { it.contains("org/ow2/asm") && !it.contains("/9.6/") }
                    if (hasModernAsm) {
                        entries.filterNot { it.contains("org/ow2/asm") && it.contains("/9.6/") }
                    } else {
                        entries
                    }.distinct().joinToString(File.pathSeparator)
                } else {
                    classpath.split(File.pathSeparator).filter { it.isNotBlank() }.distinct().joinToString(File.pathSeparator)
                }

                // Argumen JVM mod loader (Forge 1.17+: module path, --add-opens, -DignoreList, dst.)
                extraJvmArgs.forEach { jvmArgs.add(it) }

                jvmArgs.add("-cp")
                jvmArgs.add(effectiveClasspath)
                if (useWrapper) {
                    jvmArgs.add("mio.Wrapper")
                }
                jvmArgs.add(mainClass)
                jvmArgs.add("--username")
                jvmArgs.add(username)
                jvmArgs.add("--version")
                jvmArgs.add(mcVersion)
                jvmArgs.add("--gameDir")
                jvmArgs.add(gameDir.absolutePath)
                jvmArgs.add("--assetsDir")
                jvmArgs.add(assetsDir.absolutePath)
                jvmArgs.add("--assetIndex")
                jvmArgs.add(assetIndexId)
                jvmArgs.add("--uuid")
                jvmArgs.add(uuid)
                jvmArgs.add("--accessToken")
                jvmArgs.add(accessToken)
                jvmArgs.add("--userType")
                jvmArgs.add(userType)
                jvmArgs.add("--versionType")
                jvmArgs.add("release")
                // Argumen game mod loader (Forge: --launchTarget, --fml.*)
                extraGameArgs.forEach { jvmArgs.add(it) }

                LoggerBridge.appendTitle("JVM Launch Command")
                // Nilai setelah --accessToken tidak dicatat. Token ada di baris tersendiri, sehingga penyamar log
                // (yang bekerja per baris) tidak mengenalinya bila bentuknya bukan JWT.
                var maskNextArg = false
                jvmArgs.forEach { arg ->
                    LoggerBridge.append(if (maskNextArg) "▷ ***" else "▷ $arg")
                    maskNextArg = arg == "--accessToken"
                }

                CrashManager.onGameSessionStarted(
                    context = this,
                    instanceName = instanceName,
                    mcVersion = mcVersion,
                    rendererId = activeSettings.selectedRenderer,
                    gameDirPath = gameDir.absolutePath,
                    loader = loader,
                    loaderVersion = loaderVersion,
                    installedMods = installedMods,
                    graphicsInfo = graphicsSummary
                )
                appendUiLog("[Amber Engine] Memulai eksekusi VMLauncher.launchJVM()...")
                // Pemantau hanya mencatat ke log tiap 30 detik; tidak mengubah apa pun pada game.
                val perfMonitor = GamePerfMonitor(this, gcLogFile)
                perfMonitor.start()
                val exitCode = try {
                    VMLauncher.launchJVM(jvmArgs.toTypedArray())
                } finally {
                    perfMonitor.stop()
                }
                appendUiLog("[Amber Engine] JVM selesai dengan kode keluar: $exitCode")
                runOnUiThread {
                    handleGameExit(exitCode, false)
                }
            } catch (e: Throwable) {
                LoggerBridge.append("[ERROR JVM Launch] ${e.message}")
                e.printStackTrace()
                runOnUiThread {
                    handleGameExit(-1, false, e.stackTraceToString())
                }
            }
        }
    }
}

enum class FpsMode {
    NORMAL,    // Siklus 3 / Awal: Mengikuti visibilitas GUI (isControlVisible), log tertutup
    PINNED,    // Siklus 1: FPS di-pin, tetap tampil meski GUI di-hide, log tertutup
    SHOW_LOG   // Siklus 2: Memunculkan log in-game (isConsoleVisible = true)
}

/** Tombol panah D-pad (atas/bawah/kiri/kanan), dipakai untuk menyaring tombol panah sintetis. */
private fun KeyEvent.isDpadArrow(): Boolean = when (keyCode) {
    KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
    KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> true
    else -> false
}

/**
 * Info kondisi HP yang ditampilkan di samping angka FPS supaya penyebab FPS rendah terbaca
 * langsung di layar: "sisa RAM xxxMB" (memori kosong HP; kalau tinggal sedikit sistem mulai
 * menukar memori / menutup aplikasi) dan "PANAS" (prosesor diperlambat karena HP panas).
 * Pasangan hasil: (teks, peringatan?). Teks kosong = info tidak tersedia.
 */
private fun buildPerfHint(context: Context): Pair<String, Boolean> {
    var text = ""
    var warn = false
    try {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
        if (am != null) {
            val mi = android.app.ActivityManager.MemoryInfo()
            am.getMemoryInfo(mi)
            val availMb = (mi.availMem / (1024L * 1024L)).toInt()
            // Dibulatkan per 50 MB supaya angka tidak berubah-ubah terus
            text = "sisa RAM ${(availMb / 50) * 50}MB"
            if (mi.lowMemory || availMb < 450) warn = true
        }
    } catch (_: Throwable) {
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            if (pm != null && pm.currentThermalStatus >= android.os.PowerManager.THERMAL_STATUS_MODERATE) {
                text = if (text.isEmpty()) "PANAS" else "$text · PANAS"
                warn = true
            }
        } catch (_: Throwable) {
        }
    }
    // Hz layar yang benar-benar aktif: hanya bila "Kunci layar 60 Hz" menyala (selain itu tidak ditampilkan).
    // Dipakai untuk memastikan kunci berlaku; kalau tertulis 90Hz berarti sistem menolaknya.
    try {
        val hz = DeviceProfiles.refreshRateLabel(context, SettingsManager.settings.value.lockRefresh60)
        if (hz.isNotEmpty()) {
            text = if (text.isEmpty()) hz else "$text · $hz"
        }
    } catch (_: Throwable) {
    }
    return Pair(localizeHint(text), warn)
}

/** Menerjemahkan kata di teks kondisi HP bila bahasa bukan Indonesia (HUD digambar langsung, bukan lewat Text). */
private fun localizeHint(text: String): String {
    if (text.isEmpty() || Tr.effective == Tr.ID) return text
    return text.replace("sisa RAM", Tr.t("sisa RAM")).replace("PANAS", Tr.t("PANAS"))
}

@Composable
fun GameScreen(
    instanceName: String,
    mcVersion: String,
    username: String,
    mainClass: String,
    runtimeName: String,
    liveLogs: List<String>,
    loader: String = "vanilla",
    loaderVersion: String? = null,
    isControlVisibleState: MutableState<Boolean>,
    isGameRenderingState: MutableState<Boolean>,
    touchSinkState: State<TouchSink?>,
    onSurfaceReady: (SurfaceHolder) -> Unit,
    useTextureView: Boolean = false,
    onTextureViewReady: (TextureView) -> Unit = {},
    onResolutionChange: (Int) -> Unit = {},
    onToggleRecord: () -> Unit = {},
    guiScaleProvider: () -> Int = { 0 },
    onExit: () -> Unit
) {
    var isControlVisible by isControlVisibleState
    var isGameRendering by isGameRenderingState
    val showLoadingOverlay = !isGameRendering
    // Tombol layar baru muncul setelah logo Mojang tampil, supaya layar loading bersih. Bila loading
    // sangat lama (game macet), tombol dimunculkan juga agar pemain bisa membuka menu: lihat log atau keluar.
    var loadingTooLong by remember { mutableStateOf(false) }
    LaunchedEffect(isGameRendering) {
        if (!isGameRendering) {
            delay(60_000L)
            loadingTooLong = true
        }
    }
    val controlsAllowed = isGameRendering || loadingTooLong

    var showInGameSettingsDialog by remember { mutableStateOf(false) }
    var isCustomGuiEditorActive by remember { mutableStateOf(false) }
    // Non-null = mod TouchController yang memegang kontrol sentuh (kontrol bawaan disembunyikan).
    val touchSink = touchSinkState.value
    // Lapisan sentuh tertutup menu/editor: jari yang masih tercatat di mod dilepas supaya tidak menggantung.
    LaunchedEffect(touchSink, showInGameSettingsDialog, isCustomGuiEditorActive) {
        if (touchSink != null && (showInGameSettingsDialog || isCustomGuiEditorActive)) touchSink.onCancelAll()
    }
    var isKeyboardRequested by remember { mutableStateOf(false) }
    // Huruf yang dikirim keyboard layar ke game; dibaca bar preview di atas keyboard.
    val typingMirror = remember { TypingMirror() }
    var isConsoleVisible by remember { mutableStateOf(false) }
    var isConsoleExpanded by remember { mutableStateOf(false) }
    var showExitConfirmDialog by remember { mutableStateOf(false) }
    BackHandler(enabled = showLoadingOverlay && !showExitConfirmDialog && !showInGameSettingsDialog) {
        showExitConfirmDialog = true
    }
    // Lapisan launcher menutupi game: klik mouse di atasnya milik lapisan itu, bukan milik game.
    // MenuMouse (di GameActivity) membaca ini; tombol yang masih tertahan dilepas saat lapisan terbuka.
    val overlayCoversGame = showInGameSettingsDialog || showExitConfirmDialog || isCustomGuiEditorActive || showLoadingOverlay
    SideEffect { MenuMouse.overlayOpen = overlayCoversGame }
    LaunchedEffect(overlayCoversGame) {
        if (overlayCoversGame) MenuMouse.reset()
    }
    var currentFps by remember { mutableIntStateOf(0) }
    var fpsMode by remember { mutableStateOf(FpsMode.NORMAL) }
    val context = LocalContext.current

    // Handler siklus 3-klik tombol FPS
    val cycleFpsMode: () -> Unit = {
        when (fpsMode) {
            FpsMode.NORMAL -> {
                fpsMode = FpsMode.PINNED
                isConsoleVisible = false
                Toast.makeText(context, "📌 FPS Di-Pin (Tetap tampil saat GUI disembunyikan)", Toast.LENGTH_SHORT).show()
            }
            FpsMode.PINNED -> {
                fpsMode = FpsMode.SHOW_LOG
                isConsoleVisible = true
                Toast.makeText(context, "📜 Menampilkan Live Log Minecraft", Toast.LENGTH_SHORT).show()
            }
            FpsMode.SHOW_LOG -> {
                fpsMode = FpsMode.NORMAL
                isConsoleVisible = false
                Toast.makeText(context, "FPS Mode Normal (Mengikuti visibilitas GUI)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Info kondisi HP di samping FPS (sisa RAM / PANAS); amber bila kondisinya mepet
    var perfText by remember { mutableStateOf("") }
    var perfWarn by remember { mutableStateOf(false) }

    // Info RAM/panas hanya tampil di teks FPS, jadi hanya dihitung saat teks FPS terlihat.
    val wantPerfHint by rememberUpdatedState(isControlVisible || fpsMode == FpsMode.PINNED)

    // Periodically update FPS counter from native engine (1x per detik: tiap angka baru = satu
    // penggambaran UI, jadi tidak perlu lebih sering dari yang bisa dibaca mata)
    LaunchedEffect(Unit) {
        var tick = 0
        while (isActive) {
            try {
                currentFps = CallbackBridge.getCurrentFps()
            } catch (_: Throwable) {
            }
            if (tick % 3 == 0 && wantPerfHint) {
                // Panggilan ke sistem (memori, suhu) dikerjakan di luar thread utama.
                val hint = withContext(Dispatchers.Default) {
                    try { buildPerfHint(context) } catch (_: Throwable) { Pair("", false) }
                }
                if (hint.first != perfText) perfText = hint.first
                if (hint.second != perfWarn) perfWarn = hint.second
            }
            tick++
            delay(1000)
        }
    }

    val gameCursorMode by ZLBridgeStates.cursorMode.collectAsState()
    // Catatan diagnosis mouse: kursor game berganti antara menu dan bermain (hanya beberapa kali per sesi).
    LaunchedEffect(gameCursorMode) {
        // Game mengunci kursor (mis. klik "Play" lalu dunia dimuat): tombol menu yang tertahan dilepas.
        if (gameCursorMode == CURSOR_DISABLED) MenuMouse.reset()
        MouseDiag.cursorMode(gameCursorMode == CURSOR_DISABLED)
    }
    val launcherSettings by SettingsManager.settings.collectAsState()
    val customButtons by ControlLayoutManager.buttons.collectAsState()
    val mouseControlMode = if (launcherSettings.mouseControlMode == "CLICK") MouseControlMode.CLICK else MouseControlMode.SLIDE

    // Rekam mode "Game saja": perekam menangkap seluruh layar, jadi tombol layar dibuat transparan
    // selama merekam (tetap bisa disentuh, hanya tidak tergambar) supaya tidak ikut terekam.
    val recState by ScreenRecorder.state.collectAsState()
    val ghostControls = recState.recording && !launcherSettings.recordWithControls
    // Tanda mulai/berhenti merekam berupa getar singkat (toast akan ikut terekam).
    val hostView = LocalView.current
    var wasRecording by remember { mutableStateOf(false) }
    LaunchedEffect(recState.recording) {
        if (recState.recording != wasRecording) {
            wasRecording = recState.recording
            hostView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val screenWidth = constraints.maxWidth.toFloat()
        val screenHeight = constraints.maxHeight.toFloat()

        // Hotbar sentuh gaya Bedrock (bila dinyalakan): titik sentuh -> slot 0..8 atau -1.
        val hotbarLookup: ((Offset) -> Int)? = if (launcherSettings.hotbarTouchEnabled) {
            val lookup: (Offset) -> Int = { pos ->
                HotbarGeometry.slotAt(
                    pos.x, pos.y,
                    screenWidth, screenHeight,
                    CallbackBridge.windowWidth, CallbackBridge.windowHeight,
                    guiScaleProvider()
                )
            }
            lookup
        } else {
            null
        }

        var cursorX by remember { mutableFloatStateOf(0f) }
        var cursorY by remember { mutableFloatStateOf(0f) }
        var lastSurfaceTouchY by remember { mutableFloatStateOf(0f) }
        var isMousePressed by remember { mutableStateOf(false) }
        var padCursorShown by remember { mutableStateOf(false) }

        val density = LocalDensity.current
        val imeBottomPx = WindowInsets.ime.getBottom(density)
        val visibleZoneBottomPx = (screenHeight - imeBottomPx).coerceAtLeast(0f)
        val panClearancePx = with(density) { 40.dp.toPx() }
        val targetPanY = if (
            launcherSettings.enableKeyboardPanning &&
            imeBottomPx > 0 &&
            lastSurfaceTouchY > visibleZoneBottomPx
        ) {
            -((lastSurfaceTouchY - visibleZoneBottomPx + panClearancePx).coerceIn(0f, imeBottomPx.toFloat()))
        } else {
            0f
        }
        val animatedPanY by animateFloatAsState(
            targetValue = targetPanY,
            animationSpec = tween(durationMillis = 200),
            label = "keyboard_pan"
        )

        LaunchedEffect(screenWidth, screenHeight) {
            if (cursorX == 0f && screenWidth > 0f) {
                cursorX = screenWidth / 2f
                cursorY = screenHeight / 2f
                val winW = CallbackBridge.windowWidth
                val winH = CallbackBridge.windowHeight
                val targetX = if (screenWidth > 0 && winW > 0) cursorX * (winW.toFloat() / screenWidth) else cursorX
                val targetY = if (screenHeight > 0 && winH > 0) cursorY * (winH.toFloat() / screenHeight) else cursorY
                CallbackBridge.sendCursorPos(targetX, targetY)
            }
        }

        // Controller menggeser kursor menu: posisi tampilan dan posisi di game ikut diperbarui.
        DisposableEffect(screenWidth, screenHeight) {
            GamepadRouter.cursorMover = { dx, dy ->
                if (screenWidth > 0f && screenHeight > 0f) {
                    val nx = (cursorX + dx).coerceIn(0f, screenWidth)
                    val ny = (cursorY + dy).coerceIn(0f, screenHeight)
                    cursorX = nx
                    cursorY = ny
                    padCursorShown = true
                    val winW = CallbackBridge.windowWidth
                    val winH = CallbackBridge.windowHeight
                    CallbackBridge.sendCursorPos(
                        if (winW > 0) nx * (winW.toFloat() / screenWidth) else nx,
                        if (winH > 0) ny * (winH.toFloat() / screenHeight) else ny
                    )
                }
            }
            onDispose { GamepadRouter.cursorMover = null }
        }

        // Điều khiển con quay hồi chuyển (Gyroscope camera rotation)
        DisposableEffect(launcherSettings.gyroEnabled, launcherSettings.gyroSensitivityPercent) {
            if (!launcherSettings.gyroEnabled) {
                onDispose { }
            } else {
                val sensorMgr = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
                val gyro = sensorMgr?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
                if (sensorMgr == null || gyro == null) {
                    onDispose { }
                } else {
                    val listener = object : SensorEventListener {
                        private var lastNs = 0L
                        override fun onSensorChanged(event: SensorEvent) {
                            if (ZLBridgeStates.cursorMode.value != CURSOR_DISABLED) {
                                lastNs = event.timestamp
                                return
                            }
                            if (lastNs != 0L) {
                                val dt = ((event.timestamp - lastNs) / 1_000_000_000f).coerceIn(0f, 0.08f)
                                val gain = 95f * (launcherSettings.gyroSensitivityPercent.coerceIn(20, 300) / 100f)
                                val dx = -event.values[1] * gain * dt
                                val dy = -event.values[0] * gain * dt
                                if (kotlin.math.abs(dx) > 0.05f || kotlin.math.abs(dy) > 0.05f) {
                                    CallbackBridge.sendCursorDelta(dx, dy)
                                }
                            }
                            lastNs = event.timestamp
                        }
                        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                    }
                    sensorMgr.registerListener(listener, gyro, SensorManager.SENSOR_DELAY_GAME)
                    onDispose { sensorMgr.unregisterListener(listener) }
                }
            }
        }

        // 1. OpenGL/Vulkan Surface View (Pure rendering surface)
        AndroidView(
            factory = { ctx ->
                if (useTextureView) {
                    TextureView(ctx).apply {
                        isOpaque = true
                        onTextureViewReady(this)
                    }
                } else {
                    SurfaceView(ctx).apply {
                        onSurfaceReady(holder)
                    }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationY = animatedPanY }
        )

        if (isCustomGuiEditorActive) {
            // Live In-Game Custom GUI Editor (Overlay di atas SurfaceView game dengan latar transparan blur)
            CustomGuiEditorScreen(
                isIngame = true,
                onNavigateBack = {
                    isCustomGuiEditorActive = false
                }
            )
        } else {
            // Mouse fisik sedang dipakai (dari event). Penunjuk virtual disembunyikan; yang terlihat
            // adalah penunjuk sistem Android.
            var isPhysicalMouseMode by remember {
                mutableStateOf(PhysicalMouseChecker.physicalMouseConnected)
            }

        // 2. Zalith-Style Touchpad & Input Controller Layer
        SwitchableMouseLayout(
            modifier = Modifier.fillMaxSize(),
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            cursorMode = gameCursorMode,
            controlMode = mouseControlMode,
            cursorPosition = { Offset(cursorX, cursorY) },
            cursorSensitivity = launcherSettings.cursorSensitivity / 100f,
            touchAttackEnabled = launcherSettings.touchAttackEnabled,
            disableGestures = launcherSettings.disableGestures,
            longPressDelayMs = launcherSettings.longPressDelayMs.toLong(),
            disableDoubleTapOffhand = launcherSettings.disableDoubleTapOffhand,
            // Lapisan launcher di atas game memakai penunjuk sistem; tanpa lapisan itu, mouse ditangkap.
            // Pilihan "Pakai kursor sistem Android" mematikan penangkapan di menu: yang terlihat penunjuk sistem.
            menuCaptureAllowed = !overlayCoversGame && !launcherSettings.physicalMouseSystemPointer,
            physicalCursorSpeed = launcherSettings.physicalMouseCursorSpeed.coerceIn(10, 400) / 100f,
            physicalCameraSpeed = launcherSettings.physicalMouseCameraSpeed.coerceIn(10, 400) / 100f,
            hotbarSlotAt = hotbarLookup,
            touchSink = touchSink,
            onHotbarSelect = { slot ->
                val key = LwjglGlfwKeycode.GLFW_KEY_1 + slot
                CallbackBridge.sendKeyPress(key, true)
                CallbackBridge.sendKeyPress(key, false)
                hostView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            },
            onCursorPositionChange = { newPos ->
                cursorX = newPos.x
                cursorY = newPos.y
            },
            onPhysicalMouseModeChange = { isPhysical ->
                isPhysicalMouseMode = isPhysical
            },
            onMouse = {
                isPhysicalMouseMode = true
                if (isControlVisible) {
                    isControlVisible = false
                    try {
                        LoggerBridge.append("▷ [Kontrol] Tombol layar disembunyikan otomatis: mouse terdeteksi di lapisan sentuh")
                    } catch (_: Throwable) {
                    }
                }
            },
            onTouch = {
                isPhysicalMouseMode = false
                padCursorShown = false
            },
            onTouchDown = { pos ->
                lastSurfaceTouchY = pos.y
            },
            onTap = { pos ->
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
            onLongPress = {
                isMousePressed = true
                CallbackBridge.putMouseEvent(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT, true)
            },
            onLongPressEnd = {
                isMousePressed = false
                CallbackBridge.putMouseEvent(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT, false)
            },
            onCapturedMove = { delta ->
                val sens = launcherSettings.captureSensitivity / 100f
                CallbackBridge.sendCursorDelta(delta.x * sens, delta.y * sens)
            }
        )

        // 2b. Game Loading Screen Overlay (NovaCraft Java Obsidian Forge)
        val rendererLabel = remember(launcherSettings.selectedRenderer) {
            NuxRendererRegistry.findRendererById(launcherSettings.selectedRenderer).displayName
        }
        GameLoadingOverlay(
            visible = showLoadingOverlay,
            instanceName = instanceName,
            // Daftar log hanya dibaca saat layar loading tampil; selebihnya layar tidak ikut
            // digambar ulang setiap ada log baru.
            latestLog = if (showLoadingOverlay) (liveLogs.lastOrNull() ?: "") else "",
            // X: batal dan keluar (jalur yang sama dengan "Keluar dari game"), tanpa konfirmasi karena
            // belum ada dunia yang terbuka. Disembunyikan begitu tombol layar muncul (setelah 60 detik).
            onClose = onExit,
            modifier = Modifier.fillMaxSize(),
            showClose = !controlsAllowed,
            mcVersion = mcVersion,
            loader = loader,
            loaderVersion = loaderVersion,
            runtimeName = runtimeName,
            rendererName = rendererLabel,
            logs = if (showLoadingOverlay) liveLogs else emptyList()
        )

        // Hidden IME soft keyboard layer
        if (isKeyboardRequested) {
            HidableInputLayout(
                onClose = {
                    isKeyboardRequested = false
                },
                mirror = typingMirror
            )
        }

        // 3. Kursor menu milik launcher. Terlihat di menu untuk jari/controller, dan untuk mouse fisik yang sedang
        // ditangkap (penunjuk sistem Android hilang selama capture). Tanpa capture, mouse memakai penunjuk sistem.
        val launcherMouseOn = isPhysicalMouseMode && !overlayCoversGame && LauncherMouse.captureHeld.value
        val shouldShowPointer = if (launcherMouseOn) {
            gameCursorMode == CURSOR_ENABLED
        } else if (padCursorShown) {
            // Kursor digerakkan controller: panah harus terlihat walau mode ketuk menyembunyikannya.
            gameCursorMode == CURSOR_ENABLED
        } else if (launcherSettings.startWithVirtualMouse && !isPhysicalMouseMode) {
            gameCursorMode == CURSOR_ENABLED
        } else if (mouseControlMode == MouseControlMode.CLICK && launcherSettings.hideMouseInClickMode) {
            false
        } else if (isPhysicalMouseMode) {
            // Mouse fisik tanpa capture (cadangan): penunjuk sistem Android yang terlihat, bukan panah launcher.
            false
        } else {
            gameCursorMode == CURSOR_ENABLED
        }

        // Posisi kursor dibaca hanya saat menggeser lapisan kursor (lihat VirtualCursorPointer),
        // jadi menggerakkan kursor tidak menyusun ulang layar game.
        if (shouldShowPointer) {
            val effectiveCursorDp = (launcherSettings.mouseSizeDp *
                (launcherSettings.virtualMouseSizePercent.coerceIn(50, 200) / 100f))
                .roundToInt()
                .coerceIn(12, 72)
            VirtualCursorPointer(
                x = { cursorX },
                y = { cursorY + animatedPanY },
                isPressed = { isMousePressed },
                cursorSizeDp = effectiveCursorDp,
                hotspots = launcherSettings.pointerHotspots
            )
        }

        // 3. Panel log mesin (hanya saat dibuka; daftar log dibaca di dalam panelnya saja)
        if (isConsoleVisible) {
            GameConsolePanel(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
                fps = { currentFps },
                logs = liveLogs,
                expanded = isConsoleExpanded,
                onToggleExpanded = { isConsoleExpanded = !isConsoleExpanded },
                onFpsClick = { showInGameSettingsDialog = true },
                onClose = {
                    isConsoleVisible = false
                    fpsMode = FpsMode.NORMAL
                }
            )
        }

        // 4. Tombol layar (gaya dan logikanya ada di ui/control/ControlOverlay.kt). Dibungkus Box
        // penuh layar agar bisa dibuat transparan (alpha 0, tetap menerima sentuhan) saat merekam
        // "Game saja". Box tanpa penangan sentuhan tidak menghalangi sentuhan ke lapisan di bawahnya.
        if (controlsAllowed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (ghostControls) Modifier.graphicsLayer { alpha = 0f } else Modifier)
            ) {
                GameControlsLayer(
                    buttons = customButtons,
                    screenWidth = screenWidth,
                    screenHeight = screenHeight,
                    controlsVisible = isControlVisible,
                    fpsMode = fpsMode,
                    consoleVisible = isConsoleVisible,
                    keyboardActive = isKeyboardRequested,
                    fps = { currentFps },
                    perfText = { perfText },
                    perfWarn = { perfWarn },
                    onFpsClick = { showInGameSettingsDialog = true },
                    onToggleKeyboard = { isKeyboardRequested = !isKeyboardRequested },
                    onToggleControls = { isControlVisible = !isControlVisible },
                    onOpenMenu = { showInGameSettingsDialog = true },
                    inMenu = gameCursorMode != CURSOR_DISABLED,
                    buttonWidthScale = launcherSettings.controlButtonWidthPercent.coerceIn(50, 200) / 100f,
                    buttonOpacityScale = launcherSettings.controlButtonOpacityPercent.coerceIn(10, 100) / 100f,
                    uppercaseLabels = launcherSettings.uppercaseButtonLabels
                )
            }
        }

        // Preview ketikan di atas keyboard layar: di atas tombol kontrol supaya tidak tertutup, di bawah dialog.
        if (isKeyboardRequested && launcherSettings.typingPreview) {
            TypingPreviewBar(typingMirror)
        }

        // Konfirmasi keluar
        if (showExitConfirmDialog) {
            ExitConfirmDialog(
                onCancel = { showExitConfirmDialog = false },
                onConfirm = {
                    showExitConfirmDialog = false
                    onExit()
                }
            )
        }

        // Menu dalam game (ketuk gerigi atau angka FPS untuk membuka; keluar lewat tombol merah di dalamnya)
        GameMenuSheet(
            visible = showInGameSettingsDialog,
            onDismissRequest = { showInGameSettingsDialog = false },
            fps = { currentFps },
            instanceName = instanceName,
            mcVersion = mcVersion,
            currentResolutionRatio = launcherSettings.resolutionRatio,
            onResolutionChange = { newRatio ->
                onResolutionChange(newRatio)
            },
            fpsMode = fpsMode,
            onFpsModeChange = { newMode ->
                fpsMode = newMode
            },
            onOpenConsoleLog = {
                fpsMode = FpsMode.SHOW_LOG
                isConsoleVisible = true
            },
            cursorSensitivity = launcherSettings.cursorSensitivity,
            onCursorSensitivityChange = { newSens ->
                SettingsManager.updateSettings(
                    context,
                    launcherSettings.copy(cursorSensitivity = newSens)
                )
            },
            captureSensitivity = launcherSettings.captureSensitivity,
            onCaptureSensitivityChange = { newSens ->
                SettingsManager.updateSettings(
                    context,
                    launcherSettings.copy(captureSensitivity = newSens)
                )
            },
            mouseControlMode = mouseControlMode,
            onMouseControlModeChange = { newMode ->
                SettingsManager.updateSettings(
                    context,
                    launcherSettings.copy(mouseControlMode = if (newMode == MouseControlMode.CLICK) "CLICK" else "SLIDE")
                )
            },
            buttonLookDrag = launcherSettings.buttonLookDrag,
            onButtonLookDragChange = { on ->
                SettingsManager.updateSettings(
                    context,
                    launcherSettings.copy(buttonLookDrag = on)
                )
            },
            isControlVisible = isControlVisible,
            onToggleControlVisibility = {
                isControlVisible = !isControlVisible
            },
            onRequestKeyboard = {
                isKeyboardRequested = true
            },
            onSendKeycode = { keycode ->
                CallbackBridge.sendKeyPress(keycode, true)
                CallbackBridge.sendKeyPress(keycode, false)
            },
            onExitRequest = {
                showExitConfirmDialog = true
            },
            onOpenCustomGui = {
                showInGameSettingsDialog = false
                isCustomGuiEditorActive = true
            },
            onToggleRecord = onToggleRecord,
            recordWithControls = launcherSettings.recordWithControls,
            onRecordWithControlsChange = { withControls ->
                SettingsManager.updateSettings(
                    context,
                    launcherSettings.copy(recordWithControls = withControls)
                )
            }
        )
        }
    }
}

/**
 * Kursor menu game, dalam bentuk yang mengikuti kursor game (panah, tangan, kolom teks, dst.). Posisinya diberikan
 * lewat lambda dan hanya dibaca saat lapisan digeser (graphicsLayer): menggerakkan kursor tidak menyusun ulang
 * dan tidak menggambar ulang gambarnya. Hanya pergantian bentuk atau gambar yang menyusun ulang fungsi ini.
 * Gambar bawaan berupa path vektor kecil, gambar kustom didecode sekali dan disimpan (lihat [PointerStyle]).
 * Hotspot (titik ujung kursor) diletakkan tepat di posisi kursor. Belum ada posisi (0, 0) = tidak terlihat.
 */
@Composable
fun VirtualCursorPointer(
    x: () -> Float,
    y: () -> Float,
    isPressed: () -> Boolean = { false },
    cursorSizeDp: Int = 24,
    hotspots: Map<String, Int> = emptyMap(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val d = LocalDensity.current.density
    val shape by ZLBridgeStates.cursorShape.collectAsStateWithLifecycle()
    val glyph = PointerStyle.glyphFor(shape, d)
    val custom = PointerStyle.bitmap(context, shape)
    val hotX = PointerStyle.hotspotX(hotspots, shape) / 100f
    val hotY = PointerStyle.hotspotY(hotspots, shape) / 100f
    val sizePx = cursorSizeDp * d

    Box(
        modifier = modifier
            .size(cursorSizeDp.dp)
            .graphicsLayer {
                val px = x()
                val py = y()
                translationX = px - hotX * sizePx
                translationY = py - hotY * sizePx
                alpha = if (px > 0f && py > 0f) 1f else 0f
            }
            .drawBehind { drawPointer(glyph, custom, d, sizePx, hotX, hotY, isPressed()) }
    )
}

// ----------------------------------------------------------------------------
// Section: ui/activities/ErrorActivity.kt
// ----------------------------------------------------------------------------

/**
 * Halaman crash mandiri: dibuka saat game berhenti mendadak (dari proses game) atau saat launcher
 * sendiri mengalami kesalahan tak terduga. Tampilannya sama dengan dialog crash di Beranda
 * ([AmberCrashPanel]).
 */
class ErrorActivity : ComponentActivity() {

    companion object {
        const val EXTRA_CRASH_TYPE = "EXTRA_CRASH_TYPE"
        const val EXTRA_EXIT_CODE = "EXTRA_EXIT_CODE"
        const val EXTRA_IS_SIGNAL = "EXTRA_IS_SIGNAL"
        const val EXTRA_LOG_PATH = "EXTRA_LOG_PATH"
        const val EXTRA_INSTANCE_NAME = "EXTRA_INSTANCE_NAME"
        const val EXTRA_MC_VERSION = "EXTRA_MC_VERSION"
        const val EXTRA_THROWABLE_STRING = "EXTRA_THROWABLE_STRING"
        private const val MAX_INTENT_TRACE_CHARS = 20_000
        private const val MAX_LOG_TAIL_BYTES = 256L * 1024L

        fun showGameCrash(
            context: Context,
            exitCode: Int,
            isSignal: Boolean,
            logPath: String,
            instanceName: String = "Minecraft",
            mcVersion: String = "Unknown"
        ) {
            try {
                val intent = Intent(context, ErrorActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    putExtra(EXTRA_CRASH_TYPE, GameCrashInfo.CRASH_TYPE_GAME)
                    putExtra(EXTRA_EXIT_CODE, exitCode)
                    putExtra(EXTRA_IS_SIGNAL, isSignal)
                    putExtra(EXTRA_LOG_PATH, logPath)
                    putExtra(EXTRA_INSTANCE_NAME, instanceName)
                    putExtra(EXTRA_MC_VERSION, mcVersion)
                }
                context.startActivity(intent)
            } catch (_: Throwable) {
            }
        }

        fun showLauncherCrash(
            context: Context,
            throwable: Throwable
        ) {
            try {
                val rawTrace = android.util.Log.getStackTraceString(throwable)
                val stackTrace = if (rawTrace.length > MAX_INTENT_TRACE_CHARS) {
                    rawTrace.take(MAX_INTENT_TRACE_CHARS) + "\n...[dipotong agar tidak melebihi batas IPC]"
                } else {
                    rawTrace
                }
                val intent = Intent(context, ErrorActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    putExtra(EXTRA_CRASH_TYPE, GameCrashInfo.CRASH_TYPE_LAUNCHER)
                    putExtra(EXTRA_THROWABLE_STRING, stackTrace)
                }
                context.startActivity(intent)
            } catch (_: Throwable) {
            }
        }

        private fun readTailLinesBounded(file: File, maxLines: Int = 300): String {
            if (!file.exists() || !file.isFile) return ""
            val length = file.length()
            if (length <= 0L) return ""
            val lines = ArrayDeque<String>(maxLines + 1)
            java.io.RandomAccessFile(file, "r").use { raf ->
                val startOffset = (length - MAX_LOG_TAIL_BYTES).coerceAtLeast(0L)
                raf.seek(startOffset)
                if (startOffset > 0L) {
                    raf.readLine() // buang baris pertama yang mungkin terpotong di tengah
                }
                var line = raf.readLine()
                while (line != null) {
                    val utf8 = String(line.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8)
                    if (lines.size >= maxLines) lines.removeFirst()
                    lines.addLast(utf8)
                    line = raf.readLine()
                }
            }
            return lines.joinToString("\n")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        try {
            Tr.init(this)
        } catch (_: Throwable) {
        }

        val crashType = intent?.getStringExtra(EXTRA_CRASH_TYPE) ?: GameCrashInfo.CRASH_TYPE_GAME
        val exitCode = intent?.getIntExtra(EXTRA_EXIT_CODE, 1) ?: 1
        val isSignal = intent?.getBooleanExtra(EXTRA_IS_SIGNAL, false) ?: false
        val logPath = intent?.getStringExtra(EXTRA_LOG_PATH) ?: ""
        val instanceName = intent?.getStringExtra(EXTRA_INSTANCE_NAME) ?: "Minecraft"
        val mcVersion = intent?.getStringExtra(EXTRA_MC_VERSION) ?: "Unknown"
        val throwableString = intent?.getStringExtra(EXTRA_THROWABLE_STRING)

        val logFile = if (logPath.isNotBlank()) File(logPath) else File(filesDir, "latestlog.txt")

        // CrashManager sudah menulis catatan lengkap (pemuat mod, daftar mod, potongan log) sebelum
        // halaman ini dibuka. Game berjalan di proses lain, jadi dibaca dari berkas.
        val saved = if (crashType == GameCrashInfo.CRASH_TYPE_GAME) {
            CrashManager.readSavedCrash(this)?.takeIf { it.instanceName == instanceName }
        } else {
            null
        }

        // Memuat konten log untuk ditampilkan (terbatas memori agar file log raksasa tidak memicu OOM)
        val logContent = when {
            throwableString != null -> throwableString
            saved != null && saved.logSnippet.isNotBlank() -> saved.logSnippet
            logFile.exists() -> {
                try {
                    readTailLinesBounded(logFile, maxLines = 300)
                } catch (e: Throwable) {
                    Tr.t("Gagal membaca file log: ${e.message}")
                }
            }
            else -> ""
        }

        val info = GameCrashInfo(
            instanceName = instanceName,
            mcVersion = mcVersion,
            exitCode = exitCode,
            isSignal = isSignal,
            logSnippet = logContent,
            fullLogPath = logPath,
            crashType = crashType,
            loader = saved?.loader ?: "vanilla",
            loaderVersion = saved?.loaderVersion,
            installedMods = saved?.installedMods.orEmpty(),
            gameDirPath = saved?.gameDirPath,
            graphicsInfo = saved?.graphicsInfo
        )

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Amber.Ink
                ) {
                    NuxReadable(scale = 0.9f) {
                        AmberCrashPanel(
                            info = info,
                            logText = logContent,
                            logFile = logFile,
                            onClose = { finish() },
                            onBackToLauncher = {
                                try {
                                    val mainIntent = Intent(this@ErrorActivity, MainActivity::class.java).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                    }
                                    startActivity(mainIntent)
                                } catch (_: Throwable) {
                                }
                                finish()
                            },
                            modifier = Modifier.safeDrawingPadding()
                        )
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------
// Section: core/record/RecordService.kt
// ----------------------------------------------------------------------------

/**
 * Pembungkus foreground service untuk [ScreenRecorder]. Android mewajibkan tangkap layar berjalan
 * di service foreground bertipe mediaProjection. Service ini tidak melakukan pekerjaan berat
 * sendiri: hanya menampilkan notifikasi (dengan tombol Berhenti) dan memanggil [ScreenRecorder].
 * Berjalan di proses `:game` agar berbagi objek dengan GameActivity.
 */
class RecordService : Service() {

    companion object {
        private const val ACTION_START = "com.aistudio.novacraft.launcher.record.START"
        private const val ACTION_STOP = "com.aistudio.novacraft.launcher.record.STOP"
        private const val EXTRA_RESULT_CODE = "result_code"
        private const val EXTRA_DATA = "data"
        private const val EXTRA_W = "w"
        private const val EXTRA_H = "h"
        private const val EXTRA_DPI = "dpi"
        private const val CHANNEL_ID = "amber_record"
        private const val NOTIFICATION_ID = 4107

        /** Mikrofon dipakai hanya bila diminta di Pengaturan DAN izinnya sudah diberikan. */
        fun micAllowed(context: Context): Boolean =
            SettingsManager.settings.value.recordMic &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED

        /** Mulai merekam memakai hasil dialog izin tangkap layar. */
        fun start(context: Context, resultCode: Int, data: Intent, screenW: Int, screenH: Int, dpi: Int) {
            val intent = Intent(context, RecordService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_RESULT_CODE, resultCode)
                .putExtra(EXTRA_DATA, data)
                .putExtra(EXTRA_W, screenW)
                .putExtra(EXTRA_H, screenH)
                .putExtra(EXTRA_DPI, dpi)
            ContextCompat.startForegroundService(context, intent)
        }

        /** Menutup service (dipanggil setelah rekaman berhenti). Aman dipanggil kapan saja. */
        fun requestStop(context: Context?) {
            if (context == null) return
            try {
                context.stopService(Intent(context, RecordService::class.java))
            } catch (_: Throwable) {
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (intent != null && action == ACTION_START) {
            handleStart(intent)
        } else if (action == ACTION_STOP) {
            ScreenRecorder.stop()
            stopSelf()
        } else {
            // Dihidupkan ulang sistem tanpa data izin: tidak ada yang bisa dilakukan.
            stopSelf()
        }
        // Izin tangkap layar sekali pakai, jadi service tidak boleh dihidupkan ulang otomatis.
        return START_NOT_STICKY
    }

    private fun handleStart(intent: Intent) {
        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
        @Suppress("DEPRECATION")
        val data = intent.getParcelableExtra<Intent>(EXTRA_DATA)
        val useMic = micAllowed(this)

        // 1) Wajib: jadi foreground service SEBELUM mengambil MediaProjection (Android 14+).
        try {
            startAsForeground(useMic)
        } catch (_: Throwable) {
            stopSelf()
            return
        }
        if (data == null || resultCode != Activity.RESULT_OK) {
            stopSelf()
            return
        }

        // 2) Siapkan dan jalankan perekam. Bila gagal, ScreenRecorder sudah menampilkan alasannya.
        val ok = ScreenRecorder.start(
            context = this,
            resultCode = resultCode,
            data = data,
            screenW = intent.getIntExtra(EXTRA_W, 1280),
            screenH = intent.getIntExtra(EXTRA_H, 720),
            screenDpi = intent.getIntExtra(EXTRA_DPI, 320),
            settings = SettingsManager.settings.value,
            useMic = useMic
        )
        if (!ok) stopSelf()
    }

    private fun startAsForeground(useMic: Boolean) {
        val notification = buildNotification()
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                var type = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                if (useMic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                }
                startForeground(NOTIFICATION_ID, notification, type)
            }
            else -> startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                Tr.t("Rekam layar"),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }
            nm.createNotificationChannel(channel)
        }

        val flagsImmutable = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val openGame = PendingIntent.getActivity(
            this,
            0,
            Intent(this, GameActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            flagsImmutable
        )
        val stop = PendingIntent.getService(
            this,
            1,
            Intent(this, RecordService::class.java).setAction(ACTION_STOP),
            flagsImmutable
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_record_notif)
            .setContentTitle(Tr.t("Merekam layar"))
            .setContentText(Tr.t("Ketuk Berhenti untuk menyimpan rekaman."))
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setContentIntent(openGame)
            .addAction(R.drawable.ic_record_notif, Tr.t("Berhenti"), stop)
            .build()
    }

    override fun onDestroy() {
        // Jaga-jaga bila sistem mematikan service saat masih merekam: simpan berkasnya.
        if (ScreenRecorder.isRecording) ScreenRecorder.stop()
        super.onDestroy()
    }
}

// ----------------------------------------------------------------------------
// Section: core/forge/ForgeInstallService.kt
// ----------------------------------------------------------------------------

/**
 * Menjalankan installer resmi Forge/NeoForge (misalnya `SimpleInstaller --installClient <folder> --offline`)
 * di JVM bawaan Amber.
 *
 * Berjalan di proses `:forge` yang terpisah dari launcher dan dari game, karena sebuah proses hanya
 * bisa membuat satu JVM dan JVM yang selesai mematikan prosesnya. Proses ini hidup hanya selama
 * pemasangan: setelah selesai (atau dibatalkan) service berhenti dan prosesnya dimatikan, sehingga
 * tidak ada RAM maupun CPU yang tersisa. Service foreground supaya pemasangan tidak dihentikan
 * sistem bila pengguna berpindah aplikasi.
 *
 * Hubungan dengan proses utama lewat berkas di [workDir]: [LOG_NAME] (log installer) dan
 * [RESULT_NAME] (kode keluar, ditulis paling akhir).
 */
class ForgeInstallService : Service() {

    companion object {
        const val EXTRA_INSTALLER = "forge_installer"
        const val EXTRA_MAIN_CLASS = "forge_main_class"
        const val EXTRA_ARGS = "forge_args"
        const val EXTRA_LABEL = "forge_label"
        const val EXTRA_RUNTIME_HOME = "forge_runtime_home"

        const val LOG_NAME = "installer.log"
        const val RESULT_NAME = "result.txt"

        private const val CHANNEL_ID = "amber_forge_install"
        private const val NOTIFICATION_ID = 4108

        fun workDir(context: Context): File = File(context.filesDir, "forge-install")
    }

    private val started = AtomicBoolean(false)
    private val resultWritten = AtomicBoolean(false)
    private val logLock = Any()

    @Volatile
    private var logWriter: Writer? = null

    /** Nama loader ("Forge"/"NeoForge") untuk notifikasi dan awalan baris log. */
    @Volatile
    private var label = "Forge"

    /**
     * Terikat dari proses utama. Ikatan inilah yang menjamin installer jalan walau startForegroundService
     * ditolak (Android 12+ melarang memulainya saat aplikasi di latar belakang, misalnya pengguna
     * berpindah aplikasi selagi game diunduh), karena bindService tidak dibatasi seperti itu.
     */
    override fun onBind(intent: Intent?): IBinder {
        begin(intent)
        return Binder()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getStringExtra(EXTRA_LABEL)?.takeIf { it.isNotBlank() }?.let { label = it }
        startAsForeground()
        if (!begin(intent)) stopSelf()
        return START_NOT_STICKY
    }

    /** Memulai thread installer sekali saja. False bila [intent] tidak membawa parameter yang lengkap. */
    private fun begin(intent: Intent?): Boolean {
        val installer = intent?.getStringExtra(EXTRA_INSTALLER)
        val mainClass = intent?.getStringExtra(EXTRA_MAIN_CLASS)
        val installerArgs = intent?.getStringArrayExtra(EXTRA_ARGS)
        val runtimeHome = intent?.getStringExtra(EXTRA_RUNTIME_HOME)
        if (installer == null || mainClass == null || installerArgs == null || runtimeHome == null) return false
        intent?.getStringExtra(EXTRA_LABEL)?.takeIf { it.isNotBlank() }?.let { label = it }
        if (started.compareAndSet(false, true)) {
            thread(name = "Amber-Forge-Installer") {
                runInstaller(File(installer), mainClass, installerArgs.toList(), File(runtimeHome))
            }
        }
        return true
    }

    private fun startAsForeground() {
        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    Tr.t("Pemasangan Forge"),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    setShowBadge(false)
                    setSound(null, null)
                    enableVibration(false)
                }
                nm.createNotificationChannel(channel)
            }
            val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(Tr.t("Memasang $label"))
                .setContentText(Tr.t("Proses ini hanya berjalan sekali per versi $label."))
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (_: Throwable) {
            // Tetap lanjut sebagai service biasa; pemasangan hanya lebih rentan dihentikan sistem
        }
    }

    // ---------------------------------------------------------------- JVM

    private fun runInstaller(installer: File, mainClass: String, installerArgs: List<String>, runtimeHome: File) {
        val work = workDir(this)
        work.mkdirs()
        try {
            logWriter = File(work, LOG_NAME).bufferedWriter()
        } catch (_: Throwable) {
        }
        try {
            ZLNativeInvoker.onExitCallback = { exitCode, _ ->
                log("[$label] JVM keluar dengan kode $exitCode")
                finish(exitCode)
            }
            LoggerBridge.setListener { text -> if (text != null) log(text) }

            // Logger native membuka berkasnya tanpa O_CREAT (sama seperti GameActivity.setupLogger),
            // jadi berkasnya harus sudah ada; kalau tidak, start() gagal dengan
            // "No such file or directory" dan stdout/stderr sudah telanjur dialihkan ke pipa tanpa pembaca.
            val nativeOut = File(work, "native.out")
            nativeOut.delete()
            nativeOut.createNewFile()
            if (!nativeOut.exists()) throw java.io.IOException("Tidak bisa membuat ${nativeOut.absolutePath}")
            LoggerBridge.start(nativeOut.absolutePath)

            val args = prepareJvm(installer, mainClass, installerArgs, runtimeHome, work)
            log("[$label] Menjalankan installer: ${args.joinToString(" ")}")
            val code = VMLauncher.launchJVM(args.toTypedArray())
            finish(code)
        } catch (t: Throwable) {
            // Nama kelas ikut ditulis supaya penyebabnya terbaca dari pesan galat di layar
            log("[$label] ERROR: ${t.javaClass.simpleName}: ${t.message ?: ""}".trimEnd(' ', ':'))
            t.stackTrace.take(8).forEach { log("    at $it") }
            finish(-1)
        } finally {
            stopSelf()
        }
    }

    /** Menyiapkan lingkungan JVM (jalur pustaka, pre-dlopen runtime) dan mengembalikan argumen peluncuran. */
    private fun prepareJvm(
        installer: File,
        mainClass: String,
        installerArgs: List<String>,
        runtimeHome: File,
        work: File
    ): List<String> {
        val nativeLibDir = applicationInfo.nativeLibraryDir
        val rh = runtimeHome.absolutePath
        val libDirName = if (android.os.Process.is64Bit()) "lib64" else "lib"

        // Jalur linker untuk memuat pustaka JRE (sama dengan jalur proses game, tanpa renderer/LWJGL)
        val archSubDir = listOf("aarch64", "aarch32", "arm", "i386", "amd64", "x86_64")
            .map { File("$rh/lib", it) }
            .firstOrNull { it.isDirectory }
        val runtimeLd = mutableListOf<String>()
        if (archSubDir != null) {
            runtimeLd.add("${archSubDir.absolutePath}/jli")
            runtimeLd.add("${archSubDir.absolutePath}/server")
            runtimeLd.add("${archSubDir.absolutePath}/client")
            runtimeLd.add(archSubDir.absolutePath)
        }
        runtimeLd.add("$rh/lib/jli")
        if (File("$rh/jre").exists()) {
            runtimeLd.add("$rh/jre/lib/server")
            runtimeLd.add("$rh/jre/lib")
        } else {
            runtimeLd.add("$rh/lib/server")
            runtimeLd.add("$rh/lib")
        }
        runtimeLd.add(nativeLibDir)
        runtimeLd.add("/system/$libDirName")
        runtimeLd.add("/vendor/$libDirName")
        runtimeLd.add("/vendor/$libDirName/hw")
        runtimeLd.add("/system_ext/$libDirName")
        ZLBridge.setLdLibraryPath(runtimeLd.distinct().joinToString(":"))

        // LD_LIBRARY_PATH proses tidak boleh memuat folder JRE lib/server atau lib/jli
        // (libjli akan mengira perlu menjalankan ulang bin/java, yang ditolak Android)
        val processLd = listOf(
            "/system/$libDirName", "/vendor/$libDirName", "/vendor/$libDirName/hw",
            "/system_ext/$libDirName", nativeLibDir
        ).joinToString(":")

        Os.setenv("POJAV_NATIVEDIR", nativeLibDir, true)
        Os.setenv("POJAV_RENDERER", "opengles3", true)
        Os.setenv("JAVA_HOME", rh, true)
        Os.setenv("HOME", work.absolutePath, true)
        Os.setenv("TMPDIR", cacheDir.absolutePath, true)
        Os.setenv("PATH", "$rh/bin:" + (Os.getenv("PATH") ?: "/system/bin"), true)
        Os.setenv("LD_LIBRARY_PATH", processLd, true)

        preDlopenRuntime(runtimeHome)

        ZLBridge.setupExitMethod(this)
        ZLBridge.initializeGameExitHook()
        ZLBridge.chdir(work.absolutePath)

        JavaRuntimeManager.ensureExecutablePermissionsOnce(runtimeHome)

        val totalMb = try {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val info = ActivityManager.MemoryInfo()
            am.getMemoryInfo(info)
            (info.totalMem / (1024 * 1024)).toInt()
        } catch (_: Throwable) {
            4096
        }
        // Cukup untuk processor Forge/NeoForge, tetapi tidak merebut RAM HP kecil
        var heapMb = (totalMb / 4).coerceIn(384, 768)
        if (!android.os.Process.is64Bit()) heapMb = minOf(heapMb, 640)

        val jvmArgs = listOf(
            "$rh/bin/java",
            "-Djava.home=$rh",
            "-Djava.io.tmpdir=${cacheDir.absolutePath}",
            "-Djava.library.path=$processLd",
            "-Djava.awt.headless=true",
            "-Dos.name=Linux",
            "-Dos.version=Android-${Build.VERSION.RELEASE}",
            "-Duser.home=${work.absolutePath}",
            "-Dfile.encoding=UTF-8",
            "-Dsun.stdout.encoding=UTF-8",
            "-Dsun.stderr.encoding=UTF-8",
            "-Djava.net.preferIPv4Stack=true",
            "-Djdk.lang.Process.launchMechanism=FORK",
            "-XX:+IgnoreUnrecognizedVMOptions",
            "-XX:+UseSerialGC",
            "-Xms64M",
            "-Xmx${heapMb}M",
            "-cp", installer.absolutePath,
            mainClass
        )
        return jvmArgs + installerArgs
    }

    /** Memuat pustaka inti JRE lebih dulu dengan path absolut, sama seperti proses game. */
    private fun preDlopenRuntime(runtimeHome: File) {
        val rtLibDir = if (File(runtimeHome, "jre/lib").exists()) File(runtimeHome, "jre/lib") else File(runtimeHome, "lib")
        val archLibDir = listOf("aarch64", "aarch32", "arm", "i386", "amd64", "x86_64")
            .map { File(rtLibDir, it) }
            .firstOrNull { it.isDirectory } ?: rtLibDir
        val jliDir = when {
            File(rtLibDir, "jli/libjli.so").exists() -> File(rtLibDir, "jli")
            File(archLibDir, "jli/libjli.so").exists() -> File(archLibDir, "jli")
            else -> rtLibDir
        }
        val jvmDir = when {
            File(rtLibDir, "server/libjvm.so").exists() -> File(rtLibDir, "server")
            File(rtLibDir, "client/libjvm.so").exists() -> File(rtLibDir, "client")
            File(archLibDir, "server/libjvm.so").exists() -> File(archLibDir, "server")
            File(archLibDir, "client/libjvm.so").exists() -> File(archLibDir, "client")
            else -> rtLibDir
        }
        val essential = listOf(
            File(jliDir, "libjli.so"),
            File(jvmDir, "libjvm.so"),
            File(rtLibDir, "libverify.so"), File(archLibDir, "libverify.so"),
            File(rtLibDir, "libjava.so"), File(archLibDir, "libjava.so"),
            File(rtLibDir, "libnet.so"), File(archLibDir, "libnet.so"),
            File(rtLibDir, "libnio.so"), File(archLibDir, "libnio.so")
        )
        essential.forEach { so ->
            if (so.exists()) {
                try {
                    ZLBridge.dlopen(so.absolutePath)
                } catch (_: Throwable) {
                }
            }
        }
        // Sisa pustaka runtime (zip, management, dll.) dimuat juga, persis seperti proses game
        runtimeHome.walkTopDown().filter { it.isFile && it.name.endsWith(".so") }.forEach { so ->
            try {
                ZLBridge.dlopen(so.absolutePath)
            } catch (_: Throwable) {
            }
        }
    }

    // ---------------------------------------------------------------- berkas komunikasi

    private fun log(text: String) {
        synchronized(logLock) {
            try {
                val w = logWriter ?: return
                w.write(text.trimEnd('\n', '\r'))
                w.write("\n")
                w.flush()
            } catch (_: Throwable) {
            }
        }
    }

    /** Menulis kode keluar sekali saja; berkas ini yang ditunggu proses utama. */
    private fun finish(exitCode: Int) {
        if (!resultWritten.compareAndSet(false, true)) return
        try {
            val work = workDir(this)
            val tmp = File(work, "$RESULT_NAME.tmp")
            tmp.writeText(exitCode.toString())
            tmp.renameTo(File(work, RESULT_NAME))
        } catch (_: Throwable) {
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        synchronized(logLock) {
            try {
                logWriter?.close()
            } catch (_: Throwable) {
            }
            logWriter = null
        }
        // JVM tidak bisa dibuat ulang di proses yang sama dan tidak bisa dihentikan dari luar:
        // mematikan proses :forge adalah satu-satunya cara bersih (juga saat pemasangan dibatalkan).
        android.os.Process.killProcess(android.os.Process.myPid())
    }
}

// ----------------------------------------------------------------------------
// Section: core/download/DownloadKeepAliveService.kt
// ----------------------------------------------------------------------------

/**
 * Menjaga proses launcher tetap hidup selama Minecraft diunduh. Tanpa ini, di HP RAM kecil Android cepat
 * mematikan launcher begitu pengguna pindah aplikasi atau layar mati, dan unduhan berhenti di tengah jalan.
 *
 * Service ini tidak mengunduh apa pun: unduhan tetap berjalan di [MinecraftDownloader]. Service hanya
 * memberi prioritas proses (notifikasi latar depan) dan menahan CPU tetap menyala. Dimulai dan dihentikan
 * lewat [start] dan [stop]; kegagalan memulainya tidak mengganggu unduhan.
 */
class DownloadKeepAliveService : Service() {

    companion object {
        private const val CHANNEL_ID = "amber_download"
        private const val NOTIFICATION_ID = 4109

        /** Batas aman wake lock bila [stop] tidak pernah terpanggil (mis. galat tak terduga). */
        private const val WAKE_LOCK_TIMEOUT_MS = 2L * 60L * 60L * 1000L

        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, DownloadKeepAliveService::class.java))
            } catch (_: Throwable) {
                // Sistem menolak service latar depan (mis. aplikasi sedang di latar belakang): unduhan jalan seperti biasa.
            }
        }

        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, DownloadKeepAliveService::class.java))
            } catch (_: Throwable) {
            }
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForeground()
        acquireWakeLock()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        releaseWakeLock()
        super.onDestroy()
    }

    private fun startAsForeground() {
        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    Tr.t("Unduhan game"),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    setShowBadge(false)
                    setSound(null, null)
                    enableVibration(false)
                }
                nm.createNotificationChannel(channel)
            }
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            val contentIntent = launchIntent?.let {
                PendingIntent.getActivity(
                    this, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            }
            val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(Tr.t("Mengunduh Minecraft"))
                .setContentText(Tr.t("Unduhan lanjut di latar belakang. Jangan tutup paksa aplikasi."))
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (_: Throwable) {
            // Tetap jalan sebagai service biasa; unduhan hanya lebih rentan dihentikan sistem.
        }
    }

    private fun acquireWakeLock() {
        try {
            if (wakeLock?.isHeld == true) return
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NovaCraft:download").apply {
                setReferenceCounted(false)
                acquire(WAKE_LOCK_TIMEOUT_MS)
            }
        } catch (_: Throwable) {
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        } catch (_: Throwable) {
        }
        wakeLock = null
    }
}

// ----------------------------------------------------------------------------
// Section: core/vulkan/NovaVulkanEngine.kt#service
// ----------------------------------------------------------------------------

class VulkanCheckService : Service() {

    private companion object {
        const val WATCHDOG_MS = 18_000L
    }

    private val started = AtomicBoolean(false)

    override fun onBind(intent: Intent?): IBinder {
        begin()
        return Binder()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        begin()
        return START_NOT_STICKY
    }

    private fun begin() {
        if (!started.compareAndSet(false, true)) return
        val app = applicationContext
        thread(name = "NovaCraft-Vulkan-Watchdog", isDaemon = true) {
            try {
                Thread.sleep(WATCHDOG_MS)
            } catch (_: InterruptedException) {
            }
            Process.killProcess(Process.myPid())
        }
        thread(name = "NovaCraft-Vulkan-Check") {
            try {
                runCheck(app)
            } catch (_: Throwable) {
            } finally {
                Process.killProcess(Process.myPid())
            }
        }
    }

    private fun runCheck(app: Context) {
        val dir = VulkanCheck.dir(app)
        dir.mkdirs()
        val marker = File(dir, VulkanCheck.MARKER_NAME)
        if (marker.exists()) return
        marker.writeText(System.currentTimeMillis().toString())

        val report = measure()

        val temp = File(dir, VulkanCheck.RESULT_NAME + ".tmp")
        val result = File(dir, VulkanCheck.RESULT_NAME)
        temp.writeText(report.toJson(VulkanCheck.deviceKey()))
        if (!temp.renameTo(result)) {
            result.delete()
            if (!temp.renameTo(result)) {
                temp.delete()
                return
            }
        }
        marker.delete()
    }

    private fun measure(): VulkanReport {
        val log = StringBuilder()
        val caps = try {
            VulkanChecker.check { level, message ->
                if (log.length < 600) log.append('[').append(level).append("] ").append(message).append('\n')
            }
        } catch (e: LinkageError) {
            return VulkanReport(VulkanState.UNAVAILABLE, detail = e.javaClass.simpleName)
        }
        return if (caps == null) {
            VulkanReport(VulkanState.NO_VULKAN, detail = log.toString().trim())
        } else {
            VulkanReport(
                state = VulkanState.OK,
                major = caps.apiVersionMajor,
                minor = caps.apiVersionMinor,
                patch = caps.apiVersionPatch,
                extensions = caps.extensions.toHashSet(),
                features = HashMap(caps.features)
            )
        }
    }
}

// ----------------------------------------------------------------------------
// Section: core/update/NovaUpdateInstallerEngine.kt#receiver
// ----------------------------------------------------------------------------

class UpdateInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != UpdateInstaller.ACTION_RESULT) return
        val app = context.applicationContext
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)

        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            val confirm: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_INTENT)
            }
            if (confirm != null) {
                try {
                    confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    app.startActivity(confirm)
                } catch (_: Throwable) {
                    UpdateManager.onInstallResult(
                        app,
                        PackageInstaller.STATUS_FAILURE,
                        "layar konfirmasi tidak bisa dibuka"
                    )
                }
            }
            return
        }
        UpdateManager.onInstallResult(app, status, message)
    }
}
