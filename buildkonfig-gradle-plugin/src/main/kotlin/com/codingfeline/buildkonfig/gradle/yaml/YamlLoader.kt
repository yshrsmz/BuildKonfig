package com.codingfeline.buildkonfig.gradle.yaml

import org.snakeyaml.engine.v2.api.Load
import org.snakeyaml.engine.v2.api.LoadSettings

class YamlLoader {
    private val settings by lazy {
        LoadSettings.builder()
            .build()
    }

    fun load(yaml: String): List<YamlConfig> {
        val loader = Load(settings)
        val result = loader.loadAllFromString(yaml)
            .filterIsInstance<Map<String, Any>>()
            .map { YamlConfig.from(it) }

        return result
    }
}