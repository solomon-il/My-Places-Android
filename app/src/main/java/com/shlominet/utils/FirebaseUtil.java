package com.shlominet.utils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

//helper class for getting a singleton object of FireBase reference
public class FirebaseUtil {

    private static FirebaseAuth mAuth;
    private static FirebaseDatabase mDatabase;
    private static StorageReference mStorageRef;

    public static FirebaseDatabase getDatabase() {
        if (mDatabase == null) {
            mDatabase = FirebaseDatabase.getInstance();
            mDatabase.setPersistenceEnabled(true);
        }
        return mDatabase;
    }

    public static FirebaseAuth getAuth() {
        if (mAuth == null) {
            mAuth = FirebaseAuth.getInstance();
        }
        return mAuth;
    }

    public static StorageReference getStorageRef() {
        if(mStorageRef == null) {
            mStorageRef = FirebaseStorage.getInstance().getReference();
        }
        return mStorageRef;
    }
}
