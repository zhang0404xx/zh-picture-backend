package com.zh.zhpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zh.zhpicturebackend.model.dto.user.UserQueryRequest;
import com.zh.zhpicturebackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zh.zhpicturebackend.model.vo.LoginUserVo;
import com.zh.zhpicturebackend.model.vo.UserVo;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
* @author Administrator
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2026-08-11 15:57:30
*/
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount
     * @param userPassword
     * @param checkPassword
     * @return
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);


    /**
     * 用户登入
     *
     * @param userAccount
     * @param userPassword
     * @param request
     * @return 脱敏后的用户信息
     */
    LoginUserVo userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 密码加密
     *
     * @param password
     * @return
     */
    String getEncryptPassword(String password);

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);


    /**
     * 获取脱敏后的用户信息登入用户信息
     *
     * @param user
     * @return
     */
    LoginUserVo getLoginUserVo(User user);

    /**
     * 获取脱敏后的用户信息
     *
     * @param user
     * @return
     */
    UserVo getUserVo(User user);

    /**
     * 获取脱敏后的用户信息（列表）
     *
     * @param userList
     * @return
     */
    List<UserVo> getUserVoList(List<User> userList);

    /**
     * 用户注销
     *
     * @return
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获取查询条件
     *
     * @param userQueryRequest
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);
}
