package com.aricansoft.sahatakip.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aricansoft.sahatakip.SahaTakipApplication
import com.aricansoft.sahatakip.backup.GkteShare
import com.aricansoft.sahatakip.backup.SitePackManager
import com.aricansoft.sahatakip.data.SahaRepository
import com.aricansoft.sahatakip.data.normalizeWorkItemName
import com.aricansoft.sahatakip.data.db.BlockTypeEntity
import com.aricansoft.sahatakip.data.db.ProjectQuickStatusRow
import com.aricansoft.sahatakip.data.db.ReportWorkItemRow
import com.aricansoft.sahatakip.data.model.WorkItemKind
import com.aricansoft.sahatakip.report.ReportExporter
import com.aricansoft.sahatakip.report.XlsxExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private enum class HomeQuickFilter(val label:String){
    ALL("Tümü"),
    OPEN_PROBLEM("Açık problem"),
    OPEN_DEFICIENCY("Açık eksik"),
    OPEN_ADVANTAGE("Açık avantaj"),
    DEFECTIVE("Kusurlu"),
    BLOCKED("Bloke"),
    IN_PROGRESS("Devam"),
    FINISHED("Bitti")
}

private data class HomeStatusCounts(
    val total:Int,
    val inProgress:Int,
    val finished:Int,
    val defective:Int,
    val blocked:Int,
    val openProblems:Int,
    val openDeficiencies:Int,
    val openAdvantages:Int
)

private data class ProjectWorkItemOption(
    val key:String,
    val label:String,
    val kind:WorkItemKind,
    val blockCount:Int
)

private val trLocale=Locale.forLanguageTag("tr-TR")
private fun ProjectQuickStatusRow.asHomeCounts()=HomeStatusCounts(
    total=totalWorkItemCount,
    inProgress=inProgressCount,
    finished=finishedCount,
    defective=defectiveCount,
    blocked=blockedCount,
    openProblems=openProblemCount,
    openDeficiencies=openDeficiencyCount,
    openAdvantages=openAdvantageCount
)

private fun ReportWorkItemRow.asHomeCounts()=HomeStatusCounts(
    total=1,
    inProgress=if(progressStatus.name=="IN_PROGRESS")1 else 0,
    finished=if(progressStatus.name=="FINISHED")1 else 0,
    defective=if(qualityStatus.name=="DEFECTIVE" || qualityStatus.name=="CRITICAL_DEFECT")1 else 0,
    blocked=if(isBlocked)1 else 0,
    openProblems=openProblemCount,
    openDeficiencies=openDeficiencyCount,
    openAdvantages=openAdvantageCount
)

private fun HomeStatusCounts.matches(filter:HomeQuickFilter)=when(filter){
    HomeQuickFilter.ALL -> total>0
    HomeQuickFilter.OPEN_PROBLEM -> openProblems>0
    HomeQuickFilter.OPEN_DEFICIENCY -> openDeficiencies>0
    HomeQuickFilter.OPEN_ADVANTAGE -> openAdvantages>0
    HomeQuickFilter.DEFECTIVE -> defective>0
    HomeQuickFilter.BLOCKED -> blocked>0
    HomeQuickFilter.IN_PROGRESS -> inProgress>0
    HomeQuickFilter.FINISHED -> finished>0
}

