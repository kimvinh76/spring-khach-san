package com.example.demo.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.demo.model.Customer;
import com.example.demo.service.CustomerService;

@Controller
@RequestMapping("/admin/customers")
public class CustomerController {
    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public String listCustomers(Model model) {
        List<Customer> customers = customerService.getAllCustomers();
        model.addAttribute("customers", customers);
        return "admin/customers/list";
    }

    @GetMapping("/{id}")
    public String viewCustomer(@PathVariable Long id, Model model) {
        Customer customer = customerService.getCustomerById(id);
        model.addAttribute("customer", customer);
        return "admin/customers/detail";
    }

    @PostMapping("/delete/{id}")
    public String deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return "redirect:/admin/customers";
    }
}
