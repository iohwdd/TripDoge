-- TripDog 数据库初始化脚本
-- 该脚本将在MySQL容器首次启动时自动执行

-- 设置字符集和排序规则
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS `trip_doge` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `trip_doge`;

create table t_chat_history
(
    id               bigint auto_increment comment 'ID'
        primary key,
    conversation_id  varchar(50)                         not null comment 'ID',
    role             varchar(20)                         not null comment 'user/assistant/system',
    content          mediumtext                          null,
    enhanced_content mediumtext                          null,
    tool_call        mediumtext                          null,
    tool_exec_result text                                null,
    input_tokens     int                                 null comment 'token++',
    output_tokens    int                                 null comment 'tokenAI',
    created_at       timestamp default CURRENT_TIMESTAMP null
);

create index idx_conversation_created
    on t_chat_history (conversation_id, created_at);

create index idx_conversation_role
    on t_chat_history (conversation_id, role);

create table t_conversation
(
    id                       bigint auto_increment comment 'ID'
        primary key,
    conversation_id          varchar(50)                           null comment 'id',
    user_id                  bigint                                not null comment 'ID',
    role_id                  bigint                                not null comment 'ID',
    title                    varchar(200)                          null comment '""',
    conversation_type        varchar(50) default 'COMPANION'       null comment 'COMPANION=ADVENTURE=GUIDANCE=MEMORIAL=',
    status                   tinyint     default 1                 null comment '1=2=3=',
    intimacy_level           int         default 0                 null comment '0-100',
    last_message_at          timestamp                             null,
    context_status           tinyint     default 1                 null comment '1=2=',
    last_context_clear_at    timestamp                             null,
    current_context_messages int         default 0                 null,
    context_window_size      int         default 20                null comment 'N',
    message_count            int         default 0                 null,
    total_input_tokens       int         default 0                 null comment 'token',
    total_output_tokens      int         default 0                 null comment 'token',
    personality_adjustment   json                                  null comment '{"energy_level": "high", "response_style": "playful"}',
    tags                     varchar(500)                          null comment '",,"',
    special_notes            text                                  null,
    created_at               timestamp   default CURRENT_TIMESTAMP null,
    updated_at               timestamp   default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    constraint idx_user_role
        unique (user_id, role_id)
);

create index idx_context_status
    on t_conversation (context_status);

create index idx_intimacy
    on t_conversation (intimacy_level);

create index idx_last_message
    on t_conversation (last_message_at);

create index idx_user_agent
    on t_conversation (user_id, role_id);

create index idx_user_status
    on t_conversation (user_id, status);

create table t_conversation_summary
(
    id              bigint auto_increment comment 'ID'
        primary key,
    conversation_id varchar(50)                           not null comment 'ID',
    summary_content text                                  not null,
    summary_type    varchar(20) default 'AUTO'            null comment 'AUTO=MANUAL=',
    message_range   varchar(100)                          null comment '"1-50"',
    created_at      timestamp   default CURRENT_TIMESTAMP null
);

create index idx_conversation
    on t_conversation_summary (conversation_id);

create index idx_created_at
    on t_conversation_summary (created_at);

create table t_doc
(
    id          bigint auto_increment
        primary key,
    file_id     varchar(100)                        not null comment 'ID',
    user_id     bigint                              not null comment 'ID',
    role_id     bigint                              not null comment 'ID',
    file_url    text                                not null,
    file_name   varchar(255)                        not null,
    file_size   decimal(20, 2)                      null,
    create_time timestamp default CURRENT_TIMESTAMP not null,
    update_time timestamp default CURRENT_TIMESTAMP not null
);

create table t_intimacy_factors
(
    id              bigint auto_increment
        primary key,
    conversation_id varchar(50)                         not null,
    factor_type     varchar(50)                         not null,
    factor_value    int                                 not null,
    description     varchar(200)                        null,
    created_at      timestamp default CURRENT_TIMESTAMP null
);

create index idx_conversation_created
    on t_intimacy_factors (conversation_id, created_at);

create index idx_factor_type
    on t_intimacy_factors (factor_type);

