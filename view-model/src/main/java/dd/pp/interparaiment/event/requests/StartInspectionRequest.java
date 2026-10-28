package dd.pp.interparaiment.event.requests;

import dd.pp.interparaiment.InspectorViewModel.PreparedInspection;

public class StartInspectionRequest {
    private final PreparedInspection inspection;

    public StartInspectionRequest() {
        this(null);
    }

    public StartInspectionRequest(PreparedInspection inspection) {
        this.inspection = inspection;
    }

    public PreparedInspection getInspection() {
        return inspection;
    }
}
