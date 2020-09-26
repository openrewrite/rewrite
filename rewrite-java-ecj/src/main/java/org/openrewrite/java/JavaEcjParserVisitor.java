package org.openrewrite.java;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.ast.*;
import org.eclipse.jdt.internal.compiler.batch.CompilationUnit;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.openrewrite.Formatting;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeTree;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static org.openrewrite.Formatting.EMPTY;
import static org.openrewrite.Formatting.format;
import static org.openrewrite.Tree.randomId;

class JavaEcjParserVisitor {
    private static final Logger logger = LogManager.getLogger(JavaEcjParserVisitor.class);

    private final Visitor visitor = new Visitor();

    private final String source;

    JavaEcjParserVisitor(CompilationUnit source) {
        this.source = new String(source.contents);
    }

    public J.CompilationUnit visit(CompilationUnitDeclaration declaration) {
        return visitor.visit(declaration);
    }

    private class Visitor extends ASTVisitor {
        private final Stack<Scope> scopes = new Stack<>();

        private J tree;
        private int pos = 0;

        @Nullable
        private <T extends J> T visit(ASTNode node) {
            tree = null;

            if (node instanceof CompilationUnitDeclaration) {
                CompilationUnitDeclaration n = (CompilationUnitDeclaration) node;
                scopes.push(n.scope);
                visit(n, n.scope);
                scopes.pop();
            } else if (node instanceof ImportReference) {
                ImportReference n = (ImportReference) node;
                visit(n, (CompilationUnitScope) scopes.peek());
            } else if (node instanceof TypeDeclaration) {
                TypeDeclaration n = (TypeDeclaration) node;
                scopes.push(n.scope);
                visit(n, n.scope);
                scopes.pop();
            }

            //noinspection unchecked
            return (T) tree;
        }

        @Nullable
        private <T extends J> List<T> visitAll(@Nullable ASTNode[] nodes) {
            if (nodes == null) {
                return null;
            }

            //noinspection unchecked
            return Arrays.stream(nodes).map(this::visit)
                    .map(j -> (T) j)
                    .collect(Collectors.toList());
        }

        @Override
        public boolean visit(CompilationUnitDeclaration compilationUnitDeclaration,
                             CompilationUnitScope scope) {
            J.Package mappedPkg = null;

            ImportReference pkg = compilationUnitDeclaration.currentPackage;
            if (pkg != null) {
                mappedPkg = new J.Package(randomId(),
                        identOrFieldAccess(compilationUnitDeclaration.currentPackage.tokens),
                        EMPTY);
            }

            tree = new J.CompilationUnit(
                    randomId(),
                    new String(compilationUnitDeclaration.getFileName()),
                    emptyList(),
                    visit(compilationUnitDeclaration.currentPackage),
                    visitAll(compilationUnitDeclaration.imports),
                    visitAll(compilationUnitDeclaration.types),
                    EMPTY,
                    emptyList()
            );

            return false;
        }

        @Override
        public boolean visit(ImportReference importRef, CompilationUnitScope scope) {
            diagram(importRef);

            String prefix = sourceBefore(importRef.declarationSourceStart);

            tree = new J.Import(randomId(),
                    (J.FieldAccess) identOrFieldAccess(importRef.tokens),
                    importRef.isStatic(),
                    format(prefix));

            pos = importRef.declarationSourceEnd;
            tree = tree.withSuffix(sourceBefore(";"));

            return false;
        }

        @Override
        public boolean visit(TypeDeclaration typeDeclaration, ClassScope scope) {
            String prefix = sourceBefore(typeDeclaration.declarationSourceStart);

            Snip kindSnip = snip(typeDeclaration.restrictedIdentifierStart, typeDeclaration.sourceStart);
            J.ClassDecl.Kind kind;
            switch (kindSnip.source) {
                case "class":
                    kind = new J.ClassDecl.Kind.Class(randomId(), kindSnip.formatting);
                    break;
                case "interface":
                    kind = new J.ClassDecl.Kind.Interface(randomId(), kindSnip.formatting);
                    break;
                case "@interface":
                    kind = new J.ClassDecl.Kind.Annotation(randomId(), kindSnip.formatting);
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected class kind '" + kindSnip.source + "'");
            }
            pos = typeDeclaration.sourceStart;

            diagram(typeDeclaration);

            J.Ident name = J.Ident.build(randomId(), new String(typeDeclaration.name), null,
                    format(kind.getSuffix()));
            skip(name.getSimpleName());

            J.ClassDecl.Implements implementings = null;
            List<TypeTree> superInterfaces = visitAll(typeDeclaration.superInterfaces);
            if (superInterfaces != null) {
                String implementsPrefix = sourceBefore("implements");

                for (int i = 0; i < superInterfaces.size(); i++) {
                    TypeTree si = superInterfaces.get(i);
                    si = si.withPrefix(sourceBefore(si.printTrimmed()));
                    if (i < superInterfaces.size() - 1) {
                        si = si.withSuffix(sourceBefore(","));
                    }
                    superInterfaces.set(i, si);
                }

                implementings = new J.ClassDecl.Implements(randomId(),
                        superInterfaces, format(implementsPrefix));
            }

            tree = new J.ClassDecl(
                    randomId(),
                    visitAll(typeDeclaration.annotations),
                    snip(typeDeclaration.modifiersSourceStart, typeDeclaration.restrictedIdentifierStart)
                            .collectTokens(J.Modifier::buildModifier),
                    kind.withSuffix(""),
                    name,
                    typeDeclaration.typeParameters == null ? null : new J.TypeParameters(randomId(),
                            visitAll(typeDeclaration.typeParameters), EMPTY),
                    null,
                    implementings,
                    null,
                    null,
                    format(prefix)
            );

            pos = typeDeclaration.declarationSourceEnd;

            return false;
        }

