/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.groovy;

import org.openrewrite.Cursor;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.Tree;
import org.openrewrite.groovy.marker.*;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.groovy.tree.GContainer;
import org.openrewrite.groovy.tree.GRightPadded;
import org.openrewrite.groovy.tree.GSpace;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaPrinter;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;

import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

public class GroovyPrinter<P> extends GroovyVisitor<PrintOutputCapture<P>> {
    private final GroovyJavaPrinter delegate = new GroovyJavaPrinter();

    @Override
    public J visit(@Nullable Tree tree, PrintOutputCapture<P> p) {
        if (!(tree instanceof G)) {
            // re-route printing to the java printer
            return delegate.visit(tree, p);
        } else {
            return super.visit(tree, p);
        }
    }

    @Override
    public J visitCompilationUnit(G.CompilationUnit cu, PrintOutputCapture<P> p) {
        if (cu.getShebang() != null) {
            p.append(cu.getShebang());
        }
        beforeSyntax(cu, Space.Location.COMPILATION_UNIT_PREFIX, p);

        JRightPadded<J.Package> pkg = cu.getPadding().getPackageDeclaration();
        if (pkg != null) {
            visit(pkg.getElement(), p);
            visitSpace(pkg.getAfter(), Space.Location.PACKAGE_SUFFIX, p);
        }

        for (JRightPadded<Statement> statement : cu.getPadding().getStatements()) {
            visitRightPadded(statement, GRightPadded.Location.TOP_LEVEL_STATEMENT_SUFFIX, p);
        }

        visitSpace(cu.getEof(), Space.Location.COMPILATION_UNIT_EOF, p);
        afterSyntax(cu, p);
        return cu;
    }

    @Override
    public J visitGString(G.GString gString, PrintOutputCapture<P> p) {
        beforeSyntax(gString, GSpace.Location.GSTRING, p);
        String delimiter = gString.getDelimiter();
        if (delimiter == null) {
            // For backwards compatibility with ASTs before we collected this field
            delimiter = "\"";
        }
        p.append(delimiter);
        visit(gString.getStrings(), p);
        if ("$/".equals(delimiter)) {
            p.append("/$");
        } else {
            p.append(delimiter);
        }
        afterSyntax(gString, p);
        return gString;
    }

    @Override
    public J visitGStringValue(G.GString.Value value, PrintOutputCapture<P> p) {
        beforeSyntax(value, GSpace.Location.GSTRING, p);
        if (value.isEnclosedInBraces()) {
            p.append("${");
        } else {
            p.append("$");
        }
        visit(value.getTree(), p);
        visitSpace(value.getAfter(), GSpace.Location.GSTRING, p);
        if (value.isEnclosedInBraces()) {
            p.append('}');
        }
        afterSyntax(value, p);
        return value;
    }

    @Override
    public J visitListLiteral(G.ListLiteral listLiteral, PrintOutputCapture<P> p) {
        beforeSyntax(listLiteral, GSpace.Location.LIST_LITERAL, p);
        visitContainer("[", listLiteral.getPadding().getElements(), GContainer.Location.LIST_LITERAL_ELEMENTS,
                ",", "]", p);
        afterSyntax(listLiteral, p);
        return listLiteral;
    }

    @Override
    public J visitMapEntry(G.MapEntry mapEntry, PrintOutputCapture<P> p) {
        beforeSyntax(mapEntry, GSpace.Location.MAP_ENTRY, p);
        visitRightPadded(mapEntry.getPadding().getKey(), GRightPadded.Location.MAP_ENTRY_KEY, p);
        p.append(':');
        visit(mapEntry.getValue(), p);
        afterSyntax(mapEntry, p);
        return mapEntry;
    }

    @Override
    public J visitMapLiteral(G.MapLiteral mapLiteral, PrintOutputCapture<P> p) {
        beforeSyntax(mapLiteral, GSpace.Location.MAP_LITERAL, p);
        visitContainer("[", mapLiteral.getPadding().getElements(), GContainer.Location.MAP_LITERAL_ELEMENTS, ",", "]", p);
        afterSyntax(mapLiteral, p);
        return mapLiteral;
    }

