package me.thiagorigonatti.rinhadebackendjavacore;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;

public class Handler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        exchange.getResponseHeaders().add("Content-Type", "application/json");

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

            JsonNode jsonNode = TransactionCRUD.findTenById(clientId);

            if (jsonNode == null) {
                RinhaServer.reply(exchange, 404, "NOT FOUND");
                return;
            }

            RinhaServer.replyWithBodyAndCode(exchange, jsonNode.toString(), 200);
        };
    }


    public HttpHandler transactionPost() {

        return exchange -> {

            long clientId = pathId(exchange, exchange.getRequestURI().getPath());

            JsonNode checkExistingClient = ClientCRUD.findById(clientId);

            if (checkExistingClient == null) {
                RinhaServer.reply(exchange, 404, "NOT FOUND");
                return;
            }


            InputStream inputStream = exchange.getRequestBody();
            final byte[] requestBody = inputStream.readAllBytes();

            if (JSONUtils.isNotValid(requestBody)) {
                RinhaServer.reply(exchange, 400, "BAD REQUEST");
                return;
            }

            JsonNode jsonNode = JSONUtils.OBJECT_MAPPER.readTree(requestBody);

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
                    !(jValor.asLong() > 0) ||
                    !jTipo.asText().matches("[dc]") ||
                    jTipo.isNull() ||
                    !jDescricao.asText().matches("^\\w{1,10}$") ||
                    jDescricao.isNull()
            ) {
                RinhaServer.reply(exchange, 422, "UNPROCESSABLE ENTITY");
                return;
            }


            JsonNode savedTransaction = TransactionCRUD.insert(jsonNode, clientId);

            if (savedTransaction == null) {
                RinhaServer.reply(exchange, 422, "UNPROCESSABLE ENTITY");
                return;
            }
            RinhaServer.replyWithBodyAndCode(exchange, savedTransaction.toString(), 200);
        };

    }


    public HttpHandler clientesPost() {

        return exchange -> {
            InputStream inputStream = exchange.getRequestBody();
            final byte[] request = inputStream.readAllBytes();

            if (JSONUtils.isNotValid(request))
                RinhaServer.reply(exchange, 400, "BAD REQUEST");

            JsonNode clientNodeRequest = JSONUtils.OBJECT_MAPPER.readTree(request);

            JsonNode jLimite = clientNodeRequest.get("limite");
            JsonNode jSaldo = clientNodeRequest.get("saldo");

            if (!jLimite.asText().matches("^\\d{1,15}$") ||
                    !jSaldo.asText().matches("^\\d{1,15}$") ||
                    jLimite.asLong() < 0 ||
                    jSaldo.asLong() < 0
            ) {
                RinhaServer.reply(exchange, 422, "UNPROCESSABLE ENTITY");
                return;
            }

            JsonNode savedClient = ClientCRUD.insert(clientNodeRequest);

            if (savedClient == null) {
                RinhaServer.reply(exchange, 400, "BAD REQUEST");
                return;
            }

            RinhaServer.replyWithBodyAndCode(exchange, savedClient.toString(), 200);
        };
    }


    public HttpHandler clientesGet() {
        return exchange -> {
            String[] pathParts = exchange.getRequestURI().getPath().split("/");
            long id;
            if (pathParts.length >= 3) {
                id = Long.parseLong(pathParts[2]);

                JsonNode jsonNode = ClientCRUD.findById(id);

                if (jsonNode == null) {
                    RinhaServer.reply(exchange, 404, "NOT FOUND");
                    return;
                }

                RinhaServer.replyWithBodyAndCode(exchange, jsonNode.toString(), 200);

            } else RinhaServer.reply(exchange, 404, "NOT FOUND");
        };
    }
}
