package com.redhotapp.longtapoverlay

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import org.jetbrains.anko.longToast

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cmdTurnCursorServiceOn()
    }


    private fun cmdTurnCursorServiceOn() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (!Settings.canDrawOverlays(this)) {
                try {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + this.getPackageName())
                    )
                    startActivityForResult(intent, 1234)
                } catch (e: Exception) {
                }
            } else {
                startService(Intent(this, LongTapService::class.java))

            }
        } else {
            startService(Intent(this, LongTapService::class.java))
        }

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // Check which request we're responding to
        if(intent != null)
        if (requestCode == 1234) {
            // Make sure the request was successful
            if (resultCode == Activity.RESULT_OK) {
                startService(Intent(this, LongTapService::class.java))

            }else{
                longToast(R.string.overlay_was_denied)
            }
        }
    }
}
