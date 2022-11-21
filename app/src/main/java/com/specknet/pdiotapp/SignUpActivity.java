package com.specknet.pdiotapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.royrodriguez.transitionbutton.TransitionButton;
import com.specknet.pdiotapp.live.LiveDataActivity;

public class SignUpActivity extends AppCompatActivity {

    private final String TAG = this.getClass().getName();
    private TransitionButton transitionButton;
    private EditText username_view, password_view;
    private Button gotoLogin;
    private boolean isValid = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        Log.d(TAG, "Sign_up_activity");

        transitionButton = findViewById(R.id.signup_button);
        username_view = findViewById(R.id.signup_username);
        password_view = findViewById(R.id.signup_password);
        gotoLogin = findViewById(R.id.signup_login);
        gotoLogin.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);

        transitionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = username_view.getText().toString();
                String password = password_view.getText().toString();

                storeUserInfo(username, password);
                checkUserInfo(username, password);

                if(isValid){
                    Toast.makeText(getApplicationContext(),"Sign up successfully",Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(getApplicationContext(),"Sign up filed",Toast.LENGTH_SHORT).show();
                }

            }
        });

        gotoLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });
    }


    private void storeUserInfo(String username, String password){
        Log.d(TAG, username+password);
    }

    private void checkUserInfo(String username, String password){
        Log.d(TAG, username+password);
        isValid = true;
    }

}