package com.smartparking.ui.backend;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 离线演示数据源：不依赖任何业务类，保证 GUI 在队友完成前即可独立运行、演示、答辩。
 *
 * <p>实现的行为与 SRS 一致：黑名单拦截、满位排队、离场计费、队首自动补位、
 * 道闸状态、操作日志。切换到真实业务层只需在登录页选择“真实业务层”。</p>
 */
public final class DemoBackend implements ParkingBackend {

    private static final int CAPACITY = 20;
    private static final DateTimeFormatter PLATE_TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final List<DemoCar> parked = new ArrayList<>();   // index 0 = 栈顶
    private final List<DemoCar> queue = new ArrayList<>();
    private final Map<String, LocalDateTime> blacklist = new LinkedHashMap<>();
    private final List<LogEntry> logs = new ArrayList<>();
    private final Map<String, LocalDateTime> appointments = new LinkedHashMap<>();
    private final List<VisitorRegistration> registrations = new ArrayList<>();
    private final Map<String, Account> accounts = new LinkedHashMap<>();

    private int unitCost = 5;
    private boolean gateOpen = false;
    private String lastPlate;
    private LocalDateTime gateChangedAt = LocalDateTime.now();
    private UserSession session;

    public DemoBackend() {
        seedAccounts();
        seedState();
    }

    @Override
    public String modeLabel() {
        return "演示数据";
    }

    @Override
    public boolean isDemo() {
        return true;
    }

    @Override
    public synchronized UserSession login(Role role, String account, String password) {
        Account candidate = accounts.get(key(role, account));
        if (candidate == null || !candidate.password().equals(password)) {
            return null;
        }
        session = new UserSession(role, candidate.account(), candidate.displayName());
        log("登录系统", role.label() + " " + candidate.displayName() + " 登录成功", true);
        return session;
    }

    @Override
    public synchronized Snapshot snapshot() {
        LocalDateTime now = LocalDateTime.now();
        List<ParkedCar> cars = new ArrayList<>();
        for (int i = 0; i < parked.size(); i++) {
            DemoCar car = parked.get(i);
            cars.add(new ParkedCar(car.plate(), car.entryTime(),
                    minutesBetween(car.entryTime(), now), i + 1));
        }
        List<QueueCar> waiting = new ArrayList<>();
        for (int i = 0; i < queue.size(); i++) {
            DemoCar car = queue.get(i);
            waiting.add(new QueueCar(car.plate(), i + 1, car.entryTime(),
                    minutesBetween(car.entryTime(), now)));
        }
        return new Snapshot(CAPACITY, List.copyOf(cars), List.copyOf(waiting),
                new Gate(gateOpen, lastPlate, gateChangedAt), unitCost);
    }

    @Override
    public synchronized Outcome enter(String plate) {
        String normalized = normalize(plate);
        if (normalized == null) {
            return Outcome.fail("FAIL", "车牌格式不正确", plate);
        }
        if (blacklist.containsKey(normalized)) {
            log("入场识别", normalized + " 命中黑名单，拒绝入场", false);
            return Outcome.fail("REJECT", "该车牌在黑名单中，禁止入场", normalized);
        }
        if (findParked(normalized) >= 0) {
            return Outcome.fail("FAIL", "该车辆已在场内", normalized);
        }
        if (findQueued(normalized) >= 0) {
            return Outcome.fail("FAIL", "该车辆已在等候队列中", normalized);
        }
        if (parked.size() >= CAPACITY) {
            queue.add(new DemoCar(normalized, LocalDateTime.now()));
            openGate(normalized);
            log("入场识别", normalized + " 车位已满，加入等候队列（第 " + queue.size() + " 位）", true);
            return Outcome.ok("WAIT", "车位已满，车辆已加入等候队列，当前第 " + queue.size() + " 位",
                    normalized, null);
        }
        parked.add(0, new DemoCar(normalized, LocalDateTime.now()));
        openGate(normalized);
        log("入场识别", normalized + " 识别成功，道闸抬起，车辆入场", true);
        return Outcome.ok("PASS", "识别成功，道闸已抬起，车辆入场", normalized, null);
    }

