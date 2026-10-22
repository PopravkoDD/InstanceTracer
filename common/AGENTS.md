# Common module

Maintain the existing Maven module for shared utilities.

- Place code here only when it has a justified shared responsibility; do not use this module as a destination for unrelated code.
- Keep utilities independent of IntelliJ and other UI APIs.
- Do not introduce dependencies back to `agent`, `hooks`, `view-model`, or `client` that create cycles or reverse ownership.
- Preserve compatibility with existing consumers and Java 17 where loaded into the target JVM.
- Evaluate each dependency against all consumers. Shared does not mean bootstrap-visible.
- Do not relocate protocol types or utilities merely to impose a new layout. Follow the existing ownership unless the task calls for a change.
