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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.NlsRewrite;

import java.util.List;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DataTableDescriptor {

    @EqualsAndHashCode.Include
    private final String name;

    @NlsRewrite.DisplayName
    private final String displayName;

    @Nullable
    private final String instanceName;

    @NlsRewrite.Description
    private final String description;

    @Nullable
    private final String group;

    @EqualsAndHashCode.Include
    private final List<ColumnDescriptor> columns;

    @JsonCreator
    public DataTableDescriptor(
            @JsonProperty("name") String name,
            @JsonProperty("displayName") String displayName,
            @JsonProperty("instanceName") @Nullable String instanceName,
            @JsonProperty("description") String description,
            @JsonProperty("group") @Nullable String group,
            @JsonProperty("columns") List<ColumnDescriptor> columns) {
        this.name = name;
        this.displayName = displayName;
        this.instanceName = instanceName;
        this.description = description;
        this.group = group;
        this.columns = columns;
    }

    /**
     * The instance name for this data table. Falls back to {@link #displayName}
     * when not explicitly set.
     */
    public String getInstanceName() {
        return instanceName != null ? instanceName : displayName;
    }
}
