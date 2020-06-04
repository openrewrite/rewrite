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

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openrewrite.SourceVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toUnmodifiableSet;

public class Profile {
    private static final Logger logger = LoggerFactory.getLogger(Profile.class);
    private static final ObjectMapper propertyConverter = new ObjectMapper()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES);

    private String name = "default";
    private Set<Pattern> include = emptySet();
    private Set<Pattern> exclude = emptySet();

    @SuppressWarnings("rawtypes")
    private final Map<Class<? extends SourceVisitor>, Map<String, Object>> propertiesByVisitor = new IdentityHashMap<>();

    /**
     * Normalized to lower case
     */
    private Map<String, Object> configure = emptyMap();

    public Profile(String name, Set<String> include, Set<String> exclude,
                   Map<String, Object> configure) {
        this.name = name;
        this.include = toPatternSet(include);
        this.exclude = toPatternSet(exclude);
        setConfigure(configure);
    }

    public Profile() {
    }

    public void setName(@Nullable String name) {
        this.name = name;
    }

    public void setProfile(String name) {
        this.name = name;
    }

    public void setInclude(Set<String> include) {
        this.include = toPatternSet(include);
    }

    public void setExclude(Set<String> exclude) {
        this.exclude = toPatternSet(exclude);
    }

    public void setConfigure(Map<String, Object> configure) {
        this.configure = toLowerCaseMap(configure);
    }

    private Map<String, Object> toLowerCaseMap(Map<String, Object> configure) {
        return configure.entrySet().stream()
                .map(e -> {
                    Object value = e.getValue();
                    if (value instanceof Map) {
                        //noinspection unchecked
                        value = toLowerCaseMap((Map<String, Object>) value);
                    }
                    return Map.entry(e.getKey().toLowerCase()
                            .replace("-", "")
                            .replace("_", ""), value);
                })
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Set<Pattern> toPatternSet(Set<String> set) {
        return set.stream()
                .map(i -> i.replace("*", ".+"))
                .map(Pattern::compile)
                .collect(toUnmodifiableSet());
    }

    public <S, T extends SourceVisitor<S>> T configure(T visitor) {
        try {
            propertyConverter.updateValue(visitor, propertyMap(visitor));
        } catch (JsonMappingException e) {
            logger.warn("Unable to configure {}", visitor.getClass().getName(), e);
        }
        return visitor;
    }

    private Map<String, Object> propertyMap(SourceVisitor<?> visitor) {
        return propertiesByVisitor.computeIfAbsent(visitor.getClass(),
                clazz -> propertyMap(clazz.getName().toLowerCase(), configure));
    }

    private Map<String, Object> propertyMap(String partialName, @Nullable Object config) {
        if (!(config instanceof Map)) {
            return emptyMap();
        }

        @SuppressWarnings("unchecked") Map<String, Object> configMap = (Map<String, Object>) config;
        Map<String, Object> properties = new HashMap<>();

        for (int nextDot = partialName.indexOf('.'); nextDot != -1; nextDot = partialName.indexOf('.', nextDot + 1)) {
            String subpackage = partialName.substring(0, nextDot);
            String remaining = partialName.substring(nextDot + 1);

            for (Map.Entry<String, Object> configEntry : configMap.entrySet()) {
                if (Pattern.compile(configEntry.getKey().replace("*", ".+")).matcher(subpackage).matches()) {
                    properties.putAll(propertyMap(remaining, configEntry.getValue()));
                }
            }
        }

        for (Map.Entry<String, Object> configEntry : configMap.entrySet()) {
            String keyPart = configEntry.getKey();

            Map<String, Object> value;
            if(configEntry.getValue() instanceof Map) {
                //noinspection unchecked
                value = (Map<String, Object>) configEntry.getValue();
            } else {
                value = Map.of(keyPart.substring(keyPart.lastIndexOf('.') + 1), configEntry.getValue());
                keyPart = keyPart.substring(0, Math.max(0, keyPart.lastIndexOf('.')));
            }

            Matcher matcher;
            do {
                matcher = Pattern.compile("^" + keyPart.replace("*", ".+")).matcher(partialName);
                if (matcher.find()) {
                    properties.putAll(value);
                    break;
                } else if (matcher.hitEnd()) {
                    value = Map.of(keyPart.substring(keyPart.lastIndexOf('.') + 1), value);
                    keyPart = keyPart.substring(0, keyPart.lastIndexOf('.'));
                }
            } while(matcher.hitEnd());
        }

        return properties;
    }

    public Profile merge(@Nullable Profile profile) {
        if (profile != null && profile.getName().equals(name)) {
            Profile merged = new Profile();
            merged.name = name;
            merged.include = Stream.concat(this.include.stream(), profile.include.stream()).collect(toUnmodifiableSet());
            merged.exclude = Stream.concat(this.exclude.stream(), profile.exclude.stream()).collect(toUnmodifiableSet());

            merged.configure = configure;
            merged.configure.putAll(profile.configure);

            return merged;
        }

        return this;
    }

    /**
     * If a visitor is accepted by ANY profile, it will execute.
     *
     * @param visitor The visitor to test.
     * @return {@code true} if the visitor should run.
     */
    public boolean accept(SourceVisitor<?> visitor) {
        String visitorName = visitor.getClass().getName();
        return visitor.validate().isValid() &&
                include.stream().anyMatch(i -> i.matcher(visitorName).matches()) &&
                exclude.stream().noneMatch(e -> e.matcher(visitorName).matches());
    }

    public String getName() {
        return name;
    }
}
