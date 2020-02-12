package com.netflix.rewrite.tree.visitor.refactor;

import com.netflix.rewrite.tree.Statement;
import com.netflix.rewrite.tree.Tr;
import com.netflix.rewrite.tree.Tree;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.stream.IntStream.range;

public class ShiftFormatRightVisitor extends ScopedRefactorVisitor {
    private final String shift;

    public ShiftFormatRightVisitor(UUID scope, int shift, boolean isIndentedWithSpaces) {
        super(scope);
        this.shift = range(0, shift)
                .mapToObj(n -> isIndentedWithSpaces ? " " : "\t")
                .collect(Collectors.joining(""));
    }

    @Override
    protected String getRuleName() {
        return "shift-format-right";
    }

    @Override
    public List<AstTransform> visitElse(Tr.If.Else elze) {
        return maybeTransform(isInScope(elze) && isOnOwnLine(elze),
                super.visitElse(elze),
                shiftRight(elze));
    }

    @Override
    public List<AstTransform> visitStatement(Statement statement) {
        return maybeTransform(isInScope(statement) && isOnOwnLine(statement),
                super.visitStatement(statement),
                shiftRight(statement)
        );
    }

    @Override
    public List<AstTransform> visitBlock(Tr.Block<Tree> block) {
        return maybeTransform(isInScope(block),
                super.visitBlock(block),
                transform(block, b -> b.withEndOfBlockSuffix(b.getEndOfBlockSuffix() + shift))
        );
    }

    private boolean isOnOwnLine(Tree tree) {
        return tree.getFormatting().getPrefix().chars().takeWhile(c -> c == '\n' || c == '\r').count() > 0;
    }

    private List<AstTransform> shiftRight(Tree tree) {
        return transform(tree, s -> s.withFormatting(tree.getFormatting().withPrefix(tree.getFormatting().getPrefix() + shift)));
    }
}
