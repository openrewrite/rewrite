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

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.ConstructorDetector;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.openrewrite.Recipe;
import org.openrewrite.RecipeException;
import org.openrewrite.Validated;
import org.openrewrite.internal.PropertyPlaceholderHelper;
import org.openrewrite.internal.RecipeIntrospectionUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.style.Style;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.RecipeSerializer.maybeAddKotlinModule;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.Validated.invalid;

public class YamlResourceLoader implements ResourceLoader {

    int refCount = 0;

    private static final PropertyPlaceholderHelper propertyPlaceholderHelper =
            new PropertyPlaceholderHelper("${", "}", ":");

    private final URI source;
    private final String yamlSource;

    private final ObjectMapper mapper;

    private enum ResourceType {
        Recipe("specs.openrewrite.org/v1beta/recipe"),
        Style("specs.openrewrite.org/v1beta/style"),
        Category("specs.openrewrite.org/v1beta/category"),
        Example("specs.openrewrite.org/v1beta/example");

        private final String spec;

        ResourceType(String spec) {
            this.spec = spec;
        }

        public String getSpec() {
            return spec;
        }

        @Nullable
        public static ResourceType fromSpec(@Nullable String spec) {
            return Arrays.stream(values())
                    .filter(type -> type.getSpec().equals(spec))
                    .findAny()
                    .orElse(null);
        }
    }

    /**
     * Load a declarative recipe using the runtime classloader
     *
     * @param yamlInput  Declarative recipe yaml input stream
     * @param source     Declarative recipe source
     * @param properties Placeholder properties
     * @throws UncheckedIOException On unexpected IOException
     */
    public YamlResourceLoader(InputStream yamlInput, URI source, Properties properties) throws UncheckedIOException {
        this(yamlInput, source, properties, null);
    }

