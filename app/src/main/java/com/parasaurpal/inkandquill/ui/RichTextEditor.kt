package com.parasaurpal.inkandquill.ui

import android.net.Uri
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONObject

@Composable
fun RichTextEditor(value:String,onValueChange:(String)->Unit,modifier:Modifier=Modifier,fontSize:Float=18f,fontFamily:String="sans-serif",customFontData:String="",customFontMime:String="font/ttf",lineSpacing:Float=1.65f,pageMode:Boolean=false,enabledCommands:Set<String>?=null,pageWidth:Float=760f,pageMargin:Float=12f,paragraphSpacing:Float=12f,backgroundColor:String="transparent",backgroundImageData:String="",backgroundImageMime:String="image/jpeg",textColor:String="#eee8f5",findTerm:String="",findOccurrence:Int=0,onFindHandled:()->Unit={}){
    val callback by rememberUpdatedState(onValueChange)
    val findCallback by rememberUpdatedState(onFindHandled)
    val context=LocalContext.current
    val web=remember{mutableStateOf<WebView?>(null)}
    var linkDialog by remember{mutableStateOf(false)}
    var linkUrl by remember{mutableStateOf("https://")}
    val imagePicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri:Uri?->uri?.let{runCatching{val bytes=context.contentResolver.openInputStream(it)?.use{stream->stream.readBytes()}?:byteArrayOf();val mime=context.contentResolver.getType(it)?: "image/png";val data="data:$mime;base64,"+Base64.encodeToString(bytes,Base64.NO_WRAP);web.value?.evaluateJavascript("document.execCommand('insertImage',false,"+JSONObject.quote(data)+")",null)}}}
    val commands=listOf(
        "undo" to "↶","redo" to "↷","bold" to "B","italic" to "I","underline" to "U","strikeThrough" to "S",
        "formatBlock:H1" to "H1","formatBlock:H2" to "H2","formatBlock:H3" to "H3",
        "insertUnorderedList" to "•","insertOrderedList" to "1.","hiliteColor" to "Highlight","foreColor" to "Color",
        "justifyLeft" to "⇤","justifyCenter" to "↔","justifyRight" to "⇥","indent" to "Indent","outdent" to "Outdent","formatBlock:BLOCKQUOTE" to "Quote","insertHorizontalRule" to "Rule","removeFormat" to "Tx"
    )
    Column(modifier){
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(2.dp)){
            commands.filter{enabledCommands==null||it.first in enabledCommands}.forEach{(cmd,label)->
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
        Row(Modifier.horizontalScroll(rememberScrollState())){TextButton(onClick={linkDialog=true}){Text("Insert link")};TextButton(onClick={imagePicker.launch(arrayOf("image/*"))}){Text("Insert image")};TextButton(onClick={val table="""<table border="1" style="border-collapse:collapse;width:100%"><tr><td>Cell</td><td>Cell</td></tr><tr><td>Cell</td><td>Cell</td></tr></table><p></p>""";web.value?.evaluateJavascript("document.execCommand('insertHTML',false,"+JSONObject.quote(table)+")",null)}){Text("Insert table")};TextButton(onClick={val pageBreak="""<div class="page-break"></div><p></p>""";web.value?.evaluateJavascript("document.execCommand('insertHTML',false,"+JSONObject.quote(pageBreak)+")",null)}){Text("Page break")}}
        AndroidView(modifier=Modifier.fillMaxSize(),factory={ctx->
            WebView(ctx).apply{
                settings.javaScriptEnabled=true
                settings.domStorageEnabled=false
                isVerticalScrollBarEnabled=true
                webViewClient=WebViewClient()
                addJavascriptInterface(object{@JavascriptInterface fun changed(html:String){callback(html)}},"InkBridge")
                val initial=JSONObject.quote(value)
                val page="""<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1"><style>
                @font-face{font-family:Custom;src:url(data:${customFontMime};base64,${customFontData})}
                body{font-family:${if(customFontData.isNotBlank()) "Custom" else fontFamily};font-size:${fontSize}px;line-height:${lineSpacing};padding:${pageMargin}px;margin:0 auto;max-width:${if(pageMode) "${pageWidth}px" else "none"};min-height:${if(pageMode) "1056px" else "95vh"};box-sizing:border-box;${if(pageMode) "box-shadow:0 4px 22px rgba(0,0,0,.25);border:1px solid #8b8495;" else ""}background:${backgroundColor};background-image:${if(backgroundImageData.isNotBlank()) "url(data:${backgroundImageMime};base64,${backgroundImageData})" else "none"};background-size:cover;background-attachment:fixed;color:${textColor}} p{margin-bottom:${paragraphSpacing}px}
                #editor{min-height:95vh;outline:none;overflow-wrap:anywhere} blockquote{border-left:3px solid #bfa7df;padding-left:12px;color:#aaa}
                h1,h2,h3{line-height:1.25} a{color:#bfa7df} .page-break{break-after:page;page-break-after:always;height:1px} @media print{.page-break{break-after:page;page-break-after:always}}
                </style></head><body><div id="editor" contenteditable="true"></div><script>
                const editor=document.getElementById('editor');
                editor.innerHTML=$initial;
                editor.addEventListener('input',()=>InkBridge.changed(editor.innerHTML));
                function setEditorHtml(v){if(document.activeElement!==editor)editor.innerHTML=v;}
                </script></body></html>"""
                loadDataWithBaseURL(null,page,"text/html","UTF-8",null)
            }.also{web.value=it}
        },update={view->
            view.evaluateJavascript("document.body.style.fontSize='${fontSize}px';document.body.style.fontFamily='${if(customFontData.isNotBlank()) "Custom" else fontFamily}';document.body.style.lineHeight='${lineSpacing}';document.body.style.maxWidth='${if(pageMode) "${pageWidth}px" else "none"}';document.body.style.minHeight='${if(pageMode) "1056px" else "95vh"}';document.body.style.boxSizing='border-box';document.body.style.boxShadow='${if(pageMode) "0 4px 22px rgba(0,0,0,.25)" else "none"}';document.body.style.padding='${pageMargin}px';document.body.style.background='${backgroundColor}';document.body.style.backgroundImage='${if(backgroundImageData.isNotBlank()) "url(data:${backgroundImageMime};base64,${backgroundImageData})" else "none"}';document.body.style.backgroundSize='cover';document.body.style.backgroundAttachment='fixed';document.body.style.color='${textColor}';document.querySelectorAll('p').forEach(p=>p.style.marginBottom='${paragraphSpacing}px')",null)
            if(!view.hasFocus()){val q=JSONObject.quote(value);view.evaluateJavascript("setEditorHtml($q)",null)}
            if(findTerm.isNotBlank()){view.setFindListener{_,count,done->if(done){if(count>0)repeat(findOccurrence.coerceIn(0,count-1)){view.findNext(true)};findCallback()}};view.findAllAsync(findTerm)}
        })
    }
    if(linkDialog)AlertDialog(onDismissRequest={linkDialog=false},title={Text("Insert hyperlink")},text={OutlinedTextField(linkUrl,{linkUrl=it},label={Text("URL")},singleLine=true)},confirmButton={TextButton(onClick={if(linkUrl.startsWith("https://")||linkUrl.startsWith("http://")){web.value?.evaluateJavascript("document.execCommand('createLink',false,"+JSONObject.quote(linkUrl)+")",null);linkDialog=false}}){Text("Insert")}},dismissButton={TextButton(onClick={linkDialog=false}){Text("Cancel")}})
}
