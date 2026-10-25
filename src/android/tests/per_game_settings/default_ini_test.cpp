// Copyright 2026 Azahar Emulator Project
// Licensed under GPLv3 or any later version

#include <algorithm>
#include <array>
#include <iostream>
#include <string>
#include <string_view>
#include "common/setting_keys.h"
#include "android/app/src/main/jni/default_ini.h"

// Config::Reload aborts the Android process if any native setting is absent
// from both the default INI and the explicit omission list. Run that gate on
// the host as well, before handing a test APK to a device user.
int main() {
    bool valid = true;
    for (const auto key : Settings::Keys::keys_array) {
        const auto& omitted = DefaultINI::android_config_omitted_keys;
        if (std::find(omitted.begin(), omitted.end(), std::string_view(key)) == omitted.end() &&
            std::string_view(DefaultINI::android_config_default_file_content)
                    .find(std::string(key) + " =") == std::string_view::npos) {
            std::cerr << "Missing Android config declaration: " << key << '\n';
            valid = false;
        }
    }
    if (!valid) return 1;
    std::cout << "Android native startup configuration validation passed\n";
}
