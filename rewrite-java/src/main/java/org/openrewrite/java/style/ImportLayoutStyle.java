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
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaStyle;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.Space;
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
 * <li>layout - An ordered list of import groupings which define exactly how imports should be organized within a compilation unit</li>
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

    private final List<Block> blocksNoCatchalls;
    private final List<Block> blocksOnlyCatchalls;

    public ImportLayoutStyle(int classCountToUseStarImport, int nameCountToUseStarImport, List<Block> layout) {
        this.classCountToUseStarImport = classCountToUseStarImport;
        this.nameCountToUseStarImport = nameCountToUseStarImport;
        this.layout = layout;

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
                                                  J.Import toAdd, @Nullable J.Package pkg) {
        JRightPadded<J.Import> paddedToAdd = new JRightPadded<>(toAdd, Space.EMPTY, Markers.EMPTY);

        if (originalImports.isEmpty()) {
            return singletonList(pkg == null ?
                    paddedToAdd :
                    paddedToAdd.withElement(paddedToAdd.getElement().withPrefix(Space.format("\n\n")))
            );
        }

        // don't star fold just yet, because we are only going to star fold adjacent imports along with
        // the import to add at most. we don't even want to star fold other non-adjacent imports in the same
        // block that should be star folded according to the layout style (minimally invasive change).
        List<JRightPadded<J.Import>> ideallyOrdered = new ImportLayoutStyle(Integer.MAX_VALUE, Integer.MAX_VALUE, layout)
                .orderImports(ListUtils.concat(originalImports, paddedToAdd));

        if (ideallyOrdered.size() == originalImports.size()) {
            // must be a duplicate of an existing import
            return originalImports;
        }

        JRightPadded<J.Import> before = null;
        JRightPadded<J.Import> after = null;

        Block addToBlock = block(paddedToAdd);
        int insertPosition = 0;

        for (int i = 0; i < ideallyOrdered.size(); i++) {
            JRightPadded<J.Import> anImport = ideallyOrdered.get(i);
            if (anImport.getElement().isScope(paddedToAdd.getElement())) {
                before = i > 0 ? ideallyOrdered.get(i - 1) : null;
                if (before == null) {
                    after = originalImports.isEmpty() ? null : originalImports.get(0);
                } else {
                    for (int j = 0; j < originalImports.size(); j++) {
                        if (before == originalImports.get(j)) {
                            after = originalImports.size() > j + 1 ? originalImports.get(j + 1) : null;
                            break;
                        }
                    }
                }

                insertPosition = i;
                break;
            }
        }

        if (before == null) {
            if (pkg != null) {
                Space prefix = originalImports.get(0).getElement().getPrefix();
                paddedToAdd = paddedToAdd.withElement(paddedToAdd.getElement().withPrefix(prefix));
            }
        } else if (block(before) != addToBlock) {
            paddedToAdd = paddedToAdd.withElement(paddedToAdd.getElement().withPrefix(Space.format("\n\n")));
        } else {
            paddedToAdd = paddedToAdd.withElement(paddedToAdd.getElement().withPrefix(Space.format("\n")));
        }

        AtomicInteger starFoldFrom = new AtomicInteger(0);
        AtomicInteger starFoldTo = new AtomicInteger(0);
        AtomicBoolean starFold = new AtomicBoolean(false);
        for (int i = insertPosition; i < originalImports.size(); i++) {
            JRightPadded<J.Import> anImport = originalImports.get(i);
            if (block(anImport) == addToBlock && packageOrOuterClassName(anImport)
                    .equals(packageOrOuterClassName(paddedToAdd))) {
                starFoldTo.set(i);
            } else {
                break;
            }
        }
        for (int i = starFoldTo.get(); i >= insertPosition ; i--) {
            JRightPadded<J.Import> anImport = originalImports.get(i);
            if (block(anImport) == addToBlock && packageOrOuterClassName(anImport)
                    .equals(packageOrOuterClassName(paddedToAdd))) {
                starFoldFrom.set(i);
            } else {
                break;
            }
        }

        if ((paddedToAdd.getElement().isStatic() && nameCountToUseStarImport <= starFoldTo.get() - starFoldFrom.get() + 2) ||
                (!paddedToAdd.getElement().isStatic() && classCountToUseStarImport <= starFoldTo.get() - starFoldFrom.get() + 2)) {
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
                            paddedToAdd.getElement().getQualid().getName().withName("*")
                    )
            ));
            after = starFoldTo.get() < originalImports.size() - 1 ?
                    originalImports.get(starFoldTo.get() + 1) : null;
        }

        if (after != null) {
            if (block(after) == addToBlock) {
                after = after.withElement(after.getElement().withPrefix(Space.format("\n")));
            } else if (after.getElement().getPrefix().getLastWhitespace().chars()
                    .filter(c -> c == '\n').count() < 2) {
                after = after.withElement(after.getElement().withPrefix(Space.format("\n\n")));
            }
        }

        JRightPadded<J.Import> finalToAdd = paddedToAdd;
        JRightPadded<J.Import> finalAfter = after;
        return ListUtils.flatMap(originalImports, (i, anImport) -> {
            if (starFold.get() && i >= starFoldFrom.get() && i <= starFoldTo.get()) {
                return i == starFoldFrom.get() ?
                        finalToAdd /* only add the star import once */ :
                        null;
            } else if (finalAfter != null && anImport.getElement().isScope(finalAfter.getElement())) {
                return Arrays.asList(finalToAdd, finalAfter);
            } else if (i == originalImports.size() - 1 && finalAfter == null) {
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
    public List<JRightPadded<J.Import>> orderImports(List<JRightPadded<J.Import>> originalImports) {
        LayoutState layoutState = new LayoutState();
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
        String extraLineSpace = "";
        for (Block block : layout) {
            if (block instanceof Block.BlankLines) {
                extraLineSpace = "";
                for (int i = 0; i < ((Block.BlankLines) block).getCount(); i++) {
                    //noinspection StringConcatenationInLoop
                    extraLineSpace += "\n";
                }
            } else {
                for (JRightPadded<J.Import> orderedImport : block.orderedImports(layoutState,
                        classCountToUseStarImport, nameCountToUseStarImport)) {

                    Space prefix = importIndex == 0 ? originalImports.get(0).getElement().getPrefix() :
                            orderedImport.getElement().getPrefix().withWhitespace(extraLineSpace + "\n");

                    if (!orderedImport.getElement().getPrefix().equals(prefix)) {
                        orderedImports.add(orderedImport.withElement(orderedImport.getElement()
                                .withPrefix(prefix)));
                    } else {
                        orderedImports.add(orderedImport);
                    }
                    extraLineSpace = "";
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
            return new ImportLayoutStyle(classCountToUseStarImport, nameCountToUseStarImport, blocks);
        }
    }

    /**
     * The in-progress state of a single layout operation.
     */
    private static class LayoutState {
        Map<Block, List<JRightPadded<J.Import>>> imports = new HashMap<>();

        public void claimImport(Block block, JRightPadded<J.Import> impoort) {
            imports.computeIfAbsent(block, b -> new ArrayList<>()).add(impoort);
        }

        public List<JRightPadded<J.Import>> getImports(Block block) {
            return imports.getOrDefault(block, emptyList());
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
        List<JRightPadded<J.Import>> orderedImports(LayoutState layoutState, int classCountToUseStarImport, int nameCountToUseStarImport);

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
            public List<JRightPadded<J.Import>> orderedImports(LayoutState layoutState, int classCountToUseStarImport, int nameCountToUseStartImport) {
                return emptyList();
            }

            @Override
            public String toString() {
                return "<blank line>" + (count > 1 ? " (x" + count + ")" : "");
            }
        }

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
            public List<JRightPadded<J.Import>> orderedImports(LayoutState layoutState, int classCountToUseStarImport, int nameCountToUseStarImport) {
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
                    boolean statik1 = toStar.getElement().isStatic();
                    int threshold = statik1 ? nameCountToUseStarImport : classCountToUseStarImport;
                    boolean starImportExists = importGroup.stream()
                            .anyMatch(it -> it.getElement().getQualid().getSimpleName().equals("*"));

                    if (importGroup.size() >= threshold || (starImportExists && importGroup.size() > 1)) {
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
                            ordered.add(toStar.withElement(toStar.getElement().withQualid(qualid.withName(
                                    name.withName("*")))));
                            continue;
                        }
                    }

                    Predicate<JRightPadded<J.Import>> predicate = distinctBy(t -> t.getElement().printTrimmed());
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
            if (className.contains(".")) {
                return anImport.getElement().getPackageName() +
                        className.substring(0, className.lastIndexOf('.'));
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
                    if (!"layout".equals(currentField)) {
                        break;
                    }
                    String block = p.getText().trim();
                    if (block.equals("<blank line>")) {
                        builder.blankLine();
                    } else if (block.startsWith("import ")) {
                        block = block.substring("import ".length());
                        boolean statik = false;
                        if (block.startsWith("static")) {
                            statik = true;
                            block = block.substring("static ".length());
                        }
                        if (block.equals("all other imports")) {
                            if (statik) {
                                builder.importStaticAllOthers();
                            } else {
                                builder.importAllOthers();
                            }
                        } else {
                            if (statik) {
                                builder.staticImportPackage(block);
                            } else {
                                builder.importPackage(block);
                            }
                        }
                    } else {
                        throw new IllegalArgumentException("Syntax error in layout block [" + block + "]");
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
                        return "import " + (importPackage.isStatic() ? "static " : "") + importPackage.getPackageWildcard().pattern()
                                .replace("\\.", ".")
                                .replace(".+", "*")
                                .replace("[^.]+", "*");
                    }
                    return new UnsupportedOperationException("Unknown block type " + block.getClass().getName());
                })
                .toArray(String[]::new);

        gen.writeArrayFieldStart("layout");
        gen.writeArray(blocks, 0, blocks.length);
        gen.writeEndArray();
    }
}
