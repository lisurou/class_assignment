package org.example.classAssignment.pojo;

import lombok.Data;

@Data
public class UpdatePrepareSpaceRequest {
    private String accountId;
    private String name;
    private String description;
}
