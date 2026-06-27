package org.example.classAssignment.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.classAssignment.pojo.PrepareSpace;
import org.example.classAssignment.pojo.PrepareSpaceMember;
import org.example.classAssignment.pojo.PrepareSpaceOperationLog;

import java.util.List;

@Mapper
public interface PrepareSpaceMapper {
    @Insert("insert into prepare_space(name, space_type, owner_id, course_id, description, status, created_at, updated_at) " +
            "values(#{name}, #{spaceType}, #{ownerId}, #{courseId}, #{description}, #{status}, now(), now())")
    @Options(useGeneratedKeys = true, keyProperty = "prepareSpaceId", keyColumn = "prepare_space_id")
    Boolean insertPrepareSpace(PrepareSpace prepareSpace);

    @Select("select * from prepare_space where prepare_space_id=#{spaceId}")
    PrepareSpace findPrepareSpaceById(@Param("spaceId") Long spaceId);

    @Select("select distinct ps.* from prepare_space ps " +
            "left join prepare_space_member psm on ps.prepare_space_id = psm.prepare_space_id and psm.status='正常' " +
            "where ps.status='正常' and (ps.owner_id=#{accountId} or psm.account_id=#{accountId}) " +
            "order by ps.updated_at desc, ps.created_at desc")
    List<PrepareSpace> findVisibleSpaces(@Param("accountId") String accountId);

    @Update("update prepare_space set name=#{name}, description=#{description}, updated_at=now() where prepare_space_id=#{spaceId}")
    Integer updatePrepareSpace(@Param("spaceId") Long spaceId, @Param("name") String name, @Param("description") String description);

    @Update("update prepare_space set owner_id=#{ownerId}, updated_at=now() where prepare_space_id=#{spaceId}")
    Integer updatePrepareSpaceOwner(@Param("spaceId") Long spaceId, @Param("ownerId") String ownerId);

    @Update("update prepare_space set status='删除', updated_at=now() where prepare_space_id=#{spaceId}")
    Integer deletePrepareSpace(@Param("spaceId") Long spaceId);

    @Insert("insert into prepare_space_member(prepare_space_id, account_id, role, status, joined_at) " +
            "values(#{prepareSpaceId}, #{accountId}, #{role}, #{status}, now())")
    @Options(useGeneratedKeys = true, keyProperty = "memberId", keyColumn = "member_id")
    Boolean insertMember(PrepareSpaceMember member);

    @Select("select * from prepare_space_member where prepare_space_id=#{spaceId} and status='正常' order by joined_at asc")
    List<PrepareSpaceMember> findMembersBySpaceId(@Param("spaceId") Long spaceId);

    @Select("select * from prepare_space_member where prepare_space_id=#{spaceId} and account_id=#{accountId} and status='正常' limit 1")
    PrepareSpaceMember findMemberByAccountId(@Param("spaceId") Long spaceId, @Param("accountId") String accountId);

    @Select("select * from prepare_space_member where member_id=#{memberId}")
    PrepareSpaceMember findMemberById(@Param("memberId") Long memberId);

    @Update("update prepare_space_member set role=#{role} where prepare_space_id=#{spaceId} and member_id=#{memberId} and status='正常'")
    Integer updateMemberRole(@Param("spaceId") Long spaceId, @Param("memberId") Long memberId, @Param("role") String role);

    @Update("update prepare_space_member set role=#{role} where prepare_space_id=#{spaceId} and account_id=#{accountId} and status='正常'")
    Integer updateMemberRoleByAccountId(@Param("spaceId") Long spaceId, @Param("accountId") String accountId, @Param("role") String role);

    @Update("update prepare_space_member set status='移除' where prepare_space_id=#{spaceId} and member_id=#{memberId} and status='正常'")
    Integer removeMember(@Param("spaceId") Long spaceId, @Param("memberId") Long memberId);

    @Insert("insert into prepare_space_operation_log(prepare_space_id, account_id, operation_type, operation_target, target_id, detail, created_at) " +
            "values(#{prepareSpaceId}, #{accountId}, #{operationType}, #{operationTarget}, #{targetId}, #{detail}, now())")
    @Options(useGeneratedKeys = true, keyProperty = "logId", keyColumn = "log_id")
    Boolean insertLog(PrepareSpaceOperationLog log);

    @Select("select * from prepare_space_operation_log where prepare_space_id=#{spaceId} order by created_at desc, log_id desc")
    List<PrepareSpaceOperationLog> findLogsBySpaceId(@Param("spaceId") Long spaceId);
}
