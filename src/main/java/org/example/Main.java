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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    private static final String AES_URL = "https://fortnite-api.com/v2/aes";
    private static final String LAST_BUILD_FILE = "last-news.txt";
    private static final Pattern VERSION_PATTERN =
            Pattern.compile("Release-([\\d.]+)-CL-(\\d+)");

    public static void main(String[] args) throws Exception {

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AES_URL))
                .GET()
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response.body());
        String currentBuild = root.path("data").path("build").asText();

        if (currentBuild.isBlank()) {
            System.out.println("ビルド情報が取得できませんでした");
            return;
        }

        String currentVersion = extractVersion(currentBuild);
        String previousBuild = readPreviousBuild();
        String previousVersion = extractVersion(previousBuild);

        if (currentVersion.equals(previousVersion)) {
            System.out.println("アップデートなし: " + currentBuild);
            return;
        }

        sendSlack(currentBuild, previousBuild);

        Files.writeString(Path.of(LAST_BUILD_FILE), currentBuild);

        System.out.println("アップデート通知完了: " + currentBuild);
    }

    private static String extractVersion(String build) {
        Matcher m = VERSION_PATTERN.matcher(build);
        return m.find() ? m.group(1) : build;
    }

    private static String readPreviousBuild() throws IOException {
        Path path = Path.of(LAST_BUILD_FILE);
        if (!Files.exists(path)) return "";
        return Files.readString(path).trim();
    }

    private static void sendSlack(String currentBuild, String previousBuild) throws Exception {

        String webhookUrl = System.getenv("SLACK_WEBHOOK_URL");

        if (webhookUrl == null || webhookUrl.isBlank()) {
            throw new RuntimeException("SLACK_WEBHOOK_URL が設定されていません");
        }

        Matcher m = VERSION_PATTERN.matcher(currentBuild);
        String version = m.find() ? m.group(1) : currentBuild;
        String cl = m.find() ? m.group(2) : "";

        String prevVersion = "";
        if (!previousBuild.isBlank()) {
            Matcher pm = VERSION_PATTERN.matcher(previousBuild);
            if (pm.find()) prevVersion = pm.group(1);
        }

        String updateLine = prevVersion.isBlank()
                ? "バージョン: " + version
                : prevVersion + " → " + version;

        String text = String.format(
                "🎮 Fortniteアップデート！\\n\\n%s\\nビルド: %s",
                escape(updateLine),
                escape(cl.isBlank() ? currentBuild : "CL-" + cl)
        );

        String message = "{\"text\":\"" + text + "\"}";

        HttpRequest slackRequest = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(message))
                .build();

        HttpClient.newHttpClient().send(slackRequest, HttpResponse.BodyHandlers.ofString());
    }

    private static String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }
}
