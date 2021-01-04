package org.openrewrite;

import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;

import java.util.List;

import static java.util.Collections.emptyList;

public abstract class Recipe {
    public static final TreeProcessor<?, ExecutionContext> NOOP = new TreeProcessor<Tree, ExecutionContext>() {
        @Override
        Tree visitInternal(Tree tree, ExecutionContext ctx) {
            return tree;
        }
    };

    @Nullable
    private Recipe next;
    private final TreeProcessor<?, ExecutionContext> processor;

    protected Recipe(TreeProcessor<?, ExecutionContext> processor) {
        this.processor = processor;
    }

    protected Recipe() {
        this.processor = NOOP;
    }

    protected void doNext(Recipe recipe) {
        Recipe head = this;
        for (Recipe tail = next; tail != null; tail = tail.next) {
            // FIXME implement me!
        }
        head.next = recipe;
    }

    protected List<SourceFile> visit(List<SourceFile> sourceFiles, ExecutionContext execution) {
        List<SourceFile> acc = sourceFiles;
        List<SourceFile> temp = acc;
        for (int i = 0; i < execution.getMaxCycles(); i++) {
            // if this recipe isn't valid we just skip it and proceed to next
            if (validate().isValid()) {
                temp = ListUtils.map(temp, s -> (SourceFile) processor.visit(s, execution));
            }
            if (next != null) {
                temp = next.visit(temp, execution);
            }
            if (temp == acc) {
                break;
            }
            acc = temp;
        }
        return acc;
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
