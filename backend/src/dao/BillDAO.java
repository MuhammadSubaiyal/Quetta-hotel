package dao;

import db.DBConnection;

import java.sql.*;

public class BillDAO {

    public static void printBill(int orderId) {
        try {
            Connection con = DBConnection.getConnection();

            // 1️⃣ Get Order Info
            PreparedStatement psOrder = con.prepareStatement(
                "SELECT o.order_id, o.total_amount, o.order_date, " +
                "c.name AS customer_name, t.table_id " +
                "FROM Orders o " +
                "JOIN Customers c ON o.customer_id = c.customer_id " +
                "JOIN Hotel_Tables t ON o.table_id = t.table_id " +
                "WHERE o.order_id = ?"
            );

            psOrder.setInt(1, orderId);
            ResultSet rsOrder = psOrder.executeQuery();

            if (!rsOrder.next()) {
                System.out.println("Order not found!");
                return;
            }

            // 🧾 HEADER
            System.out.println("\n====================================");
            System.out.println("         QUETTA HOTEL BILL          ");
            System.out.println("====================================");
            System.out.println("Order ID : " + rsOrder.getInt("order_id"));
            System.out.println("Customer : " + rsOrder.getString("customer_name"));
            System.out.println("Table    : " + rsOrder.getInt("table_id"));
            System.out.println("Date     : " + rsOrder.getTimestamp("order_date"));
            System.out.println("------------------------------------");

            // 🧾 COLUMN HEADERS
            System.out.printf("%-12s %-5s %-7s %-8s\n", "Item", "Qty", "Price", "Total");
            System.out.println("------------------------------------");

            // 2️⃣ Get Order Items
            PreparedStatement psItems = con.prepareStatement(
                "SELECT m.name, od.quantity, m.price, od.subtotal " +
                "FROM Order_Details od " +
                "JOIN Menu_Items m ON od.item_id = m.item_id " +
                "WHERE od.order_id = ?"
            );

            psItems.setInt(1, orderId);
            ResultSet rsItems = psItems.executeQuery();

            // 🧾 ITEMS
            while (rsItems.next()) {
                System.out.printf("%-12s %-5d %-7.0f %-8.0f\n",
                        rsItems.getString("name"),
                        rsItems.getInt("quantity"),
                        rsItems.getDouble("price"),
                        rsItems.getDouble("subtotal"));
            }

            System.out.println("------------------------------------");

            // 🧾 TOTAL
            System.out.printf("%-24s %-8.0f\n", "TOTAL:", rsOrder.getDouble("total_amount"));

            System.out.println("====================================");
            System.out.println("      Thank you! Visit again 😊     ");
            System.out.println("====================================\n");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}