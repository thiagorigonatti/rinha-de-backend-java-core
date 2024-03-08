package me.thiagorigonatti.rinhadebackendjavacore;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

public class Handler implements HttpHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();


    @Override
    public void handle(HttpExchange exchange) throws IOException {

        String uri = exchange.getRequestURI().getPath();
        String context = exchange.getHttpContext().getPath();
        String method = exchange.getRequestMethod();

        if (method.equals("POST")) {

            if (uri.matches(context + "/?$")) clientesPost().handle(exchange);
            else if (uri.matches(context + "/\\d{1,15}/transacoes/?$")) transactionPost().handle(exchange);
            else RinhaServer.reply(exchange, 400, "BAD REQUEST");

        } else if (method.equals("GET")) {

            if (uri.matches(context + "(/?$|(/\\d{1,15}/?$)?$)"))
                clientesGet().handle(exchange);

            else if (uri.matches(context + "/\\d{1,15}/extrato/?$"))
                extratoGet().handle(exchange);

            else RinhaServer.reply(exchange, 404, "NOT FOUND");

        } else RinhaServer.reply(exchange, 405, "METHOD NOT ALLOWED");
    }


    public long pathId(HttpExchange exchange, String path) throws IOException {

        String[] pathParts = path.split("/");

        if (pathParts.length >= 4) return Long.parseLong(pathParts[2]);
        else {
            RinhaServer.reply(exchange, 404, "NOT FOUND");
            return -1;
        }
    }

    public HttpHandler extratoGet() {

        return exchange -> {

            long clientId = pathId(exchange, exchange.getRequestURI().getPath());

            JSONObject byId = TransactionCRUD.findTenById(clientId);

            if (byId == null) {
                RinhaServer.reply(exchange, 404, "NOT FOUND");
                return;
            }

            RinhaServer.replyWithBodyAndCode(exchange, byId.toString(), 200);
        };
    }


    public HttpHandler transactionPost() {

        return exchange -> {

            long clientId = pathId(exchange, exchange.getRequestURI().getPath());

            InputStream inputStream = exchange.getRequestBody();
            final byte[] requestBody = inputStream.readAllBytes();

            if (!JSONUtils.isValid(requestBody)) {
                RinhaServer.reply(exchange, 400, "BAD REQUEST");
                return;
            }

            JsonNode jsonNode = objectMapper.readTree(requestBody);


            if (!jsonNode.has("valor") ||
                    !jsonNode.has("tipo") ||
                    !jsonNode.has("descricao")) {
                RinhaServer.reply(exchange, 400, "BAD REQUEST");
                return;
            }

            JsonNode jValor = jsonNode.get("valor");
            JsonNode jTipo = jsonNode.get("tipo");
            JsonNode jDescricao = jsonNode.get("descricao");

            if (!jValor.asText().matches("^\\d{1,15}$") ||
                    jValor.isNull() ||
                    !(jValor.asInt() > 0) ||
                    !jTipo.asText().matches("[dc]") ||
                    jTipo.isNull() ||
                    !jDescricao.asText().matches("^\\w{1,10}$") ||
                    jDescricao.isNull()
            ) {
                RinhaServer.reply(exchange, 422, "UNPROCESSABLE ENTITY");
                return;
            }

            Transaction transaction = objectMapper.treeToValue(jsonNode, Transaction.class);
            transaction.setClientId(clientId);

            JSONObject jsonObject = TransactionCRUD.altInsert(transaction);

            if (jsonObject == null) {
                RinhaServer.reply(exchange, 422, "UNPROCESSABLE ENTITY");
                return;
            }

            RinhaServer.replyWithBodyAndCode(exchange, jsonObject.toString(), 200);
        }

                ;

    }


    public HttpHandler clientesPost() {

        return exchange -> {
            InputStream inputStream = exchange.getRequestBody();
            final byte[] request = inputStream.readAllBytes();

            if (!JSONUtils.isValid(request))
                RinhaServer.reply(exchange, 400, "BAD REQUEST");

            JsonNode jsonNode = objectMapper.readTree(request);

            JsonNode jLimite = jsonNode.get("limite");
            JsonNode jSaldo = jsonNode.get("saldo");

            if (!jLimite.asText().matches("^\\d{1,15}$") ||
                    !jSaldo.asText().matches("^\\d{1,15}$") ||
                    jLimite.asInt() < 0 ||
                    jSaldo.asInt() < 0
            ) {
                RinhaServer.reply(exchange, 422, "UNPROCESSABLE ENTITY");
                return;
            }

            Client client = objectMapper.treeToValue(jsonNode, Client.class);

            Client savedClient = ClientCRUD.insert(client);

            byte[] bytes = objectMapper.writeValueAsBytes(savedClient);

            RinhaServer.replyWithBodyAndCode(exchange, bytes, 200);
        };

    }

    public HttpHandler clientesGet() {

        return exchange -> {

            String[] pathParts = exchange.getRequestURI().getPath().split("/");
            long id;

            if (pathParts.length >= 3) {
                id = Long.parseLong(pathParts[2]);

                Client returnedClient = ClientCRUD.findById(id);

                if (returnedClient == null) {
                    RinhaServer.reply(exchange, 404, "NOT FOUND");
                    return;
                }

                byte[] bytes = objectMapper.writeValueAsBytes(returnedClient);

                RinhaServer.replyWithBodyAndCode(exchange, bytes, 200);

            } else RinhaServer.reply(exchange, 404, "NOT FOUND");
        };
    }
}
