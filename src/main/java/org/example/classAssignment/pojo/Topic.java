package org.example.classAssignment.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class Topic {
    private String topicId;
    private String courseId;
    private Long folderId;
    private String authorId;
    private String authorName;
    private String title;
    private String content;
    private Boolean isAnonymous;
    private Boolean isPinned;
    private Boolean isLocked;
    private Integer replyCount;
    private Date createTime;
    private Date updateTime;
}
