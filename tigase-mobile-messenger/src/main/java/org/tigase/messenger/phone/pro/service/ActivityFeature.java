/*
 * Tigase Mobile Messenger for Android
 * Copyright (C) 2011-2014 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */
package org.tigase.messenger.phone.pro.service;

import org.tigase.messenger.phone.pro.Preferences;

import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xmpp.stanzas.Presence;
import tigase.jaxmpp.core.client.xmpp.stanzas.Presence.Show;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

public class ActivityFeature implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
		SharedPreferences.OnSharedPreferenceChangeListener {

	private static final String TAG = "ActivityFeature";
	private final Context context;

	private DetectedActivity activity = null;
	private JaxmppService jaxmppService;

	private boolean changeOnInVehicle;
	private GoogleApiClient mGoogleApiClient;

	public ActivityFeature(Context context, JaxmppService jaxmppService) {
		this.context = context;
		this.jaxmppService = jaxmppService;
	}

	public static boolean isAvailable(Context context) {
		return GooglePlayServicesUtil.isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS;
	}

	public void beforePresenceSend(SharedPreferences prefs, Presence presence) throws JaxmppException {
		if (activity != null && activity.getType() == DetectedActivity.IN_VEHICLE) {
			String showStr = prefs.getString(Preferences.ACTIVITY_IN_VEHICLE_STATUS, "dnd");
			if (!"no_change".equals(showStr)) {
				Show show = Show.valueOf(showStr);
				presence.setShow(show);
			}
			String descr = prefs.getString(Preferences.ACTIVITY_IN_VEHICLE_DESCR, "In a car..");
			if (descr != null && descr.length() > 0) {
				presence.setStatus(descr);
			}
		}
	}

	protected void onActivityChanged(DetectedActivity oldActivity, boolean force) {
		Log.v(TAG, "changed activity from " + oldActivity + " to " + activity);
		if ((oldActivity != null && oldActivity.getType() == DetectedActivity.IN_VEHICLE)
				|| (activity != null && activity.getType() == DetectedActivity.IN_VEHICLE)) {
			if (force || changeOnInVehicle)
				jaxmppService.sendAutoPresence(false);
		}
	}

	@Override
	public void onConnected(Bundle connectionHint) {
		Log.v(TAG, "Connection to activity recognition established");
		Intent intent = new Intent(jaxmppService, JaxmppService.class);
		PendingIntent callbackIntent = PendingIntent.getService(jaxmppService, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mGoogleApiClient, 0, callbackIntent);
	}

	@Override
	public void onConnectionFailed(ConnectionResult result) {
		Log.v(TAG, "Connection to activity recognition failed, cause = " + result.getErrorCode());
	}

	@Override
	public void onConnectionSuspended(int i) {

	}

	public void onHandleIntent(Intent intent) {
		if (ActivityRecognitionResult.hasResult(intent)) {
			ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
			// Put your application specific logic here (i.e.
			// result.getMostProbableActivity())
			DetectedActivity newActivity = result.getMostProbableActivity();
			Log.v(TAG, "handing activity recognition intent " + newActivity);
			if (newActivity == null || newActivity.getConfidence() > 70) {
				DetectedActivity oldActivity = activity;
				activity = newActivity;
				if (oldActivity != activity
						&& ((oldActivity == null || activity == null) || (oldActivity.getType() != activity.getType()))) {
					onActivityChanged(oldActivity, false);
				}
			}
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(Preferences.ACTIVITIES_ENABLED)) {
			if (sharedPreferences.getBoolean(key, true)) {
				onStart();
			} else {
				onStop();
			}
		}
		if (key.startsWith("activities_")) {
			if (key.startsWith("activities_in_vehicle"))
				changeOnInVehicle = (sharedPreferences.getString(Preferences.ACTIVITY_IN_VEHICLE_DESCR,
						"In a car..").length() > 0
						|| !sharedPreferences.getString(Preferences.ACTIVITY_IN_VEHICLE_STATUS, "dnd").equals("no_change"));
			jaxmppService.sendAutoPresence(false);
		}
	}

	public void onStart() {
		if (!isAvailable(jaxmppService))
			return;

		if (!jaxmppService.prefs.getBoolean(Preferences.ACTIVITIES_ENABLED, true))
			return;

		if (mGoogleApiClient != null)
			return;

		this.mGoogleApiClient = new GoogleApiClient.Builder(context).addConnectionCallbacks(this).addOnConnectionFailedListener(
				this).addApi(ActivityRecognition.API).build();
		mGoogleApiClient.connect();
	}

	// @Override
	// public void onDisconnected() {
	// Log.v(TAG, "Connecton to activity recognition broken");
	// if (client != null) {
	// client.connect();
	// }
	// }

	public void onStop() {
		if (mGoogleApiClient == null)
			return;

		GoogleApiClient tmp = mGoogleApiClient;
		mGoogleApiClient = null;
		tmp.disconnect();
		if (activity != null) {
			DetectedActivity oldActivity = activity;
			activity = null;
			onActivityChanged(oldActivity, true);
		}
	}
}
