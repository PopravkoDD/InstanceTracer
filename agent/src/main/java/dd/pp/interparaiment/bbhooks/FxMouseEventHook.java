package dd.pp.interparaiment.bbhooks;

import dd.pp.interparaiment.BootstrapHookBridge;
import net.bytebuddy.asm.Advice;

public class FxMouseEventHook {
    @Advice.OnMethodEnter
    static void onEnter(@Advice.Argument(0) javafx.event.EventDispatchChain chain,
                        @Advice.Argument(1) javafx.event.EventTarget target,
                        @Advice.Argument(2) javafx.event.Event event) {
        BootstrapHookBridge.getInstance().add(event);
    }
}
