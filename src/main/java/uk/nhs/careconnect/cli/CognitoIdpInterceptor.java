package uk.nhs.careconnect.cli;

import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClientBuilder;
import com.amazonaws.services.cognitoidp.model.*;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class CognitoIdpInterceptor implements IClientInterceptor {

    private String apiKey;
    private String userName;
    private String password;
    private String clientId;

    AuthenticationResultType authenticationResult = null;

    CognitoIdpInterceptor(String _apiKey,
                                 String _userName,
                                 String _password,
                                 String _clientId) {
        this.apiKey = _apiKey;
        this.password = _password;
        this.userName = _userName;
        this.clientId = _clientId;
    }

    @Override
    public void interceptRequest(IHttpRequest iHttpRequest) {
        getAccessToken();
        iHttpRequest.addHeader("Authorization", "Bearer "+authenticationResult.getAccessToken());
        iHttpRequest.addHeader("x-api-key", this.apiKey);
    }

    @Override
    public void interceptResponse(IHttpResponse iHttpResponse) throws IOException {
        if (iHttpResponse.getStatus() != 200) {
            System.out.println(iHttpResponse.getStatus());
        }
        // if unauthorised force a token refresh
        if (iHttpResponse.getStatus() == 401) {
            this.authenticationResult = null;
        }
    }


    private AuthenticationResultType getAccessToken() {

        if (this.authenticationResult != null) return authenticationResult;

        AWSCognitoIdentityProvider cognitoClient = AWSCognitoIdentityProviderClientBuilder.standard()
               // .withCredentials(propertiesFileCredentialsProvider)
                .withRegion("eu-west-2")
                .build();

        final Map<String, String> authParams = new HashMap<>();
        authParams.put("USERNAME", this.userName);
        authParams.put("PASSWORD", this.password);

        final InitiateAuthRequest authRequest = new InitiateAuthRequest();
        authRequest.withAuthFlow(AuthFlowType.USER_PASSWORD_AUTH)
                .withClientId(this.clientId)
                .withAuthParameters(authParams);


        InitiateAuthResult result = cognitoClient.initiateAuth(authRequest);
        authenticationResult = result.getAuthenticationResult();
        return authenticationResult;

    }
}
