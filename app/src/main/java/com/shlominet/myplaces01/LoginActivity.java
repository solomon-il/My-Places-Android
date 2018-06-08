package com.shlominet.myplaces01;

import android.app.Dialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.shlominet.utils.FirebaseUtil;

public class LoginActivity extends AppCompatActivity {

    private EditText userNameET, passwordET;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

        userNameET = findViewById(R.id.username_et);
        passwordET = findViewById(R.id.password_et);

        mAuth = FirebaseUtil.getAuth();
    }

    //click login button
    public void signinClick(View view) {
        String email = userNameET.getText().toString().trim();
        String password = passwordET.getText().toString();
        if(email.length() == 0 || password.length() == 0) {
            Toast.makeText(LoginActivity.this, "user name/ password is empty", Toast.LENGTH_SHORT).show();
            return;
        }
        else {
            signInUser(email, password);
        }
    }

    //signIn with email and password with Firebase
    private void signInUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Toast.makeText(LoginActivity.this, task.getException().getLocalizedMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    //click signUp button - will open dialog
    public void signupClick(View view) {

        showSignUpDialog();
    }

    //dialog for sign up with new email and password
    private void showSignUpDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_signup);
        dialog.setCanceledOnTouchOutside(true);
        final EditText userNameET, passwordET;
        userNameET = dialog.findViewById(R.id.username_signup_et);
        passwordET = dialog.findViewById(R.id.password_signup_et);
        Button signupBTN = dialog.findViewById(R.id.signup_btn);
        signupBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = userNameET.getText().toString().trim();
                String password = passwordET.getText().toString();
                if(email.length() == 0 || password.length() == 0) {
                    Toast.makeText(LoginActivity.this, "user name/ password is empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                else {
                    createUser(email, password);
                }
            }
        });
        dialog.show();
    }

    //create new user with email and password with Firebase
    private void createUser(String email, final String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            FirebaseUser user = mAuth.getCurrentUser();
                            addUser(user, password);
                            updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Toast.makeText(LoginActivity.this, task.getException().getLocalizedMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    //add the new user to database
    private void addUser(FirebaseUser user, String password) {
        FirebaseDatabase database = FirebaseUtil.getDatabase();
        DatabaseReference usersRef = database.getReference("users/" + user.getUid());
        usersRef.child("email").setValue(user.getEmail());
        usersRef.child("password").setValue(password);
    }

    //for auto log-in
    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            updateUI(currentUser);
        }
    }

    //after log-in, intent to Main Activity with the current user
    private void updateUI(FirebaseUser currentUser) {
        if(currentUser != null) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("userEmail", currentUser.getEmail());
            intent.putExtra("userUid", currentUser.getUid());
            startActivity(intent);
            finish();
        }
    }
}
