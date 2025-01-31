/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.tools.idea.run.editor;

import com.android.tools.idea.flags.StudioFlags;
import com.android.tools.idea.run.AndroidRunConfiguration;
import com.android.tools.idea.run.ApkProvider;
import com.android.tools.idea.run.ValidationError;
import com.android.tools.idea.run.activity.*;
import com.android.tools.idea.run.tasks.DefaultActivityLaunchTask;
import com.android.tools.idea.run.tasks.LaunchTask;
import com.google.common.collect.ImmutableList;
import com.intellij.openapi.project.Project;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

public class DefaultActivityLaunch extends LaunchOption<DefaultActivityLaunch.State> {
  public static final DefaultActivityLaunch INSTANCE = new DefaultActivityLaunch();

  public static final class State extends LaunchOptionState {
    @Nullable
    @Override
    public LaunchTask getLaunchTask(@NotNull String applicationId,
                                    @NotNull AndroidFacet facet,
                                    @NotNull StartActivityFlagsProvider startActivityFlagsProvider,
                                    @NotNull ProfilerState profilerState,
                                    @NotNull ApkProvider apkProvider) {
      return new DefaultActivityLaunchTask(applicationId, getActivityLocatorForLaunch(facet, apkProvider), startActivityFlagsProvider);
    }

    @NotNull
    @Override
    public List<ValidationError> checkConfiguration(@NotNull AndroidFacet facet) {
      // Neither MavenDefaultActivityLocator nor DefaultApkActivityLocator can validate
      // based on Facets. There is no point calling validate().
      return ImmutableList.of();
    }

    @NotNull
    private static ActivityLocator getActivityLocatorForLaunch(@NotNull AndroidFacet facet,
                                                               @NotNull ApkProvider apkProvider) {
      if (facet.getProperties().USE_CUSTOM_COMPILER_MANIFEST) {
        return new MavenDefaultActivityLocator(facet);
      }

      if (StudioFlags.DEFAULT_ACTIVITY_LOCATOR_FROM_APKS.get()) {
         return new DefaultApkActivityLocator(apkProvider);
      } else {
        return new DefaultActivityLocator(facet);
      }
    }
  }

  @NotNull
  @Override
  public String getId() {
    return AndroidRunConfiguration.LAUNCH_DEFAULT_ACTIVITY;
  }

  @NotNull
  @Override
  public String getDisplayName() {
    return "Default Activity";
  }

  @NotNull
  @Override
  public State createState() {
    // there is no state to save in this case
    return new State();
  }

  @NotNull
  @Override
  public LaunchOptionConfigurable<State> createConfigurable(@NotNull Project project, @NotNull LaunchOptionConfigurableContext context) {
    return new LaunchOptionConfigurable<State>() {
      @Nullable
      @Override
      public JComponent createComponent() {
        return null;
      }

      @Override
      public void resetFrom(@NotNull State state) {
      }

      @Override
      public void applyTo(@NotNull State state) {
      }
    };
  }
}
