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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.activiti.app.model.common.ResultListDataRepresentation;
import org.activiti.app.model.idm.UserRepresentation;
import org.activiti.app.model.runtime.TaskRepresentation;
import org.activiti.app.rest.entity.CustomProcessDiagramGenerator;
import org.activiti.app.security.SecurityUtils;
import org.activiti.app.service.api.UserCache;
import org.activiti.app.service.api.UserCache.CachedUser;
import org.activiti.app.service.exception.BadRequestException;
import org.activiti.app.service.exception.NotPermittedException;
import org.activiti.app.service.runtime.PermissionService;
import org.activiti.app.service.util.JedisUtils;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowNode;
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.editor.language.json.converter.util.CollectionUtils;
import org.activiti.engine.HistoryService;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricTaskInstanceQuery;
import org.activiti.engine.identity.User;
import org.activiti.image.ProcessDiagramGenerator;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import redis.clients.jedis.JedisCluster;
import sun.misc.BASE64Encoder;

import javax.inject.Inject;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.io.IOUtils.copyLarge;

@RestController
public class HistoricTaskQueryResource {

  @Inject
  protected HistoryService historyService;

  @Inject
  protected UserCache userCache;

  @Inject
  protected PermissionService permissionService;
  @Inject
  private ProcessEngineConfiguration processEngineConfiguration;
  @Inject
  private RepositoryService repositoryService;

  private Logger logger = LoggerFactory.getLogger(HistoricTaskQueryResource.class);

  private static final String CHARSET_NAME = "UTF-8";

  @RequestMapping(value = "/rest/query/history/tasks", method = RequestMethod.POST, produces = "application/json")
  public ResultListDataRepresentation listTasks(@RequestBody ObjectNode requestNode) {
    if (requestNode == null) {
      throw new BadRequestException("No request found");
    }

    HistoricTaskInstanceQuery taskQuery = historyService.createHistoricTaskInstanceQuery();

    User currentUser = SecurityUtils.getCurrentUserObject();

    JsonNode processInstanceIdNode = requestNode.get("processInstanceId");
    if (processInstanceIdNode != null && processInstanceIdNode.isNull() == false) {
      String processInstanceId = processInstanceIdNode.asText();
      if (permissionService.hasReadPermissionOnProcessInstance(currentUser, processInstanceId)) {
        taskQuery.processInstanceId(processInstanceId);
      } else {
        throw new NotPermittedException();
      }
    }

    JsonNode finishedNode = requestNode.get("finished");
    if (finishedNode != null && finishedNode.isNull() == false) {
      boolean isFinished = finishedNode.asBoolean();
      if (isFinished) {
        taskQuery.finished();
      } else {
        taskQuery.unfinished();
      }
    }

    List<HistoricTaskInstance> tasks = taskQuery.list();

    // get all users to have the user object available in the task on the client side
    ResultListDataRepresentation result = new ResultListDataRepresentation(convertTaskInfoList(tasks));
    return result;
  }

  protected List<TaskRepresentation> convertTaskInfoList(List<HistoricTaskInstance> tasks) {
    List<TaskRepresentation> result = new ArrayList<TaskRepresentation>();
    if (CollectionUtils.isNotEmpty(tasks)) {
      TaskRepresentation representation = null;
      for (HistoricTaskInstance task : tasks) {
        representation = new TaskRepresentation(task);

        CachedUser cachedUser = userCache.getUser(task.getAssignee());
        if (cachedUser != null && cachedUser.getUser() != null) {
          representation.setAssignee(new UserRepresentation(cachedUser.getUser()));
        }

        result.add(representation);
      }
    }
    return result;
  }

