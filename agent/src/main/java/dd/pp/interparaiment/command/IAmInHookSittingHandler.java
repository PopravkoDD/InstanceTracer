package dd.pp.interparaiment.command;

import javafx.event.Event;
import javafx.scene.Node;

import dd.pp.interparaiment.command.context.HandlingContext;

public interface IAmInHookSittingHandler {
    void perform(final HandlingContext context, final Node handlingObject);
    void addNext(IAmInHookSittingHandler next);
}
