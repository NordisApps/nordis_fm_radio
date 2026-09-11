package com.nordisapps.fmradio

object RadioServiceEvents {
    var onStoppedExternally: (() -> Unit)? = null
}