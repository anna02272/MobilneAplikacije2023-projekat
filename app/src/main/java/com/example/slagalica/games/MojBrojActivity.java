package com.example.slagalica.games;

import static com.example.slagalica.MainActivity.socket;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.slagalica.MainActivity;

import com.example.slagalica.R;
import com.example.slagalica.config.SocketHandler;
import com.example.slagalica.game_helpers.DisableTouchActivity;
import com.example.slagalica.game_helpers.ShakeDetector;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.Scriptable;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import io.socket.emitter.Emitter;

public class MojBrojActivity extends AppCompatActivity {
    private List<Button> buttons;
    private FirebaseDatabase firebaseDatabase;
    private Map<Button, String> buttonSteps;
    private Random random;
    private int currentEnabledButtonIndex = 0;
    private String answer;
    private String finalAnswer;
    private Button stopButton;
    private Button buttonAnswer;
    private Button buttonAnswer2;
    private Button confirmButton;
    private CountDownTimer countDownTimer;
    private EditText input;
    private EditText input2;
    private ShakeDetector shakeDetector;
    private PlayersFragment playersFragment;
    private SharedPreferences preferences;
    private FirebaseUser currentUser;
    private DisableTouchActivity disableTouchActivity;
    private  JSONArray playingUsernamesArray;
    private  JSONArray playingSocketsArray;
    private  String currentPlayingUser;
    private int currentPlayingUserIndex;
    private int currentNotPlayingUserIndex;
    private String currentPlayingUserSocketId;
    private String currentNotPlayingUserSocketId;
    private String socketIdFromPlayerThatClicked;
    private int roundIndex;
    private int confirmClicked;
    private final int TOTAL_CONFIRM = 1;
    private int buttonId;
    private String number;
    private String SocketIdFromPlayerThatClicked;

    @Override
    public void onBackPressed() {
        return;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game_activity_moj_broj);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        disableTouchActivity = new DisableTouchActivity(MojBrojActivity.this);

        playersFragment = PlayersFragment.newInstance(61);
        playersFragment.setGameType("MojBroj");

        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_container, playersFragment)
                .commit();

        shakeDetector = new ShakeDetector();
        shakeDetector.setOnShakeListener(new ShakeDetector.OnShakeListener() {
            @Override
            public void onShake() {
                currentEnabledButtonIndex++;
                showButtons();
            }
        });

        buttonAnswer = findViewById(R.id.button_answer);
        buttonAnswer2 = findViewById(R.id.button_answer2);
        Button buttonEnd = findViewById(R.id.button_end);

        buttonEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playersFragment.showExitConfirmationDialog();
            }
        });

        confirmButton = findViewById(R.id.button_confirm);
        stopButton = findViewById(R.id.button_stop);

