package org.openrewrite.internal;

import org.openrewrite.DataTable;
import org.openrewrite.DataTableStore;
import org.openrewrite.ExecutionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class InMemoryDataTableStore implements DataTableStore {
    private final AtomicBoolean acceptRows = new AtomicBoolean(true);
    private final Map<Object, List<Object>> rows = new ConcurrentHashMap<>();

    @Override
    public <Row> void insertRow(DataTable<Row> dataTable, ExecutionContext ctx, Row row) {
        if (acceptRows.get()) {
            rows.computeIfAbsent(dataTable, k -> new ArrayList<>()).add(row);
        }
    }

    @Override
    public void acceptRows(boolean accept) {
        acceptRows.set(accept);
    }
}
