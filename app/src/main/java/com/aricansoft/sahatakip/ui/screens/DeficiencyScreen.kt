package com.aricansoft.sahatakip.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aricansoft.sahatakip.data.SahaRepository
import com.aricansoft.sahatakip.data.db.ProjectDeficiencyRow
import com.aricansoft.sahatakip.data.model.DeficiencyPriority
import com.aricansoft.sahatakip.data.model.DeficiencyStatus
import kotlinx.coroutines.launch
import java.util.Locale

private enum class DeficiencyListFilter(val label:String){
    ACTIVE("Aktif"),
    ALL("Tümü"),
    OPEN("Açık"),
    IN_PROGRESS("Gideriliyor"),
    FIXED("Giderildi"),
    VERIFIED("Kontrol edildi")
}

private fun ProjectDeficiencyRow.matches(filter:DeficiencyListFilter)=when(filter){
    DeficiencyListFilter.ACTIVE -> status!=DeficiencyStatus.VERIFIED
    DeficiencyListFilter.ALL -> true
    DeficiencyListFilter.OPEN -> status==DeficiencyStatus.OPEN
    DeficiencyListFilter.IN_PROGRESS -> status==DeficiencyStatus.IN_PROGRESS
    DeficiencyListFilter.FIXED -> status==DeficiencyStatus.FIXED
    DeficiencyListFilter.VERIFIED -> status==DeficiencyStatus.VERIFIED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeficiencyScreen(
    repository:SahaRepository,
    projectId:String,
    onBack:()->Unit,
    onWorkItem:(String)->Unit
){
    val scope=rememberCoroutineScope()
    val records by remember(projectId){repository.observeProjectDeficiencies(projectId)}
        .collectAsStateWithLifecycle(initialValue=emptyList())
    var filter by rememberSaveable(projectId){mutableStateOf(DeficiencyListFilter.ACTIVE)}
    var priorityFilter by rememberSaveable(projectId){mutableStateOf<DeficiencyPriority?>(null)}
    var search by rememberSaveable(projectId){mutableStateOf("")}
    val tr=remember{Locale.forLanguageTag("tr-TR")}
    val normalized=search.trim().lowercase(tr)

    val visible=remember(records,filter,priorityFilter,normalized){
        records.filter{row->
            row.matches(filter) &&
            (priorityFilter==null || row.priority==priorityFilter) && (
                normalized.isBlank() ||
                    listOfNotNull(
                        row.blockCode,
                        row.workItemName,
                        row.title,
                        row.description,
                        row.floor,
                        row.unitNumber,
                        row.unitName,
                        row.responsible,
                        row.targetDate
                    ).any{it.lowercase(tr).contains(normalized)}
            )
        }
    }

    Scaffold(
        topBar={
            TopAppBar(
                title={Text("İmalat Eksik Takibi")},
                navigationIcon={
                    IconButton(onClick=onBack){
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack,contentDescription="Geri")
                    }
                }
            )
        }
    ){padding->
        Column(
            Modifier.fillMaxSize().padding(padding)
        ){
            Surface(tonalElevation=1.dp){
                Column(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement=Arrangement.spacedBy(8.dp)
                ){
                    Text(
                        records.count{it.status!=DeficiencyStatus.VERIFIED}.toString()+
                            " aktif eksik · "+
                            records.count{it.status==DeficiencyStatus.FIXED}.toString()+
                            " kontrol bekliyor",
                        style=MaterialTheme.typography.titleSmall
                    )
                    OutlinedTextField(
                        value=search,
                        onValueChange={search=it},
                        label={Text("Blok / imalat / mahal / sorumlu ara")},
                        modifier=Modifier.fillMaxWidth(),
                        singleLine=true
                    )
                    LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        items(DeficiencyListFilter.entries,key={it.name}){item->
                            val count=records.count{it.matches(item)}
                            FilterChip(
                                selected=filter==item,
                                onClick={filter=item},
                                label={Text(item.label+" ("+count+")")}
                            )
                        }
                    }
                    Text("Öncelik",style=MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        item{
                            FilterChip(
                                selected=priorityFilter==null,
                                onClick={priorityFilter=null},
                                label={Text("Tümü ("+records.count{it.matches(filter)}+")")}
                            )
                        }
                        items(DeficiencyPriority.entries,key={it.name}){priority->
                            val count=records.count{it.matches(filter) && it.priority==priority}
                            FilterChip(
                                selected=priorityFilter==priority,
                                onClick={priorityFilter=priority},
                                label={Text(priority.label+" ("+count+")")}
                            )
                        }
                    }
                }
            }

            if(visible.isEmpty()){
                Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){
                    Text(
                        if(records.isEmpty())
                            "Henüz imalat eksiği açılmamış."
                        else
                            "Seçili filtreye uygun eksik yok."
                    )
                }
            }else{
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding=PaddingValues(12.dp),
                    verticalArrangement=Arrangement.spacedBy(10.dp)
                ){
                    items(visible,key={it.deficiencyId}){row->
                        ElevatedCard(
                            modifier=Modifier.fillMaxWidth().clickable{onWorkItem(row.blockWorkItemId)}
                        ){
                            Column(
                                Modifier.padding(14.dp),
                                verticalArrangement=Arrangement.spacedBy(6.dp)
                            ){
                                Row(verticalAlignment=Alignment.CenterVertically){
                                    Icon(
                                        Icons.Outlined.Warning,
                                        contentDescription=null,
                                        tint=if(row.priority==DeficiencyPriority.CRITICAL)
                                            MaterialTheme.colorScheme.error
                                        else
                                            MaterialTheme.colorScheme.tertiary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column(Modifier.weight(1f)){
                                        Text(row.title,style=MaterialTheme.typography.titleSmall)
                                        Text(
                                            row.blockCode+" · "+row.workItemName,
                                            style=MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    AssistChip(onClick={},label={Text(row.priority.label)})
                                }

                                row.description?.let{Text(it)}
                                val location=buildList{
                                    row.floor?.let{add("Kat: "+it)}
                                    row.unitNumber?.let{add("No: "+it)}
                                    row.unitName?.let{add("Birim: "+it)}
                                }
                                if(location.isNotEmpty()){
                                    Text(location.joinToString(" · "),style=MaterialTheme.typography.bodySmall)
                                }
                                row.responsible?.let{
                                    Text("Sorumlu: "+it,style=MaterialTheme.typography.bodySmall)
                                }
                                row.targetDate?.let{
                                    Text("Hedef: "+it,style=MaterialTheme.typography.bodySmall)
                                }

                                LazyRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                                    items(DeficiencyStatus.entries,key={it.name}){status->
                                        FilterChip(
                                            selected=row.status==status,
                                            onClick={
                                                scope.launch{
                                                    repository.setDeficiencyStatus(row.deficiencyId,status)
                                                }
                                            },
                                            label={Text(status.label)}
                                        )
                                    }
                                }
                                Text(
                                    if(row.status==DeficiencyStatus.FIXED)
                                        "Giderildi; saha kontrolü bekleniyor."
                                    else if(row.status==DeficiencyStatus.VERIFIED)
                                        "Kontrol edildi; aktif listeden çıktı."
                                    else
                                        "İmalat detayını aç →",
                                    style=MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
