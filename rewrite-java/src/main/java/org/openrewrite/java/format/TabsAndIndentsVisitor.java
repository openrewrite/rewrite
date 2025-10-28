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
package org.openrewrite.java.format;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.service.SourcePositionService;
import org.openrewrite.java.style.SpacesStyle;
import org.openrewrite.java.style.TabsAndIndentsStyle;
import org.openrewrite.java.style.WrappingAndBracesStyle;
import org.openrewrite.java.tree.*;

import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;
import static org.openrewrite.java.format.ColumnPositionCalculator.computeColumnPosition;

public class TabsAndIndentsVisitor<P> extends JavaIsoVisitor<P> {
    @Nullable
    private final Tree stopAfter;

    private final TabsAndIndentsStyle style;
    private final SpacesStyle spacesStyle;
    private final WrappingAndBracesStyle wrappingStyle;

    public TabsAndIndentsVisitor(TabsAndIndentsStyle style, SpacesStyle spacesStyle, WrappingAndBracesStyle wrappingStyle) {
        this(style, spacesStyle, wrappingStyle, null);
    }

    public TabsAndIndentsVisitor(TabsAndIndentsStyle style, SpacesStyle spacesStyle, WrappingAndBracesStyle wrappingStyle, @Nullable Tree stopAfter) {
        this.style = style;
        this.spacesStyle = spacesStyle;
        this.wrappingStyle = wrappingStyle;
        this.stopAfter = stopAfter;
    }

    @Override
    public @Nullable J visit(@Nullable Tree tree, P p, Cursor parent) {
        setCursor(parent);
        for (Cursor c = parent; c != null; c = c.getParent()) {
            Object v = c.getValue();
            Space space = null;
            if (v instanceof J) {
                space = ((J) v).getPrefix();
            } else if (v instanceof JRightPadded) {
                space = ((JRightPadded<?>) v).getAfter();
            } else if (v instanceof JLeftPadded) {
                space = ((JLeftPadded<?>) v).getBefore();
            } else if (v instanceof JContainer) {
                space = ((JContainer<?>) v).getBefore();
            }

            if (space != null && space.getLastWhitespace().contains("\n")) {
                int indent = findIndent(space);
                if (indent != 0) {
                    c.putMessage("lastIndent", indent);
                }
            }
        }
        Iterator<Object> itr = parent.getPath(J.class::isInstance);
        J next = (itr.hasNext()) ? (J) itr.next() : null;
        if (next != null) {
            preVisit(next, p);
        }

        return visit(tree, p);
    }

    @Override
    public @Nullable J preVisit(@Nullable J tree, P p) {
        if (tree instanceof JavaSourceFile ||
                tree instanceof J.Package ||
                tree instanceof J.Import ||
                tree instanceof J.Label ||
                tree instanceof J.DoWhileLoop ||
                tree instanceof J.ArrayDimension ||
                tree instanceof J.ClassDeclaration) {
            getCursor().putMessage("indentType", IndentType.ALIGN);
        } else if (tree instanceof J.Block ||
                tree instanceof J.If ||
                tree instanceof J.If.Else ||
                tree instanceof J.ForLoop ||
                tree instanceof J.ForEachLoop ||
                tree instanceof J.WhileLoop ||
                tree instanceof J.Case ||
                tree instanceof J.EnumValueSet) {
            getCursor().putMessage("indentType", IndentType.INDENT);
        } else {
            getCursor().putMessage("indentType", IndentType.CONTINUATION_INDENT);
        }

        return tree;
    }

    @Override
    public J.ForLoop.Control visitForControl(J.ForLoop.Control control, P p) {
        // FIXME fix formatting of control sections
        return control;
    }

    @Override
    public J.ForEachLoop.Control visitForEachControl(J.ForEachLoop.Control control, P p) {
        // FIXME fix formatting of control sections
        return control;
    }

