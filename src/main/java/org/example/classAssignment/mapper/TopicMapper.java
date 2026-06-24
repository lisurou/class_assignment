package org.example.classAssignment.mapper;

import org.apache.ibatis.annotations.*;
import org.example.classAssignment.pojo.Topic;
import org.example.classAssignment.pojo.Reply;

import java.util.List;

@Mapper
public interface TopicMapper {
    @Insert("INSERT INTO topic(topic_id, course_id, author_id, author_name, title, content, is_anonymous, is_pinned, is_locked, reply_count, create_time, update_time) " +
            "VALUES(#{topicId}, #{courseId}, #{authorId}, #{authorName}, #{title}, #{content}, #{isAnonymous}, #{isPinned}, #{isLocked}, #{replyCount}, #{createTime}, #{updateTime})")
    Boolean insertTopic(Topic topic);

    @Select("SELECT * FROM topic WHERE course_id = #{courseId} ORDER BY is_pinned DESC, create_time DESC")
    List<Topic> findTopicsByCourseId(String courseId);

    @Select("SELECT * FROM topic WHERE topic_id = #{topicId}")
    Topic findByTopicId(String topicId);

    @Update("UPDATE topic SET title=#{title}, content=#{content}, update_time=#{updateTime} WHERE topic_id=#{topicId}")
    Integer updateTopic(Topic topic);

    @Update("UPDATE topic SET is_pinned=#{isPinned} WHERE topic_id=#{topicId}")
    Integer updateTopicPin(String topicId, Boolean isPinned);

    @Update("UPDATE topic SET is_locked=#{isLocked} WHERE topic_id=#{topicId}")
    Integer updateTopicLock(String topicId, Boolean isLocked);

    @Delete("DELETE FROM topic WHERE topic_id = #{topicId}")
    Boolean deleteTopic(String topicId);

    @Update("UPDATE topic SET reply_count = reply_count + 1 WHERE topic_id = #{topicId}")
    Boolean incrementReplyCount(String topicId);

    @Update("UPDATE topic SET reply_count = reply_count - 1 WHERE topic_id = #{topicId} AND reply_count > 0")
    Boolean decrementReplyCount(String topicId);

    @Insert("INSERT INTO reply(reply_id, topic_id, author_id, author_name, content, is_anonymous, create_time) " +
            "VALUES(#{replyId}, #{topicId}, #{authorId}, #{authorName}, #{content}, #{isAnonymous}, #{createTime})")
    Boolean insertReply(Reply reply);

    @Select("SELECT * FROM reply WHERE topic_id = #{topicId} ORDER BY create_time ASC")
    List<Reply> findRepliesByTopicId(String topicId);

    @Select("SELECT * FROM reply WHERE reply_id = #{replyId}")
    Reply findByReplyId(String replyId);

    @Delete("DELETE FROM reply WHERE reply_id = #{replyId}")
    Boolean deleteReply(String replyId);

    @Delete("DELETE FROM reply WHERE topic_id = #{topicId}")
    Boolean deleteRepliesByTopicId(String topicId);
}
