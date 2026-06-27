package org.example.classAssignment.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.classAssignment.pojo.Assignment;
import org.example.classAssignment.pojo.Courseware;
import org.example.classAssignment.pojo.MaterialAttachment;
import org.example.classAssignment.pojo.MaterialFolder;
import org.example.classAssignment.pojo.MaterialLink;
import org.example.classAssignment.pojo.Reply;
import org.example.classAssignment.pojo.Topic;

import java.util.List;

@Mapper
public interface PrepareSpaceContentMapper {
    @Select("<script>" +
            "select folder_id as folderId, category, parent_id as parentId, name, created_by as createdBy, created_at as createdAt " +
            "from prepare_space_folder " +
            "where prepare_space_id=#{spaceId} and module=#{module} " +
            "<if test='category != null'> and category=#{category}</if> " +
            "order by created_at asc, folder_id asc" +
            "</script>")
    List<MaterialFolder> findFolders(@Param("spaceId") Long spaceId,
                                     @Param("module") String module,
                                     @Param("category") String category);

    @Select("select folder_id as folderId, category, parent_id as parentId, name, created_by as createdBy, created_at as createdAt " +
            "from prepare_space_folder " +
            "where prepare_space_id=#{spaceId} and module=#{module} and folder_id=#{folderId} " +
            "limit 1")
    MaterialFolder findFolderById(@Param("spaceId") Long spaceId, @Param("module") String module, @Param("folderId") Long folderId);

    @Insert("insert into prepare_space_folder(prepare_space_id, module, category, parent_id, name, created_by, created_at) " +
            "values(#{spaceId}, #{module}, #{folder.category}, #{folder.parentId}, #{folder.name}, #{createdBy}, now())")
    @Options(useGeneratedKeys = true, keyProperty = "folder.folderId", keyColumn = "folder_id")
    Boolean insertFolder(@Param("spaceId") Long spaceId,
                         @Param("module") String module,
                         @Param("createdBy") String createdBy,
                         @Param("folder") MaterialFolder folder);

    @Update("update prepare_space_folder set parent_id=#{parentId} where prepare_space_id=#{spaceId} and module=#{module} and folder_id=#{folderId}")
    Integer updateFolderParent(@Param("spaceId") Long spaceId,
                               @Param("module") String module,
                               @Param("folderId") Long folderId,
                               @Param("parentId") Long parentId);

    @Delete("delete from prepare_space_folder where prepare_space_id=#{spaceId} and module=#{module} and folder_id=#{folderId}")
    Integer deleteFolder(@Param("spaceId") Long spaceId, @Param("module") String module, @Param("folderId") Long folderId);

    @Insert("insert into prepare_space_import_log(prepare_space_id, module, source_type, source_course_id, source_item_type, source_item_id, target_item_id, mode, operator_id, created_at) " +
            "values(#{spaceId}, #{module}, #{sourceType}, #{sourceCourseId}, #{sourceItemType}, #{sourceItemId}, #{targetItemId}, #{mode}, #{operatorId}, now())")
    Boolean insertImportLog(@Param("spaceId") Long spaceId,
                            @Param("module") String module,
                            @Param("sourceType") String sourceType,
                            @Param("sourceCourseId") String sourceCourseId,
                            @Param("sourceItemType") String sourceItemType,
                            @Param("sourceItemId") String sourceItemId,
                            @Param("targetItemId") String targetItemId,
                            @Param("mode") String mode,
                            @Param("operatorId") String operatorId);

    @Select("<script>" +
            "select courseware_id as coursewareId, folder_id as folderId, title, item_type as itemType, visibility, url, " +
            "original_name as originalName, stored_name as storedName, relative_path as relativePath, size, content_type as contentType, " +
            "created_by as createdBy, created_at as createdAt " +
            "from prepare_space_courseware " +
            "where prepare_space_id=#{spaceId} " +
            "and (visibility='ALL_MEMBERS' or created_by=#{accountId}) " +
            "<if test='folderId != null'> and folder_id=#{folderId}</if> " +
            "<if test='keyword != null and keyword != \"\"'> and title like concat('%',#{keyword},'%')</if> " +
            "order by created_at desc, courseware_id desc" +
            "</script>")
    List<Courseware> findCoursewares(@Param("spaceId") Long spaceId,
                                     @Param("accountId") String accountId,
                                     @Param("folderId") Long folderId,
                                     @Param("keyword") String keyword);

