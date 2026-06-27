package org.example.classAssignment.pojo;

import lombok.Data;

import java.util.List;

@Data
public class PrepareSpaceResult {
    private Boolean success;
    private String message;
    private PrepareSpace prepareSpace;
    private List<PrepareSpace> prepareSpaces;
    private List<PrepareSpaceMember> members;
    private List<PrepareSpaceOperationLog> logs;
    private Course course;
}
