package model;

import java.util.*;

public class Order {
    public int orderId;
    Customer customer;
    HotelTable table;
    List<OrderDetail> items = new ArrayList<>();
    Payment payment;

    Order(int orderId, Customer customer, HotelTable table) {
        this.orderId = orderId;
        this.customer = customer;
        this.table = table;
    }

    void addItem(MenuItem item, int quantity) {
        items.add(new OrderDetail(item, quantity));
    }

    void makePayment(Payment payment) {
        this.payment = payment;
    }

    double calculateTotal() {
        double total = 0;
        for (OrderDetail od : items) {
            total += od.getSubtotal();
        }
        return total;
    }

    void showOrder() {
        System.out.println("Order ID: " + orderId);
        System.out.println("Customer: " + customer.name);
        System.out.println("Table: " + table.tableId);

        for (OrderDetail od : items) {
            System.out.println(od.item.name + " x " + od.quantity);
        }

        System.out.println("Total: " + calculateTotal());

        if (payment != null) {
            System.out.println("Payment: " + payment.method + " (" + payment.status + ")");
        }
    }
}