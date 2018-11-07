package com.mohammed.transport;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    private static final int RC_SIGN_IN = 123 ;
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
        mDriver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityFinishCurrentActivity(DriverLoginActivity.class);
            }
        });

        mCustomer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signInFirebaseUI();
            }
        });
    }

    private void startActivityFinishCurrentActivity(Class activity) {
        Intent intent = new Intent(MainActivity.this, activity);
        startActivity(intent);
        finish();
    }

    private void signInFirebaseUI(){
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(Arrays.asList(
                                new AuthUI.IdpConfig.GoogleBuilder().build(),
                                new AuthUI.IdpConfig.FacebookBuilder().build(),
                                new AuthUI.IdpConfig.EmailBuilder().build()))
                        .setIsSmartLockEnabled(false)
                        .build(),
                RC_SIGN_IN);
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // RC_SIGN_IN is the request code you passed into startActivityForResult(...) when starting the sign in flow.
        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            // Successfully signed in
            if (resultCode == RESULT_OK) {
                mAuth = FirebaseAuth.getInstance();
                String user_id = mAuth.getCurrentUser().getUid();
                addUserIdtoFirebase(user_id);
                startActivityFinishCurrentActivity(CustomerMapActivity.class);
            } else {
                // Sign in failed
                if (response == null) {
                    // User pressed back button
                    Toast.makeText(this,R.string.sign_in_cancelled , Toast.LENGTH_SHORT).show();
                    return;
                }

                if (response.getError().getErrorCode() == ErrorCodes.NO_NETWORK) {
                    Toast.makeText(this,R.string.no_internet_connection , Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(this,R.string.unknown_error , Toast.LENGTH_SHORT).show();
                Log.e("MAINACTIVITY", "Sign-in error: ", response.getError());
            }
        }
    }

    private void addUserIdtoFirebase(String user_id) {
        DatabaseReference current_user_db = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(user_id);
        current_user_db.setValue(true);
    }
}