    @Override
    public Space visitSpace(Space space, Space.Location loc, P p) {
        getCursor().putMessage("lastLocation", loc);
        boolean alignToAnnotation = false;
        Cursor parent = getCursor().getParent();
        if (parent != null && parent.getValue() instanceof J.Annotation) {
            parent.getParentOrThrow().putMessage("afterAnnotation", true);
        } else if (parent != null && !getCursor().getParentOrThrow().getPath(J.Annotation.class::isInstance).hasNext()) {
            // when annotations are on their own line, other parts of the declaration that follow are aligned left to it
            alignToAnnotation = getCursor().pollNearestMessage("afterAnnotation") != null &&
                    !(getCursor().getParentOrThrow().getValue() instanceof J.Annotation);
        }

        if (space.getComments().isEmpty() && !space.getLastWhitespace().contains("\n") || parent == null) {
            return space;
        }

        if (loc == Space.Location.METHOD_SELECT_SUFFIX) {
            Integer chainedIndent = getCursor().getParentTreeCursor().getMessage("chainedIndent");
            if (chainedIndent != null) {
                getCursor().getParentTreeCursor().putMessage("lastIndent", chainedIndent);
                return indentTo(space, chainedIndent, loc);
            }
        }

        int indent = getCursor().getNearestMessage("lastIndent", 0);

        IndentType indentType = getCursor().getParentOrThrow().getNearestMessage("indentType", IndentType.ALIGN);

        // block spaces are always aligned to their parent
        Object value = getCursor().getValue();
        boolean alignBlockPrefixToParent = loc == Space.Location.BLOCK_PREFIX && space.getWhitespace().contains("\n") &&
                // ignore init blocks.
                (value instanceof J.Block && !(getCursor().getParentTreeCursor().getValue() instanceof J.Block));

        boolean alignBlockToParent = loc == Space.Location.BLOCK_END ||
                loc == Space.Location.NEW_ARRAY_INITIALIZER_SUFFIX ||
                loc == Space.Location.CATCH_PREFIX ||
                loc == Space.Location.TRY_FINALLY ||
                loc == Space.Location.ELSE_PREFIX;

        if ((loc == Space.Location.EXTENDS && space.getWhitespace().contains("\n")) ||
                Space.Location.EXTENDS == getCursor().getParent().getMessage("lastLocation")) {
            indentType = IndentType.CONTINUATION_INDENT;
        }

        if (alignBlockPrefixToParent || alignBlockToParent || alignToAnnotation) {
            indentType = IndentType.ALIGN;
        }

        switch (indentType) {
            case ALIGN:
                break;
            case INDENT:
                indent += style.getIndentSize();
                break;
            case CONTINUATION_INDENT:
                indent += style.getContinuationIndent();
                break;
        }

        Space s = indentTo(space, indent, loc);
        if (value instanceof J && !(value instanceof J.EnumValueSet)) {
            getCursor().putMessage("lastIndent", indent);
        } else if (loc == Space.Location.METHOD_SELECT_SUFFIX) {
            getCursor().getParentTreeCursor().putMessage("lastIndent", indent);
        }

        return s;
    }

