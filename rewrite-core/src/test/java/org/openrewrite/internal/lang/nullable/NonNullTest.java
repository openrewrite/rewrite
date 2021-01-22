package org.openrewrite.internal.lang.nullable;

import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;

@SuppressWarnings("ALL")
public class NonNullTest {

    @NonNull
    private String yourCoolNonNullName = "sally";

    @NonNull
    String coolNonNullName = "fred";

    @NonNull
    protected String aCoolNonNullName;

    protected String myLessCoolNullableName;

    @NonNull
    protected String beCoolNonNullName = "Samuel L Jackson";

}
