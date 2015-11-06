package us.codecraft.webmagic.scheduler;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Sets;
import org.apache.commons.codec.digest.DigestUtils;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.scheduler.component.DuplicateRemover;

import java.util.HashSet;
import java.util.Set;

/**
 * Use Redis as url scheduler for distributed crawlers.<br>
 *
 * @author code4crafter@gmail.com <br>
 * @since 0.2.0
 */
public class RedisClusterScheduler extends DuplicateRemovedScheduler implements MonitorableScheduler, DuplicateRemover {

    private JedisCluster pool;

    private static final String QUEUE_PREFIX = "queue_";

    private static final String SET_PREFIX = "set_";

    private static final String ITEM_PREFIX = "item_";

    private static Set<HostAndPort> hostAndPortSet(String host) {
        Set<HostAndPort> hostAndPorts = Sets.newHashSet();

        // 分拆server信息
        for(String s : host.split(",")) {
            String[] infos = s.split(":");
            HostAndPort jedisShardInfo = null;
            if(infos.length == 1) {
                jedisShardInfo = new HostAndPort(infos[0], 6379);
            } else {
                jedisShardInfo = new HostAndPort(infos[0], Integer.parseInt(infos[1]));
            }
            hostAndPorts.add(jedisShardInfo);
        }

        return hostAndPorts;
    }

    public RedisClusterScheduler(String host) {
        this(new JedisCluster(hostAndPortSet(host), 30000, 30000,
                3, new JedisPoolConfig()));
    }

    public RedisClusterScheduler(String host, int timeout) {
        this(new JedisCluster(hostAndPortSet(host), timeout, timeout,
                3, new JedisPoolConfig()));
    }

    public RedisClusterScheduler(JedisCluster pool) {
        this.pool = pool;
        setDuplicateRemover(this);
    }

    @Override
    public void resetDuplicateCheck(Task task) {
        pool.del(getSetKey(task));
    }

    @Override
    public boolean isDuplicate(Request request, Task task) {
        boolean isDuplicate = pool.sismember(getSetKey(task), request.getUrl());
        if (!isDuplicate) {
            pool.sadd(getSetKey(task), request.getUrl());
        }
        return isDuplicate;
    }

    @Override
    protected void pushWhenNoDuplicate(Request request, Task task) {
            pool.rpush(getQueueKey(task), request.getUrl());
            if (request.getExtras() != null) {
                String field = DigestUtils.shaHex(request.getUrl());
                String value = JSON.toJSONString(request);
                pool.hset((ITEM_PREFIX + task.getUUID()), field, value);
            }
    }

    @Override
    public synchronized Request poll(Task task) {
            String url = pool.lpop(getQueueKey(task));
            if (url == null) {
                return null;
            }
            String key = ITEM_PREFIX + task.getUUID();
            String field = DigestUtils.shaHex(url);
            byte[] bytes = pool.hget(key.getBytes(), field.getBytes());
            if (bytes != null) {
                Request o = JSON.parseObject(new String(bytes), Request.class);
                return o;
            }
            Request request = new Request(url);
            return request;
    }

    protected String getSetKey(Task task) {
        return SET_PREFIX + task.getUUID();
    }

    protected String getQueueKey(Task task) {
        return QUEUE_PREFIX + task.getUUID();
    }

    @Override
    public int getLeftRequestsCount(Task task) {
            Long size = pool.llen(getQueueKey(task));
            return size.intValue();
    }

    @Override
    public int getTotalRequestsCount(Task task) {
            Long size = pool.scard(getQueueKey(task));
            return size.intValue();
    }
}
