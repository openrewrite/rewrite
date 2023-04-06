package org.openrewrite.yaml;

import org.openrewrite.Cursor;
import org.openrewrite.marker.Markers;
import org.openrewrite.yaml.tree.Yaml;

import java.util.List;

import static org.openrewrite.Tree.randomId;

public class AppendToSequenceVisitor<P> extends YamlIsoVisitor<P> {
    private final JsonPathMatcher matcher;
    private final String value;

    public AppendToSequenceVisitor(JsonPathMatcher matcher, String value) {
        this.matcher = matcher;
        this.value = value;
    }

    @Override
    public Yaml.Sequence visitSequence(Yaml.Sequence sequence, P p) {
        // System.out.println("xxx visitSequence called, " + sequence.toString());
        Yaml.Sequence s = super.visitSequence(sequence, p);
            Cursor parent = getCursor().getParent();
            if (!matcher.matches(parent)) {
                return s;
            }
            List<Yaml.Sequence.Entry> entries = sequence.getEntries();
            boolean hasDash = true;
            Yaml.Scalar.Style style = Yaml.Scalar.Style.PLAIN;
            String entryPrefix = "";
            String entryTrailingCommaPrefix = "";
            String itemPrefix = "";
            if (!entries.isEmpty()) {
                Yaml.Sequence.Entry existingEntry = entries.get(entries.size() - 1);
                hasDash = existingEntry.isDash();
                entryPrefix = existingEntry.getPrefix();
                entryTrailingCommaPrefix = existingEntry.getTrailingCommaPrefix();
                Yaml.Sequence.Block block = existingEntry.getBlock();
                itemPrefix = block.getPrefix();
                if (block instanceof Yaml.Sequence.Scalar) {
                    style = ((Yaml.Sequence.Scalar) block).getStyle();
                }
            }
            Yaml.Scalar newItem = new Yaml.Scalar(randomId(), itemPrefix, Markers.EMPTY, style, null, this.value);
            Yaml.Sequence.Entry newEntry = new Yaml.Sequence.Entry(randomId(), entryPrefix, Markers.EMPTY, newItem, hasDash, entryTrailingCommaPrefix);
            entries.add(newEntry);
            // System.out.println("xxx entries size " + entries.size());
            // return maybeAutoFormat(sequence, s.withEntries(entries), ec);
            return s.withEntries(entries);
    }
}