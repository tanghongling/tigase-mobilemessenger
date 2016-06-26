/*
 * ChatItemFragment.java
 *
 * Tigase Android Messenger
 * Copyright (C) 2011-2016 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
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

package org.tigase.messenger.phone.pro.conversations.chat;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;
import org.tigase.messenger.phone.pro.MainActivity;
import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import org.tigase.messenger.phone.pro.emoji.Emoji;
import org.tigase.messenger.phone.pro.emoji.EmojiUtil;
import org.tigase.messenger.phone.pro.emoji.FaceFragment;
import org.tigase.messenger.phone.pro.providers.ChatProvider;
import org.tigase.messenger.phone.pro.service.XMPPService;
import tigase.jaxmpp.android.Jaxmpp;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule;
import tigase.jaxmpp.core.client.xmpp.stanzas.Message;

import java.io.IOException;
import java.util.Date;

public class ChatItemFragment extends Fragment  implements FaceFragment.OnEmojiClickListener{

	@Bind(R.id.chat_list)
	RecyclerView recyclerView;
	@Bind(R.id.messageText)
	EditText message;
	@Bind(R.id.send_button)
	ImageView sendButton;
	@Bind(R.id.chat_textview)
	TextView chat_textview;
	@Bind(R.id.emojPanel)
	RelativeLayout emojPanel;
	@Bind(R.id.iv_face_normal)
	ImageView iv_face_normal;
	@Bind(R.id.iv_face_checked)
	ImageView iv_face_checked;
	@Bind(R.id.btn_more)
	ImageView btn_more;
	@Bind(R.id.Container)
	FrameLayout Container;
	private Chat chat;
	private Context context;

	private final MainActivity.XMPPServiceConnection mConnection = new MainActivity.XMPPServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			super.onServiceConnected(name, service);
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			setChat(null);
			super.onServiceDisconnected(name);
		}
	};
	private ChatItemIterationListener mListener = new ChatItemIterationListener() {

		@Override
		public void onCopyChatMessage(int id, String jid, String body) {
			ClipboardManager clipboard = (ClipboardManager) ChatItemFragment.this.getContext().getSystemService(
					Context.CLIPBOARD_SERVICE);
			ClipData clip = ClipData.newPlainText("Message from " + jid, body);

			clipboard.setPrimaryClip(clip);
		}
	};
	private MyChatItemRecyclerViewAdapter adapter;
	private Uri uri;
	private BareJID mAccount;
	private int mChatId;

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public ChatItemFragment() {
	}

	// TODO: Customize parameter initialization
	@SuppressWarnings("unused")
	public static ChatItemFragment newInstance(int columnCount) {
		ChatItemFragment fragment = new ChatItemFragment();
		Bundle args = new Bundle();
		fragment.setArguments(args);
		return fragment;
	}

	private Chat getChat() {
		if (this.chat != null)
			return this.chat;

		XMPPService service = mConnection.getService();

		if (service == null) {
			Log.w("ChatItemFragment", "Service is not binded");
			return null;
		}

		Jaxmpp jaxmpp = service.getJaxmpp(mAccount);

		if (jaxmpp == null) {
			Log.w("ChatItemFragment", "There is no account " + mAccount);
			return null;
		}

		for (Chat chat : jaxmpp.getModule(MessageModule.class).getChatManager().getChats()) {
			if (chat.getId() == mChatId) {
				setChat(chat);
			}
		}

		return this.chat;
	}

	private void setChat(Chat chat) {
		this.chat = chat;
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		this.context =context;
		this.mAccount = ((ChatActivity) context).getAccount();
		this.mChatId = ((ChatActivity) context).getOpenChatId();

		this.uri = Uri.parse(ChatProvider.CHAT_HISTORY_URI + "/" + ((ChatActivity) getContext()).getAccount() + "/"
				+ ((ChatActivity) getContext()).getJid());

		Intent intent = new Intent(context, XMPPService.class);
		getActivity().bindService(intent, mConnection, 0);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// setHasOptionsMenu(true);

		// if (getArguments() != null) {
		// mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
		// }
		FaceFragment faceFragment = FaceFragment.Instance();
		faceFragment.setListener(this);
		getChildFragmentManager().beginTransaction().add(R.id.Container,faceFragment).commit();

	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		// TODO
		inflater.inflate(R.menu.openchat_fragment, menu);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_chatitem_list, container, false);
		ButterKnife.bind(this, root);

		message.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
				if (id == R.id.send || id == EditorInfo.IME_NULL) {
					send();
					return true;
				}
				return false;
			}
		});
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

		sendButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				send();
			}
		});
		message.addTextChangedListener(watcher);

		// recyclerView.addItemDecoration(new
		// DividerItemDecoration(getActivity(),
		// DividerItemDecoration.VERTICAL_LIST));

		LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
		linearLayoutManager.setReverseLayout(true);

		recyclerView.setLayoutManager(linearLayoutManager);
		this.adapter = new MyChatItemRecyclerViewAdapter(getContext(), null, mListener) {
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
		(new DBUpdateTask()).execute();
	}

	private void send() {
		String body = this.message.getText().toString();
		if (body == null || body.trim().isEmpty())
			return;

		this.message.getText().clear();
		(new SendMessageTask()).execute(body);
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
					msg = getChat().createMessage(param);
					stanzaId = msg.getId();

					Jaxmpp jaxmpp = mConnection.getService().getJaxmpp(getChat().getSessionObject().getUserBareJid());
					MessageModule m = jaxmpp.getModule(
							MessageModule.class);

					if (jaxmpp.isConnected()) {
						m.sendMessage(msg);
						state = DatabaseContract.ChatHistory.STATE_OUT_SENT;
					} else {
						state = DatabaseContract.ChatHistory.STATE_OUT_NOT_SENT;
					}
				} catch (Exception e) {
					state = DatabaseContract.ChatHistory.STATE_OUT_NOT_SENT;
					Log.w("ChatItemFragment", "Cannot send message", e);
				}

				final JID recipient = getChat().getJid();

				ContentValues values = new ContentValues();
				values.put(DatabaseContract.ChatHistory.FIELD_AUTHOR_JID, getChat().getSessionObject().getUserBareJid().toString());
				values.put(DatabaseContract.ChatHistory.FIELD_JID, recipient.getBareJid().toString());
				values.put(DatabaseContract.ChatHistory.FIELD_TIMESTAMP, new Date().getTime());
				values.put(DatabaseContract.ChatHistory.FIELD_BODY, param);
				values.put(DatabaseContract.ChatHistory.FIELD_THREAD_ID, getChat().getThreadId());
				values.put(DatabaseContract.ChatHistory.FIELD_ACCOUNT, getChat().getSessionObject().getUserBareJid().toString());
				values.put(DatabaseContract.ChatHistory.FIELD_STATE, state);
				values.put(DatabaseContract.ChatHistory.FIELD_ITEM_TYPE, DatabaseContract.ChatHistory.ITEM_TYPE_MESSAGE);
				if (stanzaId != null)
					values.put(DatabaseContract.ChatHistory.FIELD_STANZA_ID, stanzaId);

				getContext().getContentResolver().insert(uri, values);
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
