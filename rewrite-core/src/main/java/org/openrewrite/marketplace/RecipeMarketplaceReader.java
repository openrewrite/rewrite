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
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.openrewrite.internal.StringUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.emptySet;

public class RecipeMarketplaceReader {

    private final Map<String, RecipeBundleLoader> bundleLoaders;

    /**
     * Create a reader with specific bundle loaders.
     *
     * @param bundleLoaders The bundle loaders to use for creating bundles from CSV ecosystem data
     */
    public RecipeMarketplaceReader(RecipeBundleLoader... bundleLoaders) {
        this.bundleLoaders = new HashMap<>();
        for (RecipeBundleLoader loader : bundleLoaders) {
            this.bundleLoaders.put(loader.getEcosystem().toLowerCase(), loader);
        }
    }

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
            RecipeMarketplace root = RecipeMarketplace.newEmpty();
            List<NamedColumn> headers = null;
            int line = 0;

            @Nullable String[] row;
            while ((row = parser.parseNext()) != null) {
                line++;

                // Skip empty rows
                if (row.length == 0 || (row.length == 1 && row[0] == null)) {
                    continue;
                }

                if (headers == null) {
                    headers = parseHeaders(row);
                } else {
                    readRecipeOffering(row, root, headers);
                }
            }

            // If there's only one top-level category, return it directly
            if (root.getCategories().size() == 1 && root.getRecipes().isEmpty()) {
                return root.getCategories().get(0);
            }
            return root;
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

    private void readRecipeOffering(@Nullable String[] row, RecipeMarketplace root, List<NamedColumn> headers) {
        String name = null;
        String displayName = null;
        String description = null;
        String ecosystem = null;
        String packageName = null;
        String version = null;
        String team = null;
        List<String> categories = new ArrayList<>();
        Map<Integer, OptionData> options = new TreeMap<>();

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
                        options.computeIfAbsent(optionIndex, k -> new OptionData()).setName(value);
                    }
                    break;
                case OPTION_DISPLAY_NAME:
                    if (value != null) {
                        int optionIndex = column.getIndex();
                        options.computeIfAbsent(optionIndex, k -> new OptionData()).setDisplayName(value);
                    }
                    break;
                case OPTION_DESCRIPTION:
                    if (value != null) {
                        int optionIndex = column.getIndex();
                        options.computeIfAbsent(optionIndex, k -> new OptionData()).setDescription(value);
                    }
                    break;
                default:
                    // Ignore unknown columns
            }
        }

        if (name == null) {
            throw new IllegalArgumentException("CSV file must contain a column named 'name' and each row must have a value for it");
        }

        // Create bundle if ecosystem information is present
        RecipeBundle bundle = null;
        if (ecosystem != null && packageName != null && version != null) {
            RecipeBundleLoader loader = bundleLoaders.get(ecosystem.toLowerCase());
            if (loader != null) {
                bundle = loader.createBundle(packageName, version, team);
            }
        }

        // Convert options map to list of RecipeOffering.Option
        List<RecipeOffering.Option> offeringOptions = new ArrayList<>();
        for (OptionData optionData : options.values()) {
            if (optionData.getName() != null) {
                offeringOptions.add(new RecipeOffering.Option(
                        optionData.getName(),
                        optionData.getDisplayName() != null ? optionData.getDisplayName() : optionData.getName(),
                        optionData.getDescription() != null ? optionData.getDescription() : ""
                ));
            }
        }

        RecipeOffering offering = new RecipeOffering(
                name,
                displayName != null ? displayName : name,
                name, // instanceName
                description != null ? description : "",
                emptySet(), // tags
                null, // estimatedEffortPerOccurrence
                offeringOptions,
                bundle
        );

        // Navigate to the correct category and add the offering
        RecipeMarketplace targetCategory = navigateToCategory(root, categories);
        targetCategory.getRecipes().add(offering);
    }

    /**
     * Navigate to the target category, creating categories as needed.
     * Categories are read left to right, with left being the deepest level.
     *
     * @param root       The root marketplace.
     * @param categories The category path (left = deepest).
     * @return The target category.
     */
    private RecipeMarketplace navigateToCategory(RecipeMarketplace root, List<String> categories) {
        if (categories.isEmpty()) {
            return root;
        }

        RecipeMarketplace current = root;

        // Read categories left to right (left is deepest, so we need to reverse)
        for (int i = categories.size() - 1; i >= 0; i--) {
            String categoryName = categories.get(i);
            RecipeMarketplace found = null;

            for (RecipeMarketplace child : current.getCategories()) {
                if (child.getDisplayName().equals(categoryName)) {
                    found = child;
                    break;
                }
            }

            if (found == null) {
                found = new RecipeMarketplace(categoryName, categoryName);
                current.getCategories().add(found);
            }

            current = found;
        }

        return current;
    }

    @Getter
    @RequiredArgsConstructor
    private enum Column {
        NAME("name"),
        DISPLAY_NAME("displayName"),
        DESCRIPTION("description"),
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

    @Data
    private static class OptionData {
        private @Nullable String name;
        private @Nullable String displayName;
        private @Nullable String description;
    }
}
