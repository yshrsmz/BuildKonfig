package com.codingfeline.buildkonfig.gradle

import com.codingfeline.buildkonfig.FieldSpec
import com.codingfeline.buildkonfig.PlatformConfig
import org.gradle.api.logging.Logger
import org.gradle.internal.impldep.org.eclipse.jgit.annotations.NonNull

import javax.inject.Inject

class PlatformConfigDsl extends PlatformConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private Logger logger

    @Inject
    public PlatformConfigDsl(@NonNull String name,
                             @NonNull Logger logger) {
        super(name)
        this.logger = logger
    }

    public void buildConfigField(
            @NonNull String type,
            @NonNull String name,
            @NonNull String value) {

        FieldSpec alreadyPresent = fieldSpecs[name]

        if (alreadyPresent != null) {
            logger.info("PlatformConfig: buildConfigField '$name' is being replaced: ${alreadyPresent.value} -> $value")
        }
        fieldSpecs.put(name, new FieldSpec(type, name, value))
    }

    PlatformConfig toPlatformConfig() {
        def config = new PlatformConfig(name)
        config.fieldSpecs.putAll(this.fieldSpecs)

        return config
    }
}
