package com.aistudio.novacraft.launcher.core.controls

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Choreographer
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Window
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.aistudio.novacraft.launcher.core.launch.InstanceManager
import com.aistudio.novacraft.launcher.core.utils.SettingsManager
import com.movtery.inputmap.keycodes.LwjglGlfwKeycode
import com.movtery.inputmap.keycodes.LwjglGlfwKeycode as K
import com.movtery.zalithlauncher.bridge.CURSOR_DISABLED
import com.movtery.zalithlauncher.bridge.LoggerBridge
import com.movtery.zalithlauncher.bridge.ZLBridgeStates
import com.movtery.zalithlauncher.game.sdl.SdlBridge
import com.movtery.zalithlauncher.game.sdl.handleGamepadKeyEvent
import com.movtery.zalithlauncher.game.sdl.handleGamepadMotionEvent
import dalvik.system.DexFile
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Proxy
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.libsdl.app.SDLControllerManager
import org.lwjgl.glfw.CallbackBridge

// ----------------------------------------------------------------------------
// Section: core/controls/models/CustomControlButton.kt
// ----------------------------------------------------------------------------

/**
 * Model konfigurasi sebuah tombol virtual touch kustom
 */
data class CustomControlButton(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "BTN",
    val keyCode: Int = LwjglGlfwKeycode.GLFW_KEY_SPACE,
    val isMouseButton: Boolean = false,
    val mouseButton: Int = LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT,
    val xPercent: Float = 50f, // 0f - 100f
    val yPercent: Float = 50f, // 0f - 100f
    val widthDp: Int = 50,
    val heightDp: Int = 48,
    val opacity: Float = 0.85f, // 0.1f - 1.0f
    val cornerRadiusDp: Int = 8,
    val isToggle: Boolean = false,
    val isScroll: Boolean = false,
    val isSystem: Boolean = false,
    val systemAction: String = "", // "FPS", "KEYBOARD", "HIDE_GUI", "MENU" ("CLOSE" = nama lama MENU)
    val isJoystick: Boolean = false, // Virtual analog WASD joystick
    // Khusus joystick: dorong sampai tepi lingkaran = lari otomatis (Ctrl). Mati = tidak pernah lari
    // lewat joystick. Layout lama tanpa kolom ini tetap lari otomatis (nilai bawaan).
    val autoSprint: Boolean = true,
    // Macro Settings
    val isMacro: Boolean = false,
    val macroType: String = "COMMAND", // "COMMAND", "COMBO", "TURBO"
    val macroCommand: String = "",      // e.g. "/gamemode creative", "/home"
    val macroComboKey: Int = 0,        // Backward compatibility for single key
    val macroComboKeys: List<Int> = emptyList(), // Multi-key sequence executed from top to bottom
    val macroTurboIntervalMs: Long = 100L, // Auto-click speed interval in ms
    // Nama ikon gambar (jump, sneak, attack, use, inventory, chat, pause, ...); kosong = tampilkan nama sebagai teks
    val icon: String = "",
    // Kapan tombol tampil: ALWAYS = selalu, GAME = hanya saat main (kursor dikunci game),
    // MENU = hanya saat ada menu/GUI game (inventori, chat, jeda). Layout lama tanpa kolom ini = ALWAYS.
    val showMode: String = ControlShowMode.ALWAYS
)

/** Nilai [CustomControlButton.showMode] dan aturan tampilnya. Nilai tak dikenal diperlakukan sebagai ALWAYS. */
object ControlShowMode {
    const val ALWAYS = "ALWAYS"
    const val GAME = "GAME"
    const val MENU = "MENU"

    /** [inMenu] = game sedang menampilkan kursor (menu, inventori, chat, layar judul). */
    fun visible(mode: String?, inMenu: Boolean): Boolean = when (mode) {
        GAME -> !inMenu
        MENU -> inMenu
        else -> true
    }

    fun isValid(mode: String): Boolean = mode == ALWAYS || mode == GAME || mode == MENU
}

// ----------------------------------------------------------------------------
// Section: core/controls/KeycodeCatalog.kt
// ----------------------------------------------------------------------------

data class KeyOption(
    val displayName: String,
    val keyCode: Int,
    val isMouseButton: Boolean = false,
    val mouseButton: Int = 0,
    val isScroll: Boolean = false,
    val category: String = "Umum"
)

object KeycodeCatalog {
    val ALL_KEYS: List<KeyOption> = listOf(
        // Mouse Controls
        KeyOption("Mouse Kiri (Attack/Break)", 0, isMouseButton = true, mouseButton = LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT, category = "Mouse"),
        KeyOption("Mouse Kanan (Use/Place)", 0, isMouseButton = true, mouseButton = LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT, category = "Mouse"),
        KeyOption("Mouse Tengah (Pick Block)", 0, isMouseButton = true, mouseButton = LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_MIDDLE, category = "Mouse"),
        KeyOption("Mouse Tombol 4", 0, isMouseButton = true, mouseButton = LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_4, category = "Mouse"),
        KeyOption("Mouse Tombol 5", 0, isMouseButton = true, mouseButton = LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_5, category = "Mouse"),
        KeyOption("Scroll Wheel (Slide Naik/Turun)", 0, isScroll = true, category = "Mouse"),

        // Gerakan (Movement)
        KeyOption("W (Maju)", LwjglGlfwKeycode.GLFW_KEY_W, category = "Gerakan"),
        KeyOption("A (Kiri)", LwjglGlfwKeycode.GLFW_KEY_A, category = "Gerakan"),
        KeyOption("S (Mundur)", LwjglGlfwKeycode.GLFW_KEY_S, category = "Gerakan"),
        KeyOption("D (Kanan)", LwjglGlfwKeycode.GLFW_KEY_D, category = "Gerakan"),
        KeyOption("SPACE (Lompat)", LwjglGlfwKeycode.GLFW_KEY_SPACE, category = "Gerakan"),
        KeyOption("LEFT SHIFT (Sneak/Jongkok)", LwjglGlfwKeycode.GLFW_KEY_LEFT_SHIFT, category = "Gerakan"),
        KeyOption("LEFT CTRL (Sprint/Lari)", LwjglGlfwKeycode.GLFW_KEY_LEFT_CONTROL, category = "Gerakan"),

        // Aksi Game
        KeyOption("E (Inventory)", LwjglGlfwKeycode.GLFW_KEY_E, category = "Aksi"),
        KeyOption("Q (Drop Item)", LwjglGlfwKeycode.GLFW_KEY_Q, category = "Aksi"),
        KeyOption("F (Swap Hand)", LwjglGlfwKeycode.GLFW_KEY_F, category = "Aksi"),
        KeyOption("T (Buka Chat)", LwjglGlfwKeycode.GLFW_KEY_T, category = "Aksi"),
        KeyOption("/ (Buka Command)", LwjglGlfwKeycode.GLFW_KEY_SLASH, category = "Aksi"),
        KeyOption("ENTER (Kirim/Pilih)", LwjglGlfwKeycode.GLFW_KEY_ENTER, category = "Aksi"),
        KeyOption("ESCAPE (Pause/Menu)", LwjglGlfwKeycode.GLFW_KEY_ESCAPE, category = "Aksi"),
        KeyOption("TAB (Daftar Player)", LwjglGlfwKeycode.GLFW_KEY_TAB, category = "Aksi"),
        KeyOption("BACKSPACE (Hapus)", LwjglGlfwKeycode.GLFW_KEY_BACKSPACE, category = "Aksi"),

        // Modifier & Kontrol
        KeyOption("TAB", LwjglGlfwKeycode.GLFW_KEY_TAB, category = "Modifier & Kontrol"),
        KeyOption("LEFT ALT", LwjglGlfwKeycode.GLFW_KEY_LEFT_ALT, category = "Modifier & Kontrol"),
        KeyOption("RIGHT ALT", LwjglGlfwKeycode.GLFW_KEY_RIGHT_ALT, category = "Modifier & Kontrol"),
        KeyOption("LEFT CONTROL", LwjglGlfwKeycode.GLFW_KEY_LEFT_CONTROL, category = "Modifier & Kontrol"),
        KeyOption("RIGHT CONTROL", LwjglGlfwKeycode.GLFW_KEY_RIGHT_CONTROL, category = "Modifier & Kontrol"),
        KeyOption("LEFT SHIFT", LwjglGlfwKeycode.GLFW_KEY_LEFT_SHIFT, category = "Modifier & Kontrol"),
        KeyOption("RIGHT SHIFT", LwjglGlfwKeycode.GLFW_KEY_RIGHT_SHIFT, category = "Modifier & Kontrol"),
        KeyOption("CAPS LOCK", LwjglGlfwKeycode.GLFW_KEY_CAPS_LOCK, category = "Modifier & Kontrol"),
        KeyOption("NUM LOCK", LwjglGlfwKeycode.GLFW_KEY_NUM_LOCK, category = "Modifier & Kontrol"),
        KeyOption("SCROLL LOCK", LwjglGlfwKeycode.GLFW_KEY_SCROLL_LOCK, category = "Modifier & Kontrol"),
        KeyOption("LEFT SUPER (Win)", LwjglGlfwKeycode.GLFW_KEY_LEFT_SUPER, category = "Modifier & Kontrol"),
        KeyOption("RIGHT SUPER", LwjglGlfwKeycode.GLFW_KEY_RIGHT_SUPER, category = "Modifier & Kontrol"),
        KeyOption("MENU", LwjglGlfwKeycode.GLFW_KEY_MENU, category = "Modifier & Kontrol"),
        KeyOption("PRINT SCREEN", LwjglGlfwKeycode.GLFW_KEY_PRINT_SCREEN, category = "Modifier & Kontrol"),
        KeyOption("PAUSE / BREAK", LwjglGlfwKeycode.GLFW_KEY_PAUSE, category = "Modifier & Kontrol"),

        // Fungsi (F1 - F12)
        KeyOption("F1 (Sembunyikan HUD)", LwjglGlfwKeycode.GLFW_KEY_F1, category = "Fungsi (F1-F12)"),
        KeyOption("F2 (Screenshot)", LwjglGlfwKeycode.GLFW_KEY_F2, category = "Fungsi (F1-F12)"),
        KeyOption("F3 (Debug Layar)", LwjglGlfwKeycode.GLFW_KEY_F3, category = "Fungsi (F1-F12)"),
        KeyOption("F4 (Shader / Spectator)", LwjglGlfwKeycode.GLFW_KEY_F4, category = "Fungsi (F1-F12)"),
        KeyOption("F5 (Ubah Perspektif)", LwjglGlfwKeycode.GLFW_KEY_F5, category = "Fungsi (F1-F12)"),
        KeyOption("F6", LwjglGlfwKeycode.GLFW_KEY_F6, category = "Fungsi (F1-F12)"),
        KeyOption("F7", LwjglGlfwKeycode.GLFW_KEY_F7, category = "Fungsi (F1-F12)"),
        KeyOption("F8 (Cinematic Camera)", LwjglGlfwKeycode.GLFW_KEY_F8, category = "Fungsi (F1-F12)"),
        KeyOption("F9", LwjglGlfwKeycode.GLFW_KEY_F9, category = "Fungsi (F1-F12)"),
        KeyOption("F10", LwjglGlfwKeycode.GLFW_KEY_F10, category = "Fungsi (F1-F12)"),
        KeyOption("F11 (Fullscreen)", LwjglGlfwKeycode.GLFW_KEY_F11, category = "Fungsi (F1-F12)"),
        KeyOption("F12", LwjglGlfwKeycode.GLFW_KEY_F12, category = "Fungsi (F1-F12)"),

        // Alfabet (A - Z)
        KeyOption("A", LwjglGlfwKeycode.GLFW_KEY_A, category = "Alfabet (A - Z)"),
        KeyOption("B", LwjglGlfwKeycode.GLFW_KEY_B, category = "Alfabet (A - Z)"),
        KeyOption("C (Zoom)", LwjglGlfwKeycode.GLFW_KEY_C, category = "Alfabet (A - Z)"),
        KeyOption("D", LwjglGlfwKeycode.GLFW_KEY_D, category = "Alfabet (A - Z)"),
        KeyOption("E", LwjglGlfwKeycode.GLFW_KEY_E, category = "Alfabet (A - Z)"),
        KeyOption("F", LwjglGlfwKeycode.GLFW_KEY_F, category = "Alfabet (A - Z)"),
        KeyOption("G", LwjglGlfwKeycode.GLFW_KEY_G, category = "Alfabet (A - Z)"),
        KeyOption("H", LwjglGlfwKeycode.GLFW_KEY_H, category = "Alfabet (A - Z)"),
        KeyOption("I", LwjglGlfwKeycode.GLFW_KEY_I, category = "Alfabet (A - Z)"),
        KeyOption("J", LwjglGlfwKeycode.GLFW_KEY_J, category = "Alfabet (A - Z)"),
        KeyOption("K", LwjglGlfwKeycode.GLFW_KEY_K, category = "Alfabet (A - Z)"),
        KeyOption("L", LwjglGlfwKeycode.GLFW_KEY_L, category = "Alfabet (A - Z)"),
        KeyOption("M", LwjglGlfwKeycode.GLFW_KEY_M, category = "Alfabet (A - Z)"),
        KeyOption("N", LwjglGlfwKeycode.GLFW_KEY_N, category = "Alfabet (A - Z)"),
        KeyOption("O", LwjglGlfwKeycode.GLFW_KEY_O, category = "Alfabet (A - Z)"),
        KeyOption("P", LwjglGlfwKeycode.GLFW_KEY_P, category = "Alfabet (A - Z)"),
        KeyOption("Q", LwjglGlfwKeycode.GLFW_KEY_Q, category = "Alfabet (A - Z)"),
        KeyOption("R", LwjglGlfwKeycode.GLFW_KEY_R, category = "Alfabet (A - Z)"),
        KeyOption("S", LwjglGlfwKeycode.GLFW_KEY_S, category = "Alfabet (A - Z)"),
        KeyOption("T", LwjglGlfwKeycode.GLFW_KEY_T, category = "Alfabet (A - Z)"),
        KeyOption("U", LwjglGlfwKeycode.GLFW_KEY_U, category = "Alfabet (A - Z)"),
        KeyOption("V", LwjglGlfwKeycode.GLFW_KEY_V, category = "Alfabet (A - Z)"),
        KeyOption("W", LwjglGlfwKeycode.GLFW_KEY_W, category = "Alfabet (A - Z)"),
        KeyOption("X", LwjglGlfwKeycode.GLFW_KEY_X, category = "Alfabet (A - Z)"),
        KeyOption("Y", LwjglGlfwKeycode.GLFW_KEY_Y, category = "Alfabet (A - Z)"),
        KeyOption("Z", LwjglGlfwKeycode.GLFW_KEY_Z, category = "Alfabet (A - Z)"),

        // Angka & Hotbar (0 - 9)
        KeyOption("1 (Slot 1)", LwjglGlfwKeycode.GLFW_KEY_1, category = "Angka & Hotbar"),
        KeyOption("2 (Slot 2)", LwjglGlfwKeycode.GLFW_KEY_2, category = "Angka & Hotbar"),
        KeyOption("3 (Slot 3)", LwjglGlfwKeycode.GLFW_KEY_3, category = "Angka & Hotbar"),
        KeyOption("4 (Slot 4)", LwjglGlfwKeycode.GLFW_KEY_4, category = "Angka & Hotbar"),
        KeyOption("5 (Slot 5)", LwjglGlfwKeycode.GLFW_KEY_5, category = "Angka & Hotbar"),
        KeyOption("6 (Slot 6)", LwjglGlfwKeycode.GLFW_KEY_6, category = "Angka & Hotbar"),
        KeyOption("7 (Slot 7)", LwjglGlfwKeycode.GLFW_KEY_7, category = "Angka & Hotbar"),
        KeyOption("8 (Slot 8)", LwjglGlfwKeycode.GLFW_KEY_8, category = "Angka & Hotbar"),
        KeyOption("9 (Slot 9)", LwjglGlfwKeycode.GLFW_KEY_9, category = "Angka & Hotbar"),
        KeyOption("0", LwjglGlfwKeycode.GLFW_KEY_0, category = "Angka & Hotbar"),

        // Navigasi
        KeyOption("Panah Atas (UP)", LwjglGlfwKeycode.GLFW_KEY_UP, category = "Navigasi"),
        KeyOption("Panah Bawah (DOWN)", LwjglGlfwKeycode.GLFW_KEY_DOWN, category = "Navigasi"),
        KeyOption("Panah Kiri (LEFT)", LwjglGlfwKeycode.GLFW_KEY_LEFT, category = "Navigasi"),
        KeyOption("Panah Kanan (RIGHT)", LwjglGlfwKeycode.GLFW_KEY_RIGHT, category = "Navigasi"),
        KeyOption("INSERT", LwjglGlfwKeycode.GLFW_KEY_INSERT, category = "Navigasi"),
        KeyOption("DELETE", LwjglGlfwKeycode.GLFW_KEY_DELETE, category = "Navigasi"),
        KeyOption("HOME", LwjglGlfwKeycode.GLFW_KEY_HOME, category = "Navigasi"),
        KeyOption("END", LwjglGlfwKeycode.GLFW_KEY_END, category = "Navigasi"),
        KeyOption("PAGE UP", LwjglGlfwKeycode.GLFW_KEY_PAGE_UP, category = "Navigasi"),
        KeyOption("PAGE DOWN", LwjglGlfwKeycode.GLFW_KEY_PAGE_DOWN, category = "Navigasi"),

        // Simbol & Tanda Baca
        KeyOption("GRAVE ( ` ~ )", LwjglGlfwKeycode.GLFW_KEY_GRAVE_ACCENT, category = "Simbol"),
        KeyOption("MINUS ( - _ )", LwjglGlfwKeycode.GLFW_KEY_MINUS, category = "Simbol"),
        KeyOption("EQUAL ( = + )", LwjglGlfwKeycode.GLFW_KEY_EQUAL, category = "Simbol"),
        KeyOption("KURUNG BUKA ( [ { )", LwjglGlfwKeycode.GLFW_KEY_LEFT_BRACKET, category = "Simbol"),
        KeyOption("KURUNG TUTUP ( ] } )", LwjglGlfwKeycode.GLFW_KEY_RIGHT_BRACKET, category = "Simbol"),
        KeyOption("BACKSLASH ( \\ | )", LwjglGlfwKeycode.GLFW_KEY_BACKSLASH, category = "Simbol"),
        KeyOption("SEMICOLON ( ; : )", LwjglGlfwKeycode.GLFW_KEY_SEMICOLON, category = "Simbol"),
        KeyOption("APOSTROPHE ( ' \" )", LwjglGlfwKeycode.GLFW_KEY_APOSTROPHE, category = "Simbol"),
        KeyOption("COMMA ( , < )", LwjglGlfwKeycode.GLFW_KEY_COMMA, category = "Simbol"),
        KeyOption("PERIOD ( . > )", LwjglGlfwKeycode.GLFW_KEY_PERIOD, category = "Simbol"),
        KeyOption("SLASH ( / ? )", LwjglGlfwKeycode.GLFW_KEY_SLASH, category = "Simbol"),

        // Numpad (Keypad)
        KeyOption("Numpad 0", LwjglGlfwKeycode.GLFW_KEY_KP_0, category = "Numpad"),
        KeyOption("Numpad 1", LwjglGlfwKeycode.GLFW_KEY_KP_1, category = "Numpad"),
        KeyOption("Numpad 2", LwjglGlfwKeycode.GLFW_KEY_KP_2, category = "Numpad"),
        KeyOption("Numpad 3", LwjglGlfwKeycode.GLFW_KEY_KP_3, category = "Numpad"),
        KeyOption("Numpad 4", LwjglGlfwKeycode.GLFW_KEY_KP_4, category = "Numpad"),
        KeyOption("Numpad 5", LwjglGlfwKeycode.GLFW_KEY_KP_5, category = "Numpad"),
        KeyOption("Numpad 6", LwjglGlfwKeycode.GLFW_KEY_KP_6, category = "Numpad"),
        KeyOption("Numpad 7", LwjglGlfwKeycode.GLFW_KEY_KP_7, category = "Numpad"),
        KeyOption("Numpad 8", LwjglGlfwKeycode.GLFW_KEY_KP_8, category = "Numpad"),
        KeyOption("Numpad 9", LwjglGlfwKeycode.GLFW_KEY_KP_9, category = "Numpad"),
        KeyOption("Numpad . (Titik)", LwjglGlfwKeycode.GLFW_KEY_KP_DECIMAL, category = "Numpad"),
        KeyOption("Numpad / (Bagi)", LwjglGlfwKeycode.GLFW_KEY_KP_DIVIDE, category = "Numpad"),
        KeyOption("Numpad * (Kali)", LwjglGlfwKeycode.GLFW_KEY_KP_MULTIPLY, category = "Numpad"),
        KeyOption("Numpad - (Kurang)", LwjglGlfwKeycode.GLFW_KEY_KP_SUBTRACT, category = "Numpad"),
        KeyOption("Numpad + (Tambah)", LwjglGlfwKeycode.GLFW_KEY_KP_ADD, category = "Numpad"),
        KeyOption("Numpad ENTER", LwjglGlfwKeycode.GLFW_KEY_KP_ENTER, category = "Numpad"),
        KeyOption("Numpad = (Sama Dengan)", LwjglGlfwKeycode.GLFW_KEY_KP_EQUAL, category = "Numpad")
    )