  /**
   * 根据流程实例Id,获取实时流程图片
   *
   * @param processInstanceId
   * @param outputStream
   * @return
   */
 /* @RequestMapping(value = "/rest/query/history/tasks/{processInstanceId}", method = RequestMethod.GET)
  public void getFlowImgByInstanceId(@PathVariable String processInstanceId, OutputStream outputStream) {
    try {
      if (StringUtils.isEmpty(processInstanceId)) {
        return;
      }
      // 获取历史流程实例
      HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
      // 获取流程中已经执行的节点，按照执行先后顺序排序
      List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstanceId)
              .orderByHistoricActivityInstanceId().asc().list();
      // 高亮已经执行流程节点ID集合
      List<String> highLightedActivitiIds = new ArrayList<>();
      for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
        highLightedActivitiIds.add(historicActivityInstance.getActivityId());
      }

      List<HistoricProcessInstance> historicFinishedProcessInstances = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).finished()
              .list();
      ProcessDiagramGenerator processDiagramGenerator = null;
      // 如果还没完成，流程图高亮颜色为绿色，如果已经完成为红色
      if (!CollectionUtils.isEmpty(historicFinishedProcessInstances)) {
        // 如果不为空，说明已经完成
        processDiagramGenerator = processEngineConfiguration.getProcessDiagramGenerator();
      } else {
        processDiagramGenerator = new CustomProcessDiagramGenerator();
      }

      BpmnModel bpmnModel = repositoryService.getBpmnModel(historicProcessInstance.getProcessDefinitionId());
      // 高亮流程已发生流转的线id集合
      List<String> highLightedFlowIds = getHighLightedFlows(bpmnModel, historicActivityInstances);

      // 使用默认配置获得流程图表生成器，并生成追踪图片字符流
      InputStream imageStream = processDiagramGenerator.generateDiagram(bpmnModel, "png", highLightedActivitiIds, highLightedFlowIds, "宋体", "微软雅黑", "黑体", null, 2.0);

      // 输出图片内容
      byte[] b = new byte[1024];
      int len;
      while ((len = imageStream.read(b, 0, 1024)) != -1) {
        outputStream.write(b, 0, len);
      }
    } catch (Exception e) {
      logger.error("processInstanceId" + processInstanceId + "生成流程图失败，原因：" + e.getMessage(), e);
    }
  }
*/
  /**
   * 获取已经流转的线
   *
   * @param bpmnModel
   * @param historicActivityInstances
   * @return
   */
  private static List<String> getHighLightedFlows(BpmnModel bpmnModel, List<HistoricActivityInstance> historicActivityInstances) {
    // 高亮流程已发生流转的线id集合
    List<String> highLightedFlowIds = new ArrayList<>();
    // 全部活动节点
    List<FlowNode> historicActivityNodes = new ArrayList<>();
    // 已完成的历史活动节点
    List<HistoricActivityInstance> finishedActivityInstances = new ArrayList<>();

    for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
      FlowNode flowNode = (FlowNode) bpmnModel.getMainProcess().getFlowElement(historicActivityInstance.getActivityId(), true);
      historicActivityNodes.add(flowNode);
      if (historicActivityInstance.getEndTime() != null) {
        finishedActivityInstances.add(historicActivityInstance);
      }
    }

