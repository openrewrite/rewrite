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
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openrewrite.Profile;
import org.openrewrite.SourceVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.openrewrite.Profile.FilterReply.NEUTRAL;

public class ProfileConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(Profile.class);
    private static final ObjectMapper propertyConverter = new ObjectMapper()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    @SuppressWarnings("rawtypes")
    private final Map<Class<? extends SourceVisitor>, Map<String, Object>> propertiesByVisitor = new IdentityHashMap<>();

    private String name = "default";
    private Set<String> extend = emptySet();
    private Set<Pattern> include = emptySet();
    private Set<Pattern> exclude = emptySet();
    private Map<String, Object> configure = emptyMap();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Alias for {@link #setName(String)}.
     *
     * @param name The profile name.
     */
    public void setProfile(String name) {
        setName(name);
    }

    public void setExtend(Set<String> extend) {
        this.extend = extend;
    }

    public void setInclude(Set<String> include) {
        this.include = toPatternSet(include);
    }

    public void setExclude(Set<String> exclude) {
        this.exclude = toPatternSet(exclude);
    }

    public void setConfigure(Map<String, Object> configure) {
        this.configure = configure;
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

    public ProfileConfiguration merge(@Nullable ProfileConfiguration profile) {
        if (profile != null && profile.name.equals(name)) {
            ProfileConfiguration merged = new ProfileConfiguration();
            merged.name = name;
            merged.include = Stream.concat(this.include.stream(), profile.include.stream()).collect(toUnmodifiableSet());
            merged.exclude = Stream.concat(this.exclude.stream(), profile.exclude.stream()).collect(toUnmodifiableSet());

            merged.configure = configure;
            merged.configure.putAll(profile.configure);

            return merged;
        }

        return this;
    }

    public Profile build(Collection<ProfileConfiguration> otherProfileConfigurations) {
        Map<String, ProfileConfiguration> configsByName = otherProfileConfigurations.stream()
                .collect(toMap(ProfileConfiguration::getName, identity()));

        Deque<ProfileConfiguration> inOrderConfigurations = new ArrayDeque<>();
        Queue<ProfileConfiguration> configs = new LinkedList<>();
        configs.add(ProfileConfiguration.this);
        while (!configs.isEmpty()) {
            ProfileConfiguration config = configs.poll();
            inOrderConfigurations.add(config);
            config.extend.stream()
                    .map(configsByName::get)
                    .filter(Objects::nonNull)
                    .forEach(configs::add);
        }

        return new Profile() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public <S, T extends SourceVisitor<S>> FilterReply accept(T visitor) {
                return inOrderConfigurations.stream().reduce(
                        NEUTRAL,
                        (reply, config) -> reply.equals(NEUTRAL) ? config.accept(visitor) : reply,
                        (r1, r2) -> r1.equals(NEUTRAL) ? r2 : r1
                );
            }

            @Override
            public <S, T extends SourceVisitor<S>> T configure(T visitor) {
                Iterator<ProfileConfiguration> configs = inOrderConfigurations.descendingIterator();
                while(configs.hasNext()) {
                    configs.next().configure(visitor);
                }
                return visitor;
            }
        };
    }

    private void configure(SourceVisitor<?> visitor) {
        try {
            propertyConverter.updateValue(visitor, propertyMap(visitor));
        } catch (JsonMappingException e) {
            logger.warn("Unable to configure {}", visitor.getClass().getName(), e);
        }
    }

    protected Map<String, Object> propertyMap(SourceVisitor<?> visitor) {
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

    private Profile.FilterReply accept(SourceVisitor<?> visitor) {
        if (exclude.stream().anyMatch(i -> i.matcher(visitor.getName()).matches())) {
            return Profile.FilterReply.DENY;
        }

        if (include.stream().anyMatch(i -> i.matcher(visitor.getName()).matches())) {
            return Profile.FilterReply.ACCEPT;
        }

        return NEUTRAL;
    }
}
