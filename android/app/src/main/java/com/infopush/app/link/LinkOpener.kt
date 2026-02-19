package com.infopush.app.link

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class LinkOpener(
    private val context: Context,
    private val processor: LinkProcessor = LinkProcessor(),
    private val scope: CoroutineScope
) {
    fun open(rawUrl: String) {
        scope.launch {
            when (val prepared = processor.prepare(rawUrl)) {
                is PreparedLink.Invalid -> {
                    Toast.makeText(context, prepared.message, Toast.LENGTH_SHORT).show()
                }
                is PreparedLink.Valid -> {
                    val target = prepared.finalUrl.ifBlank { prepared.fallbackUrl }
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(target)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    val ok = runCatching { context.startActivity(intent) }.isSuccess
                    if (!ok) {
                        Toast.makeText(context, "无法打开链接", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
