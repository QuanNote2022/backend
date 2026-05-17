package com.mineral.dto;

import lombok.Data;
import java.util.List;

@Data
public class SearchResult {
    private List<MineralInfoResponse> minerals;
    private List<ChatSessionResponse> sessions;
}