    @Override
    public J visitBinary(G.Binary binary, PrintOutputCapture<P> p) {
        String keyword = "";
        switch (binary.getOperator()) {
            case Find:
                keyword = "=~";
                break;
            case Match:
                keyword = "==~";
                break;
            case Access:
                keyword = "[";
                break;
            case In:
                keyword = "in";
                break;
        }
        beforeSyntax(binary, GSpace.Location.BINARY_PREFIX, p);
        visit(binary.getLeft(), p);
        visitSpace(binary.getPadding().getOperator().getBefore(), GSpace.Location.BINARY_OPERATOR, p);
        p.append(keyword);
        visit(binary.getRight(), p);
        if (binary.getOperator() == G.Binary.Type.Access) {
            visitSpace(binary.getAfter(), GSpace.Location.BINARY_SUFFIX, p);
            p.append("]");
        }
        afterSyntax(binary, p);
        return binary;
    }

    @Override
    public J visitRange(G.Range range, PrintOutputCapture<P> p) {
        beforeSyntax(range, GSpace.Location.RANGE_PREFIX, p);
        visit(range.getFrom(), p);
        visitSpace(range.getPadding().getInclusive().getBefore(), GSpace.Location.RANGE_INCLUSION, p);
        p.append(range.getInclusive() ? ".." : "..>");
        visit(range.getTo(), p);
        afterSyntax(range, p);
        return range;
    }

    @Override
    public Space visitSpace(Space space, GSpace.Location loc, PrintOutputCapture<P> p) {
        return delegate.visitSpace(space, Space.Location.LANGUAGE_EXTENSION, p);
    }

    @Override
    public Space visitSpace(Space space, Space.Location loc, PrintOutputCapture<P> p) {
        return delegate.visitSpace(space, loc, p);
    }

    protected void visitContainer(String before, @Nullable JContainer<? extends J> container, GContainer.Location location,
                                  String suffixBetween, @Nullable String after, PrintOutputCapture<P> p) {
        if (container == null) {
            return;
        }
        visitSpace(container.getBefore(), location.getBeforeLocation(), p);
        p.append(before);
        visitRightPadded(container.getPadding().getElements(), location.getElementLocation(), suffixBetween, p);
        p.append(after == null ? "" : after);
    }

    protected void visitRightPadded(List<? extends JRightPadded<? extends J>> nodes, GRightPadded.Location location, String suffixBetween, PrintOutputCapture<P> p) {
        for (int i = 0; i < nodes.size(); i++) {
            JRightPadded<? extends J> node = nodes.get(i);
            visit(node.getElement(), p);
            visitSpace(node.getAfter(), location.getAfterLocation(), p);
            if (i < nodes.size() - 1) {
                p.append(suffixBetween);
            } else {
                for (Marker m : node.getMarkers().getMarkers()) {
                    // Maintaining for backwards compatibility with old LSTs
                    //noinspection deprecation
                    if (m instanceof TrailingComma) {
                        p.append(suffixBetween);
                        //noinspection deprecation
                        visitSpace(((TrailingComma) m).getSuffix(), Space.Location.LANGUAGE_EXTENSION, p);
                        break;
                    } else if (m instanceof org.openrewrite.java.marker.TrailingComma) {
                        p.append(suffixBetween);
                        visitSpace(((org.openrewrite.java.marker.TrailingComma) m).getSuffix(), Space.Location.LANGUAGE_EXTENSION, p);
                        break;
                    }
                }
            }
        }
    }

    private class GroovyJavaPrinter extends JavaPrinter<P> {
        @Override
        public J visit(@Nullable Tree tree, PrintOutputCapture<P> p) {
            if (tree instanceof G) {
                // re-route printing back up to groovy
                return GroovyPrinter.this.visit(tree, p);
            } else {
                return super.visit(tree, p);
            }
        }

