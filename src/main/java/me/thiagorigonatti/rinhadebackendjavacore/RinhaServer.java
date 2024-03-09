package me.thiagorigonatti.rinhadebackendjavacore;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RinhaServer {

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


    public static void replyWithBodyAndCode(HttpExchange exchange, final byte[] body, int code) throws IOException {

        String ip = exchange.getRemoteAddress().toString();
        String method = exchange.getRequestMethod();
        String uri = exchange.getRequestURI().getPath();
        Rinha.LOG.info(String.format("[%s %s %s %d %s]", ip, method, uri, code, new String(body)));
        exchange.sendResponseHeaders(code, body.length);
        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write(body);
        outputStream.flush();
        exchange.close();
    }

    public static void replyWithBodyAndCode(HttpExchange exchange, String body, int code) throws IOException {
        final byte[] response = body.getBytes();
        replyWithBodyAndCode(exchange, response, code);
        exchange.close();
    }

    public static void reply(HttpExchange exchange, int code, String message) throws IOException {

        String ip = exchange.getRemoteAddress().toString();
        String method = exchange.getRequestMethod();
        String uri = exchange.getRequestURI().getPath();

        Rinha.LOG.info(String.format("[%s %s %s %d %s]", ip, method, uri, code, message));

        ObjectNode objectNode = JSONUtils.OBJECT_MAPPER.createObjectNode();
        objectNode.put("error_message", message);
        objectNode.put("status_code", code);
        final byte[] response = objectNode.toString().getBytes();
        exchange.sendResponseHeaders(code, response.length);
        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write(response);
        outputStream.flush();
        exchange.close();
    }

}
