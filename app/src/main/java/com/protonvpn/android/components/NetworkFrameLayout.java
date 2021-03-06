/*
 * Copyright (c) 2017 Proton Technologies AG
 *
 * This file is part of ProtonVPN.
 *
 * ProtonVPN is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonVPN is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonVPN.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.protonvpn.android.components;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.text.Html;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.protonvpn.android.BuildConfig;
import com.protonvpn.android.R;
import com.protonvpn.android.api.ApiResult;
import com.protonvpn.android.ui.login.TroubleshootActivity;
import com.protonvpn.android.utils.ConnectionTools;
import com.protonvpn.android.utils.Log;

public class NetworkFrameLayout extends RelativeLayout implements View.OnClickListener, LoaderUI {

    private View loadingView;
    private View retryView;
    String loadingTitle;
    private OnRequestRetryListener listener;
    private State state = State.EMPTY;

    public enum State {
        LOADING, ERROR, EMPTY
    }

    @Override
    public void switchToRetry(ApiResult.Error error) {
        state = State.ERROR;
        switchToRetryView(error);
    }

    @Override
    public void switchToEmpty() {
        state = State.EMPTY;
        switchToEmptyView();
    }

    @Override
    public void switchToLoading() {
        state = State.LOADING;
        switchToLoadingView(loadingTitle);
    }

    public void switchToLoading(String loadingTitle) {
        state = State.LOADING;
        switchToLoadingView(loadingTitle);
    }

    @Override
    public void setRetryListener(OnRequestRetryListener listener) {
        setOnRequestRetryListener(listener);
    }

    @Override
    public State getState() {
        return state;
    }

    public interface OnRequestRetryListener {

        void onRequestRetry();
    }

    public NetworkFrameLayout(Context context) {
        super(context);
    }

    public NetworkFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttrs(attrs);
    }

    public NetworkFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrs(attrs);
    }

    private void initAttrs(AttributeSet attrs) {
        TypedArray a =
            getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.NetworkFrameLayout, 0, 0);
        loadingTitle = a.getString(R.styleable.NetworkFrameLayout_textLoading);
        a.recycle();
    }

    private void switchToLoadingView(String loadingTitle) {
        if (loadingView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            loadingView = inflater.inflate(R.layout.fragment_app_loading, this, false);
            TextView textLoading = loadingView.findViewById(R.id.textLoading);
            textLoading.setText(loadingTitle);
            addView(loadingView);
        }
        else {
            TextView textLoading = loadingView.findViewById(R.id.textLoading);
            textLoading.setText(loadingTitle);
            loadingView.setVisibility(View.VISIBLE);
        }
        if (retryView != null) {
            retryView.setVisibility(View.GONE);
        }
    }

    private void switchToRetryView(ApiResult.Error error) {
        if (retryView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            retryView = inflater.inflate(R.layout.fragment_retry_screen, this, false);
            addView(retryView);
            retryView.findViewById(R.id.buttonRetry).setOnClickListener(this);
        }
        else {
            retryView.setVisibility(View.VISIBLE);
        }

        initRetryView(error);

        if (loadingView != null) {
            loadingView.setVisibility(View.GONE);
        }
    }

    private void initRetryView(final ApiResult.Error error) {
        TextView textDescription = retryView.findViewById(R.id.textDescription);
        TextView troubleshootButton = retryView.findViewById(R.id.buttonTroubleshoot);

        boolean showTroubleshoot = error.isPotentialBlocking(getContext());
        troubleshootButton.setVisibility(showTroubleshoot ? VISIBLE : GONE);
        if (showTroubleshoot) {
            troubleshootButton.setText(Html.fromHtml(getContext().getString(R.string.buttonTroubleshoot)));
            troubleshootButton.setOnClickListener(
                (view) -> getContext().startActivity(new Intent(getContext(), TroubleshootActivity.class)));
        }

        if (!BuildConfig.DEBUG) {
            Log.exception(new Throwable("Something went wrong: " + error.getDebugMessage()));
        }

        // TODO Remove this click listener upon release
        textDescription.setOnLongClickListener(v -> {
            Toast.makeText(getContext(), error.getDebugMessage(), Toast.LENGTH_LONG).show();
            return true;
        });
        if (!ConnectionTools.isNetworkAvailable(getContext())) {
            textDescription.setText(R.string.loaderErrorNoInternet);
        }
        else {
            textDescription.setText(error.getMessage(getContext()));
        }
    }

    private void switchToEmptyView() {
        if (loadingView != null) {
            loadingView.setVisibility(View.GONE);
        }
        if (retryView != null) {
            retryView.setVisibility(View.GONE);
        }
    }

    private void setOnRequestRetryListener(OnRequestRetryListener listener) {
        this.listener = listener;
    }

    @Override
    public void onClick(View v) {
        if (listener != null) {
            listener.onRequestRetry();
        }
    }
}