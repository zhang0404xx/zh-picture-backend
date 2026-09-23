package com.zh.zhpicturebackend.manager;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.transfer.Upload;
import com.zh.zhpicturebackend.config.CosClientConfig;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class CosManager {

    @Resource
    private CosClientConfig cosClientConfig;


    @Resource
    private COSClient cosClient;


    // 上传对象
    // 将本地文件上传到 COS
    public PutObjectResult putObject( String key, String localPath ) {
        return cosClient.putObject(new PutObjectRequest(cosClientConfig.getBucket(), key, new File(localPath)));
    };
}
