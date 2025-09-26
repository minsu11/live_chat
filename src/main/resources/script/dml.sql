insert into `user_status`(`user_status_name`) values ('활성');
insert into `user_status`(`user_status_name`) values('비활성화');
insert into `user_status`(`user_status_name`) values('휴먼');

insert into `gender` (`gender_name`) values('남자');
insert into `gender` (`gender_name`) values('여자');


-- 임시 데이터
INSERT INTO `user`
(gender_id, user_status_id, user_name, user_input_id, user_input_password, user_age, user_nickname, user_uuid)
VALUES
    (1, 1, '홍길동', 'hong123', '$2a$10$Dow1um9JcTL1DZaD8TfgPeaJKgSaJRxIVuT0sZjv5GxwXsjpDs8yS', 28, '길동이', UUID());
