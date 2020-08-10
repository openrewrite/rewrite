package org.openrewrite.java

import org.junit.jupiter.api.extension.ExtendWith
import org.openrewrite.java.tree.NewClassTest

@ExtendWith(JavaParserResolver::class)
class Test : NewClassTest, Java11Test() {

}