    @Select("select courseware_id as coursewareId, folder_id as folderId, title, item_type as itemType, visibility, url, " +
            "original_name as originalName, stored_name as storedName, relative_path as relativePath, size, content_type as contentType, " +
            "created_by as createdBy, created_at as createdAt " +
            "from prepare_space_courseware where prepare_space_id=#{spaceId} and courseware_id=#{coursewareId} limit 1")
    Courseware findCoursewareById(@Param("spaceId") Long spaceId, @Param("coursewareId") Long coursewareId);

    @Insert("insert into prepare_space_courseware(prepare_space_id, folder_id, title, item_type, visibility, url, original_name, stored_name, relative_path, size, content_type, created_by, created_at) " +
            "values(#{spaceId}, #{courseware.folderId}, #{courseware.title}, #{courseware.itemType}, #{courseware.visibility}, #{courseware.url}, " +
            "#{courseware.originalName}, #{courseware.storedName}, #{courseware.relativePath}, #{courseware.size}, #{courseware.contentType}, #{createdBy}, now())")
    @Options(useGeneratedKeys = true, keyProperty = "courseware.coursewareId", keyColumn = "courseware_id")
    Boolean insertCourseware(@Param("spaceId") Long spaceId,
                             @Param("createdBy") String createdBy,
                             @Param("courseware") Courseware courseware);

    @Delete("delete from prepare_space_courseware where prepare_space_id=#{spaceId} and courseware_id=#{coursewareId}")
    Integer deleteCourseware(@Param("spaceId") Long spaceId, @Param("coursewareId") Long coursewareId);

    @Select("<script>" +
            "select attachment_id as attachmentId, category, folder_id as folderId, original_name as originalName, stored_name as storedName, " +
            "relative_path as relativePath, size, content_type as contentType, created_by as createdBy, created_at as createdAt " +
            "from prepare_space_material_attachment " +
            "where prepare_space_id=#{spaceId} " +
            "<if test='category != null'> and category=#{category}</if> " +
            "<if test='folderId != null'> and folder_id=#{folderId}</if> " +
            "<if test='keyword != null and keyword != \"\"'> and original_name like concat('%',#{keyword},'%')</if> " +
            "order by created_at desc, attachment_id desc" +
            "</script>")
    List<MaterialAttachment> findMaterialAttachments(@Param("spaceId") Long spaceId,
                                                     @Param("category") String category,
                                                     @Param("folderId") Long folderId,
                                                     @Param("keyword") String keyword);

    @Select("select attachment_id as attachmentId, category, folder_id as folderId, original_name as originalName, stored_name as storedName, " +
            "relative_path as relativePath, size, content_type as contentType, created_by as createdBy, created_at as createdAt " +
            "from prepare_space_material_attachment " +
            "where prepare_space_id=#{spaceId} and attachment_id=#{attachmentId} limit 1")
    MaterialAttachment findMaterialAttachmentById(@Param("spaceId") Long spaceId, @Param("attachmentId") Long attachmentId);

    @Select("select attachment_id as attachmentId, category, folder_id as folderId, original_name as originalName, stored_name as storedName, " +
            "relative_path as relativePath, size, content_type as contentType, created_by as createdBy, created_at as createdAt " +
            "from prepare_space_material_attachment " +
            "where prepare_space_id=#{spaceId} and folder_id=#{folderId} order by created_at desc, attachment_id desc")
    List<MaterialAttachment> findMaterialAttachmentsByFolder(@Param("spaceId") Long spaceId, @Param("folderId") Long folderId);

    @Insert("insert into prepare_space_material_attachment(prepare_space_id, category, folder_id, original_name, stored_name, relative_path, size, content_type, created_by, created_at) " +
            "values(#{spaceId}, #{attachment.category}, #{attachment.folderId}, #{attachment.originalName}, #{attachment.storedName}, #{attachment.relativePath}, #{attachment.size}, #{attachment.contentType}, #{createdBy}, now())")
    @Options(useGeneratedKeys = true, keyProperty = "attachment.attachmentId", keyColumn = "attachment_id")
    Boolean insertMaterialAttachment(@Param("spaceId") Long spaceId,
                                    @Param("createdBy") String createdBy,
                                    @Param("attachment") MaterialAttachment attachment);

