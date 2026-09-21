package cn.minitomcatnio.servlet;

/**
 * Filter 匹配时对应的派发类型。web.xml 不写 dispatcher 时默认为 REQUEST。
 */
public enum DispatcherType {
    REQUEST,
    FORWARD,
    INCLUDE,
    ERROR
}
