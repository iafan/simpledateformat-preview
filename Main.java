import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

class HomePageHandler implements HttpHandler {
    public void handle(HttpExchange t) throws IOException {
        File file = new File("index.html").getCanonicalFile();

        if (!file.isFile()) {
            String response = "404 (Not Found)\n";
            t.sendResponseHeaders(404, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
            return;
        }

        Headers h = t.getResponseHeaders();
        h.add("Content-Type", "text/html;charset=utf-8");
        t.sendResponseHeaders(200, 0);

        FileInputStream fs = new FileInputStream(file);
        OutputStream os = t.getResponseBody();
        final byte[] buffer = new byte[0x10000];
        int count = 0;
        while ((count = fs.read(buffer)) >= 0) {
            os.write(buffer,0,count);
        }
        fs.close();
        os.close();
    }
}

class SimpleDateFormatHandler implements HttpHandler {
    public static Map<String, String> parseQueryString(String qs) {
        Map<String, String> result = new HashMap<>();
        if (qs == null)
            return result;

        int last = 0, next, l = qs.length();
        while (last < l) {
            next = qs.indexOf('&', last);
            if (next == -1)
                next = l;

            if (next > last) {
                int eqPos = qs.indexOf('=', last);
                try {
                    if (eqPos < 0 || eqPos > next)
                        result.put(
                            URLDecoder.decode(qs.substring(last, next), "utf-8"),
                            ""
                        );
                    else
                        result.put(
                            URLDecoder.decode(qs.substring(last, eqPos), "utf-8"),
                            URLDecoder.decode(qs.substring(eqPos + 1, next), "utf-8")
                        );
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e); // should never happen with utf-8
                }
            }
            last = next + 1;
        }
        return result;
    }

    public void handle(HttpExchange t) throws IOException {
        Map<String, String> q = parseQueryString(t.getRequestURI().getQuery());
        String fmt = q.get("fmt");
        String lang = q.get("lang");

        Date now = new Date();
        String response = "";
        SimpleDateFormat formatter;
        try {
            formatter = new SimpleDateFormat(fmt, Locale.forLanguageTag(lang));
            response = formatter.format(now);
        } catch (IllegalArgumentException e) {
            response = e.getMessage();
        } catch (NullPointerException e) {
            // nothing, return empty message
        }

        Headers h = t.getResponseHeaders();
        h.add("Content-Type", "text/html;charset=utf-8");

        byte[] data = response.getBytes();
        t.sendResponseHeaders(200, data.length);
        OutputStream os = t.getResponseBody();
        os.write(data);
        os.close();
    }
}

public class Main {
    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/", new HomePageHandler());
        server.createContext("/render", new SimpleDateFormatHandler());
        server.setExecutor(null); // use a default executor
        System.out.println("Running server");
        server.start();
    }
}
