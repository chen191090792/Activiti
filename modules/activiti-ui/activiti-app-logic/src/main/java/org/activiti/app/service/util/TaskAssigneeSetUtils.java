package org.activiti.app.service.util;

import org.activiti.app.security.SecurityUtils;
import org.activiti.app.service.exception.MyTaskException;
import org.activiti.app.util.KiteApiCallUtils;
import org.activiti.engine.TaskService;
import org.activiti.engine.identity.User;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * @author cxh
 * @version V1.0
 * @Description: ${todo}(这里用一句话描述这个类的作用)
 * @date 2019/7/19 9:29
 */
public class TaskAssigneeSetUtils {

    public static void setAssignee(List<Task> tasks , ProcessInstance processInstance, String assignee, TaskService taskService){
        for(Task task:tasks){
            taskService.setAssignee(task.getId(),assignee);
            sendMessage(task);
        }
    }
    /**
     　* @description: 邮箱，微信推送
     　* @param [task]
     　* @return void
     　* @throws
     　* @author cxh
     　* @date 2019/7/19 9:35
     　*/
    private static void sendMessage(Task task){
             KiteApiCallUtils.sendWxMsg(task);
          //KiteApiCallUtils.sendEmail(task);
    }

}