        @Override
        public J visitImport(J.Import import_, PrintOutputCapture<P> p) {
            J.Import i = (J.Import) super.visitImport(import_, p);
            JLeftPadded<J.Identifier> alias = i.getPadding().getAlias();
            if (alias == null) {
                return i;
            }

            visitSpace(alias.getBefore(), Space.Location.IMPORT_ALIAS_PREFIX, p);
            p.append("as");
            visitIdentifier(alias.getElement(), p);
            return i;
        }

        @Override
        public J visitTypeCast(J.TypeCast t, PrintOutputCapture<P> p) {
            if (!t.getMarkers().findFirst(AsStyleTypeCast.class).isPresent()) {
                return super.visitTypeCast(t, p);
            }
            beforeSyntax(t, Space.Location.TYPE_CAST_PREFIX, p);
            visit(t.getExpression(), p);
            visitSpace(t.getClazz().getPadding().getTree().getAfter(), Space.Location.CONTROL_PARENTHESES_PREFIX, p);
            p.append("as");
            visit(t.getClazz().getTree(), p);
            afterSyntax(t, p);
            return t;
        }

        @Override
        public J visitLambda(J.Lambda lambda, PrintOutputCapture<P> p) {
            beforeSyntax(lambda, Space.Location.LAMBDA_PREFIX, p);
            LambdaStyle ls = lambda.getMarkers().findFirst(LambdaStyle.class)
                    .orElse(new LambdaStyle(null, false, !lambda.getParameters().getParameters().isEmpty()));
            boolean parenthesized = lambda.getParameters().isParenthesized();
            if(!ls.isJavaStyle()) {
                p.append('{');
            }
            visitMarkers(lambda.getParameters().getMarkers(), p);
            visitSpace(lambda.getParameters().getPrefix(), Space.Location.LAMBDA_PARAMETERS_PREFIX, p);
            if(parenthesized) {
                p.append('(');
            }
            visitRightPadded(lambda.getParameters().getPadding().getParams(), JRightPadded.Location.LAMBDA_PARAM, ",", p);
            if(parenthesized) {
                p.append(')');
            }
            if (ls.isArrow()) {
                visitSpace(lambda.getArrow(), Space.Location.LAMBDA_ARROW_PREFIX, p);
                p.append("->");
            }
            if (lambda.getBody() instanceof J.Block) {
                J.Block block = (J.Block) lambda.getBody();
                visitStatements(block.getPadding().getStatements(), JRightPadded.Location.BLOCK_STATEMENT, p);
                visitSpace(block.getEnd(), Space.Location.BLOCK_END, p);
            } else {
                visit(lambda.getBody(), p);
            }
            if(!ls.isJavaStyle()) {
                p.append('}');
            }
            afterSyntax(lambda, p);
            return lambda;
        }

        @Override
        public J visitFieldAccess(J.FieldAccess fieldAccess, PrintOutputCapture<P> p) {
            beforeSyntax(fieldAccess, Space.Location.FIELD_ACCESS_PREFIX, p);
            visit(fieldAccess.getTarget(), p);

            Markers nameMarkers = fieldAccess.getName().getMarkers();
            if (nameMarkers.findFirst(NullSafe.class).isPresent()) {
                p.append('?');
            }
            if (nameMarkers.findFirst(StarDot.class).isPresent()) {
                p.append('*');
            }

            visitLeftPadded(".", fieldAccess.getPadding().getName(), JLeftPadded.Location.FIELD_ACCESS_NAME, p);
            afterSyntax(fieldAccess, p);
            return fieldAccess;
        }

