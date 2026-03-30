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
package org.openrewrite;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.ParameterizedType;
import java.util.function.Supplier;

/**
 * @param <Row> The model type for a single row of this data table.
 */
@Getter
@JsonIgnoreType
@RequiredArgsConstructor
public class DataTable<Row> {
    @Language("markdown")
    private final @NlsRewrite.DisplayName String displayName;

    @Language("markdown")
    private final @NlsRewrite.Description String description;

    /**
     * The recipe that owns this data table. Null when transferred over RPC.
     */
    private transient @Nullable Recipe recipe;

    /**
     * Community group name. When set, multiple recipes contributing to the same
     * group and data table class share one storage bucket.
     */
    private @Nullable String group;

    /**
     * Lazy supplier for the instance name. Evaluated on first access to
     * {@link #getInstanceName()}, allowing recipe options to be set after
     * DataTable construction.
     */
    private transient @Nullable Supplier<String> instanceNameSupplier;


    /**
     * Construct a new data table.
     *
     * @param recipe      The recipe that this data table is associated with.
     * @param displayName The display name of this data table.
     * @param description The description of this data table.
     */
    public DataTable(@Nullable Recipe recipe,
                     @NlsRewrite.DisplayName @Language("markdown") String displayName,
                     @NlsRewrite.Description @Language("markdown") String description) {
        this.displayName = displayName;
        this.description = description;
        this.recipe = recipe;

        // Only null when transferring DataTables over RPC.
        if (recipe != null) {
            recipe.addDataTable(this);
        }
    }

    public Class<Row> getType() {
        //noinspection unchecked
        return (Class<Row>) ((ParameterizedType) getClass().getGenericSuperclass())
                .getActualTypeArguments()[0];
    }

    /**
     * The fully qualified class name of this data table.
     */
    public String getName() {
        return getClass().getName();
    }

    /**
     * The instance name for this data table. Never null.
     * <p>
     * When a {@link #withInstanceName(Supplier) lazy supplier} has been set,
     * it is evaluated on first access (after recipe options are populated).
     * Otherwise, defaults to {@link #getDisplayName()}.
     * <p>
     * When no {@link #getGroup() group} is set, this name is used by the store
     * to identify the data table bucket. Also exposed in the GraphQL API.
     */
    public String getInstanceName() {
        if (instanceNameSupplier != null) {
            return instanceNameSupplier.get();
        }
        return displayName;
    }

    /**
     * Set a community group for this data table. All recipes that put the same
     * data table class into the same group share one storage bucket.
     *
     * @param group the group name
     * @return this data table for fluent chaining
     */
    @SuppressWarnings("unchecked")
    public <T extends DataTable<Row>> T withGroup(String group) {
        this.group = group;
        return (T) this;
    }

    /**
     * Set the instance name for this data table using a lazy supplier.
     * The supplier is evaluated on first access to {@link #getInstanceName()},
     * which allows it to reference recipe options that are populated after
     * DataTable construction.
     * <p>
     * Example:
     * <pre>{@code
     * transient MethodCalls methodCalls = new MethodCalls(this)
     *     .withInstanceName(() -> "Method calls matching `" + methodPattern + "`");
     * }</pre>
     *
     * @param instanceName a supplier that returns the instance name
     * @return this data table for fluent chaining
     */
    @SuppressWarnings("unchecked")
    public <T extends DataTable<Row>> T withInstanceName(Supplier<String> instanceName) {
        this.instanceNameSupplier = instanceName;
        return (T) this;
    }


    public void insertRow(ExecutionContext ctx, Row row) {
        if (!allowWritingInThisCycle(ctx)) {
            return;
        }
        DataTableExecutionContextView.view(ctx).getDataTableStore().insertRow(this, ctx, row);
    }

    /**
     * This method is used to decide whether to ignore any row insertions in the current cycle.
     * This prevents (by default) data table producing recipes from having to keep track of state across
     * multiple cycles to prevent duplicate row entries.
     *
     * @param ctx the execution context
     * @return whether to allow writing in this cycle
     */
    protected boolean allowWritingInThisCycle(ExecutionContext ctx) {
        return ctx.getCycle() <= 1;
    }
}
