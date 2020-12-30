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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.openrewrite.Validated;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaStyle;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.Validated.test;

public class DeclarationOrderStyle implements JavaStyle {
    private Map<String, Object> layout;

    @JsonProperty("layout")
    public void setLayout(Map<String, Object> layout) {
        this.layout = layout;
    }

    public Layout getLayout() {
        Layout.Builder builder = Layout.builder();

        //noinspection unchecked
        for (String block : (List<String>) layout.get("blocks")) {
            block = block.trim();
            if (block.equals("<blank line>")) {
                builder = builder.blankLine();
            } else if (block.endsWith("fields")) {
                if (block.equals("all other fields")) {
                    builder = builder.allOtherFields();
                    continue;
                }
                String[] modifiers = block.substring(0, block.length() - "fields".length()).split("\\s+");
                builder = builder.fields(modifiers);
            } else if (block.endsWith("classes")) {
                if (block.equals("all other classes")) {
                    builder = builder.allOtherClasses();
                    continue;
                }
                String[] modifiers = block.substring(0, block.length() - "classes".length()).split("\\s+");
                builder = builder.classes(modifiers);
            } else if (block.endsWith("methods")) {
                if (block.equals("all other methods")) {
                    builder = builder.allOtherMethods();
                    continue;
                }
                String[] modifiers = block.substring(0, block.length() - "methods".length()).split("\\s+");
                builder = builder.methods(modifiers);
            } else if (block.endsWith("constructors")) {
                if (block.equals("all other constructors")) {
                    builder = builder.allOtherConstructors();
                    continue;
                }
                String[] modifiers = block.substring(0, block.length() - "constructors".length()).split("\\s+");
                builder = builder.constructors(modifiers);
            } else if (block.equals("all getters")) {
                builder = builder.allGetters();
            } else if (block.equals("all setters")) {
                builder = builder.allSetters();
            } else if (block.equals("all withs")) {
                builder = builder.allWiths();
            } else if (block.equals("all accessors")) {
                builder = builder.allAccessors();
            } else if (block.equals("equals")) {
                builder = builder.equals();
            } else if (block.equals("hashCode")) {
                builder = builder.hashCoding();
            } else if (block.equals("toString")) {
                builder = builder.toStringing();
            }
        }

        return builder.build();
    }

    public static class Layout {
        public static Layout DEFAULT = Layout.builder()
                .fields("public", "static").blankLine()
                .fields("private", "static").blankLine()
                .fields("final").blankLine()
                .allOtherFields().blankLine()
                .allOtherConstructors().blankLine()
                .allOtherMethods().blankLine()
                .allAccessors().blankLine()
                .equals().blankLine()
                .hashCoding().blankLine()
                .toStringing().blankLine()
                .allOtherClasses()
                .build();

        private final List<Block<?>> blocks;

        /**
         * Placed in precedence order, not the order in which the blocks appear in
         * the final result.
         */
        private final List<Block<?>> blocksByPrecedence;

        Layout(List<Block<?>> blocks) {
            this.blocks = blocks;

            this.blocksByPrecedence = new ArrayList<>();
            blocks.stream().filter(b -> !(b instanceof Block.BlankLines))
                    .forEach(blocksByPrecedence::add);

            blocksByPrecedence.sort(new Comparator<Block<?>>() {
                @Override
                public int compare(Block<?> b1, Block<?> b2) {
                    if (b1 instanceof Block.Methods) {
                        if (b2 instanceof Block.Methods) {
                            return methodTypeRank(b1) - methodTypeRank(b2);
                        }
                        return 1;
                    }
                    return ModifierComparator.BY_STRING.compare(b1.requiredModifiers, b2.requiredModifiers);
                }

                private int methodTypeRank(Block<?> b) {
                    if (b instanceof Block.Getters) {
                        return 1;
                    }
                    if (b instanceof Block.Setters) {
                        return 2;
                    }
                    if (b instanceof Block.Withs) {
                        return 3;
                    }
                    if (b instanceof Block.Accessors) {
                        return 4;
                    }
                    if (b instanceof Block.ToString) {
                        return 5;
                    }
                    if (b instanceof Block.HashCode) {
                        return 6;
                    }
                    if (b instanceof Block.Equals) {
                        return 7;
                    }
                    return 8;
                }
            });
        }

        public void reset() {
            for (Block<?> block : blocks) {
                block.reset();
            }
        }

