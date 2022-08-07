package com.codingfeline.buildkonfig.gradle.yaml


data class YamlConfig(
    val flavor: String,
    val configs: List<TargetConfig>
) {
    companion object {
        fun from(map: Map<String, Any?>): YamlConfig {
            return YamlConfig(
                flavor = map["flavor"] as? String ?: "default",
                configs = (map["configs"] as? List<Map<String, Any?>>).orEmpty()
                    .mapNotNull { TargetConfig.from(it) }
            )
        }
    }

    data class TargetConfig(
        val name: String,
        val fields: List<TargetConfigField>
    ) {
        companion object {
            fun from(map: Map<String, Any?>): TargetConfig? {
                return TargetConfig(
                    name = map["name"] as? String ?: "default",
                    fields = (map["fields"] as? List<Map<String, Any?>>).orEmpty()
                        .mapNotNull { TargetConfigField.from(it) }
                )
            }
        }
    }

    data class TargetConfigField(
        val name: String,
        val type: String,
        val value: String?,
        val const: Boolean,
        val nullable: Boolean
    ) {
        companion object {
            fun from(map: Map<String, Any?>): TargetConfigField? {
                val name = map["name"] as? String ?: return null
                val type = map["type"] as? String ?: return null
                return TargetConfigField(
                    name = name,
                    type = type,
                    value = map["value"]?.toString(),
                    const = map["const"] as? Boolean ?: false,
                    nullable = map["nullable"] as? Boolean ?: false,
                )
            }
        }
    }
}