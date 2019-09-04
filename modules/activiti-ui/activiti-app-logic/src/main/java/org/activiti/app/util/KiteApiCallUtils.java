package org.activiti.app.util;

import com.google.common.collect.Maps;
import org.activiti.app.security.SecurityUtils;
import org.activiti.engine.identity.User;
import org.activiti.engine.task.Task;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Map;

/**
 * @author cxh
 * @version V1.0
 * @Description: 消息发送类
 * @date 2019/6/18 14:29
 */
public class KiteApiCallUtils {

    private static RestTemplate restTemplate = new RestTemplate();
    private static String GET_UPLEADER_URL="http://localhost:8080/api/kite/getUpClassInfo/%s";
    private static String GET_DEPTLEADER_URL="http://localhost:8080/api/kite/getDeptleaderInfo/%s";
    private static String MEDIA_TYPE="application/json; charset=UTF-8";
    private static String WX_MSG_URL="http://localhost:8080/api/kite/notice";
    private static String EMAIL_MSG_URL="http://localhost:8080/api/kite/email";
    private static String CHECK_ADMIN_URL="http://localhost:8080/api/kite/checkAdmin/%s";

    private static String GET_ASSIGNEE_URL="http://localhost:8080/api/kite/getActNodeDetailInfo";

    public static String getUpLeader(String startById){
        String url = String.format(GET_UPLEADER_URL,startById);
        HttpHeaders headers = new HttpHeaders();
        MediaType type = MediaType.parseMediaType(MEDIA_TYPE);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.setContentType(type);
        HttpEntity entity = new HttpEntity<>(null, headers);
        HttpEntity<String> result = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        return  result.getBody();
    }

    public static String getDeptLeader(String startById){
        String url = String.format(GET_DEPTLEADER_URL,startById);
        HttpHeaders headers = new HttpHeaders();
        MediaType type = MediaType.parseMediaType(MEDIA_TYPE);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.setContentType(type);
        HttpEntity entity = new HttpEntity<>(null, headers);
        HttpEntity<String> result = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        return  result.getBody();
    }

    public static String sendWxMsg(Task task){
        HttpEntity entity = getHttpEntity(task);
        String result = restTemplate.postForObject(WX_MSG_URL, entity, String.class);
        return  result;
    }

    public static String sendEmail(Task task){
        HttpEntity entity = getHttpEntity(task);
        String result = restTemplate.postForObject(EMAIL_MSG_URL, entity, String.class);
        return  result;
    }
    public static HttpEntity getHttpEntity(Task task) {
        User currentUser = SecurityUtils.getCurrentUserObject();
        JsonUtils utils = new JsonUtils();
        Map<String,Object> data = Maps.newConcurrentMap();
        data.put("taskId",task.getName());
        data.put("userId",currentUser.getId());
        data.put("taskName",task.getName());
        String dataJson = utils.object2Json(data);
        HttpHeaders headers = new HttpHeaders();
        MediaType type = MediaType.parseMediaType(MEDIA_TYPE);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.setContentType(type);
        return new HttpEntity<>(dataJson, headers);
    }

    public static boolean checkAdmin(){
       /*
        User currentUser = SecurityUtils.getCurrentUserObject();
        String url = String.format(CHECK_ADMIN_URL,currentUser.getId());
        HttpHeaders headers = new HttpHeaders();
        MediaType type = MediaType.parseMediaType(MEDIA_TYPE);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.setContentType(type);
        HttpEntity entity = new HttpEntity<>(null, headers);
        HttpEntity<Boolean> result = restTemplate.exchange(url, HttpMethod.GET, entity, Boolean.class);
        return  result.getBody();
        */
       return true;
    }

    public static String getAssignee(String taskDefinitionKey, Integer processDefinitionVersion) {
        String url = GET_ASSIGNEE_URL+"?processNodeId="+taskDefinitionKey+"&version="+processDefinitionVersion;
        HttpHeaders headers = new HttpHeaders();
        MediaType type = MediaType.parseMediaType(MEDIA_TYPE);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.setContentType(type);
        HttpEntity entity = new HttpEntity<>(null, headers);
        HttpEntity<String> result = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        return  result.getBody();
    }
}