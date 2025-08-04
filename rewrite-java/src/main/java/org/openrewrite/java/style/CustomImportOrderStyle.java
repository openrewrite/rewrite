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
package org.openrewrite.java.style;

import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.style.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;

@Value
@With
public class CustomImportOrderStyle implements Style {

    /**
     * Ordered grouping rules, including optional SAME_PACKAGE(n).
     * Example: [STATIC, SAME_PACKAGE(3), STANDARD_JAVA_PACKAGE, THIRD_PARTY_PACKAGE, SPECIAL_IMPORTS]
     */
    List<GroupWithDepth> importOrder;

    /**
     * Blank line should separate groups.
     */
    Boolean separateLineBetweenGroups;

    /**
     * Force sorting imports within a group alphabetically.
     */
    Boolean sortImportsInGroupAlphabetically;

    /**
     * RegExp for SPECIAL_IMPORTS group.
     */
    String specialImportsRegExp;

    /**
     * RegExp for STANDARD_JAVA_PACKAGE group.
     */
    String standardPackageRegExp;

    /**
     * RegExp for THIRD_PARTY_PACKAGE group.
     */
    String thirdPartyPackageRegExp;

    @Override
    public Style applyDefaults() {
        CustomImportOrderStyle def = Checkstyle.customImportOrderStyle();
        return new CustomImportOrderStyle(
                (importOrder != null && !importOrder.isEmpty()) ? importOrder : def.importOrder,
                separateLineBetweenGroups,
                sortImportsInGroupAlphabetically,
                specialImportsRegExp != null ? specialImportsRegExp : def.specialImportsRegExp,
                standardPackageRegExp != null ? standardPackageRegExp : def.standardPackageRegExp,
                thirdPartyPackageRegExp != null ? thirdPartyPackageRegExp : def.thirdPartyPackageRegExp
        );
    }

    public enum CustomImportOrderGroup {
        STATIC,
        SAME_PACKAGE,
        STANDARD_JAVA_PACKAGE,
        THIRD_PARTY_PACKAGE,
        SPECIAL_IMPORTS;
    }

    /**
     * Representation of a group, possibly with a parameter (depth for SAME_PACKAGE).
     */
    @Value
    @With
    public static class GroupWithDepth {
        CustomImportOrderGroup group;
        @Nullable
        Integer depth;

        @Override
        public String toString() {
            return group + (group == CustomImportOrderGroup.SAME_PACKAGE && depth != null ? "(" + depth + ")" : "");
        }
    }

    /**
     * Parses a single import group rule, e.g., "STATIC", "SAME_PACKAGE(3)", "THIRD_PARTY_PACKAGE".
     * Throws IllegalArgumentException if the rule is not recognized.
     *
     * @param groupRule The input string representing a group rule.
     *                  Example values: "STATIC", "SAME_PACKAGE(3)", "STANDARD_JAVA_PACKAGE", "SPECIAL_IMPORTS".
     * @return The corresponding {@code GroupWithDepth} object.
     * @throws IllegalArgumentException if the input does not match a known group pattern.
     */
    public static GroupWithDepth parseGroup(String groupRule) {
        Matcher m = Pattern.compile("^SAME_PACKAGE\\((\\d+)\\)$").matcher(groupRule);
        if (m.matches()) {
            return new GroupWithDepth(CustomImportOrderGroup.SAME_PACKAGE, Integer.parseInt(m.group(1)));
        }

        switch (groupRule) {
            case "STATIC":
                return new GroupWithDepth(CustomImportOrderGroup.STATIC, null);
            case "STANDARD_JAVA_PACKAGE":
                return new GroupWithDepth(CustomImportOrderGroup.STANDARD_JAVA_PACKAGE, null);
            case "THIRD_PARTY_PACKAGE":
                return new GroupWithDepth(CustomImportOrderGroup.THIRD_PARTY_PACKAGE, null);
            case "SPECIAL_IMPORTS":
                return new GroupWithDepth(CustomImportOrderGroup.SPECIAL_IMPORTS, null);
            default:
                throw new IllegalArgumentException("Unknown import group: " + groupRule);
        }
    }

    /**
     * Parses a delimited string of import group rules into a list of {@link GroupWithDepth} objects.
     * Accepted delimiters are commas and "###", e.g., "STATIC, SAME_PACKAGE(3), ...".
     *
     * @param input Delimited rules string (e.g., "STATIC, SAME_PACKAGE(3), ...").
     * @return The list of parsed {@code GroupWithDepth} objects, or an empty list if input is null/blank.
     */
    public static List<GroupWithDepth> parseImportOrder(String input) {
        if (StringUtils.isBlank(input)) return emptyList();
        List<GroupWithDepth> groups = new ArrayList<>();
        for (String rule : input.split("\\s*,\\s*|###")) {
            String groupRule = rule.trim();
            if (!groupRule.isEmpty()) {
                groups.add(parseGroup(groupRule));
            }
        }
        return groups;
    }
}
