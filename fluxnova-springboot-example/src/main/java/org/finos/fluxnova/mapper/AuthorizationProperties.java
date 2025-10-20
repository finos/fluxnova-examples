package org.finos.fluxnova.mapper;

public class AuthorizationProperties {
    private int type;
    private String resourceId;

    public int getType() {
        return type;
    }
    public void setType(int type) {
        this.type = type;
    }
    public String getResourceId() {
        return resourceId;
    }
    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }
}

