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
package org.openrewrite.yaml;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.yaml.tree.Yaml;

import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class DeleteKey extends Recipe {
    @Option(displayName = "Key path",
            description = "A [JsonPath](https://docs.openrewrite.org/reference/jsonpath-and-jsonpathmatcher-reference) expression to locate a YAML entry.",
            example = "$.source.kind")
    String keyPath;


    @Option(displayName = "File pattern",
            description = "A glob expression representing a file path to search for (relative to the project root). Blank/null matches all.",
            required = false,
            example = ".github/workflows/*.yml")
    @Nullable
    String filePattern;

    String displayName = "Delete key";

    String description = "Delete a YAML mapping entry key.";

    @Override
    public Validated<Object> validate() {
        return super.validate().and(JsonPathMatcher.validate("keyPath", keyPath));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        JsonPathMatcher matcher = new JsonPathMatcher(keyPath);
        return Preconditions.check(new FindSourceFiles(filePattern), new YamlIsoVisitor<ExecutionContext>() {

            @Override
            public Yaml.Document visitDocument(Yaml.Document document, ExecutionContext ctx) {
                return TrailingComments.visit(getCursor(), () -> super.visitDocument(document, ctx));
            }

            @Override
            public Yaml.Sequence visitSequence(Yaml.Sequence sequence, ExecutionContext ctx) {
                Yaml.Sequence s = TrailingComments.visit(getCursor(), () -> super.visitSequence(sequence, ctx));
                if (s.getOpeningBracketPrefix() == null) {
                    List<Yaml.Sequence.Entry> kept = TrailingComments.remove(getCursor(), s.getEntries(), e ->
                            isEmptied(e.getBlock()) || matcher.matches(new Cursor(getCursor(), e)) ||
                            matcher.matches(new Cursor(getCursor(), e.getBlock())));
                    if (kept.size() == s.getEntries().size()) {
                        return s;
                    }
                    removeUnused(getCursor());
                    return s.withEntries(kept);
                }
                List<Yaml.Sequence.Entry> entries = s.getEntries();
                List<Yaml.Sequence.Entry> kept = ListUtils.filter(entries, e ->
                        !matcher.matches(new Cursor(getCursor(), e)) && !matcher.matches(new Cursor(getCursor(), e.getBlock())));
                if (kept.size() == entries.size()) {
                    return s;
                }
                removeUnused(getCursor());
                if (!kept.isEmpty()) {
                    Yaml.Sequence.Entry first = entries.get(0);
                    Yaml.Sequence.Entry last = entries.get(entries.size() - 1);
                    // the item now first takes the space after the opening bracket, and the one now last the comma
                    // the last had, if any
                    kept = ListUtils.mapFirst(kept, e -> e == first ? e : withLead(e, leadOf(first)));
                    kept = ListUtils.mapLast(kept, e -> e == last ? e : e.withTrailingCommaPrefix(last.getTrailingCommaPrefix()));
                }
                return s.withEntries(kept);
            }

            @Override
            public Yaml.Mapping visitMapping(Yaml.Mapping mapping, ExecutionContext ctx) {
                Yaml.Mapping m = TrailingComments.visit(getCursor(), () -> super.visitMapping(mapping, ctx));
                if (m.getOpeningBracePrefix() == null) {
                    List<Yaml.Mapping.Entry> kept = TrailingComments.remove(getCursor(), m.getEntries(), e ->
                            isEmptied(e.getValue()) || matcher.matches(new Cursor(getCursor(), e)));
                    if (kept.size() == m.getEntries().size()) {
                        return m;
                    }
                    removeUnused(getCursor().getParent());
                    return m.withEntries(kept);
                }
                List<Yaml.Mapping.Entry> entries = m.getEntries();
                List<Yaml.Mapping.Entry> kept = ListUtils.filter(entries, e -> !matcher.matches(new Cursor(getCursor(), e)));
                if (kept.size() == entries.size()) {
                    return m;
                }
                removeUnused(getCursor().getParent());
                Yaml.Mapping.Entry first = entries.get(0);
                // the entry now first takes the first's prefix, as the one it had starts with a comma
                return m.withEntries(ListUtils.mapFirst(kept, e -> e == first ? e : e.withPrefix(first.getPrefix())));
            }
        });
    }

    /**
     * The space before a flow sequence item. The parser keeps it on the item, or on the opening bracket of a nested
     * collection, and a tree from an older parser on the entry.
     */
    private static String leadOf(Yaml.Sequence.Entry entry) {
        Yaml.Block block = entry.getBlock();
        String lead = block instanceof Yaml.Sequence ? ((Yaml.Sequence) block).getOpeningBracketPrefix() :
                block instanceof Yaml.Mapping ? ((Yaml.Mapping) block).getOpeningBracePrefix() : block.getPrefix();
        return entry.getPrefix() + (lead == null ? "" : lead);
    }

    private static Yaml.Sequence.Entry withLead(Yaml.Sequence.Entry entry, String lead) {
        Yaml.Block block = entry.getBlock();
        if (block instanceof Yaml.Sequence && ((Yaml.Sequence) block).getOpeningBracketPrefix() != null) {
            block = ((Yaml.Sequence) block).withOpeningBracketPrefix(lead);
        } else if (block instanceof Yaml.Mapping && ((Yaml.Mapping) block).getOpeningBracePrefix() != null) {
            block = ((Yaml.Mapping) block).withOpeningBracePrefix(lead);
        } else if (block instanceof Yaml.Mapping) {
            block = ((Yaml.Mapping) block).withEntries(ListUtils.mapFirst(((Yaml.Mapping) block).getEntries(), e -> e.withPrefix(lead)));
        } else {
            block = block.withPrefix(lead);
        }
        return entry.withPrefix("").withBlock(block);
    }

    // Removed here rather than by RemoveUnusedVisitor, which would drop the comments its prefix holds. The parser
    // never makes a block collection without entries, so this one had them all deleted.
    private static boolean isEmptied(Yaml.Block block) {
        if (block instanceof Yaml.Mapping) {
            Yaml.Mapping mapping = (Yaml.Mapping) block;
            return mapping.getOpeningBracePrefix() == null && mapping.getEntries().isEmpty();
        }
        if (block instanceof Yaml.Sequence) {
            Yaml.Sequence sequence = (Yaml.Sequence) block;
            return sequence.getOpeningBracketPrefix() == null && sequence.getEntries().isEmpty();
        }
        return false;
    }
}
