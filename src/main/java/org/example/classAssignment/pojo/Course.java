package org.example.classAssignment.pojo;

import lombok.Data;

@Data
public class Course {
    private String accountId;
    private String id;
    private String name;
    private String students;
    private String classes;
    private String time;
    private Integer number;
    private String teacher;
    private String archivedBy;      // 归档者（teacher或student的accountId）
    private String archivedAt;      // 归档时间
}
