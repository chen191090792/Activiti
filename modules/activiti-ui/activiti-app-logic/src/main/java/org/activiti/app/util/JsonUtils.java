package org.activiti.app.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * json封装工具
 * @author cxh
 *
 */
public class JsonUtils {

	private final ObjectMapper objectMapper;

    public JsonUtils() {
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public String writeValueAsString(Map<?, ?> map) {
        try {
            return objectMapper.writeValueAsString(map);
        }
        catch (IOException e) {
            throw new RuntimeException("把java.util.Map对象序列化为字符串时失败。", e);
        }
    }

    /**
     * 把json字符串转换为{@link Map}对象。
     *
     * @param json
     * @return
     */
    public Map<String, Object> json2Map(String json) {
        try {
            Map<String, Object> result = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() { });
            return result;
        }
        catch (IOException e) {
            throw new RuntimeException("把json转换为java.util.Map对象时失败。", e);
        }
    }

    /**
     * 把json字符串转换为指定类型的对象。
     *
     * @param json
     * @return
     */
    public <T> T json2Object(String json, Class<T> clazz) {
        try {
            T result = objectMapper.readValue(json, clazz);
            return result;
        }
        catch (IOException e) {
            throw new RuntimeException(String.format("把json转换为%s类型的对象时失败。", clazz.getName()), e);
        }
    }

    /**
     * 把json字符串转换为指定类型的对象。
     *
     * @param json
     * @param valueTypeRef
     * @return
     */
    public <T> T json2Object(String json, TypeReference<T> valueTypeRef) {
        try {
            T result = objectMapper.readValue(json, valueTypeRef);
            return result;
        }
        catch (IOException e) {
            throw new RuntimeException(String.format("把json转换为%s类型的对象时失败。", valueTypeRef), e);
        }
    }

    /**
     * 把对象转换为json字符串。
     *
     * @param value
     * @return
     */
    public String object2Json(Object value) {
        try {
            String result = objectMapper.writeValueAsString(value);
            return result;
        }
        catch (IOException e) {
            throw new RuntimeException(String.format("把对象转换为json字符串时失败：%s", value), e);
        }
    }
}
