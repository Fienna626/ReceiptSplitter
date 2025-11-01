package com.example.receiptsplitter

// In NavRoutes.kt
object NavRoutes {
    const val HOME_SCREEN = "home"
    const val SETUP_SCREEN = "setup"
    const val BILL_SPLITTER_SCREEN = "bill_splitter"
    const val TIP_SCREEN = "tip"

    // --- UPDATE THIS SECTION ---
    private const val SUMMARY_ROUTE = "summary"
    const val SUMMARY_ARG_VIEW_ONLY = "isViewOnly"
    const val SUMMARY_SCREEN = "$SUMMARY_ROUTE/{$SUMMARY_ARG_VIEW_ONLY}"

    // Helper function to build the route
    fun buildSummaryRoute(isViewOnly: Boolean): String {
        return "$SUMMARY_ROUTE/$isViewOnly"
    }
}