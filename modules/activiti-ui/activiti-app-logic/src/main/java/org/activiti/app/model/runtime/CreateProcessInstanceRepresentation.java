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
package org.activiti.app.model.runtime;

import java.util.List;

public class CreateProcessInstanceRepresentation extends CompleteFormRepresentation {
    private String processDefinitionId;
    private String name;
    private List<String> assigneeList;
    private String assigneeKey;
    private String assignee;
    //流程发起人标识
    private String startBy;

    public String getStartBy() {
        return startBy;
    }

    public void setStartBy(String startBy) {
        this.startBy = startBy;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public void setName(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }
    
    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }
    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public List<String> getAssigneeList() {
        return assigneeList;
    }

    public void setAssigneeList(List<String> assigneeList) {
        this.assigneeList = assigneeList;
    }

    public String getAssigneeKey() {
        return assigneeKey;
    }

    public void setAssigneeKey(String assigneeKey) {
        this.assigneeKey = assigneeKey;
    }
}
