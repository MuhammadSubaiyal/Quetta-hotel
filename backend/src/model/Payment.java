package model;

public class Payment {
    public int paymentId;
    public String method;
    public String status;

    Payment(int paymentId, String method, String status) {
        this.paymentId = paymentId;
        this.method = method;
        this.status = status;
    }
}