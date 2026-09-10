package com.vincent.msyep.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Background executor for outbound mail.
 *
 * <p>Franchise mails carry the Certificate plus the ~10 MB MOU, and pushing that up to the SMTP relay
 * takes well over a minute. Doing it inside the HTTP request made the browser sit on a spinner for the
 * whole time, so a send that was actually succeeding looked like it had failed. These dispatches run
 * here instead and the request returns immediately; the outcome is recorded in Sent Mail History.
 *
 * <p>The pool is deliberately small and the queue bounded: a handful of 10 MB messages in flight is
 * already plenty for the hosted instance, and CallerRunsPolicy means a flood degrades to the old
 * synchronous behaviour rather than piling messages up in memory.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "mailExecutor")
    public Executor mailExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(2);
        ex.setMaxPoolSize(3);
        ex.setQueueCapacity(50);
        ex.setThreadNamePrefix("mail-");
        ex.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        ex.setWaitForTasksToCompleteOnShutdown(true);
        ex.setAwaitTerminationSeconds(120);   // let an in-flight send finish before the app exits
        ex.initialize();
        return ex;
    }
}
