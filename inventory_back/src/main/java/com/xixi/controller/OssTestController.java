package com.xixi.controller;

import com.xixi.service.OssService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/oss/test")
public class OssTestController {
    @Autowired
    private OssService ossService;


    @PostMapping("/upload")
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file)throws IOException {
        String objectKey=ossService.upload(file,"supplier");

        Map<String,Object> result=new HashMap<>();
        result.put("fileName",file.getOriginalFilename());
        result.put("objectKey",objectKey);
        result.put("size",file.getSize());
        return result;

    }
}
