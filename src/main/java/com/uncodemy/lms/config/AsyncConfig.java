package com.uncodemy.lms.config;

import lombok.extern.slf4j.Slf4j;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * ============================================================================
 * AsyncConfig
 * ============================================================================
 *
 * Do cheezein ON karta hai:
 *
 *   @EnableAsync      ->  @Async wale methods background me chalenge
 *   @EnableScheduling ->  @Scheduled wale methods time pe chalenge
 *                         (Phase 9 me group message cleanup ke liye)
 *
 * YE FILE KYUN ZAROORI HAI
 * ---------------------------------------------------------------------------
 * @Async akela kuch nahi karta. Agar @EnableAsync kahin nahi hai
 * to Spring us annotation ko chup-chaap IGNORE kar deta hai —
 * na error, na warning. Mail synchronous hi chalti rehti hai
 * aur developer sochta rehta hai "async to laga rakha hai".
 *
 * Bahut common bug hai. Ye file wahi fix karti hai.
 *
 * APNA THREAD POOL KYUN, DEFAULT KYUN NAHI?
 * ---------------------------------------------------------------------------
 * Spring ka default executor har @Async call pe NAYA THREAD
 * bana deta hai — koi limit nahi.
 *
 * Socho 500 students ko announcement gayi -> 500 thread ban gaye
 * -> server ki memory khatam -> app crash.
 *
 * Apna pool banane se max thread fix ho jate hain.
 * ============================================================================
 */
@Slf4j
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig implements AsyncConfigurer {

    /**
     * Mail bhejne wala thread pool.
     *
     * SETTINGS SAMJHO
     * -----------------------------------------------------------------------
     *
     * corePoolSize = 3
     *   Hamesha 3 thread ready rahenge.
     *
     * maxPoolSize = 10
     *   Load badha to zyada se zyada 10 tak jayenge.
     *
     * queueCapacity = 100
     *   Saare thread busy hain? To agla kaam line me lag jayega.
     *   100 tak line lag sakti hai.
     *
     * KAAM KA ORDER (ye ulta lagta hai lekin yahi Java ka rule hai):
     *   1. Pehle 3 core thread bharenge
     *   2. Phir QUEUE bharegi (100 tak)
     *   3. Queue full hone ke BAAD hi naye thread banenge (10 tak)
     *   4. Sab full -> rejection policy chalegi
     *
     * Matlab 10 thread tabhi banenge jab 100 kaam line me pade hon.
     * Mail ke liye ye theek hai — thoda late chalega, crash nahi hoga.
     */
    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);

        /*
         * Thread ka naam. Log me aise dikhega:
         *   [lms-async-1] MAIL SENT | to=rahul@gmail.com
         *
         * Isse turant pata chal jata hai ki ye kaam
         * background thread me hua tha, main request me nahi.
         */
        executor.setThreadNamePrefix("lms-async-");

        /*
         * REJECTION POLICY --- sab full ho jaye to kya karein?
         *
         * CallerRunsPolicy = kaam ko FEK do mat,
         * jisne call kiya usi ke thread me chala do.
         *
         * Iska matlab: mail bhejne wali request thodi slow
         * ho jayegi, LEKIN mail JAYEGI ZAROOR.
         *
         * Doosra option AbortPolicy hota hai — wo kaam
         * chup-chaap fek deta hai. Mail ke case me wo galat hai,
         * student ko announcement mile hi na, ye chalega nahi.
         *
         * Ye policy ek "brake" ka kaam bhi karti hai — system
         * apne aap dheema ho jata hai bajay crash hone ke.
         */
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        /*
         * App band karte waqt chalte hue kaam pura hone do.
         *
         * Bina iske deploy/restart ke time pe aadhi bheji hui
         * mails beech me hi kat jati hain.
         *
         * 30 second tak wait karega.
         */
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();

        log.info("Async executor ready | core=3 max=10 queue=100");

        return executor;
    }


    /**
     * Async method me exception aa gayi to kya karein?
     *
     * PROBLEM
     * -----------------------------------------------------------------------
     * void return karne wale @Async method ki exception
     * KAHIN NAHI JATI. Caller ko pata hi nahi chalta.
     *
     * Matlab mail fail hui aur console pe kuch aaya hi nahi.
     * Debugging me pagal ho jaoge.
     *
     * Ye handler wahi exception pakad ke log kar deta hai.
     *
     * NOTE: EmailServiceImpl me humne pehle hi try-catch laga
     * rakha hai, to normally ye chalega hi nahi.
     * Ye SAFETY NET hai — koi aisi exception aa gayi jo
     * try-catch se bahar thi, to kam se kam log to hogi.
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {

        return (Throwable ex, Method method, Object... params) -> {
            log.error("ASYNC METHOD FAILED | method={} | message={}",
                    method.getName(), ex.getMessage(), ex);
        };
    }
}