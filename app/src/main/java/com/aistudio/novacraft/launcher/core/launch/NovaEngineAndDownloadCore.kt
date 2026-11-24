package com.aistudio.novacraft.launcher.core.launch

import android.app.ActivityManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaRecorder
import android.media.MediaScannerConnection
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.PowerManager
import android.os.RemoteException
import android.os.StatFs
import android.os.SystemClock
import android.provider.MediaStore
import android.provider.Settings
import android.system.Os
import android.text.Html
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.content.pm.PackageInfoCompat
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.aistudio.novacraft.launcher.ForgeInstallService
import com.aistudio.novacraft.launcher.GameActivity
import com.aistudio.novacraft.launcher.RecordService
import com.aistudio.novacraft.launcher.UpdateInstallReceiver
import com.aistudio.novacraft.launcher.core.account.AccountManager
import com.aistudio.novacraft.launcher.core.account.MicrosoftAuthService
import com.aistudio.novacraft.launcher.core.utils.LauncherLog
import com.aistudio.novacraft.launcher.core.utils.LauncherLog as Log
import com.aistudio.novacraft.launcher.core.mods.HorizonEngine
import com.aistudio.novacraft.launcher.core.utils.AssetIndexContent
import com.aistudio.novacraft.launcher.core.utils.FabricLibrary
import com.aistudio.novacraft.launcher.core.utils.FabricLoaderItem
import com.aistudio.novacraft.launcher.core.utils.FabricProfile
import com.aistudio.novacraft.launcher.core.utils.Instance
import com.aistudio.novacraft.launcher.core.utils.LauncherSettings
import com.aistudio.novacraft.launcher.core.utils.Library
import com.aistudio.novacraft.launcher.core.utils.NativePluginManager
import com.aistudio.novacraft.launcher.core.utils.NuxDns
import com.aistudio.novacraft.launcher.core.utils.NuxVersionUtils
import com.aistudio.novacraft.launcher.core.utils.SafePaths
import com.aistudio.novacraft.launcher.core.utils.UserAccount
import com.aistudio.novacraft.launcher.core.utils.VersionDetail
import com.aistudio.novacraft.launcher.core.utils.VersionItem
import com.aistudio.novacraft.launcher.core.utils.VersionManifest
import com.aistudio.novacraft.launcher.ui.theme.Toast
import com.aistudio.novacraft.launcher.ui.theme.Tr
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InterruptedIOException
import java.io.RandomAccessFile
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.UnknownHostException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.LinkedHashMap
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.jar.Manifest
import java.util.zip.GZIPInputStream
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import javax.net.ssl.SSLException
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.CacheControl
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.json.JSONArray
import org.json.JSONObject

// ============================================================================
// Section: NovaEngineAndDownloadCore.kt
// ============================================================================

// ----------------------------------------------------------------------------
// Section: core/manifest/MojangManifestService.kt
// ----------------------------------------------------------------------------

object MojangManifestService {
    private const val MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
    private const val DISK_CACHE_FILE = "version_manifest_cache.json"
    private const val DEFAULT_CACHE_TTL_MS = 2L * 60L * 1000L

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .dns(NuxDns)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val fetchMutex = Mutex()

    @Volatile
    private var cachedManifest: VersionManifest? = null

    @Volatile
    private var lastFetchedAtMillis: Long = 0L

    private val _liveManifest = MutableStateFlow<VersionManifest?>(null)
    val liveManifest: StateFlow<VersionManifest?> = _liveManifest.asStateFlow()

    private val _lastUpdatedMillis = MutableStateFlow(0L)
    val lastUpdatedMillis: StateFlow<Long> = _lastUpdatedMillis.asStateFlow()

    fun prewarmFromDiskCache(context: Context? = com.aistudio.novacraft.launcher.core.utils.SettingsManager.appContext) {
        if (cachedManifest != null || context == null) return
        try {
            val file = File(context.filesDir, DISK_CACHE_FILE)
            if (!file.isFile) return
            val json = file.readText()
            val manifest = gson.fromJson(json, VersionManifest::class.java)
            if (manifest != null && !manifest.versions.isNullOrEmpty() && manifest.latest != null) {
                cachedManifest = manifest
                lastFetchedAtMillis = file.lastModified().coerceAtLeast(1L)
                _liveManifest.value = manifest
                _lastUpdatedMillis.value = lastFetchedAtMillis
            }
        } catch (_: Throwable) {
        }
    }

    private fun candidateManifestUrls(): List<String> {
        val src = com.aistudio.novacraft.launcher.core.utils.SettingsManager.settings.value.gameDownloadSource
        val bmcl = "https://bmclapi2.bangbang93.com/mc/game/version_manifest_v2.json"
        return if (src == "bmclapi") listOf(bmcl, MANIFEST_URL) else listOf(MANIFEST_URL, bmcl)
    }

    private fun candidateDetailUrls(url: String): List<String> {
        val src = com.aistudio.novacraft.launcher.core.utils.SettingsManager.settings.value.gameDownloadSource
        val bmcl = url.replace("https://piston-meta.mojang.com", "https://bmclapi2.bangbang93.com")
        return if (src == "bmclapi" && bmcl != url) listOf(bmcl, url) else if (bmcl != url) listOf(url, bmcl) else listOf(url)
    }

    suspend fun getVersionManifest(forceRefresh: Boolean = false): Result<VersionManifest> = withContext(Dispatchers.IO) {
        prewarmFromDiskCache()
        val now = System.currentTimeMillis()
        val existing = cachedManifest
        val curSettings = com.aistudio.novacraft.launcher.core.utils.SettingsManager.settings.value
        val autoCheck = curSettings.checkGameVersionList
        val localOnly = curSettings.localOnlyMode
        if (!forceRefresh && existing != null && (localOnly || !autoCheck || (now - lastFetchedAtMillis) < DEFAULT_CACHE_TTL_MS)) {
            return@withContext Result.success(existing)
        }
        if (!forceRefresh && localOnly) {
            return@withContext Result.failure(IOException("Chế độ Local-Only đang bật (chưa có manifest lưu sẵn)"))
        }

        fetchMutex.withLock {
            prewarmFromDiskCache()
            val recheckNow = System.currentTimeMillis()
            val recheckExisting = cachedManifest
            val latestSettings = com.aistudio.novacraft.launcher.core.utils.SettingsManager.settings.value
            val recheckAuto = latestSettings.checkGameVersionList
            val recheckLocalOnly = latestSettings.localOnlyMode
            if (!forceRefresh && recheckExisting != null && (recheckLocalOnly || !recheckAuto || (recheckNow - lastFetchedAtMillis) < DEFAULT_CACHE_TTL_MS)) {
                return@withContext Result.success(recheckExisting)
            }

            var lastErr: Exception? = null
            for (manifestUrl in candidateManifestUrls()) {
                try {
                    val requestBuilder = Request.Builder()
                        .url(manifestUrl)
                        .header("User-Agent", "NovaCraftLauncher/2.3 (Android)")
                    if (forceRefresh) {
                        requestBuilder
                            .cacheControl(CacheControl.FORCE_NETWORK)
                            .header("Cache-Control", "no-cache")
                    }
                    client.newCall(requestBuilder.build()).execute().use { response ->
                        if (!response.isSuccessful) {
                            lastErr = IOException("HTTP Error ${response.code}")
                            return@use
                        }

                        val json = response.body?.string()
                        if (json.isNullOrBlank()) {
                            lastErr = IOException("Empty response body")
                            return@use
                        }
                        val manifest = gson.fromJson(json, VersionManifest::class.java)
                        if (manifest == null || manifest.versions.isNullOrEmpty() || manifest.latest == null) {
                            lastErr = IOException("Invalid Mojang manifest payload")
                            return@use
                        }
                        val fetchedAt = System.currentTimeMillis()
                        cachedManifest = manifest
                        lastFetchedAtMillis = fetchedAt
                        _liveManifest.value = manifest
                        _lastUpdatedMillis.value = fetchedAt
                        com.aistudio.novacraft.launcher.core.utils.SettingsManager.appContext?.let { ctx ->
                            runCatching { File(ctx.filesDir, DISK_CACHE_FILE).writeText(json) }
                        }
                        return@withContext Result.success(manifest)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    lastErr = e
                }
            }
            Result.failure(lastErr ?: IOException("Gagal mengambil manifest versi"))
        }
    }

    suspend fun getVersionDetail(url: String): Result<VersionDetail> = withContext(Dispatchers.IO) {
        var lastErr: Exception? = null
        for (detailUrl in candidateDetailUrls(url)) {
            try {
                val request = Request.Builder()
                    .url(detailUrl)
                    .header("User-Agent", "NovaCraftLauncher/2.3 (Android)")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        lastErr = IOException("HTTP Error ${response.code}")
                        return@use
                    }

                    val json = response.body?.string()
                    if (json.isNullOrBlank()) {
                        lastErr = IOException("Empty response body")
                        return@use
                    }
                    val detail = gson.fromJson(json, VersionDetail::class.java)
                    if (detail == null) {
                        lastErr = IOException("Invalid version detail payload")
                        return@use
                    }
                    return@withContext Result.success(detail)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastErr = e
            }
        }
        Result.failure(lastErr ?: IOException("Gagal membaca detail versi"))
    }
}

// ----------------------------------------------------------------------------
// Section: core/runtime/JavaRuntimeManager.kt
// ----------------------------------------------------------------------------

object JavaRuntimeManager {

    fun getDeviceArch(): String {
        // NovaCraft hanya mendukung arsitektur 64-bit (arm64-v8a).
        // Jangan mengandalkan SUPPORTED_ABIS.first() karena beberapa perangkat lama melaporkan
        // urutan ABI yang tidak konsisten meskipun berjalan dalam mode 64-bit.
        val abi = Build.SUPPORTED_64_BIT_ABIS.firstOrNull()
            ?: Build.SUPPORTED_ABIS.firstOrNull()
            ?: "arm64-v8a"
        return when {
            abi.contains("x86_64") -> "x86_64"
            else -> "arm64"
        }
    }

    fun getRuntimesDir(context: Context): File {
        val dir = File(context.filesDir, "runtimes")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getRuntimeHome(context: Context, runtimeName: String): File {
        return File(getRuntimesDir(context), runtimeName)
    }

    fun ensureExecutablePermissions(home: File) {
        if (!home.exists() || !home.isDirectory) return

        val javaBin = File(home, "bin/java")
        if (javaBin.exists()) {
            try {
                Os.chmod(javaBin.absolutePath, 493) // 0755: rwxr-xr-x
            } catch (_: Throwable) {}
            javaBin.setExecutable(true, false)
            javaBin.setReadable(true, false)
        }

        File(home, "bin").walkTopDown().forEach { file ->
            if (file.isFile) {
                try {
                    Os.chmod(file.absolutePath, 493) // 0755
                } catch (_: Throwable) {}
                file.setExecutable(true, false)
                file.setReadable(true, false)
            }
        }

        File(home, "lib").walkTopDown().forEach { file ->
            if (file.isFile && (file.extension == "so" || file.name.contains(".so"))) {
                try {
                    Os.chmod(file.absolutePath, 493) // 0755
                } catch (_: Throwable) {}
                file.setExecutable(true, false)
                file.setReadable(true, false)
            }
        }

        try {
            Runtime.getRuntime().exec(arrayOf("chmod", "-R", "755", home.absolutePath)).waitFor()
        } catch (_: Throwable) {}
    }

    private const val PERM_APPLIED_MARKER = ".nux_perm_applied"

    /**
     * Versi hemat dari [ensureExecutablePermissions]: izin diterapkan penuh SATU KALI per
     * instalasi runtime (ditandai file kecil). Peluncuran berikutnya cukup memeriksa satu file,
     * tanpa menyusuri ratusan file dan tanpa membuat proses `chmod -R` baru.
     * Jika runtime diekstrak ulang, folder (beserta penanda) dihapus sehingga izin diterapkan lagi.
     */
    fun ensureExecutablePermissionsOnce(home: File) {
        if (!home.exists() || !home.isDirectory) return
        val marker = File(home, PERM_APPLIED_MARKER)
        if (marker.exists() && File(home, "bin/java").canExecute()) return
        ensureExecutablePermissions(home)
        runCatching { marker.writeText("1") }
    }

    fun isRuntimeInstalled(context: Context, runtimeName: String): Boolean {
        val home = getRuntimeHome(context, runtimeName)
        if (!home.exists() || !home.isDirectory) return false

        val permMarker = File(home, ".nux_perm_v2")
        if (!permMarker.exists()) return false

        val javaBin = File(home, "bin/java")
        if (!javaBin.exists() || javaBin.length() == 0L) return false
        ensureExecutablePermissionsOnce(home)

        // Verify libjli.so exists
        val hasJli = File(home, "lib/jli/libjli.so").exists() ||
                File(home, "lib/libjli.so").exists() ||
                home.walkTopDown().any { it.name == "libjli.so" }
        if (!hasJli) return false

        // Verify libjvm.so exists
        val hasJvm = File(home, "lib/server/libjvm.so").exists() ||
                File(home, "lib/client/libjvm.so").exists() ||
                home.walkTopDown().any { it.name == "libjvm.so" }
        if (!hasJvm) return false

        // Verify modules (Java 9+) or rt.jar (Java 8)
        val hasModulesOrRt = File(home, "lib/modules").exists() ||
                File(home, "lib/rt.jar").exists() ||
                home.walkTopDown().any { it.name == "rt.jar" }
        if (!hasModulesOrRt) return false

        return true
    }

    enum class RuntimeSource {
        LAUNCHER_INTERNAL,
        ENV_JAVA_HOME,
        SYSTEM_PATH,
        SYSTEM_JVM_DIR
    }

    data class DetectedJavaRuntime(
        val id: String,
        val majorVersion: Int,
        val homeDir: File,
        val javaExecutable: File,
        val source: RuntimeSource,
        val versionString: String = ""
    )

    data class JavaAutoConfigResult(
        val instance: com.aistudio.novacraft.launcher.core.utils.Instance,
        val resolvedRuntimeId: String,
        val requiredMajor: Int,
        val detectedRuntime: DetectedJavaRuntime?,
        val autoUpdated: Boolean,
        val requiresExtraction: Boolean
    )

    /**
     * Memindai lingkungan sistem & penyimpanan launcher untuk menemukan seluruh runtime Java
     * yang benar-benar terpasang dan siap dijalankan (bukan ART/dalvikvm).
     */
    fun detectAvailableJavaRuntimes(context: Context): List<DetectedJavaRuntime> {
        val found = LinkedHashMap<String, DetectedJavaRuntime>()

        fun registerCandidate(homeDir: File, fallbackId: String?, source: RuntimeSource) {
            if (!homeDir.exists() || !homeDir.isDirectory) return
            val javaBin = File(homeDir, "bin/java")
            if (!javaBin.isFile || javaBin.length() == 0L) return
            if (!isOpenJdkHomeValid(homeDir)) return

            ensureExecutablePermissionsOnce(homeDir)
            val versionRaw = readJavaVersionString(homeDir, javaBin)
            val major = (versionRaw?.let { parseJavaVersionString(it) })
                ?: detectMajorVersion(homeDir, javaBin)
                ?: parseMajorFromRuntimeId(fallbackId ?: homeDir.name)
                ?: return
            val id = fallbackId?.takeIf { it.isNotBlank() } ?: "jre-$major"
            val canonicalPath = runCatching { homeDir.canonicalPath }.getOrElse { homeDir.absolutePath }
            if (!found.containsKey(canonicalPath)) {
                found[canonicalPath] = DetectedJavaRuntime(
                    id = id,
                    majorVersion = major,
                    homeDir = homeDir,
                    javaExecutable = javaBin,
                    source = source,
                    versionString = versionRaw ?: major.toString()
                )
            }
        }

        // 1. Runtime internal NovaCraft (files/runtimes/jre-*)
        val runtimesDir = getRuntimesDir(context)
        runtimesDir.listFiles()?.filter { it.isDirectory }?.sortedBy { it.name }?.forEach { dir ->
            registerCandidate(dir, dir.name, RuntimeSource.LAUNCHER_INTERNAL)
        }

        // 2. Variabel lingkungan JAVA_HOME
        val envJavaHome = runCatching { Os.getenv("JAVA_HOME") }.getOrNull() ?: System.getenv("JAVA_HOME")
        if (!envJavaHome.isNullOrBlank()) {
            registerCandidate(File(envJavaHome.trim()), null, RuntimeSource.ENV_JAVA_HOME)
        }

        // 3. Direktori PATH sistem (hindari /system/bin agar tidak tertukar dengan Android ART)
        val envPath = runCatching { Os.getenv("PATH") }.getOrNull() ?: System.getenv("PATH")
        envPath?.split(File.pathSeparatorChar)?.forEach { entry ->
            val trimmed = entry.trim()
            if (trimmed.isNotEmpty() && !trimmed.startsWith("/system/")) {
                val binDir = File(trimmed)
                val javaBin = File(binDir, "java")
                if (javaBin.isFile && javaBin.canExecute()) {
                    binDir.parentFile?.let { home ->
                        registerCandidate(home, null, RuntimeSource.SYSTEM_PATH)
                    }
                }
            }
        }

        // 4. Lokasi umum JDK/JRE eksternal di lingkungan perangkat
        val extraRoots = listOfNotNull(
            File("/usr/lib/jvm"),
            File("/data/local/tmp/jvm"),
            File(context.filesDir, "java"),
            context.getExternalFilesDir(null)?.let { File(it, "runtimes") }
        )
        for (root in extraRoots) {
            if (root.exists() && root.isDirectory) {
                root.listFiles()?.filter { it.isDirectory }?.forEach { dir ->
                    registerCandidate(dir, null, RuntimeSource.SYSTEM_JVM_DIR)
                }
            }
        }

        return found.values.sortedByDescending { it.majorVersion }
    }

    private fun isOpenJdkHomeValid(home: File): Boolean {
        val hasJli = File(home, "lib/jli/libjli.so").exists() ||
            File(home, "lib/libjli.so").exists() ||
            File(home, "lib").walkTopDown().maxDepth(3).any { it.name == "libjli.so" }
        val hasJvm = File(home, "lib/server/libjvm.so").exists() ||
            File(home, "lib/client/libjvm.so").exists() ||
            File(home, "lib").walkTopDown().maxDepth(3).any { it.name == "libjvm.so" }
        val hasModulesOrRt = File(home, "lib/modules").exists() ||
            File(home, "lib/rt.jar").exists() ||
            File(home, "jre/lib/rt.jar").exists()
        return hasJli && hasJvm && hasModulesOrRt
    }

    fun readJavaVersionString(homeDir: File, javaBin: File? = File(homeDir, "bin/java")): String? {
        val releaseFile = File(homeDir, "release")
        if (releaseFile.isFile) {
            runCatching {
                releaseFile.useLines { lines ->
                    var javaVer: String? = null
                    var fullVer: String? = null
                    for (line in lines) {
                        if (line.startsWith("JAVA_VERSION=")) {
                            javaVer = line.substringAfter('=').trim().trim('"', '\'')
                        } else if (line.startsWith("FULL_VERSION=")) {
                            fullVer = line.substringAfter('=').trim().trim('"', '\'')
                        }
                    }
                    val chosen = javaVer?.takeIf { it.isNotBlank() } ?: fullVer?.takeIf { it.isNotBlank() }
                    if (chosen != null) return chosen
                }
            }
        }
        if (javaBin != null && javaBin.isFile) {
            runCatching {
                val proc = ProcessBuilder(javaBin.absolutePath, "-version")
                    .redirectErrorStream(true)
                    .start()
                val out = proc.inputStream.bufferedReader().use { it.readText() }
                proc.waitFor()
                val match = Regex("version\\s+\"([^\"]+)\"").find(out)
                match?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }?.let { return it }
            }
        }
        return null
    }

    private fun detectMajorVersion(homeDir: File, javaBin: File): Int? {
        readJavaVersionString(homeDir, javaBin)?.let { ver ->
            parseJavaVersionString(ver)?.let { return it }
        }
        return parseMajorFromRuntimeId(homeDir.name)
    }

    private fun parseJavaVersionString(version: String): Int? {
        val cleaned = version.trim()
        if (cleaned.startsWith("1.")) {
            return cleaned.removePrefix("1.").substringBefore('.').substringBefore('_').toIntOrNull()
        }
        return cleaned.substringBefore('.').substringBefore('-').substringBefore('+').toIntOrNull()
    }

    private fun parseMajorFromRuntimeId(runtimeId: String): Int? {
        val digits = Regex("(\\d+)").find(runtimeId)?.groupValues?.getOrNull(1)
        return digits?.toIntOrNull()
    }

    fun hasBundledRuntimeAsset(context: Context, runtimeName: String): Boolean {
        return runCatching {
            context.assets.list("runtimes/$runtimeName")?.isNotEmpty() == true
        }.getOrDefault(false)
    }

    fun requiredJavaMajorForInstance(
        context: Context,
        instance: com.aistudio.novacraft.launcher.core.utils.Instance,
        mojangJavaMajor: Int? = null
    ): Int {
        if (mojangJavaMajor != null && mojangJavaMajor > 0) return mojangJavaMajor
        runCatching {
            val versionsDir = com.aistudio.novacraft.launcher.core.launch.InstanceManager.getVersionsDir(context)
            val jsonFile = File(File(versionsDir, instance.mcVersion), "${instance.mcVersion}.json")
            if (jsonFile.isFile) {
                val parsed = com.google.gson.Gson().fromJson(
                    jsonFile.readText(),
                    com.aistudio.novacraft.launcher.core.utils.VersionDetail::class.java
                )
                parsed?.javaVersion?.majorVersion?.takeIf { it > 0 }?.let { return it }
            }
        }
        val v = instance.mcVersion.trim()
        return when {
            v.startsWith("26.") -> 25
            v.startsWith("1.21") || v.startsWith("1.20.5") || v.startsWith("1.20.6") -> 21
            v.startsWith("1.20") || v.startsWith("1.19") || v.startsWith("1.18") || v.startsWith("1.17") -> 17
            else -> 8
        }
    }

    /**
     * Memeriksa lingkungan sistem & daftar runtime Java yang terpasang, lalu secara otomatis
     * memilih serta mengonfigurasi versi Java yang paling sesuai untuk instance Minecraft terpilih.
     */
    fun configureJavaForInstance(
        context: Context,
        instance: com.aistudio.novacraft.launcher.core.utils.Instance,
        mojangJavaMajor: Int? = null,
        persistChanges: Boolean = true
    ): JavaAutoConfigResult {
        if (com.aistudio.novacraft.launcher.core.mods.HorizonEngine.isEngine(instance.loader)) {
            return JavaAutoConfigResult(
                instance = instance,
                resolvedRuntimeId = "horizon-native",
                requiredMajor = 0,
                detectedRuntime = null,
                autoUpdated = false,
                requiresExtraction = false
            )
        }

        val appCtx = context.applicationContext
        val available = detectAvailableJavaRuntimes(appCtx)
        val requiredMajor = requiredJavaMajorForInstance(appCtx, instance, mojangJavaMajor)
        val recommendedId = getRecommendedRuntime(instance.mcVersion, requiredMajor)

        val globalDefault = com.aistudio.novacraft.launcher.core.utils.SettingsManager.settings.value.defaultJavaRuntime
        val requestedId = when {
            instance.javaRuntime.isNotBlank() && !instance.javaRuntime.equals("auto", ignoreCase = true) ->
                instance.javaRuntime.trim()
            globalDefault.isNotBlank() && !globalDefault.equals("auto", ignoreCase = true) ->
                globalDefault.trim()
            else -> null
        }

        val detected = when {
            requestedId != null -> {
                val reqMajor = parseMajorFromRuntimeId(requestedId)
                available.firstOrNull { it.id.equals(requestedId, ignoreCase = true) }
                    ?: reqMajor?.let { m -> available.firstOrNull { it.majorVersion == m } }
            }
            else -> {
                available.firstOrNull { it.id.equals(recommendedId, ignoreCase = true) }
                    ?: available.firstOrNull { it.majorVersion == requiredMajor }
                    ?: available.filter { it.majorVersion >= requiredMajor }.minByOrNull { it.majorVersion }
            }
        }

        val resolvedId = when {
            detected != null -> detected.id
            requestedId != null && hasBundledRuntimeAsset(appCtx, requestedId) -> requestedId
            else -> recommendedId
        }
        val requiresExtraction = detected == null && !isRuntimeInstalled(appCtx, resolvedId)

        val needsUpdate = instance.javaRuntime.isBlank() ||
            instance.javaRuntime.equals("auto", ignoreCase = true) ||
            (requestedId != null && !requestedId.equals(resolvedId, ignoreCase = true))

        val configuredInstance = if (needsUpdate && instance.javaRuntime != resolvedId) {
            instance.copy(javaRuntime = resolvedId)
        } else {
            instance
        }

        if (persistChanges && configuredInstance != instance) {
            runCatching {
                com.aistudio.novacraft.launcher.core.launch.InstanceManager.updateInstance(appCtx, configuredInstance)
            }
        }

        LauncherLog.event(
            "Runtime",
            "Auto-config Java [${configuredInstance.name}]: MC=${configuredInstance.mcVersion} (butuh Java $requiredMajor) -> " +
                "$resolvedId (terdeteksi=${detected?.source?.name ?: "EXTRACT_ASSET"}, terpasang=${available.size})"
        )

        return JavaAutoConfigResult(
            instance = configuredInstance,
            resolvedRuntimeId = resolvedId,
            requiredMajor = requiredMajor,
            detectedRuntime = detected,
            autoUpdated = configuredInstance != instance,
            requiresExtraction = requiresExtraction
        )
    }

    fun resolveRuntimeHome(
        context: Context,
        runtimeName: String,
        detected: DetectedJavaRuntime? = null
    ): File {
        if (detected != null && detected.homeDir.isDirectory) return detected.homeDir
        val internal = getRuntimeHome(context, runtimeName)
        if (internal.isDirectory) return internal
        val matched = detectAvailableJavaRuntimes(context).firstOrNull {
            it.id.equals(runtimeName, ignoreCase = true) ||
                parseMajorFromRuntimeId(runtimeName)?.let { m -> it.majorVersion == m } == true
        }
        return matched?.homeDir ?: internal
    }

    /**
     * Resolves recommended OpenJDK runtime based on Mojang manifest metadata or Minecraft version prefix.
     */
    fun getRecommendedRuntime(mcVersion: String, javaMajorVersion: Int? = null): String {
        if (javaMajorVersion != null) {
            return when {
                javaMajorVersion >= 25 -> "jre-25"
                javaMajorVersion >= 21 -> "jre-21"
                javaMajorVersion >= 17 -> "jre-17"
                else -> "jre-8"
            }
        }

        return when {
            mcVersion.startsWith("26.") -> "jre-25"
            mcVersion.startsWith("1.21") || mcVersion.startsWith("1.20.5") || mcVersion.startsWith("1.20.6") -> "jre-21"
            mcVersion.startsWith("1.20") || mcVersion.startsWith("1.19") || mcVersion.startsWith("1.18") || mcVersion.startsWith("1.17") -> "jre-17"
            else -> "jre-8"
        }
    }

    fun knownRuntimeIds(): List<String> = listOf("jre-8", "jre-21", "jre-17", "jre-25")

    fun getInternalRuntimeLabel(runtimeName: String): String {
        return when (runtimeName.lowercase()) {
            "auto" -> "Auto"
            "jre-8" -> "Internal"
            "jre-17" -> "Internal 17"
            "jre-21" -> "Internal 21"
            "jre-25" -> "Internal 25"
            else -> runtimeName
        }
    }

    fun getRuntimeDisplayName(runtimeName: String): String {
        return when (runtimeName.lowercase()) {
            "auto" -> "Auto"
            "jre-8" -> "Internal (Java 8)"
            "jre-17" -> "Internal 17"
            "jre-21" -> "Internal 21"
            "jre-25" -> "Internal 25"
            else -> runtimeName
        }
    }

    fun getJavaExecutable(context: Context, runtimeName: String): File {
        return File(resolveRuntimeHome(context, runtimeName), "bin/java")
    }

