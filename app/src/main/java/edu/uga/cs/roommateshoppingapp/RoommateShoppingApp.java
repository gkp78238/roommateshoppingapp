package edu.uga.cs.roommateshoppingapp;

import android.app.Application;
import com.google.firebase.database.FirebaseDatabase;

public class RoommateShoppingApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }
}