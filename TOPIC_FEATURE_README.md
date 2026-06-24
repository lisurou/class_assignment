# 话题讨论功能实现说明

## 功能概述

本实现为课堂派系统添加了完整的话题讨论功能，支持以下特性：

### 核心功能

1. **发起话题**
   - 教师和学生都可以发起话题
   - 支持匿名发布（选择后作者显示为"匿名用户"）
   - 话题包含标题和内容

2. **话题列表**
   - 按课程显示所有话题
   - 置顶话题显示在最前面
   - 显示话题标题、作者、发布时间、回复数

3. **话题详情**
   - 查看话题完整内容
   - 查看所有回复
   - 支持发表回复（可匿名）

4. **教师权限**
   - 置顶/取消置顶话题
   - 锁定/解锁话题（锁定后无法回复）
   - 编辑和删除任何话题
   - 删除任何回复

5. **学生权限**
   - 编辑和删除自己发起的话题
   - 删除自己发表的回复

---

## 数据库表结构

### topic 表（话题表）

```sql
CREATE TABLE topic (
    topic_id VARCHAR(50) PRIMARY KEY,
    course_id VARCHAR(50) NOT NULL,
    author_id VARCHAR(50) NOT NULL,
    author_name VARCHAR(100),
    title VARCHAR(200) NOT NULL,
    content TEXT,
    is_anonymous BOOLEAN DEFAULT FALSE,
    is_pinned BOOLEAN DEFAULT FALSE,
    is_locked BOOLEAN DEFAULT FALSE,
    reply_count INT DEFAULT 0,
    create_time DATETIME,
    update_time DATETIME,
    INDEX idx_course_id (course_id),
    INDEX idx_author_id (author_id)
);
```

### reply 表（回复表）

```sql
CREATE TABLE reply (
    reply_id VARCHAR(50) PRIMARY KEY,
    topic_id VARCHAR(50) NOT NULL,
    author_id VARCHAR(50) NOT NULL,
    author_name VARCHAR(100),
    content TEXT,
    is_anonymous BOOLEAN DEFAULT FALSE,
    create_time DATETIME,
    INDEX idx_topic_id (topic_id),
    INDEX idx_author_id (author_id),
    FOREIGN KEY (topic_id) REFERENCES topic(topic_id) ON DELETE CASCADE
);
```

---

## 后端 API 接口

### 话题相关接口

1. **创建话题**
   - 端点: `POST /topic/create`
   - 参数:
     ```json
     {
       "courseId": "课程ID",
       "authorId": "作者账号",
       "authorName": "作者姓名",
       "title": "话题标题",
       "content": "话题内容",
       "isAnonymous": false
     }
     ```

2. **获取话题列表**
   - 端点: `POST /topic/list`
   - 参数:
     ```json
     {
       "courseId": "课程ID"
     }
     ```
   - 返回: 话题列表，置顶话题在前

3. **获取话题详情**
   - 端点: `POST /topic/detail`
   - 参数:
     ```json
     {
       "topicId": "话题ID"
     }
     ```
   - 返回: 话题详情和回复列表

4. **更新话题**
   - 端点: `POST /topic/update`
   - 参数:
     ```json
     {
       "topicId": "话题ID",
       "authorId": "作者账号",
       "title": "新标题",
       "content": "新内容"
     }
     ```
   - 限制: 只能编辑自己的话题

5. **删除话题**
   - 端点: `POST /topic/delete`
   - 参数:
     ```json
     {
       "topicId": "话题ID",
       "authorId": "操作者账号",
       "identity": "老师/学生"
     }
     ```
   - 权限: 教师可删除任何话题，学生只能删除自己的

6. **置顶话题**
   - 端点: `POST /topic/pin`
   - 参数:
     ```json
     {
       "topicId": "话题ID",
        "isPinned": true
     }
     ```
   - 权限: 仅教师可操作

7. **锁定话题**
   - 端点: `POST /topic/lock`
   - 参数:
     ```json
     {
       "topicId": "话题ID",
        "isLocked": true
     }
     ```
   - 权限: 仅教师可操作

### 回复相关接口

1. **添加回复**
   - 端点: `POST /topic/reply/add`
   - 参数:
     ```json
     {
       "topicId": "话题ID",
       "authorId": "作者账号",
       "authorName": "作者姓名",
       "content": "回复内容",
       "isAnonymous": false
     }
     ```

2. **删除回复**
   - 端点: `POST /topic/reply/delete`
   - 参数:
     ```json
     {
       "replyId": "回复ID",
       "authorId": "操作者账号",
       "identity": "老师/学生"
     }
     ```
   - 权限: 教师可删除任何回复，学生只能删除自己的

---

## 前端组件说明

### TopicPage.vue
- 话题列表页面
- 显示当前课程的所有话题
- 支持发起新话题
- 教师可置顶、锁定、删除话题

### CreateTopicDialog.vue
- 创建话题的对话框
- 输入标题和内容
- 选择是否匿名发布

### TopicDetailDialog.vue
- 话题详情对话框
- 显示话题完整内容和所有回复
- 支持发表回复
- 显示回复列表，支持删除

---

## 部署步骤

### 1. 创建数据库表

执行 `topic_database.sql` 文件中的 SQL 语句：