//        confirmButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (currentUser != null) {
//                    getPlayersSocketId(new OnSocketIdReceivedListener() {
//                        @Override
//                        public void onSocketIdReceived(String socketId) {
//                            SocketIdFromPlayerThatClicked = socketId;
//                            if (confirmClicked == TOTAL_CONFIRM) {
//                                Log.d("Here", "confirmClicked: " + confirmClicked);
//                                socket.emit("checkTwoAnswers");
//                            } else{
//                                socket.emit("incrementConfirmCount");
//                                Log.d("Here", "wait for other user to finish");
//                                // show toast to user that clicked button to
//                                // wait for other user to finish
//                            }
//                            Log.d("SocketIdFromPlayerThatClicked", "SocketIdFromPlayerThatClicked: " + socketId);
//                        }
//                    });
//                } else {
//                    checkAnswer();
//                }
//            }
//        });

        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentUser != null){
                    if (confirmClicked == TOTAL_CONFIRM) {
                        Log.d("Here", "confirmClicked: " + confirmClicked);
                        socket.emit("checkTwoAnswers");
                    } else{
                        socket.emit("incrementConfirmCount");
                        Log.d("Here", "wait for other user to finish");
                        // show toast to user that clicked button to
                        // wait for other user to finish
                    }
                } else {
                    checkAnswer();
                }
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentEnabledButtonIndex++;
                showButtons();
            }
        });

        input = findViewById(R.id.input1);
        input2 = findViewById(R.id.input2);

        if (currentUser == null) {
            input2.setVisibility(View.GONE);
        }

        Drawable clearDrawable = getResources().getDrawable(R.drawable.game_clear);

        clearDrawable.setBounds(0, 0, clearDrawable.getIntrinsicWidth(), clearDrawable.getIntrinsicHeight());
        input.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, clearDrawable, null);
        input2.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, clearDrawable, null);

        input.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (event.getX() >= (v.getWidth() - v.getPaddingRight() - clearDrawable.getIntrinsicWidth())) {
                        input.setText("");
                        enableAllButtons();
                        clickableButtons();
                        return true;
                    }
                }

                int inType = input.getInputType();
                input.setInputType(InputType.TYPE_NULL);
                input.onTouchEvent(event);
                input.setInputType(inType);
                return true;
            }
        });
        input2.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (event.getX() >= (v.getWidth() - v.getPaddingRight() - clearDrawable.getIntrinsicWidth())) {
                        input2.setText("");
                        enableAllButtons();
                        clickableButtons();
                        return true;
                    }
                }

                int inType = input2.getInputType();
                input2.setInputType(InputType.TYPE_NULL);
                input2.onTouchEvent(event);
                input2.setInputType(inType);
                return true;
            }
        });

        buttons = new ArrayList<>();
        buttons.add(findViewById(R.id.button_number1));
        buttons.add(findViewById(R.id.button_number2));
        buttons.add(findViewById(R.id.button_number3));
        buttons.add(findViewById(R.id.button_number4));
        buttons.add(findViewById(R.id.button_number5));
        buttons.add(findViewById(R.id.button_number6));
        buttons.add(findViewById(R.id.button_number7));
        buttons.add(findViewById(R.id.button_plus));
        buttons.add(findViewById(R.id.button_minus));
        buttons.add(findViewById(R.id.button_multiple));
        buttons.add(findViewById(R.id.button_divide));
        buttons.add(findViewById(R.id.button_open));
        buttons.add(findViewById(R.id.button_close));

        firebaseDatabase = FirebaseDatabase.getInstance();
        random = new Random();

        buttonSteps = new HashMap<>();

        retrieveNumbers();

        for (int i = 0; i < buttons.size(); i++) {
            final int buttonIndex = i;
            final Button button = buttons.get(i);

            button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentUser != null) {
                    getPlayersSocketId(new OnSocketIdReceivedListener() {
                        @Override
                        public void onSocketIdReceived(String socketId) {
                            SocketIdFromPlayerThatClicked = socketId;
                            Log.d("SocketIdFromPlayerThatClicked", "SocketIdFromPlayerThatClicked: " + socketId);
                            String currentInput = input.getText().toString();
                            String currentInput2 = input2.getText().toString();
                            if (buttonIndex != 6) {
                                String buttonText = buttonSteps.get(button);
                                currentInput = input.getText().toString();
                                socket.emit("input1Text", currentInput + buttonText, SocketIdFromPlayerThatClicked);
                                Log.d("SocketIdFromPlayerThatClicked", "SocketIdFromPlayerThatClicked1: " + SocketIdFromPlayerThatClicked);
                                currentInput2 = input2.getText().toString();
                                socket.emit("input2Text", currentInput2 + buttonText, SocketIdFromPlayerThatClicked);
                                Log.d("SocketIdFromPlayerThatClicked", "SocketIdFromPlayerThatClicked2: " + SocketIdFromPlayerThatClicked);
                            }

                            if (buttonIndex < 6 && button.isEnabled()) {
                                button.setEnabled(false);
                                disableNumberButtons();
                            } else if (buttonIndex >= 6) {
                                currentInput = input.getText().toString();
                                if (hasOperationInInput(currentInput)) {
                                    enablePreviouslyClickableButtons();
                                } else {
                                    disableNumberButtons();
                                }
                                    currentInput2 = input2.getText().toString();
                                    if (hasOperationInInput(currentInput2)) {
                                        enablePreviouslyClickableButtons();
                                    } else {
                                        disableNumberButtons();
                                    }
                            }
                        }
                    });
                } else {
                    String currentInput = input.getText().toString();
                    if (buttonIndex != 6) {
                        String buttonText = buttonSteps.get(button);
                        currentInput = input.getText().toString();
                        input.setText(currentInput + buttonText);
                    }
                    if (buttonIndex < 6 && button.isEnabled()) {
                        button.setEnabled(false);
                        disableNumberButtons();
                    } else if (buttonIndex >= 6) {
                        currentInput = input.getText().toString();
                        if (hasOperationInInput(currentInput)) {
                            enablePreviouslyClickableButtons();
                        } else {
                            disableNumberButtons();
                        }
                    }
                }
            }
        });

            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                     String currentInput = input.getText().toString();
                     String currentInput2 = input2.getText().toString();

                    if (buttonIndex != 6) {
                        String buttonText = buttonSteps.get(button);
                        if (currentUser != null) {
                            currentInput = input.getText().toString();
                            socket.emit("input1Text", currentInput + buttonText, SocketIdFromPlayerThatClicked);
                            Log.d("SocketIdFromPlayerThatClicked", "SocketIdFromPlayerThatClicked1: " + SocketIdFromPlayerThatClicked);
                            currentInput2 = input2.getText().toString();
                            socket.emit("input2Text", currentInput2 + buttonText, SocketIdFromPlayerThatClicked);
                         Log.d("SocketIdFromPlayerThatClicked", "SocketIdFromPlayerThatClicked2: " + SocketIdFromPlayerThatClicked);

                             } else {
                            currentInput = input.getText().toString();
                            input.setText(currentInput + buttonText);
                        }

                    }

                    if (buttonIndex < 6 && button.isEnabled()) {
                            button.setEnabled(false);
                        disableNumberButtons();
                    } else if (buttonIndex >= 6) {
                        currentInput = input.getText().toString();
                        if (hasOperationInInput(currentInput)) {
                            enablePreviouslyClickableButtons();
                        } else {
                            disableNumberButtons();
                        }
                        if (currentUser != null) {
                            currentInput2 = input2.getText().toString();
                            if (hasOperationInInput(currentInput2)) {
                                enablePreviouslyClickableButtons();
                            } else {
                                disableNumberButtons();
                            }
                        }
                    }
                }
            });
        }

        socket.on("updatePlayingUsers", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                if (args.length >= 2) {
                    playingUsernamesArray = (JSONArray) args[0];
                    playingSocketsArray = (JSONArray) args[1];
                    try {
                        retrieveConnectedUsers();
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        socket.on("touchDisabled", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        disableTouchActivity.disableTouch();
                    }
                });
            }
        });
        socket.on("touchEnabled", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        disableTouchActivity.enableTouch();
                    }
                });
            }
        });
        socket.on("inputGone", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        input.setVisibility(View.GONE);
                        input.setEnabled(false);
                        input2.setVisibility(View.VISIBLE);

                    }
                });
            }
        });
        socket.on("input2Gone", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        input.setVisibility(View.VISIBLE);
                        input2.setVisibility(View.GONE);
                        input2.setEnabled(false);
                    }
                });
            }
        });
        socket.on("startActivity", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(() -> {
                    Intent intent = new Intent(MojBrojActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                });
            }
        });
        socket.on("numberChange", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                int buttonId = (int) args[0];
                String number = (String) args[1];
                Button numButton = findViewById(buttonId);
                runOnUiThread(() -> {
                    buttonSteps.put(numButton, number);
                });
            }
        });
        socket.on("answerChange", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                String receivedAnswer = (String) args[0];
                String receivedAnswerFinal = (String) args[1];
                answer = receivedAnswer;
                finalAnswer = receivedAnswerFinal;
            }
        });
        socket.on("setButtonText", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                int buttonId = (int) args[0];
                String buttonText = (String) args[1];

                runOnUiThread(() -> {
                    Button button = findViewById(buttonId);
                    if (button != null) {
                        button.setText(buttonText);
                    }
                });

            }
        });
        socket.on("buttonEnabled", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                int buttonId = (int) args[0];
                boolean enabled = (boolean) args[1];

                runOnUiThread(() -> {
                    Button button = findViewById(buttonId);
                    if (button != null) {
                        button.setEnabled(enabled);
                    }
                });

            }
        });
        socket.on("inputText", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                String text = (String) args[0];
                runOnUiThread(() -> {
                    input.setText(text);
                    input2.setText(text);
                });
            }
        });
        socket.on("input1Text", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                String text = (String) args[0];
                runOnUiThread(() -> {
                    input.setText(text);
                });
            }
        });
        socket.on("input2Text", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                String text = (String) args[0];
                runOnUiThread(() -> {
                    input2.setText(text);
                });
            }
        });
        socket.on("endGame", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(() -> {
                    endGame();
                });
            }
        });
        socket.on("startNextGame", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(() -> {
                    try {
                        startNextGame();
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        });
        socket.on("checkTwoAnswers", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(() -> {
                    checkTwoAnswers();
                });
            }
        });
        socket.on("updateConfirmClicked", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                confirmClicked = (int) args[0];
            }
        });
        socket.on("updateRoundIndex", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                roundIndex = (int) args[0];
            }
        });
        socket.on("timerStarted", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (countDownTimer != null) {
                            countDownTimer.cancel();
                        }
                        countDownTimer = new CountDownTimer(61000, 10000) {
                            private Context context = MojBrojActivity.this.getApplicationContext();

                            @Override
                            public void onTick(long millisUntilFinished) {
                            }

                            @Override
                            public void onFinish() {
                                socket.emit("checkTwoAnswers");
                            }
                        };
                        countDownTimer.start();
                    }
                });
            }
        });
    }

    private void retrieveConnectedUsers() throws JSONException {
        if (playingUsernamesArray.length() >= 2) {
            currentPlayingUserIndex = (currentPlayingUserIndex) % playingUsernamesArray.length();
            currentPlayingUser = playingUsernamesArray.getString(currentPlayingUserIndex);
        }
        if (playingSocketsArray.length() >= 2) {
            currentNotPlayingUserIndex = (currentNotPlayingUserIndex + 1) % playingSocketsArray.length();
            currentNotPlayingUserSocketId = playingSocketsArray.getString(currentNotPlayingUserIndex );
            currentPlayingUserIndex = (currentPlayingUserIndex) % playingSocketsArray.length();
            currentPlayingUserSocketId = playingSocketsArray.getString(currentPlayingUserIndex );

            socket.emit("disableTouch", currentNotPlayingUserSocketId);
            socket.emit("timerStart", currentNotPlayingUserSocketId);
            JSONObject timerData = new JSONObject();
            try {
                timerData.put("duration", 60);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            socket.emit("startTimer", timerData);
            socket.emit("inputGone", currentNotPlayingUserSocketId);
            socket.emit("input2Gone", currentPlayingUserSocketId);

        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (currentPlayingUser != null) {
                    showToastAndEmit("Playing User: " + currentPlayingUser);
                }
            }
        });
    }
    private void showToastAndEmit(String message) {
        Toast.makeText(MojBrojActivity.this, message, Toast.LENGTH_SHORT).show();
        socket.emit("showToast", message);
    }
    private void disableNumberButtons() {
        for (int i = 0; i < 6; i++) {
            Button button = buttons.get(i);
            if (button.isClickable()) {
                    button.setClickable(false);
            }
        }
    }
    private void enablePreviouslyClickableButtons() {
        for (int i = 0; i < 6; i++) {
            Button button = buttons.get(i);
            if (!button.isClickable()) {
                    button.setClickable(true);
            }
        }
    }
    private void clickableButtons() {
        for (Button button : buttons) {
                button.setClickable(true);
        }
    }
    private boolean hasOperationInInput(String input) {
        String[] operations = { "+", "-", "*", "/", "(", ")" };
        for (String operation : operations) {
            if (input.contains(operation)) {
                return true;
            }
        }
        return false;
    }
    private void retrieveNumbers () {
            firebaseDatabase.getReference("moj_broj").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.hasChildren()) {
                        DataSnapshot operationsSnapshot = dataSnapshot.child("operations");

                        for (DataSnapshot operationSnapshot : operationsSnapshot.getChildren()) {
                            String operationName = operationSnapshot.getKey();
                            String operationSymbol = operationSnapshot.getValue(String.class);
                            Button operationButton = getButtonByName(operationName);
                            if (operationButton != null) {
                                operationButton.setText(operationSymbol);
                                buttonSteps.put(operationButton, operationSymbol);
                            }
                        }

                        List<DataSnapshot> stepSnapshots = new ArrayList<>();
                        for (DataSnapshot stepSnapshot : dataSnapshot.getChildren()) {
                            if (stepSnapshot.getKey().startsWith("numbers")) {
                                stepSnapshots.add(stepSnapshot);
                            }
                        }

                        if (!stepSnapshots.isEmpty()) {
                            int randomIndex = random.nextInt(stepSnapshots.size());
                            DataSnapshot randomStepSnapshot = stepSnapshots.get(randomIndex);
                            retrieveNumber(randomStepSnapshot);
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // Handle error
                }
            });
        }
    private void retrieveNumber(final DataSnapshot stepSnapshot) {
        final String numKey = stepSnapshot.getKey();
        currentEnabledButtonIndex = 0;
        firebaseDatabase.getReference("moj_broj/" + numKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Map<String, String> stepsMap = dataSnapshot.getValue(new GenericTypeIndicator<Map<String, String>>() {});
                if (stepsMap != null && !stepsMap.isEmpty()) {
                    for (int i = 1; i <= 7; i++) {
                        number = stepsMap.get("number" + i);
                        if (number != null) {
                            int buttonIndex = i - 1;
                            if (buttonIndex < buttons.size()) {
                                Button button = buttons.get(buttonIndex);
                                buttonId = button.getId();
                                buttonSteps.put(button, number);
                                if (currentUser != null) {
                                    socket.emit("numberChange", buttonId, number);
                                }
                            }
                        }
                    }
                    answer = stepsMap.get("number7");
                    finalAnswer = stepsMap.get("number8");
                    if (currentUser != null) {
                        socket.emit("answerChange", answer, finalAnswer);
                    }
                    if (currentEnabledButtonIndex > 0) {
                        showButtons();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle error
            }
        });
    }
    private void showButtons() {
        if (currentEnabledButtonIndex == 1) {
            Button button7 = buttons.get(6);
            String buttonText7 = buttonSteps.get(button7);
            if (currentUser != null) {
                int buttonId = button7.getId();
                socket.emit("setButtonText", buttonId, buttonText7);
            } else {
                button7.setText(buttonText7);
            }
        } else if (currentEnabledButtonIndex > 1 ) {
            Button button = buttons.get(currentEnabledButtonIndex-2);
            String buttonText = buttonSteps.get(button);
            if (currentUser != null) {
                int buttonId = button.getId();
                socket.emit("setButtonText", buttonId, buttonText);
            } else {
                button.setText(buttonText);
            }
            if (currentEnabledButtonIndex == 7) {
                if (currentUser != null) {
                    int buttonId = stopButton.getId();
                    socket.emit("buttonEnabled", buttonId, false);
                } else {
                    stopButton.setEnabled(false);
                }
            }
        }
    }
    private Button getButtonByName(String name) {
            for (Button button : buttons) {
                if (button.getResources().getResourceEntryName(button.getId()).equals("button_" + name)) {
                    return button;
                }
            }
            return null;
        }
    String getResult(String data){
        try{
            org.mozilla.javascript.Context context  = org.mozilla.javascript.Context.enter();
            context.setOptimizationLevel(-1);
            Scriptable scriptable = context.initStandardObjects();
            String finalResult =  context.evaluateString(scriptable,data,"Javascript",1,null).toString();
            if(finalResult.endsWith(".0")){
                finalResult = finalResult.replace(".0","");
            }
            return finalResult;
        }catch (Exception e){
            return "Err";
        }
    }
    private void checkAnswer() {
        String userInput = input.getText().toString().trim();
        if (userInput.isEmpty()) {
            return;
        }
        String finalResult = getResult(userInput);
        buttonAnswer.setText(finalResult);
        if (answer != null && finalResult.equals(answer)) {
            updatePoints(20);
            if(!finalResult.equals("Err")){
                Toast.makeText(MojBrojActivity.this,  finalResult + " :  Tacan odgovor!", Toast.LENGTH_SHORT).show();
            }

        } else {
            if (!finalResult.equals("Err")) {
                input.setText(finalAnswer);
                Toast.makeText(MojBrojActivity.this, finalResult + " :  Netacan odgovor!", Toast.LENGTH_SHORT).show();
            }
        }
        Toast.makeText(MojBrojActivity.this, "Kraj igre!", Toast.LENGTH_SHORT).show();
        confirmButton.setEnabled(false);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(MojBrojActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                startActivity(intent);

            }
        }, 5000);
    }
    private void checkTwoAnswers() {
        String userInput = input.getText().toString().trim();
        String userInput2 = input2.getText().toString().trim();
        Log.d("Here", "userInput: " + userInput);
        Log.d("Here", "userInput2: " + userInput2);

        if (userInput.isEmpty()) {
            return;
        }
        if (userInput2.isEmpty()) {
            return;
        }
        //Player 1
            String finalResult = getResult(userInput);
        Log.d("Here", "finalResult: " + finalResult);
            int buttonId = buttonAnswer.getId();
            socket.emit("setButtonText", buttonId, finalResult);

        //Player 2
        String finalResult2 = getResult(userInput2);
        Log.d("Here", "finalResult2: " + finalResult2);
            int buttonId1 = buttonAnswer2.getId();
            socket.emit("setButtonText", buttonId1, finalResult2);
//        if (answer != null && finalResult.equals(answer)) {
//                updatePoints(currentPlayingUserIndex + 1, 20);
//
//            if(!finalResult.equals("Err")) {
//                    showToastAndEmit(finalResult + " :  Tacan odgovor!");
//            }
//        } else {
//            if (!finalResult.equals("Err")) {
//                input.setText(finalAnswer);
//                input2.setText(finalAnswer);
//                    showToastAndEmit(finalResult + " :  Netacan odgovor!");
//            }
//            }
//            showToastAndEmit("Kraj igre!");
//            int buttonId2 = confirmButton.getId();
//            socket.emit("buttonEnabled", buttonId2, false);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                    socket.emit("incrementRoundIndex");
                    socket.emit("startNextGame");

            }
        }, 5000);
    }

    private void endGame(){
        socket.emit("inputText", finalAnswer);
        showToastAndEmit("Kraj igre!");
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (countDownTimer != null) {
                    countDownTimer.cancel();
                }
                socket.emit("startActivity");
                try {
                    preferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                    String username = preferences.getString("username", "");
                    socket.emit("playerDisconnected", new JSONObject().put("username", username));
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        }, 5000);
    }
    private void startNextGame() throws JSONException {
        if (roundIndex == 1) {
            if (playingUsernamesArray.length() > 0) {
                currentPlayingUserIndex = (currentPlayingUserIndex + 1) % playingUsernamesArray.length();
                currentPlayingUser = playingUsernamesArray.getString(currentPlayingUserIndex);
                showToastAndEmit("Playing User: " + currentPlayingUser);
            }
            if (playingSocketsArray.length() > 0) {
                currentNotPlayingUserIndex = (currentNotPlayingUserIndex + 1) % playingSocketsArray.length();
                currentPlayingUserIndex = (currentPlayingUserIndex) % playingSocketsArray.length();
                currentNotPlayingUserSocketId = playingSocketsArray.getString(currentNotPlayingUserIndex);
                currentPlayingUserSocketId = playingSocketsArray.getString(currentPlayingUserIndex);
                socket.emit("enableTouch", currentPlayingUserSocketId);
                socket.emit("disableTouch", currentNotPlayingUserSocketId);
            }

            input.setText("");
            input2.setText("");
            enableAllButtons();
            clickableButtons();
            removeButtonTextForAllButtons();
            JSONObject timerData = new JSONObject();
            try {
                timerData.put("duration", 60);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            socket.emit("startTimer", timerData);
            socket.emit("timerStart", currentPlayingUserSocketId);
            showToastAndEmit("Runda 1 je gotova! Pocinje nova runda.");
            retrieveNumbers();
            onResume();
            socket.emit("decrementConfirmCount");
        } else {
            socket.emit("endGame");
        }

    }
    private void updatePoints(int points) {
        playersFragment.updateGuestPoints(points);
    }
    private void updatePoints(int currentPlayerNumber, int points) {
        if (currentUser != null) {
            if (currentPlayerNumber > 0) {
                playersFragment.updatePlayerPoints(currentPlayerNumber, points);
            }
        }
    }

    private void enableAllButtons() {
        for (Button button : buttons) {
                button.setEnabled(true);
        }
    }
    private void setButtonTextForAllButtons() {
        for (int i = 0; i < buttons.size(); i++) {
            Button button = buttons.get(i);
            String buttonText = buttonSteps.get(button);
            if (currentUser != null) {
                int buttonId = button.getId();
                socket.emit("setButtonText", buttonId, buttonText);
            } else {
                button.setText(buttonText);
            }
        }
    }
    private void removeButtonTextForAllButtons() {
        for (int i = 0; i < buttons.size(); i++) {
            Button button = buttons.get(i);
             int buttonId = button.getId();
             socket.emit("setButtonText", buttonId, "");
        }
    }
    private void getPlayersSocketId(final OnSocketIdReceivedListener listener) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                 String serverUrl = "http://192.168.0.114:3000/getSocketId?socketId=" + SocketHandler.getSocket().id();
                try {
                    URL url = new URL(serverUrl);
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    try {
                        InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                        StringBuilder result = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            result.append(line);
                        }
                        return result.toString();
                    } finally {
                        urlConnection.disconnect();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(String socketId) {
                if (socketId != null) {
                    listener.onSocketIdReceived(socketId);
                    Log.d("Socket", "SocketIdFromPlayerThatClicked: " + socketId);
                } else {
                    Log.d("Socket", "Failed to retrieve socket ID");
                }
            }
        }.execute();
    }
    interface OnSocketIdReceivedListener {
        void onSocketIdReceived(String socketId);
    }
    private void startTimer() {
        if (currentUser == null) {
            if (countDownTimer != null) {
                countDownTimer.cancel();
            }
            countDownTimer = new CountDownTimer(61000, 10000) {
                private Context context = MojBrojActivity.this.getApplicationContext();

                @Override
                public void onTick(long millisUntilFinished) {

                }
                @Override
                public void onFinish() {
                    if (countDownTimer != null) {
                        countDownTimer.cancel();
                    }
                    input.setText(finalAnswer);
                    Toast.makeText(MojBrojActivity.this, "Vase vreme je isteklo, kraj igre",
                            Toast.LENGTH_SHORT).show();
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = new Intent(MojBrojActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    }, 5000);

                }
            };

            countDownTimer.start();
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(shakeDetector, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (currentUser != null) {
                    socket.emit("enableTouch", currentNotPlayingUserSocketId);
                }
                setButtonTextForAllButtons();
            }
        }, 5000);
        startTimer();
    }
    @Override
    protected void onPause() {
        super.onPause();
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorManager.unregisterListener(shakeDetector);
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }


}



