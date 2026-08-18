package videoforge.youtube;

import org.json.JSONObject;
import videoforge.config.AppConfig;
import videoforge.logging.AppLog;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * YouTube Data API v3 integration using the device authorization flow (no
 * server required). The user creates their own OAuth client ID in Google Cloud,
 * pastes it in the Setup window, and the app exchanges it for a refresh token
 * stored locally in {@code config/oauth.json}. Videos are uploaded with the
 * resumable upload protocol, reporting real progress.
 */
public final class YouTubeManager {

    private static final AppLog LOG = AppLog.get("youtube");
    private static final String SCOPE = "https://www.googleapis.com/auth/youtube.upload";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    private JSONObject tokens;
    private long accessExpiryMs;

    public YouTubeManager() {
        loadTokens();
    }

    // ======================================================================
    //  Device authorization flow
    // ======================================================================

    /** Starts the device flow; returns the pair the user must visit & enter. */
    public DeviceCode beginDeviceAuthorization(String clientId) throws IOException, InterruptedException {
        if (clientId == null || clientId.isBlank()) {
            throw new IOException("No OAuth client ID configured. Open YouTube > Setup and add one.");
        }
        JSONObject body = new JSONObject();
        body.put("client_id", clientId);
        body.put("scope", SCOPE);
        HttpRequest req = post("https://oauth2.googleapis.com/device/code", body.toString(), "application/json", null);
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() / 100 != 2) {
            throw new IOException("Device code request failed (" + res.statusCode() + "): " + res.body());
        }
        JSONObject json = new JSONObject(res.body());
        DeviceCode code = new DeviceCode();
        code.deviceCode = json.getString("device_code");
        code.userCode = json.getString("user_code");
        code.verificationUrl = json.getString("verification_url");
        code.expiresIn = json.optInt("expires_in", 1800);
        code.interval = json.optInt("interval", 5);
        this.pendingDeviceCode = code.deviceCode;
        this.pendingClientId = clientId;
        return code;
    }

    private String pendingDeviceCode;
    private String pendingClientId;

    /** Polls the token endpoint until the user authorizes or the flow expires. */
    public boolean pollForAuthorization() throws IOException, InterruptedException {
        if (pendingDeviceCode == null) {
            return false;
        }
        long deadline = System.currentTimeMillis() + 30 * 60 * 1000L;
        int interval = 5;
        while (System.currentTimeMillis() < deadline) {
            JSONObject body = new JSONObject();
            body.put("client_id", pendingClientId);
            body.put("device_code", pendingDeviceCode);
            body.put("grant_type", "urn:ietf:params:oauth:grant-type:device_code");
            HttpResponse<String> res = http.send(
                    post("https://oauth2.googleapis.com/token", body.toString(), "application/json", null),
                    HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() / 100 == 2) {
                JSONObject tok = new JSONObject(res.body());
                tokens = new JSONObject();
                tokens.put("access_token", tok.getString("access_token"));
                tokens.put("refresh_token", tok.getString("refresh_token"));
                tokens.put("expires_in", tok.optInt("expires_in", 3600));
                accessExpiryMs = System.currentTimeMillis() + tok.optInt("expires_in", 3600) * 1000L;
                saveTokens();
                pendingDeviceCode = null;
                pendingClientId = null;
                return true;
            }
            JSONObject err = new JSONObject(res.body());
            String error = err.optString("error");
            if ("authorization_pending".equals(error)) {
                Thread.sleep(TimeUnit.SECONDS.toMillis(interval));
                continue;
            }
            if ("slow_down".equals(error)) {
                interval += 5;
                Thread.sleep(TimeUnit.SECONDS.toMillis(interval));
                continue;
            }
            throw new IOException("Authorization failed: " + error + " (" + res.body() + ")");
        }
        return false;
    }

    public boolean isAuthenticated() {
        return tokens != null && tokens.has("refresh_token");
    }

    public String accountLabel() {
        return tokens != null && tokens.has("access_token") ? "Signed in" : "Not signed in";
    }

    private String accessToken() throws IOException, InterruptedException {
        if (tokens == null || !tokens.has("refresh_token")) {
            throw new IOException("Not authenticated. Open YouTube > Setup and authorize the app.");
        }
        if (System.currentTimeMillis() < accessExpiryMs) {
            return tokens.getString("access_token");
        }
        JSONObject body = new JSONObject();
        body.put("client_id", pendingClientIdForRefresh());
        body.put("refresh_token", tokens.getString("refresh_token"));
        body.put("grant_type", "refresh_token");
        HttpResponse<String> res = http.send(
                post("https://oauth2.googleapis.com/token", body.toString(), "application/json", null),
                HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() / 100 != 2) {
            throw new IOException("Token refresh failed (" + res.statusCode() + "): " + res.body());
        }
        JSONObject tok = new JSONObject(res.body());
        tokens.put("access_token", tok.getString("access_token"));
        tokens.put("expires_in", tok.optInt("expires_in", 3600));
        accessExpiryMs = System.currentTimeMillis() + tok.optInt("expires_in", 3600) * 1000L;
        saveTokens();
        return tokens.getString("access_token");
    }

    private String pendingClientIdForRefresh() throws IOException {
        String id = clientId();
        if (id == null || id.isBlank()) {
            throw new IOException("OAuth client ID is not configured.");
        }
        return id;
    }

    // ======================================================================
    //  Upload (resumable)
    // ======================================================================

    public interface UploadListener {
        void onProgress(double percent);
        void onMessage(String message);
    }

    public static final class UploadResult {
        public boolean ok;
        public String videoId = "";
        public String url = "";
        public String message = "";
    }

    public UploadResult upload(Path file, String title, String description, List<String> tags,
                               String privacy, UploadListener listener) throws IOException, InterruptedException {
        UploadResult result = new UploadResult();
        if (!Files.exists(file)) {
            result.message = "Video file not found: " + file;
            return result;
        }
        String token = accessToken();

        JSONObject meta = new JSONObject();
        JSONObject snippet = new JSONObject();
        snippet.put("title", title == null || title.isBlank() ? "Untitled" : title);
        snippet.put("description", description == null ? "" : description);
        if (tags != null && !tags.isEmpty()) {
            snippet.put("tags", tags);
        }
        snippet.put("categoryId", "22"); // People & Blogs
        JSONObject status = new JSONObject();
        status.put("privacyStatus", privacy == null || privacy.isBlank() ? "private" : privacy);
        status.put("selfDeclaredMadeForKids", false);
        meta.put("snippet", snippet);
        meta.put("status", status);

        listener.onMessage("Initiating upload session...");
        HttpRequest init = HttpRequest.newBuilder()
                .uri(URI.create("https://www.googleapis.com/upload/youtube/v3/videos?uploadType=resumable&part=snippet,status"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("X-Upload-Content-Type", "video/*")
                .POST(HttpRequest.BodyPublishers.ofString(meta.toString()))
                .build();
        HttpResponse<String> initRes = http.send(init, HttpResponse.BodyHandlers.ofString());
        if (initRes.statusCode() / 100 != 2) {
            result.message = "Upload session failed (" + initRes.statusCode() + "): " + initRes.body();
            return result;
        }
        String uploadUri = initRes.headers().firstValue("Location").orElseThrow(
                () -> new IOException("No upload URL in response"));

        long size = Files.size(file);
        long bytesSent = 0;
        listener.onMessage("Uploading...");
        try (var in = Files.newInputStream(file)) {
            byte[] chunk = new byte[8 * 1024 * 1024];
            int read;
            while ((read = in.read(chunk)) > 0) {
                HttpRequest put = HttpRequest.newBuilder()
                        .uri(URI.create(uploadUri))
                        .header("Authorization", "Bearer " + token)
                        .header("Content-Type", "video/*")
                        .header("Content-Length", String.valueOf(read))
                        .header("Content-Range", "bytes " + bytesSent + "-" + (bytesSent + read - 1) + "/" + size)
                        .PUT(HttpRequest.BodyPublishers.ofByteArray(chunk, 0, read))
                        .build();
                HttpResponse<String> res = http.send(put, HttpResponse.BodyHandlers.ofString());
                bytesSent += read;
                listener.onProgress(bytesSent * 100.0 / size);
                if (res.statusCode() / 100 == 2) {
                    JSONObject finalRes = new JSONObject(res.body());
                    result.ok = true;
                    result.videoId = finalRes.optString("id");
                    result.url = "https://youtu.be/" + result.videoId;
                    result.message = "Upload complete";
                    listener.onMessage("Upload complete: " + result.url);
                    return result;
                }
                if (res.statusCode() == 308) {
                    continue; // resume point accepted, keep sending
                }
                if (res.statusCode() / 100 == 4) {
                    result.message = "Upload rejected (" + res.statusCode() + "): " + res.body();
                    return result;
                }
                // 5xx -> retry once
                listener.onMessage("Server error, retrying...");
                Thread.sleep(1500);
                in.close();
                return upload(file, title, description, tags, privacy, listener);
            }
        }
        result.message = "Upload ended before completion";
        return result;
    }

    // ======================================================================
    //  Config persistence
    // ======================================================================

    private void loadTokens() {
        try {
            Path f = AppConfig.get().oauthConfigFile();
            if (Files.exists(f)) {
                JSONObject o = new JSONObject(Files.readString(f, StandardCharsets.UTF_8));
                if (o.has("tokens")) {
                    tokens = o.getJSONObject("tokens");
                    accessExpiryMs = o.optLong("expiry", 0);
                }
            }
        } catch (Exception e) {
            LOG.warn("Could not load OAuth tokens: " + e.getMessage());
        }
    }

    private void saveTokens() {
        try {
            Path f = AppConfig.get().oauthConfigFile();
            JSONObject o = new JSONObject();
            o.put("tokens", tokens);
            o.put("expiry", accessExpiryMs);
            Files.writeString(f, o.toString(2), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.warn("Could not save OAuth tokens: " + e.getMessage());
        }
    }

    public String clientId() {
        return AppConfig.get().getString("youtubeClientId");
    }

    public void setClientId(String clientId) {
        AppConfig.get().put("youtubeClientId", clientId);
        AppConfig.get().save();
    }

    public void signOut() {
        tokens = null;
        accessExpiryMs = 0;
        try {
            Files.deleteIfExists(AppConfig.get().oauthConfigFile());
        } catch (IOException ignored) {
        }
    }

    private static HttpRequest post(String url, String body, String contentType, String auth) {
        var b = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", contentType)
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (auth != null) {
            b.header("Authorization", auth);
        }
        return b.build();
    }

    public static final class DeviceCode {
        public String deviceCode;
        public String userCode;
        public String verificationUrl;
        public int expiresIn;
        public int interval;
    }
}
