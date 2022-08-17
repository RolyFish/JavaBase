package com.roily.booknode.javatogod._02coll;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * @author rolyfish
 * @Date: 2022/08/09/11:20
 * @Description:
 */
public class AboutStream {


    @Test
    public void test1() {
        System.out.println("===============流  转集合===============");
        List<String> list = new ArrayList<>(Arrays.asList("1", "1", "2", "3", "4", "5", "6", "7", "8", "9"));
        List<String> collect1 = list.stream().collect(Collectors.toList());
        collect1.stream().forEach(System.out::print);
        System.out.println();

        System.out.println("===============遍历===============");
        list.stream().forEach(System.out::print);
        System.out.println();

        System.out.println("===============过滤===============");
        List<String> collect2 = list.stream().filter((ele) -> ele.equals("2")).collect(Collectors.toList());
        collect2.stream().forEach(System.out::print);
        System.out.println();

        System.out.println("===============映射===============");
        List<Integer> collect3 = list.stream().map(Integer::valueOf).collect(Collectors.toList());
        collect3.stream().forEach(System.out::print);
        System.out.println();

        System.out.println("===============求和 求平均值===============");
        int sum = list.stream().mapToInt(Integer::valueOf).sum();
        System.out.println(sum);

        System.out.println("===============去重===============");
        List<String> collect4 = list.stream().distinct().collect(Collectors.toList());
        collect4.stream().forEach(System.out::print);
        System.out.println();

        System.out.println("===============判断===============");
        final boolean b1 = list.stream().allMatch(ele -> "2".equals(ele));
        final boolean b2 = list.stream().anyMatch(ele -> "2".equals(ele));
        System.out.println(b1 + " " + b2);
        System.out.println();

        System.out.println("===============获取option===============");
        String s1 = list.stream().findAny().get();
        String s2 = list.stream().findFirst().get();
        System.out.println(s1 + " " + s2);
        System.out.println();

        System.out.println("===============对每一个元素进行操作===============");
        List<String> collect5 = list.stream().peek(ele -> {
            if (ele.equals("2")) {
                System.out.println("xxxx");
            }
        }).collect(Collectors.toList());
        collect5.stream().forEach(System.out::print);
        System.out.println();

    }

    @Test
    public void test2() {
        final Stream<String> stringStream = Stream.of("1", "2", "3");
        stringStream.filter("2"::equals);
        stringStream.forEach(System.out::println);
    }

    @Test
    public void test3() {

        final Stream<String> stringStream = Stream.of("1", "2", "3");
        final Stream<String> stringStreamFilter = stringStream.filter("2"::equals);
        stringStreamFilter.forEach(System.out::println);
    }

    @Test
    public void test4() {

        //通过集合类创建流
        final Collection<String> strings = Arrays.asList("1", "2", "3");
        final Stream<String> stream = strings.stream();

        //Stream创建流
        final Stream<String> stringStream = Stream.of("1", "2", "3");
        final Stream<String> a = Stream.of("a");

        //Arrays.stream(T[] t)
        final Stream<String> stream1 = Arrays.stream(new String[]{"1", "2", "3"});

        stream1.distinct().forEach(System.out::println);

    }


    @Test
    public void test5() {
        final StringBuffer sb1 = new StringBuffer("1");
        final StringBuffer sb2 = new StringBuffer("2");
        final StringBuffer sb3 = new StringBuffer("3");


        final Stream<StringBuffer> stream = Arrays.asList(sb1, sb2, sb3).stream();

        final HashMap<Object, Object> collect;

    }

    @Test
    public void test6() {
        final StringBuffer sb1 = new StringBuffer("1");
        final StringBuffer sb2 = new StringBuffer("2");
        final StringBuffer sb3 = new StringBuffer("3");


        final Stream<StringBuffer> stream = Arrays.asList(sb1, sb2, sb3).stream();

        final IntStream intStream = stream.mapToInt(StringBuffer::length);
        final DoubleStream doubleStream = stream.mapToDouble(StringBuffer::length);
        final LongStream longStream = stream.mapToLong(StringBuffer::length);

    }
}
