package com.parasaurpal.inkandquill.data

import android.content.ContentResolver
import android.net.Uri
import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object PortableExport {
    private fun xml(s:String)=s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;")
    private fun entry(zip:ZipOutputStream,name:String,content:String){zip.putNextEntry(ZipEntry(name));zip.write(content.toByteArray(Charsets.UTF_8));zip.closeEntry()}
    fun docx(resolver:ContentResolver,uri:Uri,title:String,html:String){
        val text=DocumentExport.plain(html)
        val paragraphs=(listOf(title)+text.split("\n")).joinToString(""){line->"<w:p><w:r><w:t xml:space=\"preserve\">${xml(line)}</w:t></w:r></w:p>"}
        val types="""<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/></Types>"""
        val rels="""<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/></Relationships>"""
        val document="""<?xml version="1.0" encoding="UTF-8" standalone="yes"?><w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>${paragraphs}<w:sectPr/></w:body></w:document>"""
        resolver.openOutputStream(uri)?.use{out->ZipOutputStream(out).use{z->entry(z,"[Content_Types].xml",types);entry(z,"_rels/.rels",rels);entry(z,"word/document.xml",document)}}
    }
    fun epub(resolver:ContentResolver,uri:Uri,title:String,html:String){
        val xhtml="""<?xml version="1.0" encoding="UTF-8"?><html xmlns="http://www.w3.org/1999/xhtml"><head><title>${xml(title)}</title><meta charset="utf-8"/></head><body><h1>${xml(title)}</h1>$html</body></html>"""
        val container="""<?xml version="1.0" encoding="UTF-8"?><container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container"><rootfiles><rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/></rootfiles></container>"""
        val opf="""<?xml version="1.0" encoding="UTF-8"?><package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="bookid"><metadata xmlns:dc="http://purl.org/dc/elements/1.1/"><dc:identifier id="bookid">urn:uuid:${java.util.UUID.randomUUID()}</dc:identifier><dc:title>${xml(title)}</dc:title><dc:language>en</dc:language></metadata><manifest><item id="content" href="content.xhtml" media-type="application/xhtml+xml"/><item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/></manifest><spine><itemref idref="content"/></spine></package>"""
        val nav="""<?xml version="1.0" encoding="UTF-8"?><html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops"><head><title>Contents</title></head><body><nav epub:type="toc"><h1>Contents</h1><ol><li><a href="content.xhtml">${xml(title)}</a></li></ol></nav></body></html>"""
        resolver.openOutputStream(uri)?.use{out->ZipOutputStream(out).use{z->
            val bytes="application/epub+zip".toByteArray();val crc=CRC32().apply{update(bytes)}
            val mime=ZipEntry("mimetype").apply{method=ZipEntry.STORED;size=bytes.size.toLong();compressedSize=bytes.size.toLong();this.crc=crc.value}
            z.putNextEntry(mime);z.write(bytes);z.closeEntry()
            entry(z,"META-INF/container.xml",container);entry(z,"OEBPS/content.xhtml",xhtml);entry(z,"OEBPS/content.opf",opf);entry(z,"OEBPS/nav.xhtml",nav)
        }}
    }
    fun extractPdf(context:Context,uri:Uri):String{PDFBoxResourceLoader.init(context);return context.contentResolver.openInputStream(uri)?.use{input->PDDocument.load(input).use{document->PDFTextStripper().getText(document)}}?: ""}
    fun extractDocx(resolver:ContentResolver,uri:Uri):String=extract(resolver,uri,"word/document.xml").replace(Regex("<w:p[ >].*?</w:p>",RegexOption.DOT_MATCHES_ALL)){p->Regex("<w:t[^>]*>(.*?)</w:t>",RegexOption.DOT_MATCHES_ALL).findAll(p.value).joinToString(""){unxml(it.groupValues[1])}+"\n"}.trim()
    fun extractEpub(resolver:ContentResolver,uri:Uri):String=DocumentExport.plain(extract(resolver,uri,"OEBPS/content.xhtml"))
    private fun extract(resolver:ContentResolver,uri:Uri,path:String):String{resolver.openInputStream(uri)?.use{input->ZipInputStream(input).use{zip->while(true){val e=zip.nextEntry?:break;if(e.name==path)return zip.readBytes().toString(Charsets.UTF_8)}}};return ""}
    private fun unxml(s:String)=s.replace("&lt;","<").replace("&gt;",">").replace("&quot;","\"").replace("&apos;","'").replace("&amp;","&")
}
