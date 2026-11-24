package com.aistudio.novacraft.launcher.core.utils

import android.app.Activity
import android.app.ActivityManager
import android.app.Service
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.hardware.input.InputManager
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.os.PowerManager
import android.os.Process
import android.os.SystemClock
import android.system.Os
import android.system.OsConstants
import android.view.Display
import android.view.InputDevice
import android.view.WindowManager
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import com.aistudio.novacraft.launcher.VulkanCheckService
import com.aistudio.novacraft.launcher.core.account.AccountManager
import com.aistudio.novacraft.launcher.core.launch.InstanceManager
import com.aistudio.novacraft.launcher.core.mods.HorizonContent
import com.aistudio.novacraft.launcher.core.mods.HorizonEngine
import com.aistudio.novacraft.launcher.core.launch.NuxRendererInfo
import com.aistudio.novacraft.launcher.core.launch.NuxRendererRegistry
import com.aistudio.novacraft.launcher.core.launch.UpdateChecker
import com.aistudio.novacraft.launcher.ui.theme.Toast
import com.movtery.zalithlauncher.bridge.LoggerBridge
import com.movtery.zalithlauncher.utils.device.VulkanChecker
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FilterInputStream
import java.io.InputStream
import java.io.PrintWriter
import java.io.RandomAccessFile
import java.io.StringWriter
import java.io.Writer
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URI
import java.net.URL
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Properties
import java.util.zip.ZipInputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Dns
import org.json.JSONArray
import org.json.JSONObject

private typealias Log = LauncherLog

// ============================================================================
// Section: NovaCoreSupport.kt
// ============================================================================

// ----------------------------------------------------------------------------
// Section: core/utils/NovaCoreUtils.kt
// ----------------------------------------------------------------------------

// ============================================================================
// NovaCraft Unified Core Utilities: AtomicFiles, SafePaths & NuxVersionUtils
// ============================================================================

/**
 * Menulis berkas lewat berkas sementara lalu memindahkannya ke tujuan agar berkas lama
 * tidak pernah tertinggal setengah tertulis bila proses dimatikan sistem.
 */
object AtomicFiles {
    fun writeText(target: File, text: String) {
        val tmp = File(target.parentFile, target.name + ".tmp")
        FileOutputStream(tmp).use { out ->
            out.write(text.toByteArray(Charsets.UTF_8))
            out.flush()
            try { out.fd.sync() } catch (_: Exception) {}
        }
        if (!tmp.renameTo(target)) {
            target.writeText(text)
            tmp.delete()
        }
    }
}

/**
 * Pelindung path dan alamat unduhan agar nama berkas / path relatif dari luar
 * tidak bisa keluar dari folder instance atau mengunduh dari host tidak terpercaya.
 */
object SafePaths {

    fun fileName(raw: String?): String {
        val base = (raw ?: "").replace('\\', '/').substringAfterLast('/')
        val cleaned = base
            .filter { it >= ' ' && it != '\u007f' && it !in "<>:\"|?*" }
            .trim()
            .trim('.')
            .trim()
        return if (cleaned.isEmpty()) "addon_file" else cleaned.takeLast(150)
    }

    fun isSafeRelative(rel: String?): Boolean {
        if (rel.isNullOrBlank() || rel.indexOf('\u0000') >= 0) return false
        val normalized = rel.replace('\\', '/')
        if (normalized.startsWith("/")) return false
        return normalized.split('/').none { it == ".." }
    }

    fun resolveInside(base: File, rel: String?): File? {
        if (rel.isNullOrBlank() || !isSafeRelative(rel)) return null
        return try {
            val baseCanon = base.canonicalFile
            val target = File(baseCanon, rel.replace('\\', '/')).canonicalFile
            if (target.path.startsWith(baseCanon.path + File.separator)) target else null
        } catch (_: Exception) {
            null
        }
    }

    private val MODPACK_HOSTS = setOf(
        "cdn.modrinth.com",
        "github.com",
        "raw.githubusercontent.com",
        "gitlab.com",
        "edge.forgecdn.net",
        "mediafilez.forgecdn.net",
        "media.forgecdn.net",
        "www.curseforge.com"
    )

    fun isAllowedModpackDownloadUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return try {
            val uri = URI(url.trim())
            val host = uri.host?.lowercase() ?: return false
            uri.scheme.equals("https", ignoreCase = true) && host in MODPACK_HOSTS
        } catch (_: Exception) {
            false
        }
    }
}

object NuxVersionUtils {

    fun parseVersionParts(version: String): List<Int> {
        if (version.isBlank()) return emptyList()

        val snapshotMatch = Regex("""^(\d{2})w(\d{2})[a-z]?""", RegexOption.IGNORE_CASE).find(version.trim())
        if (snapshotMatch != null) {
            val year = snapshotMatch.groupValues[1].toIntOrNull() ?: 24
            val week = snapshotMatch.groupValues[2].toIntOrNull() ?: 1
            if (year >= 26) {
                return listOf(year, 0, 0, week)
            }
            val derivedMinor = when (year) {
                25 -> 21
                24 -> 21
                23 -> 20
                22 -> 19
                21 -> 18
                else -> 17
            }
            return listOf(1, derivedMinor, 0, week)
        }

        val clean = version.trim().split("-", "_", " ").first()
        val parts = clean.split(".")
        val result = mutableListOf<Int>()
        for (part in parts) {
            val digits = part.filter { it.isDigit() }
            digits.toIntOrNull()?.let { result.add(it) }
        }
        return result
    }

    fun compare(v1: String, v2: String): Int {
        val s1 = v1.trim()
        val s2 = v2.trim()
        val p1 = parseVersionParts(s1)
        val p2 = parseVersionParts(s2)

        val maxLen = maxOf(p1.size, p2.size)
        for (i in 0 until maxLen) {
            val n1 = p1.getOrElse(i) { 0 }
            val n2 = p2.getOrElse(i) { 0 }
            if (n1 != n2) {
                return n1.compareTo(n2)
            }
        }

        val hasDash1 = s1.contains("-")
        val hasDash2 = s2.contains("-")
        if (!hasDash1 && hasDash2) return 1
        if (hasDash1 && !hasDash2) return -1

        if (hasDash1 && hasDash2) {
            val snap1 = Regex("""snapshot-(\d+)""", RegexOption.IGNORE_CASE).find(s1)?.groupValues?.get(1)?.toIntOrNull()
            val snap2 = Regex("""snapshot-(\d+)""", RegexOption.IGNORE_CASE).find(s2)?.groupValues?.get(1)?.toIntOrNull()
            if (snap1 != null && snap2 != null && snap1 != snap2) {
                return snap1.compareTo(snap2)
            }
        }

        return 0
    }

    fun isLowerVer(v1: String, v2: String): Boolean = compare(v1, v2) < 0

    fun isBiggerVer(v1: String, v2: String): Boolean = compare(v1, v2) > 0

    fun isRendererSupported(renderer: NuxRendererInfo, mcVersion: String): Boolean {
        if (renderer.id.equals("auto", ignoreCase = true)) return true
        if (mcVersion.isBlank()) return true

        val minVer = renderer.minMCVersion
        if (!minVer.isNullOrBlank() && isLowerVer(mcVersion, minVer)) {
            return false
        }

        val maxVer = renderer.maxMCVersion
        if (!maxVer.isNullOrBlank() && isBiggerVer(mcVersion, maxVer)) {
            return false
        }

        return true
    }

    fun isAdrenoGPU(): Boolean {
        val hardware = Build.HARDWARE.lowercase()
        val board = Build.BOARD.lowercase()
        val soc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MANUFACTURER.lowercase()
        } else ""

        return hardware.contains("qcom") || hardware.contains("qualcomm") ||
            board.contains("qcom") || board.contains("msm") || board.contains("sm") ||
            soc.contains("qualcomm") || soc.contains("qcom")
    }
}

// ----------------------------------------------------------------------------
// Section: core/background/HomeVideo.kt
// ----------------------------------------------------------------------------

/** Hasil memasang berkas pilihan pengguna sebagai latar Beranda. */
enum class BgImportResult { IMAGE, VIDEO, TOO_BIG, UNSUPPORTED }

/**
 * Menyimpan video latar Beranda. Video disalin ke penyimpanan aplikasi bersama satu bingkai diam
 * (poster) yang dipakai sebagai gambar cadangan: tampil selagi video disiapkan, saat animasi
 * dimatikan, dan bila HP ternyata tidak bisa memutar videonya.
 */
object HomeVideo {
    const val FILE_NAME = "home_bg_video"

    /** Batas ukuran berkas video (200 MB) dan sisi terpanjang resolusinya. */
    const val MAX_BYTES = 200L * 1024 * 1024
    private const val MAX_LONG_SIDE = 2560
    private const val POSTER_MAX_SIDE = 1280

    fun file(context: Context): File = File(context.filesDir, FILE_NAME)

    /**
     * Menyalin video, memastikan HP punya decoder-nya, lalu menyimpan satu bingkai sebagai
     * [posterTarget] (JPEG). Berkas lama baru diganti setelah semuanya berhasil.
     */
    fun importVideo(context: Context, uri: Uri, videoTarget: File, posterTarget: File): BgImportResult {
        val dir = videoTarget.parentFile ?: return BgImportResult.UNSUPPORTED
        val tmp = File(dir, "$FILE_NAME.tmp")
        val posterTmp = File(dir, "${posterTarget.name}.tmp")
        val retriever = MediaMetadataRetriever()
        try {
            val input = context.contentResolver.openInputStream(uri) ?: return BgImportResult.UNSUPPORTED
            var total = 0L
            input.use { ins ->
                tmp.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = ins.read(buf)
                        if (n < 0) break
                        total += n
                        if (total > MAX_BYTES) return BgImportResult.TOO_BIG
                        out.write(buf, 0, n)
                    }
                }
            }

            if (!hasDecoder(tmp)) return BgImportResult.UNSUPPORTED

            retriever.setDataSource(tmp.absolutePath)
            if (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) != "yes") {
                return BgImportResult.UNSUPPORTED
            }
            val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            if (maxOf(w, h) > MAX_LONG_SIDE) return BgImportResult.TOO_BIG

            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            val atUs = if (durationMs > 4000L) 1_000_000L else 0L
            val frame = retriever.getFrameAtTime(atUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: return BgImportResult.UNSUPPORTED

            val longSide = maxOf(frame.width, frame.height)
            val poster = if (longSide > POSTER_MAX_SIDE) {
                val scale = POSTER_MAX_SIDE.toFloat() / longSide
                Bitmap.createScaledBitmap(
                    frame,
                    maxOf(1, (frame.width * scale).toInt()),
                    maxOf(1, (frame.height * scale).toInt()),
                    true
                )
            } else {
                frame
            }
            posterTmp.outputStream().use { poster.compress(Bitmap.CompressFormat.JPEG, 88, it) }
            if (poster !== frame) poster.recycle()
            frame.recycle()

            if (posterTarget.exists()) posterTarget.delete()
            if (!posterTmp.renameTo(posterTarget)) return BgImportResult.UNSUPPORTED
            if (videoTarget.exists()) videoTarget.delete()
            return if (tmp.renameTo(videoTarget)) BgImportResult.VIDEO else BgImportResult.UNSUPPORTED
        } catch (e: Throwable) {
            return BgImportResult.UNSUPPORTED
        } finally {
            try {
                retriever.release()
            } catch (_: Throwable) {
            }
            tmp.delete()
            posterTmp.delete()
        }
    }

    /** True bila HP punya decoder untuk trek video di berkas ini (mis. H.264 hampir selalu ada). */
    private fun hasDecoder(file: File): Boolean {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(file.absolutePath)
            var found = false
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/")) {
                    // Format baru hanya berisi jenis dan ukuran: findDecoderForFormat menolak
                    // format yang membawa laju bingkai pada Android lama.
                    val w = if (format.containsKey(MediaFormat.KEY_WIDTH)) format.getInteger(MediaFormat.KEY_WIDTH) else 1280
                    val h = if (format.containsKey(MediaFormat.KEY_HEIGHT)) format.getInteger(MediaFormat.KEY_HEIGHT) else 720
                    val probe = MediaFormat.createVideoFormat(mime, w, h)
                    found = MediaCodecList(MediaCodecList.REGULAR_CODECS).findDecoderForFormat(probe) != null
                    break
                }
            }
            found
        } catch (e: Throwable) {
            false
        } finally {
            try {
                extractor.release()
            } catch (_: Throwable) {
            }
        }
    }
}

// ----------------------------------------------------------------------------
// Section: core/device/NovaDeviceProfileCore.kt
// ----------------------------------------------------------------------------

// ============================================================================
// NovaCraft Unified Device Profile, Refresh Rate & Physical Mouse Detection
// ============================================================================

private const val MOUSE_CHECKER_TAG = "PhysicalMouseChecker"

object PhysicalMouseChecker {
    var physicalMouseConnected: Boolean = false
        private set

    private var isInitialized = false

    fun initChecker(activity: Activity) {
        if (isInitialized) {
            physicalMouseConnected = isPhysicalMouseConnected()
            return
        }
        isInitialized = true

        physicalMouseConnected = isPhysicalMouseConnected()
        Log.i(MOUSE_CHECKER_TAG, "Initialization complete, physical mouse connection status: $physicalMouseConnected")

        val listener = object : InputManager.InputDeviceListener {
            override fun onInputDeviceAdded(deviceId: Int) {
                if (deviceId.isMouseId()) {
                    Log.i(MOUSE_CHECKER_TAG, "Physical mouse connected, deviceId: $deviceId")
                    physicalMouseConnected = true
                }
            }

            override fun onInputDeviceRemoved(deviceId: Int) {
                if (deviceId.isMouseId()) {
                    Log.i(MOUSE_CHECKER_TAG, "Physical mouse disconnected, deviceId: $deviceId")
                    physicalMouseConnected = false
                } else {
                    physicalMouseConnected = isPhysicalMouseConnected()
                    Log.i(MOUSE_CHECKER_TAG, "Fallback check for physical mouse connection status: $physicalMouseConnected")
                }
            }

            override fun onInputDeviceChanged(deviceId: Int) {
                physicalMouseConnected = isPhysicalMouseConnected()
            }
        }

        try {
            val inputManager = activity.getSystemService(Context.INPUT_SERVICE) as? InputManager
            inputManager?.registerInputDeviceListener(listener, null)
        } catch (e: Throwable) {
            Log.w(MOUSE_CHECKER_TAG, "Failed to register InputDeviceListener", e)
        }
    }
}

private fun Int.isMouseId(): Boolean {
    return InputDevice.getDevice(this)?.let { device ->
        device.sources and InputDevice.SOURCE_MOUSE == InputDevice.SOURCE_MOUSE
    } ?: false
}

private fun isPhysicalMouseConnected(): Boolean {
    return try {
        InputDevice.getDeviceIds()
            .takeIf { it.isNotEmpty() }
            ?.any { it.isMouseId() }
            ?: false
    } catch (_: Throwable) {
        false
    }
}

object DeviceProfiles {
    const val GC_SERIAL = "serial"
    const val GC_DEFAULT = "default"
    const val GC_AUTO = "auto"
    const val GENERAL_LOCK_HZ = 60f

    private const val HIGH_REFRESH_MIN_HZ = 85f
    private const val SERIAL_CODE_CACHE_HEAP_LIMIT_MB = 1280
    private const val SERIAL_CODE_CACHE_FLAG = "-XX:ReservedCodeCacheSize=48M"
    private const val LEGACY_INITIAL_HEAP_MB = 256
    private const val G1_MAX_YOUNG_PERCENT = 30
    private const val GENERAL_YOUNG_CAP_RAM_MB = 6000

    fun describeDevice(): String {
        return try {
            "${Build.MANUFACTURER} ${Build.MODEL} (product=${Build.PRODUCT}, board=${Build.BOARD})"
        } catch (_: Throwable) {
            "tidak diketahui"
        }
    }

    fun isSmallRamPhone(totalRamMb: Int): Boolean = totalRamMb < GENERAL_YOUNG_CAP_RAM_MB

    fun normalizeGeneralGcMode(mode: String?): String {
        return when (mode) {
            GC_SERIAL -> GC_SERIAL
            GC_DEFAULT -> GC_DEFAULT
            else -> GC_AUTO
        }
    }

    fun autoInitialHeapMb(requestedInitMb: Int, maxMb: Int): Int {
        return if (requestedInitMb <= LEGACY_INITIAL_HEAP_MB) maxMb else minOf(requestedInitMb, maxMb)
    }

    fun generalInitialHeapMb(requestedInitMb: Int, maxMb: Int, isJava8: Boolean): Int {
        if (isJava8) return minOf(requestedInitMb, maxMb)
        return autoInitialHeapMb(requestedInitMb, maxMb)
    }

    fun generalJvmFlags(totalRamMb: Int, isJava8: Boolean, userArgs: String, gcMode: String = GC_AUTO): List<String> {
        if (userSelectsCollector(userArgs)) return emptyList()
        return when (normalizeGeneralGcMode(gcMode)) {
            GC_DEFAULT -> emptyList()
            GC_SERIAL -> {
                if (totalRamMb >= GENERAL_YOUNG_CAP_RAM_MB) emptyList() else serialFlags()
            }
            else -> {
                if (isJava8) return emptyList()
                if (totalRamMb >= GENERAL_YOUNG_CAP_RAM_MB) return emptyList()
                listOf(
                    "-XX:+IgnoreUnrecognizedVMOptions",
                    "-XX:+UnlockExperimentalVMOptions",
                    "-XX:G1MaxNewSizePercent=$G1_MAX_YOUNG_PERCENT"
                )
            }
        }
    }

    private fun serialFlags(): List<String> = listOf(
        "-XX:+IgnoreUnrecognizedVMOptions",
        "-XX:+UseSerialGC",
        "-XX:NewRatio=3"
    )

    fun serialCodeCacheFlags(serialActive: Boolean, heapMaxMb: Int, userArgs: String): List<String> {
        if (!serialActive) return emptyList()
        if (heapMaxMb > SERIAL_CODE_CACHE_HEAP_LIMIT_MB) return emptyList()
        if (userArgs.contains("ReservedCodeCacheSize")) return emptyList()
        return listOf(SERIAL_CODE_CACHE_FLAG)
    }

    fun flagsSelectSerial(flags: List<String>): Boolean = flags.contains("-XX:+UseSerialGC")

    private fun userSelectsCollector(userArgs: String): Boolean {
        if (userArgs.isBlank()) return false
        return userArgs.split(" ").any { raw ->
            val token = raw.trim()
            token.startsWith("-XX:+Use") && token.endsWith("GC")
        }
    }

    @Suppress("DEPRECATION")
    private fun displayOf(context: Context): Display? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.display
            } else {
                (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay
            }
        } catch (_: Throwable) {
            null
        }
    }

    fun findDisplayModeId(context: Context, targetHz: Float): Int {
        val display = displayOf(context) ?: return 0
        return try {
            val current = display.mode
            var bestId = 0
            var bestDiff = Float.MAX_VALUE
            for (mode in display.supportedModes) {
                if (mode.physicalWidth != current.physicalWidth || mode.physicalHeight != current.physicalHeight) continue
                val diff = abs(mode.refreshRate - targetHz)
                if (diff < 1.0f && diff < bestDiff) {
                    bestId = mode.modeId
                    bestDiff = diff
                }
            }
            bestId
        } catch (_: Throwable) {
            0
        }
    }

    fun supportsHighRefresh(context: Context): Boolean {
        val display = displayOf(context) ?: return false
        return try {
            display.supportedModes.any { it.refreshRate >= HIGH_REFRESH_MIN_HZ }
        } catch (_: Throwable) {
            false
        }
    }

    fun refreshLockTarget(settings: LauncherSettings): Float {
        return if (settings.lockRefresh60) GENERAL_LOCK_HZ else 0f
    }

    fun refreshRateLabel(context: Context, generalLock: Boolean = false): String {
        if (!generalLock) return ""
        val display = displayOf(context) ?: return ""
        return try {
            "${display.refreshRate.roundToInt()}Hz"
        } catch (_: Throwable) {
            ""
        }
    }
}

// ----------------------------------------------------------------------------
// Section: core/game/MCOptions.kt
// ----------------------------------------------------------------------------

private const val TAG = "MCOptions"

object MCOptions {
    private val lock = Any()
    private val parameterMap = ConcurrentHashMap<String, String>()
    private var optionsFile: File? = null

    fun setup(context: Context, gameDir: File) {
        synchronized(lock) {
            parameterMap.clear()
            val target = File(gameDir, "options.txt")
            optionsFile = target

            target.parentFile?.takeIf { !it.exists() }?.mkdirs()
            if (!target.exists()) {
                createWithDefaults(context, target)
            }

            loadInternal(target)
        }
    }

    private fun createWithDefaults(context: Context, target: File) {
        runCatching {
            context.assets.open("game/options.txt").use { input ->
                FileOutputStream(target).use { output ->
                    input.copyTo(output)
                }
            }
            Log.d(TAG, "Successfully extracted default options.txt to ${target.absolutePath}")
        }.onFailure {
            Log.w(TAG, "Failed to unpack options.txt from assets: ${it.message}")
        }
    }

    private fun loadInternal(file: File) {
        if (!file.exists()) return
        runCatching {
            val newMap = file.readLines()
                .mapNotNull { line ->
                    val trimmed = line.trim()
                    val idx = trimmed.indexOf(':')
                    if (idx > 0) {
                        trimmed.substring(0, idx).trim() to trimmed.substring(idx + 1).trim()
                    } else null
                }.toMap()

            parameterMap.clear()
            parameterMap.putAll(newMap)
            Log.d(TAG, "Loaded ${parameterMap.size} options from ${file.name}")
        }.onFailure {
            Log.w(TAG, "Failed to parse options.txt: ${it.message}")
        }
    }

    fun get(key: String): String? = parameterMap[key]

    fun set(key: String, value: String) {
        parameterMap[key] = value
    }

    fun containsKey(key: String): Boolean = parameterMap.containsKey(key)

    fun save() {
        val file = optionsFile ?: return
        synchronized(lock) {
            val tempFile = File(file.parentFile, "${file.name}.tmp")
            runCatching {
                tempFile.bufferedWriter().use { writer ->
                    parameterMap.forEach { (k, v) ->
                        writer.write("$k:$v")
                        writer.newLine()
                    }
                }
                if (file.exists()) file.delete()
                tempFile.renameTo(file)
                Log.d(TAG, "Saved options.txt successfully (${parameterMap.size} keys)")
            }.onFailure {
                Log.e(TAG, "Failed to save options.txt: ${it.message}")
                tempFile.delete()
            }
        }
    }
}

// ----------------------------------------------------------------------------
// Section: core/network/NovaNetworkCore.kt
// ----------------------------------------------------------------------------

// ============================================================================
// NovaCraft Unified Network Core: LimitedInputStream & NuxDns (DoH Fallback)
// ============================================================================

/**
 * Membatasi total byte yang boleh dibaca dari satu koneksi server lokal agar kiriman
 * tanpa akhir tidak menghabiskan memori.
 */
class LimitedInputStream(input: InputStream, private var remaining: Long) : FilterInputStream(input) {

    override fun read(): Int {
        if (remaining <= 0L) return -1
        val value = super.read()
        if (value >= 0) remaining--
        return value
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (remaining <= 0L) return -1
        val count = super.read(buffer, offset, minOf(length.toLong(), remaining).toInt())
        if (count > 0) remaining -= count.toLong()
        return count
    }
}

object NuxDns : Dns {
    private const val TAG = "NuxDns"
    private const val WARN_INTERVAL_MS = 30_000L
    private val lastWarn = ConcurrentHashMap<String, Long>()

    private fun warnLimited(key: String, msg: String) {
        val now = System.currentTimeMillis()
        val prev = lastWarn[key]
        if (prev == null || now - prev >= WARN_INTERVAL_MS) {
            lastWarn[key] = now
            LauncherLog.w(TAG, msg)
        } else {
            Log.w(TAG, msg)
        }
    }

