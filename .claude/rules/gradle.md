## Gradle conventions

Applies to Gradle plugin sources under `buildkonfig-gradle-plugin/src/main/**/*.kt`.

### Task-output Providers cannot be queried at configuration time

Gradle 7+ rejects `.get()` on a Provider chain that resolves through a `TaskProvider`'s output before the task has run, with:

> Querying the mapped value of flatmap(provider(task '...', class ...)) before task ':...' has completed is not supported.

Don't try to source a "single source of truth" for paths from `task.flatMap { it.outputDirectory }` if any consumer eagerly resolves it (e.g. `provider.map { ... }.get().asFile` at config time). Instead:

- Compute the rooted `Provider<Directory>` once from `project.layout.buildDirectory.dir(...)`.
- Wire it into the task via `task.outputDirectory.set(rootedProvider)`.
- Reuse the same `rootedProvider` for any config-time eager resolution downstream.

Lazy chains (e.g. `kotlinSourceSet.kotlin.srcDir(task.flatMap { it.outputDirectory.dir(name) })`) are fine because Gradle resolves them at task execution.