        public void accept(J declaration) {
            for (Block<?> block : blocksByPrecedence) {
                if (block.accept(declaration)) {
                    return;
                }
            }
        }

//        public List<J> orderedDeclarations() {
//            List<J> orderedDeclarations = new ArrayList<>();
//
//            AtomicInteger blankLines = new AtomicInteger(0);
//            for (Block<?> block : blocks) {
//                if (block instanceof Block.BlankLines) {
//                    blankLines.addAndGet(((Block.BlankLines) block).count);
//                } else {
//                    AtomicBoolean first = new AtomicBoolean(true);
//                    orderedDeclarations.addAll(block.orderedDeclarations().stream()
//                            .peek(declaration -> System.out.println("  " + declaration.toString()))
//                            .map(declaration -> {
//                                if (first.getAndSet(false)) {
//                                    return declaration.withFormatting(declaration.getFormatting()
//                                            .withMinimumBlankLines(blankLines.getAndSet(0)));
//                                }
//
//                                if (declaration instanceof J.MethodDecl) {
//                                    return declaration.withFormatting(declaration.getFormatting()
//                                            .withMinimumBlankLines(1));
//                                }
//
//                                return declaration;
//                            })
//                            .collect(toList()));
//                    blankLines.set(0);
//                }
//            }
//            return orderedDeclarations;
//        }

        public Validated validate() {
            return test("accessors", "'all accessors' can only be used when 'all getters', 'all setters', or 'all with' are not",
                    blocks,
                    blocks2 -> blocks2.stream()
                            .filter(b -> b instanceof Block.Getters ||
                                    b instanceof Block.Setters ||
                                    b instanceof Block.Withs)
                            .findAny()
                            .map(conflicting -> blocks.stream().noneMatch(b -> b instanceof Block.Accessors))
                            .orElse(true)
            ).and(
                    test("accessors", "only one 'all accessors' permitted",
                            blocks,
                            blocks2 -> blocks2.stream().filter(b -> b instanceof Block.Accessors).count() == 1)
            ).and(
                    test("getters", "only one 'all getters' permitted",
                            blocks,
                            blocks2 -> blocks2.stream().filter(b -> b instanceof Block.Getters).count() == 1)
            ).and(
                    test("setters", "only one 'all setters' permitted",
                            blocks,
                            blocks2 -> blocks2.stream().filter(b -> b instanceof Block.Setters).count() == 1)
            ).and(
                    test("withs", "only one 'all withs' permitted",
                            blocks,
                            blocks2 -> blocks2.stream().filter(b -> b instanceof Block.Withs).count() == 1)
            );
        }

        private static abstract class Block<T extends J> {
            final List<String> requiredModifiers;

            @Nullable
            final Comparator<T> comparator;

            final List<T> matched = new ArrayList<>();

            protected Block(List<String> requiredModifiers, @Nullable Comparator<T> comparator) {
                this.requiredModifiers = requiredModifiers;
                this.comparator = comparator;
            }

            abstract boolean accept(J declaration);

            final void reset() {
                matched.clear();
            }

            List<? extends J> orderedDeclarations() {
                if (comparator != null) {
                    matched.sort(comparator);
                }
                return matched;
            }

            static class BlankLines extends Block<J> {
                private int count = 1;

                BlankLines() {
                    super(emptyList(), null);
                }

                @Override
                public boolean accept(J declaration) {
                    return false;
                }

                @Override
                public String toString() {
                    return "<blank line>";
                }
            }

            static class Fields extends Block<J.VariableDecls> {
                public Fields(List<String> requiredModifiers) {
                    super(requiredModifiers, (f1, f2) -> {
                        int modComp = ModifierComparator.BY_AST.compare(f1.getModifiers(), f2.getModifiers());
                        if (modComp != 0) {
                            return modComp;
                        }
                        return f1.getVars().get(0).getElem().getSimpleName().compareTo(f2.getVars().get(0).getElem().getSimpleName());
                    });
                }

                @Override
                public boolean accept(J declaration) {
                    if (declaration instanceof J.VariableDecls &&
                            requiredModifiers.stream().allMatch(((J.VariableDecls) declaration)::hasModifier)) {
                        matched.add((J.VariableDecls) declaration);
                        return true;
                    }
                    return false;
                }

                @Override
                public String toString() {
                    return (requiredModifiers.isEmpty() ? "all other" : String.join(" ", requiredModifiers)) +
                            " fields";
                }
            }

            static class Classes extends Block<J.ClassDecl> {
                public Classes(List<String> requiredModifiers) {
                    super(requiredModifiers, (c1, c2) -> {
                        int modComp = ModifierComparator.BY_AST.compare(c1.getModifiers(), c2.getModifiers());
                        if (modComp != 0) {
                            return modComp;
                        }
                        return c1.getSimpleName().compareTo(c2.getSimpleName());
                    });
                }