    @Override
    public <T> @Nullable JRightPadded<T> visitRightPadded(@Nullable JRightPadded<T> right, JRightPadded.Location loc, P p) {
        if (right == null) {
            //noinspection ConstantConditions
            return null;
        }

        setCursor(new Cursor(getCursor(), right));

        T t = right.getElement();
        Space after;

        int indent = getCursor().getNearestMessage("lastIndent", 0);
        IndentType indentType = getCursor().getParent().getNearestMessage("indentType", IndentType.ALIGN);
        if (right.getElement() instanceof J) {
            J elem = (J) right.getElement();
            if (right.getAfter().getLastWhitespace().contains("\n") ||
                    elem.getPrefix().getLastWhitespace().contains("\n") ||
                    (elem.getPrefix().getWhitespace().contains("\n") && elem.getPrefix().getComments().stream().noneMatch(c -> c.getSuffix().contains("\n")))) {
                switch (loc) {
                    case FOR_CONDITION:
                    case FOR_UPDATE: {
                        J.ForLoop.Control control = getCursor().getParentOrThrow().getValue();
                        Space initPrefix = Space.firstPrefix(control.getInit());
                        if (!initPrefix.getLastWhitespace().contains("\n")) {
                            int initIndent = forInitColumn();
                            getCursor().getParentOrThrow().putMessage("lastIndent", initIndent - style.getContinuationIndent());
                            elem = visitAndCast(elem, p);
                            getCursor().getParentOrThrow().putMessage("lastIndent", indent);
                            after = indentTo(right.getAfter(), initIndent, loc.getAfterLocation());
                        } else {
                            elem = visitAndCast(elem, p);
                            after = visitSpace(right.getAfter(), loc.getAfterLocation(), p);
                        }
                        break;
                    }
                    case METHOD_DECLARATION_PARAMETER:
                    case RECORD_STATE_VECTOR: {
                        if (elem instanceof J.Empty) {
                            elem = elem.withPrefix(indentTo(elem.getPrefix(), indent, loc.getAfterLocation()));
                            after = right.getAfter();
                            break;
                        }
                        JContainer<J> container = getCursor().getParentOrThrow().getValue();
                        List<J> elements = container.getElements();
                        J lastArg = elements.get(elements.size() - 1);
                        //noinspection ConstantConditions
                        J tree = null;
                        if (loc == JRightPadded.Location.METHOD_DECLARATION_PARAMETER) {
                            tree = getCursor().firstEnclosing(J.MethodDeclaration.class);
                        } else {
                            tree = getCursor().firstEnclosing(J.ClassDeclaration.class);
                            getCursor().getParentOrThrow().putMessage("indentType", IndentType.CONTINUATION_INDENT);
                        }
                        if (elements.size() > 1) {
                            try {
                                if ((loc == JRightPadded.Location.METHOD_DECLARATION_PARAMETER && style.getMethodDeclarationParameters().getAlignWhenMultiple()) ||
                                        (loc == JRightPadded.Location.RECORD_STATE_VECTOR && style.getRecordComponents().getAlignWhenMultiple())) {
                                    if (tree != null) {
                                        JavaSourceFile sourceFile = getCursor().firstEnclosing(JavaSourceFile.class);
                                        int alignTo;
                                        try {
                                            alignTo = sourceFile.service(SourcePositionService.class).computeColumnToAlignTo(new Cursor(getCursor(), elem), style.getContinuationIndent());
                                        } catch (UnsupportedOperationException e) {
                                            // Sourcefile$service(...) might give UnsupportedOperationException depending on the runtime, the lst build date... we use deprecated behavior in that case
                                            alignTo = computeFirstParameterColumn(tree);
                                        }
                                        if (alignTo != -1) {
                                            getCursor().getParentOrThrow().putMessage("lastIndent", alignTo - style.getContinuationIndent());
                                            elem = visitAndCast(elem, p);
                                            getCursor().getParentOrThrow().putMessage("lastIndent", indent);
                                            after = indentTo(right.getAfter(), t == lastArg ? indent : alignTo, loc.getAfterLocation());
                                        } else {
                                            after = right.getAfter();
                                        }
                                    } else {
                                        after = right.getAfter();
                                    }
                                } else {
                                    elem = visitAndCast(elem, p);
                                    after = indentTo(right.getAfter(), t == lastArg ? indent : style.getContinuationIndent(), loc.getAfterLocation());
                                }
                            } catch (NoSuchMethodError error) {
                                // style.getRecordComponents introduction might give NoSuchMethodError depending on the runtime, the lst build date...
                                elem = visitAndCast(elem, p);
                                after = indentTo(right.getAfter(), t == lastArg ? indent : style.getContinuationIndent(), loc.getAfterLocation());
                            }
                        } else {
                            if (elem.getPrefix().getLastWhitespace().contains("\n") && tree != null) {
                                JavaSourceFile sourceFile = getCursor().firstEnclosing(JavaSourceFile.class);
                                int alignTo;
                                try {
                                    alignTo = sourceFile.service(SourcePositionService.class).computeColumnToAlignTo(new Cursor(getCursor(), elem), style.getContinuationIndent());
                                } catch (UnsupportedOperationException error) {
                                    // Sourcefile$service(...) might give UnsupportedOperationException depending on the runtime, the lst build date... we use deprecated behavior in that case
                                    alignTo = computeFirstParameterColumn(tree);
                                }
                                if (alignTo != -1) {
                                    getCursor().getParentTreeCursor().putMessage("lastIndent", alignTo - style.getContinuationIndent());
                                    elem = visitAndCast(elem, p);
                                    getCursor().getParentTreeCursor().putMessage("lastIndent", indent);
                                }
                            }
                            after = right.getAfter();
                        }
                        getCursor().getParentOrThrow().putMessage("indentType", indentType);
                        break;
                    }
                    case METHOD_INVOCATION_ARGUMENT:
                        if (!elem.getPrefix().getLastWhitespace().contains("\n")) {
                            if (elem instanceof J.Lambda) {
                                J body = ((J.Lambda) elem).getBody();
                                if (!(body instanceof J.Binary)) {
                                    if (!body.getPrefix().getLastWhitespace().contains("\n")) {
                                        getCursor().getParentOrThrow().putMessage("lastIndent", indent + style.getContinuationIndent());
                                    }
                                }
                            }
                        }
                        elem = visitAndCast(elem, p);
                        after = indentTo(right.getAfter(), indent, loc.getAfterLocation());
                        if (!after.getComments().isEmpty() || after.getLastWhitespace().contains("\n")) {
                            Cursor parent = getCursor().getParentTreeCursor();
                            Cursor grandparent = parent.getParentTreeCursor();
                            // propagate indentation up in the method chain hierarchy
                            if (grandparent.getValue() instanceof J.MethodInvocation && ((J.MethodInvocation) grandparent.getValue()).getSelect() == parent.getValue()) {
                                grandparent.putMessage("lastIndent", indent);
                                grandparent.putMessage("chainedIndent", indent);
                            }
                        }
                        break;
                    case NEW_CLASS_ARGUMENTS:
                    case ARRAY_INDEX:
                    case PARENTHESES:
                    case TYPE_PARAMETER: {
                        elem = visitAndCast(elem, p);
                        after = indentTo(right.getAfter(), indent, loc.getAfterLocation());
                        break;
                    }
                    case ANNOTATION_ARGUMENT:
                        JContainer<J> args = getCursor().getParentOrThrow().getValue();
                        elem = visitAndCast(elem, p);

                        // the end parentheses on an annotation is aligned to the annotation
                        if (args.getPadding().getElements().get(args.getElements().size() - 1) == right) {
                            getCursor().getParentOrThrow().putMessage("indentType", IndentType.ALIGN);
                        }

                        after = visitSpace(right.getAfter(), loc.getAfterLocation(), p);
                        break;
                    case TRY_RESOURCE:
                        // Align subsequent resources with the first resource in try-with-resources
                        JContainer<J> resources = getCursor().getParentOrThrow().getValue();
                        List<JRightPadded<J>> resourceElements = resources.getPadding().getElements();
                        if (resourceElements.size() > 1 && resourceElements.get(0) != right && elem.getPrefix().getLastWhitespace().contains("\n")) {
                            // For resources after the first one, align with the first resource
                            J firstResource = resourceElements.get(0).getElement();
                            if (!firstResource.getPrefix().getLastWhitespace().contains("\n")) {
                                // First resource is on the same line as try(
                                // We need to calculate the indent based on the try statement position
                                // For now, let's use a simplified approach - align with the opening parenthesis
                                // Count the column position by looking at the try statement prefix and "try ("
                                J.Try tryStatement = getCursor().firstEnclosing(J.Try.class);
                                String tryPrefix = tryStatement.getPrefix().getWhitespace();
                                int tryIndent = getLengthOfWhitespace(tryPrefix.substring(tryPrefix.lastIndexOf('\n') + 1));
                                // Add 3 for "try" and 1 for "(" and optionally one for space in between
                                int firstResourceColumn = tryIndent + 4 + (spacesStyle.getBeforeParentheses().getTryParentheses() ? 1 : 0);
                                elem = elem.withPrefix(indentTo(elem.getPrefix(), firstResourceColumn, Space.Location.TRY_RESOURCE));
                                elem = visitAndCast(elem, p);
                                after = right.getAfter();
                            } else {
                                // First resource is on a new line, align with it
                                String firstResourcePrefix = firstResource.getPrefix().getWhitespace();
                                int firstResourceIndent = getLengthOfWhitespace(firstResourcePrefix.substring(firstResourcePrefix.lastIndexOf('\n') + 1));
                                elem = elem.withPrefix(indentTo(elem.getPrefix(), firstResourceIndent, Space.Location.TRY_RESOURCE));
                                elem = visitAndCast(elem, p);
                                after = right.getAfter();
                            }
                        } else {
                            // For the first resource or single resource, use default handling
                            elem = visitAndCast(elem, p);
                            after = visitSpace(right.getAfter(), loc.getAfterLocation(), p);
                        }
                        break;
                    default:
                        elem = visitAndCast(elem, p);
                        if (right.getAfter().getLastWhitespace().contains("\n") && alignWhenMultiple(loc)) {
                            JavaSourceFile sourceFile = getCursor().firstEnclosing(JavaSourceFile.class);
                            int alignTo;
                            try {
                                alignTo = sourceFile.service(SourcePositionService.class).computeColumnToAlignTo(new Cursor(getCursor(), elem), style.getContinuationIndent());
                            } catch (UnsupportedOperationException e) {
                                // Sourcefile$service(...) might give UnsupportedOperationException depending on the runtime, the lst build date... we do not align in that case.
                                alignTo = -1;
                            }
                            if (alignTo != -1) {
                                after = indentTo(right.getAfter(), alignTo, loc.getAfterLocation());
                            } else {
                                after = visitSpace(right.getAfter(), loc.getAfterLocation(), p);
                            }
                        } else {
                            after = visitSpace(right.getAfter(), loc.getAfterLocation(), p);
                        }
                }
            } else {
                switch (loc) {
                    case NEW_CLASS_ARGUMENTS:
                    case METHOD_INVOCATION_ARGUMENT:
                    case RECORD_STATE_VECTOR:
                    case METHOD_DECLARATION_PARAMETER:
                        if (!elem.getPrefix().getLastWhitespace().contains("\n")) {
                            JContainer<J> args = getCursor().getParentOrThrow().getValue();
                            boolean anyOtherArgOnOwnLine = false;
                            for (JRightPadded<J> arg : args.getPadding().getElements()) {
                                if (arg == getCursor().getValue()) {
                                    continue;
                                }
                                if (arg.getElement().getPrefix().getLastWhitespace().contains("\n")) {
                                    anyOtherArgOnOwnLine = true;
                                    break;
                                }
                            }
                            if (!anyOtherArgOnOwnLine) {
                                elem = visitAndCast(elem, p);
                                after = indentTo(right.getAfter(), indent, loc.getAfterLocation());
                                break;
                            }
                        }
                        if (!(elem instanceof J.Binary)) {
                            if (!(elem instanceof J.MethodInvocation)) {
                                getCursor().putMessage("lastIndent", indent + style.getContinuationIndent());
                            } else if (elem.getPrefix().getLastWhitespace().contains("\n")) {
                                getCursor().putMessage("lastIndent", indent + style.getContinuationIndent());
                            } else {
                                J.MethodInvocation methodInvocation = (J.MethodInvocation) elem;
                                Expression select = methodInvocation.getSelect();
                                if (select instanceof J.FieldAccess || select instanceof J.Identifier || select instanceof J.MethodInvocation) {
                                    getCursor().putMessage("lastIndent", indent + style.getContinuationIndent());
                                }
                            }
                        }
                        elem = visitAndCast(elem, p);
                        after = visitSpace(right.getAfter(), loc.getAfterLocation(), p);
                        break;
                    default:
                        elem = visitAndCast(elem, p);
                        after = right.getAfter();
                }
            }

            //noinspection unchecked
            t = (T) elem;
        } else {
            after = visitSpace(right.getAfter(), loc.getAfterLocation(), p);
        }

        setCursor(getCursor().getParent());
        return (after == right.getAfter() && t == right.getElement()) ? right : new JRightPadded<>(t, after, right.getMarkers());
    }

