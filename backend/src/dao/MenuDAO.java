package dao;

import db.DBConnection;
import java.sql.*;
import java.util.*;

public class MenuDAO {

    public static class MenuItem {
        public int id;
        public String name;
        public double price;

        public MenuItem(int id, String name, double price) {
            this.id = id;
            this.name = name;
            this.price = price;
        }
    }

    public static List<MenuItem> getAllMenuItems() {
        List<MenuItem> menu = new ArrayList<>();
        try {
            Connection con = DBConnection.getConnection();
            String sql = "SELECT item_id, name, price FROM Menu_Items";
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                menu.add(new MenuItem(
                    rs.getInt("item_id"),
                    rs.getString("name"),
                    rs.getDouble("price")
                ));
            }
            ps.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return menu;
    }
}