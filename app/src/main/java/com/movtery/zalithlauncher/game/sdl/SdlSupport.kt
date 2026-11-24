package com.movtery.zalithlauncher.game.sdl

import android.app.Activity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Surface
import android.view.ViewGroup
import com.aistudio.novacraft.launcher.core.game.input.EfficientAndroidLWJGLKeycode
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.libsdl.app.SDL
import org.libsdl.app.SDLActivity
import org.libsdl.app.SDLSurface
import org.lwjgl.glfw.CallbackBridge

// ============================================================================
// Section: DirectGamepad.kt
// ============================================================================

// Reference AAMC (https://github.com/AngelAuraMC/Amethyst-Android/blob/21cfcdb15e21771810c830036097197ba9f21941/app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/customcontrols/gamepad/direct/DirectGamepad.java)

private const val GLFW_RELEASE: Byte = 0
private const val GLFW_PRESS: Byte = 1

private const val GLFW_GAMEPAD_BUTTON_A: Int = 0
private const val GLFW_GAMEPAD_BUTTON_B: Int = 1
private const val GLFW_GAMEPAD_BUTTON_X: Int = 2
private const val GLFW_GAMEPAD_BUTTON_Y: Int = 3
private const val GLFW_GAMEPAD_BUTTON_LEFT_BUMPER: Int = 4
private const val GLFW_GAMEPAD_BUTTON_RIGHT_BUMPER: Int = 5
private const val GLFW_GAMEPAD_BUTTON_BACK: Int = 6
private const val GLFW_GAMEPAD_BUTTON_START: Int = 7
// Home 键，未被使用，因为 Android 会自己响应 Home 键事件
private const val GLFW_GAMEPAD_BUTTON_GUIDE: Int = 8
private const val GLFW_GAMEPAD_BUTTON_LEFT_THUMB: Int = 9
private const val GLFW_GAMEPAD_BUTTON_RIGHT_THUMB: Int = 10
private const val GLFW_GAMEPAD_BUTTON_DPAD_UP: Int = 11
private const val GLFW_GAMEPAD_BUTTON_DPAD_RIGHT: Int = 12
private const val GLFW_GAMEPAD_BUTTON_DPAD_DOWN: Int = 13
private const val GLFW_GAMEPAD_BUTTON_DPAD_LEFT: Int = 14

private const val GLFW_GAMEPAD_AXIS_LEFT_X: Int = 0
private const val GLFW_GAMEPAD_AXIS_LEFT_Y: Int = 1
private const val GLFW_GAMEPAD_AXIS_RIGHT_X: Int = 2
private const val GLFW_GAMEPAD_AXIS_RIGHT_Y: Int = 3
private const val GLFW_GAMEPAD_AXIS_LEFT_TRIGGER: Int = 4
private const val GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER: Int = 5

private const val PRESS_THRESHOLD = 0.85f

fun handleGamepadKeyEvent(event: KeyEvent) {
    if (!CallbackBridge.sGamepadDirectInput) return
    val isDown = event.action == KeyEvent.ACTION_DOWN
    handleGamepadInput(event.keyCode, if (isDown) 1.0f else 0.0f)
}

fun handleGamepadMotionEvent(event: MotionEvent) {
    if (!CallbackBridge.sGamepadDirectInput) return
    handleGamepadInput(MotionEvent.AXIS_X, event.getAxisValue(MotionEvent.AXIS_X))
    handleGamepadInput(MotionEvent.AXIS_Y, event.getAxisValue(MotionEvent.AXIS_Y))
    handleGamepadInput(MotionEvent.AXIS_Z, event.getAxisValue(MotionEvent.AXIS_Z))
    handleGamepadInput(MotionEvent.AXIS_RZ, event.getAxisValue(MotionEvent.AXIS_RZ))
    handleGamepadInput(MotionEvent.AXIS_LTRIGGER, event.getAxisValue(MotionEvent.AXIS_LTRIGGER))
    handleGamepadInput(MotionEvent.AXIS_RTRIGGER, event.getAxisValue(MotionEvent.AXIS_RTRIGGER))
    handleGamepadInput(MotionEvent.AXIS_HAT_X, event.getAxisValue(MotionEvent.AXIS_HAT_X))
    handleGamepadInput(MotionEvent.AXIS_HAT_Y, event.getAxisValue(MotionEvent.AXIS_HAT_Y))
}

