package org.activiti.app.rest.entity;

/**
 * @author cxh
 * @version V1.0
 * @Description: ${todo}(这里用一句话描述这个类的作用)
 * @date 2019/7/1 9:50
 */
public class ResultMsg {
    private int errorCode;
    private String errorMsg;

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }
}