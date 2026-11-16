package com.parasaurpal.inkandquill.ui

import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONObject

@Composable
fun RichTextEditor(value:String,onValueChange:(String)->Unit,modifier:Modifier=Modifier,fontSize:Float=18f){
    val callback by rememberUpdatedState(onValueChange)
    val web=remember{mutableStateOf<WebView?>(null)}
    val commands=listOf(
        "undo" to "↶","redo" to "↷","bold" to "B","italic" to "I","underline" to "U","strikeThrough" to "S",
        "formatBlock:H1" to "H1","formatBlock:H2" to "H2","formatBlock:H3" to "H3",
        "insertUnorderedList" to "•","insertOrderedList" to "1.","hiliteColor" to "Highlight","foreColor" to "Color",
        "justifyLeft" to "⇤","justifyCenter" to "↔","justifyRight" to "⇥","removeFormat" to "Tx"
    )
    Column(modifier){
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(2.dp)){
            commands.forEach{(cmd,label)->
                TextButton(onClick={
                    val action=when{
                        cmd.startsWith("formatBlock:")->"document.execCommand('formatBlock',false,'${cmd.substringAfter(":")}')"
                        cmd=="hiliteColor"->"document.execCommand('hiliteColor',false,'#fff2a8')"
                        cmd=="foreColor"->"document.execCommand('foreColor',false,'#b79ad8')"
                        else->"document.execCommand('$cmd',false,null)"
                    }
                    web.value?.evaluateJavascript(action,null)
                }){Text(label)}
            }
        }
        AndroidView(modifier=Modifier.fillMaxSize(),factory={ctx->
            WebView(ctx).apply{
                settings.javaScriptEnabled=true
                settings.domStorageEnabled=false
                isVerticalScrollBarEnabled=true
                webViewClient=WebViewClient()
                addJavascriptInterface(object{@JavascriptInterface fun changed(html:String){callback(html)}},"InkBridge")
                val initial=JSONObject.quote(value)
                val page="""<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1"><style>
                body{font-family:sans-serif;font-size:${fontSize}px;line-height:1.65;padding:12px;margin:0;background:transparent;color:var(--ink,#eee8f5)}
                #editor{min-height:95vh;outline:none;overflow-wrap:anywhere} blockquote{border-left:3px solid #bfa7df;padding-left:12px;color:#aaa}
                h1,h2,h3{line-height:1.25} a{color:#bfa7df}
                </style></head><body><div id="editor" contenteditable="true"></div><script>
                const editor=document.getElementById('editor');
                editor.innerHTML=$initial;
                editor.addEventListener('input',()=>InkBridge.changed(editor.innerHTML));
                function setEditorHtml(v){if(document.activeElement!==editor)editor.innerHTML=v;}
                </script></body></html>"""
                loadDataWithBaseURL(null,page,"text/html","UTF-8",null)
            }.also{web.value=it}
        },update={view->
            view.evaluateJavascript("document.body.style.fontSize='${fontSize}px'",null)
            if(!view.hasFocus()){val q=JSONObject.quote(value);view.evaluateJavascript("setEditorHtml($q)",null)}
        })
    }
}