    @Override
    public synchronized Outcome leave(String plate) {
        String normalized = normalize(plate);
        if (normalized == null) {
            return Outcome.fail("FAIL", "车牌格式不正确", plate);
        }
        int index = findParked(normalized);
        if (index < 0) {
            return Outcome.fail("FAIL", "该车辆不在场内", normalized);
        }
        DemoCar car = parked.get(index);
        long hours = billableHours(car.entryTime());
        int fee = (int) hours * unitCost;
        parked.remove(index);
        openGate(normalized);
        log("离场缴费", normalized + " 停车 " + hours + " 小时，缴费 " + fee + " 元，道闸抬起", true);

        if (!queue.isEmpty()) {
            DemoCar next = queue.remove(0);
            parked.add(0, new DemoCar(next.plate(), LocalDateTime.now()));
            log("队列补位", next.plate() + " 自动补位入场", true);
        }
        return Outcome.ok("LEFT", "缴费成功，车辆离场，费用 " + fee + " 元", normalized, fee);
    }

    @Override
    public synchronized Outcome setGate(boolean open) {
        gateOpen = open;
        gateChangedAt = LocalDateTime.now();
        log("道闸控制", open ? "手动抬杆" : "手动落杆", true);
        return Outcome.ok("OK", open ? "道闸已抬起" : "道闸已落下", null, null);
    }

    @Override
    public synchronized Quote quote(String plate) {
        String normalized = normalize(plate);
        if (normalized == null) {
            return null;
        }
        int index = findParked(normalized);
        if (index < 0) {
            return null;
        }
        DemoCar car = parked.get(index);
        long hours = billableHours(car.entryTime());
        return new Quote(normalized, hours, unitCost, (int) hours * unitCost,
                LocalDateTime.now());
    }

    @Override
    public synchronized Outcome addToBlacklist(String plate) {
        String normalized = normalize(plate);
        if (normalized == null) {
            return Outcome.fail("FAIL", "车牌格式不正确", plate);
        }
        if (blacklist.containsKey(normalized)) {
            return Outcome.fail("FAIL", "该车牌已在黑名单中", normalized);
        }
        blacklist.put(normalized, LocalDateTime.now());
        log("黑名单管理", normalized + " 加入黑名单", true);
        return Outcome.ok("OK", "已加入黑名单", normalized, null);
    }

    @Override
    public synchronized Outcome removeFromBlacklist(String plate) {
        String normalized = normalize(plate);
        if (normalized == null) {
            return Outcome.fail("FAIL", "车牌格式不正确", plate);
        }
        if (blacklist.remove(normalized) == null) {
            return Outcome.fail("FAIL", "该车牌不在黑名单中", normalized);
        }
        log("黑名单管理", normalized + " 移出黑名单", true);
        return Outcome.ok("OK", "已移出黑名单", normalized, null);
    }

    @Override
    public synchronized List<BlacklistEntry> blacklist() {
        List<BlacklistEntry> result = new ArrayList<>();
        blacklist.forEach((plate, time) -> result.add(new BlacklistEntry(plate, time)));
        result.sort((a, b) -> b.addedAt().compareTo(a.addedAt()));
        return List.copyOf(result);
    }

    @Override
    public synchronized List<LogEntry> logs() {
        return List.copyOf(logs);
    }

    @Override
    public synchronized int unitCost() {
        return unitCost;
    }

    @Override
    public synchronized Outcome setUnitCost(int cost) {
        if (cost < 0 || cost > 9999) {
            return Outcome.fail("FAIL", "费率必须在 0 - 9999 元/小时之间", null);
        }
        int old = unitCost;
        unitCost = cost;
        log("计费设置", "单位费率由 " + old + " 元/小时调整为 " + cost + " 元/小时", true);
        return Outcome.ok("OK", "计费规则已更新为 " + cost + " 元/小时", null, null);
    }

