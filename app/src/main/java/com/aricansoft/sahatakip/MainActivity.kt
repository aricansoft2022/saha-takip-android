package com.aricansoft.sahatakip

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aricansoft.sahatakip.ui.SahaApp

class MainActivity:ComponentActivity(){
    private var incomingGkteUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        incomingGkteUri=intent.gkteUriOrNull()

        setContent{
            MaterialTheme{
                Surface(color=MaterialTheme.colorScheme.background){
                    SahaApp(
                        externalGkteUri=incomingGkteUri,
                        onExternalGkteConsumed={incomingGkteUri=null}
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent:Intent){
        super.onNewIntent(intent)
        setIntent(intent)
        incomingGkteUri=intent.gkteUriOrNull()
    }

    private fun Intent.gkteUriOrNull():Uri?=
        if(action==Intent.ACTION_VIEW) data else null
}