    @Delete("delete from prepare_space_material_attachment where prepare_space_id=#{spaceId} and attachment_id=#{attachmentId}")
    Integer deleteMaterialAttachment(@Param("spaceId") Long spaceId, @Param("attachmentId") Long attachmentId);

    @Select("<script>" +
            "select link_id as linkId, category, folder_id as folderId, title, url, created_by as createdBy, created_at as createdAt " +
            "from prepare_space_material_link " +
            "where prepare_space_id=#{spaceId} " +
            "<if test='category != null'> and category=#{category}</if> " +
            "<if test='folderId != null'> and folder_id=#{folderId}</if> " +
            "<if test='keyword != null and keyword != \"\"'> and title like concat('%',#{keyword},'%')</if> " +
            "order by created_at desc, link_id desc" +
            "</script>")
    List<MaterialLink> findMaterialLinks(@Param("spaceId") Long spaceId,
                                         @Param("category") String category,
                                         @Param("folderId") Long folderId,
                                         @Param("keyword") String keyword);

    @Select("select link_id as linkId, category, folder_id as folderId, title, url, created_by as createdBy, created_at as createdAt " +
            "from prepare_space_material_link where prepare_space_id=#{spaceId} and link_id=#{linkId} limit 1")
    MaterialLink findMaterialLinkById(@Param("spaceId") Long spaceId, @Param("linkId") Long linkId);

    @Select("select link_id as linkId, category, folder_id as folderId, title, url, created_by as createdBy, created_at as createdAt " +
            "from prepare_space_material_link where prepare_space_id=#{spaceId} and folder_id=#{folderId} order by created_at desc, link_id desc")
    List<MaterialLink> findMaterialLinksByFolder(@Param("spaceId") Long spaceId, @Param("folderId") Long folderId);

    @Insert("insert into prepare_space_material_link(prepare_space_id, category, folder_id, title, url, created_by, created_at) " +
            "values(#{spaceId}, #{link.category}, #{link.folderId}, #{link.title}, #{link.url}, #{createdBy}, now())")
    @Options(useGeneratedKeys = true, keyProperty = "link.linkId", keyColumn = "link_id")
    Boolean insertMaterialLink(@Param("spaceId") Long spaceId,
                              @Param("createdBy") String createdBy,
                              @Param("link") MaterialLink link);

    @Delete("delete from prepare_space_material_link where prepare_space_id=#{spaceId} and link_id=#{linkId}")
    Integer deleteMaterialLink(@Param("spaceId") Long spaceId, @Param("linkId") Long linkId);

    @Select("select topic_id as topicId, folder_id as folderId, author_id as authorId, author_name as authorName, title, content, is_anonymous as isAnonymous, " +
            "is_pinned as isPinned, is_locked as isLocked, reply_count as replyCount, create_time as createTime, update_time as updateTime " +
            "from prepare_space_topic where prepare_space_id=#{spaceId} order by is_pinned desc, update_time desc")
    List<Topic> findTopics(@Param("spaceId") Long spaceId);

    @Select("select topic_id as topicId, folder_id as folderId, author_id as authorId, author_name as authorName, title, content, is_anonymous as isAnonymous, " +
            "is_pinned as isPinned, is_locked as isLocked, reply_count as replyCount, create_time as createTime, update_time as updateTime " +
            "from prepare_space_topic where prepare_space_id=#{spaceId} and topic_id=#{topicId} limit 1")
    Topic findTopicById(@Param("spaceId") Long spaceId, @Param("topicId") String topicId);

    @Insert("insert into prepare_space_topic(topic_id, prepare_space_id, folder_id, author_id, author_name, title, content, is_anonymous, is_pinned, is_locked, reply_count, create_time, update_time) " +
            "values(#{topicId}, #{spaceId}, #{folderId}, #{authorId}, #{authorName}, #{title}, #{content}, #{isAnonymous}, #{isPinned}, #{isLocked}, #{replyCount}, now(), now())")
    Boolean insertTopic(@Param("topicId") String topicId,
                        @Param("spaceId") Long spaceId,
                        @Param("folderId") Long folderId,
                        @Param("authorId") String authorId,
                        @Param("authorName") String authorName,
                        @Param("title") String title,
                        @Param("content") String content,
                        @Param("isAnonymous") Boolean isAnonymous,
                        @Param("isPinned") Boolean isPinned,
                        @Param("isLocked") Boolean isLocked,
                        @Param("replyCount") Integer replyCount);

