package com.example.demo.controller;


import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.example.demo.common.Result;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/files")
public class FileController {
    @Value("${server.port}")
    private String port;
    private static final String ip = "http://localhost";
    //上传接口
    @CrossOrigin(origins = "http://localhost:9876")
    @PostMapping("/upload")
    public Result<?> uploadFile(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        String flag= IdUtil.simpleUUID();

        String rootPath = System.getProperty("user.dir") + "/src/main/resources/files/" + flag + '_' + fileName;
        FileUtil.writeBytes(file.getBytes(),rootPath);
        return Result.success(ip+ ":"+port + "/files/"+flag);
    }

    @GetMapping("/{flag}")
    public void getFile(HttpServletResponse response, @PathVariable String flag) throws IOException {
        // 1. 定义文件存储根路径
        String rootPath = System.getProperty("user.dir") + "/src/main/resources/files/";

        // 2. 查找匹配的文件
        List<String> fileNames = FileUtil.listFileNames(rootPath);
        String fileName = fileNames.stream()
                .filter(name -> name.contains(flag))
                .findAny()
                .orElse(null);

        // 3. 如果文件不存在，返回404
        if (fileName == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("File not found");
            return;
        }

        // 4. 动态设置Content-Type
        String contentType = getContentType(fileName);
        response.setContentType(contentType);

        // 5. 设置Content-Disposition
        String contentDisposition = contentType.startsWith("image/")
                ? "inline;filename="
                : "attachment;filename=";
        response.addHeader("Content-Disposition",
                contentDisposition + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

        // 6. 读取文件并写入响应流
        try (OutputStream os = response.getOutputStream()) {
            byte[] fileBytes = FileUtil.readBytes(rootPath + fileName);
            os.write(fileBytes);
            os.flush();
        } catch (IOException e) {
            // 7. 处理异常
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Failed to read file: " + e.getMessage());
        }
    }

    // 辅助方法：根据文件名获取Content-Type
    private String getContentType(String filename) {
        // 手动提取文件扩展名
        int lastDotIndex = filename.lastIndexOf('.');
        String extension = lastDotIndex > 0 ? filename.substring(lastDotIndex + 1).toLowerCase() : "";

        // 根据扩展名返回 Content-Type
        return switch (extension) {
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "pdf" -> "application/pdf";
            case "txt" -> "text/plain";
            default -> "application/octet-stream";
        };
    }
}
