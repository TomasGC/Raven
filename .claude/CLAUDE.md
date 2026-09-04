# Project Instructions - Raven

**Purpose**: Android puzzle-solver host app instructions
**Last Updated**: 2026-09-04

---

## Project Context

@contexts/kanban.md
@contexts/architecture.md
@contexts/design-patterns.md
@contexts/commands.md
@contexts/conventions.md
@contexts/tests.md

---

## Hard Constraints (Non-Negotiable)

### Testing Requirements

**ALL TESTS MUST PASS** - No exceptions

After any code change:
1. Build: `python scripts/manage.py build --no-install`
2. Test: `python scripts/manage.py test unit`
3. **If any test fails → BLOCK COMMIT**

Alternative commands:
```bash
python scripts/manage.py build               # Build + auto-install on device
python scripts/manage.py build --no-install  # Build only (no device needed)
python scripts/manage.py test                # Everything that currently exists, unfiltered
python scripts/manage.py test unit           # Unit tier only (fast, no device)
python scripts/manage.py test integration-mock   # Integration tier (mocked Android runtime)
python scripts/manage.py test integration-real  # Integration tier, no mocks
python scripts/manage.py test instrumented   # Instrumented tests (requires device)
python scripts/manage.py coverage            # Unit tests with Kover coverage report
```

**Coverage requirement**: ≥ 80% (`:app` debug variant, Kover-enforced)

---

### Version Control Rules

#### Commit Format

**Format**: `#XXX: type: description`

**Examples**:
```
#1: feat: add core contract module and Placeholder module
#2: fix: resolve Klondike deck-reconstruction off-by-one
#2: refactor: extract solver state hashing into its own class
```

**Branch naming**:
- Features: `feature/XXX-description`
- Bugfixes: `bugfix/XXX-description`

---

### Code Quality Standards

**Mandatory rules**:

1. **No hardcoded values** - Use constants or configuration
2. **One class/interface per file** - Single responsibility
3. **Strong typing** - Avoid `Any`, use sealed classes/interfaces for state
4. **DRY principle** - No code duplication
5. **Immutability** - Prefer `val` over `var`, use data classes
6. **Null safety** - Leverage Kotlin's null-safety features
7. **Coroutines patterns** - Use structured concurrency
8. **Module boundaries** - `:core` never depends on `:app` or any game module; game modules depend only on `:core`
9. **No comments except non-obvious WHY** - a hidden constraint, a workaround, a subtlety. Never restate what the code already says.

---

## Operational Guidelines

### Build & Test Workflow

See `contexts/commands.md` for the full command reference.

**After any code change**:
1. Build must succeed
2. All tests in the tiers you touched must pass
3. Coverage threshold met (80%, `:app` only)

---

### Architecture Patterns

See `contexts/architecture.md` and `contexts/design-patterns.md` for full detail. Summary:

- **Multi-module Gradle**, analogous to separate `.csproj`s: `:core` (the `PuzzleModule` contract, nothing else), one module per puzzle solver (`:placeholder` today, `:klondike`/etc. later — each depends only on `:core`), `:app` (the Android host, depends on `:core` and every solver module).
- **MVVM** inside `:app`: Compose UI → `HiltViewModel` (exposes `StateFlow` events) → Service/util layer.
- **Dependency Injection**: Hilt, one `@Provides` registration point (`AppModule.providePuzzleModules()`) per solver module — this is the seam a future plugin loader would replace, not built until it's actually needed.
- **Sealed interfaces** for one-shot UI events (`HomeUiEvent`), plain enums with a `toggled()`-style pure function for simple state machines (`OverlayUiState`).

---

### Tech Stack

| Component | Technology | Version | Purpose |
|-----------|-----------|---------|---------|
| **Language** | Kotlin | 2.0.21 | Modern JVM language with null-safety |
| **Platform** | Android SDK | 30-35 | Android 11 to Android 15 |
| **UI** | Jetpack Compose | BOM 2024.11.00 | Declarative UI framework |
| **Design** | Material Design 3 | (via Compose BOM) | Modern Material Design |
| **DI** | Hilt | 2.52 | Dependency injection |
| **Async** | Coroutines | 1.9.0 | Structured concurrency |
| **Build** | Gradle KTS | AGP 8.7.3 / Gradle 8.9 | Kotlin DSL build scripts |
| **Testing** | JUnit4 + MockK + Robolectric | 4.13.2 / 1.13.12 / 4.14.1 | Unit testing (`:app`) |
| **Logging** | Timber | 5.0.1 | |
| **Quality gates** | Detekt + Kover + Android Lint | — | via `./gradlew check` |

Versions are pinned to match the sibling project Otter (`C:\dev\repos\GitHub\otter`) for toolchain consistency across the user's own Android projects — bump independently of Otter if a real need arises.

