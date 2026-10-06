package com.aricansoft.sahatakip.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aricansoft.sahatakip.data.SahaRepository
import com.aricansoft.sahatakip.report.ReportExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(repository:SahaRepository,onProject:(String)->Unit){
    val projects by repository.observeProjects().collectAsStateWithLifecycle(initialValue=emptyList())
    Scaffold(
        topBar={TopAppBar(title={Text("Saha Takip")})}
    ){padding->
        if(projects.isEmpty()){
            Box(Modifier.fillMaxSize().padding(padding),contentAlignment=Alignment.Center){
                CircularProgressIndicator()
            }
        }else{
            LazyColumn(
                modifier=Modifier.fillMaxSize().padding(padding),
                contentPadding=PaddingValues(16.dp),
                verticalArrangement=Arrangement.spacedBy(12.dp)
            ){
                items(projects,key={it.id}){project->
                    ElevatedCard(
                        modifier=Modifier.fillMaxWidth().clickable{onProject(project.id)}
                    ){
                        Row(
                            Modifier.fillMaxWidth().padding(18.dp),
                            verticalAlignment=Alignment.CenterVertically
                        ){
                            Icon(Icons.Outlined.Apartment,contentDescription=null)
                            Spacer(Modifier.width(12.dp))
                            Column{
                                Text(project.name,style=MaterialTheme.typography.titleMedium)
                                Text("Blokları aç",style=MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectScreen(
    repository:SahaRepository,
    projectId:String,
    onBack:()->Unit,
    onBlock:(String)->Unit,
    onMatrix:()->Unit
){
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val snackbar=remember{SnackbarHostState()}
    var exporting by remember{mutableStateOf(false)}
    val blocks by remember(projectId){repository.observeBlocks(projectId)}
        .collectAsStateWithLifecycle(initialValue=emptyList())

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
                    IconButton(onClick=onMatrix){
                        Icon(Icons.Outlined.TableChart,contentDescription="İmalat matrisi")
                    }
                    if(exporting){
                        CircularProgressIndicator(
                            modifier=Modifier.size(22.dp),
                            strokeWidth=2.dp
                        )
                    }else{
                        IconButton(onClick={exportPdf()}){
                            Icon(Icons.Outlined.PictureAsPdf,contentDescription="PDF rapor oluştur")
                        }
                    }
                }
            )
        }
    ){padding->
        LazyColumn(
            modifier=Modifier.fillMaxSize().padding(padding),
            contentPadding=PaddingValues(12.dp),
            verticalArrangement=Arrangement.spacedBy(8.dp)
        ){
            items(blocks,key={it.id}){block->
                Card(
                    modifier=Modifier.fillMaxWidth().clickable{onBlock(block.id)}
                ){
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement=Arrangement.SpaceBetween,
                        verticalAlignment=Alignment.CenterVertically
                    ){
                        Text(block.code,style=MaterialTheme.typography.titleMedium)
                        Text("İmalatlar →",style=MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
