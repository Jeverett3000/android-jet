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
import com.android.tools.idea.appinspection.inspector.api.AppInspectorClient
import com.android.tools.idea.concurrency.FutureCallbackExecutor
import com.android.tools.idea.sqlite.databaseConnection.SqliteResultSet
import com.android.tools.idea.sqlite.databaseConnection.checkOffsetAndSize
import com.android.tools.idea.sqlite.model.SqliteColumn
import com.android.tools.idea.sqlite.model.SqliteRow
import com.android.tools.idea.sqlite.model.SqliteStatement
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.intellij.openapi.util.Disposer
import java.util.concurrent.Executor

/**
 * [SqliteResultSet] for live connections.
 *
 * @param _columns The list of columns for this result set.
 * @param sqliteStatement The original [SqliteStatement] this result set is for.
 * @param messenger Used to send messages to an on-device inspector.
 * @param executor Used to execute IO operation on a background thread.
 */
class LiveSqliteResultSet(
  _columns: List<SqliteColumn>,
  private val sqliteStatement: SqliteStatement,
  private val messenger: AppInspectorClient.CommandMessenger,
  private val connectionId: Int,
  executor: Executor
) : SqliteResultSet {
  private val taskExecutor = FutureCallbackExecutor.wrap(executor)

  override val columns: ListenableFuture<List<SqliteColumn>> = Futures.immediateFuture(_columns)

  override val rowCount: ListenableFuture<Int> get() {
    val queryCommand = buildQueryCommand(sqliteStatement.toRowCountStatement(), connectionId)
    val responseFuture = messenger.sendRawCommand(queryCommand.toByteArray())

    return taskExecutor.transform(responseFuture) {
      check(!Disposer.isDisposed(this)) { "ResultSet has already been disposed." }

      val queryResponse = SqliteInspectorProtocol.Response.parseFrom(it).query
      queryResponse.rowsList.firstOrNull()?.valuesList?.size ?: 0
    }
  }

  override fun getRowBatch(rowOffset: Int, rowBatchSize: Int): ListenableFuture<List<SqliteRow>> {
    checkOffsetAndSize(rowOffset, rowBatchSize)

    val queryCommand = buildQueryCommand(sqliteStatement.toSelectLimitOffset(rowOffset, rowBatchSize), connectionId)
    val responseFuture = messenger.sendRawCommand(queryCommand.toByteArray())

    return taskExecutor.transform(responseFuture) { byteArray ->
      check(!Disposer.isDisposed(this)) { "ResultSet has already been disposed." }

      val queryResponse = SqliteInspectorProtocol.Response.parseFrom(byteArray).query
      val rows = queryResponse.rowsList.map {
        val sqliteColumnValues = it.valuesList.map { cellValue -> cellValue.toSqliteColumnValue() }
        SqliteRow(sqliteColumnValues)
      }

      rows
    }
  }

  override fun dispose() {
  }
}