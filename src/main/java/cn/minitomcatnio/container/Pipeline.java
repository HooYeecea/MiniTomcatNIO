package cn.minitomcatnio.container;

import cn.minitomcatnio.http.HttpRequest;
import cn.minitomcatnio.http.HttpResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Context 上的阀管道：额外 Valve 按添加顺序执行，最后一关是 basic。
 */
public class Pipeline {

    private final List<Valve> valves = new ArrayList<>();
    private Valve basic;

    public void addValve(Valve valve) {
        valves.add(valve);
    }

    public void setBasic(Valve basic) {
        this.basic = basic;
    }

    public void invoke(HttpRequest request, HttpResponse response) {
        new Chain(0).invoke(request, response);
    }

    private class Chain implements ValveChain {
        private final int index;

        Chain(int index) {
            this.index = index;
        }

        @Override
        public void invoke(HttpRequest request, HttpResponse response) {
            if (index < valves.size()) {
                valves.get(index).invoke(request, response, new Chain(index + 1));
                return;
            }
            if (basic != null) {
                basic.invoke(request, response, this);
            }
        }
    }
}
