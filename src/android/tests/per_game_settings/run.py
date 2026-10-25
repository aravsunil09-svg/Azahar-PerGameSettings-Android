"""Host regression tests for the real Android model, persistence and INI reader.

Requires Python 3, Java/Kotlin, ini4j and a C++ compiler (or --zig).
Android/SAF adapters below use a temporary local filesystem. This does not test
Android provider permissions, lifecycle, UI rendering, JNI or the emulator.
"""
import argparse
import os
from pathlib import Path
import re
import subprocess
import tempfile

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--java', default='java')
parser.add_argument('--kotlin-home', required=True, type=Path)
parser.add_argument('--ini4j', required=True, type=Path)
parser.add_argument('--cxx', default='c++')
parser.add_argument('--zig', type=Path)
parser.add_argument('--generated-headers', type=Path,
                    help='Android CMake generated src directory containing common/setting_keys.h')
args = parser.parse_args()
here = Path(__file__).resolve().parent
repo = here.parents[3]
java = repo / 'src/android/app/src/main/java/org/citra/citra_emu'
headers = args.generated_headers
if headers is None:
    candidates = list((repo / 'src/android/app/.cxx').glob('*/*/*/src/common/setting_keys.h'))
    if not candidates:
        parser.error('Configure the Android CMake build first or pass --generated-headers')
    headers = candidates[0].parents[1]
model = java / 'features/settings/model'
sources = [model / f'{name}.kt' for name in (
    'AbstractSetting', 'CitraSettings', 'BooleanSetting', 'IntSetting',
    'FloatSetting', 'StringSetting', 'IntListSetting', 'SettingsViewModel',
)] + [java / 'features/settings/utils/SettingsFile.kt',
      java / 'features/settings/ui/SettingsActivityView.kt',
      java / 'features/settings/ui/SettingsActivityPresenter.kt',
      here / 'SettingsTest.kt']

with tempfile.TemporaryDirectory(prefix='azahar-settings-') as tmp:
    build = Path(tmp)
    text = '\n'.join(p.read_text(encoding='utf-8') for p in sources)
    resources = sorted(set(re.findall(r'R\.string\.(\w+)', text)))
    keys = sorted(set(re.findall(r'SettingKeys\.(\w+)\(', text)))
    stubs = {
        'Resources.kt': 'package org.citra.citra_emu\nobject R { object string {\n' +
            '\n'.join(f'const val {key} = {i}' for i, key in enumerate(resources)) + '\n} }',
        'SettingKeys.kt': 'package org.citra.citra_emu.features.settings\nobject SettingKeys {\n' +
            '\n'.join(f'fun {key}() = "{key}"' for key in keys) + '\n}',
        'Uri.kt': '''package android.net
data class Uri(val path: String) {
    companion object { fun parse(path: String) = Uri(path) }
}
''',
        'Bundle.kt': '''package android.os
class Bundle {
    private val values = HashMap<String, Boolean>()
    fun getBoolean(key: String) = values[key] ?: false
    fun putBoolean(key: String, value: Boolean) { values[key] = value }
}
''',
        'ViewModel.kt': 'package androidx.lifecycle\nopen class ViewModel',
        'NativeLibrary.kt': '''package org.citra.citra_emu
object NativeLibrary {
    var reloads = 0
    fun reloadSettings() { reloads++ }
    fun updateFramebuffer(portrait: Boolean) {}
    fun isPortraitMode() = false
}
''',
        'Context.kt': '''package android.content
import android.net.Uri
import java.io.File
open class Context {
    val contentResolver = ContentResolver()
    fun getString(id: Int, vararg args: Any?) = "resource $id ${args.toList()}"
}
class ContentResolver {
    fun openInputStream(uri: Uri) = File(uri.path).inputStream()
    fun openOutputStream(uri: Uri, mode: String) = File(uri.path).outputStream()
}
''',
        'DocumentFile.kt': '''package androidx.documentfile.provider
import android.net.Uri
import android.content.Context
import java.io.File
class DocumentFile(private val file: File) {
    val uri get() = Uri(file.path)
    fun findFile(name: String): DocumentFile? = File(file, name).takeIf { it.exists() }?.let { DocumentFile(it) }
    fun createDirectory(name: String) = File(file, name).let { it.mkdirs(); DocumentFile(it) }
    fun createFile(type: String, name: String) = File(file, name).let { it.createNewFile(); DocumentFile(it) }
    fun delete() = file.delete()
    companion object { fun fromTreeUri(context: Context, uri: Uri) = DocumentFile(File(uri.path)) }
}
''',
        'Application.kt': '''package org.citra.citra_emu
object CitraApplication {
    val appContext = android.content.Context()
    val documentsTree = TestDocumentsTree()
}
class TestDocumentsTree { fun invalidateConfigCache() {} }
''',
        'Utils.kt': '''package org.citra.citra_emu.utils
object DirectoryInitialization {
    var userDirectory = ""
    fun areCitraDirectoriesReady() = true
    fun start() {}
}
object Log {
    fun error(message: String) { System.err.println(message) }
    fun debug(message: String) {}
    fun info(message: String) {}
}
object FileUtil { fun createFile(path: String, name: String) {} }
object PermissionsHandler { val citraDirectory get() = android.net.Uri(DirectoryInitialization.userDirectory) }
object SystemSaveGame {
    fun load() {}
    fun save() {}
}
object TurboHelper { fun reloadTurbo(enabled: Boolean) {} }
''',
    }
    for name, contents in stubs.items():
        path = build / name
        path.write_text(contents, encoding='utf-8')
        sources.append(path)
    jar = build / 'settings-tests.jar'
    subprocess.run([
        args.java, '-cp', str(args.kotlin_home / 'lib/*'),
        'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler',
        '-kotlin-home', str(args.kotlin_home), '-classpath', str(args.ini4j),
        '-d', str(jar), *map(str, sources),
    ], check=True)
    subprocess.run([
        args.java, '-cp', os.pathsep.join((str(jar), str(args.ini4j),
                                         str(args.kotlin_home / 'lib/kotlin-stdlib.jar'))),
        'SettingsTestKt', str(build / 'user'),
    ], check=True)
    exe = build / ('config-tests.exe' if os.name == 'nt' else 'config-tests')
    ini = repo / 'externals/inih/inih'
    compiler = [str(args.zig), 'c++'] if args.zig else [args.cxx]
    subprocess.run([
        *compiler, '-std=c++17', '-Wall', '-Wextra',
        '-I' + str(repo / 'src/android/app/src/main/jni'), '-I' + str(ini / 'cpp'),
        '-I' + str(ini), str(here / 'config_ini_test.cpp'),
        str(ini / 'cpp/INIReader.cpp'), '-x', 'c++', str(ini / 'ini.c'), '-o', str(exe),
    ], check=True)
    subprocess.run([
        str(exe), str(build / 'user/config/config.ini'),
        str(build / 'user/config/custom/0004000000123400.ini'),
    ], check=True)
    startup_exe = build / ('default-ini-test.exe' if os.name == 'nt' else 'default-ini-test')
    subprocess.run([
        *compiler, '-std=c++20', '-I' + str(headers), '-I' + str(repo / 'src'),
        '-I' + str(repo / 'externals/boost'), str(here / 'default_ini_test.cpp'),
        '-o', str(startup_exe),
    ], check=True)
    subprocess.run([str(startup_exe)], check=True)
