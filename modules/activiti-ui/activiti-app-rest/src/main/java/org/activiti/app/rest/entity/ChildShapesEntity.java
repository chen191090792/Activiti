package org.activiti.app.rest.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * Created by chx on 2019/3/29.
 */

public class ChildShapesEntity {
    private String modelId;
    private List<JsonNode> properties = Lists.newArrayList();

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public List<JsonNode> getProperties() {
        return properties;
    }

    public void setProperties(List<JsonNode> properties) {
        this.properties = properties;
    }
}