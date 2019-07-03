package com.tiza.pub.rp.support.task;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Description: VehicleInfoTask
 * Author: DIYILIU
 * Update: 2019-06-13 10:25
 */

@Slf4j
@Service
public class VehicleInfoTask {
    public static Map<String, String> vehicleMap = new ConcurrentHashMap();

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Scheduled(fixedDelay = 5 * 60 * 1000, initialDelay = 10 * 1000)
    public void execute() {
        log.info("刷新车辆信息 ... ");

        String sql = "SELECT t.vhcle,t.vhvin " +
                "FROM t_vehicle t " +
                "WHERE t.vhvin IS NOT NULL AND t.device_no IS NOT NULL";

        Map temp = new HashMap();
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet(sql);
        while (rowSet.next()) {
            temp.put(rowSet.getString("vhvin"), rowSet.getString("vhcle"));
        }

        Set oldKeys = vehicleMap.keySet();
        Set tempKeys = temp.keySet();

        Collection subKeys = CollectionUtils.subtract(oldKeys, tempKeys);
        for (Iterator iterator = subKeys.iterator(); iterator.hasNext(); ) {
            String key = (String) iterator.next();
            vehicleMap.remove(key);
        }
        vehicleMap.putAll(temp);
    }
}
