package org.example.classAssignment.pojo;

import lombok.Data;

@Data
public class TransferPrepareSpaceOwnerRequest {
    private String accountId;
    private Long targetMemberId;
    private String previousOwnerRole;
}
