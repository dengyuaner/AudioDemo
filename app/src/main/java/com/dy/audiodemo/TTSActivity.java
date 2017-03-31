package com.dy.audiodemo;

import android.content.Intent;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatSpinner;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

import java.util.Locale;

public class TTSActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {
    private TextToSpeech mTextToSpeech;
    private static final String TAG = "TTS";
    private AppCompatButton btnSpeak;
    private AppCompatEditText etContent;
    private AppCompatSpinner spinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tts);

        btnSpeak = (AppCompatButton) findViewById(R.id.btnSpeak);
        etContent = (AppCompatEditText) findViewById(R.id.etContent);
        spinner = (AppCompatSpinner) findViewById(R.id.spinner);

        Intent intent = new Intent();
        intent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(intent, 1);

        btnSpeak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mTextToSpeech.speak(etContent.getText().toString(), TextToSpeech.QUEUE_ADD, null, null);
                } else {
                    mTextToSpeech.speak(etContent.getText().toString(), TextToSpeech.QUEUE_ADD, null);
                }
            }
        });
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                int result = 0;
                switch (position) {
                    case 0:
                        result = mTextToSpeech.setLanguage(Locale.US);
                        break;
                    case 1:
                        result = mTextToSpeech.setLanguage(Locale.FRENCH);
                        break;
                    case 2:
                        result = mTextToSpeech.setLanguage(Locale.GERMAN);
                        break;
                    case 3:
                        result = mTextToSpeech.setLanguage(Locale.ITALIAN);
                        break;
                    default:

                        break;
                }
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                        result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.v(TAG, "不支持");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                mTextToSpeech = new TextToSpeech(this, this);
            } else {
                Intent intent = new Intent();
                intent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(intent);
            }
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = mTextToSpeech.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.v(TAG, "不可用的语言");
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mTextToSpeech.speak("init success", TextToSpeech.QUEUE_ADD, null, null);
                } else {
                    mTextToSpeech.speak("init success", TextToSpeech.QUEUE_ADD, null);
                }
            }
        }
    }


}
