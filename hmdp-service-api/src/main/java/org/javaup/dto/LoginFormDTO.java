package org.javaup.dto;

import lombok.Data;

@Data
public class LoginFormDTO {
    private String phone;
    private String code;
    private String password;
}
