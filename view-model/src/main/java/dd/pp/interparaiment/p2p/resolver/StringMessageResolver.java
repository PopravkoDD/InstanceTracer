package dd.pp.interparaiment.p2p.resolver;

import java.util.regex.Pattern;

import dd.pp.interparaiment.immodel.MessModel;
import dd.pp.interparaiment.immodel.context.Path;

public class StringMessageResolver {
    private static final Pattern pattern = Pattern.compile(",");

    public static Path resolve(final String message) {
        final String[] split = pattern.split(message);

        return new Path(split[1], // target
                        split[2], // caller class
                        split[3], // caller method
                        Integer.parseInt(split[4]), // caller line
                        Integer.parseInt(split[0]));// instance hash
    }
}
