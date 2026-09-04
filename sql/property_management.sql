-- MySQL dump 10.13  Distrib 8.0.46, for Linux (x86_64)
--
-- Host: localhost    Database: property_management
-- ------------------------------------------------------
-- Server version	8.0.46

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `t_bill`
--

DROP TABLE IF EXISTS `t_bill`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_bill` (
  `id` bigint NOT NULL COMMENT '账单ID',
  `bill_no` varchar(50) NOT NULL COMMENT '账单编号（规则：BILL + 日期 + 流水号）',
  `room_id` bigint NOT NULL COMMENT '房屋ID',
  `owner_id` bigint DEFAULT NULL COMMENT '当前业主ID（快照，账单生成时关联）',
  `bill_period` varchar(20) NOT NULL COMMENT '账期（如 2024-01, 2024-Q1, 2024）',
  `bill_type` tinyint NOT NULL DEFAULT '1' COMMENT '账单类型：1-周期性账单 2-临时账单 3-滞纳金',
  `bill_date` date NOT NULL COMMENT '出账日期',
  `due_date` date NOT NULL COMMENT '缴费截止日期',
  `total_amount` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '总金额',
  `paid_amount` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '已交金额',
  `discount_amount` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '减免金额',
  `late_fee` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '滞纳金',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0-未缴费 1-部分缴费 2-已缴清 3-已作废 4-已减免 5-已逾期',
  `remark` varchar(500) DEFAULT NULL,
  `del_flag` tinyint NOT NULL DEFAULT '0',
  `create_by` varchar(50) NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(50) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bill_no` (`bill_no`),
  KEY `idx_room_id` (`room_id`),
  KEY `idx_owner_id` (`owner_id`),
  KEY `idx_bill_period` (`bill_period`),
  KEY `idx_status` (`status`),
  KEY `idx_due_date` (`due_date`),
  KEY `idx_bill_date` (`bill_date`),
  KEY `idx_bill_type` (`bill_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='账单主表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_bill`
--

LOCK TABLES `t_bill` WRITE;
/*!40000 ALTER TABLE `t_bill` DISABLE KEYS */;
/*!40000 ALTER TABLE `t_bill` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_bill_item`
--

DROP TABLE IF EXISTS `t_bill_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_bill_item` (
  `id` bigint NOT NULL COMMENT '明细ID',
  `bill_id` bigint NOT NULL COMMENT '账单ID',
  `fee_item_id` bigint NOT NULL COMMENT '费用项ID',
  `fee_item_name` varchar(100) NOT NULL COMMENT '费用项名称（冗余）',
  `calc_base` decimal(12,4) NOT NULL COMMENT '计费基数（面积/用量/户数）',
  `unit_price` decimal(12,4) NOT NULL COMMENT '单价',
  `quantity` decimal(12,2) NOT NULL DEFAULT '1.00' COMMENT '数量（月数/次数）',
  `amount` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '金额 = calc_base * unit_price * quantity',
  `discount_amount` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '减免金额',
  `paid_amount` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '已交金额',
  `remark` varchar(500) DEFAULT NULL,
  `del_flag` tinyint NOT NULL DEFAULT '0',
  `create_by` varchar(50) NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(50) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_bill_id` (`bill_id`),
  KEY `idx_fee_item_id` (`fee_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='账单明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_bill_item`
--

LOCK TABLES `t_bill_item` WRITE;
/*!40000 ALTER TABLE `t_bill_item` DISABLE KEYS */;
/*!40000 ALTER TABLE `t_bill_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_building`
--

DROP TABLE IF EXISTS `t_building`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_building` (
  `id` bigint NOT NULL COMMENT '楼栋ID（雪花算法）',
  `building_code` varchar(20) NOT NULL COMMENT '楼栋编号（如 A1, B2）',
  `building_name` varchar(100) NOT NULL COMMENT '楼栋名称（如 1号楼）',
  `total_units` int NOT NULL DEFAULT '0' COMMENT '总单元数',
  `total_floors` int NOT NULL DEFAULT '0' COMMENT '总层数',
  `total_rooms` int NOT NULL DEFAULT '0' COMMENT '总户数',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-停用 1-启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标志：0-正常 1-删除',
  `create_by` varchar(50) NOT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(50) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_building_code` (`building_code`),
  KEY `idx_status` (`status`),
  KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='楼栋表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_building`
--

LOCK TABLES `t_building` WRITE;
/*!40000 ALTER TABLE `t_building` DISABLE KEYS */;
INSERT INTO `t_building` VALUES (1001,'A1','1号楼',3,18,108,1,1,'一期房源，两梯四户',0,'system','2026-06-18 08:56:41','admin','2026-06-18 17:10:55'),(1002,'A2','2号楼',2,18,72,2,1,'一期房源，两梯四户',0,'system','2026-06-18 08:56:41',NULL,NULL),(1003,'A3','3号楼',1,24,48,3,1,'一期房源，两梯两户',0,'system','2026-06-18 08:56:41',NULL,NULL),(1004,'B1','4号楼',2,30,120,4,1,'二期房源，两梯四户',0,'system','2026-06-18 08:56:41',NULL,NULL),(1005,'B2','5号楼',2,30,120,5,1,'二期房源，两梯四户',0,'system','2026-06-18 08:56:41',NULL,NULL),(1006,'B3','6号楼',3,30,180,6,1,'二期房源，三梯六户',0,'system','2026-06-18 08:56:41',NULL,NULL),(1007,'C1','物业管理中心',0,3,0,7,1,'物业办公区',0,'system','2026-06-18 08:56:41',NULL,NULL),(1008,'C2','地下车库',0,2,200,8,1,'地下两层停车场',0,'system','2026-06-18 08:56:41',NULL,NULL),(2067412717767823362,'A9','9号楼',2,18,72,1,1,'一期房源，两梯四户',0,'admin','2026-06-18 09:02:57',NULL,NULL),(2067532907314737153,'A7','7栋',2,18,0,0,1,NULL,0,'admin','2026-06-18 17:00:33',NULL,NULL);
/*!40000 ALTER TABLE `t_building` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_complaint`
--

DROP TABLE IF EXISTS `t_complaint`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_complaint` (
  `id` bigint NOT NULL COMMENT '投诉ID',
  `owner_id` bigint NOT NULL COMMENT '投诉人（业主ID）',
  `room_id` bigint DEFAULT NULL COMMENT '关联房屋ID',
  `complaint_type` tinyint NOT NULL COMMENT '投诉类型：1-噪音 2-卫生 3-安保 4-维修 5-服务态度 6-其他',
  `title` varchar(200) NOT NULL COMMENT '投诉标题',
  `content` text NOT NULL COMMENT '投诉内容',
  `images` varchar(2000) DEFAULT NULL COMMENT '图片URL（逗号分隔）',
  `urgency_level` tinyint NOT NULL DEFAULT '2' COMMENT '紧急程度：1-一般 2-紧急 3-非常紧急',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0-待处理 1-处理中 2-已处理 3-已关闭 4-已驳回',
  `handler_id` bigint DEFAULT NULL COMMENT '处理人（系统用户ID）',
  `handler_name` varchar(50) DEFAULT NULL COMMENT '处理人姓名',
  `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
  `handle_result` text COMMENT '处理结果',
  `owner_rating` tinyint DEFAULT NULL COMMENT '业主评价：1-5星',
  `owner_feedback` varchar(500) DEFAULT NULL COMMENT '业主反馈',
  `close_time` datetime DEFAULT NULL COMMENT '关闭时间',
  `remark` varchar(500) DEFAULT NULL,
  `del_flag` tinyint NOT NULL DEFAULT '0',
  `create_by` varchar(50) NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(50) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_owner_id` (`owner_id`),
  KEY `idx_room_id` (`room_id`),
  KEY `idx_status` (`status`),
  KEY `idx_complaint_type` (`complaint_type`),
  KEY `idx_urgency_level` (`urgency_level`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='投诉建议表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_complaint`
--

LOCK TABLES `t_complaint` WRITE;
/*!40000 ALTER TABLE `t_complaint` DISABLE KEYS */;
/*!40000 ALTER TABLE `t_complaint` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_fee_item`
--

DROP TABLE IF EXISTS `t_fee_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_fee_item` (
  `id` bigint NOT NULL COMMENT '费用项ID',
  `item_code` varchar(50) NOT NULL COMMENT '费用项编码（如 PROPERTY_FEE, WATER_FEE）',
  `item_name` varchar(100) NOT NULL COMMENT '费用项名称（物业费/水费/电费/停车费）',
  `billing_cycle` tinyint NOT NULL DEFAULT '1' COMMENT '计费周期：1-月 2-季 3-半年 4-年 5-一次性',
  `calc_type` tinyint NOT NULL DEFAULT '1' COMMENT '计费方式：1-按照面积 2-按照户 3-按照用量 4-固定金额',
  `unit_price` decimal(12,4) NOT NULL DEFAULT '0.0000' COMMENT '单价（元）',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-停用 1-启用',
  `remark` varchar(500) DEFAULT NULL,
  `del_flag` tinyint NOT NULL DEFAULT '0',
  `create_by` varchar(50) NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(50) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_item_code` (`item_code`),
  KEY `idx_status` (`status`),
  KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='费用项字典表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_fee_item`
--

LOCK TABLES `t_fee_item` WRITE;
/*!40000 ALTER TABLE `t_fee_item` DISABLE KEYS */;
INSERT INTO `t_fee_item` VALUES (1,'PROPERTY_FEE','物业费',1,1,2.5000,1,1,'',0,'system','2026-06-17 14:56:41',NULL,'2026-06-19 10:50:15'),(2,'WATER_FEE','水费',1,3,4.5000,2,1,'按用量计算，单价元/吨',0,'system','2026-06-17 14:56:41',NULL,NULL),(3,'ELECTRIC_FEE','电费',1,3,0.8000,3,1,'按用量计算，单价元/度',0,'system','2026-06-17 14:56:41',NULL,NULL),(4,'PARKING_FEE','停车费',1,4,300.0000,4,1,'固定金额，元/月',0,'system','2026-06-17 14:56:41',NULL,NULL),(5,'GARBAGE_FEE','垃圾清运费',1,2,15.0000,5,1,'按户收取，元/月',0,'system','2026-06-17 14:56:41',NULL,NULL),(6,'ELEVATOR_FEE','电梯维护费',1,2,20.0000,6,1,'按户收取，元/月',0,'system','2026-06-17 14:56:41',NULL,NULL),(7,'SECURITY_FEE','安保费',1,1,0.5000,7,1,'按面积收取，元/平米/月',0,'system','2026-06-17 14:56:41',NULL,NULL),(8,'LATE_FEE','滞纳金',1,4,0.0000,99,1,'按日计算，日利率0.1%',0,'system','2026-06-17 14:56:41',NULL,NULL);
/*!40000 ALTER TABLE `t_fee_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_fee_standard`
--

DROP TABLE IF EXISTS `t_fee_standard`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_fee_standard` (
  `id` bigint NOT NULL COMMENT '费用标准ID',
  `room_id` bigint NOT NULL COMMENT '房屋ID',
  `fee_item_id` bigint NOT NULL COMMENT '费用项ID',
  `unit_price` decimal(12,4) NOT NULL COMMENT '实际单价（覆盖默认单价）',
  `start_date` date NOT NULL COMMENT '生效日期',
  `end_date` date DEFAULT NULL COMMENT '失效日期（NULL表示长期有效）',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-停用 1-启用',
  `remark` varchar(500) DEFAULT NULL,
  `del_flag` tinyint NOT NULL DEFAULT '0',
  `create_by` varchar(50) NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(50) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_room_fee_date` (`room_id`,`fee_item_id`,`start_date`),
  KEY `idx_room_id` (`room_id`),
  KEY `idx_fee_item_id` (`fee_item_id`),
  KEY `idx_status` (`status`),
  KEY `idx_start_date` (`start_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='费用标准表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_fee_standard`
--

LOCK TABLES `t_fee_standard` WRITE;
/*!40000 ALTER TABLE `t_fee_standard` DISABLE KEYS */;
INSERT INTO `t_fee_standard` VALUES (9001,3005,1,2.0000,'2025-06-01',NULL,1,'老业主优惠价',0,'admin','2026-06-18 16:14:56',NULL,NULL),(9002,3009,1,3.0000,'2026-03-01',NULL,1,'顶层复式特殊单价',0,'admin','2026-06-18 16:14:56',NULL,NULL),(9003,3020,1,5.0000,'2025-01-01',NULL,1,'商铺物业费标准',0,'admin','2026-06-18 16:14:56',NULL,NULL);
/*!40000 ALTER TABLE `t_fee_standard` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_meter_reading`
--

DROP TABLE IF EXISTS `t_meter_reading`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_meter_reading` (
  `id` bigint NOT NULL COMMENT '读数ID',
  `room_id` bigint NOT NULL COMMENT '房屋ID',
  `meter_type` tinyint NOT NULL COMMENT '仪表类型：1-水表 2-电表 3-燃气表',
  `meter_no` varchar(50) NOT NULL COMMENT '仪表编号',
  `previous_reading` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '上次读数',
  `current_reading` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '本次读数',
  `usage_amount` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '用量 = current - previous',
  `reading_date` date NOT NULL COMMENT '抄表日期',
  `reading_type` tinyint NOT NULL DEFAULT '1' COMMENT '抄表方式：1-手动 2-远程 3-自报',
  `reader_id` bigint DEFAULT NULL COMMENT '抄表人ID',
  `reader_name` varchar(50) DEFAULT NULL COMMENT '抄表人姓名',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0-待确认 1-已确认 2-异常',
  `remark` varchar(500) DEFAULT NULL,
  `del_flag` tinyint NOT NULL DEFAULT '0',
  `create_by` varchar(50) NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(50) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_meter_period` (`room_id`,`meter_no`,`reading_date`),
  KEY `idx_room_id` (`room_id`),
  KEY `idx_meter_type` (`meter_type`),
  KEY `idx_reading_date` (`reading_date`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='仪表读数表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_meter_reading`
--

LOCK TABLES `t_meter_reading` WRITE;
/*!40000 ALTER TABLE `t_meter_reading` DISABLE KEYS */;
/*!40000 ALTER TABLE `t_meter_reading` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_notification`
--

DROP TABLE IF EXISTS `t_notification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_notification` (
  `id` bigint NOT NULL COMMENT '通知ID',
  `owner_id` bigint NOT NULL COMMENT '接收业主ID',
  `room_id` bigint DEFAULT NULL COMMENT '关联房屋ID',
  `notify_type` varchar(50) NOT NULL COMMENT '通知类型（BILL_REMIND/PARKING_EXPIRE/COMPLAINT_REPLY）',
  `title` varchar(200) NOT NULL COMMENT '通知标题',
  `content` text NOT NULL COMMENT '通知内容（已渲染变量）',
  `channel` tinyint NOT NULL COMMENT '发送渠道：1-短信 2-邮件 3-站内信 4-小程序',
  `is_read` tinyint NOT NULL DEFAULT '0' COMMENT '是否已读：0-未读 1-已读',
  `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
  `send_status` tinyint NOT NULL DEFAULT '0' COMMENT '发送状态：0-待发送 1-发送成功 2-发送失败',
  `send_time` datetime DEFAULT NULL COMMENT '发送时间',
  `biz_id` bigint DEFAULT NULL COMMENT '业务ID（如账单ID、合同ID）',
  `remark` varchar(500) DEFAULT NULL,
  `del_flag` tinyint NOT NULL DEFAULT '0',
  `create_by` varchar(50) NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(50) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_owner_id` (`owner_id`),
  KEY `idx_room_id` (`room_id`),
  KEY `idx_notify_type` (`notify_type`),
  KEY `idx_is_read` (`is_read`),
  KEY `idx_send_status` (`send_status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_notification`
--

LOCK TABLES `t_notification` WRITE;
/*!40000 ALTER TABLE `t_notification` DISABLE KEYS */;
/*!40000 ALTER TABLE `t_notification` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_notification_template`
--

DROP TABLE IF EXISTS `t_notification_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_notification_template` (
  `id` bigint NOT NULL COMMENT '模板ID',
  `template_code` varchar(50) NOT NULL COMMENT '模板编码（如 BILL_REMIND, PARKING_EXPIRE）',
  `template_name` varchar(100) NOT NULL COMMENT '模板名称',
  `channel` tinyint NOT NULL COMMENT '发送渠道：1-短信 2-邮件 3-站内信 4-小程序订阅 5-公众号',
  `title_template` varchar(200) NOT NULL COMMENT '标题模板（支持 {name} 占位符）',
  `content_template` text NOT NULL COMMENT '内容模板',
  `variables` varchar(500) DEFAULT NULL COMMENT '模板变量列表（name,phone,amount 逗号分隔）',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-停用 1-启用',
  `remark` varchar(500) DEFAULT NULL,
  `del_flag` tinyint NOT NULL DEFAULT '0',
  `create_by` varchar(50) NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(50) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_code` (`template_code`),
  KEY `idx_channel` (`channel`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知模板表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_notification_template`
--

LOCK TABLES `t_notification_template` WRITE;
/*!40000 ALTER TABLE `t_notification_template` DISABLE KEYS */;
INSERT INTO `t_notification_template` VALUES (12001,'BILL_REMIND','账单提醒',3,'【{property.name}】{owner.name}您好，您有新的账单待缴纳','尊敬的业主 {owner.name}，您的 {period} 期账单已生成，金额 {amount} 元，请于 {due_date} 前缴纳。','owner.name,period,amount,due_date',1,NULL,0,'admin','2026-06-18 16:15:05',NULL,NULL),(12002,'PAYMENT_SUCCESS','缴费成功通知',3,'【{property.name}】缴费成功确认','尊敬的业主 {owner.name}，您已成功缴纳 {period} 期费用 {amount} 元，感谢您的支持！','owner.name,period,amount',1,NULL,0,'admin','2026-06-18 16:15:05',NULL,NULL),(12003,'COMPLAINT_REPLY','投诉回复通知',3,'【{property.name}】您的投诉已处理','尊敬的业主 {owner.name}，您于 {create_time} 提交的投诉 \"{title}\" 已处理完毕，处理结果：{result}，请评价。','owner.name,create_time,title,result',1,NULL,0,'admin','2026-06-18 16:15:05',NULL,NULL),(12004,'PARKING_EXPIRE','车位到期提醒',3,'【{property.name}】您的车位即将到期','尊敬的业主 {owner.name}，您的车位 {space_code} 将于 {end_date} 到期，请及时续费。','owner.name,space_code,end_date',1,NULL,0,'admin','2026-06-18 16:15:05',NULL,NULL);
/*!40000 ALTER TABLE `t_notification_template` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_owner`
--

DROP TABLE IF EXISTS `t_owner`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_owner` (
  `id` bigint NOT NULL COMMENT '业主ID',
  `owner_name` varchar(100) NOT NULL COMMENT '业主姓名',
  `phone` varchar(20) NOT NULL COMMENT '手机号（登录账号）',
  `password` varchar(200) NOT NULL COMMENT '登录密码（BCrypt加密）',
  `id_card_type` tinyint NOT NULL DEFAULT '1' COMMENT '证件类型：1-身份证 2-护照 3-港澳台证',
  `id_card_no` varchar(50) NOT NULL COMMENT '证件号码',
  `gender` tinyint DEFAULT NULL COMMENT '性别：0-未知 1-男 2-女',
  `birthday` date DEFAULT NULL COMMENT '出生日期',
  `email` varchar(100) DEFAULT NULL COMMENT '电子邮箱',
  `emergency_contact` varchar(50) DEFAULT NULL COMMENT '紧急联系人',
  `emergency_phone` varchar(20) DEFAULT NULL COMMENT '紧急联系电话',
  `avatar` varchar(500) DEFAULT NULL COMMENT '头像URL',
  `owner_type` tinyint NOT NULL DEFAULT '1' COMMENT '业主类型：1-个人 2-公司 3-共有',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-禁用 1-正常 2-冻结',
  `register_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `last_login_time` datetime DEFAULT NULL COMMENT '最近登录时间',
  `remark` varchar(500) DEFAULT NULL,
  `del_flag` tinyint NOT NULL DEFAULT '0',
  `create_by` varchar(50) NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(50) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_phone` (`phone`),
  UNIQUE KEY `uk_id_card` (`id_card_type`,`id_card_no`),
  KEY `idx_owner_name` (`owner_name`),
  KEY `idx_status` (`status`),
  KEY `idx_register_time` (`register_time`),
  KEY `idx_owner_type` (`owner_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='业主信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_owner`
--

LOCK TABLES `t_owner` WRITE;
/*!40000 ALTER TABLE `t_owner` DISABLE KEYS */;
INSERT INTO `t_owner` VALUES (4001,'张三','13800001001','$2a$10$SKNPfkE6aLsVqXfegxhMs.C75WAFt7DmiBbRtvF.jUtiShyeR./9.',1,'110101199001010011',1,'1990-01-01','zhangsan@email.com','李丽','13900001001',NULL,1,1,'2026-06-18 16:14:38','2026-07-02 19:57:27','一期业主',0,'admin','2026-06-18 16:14:38',NULL,'2026-06-27 17:02:10'),(4002,'李四','13800001002','$2a$10$QmuE3zA.eiX1g11fh4KRwun2dGimIt7DfRk1u5E75/dEn3MurOoDi',1,'110101198505150022',1,'1985-05-15','lisi@email.com','王芳','13900001002',NULL,1,1,'2026-06-18 16:14:38',NULL,'一期业主',0,'admin','2026-06-18 16:14:38',NULL,NULL),(4003,'王五','13800001003','$2a$10$QmuE3zA.eiX1g11fh4KRwun2dGimIt7DfRk1u5E75/dEn3MurOoDi',1,'110101197811230033',2,'1978-11-23','wangwu@email.com','赵明','13900001003',NULL,1,1,'2026-06-18 16:14:38',NULL,'一期业主',0,'admin','2026-06-18 16:14:38',NULL,NULL),(4004,'赵六','13800001004','$2a$10$QmuE3zA.eiX1g11fh4KRwun2dGimIt7DfRk1u5E75/dEn3MurOoDi',1,'110101198812250044',1,'1988-12-25','zhaoliu@email.com','孙丽','13900001004',NULL,1,1,'2026-06-18 16:14:38',NULL,'一期业主',0,'admin','2026-06-18 16:14:38',NULL,NULL),(4005,'孙七','13800001005','$2a$10$QmuE3zA.eiX1g11fh4KRwun2dGimIt7DfRk1u5E75/dEn3MurOoDi',1,'110101199503060055',2,'1995-03-06','sunqi@email.com','周强','13900001005',NULL,1,1,'2026-06-18 16:14:38',NULL,'二期业主',0,'admin','2026-06-18 16:14:38',NULL,NULL),(4006,'周八','13800001006','$2a$10$QmuE3zA.eiX1g11fh4KRwun2dGimIt7DfRk1u5E75/dEn3MurOoDi',1,'110101198207180066',1,'1982-07-18','zhouba@email.com','吴敏','13900001006',NULL,1,1,'2026-06-18 16:14:38',NULL,'二期业主',0,'admin','2026-06-18 16:14:38',NULL,NULL),(4007,'吴九','13800001007','$2a$10$QmuE3zA.eiX1g11fh4KRwun2dGimIt7DfRk1u5E75/dEn3MurOoDi',1,'110101199109090077',1,'1991-09-09','wujiu@email.com','郑华','13900001007',NULL,1,1,'2026-06-18 16:14:38',NULL,'二期业主',0,'admin','2026-06-18 16:14:38',NULL,NULL),(4008,'郑十','13800001008','$2a$10$QmuE3zA.eiX1g11fh4KRwun2dGimIt7DfRk1u5E75/dEn3MurOoDi',1,'110101196512300088',2,'1965-12-30','zhengshi@email.com','陈明','13900001008',NULL,1,1,'2026-06-18 16:14:38',NULL,'三期业主',0,'admin','2026-06-18 16:14:38',NULL,NULL),(4009,'阳光物业管理有限公司','13800001009','$2a$10$QmuE3zA.eiX1g11fh4KRwun2dGimIt7DfRk1u5E75/dEn3MurOoDi',2,'G12345678',NULL,NULL,'company@ygwy.com','刘总','13900001009',NULL,2,1,'2026-06-18 16:14:38',NULL,'企业业主（商铺）',0,'admin','2026-06-18 16:14:38',NULL,NULL),(4010,'陈十一','13800001010','$2a$10$QmuE3zA.eiX1g11fh4KRwun2dGimIt7DfRk1u5E75/dEn3MurOoDi',1,'110101200001010099',1,'2000-01-01','chen11@email.com','林红','13900001010',NULL,1,1,'2026-06-18 16:14:38',NULL,'三期业主',0,'admin','2026-06-18 16:14:38',NULL,NULL),(2071880926017425409,'吕畅','19800000000','$2a$10$JPLdwKlnKuj8czfCqaXel.sq/elVRnmXksNo2RifAjeJH9RR3Xq3W',1,'370900000000000000',1,'2026-06-02','2745559187@qq.com','','19806160160','',1,1,'2026-06-30 16:58:01','2026-07-04 08:41:15','',1,'system','2026-06-30 16:58:01',NULL,'2026-07-04 11:02:50');
/*!40000 ALTER TABLE `t_owner` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_owner_family`
--

DROP TABLE IF EXISTS `t_owner_family`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_owner_family` (
  `id` bigint NOT NULL COMMENT '家庭成员ID',
  `owner_id` bigint NOT NULL COMMENT '所属业主ID',
  `family_name` varchar(100) NOT NULL COMMENT '成员姓名',
  `relationship` varchar(20) NOT NULL COMMENT '与业主关系（配偶/子女/父母/其他）',
  `phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `id_card_no` varchar(50) DEFAULT NULL COMMENT '证件号码',
  `gender` tinyint DEFAULT NULL COMMENT '性别：0-未知 1-男 2-女',
  `birthday` date DEFAULT NULL COMMENT '出生日期',
  `is_emergency` tinyint NOT NULL DEFAULT '0' COMMENT '是否紧急联系人：0-否 1-是',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-无效 1-有效',
  `remark` varchar(500) DEFAULT NULL,
  `del_flag` tinyint NOT NULL DEFAULT '0',
  `create_by` varchar(50) NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(50) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_owner_id` (`owner_id`),
  KEY `idx_relationship` (`relationship`),
  KEY `idx_is_emergency` (`is_emergency`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='家庭成员表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_owner_family`
--

LOCK TABLES `t_owner_family` WRITE;
/*!40000 ALTER TABLE `t_owner_family` DISABLE KEYS */;
INSERT INTO `t_owner_family` VALUES (13001,4001,'李丽','配偶','13900001001','110101199205200022',2,'1992-05-20',1,1,NULL,0,'admin','2026-06-18 16:15:07',NULL,NULL),(13002,4001,'张小明','子女',NULL,NULL,1,'2018-03-15',0,1,'儿子',0,'admin','2026-06-18 16:15:07',NULL,NULL),(13003,4003,'赵明','配偶','13900001003','110101198012150044',2,'1980-12-15',1,1,NULL,0,'admin','2026-06-18 16:15:07',NULL,NULL),(13004,4007,'吴夫人','配偶','13900001007','110101199305060066',2,'1993-05-06',1,1,NULL,0,'admin','2026-06-18 16:15:07',NULL,NULL);
/*!40000 ALTER TABLE `t_owner_family` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_owner_room`
--

DROP TABLE IF EXISTS `t_owner_room`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_owner_room` (
  `id` bigint NOT NULL COMMENT '关联ID',
  `owner_id` bigint NOT NULL COMMENT '业主ID',
  `room_id` bigint NOT NULL COMMENT '房屋ID',
  `relation_type` tinyint NOT NULL DEFAULT '1' COMMENT '关系类型：1-业主 2-家属 3-租客',
  `is_primary` tinyint NOT NULL DEFAULT '0' COMMENT '是否主要业主：0-否 1-是',
  `move_in_time` date DEFAULT NULL COMMENT '入住时间',
  `move_out_time` date DEFAULT NULL COMMENT '搬离时间',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-无效 1-有效',
  `remark` varchar(500) DEFAULT NULL,
  `del_flag` tinyint NOT NULL DEFAULT '0',
  `create_by` varchar(50) NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(50) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_owner_room` (`owner_id`,`room_id`,`relation_type`),
  KEY `idx_owner_id` (`owner_id`),
  KEY `idx_room_id` (`room_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='业主房屋关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_owner_room`
--

LOCK TABLES `t_owner_room` WRITE;
/*!40000 ALTER TABLE `t_owner_room` DISABLE KEYS */;
INSERT INTO `t_owner_room` VALUES (5001,4001,3001,1,1,'2025-01-01',NULL,1,'主要住宅',0,'admin','2026-06-18 16:14:43',NULL,NULL),(5002,4001,3005,1,0,'2025-06-01',NULL,1,'第二套住宅',0,'admin','2026-06-18 16:14:43',NULL,NULL),(5003,4002,3002,1,1,'2025-03-15',NULL,1,NULL,0,'admin','2026-06-18 16:14:43',NULL,NULL),(5004,4003,3003,1,1,'2025-02-01',NULL,1,NULL,0,'admin','2026-06-18 16:14:43',NULL,NULL),(5005,4004,3004,1,1,'2025-04-01',NULL,1,'出租中',0,'admin','2026-06-18 16:14:43',NULL,NULL),(5006,4005,3007,1,1,'2025-05-01',NULL,1,NULL,0,'admin','2026-06-18 16:14:43',NULL,NULL),(5007,4006,3008,1,1,'2025-06-01',NULL,1,NULL,0,'admin','2026-06-18 16:14:43',NULL,NULL),(5008,4007,3010,1,1,'2025-07-01',NULL,1,NULL,0,'admin','2026-06-18 16:14:43',NULL,NULL),(5009,4008,3011,1,1,'2025-08-01',NULL,1,NULL,0,'admin','2026-06-18 16:14:43',NULL,NULL),(5010,4009,3020,1,1,'2025-01-01',NULL,1,'物业自营商铺',0,'admin','2026-06-18 16:14:43',NULL,NULL),(5011,4010,3015,1,1,'2026-01-01',NULL,1,'新入住业主',0,'admin','2026-06-18 16:14:43',NULL,NULL),(5012,4001,3009,1,0,'2026-03-01',NULL,1,'顶层复式',0,'admin','2026-06-18 16:14:43',NULL,NULL),(2072653050884870145,2071880926017425409,3019,1,1,'2026-07-02',NULL,1,NULL,0,'system','2026-07-02 20:06:10',NULL,NULL);
/*!40000 ALTER TABLE `t_owner_room` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_owner_vehicle`
--

DROP TABLE IF EXISTS `t_owner_vehicle`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_owner_vehicle` (
  `id` bigint NOT NULL COMMENT '车辆ID',
  `owner_id` bigint NOT NULL COMMENT '业主ID',
  `plate_no` varchar(20) NOT NULL COMMENT '车牌号',
  `vehicle_type` tinyint NOT NULL DEFAULT '1' COMMENT '车辆类型：1-小型车 2-SUV 3-新能源 4-摩托车',
  `vehicle_color` varchar(20) DEFAULT NULL COMMENT '车辆颜色',
  `vehicle_brand` varchar(50) DEFAULT NULL COMMENT '车辆品牌',
  `parking_space_id` bigint DEFAULT NULL COMMENT '绑定车位ID',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-无效 1-有效',
  `remark` varchar(500) DEFAULT NULL,
  `del_flag` tinyint NOT NULL DEFAULT '0',
  `create_by` varchar(50) NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(50) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plate_no` (`plate_no`),
  KEY `idx_owner_id` (`owner_id`),
  KEY `idx_parking_space_id` (`parking_space_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='业主车辆表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_owner_vehicle`
--

LOCK TABLES `t_owner_vehicle` WRITE;
/*!40000 ALTER TABLE `t_owner_vehicle` DISABLE KEYS */;
INSERT INTO `t_owner_vehicle` VALUES (6001,4001,'京A·88888',1,'黑色','宝马X5',NULL,1,NULL,0,'admin','2026-06-18 16:14:46',NULL,NULL),(6002,4001,'京A·66666',3,'白色','特斯拉Model Y',NULL,1,'新能源',0,'admin','2026-06-18 16:14:46',NULL,NULL),(6003,4003,'京B·12345',1,'白色','丰田凯美瑞',NULL,1,NULL,0,'admin','2026-06-18 16:14:46',NULL,NULL),(6004,4005,'京C·56789',2,'黑色','奥迪Q7',NULL,1,'SUV',0,'admin','2026-06-18 16:14:46',NULL,NULL),(6005,4007,'京D·99999',3,'蓝色','比亚迪汉',NULL,1,'新能源',0,'admin','2026-06-18 16:14:46',NULL,NULL),(6006,4008,'京E·11111',1,'红色','本田思域',NULL,1,NULL,0,'admin','2026-06-18 16:14:46',NULL,NULL);
/*!40000 ALTER TABLE `t_owner_vehicle` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_parking_change_log`
--

DROP TABLE IF EXISTS `t_parking_change_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_parking_change_log` (
  `id` bigint NOT NULL COMMENT '日志ID',
  `space_id` bigint NOT NULL COMMENT '车位ID',
  `space_code` varchar(50) DEFAULT NULL COMMENT '车位编号（冗余）',
  `change_type` varchar(20) NOT NULL COMMENT '变更类型：BIND/CHANGE/UNBIND',
  `old_owner_id` bigint DEFAULT NULL COMMENT '变更前业主ID',
  `new_owner_id` bigint DEFAULT NULL COMMENT '变更后业主ID',
  `old_room_id` bigint DEFAULT NULL COMMENT '变更前房屋ID',
  `new_room_id` bigint DEFAULT NULL COMMENT '变更后房屋ID',
  `old_status` tinyint DEFAULT NULL COMMENT '变更前状态',
  `new_status` tinyint DEFAULT NULL COMMENT '变更后状态',
  `remark` varchar(500) DEFAULT NULL,
  `operator` varchar(50) DEFAULT NULL COMMENT '操作人',
  `del_flag` tinyint NOT NULL DEFAULT '0',
  `create_by` varchar(50) NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_space_id` (`space_id`),
  KEY `idx_change_type` (`change_type`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车位变更日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_parking_change_log`
--

LOCK TABLES `t_parking_change_log` WRITE;
/*!40000 ALTER TABLE `t_parking_change_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `t_parking_change_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_parking_lease`
--

DROP TABLE IF EXISTS `t_parking_lease`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_parking_lease` (
  `id` bigint NOT NULL COMMENT '合同ID',
  `contract_no` varchar(50) NOT NULL COMMENT '合同编号',
  `space_id` bigint NOT NULL COMMENT '车位ID',
  `owner_id` bigint NOT NULL COMMENT '承租业主ID',
  `plate_no` varchar(20) DEFAULT NULL COMMENT '绑定车牌号',
  `lease_start` date NOT NULL COMMENT '租赁开始日期',
  `lease_end` date NOT NULL COMMENT '租赁结束日期',
  `monthly_fee` decimal(10,2) NOT NULL COMMENT '月租金',
  `total_amount` decimal(12,2) NOT NULL COMMENT '合同总金额',
  `paid_amount` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '已付金额',
  `payment_method` tinyint DEFAULT NULL COMMENT '付款方式：1-月付 2-季付 3-半年付 4-年付 5-一次性',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0-待生效 1-生效中 2-已到期 3-已终止 4-已续签',
  `remark` varchar(500) DEFAULT NULL,
  `del_flag` tinyint NOT NULL DEFAULT '0',
  `create_by` varchar(50) NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(50) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_contract_no` (`contract_no`),
  KEY `idx_space_id` (`space_id`),
  KEY `idx_owner_id` (`owner_id`),
  KEY `idx_status` (`status`),
  KEY `idx_lease_end` (`lease_end`),
  KEY `idx_plate_no` (`plate_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车位租赁合同表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_parking_lease`
--

LOCK TABLES `t_parking_lease` WRITE;
/*!40000 ALTER TABLE `t_parking_lease` DISABLE KEYS */;
/*!40000 ALTER TABLE `t_parking_lease` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_parking_space`
--

DROP TABLE IF EXISTS `t_parking_space`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_parking_space` (
  `id` bigint NOT NULL COMMENT '车位ID',
  `space_code` varchar(50) NOT NULL COMMENT '车位编号（如 B1-A01）',
  `space_name` varchar(100) NOT NULL COMMENT '车位名称（如 负一层A区01号）',
  `space_type` tinyint NOT NULL DEFAULT '1' COMMENT '车位类型：1-标准车位 2-子母车位 3-机械车位 4-充电桩车位',
  `area` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '面积（平方米）',
  `floor` varchar(20) DEFAULT NULL COMMENT '所属楼层（B1/B2/F1...）',
  `zone` varchar(50) DEFAULT NULL COMMENT '所属区域（A区/B区/地下...）',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  `owner_id` bigint DEFAULT NULL COMMENT '当前绑定业主ID',
  `room_id` bigint DEFAULT NULL COMMENT '关联房屋ID',
  `rental_type` tinyint NOT NULL DEFAULT '1' COMMENT '使用方式：1-自有 2-租赁 3-临时',
  `monthly_fee` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '月租费用（元/月）',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-空闲 1-已售 2-已租 3-临时使用 4-维修中',
  `remark` varchar(500) DEFAULT NULL,
  `del_flag` tinyint NOT NULL DEFAULT '0',
  `create_by` varchar(50) NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(50) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_space_code` (`space_code`),
  KEY `idx_owner_id` (`owner_id`),
  KEY `idx_room_id` (`room_id`),
  KEY `idx_status` (`status`),
  KEY `idx_space_type` (`space_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车位信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_parking_space`
--

LOCK TABLES `t_parking_space` WRITE;
/*!40000 ALTER TABLE `t_parking_space` DISABLE KEYS */;
INSERT INTO `t_parking_space` VALUES (7001,'B1-A01','负一层A区01号',1,12.50,'B1','A区',0,4003,3003,1,300.00,1,'标准车位',0,'admin','2026-06-18 16:14:50',NULL,NULL),(7002,'B1-A02','负一层A区02号',1,12.50,'B1','A区',0,NULL,NULL,2,300.00,2,'已出租给张三',0,'admin','2026-06-18 16:14:50',NULL,NULL),(7003,'B1-A03','负一层A区03号',1,12.50,'B1','A区',0,4005,3007,1,300.00,1,NULL,0,'admin','2026-06-18 16:14:50',NULL,NULL),(7004,'B1-B01','负一层B区01号',4,14.00,'B1','B区',0,4001,3001,1,350.00,1,'充电桩车位',0,'admin','2026-06-18 16:14:50',NULL,NULL),(7005,'B1-B02','负一层B区02号',1,12.50,'B1','B区',0,NULL,NULL,3,5.00,0,'临时车位',0,'admin','2026-06-18 16:14:50',NULL,NULL),(7006,'B2-A01','负二层A区01号',1,12.50,'B2','A区',0,4007,3010,1,300.00,1,NULL,0,'admin','2026-06-18 16:14:50',NULL,NULL),(7007,'B2-A02','负二层A区02号',2,22.00,'B2','A区',0,NULL,NULL,2,450.00,2,'子母车位可停两辆',0,'admin','2026-06-18 16:14:50',NULL,NULL),(7008,'B2-B01','负二层B区01号',1,12.50,'B2','B区',0,4008,3011,1,300.00,2,'出租中',0,'admin','2026-06-18 16:14:50',NULL,NULL);
/*!40000 ALTER TABLE `t_parking_space` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_parking_usage`
--

DROP TABLE IF EXISTS `t_parking_usage`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_parking_usage` (
  `id` bigint NOT NULL COMMENT '使用记录ID',
  `space_id` bigint NOT NULL COMMENT '车位ID',
  `owner_id` bigint NOT NULL COMMENT '使用业主ID',
  `vehicle_id` bigint DEFAULT NULL COMMENT '绑定车辆ID',
  `plate_no` varchar(20) DEFAULT NULL COMMENT '车牌号（冗余）',
  `usage_type` tinyint NOT NULL COMMENT '使用类型：1-月租 2-临时 3-访客',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `duration_hours` decimal(10,2) DEFAULT NULL COMMENT '停车时长（小时）',
  `fee_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '费用金额',
  `payment_status` tinyint NOT NULL DEFAULT '0' COMMENT '支付状态：0-未支付 1-已支付',
  `payment_id` bigint DEFAULT NULL COMMENT '关联支付ID',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0-使用中 1-已完成 2-异常离场',
  `remark` varchar(500) DEFAULT NULL,
  `del_flag` tinyint NOT NULL DEFAULT '0',
  `create_by` varchar(50) NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(50) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_space_id` (`space_id`),
  KEY `idx_owner_id` (`owner_id`),
  KEY `idx_vehicle_id` (`vehicle_id`),
  KEY `idx_plate_no` (`plate_no`),
  KEY `idx_start_time` (`start_time`),
  KEY `idx_status` (`status`),
  KEY `idx_payment_status` (`payment_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车位使用记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_parking_usage`
--

LOCK TABLES `t_parking_usage` WRITE;
/*!40000 ALTER TABLE `t_parking_usage` DISABLE KEYS */;
/*!40000 ALTER TABLE `t_parking_usage` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_parking_warning`
--

DROP TABLE IF EXISTS `t_parking_warning`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_parking_warning` (
  `id` bigint NOT NULL COMMENT '预警ID',
  `space_id` bigint DEFAULT NULL COMMENT '关联车位ID',
  `warning_type` varchar(50) NOT NULL COMMENT '预警类型：LEASE_EXPIRED/SPACE_IDLE/PAYMENT_PENDING/OCCUPANCY_ANOMALY/LEASE_EXPIRING',
  `warning_level` varchar(10) NOT NULL COMMENT '预警等级：LOW/MEDIUM/HIGH',
  `description` varchar(1000) DEFAULT NULL COMMENT '预警描述',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '处理状态：0-待处理 1-处理中 2-已处理 3-已关闭',
  `handler` varchar(50) DEFAULT NULL COMMENT '处理人',
  `handle_remark` varchar(500) DEFAULT NULL COMMENT '处理备注',
  `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
  `batch_no` varchar(20) DEFAULT NULL COMMENT '对账批次号（yyyyMMddHHmmss）',
  `remark` varchar(500) DEFAULT NULL,
  `del_flag` tinyint NOT NULL DEFAULT '0',
  `create_by` varchar(50) NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(50) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_space_id` (`space_id`),
  KEY `idx_warning_type` (`warning_type`),
  KEY `idx_status` (`status`),
  KEY `idx_batch_no` (`batch_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车位预警表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_parking_warning`
--

LOCK TABLES `t_parking_warning` WRITE;
/*!40000 ALTER TABLE `t_parking_warning` DISABLE KEYS */;
/*!40000 ALTER TABLE `t_parking_warning` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_payment`
--

DROP TABLE IF EXISTS `t_payment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_payment` (
  `id` bigint NOT NULL COMMENT '支付ID',
  `payment_no` varchar(50) NOT NULL COMMENT '支付单号（规则：PAY + 日期 + 流水号）',
  `bill_id` bigint NOT NULL COMMENT '关联账单ID',
  `room_id` bigint NOT NULL COMMENT '房屋ID（冗余）',
  `owner_id` bigint NOT NULL COMMENT '付款业主ID',
  `payment_method` tinyint NOT NULL COMMENT '支付方式：1-支付宝 2-微信 3-银行卡 4-现金 5-转账 6-其他',
  `payment_amount` decimal(12,2) NOT NULL COMMENT '支付金额',
  `payment_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '支付时间',
  `transaction_id` varchar(100) DEFAULT NULL COMMENT '第三方支付流水号（微信/支付宝订单号）',
  `payment_status` tinyint NOT NULL DEFAULT '0' COMMENT '支付状态：0-待支付 1-支付中 2-支付成功 3-支付失败 4-已退款 5-部分退款',
  `refund_amount` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '已退款金额',
  `payer_name` varchar(100) DEFAULT NULL COMMENT '付款人姓名',
  `payer_phone` varchar(20) DEFAULT NULL COMMENT '付款人手机号',
  `remark` varchar(500) DEFAULT NULL,
  `del_flag` tinyint NOT NULL DEFAULT '0',
  `create_by` varchar(50) NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(50) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_no` (`payment_no`),
  KEY `idx_bill_id` (`bill_id`),
  KEY `idx_owner_id` (`owner_id`),
  KEY `idx_room_id` (`room_id`),
  KEY `idx_payment_method` (`payment_method`),
  KEY `idx_payment_status` (`payment_status`),
  KEY `idx_payment_time` (`payment_time`),
  KEY `idx_transaction_id` (`transaction_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_payment`
--

LOCK TABLES `t_payment` WRITE;
/*!40000 ALTER TABLE `t_payment` DISABLE KEYS */;
/*!40000 ALTER TABLE `t_payment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_payment_refund`
--

DROP TABLE IF EXISTS `t_payment_refund`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_payment_refund` (
  `id` bigint NOT NULL COMMENT '退款ID',
  `refund_no` varchar(50) NOT NULL COMMENT '退款单号',
  `payment_id` bigint NOT NULL COMMENT '原支付记录ID',
  `refund_amount` decimal(12,2) NOT NULL COMMENT '退款金额',
  `refund_reason` varchar(500) NOT NULL COMMENT '退款原因',
  `refund_type` tinyint NOT NULL COMMENT '退款类型：1-全额退款 2-部分退款',
  `refund_method` tinyint NOT NULL COMMENT '退款方式：1-原路退回 2-手动退款',
  `refund_status` tinyint NOT NULL DEFAULT '0' COMMENT '退款状态：0-待审核 1-审核通过 2-退款中 3-退款成功 4-退款失败 5-审核驳回',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人（系统用户ID）',
  `operator_name` varchar(50) DEFAULT NULL COMMENT '操作人姓名',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `audit_remark` varchar(500) DEFAULT NULL COMMENT '审核意见',
  `remark` varchar(500) DEFAULT NULL,
  `del_flag` tinyint NOT NULL DEFAULT '0',
  `create_by` varchar(50) NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(50) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refund_no` (`refund_no`),
  KEY `idx_payment_id` (`payment_id`),
  KEY `idx_refund_status` (`refund_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='退款记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_payment_refund`
--

LOCK TABLES `t_payment_refund` WRITE;
/*!40000 ALTER TABLE `t_payment_refund` DISABLE KEYS */;
/*!40000 ALTER TABLE `t_payment_refund` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_room`
--

DROP TABLE IF EXISTS `t_room`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_room` (
  `id` bigint NOT NULL COMMENT '房屋ID',
  `building_id` bigint NOT NULL COMMENT '所属楼栋ID',
  `unit_id` bigint NOT NULL COMMENT '所属单元ID',
  `room_code` varchar(50) NOT NULL COMMENT '房号（如 A1-1-101）',
  `room_name` varchar(100) NOT NULL COMMENT '房间名称（如 1栋1单元101）',
  `floor` int NOT NULL COMMENT '所在楼层',
  `room_type` tinyint NOT NULL DEFAULT '1' COMMENT '房屋类型：1-住宅 2-商铺 3-办公 4-仓库 5-车位',
  `area` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '建筑面积（平方米）',
  `usable_area` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '使用面积（平方米）',
  `orientation` varchar(20) DEFAULT NULL COMMENT '朝向（东/南/西/北/南北通透）',
  `decoration_status` tinyint DEFAULT '0' COMMENT '装修状态：0-毛坯 1-简装 2-精装 3-豪装',
  `occupancy_status` tinyint NOT NULL DEFAULT '0' COMMENT '入住状态：0-空置 1-自住 2-出租 3-装修中',
  `property_fee_rate` decimal(10,4) NOT NULL DEFAULT '0.0000' COMMENT '物业费单价（元/平米/月）',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-停用 1-启用',
  `remark` varchar(500) DEFAULT NULL,
  `del_flag` tinyint NOT NULL DEFAULT '0',
  `create_by` varchar(50) NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(50) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_room_code` (`building_id`,`unit_id`,`room_code`),
  KEY `idx_building_id` (`building_id`),
  KEY `idx_unit_id` (`unit_id`),
  KEY `idx_floor` (`floor`),
  KEY `idx_occupancy_status` (`occupancy_status`),
  KEY `idx_room_type` (`room_type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='房屋/房间表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_room`
--

LOCK TABLES `t_room` WRITE;
/*!40000 ALTER TABLE `t_room` DISABLE KEYS */;
INSERT INTO `t_room` VALUES (3001,1001,2001,'A1-1-101','1栋1单元101',1,1,120.00,105.00,'南北通透',2,1,2.5000,1,'业主自住，精装',0,'admin','2026-06-18 16:14:35',NULL,NULL),(3002,1001,2001,'A1-1-102','1栋1单元102',1,1,95.00,82.00,'南',1,1,2.5000,1,'出租状态',0,'admin','2026-06-18 16:14:35',NULL,NULL),(3003,1001,2001,'A1-1-501','1栋1单元501',5,1,120.00,105.00,'南北通透',2,1,2.5000,1,NULL,0,'admin','2026-06-18 16:14:35',NULL,NULL),(3004,1001,2001,'A1-1-502','1栋1单元502',5,1,95.00,82.00,'南',1,2,2.5000,1,'出租',0,'admin','2026-06-18 16:14:35',NULL,NULL),(3005,1001,2001,'A1-1-1001','1栋1单元1001',10,1,130.00,112.00,'南北通透',3,1,2.5000,1,'豪装自住',0,'admin','2026-06-18 16:14:35',NULL,NULL),(3006,1001,2001,'A1-1-1002','1栋1单元1002',10,1,95.00,82.00,'南',2,0,2.5000,1,'空置',0,'admin','2026-06-18 16:14:35',NULL,NULL),(3007,1001,2002,'A1-2-101','1栋2单元101',1,1,110.00,96.00,'南北通透',2,1,2.5000,1,NULL,0,'admin','2026-06-18 16:14:35',NULL,NULL),(3008,1001,2002,'A1-2-102','1栋2单元102',1,1,85.00,73.00,'南',1,1,2.5000,1,NULL,0,'admin','2026-06-18 16:14:35',NULL,NULL),(3009,1001,2002,'A1-2-1801','1栋2单元1801',18,1,140.00,120.00,'南北通透',3,1,2.5000,1,'顶层复式',0,'admin','2026-06-18 16:14:35',NULL,NULL),(3010,1002,2003,'A2-1-101','2栋1单元101',1,1,125.00,108.00,'南北通透',2,1,2.5000,1,NULL,0,'admin','2026-06-18 16:14:35',NULL,NULL),(3011,1002,2003,'A2-1-102','2栋1单元102',1,1,90.00,78.00,'南',1,1,2.5000,1,NULL,0,'admin','2026-06-18 16:14:35',NULL,NULL),(3012,1002,2003,'A2-1-801','2栋1单元801',8,1,125.00,108.00,'南北通透',2,0,2.5000,1,'空置待售',0,'admin','2026-06-18 16:14:35',NULL,NULL),(3013,1002,2004,'A2-2-101','2栋2单元101',1,1,115.00,100.00,'南',2,1,2.5000,1,NULL,0,'admin','2026-06-18 16:14:35',NULL,NULL),(3014,1002,2004,'A2-2-102','2栋2单元102',1,1,88.00,75.00,'南',1,2,2.5000,1,'出租',0,'admin','2026-06-18 16:14:35',NULL,NULL),(3015,1003,2005,'A3-1-101','3栋1单元101',1,1,150.00,130.00,'南北通透',2,1,2.5000,1,'大户型',0,'admin','2026-06-18 16:14:35',NULL,NULL),(3016,1003,2005,'A3-1-102','3栋1单元102',1,1,100.00,86.00,'南',1,1,2.5000,1,NULL,0,'admin','2026-06-18 16:14:35',NULL,NULL),(3017,1003,2005,'A3-1-2401','3栋1单元2401',24,1,180.00,155.00,'南北通透',3,1,2.5000,1,'顶层大平层',0,'admin','2026-06-18 16:14:35',NULL,NULL),(3018,1004,2006,'B1-1-101','4栋1单元101',1,1,110.00,95.00,'南',2,1,2.5000,1,NULL,0,'admin','2026-06-18 16:14:35',NULL,NULL),(3019,1004,2006,'B1-1-102','4栋1单元102',1,1,110.00,95.00,'南',2,1,2.5000,1,NULL,0,'admin','2026-06-18 16:14:35',NULL,NULL),(3020,1001,2001,'A1-1-001','1栋1单元001商铺',1,2,80.00,70.00,'东',2,1,5.0000,1,'临街商铺',0,'admin','2026-06-18 16:14:35',NULL,NULL),(2068333742076526593,1006,2012,'A1-1-101','6栋1单元102',1,0,0.00,0.00,'',0,0,0.0000,0,'',1,'system','2026-06-20 22:02:47',NULL,'2026-06-20 22:05:17');
/*!40000 ALTER TABLE `t_room` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_stats_arrears`
--

DROP TABLE IF EXISTS `t_stats_arrears`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_stats_arrears` (
  `id` bigint NOT NULL COMMENT '统计ID',
  `room_id` bigint NOT NULL COMMENT '房屋ID',
  `room_code` varchar(50) NOT NULL COMMENT '房号',
  `owner_name` varchar(100) DEFAULT NULL COMMENT '业主姓名',
  `owner_phone` varchar(20) DEFAULT NULL COMMENT '业主手机号',
  `arrears_amount` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '欠费金额',
  `late_fee` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '滞纳金',
  `total_amount` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '总欠费（含滞纳金）',
  `arrears_months` int NOT NULL DEFAULT '0' COMMENT '欠费月数',
  `latest_due_date` date DEFAULT NULL COMMENT '最近到期日',
  `stats_snapshot_time` datetime NOT NULL COMMENT '快照时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_by` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `update_by` varchar(50) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_room_id` (`room_id`),
  KEY `idx_total_amount` (`total_amount`),
  KEY `idx_arrears_months` (`arrears_months`),
  KEY `idx_snapshot_time` (`stats_snapshot_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='欠费统计表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_stats_arrears`
--

LOCK TABLES `t_stats_arrears` WRITE;
/*!40000 ALTER TABLE `t_stats_arrears` DISABLE KEYS */;
/*!40000 ALTER TABLE `t_stats_arrears` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_stats_daily`
--

DROP TABLE IF EXISTS `t_stats_daily`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_stats_daily` (
  `id` bigint NOT NULL COMMENT '统计ID',
  `stats_date` date NOT NULL COMMENT '统计日期',
  `stats_type` varchar(50) NOT NULL COMMENT '统计类型（PAYMENT/OWNER/BILL/PARKING）',
  `stats_key` varchar(100) NOT NULL COMMENT '统计维度（如 TOTAL_AMOUNT, NEW_OWNER_COUNT）',
  `stats_value` decimal(16,2) NOT NULL DEFAULT '0.00' COMMENT '统计值',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stats_date_type_key` (`stats_date`,`stats_type`,`stats_key`),
  KEY `idx_stats_date` (`stats_date`),
  KEY `idx_stats_type` (`stats_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='每日统计汇总表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_stats_daily`
--

LOCK TABLES `t_stats_daily` WRITE;
/*!40000 ALTER TABLE `t_stats_daily` DISABLE KEYS */;
/*!40000 ALTER TABLE `t_stats_daily` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_sys_config`
--

DROP TABLE IF EXISTS `t_sys_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_sys_config` (
  `id` bigint NOT NULL COMMENT '配置ID',
  `config_key` varchar(100) NOT NULL COMMENT '配置键（如 property.name, late.fee.rate）',
  `config_value` varchar(500) NOT NULL COMMENT '配置值',
  `config_type` tinyint NOT NULL DEFAULT '1' COMMENT '配置类型：1-系统参数 2-业务参数 3-通知参数',
  `group_name` varchar(50) DEFAULT NULL COMMENT '配置分组（如 payment, notification, system）',
  `description` varchar(200) DEFAULT NULL COMMENT '配置描述',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-禁用 1-启用',
  `remark` varchar(500) DEFAULT NULL,
  `del_flag` tinyint NOT NULL DEFAULT '0',
  `create_by` varchar(50) NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(50) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`),
  KEY `idx_group_name` (`group_name`),
  KEY `idx_config_type` (`config_type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_sys_config`
--

LOCK TABLES `t_sys_config` WRITE;
/*!40000 ALTER TABLE `t_sys_config` DISABLE KEYS */;
INSERT INTO `t_sys_config` VALUES (1,'property.name','智慧物业管理平台',1,'system','物业系统名称',1,NULL,0,'system','2026-06-17 14:56:41',NULL,NULL),(2,'property.address','xx市xx区xx路xx号',1,'system','物业公司地址',1,NULL,0,'system','2026-06-17 14:56:41',NULL,NULL),(3,'property.phone','400-xxx-xxxx',1,'system','物业客服电话',1,NULL,0,'system','2026-06-17 14:56:41',NULL,NULL),(4,'late.fee.rate','0.001',2,'payment','滞纳金日利率（0.1%）',1,NULL,0,'system','2026-06-17 14:56:41',NULL,NULL),(5,'late.fee.days','30',2,'payment','超过截止日多少天后开始计算滞纳金',1,NULL,0,'system','2026-06-17 14:56:41',NULL,NULL),(6,'bill.generate.day','1',2,'bill','每月几号生成账单',1,NULL,0,'system','2026-06-17 14:56:41',NULL,NULL),(7,'bill.due.days','15',2,'bill','账单生成后多少天截止缴费',1,NULL,0,'system','2026-06-17 14:56:41',NULL,NULL),(8,'sms.expire.minutes','5',3,'notification','短信验证码有效期（分钟）',1,NULL,0,'system','2026-06-17 14:56:41',NULL,NULL),(9,'parking.hourly.rate','5.00',2,'parking','临时停车每小时费用（元）',1,NULL,0,'system','2026-06-17 14:56:41',NULL,NULL),(10,'parking.free.minutes','30',2,'parking','临时停车免费时长（分钟）',1,NULL,0,'system','2026-06-17 14:56:41',NULL,NULL);
/*!40000 ALTER TABLE `t_sys_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_sys_dept`
--

DROP TABLE IF EXISTS `t_sys_dept`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_sys_dept` (
  `id` bigint NOT NULL COMMENT '部门ID',
  `dept_name` varchar(100) NOT NULL COMMENT '部门名称',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父级部门ID（0表示根）',
  `ancestors` varchar(500) DEFAULT NULL COMMENT '祖先ID链（如 1,2,3）',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `leader_id` bigint DEFAULT NULL COMMENT '负责人ID',
  `leader_name` varchar(50) DEFAULT NULL COMMENT '负责人姓名',
  `phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-停用 1-启用',
  `remark` varchar(500) DEFAULT NULL,
  `del_flag` tinyint NOT NULL DEFAULT '0',
  `create_by` varchar(50) NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(50) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_status` (`status`),
  KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='部门表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_sys_dept`
--

LOCK TABLES `t_sys_dept` WRITE;
/*!40000 ALTER TABLE `t_sys_dept` DISABLE KEYS */;
/*!40000 ALTER TABLE `t_sys_dept` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_sys_file`
--

DROP TABLE IF EXISTS `t_sys_file`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_sys_file` (
  `id` bigint NOT NULL COMMENT '文件ID',
  `file_name` varchar(200) NOT NULL COMMENT '原始文件名',
  `file_path` varchar(500) NOT NULL COMMENT '存储路径',
  `file_size` bigint NOT NULL COMMENT '文件大小（字节）',
  `file_type` varchar(50) NOT NULL COMMENT '文件类型（image/png, application/pdf）',
  `file_ext` varchar(20) NOT NULL COMMENT '文件扩展名（.png, .pdf）',
  `storage_type` tinyint NOT NULL DEFAULT '1' COMMENT '存储方式：1-本地 2-OSS 3-MinIO',
  `biz_type` varchar(50) DEFAULT NULL COMMENT '业务类型（avatar/complaint/contract）',
  `biz_id` bigint DEFAULT NULL COMMENT '业务ID',
  `upload_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `upload_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `del_flag` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_biz_id` (`biz_id`),
  KEY `idx_biz_type` (`biz_type`,`biz_id`),
  KEY `idx_storage_type` (`storage_type`),
  KEY `idx_upload_by` (`upload_by`),
  KEY `idx_upload_time` (`upload_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件上传记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_sys_file`
--

LOCK TABLES `t_sys_file` WRITE;
/*!40000 ALTER TABLE `t_sys_file` DISABLE KEYS */;
/*!40000 ALTER TABLE `t_sys_file` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_sys_login_log`
--

DROP TABLE IF EXISTS `t_sys_login_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_sys_login_log` (
  `id` bigint NOT NULL COMMENT '日志ID',
  `user_type` tinyint NOT NULL COMMENT '用户类型：1-系统用户 2-业主',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `username` varchar(50) NOT NULL COMMENT '登录账号',
  `login_type` tinyint NOT NULL DEFAULT '1' COMMENT '登录方式：1-密码 2-验证码 3-微信 4-支付宝',
  `login_status` tinyint NOT NULL COMMENT '登录状态：0-失败 1-成功',
  `fail_reason` varchar(200) DEFAULT NULL COMMENT '失败原因',
  `ip_address` varchar(50) DEFAULT NULL COMMENT '登录IP',
  `login_location` varchar(200) DEFAULT NULL COMMENT '登录地点',
  `user_agent` varchar(500) DEFAULT NULL COMMENT '浏览器UA',
  `device_type` varchar(50) DEFAULT NULL COMMENT '设备类型（iOS/Android/PC）',
  `login_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_type` (`user_type`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_username` (`username`),
  KEY `idx_login_status` (`login_status`),
  KEY `idx_login_time` (`login_time`),
  KEY `idx_ip_address` (`ip_address`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='登录日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_sys_login_log`
--

LOCK TABLES `t_sys_login_log` WRITE;
/*!40000 ALTER TABLE `t_sys_login_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `t_sys_login_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_sys_operation_log`
--

DROP TABLE IF EXISTS `t_sys_operation_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_sys_operation_log` (
  `id` bigint NOT NULL COMMENT '日志ID',
  `trace_id` varchar(50) DEFAULT NULL COMMENT '链路追踪ID',
  `user_id` bigint DEFAULT NULL COMMENT '操作人ID',
  `user_name` varchar(50) DEFAULT NULL COMMENT '操作人用户名',
  `real_name` varchar(100) DEFAULT NULL COMMENT '操作人真实姓名',
  `module` varchar(100) NOT NULL COMMENT '操作模块（如 业主管理/账单管理）',
  `action` varchar(100) NOT NULL COMMENT '操作动作（如 新增业主/删除账单）',
  `request_method` varchar(10) DEFAULT NULL COMMENT '请求方法（GET/POST/PUT/DELETE）',
  `request_url` varchar(500) DEFAULT NULL COMMENT '请求URL',
  `request_params` text COMMENT '请求参数',
  `response_data` text COMMENT '响应数据（截取前2000字符）',
  `ip_address` varchar(50) DEFAULT NULL COMMENT '请求IP',
  `user_agent` varchar(500) DEFAULT NULL COMMENT '浏览器UA',
  `cost_time` bigint DEFAULT '0' COMMENT '耗时（毫秒）',
  `result_code` int DEFAULT '0' COMMENT '结果码（200成功，非200失败）',
  `result_msg` varchar(500) DEFAULT NULL COMMENT '结果消息',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-失败 1-成功',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_trace_id` (`trace_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_module` (`module`),
  KEY `idx_action` (`action`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_status` (`status`),
  KEY `idx_cost_time` (`cost_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_sys_operation_log`
--

LOCK TABLES `t_sys_operation_log` WRITE;
/*!40000 ALTER TABLE `t_sys_operation_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `t_sys_operation_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_sys_permission`
--

DROP TABLE IF EXISTS `t_sys_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_sys_permission` (
  `id` bigint NOT NULL COMMENT '权限ID',
  `perm_code` varchar(100) NOT NULL COMMENT '权限标识（如 system:owner:list）',
  `perm_name` varchar(100) NOT NULL COMMENT '权限名称',
  `perm_type` tinyint NOT NULL COMMENT '类型：1-目录 2-菜单 3-按钮 4-API',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父级ID',
  `icon` varchar(100) DEFAULT NULL COMMENT '图标',
  `path` varchar(200) DEFAULT NULL COMMENT '路由路径（菜单类型时）',
  `component` varchar(200) DEFAULT NULL COMMENT '组件路径',
  `api_url` varchar(200) DEFAULT NULL COMMENT 'API路径（API类型时）',
  `http_method` varchar(10) DEFAULT NULL COMMENT 'HTTP方法（GET/POST/PUT/DELETE）',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `visible` tinyint NOT NULL DEFAULT '1' COMMENT '是否显示：0-隐藏 1-显示',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-停用 1-启用',
  `remark` varchar(500) DEFAULT NULL,
  `del_flag` tinyint NOT NULL DEFAULT '0',
  `create_by` varchar(50) NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(50) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_perm_code` (`perm_code`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_perm_type` (`perm_type`),
  KEY `idx_status` (`status`),
  KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='权限/菜单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_sys_permission`
--

LOCK TABLES `t_sys_permission` WRITE;
/*!40000 ALTER TABLE `t_sys_permission` DISABLE KEYS */;
/*!40000 ALTER TABLE `t_sys_permission` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_sys_role`
--

DROP TABLE IF EXISTS `t_sys_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_sys_role` (
  `id` bigint NOT NULL COMMENT '角色ID',
  `role_code` varchar(50) NOT NULL COMMENT '角色编码（如 ADMIN, FINANCE, CUSTOMER_SERVICE）',
  `role_name` varchar(100) NOT NULL COMMENT '角色名称（如 管理员/财务/客服）',
  `role_type` tinyint NOT NULL DEFAULT '1' COMMENT '角色类型：1-系统内置 2-自定义',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-停用 1-启用',
  `remark` varchar(500) DEFAULT NULL,
  `del_flag` tinyint NOT NULL DEFAULT '0',
  `create_by` varchar(50) NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(50) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_sys_role`
--

LOCK TABLES `t_sys_role` WRITE;
/*!40000 ALTER TABLE `t_sys_role` DISABLE KEYS */;
INSERT INTO `t_sys_role` VALUES (1,'SUPER_ADMIN','超级管理员',1,1,1,'系统超级管理员，拥有全部权限',0,'system','2026-06-17 14:56:41',NULL,NULL),(2,'ADMIN','管理员',1,2,1,'物业管理员，可管理大部分业务',0,'system','2026-06-17 14:56:41',NULL,NULL),(3,'FINANCE','财务',1,3,1,'财务管理，账单和支付相关权限',0,'system','2026-06-17 14:56:41',NULL,NULL),(4,'CUSTOMER_SERVICE','客服',1,4,1,'客服人员，处理投诉建议',0,'system','2026-06-17 14:56:41',NULL,NULL),(5,'MAINTENANCE','维修工',1,5,1,'维修人员，处理维修工单',0,'system','2026-06-17 14:56:41',NULL,NULL);
/*!40000 ALTER TABLE `t_sys_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_sys_role_permission`
--

DROP TABLE IF EXISTS `t_sys_role_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_sys_role_permission` (
  `id` bigint NOT NULL COMMENT '关联ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `permission_id` bigint NOT NULL COMMENT '权限ID',
  `create_by` varchar(50) NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_perm` (`role_id`,`permission_id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色权限关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_sys_role_permission`
--

LOCK TABLES `t_sys_role_permission` WRITE;
/*!40000 ALTER TABLE `t_sys_role_permission` DISABLE KEYS */;
/*!40000 ALTER TABLE `t_sys_role_permission` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_sys_user`
--

DROP TABLE IF EXISTS `t_sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_sys_user` (
  `id` bigint NOT NULL COMMENT '用户ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(200) NOT NULL COMMENT '密码（BCrypt加密）',
  `real_name` varchar(100) NOT NULL COMMENT '真实姓名',
  `phone` varchar(20) NOT NULL COMMENT '手机号',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `avatar` varchar(500) DEFAULT NULL COMMENT '头像',
  `gender` tinyint DEFAULT '0' COMMENT '性别：0-未知 1-男 2-女',
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
  `post` varchar(50) DEFAULT NULL COMMENT '岗位',
  `user_type` tinyint NOT NULL DEFAULT '1' COMMENT '用户类型：1-超级管理员 2-物业管理员 3-财务 4-客服 5-维修工',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-禁用 1-正常',
  `last_login_ip` varchar(50) DEFAULT NULL COMMENT '最后登录IP',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `pwd_error_count` int NOT NULL DEFAULT '0' COMMENT '密码错误次数',
  `pwd_update_time` datetime DEFAULT NULL COMMENT '密码最后修改时间',
  `remark` varchar(500) DEFAULT NULL,
  `del_flag` tinyint NOT NULL DEFAULT '0',
  `create_by` varchar(50) NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(50) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_phone` (`phone`),
  KEY `idx_status` (`status`),
  KEY `idx_user_type` (`user_type`),
  KEY `idx_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_sys_user`
--

LOCK TABLES `t_sys_user` WRITE;
/*!40000 ALTER TABLE `t_sys_user` DISABLE KEYS */;
INSERT INTO `t_sys_user` VALUES (1,'admin','$2a$10$QmuE3zA.eiX1g11fh4KRwun2dGimIt7DfRk1u5E75/dEn3MurOoDi','系统管理员','13800000000','admin@property.com',NULL,0,NULL,NULL,1,1,NULL,NULL,0,NULL,NULL,0,'system','2026-06-17 17:26:57',NULL,NULL);
/*!40000 ALTER TABLE `t_sys_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_sys_user_role`
--

DROP TABLE IF EXISTS `t_sys_user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_sys_user_role` (
  `id` bigint NOT NULL COMMENT '关联ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `create_by` varchar(50) NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`,`role_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户角色关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_sys_user_role`
--

LOCK TABLES `t_sys_user_role` WRITE;
/*!40000 ALTER TABLE `t_sys_user_role` DISABLE KEYS */;
INSERT INTO `t_sys_user_role` VALUES (1,1,1,'system','2026-06-17 14:56:41');
/*!40000 ALTER TABLE `t_sys_user_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_unit`
--

DROP TABLE IF EXISTS `t_unit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_unit` (
  `id` bigint NOT NULL COMMENT '单元ID',
  `building_id` bigint NOT NULL COMMENT '所属楼栋ID',
  `unit_code` varchar(20) NOT NULL COMMENT '单元编号（如 1单元, 2单元）',
  `unit_name` varchar(100) NOT NULL COMMENT '单元名称',
  `total_floors` int NOT NULL DEFAULT '0' COMMENT '总层数',
  `total_rooms` int NOT NULL DEFAULT '0' COMMENT '总户数',
  `elevator_count` int NOT NULL DEFAULT '0' COMMENT '电梯数量',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-停用 1-启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `del_flag` tinyint NOT NULL DEFAULT '0',
  `create_by` varchar(50) NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(50) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_building_unit` (`building_id`,`unit_code`),
  KEY `idx_building_id` (`building_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='单元表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_unit`
--

LOCK TABLES `t_unit` WRITE;
/*!40000 ALTER TABLE `t_unit` DISABLE KEYS */;
INSERT INTO `t_unit` VALUES (2001,1001,'1','1单元',18,36,2,1,1,'两梯四户',0,'admin','2026-06-18 16:14:31',NULL,NULL),(2002,1001,'2','2单元',18,36,2,2,1,'两梯四户',0,'admin','2026-06-18 16:14:31',NULL,NULL),(2003,1002,'1','1单元',18,36,2,1,1,'两梯四户',0,'admin','2026-06-18 16:14:31',NULL,NULL),(2004,1002,'2','2单元',18,36,2,2,1,'两梯四户',0,'admin','2026-06-18 16:14:31',NULL,NULL),(2005,1003,'1','1单元',24,48,2,1,1,'两梯两户',0,'admin','2026-06-18 16:14:31',NULL,NULL),(2006,1004,'1','1单元',30,60,2,1,1,'两梯四户',0,'admin','2026-06-18 16:14:31',NULL,NULL),(2007,1004,'2','2单元',30,60,2,2,1,'两梯四户',0,'admin','2026-06-18 16:14:31',NULL,NULL),(2008,1005,'1','1单元',30,60,2,1,1,'两梯四户',0,'admin','2026-06-18 16:14:31',NULL,NULL),(2009,1005,'2','2单元',30,60,2,2,1,'两梯四户',0,'admin','2026-06-18 16:14:31',NULL,NULL),(2010,1006,'1','1单元',30,60,3,1,1,'三梯六户',0,'admin','2026-06-18 16:14:31',NULL,NULL),(2011,1006,'2','2单元',30,60,3,2,1,'三梯六户',0,'admin','2026-06-18 16:14:31',NULL,NULL),(2012,1006,'3','3单元',30,60,3,3,1,'三梯六户',0,'admin','2026-06-18 16:14:31',NULL,NULL),(2013,2067412717767823362,'1','1单元',18,36,2,1,1,'两梯四户',0,'admin','2026-06-18 16:14:31',NULL,NULL),(2014,2067412717767823362,'2','2单元',18,36,2,2,1,'两梯四户',0,'admin','2026-06-18 16:14:31',NULL,NULL),(2068219518234996738,1008,'6','6',0,0,0,0,0,'',1,'system','2026-06-20 14:28:53',NULL,'2026-06-20 14:32:59');
/*!40000 ALTER TABLE `t_unit` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_chat_session`
--

DROP TABLE IF EXISTS `t_chat_session`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_chat_session` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID（业主ID）',
  `title` varchar(100) DEFAULT '新对话' COMMENT '会话标题',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_update_time` (`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天会话';

--
-- Table structure for table `t_chat_history`
--

DROP TABLE IF EXISTS `t_chat_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_chat_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID（业主ID）',
  `session_id` bigint DEFAULT NULL COMMENT '会话ID',
  `role` varchar(20) NOT NULL COMMENT '角色：user=用户消息，assistant=AI回复',
  `content` text NOT NULL COMMENT '消息内容',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天历史记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping routines for database 'property_management'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-08-13 11:19:49
DROP TABLE IF EXISTS `t_notice`;
CREATE TABLE `t_notice` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` varchar(200) NOT NULL COMMENT '公告标题',
  `content` text NOT NULL COMMENT '公告内容',
  `type` varchar(20) NOT NULL DEFAULT 'NOTICE' COMMENT '公告类型：NOTICE=普通公告，WATER_ELECTRIC=停水停电，ACTIVITY=社区活动，EMERGENCY=紧急通知',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0=草稿，1=已发布，2=已下线',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID（管理员）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_type_status` (`type`, `status`),
  KEY `idx_publish_time` (`publish_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小区公告';


INSERT INTO `t_notice` (`title`, `content`, `type`, `status`, `publish_time`, `create_by`)
VALUES ('欢迎使用智慧社区服务平台', '尊敬的业主：\n欢迎使用智慧社区服务平台！如您在使用过程中有任何问题，可通过AI客服或拨打物业服务中心电话咨询。', 'NOTICE', 1, NOW(), NULL);