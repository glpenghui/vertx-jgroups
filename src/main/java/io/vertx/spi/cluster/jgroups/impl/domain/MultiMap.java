package io.vertx.spi.cluster.jgroups.impl.domain;

import io.vertx.core.spi.cluster.ChoosableIterable;

import java.io.Externalizable;
import java.util.function.Predicate;

public interface MultiMap<K, V> extends Externalizable {

  void add(K k, V v);

  ChoosableIterable<V> get(K k);

  boolean remove(K k, V v);

  void removeAll(V v);

  void removeAllMatching(Predicate<V> p);
}
