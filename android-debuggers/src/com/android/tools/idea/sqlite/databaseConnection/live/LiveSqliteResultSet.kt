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
package com.android.tools.idea.sqlite.databaseConnection.live

import androidx.sqlite.inspection.SqliteInspectorProtocol
import com.android.tools.idea.concurrency.cancelOnDispose
import com.android.tools.idea.sqlite.DatabaseInspectorMessenger
import com.android.tools.idea.sqlite.databaseConnection.SqliteResultSet
import com.android.tools.idea.sqlite.model.SqliteStatement
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor

/**
 * [SqliteResultSet] for live connections.
 *
 * @param sqliteStatement The original [SqliteStatement] this result set is for.
 * @param messenger Used to send messages to an on-device inspector.
 * @param taskExecutor Used to execute IO operation on a background thread.
 */
abstract class LiveSqliteResultSet(
  private val sqliteStatement: SqliteStatement,
  private val messenger: DatabaseInspectorMessenger,
  private val connectionId: Int,
  private val taskExecutor: Executor
) : SqliteResultSet {

  protected fun sendQueryCommand(sqliteStatement: SqliteStatement): ListenableFuture<SqliteInspectorProtocol.Response> {
    val queryCommand = buildQueryCommand(sqliteStatement, connectionId)
    return messenger.sendCommand(queryCommand).cancelOnDispose(this)
  }

  override fun dispose() { }
}