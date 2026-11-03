package dd.pp.interparaiment.view.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import com.intellij.openapi.project.Project;

public final class DevJavaAgentResolver implements IJavaAgentResolver {
    private final Project project;
    private final String configuredRoot;
    private final String configurationError;
    private static final String RESOURCE = "dev-java-agent.properties";

    public DevJavaAgentResolver(Project project) {
        this.project = project;
        Properties properties = new Properties();
        String error = null;
        try (InputStream input = DevJavaAgentResolver.class.getResourceAsStream(RESOURCE)) {
            if (input == null) {
                throw new IOException("Missing development resolver resource " + RESOURCE);
            }
            properties.load(input);
        } catch (IOException | IllegalArgumentException e) {
            error = "Cannot load " + RESOURCE + ": " + e.getMessage();
        }
        configuredRoot = properties.getProperty("development.project.root", "").trim();
        configurationError = error;
    }

    @Override
    public AgentArtifacts resolve() throws IOException {
        Path root = resolveRoot();
        return new AgentArtifacts(requireArtifact(root.resolve("agent/target/agent-1.0.jar")),
                requireArtifact(root.resolve("hooks/target/hooks-1.0.jar")));
    }

    private Path resolveRoot() throws IOException {
        try {
            String basePath = project.getBasePath();
            Path base = basePath == null ? null : Path.of(basePath).toAbsolutePath().normalize();
            if (configurationError != null) {
                throw new IOException(configurationError);
            }
            if (configuredRoot.isEmpty()) {
                throw new IOException("Set development.project.root in " + RESOURCE
                        + " to the InstanceTracer checkout, then rebuild the client plugin");
            }
            Path root = Path.of(configuredRoot);
            if (!root.isAbsolute()) {
                if (base == null) {
                    throw new IOException("Use an absolute development.project.root in " + RESOURCE);
                }
                root = base.resolve(root);
            }
            root = root.toAbsolutePath().normalize();
            if (!Files.isDirectory(root)) {
                throw new IOException("development.project.root in " + RESOURCE + " is not a directory: " + root);
            }
            return root;
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
