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

import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A diagnostic recipe that finds and reports the styles attached to each source file.
 * <p>
 * This is useful for troubleshooting style detection issues, such as when formatting
 * produces unexpected results. The styles are output as JSON in the SearchResult marker,
 * making them easy to analyze and potentially reuse in other recipes.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class FindStyles extends Recipe {
    transient StylesInUse stylesInUse = new StylesInUse(this);

    String displayName = "Find styles";

    String description = "Find and report the styles attached to each source file. " +
                         "Styles are output as JSON in a SearchResult marker for analysis and debugging.";

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
                                "{}"
                        ));
                        return SearchResult.found(sourceFile, "No styles attached");
                    }

                    String json = stylesToJson(namedStylesList);
                    String styleNames = getStyleNames(namedStylesList);

                    stylesInUse.insertRow(ctx, new StylesInUse.Row(
                            sourceFile.getSourcePath().toString(),
                            styleNames,
                            json
                    ));

                    return SearchResult.found(sourceFile, json);
                }
                return tree;
            }
        };
    }

    private static String stylesToJson(List<NamedStyles> namedStylesList) {
        try {
            Map<String, Object> output = new LinkedHashMap<>();
            for (NamedStyles namedStyles : namedStylesList) {
                Map<String, Object> stylesMap = new LinkedHashMap<>();
                stylesMap.put("displayName", namedStyles.getDisplayName());
                stylesMap.put("description", namedStyles.getDescription());

                Map<String, Object> individualStyles = new LinkedHashMap<>();
                Collection<Style> styles = namedStyles.getStyles();
                if (styles != null) {
                    for (Style style : styles) {
                        individualStyles.put(style.getClass().getSimpleName(), style);
                    }
                }
                stylesMap.put("styles", individualStyles);

                output.put(namedStyles.getName(), stylesMap);
            }
            return OBJECT_MAPPER.writeValueAsString(output);
        } catch (JsonProcessingException e) {
            return "{\"error\": \"" + e.getMessage().replace("\"", "\\\"") + "\"}";
        }
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