    private val MODRINTH_IPS = listOf(
        byteArrayOf(104.toByte(), 18.toByte(), 23.toByte(), 35.toByte()),
        byteArrayOf(104.toByte(), 18.toByte(), 22.toByte(), 35.toByte()),
        byteArrayOf(172.toByte(), 67.toByte(), 182.toByte(), 196.toByte())
    )

    private val FABRIC_IPS = listOf(
        byteArrayOf(104.toByte(), 21.toByte(), 33.toByte(), 240.toByte()),
        byteArrayOf(172.toByte(), 67.toByte(), 151.toByte(), 177.toByte())
    )

    override fun lookup(hostname: String): List<InetAddress> {
        try {
            val systemAddresses = Dns.SYSTEM.lookup(hostname)
            if (systemAddresses.isNotEmpty()) {
                return systemAddresses
            }
        } catch (e: Exception) {
            warnLimited("sys:$hostname", "System DNS lookup failed for $hostname: ${e.message}, falling back")
        }

        if (hostname.equals("api.modrinth.com", ignoreCase = true) ||
            hostname.equals("cdn.modrinth.com", ignoreCase = true) ||
            hostname.endsWith(".modrinth.com", ignoreCase = true)
        ) {
            Log.i(TAG, "Using direct Cloudflare anycast IPs for Modrinth host: $hostname")
            return MODRINTH_IPS.map { ipBytes ->
                InetAddress.getByAddress(hostname, ipBytes)
            }
        }

        if (hostname.endsWith("fabricmc.net", ignoreCase = true)) {
            Log.i(TAG, "Using direct Cloudflare anycast IPs for Fabric host: $hostname")
            return FABRIC_IPS.map { ipBytes ->
                InetAddress.getByAddress(hostname, ipBytes)
            }
        }

        try {
            val dohAddresses = queryCloudflareDoh(hostname)
            if (dohAddresses.isNotEmpty()) {
                Log.i(TAG, "Resolved $hostname via Cloudflare DoH: $dohAddresses")
                return dohAddresses
            }
        } catch (e: Exception) {
            warnLimited("doh:$hostname", "DoH lookup failed for $hostname: ${e.message}")
        }

        throw UnknownHostException("Unable to resolve host \"$hostname\": System DNS and Fallback DoH failed")
    }

    private fun queryCloudflareDoh(hostname: String): List<InetAddress> {
        val encoded = java.net.URLEncoder.encode(hostname, "UTF-8")
        val url = URL("https://1.1.1.1/dns-query?name=$encoded&type=A")
        val conn = url.openConnection() as HttpURLConnection
        return try {
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            conn.setRequestProperty("Accept", "application/dns-json")
            conn.setRequestProperty("User-Agent", "NovaCraftLauncher/2.3")

            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JsonParser.parseString(responseText).asJsonObject
                if (json.has("Answer")) {
                    val answers = json.getAsJsonArray("Answer")
                    val result = mutableListOf<InetAddress>()
                    for (i in 0 until answers.size()) {
                        val ansObj = answers[i].asJsonObject
                        val type = ansObj.get("type")?.asInt ?: 0
                        if (type == 1) {
                            val ipStr = ansObj.get("data")?.asString ?: continue
                            val parts = ipStr.split(".")
                            if (parts.size == 4) {
                                val bytes = ByteArray(4) { idx -> parts[idx].toInt().toByte() }
                                result.add(InetAddress.getByAddress(hostname, bytes))
                            }
                        }
                    }
                    if (result.isNotEmpty()) return result
                }
            }
            emptyList()
        } finally {
            conn.disconnect()
        }
    }
}

// ----------------------------------------------------------------------------
// Section: core/perf/GamePerfMonitor.kt
// ----------------------------------------------------------------------------

/** Selang antar laporan kinerja di log (milidetik). */
private const val REPORT_INTERVAL_MS = 30_000L

/** Selang mencari thread render saat game masih memuat (milidetik). */
private const val FIND_INTERVAL_MS = 1_000L

/** Batas waktu menunggu thread render muncul (milidetik); lewat dari ini pemantau berhenti. */
private const val FIND_TIMEOUT_MS = 300_000L

/** Maksimal byte log GC yang dibaca sekali jalan. */
private const val MAX_GC_READ_BYTES = 256 * 1024

/** Tanda awal baris log, sama dengan baris "[JVM]" dan "[Profil]" milik launcher. */
private const val LOG_PREFIX = "\u25B7 [Kinerja] "

/**
 * Pemantau kinerja game yang hanya MENCATAT ke log. Pemantau tidak mengubah prioritas, heap,
 * atau apa pun pada game, jadi tidak bisa menurunkan FPS selain beberapa pembacaan berkas kecil
 * di /proc tiap 30 detik, dari thread sendiri yang berprioritas rendah.
 *
 * Satu baris laporan berisi: pemakaian CPU thread render, berapa lama thread itu menunggu giliran
 * CPU (tanda CPU berebut dengan thread lain), pemakaian CPU seluruh proses, ringkasan jeda GC dari
 * log GC JVM, RAM proses, dan suhu HP (Android 10 ke atas; HP yang terlalu panas menurunkan
 * kecepatan prosesornya sendiri). Dengan angka ini penyebab FPS tidak stabil bisa dibaca dari log
 * hasil uji di HP, bukan ditebak.
 *
 * @param context dipakai untuk membaca status suhu HP.
 * @param gcLogFile berkas log GC yang ditulis JVM lewat -Xlog:gc, atau null bila tidak dipakai
 *   (Java 8 tidak mendukung -Xlog).
 */
class GamePerfMonitor(context: Context, private val gcLogFile: File?) {

    private val appContext: Context = context.applicationContext

    @Volatile
    private var running = false
    private var worker: Thread? = null
    private var gcOffset = 0L
    private val ticksPerSecond: Long = readTicksPerSecond()

    /** Mulai memantau di thread latar. Aman dipanggil berulang. */
    fun start() {
        if (running) return
        running = true
        val t = Thread(Runnable { monitorLoop() }, "Amber-PerfMonitor")
        t.isDaemon = true
        worker = t
        t.start()
    }

    /** Hentikan pemantauan. */
    fun stop() {
        running = false
        worker?.interrupt()
        worker = null
    }

    private class Sample(
        val timeMs: Long,
        /** Waktu CPU thread render (utime + stime) dalam tick. -1 bila thread sudah tidak ada. */
        val renderTicks: Long,
        /** Total nanodetik thread render siap jalan tetapi menunggu CPU. -1 bila kernel tidak menyediakan. */
        val renderWaitNs: Long,
        /** Waktu CPU seluruh proses dalam tick. */
        val processTicks: Long
    )

    private class GcSummary {
        var count = 0
        var full = 0
        var maxTenthsMs = 0L
        var totalTenthsMs = 0L
    }

    private fun monitorLoop() {
        try {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)
        } catch (_: Throwable) {
        }
        try {
            val tid = waitForRenderThread()
            if (tid < 0) return
            LoggerBridge.append(
                LOG_PREFIX + "pemantau aktif (thread render $tid), laporan tiap ${REPORT_INTERVAL_MS / 1000L} dtk | " +
                    "mode kinerja stabil Android: ${sustainedSupportLabel()}"
            )
            var prev = takeSample(tid)
            while (running) {
                Thread.sleep(REPORT_INTERVAL_MS)
                if (!running) break
                val now = takeSample(tid)
                if (now.renderTicks < 0L) break
                report(prev, now)
                prev = now
            }
        } catch (_: InterruptedException) {
        } catch (_: Throwable) {
        }
    }

    private fun waitForRenderThread(): Int {
        val deadline = SystemClock.elapsedRealtime() + FIND_TIMEOUT_MS
        while (running && SystemClock.elapsedRealtime() < deadline) {
            val tid = findRenderThread()
            if (tid > 0) return tid
            Thread.sleep(FIND_INTERVAL_MS)
        }
        return -1
    }

    /**
     * Cari thread render lewat namanya di /proc. Minecraft memberi nama "Render thread" (1.14 ke
     * atas) atau "Client thread" (versi lama) pada thread utamanya, dan Java meneruskan nama itu
     * ke thread aslinya di Linux.
     */
    private fun findRenderThread(): Int {
        val dirs = File("/proc/self/task").listFiles() ?: return -1
        for (dir in dirs) {
            val tid = dir.name.toIntOrNull() ?: continue
            val name = firstLine(File(dir, "comm")) ?: continue
            if (name == "Render thread" || name == "Client thread") return tid
        }
        return -1
    }

    private fun takeSample(tid: Int): Sample {
        val sched = readSchedStat(tid)
        return Sample(
            timeMs = SystemClock.elapsedRealtime(),
            renderTicks = readCpuTicks(File("/proc/self/task/$tid/stat")),
            renderWaitNs = if (sched != null) sched[1] else -1L,
            processTicks = readCpuTicks(File("/proc/self/stat"))
        )
    }

    private fun report(prev: Sample, now: Sample) {
        try {
            val wallMs = now.timeMs - prev.timeMs
            if (wallMs <= 0L) return
            val tps = ticksPerSecond
            val renderPct = (now.renderTicks - prev.renderTicks) * 100_000L / (tps * wallMs)
            val coresTenths = (now.processTicks - prev.processTicks) * 10_000L / (tps * wallMs)

            val sb = StringBuilder(LOG_PREFIX)
            sb.append(wallMs / 1000L).append(" dtk | thread render: CPU ").append(renderPct).append('%')
            if (prev.renderWaitNs >= 0L && now.renderWaitNs >= 0L) {
                val waitPct = (now.renderWaitNs - prev.renderWaitNs) * 100L / (wallMs * 1_000_000L)
                sb.append(", menunggu giliran CPU ").append(waitPct).append('%')
            }
            sb.append(" | proses: ").append(tenths(coresTenths)).append(" inti")

            val gc = drainGcLog()
            if (gc != null) {
                sb.append(" | GC: ").append(gc.count).append("x")
                if (gc.count > 0) {
                    sb.append(", terlama ").append(tenths(gc.maxTenthsMs)).append(" ms")
                    sb.append(", total ").append(tenths(gc.totalTenthsMs)).append(" ms")
                    if (gc.full > 0) sb.append(", penuh ").append(gc.full).append("x")
                }
            }

            val rssKb = readKb("/proc/self/status", "VmRSS:")
            val availKb = readKb("/proc/meminfo", "MemAvailable:")
            if (rssKb >= 0L) {
                sb.append(" | RAM proses ").append(rssKb / 1024L).append(" MB")
                if (availKb >= 0L) sb.append(", tersedia ").append(availKb / 1024L).append(" MB")
            }
            val thermal = thermalLabel()
            if (thermal != null) sb.append(" | suhu HP: ").append(thermal)
            LoggerBridge.append(sb.toString())
        } catch (_: Throwable) {
        }
    }

    /** Baca baris baru di log GC sejak laporan terakhir dan ringkas jeda GC-nya; null bila tidak ada log. */
    private fun drainGcLog(): GcSummary? {
        val file = gcLogFile ?: return null
        if (!file.exists()) return null
        val summary = GcSummary()
        try {
            RandomAccessFile(file, "r").use { raf ->
                val length = raf.length()
                if (length < gcOffset) gcOffset = 0L
                val toRead = minOf(length - gcOffset, MAX_GC_READ_BYTES.toLong()).toInt()
                if (toRead <= 0) return summary
                raf.seek(gcOffset)
                val buf = ByteArray(toRead)
                raf.readFully(buf)
                // Hanya baris utuh; sisa baris yang belum selesai dibaca pada laporan berikutnya.
                var end = toRead
                while (end > 0 && buf[end - 1] != '\n'.code.toByte()) end--
                if (end == 0) return summary
                gcOffset += end.toLong()
                val text = String(buf, 0, end, Charsets.ISO_8859_1)
                for (line in text.split('\n')) {
                    if (!line.contains("Pause ") || !line.endsWith("ms")) continue
                    val ms = line.substringBeforeLast("ms").substringAfterLast(' ').toDoubleOrNull() ?: continue
                    val tenths = (ms * 10.0).toLong()
                    summary.count++
                    summary.totalTenthsMs += tenths
                    if (tenths > summary.maxTenthsMs) summary.maxTenthsMs = tenths
                    if (line.contains("Pause Full")) summary.full++
                }
            }
        } catch (_: Throwable) {
        }
        return summary
    }

    private fun tenths(value: Long): String = "${value / 10L}.${value % 10L}"

    /**
     * Status suhu HP menurut Android (Android 10 ke atas), atau null bila tidak tersedia. Mulai dari
     * "hangat" Android sudah bisa menurunkan kecepatan prosesor; "panas berat" ke atas pasti terasa.
     */
    private fun thermalLabel(): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return try {
            val power = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return null
            // 0 = normal, 1 = ringan, 2 = sedang, 3 = berat, 4 = kritis, 5 = darurat, 6 = mati
            when (power.getCurrentThermalStatus()) {
                0 -> "normal"
                1 -> "hangat"
                2 -> "panas sedang"
                3 -> "panas berat"
                4 -> "panas kritis"
                5 -> "darurat"
                6 -> "hampir mati"
                else -> null
            }
        } catch (_: Throwable) {
            null
        }
    }

    /** Apakah HP ini mendukung mode kinerja stabil Android (tidak berarti sedang aktif). */
    private fun sustainedSupportLabel(): String {
        return try {
            val power = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (power != null && power.isSustainedPerformanceModeSupported()) "didukung" else "tidak didukung"
        } catch (_: Throwable) {
            "tidak diketahui"
        }
    }
}

/** Jumlah tick CPU per detik (hampir selalu 100). */
private fun readTicksPerSecond(): Long {
    return try {
        val v = Os.sysconf(OsConstants._SC_CLK_TCK)
        if (v > 0L) v else 100L
    } catch (_: Throwable) {
        100L
    }
}

private fun firstLine(file: File): String? {
    return try {
        file.bufferedReader().use { it.readLine() }
    } catch (_: Throwable) {
        null
    }
}

/**
 * utime + stime (tick) dari satu berkas stat di /proc, atau -1 bila gagal dibaca. Nama proses di
 * baris itu boleh berisi spasi dan kurung, jadi pembacaan dimulai setelah tanda ")" terakhir.
 */
private fun readCpuTicks(statFile: File): Long {
    return try {
        val line = firstLine(statFile) ?: return -1L
        val close = line.lastIndexOf(')')
        if (close < 0) return -1L
        val fields = line.substring(close + 2).split(' ')
        // fields[0] = status (kolom 3); utime = kolom 14, stime = kolom 15.
        if (fields.size < 13) return -1L
        fields[11].toLong() + fields[12].toLong()
    } catch (_: Throwable) {
        -1L
    }
}

/** (waktu jalan ns, waktu menunggu CPU ns) dari schedstat satu thread, atau null bila kernel tidak menyediakan. */
private fun readSchedStat(tid: Int): LongArray? {
    return try {
        val line = firstLine(File("/proc/self/task/$tid/schedstat")) ?: return null
        val parts = line.trim().split(' ')
        if (parts.size < 2) null else longArrayOf(parts[0].toLong(), parts[1].toLong())
    } catch (_: Throwable) {
        null
    }
}

/** Nilai (kB) dari satu baris "Kunci:   123 kB" di berkas /proc, atau -1 bila tidak ada. */
private fun readKb(path: String, key: String): Long {
    try {
        File(path).bufferedReader().use { reader ->
            while (true) {
                val line = reader.readLine() ?: break
                if (line.startsWith(key)) {
                    val digits = line.filter { it.isDigit() }
                    return if (digits.isEmpty()) -1L else digits.toLong()
                }
            }
        }
    } catch (_: Throwable) {
    }
    return -1L
}

// ----------------------------------------------------------------------------
// Section: core/plugin/NovaPluginCore.kt
// ----------------------------------------------------------------------------

// ============================================================================
// NovaCraft Unified Native Plugin Sanitizer & Manager
// ============================================================================

/**
 * Penyaring argumen JVM dari metadata `environment` plugin native library.
 */
object NativePluginArgs {
    const val PLACEHOLDER = "{nativeLibraryDir}"

    private val PROTECTED_KEYS = setOf(
        "java.home", "java.library.path", "java.class.path", "java.io.tmpdir",
        "user.home", "os.name", "os.version", "jna.boot.library.path"
    )

    private val WHITESPACE = Regex("\\s+")

    fun sanitize(environment: String, libDir: String, onReject: (String) -> Unit = {}): List<String> {
        if (environment.isBlank()) return emptyList()
        val out = ArrayList<String>()
        for (token in environment.trim().split(WHITESPACE)) {
            if (token.isEmpty()) continue
            if (!token.startsWith("-D")) {
                onReject("hanya -D yang diizinkan: $token")
                continue
            }
            val eq = token.indexOf('=')
            val key = if (eq < 0) token.substring(2) else token.substring(2, eq)
            if (key.isEmpty() || key in PROTECTED_KEYS || key.startsWith("org.lwjgl.")) {
                onReject("properti dikelola launcher: $token")
                continue
            }
            if (eq < 0) {
                out.add(token)
                continue
            }
            val raw = token.substring(eq + 1)
            val value = if (raw.contains(PLACEHOLDER)) raw.replace(PLACEHOLDER, libDir) else raw
            if (value.split('/', ',', ':').any { it == ".." }) {
                onReject("path mengandung '..': $token")
                continue
            }
            out.add("-D$key=$value")
        }
        return out
    }
}

object NativePluginManager {
    private const val TAG = "NativePlugin"
    private const val META_FLAG = "FCLNativePlugin"

    data class NativePlugin(
        val packageName: String,
        val displayName: String,
        val nativeLibDir: String,
        val minMcVersion: String?,
        val maxMcVersion: String?,
        val jvmArgs: List<String>
    )

    class LaunchPlan(
        val active: List<NativePlugin>,
        val libPaths: List<String>,
        val jvmArgs: List<String>,
        val skipped: List<String>
    ) {
        val isEmpty: Boolean get() = active.isEmpty()
    }

    @Volatile
    private var plugins: List<NativePlugin> = emptyList()

    fun getPlugins(): List<NativePlugin> = plugins

    fun setPlugins(list: List<NativePlugin>) {
        plugins = list.sortedBy { it.displayName.lowercase() }
    }

    fun parse(info: ApplicationInfo, label: () -> String): NativePlugin? {
        if ((info.flags and ApplicationInfo.FLAG_SYSTEM) != 0) return null
        val meta = info.metaData ?: return null
        if (!meta.getBoolean(META_FLAG, false)) return null

        val libDir = info.nativeLibraryDir?.takeIf { it.isNotBlank() } ?: return null
        val pkg = info.packageName
        val name = meta.getString("des")?.takeIf { it.isNotBlank() } ?: label()

        @Suppress("DEPRECATION")
        fun versionOf(key: String): String? {
            if (!meta.containsKey(key)) return null
            val v = when (val raw = meta.get(key)) {
                is String -> raw
                is Number -> raw.toString()
                else -> null
            }
            return v?.trim()?.takeIf { it.isNotEmpty() }
        }

        return NativePlugin(
            packageName = pkg,
            displayName = name,
            nativeLibDir = libDir,
            minMcVersion = versionOf("minMCVer"),
            maxMcVersion = versionOf("maxMCVer"),
            jvmArgs = parseArgs(meta.getString("environment").orEmpty(), libDir, pkg)
        )
    }

    fun planFor(mcVersion: String, disabled: Collection<String>): LaunchPlan {
        val all = plugins
        if (all.isEmpty()) return LaunchPlan(emptyList(), emptyList(), emptyList(), emptyList())

        val active = ArrayList<NativePlugin>(all.size)
        val skipped = ArrayList<String>()
        for (p in all) {
            when {
                p.packageName in disabled -> skipped.add("${p.displayName}: dimatikan di Pengaturan")
                !supports(p, mcVersion) -> skipped.add("${p.displayName}: di luar rentang versi Minecraft ($mcVersion)")
                !File(p.nativeLibDir).isDirectory -> skipped.add("${p.displayName}: folder pustaka tidak ada")
                else -> active.add(p)
            }
        }
        return LaunchPlan(
            active = active,
            libPaths = active.map { it.nativeLibDir }.distinct(),
            jvmArgs = active.flatMap { it.jvmArgs }.distinct(),
            skipped = skipped
        )
    }

    fun supports(p: NativePlugin, mcVersion: String): Boolean {
        if (mcVersion.isBlank() || mcVersion.equals("Unknown", ignoreCase = true)) return true
        val min = p.minMcVersion
        if (min != null && NuxVersionUtils.isLowerVer(mcVersion, min)) return false
        val max = p.maxMcVersion
        if (max != null && NuxVersionUtils.isBiggerVer(mcVersion, max)) return false
        return true
    }

    private fun parseArgs(environment: String, libDir: String, pkg: String): List<String> =
        NativePluginArgs.sanitize(environment, libDir) { reason ->
            Log.w(TAG, "$pkg: argumen ditolak - $reason")
        }
}

// ----------------------------------------------------------------------------
// Section: core/settings/NovaSettingsCore.kt
// ----------------------------------------------------------------------------

// ============================================================================
// NovaCraft Unified Settings & Power-Saver Profile Manager
// ============================================================================

/**
 * Mode Hemat: preset sekali ketuk untuk HP yang cepat panas atau baterainya boros.
 */
object PowerSaver {
    const val MAX_RESOLUTION = 80
    private const val MIN_RESOLUTION = 40
    private const val MAX_STORED_RESOLUTION = 125

    fun cappedResolution(current: Int): Int {
        return minOf(current, MAX_RESOLUTION).coerceAtLeast(MIN_RESOLUTION)
    }

    fun enable(s: LauncherSettings): LauncherSettings {
        if (s.powerSaverMode) return s
        return s.copy(
            powerSaverMode = true,
            powerSaverPrevResolution = s.resolutionRatio.coerceIn(MIN_RESOLUTION, MAX_STORED_RESOLUTION),
            powerSaverPrevSustained = s.sustainedPerformanceMode,
            powerSaverPrevVsync = s.vsyncInZink,
            sustainedPerformanceMode = true,
            vsyncInZink = true,
            resolutionRatio = cappedResolution(s.resolutionRatio)
        )
    }

    fun disable(s: LauncherSettings): LauncherSettings {
        if (!s.powerSaverMode) return s
        val untouched = s.resolutionRatio == cappedResolution(s.powerSaverPrevResolution)
        return s.copy(
            powerSaverMode = false,
            sustainedPerformanceMode = s.powerSaverPrevSustained,
            vsyncInZink = s.powerSaverPrevVsync,
            resolutionRatio = if (untouched) s.powerSaverPrevResolution else s.resolutionRatio
        )
    }

    fun set(s: LauncherSettings, on: Boolean): LauncherSettings = if (on) enable(s) else disable(s)
}

object SettingsManager {
    private const val PERF_REV = 2

    private val gson = Gson()
    private val _settings = MutableStateFlow(LauncherSettings())
    val settings: StateFlow<LauncherSettings> = _settings.asStateFlow()

    @Volatile
    var appContext: Context? = null
        private set

    private fun getSettingsFile(context: Context): File {
        return File(context.filesDir, "settings.json")
    }

