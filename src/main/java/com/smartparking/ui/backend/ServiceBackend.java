package com.smartparking.ui.backend;

import com.smartparking.datastructure.QueueNode;
import com.smartparking.datastructure.StackNode;
import com.smartparking.model.Car;
import com.smartparking.model.ParkingLot;
import com.smartparking.model.WaitingQueue;
import com.smartparking.service.BlackList;
import com.smartparking.service.Function;
import com.smartparking.service.LoginSystem;
import com.smartparking.service.ParkingManager;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * 真实业务层适配器：整个 ui 包中唯一 import {@code model/service/datastructure} 的类。
 *
 * <p><b>队友接入手册：</b>业务类签名变化时，只改这个文件，界面层无需改动。
 * 文件末尾有每个模块的 TODO 与建议签名。</p>
 */
public final class ServiceBackend implements ParkingBackend {

    private final ParkingLot parkingLot = new ParkingLot();
    private final WaitingQueue waitingQueue = new WaitingQueue();
    private final BlackList blackList = new BlackList();
    private final Function function = new Function();
    private final ParkingManager parkingManager = new ParkingManager();

    private UserSession session;

    public ServiceBackend() throws BackendException {
        try {
            // 触发一次状态读取，尽早暴露 JSON / 文件损坏问题。
            snapshot();
        } catch (RuntimeException e) {
            throw new BackendException("业务层初始化失败：" + e.getMessage(), e);
        }
    }

    @Override
    public String modeLabel() {
        return "真实业务层";
    }

    @Override
    public UserSession login(Role role, String account, String password) throws BackendException {
        try {
            // TODO(队友2)：LoginSystem.login 目前只有 AccountType 参数，收不到账号密码。
            // 完成校验后建议改为 login(AccountType, String account, String password)，
            // 然后在这里把 account / password 传下去即可，界面层不用动。
            boolean ok = LoginSystem.login(toAccountType(role));
            if (!ok) {
                return null;
            }
            String name = account == null || account.isBlank() ? role.label() : account.trim();
            session = new UserSession(role, name, name);
            return session;
        } catch (RuntimeException e) {
            throw new BackendException("登录模块异常：" + e.getMessage(), e);
        }
    }

    @Override
    public Snapshot snapshot() {
        List<ParkedCar> parked = new ArrayList<>();
        StackNode<Car> node = parkingLot.parkingLot.getTop();
        int slot = 1;
        while (node != null) {
            Car car = node.getData();
            if (car != null) {
                LocalDateTime entry = toLocalDateTime(car.getEntryTime());
                parked.add(new ParkedCar(car.getLicensePlate(), entry,
                        minutesSince(entry), slot));
                slot++;
            }
            node = node.getNext();
        }

        List<QueueCar> queue = new ArrayList<>();
        QueueNode<Car> qNode = waitingQueue.getQueue().getTop();
        int position = 1;
        while (qNode != null) {
            Car car = qNode.getData();
            if (car != null) {
                LocalDateTime since = toLocalDateTime(car.getEntryTime());
                queue.add(new QueueCar(car.getLicensePlate(), position, since,
                        minutesSince(since)));
                position++;
            }
            qNode = qNode.getNext();
        }

        boolean gateOpen = parkingLot.getGateSituation() == 1;
        String lastPlate = parked.isEmpty() ? null : parked.get(0).plate();
        Gate gate = new Gate(gateOpen, lastPlate, LocalDateTime.now());
        return new Snapshot(parkingLot.getMAXSIZE(), List.copyOf(parked), List.copyOf(queue),
                gate, function.getUnitCost());
    }

    @Override
    public Outcome enter(String plate) {
        String normalized = normalize(plate);
        if (normalized == null) {
            return Outcome.fail("FAIL", "车牌格式不正确", plate);
        }
        try {
            if (blackList.isBanned(normalized)) {
                return Outcome.fail("REJECT", "该车牌在黑名单中，禁止入场", normalized);
            }
            boolean accepted = parkingManager.enter(parkingLot, normalized);

            // 不依赖返回值语义：以操作后的真实状态为准。
            if (findParked(normalized) != null) {
                return Outcome.ok("PASS", "识别成功，道闸已抬起，车辆入场", normalized, null);
            }
            if (findQueued(normalized) != null) {
                return Outcome.ok("WAIT", "车位已满，车辆已加入等候队列", normalized, null);
            }
            return Outcome.fail(accepted ? "FAIL" : "REJECT",
                    accepted ? "入场未生效，请检查 ParkingManager.enter 实现"
                            : "入场被拒绝（黑名单、满位或业务层尚未实现）", normalized);
        } catch (RuntimeException e) {
            return Outcome.fail("FAIL", "入场异常：" + e.getMessage(), normalized);
        }
    }

