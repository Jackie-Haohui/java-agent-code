package com.example.agent.tool;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class WorkspaceContextFilter implements Filter {

    private final WorkspaceContext workspaceContext;

    public WorkspaceContextFilter(WorkspaceContext workspaceContext) {
        this.workspaceContext = workspaceContext;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            chain.doFilter(request, response);
        } finally {
            workspaceContext.clear();
        }
    }
}
