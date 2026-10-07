package com.aricansoft.sahatakip.ui.screens

import com.aricansoft.sahatakip.data.db.ProjectQuickStatusRow
import com.aricansoft.sahatakip.data.db.ReportWorkItemRow
import com.aricansoft.sahatakip.data.model.WorkItemKind
import java.util.Locale

internal enum class HomeQuickFilter(val label:String){
    ALL("Tümü"),
    OPEN_PROBLEM("Açık problem"),
    OPEN_DEFICIENCY("Açık eksik"),
    OPEN_ADVANTAGE("Açık avantaj"),
    DEFECTIVE("Kusurlu"),
    BLOCKED("Bloke"),
    IN_PROGRESS("Devam"),
    FINISHED("Bitti")
}

internal data class HomeStatusCounts(
    val total:Int,
    val inProgress:Int,
    val finished:Int,
    val defective:Int,
    val blocked:Int,
    val openProblems:Int,
    val openDeficiencies:Int,
    val openAdvantages:Int
)

internal data class ProjectWorkItemOption(
    val key:String,
    val label:String,
    val kind:WorkItemKind,
    val blockCount:Int
)

internal data class ProjectFilterOption(
    val key:String,
    val label:String,
    val blockCount:Int
)

internal val trLocale:Locale=Locale.forLanguageTag("tr-TR")

internal fun ProjectQuickStatusRow.asHomeCounts()=HomeStatusCounts(
    total=totalWorkItemCount,
    inProgress=inProgressCount,
    finished=finishedCount,
    defective=defectiveCount,
    blocked=blockedCount,
    openProblems=openProblemCount,
    openDeficiencies=openDeficiencyCount,
    openAdvantages=openAdvantageCount
)

internal fun ReportWorkItemRow.asHomeCounts()=HomeStatusCounts(
    total=1,
    inProgress=if(progressStatus.name=="IN_PROGRESS")1 else 0,
    finished=if(progressStatus.name=="FINISHED")1 else 0,
    defective=if(qualityStatus.name=="DEFECTIVE" || qualityStatus.name=="CRITICAL_DEFECT")1 else 0,
    blocked=if(isBlocked)1 else 0,
    openProblems=openProblemCount,
    openDeficiencies=openDeficiencyCount,
    openAdvantages=openAdvantageCount
)

internal fun HomeStatusCounts.matches(filter:HomeQuickFilter)=when(filter){
    HomeQuickFilter.ALL -> total>0
    HomeQuickFilter.OPEN_PROBLEM -> openProblems>0
    HomeQuickFilter.OPEN_DEFICIENCY -> openDeficiencies>0
    HomeQuickFilter.OPEN_ADVANTAGE -> openAdvantages>0
    HomeQuickFilter.DEFECTIVE -> defective>0
    HomeQuickFilter.BLOCKED -> blocked>0
    HomeQuickFilter.IN_PROGRESS -> inProgress>0
    HomeQuickFilter.FINISHED -> finished>0
}

internal fun HomeStatusCounts.summaryText():String=buildList{
    if(openProblems>0) add("P $openProblems")
    if(openDeficiencies>0) add("Eksik $openDeficiencies")
    if(openAdvantages>0) add("A $openAdvantages")
    if(defective>0) add("Kusurlu $defective")
    if(blocked>0) add("Bloke $blocked")
    if(inProgress>0) add("Devam $inProgress")
    if(finished>0) add("Bitti $finished")
}.joinToString(" · ")
