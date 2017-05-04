package com.greenaddress.greenbits.ui.preferences;
import com.greenaddress.greenapi.CryptoHelper;
import com.greenaddress.greenbits.NfcWriteMnemonic;
import com.greenaddress.greenbits.ui.R;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;

import java.util.List;

public class SettingsActivity extends GaPreferenceActivity {

    public static String INTENT_SHOW_NFC_DIALOG_REQUEST = "intent_show_nfc_dialog";

    private NfcAdapter mNfcAdapter;
    private NfcWriteMnemonic mNfcWriteMnemonic;
    private PendingIntent mNfcPendingIntent;

    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(final List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        mNfcPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction()) && mNfcWriteMnemonic != null) {
            mNfcWriteMnemonic.write(intent);
        } else if (INTENT_SHOW_NFC_DIALOG_REQUEST.equals(intent.getAction())) {
            if (mNfcWriteMnemonic == null) {
                byte [] mnemonic = intent.getByteArrayExtra("mnemonic");
                String mnemonicText = CryptoHelper.mnemonic_from_bytes(mnemonic);
                mNfcWriteMnemonic = new NfcWriteMnemonic(mnemonicText, this);
            }
            mNfcWriteMnemonic.showDialog();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mNfcAdapter != null)
            mNfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mNfcAdapter != null) {
            final IntentFilter filter = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
            final IntentFilter[] filters = new IntentFilter[]{filter};
            mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent, filters, null);
        }
    }
}
