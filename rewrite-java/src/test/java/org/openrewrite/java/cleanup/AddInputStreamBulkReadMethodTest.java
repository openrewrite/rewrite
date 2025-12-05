/*
 * Copyright 2024 the original author or authors.
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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class AddInputStreamBulkReadMethodTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddInputStreamBulkReadMethod());
    }

    @Nested
    class ShouldTransform {

        @DocumentExample
        @Test
        void anonymousClassWithSimpleDelegation() {
            rewriteRun(
              java(
                """
                  import java.io.IOException;
                  import java.io.InputStream;

                  class Example {
                      private InputStream delegate;

                      InputStream getWrappedStream() {
                          return new InputStream() {
                              @Override
                              public int read() throws IOException {
                                  return delegate.read();
                              }
                          };
                      }
                  }
                  """,
                """
                  import java.io.IOException;
                  import java.io.InputStream;

                  class Example {
                      private InputStream delegate;

                      InputStream getWrappedStream() {
                          return new InputStream() {
                              @Override
                              public int read() throws IOException {
                                  return delegate.read();
                              }

                              @Override
                              public int read(byte[] b, int off, int len) throws IOException {
                                  return delegate.read(b, off, len);
                              }
                          };
                      }
                  }
                  """
              )
            );
        }

        @Test
        void anonymousClassWithNullCheck() {
            rewriteRun(
              java(
                """
                  import java.io.IOException;
                  import java.io.InputStream;

                  class Example {
                      private InputStream body;

                      InputStream getBody() {
                          return new InputStream() {
                              @Override
                              public int read() throws IOException {
                                  return body == null ? -1 : body.read();
                              }
                          };
                      }
                  }
                  """,
                """
                  import java.io.IOException;
                  import java.io.InputStream;

                  class Example {
                      private InputStream body;

                      InputStream getBody() {
                          return new InputStream() {
                              @Override
                              public int read() throws IOException {
                                  return body == null ? -1 : body.read();
                              }

                              @Override
                              public int read(byte[] b, int off, int len) throws IOException {
                                  return body == null ? -1 : body.read(b, off, len);
                              }
                          };
                      }
                  }
                  """
              )
            );
        }

        @Test
        void anonymousClassWithNullCheckReversed() {
            rewriteRun(
              java(
                """
                  import java.io.IOException;
                  import java.io.InputStream;

                  class Example {
                      private InputStream body;

                      InputStream getBody() {
                          return new InputStream() {
                              @Override
                              public int read() throws IOException {
                                  return null == body ? -1 : body.read();
                              }
                          };
                      }
                  }
                  """,
                """
                  import java.io.IOException;
                  import java.io.InputStream;

                  class Example {
                      private InputStream body;

                      InputStream getBody() {
                          return new InputStream() {
                              @Override
                              public int read() throws IOException {
                                  return null == body ? -1 : body.read();
                              }

                              @Override
                              public int read(byte[] b, int off, int len) throws IOException {
                                  return body == null ? -1 : body.read(b, off, len);
                              }
                          };
                      }
                  }
                  """
              )
            );
        }

        @Test
        void localVariableDelegate() {
            rewriteRun(
              java(
                """
                  import java.io.IOException;
                  import java.io.InputStream;

                  class Example {
                      InputStream wrap(InputStream source) {
                          return new InputStream() {
                              @Override
                              public int read() throws IOException {
                                  return source.read();
                              }
                          };
                      }
                  }
                  """,
                """
                  import java.io.IOException;
                  import java.io.InputStream;

                  class Example {
                      InputStream wrap(InputStream source) {
                          return new InputStream() {
                              @Override
                              public int read() throws IOException {
                                  return source.read();
                              }

                              @Override
                              public int read(byte[] b, int off, int len) throws IOException {
                                  return source.read(b, off, len);
                              }
                          };
                      }
                  }
                  """
              )
            );
        }

        @Test
        void anonymousClassWithCloseMethod() {
            rewriteRun(
              java(
                """
                  import java.io.IOException;
                  import java.io.InputStream;

                  class Example {
                      private InputStream body;
                      private Runnable onClose;

                      InputStream getBody() {
                          return new InputStream() {
                              @Override
                              public int read() throws IOException {
                                  return body == null ? -1 : body.read();
                              }

                              @Override
                              public void close() throws IOException {
                                  if (body != null) body.close();
                                  onClose.run();
                              }
                          };
                      }
                  }
                  """,
                """
                  import java.io.IOException;
                  import java.io.InputStream;

                  class Example {
                      private InputStream body;
                      private Runnable onClose;

                      InputStream getBody() {
                          return new InputStream() {
                              @Override
                              public int read() throws IOException {
                                  return body == null ? -1 : body.read();
                              }

                              @Override
                              public int read(byte[] b, int off, int len) throws IOException {
                                  return body == null ? -1 : body.read(b, off, len);
                              }

                              @Override
                              public void close() throws IOException {
                                  if (body != null) body.close();
                                  onClose.run();
                              }
                          };
                      }
                  }
                  """
              )
            );
        }

        @Test
        void namedClassWithSimpleDelegation() {
            rewriteRun(
              java(
                """
                  import java.io.IOException;
                  import java.io.InputStream;

                  class DelegatingInputStream extends InputStream {
                      private final InputStream delegate;

                      DelegatingInputStream(InputStream delegate) {
                          this.delegate = delegate;
                      }

                      @Override
                      public int read() throws IOException {
                          return delegate.read();
                      }
                  }
                  """,
                """
                  import java.io.IOException;
                  import java.io.InputStream;

                  class DelegatingInputStream extends InputStream {
                      private final InputStream delegate;

                      DelegatingInputStream(InputStream delegate) {
                          this.delegate = delegate;
                      }

                      @Override
                      public int read() throws IOException {
                          return delegate.read();
                      }

                      @Override
                      public int read(byte[] b, int off, int len) throws IOException {
                          return delegate.read(b, off, len);
                      }
                  }
                  """
              )
            );
        }

        @Test
        void namedClassWithNullCheck() {
            rewriteRun(
              java(
                """
                  import java.io.IOException;
                  import java.io.InputStream;

                  class SafeInputStream extends InputStream {
                      private InputStream delegate;

                      void setDelegate(InputStream delegate) {
                          this.delegate = delegate;
                      }

                      @Override
                      public int read() throws IOException {
                          return delegate == null ? -1 : delegate.read();
                      }
                  }
                  """,
                """
                  import java.io.IOException;
                  import java.io.InputStream;

                  class SafeInputStream extends InputStream {
                      private InputStream delegate;

                      void setDelegate(InputStream delegate) {
                          this.delegate = delegate;
                      }

                      @Override
                      public int read() throws IOException {
                          return delegate == null ? -1 : delegate.read();
                      }

                      @Override
                      public int read(byte[] b, int off, int len) throws IOException {
                          return delegate == null ? -1 : delegate.read(b, off, len);
                      }
                  }
                  """
              )
            );
        }

        @Test
        void anonymousClassWithIfNullCheck() {
            rewriteRun(
              java(
                """
                  import java.io.IOException;
                  import java.io.InputStream;

                  class Example {
                      private InputStream body;

                      InputStream getBody() {
                          return new InputStream() {
                              @Override
                              public int read() throws IOException {
                                  if (body == null) {
                                      return -1;
                                  }
                                  return body.read();
                              }
                          };
                      }
                  }
                  """,
                """
                  import java.io.IOException;
                  import java.io.InputStream;

                  class Example {
                      private InputStream body;

                      InputStream getBody() {
                          return new InputStream() {
                              @Override
                              public int read() throws IOException {
                                  if (body == null) {
                                      return -1;
                                  }
                                  return body.read();
                              }

                              @Override
                              public int read(byte[] b, int off, int len) throws IOException {
                                  if (body == null) {
                                      return -1;
                                  }
                                  return body.read(b, off, len);
                              }
                          };
                      }
                  }
                  """
              )
            );
        }

        @Test
        void namedClassWithIfNullCheck() {
            rewriteRun(
              java(
                """
                  import java.io.IOException;
                  import java.io.InputStream;

                  class SafeInputStream extends InputStream {
                      private InputStream delegate;

                      void setDelegate(InputStream delegate) {
                          this.delegate = delegate;
                      }

                      @Override
                      public int read() throws IOException {
                          if (delegate == null) {
                              return -1;
                          }
                          return delegate.read();
                      }
                  }
                  """,
                """
                  import java.io.IOException;
                  import java.io.InputStream;

                  class SafeInputStream extends InputStream {
                      private InputStream delegate;

                      void setDelegate(InputStream delegate) {
                          this.delegate = delegate;
                      }

                      @Override
                      public int read() throws IOException {
                          if (delegate == null) {
                              return -1;
                          }
                          return delegate.read();
                      }

                      @Override
                      public int read(byte[] b, int off, int len) throws IOException {
                          if (delegate == null) {
                              return -1;
                          }
                          return delegate.read(b, off, len);
                      }
                  }
                  """
              )
            );
        }

        @Test
        void delegateIsInputStreamSubclass() {
            rewriteRun(
              java(
                """
                  import java.io.IOException;
                  import java.io.InputStream;
                  import java.util.zip.ZipInputStream;

                  class Example {
                      private ZipInputStream zipIn;

                      InputStream getWrappedStream() {
                          return new InputStream() {
                              @Override
                              public int read() throws IOException {
                                  return zipIn.read();
                              }
                          };
                      }
                  }
                  """,
                """
                  import java.io.IOException;
                  import java.io.InputStream;
                  import java.util.zip.ZipInputStream;

                  class Example {
                      private ZipInputStream zipIn;

                      InputStream getWrappedStream() {
                          return new InputStream() {
                              @Override
                              public int read() throws IOException {
                                  return zipIn.read();
                              }

                              @Override
                              public int read(byte[] b, int off, int len) throws IOException {
                                  return zipIn.read(b, off, len);
                              }
                          };
                      }
                  }
                  """
              )
            );
        }

        @Test
        void fieldAccessWithThis() {
            rewriteRun(
              java(
                """
                  import java.io.IOException;
                  import java.io.InputStream;

                  class Example {
                      private InputStream delegate;

                      InputStream getWrappedStream() {
                          return new InputStream() {
                              @Override
                              public int read() throws IOException {
                                  return Example.this.delegate.read();
                              }
                          };
                      }
                  }
                  """,
                """
                  import java.io.IOException;
                  import java.io.InputStream;

                  class Example {
                      private InputStream delegate;

                      InputStream getWrappedStream() {
                          return new InputStream() {
                              @Override
                              public int read() throws IOException {
                                  return Example.this.delegate.read();
                              }

                              @Override
                              public int read(byte[] b, int off, int len) throws IOException {
                                  return Example.this.delegate.read(b, off, len);
                              }
                          };
                      }
                  }
                  """
              )
            );
        }
    }

    @Nested
    class ShouldNotTransform {

        @Test
        void alreadyHasBulkReadMethod() {
            rewriteRun(
              java(
                """
                  import java.io.IOException;
                  import java.io.InputStream;

                  class Example {
                      private InputStream delegate;

                      InputStream getWrappedStream() {
                          return new InputStream() {
                              @Override
                              public int read() throws IOException {
                                  return delegate.read();
                              }

                              @Override
                              public int read(byte[] b, int off, int len) throws IOException {
                                  return delegate.read(b, off, len);
                              }
                          };
                      }
                  }
                  """
              )
            );
        }

        @Test
        void notExtendingInputStream() {
            rewriteRun(
              java(
                """
                  import java.io.IOException;

                  class Example {
                      void foo() {
                          Runnable r = new Runnable() {
                              @Override
                              public void run() {
                              }
                          };
                      }
                  }
                  """
              )
            );
        }

        @Test
        void filterInputStreamSubclass() {
            rewriteRun(
              java(
                """
                  import java.io.FilterInputStream;
                  import java.io.IOException;
                  import java.io.InputStream;

                  class MyFilterStream extends FilterInputStream {
                      MyFilterStream(InputStream in) {
                          super(in);
                      }

                      @Override
                      public int read() throws IOException {
                          return in.read();
                      }
                  }
                  """
              )
            );
        }

        @Test
        void namedClassAlreadyHasBulkRead() {
            rewriteRun(
              java(
                """
                  import java.io.IOException;
                  import java.io.InputStream;

                  class DelegatingInputStream extends InputStream {
                      private final InputStream delegate;

                      DelegatingInputStream(InputStream delegate) {
                          this.delegate = delegate;
                      }

                      @Override
                      public int read() throws IOException {
                          return delegate.read();
                      }

                      @Override
                      public int read(byte[] b, int off, int len) throws IOException {
                          return delegate.read(b, off, len);
                      }
                  }
                  """
              )
            );
        }

    }

    @Nested
    class ShouldMarkForReview {
        // These tests verify that complex cases with identifiable delegates
        // get a search marker so developers can manually review them

        @Test
        void marksComplexBodyWithSideEffectsForReview() {
            rewriteRun(
              java(
                """
                  import java.io.IOException;
                  import java.io.InputStream;

                  class CountingInputStream extends InputStream {
                      private final InputStream delegate;
                      private long bytesRead = 0;

                      CountingInputStream(InputStream delegate) {
                          this.delegate = delegate;
                      }

                      @Override
                      public int read() throws IOException {
                          int b = delegate.read();
                          if (b != -1) bytesRead++;
                          return b;
                      }

                      public long getBytesRead() {
                          return bytesRead;
                      }
                  }
                  """,
                """
                  import java.io.IOException;
                  import java.io.InputStream;
                  
                  /*~~(Missing bulk read method may cause significant performance degradation)~~>*/class CountingInputStream extends InputStream {
                      private final InputStream delegate;
                      private long bytesRead = 0;
                  
                      CountingInputStream(InputStream delegate) {
                          this.delegate = delegate;
                      }
                  
                      @Override
                      public int read() throws IOException {
                          int b = delegate.read();
                          if (b != -1) bytesRead++;
                          return b;
                      }
                  
                      public long getBytesRead() {
                          return bytesRead;
                      }
                  }
                  """
              )
            );
        }

        @Test
        void marksAnonymousComplexBodyForReview() {
            rewriteRun(
              java(
                """
                  import java.io.IOException;
                  import java.io.InputStream;

                  class Example {
                      private InputStream delegate;
                      private long count = 0;

                      InputStream getWrappedStream() {
                          return new InputStream() {
                              @Override
                              public int read() throws IOException {
                                  int b = delegate.read();
                                  if (b != -1) count++;
                                  return b;
                              }
                          };
                      }
                  }
                  """,
                """
                  import java.io.IOException;
                  import java.io.InputStream;

                  class Example {
                      private InputStream delegate;
                      private long count = 0;

                      InputStream getWrappedStream() {
                          return /*~~(Missing bulk read method may cause significant performance degradation)~~>*/new InputStream() {
                              @Override
                              public int read() throws IOException {
                                  int b = delegate.read();
                                  if (b != -1) count++;
                                  return b;
                              }
                          };
                      }
                  }
                  """
              )
            );
        }

        @Test
        void marksComplexBodyWithMultipleStatementsForReview() {
            rewriteRun(
              java(
                """
                  import java.io.IOException;
                  import java.io.InputStream;

                  class LoggingInputStream extends InputStream {
                      private final InputStream delegate;

                      LoggingInputStream(InputStream delegate) {
                          this.delegate = delegate;
                      }

                      @Override
                      public int read() throws IOException {
                          System.out.println("Reading byte");
                          return delegate.read();
                      }
                  }
                  """,
                """
                  import java.io.IOException;
                  import java.io.InputStream;

                  /*~~(Missing bulk read method may cause significant performance degradation)~~>*/class LoggingInputStream extends InputStream {
                      private final InputStream delegate;

                      LoggingInputStream(InputStream delegate) {
                          this.delegate = delegate;
                      }

                      @Override
                      public int read() throws IOException {
                          System.out.println("Reading byte");
                          return delegate.read();
                      }
                  }
                  """
              )
            );
        }

        @Test
        void marksComplexBodyWithTransformationForReview() {
            rewriteRun(
              java(
                """
                  import java.io.IOException;
                  import java.io.InputStream;

                  class XorInputStream extends InputStream {
                      private final InputStream delegate;
                      private final int xorKey;

                      XorInputStream(InputStream delegate, int xorKey) {
                          this.delegate = delegate;
                          this.xorKey = xorKey;
                      }

                      @Override
                      public int read() throws IOException {
                          int b = delegate.read();
                          return b == -1 ? -1 : (b ^ xorKey);
                      }
                  }
                  """,
                """
                  import java.io.IOException;
                  import java.io.InputStream;

                  /*~~(Missing bulk read method may cause significant performance degradation)~~>*/class XorInputStream extends InputStream {
                      private final InputStream delegate;
                      private final int xorKey;

                      XorInputStream(InputStream delegate, int xorKey) {
                          this.delegate = delegate;
                          this.xorKey = xorKey;
                      }

                      @Override
                      public int read() throws IOException {
                          int b = delegate.read();
                          return b == -1 ? -1 : (b ^ xorKey);
                      }
                  }
                  """
              )
            );
        }

        @Test
        void marksComplexBodyWithTryCatchForReview() {
            rewriteRun(
              java(
                """
                  import java.io.IOException;
                  import java.io.InputStream;

                  class SafeInputStream extends InputStream {
                      private final InputStream delegate;

                      SafeInputStream(InputStream delegate) {
                          this.delegate = delegate;
                      }

                      @Override
                      public int read() throws IOException {
                          try {
                              return delegate.read();
                          } catch (IOException e) {
                              return -1;
                          }
                      }
                  }
                  """,
                """
                  import java.io.IOException;
                  import java.io.InputStream;

                  /*~~(Missing bulk read method may cause significant performance degradation)~~>*/class SafeInputStream extends InputStream {
                      private final InputStream delegate;

                      SafeInputStream(InputStream delegate) {
                          this.delegate = delegate;
                      }

                      @Override
                      public int read() throws IOException {
                          try {
                              return delegate.read();
                          } catch (IOException e) {
                              return -1;
                          }
                      }
                  }
                  """
              )
            );
        }

        @Test
        void marksNoDelegateForManualImplementation() {
            rewriteRun(
              java(
                """
                  import java.io.IOException;
                  import java.io.InputStream;

                  class Example {
                      InputStream getStream() {
                          return new InputStream() {
                              private int position = 0;
                              private byte[] data = new byte[100];

                              @Override
                              public int read() throws IOException {
                                  if (position >= data.length) return -1;
                                  return data[position++] & 0xff;
                              }
                          };
                      }
                  }
                  """,
                """
                  import java.io.IOException;
                  import java.io.InputStream;

                  class Example {
                      InputStream getStream() {
                          return /*~~(Missing bulk read method may cause significant performance degradation)~~>*/new InputStream() {
                              private int position = 0;
                              private byte[] data = new byte[100];

                              @Override
                              public int read() throws IOException {
                                  if (position >= data.length) return -1;
                                  return data[position++] & 0xff;
                              }
                          };
                      }
                  }
                  """
              )
            );
        }

        @Test
        void marksConditionalDelegationForManualImplementation() {
            rewriteRun(
              java(
                """
                  import java.io.IOException;
                  import java.io.InputStream;

                  class ConditionalInputStream extends InputStream {
                      private final InputStream primary;
                      private final InputStream fallback;
                      private boolean usePrimary = true;

                      ConditionalInputStream(InputStream primary, InputStream fallback) {
                          this.primary = primary;
                          this.fallback = fallback;
                      }

                      @Override
                      public int read() throws IOException {
                          return usePrimary ? primary.read() : fallback.read();
                      }
                  }
                  """,
                """
                  import java.io.IOException;
                  import java.io.InputStream;

                  /*~~(Missing bulk read method may cause significant performance degradation)~~>*/class ConditionalInputStream extends InputStream {
                      private final InputStream primary;
                      private final InputStream fallback;
                      private boolean usePrimary = true;

                      ConditionalInputStream(InputStream primary, InputStream fallback) {
                          this.primary = primary;
                          this.fallback = fallback;
                      }

                      @Override
                      public int read() throws IOException {
                          return usePrimary ? primary.read() : fallback.read();
                      }
                  }
                  """
              )
            );
        }
    }
}