    /** Nama tombol keyboard per kode (entri pertama yang cocok), dibuat sekali saat pertama dipakai. */
    private val keyboardNames: Map<Int, String> by lazy {
        val map = HashMap<Int, String>(ALL_KEYS.size * 2)
        for (k in ALL_KEYS) {
            if (k.isMouseButton || k.isScroll) continue
            if (!map.containsKey(k.keyCode)) map[k.keyCode] = k.displayName
        }
        map
    }

    fun getKeyName(keyCode: Int): String {
        return keyboardNames[keyCode]
            ?: when (keyCode) {
                0 -> "None"
                else -> "Key $keyCode"
            }
    }
}

// ----------------------------------------------------------------------------
// Section: core/controls/ControlLayoutIO.kt
// ----------------------------------------------------------------------------

/**
 * Ekspor dan impor tata letak tombol kontrol sebagai berkas JSON yang bisa dibagikan.
 *
 * Penulisan dan pembacaan memakai nama kolom yang tertulis di sini (bukan refleksi pada kelas
 * model), jadi berkas tetap terbaca walau nama kelas berubah atau kode diperkecil (R8). Berkas dari
 * orang lain tidak dipercaya: setiap kolom dicek tipenya, dibatasi rentangnya, dan kolom yang
 * hilang diisi nilai bawaan model. Tidak ada refleksi, tidak ada kelas yang dimuat dari berkas.
 *
 * Murni logika (tanpa Android) supaya mudah diuji.
 */
object ControlLayoutIO {
    const val FORMAT = "amber-controls"
    const val VERSION = 1

    /** Batas ukuran berkas yang dibaca. Layout normal hanya beberapa KB. */
    const val MAX_FILE_BYTES = 512 * 1024
    const val MAX_BUTTONS = 200

    private const val MAX_ID = 64
    private const val MAX_NAME = 32
    private const val MAX_ICON = 24
    private const val MAX_COMMAND = 256
    private const val MAX_COMBO_KEYS = 32
    private const val MIN_KEYCODE = -1
    private const val MAX_KEYCODE = 1024
    private const val MIN_TURBO_MS = 40L
    private const val MAX_TURBO_MS = 1000L

    private val MACRO_TYPES = setOf("COMMAND", "COMBO", "TURBO")
    private val SYSTEM_ACTIONS = setOf("FPS", "KEYBOARD", "HIDE_GUI", "MENU", "CLOSE")

    enum class Reason { TOO_LARGE, NOT_LAYOUT, NO_BUTTONS }

    sealed class ImportResult {
        /**
         * @param skipped jumlah entri yang dilewati (bukan objek, atau melebihi [MAX_BUTTONS]).
         * @param macroCommands jumlah tombol makro pengetik chat/perintah, ditampilkan sebagai peringatan.
         * @param source nama launcher asal bila berkas diubah dari format lain (Pojav/Zalith/Mojo, Zalith 2);
         * kosong untuk berkas Amber sendiri.
         */
        class Ok(
            val buttons: List<CustomControlButton>,
            val skipped: Int,
            val macroCommands: Int,
            val source: String = ""
        ) : ImportResult()

        class Failed(val reason: Reason) : ImportResult()
    }

    // Tanpa escape HTML supaya perintah makro seperti "a=b" tetap terbaca di berkas.
    private val pretty = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

    // ---- Ekspor ---------------------------------------------------------------------------

    fun toJson(buttons: List<CustomControlButton>): String {
        val root = JsonObject()
        root.addProperty("format", FORMAT)
        root.addProperty("version", VERSION)
        val arr = JsonArray()
        for (b in buttons) arr.add(buttonToJson(b))
        root.add("buttons", arr)
        return pretty.toJson(root)
    }

    private fun buttonToJson(b: CustomControlButton): JsonObject {
        val o = JsonObject()
        o.addProperty("id", b.id)
        o.addProperty("name", b.name)
        o.addProperty("keyCode", b.keyCode)
        o.addProperty("isMouseButton", b.isMouseButton)
        o.addProperty("mouseButton", b.mouseButton)
        o.addProperty("xPercent", b.xPercent)
        o.addProperty("yPercent", b.yPercent)
        o.addProperty("widthDp", b.widthDp)
        o.addProperty("heightDp", b.heightDp)
        o.addProperty("opacity", b.opacity)
        o.addProperty("cornerRadiusDp", b.cornerRadiusDp)
        o.addProperty("isToggle", b.isToggle)
        o.addProperty("isScroll", b.isScroll)
        o.addProperty("isSystem", b.isSystem)
        o.addProperty("systemAction", b.systemAction)
        o.addProperty("isJoystick", b.isJoystick)
        o.addProperty("autoSprint", b.autoSprint)
        o.addProperty("isMacro", b.isMacro)
        o.addProperty("macroType", b.macroType)
        o.addProperty("macroCommand", b.macroCommand)
        o.addProperty("macroComboKey", b.macroComboKey)
        val combo = JsonArray()
        for (k in b.macroComboKeys) combo.add(k)
        o.add("macroComboKeys", combo)
        o.addProperty("macroTurboIntervalMs", b.macroTurboIntervalMs)
        o.addProperty("icon", b.icon)
        o.addProperty("showMode", b.showMode)
        return o
    }

    // ---- Impor ----------------------------------------------------------------------------

    /**
     * Membaca isi berkas. Menerima dua bentuk: berkas ekspor (objek dengan "format") dan larik
     * telanjang seperti berkas cadangan otomatis (controls_layout.json). Berkas dari Pojav, Zalith, Mojo, dan
     * Zalith 2 diubah dulu ke tombol Amber ([ForeignLayoutConverter]); [refWidthDp] dan [refHeightDp] adalah
     * ukuran layar (landscape, dp) yang dipakai untuk menghitung posisi tombolnya.
     */
    fun parse(json: String, refWidthDp: Float = 800f, refHeightDp: Float = 360f): ImportResult {
        if (json.length > MAX_FILE_BYTES) return ImportResult.Failed(Reason.TOO_LARGE)

        val root = try {
            JsonParser.parseString(json)
        } catch (_: Exception) {
            return ImportResult.Failed(Reason.NOT_LAYOUT)
        }

        val arr: JsonArray = if (root.isJsonArray) {
            root.asJsonArray
        } else if (root.isJsonObject) {
            val o = root.asJsonObject
            val foreign = ForeignLayoutConverter.detect(o)
            if (foreign != null) {
                val r = ForeignLayoutConverter.convert(o, foreign, refWidthDp, refHeightDp, MAX_BUTTONS)
                if (r.buttons.isEmpty()) return ImportResult.Failed(Reason.NO_BUTTONS)
                return ImportResult.Ok(r.buttons, r.skipped, r.macroCommands, foreign.label)
            }
            if (o.str("format", "", 32) != FORMAT) return ImportResult.Failed(Reason.NOT_LAYOUT)
            val b = o.get("buttons")
            if (b == null || !b.isJsonArray) return ImportResult.Failed(Reason.NOT_LAYOUT)
            b.asJsonArray
        } else {
            return ImportResult.Failed(Reason.NOT_LAYOUT)
        }

        val usedIds = HashSet<String>()
        val out = ArrayList<CustomControlButton>()
        var skipped = 0
        var macros = 0
        for (el in arr) {
            if (out.size >= MAX_BUTTONS || !el.isJsonObject) {
                skipped++
                continue
            }
            val b = parseButton(el.asJsonObject, usedIds)
            if (b.isMacro && b.macroType == "COMMAND" && b.macroCommand.isNotBlank()) macros++
            out.add(b)
        }
        if (out.isEmpty()) return ImportResult.Failed(Reason.NO_BUTTONS)
        return ImportResult.Ok(out, skipped, macros)
    }

    private fun parseButton(o: JsonObject, usedIds: MutableSet<String>): CustomControlButton {
        val d = CustomControlButton()

        var id = o.str("id", "", MAX_ID).trim()
        if (id.isEmpty() || !usedIds.add(id)) {
            id = "btn_" + UUID.randomUUID().toString().take(8)
            usedIds.add(id)
        }

        val isJoystick = o.bool("isJoystick", d.isJoystick)
        val maxW = if (isJoystick) ControlLimits.MAX_JOYSTICK_DP else ControlLimits.MAX_WIDTH_DP
        val maxH = if (isJoystick) ControlLimits.MAX_JOYSTICK_DP else ControlLimits.MAX_HEIGHT_DP

        val isSystem = o.bool("isSystem", d.isSystem)
        var systemAction = o.str("systemAction", d.systemAction, 16)
        if (!isSystem || systemAction !in SYSTEM_ACTIONS) systemAction = ""

        var macroType = o.str("macroType", d.macroType, 16)
        if (macroType !in MACRO_TYPES) macroType = d.macroType

        // Mode tampil hanya berlaku untuk tombol biasa. Tombol sistem (menu, FPS, dll.) selalu tampil
        // supaya layout orang lain tidak bisa membuat menu game tidak terjangkau.
        var showMode = o.str("showMode", d.showMode, 8)
        if (isSystem || !ControlShowMode.isValid(showMode)) showMode = ControlShowMode.ALWAYS

        return CustomControlButton(
            id = id,
            name = o.str("name", d.name, MAX_NAME),
            keyCode = o.int("keyCode", d.keyCode, MIN_KEYCODE, MAX_KEYCODE),
            isMouseButton = o.bool("isMouseButton", d.isMouseButton),
            mouseButton = o.int("mouseButton", d.mouseButton, 0, 7),
            xPercent = o.float("xPercent", d.xPercent, 0f, 100f),
            yPercent = o.float("yPercent", d.yPercent, 0f, 100f),
            widthDp = o.int("widthDp", d.widthDp, ControlLimits.MIN_SIZE_DP, maxW),
            heightDp = o.int("heightDp", d.heightDp, ControlLimits.MIN_SIZE_DP, maxH),
            opacity = o.float("opacity", d.opacity, ControlLimits.MIN_OPACITY, 1f),
            cornerRadiusDp = o.int("cornerRadiusDp", d.cornerRadiusDp, 0, ControlLimits.MAX_JOYSTICK_DP / 2),
            isToggle = o.bool("isToggle", d.isToggle),
            isScroll = o.bool("isScroll", d.isScroll),
            isSystem = isSystem,
            systemAction = systemAction,
            isJoystick = isJoystick,
            autoSprint = o.bool("autoSprint", d.autoSprint),
            isMacro = o.bool("isMacro", d.isMacro),
            macroType = macroType,
            macroCommand = o.str("macroCommand", d.macroCommand, MAX_COMMAND),
            macroComboKey = o.int("macroComboKey", d.macroComboKey, 0, MAX_KEYCODE),
            macroComboKeys = o.intList("macroComboKeys", MAX_COMBO_KEYS),
            macroTurboIntervalMs = o.long("macroTurboIntervalMs", d.macroTurboIntervalMs, MIN_TURBO_MS, MAX_TURBO_MS),
            icon = o.str("icon", d.icon, MAX_ICON),
            showMode = showMode
        )
    }

    // ---- Pembaca kolom yang toleran ---------------------------------------------------------

    private fun JsonObject.str(key: String, def: String, max: Int): String {
        val e = get(key)
        if (e == null || !e.isJsonPrimitive) return def
        return e.asString.take(max)
    }

    private fun JsonObject.bool(key: String, def: Boolean): Boolean {
        val e = get(key)
        if (e == null || !e.isJsonPrimitive) return def
        val p = e.asJsonPrimitive
        return if (p.isBoolean) p.asBoolean else def
    }

    private fun JsonObject.num(key: String): Double? {
        val e = get(key)
        if (e == null || !e.isJsonPrimitive) return null
        val p = e.asJsonPrimitive
        if (!p.isNumber) return null
        val v = p.asDouble
        return if (v.isNaN() || v.isInfinite()) null else v
    }

    private fun JsonObject.int(key: String, def: Int, min: Int, max: Int): Int {
        val v = num(key) ?: return def
        return v.roundToInt().coerceIn(min, max)
    }

    private fun JsonObject.long(key: String, def: Long, min: Long, max: Long): Long {
        val v = num(key) ?: return def
        return v.toLong().coerceIn(min, max)
    }

    private fun JsonObject.float(key: String, def: Float, min: Float, max: Float): Float {
        val v = num(key) ?: return def
        return v.toFloat().coerceIn(min, max)
    }

    private fun JsonObject.intList(key: String, maxItems: Int): List<Int> {
        val e = get(key)
        if (e == null || !e.isJsonArray) return emptyList()
        val out = ArrayList<Int>()
        for (item in e.asJsonArray) {
            if (out.size >= maxItems) break
            if (!item.isJsonPrimitive) continue
            val p = item.asJsonPrimitive
            if (!p.isNumber) continue
            val v = p.asDouble
            if (v.isNaN() || v.isInfinite()) continue
            out.add(v.roundToInt().coerceIn(0, MAX_KEYCODE))
        }
        return out
    }
}

// ----------------------------------------------------------------------------
// Section: core/controls/ControlLayoutManager.kt
// ----------------------------------------------------------------------------

object ControlLayoutManager {
    private val gson = Gson()
    private val _buttons = MutableStateFlow<List<CustomControlButton>>(emptyList())
    val buttons: StateFlow<List<CustomControlButton>> = _buttons.asStateFlow()

