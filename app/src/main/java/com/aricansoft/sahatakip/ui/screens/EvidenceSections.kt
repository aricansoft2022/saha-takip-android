package com.aricansoft.sahatakip.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aricansoft.sahatakip.data.SahaRepository
import com.aricansoft.sahatakip.data.db.DeficiencyEntity
import com.aricansoft.sahatakip.data.db.ProblemRecordRow
import com.aricansoft.sahatakip.data.model.DeficiencyPriority
import com.aricansoft.sahatakip.data.model.DeficiencyStatus
import com.aricansoft.sahatakip.data.model.FindingKind
import com.aricansoft.sahatakip.data.model.ProblemRecordStatus
import com.aricansoft.sahatakip.ui.InfoTooltip
import com.aricansoft.sahatakip.ui.formatTime

private val advantageContainer=Color(0xFFE6F4D7)
private val advantageContent=Color(0xFF285F16)

@Composable
internal fun FindingSection(
    repository:SahaRepository,
    kind:FindingKind,
    records:List<ProblemRecordRow>,
    photoEnabled:Boolean,
    onTakePhoto:(String)->Unit,
    onTogglePhotoReport:(String,Boolean)->Unit,
    onToggleReport:(String,Boolean)->Unit,
    onClose:(String)->Unit,
    onAdd:()->Unit
){
    HorizontalDivider()
    val isAdvantage=kind==FindingKind.ADVANTAGE
    val activeCount=records.count{it.status==ProblemRecordStatus.OPEN}
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.SpaceBetween
    ){
        Text(if(isAdvantage)"Avantajlar" else "Problemler",style=MaterialTheme.typography.titleMedium)
        Text(
            activeCount.toString()+" aktif",
            style=MaterialTheme.typography.labelMedium,
            color=if(isAdvantage)advantageContent else MaterialTheme.colorScheme.error
        )
    }

    records.forEach{record->
        OutlinedCard(
            modifier=Modifier.fillMaxWidth(),
            colors=CardDefaults.outlinedCardColors(
                containerColor=if(isAdvantage)advantageContainer else MaterialTheme.colorScheme.surface
            )
        ){
            Column(Modifier.padding(12.dp)){
                Row(verticalAlignment=Alignment.CenterVertically){
                    Icon(
                        if(isAdvantage)Icons.Outlined.CheckCircle else Icons.Outlined.ReportProblem,
                        contentDescription=null,
                        tint=if(isAdvantage)advantageContent else MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        record.code+" — "+record.title,
                        Modifier.weight(1f),
                        color=if(isAdvantage)advantageContent else Color.Unspecified
                    )
                    record.tooltip?.let{InfoTooltip(it)}
                }
                Text(
                    if(record.status==ProblemRecordStatus.OPEN)"Açık" else "Kapalı",
                    color=if(isAdvantage && record.status==ProblemRecordStatus.OPEN)advantageContent else Color.Unspecified
                )
                record.specificDescription?.let{
                    Text("Özel tanım: "+it,style=MaterialTheme.typography.bodyMedium)
                }
                val locationParts=buildList{
                    record.floor?.let{add("Kat: "+it)}
                    record.unitNumber?.let{add("No: "+it)}
                    record.unitName?.let{add("Mahal / daire / birim: "+it)}
                }
                if(locationParts.isNotEmpty()){
                    Text(locationParts.joinToString(" · "),style=MaterialTheme.typography.bodySmall)
                }
                record.note?.let{Text("Not: "+it)}
                Text(formatTime(record.createdAt),style=MaterialTheme.typography.bodySmall)
                Row(verticalAlignment=Alignment.CenterVertically){
                    Checkbox(
                        checked=record.includeInReport,
                        onCheckedChange={checked->onToggleReport(record.id,checked)}
                    )
                    Text("Rapora dahil")
                }
                if(record.status==ProblemRecordStatus.OPEN){
                    TextButton(onClick={onClose(record.id)}){
                        Text(if(isAdvantage)"Avantajı kapat" else "Problemi kapat")
                    }
                }

                HorizontalDivider(Modifier.padding(vertical=8.dp))
                FindingEvidencePhotos(
                    repository=repository,
                    problemRecordId=record.id,
                    photoEnabled=photoEnabled,
                    onTakePhoto={onTakePhoto(record.id)},
                    onToggleReport=onTogglePhotoReport
                )
            }
        }
    }

    Button(onClick=onAdd){
        Icon(
            if(isAdvantage)Icons.Outlined.CheckCircle else Icons.Outlined.ReportProblem,
            contentDescription=null
        )
        Spacer(Modifier.width(8.dp))
        Text(if(isAdvantage)"Avantaj ekle" else "Problem ekle")
    }
}

