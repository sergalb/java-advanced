package ru.ifmo.rain.balahnin.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.nio.file.Path;

public class Test {
    public static void main(String[] args) throws ImplerException {
        Implementor implementor = new Implementor();
        implementor.implement(ru.ifmo.rain.balahnin.implementor.TestClass.class,  Path.of("C:\\Users\\Sergey\\Desktop\\java advanced\\HW4 Implementor\\Tests"));
    }
}