    @Override
    public synchronized List<AppointmentEntry> appointments() {
        List<AppointmentEntry> result = new ArrayList<>();
        appointments.forEach((plate, time) ->
                result.add(new AppointmentEntry(plate, time, "已预约")));
        result.sort((a, b) -> a.time().compareTo(b.time()));
        return List.copyOf(result);
    }

    @Override
    public synchronized Outcome appoint(String plate, LocalDateTime time) {
        String normalized = normalize(plate);
        if (normalized == null) {
            return Outcome.fail("FAIL", "车牌格式不正确", plate);
        }
        if (time == null) {
            return Outcome.fail("FAIL", "请选择预约时间", normalized);
        }
        if (time.isBefore(LocalDateTime.now().withSecond(0).withNano(0))) {
            return Outcome.fail("FAIL", "预约时间不能早于当前时间", normalized);
        }
        if (appointments.containsKey(normalized)) {
            return Outcome.fail("FAIL", "该车牌已有预约，请先取消", normalized);
        }
        appointments.put(normalized, time);
        log("访客预约", normalized + " 预约 " + time.format(DateTimeFormatter.ofPattern("MM-dd HH:mm")), true);
        return Outcome.ok("OK", "预约成功：" + time.format(DateTimeFormatter.ofPattern("MM-dd HH:mm")),
                normalized, null);
    }

    @Override
    public synchronized Outcome cancelAppointment(String plate) {
        String normalized = normalize(plate);
        if (normalized == null) {
            return Outcome.fail("FAIL", "车牌格式不正确", plate);
        }
        if (appointments.remove(normalized) == null) {
            return Outcome.fail("FAIL", "未找到该车牌的预约", normalized);
        }
        log("取消预约", normalized + " 取消预约", true);
        return Outcome.ok("OK", "预约已取消", normalized, null);
    }

    @Override
    public synchronized Outcome registerVisitor(VisitorRegistration registration) {
        if (registration == null) {
            return Outcome.fail("FAIL", "登记信息为空", null);
        }
        String plate = normalize(registration.plate());
        if (registration.name() == null || registration.name().isBlank()) {
            return Outcome.fail("FAIL", "请填写访客姓名", plate);
        }
        if (plate == null) {
            return Outcome.fail("FAIL", "车牌格式不正确", registration.plate());
        }
        if (registration.phone() == null || !registration.phone().matches("\\d{6,15}")) {
            return Outcome.fail("FAIL", "联系电话应为 6 - 15 位数字", plate);
        }
        VisitorRegistration saved = new VisitorRegistration(registration.name().trim(),
                registration.phone().trim(), plate, registration.purpose(),
                registration.time() == null ? LocalDateTime.now() : registration.time());
        registrations.add(0, saved);
        log("访客登记", saved.name() + "（" + plate + "）登记成功", true);
        return Outcome.ok("OK", "访客登记成功：" + saved.name() + " / " + plate, plate, null);
    }

    @Override
    public synchronized List<VisitorRegistration> visitorRegistrations() {
        return List.copyOf(registrations);
    }

    @Override
    public synchronized PlateQuery query(String plate) {
        String normalized = normalize(plate);
        if (normalized == null) {
            return new PlateQuery(plate, false, -1, null, 0, false, -1, false, null);
        }
        int parkedIndex = findParked(normalized);
        int queueIndex = findQueued(normalized);
        DemoCar car = parkedIndex >= 0 ? parked.get(parkedIndex) : null;
        LocalDateTime entry = car == null ? null : car.entryTime();
        Integer fee = car == null ? null : (int) billableHours(car.entryTime()) * unitCost;
        return new PlateQuery(normalized, car != null, parkedIndex + 1, entry,
                minutesBetween(entry, LocalDateTime.now()), queueIndex >= 0, queueIndex + 1,
                blacklist.containsKey(normalized), fee);
    }

    // ------------------------------------------------------------------
    // 演示数据
    // ------------------------------------------------------------------

