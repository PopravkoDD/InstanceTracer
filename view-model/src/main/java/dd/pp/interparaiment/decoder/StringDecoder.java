package dd.pp.interparaiment.decoder;

import java.nio.charset.StandardCharsets;

public class StringDecoder {
    public static String decodeUtf8(final byte[] payload) {
        return new String(payload, StandardCharsets.UTF_8);
    }
}
