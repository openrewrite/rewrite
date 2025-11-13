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

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.openrewrite.internal.StringUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RecipeMarketplaceReader {
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
        List<String> categories = new ArrayList<>();
        Map<Integer, RecipeListing.Option> options = new TreeMap<>();

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
                        categories.add(value);
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
                case OPTION_NAME:
                    if (value != null) {
                        int optionIndex = column.getIndex();
                        options.computeIfAbsent(optionIndex, k -> new RecipeListing.Option()).setName(value);
                    }
                    break;
                case OPTION_DISPLAY_NAME:
                    if (value != null) {
                        int optionIndex = column.getIndex();
                        options.computeIfAbsent(optionIndex, k -> new RecipeListing.Option()).setDisplayName(value);
                    }
                    break;
                case OPTION_DESCRIPTION:
                    if (value != null) {
                        int optionIndex = column.getIndex();
                        options.computeIfAbsent(optionIndex, k -> new RecipeListing.Option()).setDescription(value);
                    }
                    break;
                default:
                    // Ignore unknown columns
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
                new ArrayList<>(options.values()),
                bundle
        );

        Collections.reverse(categories);
        marketplace.install(listing, categories);
    }

    @Getter
    @RequiredArgsConstructor
    private enum Column {
        NAME("name"),
        DISPLAY_NAME("displayName"),
        DESCRIPTION("description"),
        ESTIMATED_EFFORT_PER_OCCURRENCE("estimatedEffortPerOccurrence"),
        CATEGORY("category"),
        ECOSYSTEM("ecosystem"),
        PACKAGE_NAME("packageName"),
        VERSION("version"),
        TEAM("team"),
        OPTION_NAME("optionName"),
        OPTION_DISPLAY_NAME("optionDisplayName"),
        OPTION_DESCRIPTION("optionDescription"),
        UNKNOWN("_unknown");

        private final String columnName;

        private static final Pattern OPTION_NAME_PATTERN = Pattern.compile("option(\\d+)Name", Pattern.CASE_INSENSITIVE);
        private static final Pattern OPTION_DISPLAY_NAME_PATTERN = Pattern.compile("option(\\d+)DisplayName", Pattern.CASE_INSENSITIVE);
        private static final Pattern OPTION_DESCRIPTION_PATTERN = Pattern.compile("option(\\d+)Description", Pattern.CASE_INSENSITIVE);

        public static NamedColumn fromString(String key) {
            String lowerKey = key.toLowerCase();

            // Check for category columns (category, category1, category2, ...)
            if (lowerKey.startsWith("category")) {
                return new NamedColumn(CATEGORY, key, -1);
            }

            // Check for option columns with patterns
            Matcher optionNameMatcher = OPTION_NAME_PATTERN.matcher(key);
            if (optionNameMatcher.matches()) {
                int index = Integer.parseInt(optionNameMatcher.group(1));
                return new NamedColumn(OPTION_NAME, key, index);
            }

            Matcher optionDisplayNameMatcher = OPTION_DISPLAY_NAME_PATTERN.matcher(key);
            if (optionDisplayNameMatcher.matches()) {
                int index = Integer.parseInt(optionDisplayNameMatcher.group(1));
                return new NamedColumn(OPTION_DISPLAY_NAME, key, index);
            }

            Matcher optionDescriptionMatcher = OPTION_DESCRIPTION_PATTERN.matcher(key);
            if (optionDescriptionMatcher.matches()) {
                int index = Integer.parseInt(optionDescriptionMatcher.group(1));
                return new NamedColumn(OPTION_DESCRIPTION, key, index);
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
