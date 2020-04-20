package de.hpi.tdgt.Stats;

import de.hpi.tdgt.requesthandling.HttpConstants;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Endpoint implements Comparable<Endpoint> {

    private static final Map<String, StatisticProtos.Endpoint.Method> stringToMethodMap = new HashMap<>();
    static {
        stringToMethodMap.put(HttpConstants.GET, StatisticProtos.Endpoint.Method.GET);
        stringToMethodMap.put(HttpConstants.PUT, StatisticProtos.Endpoint.Method.PUT);
        stringToMethodMap.put(HttpConstants.POST, StatisticProtos.Endpoint.Method.POST);
        stringToMethodMap.put(HttpConstants.DELETE, StatisticProtos.Endpoint.Method.DELETE);
    }

    public final URL url;
    public final String method;
    private final int hash;

    public Endpoint(URL url, String method)
    {
        this.url = url;
        this.method = method;
        this.hash = Objects.hash(url, method);
    }

    @Override
    public int compareTo(Endpoint o) {
        return url.toString().compareTo(o.url.toString());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass())
            return false;

        Endpoint e = (Endpoint)obj;
        return  hash == e.hash && method.equals(e.method) && url.equals(e.url);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    public StatisticProtos.Endpoint Serialize() {
        return StatisticProtos.Endpoint.newBuilder()
                .setUrl(url.toString())
                .setMethod(stringToMethodMap.getOrDefault(method, StatisticProtos.Endpoint.Method.GET)).build();
    }
}
