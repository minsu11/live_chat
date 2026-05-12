create table if not exists admin
(
    id            bigint auto_increment
        primary key,
    login_id      varchar(30)                           not null,
    password      varchar(100)                          not null,
    name          varchar(30)                           not null,
    role          varchar(20)                           not null,
    status        varchar(20) default 'ACTIVE'          not null,
    last_login_at datetime                              null,
    created_at    datetime    default CURRENT_TIMESTAMP not null,
    constraint uk_admin_login_id
        unique (login_id)
);

create index idx_admin_role
    on admin (role);

create index idx_admin_status
    on admin (status);

create table if not exists admin_action_log
(
    id          bigint auto_increment
        primary key,
    admin_id    bigint                             not null,
    action_type varchar(50)                        not null,
    target_type varchar(50)                        null,
    target_id   bigint                             null,
    description text                               null,
    created_at  datetime default CURRENT_TIMESTAMP not null,
    constraint fk_admin_action_log_admin
        foreign key (admin_id) references admin (id)
            on delete cascade
);

create index idx_admin_action_log_admin
    on admin_action_log (admin_id asc, created_at desc);

create index idx_admin_action_log_target
    on admin_action_log (target_type, target_id);

create table if not exists admin_login_history
(
    id          bigint auto_increment
        primary key,
    admin_id    bigint                             not null,
    login_at    datetime default CURRENT_TIMESTAMP not null,
    ip_address  varchar(45)                        null,
    user_agent  varchar(255)                       null,
    success     tinyint(1)                         not null,
    fail_reason varchar(255)                       null,
    constraint fk_admin_login_history_admin
        foreign key (admin_id) references admin (id)
            on delete cascade
);

create index idx_admin_login_history_admin
    on admin_login_history (admin_id asc, login_at desc);

create table if not exists gender
(
    id         int auto_increment
        primary key,
    name       varchar(10)                        not null,
    created_at datetime default CURRENT_TIMESTAMP not null,
    constraint uk_gender_name
        unique (name)
);

create index idx_gender_name
    on gender (name);

create table if not exists user
(
    id              bigint auto_increment
        primary key,
    gender_id       int                                   not null,
    input_id        varchar(30)                           not null,
    input_password  varchar(100)                          not null,
    age             int                                   not null,
    name            varchar(30)                           not null,
    nickname        varchar(30)                           not null,
    uuid            varchar(36)                           not null,
    status          varchar(20) default 'ACTIVE'          not null,
    created_at      datetime    default CURRENT_TIMESTAMP not null,
    login_lasted_at datetime                              null,
    constraint uk_user_input_id
        unique (input_id),
    constraint uk_user_uuid
        unique (uuid),
    constraint fk_user_gender
        foreign key (gender_id) references gender (id)
);

create table if not exists chat_message
(
    id                bigint auto_increment
        primary key,
    chat_room_id      bigint                               not null,
    sender_id         bigint                               not null,
    message_type      varchar(20)                          not null,
    message_content   text                                 null,
    parent_message_id bigint                               null,
    created_at        datetime   default CURRENT_TIMESTAMP not null,
    edited_at         datetime                             null,
    expires_at        datetime                             null,
    deleted_at        datetime                             null,
    is_deleted        tinyint(1) default 0                 not null,
    client_message_id varchar(100)                         null,
    constraint uk_chat_message_client
        unique (client_message_id),
    constraint fk_chat_message_parent
        foreign key (parent_message_id) references chat_message (id)
            on delete set null,
    constraint fk_chat_message_sender
        foreign key (sender_id) references user (id)
            on delete cascade,
    constraint ck_chat_message_type
        check (`message_type` in ('TEXT','EMOJI','IMAGE','FILE','SYSTEM_LEAVE','SYSTEM_INVITE','SYSTEM'))
);

create fulltext index fx_chat_content
    on chat_message (message_content);

create index idx_chat_message_parent
    on chat_message (parent_message_id);

create index idx_chat_message_room_created
    on chat_message (chat_room_id asc, created_at desc);

create index idx_chat_message_room_id
    on chat_message (chat_room_id, id);

create index idx_chat_message_sender
    on chat_message (sender_id);

