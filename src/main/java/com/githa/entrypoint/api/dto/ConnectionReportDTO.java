package com.githa.entrypoint.api.dto;

import com.githa.core.domain.SessionIdentity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionReportDTO {
    private Long accountGroupId;
    private List<SessionIdentity> connections;
}