                @Override
                public boolean accept(J declaration) {
                    if (declaration instanceof J.ClassDecl &&
                            requiredModifiers.stream().allMatch(((J.ClassDecl) declaration)::hasModifier)) {
                        matched.add((J.ClassDecl) declaration);
                        return true;
                    }
                    return false;
                }

                @Override
                public String toString() {
                    return (requiredModifiers.isEmpty() ? "all other" : String.join(" ", requiredModifiers)) +
                            " classes";
                }
            }

            static class Methods extends Block<J.MethodDecl> {
                public Methods(List<String> requiredModifiers) {
                    super(requiredModifiers, (m1, m2) -> {
                        int modComp = ModifierComparator.BY_AST.compare(m1.getModifiers(), m2.getModifiers());
                        if (modComp != 0) {
                            return modComp;
                        }
                        return m1.getSimpleName().compareTo(m2.getSimpleName());
                    });
                }

                @Override
                public boolean accept(J declaration) {
                    if (test(declaration)) {
                        matched.add((J.MethodDecl) declaration);
                        return true;
                    }
                    return false;
                }

                public boolean test(J declaration) {
                    return declaration instanceof J.MethodDecl &&
                            !((J.MethodDecl) declaration).isConstructor() &&
                            requiredModifiers.stream().allMatch(((J.MethodDecl) declaration)::hasModifier);
                }

                @Override
                public String toString() {
                    return (requiredModifiers.isEmpty() ? "all other" : String.join(" ", requiredModifiers)) +
                            " methods";
                }
            }

            static class Constructors extends Block<J.MethodDecl> {
                public Constructors(List<String> requiredModifiers) {
                    super(requiredModifiers, (m1, m2) -> {
                        int modComp = ModifierComparator.BY_AST.compare(m1.getModifiers(), m2.getModifiers());
                        if (modComp != 0) {
                            return modComp;
                        }
                        return m1.getSimpleName().compareTo(m2.getSimpleName());
                    });
                }

                @Override
                public boolean accept(J declaration) {
                    if (declaration instanceof J.MethodDecl &&
                            requiredModifiers.stream().allMatch(((J.MethodDecl) declaration)::hasModifier) &&
                            ((J.MethodDecl) declaration).isConstructor()) {
                        matched.add((J.MethodDecl) declaration);
                        return true;
                    }
                    return false;
                }

                @Override
                public String toString() {
                    return (requiredModifiers.isEmpty() ? "all other" : String.join(" ", requiredModifiers)) +
                            " constructors";
                }
            }

            static class Getters extends Methods {
                public Getters() {
                    super(singletonList("public"));
                }

                @Override
                public boolean test(J declaration) {
                    return super.test(declaration) && ((J.MethodDecl) declaration)
                            .getSimpleName().startsWith("get");
                }

                @Override
                public String toString() {
                    return "all getters";
                }
            }

            static class Setters extends Methods {
                public Setters() {
                    super(singletonList("public"));
                }

                @Override
                public boolean test(J declaration) {
                    return super.test(declaration) && ((J.MethodDecl) declaration)
                            .getSimpleName().startsWith("set");
                }

                @Override
                public String toString() {
                    return "all setters";
                }
            }

            static class Withs extends Methods {
                public Withs() {
                    super(singletonList("public"));
                }

                @Override
                public boolean test(J declaration) {
                    return super.test(declaration) && ((J.MethodDecl) declaration)
                            .getSimpleName().startsWith("with");
                }

                @Override
                public String toString() {
                    return "all withs";
                }
            }

            static class Accessors extends Methods {
                private final Getters getters;
                private final Setters setters;
                private final Withs withs;

                public Accessors() {
                    super(emptyList());
                    this.getters = new Getters();
                    this.setters = new Setters();
                    this.withs = new Withs();
                }

                @Override
                public boolean test(J declaration) {
                    return getters.test(declaration) ||
                            setters.test(declaration) ||
                            withs.test(declaration);
                }

                @Override
                public String toString() {
                    return "all accessors";
                }
            }

            static class Equals extends Methods {
                public Equals() {
                    super(singletonList("public"));
                }

                @Override
                public boolean test(J declaration) {
                    if (!super.test(declaration)) {
                        return false;
                    }

                    J.MethodDecl method = (J.MethodDecl) declaration;

                    if (!method.getSimpleName().equals("equals")) {
                        return false;
                    }

                    return method.getParams().getElem().size() == 1;
                }

                @Override
                List<? extends J> orderedDeclarations() {
                    return super.orderedDeclarations();
                }

                @Override
                public String toString() {
                    return "equals";
                }
            }

            static class ToString extends Methods {
                public ToString() {
                    super(singletonList("public"));
                }

