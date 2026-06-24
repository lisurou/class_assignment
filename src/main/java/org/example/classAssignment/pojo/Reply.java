package org.example.classAssignment.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class Reply {
    private String replyId;
    private String topicId;
    private String authorId;
    private String authorName;
    private String content;
    private Boolean isAnonymous;
    private Date createTime;
}
