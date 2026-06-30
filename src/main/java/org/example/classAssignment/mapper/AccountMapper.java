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
import org.example.classAssignment.pojo.CourseNotification;

import java.util.List;

@Mapper
public interface AccountMapper {
    @Select("select * from account where account_id= #{accountId}")
    Account findByAccountId(String accountId);

    @Select("select * from account where phone=#{phone}")
    Account findByPhone(String phone);

    @Select("select a.* from account a join course c on c.id=#{courseId} " +
            "where find_in_set(a.account_id, c.students) order by a.student_id, a.account_id")
    List<Account> findCourseMembers(String courseId);

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

    @Update("update account set avatar_stored_name=#{avatarStoredName} where account_id=#{accountId}")
    Integer updateAvatarStoredName(String accountId, String avatarStoredName);

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
    @Update("update asssignment set file_name=null, file_stored_name=null, file_size=null, file_content_type=null " +
            "where account_id=#{accountId} and id=#{id} and assignment_id=#{assignmentId}")
    Boolean clearAssignmentFileMeta(String accountId, String id, String assignmentId);
    @Select("select * from asssignment where account_id=#{accountId} and id=#{id} and assignment_id=#{assignmentId}")
    Assignment findAssignmentById(String assignmentId);
    @Update("update asssignment set submit=#{submit} where account_id=#{accountId} and id=#{id} and assignment_id=#{assignmentId}")
    Boolean updateSubmit(String accountId,String id,String assignmentId, String submit);
    @Select("select a.*, ac.name as student_name, ac.student_id as student_id, a.teacher_comment as teacherComment " +
            "from asssignment a left join account ac on a.account_id = ac.account_id " +
            "where a.id=#{id} and a.assignment_id=#{assignmentId} and (ac.identity='学生' or ac.identity is null) order by ac.student_id, a.account_id")
    List<Assignment> findAssignmentSubmissions(String id, String assignmentId);
    @Select("select * from asssignment where id=#{id} and assignment_id=#{assignmentId} and submit=#{submit}")
    List<Assignment> findSubmitAssignment(String id, String assignmentId, String submit);
    @Select("select file_name as fileName, file_stored_name as fileStoredName, file_size as fileSize, file_content_type as fileContentType " +
            "from asssignment where account_id=#{accountId} and id=#{id} and assignment_id=#{assignmentId}")
    Assignment findAssignmentFileMeta(String accountId, String id, String assignmentId);
    @Update("update asssignment set score=#{score}, teacher_comment=#{teacherComment}, correct='已批改' " +
            "where account_id=#{accountId} and id=#{id} and assignment_id=#{assignmentId}")
    Integer updateScore(Integer score,String teacherComment,String accountId,String id,String assignmentId);
    @Select("Select students from course where id=#{id}")
    String findStudents(String id);
    @Insert("insert into asssignment(account_id,id,assignment_id,title, deadline, assignment_type, content, total_score, ai_enabled) " +
            "values(#{accountId},#{id},#{assignmentId},#{title}, #{deadline}, #{assignmentType}, #{content}, #{totalScore}, #{aiEnabled})")
    Boolean insertAssignment(String accountId, String id, String assignmentId, String title, String deadline, String assignmentType, String content, Integer totalScore, Boolean aiEnabled);
    @Update("update asssignment set title=#{title}, deadline=#{deadline}, assignment_type=#{assignmentType}, content=#{content}, total_score=#{totalScore}, ai_enabled=#{aiEnabled} " +
            "where id=#{id} and assignment_id=#{assignmentId}")
    Boolean updateCourseAssignment(String id, String assignmentId, String title, String deadline, String assignmentType, String content, Integer totalScore, Boolean aiEnabled);
    @Delete("delete from asssignment where id=#{id} and assignment_id=#{assignmentId}")
    Boolean deleteCourseAssignment(String id, String assignmentId);
    @Select("select * from asssignment where assignment_id=#{assignmentId}")
    Boolean findByAssignmentId(String assignmentId);
    @Select("select a.assignment_id as assignmentId, a.id, max(a.title) as title, max(a.deadline) as deadline, " +
            "max(a.assignment_type) as assignmentType, max(a.content) as content, max(a.total_score) as totalScore, " +
            "max(a.ai_enabled) as aiEnabled, " +
            "sum(case when ac.identity='学生' and a.correct='已批改' then 1 else 0 end) as correctedCount, " +
            "sum(case when ac.identity='学生' and a.submit='已提交' and (a.correct is null or a.correct<>'已批改') then 1 else 0 end) as pendingCount, " +
            "sum(case when ac.identity='学生' and (a.submit is null or a.submit<>'已提交') then 1 else 0 end) as missingCount " +
            "from asssignment a left join account ac on a.account_id = ac.account_id where a.id=#{id} " +
            "group by a.assignment_id, a.id")
    List<Assignment> findCourseAssignments(String id);
    @Select("select count(1) from asssignment where account_id=#{accountId} and id=#{id} and assignment_id=#{assignmentId}")
    Integer countAssignmentByStudent(@Param("accountId") String accountId, @Param("id") String id, @Param("assignmentId") String assignmentId);
    @Update("update asssignment set ai_enabled=#{aiEnabled} where id=#{id} and assignment_id=#{assignmentId}")
    Boolean updateAssignmentAiEnabled(String id, String assignmentId, Boolean aiEnabled);
    @Update("update asssignment set ai_score=#{aiScore}, ai_comment=#{aiComment} where account_id=#{accountId} and id=#{id} and assignment_id=#{assignmentId}")
    Boolean updateAssignmentAiReview(String accountId, String id, String assignmentId, Integer aiScore, String aiComment);
    @Update("update asssignment set ai_score=null, ai_comment=null where id=#{id} and assignment_id=#{assignmentId}")
    Boolean clearAssignmentAiReview(String id, String assignmentId);
    @Update("update course set students=#{students}where id=#{id}")
    Boolean updateStudents( String students,String id);

