package org.openrewrite;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.marker.LstProvenance;
import org.openrewrite.table.LstProvenanceTable;

import java.util.HashSet;
import java.util.Set;

@Value
@EqualsAndHashCode(callSuper = true)
public class FindLstProvenance extends ScanningRecipe<FindLstProvenance.Accumulator> {

    @Override
    public String getDisplayName() {
        return "Find LST provenance";
    }

    @Override
    public String getDescription() {
        return "Produces a data table showing what versions of OpenRewrite/Moderne tooling was used to produce a given LST. ";
    }

    transient LstProvenanceTable provenanceTable = new LstProvenanceTable(this);

    public static class Accumulator {
        Set<LstProvenance> seenProvenance = new HashSet<>();
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                LstProvenance lstProvenance = tree.getMarkers().findFirst(LstProvenance.class).orElse(null);
                if (lstProvenance == null) {
                    return tree;
                }
                if(acc.seenProvenance.add(lstProvenance)) {
                    provenanceTable.insertRow(ctx, new LstProvenanceTable.Row(lstProvenance.getBuildToolType(),
                            lstProvenance.getBuildToolVersion(), lstProvenance.getLstSerializerVersion()));
                }
                return tree;
            }
        };
    }
}
