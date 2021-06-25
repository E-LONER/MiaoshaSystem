package com.loner.rabbitmq;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;



@Configuration
public class MQconfig {

    public static final String Miaosha_QUEUE_NAME="miaosha.queue";

    /**
     * rabbitmq直连模式
     */
    //创建队列
    @Bean
    public Queue queue(){
        return new Queue(Miaosha_QUEUE_NAME,true);
    }

    /**
     * rabbitmq topic模式
     */


    /**
     * rabbitmq 广播模式
     */


    /**
     * rabbitmq Header模式
     */

}
