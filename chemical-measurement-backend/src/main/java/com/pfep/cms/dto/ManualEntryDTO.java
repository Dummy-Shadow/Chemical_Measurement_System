package com.pfep.cms.dto;

import lombok.Data;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
public class ManualEntryDTO {
    @NotNull private Long stationId;
    @NotNull private Long mediaId;
    @NotNull private LocalDate inspectionDate;
    @NotNull private List<IndicatorValue> values;

    @Data
    public static class IndicatorValue {
        @NotNull private Long indicatorId;
        private BigDecimal value;
    }
}
