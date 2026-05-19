package com.school.dormrepair.controller;

import com.school.dormrepair.common.Result;
import com.school.dormrepair.entity.WorkOrder;
import com.school.dormrepair.mapper.WorkOrderMapper;
import com.alibaba.excel.EasyExcel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 鉴权由 JwtInterceptor 统一处理
@RestController
@RequestMapping("/stats")
public class StatsController {

    @Autowired
    private WorkOrderMapper workOrderMapper;

    // 统计：总数、待处理、处理中、已完成
    @GetMapping("/dashboard")
    public Result<Map<String, Long>> dashboard() {
        List<WorkOrder> all = workOrderMapper.selectList(null);
        long total = all.size();
        long pending = all.stream().filter(o -> "pending".equals(o.getStatus())).count();
        long processing = all.stream().filter(o -> "processing".equals(o.getStatus())).count();
        long completed = all.stream().filter(o -> "completed".equals(o.getStatus())).count();

        Map<String, Long> map = new HashMap<>();
        map.put("total", total);
        map.put("pending", pending);
        map.put("processing", processing);
        map.put("completed", completed);

        return Result.success(map);
    }

    // Excel 导出所有工单
    @GetMapping("/export")
    public void export(HttpServletResponse response) throws Exception {
        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-disposition", "attachment;filename=work_order.xlsx");

        List<WorkOrder> list = workOrderMapper.selectList(null);
        EasyExcel.write(response.getOutputStream(), WorkOrder.class).sheet("工单列表").doWrite(list);
    }
}
