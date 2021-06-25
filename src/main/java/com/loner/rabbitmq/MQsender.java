package com.loner.rabbitmq;

import com.loner.utils.ObjectConverter;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class MQsender {

    private static Logger log=LoggerFactory.getLogger(MQsender.class);
    @Autowired
    AmqpTemplate template;

    public void sendMiaoshaMsg(MiaoshaMsg msgs){
        String msg= ObjectConverter.toString(msgs);
        log.info("MQ发送信息："+msg);
        template.convertAndSend(MQconfig.Miaosha_QUEUE_NAME,msg);
    }
}