        @Override
        public J visitForEachLoop(J.ForEachLoop forEachLoop, PrintOutputCapture<P> p) {
            beforeSyntax(forEachLoop, Space.Location.FOR_EACH_LOOP_PREFIX, p);
            p.append("for");
            J.ForEachLoop.Control ctrl = forEachLoop.getControl();
            visitSpace(ctrl.getPrefix(), Space.Location.FOR_EACH_CONTROL_PREFIX, p);
            p.append('(');
            String suffix = forEachLoop.getMarkers().findFirst(InStyleForEachLoop.class).isPresent() ? "in" : ":";
            visitRightPadded(ctrl.getPadding().getVariable(), JRightPadded.Location.FOREACH_VARIABLE, suffix, p);
            visitRightPadded(ctrl.getPadding().getIterable(), JRightPadded.Location.FOREACH_ITERABLE, "", p);
            p.append(')');
            visitStatement(forEachLoop.getPadding().getBody(), JRightPadded.Location.FOR_BODY, p);
            afterSyntax(forEachLoop, p);
            return forEachLoop;
        }

        @Override
        public J visitMethodInvocation(J.MethodInvocation method, PrintOutputCapture<P> p) {
            beforeSyntax(method, Space.Location.METHOD_INVOCATION_PREFIX, p);

            Markers nameMarkers = method.getName().getMarkers();
            visitRightPadded(method.getPadding().getSelect(), JRightPadded.Location.METHOD_SELECT,
                    nameMarkers.findFirst(NullSafe.class).isPresent() ? "?." :
                            nameMarkers.findFirst(StarDot.class).isPresent() ? "*." :
                                    nameMarkers.findFirst(ImplicitDot.class).isPresent() ? "" : ".", p);

            visitContainer("<", method.getPadding().getTypeParameters(), JContainer.Location.TYPE_PARAMETERS, ",", ">", p);
            visit(method.getName(), p);
            Optional<EmptyArgumentListPrecedesArgument> maybeEal = method.getMarkers().findFirst(EmptyArgumentListPrecedesArgument.class);
            // If a recipe changed the number or kind of arguments then this marker is nonsense, even if the recipe forgets to remove it
            if (maybeEal.isPresent() && method.getArguments().size() == 1 && method.getArguments().get(0) instanceof J.Lambda) {
                EmptyArgumentListPrecedesArgument eal = maybeEal.get();
                visitSpace(eal.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p);
                p.append("(");
                visitSpace(eal.getInfix(), Space.Location.LANGUAGE_EXTENSION, p);
                p.append(")");
            }
            JContainer<Expression> argContainer = method.getPadding().getArguments();

            visitSpace(argContainer.getBefore(), Space.Location.METHOD_INVOCATION_ARGUMENTS, p);
            List<JRightPadded<Expression>> args = argContainer.getPadding().getElements();
            for (int i = 0; i < args.size(); i++) {
                JRightPadded<Expression> arg = args.get(i);
                boolean omitParens = arg.getElement().getMarkers()
                                             .findFirst(OmitParentheses.class)
                                             .isPresent() ||
                                     arg.getElement().getMarkers()
                                             .findFirst(org.openrewrite.java.marker.OmitParentheses.class)
                                             .isPresent();

                if (i == 0 && !omitParens) {
                    p.append('(');
                } else if (i > 0 && omitParens && (
                        !args.get(0).getElement().getMarkers().findFirst(OmitParentheses.class).isPresent() &&
                        !args.get(0).getElement().getMarkers().findFirst(org.openrewrite.java.marker.OmitParentheses.class).isPresent()
                )) {
                    p.append(')');
                } else if (i > 0) {
                    p.append(',');
                }

                visitRightPadded(arg, JRightPadded.Location.METHOD_INVOCATION_ARGUMENT, p);

                if (i == args.size() - 1 && !omitParens) {
                    p.append(')');
                }
            }

            afterSyntax(method, p);
            return method;
        }