    @Update("update prepare_space_topic set title=#{title}, content=#{content}, update_time=now() where prepare_space_id=#{spaceId} and topic_id=#{topicId}")
    Integer updateTopic(@Param("spaceId") Long spaceId, @Param("topicId") String topicId, @Param("title") String title, @Param("content") String content);

    @Delete("delete from prepare_space_topic where prepare_space_id=#{spaceId} and topic_id=#{topicId}")
    Integer deleteTopic(@Param("spaceId") Long spaceId, @Param("topicId") String topicId);

    @Update("update prepare_space_topic set is_pinned=#{isPinned}, update_time=now() where prepare_space_id=#{spaceId} and topic_id=#{topicId}")
    Integer updateTopicPin(@Param("spaceId") Long spaceId, @Param("topicId") String topicId, @Param("isPinned") Boolean isPinned);

    @Update("update prepare_space_topic set is_locked=#{isLocked}, update_time=now() where prepare_space_id=#{spaceId} and topic_id=#{topicId}")
    Integer updateTopicLock(@Param("spaceId") Long spaceId, @Param("topicId") String topicId, @Param("isLocked") Boolean isLocked);

    @Select("select reply_id as replyId, topic_id as topicId, author_id as authorId, author_name as authorName, content, is_anonymous as isAnonymous, create_time as createTime " +
            "from prepare_space_topic_reply where prepare_space_id=#{spaceId} and topic_id=#{topicId} order by create_time asc, reply_id asc")
    List<Reply> findReplies(@Param("spaceId") Long spaceId, @Param("topicId") String topicId);

    @Select("select reply_id as replyId, topic_id as topicId, author_id as authorId, author_name as authorName, content, is_anonymous as isAnonymous, create_time as createTime " +
            "from prepare_space_topic_reply where prepare_space_id=#{spaceId} and reply_id=#{replyId} limit 1")
    Reply findReplyById(@Param("spaceId") Long spaceId, @Param("replyId") String replyId);

    @Insert("insert into prepare_space_topic_reply(reply_id, prepare_space_id, topic_id, author_id, author_name, content, is_anonymous, create_time) " +
            "values(#{replyId}, #{spaceId}, #{topicId}, #{authorId}, #{authorName}, #{content}, #{isAnonymous}, now())")
    Boolean insertReply(@Param("replyId") String replyId,
                        @Param("spaceId") Long spaceId,
                        @Param("topicId") String topicId,
                        @Param("authorId") String authorId,
                        @Param("authorName") String authorName,
                        @Param("content") String content,
                        @Param("isAnonymous") Boolean isAnonymous);

    @Delete("delete from prepare_space_topic_reply where prepare_space_id=#{spaceId} and reply_id=#{replyId}")
    Integer deleteReply(@Param("spaceId") Long spaceId, @Param("replyId") String replyId);

    @Update("update prepare_space_topic set reply_count=reply_count+1, update_time=now() where prepare_space_id=#{spaceId} and topic_id=#{topicId}")
    Integer incrementReplyCount(@Param("spaceId") Long spaceId, @Param("topicId") String topicId);

    @Update("update prepare_space_topic set reply_count=case when reply_count>0 then reply_count-1 else 0 end, update_time=now() where prepare_space_id=#{spaceId} and topic_id=#{topicId}")
    Integer decrementReplyCount(@Param("spaceId") Long spaceId, @Param("topicId") String topicId);

    @Select("select assignment_id as assignmentId, folder_id as folderId, title, deadline, content, total_score as totalScore, created_by as accountId " +
            "from prepare_space_assignment where prepare_space_id=#{spaceId} order by update_time desc, create_time desc")
    List<Assignment> findAssignments(@Param("spaceId") Long spaceId);

