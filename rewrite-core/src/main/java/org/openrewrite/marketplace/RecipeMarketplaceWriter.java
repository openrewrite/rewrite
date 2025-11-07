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
import org.intellij.lang.annotations.Language;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.reverse;

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
            // Determine the maximum depth for categories and options
            // Start depth at 1 if this is not the root marketplace (since it will be in the path)
            int startDepth = marketplace.isRoot() ? 0 : 1;
            int maxCategoryDepth = calculateMaxCategoryDepth(marketplace, startDepth);
            int maxOptions = calculateMaxOptions(marketplace);

            // Write headers
            List<String> headers = new ArrayList<>();

            // Add optional bundle columns first if any recipe has bundle info
            Set<String> optionalColumns = findUsedOptionalColumns(marketplace);
            if (optionalColumns.contains("ecosystem")) {
                headers.add("ecosystem");
            }
            if (optionalColumns.contains("packageName")) {
                headers.add("packageName");
            }
            if (optionalColumns.contains("version")) {
                headers.add("version");
            }

            headers.add("name");
            headers.add("displayName");
            headers.add("description");

            // Add category headers (left = deepest, right = shallowest)
            for (int i = 1; i <= maxCategoryDepth; i++) {
                headers.add("category" + i);
            }

            // Add option headers
            for (int i = 1; i <= maxOptions; i++) {
                headers.add("option" + i + "Name");
                headers.add("option" + i + "DisplayName");
                headers.add("option" + i + "Description");
            }

            // Add team column at the end if present
            if (optionalColumns.contains("team")) {
                headers.add("team");
            }

            csv.writeHeaders(headers);

            // Write rows recursively
            // Start with the marketplace's name in the path unless it's the epsilon root
            List<String> categoryPath = new ArrayList<>();
            if (!marketplace.isRoot()) {
                categoryPath.add(marketplace.getDisplayName());
            }
            writeCsvRecursive(csv, marketplace, categoryPath, maxCategoryDepth, maxOptions, optionalColumns);
        } finally {
            csv.close();
        }
    }

    private void writeCsvRecursive(CsvWriter csv, RecipeMarketplace marketplace,
                                   List<String> categoryPath, int maxCategoryDepth,
                                   int maxOptions, Set<String> optionalColumns) {
        // Write all recipes in this category
        for (RecipeListing recipe : marketplace.getRecipes()) {
            List<String> row = new ArrayList<>();

            // Optional bundle columns first (ecosystem, packageName, version)
            RecipeBundle bundle = recipe.getBundle();
            if (optionalColumns.contains("ecosystem")) {
                row.add(bundle != null ? bundle.getPackageEcosystem() : "");
            }
            if (optionalColumns.contains("packageName")) {
                row.add(bundle != null ? bundle.getPackageName() : "");
            }
            if (optionalColumns.contains("version")) {
                if (bundle == null) {
                    row.add("");
                } else {
                    row.add(bundle.getVersion() == null ? "" : bundle.getVersion());
                }
            }

            // Required columns
            row.add(recipe.getName());
            row.add(recipe.getDisplayName());
            row.add(recipe.getDescription());

            // Category columns (right-aligned: empty columns first, then categories from deepest to shallowest)
            // Filter out the epsilon root from the path
            List<String> filteredPath = new ArrayList<>();
            for (String category : categoryPath) {
                if (!RecipeMarketplace.ROOT.equals(category)) {
                    filteredPath.add(category);
                }
            }
            reverse(filteredPath);
            // Right-align: pad with empty strings at the beginning
            int padding = maxCategoryDepth - filteredPath.size();
            for (int i = 0; i < maxCategoryDepth; i++) {
                if (i < padding) {
                    row.add("");
                } else {
                    row.add(filteredPath.get(i - padding));
                }
            }

            // Option columns
            List<? extends RecipeListing.Option> options = recipe.getOptions();
            for (int i = 0; i < maxOptions; i++) {
                if (i < options.size()) {
                    RecipeListing.Option option = options.get(i);
                    row.add(option.getName());
                    row.add(option.getDisplayName());
                    row.add(option.getDescription());
                } else {
                    row.add("");
                    row.add("");
                    row.add("");
                }
            }

            // Team column at the end if present
            if (optionalColumns.contains("team")) {
                String team = bundle != null ? bundle.getTeam() : null;
                row.add(team != null ? team : "");
            }

            csv.writeRow(row.toArray(new String[0]));
        }

        // Recursively write child categories
        for (RecipeMarketplace child : marketplace.getCategories()) {
            List<String> childPath = new ArrayList<>(categoryPath);
            childPath.add(child.getDisplayName());
            writeCsvRecursive(csv, child, childPath, maxCategoryDepth, maxOptions, optionalColumns);
        }
    }

    private int calculateMaxCategoryDepth(RecipeMarketplace marketplace, int currentDepth) {
        int maxDepth = 0;

        // If this marketplace has recipes, count the current depth
        if (!marketplace.getRecipes().isEmpty()) {
            maxDepth = currentDepth;
        }

        // Recurse into children
        for (RecipeMarketplace child : marketplace.getCategories()) {
            int childDepth = calculateMaxCategoryDepth(child, currentDepth + 1);
            maxDepth = Math.max(maxDepth, childDepth);
        }

        return maxDepth;
    }

    private int calculateMaxOptions(RecipeMarketplace marketplace) {
        int max = 0;

        for (RecipeListing recipe : marketplace.getRecipes()) {
            max = Math.max(max, recipe.getOptions().size());
        }

        for (RecipeMarketplace child : marketplace.getCategories()) {
            max = Math.max(max, calculateMaxOptions(child));
        }

        return max;
    }

    private Set<String> findUsedOptionalColumns(RecipeMarketplace marketplace) {
        Set<String> columns = new LinkedHashSet<>();

        if (hasAnyBundle(marketplace)) {
            columns.add("ecosystem");
            columns.add("packageName");
            if (hasAnyVersion(marketplace)) {
                columns.add("version");
            }
            if (hasAnyTeam(marketplace)) {
                columns.add("team");
            }
        }

        return columns;
    }

    private boolean hasAnyBundle(RecipeMarketplace marketplace) {
        for (RecipeListing recipe : marketplace.getRecipes()) {
            if (recipe.getBundle() != null) {
                return true;
            }
        }

        for (RecipeMarketplace child : marketplace.getCategories()) {
            if (hasAnyBundle(child)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasAnyTeam(RecipeMarketplace marketplace) {
        for (RecipeListing recipe : marketplace.getRecipes()) {
            RecipeBundle bundle = recipe.getBundle();
            if (bundle != null && bundle.getTeam() != null) {
                return true;
            }
        }

        for (RecipeMarketplace child : marketplace.getCategories()) {
            if (hasAnyTeam(child)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasAnyVersion(RecipeMarketplace marketplace) {
        for (RecipeListing recipe : marketplace.getRecipes()) {
            RecipeBundle bundle = recipe.getBundle();
            if (bundle != null && bundle.getVersion() != null) {
                return true;
            }
        }

        for (RecipeMarketplace child : marketplace.getCategories()) {
            if (hasAnyVersion(child)) {
                return true;
            }
        }

        return false;
    }
}
