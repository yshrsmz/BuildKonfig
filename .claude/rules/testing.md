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
