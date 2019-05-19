package ltd.evilcorp.atox

import android.util.Log
import im.tox.tox4j.core.enums.ToxMessageType
import im.tox.tox4j.core.options.ToxOptions
import im.tox.tox4j.impl.jni.ToxCoreImpl
import java.io.File

class Tox(options: ToxOptions) {
    private val tox: ToxCoreImpl = ToxCoreImpl(options)

    fun bootstrap(address: String, port: Int, publicKey: ByteArray) {
        tox.bootstrap(address, port, publicKey)
        tox.addTcpRelay(address, port, publicKey)
    }

    fun iterate(): Int = tox.iterate(ToxEventListener(), 42)
    fun iterationInterval(): Int = tox.iterationInterval()

    fun kill() {
        tox.close()
    }

    fun setName(name: String) {
        tox.name = name.toByteArray()
    }

    fun getName(): String {
        return String(tox.name)
    }

    fun addContact(toxId: String, message: String): Int {
        return tox.addFriend(toxId.hexToByteArray(), message.toByteArray())
    }

    fun sendMessage(friendNumber: Int, message: String): Int {
        return tox.friendSendMessage(friendNumber, ToxMessageType.NORMAL, 0, message.toByteArray())
    }

    fun save(destination: String, encrypt: Boolean) {
        val fileName = this.getName() + ".tox"

        val saveFile = File("$destination/$fileName")
        if (!saveFile.exists()) {
            saveFile.createNewFile()
        }

        Log.e("ToxCore", "Saving profile to $saveFile")

        saveFile.writeBytes(tox.savedata)
    }
}
