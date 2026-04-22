# Building and Development Commands

This page documents the main build, test, and tooling commands used in this repository.

## Requirements

- JDK 17+ for `authme-core`, `authme-tools`, and `authme-spigot-legacy`
- JDK 21+ for the full multi-module build, including:
  - `authme-spigot-1.21`
  - `authme-paper-common`
  - `authme-paper`
  - `authme-folia`
- Maven 3.8.8+

## Build commands

From the repository root:

```bash
# Build everything available for the current JDK
mvn clean package

# Build without tests
mvn clean package -DskipTests

# Build while skipping long hash tests
mvn clean package -P skipLongHashTests

# Build one deliverable with its dependencies
mvn clean package -pl authme-paper-common -am
mvn clean package -pl authme-paper -am
mvn clean package -pl authme-folia -am
mvn clean package -pl authme-spigot-1.21 -am

# Build without generating Javadoc
mvn clean package -P skipJavadocGeneration
```

On JDK 21+ builds, the full reactor also generates aggregated API docs for all built modules under
`target/site/apidocs` at the repository root.

## Test commands

```bash
# Run tests in authme-core
mvn test -pl authme-core

# Run tests in Paper-derived modules
mvn test -am -pl authme-paper-common,authme-paper,authme-folia

# Run tests in 1.21 version modules
mvn test -am -pl authme-spigot-1.21,authme-paper,authme-folia

# Run the full test suite
mvn test -P skipLongHashTests

# Run one authme-core test class or method
mvn test -Dtest=ClassName -pl authme-core
mvn test -Dtest=ClassName#methodName -pl authme-core

# Generate coverage for authme-core
mvn clean verify -pl authme-core -am
```

## Running development tools

The tooling lives in `authme-tools`, but you can run it directly from the repository root through the Maven reactor.

```bash
# Show the interactive task runner
mvn -q -pl authme-tools -am -P run-tools process-test-classes

# Regenerate repository docs
mvn -q -pl authme-tools -am -P run-tools process-test-classes \
  -Dexec.args=updateDocs

# Regenerate generated command/plugin manifests
mvn -q -pl authme-tools -am -P run-tools process-test-classes \
  "-Dexec.args=generateCommandsYml generatePluginYml"
```

## Generated files

- `docs/commands.md`
- `docs/config.md`
- `docs/hash_algorithms.md`
- `docs/permission_nodes.md`
- `docs/translations.md`
- `authme-core/src/main/resources/commands.yml`
- `authme-core/src/main/resources/plugin.yml`
- `authme-core/src/test/resources/plugin.yml`
- version-module `plugin.yml` files

If you change a generator or template, commit both the source change and the regenerated output.
