package com.techmania.quizgame

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.techmania.quizgame.databinding.ActivityResultBinding

class ResultActivity : AppCompatActivity() {

    private lateinit var resultBinding: ActivityResultBinding
    private lateinit var databaseReference: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        resultBinding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(resultBinding.root)

        auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        databaseReference = FirebaseDatabase.getInstance().reference.child("scores")

        if (user != null) {
            val userUID = user.uid

            // Read scores
            databaseReference.child(userUID)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val correct = snapshot.child("correct").value?.toString() ?: "0"
                        val wrong = snapshot.child("wrong").value?.toString() ?: "0"

                        resultBinding.textViewScoreCorrect.text = correct
                        resultBinding.textViewScoreWrong.text = wrong
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(applicationContext, "Failed to load score", Toast.LENGTH_SHORT).show()
                    }
                })
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
        }

        resultBinding.buttonPlayAgain.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        resultBinding.buttonExit.setOnClickListener {
            finishAffinity() // Close the app
        }
    }
}
