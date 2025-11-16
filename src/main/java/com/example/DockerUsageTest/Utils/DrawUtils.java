package com.example.DockerUsageTest.Utils;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ByteArrayResource;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

public class DrawUtils {
    public byte[] DrawCVOver(ByteArrayResource resource, String jsonResponse){
        try{
            BufferedImage img = ImageIO.read(resource.getInputStream());
            Graphics g = img.getGraphics();

            g.setColor(Color.RED);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonResponse);

            JsonNode labels = root.path("body").get("multiobject_labels").get(0).path("labels");

            for (JsonNode label : labels) {
                JsonNode coords = label.path("coord");
                int x1 = coords.get(0).asInt();
                int y1 = coords.get(1).asInt();
                int x2 = coords.get(2).asInt();
                int y2 = coords.get(3).asInt();

                String text = label.get("eng").asText();

                g.drawRect(x1, y1, x2 - x1, y2 - y1);
                g.drawString(text, x1, y1);
            }

            g.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
