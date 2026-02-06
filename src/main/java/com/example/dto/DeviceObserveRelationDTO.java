package com.example.dto;

import lombok.Data;
import org.eclipse.californium.core.observe.ObserveRelation;

@Data
public class DeviceObserveRelationDTO {
    private String sn;

    private String token;

    private String object;

}
