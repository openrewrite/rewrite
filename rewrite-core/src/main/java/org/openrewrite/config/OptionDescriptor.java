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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.NlsRewrite;
import org.openrewrite.marketplace.RecipeListing;

import java.util.List;

@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class OptionDescriptor implements RecipeListing.Option {

    @EqualsAndHashCode.Include
    String name;

    @EqualsAndHashCode.Include
    String type;

    @Nullable
    @NlsRewrite.DisplayName
    String displayName;

    @Nullable
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
}
