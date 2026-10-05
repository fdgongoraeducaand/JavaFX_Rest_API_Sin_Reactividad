package com.example.javafx_sinreactividad.modelos;

import com.example.javafx_sinreactividad.modelos.Usuario;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * Serviço isolado para consumo de APIs REST.
 * Independente de JavaFX e sem elementos reativos.
 */
public class ApiService {

    private static final String DEFAULT_URL = "https://jsonplaceholder.typicode.com/users";

    private final String apiUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    // Construtor padrão apontando para a URL base
    public ApiService() {
        this(DEFAULT_URL);
    }

    // Construtor flexível para injetar endpoints alternativos
    public ApiService(String apiUrl) {
        this.apiUrl = apiUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Realiza uma chamada HTTP GET síncrona e devolve uma List padrão de Java.
     *
     * @return List<Usuario> contendo os POJOs desserializados.
     * @throws IOException em falhas de rede ou status HTTP diferente de 2xx.
     * @throws InterruptedException se a requisição for interrompida.
     */
    public List<Usuario> obtenerUsuarios() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(this.apiUrl))
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            Usuario[] arrayUsuarios = objectMapper.readValue(response.body(), Usuario[].class);
            return Arrays.asList(arrayUsuarios);
        } else {
            throw new IOException("Erro na chamada REST (" + response.statusCode() + "): " + response.body());
        }
    }

    /**
     * Exemplo de envio POST (opcional, para persistência remota).
     */
    public Usuario crearUsuarioRemoto(Usuario usuario) throws IOException, InterruptedException {
        String jsonPayload = objectMapper.writeValueAsString(usuario);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(this.apiUrl))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 201 || response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), Usuario.class);
        } else {
            throw new IOException("Erro ao criar na API (" + response.statusCode() + "): " + response.body());
        }
    }
}