    suspend fun deleteRuntime(context: Context, runtimeId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val baseCanon = getRuntimesDir(context).canonicalFile
            val target = File(baseCanon, SafePaths.fileName(runtimeId)).canonicalFile
            if (!target.path.startsWith(baseCanon.path + File.separator)) {
                return@withContext Result.failure(SecurityException("Path runtime không hợp lệ"))
            }
            if (!target.exists()) {
                return@withContext Result.failure(IOException("Runtime không tồn tại"))
            }
            val deleted = target.deleteRecursively()
            if (deleted || !target.exists()) {
                Result.success(Unit)
            } else {
                Result.failure(IOException("Không thể xóa thư mục runtime"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importCustomRuntime(
        context: Context,
        uri: Uri,
        rawFileName: String? = null,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): Result<DetectedJavaRuntime> = withContext(Dispatchers.IO) {
        val stageDir = File(context.cacheDir, "rt_import_${System.currentTimeMillis()}")
        try {
            stageDir.mkdirs()
            val fileName = SafePaths.fileName(rawFileName ?: uri.lastPathSegment ?: "custom-runtime.tar.xz")
            val lower = fileName.lowercase(Locale.ROOT)
            onProgress(0.08f, "Đang đọc gói Java: $fileName...")

            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(IOException("Không thể mở tệp đã chọn"))

            inputStream.use { rawIn ->
                when {
                    lower.endsWith(".zip") -> {
                        ZipInputStream(BufferedInputStream(rawIn, 32768)).use { zipIn ->
                            val stageCanon = stageDir.canonicalFile
                            val buffer = ByteArray(32768)
                            var count = 0
                            var entry = zipIn.nextEntry
                            while (entry != null) {
                                val cleanName = entry.name.removePrefix("./").removePrefix("/")
                                if (cleanName.isNotEmpty() && cleanName != ".") {
                                    val targetFile = File(stageCanon, cleanName).canonicalFile
                                    if (!targetFile.path.startsWith(stageCanon.path + File.separator)) {
                                        throw SecurityException("Đường dẫn trong gói nén không hợp lệ: $cleanName")
                                    }
                                    if (entry.isDirectory) {
                                        targetFile.mkdirs()
                                    } else {
                                        targetFile.parentFile?.mkdirs()
                                        FileOutputStream(targetFile).use { out ->
                                            var len: Int
                                            while (zipIn.read(buffer).also { len = it } != -1) {
                                                out.write(buffer, 0, len)
                                            }
                                        }
                                    }
                                    count++
                                    if (count % 30 == 0) {
                                        onProgress(0.15f + (count.coerceAtMost(800) / 800f) * 0.65f, "Đang giải nén ($count tệp)...")
                                    }
                                }
                                zipIn.closeEntry()
                                entry = zipIn.nextEntry
                            }
                        }
                    }
                    lower.endsWith(".tar.gz") || lower.endsWith(".tgz") -> {
                        TarArchiveInputStream(GZIPInputStream(BufferedInputStream(rawIn, 32768))).use { tarIn ->
                            unpackTarStreamSafe(tarIn, stageDir, onProgress)
                        }
                    }
                    else -> {
                        // Mặc định .tar.xz / .xz
                        unpackTarXz(
                            inputStream = rawIn,
                            destDir = stageDir,
                            baseProgress = 0.1f,
                            targetProgress = 0.8f,
                            estimatedTotal = 650,
                            label = "Đang giải nén gói Java",
                            onProgress = onProgress
                        )
                    }
                }
            }

            onProgress(0.84f, "Đang kiểm tra cấu trúc OpenJDK...")
            val runtimeRoot = locateJavaRoot(stageDir)
                ?: return@withContext Result.failure(
                    IOException("Gói không hợp lệ (cần OpenJDK Android có bin/java, libjvm.so và libjli.so).")
                )

            normalizeJava8ArchLayout(runtimeRoot)
            if (!isOpenJdkHomeValid(runtimeRoot)) {
                return@withContext Result.failure(
                    IOException("Runtime thiếu thư viện lõi (libjvm.so, libjli.so hoặc modules/rt.jar).")
                )
            }

            val javaBin = File(runtimeRoot, "bin/java")
            val verStr = readJavaVersionString(runtimeRoot, javaBin)
            val major = (verStr?.let { parseJavaVersionString(it) }) ?: parseJavaVersionString(fileName) ?: 17

            val baseStem = fileName
                .removeSuffix(".tar.xz")
                .removeSuffix(".tar.gz")
                .removeSuffix(".tgz")
                .removeSuffix(".zip")
                .replace(Regex("[^a-zA-Z0-9._-]"), "-")
                .trim('-')
                .ifBlank { "custom-jre-$major" }

            val runtimesDir = getRuntimesDir(context)
            var candidateId = baseStem
            var suffix = 2
            while (File(runtimesDir, candidateId).exists()) {
                candidateId = "${baseStem}_$suffix"
                suffix++
            }
            val destDir = File(runtimesDir, candidateId)

            onProgress(0.92f, "Đang cài đặt $candidateId...")
            try {
                if (!runtimeRoot.copyRecursively(destDir, overwrite = true)) {
                    throw IOException("Không thể sao chép tệp runtime vào bộ nhớ ứng dụng")
                }
                ensureExecutablePermissions(destDir)
                runCatching { File(destDir, ".nux_perm_v2").writeText("1.0.9") }
            } catch (copyErr: Exception) {
                runCatching { destDir.deleteRecursively() }
                throw copyErr
            }

            onProgress(1f, "Đã thêm runtime $candidateId")
            val finalVer = readJavaVersionString(destDir, File(destDir, "bin/java")) ?: verStr ?: major.toString()
            Result.success(
                DetectedJavaRuntime(
                    id = candidateId,
                    majorVersion = major,
                    homeDir = destDir,
                    javaExecutable = File(destDir, "bin/java"),
                    source = RuntimeSource.LAUNCHER_INTERNAL,
                    versionString = finalVer
                )
            )
        } catch (e: Exception) {
            LauncherLog.e("JavaRuntimeManager", "Import custom runtime failed: ${e.message}", e)
            Result.failure(e)
        } finally {
            runCatching { stageDir.deleteRecursively() }
        }
    }

    private fun locateJavaRoot(dir: File): File? {
        if (File(dir, "bin/java").isFile) return dir
        val subDirs = dir.listFiles()?.filter { it.isDirectory } ?: return null
        if (subDirs.size == 1) {
            locateJavaRoot(subDirs.first())?.let { return it }
        }
        return subDirs.firstOrNull { File(it, "bin/java").isFile }
    }

    private fun unpackTarStreamSafe(
        tarIn: TarArchiveInputStream,
        destDir: File,
        onProgress: (Float, String) -> Unit
    ) {
        val destCanon = destDir.canonicalFile
        val buffer = ByteArray(32768)
        var fileCount = 0
        var entry = tarIn.nextEntry
        while (entry != null) {
            val cleanName = entry.name.removePrefix("./").removePrefix("/")
            if (cleanName.isNotEmpty() && cleanName != ".") {
                val targetFile = File(destCanon, cleanName).canonicalFile
                if (!targetFile.path.startsWith(destCanon.path + File.separator)) {
                    throw SecurityException("Đường dẫn tar không hợp lệ: $cleanName")
                }
                if (entry.isSymbolicLink) {
                    val parentDir = targetFile.parentFile ?: destCanon
                    val linkDestCanon = File(parentDir, entry.linkName).canonicalFile
                    if (linkDestCanon.path == destCanon.path || linkDestCanon.path.startsWith(destCanon.path + File.separator)) {
                        runCatching {
                            parentDir.mkdirs()
                            if (targetFile.exists()) targetFile.delete()
                            Os.symlink(entry.linkName, targetFile.absolutePath)
                        }
                    }
                } else if (entry.isDirectory) {
                    targetFile.mkdirs()
                } else {
                    targetFile.parentFile?.mkdirs()
                    FileOutputStream(targetFile).use { out ->
                        var len: Int
                        while (tarIn.read(buffer).also { len = it } != -1) {
                            out.write(buffer, 0, len)
                        }
                    }
                }
                fileCount++
                if (fileCount % 25 == 0) {
                    val ratio = (fileCount / 650f).coerceIn(0f, 0.98f)
                    onProgress(0.1f + ratio * 0.7f, "Đang giải nén gói Java ($fileCount tệp)...")
                }
            }
            entry = tarIn.nextEntry
        }
    }

    suspend fun extractRuntime(
        context: Context,
        runtimeName: String,
        onProgressString: (String) -> Unit
    ): Result<File> = extractRuntime(context, runtimeName) { _, msg -> onProgressString(msg) }

    suspend fun extractRuntime(
        context: Context,
        runtimeName: String,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val destDir = getRuntimeHome(context, runtimeName)
            if (isRuntimeInstalled(context, runtimeName)) {
                ensureExecutablePermissionsOnce(destDir)
                return@withContext Result.success(destDir)
            }

            // Bersihkan direktori runtime lama yang mungkin rusak / salah permissions
            if (destDir.exists()) {
                destDir.deleteRecursively()
            }
            destDir.mkdirs()
            val arch = getDeviceArch()
            LauncherLog.event("Runtime", "Mengekstrak Java $runtimeName (arsitektur $arch) ke ${destDir.absolutePath}")
            val assetPath = "runtimes/$runtimeName"

            // 1. Unpack universal.tar.xz (libraries, modules, config) - 0% to 55%
            onProgress(0.05f, "Mengekstrak Java Runtime ($runtimeName universal)...")
            val universalName = "$assetPath/universal.tar.xz"
            try {
                context.assets.open(universalName).use { input ->
                    unpackTarXz(
                        inputStream = input,
                        destDir = destDir,
                        baseProgress = 0.05f,
                        targetProgress = 0.55f,
                        estimatedTotal = 600,
                        label = "Mengekstrak runtime universal ($runtimeName)",
                        onProgress = onProgress
                    )
                }
            } catch (e: Exception) {
                LauncherLog.e("JavaRuntimeManager", "Gagal unpack universal.tar.xz: ${e.message}", e)
                throw e
            }

            // 2. Unpack bin-$arch.tar.xz (binaries, libjli, libjvm for arch) - 55% to 75%
            onProgress(0.55f, "Mengekstrak Java Runtime ($runtimeName bin-$arch)...")
            val binName = "$assetPath/bin-$arch.tar.xz"
            try {
                context.assets.open(binName).use { input ->
                    unpackTarXz(
                        inputStream = input,
                        destDir = destDir,
                        baseProgress = 0.55f,
                        targetProgress = 0.75f,
                        estimatedTotal = 80,
                        label = "Mengekstrak native binary ($runtimeName $arch)",
                        onProgress = onProgress
                    )
                }
            } catch (e: Exception) {
                LauncherLog.e("JavaRuntimeManager", "Gagal unpack bin-$arch.tar.xz: ${e.message}", e)
                throw e
            }

            // 3. Unpack file Pack200 (.pack) jika ada (khusus Java 8) - 75% to 92%
            unpackPack200Files(
                context = context,
                runtimeDir = destDir,
                baseProgress = 0.75f,
                targetProgress = 0.92f,
                onProgress = onProgress
            )

            // 4. Normalisasi tata letak arsitektur Java 8 (lib/aarch64 -> lib/)
            normalizeJava8ArchLayout(destDir)

            // 5. Mark executables & permissions across bin and lib folders
            onProgress(0.96f, "Memverifikasi izin sistem OpenJDK...")
            ensureExecutablePermissions(destDir)
            runCatching {
                File(destDir, ".nux_perm_v2").writeText("1.0.9")
            }

            if (!isRuntimeInstalled(context, runtimeName)) {
                LauncherLog.e("Runtime", "Verifikasi Java $runtimeName tidak lengkap setelah ekstraksi")
                return@withContext Result.failure(Exception("Verifikasi OpenJDK $runtimeName tidak lengkap setelah ekstraksi."))
            }

            onProgress(1.0f, "Ekstraksi OpenJDK $runtimeName selesai!")
            LauncherLog.event("Runtime", "Java $runtimeName siap dipakai")
            Result.success(destDir)
        } catch (e: Exception) {
            LauncherLog.e("Runtime", "Ekstraksi Java $runtimeName gagal: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Membongkar seluruh file .pack (Pack200) menjadi file .jar (diperlukan untuk Java 8).
     */
    private suspend fun unpackPack200Files(
        context: Context,
        runtimeDir: File,
        baseProgress: Float,
        targetProgress: Float,
        onProgress: (Float, String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val packFiles = runtimeDir.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".pack") }
            .toList()

        if (packFiles.isEmpty()) return@withContext

        // Temukan biner unpack200 dari nativeLibraryDir aplikasi atau fallback bin/unpack200
        val nativeUnpack = File(context.applicationInfo.nativeLibraryDir, "libunpack200.so")
        val internalUnpack = File(runtimeDir, "bin/unpack200")
        if (internalUnpack.exists()) {
            ensureExecutablePermissions(runtimeDir)
        }

        val unpackCmd = when {
            nativeUnpack.exists() && nativeUnpack.canExecute() -> nativeUnpack.absolutePath
            internalUnpack.exists() -> internalUnpack.absolutePath
            nativeUnpack.exists() -> nativeUnpack.absolutePath
            else -> "libunpack200.so"
        }

        val total = packFiles.size
        packFiles.forEachIndexed { index, packFile ->
            val jarFile = File(packFile.parentFile, packFile.name.removeSuffix(".pack"))
            val p = baseProgress + (index.toFloat() / total) * (targetProgress - baseProgress)
            onProgress(p, "Membongkar bytecode Java 8: ${jarFile.name} (${index + 1}/$total)...")

            try {
                val pb = ProcessBuilder(unpackCmd, "-r", packFile.absolutePath, jarFile.absolutePath)
                pb.directory(runtimeDir)
                pb.redirectErrorStream(true)
                val process = pb.start()
                val exitCode = process.waitFor()
                if (exitCode != 0) {
                    LauncherLog.w("JavaRuntimeManager", "unpack200 exit code $exitCode for ${packFile.name}")
                }
            } catch (e: Exception) {
                LauncherLog.e("JavaRuntimeManager", "Gagal unpack200 ${packFile.name}: ${e.message}", e)
            }
        }
    }

    /**
     * Menghubungkan subdirektori arsitektur pada Java 8 (seperti lib/aarch64/)
     * ke direktori lib/ utama agar kompatibel dengan pemanggilan libjvm.so dan libjli.so standar.
     */
    private fun normalizeJava8ArchLayout(runtimeDir: File) {
        val libDir = File(runtimeDir, "lib")
        if (!libDir.exists() || !libDir.isDirectory) return

        val archNames = listOf("aarch64", "aarch32", "arm", "i386", "amd64", "x86_64")
        val archFolder = archNames.map { File(libDir, it) }.firstOrNull { it.exists() && it.isDirectory }
            ?: return

        // 1. Hubungkan server / client JVM folder
        val serverFolder = File(archFolder, "server")
        val clientFolder = File(archFolder, "client")
        val targetServer = File(libDir, "server")
        val targetClient = File(libDir, "client")

        if (serverFolder.exists() && !targetServer.exists()) {
            try {
                Os.symlink(serverFolder.absolutePath, targetServer.absolutePath)
            } catch (_: Throwable) {
                try { serverFolder.copyRecursively(targetServer, overwrite = true) } catch (_: Throwable) {}
            }
        }
        if (clientFolder.exists() && !targetClient.exists()) {
            try {
                Os.symlink(clientFolder.absolutePath, targetClient.absolutePath)
            } catch (_: Throwable) {
                try { clientFolder.copyRecursively(targetClient, overwrite = true) } catch (_: Throwable) {}
            }
        }

        // 2. Hubungkan jli folder
        val jliFolder = File(archFolder, "jli")
        val targetJli = File(libDir, "jli")
        if (jliFolder.exists() && !targetJli.exists()) {
            try {
                Os.symlink(jliFolder.absolutePath, targetJli.absolutePath)
            } catch (_: Throwable) {
                try { jliFolder.copyRecursively(targetJli, overwrite = true) } catch (_: Throwable) {}
            }
        }

        // 3. Hubungkan setiap shared library (.so) yang ada di archFolder ke libDir
        archFolder.listFiles { f -> f.isFile && f.extension == "so" }?.forEach { soFile ->
            val targetSo = File(libDir, soFile.name)
            if (!targetSo.exists()) {
                try {
                    Os.symlink(soFile.absolutePath, targetSo.absolutePath)
                } catch (_: Throwable) {
                    try { soFile.copyTo(targetSo, overwrite = true) } catch (_: Throwable) {}
                }
            }
        }
    }

    private fun unpackTarXz(
        inputStream: InputStream,
        destDir: File,
        baseProgress: Float = 0f,
        targetProgress: Float = 1f,
        estimatedTotal: Int = 100,
        label: String = "Mengekstrak",
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ) {
        TarArchiveInputStream(XZCompressorInputStream(inputStream)).use { tarIn ->
            val buffer = ByteArray(32768)
            var entry = tarIn.nextEntry
            var fileCount = 0
            while (entry != null) {
                val cleanName = entry.name.removePrefix("./").removePrefix("/")
                if (cleanName.isNotEmpty() && cleanName != ".") {
                    val targetFile = File(destDir, cleanName)

                    if (entry.isSymbolicLink) {
                        try {
                            if (targetFile.exists()) targetFile.delete()
                            Os.symlink(entry.linkName, targetFile.absolutePath)
                        } catch (_: Throwable) {}
                    } else if (entry.isDirectory) {
                        targetFile.mkdirs()
                    } else {
                        targetFile.parentFile?.mkdirs()
                        FileOutputStream(targetFile).use { out ->
                            var len: Int
                            while (tarIn.read(buffer).also { len = it } != -1) {
                                out.write(buffer, 0, len)
                            }
                        }
                        if (cleanName.startsWith("bin/") || cleanName.endsWith(".so") || cleanName.contains(".so.")) {
                            try {
                                Os.chmod(targetFile.absolutePath, 493) // 0755
                            } catch (_: Throwable) {}
                            targetFile.setExecutable(true, false)
                            targetFile.setReadable(true, false)
                        }
                    }
                    fileCount++
                    val ratio = (fileCount.toFloat() / estimatedTotal).coerceIn(0f, 0.98f)
                    val p = baseProgress + ratio * (targetProgress - baseProgress)
                    val fileName = cleanName.substringAfterLast('/')
                    onProgress(p, "$label: $fileName ($fileCount file)")
                }
                entry = tarIn.nextEntry
            }
        }
    }
}

// ----------------------------------------------------------------------------
// Section: core/instance/InstanceManager.kt
// ----------------------------------------------------------------------------

object InstanceManager {
    private val gson = Gson()
    private val _instances = MutableStateFlow<List<Instance>>(emptyList())
    val instances: StateFlow<List<Instance>> = _instances.asStateFlow()

    private val _selectedInstance = MutableStateFlow<Instance?>(null)
    val selectedInstance: StateFlow<Instance?> = _selectedInstance.asStateFlow()

    private val migrationLock = Any()
    @Volatile
    private var migrationChecked = false

    fun getNovaDir(context: Context): File = getNuxDir(context)

    fun getNuxDir(context: Context): File {
        // 1. Primary app-scoped external storage (Android/data/<pkg>/files/novacraft):
        // 100% readable/writable on Android 11..15+ with zero storage permissions required.
        val appExt = context.getExternalFilesDir(null)
        if (appExt != null) {
            val novaExtDir = File(appExt, "novacraft")
            if (novaExtDir.exists() || novaExtDir.mkdirs()) {
                ensureNoMedia(novaExtDir)
                return novaExtDir
            }
        }

        // 2. Fallback: internal app storage (/data/data/<pkg>/files/novacraft)
        val internalDir = File(context.filesDir, "novacraft")
        if (!internalDir.exists()) internalDir.mkdirs()
        ensureNoMedia(internalDir)
        return internalDir
    }

    private fun ensureNoMedia(dir: File) {
        try {
            val noMedia = File(dir, ".nomedia")
            if (!noMedia.exists()) noMedia.createNewFile()
        } catch (_: Exception) {}
    }

    fun getAssetsDir(context: Context): File {
        val dir = File(getNuxDir(context), "assets")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getLibrariesDir(context: Context): File {
        val dir = File(getNuxDir(context), "libraries")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getVersionsDir(context: Context): File {
        val dir = File(getNuxDir(context), "versions")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getInstancesDir(context: Context): File {
        val dir = File(getNuxDir(context), "instances")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getInstanceDir(context: Context, instance: Instance): File {
        return getInstanceDirById(context, instance.id)
    }

    fun getInstanceDirById(context: Context, instanceId: String): File {
        val dir = File(getInstancesDir(context), instanceId)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getInstanceGameDir(context: Context, instance: Instance): File {
        val dir = File(getInstanceDir(context, instance), "minecraft")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun openInstanceFolder(context: Context, instance: Instance) {
        val targetDir = getInstanceDir(context, instance)
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }
        openFolder(context, targetDir)
    }

    fun openFolder(context: Context, folder: File) {
        if (!folder.exists()) {
            folder.mkdirs()
        }

        val primaryStorage = android.os.Environment.getExternalStorageDirectory().absolutePath
        val relativePath = if (folder.absolutePath.startsWith(primaryStorage)) {
            folder.absolutePath.removePrefix(primaryStorage).trimStart('/')
        } else {
            folder.name
        }

        // 1. ZArchiver (priority for Android Minecraft community)
        val pm = context.packageManager
        val isZArchiverInstalled = try {
            pm.getPackageInfo("ru.zdevs.zarchiver", 0)
            true
        } catch (_: Exception) {
            false
        }

        if (isZArchiverInstalled) {
            try {
                val oldPolicy = android.os.StrictMode.getVmPolicy()
                try {
                    android.os.StrictMode.setVmPolicy(android.os.StrictMode.VmPolicy.Builder().build())
                    val zarchiverIntent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                        setClassName("ru.zdevs.zarchiver", "ru.zdevs.zarchiver.ZArchiver")
                        setDataAndType(android.net.Uri.fromFile(folder), "resource/folder")
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(zarchiverIntent)
                    return
                } finally {
                    android.os.StrictMode.setVmPolicy(oldPolicy)
                }
            } catch (e: Exception) {
                LauncherLog.w("InstanceManager", "Failed to launch ZArchiver: ${e.message}")
            }
        }

        // 2. DocumentsUI (System Files)
        try {
            val documentUri = android.provider.DocumentsContract.buildDocumentUri(
                "com.android.externalstorage.documents",
                "primary:$relativePath"
            )
            val docIntent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(documentUri, android.provider.DocumentsContract.Document.MIME_TYPE_DIR)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(docIntent)
            return
        } catch (e: Exception) {
            LauncherLog.w("InstanceManager", "Failed to launch DocumentsUI: ${e.message}")
        }

        // 3. DocumentsUI alternate MIME
        try {
            val documentUri = android.provider.DocumentsContract.buildDocumentUri(
                "com.android.externalstorage.documents",
                "primary:$relativePath"
            )
            val docIntent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(documentUri, "vnd.android.document/directory")
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(docIntent)
            return
        } catch (e: Exception) {
            LauncherLog.w("InstanceManager", "Failed to launch DocumentsUI vnd: ${e.message}")
        }

        // 4. FileProvider Chooser
        try {
            val fileUri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                folder
            )
            val chooserIntent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(fileUri, "resource/folder")
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = android.content.Intent.createChooser(chooserIntent, com.aistudio.novacraft.launcher.ui.theme.Tr.t("Buka Folder")).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            return
        } catch (e: Exception) {
            LauncherLog.w("InstanceManager", "Failed to launch FileProvider: ${e.message}")
        }

        // 5. Fallback: Copy to clipboard
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Folder Path", folder.absolutePath)
            clipboard.setPrimaryClip(clip)
            com.aistudio.novacraft.launcher.ui.theme.Toast.makeText(
                context,
                "Lokasi folder disalin ke clipboard:\n${folder.absolutePath}",
                com.aistudio.novacraft.launcher.ui.theme.Toast.LENGTH_LONG
            ).show()
        } catch (_: Exception) {
            com.aistudio.novacraft.launcher.ui.theme.Toast.makeText(context, "Folder: ${folder.absolutePath}", com.aistudio.novacraft.launcher.ui.theme.Toast.LENGTH_LONG).show()
        }
    }

    // Alias for backward compatibility
    fun getMinecraftDir(context: Context): File {
        return getNuxDir(context)
    }

    private fun getInstancesFile(context: Context): File {
        return File(getNuxDir(context), "instances.json")
    }

    private fun migrateOldStorage(context: Context) {
        if (migrationChecked) return
        synchronized(migrationLock) {
            if (migrationChecked) return
            try {
                val targetDir = getNuxDir(context)
                val marker = File(targetDir, ".novacraft_migrated")
                if (marker.isFile) {
                    migrationChecked = true
                    return
                }

                val candidates = mutableListOf<File>()
                context.getExternalFilesDir(null)?.let { ext ->
                    candidates.add(File(ext, "nuxlauncher"))
                    candidates.add(File(ext, "amberlauncher"))
                }
                @Suppress("DEPRECATION")
                try {
                    val publicRoot = android.os.Environment.getExternalStorageDirectory()
                    if (publicRoot != null) {
                        val legacyPublic = File(publicRoot, "nuxlauncher")
                        if (legacyPublic.isDirectory && legacyPublic.canRead()) {
                            candidates.add(legacyPublic)
                        }
                    }
                } catch (_: SecurityException) {
                } catch (_: Exception) {
                }
                candidates.add(File(context.filesDir, "nuxlauncher"))
                candidates.add(File(context.dataDir ?: context.filesDir, "nuxlauncher"))
                candidates.add(File(context.filesDir, ".minecraft"))

                val targetCanonical = try { targetDir.canonicalPath } catch (_: Exception) { targetDir.absolutePath }

                for (oldLoc in candidates) {
                    if (!oldLoc.isDirectory || !oldLoc.canRead()) continue
                    val oldCanonical = try { oldLoc.canonicalPath } catch (_: Exception) { oldLoc.absolutePath }
                    if (oldCanonical == targetCanonical) continue

                    // Fast rename when on same filesystem & target subfolder doesn't exist yet, else safe copy
                    for (sub in listOf("assets", "libraries", "versions")) {
                        val oldSub = File(oldLoc, sub)
                        val newSub = File(targetDir, sub)
                        if (oldSub.isDirectory && (!newSub.exists() || newSub.list().isNullOrEmpty())) {
                            if (newSub.exists()) newSub.deleteRecursively()
                            if (!oldSub.renameTo(newSub)) {
                                oldSub.copyRecursively(newSub, overwrite = true)
                            }
                        }
                    }

                    val oldInstances = File(oldLoc, "instances")
                    if (oldInstances.isDirectory) {
                        val newInstances = File(targetDir, "instances")
                        if (!newInstances.exists()) newInstances.mkdirs()
                        oldInstances.listFiles()?.forEach { instFolder ->
                            if (instFolder.isDirectory) {
                                val targetInstDir = File(newInstances, instFolder.name)
                                if (!targetInstDir.exists() || targetInstDir.list().isNullOrEmpty()) {
                                    if (targetInstDir.exists()) targetInstDir.deleteRecursively()
                                    if (!instFolder.renameTo(targetInstDir)) {
                                        instFolder.copyRecursively(targetInstDir, overwrite = true)
                                    }
                                }
                            }
                        }
                    }

                    val oldInstJson = File(oldLoc, "instances.json")
                    val newInstJson = File(targetDir, "instances.json")
                    if (oldInstJson.isFile && (!newInstJson.exists() || newInstJson.length() == 0L)) {
                        oldInstJson.copyTo(newInstJson, overwrite = true)
                    }

                    val oldBackup = File(oldLoc, "controls_backup")
                    val newBackup = File(targetDir, "controls_backup")
                    if (oldBackup.isDirectory && !newBackup.exists()) {
                        oldBackup.copyRecursively(newBackup, overwrite = true)
                    }
                }

                try { marker.writeText("1") } catch (_: Exception) {}
                migrationChecked = true
            } catch (e: Exception) {
                LauncherLog.w("InstanceManager", "Migrasi folder data ke novacraft gagal sebagian: ${e.message}")
            }
        }
    }

    private const val PREFS_NAME = "novacraft_prefs"
    private const val LEGACY_PREFS_NAME = "nux_launcher_prefs"
    private const val KEY_LAST_SELECTED_INSTANCE = "last_selected_instance_id"
    private var appContext: Context? = null

    private fun saveSelectedInstanceId(context: Context, id: String) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_LAST_SELECTED_INSTANCE, id).apply()
            File(context.filesDir, "selected_instance_id.txt").writeText(id)
        } catch (_: Exception) {}
    }

    private fun loadSelectedInstanceId(context: Context): String? {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val id = prefs.getString(KEY_LAST_SELECTED_INSTANCE, null)
                ?: context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
                    .getString(KEY_LAST_SELECTED_INSTANCE, null)
            if (!id.isNullOrBlank()) return id
            val file = File(context.filesDir, "selected_instance_id.txt")
            if (file.exists()) {
                val text = file.readText().trim()
                if (text.isNotEmpty()) return text
            }
        } catch (_: Exception) {}
        return null
    }

    fun init(context: Context) {
        appContext = context.applicationContext
        migrateOldStorage(context)

        val file = getInstancesFile(context)
        var loadedList = mutableListOf<Instance>()
        if (file.exists()) {
            try {
                val json = file.readText()
                val type = object : TypeToken<List<Instance>>() {}.type
                val list: List<Instance>? = gson.fromJson(json, type)
                if (list != null) {
                    loadedList.addAll(list.filterNot { it.id == "instance_default" || it.name == "Minecraft 1.20.4" })
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Scan instances directory for isolated instance.json (like Windows version)
        val instancesDir = getInstancesDir(context)
        instancesDir.listFiles()?.forEach { instFolder ->
            if (instFolder.isDirectory) {
                val configFile = File(instFolder, "instance.json")
                if (configFile.exists() && loadedList.none { it.id == instFolder.name }) {
                    try {
                        val inst = gson.fromJson(configFile.readText(), Instance::class.java)
                        if (inst != null) {
                            loadedList.add(inst)
                        }
                    } catch (_: Exception) {}
                }
            }
        }

        // Check if downloaded client JAR and loader exist for each instance
        val verifiedList = loadedList.map { inst ->
            inst.copy(isDownloaded = isInstanceDownloaded(context, inst))
        }

        _instances.value = verifiedList

        // Preserve and restore the user's last selected instance
        val savedId = loadSelectedInstanceId(context)
        val currentSelectedId = _selectedInstance.value?.id ?: savedId

        val targetInstance = verifiedList.find { it.id == currentSelectedId }
            ?: (if (savedId != null) verifiedList.find { it.id == savedId } else null)
            ?: verifiedList.firstOrNull()

        _selectedInstance.value = targetInstance
        targetInstance?.let { saveSelectedInstanceId(context, it.id) }
        save(context)
    }

    fun isInstanceDownloaded(context: Context, inst: Instance): Boolean {
        // Mesin Rust tidak memakai client.jar maupun library Java: tidak ada yang perlu diunduh launcher
        if (com.aistudio.novacraft.launcher.core.mods.HorizonEngine.isEngine(inst.loader)) return true
        val versionsDir = getVersionsDir(context)
        val vDir = File(versionsDir, inst.mcVersion)
        val jarFile = File(vDir, "${inst.mcVersion}.jar")
        val jsonFile = File(vDir, "${inst.mcVersion}.json")
        if (!jarFile.exists() || jarFile.length() == 0L || !jsonFile.exists() || jsonFile.length() == 0L) {
            return false
        }
        if (inst.loader.equals("fabric", ignoreCase = true)) {
            val fabricJson = if (inst.loaderVersion.isNotBlank()) {
                File(vDir, "fabric-${inst.loaderVersion}.json")
            } else {
                vDir.listFiles { f -> f.name.startsWith("fabric-") && f.name.endsWith(".json") }?.firstOrNull()
            }
            if (fabricJson == null || !fabricJson.exists() || fabricJson.length() == 0L) {
                return false
            }
            val libDir = getLibrariesDir(context)
            val fabricLoaderDir = File(libDir, "net/fabricmc/fabric-loader")
            if (!fabricLoaderDir.exists() || fabricLoaderDir.listFiles().isNullOrEmpty()) {
                return false
            }
        }
        val forgeFlavor = com.aistudio.novacraft.launcher.core.launch.ForgeFlavor.of(inst.loader)
        if (forgeFlavor != null) {
            // Hanya cek keberadaan berkas (fungsi ini dipanggil dari komposisi UI)
            if (!com.aistudio.novacraft.launcher.core.launch.ForgeInstaller.isInstalled(context, inst.mcVersion, inst.loaderVersion, forgeFlavor)) {
                return false
            }
        }
        return true
    }


    fun createInstance(context: Context, instance: Instance) {
        // Create isolated instance folder structure
        val instDir = getInstanceDir(context, instance)
        val gameDir = getInstanceGameDir(context, instance)
        instDir.mkdirs()
        gameDir.mkdirs()

        // Write isolated instance.json
        try {
            File(instDir, "instance.json").writeText(gson.toJson(instance))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val list = _instances.value.toMutableList()
        list.add(0, instance)
        _instances.value = list
        _selectedInstance.value = instance
        saveSelectedInstanceId(context, instance.id)
        save(context)
    }

    fun selectInstance(instance: Instance) {
        _selectedInstance.value = instance
        appContext?.let { saveSelectedInstanceId(it, instance.id) }
    }

    fun updateInstance(context: Context, updated: Instance) {
        val instDir = getInstanceDir(context, updated)
        try {
            File(instDir, "instance.json").writeText(gson.toJson(updated))
        } catch (_: Exception) {}

        val list = _instances.value.map { if (it.id == updated.id) updated else it }
        _instances.value = list
        if (_selectedInstance.value?.id == updated.id) {
            _selectedInstance.value = updated
        }
        save(context)
    }

    fun deleteInstance(context: Context, instanceId: String) {
        val list = _instances.value.filterNot { it.id == instanceId }
        _instances.value = list
        if (_selectedInstance.value?.id == instanceId) {
            val nextInst = list.firstOrNull()
            _selectedInstance.value = nextInst
            nextInst?.let { saveSelectedInstanceId(context, it.id) }
        }
        save(context)

        // Delete isolated instance directory and all contents from storage
        try {
            val instDir = getInstanceDirById(context, instanceId)
            if (instDir.exists()) {
                instDir.deleteRecursively()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun save(context: Context) {
        try {
            val file = getInstancesFile(context)
            com.aistudio.novacraft.launcher.core.utils.AtomicFiles.writeText(file, gson.toJson(_instances.value))
            // Also sync each instance.json in its isolated directory
            _instances.value.forEach { inst ->
                try {
                    val instDir = getInstanceDir(context, inst)
                    com.aistudio.novacraft.launcher.core.utils.AtomicFiles.writeText(File(instDir, "instance.json"), gson.toJson(inst))
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

// ----------------------------------------------------------------------------
// Section: core/launch/LwjglManager.kt
// ----------------------------------------------------------------------------

object LwjglManager {

    // libshaderc.so identik (byte demi byte) di semua versi LWJGL, jadi APK hanya membawa satu salinan
    // (di folder 3.3.3) dan disalin ke folder natives versi lain saat diekstrak. Hemat sekitar 3 MB APK.
    private const val SHARED_NATIVE_NAME = "libshaderc.so"
    private const val SHARED_NATIVE_ASSET = "app_runtime/lwjgl/3.3.3/natives/libshaderc.so"

    fun getLwjglVersion(mcVersion: String, versionJsonFile: File? = null): String {
        if (versionJsonFile != null && versionJsonFile.exists()) {
            try {
                val content = versionJsonFile.readText()
                if (content.contains("org.lwjgl:lwjgl:3.4.") || content.contains("org.lwjgl.lwjgl:lwjgl:3.4.")) {
                    return "3.4.1"
                }
            } catch (_: Exception) {}
        }
        if (mcVersion.startsWith("26.") || mcVersion.startsWith("25w") || mcVersion.startsWith("24w") || mcVersion.contains("snapshot")) {
            return "3.4.1"
        }
        return "3.3.3"
    }

    /**
     * Versi yang memakai LWJGL 2 (Minecraft 1.12.2 ke bawah) butuh jembatan lwjglx di classpath supaya
     * kelas LWJGL 2 seperti org.lwjgl.opengl.Display dan org.lwjgl.input.Keyboard tersedia di atas LWJGL 3.
     * Tanpa itu game langsung crash dengan NoClassDefFoundError. Penentu utamanya berkas versi Mojang
     * (library org.lwjgl.lwjgl:lwjgl:2.x); nomor versi dipakai hanya bila berkasnya belum terbaca.
     */
    fun usesLwjgl2(mcVersion: String, versionJsonFile: File? = null): Boolean {
        if (versionJsonFile != null && versionJsonFile.exists()) {
            try {
                val content = versionJsonFile.readText()
                if (content.contains("org.lwjgl.lwjgl:lwjgl:2.")) return true
                if (content.contains("org.lwjgl:lwjgl:3.")) return false
            } catch (_: Exception) {}
        }
        val minor = Regex("""^1\.(\d+)""").find(mcVersion)?.groupValues?.get(1)?.toIntOrNull() ?: return false
        return minor <= 12
    }

    fun prepareLauncherComponents(context: Context): File? {
        val launcherDir = File(context.filesDir, "components/launcher")
        launcherDir.mkdirs()
        val patcher = File(launcherDir, "MioLibPatcher.jar")
        try {
            context.assets.open("components/launcher/MioLibPatcher.jar").use { input ->
                val assetLen = input.available().toLong()
                if (!patcher.exists() || patcher.length() != assetLen) {
                    FileOutputStream(patcher).use { output ->
                        input.copyTo(output)
                    }
                }
            }
        } catch (_: Exception) {}
        prepareLaunchWrapper(context)
        return patcher
    }

    fun prepareLaunchWrapper(context: Context): File? {
        val launcherDir = File(context.filesDir, "components/launcher")
        launcherDir.mkdirs()
        val wrapper = File(launcherDir, "MioLaunchWrapper.jar")
        try {
            context.assets.open("components/launcher/MioLaunchWrapper.jar").use { input ->
                val assetLen = input.available().toLong()
                if (!wrapper.exists() || wrapper.length() != assetLen) {
                    FileOutputStream(wrapper).use { output ->
                        input.copyTo(output)
                    }
                }
            }
        } catch (_: Exception) {
            return null
        }
        return wrapper
    }

    fun prepareAuthLibInjector(context: Context): File? {
        val authDir = File(context.filesDir, "components/auth_libs")
        authDir.mkdirs()
        val jar = File(authDir, "authlib-injector.jar")
        if (!jar.exists() || jar.length() == 0L) {
            try {
                context.assets.open("components/auth_libs/authlib-injector.jar").use { input ->
                    FileOutputStream(jar).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (_: Exception) {
                return null
            }
        }
        return jar
    }

    fun prepareLwjgl(context: Context, lwjglVersion: String = "3.3.3"): File {
        val targetDir = File(context.filesDir, "components/lwjgl/$lwjglVersion")
        val nativesDir = File(targetDir, "natives")

        val marker = File(targetDir, ".extracted")
        // Penanda saja tidak cukup: bila ekstraksi sebelumnya tidak lengkap (atau versi lama aplikasi
        // belum membawa jembatan lwjglx), berkas inti dicek dulu dan ekstraksi diulang bila ada yang hilang.
        val extractionComplete = marker.exists() &&
            File(targetDir, "lwjgl.jar").exists() &&
            File(targetDir, "lwjgl-lwjglx.jar").exists() &&
            File(nativesDir, "liblwjgl.so").exists()
        if (extractionComplete) {
            ensureSharedNatives(context, nativesDir)
            ensureSpirvCross(context, nativesDir)
            return nativesDir
        }

        targetDir.mkdirs()
        nativesDir.mkdirs()

        // Extract jars and natives from assets/app_runtime/lwjgl/$lwjglVersion
        val assetBasePath = "app_runtime/lwjgl/$lwjglVersion"
        copyAssetFolder(context, assetBasePath, targetDir)

        ensureSharedNatives(context, nativesDir)
        ensureSpirvCross(context, nativesDir)

        marker.createNewFile()
        return nativesDir
    }

    fun getLwjglJars(context: Context, lwjglVersion: String = "3.3.3", withLwjglx: Boolean = false): List<File> {
        // Idempoten dan murah bila sudah lengkap; memperbaiki ekstraksi yang tidak lengkap.
        prepareLwjgl(context, lwjglVersion)
        val targetDir = File(context.filesDir, "components/lwjgl/$lwjglVersion")
        val isLwjgl2 = withLwjglx || lwjglVersion.startsWith("2.")
        val jars = targetDir.listFiles { file ->
            file.isFile && file.name.endsWith(".jar") && (isLwjgl2 || file.name != "lwjgl-lwjglx.jar")
        } ?: emptyArray()
        return jars.sortedWith(Comparator { a, b ->
            when {
                a.name == "lwjgl.jar" -> -1
                b.name == "lwjgl.jar" -> 1
                a.name.contains("merged") -> -1
                b.name.contains("merged") -> 1
                else -> a.name.compareTo(b.name)
            }
        })
    }

    fun getNativesDir(context: Context, lwjglVersion: String = "3.3.3"): File {
        return prepareLwjgl(context, lwjglVersion)
    }

    /** Menyalin pustaka native yang dipakai bersama bila belum ada di folder natives versi ini. */
    private fun ensureSharedNatives(context: Context, nativesDir: File) {
        val dest = File(nativesDir, SHARED_NATIVE_NAME)
        if (dest.exists() && dest.length() > 0L) return
        try {
            nativesDir.mkdirs()
            context.assets.open(SHARED_NATIVE_ASSET).use { input ->
                FileOutputStream(dest).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (_: Throwable) {
            // Jangan tinggalkan berkas setengah jadi; percobaan berikutnya mengulang dari awal.
            try { dest.delete() } catch (_: Throwable) {}
        }
    }

    fun ensureSpirvCross(context: Context, nativesDir: File) {
        try {
            val nativeLibDir = File(context.applicationInfo.nativeLibraryDir)
            val spirvSrc = File(nativeLibDir, "libspirv-cross-c-shared.so")
            if (spirvSrc.exists()) {
                val targets = listOf("libspirv-cross.so", "libspirv-cross-c-shared.so")
                for (name in targets) {
                    val dest = File(nativesDir, name)
                    if (!dest.exists() || dest.length() == 0L) {
                        try {
                            android.system.Os.symlink(spirvSrc.absolutePath, dest.absolutePath)
                        } catch (_: Throwable) {
                            try {
                                spirvSrc.copyTo(dest, overwrite = true)
                            } catch (_: Throwable) {}
                        }
                    }
                }
            }
        } catch (_: Throwable) {}
    }

    private fun copyAssetFolder(context: Context, assetFolder: String, targetFolder: File) {
        val assets = context.assets.list(assetFolder) ?: return
        for (item in assets) {
            val subAssetPath = "$assetFolder/$item"
            val subAssets = context.assets.list(subAssetPath)
            if (subAssets != null && subAssets.isNotEmpty()) {
                val subTarget = File(targetFolder, item)
                subTarget.mkdirs()
                copyAssetFolder(context, subAssetPath, subTarget)
            } else {
                val destFile = File(targetFolder, item)
                try {
                    context.assets.open(subAssetPath).use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (e: Exception) {
                    // Try directory if open failed
                    val subTarget = File(targetFolder, item)
                    subTarget.mkdirs()
                    copyAssetFolder(context, subAssetPath, subTarget)
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------
// Section: core/launch/GameLauncher.kt
// ----------------------------------------------------------------------------

object GameLauncher {
    private val gson = Gson()

    // Batas menunggu pembaruan sesi Microsoft sebelum game tetap dijalankan.
    private const val SESSION_REFRESH_TIMEOUT_MS = 12_000L
    private val launchErrorHandler = CoroutineExceptionHandler { _, throwable ->
        LauncherLog.e("Peluncuran", "Kesalahan coroutine peluncuran game", throwable)
    }
    private val refreshScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate + launchErrorHandler)
    private val refreshing = AtomicBoolean(false)
    private val launching = AtomicBoolean(false)

    /**
     * Menjalankan game. Untuk akun Microsoft yang token Minecraft-nya sudah lama, sesi diperbarui
     * dulu (token hanya berlaku sekitar 24 jam); tanpa ini server online menolak dengan "Invalid session".
     * Bila pembaruan gagal atau terlalu lama, game tetap dijalankan dengan token yang ada.
     */
    fun launch(context: Context, instance: Instance, account: UserAccount) {
        val currentSettings = com.aistudio.novacraft.launcher.core.utils.SettingsManager.settings.value
        val offlineMode = currentSettings.offlineMode
        val localOnlyMode = currentSettings.localOnlyMode
        val effectiveAccount = AccountManager.resolveLaunchAccount(account, offlineMode)
        // Jaring pengaman: instance mesin Rust tidak lewat JVM (tombol PLAY di Beranda sudah mencabangkannya lebih dulu)
        if (HorizonEngine.isEngine(instance.loader)) {
            HorizonEngine.launch(context, instance, effectiveAccount)
            return
        }
        LauncherLog.event(
            "Peluncuran",
            "PLAY ditekan: instance=" + instance.name + ", Minecraft " + instance.mcVersion +
                ", loader=" + instance.loader + " " + instance.loaderVersion + ", akun=" + effectiveAccount.safeAccountType +
                (if (offlineMode) " (mode offline)" else " (mode online)") +
                (if (localOnlyMode) " [Local-Only]" else "")
        )
        if (localOnlyMode || offlineMode || effectiveAccount.safeAccountType == "offline" || !MicrosoftAuthService.needsRefresh(effectiveAccount)) {
            launchNow(context, instance, effectiveAccount)
            return
        }
        // Ketukan ganda pada tombol main tidak boleh memulai dua pembaruan sekaligus.
        if (!refreshing.compareAndSet(false, true)) {
            LauncherLog.event("Peluncuran", "Ketukan PLAY diabaikan: pembaruan sesi Microsoft masih berjalan")
            return
        }

        val appContext = context.applicationContext
        Toast.show(appContext, "Memperbarui sesi Microsoft...", Toast.LENGTH_SHORT)
        refreshScope.launch {
            var launchAccount = account
            try {
                // Hasil disimpan di dalam tugas ini, jadi tetap tersimpan walau selesai setelah batas waktu.
                val job = async(Dispatchers.IO) {
                    MicrosoftAuthService.refreshMicrosoftAccount(account).also { result ->
                        result.getOrNull()?.let { fresh -> AccountManager.updateAccount(appContext, fresh) }
                    }
                }
                val result = withTimeoutOrNull(SESSION_REFRESH_TIMEOUT_MS) { job.await() }
                val fresh = result?.getOrNull()
                if (fresh != null) {
                    launchAccount = fresh
                    LauncherLog.event("Peluncuran", "Sesi Microsoft diperbarui sebelum main")
                } else {
                    LauncherLog.w("GameLauncher", "Sesi Microsoft tidak diperbarui: ${result?.exceptionOrNull()?.message ?: "melebihi batas waktu"}", result?.exceptionOrNull())
                    Toast.show(
                        appContext,
                        "Sesi Microsoft belum bisa diperbarui. Game tetap dijalankan, tetapi fitur online mungkin butuh login ulang.",
                        Toast.LENGTH_LONG
                    )
                }
            } catch (e: Throwable) {
                LauncherLog.w("GameLauncher", "Pembaruan sesi Microsoft gagal", e)
            } finally {
                refreshing.set(false)
            }
            launchNow(appContext, instance, launchAccount)
        }
    }

    private fun launchNow(context: Context, instance: Instance, account: UserAccount) {
        if (!launching.compareAndSet(false, true)) {
            LauncherLog.event("Peluncuran", "Ketukan PLAY diabaikan: persiapan ${instance.name} masih berjalan")
            return
        }
        val appContext = context.applicationContext
        refreshScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    launchPrepared(appContext, instance, account)
                }
            } catch (t: Throwable) {
                LauncherLog.e("Peluncuran", "Persiapan ${instance.name} gagal sebelum game dimulai", t)
                Toast.show(
                    appContext,
                    "Gagal meluncurkan ${instance.name}: ${t.localizedMessage ?: t.javaClass.simpleName}",
                    Toast.LENGTH_LONG
                )
            } finally {
                launching.set(false)
            }
        }
    }

    private fun launchPrepared(context: Context, instance: Instance, account: UserAccount) {
        val gameDir = InstanceManager.getInstanceGameDir(context, instance)
        val versionDir = File(InstanceManager.getVersionsDir(context), instance.mcVersion)
        val versionJson = File(versionDir, "${instance.mcVersion}.json")

        var mainClass = "net.minecraft.client.main.Main"
        var assetIndexId = instance.mcVersion
        var mojangJavaMajor: Int? = null
        var versionDetail: VersionDetail? = null

        if (versionJson.exists()) {
            try {
                val detail = gson.fromJson(versionJson.readText(), VersionDetail::class.java)
                versionDetail = detail
                detail.mainClass?.let { mainClass = it }
                detail.assetIndex?.id?.let { assetIndexId = it }
                mojangJavaMajor = detail.javaVersion?.majorVersion
            } catch (e: Exception) {
                LauncherLog.w("Peluncuran", "Berkas versi ${versionJson.name} tidak terbaca: ${e.message}", e)
            }
        } else {
            LauncherLog.w("Peluncuran", "Berkas versi tidak ada: ${versionJson.absolutePath} (instance belum diunduh?)")
        }

        val isFabric = instance.loader.equals("fabric", ignoreCase = true)
        var fabricProfile: com.aistudio.novacraft.launcher.core.utils.FabricProfile? = null

        if (isFabric) {
            val fabricJsonFile = File(versionDir, "fabric-${instance.loaderVersion}.json")
            if (fabricJsonFile.exists()) {
                try {
                    fabricProfile = gson.fromJson(fabricJsonFile.readText(), com.aistudio.novacraft.launcher.core.utils.FabricProfile::class.java)
                } catch (_: Exception) {}
            }
            if (fabricProfile == null) {
                val files = versionDir.listFiles { f -> f.name.startsWith("fabric-") && f.name.endsWith(".json") }
                if (!files.isNullOrEmpty()) {
                    try {
                        fabricProfile = gson.fromJson(files.first().readText(), com.aistudio.novacraft.launcher.core.utils.FabricProfile::class.java)
                    } catch (_: Exception) {}
                }
            }

            if (fabricProfile == null) {
                Toast.show(
                    context,
                    "Profil Fabric (${instance.loaderVersion}) belum terpasang. Silakan tekan tombol UNDUH pada instance!",
                    Toast.LENGTH_LONG
                )
                return
            }

            fabricProfile.mainClass?.let { mainClass = it } ?: run {
                mainClass = "net.fabricmc.loader.impl.launch.knot.KnotClient"
            }
        }

        // Forge/NeoForge: profil dibuat saat instance diunduh; peluncuran hanya membaca berkas kecil itu
        val forgeFlavor = com.aistudio.novacraft.launcher.core.launch.ForgeFlavor.of(instance.loader)
        var forgeLaunch: com.aistudio.novacraft.launcher.core.launch.ForgeLaunchInfo? = null
        if (forgeFlavor != null) {
            val forgeProfileFile = com.aistudio.novacraft.launcher.core.launch.ForgeInstaller.findProfileFile(
                context, instance.mcVersion, instance.loaderVersion, forgeFlavor
            )
            forgeLaunch = forgeProfileFile?.let {
                com.aistudio.novacraft.launcher.core.launch.ForgeLaunch.resolve(
                    it, InstanceManager.getLibrariesDir(context), instance.mcVersion
                )
            }
            if (forgeLaunch == null) {
                Toast.show(
                    context,
                    "Profil ${forgeFlavor.displayName} (${instance.loaderVersion}) belum terpasang. Silakan tekan tombol UNDUH pada instance!",
                    Toast.LENGTH_LONG
                )
                return
            }
            mainClass = forgeLaunch.mainClass
        }

        val javaConfig = com.aistudio.novacraft.launcher.core.launch.JavaRuntimeManager.configureJavaForInstance(
            context = context,
            instance = instance,
            mojangJavaMajor = mojangJavaMajor,
            persistChanges = true
        )
        val runtimeName = javaConfig.resolvedRuntimeId
        val runtimeHome = com.aistudio.novacraft.launcher.core.launch.JavaRuntimeManager.resolveRuntimeHome(
            context = context,
            runtimeName = runtimeName,
            detected = javaConfig.detectedRuntime
        )

        // Prepare Android-native LWJGL components
        val versionJsonFile = File(versionDir, "${instance.mcVersion}.json")
        val lwjglVersion = LwjglManager.getLwjglVersion(instance.mcVersion, versionJsonFile)
        val lwjglNativesDir = LwjglManager.getNativesDir(context, lwjglVersion)
        // Minecraft 1.12.2 ke bawah memakai LWJGL 2: tanpa jembatan lwjglx game crash karena kelas LWJGL tidak ditemukan
        val needsLwjglx = LwjglManager.usesLwjgl2(instance.mcVersion, versionJsonFile)
        val lwjglJars = LwjglManager.getLwjglJars(context, lwjglVersion, withLwjglx = needsLwjglx)
        LwjglManager.prepareLauncherComponents(context)

        // Build classpath: Android LWJGL jars first, Fabric libraries, Mojang non-LWJGL libraries, then client.jar
        val libDir = InstanceManager.getLibrariesDir(context)
        val classpathEntries = mutableListOf<String>()

        // 1. Android LWJGL Jars
        lwjglJars.forEach { jar ->
            classpathEntries.add(jar.absolutePath)
        }

        // Add MioLaunchWrapper.jar if available
        val wrapperJar = LwjglManager.prepareLaunchWrapper(context)
        val useWrapper = wrapperJar != null && wrapperJar.exists()
        if (useWrapper) {
            classpathEntries.add(wrapperJar.absolutePath)
        }

        // 2. Fabric Libraries
        val loaderArtifactKeys = mutableSetOf<String>()
        val missingLibs = mutableListOf<String>()
        if (isFabric && fabricProfile != null) {
            fabricProfile.libraries.forEach { lib ->
                if (lib.name.contains("org.lwjgl")) return@forEach
                val parts = lib.name.split(":")
                if (parts.size >= 2) {
                    loaderArtifactKeys.add("${parts[0]}:${parts[1]}")
                }
                val relPath = com.aistudio.novacraft.launcher.core.launch.FabricService.artifactToPath(lib.name)
                if (relPath != null) {
                    val file = File(libDir, relPath)
                    if (file.exists()) {
                        classpathEntries.add(file.absolutePath)
                    } else {
                        missingLibs.add(relPath)
                    }
                }
            }
        }

        // 2b. Forge libraries (library Mojang dengan group:artifact yang sama dilewati: versi Forge menang)
        if (forgeLaunch != null) {
            forgeLaunch.libraryFiles.forEach { classpathEntries.add(it.absolutePath) }
            loaderArtifactKeys.addAll(forgeLaunch.libraryKeys)
        }

        // 3. Mojang libraries (excluding desktop org.lwjgl and libraries already provided/overridden by mod loader)
        versionDetail?.libraries?.forEach { lib ->
            val name = lib.name ?: ""
            if (name.contains("org.lwjgl")) return@forEach

            val parts = name.split(":")
            if (parts.size >= 2) {
                val groupArtifact = "${parts[0]}:${parts[1]}"
                if (loaderArtifactKeys.contains(groupArtifact)) return@forEach
            }

            // Exclude vanilla ASM when running under Fabric (Fabric strictly provides & requires its own ASM)
            if (isFabric && (name.startsWith("org.ow2.asm:") || lib.downloads?.artifact?.path?.contains("org/ow2/asm") == true)) {
                return@forEach
            }

            val path = lib.downloads?.artifact?.path
            if (path != null) {
                val file = File(libDir, path)
                if (file.exists()) {
                    classpathEntries.add(file.absolutePath)
                } else if (lib.rules == null) {
                    // Library beraturan OS (mis. khusus Windows atau macOS) memang tidak diunduh di Android, jadi tidak dihitung.
                    missingLibs.add(path)
                }
            }
        }

        // 4. Minecraft Client JAR
        val clientJar = File(versionDir, "${instance.mcVersion}.jar")
        val launchSettings = com.aistudio.novacraft.launcher.core.utils.SettingsManager.settings.value
        if (clientJar.exists()) {
            if (!launchSettings.localOnlyMode && !launchSettings.fastGameStartup && launchSettings.verifyGameFiles) {
                val expectedSha1 = versionDetail?.downloads?.client?.sha1?.trim()?.lowercase()
                if (!expectedSha1.isNullOrBlank()) {
                    try {
                        val md = java.security.MessageDigest.getInstance("SHA-1")
                        clientJar.inputStream().use { ins ->
                            val buf = ByteArray(64 * 1024)
                            var r: Int
                            while (ins.read(buf).also { r = it } != -1) md.update(buf, 0, r)
                        }
                        val actual = md.digest().joinToString("") { "%02x".format(it) }
                        if (actual != expectedSha1) {
                            LauncherLog.w("Peluncuran", "Client JAR SHA-1 không khớp ($actual != $expectedSha1)")
                        }
                    } catch (_: Throwable) {}
                }
            } else {
                LauncherLog.i("Peluncuran", "Fast game startup / bỏ qua kiểm tra SHA-1 client JAR")
            }
            classpathEntries.add(clientJar.absolutePath)
        } else {
            LauncherLog.w("Peluncuran", "Client Minecraft tidak ada: ${clientJar.absolutePath}")
        }

        val classpath = classpathEntries.distinct().joinToString(File.pathSeparator)
        val assetsDir = InstanceManager.getAssetsDir(context)

        val userType = if (account.safeAccountType == "microsoft") "msa" else "mojang"
        val accessToken = if (account.safeAccessToken.isNotBlank()) account.safeAccessToken else "0"

        val isElyBy = account.safeAccountType == "elyby"
        val isOffline = account.safeAccountType == "offline"

        var authlibJar: File? = null
        var authlibUrl: String? = null
        var playerUuid = account.uuid

        if (isElyBy) {
            authlibJar = LwjglManager.prepareAuthLibInjector(context)
            authlibUrl = account.authServerUrl ?: "https://authserver.ely.by/api/authlib-injector"

            // Pastikan UUID adalah 32-karakter valid yang dikenali server Ely.by (lewati panggilan mạng saat Local-Only)
            if (!launchSettings.localOnlyMode && (playerUuid.length != 32 || playerUuid.toLongOrNull() != null)) {
                val realUuid = com.aistudio.novacraft.launcher.core.account.ElyByAuthService.fetchRealPlayerUuid(account.username)
                if (realUuid.isNotBlank()) {
                    playerUuid = realUuid
                    val updated = account.copy(uuid = realUuid)
                    com.aistudio.novacraft.launcher.core.account.AccountManager.updateAccount(context, updated)
                }
            }
        } else if (isOffline) {
            val skinFile = account.getSkinFile()
            val capeFile = account.getCapeFile()
            val hasCustomSkinOrCape = (skinFile != null && skinFile.exists()) || (capeFile != null && capeFile.exists())

            if (hasCustomSkinOrCape) {
                // Hentikan server sebelumnya jika masih aktif
                com.aistudio.novacraft.launcher.core.account.OfflineYggdrasilServer.activeInstance?.stop()

                val offlineServer = com.aistudio.novacraft.launcher.core.account.OfflineYggdrasilServer(0)
                offlineServer.addCharacter(
                    username = account.username,
                    uuid = playerUuid,
                    skinFile = skinFile,
                    capeFile = capeFile,
                    isSlim = account.safeSkinModel == "slim"
                )
                val port = offlineServer.start()
                if (port > 0) {
                    authlibJar = LwjglManager.prepareAuthLibInjector(context)
                    authlibUrl = "http://127.0.0.1:$port"
                    LauncherLog.i("GameLauncher", "Mengaktifkan Offline Yggdrasil server pada port $port untuk ${account.username}")
                }
            }
        }

        val installedMods = com.aistudio.novacraft.launcher.core.utils.CrashManager.getInstalledMods(gameDir.absolutePath)

        if (missingLibs.isNotEmpty()) {
            LauncherLog.w(
                "Peluncuran",
                "${missingLibs.size} library wajib tidak ada di folder libraries (unduhan belum lengkap?): " +
                    missingLibs.take(12).joinToString(", ") + (if (missingLibs.size > 12) ", ..." else "")
            )
        }
        LauncherLog.event(
            "Peluncuran",
            "Memulai game: runtime=$runtimeName (terpasang=${com.aistudio.novacraft.launcher.core.launch.JavaRuntimeManager.isRuntimeInstalled(context, runtimeName)}), " +
                "LWJGL=$lwjglVersion, lwjglx=$needsLwjglx, wrapper=$useWrapper, classpath=${classpathEntries.size} berkas, " +
                "mainClass=$mainClass, mod=${installedMods.size}"
        )

        com.aistudio.novacraft.launcher.core.utils.DiscordPresenceManager.onGameLaunching()
        val intent = Intent(context, GameActivity::class.java).apply {
            putExtra(GameActivity.EXTRA_INSTANCE_NAME, instance.name)
            putExtra(GameActivity.EXTRA_MC_VERSION, instance.mcVersion)
            putExtra(GameActivity.EXTRA_LOADER, instance.loader)
            putExtra(GameActivity.EXTRA_LOADER_VERSION, instance.loaderVersion)
            putExtra(GameActivity.EXTRA_DISCORD_RPC_PLAY, launchSettings.discordRichPresenceOnPlay)
            putExtra(GameActivity.EXTRA_DISCORD_RPC_SERVER, launchSettings.discordShowServer)
            putStringArrayListExtra(GameActivity.EXTRA_INSTALLED_MODS, ArrayList(installedMods))
            putExtra(GameActivity.EXTRA_USERNAME, account.username)
            putExtra(GameActivity.EXTRA_UUID, playerUuid)
            putExtra(GameActivity.EXTRA_ACCESS_TOKEN, accessToken)
            putExtra(GameActivity.EXTRA_USER_TYPE, userType)
            if (authlibJar != null && authlibUrl != null) {
                putExtra(GameActivity.EXTRA_AUTHLIB_INJECTOR_PATH, authlibJar.absolutePath)
                putExtra(GameActivity.EXTRA_AUTHLIB_URL, authlibUrl)
            }
            putExtra(GameActivity.EXTRA_GAME_DIR, gameDir.absolutePath)
            putExtra(GameActivity.EXTRA_ASSETS_DIR, assetsDir.absolutePath)
            putExtra(GameActivity.EXTRA_ASSET_INDEX, assetIndexId)
            putExtra(GameActivity.EXTRA_MAIN_CLASS, mainClass)
            putExtra(GameActivity.EXTRA_CLASSPATH, classpath)
            putExtra(GameActivity.EXTRA_RUNTIME_NAME, runtimeName)
            putExtra(GameActivity.EXTRA_RUNTIME_HOME, runtimeHome.absolutePath)
            putExtra(GameActivity.EXTRA_LWJGL_NATIVES_DIR, lwjglNativesDir.absolutePath)
            putExtra(GameActivity.EXTRA_USE_WRAPPER, useWrapper)
            if (forgeLaunch != null) {
                putStringArrayListExtra(GameActivity.EXTRA_EXTRA_JVM_ARGS, ArrayList(forgeLaunch.jvmArgs))
                putStringArrayListExtra(GameActivity.EXTRA_EXTRA_GAME_ARGS, ArrayList(forgeLaunch.gameArgs))
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (t: Throwable) {
            LauncherLog.e("Peluncuran", "Gagal membuka GameActivity untuk ${instance.name}", t)
            Toast.show(
                context,
                "Tidak bisa membuka layar permainan: ${t.localizedMessage ?: t.javaClass.simpleName}",
                Toast.LENGTH_LONG
            )
        }
    }
}

// ----------------------------------------------------------------------------
// Section: core/fabric/FabricService.kt
// ----------------------------------------------------------------------------

object FabricService {
    private const val META_URL = "https://meta.fabricmc.net/v2/versions/loader"
    private const val MIRROR_META_URL = "https://bmclapi2.bangbang93.com/fabric-meta/v2/versions/loader"
    private const val GAME_URL = "https://meta.fabricmc.net/v2/versions/game"
    private const val MIRROR_GAME_URL = "https://bmclapi2.bangbang93.com/fabric-meta/v2/versions/game"

    private val gson = Gson()
    val client = OkHttpClient.Builder()
        .dispatcher(okhttp3.Dispatcher().apply {
            maxRequests = 64
            maxRequestsPerHost = 32
        })
        .connectionPool(okhttp3.ConnectionPool(32, 5, TimeUnit.MINUTES))
        .dns(NuxDns)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .retryOnConnectionFailure(true)
        .build()

    /**
     * Mengambil daftar versi Fabric Loader untuk versi game Minecraft tertentu
     */
    suspend fun getFabricLoaders(
        gameVersion: String,
        forceRefresh: Boolean = false
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        val urls = listOf(
            "$META_URL/$gameVersion",
            "$MIRROR_META_URL/$gameVersion"
        )

        var lastException: Exception? = null
        for (url in urls) {
            try {
                val requestBuilder = Request.Builder().url(url)
                if (forceRefresh) {
                    requestBuilder.cacheControl(CacheControl.FORCE_NETWORK)
                }
                client.newCall(requestBuilder.build()).execute().use { response ->
                    if (!response.isSuccessful) {
                        lastException = IOException("HTTP Error ${response.code} from $url")
                        return@use
                    }

                    val json = response.body?.string() ?: return@use
                    val type = object : TypeToken<List<FabricLoaderItem>>() {}.type
                    val items: List<FabricLoaderItem> = gson.fromJson(json, type) ?: emptyList()
                    val versions = items.mapNotNull { it.loader?.version?.takeIf { v -> v.isNotBlank() } }
                    if (versions.isNotEmpty()) {
                        return@withContext Result.success(versions)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastException = e
            }
        }
        Result.failure(lastException ?: IOException("Tidak dapat memuat versi Fabric untuk $gameVersion"))
    }

    /**
     * Daftar versi game Minecraft yang didukung Fabric (rilis dan snapshot). Dipakai layar buat instance
     * untuk hanya menampilkan versi yang memang punya Fabric. Gagal = layar memakai perkiraan sendiri.
     */
    suspend fun getSupportedGameVersions(forceRefresh: Boolean = false): Result<Set<String>> = withContext(Dispatchers.IO) {
        var lastException: Exception? = null
        for (url in listOf(GAME_URL, MIRROR_GAME_URL)) {
            try {
                val requestBuilder = Request.Builder().url(url)
                if (forceRefresh) {
                    requestBuilder.cacheControl(CacheControl.FORCE_NETWORK)
                }
                client.newCall(requestBuilder.build()).execute().use { response ->
                    if (!response.isSuccessful) {
                        lastException = IOException("HTTP Error ${response.code} from $url")
                        return@use
                    }

                    val json = response.body?.string() ?: return@use
                    val versions = com.google.gson.JsonParser.parseString(json).asJsonArray
                        .mapNotNull { element ->
                            element.asJsonObject.get("version")?.asString?.trim()?.takeIf { it.isNotEmpty() }
                        }
                        .toSet()
                    if (versions.isNotEmpty()) {
                        return@withContext Result.success(versions)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastException = e
            }
        }
        Result.failure<Set<String>>(lastException ?: IOException("Tidak dapat memuat daftar versi Fabric"))
    }

    /**
     * Mengambil profil lengkap Fabric Loader (mainClass, library list, dll.)
     */
    suspend fun getFabricProfile(gameVersion: String, loaderVersion: String): Result<FabricProfile> = withContext(Dispatchers.IO) {
        val safeVersion = gameVersion.replace("∞", "infinite")
        val urls = listOf(
            "$META_URL/$safeVersion/$loaderVersion/profile/json",
            "$MIRROR_META_URL/$safeVersion/$loaderVersion/profile/json"
        )

        var lastException: Exception? = null
        for (url in urls) {
            try {
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        lastException = IOException("HTTP Error ${response.code} from $url")
                        return@use
                    }

                    val json = response.body?.string() ?: return@use
                    val profile: FabricProfile? = gson.fromJson(json, FabricProfile::class.java)
                    if (profile != null) {
                        return@withContext Result.success(profile)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastException = e
            }
        }
        Result.failure(lastException ?: IOException("Gagal memuat profil Fabric untuk $gameVersion ($loaderVersion)"))
    }

    /**
     * Mengonversi koordinat Maven 'group:artifact:version[:classifier]' menjadi relative file path
     */
    fun artifactToPath(name: String): String? {
        val parts = name.split(":")
        if (parts.size < 3) return null
        val group = parts[0].replace('.', '/')
        val artifact = parts[1]
        val version = parts[2]
        val classifier = if (parts.size > 3) "-${parts[3]}" else ""
        return "$group/$artifact/$version/$artifact-$version$classifier.jar"
    }

    /**
     * Mengembalikan kandidat URL untuk mengunduh library Fabric (resmi & mirror)
     */
    fun getLibraryCandidateUrls(library: FabricLibrary): List<String> {
        val path = artifactToPath(library.name) ?: return emptyList()
        val urls = mutableListOf<String>()

        val rawBase = library.url?.takeIf { it.isNotBlank() } ?: "https://maven.fabricmc.net/"
        val base = if (rawBase.endsWith("/")) rawBase else "$rawBase/"
        urls.add("$base$path")

        // Jika berbasis maven fabricmc, tambahkan mirror BMCLAPI
        if (base.contains("maven.fabricmc.net")) {
            urls.add("https://bmclapi2.bangbang93.com/maven/$path")
        }

        // Tambahkan Maven Central sebagai fallback
        urls.add("https://repo1.maven.org/maven2/$path")

        return urls
    }
}

// ----------------------------------------------------------------------------
// Section: core/download/MinecraftDownloader.kt
// ----------------------------------------------------------------------------

/** Isi unduhan tidak cocok dengan sidik jari resmi: jangan dicoba ulang dari alamat yang sama. */
private class HashMismatchException(message: String) : IOException(message)

class MinecraftDownloader(
    private val context: Context,
    clientOverride: OkHttpClient? = null,
    private val gson: Gson = Gson()
) {
    private companion object {
        /** Batas baris galat per jenis unduhan di buku harian, supaya jaringan putus tidak membanjiri log. */
        const val MAX_LOGGED_FAILURES = 30
    }

    private val client: OkHttpClient = clientOverride ?: OkHttpClient.Builder()
        .dispatcher(okhttp3.Dispatcher().apply {
            maxRequests = 128
            maxRequestsPerHost = 64
        })
        .connectionPool(okhttp3.ConnectionPool(64, 5, TimeUnit.MINUTES))
        .dns(com.aistudio.novacraft.launcher.core.utils.NuxDns)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .retryOnConnectionFailure(true)
        .build()

    /** Menghapus berkas sementara unduhan (`.part_`) yang sudah lama tidak disentuh, di semua folder [roots]. */
    private fun cleanStaleParts(vararg roots: File) {
        val cutoff = System.currentTimeMillis() - 10L * 60L * 1000L
        for (root in roots) {
            try {
                root.walkTopDown()
                    .filter { it.isFile && it.name.contains(".part_") && it.lastModified() < cutoff }
                    .forEach { it.delete() }
            } catch (_: Throwable) {}
        }
    }

    /** Mengunduh instance; awal, hasil, dan galatnya dicatat di buku harian launcher ([LauncherLog]). */
    suspend fun downloadInstance(
        instance: Instance,
        onProgress: (Float, String) -> Unit
    ): Result<Unit> {
        LauncherLog.event("Unduhan", "Mulai mengunduh ${instance.name} (Minecraft ${instance.mcVersion}, loader=${instance.loader})")
        val result = downloadInstanceInternal(instance, onProgress)
        val err = result.exceptionOrNull()
        if (err == null) {
            LauncherLog.event("Unduhan", "Unduhan ${instance.name} selesai")
        } else if (err is java.util.concurrent.CancellationException) {
            LauncherLog.event("Unduhan", "Unduhan ${instance.name} dibatalkan")
        } else {
            LauncherLog.e("Unduhan", "Unduhan ${instance.name} gagal: ${err.message}", err)
        }
        return result
    }

    private suspend fun downloadInstanceInternal(
        instance: Instance,
        onProgress: (Float, String) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            onProgress(0.05f, "Mengambil metadata versi ${instance.mcVersion}...")
            val manifestRes = MojangManifestService.getVersionManifest()
            val manifest = manifestRes.getOrNull() ?: return@withContext Result.failure(
                manifestRes.exceptionOrNull() ?: IllegalStateException("Gagal mengambil manifest versi Mojang")
            )

            val versionItem = manifest.versions.find { it.id == instance.mcVersion }
                ?: return@withContext Result.failure(IllegalStateException("Versi ${instance.mcVersion} tidak ditemukan di manifest Mojang"))

            onProgress(0.10f, "Membaca konfigurasi versi...")
            val detailRes = MojangManifestService.getVersionDetail(versionItem.url)
            val detail = detailRes.getOrNull() ?: return@withContext Result.failure(
                detailRes.exceptionOrNull() ?: IllegalStateException("Gagal membaca detail versi ${instance.mcVersion}")
            )

            val versionDir = File(InstanceManager.getVersionsDir(context), instance.mcVersion)
            if (!versionDir.exists()) versionDir.mkdirs()

            // Sisa berkas sementara dari unduhan yang terputus (proses dimatikan sistem) tidak pernah dihapus
            // sendiri; di HP berpenyimpanan kecil lama-lama memakan ruang. Dibersihkan sebelum mengunduh lagi.
            cleanStaleParts(
                versionDir,
                InstanceManager.getLibrariesDir(context),
                File(InstanceManager.getAssetsDir(context), "objects")
            )

            // Unduhan baru butuh sekitar setengah GB. Gagal di tengah jalan karena penyimpanan penuh hanya
            // memunculkan galat samar, jadi dicek di awal dengan pesan yang jelas.
            if (!File(versionDir, "${instance.mcVersion}.jar").exists()) {
                val freeBytes = try { versionDir.usableSpace } catch (_: Throwable) { Long.MAX_VALUE }
                val minFreeBytes = 400L * 1024L * 1024L
                if (freeBytes < minFreeBytes) {
                    val freeMb = freeBytes / (1024L * 1024L)
                    return@withContext Result.failure(
                        IOException("Ruang penyimpanan tinggal $freeMb MB. Kosongkan minimal 500 MB lalu coba unduh lagi.")
                    )
                }
            }

            // 1. Save version.json
            val versionJsonFile = File(versionDir, "${instance.mcVersion}.json")
            if (!versionJsonFile.exists()) {
                downloadFile(versionItem.url, versionJsonFile, expectedSha1 = versionItem.sha1)
            }

            // 2. Download Client JAR
            val clientJar = File(versionDir, "${instance.mcVersion}.jar")
            val clientUrl = detail.downloads?.client?.url
            val expectedClientSize = detail.downloads?.client?.size ?: 0L
            val needsClientDownload = !clientJar.exists() || clientJar.length() == 0L || (expectedClientSize > 0L && clientJar.length() != expectedClientSize)
            if (clientUrl != null && needsClientDownload) {
                downloadFile(
                    url = clientUrl,
                    targetFile = clientJar,
                    expectedSize = expectedClientSize,
                    expectedSha1 = detail.downloads?.client?.sha1,
                    onProgress = { bytesRead, totalBytes ->
                        val total = if (totalBytes > 0) totalBytes else expectedClientSize
                        val mbRead = bytesRead / (1024 * 1024f)
                        val mbTotal = total / (1024 * 1024f)
                        val frac = if (total > 0) (bytesRead.toFloat() / total).coerceIn(0f, 1f) else 0f
                        val p = 0.20f + frac * 0.15f
                        val percent = (frac * 100).toInt()
                        onProgress(p, String.format(java.util.Locale.US, "Mengunduh client Minecraft (%.1f / %.1f MB - %d%%)...", mbRead, mbTotal, percent))
                    }
                )
            }

            // 3. Download Libraries (paralel, sampai 20 thread; lebih sedikit di HP RAM kecil)
            val libraries = detail.libraries ?: emptyList()
            val allowedLibs = libraries.filter { isLibraryAllowed(it) }
            val libDir = InstanceManager.getLibrariesDir(context)
            if (!libDir.exists()) libDir.mkdirs()

            val libThreads = parallelism(20)

            val totalLibs = allowedLibs.size
            if (totalLibs > 0) {
                val downloadedLibs = AtomicInteger(0)
                val failedLibs = AtomicInteger(0)
                val semaphore = Semaphore(libThreads)

                val libJobs = allowedLibs.map { lib ->
                    async {
                        semaphore.withPermit {
                            val artifact = lib.downloads?.artifact
                            if (artifact?.url != null && artifact.path != null) {
                                // Path dari manifest harus tetap di dalam folder libraries
                                val targetFile = SafePaths.resolveInside(libDir, artifact.path)
                                val expectedSize = artifact.size ?: 0L
                                val needsDownload = targetFile != null &&
                                    (!targetFile.exists() || targetFile.length() == 0L || (expectedSize > 0L && targetFile.length() != expectedSize))
                                if (targetFile != null && needsDownload) {
                                    targetFile.parentFile?.mkdirs()
                                    try {
                                        downloadFile(artifact.url, targetFile, expectedSize, expectedSha1 = artifact.sha1)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        if (failedLibs.incrementAndGet() <= MAX_LOGGED_FAILURES) {
                                            LauncherLog.w("Unduhan", "Library gagal: ${artifact.path}: ${e.message}")
                                        }
                                    }
                                }
                            }
                            val count = downloadedLibs.incrementAndGet()
                            val p = 0.20f + (count.toFloat() / totalLibs) * 0.40f
                            onProgress(p, "Mengunduh libraries ($count/$totalLibs)...")
                        }
                    }
                }
                libJobs.awaitAll()
                if (failedLibs.get() > 0) {
                    LauncherLog.w("Unduhan", "${failedLibs.get()} dari $totalLibs library gagal diunduh (game bisa gagal dimulai)")
                }
            }

            // 4. Download Asset Index
            val assetIndex = detail.assetIndex
            if (assetIndex != null) {
                onProgress(0.65f, "Memverifikasi asset index ${assetIndex.id}...")
                val assetsDir = InstanceManager.getAssetsDir(context)
                val indexesDir = File(assetsDir, "indexes")
                if (!indexesDir.exists()) indexesDir.mkdirs()
                val indexFile = File(indexesDir, SafePaths.fileName("${assetIndex.id}.json"))
                if (!indexFile.exists() || indexFile.length() == 0L) {
                    downloadFile(assetIndex.url, indexFile, expectedSha1 = assetIndex.sha1)
                }

                // Download objects (paralel, sampai 24 thread; lebih sedikit di HP RAM kecil)
                if (indexFile.exists()) {
                    try {
                        val indexContent = gson.fromJson(indexFile.readText(), AssetIndexContent::class.java)
                        val objects = indexContent.objects.entries.toList()
                        val totalObjects = objects.size
                        val objectsDir = File(assetsDir, "objects")

                        val downloadedAssets = AtomicInteger(0)
                        val failedAssets = AtomicInteger(0)
                        val assetThreads = parallelism(24)
                        val semaphore = Semaphore(assetThreads)

                        val assetJobs = objects.map { entry ->
                            async {
                                semaphore.withPermit {
                                    val hash = entry.value.hash
                                    // Nama aset adalah SHA-1 isinya (40 karakter hex); selain itu dilewati agar tidak jadi path aneh
                                    if (hash.length == 40 && hash.all { it in '0'..'9' || it in 'a'..'f' }) {
                                        val prefix = hash.substring(0, 2)
                                        val targetObj = File(objectsDir, "$prefix/$hash")
                                        val expectedSize = entry.value.size
                                        if (!targetObj.exists() || targetObj.length() == 0L || (expectedSize > 0L && targetObj.length() != expectedSize)) {
                                            targetObj.parentFile?.mkdirs()
                                            val objUrl = "https://resources.download.minecraft.net/$prefix/$hash"
                                            try {
                                                downloadFile(objUrl, targetObj, expectedSize, expectedSha1 = hash)
                                            } catch (e: Exception) {
                                                if (failedAssets.incrementAndGet() <= MAX_LOGGED_FAILURES) {
                                                    LauncherLog.w("Unduhan", "Aset gagal: $hash: ${e.message}")
                                                }
                                            }
                                        }
                                    }
                                    val count = downloadedAssets.incrementAndGet()
                                    val p = 0.65f + (count.toFloat() / totalObjects) * 0.27f
                                    onProgress(p, "Mengunduh aset game ($count/$totalObjects)...")
                                }
                            }
                        }
                        assetJobs.awaitAll()
                        if (failedAssets.get() > 0) {
                            LauncherLog.w("Unduhan", "${failedAssets.get()} dari $totalObjects aset gagal diunduh (suara atau bahasa bisa hilang di game)")
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        LauncherLog.w("Unduhan", "Daftar aset tidak bisa diproses: ${e.message}", e)
                    }
                }
            }

            // 5. Download Fabric Loader & Libraries if instance uses Fabric
            if (instance.loader.equals("fabric", ignoreCase = true)) {
                onProgress(0.92f, "Menyiapkan Fabric Loader...")
                var loaderVer = instance.loaderVersion
                if (loaderVer.isBlank()) {
                    val loaderListRes = com.aistudio.novacraft.launcher.core.launch.FabricService.getFabricLoaders(instance.mcVersion)
                    val list = loaderListRes.getOrNull()
                    if (!list.isNullOrEmpty()) {
                        loaderVer = list.first()
                        instance.loaderVersion = loaderVer
                    } else {
                        throw IllegalStateException("Tidak dapat menemukan Fabric loader untuk versi ${instance.mcVersion}")
                    }
                }

                onProgress(0.94f, "Mengambil profil Fabric ($loaderVer)...")
                val profileRes = com.aistudio.novacraft.launcher.core.launch.FabricService.getFabricProfile(instance.mcVersion, loaderVer)
                val profile = profileRes.getOrNull()
                    ?: throw IllegalStateException("Gagal memuat profil Fabric: ${profileRes.exceptionOrNull()?.message}")

                // Simpan file profil Fabric
                val fabricJsonFile = File(versionDir, SafePaths.fileName("fabric-$loaderVer.json"))
                fabricJsonFile.writeText(gson.toJson(profile))

                // Unduh seluruh library Fabric (Parallel with 32 concurrent workers)
                val fabricLibs = profile.libraries.filter { !it.name.contains("org.lwjgl") }
                val totalFabricLibs = fabricLibs.size
                if (totalFabricLibs > 0) {
                    val downloadedFabric = AtomicInteger(0)
                    val sem = Semaphore(parallelism(32))
                    val fabricJobs = fabricLibs.map { lib ->
                        async {
                            sem.withPermit {
                                val relPath = com.aistudio.novacraft.launcher.core.launch.FabricService.artifactToPath(lib.name)
                                // Koordinat library dari server tidak boleh membawa path keluar dari folder libraries
                                val targetFile = if (relPath != null) SafePaths.resolveInside(libDir, relPath) else null
                                if (targetFile != null) {
                                    val expectedSize = lib.size ?: 0L
                                    val needsLibDownload = !targetFile.exists() || targetFile.length() == 0L || (expectedSize > 0L && targetFile.length() != expectedSize)
                                    if (needsLibDownload) {
                                        targetFile.parentFile?.mkdirs()
                                        val candidateUrls = com.aistudio.novacraft.launcher.core.launch.FabricService.getLibraryCandidateUrls(lib)
                                        var success = false
                                        for (u in candidateUrls) {
                                            try {
                                                downloadFile(u, targetFile, expectedSize, expectedSha1 = lib.sha1)
                                                if (targetFile.exists() && targetFile.length() > 0L) {
                                                    success = true
                                                    break
                                                }
                                            } catch (_: Exception) {}
                                        }
                                        if (!success) {
                                            LauncherLog.w("MinecraftDownloader", "Gagal mengunduh library Fabric: ${lib.name}")
                                        }
                                    }
                                }
                                val count = downloadedFabric.incrementAndGet()
                                val p = 0.94f + (count.toFloat() / totalFabricLibs) * 0.05f
                                onProgress(p, "Mengunduh library Fabric ($count/$totalFabricLibs)...")
                            }
                        }
                    }
                    fabricJobs.awaitAll()
                }
            }

            // 6. Pasang Forge (Minecraft 1.13+) atau NeoForge (1.20.2+). Installer resmi dijalankan sekali
            // di proses terpisah; sesudahnya tidak ada biaya apa pun saat launcher berjalan.
            val forgeFlavor = com.aistudio.novacraft.launcher.core.launch.ForgeFlavor.of(instance.loader)
            if (forgeFlavor != null) {
                var forgeVer = instance.loaderVersion
                if (forgeVer.isBlank()) {
                    val builds = com.aistudio.novacraft.launcher.core.launch.ForgeService.getBuilds(instance.mcVersion, forgeFlavor).getOrNull()
                    val pick = builds?.let { com.aistudio.novacraft.launcher.core.launch.ForgeService.pickDefault(it) }
                        ?: throw IllegalStateException("Tidak dapat menemukan ${forgeFlavor.displayName} untuk versi ${instance.mcVersion}")
                    forgeVer = pick.version
                    instance.loaderVersion = forgeVer
                }
                com.aistudio.novacraft.launcher.core.launch.ForgeInstaller.ensureInstalled(
                    context = context,
                    downloader = this@MinecraftDownloader,
                    mcVersion = instance.mcVersion,
                    forgeVersion = forgeVer,
                    onProgress = onProgress,
                    flavor = forgeFlavor
                ).getOrThrow()
            }

            onProgress(1.0f, "Instalasi selesai!")
            instance.isDownloaded = true
            InstanceManager.updateInstance(context, instance)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isLibraryAllowed(library: Library): Boolean {
        val rules = library.rules ?: return true
        var allowed = false
        for (rule in rules) {
            if (rule.action == "allow") {
                if (rule.os == null || rule.os.name == "linux") {
                    allowed = true
                }
            } else if (rule.action == "disallow") {
                if (rule.os != null && (rule.os.name == "osx" || rule.os.name == "windows")) {
                    // Do nothing
                } else {
                    allowed = false
                }
            }
        }
        return allowed
    }

    private fun getMirrorUrls(url: String): List<String> {
        val mirrorUrl = when {
            url.startsWith("https://piston-data.mojang.com") ->
                url.replace("https://piston-data.mojang.com", "https://bmclapi2.bangbang93.com")
            url.startsWith("https://piston-meta.mojang.com") ->
                url.replace("https://piston-meta.mojang.com", "https://bmclapi2.bangbang93.com")
            url.startsWith("https://libraries.minecraft.net") ->
                url.replace("https://libraries.minecraft.net", "https://bmclapi2.bangbang93.com/libraries")
            url.startsWith("https://resources.download.minecraft.net") ->
                url.replace("https://resources.download.minecraft.net", "https://bmclapi2.bangbang93.com/assets")
            url.startsWith("https://meta.fabricmc.net") ->
                url.replace("https://meta.fabricmc.net", "https://bmclapi2.bangbang93.com/fabric-meta")
            url.startsWith("https://maven.fabricmc.net") ->
                url.replace("https://maven.fabricmc.net", "https://bmclapi2.bangbang93.com/maven")
            url.startsWith("https://maven.minecraftforge.net") ->
                url.replace("https://maven.minecraftforge.net", "https://bmclapi2.bangbang93.com/maven")
            url.startsWith("https://maven.neoforged.net/releases") ->
                url.replace("https://maven.neoforged.net/releases", "https://bmclapi2.bangbang93.com/maven")
            url.startsWith("https://files.minecraftforge.net/maven") ->
                url.replace("https://files.minecraftforge.net/maven", "https://bmclapi2.bangbang93.com/maven")
            else -> null
        }
        val preferBmcl = com.aistudio.novacraft.launcher.core.utils.SettingsManager.settings.value.gameDownloadSource == "bmclapi"
        return if (mirrorUrl != null && mirrorUrl != url) {
            if (preferBmcl) listOf(mirrorUrl, url) else listOf(url, mirrorUrl)
        } else {
            listOf(url)
        }
    }

    /**
     * Jumlah koneksi unduh paralel menurut RAM HP. Tiap koneksi memegang buffer dan menghitung SHA-1,
     * jadi di HP RAM kecil terlalu banyak koneksi membuat HP lag dan malah memperlambat unduhan.
     */
    private fun parallelism(base: Int): Int {
        val ramMb = com.aistudio.novacraft.launcher.core.utils.SettingsManager.getTotalDeviceMemoryMb(context)
        return when {
            ramMb < 3000 -> minOf(base, 6)
            ramMb < 4500 -> minOf(base, 12)
            else -> base
        }
    }

    /**
     * Mengunduh file secara cepat menggunakan buffer 64KB dan retry otomatis jika terjadi gangguan jaringan.
     * Menggunakan mirror BMCLAPI berkecepatan tinggi dengan fallback otomatis ke server resmi Mojang.
     *
     * Hanya alamat HTTPS yang dipakai. Unduhan selalu ditulis ke berkas sementara dulu dan baru dipindah
     * setelah selesai, jadi unduhan yang terputus tidak tertinggal sebagai berkas rusak. Bila [expectedSha1]
     * diberikan, isi unduhan dicocokkan dengan sidik jari resmi (mirror yang isinya berbeda ditolak,
     * lalu dicoba alamat berikutnya).
     */
    internal fun downloadFile(
        url: String,
        targetFile: File,
        expectedSize: Long = 0L,
        onProgress: ((bytesRead: Long, totalBytes: Long) -> Unit)? = null,
        retries: Int = 3,
        expectedSha1: String? = null
    ) {
        val candidateUrls = getMirrorUrls(url).filter { it.startsWith("https://", ignoreCase = true) }
        if (candidateUrls.isEmpty()) {
            throw IOException("Alamat unduhan ditolak karena tidak memakai HTTPS: $url")
        }
        val isMetadataJson = targetFile.name.endsWith(".json", ignoreCase = true)
        val shouldVerifySha1 = isMetadataJson || com.aistudio.novacraft.launcher.core.utils.SettingsManager.settings.value.verifyGameFiles
        val wantedSha1 = if (shouldVerifySha1) {
            expectedSha1?.trim()?.lowercase()
                ?.takeIf { hash -> hash.length == 40 && hash.all { it in '0'..'9' || it in 'a'..'f' } }
        } else null
        var lastError: Exception? = null
        val tempFile = File(targetFile.parentFile, "${targetFile.name}.part_${System.nanoTime()}")

        for (candidateUrl in candidateUrls) {
            for (attempt in 1..retries) {
                try {
                    val request = Request.Builder()
                        .url(candidateUrl)
                        .header("User-Agent", "NovaCraft/2.3 (Android)")
                        .build()
                    val response = client.newCall(request).execute()
                    if (!response.isSuccessful) {
                        response.close()
                        throw IOException("Failed to download $candidateUrl: ${response.code}")
                    }

                    val body = response.body ?: throw IOException("Empty body for $candidateUrl")
                    val totalLength = if (body.contentLength() > 0) body.contentLength() else expectedSize
                    targetFile.parentFile?.mkdirs()

                    val digest = if (wantedSha1 != null) MessageDigest.getInstance("SHA-1") else null
                    var bytesCopied = 0L
                    var lastReportTime = 0L
                    FileOutputStream(tempFile).use { output ->
                        body.byteStream().use { input ->
                            val buffer = ByteArray(64 * 1024)
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                output.write(buffer, 0, read)
                                digest?.update(buffer, 0, read)
                                bytesCopied += read
                                if (onProgress != null) {
                                    val now = System.currentTimeMillis()
                                    if (now - lastReportTime >= 100L) {
                                        lastReportTime = now
                                        onProgress.invoke(bytesCopied, totalLength)
                                    }
                                }
                            }
                            output.flush()
                        }
                    }
                    onProgress?.invoke(bytesCopied, totalLength)

                    if (digest != null) {
                        val actual = digest.digest().joinToString("") { "%02x".format(it) }
                        if (actual != wantedSha1) {
                            throw HashMismatchException("Isi unduhan tidak cocok dengan sidik jari resmi: $candidateUrl")
                        }
                    }

                    if (tempFile.exists() && tempFile.length() > 0L) {
                        if (targetFile.exists()) targetFile.delete()
                        if (tempFile.renameTo(targetFile)) {
                            return
                        } else {
                            tempFile.copyTo(targetFile, overwrite = true)
                            tempFile.delete()
                            return
                        }
                    }
                } catch (e: Exception) {
                    lastError = e
                    try { tempFile.delete() } catch (_: Exception) {}
                    // Isi yang salah tidak akan benar di percobaan ulang: langsung ke alamat berikutnya
                    if (e is HashMismatchException) break
                    if (attempt < retries) {
                        Thread.sleep(100L * attempt)
                    }
                }
            }
        }
        throw lastError ?: IOException("Failed to download $url")
    }
}

// ----------------------------------------------------------------------------
// Section: core/forge/NovaForgeVersionsAndModels.kt
// ----------------------------------------------------------------------------

/**
 * NovaCraft unified Forge/NeoForge data models & version resolution helpers:
 * - ForgeFlavor, ForgeBuild, ForgeInstallProfile, ForgeDataEntry, ForgeProcessor,
 *   ForgeLibrary, ForgeLibDownloads, ForgeArtifact, ForgeVersionJson, ForgeArguments
 * - ForgeMaven
 * - NeoForgeVersions
 */

enum class ForgeFlavor(
    val loaderId: String,
    val displayName: String,
    val minMcVersion: String
) {
    FORGE("forge", "Forge", "1.13"),
    NEOFORGE("neoforge", "NeoForge", "1.20.2");

    fun markerName(version: String): String = "$loaderId-$version.json"

    fun libraryDir(mcVersion: String, version: String): String = when (this) {
        FORGE -> "net/minecraftforge/forge/$mcVersion-$version"
        NEOFORGE -> "net/neoforged/neoforge/$version"
    }

    companion object {
        fun of(loader: String?): ForgeFlavor? {
            val id = loader?.trim() ?: return null
            return values().firstOrNull { it.loaderId.equals(id, ignoreCase = true) }
        }

        fun label(loader: String): String = of(loader)?.displayName
            ?: if (com.aistudio.novacraft.launcher.core.mods.HorizonEngine.isEngine(loader)) {
                com.aistudio.novacraft.launcher.core.mods.HorizonEngine.LABEL
            } else {
                loader.replaceFirstChar { it.uppercase() }
            }
    }
}

data class ForgeBuild(
    val mcVersion: String,
    val version: String,
    val recommended: Boolean = false,
    val latest: Boolean = false
) {
    val mavenVersion: String get() = "$mcVersion-$version"
}

data class ForgeInstallProfile(
    val spec: Int? = null,
    val version: String? = null,
    val minecraft: String? = null,
    val json: String? = null,
    val data: Map<String, ForgeDataEntry>? = null,
    val processors: List<ForgeProcessor>? = null,
    val libraries: List<ForgeLibrary>? = null
)

data class ForgeDataEntry(
    val client: String? = null,
    val server: String? = null
)

data class ForgeProcessor(
    val sides: List<String>? = null,
    val jar: String? = null,
    val classpath: List<String>? = null,
    val args: List<String>? = null,
    val outputs: Map<String, String>? = null
)

data class ForgeLibrary(
    val name: String? = null,
    val downloads: ForgeLibDownloads? = null,
    val url: String? = null
)

data class ForgeLibDownloads(
    val artifact: ForgeArtifact? = null
)

data class ForgeArtifact(
    val path: String? = null,
    val url: String? = null,
    val sha1: String? = null,
    val size: Long? = null
)

data class ForgeVersionJson(
    val id: String? = null,
    val inheritsFrom: String? = null,
    val mainClass: String? = null,
    val minecraftArguments: String? = null,
    val arguments: ForgeArguments? = null,
    val libraries: List<ForgeLibrary>? = null
)

data class ForgeArguments(
    val game: List<Any>? = null,
    val jvm: List<Any>? = null
)

object ForgeMaven {

    fun pathOf(coordinate: String?): String? {
        var c = coordinate?.trim() ?: return null
        var ext = "jar"
        val at = c.indexOf('@')
        if (at >= 0) {
            ext = c.substring(at + 1)
            c = c.substring(0, at)
        }
        val p = c.split(':')
        if (p.size < 3 || p[0].isBlank() || p[1].isBlank() || p[2].isBlank()) return null
        val group = p[0].replace('.', '/')
        val artifact = p[1]
        val version = p[2]
        val classifier = if (p.size > 3 && p[3].isNotEmpty()) "-${p[3]}" else ""
        return "$group/$artifact/$version/$artifact-$version$classifier.$ext"
    }

    fun keyOf(coordinate: String?): String? {
        val p = coordinate?.substringBefore('@')?.split(':') ?: return null
        return if (p.size >= 2) "${p[0]}:${p[1]}" else null
    }

    fun pathOfDataEntry(value: String?): String? {
        val v = value?.trim() ?: return null
        if (!v.startsWith("[") || !v.endsWith("]")) return null
        return pathOf(v.substring(1, v.length - 1))
    }

    fun libraryPath(lib: ForgeLibrary): String? {
        return lib.downloads?.artifact?.path?.takeIf { it.isNotBlank() } ?: pathOf(lib.name)
    }

    fun libraryUrl(lib: ForgeLibrary): String? {
        lib.downloads?.artifact?.let { artifact ->
            return artifact.url?.takeIf { it.isNotBlank() }
        }
        val path = pathOf(lib.name) ?: return null
        val base = lib.url?.takeIf { it.isNotBlank() } ?: "https://libraries.minecraft.net/"
        return base.trimEnd('/') + "/" + path
    }
}

object NeoForgeVersions {

    private const val FIRST_YEAR_BASED_MAJOR = 26
    private val VERSION_TAG = Regex("<version>\\s*([^<\\s]+)\\s*</version>")

    fun mcVersionOf(version: String): String? {
        val v = version.trim()
        if (v.isEmpty() || v.contains('+')) return null
        val suffix = v.substringAfter('-', "").lowercase()
        if (suffix.contains("alpha") || suffix.contains("snapshot")) return null
        val numbers = numericCore(v) ?: return null
        if (numbers.size < 3) return null
        val yearBased = numbers[0] >= FIRST_YEAR_BASED_MAJOR || numbers.size >= 4
        return if (yearBased) {
            if (numbers[2] == 0) "${numbers[0]}.${numbers[1]}" else "${numbers[0]}.${numbers[1]}.${numbers[2]}"
        } else {
            if (numbers[1] == 0) "1.${numbers[0]}" else "1.${numbers[0]}.${numbers[1]}"
        }
    }

    private fun numericCore(version: String): List<Int>? {
        val parts = version.trim().substringBefore('-').split('.')
        if (parts.isEmpty()) return null
        val numbers = ArrayList<Int>(parts.size)
        for (p in parts) {
            if (p.isEmpty() || !p.all { it in '0'..'9' }) return null
            numbers.add(p.toIntOrNull() ?: return null)
        }
        return numbers
    }

    fun isPrerelease(version: String): Boolean = version.contains('-')

    fun compare(a: String, b: String): Int {
        val na = numericCore(a) ?: emptyList()
        val nb = numericCore(b) ?: emptyList()
        for (i in 0 until maxOf(na.size, nb.size)) {
            val x = na.getOrElse(i) { 0 }
            val y = nb.getOrElse(i) { 0 }
            if (x != y) return x.compareTo(y)
        }
        val pa = isPrerelease(a)
        val pb = isPrerelease(b)
        if (pa != pb) return if (pa) -1 else 1
        return a.compareTo(b)
    }

    fun parseMetadataVersions(xml: String): List<String> {
        val section = xml.substringAfter("<versions>", "").substringBefore("</versions>")
        return VERSION_TAG.findAll(section).map { it.groupValues[1] }.toList()
    }

    fun parseBmclList(json: String): List<String> {
        val root = JsonParser.parseString(json)
        if (!root.isJsonArray) return emptyList()
        val result = ArrayList<String>()
        for (item in root.asJsonArray) {
            when {
                item.isJsonPrimitive && item.asJsonPrimitive.isString -> result.add(item.asString)
                item.isJsonObject -> {
                    val obj = item.asJsonObject
                    val v = listOf("version", "rawVersion")
                        .mapNotNull { key ->
                            obj.get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString
                        }
                        .map { it.trim() }
                        .firstOrNull { it.isNotEmpty() }
                    if (v != null) result.add(v)
                }
            }
        }
        return result.map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun groupByMc(versions: List<String>): Map<String, List<String>> =
        versions.distinct()
            .mapNotNull { v -> mcVersionOf(v)?.let { it to v } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, list) -> list.sortedWith { a, b -> compare(b, a) } }

    fun toBuilds(mcVersion: String, versions: List<String>): List<ForgeBuild> {
        val sorted = versions.distinct()
            .filter { mcVersionOf(it) == mcVersion }
            .sortedWith { a, b -> compare(b, a) }
        val latest = sorted.firstOrNull()
        val recommended = sorted.firstOrNull { !isPrerelease(it) }
        return sorted.map { ForgeBuild(mcVersion, it, recommended = it == recommended, latest = it == latest) }
    }
}

// ----------------------------------------------------------------------------
// Section: core/forge/ForgeService.kt
// ----------------------------------------------------------------------------

/**
 * Daftar versi Forge/NeoForge dan alamat installer-nya.
 *
 * Hanya yang modern yang didukung: Forge untuk Minecraft 1.13 ke atas dan NeoForge untuk 1.20.2 ke
 * atas. Amber memakai LWJGL 3, sedangkan Forge untuk 1.12.2 ke bawah butuh LWJGL 2. Layanan ini
 * tidak berjalan sendiri: tidak ada permintaan jaringan sampai pengguna memilih Forge/NeoForge di
 * dialog buat instance.
 */
object ForgeService {

    /** Versi Minecraft terendah yang didukung Forge. */
    const val MIN_MC_VERSION = "1.13"

    private const val PROMOS_URL = "https://files.minecraftforge.net/net/minecraftforge/forge/promotions_slim.json"
    private const val BMCL_LIST_URL = "https://bmclapi2.bangbang93.com/forge/minecraft"
    private const val MAVEN_URL = "https://maven.minecraftforge.net/net/minecraftforge/forge"

    private const val NEO_METADATA_URL = "https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml"
    private const val NEO_BMCL_LIST_URL = "https://bmclapi2.bangbang93.com/neoforge/list"
    private const val NEO_MAVEN_URL = "https://maven.neoforged.net/releases/net/neoforged/neoforge"

    private val gson = Gson()
    private val releaseRegex = Regex("""^\d+\.\d+(\.\d+)?$""")

    /** Hasil daftar per jenis dan versi Minecraft; dialog buat instance sering memuat ulang daftar yang sama. */
    private val cache = ConcurrentHashMap<String, List<ForgeBuild>>()

    /** Seluruh versi NeoForge per versi Minecraft; satu berkas metadata melayani semua versi Minecraft. */
    @Volatile
    private var neoForgeByMc: Map<String, List<String>>? = null

    private data class BmclForge(
        val version: String? = null,
        val mcversion: String? = null,
        val branch: String? = null
    )

    /** True bila [mcVersion] adalah rilis resmi yang didukung [flavor] (snapshot tidak didukung). */
    fun isMcSupported(mcVersion: String, flavor: ForgeFlavor = ForgeFlavor.FORGE): Boolean {
        val v = mcVersion.trim()
        if (!releaseRegex.matches(v)) return false
        return !NuxVersionUtils.isLowerVer(v, flavor.minMcVersion)
    }

    /** Alamat installer resmi; mirror BMCLAPI ditambahkan otomatis oleh pengunduh. */
    fun installerUrl(mcVersion: String, version: String, flavor: ForgeFlavor = ForgeFlavor.FORGE): String =
        when (flavor) {
            ForgeFlavor.FORGE -> {
                val v = "$mcVersion-$version"
                "$MAVEN_URL/$v/forge-$v-installer.jar"
            }
            ForgeFlavor.NEOFORGE -> "$NEO_MAVEN_URL/$version/neoforge-$version-installer.jar"
        }

    /** Pilihan bawaan: build "recommended" bila ada, kalau tidak yang terbaru. */
    fun pickDefault(builds: List<ForgeBuild>): ForgeBuild? =
        builds.firstOrNull { it.recommended } ?: builds.firstOrNull { it.latest } ?: builds.firstOrNull()

    /**
     * Daftar build untuk [mcVersion], terbaru lebih dulu. Forge: daftar penuh dari BMCLAPI; bila
     * mirror itu gagal, dipakai build "recommended"/"latest" dari server resmi. NeoForge: dari
     * berkas metadata Maven resmi; bila gagal, dari BMCLAPI.
     */
    suspend fun getBuilds(
        mcVersion: String,
        flavor: ForgeFlavor = ForgeFlavor.FORGE,
        forceRefresh: Boolean = false
    ): Result<List<ForgeBuild>> = withContext(Dispatchers.IO) {
        if (!isMcSupported(mcVersion, flavor)) {
            return@withContext Result.failure(
                IllegalArgumentException("${flavor.displayName} hanya mendukung Minecraft ${flavor.minMcVersion} ke atas")
            )
        }
        val key = "${flavor.loaderId}:$mcVersion"
        if (!forceRefresh) {
            cache[key]?.let { return@withContext Result.success(it) }
        }

        val result = when (flavor) {
            ForgeFlavor.FORGE -> loadForgeBuilds(mcVersion, forceRefresh)
            ForgeFlavor.NEOFORGE -> loadNeoForgeBuilds(mcVersion, forceRefresh)
        }
        result.getOrNull()?.let { cache[key] = it }
        result
    }

    /**
     * Versi Minecraft yang punya build [flavor] dan didukung Amber. Satu permintaan (Forge: daftar
     * promosi resmi; NeoForge: berkas metadata Maven yang sama dengan daftar build). Dipakai layar buat
     * instance supaya hanya menampilkan versi yang memang punya loader itu. Gagal = layar memakai
     * perkiraan sendiri.
     */
    suspend fun getSupportedMcVersions(
        flavor: ForgeFlavor,
        forceRefresh: Boolean = false
    ): Result<Set<String>> = withContext(Dispatchers.IO) {
        try {
            val found: Set<String> = when (flavor) {
                ForgeFlavor.FORGE -> fetchPromos(forceRefresh).keys
                    .map { it.substringBeforeLast('-') } // "1.20.1-latest" menjadi "1.20.1"
                    .toSet()
                ForgeFlavor.NEOFORGE -> {
                    val existing = if (forceRefresh) null else neoForgeByMc
                    val grouped = existing ?: NeoForgeVersions
                        .groupByMc(NeoForgeVersions.parseMetadataVersions(httpGet(NEO_METADATA_URL, forceRefresh)))
                        .also { if (it.isNotEmpty()) neoForgeByMc = it }
                    grouped.keys
                }
            }
            val supported = found.filter { isMcSupported(it, flavor) }.toSet()
            if (supported.isEmpty()) {
                Result.failure<Set<String>>(IOException("Tidak ada versi ${flavor.displayName} yang didukung"))
            } else {
                Result.success(supported)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure<Set<String>>(e)
        }
    }

    private suspend fun loadForgeBuilds(mcVersion: String, forceRefresh: Boolean): Result<List<ForgeBuild>> {
        val (promos, all) = coroutineScope {
            val p = async { runCatching { fetchPromos(forceRefresh) }.getOrNull() }
            val l = async { runCatching { fetchBmclList(mcVersion, forceRefresh) } }
            Pair(p.await(), l.await())
        }

        val recommended = promos?.get("$mcVersion-recommended")
        val latest = promos?.get("$mcVersion-latest")

        val versions = LinkedHashSet<String>()
        all.getOrNull()?.let { versions.addAll(it) }
        // Promosi resmi selalu dimasukkan: dipakai penuh bila mirror gagal
        recommended?.let { versions.add(it) }
        latest?.let { versions.add(it) }

        if (versions.isEmpty()) {
            val cause = all.exceptionOrNull() ?: IOException("Tidak ada versi Forge untuk $mcVersion")
            return Result.failure(cause)
        }

        val builds = versions
            .sortedWith(Comparator { a, b -> NuxVersionUtils.compare(b, a) })
            .map { ForgeBuild(mcVersion, it, recommended = it == recommended, latest = it == latest) }
        return Result.success(builds)
    }

    private fun loadNeoForgeBuilds(mcVersion: String, forceRefresh: Boolean): Result<List<ForgeBuild>> {
        var failure: Throwable? = null

        // Metadata resmi memuat semua versi sekaligus, jadi cukup diambil sekali per sesi kecuali forceRefresh
        val cached = if (forceRefresh) null else neoForgeByMc
        val official = cached ?: try {
            NeoForgeVersions.groupByMc(NeoForgeVersions.parseMetadataVersions(httpGet(NEO_METADATA_URL, forceRefresh)))
                .takeIf { it.isNotEmpty() }
                ?.also { neoForgeByMc = it }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            failure = e
            null
        }
        official?.get(mcVersion)?.let { versions ->
            val builds = NeoForgeVersions.toBuilds(mcVersion, versions)
            if (builds.isNotEmpty()) return Result.success(builds)
        }

        // Cadangan: mirror BMCLAPI, dipakai bila server resmi tak terjangkau
        try {
            val builds = NeoForgeVersions.toBuilds(
                mcVersion,
                NeoForgeVersions.parseBmclList(httpGet("$NEO_BMCL_LIST_URL/$mcVersion", forceRefresh))
            )
            if (builds.isNotEmpty()) return Result.success(builds)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (failure == null) failure = e
        }
        return Result.failure(failure ?: IOException("Tidak ada versi NeoForge untuk $mcVersion"))
    }

    /** Peta "<mc>-recommended" / "<mc>-latest" -> versi Forge, dari server resmi. */
    private fun fetchPromos(forceRefresh: Boolean): Map<String, String> {
        val body = httpGet(PROMOS_URL, forceRefresh)
        val promos = JsonParser.parseString(body).asJsonObject.getAsJsonObject("promos")
            ?: return emptyMap()
        return promos.entrySet().mapNotNull { (k, v) ->
            if (v.isJsonPrimitive) k to v.asString else null
        }.toMap()
    }

    private fun fetchBmclList(mcVersion: String, forceRefresh: Boolean): List<String> {
        val body = httpGet("$BMCL_LIST_URL/$mcVersion", forceRefresh)
        val type = object : TypeToken<List<BmclForge>>() {}.type
        val items: List<BmclForge> = gson.fromJson(body, type) ?: emptyList()
        return items
            .filter { it.branch.isNullOrBlank() } // build bercabang (gaya lama) punya nama artefak berbeda
            .filter { it.mcversion == null || it.mcversion == mcVersion }
            .mapNotNull { it.version?.trim()?.takeIf { v -> v.isNotEmpty() } }
            .distinct()
    }

    private fun httpGet(url: String, forceRefresh: Boolean = false): String {
        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", "NovaCraftLauncher/2.3 (Android)")
        if (forceRefresh) {
            builder.cacheControl(CacheControl.FORCE_NETWORK)
        }
        FabricService.client.newCall(builder.build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code} dari $url")
            return response.body?.string() ?: throw IOException("Respons kosong dari $url")
        }
    }
}

// ----------------------------------------------------------------------------
// Section: core/forge/ForgeInstaller.kt
// ----------------------------------------------------------------------------

/**
 * Memasang Forge atau NeoForge ke folder versions/libraries milik Amber (keduanya memakai format
 * installer yang sama; bedanya diatur oleh [ForgeFlavor]).
 *
 * Alurnya sengaja dibuat hemat: semua unduhan (installer, library, mappings Mojang) dikerjakan di
 * sini, paralel dan dengan cek sidik jari, lalu installer resminya dijalankan offline di
 * proses terpisah (:forge) hanya untuk memverifikasi dan membuat jar Minecraft yang ditambal.
 * Pemasangan hanya terjadi sekali per versi; sesudahnya tidak ada biaya apa pun saat
 * launcher berjalan, dan proses :forge sudah mati.
 *
 * Penanda "terpasang" adalah berkas versions/<mc>/<forge|neoforge>-<versi>.json, ditulis paling akhir.
 */
object ForgeInstaller {

    private const val TAG = "ForgeInstaller"

    /** Rentang progress yang dipakai pemasangan Forge di dalam proses unduh instance. */
    private const val P_START = 0.92f
    private const val P_END = 0.995f

    private const val LIB_THREADS = 16

    /** Jeda sebelum installer dijalankan ulang, supaya proses :forge sebelumnya sudah dimatikan sistem. */
    private const val RESTART_DELAY_MS = 600L

    /** Ruang kosong minimum (MB): library, jar hasil tambalan, dan berkas sementara processor. */
    private const val MIN_FREE_MB = 400L

    private val gson = Gson()

    /** Satu pemasangan pada satu waktu: folder kerja dan proses :forge hanya ada satu. */
    private val installLock = Mutex()

    /** Berkas profil yang dipasang untuk [forgeVersion], atau (bila kosong) yang pertama ditemukan. */
    fun findProfileFile(
        context: Context,
        mcVersion: String,
        forgeVersion: String,
        flavor: ForgeFlavor = ForgeFlavor.FORGE
    ): File? {
        val vDir = File(InstanceManager.getVersionsDir(context), mcVersion)
        val exact = if (forgeVersion.isNotBlank()) File(vDir, SafePaths.fileName(flavor.markerName(forgeVersion))) else null
        if (exact != null && exact.exists() && exact.length() > 0L) return exact
        if (forgeVersion.isNotBlank()) return null
        val prefix = flavor.loaderId + "-"
        return vDir.listFiles { f -> f.name.startsWith(prefix) && f.name.endsWith(".json") && f.length() > 0L }
            ?.minByOrNull { it.name }
    }

    /**
     * Murah dan aman dipanggil dari komposisi UI: hanya mengecek keberadaan berkas, tanpa membaca
     * isinya. Penanda profil ditulis paling akhir, jadi keberadaannya berarti pemasangan selesai.
     */
    fun isInstalled(
        context: Context,
        mcVersion: String,
        forgeVersion: String,
        flavor: ForgeFlavor = ForgeFlavor.FORGE
    ): Boolean {
        val profile = findProfileFile(context, mcVersion, forgeVersion, flavor) ?: return false
        val version = if (forgeVersion.isNotBlank()) forgeVersion
        else profile.name.removePrefix(flavor.loaderId + "-").removeSuffix(".json")
        val forgeDir = File(InstanceManager.getLibrariesDir(context), flavor.libraryDir(mcVersion, version))
        return forgeDir.isDirectory && !forgeDir.list().isNullOrEmpty()
    }

    /**
     * Memastikan Forge/NeoForge [forgeVersion] untuk [mcVersion] terpasang. Versi Minecraft (client
     * jar, library, aset) harus sudah diunduh lebih dulu. Aman dipanggil ulang: bila sudah
     * terpasang, langsung selesai.
     */
    suspend fun ensureInstalled(
        context: Context,
        downloader: MinecraftDownloader,
        mcVersion: String,
        forgeVersion: String,
        onProgress: (Float, String) -> Unit,
        flavor: ForgeFlavor = ForgeFlavor.FORGE
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val name = flavor.displayName
            if (forgeVersion.isBlank()) throw IllegalStateException("Versi $name belum dipilih")
            if (isInstalled(context, mcVersion, forgeVersion, flavor)) return@withContext Result.success(Unit)
            if (!ForgeService.isMcSupported(mcVersion, flavor)) {
                throw IllegalStateException("$name hanya mendukung Minecraft ${flavor.minMcVersion} ke atas")
            }
            installLock.withLock {
                // Pemasangan lain mungkin baru selesai selagi menunggu giliran
                if (!isInstalled(context, mcVersion, forgeVersion, flavor)) {
                    install(context, downloader, mcVersion, forgeVersion, flavor, onProgress)
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LauncherLog.w(TAG, "Pemasangan ${flavor.displayName} gagal", e)
            Result.failure(e)
        }
    }

    private fun progress(fraction: Float): Float = P_START + fraction.coerceIn(0f, 1f) * (P_END - P_START)

    private suspend fun install(
        context: Context,
        downloader: MinecraftDownloader,
        mcVersion: String,
        forgeVersion: String,
        flavor: ForgeFlavor,
        onProgress: (Float, String) -> Unit
    ) {
        val name = flavor.displayName
        val versionsDir = InstanceManager.getVersionsDir(context)
        val libDir = InstanceManager.getLibrariesDir(context)
        val nuxDir = InstanceManager.getNuxDir(context)

        // Lebih baik berhenti di awal dengan pesan jelas daripada gagal di tengah processor karena penuh
        val freeMb = libDir.apply { mkdirs() }.usableSpace / (1024L * 1024L)
        if (freeMb < MIN_FREE_MB) {
            throw IOException("Ruang penyimpanan tidak cukup untuk memasang $name (tersisa $freeMb MB, butuh minimal $MIN_FREE_MB MB)")
        }

        // 1. Installer resmi
        onProgress(progress(0f), "Mengunduh installer $name $forgeVersion...")
        val installerJar = File(
            context.cacheDir,
            "forge/" + SafePaths.fileName("${flavor.loaderId}-$mcVersion-$forgeVersion-installer.jar")
        )
        installerJar.parentFile?.mkdirs()
        if (!isUsableInstaller(installerJar)) {
            val url = ForgeService.installerUrl(mcVersion, forgeVersion, flavor)
            installerJar.delete()
            downloader.downloadFile(
                url = url,
                targetFile = installerJar,
                expectedSha1 = fetchSha1(url),
                onProgress = { read, total ->
                    val frac = if (total > 0) (read.toFloat() / total).coerceIn(0f, 1f) else 0f
                    onProgress(progress(frac * 0.08f), "Mengunduh installer $name $forgeVersion...")
                }
            )
            if (!isUsableInstaller(installerJar)) {
                installerJar.delete()
                throw IOException("Installer $name $forgeVersion rusak atau tidak lengkap")
            }
        }

        val (profile, versionJsonText) = readInstallerMeta(installerJar, name)
        if (profile.processors == null || profile.json == null && versionJsonText.isBlank()) {
            throw IllegalStateException("Format installer $name ini tidak dikenali (Amber butuh $name ${flavor.minMcVersion} ke atas)")
        }
        if (!profile.minecraft.isNullOrBlank() && profile.minecraft != mcVersion) {
            throw IllegalStateException("Installer $name untuk Minecraft ${profile.minecraft}, bukan $mcVersion")
        }
        val forgeVersionJson = gson.fromJson(versionJsonText, ForgeVersionJson::class.java)
            ?: throw IllegalStateException("version.json di installer $name tidak terbaca")
        val clientProcessorCount = (profile.processors ?: emptyList())
            .count { it.sides.isNullOrEmpty() || it.sides.any { s -> s.equals("client", ignoreCase = true) } }

        // 2. Library (installer berjalan offline, jadi semuanya harus sudah ada dan cocok sidik jarinya)
        prefetchLibraries(downloader, libDir, forgeVersionJson, profile, name, onProgress)

        // 3. Mappings Mojang (Forge 1.17+): diunduh di sini agar installer tidak butuh jaringan
        prefetchMojangMappings(downloader, libDir, versionsDir, mcVersion, profile)

        // 4. Installer resmi mensyaratkan launcher_profiles.json di folder tujuan
        ensureLauncherProfiles(nuxDir)

        // 5. Jalankan installer (offline) di proses :forge
        val mojangJson = File(File(versionsDir, mcVersion), "$mcVersion.json")
        val recommendedRuntime = JavaRuntimeManager.getRecommendedRuntime(mcVersion, readJavaMajor(mojangJson))
        // Installer Forge lama paling aman di Java 8; NeoForge (Minecraft 1.20.2 ke atas) butuh Java 17+
        val runtimes = linkedSetOf<String>()
        if (flavor == ForgeFlavor.FORGE) runtimes.add("jre-8")
        runtimes.add(recommendedRuntime)
        val commands = ForgeInstallerCli.commands(
            flavor,
            if (flavor == ForgeFlavor.FORGE) null else ForgeInstallerCli.readHints(installerJar),
            nuxDir.absolutePath
        )

        var lastFailure = "Installer $name gagal"
        var success = false
        runtimeLoop@ for (runtime in runtimes) {
            if (!JavaRuntimeManager.isRuntimeInstalled(context, runtime)) {
                onProgress(progress(0.45f), "Menyiapkan Java ($runtime)...")
                val ext = JavaRuntimeManager.extractRuntime(context, runtime) { f, msg ->
                    onProgress(progress(0.45f + f.coerceIn(0f, 1f) * 0.05f), msg)
                }
                if (ext.isFailure) {
                    lastFailure = "Gagal menyiapkan Java $runtime: ${ext.exceptionOrNull()?.message}"
                    continue@runtimeLoop
                }
            }
            for ((index, command) in commands.withIndex()) {
                if (index > 0) delay(RESTART_DELAY_MS) // beri waktu proses :forge sebelumnya benar-benar mati
                onProgress(progress(0.50f), "Memasang $name $forgeVersion...")
                var processorsDone = 0
                val result = ForgeInstallRunner.run(context, installerJar, command, runtime, name) { line ->
                    if (line.startsWith("=====")) processorsDone++
                    val frac = if (clientProcessorCount > 0) (processorsDone.toFloat() / clientProcessorCount).coerceIn(0f, 1f) else 0f
                    onProgress(progress(0.50f + frac * 0.48f), "Memasang $name $forgeVersion ($processorsDone/$clientProcessorCount)...")
                }
                if (result.exitCode == 0) {
                    success = true
                    break@runtimeLoop
                }
                lastFailure = result.describeFailure()
                if (ForgeInstallerCli.isWrongJava(result.log)) continue@runtimeLoop // coba Java berikutnya
                if (!ForgeInstallerCli.isUnknownOption(result.log)) break@runtimeLoop // galat sungguhan
                // opsi ditolak: coba bentuk perintah berikutnya
            }
        }
        if (!success) throw IOException(lastFailure)

        // 6. Pastikan hasil utamanya benar-benar ada sebelum menandai selesai
        val patchedPath = ForgeMaven.pathOfDataEntry(profile.data?.get("PATCHED")?.client)
        if (patchedPath != null) {
            val patched = SafePaths.resolveInside(libDir, patchedPath)
            if (patched == null || !patched.exists() || patched.length() == 0L) {
                throw IOException("Installer $name selesai tetapi jar Minecraft hasil tambalan tidak ditemukan ($patchedPath)")
            }
        }

        // 7. Tandai selesai: salin profilnya ke versions/<mc>/<forge|neoforge>-<versi>.json (ditulis paling akhir)
        val marker = File(File(versionsDir, mcVersion), SafePaths.fileName(flavor.markerName(forgeVersion)))
        val tmp = File(marker.parentFile, marker.name + ".tmp")
        tmp.writeText(versionJsonText)
        if (marker.exists()) marker.delete()
        if (!tmp.renameTo(marker)) {
            tmp.copyTo(marker, overwrite = true)
            tmp.delete()
        }

        // 8. Rapikan: folder versi buatan installer dan installer itu sendiri tidak dipakai lagi
        val installedId = profile.version?.takeIf { it.isNotBlank() }
        if (installedId != null && installedId != mcVersion) {
            val extraDir = SafePaths.resolveInside(versionsDir, SafePaths.fileName(installedId))
            if (extraDir != null && extraDir.isDirectory) extraDir.deleteRecursively()
        }
        installerJar.delete()
        onProgress(P_END, "$name $forgeVersion terpasang")
    }

    // ---------------------------------------------------------------- unduhan

    private class LibJob(val name: String, val url: String, val target: File, val sha1: String?, val size: Long)

    private suspend fun prefetchLibraries(
        downloader: MinecraftDownloader,
        libDir: File,
        versionJson: ForgeVersionJson,
        profile: ForgeInstallProfile,
        name: String,
        onProgress: (Float, String) -> Unit
    ) {
        val jobs = ((versionJson.libraries ?: emptyList()) + (profile.libraries ?: emptyList()))
            .mapNotNull { lib ->
                // Tanpa alamat berarti library itu dibawa di dalam installer dan diekstrak oleh installer
                val url = ForgeMaven.libraryUrl(lib)?.replaceFirst("http://", "https://") ?: return@mapNotNull null
                val path = ForgeMaven.libraryPath(lib) ?: return@mapNotNull null
                val target = SafePaths.resolveInside(libDir, path)
                    ?: throw IOException("Path library $name tidak aman: $path")
                LibJob(lib.name ?: path, url, target, lib.downloads?.artifact?.sha1, lib.downloads?.artifact?.size ?: 0L)
            }
            .distinctBy { it.target.path }

        if (jobs.isEmpty()) return
        val failed = CopyOnWriteArrayList<String>()
        val done = java.util.concurrent.atomic.AtomicInteger(0)
        val gate = Semaphore(LIB_THREADS)
        coroutineScope {
            jobs.map { job ->
                async(Dispatchers.IO) {
                    gate.withPermit {
                        if (!isUpToDate(job.target, job.sha1, job.size)) {
                            job.target.parentFile?.mkdirs()
                            try {
                                downloader.downloadFile(job.url, job.target, job.size, expectedSha1 = job.sha1)
                            } catch (e: Exception) {
                                failed.add(job.name)
                            }
                        }
                        val n = done.incrementAndGet()
                        onProgress(progress(0.08f + (n.toFloat() / jobs.size) * 0.32f), "Mengunduh library $name ($n/${jobs.size})...")
                    }
                }
            }.awaitAll()
        }
        if (failed.isNotEmpty()) {
            val shown = failed.take(3).joinToString(", ")
            val more = if (failed.size > 3) " " + Tr.t("dan ${failed.size - 3} lainnya") else ""
            val list = shown + more
            throw IOException("Gagal mengunduh library $name: $list. Periksa koneksi lalu coba lagi.")
        }
    }

    private fun prefetchMojangMappings(
        downloader: MinecraftDownloader,
        libDir: File,
        versionsDir: File,
        mcVersion: String,
        profile: ForgeInstallProfile
    ) {
        val path = ForgeMaven.pathOfDataEntry(profile.data?.get("MOJMAPS")?.client) ?: return
        val target = SafePaths.resolveInside(libDir, path) ?: throw IOException("Path mappings tidak aman: $path")
        if (target.exists() && target.length() > 0L) return

        val mojangJson = File(File(versionsDir, mcVersion), "$mcVersion.json")
        val mappings = JsonParser.parseString(mojangJson.readText()).asJsonObject
            .getAsJsonObject("downloads")?.getAsJsonObject("client_mappings")
            ?: throw IllegalStateException("Mojang tidak menyediakan mappings untuk Minecraft $mcVersion")
        val url = mappings.get("url")?.asString ?: throw IllegalStateException("Alamat mappings Mojang kosong")
        target.parentFile?.mkdirs()
        downloader.downloadFile(
            url = url,
            targetFile = target,
            expectedSize = mappings.get("size")?.asLong ?: 0L,
            expectedSha1 = mappings.get("sha1")?.asString
        )
    }

    private fun ensureLauncherProfiles(nuxDir: File) {
        val f = File(nuxDir, "launcher_profiles.json")
        if (f.exists() && f.length() > 0L) return
        f.writeText("{\"profiles\":{},\"version\":3}")
    }

    // ---------------------------------------------------------------- bantuan

    private fun isUpToDate(file: File, sha1: String?, size: Long): Boolean {
        if (!file.exists() || file.length() == 0L) return false
        if (sha1.isNullOrBlank()) return size <= 0L || file.length() == size
        return sha1Of(file).equals(sha1.trim(), ignoreCase = true)
    }

    private fun sha1Of(file: File): String {
        val digest = MessageDigest.getInstance("SHA-1")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) digest.update(buffer, 0, read)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /** Berkas `.sha1` di samping installer (server resmi lalu mirror); null bila tidak bisa diambil. */
    private fun fetchSha1(installerUrl: String): String? {
        val candidates = listOf(
            "$installerUrl.sha1",
            "$installerUrl.sha1"
                .replace("https://maven.minecraftforge.net", "https://bmclapi2.bangbang93.com/maven")
                .replace("https://maven.neoforged.net/releases", "https://bmclapi2.bangbang93.com/maven")
        ).distinct()
        for (url in candidates) {
            try {
                val request = Request.Builder().url(url).header("User-Agent", "NovaCraft/1.0 (Android)").build()
                FabricService.client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val text = response.body?.string()?.trim()?.take(40)?.lowercase() ?: return@use
                    if (text.length == 40 && text.all { it in '0'..'9' || it in 'a'..'f' }) return text
                }
            } catch (_: Exception) {
            }
        }
        return null
    }

    private fun isUsableInstaller(file: File): Boolean {
        if (!file.exists() || file.length() == 0L) return false
        return try {
            ZipFile(file).use { it.getEntry("install_profile.json") != null }
        } catch (_: Exception) {
            false
        }
    }

    private fun readInstallerMeta(installerJar: File, name: String): Pair<ForgeInstallProfile, String> {
        ZipFile(installerJar).use { zip ->
            val profileEntry = zip.getEntry("install_profile.json")
                ?: throw IllegalStateException("install_profile.json tidak ada di installer $name")
            val profileText = zip.getInputStream(profileEntry).bufferedReader().use { it.readText() }
            val profile = gson.fromJson(profileText, ForgeInstallProfile::class.java)
                ?: throw IllegalStateException("install_profile.json tidak terbaca")
            val jsonName = (profile.json ?: "/version.json").trimStart('/')
            val jsonEntry = zip.getEntry(jsonName)
                ?: throw IllegalStateException("$jsonName tidak ada di installer $name")
            val versionText = zip.getInputStream(jsonEntry).bufferedReader().use { it.readText() }
            return Pair(profile, versionText)
        }
    }

    private fun readJavaMajor(mojangJson: File): Int? = try {
        JsonParser.parseString(mojangJson.readText()).asJsonObject
            .getAsJsonObject("javaVersion")?.get("majorVersion")?.asInt
    } catch (_: Exception) {
        null
    }
}

// ----------------------------------------------------------------------------
// Section: core/forge/NovaForgeInstallCore.kt
// ----------------------------------------------------------------------------

/**
 * NovaCraft unified Forge/NeoForge CLI, Launch Resolution, and Runner Core:
 * - InstallerCommand & ForgeInstallerCli
 * - ForgeLaunchInfo & ForgeLaunch
 * - ForgeRunResult & ForgeInstallRunner
 */

internal class InstallerCommand(val mainClass: String, val args: List<String>)

internal object ForgeInstallerCli {

    private const val FORGE_MAIN = "net.minecraftforge.installer.SimpleInstaller"
    private const val NEOFORGE_MAIN_FALLBACK = "net.neoforged.installer.SimpleInstaller"

    private const val OPT_CLIENT_CAMEL = "--installClient"
    private const val OPT_CLIENT_KEBAB = "--install-client"
    private const val OPT_OFFLINE = "--offline"

    private const val MAX_CLASS_BYTES = 4L * 1024 * 1024

    class Hints(
        val mainClass: String?,
        val hasCamelClient: Boolean,
        val hasKebabClient: Boolean,
        val hasOffline: Boolean
    )

    fun commands(flavor: ForgeFlavor, hints: Hints?, targetDir: String): List<InstallerCommand> {
        if (flavor == ForgeFlavor.FORGE) {
            return listOf(InstallerCommand(FORGE_MAIN, listOf(OPT_CLIENT_CAMEL, targetDir, OPT_OFFLINE)))
        }
        val main = hints?.mainClass ?: NEOFORGE_MAIN_FALLBACK

        val clientOptions = LinkedHashSet<String>()
        if (hints?.hasCamelClient == true) clientOptions.add(OPT_CLIENT_CAMEL)
        if (hints?.hasKebabClient == true) clientOptions.add(OPT_CLIENT_KEBAB)
        clientOptions.add(OPT_CLIENT_CAMEL)
        clientOptions.add(OPT_CLIENT_KEBAB)

        val result = ArrayList<InstallerCommand>()
        if (hints == null || hints.hasOffline) {
            clientOptions.forEach { result.add(InstallerCommand(main, listOf(it, targetDir, OPT_OFFLINE))) }
        }
        clientOptions.forEach { result.add(InstallerCommand(main, listOf(it, targetDir))) }
        return result
    }

    fun isUnknownOption(log: String): Boolean {
        val l = log.lowercase()
        return l.contains("not a recognized option") || l.contains("unrecognized option") ||
            l.contains("unknown option") || l.contains("unmatched argument")
    }

    fun isWrongJava(log: String): Boolean =
        log.contains("UnsupportedClassVersionError") || log.contains("class file version")

    fun readHints(installerJar: File): Hints? = try {
        ZipFile(installerJar).use { zip ->
            val mainClass = zip.getEntry("META-INF/MANIFEST.MF")?.let { entry ->
                zip.getInputStream(entry).use { Manifest(it).mainAttributes.getValue("Main-Class") }
            }?.trim()?.takeIf { it.isNotEmpty() }

            var camel = false
            var kebab = false
            var offline = false
            val entries = zip.entries()
            while (entries.hasMoreElements() && !(camel && kebab && offline)) {
                val entry = entries.nextElement()
                if (entry.isDirectory || !entry.name.endsWith(".class")) continue
                if (entry.size > MAX_CLASS_BYTES) continue
                val bytes = zip.getInputStream(entry).use { it.readBytes() }
                if (!camel && contains(bytes, "installClient")) camel = true
                if (!kebab && contains(bytes, "install-client")) kebab = true
                if (!offline && contains(bytes, "offline")) offline = true
            }
            Hints(mainClass, camel, kebab, offline)
        }
    } catch (_: Exception) {
        null
    }

    internal fun contains(haystack: ByteArray, needle: String): Boolean {
        val n = needle.toByteArray(Charsets.US_ASCII)
        if (n.isEmpty() || haystack.size < n.size) return false
        val first = n[0]
        val last = haystack.size - n.size
        var i = 0
        while (i <= last) {
            if (haystack[i] == first) {
                var j = 1
                while (j < n.size && haystack[i + j] == n[j]) j++
                if (j == n.size) return true
            }
            i++
        }
        return false
    }
}

class ForgeLaunchInfo(
    val mainClass: String,
    val libraryFiles: List<File>,
    val libraryKeys: Set<String>,
    val jvmArgs: List<String>,
    val gameArgs: List<String>
)

object ForgeLaunch {

    private val AMBER_MANAGED_JVM_PREFIXES = listOf(
        "-Djava.net.preferIPv6Addresses",
        "-Djava.library.path",
        "-Xmx",
        "-Xms",
        "-XstartOnFirstThread"
    )

    private val gson = Gson()

    fun resolve(profileFile: File, libDir: File, mcVersion: String): ForgeLaunchInfo? {
        val parsed: ForgeVersionJson? = try {
            gson.fromJson(profileFile.readText(), ForgeVersionJson::class.java)
        } catch (_: Exception) {
            null
        }
        val version = parsed ?: return null
        val mainClass = version.mainClass?.takeIf { it.isNotBlank() } ?: return null

        val files = LinkedHashSet<File>()
        val keys = LinkedHashSet<String>()
        for (lib in version.libraries ?: emptyList()) {
            val name = lib.name ?: continue
            if (name.contains("org.lwjgl")) continue
            ForgeMaven.keyOf(name)?.let { keys.add(it) }
            val path = ForgeMaven.libraryPath(lib) ?: continue
            val file = SafePaths.resolveInside(libDir, path) ?: continue
            if (file.exists() && file.length() > 0L) files.add(file)
        }

        val vars = mapOf(
            "library_directory" to libDir.absolutePath,
            "classpath_separator" to File.pathSeparator,
            "version_name" to (version.id ?: mcVersion),
            "primary_jar_name" to "$mcVersion.jar"
        )

        val rawJvm = version.arguments?.jvm
        val rawGame = version.arguments?.game

        val jvmArgs = resolveArguments(rawJvm, vars, dropClasspath = true)
            .filterNot { arg -> AMBER_MANAGED_JVM_PREFIXES.any { arg.startsWith(it) } }
            .map { arg ->
                if (arg.startsWith("-DignoreList=") && !arg.split(',').contains("$mcVersion.jar")) "$arg,$mcVersion.jar" else arg
            }

        val gameArgs = if (rawGame != null) {
            resolveArguments(rawGame, vars)
        } else {
            legacyGameArguments(version.minecraftArguments)
        }

        return ForgeLaunchInfo(mainClass, files.toList(), keys, jvmArgs, gameArgs)
    }

    private fun resolveArguments(raw: List<Any>?, vars: Map<String, String>, dropClasspath: Boolean = false): List<String> {
        if (raw == null) return emptyList()
        val out = ArrayList<String>()

        fun add(text: String) {
            val value = substitute(text, vars)
            if (value != null) {
                out.add(value)
            } else if (!text.startsWith("-") && out.isNotEmpty()) {
                val last = out.last()
                if (last.startsWith("-") && !last.contains("=")) out.removeAt(out.size - 1)
            }
        }

        var i = 0
        while (i < raw.size) {
            val item = raw[i]
            i++
            when (item) {
                is String -> {
                    if (dropClasspath && (item == "-cp" || item == "-classpath")) {
                        i++
                        continue
                    }
                    add(item)
                }
                is Map<*, *> -> {
                    if (!rulesAllow(item["rules"] as? List<*>)) continue
                    when (val value = item["value"]) {
                        is String -> add(value)
                        is List<*> -> value.forEach { v -> (v as? String)?.let { add(it) } }
                    }
                }
            }
        }
        return out
    }

    private fun substitute(arg: String, vars: Map<String, String>): String? {
        if (!arg.contains("\${")) return arg
        var result = arg
        for ((k, v) in vars) result = result.replace("\${$k}", v)
        return if (result.contains("\${")) null else result
    }

    private fun rulesAllow(rules: List<*>?): Boolean {
        if (rules == null) return true
        var allowed = false
        for (r in rules) {
            val rule = r as? Map<*, *> ?: continue
            if (rule["features"] != null) continue
            val os = rule["os"] as? Map<*, *>
            val matches = os == null || (os["name"] == "linux" && os["arch"] == null)
            if (matches) allowed = rule["action"] == "allow"
        }
        return allowed
    }

    private fun legacyGameArguments(text: String?): List<String> {
        if (text.isNullOrBlank()) return emptyList()
        val tokens = text.trim().split(Regex("\\s+"))
        val out = ArrayList<String>()
        var i = 0
        while (i < tokens.size) {
            val t = tokens[i]
            if (t.startsWith("--") && i + 1 < tokens.size && tokens[i + 1].contains("\${")) {
                i += 2
                continue
            }
            if (!t.contains("\${")) out.add(t)
            i++
        }
        return out
    }
}

internal class ForgeRunResult(val exitCode: Int, val log: String, val label: String = "Forge") {

    fun describeFailure(): String {
        val lines = log.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val interesting = lines.filter {
            it.contains("error", ignoreCase = true) || it.contains("exception", ignoreCase = true) ||
                it.contains("failed", ignoreCase = true) || it.contains("missing", ignoreCase = true) ||
                it.contains("checksum", ignoreCase = true) || it.contains("invalid", ignoreCase = true) ||
                it.startsWith("[$label]")
        }
        val reason = (interesting.ifEmpty { lines }).takeLast(3).joinToString(" | ").take(400)
        return "Installer $label gagal (kode $exitCode)" + if (reason.isNotEmpty()) ": $reason" else ""
    }
}

internal object ForgeInstallRunner {

    private const val TIMEOUT_MS = 25 * 60 * 1000L
    private const val POLL_MS = 300L

    suspend fun run(
        context: Context,
        installerJar: File,
        command: InstallerCommand,
        runtimeName: String,
        label: String,
        onLine: (String) -> Unit
    ): ForgeRunResult = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val work = ForgeInstallService.workDir(app)
        work.deleteRecursively()
        work.mkdirs()
        val logFile = File(work, ForgeInstallService.LOG_NAME)
        val resultFile = File(work, ForgeInstallService.RESULT_NAME)

        val intent = Intent(app, ForgeInstallService::class.java)
            .putExtra(ForgeInstallService.EXTRA_INSTALLER, installerJar.absolutePath)
            .putExtra(ForgeInstallService.EXTRA_MAIN_CLASS, command.mainClass)
            .putExtra(ForgeInstallService.EXTRA_ARGS, command.args.toTypedArray())
            .putExtra(ForgeInstallService.EXTRA_LABEL, label)
            .putExtra(
                ForgeInstallService.EXTRA_RUNTIME_HOME,
                JavaRuntimeManager.resolveRuntimeHome(app, runtimeName).absolutePath
            )

        val died = CompletableDeferred<Unit>()
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                try {
                    binder?.linkToDeath({ died.complete(Unit) }, 0)
                } catch (_: RemoteException) {
                    died.complete(Unit)
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                died.complete(Unit)
            }
        }

        val tail = ArrayDeque<String>()
        var offset = 0L
        fun drain() {
            offset = readNewLines(logFile, offset) { line ->
                tail.addLast(line)
                if (tail.size > 300) tail.removeFirst()
                onLine(line)
            }
        }

        var bound = false
        try {
            try {
                ContextCompat.startForegroundService(app, intent)
            } catch (_: Exception) {
            }
            bound = app.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            if (!bound) {
                return@withContext ForgeRunResult(-1, "[$label] Proses pemasangan tidak bisa dimulai", label)
            }

            val startedAt = SystemClock.elapsedRealtime()
            var exitCode = -1
            while (true) {
                currentCoroutineContext().ensureActive()
                drain()
                if (resultFile.exists()) {
                    delay(150)
                    drain()
                    exitCode = resultFile.readText().trim().toIntOrNull() ?: -1
                    break
                }
                if (died.isCompleted) {
                    delay(500)
                    drain()
                    exitCode = resultFile.takeIf { it.exists() }?.readText()?.trim()?.toIntOrNull() ?: -1
                    if (exitCode != 0 && !resultFile.exists()) {
                        tail.addLast("[$label] Proses installer berhenti mendadak (kemungkinan RAM HP habis)")
                    }
                    break
                }
                if (SystemClock.elapsedRealtime() - startedAt > TIMEOUT_MS) {
                    exitCode = -2
                    tail.addLast("[$label] Waktu pemasangan habis")
                    break
                }
                delay(POLL_MS)
            }
            ForgeRunResult(exitCode, tail.joinToString("\n"), label)
        } finally {
            if (bound) {
                try {
                    app.unbindService(connection)
                } catch (_: Exception) {
                }
            }
            try {
                app.stopService(intent)
            } catch (_: Exception) {
            }
        }
    }

    private fun readNewLines(file: File, offset: Long, onLine: (String) -> Unit): Long {
        if (!file.exists()) return offset
        return try {
            RandomAccessFile(file, "r").use { raf ->
                val available = raf.length() - offset
                if (available <= 0L) return@use offset
                val bytes = ByteArray(available.coerceAtMost(1L shl 20).toInt())
                raf.seek(offset)
                raf.readFully(bytes)
                val lastNewline = bytes.lastIndexOf('\n'.code.toByte())
                if (lastNewline < 0) return@use offset
                String(bytes, 0, lastNewline + 1, Charsets.UTF_8)
                    .split('\n')
                    .forEach { line ->
                        val l = line.trimEnd('\r')
                        if (l.isNotEmpty()) onLine(l)
                    }
                offset + lastNewline + 1
            }
        } catch (_: Exception) {
            offset
        }
    }
}

// ============================================================================
// Section: NovaRendererAndServicesCore.kt
// ============================================================================

// ----------------------------------------------------------------------------
// Section: core/renderer/v2/RendererV2Models.kt
// ----------------------------------------------------------------------------

/**
 * Data model untuk Renderer Plugin V2 (kompatibel dengan format fclPlugin_V2 Zalith)
 */
data class RendererV2Config(
    val displayName: String,
    val rendererId: String,
    val rendererGLPath: String,
    val rendererEGLPath: String,
    val dlopenLibPaths: List<String> = emptyList(),
    val env: List<EnvConfig> = emptyList(),
    val minMCVer: String? = null,
    val maxMCVer: String? = null
)

sealed interface EnvConfig {
    val key: String

    data class NormalEnv(
        override val key: String,
        val value: String
    ) : EnvConfig

    data class SelectableEnv(
        override val key: String,
        val title: String? = null,
        val check: Boolean? = true,
        val defaultValue: String,
        val values: List<String>
    ) : EnvConfig

    data class CustomizableEnv(
        override val key: String,
        val title: String? = null,
        val defaultValue: String? = null
    ) : EnvConfig

    data class ToggleableEnv(
        override val key: String,
        val value: String,
        val title: String? = null,
        val toggle: Boolean = false
    ) : EnvConfig
}

/**
 * Unit pengaturan UI untuk masing-masing tipe environment variable
 */
sealed class EnvSettingUnit(
    val key: String,
    val summary: String?,
    protected val prefs: SharedPreferences,
    protected val storageKey: String,
    val defaultValue: String
) {
    var state by mutableStateOf(defaultValue)

    open fun init() {
        state = prefs.getString(storageKey, defaultValue) ?: defaultValue
    }

    open fun save(value: String) {
        state = value
        prefs.edit().putString(storageKey, value).apply()
    }

    open fun reset() {
        save(defaultValue)
    }

    class Selectable(
        key: String,
        val rawEnv: EnvConfig.SelectableEnv,
        val values: List<String>,
        summary: String?,
        prefs: SharedPreferences,
        storageKey: String
    ) : EnvSettingUnit(key, summary, prefs, storageKey, rawEnv.defaultValue) {
        private val checkKey = "${storageKey}:check"
        var isEnabled by mutableStateOf(rawEnv.check != false)

        override fun init() {
            super.init()
            val pluginDefault = rawEnv.check != false
            isEnabled = if (rawEnv.check == null) {
                true
            } else if (prefs.contains(checkKey)) {
                prefs.getBoolean(checkKey, pluginDefault)
            } else {
                pluginDefault
            }

            if (state !in values) {
                save(rawEnv.defaultValue)
            }
        }

        fun saveCheck(enabled: Boolean) {
            isEnabled = enabled
            prefs.edit().putBoolean(checkKey, enabled).apply()
        }

        override fun reset() {
            super.reset()
            saveCheck(rawEnv.check != false)
        }
    }

    class Customizable(
        key: String,
        val rawEnv: EnvConfig.CustomizableEnv,
        summary: String?,
        prefs: SharedPreferences,
        storageKey: String
    ) : EnvSettingUnit(key, summary, prefs, storageKey, rawEnv.defaultValue ?: "")

    class Toggleable(
        key: String,
        val rawEnv: EnvConfig.ToggleableEnv,
        val envValue: String,
        summary: String?,
        prefs: SharedPreferences,
        storageKey: String
    ) : EnvSettingUnit(key, summary, prefs, storageKey, if (rawEnv.toggle) envValue else "") {
        val isEnabled: Boolean get() = state.isNotEmpty()

        fun setToggle(enabled: Boolean) {
            save(if (enabled) envValue else "")
        }

        override fun reset() {
            setToggle(rawEnv.toggle)
        }
    }
}

/**
 * Data lengkap representasi Renderer V2
 */
class RendererV2Data(
    val packageName: String,
    val nativePath: String,
    val summary: String,
    val config: RendererV2Config,
    context: Context
) {
    private val prefs = context.getSharedPreferences("NuxRendererV2EnvConfig", Context.MODE_PRIVATE)
    val units: List<EnvSettingUnit>

    init {
        val list = mutableListOf<EnvSettingUnit>()
        val prefix = "$packageName:"

        for (env in config.env) {
            val storageKey = "$prefix${env.key}"
            when (env) {
                is EnvConfig.NormalEnv -> {}
                is EnvConfig.SelectableEnv -> {
                    val allValues = buildList {
                        add(env.defaultValue)
                        for (v in env.values) {
                            if (v != env.defaultValue) add(v)
                        }
                    }
                    val unit = EnvSettingUnit.Selectable(
                        key = env.key,
                        rawEnv = env,
                        values = allValues,
                        summary = env.title,
                        prefs = prefs,
                        storageKey = storageKey
                    )
                    unit.init()
                    list.add(unit)
                }
                is EnvConfig.CustomizableEnv -> {
                    val unit = EnvSettingUnit.Customizable(
                        key = env.key,
                        rawEnv = env,
                        summary = env.title,
                        prefs = prefs,
                        storageKey = storageKey
                    )
                    unit.init()
                    list.add(unit)
                }
                is EnvConfig.ToggleableEnv -> {
                    val unit = EnvSettingUnit.Toggleable(
                        key = env.key,
                        rawEnv = env,
                        envValue = env.value,
                        summary = env.title,
                        prefs = prefs,
                        storageKey = storageKey
                    )
                    unit.init()
                    list.add(unit)
                }
            }
        }
        units = list
    }

    /**
     * Menghasilkan map environment variables yang aktif untuk diinjeksi ke proses game
     */
    fun getEffectiveEnv(): Map<String, String> {
        val result = mutableMapOf<String, String>()

        for (env in config.env) {
            when (env) {
                is EnvConfig.NormalEnv -> {
                    result[env.key] = env.value
                }
                is EnvConfig.SelectableEnv -> {
                    val u = units.firstOrNull { it.key == env.key } as? EnvSettingUnit.Selectable
                    if (u != null && u.isEnabled && u.state.isNotBlank()) {
                        result[env.key] = u.state
                    }
                }
                is EnvConfig.CustomizableEnv -> {
                    val u = units.firstOrNull { it.key == env.key } as? EnvSettingUnit.Customizable
                    if (u != null && u.state.isNotBlank()) {
                        result[env.key] = u.state
                    }
                }
                is EnvConfig.ToggleableEnv -> {
                    val u = units.firstOrNull { it.key == env.key } as? EnvSettingUnit.Toggleable
                    if (u != null && u.isEnabled) {
                        result[env.key] = u.envValue
                    }
                }
            }
        }
        return result
    }

    fun resetAll() {
        units.forEach { it.reset() }
    }
}

// ----------------------------------------------------------------------------
// Section: core/renderer/v2/NuxRendererV2Manager.kt
// ----------------------------------------------------------------------------

object NuxRendererV2Manager {
    private const val TAG = "NuxRendererV2Manager"
    private val v2Plugins = mutableListOf<RendererV2Data>()

    fun getAllV2Plugins(): List<RendererV2Data> = synchronized(v2Plugins) {
        v2Plugins.toList()
    }

    fun getPluginData(packageNameOrId: String): RendererV2Data? = synchronized(v2Plugins) {
        if (packageNameOrId.isBlank()) return null
        v2Plugins.firstOrNull { 
            it.packageName.equals(packageNameOrId, ignoreCase = true) || 
            it.config.rendererId.equals(packageNameOrId, ignoreCase = true) ||
            it.config.displayName.equals(packageNameOrId, ignoreCase = true) ||
            "plugin_${it.packageName}".equals(packageNameOrId, ignoreCase = true) ||
            (packageNameOrId.contains("mobilegl", ignoreCase = true) && !packageNameOrId.contains("mobileglue", ignoreCase = true) && it.packageName == "top.mobilegl.plugin")
        }
    }

    /**
     * Memeriksa apakah renderer memiliki unit pengaturan konfigurasi V2 aktif.
     * Khusus MobileGlues bawaan/plugin lama selalu return false (sesuai Zalith).
     */
    fun isConfigurableRenderer(renderer: NuxRendererInfo): Boolean {
        if (renderer.id == "mobileglues" || 
            renderer.id.contains("mobileglue", ignoreCase = true) || 
            renderer.pluginPackageName == "com.fcl.plugin.mobileglues" || 
            renderer.displayName.contains("MobileGlues", ignoreCase = true)) {
            return false
        }
        val targetData = getPluginData(renderer.pluginPackageName ?: "") 
            ?: getPluginData(renderer.id) 
            ?: getPluginData(renderer.displayName)
        return targetData != null && targetData.units.isNotEmpty()
    }

    /**
     * Memindai plugin renderer V2 dari seluruh aplikasi yang terpasang di Android (fclPlugin_V2 & MobileGL)
     */
    fun scanV2Plugins(context: Context, preloaded: List<ApplicationInfo>? = null): List<RendererV2Data> {
        val result = mutableListOf<RendererV2Data>()
        val pm = context.packageManager
        val installedApps = preloaded?.takeIf { it.isNotEmpty() } ?: try {
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mengambil daftar aplikasi terpasang", e)
            emptyList()
        }

        for (info in installedApps) {
            val pkg = info.packageName
            if (pkg == context.packageName) continue
            // Plugin renderer = APK yang dipasang pengguna (sama seperti Zalith); aplikasi sistem dilewati.
            if ((info.flags and ApplicationInfo.FLAG_SYSTEM) != 0) continue

            val metaData = info.metaData ?: continue
            val nativeLibDir = info.nativeLibraryDir
            // Label mahal (membuka resource aplikasi): hanya dimuat bila plugin ditemukan.
            val appLabel by lazy(LazyThreadSafetyMode.NONE) {
                runCatching { info.loadLabel(pm).toString() }.getOrDefault(pkg)
            }

            // 1. Coba baca fclPlugin_V2 jika tersedia (MobileGL, NGG V2, dsb)
            val configResId = runCatching { metaData.getInt("fclPlugin_V2", -1).takeIf { it > 0 } }.getOrNull()
            if (configResId != null) {
                try {
                    val res = pm.getResourcesForApplication(info)
                    val jsonString = res.getString(configResId)
                    val parsedConfig = parseRendererConfigJson(jsonString, nativeLibDir, info, pm)
                    if (parsedConfig != null) {
                        val data = RendererV2Data(
                            packageName = pkg,
                            nativePath = nativeLibDir,
                            summary = "Renderer Plugin V2: $appLabel",
                            config = parsedConfig,
                            context = context
                        )
                        result.add(data)
                        Log.i(TAG, "Berhasil memuat Renderer Plugin V2 dari $pkg ($appLabel)")
                        continue
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Gagal mengurai fclPlugin_V2 dari $pkg", e)
                }
            }

            // 2. Fallback khusus untuk MobileGL (top.mobilegl.plugin) jika manifest resource gagal diurai
            // Nama paket harus persis; tebakan "mengandung kata mobilegl" membuat aplikasi lain ikut terdaftar.
            val isMobileGLPkg = pkg == "top.mobilegl.plugin"

            if (isMobileGLPkg) {
                val mgConfig = createDefaultMobileGLV2Config(nativeLibDir)
                val data = RendererV2Data(
                    packageName = pkg,
                    nativePath = nativeLibDir,
                    summary = "MobileGL Graphics Engine ($appLabel)",
                    config = mgConfig,
                    context = context
                )
                result.add(data)
                Log.i(TAG, "Menerapkan profil konfigurasi V2 bawaan untuk MobileGL ($pkg)")
            }
        }

        // Jika MobileGL terdeteksi tetapi belum ada konfigurasi, atau fallback default untuk MobileGL
        if (result.none { it.packageName == "top.mobilegl.plugin" }) {
            val defaultMobileGLData = RendererV2Data(
                packageName = "top.mobilegl.plugin",
                nativePath = "",
                summary = "MobileGL Engine (Standard V2 Profile)",
                config = createDefaultMobileGLV2Config(""),
                context = context
            )
            result.add(defaultMobileGLData)
        }

        synchronized(v2Plugins) {
            v2Plugins.clear()
            v2Plugins.addAll(result)
        }

        return result
    }

    /**
     * Mengurai string JSON fclPlugin_V2 menjadi objek RendererV2Config
     */
    private fun parseRendererConfigJson(
        jsonString: String, 
        nativeLibDir: String,
        info: ApplicationInfo? = null,
        pm: PackageManager? = null
    ): RendererV2Config? {
        return try {
            val root = JSONObject(jsonString)
            val displayName = root.optString("displayName", "MobileGL")
            val rendererId = root.optString("rendererId", "opengles3")
            val rendererGLPath = resolveNativePath(root.optString("rendererGLPath", ""), nativeLibDir)
            val rendererEGLPath = resolveNativePath(root.optString("rendererEGLPath", ""), nativeLibDir)

            val dlopenLibPaths = mutableListOf<String>()
            val dlArray = root.optJSONArray("dlopenLibPaths")
            if (dlArray != null) {
                for (i in 0 until dlArray.length()) {
                    val p = dlArray.getString(i)
                    if (p.isNotBlank()) dlopenLibPaths.add(resolveNativePath(p, nativeLibDir))
                }
            }

            val envList = mutableListOf<EnvConfig>()
            val envArray = root.optJSONArray("env")
            if (envArray != null) {
                for (i in 0 until envArray.length()) {
                    val envObj = envArray.getJSONObject(i)
                    val envType = envObj.optString("type", envObj.optString("@type", ""))
                    val key = envObj.optString("key", "")
                    if (key.isBlank()) continue

                    val rawTitleKey = envObj.optJSONObject("title")?.optString("key") ?: envObj.optString("title", null)
                    val resolvedTitle = resolveEnvTitle(rawTitleKey, info, pm)

                    when {
                        envType.contains("Selectable") || envObj.has("items") -> {
                            val itemsObj = envObj.optJSONObject("items")
                            val defVal = itemsObj?.optString("defaultValue", "") ?: ""
                            val vals = mutableListOf<String>()
                            val valArr = itemsObj?.optJSONArray("values")
                            if (valArr != null) {
                                for (j in 0 until valArr.length()) vals.add(valArr.getString(j))
                            }
                            val check = if (envObj.has("check")) envObj.optBoolean("check", true) else null
                            envList.add(EnvConfig.SelectableEnv(key, resolvedTitle, check, defVal, vals))
                        }
                        envType.contains("Toggleable") || envObj.has("toggle") -> {
                            val value = resolveNativePath(envObj.optString("value", "1"), nativeLibDir)
                            val toggle = envObj.optBoolean("toggle", false)
                            envList.add(EnvConfig.ToggleableEnv(key, value, resolvedTitle, toggle))
                        }
                        envType.contains("Customizable") || envObj.has("defaultValue") -> {
                            val defVal = envObj.optString("defaultValue", null)
                            envList.add(EnvConfig.CustomizableEnv(key, resolvedTitle, defVal))
                        }
                        else -> {
                            val value = resolveNativePath(envObj.optString("value", ""), nativeLibDir)
                            envList.add(EnvConfig.NormalEnv(key, value))
                        }
                    }
                }
            }

            RendererV2Config(
                displayName = displayName,
                rendererId = rendererId,
                rendererGLPath = rendererGLPath,
                rendererEGLPath = rendererEGLPath,
                dlopenLibPaths = dlopenLibPaths,
                env = envList,
                minMCVer = root.optString("minMCVer", null),
                maxMCVer = root.optString("maxMCVer", null)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing renderer config JSON", e)
            null
        }
    }

    private fun resolveEnvTitle(titleKey: String?, info: ApplicationInfo?, pm: PackageManager?): String? {
        if (titleKey == null || titleKey.isBlank()) return null
        if (info != null && pm != null) {
            runCatching {
                val metaData = info.metaData
                if (metaData != null) {
                    val resId = metaData.getInt(titleKey, -1).takeIf { it > 0 }
                    if (resId != null) {
                        val res = pm.getResourcesForApplication(info)
                        val str = res.getString(resId)
                        if (str.isNotBlank()) return str
                    }
                }
            }
        }
        // Human-friendly dictionary fallback for known MobileGL keys
        return when (titleKey) {
            "mobilegl_backend_type_title" -> "Rendering backend (DirectGLES / DirectVulkan)"
            "mobilegl_use_angle_title" -> "Use ANGLE GLES libraries"
            "mobilegl_disable_subgroup_title" -> "Disable Vulkan shader subgroups"
            "mobilegl_disable_timerquery_title" -> "Disable GPU timer queries"
            "mobilegl_magma_r11g11b10f_fallback_title" -> "Use Magma R11G11B10F fallback"
            "mobilegl_magma_frames_inflight_title" -> "Magma frames in flight (1-64)"
            "mobilegl_avoid_sampler_mipmap_min_filter_title" -> "Avoid sampler mipmap min filter"
            "mobilegl_coherent_as_flush_title" -> "Coherent as flush"
            "mobilegl_relaxed_semantics_title" -> "Relaxed semantics"
            else -> titleKey
        }
    }

    private fun resolveNativePath(path: String, nativeLibDir: String): String {
        if (!path.startsWith("**|") || nativeLibDir.isBlank()) return path
        val rel = path.removePrefix("**|")
        return File(nativeLibDir, rel).absolutePath
    }

    /**
     * Profil V2 bawaan lengkap untuk MobileGL persis seperti di Zalith Launcher
     * (MOBILEGL_BACKEND_TYPE, MOBILEGL_DISABLE_TIMERQUERY, MOBILEGL_MAGMA_DISABLE_SUBGROUP, dll)
     */
    fun createDefaultMobileGLV2Config(nativeLibDir: String): RendererV2Config {
        val glPath = if (nativeLibDir.isNotBlank()) "$nativeLibDir/libMobileGL.so" else "libMobileGL.so"
        val envList = listOf(
            EnvConfig.SelectableEnv(
                key = "MOBILEGL_BACKEND_TYPE",
                title = "Rendering backend (DirectGLES / DirectVulkan)",
                check = true,
                defaultValue = "DirectGLES",
                values = listOf("DirectGLES", "DirectVulkan")
            ),
            EnvConfig.ToggleableEnv(
                key = "MOBILEGL_DISABLE_TIMERQUERY",
                value = "1",
                title = "Disable GPU timer queries",
                toggle = false
            ),
            EnvConfig.ToggleableEnv(
                key = "MOBILEGL_MAGMA_DISABLE_SUBGROUP",
                value = "1",
                title = "Disable Vulkan shader subgroups",
                toggle = false
            ),
            EnvConfig.ToggleableEnv(
                key = "MOBILEGL_MAGMA_R11G11B10F_FALLBACK",
                value = "1",
                title = "Use Magma R11G11B10F fallback",
                toggle = false
            ),
            EnvConfig.CustomizableEnv(
                key = "MOBILEGL_MAGMA_FRAMESINFLIGHT",
                title = "Magma frames in flight (1-64)",
                defaultValue = "3"
            ),
            EnvConfig.ToggleableEnv(
                key = "MOBILEGL_ESPRYT_AVOID_SAMPLER_MIPMAP_MIN_FILTER",
                value = "1",
                title = "Avoid sampler mipmap min filter",
                toggle = false
            ),
            EnvConfig.ToggleableEnv(
                key = "MOBILEGL_COHERENT_AS_FLUSH",
                value = "1",
                title = "Coherent as flush",
                toggle = false
            ),
            EnvConfig.ToggleableEnv(
                key = "MOBILEGL_RELAXED_SEMANTICS",
                value = "1",
                title = "Relaxed semantics",
                toggle = false
            ),
            EnvConfig.ToggleableEnv(
                key = "MOBILEGL_ESPRYT_USE_ANGLE",
                value = "1",
                title = "Use ANGLE GLES libraries",
                toggle = false
            ),
            EnvConfig.ToggleableEnv(
                key = "allow_glsl_extension_directive_midshader",
                value = "true",
                title = "Allow GLSL extension directive midshader",
                toggle = true
            ),
            EnvConfig.ToggleableEnv(
                key = "allow_higher_compat_version",
                value = "true",
                title = "Allow higher OpenGL compatibility version",
                toggle = true
            )
        )

        return RendererV2Config(
            displayName = "MobileGL",
            rendererId = "opengles3",
            rendererGLPath = glPath,
            rendererEGLPath = glPath,
            dlopenLibPaths = emptyList(),
            env = envList
        )
    }

    /**
     * Mengambil map environment variable efektif untuk disuntikkan ke proses game saat diluncurkan
     */
    fun getEffectiveEnv(targetRenderer: NuxRendererInfo): Map<String, String> {
        val pluginPkg = targetRenderer.pluginPackageName ?: ""
        val targetData = getPluginData(pluginPkg) 
            ?: getPluginData(targetRenderer.id) 
            ?: getPluginData(targetRenderer.displayName)
            ?: if (targetRenderer.displayName.equals("MobileGL", ignoreCase = true)) getPluginData("top.mobilegl.plugin") else null

        val result = mutableMapOf<String, String>()
        if (targetData != null) {
            result.putAll(targetData.getEffectiveEnv())
        }

        // Pastikan flag GLSL & Extension compatibility selalu aktif untuk MobileGL & Custom Renderer
        // (Standar kompatibilitas Zalith Launcher 2 untuk mencegah crash parsing shader Mojang & Iris)
        if (targetRenderer.id != "gl4es" && targetRenderer.rendererId != "opengles2") {
            result["allow_glsl_extension_directive_midshader"] = "true"
            result["allow_higher_compat_version"] = "true"
            result["force_glsl_extensions_warn"] = "true"
        }

        return result
    }
}

// ----------------------------------------------------------------------------
// Section: core/renderer/NovaRendererCore.kt
// ----------------------------------------------------------------------------

// ============================================================================
// NovaCraft Unified Renderer Core: Models, GL Error Policy, ELF Deps & MobileGlues Config
// ============================================================================

/**
 * Kebijakan pemeriksaan `glGetError` untuk Minecraft 26.x+ (backend renderpearl).
 */
object GlErrorPolicy {

    fun needsRealGlError(mcVersion: String): Boolean {
        val major = Regex("""^\s*(\d+)""").find(mcVersion)?.groupValues?.get(1)?.toIntOrNull() ?: return false
        return major >= 26
    }

    fun hasBackendChoice(mcVersion: String): Boolean = needsRealGlError(mcVersion)
}

/**
 * Metadata dan konfigurasi peluncuran untuk graphics renderer.
 */
data class NuxRendererInfo(
    val id: String,
    val displayName: String,
    val badge: String,
    val summary: String,
    val compatibility: String,
    val rendererId: String,
    val libraryName: String,
    val eglName: String? = null,
    val envVariables: Map<String, String> = emptyMap(),
    val requiresVulkan: Boolean = false,
    val isAdrenoRecommended: Boolean = false,
    val isMaliRecommended: Boolean = false,
    val isPlugin: Boolean = false,
    val pluginPackageName: String? = null,
    val pluginNativePath: String? = null,
    val dlopenLibs: List<String> = emptyList(),
    val minMCVersion: String? = null,
    val maxMCVersion: String? = null
) {
    val isConfigurable: Boolean
        get() {
            if (id == "mobileglues" ||
                id.contains("mobileglue", ignoreCase = true) ||
                pluginPackageName == "com.fcl.plugin.mobileglues" ||
                displayName.contains("MobileGlues", ignoreCase = true)
            ) {
                return false
            }

            if (pluginPackageName == "top.mobilegl.plugin" ||
                id == "mobilegl" ||
                id == "plugin_top.mobilegl.plugin" ||
                displayName.equals("MobileGL", ignoreCase = true) ||
                (displayName.contains("MobileGL", ignoreCase = true) && !displayName.contains("MobileGlues", ignoreCase = true))
            ) {
                return true
            }

            return com.aistudio.novacraft.launcher.core.launch.NuxRendererV2Manager.isConfigurableRenderer(this)
        }
}

/**
 * Pembaca dependensi ELF `DT_NEEDED` dari pustaka .so plugin renderer.
 */
object ElfDeps {

    private const val PT_LOAD = 1
    private const val PT_DYNAMIC = 2
    private const val DT_NULL = 0L
    private const val DT_NEEDED = 1L
    private const val DT_STRTAB = 5L

    private const val MAX_DYNAMIC_BYTES = 1 shl 20
    private const val MAX_STRTAB_BYTES = 1 shl 22
    private const val MAX_PHDRS = 256

    fun neededLibs(file: File): List<String> = try {
        RandomAccessFile(file, "r").use { read(it) }
    } catch (_: Throwable) {
        emptyList()
    }

    private fun read(raf: RandomAccessFile): List<String> {
        val ident = ByteArray(16)
        raf.seek(0)
        raf.readFully(ident)
        if (ident[0] != 0x7f.toByte() || ident[1] != 'E'.code.toByte() ||
            ident[2] != 'L'.code.toByte() || ident[3] != 'F'.code.toByte()
        ) return emptyList()
        val is64 = when (ident[4].toInt()) {
            1 -> false
            2 -> true
            else -> return emptyList()
        }
        val order = when (ident[5].toInt()) {
            1 -> ByteOrder.LITTLE_ENDIAN
            2 -> ByteOrder.BIG_ENDIAN
            else -> return emptyList()
        }

        val header = ByteBuffer.allocate(64).order(order)
        raf.seek(0)
        raf.readFully(header.array(), 0, if (is64) 64 else 52)
        val phOff: Long
        val phEntSize: Int
        val phNum: Int
        if (is64) {
            phOff = header.getLong(32)
            phEntSize = header.getShort(54).toInt() and 0xffff
            phNum = header.getShort(56).toInt() and 0xffff
        } else {
            phOff = header.getInt(28).toLong() and 0xffffffffL
            phEntSize = header.getShort(42).toInt() and 0xffff
            phNum = header.getShort(44).toInt() and 0xffff
        }
        if (phNum == 0 || phNum > MAX_PHDRS || phEntSize < (if (is64) 56 else 32)) return emptyList()

        val ph = ByteBuffer.allocate(phNum * phEntSize).order(order)
        raf.seek(phOff)
        raf.readFully(ph.array())

        class Seg(val vaddr: Long, val offset: Long, val fileSize: Long)

        val loads = ArrayList<Seg>()
        var dynOffset = -1L
        var dynSize = 0L
        for (i in 0 until phNum) {
            val b = i * phEntSize
            val type = ph.getInt(b)
            val offset: Long
            val vaddr: Long
            val fileSize: Long
            if (is64) {
                offset = ph.getLong(b + 8)
                vaddr = ph.getLong(b + 16)
                fileSize = ph.getLong(b + 32)
            } else {
                offset = ph.getInt(b + 4).toLong() and 0xffffffffL
                vaddr = ph.getInt(b + 8).toLong() and 0xffffffffL
                fileSize = ph.getInt(b + 16).toLong() and 0xffffffffL
            }
            if (type == PT_LOAD) loads.add(Seg(vaddr, offset, fileSize))
            if (type == PT_DYNAMIC) {
                dynOffset = offset
                dynSize = fileSize
            }
        }
        if (dynOffset < 0 || dynSize <= 0 || dynSize > MAX_DYNAMIC_BYTES) return emptyList()

        val dyn = ByteBuffer.allocate(dynSize.toInt()).order(order)
        raf.seek(dynOffset)
        raf.readFully(dyn.array())

        val entry = if (is64) 16 else 8
        val needed = ArrayList<Long>()
        var strtabVaddr = -1L
        var p = 0
        while (p + entry <= dyn.capacity()) {
            val tag: Long
            val value: Long
            if (is64) {
                tag = dyn.getLong(p)
                value = dyn.getLong(p + 8)
            } else {
                tag = dyn.getInt(p).toLong()
                value = dyn.getInt(p + 4).toLong() and 0xffffffffL
            }
            if (tag == DT_NULL) break
            if (tag == DT_NEEDED) needed.add(value)
            if (tag == DT_STRTAB) strtabVaddr = value
            p += entry
        }
        if (needed.isEmpty() || strtabVaddr < 0) return emptyList()

        val seg = loads.firstOrNull { strtabVaddr >= it.vaddr && strtabVaddr < it.vaddr + it.fileSize }
            ?: return emptyList()
        val strOff = seg.offset + (strtabVaddr - seg.vaddr)
        val fileLen = raf.length()
        if (strOff < 0 || strOff >= fileLen) return emptyList()
        val strLen = minOf(fileLen - strOff, MAX_STRTAB_BYTES.toLong()).toInt()
        val strs = ByteArray(strLen)
        raf.seek(strOff)
        raf.readFully(strs)

        val out = ArrayList<String>(needed.size)
        for (nameOff in needed) {
            if (nameOff < 0 || nameOff >= strLen) continue
            var end = nameOff.toInt()
            while (end < strLen && strs[end] != 0.toByte()) end++
            if (end > nameOff) out.add(String(strs, nameOff.toInt(), end - nameOff.toInt(), Charsets.UTF_8))
        }
        return out
    }

    fun loadOrder(root: File, searchDirs: List<File>, maxDepth: Int = 6): List<File> {
        val ordered = LinkedHashMap<String, File>()
        val seen = HashSet<String>()
        seen.add(canonical(root))

        fun visit(file: File, depth: Int) {
            if (depth >= maxDepth) return
            for (name in neededLibs(file)) {
                if (name.contains('/')) continue
                val dep = searchDirs.asSequence()
                    .map { File(it, name) }
                    .firstOrNull { it.isFile }
                    ?: continue
                val key = canonical(dep)
                if (!seen.add(key)) continue
                visit(dep, depth + 1)
                ordered[key] = dep
            }
        }
        visit(root, 0)
        return ordered.values.toList()
    }

    private fun canonical(f: File): String = try {
        f.canonicalPath
    } catch (_: Throwable) {
        f.absolutePath
    }
}

/**
 * Penyiap config & shader cache MobileGlues lewat jalur `MG_DIR_PATH`.
 */
object MobileGluesConfig {
    private const val TAG = "MobileGlues"

    const val ENV_DIR = "MG_DIR_PATH"
    const val LIBRARY_NAME = "libmobileglues.so"
    const val CACHE_MIB = 30

    private const val DIR_NAME = "mobileglues"
    private const val CONFIG_FILE = "config.json"
    private const val CACHE_FILE = "glsl_cache.tmp"
    private const val KEY_FILE = "cache-key.txt"

    fun isMobileGlues(libraryName: String?): Boolean =
        !libraryName.isNullOrBlank() && File(libraryName).name == LIBRARY_NAME

    fun prepare(context: Context, library: File, enableAngle: Boolean = false): File? {
        return try {
            val dir = File(context.filesDir, DIR_NAME)
            if (!dir.isDirectory && !dir.mkdirs()) {
                LauncherLog.w(TAG, "Folder config MobileGlues tidak bisa dibuat: $dir")
                return null
            }
            val config = File(dir, CONFIG_FILE)
            if (!isValidJson(config) && !writeDefaultConfig(dir, config, enableAngle)) return null
            syncAngleOption(dir, config, enableAngle)
            dropStaleCache(context, dir, library)
            dir
        } catch (e: Throwable) {
            LauncherLog.w(TAG, "Config MobileGlues gagal disiapkan: ${e.message}", e)
            null
        }
    }

    private fun isValidJson(file: File): Boolean {
        if (!file.isFile) return false
        return try {
            JSONObject(file.readText())
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun syncAngleOption(dir: File, config: File, enableAngle: Boolean) {
        try {
            val desired = if (enableAngle) 1 else 0
            val json = JSONObject(config.readText())
            if (json.optInt("enableANGLE", -1) != desired) {
                json.put("enableANGLE", desired)
                writeJsonAtomically(dir, config, json)
            }
        } catch (_: Throwable) {}
    }

    private fun writeDefaultConfig(dir: File, config: File, enableAngle: Boolean = false): Boolean {
        val json = JSONObject()
        json.put("enableANGLE", if (enableAngle) 1 else 0)
        json.put("enableNoError", 0)
        json.put("enableExtComputeShader", 0)
        json.put("enableExtTimerQuery", 1)
        json.put("enableExtDirectStateAccess", 1)
        json.put("maxGlslCacheSize", CACHE_MIB)
        json.put("angleDepthClearFixMode", 0)
        json.put("customGLVersion", 0)
        json.put("fsr1Setting", 0)
        json.put("hideMGEnvLevel", 0)
        return writeJsonAtomically(dir, config, json)
    }

    private fun writeJsonAtomically(dir: File, config: File, json: JSONObject): Boolean {
        val temp = File(dir, "$CONFIG_FILE.tmp")
        temp.writeText(json.toString(2))
        if (!temp.renameTo(config)) {
            if (config.exists()) config.delete()
            if (!temp.renameTo(config)) {
                temp.delete()
                LauncherLog.w(TAG, "config.json MobileGlues tidak bisa disimpan: $config")
                return false
            }
        }
        return isValidJson(config)
    }

    private fun dropStaleCache(context: Context, dir: File, library: File) {
        val key = if (library.isFile) {
            "${library.length()}-${library.lastModified()}"
        } else {
            "app-${appUpdateTime(context)}"
        }
        val keyFile = File(dir, KEY_FILE)
        val old = try {
            keyFile.readText().trim()
        } catch (_: Exception) {
            ""
        }
        if (old != key) {
            File(dir, CACHE_FILE).delete()
            File(dir, "$CACHE_FILE.new").delete()
            keyFile.writeText(key)
        }
    }

    @Suppress("DEPRECATION")
    private fun appUpdateTime(context: Context): Long = try {
        context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime
    } catch (_: Exception) {
        0L
    }
}

// ----------------------------------------------------------------------------
// Section: core/renderer/NuxRendererPluginManager.kt
// ----------------------------------------------------------------------------

object NuxRendererPluginManager {
    private const val TAG = "NuxRendererPlugin"

    /**
     * Plugin renderer yang dikenal. Hanya paket dengan nama persis ini yang boleh dikenali tanpa string
     * renderer lama di AndroidManifest (mis. MobileGL yang memakai fclPlugin_V2).
     */
    private val KNOWN_PLUGIN_PACKAGES = setOf(
        "top.mobilegl.plugin",
        "com.fcl.plugin.mobileglues",
        "com.bzlzhh.plugin.ngg",
        "com.bzlzhh.plugin.ngg.angleless"
    )

    private val pluginRenderers = mutableListOf<NuxRendererInfo>()

    fun getPluginRenderers(): List<NuxRendererInfo> = synchronized(pluginRenderers) {
        pluginRenderers.toList()
    }

    /** Waktu (ms) pemindaian terakhir selesai; 0 = belum pernah dipindai di proses ini. */
    @Volatile
    var lastScanMs: Long = 0L
        private set

    /** Pindai hanya bila belum pernah atau hasilnya sudah lebih tua dari [maxAgeMs]. */
    fun scanPluginsIfStale(context: Context, maxAgeMs: Long = 5 * 60_000L) {
        val age = System.currentTimeMillis() - lastScanMs
        if (lastScanMs == 0L || age > maxAgeMs) scanPlugins(context)
    }

    /**
     * Memindai plugin renderer dari aplikasi terpasang. Pekerjaan berat (membaca semua aplikasi),
     * JANGAN dipanggil dari thread utama: pakai Dispatchers.IO / thread latar.
     */
    @Synchronized
    fun scanPlugins(context: Context): List<NuxRendererInfo> {
        val detected = mutableListOf<NuxRendererInfo>()
        val detectedNative = mutableListOf<NativePluginManager.NativePlugin>()
        val pm = context.packageManager
        val processedPackages = mutableSetOf<String>()

        fun processAppInfo(info: ApplicationInfo) {
            val pkg = info.packageName
            if (pkg == context.packageName || !processedPackages.add(pkg)) return

            try {
                // Metadata sudah ikut terambil lewat GET_META_DATA: tidak perlu panggilan IPC lagi per aplikasi.
                val metaData = info.metaData
                val nativeLibDir = info.nativeLibraryDir

                // Sama seperti Zalith: plugin renderer adalah APK yang dipasang pengguna, dan harus
                // menandai dirinya lewat metadata. Nama paket atau isi folder native TIDAK dipakai
                // menebak, supaya launcher lain (Pojav, Zalith, dll) dan aplikasi biasa tidak ikut terdaftar.
                if ((info.flags and ApplicationInfo.FLAG_SYSTEM) != 0) return

                // Label (butuh membuka resource aplikasi, mahal) hanya dimuat bila benar-benar dipakai.
                val appLabel by lazy(LazyThreadSafetyMode.NONE) {
                    runCatching { info.loadLabel(pm).toString() }.getOrDefault(pkg)
                }

                // Plugin native library (untuk mod seperti Distant Horizons) dikenali di putaran yang sama:
                // tidak ada pemindaian atau panggilan IPC tambahan. Aplikasi tanpa metadata langsung lolos.
                // Tidak langsung return: satu APK boleh sekaligus plugin renderer.
                NativePluginManager.parse(info) { appLabel }?.let {
                    detectedNative.add(it)
                    Log.i(TAG, "Found native library plugin: ${it.displayName} ($pkg)")
                }

                val hasLegacyPlugin = metaData != null && (
                    metaData.getBoolean("fclPlugin", false) ||
                    metaData.getBoolean("zalithRendererPlugin", false)
                ) && !metaData.getString("renderer").isNullOrBlank()
                if (!hasLegacyPlugin && pkg !in KNOWN_PLUGIN_PACKAGES) return

                // 1. Metadata plugin FCL / Zalith (penanda fclPlugin atau zalithRendererPlugin + string renderer)
                if (metaData != null && hasLegacyPlugin) {
                    val rendererString = metaData.getString("renderer") ?: ""
                    val des = metaData.getString("des") ?: appLabel
                    val pojavEnvString = metaData.getString("pojavEnv") ?: ""

                    val parts = rendererString.split(":")
                    var rendererId = if (parts.isNotEmpty() && parts[0].isNotBlank()) parts[0] else "opengles3"
                    val glName = if (parts.size > 1 && parts[1].isNotBlank()) parts[1] else "libgl4es.so"
                    val rawEglName = if (parts.size > 2 && parts[2].isNotBlank()) parts[2] else null
                    val eglName = if (rawEglName != null && rawEglName.startsWith("/")) "$nativeLibDir$rawEglName" else rawEglName

                    val envMap = mutableMapOf<String, String>()
                    val dlopenList = mutableListOf<String>()

                    if (pojavEnvString.isNotBlank()) {
                        pojavEnvString.split(":").forEach { envPair ->
                            if (envPair.contains("=")) {
                                val split = envPair.split("=", limit = 2)
                                val k = split[0].trim()
                                val v = split[1].trim()
                                when (k) {
                                    "POJAV_RENDERER" -> {
                                        rendererId = v
                                    }
                                    "DLOPEN" -> {
                                        v.split(",").forEach { dl ->
                                            if (dl.isNotBlank()) dlopenList.add(dl.trim())
                                        }
                                    }
                                    "LIB_MESA_NAME", "MESA_LIBRARY" -> {
                                        envMap[k] = "$nativeLibDir/$v"
                                    }
                                    else -> envMap[k] = v
                                }
                            }
                        }
                    }

                    // MobileGlues, MobileGL, and OpenGL ES based plugins must map POJAV_RENDERER to opengles3 for libpojavexec bridge
                    if (rendererId.equals("MobileGlues", ignoreCase = true) ||
                        rendererId.equals("MobileGL", ignoreCase = true) ||
                        (!rendererId.startsWith("opengles") && !rendererId.startsWith("vulkan") && !rendererId.startsWith("gallium") && rendererId != "custom_gallium")) {
                        rendererId = "opengles3"
                    }

                    val minVer = metaData.getString("minMCVer")
                    val maxVer = metaData.getString("maxMCVer")

                    val pluginInfo = NuxRendererInfo(
                        id = "plugin_$pkg",
                        displayName = des,
                        badge = "Plugin: $appLabel",
                        summary = if (pkg == "top.mobilegl.plugin" || des.equals("MobileGL", ignoreCase = true)) 
                            "Pustaka grafis MobileGL (Vulkan & GLES backend)" 
                        else "Renderer eksternal dari APK: $appLabel ($pkg)",
                        compatibility = "Minecraft (APK Plugin)",
                        rendererId = rendererId,
                        libraryName = glName,
                        eglName = eglName,
                        envVariables = envMap,
                        isPlugin = true,
                        pluginPackageName = pkg,
                        pluginNativePath = nativeLibDir,
                        dlopenLibs = dlopenList,
                        minMCVersion = minVer,
                        maxMCVersion = maxVer
                    )
                    detected.add(pluginInfo)
                    Log.i(TAG, "Found renderer plugin (metadata): ${pluginInfo.displayName} ($pkg)")
                    return
                }

                // 2. Paket plugin yang dikenal tetapi tanpa string renderer lama (mis. MobileGL V2):
                // ambil pustaka grafis dari folder native-nya. Folder harus berisi pustaka (.so).
                val nativeDirFile = File(nativeLibDir)
                if (nativeDirFile.exists() && nativeDirFile.isDirectory) {
                    val soFiles = nativeDirFile.listFiles { f -> f.extension == "so" } ?: emptyArray()

                    if (soFiles.isNotEmpty()) {
                        val glName = soFiles.firstOrNull { it.name.startsWith("libmobilegl", ignoreCase = true) || it.name.startsWith("libgl") }?.name ?: "libgl4es.so"
                        val eglName = soFiles.firstOrNull { it.name.contains("egl", ignoreCase = true) }?.name
                        val dlopenList = soFiles.map { it.name }

                        val pluginInfo = NuxRendererInfo(
                            id = "plugin_$pkg",
                            displayName = appLabel,
                            badge = "Plugin Eksternal",
                            summary = "Renderer APK: $appLabel ($pkg)",
                            compatibility = "Minecraft (APK Plugin)",
                            rendererId = "opengles3",
                            libraryName = glName,
                            eglName = eglName,
                            envVariables = mapOf(
                                "POJAVEXEC_EGL" to (eglName ?: glName),
                                "LIBGL_EGL" to (eglName ?: glName)
                            ),
                            isPlugin = true,
                            pluginPackageName = pkg,
                            pluginNativePath = nativeLibDir,
                            dlopenLibs = dlopenList
                        )
                        detected.add(pluginInfo)
                        Log.i(TAG, "Found renderer plugin (heuristic): ${pluginInfo.displayName} ($pkg)")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error checking package $pkg for renderer plugin", e)
            }
        }

        // Satu kali mengambil daftar aplikasi terpasang; dipakai juga oleh pemindai V2.
        // (Cara queryIntentActivities dibuang karena hasilnya hanya bagian dari daftar ini.)
        var installed: List<ApplicationInfo> = emptyList()
        try {
            installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (appInfo in installed) {
                processAppInfo(appInfo)
            }
        } catch (e: Exception) {
            Log.w(TAG, "getInstalledApplications failed", e)
        }

        synchronized(pluginRenderers) {
            pluginRenderers.clear()
            pluginRenderers.addAll(detected)
        }
        NuxRendererRegistry.setPluginRenderers(detected)
        NativePluginManager.setPlugins(detectedNative)
        try {
            com.aistudio.novacraft.launcher.core.launch.NuxRendererV2Manager.scanV2Plugins(context, installed)
        } catch (e: Exception) {
            Log.w(TAG, "scanV2Plugins failed", e)
        }
        lastScanMs = System.currentTimeMillis()
        return detected
    }
}

// ----------------------------------------------------------------------------
// Section: core/renderer/NuxRendererRegistry.kt
// ----------------------------------------------------------------------------

object NuxRendererRegistry {

    val RENDERER_AUTO = NuxRendererInfo(
        id = "auto",
        displayName = "Auto (Rekomendasi)",
        badge = "Otomatis Cerdas",
        summary = "Otomatis memilih renderer paling optimal & stabil berdasarkan versi Minecraft yang dijalankan.",
        compatibility = "Semua Versi Minecraft",
        rendererId = "auto",
        libraryName = "auto"
    )

    val RENDERER_KRYPTON = NuxRendererInfo(
        id = "krypton",
        displayName = "Krypton (Next-Gen GL4ES)",
        badge = "Rekomendasi Utama (Mali & Adreno)",
        summary = "Wrapper modern OpenGL ES 3.x langsung di atas driver resmi sistem. Paling stabil dan FPS maksimal untuk GPU Mali & Adreno di MC modern (1.17 – 26.3).",
        compatibility = "Minecraft 1.17 – 26.3-snapshot-4",
        rendererId = "opengles3",
        libraryName = "libng_gl4es.so",
        isMaliRecommended = true,
        isAdrenoRecommended = true,
        minMCVersion = "1.17",
        maxMCVersion = "26.3-snapshot-4",
        envVariables = mapOf(
            "LIBGL_USE_MC_COLOR" to "1",
            "LIBGL_GL" to "31",
            "LIBGL_ES" to "3",
            "LIBGL_NORMALIZE" to "1",
            "LIBGL_NOERROR" to "1"
        )
    )

    val RENDERER_MOBILEGLUES = NuxRendererInfo(
        id = "mobileglues",
        displayName = "MobileGlues (OpenGL 4.0)",
        badge = "FCL Compatibility Layer",
        summary = "Pustaka grafis OpenGL 4.0 MobileGlues untuk Minecraft 1.17 – 1.21.1. Kompatibilitas tinggi dengan berbagai GPU mobile termasuk Mali.",
        compatibility = "Minecraft 1.17 – 1.21.1",
        rendererId = "opengles3",
        libraryName = "libmobileglues.so",
        eglName = "libmobileglues.so",
        isMaliRecommended = true,
        minMCVersion = "1.17",
        maxMCVersion = "1.21.1",
        envVariables = mapOf(
            "LIBGL_ES" to "3",
            "POJAVEXEC_EGL" to "libmobileglues.so",
            "LIBGL_EGL" to "libmobileglues.so",
            "MG_COUNT_LAUNCH" to "1",
            "SDL_OPENGL_FORCE_SRGB_FRAMEBUFFER" to "0",
            "allow_higher_compat_version" to "true",
            "allow_glsl_extension_directive_midshader" to "true",
            "force_glsl_extensions_warn" to "true"
        )
    )

    val RENDERER_GL4ES = NuxRendererInfo(
        id = "gl4es",
        displayName = "GL4ES 1.1.4",
        badge = "Paling Stabil untuk MC Lama",
        summary = "Wrapper OpenGL ES 2.0 klasik. Kompatibel untuk Minecraft 1.7.10 – 1.16.5 dan perangkat spek rendah.",
        compatibility = "Minecraft 1.7.10 – 1.16.5",
        rendererId = "opengles2",
        libraryName = "libgl4es_114.so",
        minMCVersion = "1.7.2",
        maxMCVersion = "1.16.5",
        envVariables = mapOf(
            "LIBGL_ES" to "2",
            "LIBGL_MIPMAP" to "3",
            "LIBGL_NOERROR" to "1",
            "LIBGL_NOINTOVLHACK" to "1",
            "LIBGL_NORMALIZE" to "1"
        )
    )

    val RENDERER_ZINK = NuxRendererInfo(
        id = "zink",
        displayName = "Kopper Zink (Desktop GL 4.6)",
        badge = "Hardware Vulkan",
        summary = "Menerjemahkan OpenGL desktop 4.6 ke Vulkan. Mendukung mod shader modern (Iris/Oculus) & mod tingkat lanjut.",
        compatibility = "Minecraft 1.14 – 26.3",
        rendererId = "opengles3_desktopgl_zink_kopper",
        libraryName = "libglxshim.so",
        eglName = "libEGL_mesa.so",
        requiresVulkan = true,
        minMCVersion = "1.14",
        maxMCVersion = "26.3",
        envVariables = mapOf(
            "LIBGL_ES" to "3",
            "MESA_LOADER_DRIVER_OVERRIDE" to "zink",
            "MESA_GL_VERSION_OVERRIDE" to "4.6",
            "MESA_GLSL_VERSION_OVERRIDE" to "460",
            "force_glsl_extensions_warn" to "true",
            "allow_higher_compat_version" to "true",
            "allow_glsl_extension_directive_midshader" to "true",
            "LIB_MESA_NAME" to "libglxshim.so"
        )
    )

    val RENDERER_FREEDRENO = NuxRendererInfo(
        id = "freedreno",
        displayName = "Freedreno (Snapdragon Turnip)",
        badge = "Khusus GPU Adreno",
        summary = "Driver Mesa Gallium Freedreno open-source dioptimalkan khusus untuk chipset Snapdragon / GPU Adreno.",
        compatibility = "Minecraft 1.7.10 – 26.3",
        rendererId = "gallium_freedreno",
        libraryName = "libOSMesa_8.so",
        isAdrenoRecommended = true,
        minMCVersion = "1.7.2",
        maxMCVersion = "26.3",
        envVariables = mapOf(
            "MESA_LOADER_DRIVER_OVERRIDE" to "freedreno",
            "MESA_GL_VERSION_OVERRIDE" to "4.6",
            "MESA_GLSL_VERSION_OVERRIDE" to "460",
            "force_glsl_extensions_warn" to "true",
            "allow_higher_compat_version" to "true",
            "allow_glsl_extension_directive_midshader" to "true",
            "LIB_MESA_NAME" to "libOSMesa_8.so"
        )
    )

    private val baseRenderers: List<NuxRendererInfo> = listOf(
        RENDERER_AUTO,
        RENDERER_KRYPTON,
        RENDERER_MOBILEGLUES,
        RENDERER_GL4ES,
        RENDERER_ZINK,
        RENDERER_FREEDRENO
    )

    private val pluginRenderers = mutableListOf<NuxRendererInfo>()

    fun setPluginRenderers(plugins: List<NuxRendererInfo>) {
        synchronized(pluginRenderers) {
            pluginRenderers.clear()
            pluginRenderers.addAll(plugins)
        }
    }

    /**
     * Seluruh daftar renderer yang dapat dipilih oleh user di Settings (bawaan + plugin eksternal).
     */
    val availableRenderers: List<NuxRendererInfo>
        get() = synchronized(pluginRenderers) {
            baseRenderers + pluginRenderers
        }

    /**
     * Menemukan renderer berdasarkan ID
     */
    fun findRendererById(id: String): NuxRendererInfo {
        return availableRenderers.find { 
            it.id.equals(id, ignoreCase = true) || 
            (it.isPlugin && it.pluginPackageName.equals(id.removePrefix("plugin_"), ignoreCase = true)) ||
            (id.equals("mobilegl", ignoreCase = true) && it.displayName.equals("MobileGL", ignoreCase = true))
        } ?: RENDERER_AUTO
    }

    /**
     * Memeriksa apakah renderer yang dipilih kompatibel dengan versi Minecraft yang akan dimainkan.
     */
    fun isSupportedForVersion(renderer: NuxRendererInfo, mcVersion: String): Boolean {
        return NuxVersionUtils.isRendererSupported(renderer, mcVersion)
    }

    /**
     * Menyelesaikan konfigurasi renderer aktual yang akan dijalankan di game engine.
     * Jika "auto" dipilih, akan memilih Krypton untuk MC 1.17+ atau GL4ES untuk versi di bawahnya.
     */
    fun resolveRenderer(
        selectedId: String,
        mcVersion: String,
        nativeLibDir: File,
        context: android.content.Context? = null
    ): NuxRendererInfo {
        if (context != null) {
            // Hanya memindai bila belum pernah (bukan tiap kali daftar plugin kebetulan kosong).
            NuxRendererPluginManager.scanPluginsIfStale(context)
        }

        val hasNgGl4es = File(nativeLibDir, "libng_gl4es.so").exists()
        val hasMobileGlues = File(nativeLibDir, "libmobileglues.so").exists()
        val hasGl4es = File(nativeLibDir, "libgl4es_114.so").exists()

        // 1. Direct match in availableRenderers (built-in or scanned plugins)
        val requested = findRendererById(selectedId)
        if (requested.isPlugin) {
            return requested
        }

        // 2. If user selected by plugin ID pattern or package name
        if (selectedId.startsWith("plugin_") || selectedId.contains("com.fcl.plugin") || selectedId.contains("plugin") || selectedId == "top.mobilegl.plugin") {
            val pkg = selectedId.removePrefix("plugin_")
            val matchedPlugin = pluginRenderers.find {
                it.id.equals(selectedId, ignoreCase = true) ||
                it.pluginPackageName.equals(pkg, ignoreCase = true) ||
                it.pluginPackageName?.contains(pkg, ignoreCase = true) == true
            }
            if (matchedPlugin != null) {
                return matchedPlugin
            }
        }

        // 3. Match MobileGL specifically (berbeda dengan MobileGlues!)
        if (selectedId.contains("mobilegl", ignoreCase = true) && !selectedId.contains("mobileglue", ignoreCase = true)) {
            val pluginMobileGL = pluginRenderers.find {
                it.id.equals(selectedId, ignoreCase = true) ||
                it.displayName.equals("MobileGL", ignoreCase = true) ||
                it.pluginPackageName == "top.mobilegl.plugin"
            }
            if (pluginMobileGL != null) {
                return pluginMobileGL
            }
        }

        // 4. If selectedId is "mobileglues" or mentions mobileglue
        if (selectedId.contains("mobileglue", ignoreCase = true)) {
            if (hasMobileGlues) {
                return RENDERER_MOBILEGLUES
            }
            // Auto-fallback to external MobileGlues plugin if installed
            val pluginMg = pluginRenderers.find {
                it.id.contains("mobileglue", ignoreCase = true) ||
                it.displayName.contains("mobileglue", ignoreCase = true) ||
                it.libraryName.contains("mobileglue", ignoreCase = true) ||
                it.pluginPackageName?.contains("mobileglue", ignoreCase = true) == true
            }
            if (pluginMg != null) {
                return pluginMg
            }
        }

        // 4. Auto mode
        if (selectedId.equals("auto", ignoreCase = true)) {
            val isModernMc = isModernMinecraft(mcVersion)
            return if (isModernMc && hasNgGl4es) {
                RENDERER_KRYPTON
            } else if (isModernMc && (hasMobileGlues || pluginRenderers.any { it.libraryName.contains("mobileglue", ignoreCase = true) })) {
                pluginRenderers.find { it.libraryName.contains("mobileglue", ignoreCase = true) } ?: RENDERER_MOBILEGLUES
            } else if (hasGl4es) {
                RENDERER_GL4ES
            } else if (hasNgGl4es) {
                RENDERER_KRYPTON
            } else {
                RENDERER_GL4ES
            }
        }

        // 5. If requested target .so exists in nativeLibDir
        val targetSo = File(nativeLibDir, requested.libraryName)
        if (targetSo.exists()) {
            return requested
        }

        // 6. Fallback aman jika library target tidak ada di nativeLibDir
        return if (hasNgGl4es) RENDERER_KRYPTON else if (hasMobileGlues) RENDERER_MOBILEGLUES else RENDERER_GL4ES
    }

    /**
     * Deteksi apakah versi Minecraft adalah 1.17 ke atas
     */
    fun isModernMinecraft(version: String): Boolean {
        if (version.isBlank()) return true
        val clean = version.trim().lowercase()
        // Cek snapshot modern (24w..., 25w..., 26w...)
        if (clean.matches(Regex("""^\d{2}w\d{2}.*"""))) return true

        // Ekstrak angka versi mayor/minor dari 1.X.Y
        val match = Regex("""^1\.(\d+)""").find(clean)
        if (match != null) {
            val minor = match.groupValues[1].toIntOrNull() ?: 17
            return minor >= 17
        }
        return true
    }
}

// ----------------------------------------------------------------------------
// Section: core/news/MCNewsService.kt
// ----------------------------------------------------------------------------

enum class NewsKind {
    RELEASE,
    SNAPSHOT,
    NEWS
}

data class MCNewsItem(
    val id: String,
    val title: String,
    val version: String,
    val category: String,
    val kind: NewsKind,
    val isoDate: String,
    val displayDate: String,
    val summary: String,
    val imageUrl: String = "",
    val contentPath: String = "",
    val readMoreUrl: String = "",
    val manifestSha1: String = ""
)

data class MCNewsFeed(
    val latestRelease: VersionItem?,
    val latestSnapshot: VersionItem?,
    val items: List<MCNewsItem>,
    val fetchedAtMillis: Long,
    val isCached: Boolean = false
)

data class PatchNoteLink(
    val label: String,
    val url: String
)

sealed interface PatchNoteBlock {
    data class Heading(val level: Int, val text: String) : PatchNoteBlock
    data class Paragraph(val text: String) : PatchNoteBlock
    data class Bullet(val text: String) : PatchNoteBlock
    data class Quote(val text: String) : PatchNoteBlock
    data class Image(val url: String, val alt: String = "") : PatchNoteBlock
    object Divider : PatchNoteBlock
}

data class PatchNoteDetail(
    val title: String,
    val version: String,
    val type: String,
    val imageUrl: String,
    val blocks: List<PatchNoteBlock>,
    val links: List<PatchNoteLink> = emptyList()
)

/**
 * Layanan berita dan informasi versi Minecraft secara waktu nyata langsung dari server resmi Mojang:
 * - Manifest versi resmi: piston-meta.mojang.com/mc/game/version_manifest_v2.json
 * - Catatan rilis & snapshot Java Edition: launchercontent.mojang.com/v2/javaPatchNotes.json
 * - Berita resmi Minecraft Launcher: launchercontent.mojang.com/v2/news.json
 *
 * Dilengkapi cache memori untuk respons cepat dan ketahanan saat koneksi bermasalah.
 */
object MCNewsService {
    private const val CONTENT_BASE_URL = "https://launchercontent.mojang.com"
    private const val PATCH_NOTES_URL = "$CONTENT_BASE_URL/v2/javaPatchNotes.json"
    private const val NEWS_URL = "$CONTENT_BASE_URL/v2/news.json"
    private const val RELEASE_CHANGELOGS_URL =
        "https://feedback.minecraft.net/hc/en-us/sections/360001186971-Release-Changelogs"
    private const val SNAPSHOT_CHANGELOGS_URL =
        "https://feedback.minecraft.net/hc/en-us/sections/360001185332-Beta-and-Preview-Information-and-Changelogs"
    private const val FEED_CACHE_TTL_MS = 2L * 60L * 1000L

    private val client = OkHttpClient.Builder()
        .dns(NuxDns)
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val gson = Gson()

    @Volatile
    private var cachedFeed: MCNewsFeed? = null

    private val detailCache = object : LinkedHashMap<String, PatchNoteDetail>(24, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, PatchNoteDetail>?): Boolean =
            size > 24
    }

    fun getCachedFeed(): MCNewsFeed? = cachedFeed

    fun findManifestVersion(versionId: String): VersionItem? {
        val v = versionId.trim()
        if (v.isEmpty()) return null
        return MojangManifestService.liveManifest.value?.versions?.firstOrNull {
            it.id.equals(v, ignoreCase = true)
        }
    }

    fun findItemForVersion(feed: MCNewsFeed, versionId: String): MCNewsItem? {
        val v = versionId.trim()
        if (v.isEmpty()) return null
        feed.items.firstOrNull {
            (it.kind == NewsKind.RELEASE || it.kind == NewsKind.SNAPSHOT) &&
                it.version.equals(v, ignoreCase = true)
        }?.let { return it }

        feed.items.firstOrNull {
            it.version.equals(v, ignoreCase = true)
        }?.let { return it }

        feed.items.firstOrNull {
            (it.kind == NewsKind.RELEASE || it.kind == NewsKind.SNAPSHOT) &&
                (it.version.contains(v, ignoreCase = true) || it.title.contains(v, ignoreCase = true))
        }?.let { return it }

        val manifestItem = findManifestVersion(v)
            ?: feed.latestRelease?.takeIf { it.id.equals(v, ignoreCase = true) }
            ?: feed.latestSnapshot?.takeIf { it.id.equals(v, ignoreCase = true) }
        return manifestItem?.let { buildManifestNewsItem(it) }
    }

    suspend fun fetchFeed(forceRefresh: Boolean = true): Result<MCNewsFeed> = withContext(Dispatchers.IO) {
        val currentCached = cachedFeed
        if (!forceRefresh && currentCached != null &&
            (System.currentTimeMillis() - currentCached.fetchedAtMillis) < FEED_CACHE_TTL_MS
        ) {
            return@withContext Result.success(currentCached.copy(isCached = false))
        }

        try {
            coroutineScope {
                val manifestDeferred = async {
                    MojangManifestService.getVersionManifest(forceRefresh = forceRefresh).getOrNull()
                }
                val patchNotesDeferred = async {
                    fetchJson(PATCH_NOTES_URL, forceRefresh)
                }
                val newsDeferred = async {
                    fetchJson(NEWS_URL, forceRefresh)
                }

                val manifest = manifestDeferred.await()
                val patchNotesJson = patchNotesDeferred.await()
                val newsJson = newsDeferred.await()

                if (manifest == null && patchNotesJson == null && newsJson == null) {
                    return@coroutineScope Result.failure(
                        IOException(com.aistudio.novacraft.launcher.ui.theme.Tr.t("Tidak dapat terhubung ke server resmi Mojang."))
                    )
                }

                val latestReleaseItem = manifest?.let { m ->
                    m.versions.firstOrNull { it.id == m.latest.release }
                }
                val latestSnapshotItem = manifest?.let { m ->
                    m.versions.firstOrNull { it.id == m.latest.snapshot }
                }
                val manifestById = manifest?.versions?.associateBy { it.id }.orEmpty()

                val combined = ArrayList<MCNewsItem>(110)
                val seenPatchVersions = HashSet<String>()

                if (patchNotesJson != null) {
                    val entries = patchNotesJson.getAsJsonArray("entries")
                    if (entries != null) {
                        val limit = minOf(entries.size(), 60)
                        for (i in 0 until limit) {
                            val obj = entries[i].asJsonObjectOrNull() ?: continue
                            val title = obj.str("title")
                            val version = obj.str("version").trim()
                            val type = obj.str("type").lowercase(Locale.ROOT)
                            val isoDate = obj.str("date")
                            val rawShort = obj.str("shortText").replace("\r\n", "\n").trim()
                            val shortText = if (rawShort.contains('<') && rawShort.contains('>')) {
                                stripHtml(rawShort)
                            } else {
                                rawShort
                            }
                            val contentPath = obj.str("contentPath")
                            val imageUrl = obj.getAsJsonObjectOrNull("image")?.str("url").let(::resolveContentUrl)
                            val id = obj.str("id").ifBlank { "patch_${version}_$i" }
                            if (title.isBlank()) continue

                            if (version.isNotBlank()) seenPatchVersions.add(version)
                            val kind = if (type == "release") NewsKind.RELEASE else NewsKind.SNAPSHOT
                            val manifestEntry = manifestById[version]

                            combined.add(
                                MCNewsItem(
                                    id = "patch_$id",
                                    title = title,
                                    version = version,
                                    category = if (kind == NewsKind.RELEASE) "Java Release" else "Java Snapshot",
                                    kind = kind,
                                    isoDate = isoDate,
                                    displayDate = formatIsoDate(isoDate),
                                    summary = shortText,
                                    imageUrl = imageUrl,
                                    contentPath = contentPath,
                                    readMoreUrl = if (kind == NewsKind.RELEASE) RELEASE_CHANGELOGS_URL else SNAPSHOT_CHANGELOGS_URL,
                                    manifestSha1 = manifestEntry?.sha1.orEmpty()
                                )
                            )
                        }
                    }
                }

                // Jika manifest Mojang sudah memiliki rilis/snapshot lebih baru daripada feed patch notes,
                // tambahkan langsung dari manifest resmi agar informasi versi selalu seketika waktu nyata.
                if (manifest != null) {
                    appendMissingManifestVersion(manifest, latestReleaseItem, NewsKind.RELEASE, seenPatchVersions, combined)
                    appendMissingManifestVersion(manifest, latestSnapshotItem, NewsKind.SNAPSHOT, seenPatchVersions, combined)
                }

                if (newsJson != null) {
                    val entries = newsJson.getAsJsonArray("entries")
                    if (entries != null) {
                        val limit = minOf(entries.size(), 40)
                        for (i in 0 until limit) {
                            val obj = entries[i].asJsonObjectOrNull() ?: continue
                            val title = obj.str("title")
                            if (title.isBlank()) continue
                            val category = obj.str("category").ifBlank { "Minecraft" }
                            val isoDate = obj.str("date")
                            val rawText = obj.str("text").replace("\r\n", "\n").trim()
                            val text = if (rawText.contains('<') && rawText.contains('>')) stripHtml(rawText) else rawText
                            val readMoreLink = obj.str("readMoreLink")
                            val imageUrl = (
                                obj.getAsJsonObjectOrNull("newsPageImage")?.str("url")
                                    ?: obj.getAsJsonObjectOrNull("playPageImage")?.str("url")
                                    ?: ""
                                ).let(::resolveContentUrl)
                            val id = obj.str("id").ifBlank { "news_${title.hashCode()}_$i" }

                            combined.add(
                                MCNewsItem(
                                    id = "news_$id",
                                    title = title,
                                    version = "",
                                    category = category,
                                    kind = NewsKind.NEWS,
                                    isoDate = isoDate,
                                    displayDate = formatIsoDate(isoDate),
                                    summary = text,
                                    imageUrl = imageUrl,
                                    contentPath = "",
                                    readMoreUrl = readMoreLink
                                )
                            )
                        }
                    }
                }

                val sorted = combined.sortedByDescending { parseIsoMillis(it.isoDate) }
                val feed = MCNewsFeed(
                    latestRelease = latestReleaseItem,
                    latestSnapshot = latestSnapshotItem,
                    items = sorted,
                    fetchedAtMillis = System.currentTimeMillis(),
                    isCached = false
                )
                cachedFeed = feed
                Result.success(feed)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun fetchPatchNoteDetail(
        contentPath: String,
        forceRefresh: Boolean = false
    ): Result<PatchNoteDetail> = withContext(Dispatchers.IO) {
        val url = resolveDetailUrl(contentPath)
        if (!forceRefresh) {
            synchronized(detailCache) { detailCache[url] }?.let { return@withContext Result.success(it) }
        }
        try {
            val json = fetchJson(url, forceRefresh = forceRefresh)
                ?: return@withContext Result.failure(
                    IOException(com.aistudio.novacraft.launcher.ui.theme.Tr.t("Gagal memuat detail catatan rilis."))
                )
            val title = json.str("title")
            val version = json.str("version")
            val type = json.str("type")
            val imageUrl = json.getAsJsonObjectOrNull("image")?.str("url").let(::resolveContentUrl)
            val bodyHtml = json.str("body")
            val detail = PatchNoteDetail(
                title = title,
                version = version,
                type = type,
                imageUrl = imageUrl,
                blocks = parsePatchHtml(bodyHtml),
                links = extractLinks(bodyHtml)
            )
            synchronized(detailCache) {
                detailCache[url] = detail
            }
            Result.success(detail)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    fun toPlainText(item: MCNewsItem, detail: PatchNoteDetail?): String {
        val sb = StringBuilder()
        val heading = detail?.title?.ifBlank { item.title } ?: item.title
        sb.append(heading)
        if (item.version.isNotBlank()) sb.append(" (${item.version})")
        if (item.displayDate.isNotBlank()) sb.append(" - ${item.displayDate}")
        sb.append("\n\n")
        val blocks = detail?.blocks.orEmpty()
        if (blocks.isNotEmpty()) {
            for (block in blocks) {
                when (block) {
                    is PatchNoteBlock.Heading -> sb.append("## ").append(block.text).append('\n')
                    is PatchNoteBlock.Paragraph -> sb.append(block.text).append("\n\n")
                    is PatchNoteBlock.Bullet -> sb.append("• ").append(block.text).append('\n')
                    is PatchNoteBlock.Quote -> sb.append("> ").append(block.text).append("\n\n")
                    is PatchNoteBlock.Image -> if (block.alt.isNotBlank()) sb.append("[${block.alt}]\n")
                    PatchNoteBlock.Divider -> sb.append("---\n")
                }
            }
        } else if (item.summary.isNotBlank()) {
            sb.append(item.summary)
        }
        return sb.toString().trim()
    }

    private fun resolveDetailUrl(contentPath: String): String {
        return if (contentPath.startsWith("https://") || contentPath.startsWith("http://")) {
            contentPath
        } else {
            val trimmed = contentPath.trimStart('/')
            if (trimmed.startsWith("v2/")) "$CONTENT_BASE_URL/$trimmed" else "$CONTENT_BASE_URL/v2/$trimmed"
        }
    }

    private fun buildManifestNewsItem(item: VersionItem): MCNewsItem {
        val isRelease = item.type.equals("release", ignoreCase = true)
        val kind = if (isRelease) NewsKind.RELEASE else NewsKind.SNAPSHOT
        val summaryTemplate = if (isRelease) {
            "Rilis resmi terbaru di manifest Mojang ({0})."
        } else {
            "Snapshot pengembangan terbaru di manifest Mojang ({0})."
        }
        return MCNewsItem(
            id = "manifest_${item.id}",
            title = if (isRelease) "Minecraft Java ${item.id}" else "Minecraft ${item.id}",
            version = item.id,
            category = when {
                isRelease -> "Java Release"
                item.type.equals("snapshot", ignoreCase = true) -> "Java Snapshot"
                else -> item.type.replace('_', ' ').uppercase(Locale.ROOT)
            },
            kind = kind,
            isoDate = item.releaseTime,
            displayDate = formatIsoDate(item.releaseTime),
            summary = com.aistudio.novacraft.launcher.ui.theme.Tr.t(summaryTemplate).replace("{0}", item.id),
            imageUrl = "",
            contentPath = "",
            readMoreUrl = if (isRelease) RELEASE_CHANGELOGS_URL else SNAPSHOT_CHANGELOGS_URL,
            manifestSha1 = item.sha1.orEmpty()
        )
    }

    private fun appendMissingManifestVersion(
        manifest: VersionManifest,
        item: VersionItem?,
        kind: NewsKind,
        seenPatchVersions: MutableSet<String>,
        out: MutableList<MCNewsItem>
    ) {
        if (item == null || item.id in seenPatchVersions) return
        seenPatchVersions.add(item.id)
        val isRelease = kind == NewsKind.RELEASE
        val summaryTemplate = if (isRelease) {
            "Rilis resmi terbaru di manifest Mojang ({0})."
        } else {
            "Snapshot pengembangan terbaru di manifest Mojang ({0})."
        }
        val latestVersion = if (isRelease) manifest.latest.release else manifest.latest.snapshot
        out.add(
            MCNewsItem(
                id = "manifest_${item.id}",
                title = if (isRelease) "Minecraft Java ${item.id}" else "Minecraft ${item.id}",
                version = item.id,
                category = if (isRelease) "Java Release" else "Java Snapshot",
                kind = kind,
                isoDate = item.releaseTime,
                displayDate = formatIsoDate(item.releaseTime),
                summary = com.aistudio.novacraft.launcher.ui.theme.Tr.t(summaryTemplate).replace("{0}", latestVersion),
                imageUrl = "",
                contentPath = "",
                readMoreUrl = if (isRelease) RELEASE_CHANGELOGS_URL else SNAPSHOT_CHANGELOGS_URL,
                manifestSha1 = item.sha1.orEmpty()
            )
        )
    }

    private fun fetchJson(url: String, forceRefresh: Boolean): JsonObject? {
        return try {
            val builder = Request.Builder()
                .url(url)
                .header("User-Agent", "NovaCraftLauncher/2.3 (Android)")
            if (forceRefresh) {
                builder
                    .cacheControl(CacheControl.FORCE_NETWORK)
                    .header("Cache-Control", "no-cache")
            }
            client.newCall(builder.build()).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                gson.fromJson(body, JsonObject::class.java)
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun resolveContentUrl(path: String?): String {
        val raw = path?.trim().orEmpty()
        if (raw.isEmpty()) return ""
        return when {
            raw.startsWith("https://") -> raw
            raw.startsWith("http://") -> "https://" + raw.removePrefix("http://")
            raw.startsWith("//") -> "https:$raw"
            raw.startsWith("/") -> "$CONTENT_BASE_URL$raw"
            else -> "$CONTENT_BASE_URL/$raw"
        }
    }

    fun formatIsoDate(iso: String): String {
        val millis = parseIsoMillis(iso)
        if (millis <= 0L) return iso
        val hasTime = iso.contains('T')
        val pattern = if (hasTime) "dd MMM yyyy · HH:mm" else "dd MMM yyyy"
        return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))
    }

    fun formatClock(millis: Long): String {
        return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(millis))
    }

    private fun parseIsoMillis(iso: String): Long {
        if (iso.isBlank()) return 0L
        // Normalisasi pecahan detik >3 digit & offset +00:00 agar kompatibel lintas versi Android
        var cleaned = iso.trim()
        cleaned = cleaned.replace(Regex("(\\.\\d{3})\\d+"), "$1")
        if (cleaned.endsWith("+00:00")) {
            cleaned = cleaned.removeSuffix("+00:00") + "Z"
        }
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd"
        )
        for (pattern in formats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                if (pattern.endsWith("'Z'")) {
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                }
                val date = sdf.parse(cleaned)
                if (date != null) return date.time
            } catch (_: Throwable) {
            }
        }
        return 0L
    }

    private val IMG_REGEX = Regex("<img\\b[^>]*>", RegexOption.IGNORE_CASE)
    private val SRC_ATTR_REGEX = Regex("\\bsrc\\s*=\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
    private val ALT_ATTR_REGEX = Regex("\\balt\\s*=\\s*[\"']([^\"']*)[\"']", RegexOption.IGNORE_CASE)
    private val LINK_REGEX = Regex("<a\\b[^>]*\\bhref\\s*=\\s*[\"'](https?://[^\"']+)[\"'][^>]*>(.*?)</a>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))

    private fun extractLinks(html: String): List<PatchNoteLink> {
        if (html.isBlank()) return emptyList()
        val seen = HashSet<String>()
        val out = ArrayList<PatchNoteLink>()
        for (match in LINK_REGEX.findAll(html)) {
            val url = match.groupValues[1].trim()
            if (url.isBlank() || !seen.add(url)) continue
            val label = stripHtml(match.groupValues[2]).ifBlank {
                url.removePrefix("https://").removePrefix("http://").substringBefore('/')
            }
            out.add(PatchNoteLink(label = label.take(48), url = url))
            if (out.size >= 8) break
        }
        return out
    }

    private fun parsePatchHtml(html: String): List<PatchNoteBlock> {
        if (html.isBlank()) return emptyList()
        val blocks = mutableListOf<PatchNoteBlock>()

        // Ganti tag <img> dan <hr> menjadi token baris tersendiri sebelum pemisahan blok.
        val withMedia = IMG_REGEX.replace(html) { m ->
            val tag = m.value
            val src = SRC_ATTR_REGEX.find(tag)?.groupValues?.getOrNull(1)?.trim().orEmpty()
            val alt = ALT_ATTR_REGEX.find(tag)?.groupValues?.getOrNull(1)?.trim().orEmpty()
            val resolved = resolveContentUrl(src)
            if (resolved.isNotBlank()) "\n@@IMG@@$resolved@@ALT@@$alt\n" else ""
        }.replace(Regex("<hr\\s*/?>", RegexOption.IGNORE_CASE), "\n@@HR@@\n")

        // Normalisasi tag blok HTML menjadi penanda baris agar urutan heading, blockquote, li, dan p tetap rapi.
        val normalized = withMedia
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("</?(h[1-5]|p|li|blockquote|ul|ol|div)[^>]*>", RegexOption.IGNORE_CASE)) { m ->
                val tag = m.value.lowercase(Locale.ROOT)
                when {
                    tag.startsWith("<h1") -> "\n@@H1@@"
                    tag.startsWith("<h2") -> "\n@@H2@@"
                    tag.startsWith("<h3") || tag.startsWith("<h4") || tag.startsWith("<h5") -> "\n@@H3@@"
                    tag.startsWith("<li") -> "\n@@LI@@"
                    tag.startsWith("<blockquote") -> "\n@@QUOTE@@"
                    tag.startsWith("<p") -> "\n@@P@@"
                    else -> "\n"
                }
            }

        for (rawLine in normalized.split('\n')) {
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) continue
            if (trimmed == "@@HR@@") {
                if (blocks.lastOrNull() != PatchNoteBlock.Divider) blocks.add(PatchNoteBlock.Divider)
                continue
            }
            if (trimmed.startsWith("@@IMG@@")) {
                val payload = trimmed.removePrefix("@@IMG@@")
                val parts = payload.split("@@ALT@@", limit = 2)
                val url = parts[0].trim()
                val alt = parts.getOrElse(1) { "" }.trim()
                if (url.isNotBlank()) blocks.add(PatchNoteBlock.Image(url, alt))
                continue
            }

            val (kind, contentHtml) = when {
                trimmed.startsWith("@@H1@@") -> "H1" to trimmed.removePrefix("@@H1@@")
                trimmed.startsWith("@@H2@@") -> "H2" to trimmed.removePrefix("@@H2@@")
                trimmed.startsWith("@@H3@@") -> "H3" to trimmed.removePrefix("@@H3@@")
                trimmed.startsWith("@@LI@@") -> "LI" to trimmed.removePrefix("@@LI@@")
                trimmed.startsWith("@@QUOTE@@") -> "QUOTE" to trimmed.removePrefix("@@QUOTE@@")
                trimmed.startsWith("@@P@@") -> "P" to trimmed.removePrefix("@@P@@")
                else -> "P" to trimmed
            }
            val clean = stripHtml(contentHtml)
            if (clean.isEmpty()) continue
            when (kind) {
                "H1" -> blocks.add(PatchNoteBlock.Heading(1, clean))
                "H2" -> blocks.add(PatchNoteBlock.Heading(2, clean))
                "H3" -> blocks.add(PatchNoteBlock.Heading(3, clean))
                "LI" -> blocks.add(PatchNoteBlock.Bullet(clean))
                "QUOTE" -> blocks.add(PatchNoteBlock.Quote(clean))
                else -> blocks.add(PatchNoteBlock.Paragraph(clean))
            }
        }
        return blocks
    }

    private fun stripHtml(fragment: String): String {
        val text = Html.fromHtml(fragment, Html.FROM_HTML_MODE_COMPACT).toString()
        return text.replace('\u00A0', ' ').replace(Regex("\\s+"), " ").trim()
    }

    private fun JsonObject.str(key: String): String {
        val el = get(key) ?: return ""
        return if (el.isJsonPrimitive) el.asString else ""
    }

    private fun JsonObject.getAsJsonObjectOrNull(key: String): JsonObject? {
        val el = get(key) ?: return null
        return if (el.isJsonObject) el.asJsonObject else null
    }

    private fun com.google.gson.JsonElement.asJsonObjectOrNull(): JsonObject? =
        if (isJsonObject) asJsonObject else null
}

// ----------------------------------------------------------------------------
// Section: core/record/ScreenRecorder.kt
// ----------------------------------------------------------------------------

/**
 * Perekam layar bawaan, dirancang supaya FPS game tidak ikut turun:
 *
 *  - Seluruh jalur video memakai encoder H.264 perangkat keras lewat [MediaRecorder] dengan input
 *    Surface. Tidak ada satu pun frame yang disalin atau diproses oleh CPU/Java; yang menggambar
 *    ke encoder adalah compositor sistem (SurfaceFlinger), bukan game.
 *  - Tidak ada thread, timer, atau callback per-frame di sini. Satu-satunya pekerjaan terus-menerus
 *    adalah listener suhu bawaan sistem, yang hanya terpanggil saat status panas berubah.
 *  - Resolusi dan FPS rekaman dibatasi preset (bawaan 720p/30 fps) lalu disesuaikan dengan
 *    kemampuan encoder HP, karena beban rekam naik hampir sebanding dengan jumlah piksel x FPS.
 *  - Bila HP tidak punya encoder H.264 perangkat keras, rekaman ditolak (encoder perangkat lunak
 *    akan memakan CPU yang dibutuhkan game).
 *  - Rekaman otomatis dijeda saat game keluar dari layar ([pause]/[resume]) dan dihentikan bila HP
 *    terlalu panas, ruang penyimpanan hampir habis, atau sistem mencabut izin tangkap layar.
 *  - Berhenti dikerjakan di thread terpisah (finalisasi berkas MP4 bisa memakan ratusan milidetik)
 *    kecuali saat proses game akan dimatikan ([stopBlocking]).
 *
 * Semua pemanggilan publik dilakukan dari thread utama. Objek ini hidup di proses `:game`, sama
 * dengan GameActivity dan [RecordService].
 */
object ScreenRecorder {

    /**
     * Status yang dibaca UI. Semua waktu memakai [SystemClock.elapsedRealtime]. Lama rekaman yang
     * tampil tidak menghitung waktu jeda: [elapsedBeforeMs] adalah lama dari bagian-bagian
     * sebelumnya, [segmentStart] awal bagian yang sedang berjalan (dipakai hanya bila tidak dijeda).
     */
    data class State(
        val recording: Boolean = false,
        val paused: Boolean = false,
        val startedAt: Long = 0L,
        val elapsedBeforeMs: Long = 0L,
        val segmentStart: Long = 0L
    ) {
        /** Lama rekaman (ms) pada saat [now], tanpa waktu jeda. */
        fun elapsedMs(now: Long): Long =
            if (!recording) 0L
            else elapsedBeforeMs + if (paused) 0L else (now - segmentStart).coerceAtLeast(0L)
    }

    /** Alasan rekaman ditahan; rekaman jalan lagi hanya bila semua alasan sudah dilepas. */
    enum class Hold {
        /** Layar game tidak sedang tampil (Home, aplikasi lain, layar mati). */
        SCREEN,

        /** Menu samping game terbuka: menunya tidak perlu ikut terekam. */
        MENU
    }

    /** Bahan baku konfigurasi video yang sudah dihitung. */
    data class VideoPlan(val width: Int, val height: Int, val fps: Int, val bitrate: Int)

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val main = Handler(Looper.getMainLooper())

    private var appContext: Context? = null
    private var projection: MediaProjection? = null
    private var recorder: MediaRecorder? = null
    private var display: VirtualDisplay? = null
    private var output: Output? = null
    private var thermalListener: PowerManager.OnThermalStatusChangedListener? = null
    private var pendingFinalize: Thread? = null

    private class Output(
        val uri: Uri?,
        val file: File?,
        val pfd: ParcelFileDescriptor?,
        val displayName: String
    )

    val isRecording: Boolean get() = _state.value.recording

    // ---------------------------------------------------------------------------------------
    // Rencana video
    // ---------------------------------------------------------------------------------------

    /** Sisi pendek maksimum (piksel) untuk tiap preset kualitas. */
    private fun presetShortSide(quality: String): Int = when (quality) {
        "eco" -> 540
        "high" -> 1080
        else -> 720
    }

    private fun isSoftwareCodec(info: MediaCodecInfo): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) return info.isSoftwareOnly
        val n = info.name.lowercase(Locale.US)
        return n.startsWith("omx.google.") || n.startsWith("c2.android.") || n.contains(".sw.")
    }

    /**
     * Kemampuan encoder H.264 perangkat keras pertama. Null berarti pencarian gagal (jangan
     * menolak rekaman). Melempar [NoHardwareEncoder] bila daftar encoder terbaca tapi tidak ada
     * satu pun yang perangkat keras.
     */
    private fun hardwareAvcCaps(): MediaCodecInfo.VideoCapabilities? {
        val infos = try {
            MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
        } catch (_: Throwable) {
            return null
        }
        val hw = infos.firstOrNull { info ->
            info.isEncoder &&
                info.supportedTypes.any { it.equals("video/avc", ignoreCase = true) } &&
                !isSoftwareCodec(info)
        } ?: throw NoHardwareEncoder()
        return try {
            hw.getCapabilitiesForType("video/avc").videoCapabilities
        } catch (_: Throwable) {
            null
        }
    }

    private class NoHardwareEncoder : RuntimeException()

    private fun alignDown(value: Int, alignment: Int): Int {
        val a = max(alignment, 2)
        return max(a, value / a * a)
    }

    /**
     * Menghitung ukuran, FPS, dan bitrate. [srcW] x [srcH] adalah ukuran layar (bentuk landscape).
     * Tidak pernah memperbesar dari ukuran layar asli.
     */
    fun plan(srcW: Int, srcH: Int, settings: LauncherSettings): VideoPlan {
        val caps = hardwareAvcCaps()

        val srcLong = max(srcW, srcH).coerceAtLeast(2)
        val srcShort = min(srcW, srcH).coerceAtLeast(2)
        val scale = min(1f, presetShortSide(settings.recordQuality) / srcShort.toFloat())
        var w = (srcLong * scale).roundToInt()
        var h = (srcShort * scale).roundToInt()

        val wAlign = caps?.widthAlignment ?: 2
        val hAlign = caps?.heightAlignment ?: 2
        w = alignDown(w, wAlign)
        h = alignDown(h, hAlign)

        // Kecilkan bertahap bila encoder menolak ukuran ini (sangat jarang pada ukuran 16:9 biasa).
        if (caps != null) {
            var guard = 0
            while (!caps.isSizeSupported(w, h) && guard < 8) {
                w = alignDown((w * 0.85f).roundToInt(), wAlign)
                h = alignDown((h * 0.85f).roundToInt(), hAlign)
                guard++
            }
        }

        var fps = if (settings.recordFps >= 60) 60 else 30
        if (caps != null) {
            val upper = try {
                caps.getSupportedFrameRatesFor(w, h).upper.toInt()
            } catch (_: Throwable) {
                fps
            }
            if (upper in 1 until fps) fps = if (upper >= 30) 30 else upper
        }

        // ~0,10 bit per piksel per frame: cukup bersih untuk gambar Minecraft yang bergerak
        // banyak tanpa membuat encoder dan penyimpanan bekerja berlebihan.
        var bitrate = (w.toLong() * h * fps * 0.10).toLong()
            .coerceIn(1_500_000L, 16_000_000L).toInt()
        if (caps != null) {
            try {
                bitrate = caps.bitrateRange.clamp(bitrate)
            } catch (_: Throwable) {
            }
        }
        return VideoPlan(w, h, fps, bitrate)
    }

    // ---------------------------------------------------------------------------------------
    // Mulai
    // ---------------------------------------------------------------------------------------

    /**
     * Dipanggil dari [RecordService] SETELAH startForeground (wajib di Android 14+). Mengembalikan
     * true bila rekaman berjalan; bila gagal semua sumber daya sudah dilepas dan pesan sudah tampil.
     */
    fun start(
        context: Context,
        resultCode: Int,
        data: Intent,
        screenW: Int,
        screenH: Int,
        screenDpi: Int,
        settings: LauncherSettings,
        useMic: Boolean
    ): Boolean {
        if (_state.value.recording) return true
        val ctx = context.applicationContext
        appContext = ctx

        val plan = try {
            plan(screenW, screenH, settings)
        } catch (_: NoHardwareEncoder) {
            toast("HP ini tidak punya encoder video perangkat keras. Rekaman dibatalkan supaya game tidak lag.")
            return false
        } catch (e: Throwable) {
            toast("Gagal menyiapkan rekaman: ${e.message ?: e.javaClass.simpleName}")
            return false
        }

        val maxBytes = freeSpaceBudget(ctx)
        if (maxBytes <= 0L) {
            toast("Ruang penyimpanan kurang untuk merekam.")
            return false
        }

        val mpm = ctx.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
        if (mpm == null) {
            toast("Perekam layar tidak tersedia di HP ini.")
            return false
        }

        var rec: MediaRecorder? = null
        var proj: MediaProjection? = null
        var vd: VirtualDisplay? = null
        var out: Output? = null
        try {
            val o = openOutput(ctx)
            out = o

            val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(ctx)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            rec = r
            // Urutan pemanggilan MediaRecorder penting; jangan diubah.
            if (useMic) r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            val fd = o.pfd
            val outFile = o.file
            when {
                fd != null -> r.setOutputFile(fd.fileDescriptor)
                outFile != null -> r.setOutputFile(outFile.absolutePath)
                else -> throw IllegalStateException("Output rekaman tidak tersedia")
            }
            r.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            r.setVideoSize(plan.width, plan.height)
            r.setVideoFrameRate(plan.fps)
            r.setVideoEncodingBitRate(plan.bitrate)
            if (useMic) {
                r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                r.setAudioEncodingBitRate(96_000)
                r.setAudioSamplingRate(44_100)
            }
            r.setMaxFileSize(maxBytes)
            r.setOnInfoListener { _, what, _ ->
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED ||
                    what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED
                ) {
                    main.post {
                        toast("Ruang penyimpanan penuh. Rekaman disimpan dan dihentikan.")
                        stop()
                    }
                }
            }
            r.setOnErrorListener { _, _, _ ->
                main.post {
                    toast("Perekam mengalami kesalahan. Rekaman dihentikan.")
                    stop()
                }
            }
            r.prepare()

            val p = mpm.getMediaProjection(resultCode, data)
                ?: throw IllegalStateException("izin tangkap layar tidak valid")
            proj = p
            // Android 14+ mewajibkan callback terpasang sebelum VirtualDisplay dibuat.
            p.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    main.post {
                        if (_state.value.recording) {
                            toast("Tangkap layar dihentikan oleh sistem. Rekaman disimpan.")
                            stop()
                        }
                    }
                }
            }, main)

            vd = p.createVirtualDisplay(
                "AmberRecord",
                plan.width,
                plan.height,
                screenDpi.coerceAtLeast(120),
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                r.surface,
                null,
                null
            )
            r.start()
        } catch (e: Throwable) {
            try { vd?.release() } catch (_: Throwable) {}
            try { proj?.stop() } catch (_: Throwable) {}
            try { rec?.reset() } catch (_: Throwable) {}
            try { rec?.release() } catch (_: Throwable) {}
            if (out != null) discardOutput(ctx, out)
            toast("Gagal memulai rekaman: ${e.message ?: e.javaClass.simpleName}")
            return false
        }

        recorder = rec
        projection = proj
        display = vd
        output = out
        val now = SystemClock.elapsedRealtime()
        _state.value = State(recording = true, paused = false, startedAt = now, elapsedBeforeMs = 0L, segmentStart = now)
        registerThermalGuard(ctx, settings)
        // Tidak ada toast saat mulai: toast ikut terekam di awal video. Tandanya cukup getar singkat
        // (dari GameActivity), notifikasi, dan penghitung waktu di menu.
        applyHolds()
        return true
    }

    // ---------------------------------------------------------------------------------------
    // Jeda / lanjut. Alasan jeda dicatat terpisah ([Hold]) dan dipertahankan walau belum merekam,
    // jadi rekaman yang baru dimulai langsung mengikuti keadaan layar saat itu.
    // ---------------------------------------------------------------------------------------

    private val holds = HashSet<Hold>()
    private val pendingRelease = HashMap<Hold, Runnable>()

    fun hold(reason: Hold) {
        pendingRelease.remove(reason)?.let { main.removeCallbacks(it) }
        holds.add(reason)
        applyHolds()
    }

    /** [delayMs] > 0 menunda pelepasan, mis. supaya animasi menutup menu tidak ikut terekam. */
    fun release(reason: Hold, delayMs: Long = 0L) {
        pendingRelease.remove(reason)?.let { main.removeCallbacks(it) }
        if (delayMs <= 0L) {
            holds.remove(reason)
            applyHolds()
            return
        }
        val r = Runnable {
            pendingRelease.remove(reason)
            holds.remove(reason)
            applyHolds()
        }
        pendingRelease[reason] = r
        main.postDelayed(r, delayMs)
    }

    private fun applyHolds() {
        if (holds.isEmpty()) resumeRecording() else pauseRecording()
    }

    private fun pauseRecording() {
        val s = _state.value
        if (!s.recording || s.paused) return
        try {
            recorder?.pause()
            val now = SystemClock.elapsedRealtime()
            _state.value = s.copy(paused = true, elapsedBeforeMs = s.elapsedMs(now))
        } catch (_: Throwable) {
        }
    }

    private fun resumeRecording() {
        val s = _state.value
        if (!s.recording || !s.paused) return
        try {
            recorder?.resume()
            _state.value = s.copy(paused = false, segmentStart = SystemClock.elapsedRealtime())
        } catch (_: Throwable) {
        }
    }

    // ---------------------------------------------------------------------------------------
    // Berhenti
    // ---------------------------------------------------------------------------------------

    /** Berhenti tanpa menahan thread utama; berkas diselesaikan di thread latar. */
    fun stop() = stopInternal(blocking = false)

    /** Berhenti dan menunggu berkas selesai ditulis. Hanya untuk saat proses game akan dimatikan. */
    fun stopBlocking() = stopInternal(blocking = true)

    private fun stopInternal(blocking: Boolean) {
        val rec = recorder
        val ctx = appContext
        val out = output
        if (!_state.value.recording || rec == null || ctx == null || out == null) {
            // Tunggu finalisasi sebelumnya (mis. dari stop() tak lama sebelum proses dimatikan).
            if (blocking) try { pendingFinalize?.join(3000) } catch (_: Throwable) {}
            RecordService.requestStop(appContext)
            return
        }

        val proj = projection
        val vd = display
        recorder = null
        projection = null
        display = null
        output = null
        unregisterThermalGuard(ctx)
        _state.value = State()

        // Hentikan sumber frame lebih dulu supaya beban komposisi langsung hilang.
        try { vd?.release() } catch (_: Throwable) {}
        try { proj?.stop() } catch (_: Throwable) {}
        RecordService.requestStop(ctx)

        val finalize = Runnable { finalizeOutput(ctx, rec, out) }
        if (blocking) {
            finalize.run()
        } else {
            val t = thread(name = "novacraft-rec-finalize", isDaemon = false, priority = Thread.MIN_PRIORITY) {
                finalize.run()
            }
            pendingFinalize = t
        }
    }

    private fun finalizeOutput(ctx: Context, rec: MediaRecorder, out: Output) {
        var ok = true
        try {
            rec.stop()
        } catch (_: Throwable) {
            // Terjadi bila berhenti sebelum ada frame sama sekali: berkasnya tidak valid.
            ok = false
        }
        try { rec.reset() } catch (_: Throwable) {}
        try { rec.release() } catch (_: Throwable) {}
        try { out.pfd?.close() } catch (_: Throwable) {}

        if (!ok) {
            discardOutput(ctx, out)
            toast("Rekaman terlalu singkat, tidak disimpan.")
            return
        }
        if (out.uri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val values = ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }
                ctx.contentResolver.update(out.uri, values, null, null)
            } catch (_: Throwable) {
            }
            toast("Rekaman disimpan di Movies/NovaCraft")
        } else if (out.file != null) {
            try {
                MediaScannerConnection.scanFile(ctx, arrayOf(out.file.absolutePath), arrayOf("video/mp4"), null)
            } catch (_: Throwable) {
            }
            toast("Rekaman disimpan: ${out.file.name}")
        }
    }

    // ---------------------------------------------------------------------------------------
    // Penyimpanan
    // ---------------------------------------------------------------------------------------

    private const val MIN_FREE_BYTES = 300L * 1024 * 1024
    private const val MAX_FILE_BYTES = 8L * 1024 * 1024 * 1024

    /** Batas ukuran berkas = sisa ruang dikurangi cadangan 300 MB; <= 0 bila ruang tidak cukup. */
    private fun freeSpaceBudget(ctx: Context): Long {
        return try {
            val path = (ctx.getExternalFilesDir(null) ?: ctx.filesDir).path
            val free = StatFs(path).availableBytes
            min(free - MIN_FREE_BYTES, MAX_FILE_BYTES)
        } catch (_: Throwable) {
            MAX_FILE_BYTES
        }
    }

    private fun newFileName(): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "NovaCraft_$stamp.mp4"
    }

    private fun openOutput(ctx: Context): Output {
        val name = newFileName()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Lewat MediaStore: berkas muncul di Galeri tanpa izin penyimpanan, dan MediaRecorder
            // menulis langsung ke file descriptor (tanpa salinan tambahan setelah rekaman).
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/NovaCraft")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val resolver = ctx.contentResolver
            val uri = resolver.insert(
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                values
            ) ?: throw IllegalStateException("tidak bisa membuat berkas di Movies")
            // "rw": penulis MP4 perlu berpindah posisi untuk menulis indeks di akhir berkas.
            val pfd = resolver.openFileDescriptor(uri, "rw")
            if (pfd == null) {
                try { resolver.delete(uri, null, null) } catch (_: Throwable) {}
                throw IllegalStateException("tidak bisa membuka berkas rekaman")
            }
            return Output(uri, null, pfd, name)
        }

        @Suppress("DEPRECATION")
        val publicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "NovaCraft")
        val canWritePublic = ContextCompat.checkSelfPermission(
            ctx, android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val dir = if (canWritePublic && (publicDir.isDirectory || publicDir.mkdirs())) {
            publicDir
        } else {
            File(ctx.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: ctx.filesDir, "NovaCraft").also { it.mkdirs() }
        }
        return Output(null, File(dir, name), null, name)
    }

    private fun discardOutput(ctx: Context, out: Output) {
        try { out.pfd?.close() } catch (_: Throwable) {}
        try {
            if (out.uri != null) ctx.contentResolver.delete(out.uri, null, null)
            else out.file?.delete()
        } catch (_: Throwable) {
        }
    }

    // ---------------------------------------------------------------------------------------
    // Pelindung suhu
    // ---------------------------------------------------------------------------------------

    private fun registerThermalGuard(ctx: Context, settings: LauncherSettings) {
        if (!settings.recordThermalGuard || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        try {
            val pm = ctx.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
            val listener = PowerManager.OnThermalStatusChangedListener { status ->
                if (status >= PowerManager.THERMAL_STATUS_SEVERE && _state.value.recording) {
                    toast("HP terlalu panas. Rekaman disimpan dan dihentikan agar game tetap lancar.")
                    stop()
                }
            }
            pm.addThermalStatusListener(ContextCompat.getMainExecutor(ctx), listener)
            thermalListener = listener
        } catch (_: Throwable) {
        }
    }

    private fun unregisterThermalGuard(ctx: Context) {
        val listener = thermalListener ?: return
        thermalListener = null
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        try {
            val pm = ctx.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
            pm.removeThermalStatusListener(listener)
        } catch (_: Throwable) {
        }
    }

    // ---------------------------------------------------------------------------------------

    private fun toast(message: String) {
        val ctx = appContext ?: return
        val show = Runnable {
            try {
                Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show()
            } catch (_: Throwable) {
            }
        }
        if (Looper.myLooper() == Looper.getMainLooper()) show.run() else main.post(show)
    }
}

// ----------------------------------------------------------------------------
// Section: core/update/NovaUpdateReleaseEngine.kt
// ----------------------------------------------------------------------------

// ============================================================================
// NovaCraft Unified Release Update Checker, Parser & ETag Store Engine
// ============================================================================

enum class FailureKind {
    OFFLINE,
    TIMEOUT,
    RATE_LIMITED,
    SERVER,
    BAD_RESPONSE,
    STORAGE_FULL,
    DOWNLOAD,
    CORRUPT_FILE,
    WRONG_APP,
    WRONG_SIGNATURE,
    NOT_NEWER,
    GAME_RUNNING,
    INSTALL_BLOCKED,
    INSTALL_FAILED
}

data class UpdateFailure(
    val kind: FailureKind,
    val message: String,
    val retryAtMillis: Long = 0L
)

data class ApkAsset(
    val name: String,
    val url: String,
    val sizeBytes: Long?,
    val sha256: String?
)

data class AppUpdate(
    val tag: String,
    val versionName: String,
    val notes: String,
    val pageUrl: String,
    val apk: ApkAsset
)

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class UpToDate(val checkedAt: Long) : UpdateState
    data class NoApk(val tag: String, val pageUrl: String) : UpdateState
    data class Failed(val failure: UpdateFailure) : UpdateState
    data class Available(val update: AppUpdate) : UpdateState
    data class Downloading(val update: AppUpdate, val doneBytes: Long, val totalBytes: Long) : UpdateState
    data class ReadyToInstall(val update: AppUpdate, val apk: File, val note: String? = null) : UpdateState
    data class NeedsInstallPermission(val update: AppUpdate, val apk: File) : UpdateState
    data class Installing(val update: AppUpdate) : UpdateState
    data class ActionFailed(
        val update: AppUpdate,
        val failure: UpdateFailure,
        val apk: File? = null
    ) : UpdateState
}

class AppVersion private constructor(
    private val core: List<Long>,
    private val label: List<String>
) : Comparable<AppVersion> {

    override fun compareTo(other: AppVersion): Int {
        for (i in 0 until 3) {
            val c = core[i].compareTo(other.core[i])
            if (c != 0) return c
        }
        if (label.isEmpty() || other.label.isEmpty()) {
            return when {
                label.isEmpty() && other.label.isEmpty() -> 0
                label.isEmpty() -> 1
                else -> -1
            }
        }
        for (i in 0 until minOf(label.size, other.label.size)) {
            val a = label[i]
            val b = other.label[i]
            val an = a.toLongOrNull()
            val bn = b.toLongOrNull()
            val c = when {
                an != null && bn != null -> an.compareTo(bn)
                an != null -> -1
                bn != null -> 1
                else -> a.compareTo(b)
            }
            if (c != 0) return c
        }
        return label.size.compareTo(other.label.size)
    }

    override fun equals(other: Any?): Boolean = other is AppVersion && compareTo(other) == 0

    override fun hashCode(): Int = 31 * core.hashCode() + label.hashCode()

    override fun toString(): String =
        core.joinToString(".") + if (label.isEmpty()) "" else "-" + label.joinToString(".")

    companion object {
        val ZERO = AppVersion(listOf(0L, 0L, 0L), emptyList())
        private val PATTERN =
            Regex("""^v?(\d+)\.(\d+)\.(\d+)(?:-([0-9A-Za-z.-]+))?(?:\+[0-9A-Za-z.-]+)?$""")

        fun parse(text: String?): AppVersion? {
            val m = PATTERN.matchEntire(text?.trim().orEmpty()) ?: return null
            val core = (1..3).map { m.groupValues[it].toLongOrNull() ?: return null }
            val label = m.groupValues[4].takeIf { it.isNotEmpty() }?.split('.') ?: emptyList()
            if (label.any { it.isEmpty() }) return null
            return AppVersion(core, label)
        }
    }
}

internal object UpdateStore {
    private const val PREFS_NAME = "amber_launcher_prefs"
    private const val KEY_SKIPPED_TAG = "update_skipped_tag"
    private const val KEY_BLOCKED_UNTIL = "update_blocked_until"
    private const val CACHE_FILE = "update_releases.json"

    class Cache(val etag: String?, val body: String)

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun readCache(context: Context): Cache? {
        val file = File(context.filesDir, CACHE_FILE)
        if (!file.isFile) return null
        return try {
            val o = JsonParser.parseString(file.readText()).asJsonObject
            val body = o.get("body")?.takeIf { it.isJsonPrimitive }?.asString ?: return null
            val etag = o.get("etag")?.takeIf { it.isJsonPrimitive }?.asString
            Cache(etag, body)
        } catch (_: Exception) {
            null
        }
    }

    fun writeCache(context: Context, etag: String?, body: String) {
        try {
            val file = File(context.filesDir, CACHE_FILE)
            val tmp = File(context.filesDir, "$CACHE_FILE.tmp")
            val json = JsonObject().apply {
                addProperty("etag", etag)
                addProperty("body", body)
            }
            tmp.writeText(json.toString())
            if (!tmp.renameTo(file)) {
                file.delete()
                tmp.renameTo(file)
            }
        } catch (_: Exception) {
        }
    }

    fun skippedTag(context: Context): String? = prefs(context).getString(KEY_SKIPPED_TAG, null)

    fun setSkippedTag(context: Context, tag: String) {
        prefs(context).edit().putString(KEY_SKIPPED_TAG, tag).apply()
    }

    fun blockedUntil(context: Context): Long = prefs(context).getLong(KEY_BLOCKED_UNTIL, 0L)

    fun setBlockedUntil(context: Context, untilMillis: Long) {
        prefs(context).edit().putLong(KEY_BLOCKED_UNTIL, untilMillis).apply()
    }
}

class ReleaseInfo(
    val tag: String,
    val version: AppVersion,
    val notes: String,
    val pageUrl: String,
    val apks: List<ApkAsset>
)

sealed interface Evaluation {
    data object UpToDate : Evaluation
    data class Newer(val update: AppUpdate) : Evaluation
    data class NoApkForDevice(val tag: String, val pageUrl: String) : Evaluation
}

object ReleaseParser {
    private const val MAX_NOTES_CHARS = 1200

    fun parse(json: String): List<ReleaseInfo> {
        val root = JsonParser.parseString(json)
        require(root.isJsonArray) { "balasan bukan daftar rilis" }
        val out = ArrayList<ReleaseInfo>()
        for (element in root.asJsonArray) {
            if (!element.isJsonObject) continue
            val release = element.asJsonObject
            if (release.bool("draft")) continue

            val tag = release.str("tag_name") ?: continue
            val version = AppVersion.parse(tag) ?: continue
            val pageUrl = release.str("html_url")?.takeIf { isOfficialUrl(it) } ?: continue
            out += ReleaseInfo(
                tag = tag,
                version = version,
                notes = release.str("body").orEmpty(),
                pageUrl = pageUrl,
                apks = parseAssets(release.get("assets"))
            )
        }
        return out
    }

    fun evaluate(releases: List<ReleaseInfo>, current: AppVersion, want32Bit: Boolean): Evaluation {
        var best: Pair<ReleaseInfo, ApkAsset>? = null
        var newestWithoutApk: ReleaseInfo? = null
        for (release in releases) {
            if (release.version <= current) continue
            val apk = chooseApk(release.apks, want32Bit)
            if (apk != null) {
                val b = best
                if (b == null || release.version > b.first.version) best = release to apk
            } else {
                val n = newestWithoutApk
                if (n == null || release.version > n.version) newestWithoutApk = release
            }
        }
        val b = best
        if (b != null) {
            val (release, apk) = b
            return Evaluation.Newer(
                AppUpdate(
                    tag = release.tag,
                    versionName = release.tag.removePrefix("v"),
                    notes = release.notes,
                    pageUrl = release.pageUrl,
                    apk = apk
                )
            )
        }
        val n = newestWithoutApk
        if (n != null) return Evaluation.NoApkForDevice(n.tag, n.pageUrl)
        return Evaluation.UpToDate
    }

    fun evaluateJson(json: String, current: AppVersion, want32Bit: Boolean): Evaluation =
        evaluate(parse(json), current, want32Bit)

    fun chooseApk(apks: List<ApkAsset>, want32Bit: Boolean = false): ApkAsset? =
        apks.firstOrNull {
            val isLegacy32 = it.name.contains("32bit", ignoreCase = true) ||
                it.name.contains("armeabi", ignoreCase = true)
            isLegacy32 == want32Bit
        }

    fun readableNotes(raw: String): String =
        raw.lineSequence()
            .map { it.trimEnd() }
            .filterNot {
                it.startsWith("#") ||
                    it.contains("https://") ||
                    it.startsWith("Pilih APK", ignoreCase = true)
            }
            .map { line ->
                val plain = line.replace("**", "")
                if (plain.startsWith("- ")) "• " + plain.drop(2) else plain
            }
            .joinToString("\n")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
            .take(MAX_NOTES_CHARS)

    private fun parseAssets(element: JsonElement?): List<ApkAsset> {
        if (element == null || !element.isJsonArray) return emptyList()
        val out = ArrayList<ApkAsset>()
        for (asset in element.asJsonArray) {
            if (!asset.isJsonObject) continue
            val o = asset.asJsonObject
            val name = o.str("name") ?: continue
            val url = o.str("browser_download_url") ?: continue
            if (!name.endsWith(".apk", ignoreCase = true) || !isOfficialUrl(url)) continue
            val state = o.str("state")
            if (state != null && state != "uploaded") continue
            out += ApkAsset(
                name = name,
                url = url,
                sizeBytes = o.long("size")?.takeIf { it > 0 },
                sha256 = o.str("digest")?.let { parseSha256(it) }
            )
        }
        return out
    }

    internal fun parseSha256(digest: String): String? {
        val value = digest.trim().removePrefix("sha256:").lowercase()
        return value.takeIf { it.length == 64 && it.all { c -> c in '0'..'9' || c in 'a'..'f' } }
    }

    private fun isOfficialUrl(url: String): Boolean =
        url.startsWith("https://github.com/${UpdateChecker.GITHUB_REPO}/", ignoreCase = true)

    private fun JsonObject.str(name: String): String? {
        val value = get(name) ?: return null
        return if (value.isJsonPrimitive) value.asString else null
    }

    private fun JsonObject.bool(name: String): Boolean {
        val value = get(name) ?: return false
        return value.isJsonPrimitive && value.asBoolean
    }

    private fun JsonObject.long(name: String): Long? {
        val value = get(name) ?: return null
        return if (value.isJsonPrimitive && value.asJsonPrimitive.isNumber) value.asLong else null
    }
}

object ReleaseFetcher {
    sealed interface Result {
        data class Fresh(val body: String, val etag: String?) : Result
        data object NotModified : Result
        data class Failure(val failure: UpdateFailure) : Result
    }

    private const val MAX_BODY_BYTES = 2L * 1024 * 1024
    private const val RATE_LIMIT_DEFAULT_MS = 15L * 60 * 1000
    private const val RATE_LIMIT_MAX_MS = 60L * 60 * 1000

    fun fetch(
        client: OkHttpClient,
        url: String,
        userAgent: String,
        etag: String?,
        nowMs: Long = System.currentTimeMillis()
    ): Result {
        return Result.Failure(
            UpdateFailure(FailureKind.OFFLINE, "Koneksi pembaruan GitHub dinonaktifkan.")
        )
    }

    fun failureFor(e: Throwable): UpdateFailure = when (e) {
        is UnknownHostException, is ConnectException, is NoRouteToHostException ->
            UpdateFailure(FailureKind.OFFLINE, "Tidak ada koneksi internet.")
        is InterruptedIOException ->
            UpdateFailure(FailureKind.TIMEOUT, "Koneksi terlalu lambat. Coba lagi.")
        is SSLException ->
            UpdateFailure(FailureKind.OFFLINE, "Koneksi aman ke GitHub gagal. Periksa tanggal dan jam HP.")
        is IOException ->
            UpdateFailure(FailureKind.OFFLINE, "Koneksi internet terputus. Coba lagi.")
        else ->
            UpdateFailure(FailureKind.BAD_RESPONSE, "Terjadi kesalahan yang tidak terduga.")
    }

    private fun isRateLimited(resp: Response): Boolean =
        resp.code == 429 ||
            (resp.code == 403 && (resp.header("x-ratelimit-remaining") == "0" || resp.header("retry-after") != null))

    private fun rateLimited(resp: Response, nowMs: Long): Result.Failure {
        val retryAfterSec = resp.header("retry-after")?.trim()?.toLongOrNull()
        val resetEpochSec = resp.header("x-ratelimit-reset")?.trim()?.toLongOrNull()
        val until = when {
            retryAfterSec != null -> nowMs + retryAfterSec * 1000
            resetEpochSec != null -> resetEpochSec * 1000
            else -> nowMs + RATE_LIMIT_DEFAULT_MS
        }.coerceIn(nowMs + 60_000L, nowMs + RATE_LIMIT_MAX_MS)
        val clock = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(until))
        return Result.Failure(
            UpdateFailure(
                FailureKind.RATE_LIMITED,
                "Batas permintaan GitHub tercapai (biasanya karena banyak orang memakai jaringan yang sama). Coba lagi sekitar pukul $clock.",
                retryAtMillis = until
            )
        )
    }
}

object UpdateChecker {
    internal const val GITHUB_REPO = ""

    sealed interface Result {
        data class Found(val update: AppUpdate) : Result
        data object UpToDate : Result
        data class NoApk(val tag: String, val pageUrl: String) : Result
        data class Failed(val failure: UpdateFailure) : Result
    }

    fun wants32BitApk(): Boolean = false

    fun currentVersionName(context: Context): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    } catch (_: Exception) {
        ""
    }

    fun currentVersionCode(context: Context): Long = try {
        PackageInfoCompat.getLongVersionCode(context.packageManager.getPackageInfo(context.packageName, 0))
    } catch (_: Exception) {
        0L
    }

    fun evaluateCached(context: Context): Result? = null

    suspend fun check(context: Context): Result = Result.UpToDate

    private fun isReadable(body: String): Boolean = try {
        ReleaseParser.parse(body)
        true
    } catch (_: Exception) {
        false
    }

    fun skip(context: Context, tag: String) = UpdateStore.setSkippedTag(context, tag)

    fun isSkipped(context: Context, tag: String): Boolean = UpdateStore.skippedTag(context) == tag

    private fun toResult(e: Evaluation): Result = when (e) {
        is Evaluation.Newer -> Result.Found(e.update)
        Evaluation.UpToDate -> Result.UpToDate
        is Evaluation.NoApkForDevice -> Result.NoApk(e.tag, e.pageUrl)
    }
}

// ----------------------------------------------------------------------------
// Section: core/update/NovaUpdateInstallerEngine.kt
// ----------------------------------------------------------------------------

// ============================================================================
// NovaCraft Unified APK Downloader, Installer & PackageInstaller Receiver
// ============================================================================

object ApkDownloader {
    private const val BUFFER_BYTES = 128 * 1024
    private const val MAX_ATTEMPTS = 4
    private const val PROGRESS_INTERVAL_MS = 250L

    class DownloadException(val failure: UpdateFailure, cause: Throwable? = null) :
        Exception(failure.message, cause)

    class CancelToken {
        @Volatile
        var cancelled = false
            private set

        @Volatile
        private var call: Call? = null

        internal fun attach(c: Call) {
            call = c
            if (cancelled) c.cancel()
        }

        fun cancel() {
            cancelled = true
            call?.cancel()
        }

        internal fun throwIfCancelled() {
            if (cancelled) throw CancellationException("Unduhan dibatalkan")
        }
    }

    private class HttpStatusException(val code: Int) : IOException("HTTP $code")

    private val lock = Any()

    fun download(
        client: OkHttpClient,
        url: String,
        target: File,
        expectedSize: Long?,
        expectedSha256: String?,
        userAgent: String,
        token: CancelToken = CancelToken(),
        freeSpace: () -> Long = { Long.MAX_VALUE },
        spareBytes: Long = 0L,
        onProgress: (done: Long, total: Long) -> Unit = { _, _ -> }
    ) {
        synchronized(lock) {
            target.parentFile?.mkdirs()
            val part = File(target.path + ".part")

            if ((expectedSize != null || expectedSha256 != null) && target.isFile &&
                isValid(target, expectedSize, expectedSha256)
            ) {
                onProgress(target.length(), target.length())
                return
            }
            target.delete()

            var attempts = 0
            var restartedAfterMismatch = false
            var resumedThisRun = false
            while (true) {
                token.throwIfCancelled()
                val sizeBefore = part.length()
                try {
                    if (sizeBefore > 0) resumedThisRun = true
                    transfer(client, url, part, expectedSize, userAgent, token, freeSpace, spareBytes, onProgress)

                    val problem = checkComplete(part, expectedSize, expectedSha256)
                    if (problem == null) {
                        if (!part.renameTo(target)) {
                            part.copyTo(target, overwrite = true)
                            part.delete()
                        }
                        return
                    }
                    part.delete()
                    if (resumedThisRun && !restartedAfterMismatch) {
                        restartedAfterMismatch = true
                        resumedThisRun = false
                        continue
                    }
                    throw DownloadException(problem)
                } catch (e: DownloadException) {
                    throw e
                } catch (e: CancellationException) {
                    throw e
                } catch (e: IOException) {
                    if (token.cancelled) throw CancellationException("Unduhan dibatalkan")
                    if (e is HttpStatusException && !isTransient(e.code)) throw DownloadException(httpFailure(e.code), e)
                    if (part.length() > sizeBefore) attempts = 0
                    attempts++
                    if (attempts >= MAX_ATTEMPTS) {
                        throw DownloadException(
                            UpdateFailure(
                                FailureKind.DOWNLOAD,
                                "Unduhan terputus. Ketuk Coba lagi; unduhan dilanjutkan dari bagian yang sudah ada."
                            ),
                            e
                        )
                    }
                    sleepBackoff(attempts, token)
                }
            }
        }
    }

    private fun transfer(
        client: OkHttpClient,
        url: String,
        part: File,
        expectedSize: Long?,
        userAgent: String,
        token: CancelToken,
        freeSpace: () -> Long,
        spareBytes: Long,
        onProgress: (Long, Long) -> Unit
    ) {
        var offset = part.length()
        if (expectedSize != null && offset > expectedSize) {
            part.delete()
            offset = 0
        }
        if (expectedSize != null && offset == expectedSize) return

        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .header("Accept", "application/octet-stream")
            .header("Accept-Encoding", "identity")
        if (offset > 0) builder.header("Range", "bytes=$offset-")
        val call = client.newCall(builder.build())
        token.attach(call)

        call.execute().use { resp ->
            when (resp.code) {
                200 -> offset = 0
                206 -> {
                    val start = contentRangeStart(resp.header("Content-Range"))
                    if (start != offset) {
                        part.delete()
                        throw IOException("Content-Range tidak cocok")
                    }
                }
                416 -> {
                    part.delete()
                    throw IOException("Range ditolak")
                }
                else -> throw HttpStatusException(resp.code)
            }
            val body = resp.body ?: throw IOException("Balasan kosong")

            val remaining = when {
                resp.code == 206 -> body.contentLength().takeIf { it > 0 }
                else -> body.contentLength().takeIf { it > 0 } ?: expectedSize
            }
            val total = expectedSize ?: remaining?.let { it + offset } ?: 0L
            if (remaining != null) {
                val need = remaining + spareBytes
                val free = freeSpace()
                if (free < need) {
                    throw DownloadException(
                        UpdateFailure(
                            FailureKind.STORAGE_FULL,
                            "Ruang penyimpanan tidak cukup. Kosongkan sekitar ${megabytes(need - free + 16L * 1024 * 1024)} MB lalu coba lagi."
                        )
                    )
                }
            }

            var done = offset
            var lastReport = 0L
            val buffer = ByteArray(BUFFER_BYTES)
            FileOutputStream(part, offset > 0).use { out ->
                val input = body.byteStream()
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    out.write(buffer, 0, n)
                    done += n
                    token.throwIfCancelled()
                    val now = System.nanoTime() / 1_000_000L
                    if (now - lastReport >= PROGRESS_INTERVAL_MS) {
                        lastReport = now
                        onProgress(done, total)
                    }
                }
                out.flush()
                try {
                    out.fd.sync()
                } catch (_: IOException) {
                }
            }
            onProgress(done, if (total > 0) total else done)
        }
    }

    private fun checkComplete(part: File, expectedSize: Long?, expectedSha256: String?): UpdateFailure? {
        val length = part.length()
        if (expectedSize != null) {
            if (length < expectedSize) throw IOException("Unduhan belum lengkap ($length dari $expectedSize byte)")
            if (length > expectedSize) return corrupt()
        }
        if (length == 0L) throw IOException("Berkas kosong")
        if (expectedSha256 != null && sha256Of(part) != expectedSha256) return corrupt()
        return null
    }

    private fun corrupt() = UpdateFailure(
        FailureKind.CORRUPT_FILE,
        "Berkas hasil unduhan rusak (tidak cocok dengan yang di GitHub). Ketuk Coba lagi untuk mengunduh ulang."
    )

    private fun isValid(file: File, expectedSize: Long?, expectedSha256: String?): Boolean {
        if (expectedSize != null && file.length() != expectedSize) return false
        if (expectedSha256 != null && sha256Of(file) != expectedSha256) return false
        return true
    }

    fun sha256Of(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(BUFFER_BYTES)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                digest.update(buffer, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun contentRangeStart(header: String?): Long? {
        val m = Regex("""^bytes\s+(\d+)-\d+/(\d+|\*)$""").matchEntire(header?.trim().orEmpty()) ?: return null
        return m.groupValues[1].toLongOrNull()
    }

    private fun isTransient(code: Int): Boolean = code == 408 || code == 429 || code >= 500

    private fun httpFailure(code: Int): UpdateFailure = when (code) {
        404 -> UpdateFailure(FailureKind.SERVER, "Berkas APK tidak ditemukan di GitHub (kode 404). Mungkin rilisnya sudah dihapus.")
        403 -> UpdateFailure(FailureKind.SERVER, "GitHub menolak unduhan (kode 403). Coba lagi nanti.")
        else -> UpdateFailure(FailureKind.SERVER, "GitHub membalas kode $code saat mengunduh.")
    }

    private fun megabytes(bytes: Long): Long = (bytes + 1024 * 1024 - 1) / (1024 * 1024)

    private fun sleepBackoff(attempt: Int, token: CancelToken) {
        val waitMs = 1000L * attempt
        val end = System.nanoTime() + waitMs * 1_000_000L
        while (System.nanoTime() < end) {
            token.throwIfCancelled()
            try {
                Thread.sleep(100)
            } catch (_: InterruptedException) {
                throw CancellationException("Unduhan dibatalkan")
            }
        }
    }
}

object UpdateInstaller {
    const val ACTION_RESULT = "com.aistudio.novacraft.launcher.UPDATE_INSTALL_RESULT"

    private const val UPDATES_DIR = "updates"
    private const val SESSION_FILE_NAME = "NovaCraft.apk"

    fun updatesDir(context: Context): File = File(context.cacheDir, UPDATES_DIR)

    fun apkFile(context: Context, assetName: String): File {
        val safe = assetName.replace(Regex("[^A-Za-z0-9._-]"), "_").ifEmpty { "update.apk" }
        return File(updatesDir(context), safe)
    }

    fun cleanup(context: Context, installed: AppVersion) {
        val dir = updatesDir(context)
        val files = dir.listFiles() ?: return
        val pattern = Regex("""^(?:NovaCraft|AmberLauncher)-(.+?)(?:-(?:32|64)bit[A-Za-z0-9_-]*)?\.apk(\.part)?$""")
        val now = System.currentTimeMillis()
        for (f in files) {
            if (!f.isFile) continue
            val m = pattern.matchEntire(f.name)
            val version = m?.let { AppVersion.parse(it.groupValues[1]) }
            val stalePart = f.name.endsWith(".part") && now - f.lastModified() > 14L * 24 * 3600 * 1000
            if (version == null || version <= installed || stalePart) {
                try {
                    f.delete()
                } catch (_: Exception) {
                }
            }
        }
    }

    fun isGameRunning(context: Context): Boolean = try {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val name = context.packageName + ":game"
        am.runningAppProcesses?.any { it.processName == name } == true
    } catch (_: Throwable) {
        false
    }

    fun canInstall(context: Context): Boolean = try {
        context.packageManager.canRequestPackageInstalls()
    } catch (_: Throwable) {
        false
    }

    fun openInstallPermissionSettings(context: Context): Boolean {
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
        if (findActivity(context) == null) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun findActivity(context: Context): android.app.Activity? {
        var c: Context? = context
        while (c is ContextWrapper) {
            if (c is android.app.Activity) return c
            c = c.baseContext
        }
        return null
    }

    fun verifyApk(context: Context, apk: File): UpdateFailure? {
        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }
        val archive: PackageInfo = try {
            pm.getPackageArchiveInfo(apk.absolutePath, flags)
        } catch (_: Throwable) {
            null
        } ?: return UpdateFailure(
            FailureKind.CORRUPT_FILE,
            "Berkas pembaruan tidak bisa dibaca sebagai APK. Ketuk Coba lagi untuk mengunduh ulang."
        )

        if (archive.packageName != context.packageName) {
            return UpdateFailure(FailureKind.WRONG_APP, "Berkas ini bukan APK NovaCraft.")
        }

        val installed: PackageInfo = try {
            pm.getPackageInfo(context.packageName, flags)
        } catch (_: Throwable) {
            return null
        }
        if (PackageInfoCompat.getLongVersionCode(archive) <= PackageInfoCompat.getLongVersionCode(installed)) {
            return UpdateFailure(FailureKind.NOT_NEWER, "APK ini tidak lebih baru dari versi yang terpasang.")
        }

        val newCerts = certificateHashes(archive)
        val oldCerts = certificateHashes(installed)
        if (newCerts.isNotEmpty() && oldCerts.isNotEmpty() && newCerts != oldCerts) {
            return UpdateFailure(
                FailureKind.WRONG_SIGNATURE,
                "Tanda tangan APK berbeda dari aplikasi yang terpasang, jadi Android akan menolak pembaruan ini."
            )
        }
        return null
    }

    private fun certificateHashes(info: PackageInfo): Set<String> {
        val signatures: Array<Signature>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signing = info.signingInfo
            when {
                signing == null -> null
                signing.hasMultipleSigners() -> signing.apkContentsSigners
                else -> signing.signingCertificateHistory
            }
        } else {
            @Suppress("DEPRECATION")
            info.signatures
        }
        if (signatures == null) return emptySet()
        return signatures.mapTo(HashSet()) { sig ->
            MessageDigest.getInstance("SHA-256").digest(sig.toByteArray()).joinToString("") { "%02x".format(it) }
        }
    }

    fun startInstall(context: Context, apk: File) {
        val installer = context.packageManager.packageInstaller

        try {
            for (s in installer.mySessions) {
                try {
                    installer.abandonSession(s.sessionId)
                } catch (_: Exception) {
                }
            }
        } catch (_: Exception) {
        }

        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            setSize(apk.length())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
        }

        val sessionId = installer.createSession(params)
        val session = installer.openSession(sessionId)
        try {
            session.openWrite(SESSION_FILE_NAME, 0, apk.length()).use { out ->
                apk.inputStream().use { input -> input.copyTo(out, 256 * 1024) }
                session.fsync(out)
            }
            val callback = Intent(context, UpdateInstallReceiver::class.java).setAction(ACTION_RESULT)
            var flags = PendingIntent.FLAG_UPDATE_CURRENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) flags = flags or PendingIntent.FLAG_MUTABLE
            val pending = PendingIntent.getBroadcast(context, sessionId, callback, flags)
            session.commit(pending.intentSender)
        } catch (t: Throwable) {
            try {
                session.abandon()
            } catch (_: Exception) {
            }
            throw t
        } finally {
            session.close()
        }
    }

    fun failureForStatus(status: Int, detail: String?): UpdateFailure {
        val extra = detail?.takeIf { it.isNotBlank() }?.let { " ($it)" }.orEmpty()
        return when (status) {
            PackageInstaller.STATUS_FAILURE_BLOCKED ->
                UpdateFailure(FailureKind.INSTALL_BLOCKED, "Pemasangan diblokir oleh sistem atau Play Protect.$extra")
            PackageInstaller.STATUS_FAILURE_CONFLICT ->
                UpdateFailure(
                    FailureKind.WRONG_SIGNATURE,
                    "Android menolak pembaruan ini: tanda tangan atau versinya tidak cocok dengan aplikasi yang terpasang.$extra"
                )
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE ->
                UpdateFailure(FailureKind.INSTALL_FAILED, "APK ini tidak cocok dengan HP kamu.$extra")
            PackageInstaller.STATUS_FAILURE_INVALID ->
                UpdateFailure(FailureKind.CORRUPT_FILE, "APK rusak atau tidak valid. Ketuk Coba lagi untuk mengunduh ulang.$extra")
            PackageInstaller.STATUS_FAILURE_STORAGE ->
                UpdateFailure(FailureKind.STORAGE_FULL, "Ruang penyimpanan tidak cukup untuk memasang pembaruan.$extra")
            else ->
                UpdateFailure(FailureKind.INSTALL_FAILED, "Pemasangan gagal.$extra")
        }
    }
}

// ----------------------------------------------------------------------------
// Section: core/update/UpdateManager.kt
// ----------------------------------------------------------------------------

/**
 * Pusat pembaruan aplikasi (terputus dari GitHub atas permintaan pengguna).
 * Seluruh pemeriksaan otomatis, koneksi GitHub, dan pengunduhan pembaruan dinonaktifkan.
 */
object UpdateManager {
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private val _promptOpen = MutableStateFlow(false)
    val promptOpen: StateFlow<Boolean> = _promptOpen.asStateFlow()

    fun onForeground(context: Context) {
        _promptOpen.value = false
        _state.value = UpdateState.Idle
    }

    fun onBackground() = Unit

    fun checkNow(context: Context) {
        _promptOpen.value = false
        _state.value = UpdateState.Idle
    }

    fun openPrompt() {
        _promptOpen.value = false
    }

    fun dismissPrompt() {
        _promptOpen.value = false
    }

    fun skipVersion(context: Context) {
        _promptOpen.value = false
    }

    fun releasePageUrl(): String = ""

    fun startDownload(context: Context) {
        _promptOpen.value = false
    }

    fun cancelDownload() {
        _promptOpen.value = false
        _state.value = UpdateState.Idle
    }

    fun install(context: Context) {
        _promptOpen.value = false
    }

    fun openInstallPermissionSettings(context: Context): Boolean = false

    @Suppress("UNUSED_PARAMETER")
    fun onInstallResult(context: Context, status: Int, message: String?) {
        _promptOpen.value = false
        _state.value = UpdateState.Idle
    }
}
