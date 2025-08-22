package com.example.demo.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.example.demo.model.ServiceOrder;

public class AggregatedOrder {
    private String serviceName;
    private int totalQuantity;
    private double totalAmount;
    private List<ServiceOrder> orders = new ArrayList<>();
    private Map<String, Integer> statusCounts = new LinkedHashMap<>();

    public AggregatedOrder() {}

    public AggregatedOrder(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(int totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public List<ServiceOrder> getOrders() {
        return orders;
    }

    public void setOrders(List<ServiceOrder> orders) {
        this.orders = orders;
    }

    public Map<String, Integer> getStatusCounts() {
        return statusCounts;
    }

    public void setStatusCounts(Map<String, Integer> statusCounts) {
        this.statusCounts = statusCounts;
    }

    public void addOrder(ServiceOrder o) {
        if (o == null) return;
        this.orders.add(o);
        this.totalQuantity += o.getQuantity() != null ? o.getQuantity() : 0;
        this.totalAmount += o.getTotalAmount() != null ? o.getTotalAmount() : 0;
        String st = o.getStatus() == null ? "N/A" : o.getStatus();
        this.statusCounts.merge(st, 1, Integer::sum);
    }
}
