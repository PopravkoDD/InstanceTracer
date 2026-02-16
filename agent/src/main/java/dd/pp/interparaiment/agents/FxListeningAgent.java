package dd.pp.interparaiment.agents;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.URISyntaxException;
import java.util.jar.JarFile;

import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.stage.Window;

import dd.pp.interparaiment.BootstrapHookBridge;
import dd.pp.interparaiment.bbhooks.ConstructionTracker;
import dd.pp.interparaiment.bbhooks.FxMouseEventHook;
import dd.pp.interparaiment.command.IAmInHookSittingHandler;
import dd.pp.interparaiment.command.context.HandlingContext;
import dd.pp.interparaiment.command.toolhandlers.FxUiClickConstructionChecker;
import dd.pp.interparaiment.command.toolhandlers.PathLogger;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;

import static dd.pp.interparaiment.ULTRAHELPER.*;

public class FxListeningAgent {
    public static void premain(String agentArgs, Instrumentation inst) {
        System.err.println("FX spy attached, NICE");

        flex();

        initHookBridge(inst, extractBootHookStringParam(agentArgs));

        initFxHook(inst);

        final IAmInHookSittingHandler hhc = initHandlingParams(inst, agentArgs);

        startWatcher(hhc);
    }

    private static String extractBootHookStringParam(final String allParams) {
        return allParams.split(",")[0];
    }

    private static IAmInHookSittingHandler initHandlingParams(final Instrumentation instrumentation, final String agentArgs) {
        final IAmInHookSittingHandler hookHandlingChain = new PathLogger();

        if (agentArgs.contains("ScanTarget")) {
            ConstructionTracker.attachTracker(instrumentation, agentArgs);

            hookHandlingChain.addNext(new FxUiClickConstructionChecker());
        }

        return hookHandlingChain;
    }

    private static void initFxHook(Instrumentation inst) {
        new AgentBuilder.Default()
                .with(AgentBuilder.Listener.StreamWriting.toSystemError().withErrorsOnly())
                .type(ElementMatchers.named("com.sun.javafx.event.EventUtil"))
                .transform((builder, typeDescription, classLoader, module, protectionDomain) -> {
                    System.err.println("TRANSFORM HIT: " + typeDescription.getName()
                            + " loader=" + classLoader + " module=" + module);
                    return builder.visit(Advice.to(FxMouseEventHook.class)
                            .on(ElementMatchers.named("fireEventImpl")));
                }).installOn(inst);
    }

    private static void initHookBridge(Instrumentation inst, String pathToHookJar) {
        try {
            appendSelfToBootstrap(inst, pathToHookJar);
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void startWatcher(final IAmInHookSittingHandler hhc) {
        new Thread(() -> {
            while (true) {
                final Object event = BootstrapHookBridge.getInstance().poll();
                if (event instanceof MouseEvent) { // filter here
                    final MouseEvent mouseEvent = (MouseEvent) event;

                    if (mouseEvent.getEventType() == MouseEvent.MOUSE_PRESSED) {
                        final Window upper = Window.getWindows().stream().filter(Window::isFocused).findFirst().orElse(null);
                        final Node lower = mouseEvent.getPickResult().getIntersectedNode();

                        final HandlingContext handlingContext = new HandlingContext(((Event) event));

                        Node handlingObject = lower;

                        while (handlingObject != null && handlingObject != upper.getScene().getRoot()) {
                            hhc.perform(handlingContext, handlingObject);
                            handlingObject = handlingObject.getParent();
                        }

                        System.out.println("Mouse Pressed Event: " +
                                (handlingObject == null ? "Va Ta Fa Pepe?!??! Null???? -> " : "") +
                                handlingContext.pathLogResult.substring(4));
                    }
                }
            }
        }).start();
    }

    private static void appendSelfToBootstrap(final Instrumentation inst, final String pathToHookJar) throws URISyntaxException, java.io.IOException {
        File jar = new File(pathToHookJar);
        inst.appendToBootstrapClassLoaderSearch(new JarFile(jar));
        System.err.println("BOOTSTRAP APPENDED: " + jar);
    }
}
