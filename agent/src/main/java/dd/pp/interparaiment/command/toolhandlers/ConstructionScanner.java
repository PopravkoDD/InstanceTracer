package dd.pp.interparaiment.command.toolhandlers;

import java.lang.instrument.Instrumentation;
import java.util.Optional;
import java.util.stream.Stream;

import javafx.scene.Node;

import dd.pp.interparaiment.BootstrapHookBridge;
import dd.pp.interparaiment.command.BaseHookHandler;
import dd.pp.interparaiment.command.context.HandlingContext;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;

public class ConstructionScanner extends BaseHookHandler {
    public ConstructionScanner(final Instrumentation instrumentation, final String params) {
        final String targetName = extractTarget(params);
        new AgentBuilder.Default()
                .with(AgentBuilder.Listener.StreamWriting.toSystemError().withErrorsOnly())
                .type(ElementMatchers.named(targetName))
                .transform((builder, typeDesc, classLoader, module, pd) ->
                        builder.visit(
                                Advice.to(ConstructionScanner.class)
                                        .on(ElementMatchers.isConstructor())
                        )
                )
                .installOn(instrumentation);

        System.out.println(targetName + " Transformed");
    }

    private static String extractTarget(String target) {
        String key = "ScanTarget=";
        int start = target.indexOf(key);
        if (start != -1) {
            start += key.length();
            int end = target.indexOf(' ', start);
            if (end == -1) end = target.length();
            return target.substring(start, end);
        }
        return null;
    }

    @Override
    protected void handle(final HandlingContext context, final Node handlingObject) {
        final Object o = BootstrapHookBridge.getInstance().getScannedInstances().get(System.identityHashCode(handlingObject));
        if (o != null) {
            System.out.println("Target Tracked: " + o);
        }
    }

    @Advice.OnMethodExit()
    public static void exit(@Advice.This Object self) {
        final BootstrapHookBridge bhb = BootstrapHookBridge.getInstance();
        final int constructionHash = System.identityHashCode(self);

        if (!bhb.getScannedInstances().contains(constructionHash)) {
            bhb.getScannedInstances().put(constructionHash,
                    StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                            .walk(ConstructionScanner::walk)
                            .map(ConstructionScanner::format).get()
            );

            System.out.println("Tracker Attached: " + self);
        }
    }

    public static Optional<StackWalker.StackFrame> walk(Stream<StackWalker.StackFrame> s) {
        return s.skip(1).findFirst();
    }

    public static String format(StackWalker.StackFrame f) {
        return f.getClassName() + "." + f.getMethodName() +
                "(" + f.getFileName() + ":" + f.getLineNumber() + ")";
    }
}
