package me.thiagorigonatti.rinhadebackendjavacore;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RinhaServer {

    private static final JSONObject jsonObject = new JSONObject();

    private static final String RINHA_SERVER_HOST = System.getenv("RINHA_SERVER_HOST");
    private static final int RINHA_SERVER_PORT = Integer.parseInt(System.getenv("RINHA_SERVER_PORT"));
    private static final String API_CONTEXT = System.getenv("API_CONTEXT");

    public void start() throws IOException {

        InetSocketAddress inetSocketAddress = new InetSocketAddress(RINHA_SERVER_HOST, RINHA_SERVER_PORT);
        HttpServer httpServer = HttpServer.create(inetSocketAddress, 0);
        httpServer.createContext(API_CONTEXT, new Handler());
        ExecutorService executorService = Executors.newCachedThreadPool();
        httpServer.setExecutor(executorService);
        httpServer.start();
    }

    public static void replyCode(HttpExchange exchange, int code) throws IOException {
        exchange.sendResponseHeaders(code, -1);
        exchange.close();
    }


    public static void replyWithBodyAndCode(HttpExchange exchange, final byte[] bytes, int code) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, bytes.length);
        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write(bytes);
        outputStream.flush();
        outputStream.close();
    }

    public static void replyWithBodyAndCode(HttpExchange exchange, String body, int code) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        final byte[] response = body.getBytes();
        exchange.sendResponseHeaders(code, response.length);
        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write(response);
        outputStream.flush();
        outputStream.close();
    }

    public static void reply(HttpExchange exchange, int code, String message) throws IOException {
        jsonObject.clear();
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        jsonObject.accumulate("error_message", message);
        jsonObject.accumulate("status_code", code);
        final byte[] messageB = jsonObject.toString().getBytes();
        exchange.sendResponseHeaders(code, messageB.length);
        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write(messageB);
        outputStream.flush();
        outputStream.close();
    }

}
