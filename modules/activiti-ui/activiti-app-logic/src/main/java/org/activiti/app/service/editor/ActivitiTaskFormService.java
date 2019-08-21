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
package org.activiti.app.service.editor;

import java.util.*;

import org.activiti.app.model.runtime.CompleteFormRepresentation;
import org.activiti.app.model.runtime.ProcessInstanceVariableRepresentation;
import org.activiti.app.security.SecurityUtils;
import org.activiti.app.service.exception.MyTaskException;
import org.activiti.app.service.exception.NotFoundException;
import org.activiti.app.service.exception.NotPermittedException;
import org.activiti.app.service.runtime.PermissionService;
import org.activiti.app.service.util.JedisUtils;
import org.activiti.app.service.util.TaskAssigneeSetUtils;
import org.activiti.app.util.KiteApiCallUtils;
import org.activiti.engine.*;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricVariableInstance;
import org.activiti.engine.identity.User;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.form.api.FormRepositoryService;
import org.activiti.form.api.FormService;
import org.activiti.form.model.FormDefinition;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import redis.clients.jedis.JedisCluster;

/**
 * @author Tijs Rademakers
 */
@Service
public class ActivitiTaskFormService {

  private static final Logger logger = LoggerFactory.getLogger(ActivitiTaskFormService.class);

  @Autowired
  protected TaskService taskService;
  
  @Autowired
  protected RepositoryService repositoryService;
  
  @Autowired
  protected HistoryService historyService;

  @Autowired
  protected FormRepositoryService formRepositoryService;
  
  @Autowired
  protected FormService formService;

  @Autowired
  protected PermissionService permissionService;

  @Autowired
  protected ObjectMapper objectMapper;
  @Autowired
  private RuntimeService runtimeService;

  private RestTemplate restTemplate = new RestTemplate();

  public FormDefinition getTaskForm(String taskId) {
    HistoricTaskInstance task = permissionService.validateReadPermissionOnTask(SecurityUtils.getCurrentUserObject(), taskId);
    
    Map<String, Object> variables = new HashMap<String, Object>();
    if (task.getProcessInstanceId() != null) {
      List<HistoricVariableInstance> variableInstances = historyService.createHistoricVariableInstanceQuery()
          .processInstanceId(task.getProcessInstanceId())
          .list();
      
      for (HistoricVariableInstance historicVariableInstance : variableInstances) {
        variables.put(historicVariableInstance.getVariableName(), historicVariableInstance.getValue());
      }
    }
    
    String parentDeploymentId = null;
    if (StringUtils.isNotEmpty(task.getProcessDefinitionId())) {
      try {
        ProcessDefinition processDefinition = repositoryService.getProcessDefinition(task.getProcessDefinitionId());
        parentDeploymentId = processDefinition.getDeploymentId();
        
      } catch (ActivitiException e) {
        logger.error("Error getting process definition " + task.getProcessDefinitionId(), e);
      }
    }
    
    FormDefinition formDefinition = null;
    if (task.getEndTime() != null) {
      formDefinition = formService.getCompletedTaskFormDefinitionByKeyAndParentDeploymentId(task.getFormKey(), parentDeploymentId, 
          taskId, task.getProcessInstanceId(), variables, task.getTenantId());
      
    } else {
      formDefinition = formService.getTaskFormDefinitionByKeyAndParentDeploymentId(task.getFormKey(), parentDeploymentId, 
          task.getProcessInstanceId(), variables, task.getTenantId());
    }

    // If form does not exists, we don't want to leak out this info to just anyone
    if (formDefinition == null) {
      throw new NotFoundException("Form definition for task " + task.getTaskDefinitionKey() + " cannot be found for form key " + task.getFormKey());
    }

    return formDefinition;
  }
  @Transactional
  public void completeTaskForm(String taskId, CompleteFormRepresentation completeTaskFormRepresentation) {

    // Get the form definition
    Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
    if (task == null) {
      throw new NotFoundException("Task not found with id: " + taskId);
    }
    FormDefinition formDefinition = formRepositoryService.getFormDefinitionById(completeTaskFormRepresentation.getFormId());

    User currentUser = SecurityUtils.getCurrentUserObject();

    if (!permissionService.isTaskOwnerOrAssignee(currentUser, taskId)) {
      if (!permissionService.validateIfUserIsInitiatorAndCanCompleteTask(currentUser, task)) {
        throw new NotPermittedException();
      }
    }

    // Extract raw variables and complete the task
    Map<String, Object> variables = formService.getVariablesFromFormSubmission(formDefinition, completeTaskFormRepresentation.getValues(),
        completeTaskFormRepresentation.getOutcome());
    formService.storeSubmittedForm(variables, formDefinition, task.getId(), task.getProcessInstanceId());
    //会签
    if(StringUtils.isNotEmpty(completeTaskFormRepresentation.getAssigneeKey()) && completeTaskFormRepresentation.getAssigneeList()!=null &&  completeTaskFormRepresentation.getAssigneeList().size()>0 ){
      variables.put(completeTaskFormRepresentation.getAssigneeKey(),completeTaskFormRepresentation.getAssigneeList());
    }
    taskService.complete(taskId, variables);
    changeAssignee(task.getExecutionId(),task.getProcessInstanceId(),completeTaskFormRepresentation.getAssignment());
    completeTaskForm(task.getProcessInstanceId());
  }