                @Override
                public boolean test(J declaration) {
                    if (!super.test(declaration)) {
                        return false;
                    }

                    J.MethodDecl method = (J.MethodDecl) declaration;
                    return method.getSimpleName().equals("toString") && method.getParams().getElem().isEmpty();
                }

                @Override
                public String toString() {
                    return "toString";
                }
            }

            static class HashCode extends Methods {
                public HashCode() {
                    super(singletonList("public"));
                }

                @Override
                public boolean test(J declaration) {
                    if (!super.test(declaration)) {
                        return false;
                    }

                    J.MethodDecl method = (J.MethodDecl) declaration;
                    return method.getSimpleName().equals("hashCode") && method.getParams().getElem().isEmpty();
                }

                @Override
                public String toString() {
                    return "hashCode";
                }
            }
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private final List<Block<? extends J>> blocks = new ArrayList<>();

            public Builder fields(String... modifiers) {
                blocks.add(new Block.Fields(Arrays.asList(modifiers)));
                return this;
            }

            public Builder allOtherFields() {
                blocks.add(new Block.Fields(emptyList()));
                return this;
            }

            public Builder classes(String... modifiers) {
                blocks.add(new Block.Classes(Arrays.asList(modifiers)));
                return this;
            }

            public Builder allOtherClasses() {
                blocks.add(new Block.Classes(emptyList()));
                return this;
            }

            public Builder methods(String... modifiers) {
                blocks.add(new Block.Methods(Arrays.asList(modifiers)));
                return this;
            }

            public Builder allOtherMethods() {
                blocks.add(new Block.Methods(emptyList()));
                return this;
            }

            public Builder constructors(String... modifiers) {
                blocks.add(new Block.Constructors(Arrays.asList(modifiers)));
                return this;
            }

            public Builder allOtherConstructors() {
                blocks.add(new Block.Constructors(emptyList()));
                return this;
            }

            public Builder allGetters() {
                blocks.add(new Block.Getters());
                return this;
            }

            public Builder allSetters() {
                blocks.add(new Block.Setters());
                return this;
            }

            public Builder allWiths() {
                blocks.add(new Block.Withs());
                return this;
            }

            public Builder allAccessors() {
                blocks.add(new Block.Accessors());
                return this;
            }

            public Builder equals() {
                blocks.add(new Block.Equals());
                return this;
            }

            public Builder hashCoding() {
                blocks.add(new Block.HashCode());
                return this;
            }

            public Builder toStringing() {
                blocks.add(new Block.ToString());
                return this;
            }

            public Builder blankLine() {
                if (!blocks.isEmpty() &&
                        blocks.get(blocks.size() - 1) instanceof Layout.Block.BlankLines) {
                    ((Layout.Block.BlankLines) blocks.get(blocks.size() - 1)).count++;
                } else {
                    blocks.add(new Layout.Block.BlankLines());
                }
                return this;
            }

            public Layout build() {
                return new Layout(blocks);
            }
        }
    }
}

class ModifierComparator implements Comparator<List<String>> {
    static final Comparator<List<String>> BY_STRING = new ModifierComparator();

    static final Comparator<List<J.Modifier>> BY_AST = (mods1, mods2) -> BY_STRING.compare(
            mods1.stream().map(mod -> mod.getClass().getSimpleName().toLowerCase()).collect(toList()),
            mods2.stream().map(mod -> mod.getClass().getSimpleName().toLowerCase()).collect(toList())
    );

    @Override
    public int compare(List<String> mods1, List<String> mods2) {
        boolean mods1Static = mods1.contains("static");
        boolean mods2Static = mods2.contains("static");

        if (mods1Static && !mods2Static) {
            return -1;
        } else if (!mods1Static && mods2Static) {
            return 1;
        }

        boolean mods1Final = mods1.contains("final");
        boolean mods2Final = mods2.contains("final");

        if (mods1Final && !mods2Final) {
            return -1;
        } else if (!mods1Final && mods2Final) {
            return 1;
        }

        int visibility = visibilityRank(mods1) - visibilityRank(mods2);
        if (visibility != 0) {
            return visibility;
        }

        for (int i = 0, mods1Size = mods1.size(); i < mods1Size; i++) {
            if (mods2.size() <= i) {
                return -1;
            }
            int comp = mods1.get(i).compareTo(mods2.get(i));
            if (comp != 0) {
                return comp;
            }
        }

        return 0;
    }

    private int visibilityRank(List<String> modifiers) {
        if (modifiers.contains("public")) {
            return 0;
        } else if (modifiers.contains("protected")) {
            return 1;
        } else if (modifiers.contains("private")) {
            return 3;
        } else {
            // package visibility
            return 2;
        }
    }
}
