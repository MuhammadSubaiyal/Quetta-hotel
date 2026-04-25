


import dao.*;
import java.util.*;

public class Main {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        try {
            // 1️⃣ Select customer
            System.out.println("Enter Customer ID:");
            int customerId = sc.nextInt();

            // 2️⃣ Select table
            System.out.println("Enter Table ID:");
            int tableId = sc.nextInt();

            // 3️⃣ Create order
            int orderId = OrderDAO.createOrder(customerId, tableId);
            if (orderId == 0) {
                System.out.println("Failed to create order.");
                return;
            }   

            // 4️⃣ Add items
            double totalAmount = 0;
            boolean adding = true;

            while (adding) {
                System.out.println("\nMenu:");
                List<MenuDAO.MenuItem> menu = MenuDAO.getAllMenuItems();
                for (MenuDAO.MenuItem item : menu) {
                    System.out.println(item.id + ". " + item.name + " - Rs " + item.price);
                }

                System.out.println("Enter Item ID to add:");
                int itemId = sc.nextInt();
                System.out.println("Enter Quantity:");
                int qty = sc.nextInt();

                double subtotal = OrderDetailsDAO.addOrderDetail(orderId, itemId, qty);
                totalAmount += subtotal;

                System.out.println("Add more items? (y/n)");
                adding = sc.next().equalsIgnoreCase("y");
            }

            // 5️⃣ Update total
            OrderDAO.updateTotalAmount(orderId, totalAmount);

            // 6️⃣ Print bill
            System.out.println("\n==== BILL ====");
            System.out.println("Order ID: " + orderId);
            System.out.println("Customer ID: " + customerId);
            System.out.println("Table ID: " + tableId);
            System.out.println("Total Amount: Rs " + totalAmount);

        } catch (Exception e) {
            e.printStackTrace();
        }

        sc.close();
    }
}