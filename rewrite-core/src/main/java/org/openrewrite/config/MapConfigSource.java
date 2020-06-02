/*
 * Copyright 2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.config;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.ConfigSource;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyMap;

public class MapConfigSource implements ConfigSource {
    private final Map<String, String> properties;

    public MapConfigSource(Map<String, String> properties) {
        this.properties = properties;
    }

    @Override
    public Map<String, String> getProperties() {
        return properties;
    }

    @Override
    public String getValue(String propertyName) {
        return properties.get(propertyName);
    }

    @Override
    public String getName() {
        return "map";
    }

    public static Config mapConfig(Map<String, String> properties) {
        return ConfigProviderResolver.instance()
                .getBuilder()
                .addDiscoveredSources() // https://github.com/microbean/microbean-microprofile-config/issues/1
                .addDiscoveredConverters()
                .withSources(new MapConfigSource(properties))
                .build();
    }

    public static Config mapConfig(String... keyValues) {
        if (keyValues == null || keyValues.length == 0) {
            return emptyMapConfig();
        }
        if (keyValues.length % 2 == 1) {
            throw new IllegalArgumentException("size must be even, it is a set of key=value pairs");
        }
        Map<String, String> properties = new HashMap<>(keyValues.length / 2);
        for (int i = 0; i < keyValues.length; i += 2) {
            properties.put(keyValues[i], keyValues[i + 1]);
        }
        return mapConfig(properties);
    }

    public static Config emptyMapConfig() {
        return mapConfig(emptyMap());
    }
}
