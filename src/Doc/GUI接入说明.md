# GUI 接入说明（队友 4 交付）

> **一句话**：界面已经完成，而且默认使用内置演示数据，可以独立运行。
> 队友完成业务层后，只需要让 `ui/backend/ServiceBackend.java` 里的调用对上，**界面代码不需要改**。

---

## 1. 已完成内容

- 登录页：三种身份、账号密码、4 位字母数字验证码（不区分大小写、点击换图）、数据源切换。
- 角色分流：管理员 / 安保人员 / 访客进入不同导航菜单。
- 管理员：工作台、车位状态、出入场放行、等候队列、黑名单、操作日志、计费设置、综合查询。
- 安保人员：工作台、访客登记、出入场放行、车位状态、等候队列。
- 访客：工作台、预约登记、车位查询、等候队列、缴费离场。
- 道闸可视化：入场 / 缴费成功时指示灯由红变绿、栏杆抬起，3 秒后自动落杆。
- 统一组件：卡片、表格、统计块、Toast、验证码、车位平面图、空状态。
- 离线演示数据源：不依赖任何业务类，可直接演示和答辩。

对应 SRS 的 NFR-1（登录页、分角色菜单、道闸状态可视化）已全部覆盖。

---

## 2. 运行方式

环境：JDK 17+、Maven 3.8+。

```bash
cd CampusVisitorParking
mvn clean javafx:run
```

默认进入**演示数据**模式，账号：

| 角色 | 账号 | 密码 |
| --- | --- | --- |
| 管理员 | `admin` | `admin123` |
| 安保人员 | `staff` | `staff123` |
| 访客 | `visitor` | `visitor123` |

### 切换到真实业务层

三种方式，任选其一：

1. **登录页下拉框**：数据源选择「真实业务层」，立即切换，不需要重启。
2. **启动参数**：
   ```bash
   mvn javafx:run -Dparking.backend=service
   ```
3. **环境变量**：
   ```bash
   set PARKING_BACKEND=service
   mvn javafx:run
   ```
   在 IDEA 中运行时，为 `com.smartparking.main.main` 添加 VM 参数
   `-Dparking.backend=service`。

> `pom.xml` 的编译级别已从 21 调整为 **17**，因为当前机器只装了 JDK 17。
> 如果小组统一使用 21，把 `<maven.compiler.release>` 改回 21 即可，代码没有使用 21 专属语法。

---

## 3. 分层结构：界面怎么连业务

```
ui/view/*  ──只依赖──▶  ui/backend/ParkingBackend（接口 + record DTO）
                              │
                              ├── DemoBackend        演示数据，不碰业务类
                              └── ServiceBackend     唯一适配器
                                        │
                                        ├── ParkingManager
                                        ├── ParkingLot / WaitingQueue
                                        ├── BlackList
                                        ├── Function
                                        └── LoginSystem
```

关键约定：

- `ui/view`、`MainShell`、`AppContext` **不 import** `model` / `service` / `datastructure`。
- 唯一例外是 `ui/backend/ServiceBackend.java`：整个界面层与业务层的“接缝”。
- 业务层签名变化时，只改 `ServiceBackend`，其他界面文件不动。
- 界面每 4 秒自动刷新一次，全部在 JavaFX 应用线程调用，**不需要考虑并发**。

`ServiceBackend` 自己持有唯一一套状态：

```java
private final ParkingLot parkingLot = new ParkingLot();
private final WaitingQueue waitingQueue = new WaitingQueue();
private final BlackList blackList = new BlackList();
private final Function function = new Function();
private final ParkingManager parkingManager = new ParkingManager();
```

所以所有页面看到的是同一份数据，不需要额外的全局单例。

---

## 4. 现在切到真实模式会发生什么

业务层还有空方法，`ServiceBackend` 会**优雅失败**，不会让界面崩溃：

| 操作 | 当前表现 |
| --- | --- |
| 登录 | `LoginSystem.login(type)` 返回 `false` → 提示账号或密码错误 |
| 入场 | `ParkingManager.enter` 返回 `false` 且栈/队列里没有该车 → 提示入场被拒绝 |
| 离场 | 车辆不在栈中 → 提示“该车辆不在场内” |
| 日志 | 返回空列表，页面显示“暂无操作日志” |
| 预约 / 取消 | `Function.appointment()` 返回 `false` → 提示失败 |
| 访客登记 | 提示“访客登记接口尚未接入” |
| 车位 / 队列 / 计费 | 能显示空停车场，费率读写正常 |

这不是 bug，是等队友实现的占位行为。

---

## 5. 队友接入清单

### 5.1 队友 1：核心调度（`ParkingManager` + 栈/队列）

**不用改界面。** 按 `分工.md` 实现即可：

```java
public boolean enter(ParkingLot lot, String plate)
public void leave(ParkingLot lot, String plate)
```

`ServiceBackend` 已经这样调用：

- `enter`：先查 `blackList.isBanned`，再调 `parkingManager.enter(parkingLot, plate)`。
  调用后不依赖返回值语义，而是**检查车牌是否进了栈或队列**：
  - 在栈里 → PASS
  - 在队列里 → WAIT
  - 两边都没有 → 失败
  所以即使 `enter` 现在返回 `false` 表示排队，界面也能正确显示 WAIT。
