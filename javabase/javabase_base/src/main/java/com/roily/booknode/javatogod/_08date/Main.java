package com.roily.booknode.javatogod._08date;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Hollis
 */
public class Main {

    /**
     * 定义一个全局的SimpleDateFormat
     */
    private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 使用ThreadFactoryBuilder定义一个线程池
     */
    private static ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
            .setNameFormat("demo-pool-%d").build();

    private static ExecutorService pool = new ThreadPoolExecutor(5, 200,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(1024), namedThreadFactory, new ThreadPoolExecutor.AbortPolicy());

    /**
     * 定义一个CountDownLatch，保证所有子线程执行完之后主线程再执行
     */
    private static CountDownLatch countDownLatch = new CountDownLatch(100);

    public static void main(String[] args) throws InterruptedException {
        //定义一个线程安全的HashSet
        Set<String> dates = Collections.synchronizedSet(new HashSet<>());
        Set<Calendar> date = Collections.synchronizedSet(new HashSet<>());
        //获取当前时间
        Calendar calendar = Calendar.getInstance();
        for (int i = 0; i < 100; i++) {
            int finalI = i;
            pool.execute(() -> {
                //时间增加
                calendar.add(Calendar.DATE, finalI);
                date.add(calendar);
                //通过simpleDateFormat把时间转换成字符串
                String dateString = simpleDateFormat.format(calendar.getTime());
                System.out.println(dateString);
                //把字符串放入Set中
                dates.add(dateString);
                //countDown
                countDownLatch.countDown();
            });
        }
        //阻塞，直到countDown数量为0
        countDownLatch.await();
        //输出去重后的时间个数
        System.out.println(dates.size());
        System.out.println(date.size());
    }
}
