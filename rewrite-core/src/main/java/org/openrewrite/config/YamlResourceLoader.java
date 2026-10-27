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

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
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
import org.openrewrite.rpc.RpcRecipe;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.style.Style;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
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

    private final URI source;
    private final String yamlSource;

    private final ObjectMapper mapper;

    private final Collection<? extends ResourceLoader> dependencyResourceLoaders;

    private final BiFunction<String, @Nullable Map<String, Object>, Recipe> recipeLoader;

    @Getter
    private enum ResourceType {
        Recipe("specs.openrewrite.org/v1beta/recipe"),
        Style("specs.openrewrite.org/v1beta/style"),
        Category("specs.openrewrite.org/v1beta/category");

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

    public YamlResourceLoader(InputStream yamlInput, URI source, @Nullable Properties properties, RecipeMarketplace marketplace, Collection<RecipeBundleResolver> resolvers) {
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

        this.yamlSource = readYamlSource(yamlInput, properties);
    }

    /**
     * Load a declarative recipe using the runtime classloader
     *
     * @param yamlInput  Declarative recipe yaml input stream
     * @param source     Declarative recipe source
     * @param properties Placeholder properties
     * @throws UncheckedIOException On unexpected IOException
     */
    public YamlResourceLoader(InputStream yamlInput, URI source, @Nullable Properties properties) throws UncheckedIOException {
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
                              @Nullable Properties properties,
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
                              @Nullable Properties properties,
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
    public YamlResourceLoader(InputStream yamlInput, URI source, @Nullable Properties properties,
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

        this.yamlSource = readYamlSource(yamlInput, properties);
    }

    private static String readYamlSource(InputStream yamlInput, @Nullable Properties properties) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[8192];
            while ((nRead = yamlInput.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            String raw = new String(buffer.toByteArray(), StandardCharsets.UTF_8);
            return properties != null ? propertyPlaceholderHelper.replacePlaceholders(raw, properties) : raw;
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
            return mapToRecipe(recipeResource);
        }
        try {
            return recipeLoader.apply(recipeName, null);
        } catch (IllegalArgumentException | RecipeNotFoundException | NoClassDefFoundError ignored) {
            // A recipe this loader cannot supply is reported as absent so that the caller can
            // consult the remaining resource loaders.
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

            DeclarativeRecipe recipe = mapToRecipe(r);
            recipes.add(recipe);
        }
        return recipes;
    }

    @SuppressWarnings("unchecked")
    private DeclarativeRecipe mapToRecipe(Map<String, Object> yaml) {
        @Language("markdown") String name = (String) yaml.get("name");

        @Language("markdown")
        String displayName = (String) yaml.get("displayName");
        if (displayName == null) {
            displayName = name;
        }

        @Language("markdown")
        String description = (String) yaml.get("description");

        Set<String> tags = emptySet();
        List<Object> invalidTags = emptyList();
        List<Object> rawTags = (List<Object>) yaml.get("tags");
        if (rawTags != null) {
            tags = new HashSet<>(rawTags.size());
            for (Object rawTag : rawTags) {
                if (rawTag instanceof String) {
                    tags.add((String) rawTag);
                } else {
                    if (invalidTags.isEmpty()) {
                        invalidTags = new ArrayList<>();
                    }
                    invalidTags.add(rawTag);
                }
            }
        }

        String estimatedEffortPerOccurrenceStr = (String) yaml.get("estimatedEffortPerOccurrence");
        Duration estimatedEffortPerOccurrence = null;
        if (estimatedEffortPerOccurrenceStr != null) {
            estimatedEffortPerOccurrence = Duration.parse(estimatedEffortPerOccurrenceStr);
        }

        DeclarativeRecipe recipe = new DeclarativeRecipe(
                name,
                displayName,
                description,
                tags,
                estimatedEffortPerOccurrence,
                source,
                (boolean) yaml.getOrDefault("causesAnotherCycle", false),
                emptyList());

        for (Object invalidTag : invalidTags) {
            recipe.addValidation(invalid(
                    name + ".tags",
                    invalidTag,
                    "tags must be a list of strings, but found a " +
                            (invalidTag == null ? "null value" : invalidTag.getClass().getSimpleName())));
        }

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
            } catch (LinkageError e) {
                addInvalidRecipeValidation(
                        addValidation,
                        recipeName,
                        null,
                        "Recipe class " + recipeName + " could not be loaded: " + e,
                        e);
            }
        } else if (recipeData instanceof Map) {
            Map.Entry<String, Object> nameAndConfig = ((Map<String, Object>) recipeData).entrySet().iterator().next();
            String recipeName = nameAndConfig.getKey();
            Object recipeArgs = nameAndConfig.getValue();
            try {
                if (recipeArgs instanceof Map) {
                    Map<String, Object> options = (Map<String, Object>) recipeArgs;
                    try {
                        Recipe recipe = recipeLoader.apply(recipeName, options);
                        addRecipe.accept(recipe);
                        validateOptionNames(recipeName, recipe, options, addValidation);
                    } catch (IllegalArgumentException e) {
                        if (e.getCause() instanceof InvalidTypeIdException) {
                            addInvalidRecipeValidation(
                                    addValidation,
                                    recipeName,
                                    recipeArgs,
                                    "Recipe class " + recipeName + " cannot be found",
                                    e);
                        } else {
                            addInvalidRecipeValidation(
                                    addValidation,
                                    recipeName,
                                    recipeArgs,
                                    "Unable to load Recipe: " + e);
                        }
                    } catch (RecipeNotFoundException e) {
                        addInvalidRecipeValidation(
                                addValidation,
                                recipeName,
                                recipeArgs,
                                e.getMessage());
                    } catch (LinkageError e) {
                        addInvalidRecipeValidation(
                                addValidation,
                                recipeName,
                                recipeArgs,
                                "Recipe class " + nameAndConfig.getKey() + " could not be loaded: " + e,
                                e);
                    }
                } else {
                    addInvalidRecipeValidation(
                            addValidation,
                            recipeName,
                            recipeArgs,
                            "Declarative recipeList entries are expected to be strings or mappings");
                }
            } catch (Exception e) {
                addInvalidRecipeValidation(
                        addValidation,
                        recipeName,
                        recipeArgs,
                        "Unexpected declarative recipe parsing exception " + e.getClass().getName() + ": " + e.getMessage());
            }
        } else {
            addValidation.accept(invalid(
                    name + ".recipeList[" + i + "] (in " + source + ")",
                    recipeData,
                    "is an object type that isn't recognized as a recipe.",
                    null));
        }
    }

    private void validateOptionNames(String recipeName, Recipe recipe, Map<String, Object> options,
                                     Consumer<Validated<Object>> addValidation) {
        if (recipe instanceof RpcRecipe) {
            return;
        }
        Set<String> optionNames = emptySet();
        Set<String> ignoredNames = emptySet();
        if (!(recipe instanceof DeclarativeRecipe)) {
            try {
                DeserializationConfig config = mapper.getDeserializationConfig();
                BeanDescription description = config.introspect(mapper.constructType(recipe.getClass()));
                if (config.getDefaultPropertyIgnorals(recipe.getClass(), description.getClassInfo()).getIgnoreUnknown() ||
                    description.findAnySetterAccessor() != null) {
                    return;
                }
                optionNames = optionNames(recipe.getClass(), description);
                ignoredNames = description.getIgnoredPropertyNames();
            } catch (RuntimeException | LinkageError e) {
                // Introspection resolves member signatures that construction never touched, so failing
                // here must not invalidate a recipe that loaded fine
                return;
            }
        }
        for (Map.Entry<String, Object> option : options.entrySet()) {
            if (!optionNames.contains(option.getKey()) && !ignoredNames.contains(option.getKey())) {
                addValidation.accept(invalid(
                        recipeName + "." + option.getKey(),
                        option.getValue(),
                        optionNames.isEmpty() ?
                                "is not an option of this recipe, which takes no options" :
                                "is not an option of this recipe; valid options are: " + String.join(", ", optionNames)));
            }
        }
    }

    private static Set<String> optionNames(Class<?> recipeClass, BeanDescription description) {
        Set<String> names = new LinkedHashSet<>();
        for (Constructor<?> constructor : recipeClass.getConstructors()) {
            for (Parameter parameter : constructor.getParameters()) {
                if (parameter.isNamePresent()) {
                    names.add(parameter.getName());
                }
            }
        }
        for (Class<?> c = recipeClass; c != null && c != Recipe.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (field.isAnnotationPresent(Option.class)) {
                    names.add(field.getName());
                }
            }
        }
        for (BeanPropertyDefinition property : description.findProperties()) {
            if (property.couldDeserialize() && property.getPrimaryMember().getDeclaringClass() != Recipe.class) {
                names.add(property.getName());
                for (PropertyName alias : property.findAliases()) {
                    names.add(alias.getSimpleName());
                }
            }
        }
        return names;
    }

    private void addInvalidRecipeValidation(Consumer<Validated<Object>> addValidation, String recipeName,
                                            @Nullable Object recipeArgs, String message) {
        addInvalidRecipeValidation(addValidation, recipeName, recipeArgs, message, null);
    }

    private void addInvalidRecipeValidation(Consumer<Validated<Object>> addValidation, String recipeName,
                                            @Nullable Object recipeArgs, String message,
                                            @Nullable Throwable exception) {
        addValidation.accept(Validated.invalid(recipeName, recipeArgs, message, exception));
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
        return listRecipeDescriptors(emptyList());
    }

    public Collection<RecipeDescriptor> listRecipeDescriptors(Collection<Recipe> externalRecipes) {
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
            recipeDescriptors.add(declarativeRecipe.getDescriptor());
        }
        return recipeDescriptors;

    }

    @Deprecated
    public Collection<RecipeDescriptor> listRecipeDescriptors(Collection<Recipe> externalRecipes,
                                                              Map<String, List<RecipeExample>> recipeNamesToExamples) {
        return listRecipeDescriptors(externalRecipes);
    }

    @SuppressWarnings("unused")
    @Deprecated
    public Collection<RecipeDescriptor> listRecipeDescriptors(Collection<Recipe> externalRecipes,
                                                              Map<String, List<Contributor>> recipeNamesToContributors,
                                                              Map<String, List<RecipeExample>> recipeNamesToExamples) {
        return listRecipeDescriptors(externalRecipes);
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
        return emptyMap();
    }

    @Override
    public Map<String, List<Contributor>> listContributors() {
        return emptyMap();
    }
}
