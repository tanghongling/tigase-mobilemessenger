package org.tigase.messenger.phone.pro.conversations.muc;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import org.tigase.messenger.phone.pro.MainActivity;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.conversations.AbstractConversationActivity;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import org.tigase.messenger.phone.pro.emoji.Emoji;
import org.tigase.messenger.phone.pro.emoji.EmojiUtil;
import org.tigase.messenger.phone.pro.emoji.FaceFragment;
import org.tigase.messenger.phone.pro.providers.ChatProvider;
import org.tigase.messenger.phone.pro.service.XMPPService;

import java.io.IOException;

import tigase.jaxmpp.android.Jaxmpp;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.xmpp.modules.muc.MucModule;
import tigase.jaxmpp.core.client.xmpp.modules.muc.Room;
import tigase.jaxmpp.core.client.xmpp.stanzas.Message;

public class MucItemFragment extends Fragment  implements FaceFragment.OnEmojiClickListener{

	@Bind(R.id.chat_list)
	RecyclerView recyclerView;
	@Bind(R.id.messageText)
	EditText message;
	@Bind(R.id.send_button)
	ImageView sendButton;
	@Bind(R.id.iv_face_normal)
	ImageView iv_face_normal;
	@Bind(R.id.iv_face_checked)
	ImageView iv_face_checked;
	@Bind(R.id.btn_more)
	ImageView btn_more;
	@Bind(R.id.Container)
	FrameLayout Container;
	private Room room;
	private Uri uri;
	private MucItemRecyclerViewAdapter adapter;
	private Context context;
	private final MainActivity.XMPPServiceConnection mConnection = new MainActivity.XMPPServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			super.onServiceConnected(name, service);
			BareJID account = ((AbstractConversationActivity) getContext()).getAccount();
			Jaxmpp jaxmpp = getService().getJaxmpp(account);

			final BareJID roomJID = ((AbstractConversationActivity) getContext()).getJid().getBareJid();

