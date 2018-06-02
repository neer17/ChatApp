package com.example.neerajsewani.neerajschat;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Debug;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.w3c.dom.Text;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {
    ListView listView;
    private static final String TAG = "MainActivity";
    private static final int DEFAULT_MESSAGE_LIMIT = 150;
    private static final int RC_SIGN_IN = 1;
    private String mUsername;
    private static final int RC_PHOTO_PICKER = 2;
    private static final String MESSAGE_LENGTH_KEY = "message_length_key";

    EditText messageEditText;
    ImageButton imageButton;
    Button sendButton;
    TextView messageTextView;
    TextView usernameTextView;
    ImageView imageView;

    FriendlyMessageAdapter adapter;


    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;
    ChildEventListener childEventListener;
    FirebaseAuth firebaseAuth;
    FirebaseAuth.AuthStateListener authStateListener;
    StorageReference storageReference;
    FirebaseStorage firebaseStorage;
    FirebaseRemoteConfig firebaseRemoteConfig;
    FirebaseRemoteConfigSettings configSettings;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

         messageEditText = findViewById(R.id.editText);
         imageButton = findViewById(R.id.imageButton);
         sendButton = findViewById(R.id.sendButton);
         messageTextView = findViewById(R.id.textView);
         usernameTextView = findViewById(R.id.textView2);

        listView = findViewById(R.id.listView);

        //  Firebase
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference().child("messages");
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference().child("chat_photos");
        firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();


        //  Default limit of the edit text
        messageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(DEFAULT_MESSAGE_LIMIT)});

        //  ListView and adapter
        List<FriendlyMessage> list = new ArrayList<>();
        adapter = new FriendlyMessageAdapter(this, list);
        listView.setAdapter(adapter);


        messageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(messageEditText.getText().toString().trim().length() > 0){
                    sendButton.setEnabled(true);
                }
                else
                    sendButton.setEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });


        //  For the image button
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent, "Choose an option from here"), RC_PHOTO_PICKER);
            }
        });


        //  Sending the values to the firebase database
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                databaseReference.push().setValue(new FriendlyMessage(messageEditText.getText().toString(), mUsername, null));

                messageEditText.setText("");
            }
        });





        //  Firebase Auth
        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();

                if(user != null){
                    onSignedInInitialize(user.getDisplayName());
                }
                else{

                    onSignedOutCleanUp();

                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setAvailableProviders(Arrays.asList(
                                            new AuthUI.IdpConfig.EmailBuilder().build(),
                                            new AuthUI.IdpConfig.GoogleBuilder().build()
                                    ))
                                    .build(),
                            RC_SIGN_IN);
                }
            }
        };

        configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build();
        firebaseRemoteConfig.setConfigSettings(configSettings);

        //  Setting default
        Map<String, Object> defaultSettings = new HashMap<>();
        defaultSettings.put(MESSAGE_LENGTH_KEY, 5);

        firebaseRemoteConfig.setDefaults(defaultSettings);

        //  Fetching the values from the firebase
        fetchConfig();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Signed in!!!1", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Failed to sign in", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
            else if(requestCode == RC_PHOTO_PICKER && resultCode == RESULT_OK){
                Uri selectedPhoto = data.getData();

                StorageReference localRefrence = storageReference.child(selectedPhoto.getLastPathSegment());

                //  Uploading the file on the storage
                localRefrence.putFile(selectedPhoto).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Uri downloadUrl = taskSnapshot.getDownloadUrl();

                        FriendlyMessage message = new FriendlyMessage(mUsername, null, downloadUrl.toString());
                        databaseReference.push().setValue(message);
                    }
                });
            }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_sign_out, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_sign_out : AuthUI.getInstance().signOut(this);
            return true;
            default: return super.onOptionsItemSelected(item);
        }
    }

    private void onSignedInInitialize(String user){
        mUsername = user;
        attachDatabaseListener();
    }

    private void onSignedOutCleanUp(){
        mUsername = "Anonymous";
        adapter.clear();
        detachDatabaseListener();
    }

    private void attachDatabaseListener(){
        if(childEventListener == null) {
            //  Fetching the values from the firebase database
            childEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    FriendlyMessage message = dataSnapshot.getValue(FriendlyMessage.class);
                    adapter.add(message);
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {

                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            };

            databaseReference.addChildEventListener(childEventListener);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(authStateListener != null){
            firebaseAuth.removeAuthStateListener(authStateListener);
        }
        adapter.clear();
        detachDatabaseListener();

    }

    @Override
    protected void onResume() {
        super.onResume();
        firebaseAuth.addAuthStateListener(authStateListener);
    }

    private void detachDatabaseListener(){
        if(childEventListener != null){
            databaseReference.removeEventListener(childEventListener);
            childEventListener = null;
        }
    }


    private void fetchConfig(){
        int cacheExpiration = 3600;

        if(firebaseRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled()){
            cacheExpiration = 0;
        }

        firebaseRemoteConfig.fetch(cacheExpiration).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                firebaseRemoteConfig.activateFetched();
                applyTheFetchedValuesHere();
            }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        applyTheFetchedValuesHere();
                    }
                });
    }

    private void applyTheFetchedValuesHere(){
        Long message_length = firebaseRemoteConfig.getLong(MESSAGE_LENGTH_KEY);
        messageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(message_length.intValue())});

    }
}
