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
        logger.info("processDefinitionId--------->"+processDefinitionId);
        logger.info("processInstanceName--------->"+processInstanceName);
        logger.info("variables--------->"+variables);
        /*******************************个人任务测试代码start****************************************/
        variables = new HashMap<String, Object>();
        List<String> usr = new ArrayList<String>();
        usr.add("15200706014");
        usr.add("15915810133");
        usr.add("13543452355");
        variables.put("assigneeList",usr);
       // ProcessInstance processInstance =  runtimeService.startProcessInstanceById(processDefinitionId,variables);

		 // Actually start the process
        // No need to pass the tenant id here, the process definition is already tenant based and the process instance will inherit it
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinitionId, variables);
        logger.info("getProcessDefinitionId-------------->"+processInstance.getProcessDefinitionId());
        logger.info("getProcessDefinitionKey-------------->"+processInstance.getProcessDefinitionKey());
        logger.info("processDefinitionName-------------->"+processInstance.getProcessDefinitionName());
        logger.info("processInstanceId-------------->"+processInstance.getProcessInstanceId());
        logger.info("getId-------------->"+processInstance.getId());
        /*******************************个人任务测试代码end****************************************/
        // Can only set name in case process didn't end instantly
        if (!processInstance.isEnded() && processInstanceName != null) {
            runtimeService.setProcessInstanceName(processInstance.getId(), processInstanceName);
        }
        return processInstance;
        
	}

}
