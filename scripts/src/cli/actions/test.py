"""Test action — run the Kotlin test suite, tiered unit/integration-mock/integration-real/instrumented."""

import os
from pathlib import Path
from typing import Optional

from android import AdbManager, GradleRunner
from common.file_utils import get_project_root
from common.subprocess_runner import SubprocessRunner

SUITES = ["unit", "integration-mock", "integration-real", "instrumented"]


class TestAction:
    def __init__(
        self,
        runner: SubprocessRunner,
        gradle: Optional[GradleRunner] = None,
        adb: Optional[AdbManager] = None,
        project_root: Optional[Path] = None,
    ) -> None:
        self._runner = runner
        self._project_root = project_root or get_project_root()
        self._gradle = gradle or GradleRunner(runner, self._project_root)
        self._adb = adb or AdbManager(runner)

    def run_all_untiered(self) -> bool:
        # No -DtestType: the app/build.gradle.kts filter's "all" default applies no
        # filter, so this runs every JVM test that currently exists regardless of
        # tier. Used for the plain `test` command (no suite named) — looping the
        # individual tiers instead would fail today, since Gradle's test task fails
        # outright when a filter matches zero tests, and integration-real has no
        # tests yet. Once each tier has real content, `test` still works (it's a
        # superset), and naming a tier explicitly gets you the filtered view.
        return self._gradle.run_task("test", extra_args=["testDebugUnitTest"])

    def run_unit(self) -> bool:
        # "test" triggers :core:test + :placeholder:test (untiered — too small to split
        # into unit/integration-mock yet); testDebugUnitTest is :app's variant, tiered
        # via the -DtestType filter in app/build.gradle.kts.
        return self._gradle.run_task("test", extra_args=["testDebugUnitTest", "-DtestType=unit"])

    def run_integration_mock(self) -> bool:
        return self._gradle.run_task("testDebugUnitTest", extra_args=["-DtestType=integration-mock"])

    def run_integration_real(self) -> bool:
        return self._gradle.run_task("testDebugUnitTest", extra_args=["-DtestType=integration-real"])

    def run_instrumented(self, device: str) -> bool:
        os.environ["ANDROID_SERIAL"] = device
        try:
            return self._gradle.run_task("connectedDebugAndroidTest", timeout=900)
        finally:
            os.environ.pop("ANDROID_SERIAL", None)

    def run(self, suites: list[str] | None = None) -> int:
        suites = suites or []

        if not suites:
            return 0 if self.run_all_untiered() else 1

        success = True

        if "unit" in suites:
            if not self.run_unit():
                success = False

        if "integration-mock" in suites:
            if not self.run_integration_mock():
                success = False

        if "integration-real" in suites:
            if not self.run_integration_real():
                success = False

        if "instrumented" in suites:
            devices = self._adb.get_connected()
            if not devices:
                device = self._ensure_emulator()
                if not device:
                    print("No device connected for instrumented tests")
                    success = False
                elif not self.run_instrumented(device):
                    success = False
            elif not self.run_instrumented(devices[0]):
                success = False

        return 0 if success else 1

    def _ensure_emulator(self) -> str | None:
        emulators = self._adb.get_running_emulators()
        if emulators:
            print(f"Found running emulator: {emulators[0]}, waiting for ready state...")
            return self._adb.wait_for_emulator()
        avds = self._adb.list_avds()
        if not avds:
            print("No AVD found — create one in Android Studio")
            return None
        print(f"Starting emulator: {avds[0]}")
        if not self._adb.start_emulator(avds[0]):
            return None
        return self._adb.wait_for_emulator()
