package com.zh.zhpicturebackend.manager;

import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.zh.zhpicturebackend.config.CosClientConfig;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * 文件管理类：封装 COS 对象存储的上传、下载等常用操作
 */
@Slf4j
@Service
public class FileManager {

    /**
     * COS 客户端配置（bucket 等）
     */
    @Resource
    private CosClientConfig cosClientConfig;


    /**
     * COS 管理类（负责底层对象存储操作）
     */
    @Resource
    private CosManager cosManager;


    /**
     * 上传对象：将本地文件上传到 COS
     *
     * @param key  对象 key（如 uploads/xxx.jpg）
     * @param file 本地文件
     * @return 上传结果
     */
    public PutObjectResult putObject(String key, File file) {
        return cosManager.putObject(key, file.getPath());
    }

    /**
     * 下载对象：从 COS 获取指定 key 的对象
     * 注意：返回的 COSObject 包含对象内容流，调用方使用完毕后必须关闭
     * （COSObject 实现了 Closeable），否则连接不会释放
     *
     * @param key 对象 key（如 uploads/xxx.jpg）
     * @return COS 对象（含内容流与元数据）
     */
    public COSObject downloadObject(String key) {
        return cosManager.downloadObject(key);
    }

    /**
     * 上传对象：将本地文件上传到 COS 对图片信息进行处理
     *
     * @param key  对象 key（如 uploads/xxx.jpg）
     * @param file 本地文件
     * @return 上传结果
     */
    public PutObjectResult putObjectCOS(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        // 图片处理

        return cosManager.putObject(putObjectRequest);
    }
}
