package com.smartparking.ui.view;

/** 页面每次显示或定时刷新时调用，只更新数据，不重建用户正在编辑的输入框。 */
public interface Refreshable {

    void onShow();
}
