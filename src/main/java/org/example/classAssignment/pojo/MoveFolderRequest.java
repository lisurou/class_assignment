package org.example.classAssignment.pojo;

import lombok.Data;

@Data
public class MoveFolderRequest {
    private String accountId;
    private Long newParentId;
}
