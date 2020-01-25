RELEASING
===

1. Change the version in `gradle.properties` to a non-SNAPSHOT version
2. Update `CHANGELOG.md`
3. Update `README.md` with the new version
4. `git commit -am "Prepare for release vX.Y.Z."` (where X.Y.Z is the new version)
5. `git tag -a vX.Y.Z -m "Version X.Y.Z"` (where X.Y.Z is the new version)
6. Change the version in `gradle.properties` to a new SNAPSHOT version
7. `git commit -am "Prepare for next development iteration"`
8. `git push && git push --tags`
9. Wait until the `Publish a release` action completes, then visit [bintrary.com](https://bintray.com/yshrsmz/maven) and promote the artifact.




