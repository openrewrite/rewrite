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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import lombok.Getter;
import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.ObjectMappers;
import org.openrewrite.internal.PropertyPlaceholderHelper;
import org.openrewrite.internal.RecipeLoader;
import org.openrewrite.marketplace.RecipeBundle;
import org.openrewrite.marketplace.RecipeBundleResolver;
import org.openrewrite.marketplace.RecipeListing;
import org.openrewrite.marketplace.RecipeMarketplace;
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
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.RecipeSerializer.maybeAddKotlinModule;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.Validated.invalid;

public class YamlResourceLoader implements ResourceLoader {
    int refCount = 0;

    private static final PropertyPlaceholderHelper propertyPlaceholderHelper =
            new PropertyPlaceholderHelper("${", "}", ":");

    private static final String PRECONDITIONS_KEY = "preconditions";

    private final URI source;
    private final String yamlSource;

    private final ObjectMapper mapper;

    private final Collection<? extends ResourceLoader> dependencyResourceLoaders;

    @Nullable
    private Map<String, List<RecipeExample>> recipeNameToExamples;

    private final BiFunction<String, @Nullable Map<String, Object>, Recipe> recipeLoader;

    @Getter
    private enum ResourceType {
        Recipe("specs.openrewrite.org/v1beta/recipe"),
        Style("specs.openrewrite.org/v1beta/style"),
        Category("specs.openrewrite.org/v1beta/category"),
        Example("specs.openrewrite.org/v1beta/example");

        private final String spec;

        ResourceType(String spec) {
            this.spec = spec;
        }

        public static @Nullable ResourceType fromSpec(@Nullable String spec) {
            return Arrays.stream(values())
                    .filter(type -> type.getSpec().equals(spec))
                    .findAny()
                    .orElse(null);
        }
    }

