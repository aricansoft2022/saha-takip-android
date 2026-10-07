package com.aricansoft.sahatakip.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun InfoTooltip(text:String,modifier:Modifier=Modifier){
    var expanded by remember{mutableStateOf(false)}
    Box(modifier){
        IconButton(onClick={expanded=true}){
            Icon(Icons.Outlined.Info,contentDescription="Açıklama")
        }
        DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}){
            Text(text,Modifier.padding(horizontal=12.dp,vertical=8.dp))
        }
    }
}

fun formatTime(epochMillis:Long):String=
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