    private fun getLayoutFile(context: Context): File {
        val dir = File(context.filesDir, "controls")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "default_layout.json")
    }

    fun init(context: Context) {
        val file = getLayoutFile(context)
        if (file.exists()) {
            try {
                val json = file.readText()
                val loaded = parseButtons(json)
                if (!loaded.isNullOrEmpty()) {
                    // Layout bawaan versi lama yang belum pernah diubah: ganti ke layout baru.
                    // Layout yang sudah diatur pengguna tidak disentuh.
                    if (isUntouchedDefault(loaded)) {
                        val fresh = getDefaultButtons()
                        _buttons.value = fresh
                        save(context, fresh)
                    } else {
                        _buttons.value = loaded
                    }
                    return
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Data internal tidak ada (mis. aplikasi baru dipasang ulang): coba pulihkan dari cadangan.
        val restored = readBackup(context)
        if (restored != null) {
            if (isUntouchedDefault(restored)) {
                val fresh = getDefaultButtons()
                _buttons.value = fresh
                save(context, fresh)
                return
            }
            _buttons.value = restored
            // Salin ke penyimpanan internal tanpa membuat cadangan baru.
            try { file.writeText(gson.toJson(restored)) } catch (_: Exception) {}
            return
        }

        // Tidak ada cadangan: pakai bawaan di memori saja.
        _buttons.value = getDefaultButtons()
    }

    private fun parseButtons(json: String): List<CustomControlButton>? {
        val type = object : TypeToken<List<CustomControlButton>>() {}.type
        return gson.fromJson<List<CustomControlButton>>(json, type)
    }

    // ---- Cadangan otomatis ------------------------------------------------------------
    // Disimpan di folder novacraft/controls_backup agar aman di Android 11+ tanpa izin khusus.
    private const val BACKUP_SUBDIR = "controls_backup"
    private const val LEGACY_BACKUP_DIR = "Documents/LauncherBackup"
    private const val BACKUP_FILE = "controls_layout.json"
    private const val BACKUP_PREV_FILE = "controls_layout.prev.json"

    private val backupExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "controls-backup").apply { isDaemon = true; priority = Thread.MIN_PRIORITY }
    }

    private fun backupDir(context: Context): File =
        File(InstanceManager.getNuxDir(context), BACKUP_SUBDIR)

    @Suppress("DEPRECATION")
    private fun legacyBackupDir(): File =
        File(Environment.getExternalStorageDirectory(), LEGACY_BACKUP_DIR)

    private fun backupAsync(context: Context, json: String) {
        val appCtx = context.applicationContext
        backupExecutor.execute {
            try {
                val dir = backupDir(appCtx)
                if (!dir.exists() && !dir.mkdirs()) return@execute
                val target = File(dir, BACKUP_FILE)
                if (target.isFile && target.readText() == json) return@execute
                val tmp = File(dir, "$BACKUP_FILE.tmp")
                tmp.writeText(json)
                if (target.exists()) {
                    val prev = File(dir, BACKUP_PREV_FILE)
                    prev.delete()
                    target.renameTo(prev)
                }
                if (!tmp.renameTo(target)) {
                    target.writeText(json)
                    tmp.delete()
                }
            } catch (_: Throwable) {
            }
        }
    }

    private fun readBackup(context: Context): List<CustomControlButton>? {
        val dirs = listOf(backupDir(context), legacyBackupDir())
        for (dir in dirs) {
            for (name in listOf(BACKUP_FILE, BACKUP_PREV_FILE)) {
                try {
                    val f = File(dir, name)
                    if (f.isFile && f.canRead() && f.length() > 2) {
                        val list = parseButtons(f.readText())
                        if (!list.isNullOrEmpty()) return list
                    }
                } catch (_: Throwable) {}
            }
        }
        return null
    }

    /**
     * Tata letak bawaan: sedikit tombol, besar, mudah dijangkau jempol.
     * Kiri: joystick + jongkok. Kanan: serang, pakai, lompat, gulir hotbar.
     * Atas: lencana FPS, ESC/chat/inventori/F5 di tengah, tombol sistem kecil di kanan.
     * Dua mode: tombol gerak dan aksi hanya tampil saat main; saat menu (inventori, chat, jeda)
     * tersisa ESC, inventori, dan tombol Shift untuk memindah barang cepat.
     */
    fun getDefaultButtons(): List<CustomControlButton> {
        return listOf(
            CustomControlButton(
                id = "sys_fps", name = "FPS",
                xPercent = 11f, yPercent = 9f, widthDp = 84, heightDp = 26,
                opacity = 0.9f, cornerRadiusDp = 13,
                isSystem = true, systemAction = "FPS"
            ),

            // Tombol pintas di tengah atas
            CustomControlButton(
                id = "key_esc", name = "ESC", icon = "pause",
                keyCode = LwjglGlfwKeycode.GLFW_KEY_ESCAPE,
                xPercent = 41f, yPercent = 9f, widthDp = 38, heightDp = 34,
                opacity = 0.9f, cornerRadiusDp = 12
            ),
            CustomControlButton(
                id = "key_t", name = "CHAT", icon = "chat",
                keyCode = LwjglGlfwKeycode.GLFW_KEY_T,
                xPercent = 47f, yPercent = 9f, widthDp = 38, heightDp = 34,
                opacity = 0.9f, cornerRadiusDp = 12,
                showMode = ControlShowMode.GAME
            ),
            CustomControlButton(
                id = "key_inv", name = "INV", icon = "inventory",
                keyCode = LwjglGlfwKeycode.GLFW_KEY_E,
                xPercent = 53f, yPercent = 9f, widthDp = 38, heightDp = 34,
                opacity = 0.9f, cornerRadiusDp = 12
            ),
            CustomControlButton(
                id = "key_f5", name = "F5",
                keyCode = LwjglGlfwKeycode.GLFW_KEY_F5,
                xPercent = 59f, yPercent = 9f, widthDp = 38, heightDp = 34,
                opacity = 0.9f, cornerRadiusDp = 12,
                showMode = ControlShowMode.GAME
            ),

            // Tombol sistem kecil di kanan atas
            CustomControlButton(
                id = "sys_keyboard", name = "KEYBOARD",
                xPercent = 87f, yPercent = 9f, widthDp = 32, heightDp = 32,
                opacity = 0.9f, cornerRadiusDp = 16,
                isSystem = true, systemAction = "KEYBOARD"
            ),
            CustomControlButton(
                id = "sys_hide_gui", name = "HIDE GUI",
                xPercent = 91.5f, yPercent = 9f, widthDp = 32, heightDp = 32,
                opacity = 0.9f, cornerRadiusDp = 16,
                isSystem = true, systemAction = "HIDE_GUI"
            ),
            CustomControlButton(
                id = "sys_close", name = "MENU",
                xPercent = 96f, yPercent = 9f, widthDp = 32, heightDp = 32,
                opacity = 0.9f, cornerRadiusDp = 16,
                isSystem = true, systemAction = "MENU"
            ),

            // Kiri: gerak
            CustomControlButton(
                id = "joy_move", name = "JOYSTICK", isJoystick = true,
                xPercent = 15f, yPercent = 70f, widthDp = 120, heightDp = 120,
                opacity = 0.9f, cornerRadiusDp = 60,
                showMode = ControlShowMode.GAME
            ),
            CustomControlButton(
                id = "key_sneak", name = "SNEAK", icon = "sneak",
                keyCode = LwjglGlfwKeycode.GLFW_KEY_LEFT_SHIFT, isToggle = true,
                xPercent = 29.5f, yPercent = 82f, widthDp = 52, heightDp = 52,
                opacity = 0.9f, cornerRadiusDp = 26,
                showMode = ControlShowMode.GAME
            ),

            // Saat menu terbuka (inventori, peti): tahan Shift lalu ketuk barang = pindah cepat.
            CustomControlButton(
                id = "key_shift_menu", name = "SHIFT",
                keyCode = LwjglGlfwKeycode.GLFW_KEY_LEFT_SHIFT, isToggle = true,
                xPercent = 12f, yPercent = 80f, widthDp = 56, heightDp = 44,
                opacity = 0.9f, cornerRadiusDp = 14,
                showMode = ControlShowMode.MENU
            ),

            // Kanan: aksi utama di sekitar jempol
            CustomControlButton(
                id = "mouse_attack", name = "SERANG", icon = "attack",
                isMouseButton = true, mouseButton = LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT,
                xPercent = 84f, yPercent = 62f, widthDp = 72, heightDp = 72,
                opacity = 0.9f, cornerRadiusDp = 36,
                showMode = ControlShowMode.GAME
            ),
            CustomControlButton(
                id = "mouse_use", name = "PAKAI", icon = "use",
                isMouseButton = true, mouseButton = LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT,
                xPercent = 75.5f, yPercent = 84f, widthDp = 60, heightDp = 60,
                opacity = 0.9f, cornerRadiusDp = 30,
                showMode = ControlShowMode.GAME
            ),
            CustomControlButton(
                id = "key_jump", name = "JUMP", icon = "jump",
                keyCode = LwjglGlfwKeycode.GLFW_KEY_SPACE,
                xPercent = 92f, yPercent = 80f, widthDp = 70, heightDp = 70,
                opacity = 0.9f, cornerRadiusDp = 35,
                showMode = ControlShowMode.GAME
            ),
            CustomControlButton(
                id = "widget_scroll", name = "HOTBAR", isScroll = true,
                xPercent = 96.5f, yPercent = 36f, widthDp = 40, heightDp = 92,
                opacity = 0.9f, cornerRadiusDp = 14,
                showMode = ControlShowMode.GAME
            )
        )
    }

    /** Layout yang tersimpan masih persis salah satu bawaan versi lama (belum pernah diubah pengguna). */
    private fun isUntouchedDefault(list: List<CustomControlButton>): Boolean =
        list == legacyDefaultButtons() || list == previousDefaultButtons()

    /**
     * Bawaan sebelum ada mode tampil (semua tombol selalu tampil, tanpa tombol Shift menu). Pemakai yang
     * tata letaknya masih ini dipindah ke bawaan dua mode; yang sudah mengubahnya tidak disentuh.
     */
    private fun previousDefaultButtons(): List<CustomControlButton> =
        getDefaultButtons()
            .filter { it.id != "key_shift_menu" }
            .map { it.copy(showMode = ControlShowMode.ALWAYS) }

    /** Tata letak bawaan versi lama; dipakai hanya untuk mengenali layout yang belum pernah diubah pengguna. */
    private fun legacyDefaultButtons(): List<CustomControlButton> {
        return listOf(
            // Top-Left FPS Indicator
            CustomControlButton(
                id = "sys_fps",
                name = "FPS",
                xPercent = 6f,
                yPercent = 7f,
                widthDp = 64,
                heightDp = 28,
                opacity = 0.85f,
                cornerRadiusDp = 8,
                isSystem = true,
                systemAction = "FPS"
            ),

            // Top-Left Utility
            CustomControlButton(
                id = "key_esc",
                name = "ESC",
                keyCode = LwjglGlfwKeycode.GLFW_KEY_ESCAPE,
                xPercent = 6f,
                yPercent = 20f,
                widthDp = 46,
                heightDp = 38,
                opacity = 0.85f,
                cornerRadiusDp = 8
            ),

            // Left Movement & Modifier Cluster (Clean D-Pad with Center SHIFT and Top-Left SPRINT)
            CustomControlButton(
                id = "key_ctrl",
                name = "SPRINT",
                keyCode = LwjglGlfwKeycode.GLFW_KEY_LEFT_CONTROL,
                xPercent = 6f,
                yPercent = 52f,
                widthDp = 48,
                heightDp = 48,
                opacity = 0.85f,
                cornerRadiusDp = 10,
                isToggle = true
            ),
            CustomControlButton(
                id = "key_w",
                name = "W",
                keyCode = LwjglGlfwKeycode.GLFW_KEY_W,
                xPercent = 14f,
                yPercent = 52f,
                widthDp = 48,
                heightDp = 48,
                opacity = 0.85f,
                cornerRadiusDp = 10
            ),
            CustomControlButton(
                id = "key_a",
                name = "A",
                keyCode = LwjglGlfwKeycode.GLFW_KEY_A,
                xPercent = 6f,
                yPercent = 70f,
                widthDp = 48,
                heightDp = 48,
                opacity = 0.85f,
                cornerRadiusDp = 10
            ),
            CustomControlButton(
                id = "key_shift",
                name = "SHIFT",
                keyCode = LwjglGlfwKeycode.GLFW_KEY_LEFT_SHIFT,
                xPercent = 14f,
                yPercent = 70f,
                widthDp = 48,
                heightDp = 48,
                opacity = 0.85f,
                cornerRadiusDp = 10,
                isToggle = true
            ),
            CustomControlButton(
                id = "key_d",
                name = "D",
                keyCode = LwjglGlfwKeycode.GLFW_KEY_D,
                xPercent = 22f,
                yPercent = 70f,
                widthDp = 48,
                heightDp = 48,
                opacity = 0.85f,
                cornerRadiusDp = 10
            ),
            CustomControlButton(
                id = "key_s",
                name = "S",
                keyCode = LwjglGlfwKeycode.GLFW_KEY_S,
                xPercent = 14f,
                yPercent = 88f,
                widthDp = 48,
                heightDp = 48,
                opacity = 0.85f,
                cornerRadiusDp = 10
            ),

            // Top-Center Quick Actions (Chat, Enter, Zoom)
            CustomControlButton(
                id = "key_t",
                name = "T",
                keyCode = LwjglGlfwKeycode.GLFW_KEY_T,
                xPercent = 46f,
                yPercent = 22f,
                widthDp = 40,
                heightDp = 40,
                opacity = 0.85f,
                cornerRadiusDp = 10
            ),
            CustomControlButton(
                id = "key_enter",
                name = "ENTER",
                keyCode = LwjglGlfwKeycode.GLFW_KEY_ENTER,
                xPercent = 52f,
                yPercent = 22f,
                widthDp = 50,
                heightDp = 40,
                opacity = 0.85f,
                cornerRadiusDp = 10
            ),
            CustomControlButton(
                id = "key_c",
                name = "C",
                keyCode = LwjglGlfwKeycode.GLFW_KEY_C,
                xPercent = 58f,
                yPercent = 22f,
                widthDp = 40,
                heightDp = 40,
                opacity = 0.85f,
                cornerRadiusDp = 10
            ),

            // Middle-Right Utility & Scroll (F5 / F3 stacked next to vertical scroll)
            CustomControlButton(
                id = "key_f5",
                name = "F5",
                keyCode = LwjglGlfwKeycode.GLFW_KEY_F5,
                xPercent = 56f,
                yPercent = 67f,
                widthDp = 44,
                heightDp = 34,
                opacity = 0.85f,
                cornerRadiusDp = 8
            ),
            CustomControlButton(
                id = "key_f3",
                name = "F3",
                keyCode = LwjglGlfwKeycode.GLFW_KEY_F3,
                xPercent = 56f,
                yPercent = 79f,
                widthDp = 44,
                heightDp = 34,
                opacity = 0.85f,
                cornerRadiusDp = 8
            ),
            CustomControlButton(
                id = "widget_scroll",
                name = "SCROLL",
                xPercent = 62f,
                yPercent = 73f,
                widthDp = 42,
                heightDp = 76,
                opacity = 0.85f,
                cornerRadiusDp = 12,
                isScroll = true
            ),

            // Top-Right System Controls
            CustomControlButton(
                id = "sys_keyboard",
                name = "KEYBOARD",
                xPercent = 78f,
                yPercent = 7f,
                widthDp = 76,
                heightDp = 28,
                opacity = 0.85f,
                cornerRadiusDp = 8,
                isSystem = true,
                systemAction = "KEYBOARD"
            ),
            CustomControlButton(
                id = "sys_hide_gui",
                name = "HIDE GUI",
                xPercent = 89f,
                yPercent = 7f,
                widthDp = 72,
                heightDp = 28,
                opacity = 0.85f,
                cornerRadiusDp = 8,
                isSystem = true,
                systemAction = "HIDE_GUI"
            ),
            CustomControlButton(
                id = "sys_close",
                name = "✕",
                xPercent = 96.5f,
                yPercent = 7f,
                widthDp = 30,
                heightDp = 28,
                opacity = 0.85f,
                cornerRadiusDp = 8,
                isSystem = true,
                systemAction = "CLOSE"
            ),

            // Right-Side Mouse Clicks (Underneath KEYBOARD and HIDE GUI)
            CustomControlButton(
                id = "mouse_left",
                name = "L-CLICK",
                isMouseButton = true,
                mouseButton = LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT,
                xPercent = 78f,
                yPercent = 22f,
                widthDp = 56,
                heightDp = 48,
                opacity = 0.85f,
                cornerRadiusDp = 10
            ),
            CustomControlButton(
                id = "mouse_right",
                name = "R-CLICK",
                isMouseButton = true,
                mouseButton = LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT,
                xPercent = 87f,
                yPercent = 22f,
                widthDp = 56,
                heightDp = 48,
                opacity = 0.85f,
                cornerRadiusDp = 10
            ),

            // Bottom-Right In-Game Actions
            CustomControlButton(
                id = "key_inv",
                name = "INV",
                keyCode = LwjglGlfwKeycode.GLFW_KEY_E,
                xPercent = 77f,
                yPercent = 74f,
                widthDp = 54,
                heightDp = 48,
                opacity = 0.85f,
                cornerRadiusDp = 10
            ),
            CustomControlButton(
                id = "key_jump",
                name = "JUMP",
                keyCode = LwjglGlfwKeycode.GLFW_KEY_SPACE,
                xPercent = 87f,
                yPercent = 74f,
                widthDp = 64,
                heightDp = 50,
                opacity = 0.85f,
                cornerRadiusDp = 10
            )
        )
    }

    fun resetToDefaults(context: Context) {
        val defaults = getDefaultButtons()
        _buttons.value = defaults
        save(context, defaults)
    }

    fun saveButtons(context: Context, newButtons: List<CustomControlButton>) {
        _buttons.value = newButtons
        save(context, newButtons)
    }

    /**
     * Layout tersimpan sekarang dalam bentuk berkas ekspor (JSON). Dimuat dari penyimpanan dulu
     * bila belum pernah dimuat di proses ini. Panggil dari thread latar.
     */
    fun exportJson(context: Context): String {
        if (_buttons.value.isEmpty()) init(context)
        return ControlLayoutIO.toJson(_buttons.value)
    }

    /** Menulis layout tersimpan ke berkas pilihan pengguna (lewat pemilih berkas sistem). Panggil dari thread latar. */
    fun exportTo(context: Context, uri: Uri): Boolean {
        return try {
            val bytes = exportJson(context).toByteArray(Charsets.UTF_8)
            val stream = context.contentResolver.openOutputStream(uri, "wt") ?: return false
            stream.use { it.write(bytes) }
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Membaca dan memeriksa berkas layout pilihan pengguna tanpa mengubah apa pun. Pembacaan dibatasi
     * [ControlLayoutIO.MAX_FILE_BYTES] supaya berkas raksasa tidak memenuhi memori. Panggil dari thread latar.
     */
    fun readImport(context: Context, uri: Uri): ControlLayoutIO.ImportResult {
        return try {
            val input = context.contentResolver.openInputStream(uri)
                ?: return ControlLayoutIO.ImportResult.Failed(ControlLayoutIO.Reason.NOT_LAYOUT)
            val out = java.io.ByteArrayOutputStream()
            input.use {
                val buf = ByteArray(8 * 1024)
                while (out.size() <= ControlLayoutIO.MAX_FILE_BYTES) {
                    val n = it.read(buf)
                    if (n < 0) break
                    out.write(buf, 0, n)
                }
            }
            if (out.size() > ControlLayoutIO.MAX_FILE_BYTES) {
                return ControlLayoutIO.ImportResult.Failed(ControlLayoutIO.Reason.TOO_LARGE)
            }
            // Posisi tombol dari launcher lain dihitung terhadap layar perangkat ini (dalam bentuk landscape)
            val dm = context.resources.displayMetrics
            val density = if (dm.density > 0f) dm.density else 1f
            val longDp = maxOf(dm.widthPixels, dm.heightPixels) / density
            val shortDp = minOf(dm.widthPixels, dm.heightPixels) / density
            ControlLayoutIO.parse(String(out.toByteArray(), Charsets.UTF_8).removePrefix("\uFEFF"), longDp, shortDp)
        } catch (_: Exception) {
            ControlLayoutIO.ImportResult.Failed(ControlLayoutIO.Reason.NOT_LAYOUT)
        }
    }

    /**
     * Mengganti seluruh layout dengan hasil impor lalu menyimpannya. Tombol sistem (FPS, keyboard,
     * sembunyikan GUI, menu) yang tidak ada di berkas ditambahkan lagi dari bawaan, supaya layout
     * milik orang lain tidak bisa membuat menu game tidak terjangkau. Panggil dari thread latar.
     */
    fun importLayout(context: Context, imported: List<CustomControlButton>): List<CustomControlButton> {
        fun actionOf(b: CustomControlButton) = if (b.systemAction == "CLOSE") "MENU" else b.systemAction

        val result = ArrayList<CustomControlButton>(imported)
        val present = result.filter { it.isSystem }.map { actionOf(it) }.toSet()
        for (d in getDefaultButtons()) {
            if (!d.isSystem || actionOf(d) in present) continue
            var id = d.id
            var n = 2
            while (result.any { it.id == id }) id = d.id + "_" + n++
            result.add(if (id == d.id) d else d.copy(id = id))
        }
        saveButtons(context, result)
        return result
    }

    fun addButton(context: Context, button: CustomControlButton) {
        val current = _buttons.value.toMutableList()
        current.add(button)
        saveButtons(context, current)
    }

    fun updateButton(context: Context, updated: CustomControlButton) {
        val current = _buttons.value.toMutableList()
        val index = current.indexOfFirst { it.id == updated.id }
        if (index >= 0) {
            current[index] = updated
            saveButtons(context, current)
        }
    }

    fun deleteButton(context: Context, buttonId: String) {
        val current = _buttons.value.filter { it.id != buttonId }
        saveButtons(context, current)
    }

    private fun save(context: Context, list: List<CustomControlButton>) {
        try {
            val file = getLayoutFile(context)
            val json = gson.toJson(list)
            file.writeText(json)
            if (list.isNotEmpty()) backupAsync(context, json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

// ----------------------------------------------------------------------------
// Section: core/controls/ForeignLayoutConverter.kt
// ----------------------------------------------------------------------------

/**
 * Mengubah tata letak kontrol dari launcher lain menjadi tombol Amber.
 *
 * Format yang dikenali:
 * - Pojav / Zalith Launcher 1 / Mojo (dan turunan Pojav lain): berkas dengan "mControlDataList".
 * - Zalith Launcher 2: berkas dengan "layers" dan "editorVersion".
 *
 * Yang ikut diubah: posisi, ukuran, kode tombol (termasuk beberapa tombol sekaligus jadi kombinasi),
 * klik mouse, scroll, tombol keyboard/menu/sembunyikan GUI, joystick, mode toggle, kapan tombol tampil, dan
 * makro teks chat dari Zalith 2. Yang tidak punya padanan di Amber dilewati dan dihitung: layer yang
 * tersembunyi dan tombol pengatur layer, mouse virtual, laci (isinya dibentangkan jadi tombol biasa), tombol
 * scroll kedua (Amber memakai satu tombol untuk naik dan turun), serta warna, bingkai, geser-untuk-tekan,
 * dan tembus sentuhan.
 *
 * Berkas orang lain tidak dipercaya: tidak ada refleksi dan tidak ada kelas yang dimuat dari berkas, setiap
 * kolom dicek tipenya, angka dibatasi ke rentang yang sama dengan editor, dan ungkapan posisi dihitung oleh
 * pengurai kecil buatan sendiri (bukan mesin skrip). Murni logika (tanpa Android) supaya mudah diuji.
 */
object ForeignLayoutConverter {

    enum class Source(val label: String) {
        POJAV("Pojav / Zalith / Mojo"),
        ZALITH2("Zalith 2")
    }

    class Result(
        val buttons: List<CustomControlButton>,
        /** Entri yang dilewati atau digabung karena tidak ada padanannya di Amber. */
        val skipped: Int,
        /** Jumlah tombol makro pengetik chat, ditampilkan sebagai peringatan. */
        val macroCommands: Int
    )

    private const val MAX_NAME = 32
    private const val MAX_COMMAND = 256
    private const val MAX_EXPRESSION = 200
    private const val MAX_COMBO_KEYS = 32
    private const val MAX_KEYCODE = 1024
    private const val MARGIN_DP = 2.0
    private const val DRAWER_GAP_DP = 2f

    private const val EVENT_KEYBOARD = "launcher.event.switch_ime"
    private const val EVENT_MENU = "launcher.event.switch_menu"
    private const val EVENT_SCROLL_PREFIX = "launcher.event.scroll_"

    private val VAR_PATTERN = Regex("[$][{]([a-zA-Z_]+)[}]")

    /** Nama tombol GLFW (dipakai Zalith 2) ke kode angkanya. Tabel statis supaya aman dari R8. */
    private val GLFW_KEY_BY_NAME: Map<String, Int> = mapOf(
        "GLFW_KEY_SPACE" to 32, "GLFW_KEY_APOSTROPHE" to 39, "GLFW_KEY_COMMA" to 44, "GLFW_KEY_MINUS" to 45,
        "GLFW_KEY_PERIOD" to 46, "GLFW_KEY_SLASH" to 47, "GLFW_KEY_0" to 48, "GLFW_KEY_1" to 49,
        "GLFW_KEY_2" to 50, "GLFW_KEY_3" to 51, "GLFW_KEY_4" to 52, "GLFW_KEY_5" to 53, "GLFW_KEY_6" to 54,
        "GLFW_KEY_7" to 55, "GLFW_KEY_8" to 56, "GLFW_KEY_9" to 57, "GLFW_KEY_SEMICOLON" to 59,
        "GLFW_KEY_EQUAL" to 61, "GLFW_KEY_A" to 65, "GLFW_KEY_B" to 66, "GLFW_KEY_C" to 67, "GLFW_KEY_D" to 68,
        "GLFW_KEY_E" to 69, "GLFW_KEY_F" to 70, "GLFW_KEY_G" to 71, "GLFW_KEY_H" to 72, "GLFW_KEY_I" to 73,
        "GLFW_KEY_J" to 74, "GLFW_KEY_K" to 75, "GLFW_KEY_L" to 76, "GLFW_KEY_M" to 77, "GLFW_KEY_N" to 78,
        "GLFW_KEY_O" to 79, "GLFW_KEY_P" to 80, "GLFW_KEY_Q" to 81, "GLFW_KEY_R" to 82, "GLFW_KEY_S" to 83,
        "GLFW_KEY_T" to 84, "GLFW_KEY_U" to 85, "GLFW_KEY_V" to 86, "GLFW_KEY_W" to 87, "GLFW_KEY_X" to 88,
        "GLFW_KEY_Y" to 89, "GLFW_KEY_Z" to 90, "GLFW_KEY_LEFT_BRACKET" to 91, "GLFW_KEY_BACKSLASH" to 92,
        "GLFW_KEY_RIGHT_BRACKET" to 93, "GLFW_KEY_GRAVE_ACCENT" to 96, "GLFW_KEY_WORLD_1" to 161,
        "GLFW_KEY_WORLD_2" to 162, "GLFW_KEY_ESCAPE" to 256, "GLFW_KEY_ENTER" to 257, "GLFW_KEY_TAB" to 258,
        "GLFW_KEY_BACKSPACE" to 259, "GLFW_KEY_INSERT" to 260, "GLFW_KEY_DELETE" to 261,
        "GLFW_KEY_RIGHT" to 262, "GLFW_KEY_LEFT" to 263, "GLFW_KEY_DOWN" to 264, "GLFW_KEY_UP" to 265,
        "GLFW_KEY_PAGE_UP" to 266, "GLFW_KEY_PAGE_DOWN" to 267, "GLFW_KEY_HOME" to 268, "GLFW_KEY_END" to 269,
        "GLFW_KEY_CAPS_LOCK" to 280, "GLFW_KEY_SCROLL_LOCK" to 281, "GLFW_KEY_NUM_LOCK" to 282,
        "GLFW_KEY_PRINT_SCREEN" to 283, "GLFW_KEY_PAUSE" to 284, "GLFW_KEY_F1" to 290, "GLFW_KEY_F2" to 291,
        "GLFW_KEY_F3" to 292, "GLFW_KEY_F4" to 293, "GLFW_KEY_F5" to 294, "GLFW_KEY_F6" to 295,
        "GLFW_KEY_F7" to 296, "GLFW_KEY_F8" to 297, "GLFW_KEY_F9" to 298, "GLFW_KEY_F10" to 299,
        "GLFW_KEY_F11" to 300, "GLFW_KEY_F12" to 301, "GLFW_KEY_KP_0" to 320, "GLFW_KEY_KP_1" to 321,
        "GLFW_KEY_KP_2" to 322, "GLFW_KEY_KP_3" to 323, "GLFW_KEY_KP_4" to 324, "GLFW_KEY_KP_5" to 325,
        "GLFW_KEY_KP_6" to 326, "GLFW_KEY_KP_7" to 327, "GLFW_KEY_KP_8" to 328, "GLFW_KEY_KP_9" to 329,
        "GLFW_KEY_KP_DECIMAL" to 330, "GLFW_KEY_KP_DIVIDE" to 331, "GLFW_KEY_KP_MULTIPLY" to 332,
        "GLFW_KEY_KP_SUBTRACT" to 333, "GLFW_KEY_KP_ADD" to 334, "GLFW_KEY_KP_ENTER" to 335,
        "GLFW_KEY_KP_EQUAL" to 336, "GLFW_KEY_LEFT_SHIFT" to 340, "GLFW_KEY_LEFT_CONTROL" to 341,
        "GLFW_KEY_LEFT_ALT" to 342, "GLFW_KEY_LEFT_SUPER" to 343, "GLFW_KEY_RIGHT_SHIFT" to 344,
        "GLFW_KEY_RIGHT_CONTROL" to 345, "GLFW_KEY_RIGHT_ALT" to 346, "GLFW_KEY_RIGHT_SUPER" to 347,
        "GLFW_KEY_MENU" to 348
    )

    private val KEY_LABEL_BY_CODE: Map<Int, String> by lazy {
        GLFW_KEY_BY_NAME.entries.associate { it.value to it.key.removePrefix("GLFW_KEY_") }
    }

    /** Layar acuan dalam dp. Semua posisi dihitung dari sini lalu disimpan sebagai persen layar. */
    private class Screen(val w: Float, val h: Float)

    private class Acc(private val max: Int) {
        val buttons = ArrayList<CustomControlButton>()
        var skipped = 0
        var macros = 0
        var scrollAdded = false

        fun add(b: CustomControlButton): Boolean {
            if (buttons.size >= max) {
                skipped++
                return false
            }
            buttons.add(b)
            if (b.isMacro && b.macroType == "COMMAND" && b.macroCommand.isNotBlank()) macros++
            return true
        }
    }

    /** Apa yang dilakukan sebuah tombol, sudah dalam istilah Amber. */
    private class Action(
        val keyCode: Int = 0,
        val comboKeys: List<Int> = emptyList(),
        val isMouse: Boolean = false,
        val mouseButton: Int = 0,
        val isScroll: Boolean = false,
        val systemAction: String = "",
        val command: String = ""
    )

    fun detect(root: JsonObject): Source? {
        if (root.has("mControlDataList") || root.has("mDrawerDataList") || root.has("mJoystickDataList")) {
            return Source.POJAV
        }
        val layers = root.get("layers")
        if (layers != null && layers.isJsonArray && root.has("editorVersion")) return Source.ZALITH2
        return null
    }

    fun convert(root: JsonObject, source: Source, refWidthDp: Float, refHeightDp: Float, maxButtons: Int): Result {
        val screen = Screen(
            w = if (refWidthDp.isFinite() && refWidthDp >= 200f) refWidthDp else 800f,
            h = if (refHeightDp.isFinite() && refHeightDp >= 100f) refHeightDp else 360f
        )
        val acc = Acc(maxButtons)
        when (source) {
            Source.POJAV -> convertPojav(root, screen, acc)
            Source.ZALITH2 -> convertZalith2(root, screen, acc)
        }
        return Result(acc.buttons, acc.skipped, acc.macros)
    }

    // ---- Pojav / Zalith 1 / Mojo ------------------------------------------------------------------

    private fun convertPojav(root: JsonObject, screen: Screen, acc: Acc) {
        for (o in root.objects("mControlDataList")) pojavButton(o, screen, acc, null)
        for (o in root.objects("mJoystickDataList")) pojavJoystick(o, screen, acc)
        for (o in root.objects("mDrawerDataList")) pojavDrawer(o, screen, acc)
    }

    private fun pojavButton(o: JsonObject, screen: Screen, acc: Acc, at: Pair<Float, Float>?) {
        val action = pojavAction(pojavKeys(o))
        if (action == null || (action.isScroll && acc.scrollAdded)) {
            acc.skipped++
            return
        }
        val w = o.floatOr("width", 50f)
        val h = o.floatOr("height", 50f)
        val center = at ?: pojavCenter(o, w, h, screen)
        val button = build(
            name = o.strOr("name"),
            action = action,
            xPercent = center.first,
            yPercent = center.second,
            widthDp = w,
            heightDp = h,
            opacity = o.floatOr("opacity", 0.85f),
            cornerDp = min(w, h) * o.floatOr("cornerRadius", 0f) / 200f,
            toggle = o.boolOr("isToggle", false),
            showMode = pojavShowMode(o)
        )
        if (acc.add(button) && action.isScroll) acc.scrollAdded = true
    }

    private fun pojavJoystick(o: JsonObject, screen: Screen, acc: Acc) {
        val w = o.floatOr("width", 150f)
        val h = o.floatOr("height", 150f)
        val center = pojavCenter(o, w, h, screen)
        val side = min(w, h).roundToInt().coerceIn(ControlLimits.MIN_JOYSTICK_DP, ControlLimits.MAX_JOYSTICK_DP)
        addJoystick(
            acc, center.first, center.second, side,
            o.floatOr("opacity", 0.85f), o.boolOr("forwardLock", false), pojavShowMode(o)
        )
    }

    /**
     * Laci Pojav tidak ada di Amber. Tombol di dalamnya dibentangkan di sebelah posisi laci sesuai arahnya
     * (kiri, kanan, atas, bawah); untuk laci bebas, tiap tombol memakai posisinya sendiri. Tombol laci itu
     * sendiri tidak dibuat.
     */
    private fun pojavDrawer(d: JsonObject, screen: Screen, acc: Acc) {
        val props = d.obj("properties") ?: return
        val subs = d.objects("buttonProperties")
        val orientation = d.strOr("orientation")
        val dw = props.floatOr("width", 50f)
        val dh = props.floatOr("height", 50f)
        val drawerCenter = pojavCenter(props, dw, dh, screen)
        val dir = when (orientation) {
            "LEFT", "UP" -> -1f
            "RIGHT", "DOWN" -> 1f
            else -> 0f
        }
        val horizontal = orientation == "LEFT" || orientation == "RIGHT"
        var cursor = 0f
        var prevHalf = (if (horizontal) dw else dh) / 2f
        for (sub in subs) {
            if (dir == 0f) {
                pojavButton(sub, screen, acc, null)
                continue
            }
            val sw = sub.floatOr("width", 50f)
            val sh = sub.floatOr("height", 50f)
            val half = (if (horizontal) sw else sh) / 2f
            cursor += dir * (prevHalf + DRAWER_GAP_DP + half)
            prevHalf = half
            val cx = drawerCenter.first + (if (horizontal) cursor / screen.w * 100f else 0f)
            val cy = drawerCenter.second + (if (horizontal) 0f else cursor / screen.h * 100f)
            pojavButton(sub, screen, acc, Pair(cx, cy))
        }
    }

    private fun pojavKeys(o: JsonObject): List<Int> {
        val arr = o.get("keycodes")
        val out = ArrayList<Int>()
        if (arr == null || !arr.isJsonArray) return out
        for (e in arr.asJsonArray) {
            if (out.size >= 4) break
            if (!e.isJsonPrimitive || !e.asJsonPrimitive.isNumber) continue
            val v = e.asDouble
            if (v.isNaN() || v.isInfinite()) continue
            out.add(v.roundToInt())
        }
        return out
    }

    /** Kode di bawah 0 adalah tombol khusus Pojav; 0 hanya pengisi; mulai 32 adalah tombol GLFW biasa. */
    private fun pojavAction(keys: List<Int>): Action? {
        val real = keys.filter { it in 32..MAX_KEYCODE }
        val special = keys.firstOrNull { it < 0 }
        if (real.isEmpty()) {
            return when (special) {
                -1 -> Action(systemAction = "KEYBOARD")
                -2 -> Action(systemAction = "HIDE_GUI")
                -3 -> Action(isMouse = true, mouseButton = LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT)
                -4 -> Action(isMouse = true, mouseButton = LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT)
                -6 -> Action(isMouse = true, mouseButton = LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_MIDDLE)
                -7, -8 -> Action(isScroll = true)
                -9 -> Action(systemAction = "MENU")
                else -> null // -5 (mouse virtual) dan kode tak dikenal
            }
        }
        return if (real.size == 1) {
            Action(keyCode = real[0])
        } else {
            Action(keyCode = real[0], comboKeys = real.take(MAX_COMBO_KEYS))
        }
    }

    private fun pojavShowMode(o: JsonObject): String {
        val inGame = o.boolOr("displayInGame", true)
        val inMenu = o.boolOr("displayInMenu", true)
        return when {
            inGame && !inMenu -> ControlShowMode.GAME
            inMenu && !inGame -> ControlShowMode.MENU
            else -> ControlShowMode.ALWAYS
        }
    }

    /** Titik tengah tombol dalam persen layar, dari ungkapan seperti "${margin} * 3 + ${width} * 2". */
    private fun pojavCenter(o: JsonObject, w: Float, h: Float, screen: Screen): Pair<Float, Float> {
        val vars = HashMap<String, Double>()
        vars["top"] = 0.0
        vars["left"] = 0.0
        vars["width"] = w.toDouble()
        vars["height"] = h.toDouble()
        vars["right"] = (screen.w - w).toDouble()
        vars["bottom"] = (screen.h - h).toDouble()
        vars["screen_width"] = screen.w.toDouble()
        vars["screen_height"] = screen.h.toDouble()
        vars["margin"] = MARGIN_DP
        vars["preferred_scale"] = 100.0
        val left = evalPosition(o.strOr("dynamicX"), vars)
        val top = evalPosition(o.strOr("dynamicY"), vars)
        val x = if (left != null) (left + w / 2.0) / screen.w * 100.0 else 50.0
        val y = if (top != null) (top + h / 2.0) / screen.h * 100.0 else 50.0
        return Pair(x.toFloat(), y.toFloat())
    }

    private fun evalPosition(raw: String, vars: Map<String, Double>): Double? {
        val text = raw.trim()
        if (text.isEmpty() || text.length > MAX_EXPRESSION) return null
        var missing = false
        val replaced = VAR_PATTERN.replace(text) { m ->
            val v = vars[m.groupValues[1]]
            if (v == null) {
                missing = true
                "0"
            } else {
                "(" + String.format(Locale.ROOT, "%.4f", v) + ")"
            }
        }
        if (missing) return null
        return ExprParser(replaced).parse()
    }

    /** Pengurai ungkapan aritmetika: angka, + - * / ^, tanda kurung, dan fungsi dp()/px() (sama di ruang dp). */
    private class ExprParser(private val s: String) {
        private var i = 0

        fun parse(): Double? {
            return try {
                val v = expr()
                skip()
                if (i != s.length || v.isNaN() || v.isInfinite()) null else v
            } catch (_: Exception) {
                null
            }
        }

        private fun skip() {
            while (i < s.length && s[i].isWhitespace()) i++
        }

        private fun expr(): Double {
            var v = term()
            while (true) {
                skip()
                if (i < s.length && (s[i] == '+' || s[i] == '-')) {
                    val op = s[i++]
                    val r = term()
                    v = if (op == '+') v + r else v - r
                } else {
                    return v
                }
            }
        }

        private fun term(): Double {
            var v = unary()
            while (true) {
                skip()
                if (i < s.length && (s[i] == '*' || s[i] == '/')) {
                    val op = s[i++]
                    val r = unary()
                    v = if (op == '*') v * r else v / r
                } else {
                    return v
                }
            }
        }

        private fun unary(): Double {
            skip()
            if (i < s.length && s[i] == '-') {
                i++
                return -unary()
            }
            if (i < s.length && s[i] == '+') {
                i++
                return unary()
            }
            val base = atom()
            skip()
            if (i < s.length && s[i] == '^') {
                i++
                return base.pow(unary())
            }
            return base
        }

        private fun atom(): Double {
            skip()
            if (i >= s.length) throw IllegalArgumentException("akhir ungkapan")
            val c = s[i]
            if (c == '(') {
                i++
                val v = expr()
                expect(')')
                return v
            }
            if (c.isDigit() || c == '.') {
                val start = i
                while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
                return s.substring(start, i).toDouble()
            }
            if (c.isLetter()) {
                val start = i
                while (i < s.length && s[i].isLetter()) i++
                val name = s.substring(start, i)
                skip()
                expect('(')
                val arg = expr()
                expect(')')
                if (name == "dp" || name == "px") return arg
                throw IllegalArgumentException("fungsi tak dikenal")
            }
            throw IllegalArgumentException("karakter tak dikenal")
        }

        private fun expect(ch: Char) {
            skip()
            if (i >= s.length || s[i] != ch) throw IllegalArgumentException("kurang $ch")
            i++
        }
    }

    // ---- Zalith Launcher 2 ------------------------------------------------------------------------

    private fun convertZalith2(root: JsonObject, screen: Screen, acc: Acc) {
        for (layer in root.objects("layers")) {
            val normals = layer.objects("normalButtons")
            val joysticks = layer.objects("joystickButtons")
            if (layer.boolOr("hide", false)) {
                // Layer yang disembunyikan biasanya dibuka lewat tombol lain; Amber tidak punya layer.
                acc.skipped += normals.size + joysticks.size
                continue
            }
            val layerMode = zalithShowMode(layer.strOr("visibilityType"))
            for (n in normals) zalithButton(n, layerMode, screen, acc)
            for (j in joysticks) zalithJoystick(j, layerMode, screen, acc)
        }
    }

    private fun zalithButton(o: JsonObject, layerMode: String, screen: Screen, acc: Acc) {
        val action = zalithAction(o.objects("clickEvents"))
        if (action == null || (action.isScroll && acc.scrollAdded)) {
            acc.skipped++
            return
        }
        val size = zalithSize(o.obj("buttonSize"), screen)
        val center = zalithCenter(o.obj("position"), size.first, size.second, screen)
        val ownMode = zalithShowMode(o.strOr("visibilityType"))
        val button = build(
            name = o.obj("text")?.strOr("default") ?: "",
            action = action,
            xPercent = center.first,
            yPercent = center.second,
            widthDp = size.first,
            heightDp = size.second,
            opacity = 0.85f,
            cornerDp = 8f,
            toggle = o.boolOr("isToggleable", false),
            showMode = if (ownMode == ControlShowMode.ALWAYS) layerMode else ownMode
        )
        if (acc.add(button) && action.isScroll) acc.scrollAdded = true
    }

    private fun zalithJoystick(o: JsonObject, layerMode: String, screen: Screen, acc: Acc) {
        val sidePx = if (o.strOr("sizeType") == "dp") {
            o.floatOr("sizeDp", 200f)
        } else {
            o.floatOr("sizePercentage", 2500f) / 10000f * screen.h
        }
        val side = sidePx.roundToInt().coerceIn(ControlLimits.MIN_JOYSTICK_DP, ControlLimits.MAX_JOYSTICK_DP)
        val center = zalithCenter(o.obj("position"), side.toFloat(), side.toFloat(), screen)
        val ownMode = zalithShowMode(o.strOr("visibilityType"))
        addJoystick(
            acc, center.first, center.second, side, 0.85f, o.boolOr("canLock", false),
            if (ownMode == ControlShowMode.ALWAYS) layerMode else ownMode
        )
    }

    /** Ukuran dalam dp: mutlak (dp) atau persen layar (100 = 1%) terhadap lebar/tinggi layar. */
    private fun zalithSize(o: JsonObject?, screen: Screen): Pair<Float, Float> {
        if (o == null) return Pair(50f, 48f)
        return when (o.strOr("type")) {
            "dp" -> Pair(o.floatOr("widthDp", 50f), o.floatOr("heightDp", 48f))
            "percentage" -> {
                val wRef = if (o.strOr("widthReference") == "screen_width") screen.w else screen.h
                val hRef = if (o.strOr("heightReference") == "screen_width") screen.w else screen.h
                Pair(
                    o.floatOr("widthPercentage", 1400f) / 10000f * wRef,
                    o.floatOr("heightPercentage", 1400f) / 10000f * hRef
                )
            }
            else -> Pair(50f, 48f)
        }
    }

    /** Posisi Zalith 2 (0..10000) adalah bagian dari ruang sisa layar setelah dikurangi ukuran tombol. */
    private fun zalithCenter(pos: JsonObject?, w: Float, h: Float, screen: Screen): Pair<Float, Float> {
        val px = (pos?.numOrNull("x") ?: 5000.0).coerceIn(0.0, 10000.0)
        val py = (pos?.numOrNull("y") ?: 5000.0).coerceIn(0.0, 10000.0)
        val left = (screen.w - w) * px / 10000.0
        val top = (screen.h - h) * py / 10000.0
        val x = (left + w / 2.0) / screen.w * 100.0
        val y = (top + h / 2.0) / screen.h * 100.0
        return Pair(x.toFloat(), y.toFloat())
    }

    private fun zalithShowMode(v: String): String = when (v) {
        "in_game" -> ControlShowMode.GAME
        "in_menu" -> ControlShowMode.MENU
        else -> ControlShowMode.ALWAYS
    }

    private fun zalithAction(events: List<JsonObject>): Action? {
        val keys = ArrayList<Int>()
        var other: Action? = null
        for (e in events) {
            val key = e.strOr("key")
            when (e.strOr("type")) {
                "key" -> {
                    val code = GLFW_KEY_BY_NAME[key]
                    if (code != null) {
                        if (keys.size < MAX_COMBO_KEYS && !keys.contains(code)) keys.add(code)
                    } else if (other == null) {
                        other = mouseByName(key)
                    }
                }
                "launcher_event" -> if (other == null) other = zalithLauncherEvent(key)
                "send_text" -> if (other == null && key.isNotBlank()) other = Action(command = key.take(MAX_COMMAND))
            }
        }
        if (other != null) return other
        return when {
            keys.isEmpty() -> null
            keys.size == 1 -> Action(keyCode = keys[0])
            else -> Action(keyCode = keys[0], comboKeys = keys)
        }
    }

    private fun zalithLauncherEvent(key: String): Action? = when {
        key == EVENT_KEYBOARD -> Action(systemAction = "KEYBOARD")
        key == EVENT_MENU -> Action(systemAction = "MENU")
        key.startsWith(EVENT_SCROLL_PREFIX) -> Action(isScroll = true)
        else -> mouseByName(key)
    }

    private fun mouseByName(key: String): Action? = when (key) {
        "GLFW_MOUSE_BUTTON_LEFT", "GLFW_MOUSE_BUTTON_1" ->
            Action(isMouse = true, mouseButton = LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT)
        "GLFW_MOUSE_BUTTON_RIGHT", "GLFW_MOUSE_BUTTON_2" ->
            Action(isMouse = true, mouseButton = LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT)
        "GLFW_MOUSE_BUTTON_MIDDLE", "GLFW_MOUSE_BUTTON_3" ->
            Action(isMouse = true, mouseButton = LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_MIDDLE)
        else -> null
    }

    // ---- Pembuat tombol Amber ---------------------------------------------------------------------

    private fun min(a: Float, b: Float): Float = if (a < b) a else b

    private fun newId(): String = "btn_" + UUID.randomUUID().toString().take(8)

    private fun labelFor(action: Action): String = when {
        action.systemAction == "KEYBOARD" -> "KB"
        action.systemAction == "MENU" -> "MENU"
        action.systemAction == "HIDE_GUI" -> "GUI"
        action.isScroll -> "SCROLL"
        action.isMouse -> when (action.mouseButton) {
            LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT -> "RMB"
            LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_MIDDLE -> "MMB"
            else -> "LMB"
        }
        action.command.isNotEmpty() -> "CMD"
        else -> KEY_LABEL_BY_CODE[action.keyCode] ?: "BTN"
    }

    private fun build(
        name: String,
        action: Action,
        xPercent: Float,
        yPercent: Float,
        widthDp: Float,
        heightDp: Float,
        opacity: Float,
        cornerDp: Float,
        toggle: Boolean,
        showMode: String
    ): CustomControlButton {
        val w = widthDp.roundToInt().coerceIn(ControlLimits.MIN_SIZE_DP, ControlLimits.MAX_WIDTH_DP)
        val h = heightDp.roundToInt().coerceIn(ControlLimits.MIN_SIZE_DP, ControlLimits.MAX_HEIGHT_DP)
        val isSystem = action.systemAction.isNotEmpty()
        val isCombo = action.comboKeys.size > 1
        val isCommand = action.command.isNotEmpty()
        val shown = name.trim().ifEmpty { labelFor(action) }.take(MAX_NAME)
        return CustomControlButton(
            id = newId(),
            name = shown,
            keyCode = action.keyCode,
            isMouseButton = action.isMouse,
            mouseButton = action.mouseButton,
            xPercent = xPercent.coerceIn(0f, 100f),
            yPercent = yPercent.coerceIn(0f, 100f),
            widthDp = w,
            heightDp = h,
            opacity = opacity.coerceIn(ControlLimits.MIN_OPACITY, 1f),
            cornerRadiusDp = cornerDp.roundToInt().coerceIn(0, min(w.toFloat(), h.toFloat()).toInt() / 2),
            isToggle = toggle && !isSystem && !action.isScroll,
            isScroll = action.isScroll,
            isSystem = isSystem,
            systemAction = action.systemAction,
            isMacro = isCombo || isCommand,
            macroType = if (isCombo) "COMBO" else "COMMAND",
            macroCommand = action.command,
            macroComboKey = if (isCombo) action.comboKeys[0] else 0,
            macroComboKeys = if (isCombo) action.comboKeys else emptyList(),
            showMode = if (isSystem || !ControlShowMode.isValid(showMode)) ControlShowMode.ALWAYS else showMode
        )
    }

    private fun addJoystick(
        acc: Acc,
        xPercent: Float,
        yPercent: Float,
        sideDp: Int,
        opacity: Float,
        autoSprint: Boolean,
        showMode: String
    ) {
        acc.add(
            CustomControlButton(
                id = newId(),
                name = "Joystick",
                keyCode = 0,
                xPercent = xPercent.coerceIn(0f, 100f),
                yPercent = yPercent.coerceIn(0f, 100f),
                widthDp = sideDp,
                heightDp = sideDp,
                opacity = opacity.coerceIn(ControlLimits.MIN_OPACITY, 1f),
                cornerRadiusDp = sideDp / 2,
                isJoystick = true,
                autoSprint = autoSprint,
                showMode = if (ControlShowMode.isValid(showMode)) showMode else ControlShowMode.ALWAYS
            )
        )
    }

    // ---- Pembaca kolom JSON yang toleran ----------------------------------------------------------

    private fun JsonObject.strOr(key: String, def: String = ""): String {
        val e = get(key)
        if (e == null || !e.isJsonPrimitive) return def
        return e.asString
    }

    private fun JsonObject.numOrNull(key: String): Double? {
        val e = get(key)
        if (e == null || !e.isJsonPrimitive) return null
        val p = e.asJsonPrimitive
        if (!p.isNumber) return null
        val v = p.asDouble
        return if (v.isNaN() || v.isInfinite()) null else v
    }

    private fun JsonObject.floatOr(key: String, def: Float): Float = numOrNull(key)?.toFloat() ?: def

    private fun JsonObject.boolOr(key: String, def: Boolean): Boolean {
        val e = get(key)
        if (e == null || !e.isJsonPrimitive) return def
        val p = e.asJsonPrimitive
        return if (p.isBoolean) p.asBoolean else def
    }

    private fun JsonObject.obj(key: String): JsonObject? {
        val e = get(key)
        return if (e != null && e.isJsonObject) e.asJsonObject else null
    }

    private fun JsonObject.objects(key: String): List<JsonObject> {
        val e = get(key)
        if (e == null || !e.isJsonArray) return emptyList()
        val out = ArrayList<JsonObject>()
        for (item in e.asJsonArray) if (item.isJsonObject) out.add(item.asJsonObject)
        return out
    }
}

// ----------------------------------------------------------------------------
// Section: core/controls/GamepadEmulator.kt
// ----------------------------------------------------------------------------

/** Tombol logis controller, lepas dari kode Android supaya pemetaannya mudah diuji. */
object Pad {
    const val A = 0
    const val B = 1
    const val X = 2
    const val Y = 3
    const val L1 = 4
    const val R1 = 5
    const val L2 = 6
    const val R2 = 7
    const val L3 = 8
    const val R3 = 9
    const val START = 10
    const val SELECT = 11
    const val UP = 12
    const val DOWN = 13
    const val LEFT = 14
    const val RIGHT = 15
    const val COUNT = 16

    /** Kode tombol Android -> tombol logis, atau -1 bila bukan tombol yang dipetakan. */
    fun fromKeyCode(code: Int): Int = when (code) {
        KeyEvent.KEYCODE_BUTTON_A -> A
        KeyEvent.KEYCODE_BUTTON_B -> B
        KeyEvent.KEYCODE_BUTTON_X -> X
        KeyEvent.KEYCODE_BUTTON_Y -> Y
        KeyEvent.KEYCODE_BUTTON_L1 -> L1
        KeyEvent.KEYCODE_BUTTON_R1 -> R1
        KeyEvent.KEYCODE_BUTTON_L2 -> L2
        KeyEvent.KEYCODE_BUTTON_R2 -> R2
        KeyEvent.KEYCODE_BUTTON_THUMBL -> L3
        KeyEvent.KEYCODE_BUTTON_THUMBR -> R3
        KeyEvent.KEYCODE_BUTTON_START -> START
        KeyEvent.KEYCODE_BUTTON_SELECT -> SELECT
        KeyEvent.KEYCODE_DPAD_UP -> UP
        KeyEvent.KEYCODE_DPAD_DOWN -> DOWN
        KeyEvent.KEYCODE_DPAD_LEFT -> LEFT
        KeyEvent.KEYCODE_DPAD_RIGHT -> RIGHT
        else -> -1
    }
}

/** Tempat hasil emulasi dikirim (CallbackBridge di game, objek palsu di uji). */
interface GamepadOutput {
    fun key(glfwKey: Int, down: Boolean)
    fun mouse(button: Int, down: Boolean)
    fun scroll(amount: Double)

    /** Putar kamera (kursor game terkunci), dalam piksel. */
    fun look(dx: Float, dy: Float)

    /** Geser kursor menu, dalam piksel layar. */
    fun cursor(dx: Float, dy: Float)
}

/**
 * Menerjemahkan controller menjadi keyboard + mouse supaya Minecraft biasa (tanpa mod controller)
 * langsung bisa dimainkan. Dua mode, dipilih dari keadaan kursor game:
 * - bermain (kursor terkunci): stick kiri = WASD, stick kanan = kamera, A lompat, B jongkok,
 *   RT pukul, LT pasang/pakai, LB/RB ganti slot, Y inventaris, X tukar tangan, Start = Esc.
 * - menu (kursor bebas): stick menggeser kursor, A/RT klik kiri, X/LT klik kanan,
 *   Y Shift+klik (pindah tumpukan), B/Start = Esc, D-pad = tombol panah.
 *
 * Kelas ini tidak menyentuh Android selain konstanta tombol, jadi seluruhnya berjalan di JVM biasa.
 * Tidak ada apa pun yang berjalan saat controller diam: [tick] hanya dipanggil selama stick digerakkan.
 */
class GamepadEmulator(private val out: GamepadOutput) {

    /** Pengali kecepatan kamera, kursor, dan vùng chết (1 = 100%). */
    var lookScale = 1.25f
    var cursorScale = 1f
    var deadzoneScale = 1f

    var grabbed = true
        private set

    private val keyDown = BooleanArray(Pad.COUNT)
    private val axisDown = BooleanArray(Pad.COUNT)
    private val active = BooleanArray(Pad.COUNT)
    private val issued = IntArray(Pad.COUNT)

    private var lx = 0f
    private var ly = 0f
    private var rx = 0f
    private var ry = 0f

    private var moveUp = false
    private var moveDown = false
    private var moveLeft = false
    private var moveRight = false

    /** Ganti mode (bermain/menu). Semua tombol yang sedang ditahan dilepas supaya tidak ada yang macet. */
    fun setGrabbed(value: Boolean) {
        if (value == grabbed) return
        releaseAll()
        grabbed = value
        updateMove()
    }

    /** Tombol fisik ditekan/dilepas (dari KeyEvent). */
    fun button(pad: Int, down: Boolean) {
        if (pad < 0 || pad >= Pad.COUNT) return
        keyDown[pad] = down
        refresh(pad)
    }

    /** Nilai sumbu terbaru. Dipanggil tiap MotionEvent joystick. */
    fun axes(lx: Float, ly: Float, rx: Float, ry: Float, lt: Float, rt: Float, hatX: Float, hatY: Float) {
        this.lx = lx
        this.ly = ly
        this.rx = rx
        this.ry = ry

        axisDown[Pad.L2] = hysteresis(axisDown[Pad.L2], lt, TRIGGER_ON, TRIGGER_OFF)
        axisDown[Pad.R2] = hysteresis(axisDown[Pad.R2], rt, TRIGGER_ON, TRIGGER_OFF)
        axisDown[Pad.LEFT] = hatX < -0.5f
        axisDown[Pad.RIGHT] = hatX > 0.5f
        axisDown[Pad.UP] = hatY < -0.5f
        axisDown[Pad.DOWN] = hatY > 0.5f
        refresh(Pad.L2)
        refresh(Pad.R2)
        refresh(Pad.LEFT)
        refresh(Pad.RIGHT)
        refresh(Pad.UP)
        refresh(Pad.DOWN)

        updateMove()
    }

    private fun deadZone(): Float = (DEAD_ZONE * deadzoneScale).coerceIn(0.05f, 0.7f)

    /** True selama stick digerakkan (butuh [tick] tiap frame). */
    fun needsTick(): Boolean {
        val dz = deadZone()
        val right = hypot(rx, ry)
        if (grabbed) return right > dz
        return right > dz || hypot(lx, ly) > dz
    }

    /**
     * Gerakkan kamera/kursor selama [dtSec] detik. Mengembalikan true bila masih perlu dipanggil
     * lagi di frame berikutnya.
     */
    fun tick(dtSec: Float): Boolean {
        var sx = rx
        var sy = ry
        if (!grabbed && hypot(lx, ly) > hypot(rx, ry)) {
            sx = lx
            sy = ly
        }
        val dz = deadZone()
        val mag = hypot(sx, sy)
        if (mag <= dz) return false

        val m = ((mag - dz) / (1f - dz)).coerceIn(0f, 1f)
        val curve = m * (0.4f + 0.6f * m)
        val dt = dtSec.coerceIn(0f, 0.05f)
        val scale = if (grabbed) LOOK_SPEED * lookScale else CURSOR_SPEED * cursorScale
        val step = curve * scale * dt
        val dx = sx / mag * step
        val dy = sy / mag * step
        if (grabbed) out.look(dx, dy) else out.cursor(dx, dy)
        return true
    }

    /** Lepas semua yang sedang ditahan emulator (tombol fisik tetap dicatat). */
    fun releaseAll() {
        for (b in 0 until Pad.COUNT) {
            val a = issued[b]
            if (a != 0) {
                issued[b] = 0
                perform(a, false)
            }
        }
        setMove(up = false, down = false, left = false, right = false)
    }

    /** Lupakan semuanya (aplikasi dijeda, controller dicabut, atau mod mengambil alih). */
    fun reset() {
        releaseAll()
        keyDown.fill(false)
        axisDown.fill(false)
        active.fill(false)
        lx = 0f
        ly = 0f
        rx = 0f
        ry = 0f
    }

    private fun refresh(pad: Int) {
        val now = keyDown[pad] || axisDown[pad]
        if (now == active[pad]) return
        active[pad] = now
        if (now) {
            val a = if (grabbed) gameAction(pad) else menuAction(pad)
            issued[pad] = a
            if (a != 0) perform(a, true)
        } else {
            val a = issued[pad]
            issued[pad] = 0
            if (a != 0) perform(a, false)
        }
    }

    private fun updateMove() {
        if (!grabbed) {
            setMove(up = false, down = false, left = false, right = false)
            return
        }
        val onThreshold = (MOVE_ON * deadzoneScale).coerceIn(0.15f, 0.85f)
        val offThreshold = (MOVE_OFF * deadzoneScale).coerceIn(0.10f, onThreshold - 0.05f)
        setMove(
            up = hysteresis(moveUp, -ly, onThreshold, offThreshold),
            down = hysteresis(moveDown, ly, onThreshold, offThreshold),
            left = hysteresis(moveLeft, -lx, onThreshold, offThreshold),
            right = hysteresis(moveRight, lx, onThreshold, offThreshold)
        )
    }

    private fun setMove(up: Boolean, down: Boolean, left: Boolean, right: Boolean) {
        if (up != moveUp) { moveUp = up; out.key(LwjglGlfwKeycode.GLFW_KEY_W, up) }
        if (down != moveDown) { moveDown = down; out.key(LwjglGlfwKeycode.GLFW_KEY_S, down) }
        if (left != moveLeft) { moveLeft = left; out.key(LwjglGlfwKeycode.GLFW_KEY_A, left) }
        if (right != moveRight) { moveRight = right; out.key(LwjglGlfwKeycode.GLFW_KEY_D, right) }
    }

    private fun perform(action: Int, down: Boolean) {
        val arg = action and 0xFFFF
        when (action ushr 16) {
            KIND_KEY -> out.key(arg, down)
            KIND_MOUSE -> out.mouse(arg, down)
            KIND_SHIFT_CLICK -> if (down) {
                out.key(LwjglGlfwKeycode.GLFW_KEY_LEFT_SHIFT, true)
                out.mouse(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT, true)
            } else {
                out.mouse(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT, false)
                out.key(LwjglGlfwKeycode.GLFW_KEY_LEFT_SHIFT, false)
            }
            KIND_SCROLL -> if (down) out.scroll(if (arg == SCROLL_PREV) 1.0 else -1.0)
        }
    }

    private fun gameAction(pad: Int): Int = when (pad) {
        Pad.A -> key(LwjglGlfwKeycode.GLFW_KEY_SPACE)
        Pad.B -> key(LwjglGlfwKeycode.GLFW_KEY_LEFT_SHIFT)
        Pad.X -> key(LwjglGlfwKeycode.GLFW_KEY_F)
        Pad.Y -> key(LwjglGlfwKeycode.GLFW_KEY_E)
        Pad.L1, Pad.LEFT -> scroll(SCROLL_PREV)
        Pad.R1, Pad.RIGHT -> scroll(SCROLL_NEXT)
        Pad.L2 -> mouse(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT)
        Pad.R2 -> mouse(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT)
        Pad.L3 -> key(LwjglGlfwKeycode.GLFW_KEY_LEFT_CONTROL)
        Pad.R3 -> mouse(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_MIDDLE)
        Pad.START -> key(LwjglGlfwKeycode.GLFW_KEY_ESCAPE)
        Pad.SELECT -> key(LwjglGlfwKeycode.GLFW_KEY_TAB)
        Pad.UP -> key(LwjglGlfwKeycode.GLFW_KEY_F5)
        Pad.DOWN -> key(LwjglGlfwKeycode.GLFW_KEY_Q)
        else -> 0
    }

    private fun menuAction(pad: Int): Int = when (pad) {
        Pad.A, Pad.R2 -> mouse(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT)
        Pad.X, Pad.L2 -> mouse(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT)
        Pad.Y -> (KIND_SHIFT_CLICK shl 16)
        Pad.B, Pad.START -> key(LwjglGlfwKeycode.GLFW_KEY_ESCAPE)
        Pad.L1 -> scroll(SCROLL_PREV)
        Pad.R1 -> scroll(SCROLL_NEXT)
        Pad.UP -> key(LwjglGlfwKeycode.GLFW_KEY_UP)
        Pad.DOWN -> key(LwjglGlfwKeycode.GLFW_KEY_DOWN)
        Pad.LEFT -> key(LwjglGlfwKeycode.GLFW_KEY_LEFT)
        Pad.RIGHT -> key(LwjglGlfwKeycode.GLFW_KEY_RIGHT)
        else -> 0
    }

    private fun key(code: Int) = (KIND_KEY shl 16) or code
    private fun mouse(button: Int) = (KIND_MOUSE shl 16) or button
    private fun scroll(direction: Int) = (KIND_SCROLL shl 16) or direction

    private fun hysteresis(wasOn: Boolean, value: Float, on: Float, off: Float): Boolean =
        if (wasOn) value > off else value > on

    companion object {
        private const val KIND_KEY = 1
        private const val KIND_MOUSE = 2
        private const val KIND_SHIFT_CLICK = 3
        private const val KIND_SCROLL = 4
        private const val SCROLL_PREV = 1
        private const val SCROLL_NEXT = 2

        const val DEAD_ZONE = 0.18f
        private const val MOVE_ON = 0.45f
        private const val MOVE_OFF = 0.30f
        private const val TRIGGER_ON = 0.55f
        private const val TRIGGER_OFF = 0.35f

        /** Piksel per detik pada defleksi penuh (100%). */
        private const val LOOK_SPEED = 1100f
        private const val CURSOR_SPEED = 1000f
    }
}

/**
 * Pengulang arah untuk navigasi menu launcher: satu tekanan langsung, lalu menahan stick
 * mengulang pelan-pelan seperti tombol panah keyboard.
 */
class DirectionRepeater(
    private val firstDelayMs: Long = 380L,
    private val repeatMs: Long = 110L
) {
    /** Arah aktif: kode tombol panah Android, atau 0 bila stick di tengah. */
    var dir = 0
        private set
    private var nextAt = 0L

    /** Arah baru. True bila harus langsung menekan satu kali. */
    fun set(newDir: Int, now: Long): Boolean {
        if (newDir == dir) return false
        dir = newDir
        if (newDir == 0) return false
        nextAt = now + firstDelayMs
        return true
    }

    /** True bila sudah waktunya mengulang. */
    fun due(now: Long): Boolean {
        if (dir == 0 || now < nextAt) return false
        nextAt = now + repeatMs
        return true
    }

    /** Jeda (ms) sampai pengulangan berikutnya. */
    fun delayUntilNext(now: Long): Long = if (dir == 0) 0L else (nextAt - now).coerceAtLeast(1L)

    companion object {
        private const val ON = 0.6f
        private const val OFF = 0.35f

        /** Stick/hat -> kode tombol panah Android (19 atas, 20 bawah, 21 kiri, 22 kanan) atau 0. */
        fun directionOf(x: Float, y: Float, current: Int): Int {
            val ax = abs(x)
            val ay = abs(y)
            val limit = if (current != 0) OFF else ON
            if (maxOf(ax, ay) < limit) return 0
            return if (ax > ay) {
                if (x > 0f) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT
            } else {
                if (y > 0f) KeyEvent.KEYCODE_DPAD_DOWN else KeyEvent.KEYCODE_DPAD_UP
            }
        }
    }
}

// ----------------------------------------------------------------------------
// Section: core/controls/GamepadRouter.kt
// ----------------------------------------------------------------------------

/**
 * Jalur controller di dalam game. Dipanggil dari GameActivity di thread utama.
 *
 * Satu controller hanya punya satu pemilik, dipilih otomatis:
 * 1. Mod yang membaca controller lewat SDL (Controlify) atau GLFW langsung: tombol dan stick
 *    diteruskan apa adanya, launcher tidak ikut menerjemahkan (supaya tidak dobel).
 * 2. Tanpa mod: controller diterjemahkan menjadi keyboard + mouse oleh [GamepadEmulator].
 *
 * Tidak ada pekerjaan saat controller diam: pencacah frame hanya berjalan selama stick digerakkan.
 */
object GamepadRouter {

    /** Gerakan stick/trigger/hat di atas ini dianggap disengaja (di bawahnya diabaikan sebagai drift). */
    private const val DELIBERATE = 0.5f

    /** Diisi layar game: menggeser kursor menu (dalam piksel layar). */
    @Volatile
    var cursorMover: ((Float, Float) -> Unit)? = null

    private val emulator = GamepadEmulator(object : GamepadOutput {
        override fun key(glfwKey: Int, down: Boolean) = CallbackBridge.sendKeyPress(glfwKey, down)
        override fun mouse(button: Int, down: Boolean) = CallbackBridge.sendMouseButton(button, down)
        override fun scroll(amount: Double) = CallbackBridge.sendScroll(0.0, amount)
        override fun look(dx: Float, dy: Float) = CallbackBridge.sendCursorDelta(dx, dy)
        override fun cursor(dx: Float, dy: Float) {
            cursorMover?.invoke(dx, dy)
        }
    })

    private var emulating = false
    private var lastOwner = ""
    private var ticking = false
    private var lastFrameNanos = 0L

    // Sumbu stick kanan dan trigger berbeda-beda antar merek: dicari sekali per perangkat.
    private var axisDevice = Int.MIN_VALUE
    private var rightX = MotionEvent.AXIS_Z
    private var rightY = MotionEvent.AXIS_RZ
    private var leftTrigger = -1
    private var rightTrigger = -1

    private val frame = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            val dt = if (lastFrameNanos == 0L) 0.016f else (frameTimeNanos - lastFrameNanos) / 1_000_000_000f
            lastFrameNanos = frameTimeNanos
            applySettings()
            syncMode()
            if (emulator.tick(dt)) {
                Choreographer.getInstance().postFrameCallback(this)
            } else {
                ticking = false
                lastFrameNanos = 0L
            }
        }
    }

    /** Kejadian tombol ini berasal dari controller? */
    fun isPadKey(event: KeyEvent): Boolean {
        val s = event.source
        if ((s and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
            (s and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
        ) return true
        return KeyEvent.isGamepadButton(event.keyCode)
    }

    /** Gerakan stick/trigger/hat dari controller? */
    fun isPadMotion(event: MotionEvent): Boolean =
        (event.source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK &&
            event.actionMasked == MotionEvent.ACTION_MOVE

    /**
     * Stick atau tombol arah digerakkan dengan sengaja (bukan sekadar getar atau drift)? Dipakai untuk
     * menyembunyikan tombol layar otomatis begitu controller dipakai, sama seperti saat mouse fisik dipakai.
     * Tanpa alokasi; hanya dipanggil per kejadian gerak.
     */
    fun isDeliberate(event: MotionEvent): Boolean {
        findAxes(event)
        return abs(event.getAxisValue(MotionEvent.AXIS_X)) > DELIBERATE ||
            abs(event.getAxisValue(MotionEvent.AXIS_Y)) > DELIBERATE ||
            abs(event.getAxisValue(rightX)) > DELIBERATE ||
            abs(event.getAxisValue(rightY)) > DELIBERATE ||
            abs(event.getAxisValue(MotionEvent.AXIS_HAT_X)) > DELIBERATE ||
            abs(event.getAxisValue(MotionEvent.AXIS_HAT_Y)) > DELIBERATE ||
            (leftTrigger >= 0 && event.getAxisValue(leftTrigger) > DELIBERATE) ||
            (rightTrigger >= 0 && event.getAxisValue(rightTrigger) > DELIBERATE)
    }

    /** True bila kejadian sudah ditangani dan tidak perlu diteruskan ke jalur lain. */
    fun onKey(event: KeyEvent): Boolean {
        val code = event.keyCode
        val pad = Pad.fromKeyCode(code)
        if (pad < 0 && !KeyEvent.isGamepadButton(code)) return false

        val action = event.action
        if (action != KeyEvent.ACTION_DOWN && action != KeyEvent.ACTION_UP) return true
        if (action == KeyEvent.ACTION_DOWN && event.repeatCount > 0) return true

        if (modOwns(event.deviceId)) {
            stopEmulating()
            forwardKey(event)
            return true
        }
        if (pad >= 0) {
            note("emulasi keyboard+mouse")
            applySettings()
            syncMode()
            emulating = true
            emulator.button(pad, action == KeyEvent.ACTION_DOWN)
            ensureTicking()
        }
        return true
    }

    /** True bila gerakan sudah ditangani. */
    fun onMotion(event: MotionEvent): Boolean {
        if (modOwns(event.deviceId)) {
            stopEmulating()
            if (CallbackBridge.sGamepadDirectInput) handleGamepadMotionEvent(event)
            if (SdlBridge.sdlEnabled) {
                try {
                    SDLControllerManager.handleJoystickMotionEvent(event)
                } catch (_: Throwable) {
                }
            }
            return true
        }
        findAxes(event)
        applySettings()
        syncMode()
        emulating = true
        emulator.axes(
            lx = event.getAxisValue(MotionEvent.AXIS_X),
            ly = event.getAxisValue(MotionEvent.AXIS_Y),
            rx = event.getAxisValue(rightX),
            ry = event.getAxisValue(rightY),
            lt = if (leftTrigger >= 0) event.getAxisValue(leftTrigger) else 0f,
            rt = if (rightTrigger >= 0) event.getAxisValue(rightTrigger) else 0f,
            hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X),
            hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)
        )
        ensureTicking()
        return true
    }

    /** Layar dijeda, controller dicabut, atau game ditutup: lepaskan semua tombol yang tertahan. */
    fun reset() {
        stopEmulating()
    }

    private fun modOwns(deviceId: Int): Boolean {
        // Mod membaca lewat GLFW langsung: LWJGL yang sudah ditambal memberi tahu lewat onDirectInputEnable.
        if (CallbackBridge.sGamepadDirectInput) {
            note("diteruskan ke mod (GLFW)")
            return true
        }
        // Mod membaca lewat SDL (Controlify): SDL sudah mengenali controller ini.
        if (!SdlBridge.sdlEnabled) return false
        val known = try {
            SDLControllerManager.isKnownToSdl(deviceId)
        } catch (_: Throwable) {
            false
        }
        if (known) note("diteruskan ke mod (SDL)")
        return known
    }

    /** Catat ke log game saat pemilik controller berganti, untuk memudahkan pelacakan laporan. */
    private fun note(owner: String) {
        if (owner == lastOwner) return
        lastOwner = owner
        try {
            LoggerBridge.append("▷ [Controller] $owner")
        } catch (_: Throwable) {
        }
    }

    private fun forwardKey(event: KeyEvent) {
        if (CallbackBridge.sGamepadDirectInput) handleGamepadKeyEvent(event)
        if (SdlBridge.sdlEnabled) {
            try {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    SDLControllerManager.onNativePadDown(event.deviceId, event.keyCode, event.scanCode)
                } else {
                    SDLControllerManager.onNativePadUp(event.deviceId, event.keyCode, event.scanCode)
                }
            } catch (_: Throwable) {
            }
        }
    }

    private fun stopEmulating() {
        if (!emulating) return
        emulating = false
        emulator.reset()
    }

    private fun syncMode() {
        emulator.setGrabbed(ZLBridgeStates.cursorMode.value == CURSOR_DISABLED)
    }

    private fun applySettings() {
        val s = SettingsManager.settings.value
        emulator.lookScale = s.captureSensitivity / 100f
        emulator.cursorScale = s.cursorSensitivity / 100f
        emulator.deadzoneScale = s.controllerDeadzonePercent.coerceIn(50, 200) / 100f
    }

    private fun ensureTicking() {
        if (ticking || !emulator.needsTick()) return
        ticking = true
        lastFrameNanos = 0L
        Choreographer.getInstance().postFrameCallback(frame)
    }

    private fun findAxes(event: MotionEvent) {
        val id = event.deviceId
        if (id == axisDevice) return
        axisDevice = id
        val dev = event.device
        fun has(axis: Int) = dev?.getMotionRange(axis) != null
        rightX = if (has(MotionEvent.AXIS_RX)) MotionEvent.AXIS_RX else MotionEvent.AXIS_Z
        rightY = if (has(MotionEvent.AXIS_RY)) MotionEvent.AXIS_RY else MotionEvent.AXIS_RZ
        leftTrigger = when {
            has(MotionEvent.AXIS_LTRIGGER) -> MotionEvent.AXIS_LTRIGGER
            has(MotionEvent.AXIS_BRAKE) -> MotionEvent.AXIS_BRAKE
            else -> -1
        }
        rightTrigger = when {
            has(MotionEvent.AXIS_RTRIGGER) -> MotionEvent.AXIS_RTRIGGER
            has(MotionEvent.AXIS_GAS) -> MotionEvent.AXIS_GAS
            else -> -1
        }
    }
}

// ----------------------------------------------------------------------------
// Section: core/controls/NovaControlInputCore.kt
// ----------------------------------------------------------------------------

/**
 * NovaCraft unified control input utilities & geometry helpers:
 * - ControlLimits
 * - DexClassScanner
 * - HotbarGeometry & GuiScaleReader
 * - KeyCap & KeyboardLayout
 * - GamepadNav
 * - JoystickLogic
 */

object ControlLimits {
    const val MIN_SIZE_DP = 24
    const val MAX_WIDTH_DP = 600
    const val MAX_HEIGHT_DP = 400
    const val MIN_JOYSTICK_DP = 80
    const val MAX_JOYSTICK_DP = 400
    const val MAX_CORNER_DP = 28
    const val MIN_OPACITY = 0.1f
}

@Suppress("DEPRECATION")
object DexClassScanner {
    fun find(context: Context, simpleName: String, packagePrefix: String = "top.fifthlight."): Class<*>? {
        return try {
            val dex = DexFile(context.packageCodePath)
            try {
                val entries = dex.entries()
                while (entries.hasMoreElements()) {
                    val name = entries.nextElement()
                    if (name.startsWith(packagePrefix) && name.endsWith(".$simpleName")) {
                        return Class.forName(name, false, context.classLoader)
                    }
                }
                null
            } finally {
                try { dex.close() } catch (_: Throwable) {}
            }
        } catch (_: Throwable) {
            null
        }
    }
}

object HotbarGeometry {
    const val SLOTS = 9
    private const val BAR_W = 182
    private const val BAR_H = 22
    private const val TOP_SLACK = 4
    private const val MIN_GUI_W = 320
    private const val MIN_GUI_H = 240

    fun guiScale(winW: Int, winH: Int, option: Int): Int {
        val limit = if (option <= 0) Int.MAX_VALUE else option
        var i = 1
        while (i != limit && i < winW && i < winH &&
            winW / (i + 1) >= MIN_GUI_W && winH / (i + 1) >= MIN_GUI_H
        ) i++
        return i
    }

    fun slotAt(
        x: Float, y: Float,
        screenW: Float, screenH: Float,
        winW: Int, winH: Int,
        guiScaleOption: Int
    ): Int {
        if (screenW <= 0f || screenH <= 0f || winW <= 0 || winH <= 0) return -1
        val gx = x * (winW / screenW)
        val gy = y * (winH / screenH)
        val s = guiScale(winW, winH, guiScaleOption)

        val scaledW = (winW + s - 1) / s
        val scaledH = (winH + s - 1) / s
        val left = (scaledW / 2 - BAR_W / 2) * s
        val width = BAR_W * s
        val top = (scaledH - BAR_H - TOP_SLACK) * s

        if (gx < left || gx >= left + width) return -1
        if (gy < top) return -1
        val slot = ((gx - left) * SLOTS / width).toInt()
        return slot.coerceIn(0, SLOTS - 1)
    }
}

class GuiScaleReader(private val file: File) {
    private var lastCheckMs = Long.MIN_VALUE
    private var lastModified = -1L
    private var value = 0

    fun get(nowMs: Long): Int {
        if (lastCheckMs != Long.MIN_VALUE && nowMs - lastCheckMs < RECHECK_MS) return value
        lastCheckMs = nowMs
        try {
            val m = file.lastModified()
            if (m != lastModified) {
                lastModified = m
                value = if (m == 0L) 0 else file.useLines { parseGuiScale(it) }
            }
        } catch (_: Throwable) {
        }
        return value
    }

    companion object {
        private const val RECHECK_MS = 2000L

        fun parseGuiScale(lines: Sequence<String>): Int {
            for (line in lines) {
                val t = line.trim()
                if (t.startsWith("guiScale:")) {
                    return t.substring("guiScale:".length).trim().toIntOrNull()?.coerceIn(0, 16) ?: 0
                }
            }
            return 0
        }
    }
}

class KeyCap(
    val label: String,
    val code: Int?,
    val w: Float = 1f,
    name: String = label.uppercase()
) {
    val option: KeyOption? = if (code == null) null else KeyOption(name, code)
}

object KeyboardLayout {
    private fun k(label: String, code: Int, w: Float = 1f, name: String = label.uppercase()) =
        KeyCap(label, code, w, name)

    private fun gap(w: Float) = KeyCap("", null, w)

    val MAIN: List<List<KeyCap>> = listOf(
        listOf(
            k("Esc", K.GLFW_KEY_ESCAPE), gap(1f),
            k("F1", K.GLFW_KEY_F1), k("F2", K.GLFW_KEY_F2), k("F3", K.GLFW_KEY_F3), k("F4", K.GLFW_KEY_F4), gap(0.5f),
            k("F5", K.GLFW_KEY_F5), k("F6", K.GLFW_KEY_F6), k("F7", K.GLFW_KEY_F7), k("F8", K.GLFW_KEY_F8), gap(0.5f),
            k("F9", K.GLFW_KEY_F9), k("F10", K.GLFW_KEY_F10), k("F11", K.GLFW_KEY_F11), k("F12", K.GLFW_KEY_F12)
        ),
        listOf(
            k("`", K.GLFW_KEY_GRAVE_ACCENT),
            k("1", K.GLFW_KEY_1), k("2", K.GLFW_KEY_2), k("3", K.GLFW_KEY_3), k("4", K.GLFW_KEY_4), k("5", K.GLFW_KEY_5),
            k("6", K.GLFW_KEY_6), k("7", K.GLFW_KEY_7), k("8", K.GLFW_KEY_8), k("9", K.GLFW_KEY_9), k("0", K.GLFW_KEY_0),
            k("-", K.GLFW_KEY_MINUS), k("=", K.GLFW_KEY_EQUAL),
            k("Backspace", K.GLFW_KEY_BACKSPACE, 2f, "BKSP")
        ),
        listOf(
            k("Tab", K.GLFW_KEY_TAB, 1.5f),
            k("Q", K.GLFW_KEY_Q), k("W", K.GLFW_KEY_W), k("E", K.GLFW_KEY_E), k("R", K.GLFW_KEY_R), k("T", K.GLFW_KEY_T),
            k("Y", K.GLFW_KEY_Y), k("U", K.GLFW_KEY_U), k("I", K.GLFW_KEY_I), k("O", K.GLFW_KEY_O), k("P", K.GLFW_KEY_P),
            k("[", K.GLFW_KEY_LEFT_BRACKET), k("]", K.GLFW_KEY_RIGHT_BRACKET),
            k("\\", K.GLFW_KEY_BACKSLASH, 1.5f)
        ),
        listOf(
            k("Capslock", K.GLFW_KEY_CAPS_LOCK, 1.75f, "CAPS"),
            k("A", K.GLFW_KEY_A), k("S", K.GLFW_KEY_S), k("D", K.GLFW_KEY_D), k("F", K.GLFW_KEY_F), k("G", K.GLFW_KEY_G),
            k("H", K.GLFW_KEY_H), k("J", K.GLFW_KEY_J), k("K", K.GLFW_KEY_K), k("L", K.GLFW_KEY_L),
            k(";", K.GLFW_KEY_SEMICOLON), k("'", K.GLFW_KEY_APOSTROPHE),
            k("Enter", K.GLFW_KEY_ENTER, 2.25f)
        ),
        listOf(
            k("Shift", K.GLFW_KEY_LEFT_SHIFT, 2.25f),
            k("Z", K.GLFW_KEY_Z), k("X", K.GLFW_KEY_X), k("C", K.GLFW_KEY_C), k("V", K.GLFW_KEY_V),
            k("B", K.GLFW_KEY_B), k("N", K.GLFW_KEY_N), k("M", K.GLFW_KEY_M),
            k(",", K.GLFW_KEY_COMMA), k(".", K.GLFW_KEY_PERIOD), k("/", K.GLFW_KEY_SLASH),
            k("Shift", K.GLFW_KEY_RIGHT_SHIFT, 2.75f)
        ),
        listOf(
            k("Ctrl", K.GLFW_KEY_LEFT_CONTROL, 1.25f), k("Win", K.GLFW_KEY_LEFT_SUPER, 1.25f),
            k("Alt", K.GLFW_KEY_LEFT_ALT, 1.25f),
            k("Space", K.GLFW_KEY_SPACE, 6.25f),
            k("Alt", K.GLFW_KEY_RIGHT_ALT, 1.25f), k("Win", K.GLFW_KEY_RIGHT_SUPER, 1.25f),
            k("Menu", K.GLFW_KEY_MENU, 1.25f), k("Ctrl", K.GLFW_KEY_RIGHT_CONTROL, 1.25f)
        )
    )

    val NAV: List<List<KeyCap>> = listOf(
        listOf(
            k("PrtSc", K.GLFW_KEY_PRINT_SCREEN), k("ScrLk", K.GLFW_KEY_SCROLL_LOCK), k("Pause", K.GLFW_KEY_PAUSE),
            gap(12f)
        ),
        listOf(
            k("Insert", K.GLFW_KEY_INSERT), k("Home", K.GLFW_KEY_HOME), k("PgUp", K.GLFW_KEY_PAGE_UP),
            gap(8f),
            k("NumLk", K.GLFW_KEY_NUM_LOCK), k("/", K.GLFW_KEY_KP_DIVIDE, name = "NP/"),
            k("*", K.GLFW_KEY_KP_MULTIPLY, name = "NP*"), k("-", K.GLFW_KEY_KP_SUBTRACT, name = "NP-")
        ),
        listOf(
            k("Delete", K.GLFW_KEY_DELETE, name = "DEL"), k("End", K.GLFW_KEY_END), k("PgDn", K.GLFW_KEY_PAGE_DOWN),
            gap(8f),
            k("7", K.GLFW_KEY_KP_7, name = "NP7"), k("8", K.GLFW_KEY_KP_8, name = "NP8"),
            k("9", K.GLFW_KEY_KP_9, name = "NP9"), k("+", K.GLFW_KEY_KP_ADD, name = "NP+")
        ),
        listOf(
            gap(11f),
            k("4", K.GLFW_KEY_KP_4, name = "NP4"), k("5", K.GLFW_KEY_KP_5, name = "NP5"),
            k("6", K.GLFW_KEY_KP_6, name = "NP6"), k("=", K.GLFW_KEY_KP_EQUAL, name = "NP=")
        ),
        listOf(
            gap(1f), k("↑", K.GLFW_KEY_UP), gap(1f),
            gap(8f),
            k("1", K.GLFW_KEY_KP_1, name = "NP1"), k("2", K.GLFW_KEY_KP_2, name = "NP2"),
            k("3", K.GLFW_KEY_KP_3, name = "NP3"), k("Enter", K.GLFW_KEY_KP_ENTER, name = "NP↵")
        ),
        listOf(
            k("←", K.GLFW_KEY_LEFT), k("↓", K.GLFW_KEY_DOWN), k("→", K.GLFW_KEY_RIGHT),
            gap(9f),
            k("0", K.GLFW_KEY_KP_0, name = "NP0"), k(".", K.GLFW_KEY_KP_DECIMAL, name = "NP."),
            gap(1f)
        )
    )
}

object GamepadNav {

    @Volatile
    var bumperListener: ((Int) -> Unit)? = null

    fun install(window: Window, allowBumper: Boolean): () -> Unit {
        val base = window.callback ?: return {}
        if (base is NavCallback) return {}
        val callback = NavCallback(base, window, allowBumper)
        window.callback = callback
        return {
            callback.stop()
            if (window.callback === callback) window.callback = base
        }
    }

    internal fun isPad(event: KeyEvent): Boolean {
        val s = event.source
        if ((s and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
            (s and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
        ) return true
        return KeyEvent.isGamepadButton(event.keyCode)
    }

    private class NavCallback(
        private val base: Window.Callback,
        private val window: Window,
        private val allowBumper: Boolean
    ) : Window.Callback by base {

        private val handler = Handler(Looper.getMainLooper())
        private val repeater = DirectionRepeater()
        private var dpadKeyAt = 0L

        private val repeatTick = object : Runnable {
            override fun run() {
                val now = SystemClock.uptimeMillis()
                if (repeater.dir == 0) return
                if (!window.decorView.hasWindowFocus()) {
                    repeater.set(0, now)
                    return
                }
                if (repeater.due(now)) press(repeater.dir)
                handler.postDelayed(this, repeater.delayUntilNext(now).coerceAtLeast(16L))
            }
        }

        fun stop() {
            handler.removeCallbacks(repeatTick)
            repeater.set(0, SystemClock.uptimeMillis())
        }

        override fun dispatchKeyEvent(event: KeyEvent): Boolean {
            if (GamepadNav.isPad(event)) {
                when (event.keyCode) {
                    KeyEvent.KEYCODE_BUTTON_A ->
                        return base.dispatchKeyEvent(remap(event, KeyEvent.KEYCODE_DPAD_CENTER))
                    KeyEvent.KEYCODE_BUTTON_B ->
                        return base.dispatchKeyEvent(remap(event, KeyEvent.KEYCODE_BACK))
                    KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_R1 -> {
                        if (allowBumper && event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                            GamepadNav.bumperListener?.invoke(if (event.keyCode == KeyEvent.KEYCODE_BUTTON_L1) -1 else 1)
                        }
                        return true
                    }
                    KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
                    KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> dpadKeyAt = SystemClock.uptimeMillis()
                }
            }
            return base.dispatchKeyEvent(event)
        }

        override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
            if ((event.source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK &&
                event.actionMasked == MotionEvent.ACTION_MOVE
            ) {
                onStick(event)
                return true
            }
            return base.dispatchGenericMotionEvent(event)
        }

        private fun onStick(event: MotionEvent) {
            val now = SystemClock.uptimeMillis()
            var x = event.getAxisValue(MotionEvent.AXIS_X)
            var y = event.getAxisValue(MotionEvent.AXIS_Y)
            if (now - dpadKeyAt > 300L) {
                val hx = event.getAxisValue(MotionEvent.AXIS_HAT_X)
                val hy = event.getAxisValue(MotionEvent.AXIS_HAT_Y)
                if (abs(hx) > 0.5f) x = hx
                if (abs(hy) > 0.5f) y = hy
            }
            val dir = DirectionRepeater.directionOf(x, y, repeater.dir)
            if (dir == repeater.dir) return
            handler.removeCallbacks(repeatTick)
            if (repeater.set(dir, now)) {
                press(dir)
                handler.postDelayed(repeatTick, repeater.delayUntilNext(now).coerceAtLeast(16L))
            }
        }

        private fun press(keyCode: Int) {
            val t = SystemClock.uptimeMillis()
            base.dispatchKeyEvent(KeyEvent(t, t, KeyEvent.ACTION_DOWN, keyCode, 0))
            base.dispatchKeyEvent(KeyEvent(t, t, KeyEvent.ACTION_UP, keyCode, 0))
        }

        private fun remap(e: KeyEvent, code: Int): KeyEvent = KeyEvent(
            e.downTime, e.eventTime, e.action, code, e.repeatCount, e.metaState,
            e.deviceId, e.scanCode, e.flags, InputDevice.SOURCE_KEYBOARD
        )
    }
}

class JoystickLogic {

    var w = false
        private set
    var a = false
        private set
    var s = false
        private set
    var d = false
        private set
    var sprint = false
        private set

    var knobX = 0f
        private set
    var knobY = 0f
        private set

    private var anchorX = 0f
    private var anchorY = 0f
    private var maxDrag = 1f
    private var radius = 1f
    private var active = false
    private var touching = false

    fun begin(x: Float, y: Float, cx: Float, cy: Float, radius: Float, maxDrag: Float) {
        this.radius = radius
        this.maxDrag = if (maxDrag > 1f) maxDrag else 1f
        val fromCenter = hypot(x - cx, y - cy)
        if (fromCenter <= OFF_FRACTION * this.maxDrag) {
            anchorX = cx
            anchorY = cy
        } else {
            anchorX = x
            anchorY = y
        }
        active = false
        touching = true
        clearOutput()
    }

    fun update(x: Float, y: Float, autoSprint: Boolean) {
        if (!touching) return
        val dx = x - anchorX
        val dy = y - anchorY
        val dist = hypot(dx, dy)

        if (dist > 0f) {
            val k = if (dist > maxDrag) maxDrag / dist else 1f
            knobX = dx * k
            knobY = dy * k
        } else {
            knobX = 0f
            knobY = 0f
        }

        val onAt = ON_FRACTION * maxDrag
        val offAt = OFF_FRACTION * maxDrag
        active = if (active) dist >= offAt else dist >= onAt

        if (!active) {
            w = false
            a = false
            s = false
            d = false
            sprint = false
            return
        }

        val deg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble()))
        w = deg >= -157.5 && deg <= -22.5
        s = deg >= 22.5 && deg <= 157.5
        d = deg >= -67.5 && deg <= 67.5
        a = !(deg >= -112.5 && deg <= 112.5)

        if (!w || !autoSprint) {
            sprint = false
        } else if (dist >= radius * SPRINT_FRACTION) {
            sprint = true
        }
    }

    fun release() {
        touching = false
        active = false
        clearOutput()
    }

    private fun clearOutput() {
        w = false
        a = false
        s = false
        d = false
        sprint = false
        knobX = 0f
        knobY = 0f
    }

    companion object {
        const val ON_FRACTION = 0.30f
        const val OFF_FRACTION = 0.20f
        const val SPRINT_FRACTION = 0.98f
    }
}

// ----------------------------------------------------------------------------
// Section: core/controls/TouchControllerBridge.kt
// ----------------------------------------------------------------------------

/**
 * Penerima sentuhan mentah dari layar game. Koordinat sudah dinormalkan 0..1 terhadap area game
 * (kiri atas = 0,0), seperti yang diminta protokol mod TouchController.
 */
interface TouchSink {
    fun onDown(id: Long, x: Float, y: Float)
    fun onMove(id: Long, x: Float, y: Float)
    fun onUp(id: Long)

    /** Batalkan semua jari (mis. layar ditinggalkan, menu terbuka). */
    fun onCancelAll()
}

/**
 * Mencatat jari mana saja yang sudah dikirim ke mod pada mode berdampingan, supaya gerak dan angkat
 * hanya dikirim untuk jari yang tadinya dikirim (jari yang dipakai tombol launcher atau yang mulai
 * saat di menu tidak pernah sampai ke mod). Tanpa alokasi per kejadian; maksimum [capacity] jari.
 */
class TouchForwarder(capacity: Int = 10) {
    private val ids = LongArray(capacity)
    private var count = 0

    private fun find(id: Long): Int {
        for (i in 0 until count) if (ids[i] == id) return i
        return -1
    }

    /**
     * Mengirim jari baru ke [sink]. Mengembalikan true bila benar-benar dikirim. Id yang ternyata
     * masih tercatat (angkatnya tidak pernah terlihat, mis. menu menutup layar saat jari turun)
     * dianggap sentuhan baru, bukan dibuang.
     */
    fun down(sink: TouchSink, id: Long, x: Float, y: Float): Boolean {
        if (find(id) < 0) {
            if (count >= ids.size) return false
            ids[count++] = id
        }
        sink.onDown(id, x, y)
        return true
    }

    fun move(sink: TouchSink, id: Long, x: Float, y: Float) {
        if (find(id) >= 0) sink.onMove(id, x, y)
    }

    fun up(sink: TouchSink, id: Long) {
        val at = find(id)
        if (at < 0) return
        ids[at] = ids[--count]
        sink.onUp(id)
    }

    /** Lupakan semua jari tercatat (setelah [TouchSink.onCancelAll]). */
    fun clear() {
        count = 0
    }
}

/**
 * Dukungan mod TouchController (https://github.com/TouchController/TouchController).
 *
 * Mod itu menggambar dan menjalankan kontrol sentuhnya sendiri di dalam game; launcher hanya perlu
 * meneruskan posisi jari lewat socket Unix yang namanya diberikan lewat [ENV_SOCKET]. Seluruh
 * pekerjaan ini hanya berjalan bila modnya terpasang: tanpa mod tidak ada thread, socket, maupun
 * penelusuran kelas sama sekali.
 */
object TouchControllerSupport {
    const val ENV_SOCKET = "TOUCH_CONTROLLER_PROXY_SOCKET"

    /**
     * Apakah salah satu berkas di folder mods adalah mod TouchController. Hanya memeriksa nama
     * berkas dari daftar yang sudah dimiliki launcher (tidak membaca disk lagi). Berkas yang
     * dimatikan (.disabled) tidak dihitung. DirectTouch (pengganti proxy dari sisi mod) diabaikan
     * karena ia membawa jembatannya sendiri.
     */
    fun isModInstalled(modFileNames: List<String>): Boolean {
        for (name in modFileNames) {
            val n = name.lowercase()
            if (!n.endsWith(".jar")) continue
            if (n.contains("directtouch")) continue
            if (n.contains("touchcontroller")) return true
        }
        return false
    }

    /** Nama socket unik per peluncuran. */
    fun newSocketName(): String = "amber_tc_" + UUID.randomUUID().toString().substring(0, 8)
}

/**
 * Jembatan ke library resmi `proxy-client-android` milik TouchController.
 *
 * Library dipanggil lewat refleksi supaya launcher tetap berjalan walau nama paket atau tanda
 * tangannya berbeda antarversi: bila ada yang tidak cocok, [start] mengembalikan null dan
 * kontrol bawaan launcher dipakai seperti biasa. Metode dicari sekali saat mulai, sesudah itu tiap
 * kejadian sentuh hanya satu pemanggilan metode (tanpa pencarian ulang).
 *
 * Nomor jari ("index") yang dikirim ke mod harus selalu naik, tidak seperti id jari Android yang
 * dipakai ulang, jadi setiap sentuhan baru diberi nomor berikutnya.
 *
 * Socketnya milik launcher dan modnya yang menyambung saat game berjalan. Jalur data dibungkus
 * sehingga launcher tahu kapan mod benar-benar mengirim sesuatu ([isConnected]); selama itu belum
 * terjadi, pemanggil tidak boleh mengambil alih sentuhan dari kontrol bawaan.
 */
class TouchControllerBridge private constructor(
    private val client: Any,
    private val addPointer: Method,
    private val removePointer: Method,
    private val clearPointer: Method,
    private val log: (String) -> Unit
) : TouchSink {

    /** True setelah mod mengirim pesan pertamanya (artinya modnya hidup dan terhubung). */
    @Volatile
    var isConnected = false
        internal set

    private val ids = LongArray(MAX_POINTERS)
    private val indexes = IntArray(MAX_POINTERS)
    private var count = 0
    private var nextIndex = 1
    private var failures = 0

    @Synchronized
    override fun onDown(id: Long, x: Float, y: Float) {
        var slot = find(id)
        if (slot < 0) {
            if (count >= MAX_POINTERS) return
            slot = count++
            ids[slot] = id
            indexes[slot] = nextIndex++
        }
        sendAdd(indexes[slot], x, y)
    }

    @Synchronized
    override fun onMove(id: Long, x: Float, y: Float) {
        val slot = find(id)
        if (slot < 0) {
            onDown(id, x, y)
            return
        }
        sendAdd(indexes[slot], x, y)
    }

    @Synchronized
    override fun onUp(id: Long) {
        val slot = find(id)
        if (slot < 0) return
        val index = indexes[slot]
        count--
        ids[slot] = ids[count]
        indexes[slot] = indexes[count]
        invoke(removePointer, index)
    }

    @Synchronized
    override fun onCancelAll() {
        if (count == 0) return
        count = 0
        invoke(clearPointer)
    }

    private fun find(id: Long): Int {
        for (i in 0 until count) if (ids[i] == id) return i
        return -1
    }

    private fun sendAdd(index: Int, x: Float, y: Float) {
        invoke(addPointer, index, x.coerceIn(0f, 1f), y.coerceIn(0f, 1f))
    }

    private fun invoke(method: Method, vararg args: Any) {
        if (failures >= MAX_FAILURES) return
        try {
            method.invoke(client, *args)
        } catch (t: Throwable) {
            failures++
            log("[TouchController] ${method.name} gagal (${t.cause?.message ?: t.message})" +
                if (failures >= MAX_FAILURES) "; pengiriman dihentikan" else "")
        }
    }

    /** Pencari kelas. Bagian pemindaian paket (lebih berat) hanya dipakai bila nama bawaan tidak ada. */
    fun interface ClassScanner {
        /** Kelas pertama yang nama sederhananya [simpleName], atau null. */
        fun find(simpleName: String): Class<*>?
    }

    companion object {
        private const val MAX_POINTERS = 10
        private const val MAX_FAILURES = 8

        private val CLIENT_CANDIDATES = listOf(
            "top.fifthlight.touchcontroller.proxy.client.LauncherProxyClient",
            "top.fifthlight.touchcontroller.proxy.client.android.LauncherProxyClient",
            "top.fifthlight.touchcontroller.proxy.LauncherProxyClient"
        )
        private val TRANSPORT_CANDIDATES = listOf(
            "top.fifthlight.touchcontroller.proxy.client.android.transport.UnixSocketTransportKt",
            "top.fifthlight.touchcontroller.proxy.client.android.UnixSocketTransportKt",
            "top.fifthlight.touchcontroller.proxy.client.transport.UnixSocketTransportKt",
            "top.fifthlight.touchcontroller.proxy.client.UnixSocketTransportKt"
        )
        private const val CLIENT_SIMPLE = "LauncherProxyClient"
        private const val TRANSPORT_SIMPLE = "UnixSocketTransportKt"

        private fun load(loader: ClassLoader?, names: List<String>): Class<*>? {
            for (n in names) {
                try {
                    return Class.forName(n, false, loader)
                } catch (_: Throwable) {
                }
            }
            return null
        }

        /**
         * Membuat dan menjalankan klien proxy untuk socket bernama [socketName]. Variabel lingkungan
         * [TouchControllerSupport.ENV_SOCKET] harus sudah diisi dengan nama yang sama sebelum game
         * dimulai. Mengembalikan null (dan menulis alasannya ke [log]) bila library tidak bisa dipakai.
         */
        fun start(
            socketName: String,
            loader: ClassLoader? = TouchControllerBridge::class.java.classLoader,
            scanner: ClassScanner? = null,
            log: (String) -> Unit,
            onConnected: (TouchControllerBridge) -> Unit = {}
        ): TouchControllerBridge? {
            try {
                val clientClass = load(loader, CLIENT_CANDIDATES)
                    ?: scanner?.find(CLIENT_SIMPLE)
                if (clientClass == null) {
                    log("[TouchController] kelas klien tidak ditemukan; memakai kontrol bawaan")
                    return null
                }

                val transportClass = load(loader, TRANSPORT_CANDIDATES)
                    ?: scanner?.find(TRANSPORT_SIMPLE)
                val factory = transportClass?.methods?.firstOrNull {
                    Modifier.isStatic(it.modifiers) && it.name == "UnixSocketTransport" &&
                        it.parameterCount == 1 && it.parameterTypes[0] == String::class.java
                }
                if (factory == null) {
                    log("[TouchController] pembuat transport tidak ditemukan; memakai kontrol bawaan")
                    return null
                }
                val realTransport = factory.invoke(null, socketName)
                    ?: run {
                        log("[TouchController] transport kosong; memakai kontrol bawaan")
                        return null
                    }
                val transportType = factory.returnType
                if (!transportType.isInterface) {
                    log("[TouchController] jenis transport tidak dikenal (${transportType.name}); memakai kontrol bawaan")
                    return null
                }

                // Pesan pertama dari mod = mod sudah tersambung. Dihitung sekali saja.
                val self = AtomicReference<TouchControllerBridge?>()
                val fired = AtomicBoolean(false)
                val transport = Proxy.newProxyInstance(
                    transportType.classLoader ?: TouchControllerBridge::class.java.classLoader,
                    arrayOf(transportType)
                ) { _, m, args ->
                    try {
                        val result = if (args == null) m.invoke(realTransport) else m.invoke(realTransport, *args)
                        if (result == true && m.name == "receive" && fired.compareAndSet(false, true)) {
                            self.get()?.let {
                                it.isConnected = true
                                try { onConnected(it) } catch (_: Throwable) {}
                            }
                        }
                        result
                    } catch (e: InvocationTargetException) {
                        throw e.targetException ?: e
                    }
                }

                val ctor = clientClass.constructors.firstOrNull {
                    it.parameterCount == 1 && it.parameterTypes[0].isInstance(transport)
                }
                if (ctor == null) {
                    log("[TouchController] konstruktor klien tidak cocok; memakai kontrol bawaan")
                    return null
                }
                val client = ctor.newInstance(transport)

                val methods = clientClass.methods
                val runMethod = methods.firstOrNull { it.name == "run" && it.parameterCount == 0 }
                val add = methods.firstOrNull {
                    it.name == "addPointer" && it.parameterCount == 3 &&
                        it.parameterTypes[0] == Int::class.javaPrimitiveType &&
                        it.parameterTypes[1] == Float::class.javaPrimitiveType &&
                        it.parameterTypes[2] == Float::class.javaPrimitiveType
                }
                val remove = methods.firstOrNull {
                    it.name == "removePointer" && it.parameterCount == 1 &&
                        it.parameterTypes[0] == Int::class.javaPrimitiveType
                }
                val clear = methods.firstOrNull { it.name == "clearPointer" && it.parameterCount == 0 }
                if (runMethod == null || add == null || remove == null || clear == null) {
                    val found = methods.filter { it.declaringClass == clientClass }
                        .joinToString { m -> m.name + m.parameterTypes.joinToString(",", "(", ")") { it.simpleName } }
                    log("[TouchController] metode klien tidak cocok ($found); memakai kontrol bawaan")
                    return null
                }

                val bridge = TouchControllerBridge(client, add, remove, clear, log)
                self.set(bridge)
                // run() bisa berupa perulangan yang tidak kembali: jalankan di thread sendiri.
                Thread({
                    try {
                        runMethod.invoke(client)
                    } catch (t: Throwable) {
                        log("[TouchController] klien berhenti: ${t.cause?.message ?: t.message}")
                    }
                }, "TouchController-Proxy").apply { isDaemon = true }.start()
                log("[TouchController] klien proxy menunggu mod (socket $socketName)")
                return bridge
            } catch (t: Throwable) {
                log("[TouchController] gagal memulai: ${t.cause?.message ?: t.message}; memakai kontrol bawaan")
                return null
            }
        }
    }
}
