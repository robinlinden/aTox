// SPDX-FileCopyrightText: 2019-2024 Robin Lindén <dev@robinlinden.eu>
//
// SPDX-License-Identifier: GPL-3.0-only

@file:OptIn(ExperimentalStdlibApi::class)

package ltd.evilcorp.domain.tox

import android.util.Log
import im.tox.tox4j.av.ToxAv
import im.tox.tox4j.av.data.AudioChannels
import im.tox.tox4j.av.data.BitRate
import im.tox.tox4j.av.data.SampleCount
import im.tox.tox4j.av.data.SamplingRate
import im.tox.tox4j.av.enums.ToxavCallControl
import im.tox.tox4j.core.ToxCore
import im.tox.tox4j.core.data.Port
import im.tox.tox4j.core.data.ToxFileId
import im.tox.tox4j.core.data.ToxFilename
import im.tox.tox4j.core.data.ToxFriendAddress
import im.tox.tox4j.core.data.ToxFriendMessage
import im.tox.tox4j.core.data.ToxFriendNumber
import im.tox.tox4j.core.data.ToxFriendRequestMessage
import im.tox.tox4j.core.data.ToxLosslessPacket
import im.tox.tox4j.core.data.ToxNickname
import im.tox.tox4j.core.data.ToxPublicKey
import im.tox.tox4j.core.data.ToxStatusMessage
import im.tox.tox4j.core.enums.ToxFileControl
import im.tox.tox4j.core.exceptions.ToxFileControlException
import im.tox.tox4j.core.exceptions.ToxFileSendChunkException
import im.tox.tox4j.core.exceptions.ToxFriendAddException
import im.tox.tox4j.core.exceptions.ToxFriendCustomPacketException
import im.tox.tox4j.impl.jni.ToxAvImpl
import im.tox.tox4j.impl.jni.ToxCoreImpl
import kotlin.random.Random
import ltd.evilcorp.core.vo.FileKind
import ltd.evilcorp.core.vo.MessageType
import ltd.evilcorp.core.vo.UserStatus

private const val TAG = "ToxWrapper"

// TODO(robinlinden) Make configurable.
// https://wiki.xiph.org/Opus_Recommended_Settings
// 32 should be good enough for fullband stereo.
private val AUDIO_BIT_RATE = BitRate(32)

enum class CustomPacketError {
    Success,
    Empty,
    FriendNotConnected,
    FriendNotFound,
    Invalid,
    Null,
    Sendq,
    TooLong,
}

