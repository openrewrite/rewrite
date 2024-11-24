/*
 *  Copyright 2021 the original author or authors.
 *  <p>
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  <p>
 *  https://www.apache.org/licenses/LICENSE-2.0
 *  <p>
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.openrewrite.yaml;

import lombok.EqualsAndHashCode;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.yaml.internal.grammar.JsonPathLexer;
import org.openrewrite.yaml.internal.grammar.JsonPathParser;
import org.openrewrite.yaml.internal.grammar.JsonPathParserBaseVisitor;
import org.openrewrite.yaml.tree.Yaml;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * Provides methods for matching the given cursor location to a specified JsonPath expression.
 * <p>
 * This is not a full implementation of the JsonPath syntax as linked in the "see also."
 *
 * @see <a href="https://support.smartbear.com/alertsite/docs/monitors/api/endpoint/jsonpath.html">https://support.smartbear.com/alertsite/docs/monitors/api/endpoint/jsonpath.html</a>
 */
@EqualsAndHashCode
public class JsonPathMatcher {

    private final String jsonPath;
    private JsonPathParser.@Nullable JsonPathContext parsed;

    public JsonPathMatcher(String jsonPath) {
        this.jsonPath = jsonPath;
    }

    /**
     * Find all leaf nodes that match the JSONPath expression starting from the given cursor.
     * For root expressions ($), returns empty list if cursor is not at document root.
     *
     * @param startCursor The cursor position to start matching from
     * @return List of matching leaf nodes
     */
    public @Nullable List<Object> findMatchingLeaves(Cursor startCursor) {
        // For root expressions, verify we're at document root
        if (jsonPath.startsWith("$") && !isAtDocumentRoot(startCursor)) {
            return Collections.emptyList();
        }

        JsonPathParser.JsonPathContext ctx = parse();
        // The stop may be optimized by interpreting the ExpressionContext and pre-determining the last visit.
        JsonPathParser.ExpressionContext stop = (JsonPathParser.ExpressionContext) ctx.children.get(ctx.children.size() - 1);
        JsonPathYamlVisitor visitor = new JsonPathYamlVisitor(startCursor, startCursor.getValue(), stop);
        Object result = visitor.visit(ctx);
        if (result instanceof List) {
            //noinspection unchecked
            return (List<Object>) result;
        } else //noinspection ConstantValue
            if (result != null) {
                return singletonList(result);
            }
        return jsonPath.startsWith("$") ? emptyList() : null;
    }

    private boolean isAtDocumentRoot(Cursor cursor) {
        return cursor.getValue() instanceof Yaml.Document;
    }

    public boolean matches(Cursor cursor) {
        Optional<List<Object>> matches = find(cursor);
        return matches.isPresent() && matches.get().contains(cursor.<Yaml>getValue());
    }

    public Optional<List<Object>> find(Cursor cursor) {
        List<Object> matches = jsonPath.startsWith("$") ? cursor.getNearestMessage("jsonpath:" + jsonPath) :
                cursor.getMessage("jsonpath:" + jsonPath);
        if (matches == null) {
            if (jsonPath.startsWith("$") && !isAtDocumentRoot(cursor)) {
                return find(cursor.getParentOrThrow());
            }
            Cursor parent = resolvedAncestors(cursor);
            while (matches == null && parent.getParent() != null) {
                if (parent.getValue() instanceof Tree) {
                    matches = findMatchingLeaves(parent);
                }
                parent = parent.getParent();
            }
            if (matches != null) {
                parent.putMessage("jsonpath:" + jsonPath, matches);
            }
        }
        return Optional.ofNullable(matches);
    }

