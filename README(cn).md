# MiniTomcatNIO

用 Java NIO 实现的迷你版 Tomcat，目标是把 **请求怎么进来、怎么一层层分到某个 Servlet** 这条主路径走通，而不是复刻 Apache Tomcat 全部产品能力。

- 语言 / 构建：Java 21 + Maven
- 默认端口：`8080`
- 默认主机应用目录：`webapps/`（启动时自动扫描部署）
- 第二个虚拟主机：`hosts/app.local/`（`Host: app.local`）

英文版：[README.md](README.md)

## 共享 Servlet API

面向应用的类型（`Servlet`、`Filter`、`FilterChain`、`HttpRequest`、`HttpResponse`、
`ServletConfig`、`RequestDispatcher`、`HttpSession`、`DispatcherType`）已抽到独立模块，
方便 BIO / NIO 两套 Tomcat 以及后续 MiniMVC 共用同一套契约：

| 项 | 说明 |
|----|------|
| 模块 | `MiniServletApi`（`mini-servlet-api`） |
| 包名 | `com.web` |
| 仓库 | [https://github.com/HooYeecea/MiniServletAPI](https://github.com/HooYeecea/MiniServletAPI) |

本工程**实现**该 API（NIO 版 `HttpRequest` / `HttpResponse`、容器、示例）。
写 Servlet/Filter 请面向 `com.web`；容器私有能力仍留在具体实现类上。

BIO 兄弟项目：[MiniTomcat](https://github.com/HooYeecea/MiniTomcat)

同时是父工程 [MiniSpring](../README(CN).md) 中的模块。MiniMVC 可通过
`MvcNioApplication` 挂在本服务器上跑。

## 完成度（怎么看）

| 参照物 | 大致进度 | 说明 |
|--------|----------|------|
| **教学骨架**（能讲清 Tomcat 核心链路） | **约 80%** | Connector、容器树、虚拟主机、web.xml、Filter dispatcher、错误页、ClassLoader、生命周期、默认 Servlet 已齐 |
| **真实 Tomcat**（可当服务器用） | **约 5%～10%** | 无 HTTPS / HTTP/2、无 JSP、无热部署、无完整 Servlet API |

一句话：**容器主路径已经像迷你 Tomcat；离“生产可用的 Tomcat”还早。**

## 请求处理链路

```text
浏览器 / curl
  → Connector（NIO accept / read / write，解析 HTTP/1.1）
  → Worker 线程池（跑业务，不堵 Selector）
  → Engine（按 Host 头选虚拟主机；对不上则回到 localhost）
  → Host（按最长 URI 前缀选 Context）
  → Context Pipeline（Valve，如 AccessLog）
  → Mapper（精确 → 最长前缀 /* → 扩展名 *.do → 默认 /）
  → Filter 链（dispatcher 默认 REQUEST；FORWARD / INCLUDE / ERROR 要显式声明）
  → Wrapper → Servlet
```

静态文件、欢迎页和找不到文件时的 404 不再是 Mapper 之外的特例，而是由映射到 `/` 的容器 `DefaultServlet` 处理。

## 已完成能力

### 网络与协议（Connector）

- 非阻塞 `ServerSocketChannel` + `Selector`
- HTTP/1.1 请求头拼接、`Content-Length` body 读取
- Keep-Alive（写完后继续读；`Connection: close` 则关连接）
- 固定大小 Worker 线程池；业务与 I/O 分离
- `connector.stop()` 加关闭钩子：先 `destroy` Servlet / Filter，再通知 listener `contextDestroyed`

### 容器结构

- `Engine` → `Host` → `Context` → `Wrapper`
- Context 上的 `Pipeline` / `Valve`（含 AccessLog）
- 扫描某个 Host 的目录自动部署：`ROOT` → `/`，其它目录 → `/目录名`
- 两个 Host：`localhost`（`webapps/`）和 `app.local`（`hosts/app.local/`）
- `/` 上的 `DefaultServlet`，排在精确、前缀、扩展名之后

### Web 应用侧

- 每应用独立 `docBase`（如 `webapps/ROOT`、`webapps/other`）
- 最小 `WEB-INF/web.xml`：servlet、filter、listener、error-page、welcome-file、`init-param`、`context-param`
- 精确映射、最长前缀（`/app/*`）、扩展名（`*.do`），支持 `pathInfo`
- 目录请求按 `welcome-file-list` 查找（没配时默认 `index.html`）
- 静态资源、query / 表单参数、Cookie、内存 Session（`JSESSIONID`）
- Filter 链，以及 `<dispatcher>`（`REQUEST`、`FORWARD`、`INCLUDE`、`ERROR`）
- `RequestDispatcher.forward` 和 `include`
- `error-page`：抛异常走 500，`sendError` 走对应状态码（含静态文件 404）
- Servlet / Filter 的 `init` 与 `destroy`；`ServletContextListener`
- 每个 Servlet 的 `init-param`，以及整个应用的 `context-param`
- 每应用 `WebappClassLoader`（优先 `WEB-INF/classes`、`WEB-INF/lib`）

## 尚未实现（刻意延后）

- Session 过期清理、热部署、WAR 解压
- 注解扫描、完整 `web.xml` 语义（安全约束、mime-mapping 等）
- HTTPS、分块传输、HTTP/2、JSP、集群、完整 Servlet 规范 API

## 包结构

```text
cn.minitomcatnio
├── NioServer                 # 启动入口
├── connector                 # NIO Connector
├── http                      # 具体 HttpRequest / HttpResponse（实现 mini-servlet-api）
├── container                 # Engine / Host / Context / Wrapper / Valve / Mapper
├── servlet                   # GenericServlet、DefaultServlet、FilterChain、Dispatcher 等
├── session                   # 具体 HttpSession（实现 mini-servlet-api）
├── loader                    # web.xml、静态资源、WebappClassLoader
└── demo                      # 示例 Servlet / Filter（供 webapps 引用）
```

共享契约不在本仓库定义，见 [MiniServletAPI](https://github.com/HooYeecea/MiniServletAPI)。

## 目录与示例应用

```text
webapps/                      # Host localhost
├── ROOT/                     # context path = /
│   ├── welcome.html          # 欢迎页列表的第一项
│   ├── index.html
│   ├── hello.txt
│   └── WEB-INF/web.xml
└── other/                    # context path = /other
    ├── index.html
    └── WEB-INF/web.xml
hosts/app.local/              # Host app.local
└── ROOT/
    ├── index.html
    └── hello.txt
```

## 构建与运行

需要依赖 **`mini-servlet-api`**。若单独编译本模块，请先安装它
（在 [MiniServletAPI](https://github.com/HooYeecea/MiniServletAPI) 仓库或父工程 `mini-spring` 下执行 `mvn install`）。

在项目根目录（保证能读到 `webapps/` 和 `hosts/`）：

```bash
mvn -q compile
java -cp target/classes cn.minitomcatnio.NioServer
```

可选：把 demo 类拷进应用目录，便于验证 ClassLoader 从 `WEB-INF/classes` 加载：

```powershell
New-Item -ItemType Directory -Force -Path webapps\ROOT\WEB-INF\classes\cn\minitomcatnio\demo | Out-Null
New-Item -ItemType Directory -Force -Path webapps\other\WEB-INF\classes\cn\minitomcatnio\demo | Out-Null
Copy-Item target\classes\cn\minitomcatnio\demo\*.class webapps\ROOT\WEB-INF\classes\cn\minitomcatnio\demo\
Copy-Item target\classes\cn\minitomcatnio\demo\PingServlet.class webapps\other\WEB-INF\classes\cn\minitomcatnio\demo\
```

不拷贝时，应用类仍可通过 parent ClassLoader（`target/classes`）加载。

## 快速验收

| URL | 预期 |
|-----|------|
| http://127.0.0.1:8080/ | ROOT 的 `welcome.html` |
| http://127.0.0.1:8080/hello | HelloServlet |
| http://127.0.0.1:8080/hello.do | 扩展名映射，`pathInfo` 为空 |
| http://127.0.0.1:8080/app/x | 前缀映射，`pathInfo=/x` |
| http://127.0.0.1:8080/echo?name=tom | 参数解析 |
| http://127.0.0.1:8080/cookie | Cookie 读写 |
| http://127.0.0.1:8080/session | Session 计数 |
| http://127.0.0.1:8080/forward | forward 到 `/hello`，原始路径仍是 `/forward` |
| http://127.0.0.1:8080/wrap | include：外层一行加上 `/hello` 的正文 |
| http://127.0.0.1:8080/greeting | `init-param` 的 `greeting` |
| http://127.0.0.1:8080/config | `context-param` 的 `appName` |
| http://127.0.0.1:8080/boom | 500 错误页 |
| http://127.0.0.1:8080/no-such.txt | 经 `sendError` 的 404 错误页 |
| http://127.0.0.1:8080/other/ping | 第二个应用的 Servlet |
| `Host: app.local` 访问 `/` | 第二个虚拟主机首页 |

```bash
curl http://127.0.0.1:8080/hello
curl http://127.0.0.1:8080/greeting
curl -H "Host: app.local" http://127.0.0.1:8080/
curl http://127.0.0.1:8080/other/ping
```

## 设计取舍

- **Valve ≠ Filter**：Valve 是容器管道（更靠外）；Filter 是应用过滤器（套在 Servlet 前）。
- **Filter dispatcher**：没写 `<dispatcher>` 时只匹配 `REQUEST`。forward、include 和错误页不会再次进入这个 Filter。
- **Connector 不管应用**：只做连接与协议；分发交给 Engine / Host / Context。
- **`/` 是默认 Servlet**，不是上下文根的精确映射。精确、前缀、扩展名都优先于它。
- **一步一个能力**：协议 → 对象拆分 → 静态资源 → Servlet → 线程池 → Keep-Alive → Body → 参数 → 映射 → Cookie → Session → 容器拆分 → Valve → Wrapper → Engine/Host → 多应用 → docBase → web.xml → Filter → forward → 分包 → ClassLoader → 自动部署 → listener → destroy → include → error-page → sendError → 扩展名映射 → welcome-file → filter dispatcher → 默认 Servlet → 虚拟主机 → init-param → context-param。

## License

个人练习项目，仅供学习 Tomcat / NIO 原理。
