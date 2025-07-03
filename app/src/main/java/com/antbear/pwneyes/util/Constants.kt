package com.antbear.pwneyes.util

/**
 * Centralized constants for the PwnEyes application.
 * This object contains all magic numbers, strings, and configuration values
 * used throughout the application to improve maintainability and reduce errors.
 */
object Constants {
    
    // ========== BILLING CONSTANTS ==========
    object Billing {
        const val REMOVE_ADS_PRODUCT_ID = "remove_ads"
        const val MAX_RETRY_ATTEMPTS = 5
        const val CONNECTION_TIMEOUT_MS = 15000L
        const val RETRY_DELAY_BASE_MS = 3000L
        const val PERIODIC_RECONNECTION_INTERVAL_MS = 120000L // 2 minutes
        const val RECONNECT_DELAY_MS = 5000L
        
        // Billing connection states
        const val STATE_DISCONNECTED = 0
        const val STATE_CONNECTING = 1
        const val STATE_CONNECTED = 2
        const val STATE_ERROR = 3
    }
    
    // ========== NETWORK CONSTANTS ==========
    object Network {
        const val DEFAULT_TIMEOUT_MS = 30000L
        const val RETRY_DELAY_MS = 2000L
        const val MAX_RETRY_ATTEMPTS = 3
        const val CONNECTION_CHECK_INTERVAL_MS = 5000L
    }
    
    // ========== WEBVIEW CONSTANTS ==========
    object WebView {
        const val LOADING_TIMEOUT_MS = 30000L
        const val RETRY_DELAY_MS = 2000L
        const val MAX_RETRIES = 2
        const val DEFAULT_TEXT_ENCODING = "UTF-8"
        const val USER_AGENT_SUFFIX = "PwnEyes"
    }
    
    // ========== DATABASE CONSTANTS ==========
    object Database {
        const val DATABASE_NAME = "pwneyes_database"
        const val DATABASE_VERSION = 1
        const val BACKUP_INTERVAL_HOURS = 24
        const val MAX_BACKUP_FILES = 5
    }
    
    // ========== ACCESSIBILITY CONSTANTS ==========
    object Accessibility {
        const val FONT_SCALE_SMALL = 0.85f
        const val FONT_SCALE_NORMAL = 1.0f
        const val FONT_SCALE_LARGE = 1.25f
        const val FONT_SCALE_EXTRA_LARGE = 1.5f
        const val TOUCH_TARGET_NORMAL = 1.0f
        const val TOUCH_TARGET_LARGE = 1.25f
        const val MIN_TOUCH_TARGET_SIZE_DP = 48
        const val MIN_PADDING_DP = 16
        const val ADDITIONAL_PADDING_DP = 4
    }
    
    // ========== UI CONSTANTS ==========
    object UI {
        const val ANIMATION_DURATION_SHORT_MS = 200L
        const val ANIMATION_DURATION_MEDIUM_MS = 300L
        const val ANIMATION_DURATION_LONG_MS = 500L
        const val DEBOUNCE_DELAY_MS = 300L
        const val SPLASH_SCREEN_DURATION_MS = 2000L
    }
    
    // ========== PREFERENCES KEYS ==========
    object PreferenceKeys {
        const val THEME_PREFERENCE = "theme_preference"
        const val FONT_SIZE_PREFERENCE = "font_size_preference"
        const val HIGH_CONTRAST_MODE = "high_contrast_mode"
        const val TEXT_TO_SPEECH = "text_to_speech"
        const val LARGER_TOUCH_TARGETS = "larger_touch_targets"
        const val SCREEN_READER_SUPPORT = "screen_reader_support"
        const val REDUCE_ANIMATIONS = "reduce_animations"
        const val SHOW_BLUETOOTH_CHECK = "show_bluetooth_tethering_check"
        const val NIGHT_MODE = "night_mode"
        const val PREVIOUS_VERSION_CODE = "previous_version_code"
        const val FIRST_RUN = "first_run"
        const val LAST_BACKUP_TIME = "last_backup_time"
    }
    