- `leave`：先按 `Function.getStoppingCost` 预估费用，再调 `parkingManager.leave`，
  然后检查车牌是否已不在栈中。队首补位由你的 `leave` 完成。

还需要补：

- `ParkingLot.liftUp()` / `liftDown()`：把 `gateSituation` 设为 1 / 0。
  界面通过 `getGateSituation()` 显示红绿灯。
- 如果以后把 `enter` 改成返回枚举（PASS / WAIT / REJECT），只改
  `ServiceBackend.enter` 里的结果映射，界面不动。

注意：`ServiceBackend` 创建的是 `new ParkingLot()`，构造函数签名不要改；如果需要初始数据，
放在无参构造里加载，或告诉我加一个初始化钩子。

### 5.2 队友 2：安全、计费、登录

#### BlackList

已接：`add(String)`、`remove(String)`、`isBanned(String)`、`getCarList()`。
界面黑名单页会显示 `Car.getLicensePlate()`；加入时间暂时用“当前时间”（`Car` 没有该字段）。

#### Function

已接：`getUnitCost()`、`setUnitCost(int)`、`getStoppingCost(Car)`。
计费设置页已经可以工作，只要 `Car.getStoppingHour()` 正确向上取整。

**需要改签名**的：

```java
// 现在：静态、无参数，界面拿不到车牌 / 时间
public static boolean appointment()
public static boolean appointmentCancel()

// 建议：改成实例方法并带上业务参数（返回值仍表示成功与否）
public boolean appointment(String account, String plate, LocalDateTime time)
public boolean appointmentCancel(String plate)

// 访客登记同理
public boolean reservedInfo(String name, String phone, String plate,
                            String purpose, LocalDateTime time)
```

改完后，把 `ServiceBackend.appoint / cancelAppointment / registerVisitor` 里的 TODO 换成新调用即可。
把 `Visitor` / `Date` 作为参数也可以，适配器负责转成界面使用的 `LocalDateTime`。

#### LoginSystem

**这是接入的第一道坎。** 现在只有：

```java
public static boolean login(AccountType accountType)
```

界面收集了账号密码，但传不进去。建议加一个重载（保留旧方法也行）：

```java
public static boolean login(AccountType accountType, String account, String password)
```

然后 `ServiceBackend.login` 只改一行：

```java
boolean ok = LoginSystem.login(toAccountType(role), account, password);
```

在此之前，真实模式任何账号都登不进去。

#### 验证码

验证码由**界面层本地生成和校验**（`ui/component/CaptchaPane`）：4 位、字母数字混合、
忽略大小写、点击换图。`LoginSystem.getVerificationCode()` 不会被界面调用，
可以保留给注册或其他场景，不影响 GUI。

### 5.3 队友 3：持久化与日志

#### 操作日志（界面已预留）

`Log` 现在为空类，所以真实模式日志页是空的。建议：

```java
// com.smartparking.util.Log
public record Entry(LocalDateTime time, String operator, String action,
                    String result, boolean success) {}

public static void append(String operator, String action, String result, boolean success)
public static List<Entry> readAll()
```

`ServiceBackend.logs()` 改成：

```java
@Override
public List<LogEntry> logs() {
    return Log.readAll().stream()
            .map(e -> new LogEntry(e.time(), e.operator(), e.action(), e.result(), e.success()))
            .toList();
}
```

日志文件建议 UTF-8 追加写，保证中文正常。

#### JSON 加载与保存

目前 `ServiceBackend` 只是 `new` 了空对象。要恢复上次状态，有两个方案：

**方案 A（推荐）**：在 `util` 增加一个聚合入口，避免 `model` 反向依赖 `util`：

```java
// com.smartparking.util.ParkingStore
public static State load();       // 读 4 个 JSON，返回停车场 / 队列 / 黑名单 / 费率
public static void save(State s); // 覆盖写回
```

然后在 `ServiceBackend` 构造器里调用 `ParkingStore.load()`，在 `shutdown()` 里调用 `save`。
`shutdown()` 已经由 `ParkingApp.stop()` 自动触发，不用写界面代码。

**方案 B**：在各 `*Json` 类里提供 `loadXxx()` / `saveXxx()`，在 `ServiceBackend` 构造器和
`shutdown()` 里逐个调用。

无论哪种方案，都只需要改 `ServiceBackend`；把方法名告诉我即可。

#### User 多态

`User` 有 `Admin / Staff / Visitor` 子类，`LoginSystem` 如果要从 JSON 读用户列表，
务必按分工加 `@JsonTypeInfo` / `@JsonSubTypes`，否则会反序列化成抽象类失败。
界面不直接读用户 JSON。

---

## 6. `ServiceBackend` TODO 对照表

