package ru.ifmo.rain.balahnin.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.full.lang.ПриветInterface;

import java.nio.file.Path;

public class Test {
    public static void main(String[] args) throws ImplerException {
        Implementor implementor = new Implementor();
        implementor.implement(Test.class,  Path.of("C:\\Users\\Sergey\\Desktop\\java advanced\\Test\\"));
    }
}
