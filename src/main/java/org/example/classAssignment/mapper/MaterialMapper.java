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
    @Select("select folder_id as folderId, owner_id as courseId, category, parent_id as parentId, name, created_by as createdBy, created_at as createdAt " +
            "from resource_folder where owner_type='COURSE' and owner_id=#{courseId} and module='MATERIAL' and category=#{category} order by created_at asc, folder_id asc")
    List<MaterialFolder> findFoldersByCourseId(String courseId, String category);

    @Select("select folder_id as folderId, owner_id as courseId, category, parent_id as parentId, name, created_by as createdBy, created_at as createdAt " +
            "from resource_folder where owner_type='COURSE' and owner_id=#{courseId} and module='MATERIAL' and folder_id=#{folderId} limit 1")
    MaterialFolder findFolderById(String courseId, Long folderId);

    @Insert("insert into resource_folder(owner_type,owner_id,module,category,parent_id,name,created_by,created_at) values('COURSE',#{courseId},'MATERIAL',#{category},#{parentId},#{name},#{createdBy},now())")
    @Options(useGeneratedKeys = true, keyProperty = "folderId", keyColumn = "folder_id")
    Boolean insertFolder(MaterialFolder folder);

    @Update("update resource_folder set parent_id=#{parentId} where owner_type='COURSE' and owner_id=#{courseId} and module='MATERIAL' and folder_id=#{folderId}")
    Integer updateFolderParent(String courseId, Long folderId, Long parentId);

    @Delete("delete from resource_folder where owner_type='COURSE' and owner_id=#{courseId} and module='MATERIAL' and folder_id=#{folderId}")
    Integer deleteFolder(String courseId, Long folderId);

    @Select("select item_id as attachment_id, course_id, category, folder_id, original_name, stored_name, relative_path, size, content_type, created_by, created_at " +
            "from course_material_item where course_id=#{courseId} and category=#{category} and item_type='FILE' order by created_at desc, item_id desc")
    List<MaterialAttachment> findAttachmentsByCourseId(String courseId, String category);

    @Select("select item_id as attachment_id, course_id, category, folder_id, original_name, stored_name, relative_path, size, content_type, created_by, created_at " +
            "from course_material_item where course_id=#{courseId} and folder_id=#{folderId} and item_type='FILE' order by created_at desc, item_id desc")
    List<MaterialAttachment> findAttachmentsByFolder(String courseId, Long folderId);

    @Select("select item_id as attachment_id, course_id, category, folder_id, original_name, stored_name, relative_path, size, content_type, created_by, created_at " +
            "from course_material_item where course_id=#{courseId} and item_id=#{attachmentId} and item_type='FILE'")
    MaterialAttachment findAttachmentById(String courseId, Long attachmentId);

    @Insert("insert into course_material_item(course_id,category,folder_id,item_type,title,original_name,stored_name,relative_path,size,content_type,created_by,created_at) " +
            "values(#{courseId},#{category},#{folderId},'FILE',#{originalName},#{originalName},#{storedName},#{relativePath},#{size},#{contentType},#{createdBy},now())")
    @Options(useGeneratedKeys = true, keyProperty = "attachmentId", keyColumn = "item_id")
    Boolean insertAttachment(MaterialAttachment attachment);

    @Delete("delete from course_material_item where course_id=#{courseId} and item_id=#{attachmentId} and item_type='FILE'")
    Integer deleteAttachment(String courseId, Long attachmentId);

    @Select("select item_id as link_id, course_id, category, folder_id, title, url, created_by, created_at " +
            "from course_material_item where course_id=#{courseId} and category=#{category} and item_type='LINK' order by created_at desc, item_id desc")
    List<MaterialLink> findLinksByCourseId(String courseId, String category);

    @Select("select item_id as link_id, course_id, category, folder_id, title, url, created_by, created_at " +
            "from course_material_item where course_id=#{courseId} and folder_id=#{folderId} and item_type='LINK' order by created_at desc, item_id desc")
    List<MaterialLink> findLinksByFolder(String courseId, Long folderId);

    @Select("select item_id as link_id, course_id, category, folder_id, title, url, created_by, created_at " +
            "from course_material_item where course_id=#{courseId} and item_id=#{linkId} and item_type='LINK'")
    MaterialLink findLinkById(String courseId, Long linkId);

    @Insert("insert into course_material_item(course_id,category,folder_id,item_type,title,url,created_by,created_at) " +
            "values(#{courseId},#{category},#{folderId},'LINK',#{title},#{url},#{createdBy},now())")
    @Options(useGeneratedKeys = true, keyProperty = "linkId", keyColumn = "item_id")
    Boolean insertLink(MaterialLink link);

    @Delete("delete from course_material_item where course_id=#{courseId} and item_id=#{linkId} and item_type='LINK'")
    Integer deleteLink(String courseId, Long linkId);
}
