-- =========================================
-- 1. GENDER
-- =========================================
CREATE TABLE `gender` (
                          `id`                INT AUTO_INCREMENT PRIMARY KEY,
                          `name`              VARCHAR(10) NOT NULL,
                          `created_at`        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          CONSTRAINT `uk_gender_name` UNIQUE (`name`)
);

CREATE INDEX `idx_gender_name` ON `gender`(`name`);


-- =========================================
-- 2. USER (일반 사용자 전용)
-- =========================================
CREATE TABLE `user` (
                        `id`                BIGINT AUTO_INCREMENT PRIMARY KEY,
                        `gender_id`         INT NOT NULL,
                        `input_id`          VARCHAR(30) NOT NULL,
                        `input_password`    VARCHAR(100) NOT NULL,
                        `age`               INT         NOT NULL,
                        `name`              VARCHAR(30) NOT NULL,
                        `nickname`          VARCHAR(30) NOT NULL,
                        `uuid`              VARCHAR(36) NOT NULL,
                        `status`            VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, BLOCKED, DELETED
                        `created_at`        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        `login_lasted_at`   DATETIME NULL,

                        CONSTRAINT `uk_user_input_id` UNIQUE (`input_id`),
                        CONSTRAINT `uk_user_uuid` UNIQUE (`uuid`),
                        CONSTRAINT `fk_user_gender`
                            FOREIGN KEY (`gender_id`) REFERENCES `gender`(`id`)
);

CREATE INDEX `idx_user_gender` ON `user`(`gender_id`);
CREATE INDEX `idx_user_status` ON `user`(`status`);


-- =========================================
-- 3. USER PROFILE
-- =========================================
CREATE TABLE `user_profile` (
                                `id`                BIGINT AUTO_INCREMENT PRIMARY KEY,
                                `user_id`           BIGINT NOT NULL,
                                `state_message`     VARCHAR(60) NULL,
                                `created_at`        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                `updated_at`        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                                CONSTRAINT `uk_user_profile_user` UNIQUE (`user_id`),
                                CONSTRAINT `fk_user_profile_user`
                                    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
);


-- =========================================
-- 4. USER PROFILE IMAGE
-- 기존 user_profile_url 역할
-- =========================================
CREATE TABLE `user_profile_image` (
                                      `id`                BIGINT AUTO_INCREMENT PRIMARY KEY,
                                      `user_profile_id`   BIGINT NOT NULL,
                                      `image_url`         VARCHAR(255) NOT NULL,
                                      `is_current`        TINYINT(1) NOT NULL DEFAULT 0,
                                      `uploaded_at`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                      CONSTRAINT `fk_user_profile_image_profile`
                                          FOREIGN KEY (`user_profile_id`) REFERENCES `user_profile`(`id`) ON DELETE CASCADE
);

CREATE INDEX `idx_user_profile_image_current`
    ON `user_profile_image`(`user_profile_id`, `is_current`);


