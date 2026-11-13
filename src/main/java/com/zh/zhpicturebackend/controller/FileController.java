package com.zh.zhpicturebackend.controller;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.zh.zhpicturebackend.annotation.AuthCheck;
import com.zh.zhpicturebackend.common.BaseResponse;
import com.zh.zhpicturebackend.common.ErrorCode;
import com.zh.zhpicturebackend.common.ResultUtils;
import com.zh.zhpicturebackend.constant.UserConstant;
import com.zh.zhpicturebackend.exception.BusinessException;
import com.zh.zhpicturebackend.manager.CosManager;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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

    /**
     * 测试文件下载
     *
     * @param filepath COS 对象 key（上传接口返回的地址，如 uploads/xxx.jpg）
     * @param response HTTP 响应对象，用于写入文件流
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @GetMapping("/test/download")
    public void testDownloadFile(@RequestParam(value = "filepath", required = false) String filepath,
                                 HttpServletResponse response) {
        if (StrUtil.isBlank(filepath)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件路径为空");
        }
        COSObjectInputStream cosObjectInput = null;
        try {
            COSObject cosObject = cosManager.downloadObject(filepath);
            cosObjectInput = cosObject.getObjectContent();
            // 以附件形式下载：文件名取 key 的最后一段并做 URL 编码，避免中文文件名乱码
            String fileName = filepath.substring(filepath.lastIndexOf("/") + 1);
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + encodedFileName + "\"");
            response.setContentLengthLong(cosObject.getObjectMetadata().getContentLength());
            // 流式拷贝到响应输出流，避免将整个文件读入内存
            IoUtil.copy(cosObjectInput, response.getOutputStream());
            response.getOutputStream().flush();
        } catch (CosServiceException e) {
            log.error("file download error, filepath = {}", filepath, e);
            // 对象不存在时返回 40400，其余情形按系统错误处理
            if (e.getStatusCode() == 404) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文件不存在");
            }
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载失败");
        } catch (Exception e) {
            log.error("file download error, filepath = {}", filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载失败");
        } finally {
            // 关闭 COS 对象流，释放连接资源
            if (cosObjectInput != null) {
                try {
                    cosObjectInput.close();
                } catch (IOException e) {
                    log.error("cos object input stream close error, filepath = {}", filepath, e);
                }
            }
        }
    }
}
