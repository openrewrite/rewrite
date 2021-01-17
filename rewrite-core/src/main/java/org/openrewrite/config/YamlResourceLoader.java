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
import org.openrewrite.Recipe;
import org.openrewrite.Validated;
import org.openrewrite.internal.PropertyPlaceholderHelper;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.style.Style;
import org.yaml.snakeyaml.Yaml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static java.util.stream.Collectors.toList;
import static org.openrewrite.Validated.invalid;

public class YamlResourceLoader implements ResourceLoader {
    private static final ObjectMapper propertyConverter = new ObjectMapper()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private static final PropertyPlaceholderHelper propertyPlaceholderHelper =
            new PropertyPlaceholderHelper("${", "}", ":");

    private final URI source;
    private final String yamlSource;

    private enum ResourceType {
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
        public static ResourceType fromSpec(@Nullable String spec) {
            return Arrays.stream(values())
                    .filter(type -> type.getSpec().equals(spec))
                    .findAny()
                    .orElse(null);
        }
    }

    public YamlResourceLoader(InputStream yamlInput, URI source, Properties properties) throws UncheckedIOException {
        this.source = source;

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

        Yaml yaml = new Yaml();
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
                    DeclarativeRecipe recipe = new DeclarativeRecipe((String) r.get("name"), source);
                    List<Object> recipeList = (List<Object>) r.get("recipeList");
                    for (Object next : recipeList) {
                        if (next instanceof String) {
                            recipe.doNext((String) next);
                        } else if (next instanceof Map) {
                            Map.Entry<String, Object> nameAndConfig = ((Map<String, Object>) next).entrySet().iterator().next();
                            try {
                                Recipe nextRecipe = (Recipe) Class.forName(nameAndConfig.getKey()).getDeclaredConstructor().newInstance();
                                propertyConverter.updateValue(nextRecipe, nameAndConfig.getValue());
                                recipe.doNext(nextRecipe);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            recipe.addValidation(invalid(
                                    "recipeList",
                                    next,
                                    "Invalid object type in list in recipe defined in '" + source + "'",
                                    null));
                        }
                    }
                    return recipe;
                })
                .collect(toList());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<NamedStyles> listStyles() {
        return loadResources(ResourceType.Style).stream()
                .filter(r -> r.containsKey("name"))
                .map(s -> {
                    List<Style> styles = new ArrayList<>();
                    DeclarativeNamedStyles namedStyles = new DeclarativeNamedStyles((String) s.get("name"), styles);
                    List<Map<String, Object>> styleConfigs = (List<Map<String, Object>>) s.get("styleConfigs");
                    if (styleConfigs != null) {
                        for (Map<String, Object> styleConfig : styleConfigs) {
                            Map.Entry<String, Object> nameAndConfig = styleConfig.entrySet().iterator().next();
                            try {
                                styles.add((Style) Class.forName(nameAndConfig.getKey()).getDeclaredConstructor().newInstance());
                            } catch (Exception e) {
                                namedStyles.addValidation(Validated.invalid(
                                        "configure",
                                        nameAndConfig.getKey(),
                                        "Unable to construct named styles '" + namedStyles.getName() + "' defined in '" + source + "'",
                                        e));
                            }
                        }
                    }
                    return namedStyles;
                })
                .collect(toList());
    }
}
