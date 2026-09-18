package com.zh.zhpicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.zh.zhpicturebackend.common.ErrorCode;
import com.zh.zhpicturebackend.constant.UserConstant;
import com.zh.zhpicturebackend.exception.BusinessException;
import com.zh.zhpicturebackend.model.dto.user.UserQueryRequest;
import com.zh.zhpicturebackend.model.entity.User;
import com.zh.zhpicturebackend.model.enums.UserRoleEnum;
import com.zh.zhpicturebackend.model.vo.LoginUserVo;
import com.zh.zhpicturebackend.model.vo.UserVo;
import com.zh.zhpicturebackend.service.UserService;
import com.zh.zhpicturebackend.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.zh.zhpicturebackend.constant.UserConstant.USER_LOGIN_STATE;

/**
* @author Administrator
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2026-08-11 15:57:30
*/
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

    /**
     * 密码加盐
     */
    private static final String SALT = "zh_picture";

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 检验参数是否为空
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        // 2. 账号长度校验（4~16位）
        if (userAccount.length() < 4 || userAccount.length() > 16) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号长度应为 4~16 位");
        }
        // 3. 账号格式校验（只能包含字母、数字、下划线）
        if (!userAccount.matches("^[a-zA-Z0-9_]+$")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号只能包含字母、数字和下划线");
        }
        // 4. 密码长度校验（8~16位）
        if (userPassword.length() < 8 || userPassword.length() > 16) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度应为 8~16 位");
        }
        // 5. 两次密码是否一致
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        // 6. 检查账号是否已存在
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUserAccount, userAccount);
        long count = this.count(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号已被注册");
        }

        // 7. 密码加密（加盐 MD5）
        String encryptPassword = getEncryptPassword(userPassword);

        // 8. 插入数据库
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName("用户");
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，系统内部错误");
        }
        return user.getId();
    }

    /**
     * 用户登录
     *
     * @param userAccount
     * @param userPassword
     * @param request
     * @return
     */
    @Override
    public LoginUserVo userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 检验参数
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }

        // 对用户密码进行加密
        String encryptPassword = getEncryptPassword(userPassword);
        // 查询用户
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUserAccount, userAccount);
        queryWrapper.eq(User::getUserPassword, encryptPassword);
        User user = this.getOne(queryWrapper);
        // 如果用户不存在则返回错误
        if (user == null) {
            log.info("账号或密码错误");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码错误");
        }
        // 保存登入状态
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);
        return this.getLoginUserVo(user);
    }

    /**
     * 获取加密密码
     *
     * @param password
     * @return
     */
    @Override
    public String getEncryptPassword(String password) {
        // 加盐 混淆密码
        final String SALT = "zhangheng";
        return DigestUtil.md5Hex(SALT + password);
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 判断是否登入
        User attribute = (User) request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (attribute == null || attribute.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 从数据库中查询 （追求性能可以注释）
        Long userId = attribute.getId();
        attribute = this.getById(userId);
        if (attribute == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return attribute;
    }

    /**
     * 获取登录用户信息
     *
     * @param user
     * @return
     */
    @Override
    public LoginUserVo getLoginUserVo(User user) {
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在");
        }
        LoginUserVo loginUserVo = new LoginUserVo();
        BeanUtils.copyProperties(user, loginUserVo);
        return loginUserVo;
    }

    /**
     * 获取脱敏后的用户信息
     *
     * @param user
     * @return
     */
    @Override
    public UserVo getUserVo(User user) {
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在");
        }
        UserVo userVo = new UserVo();
        BeanUtils.copyProperties(user, userVo);
        return userVo;
    }

    /**
     * 获取脱敏后的用户列表
     *
     * @param userList
     * @return
     */

    @Override
    public List<UserVo> getUserVoList(List<User> userList) {
        // 空列表是合法输入（如分页超界、无匹配结果），直接返回空列表
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }

        return userList.stream().map(this::getUserVo).collect(Collectors.toList());
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 保存登入状态
        User attribute = (User) request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (attribute == null || attribute.getId() == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        // 移除登入状态
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        return true;
    }

    /**
     * 获取查询条件
     *
     * @param userQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        Long id = userQueryRequest.getId();
        String userName = userQueryRequest.getUserName();
        String userAccount = userQueryRequest.getUserAccount();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        userQueryWrapper.like(ObjUtil.isNotNull(userName), "userName", userName);
        userQueryWrapper.like(ObjUtil.isNotNull(userAccount), "userAccount", userAccount);
        userQueryWrapper.like(ObjUtil.isNotNull(userProfile), "userProfile", userProfile);
        userQueryWrapper.eq(ObjUtil.isNotNull(userRole), "userRole", userRole);
        userQueryWrapper.orderBy(ObjUtil.isNotNull(sortField), sortOrder.equals("ascend"),sortField);
        return userQueryWrapper;
    }


}




