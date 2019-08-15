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
//            if(task!=null && "leader".equalsIgnoreCase(task.getAssignee())){
//                String assignee = KiteApiCallUtils.getUpLeader(processInstance.getStartUserId());
//                if(StringUtils.isNotEmpty(assignee)){
//                    if(assignee.contains("-1")){
//                        throw new MyTaskException("拟稿人上级领导未找到");
//                    }else{
//                        taskService.setAssignee(task.getId(),assignee);
//                        sendMessage(task);
//                    }
//                }else{
//                    throw new MyTaskException("上级领导未找到");
//                }
//            }else if(task!=null && "deptleader".equalsIgnoreCase(task.getAssignee())){
//                String assignee = KiteApiCallUtils.getDeptLeader(processInstance.getStartUserId());
//                if(StringUtils.isNotEmpty(assignee)){
//                    if(assignee.contains("-1")){
//                        throw new MyTaskException("部门领导未找到");
//                    }else{
//                        taskService.setAssignee(task.getId(),assignee);
//                        sendMessage(task);
//                    }
//                }else{
//                    throw new MyTaskException("部门领导未找到");
//                }
//            }else if(task!=null && "lastnodeleader".equalsIgnoreCase(task.getAssignee())){
//                User currentUser = SecurityUtils.getCurrentUserObject();
//                String assignee = KiteApiCallUtils.getUpLeader(currentUser.getId());
//                if(StringUtils.isNotEmpty(assignee)){
//                    if(assignee.contains("-1")){
//                        throw new MyTaskException("上级领导未找到");
//                    }else{
//                        taskService.setAssignee(task.getId(),assignee);
//                        sendMessage(task);
//                    }
//                }else{
//                    throw new MyTaskException("上级领导未找到");
//                }
//            }
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
        /*  KiteApiCallUtils.sendWxMsg(task);
          KiteApiCallUtils.sendEmail(task);*/
    }

}