    private static Cursor resolvedAncestors(Cursor cursor) {
        ArrayDeque<Tree> deque = new ArrayDeque<>();
        Map<Tree, Tree> resolved = new IdentityHashMap<>();
        Cursor newCursor = null;
        for (; cursor.getParent() != null; cursor = cursor.getParent()) {
            if (!(cursor.getValue() instanceof Tree)) {
                continue;
            }
            Tree tree = cursor.getValue();
            if (tree instanceof Yaml.Document) {
                tree = new ReplaceAliasWithAnchorValueVisitor<Integer>() {
                    @Override
                    public @Nullable Yaml visit(@Nullable Tree tree, Integer p) {
                        // NOTE: not calling `super.visit()` for performance reasons
                        if (tree instanceof Yaml) {
                            Yaml updated = ((Yaml) tree).acceptYaml(this, p);
                            if (updated != tree) {
                                resolved.put(tree, updated);
                            }
                            return updated;
                        }
                        return (Yaml) tree;
                    }
                }.visitNonNull(tree, 0);
                deque.addFirst(tree);
                newCursor = cursor.getParent();
                break;
            }
            deque.addFirst(tree);
        }
        for (Tree tree : deque) {
            newCursor = new Cursor(newCursor, resolved.getOrDefault(tree, tree));
        }
        return newCursor;
    }

    private JsonPathParser.JsonPathContext parse() {
        if (parsed == null) {
            parsed = jsonPath().jsonPath();
        }
        return parsed;
    }

    private JsonPathParser jsonPath() {
        return new JsonPathParser(new CommonTokenStream(new JsonPathLexer(CharStreams.fromString(this.jsonPath))));
    }

    @SuppressWarnings({"ConstantConditions", "unchecked"})
    private static class JsonPathYamlVisitor extends JsonPathParserBaseVisitor<Object> {

        private final Cursor cursor;
        protected Object scope;
        private final JsonPathParser.ExpressionContext stop;

        public JsonPathYamlVisitor(Cursor cursor, Object scope, JsonPathParser.ExpressionContext stop) {
            this.cursor = cursor;
            this.scope = scope;
            this.stop = stop;
        }

        @Override
        protected Object defaultResult() {
            return scope;
        }

        @Override
        protected Object aggregateResult(Object aggregate, Object nextResult) {
            return (scope = nextResult);
        }

        @Override
        protected boolean shouldVisitNextChild(RuleNode node, Object currentResult) {
            return scope != null;
        }

        @Override
        public Object visitJsonPath(JsonPathParser.JsonPathContext ctx) {
            MATCH:
            if (ctx.ROOT() != null || "[".equals(ctx.start.getText())) {
                Cursor c = cursor;
                while (c.getParent() != null) {
                    if (c.getValue() instanceof Yaml.Document) {
                        scope = ((Yaml.Document) c.getValue()).getBlock();
                        break MATCH;
                    }
                    c = c.getParent();
                }
                for (c = cursor; c.getParent() != null; c = c.getParent()) {
                    Object tree = c.getValue();
                    if (tree instanceof Yaml.Mapping) {
                        scope = tree;
                        break MATCH;
                    }
                }
                scope = null;
            }
            return super.visitJsonPath(ctx);
        }

        private JsonPathParser.ExpressionContext getExpressionContext(ParserRuleContext ctx) {
            if (ctx == null || ctx instanceof JsonPathParser.ExpressionContext) {
                return (JsonPathParser.ExpressionContext) ctx;
            }
            return getExpressionContext(ctx.getParent());
        }

        @Override
        public @Nullable Object visitRecursiveDecent(JsonPathParser.RecursiveDecentContext ctx) {
            if (scope == null) {
                return null;
            }

            List<Object> results = new ArrayList<>();

            // For all nested nodes under the current scope, try to match the pattern
            collectRecursively(scope, ctx.getChild(1), results);

            return getResultFromList(results);
        }