```bash
mysql -u username -p database_name < topic_database.sql
```

### 2. 后端配置

`@MapperScan` 已在启动类 `ClassAssignmentApplication.java` 中配置，无需额外添加。

### 3. 前端配置

组件已自动集成到 CoursePage.vue，无需额外配置。

### 4. 启动应用

```bash
# 后端
cd class_assignment
mvn spring-boot:run

# 前端
cd class_assignment_ui
npm run serve
```

---

## 使用流程

### 教师端

1. 进入课程详情页
2. 点击"话题"标签
3. 可以：
   - 点击"发起话题"创建新话题
   - 点击话题查看详情和回复
   - 点击"置顶"将话题置顶
   - 点击"锁定"禁止学生回复
   - 点击"删除"删除话题或回复

### 学生端

1. 进入课程详情页
2. 点击"话题"标签
3. 可以：
   - 点击"发起话题"创建新话题（可选匿名）
   - 点击话题查看详情和回复
   - 在话题详情页发表回复（可选匿名）
   - 编辑或删除自己创建的话题
   - 删除自己发表的回复

---

## 注意事项

1. **权限控制**
   - 教师拥有所有权限
   - 学生只能管理自己创建的内容
   - 匿名发布时作者显示为"匿名用户"

2. **话题锁定**
   - 锁定的话题无法发表新回复
   - 锁定前的回复仍可查看
   - 只有教师可以锁定/解锁话题

3. **数据删除**
   - 删除话题会同时删除所有回复
   - 回复计数会自动更新

4. **时间显示**
   - 所有时间格式: YYYY-MM-DD HH:mm
   - 话题列表按创建时间降序排列

---

## 文件清单

### 后端文件

```
class_assignment/
├── src/main/java/org/example/classAssignment/
│   ├── pojo/
│   │   ├── Topic.java          # 话题实体
│   │   ├── Reply.java          # 回复实体
│   │   └── Result.java         # 已更新，添加topic和reply字段
│   ├── mapper/
│   │   └── TopicMapper.java    # 话题数据库操作
│   ├── service/
│   │   ├── TopicServiceInterface.java  # 服务接口
│   │   └── TopicService.java           # 服务实现
│   └── controller/
│       └── TopicController.java  # REST接口
└── topic_database.sql           # 数据库建表脚本
```

### 前端文件

```
class_assignment_ui/
└── src/
    ├── components/
    │   ├── TopicPage.vue              # 话题列表页面
    │   ├── CreateTopicDialog.vue      # 创建话题对话框
    │   └── TopicDetailDialog.vue      # 话题详情对话框
    └── pages/
        └── CoursePage.vue             # 已更新，集成TopicPage
```

---

## 功能验证

### 测试用例

1. **教师创建话题**
   - 登录教师账号
   - 进入课程详情
   - 点击"话题"标签
   - 点击"发起话题"
   - 填写标题和内容
   - 点击"发布"
   ✓ 话题应出现在列表中

2. **教师置顶话题**
   - 在话题列表中
   - 点击话题的"置顶"按钮
   ✓ 话题应显示"置顶"标签并移到列表顶部

3. **学生创建匿名话题**
   - 登录学生账号
   - 进入课程详情
   - 点击"话题"标签
   - 点击"发起话题"
   - 勾选"匿名发布"
   - 填写标题和内容
   - 点击"发布"
   ✓ 话题作者应显示为"匿名用户"

4. **发表回复**
   - 点击任意话题查看详情
   - 在回复框输入内容
   - 点击"回复"
   ✓ 回复应显示在回复列表中

5. **删除话题**
   - 教师：可删除任何话题
   - 学生：只能删除自己创建的话题
   ✓ 删除后话题应从列表中消失

6. **锁定话题**
   - 教师点击话题的"锁定"按钮
   ✓ 学生应无法再发表新回复

---

## 常见问题

**Q: 如何修改话题？**
A: 目前实现中只支持删除话题重新创建，不支持编辑已发布的话题内容。

**Q: 匿名发布后能否删除话题？**
A: 可以，系统通过 authorId 追踪真实作者，即使匿名也可由作者删除。

**Q: 锁定话题后如何解锁？**
A: 教师再次点击"锁定"按钮即可解锁。

**Q: 删除话题会影响回复吗？**
A: 会，删除话题会同时删除该话题下的所有回复。

---

## 技术实现亮点

1. **MyBatis 注解方式**
   - 使用注解而非 XML 配置
   - 简洁明了，易于维护

2. **UUID 生成唯一ID**
   - 使用 UUID 生成话题和回复的唯一标识
   - 避免主键冲突

3. **权限分离**
   - 后端验证用户身份和权限
   - 前端根据身份显示不同操作按钮

4. **事务处理**
   - 删除话题时级联删除回复
   - 回复计数自动维护

5. **响应式设计**
   - 前端组件完全响应式
   - 支持移动端访问

---

## 扩展建议

1. 添加话题搜索功能
2. 支持话题标签分类
3. 添加回复点赞功能
4. 支持富文本编辑
5. 添加消息通知
6. 支持附件上传

---

## 版本信息

- 版本: 1.0.0
- 更新日期: 2026-06-22
- 适用系统: 课堂派在线教育平台

