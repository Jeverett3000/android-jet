/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tools.idea.gradle.project.sync.idea.issues;

import static com.android.tools.idea.gradle.project.sync.SimulatedSyncErrors.registerSyncErrorToSimulate;
import static com.android.tools.idea.testing.TestProjectPaths.SIMPLE_APPLICATION;
import static com.google.common.truth.Truth.assertThat;
import static com.google.wireless.android.sdk.stats.AndroidStudioEvent.GradleSyncFailure.JDK8_REQUIRED;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.android.tools.idea.IdeInfo;
import com.android.tools.idea.flags.StudioFlags;
import com.android.tools.idea.gradle.project.sync.issues.TestSyncIssueUsageReporter;
import com.android.tools.idea.sdk.IdeSdks;
import com.android.tools.idea.sdk.Jdks;
import com.android.tools.idea.testing.AndroidGradleTestCase;
import com.android.tools.idea.testing.IdeComponents;
import com.intellij.openapi.projectRoots.JavaSdkVersion;
import com.intellij.openapi.projectRoots.Sdk;
import java.nio.file.Paths;
import org.jetbrains.annotations.NotNull;

/**
 * Tests for {@link JdkImportCheck#validateJdk()}.
 */
public class JdkImportCheckTest extends AndroidGradleTestCase {
  private IdeSdks myMockIdeSdks;
  private Jdks myMockJdks;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    loadSimpleApplication();

    IdeComponents ideComponents = new IdeComponents(getProject(), getTestRootDisposable());
    myMockIdeSdks = ideComponents.mockApplicationService(IdeSdks.class);
    myMockJdks = ideComponents.mockApplicationService(Jdks.class);
    assertSame(myMockIdeSdks, IdeSdks.getInstance());

    StudioFlags.ALLOW_DIFFERENT_JDK_VERSION.override(false);
  }

  @Override
  public void tearDown() throws Exception {
    StudioFlags.ALLOW_DIFFERENT_JDK_VERSION.clearOverride();
    super.tearDown();
  }

  public void testDoCheckCanSyncWithNullJdk() {
    when(myMockIdeSdks.getJdk()).thenReturn(null);

    assertThat(runValidateJdkAndGetMessage()).startsWith("Jdk location is not set");
  }

  public void testDoCheckWithJdkWithoutHomePath() {
    Sdk jdk = mock(Sdk.class);

    when(myMockIdeSdks.getJdk()).thenReturn(jdk);
    when(jdk.getHomePath()).thenReturn(null);

    assertThat(runValidateJdkAndGetMessage()).startsWith("Could not find valid Jdk home from the selected Jdk location");
  }

  public void testDoCheckWithJdkWithIncompatibleVersion() {
    Sdk jdk = mock(Sdk.class);
    String pathToJdk10 = "/path/to/jdk10";

    when(myMockIdeSdks.getJdk()).thenReturn(jdk);
    when(jdk.getHomePath()).thenReturn(pathToJdk10);
    when(myMockIdeSdks.getRunningVersionOrDefault()).thenReturn(JavaSdkVersion.JDK_1_8);
    when(myMockJdks.findVersion(Paths.get(pathToJdk10))).thenReturn(JavaSdkVersion.JDK_10);

    assertThat(runValidateJdkAndGetMessage()).startsWith(
      "The version of selected Jdk doesn't match the Jdk used by Studio. Please choose a valid Jdk 8 directory.\n" +
      "Selected Jdk location is /path/to/jdk10."
    );
  }

  public void testDoCheckWithJdkWithIncompatibleVersionNoCheck() {
    StudioFlags.ALLOW_DIFFERENT_JDK_VERSION.override(true);
    Sdk jdk = mock(Sdk.class);
    String pathToJdk10 = "/path/to/jdk10";

    when(myMockIdeSdks.getJdk()).thenReturn(jdk);
    when(jdk.getHomePath()).thenReturn(pathToJdk10);
    when(myMockIdeSdks.getRunningVersionOrDefault()).thenReturn(JavaSdkVersion.JDK_1_8);
    when(myMockJdks.findVersion(Paths.get(pathToJdk10))).thenReturn(JavaSdkVersion.JDK_10);

    assertThat(runValidateJdkAndGetMessage()).startsWith(
      "The Jdk installation is invalid.\n" +
      "Selected Jdk location is /path/to/jdk10."
    );
  }

  public void testDoCheckWithJdkWithInvalidJdkInstallation() {
    Sdk jdk = mock(Sdk.class);
    String pathToJdk8 = "/path/to/jdk8";

    when(myMockIdeSdks.getJdk()).thenReturn(jdk);
    when(jdk.getHomePath()).thenReturn(pathToJdk8);
    when(myMockIdeSdks.getRunningVersionOrDefault()).thenReturn(JavaSdkVersion.JDK_1_8);
    when(myMockJdks.findVersion(Paths.get(pathToJdk8))).thenReturn(JavaSdkVersion.JDK_1_8);
    when(myMockIdeSdks.getJdk()).thenReturn(jdk);
    when(jdk.getHomePath()).thenReturn("/path/to/jdk8");

    assertThat(runValidateJdkAndGetMessage()).startsWith(
      "The Jdk installation is invalid.\n" +
      "Selected Jdk location is /path/to/jdk8."
    );
  }

  private static String runValidateJdkAndGetMessage() {
    try {
      JdkImportCheck.validateJdk();
      return ""; // No error
    } catch (JdkImportCheckException e) {
      return e.getMessage();
    }
  }
}
