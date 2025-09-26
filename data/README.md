# StockMarketSimulatorThreaded

A Java project that simulates a real-time stock market feed using multithreading and buffered file I/O.

## 🧠 Features
- `MarketDataGenerator` thread: generates tick data
- `TradingDashboard` thread: reads and displays live stats
- Uses `BufferedReader`/`BufferedWriter`
- ASCII mini-chart + moving average (SMA)

## 💻 How to Run

```bash
javac -d out src/StockMarketSimulatorThreaded.java
java -cp out StockMarketSimulatorThreaded
