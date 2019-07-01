package org.activiti.app.service.exception;

/**
 * @author cxh
 * @version V1.0
 * @Description: 自定义异常
 * @date 2019/6/28 17:52
 */
public class MyTaskException extends RuntimeException {

    public MyTaskException() {
        super();
    }
    public MyTaskException(String msg) {
        super(msg);
    }

}