package com.tiza.pub.air.entry;

import com.google.common.collect.Maps;
import com.tiza.pub.air.model.KafkaMsg;
import com.tiza.pub.air.util.JacksonUtil;
import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Description: KafkaUtil
 * Author: DIYILIU
 * Update: 2019-04-24 14:26
 */

@Slf4j
public class KafkaUtil {
    private final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(2);

    private final static Queue<KafkaMsg> pool = new ConcurrentLinkedQueue();
    private static String upTopic;
    private static String downTopic;

    private static KafkaUtil kafkaUtil = new KafkaUtil();
    private KafkaUtil() {
    }

    public static KafkaUtil getInstance() {
        return kafkaUtil;
    }

    public void init(Producer producer, String upTopic, String downTopic) {
        this.upTopic = upTopic;
        this.downTopic = downTopic;

        // 上行
        scheduledExecutor.scheduleWithFixedDelay(() -> {
            while (!pool.isEmpty()) {
                KafkaMsg data = pool.poll();
                producer.send(new KeyedMessage(data.getTopic(), data.getKey(), data.getValue()));
            }
        }, 5, 1, TimeUnit.SECONDS);
    }

    /**
     * 写入 kafka
     *
     * @param id
     * @param cmd
     * @param value
     * @param flow  1: 上行; 2: 下行;
     */
    public static void send(String id, int cmd, String value, int flow) {
        Map map = Maps.newHashMap();
        map.put("device", id);
        map.put("cmd", cmd);
        map.put("time", System.currentTimeMillis());
        map.put("data", value);
        // map.put("flow", flow);

        KafkaMsg msg = new KafkaMsg();
        msg.setKey(id);
        msg.setValue(JacksonUtil.toJson(map));

        // 上行
        if (flow == 1) {
            msg.setTopic(upTopic);
            pool.add(msg);
        }
        // 下行
        else if (flow == 2) {
            msg.setTopic(downTopic);
            pool.add(msg);
        }
        // 打印日志
        log.info("[{}]设备[{}]写入 Kafka [{}] ...", flow == 1 ? "上行" : "下行", msg.getKey(), msg.getValue());
    }
}
