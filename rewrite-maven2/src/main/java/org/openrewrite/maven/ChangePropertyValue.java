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
package org.openrewrite.maven;

import org.openrewrite.Validated;
import org.openrewrite.xml.ChangeTagValue;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import static org.openrewrite.Validated.required;

public class ChangePropertyValue extends MavenRefactorVisitor {
    private static final XPathMatcher propertyMatcher = new XPathMatcher("/project/properties/*");

    private String key;
    private String toValue;

    public ChangePropertyValue() {
        setCursoringOn();
    }

    public void setKey(String key) {
        this.key = key.replace("${", "").replace("}", "");
    }

    public void setToValue(String toValue) {
        this.toValue = toValue;
    }

    @Override
    public Validated validate() {
        return required("key", key)
                .and(required("toValue", toValue));
    }

    @Override
    public Xml visitTag(Xml.Tag tag) {
        if (propertyMatcher.matches(getCursor()) && key.equals(tag.getName()) &&
                !toValue.equals(tag.getValue().orElse(null))) {
            andThen(new ChangeTagValue.Scoped(tag, toValue));
        }
        return super.visitTag(tag);
    }
}
