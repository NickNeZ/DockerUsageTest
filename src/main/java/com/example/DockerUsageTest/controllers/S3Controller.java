package com.example.DockerUsageTest.controllers;

import com.example.DockerUsageTest.Utils.DrawUtils;
import com.example.DockerUsageTest.service.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;


@Controller
@RequestMapping("/s3")
public class S3Controller {
    @Value("${CVISION_TOKEN}")
    private String cvtoken;

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

    @GetMapping("/{bucket}/{key}")
    public ResponseEntity<byte[]> getObject(@PathVariable String bucket, @PathVariable("key") String key) {

        String decodedKey = URLDecoder.decode(key, StandardCharsets.UTF_8);

        try {
            ResponseInputStream<GetObjectResponse> s3Object =
                    s3Service.getObject(bucket, decodedKey);

            byte[] bytes = s3Object.readAllBytes();
            String contentType = s3Object.response().contentType();

            MediaType mediaType = (contentType != null)
                    ? MediaType.parseMediaType(contentType)
                    : MediaType.APPLICATION_OCTET_STREAM;

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            mediaType.getType().equals("image") || mediaType.equals(MediaType.TEXT_PLAIN)
                                    ? "inline"
                                    : "attachment")
                    .body(bytes);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(("Ошибка при открытии файла: " + e.getMessage()).getBytes());
        }
    }

    @PostMapping("{bucket}/{key}/recognize")
    public String recognize(@PathVariable String bucket, @PathVariable String key) {
        String decodedKey = URLDecoder.decode(key, StandardCharsets.UTF_8);
        ResponseInputStream<GetObjectResponse> s3Object = s3Service.getObject(bucket, decodedKey);
        RestTemplate restTemplate = new RestTemplate();
        String URL = "https://smarty.mail.ru/api/v1/objects/detect?oauth_token=" + cvtoken + "&oauth_provider=mcs";
        try{
            ByteArrayResource fileResource = new ByteArrayResource(s3Object.readAllBytes()){
                @Override
                public String getFilename() {
                    return decodedKey;
                }
            };

            MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
            parts.add("file", fileResource);

            String metaJson = """
                {
                  "mode": ["multiobject"],
                  "images": [{"name": "file"}]
                }
                """;
            parts.add("meta", metaJson);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(parts, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    URL,
                    HttpMethod.POST,
                    request, String.class
            );
            //System.out.println(response.getBody());


            DrawUtils drawUtils = new DrawUtils();
            byte[] drawing = drawUtils.DrawCVOver(fileResource, response.getBody());
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    decodedKey.substring(0,decodedKey.lastIndexOf('.')) + "_analyzed" + ".png",
                    "image/png",
                    drawing
            );

            s3Service.uploadFile(bucket, file);



        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return "redirect:/s3/" + bucket + "/objects";
    }
}
