package com.example.controller;

import com.example.pojo.Result;
import com.example.utils.AliOssUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class FileUploadController {
    @Autowired
    private AliOssUtil aliOssUtil;
    @PostMapping("/upload")
    public Result<String> upload(MultipartFile file) throws Exception {
        String originalFile = file.getOriginalFilename();
        String fileName = System.currentTimeMillis()+originalFile.substring(originalFile.lastIndexOf('.'));
        String url = aliOssUtil.uploadFile(fileName,file.getInputStream());
        return Result.success(url);
    }
}
