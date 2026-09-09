package com.cherglow.gcs.ui;

import com.cherglow.gcs.ui.pages.BasePage;
import com.cherglow.gcs.ui.pages.DataPage;
import com.cherglow.gcs.ui.pages.FlyPage;
import com.cherglow.gcs.ui.pages.OverviewPage;
import com.cherglow.gcs.ui.pages.PlanPage;
import com.cherglow.gcs.ui.pages.SetupPage;
import javafx.geometry.Insets;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

import java.util.HashMap;
import java.util.Map;

/**
 * 主外壳：TopNav + 页面栈 + Toast 覆盖层。
 * 页面切换与数字键 1-5 快捷键在此路由（输入框聚焦时屏蔽，对齐原型行为）。
 */
public class MainShell extends BorderPane {

    private final StackPane pageStack = new StackPane();
    private final StackPane toastOverlay = new StackPane();
    private final TopNav topNav;
    private final Map<String, BasePage> pages = new HashMap<>();
    private final FlyPage flyPage = new FlyPage();
    private final PlanPage planPage = new PlanPage();
    private Runnable connectOpener;

    public MainShell() {
        register(new OverviewPage());
        register(flyPage);
        register(planPage);
        register(new SetupPage());
        register(new DataPage());
        pageStack.getChildren().addAll(pages.values());
        flyPage.setGoToPlan(() -> switchPage("plan"));

        topNav = new TopNav(this::switchPage, null, null, () -> {
            if (connectOpener != null) {
                connectOpener.run();
            }
        });
        // topNav 与 toast 覆盖层同置于顶部区域（覆盖层不拦截鼠标）
        var topArea = new StackPane(topNav, toastOverlay);
        toastOverlay.setMouseTransparent(true);
        toastOverlay.setPickOnBounds(false);
        toastOverlay.setAlignment(javafx.geometry.Pos.TOP_CENTER);
        toastOverlay.setPadding(new Insets(8, 0, 0, 0));
        setTop(topArea);
        setCenter(pageStack);
        Toast.bind(toastOverlay);
    }

    /** 注册连接对话框打开器（由 App 注入，需要 Stage） */
    public void setConnectOpener(Runnable opener) {
        this.connectOpener = opener;
    }

    /** 页面切换；返回 true 表示路由成功 */
    public boolean switchPage(String id) {
        BasePage page = pages.get(id);
        if (page == null) {
            return false;
        }
        page.toFront();
        topNav.setActive(id);
        page.onPageShown(); // 经基类引用多态分发，各页按需覆写
        return true;
    }

    /** 以 pageId() 为键注册页面（路由 id 与 TopNav.TABS 一致） */
    private void register(BasePage page) {
        pages.put(page.pageId(), page);
    }

    /** 数字键 1-5 快捷键；TextInputControl 聚焦时屏蔽 */
    public void installKeyboardShortcuts(javafx.scene.Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (scene.getFocusOwner() instanceof javafx.scene.control.TextInputControl) {
                return;
            }
            int index = digitOf(e.getCode());
            if (index >= 0 && index < TopNav.TABS.size()) {
                if (switchPage(TopNav.TABS.get(index).id())) {
                    e.consume();
                }
            }
            // Ctrl+A：解锁确认（飞行页安全指令）
            if (e.isControlDown() && e.getCode() == javafx.scene.input.KeyCode.A) {
                flyPage.confirmArm();
                e.consume();
            }
        });
    }

    private static int digitOf(KeyCode code) {
        return switch (code) {
            case DIGIT1, NUMPAD1 -> 0;
            case DIGIT2, NUMPAD2 -> 1;
            case DIGIT3, NUMPAD3 -> 2;
            case DIGIT4, NUMPAD4 -> 3;
            case DIGIT5, NUMPAD5 -> 4;
            default -> -1;
        };
    }
}
