package com.zh.zhpicturebackend.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zh.zhpicturebackend.annotation.AuthCheck;
import com.zh.zhpicturebackend.common.BaseResponse;
import com.zh.zhpicturebackend.common.DeleteRequest;
import com.zh.zhpicturebackend.common.ErrorCode;
import com.zh.zhpicturebackend.common.ResultUtils;
import com.zh.zhpicturebackend.constant.UserConstant;
import com.zh.zhpicturebackend.exception.ThrowUtils;
import com.zh.zhpicturebackend.model.dto.user.*;
import com.zh.zhpicturebackend.model.entity.User;
import com.zh.zhpicturebackend.model.vo.LoginUserVo;
import com.zh.zhpicturebackend.model.vo.UserVo;
import com.zh.zhpicturebackend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 新增用户时的初始密码（配置于 application.yml，可通过环境变量 USER_DEFAULT_PASSWORD 覆盖）
     */
    @Value("${zh-picture.user.default-password}")
    private String defaultPassword;


    /**
     * 用户注册（公开接口，新用户无需登录即可注册）
     *
     * @return 注册成功后的用户 id
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        long result = userService.userRegister(userAccount, userPassword, checkPassword);
        return ResultUtils.success(result);
    }

    /**
     * 用户登录
     *
     * @return 脱敏后的用户信息
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVo> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        LoginUserVo loginUserVo = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(loginUserVo);
    }

    /**
     * 获取当前登录用户
     *
     * @return
     */
    @GetMapping("/getLoginUser")
    public BaseResponse<LoginUserVo> getLoginUser(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        LoginUserVo loginUserVo = userService.getLoginUserVo(user);
        return ResultUtils.success(loginUserVo);
    }

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    @GetMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 创建用户
     *
     * @return
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtils.copyProperties(userAddRequest, user);
        // 优先使用请求中指定的密码；仅在未指定时使用配置的初始密码（避免无条件覆盖请求的密码）
        String rawPassword = StrUtil.isNotBlank(userAddRequest.getUserPassword())
                ? userAddRequest.getUserPassword()
                : defaultPassword;
        // 密码长度校验：与注册接口保持一致的 8~16 位规则
        ThrowUtils.throwIf(rawPassword.length() < 8 || rawPassword.length() > 16,
                ErrorCode.PARAMS_ERROR, "密码长度应为 8~16 位");
        user.setUserPassword(userService.getEncryptPassword(rawPassword));
        // 插入数据库
        boolean save = userService.save(user);
        ThrowUtils.throwIf(!save, ErrorCode.OPERATION_ERROR, "添加用户失败");

        return ResultUtils.success(user.getId());
    }
    /**
     * 根据 id 获取用户（仅管理员）
     *
     * @param id 用户 id
     * @return 脱敏后的用户信息
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<UserVo> getUserById(Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        return ResultUtils.success(userService.getUserVo(user));
    }
    /**
     * 更新用户（仅管理员）
     *
     * @param userUpdateRequest 更新请求
     * @return 是否更新成功
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        ThrowUtils.throwIf(userUpdateRequest == null || userUpdateRequest.getId() == null, ErrorCode.PARAMS_ERROR);
        // 将请求参数转换为实体，MyBatis-Plus 默认只更新非 null 字段
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "更新用户失败");
        return ResultUtils.success(true);
    }
    /**
     * 删除用户（仅管理员）
     *
     * @param deleteRequest 删除请求（携带用户 id）
     * @param request       当前请求（用于获取登录态）
     * @return 是否删除成功
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        // 防止管理员误删自己，导致当前会话失效、系统失去管理员
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser.getId().equals(deleteRequest.getId()), ErrorCode.PARAMS_ERROR, "不能删除当前登录的管理员账号");
        boolean result = userService.removeById(deleteRequest.getId());
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "删除用户失败");
        return ResultUtils.success(true);
    }
    /**
     * 分页获取用户列表（仅管理员）
     *
     * @param userQueryRequest 查询请求（支持按 id/昵称/账号/角色筛选及排序）
     * @return 分页后的脱敏用户信息
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVo>> listUserByPage(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = userQueryRequest.getCurrent();
        long size = userQueryRequest.getPageSize();
        // 分页查询
        Page<User> userPage = userService.page(new Page<>(current, size), userService.getQueryWrapper(userQueryRequest));
        // 转换为脱敏 VO（空页返回空列表，由 getUserVoList 保证）
        Page<UserVo> userVoPage = new Page<>(current, size, userPage.getTotal());
        userVoPage.setRecords(userService.getUserVoList(userPage.getRecords()));
        return ResultUtils.success(userVoPage);
    }
}
