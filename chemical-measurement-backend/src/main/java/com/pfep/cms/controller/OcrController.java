// Copyright (c) 2026 郑杭宇. All rights reserved.
// Licensed under the MIT License. See LICENSE file.

package com.pfep.cms.controller;

import com.pfep.cms.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.*;

@Tag(name = "OCR识别")
@RestController
@RequestMapping("/api/ocr")
@RequiredArgsConstructor
public class OcrController {

    private static final String UPLOAD_DIR = "uploads/";

    @Operation(summary = "上传照片并识别")
    @PostMapping("/recognize")
    public Result<Map<String, Object>> recognize(@RequestParam("file") MultipartFile file) {
        try {
            // 安全校验
            String original = file.getOriginalFilename();
            if (original == null || !original.matches("(?i).*\\.(jpg|jpeg|png|bmp)$")) {
                return Result.error("仅支持 jpg/png/bmp 图片格式");
            }
            if (file.getSize() > 5 * 1024 * 1024) {
                return Result.error("文件大小不能超过5MB");
            }

            // 保存到绝对路径
            File dir = new File(System.getProperty("user.dir"), UPLOAD_DIR);
            if (!dir.exists()) dir.mkdirs();
            String filename = System.currentTimeMillis() + "_" + sanitizeFilename(original);
            File dest = new File(dir, filename);
            file.transferTo(dest);

            // TODO: 对接真实OCR API（百度OCR/阿里云OCR/PaddleOCR）
            // String ocrResult = callOcrApi(dest);
            // Map<String, Object> parsedData = parseOcrResult(ocrResult);

            // 当前返回模拟数据，展示数据流
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("photoUrl", "/uploads/" + filename);
            result.put("ocrRaw", "模拟OCR结果：检测日期=2026-07-26 浓度=2.5% pH=9.2");
            result.put("confidence", 0.92);

            List<Map<String, Object>> fields = new ArrayList<>();
            Map<String, Object> f1 = new LinkedHashMap<>();
            f1.put("fieldName", "检测日期");
            f1.put("value", java.time.LocalDate.now().toString());
            f1.put("confidence", 0.98);
            fields.add(f1);

            Map<String, Object> f2 = new LinkedHashMap<>();
            f2.put("fieldName", "浓度");
            f2.put("value", "2.5");
            f2.put("unit", "%");
            f2.put("confidence", 0.85);
            fields.add(f2);

            Map<String, Object> f3 = new LinkedHashMap<>();
            f3.put("fieldName", "pH值");
            f3.put("value", "9.2");
            f3.put("confidence", 0.93);
            fields.add(f3);

            result.put("fields", fields);
            return Result.success(result);

        } catch (Exception e) {
            return Result.error("识别失败: " + e.getMessage());
        }
    }

    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    // ====== 以下为对接真实OCR API的预留模板 ======

    /*
    private String callOcrApi(File imageFile) throws Exception {
        // 示例：百度OCR手写体识别
        // String accessToken = getAccessToken();
        // String base64 = Base64.getEncoder().encodeToString(Files.readAllBytes(imageFile.toPath()));
        // 
        // OkHttpClient client = new OkHttpClient();
        // RequestBody body = new FormBody.Builder()
        //     .add("image", base64)
        //     .add("language_type", "CHN_ENG")
        //     .build();
        // Request request = new Request.Builder()
        //     .url("https://aip.baidubce.com/rest/2.0/ocr/v1/handwriting?access_token=" + accessToken)
        //     .post(body)
        //     .build();
        // Response response = client.newCall(request).execute();
        // return response.body().string();
        return "";
    }

    private Map<String, Object> parseOcrResult(String json) {
        // 解析OCR返回的JSON，按字段名提取检测值
        // 匹配逻辑：日期正则 (20\d{2}-\d{2}-\d{2})、数值正则 (\d+\.?\d*)
        // 结合字段位置关系判断每个数值对应哪个指标
        return new LinkedHashMap<>();
    }
    */
}
