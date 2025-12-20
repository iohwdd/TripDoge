package com.tripdog.hook.filter;

import java.io.IOException;

import org.slf4j.MDC;

import com.tripdog.common.utils.SelfTraceIdGenerator;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

/**
 * @author: iohw
 * @date: 2025/9/29 10:28
 * @description:
 */
public class ReqFilter implements Filter {
    public static final String TRACE_ID = "trace_id";
    @Override
    public void doFilter(ServletRequest req, ServletResponse rsp, FilterChain filterChain)
    throws IOException, ServletException {
        try {
            // 设置请求链路唯一标识 - traceId
            MDC.put(TRACE_ID, SelfTraceIdGenerator.generate());

            filterChain.doFilter(req, rsp);
        } finally {
            MDC.remove(TRACE_ID);
        }
    }
}
