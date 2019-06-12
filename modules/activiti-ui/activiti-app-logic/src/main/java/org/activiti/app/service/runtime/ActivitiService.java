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

import org.activiti.engine.RuntimeService;
import org.activiti.engine.runtime.ProcessInstance;
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
	
	public ProcessInstance startProcessInstance(String processDefinitionId, Map<String, Object> variables, String processInstanceName) {
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinitionId, variables);
        if (!processInstance.isEnded() && processInstanceName != null) {
            runtimeService.setProcessInstanceName(processInstance.getId(), processInstanceName);
        }
        return processInstance;
        
	}

}
