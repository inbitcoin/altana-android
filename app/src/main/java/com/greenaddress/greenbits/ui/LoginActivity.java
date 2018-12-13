package com.greenaddress.greenbits.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class LoginActivity extends GaActivity {

    protected void onLoginSuccess() {
        // After login succeeds, show system messaages if there are any
        final Intent intent = new Intent(LoginActivity.this, TabbedMainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finishOnUiThread();
    }

    @Override
    protected void onResumeWithService() {
        if (mService.isLoggedOrLoggingIn()) {
            // already logged in, could be from different app via intent
            onLoginSuccess();
        }
    }

    protected boolean checkPinExist(final boolean fromPinActivity) {
        return checkPinExist(fromPinActivity, false);
    }

    // FIXME added forceReload to workaround reload some data in PinActivity
    protected boolean checkPinExist(final boolean fromPinActivity, final boolean forceReload) {
        final String ident = mService.cfg("pin").getString("ident", null);

        if ((fromPinActivity && ident == null) || (forceReload && fromPinActivity)){
            mService.cfgGlobalEdit("network").putBoolean("network_redirect", true).apply();
            startActivity(new Intent(this, FirstScreenActivity.class));
            finish();
            return true;
        }
        if ((!fromPinActivity && ident != null) || (forceReload)) {
            mService.cfgGlobalEdit("network").putBoolean("network_redirect", true).apply();
            startActivity(new Intent(this, PinActivity.class));
            finish();
            return true;
        }
        return false;
    }

    // FIXME manage better multiple wallet case
    protected boolean checkShowChooseNetworkIfMany() {
        final boolean asked = mService.cfgGlobal("network").getBoolean("network_asked", false);
        final boolean redirect = mService.cfgGlobal("network").getBoolean("network_redirect", false);
        if(asked && redirect)
            return false;

        final Set<String> networkSelector = mService.cfgGlobal("network").getStringSet("network_enabled", new HashSet<>());
        return networkSelector.size() != 1;
    }

    protected void chooseNetworkIfMany(final boolean fromPinActivity) {
        final boolean asked = mService.cfgGlobal("network").getBoolean("network_asked", false);
        final boolean redirect = mService.cfgGlobal("network").getBoolean("network_redirect", false);
        mService.cfgGlobalEdit("network")
                .putBoolean("network_asked", false)
                .putBoolean("network_redirect", false).apply();
        if(asked && redirect)
            return;

        final Set<String> networkSelector = mService.cfgGlobal("network").getStringSet("network_enabled", new HashSet<>());
        if (networkSelector.size() == 1)
            return;

        final Set<String> networkSelectorSet = mService.cfgGlobal("network").getStringSet("network_enabled", new HashSet<>());
        final List<String> networkSelectorList = new ArrayList<>(networkSelectorSet);
        Collections.sort(networkSelectorList);

        final MaterialDialog materialDialog = UI.popup(this, R.string.select_network, R.string.choose, R.string.choose_and_default)
                .items(networkSelectorList)
                .itemsCallbackSingleChoice(0, (dialog, v, which, text) -> {
                    selectedNetwork(text.toString(), false);
                    mService.cfgGlobalEdit("network").putBoolean("network_asked", true).apply();
                    checkPinExist(fromPinActivity, true);
                    setAppNameTitle();
                    return true;
                })
                .onNegative((dialog, which) -> {
                    selectedNetwork(networkSelectorList.get(dialog.getSelectedIndex()), true);
                    mService.cfgGlobalEdit("network").putBoolean("network_asked", true).apply();
                    checkPinExist(fromPinActivity, true);
                    setAppNameTitle();
                })
                .cancelable(false)
                .build();

        // select the current network
        materialDialog.setSelectedIndex(networkSelectorList.indexOf(mService.getNetwork().getName()));

        materialDialog.show();
    }

    protected void selectedNetwork(final String which, final boolean makeDefault) {
        Log.i("TAG", "which " + which + " default:" + makeDefault);
        final SharedPreferences.Editor editor = mService.cfgGlobalEdit("network");
        if (makeDefault) {
            final Set<String> networkSelectorNew = new HashSet<>();
            networkSelectorNew.add(which);
            editor.putStringSet("network_enabled", networkSelectorNew);
        }
        editor.putString("network_active", which);
        editor.apply();
        mService.updateSelectedNetwork();
    }

}
