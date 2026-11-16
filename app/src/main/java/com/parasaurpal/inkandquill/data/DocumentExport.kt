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
            .replace(Regex("(?i)<a[^>]*href=\"([^\"]+)\"[^>]*>(.*?)</a>"),"[$2]($1)")
            .replace(Regex("(?i)<img[^>]*src=\"([^\"]+)\"[^>]*>"),"![]($1)")
            .replace(Regex("(?i)<del>|<s>"),"~~").replace(Regex("(?i)</del>|</s>"),"~~")
            .replace(Regex("(?i)<code>"),"`").replace(Regex("(?i)</code>"),"`")
        return Html.fromHtml(s,Html.FROM_HTML_MODE_COMPACT).toString().trim()
    }
    fun markdownToHtml(markdown:String):String {
        val out=StringBuilder()
        var listType=""
        fun closeList(){if(listType.isNotBlank()){out.append("</").append(listType).append(">");listType=""}}
        markdown.lines().forEach{line->
            val safe=TextUtils.htmlEncode(line)
                .replace(Regex("!\\[(.*?)\\]\\((.*?)\\)"),"<img alt=\"$1\" src=\"$2\"/>")
                .replace(Regex("\\[(.*?)\\]\\((https?://[^)]+)\\)"),"<a href=\"$2\">$1</a>")
                .replace(Regex("~~(.+?)~~"),"<del>$1</del>")
                .replace(Regex("\\*\\*(.+?)\\*\\*"),"<strong>$1</strong>")
                .replace(Regex("\\*(.+?)\\*"),"<em>$1</em>")
                .replace(Regex("`(.+?)`"),"<code>$1</code>")
            val numbered=Regex("^\\d+\\.\\s+").containsMatchIn(line)
            when{
                line.startsWith("# ")->{closeList();out.append("<h1>").append(safe.drop(2)).append("</h1>")}
                line.startsWith("## ")->{closeList();out.append("<h2>").append(safe.drop(3)).append("</h2>")}
                line.startsWith("### ")->{closeList();out.append("<h3>").append(safe.drop(4)).append("</h3>")}
                line.startsWith("> ")->{closeList();out.append("<blockquote>").append(safe.drop(2)).append("</blockquote>")}
                line.startsWith("- ")||line.startsWith("* ")||numbered->{val target=if(numbered)"ol" else "ul";if(listType!=target){closeList();listType=target;out.append("<").append(listType).append(">")};val content=if(numbered)Regex("^\\d+\\.\\s+").replaceFirst(safe,"")else safe.drop(2);out.append("<li>").append(content).append("</li>")}
                line.isBlank()->{closeList();out.append("<br/>")}
                else->{closeList();out.append("<p>").append(safe).append("</p>")}
            }
        }
        closeList()
        return out.toString()
    }
    fun htmlDocument(title:String,body:String):String = """<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>$title</title><style>body{max-width:760px;margin:40px auto;padding:0 20px;font:18px/1.7 Georgia,serif}img{max-width:100%}</style></head><body>$body</body></html>"""
    fun plainToHtml(text:String):String = TextUtils.htmlEncode(text).replace("\n","<br>")
}
