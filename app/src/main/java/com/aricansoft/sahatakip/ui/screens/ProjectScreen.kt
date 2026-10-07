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
import com.aricansoft.sahatakip.data.model.DeficiencyStatus
import com.aricansoft.sahatakip.data.model.FindingKind
import com.aricansoft.sahatakip.data.model.ProblemRecordStatus
import com.aricansoft.sahatakip.data.model.WorkItemKind
import com.aricansoft.sahatakip.report.ReportExporter
import com.aricansoft.sahatakip.report.XlsxExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

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
    var addMenuExpanded by remember{mutableStateOf(false)}
    var showAddBlock by remember{mutableStateOf(false)}
    var showFindingDefinitionKind by remember{mutableStateOf<FindingKind?>(null)}
    var showDeficiencyDefinition by remember{mutableStateOf(false)}
    val blocks by remember(projectId){repository.observeBlocks(projectId)}
        .collectAsStateWithLifecycle(initialValue=emptyList())
    val blockTypes by remember(projectId){repository.observeBlockTypes(projectId)}
        .collectAsStateWithLifecycle(initialValue=emptyList())
    val matrixRows by remember(projectId){repository.observeProjectMatrixRows(projectId)}
        .collectAsStateWithLifecycle(initialValue=emptyList())
    val projectFindings by remember(projectId){repository.observeProjectFindings(projectId)}
        .collectAsStateWithLifecycle(initialValue=emptyList())
    val projectDeficiencies by remember(projectId){repository.observeProjectDeficiencies(projectId)}
        .collectAsStateWithLifecycle(initialValue=emptyList())
    val findingDefinitions by remember(projectId){repository.observeProblemDefinitions(projectId)}
        .collectAsStateWithLifecycle(initialValue=emptyList())
    val deficiencyDefinitions by remember(projectId){repository.observeDeficiencyDefinitions(projectId)}
        .collectAsStateWithLifecycle(initialValue=emptyList())

    var selectedWorkItemKey by rememberSaveable(projectId){mutableStateOf<String?>(null)}
    var selectedWorkItemStatus by rememberSaveable(projectId){mutableStateOf(HomeQuickFilter.ALL)}
    var workItemMenuExpanded by remember{mutableStateOf(false)}
    var workItemSearch by remember{mutableStateOf("")}
    var selectedProblemKey by rememberSaveable(projectId){mutableStateOf<String?>(null)}
    var selectedAdvantageKey by rememberSaveable(projectId){mutableStateOf<String?>(null)}
    var selectedDeficiencyKey by rememberSaveable(projectId){mutableStateOf<String?>(null)}
    var matrixLoaded by remember(projectId){mutableStateOf(false)}
    var findingsLoaded by remember(projectId){mutableStateOf(false)}
    var deficienciesLoaded by remember(projectId){mutableStateOf(false)}

    LaunchedEffect(projectId){
        repository.observeProjectMatrixRows(projectId).first()
        matrixLoaded=true
    }
    LaunchedEffect(projectId){
        repository.observeProjectFindings(projectId).first()
        findingsLoaded=true
    }
    LaunchedEffect(projectId){
        repository.observeProjectDeficiencies(projectId).first()
        deficienciesLoaded=true
    }

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
    val blockOpenProblemCounts=remember(matrixRows){
        matrixRows
            .groupBy{it.blockCode}
            .mapValues{(_,rows)->rows.sumOf{it.openProblemCount}}
    }


    LaunchedEffect(matrixLoaded,workItemOptions,selectedWorkItemKey){
        if(matrixLoaded && selectedWorkItemKey!=null && workItemOptions.none{it.key==selectedWorkItemKey}){
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

    val scopedFindings=remember(projectFindings,selectedWorkItemKey){
        val key=selectedWorkItemKey
        if(key==null) projectFindings else projectFindings.filter{it.workItemDefinitionId==key}
    }
    val scopedDeficiencies=remember(projectDeficiencies,selectedWorkItemKey){
        val key=selectedWorkItemKey
        if(key==null) projectDeficiencies else projectDeficiencies.filter{it.workItemDefinitionId==key}
    }

    val problemOptions=remember(scopedFindings){
        scopedFindings
            .filter{it.kind==FindingKind.PROBLEM && it.status==ProblemRecordStatus.OPEN}
            .groupBy{it.problemDefinitionId}
            .map{(key,rows)->
                val first=rows.first()
                ProjectFilterOption(key,first.code+" — "+first.title,rows.map{it.blockCode}.distinct().size)
            }
            .sortedBy{it.label.lowercase(trLocale)}
    }
    val advantageOptions=remember(scopedFindings){
        scopedFindings
            .filter{it.kind==FindingKind.ADVANTAGE && it.status==ProblemRecordStatus.OPEN}
            .groupBy{it.problemDefinitionId}
            .map{(key,rows)->
                val first=rows.first()
                ProjectFilterOption(key,first.code+" — "+first.title,rows.map{it.blockCode}.distinct().size)
            }
            .sortedBy{it.label.lowercase(trLocale)}
    }
    val deficiencyOptions=remember(scopedDeficiencies){
        scopedDeficiencies
            .filter{it.status!=DeficiencyStatus.VERIFIED}
            .groupBy{normalizeWorkItemName(it.title)}
            .map{(key,rows)->
                ProjectFilterOption(key,rows.first().title,rows.map{it.blockCode}.distinct().size)
            }
            .sortedBy{it.label.lowercase(trLocale)}
    }

    LaunchedEffect(findingsLoaded,problemOptions,selectedProblemKey){
        if(findingsLoaded && selectedProblemKey!=null && problemOptions.none{it.key==selectedProblemKey}){
            selectedProblemKey=null
        }
    }
    LaunchedEffect(findingsLoaded,advantageOptions,selectedAdvantageKey){
        if(findingsLoaded && selectedAdvantageKey!=null && advantageOptions.none{it.key==selectedAdvantageKey}){
            selectedAdvantageKey=null
        }
    }
    LaunchedEffect(deficienciesLoaded,deficiencyOptions,selectedDeficiencyKey){
        if(deficienciesLoaded && selectedDeficiencyKey!=null && deficiencyOptions.none{it.key==selectedDeficiencyKey}){
            selectedDeficiencyKey=null
        }
    }

    val problemBlockCodes=remember(scopedFindings,selectedProblemKey){
        selectedProblemKey?.let{key->
            scopedFindings
                .filter{
                    it.problemDefinitionId==key &&
                        it.kind==FindingKind.PROBLEM &&
                        it.status==ProblemRecordStatus.OPEN
                }
                .map{it.blockCode}
                .toSet()
        }
    }
    val advantageBlockCodes=remember(scopedFindings,selectedAdvantageKey){
        selectedAdvantageKey?.let{key->
            scopedFindings
                .filter{
                    it.problemDefinitionId==key &&
                        it.kind==FindingKind.ADVANTAGE &&
                        it.status==ProblemRecordStatus.OPEN
                }
                .map{it.blockCode}
                .toSet()
        }
    }
    val deficiencyBlockCodes=remember(scopedDeficiencies,selectedDeficiencyKey){
        selectedDeficiencyKey?.let{key->
            scopedDeficiencies
                .filter{it.status!=DeficiencyStatus.VERIFIED && normalizeWorkItemName(it.title)==key}
                .map{it.blockCode}
                .toSet()
        }
    }

    val visibleBlocks=remember(
        blocks,
        selectedRowsByBlockCode,
        selectedWorkItemKey,
        selectedWorkItemStatus,
        problemBlockCodes,
        advantageBlockCodes,
        deficiencyBlockCodes
    ){
        blocks.filter{block->
            val workItemMatches=if(selectedWorkItemKey==null){
                true
            }else{
                val row=selectedRowsByBlockCode[block.code] ?: return@filter false
                selectedWorkItemStatus==HomeQuickFilter.ALL ||
                    row.asHomeCounts().matches(selectedWorkItemStatus)
            }
            workItemMatches &&
                (problemBlockCodes==null || block.code in problemBlockCodes) &&
                (advantageBlockCodes==null || block.code in advantageBlockCodes) &&
                (deficiencyBlockCodes==null || block.code in deficiencyBlockCodes)
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
            Box{
                FloatingActionButton(onClick={addMenuExpanded=true}){
                    Icon(Icons.Outlined.Add,contentDescription="Ekle")
                }
                DropdownMenu(
                    expanded=addMenuExpanded,
                    onDismissRequest={addMenuExpanded=false}
                ){
                    DropdownMenuItem(
                        text={Text("Blok ekle")},
                        onClick={
                            addMenuExpanded=false
                            showAddBlock=true
                        }
                    )
                    DropdownMenuItem(
                        text={Text("Problem tanımı ekle")},
                        onClick={
                            addMenuExpanded=false
                            showFindingDefinitionKind=FindingKind.PROBLEM
                        }
                    )
                    DropdownMenuItem(
                        text={Text("Avantaj tanımı ekle")},
                        onClick={
                            addMenuExpanded=false
                            showFindingDefinitionKind=FindingKind.ADVANTAGE
                        }
                    )
                    DropdownMenuItem(
                        text={Text("Eksik tanımı ekle")},
                        onClick={
                            addMenuExpanded=false
                            showDeficiencyDefinition=true
                        }
                    )
                }
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

                    ProjectRecordFilter(
                        title="Problem filtresi",
                        allLabel="Tüm problemler",
                        searchLabel="Problem ara",
                        emptyLabel="Açık problem yok",
                        options=problemOptions,
                        selectedKey=selectedProblemKey,
                        onSelected={selectedProblemKey=it}
                    )
                    ProjectRecordFilter(
                        title="Avantaj filtresi",
                        allLabel="Tüm avantajlar",
                        searchLabel="Avantaj ara",
                        emptyLabel="Açık avantaj yok",
                        options=advantageOptions,
                        selectedKey=selectedAdvantageKey,
                        onSelected={selectedAdvantageKey=it}
                    )
                    ProjectRecordFilter(
                        title="Eksik filtresi",
                        allLabel="Tüm eksikler",
                        searchLabel="Eksik ara",
                        emptyLabel="Aktif eksik yok",
                        options=deficiencyOptions,
                        selectedKey=selectedDeficiencyKey,
                        onSelected={selectedDeficiencyKey=it}
                    )

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
                        if(blocks.isEmpty())
                            "Projede blok yok."
                        else
                            "Seçili filtrelere uyan blok yok."
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
                                    Row(
                                        verticalAlignment=Alignment.CenterVertically,
                                        horizontalArrangement=Arrangement.spacedBy(8.dp)
                                    ){
                                        Text(block.code,style=MaterialTheme.typography.titleMedium)
                                        val openProblemCount=blockOpenProblemCounts[block.code] ?: 0
                                        if(openProblemCount>0){
                                            Surface(
                                                color=MaterialTheme.colorScheme.errorContainer,
                                                contentColor=MaterialTheme.colorScheme.onErrorContainer,
                                                shape=MaterialTheme.shapes.small
                                            ){
                                                Text(
                                                    if(openProblemCount==1)"Problem" else "Problem "+openProblemCount,
                                                    modifier=Modifier.padding(horizontal=8.dp,vertical=3.dp),
                                                    style=MaterialTheme.typography.labelMedium
                                                )
                                            }
                                        }
                                    }
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

    showFindingDefinitionKind?.let{kind->
        FindingDefinitionCatalogDialog(
            kind=kind,
            definitions=findingDefinitions,
            onDismiss={showFindingDefinitionKind=null},
            onCreate={code,title,tooltip->
                scope.launch{
                    runCatching{
                        repository.createProblemDefinition(
                            projectId=projectId,
                            code=code,
                            title=title,
                            tooltip=tooltip,
                            kind=kind
                        )
                    }.onSuccess{
                        showFindingDefinitionKind=null
                    }.onFailure{
                        snackbar.showSnackbar(it.message ?: "Tanım oluşturulamadı.")
                    }
                }
            }
        )
    }

    if(showDeficiencyDefinition){
        DeficiencyDefinitionCatalogDialog(
            definitions=deficiencyDefinitions,
            onDismiss={showDeficiencyDefinition=false},
            onCreate={title,description->
                scope.launch{
                    runCatching{
                        repository.createDeficiencyDefinition(
                            projectId=projectId,
                            title=title,
                            description=description
                        )
                    }.onSuccess{
                        showDeficiencyDefinition=false
                    }.onFailure{
                        snackbar.showSnackbar(it.message ?: "Eksik tanımı oluşturulamadı.")
                    }
                }
            }
        )
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
private fun ProjectRecordFilter(
    title:String,
    allLabel:String,
    searchLabel:String,
    emptyLabel:String,
    options:List<ProjectFilterOption>,
    selectedKey:String?,
    onSelected:(String?)->Unit
){
    var expanded by remember{mutableStateOf(false)}
    var search by remember{mutableStateOf("")}
    val selected=options.firstOrNull{it.key==selectedKey}
    val normalizedSearch=normalizeWorkItemName(search)
    val visibleOptions=options.filter{
        normalizedSearch.isBlank() || normalizeWorkItemName(it.label).contains(normalizedSearch)
    }

    Text(title,style=MaterialTheme.typography.labelLarge)
    Box(Modifier.fillMaxWidth()){
        OutlinedButton(
            onClick={expanded=true},
            modifier=Modifier.fillMaxWidth()
        ){
            Column(Modifier.weight(1f),horizontalAlignment=Alignment.Start){
                Text(selected?.label ?: allLabel)
                selected?.let{
                    Text(it.blockCount.toString()+" blok",style=MaterialTheme.typography.labelSmall)
                }
            }
            Text("▼")
        }
        DropdownMenu(
            expanded=expanded,
            onDismissRequest={
                expanded=false
                search=""
            },
            modifier=Modifier.fillMaxWidth()
        ){
            DropdownMenuItem(
                text={Text(allLabel)},
                onClick={
                    onSelected(null)
                    expanded=false
                    search=""
                }
            )
            HorizontalDivider()
            OutlinedTextField(
                value=search,
                onValueChange={search=it},
                label={Text(searchLabel)},
                modifier=Modifier.padding(horizontal=8.dp).fillMaxWidth(),
                singleLine=true
            )
            visibleOptions.forEach{option->
                DropdownMenuItem(
                    text={
                        Column{
                            Text(option.label)
                            Text(option.blockCount.toString()+" blok",style=MaterialTheme.typography.labelSmall)
                        }
                    },
                    onClick={
                        onSelected(option.key)
                        expanded=false
                        search=""
                    }
                )
            }
            if(visibleOptions.isEmpty()){
                DropdownMenuItem(
                    text={Text(if(options.isEmpty())emptyLabel else "Eşleşen kayıt yok")},
                    onClick={},
                    enabled=false
                )
            }
        }
    }
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
