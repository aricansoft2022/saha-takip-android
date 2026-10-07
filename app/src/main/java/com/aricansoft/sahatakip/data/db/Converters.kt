package com.aricansoft.sahatakip.data.db

import androidx.room.TypeConverter
import com.aricansoft.sahatakip.data.model.ControlStatus
import com.aricansoft.sahatakip.data.model.DeficiencyPriority
import com.aricansoft.sahatakip.data.model.DeficiencyStatus
import com.aricansoft.sahatakip.data.model.FindingKind
import com.aricansoft.sahatakip.data.model.ProblemRecordStatus
import com.aricansoft.sahatakip.data.model.ProgressStatus
import com.aricansoft.sahatakip.data.model.QualityStatus
import com.aricansoft.sahatakip.data.model.WorkItemKind

class Converters {
    @TypeConverter fun progressToString(value: ProgressStatus) = value.name
    @TypeConverter fun stringToProgress(value: String) = ProgressStatus.valueOf(value)
    @TypeConverter fun qualityToString(value: QualityStatus) = value.name
    @TypeConverter fun stringToQuality(value: String) = QualityStatus.valueOf(value)
    @TypeConverter fun controlToString(value: ControlStatus) = value.name
    @TypeConverter fun stringToControl(value: String) = ControlStatus.valueOf(value)
    @TypeConverter fun problemStatusToString(value: ProblemRecordStatus) = value.name
    @TypeConverter fun stringToProblemStatus(value: String) = ProblemRecordStatus.valueOf(value)
    @TypeConverter fun workItemKindToString(value: WorkItemKind) = value.name
    @TypeConverter fun stringToWorkItemKind(value: String) = WorkItemKind.valueOf(value)
    @TypeConverter fun findingKindToString(value: FindingKind) = value.name
    @TypeConverter fun stringToFindingKind(value: String) = FindingKind.valueOf(value)
    @TypeConverter fun deficiencyStatusToString(value: DeficiencyStatus) = value.name
    @TypeConverter fun stringToDeficiencyStatus(value: String) = DeficiencyStatus.valueOf(value)
    @TypeConverter fun deficiencyPriorityToString(value: DeficiencyPriority) = value.name
    @TypeConverter fun stringToDeficiencyPriority(value: String) = DeficiencyPriority.valueOf(value)
}
