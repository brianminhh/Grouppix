package com.example.grouppix;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.security.acl.Group;
import java.util.ArrayList;
import java.util.List;

public class GroupActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    DrawerLayout drawerLayout;
    List<Groups> groups;
    private DatabaseReference mDatabaseRef;
    private DatabaseReference mUserRef;
    RecyclerView recyclerView;
    RecyclerView.Adapter rAdapter;
    NavigationView navigationView;
    Toolbar toolbar;
    Intent intent;
    FirebaseUser user;
    FloatingActionButton floatingBtn;
    boolean groupExists;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group);

        user = FirebaseAuth.getInstance().getCurrentUser();

        setTitle("Groups");

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        recyclerView = findViewById(R.id.recyclerView);
        toolbar = findViewById(R.id.toolbar);
        floatingBtn = findViewById(R.id.floatingBtn);

        setSupportActionBar(toolbar);

        navigationView.bringToFront();
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,R.string.navigation_drawer_open,R.string.navigation_drawer_closed);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);

        navigationView.setCheckedItem(R.id.nav_home);

        recyclerView.setLayoutManager(new GridLayoutManager(getApplicationContext(), 3));
        recyclerView.setHasFixedSize(true);

        groups = new ArrayList<>();

        mDatabaseRef = FirebaseDatabase.getInstance().getReference("groups");

        mDatabaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    Groups groupUpload = postSnapshot.getValue(Groups.class);
                    groups.add(groupUpload);
                }

                rAdapter = new GroupAdapter(getApplicationContext(), groups);

                recyclerView.setAdapter(rAdapter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(GroupActivity.this, "Error: could not retrieve groups", Toast.LENGTH_SHORT).show();
            }
        });

        floatingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                groupExists = false;
                AlertDialog.Builder alert = new AlertDialog.Builder(GroupActivity.this);
                alert.setTitle("Please enter a group to join or make");
                alert.setMessage("If you are already in the group, this will make you leave the group");
                EditText input = new EditText(GroupActivity.this);
                alert.setView(input);
                alert.setCancelable(false);

                alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDatabaseRef = FirebaseDatabase.getInstance().getReference("groups/" + input.getText().toString());
                        mUserRef = FirebaseDatabase.getInstance().getReference("groups/" + input.getText().toString() + "/" + "members");

                        Query query = mDatabaseRef.orderByChild("name").equalTo(input.getText().toString());
                        query.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot snapshot) {
                                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                                    Groups upload = postSnapshot.getValue(Groups.class);
                                    if (upload.name.equals(input.getText().toString())) {
                                        Query userQuery = mUserRef.orderByChild("username").equalTo(user.getEmail());
                                        userQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                                                    GroupUser groupUser = postSnapshot.getValue(GroupUser.class);

                                                    if (groupUser.username.equals(user.getEmail())) {
                                                        postSnapshot.getRef().removeValue();
                                                        Toast.makeText(GroupActivity.this, "Successfully removed from group", Toast.LENGTH_SHORT).show();
                                                    } else {
                                                        postSnapshot.getRef().push().setValue(new GroupUser(user.getEmail()));
                                                        Toast.makeText(GroupActivity.this, "Successfully joined group", Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {

                                            }
                                        });

                                        groupExists = true;
                                        break;
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                Toast.makeText(GroupActivity.this, "Error: could not retrieve images", Toast.LENGTH_SHORT).show();
                            }
                        });

                        if (groupExists == false) {
                            Groups groupUpload = new Groups(input.getText().toString());
                            GroupUser groupUserUpload = new GroupUser(user.getEmail());

                            String uploadId = mDatabaseRef.push().getKey();
                            mDatabaseRef.child(uploadId).setValue(groupUpload);
                            String uploadId2 = mUserRef.push().getKey();
                            mUserRef.child(uploadId2).setValue(groupUserUpload);
                            Toast.makeText(GroupActivity.this, "Group successfully made", Toast.LENGTH_SHORT).show();
                        }

                        intent = new Intent(getApplicationContext(), GroupActivity.class);
                        finish();
                        startActivity(intent);
                    }
                });
                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });

                alert.show();
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
                finish();
                startActivity(intent);
                break;
            case R.id.groups:
                intent = new Intent(getApplicationContext(), GroupActivity.class);
                finish();
                startActivity(intent);
                break;
            case R.id.gallery:
                break;
            case R.id.settings:
                intent = new Intent(getApplicationContext(), SettingsActivity.class);
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