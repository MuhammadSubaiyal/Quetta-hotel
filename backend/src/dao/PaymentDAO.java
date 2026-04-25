package dao;

import db.DBConnection;

import java.sql.*;

public class PaymentDAO {

    public static void addPayment(int orderId, String method, String status) {
        try {
            Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO Payments(order_id, payment_method, payment_status) VALUES (?, ?, ?)"
            );
            ps.setInt(1, orderId);
            ps.setString(2, method);
            ps.setString(3, status);
            ps.executeUpdate();
            System.out.println("Payment added!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}