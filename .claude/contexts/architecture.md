# Architecture - Raven

**Last Updated**: 2026-09-04

---

## Module Graph

```
        ┌────────────┐
        │   :core    │   PuzzleModule contract only. No Android dependency.
        └─────┬──────┘
              │  depends on
      ┌───────┴────────┐
      │                │
┌─────┴──────┐   ┌─────┴──────┐
│:placeholder│   │  (future   │   Each solver is its own Gradle module,
│            │   │  solvers)  │   analogous to a separate .csproj. Depends
└─────┬──────┘   └─────┬──────┘   only on :core — never on :app.
      │                │
      └───────┬────────┘
              │  depends on
        ┌─────┴──────┐
        │    :app    │   Android host. Depends on :core and every solver
        │            │   module. Never the reverse — that would be circular,
        └────────────┘   since :app is what lists and starts each module.
```

This shape exists specifically to avoid a circular dependency: if the
`PuzzleModule` contract lived inside `:app`, a solver module would need to
depend on `:app` for the interface while `:app` also depends on the solver
module to list it. Putting the contract in its own minimal `:core` module
breaks that cycle.

No plugin-loader/reflection machinery exists yet, and shouldn't until there
are enough real solver modules that hardcoding each one in
`AppModule.providePuzzleModules()` genuinely becomes painful. Adding a
solver module today means: a new Gradle module depending on `:core`, one
class implementing `PuzzleModule`, one line added to that list.

---

## `:app` Internal Layout

```
MainActivity ──setContent──> RavenTheme { HomeScreen }
     │                              │
     │ by viewModels()              │ onModuleSelected
     ▼                              ▼
HomeViewModel <───────────── AppModule.providePuzzleModules()
     │
     │ HomeUiEvent.{RequestOverlayPermission, StartOverlay}
     ▼
MainActivity's LaunchedEffect
     ├─ RequestOverlayPermission → overlayPermissionSettingsIntent()
     └─ StartOverlay             → OverlayService.newIntent()
                                          │
                                          ▼
                                  OverlayService
                                   ├─ OverlayButtonGestureDetector (tap vs 5s long-press)
                                   ├─ OverlayUiState (PAUSED ↔ RUNNING)
                                   └─ WindowManager overlay ImageView
```

`OverlayButtonGestureDetector` and `OverlayUiState` are extracted as pure,
injectable-clock Kotlin classes specifically so the Service's only real
logic is unit-testable without Robolectric or a device. The Service itself
is a thin composition of them plus Android framework calls
(`WindowManager`, `NotificationManager`) that are verified manually on a
device, not via automated tests — this is a deliberate, documented
trade-off, not a coverage gap someone forgot about.

---

## Known Interop Constraint: Dagger + Kotlin Covariant Generics

Any `@Inject`ed or constructor-injected `List<PuzzleModule>` needs
`@JvmSuppressWildcards` (with a comment explaining why — see
`HomeViewModel.kt` and `OverlayService.kt`). Kotlin's declaration-site
covariance emits a wildcarded JVM signature (`List<? extends PuzzleModule>`)
at consumption sites, which doesn't match `AppModule`'s plain
`List<PuzzleModule>` producer signature in Dagger's binding-key lookup,
causing `Dagger/MissingBinding` if omitted. This will hit again the next
time a new class needs the full module list injected — apply the same fix,
don't re-debug it from scratch.

If the number of solver modules grows enough that editing `AppModule` for
every new one becomes real friction, Dagger multibindings (`@IntoSet`) would
let each module contribute itself without a shared list edit point — not
worth the complexity yet at 1 module.

---

## Test Architecture

See `contexts/tests.md` for counts and the full directory layout, and
`contexts/conventions.md` for the naming rule. Summary: tier is determined
by a Gradle test-name-suffix filter (`app/build.gradle.kts`,
`-DtestType=...`), not by package or directory — directories are for human
navigation only.

---

## Toolchain Parity with Otter

Gradle/Kotlin/Compose/Hilt versions, package convention (`app.<name>`, not
`com.tomasgc.*`), commit format (`#XXX: type: description`), branch naming,
the Python `scripts/` dev-tooling shape, and this `.claude/` structure all
deliberately mirror the sibling project Otter
(`C:\dev\repos\GitHub\otter`) for consistency across the user's own Android
projects. Where Otter's own conventions had drifted or grown
over-engineered for Raven's current size (test-suffix naming consistency,
the full unit/integration/integration-real/instrumented Gradle
`-DtestType` filtering apparatus, a 628-test Python test suite), Raven
adopts the *pattern* at a scope appropriate to its actual size rather than
copying the drift or the scale wholesale.
