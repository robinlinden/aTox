// SPDX-FileCopyrightText: 2019-2021 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.settings

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import java.lang.NumberFormatException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ltd.evilcorp.atox.BuildConfig
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.databinding.FragmentSettingsBinding
import ltd.evilcorp.atox.settings.BootstrapNodeSource
import ltd.evilcorp.atox.settings.FtAutoAccept
import ltd.evilcorp.atox.settings.NetworkMode
import ltd.evilcorp.atox.ui.BaseFragment
import ltd.evilcorp.atox.vmFactory
import ltd.evilcorp.domain.tox.ProxyType

private fun Spinner.onItemSelectedListener(callback: (Int) -> Unit) {
    this.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) { /* Do nothing. */ }
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            callback(position)
        }
    }
}

class SettingsFragment : BaseFragment<FragmentSettingsBinding>(FragmentSettingsBinding::inflate) {
    private val vm: SettingsViewModel by viewModels { vmFactory }
    private val blockBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            Toast.makeText(requireContext(), getString(R.string.warn_proxy_broken), Toast.LENGTH_LONG).show()
        }
    }

    private val applySettingsCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            vm.commit()
        }
    }

    private val importNodesLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        GlobalScope.launch {
            if (uri != null && vm.validateNodeJson(uri)) {
                if (vm.importNodeJson(uri)) {
                    vm.setBootstrapNodeSource(BootstrapNodeSource.UserProvided)
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    binding.settingBootstrapNodes.setSelection(BootstrapNodeSource.BuiltIn.ordinal)

                    Toast.makeText(
                        requireContext(),
                        getString(R.string.warn_node_json_import_failed),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requireActivity().onBackPressedDispatcher.addCallback(this, applySettingsCallback)
        requireActivity().onBackPressedDispatcher.addCallback(this, blockBackCallback)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) = binding.run {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, compat ->
            val insets = compat.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            toolbar.updatePadding(top = insets.top)
            v.updatePadding(left = insets.left, right = insets.right)
            version.updatePadding(bottom = insets.bottom)
            compat
        }

        toolbar.apply {
            setNavigationIcon(R.drawable.back)
            setNavigationOnClickListener {
                WindowInsetsControllerCompat(requireActivity().window, view)
                    .hide(WindowInsetsCompat.Type.ime())
                requireActivity().onBackPressed()
            }
        }

        theme.adapter = ArrayAdapter.createFromResource(
            requireContext(), R.array.pref_theme_options,
            android.R.layout.simple_spinner_item
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        theme.setSelection(vm.getTheme())

        theme.onItemSelectedListener {
            vm.setTheme(it)
            if (it == 2) {
//                val cm = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val cm = ContextCompat.getSystemService(requireContext(), ConnectivityManager::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    cm?.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                        override fun onAvailable(network: Network) {
                            super.onAvailable(network)
                            Log.e("aaaa", "connected to ${cm.isActiveNetworkMetered}")
                        }
                    })
                }
            }
        }

        settingRunAtStartup.isChecked = vm.getRunAtStartup()
        settingRunAtStartup.setOnClickListener { vm.setRunAtStartup(settingRunAtStartup.isChecked) }

        settingAutoAwayEnabled.isChecked = vm.getAutoAwayEnabled()
        settingAutoAwayEnabled.setOnClickListener { vm.setAutoAwayEnabled(settingAutoAwayEnabled.isChecked) }

        settingAutoAwaySeconds.setText(vm.getAutoAwaySeconds().toString())
        settingAutoAwaySeconds.doAfterTextChanged {
            val str = it?.toString() ?: ""
            val seconds = try {
                str.toLong()
            } catch (e: NumberFormatException) {
                settingAutoAwaySeconds.error = getString(R.string.bad_positive_number)
                return@doAfterTextChanged
            }

            vm.setAutoAwaySeconds(seconds)
        }

        settingFtAutoAccept.adapter = ArrayAdapter.createFromResource(
            requireContext(), R.array.pref_ft_auto_accept_options,
            android.R.layout.simple_spinner_item
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        settingFtAutoAccept.setSelection(vm.getFtAutoAccept().ordinal)

        settingFtAutoAccept.onItemSelectedListener {
            vm.setFtAutoAccept(FtAutoAccept.values()[it])
        }

        if (vm.getProxyType() != ProxyType.None) {
            vm.setNetworkMode(NetworkMode.TCP)
        }

        // The order of these must match the order in the NetworkMode enum.
        val networkModeOptions = mutableListOf(
            resources.getString(R.string.pref_network_mode_udp),
            resources.getString(R.string.pref_network_mode_tcp),
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            networkModeOptions.add(resources.getString(R.string.pref_network_mode_auto))
        }

        settingNetworkMode.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_item, networkModeOptions
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        settingNetworkMode.setSelection(vm.getNetworkMode().ordinal)
        settingNetworkMode.isEnabled = vm.getProxyType() == ProxyType.None
        settingNetworkMode.onItemSelectedListener {
            vm.setNetworkMode(NetworkMode.values()[it])
        }

        proxyType.adapter = ArrayAdapter.createFromResource(
            requireContext(), R.array.pref_proxy_type_options, android.R.layout.simple_spinner_item
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        proxyType.setSelection(vm.getProxyType().ordinal)

        proxyType.onItemSelectedListener {
            val selected = ProxyType.values()[it]
            vm.setProxyType(selected)

            // Disable UDP if a proxy is selected to ensure all traffic goes through the proxy.
            settingNetworkMode.isEnabled = selected == ProxyType.None
            if (selected != ProxyType.None) {
                settingNetworkMode.setSelection(NetworkMode.TCP.ordinal)
                vm.setNetworkMode(NetworkMode.TCP)
            }
        }

        proxyAddress.setText(vm.getProxyAddress())
        proxyAddress.doAfterTextChanged { vm.setProxyAddress(it?.toString() ?: "") }

        proxyPort.setText(vm.getProxyPort().toString())
        proxyPort.doAfterTextChanged {
            val str = it?.toString() ?: ""
            val port = try {
                Integer.parseInt(str)
            } catch (e: NumberFormatException) {
                proxyPort.error = getString(R.string.bad_port)
                return@doAfterTextChanged
            }

            if (port < 1 || port > 65535) {
                proxyPort.error = getString(R.string.bad_port)
                return@doAfterTextChanged
            }

            vm.setProxyPort(port)
        }

        vm.proxyStatus.observe(viewLifecycleOwner) { status: ProxyStatus ->
            proxyStatus.text = when (status) {
                ProxyStatus.Good -> ""
                ProxyStatus.BadPort -> getString(R.string.bad_port)
                ProxyStatus.BadHost -> getString(R.string.bad_host)
                ProxyStatus.BadType -> getString(R.string.bad_type)
                ProxyStatus.NotFound -> getString(R.string.proxy_not_found)
            }
            blockBackCallback.isEnabled = proxyStatus.text.isNotEmpty()
        }
        vm.checkProxy()

        vm.committed.observe(viewLifecycleOwner) { committed ->
            if (committed) {
                findNavController().popBackStack()
            }
        }

        fun onPasswordEdit() {
            passwordCurrent.error = if (vm.isCurrentPassword(passwordCurrent.text.toString())) {
                null
            } else {
                getString(R.string.incorrect_password)
            }

            passwordNewConfirm.error = if (passwordNew.text.toString() == passwordNewConfirm.text.toString()) {
                null
            } else {
                getString(R.string.passwords_must_match)
            }

            passwordConfirm.isEnabled = passwordCurrent.error == null && passwordNewConfirm.error == null
        }
        onPasswordEdit()

        passwordCurrent.doAfterTextChanged { onPasswordEdit() }
        passwordNew.doAfterTextChanged { onPasswordEdit() }
        passwordNewConfirm.doAfterTextChanged { onPasswordEdit() }
        passwordConfirm.setOnClickListener {
            passwordConfirm.isEnabled = false
            vm.setPassword(passwordNewConfirm.text.toString())
            Toast.makeText(requireContext(), getString(R.string.password_updated), Toast.LENGTH_LONG).show()
        }

        nospam.setText("%08X".format(vm.getNospam()))
        nospam.doAfterTextChanged {
            saveNospam.isEnabled =
                nospam.text.length == 8 && nospam.text.toString().toUInt(16).toInt() != vm.getNospam()
        }
        saveNospam.isEnabled = false
        saveNospam.setOnClickListener {
            vm.setNospam(nospam.text.toString().toUInt(16).toInt())
            saveNospam.isEnabled = false
            Toast.makeText(requireContext(), R.string.saved, Toast.LENGTH_LONG).show()
        }

        settingBootstrapNodes.adapter = ArrayAdapter.createFromResource(
            requireContext(), R.array.pref_bootstrap_node_options,
            android.R.layout.simple_spinner_item
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        settingBootstrapNodes.setSelection(vm.getBootstrapNodeSource().ordinal)

        settingBootstrapNodes.onItemSelectedListener {
            val source = BootstrapNodeSource.values()[it]

            // Hack to avoid triggering the document chooser again if the user has set it to UserProvided.
            if (source == vm.getBootstrapNodeSource()) return@onItemSelectedListener

            if (source == BootstrapNodeSource.BuiltIn) {
                vm.setBootstrapNodeSource(source)
            } else {
                importNodesLauncher.launch(arrayOf("application/json"))
            }
        }

        settingDisableScreenshots.isChecked = vm.getDisableScreenshots()
        settingDisableScreenshots.setOnClickListener {
            vm.setDisableScreenshots(settingDisableScreenshots.isChecked)
            if (settingDisableScreenshots.isChecked) {
                requireActivity().window.setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE
                )
            } else {
                requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }

        version.text = getString(R.string.version_display, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
    }
}
