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
package com.android.tools.idea.common.property2.impl.model.util

import com.android.SdkConstants
import com.android.tools.idea.common.property2.api.ActionIconButton
import com.android.tools.idea.common.property2.api.PropertyItem
import com.android.utils.HashCodes
import icons.StudioIcons
import javax.swing.Icon

/**
 * [PropertyItem] used in tests.
 */
open class TestPropertyItem(
  override var namespace: String,
  override var name: String,
  initialValue: String? = null,
  override var browseButton: ActionIconButton? = null,
  override var colorButton: ActionIconButton? = null
) : PropertyItem {

  override var isReference: Boolean = false

  override val namespaceIcon: Icon?
    get() = if (namespace == SdkConstants.TOOLS_URI) StudioIcons.LayoutEditor.Properties.TOOLS_ATTRIBUTE else null

  override var value: String? = initialValue
    set(value) {
      field = value
      resolvedValue = value
      updateCount++
    }

  override var defaultValue: String? = null

  override var resolvedValue: String? = initialValue

  var updateCount = 0
    protected set

  override fun equals(other: Any?) =
    when (other) {
      is TestPropertyItem -> namespace == other.namespace && name == other.name
      else -> false
    }

  override fun hashCode() = HashCodes.mix(namespace.hashCode(), name.hashCode())
}
