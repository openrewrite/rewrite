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
import org.openrewrite.*;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.*;

import static org.openrewrite.Validated.required;
import static org.openrewrite.Validated.test;

public class YamlResourceLoader implements RecipeConfigurationLoader, RefactorVisitorLoader {
    private static final ObjectMapper propertyConverter = new ObjectMapper()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private final Map<String, RecipeConfiguration> recipes = new HashMap<>();
    private final Collection<CompositeRefactorVisitor> visitors = new ArrayList<>();
    private final Map<CompositeRefactorVisitor, String> visitorExtensions = new HashMap<>();

    public static final String visitorType = "specs.openrewrite.org/v1beta/visitor";
    public static final String recipeType = "specs.openrewrite.org/v1beta/recipe";

    private static final Set<String> validTypes = new LinkedHashSet<>(Arrays.asList(visitorType, recipeType));
    private static final String validTypesString = String.join(", ", validTypes);

    public YamlResourceLoader(InputStream yamlInput) throws UncheckedIOException {
        try {
            try {
                Yaml yaml = new Yaml();
                for (Object resource : yaml.loadAll(yamlInput)) {
                    if (resource instanceof Map) {
                        @SuppressWarnings("unchecked") Map<String, Object> resourceMap = (Map<String, Object>) resource;
                        String type = resourceMap.getOrDefault("type", "missing").toString();
                        Validated validation = required("type", type)
                                .and(test("type",
                                        "must be a valid rewrite type. These are the valid types: " + validTypesString,
                                        type,
                                        validTypes::contains));
                        if(validation.isInvalid()) {
                            throw new ValidationException(validation);
                        }
                        switch (type) {
                            case visitorType:
                                mapVisitor(resourceMap);
                                break;
                            case recipeType:
                                mapRecipe(resourceMap);
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
            throw new ValidationException(validation);
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
                        Validated.invalid("visitor", subVisitorNameAndConfig, "must be constructable", e)
                );
            }
        }

        CompositeRefactorVisitor visitor = new CompositeRefactorVisitor(visitorMap.get("name").toString(), subVisitors);

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
            if(e.getCause() != null && e.getCause() instanceof ValidationException) {
                throw new ValidationException((ValidationException) e.getCause(), null);
            } else {
                throw new ValidationException(Validated.invalid("recipe", recipeMap,
                        "must be a valid recipe configuration", e));
            }
        }

        Validated validated = Validated.required("recipe.getName()", recipe.getName())
                .and(Validated.test("recipe.getName()",
                        "there is already another recipe with that name",
                        recipe.getName(),
                        it -> !recipes.containsKey(it)));
        if(validated.isInvalid()) {
            throw new ValidationException(validated);
        }
        recipes.put(recipe.getName(), recipe);
    }

    @Override
    public Collection<RecipeConfiguration> loadRecipes() {
        return recipes.values();
    }

    @Override
    public Collection<? extends RefactorVisitor<?>> loadVisitors() {
        return visitors;
    }
}
