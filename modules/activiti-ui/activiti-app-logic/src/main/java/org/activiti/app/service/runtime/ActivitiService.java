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
import org.activiti.app.service.util.TaskAssigneeSetUtils;
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

    /**
     *
     * @param processDefinitionId
     * @param variables
     * @param processInstanceName
     * @param assignee  指定人员
     * @return
     */
    public ProcessInstance startProcessInstance(String processDefinitionId, Map<String, Object> variables, String processInstanceName,String assignee) {
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinitionId, variables);
        if (!processInstance.isEnded() && processInstanceName != null) {
            runtimeService.setProcessInstanceName(processInstance.getId(), processInstanceName);
        }
        changeAssignee(processInstance.getId(),assignee);
        return processInstance;
	}

//    /**
//     * 指定人员
//     * @param processDefinitionId
//     * @param variables
//     * @param processInstanceName
//     * @return
//     */
//    public ProcessInstance startProcessInstanceForPerson(String processDefinitionId, Map<String, Object> variables, String processInstanceName) {
//        ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinitionId, variables);
//        if (!processInstance.isEnded() && processInstanceName != null) {
//            runtimeService.setProcessInstanceName(processInstance.getId(), processInstanceName);
//        }
//        changeAssignee(processInstance.getId());
//        return processInstance;
//    }

    public void changeAssignee(String processInstanceId, String assignee){
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        if(StringUtils.isNotEmpty(assignee)){
            List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstanceId).listPage(0, 1000000);
            TaskAssigneeSetUtils.setAssignee(tasks,processInstance,assignee,taskService);
        }
    }



}
