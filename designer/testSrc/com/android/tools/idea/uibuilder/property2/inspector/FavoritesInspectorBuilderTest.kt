/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.tools.idea.uibuilder.property2.inspector

import com.android.SdkConstants.ANDROID_URI
import com.android.SdkConstants.ATTR_ALPHA
import com.android.SdkConstants.ATTR_GRAVITY
import com.android.SdkConstants.ATTR_TEXT
import com.android.SdkConstants.ATTR_VISIBILITY
import com.android.SdkConstants.TEXT_VIEW
import com.android.ide.common.rendering.api.ResourceNamespace
import com.android.ide.common.rendering.api.ResourceReference
import com.android.tools.adtui.workbench.PropertiesComponentMock
import com.android.tools.idea.testing.AndroidProjectRule
import com.android.tools.idea.testing.addManifest
import com.android.tools.idea.uibuilder.property2.NeleNewPropertyItem
import com.android.tools.idea.uibuilder.property2.NelePropertyItem
import com.android.tools.idea.uibuilder.property2.testutils.InspectorTestUtil
import com.android.tools.property.panel.api.EnumSupport
import com.android.tools.property.panel.api.EnumSupportProvider
import com.google.common.truth.Truth.assertThat
import com.intellij.ide.util.PropertiesComponent
import com.intellij.testFramework.EdtRule
import com.intellij.testFramework.RunsInEdt
import icons.StudioIcons
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@RunsInEdt
class FavoritesInspectorBuilderTest {

  @get:Rule
  val projectRule = AndroidProjectRule.withSdk()

  @get:Rule
  val edtRule = EdtRule()

  @Before
  fun setUp() {
    addManifest(projectRule.fixture)
    projectRule.replaceService(PropertiesComponent::class.java, PropertiesComponentMock())
    InspectorSection.FAVORITES.visible = true
  }

  private val enumSupportProvider = object : EnumSupportProvider<NelePropertyItem> {
    override fun invoke(property: NelePropertyItem): EnumSupport? {
      return null
    }
  }

  @Test
  fun testEmptyFavorites() {
    val util = InspectorTestUtil(projectRule, TEXT_VIEW)
    val builder = FavoritesInspectorBuilder(util.model, enumSupportProvider)
    assertThat(builder.loadFavoritePropertiesIfNeeded()).isEmpty()
  }

  @Test
  fun testFavoritesParser() {
    PropertiesComponent.getInstance().setValue(FAVORITES_PROPERTY, ":alpha;tools:gravity;:visibility;", "")
    val util = InspectorTestUtil(projectRule, TEXT_VIEW)
    val builder = FavoritesInspectorBuilder(util.model, enumSupportProvider)
    assertThat(builder.loadFavoritePropertiesIfNeeded()).containsExactly(
      ResourceReference.attr(ResourceNamespace.ANDROID, ATTR_ALPHA),
      ResourceReference.attr(ResourceNamespace.TOOLS, ATTR_GRAVITY),
      ResourceReference.attr(ResourceNamespace.ANDROID, ATTR_VISIBILITY)).inOrder()
  }

  @Test
  fun testTextView() {
    PropertiesComponent.getInstance().setValue(FAVORITES_PROPERTY, ":alpha;tools:gravity;:visibility;:non_exist;", "")
    val util = InspectorTestUtil(projectRule, TEXT_VIEW)
    val builder = FavoritesInspectorBuilder(util.model, enumSupportProvider)
    util.loadProperties()
    builder.attachToInspector(util.inspector, util.properties)
    util.checkTitle(0, InspectorSection.FAVORITES.title)
    val lineModel = util.checkTable(1)
    lineModel.checkItem(0, ANDROID_URI, ATTR_ALPHA)
    lineModel.checkItem(1, ANDROID_URI, ATTR_VISIBILITY)
    lineModel.checkItemCount(2)
  }

  @Test
  fun testAddFavorite() {
    PropertiesComponent.getInstance().setValue(FAVORITES_PROPERTY, ":alpha;tools:gravity;:visibility;:non_exist;", "")
    val util = InspectorTestUtil(projectRule, TEXT_VIEW)
    val builder = FavoritesInspectorBuilder(util.model, enumSupportProvider)
    util.loadProperties()
    builder.attachToInspector(util.inspector, util.properties)
    util.performAction(0, 0, StudioIcons.Common.ADD)

    // Check that there are 3 attributes (one placeholder)
    val lineModel = util.checkTable(1)
    lineModel.checkItem(0, ANDROID_URI, ATTR_ALPHA)
    lineModel.checkItem(1, ANDROID_URI, ATTR_VISIBILITY)
    lineModel.checkItem(2, "", "")  // placeholder
    lineModel.checkItemCount(3)
  }

  @Test
  fun testAddFavoriteAndSelectProperty() {
    PropertiesComponent.getInstance().setValue(FAVORITES_PROPERTY, ":alpha;tools:gravity;:visibility;", "")
    val util = InspectorTestUtil(projectRule, TEXT_VIEW)
    val builder = FavoritesInspectorBuilder(util.model, enumSupportProvider)
    util.loadProperties()
    builder.attachToInspector(util.inspector, util.properties)
    util.performAction(0, 0, StudioIcons.Common.ADD)
    val newItem = util.checkTable(1).tableModel.items[2] as NeleNewPropertyItem

    // Select a new favorite property:
    newItem.name = "android:text"
    val lineModel = util.checkTable(1)
    lineModel.checkItem(0, ANDROID_URI, ATTR_ALPHA)
    lineModel.checkItem(1, ANDROID_URI, ATTR_TEXT)
    lineModel.checkItem(2, ANDROID_URI, ATTR_VISIBILITY)
    lineModel.checkItem(3, "", "")  // placeholder
    lineModel.checkItemCount(4)
    assertThat(PropertiesComponent.getInstance().getValue(FAVORITES_PROPERTY)).isEqualTo(":alpha;tools:gravity;:visibility;:text;")
  }

  @Test
  fun testRemoveFavorite() {
    PropertiesComponent.getInstance().setValue(FAVORITES_PROPERTY, ":alpha;tools:gravity;:visibility;", "")
    val util = InspectorTestUtil(projectRule, TEXT_VIEW)
    val builder = FavoritesInspectorBuilder(util.model, enumSupportProvider)
    util.loadProperties()
    builder.attachToInspector(util.inspector, util.properties)
    val lineModel = util.checkTable(1)
    lineModel.selectedItem = lineModel.tableModel.items[0] // select ATTR_ALPHA
    util.performAction(0, 1, StudioIcons.Common.REMOVE)
    lineModel.checkItem(0, ANDROID_URI, ATTR_VISIBILITY)
    lineModel.checkItemCount(1)
    assertThat(PropertiesComponent.getInstance().getValue(FAVORITES_PROPERTY)).isEqualTo("tools:gravity;:visibility;")
  }
}
