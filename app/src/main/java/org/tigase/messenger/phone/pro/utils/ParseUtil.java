package org.tigase.messenger.phone.pro.utils;

/**
 * Created by haoyaogang_1 on 2016/6/19.
 */
public class ParseUtil {
    public static String[] parseJID(String jid) {
        String[] result = new String[3];

        // Cut off the resource part first
        int idx = jid.indexOf('/');

        // Resource part:
        result[2] = ((idx == -1) ? null : jid.substring(idx + 1));

        String id = ((idx == -1) ? jid : jid.substring(0, idx));

        // Parse the localpart and the domain name
        idx = id.indexOf('@');
        result[0] = ((idx == -1) ? null : id.substring(0, idx));
        result[1] = ((idx == -1) ? id : id.substring(idx + 1));

        return result;
    }
}
