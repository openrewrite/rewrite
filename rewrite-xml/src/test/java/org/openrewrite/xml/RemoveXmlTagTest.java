package org.openrewrite.xml;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.io.File;
import java.nio.file.Path;

import static org.openrewrite.xml.Assertions.xml;

class RemoveXmlTagTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {spec.recipe(new RemoveXmlTag("//bean", "**/beans.xml"));}


    @DocumentExample
    @Test
    void removeMatchingElementInMatchingFile() {
        rewriteRun(
          spec -> spec.recipe(new RemoveXmlTag("//bean", "**/beans.xml")),


          xml(
            """
              <beans>
                  <bean id='myBean.subpackage.subpackage2'/>
                  <other id='myBean.subpackage.subpackage2'/>
              </beans>
              """,
            """
              <beans>
                  <other id='myBean.subpackage.subpackage2'/>
              </beans>
              """,
            documentSourceSpec -> documentSourceSpec.path("my/project/beans.xml")

          )
        );
    }

    @Test
    void elementNotMatched() {
        rewriteRun(
          spec -> spec.recipe(new RemoveXmlTag("notBean", null)),
          xml(
            """
              <beans>
                  <bean id='myBean.subpackage.subpackage2'/>
                  <other id='myBean.subpackage.subpackage2'/>
              </beans>
              """
          )
        );
    }

    @Test
    void fileNotMatched() {
        rewriteRun(
          spec -> spec.recipe(new RemoveXmlTag("//bean", "**/beans.xml")),
          xml(
            """
              <beans>
                  <bean id='myBean.subpackage.subpackage2'/>
                  <other id='myBean.subpackage.subpackage2'/>
              </beans>
              """,
            documentSourceSpec -> documentSourceSpec.path("my/project/notBeans.xml")
          )
        );
    }
}
