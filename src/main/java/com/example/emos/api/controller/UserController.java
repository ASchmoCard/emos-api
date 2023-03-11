package com.example.emos.api.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.json.JSONUtil;
import com.example.emos.api.common.util.PageUtils;
import com.example.emos.api.common.util.R;
import com.example.emos.api.controller.form.*;
import com.example.emos.api.controller.form.userform.UpdatePasswordForm;
import com.example.emos.api.controller.form.userform.UpdateUserForm;
import com.example.emos.api.db.pojo.TbUser;
import com.example.emos.api.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;

@RestController
@RequestMapping("/user")
@Tag(name = "UserController", description = "用户Web接口")
public class UserController {
    @Autowired
    private UserService userService;

    /**
     * 生成登陆二维码的字符串
     */
    @GetMapping("/createQrCode")
    @Operation(summary = "生成二维码Base64格式的字符串")
    public R createQrCode() {
        HashMap map = userService.createQrCode();
        return R.ok(map);
    }

    /**
     * 检测登陆验证码
     *
     * @param form
     * @return
     */
    @PostMapping("/checkQrCode")
    @Operation(summary = "检测登陆验证码")
    public R checkQrCode(@Valid @RequestBody CheckQrCodeForm form) {
        boolean bool = userService.checkQrCode(form.getCode(), form.getUuid());
        return R.ok().put("result", bool);
    }

    @PostMapping("/wechatLogin")
    @Operation(summary = "微信小程序登陆")
    public R wechatLogin(@Valid @RequestBody WechatLoginForm form) {
        HashMap map = userService.wechatLogin(form.getUuid());
        boolean result = (boolean) map.get("result");
        if (result) {
            int userId = (int) map.get("userId");
            StpUtil.setLoginId(userId);
            Set<String> permissions = userService.searchUserPermissions(userId);
            map.remove("userId");
            map.put("permissions", permissions);
        }
        return R.ok(map);
    }

    /**
     * 登陆成功后加载用户的基本信息
     */
    @GetMapping("/loadUserInfo")
    @Operation(summary = "登陆成功后加载用户的基本信息")
    @SaCheckLogin
    public R loadUserInfo() {
        int userId = StpUtil.getLoginIdAsInt();
        HashMap summary = userService.searchUserSummary(userId);
        return R.ok(summary);
    }

    @PostMapping("/searchById")
    @Operation(summary = "根据ID查找用户")
    @SaCheckPermission(value = {"ROOT", "USER:SELECT"}, mode = SaMode.OR)
    public R searchById(@Valid @RequestBody SearchUserByIdForm form) {
        HashMap map = userService.searchById(form.getUserId());
        return R.ok(map);
    }

    @GetMapping("/searchAllUser")
    @Operation(summary = "查询所有用户")
    @SaCheckLogin
    public R searchAllUser() {
        ArrayList<HashMap> list = userService.searchAllUser();
        return R.ok().put("list", list);
    }

    @PostMapping("/login")
    @Operation(summary = "登录系统")
    public R login(@Valid @RequestBody LoginForm form){
        HashMap param = JSONUtil.parse(form).toBean(HashMap.class);
        Integer userId = userService.login(param);
        R r = R.ok().put("result",userId != null ? true : false);
        if(userId != null){
            StpUtil.setLoginId(userId);
            Set<String> permissions = userService.searchUserPermissions(userId);
            // 因为新版 Chrome 浏览器不支持前端 Ajax 的 withCredentials，导致Ajax无法提交Cookie，所以我们要取出生成的Token返回给前端，让前端保存在Storage中，然后每次在Ajax的Header上提交Token
            String token = StpUtil.getTokenInfo().getTokenValue();
            r.put("permissions",permissions).put("token",token);
        }
        return r;
    }

    @GetMapping("/logout")
    @Operation(summary = "退出系统")
    public R logout(){
        // sa-token
        // 1.把redis缓存的用户信息抹除掉
        // 2.让浏览器保存的cookie过期
        StpUtil.logout();
        return R.ok();
    }

    @PostMapping("/updatePassword")
    @SaCheckLogin
    @Operation(summary = "修改密码")
    public R updatePassword(@Valid @RequestBody UpdatePasswordForm form){
        // 通过userId 生成的 token，在将其转换回来
        int userId = StpUtil.getLoginIdAsInt();
        HashMap param = new HashMap();
        param.put("userId",userId);
        param.put("password",form.getPassword());
        param.put("newPassword",form.getNewPassword());
        // 受影响的行数
        int rows = userService.updatePassword(param);
        return R.ok().put("rows",rows);
    }

    @PostMapping("/searchUserByPage")
    @Operation(summary = "查询用户分页记录")
    @SaCheckPermission(value = {"ROOT","USER:SELECT"}, mode = SaMode.OR)
    public R searchUserByPage(@Valid @RequestBody SearchUserByPageForm form){
        int page = form.getPage();
        int length = form.getLength();
        int start = (page - 1) * length;
        HashMap param = JSONUtil.parse(form).toBean(HashMap.class);
        param.put("start",start);
        PageUtils pageUtils = userService.searchUserByPage(param);
        return R.ok().put("page",pageUtils);
    }

    @PostMapping("/insert")
    @SaCheckPermission(value = {"ROOT","USER:INSERT"},mode = SaMode.OR)
    @Operation(summary = "添加用户")
    public R insert (@Valid @RequestBody InsertUserForm form){
        TbUser user = JSONUtil.parse(form).toBean(TbUser.class);
        user.setStatus((byte) 1);
        user.setRole(JSONUtil.parseArray(form.getRole()).toString());
        user.setCreateTime(new Date());
        int rows = userService.insert(user);
        return R.ok().put("rows",rows);
    }

    @PostMapping("/update")
    @SaCheckPermission(value = {"ROOT","USER:UPDATE"},mode = SaMode.OR)
    @Operation(summary = "修改用户信息")
    public R update(@Valid @RequestBody UpdateUserForm form){
        HashMap param = JSONUtil.parse(form).toBean(HashMap.class);
        param.replace("role",JSONUtil.parseArray(form.getRole()).toString());
        int rows = userService.update(param);
        if(rows == 1){
            StpUtil.logoutByLoginId(form.getUserId());
        }
        return R.ok().put("rows",rows);
    }

    @PostMapping("/deleteUserByIds")
    @SaCheckPermission(value = {"ROOT", "USER:DELETE"}, mode = SaMode.OR)
    @Operation(summary = "删除用户")
    public R deleteUserByIds(@Valid @RequestBody DeleteUserByIdsForm form){
        Integer userId = StpUtil.getLoginIdAsInt();
        if(ArrayUtil.contains(form.getIds(),userId)){
            return R.error("不能删除自己的帐户!");
        }
        int rows = userService.deleteUserByIds(form.getIds());
        if(rows > 0){
            for(Integer id : form.getIds()){
                StpUtil.logoutByLoginId(id);
            }
        }
        return R.ok().put("rows",rows);
    }

    @PostMapping("/searchNameAndDept")
    @Operation(summary = "查找员工姓名和部门")
    @SaCheckLogin
    public R searchNameAndDept(@Valid @RequestBody SearchNameAndDeptForm form) {
        int userId = form.getId();
        HashMap map = userService.searchNameAndDept(userId);
        return R.ok(map);
    }
}
