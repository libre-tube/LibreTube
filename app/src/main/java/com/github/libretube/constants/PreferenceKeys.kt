package com.github.libretube.constants

/**
 * keys for the shared preferences
 */
object PreferenceKeys {
    /**
     * Authentication
     */
    const val AUTH_PREF_FILE = "auth"
    const val TOKEN = "token"
    const val USERNAME = "username"
    const val IMAGE_PROXY_URL = "image_proxy_url"

    /**
     * General
     */
    const val LANGUAGE = "language"
    const val REGION = "region"
    const val AUTO_ROTATION = "auto_rotation"
    const val SLEEP_TIMER = "sleep_timer_toggle"
    const val SLEEP_TIMER_DELAY = "sleep_timer_delay"
    const val SAVE_FEED = "save_feed"
    const val NAVBAR_ITEMS = "navbar_items"
    const val START_FRAGMENT = "start_fragment"

    /**
     * Appearance
     */
    const val THEME_MODE = "theme_toggle"
    const val PURE_THEME = "pure_theme"
    const val ACCENT_COLOR = "accent_color"
    const val GRID_COLUMNS = "grid"
    const val LABEL_VISIBILITY = "label_visibility"
    const val APP_ICON = "icon_change"
    const val LEGACY_SUBSCRIPTIONS = "legacy_subscriptions"
    const val LEGACY_SUBSCRIPTIONS_COLUMNS = "legacy_subscriptions_columns"
    const val ALTERNATIVE_VIDEOS_LAYOUT = "alternative_videos_layout"
    const val NEW_VIDEOS_BADGE = "new_videos_badge"
    const val PLAYLISTS_ORDER = "playlists_order"
    const val PLAYLIST_SORT_ORDER = "playlist_sort_order"
    const val HOME_TAB_CONTENT = "home_tab_content"

    /**
     * Instance
     */
    const val FETCH_INSTANCE = "selectInstance"
    const val AUTH_INSTANCE = "selectAuthInstance"
    const val AUTH_INSTANCE_TOGGLE = "auth_instance_toggle"
    const val CUSTOM_INSTANCE = "customInstance"
    const val CLEAR_CUSTOM_INSTANCES = "clearCustomInstances"
    const val LOGIN_REGISTER = "login_register"
    const val LOGOUT = "logout"
    const val DELETE_ACCOUNT = "delete_account"