| 界面功能 | 方法 | 当前调用 | 需要补什么 |
| --- | --- | --- | --- |
| 登录 | `login` | `LoginSystem.login(AccountType)` | 增加带账号密码的重载 |
| 入场 | `enter` | `blackList.isBanned` → `parkingManager.enter(lot, plate)` → 查栈/队列 | 实现 `ParkingManager.enter` |
| 离场 | `leave` | 先 `Function.getStoppingCost` 估价 → `parkingManager.leave(lot, plate)` → 查栈 | 实现 `ParkingManager.leave` + 补位 |
| 抬杆/落杆 | `setGate` | `parkingLot.liftUp/liftDown` | 实现 `ParkingLot` 的两个方法 |
| 黑名单 | `addToBlacklist` / `removeFromBlacklist` / `blacklist` | `BlackList` 四个方法 | 完成增删查 |
| 日志 | `logs` | 返回空列表 | 按 5.3 提供 `Log.readAll` |
| 费率 | `unitCost` / `setUnitCost` | `Function` 已完成 | 无需改动 |
| 费用试算 | `quote` | 复制 `Car` 的入场时间、设离场时间后算费 | 无需改动 |
| 预约 | `appointment` / `appointments` / `cancelAppointment` | 静态无参调用 / 空列表 | 改成带参数的实例方法 |
| 访客登记 | `registerVisitor` | 返回失败提示 | `Function.reservedInfo` 带参数 |
| 综合查询 | `query` | 查栈、队列、黑名单、费用 | 依赖上面各项 |
| 退出保存 | `shutdown` | 空实现 | 调 JSON 保存 |

---

## 7. 需要给 GUI 增加数据怎么办

如果某个页面需要业务层的新数据：

1. 在 `ui/backend/ParkingBackend.java` 增加方法（或 record 字段）；
2. 在 `DemoBackend` 写一个演示实现；
3. 在 `ServiceBackend` 里调用真实业务方法；
4. 页面只调用 `context.backend().xxx()`。

不要把 `model` / `service` 对象直接塞进 `view`，否则两边会越改越乱。

---

## 8. 五分钟联调步骤

1. `mvn clean javafx:run`，用演示账号确认界面正常（不依赖队友）。
2. 实现 `LoginSystem.login(AccountType, String, String)`，返回 `true` 即可先打通登录。
3. 启动参数加 `-Dparking.backend=service`，登录后应看到空停车场。
4. 实现 `ParkingManager.enter / leave` 和 `ParkingLot.liftUp / liftDown`，
   在「出入场放行」里输入车牌，观察车位平面图和道闸变化。
5. 实现 `BlackList`，在「黑名单管理」加入车牌，再尝试入场，应提示 REJECT。
6. 实现 `Log`，在「操作日志」应能看到记录。
7. 实现 JSON 加载/保存，在 `shutdown()` 调用保存，重启后数据应恢复。

---

## 9. 自测与常见问题

```bash
mvn test
```

`DemoBackendTest` 覆盖演示数据源的核心逻辑；真实模式用同一个界面即可人工验证。

**Q：真实模式登录一直失败？**
A：`LoginSystem.login` 还没有带账号密码的重载，或仍返回 `false`。

**Q：真实模式入场提示“入场被拒绝”？**
A：`ParkingManager.enter` 还没实现；或黑名单命中；或 `ParkingLot` 已满且没有入队。

**Q：道闸不动？**
A：`ParkingLot.liftUp / liftDown` 还没设置 `gateSituation`。界面自己在操作成功时会播放动画，
但 4 秒后的自动刷新会以 `getGateSituation()` 为准。

**Q：日志页空？**
A：`ServiceBackend.logs()` 还是占位实现，按 5.3 接入 `Log.readAll`。

**Q：改完业务代码，界面编译不过？**
A：只可能出在 `ServiceBackend`，因为其他界面文件不 import 业务类。

---

## 10. 文件清单（界面部分）

```
src/main/java/com/smartparking/ui/
├── ParkingApp.java                 JavaFX 入口
├── AppContext.java                 会话 / 数据源 / 页面切换 / 退出保存
├── MainShell.java                  侧边导航 + 顶栏 + 内容区
├── backend/
│   ├── ParkingBackend.java         接口 + 全部 DTO
│   ├── BackendFactory.java         demo / service 选择
│   ├── DemoBackend.java            离线演示实现
│   ├── ServiceBackend.java         真实业务层适配器（唯一 TODO 集中地）
│   ├── BackendException.java
│   └── Role.java
├── component/
│   ├── Components.java             卡片 / 统计 / 表格 / 空状态
│   ├── GateIndicator.java          道闸动画
│   ├── CaptchaPane.java            验证码
│   ├── Icons.java                  内置 SVG 图标
│   └── Toasts.java                 浮动提示
├── util/
│   ├── Fmt.java                    时间 / 金额 / 时长格式化
│   └── Plates.java                 车牌输入校验
└── view/
    ├── LoginView.java
    ├── DashboardView.java
    ├── ParkingStatusView.java
    ├── EntryExitView.java
    ├── QueueView.java
    ├── BlacklistView.java
    ├── LogView.java
    ├── BillingView.java
    ├── QueryView.java
    ├── AppointmentView.java
    ├── RegisterVisitorView.java
    ├── BaseView.java
    └── Refreshable.java

src/main/resources/css/app.css  全部界面样式
src/test/java/.../DemoBackendTest.java
```
