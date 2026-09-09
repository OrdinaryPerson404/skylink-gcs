package com.cherglow.gcs.ui.pages;

import javafx.scene.layout.BorderPane;

/**
 * 页面抽象基类：统一页面路由标识与切换生命周期钩子。
 * MainShell 以 Map&lt;String, BasePage&gt; 持有全部页面，经基类引用多态分发切换事件；
 * 五个功能页（Overview/Fly/Plan/Setup/Data）均继承本类。
 */
public abstract class BasePage extends BorderPane {

    protected BasePage() {
        getStyleClass().add("page-root");
    }

    /** 页面路由 id（与 TopNav.TABS 的 id 一致，如 "overview"/"fly"） */
    public abstract String pageId();

    /** 页面被切换到前台时的钩子；默认无操作，子类按需覆写 */
    public void onPageShown() {
    }
}
