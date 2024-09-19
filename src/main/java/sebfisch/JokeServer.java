package sebfisch;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import sebfisch.concurrent.Server;

public class JokeServer extends Server<String, String> {
    public JokeServer(Executor executor) {
        super(executor, JokeServer::handle);
    }

    static String handle(String input)
            throws URISyntaxException, IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder().uri(uri(input)).build();
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("HTTP error: %d".formatted(response.statusCode()));
        }
        return response.body();
    }

    private static URI uri(String topic) throws MalformedURLException, URISyntaxException {
        return new URI("https://v2.jokeapi.dev/joke/Any?contains=%s"
                .formatted(URLEncoder.encode(topic, Charset.defaultCharset())));
    }

    public static void main(String[] args) {
        ExecutorService executor = Executors.newCachedThreadPool();
        Server<String, String> server = new JokeServer(executor);
        server.serve("java").thenAccept(System.out::println).join();
        executor.shutdown();
    }
}
