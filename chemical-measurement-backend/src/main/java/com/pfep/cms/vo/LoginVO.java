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
