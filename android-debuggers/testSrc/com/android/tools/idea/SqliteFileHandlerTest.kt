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
package com.android.tools.idea

import com.android.tools.idea.editors.sqlite.SqliteFileType
import com.google.common.truth.Truth.assertThat
import com.intellij.openapi.vfs.VirtualFile
import junit.framework.TestCase
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset

class SqliteFileHandlerTest : TestCase() {
  private lateinit var sqliteFileHandler: SqliteFileHandler
  private lateinit var file: VirtualFile

  override fun setUp() {
    super.setUp()
    sqliteFileHandler = SqliteFileHandler()

    file = mock(VirtualFile::class.java)
    `when`(file.fileType).thenReturn(SqliteFileType)
  }

  fun testReturnsShmAndWalFiles() {
    // Act
    val strings = sqliteFileHandler.getAdditionalDevicePaths("filePath", file)

    // Assert
    assertThat(strings).containsExactly("filePath-shm", "filePath-wal")
  }

  fun testDoesntReturnShmAndWalFilesWithNonSqliteFile() {
    // Prepare
    reset(file)

    // Act
    val strings = sqliteFileHandler.getAdditionalDevicePaths("filePath", file)

    // Assert
    assertThat(strings.isEmpty())
  }
}