# cross

一个由netty+springboot开发的内网穿透工具，通过简单的配置，使用cross可以实现将内网的服务开放到公网，以便于任何人访问。cross 提供tcp协议层面的支持，包括但不限于web应用程序、数据库服务和其他基于TCP的系统

![4](https://github.com/user-attachments/assets/d1604180-107e-401a-9e7e-3e2c5a1b2f41)

#### 示例

在对外的服务器上启动cross-server：

```
java -jar cross-server.jar --user=user --password=123456 --forward-port=7000 --receive-port=80
```

在可以访问服务器和真实服务的主机上启动 cross-client:

```
java -jar cross-client.jar -u user -p 123456 -rh 127.0.0.1 -rp 8080 -h [服务器ip] -fp 7000
```

#### 基准测试

jmeter并发测试配置（以下为本地环回测试结果）

![jmeter配置](https://github.com/user-attachments/assets/9f4ae946-3908-4523-ae3c-7ab5d52a20be)


cross测试结果:

![cross结果](https://github.com/user-attachments/assets/9cd36471-0bb6-41e4-90ec-6d32d1dc2c8a)



[frp](https://github.com/fatedier/frp) 测试结果：

![frp结果](https://github.com/user-attachments/assets/13001a08-abe3-4eb5-bc26-1545a6dcb4f3)
