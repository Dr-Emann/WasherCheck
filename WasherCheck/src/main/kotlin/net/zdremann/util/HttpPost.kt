package net.zdremann.util

import java.io.IOException
import java.io.Writer
import java.net.URLEncoder

object HttpPost {
    @JvmStatic
    @Throws(IOException::class)
    fun writePostQuery(writer: Writer, args: Map<String, String>) {
        writer.write(args.asSequence().map { (key, value) ->
            val encoding = "UTF-8"
            "${URLEncoder.encode(key, encoding)}=${URLEncoder.encode(value, encoding)}"
        }.joinToString(separator = "&"))
    }
}
