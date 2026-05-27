package com.school.dormrepair.controller;

import com.school.dormrepair.common.Result;
import com.school.dormrepair.entity.Dorm;
import com.school.dormrepair.entity.FaultType;
import com.school.dormrepair.entity.User;
import com.school.dormrepair.entity.WorkOrder;
import com.school.dormrepair.entity.WorkOrderExcelVO;
import com.school.dormrepair.mapper.DormMapper;
import com.school.dormrepair.mapper.FaultTypeMapper;
import com.school.dormrepair.mapper.UserMapper;
import com.school.dormrepair.mapper.WorkOrderMapper;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.Map;
import java.util.stream.Collectors;

// 鉴权由 JwtInterceptor 统一处理
@RestController
@RequestMapping("/stats")
public class StatsController {

    @Autowired
    private WorkOrderMapper workOrderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private FaultTypeMapper faultTypeMapper;

    @Autowired
    private DormMapper dormMapper;

    // 统计：总数、待处理、处理中、已完成、完成率、平均评分
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        List<WorkOrder> all = workOrderMapper.selectList(null);
        long total = all.size();
        long pending = all.stream().filter(o -> "pending".equals(o.getStatus())).count();
        long processing = all.stream().filter(o -> "processing".equals(o.getStatus())).count();
        long completed = all.stream().filter(o -> "completed".equals(o.getStatus())).count();

        Map<String, Object> map = new HashMap<>();
        map.put("total", total);
        map.put("pending", pending);
        map.put("processing", processing);
        map.put("completed", completed);

        // 完成率：(completed + accepted) / total
        long acceptedCount = all.stream().filter(o -> "accepted".equals(o.getStatus())).count();
        long completedTotal = completed + acceptedCount;
        String completionRate = total > 0
            ? String.format("%.1f", completedTotal * 100.0 / total) : "0.0";
        map.put("completionRate", completionRate);

        // 平均评分：三维度取平均
        LambdaQueryWrapper<WorkOrder> ratedQw = new LambdaQueryWrapper<>();
        ratedQw.isNotNull(WorkOrder::getEvaluateAttitude);
        List<WorkOrder> rated = workOrderMapper.selectList(ratedQw);
        String avgRating = "0.0";
        if (!rated.isEmpty()) {
            double avg = rated.stream()
                .mapToDouble(o -> (o.getEvaluateAttitude() + o.getEvaluateSpeed() + o.getEvaluateQuality()) / 3.0)
                .average().orElse(0);
            avgRating = String.format("%.1f", avg);
        }
        map.put("avgRating", avgRating);

