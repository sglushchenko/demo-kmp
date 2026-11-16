package pet.shared.presentation.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import pet.shared.di.AppContainer
import pet.shared.generated.resources.Res
import pet.shared.generated.resources.compose_subtitle
import pet.shared.generated.resources.compose_title
import pet.shared.generated.resources.local_banner
import pet.shared.presentation.characters.CharactersUiState

/** Compose rendering of the same ViewModel and data as the SwiftUI tab. */
@Composable
fun ComposeApp(container: AppContainer) {
    val viewModel = remember(container) { container.makeCharactersViewModel() }
    DisposableEffect(viewModel) { onDispose { viewModel.close() } }
    val state by viewModel.state.collectAsState()

    MaterialTheme {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // From this module's composeResources, not the app's asset catalog.
            Image(
                painter = painterResource(Res.drawable.local_banner),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(120.dp),
            )
            Text(stringResource(Res.string.compose_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(Res.string.compose_subtitle), style = MaterialTheme.typography.bodySmall)

            when (val s = state) {
                is CharactersUiState.Loading -> Text("Loading...")
                is CharactersUiState.Error -> {
                    Text(s.error.message)
                    Text("Request ID: ${s.error.requestId}", style = MaterialTheme.typography.bodySmall)
                    if (s.error.isRetryable) Button(onClick = viewModel::retry) { Text("Retry") }
                }
                is CharactersUiState.Content -> LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(s.items, key = { it.id }) { item ->
                        Text("${item.name} - ${item.statusLabel} - ${item.species}")
                    }
                    if (s.error != null) {
                        item { Text(s.error.message, color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }
}
