-- 中华文脉网站数据库脚本
-- 创建数据库
CREATE DATABASE IF NOT EXISTS chinese_culture DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE chinese_culture;

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    is_admin TINYINT(1) DEFAULT 0,
    reg_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 文章/内容表
CREATE TABLE IF NOT EXISTS articles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    category VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    author VARCHAR(50) DEFAULT '本站整理',
    source VARCHAR(200),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    views INT DEFAULT 0,
    INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 点赞表
CREATE TABLE IF NOT EXISTS likes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    article_id INT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_like (user_id, article_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (article_id) REFERENCES articles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 评论表
CREATE TABLE IF NOT EXISTS comments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    article_id INT NOT NULL,
    content TEXT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (article_id) REFERENCES articles(id) ON DELETE CASCADE,
    INDEX idx_article (article_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 转发表
CREATE TABLE IF NOT EXISTS shares (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    article_id INT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (article_id) REFERENCES articles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入示例文章数据
INSERT INTO articles (title, category, content, source) VALUES
('《论语》学而篇第一', 'classics', '子曰："学而时习之，不亦说乎？有朋自远方来，不亦乐乎？人不知而不愠，不亦君子乎？"\n\n有子曰："其为人也孝弟，而好犯上者，鲜矣；不好犯上，而好作乱者，未之有也。君子务本，本立而道生。孝弟也者，其为仁之本与！"\n\n子曰："巧言令色，鲜矣仁！"\n\n曾子曰："吾日三省吾身：为人谋而不忠乎？与朋友交而不信乎？传不习乎？"', '中国哲学书电子化计划 ctext.org'),
('《道德经》第一章', 'classics', '道可道，非常道；名可名，非常名。\n\n无名，天地之始；有名，万物之母。\n\n故常无欲，以观其妙；常有欲，以观其徼。\n\n此两者，同出而异名，同谓之玄。\n\n玄之又玄，众妙之门。', '中国哲学书电子化计划 ctext.org'),
('京剧脸谱艺术', 'ich', '京剧脸谱，是一种具有中国文化特色的特殊化妆方法。由于每个历史人物或某一种类型的人物都有一种大概的谱式，就像唱歌、奏乐都要按照乐谱一样，所以称为"脸谱"。\n\n脸谱的主要特点有三点：美与丑的矛盾统一，与角色的性格关系密切，其图案是程式化的。\n\n红色脸象征忠义、耿直、有血性，如："三国戏"里的关羽；黑色脸既表现性格严肃，不苟言笑，如"包公戏"里的包拯；白色脸表现奸诈多疑，如："三国戏"里的曹操。', '故宫博物院官网'),
('中国剪纸艺术', 'ich', '中国剪纸是一种用剪刀或刻刀在纸上剪刻花纹，用于装点生活或配合其他民俗活动的民间艺术。在中国，剪纸具有广泛的群众基础，交融于各族人民的社会生活，是各种民俗活动的重要组成部分。\n\n2006年5月20日，剪纸艺术遗产经国务院批准列入第一批国家级非物质文化遗产名录。2009年9月28日至10月2日举行的联合国教科文组织保护非物质文化遗产政府间委员会第四次会议上，中国申报的中国剪纸项目入选"人类非物质文化遗产代表作名录"。', '中国非物质文化遗产网'),
('王羲之《兰亭集序》', 'art', '永和九年，岁在癸丑，暮春之初，会于会稽山阴之兰亭，修禊事也。群贤毕至，少长咸集。此地有崇山峻岭，茂林修竹；又有清流激湍，映带左右，引以为流觞曲水，列坐其次。虽无丝竹管弦之盛，一觞一咏，亦足以畅叙幽情。\n\n是日也，天朗气清，惠风和畅，仰观宇宙之大，俯察品类之盛，所以游目骋怀，足以极视听之娱，信可乐也。', '故宫博物院官网'),
('清明上河图', 'art', '《清明上河图》，中国十大传世名画之一。为北宋风俗画，北宋画家张择端仅见的存世精品，属国宝级文物，现藏于北京故宫博物院。\n\n作品以长卷形式，采用散点透视构图法，生动记录了中国十二世纪北宋都城东京（又称汴京，今河南开封）的城市面貌和当时社会各阶层人民的生活状况，是北宋时期都城汴京当年繁荣的见证，也是北宋城市经济情况的写照。', '故宫博物院官网'),
('二十四节气', 'music', '二十四节气，是历法中表示自然节律变化以及确立"十二月建"的特定节令。一岁四时，春夏秋冬各三个月，每月两个节气，每个节气均有其独特的含义。\n\n二十四节气准确的反映了自然节律变化，在人们日常生活中发挥了极为重要的作用。它不仅是指导农耕生产的时节体系，更是包含有丰富民俗事象的民俗系统。二十四节气蕴含着悠久的文化内涵和历史积淀，是中华民族悠久历史文化的重要组成部分。', '中国非物质文化遗产网'),
('中国古琴艺术', 'music', '古琴是中国最早的弹弦乐器，称为"国乐之父"。在古时文人心中视为高雅的代表，琴音悠远，高山流水知音流传至今。\n\n古琴表现力特别丰富，运用不同的弹奏手法，可以发挥出很多艺术表现的特色，它的散音嘹亮、浑厚，宏如铜钟；泛音透明如珠，丰富多采，由于音区不同而有异。', '中国非物质文化遗产网'),
('中国古代四大发明', 'tech', '四大发明，是关于中国科学技术史的一种观点，是指中国古代对世界具有很大影响的四种发明，是中国古代劳动人民的重要创造，是指造纸术、指南针、火药和印刷术。\n\n这一说法最早由英国汉学家艾约瑟提出并为后来许多中国的历史学家所继承，普遍认为这四种发明对中国古代的政治、经济、文化的发展产生了巨大的推动作用，且这些发明经由各种途径传至西方，对世界文明发展史也产生了很大的影响。', '中国国家博物馆'),
('榫卯结构', 'tech', '榫卯（sǔn mǎo），是古代中国建筑、家具及其它器械的主要结构方式，是在两个构件上采用凹凸部位相结合的一种连接方式。凸出部分叫榫（或叫榫头）；凹进部分叫卯（或叫榫眼、榫槽）。\n\n其特点是在物件上不使用钉子，利用卯榫加固物件，体现出中国古老的文化和智慧。中国的榫卯结构，精妙神奇，变化无穷，由简单的两个榫卯零件，到复杂的千百个榫卯零件群，经过人们的巧妙运用，可以变化出千万种不同的组合方式。', '故宫博物院官网');

-- 创建默认管理员账号 (密码: admin123)
INSERT INTO users (username, password_hash, email, is_admin) VALUES 
('admin', '$2a$10$DRjDdCsCkMuqmTV6h.zr0eUEGq8dQ5YTWtmxPpmiY3anTdVid1GRi', 'admin@example.com', 1);
