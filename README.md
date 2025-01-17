# Gradle Java ABI extractor library

[![Build Gradle project](https://github.com/gradle/java-abi-extractor/actions/workflows/build.yaml/badge.svg)](https://github.com/gradle/java-abi-extractor/actions/workflows/build.yaml)

A library to extract ABI stubs from Java `.class` files.

## Building

The project uses [Gradle](https://gradle.org/) to build the library.

### Prerequisites

- [JDK 17](https://adoptopenjdk.net/)

### Running the build

```shell
./gradlew build
```

### Testing

To run tests with the library built locally:

```shell
./gradlew test
```

### Releasing

Add an annotated tag, such as:

```shell
git tag -a 0.1.0 -m "0.1.0"
```

Then run:

```shell
./gradlew publish
```

Make sure to update GitHub's release page.
