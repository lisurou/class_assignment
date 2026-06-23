package org.example.classAssignment.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.classAssignment.pojo.MaterialAttachment;
import org.example.classAssignment.pojo.MaterialFolder;
import org.example.classAssignment.pojo.MaterialLink;

import java.util.List;

@Mapper
public interface MaterialMapper {
    @Select("select * from course_material_folder where course_id=#{courseId} and category=#{category}")
    List<MaterialFolder> findFoldersByCourseId(String courseId, String category);

    @Select("select * from course_material_folder where folder_id=#{folderId} and course_id=#{courseId}")
    MaterialFolder findFolderById(String courseId, Long folderId);

    @Insert("insert into course_material_folder(course_id,category,parent_id,name,created_by,created_at) values(#{courseId},#{category},#{parentId},#{name},#{createdBy},now())")
    @Options(useGeneratedKeys = true, keyProperty = "folderId", keyColumn = "folder_id")
    Boolean insertFolder(MaterialFolder folder);

    @Update("update course_material_folder set parent_id=#{parentId} where course_id=#{courseId} and folder_id=#{folderId}")
    Integer updateFolderParent(String courseId, Long folderId, Long parentId);

    @Delete("delete from course_material_folder where course_id=#{courseId} and folder_id=#{folderId}")
    Integer deleteFolder(String courseId, Long folderId);

    @Select("select * from course_material_attachment where course_id=#{courseId} and category=#{category}")
    List<MaterialAttachment> findAttachmentsByCourseId(String courseId, String category);

    @Select("select * from course_material_attachment where course_id=#{courseId} and folder_id=#{folderId}")
    List<MaterialAttachment> findAttachmentsByFolder(String courseId, Long folderId);

    @Select("select * from course_material_attachment where course_id=#{courseId} and attachment_id=#{attachmentId}")
    MaterialAttachment findAttachmentById(String courseId, Long attachmentId);

    @Insert("insert into course_material_attachment(course_id,category,folder_id,original_name,stored_name,relative_path,size,content_type,created_by,created_at) values(#{courseId},#{category},#{folderId},#{originalName},#{storedName},#{relativePath},#{size},#{contentType},#{createdBy},now())")
    @Options(useGeneratedKeys = true, keyProperty = "attachmentId", keyColumn = "attachment_id")
    Boolean insertAttachment(MaterialAttachment attachment);

    @Delete("delete from course_material_attachment where course_id=#{courseId} and attachment_id=#{attachmentId}")
    Integer deleteAttachment(String courseId, Long attachmentId);

    @Select("select * from course_material_link where course_id=#{courseId} and category=#{category}")
    List<MaterialLink> findLinksByCourseId(String courseId, String category);

    @Select("select * from course_material_link where course_id=#{courseId} and folder_id=#{folderId}")
    List<MaterialLink> findLinksByFolder(String courseId, Long folderId);

    @Select("select * from course_material_link where course_id=#{courseId} and link_id=#{linkId}")
    MaterialLink findLinkById(String courseId, Long linkId);

    @Insert("insert into course_material_link(course_id,category,folder_id,title,url,created_by,created_at) values(#{courseId},#{category},#{folderId},#{title},#{url},#{createdBy},now())")
    @Options(useGeneratedKeys = true, keyProperty = "linkId", keyColumn = "link_id")
    Boolean insertLink(MaterialLink link);

    @Delete("delete from course_material_link where course_id=#{courseId} and link_id=#{linkId}")
    Integer deleteLink(String courseId, Long linkId);
}
