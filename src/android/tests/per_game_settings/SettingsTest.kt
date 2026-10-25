// Copyright 2026 Azahar Emulator Project
// Licensed under GPLv3 or any later version

import java.io.File
import kotlin.math.abs
import org.citra.citra_emu.NativeLibrary
import org.citra.citra_emu.features.settings.model.BooleanSetting
import org.citra.citra_emu.features.settings.model.CitraSettings
import org.citra.citra_emu.features.settings.model.FloatSetting
import org.citra.citra_emu.features.settings.model.IntSetting
import org.citra.citra_emu.features.settings.model.SettingsViewModel
import org.citra.citra_emu.features.settings.ui.SettingsActivityPresenter
import org.citra.citra_emu.features.settings.ui.SettingsActivityView
import org.citra.citra_emu.features.settings.utils.SettingsFile
import org.citra.citra_emu.utils.DirectoryInitialization

fun main(args: Array<String>) {
    val root = File(args[0]).apply { mkdirs() }
    DirectoryInitialization.userDirectory = root.path
    val config = File(root, "config/config.ini").apply {
        parentFile.mkdirs()
        writeText("[Renderer]\nresolution_factor=2\nuse_vsync=true\n[Audio]\nvolume=0.6\n")
    }
    val a = "0004000000123400"
    val b = "0004000000567800"
    val settings = CitraSettings()
    SettingsFile.loadSettings(settings, a)
    check(settings.get(IntSetting.RESOLUTION_FACTOR) == 2)
    check(!settings.hasOverride(IntSetting.RESOLUTION_FACTOR))
    settings.setBySettingsContext(IntSetting.RESOLUTION_FACTOR, 4)
    settings.setBySettingsContext(BooleanSetting.VSYNC, false)
    settings.setBySettingsContext(FloatSetting.AUDIO_VOLUME, 30f)
    check(settings.getGlobal(IntSetting.RESOLUTION_FACTOR) == 2)
    check(SettingsFile.saveCustomFile(settings))
    check(config.readText().contains("resolution_factor=2"))
    val profile = File(root, "config/custom/$a.ini")
    check(profile.readText().contains("/use_global"))

    // A new process/model receives exactly the saved sparse overrides.
    val reopened = CitraSettings()
    SettingsFile.loadSettings(reopened, a)
    check(reopened.get(IntSetting.RESOLUTION_FACTOR) == 4)
    check(!reopened.get(BooleanSetting.VSYNC))
    check(abs(reopened.get(FloatSetting.AUDIO_VOLUME) - 30f) < 0.001f)
    check(reopened.getAllOverrides().size == 3)

    // Reusing a settings object across titles must remove previous overrides.
    SettingsFile.loadSettings(reopened, b)
    check(reopened.gameId == b && reopened.getAllOverrides().isEmpty())
    check(reopened.get(IntSetting.RESOLUTION_FACTOR) == 2)
    SettingsFile.loadSettings(reopened, a)
    reopened.clearOverride(IntSetting.RESOLUTION_FACTOR)
    check(SettingsFile.saveCustomFile(reopened))
    check(!profile.readText().contains("resolution_factor"))
    config.writeText(config.readText().replace("resolution_factor=2", "resolution_factor=3"))
    SettingsFile.loadSettings(reopened, a)
    check(reopened.get(IntSetting.RESOLUTION_FACTOR) == 3)
    check(!reopened.get(BooleanSetting.VSYNC))

    // Updating a profile through a quick control must stay in that profile.
    reopened.setByCurrentState(BooleanSetting.VSYNC, true)
    SettingsFile.saveSetting(BooleanSetting.VSYNC, reopened)
    SettingsFile.loadSettings(reopened, a)
    check(reopened.hasOverride(BooleanSetting.VSYNC) && reopened.get(BooleanSetting.VSYNC))

    // Whole-profile reset preserves global settings and removes the file.
    reopened.clearAll()
    check(SettingsFile.saveCustomFile(reopened))
    check(!profile.exists())
    SettingsFile.loadSettings(reopened, a)
    check(reopened.get(IntSetting.RESOLUTION_FACTOR) == 3)
    check(abs(reopened.get(FloatSetting.AUDIO_VOLUME) - 60f) < 0.001f)

    // Desktop inheritance flags work regardless of ordering. Credentials and
    // unknown/wrong sections must never leak from a game profile.
    profile.writeText(
        "[Renderer]\nresolution_factor=9\nresolution_factor/use_global=true\n" +
            "use_vsync/use_global=false\nuse_vsync=false\n" +
            "[WebService]\nnetwork_token=secret\n[Wrong]\nvolume=5\n"
    )
    SettingsFile.loadSettings(reopened, a)
    check(reopened.get(IntSetting.RESOLUTION_FACTOR) == 3)
    check(!reopened.get(BooleanSetting.VSYNC))
    check(reopened.getAllOverrides().size == 1)
    SettingsFile.loadSettings(reopened)
    check(!reopened.isPerGame() && reopened.getAllOverrides().isEmpty())
    check(reopened.get(BooleanSetting.VSYNC))
    SettingsFile.loadSettings(reopened, "../../invalid")
    check(!reopened.isPerGame())
    SettingsFile.loadSettings(reopened, "0000000000000000")
    check(!reopened.isPerGame())

    // Editing a different title must not change the active title's overrides.
    val active = CitraSettings().apply { gameId = a }
    active.setOverride(IntSetting.RESOLUTION_FACTOR, 6)
    val other = CitraSettings().apply { gameId = b }
    other.setOverride(IntSetting.RESOLUTION_FACTOR, 8)
    active.mergeSettings(other)
    check(active.get(IntSetting.RESOLUTION_FACTOR) == 6)
    active.removePerGameSettings()
    check(!active.isPerGame() && active.getAllOverrides().isEmpty())

    // An offline editor cannot activate its profile. The current game's editor
    // updates only that game's running settings and survives activity rotation.
    SettingsFile.loadSettings(CitraSettings.activeSettings)
    val model = SettingsViewModel()
    val view = TestView(model.editingSettings)
    val presenter = SettingsActivityPresenter(view, model)
    presenter.onCreate(null, "config", a)
    model.editingSettings.setBySettingsContext(IntSetting.RESOLUTION_FACTOR, 5)
    presenter.onSettingChanged()
    val bundle = android.os.Bundle()
    presenter.saveState(bundle)
    val recreated = SettingsActivityPresenter(view, model)
    recreated.onCreate(bundle, "config", a)
    check(model.editingSettings.get(IntSetting.RESOLUTION_FACTOR) == 5)
    recreated.onStop(true)
    check(!CitraSettings.activeSettings.isPerGame())
    check(NativeLibrary.reloads == 0)
    model.editingSettings.setBySettingsContext(IntSetting.RESOLUTION_FACTOR, 6)
    recreated.onSettingChanged()
    recreated.onStop(false)
    val backgrounded = CitraSettings()
    SettingsFile.loadSettings(backgrounded, a)
    check(backgrounded.get(IntSetting.RESOLUTION_FACTOR) == 6)

    SettingsFile.loadSettings(CitraSettings.activeSettings, a)
    val runningModel = SettingsViewModel()
    val running = SettingsActivityPresenter(TestView(runningModel.editingSettings), runningModel)
    running.onCreate(null, "config", a)
    runningModel.editingSettings.setBySettingsContext(IntSetting.RESOLUTION_FACTOR, 7)
    running.onSettingChanged()
    running.onStop(true)
    check(CitraSettings.activeSettings.get(IntSetting.RESOLUTION_FACTOR) == 7)
    check(NativeLibrary.reloads == 1)
    runningModel.editingSettings.clearAll()
    running.onSettingChanged()
    running.onStop(true)
    check(CitraSettings.activeSettings.get(IntSetting.RESOLUTION_FACTOR) == 3)
    check(!profile.exists())
    val interop = CitraSettings()
    SettingsFile.loadSettings(interop, a)
    interop.setBySettingsContext(BooleanSetting.DEBUG_RENDERER, true)
    interop.setBySettingsContext(IntSetting.CPU_CLOCK_SPEED, 150)
    interop.setBySettingsContext(BooleanSetting.CPU_JIT, false)
    check(SettingsFile.saveCustomFile(interop))
    SettingsFile.loadSettings(interop, a)
    check(interop.get(BooleanSetting.DEBUG_RENDERER))
    check(interop.get(IntSetting.CPU_CLOCK_SPEED) == 150)
    check(!interop.get(BooleanSetting.CPU_JIT))
    SettingsFile.loadSettings(interop, b)
    check(!interop.get(BooleanSetting.DEBUG_RENDERER))
    check(interop.get(IntSetting.CPU_CLOCK_SPEED) == 100)
    SettingsFile.loadSettings(interop, a)
    interop.clearOverride(BooleanSetting.DEBUG_RENDERER)
    check(SettingsFile.saveCustomFile(interop))
    SettingsFile.loadSettings(interop, a)
    check(!interop.hasOverride(BooleanSetting.DEBUG_RENDERER))
    check(!interop.get(BooleanSetting.DEBUG_RENDERER))
    check(interop.get(IntSetting.CPU_CLOCK_SPEED) == 150)
    interop.clearAll()
    check(SettingsFile.saveCustomFile(interop))
    SettingsFile.loadSettings(interop, a)
    interop.setBySettingsContext(IntSetting.RESOLUTION_FACTOR, 4)
    interop.setBySettingsContext(BooleanSetting.VSYNC, false)
    interop.setBySettingsContext(FloatSetting.AUDIO_VOLUME, 30f)
    interop.setBySettingsContext(BooleanSetting.DEBUG_RENDERER, true)
    interop.setBySettingsContext(IntSetting.CPU_CLOCK_SPEED, 150)
    check(SettingsFile.saveCustomFile(interop))
    println("Kotlin model and persistence regression checks passed")
}

private class TestView(override val editingSettings: CitraSettings) : SettingsActivityView {
    override fun showSettingsFragment(menuTag: String, addToStack: Boolean, gameId: String) {}
    override fun onSettingsFileLoaded() {}
    override fun onSettingsFileNotFound() {
        error("Settings file not found")
    }
    override fun showToastMessage(message: String, isLong: Boolean) {
        error(message)
    }
    override fun finish() {}
    override fun onSettingChanged() {}
}
