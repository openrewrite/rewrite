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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.univocity.parsers.csv.CsvWriter;
import com.univocity.parsers.csv.CsvWriterSettings;
import io.micrometer.core.instrument.util.StringUtils;
import org.intellij.lang.annotations.Language;
import org.openrewrite.config.DataTableDescriptor;
import org.openrewrite.config.OptionDescriptor;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

public class RecipeMarketplaceWriter {
    private static final Set<String> EXCLUDED_DATA_TABLES = new HashSet<>(Arrays.asList(
            "org.openrewrite.table.SearchResults",
            "org.openrewrite.table.SourcesFileResults",
            "org.openrewrite.table.SourcesFileErrors",
            "org.openrewrite.table.RecipeRunStats",
            "org.openrewrite.table.ParseFailures"
    ));
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    public @Language("csv") String toCsv(RecipeMarketplace marketplace) {
        StringWriter sw = new StringWriter();
        toCsv(marketplace, sw);
        return sw.toString();
    }

    public void toCsv(RecipeMarketplace marketplace, Path csvFile) {
        try (BufferedWriter writer = Files.newBufferedWriter(csvFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            toCsv(marketplace, writer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void toCsv(RecipeMarketplace marketplace, Writer writer) {
        CsvWriterSettings settings = new CsvWriterSettings();
        settings.getFormat().setLineSeparator("\n");

        CsvWriter csv = new CsvWriter(writer, settings);

        try {
            int maxCategoryDepth = calculateMaxCategoryDepth(marketplace.getRoot(), -1) + 1;
            boolean hasOptions = hasAnyOptions(marketplace.getRoot());
            boolean hasDataTables = hasAnyDataTables(marketplace.getRoot());
            boolean hasVersion = hasAnyVersion(marketplace);
            boolean hasTeam = hasAnyTeam(marketplace);
            boolean hasCategoryDescription = hasAnyCategoryDescription(marketplace.getRoot());
            List<String> metadataKeys = collectMetadataKeys(marketplace);

            List<String> headers = new ArrayList<>();
            headers.add("ecosystem");
            headers.add("packageName");
            if (hasVersion) {
                headers.add("requestedVersion");
                headers.add("version");
            }
            headers.add("name");
            headers.add("displayName");
            headers.add("description");
            headers.add("recipeCount");

            // Add category headers (left = deepest, right = shallowest)
            for (int i = 1; i <= maxCategoryDepth; i++) {
                headers.add("category" + i);
            }

            // Add category description headers if any category has a description
            if (hasCategoryDescription) {
                for (int i = 1; i <= maxCategoryDepth; i++) {
                    headers.add("category" + i + "Description");
                }
            }

            // Add metadata headers
            headers.addAll(metadataKeys);

            if (hasTeam) {
                headers.add("team");
            }

            // Add options and dataTables columns last (less human-readable JSON content)
            if (hasOptions) {
                headers.add("options");
            }

            if (hasDataTables) {
                headers.add("dataTables");
            }

            csv.writeHeaders(headers);
            writeCsvRecursive(csv, marketplace.getRoot(), emptyList(), emptyList(),
                    maxCategoryDepth, hasOptions, hasDataTables, hasTeam, hasVersion, hasCategoryDescription, metadataKeys);
        } finally {
            csv.close();
        }
    }

    private void writeCsvRecursive(CsvWriter csv, RecipeMarketplace.Category category,
                                   List<String> categoryPath, List<String> categoryDescriptionPath,
                                   int maxCategoryDepth, boolean hasOptions, boolean hasDataTables, boolean hasTeam,
                                   boolean hasVersion, boolean hasCategoryDescription,
                                   List<String> metadataKeys) {
        for (RecipeListing recipe : category.getRecipes()) {
            List<String> row = new ArrayList<>();
            RecipeBundle bundle = recipe.getBundle();
            row.add(bundle.getPackageEcosystem());
            row.add(bundle.getPackageName());
            if (hasVersion) {
                row.add(StringUtils.isBlank(bundle.getRequestedVersion()) ? "" : bundle.getRequestedVersion());
                row.add(StringUtils.isBlank(bundle.getVersion()) ? "" : bundle.getVersion());
            }
            row.add(recipe.getName());
            row.add(recipe.getDisplayName());
            row.add(recipe.getDescription());
            row.add(Integer.toString(recipe.getRecipeCount()));

            // Category columns (right-aligned: empty columns first, then categories from deepest to shallowest)
            int padding = maxCategoryDepth - categoryPath.size();
            for (int i = 0; i < maxCategoryDepth; i++) {
                if (i < padding) {
                    row.add("");
                } else {
                    row.add(categoryPath.get(i - padding));
                }
            }

            // Category description columns (if any category has a description)
            if (hasCategoryDescription) {
                for (int i = 0; i < maxCategoryDepth; i++) {
                    if (i < padding) {
                        row.add("");
                    } else {
                        row.add(categoryDescriptionPath.get(i - padding));
                    }
                }
            }

            // Metadata columns
            Map<String, Object> metadata = recipe.getMetadata();
            for (String key : metadataKeys) {
                Object value = metadata.get(key);
                row.add(value != null ? String.valueOf(value) : "");
            }

            if (hasTeam) {
                row.add(StringUtils.isBlank(bundle.getTeam()) ? "" : bundle.getTeam());
            }

            // Options column (all options in one JSON cell) - at the end for readability
            if (hasOptions) {
                row.add(optionsToJson(recipe.getOptions()));
            }

            // DataTables column (all data tables in one JSON array) - at the end for readability
            if (hasDataTables) {
                row.add(dataTablesToJson(recipe.getDataTables()));
            }

            csv.writeRow(row.toArray(new String[0]));
        }

        for (RecipeMarketplace.Category child : category.getCategories()) {
            List<String> childPath = new ArrayList<>(categoryPath);
            childPath.add(0, child.getDisplayName());
            List<String> childDescriptionPath = new ArrayList<>(categoryDescriptionPath);
            childDescriptionPath.add(0, child.getDescription());
            writeCsvRecursive(csv, child, childPath, childDescriptionPath, maxCategoryDepth, hasOptions, hasDataTables,
                    hasTeam, hasVersion, hasCategoryDescription, metadataKeys);
        }
    }

    private String optionsToJson(List<OptionDescriptor> options) {
        if (options.isEmpty()) {
            return "";
        }
        try {
            return JSON_MAPPER.writeValueAsString(options);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize options to JSON", e);
        }
    }

    private String dataTablesToJson(List<DataTableDescriptor> dataTables) {
        List<DataTableDescriptor> filtered = dataTables.stream()
                .filter(dt -> !EXCLUDED_DATA_TABLES.contains(dt.getName()))
                .collect(toList());
        if (filtered.isEmpty()) {
            return "";
        }
        try {
            return JSON_MAPPER.writeValueAsString(filtered);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize data tables to JSON", e);
        }
    }

    private int calculateMaxCategoryDepth(RecipeMarketplace.Category category, int currentDepth) {
        int max = currentDepth;
        for (RecipeMarketplace.Category child : category.getCategories()) {
            int childDepth = calculateMaxCategoryDepth(child, currentDepth + 1);
            max = Math.max(max, childDepth);
        }
        return max;
    }

    private boolean hasAnyOptions(RecipeMarketplace.Category category) {
        for (RecipeListing recipe : category.getRecipes()) {
            if (!recipe.getOptions().isEmpty()) {
                return true;
            }
        }
        for (RecipeMarketplace.Category child : category.getCategories()) {
            if (hasAnyOptions(child)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAnyDataTables(RecipeMarketplace.Category category) {
        for (RecipeListing recipe : category.getRecipes()) {
            for (DataTableDescriptor dt : recipe.getDataTables()) {
                if (!EXCLUDED_DATA_TABLES.contains(dt.getName())) {
                    return true;
                }
            }
        }
        for (RecipeMarketplace.Category child : category.getCategories()) {
            if (hasAnyDataTables(child)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAnyTeam(RecipeMarketplace marketplace) {
        for (RecipeListing recipe : marketplace.getAllRecipes()) {
            RecipeBundle bundle = recipe.getBundle();
            if (StringUtils.isNotBlank(bundle.getTeam())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAnyVersion(RecipeMarketplace marketplace) {
        for (RecipeListing recipe : marketplace.getAllRecipes()) {
            RecipeBundle bundle = recipe.getBundle();
            if (StringUtils.isNotBlank(bundle.getVersion())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAnyCategoryDescription(RecipeMarketplace.Category root) {
        // Skip root description, only check child categories
        for (RecipeMarketplace.Category child : root.getCategories()) {
            if (hasAnyCategoryDescriptionRecursive(child)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAnyCategoryDescriptionRecursive(RecipeMarketplace.Category category) {
        if (StringUtils.isNotBlank(category.getDescription())) {
            return true;
        }
        for (RecipeMarketplace.Category child : category.getCategories()) {
            if (hasAnyCategoryDescriptionRecursive(child)) {
                return true;
            }
        }
        return false;
    }

    private List<String> collectMetadataKeys(RecipeMarketplace marketplace) {
        Set<String> keys = new TreeSet<>();
        for (RecipeListing recipe : marketplace.getAllRecipes()) {
            keys.addAll(recipe.getMetadata().keySet());
        }
        return new ArrayList<>(keys);
    }
}
