// SPDX-FileCopyrightText: 2019-2020 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.tox

import im.tox.tox4j.core.callbacks.ToxCoreEventListener
import im.tox.tox4j.core.data.ToxFilename
import im.tox.tox4j.core.data.ToxFriendMessage
import im.tox.tox4j.core.data.ToxFriendMessageId
import im.tox.tox4j.core.data.ToxFriendNumber
import im.tox.tox4j.core.data.ToxFriendRequestMessage
import im.tox.tox4j.core.data.ToxLosslessPacket
import im.tox.tox4j.core.data.ToxLossyPacket
import im.tox.tox4j.core.data.ToxNickname
import im.tox.tox4j.core.data.ToxPublicKey
import im.tox.tox4j.core.data.ToxStatusMessage
import im.tox.tox4j.core.enums.ToxConnection
import im.tox.tox4j.core.enums.ToxFileControl
import im.tox.tox4j.core.enums.ToxMessageType
import im.tox.tox4j.core.enums.ToxUserStatus
import javax.inject.Inject
import ltd.evilcorp.core.vo.ConnectionStatus
import ltd.evilcorp.core.vo.UserStatus

typealias FriendLosslessPacketHandler = (publicKey: String, data: ByteArray) -> Unit
typealias FileRecvControlHandler = (publicKey: String, fileNo: Int, control: ToxFileControl) -> Unit
typealias FriendStatusMessageHandler = (publicKey: String, message: String) -> Unit
typealias FriendReadReceiptHandler = (publicKey: String, messageId: Int) -> Unit
typealias FriendStatusHandler = (publicKey: String, status: UserStatus) -> Unit
typealias FriendConnectionStatusHandler = (publicKey: String, status: ConnectionStatus) -> Unit
typealias FriendRequestHandler = (publicKey: String, timeDelta: Int, message: String) -> Unit
typealias FriendMessageHandler = (
    publicKey: String,
    messageType: ToxMessageType,
    timeDelta: Int,
    message: String,
) -> Unit

typealias FriendNameHandler = (publicKey: String, newName: String) -> Unit
typealias FileRecvChunkHandler = (publicKey: String, fileNo: Int, position: Long, data: ByteArray) -> Unit
typealias FileRecvHandler = (publicKey: String, fileNo: Int, kind: Int, size: Long, name: String) -> Unit
typealias FriendLossyPacketHandler = (publicKey: String, data: ByteArray) -> Unit
typealias SelfConnectionStatusHandler = (status: ConnectionStatus) -> Unit
typealias FriendTypingHandler = (publicKey: String, isTyping: Boolean) -> Unit
typealias FileChunkRequestHandler = (publicKey: String, fileNo: Int, position: Long, length: Int) -> Unit

class ToxEventListener @Inject constructor() : ToxCoreEventListener<Unit> {
    var contactMapping: List<Pair<PublicKey, Int>> = listOf()

    var friendLosslessPacketHandler: FriendLosslessPacketHandler = { _, _ -> }
    var fileRecvControlHandler: FileRecvControlHandler = { _, _, _ -> }
    var friendStatusMessageHandler: FriendStatusMessageHandler = { _, _ -> }
    var friendReadReceiptHandler: FriendReadReceiptHandler = { _, _ -> }
    var friendStatusHandler: FriendStatusHandler = { _, _ -> }
    var friendConnectionStatusHandler: FriendConnectionStatusHandler = { _, _ -> }
    var friendRequestHandler: FriendRequestHandler = { _, _, _ -> }
    var friendMessageHandler: FriendMessageHandler = { _, _, _, _ -> }
    var friendNameHandler: FriendNameHandler = { _, _ -> }
    var fileRecvChunkHandler: FileRecvChunkHandler = { _, _, _, _ -> }
    var fileRecvHandler: FileRecvHandler = { _, _, _, _, _ -> }
    var friendLossyPacketHandler: FriendLossyPacketHandler = { _, _ -> }
    var selfConnectionStatusHandler: SelfConnectionStatusHandler = { _ -> }
    var friendTypingHandler: FriendTypingHandler = { _, _ -> }
    var fileChunkRequestHandler: FileChunkRequestHandler = { _, _, _, _ -> }

    private fun keyFor(friendNo: ToxFriendNumber) = contactMapping.find { it.second == friendNo.value }!!.first.string()

    override fun friendLosslessPacket(friendNumber: ToxFriendNumber, data: ToxLosslessPacket, state: Unit) =
        friendLosslessPacketHandler(keyFor(friendNumber), data.value)

    override fun fileRecvControl(friendNumber: ToxFriendNumber, fileNumber: Int, control: ToxFileControl, state: Unit) =
        fileRecvControlHandler(keyFor(friendNumber), fileNumber, control)

    override fun friendStatusMessage(friendNumber: ToxFriendNumber, message: ToxStatusMessage, state: Unit) =
        friendStatusMessageHandler(keyFor(friendNumber), String(message.value))

    override fun friendReadReceipt(friendNumber: ToxFriendNumber, messageId: ToxFriendMessageId, state: Unit) =
        friendReadReceiptHandler(keyFor(friendNumber), messageId.value)

    override fun friendStatus(friendNumber: ToxFriendNumber, status: ToxUserStatus, state: Unit) =
        friendStatusHandler(keyFor(friendNumber), status.toUserStatus())

    override fun friendConnectionStatus(friendNumber: ToxFriendNumber, connectionStatus: ToxConnection, state: Unit) =
        friendConnectionStatusHandler(keyFor(friendNumber), connectionStatus.toConnectionStatus())

    override fun friendRequest(publicKey: ToxPublicKey, message: ToxFriendRequestMessage, state: Unit) =
        friendRequestHandler(publicKey.value.bytesToHex(), 0, String(message.value))

    override fun friendMessage(
        friendNumber: ToxFriendNumber,
        type: ToxMessageType,
        message: ToxFriendMessage,
        state: Unit,
    ) = friendMessageHandler(keyFor(friendNumber), type, 0, String(message.value))

    override fun friendName(friendNumber: ToxFriendNumber, name: ToxNickname, state: Unit) =
        friendNameHandler(keyFor(friendNumber), String(name.value))

    override fun fileRecvChunk(
        friendNumber: ToxFriendNumber,
        fileNumber: Int,
        position: Long,
        data: ByteArray,
        state: Unit,
    ) = fileRecvChunkHandler(keyFor(friendNumber), fileNumber, position, data)

    override fun fileRecv(
        friendNumber: ToxFriendNumber,
        fileNumber: Int,
        kind: Int,
        fileSize: Long,
        filename: ToxFilename,
        state: Unit,
    ) = fileRecvHandler(keyFor(friendNumber), fileNumber, kind, fileSize, String(filename.value))

    override fun friendLossyPacket(friendNumber: ToxFriendNumber, data: ToxLossyPacket, state: Unit) =
        friendLossyPacketHandler(keyFor(friendNumber), data.value)

    override fun selfConnectionStatus(connectionStatus: ToxConnection, state: Unit) =
        selfConnectionStatusHandler(connectionStatus.toConnectionStatus())

    override fun friendTyping(friendNumber: ToxFriendNumber, isTyping: Boolean, state: Unit) =
        friendTypingHandler(keyFor(friendNumber), isTyping)

    override fun fileChunkRequest(
        friendNumber: ToxFriendNumber,
        fileNumber: Int,
        position: Long,
        length: Int,
        state: Unit,
    ) = fileChunkRequestHandler(keyFor(friendNumber), fileNumber, position, length)
}
