# Tests - Raven

**Last Updated**: 2026-09-04

---

## Counts

| Category | Tests | Runner | Description |
|----------|-------|--------|--------------|
| Unit (`:app`) | 4 | JUnit + Robolectric (1 file) | Gesture detector, UI state, overlay permission, HomeViewModel |
| Unit (`:placeholder`) | 1 | JUnit | PlaceholderModule id/displayName |
| Unit (`:core`) | 0 | — | Pure interface, nothing to unit test |
| Integration-mock | 6 | JUnit + Robolectric | `OverlayServiceIntegrationTest`: start/stop branches against a shadow `WindowManager`, plus a real down/move/up drag dispatched to the overlay view asserting the kill-zone drag stops the service and calls `ActivityManager.AppTask.finishAndRemoveTask()` |
| Integration-real | 0 | — | No `*RealIntegrationTest` classes yet — see note below |
| Instrumented | 6 | JUnit4 + Compose/UiAutomator, real device | `MainActivityTest` (2): home screen + permission-not-granted flow. `OverlayButtonInstrumentedTest` (4): real on-screen visibility, real tap toggling the icon, real drag-to-bottom-zone kill (also asserts Raven's own task leaves `ActivityManager.appTasks`), real short drag moving the overlay without killing it |
| **Total** | **17** | | |

Coverage (`:app` debug variant, Kover): gate at 80%, currently 95.3%
(`python scripts/manage.py coverage`). `OverlayService`/`OverlayWindowController`/
`MainActivity`/`RavenApplication`/`*` are excluded from the Kover report entirely
(Android-framework code verified at the integration-mock/instrumented tiers
instead — see `app/build.gradle.kts`'s `koverReport` block).

**Integration-real status**: investigated and deliberately left empty. `:app`/`:core`/`:placeholder`
have no file/network/archive-like domain yet — every piece of `:app` code either touches the
Android framework (Settings, WindowManager, Service; needs Robolectric or the instrumented tier)
or is plain zero-dependency data (`PuzzleModule`/`PlaceholderModule`; already fully covered as
plain unit tests). Wrapping either in a `*RealIntegrationTest` today would just re-test something
already covered, with no real external resource involved — revisit once a solver module (e.g.
Klondike) introduces something genuinely file/resource-backed at the JVM level. CI (Condor's
`kotlin-integration-real.yml`) now checks for any `*RealIntegrationTest.kt` file before running
and skips gracefully if none exist, instead of failing outright.

---

## Directory Structure

```
app/src/test/java/app/raven/
├── unit/
│   ├── service/
│   │   ├── OverlayButtonGestureDetectorTest.kt
│   │   └── OverlayUiStateTest.kt
│   ├── ui/viewmodel/
│   │   └── HomeViewModelTest.kt
│   └── util/
│       └── OverlayPermissionTest.kt
├── integration-mock/
│   └── service/
│       └── OverlayServiceIntegrationTest.kt
└── integration-real/       # empty — nothing genuinely fits yet, see note above

app/src/androidTest/java/app/raven/
├── HiltTestRunner.kt
├── MainActivityTest.kt
└── OverlayButtonInstrumentedTest.kt

placeholder/src/test/kotlin/app/raven/
└── unit/placeholder/
    └── PlaceholderModuleTest.kt
```

---

## Run Commands

```bash
# Everything that currently exists (default, always succeeds if any tests exist)
python scripts/manage.py test

# Unit tier only
python scripts/manage.py test unit

# Integration-mock tier (Robolectric-shadowed Android runtime)
python scripts/manage.py test integration-mock

# With Kover coverage report
python scripts/manage.py coverage
# Report: app/build/reports/kover/html/index.html (after `./gradlew koverHtmlReportDebug`)

# Gradle direct
./gradlew test testDebugUnitTest                    # everything
./gradlew testDebugUnitTest -DtestType=unit
./gradlew testDebugUnitTest -DtestType=integration-mock
./gradlew connectedDebugAndroidTest                 # instrumented, needs a device
./gradlew koverXmlReportDebug
```

## Coverage Target

≥ 80% enforced via Kover on `:app`'s debug variant
(`app/build.gradle.kts`'s `koverReport { androidReports("debug") { verify { rule { minBound(80) } } } }`).
`:core` and `:placeholder` are plain JVM modules with no Kover plugin applied yet — add it if/when their size justifies enforcing a gate there too.
