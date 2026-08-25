package com.example.utils;

import com.aliyun.oss.*;
import com.aliyun.oss.common.auth.CredentialsProvider;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.comm.SignVersion;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
public class AliOssUtil {
    @Value("${aliyun.oss.endpoint}")
    private String ENDPOINT;
    @Value("${aliyun.oss.access-key-id}")
    private String ACEESS_KEY_ID;
    @Value("${aliyun.oss.access-key-secret}")
    private String ACEESS_KEY_SECRET;
    @Value("${aliyun.oss.bucket-name}")
    private String BUCKET_NAME;
    @Value("${aliyun.oss.region}")
    private String REGIOM;

    public String uploadFile(String objectName, InputStream in) throws Exception {
        String region = "cn-guangzhou";
        CredentialsProvider credentialsProvider = new DefaultCredentialProvider(ACEESS_KEY_ID, ACEESS_KEY_SECRET);
        ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
        clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);
        OSS ossClient = OSSClientBuilder.create()
                .endpoint(ENDPOINT)
                .credentialsProvider(credentialsProvider)
                .clientConfiguration(clientBuilderConfiguration)
                .region(REGIOM)
                .build();
        String url = "";
        try {
            // 填写字符串。
            String content = "Hello OSS，你好世界";
            PutObjectRequest putObjectRequest = new PutObjectRequest(BUCKET_NAME, objectName, in);
            PutObjectResult result = ossClient.putObject(putObjectRequest);
            url = "http://"+BUCKET_NAME + "." + ENDPOINT.substring(ENDPOINT.lastIndexOf("/")+1)+"/"+objectName;
        } catch (OSSException oe) {
        } catch (ClientException ce) {
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
        return url;
    }
}