private fun HomeStatusCounts.summaryText():String=buildList{
    if(openProblems>0) add("P $openProblems")
    if(openDeficiencies>0) add("Eksik $openDeficiencies")
    if(openAdvantages>0) add("A $openAdvantages")
    if(defective>0) add("Kusurlu $defective")
    if(blocked>0) add("Bloke $blocked")
    if(inProgress>0) add("Devam $inProgress")
    if(finished>0) add("Bitti $finished")
}.joinToString(" · ")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(repository:SahaRepository,onProject:(String)->Unit){
    val context=LocalContext.current
    val app=context.applicationContext as SahaTakipApplication
    val scope=rememberCoroutineScope()
    val snackbar=remember{SnackbarHostState()}
    val projects by repository.observeProjects().collectAsStateWithLifecycle(initialValue=emptyList())
    val quickStatuses by repository.observeProjectQuickStatuses()
        .collectAsStateWithLifecycle(initialValue=emptyList())
    var selectedQuickFilter by rememberSaveable{mutableStateOf(HomeQuickFilter.ALL)}
    var showAdd by remember{mutableStateOf(false)}
    var importing by remember{mutableStateOf(false)}

    val statusByProject=remember(quickStatuses){
        quickStatuses.associate{it.projectId to it.asHomeCounts()}
    }
    val visibleProjects=remember(projects,statusByProject,selectedQuickFilter){
        if(selectedQuickFilter==HomeQuickFilter.ALL){
            projects
        }else{
            projects.filter{project->
                statusByProject[project.id]?.matches(selectedQuickFilter)==true
            }
        }
    }

    val importLauncher=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        if(uri!=null){
            importing=true
            scope.launch{
                runCatching{
                    withContext(Dispatchers.IO){app.sitePackManager.importProject(uri)}
                }.onSuccess{projectId->
                    importing=false
                    onProject(projectId)
                }.onFailure{
                    importing=false
                    snackbar.showSnackbar("GKTE içe aktarılamadı: "+(it.message ?: "Bilinmeyen hata"))
                }
            }
        }
    }

    Scaffold(
        snackbarHost={SnackbarHost(snackbar)},
        topBar={
            TopAppBar(
                title={Text("Saha Takip")},
                actions={
                    if(importing){
                        CircularProgressIndicator(modifier=Modifier.size(22.dp),strokeWidth=2.dp)
                    }else{
                        IconButton(onClick={
                            importLauncher.launch(arrayOf(
                                SitePackManager.GKTE_MIME,
                                "application/octet-stream",
                                "application/zip",
                                "*/*"
                            ))
                        }){
                            Icon(Icons.Outlined.Unarchive,contentDescription="GKTE içe aktar")
                        }
                    }
                }
            )
        },
        floatingActionButton={
            FloatingActionButton(onClick={showAdd=true}){
                Icon(Icons.Outlined.Add,contentDescription="Proje ekle")
            }
        }
    ){padding->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ){
            Surface(tonalElevation=1.dp){
                Column(
                    Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=8.dp),
                    verticalArrangement=Arrangement.spacedBy(6.dp)
                ){
                    Text("Hızlı durum filtresi",style=MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        items(HomeQuickFilter.entries,key={it.name}){filter->
                            val count=if(filter==HomeQuickFilter.ALL){
                                projects.size
                            }else{
                                projects.count{project->
                                    statusByProject[project.id]?.matches(filter)==true
                                }
                            }
                            FilterChip(
                                selected=selectedQuickFilter==filter,
                                onClick={selectedQuickFilter=filter},
                                label={Text(filter.label+" ("+count+")")}
                            )
                        }
                    }
                }
            }

            if(projects.isEmpty()){
                Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){
                    CircularProgressIndicator()
                }
            }else if(visibleProjects.isEmpty()){
                Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){
                    Text("Seçili durumda proje yok.")
                }
            }else{
                LazyColumn(
                    modifier=Modifier.fillMaxSize(),
                    contentPadding=PaddingValues(16.dp),
                    verticalArrangement=Arrangement.spacedBy(12.dp)
                ){
                    items(visibleProjects,key={it.id}){project->
                        val quick=statusByProject[project.id]
                        ElevatedCard(
                            modifier=Modifier.fillMaxWidth().clickable{onProject(project.id)}
                        ){
                            Row(
                                Modifier.fillMaxWidth().padding(18.dp),
                                verticalAlignment=Alignment.CenterVertically
                            ){
                                Icon(Icons.Outlined.Apartment,contentDescription=null)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)){
                                    Text(project.name,style=MaterialTheme.typography.titleMedium)
                                    val summary=quick?.summaryText().orEmpty()
                                    Text(
                                        if(summary.isBlank())"Henüz durum kaydı yok" else summary,
                                        style=MaterialTheme.typography.bodySmall
                                    )
                                }
                                Text("Aç →",style=MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
    }

    if(showAdd){
        AddProjectDialog(
            onDismiss={showAdd=false},
            onCreate={name->
                scope.launch{
                    val project=repository.createProject(name)
                    showAdd=false
                    onProject(project.id)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectScreen(
    repository:SahaRepository,
    projectId:String,
    onBack:()->Unit,
    onBlock:(String)->Unit,
    onWorkItem:(String)->Unit,
    onMatrix:()->Unit,
    onDeficiencies:()->Unit
){
    val context=LocalContext.current
    val app=context.applicationContext as SahaTakipApplication
    val scope=rememberCoroutineScope()
    val snackbar=remember{SnackbarHostState()}
    var exporting by remember{mutableStateOf(false)}
    var exportingXlsx by remember{mutableStateOf(false)}
    var backingUp by remember{mutableStateOf(false)}
    var actionMenu by remember{mutableStateOf(false)}
    var showAddBlock by remember{mutableStateOf(false)}
    val blocks by remember(projectId){repository.observeBlocks(projectId)}
        .collectAsStateWithLifecycle(initialValue=emptyList())
    val blockTypes by remember(projectId){repository.observeBlockTypes(projectId)}
        .collectAsStateWithLifecycle(initialValue=emptyList())
    val matrixRows by remember(projectId){repository.observeProjectMatrixRows(projectId)}
        .collectAsStateWithLifecycle(initialValue=emptyList())

    var selectedWorkItemKey by rememberSaveable(projectId){mutableStateOf<String?>(null)}
    var selectedWorkItemStatus by rememberSaveable(projectId){mutableStateOf(HomeQuickFilter.ALL)}
    var workItemMenuExpanded by remember{mutableStateOf(false)}
    var workItemSearch by remember{mutableStateOf("")}

    val workItemOptions=remember(matrixRows){
        matrixRows
            .groupBy{it.workItemDefinitionId}
            .map{entry->
                val representative=entry.value.first()
                ProjectWorkItemOption(
                    key=entry.key,
                    label=representative.workItemName,
                    kind=representative.workItemKind,
                    blockCount=entry.value.map{it.blockCode}.distinct().size
                )
            }
            .sortedWith(
                compareBy<ProjectWorkItemOption>{if(it.kind==WorkItemKind.ELECTRICAL)0 else 1}
                    .thenBy{it.label.lowercase(trLocale)}
            )
    }
    val selectedWorkItem=remember(workItemOptions,selectedWorkItemKey){
        selectedWorkItemKey?.let{key->workItemOptions.firstOrNull{it.key==key}}
    }

    LaunchedEffect(workItemOptions,selectedWorkItemKey){
        if(selectedWorkItemKey!=null && workItemOptions.none{it.key==selectedWorkItemKey}){
            selectedWorkItemKey=null
            selectedWorkItemStatus=HomeQuickFilter.ALL
        }
    }

    val selectedRowsByBlockCode=remember(matrixRows,selectedWorkItemKey){
        val key=selectedWorkItemKey
        if(key==null){
            emptyMap()
        }else{
            matrixRows
                .filter{it.workItemDefinitionId==key}
                .associateBy{it.blockCode}
        }
    }

    val visibleBlocks=remember(blocks,selectedRowsByBlockCode,selectedWorkItemKey,selectedWorkItemStatus){
        if(selectedWorkItemKey==null){
            blocks
        }else{
            blocks.filter{block->
                val row=selectedRowsByBlockCode[block.code] ?: return@filter false
                selectedWorkItemStatus==HomeQuickFilter.ALL ||
                    row.asHomeCounts().matches(selectedWorkItemStatus)
            }
        }
    }

    fun exportBackup(){
        if(backingUp) return
        backingUp=true
        scope.launch{
            runCatching{
                val uri=withContext(Dispatchers.IO){
                    app.sitePackManager.exportGkte(projectId)
                }
                GkteShare.shareToWhatsApp(context,uri)
            }.onFailure{
                snackbar.showSnackbar("GKTE oluşturulamadı: "+(it.message ?: "Bilinmeyen hata"))
            }
            backingUp=false
        }
    }

    fun exportXlsx(){
        if(exportingXlsx) return
        exportingXlsx=true
        scope.launch{
            runCatching{
                val snapshot=withContext(Dispatchers.IO){
                    repository.getProjectReportSnapshot(projectId)
                        ?: error("Proje bulunamadı.")
                }
                val uri=withContext(Dispatchers.IO){
                    XlsxExporter.exportProject(context,snapshot)
                }
                val share=Intent(Intent.ACTION_SEND).apply{
                    type="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    putExtra(Intent.EXTRA_STREAM,uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(share,"Excel raporunu paylaş"))
            }.onFailure{
                snackbar.showSnackbar("Excel oluşturulamadı: "+(it.message ?: "Bilinmeyen hata"))
            }
            exportingXlsx=false
        }
    }

    fun exportPdf(){
        if(exporting) return
        exporting=true
        scope.launch{
            runCatching{
                val snapshot=withContext(Dispatchers.IO){
                    repository.getProjectReportSnapshot(projectId)
                        ?: error("Proje bulunamadı.")
                }
                val uri=withContext(Dispatchers.IO){
                    ReportExporter.exportProject(context,snapshot)
                }
                val share=Intent(Intent.ACTION_SEND).apply{
                    type="application/pdf"
                    putExtra(Intent.EXTRA_STREAM,uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(share,"Saha raporunu paylaş"))
            }.onFailure{
                snackbar.showSnackbar("PDF oluşturulamadı: "+(it.message ?: "Bilinmeyen hata"))
            }
            exporting=false
        }
    }

    Scaffold(
        snackbarHost={SnackbarHost(snackbar)},
        topBar={
            TopAppBar(
                title={Text("Bloklar")},
                navigationIcon={
                    IconButton(onClick=onBack){
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack,contentDescription="Geri")
                    }
                },
                actions={
                    IconButton(onClick=onDeficiencies){
                        Icon(Icons.Outlined.ReportProblem,contentDescription="Eksik takibi")
                    }
                    IconButton(onClick=onMatrix){
                        Icon(Icons.Outlined.TableChart,contentDescription="İmalat matrisi")
                    }
                    if(exporting || exportingXlsx || backingUp){
                        CircularProgressIndicator(
                            modifier=Modifier.size(22.dp),
                            strokeWidth=2.dp
                        )
                    }else{
                        Box{
                            IconButton(onClick={actionMenu=true}){
                                Icon(Icons.Outlined.MoreVert,contentDescription="Dışa aktar")
                            }
                            DropdownMenu(
                                expanded=actionMenu,
                                onDismissRequest={actionMenu=false}
                            ){
                                DropdownMenuItem(
                                    text={Text("PDF raporu")},
                                    leadingIcon={Icon(Icons.Outlined.PictureAsPdf,contentDescription=null)},
                                    onClick={
                                        actionMenu=false
                                        exportPdf()
                                    }
                                )
                                DropdownMenuItem(
                                    text={Text("Excel / XLSX")},
                                    leadingIcon={Icon(Icons.Outlined.TableChart,contentDescription=null)},
                                    onClick={
                                        actionMenu=false
                                        exportXlsx()
                                    }
                                )
                                DropdownMenuItem(
                                    text={Text("GKTE / WhatsApp ile paylaş")},
                                    leadingIcon={Icon(Icons.Outlined.Archive,contentDescription=null)},
                                    onClick={
                                        actionMenu=false
                                        exportBackup()
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton={
            FloatingActionButton(onClick={showAddBlock=true}){
                Icon(Icons.Outlined.Add,contentDescription="Blok ekle")
            }
        }
    ){padding->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ){
            Surface(tonalElevation=1.dp){
                Column(
                    Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=8.dp),
                    verticalArrangement=Arrangement.spacedBy(8.dp)
                ){
                    Text("İmalat filtresi",style=MaterialTheme.typography.labelLarge)
                    Box(Modifier.fillMaxWidth()){
                        OutlinedButton(
                            onClick={workItemMenuExpanded=true},
                            modifier=Modifier.fillMaxWidth()
                        ){
                            Column(Modifier.weight(1f),horizontalAlignment=Alignment.Start){
                                Text(selectedWorkItem?.label ?: "Tüm imalatlar")
                                selectedWorkItem?.let{option->
                                    Text(
                                        (if(option.kind==WorkItemKind.RELATED_DISCIPLINE)
                                            "Alakadar başka disiplin · "
                                        else
                                            "")+
                                            option.blockCount+" blok",
                                        style=MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                            Text("▼")
                        }
                        DropdownMenu(
                            expanded=workItemMenuExpanded,
                            onDismissRequest={
                                workItemMenuExpanded=false
                                workItemSearch=""
                            },
                            modifier=Modifier.fillMaxWidth()
                        ){
                            DropdownMenuItem(
                                text={Text("Tüm imalatlar")},
                                onClick={
                                    selectedWorkItemKey=null
                                    selectedWorkItemStatus=HomeQuickFilter.ALL
                                    workItemMenuExpanded=false
                                    workItemSearch=""
                                }
                            )
                            HorizontalDivider()
                            OutlinedTextField(
                                value=workItemSearch,
                                onValueChange={workItemSearch=it},
                                label={Text("İmalat ara")},
                                modifier=Modifier.padding(horizontal=8.dp).fillMaxWidth(),
                                singleLine=true
                            )
                            val normalizedSearch=normalizeWorkItemName(workItemSearch)
                            val visibleOptions=workItemOptions.filter{option->
                                normalizedSearch.isBlank() ||
                                    normalizeWorkItemName(option.label).contains(normalizedSearch)
                            }
                            visibleOptions.forEach{option->
                                DropdownMenuItem(
                                    text={
                                        Column{
                                            Text(option.label)
                                            Text(
                                                (if(option.kind==WorkItemKind.RELATED_DISCIPLINE)
                                                    "Alakadar başka disiplin · "
                                                else
                                                    "")+
                                                    option.blockCount+" blok",
                                                style=MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    },
                                    onClick={
                                        selectedWorkItemKey=option.key
                                        selectedWorkItemStatus=HomeQuickFilter.ALL
                                        workItemMenuExpanded=false
                                        workItemSearch=""
                                    }
                                )
                            }
                            if(visibleOptions.isEmpty()){
                                DropdownMenuItem(
                                    text={Text("Eşleşen imalat yok")},
                                    onClick={},
                                    enabled=false
                                )
                            }
                        }
                    }

                    selectedWorkItem?.let{option->
                        Text("Seçili imalat durumu",style=MaterialTheme.typography.labelLarge)
                        LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                            items(HomeQuickFilter.entries,key={it.name}){filter->
                                val count=blocks.count{block->
                                    val row=selectedRowsByBlockCode[block.code]
                                    row!=null && (
                                        filter==HomeQuickFilter.ALL ||
                                            row.asHomeCounts().matches(filter)
                                    )
                                }
                                FilterChip(
                                    selected=selectedWorkItemStatus==filter,
                                    onClick={selectedWorkItemStatus=filter},
                                    label={Text(filter.label+" ("+count+")")}
                                )
                            }
                        }
                        Text(
                            "Filtre yalnız “"+option.label+"” imalatının bloklardaki gerçek durumuna uygulanır.",
                            style=MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            if(visibleBlocks.isEmpty()){
                Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){
                    Text(
                        if(selectedWorkItem==null)
                            "Projede blok yok."
                        else
                            "Seçili imalat ve durumda blok yok."
                    )
                }
            }else{
                LazyColumn(
                    modifier=Modifier.fillMaxSize(),
                    contentPadding=PaddingValues(12.dp),
                    verticalArrangement=Arrangement.spacedBy(8.dp)
                ){
                    items(visibleBlocks,key={it.id}){block->
                        val selectedRow=selectedRowsByBlockCode[block.code]
                        Card(
                            modifier=Modifier.fillMaxWidth().clickable{
                                val selectedRow=selectedRowsByBlockCode[block.code]
                                if(selectedRow!=null) onWorkItem(selectedRow.blockWorkItemId)
                                else onBlock(block.id)
                            }
                        ){
                            Row(
                                Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement=Arrangement.SpaceBetween,
                                verticalAlignment=Alignment.CenterVertically
                            ){
                                Column(Modifier.weight(1f)){
                                    Text(block.code,style=MaterialTheme.typography.titleMedium)
                                    selectedRow?.let{row->
                                        val detail=buildList{
                                            add(row.progressStatus.label)
                                            val summary=row.asHomeCounts().summaryText()
                                            if(summary.isNotBlank()) add(summary)
                                        }.joinToString(" · ")
                                        Text(
                                            selectedWorkItem?.label.orEmpty()+" — "+detail,
                                            style=MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                                Text(
                                    if(selectedWorkItem==null)"İmalatlar →" else "İmalatı aç →",
                                    style=MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if(showAddBlock){
        AddBlockDialog(
            blockTypes=blockTypes,
            onDismiss={showAddBlock=false},
            onCreate={typeId,newTypeCode,newTypeName,newTypeTooltip,sequence->
                scope.launch{
                    runCatching{
                        val resolvedTypeId=typeId ?: repository.createBlockType(
                            projectId=projectId,
                            code=newTypeCode,
                            name=newTypeName,
                            tooltip=newTypeTooltip
                        ).id
                        repository.createBlock(projectId,resolvedTypeId,sequence)
                    }.onSuccess{
                        showAddBlock=false
                    }.onFailure{
                        snackbar.showSnackbar(it.message ?: "Blok oluşturulamadı.")
                    }
                }
            }
        )
    }
}

@Composable
private fun AddProjectDialog(onDismiss:()->Unit,onCreate:(String)->Unit){
    var name by remember{mutableStateOf("")}
    AlertDialog(
        onDismissRequest=onDismiss,
        title={Text("Yeni proje")},
        text={
            OutlinedTextField(
                value=name,
                onValueChange={name=it},
                label={Text("Proje adı")},
                modifier=Modifier.fillMaxWidth()
            )
        },
        confirmButton={
            TextButton(onClick={onCreate(name)},enabled=name.isNotBlank()){Text("Oluştur")}
        },
        dismissButton={TextButton(onClick=onDismiss){Text("Vazgeç")}}
    )
}

@Composable
private fun AddBlockDialog(
    blockTypes:List<BlockTypeEntity>,
    onDismiss:()->Unit,
    onCreate:(String?,String,String?,String?,Int)->Unit
){
    var selectedTypeId by remember{mutableStateOf<String?>(blockTypes.firstOrNull()?.id)}
    var creatingType by remember{mutableStateOf(blockTypes.isEmpty())}
    var typeCode by remember{mutableStateOf("")}
    var typeName by remember{mutableStateOf("")}
    var typeTooltip by remember{mutableStateOf("")}
    var sequenceText by remember{mutableStateOf("")}

    val sequence=sequenceText.toIntOrNull()

    AlertDialog(
        onDismissRequest=onDismiss,
        title={Text("Blok ekle")},
        text={
            Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
                if(!creatingType){
                    Text("Blok tipi",style=MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                        items(blockTypes,key={it.id}){type->
                            FilterChip(
                                selected=selectedTypeId==type.id,
                                onClick={selectedTypeId=type.id},
                                label={Text(type.code)}
                            )
                        }
                    }
                    TextButton(onClick={
                        creatingType=true
                        selectedTypeId=null
                    }){Text("+ Yeni blok tipi tanımla")}
                }else{
                    Text("Yeni blok tipi",style=MaterialTheme.typography.labelLarge)
                    OutlinedTextField(
                        typeCode,
                        {typeCode=it},
                        label={Text("Kod (örn. GK)")},
                        modifier=Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        typeName,
                        {typeName=it},
                        label={Text("Ad (opsiyonel)")},
                        modifier=Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        typeTooltip,
                        {typeTooltip=it},
                        label={Text("Tooltip / açıklama (opsiyonel)")},
                        modifier=Modifier.fillMaxWidth()
                    )
                    if(blockTypes.isNotEmpty()){
                        TextButton(onClick={
                            creatingType=false
                            selectedTypeId=blockTypes.first().id
                        }){Text("Mevcut blok tipini seç")}
                    }
                }

                OutlinedTextField(
                    value=sequenceText,
                    onValueChange={sequenceText=it.filter(Char::isDigit)},
                    label={Text("Blok numarası (örn. 9)")},
                    modifier=Modifier.fillMaxWidth()
                )

                val typeCodePreview=if(creatingType) typeCode.trim().uppercase()
                    else blockTypes.firstOrNull{it.id==selectedTypeId}?.code.orEmpty()
                if(typeCodePreview.isNotBlank() && sequence!=null){
                    Text(
                        "Oluşacak blok: "+typeCodePreview+"-"+sequence,
                        style=MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton={
            TextButton(
                onClick={
                    onCreate(
                        if(creatingType)null else selectedTypeId,
                        typeCode,
                        typeName.ifBlank{null},
                        typeTooltip.ifBlank{null},
                        requireNotNull(sequence)
                    )
                },
                enabled=sequence!=null && sequence>0 &&
                    ((!creatingType && selectedTypeId!=null) || (creatingType && typeCode.isNotBlank()))
            ){Text("Ekle")}
        },
        dismissButton={TextButton(onClick=onDismiss){Text("Vazgeç")}}
    )
}
