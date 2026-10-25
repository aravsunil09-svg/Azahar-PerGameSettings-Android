// Copyright 2026 Citra Emulator Project / Azahar Emulator Project
// Licensed under GPLv3 or any later version
// Refer to the LICENSE.txt file included.

package org.citra.citra_emu.features.settings.model

interface AbstractSetting<T> {
    val key: String
    val section: String
    val defaultValue: T
    val supportsPerGame: Boolean
        get() = section in setOf(
            CitraSettings.SECTION_CORE,
            CitraSettings.SECTION_SYSTEM,
            CitraSettings.SECTION_RENDERER,
            CitraSettings.SECTION_LAYOUT,
            CitraSettings.SECTION_UTILITY,
            CitraSettings.SECTION_AUDIO,
            CitraSettings.SECTION_DEBUG
        )

    val isRuntimeEditable: Boolean

    fun valueToString(value: T): String = value.toString()

    fun valueFromString(string: String): T?
}