    @Override
    public Outcome leave(String plate) {
        String normalized = normalize(plate);
        if (normalized == null) {
            return Outcome.fail("FAIL", "车牌格式不正确", plate);
        }
        Car car = findParked(normalized);
        if (car == null) {
            return Outcome.fail("FAIL", "该车辆不在场内", normalized);
        }
        Integer fee = quoteCost(car);
        try {
            parkingManager.leave(parkingLot, normalized);
            if (findParked(normalized) != null) {
                return Outcome.fail("FAIL", "离场未生效：请检查 ParkingManager.leave 实现", normalized);
            }
            return Outcome.ok("LEFT", "缴费成功，道闸已抬起，车辆离场", normalized, fee);
        } catch (RuntimeException e) {
            return Outcome.fail("FAIL", "离场异常：" + e.getMessage(), normalized);
        }
    }

    @Override
    public Outcome setGate(boolean open) {
        try {
            if (open) {
                parkingLot.liftUp();
            } else {
                parkingLot.liftDown();
            }
            return Outcome.ok("OK", open ? "道闸已抬起" : "道闸已落下", null, null);
        } catch (RuntimeException e) {
            return Outcome.fail("FAIL", "道闸操作异常：" + e.getMessage(), null);
        }
    }

    @Override
    public Quote quote(String plate) {
        String normalized = normalize(plate);
        if (normalized == null) {
            return null;
        }
        Car car = findParked(normalized);
        if (car == null || car.getEntryTime() == null) {
            return null;
        }
        LocalDateTime exit = LocalDateTime.now();
        long hours = stoppingHours(car.getEntryTime(), exit);
        int cost = (int) hours * function.getUnitCost();
        return new Quote(normalized, hours, function.getUnitCost(), cost, exit);
    }

    @Override
    public Outcome addToBlacklist(String plate) {
        String normalized = normalize(plate);
        if (normalized == null) {
            return Outcome.fail("FAIL", "车牌格式不正确", plate);
        }
        try {
            if (blackList.isBanned(normalized)) {
                return Outcome.fail("FAIL", "该车牌已在黑名单中", normalized);
            }
            blackList.add(normalized);
            if (!blackList.isBanned(normalized)) {
                return Outcome.fail("FAIL", "加入黑名单未生效：请检查 BlackList.add 实现", normalized);
            }
            return Outcome.ok("OK", "已加入黑名单", normalized, null);
        } catch (RuntimeException e) {
            return Outcome.fail("FAIL", "黑名单操作异常：" + e.getMessage(), normalized);
        }
    }

    @Override
    public Outcome removeFromBlacklist(String plate) {
        String normalized = normalize(plate);
        if (normalized == null) {
            return Outcome.fail("FAIL", "车牌格式不正确", plate);
        }
        try {
            blackList.remove(normalized);
            if (blackList.isBanned(normalized)) {
                return Outcome.fail("FAIL", "移出黑名单未生效：请检查 BlackList.remove 实现", normalized);
            }
            return Outcome.ok("OK", "已移出黑名单", normalized, null);
        } catch (RuntimeException e) {
            return Outcome.fail("FAIL", "黑名单操作异常：" + e.getMessage(), normalized);
        }
    }

    @Override
    public List<BlacklistEntry> blacklist() {
        List<BlacklistEntry> result = new ArrayList<>();
        List<Car> cars = blackList.getCarList();
        if (cars == null) {
            return List.of();
        }
        for (Car car : cars) {
            if (car != null) {
                result.add(new BlacklistEntry(car.getLicensePlate(), LocalDateTime.now()));
            }
        }
        result.sort(Comparator.comparing(BlacklistEntry::addedAt).reversed());
        return List.copyOf(result);
    }

    @Override
    public List<LogEntry> logs() {
        // TODO(队友3)：Log 类完成后，在这里调用日志读取方法即可。
        // 建议签名：public static List<LogEntry> readAll() （ui.backend 的 DTO）
        // 或返回 List<String>，由本适配器解析。目前返回空列表，界面显示空状态。
        return List.of();
    }

    @Override
    public int unitCost() {
        return function.getUnitCost();
    }

    @Override
    public Outcome setUnitCost(int cost) {
        if (cost < 0 || cost > 9999) {
            return Outcome.fail("FAIL", "费率必须在 0 - 9999 元/小时之间", null);
        }
        try {
            function.setUnitCost(cost);
            return Outcome.ok("OK", "计费规则已更新为 " + cost + " 元/小时", null, null);
        } catch (RuntimeException e) {
            return Outcome.fail("FAIL", "设置费率异常：" + e.getMessage(), null);
        }
    }

    @Override
    public List<AppointmentEntry> appointments() {
        // TODO(队友2)：Function 目前没有“按账号查询预约”的方法，完成后在此映射。
        return List.of();
    }

    @Override
    public Outcome appoint(String plate, LocalDateTime time) {
        try {
            // TODO(队友2)：建议改为 appointment(Visitor visitor, String plate, LocalDateTime time)
            boolean ok = Function.appointment();
            return ok
                    ? Outcome.ok("OK", "预约已提交", normalize(plate), null)
                    : Outcome.fail("FAIL", "预约失败：Function.appointment 尚未实现或当前无可用参数", normalize(plate));
        } catch (RuntimeException e) {
            return Outcome.fail("FAIL", "预约异常：" + e.getMessage(), normalize(plate));
        }
    }

