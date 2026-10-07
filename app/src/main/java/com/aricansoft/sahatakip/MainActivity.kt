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
    companion object {
        private const val STATE_CONSUMED_GKTE_URI="consumed_gkte_uri"
    }

    private var incomingGkteUri by mutableStateOf<Uri?>(null)
    private var consumedGkteUri:String?=null

    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        consumedGkteUri=savedInstanceState?.getString(STATE_CONSUMED_GKTE_URI)
        incomingGkteUri=intent.gkteUriOrNull()
            ?.takeUnless{it.toString()==consumedGkteUri}

        setContent{
            MaterialTheme{
                Surface(color=MaterialTheme.colorScheme.background){
                    SahaApp(
                        externalGkteUri=incomingGkteUri,
                        onExternalGkteConsumed={
                            incomingGkteUri?.let{uri->consumedGkteUri=uri.toString()}
                            incomingGkteUri=null
                            setIntent(Intent(Intent.ACTION_MAIN))
                        }
                    )
                }
            }
        }
    }

    override fun onSaveInstanceState(outState:Bundle){
        consumedGkteUri?.let{outState.putString(STATE_CONSUMED_GKTE_URI,it)}
        super.onSaveInstanceState(outState)
    }

    override fun onNewIntent(intent:Intent){
        super.onNewIntent(intent)
        setIntent(intent)
        consumedGkteUri=null
        incomingGkteUri=intent.gkteUriOrNull()
    }

    private fun Intent.gkteUriOrNull():Uri?=
        if(action==Intent.ACTION_VIEW) data else null
}
