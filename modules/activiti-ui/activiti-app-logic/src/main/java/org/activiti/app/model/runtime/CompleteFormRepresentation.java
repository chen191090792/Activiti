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
import java.util.Map;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class CompleteFormRepresentation {
	
  protected String formId;
  protected Map<String, Object> values;
  protected String outcome;
  private String assignment;
  private String assigneeKey;
  private List<String> assigneeList;

	public String getAssigneeKey() {
		return assigneeKey;
	}
	public void setAssigneeKey(String assigneeKey) {
		this.assigneeKey = assigneeKey;
	}

	public List<String> getAssigneeList() {
		return assigneeList;
	}

	public void setAssigneeList(List<String> assigneeList) {
		this.assigneeList = assigneeList;
	}

	public String getFormId() {
    return formId;
  }

  public void setFormId(String formId) {
    this.formId = formId;
  }

  public Map<String, Object> getValues() {
		return values;
	}

	public void setValues(Map<String, Object> values) {
		this.values = values;
	}

	public String getOutcome() {
		return outcome;
	}

	public void setOutcome(String outcome) {
		this.outcome = outcome;
	}

	public String getAssignment() {
		return assignment;
	}

	public void setAssignment(String assignment) {
		this.assignment = assignment;
	}
}
