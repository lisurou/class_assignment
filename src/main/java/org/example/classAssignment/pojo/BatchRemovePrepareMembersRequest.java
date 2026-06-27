package org.example.classAssignment.pojo;

import lombok.Data;

import java.util.List;

@Data
public class BatchRemovePrepareMembersRequest {
    private String accountId;
    private List<Long> memberIds;
}
