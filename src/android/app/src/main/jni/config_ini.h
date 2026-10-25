// Copyright 2026 Azahar Emulator Project
// Licensed under GPLv3 or any later version
// Refer to the LICENSE.txt file included.

#pragma once

#include <memory>
#include <string>
#include <INIReader.h>

// Sparse game profiles inherit every missing value from config.ini. Keep the
// two readers separate so malformed overrides also fall back to global values.
class AndroidConfigIni : public INIReader {
public:
    using INIReader::INIReader;

    void LoadOverrides(const std::string& contents) {
        overrides = std::make_unique<INIReader>(contents.c_str(), contents.size());
        if (overrides->ParseError() != 0) {
            overrides.reset();
        }
    }

    std::string Get(const std::string& section, const std::string& key,
                    const std::string& fallback) const {
        const auto global = INIReader::Get(section, key, fallback);
        return UsesOverride(section, key) ? overrides->Get(section, key, global) : global;
    }

    std::string GetString(const std::string& section, const std::string& key,
                          const std::string& fallback) const {
        const auto global = INIReader::GetString(section, key, fallback);
        return UsesOverride(section, key) ? overrides->GetString(section, key, global) : global;
    }

    long GetInteger(const std::string& section, const std::string& key, long fallback) const {
        const auto global = INIReader::GetInteger(section, key, fallback);
        return UsesOverride(section, key) ? overrides->GetInteger(section, key, global) : global;
    }

    double GetReal(const std::string& section, const std::string& key, double fallback) const {
        const auto global = INIReader::GetReal(section, key, fallback);
        return UsesOverride(section, key) ? overrides->GetReal(section, key, global) : global;
    }

    bool GetBoolean(const std::string& section, const std::string& key, bool fallback) const {
        const auto global = INIReader::GetBoolean(section, key, fallback);
        return UsesOverride(section, key) ? overrides->GetBoolean(section, key, global) : global;
    }

private:
    bool UsesOverride(const std::string& section, const std::string& key) const {
        // App preferences, credentials and controller mappings stay global.
        const bool supported = section == "Core" || section == "System" || section == "Renderer" ||
                               section == "Layout" || section == "Utility" || section == "Audio" ||
                               section == "Debugging";
        return supported && overrides && overrides->HasValue(section, key) &&
               !overrides->GetBoolean(section, key + "/use_global", false);
    }

    std::unique_ptr<INIReader> overrides;
};
