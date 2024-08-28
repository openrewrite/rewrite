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

import lombok.AllArgsConstructor;
import org.openrewrite.Cursor;
import org.openrewrite.xml.tree.Xml;

import java.util.Optional;

import static org.openrewrite.xml.AddToTagVisitor.addToTag;
import static org.openrewrite.xml.MapTagChildrenVisitor.mapChildren;
import static org.openrewrite.xml.SemanticallyEqual.areEqual;


@AllArgsConstructor
public class AddOrUpdateChild<P> extends XmlVisitor<P> {
    Xml.Tag scope;
    Xml.Tag child;

    @Override
    public Xml visitTag(Xml.Tag tag, P p) {
        Xml.Tag t = (Xml.Tag) super.visitTag(tag, p);
        if (scope.isScope(tag)) {
            Optional<Xml.Tag> maybeChild = scope.getChild(child.getName());
            if (maybeChild.isPresent()) {
                if (areEqual(maybeChild.get(), child)) {
                    return t;
                }
                t = mapChildren(t, it -> {
                    if (it == maybeChild.get()) {
                        return autoFormat(child.withPrefix(maybeChild.get().getPrefix()), p, getCursor());
                    }
                    return it;
                });
            } else {
                t = addToTag(t, child, getCursor().getParentOrThrow());
            }
        }
        return t;
    }

    /**
     * Add the specified child tag to the parent tag's children. If a tag with the same name as the new child tag already
     * exists within the parent tag's children it is replaced. If no tag with the same name exists, the child tag is added.
     *
     * @param parent the tag to add 'child' to.
     * @param child the tag to add to the children of 'parent'.
     * @param parentCursor A cursor pointing one level above 'parent'. Determines the final indentation of 'child'.
     * @return 'parent' with 'child' among its direct child tags.
     */
    public static Xml.Tag addOrUpdateChild(Xml.Tag parent, Xml.Tag child, Cursor parentCursor) {
        return addOrUpdateChild(parent, parent, child, parentCursor);
    }

    /**
     * Add the specified child tag to the parent tag's children. If a tag with the same name as the new child tag already
     * exists within the parent tag's children it is replaced. If no tag with the same name exists, the child tag is added.
     *
     * @param parentScope a tag which contains 'parent' as a direct or transitive child element.
     * @param parent the tag to add 'child' to.
     * @param child the tag to add to the children of 'parent'.
     * @param parentCursor A cursor pointing one level above 'parent'. Determines the final indentation of 'child'.
     * @return 'parentScope' which somewhere contains 'parent' with 'child' among its direct child tags.
     */
    public static Xml.Tag addOrUpdateChild(Xml.Tag parentScope, Xml.Tag parent, Xml.Tag child, Cursor parentCursor) {
        //noinspection ConstantConditions
        return (Xml.Tag) new AddOrUpdateChild<Void>(parent, child).visitNonNull(parentScope, null, parentCursor);
    }
}
