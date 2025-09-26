import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

public class StockMarketSimulatorThreaded {

    static final Path FILE_PATH = Path.of("data", "ticks.ndjson");
    static final String[] SYMBOLS = {"ABC", "XYZ"};
    static final int WINDOW = 20;

    public static void main(String[] args) {
        new File("data").mkdirs(); // Ensure data/ folder exists

        Thread writerThread = new Thread(() -> {
            Random rng = new Random();
            double[] prices = {100.0, 200.0};

            while (true) {
                try (BufferedWriter writer = Files.newBufferedWriter(FILE_PATH,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                    for (int s = 0; s < SYMBOLS.length; s++) {
                        prices[s] += rng.nextGaussian() * 0.5;
                        double price = Math.max(1.0, prices[s]);

                        String tick = String.format(
                            "{\"ts\":\"%s\",\"symbol\":\"%s\",\"price\":%.4f,\"volume\":%d}\n",
                            Instant.now().toString(), SYMBOLS[s], price, 100 + rng.nextInt(900)
                        );
                        writer.write(tick);
                    }
                    writer.flush();
                    Thread.sleep(500);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        Thread readerThread = new Thread(() -> {
            Map<String, Deque<Double>> symbolData = new HashMap<>();
            long seenLines = 0;

            while (true) {
                try (BufferedReader reader = Files.newBufferedReader(FILE_PATH)) {
                    for (long i = 0; i < seenLines; i++) {
                        if (reader.readLine() == null) break;
                    }

                    String line;
                    long newLines = 0;
                    while ((line = reader.readLine()) != null) {
                        newLines++;

                        String symbol = extract(line, "\"symbol\":\"", "\"");
                        String priceStr = extract(line, "\"price\":", ",");

                        if (symbol == null || priceStr == null) continue;

                        double price = Double.parseDouble(priceStr);
                        Deque<Double> q = symbolData.computeIfAbsent(symbol, k -> new ArrayDeque<>());
                        q.addLast(price);
                        if (q.size() > WINDOW) q.removeFirst();
                    }

                    seenLines += newLines;

                    // Clear screen
                    System.out.print("\033[H\033[2J");
                    System.out.flush();
                    System.out.println("📈 Trading Dashboard  |  " + Instant.now());
                    for (var entry : symbolData.entrySet()) {
                        String sym = entry.getKey();
                        Deque<Double> prices = entry.getValue();
                        double sma = prices.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                        String chart = miniChart(prices);
                        System.out.printf("%-6s | %s | SMA(%d): %.2f%n", sym, chart, WINDOW, sma);
                    }

                    Thread.sleep(700);
                } catch (IOException | InterruptedException e) {
                    System.out.println("Waiting for file... " + e.getMessage());
                    try { Thread.sleep(700); } catch (InterruptedException ignored) {}
                }
            }
        });

        writerThread.start();
        readerThread.start();
    }

    static String extract(String line, String start, String end) {
        int a = line.indexOf(start);
        if (a < 0) return null;
        int b = line.indexOf(end, a + start.length());
        if (b < 0) return null;
        return line.substring(a + start.length(), b);
    }

    static String miniChart(Deque<Double> prices) {
        if (prices.isEmpty()) return "(no data)";
        double min = prices.stream().min(Double::compare).orElse(0.0);
        double max = prices.stream().max(Double::compare).orElse(0.0);
        double range = Math.max(1e-6, max - min);

        StringBuilder sb = new StringBuilder();
        for (double p : prices) {
            int level = (int) Math.round(((p - min) / range) * 10);
            level = Math.max(0, Math.min(10, level));
            sb.append("▁▂▃▄▅▆▇█".charAt(Math.min(7, level / 2)));
        }
        return sb.toString();
    }
}