        private void collectRecursively(Object node, ParseTree matchCtx, List<Object> results) {
            // Try to match at the current node
            Object originalScope = scope;
            scope = node;
            Object result = visit(matchCtx);
            if (result != null) {
                if (result instanceof List) {
                    if (!((List<?>) result).isEmpty()) {
                        results.addAll((List<?>) result);
                    }
                } else {
                    results.add(result);
                }
            }
            scope = originalScope;

            // Recursively check all child nodes
            if (node instanceof Yaml.Mapping) {
                Yaml.Mapping obj = (Yaml.Mapping) node;
                for (Yaml.Mapping.Entry entry : obj.getEntries()) {
                    collectRecursively(entry.getValue(), matchCtx, results);
                }
            } else if (node instanceof Yaml.Sequence) {
                Yaml.Sequence sequence = (Yaml.Sequence) node;
                for (Yaml.Sequence.Entry entry : sequence.getEntries()) {
                    collectRecursively(entry.getBlock(), matchCtx, results);
                }
            } else if (node instanceof Yaml.Scalar) {
                collectRecursively(((Yaml.Scalar) node).getValue(), matchCtx, results);
            } else if (node instanceof Yaml.Sequence.Entry) {
                collectRecursively(((Yaml.Sequence.Entry) node).getBlock(), matchCtx, results);
            } else if (node instanceof Yaml.Mapping.Entry) {
                collectRecursively(((Yaml.Mapping.Entry) node).getValue(), matchCtx, results);
            }

            scope = originalScope;
        }

        @Override
        public @Nullable Object visitBracketOperator(JsonPathParser.BracketOperatorContext ctx) {
            if (!ctx.property().isEmpty()) {
                if (ctx.property().size() == 1) {
                    return visitProperty(ctx.property(0));
                }

                // Return a list if more than 1 property is specified.
                List<Object> list = new ArrayList<>();
                for (JsonPathParser.PropertyContext propertyContext : ctx.property()) {
                    list.add(visitProperty(propertyContext));
                }
                return list;
            } else if (ctx.slice() != null) {
                return visitSlice(ctx.slice());
            } else if (ctx.indexes() != null) {
                return visitIndexes(ctx.indexes());
            } else if (ctx.filter() != null) {
                return visitFilter(ctx.filter());
            }

            return null;
        }

        @Override
        public Object visitSlice(JsonPathParser.SliceContext ctx) {
            List<Yaml> results;
            if (scope instanceof List) {
                //noinspection unchecked
                results = (List<Yaml>) scope;
            } else if (scope instanceof Yaml.Sequence) {
                Yaml.Sequence array = (Yaml.Sequence) scope;
                results = new ArrayList<>(array.getEntries());
            } else if (scope instanceof Yaml.Mapping.Entry) {
                scope = ((Yaml.Mapping.Entry) scope).getValue();
                return visitSlice(ctx);
            } else {
                results = new ArrayList<>();
            }

            // A wildcard will use these initial values, so it is not checked in the conditions.
            int start = 0;
            int limit = results.size();

            if (ctx.PositiveNumber() != null) {
                // [:n], Selects the first n elements of the array.
                limit = Integer.parseInt(ctx.PositiveNumber().getText());
            } else if (ctx.NegativeNumber() != null) {
                // [-n:], Selects the last n elements of the array.
                start = results.size() + Integer.parseInt(ctx.NegativeNumber().getText());
            } else if (ctx.start() != null) {
                // [start:end] or [start:]
                // Selects array elements from the start index and up to, but not including, end index.
                // If end is omitted, selects all elements from start until the end of the array.
                start = ctx.start() != null ? Integer.parseInt(ctx.start().getText()) : 0;
                limit = ctx.end() != null ? Integer.parseInt(ctx.end().getText()) + 1 : limit;
            }

            return results.subList(start, Math.min(start + limit, results.size()));
        }

        @Override
        public Object visitIndexes(JsonPathParser.IndexesContext ctx) {
            List<Object> results;
            if (scope instanceof List) {
                //noinspection unchecked
                results = (List<Object>) scope;
            } else if (scope instanceof Yaml.Sequence) {
                Yaml.Sequence array = (Yaml.Sequence) scope;
                results = new ArrayList<>(array.getEntries());
            } else if (scope instanceof Yaml.Mapping.Entry) {
                scope = ((Yaml.Mapping.Entry) scope).getValue();
                return visitIndexes(ctx);
            } else {
                results = new ArrayList<>();
            }

            List<Object> indexes = new ArrayList<>();
            for (TerminalNode terminalNode : ctx.PositiveNumber()) {
                for (int i = 0; i < results.size(); i++) {
                    if (terminalNode.getText().contains(String.valueOf(i))) {
                        indexes.add(results.get(i));
                    }
                }
            }

            return getResultFromList(indexes);
        }

