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

import java.util.List;

import org.activiti.app.model.runtime.CompleteFormRepresentation;
import org.activiti.app.model.runtime.ProcessInstanceVariableRepresentation;
import org.activiti.app.rest.entity.ResultMsg;
import org.activiti.app.service.editor.ActivitiTaskFormService;
import org.activiti.engine.TaskService;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.activiti.form.model.FormDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Joram Barrez
 */
@RestController
@RequestMapping("/rest/task-forms")
public class TaskFormResource {
  
  @Autowired
  protected ActivitiTaskFormService taskFormService;
  @Autowired
  private TaskService taskService;

  @RequestMapping(value = "/{taskId}", method = RequestMethod.GET, produces = "application/json")
  public FormDefinition getTaskForm(@PathVariable String taskId) {
    return taskFormService.getTaskForm(taskId);
  }

  @ResponseStatus(value = HttpStatus.OK)
  @RequestMapping(value = "/{taskId}", method = RequestMethod.POST, produces = "application/json")
  public void completeTaskForm(@PathVariable String taskId, @RequestBody CompleteFormRepresentation completeTaskFormRepresentation) {
    Task task = (Task)((TaskQuery)this.taskService.createTaskQuery().taskId(taskId)).singleResult();
    taskFormService.completeTaskForm(taskId, completeTaskFormRepresentation);
    taskFormService.changeAssignee(task.getExecutionId(),task.getProcessInstanceId(),completeTaskFormRepresentation.getAssignment());
  }

  @ResponseStatus(value = HttpStatus.OK)
  @RequestMapping(value = "/my/{taskId}", method = RequestMethod.POST, produces = "application/json")
  public ResultMsg completeMyTaskForm(@PathVariable String taskId, @RequestBody CompleteFormRepresentation completeTaskFormRepresentation) {
    ResultMsg msg = new ResultMsg();
    try {
      taskFormService.completeTaskForm(taskId, completeTaskFormRepresentation);
      msg.setErrorCode(0);
      msg.setErrorMsg("执行成功！");
    } catch (Exception e) {
      msg.setErrorCode(-1);
      msg.setErrorMsg("流程发出失败："+e.getMessage());
      e.printStackTrace();
    }
    return msg;
  }

  @RequestMapping(value = "/{taskId}/variables", method = RequestMethod.GET, produces = "application/json")
  public List<ProcessInstanceVariableRepresentation> getProcessInstanceVariables(@PathVariable String taskId) {
    return taskFormService.getProcessInstanceVariables(taskId);
  }



}
