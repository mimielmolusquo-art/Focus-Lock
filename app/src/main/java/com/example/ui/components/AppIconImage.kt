package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap

@Composable
fun AppIconImage(
    drawable: Drawable?,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    if (drawable != null) {
        val bitmap = remember(drawable) {
            try {
                val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
                val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bmp)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bmp.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }

        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = contentDescription,
                modifier = modifier.clip(CircleShape)
            )
        } else {
            DefaultAppIcon(modifier = modifier, contentDescription = contentDescription)
        }
    } else {
        DefaultAppIcon(modifier = modifier, contentDescription = contentDescription)
    }
}

@Composable
private fun DefaultAppIcon(
    modifier: Modifier,
    contentDescription: String?
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Apps,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