private fun handleGamepadInput(keycode: Int, value: Float) {
    var gKeycode = -1
    var gAxis = -1
    when (keycode) {
        KeyEvent.KEYCODE_BUTTON_A -> gKeycode = GLFW_GAMEPAD_BUTTON_A
        KeyEvent.KEYCODE_BUTTON_B -> gKeycode = GLFW_GAMEPAD_BUTTON_B
        KeyEvent.KEYCODE_BUTTON_X -> gKeycode = GLFW_GAMEPAD_BUTTON_X
        KeyEvent.KEYCODE_BUTTON_Y -> gKeycode = GLFW_GAMEPAD_BUTTON_Y
        KeyEvent.KEYCODE_BUTTON_L1 -> gKeycode = GLFW_GAMEPAD_BUTTON_LEFT_BUMPER
        KeyEvent.KEYCODE_BUTTON_R1 -> gKeycode = GLFW_GAMEPAD_BUTTON_RIGHT_BUMPER
        KeyEvent.KEYCODE_BUTTON_L2, MotionEvent.AXIS_LTRIGGER -> gAxis = GLFW_GAMEPAD_AXIS_LEFT_TRIGGER
        KeyEvent.KEYCODE_BUTTON_R2, MotionEvent.AXIS_RTRIGGER -> gAxis = GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER
        KeyEvent.KEYCODE_BUTTON_THUMBL -> gKeycode = GLFW_GAMEPAD_BUTTON_LEFT_THUMB
        KeyEvent.KEYCODE_BUTTON_THUMBR -> gKeycode = GLFW_GAMEPAD_BUTTON_RIGHT_THUMB
        KeyEvent.KEYCODE_BUTTON_START -> gKeycode = GLFW_GAMEPAD_BUTTON_START
        KeyEvent.KEYCODE_BUTTON_SELECT -> gKeycode = GLFW_GAMEPAD_BUTTON_BACK
        KeyEvent.KEYCODE_DPAD_UP -> gKeycode = GLFW_GAMEPAD_BUTTON_DPAD_UP
        KeyEvent.KEYCODE_DPAD_DOWN -> gKeycode = GLFW_GAMEPAD_BUTTON_DPAD_DOWN
        KeyEvent.KEYCODE_DPAD_LEFT -> gKeycode = GLFW_GAMEPAD_BUTTON_DPAD_LEFT
        KeyEvent.KEYCODE_DPAD_RIGHT -> gKeycode = GLFW_GAMEPAD_BUTTON_DPAD_RIGHT
        KeyEvent.KEYCODE_DPAD_CENTER -> {
            // Behave the same way as the Gamepad here, as GLFW doesn't have a keycode
            // for the dpad center.
            CallbackBridge.sGamepadButtonBuffer?.let { buf ->
                buf.put(GLFW_GAMEPAD_BUTTON_DPAD_UP, GLFW_RELEASE)
                buf.put(GLFW_GAMEPAD_BUTTON_DPAD_DOWN, GLFW_RELEASE)
                buf.put(GLFW_GAMEPAD_BUTTON_DPAD_LEFT, GLFW_RELEASE)
                buf.put(GLFW_GAMEPAD_BUTTON_DPAD_RIGHT, GLFW_RELEASE)
            }
            return
        }
        MotionEvent.AXIS_X -> gAxis = GLFW_GAMEPAD_AXIS_LEFT_X
        MotionEvent.AXIS_Y -> gAxis = GLFW_GAMEPAD_AXIS_LEFT_Y
        MotionEvent.AXIS_Z -> gAxis = GLFW_GAMEPAD_AXIS_RIGHT_X
        MotionEvent.AXIS_RZ -> gAxis = GLFW_GAMEPAD_AXIS_RIGHT_Y
        MotionEvent.AXIS_HAT_X -> {
            CallbackBridge.sGamepadButtonBuffer?.let { buf ->
                buf.put(
                    GLFW_GAMEPAD_BUTTON_DPAD_LEFT,
                    if (value < -PRESS_THRESHOLD) GLFW_PRESS else GLFW_RELEASE
                )
                buf.put(
                    GLFW_GAMEPAD_BUTTON_DPAD_RIGHT,
                    if (value > PRESS_THRESHOLD) GLFW_PRESS else GLFW_RELEASE
                )
            }
            return
        }
        MotionEvent.AXIS_HAT_Y -> {
            CallbackBridge.sGamepadButtonBuffer?.let { buf ->
                buf.put(
                    GLFW_GAMEPAD_BUTTON_DPAD_UP,
                    if (value < -PRESS_THRESHOLD) GLFW_PRESS else GLFW_RELEASE
                )
                buf.put(
                    GLFW_GAMEPAD_BUTTON_DPAD_DOWN,
                    if (value > PRESS_THRESHOLD) GLFW_PRESS else GLFW_RELEASE
                )
            }
            return
        }
        else -> return
    }
    if (gKeycode != -1) {
        CallbackBridge.sGamepadButtonBuffer?.put(
            gKeycode,
            if (value > PRESS_THRESHOLD) GLFW_PRESS else GLFW_RELEASE
        )
    }
    if (gAxis != -1) {
        CallbackBridge.sGamepadAxisBuffer?.put(gAxis, value)
    }
}

