package com.pbx.cloudpicbackend;

import com.pbx.cloudpicbackend.manager.CosManager;
import com.pbx.cloudpicbackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;

import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("local")
@Slf4j
public class RedisStringTest {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private UserService userService;

    @Resource
    private CosManager cosManager;

    @Test
    public void testRedisStringOperations() {
        // 获取操作对象
        ValueOperations<String, String> valueOps = stringRedisTemplate.opsForValue();

        // Key 和 Value
        String key = "testKey";
        String value = "testValue";

        // 1. 测试新增或更新操作
        valueOps.set(key, value);
        String storedValue = valueOps.get(key);
        assertEquals(value, storedValue, "存储的值与预期不一致");

        // 2. 测试修改操作
        String updatedValue = "updatedValue";
        valueOps.set(key, updatedValue);
        storedValue = valueOps.get(key);
        assertEquals(updatedValue, storedValue, "更新后的值与预期不一致");

        // 3. 测试查询操作
        storedValue = valueOps.get(key);
        assertNotNull(storedValue, "查询的值为空");
        assertEquals(updatedValue, storedValue, "查询的值与预期不一致");

        // 4. 测试删除操作
        stringRedisTemplate.delete(key);
        storedValue = valueOps.get(key);
        assertNull(storedValue, "删除后的值不为空");
    }

    @Test
    public void digestTest() {
        final String SALT = "lin";
        String password = "123456789";
        System.out.println(DigestUtils.md5DigestAsHex((SALT + password).getBytes()));
    }

    @Test
    public void regTest() {
        final String name = "测试测试";
        System.out.println(userService.validUserAccount(name));
    }

    @Test
    public void cosTest() {
        String string = cosManager.generateSimplePresignedDownloadUrl("public/1924737886867955714/2025-05-20_aHKZtwig7ME6bJkF.webp");
        log.info("url: {}", string);
    }

    @Test
    public void getUri() throws MalformedURLException {
        String url = "https://cloud-pic-1252917097.cos.ap-beijing.myqcloud.com/public/1924737886867955714/2025-05-29_MYfdk1FC7MIwhTZf.jpg";
        URL url1 = new URL(url);
        String path = url1.getPath();
        log.info("path: {}", path);
    }
}