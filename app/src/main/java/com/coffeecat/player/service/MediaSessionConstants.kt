package com.coffeecat.animeplayer.service

import android.os.Bundle
import androidx.media3.session.SessionCommand

object MediaSessionConstants {
    const val ACTION_MUSIC = "com.coffeecat.animeplayer.ACTION_MUSIC"
    const val ACTION_PREVIOUS = "com.coffeecat.animeplayer.ACTION_PREVIOUS"
    const val ACTION_NEXT = "com.coffeecat.animeplayer.ACTION_NEXT"
    const val ACTION_STOP = "com.coffeecat.animeplayer.ACTION_STOP"

    val CommandMusic = SessionCommand(ACTION_MUSIC, Bundle.EMPTY)
    val CommandPrevious = SessionCommand(ACTION_PREVIOUS, Bundle.EMPTY)
    val CommandNext = SessionCommand(ACTION_NEXT, Bundle.EMPTY)
    val CommandStop = SessionCommand(ACTION_STOP, Bundle.EMPTY)
}