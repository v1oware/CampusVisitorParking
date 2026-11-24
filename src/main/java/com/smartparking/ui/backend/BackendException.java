package com.smartparking.ui.backend;

/** 业务层不可用、文件损坏等基础设施异常；界面会转成可读提示。 */
public class BackendException extends Exception {

    public BackendException(String message) {
        super(message);
    }

    public BackendException(String message, Throwable cause) {
        super(message, cause);
    }
}
