package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import com.example.data.PetState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PetCanvas(
    petState: PetState,
    activeInteraction: String,
    modifier: Modifier = Modifier
) {
    // 1. Continuous breathing animation (running infinitely)
    val infiniteTransition = rememberInfiniteTransition(label = "PetAnimations")
    
    val breatheScaleY by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EasyInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreathingY"
    )

    val breatheScaleX by infiniteTransition.animateFloat(
        initialValue = 1.03f,
        targetValue = 0.97f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EasyInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreathingX"
    )

    // 2. Extra bouncing animation if happy or playing
    val isHighlyHappy = petState.happiness > 70f || activeInteraction == "petting"
    val bounceAnimationSpeed = if (activeInteraction == "playing") 400 else 1000
    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isHighlyHappy || activeInteraction == "playing") -24f else -4f,
        animationSpec = infiniteRepeatable(
            animation = tween(bounceAnimationSpeed, easing = FastOutSlowInWithBounce),
            repeatMode = RepeatMode.Reverse
        ),
        label = "HappyBounce"
    )

    // 3. Eye Blinking effect
    val eyeBlinkTimer by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 4000
                1f at 0 // Open
                1f at 3700 // Open
                0f at 3850 // Closed
                1f at 4000 // Open
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "EyeBlink"
    )

    // 4. Floating animations for indicators (Zzz, Hearts, Bubbles)
    val floatPercent by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "FloatingParticles"
    )

    // Determine color schemes based on equipped skins and active evolution path
    val (baseColor, shadowColor) = remember(petState.skinColor, petState.evolutionPath) {
        getSkinColors(petState.skinColor, petState.evolutionPath)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
    ) {
        val centerX = size.width / 2f
        val centerY = size.height * 0.62f
        val petRadius = size.width * 0.22f

        // Draw background path aura effects
        drawEvolutionAura(petState.evolutionPath, centerX, centerY, petRadius + bounceOffset, floatPercent)

        withTransform({
            // Apply bounce translation
            translate(top = bounceOffset)
            // Apply breathing scale from the center of the pet base
            scale(scaleX = breatheScaleX, scaleY = breatheScaleY, pivot = Offset(centerX, centerY + petRadius))
        }) {
            // Draw pet shadow on the ground
            drawShadow(centerX, centerY + petRadius, petRadius, breatheScaleX)

            // Draw primary body blob
            val isEgg = petState.evolutionStage == "Huevo"
            val bodyPath = Path().apply {
                if (isEgg) {
                    val topY = centerY - petRadius * 1.15f
                    val bottomY = centerY + petRadius * 0.95f
                    val leftX = centerX - petRadius * 0.82f
                    val rightX = centerX + petRadius * 0.82f
                    
                    moveTo(centerX, topY)
                    // Narrower at the top, wider at the bottom to form a cute egg shape
                    cubicTo(centerX + petRadius * 0.45f, topY, rightX, centerY - petRadius * 0.2f, rightX, centerY + petRadius * 0.2f)
                    cubicTo(rightX, centerY + petRadius * 0.7f, centerX + petRadius * 0.6f, bottomY, centerX, bottomY)
                    cubicTo(centerX - petRadius * 0.6f, bottomY, leftX, centerY + petRadius * 0.7f, leftX, centerY + petRadius * 0.2f)
                    cubicTo(leftX, centerY - petRadius * 0.2f, centerX - petRadius * 0.45f, topY, centerX, topY)
                } else {
                    val topY = centerY - petRadius * 1.05f
                    val bottomY = centerY + petRadius
                    val leftX = centerX - petRadius * 1.02f
                    val rightX = centerX + petRadius * 1.02f
                    
                    // Rounded bubbly blob
                    moveTo(centerX, topY)
                    cubicTo(rightX, topY, rightX, bottomY, centerX, bottomY)
                    cubicTo(leftX, bottomY, leftX, topY, centerX, topY)
                }
            }

            // Draw body base
            drawPath(
                path = bodyPath,
                brush = Brush.radialGradient(
                    colors = if (isEgg) listOf(Color(0xFF81D4FA), Color(0xFF0288D1)) else listOf(baseColor, shadowColor),
                    center = Offset(centerX - 12f, centerY - 20f),
                    radius = petRadius * 1.4f
                )
            )

            if (isEgg) {
                // Draw cute colorful polka dots on the egg
                val spotColor = Color(0xFFFFCC80) // Pastel orange
                drawCircle(color = spotColor, radius = petRadius * 0.14f, center = Offset(centerX - petRadius * 0.35f, centerY - petRadius * 0.2f))
                drawCircle(color = Color(0xFFA5D6A7), radius = petRadius * 0.12f, center = Offset(centerX + petRadius * 0.32f, centerY + petRadius * 0.1f))
                drawCircle(color = Color(0xFFE1BEE7), radius = petRadius * 0.15f, center = Offset(centerX, centerY - petRadius * 0.5f))
                drawCircle(color = spotColor, radius = petRadius * 0.1f, center = Offset(centerX - petRadius * 0.15f, centerY + petRadius * 0.4f))

                // Cute sleeping anime eyes on the egg ( - . - )
                val eyeL = Offset(centerX - petRadius * 0.25f, centerY + petRadius * 0.12f)
                val eyeR = Offset(centerX + petRadius * 0.25f, centerY + petRadius * 0.12f)
                val eyeSize = petRadius * 0.08f
                
                drawArc(
                    color = Color(0xFF1B5E20), // Dark forest green lines on sky blue egg
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(eyeL.x - eyeSize, eyeL.y - eyeSize * 0.5f),
                    size = Size(eyeSize * 2f, eyeSize),
                    style = Stroke(width = 5f, cap = StrokeCap.Round)
                )
                drawArc(
                    color = Color(0xFF1B5E20),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(eyeR.x - eyeSize, eyeR.y - eyeSize * 0.5f),
                    size = Size(eyeSize * 2f, eyeSize),
                    style = Stroke(width = 5f, cap = StrokeCap.Round)
                )
                
                // Cute tiny mouth / blush on the egg
                drawCircle(color = Color(0x66FF8A80), radius = petRadius * 0.08f, center = Offset(centerX - petRadius * 0.35f, centerY + petRadius * 0.24f))
                drawCircle(color = Color(0x66FF8A80), radius = petRadius * 0.08f, center = Offset(centerX + petRadius * 0.35f, centerY + petRadius * 0.24f))
            } else {
                // Draw specialized evolution physical traits
                drawEvolutionFeatures(petState.evolutionStage, petState.evolutionPath, centerX, centerY, petRadius)

                // Draw facial features based on status modes (Sleeping, Sad, Hungry, Tired, Happy, Healthy)
                drawFace(
                    petState = petState,
                    activeInteraction = activeInteraction,
                    centerX = centerX,
                    centerY = centerY,
                    petRadius = petRadius,
                    blinkFactor = eyeBlinkTimer
                )

                // Draw secondary accessories (Party Hat, Sunglasses, Bowtie, Crown, Wizard Hat)
                drawAccessories(
                    accessoryId = petState.equippedAccessory,
                    centerX = centerX,
                    centerY = centerY,
                    petRadius = petRadius
                )
            }
        }

        // Draw interaction bubbles, floating hearts or eating crumbs that aren't scaled with breathing
        drawInteractionEffects(
            interactionType = activeInteraction,
            isSleeping = petState.isSleeping,
            centerX = centerX,
            centerY = centerY,
            petRadius = petRadius,
            floatVal = floatPercent
        )
    }
}

