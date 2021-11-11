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

    /**
     * Filter the children of a tag to only those that match the supplied predicate.
     *
     * @param parent the tag whose direct child elements are to be filtered by 'childTest'
     * @param childTest the predicate used to evaluate the direct child elements of 'parent'.
     * @return 'parent' with its children that matched 'childTest'
     */
    public static Xml.Tag filterChildren(Xml.Tag parent, Predicate<Content> childTest) {
        return filterChildren(parent, parent, childTest);
    }

    /**
     * Filter the children of a tag to only those that match the supplied predicate.
     *
     * @param parentScope a tag which contains 'parent' as a direct or transitive child element.
     * @param parent the tag whose direct child elements are to be filtered by 'childTest'
     * @param childTest the predicate used to evaluate the direct child elements of 'parent'.
     * @return 'parentScope` which somewhere contains 'parent' with its children that matched 'childTest'
     */
    public static Xml.Tag filterChildren(Xml.Tag parentScope, Xml.Tag parent, Predicate<Content> childTest) {
        //noinspection ConstantConditions
        return (Xml.Tag) new FilterTagChildrenVisitor<Void>(parent, childTest)
                .visitNonNull(parentScope, null);
    }

    /**
     * Filter the children of a tag to only those that match the supplied predicate.
     * Non-tag children, such as comments, are untouched.
     *
     * @param parent the tag whose direct child elements are to be filtered by 'childTest'
     * @param childTest the predicate used to evaluate the direct child elements of 'parent'.
     * @return 'parent' with its children that matched 'childTest'
     */
    public static Xml.Tag filterTagChildren(Xml.Tag parent, Predicate<Xml.Tag> childTest) {
        return filterTagChildren(parent, parent, childTest);
    }

    /**
     * Filter the children of a tag to only those that match the supplied predicate.
     * Non-tag children, such as comments, are untouched.
     *
     * @param parentScope a tag which contains 'parent' as a direct or transitive child element.
     * @param parent the tag whose direct child elements are to be filtered by 'childTest'
     * @param childTest the predicate used to evaluate the direct child elements of 'parent'.
     * @return 'parentScope` which somewhere contains 'parent' with its children that matched 'childTest'
     */
    public static Xml.Tag filterTagChildren(Xml.Tag parentScope, Xml.Tag parent, Predicate<Xml.Tag> childTest) {
        return filterChildren(parentScope, parent, child -> {
            if(child instanceof Xml.Tag) {
                return childTest.test((Xml.Tag) child);
            }
            return true;
        });
    }
}
