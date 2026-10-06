package com.aricansoft.sahatakip.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aricansoft.sahatakip.data.SahaRepository
import com.aricansoft.sahatakip.data.db.ReportWorkItemRow
import com.aricansoft.sahatakip.data.model.ProgressStatus
import com.aricansoft.sahatakip.data.model.QualityStatus

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
    val rows by remember(projectId){repository.observeProjectMatrixRows(projectId)}
        .collectAsStateWithLifecycle(initialValue=emptyList())

    val workNames=remember(rows){
        rows.map{it.workItemName}.distinct().sorted()
    }
    val cellMap=remember(rows){
        rows.associateBy{it.workItemName to it.blockCode}
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
                .verticalScroll(vertical)
                .horizontalScroll(horizontal)
                .padding(8.dp)
        ){
            Row{
                HeaderCell("İmalat",180.dp)
                blocks.forEach{block->HeaderCell(block.code,72.dp)}
            }
            workNames.forEach{workName->
                Row{
                    WorkNameCell(workName)
                    blocks.forEach{block->
                        MatrixCell(
                            row=cellMap[workName to block.code],
                            onClick=onWorkItem
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "✓ Bitti   ◐ Devam   ○ Başlanmadı   ! Kusurlu   !! Ağır kusurlu   B Bloke   P Açık problem",
                style=MaterialTheme.typography.bodySmall
            )
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
private fun WorkNameCell(text:String){
    Box(
        Modifier
            .width(180.dp)
            .height(52.dp)
            .border(0.5.dp,MaterialTheme.colorScheme.outlineVariant)
            .padding(horizontal=6.dp,vertical=4.dp),
        contentAlignment=Alignment.CenterStart
    ){
        Text(text,style=MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun MatrixCell(row:ReportWorkItemRow?,onClick:(String)->Unit){
    val background=when{
        row==null -> MaterialTheme.colorScheme.surface
        row.qualityStatus==QualityStatus.CRITICAL_DEFECT -> MaterialTheme.colorScheme.errorContainer
        row.qualityStatus==QualityStatus.DEFECTIVE -> MaterialTheme.colorScheme.errorContainer
        row.isBlocked -> MaterialTheme.colorScheme.tertiaryContainer
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
                    Text(qualifier,style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Bold)
                }
            }
        }
    }
}
