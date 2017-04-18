package io.iotv.app

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import org.jetbrains.anko.*
import org.jetbrains.anko.cardview.v7.cardView

class MyVideosActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        verticalLayout {
            padding = dip(30)
            cardView {
                relativeLayout {
                    padding = dip(30)
                    textView {
                        width = wrapContent
                        height = wrapContent
                        text = "hello"
                    }
                    textView {
                        width = wrapContent
                        height = wrapContent
                        text = "world"
                    }
                }.lparams {
                    width = matchParent
                    height = wrapContent
                }
            }
        }
    }
}