    // ========== INTENT ACTIONS ==========
    object IntentActions {
        const val ACTION_VIEW_CONNECTION = "com.antbear.pwneyes.VIEW_CONNECTION"
        const val ACTION_ADD_CONNECTION = "com.antbear.pwneyes.ADD_CONNECTION"
        const val ACTION_SETTINGS = "com.antbear.pwneyes.SETTINGS"
    }
    
    // ========== EXTERNAL URLS ==========
    object ExternalUrls {
        const val BUY_ME_COFFEE = "https://buymeacoffee.com/ltldrk"
        const val SUPPORT_EMAIL = "PwnEyes@proton.me"
        const val PRIVACY_POLICY = "https://github.com/dpogreba/pwneyes/blob/main/PRIVACY.md"
        const val GITHUB_REPO = "https://github.com/dpogreba/pwneyes"
        const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=com.antbear.pwneyes"
    }
    
    // ========== FILE PATHS ==========
    object FilePaths {
        const val SCHEMAS_DIR = "schemas"
        const val BACKUPS_DIR = "backups"
        const val LOGS_DIR = "logs"
        const val CACHE_DIR = "cache"
        const val TEMP_DIR = "temp"
    }
    
    // ========== LOGGING CONSTANTS ==========
    object Logging {
        const val MAX_LOG_FILE_SIZE_MB = 10
        const val MAX_LOG_FILES = 5
        const val LOG_RETENTION_DAYS = 7
    }
    
    // ========== SECURITY CONSTANTS ==========
    object Security {
        const val ENCRYPTION_KEY_ALIAS = "pwneyes_master_key"
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val ENCRYPTION_ALGORITHM = "AES/GCM/NoPadding"
        const val KEY_SIZE = 256
    }
    
    // ========== CONNECTION HEALTH ==========
    object ConnectionHealth {
        const val HEALTH_CHECK_INTERVAL_MS = 30000L // 30 seconds
        const val CONNECTION_TIMEOUT_MS = 10000L
        const val MAX_FAILED_ATTEMPTS = 3
        const val RETRY_BACKOFF_MULTIPLIER = 2.0
        const val MAX_RETRY_DELAY_MS = 60000L // 1 minute
    }
    
    // ========== NOTIFICATION CONSTANTS ==========
    object Notifications {
        const val CHANNEL_ID_GENERAL = "pwneyes_general"
        const val CHANNEL_ID_ERRORS = "pwneyes_errors"
        const val CHANNEL_ID_UPDATES = "pwneyes_updates"
        const val NOTIFICATION_ID_CONNECTION_ERROR = 1001
        const val NOTIFICATION_ID_UPDATE_AVAILABLE = 1002
    }
    
    // ========== THEME VALUES ==========
    object ThemeValues {
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val THEME_SYSTEM = "system"
    }
    
    // ========== FONT SIZE VALUES ==========
    object FontSizeValues {
        const val FONT_SIZE_SMALL = "small"
        const val FONT_SIZE_NORMAL = "normal"
        const val FONT_SIZE_LARGE = "large"
        const val FONT_SIZE_EXTRA_LARGE = "extra_large"
    }
    
    // ========== ERROR CODES ==========
    object ErrorCodes {
        const val NETWORK_ERROR = 1000
        const val DATABASE_ERROR = 2000
        const val BILLING_ERROR = 3000
        const val WEBVIEW_ERROR = 4000
        const val PERMISSION_ERROR = 5000
        const val UNKNOWN_ERROR = 9999
    }
    
    // ========== VALIDATION CONSTANTS ==========
    object Validation {
        const val MIN_CONNECTION_NAME_LENGTH = 1
        const val MAX_CONNECTION_NAME_LENGTH = 50
        const val MIN_URL_LENGTH = 7 // http://
        const val MAX_URL_LENGTH = 2048
        const val MIN_USERNAME_LENGTH = 1
        const val MAX_USERNAME_LENGTH = 100
        const val MIN_PASSWORD_LENGTH = 1
        const val MAX_PASSWORD_LENGTH = 100
    }
}
