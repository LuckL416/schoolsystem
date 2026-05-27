package com.school.dormrepair.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.dormrepair.common.Result;
import com.school.dormrepair.entity.InventoryItem;
import com.school.dormrepair.entity.InventoryRecord;
import com.school.dormrepair.service.InventoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    /**
     * 分页查询耗材列表
     */
    @GetMapping("/list")
    public Result<Page<InventoryItem>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category
    ) {
        return Result.success(inventoryService.listItems(page, size, category));
    }

    /**
     * 新增耗材
     */
    @PostMapping
    public Result<Void> create(@RequestBody InventoryItem item) {
        inventoryService.createItem(item);
        return Result.success();
    }

    /**
     * 更新耗材
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody InventoryItem item) {
        item.setId(id);
        inventoryService.updateItem(item);
        return Result.success();
    }

    /**
     * 入库
     */
    @PostMapping("/{id}/in")
    public Result<Void> stockIn(
            @PathVariable Long id,
            @RequestParam int quantity,
            @RequestParam(required = false) String remark,
            @RequestAttribute("userId") Long userId
    ) {
        inventoryService.stockIn(id, quantity, userId, remark);
        return Result.success();
    }

    /**
     * 出库
     */
    @PostMapping("/{id}/out")
    public Result<Void> stockOut(
            @PathVariable Long id,
            @RequestParam int quantity,
            @RequestParam(required = false) Long workOrderId,
            @RequestParam(required = false) String remark,
            @RequestAttribute("userId") Long userId
    ) {
        inventoryService.stockOut(id, quantity, userId, workOrderId, remark);
        return Result.success();
    }

    /**
     * 查询某个耗材的出入库记录（分页）
     */
    @GetMapping("/{id}/records")
    public Result<Page<InventoryRecord>> records(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return Result.success(inventoryService.records(id, page, size));
    }

    /**
     * 查询低库存耗材列表
     */
    @GetMapping("/low-stock")
    public Result<List<InventoryItem>> lowStock() {
        return Result.success(inventoryService.lowStock());
    }
}
