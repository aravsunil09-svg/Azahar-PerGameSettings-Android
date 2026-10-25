// Copyright 2026 Azahar Emulator Project
// Licensed under GPLv3 or any later version

#include <cassert>
#include <cmath>
#include <fstream>
#include <iostream>
#include <iterator>
#include "config_ini.h"

int main(int argc, char** argv) {
    const std::string global = "[Renderer]\nresolution_factor=2\nuse_vsync=true\n"
                               "pp_shader_name=global\n[Audio]\nvolume=0.6\n"
                               "[WebService]\nnetwork_token=global-token\n"
                               "[Debugging]\nrenderer_debug=false\n";
    AndroidConfigIni reader(global.c_str(), global.size());
    assert(reader.GetInteger("Renderer", "resolution_factor", 1) == 2);
    reader.LoadOverrides("[Renderer]\nresolution_factor=4\nuse_vsync=false\n"
                         "pp_shader_name=game\n[Audio]\nvolume=0.3\n"
                         "[WebService]\nnetwork_token=game-token\n");
    assert(reader.GetInteger("Renderer", "resolution_factor", 1) == 4);
    assert(!reader.GetBoolean("Renderer", "use_vsync", true));
    assert(reader.Get("Renderer", "pp_shader_name", "default") == "game");
    assert(reader.GetString("Renderer", "pp_shader_name", "default") == "game");
    assert(reader.GetReal("Audio", "volume", 1.0) == 0.3);
    assert(reader.Get("WebService", "network_token", "default") == "global-token");
    assert(reader.GetInteger("Renderer", "frame_limit", 100) == 100);
    reader.LoadOverrides("[Renderer]\nresolution_factor=9\n"
                         "resolution_factor/use_global=true\nuse_vsync=invalid\n"
                         "[Audio]\nvolume=invalid\n");
    assert(reader.GetInteger("Renderer", "resolution_factor", 1) == 2);
    assert(reader.GetBoolean("Renderer", "use_vsync", false));
    assert(reader.GetReal("Audio", "volume", 1.0) == 0.6);
    reader.LoadOverrides("[Renderer]\nresolution_factor=invalid\n");
    assert(reader.GetInteger("Renderer", "resolution_factor", 1) == 2);
    reader.LoadOverrides("[Renderer]\nresolution_factor=9\nbroken-line\n");
    assert(reader.GetInteger("Renderer", "resolution_factor", 1) == 2);
    reader.LoadOverrides("");
    assert(reader.GetInteger("Renderer", "resolution_factor", 1) == 2);
    assert(reader.GetString("Renderer", "pp_shader_name", "default") == "global");
    reader.LoadOverrides("[Debugging]\nrenderer_debug=true\n");
    assert(reader.GetBoolean("Debugging", "renderer_debug", false));
    reader.LoadOverrides("[Debugging]\nrenderer_debug=true\nrenderer_debug/use_global=true\n");
    assert(!reader.GetBoolean("Debugging", "renderer_debug", true));
    reader.LoadOverrides("");
    assert(!reader.GetBoolean("Debugging", "renderer_debug", true));
    if (argc == 3) {
        std::ifstream global_file(argv[1]);
        std::ifstream game_file(argv[2]);
        assert(global_file && game_file);
        const std::string globals((std::istreambuf_iterator<char>(global_file)), {});
        const std::string profile((std::istreambuf_iterator<char>(game_file)), {});
        AndroidConfigIni saved(globals.c_str(), globals.size());
        saved.LoadOverrides(profile);
        assert(saved.GetInteger("Renderer", "resolution_factor", 1) == 4);
        assert(!saved.GetBoolean("Renderer", "use_vsync", true));
        assert(std::abs(saved.GetReal("Audio", "volume", 1.0) - 0.3) < 0.00001);
        assert(saved.GetBoolean("Debugging", "renderer_debug", false));
        assert(saved.GetInteger("Core", "cpu_clock_percentage", 100) == 150);
        std::cout << "Kotlin-written profile loaded correctly by native INI reader\n";
    }
    std::cout << "Native INI inheritance regression checks passed\n";
}
