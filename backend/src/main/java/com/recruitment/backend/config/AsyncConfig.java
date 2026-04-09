package com.recruitment.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // Số lượng nhân viên tối thiểu luôn túc trực
        executor.setCorePoolSize(5);

        // Số lượng nhân viên tối đa được gọi thêm khi đông khách
        executor.setMaxPoolSize(10);

        // Kích thước phòng chờ: Nếu có hơn 10 cái CV đến cùng lúc, nhét vào hàng đợi (tối đa 100 cái)
        executor.setQueueCapacity(100);

        executor.setThreadNamePrefix("CvWorker-");
        executor.initialize();
        return executor;
    }
}
