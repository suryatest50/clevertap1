package com.clevertap.android.sdk;

import android.content.Context;

abstract class CleverTapState {

    protected final Context context;

    CleverTapState(final Context context) {
        this.context = context;
    }

    public Context getContext() {
        return context;
    }

    abstract BaseDatabaseManager getDatabaseManager();

    abstract void setDatabaseManager(final BaseDatabaseManager databaseManager);

    abstract BaseNetworkManager getNetworkManager();

    abstract void setNetworkManager(final BaseNetworkManager networkManager);

    abstract PostAsyncSafelyHandler getPostAsyncSafelyHandler();

    abstract void setPostAsyncSafelyHandler(final PostAsyncSafelyHandler postAsyncSafelyHandler);

    abstract BaseLocationManager getLocationManager();

    abstract void setLocationManager(BaseLocationManager baseLocationManager);

}