			if (jaxmpp == null) {

			}
			Log.d("MucItemFragment", "RoomJID=" + roomJID);
			setRoom(jaxmpp.getModule(MucModule.class).getRoom(roomJID));
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			setRoom(null);
			super.onServiceDisconnected(name);
		}
	};
	private ChatItemIterationListener mListener = new ChatItemIterationListener() {

		@Override
		public void onCopyChatMessage(int id, String jid, String body) {
			ClipboardManager clipboard = (ClipboardManager) MucItemFragment.this.getContext().getSystemService(
					Context.CLIPBOARD_SERVICE);
			ClipData clip = ClipData.newPlainText("Message from " + jid, body);

			clipboard.setPrimaryClip(clip);
		}
	};
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		FaceFragment faceFragment = FaceFragment.Instance();
		faceFragment.setListener(this);
		getChildFragmentManager().beginTransaction().add(R.id.Container,faceFragment).commit();

	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		this.context =context;
		this.uri = Uri.parse(ChatProvider.MUC_HISTORY_URI + "/" + ((AbstractConversationActivity) getContext()).getAccount()
				+ "/" + ((AbstractConversationActivity) getContext()).getJid());

		Intent intent = new Intent(context, XMPPService.class);
		getActivity().bindService(intent, mConnection, 0);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_chatitem_list, container, false);
		ButterKnife.bind(this, root);

		message.setEnabled(false);
		message.addTextChangedListener(watcher);
		message.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Container.setVisibility(View.GONE);
				iv_face_checked.setVisibility(View.VISIBLE);
				iv_face_normal.setVisibility(View.GONE);
			}
		});

		iv_face_normal.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Container.setVisibility(View.VISIBLE);
				iv_face_checked.setVisibility(View.VISIBLE);
				iv_face_normal.setVisibility(View.GONE);
				closeIME();
			}
		});
		iv_face_checked.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Container.setVisibility(View.GONE);
				iv_face_normal.setVisibility(View.VISIBLE);
				iv_face_checked.setVisibility(View.GONE);
			}
		});
		LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
		linearLayoutManager.setReverseLayout(true);

		recyclerView.setLayoutManager(linearLayoutManager);
		this.adapter = new MucItemRecyclerViewAdapter(getContext(), null, mListener) {
			@Override
			protected void onContentChanged() {
				refreshChatHistory();
			}
		};
		recyclerView.setAdapter(adapter);

		refreshChatHistory();
		return root;
	}
	private TextWatcher watcher = new TextWatcher() {

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			// TODO Auto-generated method stub
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
									  int after) {
			// TODO Auto-generated method stub
		}

		@Override
		public void afterTextChanged(Editable s) {
			// TODO Auto-generated method stub
			String s1 = message.getText().toString();
			if(!s1.isEmpty()){
				btn_more.setVisibility(View.INVISIBLE);
				sendButton.setVisibility(View.VISIBLE);
			}else {
				btn_more.setVisibility(View.VISIBLE);
				sendButton.setVisibility(View.INVISIBLE);
			}
		}
	};

	@Override
	public void onDetach() {
		mListener = null;
		recyclerView.setAdapter(null);
		adapter.changeCursor(null);
		getActivity().unbindService(mConnection);
		super.onDetach();
	}

	private void refreshChatHistory() {
		(new MucItemFragment.DBUpdateTask()).execute();
	}

	@OnClick(R.id.send_button)
	void send() {
		String body = this.message.getText().toString();
		if (body == null || body.trim().isEmpty())
			return;

		this.message.getText().clear();
		(new SendMessageTask()).execute(body);
	}

	private void setRoom(Room room) {
		this.room = room;
		message.setEnabled(room != null);
		if (adapter != null) {
			adapter.setOwnNickname(room.getNickname());
		}
	}

	public  void closeIME() {
		InputMethodManager imm = ( InputMethodManager ) context.getSystemService( Context.INPUT_METHOD_SERVICE );
		if ( imm.isActive( ) ) {
			imm.hideSoftInputFromWindow( message.getApplicationWindowToken( ) , 0 );
		}
	}
	public void openIME() {
		InputMethodManager imm = ( InputMethodManager ) context.getSystemService( Context.INPUT_METHOD_SERVICE );

		imm.showSoftInput(message,InputMethodManager.SHOW_FORCED);
	}

	@Override
	public void onEmojiDelete() {
		String text = this.message.getText().toString();
		if (text.isEmpty()) {
			return;
		}
		if ("]".equals(text.substring(text.length() - 1, text.length()))) {
			int index = text.lastIndexOf("[");
			if (index == -1) {
				int action = KeyEvent.ACTION_DOWN;
				int code = KeyEvent.KEYCODE_DEL;
				KeyEvent event = new KeyEvent(action, code);
				this.message.onKeyDown(KeyEvent.KEYCODE_DEL, event);
				displayTextView();
				return;
			}
			Editable s =message.getText().delete(index, text.length());
			displayTextView();
			return;
		}
		int action = KeyEvent.ACTION_DOWN;
		int code = KeyEvent.KEYCODE_DEL;
		KeyEvent event = new KeyEvent(action, code);
		this.message.onKeyDown(KeyEvent.KEYCODE_DEL, event);
		displayTextView();
	}

	private void displayTextView() {
		try {
			Log.e("tanghongling","------"+this.message.getText().toString());
			EmojiUtil.handlerEmojiText(this.message, this.message.getText().toString(),context);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onEmojiClick(Emoji emoji) {
		if (emoji != null) {
			int index = message.getSelectionStart();
			Editable editable = message.getEditableText();
			if (index < 0) {
				message.getEditableText().append(emoji.getContent());
			} else {
				message.getEditableText().insert(index, emoji.getContent());
			}
		}
		displayTextView();
	}

	public interface ChatItemIterationListener {
		void onCopyChatMessage(int id, String jid, String body);
	}

	private class SendMessageTask extends AsyncTask<String, Void, Void> {
		@Override
		protected Void doInBackground(String... params) {
			for (String param : params) {
				int state;
				Message msg;
				String stanzaId = null;
				try {
					msg = room.createMessage(param);
					Jaxmpp jaxmpp = mConnection.getService().getJaxmpp(room.getSessionObject().getUserBareJid());

					stanzaId = msg.getId();
					if (jaxmpp.isConnected() && room.getState() == Room.State.joined) {
						room.sendMessage(msg);
						state = DatabaseContract.ChatHistory.STATE_OUT_SENT;
					} else {
						state = DatabaseContract.ChatHistory.STATE_OUT_NOT_SENT;
					}
				} catch (Exception e) {
					state = DatabaseContract.ChatHistory.STATE_OUT_NOT_SENT;
					Log.w("MucItemFragment", "Cannot send message", e);
				}

				ContentValues values = new ContentValues();
				values.put(DatabaseContract.ChatHistory.FIELD_JID, room.getRoomJid().toString());
				values.put(DatabaseContract.ChatHistory.FIELD_AUTHOR_NICKNAME, room.getNickname());
				values.put(DatabaseContract.ChatHistory.FIELD_TIMESTAMP, System.currentTimeMillis());
				values.put(DatabaseContract.ChatHistory.FIELD_STANZA_ID, stanzaId);

				values.put(DatabaseContract.ChatHistory.FIELD_STATE, state);
				values.put(DatabaseContract.ChatHistory.FIELD_BODY, param);
				values.put(DatabaseContract.ChatHistory.FIELD_ITEM_TYPE,
						DatabaseContract.ChatHistory.ITEM_TYPE_GROUPCHAT_MESSAGE);

				values.put(DatabaseContract.ChatHistory.FIELD_ACCOUNT, room.getSessionObject().getUserBareJid().toString());

				Uri uri = Uri.parse(ChatProvider.MUC_HISTORY_URI + "/" + room.getSessionObject().getUserBareJid() + "/"
						+ Uri.encode(room.getRoomJid().toString()));
				Uri x = getContext().getContentResolver().insert(uri, values);
				if (x != null)
					getContext().getContentResolver().notifyChange(x, null);
			}

			return null;
		}
	}

	private class DBUpdateTask extends AsyncTask<Void, Void, Cursor> {

		private final String[] cols = new String[]{DatabaseContract.ChatHistory.FIELD_ID,
				DatabaseContract.ChatHistory.FIELD_ACCOUNT, DatabaseContract.ChatHistory.FIELD_AUTHOR_JID,
				DatabaseContract.ChatHistory.FIELD_ITEM_TYPE, DatabaseContract.ChatHistory.FIELD_AUTHOR_NICKNAME,
				DatabaseContract.ChatHistory.FIELD_BODY, DatabaseContract.ChatHistory.FIELD_DATA,
				DatabaseContract.ChatHistory.FIELD_JID, DatabaseContract.ChatHistory.FIELD_STATE,
				DatabaseContract.ChatHistory.FIELD_THREAD_ID, DatabaseContract.ChatHistory.FIELD_TIMESTAMP};

		@Override
		protected Cursor doInBackground(Void... params) {
			if (getContext() == null)
				return null;
			Cursor cursor = getContext().getContentResolver().query(uri, cols, null, null,
					DatabaseContract.ChatHistory.FIELD_TIMESTAMP + " DESC");

			return cursor;
		}

		@Override
		protected void onPostExecute(Cursor cursor) {
			adapter.changeCursor(cursor);
			recyclerView.smoothScrollToPosition(0);
		}
	}
}
