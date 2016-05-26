/**
 *   Copyright 2014 Nortal AS
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.nortal.petit.converter.provider;

public class CachingProvider<K, V> extends SimpleContainer<K, V> {
	private Provider<K, V> delegate;
	
	public CachingProvider(Provider<K, V> delegate) {
		this.delegate = delegate;
	}

	@Override
	public V get(K key) {
		if (!map.containsKey(key)) {
			map.put(key, delegate.get(key));
		}
		return map.get(key);
	}
}
