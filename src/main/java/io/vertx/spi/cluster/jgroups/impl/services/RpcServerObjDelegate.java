/*
 * Copyright (c) 2011-2013 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 *     The Eclipse Public License is available at
 *     http://www.eclipse.org/legal/epl-v10.html
 *
 *     The Apache License v2.0 is available at
 *     http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */

package io.vertx.spi.cluster.jgroups.impl.services;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.spi.cluster.jgroups.impl.domain.MultiMap;
import io.vertx.spi.cluster.jgroups.impl.support.DataHolder;
import io.vertx.spi.cluster.jgroups.impl.support.LambdaLogger;
import org.jgroups.blocks.MethodCall;
import org.jgroups.blocks.MethodLookup;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class RpcServerObjDelegate implements RpcMapService, RpcMultiMapService, LambdaLogger {

  private static final Logger LOG = LoggerFactory.getLogger(RpcServerObjDelegate.class);

  private final RpcMapService mapService;
  private final RpcMultiMapService multiMapService;

  private static final short MULTIMAP_ADD = 11;
  private static final short MULTIMAP_REMOVE = 12;
  private static final short MULTIMAP_REMOVE_ALL = 13;
  private static final short MULTIMAP_REMOVE_ALL_MATCHING = 14;

  private static final short MAP_PUT = 21;
  private static final short MAP_PUTIFABSENT = 22;
  private static final short MAP_REMOVE = 23;
  private static final short MAP_REMOVEIFPRESENT = 24;
  private static final short MAP_REPLACE = 25;
  private static final short MAP_REPLACEIFPRESENT = 26;
  private static final short MAP_CLEAR = 27;
  private static final short MAP_PUTALL = 28;

  public static final MethodCallInterface.ThreeParameters CALL_MULTIMAP_ADD = (name, p1, p2) -> new MethodCall(MULTIMAP_ADD, name, DataHolder.wrap(p1), DataHolder.wrap(p2));
  public static final MethodCallInterface.ThreeParameters CALL_MULTIMAP_REMOVE = (name, p1, p2) -> new MethodCall(MULTIMAP_REMOVE, name, DataHolder.wrap(p1), DataHolder.wrap(p2));
  public static final MethodCallInterface.TwoParameters CALL_MULTIMAP_REMOVE_ALL = (name, p1) -> new MethodCall(MULTIMAP_REMOVE_ALL, name, DataHolder.wrap(p1));
  public static final MethodCallInterface.TwoParameters CALL_MULTIMAP_REMOVE_ALL_MATCHING = (name, p1) -> new MethodCall(MULTIMAP_REMOVE_ALL_MATCHING, name, DataHolder.wrap(p1));

  public static final MethodCallInterface.ThreeParameters CALL_MAP_PUT = (name, p1, p2) -> new MethodCall(MAP_PUT, name, DataHolder.wrap(p1), DataHolder.wrap(p2));
  public static final MethodCallInterface.TwoParameters CALL_MAP_PUTALL = (name, p1) -> new MethodCall(MAP_PUTIFABSENT, name, DataHolder.wrap(p1));
  public static final MethodCallInterface.ThreeParameters CALL_MAP_PUTIFABSENT = (name, p1, p2) -> new MethodCall(MAP_PUTIFABSENT, name, DataHolder.wrap(p1), DataHolder.wrap(p2));
  public static final MethodCallInterface.TwoParameters CALL_MAP_REMOVE = (name, p1) -> new MethodCall(MAP_REMOVE, name, DataHolder.wrap(p1));
  public static final MethodCallInterface.ThreeParameters CALL_MAP_REMOVEIFPRESENT = (name, p1, p2) -> new MethodCall(MAP_REMOVEIFPRESENT, name, DataHolder.wrap(p1), DataHolder.wrap(p2));
  public static final MethodCallInterface.ThreeParameters CALL_MAP_REPLACE = (name, p1, p2) -> new MethodCall(MAP_REPLACE, name, DataHolder.wrap(p1), DataHolder.wrap(p2));
  public static final MethodCallInterface.FourParameters CALL_MAP_REPLACEIFPRESENT = (name, p1, p2, p3) -> new MethodCall(MAP_REPLACEIFPRESENT, name, DataHolder.wrap(p1), DataHolder.wrap(p2), DataHolder.wrap(p3));
  public static final MethodCallInterface.OneParameter CALL_MAP_CLEAR = (name) -> new MethodCall(MAP_CLEAR, name);

  private static final Map<Short, Method> methods = new HashMap<>();

  static {
    try {
      methods.put(MULTIMAP_ADD, RpcServerObjDelegate.class.getMethod("multiMapAdd", String.class, DataHolder.class, DataHolder.class));
      methods.put(MULTIMAP_REMOVE, RpcServerObjDelegate.class.getMethod("multiMapRemove", String.class, DataHolder.class, DataHolder.class));
      methods.put(MULTIMAP_REMOVE_ALL, RpcServerObjDelegate.class.getMethod("multiMapRemoveAll", String.class, DataHolder.class));

      methods.put(MAP_PUT, RpcServerObjDelegate.class.getMethod("mapPut", String.class, DataHolder.class, DataHolder.class));
      methods.put(MAP_PUTALL, RpcServerObjDelegate.class.getMethod("mapPutAll", String.class, Map.class));
      methods.put(MAP_PUTIFABSENT, RpcServerObjDelegate.class.getMethod("mapPutIfAbsent", String.class, DataHolder.class, DataHolder.class));
      methods.put(MAP_REMOVE, RpcServerObjDelegate.class.getMethod("mapRemove", String.class, DataHolder.class));
      methods.put(MAP_REMOVEIFPRESENT, RpcServerObjDelegate.class.getMethod("mapRemoveIfPresent", String.class, DataHolder.class, DataHolder.class));
      methods.put(MAP_REPLACE, RpcServerObjDelegate.class.getMethod("mapReplace", String.class, DataHolder.class, DataHolder.class));
      methods.put(MAP_REPLACEIFPRESENT, RpcServerObjDelegate.class.getMethod("mapReplaceIfPresent", String.class, DataHolder.class, DataHolder.class, DataHolder.class));
      methods.put(MAP_CLEAR, RpcServerObjDelegate.class.getMethod("mapClear", String.class));
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }

  public RpcServerObjDelegate(RpcMapService mapService, RpcMultiMapService multiMapService) {
    this.mapService = mapService;
    this.multiMapService = multiMapService;
  }

  public MethodLookup getMethodLookup() {
    return methods::get;
  }

  //RpcMultiMapService
  @Override
  public <K, V> MultiMap<K, V> multiMapCreate(String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <K, V> void multiMapAdd(String name, DataHolder<K> k, DataHolder<V> v) {
    logTrace(() -> "RpcServerObjDelegate.multiMapAdd name = [" + name + "], k = [" + k + "], v = [" + v + "]");
    multiMapService.multiMapAdd(name, k, v);
  }

  @Override
  public <K, V> boolean multiMapRemove(String name, DataHolder<K> k, DataHolder<V> v) {
    logTrace(() -> "RpcServerObjDelegate.multiMapRemove name = [" + name + "], k = [" + k + "], v = [" + v + "]");
    return multiMapService.multiMapRemove(name, k, v);
  }

  @Override
  public <K, V> void multiMapRemoveAll(String name, DataHolder<V> v) {
    logTrace(() -> "RpcServerObjDelegate.multiMapRemoveAll name = [" + name + "], v = [" + v + "]");
    multiMapService.multiMapRemoveAll(name, v);
  }

  @Override
  public <K, V> void multiMapRemoveAllMatching(String name, DataHolder<Predicate<V>> p) {
    logTrace(() -> "RpcServerObjDelegate.multiMapRemoveAllMatching name = [" + name + "]");
    multiMapService.multiMapRemoveAll(name, p);
  }

  @Override
  public void writeTo(OutputStream output) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void readFrom(InputStream input) {
    throw new UnsupportedOperationException();
  }

  //RpcMapService
  @Override
  public <K, V> Map<K, V> mapCreate(String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <K, V> void mapPut(String name, DataHolder<K> k, DataHolder<V> v) {
    mapService.mapPut(name, k, v);
  }

  @Override
  public <K, V> void mapPutAll(String name, Map<DataHolder<K>, DataHolder<V>> m) {
    mapService.mapPutAll(name, m);
  }

  @Override
  public <K, V> DataHolder<V> mapPutIfAbsent(String name, DataHolder<K> k, DataHolder<V> v) {
    return mapService.mapPutIfAbsent(name, k, v);
  }

  @Override
  public <K, V> DataHolder<V> mapRemove(String name, DataHolder<K> k) {
    return mapService.mapRemove(name, k);
  }

  @Override
  public <K, V> boolean mapRemoveIfPresent(String name, DataHolder<K> k, DataHolder<V> v) {
    return mapService.mapRemoveIfPresent(name, k, v);
  }

  @Override
  public <K, V> DataHolder<V> mapReplace(String name, DataHolder<K> k, DataHolder<V> v) {
    return mapService.mapReplace(name, k, v);
  }

  @Override
  public <K, V> boolean mapReplaceIfPresent(String name, DataHolder<K> k, DataHolder<V> oldValue, DataHolder<V> newValue) {
    return mapService.mapReplaceIfPresent(name, k, oldValue, newValue);
  }

  @Override
  public <K, V> void mapClear(String name) {
    mapService.mapClear(name);
  }

  @Override
  public Logger log() {
    return LOG;
  }

}
