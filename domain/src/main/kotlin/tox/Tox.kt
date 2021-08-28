// SPDX-FileCopyrightText: 2019-2021 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.tox

import android.util.Log
import im.tox.tox4j.core.exceptions.ToxBootstrapException
import im.tox.tox4j.crypto.ToxCryptoConstants
import im.tox.tox4j.impl.jni.ToxCryptoImpl
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ltd.evilcorp.core.repository.ContactRepository
import ltd.evilcorp.core.repository.UserRepository
import ltd.evilcorp.core.vo.ConnectionStatus
import ltd.evilcorp.core.vo.Contact
import ltd.evilcorp.core.vo.FileKind
import ltd.evilcorp.core.vo.MessageType
import ltd.evilcorp.core.vo.UserStatus

private const val TAG = "Tox"

@Singleton
class Tox @Inject constructor(
    private val contactRepository: ContactRepository,
    private val userRepository: UserRepository,
    private val saveManager: SaveManager,
    private val nodeRegistry: BootstrapNodeRegistry,
) : CoroutineScope by GlobalScope {
    val toxId: ToxID get() = tox.getToxId()
    val publicKey: PublicKey by lazy { tox.getPublicKey() }
    var nospam: Int
        get() = tox.getNospam()
        set(value) = tox.setNospam(value)

    var started = false
    var isBootstrapNeeded = true

    private var running = false
    private var toxAvRunning = false

    private var passkey: ByteArray? = null
    var password: String? = null
        private set

    var udpEnabled: Boolean = false
        private set

    fun changePassword(new: String?) {
        passkey = if (new.isNullOrEmpty()) {
            null
        } else {
            val salt = ByteArray(ToxCryptoConstants.SaltLength())
            Random.Default.nextBytes(salt)
            ToxCryptoImpl.passKeyDeriveWithSalt(new.toByteArray(), salt)
        }
        password = new
        save()
    }

    private lateinit var tox: ToxWrapper

    fun start(saveOption: SaveOptions, password: String?, listener: ToxEventListener, avListener: ToxAvEventListener) {
        tox = if (password == null) {
            passkey = null
            ToxWrapper(listener, avListener, saveOption)
        } else {
            val salt = ToxCryptoImpl.getSalt(saveOption.saveData)
            passkey = ToxCryptoImpl.passKeyDeriveWithSalt(password.toByteArray(), salt)
            ToxWrapper(
                listener,
                avListener,
                saveOption.copy(saveData = ToxCryptoImpl.decrypt(saveOption.saveData, passkey)),
            )
        }

        this.password = password
        udpEnabled = saveOption.udpEnabled
        started = true

        fun loadContacts() = launch {
            contactRepository.resetTransientData()

            for ((publicKey, _) in tox.getContacts()) {
                if (!contactRepository.exists(publicKey.string())) {
                    contactRepository.add(Contact(publicKey.string()))
                }
            }
        }

        fun iterateForeverAv() = launch {
            toxAvRunning = true
            while (running) {
                tox.iterateAv()
                delay(tox.iterationIntervalAv())
            }
            toxAvRunning = false
        }

        fun iterateForever() = launch {
            running = true
            userRepository.updateConnection(publicKey.string(), ConnectionStatus.None)
            while (running || toxAvRunning) {
                if (isBootstrapNeeded) {
                    try {
                        bootstrap()
                        isBootstrapNeeded = false
                    } catch (e: ToxBootstrapException) {
                        Log.e(TAG, e.toString())
                    }
                }
                tox.iterate()
                delay(tox.iterationInterval())
            }
            started = false
        }

        save()
        loadContacts()
        iterateForever()
        iterateForeverAv()
    }

    fun stop() = launch {
        running = false
        while (started) delay(10)
        save().join()
        tox.stop()
        passkey = null
    }

    private val saveMutex = Mutex()
    private fun save() = launch {
        saveMutex.withLock {
            val passkey = passkey
            saveManager.save(
                publicKey,
                if (passkey == null) {
                    tox.getSaveData()
                } else {
                    ToxCryptoImpl.encrypt(tox.getSaveData(), passkey)
                }
            )
        }
    }

    fun acceptFriendRequest(publicKey: PublicKey) {
        tox.acceptFriendRequest(publicKey)
        save()
    }

    fun startFileTransfer(pk: PublicKey, fileNumber: Int) {
        Log.i(TAG, "Starting file transfer $fileNumber from ${pk.fingerprint()}")
        tox.startFileTransfer(pk, fileNumber)
    }

    fun stopFileTransfer(pk: PublicKey, fileNumber: Int) {
        Log.i(TAG, "Stopping file transfer $fileNumber from ${pk.fingerprint()}")
        tox.stopFileTransfer(pk, fileNumber)
    }

    fun sendFile(pk: PublicKey, fileKind: FileKind, fileSize: Long, fileName: String) =
        tox.sendFile(pk, fileKind, fileSize, fileName)

    fun sendFileChunk(pk: PublicKey, fileNo: Int, pos: Long, data: ByteArray) =
        tox.sendFileChunk(pk, fileNo, pos, data)

    fun getName() = tox.getName()
    fun setName(name: String) {
        tox.setName(name)
        save()
    }

    fun getStatusMessage() = tox.getStatusMessage()
    fun setStatusMessage(statusMessage: String) {
        tox.setStatusMessage(statusMessage)
        save()
    }

    fun addContact(toxId: ToxID, message: String) {
        tox.addContact(toxId, message)
        save()
    }

    fun deleteContact(publicKey: PublicKey) {
        tox.deleteContact(publicKey)
        save()
    }

    fun sendMessage(publicKey: PublicKey, message: String, type: MessageType) =
        tox.sendMessage(publicKey, message, type)

    fun getSaveData(): ByteArray {
        val passkey = passkey
        return if (passkey == null) {
            tox.getSaveData()
        } else {
            ToxCryptoImpl.encrypt(tox.getSaveData(), passkey)
        }
    }

    private fun bootstrap() {
        nodeRegistry.get(4).forEach { node ->
            Log.i(TAG, "Bootstrapping from $node")
            tox.bootstrap(node.address, node.port, node.publicKey.bytes())
        }
    }

    fun setTyping(publicKey: PublicKey, typing: Boolean) = tox.setTyping(publicKey, typing)

    fun getStatus() = tox.getStatus()
    fun setStatus(status: UserStatus) {
        tox.setStatus(status)
        save()
    }

    fun sendLosslessPacket(pk: PublicKey, packet: ByteArray) = tox.sendLosslessPacket(pk, packet)

    // ToxAv, probably move these.
    fun startCall(pk: PublicKey) = tox.startCall(pk)
    fun answerCall(pk: PublicKey) = tox.answerCall(pk)
    fun endCall(pk: PublicKey) = tox.endCall(pk)
    fun sendAudio(pk: PublicKey, pcm: ShortArray, channels: Int, samplingRate: Int) =
        tox.sendAudio(pk, pcm, channels, samplingRate)
}
