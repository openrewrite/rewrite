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

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import lombok.Value;
import org.openrewrite.java.JavaStyle;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.Space;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
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
@Value
@Getter
@JsonDeserialize(using = Deserializer.class)
@JsonSerialize(using = Serializer.class)
public class ImportLayoutStyle implements JavaStyle {
    Integer classCountToUseStarImport;
    Integer nameCountToUseStarImport;

    @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
    List<Block> layout;

    /**
     * This method will order and group a list of imports producing a new list that conforms to the rules defined
     * by the import layout style.
     *
     * @param originalImports A list of potentially unordered imports.
     * @return A list of imports that are grouped and ordered.
     */
    public List<JRightPadded<J.Import>> orderImports(List<JRightPadded<J.Import>> originalImports) {
        List<JRightPadded<J.Import>> orderedImports = new ArrayList<>();
        assert (layout.stream().anyMatch(it -> it instanceof Block.AllOthers && ((Block.AllOthers) it).isStatic()))
                : "There must be at least one block that accepts all static imports, but no such block was found in the specified layout";
        assert (layout.stream().anyMatch(it -> it instanceof Block.AllOthers && !((Block.AllOthers) it).isStatic()))
                : "There must be at least one block that accepts all non-static imports, but no such block was found in the specified layout";

        // Divide the blocks into those that accept imports from any package ("catchalls") and those that accept imports from only specific packages
        Map<Boolean, List<Block>> blockGroups = layout.stream()
                .collect(Collectors.partitioningBy(block -> block instanceof Block.AllOthers));
        List<Block> blocksNoCatchalls = blockGroups.get(false);
        List<Block> blocksOnlyCatchalls = blockGroups.get(true);

        // Allocate imports to blocks, preferring to put imports into non-catchall blocks
        for (JRightPadded<J.Import> anImport : originalImports) {
            boolean accepted = false;
            for (Block block : blocksNoCatchalls) {
                if (block.accept(anImport)) {
                    accepted = true;
                    break;
                }
            }
            if (!accepted) {
                for (Block block : blocksOnlyCatchalls) {
                    if (block.accept(anImport)) {
                        break;
                    }
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
                for (JRightPadded<J.Import> orderedImport : block.orderedImports(classCountToUseStarImport, nameCountToUseStarImport)) {
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
        private Integer classCountToUseStarImport = 5;
        private Integer nameCountToUseStarImport = 3;

        public Builder importAllOthers() {
            blocks.add(new Block.AllOthers(false, classCountToUseStarImport, nameCountToUseStarImport));
            return this;
        }

        public Builder importStaticAllOthers() {
            blocks.add(new Block.AllOthers(true, classCountToUseStarImport, nameCountToUseStarImport));
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
            blocks.add(new Block.ImportPackage(false, packageWildcard, withSubpackages, classCountToUseStarImport, nameCountToUseStarImport));
            return this;
        }

        public Builder importPackage(String packageWildcard) {
            return importPackage(packageWildcard, true);
        }

        public Builder staticImportPackage(String packageWildcard, Boolean withSubpackages) {
            blocks.add(new Block.ImportPackage(true, packageWildcard, withSubpackages, classCountToUseStarImport, nameCountToUseStarImport));
            return this;
        }

        public Builder staticImportPackage(String packageWildcard) {
            return staticImportPackage(packageWildcard, true);
        }

        public Builder classCountToUseStarImport(Integer classCountToUseStarImport) {
            this.classCountToUseStarImport = classCountToUseStarImport;
            return this;
        }

        public Builder nameCountToUseStarImport(Integer nameCountToUseStarImport) {
            this.nameCountToUseStarImport = nameCountToUseStarImport;
            return this;
        }

        public ImportLayoutStyle build() {
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
     * A block represents a grouping of imports based on matching rules. The block provides a mechanism for matching
     * and storing J.Imports that belong to the block.
     */
    public interface Block {

        /**
         * This method will determine if the passed in import is a match for the rules defined on the block. If the
         * import is matched, it will be internally stored in the block.
         *
         * @param anImport The import to be compared against the block's matching rules.
         * @return true if the import was a match (and was stored within the block)
         */
        Boolean accept(JRightPadded<J.Import> anImport);

        /**
         * @return Imports belonging to this block, folded appropriately.
         */
        List<JRightPadded<J.Import>> orderedImports(int classCountToUseStarImport, int nameCountToUseStarImport);

        /**
         * A specialized block implementation to act as a blank line separator between import groupings.
         */
        class BlankLines implements Block {
            private int count = 1;

            private int getCount() {
                return count;
            }

            @Override
            public Boolean accept(JRightPadded<J.Import> anImport) {
                return false;
            }

            @Override
            public List<JRightPadded<J.Import>> orderedImports(int classCountToUseStarImport, int nameCountToUseStartImport) {
                return emptyList();
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

            private final List<JRightPadded<J.Import>> imports = new ArrayList<>();

            private final Boolean statik;
            private final Pattern packageWildcard;

            public ImportPackage(Boolean statik, String packageWildcard, Boolean withSubpackages,
                                 Integer classCountToUseStarImport, Integer nameCountToUseStarImport) {
                this.statik = statik;
                this.packageWildcard = Pattern.compile(packageWildcard
                        .replace(".", "\\.")
                        .replace("*", withSubpackages ? ".+" : "[^.]+"));
            }

            public Boolean isStatic() {
                return statik;
            }

            public Pattern getPackageWildcard() {
                return packageWildcard;
            }

            @Override
            public Boolean accept(JRightPadded<J.Import> anImport) {
                if (anImport.getElement().isStatic() == statik &&
                        packageWildcard.matcher(anImport.getElement().getQualid().printTrimmed()).matches()) {
                    imports.add(anImport);
                    return true;
                }
                return false;
            }

            @Override
            public List<JRightPadded<J.Import>> orderedImports(int classCountToUseStarImport, int nameCountToUseStarImport) {
                Map<String, List<JRightPadded<J.Import>>> groupedImports = imports
                        .stream()
                        .sorted(IMPORT_SORTING)
                        .collect(groupingBy(
                                anImport -> {
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
                                },
                                LinkedHashMap::new, // Use an ordered map to preserve sorting
                                Collectors.toList()
                        ));

                return groupedImports.values().stream()
                        .flatMap(importGroup -> {
                            JRightPadded<J.Import> toStar = importGroup.get(0);
                            boolean statik1 = toStar.getElement().isStatic();
                            Integer threshold = statik1 ? nameCountToUseStarImport : classCountToUseStarImport;
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
                                    return Stream.of(toStar.withElement(toStar.getElement().withQualid(qualid.withName(
                                            name.withName("*")))));
                                }
                            }

                            return importGroup.stream()
                                    .filter(distinctBy(t -> t.getElement().printTrimmed()));
                        }).collect(toList());
            }
        }

        class AllOthers extends Block.ImportPackage {
            private final Boolean statik;
            private Collection<ImportPackage> packageImports = emptyList();

            public AllOthers(Boolean statik, Integer classCountToUseStarImport, Integer nameCountToUseStarImport) {
                super(statik, "*", true,
                        classCountToUseStarImport, nameCountToUseStarImport);
                this.statik = statik;
            }

            public void setPackageImports(Collection<ImportPackage> packageImports) {
                this.packageImports = packageImports;
            }

            public Boolean isStatic() {
                return statik;
            }

            @Override
            public Boolean accept(JRightPadded<J.Import> anImport) {
                if (packageImports.stream().noneMatch(pi -> pi.accept(anImport))) {
                    super.accept(anImport);
                }
                return anImport.getElement().isStatic() == statik;
            }
        }
    }
}

class Deserializer extends JsonDeserializer<ImportLayoutStyle> {
    @Override
    public ImportLayoutStyle deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ImportLayoutStyle.Builder builder = ImportLayoutStyle.builder();
        for (String currentField = null; p.hasCurrentToken(); p.nextToken()) {
            switch (p.currentToken()) {
                case FIELD_NAME:
                    currentField = p.getCurrentName();
                    break;
                case VALUE_STRING:
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
    public void serialize(ImportLayoutStyle value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeNumberField("classCountToUseStarImport", value.getClassCountToUseStarImport());
        gen.writeNumberField("nameCountToUseStarImport", value.getNameCountToUseStarImport());

        String[] blocks = value.getLayout().stream()
                .map(block -> {
                    if (block instanceof ImportLayoutStyle.Block.BlankLines) {
                        return "<blank line>";
                    } else if (block instanceof ImportLayoutStyle.Block.AllOthers) {
                        return "import " + (((ImportLayoutStyle.Block.AllOthers) block).isStatic() ? "static " : "") +
                                "all other imports";
                    } else if (block instanceof ImportLayoutStyle.Block.ImportPackage) {
                        ImportLayoutStyle.Block.ImportPackage importPackage = (ImportLayoutStyle.Block.ImportPackage) block;
                        return (importPackage.isStatic() ? "static " : "") +
                                "import " + importPackage.getPackageWildcard();
                    }
                    return new UnsupportedOperationException("Unknown block type " + block.getClass().getName());
                })
                .toArray(String[]::new);

        gen.writeArrayFieldStart("layout");
        gen.writeArray(blocks, 0, blocks.length);
        gen.writeEndArray();
    }
}
