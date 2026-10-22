# Client module

Maintain the existing Gradle-based IntelliJ plugin and its forms.

- Use `view-model` APIs for all agent communication and access to received models.
- Keep socket handling, wire decoding, and data distribution logic out of the UI module.
- Follow existing IntelliJ threading and disposal conventions. Avoid blocking UI threads with connection, reception, or model-processing work.
- Keep UI-specific adapters and rendering in this module.
- The dedicated action, configuration dialog, and agent-enabled launch sequence are planned. Implement or change them only within the assigned task.
- If working on launch configuration, inspect the current IntelliJ integration and avoid unintentionally persisting task-specific agent options into the user's normal run configuration.
- Compilation and plugin packaging are allowed. Do not launch IntelliJ, use `runIde`, run UI tests, or functionally test the plugin unless explicitly requested.
- Inspect Gradle task dependencies before using broad lifecycle tasks that could start an IDE or its test runtime.
- Respect the existing IntelliJ platform and build JDK requirements. Do not arbitrarily upgrade them or assume the plugin can run on every Java 17 runtime.