        @Override
        public @Nullable Object visitProperty(JsonPathParser.PropertyContext ctx) {
            if (scope instanceof Yaml.Mapping) {
                Yaml.Mapping mapping = (Yaml.Mapping) scope;
                for (Yaml.Mapping.Entry entry : mapping.getEntries()) {
                    if (entry instanceof Yaml.Mapping.Entry) {
                        String key = entry.getKey().getValue();
                        String name = ctx.StringLiteral() != null ?
                                unquoteStringLiteral(ctx.StringLiteral().getText()) : ctx.Identifier().getText();
                        if (key.equals(name)) {
                            return entry;
                        }
                    }
                }
            } else if (scope instanceof Yaml.Mapping.Entry) {
                Yaml.Mapping.Entry member = (Yaml.Mapping.Entry) scope;

                String key = member.getKey().getValue();
                String name = ctx.StringLiteral() != null ?
                        unquoteStringLiteral(ctx.StringLiteral().getText()) : ctx.Identifier().getText();
                if (((member.getValue() instanceof Yaml.Scalar))) {
                    return key.equals(name) ? member : null;
                }

                scope = member.getValue();
                return visitProperty(ctx);
            } else if (scope instanceof Yaml.Sequence) {
                List<Object> matches = new ArrayList<>();
                for (Yaml.Sequence.Entry entry : ((Yaml.Sequence) scope).getEntries()) {
                    scope = entry;
                    Object result = visitProperty(ctx);
                    if (result != null) {
                        matches.add(result);
                    }
                }
                return getResultFromList(matches);
            } else if (scope instanceof Yaml.Sequence.Entry) {
                Yaml.Sequence.Entry entry = (Yaml.Sequence.Entry) scope;
                scope = entry.getBlock();
                return visitProperty(ctx);
            } else if (scope instanceof List) {
                List<Object> matches = new ArrayList<>();
                for (Object object : ((List<Object>) scope)) {
                    scope = object;
                    Object result = visitProperty(ctx);
                    if (result instanceof List) {
                        matches.addAll(((List<Object>) result));
                    } else if (result != null) {
                        matches.add(result);
                    }
                }
                return getResultFromList(matches);
            }

            return null;
        }

        @Override
        public @Nullable Object visitWildcard(JsonPathParser.WildcardContext ctx) {
            if (scope instanceof Yaml.Mapping) {
                Yaml.Mapping mapping = (Yaml.Mapping) scope;
                return mapping.getEntries();
            } else if (scope instanceof Yaml.Mapping.Entry) {
                Yaml.Mapping.Entry member = (Yaml.Mapping.Entry) scope;
                return member.getValue();
            } else if (scope instanceof Yaml.Sequence) {
                List<Object> matches = new ArrayList<>();
                for (Yaml.Sequence.Entry entry : ((Yaml.Sequence) scope).getEntries()) {
                    scope = entry;
                    Object result = visitWildcard(ctx);
                    if (result != null) {
                        matches.add(result);
                    }
                }
                return getResultFromList(matches);
            } else if (scope instanceof Yaml.Sequence.Entry) {
                Yaml.Sequence.Entry entry = (Yaml.Sequence.Entry) scope;
                return entry.getBlock();
            } else if (scope instanceof List) {
                List<Object> results = new ArrayList<>();
                for (Object object : ((List<Object>) scope)) {
                    scope = object;
                    Object result = visitWildcard(ctx);
                    if (result != null) {
                        results.add(result);
                    }
                }

                List<Object> matches = new ArrayList<>();
                if (stop != null && stop == getExpressionContext(ctx)) {
                    // Return the values of each result when the JsonPath ends with a wildcard.
                    results.forEach(o -> matches.add(getValue(o)));
                } else {
                    // Unwrap lists of results from visitProperty to match the position of the cursor.
                    for (Object result : results) {
                        if (result instanceof List) {
                            matches.addAll(((List<Object>) result));
                        } else if (result != null) {
                            matches.add(result);
                        }
                    }
                }

                return getResultFromList(matches);
            }

            return null;
        }

