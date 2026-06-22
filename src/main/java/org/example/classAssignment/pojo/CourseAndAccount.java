package org.example.classAssignment.pojo;

import lombok.Data;

@Data
public class CourseAndAccount {
    String accountId;
    String id;
    String assignmentId;
    String submitContent;
    Integer score;
    Assignment assignment;
}
