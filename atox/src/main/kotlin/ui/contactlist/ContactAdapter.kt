package ltd.evilcorp.atox.ui.contactlist

import android.content.res.Resources
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.DateFormat
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.databinding.ContactListViewItemBinding
import ltd.evilcorp.atox.databinding.FriendRequestItemBinding
import ltd.evilcorp.atox.ui.colorByStatus
import ltd.evilcorp.atox.ui.setAvatarFromContact
import ltd.evilcorp.core.vo.Contact
import ltd.evilcorp.core.vo.FriendRequest

enum class ContactListItemType {
    FriendRequest,
    Contact
}

private val types = ContactListItemType.values()

class ContactAdapter(
    private val layoutInflater: LayoutInflater,
    private val menuInflater: MenuInflater,
    private val resources: Resources,
    private val onContactClick: (Contact) -> Unit,
    private val onFriendRequestClick: (FriendRequest) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var friendRequests: List<FriendRequest> = listOf()
    var contacts: List<Contact> = listOf()

    override fun getItemCount(): Int = friendRequests.size + contacts.size
    override fun getItemId(position: Int): Long = position.toLong()
    override fun getItemViewType(position: Int): Int = when {
        position < friendRequests.size -> ContactListItemType.FriendRequest.ordinal
        else -> ContactListItemType.Contact.ordinal
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder = when (viewType) {
        ContactListItemType.FriendRequest.ordinal -> {
            val view = layoutInflater.inflate(R.layout.friend_request_item, parent, false)
            FriendRequestViewHolder(FriendRequestItemBinding.bind(view), onFriendRequestClick)
        }
        ContactListItemType.Contact.ordinal -> {
            val view = layoutInflater.inflate(R.layout.contact_list_view_item, parent, false)
            ContactViewHolder(ContactListViewItemBinding.bind(view), onContactClick, menuInflater)
        }
        else -> throw IllegalArgumentException("This is unreachable")
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
        when (types[getItemViewType(position)]) {
            ContactListItemType.FriendRequest -> (holder as FriendRequestViewHolder).bind(friendRequests[position])
            ContactListItemType.Contact -> (holder as ContactViewHolder).bind(
                contacts[position - friendRequests.size],
                resources
            )
        }

    fun getItem(position: Int): Any = when {
        position < friendRequests.size -> friendRequests[position]
        else -> contacts[position - friendRequests.size]
    }

    private class FriendRequestViewHolder(
        row: FriendRequestItemBinding, private val onClick: (FriendRequest) -> Unit
    ) : RecyclerView.ViewHolder(row.root), View.OnClickListener {
        val publicKey: TextView = row.publicKey
        val message: TextView = row.message

        init {
            row.root.setOnClickListener(this)
        }

        private lateinit var friendRequest: FriendRequest

        fun bind(v: FriendRequest) {
            publicKey.text = v.publicKey
            message.text = v.message
            friendRequest = v
        }

        override fun onClick(v: View?) = onClick(friendRequest)
    }

    private class ContactViewHolder(
        row: ContactListViewItemBinding,
        private val onClick: (Contact) -> Unit,
        private val inflater: MenuInflater
    ) : RecyclerView.ViewHolder(row.root), View.OnClickListener, View.OnCreateContextMenuListener {
        val name: TextView = row.name
        val publicKey: TextView = row.publicKey
        val statusMessage: TextView = row.statusMessage
        val lastMessage: TextView = row.lastMessage
        val status: ImageView = row.profileImageLayout.statusIndicator
        val image: ImageView = row.profileImageLayout.profileImage
        val unreadIndicator: ImageView = row.unreadIndicator

        init {
            row.root.setOnClickListener(this)
            row.root.setOnCreateContextMenuListener(this)
        }

        private lateinit var contact: Contact

        fun bind(v: Contact, resources: Resources) {
            val shortId = v.publicKey.take(8)
            publicKey.text = String.format("%s %s", shortId.take(4), shortId.takeLast(4))
            name.text = v.name
            lastMessage.text = if (v.lastMessage != 0L) {
                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                    .format(v.lastMessage)
            } else {
                resources.getText(R.string.never)
            }
            if (v.draftMessage.isNotEmpty()) {
                statusMessage.text = resources.getString(R.string.draft_message, v.draftMessage)
                statusMessage.setTextColor(ResourcesCompat.getColor(resources, R.color.colorAccent, null))
            } else {
                statusMessage.text = v.statusMessage
                statusMessage.setTextColor(lastMessage.currentTextColor)
            }
            status.setColorFilter(colorByStatus(resources, v))
            setAvatarFromContact(image, v)
            unreadIndicator.visibility = if (v.hasUnreadMessages) {
                View.VISIBLE
            } else {
                View.GONE
            }

            contact = v
        }

        override fun onClick(v: View?) = onClick(contact)

        override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
            menu.setHeaderTitle(name.text)
            inflater.inflate(R.menu.contact_list_context_menu, menu)
        }
    }
}
