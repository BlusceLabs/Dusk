package com.dusk.realtime;

import io.javalin.Javalin;
import io.javalin.websocket.WsContext;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Instant;

public class Main {

    record StreamSession(
        String id,
        String hostId,
        String title,
        String category,
        int viewerCount,
        String startedAt,
        boolean isLive
    ) {}

    record PresenceEntry(String userId, String status, long lastSeen) {}

    static final Map<String, StreamSession> activeSessions  = new ConcurrentHashMap<>();
    static final Map<String, PresenceEntry> onlineUsers     = new ConcurrentHashMap<>();
    static final Map<String, Set<WsContext>> streamSockets  = new ConcurrentHashMap<>();
    static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8083"));

        var app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost()));
        });

        // ── Health ──────────────────────────────────────────────────────
        app.get("/health", ctx -> ctx.json(Map.of(
            "status",   "ok",
            "service",  "dusk-realtime",
            "version",  "0.1.0",
            "lang",     "Java 17 + Javalin",
            "streams",  activeSessions.size(),
            "online",   onlineUsers.size()
        )));

        // ── Streams ─────────────────────────────────────────────────────
        app.get("/api/streams", ctx -> {
            var live = activeSessions.values().stream()
                .filter(StreamSession::isLive)
                .toList();
            ctx.json(Map.of("streams", live, "count", live.size()));
        });

        app.post("/api/streams", ctx -> {
            var body = mapper.readValue(ctx.body(), HashMap.class);
            var id   = "stream_" + UUID.randomUUID().toString().substring(0, 8);
            var session = new StreamSession(
                id,
                (String) body.getOrDefault("hostId",   "unknown"),
                (String) body.getOrDefault("title",    "Untitled Stream"),
                (String) body.getOrDefault("category", "General"),
                0,
                Instant.now().toString(),
                true
            );
            activeSessions.put(id, session);
            ctx.status(201).json(session);
        });

        app.get("/api/streams/{id}", ctx -> {
            var session = activeSessions.get(ctx.pathParam("id"));
            if (session == null) { ctx.status(404).json(Map.of("error", "stream not found")); return; }
            ctx.json(session);
        });

        app.delete("/api/streams/{id}", ctx -> {
            var removed = activeSessions.remove(ctx.pathParam("id"));
            if (removed == null) { ctx.status(404).json(Map.of("error", "stream not found")); return; }
            ctx.json(Map.of("ended", true, "id", removed.id()));
        });

        // ── Presence ─────────────────────────────────────────────────────
        app.post("/api/presence/{userId}", ctx -> {
            var userId = ctx.pathParam("userId");
            var body   = mapper.readValue(ctx.body(), HashMap.class);
            var status = (String) body.getOrDefault("status", "online");
            onlineUsers.put(userId, new PresenceEntry(userId, status, System.currentTimeMillis()));
            ctx.json(Map.of("userId", userId, "status", status));
        });

        app.get("/api/presence/{userId}", ctx -> {
            var userId = ctx.pathParam("userId");
            var entry  = onlineUsers.get(userId);
            if (entry == null) {
                ctx.json(Map.of("userId", userId, "status", "offline", "lastSeen", (Object) null));
            } else {
                var staleMs = System.currentTimeMillis() - entry.lastSeen();
                var online  = staleMs < 60_000;
                ctx.json(Map.of(
                    "userId",   userId,
                    "status",   online ? entry.status() : "offline",
                    "lastSeen", entry.lastSeen()
                ));
            }
        });

        app.get("/api/presence", ctx -> {
            var cutoff = System.currentTimeMillis() - 60_000;
            var online = onlineUsers.values().stream()
                .filter(e -> e.lastSeen() > cutoff)
                .map(e -> Map.of("userId", e.userId(), "status", e.status()))
                .toList();
            ctx.json(Map.of("online", online, "count", online.size()));
        });

        // ── WebSocket — live stream chat ──────────────────────────────────
        app.ws("/ws/stream/{id}", ws -> {
            ws.onConnect(ctx -> {
                var streamId = ctx.pathParam("id");
                streamSockets.computeIfAbsent(streamId, k -> ConcurrentHashMap.newKeySet()).add(ctx);
            });
            ws.onMessage(ctx -> {
                var streamId = ctx.pathParam("id");
                var msg      = ctx.message();
                var sockets  = streamSockets.getOrDefault(streamId, Set.of());
                sockets.stream().filter(c -> c != ctx).forEach(c -> {
                    try { c.send(msg); } catch (Exception ignored) {}
                });
            });
            ws.onClose(ctx -> {
                var streamId = ctx.pathParam("id");
                var sockets  = streamSockets.get(streamId);
                if (sockets != null) sockets.remove(ctx);
            });
        });

        app.start("0.0.0.0", port);
        System.out.printf("[Dusk Realtime] Listening on port %d%n", port);
    }
}
