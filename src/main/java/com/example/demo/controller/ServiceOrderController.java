package com.example.demo.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.demo.model.ServiceOrder;
import com.example.demo.service.ServiceOrderService;

@Controller
@RequestMapping("/admin/service-orders")
public class ServiceOrderController {
    private final ServiceOrderService serviceOrderService;

    public ServiceOrderController(ServiceOrderService serviceOrderService) {
        this.serviceOrderService = serviceOrderService;
    }

    @GetMapping
    public String listServiceOrders(Model model) {
        List<ServiceOrder> orders = serviceOrderService.getAllServiceOrders();
        model.addAttribute("orders", orders);
        return "admin/serviceorders/list";
    }

    @GetMapping("/{id}")
    public String viewServiceOrder(@PathVariable Long id, Model model) {
        ServiceOrder order = serviceOrderService.getServiceOrderById(id);
        model.addAttribute("order", order);
        return "admin/serviceorders/detail";
    }

    @PostMapping("/delete/{id}")
    public String deleteServiceOrder(@PathVariable Long id) {
        serviceOrderService.deleteServiceOrder(id);
        return "redirect:/admin/service-orders";
    }
}
