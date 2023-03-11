package com.example.emos.api.controller.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

/**
 * Description : 删除用户表单
 *
 * @Author : 28063
 * @create 2023/2/22 20:41
 */

@Schema(description = "删除用户表单")
@Data
public class DeleteUserByIdsForm {
    @NotEmpty(message = "ids不能为空")
    @Schema(description = "用户ID")
    private Integer[] ids;
}

