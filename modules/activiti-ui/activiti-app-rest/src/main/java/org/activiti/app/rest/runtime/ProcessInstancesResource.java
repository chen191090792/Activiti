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

import org.activiti.app.model.runtime.CreateProcessInstanceRepresentation;
import org.activiti.app.model.runtime.ProcessInstanceRepresentation;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProcessInstancesResource extends AbstractProcessInstancesResource {

	@RequestMapping(value = "/rest/process-instances", method = RequestMethod.POST)
	public ProcessInstanceRepresentation startNewProcessInstance(@RequestBody CreateProcessInstanceRepresentation startRequest) {
		ProcessInstanceRepresentation processInstanceRepresentation = null;
		try {
			processInstanceRepresentation = super.startNewProcessInstance(startRequest);
			processInstanceRepresentation.setErrorCode(0);
			processInstanceRepresentation.setErrorMsg("发起成功！");
		} catch (Exception e) {
			processInstanceRepresentation = new ProcessInstanceRepresentation();
			processInstanceRepresentation.setErrorCode(-1);
			processInstanceRepresentation.setErrorMsg("流程发出失败："+e.getMessage());
			e.printStackTrace();
		}
		return processInstanceRepresentation;
	}
}
