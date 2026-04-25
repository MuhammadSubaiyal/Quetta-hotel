package dao;

import db.DBConnection;
import java.sql.*;

public class OrderDetailsDAO {

    // Add an item to order and return subtotal
    public static double addOrderDetail(int orderId, int itemId, int quantity) {
        double subtotal = 0;
        try {
            Connection con = DBConnection.getConnection();

            // Get price of item
            String priceSql = "SELECT price FROM Menu_Items WHERE item_id=?";
            PreparedStatement priceStmt = con.prepareStatement(priceSql);
            priceStmt.setInt(1, itemId);
            ResultSet rs = priceStmt.executeQuery();
            if (rs.next()) {
                double price = rs.getDouble("price");
                subtotal = price * quantity;

                // Insert into order_details
                String sql = "INSERT INTO Order_Details(order_id, item_id, quantity, subtotal) VALUES (?, ?, ?, ?)";
                PreparedStatement ps = con.prepareStatement(sql);
                ps.setInt(1, orderId);
                ps.setInt(2, itemId);
                ps.setInt(3, quantity);
                ps.setDouble(4, subtotal);
                ps.executeUpdate();
                ps.close();
            }
            priceStmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return subtotal;
    }
}