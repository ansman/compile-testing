/*
 * Copyright (C) 2016 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.testing.compile;

import static com.google.common.truth.Truth.assertThat;
import static com.google.testing.compile.JavaFileObjectSubject.assertThat;
import static com.google.testing.compile.JavaFileObjectSubject.javaFileObjects;
import static com.google.testing.compile.VerificationFailureStrategy.VERIFY;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.fail;

import com.google.testing.compile.VerificationFailureStrategy.VerificationException;
import java.io.IOException;
import java.util.regex.Pattern;
import javax.tools.JavaFileObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class JavaFileObjectSubjectTest {

  private static final JavaFileObject CLASS =
      JavaFileObjects.forSourceLines(
          "test.TestClass", //
          "package test;",
          "",
          "public class TestClass {}");

  private static final JavaFileObject DIFFERENT_NAME =
      JavaFileObjects.forSourceLines(
          "test.TestClass2", //
          "package test;",
          "",
          "public class TestClass2 {}");

  private static final JavaFileObject CLASS_WITH_FIELD =
      JavaFileObjects.forSourceLines(
          "test.TestClass", //
          "package test;",
          "",
          "public class TestClass {",
          "  Object field;",
          "}");

  private static final JavaFileObject UNKNOWN_TYPES =
      JavaFileObjects.forSourceLines(
          "test.TestClass",
          "package test;",
          "",
          "public class TestClass {",
          "  Bar badMethod(Baz baz) { return baz.what(); }",
          "}");

  @Test
  public void hasContents() {
    assertThat(CLASS_WITH_FIELD).hasContents(JavaFileObjects.asByteSource(CLASS_WITH_FIELD));
  }

  @Test
  public void hasContents_failure() {
    try {
      verifyThat(CLASS_WITH_FIELD).hasContents(JavaFileObjects.asByteSource(DIFFERENT_NAME));
      fail();
    } catch (VerificationException expected) {
      assertThat(expected.getMessage()).contains(CLASS_WITH_FIELD.getName());
    }
  }

  @Test
  public void contentsAsString() {
    assertThat(CLASS_WITH_FIELD).contentsAsString(UTF_8).containsMatch("Object +field;");
  }

  @Test
  public void contentsAsString_fail() {
    try {
      verifyThat(CLASS).contentsAsString(UTF_8).containsMatch("bad+");
      fail();
    } catch (VerificationException expected) {
      assertThat(expected.getMessage())
          .containsMatch("the contents of .*" + Pattern.quote(CLASS.getName()));
      assertThat(expected.getMessage()).contains("bad+");
    }
  }

  @Test
  public void hasSourceEquivalentTo() {
    assertThat(CLASS_WITH_FIELD).hasSourceEquivalentTo(CLASS_WITH_FIELD);
  }

  @Test
  public void hasSourceEquivalentTo_unresolvedReferences() {
    assertThat(UNKNOWN_TYPES).hasSourceEquivalentTo(UNKNOWN_TYPES);
  }

  @Test
  public void hasSourceEquivalentTo_failOnDifferences() throws IOException {
    try {
      verifyThat(CLASS).hasSourceEquivalentTo(DIFFERENT_NAME);
      fail();
    } catch (VerificationException expected) {
      assertThat(expected.getMessage()).contains("is equivalent to");
      assertThat(expected.getMessage()).contains(CLASS.getName());
      assertThat(expected.getMessage()).contains(CLASS.getCharContent(false));
    }
  }

  @Test
  public void hasSourceEquivalentTo_failOnExtraInExpected() throws IOException {
    try {
      verifyThat(CLASS).hasSourceEquivalentTo(CLASS_WITH_FIELD);
      fail();
    } catch (VerificationException expected) {
      assertThat(expected.getMessage()).contains("is equivalent to");
      assertThat(expected.getMessage()).contains("unmatched nodes in the expected tree");
      assertThat(expected.getMessage()).contains(CLASS.getName());
      assertThat(expected.getMessage()).contains(CLASS.getCharContent(false));
    }
  }

  @Test
  public void hasSourceEquivalentTo_failOnExtraInActual() throws IOException {
    try {
      verifyThat(CLASS_WITH_FIELD).hasSourceEquivalentTo(CLASS);
      fail();
    } catch (VerificationException expected) {
      assertThat(expected.getMessage()).contains("is equivalent to");
      assertThat(expected.getMessage()).contains("unmatched nodes in the actual tree");
      assertThat(expected.getMessage()).contains(CLASS_WITH_FIELD.getName());
      assertThat(expected.getMessage()).contains(CLASS_WITH_FIELD.getCharContent(false));
    }
  }

  private static JavaFileObjectSubject verifyThat(JavaFileObject file) {
    return VERIFY.about(javaFileObjects()).that(file);
  }
}