    @Deprecated
    private int computeFirstParameterColumn(J tree) {
        List<JRightPadded<Statement>> arguments;
        if (tree instanceof J.MethodDeclaration) {
            arguments = ((J.MethodDeclaration) tree).getPadding().getParameters().getPadding().getElements();
        } else if (tree instanceof J.ClassDeclaration) {
            JContainer<Statement> primaryConstructorArgs = ((J.ClassDeclaration) tree).getPadding().getPrimaryConstructor();
            arguments = primaryConstructorArgs == null ? emptyList() : primaryConstructorArgs.getPadding().getElements();
        } else {
            return -1;
        }
        J firstArg = arguments.isEmpty() ? null : arguments.get(0).getElement();
        if (firstArg == null || firstArg instanceof J.Empty) {
            return -1;
        }

        if (firstArg.getPrefix().getLastWhitespace().contains("\n")) {
            int declPrefixLength = getLengthOfWhitespace(tree.getPrefix().getLastWhitespace());

            return declPrefixLength + style.getContinuationIndent();
        } else {
            return computeColumnPosition(tree, firstArg, getCursor());
        }
    }

    @Override
    public <J2 extends J> @Nullable JContainer<J2> visitContainer(@Nullable JContainer<J2> container, JContainer.Location loc, P p) {
        if (container == null) {
            return null;
        }

        setCursor(new Cursor(getCursor(), container));

        Space before;
        List<JRightPadded<J2>> js;

        int indent = getCursor().getNearestMessage("lastIndent", 0);
        if (container.getBefore().getLastWhitespace().contains("\n")) {
            switch (loc) {
                case TYPE_PARAMETERS:
                case IMPLEMENTS:
                case THROWS:
                case NEW_CLASS_ARGUMENTS:
                    before = indentTo(container.getBefore(), indent + style.getContinuationIndent(), loc.getBeforeLocation());
                    getCursor().putMessage("indentType", IndentType.ALIGN);
                    getCursor().putMessage("lastIndent", indent + style.getContinuationIndent());
                    js = ListUtils.map(container.getPadding().getElements(), t -> visitRightPadded(t, loc.getElementLocation(), p));
                    break;
                default:
                    before = visitSpace(container.getBefore(), loc.getBeforeLocation(), p);
                    js = ListUtils.map(container.getPadding().getElements(), t -> visitRightPadded(t, loc.getElementLocation(), p));
            }
        } else {
            switch (loc) {
                case IMPLEMENTS:
                case METHOD_INVOCATION_ARGUMENTS:
                case NEW_CLASS_ARGUMENTS:
                case TYPE_PARAMETERS:
                case THROWS:
                    getCursor().putMessage("indentType", IndentType.CONTINUATION_INDENT);
                    before = visitSpace(container.getBefore(), loc.getBeforeLocation(), p);
                    js = ListUtils.map(container.getPadding().getElements(), t -> visitRightPadded(t, loc.getElementLocation(), p));
                    break;
                default:
                    before = visitSpace(container.getBefore(), loc.getBeforeLocation(), p);
                    js = ListUtils.map(container.getPadding().getElements(), t -> visitRightPadded(t, loc.getElementLocation(), p));
            }
        }

        setCursor(getCursor().getParent());
        return js == container.getPadding().getElements() && before == container.getBefore() ?
                container :
                JContainer.build(before, js, container.getMarkers());
    }

