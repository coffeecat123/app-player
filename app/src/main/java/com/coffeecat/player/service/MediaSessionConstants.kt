package com.coffeecat.player.service

import android.os.Bundle
import androidx.media3.session.SessionCommand

object MediaSessionConstants {
    const val ACTION_MUSIC = "com.coffeecat.player.ACTION_MUSIC"
    const val ACTION_PREVIOUS = "com.coffeecat.player.ACTION_PREVIOUS"
    const val ACTION_NEXT = "com.coffeecat.player.ACTION_NEXT"
    const val ACTION_REPEAT = "com.coffeecat.player.ACTION_TYPE"

    val CommandMusic = SessionCommand(ACTION_MUSIC, Bundle.EMPTY)
    val CommandPrevious = SessionCommand(ACTION_PREVIOUS, Bundle.EMPTY)
    val CommandNext = SessionCommand(ACTION_NEXT, Bundle.EMPTY)
    val CommandRepeat = SessionCommand(ACTION_REPEAT, Bundle.EMPTY)
}