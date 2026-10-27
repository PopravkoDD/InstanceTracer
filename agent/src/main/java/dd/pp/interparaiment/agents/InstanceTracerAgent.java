package dd.pp.interparaiment.agents;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.URISyntaxException;
import java.util.jar.JarFile;

import dd.pp.interparaiment.BootstrapHookBridge;
import dd.pp.interparaiment.bbhooks.ConstructionTracker;
import dd.pp.interparaiment.communicator.AgentWriter;
import dd.pp.interparaiment.configuration.AgentArgumentsResolver;
import dd.pp.interparaiment.configuration.AgentConfiguration;

import static dd.pp.interparaiment.ULTRAHELPER.flex;

public class InstanceTracerAgent {
    public static void premain(String agentArgs, Instrumentation inst) {
        final AgentConfiguration configuration;
        try {
            configuration = AgentArgumentsResolver.resolve(agentArgs);
        } catch (IllegalArgumentException e) {
            System.err.println("Instance tracer disabled: " + e.getMessage());
            return;
        }

        System.err.println("Instance tracer attached, NICE");

        flex();

        initHookBridge(inst, configuration.getBridgePath());

        attachTrackers(inst, configuration);

        initAgentPeer(configuration);
    }

    private static void initHookBridge(Instrumentation inst, final String bridgeJarPath) {
        try {
            appendBridgeToBootstrap(inst, bridgeJarPath);
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void appendBridgeToBootstrap(final Instrumentation inst, final String pathToHookJar) throws URISyntaxException, java.io.IOException {
        File jar = new File(pathToHookJar);
        inst.appendToBootstrapClassLoaderSearch(new JarFile(jar));
        System.err.println("BOOTSTRAP APPENDED: " + jar);
    }

    private static void attachTrackers(final Instrumentation instrumentation, final AgentConfiguration configuration) {
        for (String target : configuration.getTraceTargets()) {
            ConstructionTracker.attachTracker(instrumentation, target);
        }
    }

    private static void initAgentPeer(final AgentConfiguration configuration) {
        try {
            final AgentWriter instance = AgentWriter.getInstance(configuration);

            BootstrapHookBridge.getInstance().setPeerNotificator(createdHash -> {
                final Object createdInstance = BootstrapHookBridge.getInstance().getTrackedInstances().get(createdHash);
                try {
                    instance.sendText((byte) 1, ((String) createdInstance));
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            });

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
