package com.smartparking.ui.backend;

/**
 * 界面角色。刻意不复用 {@code LoginSystem.AccountType}，
 * 这样界面层不依赖业务层的枚举命名，业务层改签名时只需改适配器。
 */
public enum Role {

    ADMIN("管理员", "管理员控制台"),
    STAFF("安保人员", "安保工作台"),
    VISITOR("访客", "访客服务台");

    private final String label;
    private final String workspaceTitle;

    Role(String label, String workspaceTitle) {
        this.label = label;
        this.workspaceTitle = workspaceTitle;
    }

    /** 登录页、导航栏显示的中文名称。 */
    public String label() {
        return label;
    }

    /** 主界面右上角显示的岗位标题。 */
    public String workspaceTitle() {
        return workspaceTitle;
    }
}
