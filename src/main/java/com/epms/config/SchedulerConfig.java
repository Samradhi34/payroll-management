package com.epms.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import lombok.extern.slf4j.Slf4j;

/**
 * Configure thread pool for scheduled tasks to avoid running all tasks sequentially
 * on a single background thread.
 */
@Slf4j
@Configuration
@EnableScheduling
public class SchedulerConfig implements SchedulingConfigurer {

	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
		ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();

		threadPoolTaskScheduler.setPoolSize(3);
		threadPoolTaskScheduler.setThreadNamePrefix("epms-sched-");
		threadPoolTaskScheduler.setErrorHandler(throwable -> 
				log.error("An error occurred during scheduled task execution: {}", throwable.getMessage(), throwable)
		);
		threadPoolTaskScheduler.initialize();

		taskRegistrar.setTaskScheduler(threadPoolTaskScheduler);
		log.info("Initialized EPMS ThreadPoolTaskScheduler with pool size 3");
	}
}
