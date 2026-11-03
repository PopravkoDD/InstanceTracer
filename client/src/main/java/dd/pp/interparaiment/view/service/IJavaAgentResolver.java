package dd.pp.interparaiment.view.service;

import java.io.IOException;

public interface IJavaAgentResolver {
    JavaAgentArtifacts resolve() throws IOException;
}
