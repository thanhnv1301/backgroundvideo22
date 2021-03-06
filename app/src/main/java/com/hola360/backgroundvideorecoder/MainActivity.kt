package com.hola360.backgroundvideorecoder

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.google.android.material.snackbar.Snackbar
import com.hola360.backgroundvideorecoder.databinding.ActivityMainBinding
import com.hola360.backgroundvideorecoder.service.RecordService
import com.hola360.backgroundvideorecoder.ui.privacy.ConfirmPrivacy
import com.hola360.backgroundvideorecoder.ui.setting.applock.AppLockFragment
import com.hola360.backgroundvideorecoder.utils.SharedPreferenceUtils
import com.hola360.backgroundvideorecoder.utils.SystemUtils
import com.hola360.backgroundvideorecoder.utils.ToastUtils
import com.hola360.backgroundvideorecoder.utils.Utils
import com.hola360.backgroundvideorecoder.widget.Toolbar
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private var binding: ActivityMainBinding? = null
    private var navController: NavController? = null
    private var navHostFragment: Fragment? = null
    var recordService: RecordService? = null
    var isBound = false
    private var dataSharedPreferenceUtils: SharedPreferenceUtils? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        dataSharedPreferenceUtils = SharedPreferenceUtils.getInstance(this)
        setupNavigation()
        setupToolbar()
        bindService()
        setupPrivacyAndAppLock()
    }

    private fun setupNavigation() {
        navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_main)
        navController = findNavController(R.id.nav_host_fragment_main)
    }

    private fun setupToolbar() {
        binding!!.toolbar.setupToolbarCallback(object : Toolbar.CustomToolbarCallback {
            override fun onBack() {
                navController?.popBackStack()
            }
        })
        SCREEN_WIDTH = SystemUtils.getScreenWidth(this)
        SCREEN_HEIGHT = SystemUtils.getScreenHeight(this)
    }

    private fun setupPrivacyAndAppLock() {
        if (!dataSharedPreferenceUtils!!.getBooleanValue(PRIVACY)) {
            navController!!.navigate(R.id.nav_confirm_privacy)
        } else {
            val passcode = dataSharedPreferenceUtils!!.getPasscode() ?: ""
            if (passcode != "") {
                navController!!.navigate(NavMainGraphDirections.actionToNavAppLock(AppLockFragment.LOGIN_MODE))
            }
        }
    }

    fun hideToolbar() {
        binding?.showToolbar = false
    }

    fun showToolbar() {
        binding?.showToolbar = true
    }

    fun setToolbarTitle(title: String?) {
        binding?.toolbar?.setToolbarTitle(title)
    }

    fun showToolbarMenu(menuCode: Int) {
        binding?.toolbar?.showToolbarMenu(menuCode)
    }

    fun showAlertPermissionNotGrant() {
        if (!SystemUtils.hasShowRequestPermissionRationale(this, *Utils.getStoragePermissions())
        ) {
            val snackBar = Snackbar.make(
                mLayoutRoot,
                resources.getString(R.string.goto_settings),
                Snackbar.LENGTH_LONG
            )
            snackBar.setAction(
                resources.getString(R.string.settings)
            ) {
                val intent = Intent()
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                val uri: Uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            snackBar.show()
        } else {
            Toast.makeText(this, getString(R.string.grant_permission), Toast.LENGTH_SHORT).show()
        }
    }

    private val mConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(
            className: ComponentName,
            service: IBinder
        ) {
            val binder: RecordService.LocalBinder = service as RecordService.LocalBinder
            recordService = binder.getServiceInstance()
            isBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
        }
    }

    private fun bindService() {
        val intent = Intent(this, RecordService::class.java)
        startService(intent)
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
    }

    fun showToast(message: String) {
        ToastUtils.getInstance(this)!!.showToast(message)
    }

    override fun onDestroy() {
        super.onDestroy()
        ToastUtils.getInstance(this)!!.release()
        if (isBound) {
            recordService!!.registerListener(null)
            unbindService(mConnection)
        }
    }

    override fun onBackPressed() {
        val fragment= navHostFragment!!.childFragmentManager.fragments[0]
        if(fragment!= null){
            when(fragment){
                is AppLockFragment->{
                    if(fragment.type == AppLockFragment.LOGIN_MODE){
                        finish()
                    }
                }
                is ConfirmPrivacy ->{
                    finish()
                }
                else->{
                    super.onBackPressed()
                }
            }
        } else {
            super.onBackPressed()
        }
    }

    companion object {
        var SCREEN_WIDTH: Int = 0
        var SCREEN_HEIGHT: Int = 0
        const val PRIVACY = "privacy"
    }
}