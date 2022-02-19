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
package org.openrewrite.xml;

import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.Comparator;

/**
 * Helps to add a {@link Xml.Tag} in alphabetical order while preserving the position of non-tag {@link Content}.
 * I.E. {@link Xml.Comment} or {@link Xml.ProcessingInstruction}
 */
public class TagNameComparator implements Comparator<Content> {

    @Override
    public int compare(Content c1, Content c2) {
        if (!(c1 instanceof Xml.Tag) || !(c2 instanceof Xml.Tag)) {
            return 1;
        }

        return ((Xml.Tag) c1).getName().compareTo(((Xml.Tag) c2).getName());
    }
}
