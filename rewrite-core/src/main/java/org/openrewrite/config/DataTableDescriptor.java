/*
 * Copyright 2022 the original author or authors.
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

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.NlsRewrite;

import java.util.List;

@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DataTableDescriptor {

    @EqualsAndHashCode.Include
    String name;

    @NlsRewrite.DisplayName
    String displayName;

    @Getter(AccessLevel.NONE)
    @Nullable
    String instanceName;

    @NlsRewrite.Description
    String description;

    @Nullable
    String group;

    @EqualsAndHashCode.Include
    List<ColumnDescriptor> columns;

    /**
     * The instance name for this data table. Falls back to {@link #displayName}
     * when not explicitly set.
     */
    public String getInstanceName() {
        return instanceName != null ? instanceName : displayName;
    }
}
