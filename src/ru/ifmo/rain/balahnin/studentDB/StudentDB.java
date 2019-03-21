package ru.ifmo.rain.balahnin.studentDB;

import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.Student;
import info.kgeorgiy.java.advanced.student.StudentGroupQuery;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

public class StudentDB implements StudentGroupQuery {
    @Override
    public List<String> getFirstNames(List<Student> list) {
        return getField(list, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> list) {
        return getField(list, Student::getLastName);
    }

    @Override
    public List<String> getGroups(List<Student> list) {
        return getField(list, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(List<Student> list) {
        return getField(list, student -> student.getFirstName() + " " + student.getLastName());
    }

    private List<String> getField(List<Student> students, Function<? super Student, ? extends String> mapper) {
        return students.stream().map(mapper).collect(Collectors.toList());
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> list) {
        return list.stream()
                .map(Student::getFirstName).collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public String getMinStudentFirstName(List<Student> list) {
        return list.stream()
                .min(Comparator.comparingInt(Student::getId))
                .map(Student::getFirstName)
                .orElse("");
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> collection) {
        return sortBy(collection, Comparator.comparingInt(Student::getId));
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> collection) {
        return sortBy(collection, Comparator.comparing(Student::getLastName)
                .thenComparing(Student::getFirstName)
                .thenComparingInt(Student::getId));
    }

    private List<Student> sortBy(Collection<Student> collection, Comparator<? super Student> comparator) {
        return collection.stream().sorted(comparator).collect(Collectors.toList());
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> collection, String s) {
        return findBy(collection, (Student student) -> student.getFirstName().equals(s));
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> collection, String s) {
        return findBy(collection, (Student student) -> student.getLastName().equals(s));
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> collection, String s) {
        return findBy(collection, (Student student) -> student.getGroup().equals(s));
    }

    private List<Student> findBy(Collection<Student> collection, Predicate<? super Student> predicate) {
        return sortStudentsByName(collection.stream()
                .filter(predicate)
                .collect(Collectors.toList()));
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> collection, String s) {
        return collection.stream()
                .filter(student -> student.getGroup().equals(s))
                .collect(Collectors.
                        toMap(Student::getLastName, Student::getFirstName, BinaryOperator.minBy(String::compareTo)));
    }

    @Override
    public List<Group> getGroupsByName(Collection<Student> collection) {
        return getGroupsBy(collection, this::sortStudentsByName);
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> collection) {
        return getGroupsBy(collection, this::sortStudentsById);
    }

    private List<Group> getGroupsBy(Collection<Student> collection, Function<Collection<Student>, List<Student>> comparator) {
        return collection.stream()
                .collect(Collectors.groupingBy(Student::getGroup, Collectors.toList()))
                .entrySet().stream().map(
                        entry -> new Group(entry.getKey(), comparator.apply(entry.getValue())))
                .sorted(Comparator.comparing(Group::getName)).collect(Collectors.toList());
    }

    @Override
    public String getLargestGroup(Collection<Student> collection) {
        return getLargestGroup(collection, (Group group) -> group.getStudents().size());
    }

    @Override
    public String getLargestGroupFirstName(Collection<Student> collection) {
        return getLargestGroup(collection, (Group group) -> getDistinctFirstNames(group.getStudents()).size());
    }

    private String getLargestGroup(Collection<Student> collection, ToIntFunction<Group> comparator) {
        return getGroupsByName(collection).stream()
                .max(Comparator
                        .comparingInt(comparator)
                        .thenComparing(Group::getName, Collections.reverseOrder(String::compareTo)))
                .map(Group::getName).orElse("");
    }

}
