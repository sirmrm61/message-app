package com.sirmrm.messageapp.data.backup

import android.util.Xml
import com.sirmrm.messageapp.domain.model.SmsMessage
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.io.OutputStream

/**
 * Reads/writes the XML dialect used by "SMS Backup & Restore":
 *
 *   <smses count="N">
 *     <sms protocol="0" address="+98912..." date="1696..." type="1"
 *          subject="null" body="..." toa="null" sc_toa="null"
 *          service_center="null" read="1" status="-1" date_sent="0" readable_date="..." contact_name="..."/>
 *     ...
 *   </smses>
 */
object SmsBackupXml {

    fun write(messages: List<SmsMessage>, out: OutputStream) {
        val s = Xml.newSerializer()
        s.setOutput(out, "UTF-8")
        s.startDocument("UTF-8", true)
        s.startTag(null, "smses")
        s.attribute(null, "count", messages.size.toString())
        for (m in messages) {
            s.startTag(null, "sms")
            s.attribute(null, "protocol", "0")
            s.attribute(null, "address", m.address)
            s.attribute(null, "date", m.date.toString())
            s.attribute(null, "type", if (m.isIncoming) "1" else "2")
            s.attribute(null, "subject", "null")
            s.attribute(null, "body", m.body)
            s.attribute(null, "toa", "null")
            s.attribute(null, "sc_toa", "null")
            s.attribute(null, "service_center", "null")
            s.attribute(null, "read", if (m.read) "1" else "0")
            s.attribute(null, "status", "-1")
            s.attribute(null, "date_sent", "0")
            s.attribute(null, "readable_date", "")
            s.attribute(null, "contact_name", "")
            s.endTag(null, "sms")
        }
        s.endTag(null, "smses")
        s.endDocument()
        s.flush()
    }

    data class ParsedSms(
        val address: String,
        val body: String,
        val date: Long,
        val type: Int,
        val read: Boolean,
    )

    fun parse(input: InputStream): List<ParsedSms> {
        val parser = Xml.newPullParser()
        parser.setInput(input, "UTF-8")
        val out = ArrayList<ParsedSms>()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "sms") {
                out += ParsedSms(
                    address = parser.getAttributeValue(null, "address") ?: "",
                    body = parser.getAttributeValue(null, "body") ?: "",
                    date = parser.getAttributeValue(null, "date")?.toLongOrNull() ?: 0L,
                    type = parser.getAttributeValue(null, "type")?.toIntOrNull() ?: 1,
                    read = parser.getAttributeValue(null, "read") == "1",
                )
            }
            event = parser.next()
        }
        return out
    }
}
