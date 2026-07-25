// Copyright (c) 2026 郑杭宇. All rights reserved.
// Licensed under the MIT License. See LICENSE file.

package com.pfep.cms.service;

import com.pfep.cms.common.Result;
import com.pfep.cms.dto.LoginDTO;
import com.pfep.cms.dto.RegisterDTO;
import com.pfep.cms.vo.LoginVO;

public interface AuthService {
    Result<LoginVO> login(LoginDTO loginDTO);
    Result<?> register(RegisterDTO registerDTO);
}
