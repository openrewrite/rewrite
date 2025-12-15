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

import com.univocity.parsers.csv.CsvWriter;
import com.univocity.parsers.csv.CsvWriterSettings;
import io.micrometer.core.instrument.util.StringUtils;
import org.intellij.lang.annotations.Language;
import org.openrewrite.config.ColumnDescriptor;
import org.openrewrite.config.DataTableDescriptor;
import org.openrewrite.config.OptionDescriptor;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

import static java.util.Collections.emptyList;

public class RecipeMarketplaceWriter {

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
            boolean hasVersion = hasAnyVersion(marketplace);
            boolean hasTeam = hasAnyTeam(marketplace);
            boolean hasCategoryDescription = hasAnyCategoryDescription(marketplace.getRoot());
            List<String> metadataKeys = collectMetadataKeys(marketplace);

            List<String> headers = new ArrayList<>();
            headers.add("ecosystem");
            headers.add("packageName");
            if (hasVersion) {
                headers.add("version");
            }
            headers.add("name");
            headers.add("displayName");
            headers.add("description");

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

            if (hasAnyOptions(marketplace.getRoot())) {
                headers.add("options");
            }

            int maxDataTables = calculateMaxDataTables(marketplace.getRoot());
            for (int i = 1; i <= maxDataTables; i++) {
                headers.add("dataTable" + i);
            }

            // Add metadata headers
            headers.addAll(metadataKeys);

            if (hasTeam) {
                headers.add("team");
            }

            csv.writeHeaders(headers);
            writeCsvRecursive(csv, marketplace.getRoot(), emptyList(), emptyList(),
                    maxCategoryDepth, hasOptions, maxDataTables, hasTeam, hasVersion, hasCategoryDescription, metadataKeys);
        } finally {
            csv.close();
        }
    }

    private void writeCsvRecursive(CsvWriter csv, RecipeMarketplace.Category category,
                                   List<String> categoryPath, List<String> categoryDescriptionPath,
                                   int maxCategoryDepth, boolean hasOptions, int maxDataTables, boolean hasTeam,
                                   boolean hasVersion, boolean hasCategoryDescription,
                                   List<String> metadataKeys) {
        for (RecipeListing recipe : category.getRecipes()) {
            List<String> row = new ArrayList<>();
            RecipeBundle bundle = recipe.getBundle();
            row.add(bundle.getPackageEcosystem());
            row.add(bundle.getPackageName());
            if (hasVersion) {
                row.add(StringUtils.isBlank(bundle.getVersion()) ? "" : bundle.getVersion());
            }
            row.add(recipe.getName());
            row.add(recipe.getDisplayName());
            row.add(recipe.getDescription());

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

            // Options column (all options in one TOML cell)
            if (hasOptions) {
                row.add(optionsToToml(recipe.getOptions()));
            }

            // Data table columns (TOML format, one per column)
            List<DataTableDescriptor> dataTables = recipe.getDataTables();
            for (int i = 0; i < maxDataTables; i++) {
                if (i < dataTables.size()) {
                    DataTableDescriptor dataTable = dataTables.get(i);
                    row.add(dataTableToToml(dataTable));
                } else {
                    row.add("");
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

            csv.writeRow(row.toArray(new String[0]));
        }

        for (RecipeMarketplace.Category child : category.getCategories()) {
            List<String> childPath = new ArrayList<>(categoryPath);
            childPath.add(0, child.getDisplayName());
            List<String> childDescriptionPath = new ArrayList<>(categoryDescriptionPath);
            childDescriptionPath.add(0, child.getDescription());
            writeCsvRecursive(csv, child, childPath, childDescriptionPath, maxCategoryDepth, hasOptions, maxDataTables,
                    hasTeam, hasVersion, hasCategoryDescription, metadataKeys);
        }
    }

    private String optionsToToml(List<OptionDescriptor> options) {
        if (options.isEmpty()) {
            return "";
        }
        StringBuilder toml = new StringBuilder();
        for (OptionDescriptor option : options) {
            toml.append("[").append(option.getName()).append("]\n");
            toml.append("displayName = ").append(toTomlString(option.getDisplayName())).append("\n");
            toml.append("description = ").append(toTomlString(option.getDescription())).append("\n");
            toml.append("required = ").append(option.isRequired()).append("\n");
            if (option.getExample() != null) {
                toml.append("example = ").append(toTomlString(option.getExample())).append("\n");
            }
            toml.append("\n");
        }
        return toml.toString().trim();
    }

    private String dataTableToToml(DataTableDescriptor dataTable) {
        StringBuilder toml = new StringBuilder();
        toml.append("name = ").append(toTomlString(dataTable.getName())).append("\n");
        toml.append("displayName = ").append(toTomlString(dataTable.getDisplayName())).append("\n");
        toml.append("description = ").append(toTomlString(dataTable.getDescription())).append("\n");
        // Write columns as TOML tables keyed by column name
        if (dataTable.getColumns() != null && !dataTable.getColumns().isEmpty()) {
            for (ColumnDescriptor col : dataTable.getColumns()) {
                toml.append("\n[columns.").append(col.getName()).append("]\n");
                toml.append("type = ").append(toTomlString(col.getType())).append("\n");
                if (col.getDisplayName() != null) {
                    toml.append("displayName = ").append(toTomlString(col.getDisplayName())).append("\n");
                }
                if (col.getDescription() != null) {
                    toml.append("description = ").append(toTomlString(col.getDescription())).append("\n");
                }
            }
        }
        return toml.toString().trim();
    }

    private String toTomlString(String value) {
        if (value == null) {
            return "\"\"";
        }
        // Escape special characters for TOML string
        String escaped = value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        return "\"" + escaped + "\"";
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

    private int calculateMaxDataTables(RecipeMarketplace.Category category) {
        int max = 0;
        for (RecipeListing recipe : category.getRecipes()) {
            max = Math.max(max, recipe.getDataTables().size());
        }
        for (RecipeMarketplace.Category child : category.getCategories()) {
            max = Math.max(max, calculateMaxDataTables(child));
        }
        return max;
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
