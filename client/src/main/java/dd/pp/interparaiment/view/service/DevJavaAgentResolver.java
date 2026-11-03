package dd.pp.interparaiment.view.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Properties;
import java.util.jar.JarFile;

/** Resolves only the development artifacts explicitly configured before the client is built. */
public final class DevJavaAgentResolver implements IJavaAgentResolver {
    private static final String RESOURCE = "/dev-java-agent.properties";
    private final String configuredRoot;

    public DevJavaAgentResolver() {
        final Properties properties = new Properties();
        try (InputStream stream = DevJavaAgentResolver.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing " + RESOURCE + "; rebuild the InstanceTracer client with this resource.");
            }
            properties.load(stream);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read InstanceTracer development resolver settings from " + RESOURCE, e);
        }
        this.configuredRoot = properties.getProperty("project.root", "").trim();
    }

    @Override
    public JavaAgentArtifacts resolve() throws IOException {
        final Path root = resolveRoot();
        final Path agent = root.resolve("agent").resolve("target").resolve("agent-1.0.jar");
        final Path hooks = root.resolve("hooks").resolve("target").resolve("hooks-1.0.jar");
        validateArtifact(agent, "Agent", "dd/pp/interparaiment/agents/InstanceTracerAgent.class");
        validateArtifact(hooks, "Hooks", "dd/pp/interparaiment/BootstrapHookBridge.class");
        return new JavaAgentArtifacts(agent, hooks);
    }

    private Path resolveRoot() throws IOException {
        if (this.configuredRoot.isEmpty()) {
            throw new IOException("Set project.root to the absolute InstanceTracer checkout path in "
                    + "client/src/main/resources/dev-java-agent.properties, then rebuild the client.");
        }
        try {
            final Path root = Path.of(this.configuredRoot).normalize();
            if (!root.isAbsolute() || !Files.isDirectory(root)) {
                throw new IOException("project.root in " + RESOURCE + " must name an existing absolute InstanceTracer checkout directory: " + root);
            }
            return root;
        } catch (InvalidPathException e) {
            throw new IOException("Invalid project.root in " + RESOURCE + ": " + e.getMessage(), e);
        }
    }

    private static void validateArtifact(final Path path, final String name, final String requiredClass) throws IOException {
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new IOException(name + " JAR is missing or unreadable: " + path
                    + ". Run the documented Maven clean install in the configured InstanceTracer checkout.");
        }
        try (JarFile jar = new JarFile(path.toFile())) {
            if (jar.getJarEntry(requiredClass) == null) {
                throw new IOException("Required InstanceTracer class is absent from " + path);
            }
        } catch (IOException e) {
            throw new IOException(name + " JAR is not a valid InstanceTracer artifact: " + path
                    + ". Rebuild it with the documented Maven clean install. " + e.getMessage(), e);
        }
    }
}
