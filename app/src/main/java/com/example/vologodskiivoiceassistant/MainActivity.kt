package com.example.vologodskiivoiceassistant

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.SimpleAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.vologodskiivoiceassistant.R.menu.toolbar_menu
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.analytics.FirebaseAnalytics
import com.wolfram.alpha.WAEngine
import com.wolfram.alpha.WAPlainText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.HashMap

open class MainActivity : AppCompatActivity() {

    lateinit var requestInput: TextInputEditText

    lateinit var podsAdapter: SimpleAdapter

    lateinit var progressBar: ProgressBar

    lateinit var waEngine: WAEngine

    private lateinit var analytics: FirebaseAnalytics

    val TAG = "test_deeplink"
    val wolfram_api_key = "E4W8U4-P4GAQ2RQTH"

    //request code for voice input
    val VOICE_RECOGNITION_REQUEST_CODE: Int = 959

    val pods = mutableListOf<HashMap<String, String>>()

    lateinit var textToSpeech: TextToSpeech

    //flag of initialization of tts
    var isTtsReady: Boolean = false

        //test data
/*        HashMap<String, String>().apply {
            put("Title", "Title 1")
            put("Content", "Content 1")
        },

        HashMap<String, String>().apply {
            put("Title", "Title 2")
            put("Content", "Content 2")
        },

        HashMap<String, String>().apply {
            put("Title", "Title 3")
            put("Content", "Content 3")
        },

        HashMap<String, String>().apply {
            put("Title", "Title 4")
            put("Content", "Content 4")
        }*/

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "start of onCreate function")

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initViews()
        initWolframEngine()
        initTts()

    }

    private fun initViews() {
        analytics = FirebaseAnalytics.getInstance(this)
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        //text input
        requestInput = findViewById(R.id.text_input_edit)
        //listen to action of press button keyboard
        requestInput.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                //clean the list
                pods.clear()
                //refresh adapter for cleaning previous answers
                podsAdapter.notifyDataSetChanged()

                //get variable with question
                val question = requestInput.text.toString()
                askWolfram(question)
            }

            //false - hide keyboard after pressing button
            return@setOnEditorActionListener false
        }


        val podlist: ListView = findViewById(R.id.pods_list)
        podsAdapter = SimpleAdapter(
            applicationContext,
            pods,
            R.layout.item_pod,
            arrayOf("Title", "Content"),
            intArrayOf(R.id.title, R.id.content)
        )

        podlist.adapter = podsAdapter
        //method called wher any element is clicked. For action processing
        podlist.setOnItemClickListener { parent, view, position, id ->
            //if tts is ready get the title and content from pods list
            if (isTtsReady) {
                //getting title, content on position of pods list
                val title = pods[position]["Title"]
                val content = pods[position]["Content"]
                    //playing content, flag for setting playing to queue, no params, title for identify rows
                textToSpeech.speak(content, TextToSpeech.QUEUE_FLUSH, null, title)
            }
        }


        val voiceInputButton: FloatingActionButton = findViewById(R.id.voice_input_button)
        voiceInputButton.setOnClickListener{
            //clear pods list and shake adapter for clearing list of object
            pods.clear()
            podsAdapter.notifyDataSetChanged()

            //stop tts so it doesn't interrupt voice input
            if(isTtsReady) {
                textToSpeech.stop()
            }

            showVoiceInputDialog()

            analytics.logEvent("FloatingActionButton_clicked", null)
        }

        progressBar = findViewById(R.id.progress_bar)
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(toolbar_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.action_stop -> {
                //if tts s ready stopping it
                if (isTtsReady) {
                    textToSpeech.stop()
                }
                return true
            }
            R.id.action_clear -> {
                //clear text field
                requestInput.text?.clear()
                //clear the list
                pods.clear()
                //refresh adapter
                podsAdapter.notifyDataSetChanged()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onNewIntent(intent: Intent) {
        Log.d(TAG, "$intent.data")
        super.onNewIntent(intent)
        handleIntent(intent)
        Log.d(TAG, "$intent.data")

    }
    private fun handleIntent(Intent: Intent) {
        val intent = intent
        val action: String? = intent?.action
        val data: Uri? = intent?.data
        Log.d(TAG, "$intent.data")

    }

    fun initWolframEngine() {
        waEngine = WAEngine().apply {
            appID = wolfram_api_key
            addFormat("plaintext")
        }
    }

    fun showSnackbar(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_INDEFINITE).apply {
            setAction(android.R.string.ok) {
                dismiss()
            }
            show()
        }
    }

    fun askWolfram(request: String) {
        //show progressBar
        progressBar.visibility = View.VISIBLE

        //to IO
        CoroutineScope(Dispatchers.IO).launch {
            val query = waEngine.createQuery().apply { input = request }
            //execute runCatching with query
            kotlin.runCatching {
                waEngine.performQuery(query)
            }.onSuccess { result ->
                withContext(Dispatchers.Main) {
                    //Switch to the main thread and snackBar hiding
                    progressBar.visibility = View.GONE
                    if (result.isError) {
                        //snackBar with error
                    showSnackbar(result.errorMessage)
                    return@withContext
                    }

                    if (!result.isSuccess) {
                        //paint input text
                        requestInput.error = getString(R.string.error_do_not_understand)
                        return@withContext
                    }

                    //fill the list of pods
                    for (pod in result.pods) {
                        if (pod.isError) continue
                        val content = StringBuilder()
                        for (subpod in pod.subpods) {
                            for (element in subpod.contents) {
                                if (element is WAPlainText) {
                                    content.append(element.text)
                                }
                            }
                        }
                        pods.add(0, HashMap<String, String>().apply {
                            put("Title", pod.title)
                            put("Content", content.toString())
                        })
                    }

                    //refresh adapter
                    podsAdapter.notifyDataSetChanged()
                }
            }.onFailure { t ->
                ////Switch to the main thread and snackBar hiding
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        //show error message with elvis operator: if there is no error show prepared error message
                        showSnackbar(t.message ?:getString(R.string.error_something_went_wrong))
                    }
                }
            }
        }

    //TTS initialization
    fun initTts() {
        textToSpeech = TextToSpeech(this) {code ->
            //if initialization is not successful
            if (code !=TextToSpeech.SUCCESS) {
                //show snackbar with error message
                Log.e(TAG, "error code: $code")
                showSnackbar(getString(R.string.error_tts_is_not_ready))
                //switch the flag of initialization
            } else {
                isTtsReady = true
            }
        }
        textToSpeech.language = Locale.US
    }

    fun showVoiceInputDialog() {
        //Intent for request to Android. Apply for customization intent
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {

            //add free speech model
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            //hint for voice recognition window
            putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.request_hint))
            //Choose English language
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US)
        }

        runCatching {
            //start activity of another app using intent
            startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE)
        }.onFailure { t ->
            //processing the absence of voice recognition. Error message from answer or message from project's resource
            showSnackbar(t.message?: getString(R.string.error_voice_recognition_unavailable))
        }
    }

    //override of system method and sending data
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //check request code and successful result code
        if (requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == RESULT_OK) {
            //intent with data. Take the data for key
            data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0)?.let { question ->
                //set in requestInput as a text
                requestInput.setText(question)
                askWolfram(question)
            }
        }
    }
}