    @Select("select assignment_id as assignmentId from prepare_space_assignment where prepare_space_id=#{spaceId} and folder_id=#{folderId}")
    List<Assignment> findAssignmentsByFolder(@Param("spaceId") Long spaceId, @Param("folderId") Long folderId);

    @Select("select assignment_id as assignmentId, folder_id as folderId, title, deadline, content, total_score as totalScore, created_by as accountId " +
            "from prepare_space_assignment where prepare_space_id=#{spaceId} and assignment_id=#{assignmentId} limit 1")
    Assignment findAssignmentById(@Param("spaceId") Long spaceId, @Param("assignmentId") String assignmentId);

    @Insert("insert into prepare_space_assignment(assignment_id, prepare_space_id, folder_id, title, deadline, content, total_score, created_by, create_time, update_time) " +
            "values(#{assignmentId}, #{spaceId}, #{folderId}, #{title}, #{deadline}, #{content}, #{totalScore}, #{createdBy}, now(), now())")
    Boolean insertAssignment(@Param("assignmentId") String assignmentId,
                             @Param("spaceId") Long spaceId,
                             @Param("folderId") Long folderId,
                             @Param("title") String title,
                             @Param("deadline") String deadline,
                             @Param("content") String content,
                             @Param("totalScore") Integer totalScore,
                             @Param("createdBy") String createdBy);

    @Delete("delete from prepare_space_assignment where prepare_space_id=#{spaceId} and assignment_id=#{assignmentId}")
    Integer deleteAssignment(@Param("spaceId") Long spaceId, @Param("assignmentId") String assignmentId);

    @Delete("delete from prepare_space_assignment_submission where prepare_space_id=#{spaceId} and assignment_id=#{assignmentId}")
    Integer deleteAssignmentSubmissionsByAssignment(@Param("spaceId") Long spaceId, @Param("assignmentId") String assignmentId);

    @Insert("insert into prepare_space_assignment_submission(submission_id, prepare_space_id, assignment_id, submitter_id, submit_content, " +
            "file_name, file_stored_name, file_size, file_content_type, submitted_at) " +
            "values(#{submissionId}, #{spaceId}, #{assignmentId}, #{submitterId}, #{submitContent}, #{fileName}, #{fileStoredName}, #{fileSize}, #{fileContentType}, now())")
    Boolean insertAssignmentSubmission(@Param("submissionId") String submissionId,
                                      @Param("spaceId") Long spaceId,
                                      @Param("assignmentId") String assignmentId,
                                      @Param("submitterId") String submitterId,
                                      @Param("submitContent") String submitContent,
                                      @Param("fileName") String fileName,
                                      @Param("fileStoredName") String fileStoredName,
                                      @Param("fileSize") Long fileSize,
                                      @Param("fileContentType") String fileContentType);

    @Select("select submission_id as submissionId, assignment_id as assignmentId, submitter_id as accountId, submit_content as submitContent, submitted_at as submittedAt, " +
            "file_name as fileName, file_stored_name as fileStoredName, file_size as fileSize, file_content_type as fileContentType " +
            "from prepare_space_assignment_submission where prepare_space_id=#{spaceId} and assignment_id=#{assignmentId} order by submitted_at desc")
    List<Assignment> findAssignmentSubmissions(@Param("spaceId") Long spaceId, @Param("assignmentId") String assignmentId);

    @Select("select submission_id as submissionId, assignment_id as assignmentId, submitter_id as accountId, submit_content as submitContent, submitted_at as submittedAt, " +
            "file_name as fileName, file_stored_name as fileStoredName, file_size as fileSize, file_content_type as fileContentType " +
            "from prepare_space_assignment_submission where prepare_space_id=#{spaceId} and submission_id=#{submissionId} limit 1")
    Assignment findSubmissionById(@Param("spaceId") Long spaceId, @Param("submissionId") String submissionId);

    @Delete("delete from prepare_space_courseware where prepare_space_id=#{spaceId} and folder_id=#{folderId}")
    Integer deleteCoursewareByFolder(@Param("spaceId") Long spaceId, @Param("folderId") Long folderId);

    @Delete("delete from prepare_space_topic where prepare_space_id=#{spaceId} and folder_id=#{folderId}")
    Integer deleteTopicsByFolder(@Param("spaceId") Long spaceId, @Param("folderId") Long folderId);
}
