package com.automapoko.app.ui.screens.onboarding

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.automapoko.app.util.PermissionChecker
import kotlinx.coroutines.launch

private enum class OnboardingStep { WELCOME, OVERLAY, AUTOSTART, BATTERY, DONE }

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val permissionChecker = remember { PermissionChecker(context) }

    var currentStep by remember { mutableStateOf(OnboardingStep.WELCOME) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Indicador de progresso
            val steps = OnboardingStep.entries
            val index = steps.indexOf(currentStep)
            LinearProgressIndicator(
                progress = { (index + 1).toFloat() / steps.size.toFloat() },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    slideInHorizontally { it } + fadeIn() togetherWith
                    slideOutHorizontally { -it } + fadeOut()
                },
                modifier = Modifier.weight(1f),
                label = "onboarding_step"
            ) { step ->
                when (step) {
                    OnboardingStep.WELCOME -> WelcomeStep()
                    OnboardingStep.OVERLAY -> OverlayStep(
                        granted = permissionChecker.hasOverlayPermission(),
                        onGrant = { activity?.let { permissionChecker.openOverlaySettings(it) } }
                    )
                    OnboardingStep.AUTOSTART -> AutostartStep()
                    OnboardingStep.BATTERY -> BatteryStep(
                        granted = permissionChecker.hasBatteryOptimizationExemption(),
                        onGrant = {
                            activity?.let {
                                permissionChecker.openBatteryOptimizationSettings(it)
                            }
                        }
                    )
                    OnboardingStep.DONE -> DoneStep()
                }
            }

            // Botões
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentStep != OnboardingStep.WELCOME) {
                    OutlinedButton(
                        onClick = {
                            currentStep = OnboardingStep.entries[index - 1]
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null,
                            modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Voltar")
                    }
                }

                Button(
                    onClick = {
                        if (currentStep == OnboardingStep.DONE) {
                            scope.launch {
                                permissionChecker.setOnboardingCompleted()
                                onFinished()
                            }
                        } else {
                            currentStep = OnboardingStep.entries[index + 1]
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = if (currentStep == OnboardingStep.DONE) "Começar" else "Continuar",
                        fontWeight = FontWeight.SemiBold
                    )
                    if (currentStep != OnboardingStep.DONE) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Filled.ArrowForward, contentDescription = null,
                            modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Bolt,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Bem-vindo ao\nAutomaPoko",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Crie automações que abrem seus apps favoritos " +
                "automaticamente — baseadas em Bluetooth, Wi-Fi ou localização.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Vamos configurar 3 permissões necessárias para que tudo funcione corretamente.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun OverlayStep(granted: Boolean, onGrant: () -> Unit) {
    OnboardingPermissionStep(
        icon = Icons.Filled.Layers,
        stepNumber = "1 de 3",
        title = "Abrir apps automaticamente",
        description = "Para abrir aplicativos sem você tocar na tela, o AutomaPoko precisa da " +
            "permissão \"Exibir sobre outros aplicativos\".",
        instructions = listOf(
            "Toque em \"Conceder permissão\" abaixo",
            "Na tela que abrir, encontre o AutomaPoko",
            "Ative a opção \"Permitir exibição sobre outros apps\"",
            "Volte ao AutomaPoko"
        ),
        granted = granted,
        onGrant = onGrant
    )
}

@Composable
private fun AutostartStep() {
    OnboardingPermissionStep(
        icon = Icons.Filled.PlayCircle,
        stepNumber = "2 de 3",
        title = "Início automático (HyperOS)",
        description = "No HyperOS (Xiaomi), é necessário autorizar o início automático do " +
            "AutomaPoko para que as automações funcionem com o app fechado.",
        instructions = listOf(
            "Abra o app \"Segurança\" do seu Poco X7 Pro",
            "Vá em \"Gerenciar aplicativos\"",
            "Busque e selecione \"AutomaPoko\"",
            "Ative \"Início automático\"",
            "Volte ao AutomaPoko"
        ),
        granted = null, // Não verificável programaticamente no HyperOS
        onGrant = null
    )
}

@Composable
private fun BatteryStep(granted: Boolean, onGrant: () -> Unit) {
    OnboardingPermissionStep(
        icon = Icons.Filled.BatteryChargingFull,
        stepNumber = "3 de 3",
        title = "Isenção de otimização de bateria",
        description = "Impede que o Android encerre o AutomaPoko em segundo plano, " +
            "garantindo que as automações sempre funcionem.",
        instructions = listOf(
            "Toque em \"Conceder permissão\" abaixo",
            "Na tela que abrir, confirme a isenção",
            "Volte ao AutomaPoko"
        ),
        granted = granted,
        onGrant = onGrant
    )
}

@Composable
private fun DoneStep() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Tudo configurado!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "O AutomaPoko está pronto para criar suas primeiras automações.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun OnboardingPermissionStep(
    icon: ImageVector,
    stepNumber: String,
    title: String,
    description: String,
    instructions: List<String>,
    granted: Boolean?,
    onGrant: (() -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Cabeçalho
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stepNumber,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Status de permissão (quando verificável)
        if (granted != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (granted) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (granted) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                        contentDescription = null,
                        tint = if (granted) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = if (granted) "Permissão concedida" else "Permissão pendente",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (granted) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // Instruções
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Como fazer:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                instructions.forEachIndexed { index, instruction ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                        Text(
                            text = instruction,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Botão de ação (quando há ação programática)
        if (onGrant != null) {
            Button(
                onClick = onGrant,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Filled.OpenInNew, contentDescription = null,
                    modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Conceder permissão", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
