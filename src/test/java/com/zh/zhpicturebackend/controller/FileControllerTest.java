package com.zh.zhpicturebackend.controller;

import com.qcloud.cos.exception.CosServiceException;
import com.zh.zhpicturebackend.common.ErrorCode;
import com.zh.zhpicturebackend.exception.BusinessException;
import com.zh.zhpicturebackend.manager.CosManager;
import com.zh.zhpicturebackend.model.entity.User;
import com.zh.zhpicturebackend.model.enums.UserRoleEnum;
import com.zh.zhpicturebackend.constant.UserConstant;
import com.zh.zhpicturebackend.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 文件接口测试集（上传 / 下载）
 * <p>
 * 说明：通过 {@code @MockitoBean} 隔离外部依赖，避免测试触达真实 COS 与数据库：
 * 1. {@link CosManager}：拦截所有 COS 上传/下载调用；
 * 2. {@link UserService}：拦截 {@code getLoginUser}，用于构造“未登录 / 普通用户 / 管理员”三种登录态。
 * 鉴权由 {@code @AuthCheck(mustRole = admin)} 的 AOP 完成，异常统一经全局异常处理器转为 BaseResponse。
 */
@SpringBootTest
@AutoConfigureMockMvc
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CosManager cosManager;

    @MockitoBean
    private UserService userService;

    private static final String UPLOAD_URL = "/file/test/upload";

    private static final String DOWNLOAD_URL = "/file/test/download";

    /**
     * 构造一个已登录用户并 stub 到 userService.getLoginUser
     *
     * @param role 用户角色（admin/user）
     */
    private void stubLoginUser(String role) {
        User user = new User();
        user.setId(1L);
        user.setUserRole(role);
        when(userService.getLoginUser(any())).thenReturn(user);
    }

    /**
     * stub 为未登录：getLoginUser 直接抛未登录业务异常
     */
    private void stubNotLogin() {
        when(userService.getLoginUser(any())).thenThrow(new BusinessException(ErrorCode.NOT_LOGIN_ERROR));
    }

    private MockMultipartFile jsonFile(String name) {
        return new MockMultipartFile("file", name, "application/json",
                "{\"hello\":\"world\"}".getBytes(StandardCharsets.UTF_8));
    }

    // ---------------------------------------------------------------------
    // 上传接口 /file/test/upload
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("上传-管理员上传成功，返回 COS 对象 key 且按 uploads/原文件名 组装")
    void testUploadFileSuccess() throws Exception {
        stubLoginUser(UserRoleEnum.ADMIN.getValue());
        when(cosManager.putObject(anyString(), anyString())).thenReturn(null);

        mockMvc.perform(multipart(UPLOAD_URL).file(jsonFile("test.json")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data").value("uploads/test.json"));

        // 校验传给 COS 的对象 key 与文件名拼接规则一致
        verify(cosManager).putObject(eq("uploads/test.json"), anyString());
    }

    @Test
    @DisplayName("上传-未登录返回 40100")
    void testUploadFileNotLogin() throws Exception {
        stubNotLogin();

        mockMvc.perform(multipart(UPLOAD_URL).file(jsonFile("test.json")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.NOT_LOGIN_ERROR.getCode()));
    }

    @Test
    @DisplayName("上传-已登录但非管理员返回 40101 无权限")
    void testUploadFileNoAuth() throws Exception {
        stubLoginUser(UserConstant.DEFAULT_ROLE);

        mockMvc.perform(multipart(UPLOAD_URL).file(jsonFile("test.json")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.NO_AUTH_ERROR.getCode()));
    }

    @Test
    @DisplayName("上传-COS 上传抛异常时返回 50000 上传失败")
    void testUploadFileCosError() throws Exception {
        stubLoginUser(UserRoleEnum.ADMIN.getValue());
        when(cosManager.putObject(anyString(), anyString()))
                .thenThrow(new RuntimeException("cos down"));

        mockMvc.perform(multipart(UPLOAD_URL).file(jsonFile("test.json")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.SYSTEM_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value("上传失败"));
    }

    // ---------------------------------------------------------------------
    // 下载接口 /file/test/download
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("下载-路径为空返回 40000 参数错误")
    void testDownloadFileBlankPath() throws Exception {
        stubLoginUser(UserRoleEnum.ADMIN.getValue());

        mockMvc.perform(get(DOWNLOAD_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.PARAMS_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value("文件路径为空"));
    }

    @Test
    @DisplayName("下载-未登录返回 40100")
    void testDownloadFileNotLogin() throws Exception {
        stubNotLogin();

        mockMvc.perform(get(DOWNLOAD_URL).param("filepath", "uploads/test.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.NOT_LOGIN_ERROR.getCode()));
    }

    @Test
    @DisplayName("下载-非管理员返回 40101 无权限")
    void testDownloadFileNoAuth() throws Exception {
        stubLoginUser(UserConstant.DEFAULT_ROLE);

        mockMvc.perform(get(DOWNLOAD_URL).param("filepath", "uploads/test.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.NO_AUTH_ERROR.getCode()));
    }

    @Test
    @DisplayName("下载-COS 对象不存在（404）返回 40400")
    void testDownloadFileNotFound() throws Exception {
        stubLoginUser(UserRoleEnum.ADMIN.getValue());
        CosServiceException notFound = new CosServiceException("Not Found");
        notFound.setStatusCode(404);
        when(cosManager.downloadObject(anyString())).thenThrow(notFound);

        mockMvc.perform(get(DOWNLOAD_URL).param("filepath", "uploads/missing.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.NOT_FOUND_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value("文件不存在"));
    }

    @Test
    @DisplayName("下载-COS 其它服务异常返回 50000 下载失败")
    void testDownloadFileCosError() throws Exception {
        stubLoginUser(UserRoleEnum.ADMIN.getValue());
        CosServiceException serverError = new CosServiceException("Internal Error");
        serverError.setStatusCode(500);
        when(cosManager.downloadObject(anyString())).thenThrow(serverError);

        mockMvc.perform(get(DOWNLOAD_URL).param("filepath", "uploads/test.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.SYSTEM_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value("下载失败"));
    }
}
