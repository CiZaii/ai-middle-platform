package com.ai.middle.platform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 知识库成员DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberDTO {

    /**
     * 成员对应的用户信息
     */
    private UserDTO user;

    /**
     * 成员角色
     */
    private String role;

    /**
     * 加入时间
     */
    private LocalDateTime joinedAt;
}
