package net.acardenas.jmeter.plugins.authDigestE;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.auth.AUTH;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.message.BasicHeaderValueFormatter;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.message.BufferedHeader;
import org.apache.http.util.CharArrayBuffer;

/**
 * User: acardenas
 */
public class DigestESchema extends DigestScheme
{
    @Override
    public String getSchemeName()
    {
        return "digeste";
    }

    private String encryptPwd(String aPassword) throws IOException,
            NoSuchAlgorithmException
    {
        String password = aPassword;
        String myReturn = null;
        final MessageDigest md = MessageDigest.getInstance("SHA1");
        ByteArrayOutputStream pwsalt = new ByteArrayOutputStream();
        pwsalt.write(password.getBytes("UTF-8"));
        byte[] unhashedBytes = pwsalt.toByteArray();
        byte[] digestVonPassword = md.digest(unhashedBytes);
        myReturn = convertToHexString(digestVonPassword);
        System.out.println(myReturn);
        return myReturn;
    }

    private static String convertToHexString(byte[] data)
    {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++)
        {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do
            {
                if ((0 <= halfbyte) && (halfbyte <= 9))
                    buf.append((char) ('0' + halfbyte));
                else
                    buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[i] & 0x0F;
            }
            while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    public Header authenticate(final Credentials credentials,
            final HttpRequest request) throws AuthenticationException
    {
        String uri = getParameter("uri");
        String realm = getParameter("realm");
        String nonce = getParameter("nonce");
        String opaque = getParameter("opaque");
        String method = getParameter("methodname");
        String algorithm = getParameter("algorithm");
        String uname = credentials.getUserPrincipal().getName();
        String myPassword = credentials.getPassword();
        UsernamePasswordCredentials myCredentials = null;

        String myResponseHeader = null;
        String myAuthorizationHeader = null;
        try
        {
            myPassword = encryptPwd(myPassword);
            System.out.println(myPassword);
            System.out.println(credentials.getUserPrincipal().getName());
            String ha1 = encryptPwd(credentials.getUserPrincipal().getName()
                    + ":" + realm + ":" + myPassword);
            System.out.println(ha1);
            myResponseHeader = encryptPwd(ha1 + ":" + nonce);
            System.out.println("Response Header = " + myResponseHeader);
            myCredentials = new UsernamePasswordCredentials(
                    "alejandro.cardenas@ecofactor.com", myPassword);

            myAuthorizationHeader = "DigestE " + "username=" + "\""
                    + credentials.getUserPrincipal().getName() + "\", "
                    + "realm=" + "\"" + realm + "\", " + "nonce=" + "\""
                    + nonce + "\", " + "response=" + "\"" + myResponseHeader
                    + "\", " + "opaque=" + "\"" + opaque + "\"";

            System.out.println(" AuthorizationHeader = "
                    + myAuthorizationHeader);

        }
        catch (IOException e)
        {
            e.printStackTrace(); // To change body of catch statement use File |
                                 // Settings | File Templates.
        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace(); // To change body of catch statement use File |
                                 // Settings | File Templates.
        }

        CharArrayBuffer buffer = new CharArrayBuffer(128);
        if (isProxy())
        {
            buffer.append(AUTH.PROXY_AUTH_RESP);
        }
        else
        {
            buffer.append(AUTH.WWW_AUTH_RESP);
        }
        buffer.append(": Digest ");

        List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>(20);
        params.add(new BasicNameValuePair("username", uname));
        params.add(new BasicNameValuePair("realm", realm));
        params.add(new BasicNameValuePair("nonce", nonce));
        params.add(new BasicNameValuePair("response", myResponseHeader));

        if (algorithm != null)
        {
            params.add(new BasicNameValuePair("algorithm", algorithm));
        }
        if (opaque != null)
        {
            params.add(new BasicNameValuePair("opaque", opaque));
        }

        for (int i = 0; i < params.size(); i++)
        {
            BasicNameValuePair param = params.get(i);
            if (i > 0)
            {
                buffer.append(", ");
            }
            boolean noQuotes = "nc".equals(param.getName())
                    || "qop".equals(param.getName());
            BasicHeaderValueFormatter.DEFAULT.formatNameValuePair(buffer,
                    param, !noQuotes);
        }
        return new BufferedHeader(buffer);
    }

}