package dd.pp.interparaiment.event.requests;

import dd.pp.interparaiment.event.DataEvent;

public class ShowMessageInConsoleRequest extends DataEvent<String> {
    public ShowMessageInConsoleRequest(String s) {
        super(s);
    }
}
