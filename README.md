# cross

一个由netty+springboot开发的内网穿透工具，使用cross可以实现将内网的服务开放到公网，以便于任何人访问。cross 提供tcp协议层面的支持，可支持web服务，数据库服务等基于tcp的服务

#### 示例

在对外的服务器上启动cross-server：

```
java -jar cross-server.jar --user=user --password=123456 --forward-port=7000 --receive-port=80
```

在可以访问服务器和真实服务的主机上启动 cross-client:

```
java -jar cross-client.jar -u user -p 123456 -rh 127.0.0.1 -rp 8080 -h [服务器ip] -fp 7000
```