    @Override
    public Outcome cancelAppointment(String plate) {
        try {
            boolean ok = Function.appointmentCancel();
            return ok
                    ? Outcome.ok("OK", "预约已取消", normalize(plate), null)
                    : Outcome.fail("FAIL", "取消失败：Function.appointmentCancel 尚未实现", normalize(plate));
        } catch (RuntimeException e) {
            return Outcome.fail("FAIL", "取消预约异常：" + e.getMessage(), normalize(plate));
        }
    }

    @Override
    public Outcome registerVisitor(VisitorRegistration registration) {
        // TODO(队友2)：Function.reservedInfo() 目前无参数，无法写入登记信息。
        // 建议改为 reservedInfo(VisitorRegistration info) 或返回 boolean。
        return Outcome.fail("FAIL", "访客登记接口尚未接入（见 ServiceBackend TODO）",
                registration == null ? null : registration.plate());
    }

    @Override
    public List<VisitorRegistration> visitorRegistrations() {
        return List.of();
    }

    @Override
    public void shutdown() {
        // TODO(队友3)：应用退出时在这里统一保存 JSON，例如：
        // BlackListJson.save(blackList);
        // CarListJson.save(parkingLot.parkingLot, waitingQueue.queue);
    }

    @Override
    public PlateQuery query(String plate) {
        String normalized = normalize(plate);
        if (normalized == null) {
            return new PlateQuery(plate, false, -1, null, 0, false, -1, false, null);
        }
        Car car = findParked(normalized);
        int slot = parkedSlot(normalized);
        boolean waiting = findQueued(normalized) != null;
        int queuePosition = queuePosition(normalized);
        boolean banned = blackList.isBanned(normalized);
        Integer fee = car == null ? null : quoteCost(car);
        LocalDateTime entry = car == null ? null : toLocalDateTime(car.getEntryTime());
        return new PlateQuery(normalized, car != null, slot, entry, minutesSince(entry),
                waiting, queuePosition, banned, fee);
    }

    // ------------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------------

    private Car findParked(String plate) {
        StackNode<Car> node = parkingLot.parkingLot.getTop();
        while (node != null) {
            Car car = node.getData();
            if (car != null && plate.equalsIgnoreCase(car.getLicensePlate())) {
                return car;
            }
            node = node.getNext();
        }
        return null;
    }

    private Car findQueued(String plate) {
        QueueNode<Car> node = waitingQueue.getQueue().getTop();
        while (node != null) {
            Car car = node.getData();
            if (car != null && plate.equalsIgnoreCase(car.getLicensePlate())) {
                return car;
            }
            node = node.getNext();
        }
        return null;
    }

    private int parkedSlot(String plate) {
        StackNode<Car> node = parkingLot.parkingLot.getTop();
        int slot = 1;
        while (node != null) {
            Car car = node.getData();
            if (car != null && plate.equalsIgnoreCase(car.getLicensePlate())) {
                return slot;
            }
            slot++;
            node = node.getNext();
        }
        return -1;
    }

    private int queuePosition(String plate) {
        QueueNode<Car> node = waitingQueue.getQueue().getTop();
        int position = 1;
        while (node != null) {
            Car car = node.getData();
            if (car != null && plate.equalsIgnoreCase(car.getLicensePlate())) {
                return position;
            }
            position++;
            node = node.getNext();
        }
        return -1;
    }

    private Integer quoteCost(Car car) {
        if (car.getEntryTime() == null) {
            return null;
        }
        try {
            Car probe = new Car(car.getLicensePlate());
            probe.setEntryTime(car.getEntryTime());
            probe.setExitTime(new Date());
            return function.getStoppingCost(probe);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static long stoppingHours(Date entry, LocalDateTime exit) {
        if (entry == null) {
            return 0;
        }
        long minutes = Math.max(0, ChronoUnit.MINUTES.between(
                toLocalDateTime(entry), exit));
        if (minutes == 0) {
            return 0;
        }
        return (minutes + 59) / 60;
    }

    private static long minutesSince(LocalDateTime time) {
        if (time == null) {
            return 0;
        }
        return Math.max(0, ChronoUnit.MINUTES.between(time, LocalDateTime.now()));
    }

    private static LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return Instant.ofEpochMilli(date.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    private static String normalize(String plate) {
        if (plate == null) {
            return null;
        }
        String value = plate.replace(" ", "").replace("·", "").trim().toUpperCase();
        return value.isEmpty() ? null : value;
    }

    private static LoginSystem.AccountType toAccountType(Role role) {
        return switch (role) {
            case ADMIN -> LoginSystem.AccountType.Admin;
            case STAFF -> LoginSystem.AccountType.Staff;
            case VISITOR -> LoginSystem.AccountType.Visitor;
        };
    }
}
