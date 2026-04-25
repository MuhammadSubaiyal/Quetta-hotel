package dao;

import db.DBConnection;

import java.sql.*;
import java.util.*;
import model.HotelTable;

public class HotelTableDAO {

    public static List<HotelTable> getAllTables() {
        List<HotelTable> list = new ArrayList<>();
        try {
            Connection con = DBConnection.getConnection();
            ResultSet rs = con.createStatement().executeQuery("SELECT * FROM Hotel_Tables");

            while (rs.next()) {
                list.add(new HotelTable(
                    rs.getInt("table_id"),
                    rs.getString("name"),
                    rs.getInt("capacity"),
                    rs.getString("status")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}