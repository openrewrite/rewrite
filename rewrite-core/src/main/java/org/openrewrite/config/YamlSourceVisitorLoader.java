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
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.openrewrite.SourceVisitor;
import org.openrewrite.Tree;
import org.openrewrite.Validated;
import org.openrewrite.ValidationException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.util.Collections.*;

public class YamlSourceVisitorLoader implements SourceVisitorLoader {
    private static final ObjectMapper propertyConverter = new ObjectMapper()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private final String name;
    private final InputStream yamlInput;

    public YamlSourceVisitorLoader(String name, InputStream yaml) {
        this.name = name;
        this.yamlInput = yaml;
    }

    @Override
    public Collection<SourceVisitor<?>> load() {
        Yaml yaml = new Yaml(new Constructor(YamlSourceVisitorConfiguration.class));
        CompositeSourceVisitor compositeVisitor = ((YamlSourceVisitorConfiguration) yaml.load(yamlInput)).compositeVisitor;
        return compositeVisitor == null ? emptyList() : singletonList(compositeVisitor.setName(name));
    }

    public static class YamlSourceVisitorConfiguration {
        private CompositeSourceVisitor compositeVisitor;

        public void setVisitors(List<Object> subVisitorsMap) {
            List<SourceVisitor<Object>> subVisitors = new ArrayList<>();

            for (Object subVisitorNameAndConfig : subVisitorsMap) {
                try {
                    if (subVisitorNameAndConfig instanceof String) {
                        //noinspection unchecked
                        subVisitors.add((SourceVisitor<Object>) visitorClass((String) subVisitorNameAndConfig)
                                .getDeclaredConstructor().newInstance());
                    } else if (subVisitorNameAndConfig instanceof Map) {
                        //noinspection unchecked
                        for (Map.Entry<String, Object> subVisitorEntry : ((Map<String, Object>) subVisitorNameAndConfig)
                                .entrySet()) {
                            @SuppressWarnings("unchecked") SourceVisitor<Object> subVisitor =
                                    (SourceVisitor<Object>) visitorClass(subVisitorEntry.getKey())
                                            .getDeclaredConstructor().newInstance();

                            propertyConverter.updateValue(subVisitor, subVisitorEntry.getValue());

                            subVisitors.add(subVisitor);
                        }
                    }
                } catch (Exception e) {
                    throw new ValidationException(
                            Validated.invalid("visitor", subVisitorNameAndConfig, "must be constructable", e)
                    );
                }
            }

            this.compositeVisitor = new CompositeSourceVisitor(subVisitors);
        }

        private Class<?> visitorClass(String name) throws ClassNotFoundException {
            try {
                return Class.forName(name.toString());
            } catch (ClassNotFoundException ignored) {
                return Class.forName("org.openrewrite." + name);
            }
        }
    }

    public static class CompositeSourceVisitor extends SourceVisitor<Object> {
        private String name;
        private final List<SourceVisitor<Object>> delegates;

        public CompositeSourceVisitor(List<SourceVisitor<Object>> delegates) {
            this.name = name;
            this.delegates = delegates;
            delegates.forEach(this::andThen);
        }

        @Override
        public Iterable<Tag> getTags() {
            return Tags.of("name", name);
        }

        CompositeSourceVisitor setName(String name) {
            this.name = name;
            return this;
        }

        public Class<?> getVisitorType() {
            return delegates.stream().findAny()
                    .map(Object::getClass)
                    .orElse(null);
        }

        public String getName() {
            return name;
        }

        @Override
        public Object defaultTo(Tree t) {
            return delegates.stream().findAny().map(v -> v.defaultTo(t)).orElse(null);
        }
    }
}
