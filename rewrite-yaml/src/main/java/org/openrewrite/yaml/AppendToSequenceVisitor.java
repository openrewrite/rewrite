package org.openrewrite.yaml;

import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.marker.Markers;
import org.openrewrite.yaml.tree.Yaml;

import java.util.List;

import static org.openrewrite.Tree.randomId;

public class AppendToSequenceVisitor extends YamlIsoVisitor<org.openrewrite.ExecutionContext> {
    private final JsonPathMatcher matcher;
    private final String value;

    public AppendToSequenceVisitor(JsonPathMatcher matcher, String value) {
        this.matcher = matcher;
        this.value = value;
    }

    @Override
    public Yaml.Sequence visitSequence(Yaml.Sequence sequence, ExecutionContext executionContext) {
        Yaml.Sequence s = super.visitSequence(sequence, executionContext);
            Cursor parent = getCursor().getParent();
            if (!matcher.matches(parent)) {
                return s;
            }
            if (alreadyVisited(s, executionContext)) {
                return s;
            }
            setVisited(s, executionContext);

            List<Yaml.Sequence.Entry> entries = sequence.getEntries();
            boolean hasDash = false;
            Yaml.Scalar.Style style = Yaml.Scalar.Style.PLAIN;
            String entryPrefix = "";
            String entryTrailingCommaPrefix = null;
            String itemPrefix = "";
            if (!entries.isEmpty()) {
                final int lastEntryIndex = entries.size() - 1;
                Yaml.Sequence.Entry existingEntry = entries.get(lastEntryIndex);
                hasDash = existingEntry.isDash();
                entryPrefix = existingEntry.getPrefix();
                entryTrailingCommaPrefix = existingEntry.getTrailingCommaPrefix();
                Yaml.Sequence.Block block = existingEntry.getBlock();
                itemPrefix = block.getPrefix();
                if (block instanceof Yaml.Sequence.Scalar) {
                    style = ((Yaml.Sequence.Scalar) block).getStyle();
                }
                if (!existingEntry.isDash()) {
                    entries.set(lastEntryIndex, existingEntry.withTrailingCommaPrefix(""));
                }
            }
            Yaml.Scalar newItem = new Yaml.Scalar(randomId(), itemPrefix, Markers.EMPTY, style, null, this.value);
            Yaml.Sequence.Entry newEntry = new Yaml.Sequence.Entry(randomId(), entryPrefix, Markers.EMPTY, newItem, hasDash, entryTrailingCommaPrefix);
            entries.add(newEntry);
            // return maybeAutoFormat(sequence, s.withEntries(entries), ec);
            return s.withEntries(entries);
    }

    private static void setVisited(Yaml.Sequence seq, ExecutionContext context) {
        context.putMessage(makeAlreadyVisitedKey(seq), Boolean.TRUE);
    }

    private static boolean alreadyVisited(Yaml.Sequence seq, ExecutionContext context) {
       return context.getMessage(makeAlreadyVisitedKey(seq), Boolean.FALSE);
    }

    private static String makeAlreadyVisitedKey(Yaml.Sequence seq) {
        return AppendToSequenceVisitor.class.getName() + ".alreadyVisited." + seq.getId().toString();
    }
}