    private void seedAccounts() {
        accounts.put(key(Role.ADMIN, "admin"),
                new Account("admin", "admin123", "林老师"));
        accounts.put(key(Role.STAFF, "staff"),
                new Account("staff", "staff123", "王保安"));
        accounts.put(key(Role.VISITOR, "visitor"),
                new Account("visitor", "visitor123", "陈访客"));
    }

    private void seedState() {
        LocalDateTime now = LocalDateTime.now();
        parked.add(new DemoCar("京A12345", now.minusMinutes(126)));
        parked.add(new DemoCar("京B67890", now.minusMinutes(43)));
        parked.add(new DemoCar("京C24680", now.minusMinutes(261)));
        parked.add(new DemoCar("沪D13579", now.minusMinutes(19)));
        parked.add(new DemoCar("京E86420", now.minusMinutes(76)));
        parked.add(new DemoCar("粤F11223", now.minusMinutes(9)));

        queue.add(new DemoCar("京G55667", now.minusMinutes(16)));
        queue.add(new DemoCar("京H77889", now.minusMinutes(9)));
        queue.add(new DemoCar("京J99001", now.minusMinutes(4)));

        blacklist.put("京N00001", now.minusDays(2));
        blacklist.put("津A66666", now.minusDays(5));

        appointments.put("京K12345", now.plusDays(1).withHour(10).withMinute(0).withSecond(0));

        logs.add(new LogEntry(now.minusMinutes(4), "系统", "队列补位 京J99001 自动补位入场", "成功", true));
        logs.add(new LogEntry(now.minusMinutes(9), "王保安", "入场识别 粤F11223 识别成功，道闸抬起", "成功", true));
        logs.add(new LogEntry(now.minusMinutes(16), "系统", "入场识别 京G55667 车位已满，加入等候队列", "等待", true));
        logs.add(new LogEntry(now.minusMinutes(43), "王保安", "入场识别 京B67890 识别成功，道闸抬起", "成功", true));
        logs.add(new LogEntry(now.minusHours(2).minusMinutes(6), "林老师", "计费设置 单位费率 5 元/小时", "成功", true));
        logs.add(new LogEntry(now.minusHours(3), "林老师", "黑名单管理 京N00001 加入黑名单", "成功", true));
        logs.add(new LogEntry(now.minusDays(5), "系统", "入场识别 津A66666 命中黑名单，拒绝入场", "拒绝", false));
    }

    // ------------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------------

    private void openGate(String plate) {
        gateOpen = true;
        lastPlate = plate;
        gateChangedAt = LocalDateTime.now();
    }

    private long billableHours(LocalDateTime entry) {
        long minutes = minutesBetween(entry, LocalDateTime.now());
        return Math.max(1, (minutes + 59) / 60);
    }

    private static long minutesBetween(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) {
            return 0;
        }
        return Math.max(0, java.time.temporal.ChronoUnit.MINUTES.between(from, to));
    }

    private int findParked(String plate) {
        for (int i = 0; i < parked.size(); i++) {
            if (plate.equalsIgnoreCase(parked.get(i).plate())) {
                return i;
            }
        }
        return -1;
    }

    private int findQueued(String plate) {
        for (int i = 0; i < queue.size(); i++) {
            if (plate.equalsIgnoreCase(queue.get(i).plate())) {
                return i;
            }
        }
        return -1;
    }

    private void log(String action, String result, boolean success) {
        String operator = session == null ? "系统" : session.displayName();
        logs.add(0, new LogEntry(LocalDateTime.now(), operator, action + " " + result,
                success ? "成功" : "失败", success));
        while (logs.size() > 200) {
            logs.remove(logs.size() - 1);
        }
    }

    private static String normalize(String plate) {
        if (plate == null) {
            return null;
        }
        String value = plate.replace(" ", "").replace("·", "").trim().toUpperCase();
        return value.matches("[\\u4e00-\\u9fa5A-Z0-9]{5,9}") ? value : null;
    }

    private static String key(Role role, String account) {
        return role.name() + ":" + (account == null ? "" : account.trim().toLowerCase());
    }

    private record DemoCar(String plate, LocalDateTime entryTime) {
    }

    private record Account(String account, String password, String displayName) {
    }
}
