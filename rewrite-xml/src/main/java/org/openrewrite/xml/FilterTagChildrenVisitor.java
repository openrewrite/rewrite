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
package org.openrewrite.xml;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.function.Predicate;

/**
 * Filter the children to only those matching the supplied predicate.
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class FilterTagChildrenVisitor<T> extends XmlVisitor<T> {
    public Xml.Tag scope;
    public Predicate<Content> childTest;

    @Override
    public Xml visitTag(Xml.Tag tag, T ctx) {
        Xml.Tag t = (Xml.Tag) super.visitTag(tag, ctx);
        if (scope.isScope(t)) {
            t = t.withContent(ListUtils.map(t.getContent(), it -> {
                if (childTest.test(it)) {
                    return it;
                }
                return null;
            }));
        }
        return t;
    }
}