    private boolean alignWhenMultiple(JRightPadded.Location loc) {
        Supplier<@Nullable Boolean> isAlignedWhenMultipleFromStyle;
        boolean intelliJDefault;
        switch (loc) {
            case FOR_CONDITION:
            case FOR_UPDATE:
            case METHOD_DECLARATION_PARAMETER:
            case RECORD_STATE_VECTOR:
            case TRY_RESOURCE:
                isAlignedWhenMultipleFromStyle = () -> null;
                intelliJDefault = true;
                break;
            case METHOD_SELECT:
                //noinspection ConstantConditions
                isAlignedWhenMultipleFromStyle = () -> wrappingStyle.getChainedMethodCalls() != null && wrappingStyle.getChainedMethodCalls().getAlignWhenMultiline();
                intelliJDefault = false;
                break;
            default:
                isAlignedWhenMultipleFromStyle = () -> null;
                intelliJDefault = false;
        }
        //Using this way as it allows is later to easily add new alignment booleans on different styles without risk of NoSuchMethodError.
        try {
            Boolean aligned = isAlignedWhenMultipleFromStyle.get();
            if (aligned != null) {
                return aligned;
            }
        } catch (NoSuchMethodError ignored) {
            // some style.get...()  or style.get...().get...() might give NoSuchMethodError depending on the runtime, the lst build date... In that case we return intelliJ's default;
        }
        return intelliJDefault;
    }