        @Override
        public Object visitLiteralExpression(JsonPathParser.LiteralExpressionContext ctx) {
            String s = null;
            if (ctx.StringLiteral() != null) {
                s = ctx.StringLiteral().getText();
            } else if (!ctx.children.isEmpty()) {
                s = ctx.children.get(0).getText();
            }
            if (s != null && (s.startsWith("'") || s.startsWith("\""))) {
                return s.substring(1, s.length() - 1);
            }
            return "null".equals(s) ? null : s;
        }

        @Override
        public @Nullable Object visitUnaryExpression(JsonPathParser.UnaryExpressionContext ctx) {
            if (ctx.AT() != null) {
                if (scope instanceof Yaml.Scalar) {
                    if (ctx.Identifier() == null && ctx.StringLiteral() == null) {
                        return scope;
                    }
                } else if (scope instanceof Yaml.Mapping) {
                    Yaml.Mapping mapping = (Yaml.Mapping) scope;
                    for (Yaml.Mapping.Entry entry : mapping.getEntries()) {
                        scope = entry;
                        if (visitUnaryExpression(ctx) != null) {
                            return entry;
                        }
                    }
                } else if (scope instanceof Yaml.Mapping.Entry) {
                    Yaml.Mapping.Entry entry = (Yaml.Mapping.Entry) scope;
                    if (ctx.Identifier() != null || ctx.StringLiteral() != null) {
                        String key = entry.getKey().getValue();
                        String name = ctx.StringLiteral() != null ?
                                unquoteStringLiteral(ctx.StringLiteral().getText()) : ctx.Identifier().getText();
                        if (key.equals(name)) {
                            return entry;
                        }
                    }
                    scope = entry.getValue();
                    return getResultFromList(visitUnaryExpression(ctx));
                } else if (scope instanceof Yaml.Sequence) {
                    scope = ((Yaml.Sequence) scope).getEntries();
                    return visitUnaryExpression(ctx);
                } else if (scope instanceof Yaml.Sequence.Entry) {
                    // Unary operators set the scope of the matched key within a block.
                    Yaml.Sequence.Entry entry = (Yaml.Sequence.Entry) scope;
                    scope = entry.getBlock();
                    Object result = visitUnaryExpression(ctx);
                    if (result != null) {
                        return getResultFromList(entry.getBlock());
                    }
                } else if (scope instanceof List) {
                    List<Object> matches = new ArrayList<>();
                    for (Object object : ((List<Object>) scope)) {
                        scope = object;
                        Object result = visitUnaryExpression(ctx);
                        if (result instanceof List) {
                            // Unwrap lists of results from visitUnaryExpression to match the position of the cursor.
                            matches.addAll(((List<Object>) result));
                        } else if (result != null) {
                            matches.add(result);
                        }
                    }

                    return getResultFromList(matches);
                }
            } else if (ctx.jsonPath() != null) {
                Object result = visit(ctx.jsonPath());
                return getResultByKey(result, ctx.stop.getText());
            }
            return null;
        }

        @Override
        public @Nullable Object visitRegexExpression(JsonPathParser.RegexExpressionContext ctx) {
            if (scope == null || scope instanceof List && ((List<Object>) scope).isEmpty()) {
                return null;
            }

            Object rhs = ctx.REGEX().getText();
            Object lhs = visitUnaryExpression(ctx.unaryExpression());
            String operator = "=~";

            if (lhs instanceof List) {
                List<Object> matches = new ArrayList<>();
                for (Object match : ((List<Object>) lhs)) {
                    Yaml mappingOrEntry = getOperatorResult(match, operator, rhs);
                    if (mappingOrEntry != null) {
                        matches.add(mappingOrEntry);
                    }
                }
                return getResultFromList(matches);
            } else {
                return getOperatorResult(lhs, operator, rhs);
            }
        }

