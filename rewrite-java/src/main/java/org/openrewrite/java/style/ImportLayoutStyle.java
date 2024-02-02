/*
 * Copyright 2020 the original author or authors.
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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaPrinter;
import org.openrewrite.java.JavaStyle;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.internal.StreamUtils.distinctBy;

/**
 * A Java Style to define how imports are grouped and ordered. Additionally, this style provides configuration to dictate
 * how wildcard folding should be applied when multiple imports are in the same package or on the same, statically-imported
 * type.
 * <P><P>
 * The import layout consist of three properties:
 * <P><P>
 * <li>classCountToUseStarImport  - How many imports from the same package must be present before they should be collapsed into a star import. The default is 5.</li>
 * <li>nameCountToUseStarImport - How many static imports from the same type must be present before they should be collapsed into a static star import. The default is 3.</li>
 * <li>layout - An ordered list of import groupings which define exactly how imports should be organized within a compilation unit.</li>
 * <li>packagesToFold - An ordered list of packages which are folded when 1 or more types are in use.</li>
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
@JsonDeserialize(using = Deserializer.class)
@JsonSerialize(using = Serializer.class)
public class ImportLayoutStyle implements JavaStyle {

    @EqualsAndHashCode.Include
    private final int classCountToUseStarImport;

    @EqualsAndHashCode.Include
    private final int nameCountToUseStarImport;

    @EqualsAndHashCode.Include
    private final List<Block> layout;

    @EqualsAndHashCode.Include
    private final List<Block> packagesToFold;

    private final List<Block> blocksNoCatchalls;
    private final List<Block> blocksOnlyCatchalls;

    public ImportLayoutStyle(int classCountToUseStarImport, int nameCountToUseStarImport, List<Block> layout, List<Block> packagesToFold) {
        this.classCountToUseStarImport = classCountToUseStarImport;
        this.nameCountToUseStarImport = nameCountToUseStarImport;
        this.layout = layout.isEmpty() ? IntelliJ.importLayout().getLayout() : layout;
        this.packagesToFold = packagesToFold;

        // Divide the blocks into those that accept imports from any package ("catchalls") and those that accept imports from only specific packages
        Map<Boolean, List<Block>> blockGroups = layout.stream()
                .collect(Collectors.partitioningBy(block -> block instanceof Block.AllOthers));
        blocksNoCatchalls = blockGroups.get(false);
        blocksOnlyCatchalls = blockGroups.get(true);
    }

    /**
     * Adds a new import in a block that best represents the import layout style without
     * re-ordering any of the existing imports, i.e. a minimally invasive add.
     *
     * @param originalImports The import list before inserting.
     * @param toAdd           The import to add.
     * @param pkg             A package declaration, if one exists.
     * @return The import list with a new import added.
     */
    public List<JRightPadded<J.Import>> addImport(List<JRightPadded<J.Import>> originalImports,
                                                  J.Import toAdd, @Nullable J.Package pkg,
                                                  Collection<JavaType.FullyQualified> classpath) {
        JRightPadded<J.Import> paddedToAdd = new JRightPadded<>(toAdd, Space.EMPTY, Markers.EMPTY);

        if (originalImports.isEmpty()) {
            paddedToAdd = pkg == null ? paddedToAdd : paddedToAdd.withElement(paddedToAdd.getElement().withPrefix(Space.format("\n\n")));
            paddedToAdd = isPackageAlwaysFolded(packagesToFold, paddedToAdd.getElement()) ? paddedToAdd.withElement(paddedToAdd.getElement().withQualid(
                    paddedToAdd.getElement().getQualid().withName(
                            paddedToAdd.getElement().getQualid().getName().withSimpleName("*")
                    )
            )) : paddedToAdd;
            return singletonList(paddedToAdd);
        }

        // don't star fold just yet, because we are only going to star fold adjacent imports along with
        // the import to add at most. we don't even want to star fold other non-adjacent imports in the same
        // block that should be star folded according to the layout style (minimally invasive change).
        List<JRightPadded<J.Import>> ideallyOrdered =
                new ImportLayoutStyle(Integer.MAX_VALUE, Integer.MAX_VALUE, layout, packagesToFold)
                        .orderImports(ListUtils.concat(originalImports, paddedToAdd), new HashSet<>());

        if (ideallyOrdered.size() == originalImports.size()) {
            Set<String> originalPaths = new HashSet<>();
            for (JRightPadded<J.Import> originalImport : originalImports) {
                originalPaths.add(originalImport.getElement().getTypeName());
            }
            int sharedImports = 0;
            for (JRightPadded<J.Import> importJRightPadded : ideallyOrdered) {
                if (originalPaths.contains(importJRightPadded.getElement().getTypeName())) {
                    sharedImports++;
                }
            }
            if (sharedImports == originalImports.size()) {
                // must be a duplicate of an existing import
                return originalImports;
            }
        }

        JRightPadded<J.Import> before = null;
        JRightPadded<J.Import> after = null;

        Block addToBlock = block(paddedToAdd);
        int insertPosition = 0;

        for (int i = 0; i < ideallyOrdered.size(); i++) {
            JRightPadded<J.Import> anImport = ideallyOrdered.get(i);
            if (anImport.getElement().isScope(paddedToAdd.getElement())) {
                before = i > 0 ? ideallyOrdered.get(i - 1) : null;
                after = i + 1 < ideallyOrdered.size() ? ideallyOrdered.get(i + 1) : null;
                if (before != null) {
                    // Use the "before" import to determine insertion point.
                    // Find the import in the original list to establish insertion position.
                    for (int j = 0; j < originalImports.size(); j++) {
                        if (after != null && after.getElement().equals(originalImports.get(j).getElement()) && addToBlock.accept(after)) {
                            break;
                        } else if (before.getElement().equals(originalImports.get(j).getElement())) {
                            insertPosition = j + 1;
                            after = insertPosition < originalImports.size() ? originalImports.get(insertPosition) : null;
                            break;
                        }
                    }
                } else if (after != null) {
                    // Otherwise, "after" as the basis for the insertion.
                    // Find the import in the original list to establish insertion position.
                    for (int j = 0; j < originalImports.size(); j++) {
                        if (after.getElement().equals(originalImports.get(j).getElement())) {
                            insertPosition = j;
                            before = j > 0 ? originalImports.get(insertPosition - 1) : null;
                            break;
                        }
                    }
                }
                break;
            }
        }

        AtomicBoolean isNewBlock = new AtomicBoolean(false);
        if (!(insertPosition == 0 && pkg == null)) {
            if (before == null) {
                if (pkg != null) {
                    Space prefix = originalImports.get(0).getElement().getPrefix();
                    paddedToAdd = paddedToAdd.withElement(paddedToAdd.getElement().withPrefix(prefix));
                }
            } else if (block(before) != addToBlock) {
                boolean isFound = false;
                for (int j = insertPosition; j < originalImports.size(); j++) {
                    if (block(originalImports.get(j)) == addToBlock) {
                        insertPosition = j;
                        after = originalImports.get(j);
                        isFound = true;
                        break;
                    }
                }
                isNewBlock.set(!isFound);
                paddedToAdd = paddedToAdd.withElement(paddedToAdd.getElement().withPrefix(Space.format("\n\n")));
            } else {
                paddedToAdd = paddedToAdd.withElement(paddedToAdd.getElement().withPrefix(Space.format("\n")));
            }
        }

        List<JRightPadded<J.Import>> checkConflicts = new ArrayList<>(originalImports);
        checkConflicts.add(paddedToAdd);
        boolean isFoldable = new ImportLayoutConflictDetection(classpath, checkConflicts)
                .isPackageFoldable(packageOrOuterClassName(paddedToAdd));

        // Walk both directions from the insertion point, looking for imports that are in the same block and have the
        // same package/outerclassname.
        AtomicInteger starFoldFrom = new AtomicInteger(insertPosition);
        AtomicInteger starFoldTo = new AtomicInteger(insertPosition);
        AtomicBoolean starFold = new AtomicBoolean(false);
        int sameCount = 1; // start at 1 to account for the import being added.

        for (int i = insertPosition; i < originalImports.size(); i++) {
            JRightPadded<J.Import> anImport = originalImports.get(i);
            if (block(anImport) == addToBlock && packageOrOuterClassName(anImport)
                    .equals(packageOrOuterClassName(paddedToAdd))) {
                starFoldTo.set(i + 1);
                sameCount++;
            } else {
                break;
            }
        }

        for (int i = insertPosition - 1; i >= 0; i--) {
            JRightPadded<J.Import> anImport = originalImports.get(i);
            if (block(anImport) == addToBlock && packageOrOuterClassName(anImport)
                    .equals(packageOrOuterClassName(paddedToAdd))) {
                starFoldFrom.set(i);
                sameCount++;
            } else {
                break;
            }
        }

        if (isFoldable && (((paddedToAdd.getElement().isStatic() && nameCountToUseStarImport <= sameCount) ||
                (!paddedToAdd.getElement().isStatic() && classCountToUseStarImport <= sameCount)) || isPackageAlwaysFolded(packagesToFold, paddedToAdd.getElement()))) {
            starFold.set(true);
            if (insertPosition != starFoldFrom.get()) {
                // if we're adding to the middle of a group of imports that are getting star folded,
                // adopt the prefix of the first import in this group.
                paddedToAdd = paddedToAdd.withElement(paddedToAdd.getElement().withPrefix(
                        originalImports.get(starFoldFrom.get()).getElement().getPrefix()
                ));
            }
        }

        if (starFold.get()) {
            paddedToAdd = paddedToAdd.withElement(paddedToAdd.getElement().withQualid(
                    paddedToAdd.getElement().getQualid().withName(
                            paddedToAdd.getElement().getQualid().getName().withSimpleName("*")
                    )
            ));
            after = starFoldTo.get() < originalImports.size() ?
                    originalImports.get(starFoldTo.get()) : null;
        }

        if (after != null) {
            if (block(after) == addToBlock) {
                after = after.withElement(after.getElement().withPrefix(Space.format("\n")));
            } else if (!isNewBlock.get() && after.getElement().getPrefix().getLastWhitespace().chars()
                    .filter(c -> c == '\n').count() < 2) {
                after = after.withElement(after.getElement().withPrefix(Space.format("\n\n")));
            }
        }

        JRightPadded<J.Import> finalToAdd = paddedToAdd;
        JRightPadded<J.Import> finalAfter = after;
        return ListUtils.flatMap(originalImports, (i, anImport) -> {
            if (starFold.get() && i >= starFoldFrom.get() && i < starFoldTo.get()) {
                return i == starFoldFrom.get() ?
                        finalToAdd /* only add the star import once */ :
                        null;
            } else if (finalAfter != null && anImport.getElement().isScope(finalAfter.getElement())) {
                if (starFold.get()) {
                    // The added import is always folded, and is the first package occurrence in the imports.
                    if (starFoldFrom.get() == starFoldTo.get()) {
                        return Arrays.asList(finalToAdd, finalAfter);
                    } else {
                        return finalAfter;
                    }
                } else if (isNewBlock.get()) {
                    return anImport.getElement().isStatic() && !finalToAdd.getElement().isStatic() ?
                            Arrays.asList(finalToAdd, finalAfter) : Arrays.asList(finalAfter, finalToAdd);
                } else {
                    return Arrays.asList(finalToAdd, finalAfter);
                }
            } else if (i == originalImports.size() - 1 && (finalAfter == null)) {
                return Arrays.asList(anImport, finalToAdd);
            }
            return anImport;
        });
    }

    private Block block(JRightPadded<J.Import> anImport) {
        for (Block block : layout) {
            if (block.accept(anImport)) {
                return block;
            }
        }
        throw new IllegalStateException("Expected to find a block to fit import into.");
    }

    /**
     * This method will order and group a list of imports producing a new list that conforms to the rules defined
     * by the import layout style.
     *
     * @param originalImports A list of potentially unordered imports.
     * @return A list of imports that are grouped and ordered.
     */
    public List<JRightPadded<J.Import>> orderImports(List<JRightPadded<J.Import>> originalImports, Collection<JavaType.FullyQualified> classpath) {
        LayoutState layoutState = new LayoutState();
        ImportLayoutConflictDetection importLayoutConflictDetection = new ImportLayoutConflictDetection(classpath, originalImports);
        List<JRightPadded<J.Import>> orderedImports = new ArrayList<>();

        // Allocate imports to blocks, preferring to put imports into non-catchall blocks
        nextImport:
        for (JRightPadded<J.Import> anImport : originalImports) {
            for (Block block : blocksNoCatchalls) {
                if (block.accept(anImport)) {
                    layoutState.claimImport(block, anImport);
                    continue nextImport;
                }
            }

            for (Block block : blocksOnlyCatchalls) {
                if (block.accept(anImport)) {
                    layoutState.claimImport(block, anImport);
                    continue nextImport;
                }
            }
        }

        int importIndex = 0;
        int extraLineSpaceCount = 0;
        String prevWhitespace = "";
        for (Block block : layout) {
            if (block instanceof Block.BlankLines) {
                extraLineSpaceCount = 0;
                for (int i = 0; i < ((Block.BlankLines) block).getCount(); i++) {
                    extraLineSpaceCount += 1;
                }
            } else {
                List<JRightPadded<J.Import>> blockOrdering = block.orderedImports(layoutState, classCountToUseStarImport, nameCountToUseStarImport, importLayoutConflictDetection, packagesToFold);
                for (JRightPadded<J.Import> orderedImport : blockOrdering) {
                    boolean whitespaceContainsCRLF = orderedImport.getElement().getPrefix().getWhitespace().contains("\r\n");
                    Space prefix;
                    if (importIndex == 0) {
                        prefix = originalImports.get(0).getElement().getPrefix();
                    } else {
                        // Preserve the existing newline character type of either CRLF or LF.
                        // Classic Mac OS new line return '\r' is replaced by '\n'.
                        String newLineCharacters = whitespaceContainsCRLF ||
                                StringUtils.isNullOrEmpty(orderedImport.getElement().getPrefix().getWhitespace()) &&
                                        "\r\n".equals(prevWhitespace) ? "\r\n" : "\n";

                        StringBuilder newWhitespace = new StringBuilder(newLineCharacters);
                        for (int i = 0; i < extraLineSpaceCount; i++) {
                            newWhitespace.append(newLineCharacters);
                        }
                        prefix = orderedImport.getElement().getPrefix().withWhitespace(newWhitespace.toString());
                    }

                    if (!orderedImport.getElement().getPrefix().equals(prefix)) {
                        orderedImports.add(orderedImport.withElement(orderedImport.getElement()
                                .withPrefix(prefix)));
                    } else {
                        orderedImports.add(orderedImport);
                    }
                    // Imports with null or empty whitespace will be set to the previous prefix.
                    prevWhitespace = whitespaceContainsCRLF ? "\r\n" : "\n";
                    extraLineSpaceCount = 0;
                    importIndex++;
                }
            }
        }

        return orderedImports;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final List<Block> blocks = new ArrayList<>();
        private final List<Block> packagesToFold = new ArrayList<>();
        private int classCountToUseStarImport = 5;
        private int nameCountToUseStarImport = 3;

        public Builder importAllOthers() {
            blocks.add(new Block.AllOthers(false));
            return this;
        }

        public Builder importStaticAllOthers() {
            blocks.add(new Block.AllOthers(true));
            return this;
        }

        public Builder blankLine() {
            if (!blocks.isEmpty() &&
                    blocks.get(blocks.size() - 1) instanceof Block.BlankLines) {
                ((Block.BlankLines) blocks.get(blocks.size() - 1)).count++;
            } else {
                blocks.add(new Block.BlankLines());
            }
            return this;
        }

        public Builder importPackage(String packageWildcard, Boolean withSubpackages) {
            blocks.add(new Block.ImportPackage(false, packageWildcard, withSubpackages));
            return this;
        }

        public Builder importPackage(String packageWildcard) {
            return importPackage(packageWildcard, true);
        }

        public Builder staticImportPackage(String packageWildcard, Boolean withSubpackages) {
            blocks.add(new Block.ImportPackage(true, packageWildcard, withSubpackages));
            return this;
        }

        public Builder staticImportPackage(String packageWildcard) {
            return staticImportPackage(packageWildcard, true);
        }

        public Builder packageToFold(String packageWildcard, Boolean withSubpackages) {
            packagesToFold.add(new Block.ImportPackage(false, packageWildcard, withSubpackages));
            return this;
        }

        public Builder packageToFold(String packageWildcard) {
            return packageToFold(packageWildcard, true);
        }

        public Builder staticPackageToFold(String packageWildcard, Boolean withSubpackages) {
            packagesToFold.add(new Block.ImportPackage(true, packageWildcard, withSubpackages));
            return this;
        }

        public Builder staticPackageToFold(String packageWildcard) {
            return staticPackageToFold(packageWildcard, true);
        }

        public Builder classCountToUseStarImport(int classCountToUseStarImport) {
            this.classCountToUseStarImport = classCountToUseStarImport;
            return this;
        }

        public Builder nameCountToUseStarImport(int nameCountToUseStarImport) {
            this.nameCountToUseStarImport = nameCountToUseStarImport;
            return this;
        }

        public ImportLayoutStyle build() {
            assert (blocks.stream().anyMatch(it -> it instanceof Block.AllOthers && ((Block.AllOthers) it).isStatic()))
                    : "There must be at least one block that accepts all static imports, but no such block was found in the specified layout";
            assert (blocks.stream().anyMatch(it -> it instanceof Block.AllOthers && !((Block.AllOthers) it).isStatic()))
                    : "There must be at least one block that accepts all non-static imports, but no such block was found in the specified layout";

            for (Block block : blocks) {
                if (block instanceof Block.AllOthers) {
                    ((Block.AllOthers) block).setPackageImports(blocks.stream()
                            .filter(b -> b.getClass().equals(Block.ImportPackage.class))
                            .map(Block.ImportPackage.class::cast)
                            .collect(toList()));
                }
            }
            return new ImportLayoutStyle(classCountToUseStarImport, nameCountToUseStarImport, blocks, packagesToFold);
        }
    }

    /**
     * The in-progress state of a single layout operation.
     */
    private static class LayoutState {
        Map<Block, List<JRightPadded<J.Import>>> imports = new HashMap<>();

        public void claimImport(Block block, JRightPadded<J.Import> import_) {
            imports.computeIfAbsent(block, b -> new ArrayList<>()).add(import_);
        }

        public List<JRightPadded<J.Import>> getImports(Block block) {
            return imports.getOrDefault(block, emptyList());
        }
    }

    public static boolean isPackageAlwaysFolded(List<Block> packagesToFold, J.Import checkImport) {
        boolean isPackageFolded = false;
        String anImportName = checkImport.getQualid().printTrimmed(new JavaPrinter<>());
        for (Block block : packagesToFold) {
            Block.ImportPackage importPackage = (Block.ImportPackage) block;
            if (checkImport.isStatic() == importPackage.isStatic()) {
                if (importPackage.packageWildcard.matcher(anImportName).matches()) {
                    isPackageFolded = true;
                }
            }
        }
        return isPackageFolded;
    }

    private static class ImportLayoutConflictDetection {
        private final Collection<JavaType.FullyQualified> classpath;
        private final List<JRightPadded<J.Import>> originalImports;
        private final Set<String> jvmClasspathNames = new HashSet<>();
        private @Nullable Set<String> containsClassNameConflict = null;

        ImportLayoutConflictDetection(Collection<JavaType.FullyQualified> classpath, List<JRightPadded<J.Import>> originalImports) {
            this.classpath = classpath;
            this.originalImports = originalImports;
        }

        /**
         * Checks if folding the package will create a namespace conflict with any other classes that have already been imported.
         *
         * @param packageName package that qualifies for folding into a '*'.
         * @return folding the package will not create any namespace conflicts.
         */
        public boolean isPackageFoldable(String packageName) {
            if (containsClassNameConflict == null) {
                containsClassNameConflict = new HashSet<>();
                setJVMClassNames();

                Map<String, Set<String>> nameToPackages = mapNamesInPackageToPackages();
                for (String className : nameToPackages.keySet()) {
                    if (nameToPackages.get(className).size() > 1 || jvmClasspathNames.contains(className)) {
                        containsClassNameConflict.addAll(nameToPackages.get(className));
                    }
                }
            }
            return classpath.isEmpty() || !containsClassNameConflict.contains(packageName);
        }

        private void setJVMClassNames() {
            for (JavaType.FullyQualified fqn : classpath) {
                if ("java.lang".equals(fqn.getPackageName())) {
                    jvmClasspathNames.add(fqn.getClassName());
                }
            }
        }

        private Map<String, Set<String>> mapNamesInPackageToPackages() {
            Map<String, Set<String>> nameToPackages = new HashMap<>();
            Set<String> checkPackageForClasses = new HashSet<>();

            for (JRightPadded<J.Import> anImport : originalImports) {
                checkPackageForClasses.add(packageOrOuterClassName(anImport));
                nameToPackages.computeIfAbsent(anImport.getElement().getClassName(), p -> new HashSet<>())
                                .add(anImport.getElement().getPackageName());
            }

            for (JavaType.FullyQualified classGraphFqn : classpath) {
                String packageName = classGraphFqn.getPackageName();
                if (checkPackageForClasses.contains(packageName)) {
                    String className = classGraphFqn.getClassName();
                    Set<String> packages = nameToPackages.getOrDefault(className, new HashSet<>());
                    packages.add(packageName);
                    nameToPackages.put(className, packages);
                } else if (checkPackageForClasses.contains(classGraphFqn.getFullyQualifiedName())) {
                    packageName = classGraphFqn.getFullyQualifiedName();
                    for (JavaType.Variable member : classGraphFqn.getMembers()) {
                        if (member.getFlags().contains(Flag.Static)) {
                            Set<String> packages = nameToPackages.getOrDefault(member.getName(), new HashSet<>());
                            packages.add(packageName);
                            nameToPackages.put(member.getName(), packages);
                        }
                    }

                    for (JavaType.Method method : classGraphFqn.getMethods()) {
                        if (method.getFlags().contains(Flag.Static)) {
                            Set<String> packages = nameToPackages.getOrDefault(method.getName(), new HashSet<>());
                            packages.add(packageName);
                            nameToPackages.put(method.getName(), packages);
                        }
                    }
                }
            }
            return nameToPackages;
        }
    }

    /**
     * A block represents a grouping of imports based on matching rules. The block provides a mechanism for matching
     * and storing J.Imports that belong to the block.
     */
    public interface Block {

        /**
         * This method will determine if the passed in import is a match for the rules defined on the block. If the
         * import is matched, it will be internally stored in the block.
         *
         * @param anImport The import to be compared against the block's matching rules.
         * @return {@code true} if the import was a match
         */
        boolean accept(JRightPadded<J.Import> anImport);

        /**
         * @return Imports belonging to this block, folded appropriately.
         */
        List<JRightPadded<J.Import>> orderedImports(LayoutState layoutState, int classCountToUseStarImport, int nameCountToUseStarImport, ImportLayoutConflictDetection importLayoutConflictDetection, List<Block> packagesToFold);

        /**
         * A specialized block implementation to act as a blank line separator between import groupings.
         */
        class BlankLines implements Block {
            private int count = 1;

            private int getCount() {
                return count;
            }

            @Override
            public boolean accept(JRightPadded<J.Import> anImport) {
                return false;
            }

            @Override
            public List<JRightPadded<J.Import>> orderedImports(LayoutState layoutState, int classCountToUseStarImport, int nameCountToUseStartImport, ImportLayoutConflictDetection importLayoutConflictDetection, List<Block> packagesToFold) {
                return emptyList();
            }

            @Override
            public String toString() {
                return "<blank line>" + (count > 1 ? " (x" + count + ")" : "");
            }
        }

        @SuppressWarnings("deprecation")
        class ImportPackage implements Block {

            // VisibleForTesting
            final static Comparator<JRightPadded<J.Import>> IMPORT_SORTING = (i1, i2) -> {
                String[] import1 = i1.getElement().getQualid().printTrimmed().split("\\.");
                String[] import2 = i2.getElement().getQualid().printTrimmed().split("\\.");

                for (int i = 0; i < Math.min(import1.length, import2.length); i++) {
                    int diff = import1[i].compareTo(import2[i]);
                    if (diff != 0) {
                        return diff;
                    }
                }

                if (import1.length == import2.length) {
                    return 0;
                }

                return import1.length > import2.length ? 1 : -1;
            };

            private final Boolean statik;
            private final Pattern packageWildcard;

            public ImportPackage(Boolean statik, String packageWildcard, boolean withSubpackages) {
                this.statik = statik;
                this.packageWildcard = Pattern.compile(packageWildcard
                        .replace(".", "\\.")
                        .replace("*", withSubpackages ? ".+" : "[^.]+"));
            }

            public boolean isStatic() {
                return statik;
            }

            public Pattern getPackageWildcard() {
                return packageWildcard;
            }

            @Override
            public boolean accept(JRightPadded<J.Import> anImport) {
                return anImport.getElement().isStatic() == statik &&
                        packageWildcard.matcher(anImport.getElement().getQualid().printTrimmed()).matches();
            }

            @Override
            public List<JRightPadded<J.Import>> orderedImports(LayoutState layoutState, int classCountToUseStarImport, int nameCountToUseStarImport, ImportLayoutConflictDetection importLayoutConflictDetection, List<Block> packagesToFold) {
                List<JRightPadded<J.Import>> imports = layoutState.getImports(this);

                Map<String, List<JRightPadded<J.Import>>> groupedImports = imports
                        .stream()
                        .sorted(IMPORT_SORTING)
                        .collect(groupingBy(
                                ImportLayoutStyle::packageOrOuterClassName,
                                LinkedHashMap::new, // Use an ordered map to preserve sorting
                                Collectors.toList()
                        ));

                List<JRightPadded<J.Import>> ordered = new ArrayList<>(imports.size());

                for (List<JRightPadded<J.Import>> importGroup : groupedImports.values()) {
                    JRightPadded<J.Import> toStar = importGroup.get(0);
                    int threshold = toStar.getElement().isStatic() ? nameCountToUseStarImport : classCountToUseStarImport;
                    boolean starImportExists = importGroup.stream()
                            .anyMatch(it -> it.getElement().getQualid().getSimpleName().equals("*"));

                    if (importLayoutConflictDetection.isPackageFoldable(packageOrOuterClassName(toStar)) &&
                            (isPackageAlwaysFolded(packagesToFold, toStar.getElement()) || importGroup.size() >= threshold || (starImportExists && importGroup.size() > 1))) {

                        J.FieldAccess qualid = toStar.getElement().getQualid();
                        J.Identifier name = qualid.getName();

                        Set<String> typeNamesInThisGroup = importGroup.stream()
                                .map(im -> im.getElement().getClassName())
                                .collect(Collectors.toSet());

                        Optional<String> oneOfTheTypesIsInAnotherGroupToo = groupedImports.values().stream()
                                .filter(group -> group != importGroup)
                                .flatMap(group -> group.stream()
                                        .filter(im -> typeNamesInThisGroup.contains(im.getElement().getClassName())))
                                .map(im -> im.getElement().getTypeName())
                                .findAny();

                        if (starImportExists || !oneOfTheTypesIsInAnotherGroupToo.isPresent()) {
                            ordered.add(toStar.withElement(toStar.getElement().withQualid(qualid.withName(name.withSimpleName("*")))));
                            continue;
                        }
                    }

                    Predicate<JRightPadded<J.Import>> predicate = distinctBy(t -> t.getElement().printTrimmed(new JavaPrinter<>()));
                    for (JRightPadded<J.Import> importJRightPadded : importGroup) {
                        if (predicate.test(importJRightPadded)) {
                            ordered.add(importJRightPadded);
                        }
                    }
                }

                // interleaves inner classes and outer classes back together which are separated into different groups
                // above for the sake of determining whether groups of outer class or inner class imports need to be star
                // folded/unfolded
                ordered.sort(IMPORT_SORTING);

                return ordered;
            }

            @Override
            public String toString() {
                return "import " + (statik ? "static " : "") + packageWildcard;
            }
        }

        class AllOthers extends Block.ImportPackage {
            private final boolean statik;
            private Collection<ImportPackage> packageImports = emptyList();

            public AllOthers(boolean statik) {
                super(statik, "*", true
                );
                this.statik = statik;
            }

            public void setPackageImports(Collection<ImportPackage> packageImports) {
                this.packageImports = packageImports;
            }

            @Override
            public boolean isStatic() {
                return statik;
            }

            @Override
            public boolean accept(JRightPadded<J.Import> anImport) {
                for (ImportPackage pi : packageImports) {
                    if (pi.accept(anImport)) {
                        return false;
                    }
                }
                return anImport.getElement().isStatic() == statik;
            }

            @Override
            public String toString() {
                return "import " + (statik ? "static " : "") + "all other imports";
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("classWildcards=")
                .append(classCountToUseStarImport)
                .append(", staticWildcards=")
                .append(nameCountToUseStarImport)
                .append('\n');
        for (Block block : layout) {
            s.append(block).append("\n");
        }
        return s.toString();
    }

    private static String packageOrOuterClassName(JRightPadded<J.Import> anImport) {
        String typeName = anImport.getElement().getTypeName();
        if (anImport.getElement().isStatic()) {
            return typeName;
        } else {
            String className = anImport.getElement().getClassName();
            if (className.contains("$")) {
                return anImport.getElement().getPackageName() + "." +
                        className.substring(0, className.lastIndexOf('$'))
                                .replace('$', '.');
            }
            return anImport.getElement().getPackageName();
        }
    }
}

class Deserializer extends JsonDeserializer<ImportLayoutStyle> {
    @Override
    public ImportLayoutStyle deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ImportLayoutStyle.Builder builder = ImportLayoutStyle.builder();
        for (String currentField = null; p.hasCurrentToken() && p.getCurrentToken() != JsonToken.END_OBJECT; p.nextToken()) {
            switch (p.currentToken()) {
                case FIELD_NAME:
                    currentField = p.getCurrentName();
                    break;
                case VALUE_STRING:
                    if ("layout".equals(currentField)) {
                        String block = p.getText().trim();
                        if ("<blank line>".equals(block)) {
                            builder.blankLine();
                        } else if (block.startsWith("import ")) {
                            block = block.substring("import ".length());
                            boolean statik = false;
                            if (block.startsWith("static")) {
                                statik = true;
                                block = block.substring("static ".length());
                            }
                            if ("all other imports".equals(block)) {
                                if (statik) {
                                    builder.importStaticAllOthers();
                                } else {
                                    builder.importAllOthers();
                                }
                            } else {
                                boolean withSubpackages = !block.contains(" without subpackages");
                                block = withSubpackages ? block : block.substring(0, block.indexOf(" without subpackage"));
                                if (statik) {
                                    builder.staticImportPackage(block, withSubpackages);
                                } else {
                                    builder.importPackage(block, withSubpackages);
                                }
                            }
                        } else {
                            throw new IllegalArgumentException("Syntax error in layout block [" + block + "]");
                        }
                    } else if ("packagesToFold".equals(currentField)) {
                        String block = p.getText().trim();
                        if (block.startsWith("import ")) {
                            block = block.substring("import ".length());
                            boolean statik = false;
                            if (block.startsWith("static")) {
                                statik = true;
                                block = block.substring("static ".length());
                            }
                            boolean withSubpackages = !block.contains(" without subpackages");
                            block = withSubpackages ? block : block.substring(0, block.indexOf(" without subpackage"));
                            if (statik) {
                                builder.staticPackageToFold(block, withSubpackages);
                            } else {
                                builder.packageToFold(block, withSubpackages);
                            }
                        }
                    } else {
                        break;
                    }
                    break;
                case VALUE_NUMBER_INT:
                    if ("classCountToUseStarImport".equals(currentField)) {
                        builder.classCountToUseStarImport(p.getValueAsInt());
                    } else if ("nameCountToUseStarImport".equals(currentField)) {
                        builder.nameCountToUseStarImport(p.getValueAsInt());
                    }
                    break;
            }
        }

        return builder.build();
    }
}

