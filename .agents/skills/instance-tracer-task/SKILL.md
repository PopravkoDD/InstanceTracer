---
name: instance-tracer-task
description: Carry out an Instance Tracer repository change from a task branch through validation, commit, and push. Use when implementing or completing a development task in this project.
---

# Complete an Instance Tracer task

1. Read root `AGENTS.md` and the instructions for affected modules. Inspect existing implementation and relevant documentation; treat the project as established code.
2. Check Git status and the current task context. For a new independent task, fetch origin and create `codex/<short-task-name>` from current `origin/main`. For follow-up work, continue the existing task branch. Preserve unrelated work and report access or checkout blockers.
3. Make focused changes that satisfy the assigned task. Do not implement future lifecycle features, collection modes, or tests merely because documentation mentions them.
4. Update documentation for behavior actually changed. Keep planned and implemented behavior distinct.
5. Follow `instance-tracer-build` and `docs/build.md`. Attempt reasonable in-scope fixes for failures; retain accurate results.
6. Review the diff for unintended changes and prohibited validation. Stage only task-related files and commit meaningful changes.
7. Push the task branch with upstream, including when validation failed. Do not create a PR, merge, delete branches, or force-push by default.
8. Report branch, commit, actual push status, changes, validation commands/results, and unresolved limitations. If no file changes were needed, report that without creating an empty commit.
