package com.example.DockerUsageTest.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class MainController {

    @GetMapping("/")
    public String Main(Model model) {
        return "redirect:/s3/buckets";
    }
}
