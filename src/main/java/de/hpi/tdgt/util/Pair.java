package de.hpi.tdgt.util;

import lombok.*;

@AllArgsConstructor
@EqualsAndHashCode
@Getter
@Setter
@NoArgsConstructor
@ToString
public class Pair<K,V>{
    private K key;
    private V value;
}