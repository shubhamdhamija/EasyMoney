package com.invest.easymoney.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.dp

/**
 * Premium shape system for EasyMoney.
 *
 * Design goals:
 * - Softer, more premium corners for cards and bottom sheets
 * - Consistent rounded language across chips, dialogs, lists, and chart containers
 * - Slightly larger defaults than Material 3 standard for a modern trading-app look
 */

val EasyMoneyShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

/**
 * Optional semantic shape tokens for feature-specific usage.
 *
 * These are useful when you want very intentional UI patterns:
 * - Hero cards
 * - Stat chips
 * - Search bars
 * - Bottom sheets
 * - Dialogs
 * - Chart containers
 */
@Immutable
object TradingShapes {
    /** Small inline elements like tags, compact chips, tiny badges */
    val Pill = RoundedCornerShape(999.dp)

    /** Input fields, compact cards, small sheets */
    val Input = RoundedCornerShape(18.dp)

    /** Standard list rows and medium cards */
    val Card = RoundedCornerShape(22.dp)

    /** Hero sections, featured surfaces, large tiles */
    val HeroCard = RoundedCornerShape(28.dp)

    /** Chart containers and premium dashboard panels */
    val ChartContainer = RoundedCornerShape(30.dp)

    /** Modal bottom sheets / large dialogs */
    val BottomSheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

    /** Dialogs with softer premium appearance */
    val Dialog = RoundedCornerShape(24.dp)

    /** Small stat tiles and compact summary cards */
    val StatTile = RoundedCornerShape(18.dp)

    /** Watchlist / recommendation carousel item */
    val CarouselCard = RoundedCornerShape(26.dp)

    /** Navigation rail / segmented backgrounds if needed later */
    val Panel = RoundedCornerShape(20.dp)
}
