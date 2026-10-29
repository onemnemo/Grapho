package com.onemnemo.grapho.note

import androidx.annotation.StringRes
import java.io.IOException

/** An [IOException] whose text is shown to the person: a string resource and the arguments it formats. */
class UserIOException(@StringRes val messageRes: Int, val args: List<Any> = emptyList(), cause: Throwable? = null) :
    IOException("user-facing failure $messageRes", cause)
