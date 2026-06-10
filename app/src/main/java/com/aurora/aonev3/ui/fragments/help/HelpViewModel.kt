package com.aurora.aonev3.ui.fragments.help

import androidx.lifecycle.ViewModel
import com.aurora.aonev3.network.handlers.CloudHandler

class HelpViewModel: ViewModel() {

    suspend fun getUser() = CloudHandler.getUser()
}