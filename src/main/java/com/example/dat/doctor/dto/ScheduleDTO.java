package com.example.dat.doctor.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScheduleDTO {

    private Long id;
    private String dayOfWeek;
    private Boolean isActive;
    private String startTime; // formato "HH:mm"
    private String endTime;   // formato "HH:mm"
    private String lunchStart; // formato "HH:mm"
    private String lunchEnd;   // formato "HH:mm"
}
