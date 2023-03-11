package com.example.emos.api.controller.form.userform;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * Description : 修改密码表单
 *
 * @Author : 28063
 * @create 2023/2/22 20:31
 */
@Schema(description = "修改密码表单")
@Data
public class UpdatePasswordForm {
    @NotBlank(message = "password不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9]{6,20}$",message = "password内容不正确")
    @Schema(description = "密码")
    String password;

    @NotBlank(message = "newPassword不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9]{6,20}$",message = "newPassword格式错误")
    @Schema(description = "密码")
    private String newPassword;
}

