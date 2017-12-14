package com.kongdy.imagecropbysystem

import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.kongdy.croplib.widgets.PickImageWidget
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity(), PickImageWidget.IconPickListener {

    override fun onFail(code: Int) {
        Toast.makeText(this,"error code:"+code,Toast.LENGTH_LONG).show()
    }

    override fun onResult(compressImage: File?, uri: Uri?) {
        aciv_image.setImageURI(uri)
    }

    private lateinit var pickWidget:PickImageWidget

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pickWidget = PickImageWidget(this,this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        pickWidget.activityResult(requestCode, resultCode, data)
    }

    fun myClick(v: View){
        pickWidget.show()
    }
}
