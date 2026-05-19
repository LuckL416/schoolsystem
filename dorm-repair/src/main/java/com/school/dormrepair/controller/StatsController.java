package com.school.dormrepair.controller;

import com.school.dormrepair.common.Result;
import com.school.dormrepair.entity.WorkOrder;
import com.school.dormrepair.entity.WorkOrderExcelVO;
import com.school.dormrepair.mapper.WorkOrderMapper;
import com.alibaba.excel.EasyExcel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    // 近N天工单趋势
    @GetMapping("/trend")
    public Result<Map<String, Long>> trend(@RequestParam(defaultValue = "7") Integer days) {
        List<WorkOrder> all = workOrderMapper.selectList(null);
        Map<String, Long> map = new LinkedHashMap<>();
        java.time.LocalDate today = java.time.LocalDate.now();
        for (int i = days - 1; i >= 0; i--) {
            String key = today.minusDays(i).toString();
            map.put(key, 0L);
        }
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (WorkOrder o : all) {
            if (o.getSubmitTime() != null) {
                String key = o.getSubmitTime().format(fmt);
                if (map.containsKey(key)) {
                    map.put(key, map.get(key) + 1);
                }
            }
        }
        return Result.success(map);
    }

    // 按宿舍统计
    @GetMapping("/by-dorm")
    public Result<Map<String, Long>> byDorm() {
        List<WorkOrder> all = workOrderMapper.selectList(null);
        Map<String, Long> map = new HashMap<>();
        for (WorkOrder o : all) {
            String key = "宿舍" + o.getDormId();
            map.put(key, map.getOrDefault(key, 0L) + 1);
        }
        return Result.success(map);
    }

    // 按故障类型统计
    @GetMapping("/by-type")
    public Result<Map<String, Long>> byType() {
        List<WorkOrder> all = workOrderMapper.selectList(null);
        Map<String, Long> map = new HashMap<>();
        for (WorkOrder o : all) {
            String key = "类型" + o.getFaultTypeId();
            map.put(key, map.getOrDefault(key, 0L) + 1);
        }
        return Result.success(map);
    }

    // Excel 导出所有工单（中文表头）
    @GetMapping("/export")
    public void export(HttpServletResponse response,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) String startDate,
                       @RequestParam(required = false) String endDate) throws Exception {
        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding("utf-8");
        String filename = "工单列表_" + java.time.LocalDate.now().toString() + ".xlsx";
        response.setHeader("Content-disposition", "attachment;filename=" +
                java.net.URLEncoder.encode(filename, "UTF-8"));

        List<WorkOrder> list = workOrderMapper.selectList(null);
        // 筛选
        if (status != null && !status.isEmpty()) {
            list.removeIf(o -> !status.equals(o.getStatus()));
        }
        if (startDate != null && !startDate.isEmpty()) {
            list.removeIf(o -> o.getSubmitTime() == null ||
                    o.getSubmitTime().isBefore(java.time.LocalDateTime.parse(startDate + "T00:00:00")));
        }
        if (endDate != null && !endDate.isEmpty()) {
            list.removeIf(o -> o.getSubmitTime() == null ||
                    o.getSubmitTime().isAfter(java.time.LocalDateTime.parse(endDate + "T23:59:59")));
        }
        List<WorkOrderExcelVO> voList = list.stream().map(WorkOrderExcelVO::from).collect(Collectors.toList());
        EasyExcel.write(response.getOutputStream(), WorkOrderExcelVO.class).sheet("工单列表").doWrite(voList);
    }
}
