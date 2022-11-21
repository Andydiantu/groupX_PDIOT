package com.specknet.pdiotapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.royrodriguez.transitionbutton.TransitionButton;
import com.specknet.pdiotapp.live.LiveDataActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private final String TAG = this.getClass().getName();
    private TransitionButton transitionButton;
    private EditText username_view, password_view;
    private Button gotoSignup;

    private String passwordRecvFromDB = "";
    private boolean userExists = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        transitionButton = findViewById(R.id.login_button);
        username_view = findViewById(R.id.login_username);
        password_view = findViewById(R.id.login_password);
        gotoSignup = findViewById(R.id.login_signup);
        gotoSignup.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);

        transitionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start the loading animation when the user tap the button

                String username = username_view.getText().toString();
                String password = password_view.getText().toString();

                validation(username, password);


            }
        });

        gotoSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
                startActivity(intent);
            }
        });
    }

    private void validation(String username, String password) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        DocumentReference docRef = db.collection("Users").document(username);
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                        userExists = true;
                        passwordRecvFromDB = (String) document.getData().get("password");
                        loginLogic(username, password);
                    } else {
                        userExists = false;
                        Toast.makeText(getApplicationContext(), "User does not exist", Toast.LENGTH_SHORT).show();

                        Log.d(TAG, "No such document");
                    }
                } else {
                    userExists = false;
                    Toast.makeText(getApplicationContext(), "No connection", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    private void loginLogic(String username, String password){
        boolean isValid = passwordRecvFromDB.equals(password) && userExists;
        Log.d(TAG, passwordRecvFromDB);
        Log.d(TAG, String.valueOf(password));
        Log.d(TAG, "BISH:" + String.valueOf(passwordRecvFromDB.equals(password)));

        if (!isValid) {
            Toast.makeText(getApplicationContext(), "Wrong password", Toast.LENGTH_SHORT).show();
        } else {
            transitionButton.startAnimation();
            // Do networking task or background work here.
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    boolean isSuccessful = true;

                    // Choose a stop animation if your call was succesful or not
                    if (isSuccessful) {
                        transitionButton.stopAnimation(TransitionButton.StopAnimationStyle.EXPAND, new TransitionButton.OnAnimationStopEndListener() {
                            @Override
                            public void onAnimationStopEnd() {
                                Intent intent = new Intent(getBaseContext(), LiveDataActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                                Bundle bundle = new Bundle();
                                bundle.putString("username", username);
                                intent.putExtras(bundle);
                                startActivity(intent);
                            }
                        });
                    } else {
                        transitionButton.stopAnimation(TransitionButton.StopAnimationStyle.SHAKE, null);
                    }
                }
            }, 2000);
        }
    }

    //TODO
    private void storeUserInfo(String username, String password){
        // store username and password in the database
        Log.d(TAG, username+password);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // Create a new user with a first and last name
        Map<String, Object> user = new HashMap<>();
        user.put("username", username);
        user.put("password", password);

        // Add a new document with a generated ID
        db.collection("users")
                .add(user)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding document", e);
                    }
                });

        // Add a new document with a generated ID
        db.collection("Data")
                .add(user)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding document", e);
                    }
                });

        // CODE FOR CREATING SAMPLE DATA
        Map<String, Integer> activities = new HashMap<String, Integer>();
        activities.put("sitting", 0);
        activities.put("walking", 0);
        activities.put("running", 0);
        activities.put("lying down", 120);


        // CODE FOR GETTING DATE
        Date c = Calendar.getInstance().getTime();
        System.out.println("Current time => " + c);

        SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        String formattedDate = df.format(c);

        // CODE FOR SETTING DATA FOR DATE
        db.collection("Data").document(formattedDate)
                .set(activities)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "DocumentSnapshot added with ID: ");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding document", e);
                    }
                });



    }
}