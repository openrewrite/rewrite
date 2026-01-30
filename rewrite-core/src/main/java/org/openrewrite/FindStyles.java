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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.style.Style;
import org.openrewrite.table.StylesInUse;

import java.util.Collection;
import java.util.List;
import java.util.Map;

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

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile) {
                    SourceFile sourceFile = (SourceFile) tree;
                    List<NamedStyles> namedStylesList = sourceFile.getMarkers().findAll(NamedStyles.class);

                    if (namedStylesList.isEmpty()) {
                        stylesInUse.insertRow(ctx, new StylesInUse.Row(
                                sourceFile.getSourcePath().toString(),
                                "(no styles attached)",
                                ""
                        ));
                        return SearchResult.found(sourceFile, "No styles attached");
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

    private static String stylesToYaml(List<NamedStyles> namedStylesList) {
        // Use NamedStyles.merge() to properly merge styles of the same class
        NamedStyles mergedStyles = NamedStyles.merge(namedStylesList);
        if (mergedStyles == null) {
            return "";
        }

        StringBuilder yaml = new StringBuilder();
        yaml.append("---\n");
        yaml.append("type: specs.openrewrite.org/v1beta/style\n");
        yaml.append("name: ").append(mergedStyles.getName()).append("\n");
        yaml.append("displayName: ").append(mergedStyles.getDisplayName()).append("\n");

        Collection<Style> styles = mergedStyles.getStyles();
        if (styles != null && !styles.isEmpty()) {
            yaml.append("styleConfigs:\n");
            for (Style style : styles) {
                String styleClassName = style.getClass().getName();
                yaml.append("  - ").append(styleClassName).append(":\n");
                appendStyleProperties(yaml, style, "      ");
            }
        }

        return yaml.toString();
    }

    private static boolean isJacksonMetadataKey(String key) {
        return key.startsWith("@");
    }

    private static void appendStyleProperties(StringBuilder yaml, Object obj, String indent) {
        try {
            // Convert the style object to a Map using Jackson
            @SuppressWarnings("unchecked")
            Map<String, Object> props = OBJECT_MAPPER.convertValue(obj, Map.class);

            for (Map.Entry<String, Object> entry : props.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                // Skip null values and Jackson metadata properties (@c, @ref, etc.)
                if (value == null || isJacksonMetadataKey(key)) {
                    continue;
                }

                if (value instanceof Map) {
                    yaml.append(indent).append(key).append(":\n");
                    appendNestedMap(yaml, (Map<?, ?>) value, indent + "  ");
                } else if (value instanceof List) {
                    yaml.append(indent).append(key).append(":\n");
                    appendList(yaml, (List<?>) value, indent + "  ");
                } else {
                    yaml.append(indent).append(key).append(": ").append(formatYamlValue(value)).append("\n");
                }
            }
        } catch (IllegalArgumentException e) {
            yaml.append(indent).append("# Error serializing style: ").append(e.getMessage()).append("\n");
        }
    }

    private static void appendNestedMap(StringBuilder yaml, Map<?, ?> map, String indent) {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = String.valueOf(entry.getKey());
            Object value = entry.getValue();

            // Skip null values and Jackson metadata properties (@c, @ref, etc.)
            if (value == null || isJacksonMetadataKey(key)) {
                continue;
            }

            if (value instanceof Map) {
                yaml.append(indent).append(key).append(":\n");
                appendNestedMap(yaml, (Map<?, ?>) value, indent + "  ");
            } else if (value instanceof List) {
                yaml.append(indent).append(key).append(":\n");
                appendList(yaml, (List<?>) value, indent + "  ");
            } else {
                yaml.append(indent).append(key).append(": ").append(formatYamlValue(value)).append("\n");
            }
        }
    }

    private static void appendList(StringBuilder yaml, List<?> list, String indent) {
        for (Object item : list) {
            if (item instanceof Map) {
                yaml.append(indent).append("-\n");
                appendNestedMap(yaml, (Map<?, ?>) item, indent + "  ");
            } else {
                yaml.append(indent).append("- ").append(formatYamlValue(item)).append("\n");
            }
        }
    }

    private static String formatYamlValue(Object value) {
        if (value instanceof String) {
            String str = (String) value;
            // Quote strings that contain special characters or could be misinterpreted
            if (str.isEmpty() || str.contains(":") || str.contains("#") ||
                str.contains("'") || str.contains("\"") || str.contains("\n") ||
                str.startsWith(" ") || str.endsWith(" ") ||
                "true".equalsIgnoreCase(str) || "false".equalsIgnoreCase(str) ||
                "null".equalsIgnoreCase(str) || "yes".equalsIgnoreCase(str) ||
                "no".equalsIgnoreCase(str) || "on".equalsIgnoreCase(str) ||
                "off".equalsIgnoreCase(str)) {
                // Use single quotes, escaping any existing single quotes
                return "'" + str.replace("'", "''") + "'";
            }
            return str;
        }
        return String.valueOf(value);
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
