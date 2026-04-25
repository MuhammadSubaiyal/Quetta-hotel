package dao;

import db.DBConnection;

import java.sql.*;
import java.util.*;
import model.MenuItem;

public class MenuItemDAO {

    public static void addItem(String name, String category, double price) {
        try {
            Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO Menu_Items(name, category, price) VALUES (?, ?, ?)"
            );
            ps.setString(1, name);
            ps.setString(2, category);
            ps.setDouble(3, price);
            ps.executeUpdate();
            System.out.println("Item added!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<MenuItem> getAllItems() {
        List<MenuItem> list = new ArrayList<>();
        try {
            Connection con = DBConnection.getConnection();
            ResultSet rs = con.createStatement().executeQuery("SELECT * FROM Menu_Items");

            while (rs.next()) {
                list.add(new MenuItem(
                    rs.getInt("item_id"),
                    rs.getString("name"),
                    rs.getString("category"),
                    rs.getDouble("price")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}