private val EasyInOut = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
private val FastOutSlowInWithBounce = CubicBezierEasing(0.175f, 0.885f, 0.32f, 1.275f)

private fun getSkinColors(skinName: String, pathName: String): Pair<Color, Color> {
    // Override colors if ultimate evolution is present
    if (pathName == "Fuego") return Pair(Color(0xFFFF5252), Color(0xFFC62828))
    if (pathName == "Agua") return Pair(Color(0xFF40C4FF), Color(0xFF0277BD))
    if (pathName == "Natura") return Pair(Color(0xFF69F0AE), Color(0xFF2E7D32))
    if (pathName == "Cosmos") return Pair(Color(0xFFB388FF), Color(0xFF4A148C))

    return when (skinName.lowercase()) {
        "sky" -> Pair(Color(0xFF90CAF9), Color(0xFF1565C0))
        "mint" -> Pair(Color(0xFFA5D6A7), Color(0xFF2E7D32))
        "peach" -> Pair(Color(0xFFFFAB91), Color(0xFFD84315))
        "gold" -> Pair(Color(0xFFFFE082), Color(0xFFF57F17))
        else -> Pair(Color(0xFFE1BEE7), Color(0xFF6A1B9A)) // Lilac default Purple
    }
}

private fun DrawScope.drawShadow(cx: Float, cy: Float, radius: Float, xStretch: Float) {
    drawOval(
        color = Color(0x33000000),
        topLeft = Offset(cx - radius * 0.9f * xStretch, cy - 10f),
        size = Size(radius * 1.8f * xStretch, 20f)
    )
}