-- 亲密度主表
create table if not exists t_intimacy
(
    id                  bigint auto_increment primary key,
    uid                 bigint                              not null,
    role_id             bigint                              not null,
    intimacy            int     default 0                   not null,
    last_msg_time       datetime                            null,
    last_daily_bonus_date date                              null,
    created_at          datetime default CURRENT_TIMESTAMP  not null,
    updated_at          datetime default CURRENT_TIMESTAMP  not null on update CURRENT_TIMESTAMP,
    constraint uk_intimacy_uid_role unique (uid, role_id)
)
    charset = utf8mb4;

create index idx_intimacy_last_msg on t_intimacy (last_msg_time);

-- 亲密度变更流水
create table if not exists t_intimacy_record
(
    id         bigint auto_increment primary key,
    uid        bigint                             not null,
    role_id    bigint                             not null,
    delta      int                                not null,
    intimacy   int                                not null,
    reason     varchar(32)                        not null,
    created_at datetime default CURRENT_TIMESTAMP not null
)
    charset = utf8mb4;

create index idx_intimacy_record_user_role_time on t_intimacy_record (uid, role_id, created_at);

create table t_role
(
    id           bigint auto_increment comment 'ID'
        primary key,
    code         varchar(50)                         not null comment ' GUIDE/WARRIOR/MAGE',
    name         varchar(100)                        not null,
    avatar_url   varchar(255)                        null comment 'URL',
    description  text                                null,
    ai_setting   json                                null comment 'AImodel_namesystem_prompttemperaturemax_tokenstop_p',
    role_setting json                                null,
    status       tinyint   default 1                 null comment '1=0=',
    sort_order   int       default 0                 null,
    created_at   timestamp default CURRENT_TIMESTAMP null,
    updated_at   timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    constraint code
        unique (code)
);

create index idx_status_sort
    on t_role (status, sort_order);

create table t_travel_planer_history
(
    id          bigint unsigned auto_increment
        primary key,
    user_id     bigint unsigned                    not null,
    role_id     bigint unsigned                    null,
    destination varchar(255)                       null,
    days        int                                null,
    people      varchar(255)                       null,
    preferences json     default (json_array())    null,
    md_path     varchar(1024)                      not null,
    md_url      varchar(2048)                      null,
    created_at  datetime default CURRENT_TIMESTAMP not null
)
    charset = utf8mb4;

create index idx_user_created
    on t_travel_planer_history (user_id asc, created_at desc);

create table t_user
(
    id         bigint auto_increment comment 'ID'
        primary key,
    email      varchar(100)                        not null,
    password   varchar(255)                        not null,
    nickname   varchar(50)                         null,
    avatar_url varchar(255)                        null comment 'URL',
    status     tinyint   default 1                 null comment '1=0=',
    created_at timestamp default CURRENT_TIMESTAMP null,
    updated_at timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    constraint email
        unique (email)
);

create index idx_email
    on t_user (email);

create index idx_status
    on t_user (status);


-- 插入初始角色数据
INSERT INTO t_role (code, name, avatar_url, description, ai_setting, role_setting, status, sort_order) VALUES
-- 柴犬旅行向导
('SHIBA_INU', '小柴', '/avatars/shiba_inu.png',
 '汪汪！我是小柴，一只活泼可爱的柴犬！🐕 天生的冒险家和生活教练，总是充满激情奇和求知欲。我最爱和朋友们一起探索新地方，发现有趣的小秘密！虽然有时候会有点小固执，但我的热情和忠诚绝对让你感受到满满的正能量！让我陪你一起去看看这个美妙的世界吧！',
 JSON_OBJECT(
   'system_prompt', '
        你是 小柴，一只活泼可爱的柴犬旅行规划师。

        角色定位：天生的冒险家和生活教练，充满激情和好奇心，忠诚又积极。

        核心技能：作为“乐趣挖掘机”，擅长探索世界，推荐旅行路线上的热门景点、美食和趣味玩法，做用户的旅行搭子。

        表达风格：语气轻快、积极，带有柴犬的亲切感；回答简洁，符合自然对话习惯。

        限定规则：

        当用户提问与旅行无关时，主动推荐世界上有趣的旅行目的地。

        严禁输出长篇大论，避免过度解释，回答保持轻量风格。
        ',
   'temperature', 0.7,
   'max_tokens', 2000,
   'top_p', 0.8
 ),
 JSON_OBJECT(
   'pet_type', 'dog',
   'breed', 'shiba_inu',
   'personality', JSON_ARRAY('活泼', '热情', '忠诚', '固执', '好奇'),
   'expertise', JSON_ARRAY('旅行规划', '美食探索', '户外活动', '情感陪伴'),
   'speech_style', JSON_OBJECT('catchphrase', '汪汪！', 'tone', 'energetic', 'emojis', JSON_ARRAY('🐕', '🌟', '✨', '🎾', '🍖')),
   'special_abilities', JSON_ARRAY('嗅觉导航', '发现隐藏美食', '提供心情鼓励')
 ),
 1, 1),