    private Space indentTo(Space space, int column, Space.Location spaceLocation) {
        Space s = space;
        String whitespace = s.getWhitespace();

        if (spaceLocation == Space.Location.COMPILATION_UNIT_PREFIX && !StringUtils.isNullOrEmpty(whitespace)) {
            s = s.withWhitespace("");
        } else if (s.getComments().isEmpty() && !s.getLastWhitespace().contains("\n")) {
            return s;
        }

        if (s.getComments().isEmpty()) {
            int indent = findIndent(s);
            if (indent != column) {
                int shift = column - indent;
                s = s.withWhitespace(indent(whitespace, shift));
            }
        } else {
            boolean hasFileLeadingComment = !space.getComments().isEmpty() && (
                    spaceLocation == Space.Location.COMPILATION_UNIT_PREFIX ||
                            (spaceLocation == Space.Location.CLASS_DECLARATION_PREFIX && space.getComments().get(0).isMultiline())
            );

            int finalColumn = spaceLocation == Space.Location.BLOCK_END ?
                    column + style.getIndentSize() : column;
            String lastIndent = space.getWhitespace().substring(space.getWhitespace().lastIndexOf('\n') + 1);
            int indent = getLengthOfWhitespace(StringUtils.indent(lastIndent));

            if (indent != finalColumn) {
                if (hasFileLeadingComment || whitespace.contains("\n") &&
                        // Do not shift single line comments at col 0.
                        !(!s.getComments().isEmpty() && s.getComments().get(0) instanceof TextComment &&
                        !s.getComments().get(0).isMultiline() && getLengthOfWhitespace(s.getWhitespace()) == 0)) {
                    int shift = finalColumn - indent;
                    s = s.withWhitespace(whitespace.substring(0, whitespace.lastIndexOf('\n') + 1) +
                            indent(lastIndent, shift));
                }

                Space finalSpace = s;
                int lastCommentPos = s.getComments().size() - 1;
                s = s.withComments(ListUtils.map(s.getComments(), (i, c) -> {
                    if (c instanceof TextComment && !c.isMultiline()) {
                        // Do not shift single line comments at col 0.
                        if ((i != lastCommentPos) && getLengthOfWhitespace(c.getSuffix()) == 0) {
                            return c;
                        }
                    }
                    String priorSuffix = i == 0 ?
                            space.getWhitespace() :
                            finalSpace.getComments().get(i - 1).getSuffix();

                    int toColumn = spaceLocation == Space.Location.BLOCK_END && i != finalSpace.getComments().size() - 1 ?
                            column + style.getIndentSize() :
                            column;

                    Comment c2 = c;
                    if (priorSuffix.contains("\n") || hasFileLeadingComment) {
                        c2 = indentComment(c, priorSuffix, toColumn);
                    }

                    if (c2.getSuffix().contains("\n")) {
                        int suffixIndent = getLengthOfWhitespace(c2.getSuffix());
                        int shift = toColumn - suffixIndent;
                        c2 = c2.withSuffix(indent(c2.getSuffix(), shift));
                    }

                    return c2;
                }));
            }
        }

        return s;
    }

