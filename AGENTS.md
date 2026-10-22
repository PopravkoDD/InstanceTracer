# Instance Tracer

## Project status and scope

Instance Tracer is an existing project. The core of every module is already implemented. Develop and improve the existing implementation; do not scaffold replacements or rewrite modules merely to match this documentation.

The repository has five modules:
- `agent`: Maven; instruments the target JVM, collects information, and sends it over TCP using the existing protocol.
- `hooks`: Maven; bootstrap bridge for interaction inside the target JVM.
- `view-model`: Maven; receives agent data and distributes it into UI-independent models.
- `client`: Gradle; IntelliJ plugin that renders and interacts with these models.
- `common`: Maven; shared utilities.

Before editing a module, read its `AGENTS.md`, even when working from the repository root. Read relevant source and build files before choosing implementation details. Preserve unrelated changes.

## Architecture

- Keep all client-to-agent communication behind `view-model` APIs. Do not add socket clients or wire-protocol handling to `client`.
- Keep `view-model` free of UI APIs, including IntelliJ, Swing, and JavaFX.
- Keep bootstrap bridge dependencies compatible with their actual classloader visibility.
- Preserve Java 17 as the minimum supported baseline for target-JVM code and shared code used there. Do not introduce newer bytecode or APIs into that path. Respect the existing IntelliJ platform requirements for the plugin and build tooling; do not assume their build JDK must be 17.
- Treat the launch flow in `docs/lifecycle.md` as planned behavior, not proof of existing implementation or authorization to implement it during unrelated work.
- The TCP protocol already exists. Inspect and preserve it; do not invent or replace it based on the incomplete documentation.
- Prefer non-blocking queues, low allocation rates, and minimal target-JVM overhead. Do not claim measured improvements without measurements.
- Aim to preserve all collected data. Do not introduce silent dropping, sampling, or lossy aggregation unless the task explicitly requests that collection behavior.
- Allow collection policies to vary through agent configuration and instrumentation decisions without coupling transport or model APIs to a single mode. Do not implement an additional mode unless requested.
- Write maintained project documentation and instructions in English. Follow existing code conventions.

## Validation

- Build Maven modules with `clean install`, never plain `install` as the final Maven validation. The agent uses Shade; clean output is required to avoid recursively incorporating previously shaded artifacts.
- Build Maven artifacts before building the Gradle client. Determine the actual reactor and Gradle tasks from existing build configuration; follow `docs/build.md`.
- Do not create tests, a test project, benchmarks, or a test harness unless the user specifically assigns that work.
- Run existing applicable tests when available, subject to the following restrictions.
- Do not launch or functionally test the IntelliJ client, run `runIde`, start an IDE sandbox, or run tests/tasks that launch IntelliJ unless the user specifically requests it. Compilation and plugin packaging are allowed.
- Do not launch a target application or a separate JVM test fixture as an improvised smoke test. A dedicated external-process test project is planned and will be specified separately.
- Do not skip tests or weaken checks merely to make validation pass. If an aggregate build task would launch prohibited validation, use an allowed compilation/packaging task and report the omitted checks.
- Report exact commands, outcomes, and limitations. A successful build is not proof of runtime correctness or performance.

## Git workflow

- For each new implementation task, fetch `origin` and create a new task branch from current `origin/main`, preferably `codex/<short-task-name>`.
- Continue an existing task branch for follow-up work on that same task; do not create a fresh branch for every clarification.
- Never base a new independent task on another unfinished task branch.
- Preserve existing work. Do not reset, clean, stash, or overwrite unrelated changes merely to switch branches. If the checkout cannot be used safely, report the concrete blocker.
- Commit only task-related changes and push the task branch with an upstream.
- Push the task result even if validation fails, with an explicit failure report. Attempt reasonable fixes within scope first. Never label a failed or blocked build as passed.
- If access or network restrictions prevent fetch or push, report that limitation honestly; do not claim a branch is current or published without evidence.
- Do not push directly to `main`, merge branches, delete branches, force-push, or create a pull request unless explicitly requested. The user reviews, merges, and deletes branches.

## Task completion report

Report the branch name, commit, push status, concise change summary, build/test commands and outcomes, and any unresolved failures or unverified behavior. Creating a PR is not part of the default workflow.

## Reference documents and skills

Read `docs/architecture.md` for module boundaries; `docs/performance.md` for event handling; `docs/lifecycle.md` for startup work; `docs/protocol.md` for wire changes; and `docs/build.md` before validation.

Use the repository skills `instance-tracer-task`, `instance-tracer-build`, and `instance-tracer-protocol` for their corresponding workflows. The mandatory rules above apply even if a skill is not selected.