        public boolean visit(SingleTypeReference singleTypeReference, BlockScope scope) {
            return false;
        }

        public boolean visit(SingleTypeReference singleTypeReference, ClassScope scope) {
            return false;
        }

        /**
         * A simple debugging utility for understanding where ECJ is putting start/end position indices.
         */
        private void diagram(ASTNode node) {
            AtomicInteger min = new AtomicInteger(Integer.MAX_VALUE);
            AtomicInteger max = new AtomicInteger(Integer.MIN_VALUE);

            Arrays.stream(node.getClass().getDeclaredFields())
                    .filter(f -> f.getName().contains("Start") || f.getName().contains("End"))
                    .sorted(Comparator.comparing(Field::getName))
                    .sorted(Comparator.comparing(f -> {
                        try {
                            return (int) f.get(node);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    }))
                    .forEachOrdered(f -> {
                        try {
                            int pos = (int) f.get(node);
                            min.set(Math.min(min.get(), pos));
                            max.set(Math.max(max.get(), pos));
                            logger.info("{}: {}", f.getName(), pos);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    });

            String nodeSource = source.substring(min.get(), max.get());

            StringBuilder positions = new StringBuilder();
            StringBuilder chars = new StringBuilder();

            int i = min.get();
            for (char c : nodeSource.toCharArray()) {
                chars.append(c);
                if (i >= 10_000) {
                    chars.append("    |");
                } else if (i >= 1_000) {
                    chars.append("   |");
                } else if (i >= 100) {
                    chars.append("  |");
                } else if (i >= 10) {
                    chars.append(" |");
                } else {
                    chars.append("|");
                }
                positions.append(i++).append('|');
            }

            logger.info(positions.toString());
            logger.info(chars.toString());
        }

        private Expression identOrFieldAccess(char[][] names) {
            if (names.length == 0) {
                return null;
            }

            Expression fa = J.Ident.build(randomId(), new String(names[0]), null, EMPTY);
            for (int i = 1; i < names.length; i++) {
                fa = new J.FieldAccess(randomId(), fa,
                        J.Ident.build(randomId(), new String(names[i]), null, EMPTY),
                        null, EMPTY);
            }
            return fa;
        }

        private String sourceBefore(String delimiter) {
            int endIndex = source.indexOf(delimiter, pos);
            String snip = source.substring(pos, endIndex);
            pos = endIndex + 1;
            return snip;
        }

        private void skip(String delimiter) {
            int endIndex = source.indexOf(delimiter, pos);
            pos = endIndex + 1;
        }

        private String sourceBefore(int declarationSourceStart) {
            String snip = source.substring(pos, declarationSourceStart);
            pos = declarationSourceStart;
            return snip;
        }

        private Snip snip(int start, int end) {
            return new Snip(source.substring(start, end));
        }

        private class Snip {
            String source;
            Formatting formatting = Formatting.EMPTY;

            Snip(String source) {
                this.source = source.trim();

                StringBuilder format = new StringBuilder();
                char[] chars = source.toCharArray();
                for (char c : chars) {
                    if (!Character.isWhitespace(c)) {
                        formatting = formatting.withPrefix(format.toString());
                        break;
                    }
                    format.append(c);
                }

                format = new StringBuilder();
                for (int i = chars.length - 1; i >= 0; i--) {
                    char c = chars[i];
                    if (!Character.isWhitespace(c)) {
                        formatting = formatting.withSuffix(format.reverse().toString());
                        break;
                    }
                    format.append(c);
                }
            }

            <T> T map(BiFunction<String, Formatting, T> map) {
                return map.apply(source, formatting);
            }

            Stream<Snip> tokenStream() {
                return Arrays.stream(source.split("((?<=\\s)|(?=\\s+))")).map(Snip::new);
            }

            <T> List<T> collectTokens(BiFunction<String, Formatting, T> map) {
                return tokenStream()
                        .map(snip -> map.apply(snip.source, snip.formatting))
                        .collect(Collectors.toList());
            }
        }
    }
}
