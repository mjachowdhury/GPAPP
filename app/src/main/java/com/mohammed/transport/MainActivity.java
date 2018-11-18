package com.mohammed.transport;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private static final int RC_SIGN_IN = 123;
    private Button mDriver, mCustomer;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDriver = findViewById(R.id.driver);
        mCustomer = findViewById(R.id.customer);

        //setting onclick listener for driver and customer
        startService(new Intent(MainActivity.this, onAppKilled.class));
        mDriver.setOnClickListener(v -> startActivityFinishCurrentActivity(MapActivity.class));

        mCustomer.setOnClickListener(v -> signInFirebaseUI());
    }

    private void startActivityFinishCurrentActivity(Class activity) {
        Intent intent = new Intent(MainActivity.this, activity);
        startActivity(intent);
        finish();
    }

    private void signInFirebaseUI() {
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(Arrays.asList(
                                new AuthUI.IdpConfig.GoogleBuilder().build(),
                                new AuthUI.IdpConfig.FacebookBuilder().build(),
                                new AuthUI.IdpConfig.EmailBuilder().build()))
                        .setIsSmartLockEnabled(false)
                        .setTheme(R.style.AppTheme)
                        .build(),
                RC_SIGN_IN);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) { // Successfully signed in
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (resultCode == RESULT_OK) {
                mAuth = FirebaseAuth.getInstance();
                FirebaseUser currentUser = mAuth.getCurrentUser();
                if (currentUser != null) {
                    addUserToFirebase(mAuth.getCurrentUser());
                    startActivityFinishCurrentActivity(MapActivity.class);
                } else {
                    Toast.makeText(this, R.string.fetch_firebase_failed, Toast.LENGTH_SHORT).show();
                }

            } else {// Sign in failed
                if (response == null) {// User pressed back button
                    Toast.makeText(this, R.string.sign_in_cancelled, Toast.LENGTH_SHORT).show();
                    return;
                }

                if (Objects.requireNonNull(response.getError()).getErrorCode() == ErrorCodes.NO_NETWORK) {
                    Toast.makeText(this, R.string.no_internet_connection, Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(this, R.string.unknown_error, Toast.LENGTH_SHORT).show();
                Log.e("MAINACTIVITY", "Sign-in error: ", response.getError());
            }
        }
    }

    /**
     * @param firebaseUser
     * add a New firebase User
     */
    private void addUserToFirebase(FirebaseUser firebaseUser) {
        String userID = firebaseUser.getUid();
        Uri userIcon = firebaseUser.getPhotoUrl();
        HashMap<String, String> map = new HashMap<>();
        map.put("name", firebaseUser.getDisplayName());
        map.put("phone", firebaseUser.getPhoneNumber());
        map.put("email", firebaseUser.getEmail());
        map.put("profileImageUrl", userIcon != null ? userIcon.toString() : "");

        DatabaseReference current_user_db = FirebaseDatabase.getInstance().getReference("Users/Customers").child(userID);
        current_user_db.setValue(true); // Create the node User ID
        current_user_db.setValue(map);  // add Information to that user









    }
}