    @Insert("insert into course_notification(account_id, course_id, assignment_id, type, title, content, sender_name, read_status, created_at) " +
            "values(#{accountId}, #{courseId}, #{assignmentId}, #{type}, #{title}, #{content}, #{senderName}, #{readStatus}, now())")
    Integer insertNotification(String accountId, String courseId, String assignmentId, String type, String title, String content, String senderName, Boolean readStatus);

    @Select("select id, account_id as accountId, course_id as courseId, assignment_id as assignmentId, type, title, content, " +
            "sender_name as senderName, read_status as readStatus, created_at as createdAt " +
            "from course_notification where account_id=#{accountId} order by read_status asc, created_at desc, id desc")
    List<CourseNotification> findNotificationsByAccountId(String accountId);

    @Update("update course_notification set read_status=1 where id=#{notificationId} and account_id=#{accountId}")
    Integer markNotificationAsRead(Long notificationId, String accountId);

    @Update("update course_notification set read_status=1 where account_id=#{accountId} and read_status=0")
    Integer markAllNotificationsAsRead(String accountId);

    @Insert("insert into assignment_resource(course_id, assignment_id, file_name, file_stored_name, file_size, file_content_type, created_at) " +
            "values(#{id}, #{assignmentId}, #{fileName}, #{fileStoredName}, #{fileSize}, #{fileContentType}, now()) " +
            "on duplicate key update file_name=values(file_name), file_stored_name=values(file_stored_name), " +
            "file_size=values(file_size), file_content_type=values(file_content_type)")
    Integer upsertAssignmentResource(String id, String assignmentId, String fileName, String fileStoredName, Long fileSize, String fileContentType);

    @Delete("delete from assignment_resource where course_id=#{id} and assignment_id=#{assignmentId}")
    Integer deleteAssignmentResource(String id, String assignmentId);

    @Select("select file_name as attachmentName, file_stored_name as attachmentStoredName, file_size as attachmentSize, " +
            "file_content_type as attachmentContentType from assignment_resource where course_id=#{id} and assignment_id=#{assignmentId}")
    Assignment findAssignmentResource(String id, String assignmentId);
    
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

    @Delete("delete from course where id=#{id}")
    Integer deleteCourseById(String id);

    @Delete("delete from asssignment where id=#{id}")
    Integer deleteAssignmentsByCourseId(String id);

    @Delete("delete from assignment_resource where course_id=#{id}")
    Integer deleteAssignmentResourcesByCourseId(String id);

    @Delete("delete from course_notification where course_id=#{id}")
    Integer deleteNotificationsByCourseId(String id);
}
