package com.aistudio.novacraft.launcher.core.account

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.util.Base64
import android.util.LruCache
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import com.google.gson.reflect.TypeToken
import com.aistudio.novacraft.launcher.core.utils.LauncherLog
import com.aistudio.novacraft.launcher.core.utils.LauncherLog as Log
import com.aistudio.novacraft.launcher.core.utils.LimitedInputStream
import com.aistudio.novacraft.launcher.core.utils.SettingsManager
import com.aistudio.novacraft.launcher.core.utils.UserAccount
import java.io.*
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Signature
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

// ----------------------------------------------------------------------------
// Section: core/account/AccountManager.kt
// ----------------------------------------------------------------------------

object AccountManager {
    private val gson = Gson()
    private val lock = Any()

    private const val SESSION_PREFS = "nova_account_session_prefs"
    private const val KEY_ACTIVE_ACCOUNT_ID = "active_account_id"
    private const val KEY_ACTIVE_USERNAME = "active_username"
    private const val KEY_LAST_SAVED_AT = "last_session_saved_at"

    // Akun Minecraft (profil pemain di halaman Akun)
    private val _accounts = MutableStateFlow<List<UserAccount>>(emptyList())
    val accounts: StateFlow<List<UserAccount>> = _accounts.asStateFlow()

    private val _currentAccount = MutableStateFlow<UserAccount?>(null)
    val currentAccount: StateFlow<UserAccount?> = _currentAccount.asStateFlow()

    private fun getAccountsFile(context: Context): File {
        return File(context.filesDir, "accounts.json")
    }