class ToxWrapper(
    private val eventListener: ToxEventListener,
    private val avEventListener: ToxAvEventListener,
    options: SaveOptions,
) {
    private val tox: ToxCore = ToxCoreImpl(
        options.toToxOptions().also {
            Log.i(TAG, "Starting Tox with options $it")
        },
    )
    private val av: ToxAv = ToxAvImpl(tox as ToxCoreImpl)

    init {
        updateContactMapping()
    }

    private fun updateContactMapping() {
        val contacts = getContacts()
        eventListener.contactMapping = contacts
        avEventListener.contactMapping = contacts
    }

    fun bootstrap(address: String, port: Int, publicKey: ByteArray) {
        tox.bootstrap(address, Port(port.toUShort()), ToxPublicKey(publicKey))
        tox.addTcpRelay(address, Port(port.toUShort()), ToxPublicKey(publicKey))
    }

    fun stop() {
        av.close()
        tox.close()
    }

    fun iterate(): Unit = tox.iterate(eventListener, Unit)
    fun iterateAv(): Unit = av.iterate(avEventListener, Unit)
    fun iterationInterval(): Long = tox.iterationInterval.toLong()
    fun iterationIntervalAv(): Long = av.iterationInterval.toLong()

    fun getName(): String = String(tox.getName.value)
    fun setName(name: String) {
        tox.setName(ToxNickname(name.toByteArray()))
    }

    fun getStatusMessage(): String = String(tox.getStatusMessage.value)
    fun setStatusMessage(statusMessage: String) {
        tox.setStatusMessage(ToxStatusMessage(statusMessage.toByteArray()))
    }

    fun getToxId() = ToxID.fromBytes(tox.getAddress.value)
    fun getPublicKey() = PublicKey.fromBytes(tox.getPublicKey.value)
    fun getNospam(): Int = tox.getNospam
    fun setNospam(value: Int) {
        tox.setNospam(value)
    }

    fun getSaveData() = tox.getSavedata

    fun addContact(toxId: ToxID, message: String) {
        tox.addFriend(ToxFriendAddress(toxId.bytes()), ToxFriendRequestMessage(message.toByteArray()))
        updateContactMapping()
    }

    fun deleteContact(pk: PublicKey) {
        Log.i(TAG, "Deleting ${pk.fingerprint()}")
        tox.getFriendNumbers.find { PublicKey.fromBytes(tox.getFriendPublicKey(it).value) == pk }?.let { friend ->
            tox.deleteFriend(friend)
        } ?: Log.e(
            TAG,
            "Tried to delete nonexistent contact, this can happen if the database is out of sync with the Tox save",
        )

        updateContactMapping()
    }

    fun getContacts(): List<Pair<PublicKey, Int>> {
        val friendNumbers = tox.getFriendNumbers
        Log.i(TAG, "Loading ${friendNumbers.size} friends")
        return List(friendNumbers.size) {
            Pair(PublicKey.fromBytes(tox.getFriendPublicKey(friendNumbers[it]).value), friendNumbers[it].value)
        }
    }

    fun sendMessage(publicKey: PublicKey, message: String, type: MessageType): Int = tox.friendSendMessage(
        contactByKey(publicKey),
        type.toToxType(),
        ToxFriendMessage(message.toByteArray()),
    ).value

    fun acceptFriendRequest(pk: PublicKey) = try {
        tox.addFriendNorequest(ToxPublicKey(pk.bytes()))
        updateContactMapping()
    } catch (e: ToxFriendAddException) {
        Log.e(TAG, "Exception while accepting friend request $pk: $e")
    }

    fun startFileTransfer(pk: PublicKey, fileNumber: Int) = try {
        tox.fileControl(contactByKey(pk), fileNumber, ToxFileControl.RESUME)
    } catch (e: ToxFileControlException) {
        Log.e(TAG, "Error starting ft ${pk.fingerprint()} $fileNumber\n$e")
    }

    fun stopFileTransfer(pk: PublicKey, fileNumber: Int) = try {
        tox.fileControl(contactByKey(pk), fileNumber, ToxFileControl.CANCEL)
    } catch (e: ToxFileControlException) {
        Log.e(TAG, "Error stopping ft ${pk.fingerprint()} $fileNumber\n$e")
    }

    fun sendFile(pk: PublicKey, fileKind: FileKind, fileSize: Long, fileName: String) = try {
        tox.fileSend(
            contactByKey(pk),
            fileKind.toToxtype(),
            fileSize,
            ToxFileId(Random.nextBytes(32)),
            ToxFilename(fileName.toByteArray()),
        )
    } catch (e: ToxFileControlException) {
        Log.e(TAG, "Error sending ft $fileName ${pk.fingerprint()}\n$e")
    }

    fun sendFileChunk(pk: PublicKey, fileNo: Int, pos: Long, data: ByteArray): Result<Unit> = try {
        tox.fileSendChunk(contactByKey(pk), fileNo, pos, data)
        Result.success(Unit)
    } catch (e: ToxFileSendChunkException) {
        Log.e(TAG, "Error sending chunk $pos:${data.size} to ${pk.fingerprint()} $fileNo\n$e")
        Result.failure(e)
    }

    fun setTyping(publicKey: PublicKey, typing: Boolean) = tox.setTyping(contactByKey(publicKey), typing)

    fun getStatus() = tox.getStatus.toUserStatus()
    fun setStatus(status: UserStatus) {
        tox.setStatus(status.toToxType())
    }

    fun sendLosslessPacket(pk: PublicKey, packet: ByteArray): CustomPacketError = try {
        tox.friendSendLosslessPacket(contactByKey(pk), ToxLosslessPacket(packet))
        CustomPacketError.Success
    } catch (e: ToxFriendCustomPacketException) {
        when (e.code) {
            ToxFriendCustomPacketException.Code.EMPTY -> CustomPacketError.Empty
            ToxFriendCustomPacketException.Code.FRIEND_NOT_CONNECTED -> CustomPacketError.FriendNotConnected
            ToxFriendCustomPacketException.Code.FRIEND_NOT_FOUND -> CustomPacketError.FriendNotFound
            ToxFriendCustomPacketException.Code.INVALID -> CustomPacketError.Invalid
            ToxFriendCustomPacketException.Code.NULL -> CustomPacketError.Null
            ToxFriendCustomPacketException.Code.SENDQ -> CustomPacketError.Sendq
            ToxFriendCustomPacketException.Code.TOO_LONG -> CustomPacketError.TooLong
            else -> TODO()
        }
    }

    private fun contactByKey(pk: PublicKey): ToxFriendNumber = tox.friendByPublicKey(ToxPublicKey(pk.bytes()))

    // ToxAv, probably move these.
    fun startCall(pk: PublicKey) = av.call(contactByKey(pk), AUDIO_BIT_RATE, BitRate(0))
    fun answerCall(pk: PublicKey) = av.answer(contactByKey(pk), AUDIO_BIT_RATE, BitRate(0))
    fun endCall(pk: PublicKey) = av.callControl(contactByKey(pk), ToxavCallControl.CANCEL)
    fun sendAudio(pk: PublicKey, pcm: ShortArray, channels: Int, samplingRate: Int) {
        av.audioSendFrame(
            contactByKey(pk),
            pcm,
            SampleCount(pcm.size),
            if (channels == 1) AudioChannels.Mono else AudioChannels.Stereo,
            SamplingRate.Rate48k,
        )
        assert(samplingRate == SamplingRate.Rate48k.value) // TODO(robinlinden)
    }
}
