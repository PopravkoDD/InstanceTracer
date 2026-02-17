package dd.pp.interparaiment.agents;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.URISyntaxException;
import java.util.jar.JarFile;

import dd.pp.interparaiment.BootstrapHookBridge;
import dd.pp.interparaiment.bbhooks.ConstructionTracker;
import dd.pp.interparaiment.communication.AgentSendingPeer;

import static dd.pp.interparaiment.ULTRAHELPER.flex;

public class InstanceTracerAgent {
    public static void premain(String agentArgs, Instrumentation inst) {
        System.err.println("Instance tracer attached, NICE");

        flex();

        final String[] splitArgs = agentArgs.split(",");
        initHookBridge(inst, getBridgePathFromArgs(splitArgs));

        attachTrackers(inst, splitArgs);

        initAgentPeer();
    }

    private static void initHookBridge(Instrumentation inst, final String bridgeJarPath) {
        try {
            appendBridgeToBootstrap(inst, bridgeJarPath);
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getBridgePathFromArgs(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("BridgePath")) {
                 return arg.split("=")[1];
            }
        }
        return null;
    }

    private static void appendBridgeToBootstrap(final Instrumentation inst, final String pathToHookJar) throws URISyntaxException, java.io.IOException {
        File jar = new File(pathToHookJar);
        inst.appendToBootstrapClassLoaderSearch(new JarFile(jar));
        System.err.println("BOOTSTRAP APPENDED: " + jar);
    }

    private static void attachTrackers(final Instrumentation instrumentation, final String[] args) {
        for (String arg : args) {
            if (arg.startsWith("TraceTarget")) {
                ConstructionTracker.attachTracker(instrumentation, arg.split("=")[1]);
            }
        }
    }

    private static void initAgentPeer() {
        try {
            final AgentSendingPeer instance = AgentSendingPeer.getInstance();

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
