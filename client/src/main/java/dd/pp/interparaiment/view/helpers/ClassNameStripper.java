package dd.pp.interparaiment.view.helpers;

import java.util.regex.Pattern;

public class ClassNameStripper {
    private static final Pattern CLASS_NAME_SPLITTER = Pattern.compile("\\.");

    public static String stripFullName(final String fullName) {
        if (!fullName.contains(".")) {
            return fullName;
        }

        final String[] split = CLASS_NAME_SPLITTER.split(fullName);
        return split[split.length - 1];
    }
}
