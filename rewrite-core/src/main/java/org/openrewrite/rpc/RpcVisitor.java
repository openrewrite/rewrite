package org.openrewrite.rpc;

import lombok.RequiredArgsConstructor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;

@RequiredArgsConstructor
class RpcVisitor extends TreeVisitor<Tree, ExecutionContext> {
    private final RewriteRpc rpc;
    private final String visitorName;

    @Override
    public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
        // TODO at the point where we add a second RPC language like Python, we should
        //  narrow this check to the set of source files that the remote peer supports
        return sourceFile instanceof RpcCodec;
    }

    @Override
    public Tree preVisit(Tree tree, ExecutionContext ctx) {
        stopAfterPreVisit();
        rpc.scan((SourceFile) tree, visitorName, ctx);
        return tree;
    }
}
