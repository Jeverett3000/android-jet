/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.tools.idea.common.editor;

import static com.android.tools.idea.common.model.NlModel.DELAY_AFTER_TYPING_MS;
import static com.intellij.util.Alarm.ThreadToUse.SWING_THREAD;

import com.android.tools.idea.AndroidPsiUtils;
import com.android.tools.idea.common.model.NlComponent;
import com.android.tools.idea.common.model.NlModel;
import com.android.tools.idea.common.model.SelectionModel;
import com.android.tools.idea.common.surface.DesignSurface;
import com.android.tools.idea.common.surface.SceneView;
import com.android.tools.idea.flags.StudioFlags;
import com.google.common.collect.ImmutableList;
import com.intellij.codeInsight.hint.ImplementationViewComponent;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.ui.update.MergingUpdateQueue;
import com.intellij.util.ui.update.Update;
import java.util.Arrays;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.uipreview.AndroidEditorSettings;
import org.jetbrains.annotations.NotNull;

/**
 * Provider that accepts {@link XmlFile}s whose modules contain an {@link AndroidFacet}. Subclasses can be more restrictive with the
 * accepted files by overriding {@link #acceptAndroidFacetXml(XmlFile)}. They're also responsible for creating their editor using
 * {@link #createEditor(Project, VirtualFile)}, and specifying their ID via {@link #getEditorTypeId()}.
 */
public abstract class DesignerEditorProvider implements FileEditorProvider, DumbAware {

  /**
   * Name of the class that handles the quick definition feature in IntelliJ.
   * This class should be used by quick definition only.
   */
  private static final String CALLER_NAME_OF_QUICK_DEFINITION = ImplementationViewComponent.class.getName();

  @Override
  public boolean accept(@NotNull Project project, @NotNull VirtualFile virtualFile) {
    PsiFile psiFile = AndroidPsiUtils.getPsiFileSafely(project, virtualFile);
    if (psiFile instanceof XmlFile) {
      XmlFile xmlFile = (XmlFile) psiFile;
      AndroidFacet facet = AndroidFacet.getInstance(xmlFile);
      return facet != null && acceptAndroidFacetXml(xmlFile);
    }
    return false;
  }

  @NotNull
  @Override
  public final FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
    DesignerEditor designEditor = createDesignEditor(project, file);
    if (!StudioFlags.NELE_SPLIT_EDITOR.get()) {
      return designEditor;
    }
    TextEditor textEditor = (TextEditor)TextEditorProvider.getInstance().createEditor(project, file);
    addCaretListener(textEditor, designEditor);
    return new SplitEditor(textEditor, designEditor, "Design", project);
  }

  private static void addCaretListener(@NotNull TextEditor editor, @NotNull DesignerEditor designEditor) {
    CaretModel caretModel = editor.getEditor().getCaretModel();
    MergingUpdateQueue updateQueue = new MergingUpdateQueue("split.editor.preview.edit", DELAY_AFTER_TYPING_MS,
                                                            true, null, designEditor, null, SWING_THREAD);
    updateQueue.setRestartTimerOnAdd(true);
    caretModel.addCaretListener(new CaretListener() {
      @Override
      public void caretPositionChanged(@NotNull CaretEvent event) {
        DesignSurface surface = designEditor.getComponent().getSurface();
        SceneView sceneView = surface.getCurrentSceneView();
        int offset = caretModel.getOffset();
        if (sceneView == null || offset == -1) {
          return;
        }

        NlModel model = sceneView.getSceneManager().getModel();
        ImmutableList<NlComponent> views = model.findByOffset(offset);
        if (views.isEmpty()) {
          views = model.getComponents();
        }
        // TODO: handle preference screen intent special case if needed.
        SelectionModel selectionModel = sceneView.getSelectionModel();
        selectionModel.setSelection(views);
        updateQueue.queue(new Update("Design editor update") {
          @Override
          public void run() {
            surface.repaint();
          }

          @Override
          public boolean canEat(Update update) {
            return true;
          }
        });
      }
    });
  }

  @NotNull
  public abstract DesignerEditor createDesignEditor(@NotNull Project project, @NotNull VirtualFile file);

  @NotNull
  @Override
  public abstract String getEditorTypeId();

  /**
   * Condition to accept a given {@link XmlFile} whose module contains an {@link AndroidFacet}.
   */
  protected abstract boolean acceptAndroidFacetXml(@NotNull XmlFile xmlFile);

  @NotNull
  @Override
  public FileEditorPolicy getPolicy() {
    if (StudioFlags.NELE_SPLIT_EDITOR.get()) {
      // When using the split editor, we hide the default one since the split editor already includes the text-only view.
      return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }

    if (AndroidEditorSettings.getInstance().getGlobalState().isPreferXmlEditor()) {
      return FileEditorPolicy.PLACE_AFTER_DEFAULT_EDITOR;
    }
    if (Arrays.stream(Thread.currentThread().getStackTrace())
      .anyMatch(element -> CALLER_NAME_OF_QUICK_DEFINITION.equals(element.getClassName()))) {
      // This function is called by quick definition, make the default editor as preferred editor.
      // This is a hack fixed for http://b/37050828
      return FileEditorPolicy.PLACE_AFTER_DEFAULT_EDITOR;
    }

    return FileEditorPolicy.PLACE_BEFORE_DEFAULT_EDITOR;
  }
}
