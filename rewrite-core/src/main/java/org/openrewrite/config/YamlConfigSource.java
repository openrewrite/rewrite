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
import org.yaml.snakeyaml.Yaml;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class YamlConfigSource extends MapConfigSource {
    public YamlConfigSource(InputStream yaml) {
        super(yamlToMap(yaml));
    }

    @Override
    public String getValue(String propertyName) {
        return super.getValue(propertyName.toLowerCase().replace("-", ""));
    }

    @Override
    public String getName() {
        return "yaml";
    }

    public static Config yamlConfig(String yaml) {
        return ConfigProviderResolver.instance()
                .getBuilder()
                .addDiscoveredSources() // https://github.com/microbean/microbean-microprofile-config/issues/1
                .addDiscoveredConverters()
                .withSources(new YamlConfigSource(new ByteArrayInputStream(yaml.getBytes(Charset.defaultCharset()))))
                .build();
    }

    private static Map<String, String> yamlToMap(InputStream yaml) {
        return yamlToProperties(new Yaml().load(yaml), "")
                .collect(Collectors.toMap(Property::getName, Property::getValue));
    }

    private static Stream<Property> yamlToProperties(Map<String, Object> map, String prefix) {
        return map.entrySet().stream()
                .flatMap(e -> {
                    if(e.getValue() instanceof Map) {
                        //noinspection unchecked
                        return yamlToProperties((Map<String, Object>) e.getValue(), prefix + e.getKey().toLowerCase() + ".");
                    }
                    else {
                        return Stream.of(new Property(prefix + e.getKey().toLowerCase(), e.getValue().toString()));
                    }
                });
    }

    private static class Property {
        private final String name;
        private final String value;

        private Property(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }
    }
}
