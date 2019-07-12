/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.app.service.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.app.security.SecurityUtils;
import org.activiti.app.service.api.ModelService;
import org.activiti.app.service.exception.MyTaskException;
import org.activiti.app.service.exception.NotFoundException;
import org.activiti.app.util.KiteApiCallUtils;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.identity.User;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Wrapper for various Activiti operations
 * 
 * @author jbarrez
 */
@Service
@Transactional
public class ActivitiService {
    private static final Logger logger = LoggerFactory.getLogger(ActivitiService.class);

	@Autowired
	private RuntimeService runtimeService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private ProcessInstanceService processInstanceService;
    @Autowired
    protected HistoryService historyService;
    @Autowired
    protected PermissionService permissionService;


    public ProcessInstance startProcessInstance(String processDefinitionId, Map<String, Object> variables, String processInstanceName) {
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinitionId, variables);
        if (!processInstance.isEnded() && processInstanceName != null) {
            runtimeService.setProcessInstanceName(processInstance.getId(), processInstanceName);
        }
        changeAssignee(processInstance.getId());
        return processInstance;
	}

    public void changeAssignee(String processInstanceId){
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstanceId).listPage(0, 1000000);
        for(Task task:tasks){
            if(task!=null && "leader".equalsIgnoreCase(task.getAssignee())){
                String assignee = KiteApiCallUtils.getUpLeader(processInstance.getStartUserId());
                if(StringUtils.isNotEmpty(assignee)){
                    if(assignee.contains("-1")){
                        throw new MyTaskException("上级领导未找到");
                    }else{
                        taskService.setAssignee(task.getId(),assignee);
                       /* KiteApiCallUtils.sendWxMsg(task);
                        KiteApiCallUtils.sendEmail(task);*/
                    }
                }else{
                    throw new MyTaskException("上级领导未找到");
                }
            }else if(task!=null && "deptleader".equalsIgnoreCase(task.getAssignee())){
                String assignee = KiteApiCallUtils.getDeptLeader(processInstance.getStartUserId());
                if(StringUtils.isNotEmpty(assignee)){
                    if(assignee.contains("-1")){
                        throw new MyTaskException("部门领导未找到");
                    }else{
                        taskService.setAssignee(task.getId(),assignee);
                       /* KiteApiCallUtils.sendWxMsg(task);
                        KiteApiCallUtils.sendEmail(task);*/
                       /* if(assignee.equals(currentUser.getId())){
                            Map<String, Object> variables = new HashMap<>();
                            variables.put("applyResult","同意");
                            variables.put("applyRemarks","通过");
                            taskService.complete(task.getId(),variables);
                        }*/
                    }
                }else{
                    throw new MyTaskException("部门领导未找到");
                }
            }
        }
    }
}
