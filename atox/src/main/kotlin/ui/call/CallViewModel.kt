// SPDX-FileCopyrightText: 2021-2022 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.call

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ltd.evilcorp.atox.ui.NotificationHelper
import ltd.evilcorp.core.vo.Contact
import ltd.evilcorp.domain.feature.CallManager
import ltd.evilcorp.domain.feature.CallState
import ltd.evilcorp.domain.feature.ContactManager
import ltd.evilcorp.domain.tox.PublicKey

class CallViewModel @Inject constructor(
    private val scope: CoroutineScope,
    private val callManager: CallManager,
    private val notificationHelper: NotificationHelper,
    private val contactManager: ContactManager,
) : ViewModel() {
    private var publicKey = PublicKey("")

    val contact: LiveData<Contact> by lazy {
        contactManager.get(publicKey).asLiveData()
    }

    fun setActiveContact(pk: PublicKey) {
        publicKey = pk
    }

    fun startCall() {
        callManager.startCall(publicKey)
        when (callManager.inCall.value) {
            is CallState.InCall -> scope.launch {
                notificationHelper.showOngoingCallNotification(contactManager.get(publicKey).first())
            }
            is CallState.WaitingForContact -> scope.launch {
                notificationHelper.showWaitingForContactCallNotification(contactManager.get(publicKey).first())
            }
            else -> {}
        }
    }

    fun endCall() = scope.launch {
        callManager.endCall(publicKey)
        notificationHelper.dismissCallNotification(publicKey)
    }

    fun enableAudio() = callManager.enableAudio()
    fun disableAudio() = callManager.disableAudio()

    val inCall = callManager.inCall
    val audioEnabled = callManager.audioEnabled

    var speakerphoneOn by callManager::speakerphoneOn
}