// ============================================================================
// Section: DirectGamepadEnableHandler.kt
// ============================================================================

fun interface DirectGamepadEnableHandler {
    fun onDirectGamepadEnabled()
}

// ============================================================================
// Section: SdlBridge.kt
// ============================================================================

object SdlBridge {
    private val sdlInitialized = AtomicBoolean(false)
    private val jniSetupDone = AtomicBoolean(false)
    private val surfaceLock = Any()

    @Volatile
    private var activeSurfaceSource: Any? = null

    @Volatile
    private var boundNativeSurface: Surface? = null

    @JvmStatic
    @Volatile
    var sdlEnabled: Boolean = false

    @JvmStatic
    @Volatile
    var sdlImeAutoShowEnabled: Boolean = true

    private val _composeFocus = MutableStateFlow(0)
    val composeFocus: StateFlow<Int> = _composeFocus.asStateFlow()

    @JvmStatic
    fun requestComposeFocus() {
        _composeFocus.update { it + 1 }
    }

    @JvmStatic
    fun markSdlInitialized(): Boolean {
        return sdlInitialized.compareAndSet(false, true)
    }

    @JvmStatic
    fun clearSdlInitialized() {
        sdlInitialized.set(false)
        jniSetupDone.set(false)
    }

    @JvmStatic
    fun setupJNI() {
        if (jniSetupDone.compareAndSet(false, true)) {
            SDL.setupJNI()
        }
    }

    @JvmStatic
    fun prepareSurface(
        activity: Activity,
        surface: Surface,
        rootLayout: ViewGroup?,
        source: Any
    ) {
        synchronized(surfaceLock) {
            activeSurfaceSource = source
            boundNativeSurface = surface
            SDL.setContext(activity)
            val sdlSurface = SDLActivity.getSDLSurface()?.takeIf { existing ->
                existing.context === activity
            } ?: SDLSurface(activity)
            SDLActivity.externalInitialize(sdlSurface, rootLayout, surface)
        }
    }

    @JvmStatic
    fun beginSurfaceDestroy(source: Any, nativeSurface: Surface?): Boolean {
        synchronized(surfaceLock) {
            val currentSource = activeSurfaceSource
            val currentSurface = boundNativeSurface
            if (currentSource != null && currentSource !== source &&
                (nativeSurface == null || currentSurface == null || currentSurface !== nativeSurface)
            ) {
                return false
            }
            return true
        }
    }

    @JvmStatic
    fun unregisterSurface(nativeSurface: Surface?) {
        synchronized(surfaceLock) {
            if (nativeSurface == null || boundNativeSurface == null || boundNativeSurface === nativeSurface) {
                activeSurfaceSource = null
                boundNativeSurface = null
                SDLSurface.clearNativeSurface()
            }
        }
    }
}

// ============================================================================
// Section: SdlTextSender.kt
// ============================================================================

object SdlTextSender {
    @JvmStatic
    fun sendChar(character: Char) {
        if (!SdlBridge.sdlEnabled) return
        SDLActivity.onNativeTextInput(character.toString())
    }

    @JvmStatic
    fun sendEnter() {
        if (!SdlBridge.sdlEnabled) return
        SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_ENTER)
        SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_ENTER)
    }

    @JvmStatic
    fun sendKey(lwjglGlfwKeycode: Int) {
        if (!SdlBridge.sdlEnabled) return
        val keyCode = EfficientAndroidLWJGLKeycode.getSdlAndroidKeycode(lwjglGlfwKeycode)
        if (keyCode == KeyEvent.KEYCODE_UNKNOWN) return
        SDLActivity.onNativeKeyDown(keyCode)
        SDLActivity.onNativeKeyUp(keyCode)
    }
}