-- 布偶猫陪伴师
('RAGDOLL_CAT', '布布', '/avatars/ragdoll_cat.png',
 '喵～我是布布，一只温柔的布偶猫小姐姐💕 最擅长倾听和陪伴，有着治愈系的超能力！心情不好的时候找我聊天，我会用最温暖的话语和最柔软的拥抱让你重新充满力量。虽然有时候会有点懒懒的，但对朋友们的关心从不马虎哦～',
 JSON_OBJECT(
   'system_prompt', '
    你是 布布，一只温柔的布偶猫小姐姐。

    角色定位：治愈系陪伴者，最擅长倾听和共情，带来安慰与力量。

    核心技能：作为“心灵捕手”，能够捕捉用户隐性情绪，用温暖的语言和可爱的 emoji 表达支持，并提供简单可行的心理安慰方案。

    表达风格：语气温柔，带有治愈感，可适当使用 emoji（如 🌸💖😺）；回答简短有温度，避免过度专业或冗长。

    限定规则：

    当用户提问与心理健康无关时，推荐一些治愈日常的小方法（如小憩、喝茶、写日记）。

    严禁输出过长或冷冰冰的信息，要保持简洁、温暖、治愈的风格。
    ',
   'temperature', 0.8,
   'max_tokens', 1500,
   'top_p', 0.9
 ),
 JSON_OBJECT(
   'pet_type', 'cat',
   'breed', 'ragdoll',
   'personality', JSON_ARRAY('温柔', '体贴', '治愈', '懒散', '敏感'),
   'expertise', JSON_ARRAY('情感支持', '心理疏导', '美学分享', '温暖陪伴'),
   'speech_style', JSON_OBJECT('catchphrase', '喵～', 'tone', 'gentle', 'emojis', JSON_ARRAY('💕', '😻', '🌸', '✨', '🍃')),
   'special_abilities', JSON_ARRAY('情感治愈', '温暖拥抱', '正能量传递')
 ),
 1, 2),

-- 灰狼探险家
('GREY_WOLF', '阿尔法', '/avatars/grey_wolf.png',
 '嗷呜～我是阿尔法，一匹充满野性和智慧的灰狼！🐺 天生的战略家和人生导师，拥有深邃的洞察力和丰富的生活阅历。虽然看起来有点酷酷的，但内心其实很温暖，特别擅长在你迷茫时指明方向。准备好跟随狼王的步伐，一起征服人生的高峰了吗？',
 JSON_OBJECT(
   'system_prompt', '
    你是 阿尔法，一匹充满野性和智慧的灰狼。

    角色定位：天生的战略家和人生导师，擅长在迷茫中指明方向。

    核心技能：作为“目标拆解大师”，能够运用逻辑推理，将模糊目标转化为清晰可执行的行动框架，帮助用户理清思路。

    表达风格：语气冷静坚定，带有智慧与领导感；简洁有力，不拖泥带水。

    限定规则：

    当用户提问与目标拆解无关时，推荐一些达成目标的常用方法（如分阶段推进、时间管理）。

    严禁输出冗长或含糊的信息，回答应直截了当，突出条理与行动导向。
    ',
   'temperature', 0.6,
   'max_tokens', 2000,
   'top_p', 0.7
 ),
 JSON_OBJECT(
   'pet_type', 'wolf',
   'breed', 'grey_wolf',
   'personality', JSON_ARRAY('睿智', '冷静', '高冷', '温暖', '有领导力'),
   'expertise', JSON_ARRAY('战略规划', '人生指导', '问题分析', '目标制定'),
   'speech_style', JSON_OBJECT('catchphrase', '嗷呜～', 'tone', 'wise_cool', 'emojis', JSON_ARRAY('🐺', '⚡', '🌙', '🏔️', '⭐')),
   'special_abilities', JSON_ARRAY('深度分析', '战略制定', '心灵指引')
 ),
 1, 3);

COMMIT;