    FlowNode currentFlowNode = null;
    FlowNode targetFlowNode = null;
    // 遍历已完成的活动实例，从每个实例的outgoingFlows中找到已执行的
    for (HistoricActivityInstance currentActivityInstance : finishedActivityInstances) {
      // 获得当前活动对应的节点信息及outgoingFlows信息
      currentFlowNode = (FlowNode) bpmnModel.getMainProcess().getFlowElement(currentActivityInstance.getActivityId(), true);
      List<SequenceFlow> sequenceFlows = currentFlowNode.getOutgoingFlows();

      /**
       * 遍历outgoingFlows并找到已已流转的 满足如下条件认为已已流转： 1.当前节点是并行网关或兼容网关，则通过outgoingFlows能够在历史活动中找到的全部节点均为已流转 2.当前节点是以上两种类型之外的，通过outgoingFlows查找到的时间最早的流转节点视为有效流转
       */
      if ("parallelGateway".equals(currentActivityInstance.getActivityType()) || "inclusiveGateway".equals(currentActivityInstance.getActivityType())) {
        // 遍历历史活动节点，找到匹配流程目标节点的
        for (SequenceFlow sequenceFlow : sequenceFlows) {
          targetFlowNode = (FlowNode) bpmnModel.getMainProcess().getFlowElement(sequenceFlow.getTargetRef(), true);
          if (historicActivityNodes.contains(targetFlowNode)) {
            highLightedFlowIds.add(targetFlowNode.getId());
          }
        }
      } else {
        List<Map<String, Object>> tempMapList = new ArrayList<>();
        for (SequenceFlow sequenceFlow : sequenceFlows) {
          for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
            if (historicActivityInstance.getActivityId().equals(sequenceFlow.getTargetRef())) {
              Map<String, Object> map = new HashMap<>();
              map.put("highLightedFlowId", sequenceFlow.getId());
              map.put("highLightedFlowStartTime", historicActivityInstance.getStartTime().getTime());
              tempMapList.add(map);
            }
          }
        }

        if (!CollectionUtils.isEmpty(tempMapList)) {
          // 遍历匹配的集合，取得开始时间最早的一个
          long earliestStamp = 0L;
          String highLightedFlowId = null;
          for (Map<String, Object> map : tempMapList) {
            long highLightedFlowStartTime = Long.valueOf(map.get("highLightedFlowStartTime").toString());
            if (earliestStamp == 0 || earliestStamp >= highLightedFlowStartTime) {
              highLightedFlowId = map.get("highLightedFlowId").toString();
              earliestStamp = highLightedFlowStartTime;
            }
          }

          highLightedFlowIds.add(highLightedFlowId);
        }

      }

    }
    return highLightedFlowIds;
  }


  /**
   * 根据流程实例Id,获取实时流程图片
   *
   * @param processInstanceId
   * @param
   * @return
   */
  @RequestMapping(value = "/rest/query/history/tasks/{processInstanceId}", method = RequestMethod.GET)
  public void getFlowImgByInstanceIdInputStream(@PathVariable String processInstanceId) {
    try {
      if (StringUtils.isEmpty(processInstanceId)) {
        return ;
      }
      // 获取历史流程实例
      HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
      // 获取流程中已经执行的节点，按照执行先后顺序排序
      List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstanceId)
              .orderByHistoricActivityInstanceId().asc().list();
      // 高亮已经执行流程节点ID集合
      List<String> highLightedActivitiIds = new ArrayList<>();
      for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
        highLightedActivitiIds.add(historicActivityInstance.getActivityId());
      }

      List<HistoricProcessInstance> historicFinishedProcessInstances = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).finished()
              .list();
      ProcessDiagramGenerator processDiagramGenerator = null;
      // 如果还没完成，流程图高亮颜色为绿色，如果已经完成为红色
      if (!CollectionUtils.isEmpty(historicFinishedProcessInstances)) {
        // 如果不为空，说明已经完成
        processDiagramGenerator = processEngineConfiguration.getProcessDiagramGenerator();
      } else {
        processDiagramGenerator = new CustomProcessDiagramGenerator();
      }

      BpmnModel bpmnModel = repositoryService.getBpmnModel(historicProcessInstance.getProcessDefinitionId());
      // 高亮流程已发生流转的线id集合
      List<String> highLightedFlowIds = getHighLightedFlows(bpmnModel, historicActivityInstances);

      // 使用默认配置获得流程图表生成器，并生成追踪图片字符流
      InputStream imageStream = processDiagramGenerator.generateDiagram(bpmnModel, "png", highLightedActivitiIds, highLightedFlowIds, "宋体", "微软雅黑", "黑体", null, 2.0);
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      // 输出图片内容
      byte[] b = new byte[1024];
      int len;
      while ((len = imageStream.read(b, 0, 1024)) != -1) {
        outputStream.write(b, 0, len);
      }
      BASE64Encoder encoder = new BASE64Encoder();
      String imageEncoder = encoder.encode(outputStream.toByteArray());
      JedisCluster jedisCluser = JedisUtils.getJedisCluser();
      jedisCluser.set(processInstanceId,imageEncoder);
    } catch (Exception e) {
      logger.error("processInstanceId" + processInstanceId + "生成流程图失败，原因：" + e.getMessage(), e);
    }
  }
  public static int copy(InputStream input, OutputStream output)throws IOException{
    long count = copyLarge(input, output);
    if (count > 2147483647L) {
      return -1;
    }
    return (int)count;
  }
  /**
   * 转换为字节数组
   * @param str
   * @return
   */
  public static byte[] getBytes(String str){
    if (str != null){
      try {
        return str.getBytes(CHARSET_NAME);
      } catch (UnsupportedEncodingException e) {
        return null;
      }
    }else{
      return null;
    }
  }

}
