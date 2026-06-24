package org.example.classAssignment.controller;

import org.example.classAssignment.pojo.Result;
import org.example.classAssignment.service.TopicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class TopicController {
    @Autowired
    private TopicService topicService;

    @PostMapping("/topic/create")
    public Result createTopic(@RequestBody Map<String, Object> map) {
        Result result = new Result();
        try {
            String courseId = map.get("courseId").toString();
            String authorId = map.get("authorId").toString();
            String authorName = map.get("authorName").toString();
            String title = map.get("title").toString();
            String content = map.get("content").toString();
            Boolean isAnonymous = Boolean.parseBoolean(map.get("isAnonymous").toString());

            return topicService.createTopic(courseId, authorId, authorName, title, content, isAnonymous);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("创建话题失败: " + e.getMessage());
            return result;
        }
    }

    @PostMapping("/topic/list")
    public Result getTopicsByCourseId(@RequestBody Map<String, String> map) {
        Result result = new Result();
        try {
            String courseId = map.get("courseId");
            return topicService.getTopicsByCourseId(courseId);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("获取话题列表失败: " + e.getMessage());
            return result;
        }
    }

    @PostMapping("/topic/detail")
    public Result getTopicById(@RequestBody Map<String, String> map) {
        Result result = new Result();
        try {
            String topicId = map.get("topicId");
            return topicService.getTopicById(topicId);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("获取话题详情失败: " + e.getMessage());
            return result;
        }
    }

    @PostMapping("/topic/update")
    public Result updateTopic(@RequestBody Map<String, Object> map) {
        Result result = new Result();
        try {
            String topicId = map.get("topicId").toString();
            String authorId = map.get("authorId").toString();
            String title = map.get("title").toString();
            String content = map.get("content").toString();

            return topicService.updateTopic(topicId, authorId, title, content);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("修改话题失败: " + e.getMessage());
            return result;
        }
    }

    @PostMapping("/topic/delete")
    public Result deleteTopic(@RequestBody Map<String, Object> map) {
        Result result = new Result();
        try {
            String topicId = map.get("topicId").toString();
            String authorId = map.get("authorId").toString();
            String identity = map.get("identity").toString();

            return topicService.deleteTopic(topicId, authorId, identity);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("删除话题失败: " + e.getMessage());
            return result;
        }
    }

    @PostMapping("/topic/pin")
    public Result pinTopic(@RequestBody Map<String, Object> map) {
        Result result = new Result();
        try {
            String topicId = map.get("topicId").toString();
            Boolean isPinned = Boolean.parseBoolean(map.get("isPinned").toString());

            return topicService.pinTopic(topicId, isPinned);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("置顶操作失败: " + e.getMessage());
            return result;
        }
    }

    @PostMapping("/topic/lock")
    public Result lockTopic(@RequestBody Map<String, Object> map) {
        Result result = new Result();
        try {
            String topicId = map.get("topicId").toString();
            Boolean isLocked = Boolean.parseBoolean(map.get("isLocked").toString());

            return topicService.lockTopic(topicId, isLocked);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("锁定操作失败: " + e.getMessage());
            return result;
        }
    }

    @PostMapping("/topic/reply/add")
    public Result addReply(@RequestBody Map<String, Object> map) {
        Result result = new Result();
        try {
            String topicId = map.get("topicId").toString();
            String authorId = map.get("authorId").toString();
            String authorName = map.get("authorName").toString();
            String content = map.get("content").toString();
            Boolean isAnonymous = Boolean.parseBoolean(map.get("isAnonymous").toString());

            return topicService.addReply(topicId, authorId, authorName, content, isAnonymous);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("回复失败: " + e.getMessage());
            return result;
        }
    }

    @PostMapping("/topic/reply/delete")
    public Result deleteReply(@RequestBody Map<String, Object> map) {
        Result result = new Result();
        try {
            String replyId = map.get("replyId").toString();
            String authorId = map.get("authorId").toString();
            String identity = map.get("identity").toString();

            return topicService.deleteReply(replyId, authorId, identity);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("删除回复失败: " + e.getMessage());
            return result;
        }
    }
}
