package dd.pp.interparaiment.view.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import com.intellij.openapi.project.Project;

public final class DevJavaAgentResolver implements IJavaAgentResolver {
    private final Project project;

    public DevJavaAgentResolver(Project project) {
        this.project = project;
    }

    @Override
    public AgentArtifacts resolve(ClientConfiguration configuration) throws IOException {
        Path root = resolveRoot(configuration);
        return new AgentArtifacts(requireArtifact(root.resolve("agent/target/agent-1.0.jar")),
                requireArtifact(root.resolve("hooks/target/hooks-1.0.jar")));
    }

    private Path resolveRoot(ClientConfiguration configuration) throws IOException {
        try {
            String basePath = project.getBasePath();
            Path base = basePath == null ? null : Path.of(basePath).toAbsolutePath().normalize();
            String configuredRoot = configuration.developmentProjectRoot();
            if (!configuredRoot.isEmpty()) {
                Path root = Path.of(configuredRoot);
                if (!root.isAbsolute()) {
                    if (base == null) {
                        throw new IOException("Use an absolute development project root for this project");
                    }
                    root = base.resolve(root);
                }
                return root.toAbsolutePath().normalize();
            }
            for (Path root = base; root != null; root = root.getParent()) {
                if (Files.isRegularFile(root.resolve("pom.xml"))
                        && Files.isRegularFile(root.resolve("agent/pom.xml"))
                        && Files.isRegularFile(root.resolve("hooks/pom.xml"))) {
                    return root;
                }
            }
            throw new IOException("Cannot locate the InstanceTracer development project. "
                    + "Set its root in Configure Tracking and run Maven clean install there.");
        } catch (InvalidPathException e) {
            throw new IOException("Invalid development project root: " + e.getInput(), e);
        }
    }

    private Path requireArtifact(Path artifact) throws IOException {
        if (!Files.isRegularFile(artifact) || !Files.isReadable(artifact)) {
            throw new IOException("Missing or unreadable agent artifact: " + artifact
                    + ". Run Maven clean install in the InstanceTracer development project.");
        }
        return artifact;
    }
}
