package com.kongdy.imagecropbysystem

import android.Manifest
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.kongdy.permissionlib.utils.AppPermissionIml

class PermissionDemoActivity : AppCompatActivity(), AppPermissionIml.AppPermissionListener {

    lateinit var permissionUtils: AppPermissionIml


    override fun onAllGranted() {
        toast("已获得权限")
    }

    override fun onHaveDenied(deniedPermissions: MutableList<String>?) {
        toast("权限被拒绝")
    }

    override fun onNeverAsk(neverAskPermissions: MutableList<String>?) {
        toast("权限不在询问")
    }

    override fun onJiuShiBuGeiQuanXian() {
        toast("再次拒绝权限")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_permission_demo)

        permissionUtils = AppPermissionIml(this)
    }

    fun myClick(v: View) {
        permissionUtils.needPermission(this,
                Manifest.permission.CAMERA)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionUtils.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        permissionUtils.onActivityResult(requestCode, resultCode, data)
    }

    private fun toast(s: String) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show()
    }
}
