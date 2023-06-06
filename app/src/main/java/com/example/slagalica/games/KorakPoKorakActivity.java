package com.example.slagalica.games;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.example.slagalica.R;

public class KorakPoKorakActivity extends AppCompatActivity {
    private List<Button> buttons;
    private FirebaseDatabase firebaseDatabase;
    private Map<Button, String> buttonSteps;
    private Random random;
    private CountDownTimer countDownTimer;
    private int currentCount = 7;

    private String answer;
    private EditText input;
    private Handler buttonHandler;
    private Runnable buttonRunnable;
    private int currentEnabledButtonIndex = 0;
    private int currentButtonIndex = 1;

    @Override
    public void onBackPressed() {
        return;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game_activity_korak_po_korak);

        PlayersFragment playersFragment = PlayersFragment.newInstance(71);
        playersFragment.setGameType("KorakPoKorak");

        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_container, playersFragment)
                .commit();


        Button buttonNext = findViewById(R.id.button_next);
        input = findViewById(R.id.input);

        buttonNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(KorakPoKorakActivity.this, MojBrojActivity.class);
                startActivity(intent);
            }
        });


        Button confirmButton = findViewById(R.id.confirm);
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAnswer();
            }
        });


        buttons = new ArrayList<>();
        buttons.add(findViewById(R.id.button_1));
        buttons.add(findViewById(R.id.button_2));
        buttons.add(findViewById(R.id.button_3));
        buttons.add(findViewById(R.id.button_4));
        buttons.add(findViewById(R.id.button_5));
        buttons.add(findViewById(R.id.button_6));
        buttons.add(findViewById(R.id.button_7));
        buttons.add(findViewById(R.id.button_8));
        buttons.add(findViewById(R.id.button_9));
        buttons.add(findViewById(R.id.button_10));
        buttons.add(findViewById(R.id.button_11));
        buttons.add(findViewById(R.id.button_12));
        buttons.add(findViewById(R.id.button_13));
        buttons.add(findViewById(R.id.button_14));

        firebaseDatabase = FirebaseDatabase.getInstance();
        random = new Random();

        buttonSteps = new HashMap<>();

        retrieveSteps();

        for (int i = 1; i <= 6; i++) {
            Button button = buttons.get(i);
            button.setEnabled(false);
        }
        buttonHandler = new Handler();
        buttonRunnable = new Runnable() {
            @Override
            public void run() {
                Button button = buttons.get(currentButtonIndex);
                button.setEnabled(true);
                currentButtonIndex++;

                Button button1 = buttons.get(currentEnabledButtonIndex);
                String step = buttonSteps.get(button1);
                if (step != null) {
                    button1.setText(step);
                    button1.setEnabled(false);
                    currentEnabledButtonIndex++;
                }

                if (currentButtonIndex <= 7) {
                    buttonHandler.postDelayed(this, 10000);
                }
            }
        };

    }

    private void retrieveSteps() {
        firebaseDatabase.getReference("korak_po_korak").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChildren()) {
                    List<DataSnapshot> stepSnapshots = new ArrayList<>();
                    for (DataSnapshot stepSnapshot : dataSnapshot.getChildren()) {
                        if (stepSnapshot.getKey().startsWith("steps")) {
                            stepSnapshots.add(stepSnapshot);
                        }
                    }

                    if (!stepSnapshots.isEmpty()) {
                        int randomIndex = random.nextInt(stepSnapshots.size());
                        DataSnapshot randomStepSnapshot = stepSnapshots.get(randomIndex);
                        retrieveStep(randomStepSnapshot);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle error
            }
        });
    }


    private void retrieveStep(final DataSnapshot stepSnapshot) {
        final String stepKey = stepSnapshot.getKey();
        currentEnabledButtonIndex = 0;
        firebaseDatabase.getReference("korak_po_korak/" + stepKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Map<String, String> stepsMap = (Map<String, String>) dataSnapshot.getValue();
                if (stepsMap != null && !stepsMap.isEmpty()) {
                    for (int i = 1; i <= 7; i++) {
                        String step = stepsMap.get("step" + i);
                        if (step != null) {
                            int buttonIndex = i - 1;
                            if (buttonIndex < buttons.size()) {
                                Button button = buttons.get(buttonIndex);
                                buttonSteps.put(button, step);
                                setButtonListener(button);
                            }
                        }
                        answer = stepsMap.get("answer");
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle error
            }
        });
    }
    private void checkAnswer() {
        String userInput = input.getText().toString().trim();

        if (answer != null && userInput.equalsIgnoreCase(answer)) {
            Toast.makeText(KorakPoKorakActivity.this, "Tacan odgovor!", Toast.LENGTH_SHORT).show();
            for (Button button : buttons) {
                String step = buttonSteps.get(button);
                if (step != null) {
                    button.setText(step);
                    button.setEnabled(false);
                }
            }
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(input.getWindowToken(), 0);

            if (countDownTimer != null) {
                countDownTimer.cancel();
            }

            Toast.makeText(KorakPoKorakActivity.this, "Sledi igra MOJ BROJ!", Toast.LENGTH_SHORT).show();

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                   Intent intent = new Intent(KorakPoKorakActivity.this, MojBrojActivity.class);
                    startActivity(intent);
                    finish();
                }
            }, 5000);
        } else {
            Toast.makeText(KorakPoKorakActivity.this, "Netacan odgovor!", Toast.LENGTH_SHORT).show();
        }
    }




    private void setButtonListener(final Button button) {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String step = buttonSteps.get(button);
                if (step != null) {
                    button.setText(step);
                    button.setEnabled(false);
                }
            }
        });
    }

    private void startTimer() {
        countDownTimer = new CountDownTimer(71000, 10000) {
            private Context context = KorakPoKorakActivity.this.getApplicationContext();
            @Override
            public void onTick(long millisUntilFinished) {
                int time = (int) (millisUntilFinished / 1000);
                int buttonIndex = currentCount;
                if (buttonIndex >= 7 && buttonIndex < buttons.size()) {
                    Button button = buttons.get(buttonIndex);
                    button.setText(String.valueOf(time));
                }
                currentCount++;
            }

            @Override
            public void onFinish() {
                Toast.makeText(KorakPoKorakActivity.this, "Vase vreme je isteklo, sledi igra MOJ BROJ!",
                        Toast.LENGTH_SHORT).show(); ;
                Intent intent = new Intent(KorakPoKorakActivity.this, MojBrojActivity.class);
                startActivity(intent);

            }
        };

        countDownTimer.start();
    }


    @Override
    protected void onResume() {
        super.onResume();
        startTimer();
        buttonHandler.postDelayed(buttonRunnable, 10000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        buttonHandler.removeCallbacks(buttonRunnable);
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}