package com.cleanroom.weekplate.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cleanroom.weekplate.core.model.Recipe
import com.cleanroom.weekplate.ui.theme.*
import kotlin.math.absoluteValue

@Composable
fun WeekplateWordmark(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(Tangerine),
            contentAlignment = Alignment.Center
        ) {
            Text("W", color = Forest, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.width(9.dp))
        Text("weekplate", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun RecipeArtwork(recipe: Recipe, modifier: Modifier = Modifier) {
    val palette = listOf(
        listOf(Color(0xFFE0AE6B), Color(0xFF6D8B67), Color(0xFFFFE4BD)),
        listOf(Color(0xFFD47252), Color(0xFFF4C46C), Color(0xFF496D54)),
        listOf(Color(0xFF88A0B4), Color(0xFFF0C795), Color(0xFF315D50)),
        listOf(Color(0xFFB59269), Color(0xFFE9D8B6), Color(0xFF778C5C))
    )[recipe.id.hashCode().absoluteValue % 4]
    Box(modifier.clip(RoundedCornerShape(22.dp)).background(palette[2])) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(palette[0], radius = size.minDimension * .42f, center = center)
            drawCircle(palette[1], radius = size.minDimension * .25f, center = center)
            rotate(24f) {
                drawRect(
                    color = palette[2].copy(alpha = .65f),
                    topLeft = androidx.compose.ui.geometry.Offset(size.width * .18f, size.height * .44f),
                    size = androidx.compose.ui.geometry.Size(size.width * .64f, size.height * .12f)
                )
            }
        }
        Text(
            recipe.name.take(1),
            modifier = Modifier.align(Alignment.Center),
            style = MaterialTheme.typography.displaySmall,
            color = Color.White.copy(alpha = .9f)
        )
    }
}

@Composable
fun RecipeCard(
    recipe: Recipe,
    favourite: Boolean,
    onClick: () -> Unit,
    onFavourite: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            RecipeArtwork(recipe, Modifier.size(92.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(recipe.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(5.dp))
                Text(
                    "${recipe.timeMinutes} min · ${recipe.nutrition.calories} kcal · ${recipe.cuisine}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(5.dp))
                Text(recipe.blurb, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onFavourite, contentPadding = PaddingValues(8.dp)) {
                Text(if (favourite) "♥" else "♡", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Composable
fun SectionHeading(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) TextButton(onClick = onAction) { Text(action) }
    }
}

@Composable
fun Pill(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilterChip(
        modifier = modifier,
        selected = selected,
        onClick = onClick,
        label = { Text(text) },
        shape = RoundedCornerShape(50)
    )
}

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(76.dp).clip(RoundedCornerShape(28.dp)).background(Sage), contentAlignment = Alignment.Center) {
            Text("W", style = MaterialTheme.typography.headlineMedium, color = Forest)
        }
        Spacer(Modifier.height(18.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(7.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
