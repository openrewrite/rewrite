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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openrewrite.Style;
import org.openrewrite.Validated;
import org.openrewrite.ValidationException;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static java.util.stream.Collectors.toList;

public class StyleConfiguration {
    private static final ObjectMapper propertyConverter = new ObjectMapper()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private String name;
    private Map<String, Object> configure = new HashMap<>();

    public void setConfigure(Map<String, Object> configure) {
        this.configure = configure;
    }

    public Collection<Style> getStyles() {
        return configure.entrySet().stream()
                .map(styleConfigByName -> {
                    try {
                        Style style = (Style) Class.forName(styleConfigByName.getKey()).getDeclaredConstructor().newInstance();
                        propertyConverter.updateValue(style, styleConfigByName.getValue());
                        return style;
                    } catch (Exception e) {
                        throw new ValidationException(
                                Validated.invalid("className",
                                        styleConfigByName,
                                        "must be a fully-qualified class name on the classpath")
                        );
                    }
                })
                .collect(toList());
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
