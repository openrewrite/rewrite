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
import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.openrewrite.config.CategoryDescriptor;
import org.openrewrite.config.ColumnDescriptor;
import org.openrewrite.config.DataTableDescriptor;
import org.openrewrite.config.OptionDescriptor;
import org.openrewrite.internal.StringUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RecipeMarketplaceReader {
    private static final TomlMapper TOML_MAPPER = new TomlMapper();

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
        settings.setNullValue("");
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
        String version = null;
        String team = null;
        Map<Integer, String> categoryDisplayNames = new TreeMap<>();
        Map<Integer, String> categoryDescriptions = new TreeMap<>();
        List<OptionDescriptor> options = new ArrayList<>();
        Map<Integer, DataTableDescriptor> dataTables = new TreeMap<>();
        Map<String, Object> metadata = new LinkedHashMap<>();

        for (int i = 0; i < row.length && i < headers.size(); i++) {
            String value = row[i];
            if (value != null) {
                value = value.trim();
                if (StringUtils.isBlank(value)) {
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
                case VERSION:
                    version = value;
                    break;
                case TEAM:
                    team = value;
                    break;
                case OPTIONS:
                    if (value != null) {
                        options.addAll(parseOptionsFromToml(value));
                    }
                    break;
                case DATA_TABLE:
                    if (value != null) {
                        int dataTableIndex = column.getIndex();
                        DataTableDescriptor dataTable = parseDataTableFromToml(value);
                        if (dataTable != null) {
                            dataTables.put(dataTableIndex, dataTable);
                        }
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
        RecipeBundle bundle = new RecipeBundle(ecosystem.toLowerCase(), packageName, version, team);

        RecipeListing listing = new RecipeListing(
                marketplace,
                name,
                displayName != null ? displayName : name,
                description != null ? description : "",
                estimatedEffortPerOccurrence,
                options,
                new ArrayList<>(dataTables.values()),
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
                    Collections.emptySet(),
                    false,
                    CategoryDescriptor.LOWEST_PRECEDENCE,
                    true // synthetic
            ));
        }
        Collections.reverse(categoryPath);
        marketplace.install(listing, categoryPath);
    }

    private List<OptionDescriptor> parseOptionsFromToml(String toml) {
        try {
            // Parse TOML with multiple tables like [groupId], [artifactId], etc.
            Map<String, Map<String, Object>> parsed = TOML_MAPPER.readValue(toml,
                    new TypeReference<Map<String, Map<String, Object>>>() {});

            List<OptionDescriptor> options = new ArrayList<>();
            for (Map.Entry<String, Map<String, Object>> entry : parsed.entrySet()) {
                String optionName = entry.getKey();
                Map<String, Object> values = entry.getValue();

                options.add(new OptionDescriptor(
                        optionName,
                        getStringValue(values, "type", "String"),
                        getStringValue(values, "displayName", ""),
                        getStringValue(values, "description", ""),
                        getStringValue(values, "example", null),
                        null, // valid
                        getBooleanValue(values, "required", false),
                        null  // value
                ));
            }
            return options;
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to parse options TOML: " + toml, e);
        }
    }

    @Nullable
    private DataTableDescriptor parseDataTableFromToml(String toml) {
        try {
            // Parse flat TOML format with name = "...", displayName = "...", etc.
            Map<String, Object> values = TOML_MAPPER.readValue(toml,
                    new TypeReference<Map<String, Object>>() {});
            if (values.isEmpty()) {
                return null;
            }

            // Parse columns if present - columns is a map keyed by column name
            List<ColumnDescriptor> columns = new ArrayList<>();
            Object columnsObj = values.get("columns");
            if (columnsObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Map<String, Object>> columnsMap = (Map<String, Map<String, Object>>) columnsObj;
                for (Map.Entry<String, Map<String, Object>> entry : columnsMap.entrySet()) {
                    String columnName = entry.getKey();
                    Map<String, Object> colMap = entry.getValue();
                    columns.add(new ColumnDescriptor(
                            columnName,
                            getStringValue(colMap, "type", "String"),
                            getStringValue(colMap, "displayName", null),
                            getStringValue(colMap, "description", null)
                    ));
                }
            }

            return new DataTableDescriptor(
                    getStringValue(values, "name", ""),
                    getStringValue(values, "displayName", ""),
                    getStringValue(values, "description", ""),
                    columns
            );
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to parse data table TOML: " + toml, e);
        }
    }

    private String getStringValue(Map<String, Object> values, String key, @Nullable String defaultValue) {
        Object value = values.get(key);
        if (value == null) {
            return defaultValue;
        }
        return value.toString();
    }

    private boolean getBooleanValue(Map<String, Object> values, String key, boolean defaultValue) {
        Object value = values.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(value.toString());
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
        VERSION("version"),
        TEAM("team"),
        OPTIONS("options"),
        DATA_TABLE("dataTable"),
        UNKNOWN("_unknown");

        private final String columnName;

        private static final Pattern CATEGORY_PATTERN = Pattern.compile("category(\\d*)", Pattern.CASE_INSENSITIVE);
        private static final Pattern CATEGORY_DESCRIPTION_PATTERN = Pattern.compile("category(\\d*)Description", Pattern.CASE_INSENSITIVE);
        private static final Pattern DATA_TABLE_PATTERN = Pattern.compile("dataTable(\\d+)", Pattern.CASE_INSENSITIVE);

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

            // Check for data table columns (dataTable1, dataTable2, ...)
            Matcher dataTableMatcher = DATA_TABLE_PATTERN.matcher(key);
            if (dataTableMatcher.matches()) {
                int index = Integer.parseInt(dataTableMatcher.group(1));
                return new NamedColumn(DATA_TABLE, key, index);
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
