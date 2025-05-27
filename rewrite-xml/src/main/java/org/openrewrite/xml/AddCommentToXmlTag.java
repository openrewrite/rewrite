/*
 * Copyright 2022 the original author or authors.
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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;
import java.util.List;

import static org.openrewrite.Tree.randomId;

@Incubating(since = "7.24.0")
@Value
@EqualsAndHashCode(callSuper = false)
public class AddCommentToXmlTag extends Recipe {

    @Option(displayName = "XPath",
            description = "An XPath expression used to find matching tags.",
            example = "/project/dependencies/dependency")
    String xPath;

    @Option(displayName = "Comment text",
            description = "The text to add as a comment..",
            example = "This is excluded due to CVE <X> and will be removed when we upgrade the next version is available.")
    String commentText;

    @Override
    public String getDisplayName() {
        return "Add a comment to an XML tag";
    }

    @Override
    public String getDescription() {
        return "Adds a comment as the first element in an XML tag.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new XmlVisitor<ExecutionContext>() {
            final XPathMatcher matcher = new XPathMatcher(xPath);

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = (Xml.Tag) super.visitTag(tag, ctx);
                if (matcher.matches(getCursor())) {
                    if (tag.getContent() != null) {
                        List<Content> contents = new ArrayList<>(tag.getContent());
                        boolean containsComment = contents.stream()
                                .anyMatch(c -> c instanceof Xml.Comment &&
                                        commentText.equals(((Xml.Comment) c).getText()));
                        if (!containsComment) {
                            int insertPos = 0;
                            Xml.Comment customComment = new Xml.Comment(randomId(),
                                    contents.get(insertPos).getPrefix(),
                                    Markers.EMPTY,
                                    commentText);
                            contents.add(insertPos, customComment);
                            t = t.withContent(contents);
                        }
                    }
                }
                return t;
            }
        };
    }
}
