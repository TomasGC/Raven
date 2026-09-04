# Design Patterns - Raven

**Last Updated**: 2026-09-04

---

## Patterns In Use

### Module-per-Solver (Strategy, at the build-system level)

Each puzzle solver is a separate Gradle module implementing the `PuzzleModule`
interface (`:core`). `:app` doesn't know or care which concrete solver it's
talking to — it just holds a `List<PuzzleModule>` and displays whatever's in
it. This is the Strategy pattern applied at the module-dependency level
instead of just the class level, so a new solver can't accidentally reach
into `:app` or another solver's internals — the build graph enforces it, not
just convention.

### Dependency Injection (Hilt)

One `@Module @InstallIn(SingletonComponent::class) object AppModule` in
`:app`. Currently two `@Provides` functions: `provideHasOverlayPermission`
(wraps a plain function for testability) and `providePuzzleModules` (the
single hardcoded solver-module registration point). Add a new function
here, not a new `@Module`, unless a genuinely separate installation scope
is needed.

### Sealed Interface for One-Shot UI Events

`HomeUiEvent` (`RequestOverlayPermission` / `StartOverlay`) models a
one-shot side effect the ViewModel wants the Activity to perform, carried
through a nullable `StateFlow<HomeUiEvent?>` that gets reset to `null`
once consumed (`onEventConsumed()`). This avoids the classic "event fires
twice on configuration change" bug that a plain `State` (non-nullable,
never reset) would have.

### Pure State Machine as an Enum

`OverlayUiState.toggled()` — a 2-state machine with no external
dependencies, expressed as an enum with one pure function. Prefer this
shape over a sealed class hierarchy or a boolean flag when the state count
is small (2-3) and transitions don't carry data.

### Pure Gesture Classifier, Android I/O Kept in the Service

`OverlayButtonGestureDetector` takes only raw touch coordinates and a
touch-slop pixel value, and returns a `Tap`/`Dragged`/`Kill` classification
— it never touches `WindowManager` or any Android API. `OverlayService`
owns the actual view repositioning (`updateViewLayout`) and the kill-zone
indicator view, both of which need Robolectric/a real device to test. This
keeps the one genuinely trivial-to-get-wrong piece of logic (tap vs. drag
vs. drag-past-the-kill-line) unit-testable in isolation. Apply this split
any time new gesture-driven code mixes "was this gesture X" logic with
Android view/window operations.

---

## Patterns Deliberately Not Yet In Use

- **Repository / UseCase layers** — Otter's Clean Architecture split
  (`ui` → `domain` → `data`) doesn't exist in Raven yet because there's no
  data layer to abstract: no persistence, no network, no archive-like I/O.
  It will very likely appear once a real solver module needs a BoardVision
  data layer (screenshot capture, template matching) — introduce it then,
  scoped to that module, not preemptively in `:app` or `:core`.
- **Plugin loader / reflection-based module discovery** — `AppModule`'s
  hardcoded `listOf(...)` is intentional for as long as the solver-module
  count stays small (see `contexts/architecture.md`).
- **Dagger multibindings (`@IntoSet`)** — would remove the shared
  `AppModule` edit point per new solver module, but isn't worth the added
  complexity at 1 module. Revisit once a 3rd or 4th solver module lands
  and the shared-edit-point friction is real, not hypothetical.