        // Checks if a string contains the specified substring (case-sensitive), or an array contains the specified element.
        @Override
        public @Nullable Object visitContainsExpression(JsonPathParser.ContainsExpressionContext ctx) {
            Object originalScope = scope;
            if (ctx.children.get(0) instanceof JsonPathParser.UnaryExpressionContext) {
                Object lhs = visitUnaryExpression(ctx.unaryExpression());
                Object rhs = visitLiteralExpression(ctx.literalExpression());
                if (lhs instanceof List) {
                    String key = ctx.children.get(0).getChild(2).getText();
                    lhs = getResultByKey(lhs, key);
                }

                if (lhs instanceof Yaml.Mapping.Entry && rhs != null) {
                    Yaml.Mapping.Entry entry = (Yaml.Mapping.Entry) lhs;
                    if (entry.getValue() instanceof Yaml.Sequence) {
                        Yaml.Sequence sequence = (Yaml.Sequence) entry.getValue();
                        for (Yaml.Sequence.Entry o : sequence.getEntries()) {
                            if (o.getBlock() instanceof Yaml.Scalar) {
                                Yaml.Scalar block = (Yaml.Scalar) o.getBlock();
                                if (block.getValue().contains(String.valueOf(rhs))) {
                                    return originalScope;
                                }
                            }
                        }
                    } else if (entry.getValue() instanceof Yaml.Scalar) {
                        Yaml.Scalar scalar = (Yaml.Scalar) entry.getValue();
                        if (scalar.getValue().contains(String.valueOf(rhs))) {
                            return originalScope;
                        }
                    }
                }
            } else {
                Object lhs = visitLiteralExpression(ctx.literalExpression());
                Object rhs = visitUnaryExpression(ctx.unaryExpression());
                if (rhs instanceof List) {
                    String key = ctx.children.get(2).getChild(2).getText();
                    rhs = getResultByKey(rhs, key);
                }

                if (rhs instanceof Yaml.Mapping.Entry && lhs != null) {
                    Yaml.Mapping.Entry entry = (Yaml.Mapping.Entry) rhs;
                    if (entry.getValue() instanceof Yaml.Scalar) {
                        Yaml.Scalar scalar = (Yaml.Scalar) entry.getValue();
                        if (scalar.getValue().contains(String.valueOf(lhs))) {
                            return originalScope;
                        }
                    }
                }
            }

            return null;
        }

        @Override
        public Object visitFilterExpression(JsonPathParser.FilterExpressionContext ctx) {
            Object originalScope = scope;

            List<Object> result = new ArrayList<>();
            if (originalScope instanceof List) {
                for (Object o : ((List<?>) originalScope)) {
                    scope = o;
                    if (super.visitFilterExpression(ctx) != null) {
                        result.add(o);
                    }
                }
            } else if (originalScope instanceof Yaml.Mapping.Entry && ((Yaml.Mapping.Entry) originalScope).getValue() instanceof Yaml.Sequence) {
                Yaml.Sequence sequence = (Yaml.Sequence) ((Yaml.Mapping.Entry) originalScope).getValue();
                for (Yaml.Sequence.Entry entry : sequence.getEntries()) {
                    scope = entry.getBlock();
                    if (super.visitFilterExpression(ctx) != null) {
                        result.add(entry.getBlock());
                    }
                }
            } else if (originalScope instanceof Yaml.Sequence.Entry && ((Yaml.Sequence.Entry) originalScope).getBlock() instanceof Yaml.Sequence) {
                Yaml.Sequence array = (Yaml.Sequence) ((Yaml.Sequence.Entry) originalScope).getBlock();
                for (Yaml.Sequence.Entry entry : array.getEntries()) {
                    scope = entry.getBlock();
                    if (super.visitFilterExpression(ctx) != null) {
                        result.add(entry.getBlock());
                    }
                }
            } else if (originalScope instanceof Yaml.Sequence) {
                Yaml.Sequence sequence = (Yaml.Sequence) originalScope;
                for (Yaml.Sequence.Entry value : sequence.getEntries()) {
                    scope = value.getBlock();
                    if (super.visitFilterExpression(ctx) != null) {
                        result.add(value.getBlock());
                    }
                }
            } else if (originalScope instanceof Yaml.Mapping) {
                Yaml.Mapping mapping = (Yaml.Mapping) originalScope;
                for (Yaml.Mapping.Entry entry : mapping.getEntries()) {
                    scope = entry;
                    if (super.visitFilterExpression(ctx) != null) {
                        result.add(entry);
                    }
                }
            } else {
                Object o = super.visitFilterExpression(ctx);
                if (o != null) {
                    result.add(originalScope);
                }
            }
            return getResultFromList(result);
        }

