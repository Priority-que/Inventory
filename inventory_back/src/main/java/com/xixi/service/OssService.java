package com.xixi.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface OssService {
    String upload(MultipartFile file, String bizType)throws IOException;
}
