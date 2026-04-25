import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import db.DBConnection;

public class Server {

    public static void main(String[] args) throws Exception {

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // ================= MENU API =================
        server.createContext("/menu", (HttpExchange exchange) -> {

            String response;

            try (Connection con = DBConnection.getConnection()) {

                StringBuilder sb = new StringBuilder();
                sb.append("[");

                Statement stmt = con.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM Menu_Items");

                while (rs.next()) {
                    sb.append("{")
                      .append("\"id\":").append(rs.getInt("item_id")).append(",")
                      .append("\"name\":\"").append(rs.getString("name")).append("\",")
                      .append("\"price\":").append(rs.getDouble("price")).append(",")
                      .append("\"category\":\"").append(rs.getString("category")).append("\"")
                      .append("},");
                }

                if (sb.length() > 1 && sb.charAt(sb.length() - 1) == ',') {
                    sb.deleteCharAt(sb.length() - 1);
                }

                sb.append("]");
                response = sb.toString();

            } catch (Exception e) {
                e.printStackTrace();
                response = "{\"error\":\"" + e.getMessage() + "\"}";
                send(exchange, response, 500);
                return;
            }

            send(exchange, response, 200);
        });

        // ================= TABLES API =================
        server.createContext("/tables", (HttpExchange exchange) -> {

            String response;

            try (Connection con = DBConnection.getConnection()) {

                StringBuilder sb = new StringBuilder();
                sb.append("[");

                Statement stmt = con.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM Hotel_Tables");

                while (rs.next()) {
                    sb.append("{")
                      .append("\"id\":").append(rs.getInt("table_id")).append(",")
                      .append("\"name\":\"").append(rs.getString("name")).append("\",")
                      .append("\"status\":\"").append(rs.getString("status")).append("\"")
                      .append("},");
                }

                if (sb.length() > 1 && sb.charAt(sb.length() - 1) == ',') {
                    sb.deleteCharAt(sb.length() - 1);
                }

                sb.append("]");
                response = sb.toString();

            } catch (Exception e) {
                e.printStackTrace();
                response = "{\"error\":\"" + e.getMessage() + "\"}";
                send(exchange, response, 500);
                return;
            }

            send(exchange, response, 200);
        });

        // ================= ORDER API =================
        server.createContext("/order", (HttpExchange exchange) -> {

            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, OPTIONS");

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                System.out.println("ORDER RECEIVED: " + body);

                Connection con = DBConnection.getConnection();

                // ===== EXTRACT DATA =====
                String name = extractName(body);
                String phone = extractPhone(body);
                int tableId = extractTableId(body);
                double total = extractDouble(body, "total");

                // ===== CHECK TABLE EXISTS =====
                if (!tableExists(con, tableId)) {
                    throw new SQLException("Table ID " + tableId + " does not exist");
                }

                // ===== INSERT CUSTOMER =====
                PreparedStatement custPs = con.prepareStatement(
                        "INSERT INTO Customers (name, phone) VALUES (?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                );

                custPs.setString(1, name);
                custPs.setString(2, phone);
                custPs.executeUpdate();

                ResultSet custRs = custPs.getGeneratedKeys();
                int customerId = 0;
                if (custRs.next()) customerId = custRs.getInt(1);

                // ===== INSERT ORDER =====
                PreparedStatement orderPs = con.prepareStatement(
                        "INSERT INTO Orders (customer_id, table_id, total_amount) VALUES (?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                );

                orderPs.setInt(1, customerId);
                orderPs.setInt(2, tableId);
                orderPs.setDouble(3, total);
                orderPs.executeUpdate();

                ResultSet orderRs = orderPs.getGeneratedKeys();
                int orderId = 0;
                if (orderRs.next()) orderId = orderRs.getInt(1);

                // ===== INSERT ORDER DETAILS =====
                Pattern itemPattern = Pattern.compile(
                        "\\{[^}]*\"id\"\\s*:\\s*(\\d+)[^}]*\"qty\"\\s*:\\s*(\\d+)[^}]*\"price\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)",
                        Pattern.DOTALL
                );

                Matcher matcher = itemPattern.matcher(body);

                while (matcher.find()) {
                    int itemId = Integer.parseInt(matcher.group(1));
                    int qty = Integer.parseInt(matcher.group(2));
                    double price = Double.parseDouble(matcher.group(3));

                    double subtotal = qty * price;

                    PreparedStatement ps = con.prepareStatement(
                            "INSERT INTO Order_Details (order_id, item_id, quantity, subtotal) VALUES (?, ?, ?, ?)"
                    );

                    ps.setInt(1, orderId);
                    ps.setInt(2, itemId);
                    ps.setInt(3, qty);
                    ps.setDouble(4, subtotal);
                    ps.executeUpdate();
                }

                // ===== UPDATE TABLE STATUS =====
                PreparedStatement tablePs = con.prepareStatement(
                         "UPDATE Hotel_Tables SET status='Occupied', occupied_at=NOW() WHERE table_id=?"
                );
                tablePs.setInt(1, tableId);
                tablePs.executeUpdate();

                System.out.println("✅ ORDER SAVED → ID: " + orderId);

                send(exchange, "{\"status\":\"success\",\"orderId\":" + orderId + "}", 200);

            } catch (Exception e) {
                e.printStackTrace();
                send(exchange, "{\"error\":\"" + e.getMessage() + "\"}", 500);
            }
        });

        server.start();
        System.out.println("🚀 Server running on http://localhost:8080");
        new Thread(() -> {
    while (true) {
        try (Connection con = DBConnection.getConnection()) {

            String sql =
                "UPDATE Hotel_Tables " +
                "SET status = 'Available', occupied_at = NULL " +
                "WHERE status = 'Occupied' " +
                "AND TIMESTAMPDIFF(MINUTE, occupied_at, NOW()) >= 1";

            PreparedStatement ps = con.prepareStatement(sql);
            int updated = ps.executeUpdate();

            if (updated > 0) {
                System.out.println("♻️ Freed tables: " + updated);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            Thread.sleep(10000); // check every 10 seconds
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("🔁 Cleanup thread is running...");
    }
}).start();
    }

    // ================= HELPERS =================

    private static String extractName(String body) throws SQLException {
        Pattern p = Pattern.compile("\"customer\"\\s*:\\s*\\{[^}]*\"name\"\\s*:\\s*\"([^\"]+)\"");
        Matcher m = p.matcher(body);
        if (m.find()) return m.group(1);
        throw new SQLException("Missing name");
    }

    private static String extractPhone(String body) throws SQLException {
        Pattern p = Pattern.compile("\"customer\"\\s*:\\s*\\{[^}]*\"phone\"\\s*:\\s*\"([^\"]+)\"");
        Matcher m = p.matcher(body);
        if (m.find()) return m.group(1);
        throw new SQLException("Missing phone");
    }

    private static int extractTableId(String body) throws SQLException {
        Pattern p = Pattern.compile("\"table\"\\s*:\\s*\\{[^}]*\"id\"\\s*:\\s*(\\d+)");
        Matcher m = p.matcher(body);
        if (m.find()) return Integer.parseInt(m.group(1));
        throw new SQLException("Missing table id");
    }

    private static double extractDouble(String body, String field) throws SQLException {
        Pattern p = Pattern.compile("\"" + field + "\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)");
        Matcher m = p.matcher(body);
        if (m.find()) return Double.parseDouble(m.group(1));
        throw new SQLException("Missing " + field);
    }

    private static boolean tableExists(Connection con, int tableId) throws SQLException {
        PreparedStatement ps = con.prepareStatement("SELECT 1 FROM Hotel_Tables WHERE table_id=?");
        ps.setInt(1, tableId);
        ResultSet rs = ps.executeQuery();
        return rs.next();
    }

    private static void send(HttpExchange exchange, String response, int code) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");

        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}