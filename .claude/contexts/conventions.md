# Conventions - Raven

Coding and commit conventions for the Raven Android project.

---

## Commit Format

**Format**: `#XXX: type: description`

**Types**: feat, fix, refactor, test, docs, chore, style, perf

**Examples**:
```
#1: feat: add core contract module and Placeholder module
#2: fix: resolve off-by-one in Klondike deck reconstruction
#2: test: add solver state-hashing boundary tests
```

**Rules**:
- Always prefix with the GitHub issue number
- Description: WHAT/WHY, not HOW/WHO
- No stats (+XX lines), no implementation details, no emoji, no AI/assistant references

---

## Branch Naming

- Features: `feature/XXX-description`
- Bugfixes: `bugfix/XXX-description`

Enforced by `python scripts/manage.py validate`.

---

## Kotlin/Android Conventions

### Package Structure

```
app.raven.core            # :core — the PuzzleModule contract, nothing else
app.raven.<module-name>   # each solver module (e.g. app.raven.placeholder)
app.raven.ui.*            # :app UI Layer (Compose, ViewModels)
app.raven.service.*       # :app Android Services + their pure helpers
app.raven.di.*            # :app Dependency Injection modules
app.raven.util.*          # :app Utilities
```

### Naming

- Activities: `*Activity.kt`
- ViewModels: `*ViewModel.kt`
- Composables: PascalCase functions
- Sealed event interfaces: `*UiEvent.kt`; simple state machines: a plain enum
- Solver module implementations: `<Name>Module.kt`

### Test Structure and Naming (tier is determined by class-name suffix, not directory)

```
src/test/java/app/raven/
├── unit/                  # plain *Test classes — package UNCHANGED from production
│                          # (e.g. a test for app.raven.service.Foo still declares
│                          # `package app.raven.service`, just lives under unit/)
├── integration-mock/      # class name MUST end in *IntegrationTest
└── integration-real/      # class name MUST end in *RealIntegrationTest

src/androidTest/java/app/raven/   # instrumented tier (its own Gradle source set already)
```

The `-DtestType` Gradle filter in `app/build.gradle.kts` matches on class
name suffix (`excludeTestsMatching("*IntegrationTest*")` etc.), **not**
package or directory — directory placement is for human navigation only.
Keeping this rule consistent from the start matters: Otter's own equivalent
convention drifted (some older files there are named plain `*Test.kt` when
they're actually integration-flavored), which silently breaks its own
tier filters for those files. Don't repeat that here — name new test
classes correctly for their tier from the moment you write them.

### Code Quality

See `.claude/CLAUDE.md`'s Code Quality Standards section — same list
applies here (no hardcoded values, one class per file, strong typing, DRY,
immutability, null safety, structured concurrency, module-boundary
discipline, no comments except non-obvious WHY).

### Dagger + Kotlin Generics

Any injected `List<PuzzleModule>` needs `@JvmSuppressWildcards` plus a
one-line WHY comment. See `contexts/architecture.md`'s "Known Interop
Constraint" section — this isn't optional, omitting it reintroduces a real
`Dagger/MissingBinding` failure.
