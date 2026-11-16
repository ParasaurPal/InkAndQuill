package com.parasaurpal.inkandquill.data

import android.content.ContentResolver
import android.net.Uri
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint

object PdfExport {
    fun write(resolver:ContentResolver,uri:Uri,title:String,html:String){
        val text=title+"\n\n"+DocumentExport.plain(html)
        val paint=TextPaint(Paint.ANTI_ALIAS_FLAG).apply{color=android.graphics.Color.BLACK;textSize=12f;typeface=Typeface.SERIF}
        val width=499
        val pageHeight=842
        val margin=48
        val layout=StaticLayout.Builder.obtain(text,0,text.length,paint,width).setAlignment(Layout.Alignment.ALIGN_NORMAL).setLineSpacing(3f,1f).build()
        val pdf=PdfDocument()
        try{
            var line=0
            var pageNumber=1
            while(line<layout.lineCount){
                val page=pdf.startPage(PdfDocument.PageInfo.Builder(595,pageHeight,pageNumber).create())
                val canvas=page.canvas
                canvas.translate(margin.toFloat(),margin.toFloat())
                var end=line
                while(end<layout.lineCount && layout.getLineBottom(end)-layout.getLineTop(line)<=pageHeight-2*margin)end++
                if(end==line)end++
                val startOffset=layout.getLineStart(line)
                val endOffset=if(end<layout.lineCount)layout.getLineStart(end)else text.length
                val part=StaticLayout.Builder.obtain(text,startOffset,endOffset,paint,width).setAlignment(Layout.Alignment.ALIGN_NORMAL).setLineSpacing(3f,1f).build()
                part.draw(canvas)
                pdf.finishPage(page)
                line=end
                pageNumber++
            }
            resolver.openOutputStream(uri)?.use{pdf.writeTo(it)}
        }finally{pdf.close()}
    }
}