class Serializer extends JsonSerializer<ImportLayoutStyle> {

    @Override
    public void serializeWithType(ImportLayoutStyle value, JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSer) throws IOException {
        WritableTypeId typeId = typeSer.typeId(value, JsonToken.START_OBJECT);
        typeSer.writeTypePrefix(gen, typeId);
        serializeFields(value, gen);
        typeSer.writeTypeSuffix(gen, typeId);
    }

    @Override
    public void serialize(ImportLayoutStyle value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        serializeFields(value, gen);
        gen.writeEndObject();
    }

    private void serializeFields(ImportLayoutStyle value, JsonGenerator gen) throws IOException {
        gen.writeNumberField("classCountToUseStarImport", value.getClassCountToUseStarImport());
        gen.writeNumberField("nameCountToUseStarImport", value.getNameCountToUseStarImport());

        @SuppressWarnings("SuspiciousToArrayCall") String[] blocks = value.getLayout().stream()
                .map(block -> {
                    if (block instanceof ImportLayoutStyle.Block.BlankLines) {
                        return "<blank line>";
                    } else if (block instanceof ImportLayoutStyle.Block.AllOthers) {
                        return "import " + (((ImportLayoutStyle.Block.AllOthers) block).isStatic() ? "static " : "") +
                                "all other imports";
                    } else if (block instanceof ImportLayoutStyle.Block.ImportPackage) {
                        ImportLayoutStyle.Block.ImportPackage importPackage = (ImportLayoutStyle.Block.ImportPackage) block;
                        String withSubpackages = importPackage.getPackageWildcard().pattern().contains("[^.]+") ? " without subpackages" : "";
                        return "import " + (importPackage.isStatic() ? "static " : "") + importPackage.getPackageWildcard().pattern()
                                .replace("\\.", ".")
                                .replace(".+", "*")
                                .replace("[^.]+", "*") + withSubpackages;
                    }
                    return new UnsupportedOperationException("Unknown block type " + block.getClass().getName());
                })
                .toArray(String[]::new);

        @SuppressWarnings("SuspiciousToArrayCall") String[] packagesToFold = value.getPackagesToFold().stream()
                .map(block -> {
                    if (block instanceof ImportLayoutStyle.Block.ImportPackage) {
                        ImportLayoutStyle.Block.ImportPackage importPackage = (ImportLayoutStyle.Block.ImportPackage) block;
                        String withSubpackages = importPackage.getPackageWildcard().pattern().contains("[^.]+") ? " without subpackages" : "";
                        return "import " + (importPackage.isStatic() ? "static " : "") + importPackage.getPackageWildcard().pattern()
                                .replace("\\.", ".")
                                .replace(".+", "*")
                                .replace("[^.]+", "*")  + withSubpackages;
                    }
                    return new UnsupportedOperationException("Unknown block type " + block.getClass().getName());
                })
                .toArray(String[]::new);

        gen.writeArrayFieldStart("layout");
        gen.writeArray(blocks, 0, blocks.length);
        gen.writeEndArray();

        gen.writeArrayFieldStart("packagesToFold");
        gen.writeArray(packagesToFold, 0, packagesToFold.length);
        gen.writeEndArray();
    }
}
