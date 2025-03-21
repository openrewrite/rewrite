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
import java.util.jar.Attributes;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.RecipeSerializer.maybeAddKotlinModule;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.Validated.invalid;

public class YamlResourceLoader implements ResourceLoader {
    int refCount = 0;

    private static final PropertyPlaceholderHelper propertyPlaceholderHelper =
            new PropertyPlaceholderHelper("${", "}", ":");

    @Nullable
    private final URI resourceUri;
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

        public static @Nullable ResourceType fromSpec(@Nullable String spec) {
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
        this(yamlInput, null, source, properties, null, emptyList(), it -> {});
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
    @Deprecated
    public YamlResourceLoader(InputStream yamlInput,
                              URI source,
                              Properties properties,
                              @Nullable ClassLoader classLoader) throws UncheckedIOException {
        this(yamlInput, null, source, properties, classLoader, emptyList(), it -> {});
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
    @Deprecated
    public YamlResourceLoader(InputStream yamlInput,
                              URI source,
                              Properties properties,
                              @Nullable ClassLoader classLoader,
                              Collection<? extends ResourceLoader> dependencyResourceLoaders) throws UncheckedIOException {
        this(yamlInput, null, source, properties, classLoader, dependencyResourceLoaders, jsonMapper -> {});
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
     * @throws UncheckedIOException     On unexpected IOException
     */
    @Deprecated
    public YamlResourceLoader(InputStream yamlInput, URI source, Properties properties,
                              @Nullable ClassLoader classLoader,
                              Collection<? extends ResourceLoader> dependencyResourceLoaders,
                              Consumer<ObjectMapper> mapperCustomizer) {
        this(yamlInput, null, source, properties, classLoader, dependencyResourceLoaders, mapperCustomizer);
    }

    /**
     * Load a declarative recipe, optionally using the specified classloader and optionally including resource loaders
     * for recipes from dependencies.
     *
     * @param yamlInput                 Declarative recipe yaml input stream
     * @param resourceUri               Declarative recipe resourceUri
     * @param source                    Declarative recipe source
     * @param properties                Placeholder properties
     * @param classLoader               Optional classloader to use with jackson. If not specified, the runtime classloader will be used.
     * @param dependencyResourceLoaders Optional resource loaders for recipes from dependencies
     * @param mapperCustomizer          Customizer for the ObjectMapper
     * @throws UncheckedIOException     On unexpected IOException
     */
    public YamlResourceLoader(InputStream yamlInput, @Nullable URI resourceUri, URI source, Properties properties,
                              @Nullable ClassLoader classLoader,
                              Collection<? extends ResourceLoader> dependencyResourceLoaders,
                              Consumer<ObjectMapper> mapperCustomizer) {
        this.resourceUri = resourceUri;
        this.source = source;
        this.dependencyResourceLoaders = dependencyResourceLoaders;
        this.mapper = ObjectMappers.propertyBasedMapper(classLoader);
        this.classLoader = classLoader;

        mapperCustomizer.accept(mapper);
        maybeAddKotlinModule(mapper);


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
                if (resourceType == ResourceType.fromSpec((String) resourceMap.get("type"))) {
                    resources.add(resourceMap);
                }
            }
        }
        return resources;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<DeclarativeRecipe> listRecipes() {
        Collection<Map<String, Object>> resources = loadResources(ResourceType.Recipe);
        List<DeclarativeRecipe> recipes = new ArrayList<>(resources.size());
        Map<String, List<Contributor>> contributors = listContributors();
        for (Map<String, Object> r : resources) {
            if (!r.containsKey("name")) {
                continue;
            }

            @Language("markdown") String name = (String) r.get("name");

            List<Object> recipeList = (List<Object>) r.get("recipeList");
            if (recipeList == null) {
                throw new RecipeException("Invalid Recipe [" + name + "] recipeList is null");
            }

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
            List<Maintainer> maintainers = new ArrayList<>(rawMaintainers.size());
            for (Object rawMaintainer : rawMaintainers) {
                if (rawMaintainer instanceof Map) {
                    Map<String, Object> maintainerMap = (Map<String, Object>) rawMaintainer;
                    String maintainerName = (String) maintainerMap.get("maintainer");
                    String logoString = (String) maintainerMap.get("logo");
                    URI logo = (logoString == null) ? null : URI.create(logoString);
                    maintainers.add(new Maintainer(maintainerName, logo));
                }
            }
            DeclarativeRecipe recipe = new DeclarativeRecipe(
                    name,
                    (String) r.getOrDefault("displayName", name),
                    (String) r.get("description"),
                    tags,
                    estimatedEffortPerOccurrence,
                    source,
                    (boolean) r.getOrDefault("causesAnotherCycle", false),
                    maintainers);

            for (int i = 0; i < recipeList.size(); i++) {
                loadRecipe(name, i, recipeList.get(i), recipe::addUninitialized, recipe::addUninitialized, recipe::addValidation);
            }
            List<Object> preconditions = (List<Object>) r.get("preconditions");
            if (preconditions != null) {
                for (int i = 0; i < preconditions.size(); i++) {
                    loadRecipe(name, i, preconditions.get(i), recipe::addUninitializedPrecondition, recipe::addUninitializedPrecondition, recipe::addValidation);
                }
            }
            recipe.setContributors(contributors.get(recipe.getName()));
            //TODO Set license and source


            /*License license = License.of(attributes.getValue("License-Name"), attributes.getValue("License-Url"));
            descriptor = descriptor
                    .withLicense(license)
                    .withSource(URI.create(gitHubBase + "/" + gitHubDir));*/


            recipes.add(recipe);
        }

        return recipes;
    }

    @SuppressWarnings("unchecked")
    void loadRecipe(@Language("markdown") String name,
                    int index,
                    Object recipeData,
                    Consumer<String> addLazyLoadRecipe,
                    Consumer<Recipe> addRecipe,
                    Consumer<Validated<Object>> addValidation) {
        if (recipeData instanceof String) {
            String recipeName = (String) recipeData;
            try {
                addRecipe.accept(new RecipeLoader(classLoader).load(recipeName, null));
            } catch (IllegalArgumentException ignored) {
                // it's probably declarative
                addLazyLoadRecipe.accept(recipeName);
            } catch (NoClassDefFoundError e) {
                addInvalidRecipeValidation("Recipe class " + recipeName + " cannot be found", addValidation, recipeName, null);
            }
        } else if (recipeData instanceof Map) {
            Map.Entry<String, Object> nameAndConfig = ((Map<String, Object>) recipeData).entrySet().iterator().next();
            String recipeName = nameAndConfig.getKey();
            Object recipeArgs = nameAndConfig.getValue();
            try {
                if (recipeArgs instanceof Map) {
                    try {
                        addRecipe.accept(new RecipeLoader(classLoader).load(recipeName, (Map<String, Object>) recipeArgs));
                    } catch (IllegalArgumentException e) {
                        if (e.getCause() instanceof InvalidTypeIdException) {
                            addInvalidRecipeValidation("Recipe class " + recipeName + " cannot be found", addValidation, recipeName, recipeArgs);
                        } else {
                            addInvalidRecipeValidation("Unable to load Recipe: " + e, addValidation, recipeName, recipeArgs);
                        }
                    } catch (NoClassDefFoundError e) {
                        addInvalidRecipeValidation("Recipe class " + nameAndConfig.getKey() + " cannot be found", addValidation, recipeName, recipeArgs);
                    }
                } else {
                    addInvalidRecipeValidation("Declarative recipeList entries are expected to be strings or mappings", addValidation, recipeName, recipeArgs);
                }
            } catch (Exception e) {
                addInvalidRecipeValidation("Unexpected declarative recipe parsing exception " + e.getClass().getName(), addValidation, recipeName, recipeArgs);
            }
        } else {
            addValidation.accept(invalid(
                    name + ".recipeList[" + index + "] (in " + source + ")",
                    recipeData,
                    "is an object type that isn't recognized as a recipe.",
                    null));
        }
    }

    private void addInvalidRecipeValidation(String message, Consumer<Validated<Object>> addValidation, String property, @Nullable Object value) {
        addValidation.accept(invalid(property, value, message));
    }

    @Override
    public Collection<RecipeDescriptor> listRecipeDescriptors() {
        return listRecipeDescriptors(emptyList(), listContributors(), listRecipeExamples(), emptyMap());
    }

    public Collection<RecipeDescriptor> listRecipeDescriptors(Collection<Recipe> externalRecipes,
                                                              Map<String, List<Contributor>> recipeNamesToContributors,
                                                              Map<String, List<RecipeExample>> recipeNamesToExamples,
                                                              Map<URI, Attributes> artifactManifestAttributes) {
        Collection<DeclarativeRecipe> internalRecipes = listRecipes();
        Collection<Recipe> allRecipes = Stream.concat(
                Stream.concat(
                        externalRecipes.stream(),
                        internalRecipes.stream()
                ),
                dependencyResourceLoaders.stream().flatMap(rl -> rl.listRecipes().stream())
        ).collect(toList());

        List<RecipeDescriptor> recipeDescriptors = new ArrayList<>();
        for (DeclarativeRecipe recipe : internalRecipes) {
            recipe.initialize(allRecipes, recipeNamesToContributors);
            recipe.setExamples(recipeNamesToExamples.get(recipe.getName()));
            RecipeDescriptor descriptor = recipe.getDescriptor();

            Attributes attributes = artifactManifestAttributes.get(resourceUri);
            if (attributes != null) {
                String gitHubBase = attributes.containsKey("Module-Origin") ? attributes.getValue("Module-Origin") : "https://github.com/openrewrite";
                String gitHubDir = attributes.containsKey("Module-Source") ? attributes.getValue("Module-Source") : "";
                License license = License.of(attributes.getValue("License-Name"), attributes.getValue("License-Url"));
                descriptor = descriptor
                        .withLicense(license)
                        .withSource(URI.create(gitHubBase + "/" + gitHubDir));
            }
            recipeDescriptors.add(descriptor);
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
                contributors = emptyMap();
            } else {
                Map<String, List<Contributor>> result = new HashMap<>(rawAttribution.size());
                for (Map<String, Object> attribution : rawAttribution) {
                    String recipeName = (String) attribution.get("recipeName");
                    //noinspection unchecked
                    List<Map<String, Object>> rawContributors = (List<Map<String, Object>>) attribution.get(
                            "contributors");
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