private fun DrawScope.drawEvolutionAura(path: String, cx: Float, cy: Float, petR: Float, floatP: Float) {
    when (path) {
        "Fuego" -> {
            // Drawn flaming particles escaping upwards
            for (i in 0..4) {
                val seed = i * 72f
                val rad = seed * PI.toFloat() / 180f
                val floatOffset = floatP * 50f
                val pX = cx + (petR + 10f + floatOffset * 0.2f) * sin(rad)
                val pY = cy - 20f - floatOffset - (i * 10f)
                val fireColor = if (i % 2 == 0) Color(0xFFFF9100) else Color(0xFFFF3D00)
                
                drawCircle(
                    color = fireColor.copy(alpha = 1f - floatP),
                    radius = 8f * (1f - floatP),
                    center = Offset(pX, pY)
                )
            }
        }
        "Agua" -> {
            // Water bubbles floating up
            for (i in 0..3) {
                val floatOffset = ((floatP + (i * 0.25f)) % 1f) * 60f
                val pX = cx - petR + (i * petR * 0.6f) + sin(floatOffset * 0.08f) * 15f
                val pY = cy + 20f - floatOffset
                drawCircle(
                    color = Color(0xFF00E5FF).copy(alpha = 0.5f * (1f - (floatOffset/60f))),
                    radius = 6f,
                    center = Offset(pX, pY),
                    style = Stroke(width = 2f)
                )
            }
        }
        "Natura" -> {
            // Little leaves revolving
            for (i in 0..2) {
                val angle = (floatP * 360f + i * 120f) * PI / 180f
                val pX = cx + (petR + 25f) * cos(angle).toFloat()
                val pY = (cy - 10f) + (petR * 0.4f) * sin(angle).toFloat()
                
                drawOval(
                    color = Color(0xFF00E676).copy(alpha = 0.8f),
                    topLeft = Offset(pX - 6f, pY - 4f),
                    size = Size(12f, 8f)
                )
            }
        }
        "Cosmos" -> {
            // Tiny glowing stars
            for (i in 0..3) {
                val pulse = sin(floatP * 2f * PI + i).toFloat()
                val radius = 4f + pulse * 2f
                val pX = cx - petR * 1.1f + (i * petR * 0.7f)
                val pY = cy - petR * 0.8f + (i % 2 * 20f) - (pulse * 10f)
                
                drawStar(pX, pY, radius, Color(0xFFE040FB))
            }
        }
    }
}

