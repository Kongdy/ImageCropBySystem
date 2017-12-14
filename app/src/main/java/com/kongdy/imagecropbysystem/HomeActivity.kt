package com.kongdy.imagecropbysystem

import android.annotation.SuppressLint
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.AppCompatButton
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import kotlinx.android.synthetic.main.activity_home.*

class HomeActivity : AppCompatActivity() {

    val menuList = arrayListOf(
            "图片选择",
            "权限封装"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        lv_menu_list.adapter = MenuAdapter()
    }


    inner class MenuAdapter : BaseAdapter() {

        @SuppressLint("ViewHolder")
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            var view = LayoutInflater.from(this@HomeActivity).inflate(R.layout.layout_simple_item, null)

            var acbtn_txt = view.findViewById<AppCompatButton>(R.id.acbtn_txt)
            acbtn_txt.text = getItem(position).toString()
            acbtn_txt.setOnClickListener(View.OnClickListener {
                when (position) {
                    0 -> {
                        startActivity(Intent(this@HomeActivity, MainActivity::class.java))
                    }
                    1 -> {
                        startActivity(Intent(this@HomeActivity, PermissionDemoActivity::class.java))
                    }
                }
            })

            return view
        }

        override fun getItem(position: Int): Any {
            return menuList[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getCount(): Int {
            return menuList.size
        }

    }

}
