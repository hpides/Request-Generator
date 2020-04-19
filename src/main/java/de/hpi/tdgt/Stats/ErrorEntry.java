package de.hpi.tdgt.Stats;

import java.util.Objects;

public class ErrorEntry {

    public static int GetHashCode(Endpoint endpoint, String error){
        return Objects.hash(endpoint, error);
    }

    private final Endpoint endpoint;
    public final String error;
    public int occurences = 1;

    public ErrorEntry(Endpoint endpoint, String error) {
        this.endpoint = endpoint;
        this.error = error;
    }

    public ErrorEntry(ErrorEntry other) {
        this.endpoint = other.endpoint;
        this.error = other.error;
        this.occurences = other.occurences;
    }

    public void Occured() {
        occurences++;
    }

    public void Merge(ErrorEntry other)
    {
        this.occurences += other.occurences;
    }

    @Override
    public int hashCode() {
        return GetHashCode(endpoint, error);
    }
}
