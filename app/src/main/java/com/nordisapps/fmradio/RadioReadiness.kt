package com.nordisapps.fmradio

sealed class RadioReadiness {
    object Ready : RadioReadiness()
    object ServiceUnavailable : RadioReadiness()
    object HeadsetRequired : RadioReadiness()
}