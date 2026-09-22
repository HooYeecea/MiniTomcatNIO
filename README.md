# MiniTomcatNIO

A mini Tomcat built with Java NIO. The goal is to make the main request path clear — **how a connection is accepted and how it is routed down to a Servlet** — not to clone every Apache Tomcat product feature.

- Language / build: Java 21 + Maven
- Default port: `8080`
- Default host apps: `webapps/` (scanned and deployed at startup)
- Second virtual host: `hosts/app.local/` (`Host: app.local`)

Chinese version: [README(CN).md](README(CN).md)

## Shared Servlet API

Application-facing types (`Servlet`, `Filter`, `FilterChain`, `HttpRequest`, `HttpResponse`,
`ServletConfig`, `RequestDispatcher`, `HttpSession`, `DispatcherType`) live in a separate module
so BIO / NIO Tomcat and MiniMVC can share one contract:

| Item | Value |
|------|--------|
| Module | `MiniServletApi` (`mini-servlet-api`) |
| Package | `com.web` |
| Repository | [https://github.com/HooYeecea/MiniServletAPI](https://github.com/HooYeecea/MiniServletAPI) |

This project **implements** that API (NIO `HttpRequest` / `HttpResponse`, container, demos).
Write Servlets/Filters against `com.web`; container-only helpers stay on concrete classes.

Sibling BIO server: [MiniTomcat](https://github.com/HooYeecea/MiniTomcat)

Also a module of the parent [MiniSpring](../README.md) reactor. MiniMVC can boot on this
server via `MvcNioApplication`.

## Completeness

| Baseline | Rough progress | Notes |
|----------|----------------|-------|
| **Learning skeleton** (explain Tomcat’s core path) | **~80%** | Connector, container tree, virtual hosts, web.xml, Filter dispatcher, error pages, ClassLoader, lifecycle, and the default Servlet are in place |
| **Real Tomcat** (production-ready server) | **~5%–10%** | No HTTPS / HTTP/2, no JSP, no hot deploy, no full Servlet API |

In short: **the container main path already looks like a mini Tomcat; it is nowhere near a production Tomcat.**

## Request path

```text
Browser / curl
  → Connector (NIO accept / read / write, HTTP/1.1 parse)
  → Worker pool (business work; does not block the Selector)
  → Engine (pick Host from the Host header; unknown hosts fall back to localhost)
  → Host (pick Context by the longest URI prefix)
  → Context Pipeline (Valves, e.g. AccessLog)
  → Mapper (exact → longest prefix /* → extension *.do → default /)
  → Filter chain (dispatcher: REQUEST by default; FORWARD / INCLUDE / ERROR only when declared)
  → Wrapper → Servlet
```

Static files, welcome files, and a missing-file 404 are not a special case outside the Mapper. They are served by the container `DefaultServlet` mapped to `/`.

## What works today

### Network & protocol (Connector)

- Non-blocking `ServerSocketChannel` + `Selector`
- HTTP/1.1 header assembly and `Content-Length` body reads
- Keep-Alive (keep reading after write; close on `Connection: close`)
- Fixed-size worker pool; business logic separated from I/O
- `connector.stop()` plus a shutdown hook: servlets and filters `destroy`, then listeners `contextDestroyed`

### Container structure

- `Engine` → `Host` → `Context` → `Wrapper`
- Context `Pipeline` / `Valve` (including AccessLog)
- Auto-deploy by scanning a host directory: `ROOT` → `/`, other dirs → `/dirname`
- Two hosts: `localhost` (`webapps/`) and `app.local` (`hosts/app.local/`)
- `DefaultServlet` on `/`, after exact, prefix, and extension mappings

### Web application side

- Per-app `docBase` (e.g. `webapps/ROOT`, `webapps/other`)
- Minimal `WEB-INF/web.xml`: servlet, filter, listener, error-page, welcome-file, `init-param`, `context-param`
- Exact, longest-prefix (`/app/*`), and extension (`*.do`) mappings, with `pathInfo`
- `welcome-file-list` for directory requests (default `index.html` when unset)
- Static resources, query / form params, Cookie, in-memory Session (`JSESSIONID`)
- Filter chain and `<dispatcher>` (`REQUEST`, `FORWARD`, `INCLUDE`, `ERROR`)
- `RequestDispatcher.forward` and `include`
- `error-page` for thrown exceptions (500) and `sendError` (including static 404)
- Servlet / Filter `init` and `destroy`; `ServletContextListener`
- Per-servlet `init-param` and per-app `context-param`
- Per-app `WebappClassLoader` (prefers `WEB-INF/classes` and `WEB-INF/lib`)

## Not implemented (intentionally deferred)

- Session expiry cleanup, hot deploy, WAR unpack
- Annotation scan, full `web.xml` semantics (security constraints, mime-mapping, and so on)
- HTTPS, chunked transfer, HTTP/2, JSP, clustering, full Servlet API

## Package layout

```text
cn.minitomcatnio
├── NioServer                 # bootstrap
├── connector                 # NIO Connector
├── http                      # concrete HttpRequest / HttpResponse (implement mini-servlet-api)
├── container                 # Engine / Host / Context / Wrapper / Valve / Mapper
├── servlet                   # GenericServlet, DefaultServlet, FilterChain, Dispatcher, ...
├── session                   # concrete HttpSession (implements mini-servlet-api)
├── loader                    # web.xml, static resources, WebappClassLoader
└── demo                      # sample Servlet / Filter (referenced by webapps)
```

Shared contracts are **not** defined here — see [MiniServletAPI](https://github.com/HooYeecea/MiniServletAPI).

## Directory layout & sample apps

```text
webapps/                      # Host localhost
├── ROOT/                     # context path = /
│   ├── welcome.html          # first welcome file
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

## Build & run

Requires **`mini-servlet-api`** on the classpath. Install it first if you build this module alone
(`mvn install` in [MiniServletAPI](https://github.com/HooYeecea/MiniServletAPI) or via the parent `mini-spring` reactor).

From the project root (so `webapps/` and `hosts/` are visible):

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
| http://127.0.0.1:8080/ | ROOT `welcome.html` |
| http://127.0.0.1:8080/hello | HelloServlet |
| http://127.0.0.1:8080/hello.do | Extension mapping, `pathInfo` empty |
| http://127.0.0.1:8080/app/x | Prefix mapping, `pathInfo=/x` |
| http://127.0.0.1:8080/echo?name=tom | Parameter parsing |
| http://127.0.0.1:8080/cookie | Cookie read/write |
| http://127.0.0.1:8080/session | Session counter |
| http://127.0.0.1:8080/forward | Forward to `/hello`; original path stays `/forward` |
| http://127.0.0.1:8080/wrap | Include: outer line plus `/hello` body |
| http://127.0.0.1:8080/greeting | `init-param` `greeting` |
| http://127.0.0.1:8080/config | `context-param` `appName` |
| http://127.0.0.1:8080/boom | 500 error page |
| http://127.0.0.1:8080/no-such.txt | 404 error page via `sendError` |
| http://127.0.0.1:8080/other/ping | Second app Servlet |
| `Host: app.local` on `/` | Second virtual host home |

```bash
curl http://127.0.0.1:8080/hello
curl http://127.0.0.1:8080/greeting
curl -H "Host: app.local" http://127.0.0.1:8080/
curl http://127.0.0.1:8080/other/ping
```

## Design notes

- **Valve ≠ Filter**: Valves are container pipeline hooks (outer); Filters are app filters (in front of a Servlet).
- **Filter dispatcher**: a mapping with no `<dispatcher>` matches `REQUEST` only. Forward, include, and error dispatch do not re-enter that filter.
- **Connector does not own apps**: it only handles connections and the protocol; Engine / Host / Context do dispatch.
- **`/` is the default Servlet**, not an exact match for the context root. Exact, prefix, and extension rules win first.
- **One capability at a time**: protocol → object split → static files → Servlet → thread pool → Keep-Alive → body → params → mapping → Cookie → Session → container split → Valve → Wrapper → Engine/Host → multi-app → docBase → web.xml → Filter → forward → packages → ClassLoader → auto-deploy → listener → destroy → include → error-page → sendError → extension mapping → welcome-file → filter dispatcher → default Servlet → virtual host → init-param → context-param.

## License

Personal practice project for learning Tomcat / NIO internals.
