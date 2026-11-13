package com.zh.zhpicturebackend.manager;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.zh.zhpicturebackend.config.CosClientConfig;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class CosManager {

    @Resource
    private CosClientConfig cosClientConfig;


    @Resource
    private COSClient cosClient;


    /**
     * 上传对象：将本地文件上传到 COS
     *
     * @param key       对象 key（如 uploads/xxx.jpg）
     * @param localPath 本地文件路径
     * @return 上传结果
     */
    public PutObjectResult putObject(String key, String localPath) {
        return cosClient.putObject(new PutObjectRequest(cosClientConfig.getBucket(), key, new File(localPath)));
    }

    /**
     * 上传对象：按请求上传到 COS（调用方可设置图片处理、元数据等高级参数）
     *
     * @param putObjectRequest 上传请求（含 bucket / key / 文件及可选处理参数）
     * @return 上传结果
     */
    public PutObjectResult putObject(PutObjectRequest putObjectRequest) {
        return cosClient.putObject(putObjectRequest);
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
        return cosClient.getObject(cosClientConfig.getBucket(), key);
    }
}
