package dd.pp.interparaiment.view.service;

import java.io.IOException;
import java.nio.file.Path;

/** Artifact distribution is a client concern, independent of launch and ViewModel. */
public interface IJavaAgentResolver {
    AgentArtifacts resolve(ClientConfiguration configuration) throws IOException;

    record AgentArtifacts(Path agent, Path hooks) {}
}
