package com.pfep.cms.controller;

import com.pfep.cms.common.Result;
import com.pfep.cms.entity.User;
import com.pfep.cms.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

@Tag(name = "个人信息")
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public Result<Map<String, Object>> getProfile(HttpServletRequest req) {
        Long userId = getUserId(req);
        User u = userMapper.selectById(userId);
        if (u == null) return Result.error("用户不存在");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("userId", u.getUserId());
        m.put("username", u.getUsername());
        m.put("realName", u.getRealName());
        m.put("role", u.getRole());
        return Result.success(m);
    }

    @Operation(summary = "修改个人信息（自己）")
    @PutMapping
    public Result<?> updateProfile(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        Long userId = getUserId(req);
        User u = userMapper.selectById(userId);
        if (u == null) return Result.error("用户不存在");

        if (body.containsKey("realName")) {
            u.setRealName((String) body.get("realName"));
        }
        if (body.containsKey("password") && body.get("password") != null && !((String) body.get("password")).isEmpty()) {
            String oldPassword = (String) body.get("oldPassword");
            if (oldPassword == null || !passwordEncoder.matches(oldPassword, u.getPassword())) {
                return Result.error("旧密码错误");
            }
            u.setPassword(passwordEncoder.encode((String) body.get("password")));
        }
        userMapper.updateById(u);
        return Result.success();
    }

    private Long getUserId(HttpServletRequest req) {
        try {
            Object attr = req.getAttribute("userId");
            if (attr != null) return (Long) attr;
            return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        } catch (Exception e) { return 2L; }
    }
}