    public YamlResourceLoader(InputStream yamlInput, URI source, Properties properties, RecipeMarketplace marketplace, Collection<RecipeBundleResolver> resolvers) {
        this.source = source;
        this.dependencyResourceLoaders = emptyList();
        this.mapper = ObjectMappers.propertyBasedMapper(getClass().getClassLoader());
        this.recipeLoader = (recipeName, options) -> {
            RecipeListing listing = marketplace.findRecipe(recipeName);
            if (listing == null) {
                throw new RecipeNotFoundException(recipeName, source);
            }
            return listing.prepare(resolvers, options == null ? emptyMap() : options);
        };

        maybeAddKotlinModule(mapper);

        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[8192];
            while ((nRead = yamlInput.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            this.yamlSource = propertyPlaceholderHelper.replacePlaceholders(
                    new String(buffer.toByteArray(), StandardCharsets.UTF_8), properties);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
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
        this(yamlInput, source, properties, (ClassLoader) null);
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
    public YamlResourceLoader(InputStream yamlInput,
                              URI source,
                              Properties properties,
                              @Nullable ClassLoader classLoader) throws UncheckedIOException {
        this(yamlInput, source, properties, classLoader, emptyList());
    }

    /**
     * Load a declarative recipe, optionally using the specified classloader and optionally including resource loaders
     * for recipes from dependencies.
     *
     * @param yamlInput                 Declarative recipe yaml input stream
     * @param source                    Declarative recipe source
     * @param properties                Placeholder properties
     * @param classLoader               Optional classloader to use with jackson. If not specified, the runtime classloader will be used.
     * @param dependencyResourceLoaders Optional resource loaders for recipes from dependencies
     * @throws UncheckedIOException On unexpected IOException
     */
    public YamlResourceLoader(InputStream yamlInput,
                              URI source,
                              Properties properties,
                              @Nullable ClassLoader classLoader,
                              Collection<? extends ResourceLoader> dependencyResourceLoaders) throws UncheckedIOException {
        this(yamlInput, source, properties, classLoader, dependencyResourceLoaders, jsonMapper -> {
        });
    }

    /**
     * Load a declarative recipe, optionally using the specified classloader and optionally including resource loaders
     * for recipes from dependencies.
     *
     * @param yamlInput                 Declarative recipe yaml input stream
     * @param source                    Declarative recipe source
     * @param properties                Placeholder properties
     * @param classLoader               Optional classloader to use with jackson. If not specified, the runtime classloader will be used.
     * @param dependencyResourceLoaders Optional resource loaders for recipes from dependencies
     * @param mapperCustomizer          Customizer for the ObjectMapper
     * @throws UncheckedIOException On unexpected IOException
     */
    public YamlResourceLoader(InputStream yamlInput, URI source, Properties properties,
                              @Nullable ClassLoader classLoader,
                              Collection<? extends ResourceLoader> dependencyResourceLoaders,
                              Consumer<ObjectMapper> mapperCustomizer) {
        this.source = source;
        this.dependencyResourceLoaders = dependencyResourceLoaders;
        this.mapper = ObjectMappers.propertyBasedMapper(classLoader);

        RecipeLoader nonMarketplaceLoader = new RecipeLoader(classLoader);
        this.recipeLoader = nonMarketplaceLoader::load;

        mapperCustomizer.accept(mapper);
        maybeAddKotlinModule(mapper);

        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[8192];
            while ((nRead = yamlInput.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            this.yamlSource = propertyPlaceholderHelper.replacePlaceholders(
                    new String(buffer.toByteArray(), StandardCharsets.UTF_8), properties);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private final Map<ResourceType, Collection<Map<String, Object>>> resourceCache = new EnumMap<>(ResourceType.class);

    private Collection<Map<String, Object>> loadResources(ResourceType resourceType) {
        return resourceCache.computeIfAbsent(resourceType, rt -> {
            Collection<Map<String, Object>> resources = new ArrayList<>();
            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
            for (Object resource : yaml.loadAll(yamlSource)) {
                if (resource instanceof Map) {
                    @SuppressWarnings("unchecked") Map<String, Object> resourceMap = (Map<String, Object>) resource;
                    if (resourceType == ResourceType.fromSpec((String) resourceMap.get("type"))) {
                        resources.add(resourceMap);
                    }
                }
            }
            return resources;
        });
    }

    @Override
    public @Nullable Recipe loadRecipe(String recipeName, RecipeDetail... details) {
        Collection<Map<String, Object>> resources = loadResources(ResourceType.Recipe);
        for (Map<String, Object> recipeResource : resources) {
            if (!recipeResource.containsKey("name") || !recipeName.equals(recipeResource.get("name"))) {
                continue;
            }
            return mapToRecipe(recipeResource, EnumSet.copyOf(Arrays.asList(details)));
        }
        try {
            return recipeLoader.apply(recipeName, null);
        } catch (IllegalArgumentException | NoClassDefFoundError ignored) {
            // handled by caller
        }
        return null;
    }

    @Override
    public Collection<Recipe> listRecipes() {
        Collection<Map<String, Object>> resources = loadResources(ResourceType.Recipe);
        List<Recipe> recipes = new ArrayList<>(resources.size());
        for (Map<String, Object> r : resources) {
            if (!r.containsKey("name")) {
                continue;
            }

            DeclarativeRecipe recipe = mapToRecipe(r, EnumSet.of(RecipeDetail.MAINTAINERS, RecipeDetail.EXAMPLES));
            recipes.add(recipe);
        }
        return recipes;
    }

    @SuppressWarnings("unchecked")
    private DeclarativeRecipe mapToRecipe(Map<String, Object> yaml, EnumSet<RecipeDetail> details) {
        @Language("markdown") String name = (String) yaml.get("name");

        @Language("markdown")
        String displayName = (String) yaml.get("displayName");
        if (displayName == null) {
            displayName = name;
        }

        @Language("markdown")
        String description = (String) yaml.get("description");

        Set<String> tags = emptySet();
        List<String> rawTags = (List<String>) yaml.get("tags");
        if (rawTags != null) {
            tags = new HashSet<>(rawTags);
        }

        String estimatedEffortPerOccurrenceStr = (String) yaml.get("estimatedEffortPerOccurrence");
        Duration estimatedEffortPerOccurrence = null;
        if (estimatedEffortPerOccurrenceStr != null) {
            estimatedEffortPerOccurrence = Duration.parse(estimatedEffortPerOccurrenceStr);
        }

        List<Maintainer> maintainers;
        if (details.contains(RecipeDetail.MAINTAINERS) && yaml.containsKey("maintainers")) {
            List<Object> rawMaintainers = (List<Object>) yaml.getOrDefault("maintainers", emptyList());
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
        } else {
            maintainers = emptyList();
        }

        DeclarativeRecipe recipe = new DeclarativeRecipe(
                name,
                displayName,
                description,
                tags,
                estimatedEffortPerOccurrence,
                source,
                (boolean) yaml.getOrDefault("causesAnotherCycle", false),
                maintainers);

        List<Object> recipeList = (List<Object>) yaml.get("recipeList");
        if (recipeList == null) {
            throw new RecipeException("Invalid Recipe [" + name + "] recipeList is null");
        }
        for (int i = 0; i < recipeList.size(); i++) {
            loadRecipe(
                    name,
                    i,
                    recipeList.get(i),
                    recipe::addUninitialized,
                    recipe::addUninitialized,
                    recipe::addValidation
            );
        }
        List<Object> preconditions = (List<Object>) yaml.get("preconditions");
        if (preconditions != null) {
            for (int i = 0; i < preconditions.size(); i++) {
                loadRecipe(
                        name,
                        i,
                        preconditions.get(i),
                        recipe::addUninitializedPrecondition,
                        recipe::addUninitializedPrecondition,
                        recipe::addValidation);
            }
        }
        return recipe;
    }

    @SuppressWarnings("unchecked")
    void loadRecipe(@Language("markdown") String name,
                    int i,
                    Object recipeData,
                    Consumer<String> addLazyLoadRecipe,
                    Consumer<Recipe> addRecipe,
                    Consumer<Validated<Object>> addValidation) {
        if (recipeData instanceof String) {
            String recipeName = (String) recipeData;
            try {
                addRecipe.accept(recipeLoader.apply(recipeName, null));
            } catch (IllegalArgumentException ignored) {
                // it's probably declarative
                addLazyLoadRecipe.accept(recipeName);
            } catch (RecipeNotFoundException e) {
                addInvalidRecipeValidation(
                        addValidation,
                        recipeName,
                        null,
                        e.getMessage());
            } catch (NoClassDefFoundError e) {
                addInvalidRecipeValidation(
                        addValidation,
                        recipeName,
                        null,
                        "Recipe class " + recipeName + " cannot be found");
            }
        } else if (recipeData instanceof Map) {
            loadRecipeFromMap(name, i, (Map<String, Object>) recipeData, addRecipe, addValidation);
        } else {
            addValidation.accept(invalid(
                    name + ".recipeList[" + i + "] (in " + source + ")",
                    recipeData,
                    "is an object type that isn't recognized as a recipe.",
                    null));
        }
    }

    @SuppressWarnings("unchecked")
    private void loadRecipeFromMap(@Language("markdown") String name,
                                   int i,
                                   Map<String, Object> recipeMap,
                                   Consumer<Recipe> addRecipe,
                                   Consumer<Validated<Object>> addValidation) {
        Map.Entry<String, Object> nameAndConfig = recipeMap.entrySet().iterator().next();
        String recipeName = nameAndConfig.getKey();
        Object recipeArgs = nameAndConfig.getValue();

        // Extract inline preconditions from recipe args if present
        List<Object> inlinePreconditions = null;
        if (recipeArgs instanceof Map) {
            Map<String, Object> argsMap = (Map<String, Object>) recipeArgs;
            if (argsMap.containsKey(PRECONDITIONS_KEY)) {
                inlinePreconditions = (List<Object>) argsMap.get(PRECONDITIONS_KEY);
                Map<String, Object> cleanedArgs = new LinkedHashMap<>(argsMap);
                cleanedArgs.remove(PRECONDITIONS_KEY);
                recipeArgs = cleanedArgs.isEmpty() ? null : cleanedArgs;
            }
        }

        Recipe loadedRecipe = loadSingleRecipe(name, i, recipeName, recipeArgs, addValidation);
        if (loadedRecipe == null) {
            return;
        }

        if (inlinePreconditions != null && !inlinePreconditions.isEmpty()) {
            DeclarativeRecipe nestedRecipe = new DeclarativeRecipe(
                    name + ".recipeList[" + i + "]",
                    loadedRecipe.getDisplayName(),
                    loadedRecipe.getDescription(),
                    Collections.emptySet(),
                    null,
                    source,
                    false,
                    Collections.emptyList());
            nestedRecipe.addUninitialized(loadedRecipe);

            for (int j = 0; j < inlinePreconditions.size(); j++) {
                loadRecipe(
                        name + ".recipeList[" + i + "].preconditions",
                        j,
                        inlinePreconditions.get(j),
                        nestedRecipe::addUninitializedPrecondition,
                        nestedRecipe::addPrecondition,
                        nestedRecipe::addValidation);
            }

            addRecipe.accept(nestedRecipe);
        } else {
            addRecipe.accept(loadedRecipe);
        }
    }

    @SuppressWarnings("unchecked")
    private @Nullable Recipe loadSingleRecipe(@Language("markdown") String name,
                                              int i,
                                              String recipeName,
                                              @Nullable Object recipeArgs,
                                              Consumer<Validated<Object>> addValidation) {
        try {
            if (recipeArgs instanceof Map) {
                try {
                    return recipeLoader.apply(recipeName, (Map<String, Object>) recipeArgs);
                } catch (IllegalArgumentException e) {
                    if (e.getCause() instanceof InvalidTypeIdException) {
                        addInvalidRecipeValidation(
                                addValidation,
                                recipeName,
                                recipeArgs,
                                "Recipe class " + recipeName + " cannot be found");
                    } else {
                        addInvalidRecipeValidation(
                                addValidation,
                                recipeName,
                                recipeArgs,
                                "Unable to load Recipe: " + e);
                    }
                    return null;
                } catch (RecipeNotFoundException e) {
                    addInvalidRecipeValidation(
                            addValidation,
                            recipeName,
                            recipeArgs,
                            e.getMessage());
                    return null;
                } catch (NoClassDefFoundError e) {
                    addInvalidRecipeValidation(
                            addValidation,
                            recipeName,
                            recipeArgs,
                            "Recipe class " + recipeName + " cannot be found");
                    return null;
                }
            } else if (recipeArgs == null) {
                try {
                    return recipeLoader.apply(recipeName, null);
                } catch (IllegalArgumentException e) {
                    addInvalidRecipeValidation(
                            addValidation,
                            recipeName,
                            null,
                            "Unable to load Recipe: " + e);
                    return null;
                } catch (RecipeNotFoundException e) {
                    addInvalidRecipeValidation(
                            addValidation,
                            recipeName,
                            null,
                            e.getMessage());
                    return null;
                } catch (NoClassDefFoundError e) {
                    addInvalidRecipeValidation(
                            addValidation,
                            recipeName,
                            null,
                            "Recipe class " + recipeName + " cannot be found");
                    return null;
                }
            } else {
                addInvalidRecipeValidation(
                        addValidation,
                        recipeName,
                        recipeArgs,
                        "Declarative recipeList entries are expected to be strings or mappings");
                return null;
            }
        } catch (Exception e) {
            addInvalidRecipeValidation(
                    addValidation,
                    recipeName,
                    recipeArgs,
                    "Unexpected declarative recipe parsing exception " + e.getClass().getName() + ": " + e.getMessage());
            return null;
        }
    }

    private void addInvalidRecipeValidation(Consumer<Validated<Object>> addValidation, String recipeName,
                                            @Nullable Object recipeArgs, String message) {
        addValidation.accept(Validated.invalid(recipeName, recipeArgs, message));
    }

    public Collection<RecipeListing> listRecipeListings(RecipeBundle bundle) {
        Collection<Map<String, Object>> resources = loadResources(ResourceType.Recipe);
        List<RecipeListing> recipeListings = new ArrayList<>(resources.size());
        for (Map<String, Object> yaml : resources) {
            if (!yaml.containsKey("name")) {
                continue;
            }

            @Language("markdown") String name = (String) yaml.get("name");

            @Language("markdown")
            String displayName = (String) yaml.get("displayName");
            if (displayName == null) {
                displayName = name;
            }

            @Language("markdown")
            String description = (String) yaml.get("description");

            String estimatedEffortPerOccurrenceStr = (String) yaml.get("estimatedEffortPerOccurrence");
            Duration estimatedEffortPerOccurrence = null;
            if (estimatedEffortPerOccurrenceStr != null) {
                estimatedEffortPerOccurrence = Duration.parse(estimatedEffortPerOccurrenceStr);
            }

            recipeListings.add(new RecipeListing(
                    null,
                    name,
                    displayName,
                    description,
                    estimatedEffortPerOccurrence,
                    emptyList(),
                    emptyList(),
                    0,
                    bundle
            ));
        }
        return recipeListings;
    }

    @Override
    public Collection<RecipeDescriptor> listRecipeDescriptors() {
        return listRecipeDescriptors(emptyList(), listRecipeExamples());
    }

    public Collection<RecipeDescriptor> listRecipeDescriptors(Collection<Recipe> externalRecipes,
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
            declarativeRecipe.initialize(allRecipes);
            declarativeRecipe.setExamples(recipeNamesToExamples.get(recipe.getName()));
            recipeDescriptors.add(declarativeRecipe.getDescriptor());
        }
        return recipeDescriptors;

    }

    @SuppressWarnings("unused")
    @Deprecated
    public Collection<RecipeDescriptor> listRecipeDescriptors(Collection<Recipe> externalRecipes,
                                                              Map<String, List<Contributor>> recipeNamesToContributors,
                                                              Map<String, List<RecipeExample>> recipeNamesToExamples) {
        return listRecipeDescriptors(externalRecipes, recipeNamesToExamples);
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
                    Set<String> tags = emptySet();
                    List<String> rawTags = (List<String>) s.get("tags");
                    if (rawTags != null) {
                        tags = new HashSet<>(rawTags);
                    }
                    DeclarativeNamedStyles namedStyles = new DeclarativeNamedStyles(
                            randomId(),
                            name,
                            displayName,
                            description,
                            tags,
                            styles);
                    List<Object> styleConfigs = (List<Object>) s.get("styleConfigs");
                    if (styleConfigs != null) {
                        for (int i = 0; i < styleConfigs.size(); i++) {
                            Object next = styleConfigs.get(i);
                            if (next instanceof String) {
                                String styleClassName = (String) next;
                                try {
                                    styles.add((Style) Class.forName(styleClassName)
                                            .getDeclaredConstructor()
                                            .newInstance());
                                } catch (Exception e) {
                                    namedStyles.addValidation(invalid(
                                            name + ".styleConfigs[" + i + "] (in " + source + ")",
                                            next,
                                            "is a style that cannot be constructed.",
                                            e));
                                }
                            } else if (next instanceof Map) {
                                Map.Entry<String, Object> nameAndConfig = ((Map<String, Object>) next).entrySet()
                                        .iterator()
                                        .next();
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
                    if (name == null) {
                        name = packageName;
                    }

                    @Language("markdown")
                    String description = (String) c.get("description");

                    Set<String> tags = emptySet();
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
        return emptyMap();
    }
}
