package org.openrewrite.java;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.search.SemanticallyEqual;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.marker.Markers;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.emptyList;
import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = true)
public class InstanceOfPatternMatch extends Recipe {

    @Override
    public String getDisplayName() {
        return "Changes code to use Java 17's `instanceof` pattern matching";
    }

    @Override
    public String getDescription() {
        return "Changes `if` conditions of the form `if (o instanceof Foo && ((Foo) x).whatever())` to `if ((o instanceof Foo foo).whatever())`.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(1);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesJavaVersion<>(17);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Nullable
            InstanceOfPatternMatch.UseInstanceOfPatternMatching replacementVisitor;

            @Override
            public J.InstanceOf visitInstanceOf(J.InstanceOf instanceOf, ExecutionContext executionContext) {
                instanceOf = super.visitInstanceOf(instanceOf, executionContext);
                if (instanceOf.getPattern() != null) {
                    return instanceOf;
                }

                Cursor maybeReplacementRoot = null, maybeReplacementScope = null;
                for (Iterator<Cursor> it = getCursor().getPathAsCursors(); it.hasNext(); ) {
                    Cursor next = it.next();
                    Object value = next.getValue();
                    if (value instanceof J.Binary) {
                        J.Binary binary = (J.Binary) value;
                        if (binary.getOperator() == J.Binary.Type.And) {
                            maybeReplacementScope = next;
                        } else {
                            break;
                        }
                    } else if (value instanceof Statement) {
                        maybeReplacementRoot = next;
                        break;
                    }
                }

                if (maybeReplacementScope != null && maybeReplacementRoot != null) {
                    J root = maybeReplacementRoot.getValue();
                    Set<J> contexts = new HashSet<>();
                    if (root instanceof J.If) {
                        contexts.add(((J.If) root).getThenPart());
                    }
                    contexts.add(maybeReplacementScope.getValue());

                    InstanceOfPatternReplacements replacements = maybeReplacementRoot
                            .computeMessageIfAbsent("flowTypeScope", k -> new InstanceOfPatternReplacements(root));
                    replacements.register(instanceOf, contexts);

                    if (replacementVisitor == null) {
                        replacementVisitor = new UseInstanceOfPatternMatching();
                        doAfterVisit(replacementVisitor);
                    }
                    replacementVisitor.register(replacements);
                }
                return instanceOf;
            }

            @Override
            public J.TypeCast visitTypeCast(J.TypeCast typeCast, ExecutionContext executionContext) {
                typeCast = super.visitTypeCast(typeCast, executionContext);
                InstanceOfPatternReplacements replacements = getCursor().getNearestMessage("flowTypeScope");
                if (replacements != null) {
                    replacements.register(typeCast, getCursor());
                }
                return typeCast;
            }
        };
    }

    @Data
    private static class InstanceOfPatternReplacements {
        private final J root;
        private final Map<Expression, J.InstanceOf> instanceOfs = new HashMap<>();
        private final Map<J.InstanceOf, Set<J>> contexts = new HashMap<>();
        private final Map<J.TypeCast, J.InstanceOf> replacements = new HashMap<>();

        public void register(J.InstanceOf instanceOf, Set<J> contexts) {
            Expression expression = instanceOf.getExpression();
            Optional<Expression> existing = instanceOfs.keySet().stream().filter(e -> SemanticallyEqual.areEqual(e, expression)).findAny();
            if (!existing.isPresent()) {
                instanceOfs.put(expression, instanceOf);
                this.contexts.put(instanceOf, contexts);
            }
        }

        public void register(J.TypeCast typeCast, Cursor cursor) {
            Expression expression = typeCast.getExpression();
            Optional<Expression> match = instanceOfs.keySet().stream().filter(e -> SemanticallyEqual.areEqual(e, expression)).findAny();
            if (match.isPresent()) {
                J.InstanceOf instanceOf = instanceOfs.get(match.get());
                Set<J> validContexts = contexts.get(instanceOf);
                for (Iterator<?> it = cursor.getPath(); it.hasNext(); ) {
                    if (validContexts.contains(it.next())) {
                        replacements.put(typeCast, instanceOf);
                        break;
                    }
                }
            }
        }

        public boolean isEmpty() {
            return replacements.isEmpty();
        }

        public J.InstanceOf process(J.InstanceOf instanceOf) {
            // FIXME variable naming
            String name = "s";
            return instanceOf.withPattern(new J.Identifier(
                    randomId(),
                    Space.build(" ", emptyList()),
                    Markers.EMPTY,
                    name,
                    instanceOf.getType(),
                    null));
        }

        public J process(J.TypeCast typeCast, Cursor cursor) {
            if (replacements.containsKey(typeCast)) {
                // FIXME variable naming
                String name = "s";
                // FIXME what should owner be?
                JavaType owner = cursor.firstEnclosingOrThrow(J.MethodDeclaration.class).getType();
                JavaType.Variable fieldType = new JavaType.Variable(null, Flag.Default.getBitMask(), name, owner, typeCast.getType(), emptyList());
                return new J.Identifier(
                        randomId(),
                        typeCast.getPrefix(),
                        Markers.EMPTY,
                        name,
                        typeCast.getType(),
                        fieldType);
            }
            return typeCast;
        }
    }

    private static class UseInstanceOfPatternMatching extends JavaVisitor<ExecutionContext> {
        private final Set<InstanceOfPatternReplacements> replacements = new HashSet<>();
        private final Map<J, InstanceOfPatternReplacements> replacementsByContext = new HashMap<>();

        public void register(InstanceOfPatternReplacements replacements) {
            this.replacements.add(replacements);
            replacements.getContexts().values().forEach(c -> {
                c.forEach(j -> replacementsByContext.put(j, replacements));
            });
        }

        @Override
        public boolean isAcceptable(SourceFile sourceFile, ExecutionContext executionContext) {
            return super.isAcceptable(sourceFile, executionContext) && !replacements.isEmpty()
                    && replacements.stream().anyMatch(r -> !r.isEmpty());
        }

        @Override
        public @Nullable J preVisit(J tree, ExecutionContext executionContext) {
            InstanceOfPatternReplacements applicableReplacements = replacementsByContext.get(tree);
            if (applicableReplacements != null) {
                getCursor().putMessage("flowTypeScope", applicableReplacements);
            }
            return super.preVisit(tree, executionContext);
        }

        @Override
        public J.InstanceOf visitInstanceOf(J.InstanceOf instanceOf, ExecutionContext executionContext) {
            instanceOf = (J.InstanceOf) super.visitInstanceOf(instanceOf, executionContext);
            InstanceOfPatternReplacements applicableReplacements = getCursor().getNearestMessage("flowTypeScope");
            if (applicableReplacements != null) {
                instanceOf = applicableReplacements.process(instanceOf);
            }
            return instanceOf;
        }

        @Override
        public <T extends J> J visitParentheses(J.Parentheses<T> parens, ExecutionContext executionContext) {
            InstanceOfPatternReplacements applicableReplacements = getCursor().getNearestMessage("flowTypeScope");
            if (applicableReplacements != null && parens.getSideEffects().isEmpty() && parens.getTree() instanceof J.TypeCast) {
                return applicableReplacements.process((J.TypeCast) parens.getTree(), getCursor());
            }
            return super.visitParentheses(parens, executionContext);
        }

        @Override
        public J visitTypeCast(J.TypeCast typeCast, ExecutionContext executionContext) {
            typeCast = (J.TypeCast) super.visitTypeCast(typeCast, executionContext);
            InstanceOfPatternReplacements applicableReplacements = getCursor().getNearestMessage("flowTypeScope");
            if (applicableReplacements != null) {
                return applicableReplacements.process(typeCast, getCursor());
            }
            return typeCast;
        }
    }
}
