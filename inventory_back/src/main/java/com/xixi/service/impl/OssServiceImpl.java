package com.xixi.service.impl;

import com.aliyun.oss.OSS;
import com.xixi.config.OssProperties;
import com.xixi.service.OssService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

import static cn.hutool.core.io.FileUtil.getSuffix;


@Service
public class OssServiceImpl implements OssService{
    private  final OSS ossClient;
    private final OssProperties ossProperties;

    public OssServiceImpl(OSS ossClient,OssProperties ossProperties){
        this.ossClient=ossClient;
        this.ossProperties=ossProperties;
    }

    @Override
    public String upload(MultipartFile file, String bizType)throws IOException {
        if(file==null||file.isEmpty()){
            throw new RuntimeException("上传文件不能为空");
        }
        String originalFilename=file.getOriginalFilename();
        String suffix=getSuffix(originalFilename);

        String uuid= UUID.randomUUID().toString().replace("-", "");
        String datePath= LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));

        String objectKey=buildObjectKey(bizType,datePath,uuid+"."+suffix);

        try(InputStream inputStream=file.getInputStream()){
            ossClient.putObject(ossProperties.getBucketName(),objectKey,inputStream);
        }
        return objectKey;
    }

    private String buildObjectKey(String bizType,String datePath,String fileName){
        String baseDir=ossProperties.getBaseDir();

        if(baseDir==null||baseDir.trim().isEmpty()){
            return bizType+"/"+datePath+"/"+fileName;
        }

        baseDir=baseDir.replace("\\","/");
        if(baseDir.endsWith("/")){
            baseDir=baseDir.substring(0,baseDir.length()-1);

        }
        return baseDir+"/"+bizType+"/"+datePath+"/"+fileName;
    }
}