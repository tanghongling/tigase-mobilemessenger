package org.tigase.messenger.phone.pro.account;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.tigase.messenger.phone.pro.R;
import org.tigase.messenger.phone.pro.service.SecureTrustManagerFactory;

import butterknife.Bind;
import butterknife.ButterKnife;
import tigase.jaxmpp.android.Jaxmpp;
import tigase.jaxmpp.core.client.AsyncCallback;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.Connector;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.XMPPException;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xmpp.modules.registration.InBandRegistrationModule;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;

public class CreateAccountActivity extends AppCompatActivity {

    // UI references.
    private EditText mXMPPIDView;
    private EditText mPasswordView;
    private EditText mNicknameView;
    @Bind(R.id.email_sign_in_button)
    Button mEmailSignInButton;

    private final Jaxmpp contact = new Jaxmpp();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);
        ButterKnife.bind(this);
        // Set up the login form.
        mXMPPIDView = (EditText) findViewById(R.id.xmppid);
        mPasswordView = (EditText) findViewById(R.id.password);
        mNicknameView = (EditText) findViewById(R.id.nickname);

        mEmailSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                 InBandRegistrationModule reg = contact.getModule(InBandRegistrationModule.class);
                contact.getProperties().setUserProperty(InBandRegistrationModule.IN_BAND_REGISTRATION_MODE_KEY, Boolean.TRUE);
                contact.getProperties().setUserProperty(SessionObject.DOMAIN_NAME, Configure.DOMAIN);
                contact.getProperties().setUserProperty(Connector.TRUST_MANAGERS_KEY, SecureTrustManagerFactory.getTrustManagers(CreateAccountActivity.this));
                String xmppid = mXMPPIDView.getText().toString();
                String password = mPasswordView.getText().toString();
                String nickname = mNicknameView.getText().toString();
                Log.i("hyg","111");
                try {
                    if(reg == null)
                    {
                        contact.getModulesManager().register(new InBandRegistrationModule());
                        reg = contact.getModule(InBandRegistrationModule.class);
                        Log.i("hyg","222");
                    }
                    Log.i("hyg","333");
                    reg.register(xmppid, password, null, new AsyncCallback() {
                        @Override
                        public void onError(Stanza responseStanza, XMPPException.ErrorCondition error) throws JaxmppException {
                            Toast.makeText(CreateAccountActivity.this,"faile",Toast.LENGTH_SHORT).show();
                            Log.i("hyg","error "+error.toString());
                        }

                        @Override
                        public void onSuccess(Stanza responseStanza) throws JaxmppException {
                            Toast.makeText(CreateAccountActivity.this,"sucess",Toast.LENGTH_SHORT).show();
                            Log.i("hyg","sucess");
                        }

                        @Override
                        public void onTimeout() throws JaxmppException {
                            Toast.makeText(CreateAccountActivity.this,"onTimeout",Toast.LENGTH_SHORT).show();
                            Log.i("hyg","onTimeout");
                        }
                    });
                } catch (JaxmppException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
