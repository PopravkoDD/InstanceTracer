package dd.pp.interparaiment;

import java.io.IOException;

import dd.pp.interparaiment.event.EventManager;
import dd.pp.interparaiment.event.requests.ShowMessageInConsoleRequest;
import dd.pp.interparaiment.event.requests.StartInspectionRequest;
import dd.pp.interparaiment.event.requests.StopInspectionRequest;
import dd.pp.interparaiment.peer.AgentReader;

public class InspectorViewModel {
    private final EventManager eventManager = new EventManager();
    private final AgentReader reader;

    public InspectorViewModel() {
        try {
            this.reader = new AgentReader(this.eventManager);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        initListeners();
    }

    private void initListeners() {
        this.eventManager.subscribe(StartInspectionRequest.class, data -> {
            try {
                reader.open();
            } catch (IOException e) {
                this.eventManager.notify(new ShowMessageInConsoleRequest("Connection failed: " + e.getMessage()));
            }
        });

        this.eventManager.subscribe(StopInspectionRequest.class, data -> {
            System.out.println("Stop");
        });
    }

    public EventManager getEventManager() {
        return this.eventManager;
    }
}
