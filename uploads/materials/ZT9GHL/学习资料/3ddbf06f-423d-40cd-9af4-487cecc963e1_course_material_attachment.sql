/*
 Navicat Premium Dump SQL

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 80043 (8.0.43)
 Source Host           : localhost:3306
 Source Schema         : class_assignment

 Target Server Type    : MySQL
 Target Server Version : 80043 (8.0.43)
 File Encoding         : 65001

 Date: 25/06/2026 14:29:45
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for course_material_attachment
-- ----------------------------
DROP TABLE IF EXISTS `course_material_attachment`;
CREATE TABLE `course_material_attachment`  (
  `attachment_id` bigint NOT NULL AUTO_INCREMENT,
  `course_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `category` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `folder_id` bigint NULL DEFAULT NULL,
  `original_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `stored_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `relative_path` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `size` bigint NOT NULL,
  `content_type` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`attachment_id`) USING BTREE,
  INDEX `idx_attach_course_category`(`course_id` ASC, `category` ASC) USING BTREE,
  INDEX `idx_attach_course_folder`(`course_id` ASC, `folder_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of course_material_attachment
-- ----------------------------
INSERT INTO `course_material_attachment` VALUES (2, 'C002', '学习资料', NULL, '实验3.docx', '56c05e5b-a1f6-42de-ba33-02428122c38b_实验3.docx', 'C002/学习资料/56c05e5b-a1f6-42de-ba33-02428122c38b_实验3.docx', 16635, 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 'T001', '2026-06-23 17:04:12');
INSERT INTO `course_material_attachment` VALUES (3, 'C002', '慕课资料', NULL, '12423020431谢欣实验10.docx', 'e3ef5268-5df3-4e5e-a10e-d3052c1795f1_12423020431谢欣实验10.docx', 'C002/慕课资料/e3ef5268-5df3-4e5e-a10e-d3052c1795f1_12423020431谢欣实验10.docx', 44466, 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 'T001', '2026-06-23 18:27:04');
INSERT INTO `course_material_attachment` VALUES (4, 'C002', '直播录像', NULL, '背景.jpg', '0148d9d0-68ae-4140-84e1-5286f4afbf69_背景.jpg', 'C002/直播录像/0148d9d0-68ae-4140-84e1-5286f4afbf69_背景.jpg', 128517, 'image/jpeg', 'T001', '2026-06-23 18:27:21');

-- ----------------------------
-- Table structure for course_material_folder
-- ----------------------------
DROP TABLE IF EXISTS `course_material_folder`;
CREATE TABLE `course_material_folder`  (
  `folder_id` bigint NOT NULL AUTO_INCREMENT,
  `course_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `category` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `parent_id` bigint NULL DEFAULT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `created_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`folder_id`) USING BTREE,
  INDEX `idx_folder_course_category`(`course_id` ASC, `category` ASC) USING BTREE,
  INDEX `idx_folder_course_parent`(`course_id` ASC, `parent_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of course_material_folder
-- ----------------------------
INSERT INTO `course_material_folder` VALUES (1, 'C002', '学习资料', 2, '111', 'T001', '2026-06-23 19:30:34');
INSERT INTO `course_material_folder` VALUES (2, 'C002', '学习资料', NULL, '222', 'T001', '2026-06-23 19:38:41');

-- ----------------------------
-- Table structure for course_material_link
-- ----------------------------
DROP TABLE IF EXISTS `course_material_link`;
CREATE TABLE `course_material_link`  (
  `link_id` bigint NOT NULL AUTO_INCREMENT,
  `course_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `category` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `folder_id` bigint NULL DEFAULT NULL,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `url` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `created_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`link_id`) USING BTREE,
  INDEX `idx_link_course_category`(`course_id` ASC, `category` ASC) USING BTREE,
  INDEX `idx_link_course_folder`(`course_id` ASC, `folder_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of course_material_link
-- ----------------------------
INSERT INTO `course_material_link` VALUES (1, 'C002', '学习资料', 1, '课堂派', 'https://www.ketangpai.com/#/main/classDetail?courseid=MDAwMDAwMDAwMLOcx92G36eyhctyoQ&courserole=1&submodulename=2,8', 'T001', '2026-06-23 19:32:11');
INSERT INTO `course_material_link` VALUES (2, 'C002', '学习资料', 1, '百度', 'https://www.baidu.com/', 'T001', '2026-06-23 19:33:25');

-- ----------------------------
-- Table structure for prepare_space
-- ----------------------------
DROP TABLE IF EXISTS `prepare_space`;
CREATE TABLE `prepare_space`  (
  `prepare_space_id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `space_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `owner_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `course_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `description` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '正常',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`prepare_space_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of prepare_space
-- ----------------------------

-- ----------------------------
-- Table structure for prepare_space_member
-- ----------------------------
DROP TABLE IF EXISTS `prepare_space_member`;
CREATE TABLE `prepare_space_member`  (
  `member_id` bigint NOT NULL AUTO_INCREMENT,
  `prepare_space_id` bigint NOT NULL,
  `account_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `role` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '正常',
  `joined_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`member_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of prepare_space_member
-- ----------------------------

-- ----------------------------
-- Table structure for prepare_space_operation_log
-- ----------------------------
DROP TABLE IF EXISTS `prepare_space_operation_log`;
CREATE TABLE `prepare_space_operation_log`  (
  `log_id` bigint NOT NULL AUTO_INCREMENT,
  `prepare_space_id` bigint NOT NULL,
  `account_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `operation_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `operation_target` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `target_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `detail` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`log_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of prepare_space_operation_log
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;
