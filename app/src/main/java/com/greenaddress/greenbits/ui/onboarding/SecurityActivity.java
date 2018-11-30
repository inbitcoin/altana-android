package com.greenaddress.greenbits.ui.onboarding;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.greenaddress.greenapi.data.TwoFactorConfigData;
import com.greenaddress.greenapi.model.TwoFactorConfigDataObservable;
import com.greenaddress.greenbits.ui.GaActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.TwoFactorActivity;
import com.greenaddress.greenbits.ui.UI;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

public class SecurityActivity extends GaActivity implements View.OnClickListener, Observer {
    private static final int REQUEST_2FA = 100;
    private ViewAdapter mMethodsAdapter;
    private boolean mFromOnboarding;

    @Override
    protected void onCreateWithService(final Bundle savedInstanceState) {
        setContentView(R.layout.activity_onboarding_security);
        setTitle("");
        getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(android.R.color.transparent));

        final String[] choices = getResources().getStringArray(R.array.twoFactorChoices);
        final String[] methods = getResources().getStringArray(R.array.twoFactorMethods);
        final Integer[] images = {
            R.drawable.onboarding_email,
            R.drawable.onboarding_sms,
            R.drawable.onboarding_call,
            R.drawable.onboarding_ga
        };

        mFromOnboarding = getIntent().getBooleanExtra("from_onboarding", false);
        if (!mFromOnboarding) {
            UI.hide(UI.find(this, R.id.nextButton));
            setTitleBack();
        }

        mMethodsAdapter = new ViewAdapter(this,
                                          Arrays.asList(methods),
                                          Arrays.asList(choices),
                                          Arrays.asList(images));
        final RecyclerView wordsRecyclerView = UI.find(this, R.id.twoFactorsRecyclerView);
        wordsRecyclerView.setHasFixedSize(true);
        wordsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        wordsRecyclerView.setAdapter(mMethodsAdapter);
        initEnabledMethods();
    }

    @Override
    protected void onResumeWithService() {
        super.onResumeWithService();
        UI.mapClick(this, R.id.nextButton, this);
        initEnabledMethods();
        mService.getModel().getTwoFactorConfigDataObservable().addObserver(this);
    }

    @Override
    protected void onPauseWithService() {
        super.onPauseWithService();
        UI.unmapClick(UI.find(this, R.id.nextButton));
        mService.getModel().getTwoFactorConfigDataObservable().deleteObserver(this);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(final View view) {
        goToTabbedMainActivity();
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_2FA && resultCode == RESULT_OK) {
            getModel().getTwoFactorConfigDataObservable().refresh();
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        if (o instanceof TwoFactorConfigDataObservable) {
            initEnabledMethods();
        }
    }

    private void initEnabledMethods() {
        final TwoFactorConfigData twoFactorConfig = mService.getModel().getTwoFactorConfig();
        if (twoFactorConfig == null)
            return;
        final List<String> enabledMethods = twoFactorConfig.getEnabledMethods();
        mMethodsAdapter.setEnabled(enabledMethods);
        if (mFromOnboarding && twoFactorConfig.getAllMethods().size() == enabledMethods.size()) {
            // The user has enabled all methods, so continue to the main activity
            finish();
            goToTabbedMainActivity();
        }
    }

    class ViewAdapter extends RecyclerView.Adapter<ViewAdapter.ViewHolder> {

        private final List<String> mMethods;
        private final List<String> mChoices;
        private final List<Integer> mImages;
        private final LayoutInflater mInflater;
        private Set<String> mEnabled;

        ViewAdapter(final Context context, final List<String> methods,
                    final List<String> choices, final List<Integer> images) {
            mInflater = LayoutInflater.from(context);
            mMethods = methods;
            mChoices = choices;
            mImages = images;
            mEnabled = new HashSet<>();
        }

        @Override
        public ViewAdapter.ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
            final View view = mInflater.inflate(R.layout.list_element_two_factor, parent, false);
            return new ViewAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewAdapter.ViewHolder holder, final int position) {
            final String method = mMethods.get(position);
            holder.nameText.setText(mChoices.get(position));
            holder.imageView.setImageResource(mImages.get(position));
            final boolean isEnabled = mEnabled.contains(method);
            holder.enabled.setImageResource(isEnabled ? android.R.drawable.checkbox_on_background : R.drawable.next);
            holder.itemView.setOnClickListener(view -> {
                final Intent intent = new Intent(SecurityActivity.this, TwoFactorActivity.class);
                intent.putExtra("method", method);
                intent.putExtra("enable", !isEnabled);
                startActivityForResult(intent, REQUEST_2FA);
            });
        }

        @Override
        public int getItemCount() {
            return mChoices.size();
        }

        public void setEnabled(final List<String> methods) {
            mEnabled = new HashSet<>(methods);
            runOnUiThread(() -> mMethodsAdapter.notifyDataSetChanged());
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public TextView nameText;
            public ImageView imageView;
            public ImageView enabled;

            ViewHolder(final View itemView) {
                super(itemView);
                nameText = UI.find(itemView, R.id.nameText);
                imageView = UI.find(itemView, R.id.imageView);
                enabled = UI.find(itemView, R.id.imageEnabled);
            }
        }
    }
}