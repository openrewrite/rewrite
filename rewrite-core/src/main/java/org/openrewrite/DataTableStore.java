package org.openrewrite;

public interface DataTableStore {
    <Row> void insertRow(DataTable<Row> dataTable, ExecutionContext ctx, Row row);

    /**
     * Enable or disable the acceptance of new rows into the store. This allows, for example,
     * data table insertions to be disabled during {@link Preconditions} evaluation.
     *
     * @param accept Whether to accept new rows.
     */
    void acceptRows(boolean accept);
}
