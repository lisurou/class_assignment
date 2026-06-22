package org.example.classAssignment.pojo;

import lombok.Data;

@Data
public class CreateFolderRequest {
    private String accountId;
    private Long parentId;
    private String name;
}
