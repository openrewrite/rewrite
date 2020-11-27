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
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import kotlin.text.Charsets;
import org.openrewrite.*;
import org.openrewrite.internal.PropertyPlaceholderHelper;
import org.openrewrite.internal.lang.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

import static org.openrewrite.Validated.required;
import static org.openrewrite.Validated.test;

public class YamlResourceLoader implements ResourceLoader {
    private static final ObjectMapper propertyConverter = new ObjectMapper()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private static final PropertyPlaceholderHelper propertyPlaceholderHelper =
            new PropertyPlaceholderHelper("${", "}", ":");

    private final Map<String, RecipeConfiguration> recipes = new HashMap<>();
    private final Collection<CompositeRefactorVisitor> visitors = new ArrayList<>();
    private final Map<CompositeRefactorVisitor, String> visitorExtensions = new HashMap<>();
    private final Map<String, Collection<Style>> styles = new HashMap<>();

    private final URI source;

    private enum ResourceType {
        Visitor("specs.openrewrite.org/v1beta/visitor"),
        Recipe("specs.openrewrite.org/v1beta/recipe"),
        Style("specs.openrewrite.org/v1beta/style");

        private final String spec;

        ResourceType(String spec) {
            this.spec = spec;
        }

        public String getSpec() {
            return spec;
        }

        @Nullable
        public static ResourceType fromSpec(String spec) {
            return Arrays.stream(values())
                    .filter(type -> type.getSpec().equals(spec))
                    .findAny()
                    .orElse(null);
        }
    }

