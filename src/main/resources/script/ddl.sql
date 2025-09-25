-- ====== DROP (존재 시) ======
DROP TABLE IF EXISTS `chat_notification`;
DROP TABLE IF EXISTS `notification_type`;
DROP TABLE IF EXISTS `chat_message_read`;
DROP TABLE IF EXISTS `chat_message`;
DROP TABLE IF EXISTS `chat_list`;
DROP TABLE IF EXISTS `chat_room_setting`;
DROP TABLE IF EXISTS `chat_room`;
DROP TABLE IF EXISTS `chat_author`;
DROP TABLE IF EXISTS `friend`;
DROP TABLE IF EXISTS `user_profile`;
DROP TABLE IF EXISTS `chat_type`;
DROP TABLE IF EXISTS `user`;
DROP TABLE IF EXISTS `gender`;
DROP TABLE IF EXISTS `user_status`;

-- ====== 기초 코드 테이블 ======
CREATE TABLE `gender` (
                          `gender_id` INT NOT NULL AUTO_INCREMENT,
                          `gender_name` VARCHAR(4) NOT NULL,
                          `gender_created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          PRIMARY KEY (`gender_id`)
) ENGINE=InnoDB;

CREATE TABLE `user_status` (
                               `user_status_id` INT NOT NULL AUTO_INCREMENT,
                               `user_status_name` VARCHAR(10) NOT NULL,
                               `user_status_created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               PRIMARY KEY (`user_status_id`)
) ENGINE=InnoDB;

-- ====== 사용자 ======
CREATE TABLE `user` (
                        `user_id` BIGINT NOT NULL AUTO_INCREMENT,
                        `gender_id` INT NOT NULL,
                        `user_status_id` INT NOT NULL,
                        `user_name` VARCHAR(20) NOT NULL,
                        `user_input_id` VARCHAR(20) NOT NULL,       -- (로그인 아이디) UNIQUE 권장
                        `user_input_password` VARCHAR(60) NOT NULL, -- 해시 저장 고려(길이 확장)
                        `user_age` INT NOT NULL,
                        `user_nickname` VARCHAR(20) NOT NULL,
                        `user_created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        `user_login_lasted` DATETIME NULL,
                        `user_uuid` VARCHAR(36) NOT NULL,
                        PRIMARY KEY (`user_id`),
                        UNIQUE KEY `uk_user_input_id` (`user_input_id`),
                        FOREIGN KEY (`gender_id`) REFERENCES `gender`(`gender_id`),
                        FOREIGN KEY (`user_status_id`) REFERENCES `user_status`(`user_status_id`)
) ENGINE=InnoDB;

-- ====== 채팅 타입 ======
CREATE TABLE `chat_type` (
                             `chat_type_id` INT NOT NULL AUTO_INCREMENT,
                             `chat_type_name` VARCHAR(10) NOT NULL,
                             PRIMARY KEY (`chat_type_id`)
) ENGINE=InnoDB;

-- ====== 채팅방 ======
CREATE TABLE `chat_room` (
                             `chat_room_id` BIGINT NOT NULL AUTO_INCREMENT,
                             `chat_type_id` INT NOT NULL,
                             `chat_room_created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             `chat_room_max_person` INT NULL,
                             `chat_room_name` VARCHAR(50) NULL,
                             `chat_room_description` TEXT NULL,
                             `is_private` TINYINT(1) NOT NULL DEFAULT 0,

    -- ★ 추가: 최근 메시지 메타(목록 정렬/표시용)
                             `last_message_id` BIGINT NULL,
                             `last_message_at` DATETIME NULL,
                             `last_message_preview` VARCHAR(120) NULL,
                             `last_message_sender_id` BIGINT NULL,

                             PRIMARY KEY (`chat_room_id`),
                             FOREIGN KEY (`chat_type_id`) REFERENCES `chat_type`(`chat_type_id`),
                             FOREIGN KEY (`last_message_sender_id`) REFERENCES `user`(`user_id`),

    -- ★ 목록 정렬 최적화
                             INDEX `idx_room_last` (`last_message_at` DESC, `chat_room_id` DESC)
) ENGINE=InnoDB;

-- ====== (옵션) 작성자 코드 테이블 ======
CREATE TABLE `chat_author` (
                               `chat_author_id` INT NOT NULL AUTO_INCREMENT,
                               `chat_author_name` VARCHAR(10) NOT NULL,
                               `chat_author_creater_created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               PRIMARY KEY (`chat_author_id`)
) ENGINE=InnoDB;

-- ====== 내가 속한 방(멤버십) ======
CREATE TABLE `chat_list` (
                             `chat_list_id` BIGINT NOT NULL AUTO_INCREMENT,
                             `user_id` BIGINT NOT NULL,
                             `chat_room_id` BIGINT NOT NULL,
                             `chat_author_id` INT NOT NULL,

    -- ★ 추가: 멤버별 상태/UX
                             `last_read_message_id` BIGINT NULL,
                             `unread_count` INT NOT NULL DEFAULT 0,
                             `pinned` TINYINT(1) NOT NULL DEFAULT 0,
                             `muted` TINYINT(1) NOT NULL DEFAULT 0,
                             `archived` TINYINT(1) NOT NULL DEFAULT 0,
                             `last_opened_at` DATETIME NULL,

                             PRIMARY KEY (`chat_list_id`),
                             UNIQUE KEY `uk_user_room` (`user_id`, `chat_room_id`), -- 한 유저가 같은 방에 1회만 가입
                             FOREIGN KEY (`user_id`) REFERENCES `user`(`user_id`) ON DELETE CASCADE,
                             FOREIGN KEY (`chat_room_id`) REFERENCES `chat_room`(`chat_room_id`) ON DELETE CASCADE,
                             FOREIGN KEY (`chat_author_id`) REFERENCES `chat_author`(`chat_author_id`),

                             INDEX `idx_list_user` (`user_id`),
                             INDEX `idx_list_room` (`chat_room_id`)
) ENGINE=InnoDB;

-- ====== 메시지 ======
CREATE TABLE `chat_message` (
                                `chat_message_id` BIGINT NOT NULL AUTO_INCREMENT,
                                `chat_room_id` BIGINT NOT NULL,
                                `sender_id` BIGINT NOT NULL,
                                `message_type` VARCHAR(20) NOT NULL,
                                `message_content` TEXT,
                                `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                `is_deleted` TINYINT(1) NOT NULL DEFAULT 0,
                                PRIMARY KEY (`chat_message_id`),
                                FOREIGN KEY (`chat_room_id`) REFERENCES `chat_room`(`chat_room_id`) ON DELETE CASCADE,
                                FOREIGN KEY (`sender_id`) REFERENCES `user`(`user_id`) ON DELETE CASCADE,

    -- ★ 조회/정렬 최적화
                                INDEX `idx_msg_room_created` (`chat_room_id`, `created_at` DESC),
                                INDEX `idx_msg_room_id` (`chat_room_id`, `chat_message_id`)
) ENGINE=InnoDB;

