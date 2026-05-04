package dao;

import db.DBConnection;
import java.sql.*;

public class OrderDAO {

    // Create order and return generated order_id
    public static int createOrder(int customerId, int tableId) {
        int orderId = 0;
        try {
            Connection con = DBConnection.getConnection();
            String sql = "INSERT INTO Orders(customer_id, table_id, total_amount) VALUES (?, ?, 0)";
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, customerId);
            ps.setInt(2, tableId);
            ps.executeUpdate();

            
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                orderId = rs.getInt(1);  // Get the auto-generated order_id
            }
            ps.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return orderId;
    }

    // Update total_amount in Orders table after adding items
    public static void updateTotalAmount(int orderId, double total) {
        try {
            Connection con = DBConnection.getConnection();
            String sql = "UPDATE Orders SET total_amount=? WHERE order_id=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setDouble(1, total);
            ps.setInt(2, orderId);
            ps.executeUpdate();
            ps.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}