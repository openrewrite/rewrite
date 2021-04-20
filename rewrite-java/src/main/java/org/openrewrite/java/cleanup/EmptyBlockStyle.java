package org.openrewrite.java.cleanup;

import lombok.Value;
import lombok.With;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.NullFields;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.style.StyleHelper;
import org.openrewrite.style.Style;

@Value
@With
@NullFields
public class EmptyBlockStyle implements Style {
    @NonNull
    BlockPolicy blockPolicy;

    Boolean instanceInit;
    Boolean literalCatch;
    Boolean literalDo;
    Boolean literalElse;
    Boolean literalFinally;
    Boolean literalFor;
    Boolean literalIf;
    Boolean literalSwitch;
    Boolean literalSynchronized;
    Boolean literalTry;
    Boolean literalWhile;
    Boolean staticInit;

    public enum BlockPolicy {
        /**
         * Require that there is some text in the block. For example:
         *     catch (Exception ex) {
         *         // This is a bad coding practice
         *     }
         */
        Text,

        /**
         * Require that there is a statement in the block. For example:
         *     finally {
         *         lock.release();
         *     }
         */
        Statement
    }

    @Override
    public Style applyDefaults() {
        return StyleHelper.merge(IntelliJ.emptyBlock(), this);
    }
}
