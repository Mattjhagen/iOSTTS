package com.metroreader.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.metroreader.app.ui.theme.InterFontFamily
import com.metroreader.app.ui.theme.MetroAccentBlue

/**
 * Displays a book cover image with a Metro-style placeholder fallback.
 */
@Composable
fun BookCoverImage(
    coverPath: String?,
    title: String,
    modifier: Modifier = Modifier,
    accentColor: Color = MetroAccentBlue
) {
    if (!coverPath.isNullOrBlank()) {
        SubcomposeAsyncImage(
            model = coverPath,
            contentDescription = "Cover of $title",
            modifier = modifier,
            contentScale = ContentScale.Crop,
            loading = {
                BookCoverPlaceholder(title = title, modifier = modifier, accentColor = accentColor)
            },
            error = {
                BookCoverPlaceholder(title = title, modifier = modifier, accentColor = accentColor)
            }
        )
    } else {
        BookCoverPlaceholder(title = title, modifier = modifier, accentColor = accentColor)
    }
}

@Composable
fun BookCoverPlaceholder(
    title: String,
    modifier: Modifier = Modifier,
    accentColor: Color = MetroAccentBlue
) {
    Box(
        modifier = modifier.background(accentColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.MenuBook,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                ),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
