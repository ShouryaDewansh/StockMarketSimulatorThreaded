import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Random;

public class MarketDataGenerator {
    // Writes newline-delimited JSON ticks to data/ticks.ndjson using BufferedWriter
    public static void main(String[] args) {
        String[] symbols = {"ABC", "XYZ"};
        long tickIntervalMs = 500; // time between ticks
        int totalTicks = 200;      // how many lines to write

        Path outFile = Path.of("data", "ticks.ndjson");
        Random rng = new Random();

        // simple prices per symbol
        double[] price = {100.0, 200.0};

        // Ensure parent folder exists
        try {
            Files.createDirectories(outFile.getParent());
        } catch (IOException e) {
            System.err.println("Could not create data directory: " + e.getMessage());
            System.exit(1);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(
                outFile,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND)) {

            System.out.println("Writing ticks to " + outFile.toAbsolutePath());

            for (int i = 0; i < totalTicks; i++) {
                for (int s = 0; s < symbols.length; s++) {
                    // random small move
                    double move = (rng.nextGaussian() * 0.2);
                    price[s] = Math.max(0.01, price[s] + move);

                    String line = String.format(
                        "{\"ts\":\"%s\",\"symbol\":\"%s\",\"price\":%.4f,\"volume\":%d}\n",
                        Instant.now().toString(), symbols[s], price[s], 100 + rng.nextInt(900)
                    );

                    writer.write(line);
                }
                writer.flush(); // ensure the reader can see new data
                try { Thread.sleep(tickIntervalMs); } catch (InterruptedException ignored) {}
            }

            System.out.println("Done. Wrote " + totalTicks + " ticks per symbol.");
        } catch (IOException e) {
            System.err.println("Error writing ticks: " + e.getMessage());
        }
    }
}
