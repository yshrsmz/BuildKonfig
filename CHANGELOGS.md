# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.13.2] - 2022-08-07

### Added

- const value support ([#42](https://github.com/yshrsmz/BuildKonfig/issues/42))

### Changed

- Android Gradle Plugin 7.2.2
- Unified API. `buildConfigField` can now configure if it's nullable or const.
- Deprecated some methods. use `buildConfigField` with optional parameters.

## [0.12.0] - 2022-05-16

### Changed

- Kotlin 1.6.21
- Android Gradle Plugin 7.2.0
- Gradle Wrapper 7.4.2

### Fixed

- Gradle configuration cache compatibility ([#66](https://github.com/yshrsmz/BuildKonfig/issues/66))

## [0.11.0] - 2021-10-19

### Added

- Support intermediate SourceSets(a.k.a. HMPP) ([#38](https://github.com/yshrsmz/BuildKonfig/issues/38))

### Fixed

- Java version compatibility ([#60](https://github.com/yshrsmz/BuildKonfig/issues/60))

## [0.10.2] - 2021-10-01

### Fixed

- Fix `Duplicate content roots detected` warning in IDE ([#56](https://github.com/yshrsmz/BuildKonfig/issues/56))

## [0.10.1] - 2021-09-24

### Changed

- Improved dependent compile task detection
- Kotlin 1.5.30
- Android Gradle Plugin 7.0.2

## [0.9.0] - 2021-08-05

### Changed

- Kotlin 1.5.21
- Android Gradle Plugin 4.2.2
- Gradle Wrapper 7.1.1

## [0.8.0] - 2021-06-11

### Changed

- Kotlin 1.5.10
- Android Gradle Plugin 4.2.1
- Gradle Wrapper 6.9

## [0.7.1] - 2021-06-09

### Changed

- Better Gradle Kotlin DSL support ([#13](https://github.com/yshrsmz/BuildKonfig/issues/13)
  , [#41](https://github.com/yshrsmz/BuildKonfig/issues/41))

## [0.7.0] - 2020-08-24

### Changed

- Kotlin 1.4.0 ([#36](https://github.com/yshrsmz/BuildKonfig/issues/36))
- Gradle wrapper 6.6

### Added

- New `exposeObjectWithName` to support exposing the generated objects(in other words, an option make it public)
  . ([#31](https://github.com/yshrsmz/BuildKonfig/issues/31), [#35](https://github.com/yshrsmz/BuildKonfig/issues/35))

## [0.6.0] - 2020-08-05

### Changed

- Kotlin 1.3.72
- Android Gradle Plugin 4.0.1
- Gradle wrapper 6.5.1

### Added

- New `objectName` to support changing the name of the generated objects.

## [0.5.1] - 2020-03-31

### Changed

- Kotlin 1.3.71
- Android Gradle Plugin 3.6.1

## [0.5.0] - 2020-01-25

### Added

- New `buildConfigNullableField` to support nullable value by [@MikolajKakol](https://github.com/MikolajKakol)
  . ([#21](https://github.com/yshrsmz/BuildKonfig/pull/21))

## [0.4.1] -2019-12-12

### Changed

- Kotlin 1.3.61

## [0.4.0] - 2019-12-12

### Changed

- Simple `defaultConfigs` only configuration now creates object in common sourceSet.

## [0.3.4] - 2019-08-23

### Changed

- Kotlin 1.3.50
- Android Gradle Plugin 3.5.0
- Gradle 5.6

## [0.3.3] - 2019-07-12

### Changed

- Kotlin 1.3.41

## [0.3.2] - 2019-07-10

### Changed

- Kotlin 1.3.31(though this shouldn't be a problem as it's just a plugin dependency)

### Fixed

- Issue with task dependency in some cases

## [0.3.1] - 2019-02-25

### Fixed

- Delete old `BuildKonfig` objects before executing the task. ([#12](https://github.com/yshrsmz/BuildKonfig/issues/12))

## [0.3.0] - 2019-02-09

### Added

- Support build flavor ([#8](https://github.com/yshrsmz/BuildKonfig/issues/8)). See readme for the detail.

## [0.2.1] - 2019-02-05

### Fixed

- Generated codes are not properly recognized by IDE sometimes ([#7](https://github.com/yshrsmz/BuildKonfig/issues/7))

## [0.2.0] - 2019-02-03

### Changed

- BuildKonfig task is renamed to `generateBuildKonfig`
- BuildKonfig object is now internal [#6](https://github.com/yshrsmz/BuildKonfig/issues/6))
- Escape `$` in String value ([#5](https://github.com/yshrsmz/BuildKonfig/issues/5))
- `${target}Main` is not properly handled

## [0.1.0] - 2019-02-01

- First public release
