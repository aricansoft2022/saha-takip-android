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
