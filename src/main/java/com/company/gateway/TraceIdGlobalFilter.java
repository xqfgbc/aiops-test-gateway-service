package com.company.gateway;

import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * traceId 入口：读/生成 traceId，写 MDC，加 X-Trace-Id header 转发给下游。
 */
@Component
public class TraceIdGlobalFilter implements GlobalFilter, Ordered {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String TRACE_ID_MDC = "traceId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String traceId = request.getHeaders().getFirst(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }
        MDC.put(TRACE_ID_MDC, traceId);
        ServerHttpRequest mutated = request.mutate().header(TRACE_ID_HEADER, traceId).build();
        return chain.filter(exchange.mutate().request(mutated).build())
                .doFinally(signalType -> MDC.remove(TRACE_ID_MDC));
    }

    @Override
    public int getOrder() {
        return -1;   // 最先执行
    }
}