  protected void completeTaskForm(String processInstanceId){
    User currentUser = SecurityUtils.getCurrentUserObject();
    List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstanceId).listPage(0, 1000000);
    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
    if(processInstance!=null){//判断processInstance不为null执行
      JedisCluster jedisCluser = JedisUtils.getJedisCluser();
      String jump = jedisCluser.get(processInstance.getProcessDefinitionId());
      if(StringUtils.equalsIgnoreCase("是",jump)){
        for(Task task:tasks){
          if(task.getAssignee().equals(currentUser.getId()) || task.getAssignee().equals(processInstance.getStartUserId())){
            FormDefinition form = this.getTaskForm(task.getId());
            CompleteFormRepresentation completeFormRepresentation  = new CompleteFormRepresentation();
            completeFormRepresentation.setFormId(form.getId());
            Map<String, Object> values = new HashMap<>();
            values.put("applyResult","同意");
            values.put("applyRemarks","通过");
            completeFormRepresentation.setValues(values);
            this.myCompleteTaskForm(task.getId(),completeFormRepresentation);
          }
        }
      }
    }
  }


  @Transactional
  public void myCompleteTaskForm(String taskId, CompleteFormRepresentation completeTaskFormRepresentation) {

    // Get the form definition
    Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
    if (task == null) {
      throw new NotFoundException("Task not found with id: " + taskId);
    }
    FormDefinition formDefinition = formRepositoryService.getFormDefinitionById(completeTaskFormRepresentation.getFormId());
    // Extract raw variables and complete the task
    Map<String, Object> variables = formService.getVariablesFromFormSubmission(formDefinition, completeTaskFormRepresentation.getValues(),
            completeTaskFormRepresentation.getOutcome());
    formService.storeSubmittedForm(variables, formDefinition, task.getId(), task.getProcessInstanceId());
    taskService.complete(taskId, variables);
  }


  public List<ProcessInstanceVariableRepresentation> getProcessInstanceVariables(String taskId) {
    HistoricTaskInstance task = permissionService.validateReadPermissionOnTask(SecurityUtils.getCurrentUserObject(), taskId);
    List<HistoricVariableInstance> historicVariables = historyService.createHistoricVariableInstanceQuery().processInstanceId(task.getProcessInstanceId()).list();

    // Get all process-variables to extract values from
    Map<String, ProcessInstanceVariableRepresentation> processInstanceVariables = new HashMap<String, ProcessInstanceVariableRepresentation>();

    for (HistoricVariableInstance historicVariableInstance : historicVariables) {
        ProcessInstanceVariableRepresentation processInstanceVariableRepresentation = new ProcessInstanceVariableRepresentation(
                historicVariableInstance.getVariableName(), historicVariableInstance.getVariableTypeName(), historicVariableInstance.getValue());
        processInstanceVariables.put(historicVariableInstance.getId(), processInstanceVariableRepresentation);
    }

    List<ProcessInstanceVariableRepresentation> processInstanceVariableRepresenations = 
        new ArrayList<ProcessInstanceVariableRepresentation>(processInstanceVariables.values());
    return processInstanceVariableRepresenations;
  }

  public void changeAssignee(String executionId, String processId,String assignment) {
    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processId).singleResult();
    List<Task> tasks = taskService.createTaskQuery().executionId(executionId).processInstanceId(processId).listPage(0,100000);
    TaskAssigneeSetUtils.setAssignee(tasks,processInstance,assignment,taskService);
  }


}
