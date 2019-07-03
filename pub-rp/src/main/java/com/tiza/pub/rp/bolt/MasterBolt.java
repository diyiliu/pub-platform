package com.tiza.pub.rp.bolt;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Tuple;
import com.tiza.pub.air.cache.ICache;
import com.tiza.pub.air.model.DataMsg;
import com.tiza.pub.air.model.Gb6Header;
import com.tiza.pub.air.util.CommonUtil;
import com.tiza.pub.air.util.JacksonUtil;
import com.tiza.pub.air.util.SpringUtil;
import com.tiza.pub.rp.support.protocol.Gb6DataProcess;
import com.tiza.pub.rp.support.task.VehicleInfoTask;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Map;

/**
 * Description: MasterBolt
 * Author: DIYILIU
 * Update: 2019-07-03 09:34
 */

@Slf4j
public class MasterBolt extends BaseRichBolt {

    private OutputCollector collector;

    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        this.collector = collector;
        // 加载 spring 容器
        SpringUtil.init();
    }

    @Override
    public void execute(Tuple tuple) {
        collector.ack(tuple);

        String kafkaMsg = tuple.getString(0);
        try {
            DataMsg msg = JacksonUtil.toObject(kafkaMsg, DataMsg.class);
            String device = msg.getDevice();
            if (!VehicleInfoTask.vehicleMap.containsKey(device)) {
                log.warn("车辆[{}]信息不存在!", device);
                return;
            }
            String vehicle = VehicleInfoTask.vehicleMap.get(device);

            int cmd = msg.getCmd();
            String data = msg.getData();
            byte[] bytes = CommonUtil.hexStringToBytes(data);

            ICache cmdCacheProvider = SpringUtil.getBean("cmdCacheProvider");
            Gb6DataProcess process = (Gb6DataProcess) cmdCacheProvider.get(cmd);
            if (process != null) {
                Gb6Header header = (Gb6Header) process.parseHeader(bytes);
                header.setVehicle(vehicle);
                header.setTime(msg.getTime());

                process.parse(header.getBytes(), header);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {

    }
}
