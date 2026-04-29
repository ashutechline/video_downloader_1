package com.example.video_downloder.presentation.navigation

sealed class Screen(val route: String) {
    object NotesScreen : Screen("notes")
    object AddEditNoteScreen : Screen("add_edit_note")
    object HomeScreen : Screen("home")
    object DeviceVideosScreen : Screen("device_videos")
    
    object VideoPlayerScreen : Screen("video_player?uri={uri}") {
        fun createRoute(uri: String) = "video_player?uri=$uri"
    }

    object StatusSaverScreen : Screen("status_saver")
    object DirectChatScreen : Screen("direct_chat")
    object StatusPreviewScreen : Screen("preview_screen?statusId={statusId}") {
        fun createRoute(statusId: String) = "preview_screen?statusId=$statusId"
    }
    
    object DownloadLinkScreen : Screen("from_link")
    object DownloadManagerScreen : Screen("downloads")
    object CollectionScreen : Screen("collection")
    object SettingsScreen : Screen("settings")
    object SplashScreen : Screen("splash")
    object LockScreen : Screen("lock_screen")
    object LockedVideosScreen : Screen("locked_videos")
    object HiddenVideosScreen : Screen("hidden_videos")
    object LanguageSelectionScreen : Screen("language_selection")
    
    object AlbumListScreen : Screen("album_list/{title}") {
        fun createRoute(title: String) = "album_list/$title"
    }
    
    object AlbumVideosScreen : Screen("album_videos/{albumId}/{albumName}") {
        fun createRoute(albumId: String, albumName: String) = "album_videos/$albumId/$albumName"
    }
    
    object CollectionAlbumVideosScreen : Screen("collection_album/{albumId}") {
        fun createRoute(albumId: String) = "collection_album/$albumId"
    }
}



