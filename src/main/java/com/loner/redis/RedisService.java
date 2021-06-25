package com.loner.redis;

import com.loner.redis.keyprefix.Goods;
import com.loner.redis.keyprefix.User;
import com.loner.utils.ObjectConverter;
import org.omg.PortableInterceptor.ServerRequestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.validation.ObjectError;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@Service
public class RedisService {
    //将Redis配置注入进来
    @Autowired
    RedisConfig redisConfig;

    //使用Bean注入RedisPool，使之获取jedispool
    @Autowired
    JedisPool jedisPool;

    //jedispool得bean，相当于ssm中得xml中的bean
    @Bean
    public JedisPool jedisPoolFactory(){
        JedisPoolConfig poolConfig=new JedisPoolConfig();
        poolConfig.setMaxIdle(redisConfig.getPoolMaxIdle());
        poolConfig.setMaxTotal(redisConfig.getPoolMaxTotal());
        poolConfig.setMaxWaitMillis(redisConfig.getPoolMaxWait()*1000);
        JedisPool jedisPool=new JedisPool(poolConfig,redisConfig.getHost(),redisConfig.getPort(),redisConfig.getTimeout()*1000,redisConfig.getPassword(),0);
        return jedisPool;
    }

    //从jedispool中取出jedis执行操作
    public <T>T get(String key,Class<T> tClass){
        Jedis jedis=null;
        try{
            jedis=jedisPool.getResource();
            String res=jedis.get(key );
            if (res==null && tClass==Long.class)
                return ObjectConverter.StringTo("0",tClass);
            return ObjectConverter.StringTo(res,tClass);
        }catch (Exception e){
            e.printStackTrace();
        }finally{
            if(jedis!=null){
               jedis.close() ;
            }
        }
    return null;
    }

    //Redis保存键值对
    public <T>String set(String key,int expireTime, T value){
        Jedis jedis=null;
        try{
            jedis=jedisPool.getResource();
            String res=jedis.setex(key,expireTime, ObjectConverter.toString(value));
            return res;
        }catch (Exception e){
            e.printStackTrace();
        }finally{
            if(jedis!=null){
                jedis.close();
            }
        }
        return null;
    }

    //redis缓存减一
    public long decr(String key){
        Jedis jedis=null;
        try {
            jedis = jedisPool.getResource();
            return jedis.decr(key);
        }catch(Exception e){
            e.printStackTrace();
        }finally {
            if (jedis!=null)
                jedis.close();
        }
        return -1;
    }


}
