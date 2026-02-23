package dd.pp.interparaiment.bbhooks;

import java.lang.instrument.Instrumentation;
import java.util.Optional;
import java.util.stream.Stream;

import dd.pp.interparaiment.BootstrapHookBridge;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;

public class ConstructionTracker {

    public static void attachTracker(final Instrumentation instrumentation, final String targetName) {
        new AgentBuilder.Default()
                .with(AgentBuilder.Listener.StreamWriting.toSystemError().withErrorsOnly())
                .type(ElementMatchers.named(targetName))
                .transform((builder, typeDesc, classLoader, module, pd) ->
                        builder.visit(
                                Advice.to(ConstructionTracker.class)
                                        .on(ElementMatchers.isConstructor())
                        )
                )
                .installOn(instrumentation);

        System.out.println(targetName + " Transformed");
    }

    @Advice.OnMethodExit()
    public static void exit(@Advice.This Object self) {
        final BootstrapHookBridge bhb = BootstrapHookBridge.getInstance();
        final int constructionHash = System.identityHashCode(self);

        if (!bhb.getTrackedInstances().contains(constructionHash)) {
            bhb.getTrackedInstances().put(constructionHash, createMessage(self));
            bhb.notifyInstanceCreated(constructionHash);
            System.out.println("Instance tracked");
        }
    }
    public static Optional<StackWalker.StackFrame> walk(Stream<StackWalker.StackFrame> s) {
        return s.skip(1).findFirst();
    }

    public static String format(StackWalker.StackFrame f) {
        return f.getClassName() + "." + f.getMethodName() +
                "(" + f.getFileName() + ":" + f.getLineNumber() + ")";
    }

    public static String createMessage(final Object self) {
        return self.getClass().getName() + ": " + StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .walk(ConstructionTracker::walk)
                .map(ConstructionTracker::format).get();
    }
}
