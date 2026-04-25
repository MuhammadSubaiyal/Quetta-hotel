package model;

public class OrderDetail {
    MenuItem item;
    int quantity;

    OrderDetail(MenuItem item, int quantity) {
        this.item = item;
        this.quantity = quantity;
    }

    double getSubtotal() {
        return item.price * quantity;
    }
}
