RELEASING
===

Releases are driven by [release-please](https://github.com/googleapis/release-please).
Versions are never bumped and tags are never pushed by hand.

## How a release happens

1. Merge work into `master`. PR titles follow [Conventional Commits](https://www.conventionalcommits.org/)
   — the `pr-title-validation` workflow enforces this, and since pull requests are
   squash-merged, the PR title becomes the commit message release-please reads.
2. Every push to `master` runs the `release-please` workflow, which opens or updates a
   release PR titled `chore(master): release X.Y.Z`. That PR carries the version bump in
   `gradle.properties` and the generated `CHANGELOG.md` entry.
3. Merging the release PR creates the `vX.Y.Z` tag and the GitHub Release.
4. The tag triggers the `Publish a release` workflow, which runs
   `publishAndReleaseToMavenCentral` followed by `publishPlugins`. Nothing has to be
   promoted manually afterwards.

## Version numbers

The project is pre-1.0, configured with `bump-minor-pre-major`:

| Commit type | Bump |
| --- | --- |
| `fix:`, `perf:`, and other patch-level types | patch (`0.23.0` → `0.23.1`) |
| `feat:` | minor (`0.22.1` → `0.23.0`) |
| breaking change | minor, not major |

`README.md` needs no version edit: the install snippets use a `latest_version`
placeholder rather than a literal version.

## Before merging the release PR

- `Run Test Cases` passes on it.
- The changelog in the PR body lists what you expect to ship.

Do not commit anything to the release branch by hand. release-please regenerates it on
every push to `master`, discarding whatever else is there.

## After merging

- Watch the `Publish a release` workflow through to completion.
- Confirm the version shows up on
  [Maven Central](https://central.sonatype.com/artifact/com.codingfeline.buildkonfig/buildkonfig-gradle-plugin)
  and the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/com.codingfeline.buildkonfig).
