package com.lopata.launcher

data class Server(val name: String, val ip: String)
data class Profile(val id: String, val name: String, val nick: String, val preset: String)

enum class Preset(val title: String, val desc: String, val values: Map<String, String>) {
    BALANCED(
        "Збалансований", "Дальність 8, 60 FPS, швидка графіка, менше частинок",
        mapOf(
            "renderDistance" to "8", "simulationDistance" to "6", "maxFps" to "60",
            "graphicsMode" to "0", "particles" to "1", "entityShadows" to "false",
            "biomeBlendRadius" to "1", "mipmapLevels" to "2", "renderClouds" to "\"fast\""
        )
    ),
    PERFORMANCE(
        "Максимум FPS", "Дальність 5, без хмар і тіней, мінімум частинок",
        mapOf(
            "renderDistance" to "5", "simulationDistance" to "5", "maxFps" to "60",
            "graphicsMode" to "0", "particles" to "2", "entityShadows" to "false",
            "ao" to "false", "biomeBlendRadius" to "0", "mipmapLevels" to "0",
            "renderClouds" to "\"false\"", "entityDistanceScaling" to "0.5"
        )
    )
}
