package com.tiza.pub.air.model;

import lombok.Data;

/**
 * Description: DataMsg
 * Author: DIYILIU
 * Update: 2019-07-03 09:38
 */

@Data
public class DataMsg {

    private String device;

    private Integer cmd;

    private Long time;

    private String data;
}
