package org.openrewrite.internal.lang.nonnull;

import org.openrewrite.internal.lang.Nullable;

public class DefaultNonNullTest {

    private String yourCoolNonNullName = "sally";

    String coolNonNullName = "fred";

    @SuppressWarnings("ConstantConditions")
    private String aCoolNonNullName = null;

    @Nullable
    protected String myLessCoolNullableName;

    protected String beCoolNonNullName = "Samuel L Jackson";

}
