Change Log
===


Version 0.4.1 *(2019-12-12)*
---

- Kotlin 1.3.61


Version 0.4.0 *(2019-12-12)*
---

- Simple `defaultConfigs` only configuration now creates object in common sourceSet.


Version 0.3.4 *(2019-08-23)*
---

- Kotlin 1.3.50
- Android Gradle Plugin 3.5.0
- Gradle 5.6

Version 0.3.3 *(2019-07-12)*
---

- Kotlin 1.3.41


Version 0.3.2 *(2019-07-10)*
---

- Kotlin 1.3.31(though this shouldn't be a problem as it's just a plugin dependency)
- Fix issue with task dependency in some cases


Version 0.3.1 *(2019-02-25)*
---

- Delete old `BuildKonfig` objects before executing the task. ([#12](https://github.com/yshrsmz/BuildKonfig/issues/12))


Version 0.3.0 *(2019-02-09)*
---

- Support build flavor ([#8](https://github.com/yshrsmz/BuildKonfig/issues/8)). See readme for the detail.


Version 0.2.1 *(2019-02-05)*
---

- Generated codes are not properly recognized by IDE sometimes ([#7](https://github.com/yshrsmz/BuildKonfig/issues/7))


Version 0.2.0 *(2019-02-03)*
---

- BuildKonfig task is renamed to `generateBuildKonfig`
- BuildKonfig object is now internal [#6](https://github.com/yshrsmz/BuildKonfig/issues/6))
- Escape `$` in String value ([#5](https://github.com/yshrsmz/BuildKonfig/issues/5))
- `${target}Main` is not properly handled


Version 0.1.0 *(2019-02-01)*
---

- First public release
