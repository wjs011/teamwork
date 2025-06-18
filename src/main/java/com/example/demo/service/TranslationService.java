package com.example.demo.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class TranslationService {
    
    @Value("${baidu.translate.appid}")
    private String appId;
    
    @Value("${baidu.translate.secret}")
    private String secret;
    
    private static final String BAIDU_TRANSLATE_URL = "https://fanyi-api.baidu.com/api/trans/vip/translate";
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY = 1000; // 1秒

    public String translate(String text, String targetLang) {
        int retries = 0;
        while (retries < MAX_RETRIES) {
            try {
                // 生成随机数
                String salt = String.valueOf(System.currentTimeMillis());
                // 计算签名
                String sign = DigestUtil.md5Hex(appId + text + salt + secret);
                
                // 构建请求参数
                Map<String, Object> params = new HashMap<>();
                params.put("q", text);
                params.put("from", "auto");
                params.put("to", targetLang);
                params.put("appid", appId);
                params.put("salt", salt);
                params.put("sign", sign);
                
                // 发送请求
                String response = HttpUtil.get(BAIDU_TRANSLATE_URL, params);
                
                // 解析响应
                JSONObject json = JSONUtil.parseObj(response);
                if (json.containsKey("trans_result")) {
                    return json.getJSONArray("trans_result")
                            .getJSONObject(0)
                            .getStr("dst");
                } else {
                    String errorMsg = json.getStr("error_msg", "未知错误");
                    if (errorMsg.contains("Access Limit") && retries < MAX_RETRIES - 1) {
                        retries++;
                        Thread.sleep(RETRY_DELAY);
                        continue;
                    }
                    return "翻译失败：" + errorMsg;
                }
            } catch (Exception e) {
                if (retries == MAX_RETRIES - 1) {
                    return "翻译失败：" + e.getMessage();
                }
                retries++;
                try {
                    Thread.sleep(RETRY_DELAY);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        return "翻译失败：多次尝试后仍然无法获取翻译结果";
    }
} 