create table if not exists chat_room
(
    id                     bigint auto_increment
        primary key,
    room_type              varchar(20)                          not null,
    name                   varchar(50)                          null,
    description            text                                 null,
    max_person             int                                  null,
    is_private             tinyint(1) default 0                 not null,
    invite_code            varchar(50)                          null,
    dm_key                 varchar(100)                         null,
    created_by             bigint                               not null,
    created_at             datetime   default CURRENT_TIMESTAMP not null,
    last_message_id        bigint                               null,
    last_message_at        datetime                             null,
    last_message_preview   varchar(120)                         null,
    last_message_sender_id bigint                               null,
    pinned_message_id      bigint                               null,
    order_at               datetime as (coalesce(`last_message_at`, `created_at`)) stored,
    participant_count      int        default 1                 not null comment '현재 참여 인원수',
    constraint uk_chat_room_dm_key
        unique (dm_key),
    constraint fk_chat_room_created_by
        foreign key (created_by) references user (id),
    constraint fk_chat_room_last_message
        foreign key (last_message_id) references chat_message (id)
            on delete set null,
    constraint fk_chat_room_last_sender
        foreign key (last_message_sender_id) references user (id),
    constraint fk_chat_room_pinned_message
        foreign key (pinned_message_id) references chat_message (id)
            on delete set null,
    constraint ck_chat_room_type
        check (`room_type` in ('DM','GROUP','OPEN'))
);

create table if not exists chat_attachment
(
    id                 bigint auto_increment
        primary key,
    room_id            bigint                             not null,
    uploader_id        bigint                             not null,
    chat_message_id    bigint                             null,
    file_url           varchar(255)                       not null,
    original_file_name varchar(255)                       not null,
    stored_file_name   varchar(255)                       null,
    content_type       varchar(100)                       null,
    file_size          bigint                             null,
    created_at         datetime default CURRENT_TIMESTAMP not null,
    constraint fk_chat_attachment_message
        foreign key (chat_message_id) references chat_message (id)
            on delete cascade,
    constraint fk_chat_attachment_room
        foreign key (room_id) references chat_room (id)
            on delete cascade,
    constraint fk_chat_attachment_uploader
        foreign key (uploader_id) references user (id)
            on delete cascade
);

create index idx_chat_attachment_message
    on chat_attachment (chat_message_id);

create index idx_chat_attachment_room_id
    on chat_attachment (room_id);

create index idx_chat_attachment_uploader_id
    on chat_attachment (uploader_id);

create table if not exists chat_list
(
    id                   bigint auto_increment
        primary key,
    user_id              bigint                               not null,
    chat_room_id         bigint                               not null,
    last_read_message_id bigint                               null,
    unread_count         int        default 0                 not null,
    pinned               tinyint(1) default 0                 not null,
    muted                tinyint(1) default 0                 not null,
    archived             tinyint(1) default 0                 not null,
    custom_name          varchar(50)                          null,
    last_opened_at       datetime                             null,
    created_at           datetime   default CURRENT_TIMESTAMP not null,
    updated_at           datetime   default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    constraint uk_chat_list_user_room
        unique (user_id, chat_room_id),
    constraint fk_chat_list_room
        foreign key (chat_room_id) references chat_room (id)
            on delete cascade,
    constraint fk_chat_list_user
        foreign key (user_id) references user (id)
            on delete cascade
);

create index idx_chat_list_room
    on chat_list (chat_room_id);

create index idx_chat_list_user
    on chat_list (user_id);

alter table chat_message
    add constraint fk_chat_message_room
        foreign key (chat_room_id) references chat_room (id)
            on delete cascade;

create table if not exists chat_notification
(
    id                bigint auto_increment
        primary key,
    user_id           bigint                               not null,
    chat_room_id      bigint                               not null,
    chat_message_id   bigint                               null,
    notification_type varchar(30)                          not null,
    content           text                                 not null,
    is_read           tinyint(1) default 0                 not null,
    created_at        datetime   default CURRENT_TIMESTAMP not null,
    read_at           datetime                             null,
    constraint fk_chat_notification_message
        foreign key (chat_message_id) references chat_message (id)
            on delete cascade,
    constraint fk_chat_notification_room
        foreign key (chat_room_id) references chat_room (id)
            on delete cascade,
    constraint fk_chat_notification_user
        foreign key (user_id) references user (id)
            on delete cascade
);

create index idx_chat_notification_message
    on chat_notification (chat_message_id);

create index idx_chat_notification_room
    on chat_notification (chat_room_id);

