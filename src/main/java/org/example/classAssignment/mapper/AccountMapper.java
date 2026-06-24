package org.example.classAssignment.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.classAssignment.pojo.Account;
import org.example.classAssignment.pojo.Assignment;
import org.example.classAssignment.pojo.Course;

import java.util.List;

@Mapper
public interface AccountMapper {
    @Select("select * from account where account_id= #{accountId}")
    Account findByAccountId(String accountId);

    @Select("select * from account where phone=#{phone}")
    Account findByPhone(String phone);

    @Insert("insert into account(account_id, password, phone, name, school, identity, student_id) " +
            "values(#{accountId}, #{password}, #{phone}, #{name}, #{school}, #{identity}, #{studentId})")
    Boolean insertStudentAccount(Account account);

    @Insert("insert into account(account_id, password, phone, name, school, identity) " +
            "values(#{accountId}, #{password}, #{phone}, #{name}, #{school}, #{identity})")
    Boolean insertTeacherAccount(Account account);

    @Update("update account set identity=#{identity} where account_id=#{accountId}")
    Integer updateIdentity(Account account);

    @Update("update account set phone=#{phone} where account_id=#{accountId}")
    Integer updatePhone(Account account);

    @Update("update account set password=#{password} where account_id=#{accountId}")
    Integer updatePassword(Account account);

    @Update("update account set name=#{name},student_id=#{studentId},school=#{school},major=#{major},classes=#{classes},grade=#{grade},enrollment=#{enrollment} where account_id=#{accountId}")
    Integer updateBasicInformation(Account account);

    @Select("select * from course where id=#{id}")
    Course findByCourseId(String id);

    @Update("update account set learned=#{learned} where account_id=#{accountId}")
    Boolean updateLearned(String accountId, String learned);

    @Select("select learned from account where account_id=#{accountId}")
    String findLearned(String accountId);

    @Select("select taught from account where account_id=#{accountId}")
    String findTaught(String accountId);

    @Update("update account set taught=#{taught} where account_id=#{accountId}")
    Boolean updateTaught(String accountId, String taught);

    @Insert("insert into course(time,name,classes,id,number,teacher) values(#{time},#{name},#{classes},#{id},#{number},#{teacher})")
    Boolean insertCourse(Course course);

    @Select("select top from account where account_id=#{accountId}")
    String findTop(String accountId);

    @Update("update account set top=#{top} where account_id=#{accountId}")
    Boolean updateTop(String accountId, String top);
    @Select("select * from asssignment where account_id=#{accountId} and id= #{id}")
    List<Assignment> findAssignment(String accountId, String id);
    @Update("update asssignment set submit_content=#{submitContent} where account_id=#{accountId} and id=#{id} and assignment_id=#{assignmentId}")
    Boolean updateAssignment(String accountId, String id, String assignmentId,String submitContent);
    @Update("update asssignment set file_name=#{fileName}, file_stored_name=#{fileStoredName}, file_size=#{fileSize}, file_content_type=#{fileContentType} " +
            "where account_id=#{accountId} and id=#{id} and assignment_id=#{assignmentId}")
    Boolean updateAssignmentFileMeta(String accountId, String id, String assignmentId, String fileName, String fileStoredName, Long fileSize, String fileContentType);
    @Select("select * from asssignment where account_id=#{accountId} and id=#{id} and assignment_id=#{assignmentId}")
    Assignment findAssignmentById(String assignmentId);
    @Update("update asssignment set submit=#{submit} where account_id=#{accountId} and id=#{id} and assignment_id=#{assignmentId}")
    Boolean updateSubmit(String accountId,String id,String assignmentId, String submit);
    @Select("select * from asssignment where id=#{id} and assignment_id=#{assignmentId} order by account_id")
    List<Assignment> findAssignmentSubmissions(String id, String assignmentId);
    @Select("select * from asssignment where id=#{id} and assignment_id=#{assignmentId} and submit=#{submit}")
    List<Assignment> findSubmitAssignment(String id, String assignmentId, String submit);
    @Select("select file_name as fileName, file_stored_name as fileStoredName, file_size as fileSize, file_content_type as fileContentType " +
            "from asssignment where account_id=#{accountId} and id=#{id} and assignment_id=#{assignmentId}")
    Assignment findAssignmentFileMeta(String accountId, String id, String assignmentId);
    @Update("update asssignment set score=#{score},correct='已批改' where account_id=#{accountId} and id=#{id} and assignment_id=#{assignmentId}")
    Boolean updateScore(Integer score,String accountId,String id,String assignmentId);
    @Select("Select students from course where id=#{id}")
    String findStudents(String id);
    @Insert("insert into asssignment(account_id,id,assignment_id,title, deadline, assignment_type, content, total_score) " +
            "values(#{accountId},#{id},#{assignmentId},#{title}, #{deadline}, #{assignmentType}, #{content}, #{totalScore})")
    Boolean insertAssignment(String accountId, String id, String assignmentId, String title, String deadline, String assignmentType, String content, Integer totalScore);
    @Update("update asssignment set title=#{title}, deadline=#{deadline}, assignment_type=#{assignmentType}, content=#{content}, total_score=#{totalScore} " +
            "where id=#{id} and assignment_id=#{assignmentId}")
    Boolean updateCourseAssignment(String id, String assignmentId, String title, String deadline, String assignmentType, String content, Integer totalScore);
    @Delete("delete from asssignment where id=#{id} and assignment_id=#{assignmentId}")
    Boolean deleteCourseAssignment(String id, String assignmentId);
    @Select("select * from asssignment where assignment_id=#{assignmentId}")
    Boolean findByAssignmentId(String assignmentId);
    @Select("select assignment_id as assignmentId,id,title,deadline,assignment_type as assignmentType,content,total_score as totalScore " +
            "from asssignment where id=#{id} group by assignment_id,id,title,deadline,assignment_type,content,total_score")
    List<Assignment> findCourseAssignments(String id);
    @Select("select count(1) from asssignment where account_id=#{accountId} and id=#{id} and assignment_id=#{assignmentId}")
    Integer countAssignmentByStudent(@Param("accountId") String accountId, @Param("id") String id, @Param("assignmentId") String assignmentId);
    @Update("update course set students=#{students}where id=#{id}")
    Boolean updateStudents( String students,String id);
    
    // 归档相关方法
    @Select("select archived_learned from account where account_id=#{accountId}")
    String findArchivedLearned(String accountId);

    @Select("select archived_taught from account where account_id=#{accountId}")
    String findArchivedTaught(String accountId);

    @Update("update account set archived_learned=#{archivedLearned} where account_id=#{accountId}")
    Boolean updateArchivedLearned(String accountId, String archivedLearned);

    @Update("update account set archived_taught=#{archivedTaught} where account_id=#{accountId}")
    Boolean updateArchivedTaught(String accountId, String archivedTaught);

    @Update("update course set archived_by=#{archivedBy},archived_at=#{archivedAt} where id=#{id}")
    Boolean updateCourseArchiveStatus(String id, String archivedBy, String archivedAt);
}
