package org.example.classAssignment.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PrepareSpaceMember {
    private Long memberId;
    private Long prepareSpaceId;
    private String accountId;
    private String name;
    private String identity;
    private String role;
    private String status;
    private LocalDateTime joinedAt;
}
