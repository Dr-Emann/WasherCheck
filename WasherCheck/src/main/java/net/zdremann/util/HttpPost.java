package net.zdremann.util;

import java.io.IOException;
import java.io.Writer;
import java.net.URLEncoder;
import java.util.Map;

public class HttpPost {
    public static void writePostQuery(Writer writer, Map<String, String> args) throws IOException {
        boolean first = true;
        for(Map.Entry<String, String> entry : args.entrySet()) {
            if (first)
                first = false;
            else
                writer.write('&');

            writer.write(URLEncoder.encode(entry.getKey(), "UTF-8"));
            writer.write('=');
            writer.write(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }
    }
}
