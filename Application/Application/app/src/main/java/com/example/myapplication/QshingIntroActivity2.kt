package com.example.myapplication

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class QshingIntroActivity2 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.qshing_info2)

        val nextButton = findViewById<Button>(R.id.nextButton)
        nextButton.setOnClickListener {

            val tutorialIntent = Intent(this, TutorialActivity::class.java)
            startActivity(tutorialIntent)
            finish()
        }
    }
}