        @Override
        public @Nullable Object visitBinaryExpression(JsonPathParser.BinaryExpressionContext ctx) {
            Object lhs = ctx.children.get(0);
            Object rhs = ctx.children.get(2);

            if (ctx.LOGICAL_OPERATOR() != null) {
                Object originalScope = scope;
                Object lhsResult = getBinaryExpressionResult(ctx.children.get(0));
                scope = originalScope;
                Object rhsResult = getBinaryExpressionResult(ctx.children.get(2));
                scope = originalScope;

                if ("&&".equals(ctx.LOGICAL_OPERATOR().getText())) {
                    if (lhsResult != null && rhsResult != null) {
                        return scope;
                    }
                } else if ("||".equals(ctx.LOGICAL_OPERATOR().getText())) {
                    if (lhsResult != null || rhsResult != null) {
                        return scope;
                    }
                }

                return null;
            } else if (ctx.EQUALITY_OPERATOR() != null) {
                // Equality operators may resolve the LHS and RHS without caching scope.
                Object originalScope = scope;
                lhs = getBinaryExpressionResult(lhs);
                rhs = getBinaryExpressionResult(rhs);
                String operator;
                switch (ctx.EQUALITY_OPERATOR().getText()) {
                    case ("=="):
                        operator = "==";
                        break;
                    case ("!="):
                        operator = "!=";
                        break;
                    default:
                        return null;
                }

                if (lhs instanceof List) {
                    List<Object> matches = new ArrayList<>();
                    for (Object match : ((List<Object>) lhs)) {
                        Yaml result = getOperatorResult(match, operator, rhs);
                        if (result != null) {
                            matches.add(result);
                        }
                    }
                    // The filterExpression matched a result inside a Mapping. I.E. originalScope[?(filterExpression)]
                    if (originalScope instanceof Yaml.Mapping.Entry && ((Yaml.Mapping.Entry) originalScope).getValue() instanceof Yaml.Mapping) {
                        return originalScope;
                    }
                    return getResultFromList(matches);
                } else {
                    if (originalScope instanceof Yaml.Mapping.Entry) {
                        if (getOperatorResult(lhs, operator, rhs) != null) {
                            return originalScope;
                        }
                    } else {
                        return getOperatorResult(lhs, operator, rhs);
                    }
                }
            }
            return null;
        }

        private @Nullable Object getBinaryExpressionResult(Object ctx) {
            if (ctx instanceof JsonPathParser.BinaryExpressionContext) {
                ctx = visitBinaryExpression((JsonPathParser.BinaryExpressionContext) ctx);

            } else if (ctx instanceof JsonPathParser.RegexExpressionContext) {
                ctx = visitRegexExpression((JsonPathParser.RegexExpressionContext) ctx);

            } else if (ctx instanceof JsonPathParser.ContainsExpressionContext) {
                ctx = visitContainsExpression((JsonPathParser.ContainsExpressionContext) ctx);

            } else if (ctx instanceof JsonPathParser.UnaryExpressionContext) {
                ctx = visitUnaryExpression((JsonPathParser.UnaryExpressionContext) ctx);

            } else if (ctx instanceof JsonPathParser.LiteralExpressionContext) {
                ctx = visitLiteralExpression((JsonPathParser.LiteralExpressionContext) ctx);
            }
            return ctx;
        }

