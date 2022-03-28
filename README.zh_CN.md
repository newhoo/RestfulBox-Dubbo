# RESTKit-Dubbo

[RESTKit-Dubbo](https://plugins.jetbrains.com/plugin/18828-restkit-dubbo) 是一个依赖于[RESTKit](https://plugins.jetbrains.com/plugin/14723-restkit) 插件的插件，用于为`RESTKit`(2.0.0开始) 提供Dubbo支持。

> 注意  
> RESTKit从2.0.0开始提供了扩展点，如同本插件，你也能为RESTKit提供自己所需的接口扫描方式。

如果你觉得本插件不错，请给个🌟Star吧，同时也欢迎提供宝贵的建议。

## 功能
- 支持RESTKit的绝大多数功能。
- 支持扫描Java项目中的dubbo服务。
- 支持发送dubbo请求。
- 支持在dubbo服务实现方法上跳转到service tree窗口。

## 安装
- **插件市场安装**

推荐 <kbd>Preferences(Settings)</kbd> > <kbd>Plugins</kbd> > <kbd>Browse repositories...</kbd> > <kbd>输入"RESTKit-Dubbo"</kbd> > <kbd>点击Install</kbd>

- **本地安装**

从仓库下载<kbd>distributions/RESTKit-Dubbo-x.x.x.zip</kbd>, 然后在本地Idea安装 <kbd>Preferences(Settings)</kbd> > <kbd>Plugins</kbd> > <kbd>Install Plugin from Disk...</kbd>

## 使用
安装完成后，在`RESTKit`插件设置中启用，然后在`RESTKit`窗口刷新项目接口。

![enable](./.images/setting.png)

如下图：

![plugin](./.images/plugin.png)

UI说明：

- Config：请求配置，dubbo请求的一些配置，可使用环境变量。支持以下配置：
    - registry：注册中心，只支持zookeeper，默认`{{registry}}`，如没有配置环境变量，则请求时替换为`zookeeper://127.0.0.1:2181`
    - url：dubbo直连url，如`dubbo://127.0.0.1:20880`。默认`{{url}}`，如没有配置环境变量，则请求时替换为空
    - timeout：request timeout，默认2000ms。若没有此字段，则默认timeout为5000ms
    - applicationName：如无配置，默认为RESTKit-Dubbo-proxy
    - retries：如无配置，默认0
    - check：如无配置，默认false
    - loadbalance：如无配置，默认为空
- Headers：dubbo请求的attachments
- Params：在dubbo请求中没用到，请忽略
- Body：dubbo泛化调用的一些内容，不能引用环境变量
- Response：响应内容
- Info：一次请求响应的内容