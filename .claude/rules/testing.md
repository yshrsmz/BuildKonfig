## Testing conventions

Applies to test sources under `buildkonfig-gradle-plugin/src/test/**/*.kt`.

### Truth assertions

Use the bare `assertThat` import, not the qualified `Truth.assertThat(...)` form:

```kotlin
import com.google.common.truth.Truth.assertThat
// ...
assertThat(result.output).contains("BUILD SUCCESSFUL")
```

Do **not** import `com.google.common.truth.Truth` and call `Truth.assertThat(...)` — both styles work but mixing them within the same package hurts readability. Every existing test file uses the bare-import form; new tests should match.

### Common scaffolding

For Gradle TestKit-driven tests, extend `BaseGradlePluginTest` and rely on the helpers in `TestUtils.kt` (`gradleRunner()`, `TemporaryFolder.buildKonfigDir()`, `BuildResult.assertBuildSuccessful()`, `buildKonfigFile()`) instead of repeating the inlined `GradleRunner.create().withProjectDir(...).withPluginClasspath()` chain or `File(projectDir.root, "build/buildkonfig").also { it.deleteRecursively() }` pattern.

### Build script header

Use `buildFileHeader(kotlinPluginId, vararg additionalPlugins)` (or `buildFileHeaderKts` for Kotlin DSL fixtures) for the standard `plugins { ... }` + `repositories { google() mavenCentral() }` block. Both helpers always emit `google()` and `mavenCentral()`, which covers AGP-based fixtures without any extra arguments.

**Store the result in a `private val` at class level** (one per plugin combination), then interpolate it into the test fixture. Do not call the helper inline inside the `buildFile.writeText` heredoc and do not hand-roll the equivalent `plugins { ... }` block:

```kotlin
// Good — class-level variable, reused or single-use
class FooTest : BaseGradlePluginTest() {
    private val buildFileHeader = buildFileHeader("kotlin-multiplatform")
    // For multiple variants in one class, name them by intent:
    //   private val kmpBuildFileHeader = buildFileHeader("kotlin-multiplatform")
    //   private val jvmBuildFileHeader = buildFileHeader("org.jetbrains.kotlin.jvm")

    @Test
    fun `xyz`() {
        buildFile.writeText(
            """
            |$buildFileHeader
            |
            |kotlin { jvm() }
            """.trimMargin()
        )
    }
}

// Avoid — inline call inside the heredoc
buildFile.writeText(
    """
    |${buildFileHeader("org.jetbrains.kotlin.js")}
    |...
    """.trimMargin()
)

// Avoid — hand-rolled plugins block
buildFile.writeText(
    """
    |plugins {
    |    id 'org.jetbrains.kotlin.js'
    |    id 'com.codingfeline.buildkonfig'
    |}
    |
    |repositories {
    |   mavenCentral()
    |}
    """.trimMargin()
)
```

The two forms are functionally equivalent, but every test class in the suite uses the class-level-variable form; new tests should match.

The only exception is fixtures that intentionally omit a Kotlin plugin (e.g. tests asserting that the plugin fails when no Kotlin plugin is applied) — those obviously cannot use the helper. Anything else should go through `buildFileHeader` / `buildFileHeaderKts`; if a new plugin combination shows up, extend the helpers rather than hand-roll a fresh inline block.
