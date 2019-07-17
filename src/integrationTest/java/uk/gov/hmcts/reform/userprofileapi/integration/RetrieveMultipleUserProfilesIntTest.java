package uk.gov.hmcts.reform.userprofileapi.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static uk.gov.hmcts.reform.userprofileapi.data.UserProfileTestDataBuilder.buildUserProfile;
import static uk.gov.hmcts.reform.userprofileapi.data.UserProfileTestDataBuilder.buildUserProfileWithDeletedStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.reform.userprofileapi.client.GetUserProfilesRequest;
import uk.gov.hmcts.reform.userprofileapi.client.GetUserProfilesResponse;
import uk.gov.hmcts.reform.userprofileapi.domain.entities.Audit;
import uk.gov.hmcts.reform.userprofileapi.domain.entities.UserProfile;



@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = MOCK)
@Transactional
public class RetrieveMultipleUserProfilesIntTest extends AuthorizationEnabledIntegrationTest {

    private Map<String, UserProfile> userProfileMap;
    private Map<UUID, UserProfile> userProfileMapWithUuid;
    private List<String> userIds;

    @Autowired
    private WebApplicationContext webApplicationContext;

    public void mockWithGetSuccess() {
        idamService.stubFor(get(urlMatching("/api/v1/users/.*"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("{"
                                + "  \"active\": \"true\","
                                + "  \"forename\": \"fname\","
                                + "  \"surname\": \"lname\","
                                + "  \"email\": \"user@hmcts.net\","
                                + "  \"locked\": \"false\","
                                + "  \"roles\": ["
                                + "    \"pui-case-manager\""
                                + "  ]"
                                + "}")));

    }

    public void mockWithGetFail() {
        idamService.stubFor(get(urlMatching("/api/v1/users/.*"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(404)
                ));

    }

    @Before
    public void setUp() {
        this.mockMvc = webAppContextSetup(webApplicationContext).build();

        testUserProfileRepository.deleteAll();
        auditRepository.deleteAll();

        Iterable<UserProfile> userProfiles = testUserProfileRepository.findAll();
        assertThat(userProfiles).isEmpty();

        //adding 2 userprofiles with PENDING and 2 with DELETED status
        UserProfile user1 = testUserProfileRepository.save(buildUserProfile());
        UserProfile user2 = testUserProfileRepository.save(buildUserProfile());


        userProfileMap = new HashMap<>();
        userProfileMap.put("user1", user1);
        userProfileMap.put("user2", user2);

        UserProfile user3 = testUserProfileRepository.save(buildUserProfileWithDeletedStatus());
        UserProfile user4 = testUserProfileRepository.save(buildUserProfileWithDeletedStatus());
        
        userProfileMap.put("user3", user3);
        userProfileMap.put("user4", user4);

        userProfileMapWithUuid = new HashMap<>();
        userProfileMapWithUuid.put(user1.getIdamId(), user1);
        userProfileMapWithUuid.put(user2.getIdamId(), user2);
        userProfileMapWithUuid.put(user3.getIdamId(), user3);
        userProfileMapWithUuid.put(user4.getIdamId(), user4);

        userIds = new ArrayList<String>();
        userIds.add(user1.getIdamId().toString());
        userIds.add(user2.getIdamId().toString());
        userIds.add(user3.getIdamId().toString());
        userIds.add(user4.getIdamId().toString());
    }

    @Test
    public void should_retrieve_multiple_user_profiles_with_showDeleted_true() throws Exception {

        mockWithGetSuccess();
        GetUserProfilesRequest request = new GetUserProfilesRequest(userIds);
        request.getUserIds().add(UUID.randomUUID().toString());

        GetUserProfilesResponse response = getMultipleUsers(request, OK,"true");

        assertThat(response).isNotNull();
        assertThat(response.getUserProfiles().size()).isEqualTo(4);

        response.getUserProfiles().forEach(getUserProfilesResponse -> {
            UserProfile up =  userProfileMapWithUuid.get(getUserProfilesResponse.getIdamId());
            assertThat(getUserProfilesResponse.getEmail()).isEqualTo(up.getEmail());
            assertThat(getUserProfilesResponse.getFirstName()).isEqualTo(up.getFirstName());
            assertThat(getUserProfilesResponse.getLastName()).isEqualTo(up.getLastName());
            assertThat(getUserProfilesResponse.getIdamStatus()).isEqualTo(up.getStatus());
            assertThat(getUserProfilesResponse.getRoles().size()).isEqualTo(1);
            assertThat(getUserProfilesResponse.getRoles().get(0)).isEqualTo("pui-case-manager");
        });

        Audit audit1 = auditRepository.findByUserProfile(userProfileMap.get("user1")).orElse(null);
        assertThat(audit1).isNotNull();
        assertThat(audit1.getIdamRegistrationResponse()).isEqualTo(200);

        Audit audit2 = auditRepository.findByUserProfile(userProfileMap.get("user2")).orElse(null);
        assertThat(audit2).isNotNull();
        assertThat(audit2.getIdamRegistrationResponse()).isEqualTo(200);

        Audit audit3 = auditRepository.findByUserProfile(userProfileMap.get("user3")).orElse(null);
        assertThat(audit3).isNotNull();
        assertThat(audit3.getIdamRegistrationResponse()).isEqualTo(200);

        Audit audit4 = auditRepository.findByUserProfile(userProfileMap.get("user4")).orElse(null);
        assertThat(audit4).isNotNull();
        assertThat(audit4.getIdamRegistrationResponse()).isEqualTo(200);
    }

    @Test
    public void should_retrieve_multiple_user_profiles_with_showDeleted_false() throws Exception {

        GetUserProfilesRequest request = new GetUserProfilesRequest(userIds);

        GetUserProfilesResponse response = getMultipleUsers(request, OK,"false");

        assertThat(response).isNotNull();
        assertThat(response.getUserProfiles().size()).isEqualTo(2);
    }

    @Test
    public void should_retrieve_multiple_user_profiles_with_idam_failure() throws Exception {

        mockWithGetFail();
        GetUserProfilesRequest request = new GetUserProfilesRequest(userIds);
        request.getUserIds().add(UUID.randomUUID().toString());

        GetUserProfilesResponse response = getMultipleUsers(request, OK, "true");

        assertThat(response).isNotNull();
        assertThat(response.getUserProfiles().size()).isEqualTo(4);

        response.getUserProfiles().forEach(getUserProfilesResponse -> {
            UserProfile up =  userProfileMapWithUuid.get(getUserProfilesResponse.getIdamId());
            assertThat(getUserProfilesResponse.getEmail()).isEqualTo(up.getEmail());
            assertThat(getUserProfilesResponse.getFirstName()).isEqualTo(up.getFirstName());
            assertThat(getUserProfilesResponse.getLastName()).isEqualTo(up.getLastName());
            assertThat(getUserProfilesResponse.getIdamStatus()).isEqualTo(up.getStatus());
            assertThat(getUserProfilesResponse.getRoles().size()).isEqualTo(0);
            assertThat(getUserProfilesResponse.getIdamErrorMessage()).isNotEmpty();
            assertThat(getUserProfilesResponse.getIdamErrorStatusCode()).isEqualTo(404);
        });

        Audit audit1 = auditRepository.findByUserProfile(userProfileMap.get("user1")).orElse(null);
        assertThat(audit1).isNotNull();
        assertThat(audit1.getIdamRegistrationResponse()).isEqualTo(404);

        Audit audit2 = auditRepository.findByUserProfile(userProfileMap.get("user2")).orElse(null);
        assertThat(audit2).isNotNull();
        assertThat(audit2.getIdamRegistrationResponse()).isEqualTo(404);

        Audit audit3 = auditRepository.findByUserProfile(userProfileMap.get("user3")).orElse(null);
        assertThat(audit3).isNotNull();
        assertThat(audit3.getIdamRegistrationResponse()).isEqualTo(404);

        Audit audit4 = auditRepository.findByUserProfile(userProfileMap.get("user4")).orElse(null);
        assertThat(audit4).isNotNull();
        assertThat(audit4.getIdamRegistrationResponse()).isEqualTo(404);
    }

    @Test
    public void should_return_400_multiple_user_profiles_with_invalid_param() throws Exception {

        GetUserProfilesRequest request = new GetUserProfilesRequest(userIds);
        getMultipleUsers(request, HttpStatus.BAD_REQUEST, "invalid");
    }

    @Test
    public void should_return_400_multiple_user_profiles_with_no_user_ids_in_request() throws Exception {

        GetUserProfilesRequest request = new GetUserProfilesRequest(new ArrayList<String>());
        getMultipleUsers(request, HttpStatus.BAD_REQUEST, "true");
    }

    @Test
    public void should_return_404_multiple_user_profiles_with_user_ids_not_in_db() throws Exception {

        List<String> userIdList = new ArrayList<String>();
        userIdList.add(UUID.randomUUID().toString());
        userIdList.add(UUID.randomUUID().toString());
        GetUserProfilesRequest request = new GetUserProfilesRequest(new ArrayList<String>());

        getMultipleUsers(request, HttpStatus.BAD_REQUEST, "true");
    }

}
