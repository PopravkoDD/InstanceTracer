package dd.pp.interparaiment;

import java.io.IOException;

import dd.pp.interparaiment.event.EventManager;
import dd.pp.interparaiment.event.requests.ShowMessageInConsoleRequest;
import dd.pp.interparaiment.event.requests.StartInspectionRequest;
import dd.pp.interparaiment.event.requests.StopInspectionRequest;
import dd.pp.interparaiment.immodel.MessModel;
import dd.pp.interparaiment.p2p.AgentReader;
import dd.pp.interparaiment.p2p.DataResolvingWorker;

public class InspectorViewModel {
    private final EventManager eventManager = new EventManager();
    private final AgentReader reader;
    private final DataResolvingWorker dataResolver;

    private final MessModel messModel;


    public InspectorViewModel() {
        this.messModel = new MessModel();

        this.dataResolver = new DataResolvingWorker(this);

        try {
            this.reader = new AgentReader(this.eventManager, this.dataResolver);
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

    public MessModel getMessModel() {
        return messModel;
    }
}
