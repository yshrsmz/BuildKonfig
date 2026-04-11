# AGENTS.md

This file documents project conventions for contributors and AI coding assistants working with this repository.

## Project Overview

BuildKonfig is a Gradle plugin for Kotlin Multiplatform (KMP) projects that generates `BuildKonfig` objects containing compile-time configuration values across all KMP targets. It uses KotlinPoet for code generation and Kotlin's expect/actual mechanism for target-specific values.

## Build Commands

```bash
# Build everything
./gradlew clean build

# Run all tests
./gradlew test

# Run a single test class
./gradlew :buildkonfig-gradle-plugin:test --tests "com.codingfeline.buildkonfig.gradle.BuildKonfigPluginTest"

# Run a single test method
./gradlew :buildkonfig-gradle-plugin:test --tests "com.codingfeline.buildkonfig.gradle.BuildKonfigPluginTest.testSimple"

# Publish to local Maven for testing with sample projects
./gradlew publishAllPublicationsToTestMavenRepository -PRELEASE_SIGNING_ENABLED=false

# Test with sample projects (after local publish)
./gradlew -p sample generateBuildKonfig
./gradlew -p sample-kts generateBuildKonfig
```

## Module Structure

- **buildkonfig-compiler** — Code generation using KotlinPoet. Defines `FieldSpec` (field types: String, Int, Long, Float, Boolean), `TargetConfig`, `BuildKonfigData`, and generators that produce Kotlin source files.
- **buildkonfig-gradle-plugin** — Gradle plugin implementation. Contains `BuildKonfigPlugin` (entry point), `BuildKonfigExtension` (DSL), `BuildKonfigTask` (generation task), and KMP source set mapping logic (`SourceRoots`).
- **sample** / **sample-kts** — Example projects using Groovy DSL and Kotlin DSL respectively.

## Architecture

### Plugin Execution Flow

1. `BuildKonfigPlugin.apply()` detects the KMP plugin and registers the `buildkonfig{}` extension
2. In `afterEvaluate`, configs are merged: Flavored TargetConfig > TargetConfig > Flavored DefaultConfig > DefaultConfig
3. `BuildKonfigTask` runs `BuildKonfigEnvironment.generateConfigs()` which decides the generation strategy:
   - **No target-specific configs** → single concrete object in commonMain
   - **With target configs** → expect object in commonMain + actual objects per target
4. `SourceRoots.sources()` maps KMP targets/compilations to source sets, including HMPP intermediate source sets

### Key Design Points

- Flavors are selected via Gradle property `buildkonfig.flavor` (in gradle.properties or `-P` flag), not per-platform
- Fields support `nullable` and `const` modifiers
- `@JsExport` is auto-added when `exposeObjectWithName` is set and a JS target exists
- HMPP limitation: cannot define targetConfigs for both parent and child intermediate source sets

## Testing

Tests are in `buildkonfig-gradle-plugin/src/test/kotlin/com/codingfeline/buildkonfig/gradle/`. They use JUnit 4 + Google Truth, and exercise the plugin via Gradle TestKit by building temporary projects with various DSL configurations. Key test classes cover: basic functionality, flavors, Kotlin DSL, HMPP, and const fields.

## Code Style

Kotlin official style (`kotlin.code.style=official` in gradle.properties).
