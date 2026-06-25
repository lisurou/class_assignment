package org.example.classAssignment.pojo;

import lombok.Data;

@Data
public class CourseAndAccount {
    String accountId;
    String id;
    String assignmentId;
    String submitContent;
    Boolean removeFile;
    Integer score;
    Assignment assignment;
}
