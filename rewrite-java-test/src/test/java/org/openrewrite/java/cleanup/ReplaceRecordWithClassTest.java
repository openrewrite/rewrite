/*
 * Copyright 2023 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.cleanup;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.version;

class ReplaceRecordWithClassTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ReplaceRecordWithClass());
    }

    @Test
    void simpleRecord() {
        rewriteRun(
          version(
            java(
              """
              package com.example;

              public record Vehicle(String model, int power) {
              }
              """,
              """
              package com.example;

              import java.util.Objects;

              public final class Vehicle {
                  private final String model;
                  private final int power;

                  public Vehicle(String model, int power) {
                      this.model = model;
                      this.power = power;
                  }

                  public String model() {
                      return model;
                  }

                  public int power() {
                      return power;
                  }

                  @Override
                  public boolean equals(Object obj) {
                      if (this == obj) {
                          return true;
                      }
                      if (obj == null) {
                          return false;
                      }
                      if (getClass() != obj.getClass()) {
                          return false;
                      }
                      Vehicle other = (Vehicle) obj;
                      return Objects.equals(model, other.model) && power == other.power;
                  }

                  @Override
                  public int hashCode() {
                      return Objects.hash(model, power);
                  }

                  @Override
                  public String toString() {
                      return "Vehicle[model=" + model + ", power=" + power + "]";
                  }

              }
              """),
            14));
    }

    @Test
    void genericType() {
        rewriteRun(
          version(
            java(
              """
              package com.example;

              public record Vehicle(java.util.List<String> models) {
              }
              """,
              """
              package com.example;

              import java.util.Objects;

              public final class Vehicle {
                  private final java.util.List<String> models;

                  public Vehicle(java.util.List<String> models) {
                      this.models = models;
                  }

                  public java.util.List<String> models() {
                      return models;
                  }

                  @Override
                  public boolean equals(Object obj) {
                      if (this == obj) {
                          return true;
                      }
                      if (obj == null) {
                          return false;
                      }
                      if (getClass() != obj.getClass()) {
                          return false;
                      }
                      Vehicle other = (Vehicle) obj;
                      return Objects.equals(models, other.models);
                  }

                  @Override
                  public int hashCode() {
                      return Objects.hash(models);
                  }

                  @Override
                  public String toString() {
                      return "Vehicle[models=" + models + "]";
                  }
              }
              """),
            14));
    }

    @Test
    void implementsInterface() {
        rewriteRun(
          version(
            java(
              """
              package com.example;

              public interface Product {
                  String model();
              }

              public record Vehicle(String model) implements Product {
              }
              """,
              """
              package com.example;

              import java.util.Objects;

              public interface Product {
                  String model();
              }

              public final class Vehicle implements Product {
                  private final String model;

                  public Vehicle(String model) {
                      this.model = model;
                  }

                  public String model() {
                      return model;
                  }

                  @Override
                  public boolean equals(Object obj) {
                      if (this == obj) {
                          return true;
                      }
                      if (obj == null) {
                          return false;
                      }
                      if (getClass() != obj.getClass()) {
                          return false;
                      }
                      Vehicle other = (Vehicle) obj;
                      return Objects.equals(model, other.model);
                  }

                  @Override
                  public int hashCode() {
                      return Objects.hash(model);
                  }

                  @Override
                  public String toString() {
                      return "Vehicle[model=" + model + "]";
                  }

              }
              """),
            14));
    }

    @Test
    void statementsNotSupported() {
        rewriteRun(
          version(
            java(
              """
              package com.example;

              public record Vehicle(String model) {
                  public Vehicle() {
                      this("default");
                  }
              }
              """),
            14));
    }

}
