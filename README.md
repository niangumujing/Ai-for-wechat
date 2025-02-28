## 项目简介

本项目用于AI接入个人公众号(微信公众号不封号,个人账号易封),需要80或443接口暴露到公网供公众号服务器配置识别 (内网穿透,云服务器)

接入效果可关注我的个人公众号:  for you want
![qrcode_for_gh_fb9ed7dc4a79_258.jpg](images%2Fqrcode_for_gh_fb9ed7dc4a79_258.jpg)

目前只支持阿里云百炼API Key https://www.aliyun.com/product/bailian

并且只支持文本输出模型

支持发送消息更换模型及API key 只变换发送方的API key 后续可能会增加平台

```
格式: AI配置@1@模型名称@APIKey 示例: AI配置@1@deepseek-r1@sk-1ae0d58b66db49b39139f655826b
```

更换时只需给公众号发送 不建议发给不是自己部署的公众号

每次只能输出400字符,微信公众号回复最大600字符左右 个人公众号需要用户回复才能自动发送信息

故需要多次发送信息才能将完整AI回复全部输送

本项目使用java编写 使用了redis rabbitmq mysql

消息存储于系统为每位用户分配mq队列中,上下文存储在redis中

个人微信公众号注册链接:https://mp.weixin.qq.com/
![Snipaste_2025-02-28_13-36-55.png](images%2FSnipaste_2025-02-28_13-36-55.png)
### 如何部署?

###### windows部署 手动部署(不推荐):

需要安装javaJDK , mysql , redis,rabbitmq 自行网络寻找

##### docker部署:

#### docker 安装:

##### windows:  WSL 是个不错的选择:

##### 启用 WSL 功能

win+r 输入下面三个命令(应该会重启 需逐个输入):

`dism.exe /online /enable-feature /featurename:Microsoft-Windows-Subsystem-Linux /all /norestart`

##### 启用虚拟机平台功能（WSL 2 必需）

`dism.exe /online /enable-feature /featurename:VirtualMachinePlatform /all /norestart`

##### 安装wsl:

`wsl --install`

安装linux发行版本(可到Microsoft Store安装 搜索 Ubuntu):

###### 1.查看可用发行版列

```
wsl --list --online
```

###### 2.安装一个发行版

`wsl --install -d <DistroName>`

###### 3.安装docker-desktop或docker
(https://docs.docker.com/desktop/features/wsl/#download)

 新手建议安装(docker-desktop)

##### 部署:

目录结构:
![Snipaste_2025-02-28_15-24-57.png](images%2FSnipaste_2025-02-28_15-24-57.png)
```
--app(文件夹)

​	--dockerfile(文件)

​	--target(文件夹)

​		--releases中的to-wechat-1.0-SNAPSHOT.jar (文件)

--config(文件夹)

​	-application.yml(文件)

--docker-compose.yml(文件)
```

docker-compose.yml文件, dockerfile文件在代码目录中存在

application.yml文件模板:

```
server:
  port: 443
log:
  root:
    level: info
logging:
  level:
    com.ngmj.towechat.mapper: info
spring:
  datasource:
    username: root
    password: root
    url: jdbc:mysql://mysql:3306/towechat?useSSL=true&useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
    driver-class-name: com.mysql.cj.jdbc.Driver
    type: com.alibaba.druid.pool.DruidDataSource
    initial-size: 5
    max-active: 20
    min-idle: 5
    max-wait: 60000
  mvc:
    throw-exception-if-no-handler-found: true
  data:
    redis:
      database: 0
      host: redis
      port: 6379
  rabbitmq:
    host: rabbitmq
    port: 5672
    username: admin
    password: admin123
    publisher-confirm-type: correlated
    publisher-returns: true
    listener:
      simple:
        prefetch: 1
toWechat:
  app:
    appId: ********** 
    appSecret: **********
    encodingAESKey: **********
    token: **********
    defaultAPIInfo: **********(参考格式: AI配置@1@模型名称@APIKey 示例: AI配置@1@deepseek-r1@sk-1ae0d58b66db49b39139f655826b)
```

如果出现网络错误,需要的文件可以关注我的个人公众号 for you want 或自行寻找镜像仓库(网络上有教程)

在上述文件结构的目录下运行命令:

```
docker compose build
docker compose up -d
```

linux(centos , ubuntu)安装docker(自行网络上寻找教程,或者让AI教你 提示词: 如何在centos , ubuntu 上安装docker)

安装好后使用与上述部署教程一致,服务器上运行与上述过程一致,需要开放443端口; 喜欢的话记得点个赞!!!!
