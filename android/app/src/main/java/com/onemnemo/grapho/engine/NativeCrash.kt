package com.onemnemo.grapho.engine

/** Makes the native side write a panic to a file in [dir] before the app dies. See ffi-android/src/crash.rs. */
object NativeCrash {
    init {
        System.loadLibrary("grapho_android")
    }

    @JvmStatic external fun nativeInstall(dir: String)
}
