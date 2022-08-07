package com.codingfeline.buildkonfig.gradle.yaml

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class YamlLoaderTest {
    @Test
    fun test() {
        val yaml = """
            ---
            flavor: default
            configs:
              - name: default
                fields:
                - name: testKey
                  type: string
                  value: true
                  const: false
                  nullable: false
                - name: testKey2
                  type: string
                  value: null
                  const: true
                  nullable: true
              - name: android
                fields:
              - name: ios
                fields:

            ---
            flavor: dev
        """.trimIndent()

        val config = YamlLoader().load(yaml)

        assertThat(config).hasSize(2)

        val config1 = config[0]
        assertThat(config1.flavor).isEqualTo("default")
        assertThat(config1.configs).hasSize(3)

        val target1 = config1.configs[0]
        assertThat(target1.name).isEqualTo("default")
        assertThat(target1.fields).containsExactly(
            YamlConfig.TargetConfigField(
                name = "testKey",
                type = "string",
                value = "true",
                const = false,
                nullable = false
            ),
            YamlConfig.TargetConfigField(
                name = "testKey2",
                type = "string",
                value = null,
                const = true,
                nullable = true
            ),
        )
    }
}