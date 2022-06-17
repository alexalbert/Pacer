package com.aa.pacer

import android.annotation.SuppressLint
import android.graphics.Color
import android.widget.TextView
import android.os.Bundle
import android.view.*
import android.view.View.OnTouchListener
import androidx.fragment.app.Fragment

@SuppressLint("ClickableViewAccessibility")
class KeyboardFragment : Fragment() {
    var mK0: TextView? = null
    var mK1: TextView? = null
    var mK2: TextView? = null
    var mK3: TextView? = null
    var mK4: TextView? = null
    var mK5: TextView? = null
    var mK6: TextView? = null
    var mK7: TextView? = null
    var mK8: TextView? = null
    var mK9: TextView? = null
    var mClear: TextView? = null
    var mDone: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_keyboard, container, false)
        view.setBackgroundColor(Color.GRAY)
        mK0 = view.findViewById<View>(R.id.k0) as TextView
        mK1 = view.findViewById<View>(R.id.k1) as TextView
        mK2 = view.findViewById<View>(R.id.k2) as TextView
        mK3 = view.findViewById<View>(R.id.k3) as TextView
        mK4 = view.findViewById<View>(R.id.k4) as TextView
        mK5 = view.findViewById<View>(R.id.k5) as TextView
        mK6 = view.findViewById<View>(R.id.k6) as TextView
        mK7 = view.findViewById<View>(R.id.k7) as TextView
        mK8 = view.findViewById<View>(R.id.k8) as TextView
        mK9 = view.findViewById<View>(R.id.k9) as TextView
        mClear = view.findViewById<View>(R.id.clear) as TextView
        mDone = view.findViewById<View>(R.id.done) as TextView
        mK0!!.setOnTouchListener(mKeypadListener)
        mK1!!.setOnTouchListener(mKeypadListener)
        mK2!!.setOnTouchListener(mKeypadListener)
        mK3!!.setOnTouchListener(mKeypadListener)
        mK4!!.setOnTouchListener(mKeypadListener)
        mK5!!.setOnTouchListener(mKeypadListener)
        mK6!!.setOnTouchListener(mKeypadListener)
        mK7!!.setOnTouchListener(mKeypadListener)
        mK8!!.setOnTouchListener(mKeypadListener)
        mK9!!.setOnTouchListener(mKeypadListener)
        mClear!!.setOnTouchListener(mKeypadListener)
        mDone!!.setOnTouchListener(mKeypadListener)
        view.setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                (activity as PacerUI?)!!.removeKeyboard()
                true
            } else {
                false
            }
        }
        return view
    }

    var mKeypadListener = OnTouchListener { v, event ->
        if (event.action == MotionEvent.ACTION_DOWN) {
            if (v.id == R.id.done) {
                (activity as PacerUI?)!!.removeKeyboard()
            }
            if (v.id == R.id.clear) {
                (activity as PacerUI?)!!.mCurrentEdit.setText("")
            } else {
                val c: String
                c = when (v.id) {
                    R.id.k0 -> "0"
                    R.id.k1 -> "1"
                    R.id.k2 -> "2"
                    R.id.k3 -> "3"
                    R.id.k4 -> "4"
                    R.id.k5 -> "5"
                    R.id.k6 -> "6"
                    R.id.k7 -> "7"
                    R.id.k8 -> "8"
                    R.id.k9 -> "9"
                    else -> ""
                }
                val currentEdit = (activity as PacerUI?)!!.mCurrentEdit
                currentEdit.setText(currentEdit.text.toString() + c)
            }
        }
        false
    }
}