-- ============================================================
-- 海洋生物多样性信息管理系统 —— 数据库结构与初始数据
-- 库结构与初始数据的唯一来源，修改表结构需同步后端实体与查询
-- 约定：utf8mb4 / InnoDB / id INT AUTO_INCREMENT / create_time
-- ============================================================

-- 本文件是库结构与初始数据的唯一来源，整体执行一次即可得到与代码完全一致的库。
-- 开头直接 DROP，保证改动表结构后重新导入能真正生效（用 CREATE TABLE IF NOT EXISTS 做不到这点）。
-- 警告：执行后会清空原有数据。
DROP DATABASE IF EXISTS marine_biodiv;
CREATE DATABASE marine_biodiv DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 显式声明客户端连接字符集。mysql 命令行客户端在中文 Windows 上默认用系统
-- 代码页（GBK/936）读文件，UTF-8 的中文种子数据会被当成双字节字符，
-- 于是第一条 INSERT 就报 "Data too long for column 'real_name'" 而整份脚本导入失败。
SET NAMES utf8mb4;

USE marine_biodiv;

-- ============================================================
-- 模块一：用户与权限管理
-- role:   ADMIN 管理员 / RESEARCHER 科研人员 / STUDENT 学生 / PUBLIC 公众
-- status: PENDING 待审核 / ACTIVE 正常 / REJECTED 已拒绝
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL,
    password    VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密后的密码',
    real_name   VARCHAR(50)  NOT NULL,
    gender      VARCHAR(10)           DEFAULT NULL,
    phone       VARCHAR(20)           DEFAULT NULL,
    email       VARCHAR(100)          DEFAULT NULL,
    avatar      VARCHAR(255)          DEFAULT NULL,
    student_no  VARCHAR(30)           DEFAULT NULL,
    role        VARCHAR(20)  NOT NULL DEFAULT 'PUBLIC' COMMENT 'ADMIN/RESEARCHER/STUDENT/PUBLIC',
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/ACTIVE/REJECTED',
    -- 凭据版本号：改密码、重置密码、调整角色、停用账号时自增。
    -- 已登录会话里存的是登录那一刻的版本号，每次请求都比对一次，
    -- 不一致就作废会话——否则密码改了、角色降了，对方的会话还能继续用最长 2 小时。
    credential_version INT NOT NULL DEFAULT 1,
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_username (username),
    INDEX idx_role (role),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 模块二：物种信息管理（核心数据实体）
