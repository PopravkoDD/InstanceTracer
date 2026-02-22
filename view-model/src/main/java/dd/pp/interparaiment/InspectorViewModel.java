package dd.pp.interparaiment;

import dd.pp.interparaiment.event.EventManager;
import dd.pp.interparaiment.event.requests.StartInspectionRequest;
import dd.pp.interparaiment.event.requests.StopInspectionRequest;
import dd.pp.interparaiment.peer.AgentReader;

public class InspectorViewModel {
    private final EventManager eventManager = new EventManager();
    private final AgentReader reader;

    public InspectorViewModel() {
        this.reader = new AgentReader();
        initListeners();
    }

    private void initListeners() {
        this.eventManager.subscribe(StartInspectionRequest.class, data -> {
            System.out.println("Start");
        });

        this.eventManager.subscribe(StopInspectionRequest.class, data -> {
            System.out.println("Stop");
        });
    }

    public EventManager getEventManager() {
        return this.eventManager;
    }
}
