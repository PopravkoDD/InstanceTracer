package dd.pp.interparaiment.command;

import javafx.event.Event;
import javafx.scene.Node;

import dd.pp.interparaiment.command.context.HandlingContext;

public abstract class BaseHookHandler implements IAmInHookSittingHandler {
    private IAmInHookSittingHandler next;


    protected abstract void handle(final HandlingContext context, final Node handlingObject);

    @Override
    public void perform(final HandlingContext context, final Node handlingObject) {
        handle(context, handlingObject);

        if (this.next != null) {
            this.next.perform(context, handlingObject);
        }
    }

    @Override
    public void addNext(final IAmInHookSittingHandler next) {
        if (this.next != null) {
            this.next.addNext(next);
        }

        this.next = next;
    }
}
