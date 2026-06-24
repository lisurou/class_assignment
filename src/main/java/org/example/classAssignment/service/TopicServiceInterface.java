package org.example.classAssignment.service;

import org.example.classAssignment.pojo.Topic;
import org.example.classAssignment.pojo.Reply;
import org.example.classAssignment.pojo.Result;

import java.util.List;

public interface TopicServiceInterface {
    Result createTopic(String courseId, String authorId, String authorName, String title, String content, Boolean isAnonymous);
    Result getTopicsByCourseId(String courseId);
    Result getTopicById(String topicId);
    Result updateTopic(String topicId, String authorId, String title, String content);
    Result deleteTopic(String topicId, String authorId, String identity);
    Result pinTopic(String topicId, Boolean isPinned);
    Result lockTopic(String topicId, Boolean isLocked);
    Result addReply(String topicId, String authorId, String authorName, String content, Boolean isAnonymous);
    Result deleteReply(String replyId, String authorId, String identity);
}
