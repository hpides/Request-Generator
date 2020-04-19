package de.hpi.tdgt.Stats;

import java.net.URL;
import java.util.Objects;

public class Endpoint implements Comparable<Endpoint> {

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
}
