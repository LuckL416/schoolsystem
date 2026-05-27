package com.school.dormrepair.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.dormrepair.common.BusinessException;
import com.school.dormrepair.entity.InventoryItem;
import com.school.dormrepair.entity.InventoryRecord;
import com.school.dormrepair.mapper.InventoryItemMapper;
import com.school.dormrepair.mapper.InventoryRecordMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class InventoryService {

    private final InventoryItemMapper inventoryItemMapper;
    private final InventoryRecordMapper inventoryRecordMapper;
    private final NotificationService notificationService;

    public InventoryService(InventoryItemMapper inventoryItemMapper,
                            InventoryRecordMapper inventoryRecordMapper,
                            NotificationService notificationService) {
        this.inventoryItemMapper = inventoryItemMapper;
        this.inventoryRecordMapper = inventoryRecordMapper;
        this.notificationService = notificationService;
    }

    /**
     * 分页查询耗材列表
     */
    public Page<InventoryItem> listItems(int page, int size, String category) {
        LambdaQueryWrapper<InventoryItem> qw = new LambdaQueryWrapper<>();
        if (category != null && !category.isEmpty()) {
            qw.eq(InventoryItem::getCategory, category);
        }
        qw.orderByAsc(InventoryItem::getCategory).orderByAsc(InventoryItem::getName);
        return inventoryItemMapper.selectPage(new Page<>(page, size), qw);
    }

    /**
     * 新增耗材
     */
    public void createItem(InventoryItem item) {
        item.setUpdateTime(LocalDateTime.now());
        inventoryItemMapper.insert(item);
    }

    /**
     * 更新耗材
     */
    public void updateItem(InventoryItem item) {
        item.setUpdateTime(LocalDateTime.now());
        inventoryItemMapper.updateById(item);
    }

    /**
     * 入库：增加库存 + 写记录
     */
    public void stockIn(Long itemId, int quantity, Long operatorId, String remark) {
        InventoryItem item = inventoryItemMapper.selectById(itemId);
        if (item == null) throw new BusinessException("耗材不存在");
        item.setQuantity(item.getQuantity() + quantity);
        item.setUpdateTime(LocalDateTime.now());
        inventoryItemMapper.updateById(item);

        InventoryRecord record = new InventoryRecord();
        record.setItemId(itemId);
        record.setType("in");
        record.setQuantity(quantity);
        record.setOperatorId(operatorId);
        record.setRemark(remark);
        record.setCreateTime(LocalDateTime.now());
        inventoryRecordMapper.insert(record);
    }

    /**
     * 出库：减少库存 + 写记录 + 低库存预警
     */
    public void stockOut(Long itemId, int quantity, Long operatorId, Long workOrderId, String remark) {
        InventoryItem item = inventoryItemMapper.selectById(itemId);
        if (item == null) throw new BusinessException("耗材不存在");
        if (item.getQuantity() < quantity) throw new BusinessException("库存不足");
        item.setQuantity(item.getQuantity() - quantity);
        item.setUpdateTime(LocalDateTime.now());
        inventoryItemMapper.updateById(item);

        InventoryRecord record = new InventoryRecord();
        record.setItemId(itemId);
        record.setType("out");
        record.setQuantity(quantity);
        record.setOperatorId(operatorId);
        record.setWorkOrderId(workOrderId);
        record.setRemark(remark);
        record.setCreateTime(LocalDateTime.now());
        inventoryRecordMapper.insert(record);

        // 低库存预警
        if (item.getQuantity() < item.getSafetyThreshold()) {
            notificationService.send(1L, "low_stock",
                    "库存预警",
                    "耗材 \"" + item.getName() + "\" 库存不足（当前 " + item.getQuantity() + "，安全阈值 " + item.getSafetyThreshold() + "）",
                    itemId);
        }
    }

    /**
     * 查询某个耗材的出入库记录（分页）
     */
    public Page<InventoryRecord> records(Long itemId, int page, int size) {
        LambdaQueryWrapper<InventoryRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(InventoryRecord::getItemId, itemId)
          .orderByDesc(InventoryRecord::getCreateTime);
        return inventoryRecordMapper.selectPage(new Page<>(page, size), qw);
    }

    /**
     * 查询低库存耗材（当前数量 < 安全阈值）
     */
    public List<InventoryItem> lowStock() {
        LambdaQueryWrapper<InventoryItem> qw = new LambdaQueryWrapper<>();
        qw.apply("quantity < safety_threshold");
        return inventoryItemMapper.selectList(qw);
    }
}
