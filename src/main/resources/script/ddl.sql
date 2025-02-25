CREATE TABLE `gender` (
                          `gender_id`	int	NOT NULL auto_increment,
                          `gender_name`	varchar(4)	NOT NULL,
                          `gender_created_at`	datetime	NOT NULL	DEFAULT now(),
                          primary key (`gender_id`)
);

CREATE TABLE `chat_type` (
                             `chat_type_id`	int	NOT NULL auto_increment,
                             `chat_type_name`	varchar(10)	NOT NULL,
                             primary key (`chat_type_id`)
);


CREATE TABLE `user` (
                        `user_id`	bigint	NOT NULL auto_increment	COMMENT 'user의 pk',
                        `gender_id`	int	NOT NULL,
                        `user_status_id`	int	NOT NULL,
                        `user_name`	varchar(20)	NOT NULL,
                        `user_input_id`	varchar(20)	NOT NULL,
                        `user_input_password`	varchar(30)	NOT NULL,
                        `user_age`	int	NOT NULL,
                        `user_nickname`	varchar(20)	NOT NULL,
                        `user_created_at`	datetime	NOT NULL	DEFAULT now(),
                        primary key (`user_id`)
);

CREATE TABLE `user_status` (
                               `user_status_id`	int	NOT NULL auto_increment,
                               `user_status_name`	varchar(10)	NOT NULL,
                               `user_status_created_at`	datetime	NOT NULL	DEFAULT now(),
                               primary key (`user_status_id`)
);

CREATE TABLE `chat_room` (
                             `chat_room_id`	bigint	NOT NULL auto_increment,
                             `chat_room_created_at`	datetime	NOT NULL,
                             `chat_room_max_person`	int	NULL,
                             `chat_type_id`	int	NOT NULL,
                            primary key (`chat_room_id`)
);

CREATE TABLE `chat_list` (
                             `chat_list_id`	bigint	NOT NULL auto_increment,
                             `user_id`	bigint	NOT NULL	COMMENT 'user의 pk',
                             `chat_room_id`	bigint	NOT NULL,
                             `chat_author_id`	int	NOT NULL,
                            primary key (`chat_list_id`)
);


CREATE TABLE `chat_author` (
                               `chat_author_id`	int	NOT NULL auto_increment,
                               `chat_author_name`	varchar(10)	NOT NULL,
                               `chat_author_created_at`	datetime	NOT NULL,
                               primary key (`chat_author_id`)
);

ALTER TABLE `user` ADD CONSTRAINT `FK_gender_TO_user_1` FOREIGN KEY (
                                                                     `gender_id`
    )
    REFERENCES `gender` (
                         `gender_id`
        );

ALTER TABLE `user` ADD CONSTRAINT `FK_user_status_TO_user_1` FOREIGN KEY (
                                                                          `user_status_id`
    )
    REFERENCES `user_status` (
                              `user_status_id`
        );

ALTER TABLE `chat_room` ADD CONSTRAINT `FK_chat_type_TO_chat_room_1` FOREIGN KEY (
                                                                                  `chat_type_id`
    )
    REFERENCES `chat_type` (
                            `chat_type_id`
        );

ALTER TABLE `chat_list` ADD CONSTRAINT `FK_user_TO_chat_list_1` FOREIGN KEY (
                                                                             `user_id`
    )
    REFERENCES `user` (
                       `user_id`
        );

ALTER TABLE `chat_list` ADD CONSTRAINT `FK_chat_room_TO_chat_list_1` FOREIGN KEY (
                                                                                  `chat_room_id`
    )
    REFERENCES `chat_room` (
                            `chat_room_id`
        );

ALTER TABLE `chat_list` ADD CONSTRAINT `FK_chat_author_TO_chat_list_1` FOREIGN KEY (
                                                                                    `chat_author_id`
    )
    REFERENCES `chat_author` (
                              `chat_author_id`
        );