    /**
     * Load a declarative recipe, optionally using the specified classloader
     *
     * @param yamlInput   Declarative recipe yaml input stream
     * @param source      Declarative recipe source
     * @param properties  Placeholder properties
     * @param classLoader Optional classloader to use with jackson. If not specified, the runtime classloader will be used.
     * @throws UncheckedIOException On unexpected IOException
     */
    public YamlResourceLoader(InputStream yamlInput, URI source, Properties properties, @Nullable ClassLoader classLoader) throws UncheckedIOException {
        this.source = source;

        mapper = JsonMapper.builder()
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
                .constructorDetector(ConstructorDetector.USE_PROPERTIES_BASED)
                .build()
                .registerModule(new ParameterNamesModule())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        maybeAddKotlinModule(mapper);

        if (classLoader != null) {
            TypeFactory tf = TypeFactory.defaultInstance().withClassLoader(classLoader);
            mapper.setTypeFactory(tf);
        }

        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[1024];
            while ((nRead = yamlInput.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            this.yamlSource = propertyPlaceholderHelper.replacePlaceholders(
                    new String(buffer.toByteArray(), StandardCharsets.UTF_8), properties);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Collection<Map<String, Object>> loadResources(ResourceType resourceType) {
        Collection<Map<String, Object>> resources = new ArrayList<>();

        Yaml yaml = new Yaml(new SafeConstructor());
        for (Object resource : yaml.loadAll(yamlSource)) {
            if (resource instanceof Map) {
                @SuppressWarnings("unchecked") Map<String, Object> resourceMap = (Map<String, Object>) resource;
                if (resourceType.equals(ResourceType.fromSpec((String) resourceMap.get("type")))) {
                    resources.add(resourceMap);
                }
            }
        }

        return resources;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<Recipe> listRecipes() {
        return loadResources(ResourceType.Recipe).stream()
                .filter(r -> r.containsKey("name"))
                .map(r -> {
                    String name = (String) r.get("name");
                    String displayName = (String) r.get("displayName");
                    if (displayName == null) {
                        displayName = name;
                    }
                    String description = (String) r.get("description");
                    Set<String> tags = Collections.emptySet();
                    /*~~>*/List<String> rawTags = (/*~~>*/List<String>) r.get("tags");
                    if (rawTags != null) {
                        tags = new HashSet<>(rawTags);
                    }

                    String estimatedEffortPerOccurrenceStr = (String) r.get("estimatedEffortPerOccurrence");
                    Duration estimatedEffortPerOccurrence = null;
                    if(estimatedEffortPerOccurrenceStr != null) {
                        estimatedEffortPerOccurrence = Duration.parse(estimatedEffortPerOccurrenceStr);
                    }
                    DeclarativeRecipe recipe = new DeclarativeRecipe(name, displayName, description, tags,
                            estimatedEffortPerOccurrence, source, (boolean) r.getOrDefault("causesAnotherCycle", false));
                    /*~~>*/List<Object> recipeList = (/*~~>*/List<Object>) r.get("recipeList");
                    if (recipeList == null) {
                        throw new RecipeException("Invalid Recipe [" + name + "] recipeList is null");
                    }
                    for (int i = 0; i < recipeList.size(); i++) {
                        Object next = recipeList.get(i);
                        if (next instanceof String) {
                            recipe.addUninitialized((String) next);
                        } else if (next instanceof Map) {
                            Map.Entry<String, Object> nameAndConfig = ((Map<String, Object>) next).entrySet().iterator().next();
                            try {
                                if (nameAndConfig.getValue() instanceof Map) {
                                    Map<Object, Object> withJsonType = new HashMap<>((Map<String, Object>) nameAndConfig.getValue());
                                    withJsonType.put("@c", nameAndConfig.getKey());
                                    try {
                                        recipe.addUninitialized(mapper.convertValue(withJsonType, Recipe.class));
                                    } catch (IllegalArgumentException e) {
                                        if (e.getCause() instanceof InvalidTypeIdException) {
                                            recipe.addValidation(Validated.invalid(nameAndConfig.getKey(),
                                                    nameAndConfig.getValue(), "Recipe class " +
                                                            nameAndConfig.getKey() + " cannot be found"));
                                        } else {
                                            recipe.addValidation(Validated.invalid(nameAndConfig.getKey(), nameAndConfig.getValue(),
                                                    "Unable to load Recipe: " + e));
                                        }
                                    }
                                } else {
                                    recipe.addValidation(Validated.invalid(nameAndConfig.getKey(),
                                            nameAndConfig.getValue(),
                                            "Declarative recipeList entries are expected to be strings or mappings"));
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                recipe.addValidation(Validated.invalid(nameAndConfig.getKey(), nameAndConfig.getValue(),
                                        "Unexpected declarative recipe parsing exception " +
                                                e.getClass().getName()));
                            }
                        } else {
                            recipe.addValidation(invalid(
                                    name + ".recipeList[" + i + "] (in " + source + ")",
                                    next,
                                    "is an object type that isn't recognized as a recipe.",
                                    null));
                        }
                    }
                    return recipe;
                })
                .collect(toList());
    }

    @Override
    public Collection<RecipeDescriptor> listRecipeDescriptors() {
        return listRecipeDescriptors(emptyList());
    }

    public Collection<RecipeDescriptor> listRecipeDescriptors(Collection<Recipe> externalRecipes) {
        Collection<Recipe> internalRecipes = listRecipes();
        Collection<Recipe> allRecipes = Stream.concat(
                externalRecipes.stream(),
                internalRecipes.stream()
        ).collect(toList());

        /*~~>*/List<RecipeDescriptor> recipeDescriptors = new ArrayList<>();
        for (Recipe recipe : internalRecipes) {
            DeclarativeRecipe declarativeRecipe = (DeclarativeRecipe) recipe;
            declarativeRecipe.initialize(allRecipes);
            recipeDescriptors.add(RecipeIntrospectionUtils.recipeDescriptorFromDeclarativeRecipe(declarativeRecipe, source));
        }
        return recipeDescriptors;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<NamedStyles> listStyles() {
        return loadResources(ResourceType.Style).stream()
                .filter(r -> r.containsKey("name"))
                .map(s -> {
                    /*~~>*/List<Style> styles = new ArrayList<>();
                    String name = (String) s.get("name");
                    String displayName = (String) s.get("displayName");
                    if (displayName == null) {
                        displayName = name;
                    }
                    String description = (String) s.get("description");
                    Set<String> tags = Collections.emptySet();
                    /*~~>*/List<String> rawTags = (/*~~>*/List<String>) s.get("tags");
                    if (rawTags != null) {
                        tags = new HashSet<>(rawTags);
                    }
                    DeclarativeNamedStyles namedStyles = new DeclarativeNamedStyles(randomId(), name, displayName, description, tags, styles);
                    /*~~>*/List<Object> styleConfigs = (/*~~>*/List<Object>) s.get("styleConfigs");
                    if (styleConfigs != null) {
                        for (int i = 0; i < styleConfigs.size(); i++) {
                            Object next = styleConfigs.get(i);
                            if (next instanceof String) {
                                String styleClassName = (String) next;
                                try {
                                    styles.add((Style) Class.forName(styleClassName).getDeclaredConstructor().newInstance());
                                } catch (Exception e) {
                                    namedStyles.addValidation(invalid(
                                            name + ".styleConfigs[" + i + "] (in " + source + ")",
                                            next,
                                            "is a style that cannot be constructed.",
                                            e));
                                }
                            } else if (next instanceof Map) {
                                Map.Entry<String, Object> nameAndConfig = ((Map<String, Object>) next).entrySet().iterator().next();
                                try {
                                    Map<Object, Object> withJsonType = new HashMap<>((Map<String, Object>) nameAndConfig.getValue());
                                    withJsonType.put("@c", nameAndConfig.getKey());
                                    withJsonType.put("@ref", refCount++);
                                    Style e = mapper.convertValue(withJsonType, Style.class);
                                    styles.add(e);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            } else {
                                namedStyles.addValidation(invalid(
                                        name + ".styleConfigs[" + i + "] (in " + source + ")",
                                        next,
                                        "is an object type that isn't recognized as a style.",
                                        null));
                            }
                        }
                    }
                    return namedStyles;
                })
                .collect(toList());
    }

    @Override
    public Collection<CategoryDescriptor> listCategoryDescriptors() {
        return loadResources(ResourceType.Category).stream()
                .filter(r -> r.containsKey("name") && r.containsKey("packageName"))
                .map(c -> {
                    String name = (String) c.get("name");
                    String packageName = (String) c.get("packageName");
                    String description = (String) c.get("description");
                    Set<String> tags = Collections.emptySet();
                    @SuppressWarnings("unchecked")
                    /*~~>*/List<String> rawTags = (/*~~>*/List<String>) c.get("tags");
                    if (rawTags != null) {
                        tags = new HashSet<>(rawTags);
                    }
                    return new CategoryDescriptor(name, packageName, description, tags);
                })
                .collect(toList());
    }

    @Override
    public Collection<RecipeExample> listRecipeExamples() {
        return loadResources(ResourceType.Example).stream()
                .map(c -> new RecipeExample(
                        (String) c.get("name"),
                        (String) c.get("description"),
                        (String) c.get("recipe"),
                        (String) c.get("before"),
                        (String) c.get("after"))
                ).collect(toList());
    }
}
