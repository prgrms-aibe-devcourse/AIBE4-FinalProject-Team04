package kr.java.aibe4_finalproject_team04_test.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDto {
    private String email;
    private String password;
    private String name;
    private String secretKey; // 관리자 가입용 키
}