    private Comment indentComment(Comment comment, String priorSuffix, int column) {
        if (comment instanceof TextComment) {
            TextComment textComment = (TextComment) comment;
            String text = textComment.getText();
            if (!text.contains("\n")) {
                return comment;
            }

            // the margin is the baseline for how much we should shift left or right
            String margin = StringUtils.commonMargin(null, priorSuffix);

            int indent = getLengthOfWhitespace(margin);
            int shift = column - indent;

            if (shift > 0) {
                String newMargin = indent(margin, shift);
                if (textComment.isMultiline()) {
                    StringBuilder multiline = new StringBuilder();
                    for (int i = 0; i < text.length(); i++) {
                        char c = text.charAt(i);
                        if (c == '\n') {
                            multiline.append(c).append(newMargin);
                            i += margin.length();
                        } else {
                            multiline.append(c);
                        }
                    }
                    return textComment.withText(multiline.toString());
                }
            } else if (shift < 0) {
                if (textComment.isMultiline()) {
                    StringBuilder multiline = new StringBuilder();
                    for (int i = 0; i < text.length(); i++) {
                        char c = text.charAt(i);
                        if (c == '\n') {
                            multiline.append(c);
                            for (int j = 0; j < Math.abs(shift) && i+j+1 < text.length() && (text.charAt(j + i + 1) == ' ' || text.charAt(j + i + 1) == '\t'); j++) {
                                i++;
                            }
                        } else {
                            multiline.append(c);
                        }
                    }
                    return textComment.withText(multiline.toString());
                }
            } else {
                return textComment;
            }
        } else if (comment instanceof Javadoc.DocComment) {
            final Javadoc.DocComment docComment = (Javadoc.DocComment) comment;
            return docComment.withBody(ListUtils.map(docComment.getBody(), (i, jdoc) -> {
                if(!(jdoc instanceof Javadoc.LineBreak)) {
                    return jdoc;
                }
                Javadoc.LineBreak lineBreak = (Javadoc.LineBreak) jdoc;
                String linebreak;
                if(lineBreak.getMargin().charAt(0) == '\r') {
                    linebreak = "\r\n";
                } else {
                    linebreak = "\n";
                }
                return lineBreak.withMargin(lineBreak.getMargin().replaceAll("^\\s+", indent(linebreak, column + 1)));
            }));
        }

        return comment;
    }

