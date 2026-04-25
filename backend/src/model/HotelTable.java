
package model;

public class HotelTable {
    public int tableId;
    public int capacity;
    public String status;
    public String name;

    public HotelTable(int tableId, String name,  int capacity, String status) {
        this.tableId = tableId;
        this.capacity = capacity;
        this.status = status;
    }
}