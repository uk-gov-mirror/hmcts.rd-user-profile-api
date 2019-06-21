package uk.gov.hmcts.reform.userprofileapi.infrastructure.clients;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.reform.userprofileapi.domain.entities.UserProfile;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class GetUserProfileWithRolesResponse extends GetUserProfileResponse {

    private List<String> roles;
    public GetUserProfileWithRolesResponse(UserProfile userProfile) {
        super(userProfile);
        roles = userProfile.getRoles();
    }
}