    private String indent(String whitespace, int shift) {
        StringBuilder newWhitespace = new StringBuilder(whitespace);
        shift(newWhitespace, shift);
        return newWhitespace.toString();
    }

    private void shift(StringBuilder text, int shift) {
        int tabIndent = style.getTabSize();
        if (!style.getUseTabCharacter()) {
            tabIndent = Integer.MAX_VALUE;
        }

        if (shift > 0) {
            for (int i = 0; i < shift / tabIndent; i++) {
                text.append('\t');
            }

            for (int i = 0; i < shift % tabIndent; i++) {
                text.append(' ');
            }
        } else {
            int len;
            if (style.getUseTabCharacter()) {
                len = text.length() + (shift / tabIndent);
            } else {
                len = text.length() + shift;
            }
            if (len >= 0) {
                text.delete(len, text.length());
            }
        }
    }

    private int findIndent(Space space) {
        String indent = space.getIndent();
        return getLengthOfWhitespace(indent);
    }

    private int getLengthOfWhitespace(@Nullable String whitespace) {
        if (whitespace == null) {
            return 0;
        }

        int size = 0;
        for (int i = 0; i < whitespace.length(); i++) {
            char c = whitespace.charAt(i);
            size += c == '\t' ? style.getTabSize() : 1;
            if (c == '\n' || c == '\r') {
                size = 0;
            }
        }
        return size;
    }

    private int forInitColumn() {
        Cursor forCursor = getCursor().dropParentUntil(J.ForLoop.class::isInstance);
        J.ForLoop forLoop = forCursor.getValue();
        Object parent = forCursor.getParentOrThrow().getValue();
        @SuppressWarnings("ConstantConditions") J alignTo = parent instanceof J.Label ?
                ((J.Label) parent).withStatement(forLoop.withBody(null)) :
                forLoop.withBody(null);

        int column = 0;
        boolean afterInitStart = false;
        String print = alignTo.print(getCursor());
        for (int i = 0; i < print.length(); i++) {
            char c = print.charAt(i);
            if (c == '(') {
                afterInitStart = true;
            } else if (afterInitStart && !Character.isWhitespace(c)) {
                return column - 1;
            }
            column++;
        }
        throw new IllegalStateException("For loops must have a control section");
    }

    @Override
    public @Nullable J postVisit(J tree, P p) {
        if (stopAfter != null && stopAfter.isScope(tree)) {
            getCursor().putMessageOnFirstEnclosing(JavaSourceFile.class, "stop", true);
        }
        return super.postVisit(tree, p);
    }

    @Override
    public @Nullable J visit(@Nullable Tree tree, P p) {
        if (getCursor().getNearestMessage("stop") != null) {
            return (J) tree;
        }
        return super.visit(tree, p);
    }

    private enum IndentType {
        ALIGN,
        INDENT,
        CONTINUATION_INDENT
    }
}
