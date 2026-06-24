package org.example.classAssignment.service;

import org.example.classAssignment.mapper.TopicMapper;
import org.example.classAssignment.pojo.Topic;
import org.example.classAssignment.pojo.Reply;
import org.example.classAssignment.pojo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class TopicService implements TopicServiceInterface {
    @Autowired
    private TopicMapper topicMapper;

    @Override
    public Result createTopic(String courseId, String authorId, String authorName, String title, String content, Boolean isAnonymous) {
        Result result = new Result();
        try {
            Topic topic = new Topic();
            topic.setTopicId(UUID.randomUUID().toString().replace("-", ""));
            topic.setCourseId(courseId);
            topic.setAuthorId(authorId);
            topic.setAuthorName(isAnonymous ? "匿名用户" : authorName);
            topic.setTitle(title);
            topic.setContent(content);
            topic.setIsAnonymous(isAnonymous);
            topic.setIsPinned(false);
            topic.setIsLocked(false);
            topic.setReplyCount(0);
            topic.setCreateTime(new Date());
            topic.setUpdateTime(new Date());

            if (topicMapper.insertTopic(topic)) {
                result.setSuccess(true);
                result.setMessage("话题创建成功");
                result.setTopic(topic);
            } else {
                result.setSuccess(false);
                result.setMessage("话题创建失败");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("话题创建失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    public Result getTopicsByCourseId(String courseId) {
        Result result = new Result();
        try {
            List<Topic> topics = topicMapper.findTopicsByCourseId(courseId);
            result.setSuccess(true);
            result.setTopics(topics);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("获取话题列表失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    public Result getTopicById(String topicId) {
        Result result = new Result();
        try {
            Topic topic = topicMapper.findByTopicId(topicId);
            if (topic != null) {
                List<Reply> replies = topicMapper.findRepliesByTopicId(topicId);
                result.setSuccess(true);
                result.setTopic(topic);
                result.setReplies(replies);
            } else {
                result.setSuccess(false);
                result.setMessage("话题不存在");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("获取话题详情失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    public Result updateTopic(String topicId, String authorId, String title, String content) {
        Result result = new Result();
        try {
            Topic topic = topicMapper.findByTopicId(topicId);
            if (topic == null) {
                result.setSuccess(false);
                result.setMessage("话题不存在");
                return result;
            }

            if (!topic.getAuthorId().equals(authorId)) {
                result.setSuccess(false);
                result.setMessage("无权修改此话题");
                return result;
            }

            topic.setTitle(title);
            topic.setContent(content);
            topic.setUpdateTime(new Date());

            if (topicMapper.updateTopic(topic) > 0) {
                result.setSuccess(true);
                result.setMessage("话题修改成功");
                result.setTopic(topic);
            } else {
                result.setSuccess(false);
                result.setMessage("话题修改失败");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("话题修改失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    public Result deleteTopic(String topicId, String authorId, String identity) {
        Result result = new Result();
        try {
            Topic topic = topicMapper.findByTopicId(topicId);
            if (topic == null) {
                result.setSuccess(false);
                result.setMessage("话题不存在");
                return result;
            }

            // 教师可以删除任何话题，学生只能删除自己的
            if (!"老师".equals(identity) && !topic.getAuthorId().equals(authorId)) {
                result.setSuccess(false);
                result.setMessage("无权删除此话题");
                return result;
            }

            // 删除话题的所有回复
            topicMapper.deleteRepliesByTopicId(topicId);

            if (topicMapper.deleteTopic(topicId)) {
                result.setSuccess(true);
                result.setMessage("话题删除成功");
            } else {
                result.setSuccess(false);
                result.setMessage("话题删除失败");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("话题删除失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    public Result pinTopic(String topicId, Boolean isPinned) {
        Result result = new Result();
        try {
            Topic topic = topicMapper.findByTopicId(topicId);
            if (topic == null) {
                result.setSuccess(false);
                result.setMessage("话题不存在");
                return result;
            }

            if (topicMapper.updateTopicPin(topicId, isPinned) > 0) {
                result.setSuccess(true);
                result.setMessage(isPinned ? "话题置顶成功" : "取消置顶成功");
            } else {
                result.setSuccess(false);
                result.setMessage("操作失败");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("操作失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    public Result lockTopic(String topicId, Boolean isLocked) {
        Result result = new Result();
        try {
            Topic topic = topicMapper.findByTopicId(topicId);
            if (topic == null) {
                result.setSuccess(false);
                result.setMessage("话题不存在");
                return result;
            }

            if (topicMapper.updateTopicLock(topicId, isLocked) > 0) {
                result.setSuccess(true);
                result.setMessage(isLocked ? "话题已锁定" : "话题已解锁");
            } else {
                result.setSuccess(false);
                result.setMessage("操作失败");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("操作失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    public Result addReply(String topicId, String authorId, String authorName, String content, Boolean isAnonymous) {
        Result result = new Result();
        try {
            Topic topic = topicMapper.findByTopicId(topicId);
            if (topic == null) {
                result.setSuccess(false);
                result.setMessage("话题不存在");
                return result;
            }

            if (topic.getIsLocked()) {
                result.setSuccess(false);
                result.setMessage("话题已锁定，无法回复");
                return result;
            }

            Reply reply = new Reply();
            reply.setReplyId(UUID.randomUUID().toString().replace("-", ""));
            reply.setTopicId(topicId);
            reply.setAuthorId(authorId);
            reply.setAuthorName(isAnonymous ? "匿名用户" : authorName);
            reply.setContent(content);
            reply.setIsAnonymous(isAnonymous);
            reply.setCreateTime(new Date());

            if (topicMapper.insertReply(reply)) {
                topicMapper.incrementReplyCount(topicId);
                result.setSuccess(true);
                result.setMessage("回复成功");
                result.setReply(reply);
            } else {
                result.setSuccess(false);
                result.setMessage("回复失败");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("回复失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    public Result deleteReply(String replyId, String authorId, String identity) {
        Result result = new Result();
        try {
            Reply reply = topicMapper.findByReplyId(replyId);
            if (reply == null) {
                result.setSuccess(false);
                result.setMessage("回复不存在");
                return result;
            }

            // 教师可以删除任何回复，学生只能删除自己的
            if (!"老师".equals(identity) && !reply.getAuthorId().equals(authorId)) {
                result.setSuccess(false);
                result.setMessage("无权删除此回复");
                return result;
            }

            if (topicMapper.deleteReply(replyId)) {
                topicMapper.decrementReplyCount(reply.getTopicId());
                result.setSuccess(true);
                result.setMessage("回复删除成功");
            } else {
                result.setSuccess(false);
                result.setMessage("回复删除失败");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("回复删除失败: " + e.getMessage());
        }
        return result;
    }
}
