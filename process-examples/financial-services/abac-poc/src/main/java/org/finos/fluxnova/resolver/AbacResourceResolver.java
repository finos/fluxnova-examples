package org.finos.fluxnova.resolver;

import java.util.Map;

public interface AbacResourceResolver<T> {

    Map<String, String> getAttributes(T resource);
}

