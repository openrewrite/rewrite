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
import org.openrewrite.Validated;
import org.openrewrite.ValidationException;
import org.openrewrite.internal.lang.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
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

    private List<CompositeSourceVisitor<?>> define = new ArrayList<>();

    @SuppressWarnings("rawtypes")
    private final Map<Class<? extends SourceVisitor>, Map<String, Object>> propertiesByVisitor = new IdentityHashMap<>();

    /**
     * Normalized to lower case
     */
    private Map<String, Object> configure = emptyMap();

    public Profile setName(@Nullable String name) {
        this.name = name;
        return this;
    }

    public Profile setProfile(String name) {
        this.name = name;
        return this;
    }

    public Profile setInclude(Set<String> include) {
        this.include = toPatternSet(include);
        return this;
    }

    public Profile setExclude(Set<String> exclude) {
        this.exclude = toPatternSet(exclude);
        return this;
    }

    public Profile setConfigure(Map<String, Object> configure) {
        this.configure = configure;
        return this;
    }

    public Profile setDefine(Map<String, Object> define) {
        Map<String, List<Object>> definitionsByName = definitionsByName("", define);

        for (Map.Entry<String, List<Object>> visitorsByCompositeName : definitionsByName.entrySet()) {
            List<SourceVisitor<?>> visitors = new ArrayList<>();

            for (Object visitorObject : visitorsByCompositeName.getValue()) {
                Object value = null;
                Object visitorClassName = visitorObject;

                if (visitorObject instanceof Map) {
                    @SuppressWarnings("unchecked") Map.Entry<String, ?> visitorEntry = ((Map<String, ?>) visitorObject)
                            .entrySet().iterator().next();
                    visitorClassName = visitorEntry.getKey();
                    value = visitorEntry.getValue();
                }

                try {
                    Class<?> visitorClass;
                    try {
                        visitorClass = Class.forName(visitorClassName.toString());
                    } catch (ClassNotFoundException ignored) {
                        visitorClass = Class.forName("org.openrewrite." + visitorClassName);
                    }

                    SourceVisitor<?> visitor = (SourceVisitor<?>) visitorClass
                            .getDeclaredConstructor().newInstance();
                    propertyConverter.updateValue(visitor, value);
                    visitors.add(visitor);
                } catch (Exception e) {
                    throw new ValidationException(
                            Validated.invalid("define", visitorClassName, "must be constructable", e)
                    );
                }
            }

            this.define.add(new CompositeSourceVisitor(visitorsByCompositeName.getKey(), visitors));
        }

        return this;
    }

    private Map<String, List<Object>> definitionsByName(String prefix, Map<String, Object> define) {
        return define.entrySet().stream()
                .flatMap(e -> {
                    Object value = e.getValue();
                    if (value instanceof Map) {
                        //noinspection unchecked
                        return definitionsByName(prefix + "." + e.getKey(), (Map<String, Object>) value)
                                .entrySet().stream();
                    } else if (value instanceof List) {
                        //noinspection unchecked
                        return Stream.of(Map.entry(prefix + "." + e.getKey(), (List<Object>) value));
                    } else {
                        throw new ValidationException(Validated.invalid("define." + prefix + "." + e.getKey(),
                                value, "should be a list of visitors"));
                    }
                })
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Set<Pattern> toPatternSet(Set<String> set) {
        return set.stream()
                .map(i -> i
                        .replace(".", "\\.")
                        .replace("*", ".+"))
                .map(i -> "(org\\.openrewrite\\.)?" + i)
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
                clazz -> propertyMap(clazz.getName(), configure));
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
            if (configEntry.getValue() instanceof Map) {
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
            } while (matcher.hitEnd());
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

    public List<CompositeSourceVisitor<?>> getDefinitions() {
        return define;
    }
}
