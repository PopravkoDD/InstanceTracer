package dd.pp.interparaiment.p2p.resolver;

import java.util.regex.Pattern;

import dd.pp.interparaiment.immodel.MessModel;
import dd.pp.interparaiment.immodel.context.Path;

public class StringMessageResolver {
    private final MessModel model;

    private static final Pattern pattern = Pattern.compile(",");

    public StringMessageResolver(MessModel model) {
        this.model = model;
    }

    public void resolve(final String message) {
        final String[] split = pattern.split(message);

        final Path path = new Path(split[1], // target
                                   split[2], // caller class
                                   split[3], // caller method
                                   Integer.parseInt(split[4]), // caller line
                                   Integer.parseInt(split[0]));// instance hash

        model.put(path);
    }
}