---

## Communication Style

### Language

**Code/Documentation/Commits**: English (always)
**Conversation**: can be customized in `.claude/CLAUDE.local.md`

---

### Decision Making

**When proposing solutions**:
1. Present 2-3 alternatives
2. List pros/cons for each
3. State recommendation with reasoning
4. Wait for user choice

**Before major changes**:
1. Analyze current code
2. Propose approach with trade-offs
3. Show impact (files affected, effort estimate)
4. Get approval before coding

---

## Project Structure

### Directory Layout

```
Raven/
├── core/                                  # PuzzleModule contract only, no Android dep
│   └── src/main/kotlin/app/raven/core/
│       └── PuzzleModule.kt
├── placeholder/                           # First (stand-in) solver module
│   └── src/
│       ├── main/kotlin/app/raven/placeholder/
│       └── test/kotlin/app/raven/unit/placeholder/
├── app/                                   # Android host
│   └── src/
│       ├── main/
│       │   ├── java/app/raven/
│       │   │   ├── RavenApplication.kt
│       │   │   ├── MainActivity.kt
│       │   │   ├── di/AppModule.kt        # registers every solver module here
│       │   │   ├── service/               # OverlayService + its pure helpers
│       │   │   ├── ui/{theme,viewmodel,screen}/
│       │   │   └── util/
│       │   └── res/
│       ├── test/java/app/raven/
│       │   ├── unit/                      # plain *Test classes
│       │   ├── integration-mock/          # *IntegrationTest classes
│       │   └── integration-real/          # *RealIntegrationTest classes (none yet)
│       └── androidTest/java/app/raven/    # instrumented tier
├── scripts/                               # Python dev tooling
│   ├── manage.py
│   └── src/{common,android,cli}/
└── .claude/
    ├── CLAUDE.md                          # this file
    ├── contexts/                          # living project docs (this @-included set)
    └── sessions/{specs,plans}/            # point-in-time design specs and SDD plans
```

---

### Key Files

| File | Purpose |
|------|---------|
| `settings.gradle.kts` | Declares the 3 modules |
| `core/src/main/kotlin/app/raven/core/PuzzleModule.kt` | The whole solver-module contract |
| `app/src/main/java/app/raven/di/AppModule.kt` | Single hardcoded solver-module registration point |
| `app/src/main/java/app/raven/service/OverlayService.kt` | Owns the floating overlay button |
| `app/src/main/java/app/raven/ui/viewmodel/HomeViewModel.kt` | Home screen state + permission-vs-start decision |
| `scripts/manage.py` | Entry point for build/test/validate/coverage |

---

## Project Documentation Files

**Core Documentation** (`.claude/` directory):
- `.claude/CLAUDE.md` - This file (project instructions)
- `.claude/contexts/kanban.md` - Task tracking, backlog, session history
- `.claude/contexts/architecture.md` - Architecture diagrams, design decisions
- `.claude/contexts/design-patterns.md` - Patterns in use and why
- `.claude/contexts/commands.md` - Full command reference
- `.claude/contexts/conventions.md` - Coding/commit/test conventions
- `.claude/contexts/tests.md` - Test counts, directory structure, coverage

**Point-in-time Documentation** (`.claude/sessions/` directory, not living docs):
- `.claude/sessions/specs/*.md` - Design specs from brainstorming, one per feature
- `.claude/sessions/plans/*.md` - Implementation plans from writing-plans/SDD, one per feature

**Public Documentation** (committed to git, none yet beyond this):
- `README.md` - not yet written

---

## References

### Available Skills

- `/update-context` - Update kanban.md/architecture.md/etc. after finishing work
- `/project-setup` - Initialize or update `.claude/` structure
- `/analyze-commit` - Pre-commit analysis (security, quality, tests)
- `/skill-setup` - Create or update skills

---

### Android-Specific Conventions

**Package Structure**:
```
app.raven.core            # :core — the PuzzleModule contract
app.raven.<module-name>   # each solver module, e.g. app.raven.placeholder
app.raven.ui.*            # :app UI Layer (Compose, ViewModels)
app.raven.service.*       # :app Android Services
app.raven.di.*            # :app Dependency Injection modules
app.raven.util.*          # :app Utilities
```

**Naming Conventions**:
- Activities: `*Activity.kt`
- ViewModels: `*ViewModel.kt`
- Composables: PascalCase functions
- Sealed event/state interfaces: `*UiEvent.kt` / `*UiState.kt` (or a plain enum for a simple 2-3-state machine)
- Solver module contract implementations: `<Name>Module.kt` (e.g. `PlaceholderModule.kt`)

**Test Structure**: see `contexts/conventions.md` and `contexts/tests.md`.
