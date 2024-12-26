package net.hywave.auth.utils;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import net.hywave.auth.Auth;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("restriction")

public class WebServer {
    private static HttpServer server;
    private static final Map<String, List<String>> backupCodeMap = new ConcurrentHashMap<>();
    private static final int PORT = 8099;
    
    public static void initialize(Auth plugin) {
        try {
            server = HttpServer.create(new InetSocketAddress(
                plugin.getConfig().getInt("web-server.port", PORT)), 0);
                
            server.createContext("/backup_codes/", new BackupCodesHandler());
            server.setExecutor(null);
            server.start();
            
            Logger.info("Web server started on port " + PORT);
        } catch (IOException e) {
            Logger.error("Failed to start web server", e);
        }
    }
    
    public static String generateBackupCodesUrl(UUID playerUuid, List<String> codes) {
        String secretKey = UUID.randomUUID().toString().replace("-", "");
        backupCodeMap.put(secretKey, codes);
        
        Auth.getInstance().getServer().getScheduler().runTaskLater(
            Auth.getInstance(),
            () -> backupCodeMap.remove(secretKey),
            72000L // 1 saat tick
        );
        
        return "http://localhost:" + PORT + "/backup_codes/" + secretKey;
    }
    
    static class BackupCodesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String secretKey = path.substring(path.lastIndexOf('/') + 1);
            
            List<String> codes = backupCodeMap.get(secretKey);
            if (codes == null) {
                String response = "Backup codes not found or expired";
                exchange.sendResponseHeaders(404, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                return;
            }
            
            StringBuilder response = new StringBuilder();
            response.append("Your Backup Codes:\n\n");
            for (String code : codes) {
                response.append(code).append("\n");
            }
            
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            byte[] responseBytes = response.toString().getBytes();
            exchange.sendResponseHeaders(200, responseBytes.length);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
    }
    
    public static void shutdown() {
        if (server != null) {
            server.stop(0);
            Logger.info("Web server stopped");
        }
    }
} 