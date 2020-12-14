package com.xuecheng.search;

import org.junit.Test;

public class ExceptionTest {
    @Test
    public void testTryCatch() {
        try {
            System.out.println("A");
//            Integer.parseInt(null);
            try {
                System.out.println("B");
                Integer.parseInt(null);
            } catch (Exception ex2) {
                System.out.println("C");
//                Integer.parseInt(null);
            } finally {
                System.out.println(" D ");
//                Integer.parseInt(null);
            }
            System.out.println(" E");
//            Integer.parseInt(null);
        } catch (Exception ex) {
            try {
                System.out.println("F ");
//                Integer.parseInt(null);
            } catch (Exception ex2) {
                System.out.println("G ");
//                Integer.parseInt(null);
            } finally {
                System.out.println("H ");
//                Integer.parseInt(null);
            }
            System.out.println("I ");
//            Integer.parseInt(null);
        } finally {
            System.out.println("J ");
//            Integer.parseInt(null);
        }
    }
}