package com.yourname.womensafety.ui.tour

enum class BubbleAlign { TOP, BOTTOM, CENTER }
enum class SpotlightShape { CIRCLE, ROUNDED_RECT, FULL_WIDTH_BAR, NONE }

data class TourStep(
    val id: Int,
    val screenRoute: String,
    val targetTag: String,
    val emoji: String,
    val title: String,
    val body: String,
    val bubbleAlign: BubbleAlign = BubbleAlign.BOTTOM,
    val spotlightShape: SpotlightShape = SpotlightShape.ROUNDED_RECT,
    val navigateTo: String? = null,
)

/**
 * Master tour step list — 11 stops total.
 *
 * REMOVED from tour (pages/features remain fully intact in the app):
 *  - Safety Statistics page
 *  - Go Premium / Upgrade banner
 *  - Help & Support page
 *  - About Asfalis page
 */
val TOUR_STEPS: List<TourStep> = listOf(

    // 0 — Welcome splash (no spotlight)
    TourStep(
        id = 0, screenRoute = "dashboard", targetTag = "",
        emoji = "🛡️",
        title = "Welcome to Asfalis",
        body = "Your personal safety guardian. Quick tour in 30 seconds.",
        bubbleAlign = BubbleAlign.CENTER,
        spotlightShape = SpotlightShape.NONE
    ),

    // 1 — Dashboard overview (no spotlight)
    TourStep(
        id = 1, screenRoute = "dashboard", targetTag = "",
        emoji = "🏠",
        title = "Your Dashboard",
        body = "View protection status and emergency actions.",
        bubbleAlign = BubbleAlign.CENTER,
        spotlightShape = SpotlightShape.NONE
    ),

    // 2 — Protection Shield Button
    TourStep(
        id = 2, screenRoute = "dashboard", targetTag = "tour_protection_btn",
        emoji = "⚡",
        title = "Protection",
        body = "Tap to arm or disarm your safety shield.",
        bubbleAlign = BubbleAlign.BOTTOM,
        spotlightShape = SpotlightShape.CIRCLE
    ),

    // 3 — Manual SOS Trigger
    TourStep(
        id = 3, screenRoute = "dashboard", targetTag = "tour_sos_btn",
        emoji = "🆘",
        title = "Emergency SOS",
        body = "Send instant SOS alerts with live location.",
        bubbleAlign = BubbleAlign.TOP,
        spotlightShape = SpotlightShape.ROUNDED_RECT
    ),

    // 4 — Bluetooth Wearable Card
    TourStep(
        id = 4, screenRoute = "dashboard", targetTag = "tour_ble_card",
        emoji = "📡",
        title = "Safety Bracelet",
        body = "Connect your ESP32 wearable button.",
        bubbleAlign = BubbleAlign.TOP,
        spotlightShape = SpotlightShape.ROUNDED_RECT
    ),

    // 5 — Trusted Contacts FAB
    TourStep(
        id = 5, screenRoute = "contacts", targetTag = "tour_contacts_fab",
        emoji = "👥",
        title = "Add Contact",
        body = "Add someone you trust for emergencies.",
        bubbleAlign = BubbleAlign.TOP,
        spotlightShape = SpotlightShape.CIRCLE,
        navigateTo = "contacts"
    ),

    // 6 — Auto SOS Toggle
    TourStep(
        id = 6, screenRoute = "settings", targetTag = "tour_auto_sos_card",
        emoji = "🤖",
        title = "Auto SOS",
        body = "ML detects danger and alerts automatically.",
        bubbleAlign = BubbleAlign.BOTTOM,
        spotlightShape = SpotlightShape.ROUNDED_RECT,
        navigateTo = "settings"
    ),

    // 7 — Shake Sensitivity
    TourStep(
        id = 7, screenRoute = "settings", targetTag = "tour_sensitivity_card",
        emoji = "📳",
        title = "Sensitivity",
        body = "Adjust how easily Auto SOS activates.",
        bubbleAlign = BubbleAlign.TOP,
        spotlightShape = SpotlightShape.ROUNDED_RECT
    ),

    // 8 — SOS History Filters
    TourStep(
        id = 8, screenRoute = "sos_history", targetTag = "tour_history_filters",
        emoji = "📋",
        title = "Alert History",
        body = "Filter and review all past SOS events.",
        bubbleAlign = BubbleAlign.BOTTOM,
        spotlightShape = SpotlightShape.FULL_WIDTH_BAR,
        navigateTo = "sos_history"
    ),

    // 9 — Account Security (was 10)
    TourStep(
        id = 9, screenRoute = "account_security", targetTag = "tour_app_lock_card",
        emoji = "🔐",
        title = "App Lock",
        body = "Secure with biometrics or PIN.",
        bubbleAlign = BubbleAlign.BOTTOM,
        spotlightShape = SpotlightShape.ROUNDED_RECT,
        navigateTo = "account_security"
    ),

    // 10 — Celebration (no spotlight, was 14)
    TourStep(
        id = 10, screenRoute = "", targetTag = "",
        emoji = "🎉",
        title = "You're All Set!",
        body = "Stay safe. Replay this tour from Help anytime. 💖",
        bubbleAlign = BubbleAlign.CENTER,
        spotlightShape = SpotlightShape.NONE
    ),
)
