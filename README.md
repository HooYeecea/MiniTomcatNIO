# MiniTomcatNIO

A mini Tomcat built with Java NIO. The goal is to make the main request path clear — **how a connection is accepted and how it is routed down to a Servlet** — not to clone every Apache Tomcat product feature.

- Language / build: Java 21 + Maven
- Default port: `8080`
- App directory: `webapps/` (scanned and deployed at startup)

Chinese version: [README(cn).md](README(cn).md)

## Completeness

| Baseline | Rough progress | Notes |
|----------|----------------|-------|
| **Learning skeleton** (explain Tomcat’s core path) | **~60%–70%** | Connector, container tree, web.xml, Filter, ClassLoader, auto-deploy are in place |
| **Real Tomcat** (production-ready server) | **~5%–10%** | No HTTPS / HTTP/2, no JSP, no hot deploy, no full Servlet API |

In short: **the container main path already looks like a mini Tomcat; it is nowhere near a production Tomcat.**

## Request path

```text
Browser / curl
  → Connector (NIO accept / read / write, HTTP/1.1 parse)
  → Worker pool (business work; does not block the Selector)
  → Engine (pick Host from the Host header)
  → Host (pick Context by URI prefix)
  → Context Pipeline (Valves, e.g. AccessLog)
  → Mapper (exact path / prefix /*)
  → Filter chain
  → Wrapper → Servlet
  (if no match: static file from that app’s docBase)
```

## What works today

### Network & protocol (Connector)

- Non-blocking `ServerSocketChannel` + `Selector`
- HTTP/1.1 header assembly and `Content-Length` body reads
- Keep-Alive (keep reading after write; close on `Connection: close`)
- Fixed-size worker pool; business logic separated from I/O

### Container structure

- `Engine` → `Host` → `Context` → `Wrapper`
- Context `Pipeline` / `Valve` (including AccessLog)
- Auto-deploy by scanning `webapps/`: `ROOT` → `/`, other dirs → `/dirname`

### Web application side

- Per-app `docBase` (e.g. `webapps/ROOT`, `webapps/other`)
- Minimal `WEB-INF/web.xml`: `servlet` / `filter` and mappings
- Exact and prefix mappings (`/app/*`), with `pathInfo`
- Static resources, query / form params, Cookie, in-memory Session (`JSESSIONID`)
- Filter chain, `RequestDispatcher.forward`
- Servlet / Filter `init`
- Per-app `WebappClassLoader` (prefers `WEB-INF/classes` and `WEB-INF/lib`)

## Not implemented (intentionally deferred)

- Listener, `RequestDispatcher.include`, `destroy` / graceful shutdown
- Session expiry cleanup, hot deploy, WAR unpack
- Extension mapping (`*.do`), annotation scan, full `web.xml` semantics
- HTTPS, chunked transfer, HTTP/2, JSP, clustering, full Servlet API

## Package layout

```text
cn.minitomcatnio
├── NioServer                 # bootstrap
├── connector                 # NIO Connector
├── http                      # HttpRequest / HttpResponse
├── container                 # Engine / Host / Context / Wrapper / Valve / Mapper
├── servlet                   # Servlet / Filter / RequestDispatcher
├── session                   # Session
├── loader                    # web.xml, static resources, WebappClassLoader
└── demo                      # sample Servlet / Filter (referenced by webapps)
```

## Directory layout & sample apps

```text
webapps/
├── ROOT/                     # default app, context path = /
│   ├── index.html
│   ├── hello.txt
│   └── WEB-INF/web.xml
└── other/                    # second app, context path = /other
    ├── index.html
    └── WEB-INF/web.xml
```

## Build & run

From the project root (so `webapps/` is visible):

```bash
mvn -q compile
java -cp target/classes cn.minitomcatnio.NioServer
```

Optional: copy demo classes into app dirs to verify ClassLoader loading from `WEB-INF/classes`:

```powershell
New-Item -ItemType Directory -Force -Path webapps\ROOT\WEB-INF\classes\cn\minitomcatnio\demo | Out-Null
New-Item -ItemType Directory -Force -Path webapps\other\WEB-INF\classes\cn\minitomcatnio\demo | Out-Null
Copy-Item target\classes\cn\minitomcatnio\demo\*.class webapps\ROOT\WEB-INF\classes\cn\minitomcatnio\demo\
Copy-Item target\classes\cn\minitomcatnio\demo\PingServlet.class webapps\other\WEB-INF\classes\cn\minitomcatnio\demo\
```

Without copying, app classes can still load via the parent ClassLoader (`target/classes`).

## Quick checks

| URL | Expected |
|-----|----------|
| http://127.0.0.1:8080/ | ROOT static home |
| http://127.0.0.1:8080/hello | HelloServlet |
| http://127.0.0.1:8080/app/x | Prefix mapping, `pathInfo=/x` |
| http://127.0.0.1:8080/echo?name=tom | Parameter parsing |
| http://127.0.0.1:8080/cookie | Cookie read/write |
| http://127.0.0.1:8080/session | Session counter |
| http://127.0.0.1:8080/forward | Forward to `/hello` |
| http://127.0.0.1:8080/other/ | Second app home |
| http://127.0.0.1:8080/other/ping | Second app Servlet |

```bash
curl http://127.0.0.1:8080/hello
curl -d "name=tom" http://127.0.0.1:8080/echo
curl http://127.0.0.1:8080/other/ping
```

## Design notes

- **Valve ≠ Filter**: Valves are container pipeline hooks (outer); Filters are app filters (in front of a Servlet).
- **Connector does not own apps**: it only handles connections and the protocol; Engine / Host / Context do dispatch.
- **One capability at a time**: protocol → object split → static files → Servlet → thread pool → Keep-Alive → body → params → mapping → Cookie → Session → container split → Valve → Wrapper → Engine/Host → multi-app → docBase → web.xml → Filter → forward → packages → ClassLoader → auto-deploy.

## License

Personal practice project for learning Tomcat / NIO internals.
