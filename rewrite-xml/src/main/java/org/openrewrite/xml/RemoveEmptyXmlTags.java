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
package org.openrewrite.xml;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Value
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor(force = true)
public class RemoveEmptyXmlTags extends Recipe {

    private static final Pattern PREDICATE_WITH_ATTR = Pattern.compile("\\[([^\\[\\]]*@[^\\[\\]]*)]");
    private static final Pattern ATTR_REFERENCE = Pattern.compile("@(\\*|[A-Za-z_][\\w.\\-]*)");

    @Option(displayName = "XPaths",
            description = "Whitelist of XPath expressions identifying empty tags eligible for removal. Attribute predicates " +
                          "enumerate the attributes a candidate tag is *allowed* to carry: a tag is a candidate when its " +
                          "attribute set is a subset of the union of attribute names appearing in matched predicates, with " +
                          "`@*` acting as a wildcard. " +
                          "Examples: `/server` matches only attribute-free `<server>` tags; `/server[@*]` also matches tags " +
                          "carrying any attributes; `/server[@description]` matches a `<server>` with no attributes or only " +
                          "a `description` attribute; `/server[@description or @other]` matches when attributes are a subset " +
                          "of `{description, other}`. When this list is omitted, every empty no-attribute tag is removed.",
            required = false,
            example = "/server/featureManager")
    @Nullable
    List<String> xPaths;

    @Option(displayName = "File matcher",
            description = "If provided only matching files will be modified. This is a glob expression.",
            required = false,
            example = "**/server.xml")
    @Nullable
    String fileMatcher;

    @Option(displayName = "Delete file if empty",
            description = "Delete the source file when the root tag has no remaining content after collapsing empty tags. Defaults to true.",
            required = false,
            example = "false")
    @Nullable
    Boolean deleteFileIfEmpty;

    String displayName = "Remove empty XML tags";

    String description = "Repeatedly removes empty XML tags (optionally scoped by an XPath whitelist) until the tree is stable, " +
                         "and optionally deletes the file when its root tag becomes empty. " +
                         "Useful as a follow-up to recipes that strip individual tags and leave empty containers behind.";

    @JsonCreator
    public RemoveEmptyXmlTags(@Nullable List<String> xPaths, @Nullable String fileMatcher, @Nullable Boolean deleteFileIfEmpty) {
        this.xPaths = xPaths;
        this.fileMatcher = fileMatcher;
        this.deleteFileIfEmpty = deleteFileIfEmpty;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TreeVisitor<?, ExecutionContext> repeat = Repeat.repeatUntilStable(() -> new TreeVisitor<Tree, ExecutionContext>() {
            final List<XPathRule> rules;
            {
                if (xPaths == null || xPaths.isEmpty()) {
                    rules = Collections.emptyList();
                } else {
                    rules = new ArrayList<>(xPaths.size());
                    for (String xPath : xPaths) {
                        rules.add(new XPathRule(xPath));
                    }
                }
            }

            @Override
            public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
                return sourceFile instanceof Xml.Document;
            }

            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof Xml.Document)) {
                    return tree;
                }
                Xml.Document doc = (Xml.Document) new XmlIsoVisitor<ExecutionContext>() {
                    @Override
                    public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext innerCtx) {
                        Xml.Tag t = super.visitTag(tag, innerCtx);
                        if (isRemovalCandidate(t, getCursor())) {
                            doAfterVisit(new RemoveContentVisitor<>(t, false, true));
                        }
                        return t;
                    }
                }.visitNonNull(tree, ctx);
                if (deleteFileIfEmpty == null || deleteFileIfEmpty) {
                    Xml.Tag root = doc.getRoot();
                    Cursor rootCursor = new Cursor(new Cursor(null, doc), root);
                    if (isRemovalCandidate(root, rootCursor)) {
                        return null;
                    }
                }
                return doc;
            }

            private boolean isRemovalCandidate(Xml.Tag t, Cursor cursor) {
                boolean empty = t.getContent() == null || t.getContent().isEmpty();
                if (!empty) {
                    return false;
                }
                if (rules.isEmpty()) {
                    return t.getAttributes().isEmpty();
                }
                for (XPathRule rule : rules) {
                    if (rule.matches(cursor, t)) {
                        return true;
                    }
                }
                return false;
            }
        }, 50);
        return fileMatcher == null ? repeat : Preconditions.check(new FindSourceFiles(fileMatcher), repeat);
    }

    private static final class XPathRule {
        final XPathMatcher pathMatcher;
        final Set<String> allowedAttributes;
        final boolean wildcardAttributes;

        XPathRule(String xPath) {
            Set<String> allowed = new HashSet<>();
            boolean wildcard = false;
            StringBuilder strippedPath = new StringBuilder();
            Matcher predicate = PREDICATE_WITH_ATTR.matcher(xPath);
            int lastEnd = 0;
            while (predicate.find()) {
                strippedPath.append(xPath, lastEnd, predicate.start());
                lastEnd = predicate.end();
                Matcher attrRef = ATTR_REFERENCE.matcher(predicate.group(1));
                while (attrRef.find()) {
                    String name = attrRef.group(1);
                    if ("*".equals(name)) {
                        wildcard = true;
                    } else {
                        allowed.add(name);
                    }
                }
            }
            strippedPath.append(xPath, lastEnd, xPath.length());
            this.pathMatcher = new XPathMatcher(strippedPath.toString());
            this.allowedAttributes = allowed;
            this.wildcardAttributes = wildcard;
        }

        boolean matches(Cursor cursor, Xml.Tag tag) {
            if (!pathMatcher.matches(cursor)) {
                return false;
            }
            if (wildcardAttributes) {
                return true;
            }
            for (Xml.Attribute attr : tag.getAttributes()) {
                if (!allowedAttributes.contains(attr.getKeyAsString())) {
                    return false;
                }
            }
            return true;
        }
    }
}
