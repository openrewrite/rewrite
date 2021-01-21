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

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeProcessor;
import org.openrewrite.Validated;
import org.openrewrite.xml.ChangeTagValueProcessor;
import org.openrewrite.xml.tree.Xml;

import static org.openrewrite.Validated.required;

@Data
@EqualsAndHashCode(callSuper = true)
public class ChangePropertyValue extends Recipe {
    private final String key;
    private final String toValue;

    @Override
    protected TreeProcessor<?, ExecutionContext> getProcessor() {
        return new ChangePropertyValueProcessor();
    }

    public ChangePropertyValue(String key, String toValue) {
        //Customizing lombok constructor to replace the property markers.
        this.key = key.replace("${", "").replace("}", "");
        this.toValue = toValue;
    }


    @Override
    public Validated validate() {
        return required("key", key)
                .and(required("toValue", toValue));
    }

    private class ChangePropertyValueProcessor extends MavenProcessor<ExecutionContext> {

        public ChangePropertyValueProcessor() {
            setCursoringOn();
        }

        @Override
        public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
            if (isPropertyTag() && key.equals(tag.getName()) &&
                    !toValue.equals(tag.getValue().orElse(null))) {
                doAfterVisit(new ChangeTagValueProcessor<>(tag, toValue));
            }
            return super.visitTag(tag, ctx);
        }
    }
}
