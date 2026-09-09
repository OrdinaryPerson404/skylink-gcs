package com.cherglow.gcs.model;

import javafx.application.Platform;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** S17 GNSS 在线状态：有效定位置位、5s 超时复位、NaN 不影响。
 *  注意：LiveVehicle 是单例，测试之间状态共享，因此按顺序执行。 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LiveVehicleGnssOnlineTest {

    private static boolean fxOk;

    @BeforeAll
    static void initFx() {
        try {
            Platform.startup(() -> {});
            fxOk = true;
        } catch (Throwable t) {
            fxOk = false;
        }
    }

    @AfterAll
    static void stopFx() {
        if (fxOk) {
            Platform.exit();
        }
    }

    private static void waitForFxUpdate(CountDownLatch latch, long timeoutSec) throws Exception {
        assertTrue(latch.await(timeoutSec, TimeUnit.SECONDS), "FX 线程应用更新超时");
    }

    /** 排空 FX 事件队列：空 runLater 串行排在 apply 之后，返回即代表 updateGps 的 apply 已整体完成 */
    private static void drainFxQueue() throws Exception {
        CountDownLatch tail = new CountDownLatch(1);
        Platform.runLater(tail::countDown);
        assertTrue(tail.await(3, TimeUnit.SECONDS), "FX 队列排空超时");
    }

    @Test
    @Order(1)
    void validGpsPushSetsGnssOnline() throws Exception {
        Assumptions.assumeTrue(fxOk);
        LiveVehicle lv = LiveVehicle.get();
        CountDownLatch latch = new CountDownLatch(1);
        // 监听 latDeg 变化（总是触发，不受 gnssOnline 当前状态影响）
        lv.latDeg.addListener((o, a, b) -> latch.countDown());
        lv.updateGps(31.1774276, 121.5272106, 1.8);
        waitForFxUpdate(latch, 3);
        drainFxQueue(); // latDeg 监听先于 gnssOnline 置位触发，须等 apply 整体执行完再断言
        assertTrue(lv.gnssOnline.get(), "有效定位应置位 gnssOnline");
        assertTrue(lv.gpsFix.get(), "有效定位应置位 gpsFix");
    }

    @Test
    @Order(2)
    void secondValidPushRefreshesTimestamp() throws Exception {
        Assumptions.assumeTrue(fxOk);
        LiveVehicle lv = LiveVehicle.get();
        // gnssOnline 已经是 true（来自 test 1），再次推流应刷新时间戳但不触发 change
        CountDownLatch latch = new CountDownLatch(1);
        lv.latDeg.addListener((o, a, b) -> latch.countDown());
        lv.updateGps(28.68, 115.88, 1.5);
        waitForFxUpdate(latch, 3);
        assertTrue(lv.gnssOnline.get(), "连续推流应保持 gnssOnline = true");
        // 给看门狗一点时间确认不触发离线
        Thread.sleep(1500);
        assertTrue(lv.gnssOnline.get(), "1.5s 内不应超时");
    }

    @Test
    @Order(3)
    void gnssOnlineResetsAfterTimeout() throws Exception {
        Assumptions.assumeTrue(fxOk);
        LiveVehicle lv = LiveVehicle.get();
        // 前置：当前 gnssOnline = true（来自 test 2）
        assertTrue(lv.gnssOnline.get(), "前置条件：gnssOnline 应为 true");

        // 等待超时（5s 超时 + 1s 检测间隔 + 余量）
        CountDownLatch offlineLatch = new CountDownLatch(1);
        lv.gnssOnline.addListener((o, a, b) -> {
            if (!b) offlineLatch.countDown();
        });
        assertTrue(offlineLatch.await(8, TimeUnit.SECONDS), "超时后 gnssOnline 应复位为 false");
        assertFalse(lv.gnssOnline.get(), "gnssOnline 应为 false");
        // gpsFix 不复位（曾有定位语义保留）
        assertTrue(lv.gpsFix.get(), "gpsFix 不应因超时而复位");
    }

    @Test
    @Order(4)
    void gnssOnlineRecoversAfterNewFix() throws Exception {
        Assumptions.assumeTrue(fxOk);
        LiveVehicle lv = LiveVehicle.get();
        // 前置：当前 gnssOnline = false（来自 test 3 超时）
        assertFalse(lv.gnssOnline.get(), "前置条件：gnssOnline 应为 false");

        // 重新推流 → 应恢复在线
        CountDownLatch latch = new CountDownLatch(1);
        lv.gnssOnline.addListener((o, a, b) -> {
            if (b) latch.countDown();
        });
        lv.updateGps(28.69, 115.89, 2.0);
        assertTrue(latch.await(3, TimeUnit.SECONDS), "新定位应恢复 gnssOnline 为 true");
        assertTrue(lv.gnssOnline.get());
    }

    @Test
    @Order(5)
    void nanPushDoesNotAffectGnssOnline() throws Exception {
        Assumptions.assumeTrue(fxOk);
        LiveVehicle lv = LiveVehicle.get();
        // 前置：当前 gnssOnline = true（来自 test 4）
        assertTrue(lv.gnssOnline.get(), "前置条件：gnssOnline 应为 true");

        // NaN 推流：不应更新时间戳，也不应翻转在线状态
        boolean before = lv.gnssOnline.get();
        lv.updateGps(Double.NaN, Double.NaN, Double.NaN);
        Thread.sleep(200);
        assertTrue(lv.gnssOnline.get(), "NaN 推流不应改变 gnssOnline 状态");
    }
}
