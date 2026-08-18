package ids;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;

/** Client for the local Datacenter ID socket. Raw stored IDs are never returned. */
public final class IdSocketClient {
    private static final int MAX_RESPONSE_BYTES = 4_096;
    private final Path socketPath;

    public IdSocketClient(Path dataRoot) {
        Path root = Objects.requireNonNull(dataRoot, "dataRoot").toAbsolutePath().normalize();
        socketPath = root.resolve("ids").resolve(IdSocketServer.SOCKET_NAME);
    }

    public boolean ping() throws IOException {
        String[] response = request("PING");
        return response.length == 2 && "PONG".equals(response[1]);
    }

    public Token requestToken(String applicationName) throws IOException {
        validateField(applicationName, "application name");
        String[] response = request("TOKEN\t" + applicationName);
        if (response.length != 3) throw new IOException("Invalid token response from ID broker");
        long epoch;
        try {
            epoch = Long.parseLong(response[2]);
        } catch (NumberFormatException exception) {
            throw new IOException("Invalid token expiration from ID broker", exception);
        }
        return new Token(response[1], Instant.ofEpochSecond(epoch));
    }

    /** Returns a normalized application key, never the raw value in .id.txt. */
    public String resolveApplication(String token) throws IOException {
        validateField(token, "token");
        String[] response = request("IDENTITY\t" + token);
        if (response.length != 2) throw new IOException("Invalid identity response from ID broker");
        return response[1];
    }

    public void revoke(String token) throws IOException {
        validateField(token, "token");
        String[] response = request("REVOKE\t" + token);
        if (response.length != 2 || !"REVOKED".equals(response[1])) {
            throw new IOException("ID broker did not revoke the token");
        }
    }

    private String[] request(String request) throws IOException {
        try (SocketChannel channel = SocketChannel.open(StandardProtocolFamily.UNIX)) {
            channel.connect(UnixDomainSocketAddress.of(socketPath));
            BufferedOutputStream output = new BufferedOutputStream(Channels.newOutputStream(channel));
            output.write(request.getBytes(StandardCharsets.UTF_8));
            output.write('\n');
            output.flush();

            String response = readLine(channel);
            String[] parts = response.split("\\t", -1);
            if (parts.length < 2) throw new IOException("Invalid response from ID broker");
            if ("ERR".equals(parts[0])) throw new IOException("ID broker denied request: " + parts[1]);
            if (!"OK".equals(parts[0])) throw new IOException("Unknown response from ID broker");
            return parts;
        }
    }

    private static String readLine(SocketChannel channel) throws IOException {
        BufferedInputStream input = new BufferedInputStream(Channels.newInputStream(channel));
        byte[] bytes = new byte[MAX_RESPONSE_BYTES];
        int length = 0;
        while (length < bytes.length) {
            int next = input.read();
            if (next < 0 || next == '\n') break;
            if (next == '\r') continue;
            bytes[length++] = (byte) next;
        }
        if (length == bytes.length) throw new IOException("ID broker response is too large");
        return new String(bytes, 0, length, StandardCharsets.UTF_8);
    }

    private static void validateField(String value, String name) {
        if (value == null || value.isBlank() || value.indexOf('\t') >= 0
                || value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0) {
            throw new IllegalArgumentException("Invalid " + name);
        }
    }

    public static final class Token {
        private final String value;
        private final Instant expires;
        Token(String value, Instant expires) {
            this.value = value;
            this.expires = expires;
        }
        public String value() { return value; }
        public Instant expires() { return expires; }
        public boolean expired() { return expires.isBefore(Instant.now()); }
        @Override public String toString() { return "Token[expires=" + expires + "]"; }
    }
}
