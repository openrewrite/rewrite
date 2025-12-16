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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

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
            int maxOptions = calculateMaxOptions(marketplace.getRoot());
            boolean hasVersion = hasAnyVersion(marketplace);
            boolean hasTeam = hasAnyTeam(marketplace);

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

            for (int i = 1; i <= maxOptions; i++) {
                headers.add("option" + i + "Name");
                headers.add("option" + i + "DisplayName");
                headers.add("option" + i + "Description");
            }

            if (hasTeam) {
                headers.add("team");
            }

            csv.writeHeaders(headers);
            writeCsvRecursive(csv, marketplace.getRoot(), emptyList(),
                    maxCategoryDepth, maxOptions, hasTeam, hasVersion);
        } finally {
            csv.close();
        }
    }

    private void writeCsvRecursive(CsvWriter csv, RecipeMarketplace.Category category,
                                   List<String> categoryPath, int maxCategoryDepth,
                                   int maxOptions, boolean hasTeam, boolean hasVersion) {
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

            if (hasTeam) {
                row.add(StringUtils.isBlank(bundle.getTeam()) ? "" : bundle.getTeam());
            }

            csv.writeRow(row.toArray(new String[0]));
        }

        for (RecipeMarketplace.Category child : category.getCategories()) {
            List<String> childPath = new ArrayList<>(categoryPath);
            childPath.add(0, child.getDisplayName());
            writeCsvRecursive(csv, child, childPath, maxCategoryDepth, maxOptions, hasTeam, hasVersion);
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

    private int calculateMaxOptions(RecipeMarketplace.Category category) {
        int max = 0;
        for (RecipeListing recipe : category.getRecipes()) {
            max = Math.max(max, recipe.getOptions().size());
        }
        for (RecipeMarketplace.Category child : category.getCategories()) {
            max = Math.max(max, calculateMaxOptions(child));
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
}
