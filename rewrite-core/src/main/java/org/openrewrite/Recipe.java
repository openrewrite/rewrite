package org.openrewrite;

import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;

import java.util.List;

import static java.util.Collections.emptyList;

public abstract class Recipe {
    @Nullable
    private Recipe next;
    private final TreeProcessor<?> processor;

    protected Recipe(TreeProcessor<?> processor) {
        this.processor = processor;
    }

    protected Recipe() {
        this.processor = TreeProcessor.NOOP;
    }

    protected void doNext(Recipe recipe) {
        Recipe head = this;
        for (Recipe tail = next; tail != null; tail = tail.next) {

        }
        head.next = recipe;
    }

    protected List<SourceFile> visit(List<SourceFile> sourceFiles, ExecutionContext execution) {
        List<SourceFile> after = emptyList();
        for (int i = 0; i < execution.getMaxCycles() && after != sourceFiles; i++) {
            // if this recipe isn't valid we just skip it and proceed to next
            if (validate().isValid()) {
                after = ListUtils.map(sourceFiles, s -> (SourceFile) processor.visit(s, execution));
            }
            if (next != null) {
                after = next.visit(after, execution);
            }
        }
        return after;
    }

    public final List<Result> run(List<SourceFile> sourceFiles) {
        return run(sourceFiles, new ExecutionContext(3, false));
    }

    public final List<Result> run(List<SourceFile> sourceFiles, ExecutionContext context) {
        List<SourceFile> after = visit(sourceFiles, context);

        if (after == sourceFiles) {
            return emptyList();
        }

        // FIXME compute list difference between sourceFiles and after and generate Result
        return emptyList();
    }

    public Validated validate() {
        return Validated.none();
    }

    public String getName() {
        return getClass().getName();
    }
}
