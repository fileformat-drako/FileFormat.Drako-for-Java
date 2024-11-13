package dev.fileformat.drako;


import java.util.Map;

/**
 * Created by lexchou on 12/8/2016.
 */
@Internal
class KeyValuePair<K, V> implements Map.Entry<K, V>{
    private K key;
    private V value;
    public static  <K, V> KeyValuePair<K, V> of(K key, V value)
    {
        KeyValuePair<K, V> ret = new KeyValuePair<>();
        ret.key = key;
        ret.value = value;
        return ret;
    }

    @Override
    public K getKey() {
        return key;
    }

    @Override
    public V getValue() {
        return value;
    }

    @Override
    public V setValue(V value) {
        V ret = this.value;
        this.value = value;
        return ret;
    }
}
