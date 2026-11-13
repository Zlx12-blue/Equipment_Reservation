package com.lab.reservation.dto;

import com.lab.reservation.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度需为 3-50 位")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度需为 6-50 位")
    private String password;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 50, message = "姓名最长 50 个字符")
    private String realName;

    /** 注册类型:STUDENT / TEACHER(管理员账号由管理员在管理端创建) */
    private User.Role role;
}
