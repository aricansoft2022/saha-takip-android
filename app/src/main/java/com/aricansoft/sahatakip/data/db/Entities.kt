package com.aricansoft.sahatakip.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aricansoft.sahatakip.data.model.ControlStatus
import com.aricansoft.sahatakip.data.model.ProblemRecordStatus
import com.aricansoft.sahatakip.data.model.ProgressStatus
import com.aricansoft.sahatakip.data.model.QualityStatus

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long
)

@Entity(
    tableName = "block_types",
    indices = [Index("projectId"), Index(value = ["projectId", "code"], unique = true)]
)
data class BlockTypeEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val code: String,
    val name: String,
    val tooltip: String? = null
)

@Entity(
    tableName = "blocks",
    indices = [Index("projectId"), Index("blockTypeId"), Index(value = ["projectId", "code"], unique = true)]
)
data class BlockEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val blockTypeId: String,
    val code: String,
    val sequence: Int
)

@Entity(
    tableName = "work_item_definitions",
    indices = [Index("projectId"), Index(value = ["projectId", "name"], unique = true)]
)
data class WorkItemDefinitionEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val code: String? = null,
    val name: String,
    val description: String? = null,
    val tooltip: String? = null,
    val active: Boolean = true
)

@Entity(
    tableName = "block_type_work_items",
    primaryKeys = ["blockTypeId", "workItemDefinitionId"],
    indices = [Index("workItemDefinitionId")]
)
data class BlockTypeWorkItemEntity(
    val blockTypeId: String,
    val workItemDefinitionId: String,
    val sortOrder: Int
)

@Entity(
    tableName = "block_work_items",
    indices = [
        Index("blockId"),
        Index("workItemDefinitionId"),
        Index(value = ["blockId", "workItemDefinitionId"], unique = true)
    ]
)
data class BlockWorkItemEntity(
    @PrimaryKey val id: String,
    val blockId: String,
    val workItemDefinitionId: String,
    val progressStatus: ProgressStatus = ProgressStatus.NOT_STARTED,
    val qualityStatus: QualityStatus = QualityStatus.NOT_EVALUATED,
    val controlStatus: ControlStatus = ControlStatus.NOT_CHECKED,
    val isBlocked: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "problem_definitions",
    indices = [Index("projectId"), Index(value = ["projectId", "code"], unique = true)]
)
data class ProblemDefinitionEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val code: String,
    val title: String,
    val description: String? = null,
    val tooltip: String? = null,
    val active: Boolean = true
)

@Entity(
    tableName = "problem_records",
    indices = [Index("blockWorkItemId"), Index("problemDefinitionId")]
)
data class ProblemRecordEntity(
    @PrimaryKey val id: String,
    val blockWorkItemId: String,
    val problemDefinitionId: String,
    val status: ProblemRecordStatus = ProblemRecordStatus.OPEN,
    val note: String? = null,
    val includeInReport: Boolean = true,
    val createdAt: Long,
    val closedAt: Long? = null
)

@Entity(
    tableName = "notes",
    indices = [Index("blockWorkItemId")]
)
data class NoteEntity(
    @PrimaryKey val id: String,
    val blockWorkItemId: String,
    val text: String,
    val includeInReport: Boolean,
    val createdAt: Long
)

@Entity(
    tableName = "photos",
    indices = [Index("blockWorkItemId")]
)
data class PhotoEntity(
    @PrimaryKey val id: String,
    val blockWorkItemId: String,
    val localUri: String,
    val caption: String? = null,
    val includeInReport: Boolean = true,
    val createdAt: Long
)

@Entity(
    tableName = "audit_events",
    indices = [Index("blockWorkItemId"), Index("createdAt")]
)
data class AuditEventEntity(
    @PrimaryKey val id: String,
    val blockWorkItemId: String,
    val eventType: String,
    val detail: String,
    val createdAt: Long
)
