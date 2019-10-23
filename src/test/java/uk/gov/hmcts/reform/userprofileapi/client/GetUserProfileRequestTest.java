package uk.gov.hmcts.reform.userprofileapi.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import uk.gov.hmcts.reform.userprofileapi.controller.request.UserProfileDataRequest;

public class GetUserProfileRequestTest {


    @Test
    public void should_hold_values_after_creation() {

        List<String> userIds = new ArrayList<String>();
        userIds.add(UUID.randomUUID().toString());
        UserProfileDataRequest getUserProfileRequest = new UserProfileDataRequest(userIds);


        assertThat(getUserProfileRequest.getUserIds().size()).isEqualTo(1);

    }

    @Test
    public void should_hold_null_values_after_creation() {

        List<String> userIds = new ArrayList<String>();
        userIds.add(UUID.randomUUID().toString());
        UserProfileDataRequest getUserProfileRequest = new UserProfileDataRequest(null);
        assertThat(getUserProfileRequest.getUserIds()).isNull();
        getUserProfileRequest.setUserIds(userIds);
        assertThat(getUserProfileRequest.getUserIds()).isNotNull();
        assertThat(getUserProfileRequest.getUserIds().size()).isEqualTo(1);

        getUserProfileRequest.setUserIds(null);
        assertThat(getUserProfileRequest.getUserIds()).isNull();


    }
}
