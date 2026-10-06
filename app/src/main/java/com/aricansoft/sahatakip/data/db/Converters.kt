package com.aricansoft.sahatakip.data.db

import androidx.room.TypeConverter
import com.aricansoft.sahatakip.data.model.ControlStatus
import com.aricansoft.sahatakip.data.model.ProblemRecordStatus
import com.aricansoft.sahatakip.data.model.ProgressStatus
import com.aricansoft.sahatakip.data.model.QualityStatus

class Converters {
    @TypeConverter fun progressToString(value: ProgressStatus) = value.name
    @TypeConverter fun stringToProgress(value: String) = ProgressStatus.valueOf(value)
    @TypeConverter fun qualityToString(value: QualityStatus) = value.name
    @TypeConverter fun stringToQuality(value: String) = QualityStatus.valueOf(value)
    @TypeConverter fun controlToString(value: ControlStatus) = value.name
    @TypeConverter fun stringToControl(value: String) = ControlStatus.valueOf(value)
    @TypeConverter fun problemStatusToString(value: ProblemRecordStatus) = value.name
    @TypeConverter fun stringToProblemStatus(value: String) = ProblemRecordStatus.valueOf(value)
}
