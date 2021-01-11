package com.example.grouppix;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.MultiFactor;
import com.google.firebase.auth.MultiFactorAssertion;
import com.google.firebase.auth.MultiFactorSession;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.PhoneMultiFactorAssertion;
import com.google.firebase.auth.PhoneMultiFactorGenerator;

import java.util.concurrent.TimeUnit;

public class SettingsActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    DrawerLayout drawerLayout;
    NavigationView navigationView;
    Toolbar toolbar;
    FirebaseUser user;

    Intent intent;
    private FirebaseAuth mAuth;
    private String m_Text = "";
    String verificationId;
    PhoneAuthProvider.ForceResendingToken token;
    Switch mfaSwitch;
    SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();

        preferences = getSharedPreferences("prefName", MODE_PRIVATE);
        boolean silent = preferences.getBoolean("mfaSwitch", false);

        setTitle("Settings");

        user = FirebaseAuth.getInstance().getCurrentUser();

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view_settings);
        toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        navigationView.bringToFront();
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,R.string.navigation_drawer_open,R.string.navigation_drawer_closed);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);

        navigationView.setCheckedItem(R.id.nav_home);

        mfaSwitch = (Switch) findViewById(R.id.mfaSwitch);
        mfaSwitch.setChecked(silent);

        mfaSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    AlertDialog.Builder alert = new AlertDialog.Builder(SettingsActivity.this);
                    alert.setTitle("Please input your phone number");
                    alert.setMessage("You will receive an SMS for verification");
                    EditText input = new EditText(SettingsActivity.this);
                    input.setInputType(InputType.TYPE_CLASS_PHONE);
                    alert.setView(input);

                    alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            m_Text = "+1" + input.getText().toString();
                            PhoneAuthOptions options =
                                    PhoneAuthOptions.newBuilder(mAuth)
                                            .setPhoneNumber(m_Text)       // Phone number to verify
                                            .setTimeout(120L, TimeUnit.SECONDS) // Timeout and unit
                                            .setActivity(SettingsActivity.this)                 // Activity (for callback binding)
                                            .setCallbacks(callbacks)
                                            .build();
                            PhoneAuthProvider.verifyPhoneNumber(options);
                        }
                    });
                    alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            user.getMultiFactor().unenroll(user.getMultiFactor().getEnrolledFactors().get(0));
                            dialog.cancel();
                        }
                    });

                    alert.show();
                } else {
                    user.getMultiFactor().unenroll(user.getMultiFactor().getEnrolledFactors().get(0));
                    preferences.edit().putBoolean("mfaSwitch", isChecked);
                    preferences.edit().commit();
                }
            }
        });
    }

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        @Override
        public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
            Toast.makeText(SettingsActivity.this, "2FA Successfully set up!", Toast.LENGTH_SHORT).show();
            user.updatePhoneNumber(phoneAuthCredential);
            MultiFactorAssertion multiFactorAssertion = PhoneMultiFactorGenerator.getAssertion(phoneAuthCredential);
            user.getMultiFactor().enroll(multiFactorAssertion, user.getDisplayName());
            preferences.edit().putBoolean("mfaSwitch", true);
            preferences.edit().commit();
        }

        @Override
        public void onVerificationFailed(FirebaseException e) {
            Toast.makeText(SettingsActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.v("Login__", "Error: exception: " + e);
            mfaSwitch.toggle();
        }

        @Override
        public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
            SettingsActivity.this.verificationId = verificationId;
            token = forceResendingToken;

            AlertDialog.Builder alert = new AlertDialog.Builder(SettingsActivity.this);
            alert.setTitle("Please input your verification code sent to your phone");
            alert.setMessage("ex: 123456");
            EditText input = new EditText(SettingsActivity.this);
            alert.setView(input);
            alert.setCancelable(false);

            alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    PhoneAuthCredential credential = PhoneAuthProvider.getCredential(SettingsActivity.this.verificationId, input.getText().toString());
                    linkPhoneAuthCredential(credential);
                }
            });
            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });

            alert.show();
        }
    };

    private void linkPhoneAuthCredential(PhoneAuthCredential credential) {
        user.linkWithCredential(credential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(SettingsActivity.this, "2FA Successfully set up!", Toast.LENGTH_SHORT).show();
                    user.updatePhoneNumber(credential);
                    user.getMultiFactor().getSession()
                            .addOnCompleteListener(
                                    new OnCompleteListener<MultiFactorSession>() {
                                        @Override
                                        public void onComplete(@NonNull Task<MultiFactorSession> task) {
                                            if (task.isSuccessful()) {
                                                MultiFactorSession multiFactorSession = task.getResult();
                                            }
                                        }
                                    });
                    MultiFactorAssertion multiFactorAssertion = PhoneMultiFactorGenerator.getAssertion(credential);
                    user.getMultiFactor().enroll(multiFactorAssertion, user.getDisplayName());
                    preferences.edit().putBoolean("mfaSwitch", true);
                    preferences.edit().commit();
                } else {
                    Toast.makeText(SettingsActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        switch(menuItem.getItemId())
        {
            case R.id.nav_home:
                intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                break;
            case R.id.profile:
                intent = new Intent(getApplicationContext(), ProfileActivity.class);
                startActivity(intent);
                break;
            case R.id.groups:
                intent = new Intent(getApplicationContext(), GroupActivity.class);
                startActivity(intent);
                break;
            case R.id.gallery:
                break;
            case R.id.settings:
                intent = new Intent(getApplicationContext(), SettingsActivity.class);
                finish();
                startActivity(intent);
                break;
            case R.id.logout:
                AuthUI.getInstance()
                        .signOut(this)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            public void onComplete(@NonNull Task<Void> task) {
                                user = null;
                                intent = new Intent(getApplicationContext(), MainActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                            }
                        });
                FirebaseAuth.getInstance().signOut();
                break;
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
}