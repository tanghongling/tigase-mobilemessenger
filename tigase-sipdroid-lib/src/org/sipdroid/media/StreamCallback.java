package org.sipdroid.media;

import android.content.Context;
import android.media.AudioManager;
import android.telephony.TelephonyManager;

public interface StreamCallback {
	public static final int UA_STATE_IDLE = 0;
	public static final int UA_STATE_INCOMING_CALL = 1;
	public static final int UA_STATE_OUTGOING_CALL = 2;
	public static final int UA_STATE_INCALL = 3;
	public static final int UA_STATE_HOLD = 4;

	TelephonyManager getTelephonyManager();

	boolean isImprove();

	boolean isUseGSM();

	int getCallState();

	Context getContext();

	int getHeadset();

	void setStreamVolume(int stream, int vol, int flags);

	int getVolume(int speakermode);

	void setVolume(int speakermode, int streamVolume);

	AudioManager getAudioManager();

}
