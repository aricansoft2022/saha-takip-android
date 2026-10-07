package com.aricansoft.sahatakip.report

import com.aricansoft.sahatakip.data.db.ReportDeficiencyRow
import com.aricansoft.sahatakip.data.db.ReportNoteRow
import com.aricansoft.sahatakip.data.db.ReportPhotoRow
import com.aricansoft.sahatakip.data.db.ReportProblemRow
import com.aricansoft.sahatakip.data.db.ReportWorkItemRow

data class ProjectReportSnapshot(
    val projectId:String,
    val projectName:String,
    val generatedAt:Long,
    val workItems:List<ReportWorkItemRow>,
    val problems:List<ReportProblemRow>,
    val deficiencies:List<ReportDeficiencyRow>,
    val notes:List<ReportNoteRow>,
    val photos:List<ReportPhotoRow>
)
