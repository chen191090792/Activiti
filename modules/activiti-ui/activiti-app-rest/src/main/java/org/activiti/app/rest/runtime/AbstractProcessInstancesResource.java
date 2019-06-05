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
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import org.activiti.app.domain.editor.Model;
import org.activiti.app.domain.runtime.RelatedContent;
import org.activiti.app.model.component.SimpleContentTypeMapper;
import org.activiti.app.model.runtime.CreateProcessInstanceRepresentation;
import org.activiti.app.model.runtime.ProcessInstanceRepresentation;
import org.activiti.app.model.runtime.RelatedContentRepresentation;
import org.activiti.app.security.SecurityUtils;
import org.activiti.app.service.api.ModelService;
import org.activiti.app.service.api.UserCache;
import org.activiti.app.service.api.UserCache.CachedUser;
import org.activiti.app.service.exception.BadRequestException;
import org.activiti.app.service.runtime.ActivitiService;
import org.activiti.app.service.runtime.PermissionService;
import org.activiti.app.service.runtime.RelatedContentService;
import org.activiti.bpmn.model.*;
import org.activiti.bpmn.model.Process;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import redis.clients.jedis.HostAndPort;
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
  protected ObjectMapper objectMapper;
  @Autowired
  private TaskService taskService;
  @Autowired
  private ModelService modelService;
  private RestTemplate restTemplate = new RestTemplate();

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
     // List<String> startElementOutFlowIds = Lists.newArrayList();
      if (startElement instanceof StartEvent) {
        StartEvent startEvent = (StartEvent) startElement;
        List<SequenceFlow> outgoingFlows = startEvent.getOutgoingFlows();
        if (StringUtils.isNotEmpty(startEvent.getFormKey())) {
          //startElementOutFlowIds = outgoingFlows.stream().map(SequenceFlow::getId).collect(Collectors.toList());
          formDefinition = formRepositoryService.getFormDefinitionByKey(startEvent.getFormKey());
          if (formDefinition != null) {
            variables = formService.getVariablesFromFormSubmission(formDefinition, startRequest.getValues(), startRequest.getOutcome());
          }
        }
      }
      /*Collection<FlowElement> flowElements = process.getFlowElements();
      Collection<FlowElement> elements = process.getFlowElements();
      List<String> targetIds = Lists.newArrayList();
      for (FlowElement flowElement :elements) {
        if(flowElement instanceof SequenceFlow){
          SequenceFlow sequenceFlow = (SequenceFlow)flowElement;
          for (String outFlowId:startElementOutFlowIds){
            if(outFlowId.equalsIgnoreCase(sequenceFlow.getId())){
              targetIds.add(sequenceFlow.getTargetRef());
            }
          }
        }
      }
      for (FlowElement flowElement :flowElements) {
        if(flowElement instanceof UserTask){
          UserTask userTask = (UserTask)flowElement;
          String assignee = userTask.getAssignee();
          if("leader".equalsIgnoreCase(assignee) && targetIds.contains(userTask.getId())){
              userTask.setAssignee(getAssignee());
          }
        }
      }*/
    }
    ProcessInstance processInstance = activitiService.startProcessInstance(startRequest.getProcessDefinitionId(), variables, startRequest.getName());

    // Mark any content created as part of the form-submission connected to the process instance
    /*if (formSubmission != null) {
      if (formSubmission.hasContent()) {
        ObjectNode contentNode = objectMapper.createObjectNode();
        submittedFormValuesJson.put("content", contentNode);
        for (Entry<String, List<RelatedContent>> entry : formSubmission.getVariableContent().entrySet()) {
          ArrayNode contentArray = objectMapper.createArrayNode();
          for (RelatedContent content : entry.getValue()) {
            relatedContentService.setContentField(content.getId(), entry.getKey(), processInstance.getId(), null);
            contentArray.add(content.getId());
          }
          contentNode.put(entry.getKey(), contentArray);
        }
      }*/

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
    return new ProcessInstanceRepresentation(historicProcess, processDefinition, ((ProcessDefinitionEntity) processDefinition).isGraphicalNotationDefined(), user);

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

  public void changeAssignee(String processInstanceId){
//    Task task = taskService.createTaskQuery().processInstanceId(processInstanceId).list();
    List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstanceId).list();
//    if(task!=null && "leader".equalsIgnoreCase(task.getAssignee())){
//      taskService.setAssignee(task.getId(),getAssignee());
//    }
//    if(task!=null && "分管领导".equals(task.getName())){
//      List<Task> tasks = taskService.createTaskQuery().taskAssignee("13543452355").list();
      String [] person = {"15200706014","13543452355","15915810133"};
      for (int i=0;i<tasks.size();i++){
        System.out.print("tasks-------------->"+tasks.get(i).getId());
        taskService.createTaskQuery().processInstanceId(tasks.get(i).getId()).taskAssignee(person[i]).singleResult();
//        taskService.setAssignee(tasks.get(i).getId(),person[i]);
      //  taskService.complete(tasks.get(i).getId());
      }
//    }
  }





  public String getAssignee(){
    User currentUser = SecurityUtils.getCurrentUserObject();
    String url = String.format("http://localhost:8080/api/user/getUpClassInfo/%s",currentUser.getId());
    HttpHeaders headers = new HttpHeaders();
    MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
    headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
    headers.setContentType(type);
    HttpEntity entity = new HttpEntity<>(null, headers);
    HttpEntity<String> result = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
    return  result.getBody();
  }
}
