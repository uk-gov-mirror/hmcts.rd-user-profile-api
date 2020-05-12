package uk.gov.hmcts.reform.userprofileapi.controller.response;

import static uk.gov.hmcts.reform.userprofileapi.util.IdamStatusResolver.resolveStatusAndReturnMessage;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseEntity;

@Data
@NoArgsConstructor
public class RoleAdditionResponse {

    private String idamStatusCode;
    private String idamMessage;

    public RoleAdditionResponse(ResponseEntity responseEntity) {
        this.idamStatusCode = String.valueOf(responseEntity.getStatusCode().value());
        this.idamMessage = resolveStatusAndReturnMessage(responseEntity);
    }
}
