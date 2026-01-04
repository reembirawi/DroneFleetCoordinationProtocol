package org.example;

import org.example.util.GeoLocation;

import java.util.concurrent.ConcurrentHashMap;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {

    public static ConcurrentHashMap<String, GeoLocation> tasks = new ConcurrentHashMap<>();

    public static void main(String[] args) {

        System.out.println("Drone Fleet Coordination Protocol Project");

    }
}
