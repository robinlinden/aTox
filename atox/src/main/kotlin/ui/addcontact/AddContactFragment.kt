package ltd.evilcorp.atox.ui.addcontact

import com.google.zxing.integration.android.IntentIntegrator
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import kotlinx.android.synthetic.main.add_contact_fragment.view.*
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.vmFactory
import ltd.evilcorp.domain.tox.ToxID
import ltd.evilcorp.domain.tox.ToxIdValidator
import android.util.Log

class AddContactFragment : Fragment() {
    private val viewModel: AddContactViewModel by viewModels { vmFactory }

    private var toxIdValid: Boolean = false
    private var messageValid: Boolean = true

    private fun isAddAllowed(): Boolean = toxIdValid && messageValid

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.add_contact_fragment, container, false).apply {
        toolbar.setNavigationIcon(R.drawable.back)
        toolbar.setNavigationOnClickListener {
            activity?.onBackPressed()
        }

        toxId.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                toxId.error = when (ToxIdValidator.validate(ToxID(s?.toString() ?: ""))) {
                    ToxIdValidator.Result.INCORRECT_LENGTH -> getString(R.string.tox_id_error_length)
                    ToxIdValidator.Result.INVALID_CHECKSUM -> getString(R.string.tox_id_error_checksum)
                    ToxIdValidator.Result.NOT_HEX -> getString(R.string.tox_id_error_hex)
                    ToxIdValidator.Result.NO_ERROR -> null
                }

                val count = s?.toString()?.length
                val str = String.format("%d/%d", count, 76)

                n_characters.text = str

                toxIdValid = toxId.error == null
                add.isEnabled = isAddAllowed()
            }
        })

        message.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val content = s?.toString() ?: ""
                message.error = if (content.isNotEmpty()) null else getString(R.string.add_contact_message_error_empty)

                messageValid = message.error == null
                add.isEnabled = isAddAllowed()
            }
        })

        add.setOnClickListener {
            viewModel.addContact(ToxID(toxId.text.toString()), message.text.toString())
            findNavController().popBackStack()
        }

        var qrScanIntegrator: IntentIntegrator?

        read_qr.setOnClickListener {
            // Open QR code scanner
            qrScanIntegrator = IntentIntegrator(activity)
            qrScanIntegrator?.setOrientationLocked(false)
            qrScanIntegrator?.setBeepEnabled(false)

            val codes: Collection<String> = listOf(IntentIntegrator.QR_CODE)

            qrScanIntegrator?.initiateScan(codes)


        }

        add.isEnabled = false

        toxId.setText(arguments?.getString("toxId"), TextView.BufferType.EDITABLE)
    }
}
