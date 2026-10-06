package com.aricansoft.sahatakip.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aricansoft.sahatakip.data.SahaRepository
import com.aricansoft.sahatakip.data.db.ReportWorkItemRow
import com.aricansoft.sahatakip.data.model.ProgressStatus
import com.aricansoft.sahatakip.data.model.QualityStatus
import com.aricansoft.sahatakip.data.model.WorkItemKind

private val matrixAdvantageContainer=Color(0xFFE6F4D7)
private val matrixAdvantageContent=Color(0xFF285F16)

private enum class MatrixFilter(val label:String){
    ALL("Tümü"),
    OPEN_PROBLEM("Açık problem"),
    OPEN_ADVANTAGE("Açık avantaj"),
    RELATED_DISCIPLINE("Başka disiplin"),
    DEFECTIVE("Kusurlu"),
    BLOCKED("Bloke"),
    IN_PROGRESS("Devam"),
    FINISHED("Bitti")
}

private data class MatrixWorkRow(
    val name:String,
    val kind:WorkItemKind
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatrixScreen(
    repository:SahaRepository,
    projectId:String,
    onBack:()->Unit,
    onWorkItem:(String)->Unit
){
    val blocks by remember(projectId){repository.observeBlocks(projectId)}
        .collectAsStateWithLifecycle(initialValue=emptyList())
    val blockTypes by remember(projectId){repository.observeBlockTypes(projectId)}
        .collectAsStateWithLifecycle(initialValue=emptyList())
    val rows by remember(projectId){repository.observeProjectMatrixRows(projectId)}
        .collectAsStateWithLifecycle(initialValue=emptyList())

    var selectedBlockTypeId by rememberSaveable(projectId){mutableStateOf<String?>(null)}
    var selectedFilter by rememberSaveable(projectId){mutableStateOf(MatrixFilter.ALL)}

    LaunchedEffect(blockTypes,selectedBlockTypeId){
        if(selectedBlockTypeId!=null && blockTypes.none{it.id==selectedBlockTypeId}){
            selectedBlockTypeId=null
        }
    }

    val visibleBlocks=remember(blocks,selectedBlockTypeId){
        if(selectedBlockTypeId==null) blocks else blocks.filter{it.blockTypeId==selectedBlockTypeId}
    }
    val visibleBlockCodes=remember(visibleBlocks){visibleBlocks.map{it.code}.toSet()}
    val visibleRows=remember(rows,visibleBlockCodes,selectedFilter){
        rows.filter{row->
            row.blockCode in visibleBlockCodes && when(selectedFilter){
                MatrixFilter.ALL -> true
                MatrixFilter.OPEN_PROBLEM -> row.openProblemCount>0
                MatrixFilter.OPEN_ADVANTAGE -> row.openAdvantageCount>0
                MatrixFilter.RELATED_DISCIPLINE -> row.workItemKind==WorkItemKind.RELATED_DISCIPLINE
                MatrixFilter.DEFECTIVE ->
                    row.qualityStatus==QualityStatus.DEFECTIVE ||
                        row.qualityStatus==QualityStatus.CRITICAL_DEFECT
                MatrixFilter.BLOCKED -> row.isBlocked
                MatrixFilter.IN_PROGRESS -> row.progressStatus==ProgressStatus.IN_PROGRESS
                MatrixFilter.FINISHED -> row.progressStatus==ProgressStatus.FINISHED
            }
        }
    }
    val workRows=remember(visibleRows){
        visibleRows
            .groupBy{it.workItemName}
            .map{entry->MatrixWorkRow(entry.key,entry.value.first().workItemKind)}
            .sortedWith(
                compareBy<MatrixWorkRow>{if(it.kind==WorkItemKind.ELECTRICAL)0 else 1}
                    .thenBy{it.name}
            )
    }
    val cellMap=remember(visibleRows){
        visibleRows.associateBy{it.workItemName to it.blockCode}
    }
    val horizontal=rememberScrollState()
    val vertical=rememberScrollState()

    Scaffold(
        topBar={
            TopAppBar(
                title={Text("İmalat Matrisi")},
                navigationIcon={
                    IconButton(onClick=onBack){
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack,contentDescription="Geri")
                    }
                }
            )
        }
    ){padding->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ){
            MatrixFilters(
                blockTypes=blockTypes.map{it.id to it.code},
                selectedBlockTypeId=selectedBlockTypeId,
                onBlockTypeSelected={selectedBlockTypeId=it},
                selectedFilter=selectedFilter,
                onFilterSelected={selectedFilter=it}
            )

            HorizontalDivider()

            if(visibleBlocks.isEmpty()){
                Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){
                    Text("Seçili filtreye uygun blok yok.")
                }
            }else if(workRows.isEmpty()){
                Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){
                    Text("Seçili filtreye uygun imalat kaydı yok.")
                }
            }else{
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .horizontalScroll(horizontal)
                        .verticalScroll(vertical)
                        .padding(8.dp)
                ){
                    Column{
                        Row{
                            HeaderCell("İmalat",180.dp)
                            visibleBlocks.forEach{block->HeaderCell(block.code,72.dp)}
                        }
                        workRows.forEach{work->
                            Row{
                                WorkNameCell(work)
                                visibleBlocks.forEach{block->
                                    MatrixCell(
                                        row=cellMap[work.name to block.code],
                                        onClick=onWorkItem
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "✓ Bitti   ◐ Devam   ○ Başlanmadı   ! Kusurlu   !! Ağır kusurlu   B Bloke   P Açık problem   A Açık avantaj",
                            style=MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "Farklı zemin: alakadar başka disiplin kalemi",
                            style=MaterialTheme.typography.bodySmall,
                            color=MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MatrixFilters(
    blockTypes:List<Pair<String,String>>,
    selectedBlockTypeId:String?,
    onBlockTypeSelected:(String?)->Unit,
    selectedFilter:MatrixFilter,
    onFilterSelected:(MatrixFilter)->Unit
){
    Column(
        Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=8.dp),
        verticalArrangement=Arrangement.spacedBy(8.dp)
    ){
        Text("Blok tipi",style=MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
            item{
                FilterChip(
                    selected=selectedBlockTypeId==null,
                    onClick={onBlockTypeSelected(null)},
                    label={Text("Tümü")}
                )
            }
            items(blockTypes,key={it.first}){type->
                FilterChip(
                    selected=selectedBlockTypeId==type.first,
                    onClick={onBlockTypeSelected(type.first)},
                    label={Text(type.second)}
                )
            }
        }

        Text("Görünüm",style=MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
            items(MatrixFilter.entries,key={it.name}){filter->
                FilterChip(
                    selected=selectedFilter==filter,
                    onClick={onFilterSelected(filter)},
                    label={Text(filter.label)}
                )
            }
        }
    }
}

@Composable
private fun HeaderCell(text:String,width:androidx.compose.ui.unit.Dp){
    Box(
        Modifier
            .width(width)
            .height(44.dp)
            .border(0.5.dp,MaterialTheme.colorScheme.outlineVariant)
            .padding(4.dp),
        contentAlignment=Alignment.Center
    ){
        Text(
            text,
            style=MaterialTheme.typography.labelMedium,
            fontWeight=FontWeight.SemiBold,
            textAlign=TextAlign.Center
        )
    }
}

@Composable
private fun WorkNameCell(work:MatrixWorkRow){
    val related=work.kind==WorkItemKind.RELATED_DISCIPLINE
    Surface(
        color=if(related)MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surface,
        modifier=Modifier
            .width(180.dp)
            .height(52.dp)
            .border(0.5.dp,MaterialTheme.colorScheme.outlineVariant)
    ){
        Box(
            Modifier.fillMaxSize().padding(horizontal=6.dp,vertical=4.dp),
            contentAlignment=Alignment.CenterStart
        ){
            Column{
                Text(work.name,style=MaterialTheme.typography.labelMedium)
                if(related){
                    Text(
                        "Başka disiplin",
                        style=MaterialTheme.typography.labelSmall,
                        color=MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun MatrixCell(row:ReportWorkItemRow?,onClick:(String)->Unit){
    val background=when{
        row==null -> MaterialTheme.colorScheme.surface
        row.openProblemCount>0 -> MaterialTheme.colorScheme.errorContainer
        row.openAdvantageCount>0 -> matrixAdvantageContainer
        row.qualityStatus==QualityStatus.CRITICAL_DEFECT -> MaterialTheme.colorScheme.errorContainer
        row.qualityStatus==QualityStatus.DEFECTIVE -> MaterialTheme.colorScheme.errorContainer
        row.isBlocked -> MaterialTheme.colorScheme.tertiaryContainer
        row.workItemKind==WorkItemKind.RELATED_DISCIPLINE -> MaterialTheme.colorScheme.tertiaryContainer
        row.progressStatus==ProgressStatus.FINISHED -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val progress=when(row?.progressStatus){
        ProgressStatus.FINISHED -> "✓"
        ProgressStatus.IN_PROGRESS -> "◐"
        ProgressStatus.NOT_STARTED -> "○"
        null -> ""
    }
    val qualifier=if(row==null){
        ""
    }else{
        buildList{
            when(row.qualityStatus){
                QualityStatus.CRITICAL_DEFECT -> add("!!")
                QualityStatus.DEFECTIVE -> add("!")
                else -> Unit
            }
            if(row.isBlocked) add("B")
            if(row.openProblemCount>0){
                add(if(row.openProblemCount==1)"P" else "P"+row.openProblemCount)
            }
            if(row.openAdvantageCount>0){
                add(if(row.openAdvantageCount==1)"A" else "A"+row.openAdvantageCount)
            }
        }.joinToString(" ")
    }

    Surface(
        color=background,
        modifier=Modifier
            .width(72.dp)
            .height(52.dp)
            .border(0.5.dp,MaterialTheme.colorScheme.outlineVariant)
            .then(if(row!=null) Modifier.clickable{onClick(row.blockWorkItemId)} else Modifier)
    ){
        Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){
            Column(horizontalAlignment=Alignment.CenterHorizontally){
                Text(progress,style=MaterialTheme.typography.titleMedium)
                if(qualifier.isNotEmpty()){
                    Text(
                        qualifier,
                        style=MaterialTheme.typography.labelSmall,
                        fontWeight=FontWeight.Bold,
                        color=if((row?.openAdvantageCount ?: 0)>0 && (row?.openProblemCount ?: 0)==0)
                            matrixAdvantageContent
                        else
                            Color.Unspecified
                    )
                }
            }
        }
    }
}
