# Kanban - Raven

**Last Updated**: 2026-09-04

---

## Project Status

- Issue #1 ("First commit: Base") complete: implemented, reviewed, CI fully
  green (Kotlin + Python pipelines, every tier), pushed to
  `feature/1-Base_Shell` as 6 clean logical commits (scaffold → core module →
  overlay feature → Python tooling → wireless ADB → CI wiring). Manual
  on-device verification done on the user's real phone and an emulator.
- 3-module structure in place: `:core` (PuzzleModule contract), `:placeholder`
  (stand-in solver module), `:app` (Android host).
- Overlay button: circular faded background, freely draggable, drag-to-
  bottom-zone kill (red indicator) that also removes Raven's task from
  Recents (`OverlayService.killSelf()` calls `ActivityManager.appTasks`
  `.finishAndRemoveTask()` — a real bug found via manual testing, now fixed
  and covered at integration-mock + instrumented level).
- Test-tier architecture: unit / integration-mock / integration-real /
  instrumented, all wired into CI. integration-mock and instrumented both
  have real content; integration-real is deliberately still empty (see
  `contexts/tests.md`) and CI now tolerates that gracefully instead of
  failing.
- Python `scripts/` dev-tooling ported from the sibling project's own
  tooling (build/test/validate/coverage), plus wireless ADB device connect.
- CI wired via `TomasGC/condor`'s reusable workflows. Getting it green
  surfaced several real Condor bugs (fixed there too, `[raven] #1:`-tagged
  commits): Otter-specific hardcoded job splits (now an opt-in
  `unit-job`/`mock-job` single-job mode), a missing `not local_only` pytest
  marker filter, and no tolerance for a project with an empty
  integration-mock/integration-real tier.
- `.claude/` structure (this file and its siblings) aligned to the sibling
  project's pattern; no `Raven`-prefixed class names remain anywhere
  (`OverlayService`, `OverlayButtonInstrumentedTest`, etc.) except
  `RavenApplication`/`RavenTheme`, which match that project's own
  convention of keeping the app name on those two specifically.

---

## Backlog

**High priority**
- Issue #2: Klondike solver (see `.claude/sessions/specs/2026-08-31-klondike-solver-design.md`
  and its implementation plan) — first real solver module.

**Medium priority**
- Issue #3: Queenzie solver — not yet designed.
- Issue #4: Amaze Go solver — design spec exists
  (`.claude/sessions/specs/2026-08-30-raven-v1-design.md`), shelved since
  the module-per-solver pivot; needs revisiting against the current
  `:core`/`PuzzleModule` shape before implementation.
- Apply the same Condor fixes to the sibling project (Otter) — separate
  session/ticket, not done yet.
- `gradle/verification-metadata.xml` holds a checksum per artifact, and AGP's `aapt2` has separate Windows and Linux
  jars, so it needs both platforms. After a dependency change, empty its `<components>`, rebuild the Windows entries
  with `./gradlew --write-verification-metadata sha256 --refresh-dependencies clean assembleDebug
  assembleDebugAndroidTest :app:testDebugUnitTest :core:test :placeholder:test detekt lintDebug koverXmlReportDebug
  koverHtmlReportDebug pixel4api30Setup`, then add the Linux entries with a throwaway push-triggered Actions job
  (KVM enabled, same command, uploads the file) on a branch that is not `feature/**`, and commit that file.
  The old entries must go, because the dependency scan reads this file and stale versions keep being flagged.

**Low priority**
- Version catalog (`gradle/libs.versions.toml`) instead of inline version
  literals — cost grows with each new solver module, not urgent yet.

---

2026-11-10 - [#10] Update the whole stack to the latest stable, JDK 21 included (with #7, #8, #9)
- Gradle 9.9.0 (`retries` and a 60 s `networkTimeout`), AGP 9.5.1 with Kotlin built in, Kotlin 2.4.21, Hilt 2.60.1 on KSP
  instead of kapt, Kover 0.9.11, Compose BOM 2026.09.00, androidx, coroutines 1.11.0, the androidx test set,
  Robolectric 4.17, the Python test packages and JDK 21; `compileSdk` is 37 and `targetSdk` stays 35, because raising it
  changes how the overlay behaves on a phone and deserves its own issue
- CI runs on Condor `v1`: PR-CI on pull requests (#9), Python tests marked by tier (#8), `push-ci.yml` with `unit-tasks`
  and `app-environments` (#7). Before, Push-CI failed at startup on inputs Condor no longer declares
- Coverage 97% (41/42 lines) against 95% (41/43) before; the denominator moved with the compiler. Dependency scan:
  scoped entries for build-tooling Guava and Freemarker, re-check 2026-12-05
- #7 is not done as written, and `contexts/tests.md` says why: the mock test uses Robolectric (no JVM suite), the
  instrumented tests are white-box Hilt tests (no `:app-instrumented`), and a separate mock task cost 5 points of
  coverage because Kover instruments only the variant's own unit-test task
tags: #ci-cd #condor #upgrade
Ref: https://github.com/TomasGC/Raven/issues/10
Commits: f233675, 4f42133, 683ecd0, ccbc4b3, 3419bcf, 6603f10, 11ed480, da92340, aa5dd6e, c495a46

---

2026-09-04 - [#1] Base shell: overlay button, dev tooling, CI
- Built the 3-module base shell (:core/:placeholder/:app) with a floating
  overlay control button: circular draggable button, drag-to-kill-zone
  removal that also clears Raven's task from Recents
- Added Python scripts/ dev tooling (build/test/validate/coverage) and
  wireless ADB device connect
- Wired CI via Condor's reusable workflows; fixed a chain of issues to get
  every tier green (detekt, dependency verification, permission grants,
  CVE exemptions), several of them real bugs in Condor itself
- Removed redundant Raven-prefix naming (OverlayService,
  OverlayButtonInstrumentedTest); restructured the branch into 6 clean
  logical commits
tags: #android #ci-cd #condor #testing
Ref: https://github.com/TomasGC/Raven/issues/1
Commits: 5aa5e0a, 26b60cb, 000a923, 8391eb7, f4de6cb, 72976fa

---

## Ideas

- Dagger multibindings (`@IntoSet`) to remove `AppModule`'s shared
  per-module edit point, once there are enough solver modules for the
  friction to be real.

---

## Related Documentation

- `.claude/CLAUDE.md` - Project instructions
- `.claude/contexts/architecture.md` - Module graph, internal `:app` flow
- `.claude/contexts/design-patterns.md` - Patterns in use and why
- `.claude/contexts/tests.md` - Test counts and structure
- `.claude/sessions/specs/` - Point-in-time design specs
- `.claude/sessions/plans/` - Point-in-time SDD implementation plans
