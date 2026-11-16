package com.parasaurpal.inkandquill.data

import android.text.Html
import android.text.TextUtils

object DocumentExport {
    fun plain(html:String):String = Html.fromHtml(html,Html.FROM_HTML_MODE_COMPACT).toString()
    fun markdown(html:String):String {
        var s=html
        s=s.replace(Regex("(?i)<br\\s*/?>"),"\n")
            .replace(Regex("(?i)</p>|</div>"),"\n\n")
            .replace(Regex("(?i)<h1[^>]*>"),"# ").replace(Regex("(?i)</h1>"),"\n\n")
            .replace(Regex("(?i)<h2[^>]*>"),"## ").replace(Regex("(?i)</h2>"),"\n\n")
            .replace(Regex("(?i)<h3[^>]*>"),"### ").replace(Regex("(?i)</h3>"),"\n\n")
            .replace(Regex("(?i)<strong>|<b>"),"**").replace(Regex("(?i)</strong>|</b>"),"**")
            .replace(Regex("(?i)<em>|<i>"),"*").replace(Regex("(?i)</em>|</i>"),"*")
            .replace(Regex("(?i)<u>"),"__").replace(Regex("(?i)</u>"),"__")
            .replace(Regex("(?i)<li[^>]*>"),"- ").replace(Regex("(?i)</li>"),"\n")
            .replace(Regex("(?i)</?(ul|ol)[^>]*>"),"\n")
            .replace(Regex("(?i)<blockquote[^>]*>"),"> ").replace(Regex("(?i)</blockquote>"),"\n")
        return Html.fromHtml(s,Html.FROM_HTML_MODE_COMPACT).toString().trim()
    }
    fun markdownToHtml(markdown:String):String {
        val out=StringBuilder()
        var inList=false
        markdown.lines().forEach{line->
            val safe=TextUtils.htmlEncode(line)
                .replace(Regex("\\*\\*(.+?)\\*\\*"),"<strong>$1</strong>")
                .replace(Regex("\\*(.+?)\\*"),"<em>$1</em>")
                .replace(Regex("`(.+?)`"),"<code>$1</code>")
            when{
                line.startsWith("# ")->{if(inList){out.append("</ul>");inList=false};out.append("<h1>").append(safe.drop(2)).append("</h1>")}
                line.startsWith("## ")->{if(inList){out.append("</ul>");inList=false};out.append("<h2>").append(safe.drop(3)).append("</h2>")}
                line.startsWith("### ")->{if(inList){out.append("</ul>");inList=false};out.append("<h3>").append(safe.drop(4)).append("</h3>")}
                line.startsWith("- ")||line.startsWith("* ")->{if(!inList){out.append("<ul>");inList=true};out.append("<li>").append(safe.drop(2)).append("</li>")}
                line.isBlank()->{if(inList){out.append("</ul>");inList=false};out.append("<br/>")}
                else->{if(inList){out.append("</ul>");inList=false};out.append("<p>").append(safe).append("</p>")}
            }
        }
        if(inList)out.append("</ul>")
        return out.toString()
    }
    fun htmlDocument(title:String,body:String):String = """<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>$title</title><style>body{max-width:760px;margin:40px auto;padding:0 20px;font:18px/1.7 Georgia,serif}img{max-width:100%}</style></head><body>$body</body></html>"""
    fun plainToHtml(text:String):String = TextUtils.htmlEncode(text).replace("\n","<br>")
}
