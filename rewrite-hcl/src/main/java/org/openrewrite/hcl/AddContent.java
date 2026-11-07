/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.hcl;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.hcl.format.AttributeSpaceVisitor;
import org.openrewrite.hcl.style.SpacesStyle;
import org.openrewrite.hcl.style.TabsAndIndentsStyle;
import org.openrewrite.hcl.tree.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.marker.Markers;
import org.openrewrite.style.Style;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddContent extends Recipe {
    private static final Pattern LINE_START = Pattern.compile("(\\r?\\n)(?![\\r\\n])");

    @Option(displayName = "Block path",
            description = "A JSONPath expression selecting the blocks to add `content` to. `$` selects the top level " +
                          "of the file, e.g. of a `.tfvars` file. A path selecting an attribute whose value is an " +
                          "object, such as `$.resource.aws_instance.tags`, adds `content` to that object.",
            example = "$.resource.aws_db_instance")
    String blockPath;

    @Option(displayName = "Content",
            description = "The HCL source of exactly one attribute, or of one block with its body.",
            example = "publicly_accessible = false")
    String content;

    @Option(displayName = "Prepend",
            description = "If `true`, `content` is added as the first element instead of the last one.",
            required = false)
    @Nullable
    Boolean prepend;

    String displayName = "Add content";

    String description = "Add an attribute or a block to HCL blocks selected by a JSONPath expression, unless an " +
                         "attribute with the same name or a block with the same type and labels is already there. " +
                         "Existing values are left unchanged, use `ChangeValue` to change them.";

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s` to `%s`", content, blockPath);
    }

    @Override
    public Validated<Object> validate() {
        return super.validate().and(Validated.test("content", "must be exactly one HCL attribute or block", content,
                c -> c == null || parseBodyContent(c) != null || parseObjectElement(c) != null));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        boolean root = "$".equals(blockPath.trim());
        JsonPathMatcher matcher = new JsonPathMatcher(blockPath);
        BodyContent bodyContent = parseBodyContent(content);
        Hcl.Attribute objectElement = bodyContent instanceof Hcl.Attribute ?
                (Hcl.Attribute) bodyContent : parseObjectElement(content);
        boolean first = Boolean.TRUE.equals(prepend);
        return new HclIsoVisitor<ExecutionContext>() {
            @Override
            public Hcl.ConfigFile visitConfigFile(Hcl.ConfigFile configFile, ExecutionContext ctx) {
                Hcl.ConfigFile c = super.visitConfigFile(configFile, ctx);
                if (!root || bodyContent == null || isPresent(c.getBody(), bodyContent)) {
                    return c;
                }
                BodyContent added = copy(bodyContent, "");
                List<BodyContent> body = c.getBody();
                Space eof = c.getEof();
                if (body.isEmpty()) {
                    Space prefix = c.getPrefix();
                    if (!prefix.getComments().isEmpty() && !prefix.getLastWhitespace().contains("\n")) {
                        added = startingWith(added, "\n");
                    }
                    body = singletonList(added);
                } else if (first) {
                    body = ListUtils.concat(added, ListUtils.mapFirst(body, e -> {
                        String whitespace = e.getPrefix().getWhitespace();
                        return whitespace.contains("\n") ? e : startingWith(e, "\n" + whitespace);
                    }));
                } else {
                    Space[] split = splitSameLineComments(eof);
                    added = startingWith(added, "\n");
                    added = added.withPrefix(join(split[0], added.getPrefix()));
                    eof = split[1];
                    body = ListUtils.concat(body, added);
                }
                Hcl.Block before = topLevel(c.getBody());
                Hcl.Block after = topLevel(body);
                return c.withBody(realign(getCursor(), before, before.getBody(), after, body, ctx)).withEof(eof);
            }

            @Override
            public Hcl.Block visitBlock(Hcl.Block block, ExecutionContext ctx) {
                Hcl.Block b = super.visitBlock(block, ctx);
                if (root || bodyContent == null || isPresent(b.getBody(), bodyContent) ||
                    !matcher.matches(getCursor())) {
                    return b;
                }
                String blockIndent = indentOf(b.getPrefix());
                String indent = siblingIndent(b.getBody(), first, blockIndent + indentUnit());
                BodyContent added = startingWith(copy(bodyContent, indent), "\n" + indent);
                List<BodyContent> body = b.getBody();
                Space end = b.getEnd();
                if (first && !body.isEmpty()) {
                    Space firstPrefix = body.get(0).getPrefix();
                    if (firstPrefix.getLastWhitespace().contains("\n")) {
                        Space[] split = splitSameLineComments(firstPrefix);
                        added = added.withPrefix(join(split[0], added.getPrefix()));
                        body = ListUtils.mapFirst(body, e -> e.withPrefix(split[1]));
                    }
                } else {
                    Space[] split = splitSameLineComments(end);
                    added = added.withPrefix(join(split[0], added.getPrefix()));
                    end = split[1];
                }
                body = ListUtils.map(body, e -> onOwnLine(e, indent));
                body = first ? ListUtils.concat(added, body) : ListUtils.concat(body, added);
                Hcl.Block after = b.withBody(body).withEnd(onOwnLine(end, blockIndent));
                return after.withBody(realign(getCursor().getParentOrThrow(), b, b.getBody(), after, body, ctx));
            }

            @Override
            public Hcl.Attribute visitAttribute(Hcl.Attribute attribute, ExecutionContext ctx) {
                Hcl.Attribute a = super.visitAttribute(attribute, ctx);
                if (root || objectElement == null || !(a.getValue() instanceof Hcl.ObjectValue)) {
                    return a;
                }
                Hcl.ObjectValue o = (Hcl.ObjectValue) a.getValue();
                if (isPresent(o.getAttributes(), objectElement) || !matcher.matches(getCursor())) {
                    return a;
                }
                Hcl.ObjectValue after = addToObject(o, indentOf(a.getPrefix()));
                if (!isMultiline(after)) {
                    return a.withValue(after);
                }
                return a.withValue(after.withArguments(realign(getCursor(), o, o.getAttributes(), after,
                        after.getAttributes(), ctx)));
            }

            private Hcl.ObjectValue addToObject(Hcl.ObjectValue o, String objectIndent) {
                List<HclRightPadded<Expression>> elements = o.getPadding().getAttributes().getPadding().getElements();
                List<Expression> attributes = o.getAttributes();
                if (attributes.size() == 1 && attributes.get(0) instanceof Hcl.Empty) {
                    String indent = objectIndent + indentUnit();
                    Hcl.Attribute added = startingWith(copy(objectElement, indent), "\n" + indent);
                    Space[] split = splitSameLineComments(attributes.get(0).getPrefix());
                    added = added.withPrefix(join(split[0], added.getPrefix()));
                    Space end = split[1].getComments().isEmpty() ?
                            Space.format("\n" + objectIndent) : onOwnLine(split[1], objectIndent);
                    return withElements(o, singletonList(new HclRightPadded<>(added, end, Markers.EMPTY)));
                }
                HclRightPadded<Expression> neighbor = elements.get(first ? 0 : elements.size() - 1);
                if (!(neighbor.getElement() instanceof Hcl.Attribute)) {
                    return o;
                }
                Hcl.Attribute neighborAttribute = (Hcl.Attribute) neighbor.getElement();
                boolean multiline = isMultiline(o);
                Hcl.Attribute added;
                if (multiline) {
                    String indent = siblingIndent(attributes, first, objectIndent + indentUnit());
                    added = startingWith(copy(objectElement, indent), "\n" + indent)
                            .withComma(neighborAttribute.getComma() == null ? null : comma());
                } else {
                    added = startingWith(copy(objectElement, ""),
                            first ? neighborAttribute.getPrefix().getWhitespace() : " ");
                }
                List<HclRightPadded<Expression>> padded = new ArrayList<>(elements);
                if (first) {
                    if (!multiline) {
                        added = added.withComma(comma());
                        if (neighborAttribute.getPrefix().isEmpty()) {
                            neighborAttribute = neighborAttribute.withPrefix(Space.format(" "));
                        }
                    } else if (neighborAttribute.getPrefix().getLastWhitespace().contains("\n")) {
                        Space[] split = splitSameLineComments(neighborAttribute.getPrefix());
                        added = added.withPrefix(join(split[0], added.getPrefix()));
                        neighborAttribute = neighborAttribute.withPrefix(split[1]);
                    }
                    padded.set(0, neighbor.withElement(neighborAttribute));
                    padded.add(0, new HclRightPadded<>(added, Space.EMPTY, Markers.EMPTY));
                } else {
                    Space after = neighbor.getAfter();
                    if (!multiline) {
                        if (neighborAttribute.getComma() == null) {
                            neighborAttribute = neighborAttribute.withComma(comma());
                        } else {
                            added = added.withComma(comma());
                        }
                    } else {
                        Space[] split = splitSameLineComments(after);
                        added = added.withPrefix(join(split[0], added.getPrefix()));
                        after = split[1];
                    }
                    padded.set(padded.size() - 1, neighbor.withElement(neighborAttribute).withAfter(Space.EMPTY));
                    padded.add(new HclRightPadded<>(added, after, Markers.EMPTY));
                }
                return withElements(o, padded);
            }

            private String indentUnit() {
                TabsAndIndentsStyle style = Style.from(TabsAndIndentsStyle.class,
                        getCursor().firstEnclosingOrThrow(Hcl.ConfigFile.class), () -> TabsAndIndentsStyle.DEFAULT);
                return Boolean.TRUE.equals(style.getUseTabCharacter()) ?
                        "\t" : StringUtils.repeat(" ", style.getIndentSize());
            }

            @SuppressWarnings("unchecked")
            private <T extends Hcl> List<T> realign(Cursor parentCursor, Hcl parentBefore, List<T> before,
                                                    Hcl parentAfter, List<T> after, ExecutionContext ctx) {
                SpacesStyle style = Style.from(SpacesStyle.class,
                        getCursor().firstEnclosingOrThrow(Hcl.ConfigFile.class), () -> SpacesStyle.DEFAULT);
                AttributeSpaceVisitor<ExecutionContext> realigner = new AttributeSpaceVisitor<ExecutionContext>(style) {
                    @Override
                    public Hcl.ObjectValue visitObjectValue(Hcl.ObjectValue objectValue, ExecutionContext ctx) {
                        return objectValue;
                    }
                };
                Cursor cursorBefore = new Cursor(parentCursor, parentBefore);
                Cursor cursorAfter = new Cursor(parentCursor, parentAfter);
                Set<UUID> existing = new HashSet<>();
                Set<UUID> aligned = new HashSet<>();
                boolean allAligned = true;
                for (T original : before) {
                    existing.add(original.getId());
                    if (original instanceof Hcl.Attribute) {
                        Hcl realigned = realigner.visitNonNull(original, ctx, cursorBefore);
                        if (typeSpace(realigned).equals(typeSpace(original))) {
                            aligned.add(original.getId());
                        } else {
                            allAligned = false;
                        }
                    }
                }
                boolean alignAdded = allAligned;
                return ListUtils.map(after, e -> {
                    if (!(e instanceof Hcl.Attribute)) {
                        return e;
                    }
                    boolean isAdded = !existing.contains(e.getId());
                    if (isAdded ? alignAdded : aligned.contains(e.getId())) {
                        return (T) realigner.visitNonNull(e, ctx, cursorAfter);
                    }
                    if (isAdded) {
                        Hcl.Attribute a = (Hcl.Attribute) e;
                        HclLeftPadded<Hcl.Attribute.Type> type = a.getPadding().getType();
                        return (T) a.getPadding().withType(type.withBefore(type.getBefore().withWhitespace(" ")));
                    }
                    return e;
                });
            }
        };
    }

    private static boolean isMultiline(Hcl.ObjectValue o) {
        List<HclRightPadded<Expression>> elements = o.getPadding().getAttributes().getPadding().getElements();
        return elements.get(elements.size() - 1).getAfter().getLastWhitespace().contains("\n");
    }

    private static Hcl.ObjectValue withElements(Hcl.ObjectValue o, List<HclRightPadded<Expression>> elements) {
        HclContainer<Expression> container = o.getPadding().getAttributes();
        return o.getPadding().withAttributes(container.getPadding().withElements(elements));
    }

    private static Hcl.Empty comma() {
        return new Hcl.Empty(Tree.randomId(), Space.EMPTY, Markers.EMPTY);
    }

    private static Hcl.Block topLevel(List<BodyContent> body) {
        return new Hcl.Block(Tree.randomId(), Space.EMPTY, Markers.EMPTY, null, emptyList(), Space.EMPTY, body,
                Space.EMPTY);
    }

    private static String typeSpace(Hcl attribute) {
        return ((Hcl.Attribute) attribute).getPadding().getType().getBefore().getWhitespace();
    }

    private static boolean isPresent(List<? extends Hcl> siblings, Hcl added) {
        for (Hcl sibling : siblings) {
            if (added instanceof Hcl.Attribute) {
                String name = ((Hcl.Attribute) added).getSimpleName();
                if (sibling instanceof Hcl.Attribute && name.equals(((Hcl.Attribute) sibling).getSimpleName()) ||
                    sibling instanceof Hcl.Block && name.equals(((Hcl.Block) sibling).getSimpleName())) {
                    return true;
                }
            } else if (added instanceof Hcl.Block) {
                Hcl.Block block = (Hcl.Block) added;
                String type = block.getSimpleName();
                if (sibling instanceof Hcl.Attribute && block.getLabels().isEmpty() &&
                    Objects.equals(type, ((Hcl.Attribute) sibling).getSimpleName()) ||
                    sibling instanceof Hcl.Block && Objects.equals(type, ((Hcl.Block) sibling).getSimpleName()) &&
                    labelNames(block).equals(labelNames((Hcl.Block) sibling))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static List<String> labelNames(Hcl.Block block) {
        List<String> names = new ArrayList<>();
        for (Label label : block.getLabels()) {
            names.add(label.getSimpleName());
        }
        return names;
    }

    private static String indentOf(Space space) {
        return space.getLastWhitespace().contains("\n") ? space.getIndent() : "";
    }

    private static String siblingIndent(List<? extends Hcl> siblings, boolean first, String fallback) {
        for (int i = 0; i < siblings.size(); i++) {
            Space prefix = siblings.get(first ? i : siblings.size() - 1 - i).getPrefix();
            if (prefix.getLastWhitespace().contains("\n")) {
                return prefix.getIndent();
            }
        }
        return fallback;
    }

    private static <T extends Hcl> T startingWith(T tree, String whitespace) {
        return tree.withPrefix(tree.getPrefix().withWhitespace(whitespace));
    }

    private static Space[] splitSameLineComments(Space space) {
        List<Comment> comments = space.getComments();
        if (comments.isEmpty() || space.getWhitespace().contains("\n")) {
            return new Space[]{Space.EMPTY, space};
        }
        int last = 0;
        while (last < comments.size() - 1 && !comments.get(last).getSuffix().contains("\n")) {
            last++;
        }
        Comment lastComment = comments.get(last);
        List<Comment> sameLine = new ArrayList<>(comments.subList(0, last));
        sameLine.add(lastComment.withSuffix(""));
        return new Space[]{
                Space.build(space.getWhitespace(), sameLine),
                Space.build(lastComment.getSuffix(), new ArrayList<>(comments.subList(last + 1, comments.size())))
        };
    }

    private static Space join(Space sameLine, Space prefix) {
        if (sameLine.getComments().isEmpty()) {
            return prefix;
        }
        return Space.build(sameLine.getWhitespace(), ListUtils.concatAll(
                ListUtils.mapLast(sameLine.getComments(), c -> c.withSuffix(prefix.getWhitespace())),
                prefix.getComments()));
    }

    private static <T extends Hcl> T onOwnLine(T tree, String indent) {
        return tree.withPrefix(onOwnLine(tree.getPrefix(), indent));
    }

    private static Space onOwnLine(Space space, String indent) {
        if (space.getLastWhitespace().contains("\n")) {
            return space;
        }
        List<Comment> comments = space.getComments();
        if (comments.isEmpty()) {
            return space.withWhitespace("\n" + indent);
        }
        return space.withComments(ListUtils.mapLast(comments, c -> c.withSuffix("\n" + indent)));
    }

    @SuppressWarnings("unchecked")
    private static <T extends Hcl> T copy(T content, String indent) {
        Hcl copy = new RandomizeIdVisitor<Integer>().visitNonNull(content, 0);
        return (T) new HclIsoVisitor<Integer>() {
            @Override
            public Space visitSpace(Space space, Space.Location loc, Integer p) {
                if (indent.isEmpty()) {
                    return space;
                }
                return space.withWhitespace(indented(space.getWhitespace(), indent))
                        .withComments(ListUtils.map(space.getComments(),
                                c -> c.withSuffix(indented(c.getSuffix(), indent))));
            }

            @Override
            public Hcl.HeredocTemplate visitHeredocTemplate(Hcl.HeredocTemplate heredocTemplate, Integer p) {
                return heredocTemplate;
            }
        }.visitNonNull(copy, 0);
    }

    private static String indented(String whitespace, String indent) {
        return LINE_START.matcher(whitespace).replaceAll("$1" + indent);
    }

    private static @Nullable BodyContent parseBodyContent(String content) {
        return parse(content.trim())
                .filter(c -> c.getBody().size() == 1)
                .map(c -> {
                    BodyContent b = c.getBody().get(0);
                    return b.getPrefix().getComments().isEmpty() ? b.<BodyContent>withPrefix(c.getPrefix()) : b;
                })
                .filter(b -> b instanceof Hcl.Attribute || b instanceof Hcl.Block && ((Hcl.Block) b).getType() != null)
                .orElse(null);
    }

    private static Hcl.@Nullable Attribute parseObjectElement(String content) {
        return parse("object = {\n" + content.trim() + "\n}")
                .filter(c -> c.getBody().size() == 1 && c.getBody().get(0) instanceof Hcl.Attribute)
                .map(c -> ((Hcl.Attribute) c.getBody().get(0)).getValue())
                .filter(Hcl.ObjectValue.class::isInstance)
                .map(o -> ((Hcl.ObjectValue) o).getPadding().getAttributes().getPadding().getElements())
                .filter(elements -> elements.size() == 1 && elements.get(0).getElement() instanceof Hcl.Attribute &&
                                    elements.get(0).getAfter().getComments().isEmpty())
                .map(elements -> startingWith((Hcl.Attribute) elements.get(0).getElement(), "").withComma(null))
                .orElse(null);
    }

    private static Optional<Hcl.ConfigFile> parse(String source) {
        return HclParser.builder().build()
                .parse(new InMemoryExecutionContext(), source + "\n")
                .filter(Hcl.ConfigFile.class::isInstance)
                .map(Hcl.ConfigFile.class::cast)
                .filter(c -> c.getEof().getWhitespace().trim().isEmpty() && c.getEof().getComments().isEmpty() &&
                             !hasMissingValue(c))
                .findFirst();
    }

    @SuppressWarnings("ConstantValue")
    private static boolean hasMissingValue(Hcl.ConfigFile configFile) {
        return new HclIsoVisitor<AtomicBoolean>() {
            @Override
            public Hcl.Attribute visitAttribute(Hcl.Attribute attribute, AtomicBoolean missing) {
                if (attribute.getValue() == null) {
                    missing.set(true);
                    return attribute;
                }
                return super.visitAttribute(attribute, missing);
            }
        }.reduce(configFile, new AtomicBoolean()).get();
    }
}
