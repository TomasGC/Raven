# Commands - Raven Build & Test

Build and test commands for the Raven Android project.

---

## Android Build & Test (manage.py — recommended)

All operations go through `scripts/manage.py` from the repo root.

```bash
# Build debug APK (auto-increments version, auto-installs on device if connected)
python scripts/manage.py build

# Build without installing on device
python scripts/manage.py build --no-install

# Run everything that currently exists, unfiltered (default — safe even
# while integration/integration-real tiers are still empty)
python scripts/manage.py test

# Run unit tests only (JVM, fast, no device) — :core, :placeholder, and
# :app's unit tier
python scripts/manage.py test unit

# Run the integration-mock tier (real logic, mocked Android runtime) — *IntegrationTest classes.
python scripts/manage.py test integration-mock

# Run the integration-real tier (no mocks at all) — *RealIntegrationTest classes.
# Same "no tests found until one exists" caveat applies.
python scripts/manage.py test integration-real

# Run instrumented tests only (requires a connected device or running emulator)
python scripts/manage.py test instrumented

# Run unit tests with Kover coverage report + 80% threshold check
python scripts/manage.py coverage
```

## Validation (manage.py)

```bash
# Branch name, commit messages, no-TODO, large-file checks
python scripts/manage.py validate
```

## Direct Gradle Commands (reference only — prefer manage.py above)

```bash
./gradlew assembleDebug
./gradlew test testDebugUnitTest                    # everything, unfiltered
./gradlew testDebugUnitTest -DtestType=unit
./gradlew testDebugUnitTest -DtestType=integration-mock
./gradlew testDebugUnitTest -DtestType=integration-real
./gradlew connectedDebugAndroidTest                 # instrumented, needs a device
./gradlew koverXmlReportDebug
./gradlew check                                      # detekt + lint + kover verify + tests
```
