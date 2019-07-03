package com.tiza.pub.rp.support.config;

import com.tiza.pub.air.entry.KafkaUtil;
import kafka.javaapi.producer.Producer;
import kafka.producer.ProducerConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Properties;

/**
 * Description: Gb6Config
 * Author: DIYILIU
 * Update: 2019-07-03 10:05
 */

@Slf4j
@Configuration
@EnableScheduling
public class Gb6Config implements InitializingBean {

    @Value("${kafka.brokers}")
    private String brokers;

    @Override
    public void afterPropertiesSet() {
        // 创建 kafka 工具类
        buildKafka();
    }

    public void buildKafka() {
        Properties props = new Properties();
        props.put("metadata.broker.list", brokers);
        // 消息传递到broker时的序列化方式
        props.put("key.serializer.class", "kafka.serializer.StringEncoder");
        props.put("serializer.class", "kafka.serializer.DefaultEncoder");
        // 是否获取反馈
        props.put("request.required.acks", "1");
        // 内部发送数据是异步还是同步 sync：同步(来一条数据提交一条不缓存), 默认 async：异步
        props.put("producer.type", "async");
        // 重试次数
        props.put("message.send.max.retries", "3");
        Producer producer = new Producer(new ProducerConfig(props));
        KafkaUtil.getInstance().init(producer);
    }
}
