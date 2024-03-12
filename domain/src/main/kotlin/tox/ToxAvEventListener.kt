// SPDX-FileCopyrightText: 2020 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.tox

import im.tox.tox4j.av.callbacks.ToxAvEventListener
import im.tox.tox4j.av.data.AudioChannels
import im.tox.tox4j.av.data.BitRate
import im.tox.tox4j.av.data.Height
import im.tox.tox4j.av.data.SamplingRate
import im.tox.tox4j.av.data.Width
import im.tox.tox4j.av.enums.ToxavFriendCallState
import im.tox.tox4j.core.data.ToxFriendNumber
import javax.inject.Inject

typealias CallHandler = (pk: String, audioEnabled: Boolean, videoEnabled: Boolean) -> Unit
typealias CallStateHandler = (pk: String, callState: Set<ToxavFriendCallState>) -> Unit
typealias VideoBitRateHandler = (pk: String, bitRate: Int) -> Unit
typealias VideoReceiveFrameHandler = (
    pk: String,
    width: Int,
    height: Int,
    y: ByteArray,
    u: ByteArray,
    v: ByteArray,
    yStride: Int,
    uStride: Int,
    vStride: Int,
) -> Unit

typealias AudioReceiveFrameHandler = (pk: String, pcm: ShortArray, channels: Int, samplingRate: Int) -> Unit
typealias AudioBitRateHandler = (pk: String, bitRate: Int) -> Unit

class ToxAvEventListener @Inject constructor() : ToxAvEventListener<Unit> {
    var contactMapping: List<Pair<PublicKey, Int>> = listOf()

    var callHandler: CallHandler = { _, _, _ -> }
    var callStateHandler: CallStateHandler = { _, _ -> }
    var videoBitRateHandler: VideoBitRateHandler = { _, _ -> }
    var videoReceiveFrameHandler: VideoReceiveFrameHandler = { _, _, _, _, _, _, _, _, _ -> }
    var audioReceiveFrameHandler: AudioReceiveFrameHandler = { _, _, _, _ -> }
    var audioBitRateHandler: AudioBitRateHandler = { _, _ -> }

    private fun keyFor(friendNo: ToxFriendNumber) = contactMapping.find { it.second == friendNo.value }!!.first.string()

    override fun call(friendNumber: ToxFriendNumber, audioEnabled: Boolean, videoEnabled: Boolean, state: Unit) =
        callHandler(keyFor(friendNumber), audioEnabled, videoEnabled)

    override fun videoBitRate(friendNumber: ToxFriendNumber, videoBitRate: BitRate, state: Unit) =
        videoBitRateHandler(keyFor(friendNumber), videoBitRate.value)

    override fun videoFrameCachedYUV(
        height: Height,
        yStride: Int,
        uStride: Int,
        vStride: Int,
    ): Triple<ByteArray, ByteArray, ByteArray>? = null

    override fun videoReceiveFrame(
        friendNumber: ToxFriendNumber,
        width: Width,
        height: Height,
        y: ByteArray,
        u: ByteArray,
        v: ByteArray,
        yStride: Int,
        uStride: Int,
        vStride: Int,
        state: Unit,
    ) = videoReceiveFrameHandler(
        keyFor(friendNumber),
        width.value, height.value,
        y, u, v,
        yStride, uStride, vStride,
    )

    override fun callState(friendNumber: ToxFriendNumber, callState: Set<ToxavFriendCallState>, state: Unit) =
        callStateHandler(keyFor(friendNumber), callState)

    override fun audioReceiveFrame(
        friendNumber: ToxFriendNumber,
        pcm: ShortArray,
        channels: AudioChannels,
        samplingRate: SamplingRate,
        state: Unit,
    ) = audioReceiveFrameHandler(keyFor(friendNumber), pcm, channels.value, samplingRate.value)

    override fun audioBitRate(friendNumber: ToxFriendNumber, audioBitRate: BitRate, state: Unit) =
        audioBitRateHandler(keyFor(friendNumber), audioBitRate.value)
}
