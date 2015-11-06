package us.codecraft.webmagic.scheduler;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import org.apache.commons.codec.digest.DigestUtils;
import redis.clients.jedis.*;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.scheduler.component.DuplicateRemover;

import java.util.ArrayList;
import java.util.List;

/**
 * Use Redis as url scheduler for distributed crawlers.<br>
 *
 * @author code4crafter@gmail.com <br>
 * @since 0.2.0
 */
public class RedisScheduler extends DuplicateRemovedScheduler implements MonitorableScheduler, DuplicateRemover {

    private ShardedJedisPool pool;

    private static final String QUEUE_PREFIX = "queue_";

    private static final String SET_PREFIX = "set_";

    private static final String ITEM_PREFIX = "item_";

    public RedisScheduler(String host) {
        this(shardedJedisPool(host, 30000));
    }

    public RedisScheduler(String host, int timeout) {
        this(shardedJedisPool(host, timeout));
    }

    private static ShardedJedisPool shardedJedisPool(String servers, int timeout) {
        List<JedisShardInfo> shardInfos = Lists.newArrayList();
        // 分拆server信息
        for(String s : servers.split(",")) {
            String[] infos = s.split(":");
            JedisShardInfo jedisShardInfo = null;
            if(infos.length == 1) {
                jedisShardInfo = new JedisShardInfo(infos[0]);
            } else if (infos.length == 2) {
                jedisShardInfo = new JedisShardInfo(infos[0],
                        Integer.parseInt(infos[1]));
            } else {
                jedisShardInfo = new JedisShardInfo(infos[0],
                        Integer.parseInt(infos[1]), timeout, timeout,
                        Integer.parseInt(infos[2]));
            }
            shardInfos.add(jedisShardInfo);
        }

        return new ShardedJedisPool(new JedisPoolConfig(), shardInfos);
    }

    public RedisScheduler(ShardedJedisPool pool) {
        this.pool = pool;
        setDuplicateRemover(this);
    }

    @Override
    public void resetDuplicateCheck(Task task) {
        ShardedJedis jedis = pool.getResource();
        try {
            jedis.del(getSetKey(task));
        } finally {
            jedis.close();
        }
    }

    @Override
    public boolean isDuplicate(Request request, Task task) {
        ShardedJedis jedis = pool.getResource();
        try {
            boolean isDuplicate = jedis.sismember(getSetKey(task), request.getUrl());
            if (!isDuplicate) {
                jedis.sadd(getSetKey(task), request.getUrl());
            }
            return isDuplicate;
        } finally {
            jedis.close();
        }

    }

    @Override
    protected void pushWhenNoDuplicate(Request request, Task task) {
        ShardedJedis jedis = pool.getResource();
        try {
            jedis.rpush(getQueueKey(task), request.getUrl());
            if (request.getExtras() != null) {
                String field = DigestUtils.shaHex(request.getUrl());
                String value = JSON.toJSONString(request);
                jedis.hset((ITEM_PREFIX + task.getUUID()), field, value);
            }
        } finally {
            jedis.close();
        }
    }

    @Override
    public synchronized Request poll(Task task) {
        ShardedJedis jedis = pool.getResource();
        try {
            String url = jedis.lpop(getQueueKey(task));
            if (url == null) {
                return null;
            }
            String key = ITEM_PREFIX + task.getUUID();
            String field = DigestUtils.shaHex(url);
            byte[] bytes = jedis.hget(key.getBytes(), field.getBytes());
            if (bytes != null) {
                Request o = JSON.parseObject(new String(bytes), Request.class);
                return o;
            }
            Request request = new Request(url);
            return request;
        } finally {
            jedis.close();
        }
    }

    protected String getSetKey(Task task) {
        return SET_PREFIX + task.getUUID();
    }

    protected String getQueueKey(Task task) {
        return QUEUE_PREFIX + task.getUUID();
    }

    @Override
    public int getLeftRequestsCount(Task task) {
        ShardedJedis jedis = pool.getResource();
        try {
            Long size = jedis.llen(getQueueKey(task));
            return size.intValue();
        } finally {
            jedis.close();
        }
    }

    @Override
    public int getTotalRequestsCount(Task task) {
        ShardedJedis jedis = pool.getResource();
        try {
            Long size = jedis.scard(getQueueKey(task));
            return size.intValue();
        } finally {
            jedis.close();
        }
    }
}
