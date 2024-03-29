package com.roily.base.collectionframework.base.list.arraylist;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.Test;
import org.openjdk.jol.vm.VM;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * descripte:
 *
 * @author: RoilyFish
 * @date: 2022/2/22
 */
public class ArrayListLearn {

    /**
     * ①ArrayList在扩容时，会对数组进行拷贝，会调用util包下的数组工具类Arrays，最终还是会调用System.arraycopy
     * ②这个拷贝是一个浅拷贝
     */
    @Test
    public void testArrayCopy() {
        String[] oldArray = new String[10];
        oldArray[0] = "123";
        //100为拷贝后新数组长度
        //新数组长度位 min(original.length, newLength)
        String[] newArray = Arrays.copyOf(oldArray, 100);

        System.out.println("=========oldArray==========");
        System.out.println("oldArray 长度" + oldArray.length);
        System.out.println("oldArray 哈希值" + Arrays.hashCode(oldArray));
        System.out.println("oldArray[0] 哈希值" + oldArray[0].hashCode());

        System.out.println("=========newArray==========");
        System.out.println("newArray 长度" + newArray.length);
        System.out.println("newArray 哈希值" + Arrays.hashCode(newArray));
        System.out.println("newArray[0] 哈希值" + newArray[0].hashCode());
        System.err.println("array[0]哈希值相同，是一个浅拷贝");

        System.out.println("========如果说newLength不足的话，新数组会被截断=========");
        String[] oldArray1 = new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};
        String[] newArray1 = Arrays.copyOf(oldArray1, 5);
        Arrays.stream(newArray1).forEach(System.out::println);

        System.out.println("========Arrays.copy方法默认从0开始拷贝,可使用System.arraycopy定制=========");
        String[] oldArray2 = new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};
        String[] newArray2 = new String[5];
        //params：源数组、开始拷贝起始下表、目标数组、拷贝起始下标、拷贝长度（这个长度不能超过目标数组长度）
        System.arraycopy(oldArray2, 5, newArray2, 0, 5);
        Arrays.stream(newArray2).forEach(System.out::println);

    }

    @Test
    public void sysArrayCopy() {
        int[] a = new int[10];
        a[0] = 0;
        a[1] = 1;
        a[2] = 2;
        a[3] = 3;
        /**
         * 参数：
         * - 源数组
         * - 源数组需要被拷贝数据的起始下标
         * - 目标数组
         * - 目标数组接受源数组数据的起始下标
         * - 拷贝长度
         */
        System.arraycopy(a, 2, a, 3, 3);
        //a[2]=99;
        for (int j : a) {
            System.out.print(j + " ");
        }
    }

    //ArrayList的size指的是元素个数，而不是数组长度
    @Test
    public void testSize() {
        ArrayList<Object> list = new ArrayList<>(10);
        System.out.println(list.size());
        //size是arrayList数组elementData中元素的个数，并不是数组定义长度
    }

    @Test
    public void StrASProperty01() {
        //String有常量池的概念，借助VM获取其内存地址。或者直接等号判断
        String str = "abc";
        String str2 = "abc";
        System.out.println(str == str2);
        System.out.println(VM.current().addressOf(str));
        System.out.println(VM.current().addressOf(str2));

        //使用new关键字创建String对象，对象被分配在堆内存，和普通引用对象是一样的
        System.out.println();
        String str3 = new String("abc");
        String str4 = new String("abc");
        System.out.println(str3 == str4);
        System.out.println(VM.current().addressOf(str3));
        System.out.println(VM.current().addressOf(str4));

    }

    @Test
    public void StrASProperty02() throws NoSuchFieldException {
        //
        StrASProperty strASProperty1 = new StrASProperty("123");
        StrASProperty strASProperty2 = new StrASProperty("123");
        Field str1 = strASProperty1.getClass().getDeclaredField("str");
        Field str2 = strASProperty2.getClass().getDeclaredField("str");
        System.out.println(str1 == str2);
        System.out.println(VM.current().addressOf(str1));
        System.out.println(VM.current().addressOf(str2));

        //str作为对象属性也是共用一个str对象
        String val1 = strASProperty1.getStr();
        String val2 = strASProperty2.getStr();
        System.out.println(val1 == val2);
        System.out.println(VM.current().addressOf(val1));
        System.out.println(VM.current().addressOf(val2));
    }


    /**
     * 测试return的值是否会被finally修改
     * <p>
     * finally执行在return真正成功之前
     * <p>
     * 记住，return会记住要return的引用，而finally可以修改此引用所指向的内存空间
     */
    @Test
    public void test() {
        System.out.println("return记住了引用sb，但是可以被修改。所以此例返回“123abc”");
        System.out.println(get01());

        System.out.println("return记住了引用sb，但是可以被修改。所以此例返回“123”");
        System.out.println(get02());

        System.out.println("对于基本数据类型只有值，return记住了值");
        System.out.println(get03());
    }

    //return记住了引用str，但是String不可变，即return记住的引用所指向的String不变。
    public Object get02() {
        String str = "123";
        try {
            return str;
        } finally {
            str = "abc";
        }
    }

    //"return记住了引用sb，但是可以被修改。所以此例返回“123abc”"
    public Object get01() {
        StringBuffer sb = new StringBuffer("123");
        try {
            return sb;
        } finally {
            sb.append("abc");
        }
    }

    //"return记住了引用sb，但是可以被修改。所以此例返回“123abc”"
    public int get03() {
        int i = 0;
        try {
            return i;
        } finally {
            ++i;
        }
    }

}

@Data
@NoArgsConstructor
@AllArgsConstructor
class StrASProperty {

    String str;

}