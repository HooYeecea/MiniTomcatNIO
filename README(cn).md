# MiniTomcatNIO

用 Java NIO 实现的迷你版 Tomcat，目标是把 **请求怎么进来、怎么一层层分到某个 Servlet** 这条主路径走通，而不是复刻 Apache Tomcat 全部产品能力。

- 语言 / 构建：Java 21 + Maven
- 默认端口：`8080`
- 应用目录：`webapps/`（启动时自动扫描部署）

英文版：[README.md](README.md)

## 完成度（怎么看）

| 参照物 | 大致进度 | 说明 |
|--------|----------|------|
| **教学骨架**（能讲清 Tomcat 核心链路） | **约 60%～70%** | Connector、容器树、web.xml、Filter、ClassLoader、自动部署已齐 |
| **真实 Tomcat**（可当服务器用） | **约 5%～10%** | 无 HTTPS / HTTP/2、无 JSP、无热部署、无完整 Servlet API |

一句话：**容器主路径已经像迷你 Tomcat；离“生产可用的 Tomcat”还早。**

## 请求处理链路

```text
浏览器 / curl
  → Connector（NIO accept / read / write，解析 HTTP/1.1）
  → Worker 线程池（跑业务，不堵 Selector）
  → Engine（按 Host 头选虚拟主机）
  → Host（按 URI 前缀选 Context）
  → Context Pipeline（Valve，如 AccessLog）
  → Mapper（精确路径 / 前缀 /*）
  → Filter 链
  → Wrapper → Servlet
  （未命中则读该应用 docBase 下的静态资源）
```

## 已完成能力

### 网络与协议（Connector）

- 非阻塞 `ServerSocketChannel` + `Selector`
- HTTP/1.1 请求头拼接、`Content-Length` body 读取
- Keep-Alive（写完后继续读；`Connection: close` 则关连接）
- 固定大小 Worker 线程池；业务与 I/O 分离

### 容器结构

- `Engine` → `Host` → `Context` → `Wrapper`
- Context 上的 `Pipeline` / `Valve`（含 AccessLog）
- 扫描 `webapps/` 自动部署：`ROOT` → `/`，其它目录 → `/目录名`

### Web 应用侧

- 每应用独立 `docBase`（如 `webapps/ROOT`、`webapps/other`）
- 最小 `WEB-INF/web.xml`：`servlet` / `filter` 及 mapping
- 精确映射 + 前缀映射（`/app/*`），支持 `pathInfo`
- 静态资源、query / 表单参数、Cookie、内存 Session（`JSESSIONID`）
- Filter 链、`RequestDispatcher.forward`
- Servlet / Filter `init`
- 每应用 `WebappClassLoader`（优先 `WEB-INF/classes`、`WEB-INF/lib`）

## 尚未实现（刻意延后）

- Listener、`RequestDispatcher.include`、`destroy` / 优雅停机
- Session 过期清理、热部署、WAR 解压
- `*.do` 扩展名映射、注解扫描、完整 `web.xml` 语义
- HTTPS、分块传输、HTTP/2、JSP、集群、完整 Servlet 规范 API

## 包结构

```text
cn.minitomcatnio
├── NioServer                 # 启动入口
├── connector                 # NIO Connector
├── http                      # HttpRequest / HttpResponse
├── container                 # Engine / Host / Context / Wrapper / Valve / Mapper
├── servlet                   # Servlet / Filter / RequestDispatcher
├── session                   # Session
├── loader                    # web.xml、静态资源、WebappClassLoader
└── demo                      # 示例 Servlet / Filter（供 webapps 引用）
```

## 目录与示例应用

```text
webapps/
├── ROOT/                     # 默认应用，context path = /
│   ├── index.html
│   ├── hello.txt
│   └── WEB-INF/web.xml
└── other/                    # 第二个应用，context path = /other
    ├── index.html
    └── WEB-INF/web.xml
```

## 构建与运行

在项目根目录（保证能读到 `webapps/`）：

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
| http://127.0.0.1:8080/ | ROOT 静态首页 |
| http://127.0.0.1:8080/hello | HelloServlet |
| http://127.0.0.1:8080/app/x | 前缀映射，`pathInfo=/x` |
| http://127.0.0.1:8080/echo?name=tom | 参数解析 |
| http://127.0.0.1:8080/cookie | Cookie 读写 |
| http://127.0.0.1:8080/session | Session 计数 |
| http://127.0.0.1:8080/forward | forward 到 `/hello` |
| http://127.0.0.1:8080/other/ | 第二个应用首页 |
| http://127.0.0.1:8080/other/ping | 第二个应用 Servlet |

```bash
curl http://127.0.0.1:8080/hello
curl -d "name=tom" http://127.0.0.1:8080/echo
curl http://127.0.0.1:8080/other/ping
```

## 设计取舍

- **Valve ≠ Filter**：Valve 是容器管道（更靠外）；Filter 是应用过滤器（套在 Servlet 前）。
- **Connector 不管应用**：只做连接与协议；分发交给 Engine / Host / Context。
- **一步一个能力**：协议 → 对象拆分 → 静态资源 → Servlet → 线程池 → Keep-Alive → Body → 参数 → 映射 → Cookie → Session → 容器拆分 → Valve → Wrapper → Engine/Host → 多应用 → docBase → web.xml → Filter → forward → 分包 → ClassLoader → 自动部署。

## License

个人练习项目，仅供学习 Tomcat / NIO 原理。