-- 分类字段 phylum~species_name 对应 门/纲/目/科/属/种
-- is_public = 0 的物种仅科研人员与管理员可见（RBAC 数据级控制）
-- ============================================================
CREATE TABLE IF NOT EXISTS species (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    chinese_name     VARCHAR(100) NOT NULL,
    scientific_name  VARCHAR(150)          DEFAULT NULL,
    phylum           VARCHAR(50)           DEFAULT NULL COMMENT '门',
    class_name       VARCHAR(50)           DEFAULT NULL COMMENT '纲',
    order_name       VARCHAR(50)           DEFAULT NULL COMMENT '目',
    family_name      VARCHAR(50)           DEFAULT NULL COMMENT '科',
    genus_name       VARCHAR(50)           DEFAULT NULL COMMENT '属',
    species_name     VARCHAR(50)           DEFAULT NULL COMMENT '种',
    morphology       TEXT,
    habits           TEXT,
    distribution     VARCHAR(255),
    longitude        DOUBLE               DEFAULT NULL,
    latitude         DOUBLE               DEFAULT NULL,
    protection_level VARCHAR(30)           DEFAULT NULL COMMENT '国家一级/国家二级/自治区重点/无',
    endanger_status  VARCHAR(30)           DEFAULT NULL COMMENT '未评估/无危/易危/濒危/极危',
    image_url        VARCHAR(255)          DEFAULT NULL,
    video_url        VARCHAR(255)          DEFAULT NULL,
    reference        TEXT,
    is_public        TINYINT(1)            DEFAULT 1,
    created_by       INT                   DEFAULT NULL,
    create_time      DATETIME              DEFAULT CURRENT_TIMESTAMP,
    update_time      DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL,
    -- 中文名唯一：SpeciesService.create/update 已经用 existsByChineseName 拦过一次，
    -- 这里再落一个唯一键，防止两个请求同时通过检查后各写一条同名记录。
    UNIQUE KEY unique_chinese_name (chinese_name),
    INDEX idx_phylum (phylum),
    INDEX idx_protection_level (protection_level),
    INDEX idx_endanger_status (endanger_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 模块三：生态系统与观测记录管理
-- ============================================================
CREATE TABLE IF NOT EXISTS ecosystems (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    code        VARCHAR(30)           DEFAULT NULL,
    description TEXT,
    image_url   VARCHAR(255)          DEFAULT NULL,
    create_time DATETIME              DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS observations (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    ecosystem_id  INT          NOT NULL,
    observer_id   INT                   DEFAULT NULL,
    observe_time  DATETIME     NOT NULL,
    longitude     DOUBLE               DEFAULT NULL,
    latitude      DOUBLE               DEFAULT NULL,
    location_name VARCHAR(200),
    water_temp    DOUBLE                DEFAULT NULL COMMENT '水温 ℃',
    salinity      DOUBLE                DEFAULT NULL COMMENT '盐度 ‰',
    depth         DOUBLE                DEFAULT NULL COMMENT '水深 m',
    weather       VARCHAR(30),
    notes         TEXT,
    create_time   DATETIME              DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (ecosystem_id) REFERENCES ecosystems (id) ON DELETE CASCADE,
    FOREIGN KEY (observer_id) REFERENCES users (id) ON DELETE SET NULL,
    INDEX idx_ecosystem (ecosystem_id),
    INDEX idx_observe_time (observe_time),
    INDEX idx_observer (observer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 观测记录与物种的关联（多对多）——愿景文档标注的「核心交互点」
-- count   此次观测到的估算数量
-- behavior 观测到的行为特征
CREATE TABLE IF NOT EXISTS observation_species (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    observation_id INT          NOT NULL,
    species_id     INT          NOT NULL,
    count          INT                   DEFAULT 0,
    behavior       VARCHAR(255)          DEFAULT NULL,
    note           VARCHAR(500)          DEFAULT NULL,
    create_time    DATETIME              DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (observation_id) REFERENCES observations (id) ON DELETE CASCADE,
    FOREIGN KEY (species_id) REFERENCES species (id) ON DELETE CASCADE,
    UNIQUE KEY unique_observation_species (observation_id, species_id),
    INDEX idx_species (species_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 模块一：用户活动日志
-- ============================================================
CREATE TABLE IF NOT EXISTS operation_logs (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT          DEFAULT NULL,
    username    VARCHAR(50)           DEFAULT NULL,
    module      VARCHAR(30)           DEFAULT NULL COMMENT '模块一~模块五',
    operation   VARCHAR(50)           DEFAULT NULL,
    target_type VARCHAR(30)           DEFAULT NULL,
    target_id   INT                   DEFAULT NULL,
    detail      VARCHAR(500)          DEFAULT NULL,
    ip          VARCHAR(50)           DEFAULT NULL,
    create_time DATETIME              DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_module (module),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 模块五：智能服务记录
-- 关键识别结果入库，便于追溯与质量分析（非功能性需求 2.3）
-- ============================================================
CREATE TABLE IF NOT EXISTS ai_records (
    id                INT AUTO_INCREMENT PRIMARY KEY,
    user_id           INT                   DEFAULT NULL,
    type              VARCHAR(30)           DEFAULT NULL COMMENT 'IDENTIFY/COMPLETE/TRANSLATE/TAG/QA',
    input_text        VARCHAR(1000)         DEFAULT NULL,
    input_image       VARCHAR(255)          DEFAULT NULL,
    result            TEXT,
    confidence        DOUBLE                DEFAULT NULL COMMENT '0~1，图像识别置信度',
    target_species_id INT                   DEFAULT NULL,
    model             VARCHAR(50)           DEFAULT NULL,
    cost_ms           INT                   DEFAULT NULL COMMENT '调用耗时，用于性能分析',
    success           TINYINT(1)            DEFAULT 1,
    create_time       DATETIME              DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS chat_messages (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT     DEFAULT NULL,
    role        VARCHAR(20) DEFAULT NULL COMMENT 'user/assistant',
    content     TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 初始数据
-- 下面全部是演示数据。脚本开头已经 DROP 过数据库，重复导入结果完全一致。
-- ============================================================

-- 默认账号（密码均为 BCrypt 加密，明文见注释）
-- admin/admin123          管理员
-- researcher/research123  科研人员
-- student/student123      学生
-- visitor/public123       公众
-- pending/student123      待审核申请（演示模块一审核流程）
INSERT INTO users (username, password, real_name, phone, email, student_no, role, status) VALUES
('admin',      '$2a$10$KWCY9W24m38SJFvwBl86ouUyTXnPdnQH/wWS6MrA9Nmsk8SNzhF1y', '系统管理员', '13800000001', 'admin@gdou.edu.cn',     NULL,       'ADMIN',      'ACTIVE'),
('researcher', '$2a$10$5ztUaOPc/GqOaAd2gvv3u.8b1yd4FKaPRBkFmcArPAk7YKu4atgHa', '陈老师',     '13800000002', 'chen@gdou.edu.cn',      '202411701404', 'RESEARCHER', 'ACTIVE'),
('student',    '$2a$10$y0ebm1yiyTtqz7VrYcji.Or8RJ6erBRCfspP.8iFKX.SbbDBUFE02', '李同学',     '13800000003', 'li@stu.gdou.edu.cn',    '202411701405', 'STUDENT',    'ACTIVE'),
('visitor',    '$2a$10$RPolTbtsDoQGFAuGbjbI2.QUKRVrIYMnMX2//n111ynD0EuNznTBC', '王先生',     '13800000004', 'wang@example.com',      NULL,           'PUBLIC',     'ACTIVE'),
('pending',    '$2a$10$y0ebm1yiyTtqz7VrYcji.Or8RJ6erBRCfspP.8iFKX.SbbDBUFE02', '待审核用户', '13800000005', 'new@stu.gdou.edu.cn',   '202411701406', 'STUDENT',    'PENDING');

-- 海洋生态系统
INSERT INTO ecosystems (name, code, description) VALUES
('珊瑚礁生态系统',   'CR',   '由造礁石珊瑚构成的浅海生态系统，生物多样性极高，是海洋生物的“热带雨林”。'),
('红树林生态系统',   'MF',   '生长于潮间带的木本植物群落，兼有海洋与陆地特征，为鸟类和幼鱼提供栖息地。'),
('海草床生态系统',   'SG',   '由海洋被子植物构成，固碳能力强，是儒艮等大型海洋哺乳动物的主要食物来源。'),
('潮间带岩礁生态系统', 'IR', '高潮与低潮之间的岩礁区域，生物面临剧烈的干露与温度、盐度变化。'),
('河口湿地生态系统', 'ES',   '河流与海洋交汇的过渡地带，盐度梯度大，鸟类与底栖生物丰富。'),
('深海生态系统',     'DP',   '水深 200 米以下的海洋区域，黑暗、低温、高压，物种组成独特。');

-- 物种信息（is_public = 0 表示仅科研人员与管理员可见）
INSERT INTO species (chinese_name, scientific_name, phylum, class_name, order_name, family_name, genus_name, species_name, morphology, habits, distribution, longitude, latitude, protection_level, endanger_status, is_public, created_by) VALUES
('珍珠牡蛎', 'Crassostrea gigas', '软体动物门', '瓣鳃纲', '牡蛎目', '牡蛎科', '巨蛎属', '太平洋牡蛎', '贝壳呈椭圆形，壳面有放射状肋纹，壳顶前后有耳，壳内面呈珍珠色。', '固着生活于潮间带及浅水区，滤食性，幼体阶段营浮游生活。', '中国东南沿海 广东 福建 台湾', 110.4236, 21.5088, '无', '无危', 1, 2),
('中华海参', 'Apostichopus japonicus', '棘皮动物门', '海参纲', '盾形目', '海参科', '刺参属', '刺参', '体呈圆筒形，背面有疣足与大小不等的棘突，腹面平坦，具管足。', '栖息于泥沙质或岩礁底，杂食性，昼伏夜出，夏季进入夏眠。', '辽宁至海南沿海 尤以北部出产', 122.1200, 39.6000, '国家二级', '易危', 1, 2),
('马尾藻', 'Sargassum fusiforme', '褐藻门', '褐藻纲', '墨角藻目', '马尾藻科', '马尾藻属', '马尾藻', '藻体黄褐色，分为茎、柄、叶三部分，叶细长匙形，气囊明显。', '附着于礁石上生长，随水流摆动，附着小动物众多。', '南海 东海沿岸浅水区', 110.1800, 21.3200, '无', '无危', 1, 2),
('中国对虾', 'Penaeus chinensis', '节肢动物门', '软甲纲', '十足目', '对虾科', '对虾属', '中国对虾', '体长约 8 厘米，体表呈青蓝色，额角细长，胸部附有粗刚毛。', '洄游性，幼体浮游，成虾底栖，春季繁殖洄游至近岸产卵。', '中国沿海 黄渤海至北部湾', 118.5000, 37.2000, '无', '易危', 1, 2),
('大黄鱼', 'Larimichthys crocea', '脊索动物门', '硬骨鱼纲', '鲈形目', '石首鱼科', '黄鱼属', '大黄鱼', '体侧金黄色，腹部银白，头部、眼部被金黄色皮膜覆盖。', '集群洄游，栖息于 30~80 米水层的底层，春季产卵。', '东海 浙江 福建 广东近海', 120.3000, 28.2000, '无', '无危', 1, 2),
('海月水母', 'Aurelia aurita', '刺胞动物门', '钵水母纲', '旗口水母目', '海月水母科', '海月水母属', '海月水母', '伞体扁平呈圆盘状，伞缘有 8 个深缺刻，半透明，胃腔呈四叶形。', '漂游生活，随波逐流，季节性大量出现，具刺细胞。', '广布于热带与温带近岸海域', 110.2500, 21.4000, '无', '无危', 1, 2),
('石芝珊瑚', 'Fungia fungites', '刺胞动物门', '珊瑚虫纲', '石芝目', '石芝科', '石芝属', '石芝', '单体珊瑚，个体呈圆盘状或椭圆形，辐射肋明显，附着于基盘。', '固着于礁盘，昼间张开触手捕食浮游生物，夜间闭合。', '南海 印度洋 太平洋热带浅水珊瑚礁', 110.5600, 21.1800, '自治区重点', '濒危', 1, 2),
('泰来草', 'Thalassia hemprichii', '被子植物门', '单子叶植物纲', '眼子菜目', '大叶藻科', '泰来草属', '泰来草', '海生高等植物，根状茎匍匐于泥沙中，叶带状，密集成丛。', '生长于浅水沙质底，水深 1~10 米，固碳效率高。', '印度洋 太平洋热带浅海', 110.3800, 21.2600, '国家二级', '濒危', 1, 2),
('招潮蟹', 'Tubuca spp.', '节肢动物门', '软甲纲', '十足目', '沙蟹科', '招潮蟹属', '弧边招潮蟹', '螯足一侧特大，另一侧小，甲壳青褐色，具细密花纹。', '红树林滩涂常见，潮间带活动，雄蟹以大螯挥动示偶。', '华南红树林湿地 台湾 广东 广西', 110.4100, 21.5900, '无', '无危', 1, 2),
('中华绒螯蟹', 'Eriocheir sinensis', '节肢动物门', '软甲纲', '十足目', '螃蟹科', '绒螯蟹属', '中华绒螯蟹', '甲壳近圆形，螯足密生绒毛，步足 5 对，背面青绿色。', '主要淡水养殖种，河蟹在海里繁殖洄游，也可河口半咸水生活。', '中国沿海河口与淡水水域', 110.1500, 21.7000, '无', '无危', 1, 2),
('乌贼', 'Sepia pharaonis', '软体动物门', '头足纲', '枪形目', '乌贼科', '乌贼属', '拟乌贼', '头部具 10 条腕，腕上吸盘两列，胴体呈纺锤形，内有退化贝壳。', '栖息于浅海沙质底，喷水游泳，夜间活动，摄食小型鱼类。', '华南沿海 印度洋 西太平洋', 110.0900, 21.2400, '无', '无危', 1, 2),
('鲻', 'Mugil cephalus', '脊索动物门', '硬骨鱼纲', '鲻形目', '鲻科', '鲻属', '鲻', '体呈纺锤形，头部扁平，眼大具发达脂眼睑，背部青灰色。', '广盐性鱼类，可往返于淡水与海水之间，以底泥有机物为食。', '全球温带至热带近岸及河口', 110.3300, 21.4800, '无', '无危', 1, 2),
('圆紫菜', 'Porphyra yezoensis', '红藻门', '红藻纲', '紫菜目', '紫菜科', '紫菜属', '甘紫菜', '藻体呈紫红色薄膜状，边缘波状，细胞多排成双列。', '附着于潮间带岩石，生长迅速，是重要食用藻类。', '辽宁 河北 山东至福建沿海', 121.5000, 36.8000, '无', '易危', 1, 2),
('海葡萄', 'Caulerpa racemosa', '绿藻门', '绿藻纲', '伞藻目', '羽藻科', '蕨藻属', '球形蕨藻', '藻体分枝繁密呈葡萄串状，绿色，质软，基部固着。', '生长于浅水礁区，能耐受较强的光照波动。', '南海 热带印度洋 太平洋', 110.5200, 21.2000, '无', '无危', 1, 2),
('海马', 'Hippocampus spp.', '脊索动物门', '硬骨鱼纲', '海龙鱼目', '海龙科', '海龙属', '大海马', '身体侧扁弯曲，头部呈马状与体轴垂直，尾部卷曲缠绕，骨质环明显。', '游泳缓慢，常以尾部缠绕海草或珊瑚栖息，行动迟缓。', '广东 福建 台湾近岸海域', 110.2000, 21.1000, '国家二级', '易危', 1, 2),
('白斑刺鲀', 'Diodon hystrix', '棘索动物门', '硬骨鱼纲', '鲀形目', '鲀科', '刺鲀属', '白斑刺鲀', '体粗短，棘刺发达，腹面有白斑，受惊后能吸水膨胀。', '栖于礁区洞穴，杂食性，具强毒内脏。', '热带浅海 珊瑚礁区', 110.6000, 21.1500, '自治区重点', '易危', 0, 2),
('秋茄', 'Kandelia obovata', '被子植物门', '双子叶植物纲', '蔷薇目', '红树科', '秋茄属', '秋茄', '灌木或小乔木，叶对生椭圆，肉质，具红色胎生苗，根有支柱根。', '生长于高潮线附近的泥滩，耐盐碱，具泌盐现象。', '广东 广西 福建 台湾红树林', 110.4300, 21.5500, '无', '无危', 1, 2),
('粗纹鹅卵石斑鱼', 'Epinephelus moara', '脊索动物门', '硬骨鱼纲', '鲈形目', '鮨科', '石斑鱼属', '条纹石斑鱼', '体侧具 5 条斜向黑褐色横带，鳃盖上有 3 条黑褐色斜纹。', '定居性鱼类，栖息于岩礁区洞穴，性情凶猛，掠食性。', '东海 南海 广东沿海礁区', 110.4500, 21.1700, '无', '无危', 0, 2),
('红树林弹涂鱼', 'Periophthalmus spp.', '脊索动物门', '硬骨鱼纲', '鲈形目', '塘鳢科', '弹涂鱼属', '大弹涂鱼', '体粗短，头大，眼高位突出，鳍条减少，可离水跳跃滑行。', '红树林滩涂洞穴中生活，涨潮出穴觅食，具泥鳅式滑行能力。', '华南红树林湿地', 110.4000, 21.6000, '无', '无危', 1, 2);

-- 观测记录
INSERT INTO observations (ecosystem_id, observer_id, observe_time, longitude, latitude, location_name, water_temp, salinity, depth, weather, notes) VALUES
(2, 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '09:30:00'), 110.4312, 21.5561, '雷州红树林国家级自然保护区核心区', 26.5, 21.4, 1.5, '晴', '涨潮时在树干基部发现大量招潮蟹活动。'),
(2, 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 18 DAY), '15:20:00'), 110.4050, 21.6010, '廉江高桥红树林片区', 28.0, 24.0, 1.0, '多云', '幼鱼群在潮沟入口处聚集，随退潮下滩觅食。'),
(2, 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 26 DAY), '08:45:00'), 110.4236, 21.5088, '特呈岛红树林', 30.2, 27.5, 0.8, '晴', '高温高盐，记录到 8 月种苗移栽前的基线数据。'),
(2, 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 47 DAY), '10:15:00'), 110.4312, 21.5561, '雷州红树林国家级自然保护区核心区', 25.8, 20.6, 1.5, '阴', '与去年同期对比，招潮蟹密度略有回升。'),
(1, 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 62 DAY), '14:00:00'), 110.5610, 21.1815, '硇洲岛附近珊瑚礁区', 27.2, 33.5, 6.0, '晴', '水下 6 米处可见石芝与海葡萄共生。'),
(1, 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 78 DAY), '11:30:00'), 110.5690, 21.1740, '硇洲岛南侧礁盘', 29.8, 34.2, 8.0, '晴', '石芝覆盖率目测约 35%，较去年略有下降。'),
(1, 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 95 DAY), '09:50:00'), 110.5610, 21.1815, '硇洲岛附近珊瑚礁区', 27.6, 33.8, 6.0, '多云', '发现白斑刺鲀 2 尾，体型较大，疑似繁殖期。'),
(1, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 112 DAY), '15:10:00'), 110.5580, 21.1830, '硇洲岛北侧岩礁', 28.4, 33.9, 4.5, '晴', '管理员随行，记录了石斑鱼育幼期的分布变化。'),
(3, 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 130 DAY), '09:00:00'), 110.3800, 21.2600, '特呈岛海草床', 29.5, 32.0, 3.0, '晴', '海草叶面附着大量固着藤壶，需清理后再计数。'),
(3, 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 148 DAY), '14:30:00'), 110.3850, 21.2550, '东海岛海草床', 26.0, 31.2, 3.5, '晴', '秋季海草密度达到峰值，覆盖面积明显扩大。'),
(3, 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 166 DAY), '10:20:00'), 110.3800, 21.2600, '特呈岛海草床', 28.0, 31.8, 3.0, '阴', '未发现明显鱼类聚集，可能与水温升高有关。'),
(4, 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 184 DAY), '08:20:00'), 110.2500, 21.4000, '霞山观海长廊外礁', 24.0, 31.0, 1.0, '阴', '大潮退至最低潮位，记录岩礁区潮间带生物。'),
(4, 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 202 DAY), '16:00:00'), 110.2450, 21.4050, '特呈岛北岸岩礁', 28.8, 32.5, 1.2, '晴', '水母大量出现，为该海域夏季常见现象。'),
(4, 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 220 DAY), '07:50:00'), 110.2500, 21.4000, '霞山观海长廊外礁', 23.6, 30.8, 1.0, '晴', '清晨低潮，海月水母数量较去年明显增多。'),
(4, 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 238 DAY), '17:10:00'), 110.4500, 21.1700, '硇洲岛东侧岩礁', 30.4, 34.0, 2.0, '晴', '黄昏潜水，记录石斑鱼栖息洞穴。'),
(5, 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 256 DAY), '10:40:00'), 110.3300, 21.4800, '雷州湾河口', 27.5, 18.0, 4.0, '多云', '盐度梯度明显，鲻鱼群上溯觅食。'),
(5, 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 274 DAY), '13:30:00'), 110.1500, 21.7000, '遂溪港河口湿地', 22.5, 12.0, 2.0, '阴', '中华绒螯蟹幼蟹在泥滩活动，为洄游前兆。'),
(5, 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 292 DAY), '09:15:00'), 110.3300, 21.4800, '雷州湾河口', 28.8, 20.5, 4.0, '晴', '夏季河口高温，鱼类活动深度下移至 5 米以下。'),
(5, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 310 DAY), '11:00:00'), 110.1200, 21.6800, '雷州港潮间带', 30.0, 15.0, 1.0, '晴', '公众科普活动的观测数据。'),
(1, 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 328 DAY), '13:20:00'), 110.0900, 21.2400, '硇洲岛西侧浅滩', 22.0, 33.0, 5.0, '多云', '冬季水温低，仅记录到少量底栖生物。'),
(3, 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 350 DAY), '09:40:00'), 110.5600, 21.2000, '硇洲岛周边海草床', 25.0, 32.0, 3.2, '晴', '春季海草萌发期，叶片数量增加。'),
(4, 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 372 DAY), '18:00:00'), 110.4300, 21.5500, '雷州红树林外缘潮沟', 30.5, 22.0, 1.8, '晴', '夜间灯诱，记录到多种小型鱼类进入潮沟。'),
(2, 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 396 DAY), '08:30:00'), 110.4000, 21.6000, '廉江安铺红树林', 29.8, 23.5, 1.2, '多云', '秋季红树林鸟类观测同期进行。'),
(1, 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 420 DAY), '15:30:00'), 110.5200, 21.2000, '硇洲岛东侧礁区', 28.0, 33.6, 7.0, '晴', '海葡萄覆盖面积较大的样方。'),
(6, 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 450 DAY), '20:00:00'), 110.0000, 20.5000, '珠江口外海陆坡', 18.0, 34.5, 320.0, '晴', '拖网样品，含少量未鉴定深海鱼种。'),
(6, 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 486 DAY), '21:30:00'), 110.0000, 20.5000, '珠江口外海陆坡', 19.0, 34.6, 320.0, '晴', '第二次陆坡采样，与去年样品对比。'),
(5, 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 530 DAY), '14:10:00'), 110.3300, 21.4800, '雷州湾河口', 20.5, 19.0, 3.5, '阴', '冬季河口，鲻鱼较少，以底栖贝类为主。');

