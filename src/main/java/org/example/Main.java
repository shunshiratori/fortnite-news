package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {

    private static final String NEWS_URL =
            "https://fortnite-api.com/v2/news";

    private static final String LAST_NEWS_FILE =
            "last-news.txt";

    public static void main(String[] args) throws Exception {

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(NEWS_URL))
                .GET()
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        ObjectMapper mapper = new ObjectMapper();

        JsonNode root = mapper.readTree(response.body());

        JsonNode news =
                root.path("data")
                        .path("br")
                        .path("motds")
                        .get(0);

        String title = news.path("title").asText();
        String body = news.path("body").asText();

        String currentId = title;

        String previousId = readLastNews();

        if (currentId.equals(previousId)) {
            System.out.println("新着なし");
            return;
        }

        sendSlack(title, body);

        Files.writeString(
                Path.of(LAST_NEWS_FILE),
                currentId
        );

        System.out.println("通知完了");
    }

    private static String readLastNews() throws IOException {

        Path path = Path.of(LAST_NEWS_FILE);

        if (!Files.exists(path)) {
            return "";
        }

        return Files.readString(path).trim();
    }

    private static void sendSlack(
            String title,
            String body
    ) throws Exception {

        String webhookUrl =
                System.getenv("SLACK_WEBHOOK_URL");

        if (webhookUrl == null || webhookUrl.isBlank()) {
            throw new RuntimeException(
                    "SLACK_WEBHOOK_URL が設定されていません"
            );
        }

        String message =
                """
                {
                  "text":"🎮 Fortnite更新通知\\n\\nタイトル: %s\\n\\n%s"
                }
                """
                        .formatted(
                                escape(title),
                                escape(body)
                        );

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(message))
                .build();

        client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );
    }

    private static String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}