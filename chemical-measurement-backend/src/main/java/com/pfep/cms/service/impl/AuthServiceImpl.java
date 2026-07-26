// Copyright (c) 2026 郑杭宇. All rights reserved.
// Licensed under the MIT License. See LICENSE file.

package com.pfep.cms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pfep.cms.common.Result;
import com.pfep.cms.dto.LoginDTO;
import com.pfep.cms.dto.RegisterDTO;
import com.pfep.cms.entity.User;
import com.pfep.cms.mapper.UserMapper;
import com.pfep.cms.service.AuthService;
import com.pfep.cms.util.JwtUtil;
import com.pfep.cms.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public Result<LoginVO> login(LoginDTO loginDTO) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, loginDTO.getUsername()));
        if (user == null || !passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            return Result.unauthorized("用户名或密码错误");
        }
        String token = jwtUtil.generateToken(user.getUserId(), user.getUsername(), user.getRole(), user.getManagedLines());
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUserId(user.getUserId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setRole(user.getRole());
        return Result.success(vo);
    }

    @Override
    public Result<?> register(RegisterDTO registerDTO) {
        if (registerDTO.getPassword() == null || registerDTO.getPassword().length() < 8) {
            return Result.error("密码至少8位");
        }
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, registerDTO.getUsername()));
        if (count > 0) {
            return Result.error("用户名已存在");
        }
        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setRealName(registerDTO.getRealName());
        user.setRole(registerDTO.getRole() != null ? registerDTO.getRole() : "INSPECTOR");
        userMapper.insert(user);
        return Result.success();
    }
}
