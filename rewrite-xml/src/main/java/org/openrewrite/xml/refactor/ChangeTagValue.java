package org.openrewrite.xml.refactor;

import org.openrewrite.Formatting;
import org.openrewrite.xml.tree.Xml;

import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;

public class ChangeTagValue extends ScopedXmlRefactorVisitor {
    private final String value;

    public ChangeTagValue(Xml.Tag scope, String value) {
        super(scope.getId());
        this.value = value;
    }

    @Override
    public Xml visitTag(Xml.Tag tag) {
        Xml.Tag t = refactor(tag, super::visitTag);

        if (isScope()) {
            Formatting formatting = Formatting.EMPTY;
            if(t.getContent() != null && t.getContent().size() == 1 && t.getContent().get(0) instanceof Xml.CharData) {
                Xml.CharData existingValue = (Xml.CharData) t.getContent().get(0);

                if(existingValue.getText().equals(value)) {
                    return t;
                }

                // if the previous content was also character data, preserve its formatting
                formatting = existingValue.getFormatting();
            }
            t = t.withContent(singletonList(new Xml.CharData(randomId(), false, value, formatting)));
        }

        return t;
    }
}
