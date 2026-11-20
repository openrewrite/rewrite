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
package org.openrewrite.hcl.tree;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.openrewrite.Incubating;
import org.openrewrite.hcl.internal.HclPrinter;

public interface Label extends Hcl {

    /**
     * @return The name of this label. Quoted labels like {@code "aws_instance"} yield their unquoted content.
     */
    @Incubating(since = "8.94.0")
    @JsonIgnore
    default String getSimpleName() {
        if (this instanceof Hcl.Identifier) {
            return ((Hcl.Identifier) this).getName();
        }
        String value = this instanceof Expression ? HclValues.stringValue((Expression) this) : null;
        return value != null ? value : printTrimmed(new HclPrinter<>());
    }
}
