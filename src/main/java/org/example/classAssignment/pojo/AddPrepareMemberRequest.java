package org.example.classAssignment.pojo;

import lombok.Data;

@Data
public class AddPrepareMemberRequest {
    private String accountId;
    private String memberAccountId;
    private String role;
}