create index idx_chat_notification_user
    on chat_notification (user_id asc, is_read asc, created_at desc);

create index idx_chat_room_last
    on chat_room (last_message_at desc, id desc);

create index idx_chat_room_order
    on chat_room (order_at desc, id desc);

create index idx_chat_room_type
    on chat_room (room_type);

create table if not exists chat_room_member
(
    id                        bigint auto_increment
        primary key,
    chat_room_id              bigint                                not null,
    user_id                   bigint                                not null,
    role                      varchar(20) default 'MEMBER'          not null,
    joined_at                 datetime    default CURRENT_TIMESTAMP not null,
    left_at                   datetime                              null,
    is_active                 tinyint(1)  default 1                 not null,
    last_delivered_message_id bigint                                null,
    constraint uk_chat_room_member
        unique (chat_room_id, user_id),
    constraint fk_chat_room_member_room
        foreign key (chat_room_id) references chat_room (id)
            on delete cascade,
    constraint fk_chat_room_member_user
        foreign key (user_id) references user (id)
            on delete cascade,
    constraint ck_chat_room_member_role
        check (`role` in ('OWNER','ADMIN','MEMBER'))
);

create index idx_chat_room_member_room
    on chat_room_member (chat_room_id);

create index idx_chat_room_member_user
    on chat_room_member (user_id);

create table if not exists chat_room_setting
(
    id                          bigint auto_increment
        primary key,
    chat_room_id                bigint                               not null,
    allow_file_upload           tinyint(1) default 1                 not null,
    allow_self_destruct_message tinyint(1) default 0                 not null,
    allow_thread                tinyint(1) default 1                 not null,
    message_edit_time_limit     int                                  null,
    message_delete_time_limit   int                                  null,
    default_notification_on     tinyint(1) default 1                 not null,
    created_at                  datetime   default CURRENT_TIMESTAMP not null,
    updated_at                  datetime   default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    constraint uk_chat_room_setting_room
        unique (chat_room_id),
    constraint fk_chat_room_setting_room
        foreign key (chat_room_id) references chat_room (id)
            on delete cascade
);

create table if not exists friend
(
    id              bigint auto_increment
        primary key,
    user_id         bigint                             not null,
    friend_user_id  bigint                             not null,
    custom_nickname varchar(30)                        null,
    created_at      datetime default CURRENT_TIMESTAMP not null,
    constraint uk_friend
        unique (user_id, friend_user_id),
    constraint fk_friend_target
        foreign key (friend_user_id) references user (id)
            on delete cascade,
    constraint fk_friend_user
        foreign key (user_id) references user (id)
            on delete cascade
);

create index idx_friend_target
    on friend (friend_user_id);

create index idx_user_gender
    on user (gender_id);

create index idx_user_status
    on user (status);

create table if not exists user_block
(
    id         bigint auto_increment
        primary key,
    blocker_id bigint                             not null,
    blocked_id bigint                             not null,
    reason     varchar(255)                       null,
    expires_at datetime                           null,
    created_at datetime default CURRENT_TIMESTAMP not null,
    constraint uk_user_block
        unique (blocker_id, blocked_id),
    constraint fk_user_block_blocked
        foreign key (blocked_id) references user (id)
            on delete cascade,
    constraint fk_user_block_blocker
        foreign key (blocker_id) references user (id)
            on delete cascade
);

create index idx_user_block_blocked
    on user_block (blocked_id);

create index idx_user_block_blocker
    on user_block (blocker_id);

create table if not exists user_profile
(
    id            bigint auto_increment
        primary key,
    user_id       bigint                             not null,
    state_message varchar(60)                        null,
    created_at    datetime default CURRENT_TIMESTAMP not null,
    updated_at    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    constraint uk_user_profile_user
        unique (user_id),
    constraint fk_user_profile_user
        foreign key (user_id) references user (id)
            on delete cascade
);

create table if not exists user_profile_image
(
    id              bigint auto_increment
        primary key,
    user_profile_id bigint                               not null,
    image_url       varchar(255)                         not null,
    is_current      tinyint(1) default 0                 not null,
    uploaded_at     datetime   default CURRENT_TIMESTAMP not null,
    constraint fk_user_profile_image_profile
        foreign key (user_profile_id) references user_profile (id)
            on delete cascade
);

create index idx_user_profile_image_current
    on user_profile_image (user_profile_id, is_current);

