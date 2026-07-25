// Copyright (c) 2026 郑杭宇. All rights reserved.
// Licensed under the MIT License. See LICENSE file.

package com.pfep.cms.vo;

import lombok.Data;

@Data
public class LoginVO {
    private Long userId;
    private String token;
    private String username;
    private String realName;
    private String role;
    private String managedLines;
}
