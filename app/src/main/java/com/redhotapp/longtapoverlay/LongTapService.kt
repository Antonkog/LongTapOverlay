package com.redhotapp.longtapoverlay

//
// Created by Antonio on 2019-12-16.
// email: akogan777@gmail.com
// Copyright (c) 2019 redhotapp. All rights reserved.
//


import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Binder
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
import android.widget.RelativeLayout
import com.redhotapp.longtapoverlay.MotionRelay.LEFT_CLICK
import com.redhotapp.longtapoverlay.MotionRelay.LONG_CLICK
import com.redhotapp.longtapoverlay.MotionRelay.UPDATE_CURSOR_POSITION

import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import org.jetbrains.anko.longToast
import timber.log.Timber
import java.util.concurrent.TimeUnit


class LongTapService : Service() {
    private lateinit var circleView: RelativeLayout
    private lateinit var wheelView: View
    private lateinit var windowManager: WindowManager

    private val binder = LocalBinder()

    @Suppress("DEPRECATION")
    private val menuLayout = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,//TYPE_SYSTEM_ALERT,//TYPE_SYSTEM_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                FLAG_NOT_TOUCH_MODAL, //will cover status bar as well!!!
        PixelFormat.TRANSLUCENT).apply { gravity = Gravity.START or Gravity.TOP }

    private var hideTimer: Disposable? = null
//    private val instrumentation = Instrumentation()

    private var isAdded: Boolean = false

    private var x = 200f
    private var y = 200f

    private var maxX: Int = 0
    private var maxY: Int = 0

    override fun onBind(intent: Intent?): IBinder = binder

    private val motionDisposable = MotionRelay.relay.subscribe({
        update(it)
    }, {})

    inner class LocalBinder : Binder() {
        fun getService() = this@LongTapService
    }

    override fun onCreate() {
        super.onCreate()
        circleView = View.inflate(applicationContext, R.layout.layout_long_tap, null) as RelativeLayout

        val displayMetrics = DisplayMetrics()
        try {
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.defaultDisplay?.getMetrics(displayMetrics)
            with(displayMetrics) {
                maxX = widthPixels
                maxY = heightPixels
            }

            windowManager.addView(circleView, menuLayout)
            isAdded = true
        } catch (e: Throwable) {
            Timber.e(e, e.message)
            longToast(R.string.overlay_was_denied)
            isAdded = false
        }

//        setWheelView()

        Timber.e(this.javaClass.simpleName + "setting wheel")

    }
//
//    private fun setWheelView() {
//        wheelView = circleView.findViewById<View>(R.id.wheelview) as WheelView
//
//      val  map   =  listOf( " rm" to Color.BLACK, "rw " to  Color.GREEN,
//              " wm" to Color.BLACK, "rr " to  Color.GREEN,
//              " rm" to Color.BLACK, "rr " to  Color.GREEN)
//
//
//        //populate the adapter, that knows how to draw each item (as you would do with a ListAdapter)
//        wheelView.adapter = MaterialColorAdapter(map)
//
//        //a listener for receiving a callback for when the item closest to the selection angle changes
//        wheelView.setOnWheelItemSelectedListener { parent, itemDrawable, position ->
//            //get the item at this position
////            val selectedEntry = (parent.adapter as MaterialColorAdapter).getItem(position)
//            parent.setSelectionColor(Color.MAGENTA)
//        }
//
//        wheelView.onWheelItemClickListener = WheelView.OnWheelItemClickListener { parent, position, isSelected ->
//            val msg = "$position $isSelected"
//            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
//        }
//
//        //initialise the selection drawable with the first contrast color
//        wheelView.setSelectionColor(Color.CYAN)
//    }


    private fun update(event: MotionRelay.CursorMotionEvent) {
        val tx = (event.x as Int).toPx
        val ty = (event.y as Int).toPx
        x += tx
        y += ty

        //SET DISPLAY BORDERS
        if (maxY <= y) {
            y = maxY.toFloat()
        }
        if (maxX <= x) {
            x = maxX.toFloat()
        }

        if (x <= 0) {
            x = 0f
        }

        if (y <= 0) {
            y = 0f
        }


        when (event.type) {
            UPDATE_CURSOR_POSITION -> {
//                wheelView.onRemoteTouch(MotionEvent.obtain(System.currentTimeMillis() - 100, System.currentTimeMillis(), MotionEvent.ACTION_MOVE, x, y, 0))
//                onMouseMove(event.x.toInt(), event.y.toInt())
            }
            LONG_CLICK -> {
//                wheelView.onRemoteTouch(MotionEvent.obtain(System.currentTimeMillis() - 100, System.currentTimeMillis(), MotionEvent.ACTION_UP, x, y, 0))
            }

            LEFT_CLICK -> {
                showMenu()
//                wheelView.onRemoteTouch(MotionEvent.obtain(System.currentTimeMillis() - 100, System.currentTimeMillis(), MotionEvent.ACTION_UP, x, y, 0))
            }

            else -> Timber.e("Unknown action")
        }


        try {
            windowManager.updateViewLayout(wheelView, menuLayout)
        } catch (e: IllegalArgumentException) {
            Timber.e(e, "Illegal argument exception: ${e.message}. Are you sure that you have SYSTEM_ALERT_WINDOW permission?")
        }

    }

    private fun showMenu() {
        showMenu(true)
        hideTimer?.takeUnless { it.isDisposed }?.dispose()
        hideTimer = Observable.timer(5, TimeUnit.SECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ _ -> showMenu(false) }) { e -> Timber.e(e, e.message) }
    }

    private fun showMenu(status: Boolean) = circleView.run {
        //        showMenu(status)
        wheelView.visibility = if(status)View.VISIBLE else View.VISIBLE
        //need to do something
        postInvalidate()
    }

    override fun onDestroy() {
        if (isAdded) windowManager.removeView(circleView)
        motionDisposable.takeUnless { it.isDisposed }?.dispose()
        super.onDestroy()
    }

    private fun onMouseMove(dx: Int, dy: Int) {
        val tx = dx.toPx
        val ty = dy.toPx

        x += tx
        y += ty

        //SET DISPLAY BORDERS
        if (maxY <= y) {
            y = maxY.toFloat()
        }
        if (maxX <= x) {
            x = maxX.toFloat()
        }

        if (x <= 0) {
            x = 0f
        }

        if (y <= 0) {
            y = 0f
        }

        try {
            windowManager.updateViewLayout(circleView, menuLayout)
        } catch (e: IllegalArgumentException) {
            Timber.e(e, "Illegal argument exception: ${e.message}. Are you sure that you have SYSTEM_ALERT_WINDOW permission?")
        }

    }

//
//
//    internal class MaterialColorAdapter(entries: List<Pair<String, Int>>) : WheelArrayAdapter<List<Pair<String, Int>>>(listOf(entries)) {
//        override fun getDrawable(position: Int): Drawable {
//            val drawable = arrayOf(createOvalDrawable(Color.GREEN), TextDrawable(position.toString()))
//            return LayerDrawable(drawable)
//        }
//
//        private fun createOvalDrawable(color: Int): Drawable {
//            val shapeDrawable = ShapeDrawable(OvalShape())
//            shapeDrawable.paint.color = color
//            return shapeDrawable
//        }
//    }

}