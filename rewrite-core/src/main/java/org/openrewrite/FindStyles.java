/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.internal.ObjectMappers;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.style.Style;
import org.openrewrite.table.StylesInUse;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.util.*;

/**
 * A diagnostic recipe that finds and reports the styles attached to each source file.
 * <p>
 * This is useful for troubleshooting style detection issues, such as when formatting
 * produces unexpected results. The styles are output as valid OpenRewrite style YAML
 * in the SearchResult marker, which can be directly used in a rewrite.yml configuration.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class FindStyles extends Recipe {
    transient StylesInUse stylesInUse = new StylesInUse(this);

    String displayName = "Find styles";

    String description = "Find and report the styles attached to each source file. " +
                         "Styles are output as valid OpenRewrite style YAML that can be used directly in rewrite.yml configuration.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile) {
                    SourceFile sourceFile = (SourceFile) tree;
                    List<NamedStyles> namedStylesList = sourceFile.getMarkers().findAll(NamedStyles.class);

                    if (namedStylesList.isEmpty()) {
                        return sourceFile;
                    }

                    String yaml = stylesToYaml(namedStylesList);
                    String styleNames = getStyleNames(namedStylesList);

                    stylesInUse.insertRow(ctx, new StylesInUse.Row(
                            sourceFile.getSourcePath().toString(),
                            styleNames,
                            yaml
                    ));

                    return SearchResult.found(sourceFile, yaml);
                }
                return tree;
            }
        };
    }

    private static final Yaml SNAKE_YAML;
    private static final ObjectMapper MAPPER = ObjectMappers.propertyBasedMapper(FindStyles.class.getClassLoader());
    private static final TypeReference<Map<String, Object>> STYLE_MAP = new TypeReference<Map<String, Object>>() {};

    static {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setIndent(2);
        options.setPrettyFlow(true);
        SNAKE_YAML = new Yaml(options);
    }

    private static String stylesToYaml(List<NamedStyles> namedStylesList) {
        // Use NamedStyles.merge() to properly merge styles of the same class
        NamedStyles mergedStyles = NamedStyles.merge(namedStylesList);
        if (mergedStyles == null) {
            return "";
        }

        // Build the document structure that matches the rewrite.yml style format
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("type", "specs.openrewrite.org/v1beta/style");
        doc.put("name", mergedStyles.getName());
        doc.put("displayName", mergedStyles.getDisplayName());

        Collection<Style> styles = mergedStyles.getStyles();
        if (styles != null && !styles.isEmpty()) {
            List<Map<String, Object>> styleConfigs = new ArrayList<>();
            for (Style style : styles) {
                Map<String, Object> entry = new LinkedHashMap<>();
                // Round-trip through Jackson so any custom @JsonSerialize on the Style
                // (e.g. ImportLayoutStyle.Serializer) is honored before SnakeYAML sees it.
                entry.put(style.getClass().getName(), MAPPER.convertValue(style, STYLE_MAP));
                styleConfigs.add(entry);
            }
            doc.put("styleConfigs", styleConfigs);
        }

        return "---\n" + SNAKE_YAML.dump(doc);
    }

    private static String getStyleNames(List<NamedStyles> namedStylesList) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < namedStylesList.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(namedStylesList.get(i).getName());
        }
        return sb.toString();
    }
}