private fun DrawScope.drawStar(x: Float, y: Float, size: Float, color: Color) {
    val path = Path().apply {
        moveTo(x, y - size)
        lineTo(x + size * 0.3f, y - size * 0.3f)
        lineTo(x + size, y)
        lineTo(x + size * 0.3f, y + size * 0.3f)
        lineTo(x, y + size)
        lineTo(x - size * 0.3f, y + size * 0.3f)
        lineTo(x - size, y)
        lineTo(x - size * 0.3f, y - size * 0.3f)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawEvolutionFeatures(stage: String, path: String, cx: Float, cy: Float, petR: Float) {
    // Add horns or leaf buds on head based on decisions/evolution state
    val isEvolved = stage != "Bebé"
    if (!isEvolved) return

    when (path) {
        "Fuego" -> {
            // Drawing dual little fire horns
            val hornPathL = Path().apply {
                moveTo(cx - petR * 0.4f, cy - petR * 0.8f)
                quadraticTo(cx - petR * 0.8f, cy - petR * 1.5f, cx - petR * 0.7f, cy - petR * 1.6f)
                quadraticTo(cx - petR * 0.4f, cy - petR * 1.2f, cx - petR * 0.2f, cy - petR * 0.88f)
            }
            val hornPathR = Path().apply {
                moveTo(cx + petR * 0.4f, cy - petR * 0.8f)
                quadraticTo(cx + petR * 0.8f, cy - petR * 1.5f, cx + petR * 0.7f, cy - petR * 1.6f)
                quadraticTo(cx + petR * 0.4f, cy - petR * 1.2f, cx + petR * 0.2f, cy - petR * 0.88f)
            }
            drawPath(hornPathL, Color(0xFFFF3D00))
            drawPath(hornPathR, Color(0xFFFF3D00))
        }
        "Agua" -> {
            // Little cute sea-fin ears on sides
            val finL = Path().apply {
                moveTo(cx - petR * 0.95f, cy - petR * 0.1f)
                quadraticTo(cx - petR * 1.4f, cy - petR * 0.4f, cx - petR * 1.3f, cy + petR * 0.2f)
                quadraticTo(cx - petR * 1.1f, cy + petR * 0.1f, cx - petR * 0.95f, cy + petR * 0.15f)
            }
            val finR = Path().apply {
                moveTo(cx + petR * 0.95f, cy - petR * 0.1f)
                quadraticTo(cx + petR * 1.4f, cy - petR * 0.4f, cx + petR * 1.3f, cy + petR * 0.2f)
                quadraticTo(cx + petR * 1.1f, cy + petR * 0.1f, cx + petR * 0.95f, cy + petR * 0.15f)
            }
            drawPath(finL, Color(0xFF00B0FF))
            drawPath(finR, Color(0xFF00B0FF))
        }
        "Natura" -> {
            // Sprouting green flower bud on head
            drawRect(
                color = Color(0xFF4CAF50),
                topLeft = Offset(cx - 3f, cy - petR * 1.16f),
                size = Size(6f, petR * 0.2f)
            )
            val leafL = Path().apply {
                moveTo(cx, cy - petR * 1.16f)
                quadraticTo(cx - 15f, cy - petR * 1.3f, cx - 25f, cy - petR * 1.25f)
                quadraticTo(cx - 12f, cy - petR * 1.12f, cx, cy - petR * 1.16f)
            }
            val leafR = Path().apply {
                moveTo(cx, cy - petR * 1.16f)
                quadraticTo(cx + 15f, cy - petR * 1.3f, cx + 25f, cy - petR * 1.25f)
                quadraticTo(cx + 12f, cy - petR * 1.12f, cx, cy - petR * 1.16f)
            }
            drawPath(leafL, Color(0xFF81C784))
            drawPath(leafR, Color(0xFF81C784))
        }
        "Cosmos" -> {
            // Little cute celestial star antenna
            drawRect(
                color = Color(0xFFBA68C8),
                topLeft = Offset(cx - 2f, cy - petR * 1.25f),
                size = Size(4f, petR * 0.3f)
            )
            drawStar(cx, cy - petR * 1.3f, 12f, Color(0xFFFFD54F))
        }
    }
}

private fun DrawScope.drawFace(
    petState: PetState,
    activeInteraction: String,
    centerX: Float,
    centerY: Float,
    petRadius: Float,
    blinkFactor: Float
) {
    val isSleeping = petState.isSleeping
    val isSad = petState.happiness < 35f && activeInteraction != "petting"
    val isTired = petState.sleep < 30f && !isSleeping
    val isHungry = petState.hunger < 30f

    val eyeOffsetOuterX = petRadius * 0.38f
    val eyeOffsetY = petRadius * 0.1f
    
    val eyeL = Offset(centerX - eyeOffsetOuterX, centerY - eyeOffsetY)
    val eyeR = Offset(centerX + eyeOffsetOuterX, centerY - eyeOffsetY)
    val eyeR_size = petRadius * 0.12f

    // --- DRAW EYES ---
    when {
        isSleeping -> {
            // Closed happy/relaxed eyes (^^)
            drawArc(
                color = Color(0x99000000),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(eyeL.x - eyeR_size, eyeL.y - eyeR_size * 0.5f),
                size = Size(eyeR_size * 2, eyeR_size),
                style = Stroke(width = 6f, cap = StrokeCap.Round)
            )
            drawArc(
                color = Color(0x99000000),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(eyeR.x - eyeR_size, eyeR.y - eyeR_size * 0.5f),
                size = Size(eyeR_size * 2, eyeR_size),
                style = Stroke(width = 6f, cap = StrokeCap.Round)
            )
        }
        isSad -> {
            // Downward looking sad crying eyes with dynamic blue tear drops
            drawCircle(color = Color(0xFF263238), radius = eyeR_size, center = eyeL)
            drawCircle(color = Color(0xFF263238), radius = eyeR_size, center = eyeR)
            
            // Highlights
            drawCircle(color = Color.White, radius = eyeR_size * 0.3f, center = Offset(eyeL.x - 3f, eyeL.y - 3f))
            drawCircle(color = Color.White, radius = eyeR_size * 0.3f, center = Offset(eyeR.x - 3f, eyeR.y - 3f))

            // Tear droplets running down
            val tearY = centerY + petRadius * 0.18f
            drawCircle(color = Color(0xFF40C4FF), radius = 6f, center = Offset(eyeL.x, tearY))
            drawCircle(color = Color(0xFF40C4FF), radius = 6f, center = Offset(eyeR.x, tearY))
        }
        isTired -> {
            // Half open heavy eyes
            val eyePathL = Path().apply {
                addOval(Rect(eyeL.x - eyeR_size, eyeL.y - eyeR_size * 0.5f, eyeL.x + eyeR_size, eyeL.y + eyeR_size * 0.5f))
            }
            val eyePathR = Path().apply {
                addOval(Rect(eyeR.x - eyeR_size, eyeR.y - eyeR_size * 0.5f, eyeR.x + eyeR_size, eyeR.y + eyeR_size * 0.5f))
            }
            drawPath(eyePathL, Color(0xFF37474F))
            drawPath(eyePathR, Color(0xFF37474F))
            // Drooping eyelid lines
            drawLine(
                color = Color(0xFF78909C),
                start = Offset(eyeL.x - eyeR_size * 1.1f, eyeL.y - 3f),
                end = Offset(eyeL.x + eyeR_size * 1.1f, eyeL.y - 1f),
                strokeWidth = 4f
            )
            drawLine(
                color = Color(0xFF78909C),
                start = Offset(eyeR.x - eyeR_size * 1.1f, eyeR.y - 3f),
                end = Offset(eyeR.x + eyeR_size * 1.1f, eyeR.y - 1f),
                strokeWidth = 4f
            )
        }
        else -> {
            // Default healthy blinking eyes
            val animatedHeight = eyeR_size * blinkFactor
            if (animatedHeight > 2f) {
                drawOval(
                    color = Color(0xFF212121),
                    topLeft = Offset(eyeL.x - eyeR_size, eyeL.y - animatedHeight),
                    size = Size(eyeR_size * 2f, animatedHeight * 2f)
                )
                drawOval(
                    color = Color(0xFF212121),
                    topLeft = Offset(eyeR.x - eyeR_size, eyeR.y - animatedHeight),
                    size = Size(eyeR_size * 2f, animatedHeight * 2f)
                )
                // Spark highlights
                drawCircle(color = Color.White, radius = eyeR_size * 0.35f, center = Offset(eyeL.x - 3f, eyeL.y - eyeR_size * 0.34f))
                drawCircle(color = Color.White, radius = eyeR_size * 0.35f, center = Offset(eyeR.x - 3f, eyeR.y - eyeR_size * 0.34f))
            } else {
                // Line for closed blink
                drawLine(
                    color = Color(0xFF212121),
                    start = Offset(eyeL.x - eyeR_size, eyeL.y),
                    end = Offset(eyeL.x + eyeR_size, eyeL.y),
                    strokeWidth = 5f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color(0xFF212121),
                    start = Offset(eyeR.x - eyeR_size, eyeR.y),
                    end = Offset(eyeR.x + eyeR_size, eyeR.y),
                    strokeWidth = 5f,
                    cap = StrokeCap.Round
                )
            }
        }
    }

    // --- DRAW CHEEKS (Blush) ---
    val blushColor = Color(0x66FF8A80)
    val blushRadius = petRadius * 0.12f
    if (!isSleeping) {
        drawCircle(color = blushColor, radius = blushRadius, center = Offset(eyeL.x - petRadius * 0.15f, eyeL.y + petRadius * 0.18f))
        drawCircle(color = blushColor, radius = blushRadius, center = Offset(eyeR.x + petRadius * 0.15f, eyeR.y + petRadius * 0.18f))
    }

    // --- DRAW MOUTH ---
    val mouthY = centerY + petRadius * 0.14f
    when {
        isSleeping -> {
            // Cute tiny horizontal line mouth
            drawLine(
                color = Color(0xFF212121),
                start = Offset(centerX - 8f, mouthY),
                end = Offset(centerX + 8f, mouthY),
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )
        }
        activeInteraction == "eating" -> {
            // Chomping circular mouth
            drawCircle(
                color = Color(0xFF424242),
                radius = 12f,
                center = Offset(centerX, mouthY)
            )
        }
        isSad || isHungry -> {
            // Sad down-mouth arc (inverted smile)
            drawArc(
                color = Color(0xFF212121),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(centerX - 16f, mouthY - 4f),
                size = Size(32f, 16f),
                style = Stroke(width = 5f, cap = StrokeCap.Round)
            )
        }
        else -> {
            // Wide happy cat-like smile (w shape)
            val mouthPath = Path().apply {
                moveTo(centerX - 16f, mouthY)
                quadraticTo(centerX - 8f, mouthY + 10f, centerX, mouthY + 1f)
                quadraticTo(centerX + 8f, mouthY + 10f, centerX + 16f, mouthY)
            }
            drawPath(
                path = mouthPath,
                color = Color(0xFF212121),
                style = Stroke(width = 5f, cap = StrokeCap.Round)
            )
        }
    }
}

private fun DrawScope.drawAccessories(accessoryId: String, centerX: Float, centerY: Float, petRadius: Float) {
    when (accessoryId) {
        "party_hat" -> {
            // Striped party hat on head
            val hatTopY = centerY - petRadius * 1.9f
            val baseLeft = Offset(centerX - petRadius * 0.45f, centerY - petRadius * 0.98f)
            val baseRight = Offset(centerX + petRadius * 0.45f, centerY - petRadius * 0.98f)
            
            val hatPath = Path().apply {
                moveTo(centerX, hatTopY)
                lineTo(baseRight.x, baseRight.y)
                lineTo(baseLeft.x, baseLeft.y)
                close()
            }
            drawPath(hatPath, Color(0xFFE91E63))
            
            // Draw yellow stripes
            drawLine(
                color = Color(0xFFFFEB3B),
                start = Offset(centerX - petRadius * 0.2f, centerY - petRadius * 1.4f),
                end = Offset(centerX + petRadius * 0.2f, centerY - petRadius * 1.4f),
                strokeWidth = 10f
            )
            drawLine(
                color = Color(0xFFFFEB3B),
                start = Offset(centerX - petRadius * 0.35f, centerY - petRadius * 1.15f),
                end = Offset(centerX + petRadius * 0.35f, centerY - petRadius * 1.15f),
                strokeWidth = 10f
            )

            // Pom-pom on top
            drawCircle(color = Color(0xFF00BCD4), radius = 10f, center = Offset(centerX, hatTopY - 4f))
        }
        "sunglasses" -> {
            // Cool retro black pixel/shades
            val glassesY = centerY - petRadius * 0.16f
            val width = petRadius * 0.44f
            val height = petRadius * 0.18f
            
            val leftBox = Rect(centerX - petRadius * 0.85f, glassesY, centerX - petRadius * 0.08f, glassesY + height)
            val rightBox = Rect(centerX + petRadius * 0.08f, glassesY, centerX + petRadius * 0.85f, glassesY + height)
            
            // Draw left shade
            drawRoundRect(
                color = Color(0xFF1E1E1E),
                topLeft = Offset(leftBox.left, leftBox.top),
                size = Size(leftBox.width, leftBox.height),
                cornerRadius = CornerRadius(10f, 10f)
            )
            // Draw right shade
            drawRoundRect(
                color = Color(0xFF1E1E1E),
                topLeft = Offset(rightBox.left, rightBox.top),
                size = Size(rightBox.width, rightBox.height),
                cornerRadius = CornerRadius(10f, 10f)
            )
            // Bridge line
            drawLine(
                color = Color(0xFF1E1E1E),
                start = Offset(centerX - petRadius * 0.1f, glassesY + height * 0.3f),
                end = Offset(centerX + petRadius * 0.1f, glassesY + height * 0.3f),
                strokeWidth = 8f
            )
            // Highlight shine lines
            drawLine(
                color = Color.White,
                start = Offset(leftBox.left + 5f, leftBox.top + 5f),
                end = Offset(leftBox.left + 20f, leftBox.top + 15f),
                strokeWidth = 4f
            )
            drawLine(
                color = Color.White,
                start = Offset(rightBox.left + 5f, rightBox.top + 5f),
                end = Offset(rightBox.left + 20f, rightBox.top + 15f),
                strokeWidth = 4f
            )
        }
        "bowtie" -> {
            // Red bow tie under neck
            val tieY = centerY + petRadius * 0.96f
            val leftKnot = Path().apply {
                moveTo(centerX, tieY)
                lineTo(centerX - 35f, tieY - 18f)
                lineTo(centerX - 35f, tieY + 18f)
                close()
            }
            val rightKnot = Path().apply {
                moveTo(centerX, tieY)
                lineTo(centerX + 35f, tieY - 18f)
                lineTo(centerX + 35f, tieY + 18f)
                close()
            }
            drawPath(leftKnot, Color(0xFFD50000))
            drawPath(rightKnot, Color(0xFFD50000))
            drawCircle(color = Color(0xFF9E0000), radius = 10f, center = Offset(centerX, tieY))
        }
        "crown" -> {
            // Royal gold crown floating above head
            val crownY = centerY - petRadius * 1.55f
            val cWidth = petRadius * 0.72f
            val cHeight = petRadius * 0.36f
            
            val crownPath = Path().apply {
                moveTo(centerX - cWidth * 0.5f, crownY)
                lineTo(centerX - cWidth * 0.5f, crownY - cHeight)
                lineTo(centerX - cWidth * 0.2f, crownY - cHeight * 0.4f)
                lineTo(centerX, crownY - cHeight * 1.2f)
                lineTo(centerX + cWidth * 0.2f, crownY - cHeight * 0.4f)
                lineTo(centerX + cWidth * 0.5f, crownY - cHeight)
                lineTo(centerX + cWidth * 0.5f, crownY)
                close()
            }
            drawPath(crownPath, Color(0xFFFFD54F))
            
            // Red gems on crown spokes
            drawCircle(color = Color(0xFFD50000), radius = 4f, center = Offset(centerX - cWidth * 0.5f, crownY - cHeight))
            drawCircle(color = Color(0xFFD50000), radius = 4f, center = Offset(centerX, crownY - cHeight * 1.2f))
            drawCircle(color = Color(0xFFD50000), radius = 4f, center = Offset(centerX + cWidth * 0.5f, crownY - cHeight))
        }
        "wizard_hat" -> {
            // Deep indigo magic hat with a yellow crescent moon
            val hatTopY = centerY - petRadius * 2.1f
            val baseLeft = Offset(centerX - petRadius * 0.65f, centerY - petRadius * 0.95f)
            val baseRight = Offset(centerX + petRadius * 0.65f, centerY - petRadius * 0.95f)
            
            val brimPath = Path().apply {
                val brimY = centerY - petRadius * 0.92f
                moveTo(centerX - petRadius * 0.85f, brimY)
                quadraticTo(centerX, brimY - 10f, centerX + petRadius * 0.85f, brimY)
                quadraticTo(centerX, brimY + 8f, centerX - petRadius * 0.85f, brimY)
            }
            
            val conePath = Path().apply {
                moveTo(centerX, hatTopY)
                quadraticTo(centerX + petRadius * 0.35f, centerY - petRadius * 1.4f, baseRight.x, baseRight.y)
                lineTo(baseLeft.x, baseLeft.y)
                quadraticTo(centerX - petRadius * 0.35f, centerY - petRadius * 1.4f, centerX, hatTopY)
                close()
            }
            
            drawPath(conePath, Color(0xFF3F51B5))
            drawPath(brimPath, Color(0xFF303F9F))
            
            // Star on the wizard hat
            drawStar(centerX, centerY - petRadius * 1.4f, 10f, Color(0xFFFFEB3B))
        }
    }
}

private fun DrawScope.drawInteractionEffects(
    interactionType: String,
    isSleeping: Boolean,
    centerX: Float,
    centerY: Float,
    petRadius: Float,
    floatVal: Float
) {
    if (isSleeping) {
        // Floating Zzzs moving upward in sequence
        for (i in 0..2) {
            val personalFloat = ((floatVal + i * 0.33f) % 1f)
            val zOffset = personalFloat * 80f
            val zX = centerX + petRadius * 0.65f + personalFloat * 20f
            val zY = centerY - petRadius * 0.8f - zOffset
            
            val zSize = 8f + personalFloat * 10f
            drawZ(zX, zY, zSize, Color(0xAA1E88E5).copy(alpha = 1f - personalFloat))
        }
    }

    when (interactionType) {
        "petting" -> {
            // Heart particles floating out from the sides
            for (i in 0..2) {
                val personalFloat = ((floatVal + i * 0.33f) % 1f)
                val heartOffset = personalFloat * 75f
                val hX = centerX - petRadius + (i * petRadius) + sin(personalFloat * 10f) * 20f
                val hY = centerY - petRadius - heartOffset
                
                drawHeart(hX, hY, 15f * (1f - personalFloat), Color(0xFFFF4081).copy(alpha = 1f - personalFloat))
            }
        }
        "eating" -> {
            // Delicious cookies particles or feeding fragments scattering
            for (i in 0..4) {
                val rad = (i * 72f) * PI / 180f
                val explodeDist = floatVal * 40f
                val pX = centerX + explodeDist * cos(rad).toFloat()
                val pY = centerY + petRadius * 0.14f + explodeDist * sin(rad).toFloat()
                
                if (floatVal < 0.8f) {
                    drawCircle(
                        color = Color(0xFF8D6E63), // brown crumbs
                        radius = 4f * (1f - floatVal),
                        center = Offset(pX, pY)
                    )
                }
            }
        }
        "cleaning" -> {
            // Sparkly bubble sparkles floating around
            for (i in 0..3) {
                val personalFloat = ((floatVal + i * 0.25f) % 1f)
                val bubbleOffset = personalFloat * 90f
                val bX = centerX - petRadius * 0.9f + (i * petRadius * 0.6f)
                val bY = centerY + petRadius * 0.8f - bubbleOffset
                
                if (personalFloat < 0.9f) {
                    drawCircle(
                        color = Color(0xCC80DEEA),
                        radius = 12f * (1f - personalFloat),
                        center = Offset(bX, bY),
                        style = Stroke(width = 3f)
                    )
                    // shine highlight in bubble
                    drawCircle(
                        color = Color.White.copy(alpha = 0.6f * (1f - personalFloat)),
                        radius = 3f * (1f - personalFloat),
                        center = Offset(bX - 4f, bY - 4f)
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawZ(x: Float, y: Float, size: Float, color: Color) {
    val path = Path().apply {
        moveTo(x - size * 0.5f, y - size * 0.5f)
        lineTo(x + size * 0.5f, y - size * 0.5f)
        lineTo(x - size * 0.5f, y + size * 0.5f)
        lineTo(x + size * 0.5f, y + size * 0.5f)
    }
    drawPath(path, color, style = Stroke(width = size * 0.22f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.drawHeart(x: Float, y: Float, size: Float, color: Color) {
    if (size <= 1f) return
    val path = Path().apply {
        val width = size * 2f
        val height = size * 1.8f
        
        // Left lobe
        moveTo(x, y + height * 0.25f)
        cubicTo(x - width * 0.5f, y - height * 0.35f, x - width, y + height * 0.15f, x, y + height)
        // Right lobe
        moveTo(x, y + height * 0.25f)
        cubicTo(x + width * 0.5f, y - height * 0.35f, x + width, y + height * 0.15f, x, y + height)
    }
    drawPath(path, color)
}
