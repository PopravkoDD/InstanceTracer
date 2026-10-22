---
name: instance-tracer-build
description: Build and validate Instance Tracer changes using Maven clean install followed by allowed Gradle client tasks. Use before finishing a code or build change, or when asked to build the project.
---

# Validate an Instance Tracer change

1. Read root and client validation restrictions plus `docs/build.md`.
2. Inspect current build files and wrappers. Determine the Maven reactor or dependency order, required toolchains, and allowed Gradle compilation/packaging tasks. Do not assume Java 17 is the build JDK for every tool.
3. Build Maven modules using `clean install`. Never replace this with plain `install` for final validation of shaded artifacts.
4. Build the client using its existing Gradle configuration after Maven artifacts are installed. Check task dependencies to avoid launching IntelliJ or functional client tests.
5. Run existing applicable allowed tests if present. Do not create tests, launch target processes, or build a new harness without a separate assignment.
6. Diagnose failures without weakening checks. Distinguish code errors from environment/access problems. Record any checks omitted because they are prohibited.
7. Report exact commands, outcomes, and runtime behavior not verified. Return these results to the task workflow; a failed build does not cancel the required branch push.
