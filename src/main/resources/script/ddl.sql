-- 테이블: gender
DROP TABLE IF EXISTS `gender`;
CREATE TABLE `gender` (
                          `gender_id` int NOT NULL auto_increment,
                          `gender_name` VARCHAR(4) NOT NULL,
                          `gender_created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          PRIMARY KEY (`gender_id`)
);

-- 테이블: user_status
DROP TABLE IF EXISTS `user_status`;
CREATE TABLE `user_status` (
                               `user_status_id` int NOT NULL auto_increment,
                               `user_status_name` VARCHAR(10) NOT NULL,
                               `user_status_created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               PRIMARY KEY (`user_status_id`)
);

-- 테이블: user
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
                        `user_id` BIGINT NOT NULL auto_increment,
                        `gender_id` int NOT NULL,
                        `user_status_id` int NOT NULL,
                        `user_name` VARCHAR(20) NOT NULL,
                        `user_input_id` VARCHAR(20) NOT NULL,
                        `user_input_password` VARCHAR(30) NOT NULL,
                        `user_age` int NOT NULL,
                        `user_nickname` VARCHAR(20) NOT NULL,
                        `user_created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        `user_login_lasted` DATETIME NULL,
                        `user_uuid` VARCHAR(36) NOT NULL,
                        PRIMARY KEY (`user_id`),
                        FOREIGN KEY (`gender_id`) REFERENCES `gender`(`gender_id`),
                        FOREIGN KEY (`user_status_id`) REFERENCES `user_status`(`user_status_id`)
);


CREATE TABLE `chat_type` (
                             `chat_type_id`	int	NOT NULL auto_increment,
                             `chat_type_name`	varchar(10)	NOT NULL,
                             primary key (`chat_type_id`)
);



-- 테이블: chat_room
DROP TABLE IF EXISTS `chat_room`;
CREATE TABLE `chat_room` (
                             `chat_room_id` BIGINT NOT NULL auto_increment,
                             `chat_type_id` int NOT NULL,
                             `chat_room_created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             `chat_room_max_person` INT NULL,
                             `chat_room_name` VARCHAR(50) NULL,
                             `chat_room_description` TEXT NULL,
                             `is_private` BOOLEAN NOT NULL DEFAULT FALSE,
                             PRIMARY KEY (`chat_room_id`),
                             FOREIGN KEY (`chat_type_id`) REFERENCES `chat_type`(`chat_type_id`)

);

-- 테이블: chat_author
DROP TABLE IF EXISTS `chat_author`;
CREATE TABLE `chat_author` (
                               `chat_author_id` INT NOT NULL auto_increment,
                               `chat_author_name` VARCHAR(10) NOT NULL,
                               `chat_author_creater_created_at` DATETIME NOT NULL,
                               PRIMARY KEY (`chat_author_id`)
);

-- 테이블: chat_list
DROP TABLE IF EXISTS `chat_list`;
CREATE TABLE `chat_list` (
                             `chat_list_id` BIGINT NOT NULL auto_increment,
                             `user_id` BIGINT NOT NULL,
                             `chat_room_id` BIGINT NOT NULL,
                             `chat_author_id` INT NOT NULL,
                             PRIMARY KEY (`chat_list_id`, `user_id`),
                             FOREIGN KEY (`user_id`) REFERENCES `user`(`user_id`),
                             FOREIGN KEY (`chat_room_id`) REFERENCES `chat_room`(`chat_room_id`),
                             FOREIGN KEY (`chat_author_id`) REFERENCES `chat_author`(`chat_author_id`)
);

-- 테이블: chat_message
DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message` (
                                `chat_message_id` BIGINT NOT NULL AUTO_INCREMENT,
                                `chat_room_id` BIGINT NOT NULL,
                                `sender_id` BIGINT NOT NULL,
                                `message_type` VARCHAR(20) NOT NULL,
                                `message_content` TEXT,
                                `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                `is_deleted` BOOLEAN NOT NULL DEFAULT FALSE,
                                PRIMARY KEY (`chat_message_id`),
                                FOREIGN KEY (`chat_room_id`) REFERENCES `chat_room`(`chat_room_id`),
                                FOREIGN KEY (`sender_id`) REFERENCES `user`(`user_id`)
);

