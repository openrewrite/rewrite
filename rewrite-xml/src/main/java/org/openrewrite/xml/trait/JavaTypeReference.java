package org.openrewrite.xml.trait;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Tree;
import org.openrewrite.trait.Trait;

public interface JavaTypeReference extends Trait<Tree> {

    @Nullable String getValue();

}