-- 观测-物种关联（估算数量与行为）
INSERT INTO observation_species (observation_id, species_id, count, behavior, note) VALUES
(1, 9, 120, '挥螯求偶、争抢洞穴', '集中在树干北侧'),
(1, 16, 30, '跳跃、穿行于滩涂表面', ''),
(1, 19, 15, '滑行觅食', '涨潮前出穴'),
(2, 9, 80, '幼蟹随潮沟水流上下移动', ''),
(2, 19, 6, '停栖在枝干上', '疑似产仔期'),
(3, 9, 200, '大量成蟹活动', '数量为四个季度最高'),
(3, 17, 45, '涨潮时迁移到高处的根部', ''),
(4, 9, 140, '争抢洞穴', ''),
(4, 19, 9, '滑行觅食', ''),
(5, 7, 25, '触手张开捕食浮游生物', '水下 6 米'),
(5, 14, 40, '随流摆动', '成丛分布'),
(5, 12, 8, '底层游动', ''),
(6, 7, 18, '触手半开', '覆盖率约 35%'),
(6, 14, 60, '随流摆动', ''),
(6, 12, 5, '底层游动', ''),
(7, 7, 22, '触手张开', '体型明显大于去年'),
(7, 16, 2, '巡游于礁区', '体型较大'),
(7, 14, 50, '随流摆动', ''),
(8, 19, 4, '潜伏于洞穴，仅露头', '育幼期个体'),
(8, 7, 15, '触手张开', ''),
(9, 8, 300, '根系固着，叶片随流摆动', '需清理藤壶后计数'),
(9, 12, 12, '穿梭于草丛间', ''),
(10, 8, 480, '覆盖率峰值', '秋季为生长旺季'),
(10, 15, 6, '缠绕于茎上', ''),
(10, 12, 9, '底层游动', ''),
(11, 8, 380, '叶面附着小生物', ''),
(12, 11, 60, '漂游，随波逐流', '伞缘有缺刻'),
(12, 18, 10, '跳跃、滑行于岩缝', ''),
(13, 11, 95, '漂游', '数量较去年增多'),
(13, 18, 14, '跳跃、翻越岩块', ''),
(14, 19, 5, '伏击型捕食', '黄昏活动高峰'),
(14, 17, 2, '巡游', ''),
(15, 12, 35, '上溯觅食，集群', '盐度 18‰ 处密度最高'),
(15, 10, 20, '泥滩横行', '幼蟹'),
(16, 12, 28, '活动深度下移至 5 米以下', '水温 28.8℃'),
(17, 10, 15, '泥滩活动', '科普活动记录'),
(18, 6, 12, '沙质底匍匐', '冬季水温低'),
(19, 8, 400, '叶片萌发，数量增加', '春季萌发期'),
(20, 9, 60, '夜间出穴', '灯诱'),
(20, 12, 10, '进入潮沟', ''),
(21, 16, 10, '穿行于滩涂', '秋季观测同期鸟类'),
(21, 17, 25, '根系固着', ''),
(22, 14, 90, '成丛分布', '覆盖面积较大'),
(22, 19, 3, '底层游动', ''),
(23, 6, 8, '底层匍匐', '未定名种'),
(23, 12, 6, '底层游动', ''),
(24, 12, 22, '上溯觅食', '冬季以底栖贝类为主'),
(24, 1, 40, '滤食', '牡蛎礁附近'),
(1, 17, 35, '幼苗阶段，扎根于泥滩', '准备移栽'),
(5, 13, 25, '附着于礁石', '采集样品时观察到');

