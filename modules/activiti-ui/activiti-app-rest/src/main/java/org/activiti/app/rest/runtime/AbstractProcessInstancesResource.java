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

import java.util.*;

import org.activiti.app.domain.runtime.RelatedContent;
import org.activiti.app.model.component.SimpleContentTypeMapper;
import org.activiti.app.model.runtime.CompleteFormRepresentation;
import org.activiti.app.model.runtime.CreateProcessInstanceRepresentation;
import org.activiti.app.model.runtime.ProcessInstanceRepresentation;
import org.activiti.app.model.runtime.RelatedContentRepresentation;
import org.activiti.app.security.SecurityUtils;
import org.activiti.app.service.api.ModelService;
import org.activiti.app.service.api.UserCache;
import org.activiti.app.service.api.UserCache.CachedUser;
import org.activiti.app.service.editor.ActivitiTaskFormService;
import org.activiti.app.service.exception.BadRequestException;
import org.activiti.app.service.runtime.ActivitiService;
import org.activiti.app.service.runtime.PermissionService;
import org.activiti.app.service.runtime.ProcessInstanceService;
import org.activiti.app.service.runtime.RelatedContentService;
import org.activiti.app.service.util.JedisUtils;
import org.activiti.bpmn.model.*;
import org.activiti.bpmn.model.Process;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.identity.User;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.form.api.FormRepositoryService;
import org.activiti.form.api.FormService;
import org.activiti.form.model.FormDefinition;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestTemplate;
import redis.clients.jedis.JedisCluster;

public abstract class AbstractProcessInstancesResource {

  @Autowired
  protected ActivitiService activitiService;

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
  protected RelatedContentService relatedContentService;

  @Autowired
  protected SimpleContentTypeMapper typeMapper;

  @Autowired
  protected UserCache userCache;
  @Autowired
  private TaskService taskService;
  @Autowired
  protected ObjectMapper objectMapper;
  @Autowired
  private ActivitiTaskFormService activitiTaskFormService;


  public ProcessInstanceRepresentation startNewProcessInstance(CreateProcessInstanceRepresentation startRequest) {
    if (StringUtils.isEmpty(startRequest.getProcessDefinitionId())) {
      throw new BadRequestException("Process definition id is required");
    }
    
    FormDefinition formDefinition = null;
    Map<String, Object> variables = null;

    ProcessDefinition processDefinition = permissionService.getProcessDefinitionById(startRequest.getProcessDefinitionId());
    if (startRequest.getValues() != null || startRequest.getOutcome() != null) {
      BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
      Process process = bpmnModel.getProcessById(processDefinition.getKey());
      FlowElement startElement = process.getInitialFlowElement();
      if (startElement instanceof StartEvent) {
        StartEvent startEvent = (StartEvent) startElement;
        if (StringUtils.isNotEmpty(startEvent.getFormKey())) {
          formDefinition = formRepositoryService.getFormDefinitionByKey(startEvent.getFormKey());
          if (formDefinition != null) {
            variables = formService.getVariablesFromFormSubmission(formDefinition, startRequest.getValues(), startRequest.getOutcome());
          }
        }
      }
    }


    /** 2019-06-14 自定义会签字段*/
    if(startRequest.getAssigneeList()!=null && startRequest.getAssigneeList().size()>0 && StringUtils.isNotEmpty(startRequest.getAssigneeKey())){
      variables.put(startRequest.getAssigneeKey(), startRequest.getAssigneeList());
    }
    ProcessInstance processInstance = activitiService.startProcessInstance(startRequest.getProcessDefinitionId(), variables, startRequest.getName());
    HistoricProcessInstance historicProcess = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
    if (formDefinition != null) {
      formService.storeSubmittedForm(variables, formDefinition, null, historicProcess.getId());
    }
    
    User user = null;
    if (historicProcess.getStartUserId() != null) {
      CachedUser cachedUser = userCache.getUser(historicProcess.getStartUserId());
      if (cachedUser != null && cachedUser.getUser() != null) {
        user = cachedUser.getUser();
      }
    }
    ProcessInstanceRepresentation processInstanceRepresentation = new ProcessInstanceRepresentation(historicProcess, processDefinition, ((ProcessDefinitionEntity) processDefinition).isGraphicalNotationDefined(), user);

    JedisCluster jedisCluser = JedisUtils.getJedisCluser();
    String jump = jedisCluser.get(processDefinition.getId());
    if(StringUtils.equalsIgnoreCase("是",jump)){
      completeTaskForm(processInstance.getId());
    }
    return processInstanceRepresentation;

  }
  protected void completeTaskForm(String processInstanceId){
    User currentUser = SecurityUtils.getCurrentUserObject();
    List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstanceId).listPage(0, 1000000);
    for(Task task:tasks){
      if(task.getAssignee().equals(currentUser.getId())){
        FormDefinition form = activitiTaskFormService.getTaskForm(task.getId());
        CompleteFormRepresentation completeFormRepresentation  = new CompleteFormRepresentation();
        completeFormRepresentation.setFormId(form.getId());
        Map<String, Object> values = new HashMap<>();
        values.put("applyResult","同意");
        values.put("applyRemarks","通过");
        completeFormRepresentation.setValues(values);
        activitiTaskFormService.completeTaskForm(task.getId(),completeFormRepresentation);
      }
    }
  }




  protected Map<String, List<RelatedContent>> groupContentByField(Page<RelatedContent> allContent) {
    HashMap<String, List<RelatedContent>> result = new HashMap<String, List<RelatedContent>>();
    List<RelatedContent> list;
    for (RelatedContent content : allContent.getContent()) {
      list = result.get(content.getField());
      if (list == null) {
        list = new ArrayList<RelatedContent>();
        result.put(content.getField(), list);
      }
      list.add(content);
    }
    return result;
  }

  protected RelatedContentRepresentation createRelatedContentResponse(RelatedContent relatedContent) {
    RelatedContentRepresentation relatedContentResponse = new RelatedContentRepresentation(relatedContent, typeMapper);
    return relatedContentResponse;
  }
}
