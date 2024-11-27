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
package org.openrewrite.properties;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.marker.Markers;
import org.openrewrite.properties.search.FindProperties;
import org.openrewrite.properties.tree.Properties;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddPropertyComment extends Recipe {

    @Option(displayName = "Property key",
            description = "The name of the property key whose value is to be changed. Supports glob patterns.",
            example = "management.metrics.binders.*.enabled")
    String propertyKey;

    @Option(example = "comment", displayName = "Comment",
            description = "The inline comment to be added.")
    String comment;

    @Override
    public String getDisplayName() {
        return "Add comment";
    }

    @Override
    public String getDescription() {
        return "Add comment leaving the key and value intact.";
    }

    @Override
    public PropertiesIsoVisitor<ExecutionContext> getVisitor() {
        return new PropertiesIsoVisitor<ExecutionContext>() {
            @Override
            public Properties.File visitFile(Properties.File file, ExecutionContext ctx) {
                Properties.File p = super.visitFile(file, ctx);
                if (StringUtils.isBlank(propertyKey) || StringUtils.isBlank(comment)) {
                    return p;
                }
                Set<Properties.Entry> properties = FindProperties.find(p, propertyKey, false);
                if (properties.isEmpty()) {
                    return p;
                }

                List<Properties.Content> newContentList = new ArrayList<>(p.getContent().size() + 1);
                Properties.Content previousContent = null;
                for(Properties.Content c : p.getContent()) {
                    Properties.Content currentContent = c;
                    if ((c instanceof Properties.Entry)
                            && ((Properties.Entry) c).getKey().equals(propertyKey)
                            && !isCommentAlreadyPresent(previousContent, comment)) {
                        Properties.Comment commentContent = new Properties.Comment(
                                randomId(),
                                newContentList.isEmpty() ? "" : "\n",
                                Markers.EMPTY,
                                Properties.Comment.Delimiter.HASH_TAG,
                                " " + comment.trim());
                        newContentList.add(commentContent);
                        if (!c.getPrefix().contains("\n")) {
                            currentContent = (Properties.Content) c.withPrefix("\n" + c.getPrefix());
                        }
                    }
                    newContentList.add(currentContent);
                    previousContent = currentContent;
                }
                return (newContentList.size() > p.getContent().size()) ? p.withContent(newContentList) : p;
            }
        };
    }

    private boolean isCommentAlreadyPresent(Properties.Content previousContent, String comment) {
        return ((previousContent instanceof Properties.Comment)
                && ((Properties.Comment) previousContent).getMessage().contains(comment.trim()));
    }
}