    private fun sessionPrefs(context: Context) =
        context.applicationContext.getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE)

    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        val app = context.applicationContext
        appContext = app
        synchronized(lock) {
            val accountsFile = getAccountsFile(app)
            if (!accountsFile.exists()) return
            try {
                val json = accountsFile.readText()
                val type = object : TypeToken<List<UserAccount>>() {}.type
                val rawList: List<UserAccount> = gson.fromJson(json, type) ?: emptyList()
                val list: List<UserAccount> = rawList.map { acc ->
                    val cleanName = acc.username.trim().ifBlank { "Player" }
                    val cleanType = acc.safeAccountType
                    val resolvedUuid = if (acc.uuid.isNullOrBlank()) {
                        deterministicOfflineUuid(cleanName)
                    } else {
                        acc.uuid
                    }
                    acc.copy(
                        id = if (acc.id.isNullOrBlank()) UUID.randomUUID().toString() else acc.id,
                        username = cleanName,
                        uuid = resolvedUuid,
                        accessToken = acc.safeAccessToken,
                        refreshToken = acc.safeRefreshToken,
                        email = acc.safeEmail,
                        tier = acc.safeTier,
                        accountType = cleanType,
                        skinModel = acc.safeSkinModel
                    )
                }

                // Buang sisa akun bawaan lama "NuxPlayer" dari daftar profil game
                val cleanGameList = list.filterNot {
                    it.username.equals("NuxPlayer", ignoreCase = true)
                }

                _accounts.value = cleanGameList

                val shouldRestore = SettingsManager.settings.value.persistAccountSession
                val savedId = if (shouldRestore) sessionPrefs(app).getString(KEY_ACTIVE_ACCOUNT_ID, null) else null
                val savedUsername = if (shouldRestore) sessionPrefs(app).getString(KEY_ACTIVE_USERNAME, null) else null
                val restored = cleanGameList.firstOrNull { !savedId.isNullOrBlank() && it.id == savedId }
                    ?: cleanGameList.firstOrNull { !savedUsername.isNullOrBlank() && it.username.equals(savedUsername, ignoreCase = true) }
                    ?: cleanGameList.firstOrNull()

                _currentAccount.value = restored
                saveGameAccounts(app)
                if (shouldRestore) {
                    persistActiveSessionLocked(app, restored)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ==========================================
    // MINECRAFT GAME ACCOUNTS OPERATIONS
    // ==========================================

    fun deterministicOfflineUuid(username: String): String {
        val clean = username.trim().ifBlank { "Player" }
        return UUID.nameUUIDFromBytes("OfflinePlayer:$clean".toByteArray(Charsets.UTF_8))
            .toString()
            .replace("-", "")
    }

    fun createGuestAccount(username: String): UserAccount {
        val clean = username.trim().ifBlank { "Player" }
        return UserAccount(
            id = "offline_${System.currentTimeMillis()}",
            username = clean,
            uuid = deterministicOfflineUuid(clean),
            accessToken = "0",
            isOffline = true,
            accountType = "offline"
        )
    }

    /**
     * Menghasilkan akun yang dipakai saat peluncuran game:
     * - Mode Online: memakai akun asli apa adanya.
     * - Mode Offline: memakai sesi lokal offline tanpa menyentuh atau menghapus token akun tersimpan.
     */
    fun resolveLaunchAccount(account: UserAccount, offlineMode: Boolean): UserAccount {
        if (!offlineMode || account.safeAccountType == "offline") return account
        val cleanName = account.username.trim().ifBlank { "Player" }
        return account.copy(
            username = cleanName,
            uuid = deterministicOfflineUuid(cleanName),
            accessToken = "0",
            isOffline = true,
            accountType = "offline"
        )
    }

    fun addAccount(context: Context, account: UserAccount) {
        val app = context.applicationContext
        appContext = app
        synchronized(lock) {
            val list = _accounts.value.toMutableList()
            list.removeAll {
                it.id == account.id ||
                    (it.username.equals(account.username, ignoreCase = true) && it.safeAccountType == account.safeAccountType)
            }
            list.add(0, account)
            _accounts.value = list
            _currentAccount.value = account
            saveGameAccounts(app)
            if (SettingsManager.settings.value.persistAccountSession) {
                persistActiveSessionLocked(app, account)
            }
        }
    }

    fun updateAccount(context: Context, account: UserAccount) {
        val app = context.applicationContext
        appContext = app
        synchronized(lock) {
            val list = _accounts.value.toMutableList()
            val index = list.indexOfFirst {
                it.id == account.id ||
                    (it.username.equals(account.username, ignoreCase = true) && it.safeAccountType == account.safeAccountType)
            }
            if (index != -1) {
                list[index] = account
            } else {
                list.add(0, account)
            }
            _accounts.value = list
            val isCurrentMatch = _currentAccount.value?.id == account.id ||
                (_currentAccount.value?.username?.equals(account.username, ignoreCase = true) == true &&
                    _currentAccount.value?.safeAccountType == account.safeAccountType)
            if (isCurrentMatch || _currentAccount.value == null) {
                _currentAccount.value = account
                if (SettingsManager.settings.value.persistAccountSession) {
                    persistActiveSessionLocked(app, account)
                }
            }
            saveGameAccounts(app)
        }
    }

    fun selectAccount(account: UserAccount) {
        val ctx = appContext
        if (ctx != null) {
            selectAccount(ctx, account)
        } else {
            _currentAccount.value = account
        }
    }

    fun selectAccount(context: Context, account: UserAccount) {
        val app = context.applicationContext
        appContext = app
        synchronized(lock) {
            _currentAccount.value = account
            if (SettingsManager.settings.value.persistAccountSession) {
                persistActiveSessionLocked(app, account)
            }
        }
    }

    fun setSessionPersistence(context: Context, enabled: Boolean) {
        val app = context.applicationContext
        appContext = app
        synchronized(lock) {
            if (enabled) {
                persistActiveSessionLocked(app, _currentAccount.value)
            } else {
                sessionPrefs(app).edit().clear().apply()
            }
        }
    }

    fun lastSessionSavedAt(context: Context): Long {
        return sessionPrefs(context).getLong(KEY_LAST_SAVED_AT, 0L)
    }

    fun deleteAccount(context: Context, account: UserAccount) {
        val app = context.applicationContext
        appContext = app
        synchronized(lock) {
            val list = _accounts.value.toMutableList()
            list.removeAll { it.id == account.id || (it.username.equals(account.username, ignoreCase = true) && it.safeAccountType == account.safeAccountType) }
            _accounts.value = list
            if (_currentAccount.value?.id == account.id ||
                (_currentAccount.value?.username?.equals(account.username, ignoreCase = true) == true &&
                    _currentAccount.value?.safeAccountType == account.safeAccountType)
            ) {
                _currentAccount.value = list.firstOrNull()
            }
            saveGameAccounts(app)
            if (SettingsManager.settings.value.persistAccountSession) {
                persistActiveSessionLocked(app, _currentAccount.value)
            }
        }
    }

    fun removeAccount(context: Context, account: UserAccount) {
        deleteAccount(context, account)
    }

    private fun persistActiveSessionLocked(context: Context, account: UserAccount?) {
        try {
            val editor = sessionPrefs(context).edit()
            if (account != null) {
                editor.putString(KEY_ACTIVE_ACCOUNT_ID, account.id)
                editor.putString(KEY_ACTIVE_USERNAME, account.username)
                editor.putLong(KEY_LAST_SAVED_AT, System.currentTimeMillis())
            } else {
                editor.remove(KEY_ACTIVE_ACCOUNT_ID)
                editor.remove(KEY_ACTIVE_USERNAME)
            }
            editor.apply()
        } catch (_: Exception) {
        }
    }

    private fun saveGameAccounts(context: Context) {
        try {
            val file = getAccountsFile(context)
            com.aistudio.novacraft.launcher.core.utils.AtomicFiles.writeText(file, gson.toJson(_accounts.value))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

// ----------------------------------------------------------------------------
// Section: core/account/elyby/ElyByAuthService.kt
// ----------------------------------------------------------------------------

data class ElyByDeviceCode(
    val deviceCode: String,
    val userCode: String,
    val verificationUri: String,
    val expiresIn: Long,
    val interval: Long
) {
    val verificationUrlWithCode: String
        get() {
            val base = if (verificationUri.isNotBlank()) {
                if (verificationUri.startsWith("http://")) verificationUri.replace("http://", "https://")
                else verificationUri
            } else {
                "https://account.ely.by/code"
            }
            return if (base.contains("?")) {
                "$base&user_code=$userCode"
            } else {
                "$base?user_code=$userCode"
            }
        }
}

sealed class ElyByLoginState {
    object Idle : ElyByLoginState()
    object Starting : ElyByLoginState()
    data class WaitingForApproval(
        val deviceCode: ElyByDeviceCode,
        val statusMsg: String
    ) : ElyByLoginState()
    data class Success(val account: UserAccount) : ElyByLoginState()
    data class Error(val message: String) : ElyByLoginState()
}

object ElyByAuthService {
    const val AUTHLIB_INJECTOR_URL = "https://authserver.ely.by/api/authlib-injector"
    private const val CLIENT_ID = "ely"
    private const val SCOPE = "account_info minecraft_server_session"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val gson = Gson()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var activeJob: Job? = null

    private val _loginState = MutableStateFlow<ElyByLoginState>(ElyByLoginState.Idle)
    val loginState: StateFlow<ElyByLoginState> = _loginState.asStateFlow()

    fun resetState() {
        activeJob?.cancel()
        activeJob = null
        _loginState.value = ElyByLoginState.Idle
    }

    fun cancelLogin() {
        activeJob?.cancel()
        activeJob = null
        _loginState.value = ElyByLoginState.Idle
    }

    fun openBrowser(context: Context, url: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    /**
     * Memulai alur login browser Ely.by OAuth Device Code.
     * Berjalan di serviceScope sehingga tidak terputus saat pengguna berpindah ke browser.
     */
    fun startLogin(context: Context) {
        activeJob?.cancel()
        _loginState.value = ElyByLoginState.Starting

        activeJob = serviceScope.launch {
            try {
                val dcRes = startDeviceCodeFlow()
                if (dcRes.isFailure) {
                    _loginState.value = ElyByLoginState.Error(
                        dcRes.exceptionOrNull()?.message ?: "Gagal memulai otorisasi Ely.by."
                    )
                    return@launch
                }

                val dc = dcRes.getOrThrow()
                _loginState.value = ElyByLoginState.WaitingForApproval(
                    deviceCode = dc,
                    statusMsg = "Menunggu otorisasi di browser Ely.by..."
                )

                // Salin kode ke clipboard
                runCatching {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    val clip = ClipData.newPlainText("Ely.by User Code", dc.userCode)
                    clipboard?.setPrimaryClip(clip)
                }

                // Buka browser ke URL otorisasi
                openBrowser(context, dc.verificationUrlWithCode)

                // Polling token sampai selesai atau dibatalkan
                val pollRes = pollForToken(
                    deviceCode = dc,
                    isCancelled = { activeJob?.isCancelled == true },
                    onStatusUpdate = { msg ->
                        val current = _loginState.value
                        if (current is ElyByLoginState.WaitingForApproval) {
                            _loginState.value = current.copy(statusMsg = msg)
                        }
                    }
                )

                if (pollRes.isSuccess) {
                    val rawAcc = pollRes.getOrThrow()
                    val cachedAcc = SkinUtils.ensureSkinAndCapeCached(context, rawAcc)
                    AccountManager.addAccount(context, cachedAcc)
                    _loginState.value = ElyByLoginState.Success(cachedAcc)
                } else {
                    _loginState.value = ElyByLoginState.Error(
                        pollRes.exceptionOrNull()?.message ?: "Otorisasi Ely.by gagal."
                    )
                }
            } catch (e: Exception) {
                _loginState.value = ElyByLoginState.Error(
                    e.message ?: "Terjadi kesalahan saat login Ely.by."
                )
            }
        }
    }

    /**
     * Memulai request device code ke Ely.by API
     */
    suspend fun startDeviceCodeFlow(): Result<ElyByDeviceCode> = withContext(Dispatchers.IO) {
        try {
            val formBody = FormBody.Builder()
                .add("client_id", CLIENT_ID)
                .add("scope", SCOPE)
                .build()

            val request = Request.Builder()
                .url("https://account.ely.by/api/oauth2/v1/devicecode")
                .post(formBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Gagal memulai login Ely.by Device Code (${response.code}): $bodyStr"))
                }

                val json = JsonParser.parseString(bodyStr).asJsonObject
                val deviceCode = json.get("device_code")?.asString ?: ""
                val userCode = json.get("user_code")?.asString ?: ""
                val verificationUri = json.get("verification_uri")?.asString ?: "https://account.ely.by/code"
                val expiresIn = json.get("expires_in")?.asLong ?: 600L
                val interval = json.get("interval")?.asLong ?: 5L

                Result.success(
                    ElyByDeviceCode(
                        deviceCode = deviceCode,
                        userCode = userCode,
                        verificationUri = verificationUri,
                        expiresIn = expiresIn,
                        interval = interval
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Melakukan polling ke server Ely.by sampai otorisasi browser selesai
     */
    suspend fun pollForToken(
        deviceCode: ElyByDeviceCode,
        isCancelled: () -> Boolean,
        onStatusUpdate: (String) -> Unit
    ): Result<UserAccount> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val timeoutMs = deviceCode.expiresIn * 1000L
        var pollIntervalMs = (if (deviceCode.interval <= 0) 5L else deviceCode.interval) * 1000L

        var accessToken: String? = null

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (isCancelled()) {
                return@withContext Result.failure(Exception("Login dibatalkan oleh pengguna."))
            }

            delay(pollIntervalMs)

            if (isCancelled()) {
                return@withContext Result.failure(Exception("Login dibatalkan oleh pengguna."))
            }

            try {
                val formBody = FormBody.Builder()
                    .add("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
                    .add("client_id", CLIENT_ID)
                    .add("device_code", deviceCode.deviceCode)
                    .build()

                val request = Request.Builder()
                    .url("https://account.ely.by/api/oauth2/v1/token")
                    .post(formBody)
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    val bodyStr = response.body?.string() ?: ""
                    val json = runCatching { JsonParser.parseString(bodyStr).asJsonObject }.getOrNull()

                    if (response.isSuccessful && json != null) {
                        val token = json.get("access_token")?.asString
                        if (!token.isNullOrBlank()) {
                            accessToken = token
                        }
                    }

                    if (json != null && json.has("error")) {
                        val err = json.get("error").asString
                        when (err) {
                            "authorization_pending" -> {
                                onStatusUpdate("Menunggu konfirmasi di browser...")
                            }
                            "slow_down" -> {
                                pollIntervalMs += 2000L
                                onStatusUpdate("Memperlambat polling...")
                            }
                            else -> {
                                return@withContext Result.failure(Exception("Ely.by OAuth Error: $err"))
                            }
                        }
                    }
                }

                if (accessToken != null) {
                    break
                }
            } catch (e: Exception) {
                // Abaikan error jaringan sementara
            }
        }

        val token = accessToken ?: return@withContext Result.failure(Exception("Waktu otorisasi Ely.by habis."))

        // Step 2: Ambil info akun (username & email)
        onStatusUpdate("Mengambil informasi akun Ely.by...")
        val infoReq = Request.Builder()
            .url("https://account.ely.by/api/account/v1/info")
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        var username = "Player"
        var email = ""
        httpClient.newCall(infoReq).execute().use { response ->
            val bodyStr = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Gagal mengambil info akun Ely.by (${response.code})"))
            }
            val json = JsonParser.parseString(bodyStr).asJsonObject
            username = json.get("username")?.asString ?: "Player"
            email = json.get("email")?.asString ?: ""
        }

        // Step 3: Ambil Minecraft UUID dari server Ely.by
        onStatusUpdate("Mengambil UUID Minecraft untuk $username...")
        var playerUuid = UUID.nameUUIDFromBytes("OfflinePlayer:$username".toByteArray(Charsets.UTF_8)).toString().replace("-", "")
        try {
            val uuidReq = Request.Builder()
                .url("https://authserver.ely.by/api/users/profiles/minecraft/$username")
                .get()
                .build()

            httpClient.newCall(uuidReq).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    val json = JsonParser.parseString(bodyStr).asJsonObject
                    val id = json.get("id")?.asString
                    if (!id.isNullOrBlank()) {
                        playerUuid = id.replace("-", "")
                    }
                }
            }
        } catch (_: Exception) {}

        val cleanUuid = playerUuid.replace("-", "")
        val skinUrl = "https://skinsystem.ely.by/skins/$username.png"
        val account = UserAccount(
            id = "ely_$cleanUuid",
            username = username,
            uuid = cleanUuid,
            accessToken = token,
            isOffline = false,
            accountType = "elyby",
            authServerUrl = AUTHLIB_INJECTOR_URL,
            email = email,
            skinUrl = skinUrl
        )

        Result.success(account)
    }

    /**
     * Resolves authentic 32-character Ely.by Yggdrasil UUID for username
     */
    fun fetchRealPlayerUuid(username: String): String {
        return try {
            val uuidReq = Request.Builder()
                .url("https://authserver.ely.by/api/users/profiles/minecraft/$username")
                .get()
                .build()
            httpClient.newCall(uuidReq).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    val json = JsonParser.parseString(bodyStr).asJsonObject
                    json.get("id")?.asString?.replace("-", "") ?: ""
                } else ""
            }
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Fallback login langsung (tidak ditampilkan di UI, disimpan untuk kompatibilitas)
     */
    suspend fun loginWithCredentials(
        usernameOrEmail: String,
        password: String
    ): Result<UserAccount> = withContext(Dispatchers.IO) {
        try {
            val clientToken = UUID.randomUUID().toString().replace("-", "")
            val payload = JsonObject().apply {
                val agent = JsonObject().apply {
                    addProperty("name", "Minecraft")
                    addProperty("version", 1)
                }
                add("agent", agent)
                addProperty("username", usernameOrEmail.trim())
                addProperty("password", password)
                addProperty("requestUser", true)
                addProperty("clientToken", clientToken)
            }

            val request = Request.Builder()
                .url("$AUTHLIB_INJECTOR_URL/authserver/authenticate")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                val json = runCatching { JsonParser.parseString(bodyStr).asJsonObject }.getOrNull()

                if (json != null && json.has("errorMessage")) {
                    val errMsg = json.get("errorMessage").asString
                    return@withContext Result.failure(Exception("Ely.by: $errMsg"))
                }

                if (!response.isSuccessful || json == null) {
                    return@withContext Result.failure(Exception("Gagal login ke Ely.by (HTTP ${response.code}): $bodyStr"))
                }

                val accessToken = json.get("accessToken")?.asString ?: "0"
                val resClientToken = json.get("clientToken")?.asString ?: clientToken
                val selectedProfile = json.getAsJsonObject("selectedProfile")
                    ?: return@withContext Result.failure(Exception("Profil Ely.by tidak ditemukan dalam respon."))

                val profileId = selectedProfile.get("id")?.asString ?: UUID.randomUUID().toString().replace("-", "")
                val profileName = selectedProfile.get("name")?.asString ?: usernameOrEmail.trim()
                val skinUrl = "https://skinsystem.ely.by/skins/$profileName.png"

                val account = UserAccount(
                    id = "ely_${profileId.replace("-", "")}",
                    username = profileName,
                    uuid = profileId.replace("-", ""),
                    accessToken = accessToken,
                    refreshToken = resClientToken,
                    isOffline = false,
                    accountType = "elyby",
                    authServerUrl = AUTHLIB_INJECTOR_URL,
                    email = usernameOrEmail.trim(),
                    skinUrl = skinUrl
                )

                Result.success(account)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// ----------------------------------------------------------------------------
// Section: core/account/microsoft/MicrosoftAuthService.kt
// ----------------------------------------------------------------------------

data class MicrosoftDeviceCode(
    val deviceCode: String,
    val userCode: String,
    val verificationUri: String,
    val expiresIn: Long,
    val interval: Long
)

object MicrosoftAuthService {
    private const val CLIENT_ID = "c36a9fb6-4f2a-41ff-90bd-ae7cc92031eb"
    private const val SCOPE = "XboxLive.signin offline_access openid profile email"
    private const val TENANT = "/consumers"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val gson = Gson()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    // Mencegah multiple concurrent polling sessions
    private val isCurrentlyPolling = AtomicBoolean(false)

    // Token Minecraft berlaku sekitar 24 jam. Diperbarui bila sudah lebih tua dari ini supaya
    // masih berlaku sepanjang sesi bermain (mis. untuk masuk ke server).
    private const val SESSION_MAX_AGE_MS = 8L * 60L * 60L * 1000L

    /** True bila akun Microsoft ini punya refresh token dan token Minecraft-nya sudah (hampir) kedaluwarsa. */
    fun needsRefresh(account: UserAccount): Boolean {
        if (account.safeAccountType != "microsoft") return false
        if (account.safeRefreshToken.isBlank() || account.safeRefreshToken == "0") return false
        val issued = account.tokenIssuedAt ?: return true
        val age = System.currentTimeMillis() - issued
        return age < 0L || age > SESSION_MAX_AGE_MS
    }

    /** Galat jaringan diganti pesan yang bisa dipahami; galat lain dibiarkan apa adanya. */
    private fun friendly(e: Exception): Exception =
        if (e is IOException) Exception("Tidak bisa terhubung ke server Microsoft. Periksa sambungan internet lalu coba lagi.") else e

    /**
     * Memulai alur Microsoft Device Code Authorization
     */
    suspend fun startDeviceCodeFlow(): Result<MicrosoftDeviceCode> = withContext(Dispatchers.IO) {
        try {
            val formBody = FormBody.Builder()
                .add("client_id", CLIENT_ID)
                .add("scope", SCOPE)
                .build()

            val request = Request.Builder()
                .url("https://login.microsoftonline.com$TENANT/oauth2/v2.0/devicecode")
                .post(formBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    LauncherLog.w("MicrosoftAuth", "devicecode HTTP ${response.code}: $bodyStr")
                    return@withContext Result.failure(Exception("Gagal memulai login Microsoft (kode ${response.code}). Coba lagi nanti."))
                }

                val json = JsonParser.parseString(bodyStr).asJsonObject
                val deviceCode = json.get("device_code")?.asString ?: ""
                val userCode = json.get("user_code")?.asString ?: ""
                val verificationUri = json.get("verification_uri")?.asString ?: "https://microsoft.com/link"
                val expiresIn = json.get("expires_in")?.asLong ?: 900L
                val interval = json.get("interval")?.asLong ?: 5L

                Result.success(
                    MicrosoftDeviceCode(
                        deviceCode = deviceCode,
                        userCode = userCode,
                        verificationUri = verificationUri,
                        expiresIn = expiresIn,
                        interval = interval
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(friendly(e))
        }
    }

    /**
     * Melakukan polling ke server Microsoft sampai user mengizinkan di browser/WebView
     */
    suspend fun pollForToken(
        deviceCode: MicrosoftDeviceCode,
        isCancelled: () -> Boolean,
        onStatusUpdate: (String) -> Unit
    ): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val timeoutMs = deviceCode.expiresIn * 1000L
        var pollIntervalMs = (if (deviceCode.interval <= 0) 5L else deviceCode.interval) * 1000L
        var consecutiveNetworkFailures = 0

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (isCancelled()) {
                return@withContext Result.failure(Exception("Login dibatalkan oleh pengguna."))
            }

            delay(pollIntervalMs)

            if (isCancelled()) {
                return@withContext Result.failure(Exception("Login dibatalkan oleh pengguna."))
            }

            try {
                val formBody = FormBody.Builder()
                    .add("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
                    .add("client_id", CLIENT_ID)
                    .add("device_code", deviceCode.deviceCode)
                    .add("tenant", TENANT)
                    .build()

                val request = Request.Builder()
                    .url("https://login.microsoftonline.com$TENANT/oauth2/v2.0/token")
                    .post(formBody)
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    val bodyStr = response.body?.string() ?: ""
                    val json = runCatching { JsonParser.parseString(bodyStr).asJsonObject }.getOrNull()

                    consecutiveNetworkFailures = 0

                    if (response.isSuccessful && json != null) {
                        val accessToken = json.get("access_token")?.asString
                        val refreshToken = json.get("refresh_token")?.asString ?: ""
                        if (!accessToken.isNullOrBlank()) {
                            onStatusUpdate("Token berhasil diterima! Menghubungkan ke Xbox Live...")
                            return@withContext Result.success(Pair(accessToken, refreshToken))
                        }
                    }

                    if (json != null && json.has("error")) {
                        val err = json.get("error").asString
                        val desc = json.get("error_description")?.asString ?: err

                        when (err) {
                            "authorization_pending" -> {
                                onStatusUpdate("Menunggu otorisasi di halaman web...")
                                // Lanjut polling normal
                            }
                            "slow_down" -> {
                                pollIntervalMs += 2000L
                                onStatusUpdate("Memperlambat frekuensi polling...")
                            }
                            // Microsoft v2.0 memakai "expired_token"; "code_expired" nama lama dari v1.
                            "code_expired", "expired_token" -> {
                                return@withContext Result.failure(Exception("Kode login kedaluwarsa. Silakan mulai ulang."))
                            }
                            "bad_verification_code" -> {
                                return@withContext Result.failure(Exception("Kode login tidak dikenali. Silakan mulai ulang."))
                            }
                            "authorization_declined" -> {
                                return@withContext Result.failure(Exception("Login ditolak di halaman Microsoft. Silakan mulai ulang."))
                            }
                            "invalid_grant" -> {
                                if (desc.contains("AADSTS70000") || desc.contains("already been used")) {
                                    return@withContext Result.failure(
                                        Exception("Kode otorisasi telah digunakan atau kedaluwarsa. Silakan klik Login untuk mendapatkan kode baru.")
                                    )
                                } else {
                                    return@withContext Result.failure(Exception("Otorisasi ditolak ($err): $desc"))
                                }
                            }
                            else -> {
                                return@withContext Result.failure(Exception("Microsoft OAuth Error: $desc"))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Jangan batalkan polling hanya karena fluktuasi jaringan sementara
                consecutiveNetworkFailures++
                if (consecutiveNetworkFailures >= 6) {
                    return@withContext Result.failure(Exception("Koneksi jaringan terputus berulang kali saat polling. Silakan periksa koneksi internet."))
                }
            }
        }

        Result.failure(Exception("Waktu otorisasi habis (timeout). Silakan coba lagi."))
    }

    /**
     * Menukarkan Token Microsoft menjadi Profil & Token Minecraft Java
     * Mengadopsi arsitektur tangguh Zalith Launcher (XBL fallback, penanganan XErr XSTS lengkap)
     */
    suspend fun completeMinecraftAuth(
        msAccessToken: String,
        msRefreshToken: String,
        onStatusUpdate: (String) -> Unit
    ): Result<UserAccount> = withContext(Dispatchers.IO) {
        try {
            // =========================================================
            // 1. Authenticate with Xbox Live (XBL) dengan Fallback RpsTicket
            // =========================================================
            onStatusUpdate("Autentikasi dengan Xbox Live...")

            fun doXblRequest(rpsTicket: String): Pair<Int, String> {
                val xblPayload = JsonObject().apply {
                    val props = JsonObject().apply {
                        addProperty("AuthMethod", "RPS")
                        addProperty("SiteName", "user.auth.xboxlive.com")
                        addProperty("RpsTicket", rpsTicket)
                    }
                    add("Properties", props)
                    addProperty("RelyingParty", "http://auth.xboxlive.com")
                    addProperty("TokenType", "JWT")
                }

                val xblReq = Request.Builder()
                    .url("https://user.auth.xboxlive.com/user/authenticate")
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("x-xbl-contract-version", "1")
                    .post(xblPayload.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                return httpClient.newCall(xblReq).execute().use { response ->
                    Pair(response.code, response.body?.string() ?: "")
                }
            }

            // Pertama coba dengan prefix "d=" (standar MSA)
            var (xblCode, xblBody) = doXblRequest("d=$msAccessToken")

            // Jika menerima HTTP 400 Bad Request, coba tanpa prefix "d=" (sesuai dokumentasi & implementasi Zalith)
            if (xblCode == 400) {
                val fallbackRes = doXblRequest(msAccessToken)
                xblCode = fallbackRes.first
                xblBody = fallbackRes.second
            }

            if (xblCode !in 200..299) {
                LauncherLog.w("MicrosoftAuth", "XBL HTTP $xblCode: $xblBody")
                if (xblCode == 429) {
                    return@withContext Result.failure(Exception("Terlalu banyak percobaan login. Tunggu beberapa menit lalu coba lagi."))
                }
                return@withContext Result.failure(Exception("Gagal autentikasi Xbox Live (kode $xblCode). Coba lagi nanti."))
            }

            val xblJson = JsonParser.parseString(xblBody).asJsonObject
            val xblToken = xblJson.get("Token")?.asString ?: ""
            val claims = xblJson.getAsJsonObject("DisplayClaims")
            val xui = claims?.getAsJsonArray("xui")
            val userHash = xui?.get(0)?.asJsonObject?.get("uhs")?.asString ?: ""

            if (xblToken.isBlank() || userHash.isBlank()) {
                return@withContext Result.failure(Exception("Gagal memperoleh token Xbox Live atau UserHash."))
            }

            // =========================================================
            // 2. Authenticate with Xbox Secure Token Service (XSTS)
            // =========================================================
            onStatusUpdate("Meminta otorisasi XSTS Minecraft...")
            val xstsPayload = JsonObject().apply {
                val props = JsonObject().apply {
                    addProperty("SandboxId", "RETAIL")
                    val tokensArray = com.google.gson.JsonArray().apply { add(xblToken) }
                    add("UserTokens", tokensArray)
                }
                add("Properties", props)
                addProperty("RelyingParty", "rp://api.minecraftservices.com/")
                addProperty("TokenType", "JWT")
            }

            val xstsReq = Request.Builder()
                .url("https://xsts.auth.xboxlive.com/xsts/authorize")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("x-xbl-contract-version", "1")
                .post(xstsPayload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            var xstsToken = ""
            httpClient.newCall(xstsReq).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    val xErrJson = runCatching { JsonParser.parseString(bodyStr).asJsonObject }.getOrNull()
                    val xErr = xErrJson?.get("XErr")?.asString ?: ""

                    when (xErr) {
                        "2148916227" -> return@withContext Result.failure(Exception("Akun ini telah dilarang/diblokir dari Xbox Live."))
                        "2148916229" -> return@withContext Result.failure(Exception("Akun dibatasi oleh kontrol orang tua (Restricted)."))
                        "2148916233" -> return@withContext Result.failure(Exception("Akun Microsoft ini belum memiliki profil Xbox. Silakan buat gamertag di xbox.com terlebih dahulu."))
                        "2148916234" -> return@withContext Result.failure(Exception("Akun belum menyetujui Ketentuan Layanan Xbox Live."))
                        "2148916235" -> return@withContext Result.failure(Exception("Layanan Xbox Live tidak tersedia di negara/wilayah akun Anda."))
                        "2148916236" -> return@withContext Result.failure(Exception("Akun memerlukan verifikasi bukti usia dewasa (Proof of Age)."))
                        "2148916237" -> return@withContext Result.failure(Exception("Batas waktu bermain harian Xbox Live telah tercapai."))
                        "2148916238" -> return@withContext Result.failure(Exception("Akun di bawah umur (Child Account). Harus ditautkan ke akun keluarga (Family Safety) oleh orang tua."))
                        else -> {
                            LauncherLog.w("MicrosoftAuth", "XSTS HTTP ${response.code} XErr=$xErr: $bodyStr")
                            if (response.code == 429) {
                                return@withContext Result.failure(Exception("Terlalu banyak percobaan login. Tunggu beberapa menit lalu coba lagi."))
                            }
                            return@withContext Result.failure(Exception("Otorisasi XSTS gagal (kode ${xErr.ifBlank { response.code.toString() }}). Coba lagi nanti."))
                        }
                    }
                }

                val json = JsonParser.parseString(bodyStr).asJsonObject
                xstsToken = json.get("Token")?.asString ?: ""
            }

            if (xstsToken.isBlank()) {
                return@withContext Result.failure(Exception("Gagal memperoleh token XSTS."))
            }

            // =========================================================
            // 3. Login with Xbox to Mojang Minecraft Services
            // =========================================================
            onStatusUpdate("Menghubungkan ke Mojang Minecraft Services...")
            val mojangPayload = JsonObject().apply {
                addProperty("identityToken", "XBL3.0 x=$userHash;$xstsToken")
                addProperty("ensureLegacyEnabled", true)
            }

            val mojangReq = Request.Builder()
                .url("https://api.minecraftservices.com/authentication/login_with_xbox")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .post(mojangPayload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            var mcAccessToken = ""
            httpClient.newCall(mojangReq).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    LauncherLog.w("MicrosoftAuth", "login_with_xbox HTTP ${response.code}: $bodyStr")
                    // Mojang hanya menerima aplikasi (Client ID) yang sudah terdaftar dan disetujui.
                    if (response.code == 403 && bodyStr.contains("Invalid app registration", ignoreCase = true)) {
                        return@withContext Result.failure(
                            Exception("Mojang menolak aplikasi launcher ini (Invalid app registration, kode 403). Client ID login belum disetujui; hubungi pengembang launcher.")
                        )
                    }
                    return@withContext Result.failure(
                        Exception(
                            when {
                                response.code == 429 -> "Terlalu banyak percobaan login. Tunggu beberapa menit lalu coba lagi."
                                response.code >= 500 -> "Server Minecraft sedang bermasalah (kode ${response.code}). Coba lagi beberapa menit lagi."
                                else -> "Layanan Minecraft menolak login (kode ${response.code}). Coba lagi nanti. Bila terus berulang, laporkan ke pengembang launcher."
                            }
                        )
                    )
                }
                val json = JsonParser.parseString(bodyStr).asJsonObject
                mcAccessToken = json.get("access_token")?.asString ?: ""
            }

            if (mcAccessToken.isBlank()) {
                return@withContext Result.failure(Exception("Layanan Minecraft tidak mengembalikan token akses."))
            }

            // =========================================================
            // 4. Fetch Minecraft Profile
            // =========================================================
            onStatusUpdate("Mengambil profil pemain Minecraft Java...")
            val profileReq = Request.Builder()
                .url("https://api.minecraftservices.com/minecraft/profile")
                .header("Authorization", "Bearer $mcAccessToken")
                .get()
                .build()

            httpClient.newCall(profileReq).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.code == 404) {
                    return@withContext Result.failure(
                        Exception("Akun ini belum punya profil Minecraft Java. Pastikan Minecraft: Java Edition (atau Game Pass PC) sudah dimiliki, lalu buat nama pemain di minecraft.net/profile.")
                    )
                }
                if (!response.isSuccessful) {
                    LauncherLog.w("MicrosoftAuth", "profile HTTP ${response.code}: $bodyStr")
                    return@withContext Result.failure(Exception("Gagal mengambil profil Minecraft (kode ${response.code})."))
                }

                val json = JsonParser.parseString(bodyStr).asJsonObject
                val profileId = json.get("id")?.asString ?: UUID.randomUUID().toString()
                val profileName = json.get("name")?.asString ?: "Player"

                var skinUrl: String? = null
                val skinsArray = json.getAsJsonArray("skins")
                if (skinsArray != null && skinsArray.size() > 0) {
                    val firstSkin = skinsArray.get(0).asJsonObject
                    if (firstSkin.has("url")) {
                        skinUrl = firstSkin.get("url").asString
                    }
                }

                val account = UserAccount(
                    id = "ms_$profileId",
                    username = profileName,
                    uuid = profileId,
                    accessToken = mcAccessToken,
                    refreshToken = msRefreshToken.ifBlank { "0" },
                    isOffline = false,
                    accountType = "microsoft",
                    xuid = userHash,
                    skinUrl = skinUrl,
                    tokenIssuedAt = System.currentTimeMillis()
                )

                Result.success(account)
            }
        } catch (e: Exception) {
            Result.failure(friendly(e))
        }
    }

    /**
     * Memperbarui sesi akun Microsoft yang sudah tersimpan menggunakan refresh_token
     */
    suspend fun refreshMicrosoftAccount(account: UserAccount): Result<UserAccount> = withContext(Dispatchers.IO) {
        if (account.safeRefreshToken.isBlank() || account.safeRefreshToken == "0") {
            return@withContext Result.failure(Exception("Tidak ada refresh token tersimpan."))
        }

        try {
            val formBody = FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("client_id", CLIENT_ID)
                .add("scope", SCOPE)
                .add("refresh_token", account.safeRefreshToken)
                .add("tenant", TENANT)
                .build()

            val request = Request.Builder()
                .url("https://login.microsoftonline.com$TENANT/oauth2/v2.0/token")
                .post(formBody)
                .build()

            var newAccessToken = ""
            var newRefreshToken = account.safeRefreshToken

            httpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    LauncherLog.w("MicrosoftAuth", "refresh HTTP ${response.code}: $bodyStr")
                    return@withContext Result.failure(Exception("Gagal memperbarui sesi Microsoft (kode ${response.code}). Silakan login ulang."))
                }
                val json = JsonParser.parseString(bodyStr).asJsonObject
                newAccessToken = json.get("access_token")?.asString ?: ""
                val ref = json.get("refresh_token")?.asString
                if (!ref.isNullOrBlank()) newRefreshToken = ref
            }

            if (newAccessToken.isBlank()) {
                return@withContext Result.failure(Exception("Token pembaruan kosong."))
            }

            // Data lokal (skin kustom, jubah, model skin, id) dipertahankan; hanya token dan profil yang baru.
            completeMinecraftAuth(newAccessToken, newRefreshToken) { _ -> }.map { fresh ->
                fresh.copy(
                    id = account.id.ifBlank { fresh.id },
                    email = account.email,
                    isActivated = account.isActivated,
                    tier = account.tier,
                    photoUrl = account.photoUrl,
                    skinModel = account.skinModel,
                    customSkinPath = account.customSkinPath,
                    customCapePath = account.customCapePath
                )
            }
        } catch (e: Exception) {
            Result.failure(friendly(e))
        }
    }
}

// ----------------------------------------------------------------------------
// Section: core/account/offline/OfflineYggdrasilServer.kt
// ----------------------------------------------------------------------------

/**
 * Local embedded Yggdrasil session server for offline Minecraft accounts.
 * Enables in-game custom skin & cape rendering via authlib-injector (matching Zalith Launcher & HMCL).
 */
class OfflineYggdrasilServer(
    private val preferredPort: Int = 0
) {
    companion object {
        private const val TAG = "OfflineYggdrasil"

        /** Batas total permintaan (header + isi) dan isi permintaan yang diterima server lokal ini. */
        private const val MAX_REQUEST_BYTES = 256L * 1024L
        private const val MAX_BODY_BYTES = 64 * 1024

        var activeInstance: OfflineYggdrasilServer? = null
            private set
    }

    private val gson = Gson()
    private val isRunning = AtomicBoolean(false)
    private var serverSocket: ServerSocket? = null
    private val executor = Executors.newCachedThreadPool()

    private val keyPair: KeyPair = KeyPairGenerator.getInstance("RSA").apply {
        initialize(2048)
    }.genKeyPair()

    private val charactersByUuid = ConcurrentHashMap<String, Character>()
    private val charactersByName = ConcurrentHashMap<String, Character>()

    data class Character(
        val uuid: String,
        val name: String,
        val skinHash: String?,
        val skinBytes: ByteArray?,
        val isSlim: Boolean,
        val capeHash: String?,
        val capeBytes: ByteArray?
    )

    fun addCharacter(
        username: String,
        uuid: String,
        skinFile: File?,
        capeFile: File?,
        isSlim: Boolean
    ) {
        val cleanUuid = uuid.replace("-", "").lowercase()
        val skinBytes = if (skinFile != null && skinFile.exists() && skinFile.length() > 0L) {
            runCatching { skinFile.readBytes() }.getOrNull()
        } else null
        val skinHash = skinBytes?.let { sha256(it) }

        val capeBytes = if (capeFile != null && capeFile.exists() && capeFile.length() > 0L) {
            runCatching { capeFile.readBytes() }.getOrNull()
        } else null
        val capeHash = capeBytes?.let { sha256(it) }

        val character = Character(
            uuid = cleanUuid,
            name = username,
            skinHash = skinHash,
            skinBytes = skinBytes,
            isSlim = isSlim,
            capeHash = capeHash,
            capeBytes = capeBytes
        )

        charactersByUuid[cleanUuid] = character
        charactersByName[username.lowercase()] = character
        Log.i(TAG, "Registered offline character: $username ($cleanUuid) with skin=${skinHash != null}, cape=${capeHash != null}")
    }

    @Synchronized
    fun start(): Int {
        val existingSocket = serverSocket
        if (isRunning.get() && existingSocket != null && !existingSocket.isClosed) {
            return existingSocket.localPort
        }

        try {
            // Hanya menerima koneksi dari HP ini sendiri (loopback), bukan dari Wi-Fi atau jaringan lain.
            val socket = ServerSocket(preferredPort, 50, InetAddress.getByAddress(byteArrayOf(127, 0, 0, 1)))
            serverSocket = socket
            isRunning.set(true)
            activeInstance = this

            val port = socket.localPort
            Log.i(TAG, "Offline Yggdrasil server listening on port $port")

            executor.execute {
                while (isRunning.get() && !socket.isClosed) {
                    try {
                        val client = socket.accept()
                        executor.execute {
                            handleClient(client)
                        }
                    } catch (e: Exception) {
                        if (!isRunning.get()) break
                    }
                }
            }

            return port
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start offline Yggdrasil server: ${e.message}", e)
            return -1
        }
    }

    @Synchronized
    fun stop() {
        isRunning.set(false)
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
        if (activeInstance == this) {
            activeInstance = null
        }
        Log.i(TAG, "Offline Yggdrasil server stopped")
    }

    fun getPort(): Int {
        return serverSocket?.localPort ?: -1
    }

    private fun handleClient(socket: Socket) {
        try {
            socket.soTimeout = 10000
            // Kiriman dibatasi supaya permintaan tanpa akhir tidak menghabiskan memori.
            val input = LimitedInputStream(socket.getInputStream(), MAX_REQUEST_BYTES)
            val output = socket.getOutputStream()
            val reader = BufferedReader(InputStreamReader(input, Charsets.UTF_8))

            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return

            val method = parts[0].uppercase()
            val uri = parts[1]

            // Read headers
            val headers = mutableMapOf<String, String>()
            var contentLength = 0
            while (true) {
                val line = reader.readLine() ?: break
                if (line.isEmpty()) break
                val colonIdx = line.indexOf(':')
                if (colonIdx > 0) {
                    val key = line.substring(0, colonIdx).trim().lowercase()
                    val value = line.substring(colonIdx + 1).trim()
                    headers[key] = value
                    if (key == "content-length") {
                        contentLength = value.toIntOrNull() ?: 0
                    }
                }
            }

            if (contentLength > MAX_BODY_BYTES) return

            // Read body if POST
            var body = ""
            if (contentLength > 0) {
                val bodyChars = CharArray(contentLength)
                var readTotal = 0
                while (readTotal < contentLength) {
                    val read = reader.read(bodyChars, readTotal, contentLength - readTotal)
                    if (read == -1) break
                    readTotal += read
                }
                body = String(bodyChars, 0, readTotal)
            }

            val path = if (uri.contains('?')) uri.substringBefore('?') else uri

            when {
                path == "/" || path.isEmpty() -> {
                    val pem = "-----BEGIN PUBLIC KEY-----\n" +
                            java.util.Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(keyPair.public.encoded) +
                            "\n-----END PUBLIC KEY-----\n"

                    val json = JsonObject().apply {
                        val skinDomains = JsonArray().apply {
                            add("127.0.0.1")
                            add("localhost")
                        }
                        add("skinDomains", skinDomains)
                        val meta = JsonObject().apply {
                          addProperty("serverName", "NovaCraft_Offline")
                          addProperty("implementationName", "NovaCraft")
                          addProperty("implementationVersion", "1.0")
                          addProperty("feature.non_email_login", true)
                        }
                        add("meta", meta)
                        addProperty("signaturePublickey", pem)
                    }
                    sendJsonResponse(output, 200, json.toString())
                }

                path == "/status" -> {
                    val json = JsonObject().apply {
                        addProperty("user.count", charactersByUuid.size)
                        addProperty("token.count", 0)
                    }
                    sendJsonResponse(output, 200, json.toString())
                }

                path == "/api/profiles/minecraft" && method == "POST" -> {
                    val requestedNames = runCatching {
                        gson.fromJson(body, Array<String>::class.java)
                    }.getOrNull() ?: emptyArray()

                    val profilesArray = JsonArray()
                    requestedNames.distinct().forEach { name ->
                        val char = charactersByName[name.lowercase()]
                        if (char != null) {
                            val obj = JsonObject().apply {
                                addProperty("id", char.uuid)
                                addProperty("name", char.name)
                            }
                            profilesArray.add(obj)
                        }
                    }
                    sendJsonResponse(output, 200, profilesArray.toString())
                }

                (path == "/sessionserver/session/minecraft/join" || path == "/session/minecraft/join") && method == "POST" -> {
                    sendEmptyResponse(output, 204, "No Content")
                }

                path == "/sessionserver/session/minecraft/hasJoined" || path == "/session/minecraft/hasJoined" -> {
                    val query = if (uri.contains('?')) uri.substringAfter('?') else ""
                    val username = parseQueryParam(query, "username")
                    val char = if (username != null) charactersByName[username.lowercase()] else null
                    if (char != null) {
                        val port = getPort()
                        val profileJson = buildSignedProfileJson(char, port)
                        sendJsonResponse(output, 200, profileJson)
                    } else {
                        sendEmptyResponse(output, 204, "No Content")
                    }
                }

                path.startsWith("/sessionserver/session/minecraft/profile/") || path.startsWith("/session/minecraft/profile/") -> {
                    val uuid = path.substringAfter("/profile/").replace("-", "").lowercase()
                    val char = charactersByUuid[uuid]
                    if (char != null) {
                        val port = getPort()
                        val profileJson = buildSignedProfileJson(char, port)
                        sendJsonResponse(output, 200, profileJson)
                    } else {
                        sendEmptyResponse(output, 204, "No Content")
                    }
                }

                path.startsWith("/textures/") -> {
                    val hash = path.substringAfter("/textures/")
                    val matchingBytes = charactersByUuid.values.firstNotNullOfOrNull { c ->
                        when (hash) {
                            c.skinHash -> c.skinBytes
                            c.capeHash -> c.capeBytes
                            else -> null
                        }
                    }

                    if (matchingBytes != null) {
                        sendBinaryResponse(output, 200, matchingBytes, "image/png", hash)
                    } else {
                        sendEmptyResponse(output, 404, "Not Found")
                    }
                }

                else -> {
                    sendEmptyResponse(output, 404, "Not Found")
                }
            }
        } catch (_: Exception) {
        } finally {
            runCatching { socket.close() }
        }
    }

    private fun buildSignedProfileJson(character: Character, port: Int): String {
        val rootUrl = "http://127.0.0.1:$port"

        val texturesObj = JsonObject().apply {
            addProperty("timestamp", System.currentTimeMillis())
            addProperty("profileId", character.uuid)
            addProperty("profileName", character.name)

            val innerTextures = JsonObject()

            if (character.skinHash != null) {
                val skinObj = JsonObject().apply {
                    addProperty("url", "$rootUrl/textures/${character.skinHash}")
                    if (character.isSlim) {
                        val metaObj = JsonObject().apply {
                            addProperty("model", "slim")
                        }
                        add("metadata", metaObj)
                    }
                }
                innerTextures.add("SKIN", skinObj)
            }

            if (character.capeHash != null) {
                val capeObj = JsonObject().apply {
                    addProperty("url", "$rootUrl/textures/${character.capeHash}")
                }
                innerTextures.add("CAPE", capeObj)
            }

            add("textures", innerTextures)
        }

        val jsonString = texturesObj.toString()
        val base64Textures = java.util.Base64.getEncoder().encodeToString(jsonString.toByteArray(Charsets.UTF_8))
        val signature = signWithRsa(base64Textures)

        val root = JsonObject().apply {
            addProperty("id", character.uuid)
            addProperty("name", character.name)
            val properties = JsonArray().apply {
                val prop = JsonObject().apply {
                    addProperty("name", "textures")
                    addProperty("value", base64Textures)
                    addProperty("signature", signature)
                }
                add(prop)
            }
            add("properties", properties)
        }

        return root.toString()
    }

    private fun signWithRsa(data: String): String {
        val sig = Signature.getInstance("SHA1withRSA")
        sig.initSign(keyPair.private)
        sig.update(data.toByteArray(Charsets.UTF_8))
        return java.util.Base64.getEncoder().encodeToString(sig.sign())
    }

    private fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(bytes)
        return hash.joinToString("") { "%02x".format(it) }
    }

    private fun parseQueryParam(query: String, param: String): String? {
        val pairs = query.split("&")
        for (pair in pairs) {
            val idx = pair.indexOf('=')
            if (idx > 0 && pair.substring(0, idx) == param) {
                return pair.substring(idx + 1)
            }
        }
        return null
    }

    private fun sendJsonResponse(output: OutputStream, status: Int, json: String) {
        val bytes = json.toByteArray(Charsets.UTF_8)
        val writer = PrintWriter(OutputStreamWriter(output, Charsets.UTF_8))
        writer.print("HTTP/1.1 $status OK\r\n")
        writer.print("Content-Type: application/json; charset=utf-8\r\n")
        writer.print("Content-Length: ${bytes.size}\r\n")
        writer.print("Connection: close\r\n")
        writer.print("\r\n")
        writer.flush()
        output.write(bytes)
        output.flush()
    }

    private fun sendBinaryResponse(output: OutputStream, status: Int, bytes: ByteArray, mimeType: String, etag: String) {
        val writer = PrintWriter(OutputStreamWriter(output, Charsets.UTF_8))
        writer.print("HTTP/1.1 $status OK\r\n")
        writer.print("Content-Type: $mimeType\r\n")
        writer.print("Content-Length: ${bytes.size}\r\n")
        writer.print("Cache-Control: max-age=2592000, public\r\n")
        writer.print("ETag: \"$etag\"\r\n")
        writer.print("Connection: close\r\n")
        writer.print("\r\n")
        writer.flush()
        output.write(bytes)
        output.flush()
    }

    private fun sendEmptyResponse(output: OutputStream, status: Int, statusText: String) {
        val writer = PrintWriter(OutputStreamWriter(output, Charsets.UTF_8))
        writer.print("HTTP/1.1 $status $statusText\r\n")
        writer.print("Content-Length: 0\r\n")
        writer.print("Connection: close\r\n")
        writer.print("\r\n")
        writer.flush()
    }
}

// ----------------------------------------------------------------------------
// Section: core/skin/NovaSkinServerAndWardrobe.kt
// ----------------------------------------------------------------------------

// ============================================================================
// NovaCraft Unified Offline Yggdrasil Skin Server & Microsoft Wardrobe Service
// ============================================================================

private const val OFFLINE_SKIN_TAG = "OfflineSkinServer"
private const val MAX_SKIN_REQUEST_BYTES = 256L * 1024L
private const val MAX_SKIN_BODY_BYTES = 64 * 1024

object OfflineSkinServerManager {
    private var instance: OfflineSkinServer? = null

    @Synchronized
    fun getOrCreateServer(): OfflineSkinServer {
        return instance ?: OfflineSkinServer().also { instance = it }
    }

    @Synchronized
    fun stopServer() {
        instance?.stop()
        instance = null
    }
}

class OfflineSkinServer {
    private var serverSocket: ServerSocket? = null
    private val isRunning = AtomicBoolean(false)
    private var serverThread: Thread? = null

    private val keyPair: KeyPair by lazy {
        KeyPairGenerator.getInstance("RSA").apply {
            initialize(2048)
        }.genKeyPair()
    }

    private val charactersByUuid = ConcurrentHashMap<String, CharacterData>()
    private val charactersByName = ConcurrentHashMap<String, CharacterData>()
    private val texturesByHash = ConcurrentHashMap<String, ByteArray>()

    var port: Int = 0
        private set

    data class CharacterData(
        val uuid: String,
        val name: String,
        val isSlim: Boolean,
        val skinHash: String?,
        val capeHash: String?
    )

    fun start(): Int {
        if (isRunning.get()) return port

        val socket = ServerSocket(0, 50, InetAddress.getByAddress(byteArrayOf(127, 0, 0, 1)))
        port = socket.localPort
        serverSocket = socket
        isRunning.set(true)

        serverThread = Thread({
            Log.d(OFFLINE_SKIN_TAG, "OfflineSkinServer started on port $port")
            while (isRunning.get()) {
                try {
                    val client = socket.accept()
                    Thread({ handleClient(client) }, "OfflineSkinClient-${client.port}").start()
                } catch (e: Exception) {
                    if (!isRunning.get()) break
                    Log.e(OFFLINE_SKIN_TAG, "Accept error: ${e.message}")
                }
            }
        }, "OfflineSkinServerThread").apply {
            isDaemon = true
            start()
        }

        return port
    }

    fun stop() {
        isRunning.set(false)
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
        charactersByUuid.clear()
        charactersByName.clear()
        texturesByHash.clear()
        Log.d(OFFLINE_SKIN_TAG, "OfflineSkinServer stopped")
    }

    fun registerAccount(account: UserAccount) {
        val cleanUuid = account.uuid.replace("-", "").lowercase()
        val cleanName = account.username.trim()

        var skinHash: String? = null
        if (!account.customSkinPath.isNullOrBlank()) {
            val skinFile = File(account.customSkinPath)
            if (skinFile.exists()) {
                val bytes = skinFile.readBytes()
                val hash = sha256(bytes)
                texturesByHash[hash] = bytes
                skinHash = hash
            }
        }

        var capeHash: String? = null
        if (!account.customCapePath.isNullOrBlank()) {
            val capeFile = File(account.customCapePath)
            if (capeFile.exists()) {
                val bytes = capeFile.readBytes()
                val hash = sha256(bytes)
                texturesByHash[hash] = bytes
                capeHash = hash
            }
        }

        val charData = CharacterData(
            uuid = cleanUuid,
            name = cleanName,
            isSlim = account.safeSkinModel.lowercase() == "slim",
            skinHash = skinHash,
            capeHash = capeHash
        )

        charactersByUuid[cleanUuid] = charData
        charactersByName[cleanName.lowercase()] = charData
        Log.d(OFFLINE_SKIN_TAG, "Registered character: ${charData.name} (UUID: ${charData.uuid})")
    }

    private fun handleClient(socket: Socket) {
        try {
            socket.soTimeout = 10000
            val input = BufferedReader(
                InputStreamReader(LimitedInputStream(socket.getInputStream(), MAX_SKIN_REQUEST_BYTES), Charsets.UTF_8)
            )
            val output = BufferedOutputStream(socket.getOutputStream())

            val requestLine = input.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return

            val path = parts[1]

            var contentLength = 0
            var headerLine: String?
            while (input.readLine().also { headerLine = it } != null) {
                val line = headerLine
                if (line.isNullOrBlank()) break
                val lower = line.lowercase()
                if (lower.startsWith("content-length:")) {
                    contentLength = lower.substringAfter(":").trim().toIntOrNull() ?: 0
                }
            }

            if (contentLength > MAX_SKIN_BODY_BYTES) return

            var requestBody = ""
            if (contentLength > 0) {
                val chars = CharArray(contentLength)
                var read = 0
                while (read < contentLength) {
                    val r = input.read(chars, read, contentLength - read)
                    if (r == -1) break
                    read += r
                }
                requestBody = String(chars, 0, read)
            }

            when {
                path == "/" || path == "/api" -> {
                    val pem = exportPublicKeyToPEM()
                    val json = """{"skinDomains":["127.0.0.1","localhost"],"meta":{"serverName":"NovaCraft_Offline","implementationName":"NovaCraft","implementationVersion":"1.0","feature.non_email_login":true},"signaturePublickey":"$pem"}"""
                    sendResponse(output, 200, "application/json", json.toByteArray(Charsets.UTF_8))
                }
                path == "/status" -> {
                    val json = """{"user.count":${charactersByUuid.size},"token.count":0}"""
                    sendResponse(output, 200, "application/json", json.toByteArray(Charsets.UTF_8))
                }
                path.startsWith("/api/profiles/minecraft") -> {
                    val results = mutableListOf<String>()
                    for ((nameLower, charData) in charactersByName) {
                        if (requestBody.contains("\"$nameLower\"", ignoreCase = true) || requestBody.contains("\"${charData.name}\"", ignoreCase = true) || requestBody.isEmpty()) {
                            results.add("""{"id":"${charData.uuid}","name":"${charData.name}"}""")
                        }
                    }
                    if (results.isEmpty() && charactersByName.isNotEmpty()) {
                        val first = charactersByName.values.first()
                        results.add("""{"id":"${first.uuid}","name":"${first.name}"}""")
                    }
                    val resp = "[${results.joinToString(",")}]"
                    sendResponse(output, 200, "application/json", resp.toByteArray(Charsets.UTF_8))
                }
                path.startsWith("/sessionserver/session/minecraft/join") -> {
                    sendResponse(output, 204, "application/json", ByteArray(0))
                }
                path.startsWith("/sessionserver/session/minecraft/hasJoined") -> {
                    val query = path.substringAfter("?", "")
                    val rawUsername = query.split("&")
                        .firstOrNull { it.startsWith("username=") }
                        ?.substringAfter("username=")
                    val username = if (!rawUsername.isNullOrBlank()) {
                        runCatching { java.net.URLDecoder.decode(rawUsername, "UTF-8") }.getOrDefault(rawUsername)
                    } else null
                    val charData = if (!username.isNullOrBlank()) charactersByName[username.lowercase()] else null
                    if (charData != null) {
                        val resp = buildProfileResponse(charData)
                        sendResponse(output, 200, "application/json", resp.toByteArray(Charsets.UTF_8))
                    } else {
                        sendResponse(output, 204, "application/json", ByteArray(0))
                    }
                }
                path.startsWith("/sessionserver/session/minecraft/profile/") -> {
                    val rawUuid = path.substringAfter("/sessionserver/session/minecraft/profile/").substringBefore("?").replace("-", "").lowercase()
                    val charData = charactersByUuid[rawUuid]
                    if (charData != null) {
                        val resp = buildProfileResponse(charData)
                        sendResponse(output, 200, "application/json", resp.toByteArray(Charsets.UTF_8))
                    } else {
                        sendResponse(output, 204, "application/json", ByteArray(0))
                    }
                }
                path.startsWith("/textures/") -> {
                    val hash = path.substringAfter("/textures/").substringBefore("?")
                    val textureBytes = texturesByHash[hash]
                    if (textureBytes != null) {
                        sendResponse(output, 200, "image/png", textureBytes)
                    } else {
                        sendResponse(output, 404, "text/plain", "Not found".toByteArray(Charsets.UTF_8))
                    }
                }
                else -> {
                    sendResponse(output, 204, "application/json", ByteArray(0))
                }
            }
        } catch (_: Exception) {
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private fun buildProfileResponse(charData: CharacterData): String {
        val rootUrl = "http://127.0.0.1:$port"
        val skinBlock = if (charData.skinHash != null) {
            val modelAttr = if (charData.isSlim) ""","metadata":{"model":"slim"}""" else ""
            """"SKIN":{"url":"$rootUrl/textures/${charData.skinHash}"$modelAttr}"""
        } else null

        val capeBlock = if (charData.capeHash != null) {
            """"CAPE":{"url":"$rootUrl/textures/${charData.capeHash}"}"""
        } else null

        val texturesInner = listOfNotNull(skinBlock, capeBlock).joinToString(",")
        val texturesJson = """{"timestamp":${System.currentTimeMillis()},"profileId":"${charData.uuid}","profileName":"${charData.name}","textures":{$texturesInner}}"""
        val base64Value = Base64.encodeToString(texturesJson.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        val signature = signData(base64Value)

        return """{"id":"${charData.uuid}","name":"${charData.name}","properties":[{"name":"textures","value":"$base64Value","signature":"$signature"}]}"""
    }

    private fun signData(data: String): String {
        val sig = Signature.getInstance("SHA1withRSA")
        sig.initSign(keyPair.private)
        sig.update(data.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(sig.sign(), Base64.NO_WRAP)
    }

    private fun exportPublicKeyToPEM(): String {
        val base64Key = Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP)
        return "-----BEGIN PUBLIC KEY-----\\n$base64Key\\n-----END PUBLIC KEY-----"
    }

    private fun sha256(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun sendResponse(output: BufferedOutputStream, status: Int, contentType: String, body: ByteArray) {
        val statusText = when (status) {
            200 -> "OK"
            204 -> "No Content"
            404 -> "Not Found"
            else -> "OK"
        }
        val header = "HTTP/1.1 $status $statusText\r\n" +
            "Content-Type: $contentType\r\n" +
            "Content-Length: ${body.size}\r\n" +
            "Connection: close\r\n\r\n"
        output.write(header.toByteArray(Charsets.UTF_8))
        if (body.isNotEmpty()) {
            output.write(body)
        }
        output.flush()
    }
}

data class MicrosoftCape(
    val id: String,
    var state: String = "INACTIVE",
    val url: String,
    val alias: String? = null
)

data class MicrosoftProfileResponse(
    val id: String,
    val name: String,
    val skins: List<MicrosoftSkinItem> = emptyList(),
    val capes: List<MicrosoftCape> = emptyList()
)

data class MicrosoftSkinItem(
    val id: String,
    val state: String = "INACTIVE",
    val url: String,
    val variant: String = "CLASSIC"
)

sealed class SessionCape {
    class Found(val url: String) : SessionCape()
    object None : SessionCape()
    object Failed : SessionCape()
}

object MicrosoftWardrobeService {
    private const val MINECRAFT_SERVICES_URL = "https://api.minecraftservices.com"
    private const val MOJANG_SESSION_URL = "https://sessionserver.mojang.com"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    fun getCapeDisplayName(alias: String?): String {
        return when (alias) {
            "Migrator" -> "Migrator Cape"
            "Vanilla" -> "Vanilla Cape"
            "Cherry Blossom" -> "Cherry Blossom Cape"
            "15th Anniversary" -> "15th Anniversary Cape"
            "Purple Heart" -> "Purple Heart Cape (Twitch)"
            "Follower's" -> "Follower's Cape (TikTok)"
            "MCC 15th Year" -> "MCC 15th Year Cape"
            "Minecon2011" -> "Minecon 2011 Cape"
            "Minecon2012" -> "Minecon 2012 Cape"
            "Minecon2013" -> "Minecon 2013 Cape"
            "Minecon2015" -> "Minecon 2015 Cape"
            "Minecon2016" -> "Minecon 2016 Cape"
            "Minecraft Experience" -> "Minecraft Experience Cape"
            "Mojang Office" -> "Mojang Office Cape"
            "Home" -> "Home Cape"
            "Menace" -> "Menace Cape"
            "Yearn" -> "Yearn Cape"
            "Common" -> "Common Cape"
            null, "" -> "Jubah Resmi Minecraft"
            else -> "$alias Cape"
        }
    }

    suspend fun lookupSessionCape(uuid: String): SessionCape = withContext(Dispatchers.IO) {
        try {
            val cleanUuid = uuid.replace("-", "")
            val req = Request.Builder()
                .url("$MOJANG_SESSION_URL/session/minecraft/profile/$cleanUuid")
                .get()
                .build()

            httpClient.newCall(req).execute().use { resp ->
                if (resp.code == 204 || resp.code == 404) return@withContext SessionCape.None
                if (!resp.isSuccessful) return@withContext SessionCape.Failed
                val body = resp.body?.string().orEmpty()
                if (body.isBlank()) return@withContext SessionCape.Failed
                val json = JsonParser.parseString(body).asJsonObject
                val props = json.getAsJsonArray("properties") ?: return@withContext SessionCape.None
                for (p in props) {
                    val pObj = p.asJsonObject
                    if (pObj.get("name")?.asString == "textures") {
                        val base64Val = pObj.get("value")?.asString ?: continue
                        val decodedBytes = Base64.decode(base64Val, Base64.DEFAULT)
                        val decodedJson = JsonParser.parseString(String(decodedBytes)).asJsonObject
                        val textures = decodedJson.getAsJsonObject("textures") ?: continue
                        val cape = textures.getAsJsonObject("CAPE")
                        val capeUrl = cape?.get("url")?.asString
                        if (!capeUrl.isNullOrBlank()) {
                            return@withContext SessionCape.Found(
                                if (capeUrl.startsWith("http://")) capeUrl.replace("http://", "https://") else capeUrl
                            )
                        }
                    }
                }
                SessionCape.None
            }
        } catch (_: Exception) {
            SessionCape.Failed
        }
    }

    suspend fun getSessionActiveCape(uuid: String): String? =
        (lookupSessionCape(uuid) as? SessionCape.Found)?.url

    suspend fun getProfile(
        context: Context,
        account: UserAccount
    ): Result<Pair<UserAccount, MicrosoftProfileResponse>> = withContext(Dispatchers.IO) {
        var currentAcc = account
        var firstResult = fetchProfileWithToken(currentAcc.safeAccessToken)

        if (firstResult.isFailure && currentAcc.safeRefreshToken.isNotBlank() && currentAcc.safeRefreshToken != "0") {
            val refreshResult = MicrosoftAuthService.refreshMicrosoftAccount(currentAcc)
            if (refreshResult.isSuccess) {
                currentAcc = refreshResult.getOrThrow()
                AccountManager.updateAccount(context, currentAcc)
                firstResult = fetchProfileWithToken(currentAcc.safeAccessToken)
            }
        }

        val sessionCapeUrl = getSessionActiveCape(currentAcc.uuid)

        if (firstResult.isSuccess) {
            val baseProf = firstResult.getOrThrow()
            val capesList = baseProf.capes.toMutableList()

            if (sessionCapeUrl != null) {
                var foundMatch = false
                for (cape in capesList) {
                    val safeCapeUrl = if (cape.url.startsWith("http://")) cape.url.replace("http://", "https://") else cape.url
                    if (safeCapeUrl == sessionCapeUrl || cape.url == sessionCapeUrl) {
                        cape.state = "ACTIVE"
                        foundMatch = true
                    }
                }
                if (!foundMatch) {
                    capesList.add(
                        MicrosoftCape(
                            id = "mojang_active_cape",
                            state = "ACTIVE",
                            url = sessionCapeUrl,
                            alias = "Jubah Aktif Minecraft"
                        )
                    )
                }
            }

            val finalProf = baseProf.copy(capes = capesList)
            return@withContext Result.success(Pair(currentAcc, finalProf))
        }

        if (sessionCapeUrl != null) {
            val fallbackProf = MicrosoftProfileResponse(
                id = currentAcc.uuid,
                name = currentAcc.username,
                capes = listOf(
                    MicrosoftCape(
                        id = "mojang_active_cape",
                        state = "ACTIVE",
                        url = sessionCapeUrl,
                        alias = "Jubah Aktif Minecraft"
                    )
                )
            )
            return@withContext Result.success(Pair(currentAcc, fallbackProf))
        }

        Result.failure(firstResult.exceptionOrNull() ?: IOException("Gagal memuat jubah Minecraft."))
    }

    private fun fetchProfileWithToken(accessToken: String): Result<MicrosoftProfileResponse> {
        return try {
            val req = Request.Builder()
                .url("$MINECRAFT_SERVICES_URL/minecraft/profile")
                .header("Authorization", "Bearer $accessToken")
                .get()
                .build()

            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return Result.failure(IOException("HTTP ${resp.code}: ${resp.message}"))
                }
                val body = resp.body?.string() ?: return Result.failure(IOException("Respon profil kosong"))
                val profile = gson.fromJson(body, MicrosoftProfileResponse::class.java)
                Result.success(profile)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun changeCape(
        context: Context,
        account: UserAccount,
        capeId: String?
    ): Result<UserAccount> = withContext(Dispatchers.IO) {
        var currentAcc = account
        var res = doChangeCapeCall(currentAcc.safeAccessToken, capeId)
        if (res.isFailure && currentAcc.safeRefreshToken.isNotBlank() && currentAcc.safeRefreshToken != "0") {
            val refreshResult = MicrosoftAuthService.refreshMicrosoftAccount(currentAcc)
            if (refreshResult.isSuccess) {
                currentAcc = refreshResult.getOrThrow()
                AccountManager.updateAccount(context, currentAcc)
                res = doChangeCapeCall(currentAcc.safeAccessToken, capeId)
            }
        }

        if (res.isSuccess) {
            Result.success(currentAcc)
        } else {
            Result.failure(res.exceptionOrNull() ?: IOException("Gagal mengubah jubah."))
        }
    }

    private fun doChangeCapeCall(accessToken: String, capeId: String?): Result<Unit> {
        return try {
            val url = "$MINECRAFT_SERVICES_URL/minecraft/profile/capes/active"
            val req = if (capeId.isNullOrBlank() || capeId == "mojang_active_cape") {
                Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer $accessToken")
                    .delete()
                    .build()
            } else {
                val jsonBody = JsonObject().apply {
                    addProperty("capeId", capeId)
                }.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

                Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer $accessToken")
                    .put(jsonBody)
                    .build()
            }

            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful && resp.code != 200 && resp.code != 204) {
                    return Result.failure(IOException("HTTP ${resp.code}: ${resp.message}"))
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadSkin(
        context: Context,
        account: UserAccount,
        file: File,
        isSlim: Boolean
    ): Result<UserAccount> = withContext(Dispatchers.IO) {
        var currentAcc = account
        var res = doUploadSkinCall(currentAcc.safeAccessToken, file, isSlim)
        if (res.isFailure && currentAcc.safeRefreshToken.isNotBlank() && currentAcc.safeRefreshToken != "0") {
            val refreshResult = MicrosoftAuthService.refreshMicrosoftAccount(currentAcc)
            if (refreshResult.isSuccess) {
                currentAcc = refreshResult.getOrThrow()
                AccountManager.updateAccount(context, currentAcc)
                res = doUploadSkinCall(currentAcc.safeAccessToken, file, isSlim)
            }
        }

        if (res.isSuccess) {
            Result.success(currentAcc)
        } else {
            Result.failure(res.exceptionOrNull() ?: IOException("Gagal mengunggah skin."))
        }
    }

    private fun doUploadSkinCall(accessToken: String, file: File, isSlim: Boolean): Result<Unit> {
        return try {
            val variant = if (isSlim) "slim" else "classic"
            val fileBody = file.readBytes().toRequestBody("image/png".toMediaType())
            val multipartBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("variant", variant)
                .addFormDataPart("file", file.name, fileBody)
                .build()

            val req = Request.Builder()
                .url("$MINECRAFT_SERVICES_URL/minecraft/profile/skins")
                .header("Authorization", "Bearer $accessToken")
                .post(multipartBody)
                .build()

            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful && resp.code != 200 && resp.code != 204) {
                    return Result.failure(IOException("HTTP ${resp.code}: ${resp.message}"))
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// ----------------------------------------------------------------------------
// Section: core/skin/SkinUtils.kt
// ----------------------------------------------------------------------------

data class CapePreset(
    val id: String,
    val name: String,
    val description: String,
    val badgeColor: Long
)

object SkinUtils {

    private val headCache = LruCache<String, Bitmap>(60)

    val PRESET_CAPES = listOf(
        CapePreset("none", "Tanpa Cape", "Reset / Lepas cape", 0xFF6B7280),
        CapePreset("mojang_red", "Mojang Studios", "Klasik merah resmi Mojang", 0xFFEF4444),
        CapePreset("anniversary_15", "15th Anniversary", "Creeper emas peringatan 15 tahun", 0xFF10B981),
        CapePreset("migrator", "Migrator Cape", "Jubah biru kehormatan migrasi", 0xFF3B82F6),
        CapePreset("cherry_blossom", "Cherry Blossom", "Motif sakura edisi 1.20", 0xFFEC4899),
        CapePreset("founders", "Founder's Cape", "Edisi emas pencipta", 0xFFF59E0B),
        CapePreset("vanilla", "Vanilla Cape", "Edisi Java & Bedrock bundle", 0xFF8B5CF6)
    )

    fun getSkinsDir(context: Context): File {
        val dir = File(context.filesDir, "skins")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getCapesDir(context: Context): File {
        val dir = File(context.filesDir, "capes")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getSkinFileForAccount(context: Context, accountId: String): File {
        return File(getSkinsDir(context), "${accountId}_skin.png")
    }

    fun getCapeFileForAccount(context: Context, accountId: String): File {
        return File(getCapesDir(context), "${accountId}_cape.png")
    }

    fun resolveSkinFile(context: Context, account: UserAccount): File? {
        account.getSkinFile()?.let { if (it.exists() && it.length() > 0) return it }
        val defaultFile = getSkinFileForAccount(context, account.id)
        return if (defaultFile.exists() && defaultFile.length() > 0) defaultFile else null
    }

    fun resolveCapeFile(context: Context, account: UserAccount): File? {
        account.getCapeFile()?.let { if (it.exists() && it.length() > 0) return it }
        val defaultFile = getCapeFileForAccount(context, account.id)
        return if (defaultFile.exists() && defaultFile.length() > 0) defaultFile else null
    }

    fun invalidateHeadCache(accountId: String? = null) {
        headCache.evictAll()
    }

    /**
     * Extracts a 2D Minecraft face avatar by compositing:
     * - Base Head layer: Rect(8, 8, 16, 16)
     * - Hat / Helmet outer layer: Rect(40, 8, 48, 16)
     * Scales up with nearest-neighbor interpolation to preserve crisp Minecraft pixel art.
     */
    fun extractHeadFromSkin(skinBitmap: Bitmap, targetSize: Int = 128): Bitmap {
        val headBitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(headBitmap)
        val paint = Paint().apply {
            isFilterBitmap = false // Nearest-neighbor: essential for sharp pixel art
            isAntiAlias = false
            isDither = false
        }

        val scale = (skinBitmap.width / 64).coerceAtLeast(1)
        val dst = Rect(0, 0, targetSize, targetSize)

        // 1. Base head front face
        val srcHead = Rect(8 * scale, 8 * scale, 16 * scale, 16 * scale)
        canvas.drawBitmap(skinBitmap, srcHead, dst, paint)

        // 2. Outer hat/helmet layer
        if (skinBitmap.width >= 48 * scale && skinBitmap.height >= 16 * scale) {
            val srcHat = Rect(40 * scale, 8 * scale, 48 * scale, 16 * scale)
            canvas.drawBitmap(skinBitmap, srcHat, dst, paint)
        }

        return headBitmap
    }

    fun getSteveHeadBitmap(context: Context, targetSize: Int = 128): Bitmap {
        val cacheKey = "steve_head_$targetSize"
        headCache.get(cacheKey)?.let { return it }

        val bitmap = runCatching {
            context.assets.open("steve.png").use { stream ->
                BitmapFactory.decodeStream(stream)
            }?.let { steveSkin ->
                try {
                    extractHeadFromSkin(steveSkin, targetSize)
                } finally {
                    steveSkin.recycle()
                }
            }
        }.getOrNull()

        val finalBitmap = bitmap ?: Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888).also { b ->
            Canvas(b).drawColor(Color.parseColor("#805335"))
        }
        headCache.put(cacheKey, finalBitmap)
        return finalBitmap
    }

    suspend fun getAccountHeadBitmap(
        context: Context,
        account: UserAccount,
        targetSize: Int = 128
    ): Bitmap = withContext(Dispatchers.IO) {
        val skinFile = resolveSkinFile(context, account)
        val fileModified = skinFile?.takeIf { it.exists() }?.lastModified() ?: 0L
        val cacheKey = "${account.safeAccountType}_${account.id}_${fileModified}_$targetSize"

        headCache.get(cacheKey)?.let { return@withContext it }

        // A. If local skin file exists, decode and extract head
        if (skinFile != null && skinFile.exists() && skinFile.length() > 0) {
            val skinBitmap = BitmapFactory.decodeFile(skinFile.absolutePath)
            if (skinBitmap != null) {
                val head = extractHeadFromSkin(skinBitmap, targetSize)
                skinBitmap.recycle()
                headCache.put(cacheKey, head)
                return@withContext head
            }
        }

        // B. If Ely.by account, ensure skin file is downloaded and cached
        if (account.safeAccountType == "elyby") {
            val cachedAcc = ensureSkinAndCapeCached(context, account)
            val downloadedSkinFile = resolveSkinFile(context, cachedAcc)
            if (downloadedSkinFile != null && downloadedSkinFile.exists() && downloadedSkinFile.length() > 0) {
                val skinBitmap = BitmapFactory.decodeFile(downloadedSkinFile.absolutePath)
                if (skinBitmap != null) {
                    val head = extractHeadFromSkin(skinBitmap, targetSize)
                    skinBitmap.recycle()
                    val newKey = "${account.safeAccountType}_${account.id}_${downloadedSkinFile.lastModified()}_$targetSize"
                    headCache.put(newKey, head)
                    return@withContext head
                }
            }
        }

        // C. If Microsoft account, fetch head from mc-heads.net
        if (account.safeAccountType == "microsoft" && account.uuid.isNotBlank()) {
            runCatching {
                val client = OkHttpClient()
                val req = Request.Builder().url("https://mc-heads.net/avatar/${account.uuid}/$targetSize").build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        resp.body?.byteStream()?.use { stream ->
                            BitmapFactory.decodeStream(stream)?.let { bmp ->
                                headCache.put(cacheKey, bmp)
                                return@withContext bmp
                            }
                        }
                    }
                }
            }
        }

        // D. Fallback to Steve head
        getSteveHeadBitmap(context, targetSize)
    }

    // ---- Skin & jubah Ely.by ----

    private const val ELY_SKINSYSTEM = "https://skinsystem.ely.by"
    private const val ELY_TEXTURE_TTL_MS = 30L * 60 * 1000 // cek ulang skin paling cepat tiap 30 menit
    private const val ELY_MISSING_RETRY_MS = 10L * 60 * 1000 // akun tanpa skin: jangan tanya server terus-menerus
    private const val MAX_TEXTURE_BYTES = 2_000_000

    // Pengalihan (redirect) diikuti manual di downloadTexture supaya alamat http:// bisa dinaikkan ke https://.
    private val textureClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    private val elyMissingUntil = ConcurrentHashMap<String, Long>()

    private enum class TextureFetch { SAVED, NOT_FOUND, FAILED }

    private class ElyTextures(val skinUrl: String?, val capeUrl: String?, val slim: Boolean?)

    /** Minta alamat skin/jubah (dan model slim/classic) ke Ely.by. Null bila gagal atau tidak ada data. */
    private fun fetchElyTextures(encodedName: String): ElyTextures? {
        return try {
            val request = Request.Builder().url("$ELY_SKINSYSTEM/textures/$encodedName").build()
            textureClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string().orEmpty()
                if (body.isBlank()) return null
                val root = JsonParser.parseString(body).asJsonObject
                val skin = root.getAsJsonObject("SKIN")
                val cape = root.getAsJsonObject("CAPE")
                ElyTextures(
                    skinUrl = skin?.get("url")?.asString,
                    capeUrl = cape?.get("url")?.asString,
                    slim = skin?.getAsJsonObject("metadata")?.get("model")?.asString
                        ?.let { it.equals("slim", ignoreCase = true) }
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun looksLikePng(bytes: ByteArray): Boolean =
        bytes.size > 8 &&
            bytes[0] == 0x89.toByte() &&
            bytes[1] == 0x50.toByte() && // P
            bytes[2] == 0x4E.toByte() && // N
            bytes[3] == 0x47.toByte() // G

    private fun writeAtomically(target: File, bytes: ByteArray): Boolean {
        val tmp = File(target.parentFile, "${target.name}.${System.nanoTime()}.tmp")
        return try {
            tmp.writeBytes(bytes)
            tmp.renameTo(target) || run {
                target.delete()
                tmp.renameTo(target)
            }
        } catch (_: IOException) {
            false
        } finally {
            if (tmp.exists()) tmp.delete()
        }
    }

    /**
     * Mengunduh satu tekstur PNG ke [target]. Server skin Ely.by menjawab dengan redirect, dan alamat
     * tujuannya bisa berupa http://. Aplikasi ini melarang cleartext (network_security_config), jadi
     * redirect diikuti manual dan setiap http:// dinaikkan ke https:// sebelum diminta.
     * SAVED = tersimpan, NOT_FOUND = pemain memang tidak punya tekstur itu, FAILED = jaringan/respon bermasalah.
     */
    private fun downloadTexture(startUrl: String, target: File): TextureFetch {
        var url = startUrl
        for (hop in 0 until 5) {
            val secureUrl = if (url.startsWith("http://")) "https://" + url.removePrefix("http://") else url
            val request = try {
                Request.Builder().url(secureUrl).build()
            } catch (_: IllegalArgumentException) {
                return TextureFetch.FAILED
            }

            val outcome: TextureFetch? = try {
                textureClient.newCall(request).execute().use { resp ->
                    when {
                        resp.isRedirect -> {
                            val next = resp.header("Location")?.let { resp.request.url.resolve(it)?.toString() }
                            if (next == null) {
                                TextureFetch.FAILED
                            } else {
                                url = next
                                null
                            }
                        }
                        resp.code == 404 || resp.code == 204 -> TextureFetch.NOT_FOUND
                        resp.isSuccessful -> {
                            val bytes = resp.body?.bytes()
                            when {
                                bytes == null -> TextureFetch.FAILED
                                bytes.isEmpty() -> TextureFetch.NOT_FOUND
                                bytes.size > MAX_TEXTURE_BYTES || !looksLikePng(bytes) -> TextureFetch.FAILED
                                writeAtomically(target, bytes) -> TextureFetch.SAVED
                                else -> TextureFetch.FAILED
                            }
                        }
                        else -> TextureFetch.FAILED
                    }
                }
            } catch (_: IOException) {
                TextureFetch.FAILED
            }
            if (outcome != null) return outcome
        }
        return TextureFetch.FAILED
    }

    /**
     * Menyegarkan skin dan jubah Ely.by di berkas lokal. Alamat tekstur diminta dulu lewat /textures
     * (sekaligus memberi model slim/classic); bila itu tidak membuahkan hasil dipakai /skins dan /cloaks.
     * Mengembalikan true bila skin slim, false bila classic, null bila tidak diketahui dari server.
     */
    private fun refreshElyByTextures(account: UserAccount, skinFile: File, capeFile: File): Boolean? {
        val encodedName = Uri.encode(account.username)
        val textures = fetchElyTextures(encodedName)

        var skinResult = textures?.skinUrl?.let { downloadTexture(it, skinFile) } ?: TextureFetch.NOT_FOUND
        if (skinResult != TextureFetch.SAVED) {
            skinResult = downloadTexture("$ELY_SKINSYSTEM/skins/$encodedName.png", skinFile)
        }

        var capeResult = textures?.capeUrl?.let { downloadTexture(it, capeFile) } ?: TextureFetch.NOT_FOUND
        if (capeResult != TextureFetch.SAVED) {
            capeResult = downloadTexture("$ELY_SKINSYSTEM/cloaks/$encodedName.png", capeFile)
        }

        when (skinResult) {
            TextureFetch.SAVED -> {
                elyMissingUntil.remove(account.id)
                invalidateHeadCache()
            }
            TextureFetch.NOT_FOUND -> {
                if (skinFile.exists()) skinFile.delete()
                elyMissingUntil[account.id] = System.currentTimeMillis() + ELY_MISSING_RETRY_MS
                invalidateHeadCache()
            }
            TextureFetch.FAILED -> Unit // jaringan bermasalah: pertahankan berkas lama, coba lagi nanti
        }
        if (capeResult == TextureFetch.NOT_FOUND && capeFile.exists()) capeFile.delete()

        return if (skinResult == TextureFetch.SAVED) textures?.slim else null
    }

    suspend fun ensureSkinAndCapeCached(context: Context, account: UserAccount): UserAccount = withContext(Dispatchers.IO) {
        var updatedAccount = account

        if (account.safeAccountType == "elyby") {
            val skinFile = getSkinFileForAccount(context, account.id)
            val capeFile = getCapeFileForAccount(context, account.id)

            val now = System.currentTimeMillis()
            val skinFresh = skinFile.exists() && skinFile.length() > 0 &&
                now - skinFile.lastModified() < ELY_TEXTURE_TTL_MS
            val recentlyMissing = (elyMissingUntil[account.id] ?: 0L) > now

            // Unduh bila belum ada atau sudah lama, supaya skin yang diganti di ely.by ikut berubah
            val shouldRefresh = !skinFresh && !recentlyMissing
            val slimFromServer = if (shouldRefresh) refreshElyByTextures(account, skinFile, capeFile) else null

            val hasSkin = skinFile.exists() && skinFile.length() > 0
            val hasCape = capeFile.exists() && capeFile.length() > 0
            val isSlim = when {
                !hasSkin -> false
                slimFromServer != null -> slimFromServer
                shouldRefresh -> isSlimModel(skinFile)
                else -> account.safeSkinModel == "slim"
            }

            updatedAccount = updatedAccount.copy(
                customSkinPath = if (hasSkin) skinFile.absolutePath else null,
                customCapePath = if (hasCape) capeFile.absolutePath else null,
                skinModel = if (isSlim) "slim" else "classic",
                skinUrl = "$ELY_SKINSYSTEM/skins/${account.username}.png"
            )

            if (updatedAccount != account) {
                AccountManager.updateAccount(context, updatedAccount)
            }
        } else if (account.isOffline) {
            val skinFile = getSkinFileForAccount(context, account.id)
            val capeFile = getCapeFileForAccount(context, account.id)

            var changed = false
            var newSkinPath = account.customSkinPath
            var newCapePath = account.customCapePath

            if (skinFile.exists() && skinFile.length() > 0 && account.customSkinPath.isNullOrBlank()) {
                newSkinPath = skinFile.absolutePath
                changed = true
            }
            if (capeFile.exists() && capeFile.length() > 0 && account.customCapePath.isNullOrBlank()) {
                newCapePath = capeFile.absolutePath
                changed = true
            }

            if (changed) {
                updatedAccount = updatedAccount.copy(
                    customSkinPath = newSkinPath,
                    customCapePath = newCapePath
                )
                AccountManager.updateAccount(context, updatedAccount)
            }
        }

        updatedAccount
    }

    // ---- Jubah akun Microsoft ----
    // Game memuat jubah Microsoft sendiri dari server Mojang, jadi launcher hanya perlu menirunya untuk
    // pratinjau: tanya Session Server (tanpa token) sekali tiap beberapa menit, lalu unduh PNG-nya bila berubah.

    private const val MOJANG_CAPE_TTL_MS = 10L * 60 * 1000 // tanya ulang Session Server paling cepat tiap 10 menit
    private const val MOJANG_CAPE_RETRY_MS = 60L * 1000 // bila Session Server gagal dijangkau, coba lagi semenit kemudian

    private class CapeLookup(val validUntil: Long, val url: String?)

    private val mojangCapes = ConcurrentHashMap<String, CapeLookup>()
    private val capeRevisionState = MutableStateFlow(0)

    /** Naik setiap jubah Microsoft diganti dari dalam aplikasi, supaya pratinjau memuat ulang. */
    val capeRevision: StateFlow<Int> = capeRevisionState

    private fun mojangCapeFile(context: Context, accountId: String) =
        File(getCapesDir(context), "${accountId}_mojang_cape.png")

    // Berisi alamat jubah yang sedang tersimpan di berkas PNG di atas.
    private fun mojangCapeMarker(context: Context, accountId: String) =
        File(getCapesDir(context), "${accountId}_mojang_cape.url")

    private fun isMojangTextureUrl(url: String): Boolean {
        val host = url.toHttpUrlOrNull()?.host ?: return false
        return host == "textures.minecraft.net" || host.endsWith(".minecraft.net") || host.endsWith(".mojang.com")
    }

    /**
     * Dipanggil setelah jubah Microsoft diganti di Minecraft.net. [url] adalah alamat jubah baru,
     * null berarti tanpa jubah. Pratinjau langsung memakai ini tanpa menunggu Session Server.
     */
    fun rememberMojangCape(accountId: String, url: String?) {
        mojangCapes[accountId] = CapeLookup(System.currentTimeMillis() + MOJANG_CAPE_TTL_MS, url)
        capeRevisionState.update { it + 1 }
    }

    /**
     * Berkas PNG jubah yang sedang aktif untuk akun Microsoft (sama dengan yang tampil di game), atau null
     * bila akun tidak punya jubah atau belum bisa diunduh. Tidak ada permintaan jaringan bila hasil terakhir
     * masih segar dan berkasnya sudah ada.
     */
    suspend fun ensureMojangCape(context: Context, account: UserAccount): File? = withContext(Dispatchers.IO) {
        if (account.uuid.isBlank()) return@withContext null
        val file = mojangCapeFile(context, account.id)
        val marker = mojangCapeMarker(context, account.id)
        val now = System.currentTimeMillis()

        val cached = mojangCapes[account.id]
        val known: CapeLookup = if (cached != null && now < cached.validUntil) {
            cached
        } else {
            val fresh = when (val found = MicrosoftWardrobeService.lookupSessionCape(account.uuid)) {
                is SessionCape.Found -> CapeLookup(now + MOJANG_CAPE_TTL_MS, found.url)
                is SessionCape.None -> CapeLookup(now + MOJANG_CAPE_TTL_MS, null)
                // Tidak bisa dipastikan (offline, dibatasi server): pakai jubah terakhir yang tersimpan
                is SessionCape.Failed -> CapeLookup(
                    now + MOJANG_CAPE_RETRY_MS,
                    if (file.exists()) runCatching { marker.readText().trim() }.getOrNull()?.takeIf { it.isNotEmpty() } else null
                )
            }
            mojangCapes[account.id] = fresh
            fresh
        }

        val url = known.url
        if (url == null) {
            if (file.exists()) file.delete()
            if (marker.exists()) marker.delete()
            return@withContext null
        }

        val stored = if (file.exists() && file.length() > 0) runCatching { marker.readText().trim() }.getOrNull() else null
        if (stored == url) return@withContext file
        if (!isMojangTextureUrl(url)) return@withContext null

        if (downloadTexture(url, file) == TextureFetch.SAVED) {
            runCatching { marker.writeText(url) }
            file
        } else {
            null
        }
    }

    suspend fun validateSkinFile(file: File): Boolean = withContext(Dispatchers.IO) {
        if (!file.exists() || file.length() <= 0) return@withContext false
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        (options.outWidth == 64 && options.outHeight == 64) || (options.outWidth == 64 && options.outHeight == 32)
    }

    suspend fun validateCapeFile(file: File): Boolean = withContext(Dispatchers.IO) {
        if (!file.exists() || file.length() <= 0) return@withContext false
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        (options.outWidth == 64 && options.outHeight == 32) || (options.outWidth == 128 && options.outHeight == 64)
    }

    suspend fun isSlimModel(file: File): Boolean = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext false
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        if (options.outWidth != 64 || options.outHeight != 64) {
            return@withContext false
        }

        val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return@withContext false
        try {
            var isTransparent = true
            for (x in 54..55) {
                for (y in 20..31) {
                    if (bitmap.getPixel(x, y) ushr 24 != 0) {
                        isTransparent = false
                        break
                    }
                }
                if (!isTransparent) break
            }
            isTransparent
        } catch (_: Exception) {
            false
        } finally {
            bitmap.recycle()
        }
    }

    suspend fun importSkinFromUri(context: Context, accountId: String, uri: Uri): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val targetFile = getSkinFileForAccount(context, accountId)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: throw Exception("Gagal membaca file dari penyimpanan.")

            if (!validateSkinFile(targetFile)) {
                targetFile.delete()
                throw Exception("Format skin tidak valid. Ukuran harus 64x64 atau 64x32 piksel.")
            }
            invalidateHeadCache(accountId)
            targetFile
        }
    }

    suspend fun importCapeFromUri(context: Context, accountId: String, uri: Uri): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val targetFile = getCapeFileForAccount(context, accountId)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: throw Exception("Gagal membaca file dari penyimpanan.")

            if (!validateCapeFile(targetFile)) {
                targetFile.delete()
                throw Exception("Format cape tidak valid. Ukuran harus 64x32 atau 128x64 piksel.")
            }
            targetFile
        }
    }

    fun createPresetCape(presetId: String): Bitmap? {
        if (presetId == "none") return null

        val bitmap = Bitmap.createBitmap(64, 32, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        when (presetId) {
            "mojang_red" -> {
                paint.color = Color.parseColor("#B31217")
                canvas.drawRect(Rect(0, 0, 22, 17), paint)
                paint.color = Color.WHITE
                canvas.drawRect(Rect(3, 5, 8, 7), paint)
                canvas.drawRect(Rect(3, 7, 5, 12), paint)
                canvas.drawRect(Rect(6, 7, 8, 12), paint)
                canvas.drawRect(Rect(3, 12, 8, 14), paint)
                canvas.drawRect(Rect(14, 5, 19, 7), paint)
                canvas.drawRect(Rect(14, 7, 16, 12), paint)
                canvas.drawRect(Rect(17, 7, 19, 12), paint)
                canvas.drawRect(Rect(14, 12, 19, 14), paint)
            }
            "anniversary_15" -> {
                paint.color = Color.parseColor("#15803D")
                canvas.drawRect(Rect(0, 0, 22, 17), paint)
                paint.color = Color.parseColor("#FBBF24")
                canvas.drawRect(Rect(3, 4, 5, 6), paint)
                canvas.drawRect(Rect(7, 4, 9, 6), paint)
                canvas.drawRect(Rect(5, 6, 7, 9), paint)
                canvas.drawRect(Rect(4, 8, 8, 10), paint)
                canvas.drawRect(Rect(3, 9, 5, 12), paint)
                canvas.drawRect(Rect(7, 9, 9, 12), paint)
                canvas.drawRect(Rect(14, 4, 16, 6), paint)
                canvas.drawRect(Rect(18, 4, 20, 6), paint)
                canvas.drawRect(Rect(16, 6, 18, 9), paint)
                canvas.drawRect(Rect(15, 8, 19, 10), paint)
                canvas.drawRect(Rect(14, 9, 16, 12), paint)
                canvas.drawRect(Rect(18, 9, 20, 12), paint)
            }
            "migrator" -> {
                paint.color = Color.parseColor("#1E3A8A")
                canvas.drawRect(Rect(0, 0, 22, 17), paint)
                paint.color = Color.parseColor("#DC2626")
                canvas.drawRect(Rect(3, 4, 9, 12), paint)
                canvas.drawRect(Rect(14, 4, 20, 12), paint)
                paint.color = Color.parseColor("#F59E0B")
                canvas.drawRect(Rect(5, 6, 7, 10), paint)
                canvas.drawRect(Rect(16, 6, 18, 10), paint)
            }
            "cherry_blossom" -> {
                paint.color = Color.parseColor("#F472B6")
                canvas.drawRect(Rect(0, 0, 22, 17), paint)
                paint.color = Color.WHITE
                canvas.drawRect(Rect(4, 4, 6, 6), paint)
                canvas.drawRect(Rect(7, 8, 9, 10), paint)
                canvas.drawRect(Rect(3, 11, 5, 13), paint)
                canvas.drawRect(Rect(15, 4, 17, 6), paint)
                canvas.drawRect(Rect(18, 8, 20, 10), paint)
                canvas.drawRect(Rect(14, 11, 16, 13), paint)
            }
            "founders" -> {
                paint.color = Color.parseColor("#D97706")
                canvas.drawRect(Rect(0, 0, 22, 17), paint)
                paint.color = Color.parseColor("#FEF08A")
                canvas.drawRect(Rect(3, 5, 9, 7), paint)
                canvas.drawRect(Rect(5, 7, 7, 13), paint)
                canvas.drawRect(Rect(14, 5, 20, 7), paint)
                canvas.drawRect(Rect(16, 7, 18, 13), paint)
            }
            "vanilla" -> {
                paint.color = Color.parseColor("#4A3728")
                canvas.drawRect(Rect(0, 0, 22, 17), paint)
                paint.color = Color.parseColor("#FDE047")
                canvas.drawRect(Rect(5, 6, 7, 10), paint)
                canvas.drawRect(Rect(3, 8, 9, 9), paint)
                canvas.drawRect(Rect(16, 6, 18, 10), paint)
                canvas.drawRect(Rect(14, 8, 20, 9), paint)
            }
            else -> return null
        }

        return bitmap
    }

    suspend fun applyPresetCape(context: Context, accountId: String, presetId: String): File? = withContext(Dispatchers.IO) {
        val targetFile = getCapeFileForAccount(context, accountId)
        if (presetId == "none") {
            if (targetFile.exists()) targetFile.delete()
            return@withContext null
        }

        val bitmap = createPresetCape(presetId) ?: return@withContext null
        try {
            FileOutputStream(targetFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            targetFile
        } finally {
            bitmap.recycle()
        }
    }
}
