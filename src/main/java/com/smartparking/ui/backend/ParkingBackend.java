package com.smartparking.ui.backend;

import java.time.LocalDateTime;
import java.util.List;

/**
 * GUI 与业务层之间唯一的契约。
 *
 * <p>界面层（view 包）只依赖本接口和下面的 DTO，从不直接 import
 * {@code model} / {@code service} / {@code datastructure} 中的任何类。
 * 因此队友改业务实现时，只需要保证 {@link ServiceBackend} 能编译通过，
 * 或者自行实现本接口并在 {@link BackendFactory} 中注册。</p>
 *
 * <p>所有方法都在 JavaFX 应用线程上调用；实现类无需考虑并发。</p>
 */
public interface ParkingBackend {

    /** 数据源显示名，例如“演示数据”。 */
    String modeLabel();

    /** 是否为可离线演示的模拟实现（界面会显示提示条）。 */
    default boolean isDemo() {
        return false;
    }

    /**
     * 登录。账号密码错误返回 {@code null}；后端不可用时抛 {@link BackendException}。
     * 验证码由界面层本地校验，不传入业务层。
     */
    UserSession login(Role role, String account, String password) throws BackendException;

    /** 当前停车场完整快照。返回的集合不为 null。 */
    Snapshot snapshot();

    /** 车辆入场（含黑名单拦截、满位排队）。 */
    Outcome enter(String plate);

    /** 车辆离场（含挪车、计费、等候队列补位）。 */
    Outcome leave(String plate);

    /** 手动抬杆 / 落杆。 */
    Outcome setGate(boolean open);

    /** 预估停车费；车辆不在场时返回 {@code null}。 */
    Quote quote(String plate);

    /** 加入黑名单。 */
    Outcome addToBlacklist(String plate);

    /** 移出黑名单。 */
    Outcome removeFromBlacklist(String plate);

    /** 黑名单列表，按加入时间倒序。 */
    List<BlacklistEntry> blacklist();

    /** 操作日志，最新在前。 */
    List<LogEntry> logs();

    int unitCost();

    /** 设置单位小时费率。 */
    Outcome setUnitCost(int cost);

    /** 当前账号的预约列表。 */
    List<AppointmentEntry> appointments();

    /** 提交预约。 */
    Outcome appoint(String plate, LocalDateTime time);

    /** 取消预约。 */
    Outcome cancelAppointment(String plate);

    /** 安保人员登记访客。 */
    Outcome registerVisitor(VisitorRegistration registration);

    /** 已登记的访客记录，最新在前。 */
    List<VisitorRegistration> visitorRegistrations();

    /** 按车牌综合查询：是否在场、是否排队、是否黑名单、预估费用。 */
    PlateQuery query(String plate);

    /**
     * 应用退出前的收尾钩子。真实业务层可在这里统一保存 JSON、关闭文件。
     * 默认空实现，演示数据无需持久化。
     */
    default void shutdown() {
    }

    // ------------------------------------------------------------------
    // DTO：全部使用 record，字段只读，界面层可以直接展示。
    // ------------------------------------------------------------------

    /** 登录成功后的会话信息。 */
    record UserSession(Role role, String account, String displayName) {
    }

    /** 场内车辆。index 0 为栈顶（最靠近出口、最后驶入的车辆）。 */
    record ParkedCar(String plate, LocalDateTime entryTime, long parkedMinutes, int slot) {
    }

    /** 排队车辆。position 从 1 开始。 */
    record QueueCar(String plate, int position, LocalDateTime since, long waitingMinutes) {
    }

    /** 道闸状态。 */
    record Gate(boolean open, String lastPlate, LocalDateTime changedAt) {
    }

    /** 某一时刻的停车场全量状态。 */
    record Snapshot(int capacity, List<ParkedCar> parkedTopFirst, List<QueueCar> queue,
                    Gate gate, int unitCost) {
        public int occupied() {
            return parkedTopFirst.size();
        }

        public int free() {
            return Math.max(0, capacity - occupied());
        }

        public boolean full() {
            return occupied() >= capacity;
        }

        public double occupancyRate() {
            return capacity <= 0 ? 0 : (double) occupied() / capacity;
        }
    }

    /**
     * 业务操作结果。
     *
     * @param code    机器可读的结果码：PASS / WAIT / REJECT / LEFT / OK / FAIL ...
     * @param message 给用户看的中文说明
     * @param fee     涉及费用时的金额，否则为 null
     */
    record Outcome(boolean success, String code, String message, String plate,
                   Integer fee, LocalDateTime time) {

        public static Outcome ok(String code, String message, String plate, Integer fee) {
            return new Outcome(true, code, message, plate, fee, LocalDateTime.now());
        }

        public static Outcome fail(String code, String message, String plate) {
            return new Outcome(false, code, message, plate, null, LocalDateTime.now());
        }
    }

    /** 停车费报价。收费不足一小时按一小时计。 */
    record Quote(String plate, long hours, int unitCost, int fee, LocalDateTime exitTime) {
    }

    record BlacklistEntry(String plate, LocalDateTime addedAt) {
    }

    record LogEntry(LocalDateTime time, String operator, String action, String result, boolean success) {
    }

    record AppointmentEntry(String plate, LocalDateTime time, String status) {
    }

    record VisitorRegistration(String name, String phone, String plate, String purpose,
                               LocalDateTime time) {
    }

    record PlateQuery(String plate, boolean parked, int slot, LocalDateTime entryTime,
                      long parkedMinutes, boolean waiting, int queuePosition,
                      boolean banned, Integer estimatedFee) {
    }
}