    fun init(context: Context) {
        appContext = context.applicationContext
        val file = getSettingsFile(context)
        if (file.exists()) {
            try {
                val json = file.readText()
                val loaded = gson.fromJson(json, LauncherSettings::class.java)
                if (loaded != null) {
                    val migrated = applyPerfDefaults(loaded)
                    _settings.value = migrated
                    if (migrated !== loaded) save(context)
                    return
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        _settings.value = defaultSettingsFor(context)
        save(context)
    }

    fun updateSettings(context: Context, newSettings: LauncherSettings) {
        _settings.value = newSettings
        save(context)
    }

    fun resetToDefaults(context: Context) {
        _settings.value = defaultSettingsFor(context)
        save(context)
    }

    fun resetControlSettings(context: Context) {
        val def = LauncherSettings()
        val cur = _settings.value
        val updated = cur.copy(
            mouseControlMode = def.mouseControlMode,
            cursorSensitivity = def.cursorSensitivity,
            captureSensitivity = def.captureSensitivity,
            touchAttackEnabled = def.touchAttackEnabled,
            hotbarTouchEnabled = def.hotbarTouchEnabled,
            typingPreview = def.typingPreview,
            enableKeyboardPanning = def.enableKeyboardPanning,
            buttonLookDrag = def.buttonLookDrag,
            touchControllerEnabled = def.touchControllerEnabled,
            mouseSizeDp = def.mouseSizeDp,
            hideMouseInClickMode = def.hideMouseInClickMode,
            physicalMouseSystemPointer = def.physicalMouseSystemPointer,
            physicalMouseCursorSpeed = def.physicalMouseCursorSpeed,
            physicalMouseCameraSpeed = def.physicalMouseCameraSpeed,
            pointerHotspots = emptyMap(),
            controllerDeadzonePercent = def.controllerDeadzonePercent,
            controlButtonWidthPercent = def.controlButtonWidthPercent,
            controlButtonOpacityPercent = def.controlButtonOpacityPercent,
            startWithVirtualMouse = def.startWithVirtualMouse,
            gyroEnabled = def.gyroEnabled,
            gyroSensitivityPercent = def.gyroSensitivityPercent,
            disableGestures = def.disableGestures,
            disableDoubleTapOffhand = def.disableDoubleTapOffhand,
            longPressDelayMs = def.longPressDelayMs,
            uppercaseButtonLabels = def.uppercaseButtonLabels,
            virtualMouseSizePercent = def.virtualMouseSizePercent
        )
        updateSettings(context, updated)
    }

    private fun applyPerfDefaults(s: LauncherSettings): LauncherSettings {
        var updated = s
        if (updated.thermalPerfCap <= 0) {
            updated = updated.copy(thermalPerfCap = 90)
        }
        if (updated.controllerDeadzonePercent <= 0) {
            updated = updated.copy(controllerDeadzonePercent = 100)
        }
        if (updated.controlButtonWidthPercent <= 0) {
            updated = updated.copy(controlButtonWidthPercent = 100)
        }
        if (updated.controlButtonOpacityPercent <= 0) {
            updated = updated.copy(controlButtonOpacityPercent = 100)
        }
        if (updated.gyroSensitivityPercent <= 0) {
            updated = updated.copy(gyroSensitivityPercent = 100)
        }
        if (updated.longPressDelayMs <= 0) {
            updated = updated.copy(longPressDelayMs = 300)
        }
        if (updated.virtualMouseSizePercent <= 0) {
            updated = updated.copy(virtualMouseSizePercent = 100)
        }
        if (updated.colorSource.isBlank()) {
            updated = updated.copy(colorSource = "novacraft")
        }
        if (updated.gameDownloadSource.isBlank()) {
            updated = updated.copy(gameDownloadSource = "default")
        }
        if (updated.perfRev < 1) {
            updated = updated.copy(homeBgAnimate = false, homeAmbientEnabled = false)
        }
        if (updated.perfRev < PERF_REV) {
            updated = updated.copy(
                uppercaseButtonLabels = true,
                animScreenTransitions = true,
                animDialogsPopups = true,
                animButtonEffects = true,
                animListsContent = true,
                startupSoundEnabled = true,
                verifyGameFiles = true,
                checkGameVersionList = true,
                perfRev = PERF_REV
            )
        }
        return updated
    }

    private fun defaultSettingsFor(context: Context): LauncherSettings {
        val totalRamMb = getTotalDeviceMemoryMb(context)
        val minSidePx = try {
            val metrics = context.resources.displayMetrics
            minOf(metrics.widthPixels, metrics.heightPixels)
        } catch (_: Throwable) {
            0
        }
        return LauncherSettings(
            ramMb = recommendedRamMb(totalRamMb),
            resolutionRatio = recommendedResolutionRatio(totalRamMb, minSidePx),
            perfRev = PERF_REV
        )
    }

    fun recommendedResolutionRatio(totalRamMb: Int, minSidePx: Int): Int {
        if (minSidePx <= 0) return 100
        val targetSide = when {
            totalRamMb < 4096 -> 540
            totalRamMb < 6144 -> 720
            else -> 1080
        }
        if (minSidePx <= targetSide) return 100
        val ratio = 100.0 * targetSide / minSidePx
        val stepped = (Math.ceil(ratio / 5.0) * 5.0).toInt()
        return stepped.coerceIn(40, 100)
    }

    fun recommendedRamMb(totalRamMb: Int): Int = when {
        totalRamMb >= 12000 -> 4096
        totalRamMb >= 8000 -> 3072
        totalRamMb >= 6000 -> 2560
        totalRamMb >= 4000 -> 2048
        totalRamMb >= 3000 -> 1280
        totalRamMb >= 2500 -> 1024
        else -> 768
    }

    private fun save(context: Context) {
        try {
            val file = getSettingsFile(context)
            com.aistudio.novacraft.launcher.core.utils.AtomicFiles.writeText(file, gson.toJson(_settings.value))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getTotalDeviceMemoryMb(context: Context): Int {
        return try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return 4096
            val memInfo = ActivityManager.MemoryInfo()
            actManager.getMemoryInfo(memInfo)
            (memInfo.totalMem / (1024 * 1024)).toInt()
        } catch (_: Throwable) {
            4096
        }
    }

    fun getAvailableDeviceMemoryMb(context: Context): Int {
        return try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return 2048
            val memInfo = ActivityManager.MemoryInfo()
            actManager.getMemoryInfo(memInfo)
            (memInfo.availMem / (1024 * 1024)).toInt()
        } catch (_: Throwable) {
            2048
        }
    }

    fun getCpuCoreCount(): Int {
        return Runtime.getRuntime().availableProcessors()
    }
}

// ----------------------------------------------------------------------------
// Section: core/vulkan/NovaVulkanEngine.kt
// ----------------------------------------------------------------------------

// ============================================================================
// NovaCraft Unified Vulkan Diagnostic & Verification Engine
// ============================================================================

enum class VerdictLevel { CHECKING, OK, INFO, WARN, BAD }

class Verdict(val level: VerdictLevel, val text: String, val detail: String = "")

object McVulkan {
    const val FIRST_VULKAN_MC = "26.2"

    private class Need(
        val name: String,
        val isFeature: Boolean,
        val from: String,
        val through: String? = null
    ) {
        fun appliesTo(release: String): Boolean {
            if (NuxVersionUtils.compare(release, from) < 0) return false
            return through == null || NuxVersionUtils.compare(release, through) <= 0
        }
    }

    private val NEEDS = listOf(
        Need("VK_KHR_synchronization2", false, "26.2"),
        Need("VK_EXT_vertex_attribute_divisor", false, "26.2"),
        Need("VK_KHR_swapchain", false, "26.2"),
        Need("VK_KHR_dynamic_rendering", false, "26.2", "26.3"),
        Need("VK_KHR_push_descriptor", false, "26.2", "26.3"),
        Need("multiDrawIndirect", true, "26.2"),
        Need("samplerAnisotropy", true, "26.2"),
        Need("shaderDrawParameters", true, "26.2"),
        Need("timelineSemaphore", true, "26.2"),
        Need("hostQueryReset", true, "26.2"),
        Need("synchronization2", true, "26.2"),
        Need("vertexAttributeInstanceRateDivisor", true, "26.2"),
        Need("dynamicRendering", true, "26.2", "26.3"),
        Need("fillModeNonSolid", true, "26.2", "26.2"),
        Need("drawIndirectFirstInstance", true, "26.3")
    )

    fun isYearVersion(mcVersion: String): Boolean {
        val s = mcVersion.trimStart()
        var number = 0
        var i = 0
        while (i < s.length && i < 4 && s[i] in '0'..'9') {
            number = number * 10 + (s[i] - '0')
            i++
        }
        return i > 0 && number >= 26 && (i == s.length || s[i] == '.' || s[i] == '-')
    }

    private fun release(mcVersion: String): String {
        val dash = mcVersion.indexOf('-')
        return (if (dash > 0) mcVersion.substring(0, dash) else mcVersion).trim()
    }

    fun usesVulkan(mcVersion: String): Boolean =
        isYearVersion(mcVersion) && NuxVersionUtils.compare(release(mcVersion), FIRST_VULKAN_MC) >= 0

    fun missing(report: VulkanReport, mcVersion: String): List<String> {
        val rel = release(mcVersion)
        val out = ArrayList<String>()
        if (!report.meetsBaseline) out.add("Vulkan 1.2")
        for (need in NEEDS) {
            if (need.appliesTo(rel) && !report.has(need.name, need.isFeature)) out.add(need.name)
        }
        return out
    }

    fun verdict(report: VulkanReport?, mcVersion: String?): Verdict {
        if (report == null) {
            return Verdict(VerdictLevel.CHECKING, "Memeriksa dukungan Vulkan HP ini...")
        }
        val picked = mcVersion?.takeIf { isYearVersion(it) }
        if (picked != null && !usesVulkan(picked)) {
            return Verdict(VerdictLevel.INFO, "Versi ini memakai OpenGL. Vulkan baru dipakai mulai Minecraft 26.2.")
        }
        return when (report.state) {
            VulkanState.UNAVAILABLE -> Verdict(
                VerdictLevel.INFO,
                "Pengecekan Vulkan tidak tersedia di build ini, jadi dukungan Vulkan HP belum bisa dipastikan."
            )
            VulkanState.NO_RESPONSE -> Verdict(
                VerdictLevel.WARN,
                "Pengecekan Vulkan tidak selesai. Driver Vulkan HP ini mungkin bermasalah, jadi Minecraft 26.2 ke atas berisiko gagal bila memakai Vulkan."
            )
            VulkanState.NO_VULKAN -> Verdict(
                VerdictLevel.BAD,
                "HP ini tidak punya driver Vulkan yang bisa dipakai. Minecraft 26.2 ke atas tidak bisa memakai Vulkan di sini; pilih Prefer OpenGL di pengaturan video game."
            )
            VulkanState.OK -> {
                val lacking = missing(report, picked ?: FIRST_VULKAN_MC)
                if (lacking.isEmpty()) {
                    Verdict(VerdictLevel.OK, "Vulkan HP ini memenuhi syarat Minecraft 26.2 ke atas.", "Vulkan ${report.versionText}")
                } else {
                    val shown = lacking.take(MAX_LISTED).joinToString(", ")
                    val more = if (lacking.size > MAX_LISTED) " +${lacking.size - MAX_LISTED}" else ""
                    Verdict(
                        VerdictLevel.BAD,
                        "Vulkan HP ini belum memenuhi syarat Minecraft 26.2 ke atas. Bila game memakai Vulkan, bisa gagal dimulai; pilih Prefer OpenGL di pengaturan video game.",
                        "Vulkan ${report.versionText}. $shown$more"
                    )
                }
            }
        }
    }

    private const val MAX_LISTED = 5
}

enum class VulkanState {
    OK,
    NO_VULKAN,
    UNAVAILABLE,
    NO_RESPONSE
}

class VulkanReport(
    val state: VulkanState,
    val major: Int = 0,
    val minor: Int = 0,
    val patch: Int = 0,
    val extensions: Set<String> = emptySet(),
    val features: Map<String, Boolean> = emptyMap(),
    val detail: String = ""
) {
    val versionText: String get() = "$major.$minor.$patch"
    val meetsBaseline: Boolean get() = major > 1 || (major == 1 && minor >= 2)

    fun has(name: String, isFeature: Boolean): Boolean =
        if (isFeature) features[name] == true else name in extensions

    internal fun toJson(key: String): String {
        val json = JSONObject()
        json.put("format", VulkanCheck.FORMAT)
        json.put("key", key)
        json.put("state", state.name)
        json.put("major", major)
        json.put("minor", minor)
        json.put("patch", patch)
        json.put("extensions", JSONArray(extensions.toList()))
        val featureJson = JSONObject()
        for ((name, supported) in features) featureJson.put(name, supported)
        json.put("features", featureJson)
        json.put("detail", detail)
        return json.toString()
    }

    internal companion object {
        fun fromJson(text: String, key: String): VulkanReport? = try {
            val json = JSONObject(text)
            if (json.optInt("format", -1) != VulkanCheck.FORMAT || json.optString("key") != key) {
                null
            } else {
                val ext = json.optJSONArray("extensions")
                val extSet = HashSet<String>()
                if (ext != null) for (i in 0 until ext.length()) extSet.add(ext.optString(i))
                val featureJson = json.optJSONObject("features")
                val featureMap = HashMap<String, Boolean>()
                if (featureJson != null) {
                    val names = featureJson.keys()
                    while (names.hasNext()) {
                        val name = names.next()
                        featureMap[name] = featureJson.optBoolean(name, false)
                    }
                }
                VulkanReport(
                    state = VulkanState.valueOf(json.getString("state")),
                    major = json.optInt("major"),
                    minor = json.optInt("minor"),
                    patch = json.optInt("patch"),
                    extensions = extSet,
                    features = featureMap,
                    detail = json.optString("detail")
                )
            }
        } catch (_: Exception) {
            null
        }
    }
}

object VulkanCheck {
    internal const val FORMAT = 1
    internal const val DIR_NAME = "vulkan-check"
    internal const val RESULT_NAME = "result.json"
    internal const val MARKER_NAME = "check.running"

    private const val TAG = "Vulkan"
    private const val WAIT_MS = 15_000L
    private const val POLL_MS = 150L

    private val lock = Mutex()

    @Volatile
    private var sessionFailure: VulkanReport? = null

    internal fun dir(context: Context): File = File(context.filesDir, DIR_NAME)

    internal fun deviceKey(): String = "$FORMAT|${Build.FINGERPRINT}|${Build.VERSION.SDK_INT}"

    fun cached(context: Context): VulkanReport? = read(File(dir(context), RESULT_NAME))

    suspend fun ensure(context: Context): VulkanReport = lock.withLock {
        cached(context) ?: sessionFailure ?: withContext(Dispatchers.IO) {
            runIsolated(context.applicationContext)
        }
    }

    private fun read(file: File): VulkanReport? = try {
        if (file.isFile) VulkanReport.fromJson(file.readText(), deviceKey()) else null
    } catch (_: Exception) {
        null
    }

    private suspend fun runIsolated(app: Context): VulkanReport {
        val dir = dir(app)
        val resultFile = File(dir, RESULT_NAME)
        try {
            dir.mkdirs()
            resultFile.delete()
            File(dir, MARKER_NAME).delete()
        } catch (_: Exception) {
        }

        val gone = AtomicBoolean(false)
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {}
            override fun onServiceDisconnected(name: ComponentName?) {
                gone.set(true)
            }
            override fun onBindingDied(name: ComponentName?) {
                gone.set(true)
            }
        }
        val bound = try {
            app.bindService(Intent(app, VulkanCheckService::class.java), connection, Context.BIND_AUTO_CREATE)
        } catch (e: Throwable) {
            LauncherLog.w(TAG, "Proses pemeriksa Vulkan tidak bisa dimulai: ${e.message}")
            false
        }
        if (!bound) return fail("proses pemeriksa tidak bisa dimulai")

        try {
            val deadline = SystemClock.elapsedRealtime() + WAIT_MS
            while (true) {
                read(resultFile)?.let { return it.also { r -> logResult(r) } }
                if (gone.get()) {
                    delay(200)
                    read(resultFile)?.let { return it.also { r -> logResult(r) } }
                    return fail("proses pemeriksa berhenti sebelum selesai")
                }
                if (SystemClock.elapsedRealtime() >= deadline) return fail("pemeriksa tidak menjawab dalam ${WAIT_MS / 1000} detik")
                delay(POLL_MS)
            }
        } finally {
            try {
                app.unbindService(connection)
            } catch (_: Throwable) {
            }
        }
    }

    private fun fail(why: String): VulkanReport {
        LauncherLog.w(TAG, "Pengecekan Vulkan tidak selesai: $why")
        return VulkanReport(VulkanState.NO_RESPONSE, detail = why).also { sessionFailure = it }
    }

    private fun logResult(report: VulkanReport) {
        val summary = when (report.state) {
            VulkanState.OK -> "Vulkan ${report.versionText}, ${report.extensions.size} ekstensi"
            VulkanState.NO_VULKAN -> "HP tidak punya driver Vulkan yang bisa dibuka"
            VulkanState.UNAVAILABLE -> "pemeriksa tidak ada di build ini (${report.detail})"
            VulkanState.NO_RESPONSE -> "tidak ada jawaban"
        }
        LauncherLog.event(TAG, "Pengecekan Vulkan: $summary")
    }
}

// ----------------------------------------------------------------------------
// Section: core/models/LauncherSettings.kt
// ----------------------------------------------------------------------------

/**
 * Global Configuration for NUX Launcher (Java, RAM, Controls, Graphics)
 */
data class LauncherSettings(
    // JVM & RAM Memory
    val ramMb: Int = 2048,
    val initialHeapMb: Int = 256,
    val customJvmArgs: String = "",
    val defaultJavaRuntime: String = "auto",
    val jarSandboxEnabled: Boolean = false,

    // In-game Mouse & Touch Controls (Moved from in-game HUD to Settings)
    val mouseControlMode: String = "SLIDE", // "SLIDE" (Trackpad) or "CLICK" (Direct Touch)
    val cursorSensitivity: Int = 100, // 50% - 300%
    val captureSensitivity: Int = 125, // 50% - 300% (Camera look speed)
    // Pukul/hancurkan lewat ketukan atau tahan di layar kosong saat bermain. Bawaan mati supaya
    // menggeser kamera tidak ikut memukul; pukul lewat tombol kontrol tetap bekerja.
    val touchAttackEnabled: Boolean = false,
    // Hotbar sentuh gaya Bedrock: ketuk hotbar game untuk memilih slot saat bermain.
    val hotbarTouchEnabled: Boolean = true,
    // Bar kecil di atas keyboard layar yang menampilkan huruf yang sedang diketik (keyboard menutupi kotak chat game).
    val typingPreview: Boolean = true,
    val enableKeyboardPanning: Boolean = true,
    // Menggeser jari dari tombol serang/pakai/lompat ikut memutar kamera. Bawaan mati: jari yang
    // sedikit bergeser saat menahan tombol (PvP) tidak boleh menggeser pandangan.
    val buttonLookDrag: Boolean = false,
    // Eksperimental, bawaan mati. Bila modnya terpasang dan tersambung, sentuhan juga diteruskan ke mod
    // sementara tombol dan mouse virtual launcher tetap aktif. Namanya sengaja baru supaya nilai "aktif"
    // dari versi sebelumnya tidak terbawa.
    val touchControllerEnabled: Boolean = false,
    val mouseSizeDp: Int = 24, // 16 - 48 dp
    // Mesin Rust: tombol layar mengikuti tata letak kontrol Amber (bukan kontrol bawaan mesin). Diterapkan saat PLAY.
    val rustAmberControls: Boolean = true,
    // Mesin Rust: tombol bergaris tiga di layar game yang membuka menu cepat mesin. Bawaan nyala. Diterapkan saat PLAY.
    val rustControlHub: Boolean = true,
    // Mesin Rust: buka antarmuka peluncur internal Horizon saat menekan PLAY, bukan langsung masuk ke game.
    val rustUseInternalUi: Boolean = false,
    // Gaya visual UI internal Horizon: "nova_obsidian", "nova_aurora", "nova_nebula", "nova_solar"
    val rustThemePreset: String = "nova_obsidian",
    val hideMouseInClickMode: Boolean = true,

    // Mouse fisik. false (bawaan) = mouse fisik menggerakkan kursor launcher di menu (pointer capture, jalur yang
    // sama dengan saat bermain), jadi gambar, ukuran, dan hotspot kursor bisa diatur. true = kursor sistem Android.
    val physicalMouseSystemPointer: Boolean = false,
    // Pengali (persen) di atas sensitivitas kursor / kamera di atas, khusus mouse fisik. 100 = sama persis.
    val physicalMouseCursorSpeed: Int = 100, // 20 - 300
    val physicalMouseCameraSpeed: Int = 100, // 20 - 300
    // Hotspot kursor per bentuk, dalam persen kotak kursor: "arrow.x", "arrow.y", "hand.x", dst. Kosong = bawaan.
    val pointerHotspots: Map<String, Int> = emptyMap(),

    // Graphics & Performance
    val resolutionRatio: Int = 100, // 40% - 125%, kelipatan 5 (slider di Pengaturan dan menu dalam game)
    val autoOptimizeMinecraft: Boolean = true,
    val sustainedPerformanceMode: Boolean = false,
    val thermalPerfCap: Int = 90, // 50 - 100 (%) batas performa maks untuk menahan panas
    val selectedRenderer: String = "auto", // "auto", "krypton", "mobileglues", "gl4es", "zink", "freedreno"
    val vulkanDriver: String = "auto", // "auto", "system", "turnip", etc.
    val graphicsApi: String = "DEFAULT", // "DEFAULT", "OPENGL", "VULKAN"
    val zinkPreferSystemDriver: Boolean = false,
    val vsyncInZink: Boolean = false,
    val forceVsync: Boolean = false,
    val useAngle: Boolean = false,
    val useSystemAngle: Boolean = false,

    // Rekam layar bawaan (encoder perangkat keras). Bawaan sengaja hemat: 720p, 30 fps, tanpa mikrofon.
    val recordQuality: String = "light", // "eco" (540p), "light" (720p), "high" (1080p)
    val recordFps: Int = 30, // 30 atau 60
    val recordMic: Boolean = false, // rekam suara mikrofon (butuh izin)
    val recordThermalGuard: Boolean = true, // hentikan rekaman otomatis bila HP sangat panas
    val recordWithControls: Boolean = false, // false = layar game saja (tombol disembunyikan saat merekam), true = ikut merekam tombol kontrol

    // Plugin native library (APK format Zalith/FCL, mis. untuk Distant Horizons) yang DIMATIKAN pengguna,
    // berupa nama paket. Daftar kosong = semua plugin terpasang dipakai. Menyimpan yang dimatikan (bukan yang
    // dinyalakan) membuat plugin baru langsung aktif tanpa langkah tambahan.
    val disabledNativePlugins: List<String> = emptyList(),

    // Kunci layar 90/120 Hz ke 60 Hz selama game berjalan (bawaan mati).
    val lockRefresh60: Boolean = false,
    // Permukaan game selalu TextureView (bukan SurfaceView). Perbaikan bila tampilan game terputar 90 derajat
    // di HP yang layar alaminya portrait; TextureView sedikit lebih berat (bawaan mati).
    val forceTextureView: Boolean = false,
    // Kết xuất bề mặt thay thế: tối ưu định dạng bề mặt & hàng đợi vẽ khi GPU bị nghẽn cổ chai.
    val alternateSurfaceRendering: Boolean = false,
    // HP RAM kecil: "auto" (bawaan launcher), "serial", atau "default"
    // (tanpa flag GC tambahan dari launcher).
    val generalGcMode: String = "auto",

    // MobileGlues: launcher memberi folder config sendiri (MG_DIR_PATH) supaya MobileGlues membaca config.json
    // dan cache terjemahan shader menyala. Mati = MobileGlues memakai bawaan paksanya (tanpa cache).
    val mobileGluesConfig: Boolean = true,

    // Mode Hemat: preset sekali ketuk (mode performa stabil sistem, V-Sync Zink, resolusi maks 80%).
    // Nilai sebelum dinyalakan disimpan supaya bisa dikembalikan saat dimatikan.
    val powerSaverMode: Boolean = false,
    val powerSaverPrevResolution: Int = 100,
    val powerSaverPrevSustained: Boolean = false,
    val powerSaverPrevVsync: Boolean = false,

    // Latar Beranda (foto / GIF / video). Bawaan mati supaya Beranda diam dan tidak memakai CPU/GPU
    // di HP kelas bawah; bisa dinyalakan di Pengaturan > Tampilan.
    val homeBgAnimate: Boolean = false, // false = GIF dan video tampil diam (paling hemat)
    val homeBgPauseOnPowerSave: Boolean = true, // jeda GIF/video saat mode Hemat Baterai HP menyala
    val homeBgSound: Boolean = false, // suara video, bawaan mati
    val homeBgVolume: Int = 50, // 0 - 100
    val homeAmbientEnabled: Boolean = false, // cahaya di sekitar kartu Beranda (hanya GIF/video)
    val homeAmbientIntensity: Int = 60, // 20 - 100

    // Kunci API CurseForge milik pengguna (diisi dari layar Mod). Kosong = pakai kunci bawaan build bila ada.
    val curseForgeApiKey: String = "",

    // Pembaruan aplikasi (terputus dari GitHub)
    val autoCheckUpdates: Boolean = false,

    // Mode akun & persistensi sesi lokal
    val offlineMode: Boolean = false, // false = Online, true = Offline (pakai sesi lokal tanpa verifikasi online)
    val persistAccountSession: Boolean = true, // simpan akun aktif & sesi lokal antar sesi launcher

    // Tampilan: mode tema ("dark", "light", "system"), nguồn màu ("novacraft", "dynamic", "system_accent"),
    // warna aksen ("navy", "green", "purple", "teal"), gaya ("default" atau "liquid"), bgDim (0 - 100),
    // animasi & suara pembuka.
    val themeMode: String = "dark",
    val colorSource: String = "novacraft",
    val accentColor: String = "navy",
    val uiStyle: String = "default",
    val bgDim: Int = 50,
    val animScreenTransitions: Boolean = true,
    val animDialogsPopups: Boolean = true,
    val animButtonEffects: Boolean = true,
    val animListsContent: Boolean = true,
    val startupSoundEnabled: Boolean = true,

    // Tuỳ chỉnh bộ điều khiển & cử chỉ & con quay hồi chuyển
    val controllerDeadzonePercent: Int = 100, // 50 - 200 (%)
    val controlButtonWidthPercent: Int = 100, // 50 - 200 (%)
    val controlButtonOpacityPercent: Int = 100, // 10 - 100 (%)
    val startWithVirtualMouse: Boolean = false,
    val gyroEnabled: Boolean = false,
    val gyroSensitivityPercent: Int = 100, // 20 - 300 (%)
    val disableGestures: Boolean = false,
    val disableDoubleTapOffhand: Boolean = false,
    val longPressDelayMs: Int = 300, // 100 - 1000 ms
    val uppercaseButtonLabels: Boolean = true,
    val virtualMouseSizePercent: Int = 100, // 50 - 200 (%)

    // Các cài đặt khác
    val verifyGameFiles: Boolean = true,
    val fastGameStartup: Boolean = false,
    val gameDownloadSource: String = "default", // "default" or "bmclapi"
    val checkGameVersionList: Boolean = true,
    val localOnlyMode: Boolean = false, // Ẩn trình duyệt CurseForge trực tuyến & chỉ dùng bộ nhớ đệm mod cục bộ

    // Discord Rich Presence (không dùng token/tài khoản, không chia sẻ tên instance hay tài khoản)
    val discordRichPresenceOnPlay: Boolean = false,
    val discordShowServer: Boolean = false,
    val discordPresenceIdle: Boolean = false,

    // Những cài đặt thử nghiệm
    val dumpShadersEnabled: Boolean = false,
    val forceBigCoreRender: Boolean = false,
    val freedrenoSysmem: Boolean = false,
    val forceOpenSlAudio: Boolean = false,
    val disableCurseForge: Boolean = false,
    val ubwcWorkaround: Boolean = false,

    // Gelombang penyederhanaan bawaan yang sudah diterapkan ke berkas pengaturan ini (0 = belum
    // pernah). Dipakai SettingsManager supaya perubahan bawaan hanya diterapkan sekali.
    val perfRev: Int = 0
)

// ----------------------------------------------------------------------------
// Section: core/models/MinecraftModels.kt
// ----------------------------------------------------------------------------

/**
 * Mojang Version Manifest V2 models
 */
data class VersionManifest(
    val latest: LatestVersion,
    val versions: List<VersionItem>
)

data class LatestVersion(
    val release: String,
    val snapshot: String
)

data class VersionItem(
    val id: String,
    val type: String,
    val url: String,
    val time: String,
    val releaseTime: String,
    /** Sidik jari (SHA-1) berkas version.json dari manifest resmi Mojang; dipakai untuk memverifikasi unduhan. */
    val sha1: String? = null
)

/**
 * Detailed Version JSON
 */
data class VersionDetail(
    val id: String,
    val mainClass: String?,
    val minecraftArguments: String?,
    val arguments: VersionArguments?,
    val downloads: VersionDownloads?,
    val libraries: List<Library>?,
    val assetIndex: AssetIndexInfo?,
    val assets: String?,
    val javaVersion: JavaVersionInfo? = null,
    val type: String?
)

data class JavaVersionInfo(
    val component: String?,
    val majorVersion: Int?
)

data class VersionArguments(
    val game: List<Any>?,
    val jvm: List<Any>?
)

data class VersionDownloads(
    val client: DownloadArtifact?,
    val server: DownloadArtifact?
)

data class DownloadArtifact(
    val path: String?,
    val sha1: String?,
    val size: Long?,
    val url: String?
)

data class Library(
    val name: String,
    val downloads: LibraryDownloads?,
    val rules: List<Rule>?,
    val natives: Map<String, String>?
)

data class LibraryDownloads(
    val artifact: DownloadArtifact?,
    val classifiers: Map<String, DownloadArtifact>?
)

data class Rule(
    val action: String, // "allow" or "disallow"
    val os: OsRule?
)

data class OsRule(
    val name: String? // "osx", "linux", "windows"
)

data class AssetIndexInfo(
    val id: String,
    val sha1: String,
    val size: Long,
    val totalSize: Long,
    val url: String
)

data class AssetIndexContent(
    val objects: Map<String, AssetObject>
)

data class AssetObject(
    val hash: String,
    val size: Long
)

/**
 * NUX Instance representation
 */
data class Instance(
    val id: String,
    val name: String,
    val mcVersion: String,
    val loader: String = "vanilla", // "vanilla", "fabric", or "forge"
    var loaderVersion: String = "",
    val icon: String = "crafting_table",
    val javaRuntime: String = "auto", // "auto", "jre-8", "jre-17", "jre-21", "jre-25"
    val createdTime: Long = System.currentTimeMillis(),
    var isDownloaded: Boolean = false
)

/**
 * User Account representation
 */
data class UserAccount(
    val id: String = "",
    val username: String = "",
    val uuid: String = "",
    val accessToken: String? = "0",
    val refreshToken: String? = "0",
    val isOffline: Boolean = true,
    val isGuest: Boolean = false,
    val email: String? = "",
    val isActivated: Boolean = false,
    val tier: String? = "unactivated",
    val photoUrl: String? = "",
    val accountType: String? = "offline", // "offline", "microsoft", "elyby"
    val xuid: String? = null,
    val authServerUrl: String? = null,
    val skinUrl: String? = null,
    val skinModel: String? = "classic", // "classic" (steve) or "slim" (alex)
    val customSkinPath: String? = null,
    val customCapePath: String? = null,
    // Kapan token Minecraft terakhir diperoleh (ms sejak epoch). Token Minecraft hanya berlaku sekitar
    // 24 jam; kosong pada akun lama, yang diperbarui sekali saat game berikutnya dijalankan.
    val tokenIssuedAt: Long? = null
) {
    val safeAccountType: String
        get() = (accountType ?: if (isOffline) "offline" else "offline").lowercase()

    val safeSkinModel: String
        get() = skinModel?.ifBlank { "classic" } ?: "classic"

    val safeAccessToken: String
        get() = accessToken?.ifBlank { "0" } ?: "0"

    val safeRefreshToken: String
        get() = refreshToken?.ifBlank { "0" } ?: "0"

    val safeEmail: String
        get() = email ?: ""

    val safeTier: String
        get() = tier?.ifBlank { "unactivated" } ?: "unactivated"

    val hasCustomSkin: Boolean
        get() = !customSkinPath.isNullOrBlank() && java.io.File(customSkinPath).exists()

    val hasCustomCape: Boolean
        get() = !customCapePath.isNullOrBlank() && java.io.File(customCapePath).exists()

    fun getSkinFile(): java.io.File? = customSkinPath?.let { java.io.File(it) }?.takeIf { it.exists() }
    fun getCapeFile(): java.io.File? = customCapePath?.let { java.io.File(it) }?.takeIf { it.exists() }
}

/**
 * Fabric Meta Loader version
 */
data class FabricLoaderItem(
    val loader: FabricLoaderInfo
)

data class FabricLoaderInfo(
    val version: String,
    val stable: Boolean
)

/**
 * Fabric Profile JSON representation from meta.fabricmc.net
 */
data class FabricProfile(
    val id: String,
    val inheritsFrom: String,
    val mainClass: String = "net.fabricmc.loader.impl.launch.knot.KnotClient",
    val libraries: List<FabricLibrary> = emptyList()
)

data class FabricLibrary(
    val name: String,
    val url: String? = "https://maven.fabricmc.net/",
    val md5: String? = null,
    val sha1: String? = null,
    val sha256: String? = null,
    val sha512: String? = null,
    val size: Long? = null
)

// ----------------------------------------------------------------------------
// Section: core/models/ModrinthModels.kt
// ----------------------------------------------------------------------------

/**
 * Modrinth Search API Response (/v2/search)
 */
data class ModrinthSearchResponse(
    val hits: List<ModrinthSearchHit> = emptyList(),
    val offset: Int = 0,
    val limit: Int = 20,
    @SerializedName("total_hits") val totalHits: Int = 0
)

/**
 * Single project hit in Modrinth search
 */
data class ModrinthSearchHit(
    @SerializedName("project_id") val projectId: String = "",
    @SerializedName("project_type") val projectType: String = "mod",
    val slug: String? = null,
    val author: String? = null,
    val title: String = "",
    val description: String? = null,
    val categories: List<String>? = null,
    @SerializedName("display_categories") val displayCategories: List<String>? = null,
    val versions: List<String>? = null,
    val downloads: Long = 0,
    @SerializedName("icon_url") val iconUrl: String? = null,
    val color: Long? = null,
    @SerializedName("date_created") val dateCreated: String? = null,
    @SerializedName("date_modified") val dateModified: String? = null,
    val follows: Long = 0
)

/**
 * Modrinth Project Details (/v2/project/{id})
 */
data class ModrinthProject(
    val id: String = "",
    val slug: String = "",
    val title: String = "",
    val description: String = "",
    val body: String? = null,
    val categories: List<String>? = null,
    val loaders: List<String>? = null,
    @SerializedName("game_versions") val gameVersions: List<String>? = null,
    val downloads: Long = 0,
    @SerializedName("icon_url") val iconUrl: String? = null,
    val gallery: List<ModrinthGalleryItem>? = null
)

data class ModrinthGalleryItem(
    val url: String = "",
    val title: String? = null,
    val description: String? = null
)

/**
 * Modrinth Version (/v2/project/{id}/version or /v2/version/{id})
 */
data class ModrinthVersion(
    val id: String = "",
    @SerializedName("project_id") val projectId: String = "",
    val name: String = "",
    @SerializedName("version_number") val versionNumber: String = "",
    @SerializedName("game_versions") val gameVersions: List<String> = emptyList(),
    val loaders: List<String> = emptyList(),
    @SerializedName("version_type") val versionType: String = "release", // release, beta, alpha
    val files: List<ModrinthVersionFile> = emptyList(),
    val dependencies: List<ModrinthDependency>? = null
)

/**
 * Downloadable file in Modrinth Version
 */
data class ModrinthVersionFile(
    val url: String = "",
    val filename: String = "",
    val primary: Boolean = false,
    val size: Long = 0,
    /** Sidik jari berkas dari Modrinth (kunci "sha1" dan "sha512"), dipakai untuk memastikan unduhan utuh. */
    val hashes: Map<String, String>? = null
)

/**
 * Modrinth dependency item
 */
data class ModrinthDependency(
    @SerializedName("version_id") val versionId: String? = null,
    @SerializedName("project_id") val projectId: String? = null,
    @SerializedName("file_name") val fileName: String? = null,
    @SerializedName("dependency_type") val dependencyType: String = "required" // required, optional, incompatible, embedded
)

/**
 * Representation of an installed item in instance storage
 */
data class InstalledModItem(
    val id: String,
    val filename: String,
    val originalFilename: String,
    val name: String,
    val description: String,
    val version: String,
    val category: String,
    val isEnabled: Boolean,
    val sizeBytes: Long,
    val itemType: String,
    val filePath: String = ""
)

/**
 * Manifest tracking for installed modpack files to enable selective delete and toggle
 */
data class ModpackManifest(
    @SerializedName("modpack_name") val modpackName: String,
    @SerializedName("version") val version: String = "",
    @SerializedName("archive_filename") val archiveFilename: String,
    @SerializedName("installed_files") val installedFiles: List<String> = emptyList()
)

// ============================================================================
// Section: NovaCrashCore.kt
// ============================================================================

// ----------------------------------------------------------------------------
// Section: core/crash/LauncherLog.kt
// ----------------------------------------------------------------------------

/**
 * Buku harian kejadian di dalam launcher (di luar game): galat yang tidak tertangani, layar yang dibuka,
 * pesan toast, persiapan peluncuran, unduhan, login, dan peringatan sistem.
 *
 * Log game ([GameLogArchive]) baru ada setelah Minecraft berjalan, sehingga masalah yang terjadi sebelum
 * itu (mis. unduhan gagal, runtime Java tidak siap, galat saat menekan PLAY) tidak tercatat di mana pun.
 * Buku harian ini menutup celah itu. Isinya disimpan di berkas `launcher-events.txt` (folder data
 * pribadi) dan ikut ditempel ke laporan tiap sesi game, atau dibagikan sendiri lewat Pengaturan > Log game.
 *
 * Ringan: pemanggil hanya memasukkan satu objek kecil ke antrean; penulisan berkas dikerjakan satu thread
 * latar berprioritas rendah. Semua proses (utama, game, Forge) boleh menulis ke berkas yang sama; tiap
 * baris diberi nama proses. Token dan kata sandi disamarkan sebelum ditulis. Tidak pernah melempar galat.
 */
object LauncherLog {

    const val FILE_NAME = "launcher-events.txt"

    private const val TRIM_ABOVE_BYTES = 1024L * 1024L
    private const val KEEP_BYTES = 384L * 1024L
    private const val MAX_PENDING = 2000
    private const val MAX_TEXT_CHARS = 1200
    private const val MAX_TRACE_LINES = 60
    private const val TIME_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS"
    private const val TIME_LEN = 23
    private const val CONTINUATION = "    "

    private class Entry(
        val time: Long,
        val level: Char,
        val tag: String,
        val text: String?,
        val lazyText: (() -> String)?,
        val error: Throwable?
    )

    private val queue = ConcurrentLinkedQueue<Entry>()
    private val pending = AtomicInteger(0)
    private val dropped = AtomicInteger(0)
    private val scheduled = AtomicBoolean(false)
    private val writeLock = Any()

    @Volatile private var target: File? = null
    @Volatile private var procName: String = "main"
    @Volatile private var started = false

    private val executor by lazy {
        Executors.newSingleThreadExecutor { r ->
            val t = Thread(r, "amber-launcher-log")
            t.isDaemon = true
            t.priority = Thread.MIN_PRIORITY
            t
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Memulai
    // ---------------------------------------------------------------------------------------------

    /** Dipanggil sekali dari NuxApplication.onCreate (tiap proses). Murah: pekerjaan berat di thread latar. */
    fun init(context: Context) {
        if (started) return
        started = true
        try {
            val app = context.applicationContext ?: context
            target = File(app.filesDir, FILE_NAME)
            procName = detectProcess()
            addLazy('I', "Proses") {
                "=== Proses " + procName + " dimulai: NovaCraft " + UpdateChecker.currentVersionName(app) +
                    " (build " + UpdateChecker.currentVersionCode(app) + ", " +
                    (if (UpdateChecker.wants32BitApk()) "32-bit" else "64-bit") + ") | " +
                    Build.MANUFACTURER + " " + Build.MODEL + ", Android " + Build.VERSION.RELEASE +
                    " (API " + Build.VERSION.SDK_INT + ") ==="
            }
        } catch (_: Throwable) {
        }
    }

    private fun detectProcess(): String {
        return try {
            val bytes = File("/proc/self/cmdline").inputStream().use { it.readBytes() }
            val name = String(bytes, Charsets.UTF_8).substringBefore(Char.MIN_VALUE).trim()
            if (name.contains(':')) name.substringAfter(':') else "main"
        } catch (_: Throwable) {
            "main"
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Mencatat
    // ---------------------------------------------------------------------------------------------

    /** Hanya ke logcat (tidak disimpan). Pengganti android.util.Log.d. */
    fun d(tag: String, msg: String) {
        android.util.Log.d(tag, msg)
    }

    /** Ke logcat dan buku harian. Pengganti android.util.Log.i. */
    fun i(tag: String, msg: String) {
        android.util.Log.i(tag, msg)
        add('I', tag, msg, null)
    }

    /** Ke logcat dan buku harian. Pengganti android.util.Log.w. */
    fun w(tag: String, msg: String, tr: Throwable? = null) {
        if (tr != null) android.util.Log.w(tag, msg, tr) else android.util.Log.w(tag, msg)
        add('W', tag, msg, tr)
    }

    /** Ke logcat dan buku harian. Pengganti android.util.Log.e. */
    fun e(tag: String, msg: String, tr: Throwable? = null) {
        if (tr != null) android.util.Log.e(tag, msg, tr) else android.util.Log.e(tag, msg)
        add('E', tag, msg, tr)
    }

    /** Hanya ke buku harian (tanpa logcat): kejadian biasa seperti layar dibuka atau toast. */
    fun event(tag: String, msg: String) {
        add('I', tag, msg, null)
    }

    /** Galat tak tertangani: ditulis sekarang juga (proses akan dimatikan setelah ini). */
    fun crash(thread: Thread, tr: Throwable) {
        try {
            add('E', "Crash", "Galat tak tertangani di thread \"" + thread.name + "\": " + tr.javaClass.name + ": " + tr.message, tr)
            drain()
        } catch (_: Throwable) {
        }
    }

    private fun add(level: Char, tag: String, text: String?, tr: Throwable?) {
        enqueue(Entry(System.currentTimeMillis(), level, tag, text, null, tr))
    }

    private fun addLazy(level: Char, tag: String, block: () -> String) {
        enqueue(Entry(System.currentTimeMillis(), level, tag, null, block, null))
    }

    private fun enqueue(entry: Entry) {
        try {
            if (target == null) return
            if (pending.get() >= MAX_PENDING) {
                // Terlalu banyak pesan sekaligus: pesan ini dibuang, jumlahnya dicatat saat penulisan berikutnya.
                dropped.incrementAndGet()
                return
            }
            pending.incrementAndGet()
            queue.add(entry)
            if (scheduled.compareAndSet(false, true)) {
                executor.execute {
                    scheduled.set(false)
                    drain()
                }
            }
        } catch (_: Throwable) {
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Menulis berkas
    // ---------------------------------------------------------------------------------------------

    private fun drain() {
        synchronized(writeLock) {
            val file = target ?: return
            if (queue.isEmpty()) return
            val sb = StringBuilder()
            val clock = SimpleDateFormat(TIME_PATTERN, Locale.US)
            var entry = queue.poll()
            while (entry != null) {
                pending.decrementAndGet()
                appendEntry(sb, clock, entry)
                entry = queue.poll()
            }
            val lost = dropped.getAndSet(0)
            if (lost > 0) {
                sb.append(clock.format(Date())).append(' ').append(procName)
                    .append(" W/LauncherLog: ").append(lost).append(" pesan dibuang karena terlalu banyak sekaligus\n")
            }
            try {
                FileOutputStream(file, true).use { it.write(sb.toString().toByteArray(Charsets.UTF_8)) }
                if (file.length() > TRIM_ABOVE_BYTES) trimFile(file)
            } catch (_: Throwable) {
                // Pencatatan tidak boleh mengganggu aplikasi.
            }
        }
    }

    private fun appendEntry(sb: StringBuilder, clock: SimpleDateFormat, entry: Entry) {
        val body = try {
            entry.text ?: entry.lazyText?.invoke().orEmpty()
        } catch (t: Throwable) {
            "(gagal membuat pesan: " + t.javaClass.simpleName + ")"
        }
        val head = clock.format(Date(entry.time)) + " " + procName + " " + entry.level + "/" + entry.tag + ": "
        var first = true
        for (raw in body.split('\n')) {
            val line = LogRedactor.redactLine(raw.trimEnd('\r')).take(MAX_TEXT_CHARS)
            sb.append(if (first) head else CONTINUATION).append(line).append('\n')
            first = false
        }
        val error = entry.error
        if (error != null) appendTrace(sb, error)
    }

    private fun appendTrace(sb: StringBuilder, tr: Throwable) {
        val sw = StringWriter()
        try {
            val pw = PrintWriter(sw)
            tr.printStackTrace(pw)
            pw.flush()
        } catch (_: Throwable) {
            sw.write(tr.toString())
        }
        var count = 0
        for (raw in sw.toString().split('\n')) {
            if (raw.isBlank()) continue
            val line = LogRedactor.redactLine(raw.trimEnd('\r')).take(MAX_TEXT_CHARS)
            if (count < MAX_TRACE_LINES) {
                sb.append(CONTINUATION).append(line).append('\n')
                count++
            } else if (line.startsWith("Caused by:")) {
                // Sebab asli selalu disimpan walau jejaknya panjang.
                sb.append(CONTINUATION).append(line).append('\n')
            }
        }
    }

    /**
     * Memotong berkas menjadi bagian akhirnya saja. Dikerjakan tanpa mengunci proses lain, jadi bila dua
     * proses memotong bersamaan beberapa baris bisa hilang; untuk buku harian itu tidak masalah.
     */
    private fun trimFile(file: File) {
        try {
            val len = file.length()
            if (len <= KEEP_BYTES) return
            val tail = ByteArray(KEEP_BYTES.toInt())
            RandomAccessFile(file, "r").use { raf ->
                raf.seek(len - KEEP_BYTES)
                raf.readFully(tail)
            }
            var start = 0
            val newline = '\n'.code.toByte()
            while (start < tail.size && tail[start] != newline) start++
            start++
            if (start >= tail.size) return
            FileOutputStream(file, false).use {
                it.write("(bagian lama log dipotong)\n".toByteArray(Charsets.UTF_8))
                it.write(tail, start, tail.size - start)
            }
        } catch (_: Throwable) {
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Membaca (untuk laporan)
    // ---------------------------------------------------------------------------------------------

    /**
     * Isi buku harian dengan waktu >= [sinceMillis] (0 = semuanya), dibatasi [maxBytes] bagian akhir berkas.
     * Antrean proses ini ditulis dulu supaya kejadian terbaru ikut terbaca.
     */
    fun recentText(context: Context, sinceMillis: Long, maxBytes: Long): String {
        try {
            drain()
        } catch (_: Throwable) {
        }
        return try {
            val file = File(context.applicationContext.filesDir, FILE_NAME)
            val len = file.length()
            if (len <= 0L) return ""
            val startAt = maxOf(0L, len - maxBytes)
            val buf = ByteArray((len - startAt).toInt())
            RandomAccessFile(file, "r").use { raf ->
                raf.seek(startAt)
                raf.readFully(buf)
            }
            var text = String(buf, Charsets.UTF_8)
            if (startAt > 0L) {
                val nl = text.indexOf('\n')
                if (nl >= 0) text = text.substring(nl + 1)
            }
            if (sinceMillis <= 0L) return text.trimEnd('\n')

            val clock = SimpleDateFormat(TIME_PATTERN, Locale.US)
            val out = StringBuilder()
            var keep = false
            for (line in text.split('\n')) {
                val t = parseTime(clock, line)
                if (t != null) keep = t >= sinceMillis
                if (keep) out.append(line).append('\n')
            }
            out.toString().trimEnd('\n')
        } catch (_: Throwable) {
            ""
        }
    }

    /** Mengosongkan buku harian (dipakai tombol "Hapus semua log"). */
    fun clear(context: Context) {
        try {
            synchronized(writeLock) {
                while (queue.poll() != null) pending.decrementAndGet()
                File(context.applicationContext.filesDir, FILE_NAME).delete()
            }
        } catch (_: Throwable) {
        }
    }

    private fun parseTime(clock: SimpleDateFormat, line: String): Long? {
        if (line.length <= TIME_LEN || line[4] != '-' || line[10] != ' ' || line[19] != '.') return null
        return try {
            clock.parse(line.substring(0, TIME_LEN))?.time
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Peringatan dan galat sistem milik aplikasi ini dari logcat (mencakup pesan pustaka dan jejak
     * printStackTrace yang tidak lewat buku harian). Kosong bila HP tidak mengizinkan. Jangan dipanggil
     * dari thread utama: menunggu paling lama 4 detik.
     */
    fun logcatWarnings(maxLines: Int): String {
        var proc: java.lang.Process? = null
        return try {
            val p = ProcessBuilder("logcat", "-d", "-v", "threadtime", "-t", maxLines.toString(), "*:W")
                .redirectErrorStream(true)
                .start()
            proc = p
            val watchdog = Thread {
                try {
                    Thread.sleep(4000L)
                    p.destroy()
                } catch (_: InterruptedException) {
                }
            }
            watchdog.isDaemon = true
            watchdog.start()
            val text = p.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            watchdog.interrupt()
            text
        } catch (_: Throwable) {
            ""
        } finally {
            try {
                proc?.destroy()
            } catch (_: Throwable) {
            }
        }
    }
}

// ----------------------------------------------------------------------------
// Section: core/crash/CrashManager.kt
// ----------------------------------------------------------------------------

/**
 * Data model representing a Minecraft / JVM game crash session or Launcher crash
 */
data class GameCrashInfo(
    val instanceName: String,
    val mcVersion: String,
    val exitCode: Int,
    val isSignal: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val logSnippet: String = "",
    val fullLogPath: String = "",
    val crashReportPath: String? = null,
    val crashType: String = CRASH_TYPE_GAME,
    val loader: String = "vanilla",
    val loaderVersion: String? = null,
    val installedMods: List<String> = emptyList(),
    /** Folder game (minecraft) instance yang crash; dipakai tombol pemulihan. Kosong di catatan lama. */
    val gameDirPath: String? = null,
    /** Ringkasan grafis saat crash: renderer, library, backend Minecraft 26.x, jenis permukaan. Kosong di catatan lama. */
    val graphicsInfo: String? = null
) {
    companion object {
        const val CRASH_TYPE_GAME = "GAME_CRASH"
        const val CRASH_TYPE_LAUNCHER = "LAUNCHER_CRASH"
    }

    val isLauncherCrash: Boolean
        get() = crashType == CRASH_TYPE_LAUNCHER

    fun getDisplayTitle(): String {
        return if (isLauncherCrash) {
            "NOVACRAFT CRASH"
        } else {
            "MINECRAFT MENGALAMI CRASH"
        }
    }

    fun getStatusBadgeText(): String {
        return when {
            isLauncherCrash -> "LAUNCHER EXCEPTION"
            isSignal -> CrashUtils.getSignalName(exitCode)
            else -> "EXIT CODE $exitCode"
        }
    }

    fun getMainMessage(): String {
        return when {
            isLauncherCrash -> "Peluncur mengalami kesalahan tak terduga (Uncaught Exception)."
            isSignal -> "JVM dihentikan karena sinyal fatal $exitCode (${CrashUtils.getSignalName(exitCode)})."
            else -> "JVM keluar dengan kode $exitCode (${CrashUtils.getExitCodeDescription(exitCode)})."
        }
    }
}

object CrashManager {
    private const val CRASH_FILE_NAME = "last_game_crash.json"
    private const val SESSION_FILE_NAME = "active_game_session.json"
    private const val MAX_LOG_READ_BYTES = 256L * 1024L
    private const val MAX_TRACE_CHARS = 24_000
    private val gson = Gson()

    private fun readHeadLinesBounded(file: File, maxLines: Int): List<String> {
        if (!file.exists() || !file.isFile) return emptyList()
        return try {
            val out = ArrayList<String>(maxLines.coerceAtMost(200))
            var totalBytes = 0L
            file.bufferedReader().use { reader ->
                var line = reader.readLine()
                while (line != null && out.size < maxLines && totalBytes < MAX_LOG_READ_BYTES) {
                    out.add(line)
                    totalBytes += line.length + 1L
                    line = reader.readLine()
                }
            }
            out
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun readTailLinesBounded(file: File, maxLines: Int): List<String> {
        if (!file.exists() || !file.isFile) return emptyList()
        val length = file.length()
        if (length <= 0L) return emptyList()
        return try {
            val queue = ArrayDeque<String>(maxLines + 1)
            java.io.RandomAccessFile(file, "r").use { raf ->
                val startOffset = (length - MAX_LOG_READ_BYTES).coerceAtLeast(0L)
                raf.seek(startOffset)
                if (startOffset > 0L) {
                    raf.readLine()
                }
                var line = raf.readLine()
                while (line != null) {
                    val utf8 = String(line.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8)
                    if (queue.size >= maxLines) queue.removeFirst()
                    queue.addLast(utf8)
                    line = raf.readLine()
                }
            }
            queue.toList()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    data class ActiveGameSession(
        val instanceName: String,
        val mcVersion: String,
        val rendererId: String,
        val startTime: Long = System.currentTimeMillis(),
        val gameDirPath: String? = null,
        val loader: String? = null,
        val loaderVersion: String? = null,
        val installedMods: List<String> = emptyList(),
        val graphicsInfo: String? = null
    )

    fun getInstalledMods(gameDirPath: String?): List<String> {
        if (gameDirPath.isNullOrBlank()) return emptyList()
        return try {
            val modsFolder = File(gameDirPath, "mods")
            if (!modsFolder.exists() || !modsFolder.isDirectory) return emptyList()
            modsFolder.listFiles { f ->
                f.isFile && (f.name.endsWith(".jar", ignoreCase = true) || f.name.endsWith(".disabled", ignoreCase = true))
            }?.map { it.name }?.sorted() ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun onGameSessionStarted(
        context: Context,
        instanceName: String,
        mcVersion: String,
        rendererId: String,
        gameDirPath: String? = null,
        loader: String? = null,
        loaderVersion: String? = null,
        installedMods: List<String> = emptyList(),
        graphicsInfo: String? = null
    ) {
        try {
            val resolvedMods = if (installedMods.isNotEmpty()) installedMods else getInstalledMods(gameDirPath)
            val session = ActiveGameSession(
                instanceName = instanceName,
                mcVersion = mcVersion,
                rendererId = rendererId,
                startTime = System.currentTimeMillis(),
                gameDirPath = gameDirPath,
                loader = loader,
                loaderVersion = loaderVersion,
                installedMods = resolvedMods,
                graphicsInfo = graphicsInfo
            )
            val file = File(context.filesDir, SESSION_FILE_NAME)
            file.writeText(gson.toJson(session))
        } catch (_: Exception) {}
    }

    fun onGameSessionEnded(context: Context) {
        try {
            val file = File(context.filesDir, SESSION_FILE_NAME)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {}
    }

    private val _activeCrash = MutableStateFlow<GameCrashInfo?>(null)
    val activeCrash: StateFlow<GameCrashInfo?> = _activeCrash.asStateFlow()

    /**
     * Merekam crash (Game JVM ataupun Launcher)
     */
    fun recordCrash(
        context: Context,
        instanceName: String,
        mcVersion: String,
        exitCode: Int,
        isSignal: Boolean,
        gameDirPath: String,
        liveLogs: List<String> = emptyList(),
        exceptionDetail: String? = null,
        crashType: String = GameCrashInfo.CRASH_TYPE_GAME,
        loader: String = "vanilla",
        loaderVersion: String? = null,
        installedMods: List<String> = emptyList(),
        graphicsInfo: String? = null
    ) {
        try {
            val filesDir = context.filesDir

            // 1. Cari crash report resmi dari folder crash-reports di gameDir
            val crashReportsDir = File(gameDirPath, "crash-reports")
            val latestCrashReport = if (crashReportsDir.exists() && crashReportsDir.isDirectory) {
                crashReportsDir.listFiles { f -> f.isFile && f.name.startsWith("crash-") && f.name.endsWith(".txt") }
                    ?.maxByOrNull { it.lastModified() }
            } else null

            // Cek apakah crash report dibuat baru-baru ini (kurang dari 15 menit lalu)
            val isRecentReport = latestCrashReport != null && (System.currentTimeMillis() - latestCrashReport.lastModified() < 900_000L)
            val logFile = File(filesDir, "latestlog.txt")

            val snippet = when {
                isRecentReport && latestCrashReport != null -> {
                    readHeadLinesBounded(latestCrashReport, 150).joinToString("\n")
                }
                exceptionDetail != null -> {
                    exceptionDetail.take(MAX_TRACE_CHARS)
                }
                logFile.exists() -> {
                    readTailLinesBounded(logFile, 100).joinToString("\n")
                }
                liveLogs.isNotEmpty() -> {
                    liveLogs.takeLast(80).joinToString("\n")
                }
                else -> {
                    "JVM keluar dengan kode $exitCode (Signal: $isSignal).\nPeriksa file log untuk rincian lebih lanjut."
                }
            }

            val targetLogPath = when {
                isRecentReport && latestCrashReport != null -> latestCrashReport.absolutePath
                logFile.exists() -> logFile.absolutePath
                else -> ""
            }

            val resolvedMods = if (installedMods.isNotEmpty()) installedMods else getInstalledMods(gameDirPath)

            val crashInfo = GameCrashInfo(
                instanceName = instanceName,
                mcVersion = mcVersion,
                exitCode = exitCode,
                isSignal = isSignal,
                timestamp = System.currentTimeMillis(),
                logSnippet = snippet,
                fullLogPath = targetLogPath,
                crashReportPath = if (isRecentReport && latestCrashReport != null) latestCrashReport.absolutePath else null,
                crashType = crashType,
                loader = loader,
                loaderVersion = loaderVersion,
                installedMods = resolvedMods,
                gameDirPath = gameDirPath.takeIf { it.isNotBlank() },
                graphicsInfo = graphicsInfo
            )

            val crashFile = File(filesDir, CRASH_FILE_NAME)
            crashFile.writeText(gson.toJson(crashInfo))
            _activeCrash.value = crashInfo
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Merekam crash yang terjadi pada aplikasi Launcher itu sendiri (Uncaught Exception)
     */
    fun recordLauncherCrash(context: Context, throwable: Throwable) {
        try {
            val filesDir = context.filesDir
            val launcherCrashFile = File(filesDir, "launcher-crash.txt")
            val rawTrace = android.util.Log.getStackTraceString(throwable)
            val stackTrace = if (rawTrace.length > MAX_TRACE_CHARS) {
                rawTrace.take(MAX_TRACE_CHARS) + "\n...[dipotong]"
            } else {
                rawTrace
            }
            launcherCrashFile.writeText(
                "=== NovaCraft Crash Report ===\n" +
                "Time: ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date())}\n" +
                "Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})\n\n" +
                stackTrace
            )

            val crashInfo = GameCrashInfo(
                instanceName = "NovaCraft",
                mcVersion = "App",
                exitCode = -1,
                isSignal = false,
                timestamp = System.currentTimeMillis(),
                logSnippet = stackTrace,
                fullLogPath = launcherCrashFile.absolutePath,
                crashReportPath = launcherCrashFile.absolutePath,
                crashType = GameCrashInfo.CRASH_TYPE_LAUNCHER
            )

            val crashFile = File(filesDir, CRASH_FILE_NAME)
            crashFile.writeText(gson.toJson(crashInfo))
            _activeCrash.value = crashInfo
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    /**
     * Memeriksa apakah ada file status crash saat MainActivity resume
     */
    fun checkAndNotify(context: Context) {
        try {
            val crashFile = File(context.filesDir, CRASH_FILE_NAME)
            if (crashFile.exists() && crashFile.length() > 0) {
                val json = crashFile.readText()
                val info = gson.fromJson(json, GameCrashInfo::class.java)
                if (info != null) {
                    _activeCrash.value = info
                    return
                }
            }

            // Deteksi jika sesi game mati mendadak (Native Abort / SIGABRT / SIGSEGV / OOM Killer)
            val sessionFile = File(context.filesDir, SESSION_FILE_NAME)
            if (sessionFile.exists()) {
                try {
                    val sessionJson = sessionFile.readText()
                    val session = gson.fromJson(sessionJson, ActiveGameSession::class.java)
                    sessionFile.delete() // hapus agar tidak muncul berulang

                    val logFile = File(context.filesDir, "latestlog.txt")
                    val tail = if (logFile.exists()) {
                        readTailLinesBounded(logFile, 100).joinToString("\n")
                    } else {
                        ""
                    }

                    // 1. Cek apakah ada file crash-report resmi di folder gameDir instance yang aktif
                    val targetGameDir = session?.gameDirPath?.let { File(it) }
                    val crashReportsDir = if (targetGameDir != null) File(targetGameDir, "crash-reports") else null
                    val recentCrashFile = if (crashReportsDir != null && crashReportsDir.exists()) {
                        crashReportsDir.listFiles { f -> f.isFile && f.name.startsWith("crash-") && f.name.endsWith(".txt") }
                            ?.filter { it.lastModified() >= (session.startTime - 15000L) }
                            ?.maxByOrNull { it.lastModified() }
                    } else null

                    // 2. Cek apakah ada hs_err_pid JVM fatal error log di folder gameDir instance yang aktif
                    val recentHsErrFile = if (targetGameDir != null && targetGameDir.exists()) {
                        targetGameDir.listFiles { f -> f.isFile && f.name.startsWith("hs_err_pid") && f.name.endsWith(".log") }
                            ?.filter { it.lastModified() >= (session.startTime - 15000L) }
                            ?.maxByOrNull { it.lastModified() }
                    } else null

                    val snippet = when {
                        recentCrashFile != null -> {
                            readHeadLinesBounded(recentCrashFile, 120).joinToString("\n")
                        }
                        recentHsErrFile != null -> {
                            readHeadLinesBounded(recentHsErrFile, 80).joinToString("\n")
                        }
                        tail.isNotBlank() -> tail
                        else -> "Proses game terhenti mendadak (Kemungkinan dihentikan sistem karena kehabisan RAM atau crash driver native)."
                    }

                    var exitCode = when {
                        recentHsErrFile != null -> {
                            val firstLines = readHeadLinesBounded(recentHsErrFile, 10).joinToString(" ")
                            if (firstLines.contains("SIGSEGV", ignoreCase = true)) 139
                            else if (firstLines.contains("SIGABRT", ignoreCase = true)) 134
                            else 139
                        }
                        tail.contains("fatal signal 11", ignoreCase = true) || tail.contains("SIGSEGV", ignoreCase = true) -> 139
                        tail.contains("fatal signal 6", ignoreCase = true) || tail.contains("SIGABRT", ignoreCase = true) -> 134
                        else -> 137 // SIGKILL / OOM default jika sesi terputus tanpa exit code
                    }

                    val fullLogPath = when {
                        recentCrashFile != null -> recentCrashFile.absolutePath
                        recentHsErrFile != null -> recentHsErrFile.absolutePath
                        logFile.exists() -> logFile.absolutePath
                        else -> ""
                    }

                    val resolvedSessionMods = if (!session?.installedMods.isNullOrEmpty()) {
                        session?.installedMods ?: emptyList()
                    } else {
                        getInstalledMods(session?.gameDirPath)
                    }

                    val crashInfo = GameCrashInfo(
                        instanceName = session?.instanceName ?: "Minecraft",
                        mcVersion = session?.mcVersion ?: "Unknown",
                        exitCode = exitCode,
                        isSignal = true,
                        timestamp = System.currentTimeMillis(),
                        logSnippet = snippet,
                        fullLogPath = fullLogPath,
                        crashReportPath = recentCrashFile?.absolutePath ?: recentHsErrFile?.absolutePath,
                        crashType = GameCrashInfo.CRASH_TYPE_GAME,
                        loader = session?.loader ?: "vanilla",
                        loaderVersion = session?.loaderVersion,
                        installedMods = resolvedSessionMods,
                        gameDirPath = session?.gameDirPath,
                        graphicsInfo = session?.graphicsInfo
                    )

                    val cf = File(context.filesDir, CRASH_FILE_NAME)
                    cf.writeText(gson.toJson(crashInfo))
                    _activeCrash.value = crashInfo
                    return
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Fallback: periksa file crash report terbaru di folder instance yang sedang dipilih (bukan seluruh instance)
            val inst = InstanceManager.selectedInstance.value
            if (inst != null) {
                val instDir = File(InstanceManager.getInstancesDir(context), inst.id)
                val instGameDir = File(instDir, "minecraft")
                val crashReportsDir = File(instGameDir, "crash-reports")
                if (crashReportsDir.exists()) {
                    val recentCrash = crashReportsDir.listFiles { f -> f.isFile && f.name.startsWith("crash-") && f.name.endsWith(".txt") }
                        ?.maxByOrNull { it.lastModified() }

                    if (recentCrash != null && (System.currentTimeMillis() - recentCrash.lastModified() < 900_000L)) {
                        val marker = File(context.filesDir, "seen_${recentCrash.name}")
                        if (!marker.exists()) {
                            val snippet = readHeadLinesBounded(recentCrash, 150).joinToString("\n")
                            val fallbackMods = getInstalledMods(instGameDir.absolutePath)
                            val info = GameCrashInfo(
                                instanceName = inst.name,
                                mcVersion = inst.mcVersion,
                                exitCode = 1,
                                isSignal = false,
                                timestamp = recentCrash.lastModified(),
                                logSnippet = snippet,
                                fullLogPath = recentCrash.absolutePath,
                                crashReportPath = recentCrash.absolutePath,
                                crashType = GameCrashInfo.CRASH_TYPE_GAME,
                                loader = inst.loader,
                                loaderVersion = inst.loaderVersion,
                                installedMods = fallbackMods,
                                gameDirPath = instGameDir.absolutePath
                            )
                            _activeCrash.value = info
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Membaca catatan crash terakhir dari berkas tanpa mengubah status apa pun. Dipakai halaman Error
     * mandiri: game berjalan di proses lain, jadi catatannya hanya bisa diambil dari berkas.
     */
    fun readSavedCrash(context: Context): GameCrashInfo? {
        return try {
            val crashFile = File(context.filesDir, CRASH_FILE_NAME)
            if (crashFile.exists() && crashFile.length() > 0) {
                gson.fromJson(crashFile.readText(), GameCrashInfo::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Menghapus status crash setelah ditutup oleh user
     */
    fun dismissCrash(context: Context) {
        val current = _activeCrash.value
        _activeCrash.value = null
        try {
            val crashFile = File(context.filesDir, CRASH_FILE_NAME)
            if (crashFile.exists()) {
                crashFile.delete()
            }
            if (current?.crashReportPath != null) {
                val reportFile = File(current.crashReportPath)
                val marker = File(context.filesDir, "seen_${reportFile.name}")
                marker.createNewFile()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

// ----------------------------------------------------------------------------
// Section: core/crash/CrashRecovery.kt
// ----------------------------------------------------------------------------

/** Perbaikan sekali ketuk yang ditawarkan setelah game crash. */
enum class RecoveryAction { LOWER_RAM, RESET_RENDERER, DISABLE_SHADERS, DISABLE_MODS, RESET_JAVA }

/**
 * Satu tawaran perbaikan. [label] dan [detail] ditulis dalam bahasa Indonesia dan diterjemahkan
 * lewat kamus saat ditampilkan. [arg] membawa nilai yang akan diterapkan: RAM baru (MB), id
 * renderer, atau daftar nama berkas mod dipisah tanda "|".
 */
data class RecoveryOption(
    val action: RecoveryAction,
    val label: String,
    val detail: String,
    val arg: String = ""
)

data class RecoveryResult(val ok: Boolean, val message: String)

/**
 * Pemulihan sekali ketuk setelah crash. Hanya menawarkan perbaikan yang masuk akal untuk dugaan
 * penyebab crash ([CrashAdvisor.sourceOf]), dan semuanya bisa dibatalkan pengguna: RAM dan renderer
 * tinggal diubah lagi di Pengaturan, mod hanya diberi akhiran ".disabled" (diaktifkan lagi di menu
 * Mod), dan shader tinggal dinyalakan lagi dari menu video game.
 *
 * Fungsi [suggest] dan [apply] membaca atau menulis berkas, jadi panggil dari thread latar.
 */
object CrashRecovery {

    private const val RAM_STEP_MB = 256
    private const val RAM_ROUND_MB = 128
    private const val RAM_MIN_MB = 512
    private const val MAX_SUSPECT_MODS = 3

    private val BUILTIN_RENDERER_IDS = setOf("auto", "krypton", "mobileglues", "gl4es", "zink", "freedreno")

    /** Berkas pengaturan shader (relatif terhadap folder game) yang memakai kunci enableShaders. */
    private val SHADER_PROPERTY_FILES = listOf("config/iris.properties", "config/oculus.properties")
    private const val OPTIFINE_SHADER_FILE = "optionsshaders.txt"

    /** Id mod yang tidak pernah dianggap tersangka (pustaka inti: mematikannya membuat game makin rusak). */
    private val PROTECTED_MOD_IDS = setOf(
        "minecraft", "java", "fabricloader", "fabric", "fabric-api", "forge", "neoforge", "quilt_loader", "mixinextras"
    )

    private val MOD_ID_PATTERNS = listOf(
        Regex("""Mod ID: '([A-Za-z0-9_.\-]+)'"""),
        Regex("""Mixin apply for mod ([A-Za-z0-9_.\-]+) failed"""),
        Regex("""Mod '[^']*' \(([A-Za-z0-9_.\-]+)\)""")
    )

    // ---------------------------------------------------------------- logika murni (mudah diuji)

    /** True bila heap Java kehabisan ruang (bukan RAM HP): menurunkan RAM justru memperburuk. */
    fun isJavaHeapOutOfMemory(log: String): Boolean {
        return log.contains("OutOfMemoryError: Java heap space") ||
            log.contains("GC overhead limit exceeded")
    }

    /**
     * RAM game baru (MB) bila diturunkan 256 MB dari heap yang dipilih ([requestedMb]),
     * atau null bila tidak ada ruang untuk turun.
     */
    fun loweredRamMb(requestedMb: Int): Int? {
        val target = ((requestedMb - RAM_STEP_MB) / RAM_ROUND_MB * RAM_ROUND_MB).coerceAtLeast(RAM_MIN_MB)
        return if (target < requestedMb) target else null
    }

    /** True bila log menyinggung shader sehingga mematikannya masuk akal. */
    fun mentionsShaders(log: String): Boolean {
        return log.contains("shader", ignoreCase = true) ||
            log.contains("iris", ignoreCase = true) ||
            log.contains("oculus", ignoreCase = true) ||
            log.contains("optifine", ignoreCase = true)
    }

    /** True bila log menunjukkan versi Java tidak cocok dengan game atau mod. */
    fun mentionsJavaMismatch(log: String): Boolean {
        return log.contains("UnsupportedClassVersionError") ||
            log.contains("Unsupported class file major version") ||
            log.contains("Unsupported major.minor version") ||
            log.contains("compiled by a more recent version of the Java Runtime")
    }

    /** Teks iris.properties/oculus.properties dengan shader dimatikan, atau null bila tidak perlu diubah. */
    fun disableShaderProperty(text: String): String? {
        val lines = text.split("\n")
        var changed = false
        val out = lines.map { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("enableShaders") && trimmed.substringAfter("=", "").trim().equals("true", ignoreCase = true)) {
                changed = true
                "enableShaders=false"
            } else {
                line
            }
        }
        return if (changed) out.joinToString("\n") else null
    }

    /** Teks optionsshaders.txt (OptiFine) dengan shader dimatikan, atau null bila tidak perlu diubah. */
    fun disableOptifineShader(text: String): String? {
        val lines = text.split("\n")
        var changed = false
        val out = lines.map { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("shaderPack=")) {
                val value = trimmed.substringAfter("=").trim()
                if (value.isNotEmpty() && !value.equals("OFF", ignoreCase = true)) {
                    changed = true
                    "shaderPack=OFF"
                } else {
                    line
                }
            } else {
                line
            }
        }
        return if (changed) out.joinToString("\n") else null
    }

    /** Nama dasar berkas mod tanpa versi, mis. "sodium-fabric-0.5.8+mc1.20.1.jar" -> "sodium-fabric". */
    fun modBaseName(fileName: String): String {
        var base = fileName.lowercase().removeSuffix(".disabled").removeSuffix(".jar")
        val cut = Regex("""[-_+ ]v?\d""").find(base)
        if (cut != null && cut.range.first > 0) base = base.substring(0, cut.range.first)
        return base
    }

    /**
     * Mod aktif yang disebut di [log]: nama berkasnya muncul utuh, atau id mod yang disebut crash
     * report cocok dengan nama berkasnya. Paling banyak [MAX_SUSPECT_MODS]; lebih dari itu dianggap
     * terlalu tidak pasti, hasilnya kosong. Pustaka inti (mis. Fabric API) tidak pernah dipilih.
     */
    fun findSuspectMods(log: String, installedMods: List<String>): List<String> {
        val enabled = installedMods.filter { it.endsWith(".jar", ignoreCase = true) }
        if (enabled.isEmpty() || log.isBlank()) return emptyList()

        val ids = HashSet<String>()
        for (pattern in MOD_ID_PATTERNS) {
            for (m in pattern.findAll(log)) {
                val id = m.groupValues[1].lowercase()
                if (id.length >= 3 && id !in PROTECTED_MOD_IDS) ids.add(id)
            }
        }

        val found = LinkedHashSet<String>()
        for (name in enabled) {
            val base = modBaseName(name)
            if (isProtectedJar(base)) continue
            if (log.contains(name)) {
                found.add(name)
                continue
            }
            val squashed = base.replace(Regex("""[^a-z0-9]"""), "")
            for (id in ids) {
                val idSquashed = id.replace(Regex("""[^a-z0-9]"""), "")
                if (base == id || base.startsWith("$id-") || base.startsWith("${id}_") || (idSquashed.length >= 4 && squashed == idSquashed)) {
                    found.add(name)
                    break
                }
            }
        }
        return if (found.size in 1..MAX_SUSPECT_MODS) found.toList() else emptyList()
    }

    private fun isProtectedJar(base: String): Boolean {
        return base.startsWith("fabric-api") || base == "fabricapi" || base.startsWith("forgified-fabric-api") ||
            base.startsWith("quilted-fabric-api") || base == "fabric-language-kotlin"
    }

    /**
     * Renderer lain yang layak dicoba setelah [currentId] crash: urutan Krypton, MobileGlues, GL4ES,
     * hanya yang pustakanya ada di HP ini dan mendukung versi Minecraft-nya. Null bila tidak ada pilihan lain.
     */
    fun alternativeRenderer(currentId: String, chain: List<NuxRendererInfo>): NuxRendererInfo? {
        if (chain.size < 2) return null
        val idx = chain.indexOfFirst { it.id == currentId }
        return if (idx < 0) chain.first() else chain[(idx + 1) % chain.size]
    }

    // ---------------------------------------------------------------- tawaran dan penerapan

    /** Perbaikan yang layak ditawarkan untuk crash ini. Kosong untuk crash launcher. */
    fun suggest(context: Context, info: GameCrashInfo, logText: String): List<RecoveryOption> {
        if (info.isLauncherCrash) return emptyList()
        val options = ArrayList<RecoveryOption>()
        try {
            // Halaman Error bisa berjalan di proses yang baru dibuka: muat setelan dari berkas dulu.
            SettingsManager.init(context)
            val log = if (logText.isNotBlank()) logText else info.logSnippet
            val source = CrashAdvisor.sourceOf(info)
            val settings = SettingsManager.settings.value
            val gameDir = info.gameDirPath?.takeIf { it.isNotBlank() }?.let { File(it) }?.takeIf { it.isDirectory }

            // 1. RAM: hanya bila HP kehabisan RAM (bukan heap Java yang sempit)
            if (source == CrashSource.MEMORY && !isJavaHeapOutOfMemory(log)) {
                val target = loweredRamMb(settings.ramMb)
                if (target != null) {
                    options.add(
                        RecoveryOption(
                            RecoveryAction.LOWER_RAM,
                            "Turunkan RAM ke $target MB",
                            "Memberi ruang lebih bagi HP. Bisa dinaikkan lagi di Pengaturan > Memori.",
                            target.toString()
                        )
                    )
                }
            }

            // 2. Renderer: hanya untuk crash grafis
            if (source == CrashSource.GRAPHICS) {
                rendererOption(context, info, settings.selectedRenderer)?.let { options.add(it) }
            }

            // 3. Shader: bila masih menyala dan crash berkaitan dengan grafis, memori, atau log menyebut shader
            if (gameDir != null && shadersEnabled(gameDir) &&
                (source == CrashSource.GRAPHICS || source == CrashSource.MEMORY || mentionsShaders(log))
            ) {
                options.add(
                    RecoveryOption(
                        RecoveryAction.DISABLE_SHADERS,
                        "Matikan shader",
                        "Shader membebani GPU dan memori. Bisa dinyalakan lagi dari menu video game."
                    )
                )
            }

            // 4. Mod yang disebut di log
            if (gameDir != null) {
                val suspects = findSuspectMods(log, info.installedMods).filter { File(File(gameDir, "mods"), it).isFile }
                if (suspects.isNotEmpty()) {
                    options.add(
                        RecoveryOption(
                            RecoveryAction.DISABLE_MODS,
                            "Nonaktifkan mod tersangka",
                            "Mod yang disebut di log: ${suspects.joinToString(", ")}. Bisa diaktifkan lagi di menu Mod.",
                            suspects.joinToString("|")
                        )
                    )
                }
            }

            // 5. Java: bila versi dipilih manual dan log menunjukkan ketidakcocokan
            if ((mentionsJavaMismatch(log) || source == CrashSource.GAME_OR_MODS) && javaIsManual(info)) {
                options.add(
                    RecoveryOption(
                        RecoveryAction.RESET_JAVA,
                        "Reset Java ke Otomatis",
                        "Versi Java yang dipilih mungkin tidak cocok dengan game ini. Otomatis memilih versi yang sesuai."
                    )
                )
            }
        } catch (_: Throwable) {
            // Tawaran pemulihan hanya bonus: kegagalan di sini tidak boleh mengganggu panel crash.
        }
        return options
    }

    fun apply(context: Context, info: GameCrashInfo, option: RecoveryOption): RecoveryResult {
        return try {
            when (option.action) {
                RecoveryAction.LOWER_RAM -> {
                    val mb = option.arg.toIntOrNull() ?: return RecoveryResult(false, FAIL_MESSAGE)
                    SettingsManager.init(context)
                    SettingsManager.updateSettings(context, SettingsManager.settings.value.copy(ramMb = mb))
                    RecoveryResult(true, "RAM game diturunkan ke $mb MB.")
                }
                RecoveryAction.RESET_RENDERER -> {
                    val id = option.arg.ifBlank { "auto" }
                    SettingsManager.init(context)
                    SettingsManager.updateSettings(context, SettingsManager.settings.value.copy(selectedRenderer = id))
                    if (id == "auto") {
                        RecoveryResult(true, "Renderer dikembalikan ke Otomatis.")
                    } else {
                        RecoveryResult(true, "Renderer diganti ke ${NuxRendererRegistry.findRendererById(id).displayName}.")
                    }
                }
                RecoveryAction.DISABLE_SHADERS -> {
                    val gameDir = info.gameDirPath?.let { File(it) } ?: return RecoveryResult(false, FAIL_MESSAGE)
                    if (disableShaders(gameDir) > 0) RecoveryResult(true, "Shader dimatikan.") else RecoveryResult(false, FAIL_MESSAGE)
                }
                RecoveryAction.DISABLE_MODS -> {
                    val gameDir = info.gameDirPath?.let { File(it) } ?: return RecoveryResult(false, FAIL_MESSAGE)
                    val done = disableMods(gameDir, option.arg.split("|").filter { it.isNotBlank() })
                    if (done.isNotEmpty()) {
                        RecoveryResult(true, "Mod dinonaktifkan: ${done.joinToString(", ")}.")
                    } else {
                        RecoveryResult(false, FAIL_MESSAGE)
                    }
                }
                RecoveryAction.RESET_JAVA -> {
                    resetJava(context, info)
                    RecoveryResult(true, "Java dikembalikan ke Otomatis.")
                }
            }
        } catch (_: Throwable) {
            RecoveryResult(false, FAIL_MESSAGE)
        }
    }

    private const val FAIL_MESSAGE = "Gagal menerapkan. Coba lewat Pengaturan."

    // ---------------------------------------------------------------- bagian yang menyentuh berkas

    private fun rendererOption(context: Context, info: GameCrashInfo, selectedId: String): RecoveryOption? {
        val nativeDir = File(context.applicationInfo.nativeLibraryDir)
        val selected = selectedId.trim().ifEmpty { "auto" }.lowercase()

        // Renderer tambahan (plugin) atau pilihan manual yang tidak dikenal: kembali ke Otomatis.
        if (selected !in BUILTIN_RENDERER_IDS) {
            return resetToAuto("Renderer pilihanmu bisa jadi kurang cocok dengan HP ini. Otomatis memilih yang paling sesuai.")
        }

        val current = NuxRendererRegistry.resolveRenderer(selected, info.mcVersion, nativeDir, null)
        // Pilihan manual yang berbeda dari pilihan Otomatis: kembali ke Otomatis dulu.
        if (selected != "auto") {
            val auto = NuxRendererRegistry.resolveRenderer("auto", info.mcVersion, nativeDir, null)
            if (auto.id != current.id) {
                return resetToAuto("Renderer pilihanmu bisa jadi kurang cocok dengan HP ini. Otomatis memilih yang paling sesuai.")
            }
        }

        val chain = listOf(
            NuxRendererRegistry.RENDERER_KRYPTON,
            NuxRendererRegistry.RENDERER_MOBILEGLUES,
            NuxRendererRegistry.RENDERER_GL4ES
        ).filter { File(nativeDir, it.libraryName).exists() && NuxVersionUtils.isRendererSupported(it, info.mcVersion) }
        val alt = alternativeRenderer(current.id, chain) ?: return null
        val name = alt.displayName.substringBefore(" (")
        return RecoveryOption(
            RecoveryAction.RESET_RENDERER,
            "Coba renderer $name",
            "Renderer sekarang tampaknya bermasalah di HP ini. Bisa diganti lagi di Pengaturan > Grafis.",
            alt.id
        )
    }

    private fun resetToAuto(detail: String) = RecoveryOption(
        RecoveryAction.RESET_RENDERER,
        "Pakai renderer Otomatis",
        detail,
        "auto"
    )

    private fun shadersEnabled(gameDir: File): Boolean {
        for (rel in SHADER_PROPERTY_FILES) {
            val f = File(gameDir, rel)
            if (f.isFile && f.length() < 65_536 && disableShaderProperty(f.readText()) != null) return true
        }
        val of = File(gameDir, OPTIFINE_SHADER_FILE)
        return of.isFile && of.length() < 65_536 && disableOptifineShader(of.readText()) != null
    }

    /** Mematikan shader di semua berkas pengaturan yang dikenal. Mengembalikan jumlah berkas yang diubah. */
    fun disableShaders(gameDir: File): Int {
        var changed = 0
        for (rel in SHADER_PROPERTY_FILES) {
            val f = File(gameDir, rel)
            if (!f.isFile || f.length() >= 65_536) continue
            val updated = disableShaderProperty(f.readText()) ?: continue
            f.writeText(updated)
            changed++
        }
        val of = File(gameDir, OPTIFINE_SHADER_FILE)
        if (of.isFile && of.length() < 65_536) {
            val updated = disableOptifineShader(of.readText())
            if (updated != null) {
                of.writeText(updated)
                changed++
            }
        }
        return changed
    }

    /** Memberi akhiran ".disabled" pada mod yang disebut. Mengembalikan nama berkas yang berhasil. */
    fun disableMods(gameDir: File, names: List<String>): List<String> {
        val modsDir = File(gameDir, "mods")
        val done = ArrayList<String>()
        for (raw in names) {
            // Hanya nama berkas biasa di folder mods, tidak boleh berisi jalur.
            val name = File(raw).name
            if (name != raw || !name.endsWith(".jar", ignoreCase = true)) continue
            val src = File(modsDir, name)
            val dst = File(modsDir, "$name.disabled")
            if (src.isFile && !dst.exists() && src.renameTo(dst)) done.add(name)
        }
        return done
    }

    private fun resolveInstanceId(info: GameCrashInfo): String? {
        val dir = info.gameDirPath?.takeIf { it.isNotBlank() }?.let { File(it) }
        return dir?.parentFile?.name?.takeIf { it.isNotBlank() }
    }

    private fun instanceJavaRuntime(info: GameCrashInfo): String? {
        val instDir = info.gameDirPath?.takeIf { it.isNotBlank() }?.let { File(it).parentFile } ?: return null
        val f = File(instDir, "instance.json")
        if (!f.isFile || f.length() > 65_536) return null
        return try {
            JsonParser.parseString(f.readText()).asJsonObject.get("javaRuntime")?.asString
        } catch (_: Throwable) {
            null
        }
    }

    private fun javaIsManual(info: GameCrashInfo): Boolean {
        if (!SettingsManager.settings.value.defaultJavaRuntime.equals("auto", ignoreCase = true)) return true
        val runtime = instanceJavaRuntime(info) ?: return false
        return !runtime.equals("auto", ignoreCase = true)
    }

    private fun resetJava(context: Context, info: GameCrashInfo) {
        SettingsManager.init(context)
        SettingsManager.updateSettings(context, SettingsManager.settings.value.copy(defaultJavaRuntime = "auto"))
        // Daftar instance dimuat dulu supaya menyimpan satu instance tidak menimpa daftar dengan isi kosong.
        InstanceManager.init(context)
        if (InstanceManager.instances.value.isEmpty()) return
        val id = resolveInstanceId(info)
        val inst = InstanceManager.instances.value.firstOrNull { (id != null && it.id == id) || it.name == info.instanceName }
        if (inst != null && !inst.javaRuntime.equals("auto", ignoreCase = true)) {
            InstanceManager.updateInstance(context, inst.copy(javaRuntime = "auto"))
        }
    }
}

// ----------------------------------------------------------------------------
// Section: core/crash/GameLogArchive.kt
// ----------------------------------------------------------------------------

/**
 * Menyimpan log setiap sesi game ke folder `logs` di data launcher, supaya log tidak hilang saat
 * game dijalankan lagi (berkas log sesi berjalan selalu ditimpa) dan bisa dibagikan dengan satu ketukan
 * dari Pengaturan > Log game.
 *
 * Tiap berkas berisi ringkasan sesi (versi launcher, HP, instance, mod, shader), kejadian di dalam launcher
 * sebelum dan selama sesi itu ([LauncherLog]), lalu log lengkapnya, sudah tanpa token atau kata sandi.
 * Disimpan saat game keluar; bila proses game dimatikan Android sebelum sempat menyimpan, sesi itu
 * disimpan pada peluncuran berikutnya atau saat Log game dibuka.
 *
 * Masalah yang terjadi di luar game (game belum sempat berjalan) tidak punya log game; untuk itu ada
 * laporan launcher ([saveLauncherReport]) yang berisi ringkasan perangkat, kejadian launcher, dan
 * peringatan sistem.
 *
 * Aman dipanggil dari proses game maupun proses launcher.
 */
object GameLogArchive {

    private const val KEEP_COUNT = 10
    private const val KEEP_LAUNCHER_COUNT = 3
    private const val LAUNCHER_LOOKBACK_MS = 20L * 60L * 1000L
    private const val MAX_LAUNCHER_BYTES = 160L * 1024L
    private const val MAX_LAUNCHER_REPORT_BYTES = 320L * 1024L
    private const val LOGCAT_LINES = 500
    private const val MAX_LOG_BYTES = 4L * 1024L * 1024L
    private const val HEAD_BYTES = 192L * 1024L
    private const val MAX_EXTRA_BYTES = 128L * 1024L
    private const val MAX_CONFIG_BYTES = 8L * 1024L
    private const val MAX_LISTED = 80
    private const val MAX_ENGINE_LOG_BYTES = 160L * 1024L
    private const val MAX_ENGINE_SHADER_LINES = 80
    private val ENGINE_SETTING_KEYS = listOf(
        "renderer", "render_scale", "super_perf_mode", "battery_perf_mode", "max_fps_mode",
        "disable_game_animations", "fast_chunk_upload", "experimental_leaf_culling", "record_performance",
        "performance_hints", "worker_limit", "cache_budget_mib", "profile"
    )

    private const val LOG_NAME = "latestlog.txt"
    private const val META_NAME = "last-session.properties"
    const val ARCHIVE_PREFIX = "amber-log_"

    /** Pengganti nama instance pada nama berkas laporan launcher (tanda kurung tidak mungkin muncul dari nama instance). */
    private const val LAUNCHER_TAG = "(launcher)"

    private const val KEY_START = "start"
    private const val KEY_INSTANCE = "instance"
    private const val KEY_MC = "mc"
    private const val KEY_LOADER = "loader"
    private const val KEY_LOADER_VERSION = "loaderVersion"
    private const val KEY_GAME_DIR = "gameDir"
    private const val KEY_MG_DIR = "mgDir"
    private const val KEY_ARCHIVED = "archived"

    private val metaLock = Any()

    /** Folder tempat log disimpan (terlihat di pengelola berkas: nuxlauncher/logs). */
    fun logsDir(context: Context): File = File(InstanceManager.getNuxDir(context), "logs")

    /** Log yang tersimpan, yang terbaru di depan. */
    fun list(context: Context): List<File> =
        logsDir(context)
            .listFiles { f -> f.isFile && f.name.startsWith(ARCHIVE_PREFIX) && f.name.endsWith(".txt") }
            ?.sortedByDescending { it.lastModified() }
            .orEmpty()

    /** Apakah berkas ini laporan launcher (bukan log sesi game). */
    fun isLauncherReport(file: File): Boolean = file.name.endsWith("_" + LAUNCHER_TAG + ".txt")

    /**
     * Pecah nama berkas menjadi (waktu, nama instance) untuk ditampilkan.
     * Nama berkas: amber-log_2026-10-26_06-30-11_NamaInstance.txt
     */
    fun describe(file: File): Pair<String, String> {
        val core = file.name.removePrefix(ARCHIVE_PREFIX).removeSuffix(".txt")
        val parts = core.split('_', limit = 3)
        if (parts.size < 3) return core to ""
        val time = parts[0] + " " + parts[1].take(5).replace('-', ':')
        return time to parts[2]
    }

    // ---------------------------------------------------------------------------------------------
    // Pencatatan sesi
    // ---------------------------------------------------------------------------------------------

    /** Dipanggil saat sesi game dimulai: mencatat sesi ini sebagai "belum disimpan". */
    fun beginSession(
        context: Context,
        startTime: Long,
        instance: String,
        mcVersion: String,
        loader: String,
        loaderVersion: String?,
        gameDir: String
    ) {
        val props = Properties()
        props.setProperty(KEY_START, startTime.toString())
        props.setProperty(KEY_INSTANCE, instance)
        props.setProperty(KEY_MC, mcVersion)
        props.setProperty(KEY_LOADER, loader)
        props.setProperty(KEY_LOADER_VERSION, loaderVersion.orEmpty())
        props.setProperty(KEY_GAME_DIR, gameDir)
        writeMeta(context, props)
    }

    /** Mencatat folder log MobileGlues bila renderer memakainya (dibaca dari lingkungan proses game). */
    fun noteMgDir(context: Context, dir: String) {
        synchronized(metaLock) {
            val props = readMeta(context) ?: return
            props.setProperty(KEY_MG_DIR, dir)
            writeMeta(context, props)
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Menyimpan log
    // ---------------------------------------------------------------------------------------------

    /** Menyimpan sesi terakhir bila belum pernah disimpan. Mengembalikan berkas baru, atau null. */
    fun archiveIfNeeded(context: Context): File? {
        val props = readMeta(context) ?: return null
        if (props.getProperty(KEY_ARCHIVED) == "1") return null
        return archiveSession(context)
    }

    /** Menyimpan sesi terakhir (menimpa simpanan sebelumnya untuk sesi yang sama). */
    fun archiveSession(context: Context): File? {
        val props = readMeta(context) ?: return null
        val source = File(context.filesDir, LOG_NAME)
        var result: File? = null
        try {
            if (source.exists() && source.length() > 0L) {
                val start = props.getProperty(KEY_START)?.toLongOrNull() ?: source.lastModified()
                val dir = logsDir(context)
                if (!dir.exists()) dir.mkdirs()
                val out = File(dir, ARCHIVE_PREFIX + fileStamp(start) + "_" + safeName(props.getProperty(KEY_INSTANCE)) + ".txt")
                val tmp = File(dir, out.name + ".tmp")
                tmp.bufferedWriter(Charsets.UTF_8).use { w -> writeReport(context, w, props, source, start) }
                if (out.exists()) out.delete()
                if (tmp.renameTo(out)) result = out else tmp.delete()
                prune(dir)
            }
        } catch (_: Throwable) {
            // Penyimpanan log tidak boleh mengganggu game atau peluncur.
        }
        // Tandai sudah ditangani walau gagal, agar tidak diulang tiap kali.
        props.setProperty(KEY_ARCHIVED, "1")
        writeMeta(context, props)
        return result
    }

    /**
     * Membuat laporan khusus launcher (tanpa game): ringkasan perangkat, instance, akun (hanya jenisnya),
     * kejadian launcher, dan peringatan sistem. Untuk masalah yang muncul sebelum game dimulai.
     * Jangan dipanggil dari thread utama (membaca logcat). Mengembalikan berkas laporan, atau null bila gagal.
     */
    fun saveLauncherReport(context: Context): File? {
        return try {
            val dir = logsDir(context)
            if (!dir.exists()) dir.mkdirs()
            val now = System.currentTimeMillis()
            val out = File(dir, ARCHIVE_PREFIX + fileStamp(now) + "_" + LAUNCHER_TAG + ".txt")
            val tmp = File(dir, out.name + ".tmp")
            tmp.bufferedWriter(Charsets.UTF_8).use { w -> writeLauncherReport(context, w, now) }
            if (out.exists()) out.delete()
            if (tmp.renameTo(out)) {
                prune(dir)
                out
            } else {
                tmp.delete()
                null
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun prune(dir: File) {
        val all = dir.listFiles { f -> f.isFile && f.name.startsWith(ARCHIVE_PREFIX) } ?: return
        val sorted = all.sortedByDescending { it.lastModified() }
        // Log game dan laporan launcher dihitung terpisah, supaya laporan launcher tidak menggeser log game.
        sorted.filter { !isLauncherReport(it) }.drop(KEEP_COUNT).forEach { it.delete() }
        sorted.filter { isLauncherReport(it) }.drop(KEEP_LAUNCHER_COUNT).forEach { it.delete() }
    }

    /** Menghapus semua log tersimpan. Mengembalikan jumlah berkas yang terhapus. */
    fun deleteAll(context: Context): Int {
        var n = 0
        list(context).forEach { if (it.delete()) n++ }
        LauncherLog.clear(context)
        return n
    }

    // ---------------------------------------------------------------------------------------------
    // Isi laporan
    // ---------------------------------------------------------------------------------------------

    private fun writeReport(context: Context, w: Writer, props: Properties, source: File, start: Long) {
        val gameDir = props.getProperty(KEY_GAME_DIR).orEmpty()
        val clock = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

        w.line("===== NovaCraft: laporan sesi game =====")
        w.line("Disimpan     : " + clock.format(Date()))
        w.line("Mulai        : " + clock.format(Date(start)))
        val lastWrite = source.lastModified()
        if (lastWrite > start) {
            val secs = (lastWrite - start) / 1000L
            w.line("Log terakhir : " + clock.format(Date(lastWrite)) + " (sekitar " + (secs / 60L) + " menit " + (secs % 60L) + " detik)")
        }
        w.line("Launcher     : " + UpdateChecker.currentVersionName(context) + " (build " +
            UpdateChecker.currentVersionCode(context) + ", " + (if (UpdateChecker.wants32BitApk()) "32-bit" else "64-bit") + ")")
        w.line("Perangkat    : " + Build.MANUFACTURER + " " + Build.MODEL + ", Android " + Build.VERSION.RELEASE +
            " (API " + Build.VERSION.SDK_INT + "), " + Build.SUPPORTED_ABIS.joinToString("/") + ", RAM " + totalRamMb(context) + " MB")
        w.line("Instance     : " + props.getProperty(KEY_INSTANCE).orEmpty())
        val loaderVersion = props.getProperty(KEY_LOADER_VERSION).orEmpty()
        w.line("Minecraft    : " + props.getProperty(KEY_MC).orEmpty() + " | Loader: " +
            props.getProperty(KEY_LOADER).orEmpty() + (if (loaderVersion.isNotBlank()) " $loaderVersion" else ""))
        try {
            val s = SettingsManager.settings.value
            w.line("Pengaturan   : renderer=" + s.selectedRenderer + ", RAM game=" + s.ramMb + " MB, resolusi=" + s.resolutionRatio + "%")
        } catch (_: Throwable) {
        }

        // Kejadian di dalam launcher sebelum dan selama sesi ini (persiapan, unduhan, login, galat sebelum game)
        w.section("Log launcher (sebelum dan selama sesi ini)")
        val launcherText = LauncherLog.recentText(context, start - LAUNCHER_LOOKBACK_MS, MAX_LAUNCHER_BYTES)
        if (launcherText.isBlank()) w.line("(tidak ada kejadian tercatat)") else writeLines(w, launcherText)

        if (gameDir.isNotBlank()) {
            writeModsAndShaders(w, File(gameDir))
        }

        // Log MobileGlues (berisi galat penerjemahan shader), hanya yang ditulis selama sesi ini
        writeMobileGluesFiles(w, props.getProperty(KEY_MG_DIR).orEmpty(), start)

        // Laporan crash Minecraft bila ada yang terbit selama sesi ini
        if (gameDir.isNotBlank()) {
            val reports = File(gameDir, "crash-reports")
            val newest = reports.listFiles { f -> f.isFile && f.name.startsWith("crash-") && f.name.endsWith(".txt") }
                ?.maxByOrNull { it.lastModified() }
            if (newest != null && newest.lastModified() >= start - 10_000L) {
                w.section("Laporan crash Minecraft (" + newest.name + ")")
                writeHead(w, newest, MAX_EXTRA_BYTES)
            }
        }

        w.section("Log peluncur dan game")
        writeMainLog(w, source)
        w.flush()
    }

    private fun writeLauncherReport(context: Context, w: Writer, now: Long) {
        val clock = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

        w.line("===== NovaCraft: laporan launcher =====")
        w.line("Disimpan     : " + clock.format(Date(now)))
        w.line("Launcher     : " + UpdateChecker.currentVersionName(context) + " (build " +
            UpdateChecker.currentVersionCode(context) + ", " + (if (UpdateChecker.wants32BitApk()) "32-bit" else "64-bit") + ")")
        w.line("Perangkat    : " + Build.MANUFACTURER + " " + Build.MODEL + ", Android " + Build.VERSION.RELEASE +
            " (API " + Build.VERSION.SDK_INT + "), " + Build.SUPPORTED_ABIS.joinToString("/") + ", RAM " + totalRamMb(context) + " MB")
        try {
            val s = SettingsManager.settings.value
            w.line("Pengaturan   : renderer=" + s.selectedRenderer + ", RAM game=" + s.ramMb + " MB, resolusi=" + s.resolutionRatio + "%")
        } catch (_: Throwable) {
        }
        w.line("Penyimpanan  : " + storageText(context))

        try {
            val list = InstanceManager.instances.value
            w.line("Instance     : " + list.size + " terdaftar")
            for (inst in list.take(MAX_LISTED)) {
                w.line("  " + inst.name + " | " + inst.mcVersion + " | " + inst.loader +
                    (if (inst.loaderVersion.isNotBlank()) " " + inst.loaderVersion else "") +
                    " | java=" + inst.javaRuntime + " | diunduh=" + yesNo(inst.isDownloaded))
            }
        } catch (_: Throwable) {
            w.line("Instance     : (tidak tersedia)")
        }
        try {
            val types = AccountManager.accounts.value.groupingBy { it.safeAccountType }.eachCount()
            w.line("Akun         : " +
                (if (types.isEmpty()) "belum ada" else types.entries.joinToString(", ") { it.key + " x" + it.value }))
        } catch (_: Throwable) {
            w.line("Akun         : (tidak tersedia)")
        }

        w.section("Kejadian di dalam launcher")
        val events = LauncherLog.recentText(context, 0L, MAX_LAUNCHER_REPORT_BYTES)
        if (events.isBlank()) w.line("(tidak ada kejadian tercatat)") else writeLines(w, events)

        writeEngineSection(context, w)

        w.section("Peringatan dan galat sistem (logcat, hanya milik aplikasi ini)")
        val logcat = LauncherLog.logcatWarnings(LOGCAT_LINES)
        if (logcat.isBlank()) w.line("(tidak tersedia di HP ini atau kosong)") else writeLines(w, logcat.trimEnd('\n'))
        w.flush()
    }

    /**
     * Mesin Rust (Horizon) berjalan di proses lain dan mencatat dirinya sendiri di `performance/diagnostic.log`
     * (sudah disamarkan oleh mesin): pengaturan yang dibaca, shader yang dimuat (baris "shader pack ..." memuat
     * nama folder dan daftar programnya, atau alasan gagalnya), dan peringatan. Dua bagian pertama berguna saat
     * shader tidak tampil seperti seharusnya. Tidak ditulis bila mesin belum pernah dijalankan.
     */
    private fun writeEngineSection(context: Context, w: Writer) {
        try {
            val home = HorizonEngine.engineHome(context)
            if (!home.isDirectory) return
            val game = HorizonContent.gameHome(home)
            w.section("Mesin Rust (Horizon)")

            val config = File(home, "android-launcher.json")
            val shown = ArrayList<String>()
            if (config.isFile && config.length() < 64L * 1024L) {
                val json = JSONObject(config.readText())
                for (key in ENGINE_SETTING_KEYS) {
                    if (json.has(key)) shown.add(key + "=" + json.opt(key))
                }
            }
            w.line("Pengaturan mesin: " + if (shown.isEmpty()) "(bawaan mesin)" else shown.joinToString(", "))

            for (folder in listOf("shaderpacks", "resourcepacks")) {
                val items = File(game, folder).listFiles()?.sortedBy { it.name.lowercase() }.orEmpty()
                w.line("Folder " + folder + ": " + if (items.isEmpty()) "kosong atau tidak ada" else "")
                items.take(MAX_LISTED).forEach {
                    w.line("  " + it.name + if (it.isDirectory) " (folder)" else " (" + sizeText(it.length()) + ")")
                }
            }

            val graphics = File(game, "horizon-graphics.txt")
            if (graphics.isFile) {
                w.line("")
                w.line("-- horizon-graphics.txt (pickedShaderPack = shader yang dipilih) --")
                writeHead(w, graphics, MAX_CONFIG_BYTES)
            }

            val log = File(home, "performance/diagnostic.log")
            if (log.isFile && log.length() > 0L) {
                val shaderLines = log.useLines { lines ->
                    lines.filter { it.contains("shader pack", ignoreCase = true) }.toList()
                }
                w.section("Mesin Rust: baris shader (" + shaderLines.size + ")")
                if (shaderLines.isEmpty()) {
                    w.line("(tidak ada baris shader; shader belum dipilih atau belum sempat dimuat)")
                } else {
                    writeLines(w, shaderLines.takeLast(MAX_ENGINE_SHADER_LINES).joinToString("\n"))
                }
                w.section("Mesin Rust: log terakhir")
                writeTail(w, log, MAX_ENGINE_LOG_BYTES)
            } else {
                w.line("Log mesin: belum ada (mesin belum pernah dijalankan di HP ini).")
            }
        } catch (_: Throwable) {
            w.line("(log mesin Rust tidak bisa dibaca)")
        }
    }

    private fun storageText(context: Context): String {
        val allFiles = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) yesNo(Environment.isExternalStorageManager()) else "(tidak berlaku)"
        } catch (_: Throwable) {
            "?"
        }
        val nux = try {
            InstanceManager.getNuxDir(context)
        } catch (_: Throwable) {
            null
        }
        val free = try {
            sizeText(Environment.getExternalStorageDirectory().usableSpace)
        } catch (_: Throwable) {
            "?"
        }
        return "izin semua berkas=" + allFiles + ", folder data=" + (nux?.absolutePath ?: "?") +
            " (ada=" + yesNo(nux?.exists()) + ", bisa ditulis=" + yesNo(nux?.canWrite()) + "), ruang kosong=" + free
    }

    private fun yesNo(value: Boolean?): String = when (value) {
        null -> "?"
        true -> "ya"
        false -> "tidak"
    }

    private fun writeModsAndShaders(w: Writer, gameDir: File) {
        val mods = File(gameDir, "mods").listFiles { f -> f.isFile && f.name.endsWith(".jar") }
            ?.map { it.name }?.sortedBy { it.lowercase() }.orEmpty()
        w.section("Mod terpasang (" + mods.size + ")")
        if (mods.isEmpty()) {
            w.line("(tidak ada)")
        } else {
            mods.take(MAX_LISTED).forEach { w.line(it) }
            if (mods.size > MAX_LISTED) w.line("... dan " + (mods.size - MAX_LISTED) + " lainnya")
        }

        w.section("Shader")
        val packs = File(gameDir, "shaderpacks").listFiles()?.sortedBy { it.name.lowercase() }.orEmpty()
        if (packs.isEmpty()) {
            w.line("Folder shaderpacks: kosong atau tidak ada")
        } else {
            w.line("Folder shaderpacks:")
            packs.take(MAX_LISTED).forEach { w.line("  " + it.name + if (it.isDirectory) " (folder)" else " (" + sizeText(it.length()) + ")") }
        }
        for (rel in listOf("config/iris.properties", "config/oculus.properties", "optionsshaders.txt")) {
            val f = File(gameDir, rel)
            if (f.isFile) {
                w.line("")
                w.line("-- " + rel + " --")
                writeHead(w, f, MAX_CONFIG_BYTES)
            }
        }
    }

    private fun writeMobileGluesFiles(w: Writer, mgDirFromGame: String, start: Long) {
        val dirs = LinkedHashSet<File>()
        if (mgDirFromGame.isNotBlank()) dirs.add(File(mgDirFromGame))
        try {
            dirs.add(File(Environment.getExternalStorageDirectory(), "MobileGlues"))
        } catch (_: Throwable) {
        }
        for (dir in dirs) {
            if (!dir.isDirectory) continue
            val files = dir.listFiles { f -> f.isFile }.orEmpty().sortedBy { it.name }
            val config = files.firstOrNull { it.name.equals("config.json", ignoreCase = true) }
            if (config != null) {
                w.section("Konfigurasi MobileGlues (" + config.absolutePath + ")")
                writeHead(w, config, MAX_CONFIG_BYTES)
            }
            for (f in files) {
                val n = f.name.lowercase()
                val isLog = n.endsWith(".log") || n == "log.txt"
                if (isLog && f.length() > 0L && f.lastModified() >= start - 5_000L) {
                    w.section("Log MobileGlues (" + f.absolutePath + ")")
                    writeTail(w, f, MAX_EXTRA_BYTES)
                }
            }
        }
    }

    private fun writeMainLog(w: Writer, source: File) {
        val len = source.length()
        if (len <= MAX_LOG_BYTES) {
            writeHead(w, source, MAX_LOG_BYTES)
            return
        }
        writeHead(w, source, HEAD_BYTES)
        w.line("")
        w.line("[... bagian tengah log dipotong; berikut akhir log ...]")
        w.line("")
        writeTail(w, source, MAX_LOG_BYTES - HEAD_BYTES)
    }

    /** Menulis awal berkas (maksimal [maxBytes]); baris terakhir yang terpotong dibuang. */
    private fun writeHead(w: Writer, f: File, maxBytes: Long) {
        val len = f.length()
        val take = minOf(len, maxBytes).toInt()
        if (take <= 0) return
        val buf = ByteArray(take)
        FileInputStream(f).use { input ->
            var read = 0
            while (read < take) {
                val n = input.read(buf, read, take - read)
                if (n < 0) break
                read += n
            }
        }
        var text = String(buf, Charsets.UTF_8)
        if (len > take) {
            val lastNl = text.lastIndexOf('\n')
            if (lastNl > 0) text = text.substring(0, lastNl)
        }
        writeLines(w, text)
    }

    /** Menulis akhir berkas (maksimal [maxBytes]); baris pertama yang terpotong dibuang. */
    private fun writeTail(w: Writer, f: File, maxBytes: Long) {
        RandomAccessFile(f, "r").use { raf ->
            val len = raf.length()
            val startAt = maxOf(0L, len - maxBytes)
            val size = (len - startAt).toInt()
            if (size <= 0) return
            raf.seek(startAt)
            val buf = ByteArray(size)
            raf.readFully(buf)
            var text = String(buf, Charsets.UTF_8)
            if (startAt > 0L) {
                val nl = text.indexOf('\n')
                if (nl >= 0) text = text.substring(nl + 1)
            }
            writeLines(w, text)
        }
    }

    private fun writeLines(w: Writer, text: String) {
        for (raw in text.split('\n')) {
            w.write(LogRedactor.redactLine(raw.trimEnd('\r')))
            w.write("\n")
        }
    }

    private fun Writer.line(text: String) {
        write(text)
        write("\n")
    }

    private fun Writer.section(title: String) {
        write("\n===== " + title + " =====\n")
    }

    // ---------------------------------------------------------------------------------------------
    // Pembantu
    // ---------------------------------------------------------------------------------------------

    private fun fileStamp(millis: Long): String =
        SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date(millis))

    private fun safeName(raw: String?): String {
        val cleaned = raw.orEmpty().replace(Regex("[^A-Za-z0-9._-]+"), "_").trim('_').take(24)
        return cleaned.ifBlank { "game" }
    }

    private fun sizeText(bytes: Long): String = when {
        bytes >= 1024L * 1024L -> String.format(Locale.US, "%.1f MB", bytes / 1048576.0)
        bytes >= 1024L -> (bytes / 1024L).toString() + " KB"
        else -> "$bytes B"
    }

    private fun totalRamMb(context: Context): Long = try {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        info.totalMem / (1024L * 1024L)
    } catch (_: Throwable) {
        0L
    }

    private fun metaFile(context: Context): File = File(context.filesDir, META_NAME)

    private fun readMeta(context: Context): Properties? {
        synchronized(metaLock) {
            val f = metaFile(context)
            if (!f.isFile) return null
            return try {
                val p = Properties()
                FileInputStream(f).use { p.load(it) }
                p
            } catch (_: Throwable) {
                null
            }
        }
    }

    private fun writeMeta(context: Context, props: Properties) {
        synchronized(metaLock) {
            try {
                FileOutputStream(metaFile(context)).use { props.store(it, null) }
            } catch (_: Throwable) {
            }
        }
    }
}

// ----------------------------------------------------------------------------
// Section: core/crash/NovaCrashDiagnostics.kt
// ----------------------------------------------------------------------------

// ============================================================================
// NovaCraft Unified Crash Diagnostics: LogRedactor, CrashAdvisor & CrashUtils
// ============================================================================

/**
 * Menyamarkan token dan kata sandi di dalam teks log sebelum log dibagikan.
 */
object LogRedactor {

    private val ARG_PATTERN = Regex("""(?i)(--(?:accessToken|session|xuid|clientId))\s+\S+""")
    private val KEY_VALUE_PATTERN =
        Regex("""(?i)((?:access|refresh|client|id)[_-]?token|authorization|password|secret)(["']?\s*[:=]\s*["']?)[^\s"',;}&]+""")
    private val AUTH_HEADER_PATTERN = Regex("""(?i)(authorization["']?\s*[:=]\s*)[^\r\n]+""")
    private val BEARER_PATTERN = Regex("""(?i)(bearer\s+)[A-Za-z0-9._~+/=-]{8,}""")
    private val JWT_PATTERN = Regex("""eyJ[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{4,}""")
    private val HINTS = listOf("token", "--session", "--xuid", "--clientid", "authorization", "password", "secret", "bearer", "eyj")

    fun redactLine(line: String): String {
        val lower = line.lowercase()
        if (HINTS.none { lower.contains(it) }) return line

        var result = line
        result = ARG_PATTERN.replace(result) { "${it.groupValues[1]} ***" }
        result = AUTH_HEADER_PATTERN.replace(result) { "${it.groupValues[1]}***" }
        result = BEARER_PATTERN.replace(result) { "${it.groupValues[1]}***" }
        result = KEY_VALUE_PATTERN.replace(result) { "${it.groupValues[1]}${it.groupValues[2]}***" }
        result = JWT_PATTERN.replace(result, "***")
        return result
    }

    fun redact(text: String): String {
        return text.lineSequence().joinToString("\n") { redactLine(it) }
    }
}

enum class CrashSource { LAUNCHER, MEMORY, GRAPHICS, GAME_OR_MODS, UNKNOWN }

object CrashAdvisor {

    private const val MAX_LOG_CHARS = 14_000
    private const val LOG_HEAD_CHARS = 4_000

    fun sourceOf(info: GameCrashInfo): CrashSource {
        if (info.isLauncherCrash) return CrashSource.LAUNCHER
        val code = info.exitCode
        if (code == 137 || (info.isSignal && code == 9) || info.logSnippet.contains("OutOfMemoryError")) {
            return CrashSource.MEMORY
        }
        if (code == 134 || code == 139 || (info.isSignal && (code == 4 || code == 6 || code == 7 || code == 8 || code == 11))) {
            return CrashSource.GRAPHICS
        }
        if (!info.isSignal && code == 1) return CrashSource.GAME_OR_MODS
        return CrashSource.UNKNOWN
    }

    fun describeExit(info: GameCrashInfo): String {
        val code = info.exitCode
        return when {
            code == 137 || code == 139 || code == 134 || code == 130 -> CrashUtils.getExitCodeDescription(code)
            info.isSignal -> CrashUtils.getSignalName(code)
            else -> CrashUtils.getExitCodeDescription(code)
        }
    }

    fun logForSharing(logText: String): String = trimLog(LogRedactor.redact(logText))

    private fun trimLog(text: String): String {
        if (text.length <= MAX_LOG_CHARS) return text
        val head = text.substring(0, LOG_HEAD_CHARS)
        val tail = text.substring(text.length - (MAX_LOG_CHARS - LOG_HEAD_CHARS))
        return head + "\n\n[...]\n\n" + tail
    }
}

object CrashUtils {

    fun getSignalName(signal: Int): String {
        return when (signal) {
            4 -> "SIGILL (Instruksi CPU Tidak Didukung)"
            6 -> "SIGABRT (Native Assertion / Abort)"
            7 -> "SIGBUS (Kesalahan Bus Memori)"
            8 -> "SIGFPE (Kesalahan Perhitungan Float)"
            9 -> "SIGKILL (Dihentikan OS / Kehabisan RAM)"
            11 -> "SIGSEGV (Akses Memori Ilegal / Segfault)"
            15 -> "SIGTERM (Permintaan Terminasi)"
            else -> "Signal $signal"
        }
    }

    fun getExitCodeDescription(exitCode: Int): String {
        return when (exitCode) {
            0 -> "Selesai Normal (0)"
            1 -> "Kesalahan Inisialisasi Mod / Java Exception (1)"
            130 -> "Dihentikan Pengguna (SIGINT 130)"
            134 -> "Native Driver Abort (SIGABRT 134)"
            137 -> "Kehabisan Memori RAM / OOM (SIGKILL 137)"
            139 -> "Crash Driver Grafis / Segfault (SIGSEGV 139)"
            -1 -> "Kegagalan Bootstrap Mesin Virtual (-1)"
            else -> "Kode Keluar $exitCode"
        }
    }

    const val CRASH_LOG_NOTE = "Untuk detail penyebabnya, silahkan periksa file log. Jika Anda tidak bisa menyelesaikan masalah sendiri, silakan kirim file log kepada seseorang atau tim yang dapat menganalisisnya. Jangan hanya mengambil screenshot dari layar saat ini, karena screenshot tidak bisa memberikan informasi teknis yang cukup untuk menyelesaikan masalah."

    fun getExitMessage(exitCode: Int, isSignal: Boolean): String {
        return if (isSignal) {
            "JVM digugurkan karena sinyal fatal $exitCode (${getSignalName(exitCode)})."
        } else {
            "JVM keluar dengan kode $exitCode (${getExitCodeDescription(exitCode)})."
        }
    }

    fun shareLogFile(
        context: Context,
        file: File,
        subjectPrefix: String = "Log Crash NovaCraft",
        chooserTitle: String = "Bagikan Log Crash"
    ) {
        try {
            if (!file.exists()) {
                Toast.makeText(context, "File log tidak ditemukan.", Toast.LENGTH_SHORT).show()
                return
            }

            val shareDir = File(context.cacheDir, "shared_logs").apply { mkdirs() }
            val shareCopy = File(shareDir, file.name)
            file.useLines(Charsets.UTF_8) { lines ->
                shareCopy.bufferedWriter(Charsets.UTF_8).use { writer ->
                    for (line in lines) {
                        writer.write(LogRedactor.redactLine(line))
                        writer.newLine()
                    }
                }
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", shareCopy)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "$subjectPrefix - ${file.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "$chooserTitle: ${file.name}").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "Tidak ada aplikasi untuk membagikan file ini.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membagikan file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

/**
 * Mengimpor data (instance, layout kontrol) dari berkas arsip (.zip) atau berkas kontrol (.json)
 * dengan penamaan folder unik agar tidak menimpa data yang sudah ada.
 */
object LauncherDataImporter {
    data class ImportSummary(val importedInstances: Int, val importedControls: Int)

    fun importFromUri(context: Context, uri: Uri): Result<ImportSummary> {
        return try {
            val contentResolver = context.contentResolver
            val tempRoot = File(context.cacheDir, "import_data_${System.nanoTime()}").apply { mkdirs() }
            try {
                val lowerName = (uri.lastPathSegment ?: "").lowercase(Locale.US)
                val mime = contentResolver.getType(uri)?.lowercase(Locale.US).orEmpty()
                val isJson = lowerName.endsWith(".json") || mime.contains("json")
                if (isJson) {
                    val raw = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: return Result.failure(IllegalStateException("Không thể đọc tệp đã chọn"))
                    val controlsDir = File(context.filesDir, "controls").apply { mkdirs() }
                    val target = uniqueFile(controlsDir, "imported_layout", ".json")
                    target.writeBytes(raw)
                    File(controlsDir, "default_layout.json").writeBytes(raw)
                    com.aistudio.novacraft.launcher.core.controls.ControlLayoutManager.init(context)
                    return Result.success(ImportSummary(importedInstances = 0, importedControls = 1))
                }

                contentResolver.openInputStream(uri)?.use { rawStream ->
                    ZipInputStream(rawStream).use { zis ->
                        val canonicalRoot = tempRoot.canonicalFile
                        var entry = zis.nextEntry
                        while (entry != null) {
                            val cleanName = entry.name.replace('\\', '/')
                            if (!entry.isDirectory && cleanName.isNotBlank() && !cleanName.startsWith("/")) {
                                val outFile = File(canonicalRoot, cleanName).canonicalFile
                                if (outFile.path.startsWith(canonicalRoot.path + File.separator)) {
                                    outFile.parentFile?.mkdirs()
                                    FileOutputStream(outFile).use { fos ->
                                        zis.copyTo(fos, 32 * 1024)
                                    }
                                }
                            }
                            zis.closeEntry()
                            entry = zis.nextEntry
                        }
                    }
                } ?: return Result.failure(IllegalStateException("Không thể mở tệp sao lưu"))

                var instCount = 0
                var ctrlCount = 0
                val instancesDir = InstanceManager.getInstancesDir(context)
                val instConfigs = tempRoot.walkTopDown()
                    .filter { it.isFile && it.name.equals("instance.json", ignoreCase = true) }
                    .toList()
                for (cfg in instConfigs) {
                    val srcDir = cfg.parentFile ?: continue
                    val targetDir = uniqueDir(instancesDir, srcDir.name.ifBlank { "imported_instance" })
                    if (srcDir.copyRecursively(targetDir, overwrite = false)) {
                        instCount++
                    }
                }

                val controlsDir = File(context.filesDir, "controls").apply { mkdirs() }
                val ctrlFiles = tempRoot.walkTopDown().filter { f ->
                    if (!f.isFile || !f.extension.equals("json", ignoreCase = true)) return@filter false
                    val pathLower = f.absolutePath.lowercase(Locale.US)
                    pathLower.contains("control") || f.name.contains("layout", ignoreCase = true)
                }.toList()
                for (cf in ctrlFiles) {
                    val text = cf.readText()
                    if (text.contains("\"keyCode\"") || text.contains("\"isJoystick\"") || text.contains("\"buttons\"")) {
                        val dest = uniqueFile(controlsDir, cf.nameWithoutExtension, ".json")
                        cf.copyTo(dest, overwrite = false)
                        if (ctrlCount == 0) {
                            File(controlsDir, "default_layout.json").writeText(text)
                        }
                        ctrlCount++
                    }
                }

                if (instCount > 0) InstanceManager.init(context)
                if (ctrlCount > 0) com.aistudio.novacraft.launcher.core.controls.ControlLayoutManager.init(context)
                if (instCount == 0 && ctrlCount == 0) {
                    Result.failure(IllegalStateException("Không tìm thấy dữ liệu instance hoặc bố cục điều khiển hợp lệ trong tệp"))
                } else {
                    Result.success(ImportSummary(instCount, ctrlCount))
                }
            } finally {
                tempRoot.deleteRecursively()
            }
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    private fun uniqueDir(parent: File, baseName: String): File {
        val safe = baseName.replace(Regex("[^A-Za-z0-9._-]+"), "_").trim('_').ifBlank { "imported" }
        var candidate = File(parent, safe)
        var idx = 1
        while (candidate.exists()) {
            candidate = File(parent, "${safe}_$idx")
            idx++
        }
        return candidate
    }

    private fun uniqueFile(parent: File, baseName: String, ext: String): File {
        val safe = baseName.replace(Regex("[^A-Za-z0-9._-]+"), "_").trim('_').ifBlank { "layout" }
        var candidate = File(parent, "$safe$ext")
        var idx = 1
        while (candidate.exists()) {
            candidate = File(parent, "${safe}_$idx$ext")
            idx++
        }
        return candidate
    }
}

// ----------------------------------------------------------------------------
// Section: core/utils/DiscordPresenceManager.kt
// ----------------------------------------------------------------------------

enum class DiscordPresenceMode {
    HIDDEN,
    IDLE,
    PLAYING
}

data class DiscordPresenceStatus(
    val mode: DiscordPresenceMode = DiscordPresenceMode.HIDDEN,
    val discordInstalled: Boolean = false,
    val discordPackage: String? = null,
    val connected: Boolean = false,
    val headline: String = "",
    val detail: String = ""
)

/**
 * Quản lý Discord Rich Presence cục bộ cho Amber Launcher:
 * - Không dùng token hay thông tin đăng nhập Discord; kiểm tra và kết nối cục bộ với ứng dụng Discord đã cài.
 * - Không bao giờ chia sẻ tên instance hay tài khoản người dùng.
 * - Tôn trọng hiệu lực theo phiên: trạng thái launcher chờ được chụp lúc mở launcher; trạng thái khi chơi
 *   và hiện máy chủ được chụp lúc bắt đầu phiên chơi.
 * - Địa chỉ máy chủ Java được đọc từ nhật ký game và lọc bỏ tuyệt đối mọi địa chỉ mạng cục bộ/nội bộ.
 */
object DiscordPresenceManager {

    private const val TAG = "DiscordRPC"
    private const val APP_DISPLAY_NAME = "Amber Launcher"
    private const val DISCORD_CLIENT_ID = "1278946135449600000"
    private val DISCORD_PACKAGES = listOf("com.discord", "com.discord.ptb", "com.discord.canary")

    private val CONNECTING_SERVER_REGEX = Regex(
        """Connecting to\s+\[?([^,\s\]]+)\]?(?:\s*,\s*(\d{1,5}))?(?::(\d{1,5}))?""",
        RegexOption.IGNORE_CASE
    )
    private val SINGLEPLAYER_START_REGEX = Regex(
        """Starting integrated minecraft server""",
        RegexOption.IGNORE_CASE
    )
    private val IPV4_REGEX = Regex("""^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$""")

    private val rpcExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "NovaDiscordRpc").apply { isDaemon = true }
    }

    private val _status = MutableStateFlow(DiscordPresenceStatus())
    val status: StateFlow<DiscordPresenceStatus> = _status.asStateFlow()

    @Volatile private var startupIdleSnapshotted = false
    @Volatile private var launcherIdleEnabledForSession = false
    @Volatile private var gameSessionActive = false
    @Volatile private var activeMcVersion: String = ""
    @Volatile private var activeLoaderLabel: String = ""
    @Volatile private var allowServerInSession: Boolean = false
    @Volatile private var activeServerHost: String? = null
    @Volatile private var sessionStartEpochSec: Long = 0L

    @Volatile private var localUnixSocket: android.net.LocalSocket? = null
    @Volatile private var tcpSocket: java.net.Socket? = null
    @Volatile private var boundDiscordService: ServiceConnection? = null
    @Volatile private var boundDiscordContext: Context? = null

    fun findInstalledDiscordPackage(context: Context): String? {
        val pm = context.packageManager
        for (pkg in DISCORD_PACKAGES) {
            val found = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(pkg, android.content.pm.PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(pkg, 0)
                }
            }.isSuccess
            if (found) return pkg
        }
        return null
    }

    fun isDiscordInstalled(context: Context): Boolean = findInstalledDiscordPackage(context) != null

    /**
     * Chụp cấu hình trạng thái chờ ("Getting ready to play") một lần khi launcher khởi động,
     * đảm bảo thay đổi trong Cài đặt chỉ có hiệu lực ở lần mở launcher tiếp theo.
     */
    fun initLauncherStartup(context: Context) {
        if (!startupIdleSnapshotted) {
            startupIdleSnapshotted = true
            launcherIdleEnabledForSession = SettingsManager.settings.value.discordPresenceIdle
        }
        if (launcherIdleEnabledForSession && !gameSessionActive) {
            enterLauncherIdle(context)
        } else {
            refreshInstallationStateOnly(context)
        }
    }

    fun onGameLaunching() {
        gameSessionActive = true
    }

    fun onReturnedToLauncher(context: Context) {
        gameSessionActive = false
        activeServerHost = null
        if (launcherIdleEnabledForSession) {
            enterLauncherIdle(context)
        } else {
            clearPresence(context)
        }
    }

    fun enterLauncherIdle(context: Context) {
        if (!launcherIdleEnabledForSession || gameSessionActive) return
        val appCtx = context.applicationContext
        if (sessionStartEpochSec <= 0L) {
            sessionStartEpochSec = System.currentTimeMillis() / 1000L
        }
        publishActivity(
            context = appCtx,
            mode = DiscordPresenceMode.IDLE,
            details = APP_DISPLAY_NAME,
            stateText = "Getting ready to play"
        )
    }

    fun startGameSession(
        context: Context,
        mcVersion: String,
        loader: String,
        allowServerDisplay: Boolean
    ) {
        val appCtx = context.applicationContext
        gameSessionActive = true
        activeMcVersion = mcVersion.trim().ifBlank { "Java" }
        activeLoaderLabel = formatLoaderLabel(loader)
        allowServerInSession = allowServerDisplay && !loader.equals("rust", ignoreCase = true) &&
            !loader.equals("horizon", ignoreCase = true) &&
            !loader.contains("rust", ignoreCase = true)
        activeServerHost = null
        sessionStartEpochSec = System.currentTimeMillis() / 1000L

        publishCurrentGameActivity(appCtx)
    }

    /**
     * Đọc dòng nhật ký game từ Loader Java để cập nhật máy chủ nhiều người chơi
     * hoặc đặt lại khi mở thế giới chơi đơn.
     */
    fun onGameLogLine(context: Context, line: String) {
        if (!gameSessionActive || !allowServerInSession || line.isBlank()) return
        val hasConnecting = line.contains("Connecting to", ignoreCase = true)
        val hasSingleplayer = !hasConnecting && line.contains("Starting integrated minecraft server", ignoreCase = true)
        if (!hasConnecting && !hasSingleplayer) return

        if (hasSingleplayer || SINGLEPLAYER_START_REGEX.containsMatchIn(line)) {
            if (activeServerHost != null) {
                activeServerHost = null
                publishCurrentGameActivity(context.applicationContext)
            }
            return
        }

        val match = CONNECTING_SERVER_REGEX.find(line) ?: return
        val rawHost = match.groupValues.getOrNull(1)?.trim().orEmpty()
        val rawPort = (match.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() }
            ?: match.groupValues.getOrNull(3)?.takeIf { it.isNotBlank() })
            ?.toIntOrNull()

        val publicDisplayHost = sanitizePublicServerHost(rawHost, rawPort)
        if (publicDisplayHost == null) {
            if (activeServerHost != null) {
                activeServerHost = null
                publishCurrentGameActivity(context.applicationContext)
            }
            return
        }

        if (publicDisplayHost != activeServerHost) {
            activeServerHost = publicDisplayHost
            publishCurrentGameActivity(context.applicationContext)
        }
    }

    fun clearPresence(context: Context) {
        val appCtx = context.applicationContext
        gameSessionActive = false
        activeServerHost = null
        val pkg = findInstalledDiscordPackage(appCtx)
        _status.value = DiscordPresenceStatus(
            mode = DiscordPresenceMode.HIDDEN,
            discordInstalled = pkg != null,
            discordPackage = pkg,
            connected = false,
            headline = "",
            detail = ""
        )
        rpcExecutor.execute {
            runCatching {
                sendClearToLocalRpcChannels(appCtx, pkg)
            }
        }
    }

    private fun refreshInstallationStateOnly(context: Context) {
        val pkg = findInstalledDiscordPackage(context)
        _status.value = _status.value.copy(
            discordInstalled = pkg != null,
            discordPackage = pkg
        )
    }

    private fun publishCurrentGameActivity(context: Context) {
        val server = activeServerHost
        val versionLoader = "Minecraft $activeMcVersion · $activeLoaderLabel"
        val details: String
        val stateText: String
        if (!server.isNullOrBlank()) {
            details = "$APP_DISPLAY_NAME · $versionLoader"
            stateText = "Playing on $server"
        } else {
            details = APP_DISPLAY_NAME
            stateText = versionLoader
        }
        publishActivity(
            context = context,
            mode = DiscordPresenceMode.PLAYING,
            details = details,
            stateText = stateText
        )
    }

    private fun publishActivity(
        context: Context,
        mode: DiscordPresenceMode,
        details: String,
        stateText: String
    ) {
        val appCtx = context.applicationContext
        val startSec = if (sessionStartEpochSec > 0L) sessionStartEpochSec else System.currentTimeMillis() / 1000L
        val pkg = findInstalledDiscordPackage(appCtx)
        if (pkg == null) {
            _status.value = DiscordPresenceStatus(
                mode = mode,
                discordInstalled = false,
                discordPackage = null,
                connected = false,
                headline = details,
                detail = stateText
            )
            LauncherLog.d(TAG, "Bỏ qua cập nhật Discord RPC ($details | $stateText): chưa cài ứng dụng Discord")
            return
        }

        _status.value = DiscordPresenceStatus(
            mode = mode,
            discordInstalled = true,
            discordPackage = pkg,
            connected = false,
            headline = details,
            detail = stateText
        )

        rpcExecutor.execute {
            val dispatched = runCatching {
                dispatchToInstalledDiscord(appCtx, pkg, details, stateText, startSec)
            }.getOrDefault(false)
            _status.value = _status.value.copy(
                discordInstalled = true,
                discordPackage = pkg,
                connected = dispatched
            )
        }
    }

    private fun dispatchToInstalledDiscord(
        context: Context,
        discordPkg: String,
        details: String,
        stateText: String,
        startSec: Long
    ): Boolean {
        if (trySocialSdkReflection(details, stateText, startSec)) return true
        if (tryBindDiscordService(context, discordPkg, details, stateText, startSec)) return true
        if (tryLocalUnixIpcSocket(details, stateText, startSec)) return true
        if (tryLocalTcpRpcSocket(details, stateText, startSec)) return true
        return false
    }

    private fun sendClearToLocalRpcChannels(context: Context, discordPkg: String?) {
        runCatching {
            boundDiscordService?.let { conn ->
                boundDiscordContext?.unbindService(conn)
            }
        }
        boundDiscordService = null
        boundDiscordContext = null
        runCatching { localUnixSocket?.close() }
        localUnixSocket = null
        runCatching { tcpSocket?.close() }
        tcpSocket = null
        if (discordPkg != null) {
            runCatching {
                val cls = Class.forName("com.discord.socialsdk.Client")
                cls.methods.firstOrNull { it.name == "ClearRichPresence" && it.parameterCount == 0 }
                    ?.invoke(null)
            }
        }
    }

    private fun trySocialSdkReflection(details: String, stateText: String, startSec: Long): Boolean {
        return runCatching {
            val activityCls = Class.forName("com.discord.socialsdk.Activity")
            val activity = activityCls.getDeclaredConstructor().newInstance()
            activityCls.methods.firstOrNull { it.name == "SetName" && it.parameterCount == 1 }?.invoke(activity, APP_DISPLAY_NAME)
            activityCls.methods.firstOrNull { it.name == "SetDetails" && it.parameterCount == 1 }?.invoke(activity, details)
            activityCls.methods.firstOrNull { it.name == "SetState" && it.parameterCount == 1 }?.invoke(activity, stateText)
            val tsCls = Class.forName("com.discord.socialsdk.ActivityTimestamps")
            val ts = tsCls.getDeclaredConstructor().newInstance()
            tsCls.methods.firstOrNull { it.name == "SetStart" && it.parameterCount == 1 }?.invoke(ts, startSec)
            activityCls.methods.firstOrNull { it.name == "SetTimestamps" && it.parameterCount == 1 }?.invoke(activity, ts)
            val clientCls = Class.forName("com.discord.socialsdk.Client")
            val update = clientCls.methods.firstOrNull { it.name == "UpdateRichPresence" }
            if (update != null) {
                true
            } else {
                false
            }
        }.getOrDefault(false)
    }

    private fun tryBindDiscordService(
        context: Context,
        discordPkg: String,
        details: String,
        stateText: String,
        startSec: Long
    ): Boolean {
        return runCatching {
            val pm = context.packageManager
            val candidateActions = listOf(
                "com.discord.intent.action.SOCIAL_RPC",
                "com.discord.intent.action.RPC",
                "com.discord.social.RPC"
            )
            var resolvedComponent: ComponentName? = null
            for (action in candidateActions) {
                val queryIntent = Intent(action).setPackage(discordPkg)
                val matches = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.queryIntentServices(queryIntent, android.content.pm.PackageManager.ResolveInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.queryIntentServices(queryIntent, 0)
                }
                val svc = matches.firstOrNull()?.serviceInfo
                if (svc != null && svc.exported) {
                    resolvedComponent = ComponentName(svc.packageName, svc.name)
                    break
                }
            }
            val target = resolvedComponent ?: return false
            val bindIntent = Intent().setComponent(target).apply {
                putExtra("client_id", DISCORD_CLIENT_ID)
                putExtra("name", APP_DISPLAY_NAME)
                putExtra("details", details)
                putExtra("state", stateText)
                putExtra("start_timestamp", startSec)
            }
            val conn = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    if (service != null) {
                        runCatching {
                            val messenger = android.os.Messenger(service)
                            val msg = android.os.Message.obtain(null, 1)
                            msg.data = android.os.Bundle().apply {
                                putString("cmd", "SET_ACTIVITY")
                                putString("client_id", DISCORD_CLIENT_ID)
                                putString("name", APP_DISPLAY_NAME)
                                putString("details", details)
                                putString("state", stateText)
                                putLong("start_timestamp", startSec)
                            }
                            messenger.send(msg)
                        }
                    }
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    boundDiscordService = null
                }
            }
            boundDiscordService?.let { old -> runCatching { boundDiscordContext?.unbindService(old) } }
            val ok = context.bindService(bindIntent, conn, Context.BIND_AUTO_CREATE)
            if (ok) {
                boundDiscordService = conn
                boundDiscordContext = context
            }
            ok
        }.getOrDefault(false)
    }

    private fun buildSetActivityPayloadJson(details: String, stateText: String, startSec: Long): String {
        val activity = JSONObject().apply {
            put("name", APP_DISPLAY_NAME)
            put("details", details)
            put("state", stateText)
            put("timestamps", JSONObject().put("start", startSec))
        }
        val args = JSONObject().apply {
            put("pid", Process.myPid())
            put("activity", activity)
        }
        return JSONObject().apply {
            put("cmd", "SET_ACTIVITY")
            put("args", args)
            put("nonce", "${System.currentTimeMillis()}-${Process.myPid()}")
        }.toString()
    }

    private fun tryLocalUnixIpcSocket(details: String, stateText: String, startSec: Long): Boolean {
        val candidateNames = listOf("discord-ipc-0", "discord-ipc-1")
        for (socketName in candidateNames) {
            val ok = runCatching {
                val sock = android.net.LocalSocket()
                sock.soTimeout = 350
                sock.connect(android.net.LocalSocketAddress(socketName, android.net.LocalSocketAddress.Namespace.ABSTRACT))
                val out = sock.outputStream
                val handshake = JSONObject().put("v", 1).put("client_id", DISCORD_CLIENT_ID).toString()
                writeIpcFrame(out, 0, handshake)
                val payload = buildSetActivityPayloadJson(details, stateText, startSec)
                writeIpcFrame(out, 1, payload)
                out.flush()
                runCatching { localUnixSocket?.close() }
                localUnixSocket = sock
                true
            }.getOrDefault(false)
            if (ok) return true
        }
        return false
    }

    private fun tryLocalTcpRpcSocket(details: String, stateText: String, startSec: Long): Boolean {
        for (port in 6463..6465) {
            val ok = runCatching {
                val sock = java.net.Socket()
                sock.connect(java.net.InetSocketAddress("127.0.0.1", port), 250)
                sock.soTimeout = 350
                val out = sock.getOutputStream()
                val handshake = JSONObject().put("v", 1).put("client_id", DISCORD_CLIENT_ID).toString()
                writeIpcFrame(out, 0, handshake)
                val payload = buildSetActivityPayloadJson(details, stateText, startSec)
                writeIpcFrame(out, 1, payload)
                out.flush()
                runCatching { tcpSocket?.close() }
                tcpSocket = sock
                true
            }.getOrDefault(false)
            if (ok) return true
        }
        return false
    }

    private fun writeIpcFrame(out: java.io.OutputStream, opcode: Int, json: String) {
        val bytes = json.toByteArray(Charsets.UTF_8)
        val header = java.nio.ByteBuffer.allocate(8).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        header.putInt(opcode)
        header.putInt(bytes.size)
        out.write(header.array())
        out.write(bytes)
    }

    private fun formatLoaderLabel(loader: String): String {
        val clean = loader.trim().lowercase(Locale.US)
        return when {
            clean == "vanilla" || clean.isEmpty() -> "Vanilla"
            clean == "fabric" -> "Fabric"
            clean == "forge" -> "Forge"
            clean == "neoforge" -> "NeoForge"
            clean == "quilt" -> "Quilt"
            clean == "rust" || clean == "horizon" || clean.contains("rust") -> "Engine Rust"
            else -> loader.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
        }
    }

    internal fun sanitizePublicServerHost(rawHost: String, port: Int?): String? {
        var host = rawHost.trim().trimEnd('.')
        if (host.startsWith("[") && host.endsWith("]") && host.length > 2) {
            host = host.substring(1, host.length - 1)
        }
        if (host.isEmpty() || host.length > 253) return null
        if (isLocalNetworkAddress(host)) return null
        return if (port != null && port > 0 && port != 25565) "$host:$port" else host
    }

    internal fun isLocalNetworkAddress(rawHost: String): Boolean {
        val host = rawHost.trim().trimEnd('.').lowercase(Locale.US)
        if (host.isEmpty()) return true
        if (host == "localhost" || host == "localhost.localdomain" ||
            host.endsWith(".localhost") || host.endsWith(".local") ||
            host.endsWith(".lan") || host.endsWith(".home") ||
            host.endsWith(".internal") || host.endsWith(".arpa")
        ) {
            return true
        }
        // Single-label hostnames (no dot and not an IPv6 address) belong to local mDNS / LAN hosts.
        if (!host.contains('.') && !host.contains(':')) {
            return true
        }
        val ipv4Match = IPV4_REGEX.matchEntire(host)
        if (ipv4Match != null) {
            val a = ipv4Match.groupValues[1].toIntOrNull() ?: return true
            val b = ipv4Match.groupValues[2].toIntOrNull() ?: return true
            val c = ipv4Match.groupValues[3].toIntOrNull() ?: return true
            val d = ipv4Match.groupValues[4].toIntOrNull() ?: return true
            if (a > 255 || b > 255 || c > 255 || d > 255) return true
            if (a == 0) return true // 0.0.0.0/8
            if (a == 10) return true // 10.0.0.0/8
            if (a == 100 && b in 64..127) return true // 100.64.0.0/10 CGNAT / overlay LAN
            if (a == 127) return true // 127.0.0.0/8 loopback
            if (a == 169 && b == 254) return true // 169.254.0.0/16 link-local
            if (a == 172 && b in 16..31) return true // 172.16.0.0/12 private
            if (a == 192 && b == 168) return true // 192.168.0.0/16 private
            if (a >= 224) return true // multicast / reserved
            return false
        }
        if (host.contains(':')) {
            // Numeric IPv6 literal check via InetAddress (no DNS resolution performed on IP literals)
            val cleanV6 = host.substringBefore('%')
            val addr = runCatching { InetAddress.getByName(cleanV6) }.getOrNull() ?: return true
            if (addr.isAnyLocalAddress || addr.isLoopbackAddress || addr.isLinkLocalAddress ||
                addr.isSiteLocalAddress || addr.isMulticastAddress
            ) {
                return true
            }
            val raw = addr.address
            if (raw.size == 16) {
                val b0 = raw[0].toInt() and 0xFF
                if ((b0 and 0xFE) == 0xFC) return true // fc00::/7 Unique Local Address
            }
            if (raw.size == 4) {
                return isLocalNetworkAddress(addr.hostAddress ?: "")
            }
        }
        return false
    }
}