    public YamlResourceLoader(InputStream yamlInput, URI source, Properties properties) throws UncheckedIOException {
        this.source = source;
        try {
            try {
                Yaml yaml = new Yaml();

                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                int nRead;
                byte[] data = new byte[1024];
                while ((nRead = yamlInput.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }

                String yamlSource = new String(buffer.toByteArray(), Charsets.UTF_8);
                yamlSource = propertyPlaceholderHelper.replacePlaceholders(yamlSource, properties);

                for (Object resource : yaml.loadAll(yamlSource)) {
                    if (resource instanceof Map) {
                        @SuppressWarnings("unchecked") Map<String, Object> resourceMap = (Map<String, Object>) resource;
                        String type = resourceMap
                                .getOrDefault("type", "missing")
                                .toString();

                        Validated validated = required("type", type)
                                .and(test("type",
                                        "must be one of the following resource types: " +
                                                Arrays.stream(ResourceType.values()).map(ResourceType::getSpec)
                                                        .collect(Collectors.joining(", ")),
                                        type,
                                        type2 -> ResourceType.fromSpec(type2) != null)
                                );

                        if (validated.isInvalid()) {
                            throw new ValidationException(validated, source);
                        }

                        //noinspection ConstantConditions
                        switch (ResourceType.fromSpec(type)) {
                            case Visitor:
                                mapVisitor(resourceMap);
                                break;
                            case Recipe:
                                mapRecipe(resourceMap);
                                break;
                            case Style:
                                mapStyle(resourceMap);
                                break;
                        }
                    }
                }

                for (Map.Entry<CompositeRefactorVisitor, String> extendingVisitor : visitorExtensions.entrySet()) {
                    visitors.stream().filter(v -> v.getName().equals(extendingVisitor.getValue())).findAny()
                            .ifPresent(v -> extendingVisitor.getKey().extendsFrom(v));
                }
            } finally {
                yamlInput.close();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void mapVisitor(Map<String, Object> visitorMap) {
        Validated validation = required("name", visitorMap.get("name"))
                .and(required("visitors", visitorMap.get("visitors")))
                .and(test("visitors",
                        "must be a list",
                        visitorMap.get("visitors"),
                        visitors -> visitors instanceof List));

        if (validation.isInvalid()) {
            throw new ValidationException(validation, source);
        }

        List<RefactorVisitor<? extends Tree>> subVisitors = new ArrayList<>();

        //noinspection unchecked
        for (Object subVisitorNameAndConfig : (List<Object>) visitorMap.get("visitors")) {
            try {
                if (subVisitorNameAndConfig instanceof String) {
                    //noinspection unchecked
                    subVisitors.add((RefactorVisitor<Tree>) visitorClass((String) subVisitorNameAndConfig)
                            .getDeclaredConstructor().newInstance());
                } else if (subVisitorNameAndConfig instanceof Map) {
                    //noinspection unchecked
                    for (Map.Entry<String, Object> subVisitorEntry : ((Map<String, Object>) subVisitorNameAndConfig)
                            .entrySet()) {
                        @SuppressWarnings("unchecked") RefactorVisitor<Tree> subVisitor =
                                (RefactorVisitor<Tree>) visitorClass(subVisitorEntry.getKey())
                                        .getDeclaredConstructor().newInstance();

                        propertyConverter.updateValue(subVisitor, subVisitorEntry.getValue());

                        subVisitors.add(subVisitor);
                    }
                }
            } catch (Exception e) {
                throw new ValidationException(
                        Validated.invalid("visitor", subVisitorNameAndConfig, "must be constructable", e),
                        source
                );
            }
        }

        ResourceLoadedVisitor visitor = new ResourceLoadedVisitor(visitorMap.get("name").toString(), subVisitors);

        if (visitorMap.containsKey("extends")) {
            visitorExtensions.put(visitor, visitorMap.get("extends").toString());
        }

        this.visitors.add(visitor);
    }

    private Class<?> visitorClass(String name) throws ClassNotFoundException {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException ignored) {
            return Class.forName("org.openrewrite." + name);
        }
    }

    private void mapRecipe(Map<String, Object> recipeMap) {
        RecipeConfiguration recipe = new RecipeConfiguration();
        try {
            propertyConverter.updateValue(recipe, recipeMap);
        } catch (JsonMappingException e) {
            if (e.getCause() != null && e.getCause() instanceof ValidationException) {
                throw (ValidationException) e.getCause();
            } else {
                throw new ValidationException(Validated.invalid("recipe", recipeMap,
                        "must be a valid recipe configuration", e), source);
            }
        }

        Validated validated = Validated.required("name", recipe.getName())
                .and(Validated.test("name",
                        "there is already another recipe with that name",
                        recipe.getName(),
                        it -> !recipes.containsKey(it)));
        if (validated.isInvalid()) {
            throw new ValidationException(validated, source);
        }
        recipes.put(recipe.getName(), recipe);
    }

    private void mapStyle(Map<String, Object> styleMap) {
        StyleConfiguration style = new StyleConfiguration();
        try {
            propertyConverter.updateValue(style, styleMap);
        } catch (JsonMappingException e) {
            if (e.getCause() != null && e.getCause() instanceof ValidationException) {
                throw (ValidationException) e.getCause();
            } else {
                throw new ValidationException(Validated.invalid("styles", styleMap,
                        "must be a valid style configuration", e), source);
            }
        }

        Validated validated = Validated.required("name", style.getName())
                .and(Validated.test("name",
                        "there is already another style configuration with that name",
                        style.getName(),
                        it -> !styles.containsKey(it)));
        if (validated.isInvalid()) {
            throw new ValidationException(validated, source);
        }

        styles.put(style.getName(), style.getStyles());
    }

    @Override
    public Collection<RecipeConfiguration> loadRecipes() {
        return recipes.values();
    }

    @Override
    public Collection<? extends RefactorVisitor<?>> loadVisitors() {
        return visitors;
    }

    @Override
    public Map<String, Collection<Style>> loadStyles() {
        return styles;
    }

    private static class ResourceLoadedVisitor extends CompositeRefactorVisitor {
        private final String name;

        public ResourceLoadedVisitor(String name, List<RefactorVisitor<? extends Tree>> delegates) {
            this.name = name;
            delegates.forEach(this::addVisitor);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Iterable<Tag> getTags() {
            return Tags.of("name", name);
        }
    }
}