-- 操作日志（演示模块一的活动日志）
INSERT INTO operation_logs (user_id, username, module, operation, target_type, target_id, detail, ip) VALUES
(1, 'admin', '模块一', '用户登录', 'User', 1, '管理员登录系统', '127.0.0.1'),
(2, 'researcher', '模块一', '用户登录', 'User', 2, '科研人员登录系统', '127.0.0.1'),
(1, 'admin', '模块一', '审核通过', 'User', 5, '通过学生用户的注册申请，角色设为 STUDENT', '127.0.0.1'),
(2, 'researcher', '模块二', '新增物种', 'Species', 16, '新增物种「白斑刺鲀」', '127.0.0.1'),
(2, 'researcher', '模块三', '新增观测记录', 'Observation', 25, '新增观测记录并关联 2 个物种', '127.0.0.1'),
(2, 'researcher', '模块二', '编辑物种', 'Species', 7, '更新「石芝珊瑚」的分布描述', '127.0.0.1'),
(1, 'admin', '模块四', '导出', NULL, NULL, '导出物种信息 Excel', '127.0.0.1'),
(3, 'student', '模块二', '查询', 'Species', NULL, '按保护等级筛选物种', '127.0.0.1'),
(2, 'researcher', '模块五', '识别', 'AiRecord', NULL, '图像智能识别物种，置信度 0.86', '127.0.0.1');