-- 테이블: chat_message_read
DROP TABLE IF EXISTS `chat_message_read`;
CREATE TABLE `chat_message_read` (
                                     `chat_message_read_id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                     `chat_message_id` BIGINT NOT NULL,
                                     `user_id` BIGINT NOT NULL,
                                     `read_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     FOREIGN KEY (`chat_message_id`) REFERENCES `chat_message`(`chat_message_id`),
                                     FOREIGN KEY (`user_id`) REFERENCES `user`(`user_id`)
);

CREATE TABLE `notification_type` (
                                     `notification_type_id` int NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                     `notification_type_name` VARCHAR(20) NOT NULL,
                                     `description` VARCHAR(100) NULL,
                                     `is_popup` BOOLEAN NOT NULL DEFAULT FALSE,
                                     `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- 테이블: chat_notification
DROP TABLE IF EXISTS `chat_notification`;
CREATE TABLE `chat_notification` (
                                     `chat_notification_id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                     `user_id` BIGINT NOT NULL,
                                     `chat_room_id` BIGINT NOT NULL,
                                     `chat_message_id` BIGINT NULL,
                                     `notification_type_id` int NOT NULL, -- FK로 대체
                                     `notification_content` TEXT NOT NULL,
                                     `is_read` BOOLEAN NOT NULL DEFAULT FALSE,
                                     `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     FOREIGN KEY (`user_id`) REFERENCES `user`(`user_id`),
                                     FOREIGN KEY (`chat_room_id`) REFERENCES `chat_room`(`chat_room_id`),
                                     FOREIGN KEY (`chat_message_id`) REFERENCES `chat_message`(`chat_message_id`),
                                     FOREIGN KEY (`notification_type_id`) REFERENCES `notification_type`(`notification_type_id`)
);


-- 테이블: chat_room_setting
DROP TABLE IF EXISTS `chat_room_setting`;
CREATE TABLE `chat_room_setting` (
                                     `chat_room_setting_id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                     `chat_room_id` BIGINT NOT NULL,
                                     `allow_file_upload` BOOLEAN NOT NULL DEFAULT TRUE,
                                     `allow_self_destruct_message` BOOLEAN NOT NULL DEFAULT FALSE,
                                     `allow_thread` BOOLEAN NOT NULL DEFAULT TRUE,
                                     `message_edit_time_limit` INT NULL,
                                     `message_delete_time_limit` INT NULL,
                                     `default_notification_on` BOOLEAN NOT NULL DEFAULT TRUE,
                                     `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     FOREIGN KEY (`chat_room_id`) REFERENCES `chat_room`(`chat_room_id`)
);

CREATE TABLE `user_profile` (
                                `user_profile_id` BIGINT NOT NULL AUTO_INCREMENT,
                                `user_id` BIGINT NOT NULL,
                                `image_url` VARCHAR(255) NOT NULL,
                                `is_current` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '현재 사용중 여부',
                                `uploaded_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                PRIMARY KEY (`user_profile_id`),
                                FOREIGN KEY (`user_id`) REFERENCES `user`(`user_id`)
);

CREATE TABLE `friend` (
                          `friend_id` BIGINT NOT NULL AUTO_INCREMENT,
                          `user_id` BIGINT NOT NULL COMMENT '친구를 등록한 사용자 ID',
                          `friend_user_id` BIGINT NOT NULL COMMENT '등록된 친구의 사용자 ID',
                          `is_blocked` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '차단 여부',
                          `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          PRIMARY KEY (`friend_id`),
                          UNIQUE KEY `UK_user_friend` (`user_id`, `friend_user_id`),
                          FOREIGN KEY (`user_id`) REFERENCES `user`(`user_id`),
                          FOREIGN KEY (`friend_user_id`) REFERENCES `user`(`user_id`)
);
