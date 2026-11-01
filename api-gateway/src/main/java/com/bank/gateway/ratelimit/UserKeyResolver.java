package com.bank.gateway.ratelimit;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class UserKeyResolver implements KeyResolver{

	@Override
	public Mono<String> resolve(ServerWebExchange exchange) {
	System.out.println("this is resolve method of key resolver");
		return Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst("X-User-Id"))
				.switchIfEmpty(
						Mono.just(exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()));
	}

}
