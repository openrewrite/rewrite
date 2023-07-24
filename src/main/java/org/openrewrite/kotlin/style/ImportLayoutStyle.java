/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.kotlin.style;

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
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.KotlinStyle;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

/**
 * A Java Style to define how imports are grouped and ordered. Additionally, this style provides configuration to dictate
 * how wildcard folding should be applied when multiple imports are in the same package or on the same, statically-imported
 * type.
 * <P><P>
 * The import layout consist of three properties:
 * <P><P>
 * <li>topLevelSymbolsToUseStarImport  - How many imports from the same package must be present before they should be collapsed into a star import. The default is 5.</li>
 * <li>javaStaticsAndEnumsToUseStarImport - How many java static and enum imports from the same type must be present before they should be collapsed into a star import. The default is 3.</li>
 * <li>layout - An ordered list of import groupings which define exactly how imports should be organized within a compilation unit.</li>
 * <li>packagesToFold - An ordered list of packages which are folded when 1 or more types are in use.</li>
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
@JsonDeserialize(using = Deserializer.class)
@JsonSerialize(using = Serializer.class)
public class ImportLayoutStyle implements KotlinStyle {

    @EqualsAndHashCode.Include
    private final int topLevelSymbolsToUseStarImport;

    @EqualsAndHashCode.Include
    private final int javaStaticsAndEnumsToUseStarImport;

    @EqualsAndHashCode.Include
    private final List<Block> layout;

    @EqualsAndHashCode.Include
    private final List<Block> packagesToFold;

    private final List<Block> blocksNoCatchalls;
    private final List<Block> blocksOnlyCatchalls;

    public ImportLayoutStyle(int topLevelSymbolsToUseStarImport, int javaStaticsAndEnumsToUseStarImport, List<Block> layout, List<Block> packagesToFold) {
        this.topLevelSymbolsToUseStarImport = topLevelSymbolsToUseStarImport;
        this.javaStaticsAndEnumsToUseStarImport = javaStaticsAndEnumsToUseStarImport;
        this.layout = layout.isEmpty() ? IntelliJ.importLayout().getLayout() : layout;
        this.packagesToFold = packagesToFold;

        // Divide the blocks into those that accept imports from any package ("catchalls") and those that accept imports from only specific packages
        Map<Boolean, List<Block>> blockGroups = layout.stream()
                .collect(Collectors.partitioningBy(block -> block instanceof Block.AllOthers));
        blocksNoCatchalls = blockGroups.get(false);
        blocksOnlyCatchalls = blockGroups.get(true);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final List<Block> blocks = new ArrayList<>();
        private final List<Block> packagesToFold = new ArrayList<>();
        private int topLevelSymbolsToUseStarImport = 5;
        private int javaStaticsAndEnumsToUseStarImport = 3;

        public Builder importAllOthers() {
            blocks.add(new Block.AllOthers());
            return this;
        }

        public Builder importAllAliases() {
            blocks.add(new Block.AllAliases());
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
            blocks.add(new Block.ImportPackage(packageWildcard, withSubpackages));
            return this;
        }

        public Builder importPackage(String packageWildcard) {
            return importPackage(packageWildcard, true);
        }

        public Builder packageToFold(String packageWildcard, Boolean withSubpackages) {
            packagesToFold.add(new Block.ImportPackage(packageWildcard, withSubpackages));
            return this;
        }

        public Builder packageToFold(String packageWildcard) {
            return packageToFold(packageWildcard, true);
        }

        public Builder topLevelSymbolsToUseStarImport(int topLevelSymbolsToUseStarImport) {
            this.topLevelSymbolsToUseStarImport = topLevelSymbolsToUseStarImport;
            return this;
        }

        public Builder javaStaticsAndEnumsToUseStarImport(int javaStaticsAndEnumsToUseStarImport) {
            this.javaStaticsAndEnumsToUseStarImport = javaStaticsAndEnumsToUseStarImport;
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
            return new ImportLayoutStyle(topLevelSymbolsToUseStarImport, javaStaticsAndEnumsToUseStarImport, blocks, packagesToFold);
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

            private final Pattern packageWildcard;

            public ImportPackage(String packageWildcard, boolean withSubpackages) {
                this.packageWildcard = Pattern.compile(packageWildcard
                        .replace(".", "\\.")
                        .replace("*", withSubpackages ? ".+" : "[^.]+"));
            }

            public Pattern getPackageWildcard() {
                return packageWildcard;
            }

            @Override
            public boolean accept(JRightPadded<J.Import> anImport) {
                return packageWildcard.matcher(anImport.getElement().getQualid().printTrimmed()).matches();
            }

            @Override
            public String toString() {
                return "import " + packageWildcard;
            }
        }

        class AllOthers extends ImportPackage {
            private Collection<ImportPackage> packageImports = emptyList();

            public AllOthers() {
                super("*", true);
            }

            public void setPackageImports(Collection<ImportPackage> packageImports) {
                this.packageImports = packageImports;
            }

            @Override
            public boolean accept(JRightPadded<J.Import> anImport) {
                for (ImportPackage pi : packageImports) {
                    if (pi.accept(anImport)) {
                        return false;
                    }
                }
                return true;
            }

            @Override
            public String toString() {
                return "import all other imports";
            }
        }

        class AllAliases extends ImportPackage {
            private Collection<ImportPackage> packageImports = emptyList();

            public AllAliases() {
                super("*", true);
            }

            public void setPackageImports(Collection<ImportPackage> packageImports) {
                this.packageImports = packageImports;
            }

            @Override
            public boolean accept(JRightPadded<J.Import> anImport) {
                for (ImportPackage pi : packageImports) {
                    if (pi.accept(anImport)) {
                        return false;
                    }
                }
                return true;
            }

            @Override
            public String toString() {
                return "import all alias imports";
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("topLevelSymbols=")
                .append(topLevelSymbolsToUseStarImport)
                .append(", javaStaticAndEnums=")
                .append(javaStaticsAndEnumsToUseStarImport)
                .append('\n');
        for (Block block : layout) {
            s.append(block).append("\n");
        }
        return s.toString();
    }
}

class Deserializer extends JsonDeserializer<ImportLayoutStyle> {
    // TODO: verify deserialization with tests.
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
                            if ("all other imports".equals(block)) {
                                builder.importAllOthers();
                            } else {
                                boolean withSubpackages = !block.contains(" without subpackages");
                                block = withSubpackages ? block : block.substring(0, block.indexOf(" without subpackage"));
                                builder.importPackage(block, withSubpackages);
                            }
                        } else {
                            throw new IllegalArgumentException("Syntax error in layout block [" + block + "]");
                        }
                    } else if ("packagesToFold".equals(currentField)) {
                        String block = p.getText().trim();
                        if (block.startsWith("import ")) {
                            block = block.substring("import ".length());
                            boolean withSubpackages = !block.contains(" without subpackages");
                            block = withSubpackages ? block : block.substring(0, block.indexOf(" without subpackage"));
                            builder.packageToFold(block, withSubpackages);
                        }
                    } else {
                        break;
                    }
                    break;
                case VALUE_NUMBER_INT:
                    if ("topLevelSymbolsToUseStarImport".equals(currentField)) {
                        builder.topLevelSymbolsToUseStarImport(p.getValueAsInt());
                    } else if ("javaStaticsAndEnumsToUseStarImport".equals(currentField)) {
                        builder.javaStaticsAndEnumsToUseStarImport(p.getValueAsInt());
                    }
                    break;
            }
        }

        return builder.build();
    }
}

class Serializer extends JsonSerializer<ImportLayoutStyle> {

    // TODO: verify serialization with tests.
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
        gen.writeNumberField("topLevelSymbolsToUseStarImport", value.getTopLevelSymbolsToUseStarImport());
        gen.writeNumberField("javaStaticsAndEnumsToUseStarImport", value.getJavaStaticsAndEnumsToUseStarImport());

        @SuppressWarnings("SuspiciousToArrayCall") String[] blocks = value.getLayout().stream()
                .map(block -> {
                    if (block instanceof ImportLayoutStyle.Block.BlankLines) {
                        return "<blank line>";
                    } else if (block instanceof ImportLayoutStyle.Block.AllOthers) {
                        return "import all other imports";
                    } else if (block instanceof ImportLayoutStyle.Block.ImportPackage) {
                        ImportLayoutStyle.Block.ImportPackage importPackage = (ImportLayoutStyle.Block.ImportPackage) block;
                        String withSubpackages = importPackage.getPackageWildcard().pattern().contains("[^.]+") ? " without subpackages" : "";
                        return "import " + importPackage.getPackageWildcard().pattern()
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
                        return "import " + importPackage.getPackageWildcard().pattern()
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