-- =========================================
-- 5. FRIEND
-- is_blocked 제거, 차단은 user_block으로 분리
-- =========================================
CREATE TABLE `friend` (
                          `id`                BIGINT AUTO_INCREMENT PRIMARY KEY,
                          `user_id`           BIGINT NOT NULL,
                          `friend_user_id`    BIGINT NOT NULL,
                          `custom_nickname`   VARCHAR(30) NULL,
                          `created_at`        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                          CONSTRAINT `uk_friend` UNIQUE (`user_id`, `friend_user_id`),
                          CONSTRAINT `fk_friend_user`
                              FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
                          CONSTRAINT `fk_friend_target`
                              FOREIGN KEY (`friend_user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
);

CREATE INDEX `idx_friend_target` ON `friend`(`friend_user_id`);


-- =========================================
-- 6. USER BLOCK
-- =========================================
CREATE TABLE `user_block` (
                              `id`                BIGINT AUTO_INCREMENT PRIMARY KEY,
                              `blocker_id`        BIGINT NOT NULL,
                              `blocked_id`        BIGINT NOT NULL,
                              `reason`            VARCHAR(255) NULL,
                              `expires_at`        DATETIME NULL,
                              `created_at`        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                              CONSTRAINT `uk_user_block` UNIQUE (`blocker_id`, `blocked_id`),
                              CONSTRAINT `fk_user_block_blocker`
                                  FOREIGN KEY (`blocker_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
                              CONSTRAINT `fk_user_block_blocked`
                                  FOREIGN KEY (`blocked_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
);

CREATE INDEX `idx_user_block_blocker` ON `user_block`(`blocker_id`);
CREATE INDEX `idx_user_block_blocked` ON `user_block`(`blocked_id`);


-- =========================================
-- 7. ADMIN
-- 일반 user와 완전 분리
-- =========================================
CREATE TABLE `admin` (
                         `id`                BIGINT AUTO_INCREMENT PRIMARY KEY,
                         `login_id`          VARCHAR(30) NOT NULL,
                         `password`          VARCHAR(100) NOT NULL,
                         `name`              VARCHAR(30) NOT NULL,
                         `role`              VARCHAR(20) NOT NULL, -- SUPER_ADMIN, MODERATOR, AUDITOR
                         `status`            VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, LOCKED, DELETED
                         `last_login_at`     DATETIME NULL,
                         `created_at`        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                         CONSTRAINT `uk_admin_login_id` UNIQUE (`login_id`)
);

CREATE INDEX `idx_admin_role` ON `admin`(`role`);
CREATE INDEX `idx_admin_status` ON `admin`(`status`);


-- =========================================
-- 8. ADMIN LOGIN HISTORY
-- =========================================
CREATE TABLE `admin_login_history` (
                                       `id`                BIGINT AUTO_INCREMENT PRIMARY KEY,
                                       `admin_id`          BIGINT NOT NULL,
                                       `login_at`          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       `ip_address`        VARCHAR(45) NULL,
                                       `user_agent`        VARCHAR(255) NULL,
                                       `success`           TINYINT(1) NOT NULL,
                                       `fail_reason`       VARCHAR(255) NULL,

                                       CONSTRAINT `fk_admin_login_history_admin`
                                           FOREIGN KEY (`admin_id`) REFERENCES `admin`(`id`) ON DELETE CASCADE
);

CREATE INDEX `idx_admin_login_history_admin`
    ON `admin_login_history`(`admin_id`, `login_at` DESC);


-- =========================================
-- 9. ADMIN ACTION LOG
-- =========================================
CREATE TABLE `admin_action_log` (
                                    `id`                BIGINT AUTO_INCREMENT PRIMARY KEY,
                                    `admin_id`          BIGINT NOT NULL,
                                    `action_type`       VARCHAR(50) NOT NULL, -- DELETE_MESSAGE, BLOCK_USER, KICK_MEMBER ...
                                    `target_type`       VARCHAR(50) NULL,     -- USER, ROOM, MESSAGE ...
                                    `target_id`         BIGINT NULL,
                                    `description`       TEXT NULL,
                                    `created_at`        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                    CONSTRAINT `fk_admin_action_log_admin`
                                        FOREIGN KEY (`admin_id`) REFERENCES `admin`(`id`) ON DELETE CASCADE
);

CREATE INDEX `idx_admin_action_log_admin`
    ON `admin_action_log`(`admin_id`, `created_at` DESC);
CREATE INDEX `idx_admin_action_log_target`
    ON `admin_action_log`(`target_type`, `target_id`);


-- =========================================
-- 10. CHAT ROOM
-- participants_hash 제거
-- dm_key 도입 (DM 전용)
-- =========================================
CREATE TABLE `chat_room` (
                             `id`                      BIGINT AUTO_INCREMENT PRIMARY KEY,
                             `room_type`               VARCHAR(20) NOT NULL, -- DM, GROUP, OPEN
                             `name`                    VARCHAR(50) NULL,
                             `description`             TEXT NULL,
                             `max_person`              INT NULL,
                             `is_private`              TINYINT(1) NOT NULL DEFAULT 0,
                             `invite_code`             VARCHAR(50) NULL,
                             `dm_key`                  VARCHAR(50) NULL, -- DM일 때만 사용. 예: 3:10
                             `created_by`              BIGINT NOT NULL,
                             `created_at`              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                             `last_message_id`         BIGINT NULL,
                             `last_message_at`         DATETIME NULL,
                             `last_message_preview`    VARCHAR(120) NULL,
                             `last_message_sender_id`  BIGINT NULL,

                             `pinned_message_id`       BIGINT NULL,

                             `order_at`                DATETIME AS (COALESCE(`last_message_at`, `created_at`)) STORED,

                             CONSTRAINT `ck_chat_room_type` CHECK (`room_type` IN ('DM', 'GROUP', 'OPEN')),
                             CONSTRAINT `uk_chat_room_dm_key` UNIQUE (`dm_key`),
                             CONSTRAINT `fk_chat_room_created_by`
                                 FOREIGN KEY (`created_by`) REFERENCES `user`(`id`),
                             CONSTRAINT `fk_chat_room_last_sender`
                                 FOREIGN KEY (`last_message_sender_id`) REFERENCES `user`(`id`)
);

CREATE INDEX `idx_chat_room_type` ON `chat_room`(`room_type`);
CREATE INDEX `idx_chat_room_last` ON `chat_room`(`last_message_at` DESC, `id` DESC);
CREATE INDEX `idx_chat_room_order` ON `chat_room`(`order_at` DESC, `id` DESC);


-- =========================================
-- 11. CHAT ROOM MEMBER
-- 핵심
-- =========================================
CREATE TABLE `chat_room_member` (
                                    `id`                         BIGINT AUTO_INCREMENT PRIMARY KEY,
                                    `chat_room_id`               BIGINT NOT NULL,
                                    `user_id`                    BIGINT NOT NULL,
                                    `role`                       VARCHAR(20) NOT NULL DEFAULT 'MEMBER', -- OWNER, ADMIN, MEMBER
                                    `joined_at`                  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    `left_at`                    DATETIME NULL,
                                    `is_active`                  TINYINT(1) NOT NULL DEFAULT 1,
                                    `last_delivered_message_id`  BIGINT NULL,

                                    CONSTRAINT `uk_chat_room_member` UNIQUE (`chat_room_id`, `user_id`),
                                    CONSTRAINT `ck_chat_room_member_role` CHECK (`role` IN ('OWNER', 'ADMIN', 'MEMBER')),
                                    CONSTRAINT `fk_chat_room_member_room`
                                        FOREIGN KEY (`chat_room_id`) REFERENCES `chat_room`(`id`) ON DELETE CASCADE,
                                    CONSTRAINT `fk_chat_room_member_user`
                                        FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
);

CREATE INDEX `idx_chat_room_member_room` ON `chat_room_member`(`chat_room_id`);
CREATE INDEX `idx_chat_room_member_user` ON `chat_room_member`(`user_id`);


-- =========================================
-- 12. CHAT LIST
-- 읽음은 last_read_message_id 기반
-- =========================================
CREATE TABLE `chat_list` (
                             `id`                   BIGINT AUTO_INCREMENT PRIMARY KEY,
                             `user_id`              BIGINT NOT NULL,
                             `chat_room_id`         BIGINT NOT NULL,
                             `last_read_message_id` BIGINT NULL,
                             `unread_count`         INT NOT NULL DEFAULT 0,
                             `pinned`               TINYINT(1) NOT NULL DEFAULT 0,
                             `muted`                TINYINT(1) NOT NULL DEFAULT 0,
                             `archived`             TINYINT(1) NOT NULL DEFAULT 0,
                             `custom_name`          VARCHAR(50) NULL,
                             `last_opened_at`       DATETIME NULL,
                             `created_at`           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             `updated_at`           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                             CONSTRAINT `uk_chat_list_user_room` UNIQUE (`user_id`, `chat_room_id`),
                             CONSTRAINT `fk_chat_list_user`
                                 FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
                             CONSTRAINT `fk_chat_list_room`
                                 FOREIGN KEY (`chat_room_id`) REFERENCES `chat_room`(`id`) ON DELETE CASCADE
);

CREATE INDEX `idx_chat_list_user` ON `chat_list`(`user_id`);
CREATE INDEX `idx_chat_list_room` ON `chat_list`(`chat_room_id`);


-- =========================================
-- 13. CHAT MESSAGE
-- thread/edit/self-destruct/delete/client id 지원
-- =========================================
CREATE TABLE `chat_message` (
                                `id`                  BIGINT AUTO_INCREMENT PRIMARY KEY,
                                `chat_room_id`        BIGINT NOT NULL,
                                `sender_id`           BIGINT NOT NULL,
                                `message_type`        VARCHAR(20) NOT NULL, -- TEXT, IMAGE, FILE, SYSTEM
                                `message_content`     TEXT NULL,
                                `parent_message_id`   BIGINT NULL,
                                `created_at`          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                `edited_at`           DATETIME NULL,
                                `expires_at`          DATETIME NULL,
                                `deleted_at`          DATETIME NULL,
                                `is_deleted`          TINYINT(1) NOT NULL DEFAULT 0,
                                `client_message_id`   VARCHAR(100) NULL,

                                CONSTRAINT `ck_chat_message_type` CHECK (`message_type` IN ('TEXT', 'EMOJI', 'IMAGE', 'FILE', 'SYSTEM')),
                                CONSTRAINT `uk_chat_message_client` UNIQUE (`client_message_id`),
                                CONSTRAINT `fk_chat_message_room`
                                    FOREIGN KEY (`chat_room_id`) REFERENCES `chat_room`(`id`) ON DELETE CASCADE,
                                CONSTRAINT `fk_chat_message_sender`
                                    FOREIGN KEY (`sender_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
                                CONSTRAINT `fk_chat_message_parent`
                                    FOREIGN KEY (`parent_message_id`) REFERENCES `chat_message`(`id`) ON DELETE SET NULL
);

CREATE INDEX `idx_chat_message_room_created` ON `chat_message`(`chat_room_id`, `created_at` DESC);
CREATE INDEX `idx_chat_message_room_id` ON `chat_message`(`chat_room_id`, `id`);
CREATE INDEX `idx_chat_message_sender` ON `chat_message`(`sender_id`);
CREATE INDEX `idx_chat_message_parent` ON `chat_message`(`parent_message_id`);


-- =========================================
-- 14. CHAT ROOM -> CHAT MESSAGE FK
-- 순환참조 방지 위해 뒤에 추가
-- =========================================
ALTER TABLE `chat_room`
    ADD CONSTRAINT `fk_chat_room_last_message`
        FOREIGN KEY (`last_message_id`) REFERENCES `chat_message`(`id`) ON DELETE SET NULL;

ALTER TABLE `chat_room`
    ADD CONSTRAINT `fk_chat_room_pinned_message`
        FOREIGN KEY (`pinned_message_id`) REFERENCES `chat_message`(`id`) ON DELETE SET NULL;


-- =========================================
-- 15. CHAT ATTACHMENT
-- =========================================
CREATE TABLE `chat_attachment` (
                                   `id`                  BIGINT AUTO_INCREMENT PRIMARY KEY,
                                   `chat_message_id`     BIGINT NOT NULL,
                                   `file_url`            VARCHAR(255) NOT NULL,
                                   `original_file_name`  VARCHAR(255) NOT NULL,
                                   `stored_file_name`    VARCHAR(255) NULL,
                                   `content_type`        VARCHAR(100) NULL,
                                   `file_size`           BIGINT NULL,
                                   `created_at`          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                   CONSTRAINT `fk_chat_attachment_message`
                                       FOREIGN KEY (`chat_message_id`) REFERENCES `chat_message`(`id`) ON DELETE CASCADE
);

CREATE INDEX `idx_chat_attachment_message` ON `chat_attachment`(`chat_message_id`);


-- =========================================
-- 16. CHAT ROOM SETTING
-- =========================================
CREATE TABLE `chat_room_setting` (
                                     `id`                           BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     `chat_room_id`                 BIGINT NOT NULL,
                                     `allow_file_upload`            TINYINT(1) NOT NULL DEFAULT 1,
                                     `allow_self_destruct_message`  TINYINT(1) NOT NULL DEFAULT 0,
                                     `allow_thread`                 TINYINT(1) NOT NULL DEFAULT 1,
                                     `message_edit_time_limit`      INT NULL,
                                     `message_delete_time_limit`    INT NULL,
                                     `default_notification_on`      TINYINT(1) NOT NULL DEFAULT 1,
                                     `created_at`                   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     `updated_at`                   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                                     CONSTRAINT `uk_chat_room_setting_room` UNIQUE (`chat_room_id`),
                                     CONSTRAINT `fk_chat_room_setting_room`
                                         FOREIGN KEY (`chat_room_id`) REFERENCES `chat_room`(`id`) ON DELETE CASCADE
);


-- =========================================
-- 17. CHAT NOTIFICATION
-- =========================================
CREATE TABLE `chat_notification` (
                                     `id`                   BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     `user_id`              BIGINT NOT NULL,
                                     `chat_room_id`         BIGINT NOT NULL,
                                     `chat_message_id`      BIGINT NULL,
                                     `notification_type`    VARCHAR(30) NOT NULL, -- MESSAGE, MENTION, INVITE, SYSTEM
                                     `content`              TEXT NOT NULL,
                                     `is_read`              TINYINT(1) NOT NULL DEFAULT 0,
                                     `created_at`           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     `read_at`              DATETIME NULL,

                                     CONSTRAINT `fk_chat_notification_user`
                                         FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
                                     CONSTRAINT `fk_chat_notification_room`
                                         FOREIGN KEY (`chat_room_id`) REFERENCES `chat_room`(`id`) ON DELETE CASCADE,
                                     CONSTRAINT `fk_chat_notification_message`
                                         FOREIGN KEY (`chat_message_id`) REFERENCES `chat_message`(`id`) ON DELETE CASCADE
);

CREATE INDEX `idx_chat_notification_user`
    ON `chat_notification`(`user_id`, `is_read`, `created_at` DESC);
CREATE INDEX `idx_chat_notification_room`
    ON `chat_notification`(`chat_room_id`);
CREATE INDEX `idx_chat_notification_message`
    ON `chat_notification`(`chat_message_id`);