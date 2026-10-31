
package dev.zyutsu.idea.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.platform.LocalContext
import dev.zyutsu.idea.LocalSettings
import dev.zyutsu.idea.LocalSettingsKey
import dev.zyutsu.idea.ui.graph.GraphRoute
import dev.zyutsu.idea.ui.home.HomeRoute
import dev.zyutsu.idea.ui.note.EditorRoute
import dev.zyutsu.idea.ui.search.SearchRoute
import dev.zyutsu.idea.ui.search.TagRoute
import dev.zyutsu.idea.ui.settings.SettingsRoute
import dev.zyutsu.idea.ui.theme.IdeaTheme
import dev.zyutsu.idea.ui.theme.Palette

object Routes {
    const val HOME = "home"
    const val GRAPH = "graph"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val NOTE = "note/{id}"
    const val TAG = "tag/{name}"
    fun note(id: String) = "note/" + id
    fun tag(name: String) = "tag/" + android.net.Uri.encode(name)
}

@Composable
fun IdeaApplication(palette: Palette, openCapture: Boolean, onCaptureDismiss: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { LocalSettings.get(context) }
    val repo = remember { (context.applicationContext as dev.zyutsu.idea.IdeaApp).repository }
    val nav: NavHostController = rememberNavController()

    IdeaTheme(palette = palette) {
        CompositionLocalProvider(LocalSettingsKey provides settings) {
            Box(Modifier.fillMaxSize()) {
                NavHost(
                    navController = nav,
                    startDestination = Routes.HOME,
                    modifier = Modifier.fillMaxSize(),
                    enterTransition = { fadeIn(tween(180)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, tween(240)) },
                    exitTransition = { fadeOut(tween(120)) },
                    popEnterTransition = { fadeIn(tween(160)) },
                    popExitTransition = { fadeOut(tween(140)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(240)) }
                ) {
                    composable(Routes.HOME) {
                        HomeRoute(
                            repo = repo,
                            onOpenNote = { nav.navigate(Routes.note(it)) },
                            onOpenGraph = { nav.navigate(Routes.GRAPH) },
                            onOpenSearch = { nav.navigate(Routes.SEARCH) },
                            onOpenTag = { nav.navigate(Routes.tag(it)) },
                            onOpenSettings = { nav.navigate(Routes.SETTINGS) }
                        )
                    }
                    composable(Routes.GRAPH) {
                        GraphRoute(repo = repo, onOpenNote = { nav.navigate(Routes.note(it)) }, onBack = { nav.popBackStack() })
                    }
                    composable(Routes.SEARCH) {
                        SearchRoute(repo = repo, onOpenNote = { nav.navigate(Routes.note(it)) }, onOpenTag = { nav.navigate(Routes.tag(it)) }, onBack = { nav.popBackStack() })
                    }
                    composable(Routes.SETTINGS) {
                        SettingsRoute(repo = repo, onBack = { nav.popBackStack() })
                    }
                    composable(Routes.NOTE) { entry ->
                        val id = entry.arguments?.getString("id").orEmpty()
                        EditorRoute(repo = repo, noteId = id, onBack = { nav.popBackStack() }, onOpenNote = { nav.navigate(Routes.note(it)) }, onOpenTag = { nav.navigate(Routes.tag(it)) })
                    }
                    composable(Routes.TAG) { entry ->
                        val name = android.net.Uri.decode(entry.arguments?.getString("name").orEmpty())
                        TagRoute(repo = repo, tag = name, onOpenNote = { nav.navigate(Routes.note(it)) }, onBack = { nav.popBackStack() })
                    }
                }
                if (openCapture) {
                    CaptureDialog(repo = repo, onDismiss = onCaptureDismiss) { id ->
                        onCaptureDismiss()
                        nav.navigate(Routes.note(id))
                    }
                }
            }
        }
    }
}

