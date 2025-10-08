package com.example.DockerUsageTest.controllers;

import com.example.DockerUsageTest.service.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Controller
@RequestMapping("/s3")
public class S3Controller {
    @Autowired
    private S3Service s3Service;

    @GetMapping("/buckets")
    public String listBuckets(Model model) {
        model.addAttribute("buckets", s3Service.listBuckets());
        return "buckets";
    }

    @PostMapping("/buckets/create")
    public String createBucket(@RequestParam String name) {
        s3Service.createBucket(name);
        return "redirect:/s3/buckets";
    }

    @PostMapping("/buckets/delete")
    public String deleteBucket(@RequestParam String name) {
        s3Service.deleteBucket(name);
        return "redirect:/s3/buckets";
    }


    @GetMapping("/{bucketName}/objects")
    public String listObjects(@PathVariable String bucketName, Model model) {
        model.addAttribute("bucketName", bucketName);
        model.addAttribute("objects", s3Service.listObjects(bucketName));
        return "objects";
    }

    @PostMapping("/{bucketName}/upload")
    public String upload(@PathVariable String bucketName, @RequestParam MultipartFile file) throws IOException {
        s3Service.uploadFile(bucketName, file);
        return "redirect:/s3/" + bucketName + "/objects";
    }

    @PostMapping("/{bucketName}/delete")
    public String delete(@PathVariable String bucketName, @RequestParam String key) {
        s3Service.deleteFile(bucketName, key);
        return "redirect:/s3/" + bucketName + "/objects";
    }
}
