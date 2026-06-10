package com.aurora.aonev3.ui.fragments.help.account

import androidx.lifecycle.ViewModel
import com.aurora.aonev3.network.handlers.CloudHandler

class AccountViewModel : ViewModel() {

    suspend fun resetPassword(username: String) = CloudHandler.resetPassword(username)
}