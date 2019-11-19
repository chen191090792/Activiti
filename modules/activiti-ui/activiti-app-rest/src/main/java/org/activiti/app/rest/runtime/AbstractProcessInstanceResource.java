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
package org.activiti.app.rest.runtime;

import javax.servlet.http.HttpServletResponse;

import org.activiti.app.model.runtime.CompleteFormRepresentation;
import org.activiti.app.model.runtime.ProcessInstanceRepresentation;
import org.activiti.app.security.SecurityUtils;
import org.activiti.app.service.api.UserCache;
import org.activiti.app.service.api.UserCache.CachedUser;
import org.activiti.app.service.editor.ActivitiTaskFormService;
import org.activiti.app.service.exception.NotFoundException;
import org.activiti.app.service.runtime.PermissionService;
import org.activiti.app.service.runtime.ProcessInstanceService;
import org.activiti.app.util.KiteApiCallUtils;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.Process;
import org.activiti.bpmn.model.StartEvent;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.identity.User;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.task.Task;
import org.activiti.form.api.FormRepositoryService;
import org.activiti.form.api.FormService;
import org.activiti.form.model.FormDefinition;
import org.activiti.form.model.FormField;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractProcessInstanceResource {

  private static final Logger logger = LoggerFactory.getLogger(AbstractProcessInstanceResource.class);
  private static final String ROLE_VALUE = "admin";

  @Autowired
  protected RepositoryService repositoryService;

  @Autowired
  protected HistoryService historyService;

  @Autowired
  protected RuntimeService runtimeService;
  
  @Autowired
  protected FormRepositoryService formRepositoryService;
  
  @Autowired
  protected FormService formService;

  @Autowired
  protected PermissionService permissionService;

  @Autowired
  protected ProcessInstanceService processInstanceService;

  @Autowired
  protected UserCache userCache;
  @Autowired
  protected TaskService taskService;
  @Autowired
  ActivitiTaskFormService activitiTaskFormService;

  public ProcessInstanceRepresentation getProcessInstance(String processInstanceId, HttpServletResponse response) {

    HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();

    if (!permissionService.hasReadPermissionOnProcessInstance(SecurityUtils.getCurrentUserObject(), processInstance, processInstanceId)) {
      throw new NotFoundException("Process with id: " + processInstanceId + " does not exist or is not available for this user");
    }

    ProcessDefinitionEntity processDefinition = (ProcessDefinitionEntity) repositoryService.getProcessDefinition(processInstance.getProcessDefinitionId());

    User userRep = null;
    if (processInstance.getStartUserId() != null) {
      CachedUser user = userCache.getUser(processInstance.getStartUserId());
      if (user != null && user.getUser() != null) {
        userRep = user.getUser();
      }
    }

    ProcessInstanceRepresentation processInstanceResult = new ProcessInstanceRepresentation(processInstance, processDefinition, processDefinition.isGraphicalNotationDefined(), userRep);

    FormDefinition formDefinition = getStartFormDefinition(processInstance.getProcessDefinitionId(), processDefinition, processInstance.getId());
    if (formDefinition != null) {
      processInstanceResult.setStartFormDefined(true);
    }

    return processInstanceResult;
  }

  public ProcessInstanceRepresentation getMyProcessInstance(String processInstanceId,String role, HttpServletResponse response) {

    HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();

    if(!ROLE_VALUE.equals(role)){
      if (!permissionService.hasReadPermissionOnProcessInstance(SecurityUtils.getCurrentUserObject(), processInstance, processInstanceId)) {
        throw new NotFoundException("Process with id: " + processInstanceId + " does not exist or is not available for this user");
      }
    }
    ProcessDefinitionEntity processDefinition = (ProcessDefinitionEntity) repositoryService.getProcessDefinition(processInstance.getProcessDefinitionId());

    User userRep = null;
    if (processInstance.getStartUserId() != null) {
      CachedUser user = userCache.getUser(processInstance.getStartUserId());
      if (user != null && user.getUser() != null) {
        userRep = user.getUser();
      }
    }

    ProcessInstanceRepresentation processInstanceResult = new ProcessInstanceRepresentation(processInstance, processDefinition, processDefinition.isGraphicalNotationDefined(), userRep);

    FormDefinition formDefinition = getStartFormDefinition(processInstance.getProcessDefinitionId(), processDefinition, processInstance.getId());
    if (formDefinition != null) {
      processInstanceResult.setStartFormDefined(true);
      //processInstanceResult.setFlowBelong(formDefinition.getFields().get());
    }

    return processInstanceResult;
  }

  public FormDefinition getProcessInstanceStartForm(String processInstanceId, HttpServletResponse response) {

    HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
    boolean admin = KiteApiCallUtils.checkAdmin();
    if (!admin && !permissionService.hasReadPermissionOnProcessInstance(SecurityUtils.getCurrentUserObject(), processInstance, processInstanceId)) {
      throw new NotFoundException("Process with id: " + processInstanceId + " does not exist or is not available for this user");
    }
    
    ProcessDefinitionEntity processDefinition = (ProcessDefinitionEntity) repositoryService.getProcessDefinition(processInstance.getProcessDefinitionId());

    return getStartFormDefinition(processInstance.getProcessDefinitionId(), processDefinition, processInstance.getId());
  }
  //flag确定admin还是普通用户
  public void deleteProcessInstance(String processInstanceId,String flag) {

    if("admin".equals(flag)){
        completeForm(processInstanceId,"管理员终止流程");
     // runtimeService.deleteProcessInstance(processInstanceId, "Cancelled by " + SecurityUtils.getCurrentUserId());
    }else{
          User currentUser = SecurityUtils.getCurrentUserObject();
          HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery()
                  .processInstanceId(processInstanceId)
                  .startedBy(String.valueOf(currentUser.getId())) // Permission
                  .singleResult();
          if (processInstance == null) {
            throw new NotFoundException("Process with id: " + processInstanceId + " does not exist or is not started by this user");
          }
          if (processInstance.getEndTime() != null) {
            // Check if a hard delete of process instance is allowed
                if (!permissionService.canDeleteProcessInstance(currentUser, processInstance)) {
                  throw new NotFoundException("Process with id: " + processInstanceId + " is already completed and can't be deleted");
                }

            // Delete cascade behavior in separate service to share a single transaction for all nested service-calls
                processInstanceService.deleteProcessInstance(processInstanceId);

          } else {
            completeForm(processInstanceId,"流程发起人终止流程");
          }
    }
  }

  public void completeForm(String processInstanceId,String remarks){
    List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstanceId).listPage(0, 1000000);
    for(Task task:tasks){
      FormDefinition form = activitiTaskFormService.getTaskForm(task.getId());
      if(form!=null){
        CompleteFormRepresentation completeFormRepresentation  = new CompleteFormRepresentation();
        completeFormRepresentation.setFormId(form.getId());
        Map<String, Object> values = new HashMap<>();
        List<FormField> fields = form.getFields();
        for(FormField formField:fields){
          if(formField.isRequired()){
            values.put(formField.getId(),0);
          }
          if("applyResult".equals(formField.getId())){
            values.put("applyResult","不同意");
          }
          if("applyRemarks".equals(formField.getId())){
            values.put("applyRemarks",remarks);
          }
        }
        completeFormRepresentation.setValues(values);
        myCompleteTaskForm(task.getId(),completeFormRepresentation);
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

  protected FormDefinition getStartFormDefinition(String processDefinitionId, ProcessDefinitionEntity processDefinition, String processInstanceId) {
    FormDefinition formDefinition = null;
    BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
    Process process = bpmnModel.getProcessById(processDefinition.getKey());
    FlowElement startElement = process.getInitialFlowElement();
    if (startElement instanceof StartEvent) {
      StartEvent startEvent = (StartEvent) startElement;
      if (StringUtils.isNotEmpty(startEvent.getFormKey())) {
        formDefinition = formService.getCompletedTaskFormDefinitionByKeyAndParentDeploymentId(startEvent.getFormKey(), 
            processDefinition.getDeploymentId(), null, processInstanceId, null, processDefinition.getTenantId());
      }
    }
    
    return formDefinition;
  }

}
