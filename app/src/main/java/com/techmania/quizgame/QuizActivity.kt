package com.techmania.quizgame

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.techmania.quizgame.databinding.ActivityQuizBinding
import kotlin.random.Random

class QuizActivity : AppCompatActivity() {

    lateinit var quizBinding: ActivityQuizBinding

    // ✅ FIXED ORDER: Declare database before using it
    val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    val databaseReference: DatabaseReference = database.reference.child("questions")

    var question = ""
    var answerA = ""
    var answerB = ""
    var answerC = ""
    var answerD = ""
    var correctAnswer = ""
    var questionCount = 0
    var questionNumber = 0

    var userAnswer = ""
    var userCorrect = 0
    var userWrong = 0

    lateinit var timer: CountDownTimer
    private val totalTime = 25000L
    var timerContinue = false
    var leftTime = totalTime

    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser
    val scoreRef = database.reference

    val questions = HashSet<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        quizBinding = ActivityQuizBinding.inflate(layoutInflater)
        val view = quizBinding.root
        setContentView(view)

        do {
            val number = Random.nextInt(1, 11)
            questions.add(number)
        } while (questions.size < 5)

        gameLogic()

        quizBinding.buttonNext.setOnClickListener {
            resetTimer()
            gameLogic()
        }

        quizBinding.buttonFinish.setOnClickListener {
            sendScore()
        }

        quizBinding.textViewA.setOnClickListener {
            pauseTimer()
            userAnswer = "a"
            evaluateAnswer("a", quizBinding.textViewA)
        }

        quizBinding.textViewB.setOnClickListener {
            pauseTimer()
            userAnswer = "b"
            evaluateAnswer("b", quizBinding.textViewB)
        }

        quizBinding.textViewC.setOnClickListener {
            pauseTimer()
            userAnswer = "c"
            evaluateAnswer("c", quizBinding.textViewC)
        }

        quizBinding.textViewD.setOnClickListener {
            pauseTimer()
            userAnswer = "d"
            evaluateAnswer("d", quizBinding.textViewD)
        }
    }

    private fun gameLogic() {
        restoreOptions()

        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                questionCount = snapshot.childrenCount.toInt()

                if (questionNumber < questions.size) {
                    val questionId = questions.elementAt(questionNumber).toString()
                    val questionSnapshot = snapshot.child(questionId)

                    question = questionSnapshot.child("q").value.toString()
                    answerA = questionSnapshot.child("a").value.toString()
                    answerB = questionSnapshot.child("b").value.toString()
                    answerC = questionSnapshot.child("c").value.toString()
                    answerD = questionSnapshot.child("d").value.toString()
                    correctAnswer = questionSnapshot.child("answer").value.toString()

                    quizBinding.textViewQuestion.text = question
                    quizBinding.textViewA.text = answerA
                    quizBinding.textViewB.text = answerB
                    quizBinding.textViewC.text = answerC
                    quizBinding.textViewD.text = answerD

                    quizBinding.progressBarQuiz.visibility = View.GONE
                    quizBinding.linearLayoutInfo.visibility = View.VISIBLE
                    quizBinding.linearLayoutQuestion.visibility = View.VISIBLE
                    quizBinding.linearLayoutButtons.visibility = View.VISIBLE

                    startTimer()
                } else {
                    showEndDialog()
                }

                questionNumber++
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(applicationContext, error.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun evaluateAnswer(answer: String, view: View) {
        if (correctAnswer == answer) {
            view.setBackgroundColor(Color.GREEN)
            userCorrect++
            quizBinding.textViewCorrect.text = userCorrect.toString()
        } else {
            view.setBackgroundColor(Color.RED)
            userWrong++
            quizBinding.textViewWrong.text = userWrong.toString()
            showCorrectAnswer()
        }
        disableOptions()
    }

    private fun showCorrectAnswer() {
        when (correctAnswer) {
            "a" -> quizBinding.textViewA.setBackgroundColor(Color.GREEN)
            "b" -> quizBinding.textViewB.setBackgroundColor(Color.GREEN)
            "c" -> quizBinding.textViewC.setBackgroundColor(Color.GREEN)
            "d" -> quizBinding.textViewD.setBackgroundColor(Color.GREEN)
        }
    }

    private fun disableOptions() {
        quizBinding.textViewA.isClickable = false
        quizBinding.textViewB.isClickable = false
        quizBinding.textViewC.isClickable = false
        quizBinding.textViewD.isClickable = false
    }

    private fun restoreOptions() {
        quizBinding.textViewA.setBackgroundColor(Color.WHITE)
        quizBinding.textViewB.setBackgroundColor(Color.WHITE)
        quizBinding.textViewC.setBackgroundColor(Color.WHITE)
        quizBinding.textViewD.setBackgroundColor(Color.WHITE)

        quizBinding.textViewA.isClickable = true
        quizBinding.textViewB.isClickable = true
        quizBinding.textViewC.isClickable = true
        quizBinding.textViewD.isClickable = true
    }

    private fun startTimer() {
        timer = object : CountDownTimer(leftTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                leftTime = millisUntilFinished
                updateTimerUI()
            }

            override fun onFinish() {
                disableOptions()
                resetTimer()
                updateTimerUI()
                quizBinding.textViewQuestion.text = "Time's up! Moving to next question."
                timerContinue = false
            }
        }.start()

        timerContinue = true
    }

    private fun pauseTimer() {
        timer.cancel()
        timerContinue = false
    }

    private fun resetTimer() {
        pauseTimer()
        leftTime = totalTime
        updateTimerUI()
    }

    private fun updateTimerUI() {
        quizBinding.textViewTime.text = (leftTime / 1000).toString()
    }

    private fun sendScore() {
        user?.let {
            val uid = it.uid
            scoreRef.child("scores").child(uid).child("correct").setValue(userCorrect)
            scoreRef.child("scores").child(uid).child("wrong").setValue(userWrong).addOnSuccessListener {
                Toast.makeText(applicationContext, "Score uploaded!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this@QuizActivity, ResultActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun showEndDialog() {
        AlertDialog.Builder(this@QuizActivity)
            .setTitle("Quiz Completed")
            .setMessage("You've finished the quiz!\nSee result or play again?")
            .setCancelable(false)
            .setPositiveButton("See Result") { _, _ -> sendScore() }
            .setNegativeButton("Play Again") { _, _ ->
                startActivity(Intent(this@QuizActivity, MainActivity::class.java))
                finish()
            }
            .show()
    }
}
