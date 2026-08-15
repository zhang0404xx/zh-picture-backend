package com.zh.zhpicturebackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@MapperScan("com.zh.zhpicturebackend.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
public class ZhPictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZhPictureBackendApplication.class, args);
    }

}
