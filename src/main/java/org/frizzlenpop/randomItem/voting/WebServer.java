package org.frizzlenpop.randomItem.voting;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class WebServer {

    private final VotingManager votingManager;
    private final int port;
    private HttpServer server;

    public WebServer(VotingManager votingManager, int port) {
        this.votingManager = votingManager;
        this.port = port;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", this::handlePage);
        server.createContext("/api/status", this::handleStatus);
        server.createContext("/api/vote", this::handleVote);
        server.setExecutor(null);
        server.start();
    }

    public void stop() {
        if (server != null) server.stop(0);
    }

    private void handlePage(HttpExchange exchange) throws IOException {
        byte[] response = HTML_PAGE.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    private void handleStatus(HttpExchange exchange) throws IOException {
        VotingManager.VoteStatus status = votingManager.getStatus();
        String json = buildStatusJson(status);
        byte[] response = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    private void handleVote(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        String ip = exchange.getRemoteAddress().getAddress().getHostAddress();
        String json;

        if (query != null && query.startsWith("option=")) {
            try {
                int option = Integer.parseInt(query.substring(7));
                boolean success = votingManager.castVote(ip, option);
                json = "{\"success\":" + success + "}";
            } catch (NumberFormatException e) {
                json = "{\"success\":false,\"error\":\"invalid option\"}";
            }
        } else {
            json = "{\"success\":false,\"error\":\"missing option parameter\"}";
        }

        byte[] response = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    private String buildStatusJson(VotingManager.VoteStatus status) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"active\":").append(status.active());
        sb.append(",\"timeRemaining\":").append(status.timeRemainingMs());
        sb.append(",\"options\":[");

        List<VoteOption> options = status.options();
        int[] counts = status.voteCounts();
        for (int i = 0; i < options.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("{\"description\":\"").append(escapeJson(options.get(i).getDescription())).append("\"");
            sb.append(",\"votes\":").append(i < counts.length ? counts[i] : 0).append("}");
        }

        sb.append("]}");
        return sb.toString();
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private static final String HTML_PAGE = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>RandomItem - Vote</title>
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body {
                        background: linear-gradient(135deg, #0f0c29, #302b63, #24243e);
                        color: #eee;
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                        min-height: 100vh;
                        display: flex;
                        justify-content: center;
                        padding: 20px;
                    }
                    .container { max-width: 500px; width: 100%; }
                    h1 {
                        text-align: center;
                        font-size: 2em;
                        background: linear-gradient(90deg, #f39c12, #e74c3c, #9b59b6);
                        -webkit-background-clip: text;
                        -webkit-text-fill-color: transparent;
                        margin: 20px 0 10px;
                    }
                    .subtitle { text-align: center; color: #888; margin-bottom: 20px; }
                    .timer {
                        text-align: center;
                        font-size: 2.5em;
                        font-weight: bold;
                        color: #f39c12;
                        background: rgba(255,255,255,0.05);
                        padding: 15px;
                        border-radius: 12px;
                        margin: 15px 0;
                        border: 1px solid rgba(243,156,18,0.3);
                    }
                    .option {
                        background: rgba(255,255,255,0.05);
                        border: 2px solid rgba(255,255,255,0.1);
                        border-radius: 12px;
                        padding: 18px;
                        margin: 12px 0;
                        cursor: pointer;
                        transition: all 0.3s ease;
                        position: relative;
                        overflow: hidden;
                    }
                    .option:hover { border-color: #f39c12; transform: translateY(-2px); box-shadow: 0 5px 20px rgba(243,156,18,0.2); }
                    .option:active { transform: scale(0.98); }
                    .option .desc { font-size: 1.1em; font-weight: 600; margin-bottom: 8px; }
                    .option .votes { color: #f39c12; font-size: 0.9em; }
                    .option .bar {
                        position: absolute;
                        bottom: 0; left: 0;
                        height: 3px;
                        background: linear-gradient(90deg, #f39c12, #e74c3c);
                        transition: width 0.5s ease;
                    }
                    .voted { opacity: 0.6; pointer-events: none; border-color: #27ae60 !important; }
                    .voted::after { content: '\\2714 VOTED'; position: absolute; top: 10px; right: 10px; color: #27ae60; font-size: 0.8em; font-weight: bold; }
                    .inactive { text-align: center; color: #666; font-size: 1.2em; padding: 60px 20px; }
                    .inactive .waiting { font-size: 3em; margin-bottom: 15px; }
                    .total { text-align: center; color: #888; margin-top: 15px; font-size: 0.9em; }
                    .footer { text-align: center; color: #444; margin-top: 30px; font-size: 0.8em; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>RANDOMITEM</h1>
                    <p class="subtitle">Vote to change the game!</p>
                    <div id="timer" class="timer"></div>
                    <div id="options"></div>
                    <div id="total" class="total"></div>
                    <div class="footer">Powered by RandomItem Plugin</div>
                </div>
                <script>
                    let hasVoted = false;
                    async function refresh() {
                        try {
                            const res = await fetch('/api/status');
                            const data = await res.json();
                            const timer = document.getElementById('timer');
                            const options = document.getElementById('options');
                            const total = document.getElementById('total');
                            if (!data.active) {
                                timer.textContent = 'WAITING...';
                                options.innerHTML = '<div class="inactive"><div class="waiting">\\u23F3</div>Next vote round starting soon...</div>';
                                total.textContent = '';
                                hasVoted = false;
                                return;
                            }
                            const secs = Math.max(0, Math.floor(data.timeRemaining / 1000));
                            const m = Math.floor(secs / 60);
                            const s = String(secs % 60).padStart(2, '0');
                            timer.textContent = m + ':' + s;
                            const totalVotes = data.options.reduce((sum, o) => sum + o.votes, 0);
                            options.innerHTML = '';
                            data.options.forEach((opt, i) => {
                                const pct = totalVotes > 0 ? Math.round(opt.votes / totalVotes * 100) : 0;
                                const div = document.createElement('div');
                                div.className = 'option' + (hasVoted ? ' voted' : '');
                                div.innerHTML = '<div class="desc">' + opt.description + '</div>' +
                                    '<div class="votes">' + opt.votes + ' votes (' + pct + '%)</div>' +
                                    '<div class="bar" style="width:' + pct + '%"></div>';
                                div.onclick = () => vote(i);
                                options.appendChild(div);
                            });
                            total.textContent = 'Total votes: ' + totalVotes;
                        } catch(e) {}
                    }
                    async function vote(index) {
                        if (hasVoted) return;
                        try {
                            const res = await fetch('/api/vote?option=' + index, { method: 'POST' });
                            const data = await res.json();
                            if (data.success) hasVoted = true;
                        } catch(e) {}
                        refresh();
                    }
                    setInterval(refresh, 2000);
                    refresh();
                </script>
            </body>
            </html>
            """;
}
