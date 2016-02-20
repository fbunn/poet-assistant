/*
 * Copyright (c) 2016 Carmen Alvarez
 *
 * This file is part of Poet Assistant.
 *
 * Poet Assistant is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Poet Assistant is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Poet Assistant.  If not, see <http://www.gnu.org/licenses/>.
 */

package ca.rmen.android.poetassistant.main.reader;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import ca.rmen.android.poetassistant.Constants;
import ca.rmen.android.poetassistant.R;
import ca.rmen.android.poetassistant.Tts;


public class ReaderFragment extends Fragment implements
        PoemFile.PoemFileCallback {
    private static final String TAG = Constants.TAG + ReaderFragment.class.getSimpleName();
    private static final String EXTRA_INITIAL_TEXT = "initial_text";
    private static final int ACTION_FILE_OPEN = 0;
    private static final int ACTION_FILE_SAVE_AS = 1;

    private Tts mTts;
    private ImageView mPlayButton;
    private EditText mTextView;
    private Handler mHandler;
    private PoemPrefs mPoemPrefs;

    public static ReaderFragment newInstance(String initialText) {
        Log.d(TAG, "newInstance() called with: " + "initialText = [" + initialText + "]");
        ReaderFragment fragment = new ReaderFragment();
        fragment.setRetainInstance(true);
        Bundle bundle = new Bundle(1);
        bundle.putString(EXTRA_INITIAL_TEXT, initialText);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreate() called with: " + "savedInstanceState = [" + savedInstanceState + "]");
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mPoemPrefs = new PoemPrefs(getActivity());
        mTts = Tts.getInstance(getActivity());
        EventBus.getDefault().register(this);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "onActivityCreated() called with: " + "savedInstanceState = [" + savedInstanceState + "]");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView() called with: " + "inflater = [" + inflater + "], container = [" + container + "], savedInstanceState = [" + savedInstanceState + "]");
        View view = inflater.inflate(R.layout.fragment_reader, container, false);
        mPlayButton = (ImageView) view.findViewById(R.id.btn_play);
        mTextView = (EditText) view.findViewById(R.id.tv_text);
        mPlayButton.setOnClickListener(mOnClickListener);
        mTextView.addTextChangedListener(mTextWatcher);
        mHandler = new Handler();
        loadPoem();
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        Log.d(TAG, "onCreateOptionsMenu() called with: " + "menu = [" + menu + "], inflater = [" + inflater + "]");
        inflater.inflate(R.menu.menu_tts, menu);
        menu.findItem(R.id.action_save).setEnabled(mPoemPrefs.hasSavedPoem());
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            menu.findItem(R.id.action_new).setTitle(R.string.file_clear);
            menu.findItem(R.id.action_open).setVisible(false);
            menu.findItem(R.id.action_save).setVisible(false);
            menu.findItem(R.id.action_save_as).setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_new) {
            mPoemPrefs.clear();
            mTextView.setText("");
        } else if (item.getItemId() == R.id.action_open) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) open();
        } else if (item.getItemId() == R.id.action_save) {
            PoemFile poemFile = mPoemPrefs.getSavedPoem();
            PoemFile.save(getActivity(), poemFile.uri, mTextView.getText().toString(), this);
        } else if (item.getItemId() == R.id.action_save_as) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) saveAs();
        } else if (item.getItemId() == R.id.action_share) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_TEXT, mTextView.getText().toString());
            intent.setType("text/plain");
            startActivity(Intent.createChooser(intent, getString(R.string.file_share)));
        }
        return true;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void open() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        PoemFile poemFile = mPoemPrefs.getSavedPoem();
        if (poemFile != null) intent.setData(poemFile.uri);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        startActivityForResult(intent, ACTION_FILE_OPEN);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void saveAs() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        PoemFile poemFile = mPoemPrefs.getSavedPoem();
        if (poemFile != null) intent.putExtra(Intent.EXTRA_TITLE, poemFile.name);
        startActivityForResult(intent, ACTION_FILE_SAVE_AS);
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause() called with: " + "");
        mPoemPrefs.updatePoemText(mTextView.getText().toString());
        super.onPause();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy() called with: " + "");
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult() called with: " + "requestCode = [" + requestCode + "], resultCode = [" + resultCode + "], data = [" + data + "]");
        if (requestCode == ACTION_FILE_OPEN && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                PoemFile.open(getActivity(), uri, this);
            }
        } else if (requestCode == ACTION_FILE_SAVE_AS && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                PoemFile.save(getActivity(), uri, mTextView.getText().toString(), this);
            }
        }
    }

    public void speak(String text) {
        Log.d(TAG, "speak() called with: " + "text = [" + text + "]");
        mTextView.setText(text);
        mPlayButton.callOnClick();
    }

    @Override
    public void onPoemLoaded(PoemFile poemFile) {
        Log.d(TAG, "onPoemLoaded() called with: " + "poemFile = [" + poemFile + "]");
        if (poemFile == null) {
            mPoemPrefs.clear();
            Snackbar.make(mTextView, getString(R.string.file_opened_error), Snackbar.LENGTH_LONG).show();
        } else {
            mTextView.setText(poemFile.text);
            mPoemPrefs.setSavedPoem(poemFile);
            getActivity().supportInvalidateOptionsMenu();
            Snackbar.make(mTextView, getString(R.string.file_opened, poemFile.name), Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onPoemSaved(PoemFile poemFile) {
        if (poemFile == null) {
            Snackbar.make(mTextView, getString(R.string.file_saved_error), Snackbar.LENGTH_LONG).show();
        } else {
            Log.d(TAG, "onPoemSaved() called with: " + "poemFile = [" + poemFile + "]");
            mPoemPrefs.setSavedPoem(poemFile);
            Snackbar.make(mTextView, getString(R.string.file_saved, poemFile.name), Snackbar.LENGTH_LONG).show();
        }
    }

    /**
     * The button shall be disabled if TTS isn't initialized, or if there is no text to play.
     * The button should display a "Play" icon if TTS isn't running but can be started.
     * The button should display a "Stop" icon if TTS is currently running.
     * This is called from a background thread by TTS.
     */
    private void updatePlayButton() {
        Log.d(TAG, "updatePlayButton");
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                boolean enabled = !TextUtils.isEmpty(mTextView.getText())
                        && mTts.getStatus() == TextToSpeech.SUCCESS;
                mPlayButton.setEnabled(enabled);
                if (mTts.isSpeaking()) {
                    mPlayButton.setImageResource(R.drawable.ic_stop);
                } else {
                    mPlayButton.setImageResource(R.drawable.ic_play);
                }
            }
        });
    }

    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.v(TAG, "Play button clicked");
            if (mTts.isSpeaking()) mTts.stop();
            else speak();

            updatePlayButton();
        }
    };

    /**
     * Read the text in our text view.
     */
    private void speak() {
        String text = mTextView.getText().toString();
        int startPosition = mTextView.getSelectionStart();
        if (startPosition == text.length()) startPosition = 0;
        int endPosition = mTextView.getSelectionEnd();
        if (startPosition == endPosition) endPosition = text.length();
        text = text.substring(startPosition, endPosition);
        mTts.speak(text);
    }

    private void loadPoem() {
        Log.d(TAG, "loadPoem() called with: " + "");
        // First see if we have poem in the arguments
        // (the user chose to share some text with our app)
        Bundle arguments = getArguments();
        if (arguments != null) {
            String initialText = arguments.getString(EXTRA_INITIAL_TEXT);
            if (!TextUtils.isEmpty(initialText)) {
                mTextView.setText(initialText);
                PoemFile poemFile = new PoemFile(null, null, initialText);
                mPoemPrefs.setSavedPoem(poemFile);
                if (mTts.getStatus() == TextToSpeech.SUCCESS) {
                    speak();
                }
                getActivity().supportInvalidateOptionsMenu();
                return;
            }
        }
        // Load the poem we previously saved
        if (mPoemPrefs.hasSavedPoem()) {
            PoemFile poemFile = mPoemPrefs.getSavedPoem();
            PoemFile.open(getActivity(), poemFile.uri, this);
        } else if (mPoemPrefs.hasTempPoem()) {
            String tempPoemText = mPoemPrefs.getTempPoem();
            mTextView.setText(tempPoemText);
        }
    }

    private final TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            updatePlayButton();
        }
    };

    @Subscribe
    public void onTtsInitialized(Tts.OnTtsInitialized event) {
        Log.d(TAG, "onTtsInitialized() called with: " + "event = [" + event + "]");
        updatePlayButton();
        if (event.status == TextToSpeech.SUCCESS) {
            loadPoem();
        }
    }

    @Subscribe
    public void onTtsUtteranceCompleted(Tts.OnUtteranceCompleted event) {
        Log.d(TAG, "onTtsUtteranceCompleted() called with: " + "event = [" + event + "]");
        updatePlayButton();
    }

}