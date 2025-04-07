package com.cubrid.cubridmigration.cubrid.export;

public class GitActionTestClass {
    public void doSomething() {
        try {
            System.out.println("This should trigger PMD: System.print used!");
        } catch (Exception e) {
            // PMD: EmptyCatchBlock rule
        }
    } 
}
