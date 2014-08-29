package com.softmotions.web.security.tomcat;

import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.GeneralException;
import com.nimbusds.oauth2.sdk.TokenErrorResponse;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.util.URLUtils;

import org.apache.catalina.Realm;
import org.apache.catalina.Session;
import org.apache.catalina.authenticator.Constants;
import org.apache.catalina.authenticator.FormAuthenticator;
import org.apache.catalina.connector.Request;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Tyutyunkov Vyacheslav (tve@softmotions.com)
 * @version $Id$
 */
public class MyNSUOAuth2Authenticator extends FormAuthenticator {

    private static final Log log = LogFactory.getLog(MyNSUOAuth2Authenticator.class);

    /**
     * OAuth2 client id
     */
    protected String clientId;

    /**
     * OAuth2 client secret
     */
    protected String clientSecret;

    /**
     * OAuth2 authorize endpoint
     */
    protected String authEndpoint;

    /**
     * OAuth2 token endpoint
     */
    protected String tokenEndpoint;

    /**
     * User info endpoint
     */
    protected String userinfoEndpoint;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getAuthEndpoint() {
        return authEndpoint;
    }

    public void setAuthEndpoint(String authEndpoint) {
        this.authEndpoint = authEndpoint;
    }

    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    public void setTokenEndpoint(String tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }

    public String getUserinfoEndpoint() {
        return userinfoEndpoint;
    }

    public void setUserinfoEndpoint(String userinfoEndpoint) {
        this.userinfoEndpoint = userinfoEndpoint;
    }

    public boolean authenticate(Request request, HttpServletResponse response, LoginConfig config) throws IOException {
        Principal principal = null;
        boolean loginAction = false;
        boolean isAuthenticated = false;
        String requestURI = null;
        Realm realm = null;
        String openID = null;

        // References to objects we will need later
        Session session = null;

        principal = request.getPrincipal();
        if (principal != null) {
            // We are here because we have being authenticated successfully, before.
            return true;
        }

        // Check whether this is a re-submit of the original request URI after successful
        // authentication? If so, forward the *original* request instead.
        if (matchRequest(request)) {
            return matchRequest(request, response, config);
        }

        // This request came from the login page - let me login - here are my credentials.
        loginAction = (request.getParameter(Constants.FORM_USERNAME) != null);
        if (!loginAction) {
            if (!StringUtils.isBlank(request.getParameter("code"))) {
                try {
                    principal = doOAuth2Authentication(request, response);
                } catch (GeneralException ignored) {
                    forwardToErrorPage(request, response, config);
                    return false;
                }
            } else {
                try {
                    session = request.getSessionInternal(true);
                    saveRequest(request, session);
                } catch (IOException ignored) {
                    return false;
                }
                request.getSession().setAttribute("requestURI", requestURI);
                forwardToLoginPage(request, response, config);
                return false;
            }
        }

        if (principal == null) {
            forwardToErrorPage(request, response, config);
            return false;
        }

        session = request.getSessionInternal(false);

        // Save the authenticated Principal in our session
        session.setNote(Constants.FORM_PRINCIPAL_NOTE, principal);

        // Save the user login and no password for OAuth2
        session.setNote(Constants.SESS_USERNAME_NOTE, principal.getName());
        session.setNote(Constants.SESS_PASSWORD_NOTE, "");

        // Redirect the user to the original request URI
        // (which will cause the original request to be restored)
        requestURI = savedRequestURL(session);
        try {
            response.sendRedirect(response.encodeRedirectURL(requestURI != null ? requestURI : "/"));
        } catch (IOException ignored) {
            return false;
        }

        return false;
    }

    private Principal doOAuth2Authentication(Request request, HttpServletResponse response) throws IOException, GeneralException {
        Session session = request.getSessionInternal(true);

        ClientSecretBasic auth = new ClientSecretBasic(new ClientID(clientId), new Secret(clientSecret));

        String code = request.getParameter("code");
        String rootUrl = request.getRequestURL().substring(0, request.getRequestURL().length() - request.getRequestURI().length());
        AuthorizationCodeGrant grant = new AuthorizationCodeGrant(new AuthorizationCode(code), URI.create(rootUrl + Constants.FORM_ACTION));

        TokenRequest tokenRequest = new TokenRequest(URI.create(tokenEndpoint), auth, grant);
        TokenResponse tokenResponse = TokenResponse.parse(tokenRequest.toHTTPRequest().send());

        if (tokenResponse instanceof TokenErrorResponse) {
            throw new GeneralException(((TokenErrorResponse) tokenResponse).getErrorObject().getDescription());
        } else if (!(tokenResponse instanceof AccessTokenResponse)) {
            throw new RuntimeException("Unexpected response from IP token endpoint");
        }

        AccessTokenResponse accessTokenResponse = (AccessTokenResponse) tokenResponse;

        Map<String, String> params = new HashMap<>();
        params.put("client_id", clientId);
        params.put("access_token", accessTokenResponse.getAccessToken().getValue());

        HTTPRequest r = new HTTPRequest(HTTPRequest.Method.GET, new URL(userinfoEndpoint));
        r.setQuery(URLUtils.serializeParameters(params));

        HTTPResponse uiResponse = r.send();
        String email = (String) uiResponse.getContentAsJSONObject().get("email");
        if (StringUtils.isBlank(email)) {
            throw new RuntimeException("Missing required field in response from userinfo endpoint");
        }

        Realm realm = request.getContext().getRealm();

        if (!(realm instanceof WSUserDatabaseRealm)) {
            realm = new WSUserDatabaseRealm();
            context.setRealm(realm);
        }

        return ((WSUserDatabaseRealm) realm).getPrincipal(email);
    }

    /**
     * Check whether this is a re-submit of the original request URI after successful
     * authentication? If so, forward the *original* request instead.
     *
     * @param request  Request we are processing
     * @param response Response we are creating
     * @param config   Login configuration describing how authentication should be performed
     */
    private boolean matchRequest(Request request, HttpServletResponse response, LoginConfig config) throws IOException {
        Session session = null;
        Principal principal = null;

        session = request.getSessionInternal(true);
        principal = (Principal) session.getNote(Constants.FORM_PRINCIPAL_NOTE);
        register(request,
                 response,
                 principal,
                 Constants.FORM_METHOD,
                 (String) session.getNote(Constants.SESS_USERNAME_NOTE),
                 (String) session.getNote(Constants.SESS_PASSWORD_NOTE));

        // If we're caching principals we no longer need the username
        // and password in the session, so remove them
        if (cache) {
            session.removeNote(Constants.SESS_USERNAME_NOTE);
            session.removeNote(Constants.SESS_PASSWORD_NOTE);
        }
        try {
            if (restoreRequest(request, session)) {
                return true;
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return false;
            }
        } catch (IOException ignored) {
            forwardToErrorPage(request, response, config);
            return false;
        }
    }
}
