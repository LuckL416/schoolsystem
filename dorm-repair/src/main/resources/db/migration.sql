-- ============================================
-- Dorm Repair System - Comprehensive Migration
-- ============================================

-- work_order: acceptance & rating fields
ALTER TABLE work_order
    ADD COLUMN acceptance_time DATETIME NULL COMMENT '验收确认时间',
    ADD COLUMN evaluate_attitude INT NULL COMMENT '态度评分 1-5',
    ADD COLUMN evaluate_speed INT NULL COMMENT '速度评分 1-5',
    ADD COLUMN evaluate_quality INT NULL COMMENT '质量评分 1-5',
    ADD COLUMN evaluate_comment VARCHAR(200) NULL COMMENT '文字评价',
    ADD COLUMN evaluate_time DATETIME NULL COMMENT '评价时间',
    ADD COLUMN is_urgent TINYINT NOT NULL DEFAULT 0 COMMENT '是否紧急 0/1',
    ADD COLUMN urgent_level INT NULL COMMENT '紧急级别 1-3',
    ADD COLUMN assigned_teacher_id BIGINT NULL COMMENT '被分配师傅ID',
    ADD COLUMN assign_time DATETIME NULL COMMENT '分配/抢单时间',
    ADD COLUMN is_overdue TINYINT NOT NULL DEFAULT 0 COMMENT '是否超时 0/1';

-- fault_type: timeout config
ALTER TABLE fault_type
    ADD COLUMN timeout_hours INT NOT NULL DEFAULT 48 COMMENT '超时时限(小时)';

-- Notification center table
CREATE TABLE IF NOT EXISTS notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '接收用户ID',
    type VARCHAR(30) NOT NULL COMMENT 'urgent/bad_rating/overdue/low_stock/assign/system',
    title VARCHAR(100) NOT NULL,
    content VARCHAR(255) NOT NULL DEFAULT '',
    related_id BIGINT NULL COMMENT '关联业务ID',
    is_read TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_read (user_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Announcements table
CREATE TABLE IF NOT EXISTS announcement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    category VARCHAR(20) NOT NULL COMMENT 'notice/tip/general',
    is_pinned TINYINT NOT NULL DEFAULT 0,
    publisher_id BIGINT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NULL,
    INDEX idx_category_pinned (category, is_pinned)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Inventory items table
CREATE TABLE IF NOT EXISTS inventory_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    category VARCHAR(20) NOT NULL COMMENT '电工/水工/木工/五金',
    quantity INT NOT NULL DEFAULT 0,
    safety_threshold INT NOT NULL DEFAULT 10,
    unit VARCHAR(10) NOT NULL DEFAULT '个',
    update_time DATETIME NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Inventory records table
CREATE TABLE IF NOT EXISTS inventory_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_id BIGINT NOT NULL,
    type VARCHAR(10) NOT NULL COMMENT 'in/out',
    quantity INT NOT NULL,
    operator_id BIGINT NOT NULL,
    work_order_id BIGINT NULL,
    remark VARCHAR(100) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_item (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed data: 6 maintenance tips
INSERT INTO announcement (title, content, category, is_pinned, publisher_id, create_time) VALUES
('宿舍跳闸恢复指南',
 '<p>跳闸后先检查宿舍总闸(通常在门后配电箱)，将所有开关全部下拉到底再推上去。若仍无法恢复，请逐个拔掉大功率电器(热水壶、电磁炉等)后再尝试。<strong>注意：切勿湿手触碰配电箱。</strong></p>',
 'tip', 0, 1, NOW()),
('空调滤网清洗教程',
 '<p>每学期至少清洗一次。<ol><li>打开空调面板</li><li>取出滤网</li><li>清水冲洗(勿用刷子)</li><li>阴凉处晾干</li><li>装回</li></ol>滤网堵塞会导致制冷差、耗电增加。</p>',
 'tip', 0, 1, NOW()),
('马桶/下水道轻微堵塞处理',
 '<p>先用皮搋子反复按压5-10次。无效可倒半杯洗洁精+一壶热水(非开水)，静置15分钟后冲水。<strong>不要将剩饭、卫生巾、湿厕纸丢入马桶。</strong></p>',
 'tip', 0, 1, NOW()),
('水龙头滴水简易处理',
 '<p>关闭水阀 → 用扳手拧开龙头帽 → 检查橡皮垫圈是否老化破裂 → 若损坏可报修更换。临时应急可在龙头下放桶接水避免浪费。</p>',
 'tip', 0, 1, NOW()),
('热水器不出热水排查',
 '<p>先确认电源指示灯是否亮起 → 检查温度设置是否被调至最低 → 查看进出水阀门是否完全打开。如以上均正常仍无热水，请报修。</p>',
 'tip', 0, 1, NOW()),
('窗户卡住/推拉不畅',
 '<p>检查滑轨是否有异物卡住 → 用湿布清理轨道灰尘 → 可涂抹少量蜡烛/肥皂润滑。勿用蛮力，以免玻璃破裂。</p>',
 'tip', 0, 1, NOW());

-- Update urgent fault types with shorter timeouts
UPDATE fault_type SET timeout_hours = 4 WHERE urgent_level = 1;
UPDATE fault_type SET timeout_hours = 24 WHERE urgent_level = 2;