        // Interpret the LHS to check the appropriate value.
        private @Nullable Yaml getOperatorResult(Object lhs, String operator, Object rhs) {
            if (lhs instanceof Yaml.Mapping) {
                Yaml.Mapping mapping = (Yaml.Mapping) lhs;
                for (Yaml.Mapping.Entry entry : mapping.getEntries()) {
                    if (entry.getValue() instanceof Yaml.Scalar &&
                        checkObjectEquality(((Yaml.Scalar) entry.getValue()).getValue(), operator, rhs)) {
                        return mapping;
                    }
                }
            } else if (lhs instanceof Yaml.Mapping.Entry) {
                Yaml.Mapping.Entry entry = (Yaml.Mapping.Entry) lhs;
                if (entry.getValue() instanceof Yaml.Scalar &&
                    checkObjectEquality(((Yaml.Scalar) entry.getValue()).getValue(), operator, rhs)) {
                    return entry;
                }
            } else if (lhs instanceof Yaml.Scalar) {
                Yaml.Scalar scalar = (Yaml.Scalar) lhs;
                if (checkObjectEquality(scalar.getValue(), operator, rhs)) {
                    return scalar;
                }
            }
            return null;
        }

        private boolean checkObjectEquality(Object lhs, String operator, Object rhs) {
            BiPredicate<Object, Object> predicate = (lh, rh) -> {
                switch (operator) {
                    case "==":
                        return Objects.equals(lh, rh);
                    case "!=":
                        return !Objects.equals(lh, rh);
                    case "=~":
                        return Pattern.compile(rh.toString()).matcher(lh.toString()).matches();
                }
                return false;
            };

            return predicate.test(lhs, rhs);
        }

        // Extract the result from YAML objects that can match by key.
        public @Nullable Object getResultByKey(Object result, String key) {
            if (result instanceof Yaml.Mapping.Entry) {
                Yaml.Mapping.Entry member = (Yaml.Mapping.Entry) result;
                if (member.getValue() instanceof Yaml.Scalar) {
                    return member.getKey().getValue().equals(key) ? member : null;
                }
            } else if (result instanceof List) {
                for (Object o : ((List<Object>) result)) {
                    Object r = getResultByKey(o, key);
                    if (r != null) {
                        return r;
                    }
                }
            }
            return null;
        }

        // Ensure the scope is set correctly when results are wrapped in a list.
        private Object getResultFromList(Object results) {
            if (results instanceof List) {
                List<Object> matches = (List<Object>) results;
                if (matches.isEmpty()) {
                    return null;
                } else if (matches.size() == 1) {
                    return matches.get(0);
                }
            }
            return results;
        }

        // Extract the value from a Json object.
        private @Nullable Object getValue(Object result) {
            if (result instanceof Yaml.Mapping.Entry) {
                return getValue(((Yaml.Mapping.Entry) result).getValue());
            } else if (result instanceof Yaml.Mapping) {
                return ((Yaml.Mapping) result).getEntries();
            } else if (result instanceof List) {
                List<Object> list = new ArrayList<>();
                for (Object o : ((List<Object>) result)) {
                    Object value = getValue(o);
                    if (value != null) {
                        list.add(value);
                    }
                }
                return getResultFromList(list);
            } else if (result instanceof Yaml.Sequence) {
                return ((Yaml.Sequence) result).getEntries();
            } else if (result instanceof Yaml.Scalar) {
                return ((Yaml.Scalar) result).getValue();
            } else if (result instanceof String) {
                return result;
            }

            return null;
        }

        private static String unquoteStringLiteral(String literal) {
            if (literal != null && (literal.startsWith("'") || literal.startsWith("\""))) {
                return literal.substring(1, literal.length() - 1);
            }
            return "null".equals(literal) ? null : literal;
        }
    }
}
