package com.example.demo.service;

import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

@Service
public class JdUrlParserService {
    public String getProductIdFromUrl(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();

            if (host.contains("item.jd.com")) {
                return Arrays.stream(uri.getPath().split("/"))
                        .filter(part -> part.endsWith(".html") && part.replace(".html", "").matches("\\d+"))
                        .map(part -> part.replace(".html", ""))
                        .findFirst()
                        .orElse(null);
            }
            return null;
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("无效的URL: " + url);
        }
    }
}