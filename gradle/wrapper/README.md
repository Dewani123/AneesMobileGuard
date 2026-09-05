The Gradle wrapper JAR is intentionally not required by the GitHub Actions build.
CI installs Gradle 8.7 with gradle/actions/setup-gradle. Android Studio can use
its configured Gradle distribution. This avoids a broken project caused by a
missing binary wrapper JAR in the source archive.
