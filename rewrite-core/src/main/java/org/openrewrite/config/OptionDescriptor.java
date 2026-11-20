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
package org.openrewrite.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.NlsRewrite;

import java.util.List;

@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class OptionDescriptor {
    /**
     * What a secret option's value is shown and recorded as.
     */
    public static final String REDACTED = "********";

    @EqualsAndHashCode.Include
    String name;

    @EqualsAndHashCode.Include
    String type;

    @NlsRewrite.DisplayName
    String displayName;

    @NlsRewrite.Description
    String description;

    @Nullable
    String example;

    @Nullable
    List<String> valid;

    boolean required;

    @Nullable
    @EqualsAndHashCode.Include
    Object value;

    /**
     * Whether the value is a secret, such as a password or token, to be masked wherever it is
     * displayed, stored or exported. See {@link org.openrewrite.Option#secret()}.
     */
    boolean secret;

    /**
     * @return This option, with its value replaced by {@link #REDACTED} when it is a secret that is set.
     */
    public OptionDescriptor redacted() {
        return secret && value != null ?
                new OptionDescriptor(name, type, displayName, description, example, valid, required, REDACTED, true) :
                this;
    }

    /**
     * @return The value to display or record: {@link #REDACTED} for a secret that is set.
     */
    @JsonIgnore
    public @Nullable Object getDisplayValue() {
        return redacted().value;
    }
}
