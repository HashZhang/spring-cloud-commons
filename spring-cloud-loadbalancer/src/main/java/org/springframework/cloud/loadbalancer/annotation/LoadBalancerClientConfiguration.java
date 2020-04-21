/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.loadbalancer.annotation;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.ConditionalOnBlockingDiscoveryEnabled;
import org.springframework.cloud.client.ConditionalOnDiscoveryEnabled;
import org.springframework.cloud.client.ConditionalOnReactiveDiscoveryEnabled;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.loadbalancer.annotation.configutil.LoadBalancers;
import org.springframework.cloud.loadbalancer.cache.LoadBalancerCacheManager;
import org.springframework.cloud.loadbalancer.core.CachingServiceInstanceSupplier;
import org.springframework.cloud.loadbalancer.core.DiscoveryClientServiceInstanceSupplier;
import org.springframework.cloud.loadbalancer.core.ReactorLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;

import static org.springframework.cloud.loadbalancer.annotation.configutil.ServiceInstanceListSuppliers.cachedOrDelegate;
import static org.springframework.cloud.loadbalancer.annotation.configutil.ServiceInstanceListSuppliers.discoveryClientBased;

/**
 * @author Spencer Gibb
 * @author Olga Maciaszek-Sharma
 * @author Tim Ysewyn
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnDiscoveryEnabled
public class LoadBalancerClientConfiguration {

	private static final int REACTIVE_SERVICE_INSTANCE_SUPPLIER_ORDER = 193827465;

	@Bean
	@ConditionalOnMissingBean
	public ReactorLoadBalancer<ServiceInstance> reactorServiceInstanceLoadBalancer(
			Environment environment,
			LoadBalancerClientFactory loadBalancerClientFactory) {
		return LoadBalancers.roundRobin(loadBalancerClientFactory, environment);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnReactiveDiscoveryEnabled
	@Order(REACTIVE_SERVICE_INSTANCE_SUPPLIER_ORDER)
	public static class ReactiveSupportConfiguration {


		@Bean
		@ConditionalOnBean(ReactiveDiscoveryClient.class)
		@ConditionalOnMissingBean
		public ServiceInstanceListSupplier discoveryClientServiceInstanceListSupplier(
				ReactiveDiscoveryClient discoveryClient, Environment env,
				ApplicationContext context) {
			return cachedOrDelegate(context, discoveryClientBased(discoveryClient, env));
		}

		@Bean
		@ConditionalOnBean(ReactiveDiscoveryClient.class)
		@ConditionalOnMissingBean
		public ServiceInstanceSupplier discoveryClientServiceInstanceSupplier(
				ReactiveDiscoveryClient discoveryClient, Environment env,
				ApplicationContext context) {
			DiscoveryClientServiceInstanceSupplier delegate = new DiscoveryClientServiceInstanceSupplier(
					discoveryClient, env);
			ObjectProvider<LoadBalancerCacheManager> cacheManagerProvider = context
					.getBeanProvider(LoadBalancerCacheManager.class);
			if (cacheManagerProvider.getIfAvailable() != null) {
				return new CachingServiceInstanceSupplier(delegate,
						cacheManagerProvider.getIfAvailable());
			}
			return delegate;
		}
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBlockingDiscoveryEnabled
	@Order(REACTIVE_SERVICE_INSTANCE_SUPPLIER_ORDER + 1)
	public static class BlockingSupportConfiguration {

		@Bean
		@ConditionalOnBean(DiscoveryClient.class)
		@ConditionalOnMissingBean
		public ServiceInstanceListSupplier discoveryClientServiceInstanceListSupplier(
				DiscoveryClient discoveryClient, Environment env,
				ApplicationContext context) {
			return cachedOrDelegate(context, discoveryClientBased(discoveryClient, env));
		}

		@Bean
		@ConditionalOnBean(DiscoveryClient.class)
		@ConditionalOnMissingBean
		public ServiceInstanceSupplier discoveryClientServiceInstanceSupplier(
				DiscoveryClient discoveryClient, Environment env,
				ApplicationContext context) {
			DiscoveryClientServiceInstanceSupplier delegate = new DiscoveryClientServiceInstanceSupplier(
					discoveryClient, env);
			ObjectProvider<LoadBalancerCacheManager> cacheManagerProvider = context
					.getBeanProvider(LoadBalancerCacheManager.class);
			if (cacheManagerProvider.getIfAvailable() != null) {
				return new CachingServiceInstanceSupplier(delegate,
						cacheManagerProvider.getIfAvailable());
			}
			return delegate;
		}

	}

}
