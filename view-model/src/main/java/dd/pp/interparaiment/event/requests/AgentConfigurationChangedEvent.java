package dd.pp.interparaiment.event.requests;

import dd.pp.interparaiment.InspectorConfiguration;
import dd.pp.interparaiment.event.DataEvent;

public final class AgentConfigurationChangedEvent extends DataEvent<InspectorConfiguration> {
    public AgentConfigurationChangedEvent(InspectorConfiguration configuration) {
        super(configuration);
    }
}
