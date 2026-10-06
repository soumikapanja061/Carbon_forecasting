import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.InetSocketAddress;
import java.util.*;

public class CarbonWebServer {
    private static Map<Integer, Double> carbonData = new TreeMap<>();
    private static String dataStatus = "Checking data...";

    public static void main(String[] args) throws IOException {
        // UPDATE THIS PATH to where your file actually is
        String path = "C:\\Users\\Soumika\\Downloads\\Carbon_(CO2)_Emissions_by_Country.csv";
        loadCSV(path);

        HttpServer server = HttpServer.create(new InetSocketAddress(9000), 0);
        server.createContext("/predict", new PredictHandler());

        System.out.println("SERVER STARTING...");
        System.out.println("Go to: http://localhost:9000/predict");

        server.setExecutor(null);
        server.start();
    }

    static class PredictHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String query = t.getRequestURI().getQuery();
            StringBuilder htmlResponse = new StringBuilder();

            htmlResponse.append("<html><head><style>")
                    .append("body { font-family: sans-serif; text-align: center; background: #f0f2f5; padding-top: 50px; }")
                    .append(".card { background: white; padding: 30px; border-radius: 10px; display: inline-block; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }")
                    .append(".btn { padding: 10px 20px; background: #28a745; color: white; border: none; border-radius: 5px; cursor: pointer; }")
                    .append(".error { color: red; font-weight: bold; }")
                    .append("</style></head><body><div class='card'>");

            if (query == null || !query.contains("year=")) {
                htmlResponse.append("<h1>CO2 Forecast Tool</h1>")
                        .append("<p>Status: ").append(dataStatus).append("</p>")
                        .append("<form action='/predict' method='get'>")
                        .append("Enter Year: <input type='number' name='year' value='2030'> ")
                        .append("<input type='submit' class='btn' value='Predict'>")
                        .append("</form>");
            } else {
                try {
                    int year = Integer.parseInt(query.split("=")[1]);
                    if (carbonData.isEmpty()) {
                        htmlResponse.append("<p class='error'>Error: CSV data not found at the specified path!</p>");
                    } else {
                        double prediction = calculateRegression(year);
                        htmlResponse.append("<h1>Year: ").append(year).append("</h1>")
                                .append("<h2 style='color:#007bff;'>").append(String.format("%,.2f", prediction)).append(" kt</h2>");
                    }
                } catch (Exception e) {
                    htmlResponse.append("<p class='error'>Invalid year entered.</p>");
                }
                htmlResponse.append("<br><a href='/predict'>Try Again</a>");
            }

            htmlResponse.append("</div></body></html>");
            byte[] bytes = htmlResponse.toString().getBytes();
            t.sendResponseHeaders(200, bytes.length);
            t.getResponseBody().write(bytes);
            t.getResponseBody().close();
        }
    }

    private static void loadCSV(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            dataStatus = "FILE NOT FOUND at: " + fileName;
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            br.readLine(); // Skip header
            String line;
            while ((line = br.readLine()) != null) {
                String[] v = line.split(",");
                if (v.length < 4) continue;

                // Handling your specific Date format: 01-01-2011
                String[] dateParts = v[2].split("-");
                int year = Integer.parseInt(dateParts[dateParts.length - 1]);
                double emissions = Double.parseDouble(v[3]);

                carbonData.put(year, carbonData.getOrDefault(year, 0.0) + emissions);
            }
            dataStatus = "Successfully loaded " + carbonData.size() + " years of data.";
        } catch (Exception e) {
            dataStatus = "Error reading file: " + e.getMessage();
        }
    }

    private static double calculateRegression(int targetYear) {
        int n = carbonData.size();
        if (n < 2) return 0.0;
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        for (Map.Entry<Integer, Double> entry : carbonData.entrySet()) {
            double x = entry.getKey();
            double y = entry.getValue();
            sumX += x; sumY += y; sumXY += (x * y); sumX2 += (x * x);
        }
        double m = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        double c = (sumY - m * sumX) / n;
        return m * targetYear + c;
    }
}