package ru.synergy.webSocket;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class Payload {
    private User user;
    private Map<String, Object> properties = new HashMap<>();

    public Payload(User user, Map<String, Object> properties) {
        this.user = user;
        this.properties = properties;
    }

    @JsonCreator
    public Payload(@JsonProperty("user") User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    @JsonAnySetter
    public void setProperties(String name, Object value) {
        properties.put(name, value);
    }

    @JsonAnyGetter
    public Map<String, Object> getProperties() {
        return properties;
    }
}
