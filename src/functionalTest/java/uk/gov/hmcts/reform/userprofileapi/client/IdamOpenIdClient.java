package uk.gov.hmcts.reform.userprofileapi.client;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.userprofileapi.AbstractFunctional.EMAIL;
import static uk.gov.hmcts.reform.userprofileapi.AbstractFunctional.CREDS;
import static uk.gov.hmcts.reform.userprofileapi.helper.CreateUserProfileTestDataBuilder.generateRandomEmail;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.mifmif.common.regex.Generex;
import io.restassured.RestAssured;
import io.restassured.response.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.userprofileapi.config.TestConfigProperties;
import uk.gov.hmcts.reform.userprofileapi.resource.UserProfileCreationData;

@Slf4j
public class IdamOpenIdClient {

    private final TestConfigProperties testConfig;

    public static String password;

    private Gson gson = new Gson();

    private static String bearerToken;

    public IdamOpenIdClient(TestConfigProperties testConfig) {
        this.testConfig = testConfig;
    }

    public Map<String, String> createUser(List<String> roles) {
        log.info(":::: Creating a User");

        //Generating a random user
        String userEmail = generateRandomEmail();
        String firstName = "First";
        String lastName = "Last";
        String password = generateSidamPassword();

        String id = UUID.randomUUID().toString();

        List<Role> rolesList = roles.stream().map(Role::new).collect(Collectors.toList());

        User user = new User(userEmail, firstName, id, lastName, password, rolesList);

        String serializedUser = gson.toJson(user);

        Response createdUserResponse = null;

        for (int i = 0; i < 5; i++) {
            log.info("SIDAM createUser retry attempt : " + i + 1);
            createdUserResponse = RestAssured
                    .given()
                    .relaxedHTTPSValidation()
                    .baseUri(testConfig.getIdamApiUrl())
                    .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                    .body(serializedUser)
                    .post("/testing-support/accounts")
                    .andReturn();
            if (createdUserResponse.getStatusCode() == 504) {
                log.info("SIDAM createUser retry response for attempt " + i + 1 + " 504");
            } else {
                break;
            }
        }

        assertThat(createdUserResponse.getStatusCode()).isEqualTo(201);

        Map<String, String> userCreds = new HashMap<>();
        userCreds.put(EMAIL, userEmail);
        userCreds.put(CREDS, password);
        return userCreds;
    }

    public Map<String, String> createUserWithGivenFields(List<String> roles,
                                                         UserProfileCreationData userProfileCreationData) {

        log.info(":::: Creating a User");

        String userEmail = userProfileCreationData.getEmail();
        String firstName = userProfileCreationData.getFirstName();
        String lastName = userProfileCreationData.getLastName();
        String password = generateSidamPassword();

        String id = UUID.randomUUID().toString();

        List<Role> rolesList = roles.stream().map(Role::new).collect(Collectors.toList());

        User user = new User(userEmail, firstName, id, lastName, password, rolesList);

        String serializedUser = gson.toJson(user);

        Response createdUserResponse = RestAssured
                .given()
                .relaxedHTTPSValidation()
                .baseUri(testConfig.getIdamApiUrl())
                .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .body(serializedUser)
                .post("/testing-support/accounts")
                .andReturn();


        assertThat(createdUserResponse.getStatusCode()).isEqualTo(201);

        Map<String, String> userCreds = new HashMap<>();
        userCreds.put(EMAIL, userEmail);
        userCreds.put(CREDS, password);
        return userCreds;
    }

    public String getBearerToken() {
        if (null == bearerToken) {
            log.info(":::: Generating Bearer Token");
            List<String> roles = new ArrayList<>();
            roles.add("prd-admin");
            Map<String, String> userCreds = createUser(roles);
            Map<String, String> tokenParams = new HashMap<>();
            tokenParams.put("grant_type", "password");
            tokenParams.put("username", userCreds.get(EMAIL));
            tokenParams.put("password", userCreds.get(CREDS));
            tokenParams.put("client_id", testConfig.getClientId());
            tokenParams.put("client_secret", testConfig.getClientSecret());
            tokenParams.put("redirect_uri", testConfig.getOauthRedirectUrl());
            tokenParams.put("scope", "openid profile roles manage-user create-user search-user");

            Response bearerTokenResponse = RestAssured
                    .given()
                    .relaxedHTTPSValidation()
                    .baseUri(testConfig.getIdamApiUrl())
                    .header(CONTENT_TYPE, APPLICATION_FORM_URLENCODED_VALUE)
                    .params(tokenParams)
                    .post("/o/token")
                    .andReturn();

            assertThat(bearerTokenResponse.getStatusCode()).isEqualTo(200);

            BearerTokenResponse accessTokenResponse = gson.fromJson(bearerTokenResponse.getBody().asString(),
                    BearerTokenResponse.class);

            bearerToken = accessTokenResponse.getAccessToken();
        }
        return bearerToken;
    }

    public static String generateSidamPassword() {
        if (isBlank(password)) {
            password = new Generex("([A-Z])([a-z]{4})([0-9]{4})").random();
        }
        return password;
    }

    @AllArgsConstructor
    class User {
        private String email;
        private String forename;
        private String id;
        private String surname;
        private String password;
        private List<Role> roles;
    }

    @AllArgsConstructor
    class Role {
        private String code;
    }

    @AllArgsConstructor
    class Group {
        private String code;
    }

    @Getter
    @AllArgsConstructor
    class AuthorizationResponse {
        private String code;
    }

    @Getter
    @AllArgsConstructor
    class BearerTokenResponse {
        @SerializedName("access_token")
        private String accessToken;

        @SerializedName("id_token")
        private String idToken;

    }
}
