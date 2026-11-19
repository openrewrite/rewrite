/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.scala.service;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.service.ScopeService;
import org.openrewrite.java.trait.Scope;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.scala.tree.S;

import java.util.*;

/**
 * Scala's binding rules where they part from Java's. A name in a {@code case} pattern, a {@code for} enumerator or a
 * pattern definition binds a variable. An import in a block binds its name for the rest of the block. A local
 * {@code def} is visible throughout its block.
 */
@Incubating(since = "8.113.0")
public class ScalaScopeService extends ScopeService {
    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            "abstract", "case", "catch", "class", "def", "do", "else", "enum", "export", "extends", "false", "final",
            "finally", "for", "forSome", "given", "if", "implicit", "import", "lazy", "match", "new", "null", "object",
            "override", "package", "private", "protected", "return", "sealed", "super", "then", "this", "throw",
            "trait", "try", "true", "type", "val", "var", "while", "with", "yield"));

    @Override
    public boolean isScope(Cursor cursor) {
        return cursor.getValue() instanceof S.For || super.isScope(cursor);
    }

    @Override
    public List<Scope.Binding> bindings(Cursor scope) {
        List<Scope.Binding> bindings = new ArrayList<>(super.bindings(scope));
        Object tree = scope.getValue();
        if (tree instanceof J.Case) {
            patternVariables(scope, new ArrayList<>(((J.Case) tree).getCaseLabels()), bindings);
        } else if (tree instanceof S.For) {
            patternVariables(scope, new ArrayList<>(((S.For) tree).getEnumerators()), bindings);
        } else if (tree instanceof J.Lambda) {
            // A parameter without a type is a bare name
            Cursor parameters = new Cursor(scope, ((J.Lambda) tree).getParameters());
            for (J parameter : ((J.Lambda) tree).getParameters().getParameters()) {
                if (parameter instanceof J.Identifier) {
                    bindings.add(new Scope.Binding(new Cursor(parameters, parameter), ((J.Identifier) parameter).getSimpleName(),
                            Scope.Namespace.VARIABLE, scope, ((J.Identifier) parameter).getFieldType(), this));
                }
            }
        }
        List<Statement> statements = tree instanceof J.Block ? ((J.Block) tree).getStatements() :
                tree instanceof J.ClassDeclaration ? ((J.ClassDeclaration) tree).getBody().getStatements() :
                tree instanceof S.CompilationUnit ? ((S.CompilationUnit) tree).getStatements() : Collections.emptyList();
        Cursor owner = tree instanceof J.ClassDeclaration ? new Cursor(scope, ((J.ClassDeclaration) tree).getBody()) : scope;
        for (Statement statement : statements) {
            if (statement instanceof S.PatternDefinition) {
                patternVariables(scope, Collections.singletonList(statement), owner, bindings);
            }
        }
        if (tree instanceof J.Block) {
            for (Statement statement : statements) {
                Cursor anchor = new Cursor(scope, statement);
                if (statement instanceof J.Import) {
                    String name = ((J.Import) statement).getQualid().getSimpleName();
                    if (!"_".equals(name) && !"*".equals(name)) {
                        for (Scope.Namespace namespace : Scope.Namespace.values()) {
                            bindings.add(new Scope.Binding(anchor, name, namespace, scope, ((J.Import) statement).getQualid().getType(), this));
                        }
                    }
                }
            }
        }
        return bindings;
    }

    @Override
    public boolean isVisibleAt(Scope.Binding binding, Cursor position) {
        J scopeTree = binding.getScope().getTree();
        J anchor = binding.getTree();
        if (scopeTree instanceof S.For) {
            S.For for_ = (S.For) scopeTree;
            if (under(position, for_.getBody())) {
                return true;
            }
            List<S.For.Enumerator> enumerators = for_.getEnumerators();
            int declared = indexUnder(binding.getCursor(), enumerators);
            int asked = indexUnder(position, enumerators);
            return declared >= 0 && asked > declared;
        }
        if (scopeTree instanceof J.Block && anchor instanceof J.Import) {
            List<Statement> statements = ((J.Block) scopeTree).getStatements();
            return indexUnder(position, statements) > statements.indexOf(anchor);
        }
        if (scopeTree instanceof J.Block && binding.getNamespace() == Scope.Namespace.METHOD) {
            return under(position, scopeTree);
        }
        return super.isVisibleAt(binding, position);
    }

    @Override
    public Scope.@Nullable Namespace namespaceOf(Cursor identifier) {
        Cursor parent = identifier.getParentTreeCursor();
        if (parent.getValue() instanceof J.Assignment &&
            ((J.Assignment) parent.getValue()).getVariable() == identifier.getValue() &&
            parent.getParentTreeCursor().getValue() instanceof J.MethodInvocation) {
            return null;
        }
        if (isPatternVariable(identifier) || parent.getValue() instanceof J.Lambda.Parameters) {
            return null;
        }
        return super.namespaceOf(identifier);
    }

    @Override
    public boolean localMayShadowLocal() {
        return true;
    }

    @Override
    public boolean isReserved(String name) {
        return KEYWORDS.contains(name);
    }

    private void patternVariables(Cursor scope, List<? extends J> patterns, List<Scope.Binding> bindings) {
        patternVariables(scope, patterns, scope, bindings);
    }

    private void patternVariables(Cursor scope, List<? extends J> patterns, Cursor parent, List<Scope.Binding> bindings) {
        for (J pattern : patterns) {
            new JavaIsoVisitor<List<Scope.Binding>>() {
                @Override
                public @Nullable J preVisit(J tree, List<Scope.Binding> found) {
                    // A scope in a definition's or an enumerator's value binds the names of its own patterns
                    if (tree != pattern && isScope(getCursor())) {
                        stopAfterPreVisit();
                    }
                    return tree;
                }

                @Override
                public J.Identifier visitIdentifier(J.Identifier identifier, List<Scope.Binding> found) {
                    if (isPatternVariable(getCursor())) {
                        found.add(new Scope.Binding(getCursor(), identifier.getSimpleName(), Scope.Namespace.VARIABLE,
                                scope, identifier.getFieldType(), ScalaScopeService.this));
                    }
                    return identifier;
                }
            }.visit(pattern, bindings, parent);
        }
    }

    /**
     * Whether the identifier is a name standing alone in a pattern, which Scala reads as a new variable. In a
     * {@code case} pattern, an enumerator's left side or a single-pattern definition only a lowercase name is one, an
     * uppercase name being a constant. In a definition listing several names, such as {@code val A, B = 1}, each is.
     */
    private static boolean isPatternVariable(Cursor identifier) {
        J.Identifier ident = identifier.getValue();
        String name = ident.getSimpleName();
        if (name.isEmpty() || "_".equals(name)) {
            return false;
        }
        boolean lowercase = Character.isLowerCase(name.charAt(0)) || name.charAt(0) == '_';
        Object child = ident;
        for (Cursor c = identifier.getParentTreeCursor(); ; c = c.getParentTreeCursor()) {
            Object parent = c.getValue();
            if (parent instanceof S.TypeAscription) {
                if (((S.TypeAscription) parent).getExpression() != child) {
                    return false;
                }
            } else if (parent instanceof S.Binding) {
                if (((S.Binding) parent).getName() != child && ((S.Binding) parent).getPattern() != child) {
                    return false;
                }
            } else if (parent instanceof J.DeconstructionPattern) {
                if (((J.DeconstructionPattern) parent).getDeconstructor() == child) {
                    return false;
                }
            } else if (parent instanceof J.Case) {
                return lowercase && ((J.Case) parent).getCaseLabels().contains(child);
            } else if (parent instanceof S.For.Enumerator) {
                return lowercase && ((S.For.Enumerator) parent).getLhs() == child;
            } else if (parent instanceof S.PatternDefinition) {
                List<J> patterns = ((S.PatternDefinition) parent).getPatterns();
                return patterns.contains(child) && (lowercase || patterns.size() > 1 && child == ident);
            } else if (!(parent instanceof S.TuplePattern || parent instanceof J.Parentheses)) {
                return false;
            }
            child = parent;
        }
    }

    private static boolean under(Cursor cursor, J tree) {
        for (Cursor c = cursor; c != null; c = c.getParent()) {
            if (c.getValue() instanceof J && ((J) c.getValue()).getId().equals(tree.getId())) {
                return true;
            }
        }
        return false;
    }

    private static int indexUnder(Cursor cursor, List<? extends J> trees) {
        for (int i = 0; i < trees.size(); i++) {
            if (under(cursor, trees.get(i))) {
                return i;
            }
        }
        return -1;
    }
}
