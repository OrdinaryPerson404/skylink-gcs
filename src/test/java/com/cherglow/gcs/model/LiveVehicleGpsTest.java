package com.cherglow.gcs.model;

import javafx.application.Platform;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** S15 融合覆盖语义：有源定位写入、无源(NaN)不清空已有值。 */
class LiveVehicleGpsTest {

    private static boolean fxOk;

    @BeforeAll
    static void initFx() {
        try {
            Platform.startup(() -> {});
            fxOk = true;
        } catch (Throwable t) {
            fxOk = false; // 无显示环境则跳过（不阻塞构建）
        }
    }

    @AfterAll
    static void stopFx() {
        if (fxOk) {
            Platform.exit();
        }
    }

    /** 排空 FX 事件队列：空 runLater 串行排在 apply 之后，返回即代表 updateGps 的 apply 已整体完成 */
    private static void drainFxQueue() throws Exception {
        CountDownLatch tail = new CountDownLatch(1);
        Platform.runLater(tail::countDown);
        assertTrue(tail.await(3, TimeUnit.SECONDS), "FX 队列排空超时");
    }

    @Test
    void validGpsPushWritesPositionAndFix() throws Exception {
        Assumptions.assumeTrue(fxOk);
        LiveVehicle lv = LiveVehicle.get();
        CountDownLatch latch = new CountDownLatch(1);
        lv.latDeg.addListener((o, a, b) -> latch.countDown());
        lv.updateGps(31.1774276, 121.5272106, 1.8);
        assertTrue(latch.await(3, TimeUnit.SECONDS), "FX 线程应用更新超时");
        drainFxQueue(); // latDeg 监听先于 gpsFix 置位触发，须等 apply 整体执行完再断言
        assertEquals(31.1774276, lv.latDeg.get(), 1e-9);
        assertEquals(121.5272106, lv.lonDeg.get(), 1e-9);
        assertEquals(1.8, lv.spdMps.get(), 1e-9);
        assertTrue(lv.gpsFix.get());
    }

    @Test
    void nanPushDoesNotClearActiveFix() throws Exception {
        Assumptions.assumeTrue(fxOk);
        LiveVehicle lv = LiveVehicle.get();
        CountDownLatch first = new CountDownLatch(1);
        lv.latDeg.addListener((o, a, b) -> first.countDown());
        lv.updateGps(28.68, 115.88, Double.NaN);
        assertTrue(first.await(3, TimeUnit.SECONDS), "首个有效定位应用超时");
        double keepLat = lv.latDeg.get();
        assertTrue(Double.isFinite(keepLat));

        // 后续无源推流（NaN）：不得清空已锁定值、不得翻转 fix 位
        lv.updateGps(Double.NaN, Double.NaN, Double.NaN);
        Thread.sleep(150);
        assertEquals(keepLat, lv.latDeg.get(), 0.0, "无源不应清空有源纬度");
        assertTrue(lv.gpsFix.get(), "无源不应翻转定位标志");
    }
}