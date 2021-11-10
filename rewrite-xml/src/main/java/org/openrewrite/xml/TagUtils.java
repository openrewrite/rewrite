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

import org.openrewrite.Cursor;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Provides convenience methods for manipulating Xml tags.
 */
public final class TagUtils {
    private TagUtils() {
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
     * Transform the children of a tag with the supplied mapping function.
     *
     * @param parent the tag whose direct child elements are to be transformed by 'map'.
     * @param map the function used to transform the direct child elements of 'parent'.
     * @return 'parent' with its children transformed by 'map'
     */
    public static Xml.Tag mapChildren(Xml.Tag parent, UnaryOperator<Content> map) {
        return mapChildren(parent, parent, map);
    }

    /**
     * Transform the children of a tag with the supplied mapping function.
     *
     * @param parentScope a tag which contains 'parent' as a direct or transitive child element.
     * @param parent the tag whose direct child elements are to be transformed by 'map'.
     * @param map the function used to transform the direct child elements of 'parent'.
     * @return 'parentScope' which somewhere contains 'parent' with its children transformed by 'map'
     */
    public static Xml.Tag mapChildren(Xml.Tag parentScope, Xml.Tag parent, UnaryOperator<Content> map) {
        //noinspection ConstantConditions
        return (Xml.Tag) new MapTagChildrenVisitor<Void>(parent, map).visitNonNull(parentScope, null);
    }

    /**
     * Add a tag to the children of another tag
     *
     * @param parent the tag that will have 'newChild' added to its children
     * @param newChild the tag to add as a child of 'parent'
     * @param parentCursor A cursor pointing one level above 'parent'. Determines the final indentation of 'newChild'.
     *
     * @return 'parent' with 'newChild' amongst its child elements
     */
    public static Xml.Tag addToTag(Xml.Tag parent, Xml.Tag newChild, Cursor parentCursor) {
        return addToTag(parent, parent, newChild, parentCursor);
    }

    /**
     * Add a tag to the children of another tag
     *
     * @param parentScope a tag which contains 'parent' as a direct or transitive child element.
     * @param parent the tag that will have 'newChild' added to its children
     * @param newChild the tag to add as a child of 'parent'
     * @param parentCursor A cursor pointing one level above 'parentScope'. Determines the final indentation of 'newChild'.
     *
     * @return 'parentScope' which somewhere contains 'parent' with 'newChild' amongst its child elements
     */
    public static Xml.Tag addToTag(Xml.Tag parentScope, Xml.Tag parent, Xml.Tag newChild, Cursor parentCursor) {
        //noinspection ConstantConditions
        return (Xml.Tag) new AddToTagVisitor<Void>(parent, newChild)
                .visitNonNull(parentScope, null, parentCursor);
    }

    /**
     * Determines whether two tags have effectively the same contents.
     *
     * @return true if the two tags have the same name, attributes (regardless of order), and children (considering order)
     */
    public static boolean semanticallyEqual(Xml first, Xml second) {
        return SemanticallyEqual.areEqual(first, second);
    }
}
