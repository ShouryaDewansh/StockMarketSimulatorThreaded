import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class TradingDashboard {
    private static final Path IN_FILE = Path.of("data", "ticks.ndjson");
    private static final int WINDOW = 20; // last N prices for chart + SMA

    // Track rolling windows per symbol
    private static class Series {
        Deque<Double> prices = new ArrayDeque<>();
        double sum = 0.0;

        void add(double p) {
            prices.addLast(p);
            sum += p;
            while (prices.size() > WINDOW) {
                sum -= prices.removeFirst();
            }
        }

        double sma() {
            if (prices.isEmpty()) return Double.NaN;
            return sum / prices.size();
        }

        String miniChart() {
            if (prices.isEmpty()) return "(no data)";
            double min = prices.stream().min(Double::compare).orElse(0.0);
            double max = prices.stream().max(Double::compare).orElse(0.0);
            double range = Math.max(1e-9, max - min);

            StringBuilder sb = new StringBuilder();
            for (double p : prices) {
                // scale to 0..10
                int level = (int) Math.round(((p - min) / range) * 10);
                level = Math.max(0, Math.min(10, level));
                sb.append("▁▂▃▄▅▆▇█".charAt(Math.min(7, level / 2)));
            }
            return sb.toString();
        }
    }

    public static void main(String[] args) {
        System.out.println("Watching file: " + IN_FILE.toAbsolutePath());
        long processedLines = 0;
        Map<String, Series> perSymbol = new HashMap<>();

        // Loop: reopen the file, skip already processed lines, read any new ones, sleep, repeat.
        while (true) {
            try (BufferedReader reader = Files.newBufferedReader(IN_FILE)) {
                // Skip lines we've already processed
                for (long i = 0; i < processedLines; i++) {
                    if (reader.readLine() == null) break;
                }

                String line;
                long newLines = 0;
                while ((line = reader.readLine()) != null) {
                    newLines++;
                    // Very small JSON parse: we only need symbol and price
                    // Expect: {"ts":"...","symbol":"ABC","price":123.4567,"volume":123}
                    String symbol = extract(line, "\"symbol\":\"", "\"");
                    String priceStr = extract(line, "\"price\":", ",");
                    if (priceStr == null) {
                        // last field may be at end before }
                        priceStr = extract(line, "\"price\":", "}");
                    }

                    if (symbol != null && priceStr != null) {
                        double px;
                        try { px = Double.parseDouble(priceStr); }
                        catch (NumberFormatException e) { continue; }

                        Series s = perSymbol.computeIfAbsent(symbol, k -> new Series());
                        s.add(px);
                    }
                }
                processedLines += newLines;

                // Clear the console-ish
                System.out.print("\033[H\033[2J");
                System.out.flush();

                System.out.println("Trading Dashboard   " + Instant.now());
                System.out.println("(Reading: " + IN_FILE.toAbsolutePath() + ")");
                System.out.println("--------------------------------------------------");
                for (var entry : perSymbol.entrySet()) {
                    String sym = entry.getKey();
                    Series s = entry.getValue();
                    double last = s.prices.isEmpty() ? Double.NaN : s.prices.peekLast();
                    double sma = s.sma();
                    System.out.printf("%-6s | %-30s | last: %.4f  SMA(%d): %.4f%n",
                            sym, s.miniChart(), last, WINDOW, sma);
                }
                System.out.println("--------------------------------------------------");
                System.out.println("Tip: Start the writer first. This reader will auto-refresh.");

                try { Thread.sleep(700); } catch (InterruptedException ignored) {}
            } catch (IOException e) {
                // If file not found yet, just wait
                System.out.println("Waiting for file... (" + e.getMessage() + ")");
                try { Thread.sleep(700); } catch (InterruptedException ignored) {}
            }
        }
    }

    // Tiny helper to extract substring between a start key and the next end token.
    private static String extract(String src, String startKey, String endToken) {
        int a = src.indexOf(startKey);
        if (a < 0) return null;
        int b = src.indexOf(endToken, a + startKey.length());
        if (b < 0) return null;
        return src.substring(a + startKey.length(), b);
    }
}
