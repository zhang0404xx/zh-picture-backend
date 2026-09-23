package com.zh.zhpicturebackend.controller;

import com.zh.zhpicturebackend.annotation.AuthCheck;
import com.zh.zhpicturebackend.common.BaseResponse;
import com.zh.zhpicturebackend.common.ErrorCode;
import com.zh.zhpicturebackend.common.ResultUtils;
import com.zh.zhpicturebackend.constant.UserConstant;
import com.zh.zhpicturebackend.exception.BusinessException;
import com.zh.zhpicturebackend.manager.CosManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@Slf4j
@RestController
@RequestMapping("/file")
public class FileController {

    private final CosManager cosManager;

    public FileController(CosManager cosManager) {
        this.cosManager = cosManager;
    }

    /**
     * 测试文件上传
     *
     * @param multipartFile 待上传的文件
     * @return 上传结果（COS 对象 key）
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/test/upload")
    public BaseResponse<String> testUploadFile(@RequestParam("file") MultipartFile multipartFile){
        String fileName = multipartFile.getOriginalFilename();
        // COS 对象 key：uploads/原文件名
        String filePath = String.format("uploads/%s", fileName);
        File file = null;
        try {
            // 创建临时文件（使用固定前缀，避免路径分隔符混入临时文件名）
            file = File.createTempFile("upload-", null);
            multipartFile.transferTo(file);
            cosManager.putObject(filePath, file.getPath());
            return ResultUtils.success(filePath);
        } catch (Exception e) {
            log.error("file upload error, file = {}", filePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"上传失败");
        } finally {
            if (file != null) {
                // 删除临时文件
                boolean delete = file.delete();
                if (!delete) {
                    log.error("file delete error, file = {}", filePath);
                }
            }
        }

    }
}
