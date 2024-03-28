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
import com.fasterxml.jackson.databind.cfg.ConstructorDetector;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import lombok.Getter;
import org.intellij.lang.annotations.Language;
import org.openrewrite.*;
import org.openrewrite.internal.PropertyPlaceholderHelper;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.style.Style;
import org.yaml.snakeyaml.LoaderOptions;
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
import java.util.function.Consumer;
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

    @Nullable
    private final ClassLoader classLoader;
    private final Collection<? extends ResourceLoader> dependencyResourceLoaders;

    @Nullable
    private Map<String, List<Contributor>> contributors;

    @Nullable
    private Map<String, List<RecipeExample>> recipeNameToExamples;

    @Getter
    private enum ResourceType {
        Recipe("specs.openrewrite.org/v1beta/recipe"),
        Style("specs.openrewrite.org/v1beta/style"),
        Category("specs.openrewrite.org/v1beta/category"),
        Example("specs.openrewrite.org/v1beta/example"),
        Attribution("specs.openrewrite.org/v1beta/attribution");

        private final String spec;

        ResourceType(String spec) {
            this.spec = spec;
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
        this(yamlInput, source, properties, classLoader, emptyList());
    }

    /**
     * Load a declarative recipe, optionally using the specified classloader and optionally including resource loaders
     * for recipes from dependencies.
     *
     * @param yamlInput   Declarative recipe yaml input stream
     * @param source      Declarative recipe source
     * @param properties  Placeholder properties
     * @param classLoader Optional classloader to use with jackson. If not specified, the runtime classloader will be used.
     * @throws UncheckedIOException On unexpected IOException
     */
    public YamlResourceLoader(InputStream yamlInput, URI source, Properties properties, @Nullable ClassLoader classLoader, Collection<? extends ResourceLoader> dependencyResourceLoaders) throws UncheckedIOException {
        this.source = source;
        this.dependencyResourceLoaders = dependencyResourceLoaders;

        mapper = JsonMapper.builder()
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .constructorDetector(ConstructorDetector.USE_PROPERTIES_BASED)
                .build()
                .registerModule(new ParameterNamesModule())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        maybeAddKotlinModule(mapper);

        this.classLoader = classLoader;

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
        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
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
        Collection<Map<String, Object>> resources = loadResources(ResourceType.Recipe);
        List<Recipe> recipes = new ArrayList<>(resources.size());
        Map<String, List<Contributor>> contributors = listContributors();
        for (Map<String, Object> r : resources) {
            if (!r.containsKey("name")) {
                continue;
            }

            @Language("markdown") String name = (String) r.get("name");

            @Language("markdown")
            String displayName = (String) r.get("displayName");
            if (displayName == null) {
                displayName = name;
            }

            @Language("markdown")
            String description = (String) r.get("description");

            Set<String> tags = Collections.emptySet();
            List<String> rawTags = (List<String>) r.get("tags");
            if (rawTags != null) {
                tags = new HashSet<>(rawTags);
            }

            String estimatedEffortPerOccurrenceStr = (String) r.get("estimatedEffortPerOccurrence");
            Duration estimatedEffortPerOccurrence = null;
            if (estimatedEffortPerOccurrenceStr != null) {
                estimatedEffortPerOccurrence = Duration.parse(estimatedEffortPerOccurrenceStr);
            }

            List<Object> rawMaintainers = (List<Object>) r.getOrDefault("maintainers", emptyList());
            List<Maintainer> maintainers;
            if (rawMaintainers.isEmpty()) {
                maintainers = emptyList();
            } else {
                maintainers = new ArrayList<>(rawMaintainers.size());
                for (Object rawMaintainer : rawMaintainers) {
                    if (rawMaintainer instanceof Map) {
                        Map<String, Object> maintainerMap = (Map<String, Object>) rawMaintainer;
                        String maintainerName = (String) maintainerMap.get("maintainer");
                        String logoString = (String) maintainerMap.get("logo");
                        URI logo = (logoString == null) ? null : URI.create(logoString);
                        maintainers.add(new Maintainer(maintainerName, logo));
                    }
                }
            }
            DeclarativeRecipe recipe = new DeclarativeRecipe(name, displayName, description, tags,
                    estimatedEffortPerOccurrence, source, (boolean) r.getOrDefault("causesAnotherCycle", false), maintainers);

            List<Object> recipeList = (List<Object>) r.get("recipeList");
            if (recipeList == null) {
                throw new RecipeException("Invalid Recipe [" + name + "] recipeList is null");
            }
            for (int i = 0; i < recipeList.size(); i++) {
                loadRecipe(name, i, recipeList.get(i), recipe::addUninitialized, recipe::addUninitialized, recipe::addValidation);
            }
            List<Object> preconditions = (List<Object>) r.get("preconditions");
            if(preconditions != null) {
                for (int i = 0; i < preconditions.size(); i++) {
                    loadRecipe(name, i, preconditions.get(i), recipe::addUninitializedPrecondition, recipe::addUninitializedPrecondition, recipe::addValidation);
                }
            }
            recipe.setContributors(contributors.get(recipe.getName()));
            recipes.add(recipe);
        }

        return recipes;
    }

    @SuppressWarnings("unchecked")
    private void loadRecipe(@Language("markdown") String name,
                            int i,
                            Object recipeData,
                            Consumer<String> addLazyLoadRecipe,
                            Consumer<Recipe> addRecipe,
                            Consumer<Validated<Object>> addValidation) {
        if (recipeData instanceof String) {
            String recipeName = (String) recipeData;
            try {

                // first try an explicitly-declared zero-arg constructor
                addRecipe.accept((Recipe) Class.forName(recipeName, true,
                                classLoader == null ? this.getClass().getClassLoader() : classLoader)
                        .getDeclaredConstructor()
                        .newInstance());
            } catch (ReflectiveOperationException reflectiveOperationException) {
                try {
                    // then try jackson
                    addRecipe.accept(instantiateRecipe(recipeName, new HashMap<>()));
                } catch (IllegalArgumentException illegalArgumentException) {
                    // else, it's probably declarative
                    addLazyLoadRecipe.accept(recipeName);
                }
            }
        } else if (recipeData instanceof Map) {
            Map.Entry<String, Object> nameAndConfig = ((Map<String, Object>) recipeData).entrySet().iterator().next();
            try {
                if (nameAndConfig.getValue() instanceof Map) {
                    try {
                        addRecipe.accept(instantiateRecipe(nameAndConfig.getKey(),
                                (Map<String, Object>) nameAndConfig.getValue()));
                    } catch (IllegalArgumentException e) {
                        if (e.getCause() instanceof InvalidTypeIdException) {
                            addValidation.accept(Validated.invalid(nameAndConfig.getKey(),
                                    nameAndConfig.getValue(), "Recipe class " +
                                                              nameAndConfig.getKey() + " cannot be found"));
                        } else {
                            addValidation.accept(Validated.invalid(nameAndConfig.getKey(), nameAndConfig.getValue(),
                                    "Unable to load Recipe: " + e));
                        }
                    }
                } else {
                    addValidation.accept(Validated.invalid(nameAndConfig.getKey(),
                            nameAndConfig.getValue(),
                            "Declarative recipeList entries are expected to be strings or mappings"));
                }
            } catch (Exception e) {
                addValidation.accept(Validated.invalid(nameAndConfig.getKey(), nameAndConfig.getValue(),
                        "Unexpected declarative recipe parsing exception " +
                        e.getClass().getName()));
            }
        } else {
            addValidation.accept(invalid(
                    name + ".recipeList[" + i + "] (in " + source + ")",
                    recipeData,
                    "is an object type that isn't recognized as a recipe.",
                    null));
        }
    }

    private Recipe instantiateRecipe(String recipeName, Map<String, Object> args) throws IllegalArgumentException {
        Map<Object, Object> withJsonType = new HashMap<>(args);
        withJsonType.put("@c", recipeName);
        return mapper.convertValue(withJsonType, Recipe.class);
    }

    @Override
    public Collection<RecipeDescriptor> listRecipeDescriptors() {
        return listRecipeDescriptors(emptyList(), listContributors(), listRecipeExamples());
    }

    public Collection<RecipeDescriptor> listRecipeDescriptors(Collection<Recipe> externalRecipes,
                                                              Map<String, List<Contributor>> recipeNamesToContributors,
                                                              Map<String, List<RecipeExample>> recipeNamesToExamples) {
        Collection<Recipe> internalRecipes = listRecipes();
        Collection<Recipe> allRecipes = Stream.concat(
                Stream.concat(
                        externalRecipes.stream(),
                        internalRecipes.stream()
                ),
                dependencyResourceLoaders.stream().flatMap(rl -> rl.listRecipes().stream())
        ).collect(toList());

        List<RecipeDescriptor> recipeDescriptors = new ArrayList<>();
        for (Recipe recipe : internalRecipes) {
            DeclarativeRecipe declarativeRecipe = (DeclarativeRecipe) recipe;
            declarativeRecipe.initialize(allRecipes, recipeNamesToContributors);
            declarativeRecipe.setContributors(recipeNamesToContributors.get(recipe.getName()));
            declarativeRecipe.setExamples(recipeNamesToExamples.get(recipe.getName()));
            recipeDescriptors.add(declarativeRecipe.getDescriptor());
        }
        return recipeDescriptors;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<NamedStyles> listStyles() {
        return loadResources(ResourceType.Style).stream()
                .filter(r -> r.containsKey("name"))
                .map(s -> {
                    List<Style> styles = new ArrayList<>();
                    String name = (String) s.get("name");
                    String displayName = (String) s.get("displayName");
                    if (displayName == null) {
                        displayName = name;
                    }
                    String description = (String) s.get("description");
                    Set<String> tags = Collections.emptySet();
                    List<String> rawTags = (List<String>) s.get("tags");
                    if (rawTags != null) {
                        tags = new HashSet<>(rawTags);
                    }
                    DeclarativeNamedStyles namedStyles = new DeclarativeNamedStyles(randomId(), name, displayName, description, tags, styles);
                    List<Object> styleConfigs = (List<Object>) s.get("styleConfigs");
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
                                    namedStyles.addValidation(invalid(
                                            name + ".styleConfigs[" + i + "] (in " + source + ")",
                                            next,
                                            " encountered an error being loaded as a style.",
                                            e));
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
                .filter(r -> r.containsKey("packageName"))
                .map(c -> {
                    @Language("markdown")
                    String name = (String) c.get("name");

                    @Language("markdown")
                    String packageName = (String) c.get("packageName");
                    if (packageName.endsWith("." + CategoryTree.CORE) ||
                        packageName.contains("." + CategoryTree.CORE + ".")) {
                        throw new IllegalArgumentException("The package name 'core' is reserved.");
                    }

                    if (name == null) {
                        name = packageName;
                    }

                    @Language("markdown")
                    String description = (String) c.get("description");

                    Set<String> tags = Collections.emptySet();
                    @SuppressWarnings("unchecked")
                    List<String> rawTags = (List<String>) c.get("tags");
                    if (rawTags != null) {
                        tags = new HashSet<>(rawTags);
                    }

                    boolean root = c.containsKey("root") && (Boolean) c.get("root");
                    int priority = c.containsKey("priority") ? (Integer) c.get("priority") : CategoryDescriptor.DEFAULT_PRECEDENCE;

                    return new CategoryDescriptor(name, packageName, description, tags, root, priority, false);
                })
                .collect(toList());
    }

    @Override
    public Map<String, List<RecipeExample>> listRecipeExamples() {
        if (recipeNameToExamples == null) {
            recipeNameToExamples = new HashMap<>();
        }

        Collection<Map<String, Object>> rawExamples = loadResources(ResourceType.Example);

        for (Map<String, Object> examplesMap : rawExamples) {
            String recipeName = (String) examplesMap.get("recipeName");
            recipeNameToExamples.computeIfAbsent(recipeName, key -> new ArrayList<>());


            @SuppressWarnings("unchecked") List<Map<String, Object>> examples =
                    (List<Map<String, Object>>) examplesMap.get("examples");

            List<RecipeExample> newExamples = examples.stream().map(exam -> {
                        RecipeExample recipeExample = new RecipeExample();
                        recipeExample.setDescription((String) exam.get("description"));

                        if (exam.get("parameters") != null) {
                            //noinspection unchecked
                            recipeExample.setParameters(((List<Object>) exam.get("parameters")).stream()
                                    .filter(Objects::nonNull)
                                    .map(Object::toString).collect(toList()));
                        }

                        List<RecipeExample.Source> sources = new ArrayList<>();
                        //noinspection unchecked
                        List<Object> ss = (List<Object>) exam.get("sources");
                        if (ss != null) {
                            for (Object s : ss) {
                                //noinspection rawtypes
                                HashMap sMap = (HashMap) s;
                                String before = (String) sMap.get("before");
                                String after = (String) sMap.get("after");
                                String path = (String) sMap.get("path");
                                //noinspection rawtypes
                                String language = (String) ((HashMap) s).get("language");

                                RecipeExample.Source source = new RecipeExample.Source(before, after, path, language);
                                sources.add(source);
                            }
                        }

                        recipeExample.setSources(sources);
                        return recipeExample;
                    }
            ).collect(toList());

            recipeNameToExamples.get(recipeName).addAll(newExamples);
        }

        return recipeNameToExamples;
    }

    @Override
    public Map<String, List<Contributor>> listContributors() {
        if (contributors == null) {
            Collection<Map<String, Object>> rawAttribution = loadResources(ResourceType.Attribution);
            if (rawAttribution.isEmpty()) {
                contributors = Collections.emptyMap();
            } else {
                Map<String, List<Contributor>> result = new HashMap<>(rawAttribution.size());
                for (Map<String, Object> attribution : rawAttribution) {
                    String recipeName = (String) attribution.get("recipeName");

                    //noinspection unchecked
                    List<Map<String, Object>> rawContributors = (List<Map<String, Object>>) attribution.get("contributors");
                    List<Contributor> contributors = new ArrayList<>(rawContributors.size());
                    for (Map<String, Object> rawContributor : rawContributors) {
                        contributors.add(new Contributor(
                                (String) rawContributor.get("name"),
                                (String) rawContributor.get("email"),
                                (int) rawContributor.get("lineCount")
                        ));
                    }
                    result.put(recipeName, contributors);
                }
                contributors = result;
            }
        }
        return contributors;

    }
}
