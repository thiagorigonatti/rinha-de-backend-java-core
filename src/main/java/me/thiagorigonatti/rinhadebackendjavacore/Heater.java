package me.thiagorigonatti.rinhadebackendjavacore;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Heater {

    private final HttpClient client = HttpClient.newHttpClient();


    private final HttpRequest httpRequest1 = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8080/clientes/1/transacoes/"))
            .method("POST", HttpRequest.BodyPublishers.ofString("""
                    {
                        "valor": 5555,
                        "tipo" : "c",
                        "descricao" : "warmupc"
                    }
                    """))
            .build();

    private final HttpRequest httpRequest2 = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8080/clientes/1/"))
            .method("GET", HttpRequest.BodyPublishers.noBody())
            .build();


    private final HttpRequest httpRequest3 = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8080/clientes/1/transacoes/"))
            .method("POST", HttpRequest.BodyPublishers.ofString("""
                    {
                        "valor": 5555,
                        "tipo" : "d",
                        "descricao" : "warmupd"
                    }
                    """))
            .build();


    private final HttpRequest httpRequest4 = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8080/clientes/1/extrato/"))
            .method("GET", HttpRequest.BodyPublishers.noBody())
            .build();


    public void warm() throws IOException, InterruptedException {
        client.send(httpRequest1, HttpResponse.BodyHandlers.discarding());
        client.send(httpRequest2, HttpResponse.BodyHandlers.discarding());
        client.send(httpRequest3, HttpResponse.BodyHandlers.discarding());
        client.send(httpRequest4, HttpResponse.BodyHandlers.discarding());
    }


}