@Composable
private fun FindingEvidencePhotos(
    repository:SahaRepository,
    problemRecordId:String,
    photoEnabled:Boolean,
    onTakePhoto:()->Unit,
    onToggleReport:(String,Boolean)->Unit
){
    val photos by remember(problemRecordId){repository.observeFindingPhotos(problemRecordId)}
        .collectAsStateWithLifecycle(initialValue=emptyList())

    Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.SpaceBetween
        ){
            Text(
                "Kanıt fotoğrafları ("+photos.size+")",
                style=MaterialTheme.typography.labelLarge
            )
            FilledTonalButton(
                onClick=onTakePhoto,
                enabled=photoEnabled
            ){
                Icon(Icons.Outlined.CameraAlt,contentDescription=null)
                Spacer(Modifier.width(6.dp))
                Text("Fotoğraf ekle")
            }
        }

        if(photos.isNotEmpty()){
            LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                items(photos,key={it.id}){photo->
                    OutlinedCard(Modifier.width(240.dp)){
                        Row(
                            Modifier.fillMaxWidth().padding(8.dp),
                            verticalAlignment=Alignment.CenterVertically
                        ){
                            LocalPhotoThumbnail(photo.localUri)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)){
                                Text(formatTime(photo.createdAt),style=MaterialTheme.typography.bodySmall)
                                Row(verticalAlignment=Alignment.CenterVertically){
                                    Checkbox(
                                        checked=photo.includeInReport,
                                        onCheckedChange={checked->onToggleReport(photo.id,checked)}
                                    )
                                    Text("Rapora dahil",style=MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }

        if(photos.isEmpty()){
            Text(
                "Henüz kanıt fotoğrafı yok. İstediğin kadar ekleyebilirsin.",
                style=MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
internal fun DeficiencySection(
    repository:SahaRepository,
    records:List<DeficiencyEntity>,
    photoEnabled:Boolean,
    onTakePhoto:(String)->Unit,
    onTogglePhotoReport:(String,Boolean)->Unit,
    onToggleReport:(String,Boolean)->Unit,
    onStatus:(String,DeficiencyStatus)->Unit,
    onEdit:(DeficiencyEntity)->Unit,
    onAdd:()->Unit
){
    HorizontalDivider()
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.SpaceBetween
    ){
        Text("İmalat Eksikleri",style=MaterialTheme.typography.titleMedium)
        Text(
            records.count{it.status!=DeficiencyStatus.VERIFIED}.toString()+" aktif",
            style=MaterialTheme.typography.labelMedium,
            color=MaterialTheme.colorScheme.tertiary
        )
    }

    records.forEach{record->
        OutlinedCard(
            modifier=Modifier.fillMaxWidth(),
            colors=CardDefaults.outlinedCardColors(
                containerColor=if(record.status==DeficiencyStatus.VERIFIED)
                    MaterialTheme.colorScheme.surface
                else
                    MaterialTheme.colorScheme.tertiaryContainer
            )
        ){
            Column(
                Modifier.padding(12.dp),
                verticalArrangement=Arrangement.spacedBy(6.dp)
            ){
                Row(verticalAlignment=Alignment.CenterVertically){
                    Icon(
                        Icons.Outlined.Warning,
                        contentDescription=null,
                        tint=if(record.priority==DeficiencyPriority.CRITICAL)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(record.title,Modifier.weight(1f),style=MaterialTheme.typography.titleSmall)
                    AssistChip(onClick={},label={Text(record.priority.label)})
                }

                record.description?.let{Text(it)}
                val location=buildList{
                    record.floor?.let{add("Kat: "+it)}
                    record.unitNumber?.let{add("No: "+it)}
                    record.unitName?.let{add("Mahal / daire / birim: "+it)}
                }
                if(location.isNotEmpty()){
                    Text(location.joinToString(" · "),style=MaterialTheme.typography.bodySmall)
                }
                record.targetDate?.let{
                    Text("Hedef: "+it,style=MaterialTheme.typography.bodySmall)
                }

                Text("Durum",style=MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    items(DeficiencyStatus.entries,key={it.name}){status->
                        FilterChip(
                            selected=record.status==status,
                            onClick={onStatus(record.id,status)},
                            label={Text(status.label)}
                        )
                    }
                }

                Row(verticalAlignment=Alignment.CenterVertically){
                    Checkbox(
                        checked=record.includeInReport,
                        onCheckedChange={checked->onToggleReport(record.id,checked)}
                    )
                    Text("Rapora dahil")
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick={onEdit(record)}){Text("Düzenle")}
                }

                HorizontalDivider(Modifier.padding(vertical=4.dp))
                DeficiencyEvidencePhotos(
                    repository=repository,
                    deficiencyId=record.id,
                    photoEnabled=photoEnabled,
                    onTakePhoto={onTakePhoto(record.id)},
                    onToggleReport=onTogglePhotoReport
                )
            }
        }
    }

    Button(onClick=onAdd){
        Icon(Icons.Outlined.Warning,contentDescription=null)
        Spacer(Modifier.width(8.dp))
        Text("Eksik ekle")
    }
}

@Composable
private fun DeficiencyEvidencePhotos(
    repository:SahaRepository,
    deficiencyId:String,
    photoEnabled:Boolean,
    onTakePhoto:()->Unit,
    onToggleReport:(String,Boolean)->Unit
){
    val photos by remember(deficiencyId){repository.observeDeficiencyPhotos(deficiencyId)}
        .collectAsStateWithLifecycle(initialValue=emptyList())

    Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.SpaceBetween
        ){
            Text("Eksik fotoğrafları ("+photos.size+")",style=MaterialTheme.typography.labelLarge)
            FilledTonalButton(onClick=onTakePhoto,enabled=photoEnabled){
                Icon(Icons.Outlined.CameraAlt,contentDescription=null)
                Spacer(Modifier.width(6.dp))
                Text("Fotoğraf ekle")
            }
        }
        if(photos.isNotEmpty()){
            LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                items(photos,key={it.id}){photo->
                    OutlinedCard(Modifier.width(240.dp)){
                        Row(
                            Modifier.fillMaxWidth().padding(8.dp),
                            verticalAlignment=Alignment.CenterVertically
                        ){
                            LocalPhotoThumbnail(photo.localUri)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)){
                                Text(formatTime(photo.createdAt),style=MaterialTheme.typography.bodySmall)
                                Row(verticalAlignment=Alignment.CenterVertically){
                                    Checkbox(
                                        checked=photo.includeInReport,
                                        onCheckedChange={checked->onToggleReport(photo.id,checked)}
                                    )
                                    Text("Rapora dahil",style=MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
        if(photos.isEmpty()){
            Text("Henüz eksik fotoğrafı yok.",style=MaterialTheme.typography.bodySmall)
        }
    }
}

