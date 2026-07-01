package org.example.classAssignment.pojo;

import lombok.Data;

@Data
public class Course {
    private String accountId;        // 授课老师账号
    private String id;
    private String name;
    private String students;
    private String classes;
    private String time;
    private Integer number;
    private String teacher;          // 展示用老师姓名，通过 accountId 关联查询
    private String archivedBy;      // 归档者（teacher或student的accountId）
    private String archivedAt;      // 归档时间
    private String bannerStoredName; // 课程横幅存储文件名
    private String bannerUrl;       // 课程横幅访问URL
}
