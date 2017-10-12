package com.crazy.util.client;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public class UserClient {
	
	private static Cache<String, Object> userClientMap = CacheBuilder.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).build() ;
	
	public static  ConcurrentMap<String, Object> getUserClientMap(){
		return userClientMap.asMap() ;
	}
}