        @Override
        public J visitReturn(J.Return return_, PrintOutputCapture<P> p) {
            if (return_.getMarkers().findFirst(ImplicitReturn.class).isPresent() ||
                return_.getMarkers().findFirst(org.openrewrite.java.marker.ImplicitReturn.class).isPresent()) {
                visitSpace(return_.getPrefix(), Space.Location.RETURN_PREFIX, p);
                visitMarkers(return_.getMarkers(), p);
                visit(return_.getExpression(), p);
                afterSyntax(return_, p);
                return return_;
            }
            return super.visitReturn(return_, p);
        }

        protected void visitStatement(@Nullable JRightPadded<Statement> paddedStat, JRightPadded.Location location, PrintOutputCapture<P> p) {
            if (paddedStat != null) {
                visit(paddedStat.getElement(), p);
                visitSpace(paddedStat.getAfter(), location.getAfterLocation(), p);
                visitMarkers(paddedStat.getMarkers(), p);
            }
        }

        @Override
        public J visitTernary(J.Ternary ternary, PrintOutputCapture<P> p) {
            beforeSyntax(ternary, Space.Location.TERNARY_PREFIX, p);
            visit(ternary.getCondition(), p);
            if (ternary.getMarkers().findFirst(Elvis.class).isPresent()) {
                visitSpace(ternary.getPadding().getTruePart().getBefore(), Space.Location.TERNARY_TRUE, p);
                p.append("?:");
                visit(ternary.getFalsePart(), p);
            } else {
                visitLeftPadded("?", ternary.getPadding().getTruePart(), JLeftPadded.Location.TERNARY_TRUE, p);
                visitLeftPadded(":", ternary.getPadding().getFalsePart(), JLeftPadded.Location.TERNARY_FALSE, p);
            }
            afterSyntax(ternary, p);
            return ternary;
        }

        @Override
        public <M extends Marker> M visitMarker(Marker marker, PrintOutputCapture<P> p) {
            //noinspection deprecation
            if (marker instanceof Semicolon || marker instanceof org.openrewrite.java.marker.Semicolon) {
                p.append(';');
            }
            return super.visitMarker(marker, p);
        }
    }

    @Override
    public <M extends Marker> M visitMarker(Marker marker, PrintOutputCapture<P> p) {
        return delegate.visitMarker(marker, p);
    }

    private static final UnaryOperator<String> JAVA_MARKER_WRAPPER =
            out -> "/*~~" + out + (out.isEmpty() ? "" : "~~") + ">*/";

    private void beforeSyntax(G g, Space.Location loc, PrintOutputCapture<P> p) {
        beforeSyntax(g.getPrefix(), g.getMarkers(), loc, p);
    }

    private void beforeSyntax(G g, GSpace.Location loc, PrintOutputCapture<P> p) {
        beforeSyntax(g.getPrefix(), g.getMarkers(), loc, p);
    }

    private void beforeSyntax(Space prefix, Markers markers, @Nullable GSpace.Location loc, PrintOutputCapture<P> p) {
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().beforePrefix(marker, new Cursor(getCursor(), marker), JAVA_MARKER_WRAPPER));
        }
        if (loc != null) {
            visitSpace(prefix, loc, p);
        }
        visitMarkers(markers, p);
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().beforeSyntax(marker, new Cursor(getCursor(), marker), JAVA_MARKER_WRAPPER));
        }
    }

    private void beforeSyntax(Space prefix, Markers markers, @Nullable Space.Location loc, PrintOutputCapture<P> p) {
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().beforePrefix(marker, new Cursor(getCursor(), marker), JAVA_MARKER_WRAPPER));
        }
        if (loc != null) {
            visitSpace(prefix, loc, p);
        }
        visitMarkers(markers, p);
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().beforeSyntax(marker, new Cursor(getCursor(), marker), JAVA_MARKER_WRAPPER));
        }
    }

    private void afterSyntax(G g, PrintOutputCapture<P> p) {
        afterSyntax(g.getMarkers(), p);
    }

    private void afterSyntax(Markers markers, PrintOutputCapture<P> p) {
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().afterSyntax(marker, new Cursor(getCursor(), marker), JAVA_MARKER_WRAPPER));
        }
    }
}
