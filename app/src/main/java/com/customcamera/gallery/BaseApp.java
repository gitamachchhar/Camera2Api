package com.customcamera.gallery;

import android.app.Application;

import io.realm.Realm;
import io.realm.RealmConfiguration;

public class BaseApp extends Application {

    private static RealmConfiguration realmConfiguration;

    @Override
    public void onCreate() {
        super.onCreate();
        Realm.init(this);
        realmConfiguration = realmConfiguration();
        Realm.setDefaultConfiguration(realmConfiguration);
    }

    private static RealmConfiguration realmConfiguration() {
        if (realmConfiguration != null) return realmConfiguration;

        realmConfiguration = new RealmConfiguration.Builder()
                .name("CameraExample.db")
                .deleteRealmIfMigrationNeeded()
                .schemaVersion(1)
                .build();
        return realmConfiguration;
    }
}
