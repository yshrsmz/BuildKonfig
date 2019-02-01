#!/usr/bin/env sh

./gradlew clean
./gradlew build
./gradlew :buildkonfig-compiler:uploadArchives
./gradlew :buildkonfig-gradle-plugin:uploadArchives
