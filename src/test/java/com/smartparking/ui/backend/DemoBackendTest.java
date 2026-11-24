package com.smartparking.ui.backend;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 演示数据源的业务自测，同时确认真实业务层适配器可实例化。 */
class DemoBackendTest {

    @Test
    void loginValidatesRoleAndPassword() {
        DemoBackend backend = new DemoBackend();
        assertNotNull(backend.login(Role.ADMIN, "admin", "admin123"));
        assertNull(backend.login(Role.ADMIN, "admin", "wrong"));
        assertNull(backend.login(Role.VISITOR, "admin", "admin123"));
        assertEquals(Role.STAFF, backend.login(Role.STAFF, "staff", "staff123").role());
    }

    @Test
    void snapshotReportsCapacityAndSeedData() {
        ParkingBackend.Snapshot snapshot = new DemoBackend().snapshot();
        assertEquals(20, snapshot.capacity());
        assertEquals(6, snapshot.occupied());
        assertEquals(14, snapshot.free());
        assertEquals(3, snapshot.queue().size());
        assertEquals(5, snapshot.unitCost());
        assertEquals(1, snapshot.parkedTopFirst().get(0).slot());
    }

    @Test
    void billingRoundsUpToWholeHours() {
        DemoBackend backend = new DemoBackend();
        ParkingBackend.Quote quote = backend.quote("京A12345");
        assertNotNull(quote);
        assertEquals(3, quote.hours());
        assertEquals(15, quote.fee());
        assertNull(backend.quote("京Z00000"));
    }

    @Test
    void blacklistRejectsEntryAndCanBeRemoved() {
        DemoBackend backend = new DemoBackend();
        ParkingBackend.Outcome rejected = backend.enter("京N00001");
        assertFalse(rejected.success());
        assertEquals("REJECT", rejected.code());

        assertTrue(backend.addToBlacklist("京Z99999").success());
        assertTrue(backend.blacklist().stream()
                .anyMatch(entry -> entry.plate().equals("京Z99999")));
        assertTrue(backend.removeFromBlacklist("京Z99999").success());
        assertFalse(backend.blacklist().stream()
                .anyMatch(entry -> entry.plate().equals("京Z99999")));
    }

    @Test
    void leavingChargesFeeAndRefillsFromQueue() {
        DemoBackend backend = new DemoBackend();
        ParkingBackend.Outcome leave = backend.leave("京A12345");
        assertTrue(leave.success());
        assertEquals(15, leave.fee());
        assertEquals(2, backend.snapshot().queue().size());
        assertEquals(6, backend.snapshot().occupied());
        assertEquals(1, backend.snapshot().queue().get(0).position());
    }

    @Test
    void fullLotQueuesNewArrivalAndRejectsDuplicate() {
        DemoBackend backend = new DemoBackend();
        int index = 0;
        while (backend.snapshot().occupied() < 20) {
            backend.enter("京T2" + String.format("%04d", index++));
        }
        assertEquals(20, backend.snapshot().occupied());
        ParkingBackend.Outcome waiting = backend.enter("京T30001");
        assertTrue(waiting.success());
        assertEquals("WAIT", waiting.code());
        assertFalse(backend.enter("京T30001").success());
    }

    @Test
    void appointmentAndVisitorRegistrationWork() {
        DemoBackend backend = new DemoBackend();
        LocalDateTime time = LocalDateTime.now().plusDays(2).withHour(9).withMinute(0);
        assertTrue(backend.appoint("京T40001", time).success());
        assertTrue(backend.appointments().stream()
                .anyMatch(entry -> entry.plate().equals("京T40001")));
        assertTrue(backend.cancelAppointment("京T40001").success());

        var registration = new ParkingBackend.VisitorRegistration("张三", "13800138000",
                "京T50001", "探亲访友", LocalDateTime.now());
        assertTrue(backend.registerVisitor(registration).success());
        assertEquals(1, backend.visitorRegistrations().size());
    }

    @Test
    void queryAggregatesParkedQueueAndBlacklistState() {
        DemoBackend backend = new DemoBackend();
        ParkingBackend.PlateQuery parked = backend.query("京B67890");
        assertTrue(parked.parked());
        assertTrue(parked.slot() > 0);
        assertNotNull(parked.estimatedFee());

        ParkingBackend.PlateQuery queued = backend.query("京G55667");
        assertFalse(queued.parked());
        assertTrue(queued.waiting());
        assertEquals(1, queued.queuePosition());

        assertTrue(backend.query("京N00001").banned());
    }

    @Test
    void rateChangesArePersistedAndLogged() {
        DemoBackend backend = new DemoBackend();
        assertTrue(backend.setUnitCost(8).success());
        assertEquals(8, backend.snapshot().unitCost());
        assertFalse(backend.logs().isEmpty());
    }

    @Test
    void serviceBackendStartsAndFailsGracefully() throws BackendException {
        ParkingBackend service = new ServiceBackend();
        assertEquals(20, service.snapshot().capacity());
        assertNull(service.login(Role.ADMIN, "admin", "admin123"));
        assertFalse(service.enter("京A12345").success());
        assertTrue(service.logs().isEmpty());
    }
}
