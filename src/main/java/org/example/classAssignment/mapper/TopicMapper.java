package org.example.classAssignment.mapper;

import org.apache.ibatis.annotations.*;
import org.example.classAssignment.pojo.Topic;
import org.example.classAssignment.pojo.Reply;

import java.util.List;

@Mapper
public interface TopicMapper {
    @Insert("INSERT INTO discussion_topic(topic_id, scene_type, scene_id, folder_id, author_id, author_name, title, content, is_anonymous, is_pinned, is_locked, reply_count, create_time, update_time) " +
            "VALUES(#{topicId}, 'COURSE', #{courseId}, null, #{authorId}, #{authorName}, #{title}, #{content}, #{isAnonymous}, #{isPinned}, #{isLocked}, #{replyCount}, #{createTime}, #{updateTime})")
    Boolean insertTopic(Topic topic);

    @Select("SELECT topic_id as topicId, scene_id as courseId, folder_id as folderId, author_id as authorId, author_name as authorName, title, content, " +
            "is_anonymous as isAnonymous, is_pinned as isPinned, is_locked as isLocked, reply_count as replyCount, create_time as createTime, update_time as updateTime " +
            "FROM discussion_topic WHERE scene_type='COURSE' AND scene_id = #{courseId} ORDER BY is_pinned DESC, create_time DESC")
    List<Topic> findTopicsByCourseId(String courseId);

    @Select("SELECT topic_id as topicId, scene_id as courseId, folder_id as folderId, author_id as authorId, author_name as authorName, title, content, " +
            "is_anonymous as isAnonymous, is_pinned as isPinned, is_locked as isLocked, reply_count as replyCount, create_time as createTime, update_time as updateTime " +
            "FROM discussion_topic WHERE scene_type='COURSE' AND topic_id = #{topicId}")
    Topic findByTopicId(String topicId);

    @Update("UPDATE discussion_topic SET title=#{title}, content=#{content}, update_time=#{updateTime} WHERE scene_type='COURSE' AND topic_id=#{topicId}")
    Integer updateTopic(Topic topic);

    @Update("UPDATE discussion_topic SET is_pinned=#{isPinned} WHERE scene_type='COURSE' AND topic_id=#{topicId}")
    Integer updateTopicPin(String topicId, Boolean isPinned);

    @Update("UPDATE discussion_topic SET is_locked=#{isLocked} WHERE scene_type='COURSE' AND topic_id=#{topicId}")
    Integer updateTopicLock(String topicId, Boolean isLocked);

    @Delete("DELETE FROM discussion_topic WHERE scene_type='COURSE' AND topic_id = #{topicId}")
    Boolean deleteTopic(String topicId);

    @Update("UPDATE discussion_topic SET reply_count = reply_count + 1 WHERE scene_type='COURSE' AND topic_id = #{topicId}")
    Boolean incrementReplyCount(String topicId);

    @Update("UPDATE discussion_topic SET reply_count = reply_count - 1 WHERE scene_type='COURSE' AND topic_id = #{topicId} AND reply_count > 0")
    Boolean decrementReplyCount(String topicId);

    @Insert("INSERT INTO discussion_reply(reply_id, topic_id, author_id, author_name, content, is_anonymous, create_time) " +
            "VALUES(#{replyId}, #{topicId}, #{authorId}, #{authorName}, #{content}, #{isAnonymous}, #{createTime})")
    Boolean insertReply(Reply reply);

    @Select("SELECT reply_id as replyId, topic_id as topicId, author_id as authorId, author_name as authorName, content, is_anonymous as isAnonymous, create_time as createTime " +
            "FROM discussion_reply WHERE topic_id = #{topicId} ORDER BY create_time ASC")
    List<Reply> findRepliesByTopicId(String topicId);

    @Select("SELECT reply_id as replyId, topic_id as topicId, author_id as authorId, author_name as authorName, content, is_anonymous as isAnonymous, create_time as createTime " +
            "FROM discussion_reply WHERE reply_id = #{replyId}")
    Reply findByReplyId(String replyId);

    @Delete("DELETE FROM discussion_reply WHERE reply_id = #{replyId}")
    Boolean deleteReply(String replyId);

    @Delete("DELETE FROM discussion_reply WHERE topic_id = #{topicId}")
    Boolean deleteRepliesByTopicId(String topicId);
}
