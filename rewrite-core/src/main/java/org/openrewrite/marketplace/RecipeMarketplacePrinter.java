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

import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.BitSet;
import java.util.Collection;
import java.util.StringJoiner;
import java.util.function.Consumer;

import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
public class RecipeMarketplacePrinter {
    private final Options options;

    public RecipeMarketplacePrinter() {
        this.options = Options.builder().build();
    }

    public RecipeMarketplacePrinter(Consumer<Options.Builder> optionsConfigurer) {
        Options.Builder builder = Options.builder();
        optionsConfigurer.accept(builder);
        this.options = builder.build();
    }

    public String print(RecipeMarketplace marketplace) {
        StringJoiner out = new StringJoiner("\n");
        print(marketplace, out, 0, options, new BitSet());
        return out.toString();
    }

    private void print(RecipeMarketplace marketplace,
                       StringJoiner out,
                       int level,
                       Options options,
                       BitSet lastCategoryMask) {
        StringBuilder line = new StringBuilder();
        printTreeLines(line, level, lastCategoryMask);
        if (level > 0) {
            line.append("|-");
        }
        line.append(marketplace.isRoot() ? "√" : "\uD83D\uDCC1");

        String displayName = marketplace.getDisplayName();
        switch (options.getNameStyle()) {
            case DISPLAY_NAME:
            case ID:
                line.append(displayName);
                break;
            case BOTH:
                line.append(displayName);
                if (!marketplace.getDescription().isEmpty()) {
                    line.append(" (").append(marketplace.getDescription()).append(')');
                }
                break;
        }
        out.add(line);

        Collection<RecipeMarketplace> categories = marketplace.getCategories();
        if (options.isOmitEmptyCategories()) {
            categories = categories.stream()
                    .filter(cat -> !cat.getRecipes().isEmpty() || !cat.getCategories().isEmpty())
                    .collect(toList());
        }

        int i = 0;
        for (RecipeMarketplace category : categories) {
            if (++i == categories.size()) {
                lastCategoryMask.set(level, true);
            }
            print(category, out, level + 1, options, (BitSet) lastCategoryMask.clone());
        }

        lastCategoryMask.set(level, true);
        level++;
        for (RecipeListing recipe : marketplace.getRecipes()) {
            StringBuilder recipeLine = new StringBuilder();
            printTreeLines(recipeLine, level, lastCategoryMask);
            recipeLine.append("|-\uD83E\uDD16");
            switch (options.getNameStyle()) {
                case DISPLAY_NAME:
                    recipeLine.append(recipe.getDisplayName());
                    break;
                case ID:
                    recipeLine.append(recipe.getName());
                    break;
                case BOTH:
                    recipeLine.append(recipe.getDisplayName())
                            .append(" (")
                            .append(recipe.getName())
                            .append(')');
                    break;
            }
            out.add(recipeLine);
        }
    }

    private static void printTreeLines(StringBuilder line, int level, BitSet lastCategoryMask) {
        for (int i = 0; i < level - 1; i++) {
            if (lastCategoryMask.get(i)) {
                line.append("   ");
            } else {
                line.append("│  ");
            }
        }
    }

    public enum NameStyle {
        DISPLAY_NAME,
        ID,
        BOTH
    }

    @Value
    public static class Options {
        boolean omitEmptyCategories;
        NameStyle nameStyle;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private boolean omitEmptyCategories = true;
            private NameStyle nameStyle = NameStyle.ID;

            public Builder omitEmptyCategories(boolean omitEmptyCategories) {
                this.omitEmptyCategories = omitEmptyCategories;
                return this;
            }

            public Builder nameStyle(NameStyle nameStyle) {
                this.nameStyle = nameStyle;
                return this;
            }

            public Options build() {
                return new Options(omitEmptyCategories, nameStyle);
            }
        }
    }
}
