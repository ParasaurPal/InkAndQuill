package com.parasaurpal.inkandquill.ui

import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONObject

@Composable
fun RichTextEditor(value:String,onValueChange:(String)->Unit,modifier:Modifier=Modifier,fontSize:Float=18f,fontFamily:String="sans-serif",lineSpacing:Float=1.65f,pageMode:Boolean=false){
    val callback by rememberUpdatedState(onValueChange)
    val web=remember{mutableStateOf<WebView?>(null)}
    var linkDialog by remember{mutableStateOf(false)}
    var linkUrl by remember{mutableStateOf("https://")}
    val commands=listOf(
        "undo" to "↶","redo" to "↷","bold" to "B","italic" to "I","underline" to "U","strikeThrough" to "S",
        "formatBlock:H1" to "H1","formatBlock:H2" to "H2","formatBlock:H3" to "H3",
        "insertUnorderedList" to "•","insertOrderedList" to "1.","hiliteColor" to "Highlight","foreColor" to "Color",
        "justifyLeft" to "⇤","justifyCenter" to "↔","justifyRight" to "⇥","indent" to "Indent","outdent" to "Outdent","formatBlock:BLOCKQUOTE" to "Quote","insertHorizontalRule" to "Rule","removeFormat" to "Tx"
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
        TextButton(onClick={linkDialog=true}){Text("Insert link")}
        AndroidView(modifier=Modifier.fillMaxSize(),factory={ctx->
            WebView(ctx).apply{
                settings.javaScriptEnabled=true
                settings.domStorageEnabled=false
                isVerticalScrollBarEnabled=true
                webViewClient=WebViewClient()
                addJavascriptInterface(object{@JavascriptInterface fun changed(html:String){callback(html)}},"InkBridge")
                val initial=JSONObject.quote(value)
                val page="""<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1"><style>
                body{font-family:${fontFamily};font-size:${fontSize}px;line-height:${lineSpacing};padding:12px;margin:0 auto;max-width:${if(pageMode) "760px" else "none"};background:transparent;color:var(--ink,#eee8f5)}
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
            view.evaluateJavascript("document.body.style.fontSize='${fontSize}px';document.body.style.fontFamily='${fontFamily}';document.body.style.lineHeight='${lineSpacing}';document.body.style.maxWidth='${if(pageMode) "760px" else "none"}'",null)
            if(!view.hasFocus()){val q=JSONObject.quote(value);view.evaluateJavascript("setEditorHtml($q)",null)}
        })
    }
    if(linkDialog)AlertDialog(onDismissRequest={linkDialog=false},title={Text("Insert hyperlink")},text={OutlinedTextField(linkUrl,{linkUrl=it},label={Text("URL")},singleLine=true)},confirmButton={TextButton(onClick={if(linkUrl.startsWith("https://")||linkUrl.startsWith("http://")){web.value?.evaluateJavascript("document.execCommand('createLink',false,"+JSONObject.quote(linkUrl)+")",null);linkDialog=false}}){Text("Insert")}},dismissButton={TextButton(onClick={linkDialog=false}){Text("Cancel")}})
}
