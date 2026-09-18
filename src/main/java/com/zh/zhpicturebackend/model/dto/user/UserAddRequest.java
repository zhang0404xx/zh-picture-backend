package com.zh.zhpicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户注册请求
 *
 * @author zh
 */
@Data
public class UserAddRequest implements Serializable {

    /**
     * 序列化id
     */
    private static final long serialVersionUID = 1L;


    /**
     * 用户姓名
     */
    private String userName;

    /**
     * 用户角色  admin/user
     */
    private String userRole;

    /**
     * 用户账号
     */
    private String userAccount;

    /**
     * 用户密码
     */
    private String userPassword;

    /**
     * 用户邮箱
     */
    private String userEmail;

    /**
     * 用户手机号
     */
    private String userPhone;


}
