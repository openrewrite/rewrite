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
package org.openrewrite.marketplace;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.ConstructorDetector;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.openrewrite.config.CategoryDescriptor;
import org.openrewrite.config.DataTableDescriptor;
import org.openrewrite.config.OptionDescriptor;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.emptySet;
import static java.util.Collections.reverse;

public class RecipeMarketplaceReader {
    private static final ObjectMapper JSON_MAPPER = JsonMapper.builder()
            .constructorDetector(ConstructorDetector.USE_PROPERTIES_BASED)
            .build()
            .registerModule(new ParameterNamesModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    /**
     * Returns the recipe marketplace from CSV.
     *
     * @param csv A CSV string containing recipe marketplace data.
     * @return The recipe marketplace.
     */
    public RecipeMarketplace fromCsv(@Language("csv") String csv) {
        return fromCsv(new StringReader(csv));
    }

    /**
     * Returns the recipe marketplace from CSV.
     *
     * @param csv A file containing recipe marketplace CSV.
     * @return The recipe marketplace.
     */
    public RecipeMarketplace fromCsv(Path csv) {
        try {
            return fromCsv(Files.newBufferedReader(csv));
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read from CSV", e);
        }
    }

    /**
     * Returns the recipe marketplace from CSV.
     *
     * @param csv An input stream containing recipe marketplace CSV.
     * @return The recipe marketplace.
     */
    public RecipeMarketplace fromCsv(InputStream csv) {
        return fromCsv(new InputStreamReader(csv));
    }

    /**
     * Returns the recipe marketplace from CSV.
     *
     * @param csv A reader containing recipe marketplace CSV.
     * @return The recipe marketplace.
     */
    public RecipeMarketplace fromCsv(Reader csv) {
        CsvParserSettings settings = new CsvParserSettings();
        settings.setLineSeparatorDetectionEnabled(true);
        settings.setHeaderExtractionEnabled(false);
        settings.setDelimiterDetectionEnabled(true, ',', '\t', ';');
        // Allow larger content in columns (e.g., long recipe descriptions)
        settings.setMaxCharsPerColumn(-1); // No limit

        CsvParser parser = new CsvParser(settings);
        parser.beginParsing(csv);

        try {
            RecipeMarketplace marketplace = new RecipeMarketplace();
            List<NamedColumn> headers = null;

            @Nullable String[] row;
            while ((row = parser.parseNext()) != null) {
                // Skip empty rows
                if (row.length == 0 || (row.length == 1 && row[0] == null)) {
                    continue;
                }

                if (headers == null) {
                    headers = parseHeaders(row);
                } else {
                    readRecipe(row, marketplace, headers);
                }
            }

            return marketplace;
        } finally {
            parser.stopParsing();
        }
    }

    private List<NamedColumn> parseHeaders(@Nullable String[] row) {
        List<NamedColumn> headers = new ArrayList<>();
        for (String headerName : row) {
            if (headerName != null) {
                headerName = headerName.trim();
                headers.add(Column.fromString(headerName));
            }
        }
        return headers;
    }

    private void readRecipe(@Nullable String[] row, RecipeMarketplace marketplace, List<NamedColumn> headers) {
        String name = null;
        String displayName = null;
        String description = null;
        Duration estimatedEffortPerOccurrence = null;
        String ecosystem = null;
        String packageName = null;
        String requestedVersion = null;
        String version = null;
        int recipeCount = 1;
        String team = null;
        Map<Integer, String> categoryDisplayNames = new TreeMap<>();
        Map<Integer, String> categoryDescriptions = new TreeMap<>();
        List<OptionDescriptor> options = new ArrayList<>();
        List<DataTableDescriptor> dataTables = new ArrayList<>();
        Map<String, Object> metadata = new LinkedHashMap<>();

        for (int i = 0; i < row.length && i < headers.size(); i++) {
            String value = row[i];
            if (value != null) {
                value = value.trim();
                if ("null".equals(value)) {
                    value = null;
                }
            }

            NamedColumn column = headers.get(i);
            switch (column.getColumn()) {
                case NAME:
                    name = value;
                    break;
                case DISPLAY_NAME:
                    displayName = value;
                    break;
                case DESCRIPTION:
                    description = value;
                    break;
                case ESTIMATED_EFFORT_PER_OCCURRENCE:
                    if (value != null) {
                        try {
                            estimatedEffortPerOccurrence = Duration.parse(value);
                        } catch (Exception e) {
                            throw new IllegalArgumentException(
                                    "Invalid duration format for estimatedEffortPerOccurrence: '" + value +
                                    "'. Expected ISO-8601 duration format (e.g., PT5M, PT1H)", e);
                        }
                    }
                    break;
                case CATEGORY:
                    if (value != null) {
                        int categoryIndex = column.getIndex();
                        categoryDisplayNames.put(categoryIndex, value);
                    }
                    break;
                case CATEGORY_DESCRIPTION:
                    if (value != null) {
                        int categoryIndex = column.getIndex();
                        categoryDescriptions.put(categoryIndex, value);
                    }
                    break;
                case ECOSYSTEM:
                    ecosystem = value;
                    break;
                case PACKAGE_NAME:
                    packageName = value;
                    break;
                case REQUESTED_VERSION:
                    requestedVersion = value;
                    break;
                case VERSION:
                    version = value;
                    break;
                case RECIPE_COUNT:
                    recipeCount = value == null ? 1 : Integer.parseInt(value);
                    break;
                case TEAM:
                    team = value;
                    break;
                case OPTIONS:
                    if (value != null) {
                        options.addAll(parseOptionsFromJson(value));
                    }
                    break;
                case DATA_TABLES:
                    if (value != null) {
                        dataTables.addAll(parseDataTablesFromJson(value));
                    }
                    break;
                case UNKNOWN:
                    // Unknown columns are treated as metadata
                    if (value != null) {
                        metadata.put(column.getHeaderName(), value);
                    }
                    break;
            }
        }

        if (name == null) {
            throw new IllegalArgumentException("CSV file must contain a column named 'name' and each row must have a value for it");
        } else if (packageName == null) {
            throw new IllegalArgumentException("CSV file must contain a column named 'packageName' and each row must have a value for it");
        } else if (ecosystem == null) {
            throw new IllegalArgumentException("CSV file must contain a column named 'ecosystem' and each row must have a value for it");
        }

        // Create bundle if ecosystem information is present
        RecipeBundle bundle = new RecipeBundle(ecosystem.toLowerCase(), packageName, requestedVersion, version, team);

        RecipeListing listing = new RecipeListing(
                marketplace,
                name,
                displayName != null ? displayName : name,
                description != null ? description : "",
                estimatedEffortPerOccurrence,
                options,
                dataTables,
                recipeCount,
                bundle
        );

        // Populate metadata from unknown columns
        listing.getMetadata().putAll(metadata);

        // Convert category maps to CategoryDescriptor list
        // Categories are stored with index (1, 2, 3, ...) representing depth from deepest to shallowest
        // But we need to reverse them to get shallowest to deepest order for install
        List<CategoryDescriptor> categoryPath = new ArrayList<>();
        for (Map.Entry<Integer, String> entry : categoryDisplayNames.entrySet()) {
            int index = entry.getKey();
            @Language("markdown") String catDisplayName = entry.getValue();
            @Language("markdown") String catDescription = categoryDescriptions.getOrDefault(index, "");
            categoryPath.add(new CategoryDescriptor(
                    catDisplayName,
                    "", // packageName not used for marketplace categories
                    catDescription,
                    emptySet(),
                    false,
                    CategoryDescriptor.LOWEST_PRECEDENCE,
                    true // synthetic
            ));
        }
        reverse(categoryPath);
        marketplace.install(listing, categoryPath);
    }

    private List<OptionDescriptor> parseOptionsFromJson(String json) {
        try {
            return JSON_MAPPER.readValue(json, new TypeReference<List<OptionDescriptor>>() {
            });
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to parse options JSON: " + json, e);
        }
    }

    private List<DataTableDescriptor> parseDataTablesFromJson(String json) {
        try {
            return JSON_MAPPER.readValue(json, new TypeReference<List<DataTableDescriptor>>() {
            });
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to parse data tables JSON: " + json, e);
        }
    }

    @Getter
    @RequiredArgsConstructor
    private enum Column {
        NAME("name"),
        DISPLAY_NAME("displayName"),
        DESCRIPTION("description"),
        ESTIMATED_EFFORT_PER_OCCURRENCE("estimatedEffortPerOccurrence"),
        CATEGORY("category"),
        CATEGORY_DESCRIPTION("categoryDescription"),
        ECOSYSTEM("ecosystem"),
        PACKAGE_NAME("packageName"),
        REQUESTED_VERSION("requestedVersion"),
        VERSION("version"),
        RECIPE_COUNT("recipeCount"),
        TEAM("team"),
        OPTIONS("options"),
        DATA_TABLES("dataTables"),
        UNKNOWN("_unknown");

        private final String columnName;

        private static final Pattern CATEGORY_PATTERN = Pattern.compile("category(\\d*)", Pattern.CASE_INSENSITIVE);
        private static final Pattern CATEGORY_DESCRIPTION_PATTERN = Pattern.compile("category(\\d*)Description", Pattern.CASE_INSENSITIVE);

        public static NamedColumn fromString(String key) {
            // Check for category description columns first (category1Description, category2Description, ...)
            // Must check before category pattern since "category1Description" starts with "category"
            Matcher categoryDescriptionMatcher = CATEGORY_DESCRIPTION_PATTERN.matcher(key);
            if (categoryDescriptionMatcher.matches()) {
                String indexStr = categoryDescriptionMatcher.group(1);
                int index = indexStr.isEmpty() ? 1 : Integer.parseInt(indexStr);
                return new NamedColumn(CATEGORY_DESCRIPTION, key, index);
            }

            // Check for category columns (category, category1, category2, ...)
            Matcher categoryMatcher = CATEGORY_PATTERN.matcher(key);
            if (categoryMatcher.matches()) {
                String indexStr = categoryMatcher.group(1);
                int index = indexStr.isEmpty() ? 1 : Integer.parseInt(indexStr);
                return new NamedColumn(CATEGORY, key, index);
            }

            // Check for standard columns
            for (Column column : values()) {
                if (column.columnName.equalsIgnoreCase(key)) {
                    return new NamedColumn(column, key, -1);
                }
            }

            return new NamedColumn(UNKNOWN, key, -1);
        }

    }

    @Value
    private static class NamedColumn {
        Column column;
        String headerName;
        int index;
    }
}
