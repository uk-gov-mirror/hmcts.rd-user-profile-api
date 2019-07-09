package uk.gov.hmcts.reform.userprofileapi.service;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.userprofileapi.client.CreateUserProfileData;
import uk.gov.hmcts.reform.userprofileapi.client.RoleRequest;
import uk.gov.hmcts.reform.userprofileapi.domain.IdamRegistrationInfo;
import uk.gov.hmcts.reform.userprofileapi.domain.IdamRolesInfo;

import java.util.List;

@Component
public interface IdentityManagerService {

    IdamRegistrationInfo registerUser(CreateUserProfileData requestData);

    IdamRolesInfo getUserById(String id);

    IdamRolesInfo searchUserByEmail(String email);

    IdamRolesInfo updateUserRoles(List roleRequest, String userId);

}