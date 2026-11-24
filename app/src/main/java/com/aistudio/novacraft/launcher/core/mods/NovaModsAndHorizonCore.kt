package com.aistudio.novacraft.launcher.core.mods

import android.app.ActivityManager
import android.app.ActivityOptions
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.aistudio.novacraft.launcher.BuildConfig
import com.aistudio.novacraft.launcher.core.account.AccountManager
import com.aistudio.novacraft.launcher.core.account.MicrosoftAuthService
import com.aistudio.novacraft.launcher.core.controls.ControlLayoutManager
import com.aistudio.novacraft.launcher.core.controls.ControlShowMode
import com.aistudio.novacraft.launcher.core.controls.CustomControlButton
import com.aistudio.novacraft.launcher.core.utils.LauncherLog
import com.aistudio.novacraft.launcher.core.utils.LogRedactor
import com.aistudio.novacraft.launcher.core.launch.InstanceManager
import com.aistudio.novacraft.launcher.core.launch.MojangManifestService
import com.aistudio.novacraft.launcher.core.utils.*
import com.aistudio.novacraft.launcher.core.utils.InstalledModItem
import com.aistudio.novacraft.launcher.core.utils.Instance
import com.aistudio.novacraft.launcher.core.utils.ModrinthDependency
import com.aistudio.novacraft.launcher.core.utils.ModrinthGalleryItem
import com.aistudio.novacraft.launcher.core.utils.ModrinthProject
import com.aistudio.novacraft.launcher.core.utils.ModrinthSearchHit
import com.aistudio.novacraft.launcher.core.utils.ModrinthSearchResponse
import com.aistudio.novacraft.launcher.core.utils.ModrinthVersion
import com.aistudio.novacraft.launcher.core.utils.ModrinthVersionFile
import com.aistudio.novacraft.launcher.core.utils.SafePaths
import com.aistudio.novacraft.launcher.core.utils.SettingsManager
import com.aistudio.novacraft.launcher.core.utils.UserAccount
import com.aistudio.novacraft.launcher.ui.theme.AMBER_ACCENTS
import com.aistudio.novacraft.launcher.ui.theme.AmberAccent
import com.aistudio.novacraft.launcher.ui.theme.Toast
import com.aistudio.novacraft.launcher.ui.theme.Tr
import com.movtery.inputmap.keycodes.LwjglGlfwKeycode as Key
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.security.MessageDigest
import java.text.Normalizer
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.GZIPInputStream
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

// ----------------------------------------------------------------------------
// Section: core/mods/ModMetadata.kt
// ----------------------------------------------------------------------------

/** Info singkat sebuah mod yang dibaca dari jar-nya. Setiap bagian boleh kosong. */
internal class ModJarInfo(val name: String?, val version: String?, val description: String?)

/**
 * Membaca nama, versi, dan deskripsi mod dari jar-nya untuk daftar "Mod terpasang".
 * Mendukung Fabric (`fabric.mod.json`), Forge (`META-INF/mods.toml`), dan NeoForge
 * (`META-INF/neoforge.mods.toml`, formatnya sama dengan mods.toml Forge).
 */
internal object ModMetadata {

    private const val JAR_VERSION_PLACEHOLDER = "\${file.jarVersion}"

    /** Info mod dari [zip], atau null bila jar tidak memuat berkas deskripsi yang dikenal. */
    fun read(zip: ZipFile): ModJarInfo? {
        zip.getEntry("fabric.mod.json")?.let { entry ->
            return zip.getInputStream(entry).bufferedReader().use { reader ->
                val json = JsonParser.parseReader(reader).asJsonObject
                ModJarInfo(
                    name = text(json.get("name")),
                    version = text(json.get("version")),
                    description = text(json.get("description"))
                )
            }
        }
        // NeoForge lebih dulu: jar yang dibuat untuk keduanya memuat dua berkas toml
        for (path in listOf("META-INF/neoforge.mods.toml", "META-INF/mods.toml")) {
            val entry = zip.getEntry(path) ?: continue
            val toml = zip.getInputStream(entry).bufferedReader().use { it.readText() }
            return parseModsToml(toml) { readManifestVersion(zip) }
        }
        return null
    }

    private fun text(element: JsonElement?): String? =
        element?.takeIf { it.isJsonPrimitive }?.asString

    /** `Implementation-Version` dari MANIFEST.MF; dipakai bila mods.toml menulis `${file.jarVersion}`. */
    private fun readManifestVersion(zip: ZipFile): String? {
        val entry = zip.getEntry("META-INF/MANIFEST.MF") ?: return null
        return zip.getInputStream(entry).bufferedReader().use { reader ->
            reader.readLines()
                .firstOrNull { it.startsWith("Implementation-Version:", ignoreCase = true) }
                ?.substringAfter(':')
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
        }
    }

    /**
     * Membaca blok `[[mods]]` pertama dari mods.toml. Nilai yang didukung: teks "..." atau '...' satu
     * baris, dan teks banyak baris """...""" atau '''...'''. Versi `${file.jarVersion}` diganti hasil
     * [jarVersion]; placeholder lain yang tak terselesaikan dibuang (null) supaya tidak tampil mentah.
     */
    fun parseModsToml(text: String, jarVersion: () -> String?): ModJarInfo? {
        val lines = text.lines()
        var i = 0
        while (i < lines.size && !lines[i].trim().startsWith("[[mods]]")) i++
        if (i >= lines.size) return null
        i++

        var modId: String? = null
        var displayName: String? = null
        var version: String? = null
        var description: String? = null

        while (i < lines.size) {
            val line = lines[i].trim()
            i++
            if (line.isEmpty() || line.startsWith("#")) continue
            if (line.startsWith("[")) break // tabel berikutnya ([[dependencies.x]] dan sebagainya)

            val eq = line.indexOf('=')
            if (eq <= 0) continue
            val key = line.substring(0, eq).trim()
            var rest = line.substring(eq + 1).trim()

            val value: String?
            val delimiter = when {
                rest.startsWith("\"\"\"") -> "\"\"\""
                rest.startsWith("'''") -> "'''"
                else -> null
            }
            if (delimiter != null) {
                rest = rest.substring(3)
                val sb = StringBuilder()
                var closed = rest.indexOf(delimiter)
                if (closed >= 0) {
                    sb.append(rest, 0, closed)
                } else {
                    sb.append(rest)
                    while (i < lines.size) {
                        val next = lines[i]
                        i++
                        closed = next.indexOf(delimiter)
                        if (closed >= 0) {
                            sb.append('\n').append(next, 0, closed)
                            break
                        }
                        sb.append('\n').append(next)
                    }
                }
                value = sb.toString().trim().replace(Regex("\\s*\\n\\s*"), " ")
            } else {
                value = singleLineString(rest)
            }

            when (key) {
                "modId" -> modId = value
                "displayName" -> displayName = value
                "version" -> version = value
                "description" -> description = value
            }
        }

        val rawVersion = version
        val resolvedVersion = when {
            rawVersion == null -> null
            rawVersion.contains(JAR_VERSION_PLACEHOLDER) -> jarVersion()
            rawVersion.contains("\${") -> null
            else -> rawVersion
        }
        val name = displayName?.takeIf { it.isNotBlank() } ?: modId
        if (name == null && resolvedVersion == null && description == null) return null
        return ModJarInfo(name, resolvedVersion, description)
    }

    /** Nilai TOML satu baris: "teks" (dengan \" dan \\), 'teks', atau tanpa tanda kutip; komentar `#` dibuang. */
    private fun singleLineString(raw: String): String? {
        if (raw.isEmpty()) return null
        val quote = raw[0]
        if (quote == '"') {
            val sb = StringBuilder()
            var i = 1
            while (i < raw.length) {
                val c = raw[i]
                if (c == '\\' && i + 1 < raw.length) {
                    when (val n = raw[i + 1]) {
                        'n' -> sb.append(' ')
                        't' -> sb.append(' ')
                        else -> sb.append(n)
                    }
                    i += 2
                    continue
                }
                if (c == '"') return sb.toString()
                sb.append(c)
                i++
            }
            return sb.toString()
        }
        if (quote == '\'') {
            val end = raw.indexOf('\'', 1)
            return if (end > 0) raw.substring(1, end) else raw.substring(1)
        }
        return raw.substringBefore('#').trim().takeIf { it.isNotEmpty() }
    }
}

// ----------------------------------------------------------------------------
// Section: core/mods/ModIconLoader.kt
// ----------------------------------------------------------------------------

/**
 * Mengambil gambar kecil (ikon) tiap mod / resource pack dari dalam filenya sendiri, tanpa internet.
 * - Mod Fabric/Quilt: field "icon" di fabric.mod.json / quilt.mod.json.
 * - Mod Forge/NeoForge: "logoFile" di mods.toml, atau mcmod.info untuk mod lama.
 * - Resource pack & datapack: pack.png di akar zip atau folder.
 * Hasil diperkecil (maks 96px), disimpan di cache memori kecil dan di folder cache supaya daftar
 * dengan ratusan mod tidak membuka ulang file jar setiap kali digulir. Bila tidak ada gambar,
 * hasilnya null dan tampilan memakai inisial nama.
 */
object ModIconLoader {

    private const val MAX_PX = 96
    private const val MAX_BYTES = 1_500_000L

    private val memory = LruCache<String, Bitmap>(64)
    private val missing = ConcurrentHashMap.newKeySet<String>()

    private fun keyOf(file: File): String =
        "${file.absolutePath}|${file.length()}|${file.lastModified()}"

    private fun sha1(text: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(text.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    /** Hasil dari memori (tanpa membuka file), atau null bila belum pernah dimuat. */
    fun peek(path: String): Bitmap? {
        if (path.isEmpty()) return null
        val file = File(path)
        if (!file.exists()) return null
        return memory.get(keyOf(file))
    }

    suspend fun load(context: Context, file: File, itemType: String): Bitmap? = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext null
        val key = keyOf(file)
        val cached = memory.get(key)
        if (cached != null) return@withContext cached
        if (missing.contains(key)) return@withContext null

        val dir = File(context.cacheDir, "mod_icons")
        if (!dir.exists()) dir.mkdirs()
        val hash = sha1(key)
        val pngFile = File(dir, "$hash.png")
        val noneFile = File(dir, "$hash.none")

        if (pngFile.exists()) {
            val fromDisk = decodeScaled(pngFile.readBytes())
            if (fromDisk != null) {
                memory.put(key, fromDisk)
                return@withContext fromDisk
            }
        }
        if (noneFile.exists()) {
            missing.add(key)
            return@withContext null
        }

        val bytes = try {
            extractIconBytes(file, itemType)
        } catch (_: Exception) {
            null
        }
        val bitmap = if (bytes != null) decodeScaled(bytes) else null
        if (bitmap == null) {
            missing.add(key)
            try {
                noneFile.createNewFile()
            } catch (_: Exception) {
            }
            return@withContext null
        }

        memory.put(key, bitmap)
        try {
            FileOutputStream(pngFile).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        } catch (_: Exception) {
        }
        bitmap
    }

    private fun extractIconBytes(file: File, itemType: String): ByteArray? {
        if (file.isDirectory) {
            if (itemType == "worlds") {
                val iconPng = File(file, "icon.png")
                if (iconPng.isFile && iconPng.length() < MAX_BYTES) return iconPng.readBytes()
            }
            val packPng = File(file, "pack.png")
            return if (packPng.isFile && packPng.length() < MAX_BYTES) packPng.readBytes() else null
        }
        if (!file.isFile) return null
        return ZipFile(file).use { zip -> findIconInZip(zip, itemType) }
    }

    private fun readEntry(zip: ZipFile, path: String): ByteArray? {
        val entry = zip.getEntry(path.trimStart('/')) ?: return null
        if (entry.isDirectory || entry.size > MAX_BYTES) return null
        return zip.getInputStream(entry).use { it.readBytes() }
    }

    private fun readText(zip: ZipFile, path: String): String? {
        val entry = zip.getEntry(path) ?: return null
        if (entry.isDirectory || entry.size > 512_000L) return null
        return zip.getInputStream(entry).bufferedReader().use { it.readText() }
    }

    private fun findIconInZip(zip: ZipFile, itemType: String): ByteArray? {
        if (itemType != "mods") {
            return readEntry(zip, "pack.png")
        }

        val candidates = mutableListOf<String>()

        readText(zip, "fabric.mod.json")?.let { text ->
            try {
                val root = JsonParser.parseString(text).asJsonObject
                candidates.addAll(iconPaths(root.get("icon")))
            } catch (_: Exception) {
            }
        }
        readText(zip, "quilt.mod.json")?.let { text ->
            try {
                val root = JsonParser.parseString(text).asJsonObject
                val metadata = root.getAsJsonObject("quilt_loader")?.getAsJsonObject("metadata")
                candidates.addAll(iconPaths(metadata?.get("icon")))
            } catch (_: Exception) {
            }
        }
        for (tomlPath in listOf("META-INF/neoforge.mods.toml", "META-INF/mods.toml")) {
            readText(zip, tomlPath)?.let { text ->
                val match = Regex("logoFile\\s*=\\s*[\"']([^\"']+)[\"']").find(text)
                if (match != null) candidates.add(match.groupValues[1])
            }
        }
        readText(zip, "mcmod.info")?.let { text ->
            val match = Regex("\"logoFile\"\\s*:\\s*\"([^\"]+)\"").find(text)
            if (match != null) candidates.add(match.groupValues[1])
        }
        candidates.addAll(listOf("pack.png", "icon.png", "logo.png"))

        for (candidate in candidates) {
            val bytes = readEntry(zip, candidate)
            if (bytes != null) return bytes
        }
        return null
    }

    /** Field "icon" bisa berupa satu path atau peta ukuran -> path; ambil yang terbesar dulu. */
    private fun iconPaths(element: JsonElement?): List<String> {
        if (element == null || element.isJsonNull) return emptyList()
        if (element.isJsonPrimitive) return listOf(element.asString)
        if (element.isJsonObject) {
            return element.asJsonObject.entrySet()
                .sortedByDescending { it.key.toIntOrNull() ?: 0 }
                .mapNotNull { entry ->
                    val value = entry.value
                    if (value.isJsonPrimitive) value.asString else null
                }
        }
        return emptyList()
    }

    private fun decodeScaled(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (bounds.outWidth / (sample * 2) >= MAX_PX && bounds.outHeight / (sample * 2) >= MAX_PX) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null

        val longestSide = max(bitmap.width, bitmap.height)
        if (longestSide <= MAX_PX) return bitmap
        val ratio = MAX_PX.toFloat() / longestSide
        val newWidth = max(1, (bitmap.width * ratio).roundToInt())
        val newHeight = max(1, (bitmap.height * ratio).roundToInt())
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}

// ----------------------------------------------------------------------------
// Section: core/mods/CurseForgeApi.kt
// ----------------------------------------------------------------------------

/**
 * Klien CurseForge. Hasilnya dipetakan oleh [CurseForgeMapper] ke model yang sama dengan Modrinth.
 *
 * Mode tanpa API key (default): menggunakan API publik CurseForge (www.curseforge.com/api/v1,
 * cfproxy.fly.dev/v1, serta api.cfwidget.com sebagai cadangan detail & file) sehingga Modrinth dan
 * CurseForge sama-sama bisa langsung dipakai tanpa API key.
 * Mode dengan API key (opsional): jika pengguna mengisi key di Pengaturan atau BuildConfig.CURSEFORGE_API_KEY
 * tersedia, permintaan diarahkan ke API resmi api.curseforge.com/v1 lewat header x-api-key (dengan
 * fallback otomatis ke mode tanpa key bila key ditolak).
 *
 * Mod yang penulisnya menutup unduhan lewat API (allowModDistribution == false / downloadUrl kosong di
 * endpoint resmi) tetap dihormati dan pengguna diberi tahu.
 */
object CurseForgeApi {
    private const val OFFICIAL_BASE = "https://api.curseforge.com/v1"
    private const val CURSE_TOOLS_BASE = "https://api.curse.tools/v1/cf"
    private const val WEB_BASE = "https://www.curseforge.com/api/v1"
    private const val CFWIDGET_BASE = "https://api.cfwidget.com"

    /** Batas jendela indeks halaman API CurseForge; halaman lebih tinggi ditangani halus tanpa error. */
    private const val MAX_PAGE_SIZE = 50
    private const val MAX_RESULT_WINDOW = 10_000

    /** Jumlah id per permintaan massal (POST /mods/files dan /mods). */
    private const val BULK_CHUNK = 100
    private const val SEARCH_CACHE_TTL_MS = 90_000L
    private val searchCache = LruCache<String, Pair<Long, ModrinthSearchResponse>>(48)
    private val projectCache = ConcurrentHashMap<String, Pair<Long, ModrinthProject>>()

    /** Kunci opsional dari Pengaturan pengguna dipakai lebih dulu, lalu kunci build (bila ada). */
    fun apiKey(): String {
        val userKey = com.aistudio.novacraft.launcher.core.utils.SettingsManager.settings.value.curseForgeApiKey.trim()
        if (userKey.isNotEmpty()) return userKey
        return BuildConfig.CURSEFORGE_API_KEY.trim()
    }

    fun hasApiKey(): Boolean = apiKey().isNotEmpty()

    /** CurseForge bị ẩn khi tắt thủ công hoặc khi chế độ Local-Only đang bật. */
    fun isConfigured(): Boolean {
        val s = com.aistudio.novacraft.launcher.core.utils.SettingsManager.settings.value
        return !s.disableCurseForge && !s.localOnlyMode
    }

    // ------------------------------------------------------------------
    // Permintaan dasar
    // ------------------------------------------------------------------

    private fun query(vararg params: Pair<String, String?>): String {
        val sb = StringBuilder()
        for ((name, value) in params) {
            if (value.isNullOrEmpty()) continue
            sb.append(if (sb.isEmpty()) '?' else '&')
            sb.append(name).append('=').append(URLEncoder.encode(value, "UTF-8").replace("+", "%20"))
        }
        return sb.toString()
    }

    private fun authed(url: String): Request.Builder {
        val key = apiKey()
        if (key.isEmpty()) throw IllegalStateException("API key CurseForge kosong.")
        return Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("x-api-key", key)
            .header("User-Agent", NuxModManager.USER_AGENT)
    }

    private fun publicWeb(url: String): Request.Builder = Request.Builder()
        .url(url)
        .header("Accept", "application/json, text/plain, */*")
        .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36")
        .header("Referer", "https://www.curseforge.com/")
        .header("Origin", "https://www.curseforge.com")

    private fun publicJson(url: String): Request.Builder = Request.Builder()
        .url(url)
        .header("Accept", "application/json")
        .header("User-Agent", NuxModManager.USER_AGENT)

    private fun httpError(code: Int): Exception = when (code) {
        401, 403 -> Exception("Layanan CurseForge sementara menolak permintaan (kode $code).")
        429 -> Exception("CurseForge membatasi permintaan. Tunggu sebentar lalu coba lagi.")
        else -> Exception("CurseForge API error: kode $code")
    }

    private fun execute(request: Request): JsonObject {
        return NuxModManager.httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw httpError(response.code)
            val body = response.body?.string() ?: throw Exception("Respons kosong dari CurseForge")
            val trimmed = body.trim()
            if (trimmed.startsWith("<")) {
                throw Exception("Layanan CurseForge sementara mengembalikan halaman verifikasi.")
            }
            val parsed = JsonParser.parseString(trimmed)
            when {
                parsed.isJsonObject -> parsed.asJsonObject
                parsed.isJsonArray -> JsonObject().apply { add("data", parsed.asJsonArray) }
                else -> throw Exception("Respons CurseForge tidak dikenali")
            }
        }
    }

    private fun gameVersionOf(
        instance: Instance?,
        filterByGameVersion: Boolean,
        mcVersionOverride: String? = null
    ): String? {
        if (!filterByGameVersion) return null
        val explicit = mcVersionOverride?.trim()?.takeIf {
            it.isNotEmpty() && !it.equals("all", ignoreCase = true)
        }
        if (explicit != null) return explicit
        return instance?.mcVersion?.trim()?.takeIf { it.isNotEmpty() }
    }

    // ------------------------------------------------------------------
    // Cari, detail, versi
    // ------------------------------------------------------------------

    suspend fun search(
        queryText: String,
        itemType: String,
        instance: Instance?,
        filterByGameVersion: Boolean,
        page: Int,
        limit: Int,
        sortIndex: String,
        filterByLoader: Boolean,
        category: String? = null,
        mcVersionOverride: String? = null,
        loaderOverride: String? = null
    ): Result<ModrinthSearchResponse> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext Result.failure(IllegalStateException("CurseForge dinonaktifkan di Pengaturan."))
        }
        try {
            val size = limit.coerceIn(1, MAX_PAGE_SIZE)
            val offset = (page - 1).coerceAtLeast(0) * size
            if (offset >= MAX_RESULT_WINDOW) {
                return@withContext Result.success(
                    ModrinthSearchResponse(
                        hits = emptyList(),
                        offset = offset,
                        limit = size,
                        totalHits = MAX_RESULT_WINDOW
                    )
                )
            }
            val reqPageSize = size.coerceAtMost((MAX_RESULT_WINDOW - offset).coerceAtLeast(1))
            val gameVersion = gameVersionOf(instance, filterByGameVersion, mcVersionOverride)
            val wantedLoader = if (filterByLoader && (itemType == "mods" || itemType == "modpacks")) {
                val explicitLoader = loaderOverride?.trim()?.lowercase()?.takeIf {
                    it.isNotEmpty() && it != "all"
                }
                if (loaderOverride.equals("all", ignoreCase = true)) {
                    null
                } else {
                    explicitLoader ?: NuxModManager.effectiveLoader(instance)
                }
            } else {
                null
            }
            val loaderType = if (wantedLoader != null) CurseForgeMapper.loaderTypeFor(wantedLoader) else 0
            val mappedCategoryId = CurseForgeMapper.categoryIdFor(itemType, category)
            val baseText = queryText.trim().takeIf { it.isNotEmpty() }
            val qText = if (baseText == null && mappedCategoryId == 0 && !category.isNullOrBlank() && !category.equals("Semua", ignoreCase = true)) {
                category.trim()
            } else {
                baseText
            }
            val classId = CurseForgeMapper.classIdFor(itemType).toString()
            val sortField = CurseForgeMapper.sortFieldFor(sortIndex).toString()
            val cacheKey = "$itemType|$category|$qText|$gameVersion|$wantedLoader|$sortIndex|$page|$size|${hasApiKey()}"
            val nowMs = System.currentTimeMillis()
            val cached = synchronized(searchCache) { searchCache.get(cacheKey) }
            if (cached != null && nowMs - cached.first < SEARCH_CACHE_TTL_MS) {
                return@withContext Result.success(cached.second)
            }

            fun executeSearchRequest(
                reqQuery: String?,
                reqLoaderType: Int,
                reqCategoryId: Int,
                reqSize: Int,
                reqOffset: Int
            ): JsonObject {
                return if (hasApiKey()) {
                    val officialUrl = OFFICIAL_BASE + "/mods/search" + query(
                        "gameId" to CurseForgeMapper.GAME_ID.toString(),
                        "classId" to classId,
                        "categoryId" to reqCategoryId.takeIf { it != 0 }?.toString(),
                        "searchFilter" to reqQuery,
                        "gameVersion" to gameVersion,
                        "modLoaderType" to reqLoaderType.takeIf { it != 0 }?.toString(),
                        "sortField" to sortField,
                        "sortOrder" to "desc",
                        "index" to reqOffset.toString(),
                        "pageSize" to reqSize.toString()
                    )
                    try {
                        execute(authed(officialUrl).build())
                    } catch (_: Exception) {
                        searchKeyless(classId, reqCategoryId, reqQuery, gameVersion, reqLoaderType, sortField, reqOffset, reqSize)
                    }
                } else {
                    searchKeyless(classId, reqCategoryId, reqQuery, gameVersion, reqLoaderType, sortField, reqOffset, reqSize)
                }
            }

            var root = executeSearchRequest(qText, loaderType, mappedCategoryId, reqPageSize, offset)
            var hits = CurseForgeMapper.dataObjects(root).mapNotNull { CurseForgeMapper.toSearchHit(it, itemType) }

            // Sebagian endpoint CurseForge menolak atau mengosongkan hasil bila modLoaderType dikirim tanpa gameVersion.
            if (hits.isEmpty() && loaderType != 0 && gameVersion == null) {
                val relaxedSize = MAX_PAGE_SIZE.coerceAtMost((MAX_RESULT_WINDOW - offset).coerceAtLeast(1))
                val fallbackRoot = runCatching {
                    executeSearchRequest(qText, 0, mappedCategoryId, relaxedSize, offset)
                }.getOrNull()
                if (fallbackRoot != null) {
                    val unfilteredHits = CurseForgeMapper.dataObjects(fallbackRoot)
                        .mapNotNull { CurseForgeMapper.toSearchHit(it, itemType) }
                    val filtered = unfilteredHits.filter { CurseForgeMapper.searchHitMatchesLoader(it, wantedLoader) }
                    if (filtered.isNotEmpty()) {
                        root = fallbackRoot
                        hits = filtered.take(reqPageSize)
                    } else if (unfilteredHits.isNotEmpty()) {
                        root = fallbackRoot
                        hits = unfilteredHits.take(reqPageSize)
                    }
                }
            }

            // Jika categoryId belum cocok di CurseForge, coba longgarkan categoryId sambil menjaga pencarian tetap akurat.
            if (hits.isEmpty() && mappedCategoryId != 0 && page == 1) {
                val noCatRoot = runCatching {
                    executeSearchRequest(qText ?: category?.trim(), loaderType, 0, reqPageSize, 0)
                }.getOrNull()
                if (noCatRoot != null) {
                    val noCatHits = CurseForgeMapper.dataObjects(noCatRoot).mapNotNull { CurseForgeMapper.toSearchHit(it, itemType) }
                    if (noCatHits.isNotEmpty()) {
                        root = noCatRoot
                        hits = noCatHits
                    }
                }
            }

            val total = CurseForgeMapper.totalCountOf(root).let { t ->
                if (t > 0) t else hits.size
            }
            val resultResp = ModrinthSearchResponse(hits = hits, offset = offset, limit = size, totalHits = total)
            if (hits.isNotEmpty()) {
                synchronized(searchCache) {
                    searchCache.put(cacheKey, System.currentTimeMillis() to resultResp)
                }
            }
            Result.success(resultResp)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun searchKeyless(
        classId: String,
        categoryId: Int,
        qText: String?,
        gameVersion: String?,
        loaderType: Int,
        sortField: String,
        offset: Int,
        size: Int
    ): JsonObject {
        val catStr = categoryId.takeIf { it != 0 }?.toString()
        val loaderStr = loaderType.takeIf { it != 0 }?.toString()
        val stdQuery = query(
            "gameId" to CurseForgeMapper.GAME_ID.toString(),
            "classId" to classId,
            "categoryId" to catStr,
            "searchFilter" to qText,
            "gameVersion" to gameVersion,
            "modLoaderType" to loaderStr,
            "sortField" to sortField,
            "sortOrder" to "desc",
            "index" to offset.toString(),
            "pageSize" to size.toString()
        )
        return try {
            execute(publicJson("$CURSE_TOOLS_BASE/mods/search$stdQuery").build())
        } catch (firstErr: Exception) {
            // Coba ulang sekali bila terjadi lonjakan jaringan sementara sebelum fallback ke mirror cadangan
            val retried = runCatching {
                execute(publicJson("$CURSE_TOOLS_BASE/mods/search$stdQuery").build())
            }.getOrNull()
            if (retried != null) return retried

            val webUrl = WEB_BASE + "/mods/search" + query(
                "gameId" to CurseForgeMapper.GAME_ID.toString(),
                "classId" to classId,
                "categoryId" to catStr,
                "filterText" to qText,
                "gameVersion" to gameVersion,
                "modLoaderType" to loaderStr,
                "sortField" to sortField,
                "sortOrder" to "desc",
                "index" to offset.toString(),
                "pageSize" to size.toString()
            )
            runCatching {
                execute(publicWeb(webUrl).build())
            }.getOrNull() ?: throw firstErr
        }
    }

    suspend fun projectDetails(prefixedId: String): Result<ModrinthProject> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext Result.failure(IllegalStateException("CurseForge dinonaktifkan di Pengaturan."))
        }
        try {
            val id = CurseForgeMapper.modIdOf(prefixedId)
                ?: return@withContext Result.failure(Exception("Id CurseForge tidak valid"))
            val cached = projectCache[prefixedId]
            if (cached != null && System.currentTimeMillis() - cached.first < SEARCH_CACHE_TTL_MS * 4) {
                return@withContext Result.success(cached.second)
            }
            if (hasApiKey()) {
                try {
                    val root = execute(authed("$OFFICIAL_BASE/mods/$id").build())
                    val mod = CurseForgeMapper.dataObject(root)
                    val project = if (mod != null) CurseForgeMapper.toProject(mod) else null
                    if (project != null) {
                        projectCache[prefixedId] = System.currentTimeMillis() to project
                        return@withContext Result.success(project)
                    }
                } catch (_: Exception) {
                }
            }
            val keylessProject = runCatching {
                val root = execute(publicJson("$CURSE_TOOLS_BASE/mods/$id").build())
                val mod = CurseForgeMapper.dataObject(root) ?: root
                CurseForgeMapper.toProject(mod)
            }.getOrNull() ?: runCatching {
                val root = execute(publicWeb("$WEB_BASE/mods/$id").build())
                val mod = CurseForgeMapper.dataObject(root) ?: root
                CurseForgeMapper.toProject(mod)
            }.getOrNull() ?: runCatching {
                val widgetRoot = execute(publicJson("$CFWIDGET_BASE/$id").build())
                CurseForgeMapper.cfWidgetToProject(widgetRoot)
            }.getOrNull()

            if (keylessProject == null) {
                Result.failure(Exception("Detail proyek sementara tidak tersedia"))
            } else {
                projectCache[prefixedId] = System.currentTimeMillis() to keylessProject
                Result.success(keylessProject)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * File yang cocok dengan instance dan bisa diunduh lewat launcher, rilis stabil lebih dulu lalu yang
     * terbaru. Mendukung fallback CDN otomatis sehingga seluruh mod CurseForge tetap tampil normal.
     */
    suspend fun compatibleVersions(
        prefixedId: String,
        instance: Instance?,
        itemType: String,
        filterByGameVersion: Boolean,
        mcVersionOverride: String? = null,
        loaderOverride: String? = null,
        filterByLoader: Boolean = true
    ): Result<List<ModrinthVersion>> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext Result.failure(IllegalStateException("CurseForge dinonaktifkan di Pengaturan."))
        }
        try {
            val id = CurseForgeMapper.modIdOf(prefixedId)
                ?: return@withContext Result.failure(Exception("Id CurseForge tidak valid"))
            val gameVersion = gameVersionOf(instance, filterByGameVersion, mcVersionOverride)
            val wantedLoader = if (filterByLoader && !loaderOverride.equals("all", ignoreCase = true) && (itemType == "mods" || itemType == "modpacks")) {
                NuxModManager.effectiveLoader(instance, loaderOverride)
            } else {
                null
            }
            val loaderType = if (gameVersion != null && wantedLoader != null) CurseForgeMapper.loaderTypeFor(wantedLoader) else 0

            fun fetchRawFiles(reqGameVersion: String?, reqLoaderType: Int): List<JsonObject>? {
                val filesQuery = query(
                    "gameVersion" to reqGameVersion,
                    "modLoaderType" to reqLoaderType.takeIf { it != 0 }?.toString(),
                    "index" to "0",
                    "pageSize" to MAX_PAGE_SIZE.toString()
                )
                if (hasApiKey()) {
                    val official = runCatching { execute(authed("$OFFICIAL_BASE/mods/$id/files$filesQuery").build()) }.getOrNull()
                    if (official != null) {
                        val list = CurseForgeMapper.dataObjects(official)
                        if (list.isNotEmpty()) return list
                    }
                }
                val curseTools = runCatching { execute(publicJson("$CURSE_TOOLS_BASE/mods/$id/files$filesQuery").build()) }.getOrNull()
                if (curseTools != null) {
                    val list = CurseForgeMapper.dataObjects(curseTools)
                    if (list.isNotEmpty()) return list
                }
                val web = runCatching { execute(publicWeb("$WEB_BASE/mods/$id/files$filesQuery").build()) }.getOrNull()
                if (web != null) {
                    val list = CurseForgeMapper.dataObjects(web)
                    if (list.isNotEmpty()) return list
                }
                return null
            }

            var raw = fetchRawFiles(gameVersion, loaderType)
            if (raw.isNullOrEmpty() && loaderType != 0) {
                raw = fetchRawFiles(gameVersion, 0)
            }
            if (raw.isNullOrEmpty() && gameVersion != null && (itemType == "worlds" || itemType == "resourcepacks" || itemType == "shaderpacks")) {
                raw = fetchRawFiles(null, 0)
            }

            if (raw != null) {
                val usable = CurseForgeMapper.orderedFiles(raw)
                    .mapNotNull { CurseForgeMapper.toVersion(it, modIdFallback = id, allowCdnFallback = true) }
                    .filter { CurseForgeMapper.loaderMatches(it.loaders, wantedLoader) }

                if (usable.isNotEmpty()) {
                    return@withContext Result.success(usable)
                }
            }

            // Cadangan publik tanpa kunci melalui CFWidget bila endpoint file utama belum mengembalikan hasil
            val widgetRoot = runCatching { execute(publicJson("$CFWIDGET_BASE/$id").build()) }.getOrNull()
            val widgetVersions = if (widgetRoot != null) {
                CurseForgeMapper.cfWidgetToVersions(widgetRoot, id, gameVersion, wantedLoader)
            } else {
                emptyList()
            }
            Result.success(widgetVersions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ------------------------------------------------------------------
    // Modpack: cari alamat unduhan banyak file sekaligus (blocking, panggil dari thread IO)
    // ------------------------------------------------------------------

    private fun idsBody(field: String, ids: List<Long>): okhttp3.RequestBody {
        val arr = JsonArray()
        for (id in ids) arr.add(id)
        val obj = JsonObject()
        obj.add(field, arr)
        return obj.toString().toRequestBody("application/json".toMediaType())
    }

    /** Alamat unduhan, nama, dan sidik jari tiap file (kunci = id file). File yang tidak dikenal CurseForge tidak ada di hasil. */
    fun resolveFiles(fileIds: List<Long>): Map<Long, CurseForgeMapper.ResolvedFile> =
        resolvePackFiles(fileIds.distinct().map { CurseForgeMapper.PackFile(0L, it, true) })

    /** Versi modpack dengan projectId + fileId agar tetap bisa resolve tanpa API key. */
    fun resolvePackFiles(packFiles: List<CurseForgeMapper.PackFile>): Map<Long, CurseForgeMapper.ResolvedFile> {
        val out = HashMap<Long, CurseForgeMapper.ResolvedFile>()
        if (!isConfigured() || packFiles.isEmpty()) return out

        val fileIds = packFiles.map { it.fileId }.distinct()
        var batchSucceeded = false
        if (hasApiKey()) {
            for (chunk in fileIds.chunked(BULK_CHUNK)) {
                val batchRoot = runCatching {
                    execute(authed("$OFFICIAL_BASE/mods/files").post(idsBody("fileIds", chunk)).build())
                }.getOrNull()
                if (batchRoot != null) {
                    batchSucceeded = true
                    for (f in CurseForgeMapper.dataObjects(batchRoot)) {
                        CurseForgeMapper.toResolvedFile(f, allowCdnFallback = true)?.let { out[it.fileId] = it }
                    }
                }
            }
        }

        // Fallback per file tanpa API key (curse.tools / web API / CFWidget)
        val missing = packFiles.filter { it.projectId > 0L && !out.containsKey(it.fileId) }
        val widgetCache = HashMap<Long, JsonObject>()
        for (pf in missing) {
            val toolsFile = runCatching {
                val r = execute(publicJson("$CURSE_TOOLS_BASE/mods/${pf.projectId}/files/${pf.fileId}").build())
                CurseForgeMapper.dataObject(r) ?: r
            }.getOrNull() ?: runCatching {
                val r = execute(publicWeb("$WEB_BASE/mods/${pf.projectId}/files/${pf.fileId}").build())
                CurseForgeMapper.dataObject(r) ?: r
            }.getOrNull()
            val resolvedTools = toolsFile?.let {
                CurseForgeMapper.toResolvedFile(it, modIdFallback = pf.projectId, allowCdnFallback = true)
            }
            if (resolvedTools != null) {
                out[resolvedTools.fileId] = resolvedTools
                continue
            }
            if (!batchSucceeded || out[pf.fileId] == null) {
                val widget = widgetCache.getOrPut(pf.projectId) {
                    runCatching { execute(publicJson("$CFWIDGET_BASE/${pf.projectId}").build()) }.getOrDefault(JsonObject())
                }
                CurseForgeMapper.cfWidgetResolvedFile(widget, pf.projectId, pf.fileId)?.let {
                    out[it.fileId] = it
                }
            }
        }
        return out
    }

    /** classId tiap proyek (kunci = id proyek). Hanya pelengkap: bila gagal, hasilnya kosong. */
    fun classIdsOf(modIds: List<Long>): Map<Long, Int> {
        val out = HashMap<Long, Int>()
        if (!isConfigured()) return out
        if (hasApiKey()) {
            for (chunk in modIds.distinct().chunked(BULK_CHUNK)) {
                try {
                    val root = execute(authed("$OFFICIAL_BASE/mods").post(idsBody("modIds", chunk)).build())
                    for (m in CurseForgeMapper.dataObjects(root)) {
                        CurseForgeMapper.classIdOfMod(m)?.let { out[it.first] = it.second }
                    }
                } catch (_: Exception) {
                }
            }
        }
        for (modId in modIds.distinct()) {
            if (out.containsKey(modId)) continue
            runCatching {
                val root = execute(publicJson("$CURSE_TOOLS_BASE/mods/$modId").build())
                val mod = CurseForgeMapper.dataObject(root) ?: root
                CurseForgeMapper.classIdOfMod(mod)?.let { out[it.first] = it.second }
            }
        }
        return out
    }
}

// ----------------------------------------------------------------------------
// Section: core/mods/CurseForgeMapper.kt
// ----------------------------------------------------------------------------

/**
 * Pemetaan data CurseForge ke model yang dipakai layar Mod (bentuknya model Modrinth), sehingga daftar,
 * jendela detail, dan pengunduh tidak perlu tahu sumbernya. Murni JSON ke model, tanpa jaringan dan
 * tanpa Android, jadi mudah diuji.
 *
 * Semua id proyek CurseForge diberi awalan [ID_PREFIX] ("cf:123"). Awalan itulah yang memberi tahu
 * NuxModManager harus bertanya ke CurseForge, bukan ke Modrinth, untuk detail, versi, dan dependensi.
 *
 * JSON dibaca tanpa refleksi (JsonObject), jadi aman dari R8 dan tidak mengalokasi kelas tambahan.
 */
object CurseForgeMapper {
    const val ID_PREFIX = "cf:"

    /** Id game Minecraft di CurseForge. */
    const val GAME_ID = 432

    /** Pesan untuk mod yang penulisnya melarang unduhan lewat aplikasi pihak ketiga. */
    const val BLOCKED_NOTE = "Penulis melarang unduhan lewat launcher. Unduh manual dari situs CurseForge."

    fun isCurseForgeId(id: String?): Boolean = id != null && id.startsWith(ID_PREFIX)

    fun prefixed(modId: Long): String = "$ID_PREFIX$modId"

    /** Id numerik dari "cf:123", atau null bila bukan id CurseForge yang sah. */
    fun modIdOf(prefixedId: String?): Long? {
        if (prefixedId == null || !prefixedId.startsWith(ID_PREFIX)) return null
        return prefixedId.substring(ID_PREFIX.length).toLongOrNull()
    }

    /** classId CurseForge untuk jenis item di launcher. */
    fun classIdFor(itemType: String): Int = when (itemType) {
        "modpacks" -> 4471
        "worlds" -> 17
        "resourcepacks" -> 12
        "shaderpacks" -> 6552
        "datapacks" -> 6945
        else -> 6
    }

    /** categoryId CurseForge untuk kategori populer (0 = tanpa filter kategori khusus). */
    fun categoryIdFor(itemType: String, category: String?): Int {
        val cat = category?.trim()?.lowercase()?.takeIf { it.isNotEmpty() && it != "semua" } ?: return 0
        return when (itemType) {
            "mods" -> when (cat) {
                "technology" -> 412
                "magic" -> 419
                "adventure" -> 422
                "utility" -> 5191
                "performance", "optimization" -> 6814
                "cosmetic", "decoration" -> 424
                "worldgen", "world-gen" -> 406
                "storage" -> 420
                "food" -> 436
                "equipment", "armor" -> 434
                else -> 0
            }
            "modpacks" -> when (cat) {
                "technology" -> 4472
                "magic" -> 4473
                "adventure" -> 4475
                "optimization", "performance" -> 4476
                "quests" -> 4478
                else -> 0
            }
            else -> 0
        }
    }

    /** Nilai project_type yang dipakai layar (sama dengan istilah Modrinth). */
    fun projectTypeFor(itemType: String): String = when (itemType) {
        "resourcepacks" -> "resourcepack"
        "shaderpacks" -> "shader"
        "datapacks" -> "datapack"
        "modpacks" -> "modpack"
        "worlds" -> "world"
        else -> "mod"
    }

    /** modLoaderType CurseForge untuk nama loader; 0 = tidak dikenal (tanpa penyaringan). */
    fun loaderTypeFor(loader: String?): Int = when (loader?.trim()?.lowercase()) {
        "forge" -> 1
        "fabric" -> 4
        "quilt" -> 5
        "neoforge" -> 6
        else -> 0
    }

    private fun loaderNameFor(type: Int): String? = when (type) {
        1 -> "forge"
        4 -> "fabric"
        5 -> "quilt"
        6 -> "neoforge"
        else -> null
    }

    /** sortField CurseForge: 1=Featured, 2=Popularity, 3=LastUpdated, 6=TotalDownloads, 11=ReleasedDate. */
    fun sortFieldFor(sortIndex: String): Int = when (sortIndex) {
        "downloads" -> 6
        "follows" -> 2
        "updated" -> 3
        "newest" -> 11
        else -> 1
    }

    fun searchHitMatchesLoader(hit: ModrinthSearchHit, wantedLoader: String?): Boolean {
        if (wantedLoader.isNullOrBlank()) return true
        val hitLoaders = hit.categories.orEmpty()
            .map { it.lowercase() }
            .filter { it in KNOWN_LOADER_NAMES }
        if (hitLoaders.isEmpty()) return true
        return hitLoaders.any { it.equals(wantedLoader, ignoreCase = true) }
    }

    // ------------------------------------------------------------------
    // Pembaca JSON yang tahan JsonNull dan tipe tak terduga
    // ------------------------------------------------------------------

    private fun JsonObject.str(name: String): String? {
        val e = get(name) ?: return null
        return if (e.isJsonPrimitive) e.asString else null
    }

    private fun JsonObject.long(name: String): Long? {
        val e = get(name) ?: return null
        if (!e.isJsonPrimitive) return null
        return try {
            e.asLong
        } catch (_: Exception) {
            null
        }
    }

    private fun JsonObject.int(name: String): Int? = long(name)?.toInt()

    private fun JsonObject.obj(name: String): JsonObject? {
        val e = get(name) ?: return null
        return if (e.isJsonObject) e.asJsonObject else null
    }

    private fun JsonObject.objects(name: String): List<JsonObject> {
        val e = get(name) ?: return emptyList()
        if (!e.isJsonArray) return emptyList()
        val out = ArrayList<JsonObject>(e.asJsonArray.size())
        for (item in e.asJsonArray) {
            if (item.isJsonObject) out.add(item.asJsonObject)
        }
        return out
    }

    /** true / false bila ada nilai boolean yang sah, null bila kosong atau JsonNull. */
    private fun JsonObject.bool(name: String): Boolean? {
        val e: JsonElement = get(name) ?: return null
        if (!e.isJsonPrimitive || !e.asJsonPrimitive.isBoolean) return null
        return e.asBoolean
    }

    private fun JsonObject.strings(name: String): List<String> {
        val e = get(name) ?: return emptyList()
        if (!e.isJsonArray) return emptyList()
        val out = ArrayList<String>(e.asJsonArray.size())
        for (item in e.asJsonArray) {
            if (item.isJsonPrimitive) {
                val v = item.asString.trim()
                if (v.isNotEmpty()) out.add(v)
            }
        }
        return out
    }

    /** Isi larik "data" pada respons CurseForge (hanya yang berupa objek). */
    fun dataObjects(root: JsonObject): List<JsonObject> = root.objects("data")

    /** Objek tunggal pada "data" (mis. GET /mods/{id}), atau null. */
    fun dataObject(root: JsonObject): JsonObject? = root.obj("data")

    /** Total hasil pada "pagination.totalCount", atau 0. */
    fun totalCountOf(root: JsonObject): Int = root.obj("pagination")?.int("totalCount") ?: 0

    // ------------------------------------------------------------------
    // Mod -> hasil pencarian dan detail proyek
    // ------------------------------------------------------------------

    /** Loader yang dikenal dari latestFilesIndexes (modLoader) maupun gameVersions teks, tanpa duplikat. */
    private fun loadersOfMod(mod: JsonObject): List<String> {
        val out = LinkedHashSet<String>()
        for (idx in mod.objects("latestFilesIndexes")) {
            val type = idx.int("modLoader") ?: continue
            loaderNameFor(type)?.let { out.add(it) }
        }
        for (gv in mod.strings("gameVersions")) {
            val lower = gv.lowercase()
            if (lower in KNOWN_LOADER_NAMES) out.add(lower)
        }
        mod.obj("latestFileDetails")?.let { latest ->
            out.addAll(loaderNamesOf(latest))
        }
        return out.toList()
    }

    private fun gameVersionsOfMod(mod: JsonObject): List<String> {
        val out = LinkedHashSet<String>()
        for (idx in mod.objects("latestFilesIndexes")) {
            idx.str("gameVersion")?.let { if (it.isNotBlank()) out.add(it) }
        }
        for (gv in mod.strings("gameVersions")) {
            if (gv.isNotEmpty() && gv[0].isDigit()) out.add(gv)
        }
        for (gv in mod.strings("versions")) {
            if (gv.isNotEmpty() && gv[0].isDigit()) out.add(gv)
        }
        return out.toList()
    }

    /** Penulis melarang unduhan pihak ketiga bila allowModDistribution bernilai false (null = tidak diketahui). */
    fun isDistributionBlocked(mod: JsonObject): Boolean = mod.bool("allowModDistribution") == false

    private fun downloadsOf(mod: JsonObject): Long =
        mod.long("downloadCount")
            ?: mod.long("downloads")
            ?: mod.obj("downloads")?.long("total")
            ?: 0L

    private fun authorOf(mod: JsonObject): String? =
        mod.objects("authors").firstOrNull()?.str("name")
            ?: mod.obj("author")?.str("name")
            ?: mod.str("author")

    private fun categorySlugsOf(mod: JsonObject): List<String> {
        val objSlugs = mod.objects("categories").mapNotNull { it.str("slug") ?: it.str("name") }.filter { it.isNotBlank() }
        return if (objSlugs.isNotEmpty()) objSlugs else mod.strings("categories")
    }

    private fun iconOf(mod: JsonObject): String? {
        val logo = mod.obj("logo")
        val fromLogo = logo?.let { it.str("thumbnailUrl")?.takeIf { s -> s.isNotBlank() } ?: it.str("url") }
            ?.takeIf { it.isNotBlank() }
        if (fromLogo != null) return fromLogo
        return mod.str("avatarUrl")?.takeIf { it.isNotBlank() }
            ?: mod.str("thumbnail")?.takeIf { it.isNotBlank() }
    }

    fun toSearchHit(mod: JsonObject, itemType: String): ModrinthSearchHit? {
        val id = mod.long("id") ?: return null
        val name = mod.str("name")?.takeIf { it.isNotBlank() } ?: return null
        val topics = categorySlugsOf(mod)
        val loaders = loadersOfMod(mod)
        val summary = mod.str("summary").orEmpty()
        val tags = (topics + loaders).distinct()
        val created = mod.str("dateCreated")
        val modified = mod.str("dateModified") ?: mod.str("dateReleased")
        val follows = mod.long("thumbsUpCount") ?: mod.long("favoriteCount") ?: 0L
        return ModrinthSearchHit(
            projectId = prefixed(id),
            projectType = projectTypeFor(itemType),
            slug = mod.str("slug"),
            author = authorOf(mod),
            title = name,
            description = summary,
            categories = tags,
            displayCategories = tags,
            versions = gameVersionsOfMod(mod),
            downloads = downloadsOf(mod),
            iconUrl = iconOf(mod),
            color = null,
            dateCreated = created,
            dateModified = modified,
            follows = follows
        )
    }

    fun toProject(mod: JsonObject): ModrinthProject? {
        val id = mod.long("id") ?: return null
        val summary = mod.str("summary").orEmpty()
        val gallery = mod.objects("screenshots").mapNotNull { shot ->
            val url = shot.str("url")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            ModrinthGalleryItem(url = url, title = shot.str("title"), description = shot.str("description"))
        }
        return ModrinthProject(
            id = prefixed(id),
            slug = mod.str("slug").orEmpty(),
            title = mod.str("name").orEmpty(),
            description = summary,
            body = null,
            categories = categorySlugsOf(mod),
            loaders = loadersOfMod(mod),
            gameVersions = gameVersionsOfMod(mod),
            downloads = downloadsOf(mod),
            iconUrl = iconOf(mod),
            gallery = gallery
        )
    }

    fun cfWidgetToProject(widget: JsonObject): ModrinthProject? {
        val id = widget.long("id") ?: return null
        val name = widget.str("name")?.takeIf { it.isNotBlank() } ?: return null
        return ModrinthProject(
            id = prefixed(id),
            slug = widget.obj("urls")?.str("website")?.substringAfterLast('/') ?: id.toString(),
            title = name,
            description = widget.str("summary").orEmpty(),
            body = null,
            categories = widget.strings("categories"),
            loaders = emptyList(),
            gameVersions = widget.strings("versions"),
            downloads = downloadsOf(widget),
            iconUrl = iconOf(widget),
            gallery = emptyList()
        )
    }

    // ------------------------------------------------------------------
    // File -> versi yang bisa diunduh
    // ------------------------------------------------------------------

    private val KNOWN_LOADER_NAMES = setOf("forge", "fabric", "quilt", "neoforge", "liteloader", "cauldron")

    private fun versionListFromFile(file: JsonObject): List<String> {
        val gv = file.strings("gameVersions")
        if (gv.isNotEmpty()) return gv
        val cfv = file.strings("versions")
        if (cfv.isNotEmpty()) return cfv
        return file.strings("game_versions")
    }

    /** Daftar gameVersions CurseForge mencampur versi Minecraft dan nama loader; yang pertama diawali angka. */
    private fun minecraftVersionsOf(file: JsonObject): List<String> =
        versionListFromFile(file).filter { it.isNotEmpty() && it[0].isDigit() }

    private fun loaderNamesOf(file: JsonObject): List<String> =
        versionListFromFile(file).map { it.lowercase() }.filter { it in KNOWN_LOADER_NAMES }

    fun cdnDownloadUrl(fileId: Long, fileName: String): String {
        val major = fileId / 1000L
        val minor = fileId % 1000L
        val encoded = URLEncoder.encode(SafePaths.fileName(fileName), "UTF-8").replace("+", "%20")
        return "https://edge.forgecdn.net/files/$major/$minor/$encoded"
    }

    /** Alamat unduhan sebuah file, otomatis memakai ForgeCDN bila field downloadUrl kosong. */
    fun downloadUrlOf(file: JsonObject, allowCdnFallback: Boolean = false): String? {
        val explicit = (file.str("downloadUrl") ?: file.str("file_url"))?.trim()?.takeIf { it.isNotEmpty() }
        if (explicit != null) return explicit
        if (allowCdnFallback) {
            val fileId = file.long("id")
            val fileName = (file.str("fileName") ?: file.str("name"))?.takeIf { it.isNotBlank() }
            if (fileId != null && fileName != null) {
                return cdnDownloadUrl(fileId, fileName)
            }
        }
        return null
    }

    private fun sha1Of(file: JsonObject): String? {
        for (h in file.objects("hashes")) {
            if (h.int("algo") == 1) {
                val v = h.str("value")?.trim()
                if (!v.isNullOrEmpty()) return v
            }
        }
        return null
    }

    private fun dependencyTypeOf(relationType: Int?): String = when (relationType) {
        3 -> "required"
        5 -> "incompatible"
        1, 6 -> "embedded"
        else -> "optional"
    }

    /** Null bila file tidak bisa diunduh lewat launcher (tanpa alamat unduhan atau tidak tersedia). */
    fun toVersion(
        file: JsonObject,
        modIdFallback: Long? = null,
        allowCdnFallback: Boolean = false
    ): ModrinthVersion? {
        val fileId = file.long("id") ?: return null
        val url = downloadUrlOf(file, allowCdnFallback = allowCdnFallback) ?: return null
        if (file.bool("isAvailable") == false) return null
        val fileName = (file.str("fileName") ?: file.str("name"))?.takeIf { it.isNotBlank() } ?: return null
        val display = file.str("displayName")?.takeIf { it.isNotBlank() } ?: fileName

        val hashes = HashMap<String, String>()
        sha1Of(file)?.let { hashes["sha1"] = it }

        val deps = file.objects("dependencies").mapNotNull { dep ->
            val depMod = dep.long("modId") ?: dep.long("mod_id") ?: return@mapNotNull null
            val relType = dep.str("type")?.lowercase()
            val depType = when (relType) {
                "required" -> "required"
                "incompatible" -> "incompatible"
                "embedded" -> "embedded"
                "optional" -> "optional"
                else -> dependencyTypeOf(dep.int("relationType"))
            }
            ModrinthDependency(
                versionId = null,
                projectId = prefixed(depMod),
                fileName = null,
                dependencyType = depType
            )
        }

        val releaseStr = (file.str("type") ?: file.str("download_type"))?.lowercase()
        val releaseType = when {
            releaseStr == "beta" || file.int("releaseType") == 2 -> "beta"
            releaseStr == "alpha" || file.int("releaseType") == 3 -> "alpha"
            else -> "release"
        }

        val modId = file.long("modId") ?: modIdFallback
        val fileLength = file.long("fileLength")
            ?: file.long("filesize")
            ?: file.str("filesize")?.toLongOrNull()
            ?: 0L

        return ModrinthVersion(
            id = fileId.toString(),
            projectId = if (modId != null) prefixed(modId) else "",
            name = display,
            versionNumber = display,
            gameVersions = minecraftVersionsOf(file),
            loaders = loaderNamesOf(file),
            versionType = releaseType,
            files = listOf(
                ModrinthVersionFile(
                    url = url,
                    filename = fileName,
                    primary = true,
                    size = fileLength,
                    hashes = if (hashes.isEmpty()) null else hashes
                )
            ),
            dependencies = deps
        )
    }

    fun cfWidgetToVersions(
        widget: JsonObject,
        modId: Long,
        gameVersion: String?,
        wantedLoader: String?
    ): List<ModrinthVersion> {
        val raw = widget.objects("files")
        val all = orderedFiles(raw).mapNotNull { file ->
            val fileId = file.long("id") ?: return@mapNotNull null
            val fileName = file.str("name")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val directCdn = cdnDownloadUrl(fileId, fileName)
            val cloned = file.deepCopy().apply { addProperty("downloadUrl", directCdn) }
            toVersion(cloned, modIdFallback = modId, allowCdnFallback = true)
        }
        return all.filter { v ->
            val matchesMc = gameVersion.isNullOrBlank() || v.gameVersions.isEmpty() || v.gameVersions.contains(gameVersion)
            val matchesLoader = loaderMatches(v.loaders, wantedLoader)
            matchesMc && matchesLoader
        }
    }

    fun cfWidgetResolvedFile(widget: JsonObject, modId: Long, fileId: Long): ResolvedFile? {
        val match = widget.objects("files").firstOrNull { it.long("id") == fileId } ?: return null
        val fileName = match.str("name")?.takeIf { it.isNotBlank() } ?: return null
        return ResolvedFile(
            fileId = fileId,
            modId = modId,
            fileName = fileName,
            url = cdnDownloadUrl(fileId, fileName),
            sha1 = null
        )
    }

    /** Tanggal unggah (teks ISO 8601, urut sama dengan urutan waktu) untuk mengurutkan file. */
    fun fileDateOf(file: JsonObject): String =
        (file.str("fileDate") ?: file.str("uploaded_at") ?: file.str("file_date")).orEmpty()

    fun releaseRankOf(file: JsonObject): Int {
        val t = (file.str("type") ?: file.str("download_type"))?.lowercase()
        return when (t) {
            "beta" -> 2
            "alpha" -> 3
            "release" -> 1
            else -> (file.int("releaseType") ?: 1).coerceIn(1, 3)
        }
    }

    /**
     * Urutan daftar versi: rilis stabil dulu, lalu beta, lalu alpha; di dalam tiap kelompok yang terbaru
     * di depan. Versi pertama itulah yang dipasang tombol "Pasang" di daftar, jadi tidak boleh alpha
     * selama masih ada rilis stabil.
     */
    fun orderedFiles(files: List<JsonObject>): List<JsonObject> =
        files.sortedWith(
            compareBy<JsonObject> { releaseRankOf(it) }
                .thenByDescending { fileDateOf(it) }
        )

    /**
     * Apakah loader file cocok dengan [wantedLoader]. File yang tidak menyebut loader apa pun (mod lama
     * atau pack yang tidak menandai loader) dianggap cocok, supaya tidak ikut tersaring habis.
     */
    fun loaderMatches(versionLoaders: List<String>, wantedLoader: String?): Boolean {
        if (wantedLoader.isNullOrBlank()) return true
        if (versionLoaders.isEmpty()) return true
        return versionLoaders.any { it.equals(wantedLoader, ignoreCase = true) }
    }

    // ------------------------------------------------------------------
    // Modpack CurseForge: manifest.json di dalam zip
    // ------------------------------------------------------------------

    class PackFile(val projectId: Long, val fileId: Long, val required: Boolean)

    class PackManifest(
        val name: String,
        val version: String,
        val minecraftVersion: String,
        /** Loader utama modpack ("forge", "fabric", "neoforge", "quilt"), atau null bila tidak disebut. */
        val loader: String?,
        /** Nama folder isi tambahan (biasanya "overrides"). */
        val overrides: String,
        val files: List<PackFile>
    )

    /** Null bila [root] bukan manifest modpack Minecraft dari CurseForge. */
    fun parseManifest(root: JsonObject): PackManifest? {
        val mc = root.obj("minecraft") ?: return null
        val filesElement = root.get("files")
        if (filesElement == null || !filesElement.isJsonArray) return null
        val type = root.str("manifestType")
        if (type != null && !type.equals("minecraftModpack", ignoreCase = true)) return null

        val loaders = mc.objects("modLoaders")
        val primary = loaders.firstOrNull { it.bool("primary") == true } ?: loaders.firstOrNull()
        val loader = primary?.str("id")?.substringBefore('-')?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }

        val files = ArrayList<PackFile>()
        for (item in filesElement.asJsonArray) {
            if (!item.isJsonObject) continue
            val f = item.asJsonObject
            val pid = f.long("projectID") ?: continue
            val fid = f.long("fileID") ?: continue
            files.add(PackFile(pid, fid, f.bool("required") != false))
        }

        return PackManifest(
            name = root.str("name").orEmpty(),
            version = root.str("version").orEmpty(),
            minecraftVersion = mc.str("version").orEmpty(),
            loader = loader,
            overrides = root.str("overrides")?.trim()?.trim('/')?.takeIf { it.isNotEmpty() } ?: "overrides",
            files = files
        )
    }

    /** Satu file modpack yang sudah dicari alamat unduhannya lewat API. */
    class ResolvedFile(
        val fileId: Long,
        val modId: Long,
        val fileName: String,
        /** Null = penulis menutup unduhan lewat API. */
        val url: String?,
        val sha1: String?
    )

    fun toResolvedFile(
        file: JsonObject,
        modIdFallback: Long = 0L,
        allowCdnFallback: Boolean = false
    ): ResolvedFile? {
        val id = file.long("id") ?: return null
        val name = (file.str("fileName") ?: file.str("name"))?.takeIf { it.isNotBlank() } ?: return null
        return ResolvedFile(
            fileId = id,
            modId = file.long("modId") ?: modIdFallback,
            fileName = name,
            url = downloadUrlOf(file, allowCdnFallback = allowCdnFallback),
            sha1 = sha1Of(file)
        )
    }

    /** Folder tujuan (relatif ke folder game) untuk file modpack menurut classId proyeknya. */
    fun folderFor(classId: Int?, fileName: String): String = when (classId) {
        12 -> "resourcepacks"
        6552 -> "shaderpacks"
        6 -> "mods"
        else -> if (fileName.endsWith(".jar", ignoreCase = true)) "mods" else "resourcepacks"
    }

    /** classId proyek dari objek mod hasil POST /v1/mods. */
    fun classIdOfMod(mod: JsonObject): Pair<Long, Int>? {
        val id = mod.long("id") ?: return null
        val cls = mod.int("classId") ?: return null
        return id to cls
    }
}

// ----------------------------------------------------------------------------
// Section: core/mods/NovaUnifiedFileImportEngine.kt
// ----------------------------------------------------------------------------

data class PendingAddonImport(
    val uri: Uri,
    val fileName: String,
    val suggestedType: String, // "mods" | "modpacks" | "worlds" | "resourcepacks" | "shaderpacks" | "horizon_mods" | "horizon_shaders" | "unknown"
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * NovaUnifiedFileImportEngine
 *
 * Mesin impor berkas terpadu yang menyatukan seluruh alur impor lokal dan "Open With":
 * - Validasi URI eksternal, MIME type, ekstensi, dan nama berkas aman ([SafePaths]).
 * - Deteksi otomatis isi arsip ZIP / .mrpack / .jar / .hmod / .hshader dan folder dunia Minecraft Java.
 * - Impor Mod, Modpack, Resource Pack, Shaderpack, Datapack, Addon Horizon (Rust), dan Dunia Minecraft Java (saves/).
 * - Ekstraksi dan salin folder dunia Java yang aman (anti Zip-Slip, batas ukuran & kedalaman, validasi NBT level.dat & struktur resmi Mojang Java Edition, nama folder unik).
 */
object NovaUnifiedFileImportEngine {

    private const val MAX_WORLD_ARCHIVE_BYTES = 1_500L * 1024L * 1024L
    private const val MAX_WORLD_EXTRACT_BYTES = 2_000L * 1024L * 1024L
    private const val MAX_ZIP_INSPECT_ENTRIES = 1000
    private const val MAX_WORLD_TREE_DEPTH = 8
    private const val MAX_WORLD_TREE_FILES = 50_000
    private const val STALE_STAGE_MAX_AGE_MS = 2L * 60L * 60L * 1000L
    private const val MOJANG_JAVA_WORLD_REQUIRED_MSG =
        "Dunia harus memiliki konfigurasi Minecraft Java Edition resmi dari Mojang (level.dat NBT + folder region/)."
    private const val ANDROID_11_SAF_RESTRICTED_MSG =
        "Akses folder dibatasi oleh sistem Android 11+. Pilih folder dunia Java di lokasi yang diizinkan."

    private val _pendingImport = MutableStateFlow<PendingAddonImport?>(null)
    val pendingImport = _pendingImport.asStateFlow()

    fun setPendingImport(import: PendingAddonImport) {
        _pendingImport.value = import
    }

    fun clearPendingImport() {
        _pendingImport.value = null
    }

    // ========================================================================
    // 1. VALIDASI SUMBER URI, NAMA BERKAS, MIME & EKSTENSI
    // ========================================================================

    fun getFileName(context: Context, uri: Uri): String {
        var name = "addon_file"
        try {
            if (uri.scheme == "content") {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIdx != -1 && cursor.moveToFirst()) {
                        name = cursor.getString(nameIdx) ?: name
                    }
                }
            } else if (uri.scheme == "file") {
                name = uri.lastPathSegment ?: "addon_file"
            }
        } catch (_: Exception) {
        }
        if (name == "addon_file") {
            val fallback = uri.lastPathSegment?.substringAfterLast('/')
            if (!fallback.isNullOrBlank()) name = fallback
        }
        return SafePaths.fileName(name)
    }

    /**
     * Apakah [uri] dari aplikasi lain boleh dibaca untuk impor. Berkas pribadi launcher sendiri
     * ditolak agar aplikasi lain tidak bisa menyisipkan/menarik data akun atau pengaturan.
     */
    fun isAcceptableSource(context: Context, uri: Uri): Boolean {
        return when (uri.scheme?.lowercase()) {
            "content" -> {
                val authority: String = uri.authority ?: ""
                val ownAuthority: String = context.packageName + ".provider"
                authority.isNotBlank() && !authority.equals(ownAuthority, ignoreCase = true)
            }
            "file" -> try {
                val path = uri.path
                if (path.isNullOrBlank()) {
                    false
                } else {
                    val canonical = File(path).canonicalPath
                    val ownExternal = context.getExternalFilesDir(null)?.parentFile?.canonicalPath
                    val ownInternal = context.filesDir.parentFile?.canonicalPath
                    val inOwnExternal = ownExternal != null && canonical.startsWith(ownExternal + File.separator)
                    val inOwnInternal = ownInternal != null && canonical.startsWith(ownInternal + File.separator)
                    !inOwnExternal && !inOwnInternal
                }
            } catch (_: Exception) {
                false
            }
            else -> false
        }
    }

    fun getMimeTypesForType(type: String, rust: Boolean = false): Array<String> {
        return when (type) {
            // Pemilih berkas Android sering menonaktifkan .jar / .hmod / .hshader bila MIME terlalu sempit;
            // gunakan */* lalu saring ketat melalui isValidExtensionForType().
            "mods", "worlds", "modpacks" -> arrayOf("*/*")
            "shaderpacks" -> if (rust) arrayOf("*/*") else arrayOf(
                "application/zip",
                "application/x-zip-compressed",
                "application/octet-stream"
            )
            else -> arrayOf(
                "application/zip",
                "application/x-zip-compressed",
                "application/octet-stream"
            )
        }
    }

    fun isValidExtensionForType(itemType: String, filename: String, rust: Boolean = false): Boolean {
        val lower = filename.lowercase()
        return when (itemType) {
            "mods" -> if (rust) lower.endsWith(".hmod") else lower.endsWith(".jar")
            "modpacks" -> lower.endsWith(".mrpack") || lower.endsWith(".zip")
            "worlds" -> lower.endsWith(".zip")
            "shaderpacks" -> lower.endsWith(".zip") || (rust && lower.endsWith(".hshader"))
            "resourcepacks", "datapacks" -> lower.endsWith(".zip")
            else -> false
        }
    }

    fun getAllowedExtensionLabel(itemType: String, rust: Boolean = false): String {
        return when (itemType) {
            "mods" -> if (rust) ".hmod" else ".jar"
            "modpacks" -> ".mrpack, .zip"
            "worlds" -> ".zip (Dunia Java)"
            "shaderpacks" -> if (rust) ".hshader, .zip" else ".zip (Shaderpack)"
            "resourcepacks" -> ".zip (Resource Pack)"
            "datapacks" -> ".zip (Datapack)"
            else -> "format yang didukung"
        }
    }

    // ========================================================================
    // 2. DETEKSI TIPE BERKAS & INSPEKSI ARSIP ZIP TERPADU
    // ========================================================================

    fun resolveTypeFromAliasOrFile(context: Context, uri: Uri, aliasClass: String?): String {
        val fileName = getFileName(context, uri).lowercase()
        when {
            fileName.endsWith(".hmod") -> return "horizon_mods"
            fileName.endsWith(".hshader") -> return "horizon_shaders"
            fileName.endsWith(".jar") -> return "mods"
            fileName.endsWith(".mrpack") -> return "modpacks"
        }

        if (!aliasClass.isNullOrBlank()) {
            when {
                aliasClass.endsWith("ImportHorizonModActivity") -> return "horizon_mods"
                aliasClass.endsWith("ImportHorizonShaderActivity") -> return "horizon_shaders"
                aliasClass.endsWith("ImportWorldActivity") -> return "worlds"
                aliasClass.endsWith("ImportModActivity") -> return "mods"
                aliasClass.endsWith("ImportModpackActivity") -> return "modpacks"
                aliasClass.endsWith("ImportResourcepackActivity") -> return "resourcepacks"
                aliasClass.endsWith("ImportShaderpackActivity") -> return "shaderpacks"
            }
        }

        return when {
            fileName.endsWith(".zip") -> inspectZipFromUri(context, uri)
            else -> "unknown"
        }
    }

    fun inspectZipType(zipFile: File): String {
        return try {
            ZipFile(zipFile).use { zip ->
                val entries = zip.entries()
                inspectZipEntryNames(sequence {
                    var count = 0
                    while (entries.hasMoreElements() && count < MAX_ZIP_INSPECT_ENTRIES) {
                        count++
                        yield(entries.nextElement().name)
                    }
                })
            }
        } catch (e: Exception) {
            LauncherLog.e("NovaUnifiedImport", "Gagal inspect ZIP: ${e.message}", e)
            "unknown"
        }
    }

    private fun inspectZipFromUri(context: Context, uri: Uri): String {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                ZipInputStream(stream).use { zis ->
                    val names = ArrayList<String>(128)
                    var entry = zis.nextEntry
                    var count = 0
                    while (entry != null && count < MAX_ZIP_INSPECT_ENTRIES) {
                        count++
                        names.add(entry.name)
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                    val detected = inspectZipEntryNames(names.asSequence())
                    if (detected == "unknown") "modpacks" else detected
                }
            } ?: "modpacks"
        } catch (_: Exception) {
            "modpacks"
        }
    }

    private fun inspectZipEntryNames(names: Sequence<String>): String {
        var hasHorizonMod = false
        var hasHorizonShader = false
        var hasLevelDat = false
        var hasJavaRegion = false
        var hasJavaSaveMarker = false
        var hasShaders = false
        var hasPackMcmeta = false
        var hasAssets = false
        var hasData = false
        var hasModpackIndex = false
        var hasCurseforgeManifest = false
        var hasModsFolder = false
        var hasJarFiles = false

        for (raw in names) {
            val name = raw.replace('\\', '/').trimStart('/').lowercase()
            if (name == "horizon-mod.toml" || name.endsWith("/horizon-mod.toml")) hasHorizonMod = true
            if (name == "horizon-shaders.toml" || name.endsWith("/horizon-shaders.toml")) hasHorizonShader = true
            if (name == "level.dat" || isShallowLevelDat(name)) hasLevelDat = true
            if (name.startsWith("region/") || name.contains("/region/") ||
                name.startsWith("dim-1/") || name.contains("/dim-1/") ||
                name.startsWith("dim1/") || name.contains("/dim1/")
            ) {
                hasJavaRegion = true
            }
            if (name == "level.dat_old" || name.endsWith("/level.dat_old") ||
                name == "session.lock" || name.endsWith("/session.lock") ||
                name.startsWith("playerdata/") || name.contains("/playerdata/") ||
                name.startsWith("datapacks/") || name.contains("/datapacks/") ||
                name.startsWith("advancements/") || name.contains("/advancements/") ||
                name.startsWith("stats/") || name.contains("/stats/")
            ) {
                hasJavaSaveMarker = true
            }
            if (name.startsWith("shaders/") || name.contains("/shaders/")) hasShaders = true
            if (name == "pack.mcmeta" || name.endsWith("/pack.mcmeta")) hasPackMcmeta = true
            if (name.startsWith("assets/") || name.contains("/assets/")) hasAssets = true
            if (name.startsWith("data/") || name.contains("/data/")) hasData = true
            if (name.endsWith("modrinth.index.json")) hasModpackIndex = true
            if (name.endsWith("manifest.json")) hasCurseforgeManifest = true
            if (name.startsWith("mods/") || name.contains("/mods/")) hasModsFolder = true
            if (name.endsWith(".jar")) hasJarFiles = true
        }

        val isJavaWorldZip = hasLevelDat && (hasJavaRegion || hasJavaSaveMarker)

        return when {
            hasHorizonMod -> "horizon_mods"
            hasHorizonShader -> "horizon_shaders"
            isJavaWorldZip -> "worlds"
            hasModpackIndex || hasCurseforgeManifest -> "modpacks"
            hasShaders -> "shaderpacks"
            hasPackMcmeta && hasAssets -> "resourcepacks"
            hasPackMcmeta && hasData -> "datapacks"
            hasModsFolder || hasJarFiles -> "modpacks"
            hasPackMcmeta -> "resourcepacks"
            else -> "unknown"
        }
    }

    /** True untuk `level.dat` di akar, 1 folder dalam (`MyWorld/level.dat`), atau `saves/MyWorld/level.dat`. */
    private fun isShallowLevelDat(lowerNormalized: String): Boolean {
        if (!lowerNormalized.endsWith("/level.dat")) return false
        val slashCount = lowerNormalized.count { it == '/' }
        return slashCount <= 2 || lowerNormalized.contains("/saves/")
    }

    // ========================================================================
    // 3. IMPOR BERKAS LOKAL TERPADU (JAVA & HORIZON RUST + DUNIA)
    // ========================================================================

    suspend fun importLocalFile(
        context: Context,
        instance: Instance,
        itemType: String,
        uri: Uri,
        worldName: String? = null,
        onProgress: ((statusText: String, current: Int, total: Int) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val fileName = getFileName(context, uri)
            val normalizedType = when (itemType) {
                "horizon_mods" -> HorizonContent.TYPE_MODS
                "horizon_shaders" -> HorizonContent.TYPE_SHADERS
                else -> itemType
            }

            // Mesin Rust (Horizon)
            if (HorizonEngine.isEngine(instance.loader)) {
                if (normalizedType == "worlds") {
                    return@withContext Result.failure(
                        IllegalArgumentException("Dunia Java hanya dapat dipasang ke instance Java.")
                    )
                }
                return@withContext importEngineFile(context, uri, normalizedType, fileName)
            }

            // Instance Java: validasi ekstensi ketat
            val lowerName = fileName.lowercase()
            if (lowerName.endsWith(".hmod") || lowerName.endsWith(".hshader")) {
                return@withContext Result.failure(
                    IllegalArgumentException("Berkas .hmod / .hshader hanya untuk instance mesin Rust (Horizon).")
                )
            }
            if (!isValidExtensionForType(normalizedType, fileName, rust = false)) {
                val allowed = getAllowedExtensionLabel(normalizedType, rust = false)
                return@withContext Result.failure(
                    IllegalArgumentException("Format berkas tidak valid. Format yang didukung: $allowed")
                )
            }

            // Dunia Minecraft Java (.zip) -> ekstrak aman ke <gameDir>/saves/<NamaDunia>/
            if (normalizedType == "worlds") {
                val stageDir = File(context.cacheDir, "world_import_stage")
                if (!stageDir.exists()) stageDir.mkdirs()
                val stagedZip = File(stageDir, "import_${System.currentTimeMillis()}_$fileName")
                try {
                    onProgress?.invoke("Menyalin arsip dunia $fileName...", 0, 0)
                    copyUriBounded(context, uri, stagedZip, MAX_WORLD_ARCHIVE_BYTES)
                    return@withContext installWorldArchive(
                        context = context,
                        instance = instance,
                        archiveFile = stagedZip,
                        suggestedName = File(fileName).nameWithoutExtension,
                        onProgress = onProgress
                    )
                } finally {
                    try {
                        stagedZip.delete()
                    } catch (_: Exception) {
                    }
                }
            }

            // Mod, Modpack, Resource Pack, Shaderpack, Datapack
            val targetDir = NuxModManager.getTargetDir(context, instance, normalizedType, worldName)
            val destFile = File(targetDir, fileName)
            val partFile = File(targetDir, "$fileName.part")

            onProgress?.invoke("Menyalin file $fileName...", 0, 0)
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(partFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: return@withContext Result.failure(IOException("Gagal membaca file sumber"))

                if (destFile.exists()) destFile.delete()
                if (!partFile.renameTo(destFile)) {
                    partFile.copyTo(destFile, overwrite = true)
                    partFile.delete()
                }
            } catch (e: Exception) {
                partFile.delete()
                throw e
            }

            if (normalizedType == "modpacks" && (lowerName.endsWith(".mrpack") || lowerName.endsWith(".zip"))) {
                onProgress?.invoke("Membaca daftar modpack...", 0, 0)
                NuxModManager.processMrpackFile(instance, destFile, context) { statusText, cur, tot ->
                    onProgress?.invoke(statusText, cur, tot)
                }
            }

            Result.success(fileName)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun importEngineFile(
        context: Context,
        uri: Uri,
        itemType: String,
        fileName: String
    ): Result<String> {
        var staged: File? = null
        return try {
            val lower = fileName.lowercase()
            if (itemType == HorizonContent.TYPE_MODS && !lower.endsWith(HorizonMods.EXTENSION)) {
                throw IOException("Mod Horizon eksternal harus berformat .hmod.")
            }
            if (itemType == HorizonContent.TYPE_SHADERS && !lower.endsWith(".zip") && !lower.endsWith(HorizonContent.NATIVE_SHADER_EXTENSION)) {
                throw IOException("Shader Horizon harus berformat .hshader atau .zip.")
            }
            if (itemType == HorizonContent.TYPE_RESOURCES && !lower.endsWith(".zip")) {
                throw IOException("Resource pack Horizon harus berformat .zip.")
            }
            if (!HorizonContent.supports(itemType)) throw IOException(HorizonContent.MSG_TYPE)
            val stageDir = File(context.cacheDir, "horizon-dl")
            if (!stageDir.isDirectory && !stageDir.mkdirs()) throw IOException(HorizonContent.MSG_SAVE)
            val target = File(stageDir, fileName)
            staged = target
            val input = context.contentResolver.openInputStream(uri)
                ?: throw IOException("Gagal membuka berkas yang dipilih")
            input.use { HorizonContent.copyBounded(it, target) }
            HorizonContent.install(target, HorizonEngine.engineHome(context), itemType, fileName)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            staged?.delete()
        }
    }

    // ========================================================================
    // 4. PEMASANG DUNIA MINECRAFT JAVA (ARSIP .ZIP & FOLDER DUNIA JAVA)
    // ========================================================================

    /**
     * Mengekstrak arsip dunia Minecraft Java (.zip) ke `<gameDir>/saves/<NamaDuniaUnik>/` secara atomik.
     * Memastikan arsip memiliki konfigurasi dunia Minecraft Java Edition resmi dari Mojang.
     */
    fun installWorldArchive(
        context: Context,
        instance: Instance,
        archiveFile: File,
        suggestedName: String,
        onProgress: ((statusText: String, current: Int, total: Int) -> Unit)? = null
    ): Result<String> {
        if (HorizonEngine.isEngine(instance.loader)) {
            return Result.failure(IllegalArgumentException("Dunia Java hanya dapat dipasang ke instance Java."))
        }
        val gameDir = InstanceManager.getInstanceGameDir(context, instance)
        val savesDir = File(gameDir, "saves")
        if (!savesDir.exists() && !savesDir.mkdirs()) {
            return Result.failure(IOException("Gagal membuat folder saves."))
        }
        cleanupStaleWorldStages(savesDir)

        val stagingDir = File(savesDir, ".nova_world_stage_${System.currentTimeMillis()}")
        return try {
            if (!stagingDir.mkdirs()) {
                return Result.failure(IOException("Gagal membuat folder sementara untuk dunia."))
            }

            onProgress?.invoke("Memeriksa & mengekstrak dunia Java...", 0, 0)
            ZipFile(archiveFile).use { zip ->
                val totalEntries = zip.size().coerceAtLeast(1)
                var processed = 0
                var extractedBytes = 0L
                val entries = zip.entries()
                val buffer = ByteArray(64 * 1024)

                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    processed++
                    if (processed % 25 == 0 || processed == totalEntries) {
                        onProgress?.invoke(
                            "Mengekstrak dunia ($processed/$totalEntries)...",
                            processed,
                            totalEntries
                        )
                    }

                    val rel = entry.name.replace('\\', '/').trimStart('/')
                    if (rel.isBlank()) continue
                    val outFile = SafePaths.resolveInside(stagingDir, rel) ?: continue

                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        zip.getInputStream(entry).use { input ->
                            FileOutputStream(outFile).use { output ->
                                while (true) {
                                    val read = input.read(buffer)
                                    if (read < 0) break
                                    extractedBytes += read
                                    if (extractedBytes > MAX_WORLD_EXTRACT_BYTES) {
                                        throw IOException("Ukuran dunia melebihi batas aman (2 GB).")
                                    }
                                    output.write(buffer, 0, read)
                                }
                            }
                        }
                    }
                }
            }

            val worldRoot = locateWorldRoot(stagingDir)
            if (worldRoot == null) {
                return Result.failure(IOException(MOJANG_JAVA_WORLD_REQUIRED_MSG))
            }

            finalizeJavaWorldDir(
                worldRoot = worldRoot,
                stagingDir = stagingDir,
                savesDir = savesDir,
                suggestedName = suggestedName,
                onProgress = onProgress
            )
        } catch (e: Exception) {
            LauncherLog.e("NovaUnifiedImport", "Gagal pasang arsip dunia: ${e.message}", e)
            Result.failure(e)
        } finally {
            try {
                if (stagingDir.exists()) stagingDir.deleteRecursively()
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Mengimpor langsung **folder dunia Minecraft Java** yang dipilih via SAF (`OpenDocumentTree`)
     * ke `<gameDir>/saves/<NamaDuniaUnik>/` secara atomik.
     * Wajib memiliki konfigurasi dunia Minecraft Java Edition resmi dari Mojang.
     */
    suspend fun importJavaWorldFolder(
        context: Context,
        instance: Instance,
        treeUri: Uri,
        onProgress: ((statusText: String, current: Int, total: Int) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        if (HorizonEngine.isEngine(instance.loader)) {
            return@withContext Result.failure(IllegalArgumentException("Dunia Java hanya dapat dipasang ke instance Java."))
        }
        val gameDir = InstanceManager.getInstanceGameDir(context, instance)
        val savesDir = File(gameDir, "saves")
        if (!savesDir.exists() && !savesDir.mkdirs()) {
            return@withContext Result.failure(IOException("Gagal membuat folder saves."))
        }
        cleanupStaleWorldStages(savesDir)

        val stagingDir = File(savesDir, ".nova_world_stage_${System.currentTimeMillis()}")
        try {
            val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val rootMeta = queryDocMeta(context, treeUri, treeDocId)
            val rootName = SafePaths.fileName(rootMeta?.first ?: treeDocId.substringAfterLast('/').ifBlank { "Java World" })

            onProgress?.invoke("Memeriksa folder dunia Java...", 0, 0)
            val resolvedTarget = resolveJavaWorldTreeTarget(context, treeUri, treeDocId, rootName)
                ?: return@withContext Result.failure(IOException(MOJANG_JAVA_WORLD_REQUIRED_MSG))

            if (!stagingDir.mkdirs()) {
                return@withContext Result.failure(IOException("Gagal membuat folder sementara untuk dunia."))
            }

            val targetFolderName = SafePaths.fileName(resolvedTarget.second)
            val stagedWorldDir = File(stagingDir, targetFolderName)
            if (!stagedWorldDir.mkdirs()) {
                return@withContext Result.failure(IOException("Gagal menyiapkan folder sementara dunia."))
            }

            var copiedFiles = 0
            var totalBytes = 0L
            val buffer = ByteArray(64 * 1024)

            fun copyTreeNode(docId: String, destDir: File, depth: Int) {
                if (depth > MAX_WORLD_TREE_DEPTH) {
                    throw IOException("Struktur folder dunia terlalu dalam.")
                }
                for (child in listDocChildren(context, treeUri, docId)) {
                    val safeName = SafePaths.fileName(child.name)
                    if (safeName == "." || safeName == "..") continue
                    val outTarget = SafePaths.resolveInside(destDir, safeName) ?: continue

                    if (child.isDirectory) {
                        outTarget.mkdirs()
                        copyTreeNode(child.docId, outTarget, depth + 1)
                    } else {
                        copiedFiles++
                        if (copiedFiles > MAX_WORLD_TREE_FILES) {
                            throw IOException("Jumlah berkas di folder terlalu banyak (maks. $MAX_WORLD_TREE_FILES).")
                        }
                        if (copiedFiles % 15 == 1) {
                            onProgress?.invoke("Menyalin dunia Java ($copiedFiles file)...", 0, 0)
                        }
                        outTarget.parentFile?.mkdirs()
                        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, child.docId)
                        val input = context.contentResolver.openInputStream(docUri)
                            ?: throw IOException("Gagal membaca berkas ${child.name}.")
                        input.use { stream ->
                            FileOutputStream(outTarget).use { output ->
                                while (true) {
                                    val read = stream.read(buffer)
                                    if (read < 0) break
                                    totalBytes += read
                                    if (totalBytes > MAX_WORLD_EXTRACT_BYTES) {
                                        throw IOException("Ukuran dunia melebihi batas aman (2 GB).")
                                    }
                                    output.write(buffer, 0, read)
                                }
                            }
                        }
                    }
                }
            }

            copyTreeNode(resolvedTarget.first, stagedWorldDir, 0)

            finalizeJavaWorldDir(
                worldRoot = stagedWorldDir,
                stagingDir = stagingDir,
                savesDir = savesDir,
                suggestedName = targetFolderName,
                onProgress = onProgress
            )
        } catch (e: SecurityException) {
            LauncherLog.e("NovaUnifiedImport", "Akses folder dibatasi sistem Android: ${e.message}", e)
            Result.failure(IOException(ANDROID_11_SAF_RESTRICTED_MSG, e))
        } catch (e: Exception) {
            LauncherLog.e("NovaUnifiedImport", "Gagal impor folder dunia Java: ${e.message}", e)
            Result.failure(e)
        } finally {
            try {
                if (stagingDir.exists()) stagingDir.deleteRecursively()
            } catch (_: Exception) {
            }
        }
    }

    private data class DocChildNode(
        val docId: String,
        val name: String,
        val isDirectory: Boolean
    )

    private fun queryDocMeta(context: Context, treeUri: Uri, docId: String): Pair<String, Boolean>? {
        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )
        return try {
            context.contentResolver.query(docUri, projection, null, null, null)?.use { c ->
                if (!c.moveToFirst()) return null
                val name = c.getString(0) ?: ""
                val mime = c.getString(1) ?: ""
                name to (mime == DocumentsContract.Document.MIME_TYPE_DIR)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun listDocChildren(context: Context, treeUri: Uri, parentDocId: String): List<DocChildNode> {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )
        val out = ArrayList<DocChildNode>()
        context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getString(0) ?: continue
                val name = cursor.getString(1) ?: continue
                val mime = cursor.getString(2) ?: ""
                out.add(DocChildNode(id, name, mime == DocumentsContract.Document.MIME_TYPE_DIR))
            }
        }
        return out
    }

    /**
     * Memeriksa depth 0 & depth 1 dari SAF tree sebelum menyalin penuh:
     * - Menerima folder dunia Java langsung (punya `level.dat` NBT Mojang + struktur save Java)
     * - Menerima folder pembungkus 1 lapis (`<Folder>/<World>/` atau `<Folder>/saves/<World>/`)
     */
    private fun resolveJavaWorldTreeTarget(
        context: Context,
        treeUri: Uri,
        rootDocId: String,
        rootName: String
    ): Pair<String, String>? {
        val rootChildren = listDocChildren(context, treeUri, rootDocId)
        val rootLevelDat = rootChildren.firstOrNull { !it.isDirectory && it.name.equals("level.dat", ignoreCase = true) }
        if (rootLevelDat != null) {
            if (!isValidMojangLevelDatDoc(context, treeUri, rootLevelDat.docId) ||
                !hasMojangJavaChildrenInTree(rootChildren)
            ) {
                throw IOException(MOJANG_JAVA_WORLD_REQUIRED_MSG)
            }
            return rootDocId to rootName
        }

        val dirs = rootChildren.filter { it.isDirectory && !it.name.startsWith(".") }
        val savesChild = dirs.firstOrNull { it.name.equals("saves", ignoreCase = true) }
        val candidates = if (savesChild != null) {
            listDocChildren(context, treeUri, savesChild.docId).filter { it.isDirectory && !it.name.startsWith(".") }
        } else {
            dirs
        }
        var foundInvalidWorldCandidate = false
        for (dir in candidates) {
            val subChildren = listDocChildren(context, treeUri, dir.docId)
            val subLevelDat = subChildren.firstOrNull { !it.isDirectory && it.name.equals("level.dat", ignoreCase = true) }
            if (subLevelDat != null) {
                if (isValidMojangLevelDatDoc(context, treeUri, subLevelDat.docId) &&
                    hasMojangJavaChildrenInTree(subChildren)
                ) {
                    return dir.docId to dir.name
                }
                foundInvalidWorldCandidate = true
            }
        }
        if (foundInvalidWorldCandidate) {
            throw IOException(MOJANG_JAVA_WORLD_REQUIRED_MSG)
        }
        return null
    }

    private fun isValidMojangLevelDatDoc(context: Context, treeUri: Uri, docId: String): Boolean {
        return try {
            val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
            context.contentResolver.openInputStream(docUri)?.use { stream ->
                isValidMojangJavaLevelDat(stream)
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    private fun hasMojangJavaChildrenInTree(children: List<DocChildNode>): Boolean {
        val hasRegionOrDim = children.any {
            it.isDirectory && (
                it.name.equals("region", ignoreCase = true) ||
                    it.name.equals("DIM-1", ignoreCase = true) ||
                    it.name.equals("DIM1", ignoreCase = true)
                )
        }
        if (hasRegionOrDim) return true
        val hasDataDir = children.any { it.isDirectory && it.name.equals("data", ignoreCase = true) }
        val hasJavaCompanion = children.any {
            (it.isDirectory && (
                it.name.equals("playerdata", ignoreCase = true) ||
                    it.name.equals("datapacks", ignoreCase = true)
                )) || (!it.isDirectory && (
                it.name.equals("level.dat_old", ignoreCase = true) ||
                    it.name.equals("session.lock", ignoreCase = true)
                ))
        }
        return hasDataDir && hasJavaCompanion
    }

    private fun finalizeJavaWorldDir(
        worldRoot: File,
        stagingDir: File,
        savesDir: File,
        suggestedName: String,
        onProgress: ((statusText: String, current: Int, total: Int) -> Unit)? = null
    ): Result<String> {
        if (!hasMojangJavaSaveConfig(worldRoot)) {
            return Result.failure(IOException(MOJANG_JAVA_WORLD_REQUIRED_MSG))
        }

        val rawWorldName = when {
            worldRoot != stagingDir && worldRoot.name.isNotBlank() &&
                !worldRoot.name.equals("world", ignoreCase = true) -> worldRoot.name
            suggestedName.isNotBlank() -> suggestedName
            else -> worldRoot.name.ifBlank { "Minecraft Java World" }
        }
        val cleanWorldName = SafePaths.fileName(rawWorldName)
        val finalDest = uniqueWorldDir(savesDir, cleanWorldName)

        onProgress?.invoke("Menyimpan dunia ${finalDest.name}...", 1, 1)
        moveOrCopyDir(worldRoot, finalDest)
        return Result.success(finalDest.name)
    }

    /**
     * Memastikan berkas `level.dat` adalah NBT resmi Minecraft Java Edition dari Mojang
     * (GZip `0x1F 0x8B` atau NBT root `0x0A` yang langsung berisi compound tag `"Data"`).
     */
    private fun isValidMojangJavaLevelDat(file: File): Boolean {
        if (!file.isFile || file.length() < 10L) return false
        return try {
            FileInputStream(file).use { isValidMojangJavaLevelDat(it) }
        } catch (_: Exception) {
            false
        }
    }

    private fun isValidMojangJavaLevelDat(rawStream: InputStream): Boolean {
        return try {
            val bis = BufferedInputStream(rawStream, 8192)
            bis.mark(4)
            val b0 = bis.read()
            val b1 = bis.read()
            bis.reset()
            if (b0 < 0 || b1 < 0) return false

            val nbtStream: InputStream = when {
                b0 == 0x1F && b1 == 0x8B -> GZIPInputStream(bis)
                b0 == 0x0A -> bis
                else -> return false
            }

            val header = ByteArray(1024)
            var total = 0
            while (total < header.size) {
                val read = nbtStream.read(header, total, header.size - total)
                if (read <= 0) break
                total += read
            }
            if (total < 10 || header[0] != 0x0A.toByte()) return false

            val dataCompoundMarker = byteArrayOf(
                0x0A, 0x00, 0x04,
                'D'.code.toByte(), 'a'.code.toByte(), 't'.code.toByte(), 'a'.code.toByte()
            )
            indexOfBytes(header, total, dataCompoundMarker) >= 0
        } catch (_: Exception) {
            false
        }
    }

    private fun indexOfBytes(haystack: ByteArray, length: Int, needle: ByteArray): Int {
        if (needle.isEmpty() || length < needle.size) return -1
        val limit = length - needle.size
        outer@ for (i in 0..limit) {
            for (j in needle.indices) {
                if (haystack[i + j] != needle[j]) continue@outer
            }
            return i
        }
        return -1
    }

    private fun hasMojangJavaSaveConfig(dir: File): Boolean {
        val levelDat = File(dir, "level.dat")
        if (!isValidMojangJavaLevelDat(levelDat)) return false

        val hasRegionOrDim = File(dir, "region").isDirectory ||
            File(dir, "DIM-1").isDirectory ||
            File(dir, "DIM1").isDirectory
        if (hasRegionOrDim) return true

        val hasDataDir = File(dir, "data").isDirectory
        val hasJavaCompanion = File(dir, "playerdata").isDirectory ||
            File(dir, "datapacks").isDirectory ||
            File(dir, "level.dat_old").isFile ||
            File(dir, "session.lock").isFile
        return hasDataDir && hasJavaCompanion
    }

    private fun cleanupStaleWorldStages(savesDir: File) {
        val cutoff = System.currentTimeMillis() - STALE_STAGE_MAX_AGE_MS
        val children = savesDir.listFiles() ?: return
        for (child in children) {
            if (child.isDirectory && child.name.startsWith(".nova_world_stage_") && child.lastModified() < cutoff) {
                try {
                    child.deleteRecursively()
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun locateWorldRoot(stagingDir: File): File? {
        if (hasMojangJavaSaveConfig(stagingDir)) return stagingDir

        // Cek overrides/saves/<World>/level.dat atau saves/<World>/level.dat
        val candidateParents = listOf(
            File(stagingDir, "overrides/saves"),
            File(stagingDir, "client-overrides/saves"),
            File(stagingDir, "saves"),
            stagingDir
        )
        for (parent in candidateParents) {
            if (!parent.isDirectory) continue
            val children = parent.listFiles()?.filter { it.isDirectory && !it.name.startsWith(".") } ?: continue
            val directMatch = children.firstOrNull { hasMojangJavaSaveConfig(it) }
            if (directMatch != null) return directMatch
            for (child in children) {
                val grandChildren = child.listFiles()?.filter { it.isDirectory && !it.name.startsWith(".") } ?: continue
                val nestedMatch = grandChildren.firstOrNull { hasMojangJavaSaveConfig(it) }
                if (nestedMatch != null) return nestedMatch
            }
        }
        return null
    }

    const val DESIGNATED_MOJANG_WORLDS_PATH = "games/com.mojang/minecraftWorlds"

    /**
     * Mengembalikan folder tujuan `/games/com.mojang/minecraftWorlds` di bawah [baseDir]
     * dan membuatnya bila belum ada.
     */
    fun getDesignatedMojangWorldsDir(baseDir: File): File {
        val normalized = DESIGNATED_MOJANG_WORLDS_PATH.trimStart('/')
        val target = File(baseDir, normalized)
        if (!target.exists()) target.mkdirs()
        return target
    }

    /**
     * Memindahkan struktur folder dunia hasil impor atau mengekstrak arsip `.zip` ke jalur tujuan
     * `<baseDir>/games/com.mojang/minecraftWorlds/<NamaUnik>/`, sambil memastikan nama folder
     * selalu unik agar tidak menimpa dunia yang sudah ada.
     */
    suspend fun moveImportedWorldToDesignatedPath(
        source: File,
        baseDir: File,
        preferredName: String? = null,
        onProgress: ((statusText: String, current: Int, total: Int) -> Unit)? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        if (!source.exists()) {
            return@withContext Result.failure(IOException("Sumber dunia yang diimpor tidak ditemukan."))
        }
        val targetWorldsDir = getDesignatedMojangWorldsDir(baseDir)
        if (!targetWorldsDir.isDirectory && !targetWorldsDir.mkdirs()) {
            return@withContext Result.failure(IOException("Gagal menyiapkan folder minecraftWorlds."))
        }
        cleanupStaleWorldStages(targetWorldsDir)

        val stagingDir = File(targetWorldsDir, ".nova_world_stage_${System.currentTimeMillis()}")
        try {
            val worldRoot: File = if (source.isFile && source.name.endsWith(".zip", ignoreCase = true)) {
                if (!stagingDir.mkdirs()) {
                    return@withContext Result.failure(IOException("Gagal membuat folder sementara untuk dunia."))
                }
                onProgress?.invoke("Mengekstrak arsip dunia ke minecraftWorlds...", 0, 0)
                ZipFile(source).use { zip ->
                    val totalEntries = zip.size().coerceAtLeast(1)
                    var processed = 0
                    var extractedBytes = 0L
                    val entries = zip.entries()
                    val buffer = ByteArray(64 * 1024)
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        processed++
                        if (processed % 25 == 0 || processed == totalEntries) {
                            onProgress?.invoke("Mengekstrak dunia ($processed/$totalEntries)...", processed, totalEntries)
                        }
                        val rel = entry.name.replace('\\', '/').trimStart('/')
                        if (rel.isBlank()) continue
                        val outFile = SafePaths.resolveInside(stagingDir, rel) ?: continue
                        if (entry.isDirectory) {
                            outFile.mkdirs()
                        } else {
                            outFile.parentFile?.mkdirs()
                            zip.getInputStream(entry).use { input ->
                                FileOutputStream(outFile).use { output ->
                                    while (true) {
                                        val read = input.read(buffer)
                                        if (read < 0) break
                                        extractedBytes += read
                                        if (extractedBytes > MAX_WORLD_EXTRACT_BYTES) {
                                            throw IOException("Ukuran dunia melebihi batas aman (2 GB).")
                                        }
                                        output.write(buffer, 0, read)
                                    }
                                }
                            }
                        }
                    }
                }
                locateWorldRoot(stagingDir)
                    ?: return@withContext Result.failure(IOException(MOJANG_JAVA_WORLD_REQUIRED_MSG))
            } else if (source.isDirectory) {
                val detected = locateWorldRoot(source) ?: if (hasMojangJavaSaveConfig(source)) source else null
                detected ?: return@withContext Result.failure(IOException(MOJANG_JAVA_WORLD_REQUIRED_MSG))
            } else {
                return@withContext Result.failure(IOException("Sumber harus berupa folder dunia atau arsip .zip."))
            }

            val rawName = preferredName?.takeIf { it.isNotBlank() }
                ?: if (worldRoot != stagingDir && worldRoot.name.isNotBlank() && !worldRoot.name.equals("world", ignoreCase = true)) {
                    worldRoot.name
                } else {
                    source.nameWithoutExtension.ifBlank { "Minecraft Java World" }
                }
            val cleanName = SafePaths.fileName(rawName)
            val finalDest = uniqueWorldDir(targetWorldsDir, cleanName)
            onProgress?.invoke("Memindahkan dunia ${finalDest.name}...", 1, 1)
            moveOrCopyDir(worldRoot, finalDest)
            Result.success(finalDest)
        } catch (e: Exception) {
            LauncherLog.e("NovaUnifiedImport", "Gagal memindahkan dunia ke minecraftWorlds: ${e.message}", e)
            Result.failure(e)
        } finally {
            try {
                if (stagingDir.exists()) stagingDir.deleteRecursively()
            } catch (_: Exception) {
            }
        }
    }

    private fun uniqueWorldDir(savesDir: File, baseName: String): File {
        val first = File(savesDir, baseName)
        if (!first.exists()) return first
        for (idx in 2..999) {
            val candidate = File(savesDir, "$baseName ($idx)")
            if (!candidate.exists()) return candidate
        }
        return File(savesDir, "${baseName}_${System.currentTimeMillis()}")
    }

    private fun moveOrCopyDir(src: File, dest: File) {
        if (src.renameTo(dest)) return
        if (!src.copyRecursively(dest, overwrite = false)) {
            throw IOException("Gagal memindahkan folder dunia ke saves/.")
        }
        src.deleteRecursively()
    }

    private fun copyUriBounded(context: Context, uri: Uri, target: File, maxBytes: Long) {
        var failed = true
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: throw IOException("Gagal membaca berkas sumber.")
            input.use { stream ->
                FileOutputStream(target).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var total = 0L
                    while (true) {
                        val read = stream.read(buffer)
                        if (read < 0) break
                        total += read
                        if (total > maxBytes) {
                            throw IOException("Ukuran berkas melebihi batas aman.")
                        }
                        output.write(buffer, 0, read)
                    }
                }
            }
            failed = false
        } finally {
            if (failed) {
                try {
                    target.delete()
                } catch (_: Exception) {
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------
// Section: core/mods/NuxModManager.kt
// ----------------------------------------------------------------------------

data class WorldInfo(
    val folderName: String,
    val displayName: String,
    val lastPlayed: Long = 0L,
    val iconFile: File? = null
)

/** Modpack tidak cocok dengan instance atau loader yang didukung; pesannya aman ditampilkan ke pengguna. */
private class ModpackIncompatibleException(message: String) : Exception(message)

/** Satu berkas di dalam modrinth.index.json yang sudah lolos pemeriksaan path dan alamat. */
private data class MrpackFileEntry(
    val url: String,
    val relPath: String,
    val hashes: Map<String, String>
)

object NuxModManager {

    private val gson = Gson()
    // Dipakai juga oleh CurseForgeApi (satu klien, satu kolam sambungan)
    internal val httpClient = OkHttpClient.Builder()
        .dns(com.aistudio.novacraft.launcher.core.utils.NuxDns)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        // Jangan ikuti pengalihan dari HTTPS ke HTTP polos (isi unduhan bisa diganti di jalan)
        .followSslRedirects(false)
        .build()

    internal const val USER_AGENT = "NovaCraft/2.3 (Android)"

    /** Batas total isi yang boleh diekstrak dari satu modpack (cegah arsip "bom" yang memenuhi penyimpanan). */
    private const val MAX_MODPACK_EXTRACT_BYTES = 1_500L * 1024L * 1024L
    private const val MODRINTH_API_BASE = "https://api.modrinth.com/v2"

    /** Sumber unduhan di layar Mod: 2 penyedia online (Modrinth & CurseForge) + Horizon khusus mesin Rust. */
    const val SOURCE_ALL = "all"
    const val SOURCE_MODRINTH = "modrinth"
    const val SOURCE_CURSEFORGE = "curseforge"
    const val SOURCE_HORIZON = "horizon"

    /** Loader yang didukung launcher saat ini. Ganti di sini bila nanti loader lain ditambahkan. */
    private const val SUPPORTED_LOADER = "fabric"
    private val SEARCH_SORT_INDEXES = setOf("relevance", "downloads", "follows", "newest", "updated")
    private val VI_COMBINING_MARKS = Regex("\\p{Mn}+")
    private val WHITESPACE_COLLAPSE = Regex("\\s+")
    private val VI_TO_EN_PHRASES = listOf(
        "tiếng việt" to "vietnamese",
        "tieng viet" to "vietnamese",
        "việt hóa" to "vietnamese",
        "viet hoa" to "vietnamese",
        "việt nam" to "vietnam",
        "viet nam" to "vietnam",
        "giảm lag" to "optimization fps",
        "giam lag" to "optimization fps",
        "tăng fps" to "fps boost optimization",
        "tang fps" to "fps boost optimization",
        "tối ưu" to "optimization",
        "toi uu" to "optimization",
        "vũ khí" to "weapon",
        "vu khi" to "weapon",
        "súng" to "gun",
        "sung" to "gun",
        "kiếm" to "sword",
        "kiem" to "sword",
        "nội thất" to "furniture",
        "noi that" to "furniture",
        "trang trí" to "decoration",
        "trang tri" to "decoration",
        "phép thuật" to "magic",
        "phep thuat" to "magic",
        "công nghệ" to "technology",
        "cong nghe" to "technology",
        "hầm ngục" to "dungeon",
        "ham nguc" to "dungeon",
        "ba lô" to "backpack",
        "ba lo" to "backpack",
        "bản đồ" to "map minimap",
        "ban do" to "map minimap",
        "quái vật" to "mobs",
        "quai vat" to "mobs",
        "động vật" to "animals",
        "dong vat" to "animals",
        "sinh tồn" to "survival",
        "sinh ton" to "survival",
        "kinh dị" to "horror",
        "kinh di" to "horror",
        "trình tạo bóng" to "shader",
        "trinh tao bong" to "shader",
        "gói tài nguyên" to "resource pack",
        "goi tai nguyen" to "resource pack",
        "nấu ăn" to "food cooking",
        "nau an" to "food cooking",
        "đồ ăn" to "food",
        "do an" to "food",
        "phương tiện" to "vehicle",
        "phuong tien" to "vehicle",
        "rồng" to "dragon",
        "rong" to "dragon",
        "nông trại" to "farming",
        "nong trai" to "farming",
        "kho chứa" to "storage",
        "kho chua" to "storage",
        "công cụ" to "utility tools",
        "cong cu" to "utility tools"
    )

    internal fun stripDiacritics(input: String): String {
        val replaced = input.replace('đ', 'd').replace('Đ', 'D')
        val normalized = Normalizer.normalize(replaced, Normalizer.Form.NFD)
        return VI_COMBINING_MARKS.replace(normalized, "").replace(WHITESPACE_COLLAPSE, " ").trim()
    }

    internal fun buildQueryVariants(rawQuery: String): List<String> {
        val trimmed = rawQuery.replace(WHITESPACE_COLLAPSE, " ").trim()
        if (trimmed.isEmpty()) return listOf("")
        val variants = LinkedHashSet<String>()
        variants.add(trimmed)

        val lower = trimmed.lowercase(Locale.ROOT)
        val asciiLower = stripDiacritics(trimmed).lowercase(Locale.ROOT)
        var translated = lower
        for ((vi, en) in VI_TO_EN_PHRASES) {
            if (translated.contains(vi)) {
                translated = translated.replace(vi, en)
            }
        }
        if (translated == lower) {
            var asciiTranslated = asciiLower
            for ((vi, en) in VI_TO_EN_PHRASES) {
                if (asciiTranslated.contains(vi)) {
                    asciiTranslated = asciiTranslated.replace(vi, en)
                }
            }
            if (asciiTranslated != asciiLower) {
                translated = asciiTranslated
            }
        }
        val cleanTranslated = stripDiacritics(translated)
        if (cleanTranslated.isNotBlank() && !cleanTranslated.equals(trimmed, ignoreCase = true)) {
            variants.add(cleanTranslated)
        }
        val asciiVariant = stripDiacritics(trimmed)
        if (asciiVariant.isNotBlank() && !asciiVariant.equals(trimmed, ignoreCase = true)) {
            variants.add(asciiVariant)
        }
        return variants.take(3)
    }

    /**
     * Loader yang dipakai untuk menyaring mod dan modpack. Instance Vanilla (atau tanpa instance)
     * ikut disaring ke loader yang didukung, supaya pengguna tidak memasang mod Forge yang pasti gagal.
     */
    internal fun effectiveLoader(instance: Instance?, loaderOverride: String? = null): String {
        val explicit = loaderOverride?.trim()?.lowercase().orEmpty()
        if (explicit.isNotEmpty() && explicit != "all") return explicit
        val loader = instance?.loader?.trim()?.lowercase().orEmpty()
        return if (loader.isEmpty() || loader == "vanilla") SUPPORTED_LOADER else loader
    }

    /**
     * Membaca daftar dunia (saves) yang ada di dalam instance
     */
    fun getInstanceWorlds(context: Context, instance: Instance): List<WorldInfo> {
        val gameDir = InstanceManager.getInstanceGameDir(context, instance)
        val savesDir = File(gameDir, "saves")
        if (!savesDir.exists() || !savesDir.isDirectory) return emptyList()

        val dirs = savesDir.listFiles { file -> file.isDirectory && !file.name.startsWith(".") } ?: return emptyList()
        return dirs.map { worldFolder ->
            val iconFile = File(worldFolder, "icon.png").takeIf { it.exists() }
            val levelDat = File(worldFolder, "level.dat")
            val lastPlayed = if (levelDat.exists()) levelDat.lastModified() else worldFolder.lastModified()
            WorldInfo(
                folderName = worldFolder.name,
                displayName = worldFolder.name,
                lastPlayed = lastPlayed,
                iconFile = iconFile
            )
        }.sortedByDescending { it.lastPlayed }
    }

    /**
     * Menghasilkan folder target sesuai jenis addon dan instance Minecraft
     */
    fun getTargetDir(context: Context, instance: Instance, itemType: String, worldName: String? = null): File {
        val gameDir = InstanceManager.getInstanceGameDir(context, instance)
        val dir = when (itemType) {
            "datapacks" -> {
                if (!worldName.isNullOrBlank()) {
                    File(gameDir, "saves/$worldName/datapacks")
                } else {
                    File(gameDir, "datapacks")
                }
            }
            "resourcepacks" -> File(gameDir, "resourcepacks")
            "shaderpacks" -> File(gameDir, "shaderpacks")
            "modpacks" -> File(gameDir, "modpacks")
            "worlds" -> File(gameDir, "saves")
            else -> File(gameDir, "mods")
        }
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /** Menghitung perkiraan ukuran folder dunia secara cepat tanpa traversal berlebihan. */
    private fun quickWorldSize(worldDir: File): Long {
        var total = 0L
        var count = 0
        fun walk(dir: File, depth: Int) {
            if (depth > 3 || count > 2000) return
            val children = dir.listFiles() ?: return
            for (f in children) {
                if (count > 2000) break
                count++
                if (f.isFile) {
                    total += f.length()
                } else if (f.isDirectory && !f.name.startsWith(".")) {
                    walk(f, depth + 1)
                }
            }
        }
        walk(worldDir, 0)
        return total
    }

    /**
     * Membaca semua item yang telah terpasang di dalam instance untuk jenis addon tertentu
     */
    suspend fun getInstalledItems(
        context: Context,
        instance: Instance,
        itemType: String,
        worldName: String? = null
    ): List<InstalledModItem> = withContext(Dispatchers.IO) {
        // Mesin Rust: shader dan resource pack ada di folder mesin, bukan di folder instance Java
        if (HorizonEngine.isEngine(instance.loader)) {
            return@withContext HorizonContent.installed(HorizonEngine.engineHome(context), itemType)
        }
        val gameDir = InstanceManager.getInstanceGameDir(context, instance)
        if (itemType == "worlds") {
            val savesDir = File(gameDir, "saves")
            val worldsList = getInstanceWorlds(context, instance)
            return@withContext worldsList.map { w ->
                val folder = File(savesDir, w.folderName)
                InstalledModItem(
                    id = w.folderName,
                    filename = w.folderName,
                    originalFilename = w.folderName,
                    name = w.displayName,
                    description = if (File(folder, "level.dat").isFile) "Dunia Minecraft (saves/${w.folderName})" else "Folder dunia (${w.folderName})",
                    version = "World",
                    category = "Dunia",
                    isEnabled = true,
                    sizeBytes = quickWorldSize(folder),
                    itemType = "worlds",
                    filePath = folder.absolutePath
                )
            }
        }
        val targetDir = getTargetDir(context, instance, itemType, worldName)
        if (!targetDir.exists() || !targetDir.isDirectory) return@withContext emptyList()

        val files = targetDir.listFiles() ?: return@withContext emptyList()
        val dao = runCatching { NovaLauncherDatabase.getInstance(context).favoriteModDao() }.getOrNull()
        val cachedByPath = if (dao != null) {
            runCatching { dao.getInstalledCacheForDir(targetDir.absolutePath, itemType).associateBy { it.filePath } }.getOrElse { emptyMap() }
        } else {
            emptyMap()
        }
        val preferCacheFast = com.aistudio.novacraft.launcher.core.utils.SettingsManager.settings.value.localOnlyMode
        val result = mutableListOf<InstalledModItem>()
        val cacheUpdates = mutableListOf<InstalledModCacheEntity>()

        for (file in files) {
            if (file.isDirectory && itemType != "datapacks") continue
            // Sisa unduhan yang terputus bukan item yang terpasang
            if (file.isFile && file.name.endsWith(".part")) continue

            val originalFilename = file.name
            val isEnabled = !originalFilename.endsWith(".disabled")
            val cleanFilename = if (!isEnabled) originalFilename.removeSuffix(".disabled") else originalFilename
            val filePath = file.absolutePath
            val fileLen = if (file.isFile) file.length() else 0L
            val fileMod = file.lastModified()

            val cached = cachedByPath[filePath]
            if (cached != null &&
                cached.originalFilename == originalFilename &&
                (preferCacheFast || (cached.sizeBytes == fileLen && cached.lastModified == fileMod))
            ) {
                result.add(cached.toInstalledModItem())
                continue
            }

            val fileStem = File(cleanFilename).nameWithoutExtension
            var title = fileStem
            var version = "Lokal"
            var description = cleanFilename

            // Coba ekstrak metadata dari file JAR mod jika bertipe mods
            if (itemType == "mods" && cleanFilename.endsWith(".jar", ignoreCase = true) && file.isFile) {
                try {
                    ZipFile(file).use { zip ->
                        // Fabric (fabric.mod.json), Forge (mods.toml), dan NeoForge (neoforge.mods.toml)
                        ModMetadata.read(zip)?.let { info ->
                            info.name?.takeIf { it.isNotBlank() }?.let { title = it }
                            info.version?.takeIf { it.isNotBlank() }?.let { version = it }
                            info.description?.takeIf { it.isNotBlank() }?.let { description = it }
                        }
                    }
                } catch (_: Exception) {
                    // Abaikan jika JAR tidak dapat diekstrak, fallback ke nama file
                }
            } else if (itemType == "modpacks" && (cleanFilename.endsWith(".mrpack", ignoreCase = true) || cleanFilename.endsWith(".zip", ignoreCase = true)) && file.isFile) {
                val manifest = loadModpackManifest(gameDir, cleanFilename)
                if (manifest != null) {
                    if (manifest.modpackName.isNotBlank()) title = manifest.modpackName
                    if (manifest.version.isNotBlank()) version = manifest.version
                    val modCount = manifest.installedFiles.count { it.startsWith("mods/") || it.endsWith(".jar", ignoreCase = true) }
                    description = if (modCount > 0) "$modCount mod terpasang" else cleanFilename
                } else {
                    try {
                        ZipFile(file).use { zip ->
                            val indexEntry = zip.getEntry("modrinth.index.json")
                                ?: zip.entries().asSequence().firstOrNull { it.name.endsWith("modrinth.index.json") }
                            if (indexEntry != null) {
                                zip.getInputStream(indexEntry).bufferedReader().use { reader ->
                                    val json = JsonParser.parseReader(reader).asJsonObject
                                    if (json.has("name")) title = json.get("name").asString
                                    if (json.has("versionId")) version = json.get("versionId").asString
                                    if (json.has("summary")) description = json.get("summary").asString
                                }
                            }
                        }
                    } catch (_: Exception) {
                    }
                }
            }

            val item = InstalledModItem(
                id = cleanFilename,
                filename = cleanFilename,
                originalFilename = originalFilename,
                name = title,
                description = description,
                version = version,
                category = itemType.replaceFirstChar { it.uppercase() },
                isEnabled = isEnabled,
                sizeBytes = fileLen,
                itemType = itemType,
                filePath = filePath
            )
            result.add(item)
            if (file.isFile) {
                cacheUpdates.add(InstalledModCacheEntity.fromInstalledModItem(item, targetDir.absolutePath, fileMod))
            }
        }

        if (dao != null && cacheUpdates.isNotEmpty()) {
            runCatching { dao.upsertInstalledCache(cacheUpdates) }
        }

        result.sortedBy { it.name.lowercase() }
    }

    /**
     * Mengubah status aktif/nonaktif mod dengan menambah atau melepas ekstensi .disabled
     */
    suspend fun toggleItemEnabled(
        context: Context,
        instance: Instance,
        itemType: String,
        item: InstalledModItem,
        worldName: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val targetDir = getTargetDir(context, instance, itemType, worldName)
            val currentFile = File(targetDir, item.originalFilename)
            val gameDir = InstanceManager.getInstanceGameDir(context, instance)

            if (itemType == "modpacks") {
                val cleanFilename = if (item.originalFilename.endsWith(".disabled")) {
                    item.originalFilename.removeSuffix(".disabled")
                } else {
                    item.originalFilename
                }

                // Ambil daftar file modpack dari manifest tracking atau arsip
                val manifest = loadModpackManifest(gameDir, cleanFilename)
                val fileList = manifest?.installedFiles ?: getModpackInstalledFilesFromArchive(currentFile)

                if (item.isEnabled) {
                    // Dinonaktifkan: rename semua mod jar milik modpack menjadi .disabled
                    for (relPath in fileList) {
                        if (relPath.startsWith("mods/") || relPath.endsWith(".jar", ignoreCase = true)) {
                            val activeFile = resolveModpackFile(gameDir, relPath) ?: continue
                            val disabledFile = resolveModpackFile(gameDir, "$relPath.disabled") ?: continue
                            if (activeFile.exists()) {
                                activeFile.renameTo(disabledFile)
                            }
                        }
                    }
                    val newFile = File(targetDir, "$cleanFilename.disabled")
                    if (currentFile.exists() && currentFile.name != newFile.name) {
                        currentFile.renameTo(newFile)
                    }
                } else {
                    // Diaktifkan: rename semua mod jar .disabled milik modpack kembali ke normal
                    for (relPath in fileList) {
                        if (relPath.startsWith("mods/") || relPath.endsWith(".jar", ignoreCase = true)) {
                            val disabledFile = resolveModpackFile(gameDir, "$relPath.disabled") ?: continue
                            val activeFile = resolveModpackFile(gameDir, relPath) ?: continue
                            if (disabledFile.exists()) {
                                disabledFile.renameTo(activeFile)
                            }
                        }
                    }
                    val newFile = File(targetDir, cleanFilename)
                    if (currentFile.exists() && currentFile.name != newFile.name) {
                        currentFile.renameTo(newFile)
                    }
                }
                return@withContext true
            }

            if (!currentFile.exists()) return@withContext false

            val newFilename = if (item.isEnabled) {
                "${item.filename}.disabled"
            } else {
                item.filename
            }

            val newFile = File(targetDir, newFilename)
            currentFile.renameTo(newFile)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Menghapus file mod / addon dari instance
     */
    suspend fun deleteItem(
        context: Context,
        instance: Instance,
        itemType: String,
        item: InstalledModItem,
        worldName: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        if (HorizonEngine.isEngine(instance.loader)) {
            return@withContext HorizonContent.delete(HorizonEngine.engineHome(context), itemType, item.originalFilename)
        }
        try {
            val gameDir = InstanceManager.getInstanceGameDir(context, instance)
            if (itemType == "worlds") {
                val savesDir = File(gameDir, "saves")
                val worldDir = SafePaths.resolveInside(savesDir, item.originalFilename) ?: return@withContext false
                return@withContext if (worldDir.exists()) worldDir.deleteRecursively() else false
            }
            val targetDir = getTargetDir(context, instance, itemType, worldName)
            val file = File(targetDir, item.originalFilename)

            if (itemType == "modpacks") {
                val cleanFilename = if (item.originalFilename.endsWith(".disabled")) {
                    item.originalFilename.removeSuffix(".disabled")
                } else {
                    item.originalFilename
                }

                // 1. Baca manifest modpack (atau fallback dari arsip zip jika metadata belum ada)
                val manifest = loadModpackManifest(gameDir, cleanFilename)
                val fileList = (manifest?.installedFiles ?: getModpackInstalledFilesFromArchive(file)).toMutableSet()

                // 2. Hapus HANYA file-file yang terpasang oleh modpack ini (baik aktif maupun disabled)
                // Daftar ini bisa berasal dari berkas yang diubah orang lain: hanya path di dalam folder instance
                // (dan bukan dunia/saves) yang boleh dihapus.
                for (relPath in fileList) {
                    val activeFile = resolveModpackFile(gameDir, relPath)
                    if (activeFile != null && activeFile.exists()) {
                        if (activeFile.isDirectory) activeFile.deleteRecursively() else activeFile.delete()
                    }
                    val disabledFile = resolveModpackFile(gameDir, "$relPath.disabled")
                    if (disabledFile != null && disabledFile.exists()) {
                        if (disabledFile.isDirectory) disabledFile.deleteRecursively() else disabledFile.delete()
                    }
                }

                // 3. Hapus file metadata manifest
                val metaFile = getModpackMetadataFile(gameDir, cleanFilename)
                if (metaFile.exists()) {
                    metaFile.delete()
                }

                // 4. Hapus file modpack itu sendiri (baik .mrpack maupun .mrpack.disabled)
                if (file.exists()) {
                    if (file.isDirectory) file.deleteRecursively() else file.delete()
                }
                val cleanArchive = File(targetDir, cleanFilename)
                if (cleanArchive.exists()) cleanArchive.delete()
                val disabledArchive = File(targetDir, "$cleanFilename.disabled")
                if (disabledArchive.exists()) disabledArchive.delete()

                true
            } else {
                if (file.exists()) {
                    if (file.isDirectory) file.deleteRecursively() else file.delete()
                } else {
                    false
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    // (Impor berkas lokal telah disatukan ke NovaUnifiedFileImportEngine)

    // ---------------------------------------------------------------------------------------------
    // MESIN RUST (Horizon): hanya Shader dan Resource Pack, langsung ke folder dan pengaturan mesin
    // ---------------------------------------------------------------------------------------------

    /**
     * Memilih atau melepas satu shader / resource pack lewat berkas pengaturan mesin (bukan ganti nama berkas).
     * Berlaku saat game dibuka lagi. Galatnya berisi pesan yang aman ditampilkan ke pengguna.
     */
    suspend fun toggleEngineItem(context: Context, itemType: String, item: InstalledModItem): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                // Mesin tidak menjalankan mod di APK 32-bit; mematikan tetap boleh
                if (itemType == HorizonContent.TYPE_MODS && !item.isEnabled && !android.os.Process.is64Bit()) {
                    throw java.io.IOException(HorizonMods.MSG_NEED_64)
                }
                HorizonContent.setEnabled(
                    HorizonEngine.engineHome(context),
                    itemType,
                    item.originalFilename,
                    !item.isEnabled
                )
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Mengunduh satu versi untuk mesin Rust: hanya berkas .zip yang punya sidik jari, diperiksa dengan aturan
     * mesin, lalu dipindah ke folder mesin. Dependensi sengaja diabaikan (shader dan resource pack tidak
     * memakainya). Berkas yang sudah ada dengan nama sama dilewati.
     */
    private fun downloadForEngine(
        context: Context,
        version: ModrinthVersion,
        itemType: String,
        onProgress: (String) -> Unit
    ): Result<List<String>> {
        // Mod selalu dari katalog Horizon; shader dan resource pack bila versinya berasal dari katalog itu
        if (itemType == HorizonContent.TYPE_MODS || HorizonRegistry.isRegistryVersion(version)) {
            return downloadHorizonFile(context, version, itemType, onProgress)
        }
        var staged: File? = null
        return try {
            if (!HorizonContent.supports(itemType)) throw java.io.IOException(HorizonContent.MSG_TYPE)
            val picked = HorizonContent.pickFile(version) ?: throw java.io.IOException(HorizonContent.MSG_NO_ZIP)
            if (picked.size > HorizonContent.MAX_FILE_BYTES) throw java.io.IOException(HorizonContent.MSG_TOO_BIG)
            if (!HorizonContent.hasChecksum(picked)) throw java.io.IOException(HorizonContent.MSG_NO_HASH)

            // Nama berkas datang dari respons server: pastikan tidak berisi folder ("../")
            val name = SafePaths.fileName(picked.filename)
            val home = HorizonEngine.engineHome(context)
            if (HorizonContent.exists(home, itemType, name)) {
                Result.success(emptyList())
            } else {
                onProgress("Mengunduh $name...")
                val stageDir = File(context.cacheDir, "horizon-dl")
                if (!stageDir.isDirectory && !stageDir.mkdirs()) throw java.io.IOException(HorizonContent.MSG_SAVE)
                val target = File(stageDir, name)
                staged = target
                downloadFile(picked.url, target)
                verifyHashes(target, picked.hashes)
                onProgress("Memeriksa $name...")
                val finalName = HorizonContent.install(target, home, itemType, name).getOrThrow()
                Result.success(listOf(finalName))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            staged?.delete()
        }
    }

    /**
     * Mengunduh satu berkas dari katalog Horizon (mod, shader asli, atau resource pack): alamat harus rilis GitHub,
     * ukuran dan SHA-256 harus sama dengan katalog, lalu berkasnya diperiksa dengan aturan mesin sebelum masuk ke
     * folder tujuan. Dependensi tidak ada. Mod yang sama diganti (id manifest), shader dan resource pack yang
     * namanya sudah ada dilewati.
     */
    private fun downloadHorizonFile(
        context: Context,
        version: ModrinthVersion,
        itemType: String,
        onProgress: (String) -> Unit
    ): Result<List<String>> {
        var staged: File? = null
        return try {
            val kind = HorizonRegistry.kindFor(itemType) ?: throw java.io.IOException(HorizonContent.MSG_TYPE)
            val extension = HorizonRegistry.extensionOf(kind) ?: throw java.io.IOException(HorizonContent.MSG_TYPE)
            val file = version.files.firstOrNull { it.primary } ?: version.files.firstOrNull()
                ?: throw java.io.IOException(HorizonRegistry.MSG_NO_FILE)
            val name = SafePaths.fileName(file.filename)
            val sha256 = file.hashes?.get("sha256")?.trim()?.lowercase().orEmpty()
            if (!name.endsWith(extension)) throw java.io.IOException(HorizonRegistry.MSG_NO_FILE)
            if (!HorizonRegistry.acceptedUrl(file.url, name)) throw java.io.IOException(HorizonRegistry.MSG_URL)
            if (!HorizonRegistry.isSha256(sha256)) throw java.io.IOException(HorizonRegistry.MSG_HASH)
            if (file.size <= 0L || file.size > HorizonRegistry.maxBytes(kind)) {
                throw java.io.IOException(if (kind == HorizonRegistry.KIND_MOD) HorizonMods.MSG_TOO_BIG else HorizonContent.MSG_TOO_BIG)
            }

            val home = HorizonEngine.engineHome(context)
            if (kind != HorizonRegistry.KIND_MOD && HorizonContent.exists(home, itemType, name)) {
                Result.success(emptyList())
            } else {
                val stageDir = File(context.cacheDir, "horizon-dl")
                if (!stageDir.isDirectory && !stageDir.mkdirs()) throw java.io.IOException(HorizonContent.MSG_SAVE)
                val target = File(stageDir, name)
                staged = target
                onProgress("Mengunduh $name...")
                downloadFile(file.url, target, file.size)
                if (target.length() != file.size) throw java.io.IOException(HorizonRegistry.MSG_SIZE)
                verifyHashes(target, file.hashes)
                onProgress("Memeriksa $name...")
                val finalName = HorizonContent.install(target, home, itemType, name).getOrThrow()
                Result.success(listOf(finalName))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            staged?.delete()
        }
    }

    /** Katalog Horizon disimpan di memori 10 menit supaya mencari dan membuka detail tidak mengunduhnya terus. */
    @Volatile
    private var horizonIndexCache: Pair<Long, HorizonRegistry.Index>? = null
    private const val HORIZON_INDEX_TTL_MS = 10L * 60L * 1000L

    internal fun fetchHorizonIndexCached(forceRefresh: Boolean = false): HorizonRegistry.Index? {
        return try {
            if (forceRefresh) horizonIndexCache = null
            horizonIndex()
        } catch (_: Exception) {
            horizonIndexCache?.second
        }
    }

    private fun horizonIndex(): HorizonRegistry.Index {
        val cached = horizonIndexCache
        val now = System.currentTimeMillis()
        if (cached != null && now - cached.first < HORIZON_INDEX_TTL_MS) return cached.second
        val index = try {
            val request = Request.Builder()
                .url(HorizonRegistry.INDEX_URL)
                .header("User-Agent", USER_AGENT)
                .build()
            val text = httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw java.io.IOException(HorizonRegistry.MSG_INDEX)
                val body = response.body ?: throw java.io.IOException(HorizonRegistry.MSG_INDEX)
                val out = java.io.ByteArrayOutputStream()
                body.byteStream().use { input ->
                    val buffer = ByteArray(32 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        if (out.size() + read > HorizonRegistry.MAX_INDEX_BYTES) {
                            throw java.io.IOException(HorizonRegistry.MSG_INDEX)
                        }
                        out.write(buffer, 0, read)
                    }
                }
                out.toString(Charsets.UTF_8.name())
            }
            HorizonRegistry.parse(text)
        } catch (e: java.io.IOException) {
            // Galat jaringan dari pustaka berbahasa Inggris; ganti dengan pesan yang sudah diterjemahkan
            throw if (e.message == HorizonRegistry.MSG_NEWER) e else java.io.IOException(HorizonRegistry.MSG_INDEX)
        }
        horizonIndexCache = now to index
        HorizonEngine.updateFromRegistryIndex(null, index)
        return index
    }

    // (importEngineFile telah disatukan ke NovaUnifiedFileImportEngine)

    /**
     * Pencarian katalog global + Vietnam (Modrinth & CurseForge) dengan Auto-Version,
     * Auto-Loader, dukungan bahasa Vietnam/multibahasa, dan pelonggaran filter otomatis saat hasil kosong.
     */
    suspend fun searchProjects(
        query: String,
        itemType: String,
        instance: Instance?,
        category: String? = null,
        filterByGameVersion: Boolean = true,
        page: Int = 1,
        limit: Int = 24,
        sortIndex: String = "relevance",
        filterByLoader: Boolean = true,
        source: String = SOURCE_ALL,
        mcVersionOverride: String? = null,
        loaderOverride: String? = null,
        timeWindow: String = "all"
    ): Result<ModrinthSearchResponse> = withContext(Dispatchers.IO) {
        // Mesin Rust: Modrinth atau katalog Horizon, tanpa CurseForge dan tanpa saringan versi Minecraft
        val engineSearch = HorizonEngine.isEngine(instance?.loader)
        // Mod untuk mesin Rust selalu dari katalog Horizon; shader dan resource pack dari Horizon bila sumber itu dipilih
        val horizonKind = if (engineSearch && (itemType == HorizonContent.TYPE_MODS || source == SOURCE_HORIZON)) {
            HorizonRegistry.kindFor(itemType)
        } else {
            null
        }
        if (horizonKind != null) {
            return@withContext try {
                val activeMc = HorizonEngine.resolvedMcVersion()
                Result.success(
                    HorizonRegistry.search(
                        horizonIndex(), horizonKind, query, activeMc, sortIndex, page, limit
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        val effectiveVersionFilter = filterByGameVersion && !mcVersionOverride.equals("all", ignoreCase = true)
        val effectiveLoaderFilter = filterByLoader && !loaderOverride.equals("all", ignoreCase = true)
        val variants = buildQueryVariants(query)
        val effectiveSortForQuery = if (timeWindow != "all" && sortIndex == "relevance") "updated" else sortIndex

        // Chế độ Local-Only: bỏ qua truy vấn mạng, đọc trực tiếp từ bộ nhớ đệm mod & danh sách yêu thích cục bộ
        if (com.aistudio.novacraft.launcher.core.utils.SettingsManager.settings.value.localOnlyMode) {
            return@withContext Result.success(
                searchCachedModsLocally(
                    query = query,
                    itemType = itemType,
                    instance = instance,
                    category = category,
                    filterByGameVersion = effectiveVersionFilter,
                    filterByLoader = effectiveLoaderFilter,
                    mcVersionOverride = mcVersionOverride,
                    loaderOverride = loaderOverride,
                    sortIndex = effectiveSortForQuery,
                    timeWindow = timeWindow,
                    page = page,
                    limit = limit
                )
            )
        }

        suspend fun searchCurseForgeSmart(): Result<ModrinthSearchResponse> {
            val isDefaultWorldBrowse = itemType == "worlds" && query.isBlank()
            val effectiveCfSort = if (isDefaultWorldBrowse && effectiveSortForQuery == "relevance") "downloads" else effectiveSortForQuery

            var best: Result<ModrinthSearchResponse>? = null
            for (qVariant in variants) {
                val cfQuery = if (isDefaultWorldBrowse && !category.isNullOrBlank()) category else qVariant
                val attempt = CurseForgeApi.search(
                    queryText = cfQuery,
                    itemType = itemType,
                    instance = instance,
                    filterByGameVersion = effectiveVersionFilter,
                    page = page,
                    limit = limit,
                    sortIndex = effectiveCfSort,
                    filterByLoader = effectiveLoaderFilter,
                    category = category,
                    mcVersionOverride = mcVersionOverride,
                    loaderOverride = loaderOverride
                )
                if (attempt.isSuccess) {
                    best = attempt
                    if (!attempt.getOrNull()?.hits.isNullOrEmpty()) {
                        return attempt
                    }
                } else if (best == null) {
                    best = attempt
                }
            }

            // Pelonggaran otomatis pada halaman 1 bila pencarian ketat tidak memberi hasil
            if (page == 1 && (effectiveVersionFilter || effectiveLoaderFilter)) {
                val primaryQ = variants.lastOrNull() ?: variants.first()
                val cfQuery = if (isDefaultWorldBrowse && !category.isNullOrBlank()) category else primaryQ
                if (effectiveVersionFilter) {
                    val noVer = CurseForgeApi.search(
                        queryText = cfQuery,
                        itemType = itemType,
                        instance = instance,
                        filterByGameVersion = false,
                        page = 1,
                        limit = limit,
                        sortIndex = effectiveCfSort,
                        filterByLoader = effectiveLoaderFilter,
                        category = category,
                        mcVersionOverride = null,
                        loaderOverride = loaderOverride
                    )
                    if (noVer.isSuccess && !noVer.getOrNull()?.hits.isNullOrEmpty()) return noVer
                }
                if (query.isNotBlank() && effectiveLoaderFilter) {
                    val broad = CurseForgeApi.search(
                        queryText = cfQuery,
                        itemType = itemType,
                        instance = instance,
                        filterByGameVersion = false,
                        page = 1,
                        limit = limit,
                        sortIndex = effectiveCfSort,
                        filterByLoader = false,
                        category = category,
                        mcVersionOverride = null,
                        loaderOverride = null
                    )
                    if (broad.isSuccess && !broad.getOrNull()?.hits.isNullOrEmpty()) return broad
                }
            }
            return best ?: Result.failure(Exception("Gagal memuat dari CurseForge"))
        }

        fun searchModrinthSmart(): Result<ModrinthSearchResponse> {
            var best: Result<ModrinthSearchResponse>? = null
            for (qVariant in variants) {
                val attempt = executeModrinthSearch(
                    query = qVariant,
                    itemType = itemType,
                    instance = instance,
                    category = category,
                    useGameVersionFacet = effectiveVersionFilter,
                    page = page,
                    limit = limit,
                    sortIndex = effectiveSortForQuery,
                    filterByLoader = effectiveLoaderFilter,
                    engineSearch = engineSearch,
                    mcVersionOverride = mcVersionOverride,
                    loaderOverride = loaderOverride
                )
                if (attempt.isSuccess) {
                    best = attempt
                    if (!attempt.getOrNull()?.hits.isNullOrEmpty()) {
                        return attempt
                    }
                } else if (best == null) {
                    best = attempt
                }
            }

            if (page == 1 && best?.isSuccess == true && best.getOrNull()?.hits.isNullOrEmpty()) {
                val fallbackQuery = variants.lastOrNull() ?: variants.first()
                if (effectiveVersionFilter) {
                    val relaxedVer = executeModrinthSearch(
                        query = fallbackQuery,
                        itemType = itemType,
                        instance = instance,
                        category = category,
                        useGameVersionFacet = false,
                        page = 1,
                        limit = limit,
                        sortIndex = effectiveSortForQuery,
                        filterByLoader = effectiveLoaderFilter,
                        engineSearch = engineSearch,
                        mcVersionOverride = null,
                        loaderOverride = loaderOverride
                    )
                    if (relaxedVer.isSuccess && !relaxedVer.getOrNull()?.hits.isNullOrEmpty()) {
                        return relaxedVer
                    }
                }
                if (query.isNotBlank() && effectiveLoaderFilter) {
                    val relaxedAll = executeModrinthSearch(
                        query = fallbackQuery,
                        itemType = itemType,
                        instance = instance,
                        category = category,
                        useGameVersionFacet = false,
                        page = 1,
                        limit = limit,
                        sortIndex = effectiveSortForQuery,
                        filterByLoader = false,
                        engineSearch = engineSearch,
                        mcVersionOverride = null,
                        loaderOverride = null
                    )
                    if (relaxedAll.isSuccess && !relaxedAll.getOrNull()?.hits.isNullOrEmpty()) {
                        return relaxedAll
                    }
                }
                if (itemType == "worlds" && query.isBlank() && (category.isNullOrBlank() || category.equals("Semua", ignoreCase = true))) {
                    val broad = executeModrinthSearch(
                        query = "",
                        itemType = itemType,
                        instance = instance,
                        category = "Semua",
                        useGameVersionFacet = false,
                        page = 1,
                        limit = limit,
                        sortIndex = "downloads",
                        filterByLoader = false,
                        engineSearch = engineSearch,
                        broadWorldFallback = true
                    )
                    if (broad.isSuccess && !broad.getOrNull()?.hits.isNullOrEmpty()) {
                        return broad
                    }
                }
            }
            return best ?: Result.failure(Exception("Gagal memuat dari Modrinth"))
        }

        val cfEnabled = CurseForgeApi.isConfigured()
        val rawResult = if (source == SOURCE_CURSEFORGE && !engineSearch && cfEnabled) {
            searchCurseForgeSmart()
        } else if (source == SOURCE_ALL && !engineSearch && cfEnabled) {
            coroutineScope {
                val modrinthDef = async { searchModrinthSmart() }
                val curseForgeDef = async { searchCurseForgeSmart() }
                val modRes = modrinthDef.await()
                val cfRes = curseForgeDef.await()

                val modData = modRes.getOrNull()
                val cfData = cfRes.getOrNull()
                if (modData == null && cfData == null) {
                    val msg = modRes.exceptionOrNull()?.message
                        ?: cfRes.exceptionOrNull()?.message
                        ?: "Gagal terhubung ke Modrinth & CurseForge"
                    Result.failure(Exception(msg))
                } else {
                    val merged = mergeDualProviderResponses(
                        modData = modData,
                        cfData = cfData,
                        sortIndex = effectiveSortForQuery,
                        page = page,
                        limit = limit
                    )
                    Result.success(merged)
                }
            }
        } else {
            searchModrinthSmart()
        }
        rawResult.getOrNull()?.hits?.takeIf { it.isNotEmpty() }?.let { hits ->
            val ctx = com.aistudio.novacraft.launcher.core.utils.SettingsManager.appContext
            if (ctx != null) {
                runCatching {
                    val dao = NovaLauncherDatabase.getInstance(ctx).favoriteModDao()
                    dao.upsertCachedMods(hits.map { CachedModEntity.fromSearchHit(it, itemType) })
                }
            }
        }
        rawResult.map { resp -> applyTimeWindowFilter(resp, timeWindow, effectiveSortForQuery) }
    }

    private suspend fun searchCachedModsLocally(
        query: String,
        itemType: String,
        instance: Instance?,
        category: String?,
        filterByGameVersion: Boolean,
        filterByLoader: Boolean,
        mcVersionOverride: String?,
        loaderOverride: String?,
        sortIndex: String,
        timeWindow: String,
        page: Int,
        limit: Int
    ): ModrinthSearchResponse {
        val ctx = com.aistudio.novacraft.launcher.core.utils.SettingsManager.appContext
            ?: return ModrinthSearchResponse(emptyList(), 0, limit, 0)
        val dao = NovaLauncherDatabase.getInstance(ctx).favoriteModDao()
        val cachedHits = dao.getCachedModsByType(itemType)
            .filterNot { CurseForgeMapper.isCurseForgeId(it.projectId) }
            .map { it.toSearchHit() }
        val favHits = dao.getFavoritesByType(itemType)
            .filterNot { CurseForgeMapper.isCurseForgeId(it.projectId) }
            .map { it.toSearchHit() }

        val mergedMap = LinkedHashMap<String, ModrinthSearchHit>()
        for (hit in favHits) mergedMap[hit.projectId] = hit
        for (hit in cachedHits) mergedMap.putIfAbsent(hit.projectId, hit)

        val q = query.trim().lowercase(Locale.ROOT)
        val targetMc = mcVersionOverride?.trim()?.takeIf { it.isNotEmpty() && !it.equals("all", ignoreCase = true) }
            ?: instance?.mcVersion?.trim()?.takeIf { it.isNotEmpty() }
        val targetLoader = if (filterByLoader && !loaderOverride.equals("all", ignoreCase = true) && (itemType == "mods" || itemType == "modpacks")) {
            effectiveLoader(instance, loaderOverride).lowercase(Locale.ROOT)
        } else null
        val normalizedCat = category?.trim()?.takeIf { it.isNotEmpty() && !it.equals("Semua", ignoreCase = true) }?.lowercase(Locale.ROOT)

        val filtered = mergedMap.values.filter { hit ->
            val matchesQ = q.isEmpty() ||
                hit.title.lowercase(Locale.ROOT).contains(q) ||
                (hit.author?.lowercase(Locale.ROOT)?.contains(q) == true) ||
                (hit.description?.lowercase(Locale.ROOT)?.contains(q) == true)
            val cats = (hit.displayCategories ?: hit.categories).orEmpty().map { it.lowercase(Locale.ROOT) }
            val matchesCat = normalizedCat == null || cats.any { c ->
                c == normalizedCat ||
                    (normalizedCat == "performance" && c == "optimization") ||
                    (normalizedCat == "cosmetic" && c == "decoration") ||
                    (normalizedCat == "world-gen" && c == "worldgen")
            }
            val matchesVer = !filterByGameVersion || targetMc == null ||
                hit.versions.isNullOrEmpty() || hit.versions.any { it.equals(targetMc, ignoreCase = true) }
            val matchesLoader = targetLoader == null ||
                cats.none { it in setOf("fabric", "forge", "neoforge", "quilt", "liteloader") } ||
                cats.any { it == targetLoader }
            matchesQ && matchesCat && matchesVer && matchesLoader
        }

        val sorted = when (sortIndex) {
            "downloads" -> filtered.sortedByDescending { it.downloads }
            "follows" -> filtered.sortedWith(compareByDescending<ModrinthSearchHit> { it.follows }.thenByDescending { it.downloads })
            "updated" -> filtered.sortedByDescending { parseInstantMillis(it.dateModified).takeIf { ts -> ts > 0L } ?: parseInstantMillis(it.dateCreated) }
            "newest" -> filtered.sortedByDescending { parseInstantMillis(it.dateCreated).takeIf { ts -> ts > 0L } ?: parseInstantMillis(it.dateModified) }
            else -> filtered.sortedByDescending { it.downloads }
        }
        val offset = (page - 1).coerceAtLeast(0) * limit
        val pageHits = if (offset >= sorted.size) emptyList() else sorted.subList(offset, min(offset + limit, sorted.size))
        return applyTimeWindowFilter(
            ModrinthSearchResponse(hits = pageHits, offset = offset, limit = limit, totalHits = sorted.size),
            timeWindow,
            sortIndex
        )
    }

    private fun parseInstantMillis(iso: String?): Long {
        if (iso.isNullOrBlank()) return 0L
        return try {
            Instant.parse(iso.trim()).toEpochMilli()
        } catch (_: Exception) {
            0L
        }
    }

    private fun applyTimeWindowFilter(
        response: ModrinthSearchResponse,
        timeWindow: String,
        sortIndex: String
    ): ModrinthSearchResponse {
        val days = when (timeWindow) {
            "day" -> 1L
            "week" -> 7L
            "month" -> 30L
            "year" -> 365L
            else -> return response
        }
        val cutoffMs = Instant.now().minus(days, ChronoUnit.DAYS).toEpochMilli()
        val filtered = response.hits.filter { hit ->
            val stamp = if (sortIndex == "newest") {
                parseInstantMillis(hit.dateCreated).takeIf { it > 0L } ?: parseInstantMillis(hit.dateModified)
            } else {
                parseInstantMillis(hit.dateModified).takeIf { it > 0L } ?: parseInstantMillis(hit.dateCreated)
            }
            stamp >= cutoffMs
        }
        return if (filtered.isNotEmpty()) {
            response.copy(hits = filtered, totalHits = filtered.size)
        } else {
            response
        }
    }

    private fun mergeDualProviderResponses(
        modData: ModrinthSearchResponse?,
        cfData: ModrinthSearchResponse?,
        sortIndex: String,
        page: Int,
        limit: Int
    ): ModrinthSearchResponse {
        val mHits = modData?.hits.orEmpty()
        val cHits = cfData?.hits.orEmpty()
        val combined = ArrayList<ModrinthSearchHit>(mHits.size + cHits.size)
        when (sortIndex) {
            "downloads" -> {
                combined.addAll(mHits)
                combined.addAll(cHits)
                combined.sortByDescending { it.downloads }
            }
            "follows" -> {
                combined.addAll(mHits)
                combined.addAll(cHits)
                combined.sortWith(compareByDescending<ModrinthSearchHit> { it.follows }.thenByDescending { it.downloads })
            }
            "updated" -> {
                combined.addAll(mHits)
                combined.addAll(cHits)
                combined.sortByDescending {
                    parseInstantMillis(it.dateModified).takeIf { ts -> ts > 0L } ?: parseInstantMillis(it.dateCreated)
                }
            }
            "newest" -> {
                combined.addAll(mHits)
                combined.addAll(cHits)
                combined.sortByDescending {
                    parseInstantMillis(it.dateCreated).takeIf { ts -> ts > 0L } ?: parseInstantMillis(it.dateModified)
                }
            }
            else -> {
                val maxLen = max(mHits.size, cHits.size)
                for (i in 0 until maxLen) {
                    if (i < mHits.size) combined.add(mHits[i])
                    if (i < cHits.size) combined.add(cHits[i])
                }
            }
        }
        val maxMerged = limit * 2
        val seenKeys = HashSet<String>(combined.size)
        val deduped = ArrayList<ModrinthSearchHit>(maxMerged)
        for (hit in combined) {
            val normTitle = stripDiacritics(hit.title).lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{Nd}]"), "")
            val dedupKey = if (normTitle.length >= 4) "${hit.projectType}:$normTitle" else hit.projectId
            if (seenKeys.add(dedupKey)) {
                deduped.add(hit)
                if (deduped.size >= maxMerged) break
            }
        }
        val total = (modData?.totalHits ?: 0) + (cfData?.totalHits ?: 0)
        val finalTotal = if (total > 0) total else deduped.size
        val offset = (page - 1).coerceAtLeast(0) * maxMerged
        return ModrinthSearchResponse(
            hits = deduped,
            offset = offset,
            limit = maxMerged,
            totalHits = finalTotal
        )
    }

    private fun executeModrinthSearch(
        query: String,
        itemType: String,
        instance: Instance?,
        category: String?,
        useGameVersionFacet: Boolean,
        page: Int,
        limit: Int,
        sortIndex: String,
        filterByLoader: Boolean,
        engineSearch: Boolean,
        broadWorldFallback: Boolean = false,
        mcVersionOverride: String? = null,
        loaderOverride: String? = null
    ): Result<ModrinthSearchResponse> {
        return try {
            val facets = mutableListOf<List<String>>()
            var effectiveQuery = query.trim()

            // 1. Project type facet & Rekomendasi Otomatis untuk Toko Dunia
            if (itemType == "worlds") {
                facets.add(listOf("project_type:datapack", "project_type:modpack"))
                if (!broadWorldFallback) {
                    when (category?.lowercase()) {
                        null, "", "semua" -> {
                            facets.add(listOf("categories:adventure", "categories:worldgen"))
                            if (effectiveQuery.isBlank()) {
                                effectiveQuery = "world"
                            }
                        }
                        "adventure", "worldgen" -> {
                            facets.add(listOf("categories:${category.lowercase()}"))
                            if (effectiveQuery.isBlank()) {
                                effectiveQuery = if (category.equals("worldgen", ignoreCase = true)) "world" else "map"
                            }
                        }
                        else -> {
                            // survival, skyblock, parkour, dll.
                            effectiveQuery = if (effectiveQuery.isBlank()) category else "$effectiveQuery $category"
                        }
                    }
                }
            } else {
                val projectType = when (itemType) {
                    "resourcepacks" -> "resourcepack"
                    "shaderpacks" -> "shader"
                    "datapacks" -> "datapack"
                    "modpacks" -> "modpack"
                    else -> "mod"
                }
                facets.add(listOf("project_type:$projectType"))

                // 2. Category facet (jika dipilih user; petakan alias kategori terpadu ke slug Modrinth)
                if (!category.isNullOrBlank() && !category.equals("Semua", ignoreCase = true)) {
                    val mrCategory = when (category.trim().lowercase(Locale.ROOT)) {
                        "performance" -> "optimization"
                        "cosmetic" -> "decoration"
                        "world-gen" -> "worldgen"
                        else -> category.trim().lowercase(Locale.ROOT)
                    }
                    facets.add(listOf("categories:$mrCategory"))
                }
            }

            // 3. Game Version filter (Auto-Version dari instance atau pilihan manual)
            val explicitMc = mcVersionOverride?.trim()?.takeIf {
                it.isNotEmpty() && !it.equals("all", ignoreCase = true)
            }
            val targetMc = explicitMc ?: instance?.mcVersion?.trim()?.takeIf { it.isNotEmpty() }
            if (useGameVersionFacet && !engineSearch && targetMc != null) {
                facets.add(listOf("versions:$targetMc"))
            }

            // 4. Loader filter (loader instance / override; Vanilla atau tanpa instance memakai loader yang didukung)
            if (filterByLoader && !loaderOverride.equals("all", ignoreCase = true) && (itemType == "mods" || itemType == "modpacks")) {
                facets.add(listOf("categories:${effectiveLoader(instance, loaderOverride)}"))
            }

            val facetsJson = gson.toJson(facets)
            val offset = (page - 1) * limit

            // Urutan hasil: untuk dunia tanpa kata kunci pencarian, otomatis urutkan berdasarkan unduhan terbanyak (rekomendasi)
            val baseSort = if (itemType == "worlds" && query.isBlank() && sortIndex == "relevance") {
                "downloads"
            } else {
                sortIndex
            }
            val index = if (baseSort in SEARCH_SORT_INDEXES) baseSort else "relevance"

            val url = StringBuilder("$MODRINTH_API_BASE/search?")
                .append("query=").append(URLEncoder.encode(effectiveQuery, "UTF-8").replace("+", "%20"))
                .append("&limit=").append(limit)
                .append("&offset=").append(offset)
                .append("&index=").append(index)
                .append("&facets=").append(URLEncoder.encode(facetsJson, "UTF-8").replace("+", "%20"))
                .toString()

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.failure(Exception("Modrinth API error code: ${response.code}"))
                }
                val body = response.body?.string() ?: return Result.failure(Exception("Empty response"))
                val searchRes = gson.fromJson(body, ModrinthSearchResponse::class.java)
                Result.success(searchRes)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mengambil detail lengkap suatu project dari Modrinth
     */
    suspend fun getProjectDetails(
        projectId: String,
        horizon: Boolean = false
    ): Result<ModrinthProject> = withContext(Dispatchers.IO) {
        // Proyek dari katalog Horizon (mesin Rust): detail diambil dari katalog, bukan Modrinth
        if (horizon) {
            return@withContext try {
                val project = HorizonRegistry.find(horizonIndex(), projectId)
                    ?: throw java.io.IOException(HorizonRegistry.MSG_INDEX)
                Result.success(HorizonRegistry.toProject(project, HorizonEngine.resolvedMcVersion()))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
        if (com.aistudio.novacraft.launcher.core.utils.SettingsManager.settings.value.localOnlyMode) {
            val ctx = com.aistudio.novacraft.launcher.core.utils.SettingsManager.appContext
            if (ctx != null) {
                val dao = NovaLauncherDatabase.getInstance(ctx).favoriteModDao()
                val localProj = dao.getCachedModById(projectId)?.toProject()
                    ?: dao.getFavoriteById(projectId)?.toProject()
                if (localProj != null) return@withContext Result.success(localProj)
            }
            return@withContext Result.failure(IllegalStateException("Đang bật chế độ Local-Only: chỉ hiển thị dữ liệu trong bộ nhớ đệm cục bộ."))
        }
        if (CurseForgeMapper.isCurseForgeId(projectId)) {
            return@withContext CurseForgeApi.projectDetails(projectId)
        }
        try {
            val url = "$MODRINTH_API_BASE/project/$projectId"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Gagal mengambil detail project: ${response.code}"))
            }

            val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
            val project = gson.fromJson(body, ModrinthProject::class.java)
            Result.success(project)
        } catch (e: Exception) {
            val ctx = com.aistudio.novacraft.launcher.core.utils.SettingsManager.appContext
            if (ctx != null) {
                val dao = NovaLauncherDatabase.getInstance(ctx).favoriteModDao()
                val fallback = dao.getCachedModById(projectId)?.toProject()
                    ?: dao.getFavoriteById(projectId)?.toProject()
                if (fallback != null) return@withContext Result.success(fallback)
            }
            Result.failure(e)
        }
    }

    /**
     * Mengambil seluruh daftar rilis versi untuk project tertentu
     */
    suspend fun getProjectVersions(projectId: String): Result<List<ModrinthVersion>> = withContext(Dispatchers.IO) {
        try {
            val url = "$MODRINTH_API_BASE/project/$projectId/version"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Gagal mengambil versi project: ${response.code}"))
            }

            val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
            val listType = object : TypeToken<List<ModrinthVersion>>() {}.type
            val versions: List<ModrinthVersion> = gson.fromJson(body, listType)
            Result.success(versions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mengambil dan menyaring secara ketat versi yang kompatibel dengan instance aktif atau filter pengguna
     */
    suspend fun getCompatibleVersions(
        projectId: String,
        instance: Instance?,
        itemType: String,
        filterByGameVersion: Boolean = true,
        source: String = SOURCE_MODRINTH,
        mcVersionOverride: String? = null,
        loaderOverride: String? = null,
        filterByLoader: Boolean = true
    ): Result<List<ModrinthVersion>> = withContext(Dispatchers.IO) {
        // Mesin Rust: mod (dan shader/resource pack bila sumbernya Horizon) dari katalog Horizon, versi yang cocok
        // dengan versi game mesin
        val horizonKind = if (HorizonEngine.isEngine(instance?.loader) &&
            (itemType == HorizonContent.TYPE_MODS || source == SOURCE_HORIZON)
        ) {
            HorizonRegistry.kindFor(itemType)
        } else {
            null
        }
        if (horizonKind != null) {
            return@withContext try {
                val index = horizonIndex()
                val project = HorizonRegistry.find(index, projectId, horizonKind)
                    ?: throw java.io.IOException(HorizonRegistry.MSG_INDEX)
                val list = HorizonRegistry.toVersions(index, project, HorizonEngine.resolvedMcVersion())
                if (list.isEmpty()) Result.failure(java.io.IOException(HorizonRegistry.MSG_NO_FILE)) else Result.success(list)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
        // Mesin Rust: semua versi yang punya berkas .zip; yang mencantumkan versi game mesin diurutkan lebih dulu
        if (HorizonEngine.isEngine(instance?.loader)) {
            return@withContext getProjectVersions(projectId)
                .map { HorizonContent.usableVersions(it, HorizonEngine.resolvedMcVersion()) }
        }
        if (com.aistudio.novacraft.launcher.core.utils.SettingsManager.settings.value.localOnlyMode) {
            return@withContext Result.failure(
                IllegalStateException("Chế độ Local-Only đang bật: hãy nhập tệp mod cục bộ hoặc tắt Local-Only để tải phiên bản mới.")
            )
        }
        if (CurseForgeMapper.isCurseForgeId(projectId)) {
            return@withContext CurseForgeApi.compatibleVersions(
                prefixedId = projectId,
                instance = instance,
                itemType = itemType,
                filterByGameVersion = filterByGameVersion,
                mcVersionOverride = mcVersionOverride,
                loaderOverride = loaderOverride,
                filterByLoader = filterByLoader
            )
        }
        val allVersResult = getProjectVersions(projectId)
        if (allVersResult.isFailure) return@withContext allVersResult

        val allVersions = allVersResult.getOrNull() ?: emptyList()
        if (instance == null) return@withContext Result.success(allVersions)

        val targetMc = mcVersionOverride?.trim()?.takeIf { it.isNotEmpty() && !it.equals("all", ignoreCase = true) }
            ?: instance.mcVersion.trim()
        val enforceMc = filterByGameVersion && !mcVersionOverride.equals("all", ignoreCase = true) && targetMc.isNotEmpty()
        val enforceLoader = filterByLoader && !loaderOverride.equals("all", ignoreCase = true) && (itemType == "mods" || itemType == "modpacks")
        val instLoader = effectiveLoader(instance, loaderOverride)

        val compatible = allVersions.filter { v ->
            val matchesVersion = !enforceMc || v.gameVersions.contains(targetMc)
            val matchesLoader = !enforceLoader || v.loaders.any { it.equals(instLoader, ignoreCase = true) }
            matchesVersion && matchesLoader
        }

        val finalVersions = if (compatible.isEmpty() && itemType == "worlds" && enforceMc) {
            allVersions.sortedByDescending { it.gameVersions.contains(targetMc) }
        } else {
            compatible
        }

        Result.success(finalVersions)
    }

    // (inspectZipType telah disatukan ke NovaUnifiedFileImportEngine)

    /**
     * Sistem Auto-Download Dependensi:
     * Mengunduh file utama versi target, lalu secara rekursif mencari dan mengunduh dependensi wajib (required)
     * yang kompatibel dengan instance dan belum terpasang.
     */
    suspend fun downloadVersionAndDependencies(
        context: Context,
        instance: Instance,
        version: ModrinthVersion,
        itemType: String,
        worldName: String? = null,
        onProgress: (statusText: String) -> Unit = {}
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        if (HorizonEngine.isEngine(instance.loader)) {
            return@withContext downloadForEngine(context, version, itemType, onProgress)
        }
        if (itemType == "worlds") {
            return@withContext downloadWorldVersion(context, instance, version, worldName, onProgress)
        }
        val visitedProjects = mutableSetOf<String>()
        val downloadedFiles = mutableListOf<String>()

        try {
            // Ambil daftar file yang sudah terpasang agar tidak mendownload ulang
            val installedList = getInstalledItems(context, instance, itemType, worldName)
            val installedFilenames = installedList.map { it.filename.lowercase() }.toMutableSet()

            suspend fun resolveAndDownload(targetVer: ModrinthVersion) {
                if (targetVer.files.isEmpty()) return

                // 1. Pilih primary file atau file pertama
                val primaryFile = targetVer.files.firstOrNull { it.primary } ?: targetVer.files.first()
                // Nama berkas datang dari respons server: pastikan tidak berisi folder ("../")
                val filename = SafePaths.fileName(primaryFile.filename)

                if (targetVer.projectId.isNotBlank() && visitedProjects.contains(targetVer.projectId)) {
                    return
                }
                if (targetVer.projectId.isNotBlank()) {
                    visitedProjects.add(targetVer.projectId)
                }

                // Download file jika belum ada
                if (!installedFilenames.contains(filename.lowercase())) {
                    onProgress("Mengunduh $filename...")
                    val targetDir = getTargetDir(context, instance, itemType, worldName)
                    val destFile = File(targetDir, filename)

                    downloadFile(primaryFile.url, destFile)
                    verifyHashes(destFile, primaryFile.hashes)
                    installedFilenames.add(filename.lowercase())
                    downloadedFiles.add(filename)

                    if (itemType == "modpacks" && (filename.endsWith(".mrpack", ignoreCase = true) || filename.endsWith(".zip", ignoreCase = true))) {
                        onProgress("Mengekstrak dan menyiapkan modpack...")
                        processMrpackFile(instance, destFile, context) { status, _, _ ->
                            onProgress(status)
                        }
                    }
                }

                // 2. Periksa dependensi wajib (required dependencies)
                val requiredDeps = targetVer.dependencies?.filter {
                    it.dependencyType.equals("required", ignoreCase = true) &&
                            (!it.versionId.isNullOrBlank() || !it.projectId.isNullOrBlank())
                } ?: emptyList()

                for (dep in requiredDeps) {
                    try {
                        if (!dep.projectId.isNullOrBlank() && visitedProjects.contains(dep.projectId)) {
                            continue
                        }

                        var depVersionObj: ModrinthVersion? = null

                        // Cari berdasarkan version_id jika ada
                        if (!dep.versionId.isNullOrBlank()) {
                            val vUrl = "$MODRINTH_API_BASE/version/${dep.versionId}"
                            val req = Request.Builder().url(vUrl).header("User-Agent", USER_AGENT).build()
                            val resp = httpClient.newCall(req).execute()
                            if (resp.isSuccessful) {
                                val vBody = resp.body?.string()
                                if (vBody != null) {
                                    depVersionObj = gson.fromJson(vBody, ModrinthVersion::class.java)
                                }
                            }
                        }

                        // Jika tidak ada version_id atau gagal, cari berdasarkan project_id versi yang kompatibel
                        if (depVersionObj == null && !dep.projectId.isNullOrBlank()) {
                            val compRes = getCompatibleVersions(dep.projectId, instance, itemType)
                            val compList = compRes.getOrNull()
                            if (!compList.isNullOrEmpty()) {
                                depVersionObj = compList.first()
                            }
                        }

                        if (depVersionObj != null && depVersionObj.files.isNotEmpty()) {
                            val depPrimary = depVersionObj.files.firstOrNull { it.primary } ?: depVersionObj.files.first()
                            if (!installedFilenames.contains(SafePaths.fileName(depPrimary.filename).lowercase())) {
                                onProgress("Menyelesaikan dependensi: ${depPrimary.filename}...")
                                resolveAndDownload(depVersionObj)
                            }
                        }
                    } catch (e: Exception) {
                        // Jangan batalkan seluruh instalasi jika salah satu dependensi sekunder gagal diakses
                    }
                }
            }

            resolveAndDownload(version)
            Result.success(downloadedFiles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mengunduh arsip dunia Java (.zip / world-datapack) ke folder sementara lalu memasangnya lewat
     * [NovaUnifiedFileImportEngine.installWorldArchive]. Bila item Modrinth ternyata berupa datapack worldgen
     * murni (tanpa level.dat), salin aman ke datapack instance/dunia yang dipilih.
     */
    private suspend fun downloadWorldVersion(
        context: Context,
        instance: Instance,
        version: ModrinthVersion,
        worldName: String?,
        onProgress: (statusText: String) -> Unit
    ): Result<List<String>> {
        if (version.files.isEmpty()) {
            return Result.failure(java.io.IOException("Versi ini tidak memiliki berkas unduhan."))
        }
        val picked = version.files.firstOrNull {
            it.filename.lowercase().endsWith(".zip")
        } ?: version.files.firstOrNull { it.primary } ?: version.files.first()
        val filename = SafePaths.fileName(picked.filename)
        val stageDir = File(context.cacheDir, "world_dl_stage")
        if (!stageDir.exists()) stageDir.mkdirs()
        val stagedFile = File(stageDir, "dl_${System.currentTimeMillis()}_$filename")

        return try {
            onProgress("Mengunduh dunia $filename...")
            downloadFile(picked.url, stagedFile)
            verifyHashes(stagedFile, picked.hashes)

            val suggestedName = version.name.ifBlank { File(filename).nameWithoutExtension }
            val worldInstall = NovaUnifiedFileImportEngine.installWorldArchive(
                context = context,
                instance = instance,
                archiveFile = stagedFile,
                suggestedName = suggestedName,
                onProgress = { status, _, _ -> onProgress(status) }
            )
            if (worldInstall.isSuccess) {
                Result.success(listOf(worldInstall.getOrThrow()))
            } else {
                val detected = NovaUnifiedFileImportEngine.inspectZipType(stagedFile)
                if (detected == "datapacks") {
                    val dpDir = getTargetDir(context, instance, "datapacks", worldName)
                    val destDp = File(dpDir, filename)
                    stagedFile.copyTo(destDp, overwrite = true)
                    Result.success(listOf(filename))
                } else if (detected == "modpacks" && filename.endsWith(".mrpack", ignoreCase = true)) {
                    val packDir = getTargetDir(context, instance, "modpacks", null)
                    val destPack = File(packDir, filename)
                    stagedFile.copyTo(destPack, overwrite = true)
                    processMrpackFile(instance, destPack, context) { status, _, _ -> onProgress(status) }
                    Result.success(listOf(filename))
                } else {
                    Result.failure(
                        worldInstall.exceptionOrNull()
                            ?: java.io.IOException("Arsip tidak berisi dunia Minecraft yang valid.")
                    )
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try {
                stagedFile.delete()
            } catch (_: Exception) {
            }
        }
    }

    internal suspend fun processMrpackFile(
        instance: Instance,
        mrpackFile: File,
        context: Context,
        onProgress: (statusText: String, current: Int, total: Int) -> Unit = { _, _, _ -> }
    ) = withContext(Dispatchers.IO) {
        try {
            val gameDir = InstanceManager.getInstanceGameDir(context, instance)
            val cleanFilename = mrpackFile.name.removeSuffix(".disabled")
            val installedRelativeFiles = mutableListOf<String>()
            var packTitle = mrpackFile.nameWithoutExtension
            var packVersion = ""
            var skippedFiles = 0
            var failedFiles = 0
            var extractedBytes = 0L
            // Catatan tambahan di pesan akhir (mis. mod CurseForge yang unduhannya ditutup penulis)
            var packNote = ""

            ZipFile(mrpackFile).use { zip ->
                // 1. Cari modrinth.index.json
                val indexEntry = zip.getEntry("modrinth.index.json")
                    ?: zip.entries().asSequence().firstOrNull { it.name.endsWith("modrinth.index.json") }
                // Bukan mrpack: mungkin modpack CurseForge (manifest.json di akar zip)
                val cfPack = if (indexEntry == null) readCurseForgeManifest(zip) else null

                if (indexEntry != null) {
                    val prefix = if (indexEntry.name.contains("/")) indexEntry.name.substringBeforeLast("/") + "/" else ""
                    val filesToDownload = mutableListOf<MrpackFileEntry>()

                    zip.getInputStream(indexEntry).bufferedReader().use { reader ->
                        val indexJson = JsonParser.parseReader(reader).asJsonObject
                        // Tolak modpack yang tidak cocok sebelum ada file yang diunduh
                        checkModpackCompatibility(indexJson, instance)
                        if (indexJson.has("name")) {
                            packTitle = indexJson.get("name").asString
                        }
                        if (indexJson.has("versionId")) {
                            packVersion = indexJson.get("versionId").asString
                        }

                        if (indexJson.has("files")) {
                            val files = indexJson.getAsJsonArray("files")
                            for (element in files) {
                                try {
                                    val fileObj = element.asJsonObject
                                    val env = fileObj.getAsJsonObject("env")
                                    if (env != null && env.has("client") && env.get("client").asString.equals("unsupported", ignoreCase = true)) {
                                        continue
                                    }
                                    val relPath = fileObj.get("path")?.asString ?: continue
                                    val downloads = fileObj.getAsJsonArray("downloads")
                                    if (downloads == null || downloads.size() == 0) continue
                                    val dlUrl = downloads.get(0).asString

                                    // Path harus tetap di dalam folder instance, dan sumber unduhan harus HTTPS
                                    // dari host modpack yang dikenal. Selain itu dilewati.
                                    if (resolveModpackFile(gameDir, relPath) == null ||
                                        !SafePaths.isAllowedModpackDownloadUrl(dlUrl)
                                    ) {
                                        skippedFiles++
                                        LauncherLog.w("NuxModManager", "File modpack dilewati (path/alamat tidak aman): $relPath")
                                        continue
                                    }

                                    val hashes = mutableMapOf<String, String>()
                                    val hashObj = fileObj.getAsJsonObject("hashes")
                                    if (hashObj != null) {
                                        for ((algo, value) in hashObj.entrySet()) {
                                            if (value.isJsonPrimitive) hashes[algo.lowercase()] = value.asString
                                        }
                                    }
                                    filesToDownload.add(MrpackFileEntry(dlUrl, relPath.replace('\\', '/'), hashes))
                                } catch (e: Exception) {
                                    skippedFiles++
                                }
                            }
                        }
                    }

                    val totalFiles = filesToDownload.size
                    onProgress("Membaca daftar modpack ($totalFiles mod ditemukan)...", 0, totalFiles)
                    var currentIdx = 0
                    for (entry in filesToDownload) {
                        currentIdx++
                        val targetFile = resolveModpackFile(gameDir, entry.relPath)
                        if (targetFile == null) {
                            skippedFiles++
                            continue
                        }
                        if (!targetFile.exists()) {
                            targetFile.parentFile?.mkdirs()
                            onProgress("Mengunduh mod ($currentIdx/$totalFiles): ${targetFile.name}...", currentIdx, totalFiles)
                            try {
                                downloadFile(entry.url, targetFile)
                                verifyHashes(targetFile, entry.hashes)
                                installedRelativeFiles.add(entry.relPath)
                            } catch (e: Exception) {
                                failedFiles++
                                LauncherLog.e("NuxModManager", "Gagal unduh file modpack: ${entry.url}", e)
                            }
                        } else {
                            installedRelativeFiles.add(entry.relPath)
                            onProgress("Mod sudah ada ($currentIdx/$totalFiles): ${targetFile.name}", currentIdx, totalFiles)
                        }
                    }

                    // 2. Extract overrides/ and client-overrides/
                    onProgress("Mengekstrak konfigurasi modpack...", totalFiles, totalFiles)
                    val overridesPrefix = "${prefix}overrides/"
                    val clientOverridesPrefix = "${prefix}client-overrides/"
                    val entries = zip.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        val name = entry.name
                        val rel = when {
                            name.startsWith(overridesPrefix) -> name.removePrefix(overridesPrefix)
                            name.startsWith(clientOverridesPrefix) -> name.removePrefix(clientOverridesPrefix)
                            else -> null
                        }
                        if (rel != null && rel.isNotBlank() && !rel.startsWith("saves/")) {
                            val outFile = resolveModpackFile(gameDir, rel)
                            if (outFile == null) {
                                skippedFiles++
                                LauncherLog.w("NuxModManager", "Isi modpack dilewati (path tidak aman): $name")
                                continue
                            }
                            installedRelativeFiles.add(rel)
                            if (entry.isDirectory) {
                                outFile.mkdirs()
                            } else {
                                extractedBytes = extractZipEntry(zip, entry, outFile, extractedBytes)
                            }
                        }
                    }
                } else if (cfPack != null) {
                    // Modpack CurseForge: daftar mod ada di manifest.json, isi tambahan di folder overrides/
                    checkCurseForgePackCompatibility(cfPack, instance)
                    if (cfPack.name.isNotBlank()) packTitle = cfPack.name
                    packVersion = cfPack.version

                    onProgress("Mencari alamat unduhan mod dari CurseForge...", 0, 0)
                    val wanted = cfPack.files
                    val resolved = CurseForgeApi.resolvePackFiles(wanted)
                    val classIds = CurseForgeApi.classIdsOf(wanted.map { it.projectId })

                    val totalFiles = wanted.size
                    var currentIdx = 0
                    var blockedFiles = 0
                    for (packFile in wanted) {
                        currentIdx++
                        val info = resolved[packFile.fileId]
                        val dlUrl = info?.url
                        if (info == null || dlUrl == null) {
                            // Penulis menutup unduhan lewat API (atau file sudah dihapus): dihormati, tidak dipaksa
                            blockedFiles++
                            skippedFiles++
                            continue
                        }
                        val folder = CurseForgeMapper.folderFor(classIds[packFile.projectId], info.fileName)
                        val relPath = folder + "/" + SafePaths.fileName(info.fileName)
                        val targetFile = resolveModpackFile(gameDir, relPath)
                        if (targetFile == null || !SafePaths.isAllowedModpackDownloadUrl(dlUrl)) {
                            skippedFiles++
                            LauncherLog.w("NuxModManager", "File modpack dilewati (path/alamat tidak aman): $relPath")
                            continue
                        }
                        if (!targetFile.exists()) {
                            targetFile.parentFile?.mkdirs()
                            onProgress("Mengunduh mod ($currentIdx/$totalFiles): ${targetFile.name}...", currentIdx, totalFiles)
                            try {
                                downloadFile(dlUrl, targetFile)
                                verifyHashes(targetFile, info.sha1?.let { mapOf("sha1" to it) })
                                installedRelativeFiles.add(relPath)
                            } catch (e: Exception) {
                                failedFiles++
                                LauncherLog.e("NuxModManager", "Gagal unduh file modpack: $dlUrl", e)
                            }
                        } else {
                            installedRelativeFiles.add(relPath)
                            onProgress("Mod sudah ada ($currentIdx/$totalFiles): ${targetFile.name}", currentIdx, totalFiles)
                        }
                    }
                    if (blockedFiles > 0) {
                        packNote = " $blockedFiles mod tidak bisa diunduh karena penulisnya menutup unduhan lewat launcher. " +
                            "Pasang manual dari situs CurseForge bila perlu."
                    }

                    onProgress("Mengekstrak konfigurasi modpack...", totalFiles, totalFiles)
                    val overridesPrefix = cfPack.overrides + "/"
                    val overrideEntries = zip.entries()
                    while (overrideEntries.hasMoreElements()) {
                        val entry = overrideEntries.nextElement()
                        val name = entry.name
                        if (!name.startsWith(overridesPrefix)) continue
                        val rel = name.removePrefix(overridesPrefix)
                        if (rel.isBlank() || rel.startsWith("saves/")) continue
                        val outFile = resolveModpackFile(gameDir, rel)
                        if (outFile == null) {
                            skippedFiles++
                            LauncherLog.w("NuxModManager", "Isi modpack dilewati (path tidak aman): $name")
                            continue
                        }
                        installedRelativeFiles.add(rel)
                        if (entry.isDirectory) {
                            outFile.mkdirs()
                        } else {
                            extractedBytes = extractZipEntry(zip, entry, outFile, extractedBytes)
                        }
                    }
                } else {
                    // Fallback jika file zip modpack bukan standar Modrinth mrpack
                    // (misal zip kumpulan mod yang langsung berisi folder mods/ atau file .jar)
                    onProgress("Mengekstrak file arsip modpack...", 0, 0)
                    val entries = zip.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        val name = entry.name
                        if (entry.isDirectory) continue

                        val relPath: String? = when {
                            name.startsWith("mods/") || name.startsWith("config/") ||
                            name.startsWith("shaderpacks/") || name.startsWith("resourcepacks/") ||
                            name.startsWith("datapacks/") -> name
                            name.endsWith(".jar", ignoreCase = true) && !name.contains("/") -> "mods/" + SafePaths.fileName(name)
                            else -> null
                        }
                        if (relPath == null) continue

                        val outFile = resolveModpackFile(gameDir, relPath)
                        if (outFile == null) {
                            skippedFiles++
                            LauncherLog.w("NuxModManager", "Isi arsip dilewati (path tidak aman): $name")
                            continue
                        }
                        installedRelativeFiles.add(relPath)
                        extractedBytes = extractZipEntry(zip, entry, outFile, extractedBytes)
                    }
                }
            }

            // Simpan manifest modpack untuk pelacakan delete & toggle
            val manifest = ModpackManifest(
                modpackName = packTitle,
                version = packVersion,
                archiveFilename = cleanFilename,
                installedFiles = installedRelativeFiles.distinct()
            )
            saveModpackManifest(gameDir, manifest)

            val installedModCount = installedRelativeFiles.count { it.startsWith("mods/") || it.endsWith(".jar", ignoreCase = true) }
            val problemCount = failedFiles + skippedFiles
            val doneMessage = if (problemCount == 0) {
                "Selesai! $installedModCount mod berhasil dipasang."
            } else {
                "Selesai dengan catatan: $installedModCount mod terpasang, $problemCount file gagal atau dilewati. Coba pasang ulang bila ada mod yang kurang."
            }
            onProgress(doneMessage + packNote, installedRelativeFiles.size, installedRelativeFiles.size)
        } catch (e: Exception) {
            LauncherLog.e("NuxModManager", "Error processing mrpack: ${e.message}", e)
            if (e is ModpackIncompatibleException) {
                // Modpack ditolak: buang arsipnya supaya tidak tampil sebagai modpack yang terpasang
                try {
                    mrpackFile.delete()
                } catch (_: Exception) {
                }
            }
            throw e
        }
    }

    /** Membaca manifest.json di akar zip; null bila bukan modpack CurseForge. */
    private fun readCurseForgeManifest(zip: ZipFile): CurseForgeMapper.PackManifest? {
        val entry = zip.getEntry("manifest.json") ?: return null
        return try {
            zip.getInputStream(entry).bufferedReader().use { reader ->
                val root = JsonParser.parseReader(reader)
                if (root.isJsonObject) CurseForgeMapper.parseManifest(root.asJsonObject) else null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Seperti [checkModpackCompatibility] tetapi untuk modpack CurseForge: loader dan versi Minecraft
     * harus cocok dengan instance sebelum ada file yang diunduh.
     */
    private fun checkCurseForgePackCompatibility(pack: CurseForgeMapper.PackManifest, instance: Instance) {
        val loader = pack.loader
        if (loader == "quilt") {
            throw ModpackIncompatibleException(
                "Modpack ini membutuhkan Quilt, sedangkan NovaCraft saat ini hanya mendukung Fabric, Forge, dan NeoForge."
            )
        }
        if ((loader == "forge" || loader == "fabric" || loader == "neoforge") &&
            !instance.loader.equals(loader, ignoreCase = true)
        ) {
            val label = if (loader == "neoforge") "NeoForge" else loader.replaceFirstChar { it.uppercase() }
            throw ModpackIncompatibleException(
                "Modpack ini membutuhkan $label. " +
                    "Buat instance $label dulu, lalu pasang modpack di instance itu."
            )
        }
        val packMc = pack.minecraftVersion
        if (packMc.isNotBlank() && instance.mcVersion.isNotBlank() && packMc != instance.mcVersion) {
            throw ModpackIncompatibleException(
                "Modpack ini untuk Minecraft $packMc, sedangkan instance ini memakai ${instance.mcVersion}. " +
                    "Pilih atau buat instance dengan versi yang sama."
            )
        }
    }

    /**
     * Memeriksa modpack Modrinth sebelum ada file yang diunduh: loader dan versi Minecraft-nya harus
     * cocok dengan instance. Bila tidak cocok, dilempar [ModpackIncompatibleException] dengan pesan jelas.
     */
    private fun checkModpackCompatibility(indexJson: com.google.gson.JsonObject, instance: Instance) {
        val deps = indexJson.getAsJsonObject("dependencies") ?: return

        val otherLoader = listOf("quilt-loader" to "Quilt")
            .firstOrNull { deps.has(it.first) }
        // Satu modpack hanya punya satu loader; bila ada beberapa, Fabric diutamakan
        val requiredLoader = when {
            deps.has("fabric-loader") -> "fabric" to "Fabric"
            deps.has("neoforge") -> "neoforge" to "NeoForge"
            deps.has("forge") -> "forge" to "Forge"
            else -> null
        }

        if (requiredLoader == null && otherLoader != null) {
            throw ModpackIncompatibleException(
                "Modpack ini membutuhkan ${otherLoader.second}, sedangkan NovaCraft saat ini hanya mendukung Fabric, Forge, dan NeoForge."
            )
        }
        if (requiredLoader != null && !instance.loader.equals(requiredLoader.first, ignoreCase = true)) {
            throw ModpackIncompatibleException(
                "Modpack ini membutuhkan ${requiredLoader.second}. " +
                    "Buat instance ${requiredLoader.second} dulu, lalu pasang modpack di instance itu."
            )
        }

        val packMc = deps.get("minecraft")?.takeIf { it.isJsonPrimitive }?.asString
        if (!packMc.isNullOrBlank() && instance.mcVersion.isNotBlank() && packMc != instance.mcVersion) {
            throw ModpackIncompatibleException(
                "Modpack ini untuk Minecraft $packMc, sedangkan instance ini memakai ${instance.mcVersion}. " +
                    "Pilih atau buat instance dengan versi yang sama."
            )
        }
    }

    /**
     * Mengunduh [url] ke [destFile]. Hanya HTTPS. Unduhan ditulis ke berkas sementara (.part) dulu dan
     * baru dipindah setelah selesai, jadi unduhan yang terputus tidak tertinggal sebagai berkas rusak.
     */
    private fun downloadFile(url: String, destFile: File, maxBytes: Long = Long.MAX_VALUE) {
        if (!url.trim().startsWith("https://", ignoreCase = true)) {
            throw java.io.IOException("Alamat unduhan ditolak karena tidak memakai HTTPS")
        }
        val request = Request.Builder()
            .url(url.trim())
            .header("User-Agent", USER_AGENT)
            .build()

        val partFile = File(destFile.parentFile, destFile.name + ".part")
        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("Gagal mengunduh file: HTTP ${response.code}")
                }
                val body = response.body ?: throw Exception("Body stream kosong")
                body.byteStream().use { input ->
                    FileOutputStream(partFile).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var total = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            total += read
                            // Sumber yang mengirim lebih banyak dari yang dijanjikan dihentikan di tengah jalan
                            if (total > maxBytes) throw java.io.IOException(HorizonRegistry.MSG_SIZE)
                            output.write(buffer, 0, read)
                        }
                    }
                }
            }
            if (destFile.exists()) destFile.delete()
            if (!partFile.renameTo(destFile)) {
                throw java.io.IOException("Gagal menyimpan file unduhan")
            }
        } catch (e: Exception) {
            partFile.delete()
            throw e
        }
    }

    /**
     * Mencocokkan sidik jari (sha256 dari katalog Horizon, atau sha512, atau sha1 bila tidak ada) berkas dengan yang diumumkan sumbernya.
     * Bila tidak cocok, berkas dihapus dan dilempar IOException. Tanpa sidik jari, tidak ada yang diperiksa.
     */
    private fun verifyHashes(file: File, hashes: Map<String, String>?) {
        val map = hashes ?: return
        val sha256 = map["sha256"]?.trim().orEmpty()
        val sha512 = map["sha512"]?.trim().orEmpty()
        val sha1 = map["sha1"]?.trim().orEmpty()
        val algorithm: String
        val expected: String
        when {
            sha256.isNotEmpty() -> { algorithm = "SHA-256"; expected = sha256 }
            sha512.isNotEmpty() -> { algorithm = "SHA-512"; expected = sha512 }
            sha1.isNotEmpty() -> { algorithm = "SHA-1"; expected = sha1 }
            else -> return
        }

        val digest = java.security.MessageDigest.getInstance(algorithm)
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        if (!actual.equals(expected, ignoreCase = true)) {
            file.delete()
            throw java.io.IOException("Berkas ${file.name} rusak atau berubah di jalan (sidik jari tidak cocok), jadi tidak dipasang.")
        }
    }

    /**
     * Menyalin satu entri zip ke [outFile] dan mengembalikan total byte yang sudah diekstrak.
     * Berhenti dengan IOException bila total melewati [MAX_MODPACK_EXTRACT_BYTES] (arsip "bom").
     */
    private fun extractZipEntry(zip: ZipFile, entry: java.util.zip.ZipEntry, outFile: File, usedBytes: Long): Long {
        var total = usedBytes
        outFile.parentFile?.mkdirs()
        zip.getInputStream(entry).use { input ->
            FileOutputStream(outFile).use { output ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > MAX_MODPACK_EXTRACT_BYTES) {
                        throw java.io.IOException("Isi modpack terlalu besar, pemasangan dibatalkan.")
                    }
                    output.write(buffer, 0, read)
                }
            }
        }
        return total
    }

    /**
     * File di dalam [gameDir] untuk path milik modpack. Null bila path tidak aman (keluar folder instance)
     * atau menyentuh dunia permainan (saves), supaya modpack tidak bisa menimpa atau menghapus dunia.
     */
    private fun resolveModpackFile(gameDir: File, relPath: String): File? {
        val normalized = relPath.replace('\\', '/')
        if (normalized == "saves" || normalized.startsWith("saves/")) return null
        return SafePaths.resolveInside(gameDir, relPath)
    }

    fun getModpackMetadataFile(gameDir: File, cleanFilename: String): File {
        val metaDir = File(gameDir, "modpacks/.metadata")
        if (!metaDir.exists()) metaDir.mkdirs()
        val baseName = if (cleanFilename.endsWith(".disabled")) cleanFilename.removeSuffix(".disabled") else cleanFilename
        return File(metaDir, "$baseName.json")
    }

    fun saveModpackManifest(gameDir: File, manifest: ModpackManifest) {
        try {
            val metaFile = getModpackMetadataFile(gameDir, manifest.archiveFilename)
            metaFile.writeText(gson.toJson(manifest))
        } catch (e: Exception) {
            LauncherLog.e("NuxModManager", "Gagal menyimpan metadata modpack: ${e.message}", e)
        }
    }

    fun loadModpackManifest(gameDir: File, cleanFilename: String): ModpackManifest? {
        try {
            val metaFile = getModpackMetadataFile(gameDir, cleanFilename)
            if (metaFile.exists() && metaFile.isFile) {
                val json = metaFile.readText()
                return gson.fromJson(json, ModpackManifest::class.java)
            }
        } catch (e: Exception) {
            LauncherLog.e("NuxModManager", "Gagal membaca metadata modpack: ${e.message}", e)
        }
        return null
    }

    fun getModpackInstalledFilesFromArchive(archiveFile: File): List<String> {
        val files = mutableListOf<String>()
        if (!archiveFile.exists() || !archiveFile.isFile) return files
        try {
            ZipFile(archiveFile).use { zip ->
                val indexEntry = zip.getEntry("modrinth.index.json")
                    ?: zip.entries().asSequence().firstOrNull { it.name.endsWith("modrinth.index.json") }
                if (indexEntry != null) {
                    val prefix = if (indexEntry.name.contains("/")) indexEntry.name.substringBeforeLast("/") + "/" else ""
                    zip.getInputStream(indexEntry).bufferedReader().use { reader ->
                        val indexJson = JsonParser.parseReader(reader).asJsonObject
                        if (indexJson.has("files")) {
                            for (el in indexJson.getAsJsonArray("files")) {
                                val rel = el.asJsonObject.get("path")?.asString
                                if (!rel.isNullOrBlank()) files.add(rel)
                            }
                        }
                    }
                    val overridesPrefix = "${prefix}overrides/"
                    val clientOverridesPrefix = "${prefix}client-overrides/"
                    val entries = zip.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        if (entry.isDirectory) continue
                        val name = entry.name
                        val rel = when {
                            name.startsWith(overridesPrefix) -> name.removePrefix(overridesPrefix)
                            name.startsWith(clientOverridesPrefix) -> name.removePrefix(clientOverridesPrefix)
                            else -> null
                        }
                        if (rel != null && rel.isNotBlank() && !rel.startsWith("saves/")) {
                            files.add(rel)
                        }
                    }
                } else {
                    val entries = zip.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        if (entry.isDirectory) continue
                        val name = entry.name
                        when {
                            name.startsWith("mods/") || name.startsWith("config/") ||
                            name.startsWith("shaderpacks/") || name.startsWith("resourcepacks/") ||
                            name.startsWith("datapacks/") -> {
                                files.add(name)
                            }
                            name.endsWith(".jar", ignoreCase = true) && !name.contains("/") -> {
                                files.add("mods/$name")
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            LauncherLog.e("NuxModManager", "Error getModpackInstalledFilesFromArchive: ${e.message}", e)
        }
        // Path dari arsip luar tidak boleh keluar dari folder instance (mis. "../../")
        return files.distinct().filter { SafePaths.isSafeRelative(it) }
    }
}

// ----------------------------------------------------------------------------
// Section: core/horizon/HorizonEngine.kt
// ----------------------------------------------------------------------------

/**
 * Mesin Rust (Horizon): pengganti SELURUH client Java Minecraft, bukan loader mod seperti Fabric atau
 * Forge. Di jalur ini tidak ada JVM, client.jar, maupun library Java, dan mod Java tidak bisa dijalankannya.
 *
 * Cara kerja:
 * - Mesin berupa satu pustaka native (libhorizon_android.so) dari proyek Horizon Android (MIT). Pustaka itu
 *   dijalankan oleh [ACTIVITY_CLASS] di proses sendiri (:horizon). Proses terpisah itu penting: mesin
 *   memanggil process::exit saat selesai, dan itu tidak boleh ikut mematikan launcher.
 * - Pustaka dan data tabelnya TIDAK disimpan di repo. scripts/fetch-horizon-engine.sh menaruhnya saat build
 *   (lihat docs/rust-engine.md). Tanpa keduanya instance Rust tidak bisa dibuat: [availability] menolaknya.
 * - PLAY menulis [LAUNCH_FILE] (akun yang dipilih di launcher: offline atau Microsoft) ke folder data mesin lalu
 *   membuka [ACTIVITY_CLASS]. Mesin yang dikompilasi dengan perubahan Amber (patch_set di horizon-engine.properties)
 *   membaca berkas itu, memasang sumber daya game bila belum ada, langsung bermain dengan akun tadi, dan menutup
 *   dirinya saat game selesai sehingga pemain kembali ke launcher. Mesin tanpa perubahan itu mengabaikan berkasnya
 *   dan menampilkan layar peluncur Horizon seperti biasa.
 * - Sumber daya game (sekitar 500 MiB) diunduh dari Mojang oleh mesinnya sendiri saat pertama dibuka.
 */
object HorizonEngine {
    /** Nilai `Instance.loader` untuk instance yang dijalankan mesin Rust. */
    const val LOADER_ID = "horizon"

    /** Nama di rel loader layar Buat Instance. */
    const val DISPLAY_NAME = "Rust"

    /** Nama lengkap untuk label instance (kartu Home, daftar instance). */
    const val LABEL = "Rust (Horizon)"

    /** Versi dasar Minecraft Horizon saat belum ada pembaruan otomatis yang terdeteksi. */
    const val DEFAULT_MC_VERSION = "26.3"
    const val BASELINE_MC_VERSION = DEFAULT_MC_VERSION
    const val SUPPORTED_MC_VERSION = DEFAULT_MC_VERSION

    sealed interface VersionSyncState {
        object Idle : VersionSyncState
        object Syncing : VersionSyncState
        data class Synced(
            val source: String,
            val updatedAtMs: Long,
            val currentVersion: String
        ) : VersionSyncState
        data class Error(
            val reason: String,
            val currentVersion: String
        ) : VersionSyncState
    }

    private const val VERSION_CACHE_FILE = "horizon-version.txt"

    private val _liveMcVersion = MutableStateFlow(DEFAULT_MC_VERSION)
    val liveMcVersion: StateFlow<String> = _liveMcVersion.asStateFlow()

    private val _versionSyncState = MutableStateFlow<VersionSyncState>(VersionSyncState.Idle)
    val versionSyncState: StateFlow<VersionSyncState> = _versionSyncState.asStateFlow()

    private val _lastVersionSyncMillis = MutableStateFlow(0L)
    val lastVersionSyncMillis: StateFlow<Long> = _lastVersionSyncMillis.asStateFlow()

    private val _latestMojang26x = MutableStateFlow<String?>(null)
    val latestMojang26x: StateFlow<String?> = _latestMojang26x.asStateFlow()

    /**
     * Nama kelas WAJIB persis ini: fungsi JNI di libhorizon_android.so terikat pada nama paket dan kelasnya,
     * jadi kelasnya tidak boleh dipindah ke paket launcher.
     */
    const val ACTIVITY_CLASS = "dev.horizon.android.HorizonActivity"

    /** Peringatan RAM kecil; dipakai bersama oleh layar Buat Instance dan saat peluncuran. */
    const val LOW_RAM_WARNING =
        "RAM HP ini terbatas untuk mesin Rust (disarankan 4 GB ke atas). Game bisa tertutup sendiri."

    private const val LIBRARY_FILE = "libhorizon_android.so"
    private const val MIN_SDK = 28

    /** Berkas permintaan peluncuran di folder data mesin ([engineHome]); dibaca lalu dihapus oleh mesin. */
    private const val LAUNCH_FILE = "amber-launch.json"

    // Batas menunggu pembaruan sesi Microsoft sebelum game tetap dijalankan (sama dengan GameLauncher).
    private const val SESSION_REFRESH_TIMEOUT_MS = 12_000L

    // Token Minecraft berlaku 24 jam. Token yang umurnya di bawah batas ini tetap dipakai langsung dan diperbarui di
    // latar untuk peluncuran berikutnya, supaya menu tidak menunggu jaringan sampai 12 detik. Masih tersisa 8 jam
    // lebih untuk masuk server online.
    private const val FAST_START_MAX_TOKEN_AGE_MS = 16L * 60L * 60L * 1000L
    private val scopeErrorHandler = CoroutineExceptionHandler { _, throwable ->
        LauncherLog.e("HorizonEngine", "Kesalahan coroutine HorizonEngine", throwable)
        starting.set(false)
        refreshing.set(false)
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate + scopeErrorHandler)
    private val starting = AtomicBoolean(false)
    private val refreshing = AtomicBoolean(false)

    // Android melaporkan RAM sedikit di bawah angka di kotak HP, jadi HP "4 GB" biasanya terbaca sekitar 3,6 GiB.
    private const val LOW_RAM_BYTES = 3_400L * 1024L * 1024L

    sealed interface Availability {
        /** Siap dijalankan. [lowRam] = RAM HP di bawah yang disarankan: tetap boleh, hanya diberi peringatan. */
        data class Ready(val lowRam: Boolean) : Availability

        /** Tidak bisa dijalankan di build atau HP ini; [reason] ditampilkan apa adanya ke pengguna. */
        data class Unavailable(val reason: String) : Availability
    }

    /** True bila [loader] (nilai `Instance.loader`) adalah mesin Rust. */
    fun isEngine(loader: String?): Boolean = loader?.trim().equals(LOADER_ID, ignoreCase = true)

    /**
     * True bila pustaka mesin ikut terpasang di APK ini. Memeriksa folder pustaka native aplikasi, yang
     * terisi karena build memakai useLegacyPackaging (lihat app/build.gradle.kts). Hanya cek satu berkas,
     * jadi aman dipanggil dari komposisi UI.
     */
    fun isBundled(context: Context): Boolean =
        File(context.applicationInfo.nativeLibraryDir, LIBRARY_FILE).isFile

    fun availability(context: Context): Availability {
        if (Build.VERSION.SDK_INT < MIN_SDK) {
            return Availability.Unavailable("Mesin Rust butuh Android 9 (API 28) ke atas.")
        }
        // Hanya mendukung perangkat dan proses 64-bit (arm64-v8a).
        if (!android.os.Process.is64Bit() || "arm64-v8a" !in Build.SUPPORTED_64_BIT_ABIS) {
            return Availability.Unavailable("Mesin Rust hanya tersedia untuk perangkat 64-bit (arm64-v8a).")
        }
        if (!isBundled(context)) {
            return Availability.Unavailable(
                "Mesin Rust belum disertakan di build ini."
            )
        }
        return Availability.Ready(lowRam = totalRamBytes(context) < LOW_RAM_BYTES)
    }

    /** Folder data mesin: sama dengan getFilesDir() milik HorizonActivity di proses :horizon. */
    internal fun engineHome(context: Context): File = File(context.filesDir, "horizon")

    /** Membandingkan versi seperti "26.3" vs "26.4" vs "26.3.1". */
    fun compareMcVersions(a: String, b: String): Int {
        val pa = a.trim().split('.', '-', '_').map { it.toIntOrNull() }
        val pb = b.trim().split('.', '-', '_').map { it.toIntOrNull() }
        val len = maxOf(pa.size, pb.size)
        for (i in 0 until len) {
            val va = pa.getOrNull(i) ?: 0
            val vb = pb.getOrNull(i) ?: 0
            if (va != vb) return va.compareTo(vb)
        }
        return a.compareTo(b)
    }

    /** Memindai versi game yang sudah terpasang di `.local/horizon-client/`. */
    fun installedClientVersion(context: Context): String? {
        val clientRoot = File(engineHome(context), ".local/horizon-client")
        val dirs = clientRoot.listFiles { f -> f.isDirectory && !f.list().isNullOrEmpty() } ?: return null
        return dirs.map { it.name.trim() }
            .filter { compareMcVersions(it, DEFAULT_MC_VERSION) >= 0 }
            .maxWithOrNull { x, y -> compareMcVersions(x, y) }
    }

    /** Mengembalikan versi aktif Horizon saat ini (dari klien terpasang / cache / live registry). */
    fun resolvedMcVersion(context: Context? = null): String {
        var best = _liveMcVersion.value
        if (context != null) {
            installedClientVersion(context)?.let { instVer ->
                if (compareMcVersions(instVer, best) > 0) best = instVer
            }
            try {
                val cached = File(engineHome(context), VERSION_CACHE_FILE).takeIf { it.isFile }?.readText()?.trim()
                if (!cached.isNullOrBlank() && compareMcVersions(cached, best) > 0) {
                    best = cached
                }
            } catch (_: Exception) {}
            if (best != _liveMcVersion.value) _liveMcVersion.value = best
        }
        return best
    }

    fun updateFromRegistryIndex(context: Context?, index: HorizonRegistry.Index) {
        val regLatest = HorizonRegistry.latestGameVersion(index, DEFAULT_MC_VERSION) ?: return
        var best = resolvedMcVersion(context)
        if (compareMcVersions(regLatest, best) > 0) {
            best = regLatest
            _liveMcVersion.value = best
            if (context != null) persistDetectedVersion(context, best)
        }
        _lastVersionSyncMillis.value = System.currentTimeMillis()
    }

    fun triggerRealtimeSync(context: Context, forceRefresh: Boolean = false) {
        val appCtx = context.applicationContext
        scope.launch {
            refreshLatestVersion(appCtx, forceNetwork = forceRefresh)
        }
    }

    /**
     * Memeriksa pembaruan versi Minecraft (26.3 ke atas) secara real-time untuk Horizon dari:
     * 1) klien Horizon lokal (`.local/horizon-client/`),
     * 2) katalog resmi Horizon (`index.json`), dan
     * 3) manifest resmi Mojang (seri `26.x`).
     */
    suspend fun refreshLatestVersion(context: Context, forceNetwork: Boolean = false): String =
        withContext(Dispatchers.IO) {
            val appCtx = context.applicationContext
            _versionSyncState.value = VersionSyncState.Syncing
            var best = resolvedMcVersion(appCtx)
            var source = if (installedClientVersion(appCtx) != null) "Klien Lokal" else "Baseline $DEFAULT_MC_VERSION"
            var networkSuccess = false
            var lastErrorReason: String? = null

            try {
                val index = NuxModManager.fetchHorizonIndexCached(forceRefresh = forceNetwork)
                if (index != null) {
                    networkSuccess = true
                    val regLatest = HorizonRegistry.latestGameVersion(index, DEFAULT_MC_VERSION)
                    if (regLatest != null && compareMcVersions(regLatest, best) >= 0) {
                        best = regLatest
                        source = "Katalog Horizon"
                    }
                }
            } catch (e: Exception) {
                lastErrorReason = e.message ?: "Gagal memeriksa katalog Horizon"
            }

            try {
                val manifestResult = MojangManifestService.getVersionManifest(forceRefresh = forceNetwork)
                val manifest = manifestResult.getOrNull()
                if (manifest != null) {
                    networkSuccess = true
                    val latest26 = manifest.versions
                        .asSequence()
                        .filter { it.type == "release" && it.id.startsWith("26.") }
                        .map { it.id }
                        .filter { compareMcVersions(it, DEFAULT_MC_VERSION) >= 0 }
                        .maxWithOrNull { a, b -> compareMcVersions(a, b) }
                    _latestMojang26x.value = latest26
                    if (latest26 != null && compareMcVersions(latest26, best) >= 0) {
                        best = latest26
                        source = "Mojang Real-Time"
                    }
                } else if (lastErrorReason == null) {
                    lastErrorReason = manifestResult.exceptionOrNull()?.message
                }
            } catch (e: Exception) {
                if (lastErrorReason == null) lastErrorReason = e.message ?: "Gagal membaca manifest Mojang"
            }

            val now = System.currentTimeMillis()
            _liveMcVersion.value = best
            _lastVersionSyncMillis.value = now
            persistDetectedVersion(appCtx, best)
            _versionSyncState.value = if (networkSuccess) {
                VersionSyncState.Synced(source = source, updatedAtMs = now, currentVersion = best)
            } else {
                VersionSyncState.Error(
                    reason = lastErrorReason ?: "Offline",
                    currentVersion = best
                )
            }
            best
        }

    private fun persistDetectedVersion(context: Context, version: String) {
        try {
            val dir = engineHome(context)
            if (dir.isDirectory || dir.mkdirs()) {
                File(dir, VERSION_CACHE_FILE).writeText(version)
            }
        } catch (_: Exception) {}
    }

    /**
     * True bila sumber daya game sudah terpasang di folder mesin. Bila belum, PLAY pertama mengunduh sekitar
     * 500 MiB lebih dulu.
     */
    fun gameInstalled(context: Context): Boolean {
        val clientRoot = File(engineHome(context), ".local/horizon-client")
        if (File(clientRoot, resolvedMcVersion(context)).isDirectory) return true
        return installedClientVersion(context) != null
    }

    /** Nama pemain untuk mesin: 1 sampai 16 karakter ASCII yang tercetak, tanpa spasi (aturan Minecraft di mesin). */
    private fun validEngineName(name: String): Boolean =
        name.length in 1..16 && name.all { it.code in 33..126 }

    /**
     * Membuka mesin Rust dengan akun yang dipilih di launcher (offline atau Microsoft). Tidak ada JVM. Akun
     * Microsoft yang token-nya sudah lama diperbarui dulu, seperti GameLauncher. Mengembalikan false bila
     * permintaannya ditolak (mesin tidak ada, jenis akun belum didukung); true bila peluncuran dimulai.
     */
    fun launch(context: Context, instance: Instance, account: UserAccount): Boolean =
        launchInternal(context, instance, account, forceDirect = false)

    /** Meluncurkan game di mesin Rust secara langsung tanpa tertahan ke antarmuka internal Horizon. */
    fun launchDirect(context: Context, instance: Instance, account: UserAccount): Boolean =
        launchInternal(context, instance, account, forceDirect = true)

    private fun launchInternal(
        context: Context,
        instance: Instance,
        account: UserAccount,
        forceDirect: Boolean
    ): Boolean {
        val status = availability(context)
        if (status is Availability.Unavailable) {
            Toast.makeText(context, status.reason, Toast.LENGTH_LONG).show()
            return false
        }
        val offlineMode = SettingsManager.settings.value.offlineMode
        val effectiveAccount = AccountManager.resolveLaunchAccount(account, offlineMode)
        val type = effectiveAccount.safeAccountType
        if (type != "offline" && type != "microsoft") {
            Toast.makeText(
                context,
                "Mesin Rust baru mendukung akun Offline dan Microsoft. Pilih salah satunya di tab Akun.",
                Toast.LENGTH_LONG
            ).show()
            return false
        }
        if (type == "offline" && !validEngineName(effectiveAccount.username)) {
            Toast.makeText(
                context,
                "Nama akun offline untuk mesin Rust harus 1 sampai 16 karakter tanpa spasi. Buat akun offline baru di tab Akun.",
                Toast.LENGTH_LONG
            ).show()
            return false
        }
        if (type == "microsoft" && (effectiveAccount.safeAccessToken == "0" || effectiveAccount.uuid.isBlank())) {
            Toast.makeText(
                context,
                "Akun Microsoft ini belum login lengkap. Masuk lagi lewat tab Akun.",
                Toast.LENGTH_LONG
            ).show()
            return false
        }
        if (status is Availability.Ready && status.lowRam) {
            Toast.makeText(context, LOW_RAM_WARNING, Toast.LENGTH_LONG).show()
        }
        // Ketukan ganda pada tombol main tidak boleh memulai dua peluncuran sekaligus.
        if (!starting.compareAndSet(false, true)) {
            LauncherLog.event("Peluncuran", "Ketukan PLAY diabaikan: mesin Rust sedang dibuka")
            return true
        }
        val appContext = context.applicationContext
        val useInternalUi = false
        if (offlineMode || type == "offline" || !MicrosoftAuthService.needsRefresh(effectiveAccount)) {
            if (offlineMode) {
                LauncherLog.event("Peluncuran", "Mode Offline aktif: meluncurkan mesin Rust dengan sesi lokal (${effectiveAccount.username})")
            }
            return start(appContext, instance, effectiveAccount, useInternalUi = useInternalUi)
        }
        val tokenAge = effectiveAccount.tokenIssuedAt?.let { System.currentTimeMillis() - it }
        if (tokenAge != null && tokenAge in 0L..FAST_START_MAX_TOKEN_AGE_MS) {
            LauncherLog.event("Peluncuran", "Token Microsoft masih sah (umur ${tokenAge / 60_000} menit): mesin Rust langsung dibuka, sesi diperbarui di latar")
            refreshInBackground(appContext, effectiveAccount)
            return start(appContext, instance, effectiveAccount, useInternalUi = false)
        }
        Toast.makeText(appContext, "Memperbarui sesi Microsoft...", Toast.LENGTH_SHORT).show()
        scope.launch {
            var launchAccount = effectiveAccount
            var shouldStart = true
            try {
                // Hasil disimpan di dalam tugas ini, jadi tetap tersimpan walau selesai setelah batas waktu.
                val job = async(Dispatchers.IO) {
                    MicrosoftAuthService.refreshMicrosoftAccount(effectiveAccount).also { result ->
                        result.getOrNull()?.let { fresh -> AccountManager.updateAccount(appContext, fresh) }
                    }
                }
                val result = withTimeoutOrNull(SESSION_REFRESH_TIMEOUT_MS) { job.await() }
                val fresh = result?.getOrNull()
                if (fresh != null) {
                    launchAccount = fresh
                    LauncherLog.event("Peluncuran", "Sesi Microsoft diperbarui sebelum membuka mesin Rust")
                } else {
                    LauncherLog.w("HorizonEngine", "Sesi Microsoft tidak diperbarui: ${result?.exceptionOrNull()?.message ?: "melebihi batas waktu"}", result?.exceptionOrNull())
                    Toast.makeText(
                        appContext,
                        "Sesi Microsoft belum bisa diperbarui. Game tetap dijalankan, tetapi fitur online mungkin butuh login ulang.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: CancellationException) {
                shouldStart = false
                starting.set(false)
                throw e
            } catch (e: Exception) {
                LauncherLog.w("HorizonEngine", "Pembaruan sesi Microsoft gagal", e)
            } finally {
                if (shouldStart) {
                    start(appContext, instance, launchAccount, useInternalUi = false)
                } else {
                    starting.set(false)
                }
            }
        }
        return true
    }

    /** Membuka layar permukaan native Horizon (:horizon) secara langsung. */
    fun openNativeSurface(context: Context): Boolean = openInternalUi(context)

    /**
     * Membuka layar peluncur internal bawaan Horizon secara langsung tanpa menulis [LAUNCH_FILE].
     * Setiap berkas peluncuran lama dihapus dulu agar mesin menampilkan antarmuka internal Horizon
     * (kelola profil, dunia, sumber daya, mod Horizon, atau main langsung dari dalam Horizon).
     */
    fun openInternalUi(context: Context): Boolean {
        val status = availability(context)
        if (status is Availability.Unavailable) {
            Toast.makeText(context, status.reason, Toast.LENGTH_LONG).show()
            return false
        }
        if (status is Availability.Ready && status.lowRam) {
            Toast.makeText(context, LOW_RAM_WARNING, Toast.LENGTH_LONG).show()
        }
        if (!starting.compareAndSet(false, true)) {
            LauncherLog.event("Peluncuran", "Ketukan UI Horizon diabaikan: mesin Rust sedang dibuka")
            return true
        }
        val appContext = context.applicationContext
        return try {
            val dir = engineHome(appContext)
            if (!dir.isDirectory && !dir.mkdirs()) {
                throw IOException("Folder data mesin tidak bisa dibuat")
            }
            clearLaunchFile(appContext)
            val s = SettingsManager.settings.value
            RustTouchLayout.sync(appContext, s.rustAmberControls)
            RustSettingsStore.syncTheme(appContext, s.accentColor, s.rustThemePreset)
            LauncherLog.event("Peluncuran", "Membuka antarmuka internal Horizon NovaCraft")
            launchHorizonActivity(appContext)
            true
        } catch (e: Throwable) {
            LauncherLog.e("Peluncuran", "Antarmuka internal Horizon gagal dibuka", e)
            Toast.show(context, "Mesin Rust gagal dibuka: ${e.message ?: e.javaClass.simpleName}", Toast.LENGTH_LONG)
            false
        } finally {
            starting.set(false)
        }
    }

    /** Memperbarui sesi Microsoft tanpa membuat peluncuran menunggu; hasilnya dipakai peluncuran berikutnya. */
    private fun refreshInBackground(context: Context, account: UserAccount) {
        if (!refreshing.compareAndSet(false, true)) return
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    MicrosoftAuthService.refreshMicrosoftAccount(account).getOrNull()
                        ?.let { fresh -> AccountManager.updateAccount(context, fresh) }
                }
            } catch (e: Exception) {
                LauncherLog.w("HorizonEngine", "Pembaruan sesi Microsoft di latar gagal", e)
            } finally {
                refreshing.set(false)
            }
        }
    }

    /** Menulis permintaan peluncuran lalu membuka mesin. Selalu melepas penanda [starting]. */
    private fun start(
        context: Context,
        instance: Instance,
        account: UserAccount,
        useInternalUi: Boolean = false
    ): Boolean {
        return try {
            val firstRun = !gameInstalled(context)
            val s = SettingsManager.settings.value
            // Tombol layar mesin mengikuti tata letak NovaCraft (atau dikembalikan ke bawaan mesin bila dimatikan).
            RustTouchLayout.sync(context, s.rustAmberControls)
            // Warna UI mesin (menu garis tiga, editor kontrol, layar mesin) mengikuti gaya Horizon NovaCraft.
            RustSettingsStore.syncTheme(context, s.accentColor, s.rustThemePreset)
            if (useInternalUi) {
                val dir = engineHome(context)
                if (!dir.isDirectory && !dir.mkdirs()) throw IOException("Folder data mesin tidak bisa dibuat")
                clearLaunchFile(context)
                LauncherLog.event(
                    "Peluncuran",
                    "Membuka antarmuka internal Horizon untuk instance ${instance.name}"
                )
            } else {
                writeLaunchFile(context, account)
                if (firstRun) {
                    Toast.makeText(
                        context,
                        "Pertama kali dibuka: mesin mengunduh sumber daya game (sekitar 500 MiB). Biarkan sampai selesai, game langsung dimulai setelahnya.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                LauncherLog.event(
                    "Peluncuran",
                    "Membuka mesin Rust (Horizon) untuk instance ${instance.name}, akun=${account.safeAccountType}"
                )
            }
            com.aistudio.novacraft.launcher.core.utils.DiscordPresenceManager.onGameLaunching()
            if (s.discordRichPresenceOnPlay) {
                val rustMcVersion = resolvedMcVersion(context).ifBlank { instance.mcVersion }
                com.aistudio.novacraft.launcher.core.utils.DiscordPresenceManager.startGameSession(
                    context = context,
                    mcVersion = rustMcVersion,
                    loader = "Engine Rust",
                    allowServerDisplay = false
                )
            } else {
                com.aistudio.novacraft.launcher.core.utils.DiscordPresenceManager.clearPresence(context)
            }
            launchHorizonActivity(context)
            true
        } catch (e: Throwable) {
            LauncherLog.e("Peluncuran", "Mesin Rust (Horizon) gagal dibuka", e)
            Toast.show(context, "Mesin Rust gagal dibuka: ${e.message ?: e.javaClass.simpleName}", Toast.LENGTH_LONG)
            false
        } finally {
            starting.set(false)
        }
    }

    private fun launchHorizonActivity(context: Context) {
        val intent = Intent()
            .setClassName(context.packageName, ACTIVITY_CLASS)
            // REORDER_TO_FRONT: bila mesin masih berjalan di latar, kembali ke sana, bukan membuka yang kedua.
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        val optionsBundle = try {
            // Tanpa animasi peralihan: layar mesin muncul seketika begitu siap.
            ActivityOptions.makeCustomAnimation(context, 0, 0).toBundle()
        } catch (_: Throwable) {
            null
        }
        if (optionsBundle != null) {
            context.startActivity(intent, optionsBundle)
        } else {
            context.startActivity(intent)
        }
    }

    private fun clearLaunchFile(context: Context) {
        val dir = engineHome(context)
        try {
            File(dir, LAUNCH_FILE).delete()
            File(dir, "$LAUNCH_FILE.tmp").delete()
        } catch (_: Throwable) {
        }
    }

    /**
     * Berkas permintaan untuk mesin: {"version":1,"type":"offline|microsoft","name","uuid","access_token","xuid"}.
     * Ditulis ke berkas sementara lalu dipindah, supaya mesin tidak pernah membaca separuh isinya. Hanya bisa
     * dibaca aplikasi ini; mesin menghapusnya begitu dibaca dan menolaknya bila lebih tua dari dua menit.
     */
    private fun writeLaunchFile(context: Context, account: UserAccount) {
        val dir = engineHome(context)
        if (!dir.isDirectory && !dir.mkdirs()) throw IOException("Folder data mesin tidak bisa dibuat")
        val microsoft = account.safeAccountType == "microsoft"
        val json = JSONObject()
            .put("version", 1)
            .put("type", if (microsoft) "microsoft" else "offline")
            .put("name", account.username)
        if (microsoft) {
            json.put("uuid", account.uuid)
            json.put("access_token", account.safeAccessToken)
            account.xuid?.takeIf { it.isNotBlank() }?.let { json.put("xuid", it) }
        }
        val target = File(dir, LAUNCH_FILE)
        val temp = File(dir, "$LAUNCH_FILE.tmp")
        temp.writeText(json.toString())
        temp.setReadable(false, false)
        temp.setReadable(true, true)
        temp.setWritable(false, false)
        temp.setWritable(true, true)
        if (target.exists()) target.delete()
        if (!temp.renameTo(target)) {
            try {
                temp.copyTo(target, overwrite = true)
                target.setReadable(false, false)
                target.setReadable(true, true)
                target.setWritable(false, false)
                target.setWritable(true, true)
                temp.delete()
            } catch (e: Exception) {
                temp.delete()
                throw IOException("Berkas peluncuran tidak bisa disimpan", e)
            }
        }
    }

    private fun totalRamBytes(context: Context): Long = try {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        ActivityManager.MemoryInfo().also { manager.getMemoryInfo(it) }.totalMem
    } catch (_: Throwable) {
        // Tidak terbaca: jangan memberi peringatan yang belum tentu benar
        Long.MAX_VALUE
    }
}

// ----------------------------------------------------------------------------
// Section: core/horizon/HorizonContent.kt
// ----------------------------------------------------------------------------

/**
 * Shader dan resource pack untuk mesin Rust (Horizon), supaya tempat unduh Amber dan mesin memakai folder dan
 * berkas pengaturan yang SAMA. Tidak ada yang disimpan di tempat lain: apa yang dipasang atau dipilih di Amber
 * langsung terlihat di mesin, dan sebaliknya.
 *
 * Semua fungsi di sini tidak butuh Android (hanya berkas dan JSON), jadi mudah diuji. [home] selalu folder data
 * mesin (HorizonEngine.engineHome); folder game yang sebenarnya dicari lewat [gameHome].
 *
 * Aturan mengikuti mesin (horizon-android: launcher/src/packs.rs dan crates/horizon-client/src/content_files.rs):
 * - Shader  : folder `shaderpacks`, satu pilihan aktif di `horizon-graphics.txt` (baris `pickedShaderPack:<berkas>`).
 * - Resource: folder `resourcepacks`, daftar aktif di `options.txt` (baris `resourcePacks:["vanilla","file/<berkas>"]`,
 *             yang terakhir paling atas).
 * - Baris lain di kedua berkas pengaturan itu ditulis kembali apa adanya.
 * - Berkas pengaturan dibaca mesin saat game dibuka, jadi perubahan berlaku pada permainan berikutnya.
 */
object HorizonContent {

    const val TYPE_SHADERS = "shaderpacks"
    const val TYPE_RESOURCES = "resourcepacks"

    /** Mod (.hmod dan modul bawaan mesin): diurus oleh [HorizonMods]. */
    const val TYPE_MODS = "mods"

    /** Batas ukuran berkas dari mesin (content_files::MAX_INPUT). */
    const val MAX_FILE_BYTES = 256L * 1024L * 1024L
    private const val MAX_EXPANDED_BYTES = 512L * 1024L * 1024L
    private const val MAX_ENTRIES = 32768
    private const val MAX_ENTRY_PATH_BYTES = 1024
    private const val MAX_NAME_BYTES = 180
    private const val MAX_META_BYTES = 1L shl 20

    private const val OPTIONS_FILE = "options.txt"
    private const val GRAPHICS_FILE = "horizon-graphics.txt"
    private const val LAUNCHER_CONFIG = "android-launcher.json"
    private const val KEY_SHADER = "pickedShaderPack"
    private const val KEY_RESOURCES = "resourcePacks"
    private const val KEY_INCOMPATIBLE = "incompatibleResourcePacks"

    private val SHADER_EXTENSIONS = listOf("vsh", "fsh", "glsl", "csh", "gsh", "tcs", "tes")

    /** Shader asli Horizon: zip berekstensi .hshader dengan horizon-shaders.toml di dasar arsip (crate horizon-shader-api). */
    const val NATIVE_SHADER_EXTENSION = ".hshader"
    private const val NATIVE_MANIFEST = "horizon-shaders.toml"
    private const val NATIVE_MAX_FILE_BYTES = 16L * 1024L * 1024L
    private const val NATIVE_MAX_ENTRIES = 512
    private const val NATIVE_MAX_WGSL_BYTES = 256L * 1024L
    private const val NATIVE_MAX_OTHER_BYTES = 1L shl 20
    private const val NATIVE_MAX_TOTAL_BYTES = 8L shl 20

    // Pesan galat ditampilkan apa adanya ke pengguna; semuanya ada di kamus bahasa (mods.tsv).
    const val MSG_TYPE = "Jenis berkas ini belum didukung mesin Rust."
    const val MSG_NAME = "Nama berkas tidak cocok untuk mesin Rust (terlalu panjang atau berisi karakter terlarang)."
    const val MSG_FORMAT = "Mesin Rust hanya menerima berkas .zip di sini."
    const val MSG_TOO_BIG = "Berkas lebih dari 256 MB, terlalu besar untuk mesin Rust."
    const val MSG_BROKEN = "Arsip rusak atau tidak bisa dibaca."
    const val MSG_TOO_MANY = "Arsip berisi terlalu banyak berkas."
    const val MSG_UNSAFE = "Arsip berisi path berbahaya atau ganda, jadi tidak dipasang."
    const val MSG_EXPANDED = "Isi arsip terlalu besar setelah dibuka (lebih dari 512 MB)."
    const val MSG_META = "Metadata resource pack (pack.mcmeta) tidak valid."
    const val MSG_NOT_RESOURCE = "Berkas ini bukan resource pack: pack.mcmeta tidak ditemukan di dasar arsip."
    const val MSG_NOT_SHADER = "Berkas ini bukan shader pack: folder shaders tidak ditemukan di dasar arsip."
    const val MSG_NATIVE = "Ini shader asli Horizon (.hshader). Impor berkas langsung dari Vault konten lokal Horizon."
    const val MSG_NATIVE_MANIFEST = "Ini bukan shader asli Horizon: horizon-shaders.toml tidak ditemukan di dasar arsip."
    const val MSG_NATIVE_BIG = "Shader asli Horizon terlalu besar (berkas lebih dari 16 MB, isi lebih dari 8 MB, atau ada berkas yang terlalu besar)."
    const val MSG_NO_ZIP = "Tidak ada berkas .zip yang cocok di versi ini."
    const val MSG_NO_HASH = "Sumber tidak memberi sidik jari berkas, jadi unduhan tidak bisa diperiksa."
    const val MSG_SAVE = "Gagal menyimpan berkas ke folder mesin Rust."
    const val MSG_OPTIONS =
        "Pengaturan game belum dibuat. Buka game sekali, masuk ke Pengaturan dan ubah satu opsi, lalu coba lagi."
    const val MSG_SELECT = "Gagal menyimpan pilihan ke pengaturan mesin."

    private val gson = GsonBuilder().disableHtmlEscaping().create()

    /** True bila [itemType] (jenis di layar Mod) bisa dipakai mesin Rust. */
    fun supports(itemType: String): Boolean =
        itemType == TYPE_SHADERS || itemType == TYPE_RESOURCES || itemType == TYPE_MODS

    private fun folderOf(itemType: String): String = when (itemType) {
        TYPE_SHADERS -> "shaderpacks"
        TYPE_MODS -> HorizonMods.FOLDER
        else -> "resourcepacks"
    }

    /**
     * Folder game yang dipakai mesin: [home] untuk profil 0 (bawaan), atau profiles/profile-N bila pengguna
     * memilih profil lain di dalam mesin (android-launcher.json, kunci `profile`).
     */
    fun gameHome(home: File): File {
        var profile = 0L
        try {
            val config = File(home, LAUNCHER_CONFIG)
            if (config.isFile && config.length() < 1_000_000L) {
                val root = JsonParser.parseString(config.readText())
                if (root.isJsonObject) {
                    val value = root.asJsonObject.get("profile")
                    if (value != null && value.isJsonPrimitive && value.asJsonPrimitive.isNumber) {
                        profile = value.asLong
                    }
                }
            }
        } catch (_: Exception) {
            profile = 0L
        }
        return if (profile <= 0L) home else File(File(home, "profiles"), "profile-$profile")
    }

    /** Folder tujuan satu jenis berkas di folder game mesin. */
    fun dirFor(home: File, itemType: String): File = File(gameHome(home), folderOf(itemType))

    /** Aturan nama berkas dari mesin (content_files::plain_name): satu nama biasa, bukan path. */
    fun plainName(name: String): Boolean =
        name.isNotEmpty() &&
            name.toByteArray(Charsets.UTF_8).size <= MAX_NAME_BYTES &&
            !name.startsWith(".") &&
            name.none { it == '/' || it == '\\' || it == ':' || it.isISOControl() }

    // ------------------------------------------------------------------------------------------
    // Daftar terpasang
    // ------------------------------------------------------------------------------------------

    /** Berkas shader / resource pack di folder mesin, lengkap dengan status "dipilih" dari berkas pengaturan. */
    fun installed(home: File, itemType: String): List<InstalledModItem> {
        if (itemType == TYPE_MODS) return HorizonMods.installed(home)
        if (!supports(itemType)) return emptyList()
        val files = dirFor(home, itemType).listFiles() ?: return emptyList()
        val on = enabledNames(home, itemType)
        val result = ArrayList<InstalledModItem>()
        for (file in files) {
            if (!file.isFile) continue
            val name = file.name
            if (name.startsWith(".") || name.endsWith(".part")) continue
            val lower = name.lowercase()
            val listed = lower.endsWith(".zip") || (itemType == TYPE_SHADERS && lower.endsWith(".hshader"))
            if (!listed) continue
            result.add(
                InstalledModItem(
                    id = name,
                    filename = name,
                    originalFilename = name,
                    name = file.nameWithoutExtension,
                    description = name,
                    version = "Lokal",
                    category = itemType.replaceFirstChar { it.uppercase() },
                    isEnabled = on.contains(name),
                    sizeBytes = file.length(),
                    itemType = itemType,
                    filePath = file.absolutePath
                )
            )
        }
        return result.sortedBy { it.name.lowercase() }
    }

    /** Nama berkas yang sedang dipilih (shader: paling banyak satu; resource pack: urutan dari bawah). */
    fun enabledNames(home: File, itemType: String): Set<String> {
        val game = gameHome(home)
        return when (itemType) {
            TYPE_SHADERS -> {
                val picked = lineValue(readText(File(game, GRAPHICS_FILE)), KEY_SHADER)?.trim().orEmpty()
                if (picked.isEmpty()) emptySet() else setOf(picked)
            }
            TYPE_RESOURCES -> {
                val ids = parseStringList(lineValue(readText(File(game, OPTIONS_FILE)), KEY_RESOURCES))
                ids.filter { it.startsWith("file/") }.map { it.removePrefix("file/") }.toSet()
            }
            else -> emptySet()
        }
    }

    /** True bila [name] sudah ada di folder tujuan. */
    fun exists(home: File, itemType: String, name: String): Boolean =
        plainName(name) && File(dirFor(home, itemType), name).exists()

    // ------------------------------------------------------------------------------------------
    // Pilih / lepas, hapus
    // ------------------------------------------------------------------------------------------

    /**
     * Memilih ([on] = true) atau melepas satu berkas. Shader: yang baru menggantikan yang lama. Resource pack:
     * yang dihidupkan masuk paling atas. Resource pack butuh options.txt yang sudah dibuat game: berkas itu
     * tidak dibuat dari sini karena tanpa baris versi, mesin menganggapnya berkas versi sangat lama dan
     * mengisi ulang beberapa pengaturan grafik dengan nilai standar yang lebih berat.
     */
    fun setEnabled(home: File, itemType: String, fileName: String, on: Boolean) {
        if (itemType == TYPE_MODS) {
            HorizonMods.setEnabled(home, fileName, on)
            return
        }
        if (!supports(itemType)) throw IOException(MSG_TYPE)
        if (!plainName(fileName)) throw IOException(MSG_NAME)
        val game = gameHome(home)
        try {
            if (itemType == TYPE_SHADERS) {
                setLine(File(game, GRAPHICS_FILE), KEY_SHADER, if (on) fileName else "")
                return
            }
            val options = File(game, OPTIONS_FILE)
            if (!options.isFile) throw IOException(MSG_OPTIONS)
            val text = options.readText()
            val id = "file/$fileName"
            val list = parseStringList(lineValue(text, KEY_RESOURCES))
            list.removeAll { it == id }
            if (list.isEmpty()) list.add("vanilla")
            if (on) {
                list.add(id)
            } else {
                // Jawaban "tetap pakai" dari game untuk pack ini ikut dilepas (sama seperti mesin).
                val incompatible = parseStringList(lineValue(text, KEY_INCOMPATIBLE))
                if (incompatible.contains(id)) {
                    incompatible.removeAll { it == id }
                    setLine(options, KEY_INCOMPATIBLE, toJsonArray(incompatible))
                }
            }
            setLine(options, KEY_RESOURCES, toJsonArray(list))
        } catch (e: IOException) {
            if (e.message == MSG_OPTIONS) throw e
            throw IOException(MSG_SELECT)
        }
    }

    /** Menghapus berkas. Bila sedang dipilih, pilihannya dilepas juga supaya mesin tidak mencari berkas yang hilang. */
    fun delete(home: File, itemType: String, fileName: String): Boolean {
        if (itemType == TYPE_MODS) return HorizonMods.delete(home, fileName)
        if (!supports(itemType) || !plainName(fileName)) return false
        val file = File(dirFor(home, itemType), fileName)
        if (!file.isFile) return false
        val wasOn = enabledNames(home, itemType).contains(fileName)
        if (!file.delete()) return false
        if (wasOn) {
            try {
                setEnabled(home, itemType, fileName, false)
            } catch (_: Exception) {
                // Berkasnya sudah terhapus; pilihan yang tersisa tidak merusak apa pun.
            }
        }
        return true
    }

    // ------------------------------------------------------------------------------------------
    // Pasang berkas (unduhan atau impor): periksa seperti mesin, lalu pindahkan ke folder tujuan
    // ------------------------------------------------------------------------------------------

    /**
     * Memeriksa [staged] (berkas sementara) dengan aturan mesin, lalu memindahkannya ke folder mesin dengan nama
     * [name] (diberi " (2)" dan seterusnya bila sudah dipakai). Berkas sementara tidak dihapus di sini.
     * Mengembalikan nama akhir di folder mesin.
     */
    fun install(staged: File, home: File, itemType: String, name: String): Result<String> {
        if (itemType == TYPE_MODS) return HorizonMods.install(staged, home, name)
        return try {
            if (!supports(itemType)) throw IOException(MSG_TYPE)
            validate(staged, itemType, name)
            val dir = dirFor(home, itemType)
            if (!dir.isDirectory && !dir.mkdirs()) throw IOException(MSG_SAVE)
            val dest = uniqueFile(dir, name)
            if (!plainName(dest.name)) throw IOException(MSG_NAME)
            moveInto(staged, dest)
            Result.success(dest.name)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Menyalin [input] ke [target] dan berhenti dengan galat bila lebih dari [MAX_FILE_BYTES]
     * (berkas yang terlalu besar tidak boleh memenuhi penyimpanan). Berkas parsial dihapus bila gagal.
     */
    fun copyBounded(input: InputStream, target: File) {
        var failed = true
        try {
            target.outputStream().use { output ->
                val buffer = ByteArray(64 * 1024)
                var total = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > MAX_FILE_BYTES) throw IOException(MSG_TOO_BIG)
                    output.write(buffer, 0, read)
                }
            }
            failed = false
        } finally {
            if (failed) target.delete()
        }
    }

    /**
     * Pemeriksaan seperti content_files::validate milik mesin: nama polos, .zip, paling besar 256 MB, paling
     * banyak 32768 entri, path entri aman, total isi paling besar 512 MB, dan isinya cocok jenisnya
     * (resource pack: pack.mcmeta di dasar arsip; shader: berkas di folder shaders/ di dasar arsip).
     * Isi tiap berkas tidak dibaca ulang seluruhnya (hemat waktu di HP lemah); mesin tetap memeriksa saat memuat.
     */
    internal fun validate(file: File, itemType: String, name: String) {
        if (!plainName(name)) throw IOException(MSG_NAME)
        // Shader asli Horizon (.hshader) punya aturan paket sendiri; hanya jenis Shader yang menerimanya
        if (itemType == TYPE_SHADERS && name.lowercase().endsWith(NATIVE_SHADER_EXTENSION)) {
            validateNativeShader(file)
            return
        }
        if (!name.lowercase().endsWith(".zip")) throw IOException(MSG_FORMAT)
        if (file.length() > MAX_FILE_BYTES) throw IOException(MSG_TOO_BIG)

        var hasPack = false
        var hasShader = false
        var hasNative = false
        try {
            ZipFile(file).use { zip ->
                if (zip.size() > MAX_ENTRIES) throw IOException(MSG_TOO_MANY)
                val seen = HashSet<String>()
                var total = 0L
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val raw = entry.name
                    if (isUnsafeEntry(raw) || !seen.add(raw)) throw IOException(MSG_UNSAFE)
                    val size = entry.size
                    if (size < 0L) throw IOException(MSG_BROKEN)
                    total += size
                    if (total > MAX_EXPANDED_BYTES) throw IOException(MSG_EXPANDED)

                    if (raw == "pack.mcmeta") {
                        hasPack = true
                        if (size > MAX_META_BYTES) throw IOException(MSG_META)
                        val text = zip.getInputStream(entry).use { it.readBytes() }.toString(Charsets.UTF_8)
                        val meta = try {
                            JsonParser.parseString(text)
                        } catch (_: Exception) {
                            throw IOException(MSG_META)
                        }
                        val pack = if (meta.isJsonObject) meta.asJsonObject.get("pack") else null
                        if (pack == null || !pack.isJsonObject) throw IOException(MSG_META)
                    }
                    if (raw == "horizon-shaders.toml") hasNative = true
                    if (raw.startsWith("shaders/") && !entry.isDirectory &&
                        SHADER_EXTENSIONS.any { raw.endsWith(".$it") }
                    ) {
                        hasShader = true
                    }
                }
            }
        } catch (e: IOException) {
            // IOException dari pemeriksaan di atas berisi pesan kita; zip rusak dari pustaka diganti pesan umum.
            throw if (isOurMessage(e.message)) e else IOException(MSG_BROKEN)
        } catch (_: Exception) {
            throw IOException(MSG_BROKEN)
        }

        if (itemType == TYPE_RESOURCES) {
            if (!hasPack) throw IOException(MSG_NOT_RESOURCE)
        } else {
            if (hasNative) throw IOException(MSG_NATIVE)
            if (!hasShader) throw IOException(MSG_NOT_SHADER)
        }
    }

    /**
     * Pemeriksaan bentuk paket .hshader seperti horizon_shader_api::Pack::read: zip, paling banyak 512 entri,
     * path entri aman, horizon-shaders.toml di dasar arsip, berkas .wgsl paling besar 256 KB, berkas lain 1 MB,
     * total isi 8 MB. Isi WGSL dan manifestnya sendiri tidak dibaca di sini: katalog dan mesin memeriksanya
     * (mesin menolak paket yang rusak sebelum memuatnya).
     */
    private fun validateNativeShader(file: File) {
        if (file.length() > NATIVE_MAX_FILE_BYTES) throw IOException(MSG_NATIVE_BIG)
        var hasManifest = false
        try {
            ZipFile(file).use { zip ->
                if (zip.size() > NATIVE_MAX_ENTRIES) throw IOException(MSG_TOO_MANY)
                val seen = HashSet<String>()
                var total = 0L
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val raw = entry.name
                    if (isUnsafeEntry(raw) || !seen.add(raw)) throw IOException(MSG_UNSAFE)
                    if (entry.isDirectory) continue
                    val size = entry.size
                    if (size < 0L) throw IOException(MSG_BROKEN)
                    val limit = if (raw.endsWith(".wgsl")) NATIVE_MAX_WGSL_BYTES else NATIVE_MAX_OTHER_BYTES
                    if (size > limit) throw IOException(MSG_NATIVE_BIG)
                    total += size
                    if (total > NATIVE_MAX_TOTAL_BYTES) throw IOException(MSG_NATIVE_BIG)
                    if (raw == NATIVE_MANIFEST) hasManifest = true
                }
            }
        } catch (e: IOException) {
            throw if (isOurMessage(e.message)) e else IOException(MSG_BROKEN)
        } catch (_: Exception) {
            throw IOException(MSG_BROKEN)
        }
        if (!hasManifest) throw IOException(MSG_NATIVE_MANIFEST)
    }

    /** Path entri arsip yang ditolak mesin: kosong, terlalu panjang, berkarakter terlarang, mutlak, atau berisi ".." / ".". */
    private fun isUnsafeEntry(raw: String): Boolean =
        raw.isEmpty() ||
            raw.toByteArray(Charsets.UTF_8).size > MAX_ENTRY_PATH_BYTES ||
            raw.any { it.isISOControl() || it == '\\' || it == ':' } ||
            raw.startsWith("/") ||
            raw.split('/').any { it == ".." || it == "." }

    private fun isOurMessage(message: String?): Boolean = message != null && message in OUR_MESSAGES

    private val OUR_MESSAGES = setOf(
        MSG_TYPE, MSG_NAME, MSG_FORMAT, MSG_TOO_BIG, MSG_BROKEN, MSG_TOO_MANY, MSG_UNSAFE, MSG_EXPANDED,
        MSG_META, MSG_NOT_RESOURCE, MSG_NOT_SHADER, MSG_NATIVE, MSG_NATIVE_MANIFEST, MSG_NATIVE_BIG,
        MSG_NO_ZIP, MSG_NO_HASH, MSG_SAVE
    )

    internal fun uniqueFile(dir: File, name: String): File {
        val first = File(dir, name)
        if (!first.exists()) return first
        val dot = name.lastIndexOf('.')
        val stem = if (dot > 0) name.substring(0, dot) else name
        val ext = if (dot > 0) name.substring(dot) else ""
        for (n in 2..9999) {
            val candidate = File(dir, "$stem ($n)$ext")
            if (!candidate.exists()) return candidate
        }
        throw IOException(MSG_SAVE)
    }

    internal fun moveInto(staged: File, dest: File) {
        if (staged.renameTo(dest)) return
        val part = File(dest.parentFile, dest.name + ".part")
        try {
            staged.inputStream().use { input ->
                part.outputStream().use { output -> input.copyTo(output) }
            }
            if (!part.renameTo(dest)) throw IOException(MSG_SAVE)
        } catch (e: Exception) {
            part.delete()
            throw if (e is IOException) e else IOException(MSG_SAVE)
        }
        staged.delete()
    }

    // ------------------------------------------------------------------------------------------
    // Pilihan versi dari Modrinth
    // ------------------------------------------------------------------------------------------

    /** Berkas .zip yang dipakai dari satu versi (yang utama bila .zip, jika tidak .zip pertama). */
    fun pickFile(version: ModrinthVersion): ModrinthVersionFile? {
        val zips = version.files.filter { it.url.isNotBlank() && it.filename.lowercase().endsWith(".zip") }
        return zips.firstOrNull { it.primary } ?: zips.firstOrNull()
    }

    /**
     * Versi yang bisa dipasang (punya berkas .zip), dengan yang mencantumkan [mcVersion] di depan dan sisanya
     * mengikuti urutan dari Modrinth (terbaru lebih dulu). Sama seperti pilihan mesin sendiri.
     */
    fun usableVersions(all: List<ModrinthVersion>, mcVersion: String): List<ModrinthVersion> {
        val usable = all.filter { pickFile(it) != null }
        val matching = usable.filter { it.gameVersions.contains(mcVersion) }
        val others = usable.filter { !it.gameVersions.contains(mcVersion) }
        return matching + others
    }

    /** True bila berkas membawa sidik jari (sha512 atau sha1) yang bisa dicocokkan. */
    fun hasChecksum(file: ModrinthVersionFile): Boolean {
        val hashes = file.hashes ?: return false
        return !hashes["sha512"].isNullOrBlank() || !hashes["sha1"].isNullOrBlank()
    }

    // ------------------------------------------------------------------------------------------
    // Baca-tulis berkas pengaturan "kunci:nilai" per baris (sama seperti mesin)
    // ------------------------------------------------------------------------------------------

    private fun readText(file: File): String =
        try {
            if (file.isFile) file.readText() else ""
        } catch (_: Exception) {
            ""
        }

    /** Nilai `key:value` pertama di [text]; null bila barisnya tidak ada. */
    internal fun lineValue(text: String, key: String): String? {
        val prefix = "$key:"
        for (line in text.lines()) {
            if (line.startsWith(prefix)) return line.substring(prefix.length)
        }
        return null
    }

    /** Daftar teks JSON `["a","b"]`; kosong bila tidak ada atau bentuknya salah (sama seperti mesin). */
    internal fun parseStringList(value: String?): MutableList<String> {
        val out = ArrayList<String>()
        if (value == null) return out
        try {
            val parsed = JsonParser.parseString(value.trim())
            if (!parsed.isJsonArray) return ArrayList()
            for (element in parsed.asJsonArray) {
                if (!element.isJsonPrimitive || !element.asJsonPrimitive.isString) return ArrayList()
                out.add(element.asString)
            }
        } catch (_: Exception) {
            return ArrayList()
        }
        return out
    }

    internal fun toJsonArray(values: List<String>): String {
        val array = JsonArray()
        for (v in values) array.add(v)
        return gson.toJson(array)
    }

    /**
     * Mengubah `key:value` di [file], atau menambah barisnya; baris lain tetap. Ditulis ke berkas sementara
     * lalu dipindah, jadi mesin tidak pernah membaca berkas yang setengah tertulis.
     */
    internal fun setLine(file: File, key: String, value: String) {
        val text = readText(file)
        val prefix = "$key:"
        var lines = text.lines()
        if (lines.isNotEmpty() && lines.last().isEmpty()) lines = lines.dropLast(1)
        var found = false
        val out = ArrayList<String>(lines.size + 1)
        for (line in lines) {
            if (!found && line.startsWith(prefix)) {
                found = true
                out.add(prefix + value)
            } else {
                out.add(line)
            }
        }
        if (!found) out.add(prefix + value)
        val parent = file.parentFile
        if (parent != null && !parent.isDirectory && !parent.mkdirs()) throw IOException(MSG_SELECT)
        val temp = File(parent, file.name + ".novacraft-tmp")
        try {
            temp.writeText(out.joinToString("\n") + "\n")
            if (!temp.renameTo(file)) {
                if (file.exists()) file.delete()
                if (!temp.renameTo(file)) {
                    temp.copyTo(file, overwrite = true)
                    temp.delete()
                }
            }
        } catch (e: Exception) {
            temp.delete()
            throw if (e is IOException) e else IOException(MSG_SELECT)
        }
    }
}

// ----------------------------------------------------------------------------
// Section: core/horizon/HorizonMods.kt
// ----------------------------------------------------------------------------

/**
 * Mod untuk mesin Rust (Horizon): modul bawaan mesin dan berkas `.hmod`. Mod Java (.jar Fabric / Forge) tidak
 * bisa berjalan di mesin ini; yang bisa adalah mod konten (data dan aset) dan mod berkode WASM milik Horizon.
 *
 * Aturan mengikuti mesin (crates/horizon-mods, horizon-client-render/src/mod_list.rs, content_files.rs):
 * - Berkas ada di `horizon-mods/` di folder game (sebelah options.txt), bernama `<id>.hmod`.
 * - Hidup / mati ada di `horizon-mods.txt`, satu baris `mod.<id>:true` per mod yang hidup. Baris lain di berkas
 *   itu (misalnya pengaturan tombol mod) ditulis kembali apa adanya. Mod yang tidak tercantum dianggap mati.
 * - Empat modul bawaan (zoom, minimap, recipes, smartf3) memakai berkas dan aturan hidup / mati yang sama,
 *   tanpa berkas `.hmod`.
 * - Isi .hmod diperiksa seperti mesin: zip dengan manifest `horizon-mod.toml` di dasar arsip; kode `mod.wasm`
 *   wajib kecuali manifest memberi kemampuan `registries` (mod konten murni).
 *
 * Tanpa Android: hanya berkas dan teks, jadi mudah diuji. [home] selalu folder data mesin.
 */
object HorizonMods {

    const val TYPE = "mods"
    const val FOLDER = "horizon-mods"
    const val LIST_FILE = "horizon-mods.txt"
    const val EXTENSION = ".hmod"

    /** Awalan nama berkas untuk modul bawaan di daftar Terpasang (bukan berkas sungguhan). */
    const val BUILTIN = "builtin:"

    const val MAX_FILE_BYTES = 32L shl 20
    private const val MAX_EXPANDED_BYTES = 64L shl 20
    private const val MAX_WASM_BYTES = 32L shl 20
    private const val MAX_MANIFEST_BYTES = 1L shl 20
    private const val MAX_ENTRIES = 32768
    private const val MAX_ENTRY_PATH_BYTES = 1024
    private const val MANIFEST = "horizon-mod.toml"
    private const val WASM = "mod.wasm"

    // Pesan galat ditampilkan apa adanya ke pengguna; semuanya ada di kamus bahasa (mods.tsv).
    const val MSG_FORMAT = "Mod mesin Rust berformat .hmod, bukan .jar Fabric atau Forge."
    const val MSG_TOO_BIG = "Berkas mod lebih dari 32 MB, terlalu besar untuk mesin Rust."
    const val MSG_EXPANDED = "Isi mod terlalu besar setelah dibuka (lebih dari 64 MB)."
    const val MSG_NO_MANIFEST = "Ini bukan mod Horizon: horizon-mod.toml tidak ada di dasar arsip."
    const val MSG_BAD_MANIFEST = "Manifest mod (horizon-mod.toml) tidak valid atau tidak didukung."
    const val MSG_NO_CODE =
        "Mod ini tidak membawa kode (mod.wasm) dan bukan mod konten (kemampuan registries), jadi tidak bisa dipasang."
    const val MSG_WASM_BIG = "Kode mod (mod.wasm) lebih dari 32 MB."
    const val MSG_FOLDER_CLASH = "Sudah ada mod berbentuk folder dengan id yang sama. Hapus yang lama dulu."
    const val MSG_LIST = "Gagal menyimpan pilihan mod ke pengaturan mesin."
    const val MSG_NEED_64 = "Mod hanya bisa dihidupkan di APK 64-bit."
    const val MSG_NOT_FOUND = "Mod tidak ditemukan atau manifest-nya tidak bisa dibaca."

    private val OUR_MESSAGES = setOf(
        MSG_FORMAT, MSG_TOO_BIG, MSG_EXPANDED, MSG_NO_MANIFEST, MSG_BAD_MANIFEST, MSG_NO_CODE, MSG_WASM_BIG,
        MSG_FOLDER_CLASH, MSG_LIST, MSG_NOT_FOUND,
        HorizonContent.MSG_NAME, HorizonContent.MSG_TOO_MANY, HorizonContent.MSG_UNSAFE, HorizonContent.MSG_SAVE
    )

    /** Modul yang sudah ada di dalam APK mesin (semuanya mati sampai dihidupkan). */
    class Bundled(val id: String, val title: String, val description: String)

    val BUNDLED = listOf(
        Bundled("zoom", "Zoom", "Tahan tombol Z untuk zoom; bisa diatur di Options > Mods dalam game"),
        Bundled("minimap", "Minimap", "Peta kecil yang bisa diatur di sudut layar"),
        Bundled("recipes", "Resep", "Resep dan kegunaan item di samping inventori"),
        Bundled("smartf3", "SmartF3", "Modul F3 yang bisa diatur untuk layar debug")
    )

    /** Isi manifest yang dipakai Amber. [hasCode] = ada mod.wasm di dalam arsip. */
    data class Manifest(
        val id: String,
        val name: String,
        val version: String,
        val description: String,
        val capabilities: List<String>,
        val hasCode: Boolean = false
    )

    // ------------------------------------------------------------------------------------------
    // Daftar terpasang
    // ------------------------------------------------------------------------------------------

    /** Modul bawaan lalu berkas / folder mod di folder mesin, lengkap dengan status hidup dari horizon-mods.txt. */
    fun installed(home: File): List<InstalledModItem> {
        val game = HorizonContent.gameHome(home)
        val state = readList(game)
        val result = ArrayList<InstalledModItem>()
        for (b in BUNDLED) {
            result.add(
                InstalledModItem(
                    id = BUILTIN + b.id,
                    filename = b.id,
                    originalFilename = BUILTIN + b.id,
                    name = b.title,
                    description = b.description,
                    version = "Bawaan",
                    category = "Mod",
                    isEnabled = state.enabled.contains(b.id) && !state.disabled.contains(b.id),
                    sizeBytes = 0L,
                    itemType = TYPE,
                    filePath = ""
                )
            )
        }
        val files = File(game, FOLDER).listFiles() ?: return result
        val found = ArrayList<InstalledModItem>()
        for (file in files) {
            val name = file.name
            if (name.startsWith(".")) continue
            val manifest = when {
                file.isFile && name.endsWith(EXTENSION) -> readManifest(file)
                file.isDirectory && File(file, MANIFEST).isFile -> readFolderManifest(file)
                else -> continue
            }
            val stem = if (file.isFile) name.removeSuffix(EXTENSION) else name
            found.add(
                InstalledModItem(
                    id = name,
                    filename = name,
                    originalFilename = name,
                    name = manifest?.name ?: stem,
                    description = manifest?.description.orEmpty(),
                    version = manifest?.version ?: "Lokal",
                    category = "Mod",
                    isEnabled = manifest != null && state.enabled.contains(manifest.id),
                    sizeBytes = if (file.isFile) file.length() else 0L,
                    itemType = TYPE,
                    filePath = file.absolutePath
                )
            )
        }
        result.addAll(found.sortedBy { it.name.lowercase() })
        return result
    }

    /** True bila [item] boleh dihapus dari Amber: hanya berkas .hmod (modul bawaan dan mod folder tidak). */
    fun removable(item: InstalledModItem): Boolean =
        item.itemType == TYPE && item.filePath.endsWith(EXTENSION)

    // ------------------------------------------------------------------------------------------
    // Hidup / mati dan hapus
    // ------------------------------------------------------------------------------------------

    /** Menghidupkan atau mematikan satu mod ([fileName] dari daftar Terpasang). Berlaku saat game dibuka lagi. */
    fun setEnabled(home: File, fileName: String, on: Boolean) {
        val id = idOf(home, fileName) ?: throw IOException(MSG_NOT_FOUND)
        writeEnabled(HorizonContent.gameHome(home), id, on)
    }

    /** Menghapus berkas .hmod; pilihannya di horizon-mods.txt ikut dilepas. */
    fun delete(home: File, fileName: String): Boolean {
        if (fileName.startsWith(BUILTIN) || !HorizonContent.plainName(fileName) || !fileName.endsWith(EXTENSION)) {
            return false
        }
        val game = HorizonContent.gameHome(home)
        val file = File(File(game, FOLDER), fileName)
        if (!file.isFile) return false
        val id = readManifest(file)?.id
        if (!file.delete()) return false
        if (id != null) {
            try {
                writeEnabled(game, id, false)
            } catch (_: Exception) {
                // Berkasnya sudah terhapus; baris yang tersisa tidak merusak apa pun.
            }
        }
        return true
    }

    /** Id mod untuk satu nama di daftar Terpasang (modul bawaan, berkas .hmod, atau folder mod). */
    internal fun idOf(home: File, fileName: String): String? {
        if (fileName.startsWith(BUILTIN)) {
            val id = fileName.removePrefix(BUILTIN)
            return if (BUNDLED.any { it.id == id }) id else null
        }
        if (!HorizonContent.plainName(fileName)) return null
        val target = File(File(HorizonContent.gameHome(home), FOLDER), fileName)
        return when {
            target.isFile && fileName.endsWith(EXTENSION) -> readManifest(target)?.id
            target.isDirectory -> readFolderManifest(target)?.id
            else -> null
        }
    }

    internal class ListState(val enabled: Set<String>, val disabled: Set<String>)

    private fun readList(game: File): ListState {
        val text = try {
            val file = File(game, LIST_FILE)
            if (file.isFile && file.length() < 1_000_000L) file.readText() else ""
        } catch (_: Exception) {
            ""
        }
        return parseList(text)
    }

    /** Sama seperti ModList::parse milik mesin: `mod.<id>:true` hidup, `mod.<id>:false` mati. */
    internal fun parseList(text: String): ListState {
        val on = LinkedHashSet<String>()
        val off = LinkedHashSet<String>()
        for (line in text.lines()) {
            val colon = line.indexOf(':')
            if (colon < 0) continue
            val key = line.substring(0, colon).trim()
            if (!key.startsWith("mod.")) continue
            val id = key.removePrefix("mod.")
            when (line.substring(colon + 1).trim()) {
                "true" -> on.add(id)
                "false" -> off.add(id)
            }
        }
        return ListState(on, off)
    }

    /** Isi baru horizon-mods.txt: semua baris `mod.<id>:...` diganti satu baris `true` (atau dibuang bila mati). */
    internal fun withEnabled(text: String, id: String, on: Boolean): String {
        var lines = text.lines()
        if (lines.isNotEmpty() && lines.last().isEmpty()) lines = lines.dropLast(1)
        val key = "mod.$id"
        val out = ArrayList<String>(lines.size + 1)
        for (line in lines) {
            val colon = line.indexOf(':')
            if (colon >= 0 && line.substring(0, colon).trim() == key) continue
            out.add(line)
        }
        if (on) out.add("$key:true")
        return if (out.isEmpty()) "" else out.joinToString("\n") + "\n"
    }

    private fun writeEnabled(game: File, id: String, on: Boolean) {
        val file = File(game, LIST_FILE)
        val old = try {
            if (file.isFile) file.readText() else ""
        } catch (_: Exception) {
            throw IOException(MSG_LIST)
        }
        if (!file.exists() && !on) return
        val next = withEnabled(old, id, on)
        if (next == old) return
        val parent = file.parentFile
        if (parent != null && !parent.isDirectory && !parent.mkdirs()) throw IOException(MSG_LIST)
        val temp = File(parent, file.name + ".novacraft-tmp")
        try {
            temp.writeText(next)
            if (!temp.renameTo(file)) {
                if (file.exists()) file.delete()
                if (!temp.renameTo(file)) {
                    temp.copyTo(file, overwrite = true)
                    temp.delete()
                }
            }
        } catch (e: Exception) {
            temp.delete()
            throw if (e is IOException) e else IOException(MSG_LIST)
        }
    }

    // ------------------------------------------------------------------------------------------
    // Pasang berkas (unduhan atau impor)
    // ------------------------------------------------------------------------------------------

    /**
     * Memeriksa [staged] (berkas sementara) seperti mesin, lalu memindahkannya ke `horizon-mods/<id>.hmod`.
     * Mod dengan id yang sama diganti (pembaruan) dan pilihan hidup / matinya dipertahankan. Mod baru yang hanya
     * berisi konten langsung dihidupkan; mod berkode dibiarkan mati sampai pengguna menghidupkannya sendiri.
     * Berkas sementara tidak dihapus di sini. Mengembalikan nama berkas akhir.
     */
    fun install(staged: File, home: File, name: String): Result<String> {
        return try {
            val manifest = inspect(staged, name)
            val game = HorizonContent.gameHome(home)
            val dir = File(game, FOLDER)
            if (!dir.isDirectory && !dir.mkdirs()) throw IOException(HorizonContent.MSG_SAVE)

            val listing = dir.listFiles() ?: emptyArray()
            if (listing.any { it.isDirectory && readFolderManifest(it)?.id == manifest.id }) {
                throw IOException(MSG_FOLDER_CLASH)
            }
            val wanted = File(dir, manifest.id + EXTENSION)
            val sameId = listing.filter {
                it.isFile && it.name.endsWith(EXTENSION) && readManifest(it)?.id == manifest.id
            }
            val dest = if (wanted.exists() && wanted !in sameId) HorizonContent.uniqueFile(dir, wanted.name) else wanted
            if (!HorizonContent.plainName(dest.name)) throw IOException(HorizonContent.MSG_NAME)

            HorizonContent.moveInto(staged, dest)
            for (old in sameId) if (old != dest) old.delete()
            if (sameId.isEmpty() && !manifest.hasCode) {
                try {
                    writeEnabled(game, manifest.id, true)
                } catch (_: Exception) {
                    // Terpasang tetapi belum hidup; pengguna bisa menyalakannya dari daftar.
                }
            }
            Result.success(dest.name)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pemeriksaan seperti content_files::validate dan horizon_mods::Package::read milik mesin: nama polos
     * berakhiran .hmod, paling besar 32 MB, paling banyak 32768 entri, path entri aman, isi paling besar 64 MB,
     * `horizon-mod.toml` di dasar arsip dan sah, `mod.wasm` paling besar 32 MB, dan kode wajib ada kecuali
     * mod konten. Isi kode WASM tidak dibaca di sini; mesin memeriksanya saat memuat.
     */
    internal fun inspect(file: File, name: String): Manifest {
        if (!HorizonContent.plainName(name)) throw IOException(HorizonContent.MSG_NAME)
        if (!name.lowercase().endsWith(EXTENSION)) throw IOException(MSG_FORMAT)
        if (file.length() > MAX_FILE_BYTES) throw IOException(MSG_TOO_BIG)

        var manifestText: String? = null
        var hasCode = false
        try {
            ZipFile(file).use { zip ->
                if (zip.size() > MAX_ENTRIES) throw IOException(HorizonContent.MSG_TOO_MANY)
                val seen = HashSet<String>()
                var total = 0L
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val raw = entry.name
                    if (raw.isEmpty() ||
                        raw.toByteArray(Charsets.UTF_8).size > MAX_ENTRY_PATH_BYTES ||
                        raw.any { it.isISOControl() || it == '\\' || it == ':' } ||
                        raw.startsWith("/") ||
                        raw.split('/').any { it == ".." || it == "." } ||
                        !seen.add(raw)
                    ) {
                        throw IOException(HorizonContent.MSG_UNSAFE)
                    }
                    val size = entry.size
                    if (size < 0L) throw IOException(HorizonContent.MSG_BROKEN)
                    total += size
                    if (total > MAX_EXPANDED_BYTES) throw IOException(MSG_EXPANDED)
                    if (raw == WASM) {
                        hasCode = true
                        if (size > MAX_WASM_BYTES) throw IOException(MSG_WASM_BIG)
                    }
                    if (raw == MANIFEST) {
                        if (size > MAX_MANIFEST_BYTES) throw IOException(MSG_BAD_MANIFEST)
                        manifestText = decodeUtf8(zip.getInputStream(entry).use { it.readBytes() })
                    }
                }
            }
        } catch (e: IOException) {
            // IOException dari pemeriksaan di atas berisi pesan kita; zip rusak dari pustaka diganti pesan umum.
            throw if (e.message.let { it != null && it in OUR_MESSAGES }) e else IOException(HorizonContent.MSG_BROKEN)
        } catch (_: Exception) {
            throw IOException(HorizonContent.MSG_BROKEN)
        }

        val text = manifestText ?: throw IOException(MSG_NO_MANIFEST)
        val manifest = parseManifest(text)
        if (!hasCode && "registries" !in manifest.capabilities) throw IOException(MSG_NO_CODE)
        return manifest.copy(hasCode = hasCode)
    }

    /** Manifest dari berkas .hmod yang sudah terpasang, atau null bila tidak bisa dibaca. */
    internal fun readManifest(file: File): Manifest? =
        try {
            ZipFile(file).use { zip ->
                val entry = zip.getEntry(MANIFEST)
                if (entry == null || entry.size < 0L || entry.size > MAX_MANIFEST_BYTES) {
                    null
                } else {
                    parseManifest(decodeUtf8(zip.getInputStream(entry).use { it.readBytes() }))
                        .copy(hasCode = zip.getEntry(WASM) != null)
                }
            }
        } catch (_: Exception) {
            null
        }

    private fun readFolderManifest(dir: File): Manifest? =
        try {
            val file = File(dir, MANIFEST)
            if (!file.isFile || file.length() > MAX_MANIFEST_BYTES) {
                null
            } else {
                parseManifest(decodeUtf8(file.readBytes())).copy(hasCode = File(dir, WASM).isFile)
            }
        } catch (_: Exception) {
            null
        }

    private fun decodeUtf8(bytes: ByteArray): String =
        try {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        } catch (_: Exception) {
            throw IOException(MSG_BAD_MANIFEST)
        }

    // ------------------------------------------------------------------------------------------
    // Manifest (horizon-mod.toml): aturan sama dengan horizon_mods::Manifest::parse
    // ------------------------------------------------------------------------------------------

    private val CAPABILITIES = setOf(
        "game-read", "chat", "hud", "storage", "command", "registries", "ui", "render", "input", "players",
        "world", "recipes"
    )
    private val SERVER_CAPABILITIES = setOf("storage", "players", "world")
    private val EXPERIMENTAL = setOf("ui", "render", "input", "recipes")
    private val RESERVED_NAMESPACES = setOf("minecraft", "c", "horizon")
    private val MANIFEST_KEYS = setOf(
        "id", "name", "version", "description", "authors", "api", "side", "capabilities", "namespaces",
        "experimental"
    )

    internal fun parseManifest(text: String): Manifest {
        val map = parseToml(text)
        if (map.keys.any { it !in MANIFEST_KEYS }) throw IOException(MSG_BAD_MANIFEST)

        fun string(key: String, required: Boolean): String {
            val value = map[key] ?: if (required) throw IOException(MSG_BAD_MANIFEST) else return ""
            return value as? String ?: throw IOException(MSG_BAD_MANIFEST)
        }

        fun list(key: String): List<String> {
            val value = map[key] ?: return emptyList()
            @Suppress("UNCHECKED_CAST")
            return (value as? List<String>) ?: throw IOException(MSG_BAD_MANIFEST)
        }

        val id = string("id", true)
        val name = string("name", true)
        val version = string("version", true)
        val description = string("description", false)
        val api = string("api", true)
        val side = if (map.containsKey("side")) string("side", true) else "client"
        val capabilities = list("capabilities")
        val namespaces = list("namespaces")
        val experimental = list("experimental")
        list("authors")

        val idOk = id.length in 1..64 && id.all { it in 'a'..'z' || it in '0'..'9' || it == '_' }
        if (!idOk) throw IOException(MSG_BAD_MANIFEST)
        if (api != "0.1" && api != "0.2" && api != "0.3") throw IOException(MSG_BAD_MANIFEST)
        if (side != "client" && side != "server" && side != "both") throw IOException(MSG_BAD_MANIFEST)
        if (capabilities.any { it !in CAPABILITIES } || experimental.any { it !in CAPABILITIES }) {
            throw IOException(MSG_BAD_MANIFEST)
        }
        if ("registries" in capabilities && api == "0.1") throw IOException(MSG_BAD_MANIFEST)
        val server = side == "server"
        if (server != (api == "0.3")) throw IOException(MSG_BAD_MANIFEST)
        if (capabilities.any { (it in SERVER_CAPABILITIES) != server && it != "storage" }) {
            throw IOException(MSG_BAD_MANIFEST)
        }
        if (capabilities.any { it in EXPERIMENTAL && it !in experimental }) throw IOException(MSG_BAD_MANIFEST)
        if (namespaces.isNotEmpty() && "registries" !in capabilities) throw IOException(MSG_BAD_MANIFEST)
        for ((i, ns) in namespaces.withIndex()) {
            val charsOk = ns.length in 1..64 &&
                ns.all { it in 'a'..'z' || it in '0'..'9' || it == '_' || it == '.' || it == '-' }
            if (!charsOk || ns in RESERVED_NAMESPACES || ns == id || namespaces.subList(0, i).contains(ns)) {
                throw IOException(MSG_BAD_MANIFEST)
            }
        }
        return Manifest(id, name, version, description, capabilities)
    }

    /**
     * Bagian TOML yang dipakai horizon-mod.toml: `kunci = "teks"` dan `kunci = ["a", "b"]`, dengan komentar,
     * teks satu baris atau tiga tanda kutip, dan larik yang memanjang beberapa baris. Selain itu (tabel, angka,
     * kunci bertitik, kunci dobel) ditolak, sama seperti mesin yang menolak kolom tak dikenal.
     */
    internal fun parseToml(source: String): Map<String, Any> {
        val text = source.removePrefix("\uFEFF")
        val n = text.length
        val out = LinkedHashMap<String, Any>()
        var i = 0

        fun fail(): Nothing = throw IOException(MSG_BAD_MANIFEST)

        fun skipSpaces() {
            while (i < n && (text[i] == ' ' || text[i] == '\t')) i++
        }

        fun skipAll() {
            while (i < n) {
                val c = text[i]
                if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                    i++
                } else if (c == '#') {
                    while (i < n && text[i] != '\n') i++
                } else {
                    break
                }
            }
        }

        fun readEscape(sb: StringBuilder) {
            // text[i] berada tepat setelah tanda '\'
            if (i >= n) fail()
            when (val e = text[i++]) {
                'b' -> sb.append('\b')
                't' -> sb.append('\t')
                'n' -> sb.append('\n')
                'f' -> sb.append('\u000c')
                'r' -> sb.append('\r')
                '"' -> sb.append('"')
                '\\' -> sb.append('\\')
                'u', 'U' -> {
                    val len = if (e == 'u') 4 else 8
                    if (i + len > n) fail()
                    val code = text.substring(i, i + len).toIntOrNull(16) ?: fail()
                    if (!Character.isValidCodePoint(code) || code in 0xD800..0xDFFF) fail()
                    sb.appendCodePoint(code)
                    i += len
                }
                else -> fail()
            }
        }

        fun readString(): String {
            val quote = text[i]
            val triple = text.startsWith("$quote$quote$quote", i)
            val sb = StringBuilder()
            if (triple) {
                i += 3
                if (i < n && text[i] == '\r' && i + 1 < n && text[i + 1] == '\n') i += 2 else if (i < n && text[i] == '\n') i++
                while (true) {
                    if (i >= n) fail()
                    if (text.startsWith("$quote$quote$quote", i)) {
                        i += 3
                        // Tanda kutip ekstra tepat sebelum penutup (paling banyak dua) ikut menjadi isi.
                        var extra = 0
                        while (extra < 2 && i < n && text[i] == quote) {
                            sb.append(quote)
                            i++
                            extra++
                        }
                        return sb.toString()
                    }
                    val c = text[i++]
                    if (c == '\\' && quote == '"') {
                        if (i < n && (text[i] == '\n' || text[i] == '\r' || text[i] == ' ' || text[i] == '\t')) {
                            // Garis miring di akhir baris: buang baris baru dan spasi sesudahnya.
                            var j = i
                            while (j < n && (text[j] == ' ' || text[j] == '\t')) j++
                            if (j < n && (text[j] == '\n' || text[j] == '\r')) {
                                while (j < n && (text[j] == ' ' || text[j] == '\t' || text[j] == '\n' || text[j] == '\r')) j++
                                i = j
                                continue
                            }
                            fail()
                        }
                        readEscape(sb)
                    } else {
                        sb.append(c)
                    }
                }
            }
            i++
            while (true) {
                if (i >= n) fail()
                val c = text[i++]
                when {
                    c == quote -> return sb.toString()
                    c == '\n' || c == '\r' -> fail()
                    c == '\\' && quote == '"' -> readEscape(sb)
                    else -> sb.append(c)
                }
            }
        }

        fun readKey(): String {
            if (i < n && (text[i] == '"' || text[i] == '\'')) return readString()
            val start = i
            while (i < n && (text[i].isLetterOrDigit() && text[i].code < 128 || text[i] == '_' || text[i] == '-')) i++
            if (i == start) fail()
            return text.substring(start, i)
        }

        while (true) {
            skipAll()
            if (i >= n) break
            if (text[i] == '[') fail()
            val key = readKey()
            skipSpaces()
            if (i >= n || text[i] != '=') fail()
            i++
            skipSpaces()
            if (i >= n) fail()
            val value: Any = when (text[i]) {
                '"', '\'' -> readString()
                '[' -> {
                    i++
                    val items = ArrayList<String>()
                    while (true) {
                        skipAll()
                        if (i >= n) fail()
                        if (text[i] == ']') {
                            i++
                            break
                        }
                        if (text[i] != '"' && text[i] != '\'') fail()
                        items.add(readString())
                        skipAll()
                        if (i >= n) fail()
                        if (text[i] == ',') {
                            i++
                        } else if (text[i] != ']') {
                            fail()
                        }
                    }
                    items
                }
                else -> fail()
            }
            if (out.containsKey(key)) fail()
            out[key] = value
            skipSpaces()
            if (i < n && text[i] == '#') while (i < n && text[i] != '\n') i++
            if (i < n && text[i] == '\r') i++
            if (i < n && text[i] != '\n') fail()
        }
        return out
    }
}

// ----------------------------------------------------------------------------
// Section: core/horizon/HorizonRegistry.kt
// ----------------------------------------------------------------------------

/**
 * Katalog Horizon (registry resmi mesin): satu berkas `index.json` yang dibangun CI repo horizon-mods.
 * Aturan mengikuti docs/api/mod-index.md milik mesin (schema 1):
 * - Tiga jenis proyek dibaca di sini: `mod` (berkas .hmod), `shader` (shader asli Horizon, berkas .hshader) dan
 *   `resourcepack` (berkas .zip). Jenis lain (modpack, native-mod, dst.) dilewati.
 * - Setiap berkas wajib: nama polos berakhiran sesuai jenisnya, alamat `https://github.com/horizon-rust/horizon-mods/
 *   releases/download/<tag>/<nama>` (satu-satunya tempat unduh yang diterima mesin, index::FILE_HOST), ukuran, dan
 *   SHA-256 huruf kecil. Yang tidak lengkap dibuang.
 * - Daftar `revoked` (berkas yang ditarik) tidak pernah ditawarkan.
 * - Yang ditawarkan hanya versi untuk versi game mesin, dan hanya yang tidak melewati batas ukuran jenisnya
 *   ([maxBytes]: mod 32 MB karena batas mesin di Android, shader 16 MB, resource pack 64 MB sesuai batas katalog).
 *
 * Tanda tangan minisign tidak diperiksa di Amber; keutuhan dijaga dengan ukuran dan SHA-256 dari katalog
 * (HTTPS), yang dicocokkan lagi sebelum berkas dipasang.
 *
 * Tanpa Android dan tanpa jaringan: [parse] menerima teks JSON yang sudah diunduh, jadi mudah diuji.
 */
object HorizonRegistry {

    const val INDEX_URL = "https://horizon-rust.github.io/horizon-mods/index.json"

    /** Batas ukuran katalog yang dibaca (cegah respons raksasa memenuhi memori HP). */
    const val MAX_INDEX_BYTES = 8L * 1024L * 1024L

    /** Jenis proyek di katalog yang dipakai Amber (nama persis seperti di index.json). */
    const val KIND_MOD = "mod"
    const val KIND_SHADER = "shader"
    const val KIND_RESOURCE_PACK = "resourcepack"

    // Pesan galat ditampilkan apa adanya ke pengguna; semuanya ada di kamus bahasa (horizon.tsv).
    const val MSG_INDEX = "Katalog Horizon tidak bisa dibaca. Coba lagi nanti."
    const val MSG_NEWER = "Katalog Horizon memakai format yang lebih baru. Perbarui NovaCraft."
    const val MSG_NO_FILE = "Tidak ada berkas yang cocok di versi ini."
    const val MSG_HASH = "Katalog tidak memberi sidik jari SHA-256, jadi unduhan tidak bisa diperiksa."
    const val MSG_URL = "Alamat unduhan tidak dikenali, jadi ditolak."
    const val MSG_SIZE = "Ukuran berkas tidak sama dengan yang tertulis di katalog, jadi tidak dipasang."

    /** Jenis katalog untuk satu jenis isi di layar Mod (null bila katalog tidak melayaninya). */
    fun kindFor(itemType: String): String? = when (itemType) {
        HorizonContent.TYPE_MODS -> KIND_MOD
        HorizonContent.TYPE_SHADERS -> KIND_SHADER
        HorizonContent.TYPE_RESOURCES -> KIND_RESOURCE_PACK
        else -> null
    }

    /** Ekstensi berkas tiap jenis, sama dengan yang dipakai mesin (huruf kecil, dengan titik). */
    fun extensionOf(kind: String): String? = when (kind) {
        KIND_MOD -> HorizonMods.EXTENSION
        KIND_SHADER -> HorizonContent.NATIVE_SHADER_EXTENSION
        KIND_RESOURCE_PACK -> ".zip"
        else -> null
    }

    /** Ukuran berkas terbesar yang ditawarkan per jenis. */
    fun maxBytes(kind: String): Long = when (kind) {
        KIND_MOD -> HorizonMods.MAX_FILE_BYTES
        KIND_SHADER -> 16L * 1024L * 1024L
        else -> 64L * 1024L * 1024L
    }

    /** Versi hasil [toVersions] ditandai dengan titik dua di id ("proyek:versi"); id Modrinth tidak pernah memuatnya. */
    fun isRegistryVersion(version: ModrinthVersion): Boolean = version.id.contains(':')

    private val ID_RE = Regex("[a-z0-9_]{1,64}")
    private val SHA256_RE = Regex("[0-9a-f]{64}")
    // Sama dengan index::FILE_HOST milik mesin: hanya rilis repo horizon-mods, tanpa subfolder, query, atau fragmen
    private val URL_RE = Regex(
        "https://github\\.com/horizon-rust/horizon-mods/releases/download/[^/?#\\s]{1,200}/[^/?#\\s]{1,200}"
    )

    class FileInfo(val name: String, val url: String, val size: Long, val sha256: String)

    class VersionInfo(
        val version: String,
        val published: String,
        val changelog: String,
        val side: String,
        val capabilities: List<String>,
        val game: String,
        val file: FileInfo
    )

    class ProjectInfo(
        val id: String,
        val name: String,
        val summary: String,
        val description: String,
        val authors: List<String>,
        val license: String,
        val source: String,
        val icon: String?,
        val downloads: Long,
        val versions: List<VersionInfo>,
        val kind: String = KIND_MOD
    )

    /** Berkas yang ditarik: [sha256] kosong berarti semua versi [id]. */
    class Revoked(val id: String?, val sha256: String?, val reason: String)

    /** Semua proyek yang dikenal (mod, shader, resource pack); id unik di seluruh katalog. */
    class Index(val projects: List<ProjectInfo>, val revoked: List<Revoked>)

    /** Alamat unduhan yang diterima mesin: rilis repo horizon-mods di GitHub, dan nama di ujung alamat sama dengan nama berkasnya. */
    fun acceptedUrl(url: String, name: String): Boolean = URL_RE.matches(url) && url.substringAfterLast('/') == name

    fun isSha256(value: String): Boolean = SHA256_RE.matches(value)

    // ------------------------------------------------------------------------------------------
    // Membaca katalog
    // ------------------------------------------------------------------------------------------

    fun parse(json: String): Index {
        val root = try {
            JsonParser.parseString(json)
        } catch (_: Exception) {
            throw IOException(MSG_INDEX)
        }
        if (!root.isJsonObject) throw IOException(MSG_INDEX)
        val obj = root.asJsonObject
        val schema = obj.long("schema") ?: throw IOException(MSG_INDEX)
        if (schema > 1L) throw IOException(MSG_NEWER)

        val projects = ArrayList<ProjectInfo>()
        val seen = HashSet<String>()
        for (element in obj.array("projects")) {
            val p = element.asObjectOrNull() ?: continue
            val kind = p.string("kind") ?: continue
            if (extensionOf(kind) == null) continue
            val id = p.string("id") ?: continue
            if (!ID_RE.matches(id) || !seen.add(id)) continue
            val versions = p.array("versions").mapNotNull { e -> e.asObjectOrNull()?.let { o -> parseVersion(o, kind) } }
            if (versions.isEmpty()) continue
            val summary = p.string("summary").orEmpty()
            val icon = p.string("icon")?.takeIf { it.startsWith("https://") }
            projects.add(
                ProjectInfo(
                    id = id,
                    name = p.string("name")?.takeIf { it.isNotBlank() } ?: id,
                    summary = summary,
                    description = p.string("description")?.takeIf { it.isNotBlank() } ?: summary,
                    authors = p.array("authors").mapNotNull { if (it.isJsonPrimitive) it.asString else null },
                    license = p.string("license").orEmpty(),
                    source = p.string("source")?.takeIf { it.startsWith("https://") }.orEmpty(),
                    icon = icon,
                    downloads = p.long("downloads") ?: 0L,
                    versions = versions,
                    kind = kind
                )
            )
        }

        val revoked = ArrayList<Revoked>()
        for (element in obj.array("revoked")) {
            val r = element.asObjectOrNull() ?: continue
            val id = r.string("id")
            val sha = r.string("sha256")?.lowercase()
            if (id == null && sha == null) continue
            revoked.add(Revoked(id, sha, r.string("reason").orEmpty()))
        }
        return Index(projects, revoked)
    }

    private fun parseVersion(v: JsonObject, kind: String): VersionInfo? {
        val version = v.string("version")?.takeIf { it.isNotBlank() } ?: return null
        val game = v.string("game")?.takeIf { it.isNotBlank() } ?: return null
        val f = v.get("file")?.asObjectOrNull() ?: return null
        val name = f.string("name") ?: return null
        val url = f.string("url") ?: return null
        val size = f.long("size") ?: return null
        val sha = f.string("sha256") ?: return null
        val extension = extensionOf(kind) ?: return null
        if (!HorizonContent.plainName(name) || !name.endsWith(extension)) return null
        if (!acceptedUrl(url, name)) return null
        if (size <= 0L || size > maxBytes(kind) || !isSha256(sha)) return null
        return VersionInfo(
            version = version,
            published = v.string("published").orEmpty(),
            changelog = v.string("changelog").orEmpty(),
            side = v.string("side") ?: "client",
            capabilities = v.array("capabilities").mapNotNull { if (it.isJsonPrimitive) it.asString else null },
            game = game,
            file = FileInfo(name, url, size, sha)
        )
    }

    // ------------------------------------------------------------------------------------------
    // Menyaring dan mencari
    // ------------------------------------------------------------------------------------------

    private fun isRevoked(index: Index, projectId: String, file: FileInfo): Boolean =
        index.revoked.any { (it.id == null || it.id == projectId) && (it.sha256 == null || it.sha256 == file.sha256) }

    /** Versi game terbaru (>= [minVersion]) yang tersedia di katalog Horizon. */
    fun latestGameVersion(index: Index, minVersion: String = HorizonEngine.DEFAULT_MC_VERSION): String? {
        var best: String? = null
        for (project in index.projects) {
            for (v in project.versions) {
                if (isRevoked(index, project.id, v.file)) continue
                val g = v.game.trim()
                if (HorizonEngine.compareMcVersions(g, minVersion) >= 0) {
                    if (best == null || HorizonEngine.compareMcVersions(g, best) > 0) {
                        best = g
                    }
                }
            }
        }
        return best
    }

    /** Versi yang boleh dipasang untuk [game]: bukan yang ditarik, terbaru lebih dulu (urutan katalog). */
    fun usableVersions(index: Index, project: ProjectInfo, game: String): List<VersionInfo> {
        val valid = project.versions.filter { !isRevoked(index, project.id, it.file) }
        val exact = valid.filter { it.game == game }
        if (exact.isNotEmpty()) return exact
        return valid.filter { HorizonEngine.compareMcVersions(it.game, HorizonEngine.DEFAULT_MC_VERSION) >= 0 }
    }

    /** Satu proyek menurut id; bila [kind] diberikan, proyek dengan jenis lain dianggap tidak ada. */
    fun find(index: Index, id: String, kind: String? = null): ProjectInfo? =
        index.projects.firstOrNull { it.id == id && (kind == null || it.kind == kind) }

    /** Mod konten murni (data dan aset, tanpa kode): aman dihidupkan langsung. */
    fun contentOnly(version: VersionInfo): Boolean = version.capabilities == listOf("registries")

    fun search(
        index: Index,
        kind: String,
        query: String,
        game: String,
        sort: String,
        page: Int,
        limit: Int
    ): ModrinthSearchResponse {
        val tokens = query.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val scored = ArrayList<Pair<Int, ProjectInfo>>()
        for (project in index.projects) {
            if (project.kind != kind) continue
            if (usableVersions(index, project, game).isEmpty()) continue
            val name = project.name.lowercase()
            val haystack = listOf(name, project.id, project.summary.lowercase(), project.authors.joinToString(" ").lowercase())
                .joinToString(" ")
            if (!tokens.all { haystack.contains(it) }) continue
            val score = when {
                tokens.isEmpty() -> 0
                name == query.trim().lowercase() -> 3
                name.startsWith(tokens.first()) -> 2
                tokens.all { name.contains(it) || project.id.contains(it) } -> 1
                else -> 0
            }
            scored.add(score to project)
        }
        fun latest(p: ProjectInfo) = usableVersions(index, p, game).firstOrNull()?.published.orEmpty()
        val sorted = when (sort) {
            "downloads", "follows" -> scored.sortedWith(compareByDescending<Pair<Int, ProjectInfo>> { it.second.downloads }.thenBy { it.second.name.lowercase() })
            "newest", "updated" -> scored.sortedWith(compareByDescending<Pair<Int, ProjectInfo>> { latest(it.second) }.thenBy { it.second.name.lowercase() })
            else -> scored.sortedWith(compareByDescending<Pair<Int, ProjectInfo>> { it.first }.thenByDescending { it.second.downloads }.thenBy { it.second.name.lowercase() })
        }
        val safeLimit = limit.coerceAtLeast(1)
        val offset = ((page - 1).coerceAtLeast(0)) * safeLimit
        val hits = sorted.drop(offset).take(safeLimit).map { toHit(index, it.second, game) }
        return ModrinthSearchResponse(hits = hits, offset = offset, limit = safeLimit, totalHits = sorted.size)
    }

    // ------------------------------------------------------------------------------------------
    // Diubah ke bentuk yang dipakai layar Mod (sama dengan hasil Modrinth)
    // ------------------------------------------------------------------------------------------

    private fun toHit(index: Index, p: ProjectInfo, game: String): ModrinthSearchHit {
        val newest = usableVersions(index, p, game).firstOrNull()
        // Hanya mod yang diberi label (konten murni atau berkode); shader dan resource pack tanpa label
        val tags = if (p.kind != KIND_MOD) emptyList<String>() else listOf(if (newest != null && contentOnly(newest)) "konten" else "berkode")
        return ModrinthSearchHit(
            projectId = p.id,
            projectType = p.kind,
            slug = p.id,
            author = p.authors.firstOrNull(),
            title = p.name,
            description = p.summary,
            categories = tags,
            displayCategories = tags,
            versions = listOf(game),
            downloads = p.downloads,
            iconUrl = p.icon
        )
    }

    fun toProject(p: ProjectInfo, game: String): ModrinthProject {
        val extra = ArrayList<String>()
        if (p.license.isNotBlank()) extra.add(p.license)
        if (p.authors.isNotEmpty()) extra.add(p.authors.joinToString(", "))
        val body = (if (extra.isEmpty()) "" else extra.joinToString(" \u2022 ") + "\n\n") + p.description
        return ModrinthProject(
            id = p.id,
            slug = p.id,
            title = p.name,
            description = p.summary,
            body = body,
            categories = null,
            loaders = null,
            gameVersions = listOf(game),
            downloads = p.downloads,
            iconUrl = p.icon,
            gallery = null
        )
    }

    fun toVersions(index: Index, p: ProjectInfo, game: String): List<ModrinthVersion> =
        usableVersions(index, p, game).map { v ->
            ModrinthVersion(
                id = p.id + ":" + v.version,
                projectId = p.id,
                name = v.version,
                versionNumber = v.version,
                gameVersions = listOf(v.game),
                loaders = emptyList(),
                versionType = "release",
                files = listOf(
                    ModrinthVersionFile(
                        url = v.file.url,
                        filename = v.file.name,
                        primary = true,
                        size = v.file.size,
                        hashes = mapOf("sha256" to v.file.sha256)
                    )
                ),
                dependencies = null
            )
        }

    // ------------------------------------------------------------------------------------------
    // Pembantu JSON yang longgar (kolom tak dikenal dan tipe yang salah diabaikan, bukan galat)
    // ------------------------------------------------------------------------------------------

    private fun JsonElement.asObjectOrNull(): JsonObject? = if (isJsonObject) asJsonObject else null

    private fun JsonObject.string(key: String): String? {
        val e = get(key) ?: return null
        return if (e.isJsonPrimitive && e.asJsonPrimitive.isString) e.asString else null
    }

    private fun JsonObject.long(key: String): Long? {
        val e = get(key) ?: return null
        return if (e.isJsonPrimitive && e.asJsonPrimitive.isNumber) {
            try {
                e.asLong
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    }

    private fun JsonObject.array(key: String): List<JsonElement> {
        val e = get(key) ?: return emptyList()
        return if (e.isJsonArray) e.asJsonArray.toList() else emptyList()
    }
}

// ----------------------------------------------------------------------------
// Section: core/horizon/NovaHorizonConfigCore.kt
// ----------------------------------------------------------------------------

// ============================================================================
// NovaCraft Unified Horizon Config Core: RustTheme, RustSettings & RustPerformanceReport
// ============================================================================

internal object RustTheme {
    const val PRESET_OBSIDIAN = "nova_obsidian"
    const val PRESET_AURORA = "nova_aurora"
    const val PRESET_NEBULA = "nova_nebula"
    const val PRESET_SOLAR = "nova_solar"

    data class HorizonUiPreset(
        val id: String,
        val label: String,
        val bgHex: String,
        val surfaceHex: String,
        val accentTint: Int
    )

    val PRESETS = listOf(
        HorizonUiPreset(
            id = PRESET_OBSIDIAN,
            label = "NovaCraft Obsidian",
            bgHex = "#060B1D",
            surfaceHex = "#0E1836",
            accentTint = Color(0xFF38BDF8).toArgb()
        ),
        HorizonUiPreset(
            id = PRESET_AURORA,
            label = "Aurora Teal",
            bgHex = "#041418",
            surfaceHex = "#0B262E",
            accentTint = Color(0xFF2DD4BF).toArgb()
        ),
        HorizonUiPreset(
            id = PRESET_NEBULA,
            label = "Nebula Cosmic",
            bgHex = "#0E071E",
            surfaceHex = "#1B113B",
            accentTint = Color(0xFFC084FC).toArgb()
        ),
        HorizonUiPreset(
            id = PRESET_SOLAR,
            label = "Solar Forge",
            bgHex = "#140B06",
            surfaceHex = "#24150D",
            accentTint = Color(0xFFFBBF24).toArgb()
        )
    )

    private const val SOFT_SHARE = 0.45f

    fun presetFor(presetId: String?): HorizonUiPreset =
        PRESETS.firstOrNull { it.id == presetId } ?: PRESETS.first()

    fun json(accentId: String?, presetId: String? = PRESET_OBSIDIAN): JSONObject {
        val preset = presetFor(presetId)
        val accent = AMBER_ACCENTS.firstOrNull { it.id == accentId } ?: AMBER_ACCENTS.first()
        return JSONObject()
            .put("preset", "custom")
            .put("appearance", "dark")
            .put("background_mode", "solid")
            .put("custom_bg_hex", preset.bgHex)
            .put("custom_surface_hex", preset.surfaceHex)
            .put("custom_accent_hex", accentHex(accent, preset))
    }

    internal fun accentHex(accent: AmberAccent, preset: HorizonUiPreset = PRESETS.first()): String {
        val base = accent.base.toArgb()
        val soft = accent.soft.toArgb()
        val themeTint = preset.accentTint
        fun channel(shift: Int): Int {
            val a = (base shr shift) and 0xFF
            val b = (soft shr shift) and 0xFF
            val t = (themeTint shr shift) and 0xFF
            val launcherMix = a + (b - a) * SOFT_SHARE
            return (launcherMix * 0.45f + t * 0.55f).toInt().coerceIn(0, 255)
        }
        return "#%02X%02X%02X".format(channel(16), channel(8), channel(0))
    }
}

data class RustSettings(
    val renderer: String = RustSettingsStore.RENDERER_COMPAT,
    val renderScale: Int = 100,
    val workerLimit: Int = 0,
    val cacheBudgetMib: Int = 0,
    val displayHz: Int = 0,
    val performanceHints: Boolean = false,
    val inputHost: String = "auto",
    val orientation: String = "landscape",
    val immersive: Boolean = false,
    val avoidCutout: Boolean = false,
    val aspect: String = "native",
    val server: String = "",
    val perfMode: String = RustSettingsStore.PERF_NORMAL,
    val gameAnimationsOff: Boolean = false,
    val fastChunkUpload: Boolean = true,
    val leafCulling: Boolean = false,
    val recordPerformance: Boolean = false
)

object RustSettingsStore {
    const val RENDERER_COMPAT = "vulkan-compatibility"

    private const val TAG = "RustSettings"
    private const val CONFIG_FILE = "android-launcher.json"
    private const val TEMP_FILE = "android-launcher.novacraft-tmp"

    val RENDERERS = setOf(RENDERER_COMPAT, "vulkan", "open-gl")
    val INPUT_HOSTS = setOf("auto", "touch", "keyboard-mouse", "controller")
    val ORIENTATIONS = setOf("landscape", "portrait", "reverse-landscape", "reverse-portrait", "auto")
    val ASPECTS = setOf("native", "wide", "classic", "square")

    const val PERF_NORMAL = "normal"
    const val PERF_SUPER = "super"
    const val PERF_BATTERY = "battery"
    const val PERF_FPS = "fps"
    val PERF_MODES = setOf(PERF_NORMAL, PERF_SUPER, PERF_BATTERY, PERF_FPS)

    val DISPLAY_RATES = listOf(0, 30, 60, 90, 120, 144, 165)
    val RENDER_SCALE_PRESETS = listOf(100, 85, 75, 67, 50, 25)
    val CACHE_BUDGETS = listOf(0, 16, 32, 64, 128, 256)

    const val MAX_WORKERS = 16
    private const val MAX_SERVER_CHARS = 260

    private val state = MutableStateFlow(RustSettings())
    val settings: StateFlow<RustSettings> = state.asStateFlow()

    private val io = Executors.newSingleThreadExecutor { task ->
        Thread(task, "rust-settings").apply { isDaemon = true }
    }

    private val revision = AtomicInteger(0)
    private val lock = Any()
    private val fileLock = Any()

    private fun configFile(context: Context): File = File(HorizonEngine.engineHome(context), CONFIG_FILE)

    fun reload(context: Context) {
        val file = configFile(context.applicationContext)
        val startedAt = revision.get()
        io.execute {
            val loaded = try {
                parse(readJson(file))
            } catch (e: Exception) {
                LauncherLog.w(TAG, "Config mesin Rust tidak terbaca, memakai bawaan: ${e.message}", e)
                RustSettings()
            }
            synchronized(lock) {
                if (revision.get() == startedAt) state.value = loaded
            }
        }
    }

    fun update(context: Context, change: (RustSettings) -> RustSettings) {
        val file = configFile(context.applicationContext)
        val next = synchronized(lock) {
            val candidate = sanitize(change(state.value))
            if (candidate == state.value) return
            revision.incrementAndGet()
            state.value = candidate
            candidate
        }
        io.execute { write(file, next, reset = false) }
    }

    fun reset(context: Context) {
        val file = configFile(context.applicationContext)
        synchronized(lock) {
            revision.incrementAndGet()
            state.value = RustSettings()
        }
        io.execute { write(file, RustSettings(), reset = true) }
    }

    private fun readJson(file: File): JSONObject {
        if (!file.isFile) return JSONObject()
        return try {
            JSONObject(file.readText())
        } catch (e: Exception) {
            LauncherLog.w(TAG, "android-launcher.json rusak, dianggap kosong: ${e.message}")
            JSONObject()
        }
    }

    private fun parse(json: JSONObject): RustSettings {
        val defaults = RustSettings()
        return sanitize(
            RustSettings(
                renderer = json.optString("renderer", defaults.renderer),
                renderScale = json.optInt("render_scale", defaults.renderScale),
                workerLimit = json.optInt("worker_limit", defaults.workerLimit),
                cacheBudgetMib = json.optInt("cache_budget_mib", defaults.cacheBudgetMib),
                displayHz = json.optInt("display_hz", defaults.displayHz),
                performanceHints = json.optBoolean("performance_hints", defaults.performanceHints),
                inputHost = json.optString("input_host", defaults.inputHost),
                orientation = json.optString("orientation", defaults.orientation),
                immersive = json.optBoolean("immersive", defaults.immersive),
                avoidCutout = json.optBoolean("avoid_cutout", defaults.avoidCutout),
                aspect = json.optString("aspect", defaults.aspect),
                server = if (json.isNull("server")) "" else json.optString("server", ""),
                perfMode = when {
                    json.optBoolean("super_perf_mode", false) -> PERF_SUPER
                    json.optBoolean("battery_perf_mode", false) -> PERF_BATTERY
                    json.optBoolean("max_fps_mode", false) -> PERF_FPS
                    else -> PERF_NORMAL
                },
                gameAnimationsOff = json.optBoolean("disable_game_animations", defaults.gameAnimationsOff),
                fastChunkUpload = json.optBoolean("fast_chunk_upload", defaults.fastChunkUpload),
                leafCulling = json.optBoolean("experimental_leaf_culling", defaults.leafCulling),
                recordPerformance = json.optBoolean("record_performance", defaults.recordPerformance)
            )
        )
    }

    private fun sanitize(s: RustSettings): RustSettings {
        val defaults = RustSettings()
        return s.copy(
            renderer = if (s.renderer in RENDERERS) s.renderer else defaults.renderer,
            renderScale = s.renderScale.coerceIn(25, 100),
            workerLimit = s.workerLimit.coerceIn(0, 64),
            cacheBudgetMib = s.cacheBudgetMib.coerceIn(0, 1024),
            displayHz = if (s.displayHz in DISPLAY_RATES) s.displayHz else 0,
            inputHost = if (s.inputHost in INPUT_HOSTS) s.inputHost else defaults.inputHost,
            orientation = if (s.orientation in ORIENTATIONS) s.orientation else defaults.orientation,
            aspect = if (s.aspect in ASPECTS) s.aspect else defaults.aspect,
            perfMode = if (s.perfMode in PERF_MODES) s.perfMode else defaults.perfMode,
            server = cleanServer(s.server)
        )
    }

    fun cleanServer(raw: String): String =
        raw.filter { !it.isWhitespace() && it.code >= 32 }.take(MAX_SERVER_CHARS)

    private fun write(file: File, s: RustSettings, reset: Boolean) =
        synchronized(fileLock) { writeLocked(file, s, reset) }

    private fun writeLocked(file: File, s: RustSettings, reset: Boolean) {
        try {
            val dir = file.parentFile ?: return
            if (!dir.isDirectory && !dir.mkdirs()) {
                LauncherLog.w(TAG, "Folder data mesin tidak bisa dibuat: $dir")
                return
            }
            if (reset && !file.isFile) return
            val json = readJson(file)
            if (reset) {
                for (key in OWN_KEYS) json.remove(key)
            } else {
                json.put("renderer", s.renderer)
                json.put("render_scale", s.renderScale)
                json.put("worker_limit", s.workerLimit)
                json.put("cache_budget_mib", s.cacheBudgetMib)
                json.put("display_hz", s.displayHz)
                json.put("performance_hints", s.performanceHints)
                json.put("input_host", s.inputHost)
                json.put("orientation", s.orientation)
                json.put("immersive", s.immersive)
                json.put("avoid_cutout", s.avoidCutout)
                json.put("aspect", s.aspect)
                json.put("server", s.server)
                json.put("super_perf_mode", s.perfMode == PERF_SUPER)
                json.put("battery_perf_mode", s.perfMode == PERF_BATTERY)
                json.put("max_fps_mode", s.perfMode == PERF_FPS)
                json.put("disable_game_animations", s.gameAnimationsOff)
                json.put("fast_chunk_upload", s.fastChunkUpload)
                json.put("experimental_leaf_culling", s.leafCulling)
                json.put("record_performance", s.recordPerformance)
            }
            commit(dir, file, json)
        } catch (e: Exception) {
            LauncherLog.w(TAG, "Config mesin Rust gagal ditulis: ${e.message}", e)
        }
    }

    private fun commit(dir: File, file: File, json: JSONObject) {
        val temp = File(dir, TEMP_FILE)
        temp.writeText(json.toString(2))
        if (!temp.renameTo(file)) {
            if (file.exists()) file.delete()
            if (!temp.renameTo(file)) {
                temp.delete()
                LauncherLog.w(TAG, "Config mesin Rust tidak bisa disimpan: $file")
            }
        }
    }

    fun syncTheme(context: Context, accentId: String?, presetId: String? = null) {
        try {
            val file = configFile(context.applicationContext)
            val dir = file.parentFile ?: return
            if (!dir.isDirectory && !dir.mkdirs()) return
            synchronized(fileLock) {
                val json = readJson(file)
                val theme = RustTheme.json(accentId, presetId)
                val current = json.optJSONObject("theme")
                if (current != null && theme.keys().asSequence().all { current.opt(it) == theme.opt(it) }) return
                json.put("theme", theme)
                commit(dir, file, json)
            }
        } catch (e: Exception) {
            LauncherLog.w(TAG, "Tema mesin Rust gagal diselaraskan: ${e.message}", e)
        }
    }

    private val OWN_KEYS = listOf(
        "renderer", "render_scale", "worker_limit", "cache_budget_mib", "display_hz", "performance_hints",
        "input_host", "orientation", "immersive", "avoid_cutout", "aspect", "server",
        "super_perf_mode", "battery_perf_mode", "max_fps_mode", "disable_game_animations",
        "fast_chunk_upload", "experimental_leaf_culling", "record_performance"
    )
}

object RustPerformanceReport {
    private const val TAG = "RustPerformance"
    private const val FOLDER = "performance"
    private const val PREFIX = "horizon-performance-"
    private const val EXTENSION = ".jsonl"
    private const val SHARE_FOLDER = "shared_logs"

    private const val MSG_NONE = "Belum ada laporan performa. Nyalakan Rekam performa, mainkan sebentar, lalu coba lagi."
    private const val MSG_NO_APP = "Tidak ada aplikasi untuk membagikan file ini."
    private const val MSG_FAILED = "Gagal membagikan file: "
    private const val CHOOSER_TITLE = "Bagikan laporan performa"
    private const val SUBJECT = "Laporan performa NovaCraft - "

    internal fun latestIn(dir: File): File? =
        dir.listFiles { f -> f.isFile && f.length() > 0L && f.name.startsWith(PREFIX) && f.name.endsWith(EXTENSION) }
            ?.maxByOrNull { it.name }

    fun latest(context: Context): File? = latestIn(File(HorizonEngine.engineHome(context), FOLDER))

    fun share(context: Context) {
        val app = context.applicationContext
        val worker = Thread({
            var copy: File? = null
            var error: String? = null
            try {
                copy = prepareCopy(app)
            } catch (e: Exception) {
                LauncherLog.w(TAG, "Laporan performa gagal disiapkan: ${e.message}", e)
                error = e.message ?: e.javaClass.simpleName
            }
            Handler(Looper.getMainLooper()).post { deliver(app, copy, error) }
        }, "rust-report-share")
        worker.isDaemon = true
        worker.start()
    }

    private fun prepareCopy(context: Context): File? {
        val report = latest(context) ?: return null
        val shareDir = File(context.cacheDir, SHARE_FOLDER).apply { mkdirs() }
        shareDir.listFiles { f -> f.name.startsWith(PREFIX) }?.forEach { it.delete() }
        val copy = File(shareDir, report.name)
        report.useLines(Charsets.UTF_8) { lines ->
            copy.bufferedWriter(Charsets.UTF_8).use { writer ->
                for (line in lines) {
                    writer.write(LogRedactor.redactLine(line))
                    writer.newLine()
                }
            }
        }
        return copy
    }

    private fun deliver(context: Context, copy: File?, error: String?) {
        if (copy == null) {
            val message = if (error != null) MSG_FAILED + error else MSG_NONE
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", copy)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, SUBJECT + copy.name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(send, Tr.t(CHOOSER_TITLE)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, MSG_NO_APP, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, MSG_FAILED + (e.message ?: e.javaClass.simpleName), Toast.LENGTH_SHORT).show()
        }
    }
}

// ----------------------------------------------------------------------------
// Section: core/horizon/RustTouchLayout.kt
// ----------------------------------------------------------------------------

/**
 * Membuat kontrol layar mesin Rust memakai tata letak DAN tampilan kontrol Amber, tanpa mengubah mesin dan tanpa
 * lapisan tambahan di atasnya.
 *
 * Mesin (Horizon 0.7.8 ke atas) punya gaya kontrol "GoyControl": joystick dan tombol statis yang bisa diatur
 * posisi, ukuran, warna, bentuk, dan labelnya lewat `horizon-touch.json` di folder data mesin. Di sini tata letak
 * Amber diterjemahkan ke gaya itu sebelum mesin dibuka (saat PLAY), jadi hanya ada satu set kontrol:
 *
 * - Joystick Amber menjadi joystick mesin (tetap di tempatnya, lari otomatis mengikuti pilihan Amber).
 * - Setiap tombol keyboard/mouse Amber menjadi tombol mesin di posisi dan ukuran yang sama, dengan label, sudut
 *   membulat, kaca gelap, garis putih tipis, dan warna kuning saat ditekan seperti tombol Amber. Pilihan tombol
 *   tahan/kunci (toggle), kombinasi tombol, dan kapan tampil (main/menu/selalu) ikut dibawa.
 * - Widget gulir hotbar menjadi dua tombol (naik/turun). Tombol sistem HIDE GUI menjadi F1 dan MENU menjadi ESC
 *   (mesin tidak punya menu Amber); FPS, keyboard, dan makro perintah/turbo tidak punya padanan dan dilewati.
 * - Geser dari tombol serang/pakai/lompat hanya ikut memutar kamera bila pilihan itu menyala di Pengaturan Amber.
 *
 * Hanya kunci yang diperlukan yang ditulis; kunci lain di berkas itu (sensitivitas, giroskop, dan seterusnya)
 * dibiarkan. Berkas aslinya disalin dulu ke `horizon-touch.amber-backup.json` dan dipulihkan saat pilihan ini
 * dimatikan. Tidak ada pekerjaan selain satu kali tulis berkas kecil per PLAY, dan setiap kegagalan hanya dicatat
 * (game tetap dibuka).
 */
object RustTouchLayout {
    private const val TAG = "RustTouchLayout"
    private const val CONFIG_FILE = "android-launcher.json"
    private const val TOUCH_FILE = "horizon-touch.json"
    private const val BACKUP_FILE = "horizon-touch.novacraft-backup.json"
    private const val LEGACY_BACKUP_FILE = "horizon-touch.amber-backup.json"
    private const val TEMP_FILE = "horizon-touch.novacraft-tmp"

    private const val CONTROLS = 14 // jumlah kontrol bawaan mesin (enum Control)
    private const val MOVE = 0 // indeks kontrol gerak (joystick) di mesin
    private const val MAX_CUSTOM = 32 // batas tombol kustom mesin

    /** Tinggi ruang UI mesin dalam satuan "dp mesin" (mesin menargetkan 410..440): dasar skala sudut dan garis. */
    private const val ENGINE_UI_HEIGHT = 430f

    /** Tata letak bawaan mesin (x, y, ukuran) untuk 14 kontrol; hanya pengisi karena gaya Goy memakai tombol kustom. */
    private val FILLER = arrayOf(
        floatArrayOf(0.13f, 0.76f, 0.32f),
        floatArrayOf(0.72f, 0.78f, 0.105f),
        floatArrayOf(0.92f, 0.78f, 0.16f),
        floatArrayOf(0.81f, 0.86f, 0.115f),
        floatArrayOf(0.80f, 0.64f, 0.16f),
        floatArrayOf(0.92f, 0.52f, 0.16f),
        floatArrayOf(0.46f, 0.055f, 0.075f),
        floatArrayOf(0.50f, 0.055f, 0.075f),
        floatArrayOf(0.54f, 0.055f, 0.075f),
        floatArrayOf(0.73f, 0.95f, 0.075f),
        floatArrayOf(0.42f, 0.055f, 0.075f),
        floatArrayOf(0.58f, 0.055f, 0.075f),
        floatArrayOf(0.80f, 0.96f, 0.075f),
        floatArrayOf(0.92f, 0.36f, 0.12f)
    )

    // Warna tampilan NovaCraft Cosmic Cyber-Glass.
    private const val NOVA_CYAN = 0x38BDF8
    private const val NOVA_CYAN_SOFT = 0x7DD3FC
    private const val ICE_WHITE = 0xE0F2FE
    private const val COSMIC_GLASS = 0x0A1228

    /** Posisi dan ukuran satu elemen: pusat (0..1 dari layar), ukuran dasar sebagai pecahan tinggi layar, rasio lebar/tinggi. */
    private class Place(val x: Float, val y: Float, val size: Float, val wr: Float, val hr: Float)

    private class Joy(val place: Place, val opacity: Float, val autoSprint: Boolean, val inGame: Boolean, val inMenu: Boolean)

    private class Btn(
        val label: String,
        val keys: List<String>,
        val place: Place,
        val opacity: Float,
        val primary: Boolean,
        val cornerDp: Float,
        val toggle: Boolean,
        val passthrough: Boolean,
        val inGame: Boolean,
        val inMenu: Boolean
    )

    private class Plan(val joy: Joy?, val buttons: List<Btn>, val scale: Float)

    /**
     * Dipanggil tepat sebelum mesin dibuka. [enabled] = pilihan "Kontrol Amber" di Pengaturan Rust. Tidak pernah
     * melempar: kegagalan hanya dicatat dan mesin memakai kontrol bawaannya.
     */
    fun sync(context: Context, enabled: Boolean) {
        try {
            apply(context.applicationContext, enabled)
        } catch (e: Throwable) {
            LauncherLog.w(TAG, "Tata letak kontrol Amber tidak bisa diterapkan ke mesin Rust: ${e.message}", e)
        }
    }

    private fun apply(context: Context, enabled: Boolean) {
        val home = HorizonEngine.engineHome(context)
        val config = readObject(File(home, CONFIG_FILE))
        val profile = config?.optInt("profile", 0) ?: 0
        val dir = if (profile > 0) File(home, "profiles/profile-$profile") else home
        if (!dir.isDirectory && (!enabled || !dir.mkdirs())) return

        val touch = File(dir, TOUCH_FILE)
        val backup = File(dir, BACKUP_FILE)
        val legacyBackup = File(dir, LEGACY_BACKUP_FILE)
        if (!enabled) {
            restore(touch, backup)
            restore(touch, legacyBackup)
            return
        }

        val buttons = ControlLayoutManager.buttons.value.ifEmpty {
            ControlLayoutManager.init(context)
            ControlLayoutManager.buttons.value
        }
        val heightDp = surfaceHeightDp(context, config?.optString("orientation", "landscape"))
        val plan = plan(buttons, heightDp, SettingsManager.settings.value.buttonLookDrag)
        // Tata letak tanpa joystick dan tanpa tombol apa pun jangan diterapkan: game jadi tidak bisa dikendalikan.
        if (plan.joy == null && plan.buttons.isEmpty()) return

        // Salinan berkas asli disimpan sekali, sebelum NovaCraft menyentuhnya pertama kali.
        if (!backup.isFile) {
            if (legacyBackup.isFile) {
                legacyBackup.renameTo(backup)
            } else {
                writeAtomic(backup, if (touch.isFile) touch.readText() else "{}")
            }
        }

        val json = build(readObject(touch) ?: JSONObject(), plan)
        val text = json.toString(2)
        if (touch.isFile && touch.readText() == text) return
        writeAtomic(touch, text)
    }

    /** Mengembalikan berkas mesin seperti sebelum NovaCraft menerapkan tata letaknya (bila pernah). */
    private fun restore(touch: File, backup: File) {
        if (!backup.isFile) return
        val saved = backup.readText().trim()
        if (saved.isEmpty() || saved == "{}") {
            // Dulu tidak ada berkasnya: mesin kembali ke bawaannya.
            touch.delete()
        } else {
            writeAtomic(touch, saved)
        }
        backup.delete()
    }

    // ---- terjemahan tata letak ----

    private fun plan(buttons: List<CustomControlButton>, heightDp: Float, lookDrag: Boolean): Plan {
        val scale = ENGINE_UI_HEIGHT / heightDp
        var joy: Joy? = null
        val out = ArrayList<Btn>()

        fun add(btn: Btn) {
            if (out.size < MAX_CUSTOM) out.add(btn)
        }

        // Tombol sistem (HIDE GUI, MENU) menjadi tombol keyboard: ditambahkan terakhir dan dilewati bila tombol
        // keyboard yang sama (mis. ESC) sudah ada di tata letak, supaya tidak ada dua tombol yang sama.
        val system = ArrayList<CustomControlButton>()

        for (b in buttons) {
            val inGame = b.showMode != ControlShowMode.MENU
            val inMenu = b.showMode != ControlShowMode.GAME
            val k = b.opacity.coerceIn(0.15f, 1f)

            if (b.isJoystick) {
                if (joy == null) joy = Joy(placeOf(b.xPercent, b.yPercent, b.widthDp.toFloat(), b.heightDp.toFloat(), heightDp), k, b.autoSprint, inGame, inMenu)
                continue
            }
            if (b.isScroll) {
                // Widget gulir hotbar: separuh atas = gulir naik, separuh bawah = gulir turun.
                val halfH = b.heightDp / 2f
                val up = placeOf(b.xPercent, b.yPercent - quarter(b, heightDp), b.widthDp.toFloat(), halfH, heightDp)
                val down = placeOf(b.xPercent, b.yPercent + quarter(b, heightDp), b.widthDp.toFloat(), halfH, heightDp)
                add(Btn("UP", listOf("mouse.scroll.up"), up, k, false, b.cornerRadiusDp.toFloat(), false, false, inGame, inMenu))
                add(Btn("DN", listOf("mouse.scroll.down"), down, k, false, b.cornerRadiusDp.toFloat(), false, false, inGame, inMenu))
                continue
            }

            if (b.isSystem) {
                system.add(b)
                continue
            }
            buttonOf(b, heightDp, lookDrag)?.let { add(it) }
        }
        for (b in system) {
            val btn = buttonOf(b, heightDp, lookDrag) ?: continue
            if (out.none { it.keys == btn.keys }) add(btn)
        }
        return Plan(joy, out, scale)
    }

    /** Tombol Amber biasa (keyboard, mouse, kombinasi, atau sistem yang punya padanan) sebagai tombol mesin. */
    private fun buttonOf(b: CustomControlButton, heightDp: Float, lookDrag: Boolean): Btn? {
        val keys = keysOf(b)
        if (keys.isEmpty()) return null
        return Btn(
            label = labelOf(b),
            keys = keys,
            place = placeOf(b.xPercent, b.yPercent, b.widthDp.toFloat(), b.heightDp.toFloat(), heightDp),
            opacity = b.opacity.coerceIn(0.15f, 1f),
            primary = !b.isSystem && b.heightDp >= 56,
            cornerDp = b.cornerRadiusDp.toFloat(),
            toggle = b.isToggle || (b.isMacro && b.macroType == "COMBO"),
            passthrough = lookDrag && !b.isSystem && !b.isMacro && (b.isMouseButton || b.keyCode == Key.GLFW_KEY_SPACE),
            inGame = b.showMode != ControlShowMode.MENU,
            inMenu = b.showMode != ControlShowMode.GAME
        )
    }

    /** Pergeseran vertikal (persen layar) dari pusat widget gulir ke pusat separuh atas/bawahnya. */
    private fun quarter(b: CustomControlButton, heightDp: Float): Float = (b.heightDp / 4f) / heightDp * 100f

    private fun placeOf(xPercent: Float, yPercent: Float, widthDp: Float, heightDp: Float, screenHeightDp: Float): Place {
        val wFrac = widthDp / screenHeightDp
        val hFrac = heightDp / screenHeightDp
        val size = sqrt(wFrac * hFrac).coerceIn(0.045f, 0.45f)
        return Place(
            x = (xPercent / 100f).coerceIn(0.025f, 0.975f),
            y = (yPercent / 100f).coerceIn(0.025f, 0.975f),
            size = size,
            wr = (wFrac / size).coerceIn(0.4f, 3.2f),
            hr = (hFrac / size).coerceIn(0.4f, 3.2f)
        )
    }

    private fun labelOf(b: CustomControlButton): String {
        val name = b.name.trim()
        return if (name.isNotEmpty()) name.take(9) else ""
    }

    /** Daftar token tombol mesin untuk tombol Amber ini (satu sampai tiga); kosong bila tidak punya padanan. */
    private fun keysOf(b: CustomControlButton): List<String> {
        if (b.isSystem) {
            return when (b.systemAction) {
                "HIDE_GUI" -> listOf("key.keyboard.f1")
                "MENU", "CLOSE" -> listOf("key.keyboard.escape")
                else -> emptyList() // FPS dan keyboard layar milik launcher, tidak ada di mesin
            }
        }
        if (b.isMacro) {
            if (b.macroType != "COMBO") return emptyList()
            val codes = if (b.macroComboKeys.isNotEmpty()) b.macroComboKeys else if (b.macroComboKey != 0) listOf(b.macroComboKey) else emptyList()
            val tokens = ArrayList<String>()
            tokenOf(b)?.let { tokens.add(it) }
            for (code in codes) keyToken(code)?.let { if (it !in tokens) tokens.add(it) }
            return tokens.take(3)
        }
        return listOfNotNull(tokenOf(b))
    }

    /** Token tombol/mouse mesin untuk tombol utama Amber ini, atau null bila mesin tidak mengenalnya. */
    private fun tokenOf(b: CustomControlButton): String? {
        if (b.isMouseButton) {
            return when (b.mouseButton) {
                Key.GLFW_MOUSE_BUTTON_LEFT -> "key.mouse.left"
                Key.GLFW_MOUSE_BUTTON_RIGHT -> "key.mouse.right"
                Key.GLFW_MOUSE_BUTTON_MIDDLE -> "key.mouse.middle"
                else -> null
            }
        }
        return keyToken(b.keyCode)
    }

    /** Kode tombol GLFW ke token keyboard mesin (`key.keyboard.*`); null bila mesin tidak mengenalnya. */
    private fun keyToken(code: Int): String? {
        val name = when (code) {
            in 65..90 -> ('a' + (code - 65)).toString() // A..Z
            in 48..57 -> (code - 48).toString() // 0..9
            in 290..301 -> "f${code - 289}" // F1..F12
            32 -> "space"
            39 -> "apostrophe"
            44 -> "comma"
            45 -> "minus"
            46 -> "period"
            47 -> "slash"
            59 -> "semicolon"
            61 -> "equal"
            91 -> "left.bracket"
            92 -> "backslash"
            93 -> "right.bracket"
            96 -> "grave.accent"
            256 -> "escape"
            257 -> "enter"
            258 -> "tab"
            259 -> "backspace"
            260 -> "insert"
            261 -> "delete"
            262 -> "right"
            263 -> "left"
            264 -> "down"
            265 -> "up"
            266 -> "page.up"
            267 -> "page.down"
            268 -> "home"
            269 -> "end"
            280 -> "caps.lock"
            340 -> "left.shift"
            341 -> "left.control"
            342 -> "left.alt"
            344 -> "right.shift"
            345 -> "right.control"
            346 -> "right.alt"
            else -> return null
        }
        return "key.keyboard.$name"
    }

    /** Tinggi layar game dalam dp, sebagai pembagi ukuran tombol (mesin menyatakan ukuran terhadap tinggi layar). */
    private fun surfaceHeightDp(context: Context, orientation: String?): Float {
        val m = context.resources.displayMetrics
        val shortPx = min(m.widthPixels, m.heightPixels)
        val longPx = max(m.widthPixels, m.heightPixels)
        val portrait = orientation == "portrait" || orientation == "reverse-portrait"
        return max(1f, (if (portrait) longPx else shortPx) / m.density)
    }

    // ---- berkas mesin ----

    private fun build(json: JSONObject, plan: Plan): JSONObject {
        json.put("control_style", "goy-control")
        json.put("mode", "crosshair")
        json.put("split_controls", true)
        json.put("left_handed", false)
        json.put("button_scale", 1.0)
        json.put("horizontal_margin", 0.0)
        json.put("vertical_margin", 0.0)
        json.put("dynamic_joystick", false)
        // Menu garis tiga (tombol yang membuka menu cepat mesin) mengikuti Pengaturan Rust > Menu garis tiga.
        // Tata letak tombol tetap datang dari editor kontrol Amber setiap PLAY.
        json.put("show_control_hub", SettingsManager.settings.value.rustControlHub)
        plan.joy?.let { json.put("sprint_with_joystick", it.autoSprint) }
        // Selalu preset bawaan, supaya mesin tidak menyimpan tata letak Amber ke preset pemain.
        json.put("preset_id", "default")
        json.put("active_profile", "default")
        json.put("active_goy_preset", "default")
        json.put("joystick_style", joystickStyle(plan.joy?.opacity ?: 0.9f, plan.scale))

        // Tiga baris tata letak (satu per mode) wajib lengkap; gaya Goy hanya memakai baris joystick.
        val layouts = JSONArray()
        repeat(3) {
            val row = JSONArray()
            for (i in 0 until CONTROLS) {
                val f = FILLER[i]
                val p = placement(f[0], f[1], f[2], 1f, 1f, 0.65f)
                val joy = plan.joy
                if (i == MOVE && joy != null) {
                    row.put(
                        placement(joy.place.x, joy.place.y, joy.place.size, joy.place.wr, joy.place.hr, joy.opacity)
                            .put("visible_in_game", joy.inGame)
                            .put("visible_in_menu", joy.inMenu)
                    )
                } else {
                    row.put(p)
                }
            }
            layouts.put(row)
        }
        json.put("layouts", layouts)

        // Gaya Goy hanya memakai joystick bawaan mesin; kontrol bawaan lain diganti tombol kustom di bawah.
        val visibility = JSONArray()
        for (i in 0 until CONTROLS) visibility.put(if (i == MOVE && plan.joy != null) 1 else 2)
        json.put("button_visibility", visibility)

        val custom = JSONArray()
        for (b in plan.buttons) custom.put(customButton(b, plan.scale))
        json.put("custom_buttons", custom)
        return json
    }

    /** Joystick berwarna kaca kosmik dengan tepi Nova cyan lembut. */
    private fun joystickStyle(k: Float, scale: Float): JSONObject = JSONObject()
        .put("stick_stroke", round(1.6f * scale))
        .put("stick_stroke_color", argb(fade(0.45f + 0.30f * k, k), NOVA_CYAN_SOFT))
        .put("stick_color", argb(fade(0.16f + 0.10f * k, k), NOVA_CYAN))
        .put("stick_opacity", 1.0)
        .put("stick_size", 0.36)
        .put("base_stroke", round(1.3f * scale))
        .put("base_stroke_color", argb(fade(0.24f + 0.16f * k, k), ICE_WHITE))
        .put("base_color", argb(fade(0.30f * (0.4f + 0.6f * k), k), COSMIC_GLASS))
        .put("base_opacity", 1.0)
        .put("base_roundness", 50.0)
        .put("move_area", "absolute-fixed")

    /**
     * Satu tombol NovaCraft sebagai tombol statis mesin bergaya Cyber-Glass.
     */
    private fun customButton(b: Btn, scale: Float): JSONObject {
        val k = b.opacity
        val fillBase = if (b.primary) 0.36f else 0.28f
        val edgeBase = if (b.primary) 0.26f else 0.18f
        // Satu penempatan per mode mesin (tiga); isinya sama karena mode dikunci ke crosshair.
        fun styled(): JSONObject = placement(b.place.x, b.place.y, b.place.size, b.place.wr, b.place.hr, k)
            .put("passthrough", b.passthrough)
            .put("toggleable", b.toggle)
            .put("swipeable", false)
            .put("shape", 0)
            .put("icon_id", 0)
            .put("use_goy_style", true)
            .put("stroke_size", round((if (b.primary) 1.4f else 1.25f) * scale))
            .put("stroke_margin", 0.0)
            .put("stroke_roundness", round(min(b.cornerDp * scale, 50f)))
            .put("roundness", round(min(b.cornerDp * scale, 50f)))
            .put("shadow_size", 0.0)
            .put("stroke_color", argb(fade(edgeBase + 0.16f * k, k), ICE_WHITE))
            .put("button_color", argb(fade(fillBase * (0.4f + 0.6f * k), k), COSMIC_GLASS))
            .put("on_click_color", argb(fade(0.36f + 0.30f * k, k), NOVA_CYAN))
            .put("on_click_stroke_color", argb(fade(0.90f, k), NOVA_CYAN_SOFT))
            .put("on_toggle_color", argb(fade(0.22f, k), NOVA_CYAN))
            .put("on_toggle_stroke_color", argb(1f, NOVA_CYAN_SOFT))
            .put("visible_in_game", b.inGame)
            .put("visible_in_menu", b.inMenu)
        val layouts = JSONArray()
        repeat(3) { layouts.put(styled()) }
        return JSONObject()
            .put("key", b.keys[0])
            .put("second_key", b.keys.getOrElse(1) { "" })
            .put("third_key", b.keys.getOrElse(2) { "" })
            .put("custom_label", b.label)
            .put("always_visible", b.inMenu)
            .put("layouts", layouts)
    }

    private fun placement(x: Float, y: Float, size: Float, wr: Float, hr: Float, opacity: Float): JSONObject =
        JSONObject()
            .put("center", JSONArray().put(round(x)).put(round(y)))
            .put("size", round(size))
            .put("opacity", round(opacity))
            .put("width_ratio", round(wr))
            .put("height_ratio", round(hr))

    /** Alfa yang dibagi kepekatan tombol (mesin mengalikannya lagi), dibatasi 0..1. */
    private fun fade(alpha: Float, opacity: Float): Float = (alpha / opacity).coerceIn(0f, 1f)

    /** Warna ARGB tanpa tanda (0xAARRGGBB) sebagai Long, bentuk yang dibaca mesin (u32). */
    private fun argb(alpha: Float, rgb: Int): Long =
        ((alpha * 255f).roundToInt().coerceIn(0, 255).toLong() shl 24) or (rgb.toLong() and 0xFFFFFFL)

    private fun round(v: Float): Double = Math.round(v * 1000f) / 1000.0

    private fun readObject(file: File): JSONObject? {
        if (!file.isFile) return null
        return try {
            JSONObject(file.readText())
        } catch (_: Exception) {
            null
        }
    }

    /** Tulis ke berkas sementara lalu pindahkan, supaya mesin tidak pernah membaca separuh isi. */
    private fun writeAtomic(target: File, text: String) {
        val temp = File(target.parentFile, TEMP_FILE)
        temp.writeText(text)
        if (!temp.renameTo(target)) {
            if (target.exists()) target.delete()
            if (!temp.renameTo(target)) {
                temp.delete()
                throw IOException("Tidak bisa menyimpan $target")
            }
        }
    }
}