        return Result.success(map);
    }

    // 近N天工单趋势（返回列表格式，供 index.html 和 bigscreen.html 共用）
    @GetMapping("/trend")
    public Result<List<Map<String, Object>>> trend(@RequestParam(defaultValue = "7") Integer days) {
        List<WorkOrder> all = workOrderMapper.selectList(null);
        Map<String, Long> dayCount = new LinkedHashMap<>();
        java.time.LocalDate today = java.time.LocalDate.now();
        for (int i = days - 1; i >= 0; i--) {
            dayCount.put(today.minusDays(i).toString(), 0L);
        }
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (WorkOrder o : all) {
            if (o.getSubmitTime() != null) {
                String key = o.getSubmitTime().format(fmt);
                if (dayCount.containsKey(key)) {
                    dayCount.put(key, dayCount.get(key) + 1);
                }
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Long> e : dayCount.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("date", e.getKey());
            item.put("count", e.getValue());
            result.add(item);
        }
        return Result.success(result);
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

    // 按故障类型统计（使用故障类型名称）
    @GetMapping("/by-type")
    public Result<Map<String, Long>> byType() {
        List<WorkOrder> all = workOrderMapper.selectList(null);
        Map<String, Long> map = new LinkedHashMap<>();
        // Build name lookup
        Map<Long, String> nameMap = new HashMap<>();
        List<FaultType> types = faultTypeMapper.selectList(null);
        for (FaultType ft : types) {
            nameMap.put(ft.getId(), ft.getName());
        }
        for (WorkOrder o : all) {
            String name = nameMap.getOrDefault(o.getFaultTypeId(), "未知类型");
            map.put(name, map.getOrDefault(name, 0L) + 1);
        }
        return Result.success(map);
    }

    // 楼栋房间热力图数据
    @GetMapping("/heatmap")
    public Result<List<Map<String, Object>>> heatmap() {
        List<Dorm> dorms = dormMapper.selectList(null);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Dorm d : dorms) {
            LambdaQueryWrapper<WorkOrder> qw = new LambdaQueryWrapper<>();
            qw.eq(WorkOrder::getDormId, d.getId());
            Map<String, Object> item = new HashMap<>();
            item.put("building", d.getBuilding());
            item.put("room", d.getRoom());
            item.put("label", d.getBuilding() + "-" + d.getRoom());
            item.put("count", workOrderMapper.selectCount(qw));
            result.add(item);
        }
        return Result.success(result);
    }

    // 维修师傅评分排行
    @GetMapping("/teacher-ranking")
    public Result<List<Map<String, Object>>> teacherRanking() {
        LambdaQueryWrapper<WorkOrder> qw = new LambdaQueryWrapper<>();
        qw.isNotNull(WorkOrder::getEvaluateAttitude)
          .isNotNull(WorkOrder::getTeacherId);
        List<WorkOrder> orders = workOrderMapper.selectList(qw);

        // Group by teacherId
        Map<Long, List<WorkOrder>> group = orders.stream()
            .collect(Collectors.groupingBy(WorkOrder::getTeacherId));

        List<Map<String, Object>> ranking = new ArrayList<>();
        for (Map.Entry<Long, List<WorkOrder>> entry : group.entrySet()) {
            double avg = entry.getValue().stream()
                .mapToDouble(o -> (o.getEvaluateAttitude() + o.getEvaluateSpeed() + o.getEvaluateQuality()) / 3.0)
                .average().orElse(0);
            User t = userMapper.selectById(entry.getKey());
            Map<String, Object> item = new HashMap<>();
            item.put("teacherId", entry.getKey());
            item.put("teacherName", t != null ? t.getName() : "未知");
            item.put("avgRating", String.format("%.1f", avg));
            item.put("orderCount", entry.getValue().size());
            ranking.add(item);
        }
        ranking.sort((a, b) -> Double.compare(
            Double.parseDouble((String) b.get("avgRating")),
            Double.parseDouble((String) a.get("avgRating"))));
        return Result.success(ranking);
    }

    // 最近10条工单动态
    @GetMapping("/recent-orders")
    public Result<List<WorkOrder>> recentOrders() {
        LambdaQueryWrapper<WorkOrder> qw = new LambdaQueryWrapper<>();
        qw.orderByDesc(WorkOrder::getSubmitTime).last("LIMIT 10");
        return Result.success(workOrderMapper.selectList(qw));
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
        // Preload lookup maps
        List<Dorm> dorms = dormMapper.selectList(null);
        Map<Long, Dorm> dormMap = dorms.stream().collect(Collectors.toMap(Dorm::getId, d -> d));
        List<FaultType> ftypes = faultTypeMapper.selectList(null);
        Map<Long, FaultType> ftMap = ftypes.stream().collect(Collectors.toMap(FaultType::getId, f -> f));
        List<User> users = userMapper.selectList(null);
        Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));

        List<WorkOrderExcelVO> voList = list.stream()
            .map(o -> WorkOrderExcelVO.from(o,
                dormMap.get(o.getDormId()),
                ftMap.get(o.getFaultTypeId()),
                userMap.get(o.getStudentId()),
                userMap.get(o.getTeacherId())))
            .collect(Collectors.toList());
        EasyExcel.write(response.getOutputStream(), WorkOrderExcelVO.class).sheet("工单列表").doWrite(voList);
    }
}