-- ====== 읽음 기록 ======
CREATE TABLE `chat_message_read` (
                                     `chat_message_read_id` BIGINT NOT NULL AUTO_INCREMENT,
                                     `chat_message_id` BIGINT NOT NULL,
                                     `user_id` BIGINT NOT NULL,
                                     `read_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     PRIMARY KEY (`chat_message_read_id`),
                                     FOREIGN KEY (`chat_message_id`) REFERENCES `chat_message`(`chat_message_id`) ON DELETE CASCADE,
                                     FOREIGN KEY (`user_id`) REFERENCES `user`(`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ====== 알림 타입 ======
CREATE TABLE `notification_type` (
                                     `notification_type_id` INT NOT NULL AUTO_INCREMENT,
                                     `notification_type_name` VARCHAR(20) NOT NULL,
                                     `description` VARCHAR(100) NULL,
                                     `is_popup` TINYINT(1) NOT NULL DEFAULT 0,
                                     `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     PRIMARY KEY (`notification_type_id`)
) ENGINE=InnoDB;

-- ====== 알림 ======
CREATE TABLE `chat_notification` (
                                     `chat_notification_id` BIGINT NOT NULL AUTO_INCREMENT,
                                     `user_id` BIGINT NOT NULL,
                                     `chat_room_id` BIGINT NOT NULL,
                                     `chat_message_id` BIGINT NULL,
                                     `notification_type_id` INT NOT NULL,
                                     `notification_content` TEXT NOT NULL,
                                     `is_read` TINYINT(1) NOT NULL DEFAULT 0,
                                     `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     PRIMARY KEY (`chat_notification_id`),
                                     FOREIGN KEY (`user_id`) REFERENCES `user`(`user_id`) ON DELETE CASCADE,
                                     FOREIGN KEY (`chat_room_id`) REFERENCES `chat_room`(`chat_room_id`) ON DELETE CASCADE,
                                     FOREIGN KEY (`chat_message_id`) REFERENCES `chat_message`(`chat_message_id`) ON DELETE CASCADE,
                                     FOREIGN KEY (`notification_type_id`) REFERENCES `notification_type`(`notification_type_id`)
) ENGINE=InnoDB;

-- ====== 방 설정 ======
CREATE TABLE `chat_room_setting` (
                                     `chat_room_setting_id` BIGINT NOT NULL AUTO_INCREMENT,
                                     `chat_room_id` BIGINT NOT NULL,
                                     `allow_file_upload` TINYINT(1) NOT NULL DEFAULT 1,
                                     `allow_self_destruct_message` TINYINT(1) NOT NULL DEFAULT 0,
                                     `allow_thread` TINYINT(1) NOT NULL DEFAULT 1,
                                     `message_edit_time_limit` INT NULL,
                                     `message_delete_time_limit` INT NULL,
                                     `default_notification_on` TINYINT(1) NOT NULL DEFAULT 1,
                                     `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     PRIMARY KEY (`chat_room_setting_id`),
                                     FOREIGN KEY (`chat_room_id`) REFERENCES `chat_room`(`chat_room_id`) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ====== 프로필 ======
CREATE TABLE `user_profile` (
                                `user_profile_id` BIGINT NOT NULL AUTO_INCREMENT,
                                `user_id` BIGINT NOT NULL,
                                `image_url` VARCHAR(255) NOT NULL,
                                `is_current` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '현재 사용중 여부',
                                `uploaded_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                PRIMARY KEY (`user_profile_id`),
                                FOREIGN KEY (`user_id`) REFERENCES `user`(`user_id`) ON DELETE CASCADE,
                                INDEX `idx_profile_current` (`user_id`, `is_current`)
) ENGINE=InnoDB;

-- ====== 친구 ======
CREATE TABLE `friend` (
                          `friend_id` BIGINT NOT NULL AUTO_INCREMENT,
                          `user_id` BIGINT NOT NULL COMMENT '친구를 등록한 사용자 ID',
                          `friend_user_id` BIGINT NOT NULL COMMENT '등록된 친구의 사용자 ID',
                          `is_blocked` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '차단 여부',
                          `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          PRIMARY KEY (`friend_id`),
                          UNIQUE KEY `UK_user_friend` (`user_id`, `friend_user_id`),
                          FOREIGN KEY (`user_id`) REFERENCES `user`(`user_id`) ON DELETE CASCADE,
                          FOREIGN KEY (`friend_user_id`) REFERENCES `user`(`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB;