    /**
     * Player
     */
    const val AUTO_FULLSCREEN = "auto_fullscreen"
    const val AUTO_PLAY = "autoplay"
    const val RELATED_STREAMS = "related_streams_toggle"
    const val CUSTOM_PLAYBACK_SPEED = "custom_playback_speed"
    const val PLAYBACK_SPEED = "playback_speed"
    const val BACKGROUND_PLAYBACK_SPEED = "background_playback_speed"
    const val FULLSCREEN_ORIENTATION = "fullscreen_orientation"
    const val PAUSE_ON_SCREEN_OFF = "pause_screen_off"
    const val WATCH_POSITIONS = "watch_positions"
    const val WATCH_HISTORY_TOGGLE = "watch_history_toggle"
    const val SEARCH_HISTORY_TOGGLE = "search_history_toggle"
    const val SYSTEM_CAPTION_STYLE = "system_caption_style"
    const val CAPTION_SETTINGS = "caption_settings"
    const val SEEK_INCREMENT = "seek_increment"
    const val DEFAULT_RESOLUTION = "default_res"
    const val DEFAULT_RESOLUTION_MOBILE = "default_res_mobile"
    const val BUFFERING_GOAL = "buffering_goal"
    const val PLAYER_AUDIO_FORMAT = "player_audio_format"
    const val PLAYER_AUDIO_QUALITY = "player_audio_quality"
    const val PLAYER_AUDIO_QUALITY_MOBILE = "player_audio_quality_mobile"
    const val DEFAULT_SUBTITLE = "default_subtitle"
    const val SKIP_BUTTONS = "skip_buttons"
    const val PICTURE_IN_PICTURE = "picture_in_picture"
    const val PLAYER_RESIZE_MODE = "player_resize_mode"
    const val SB_SKIP_MANUALLY = "sb_skip_manually_key"
    const val SB_SHOW_MARKERS = "sb_show_markers"
    const val ALTERNATIVE_PLAYER_LAYOUT = "alternative_player_layout"
    const val USE_HLS_OVER_DASH = "use_hls"
    const val QUEUE_AUTO_INSERT_RELATED = "queue_insert_related_videos"
    const val PLAYER_SWIPE_CONTROLS = "player_swipe_controls"
    const val PLAYER_PINCH_CONTROL = "player_pinch_control"
    const val CAPTIONS_SIZE = "captions_size"
    const val DOUBLE_TAP_TO_SEEK = "double_tap_seek"
    const val PAUSE_ON_QUIT = "pause_on_quit"
    const val ALTERNATIVE_PIP_CONTROLS = "alternative_pip_controls"
    const val SKIP_SILENCE = "skip_silence"
    const val ENABLED_VIDEO_CODECS = "video_codecs"
    const val AUTOPLAY_COUNTDOWN = "autoplay_countdown"
    const val LBRY_HLS = "lbry_hls"
    const val AUTO_FULLSCREEN_SHORTS = "auto_fullscreen_shorts"
    const val PLAY_AUTOMATICALLY = "play_automatically"
    const val FULLSCREEN_GESTURES = "fullscreen_gestures"
    const val UNLIMITED_SEARCH_HISTORY = "unlimited_search_history"

    /**
     * Background mode
     */
    const val AUDIO_ONLY_MODE = "audio_only_mode"

    /**
     * Notifications
     */
    const val NOTIFICATION_ENABLED = "notification_toggle"
    const val SHOW_STREAM_THUMBNAILS = "show_stream_thumbnails"
    const val SHORTS_NOTIFICATIONS = "shorts_notifications"
    const val CHECKING_FREQUENCY = "checking_frequency"
    const val REQUIRED_NETWORK = "required_network"
    const val IGNORED_NOTIFICATION_CHANNELS = "ignored_notification_channels"
    const val NOTIFICATION_TIME_ENABLED = "notification_time"
    const val NOTIFICATION_START_TIME = "notification_start_time"
    const val NOTIFICATION_END_TIME = "notification_end_time"

    /**
     * Subscriptions
     */
    const val LAST_STREAM_VIDEO_ID = "last_stream_video_id"
    const val LAST_WATCHED_FEED_TIME = "last_watched_feed_time"
    const val HIDE_WATCHED_FROM_FEED = "hide_watched_from_feed"
    const val SELECTED_FEED_FILTER = "filer_feed"
    const val FEED_SORT_ORDER = "sort_oder_feed"

    /**
     * Advanced
     */
    const val DATA_SAVER_MODE = "data_saver_mode_key"
    const val MAX_IMAGE_CACHE = "image_cache_size"
    const val RESET_SETTINGS = "reset_settings"
    const val CLEAR_SEARCH_HISTORY = "clear_search_history"
    const val CLEAR_WATCH_HISTORY = "clear_watch_history"
    const val CLEAR_WATCH_POSITIONS = "clear_watch_positions"
    const val SHARE_WITH_TIME_CODE = "share_with_time_code"
    const val CONFIRM_UNSUBSCRIBE = "confirm_unsubscribing"
    const val CLEAR_BOOKMARKS = "clear_bookmarks"
    const val MAX_CONCURRENT_DOWNLOADS = "max_concurrent_downloads"
    const val DISABLE_VIDEO_IMAGE_PROXY = "disable_video_image_proxy"

    /**
     * History
     */
    const val WATCH_HISTORY_SIZE = "watch_history_size"

    /**
     * Error logs
     */
    const val ERROR_LOG = "error_log"
}
