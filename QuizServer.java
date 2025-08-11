package com.example.quizgame;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.*;

public class QuizServer {
    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getProperty("port", "8082"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/", exchange -> {
            String raw = exchange.getRequestURI().getPath();
            String path = raw.equals("/") ? "index.html" : raw.substring(1); // strip leading '/'
            System.out.println("Request path: " + raw);

            // Try these locations in order:
            Path[] candidates = new Path[] {
                    Paths.get(System.getProperty("user.dir"), "public", path),                 // <project>/public/...
                    Paths.get(System.getProperty("user.dir"), "src", "public", path),          // <project>/src/public/...
                    Paths.get(System.getProperty("user.dir"), "src", "main", "resources", "public", path) // resources/public/...
            };

            Path found = null;
            for (Path p : candidates) {
                System.out.println("Check: " + p.toAbsolutePath());
                if (Files.exists(p) && !Files.isDirectory(p)) { found = p; break; }
            }

            try {
                if (found != null) {
                    byte[] bytes = Files.readAllBytes(found);
                    Headers h = exchange.getResponseHeaders();
                    h.add("Content-Type", contentType(path));
                    h.add("Access-Control-Allow-Origin", "*");
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
                    System.out.println("200 OK -> " + found.toAbsolutePath());
                } else {
                    byte[] bytes = ("Not Found. Searched above paths for: " + path).getBytes();
                    exchange.sendResponseHeaders(404, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
                    System.out.println("404 Not Found for: " + path);
                }
            } catch (IOException e) {
                byte[] bytes = ("Server error: " + e.getMessage()).getBytes();
                exchange.sendResponseHeaders(500, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
                e.printStackTrace();
            }
        });

        server.setExecutor(null);
        System.out.println("Quiz server running at http://localhost:" + port);
        server.start();
    }

    static String contentType(String path) {
        String lower = path.toLowerCase();
        if (lower.endsWith(".html")) return "text/html; charset=utf-8";
        if (lower.endsWith(".js"))   return "text/javascript; charset=utf-8";
        if (lower.endsWith(".css"))  return "text/css; charset=utf-8";
        if (lower.endsWith(".png"))  return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".svg"))  return "image/svg+xml";
        if (lower.endsWith(".json")) return "application/json; charset=utf-8";
        return "application/octet-stream";
    }
}
