package com.example.DockerUsageTest.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class MainController {
    @Value("${app.message}")
    private String message;

    @GetMapping("/")
    public String Main(Model model) {
        model.addAttribute("title", "Hello World");
        model.addAttribute("message", message);
        return "index";
    }
}
