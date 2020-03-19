package com.clevertap.android.sdk.featureFlags;

import android.content.Context;
import android.text.TextUtils;

import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.TaskManager;
import com.clevertap.android.sdk.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;

public class CTFeatureFlagsController {

    private String guid;
    private CleverTapInstanceConfig config;
    private HashMap<String, Boolean> store;
    private boolean isInitialized = false;
    private WeakReference<FeatureFlagListener> listenerWeakReference;
    private Context mContext;

    public CTFeatureFlagsController(Context context, String guid, CleverTapInstanceConfig config, FeatureFlagListener listener) {
        this.guid = guid;
        this.config = config;
        this.store = new HashMap<>();
        this.setListener(listener);
        this.mContext = context.getApplicationContext();
        unarchiveData(false);
    }

    private void setListener(FeatureFlagListener listener) {
        listenerWeakReference = new WeakReference<>(listener);
    }

    private FeatureFlagListener getListener() {
        FeatureFlagListener listener = null;
        try {
            listener = listenerWeakReference.get();
        } catch (Throwable t) {
            // no-op
        }
        if (listener == null) {
            config.getLogger().verbose(config.getAccountId(), "CTABTestListener is null in CTABTestController");
        }
        return listener;
    }


    public void updateFeatureFlags(JSONObject featureFlagResponseObj) throws JSONException {

        updateFeatureFlags(featureFlagResponseObj, true);
    }

    private void notifyFeatureFlagUpdate() {
        FeatureFlagListener listener = getListener();
        if (listener != null) {
            listener.featureFlagsDidUpdate();
        }
    }

    public void fetchFeatureFlags() {
        FeatureFlagListener listener = getListener();
        if (listener != null) {
            listener.fetchFeatureFlags();
        }
    }

    private void updateFeatureFlags(JSONObject jsonObject, boolean isNew) throws JSONException {
        getConfigLogger().verbose(getAccountId(), "Updating feature flags...");
        JSONArray featureFlagList = jsonObject.getJSONArray(Constants.KEY_KV);
        try {
            for (int i = 0; i < featureFlagList.length(); i++) {
                JSONObject ff = featureFlagList.getJSONObject(i);
                store.put(ff.getString("n"), ff.getBoolean("v"));
            }
        } catch (JSONException e) {
            getConfigLogger().verbose(getAccountId(), "Error parsing Feature Flag array " + e.getLocalizedMessage());
        }

        if (isNew) {
            archiveData(jsonObject, false);
            notifyFeatureFlagUpdate();
        }
    }

    private void archiveData(JSONObject featureFlagRespObj, boolean sync) {

        if (featureFlagRespObj != null) {
            try {
                Utils.writeJsonToFile(mContext, getCachedDirName(), getCachedFileName(), featureFlagRespObj);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void unarchiveData(boolean sync) {
        TaskManager.getInstance().execute(new TaskManager.TaskListener<Void, Boolean>() {
            @Override
            public Boolean doInBackground(Void aVoid) {
                store.clear();
                String content = Utils.readFromFile(mContext, getCachedFullPath());
                if (!TextUtils.isEmpty(content)) {
                    try {
                        JSONObject jsonObject = new JSONObject(content);
                        JSONArray kvArray = jsonObject.getJSONArray(Constants.KEY_KV);

                        if (kvArray != null && kvArray.length() > 0) {
                            for (int i = 0; i < kvArray.length(); i++) {
                                JSONObject object = (JSONObject) kvArray.get(i);
                                if (object != null) {
                                    Iterator<String> iterator = object.keys();
                                    while (iterator.hasNext()) {
                                        String key = iterator.next();
                                        if (!TextUtils.isEmpty(key)) {
                                            Boolean value = (Boolean) object.get(key);
                                            store.put(key, value);
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        return false;
                    }
                }
                return true;
            }

            @Override
            public void onPostExecute(Boolean aBoolean) {

            }
        });

    }

    private boolean get(String key, boolean defaultValue) {
        getConfigLogger().verbose(getAccountId(), "getting feature flag with key - " + key + " and default value - " + defaultValue);
        if (store.get(key) != null) {
            return store.get(key);
        } else {
            getConfigLogger().verbose(getAccountId(), "feature flag not found, returning default value - " + defaultValue);
            return defaultValue;
        }
    }

    private Logger getConfigLogger() {
        return config.getLogger();
    }

    private String getAccountId() {
        return config.getAccountId();
    }

    public void resetWithGuid(String guid) {
        this.guid = guid;
    }

    public boolean isInitialized() {
        return isInitialized;
    }


    private String getCachedFileName() {
        return CTFeatureFlagConstants.CACHED_FILE_NAME ;
    }

    private String getCachedDirName() {
        return CTFeatureFlagConstants.DIR_FEATURE_FLAG + "_" + config.getAccountId() + "_" + guid;
    }


    private String getCachedFullPath() {
        return getCachedDirName() + "/" + getCachedFileName();
    }
}
