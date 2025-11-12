package ru.wassertech.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

data class OnboardingPage(
    val title: String,
    val description: String,
    val imageRes: Int? = null
)

@Composable
fun OnboardingScreen(
    pages: List<OnboardingPage> = defaultOnboardingPages,
    onFinish: () -> Unit,
    onSkip: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Кнопка "Пропустить"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onSkip) {
                Text("Пропустить")
            }
        }
        
        // Pager с страницами
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            OnboardingPageContent(
                page = pages[page],
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Индикаторы и кнопки
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Индикаторы страниц
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(pages.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (pagerState.currentPage == index) 12.dp else 8.dp)
                            .padding(4.dp)
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = if (pagerState.currentPage == index) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxSize()
                        ) {}
                    }
                }
            }
            
            // Кнопка "Далее" / "Начать"
            Button(
                onClick = {
                    if (pagerState.currentPage < pages.size - 1) {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        onFinish()
                    }
                }
            ) {
                Text(if (pagerState.currentPage < pages.size - 1) "Далее" else "Начать")
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Изображение (если есть)
        page.imageRes?.let { imageRes ->
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = null,
                modifier = Modifier
                    .size(200.dp)
                    .padding(bottom = 32.dp),
                contentScale = ContentScale.Fit
            )
        }
        
        // Заголовок
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Описание
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private val defaultOnboardingPages = listOf(
    OnboardingPage(
        title = "Добро пожаловать",
        description = "Добро пожаловать в Wassertech! Управляйте вашими клиентами и объектами эффективно."
    ),
    OnboardingPage(
        title = "Простое управление",
        description = "Легко добавляйте клиентов, объекты и отслеживайте техническое обслуживание."
    ),
    OnboardingPage(
        title = "Начните работу",
        description = "Войдите в систему или зарегистрируйтесь, чтобы начать работу."
    )
)

