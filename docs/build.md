# Build and validation

## Determine commands from the repository

The exact root Maven reactor, wrapper availability, Gradle packaging task, tool versions, and IntelliJ target have not been verified in this instruction package.

Read existing POMs, Gradle scripts, wrapper properties, and build documentation. Prefer committed wrappers when present. Do not copy machine-specific paths from another environment. Record verified commands here when established.

If a root POM aggregates all Maven modules, run from that reactor root:

    ./mvnw -B clean install

Use `mvn -B clean install` if there is no Maven wrapper and the environment provides the required Maven version. These are conditional command forms, not confirmation that a root reactor or wrapper exists.

If no complete root reactor exists, determine the dependency order from the POMs and build each required Maven module with `clean install`. Do not invent an order from directory names.

After Maven installation, use the client's Gradle wrapper and its existing compilation/plugin-packaging task. Inspect the task and dependencies before execution. Ensure Gradle resolves the freshly installed Maven artifacts through the project's existing dependency configuration; do not arbitrarily change versions or repositories.

## Allowed validation

Build the project. Maven validation must include `clean install` because the agent uses Shade. Do not substitute incremental `install` as the final check.

Tests do not currently exist according to the owner. When applicable tests are added, run them subject to the root validation rules. Do not create tests as incidental work.

A separate project with test algorithms is planned: it will run in its own process with the agent, and tests will inspect the received agent data. Its creation, scenarios, invocation, and assertions will be specified in separate tasks. Do not construct or launch an improvised equivalent.

Do not launch IntelliJ or functionally test the client unless explicitly requested. Select allowed compilation/packaging tasks if a broader Gradle task starts prohibited checks. Compilation may require IntelliJ SDK dependencies; downloading build dependencies is distinct from launching an IDE, and remains subject to environment access.

## Results

Record each command and outcome. Distinguish compilation failures, dependency/environment failures, test failures, checks not run, and runtime behavior not verified. Do not silently disable validation to obtain a green result.

Attempt in-scope repairs. Push the task branch even when the build remains failed or blocked, with an explicit report. If push itself fails, preserve the commit and report the actual publication status.
