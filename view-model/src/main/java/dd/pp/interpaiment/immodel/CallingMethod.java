package dd.pp.interpaiment.immodel;

import java.util.HashMap;
import java.util.Map;

public class CallingMethod {
    private final String name;
    private final Map<Integer, CallingLine> callingLines = new HashMap<>();

    public CallingMethod(final Path path) {
        this.name = path.method;
        this.callingLines.put(path.line, new CallingLine(path));
    }

    public void put(final Path path) {
        final CallingLine callingLine = callingLines.get(path.line);

        if (callingLine != null) {
            callingLine.put(path);
        } else {
            callingLines.put(path.line, new CallingLine(path));
        }
    }
}
