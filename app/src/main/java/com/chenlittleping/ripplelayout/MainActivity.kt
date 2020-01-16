package com.chenlittleping.ripplelayout

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ripple1.setStateListener(object : RippleLayout.IRippleStateChange {
            override fun onRippleChangeStart(selected: Boolean) {
                if (selected) {
                    ripple2.unCheck()
                    ripple3.unCheck()
                }
            }

            override fun onRippleChanging(selected: Boolean, percent: Float) {
                if (percent > 0.5) {
                    if (selected) {
                        tv1.setTextColor(Color.WHITE)
                    } else {
                        tv1.setTextColor(Color.BLACK)
                    }
                }
            }

            override fun onRippleChanged(selected: Boolean) {
            }

        })
        ripple2.setStateListener(object : RippleLayout.IRippleStateChange {
            override fun onRippleChangeStart(selected: Boolean) {
                if (selected) {
                    ripple1.unCheck()
                    ripple3.unCheck()
                }
            }

            override fun onRippleChanging(selected: Boolean, percent: Float) {
                if (percent > 0.5) {
                    if (selected) {
                        tv2.setTextColor(Color.WHITE)
                    } else {
                        tv2.setTextColor(Color.BLACK)
                    }
                }
            }

            override fun onRippleChanged(selected: Boolean) {
            }

        })
        ripple3.setStateListener(object : RippleLayoutKtl.IRippleStateChange {
            override fun onRippleChangeStart(selected: Boolean) {
                if (selected) {
                    ripple1.unCheck()
                    ripple2.unCheck()
                }
            }

            override fun onRippleChanging(selected: Boolean, percent: Float) {
                if (percent > 0.5) {
                    if (selected) {
                        tv3.setTextColor(Color.WHITE)
                    } else {
                        tv3.setTextColor(Color.BLACK)
                    }
                }
            }

            override fun onRippleChanged(selected: Boolean) {
            }
        })
    }
}
